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

import com.martiansoftware.jsap.JSAPException;
import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.scoredresult.ScoredResult;
import textractor.scoredresult.ScoredResultComparator;
import textractor.tools.BuildDocumentIndexFromTextDocuments;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * Test the DidYouMean feature.
 * Date: Nov 8, 2005
 * Time: 2:38:50 PM
 */
public class TestDidYouMean extends TestCase {
    /** Used to log informational and debug messages. */
    private static final Log LOG = LogFactory.getLog(TestDidYouMean.class);
    private static final String BASENAME = "index/didyoumean-test";
    private static final float SCORE_THRESHOLD = 0.02f;

    @Override
    protected void setUp() throws Exception {
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(BASENAME);

        final String[] documents = {
                "Hello, this is the text to index, first document",
                "second estrogens, estrogenic, document, in a string buffer",
                "third, hello a mutable string doccie",
                "Hello, document document document HELLO, hello",
                "estrogenic, estrogenemia, estrogens, estrogenically, estrogen"
        };

        indexBuilder.index(documents);
    }

    public void testWriteStore() throws IllegalAccessException,
            NoSuchMethodException, ConfigurationException, IOException,
            InvocationTargetException, ClassNotFoundException,
            InstantiationException, URISyntaxException, JSAPException,
            QueryParserException, ParseException, QueryBuilderVisitorException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);

        final DidYouMeanIndexer indexer = new DidYouMeanIndexer(docmanager);
        indexer.index(1);

        final DidYouMeanI searchTool = new DidYouMean(docmanager);
        List<ScoredResult> suggestions =
            searchTool.suggest("helo", SCORE_THRESHOLD);
        assertEquals("hello", suggestions.get(0).getTerm());

        suggestions = searchTool.suggest("documents", SCORE_THRESHOLD);
        assertEquals("document", suggestions.get(0).getTerm());
        assertEquals(1, suggestions.size());
        docmanager.close();
    }

    public void testSorting() {
        final SimilarityScorer scorer = new SimilarityScorer();
        final String targetString = "estrogen";
        final String[] diffStrings = {
                "estrogen", "estrogenic", "hedgehog",
                "oestrogen", "estrogens", "wildcard"
        };

        final ScoredResult[] suggestions =
            new ScoredResult[diffStrings.length];
        double score;
        for (int n = 0; n < diffStrings.length; n++) {
            score = scorer.getSimilarity(targetString, diffStrings[n]);
            final ScoredResult dymResult =
                new ScoredResult(diffStrings[n], score);
            LOG.debug(dymResult.getTerm() + ": " + dymResult.getScore());
            suggestions[n] = dymResult;
        }

        Arrays.sort(suggestions, new ScoredResultComparator());

        assertEquals("estrogen", suggestions[0].getTerm());
        assertEquals("oestrogen", suggestions[1].getTerm());
        assertEquals("estrogens", suggestions[2].getTerm());
        assertEquals("estrogenic", suggestions[3].getTerm());
        assertEquals("hedgehog", suggestions[4].getTerm());
        assertEquals("wildcard", suggestions[5].getTerm());

    }

    public void testMultipleSuggestions() throws IllegalAccessException,
            NoSuchMethodException, ConfigurationException, IOException,
            InvocationTargetException, ClassNotFoundException,
            InstantiationException, URISyntaxException, JSAPException,
            QueryParserException, ParseException, QueryBuilderVisitorException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);
        final DidYouMeanIndexer indexer = new DidYouMeanIndexer(docmanager);
        indexer.index(0);

        final DidYouMeanI searchTool = new DidYouMean(docmanager);
        final List<ScoredResult> suggestions =
            searchTool.suggest("document", SCORE_THRESHOLD);
        assertEquals(1, suggestions.size());
        docmanager.close();
    }

    public void testDidYouMeanScoreOrdering() throws IllegalAccessException,
            NoSuchMethodException, ConfigurationException, IOException,
            InvocationTargetException, ClassNotFoundException,
            InstantiationException, URISyntaxException, JSAPException,
            QueryParserException, ParseException, QueryBuilderVisitorException {
        final DocumentIndexManager docmanager =
            new DocumentIndexManager(BASENAME);

        final DidYouMeanIndexer indexer = new DidYouMeanIndexer(docmanager);
        indexer.index(0);

        final DidYouMeanI searchTool = new DidYouMean(docmanager);
        final List<ScoredResult> suggestions =
            searchTool.suggest("estrogen", SCORE_THRESHOLD);
        assertEquals("estrogens", suggestions.get(0).getTerm());
        assertEquals("estrogenic", suggestions.get(1).getTerm());
        assertEquals("estrogenemia", suggestions.get(2).getTerm());
        assertEquals("estrogenically", suggestions.get(3).getTerm());
        assertEquals(4, suggestions.size());
        docmanager.close();
    }

    public void testDidYouMeanDocument() {
        DidYouMeanDocument doc = new DidYouMeanDocument("word");
        assertEquals("word", doc.getContent(DidYouMeanDocumentFactory.WORD).trim().toString());
        assertEquals("word",
                doc.getContent(DidYouMeanDocumentFactory.FOUR_START).trim().toString());
        assertEquals("word",
                doc.getContent(DidYouMeanDocumentFactory.FOUR_END).trim().toString());
        assertEquals("wor ord",
                doc.getContent(DidYouMeanDocumentFactory.GRAM3).trim().toString());
        assertEquals("word",
                doc.getContent(DidYouMeanDocumentFactory.GRAM4).trim().toString());
        assertEquals("wor",
                doc.getContent(DidYouMeanDocumentFactory.THREE_START).trim().toString());
        assertEquals("ord",
                doc.getContent(DidYouMeanDocumentFactory.THREE_END).trim().toString());

        doc = new DidYouMeanDocument("abracadabra");
        assertEquals("abracadabra",
                doc.getContent(DidYouMeanDocumentFactory.WORD).trim().toString());
        assertEquals("abra",
                doc.getContent(DidYouMeanDocumentFactory.FOUR_START).trim().toString());
        assertEquals("abra",
                doc.getContent(DidYouMeanDocumentFactory.FOUR_END).trim().toString());
        assertEquals("abr bra rac aca cad ada dab abr bra",
                doc.getContent(DidYouMeanDocumentFactory.GRAM3).trim().toString());
        assertEquals("abra brac raca acad cada adab dabr abra",
                doc.getContent(DidYouMeanDocumentFactory.GRAM4).trim().toString());
        assertEquals("abr",
                doc.getContent(DidYouMeanDocumentFactory.THREE_START).trim().toString());
        assertEquals("bra",
                doc.getContent(DidYouMeanDocumentFactory.THREE_END).trim().toString());
    }

    public void testSuggestRelated() throws IllegalAccessException,
            NoSuchMethodException, ConfigurationException, IOException,
            InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException, JSAPException, QueryParserException, ParseException, QueryBuilderVisitorException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);

        final DidYouMeanIndexer indexer = new DidYouMeanIndexer(docmanager);
        indexer.index(0);

        final DidYouMean searchTool = new DidYouMean(docmanager);

        final List<ScoredResult> suggestions =
            searchTool.suggestRelated2("estrogenically", 1.29E-4f);
        int count = 0;
        for (final ScoredResult ScoredResult : suggestions) {
            assertNotNull(ScoredResult);
            count++;
        }
        assertEquals(3, count);
        docmanager.close();

    }
     public void testSuggestPaiceHusk() throws IllegalAccessException,
            NoSuchMethodException, ConfigurationException, IOException,
            InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException, JSAPException, QueryParserException, ParseException, QueryBuilderVisitorException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);

        final DidYouMeanIndexer indexer = new DidYouMeanIndexer(docmanager);
        indexer.index(0);

        final DidYouMeanI searchTool = new DidYouMean(docmanager);

        final List<ScoredResult> suggestions =
            searchTool.suggestPaiceHusk("estrogenic", 1.29E-4f);
        int count = 0;
        for (final ScoredResult ScoredResult : suggestions) {
            assertNotNull(ScoredResult);
            count++;
        }
        assertEquals(3, count);
        docmanager.close();

    }
}
