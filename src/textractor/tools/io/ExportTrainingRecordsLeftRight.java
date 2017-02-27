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
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.SingleBagOfWordFeatureCreationParameters;
import textractor.datamodel.annotation.AnnotationSource;
import textractor.learning.MutationBagOfWordExporter;
import textractor.learning.ProteinNameBagOfWordExporter;
import textractor.learning.SVMFeatureExporter;
import textractor.learning.SingleBagOfWordExporter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * A tool to export annotation in a format suitable for training a machine learning algorithm.
 * At this time, the only learning algorithms supported are Support Vector Machines, as implemented
 * in <a href="http://svmlight.joachims.org/">SVMlight</a> or in <a href="http://www.csie.ntu.edu.tw/~cjlin/libsvm/">libSVM</a>
 *
 * Creation Date: Jan 17, 2004
 * Creation Time: 7:19:14 PM
 * @author Fabien Campagne
 */
public final class ExportTrainingRecordsLeftRight {
    private boolean verbose;

    public static void main(final String[] args) throws ConfigurationException, IllegalAccessException, NoSuchMethodException, IOException, TextractorDatabaseException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        final ExportTrainingRecordsLeftRight exporter = new ExportTrainingRecordsLeftRight();
        exporter.process(args);
    }

    private void process(final String[] args) throws ConfigurationException, IOException, TextractorDatabaseException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final String output_filename = CLI.getOption(args, "-o", "tranining-records.out");
        final int batchId = CLI.getIntOption(args, "-batch", -1);
        final int windowWidth = CLI.getIntOption(args, "-window", 5);
        final String record_type = CLI.getOption(args, "-recordType", "protein");
        verbose = CLI.isKeywordGiven(args, "-v", false);

        if (CLI.isKeywordGiven(args, "-help")) {
            System.err.println("use -window to set window width (default= 5 words). -o for output filename, -batch for batchNumber, -recordType for type of record (e.g., protein, mutation).");
            System.exit(1);
        }
        if (output_filename == null) {
            System.err.println("Output file must be provided (-o).");
            System.exit(1);
        }
        if (batchId == -1) {
            System.err.println("Batch id must be provided (-batch)");
            System.exit(1);
        }

        final FileWriter writer = new FileWriter(output_filename);
        final DbManager dbm = new DbManager(args);
        dbm.beginTxn();
        final int paramNumber = export(dbm, batchId, windowWidth, writer, record_type);
        System.err.println("Parameters of this export saved in parameter number " + paramNumber);
        writer.flush();
        writer.close();
        dbm.commitTxn();
        System.exit(0);
    }

    public int export(final DbManager dbm, final int batchId, final int windowWidth, final Writer writer, final String record_type) throws IOException, TextractorDatabaseException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        final TextractorManager tm = dbm.getTextractorManager();
        final Collection annotations = tm.getAnnotationsInBatch(batchId);
        final SingleBagOfWordFeatureCreationParameters parameters_left = new SingleBagOfWordFeatureCreationParameters();
        parameters_left.setParameterNumber(tm.getNextParameterNumber());
        parameters_left.setWindowSize(windowWidth);
        parameters_left.setWindowLocation(SingleBagOfWordFeatureCreationParameters.LOCATION_LEFT_OF_WORD);

        final SingleBagOfWordFeatureCreationParameters parameters_right = new SingleBagOfWordFeatureCreationParameters();

        parameters_right.setParameterNumber(tm.getNextParameterNumber());
        parameters_right.setWindowSize(windowWidth);
        parameters_right.setWindowLocation(SingleBagOfWordFeatureCreationParameters.LOCATION_RIGHT_OF_WORD);
        exportTrainingRecords(tm, writer, annotations, parameters_left, parameters_right, record_type);
        dbm.makePersistent(parameters_left);
        return parameters_left.getParameterNumber();
    }


    private void exportTrainingRecords(final TextractorManager tm,
                                       final Writer writer, final Collection<AnnotationSource> annotations, final SingleBagOfWordFeatureCreationParameters parameters_left,
                                       final SingleBagOfWordFeatureCreationParameters parameters_right,
                                       final String record_type) throws IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager = new DocumentIndexManager(tm.getInfo().getCaseSensitiveIndexBasename());
        final SingleBagOfWordExporter exporter_left;
        if (record_type.equals("protein")) {
            exporter_left = new ProteinNameBagOfWordExporter(docmanager,false);
        } else {
            exporter_left = new MutationBagOfWordExporter(docmanager,false);
        }
        exporter_left.firstPass(parameters_left, annotations);
        exporter_left.setLastFeatureGroup(false); //export_right after this one.

        final SingleBagOfWordExporter exporter_right;
        if (record_type.equals("protein")) {
            exporter_right = new ProteinNameBagOfWordExporter(docmanager,true);
        } else {
            exporter_right = new MutationBagOfWordExporter(docmanager,true);
        }
        parameters_right.setWindowLocation(SingleBagOfWordFeatureCreationParameters.LOCATION_RIGHT_OF_WORD);
        exporter_right.firstPass(parameters_right, annotations);
        exporter_right.setLastFeatureGroup(true); // no more feature exporter after this one.
        parameters_right.setFirstFeatureNumber(parameters_left.getLastFeatureNumber()+1);

        if (verbose) {
            System.err.print("These terms are considered on the left:  ");

            for (int i = 0; i < parameters_left.getTerms().length; i++) {
                final String term = parameters_left.getTerms()[i];
                System.err.print(term);
                System.err.print("(" + i + ")");
                System.err.print(' ');
            }
            System.err.println();
            System.err.print("These terms are considered on the right:  ");

            for (int i = 0; i < parameters_right.getTerms().length; i++) {
                final String term = parameters_right.getTerms()[i];
                System.err.print(term);
                System.err.print("(" + (i+parameters_left.getTerms().length) + ")");
                System.err.print(' ');
            }
            System.err.println();
        }

        // Now, export bag of words: if the term appears in the window around
        // the word in the annotation, output 1, otherwise output 0
        // for the feature.
        for (final AnnotationSource annotation : annotations) { // for each annotation
            exporter_left.secondPass(SVMFeatureExporter.fromAnnotationClass, parameters_left, annotation, writer);
            exporter_right.secondPass(SVMFeatureExporter.fromAnnotationClass, parameters_right, annotation, writer);
        }

        writer.flush();
    }
}



