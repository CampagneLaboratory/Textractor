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
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.Article;
import textractor.datamodel.annotation.AnnotationSource;
import textractor.datamodel.annotation.DoubleTermAnnotation;
import textractor.datamodel.annotation.SingleTermAnnotation;
import textractor.learning.AnnotationFormatWriter;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

public final class ImportSVMResultsByAnnotation extends ImportSVMResults {
    private int batchId;

    public ImportSVMResultsByAnnotation() {
        super();
    }

    public ImportSVMResultsByAnnotation(final DbManager dbm, final int newBatchID) {
        super();
        this.dbm = dbm;
        batchId = newBatchID;
    }


    public static void main(final String[] args) throws IOException, TextractorDatabaseException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final ImportSVMResultsByAnnotation importResults =
                new ImportSVMResultsByAnnotation();
        importResults.process(args);
    }

    @Override
    protected void getParameters(final String[] args) throws IOException {
        super.getParameters(args);
        batchId = CLI.getIntOption(args, "-batch", INT_BATCH_NOT_SET);
        if (batchId == INT_BATCH_NOT_SET) {
            System.err.println("You must provide a batch number (-batch).");
            System.exit(1);
        }
    }

    @Override
    protected void process(final String[] args) throws TextractorDatabaseException, IOException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        getParameters(args);
        super.process(args);

    }

    public void setAnnotations(final Collection anns) {
    }

    /**
     * Reads in SVM results, gets relevant Sentence, finds the term being
     * searched, and creates a TermPredictionStatistics object in the relevant
     * Article. If already created, increments the prediction count, if
     * predicted to be a protein name.
     *
     * @param reader Used to read the svm classification results.
     * @throws IOException
     * @throws TextractorDatabaseException
     */
    @Override
    public void importSVM(final Reader reader)
            throws IOException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        int lowerBound = -1;
        final int chunkSize = 10000;
        final DocumentIndexManager docManager = new DocumentIndexManager(dbm.getTextractorManager().getInfo().getCaseSensitiveIndexBasename());
        final Collection annotations = dbm.getTextractorManager().getUnannotatedAnnotationsInBatch(batchId, lowerBound, chunkSize);
        final BufferedReader breader = new BufferedReader(reader);
        Iterator annotation_it = annotations.iterator();
        String line;
        StringTokenizer st;
        double probability;
        Article article;
        int[] terms;
        AnnotationFormatWriter sentenceWriter = null;
        AnnotationFormatWriter nonSentenceWriter = null;
        if (this.getPrintSentences()) {
            sentenceWriter = new AnnotationFormatWriter(docManager,new FileWriter("sentences.out"), true);
            nonSentenceWriter = new AnnotationFormatWriter(docManager,new FileWriter("non-sentences.out"), true);
        }
        while ((line = breader.readLine()) != null) {
            st = new StringTokenizer(line);
            probability = Double.parseDouble(st.nextToken());
            if (!annotation_it.hasNext()) {
                dbm.commitTxn();
                dbm.beginTxn();
                lowerBound += chunkSize;
                annotation_it = dbm.getTextractorManager().getUnannotatedAnnotationsInBatch(batchId, lowerBound, chunkSize).iterator();
                if (!annotation_it.hasNext()) {
                    System.err.println("There are more lines in the input file but no more annotations to update in the database.");
                    break;
                }
            }
            final AnnotationSource ann = (AnnotationSource) annotation_it.next();

            article = ann.getSentence().getArticle();
            ann.createIndexedTerms(docManager);
            final int[] indexTerm= docManager.extractTerms(new MutableString((ann.getTerm(AnnotationSource.FIRST_TERM)).getTermText()));
            terms = ann.getIndexedTerms();
            if (terms.length != 0) {
                incrementTermCounter(article, indexTerm, probability);
                if (getPrintSentences()) {
                    if (probability>0) {
                        if (ann instanceof SingleTermAnnotation) {
			    sentenceWriter.writeAnnotation((SingleTermAnnotation) ann);
			} else {
			    sentenceWriter.writeAnnotation((DoubleTermAnnotation) ann);
			}
                    } else {
                        if (ann instanceof SingleTermAnnotation) {
			    nonSentenceWriter.writeAnnotation((SingleTermAnnotation) ann);
			} else {
			    nonSentenceWriter.writeAnnotation((DoubleTermAnnotation) ann);
			}
                    }
                }
            } else {
		throw new InternalError("Indexed term must not have length zero.");
	    }

        }
        if (this.getPrintSentences()) {
            sentenceWriter.flush();
            nonSentenceWriter.flush();
        }
        docManager.close();
       // System.err.println("Leaving importSVM");
    }

}
