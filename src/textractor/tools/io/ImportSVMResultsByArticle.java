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
import textractor.datamodel.Article;
import textractor.datamodel.TermOccurrence;
import textractor.learning.AnnotationFormatWriter;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Jun 4, 2004
 * Time: 5:14:31 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ImportSVMResultsByArticle extends ImportSVMResults {
    private static final Log LOG =
        LogFactory.getLog(ImportSVMResultsByArticle.class);
    private BufferedReader breader;

    public static void main(final String[] args) throws IOException, TextractorDatabaseException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final textractor.tools.io.ImportSVMResultsByArticle importResults = new textractor.tools.io.ImportSVMResultsByArticle();
        importResults.process(args);
    }

    @Override
    protected void getParameters(final String[] args) throws IOException {
        super.getParameters(args);
    }

    @Override
    protected void process(final String[] args) throws TextractorDatabaseException, IOException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        getParameters(args);
        super.process(args);
    }

    /**
     * Reads in SVM results, gets relevant Sentence, finds the term being searched,
     * and creates a TermPredictionStatistics object in the relevant Article.
     * If already created, increments the prediction count, if predicted to be a protein name.
     *
     * @param reader  Used to read the svm classification results.
     * @throws IOException
     * @throws TextractorDatabaseException
     */
    @Override
    public void importSVM(final Reader reader) throws IOException {
        breader = new BufferedReader(reader);
        AnnotationFormatWriter sentenceWriter = null;
        AnnotationFormatWriter nonSentenceWriter = null;
        if (this.getPrintSentences()) {
            sentenceWriter = new AnnotationFormatWriter(docManager, new FileWriter("sentences.out"), true);
            nonSentenceWriter = new AnnotationFormatWriter(docManager, new FileWriter("non-sentences.out"), true);
        }
        parseByArticle(LOG);
        if (this.getPrintSentences()) {
            sentenceWriter.flush();
            nonSentenceWriter.flush();
        }
       // System.err.println("Leaving importSVM");
    }

    @Override
    protected int toDoForEachArticle(final Article article, final int[] targetTermOccurrenceIndex, int recordCounter) throws IOException {
        String line;
        StringTokenizer st;
        double probability;
        for (int i = 0; i < targetTermOccurrenceIndex.length; i++) {
            if (i < article.getNumMostFrequentTerms()) {
                final TermOccurrence mostFrequentTerm = article.getTermOccurrence(targetTermOccurrenceIndex[i]);
                if (exportCategory==toExportListedTerms){
                    if(positiveNames.contains(mostFrequentTerm.getTerm())
                        ||negativeNames.contains(mostFrequentTerm.getTerm())){

                    }else{
                        return recordCounter;
                    }
                }
                for (int termOccurrenceInArticle=0; termOccurrenceInArticle<mostFrequentTerm.getCount();termOccurrenceInArticle++){
                    if ((line = breader.readLine()) != null){
                        recordCounter++;
                        st = new StringTokenizer(line);
                        probability = Double.parseDouble(st.nextToken());
                        incrementTermCounter(article, mostFrequentTerm.getIndexedTerm(), probability);
                    }else{
                        LOG.error(article.getFilename()+" "+article.getArticleNumber()+"not enough import data");
                    }
                }
            } else {
                LOG.error("not enough frequent term in article." + article.getFilename());
            }
        }
        return recordCounter;
    }
}
