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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.FeatureCreationParameters;
import textractor.datamodel.annotation.AnnotationSource;
import textractor.datamodel.annotation.TextFragmentAnnotation;
import textractor.learning.SVMFeatureExporter;
import textractor.learning.SingleBagOfWordExporter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;

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
public final class ExportUnannotatedRecords extends ExportRecords {
    private static final Log LOG =
        LogFactory.getLog(ExportUnannotatedRecords.class);

    public static void main(final String[] args) throws IOException, TextractorDatabaseException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        final ExportUnannotatedRecords eur = new ExportUnannotatedRecords();
        eur.process(args);
    }

    @Override
    public void printHelp() {
        System.out.println("ExportUnannotatedRecords: help file ");
        System.out.println("" +
                "\t-batch (the batch ID of the annotations to export)\n" +
                "\t-window (the size of the window of the Bag Of Words)\n" +
                "\t-recordType (the type of record to be exported, e.g., protein/mutation)\n" +
                "\t-parameter (the parameter set id number from which to get the parameters (such as terms in the Bag of Words).");
    }

    @Override
    protected void process(final String[] args) throws IOException, TextractorDatabaseException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        getParameters(args);
        super.process(args);
    }

    @Override
    public int export(final FeatureCreationParameters parameters) throws IOException, TextractorDatabaseException {
        final int chunkSize = 10000;
        dbm.makePersistent(parameters);

        Collection<AnnotationSource> c =
                tm.getUnannotatedAnnotationsInBatch(batchId);
        System.out.println("get "+c.size()+ " unAnnotatedAnnotation from batch "+batchId);
        final Iterator it = c.iterator();
        if (!it.hasNext()) {
            return parameters.getParameterNumber(); // no annotations in batch.
        }
        int lowerBound = ((TextFragmentAnnotation)it.next()).getAnnotationNumber();
        c = null; // we discard the collection now as to not keep resources.

        while (true) {
            final Collection<AnnotationSource> annotations =
                    tm.getUnannotatedAnnotationsInBatch(batchId, lowerBound, chunkSize);
            if (annotations == null) {
                break;
            }
            if (annotations.size() == 0) {
                break;
            }

            LOG.info("Number of annotations: " + annotations.size());
            exportUnannotatedRecords(annotations, parameters);
            lowerBound += chunkSize;
            dbm.commitTxn();
            dbm.beginTxn();
        }
        return parameters.getParameterNumber();
    }

    public void setExporter(final SingleBagOfWordExporter exporter) {
        this.exporter = exporter;
    }

    private void exportUnannotatedRecords(final Collection<AnnotationSource> annotations, FeatureCreationParameters parameters)
            throws IOException, TextractorDatabaseException {
        LOG.info("Exporting a test set");
        if (parameter_set != NO_PARAMETER_SET) {
	    parameters = tm.getParameterSetById(parameter_set);
	}
        if (parameters == null) {
            throw new TextractorDatabaseException("Parameter set " + parameter_set + " cannot be found in the database.");
        }
        exporter.secondPass(SVMFeatureExporter.fromAnnotationClass, parameters, annotations, writer);
        writer.flush();

        if (verbose) {
            System.err.print("These terms are considered:  ");
            for (int i = 0; i <parameters.getTerms().length; i++) {
                final String term = parameters.getTerms()[i];
                System.err.print(term);
                System.err.print("(" + i + ")");
                System.err.print(' ');
            }
            System.err.println("Parameter attributes:");
            System.err.println(parameters.getWindowSize());
        }
    }
}
