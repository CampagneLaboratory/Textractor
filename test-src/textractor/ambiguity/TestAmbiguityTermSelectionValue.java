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

package textractor.ambiguity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.database.TermDocumentPositions;
import textractor.mg4j.docstore.TermDocumentFrequencyReader;
import textractor.pseudorelevance.QueryTerm;
import textractor.pseudorelevance.TermSelectionPseudoRelevanceFeedback;
import textractor.tfidf.TfIdfCalculator;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Test Query Expansion strategies.
 *
 * @author Fabien Campagne
 *         Date: Nov 7, 2006
 *         Time: 6:50:48 PM
 *         To change this template use File | Settings | File Templates.
 */
public class TestAmbiguityTermSelectionValue extends TestCase {
    private static final Log LOG =
            LogFactory.getLog(TestAmbiguityTermSelectionValue.class);
    private static final String BASENAME = "dataset-a-index/index";
    private static final String OUTPUT_FILENAME =
            "test-results/test-ambiguity-term-selection-value.xml";

    public void testPass(){
        // just to have a test in this class.
    }
    // TODO enable these tests against an appropriate corpus. (ambiguity is sentence-level and these tests need document-level).
    public void TestTermSelectionValue() throws IOException, NoSuchMethodException, IllegalAccessException, ConfigurationException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        DocumentIndexManager docManager = new DocumentIndexManager(BASENAME);
        TermDocumentFrequencyReader tfReader = new TermDocumentFrequencyReader(docManager);
        TfIdfCalculator calculator = new TfIdfCalculator(tfReader, docManager);
        String[] query = new String[]{"troponin", "T"};
        int documents[] = null;
        TermDocumentPositions positions = docManager.queryAndExactOrder(query);
        documents = positions.getDocuments();
        int[] numDocForTerm = calculator.allocateNumDocForTerm();
        IntArrayList topDocuments = new IntArrayList();
        int R = 10;
        for (int i = 0; i < R; i++) {
            topDocuments.add(documents[i]);
        }

        float tsv[] = calculator.termSelectionValue(topDocuments.toIntArray(), numDocForTerm);
        assertNotNull(tsv);
        for (float score : tsv) {
            if (score != 0) System.out.println("score: " + score);
        }
        int[] bestIndices = calculator.bestScoresFavorSmall(tsv, 10);
        assertNotNull(bestIndices);
        for (int bestIndex : bestIndices) {

            System.out.println("term: " + calculator.getTerm(bestIndex) + " score: " + tsv[bestIndex]);

        }
        System.out.println("tsv: " + tsv);
    }
    // TODO enable these tests against an appropriate corpus (ambiguity is sentence-level and these tests need document-level).
    public void TestTermSelectionValueSimpler() throws IOException, NoSuchMethodException, IllegalAccessException, ConfigurationException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        DocumentIndexManager docManager = new DocumentIndexManager(BASENAME);
        TermDocumentFrequencyReader tfReader = new TermDocumentFrequencyReader(docManager);
        TfIdfCalculator calculator = new TfIdfCalculator(tfReader, docManager);
        String[] query = new String[]{"troponin", "T"};
        int documents[] = null;
        TermDocumentPositions positions = docManager.queryAndExactOrder(query);
        documents = positions.getDocuments();
        IntArrayList topDocuments = new IntArrayList();
        int R = 10;
        for (int i = 0; i < R; i++) {
            topDocuments.add(documents[i]);
        }
        TermSelectionPseudoRelevanceFeedback expander = new TermSelectionPseudoRelevanceFeedback(calculator);
        List<QueryTerm> newTerms = expander.expand(topDocuments.toIntArray(), getQueryTerms(query), 10);
        assertNotNull(newTerms);
        assertEquals(2, newTerms.size());
        assertEquals("troponin", newTerms.get(0).getTerm());
        assertEquals("T", newTerms.get(1).getTerm());
    }

    private QueryTerm[] getQueryTerms(String[] query) {
        QueryTerm[] result = new QueryTerm[query.length];
        int i = 0;
        for (String term : query) {
            result[i++] = new QueryTerm(term, 1);
        }
        return result;
    }
}
