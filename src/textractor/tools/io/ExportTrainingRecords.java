/*
 * Copyright (C) 2004-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package textractor.tools.io;

import edu.mssm.crover.cli.CLI;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DbManager;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.FeatureCreationParameters;
import textractor.datamodel.annotation.AnnotationSource;
import textractor.learning.SVMFeatureExporter;
import textractor.learning.SingleBagOfWordExporter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * A tool to export annotation in a format suitable for training a machine learning algorithm.
 * At this time, the only learning algorithms supported are Support Vector Machines, as implemented
 * in <a href="http://svmlight.joachims.org/">SVMlight</a> or in <a href="http://www.csie.ntu.edu.tw/~cjlin/libsvm/">libSVM</a>
 * <p/>
 * Creation Date: Jan 17, 2004
 * Creation Time: 7:19:14 PM
 *
 * @author Fabien Campagne
 */
public final class ExportTrainingRecords extends ExportRecords {
    private static final Log LOG =
            LogFactory.getLog(ExportTrainingRecords.class);
    private int seed;
    private int fold;
    private Writer testWriter;

    public static void main(final String[] args) throws IOException, TextractorDatabaseException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        final ExportTrainingRecords trainingExporter = new ExportTrainingRecords();
        trainingExporter.process(args);

        /*  Args:   (training only (TO); training and testing (TT); testing only (T))
            training-output filename:   TO, TT
            testing-output filename:    TT, T
            batch id:                   TO, TT, T
            window:                     TO, TT
            recordType:                 TO, TT, T
            fold:                       TT
            seed:                       TT
            parameter-set:              T
        */
    }

    public ExportTrainingRecords() {
        parameter_set = NO_PARAMETER_SET;
        fold = NO_TEST_SET;
    }

    public ExportTrainingRecords(final DbManager dbm, final int batch_id,final Writer training_writer, final Writer test_writer) {
        this();

        batchId=batch_id;
        writer=training_writer;
        testWriter=test_writer;
        this.dbm=dbm;

    }

    @Override
    public void printHelp() {
        System.out.println("ExportTrainingRecords: help file ");
        System.out.println("There are three options, with slightly different arguments: \n" +
                "exporting a training set only; exporting a training and a test set; exporting a test set only\n" +
                "To export a training set only, the required arguments are \n\t-o (training records output file); \n" +
                "\t-batch (the batch ID of the annotations to export)\n" +
                "\t-window (the window size around the term to consider for the Bag Of Words): default = 5 \n" +
                "\t-recordType (the type of record to be exported, e.g., protein/mutation)\n" +
                "To export a training set and a test set, the required arguments are\n\t-o (training records output file); \n" +
                "\t-ot (testing records output file)\n" +
                "\t-batch (the batch ID of the annotations to export)\n" +
                "\t-window (the window size around the term to consider for the Bag Of Words): default = 5 \n" +
                "\t-recordType (the type of record to be exported, e.g., protein/mutation)\n" +
                "\t-fold (the fold difference between the training and the test set)\n" +
                "\t-seed (the random number generator seed to choose which records become training records, and which become test records\n" +
                "If the -fold argument is given, it will be assumed that both a training and test set of records are to be exported.\n" +
                "To export test records only, the required arguments are\n\t-ot (testing records output file)\n" +
                "\t-batch (the batch ID of the annotations to export)\n" +
                "\t-recordType (the type of record to be exported, e.g., protein/mutation)\n" +
                "\t-parameter (the parameter set id number from which to get the parameters (such as terms in the Bag of Words).\n" +
                "If the -parameter argument is given, only a test set of records will be exported.\n" +
                "Precedence is given to exporting a training set only, then exporting a test set only, then both a training and test set.");
    }

    public void setFold(final int fold) {
        this.fold = fold;
    }

    public void setSeed(final int seed) {
        this.seed = seed;
    }

    @Override
    protected void process(final String[] args) throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        getParameters(args);
        super.process(args);
    }

    @Override
    protected void getParameters(final String[] args) throws IOException {
        super.getParameters(args);
        final String test_output_filename = CLI.getOption(args, "-ot", "testing-records.out");
        fold = CLI.getIntOption(args, "-fold", NO_TEST_SET); // number of fold for n-fold validation. The annotation set
        // is split into n parts. n-1 parts are used for training and testing is done on the remaining part (fold).
        seed = CLI.getIntOption(args, "-seed", 25);   // random seed for the n-fold random split.
        if ((fold != NO_TEST_SET) || (parameter_set != NO_PARAMETER_SET)) {
            testWriter = new FileWriter(test_output_filename);
        }
    }

    @Override
    public int export(final FeatureCreationParameters parameters) throws IOException, TextractorDatabaseException {
        final Collection<AnnotationSource> annotations = dbm.getTextractorManager().getAnnotationsInBatch(batchId);
        LOG.info("Number of annotations: " + annotations.size());
        exportTrainingRecords(annotations, parameters);
        dbm.makePersistent(parameters);
        dbm.checkpointTxn();
        return parameters.getParameterNumber();
    }

    public void setExporter(final SingleBagOfWordExporter exporter) {
        this.exporter = exporter;
    }

    /**
     * Splits annotations into training_annotations and testing_annotations. The split is based on seed and fold parameters.
     *
     * @param randomNumberGenerator uses the seed to generate random numbers between 0.0 and 1.0.
     * @param annotations           the set of TextFragmentAnnotations to be split.
     * @param training_annotations  the resulting set of TextFragmentAnnotations to be used as a training set.
     * @param testing_annotations   the resulting set of TextFragmentAnnotations to be used as a test set.
     */
    private void splitAnnotations(final Random randomNumberGenerator, final Collection<AnnotationSource> annotations, final Collection<AnnotationSource> training_annotations, final Collection<AnnotationSource> testing_annotations) {
        final double fold_cutoff = 1.0 / fold;
        LOG.info("Fold cutoff: " + fold_cutoff);
        for (final AnnotationSource annotation : annotations) {
            if (randomNumberGenerator.nextDouble() > fold_cutoff) {
                LOG.debug("Adding to training");
                training_annotations.add(annotation);
            } else {
                LOG.debug("Adding to testing");
                testing_annotations.add(annotation);
            }
        }
    }

    private void exportTrainingRecords(final Collection<AnnotationSource> annotations, FeatureCreationParameters parameters) throws IOException, TextractorDatabaseException {
        boolean train = false;
        boolean test = false;
        if (parameter_set != NO_PARAMETER_SET) {    // if we have a parameter set id, we export only test records
            test = true;
        } else if (fold != NO_TEST_SET) {           // if we have no parameter set id, but we do have a fold difference, we export both a training and a test set
            test = true;
            train = true;
        } else {
            train = true;
        }
        // here  - split annotations into training and testing; pass only training into first pass
        Collection<AnnotationSource> training_annotations = null;
        Collection<AnnotationSource> testing_annotations = null;
        if (train && test) {
            training_annotations = new ArrayList<AnnotationSource>();
            testing_annotations = new ArrayList<AnnotationSource>();
            final Random randomNumberGenerator = new Random(seed);
            splitAnnotations(randomNumberGenerator, annotations, training_annotations, testing_annotations);
        } else if (train) { // split is not required. All annotations go to one pool.
            training_annotations = annotations;
        } else {
            testing_annotations = annotations;
        }

        // first pass - training records only, to get the parameters
        if (train) {
            exporter.firstPass(parameters, training_annotations);
        }

        // second pass - export required records (training and/or test)
        if (train) {
            LOG.info("Exporting a training set");

            exporter.secondPass(SVMFeatureExporter.fromAnnotationClass, parameters, training_annotations, writer);
            writer.flush();
        }
        if (test) {
            LOG.info("Exporting a test set");
            if (parameter_set != NO_PARAMETER_SET) {
		parameters = dbm.getTextractorManager().getParameterSetById(parameter_set);
	    }
            if (parameters == null) {
                throw new TextractorDatabaseException("Parameter set " + parameter_set + " cannot be found in the database.");
            }

            LOG.info("Parameter set info: window size: " + parameters.getWindowSize());
            exporter.secondPass(SVMFeatureExporter.fromAnnotationClass, parameters, testing_annotations, testWriter);
            testWriter.flush();
        }
        if (verbose) {
            System.err.print("These terms are considered:  ");
            for (int i = 0; i < parameters.getTerms().length; i++) {
                final String term = parameters.getTerms()[i];
                System.err.print(term);
                System.err.print("(" + i + ")");
                System.err.print(' ');
            }
        }
    }
}
