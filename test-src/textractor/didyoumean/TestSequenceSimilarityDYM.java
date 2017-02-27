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

package textractor.didyoumean;

import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.scoredresult.ScoredResult;
import textractor.tools.BuildDocumentIndexFromTextDocuments;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabien Campagne
 *         Date: Oct 26, 2006
 *         Time: 6:04:40 PM
 */
public class TestSequenceSimilarityDYM extends TestCase {
    private DidYouMeanI dym;
    private DidYouMeanI dym2;
    private DocumentIndexManager docManager;

    protected void setUp() throws Exception {
        super.setUp();
        dym = new SequenceSimilarityDYM(null);

        final String basename = "index/sequence-dym-test";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);

        // Create a corpus with
        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add("A C D E F I J AAA");
        indexBuilder.index(textCollection);
        docManager =
                new DocumentIndexManager(basename);

        dym2 = new SequenceSimilarityDYM(docManager); // removes terms not in corpus.
    }


    public void testSingleCharTerm() throws ConfigurationException, QueryParserException, IOException, ParseException, ClassNotFoundException, QueryBuilderVisitorException {

        String term = "A";
        List<ScoredResult>  result;
        result = dym.suggestRelated(term, 0);
        assertNotNull(result);
        assertEquals(23 - 1, result.size());
    }

    public void testSingleCharTermFiltering() throws ConfigurationException, QueryParserException, IOException, ParseException, ClassNotFoundException, QueryBuilderVisitorException {

        String term = "A";
        List<ScoredResult>  result;
        result = dym2.suggestRelated(term, 0);
        assertNotNull(result);
        assertEquals(5, result.size());
    }

    public void testTwoCharTerm() throws ConfigurationException, QueryParserException, IOException, ParseException, ClassNotFoundException, QueryBuilderVisitorException {

        String term = "AA";
        List<ScoredResult>   result = dym.suggestRelated(term, 0);
        assertNotNull(result);
        assertEquals(506, result.size());
    }

    public void testThreeCharTerm() throws ConfigurationException, QueryParserException, IOException, ParseException, ClassNotFoundException, QueryBuilderVisitorException {

        String term = "AWY";
        List<ScoredResult>    result = dym.suggestRelated(term, 0);
        assertNotNull(result);
        assertEquals(11639, result.size());
    }

    public void testScoringMatrix() throws IOException {
        ScoringMatrix matrix = new ScoringMatrix(new FileReader("data/matrices/BLOSUM62"));
        assertEquals(-1, matrix.score('A', 'E'));
        assertEquals(4, matrix.score('A', 'A'));
        assertEquals(-3, matrix.score('Q', 'C'));

        matrix = new ScoringMatrix(new FileReader("data/matrices/PAM30"));
        assertEquals(-2, matrix.score('A', 'E'));
        assertEquals(6, matrix.score('A', 'A'));
        assertEquals(-14, matrix.score('Q', 'C'));
    }

    public void testThreeCharTermWithMatrix() throws ConfigurationException, QueryParserException, IOException, ParseException, ClassNotFoundException, QueryBuilderVisitorException {

        String term = "AWY";
        SequenceSimilarityDYM dym = new SequenceSimilarityDYM();
        dym.setScoringMatrix(new ScoringMatrix(new FileReader("data/matrices/PAM30")));
        List<ScoredResult>    result = dym.suggestRelated(term, 0);
        assertNotNull(result);
        assertEquals(11639, result.size());
    }

    public void testThreeCharTermWithMatrixAndFiltering() throws ConfigurationException, QueryParserException, IOException, ParseException, ClassNotFoundException, QueryBuilderVisitorException {

        String term = "AWY";
        SequenceSimilarityDYM dym = new SequenceSimilarityDYM(docManager);
        dym.setScoringMatrix(new ScoringMatrix(new FileReader("data/matrices/PAM30")));
        List<ScoredResult>    result = dym.suggestRelated(term, 0);
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
