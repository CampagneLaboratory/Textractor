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

package textractor.mg4j.docstore;

import junit.framework.TestCase;
import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Chain;
import org.apache.commons.chain.impl.CatalogBase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.math.IntRange;
import textractor.chain.AbstractSentenceTransformer;
import textractor.chain.ArticleSentencesPair;
import textractor.chain.docstore.DocumentStoreBuilder;
import textractor.chain.indexer.Indexer;
import textractor.chain.loader.Html2TextArticleLoader;
import textractor.database.DocumentIndexManager;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.event.sentence.SentenceProcessingCompleteEvent;
import textractor.event.sentence.SentenceProcessingCompleteListener;
import textractor.mg4j.index.PositionedTerm;
import textractor.sentence.SentenceProcessingException;
import textractor.tools.chain.ChainExecutor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestParagraphPositions extends TestCase {
    /**
     * Article under test.
     */
    private Article article;

    /**
     * Sentences under test.
     */
    private Collection<Sentence> sentences;

    /**
     * Indicates that the indexing process has finished.
     */
    private CountDownLatch indexingComplete = new CountDownLatch(1);

    /**
     * Indicates that the docstore process has finished.
     */
    private CountDownLatch docstoreComplete = new CountDownLatch(1);

    private final PositionedTerm[][] expectedPositionedTerms = {{
            // John
            new PositionedTerm(14, new IntRange(0x1B, 0x1E), "John"),
            // ,
            new PositionedTerm(0, new IntRange(0x1F), ","),
            // Paul
            new PositionedTerm(17, new IntRange(0x21, 0x24), "Paul"),
            // ,
            new PositionedTerm(0, new IntRange(0x25), ","),
            // George
            new PositionedTerm(9, new IntRange(0x27, 0x2C), "George"),
            // and
            new PositionedTerm(5, new IntRange(0x2E, 0x30), "and"),
            // Ringo
            new PositionedTerm(22, new IntRange(0x32, 0x36), "Ringo")
    }, {
            // It
            new PositionedTerm(2, new IntRange(0x58, 0x59), "It"),
            // was
            new PositionedTerm(32, new IntRange(0x5B, 0x5D), "was"),
            // 20
            new PositionedTerm(1, new IntRange(0x62, 0x63), "20"),
            // years
            new PositionedTerm(33, new IntRange(0x65, 0x69), "years"),
            // ago
            new PositionedTerm(4, new IntRange(0x6B, 0x6D), "ago"),
            // today
            new PositionedTerm(29, new IntRange(0x6F, 0x73), "today")
    }, {
            // That
            new PositionedTerm(26, new IntRange(0x89, 0x8C), "That"),
            // Sgt.
            new PositionedTerm(23, new IntRange(0x91, 0x94), "Sgt."),
            // Pepper
            new PositionedTerm(18, new IntRange(0x96, 0x9B), "Pepper"),
            // told
            new PositionedTerm(30, new IntRange(0x9D, 0xA0), "told"),
            // his
            new PositionedTerm(12, new IntRange(0xA6, 0xA8), "his"),
            // band
            new PositionedTerm(6, new IntRange(0xAA, 0xAD), "band"),
            // to
            new PositionedTerm(28, new IntRange(0xAF, 0xB0), "to"),
            // play
            new PositionedTerm(19, new IntRange(0xB2, 0xB5), "play")
    }, {
            // They
            new PositionedTerm(27, new IntRange(0xC7, 0xCA), "They"),
            // ve
            new PositionedTerm(31, new IntRange(0xCC, 0xCD), "ve"),
            // been
            new PositionedTerm(7, new IntRange(0xCF, 0xD2), "been"),
            // going
            new PositionedTerm(10, new IntRange(0xD4, 0xD8), "going"),
            // in
            new PositionedTerm(13, new IntRange(0xDA, 0xDB), "in"),
            // and
            new PositionedTerm(5, new IntRange(0xDD, 0xDF), "and"),
            // out
            new PositionedTerm(16, new IntRange(0xE1, 0xE3), "out"),
            // of
            new PositionedTerm(15, new IntRange(0xE5, 0xE6), "of"),
            // style
            new PositionedTerm(25, new IntRange(0xE8, 0xEC), "style")
    }, {
            // But
            new PositionedTerm(8, new IntRange(0xFE, 0x100), "But"),
            // they
            new PositionedTerm(27, new IntRange(0x102, 0x105), "they"),
            // re
            new PositionedTerm(21, new IntRange(0x107, 0x108), "re"),
            // guaranteed
            new PositionedTerm(11, new IntRange(0x10A, 0x113), "guaranteed"),
            // to
            new PositionedTerm(28, new IntRange(0x115, 0x116), "to"),
            // raise
            new PositionedTerm(20, new IntRange(0x118, 0x11C), "raise"),
            // a
            new PositionedTerm(3, new IntRange(0x11E), "a"),
            // smile
            new PositionedTerm(24, new IntRange(0x120, 0x124), "smile")
    }};

    public TestParagraphPositions() throws Exception {
        super();
        // create a simple loader
        final Html2TextArticleLoader loader = new Html2TextArticleLoader();
        loader.setFile("data/test/html/medium-with-paragraphs.html");
        loader.setSplitterClass(textractor.tools.ParagraphSplitter.class.getName());
        loader.setAppendSentencesInOneDocument(false);
        loader.setParagraphBoundary(" paragraphboundary ");
        // add a node that will store the sentences created
        final Chain filter = new TestFilter();
        loader.addCommand(filter);

        // add an indexer
        final Indexer indexer = new Indexer();
        indexer.setBasename("index/paragraph-position-test");
        indexer.addSentenceProcessingCompleteListener(new SentenceProcessingCompleteListener() {
            public void processingComplete(final SentenceProcessingCompleteEvent event) {
                indexingComplete.countDown();
            }
        });
        filter.addCommand(indexer);

        // and execute it
        final Catalog indexerCatalog = new CatalogBase();
        indexerCatalog.addCommand("PositionTestIndexer", loader);
        final ChainExecutor executor = new ChainExecutor(indexerCatalog);
        executor.execute();

        // wait for indexer to finish - but not forever
        indexingComplete.await(60, TimeUnit.SECONDS);

        // create a simple loader
        final Html2TextArticleLoader loader2 = new Html2TextArticleLoader();
        loader2.setFile("data/test/html/medium-with-paragraphs.html");
        loader2.setSplitterClass(textractor.tools.ParagraphSplitter.class.getName());
        loader2.setAppendSentencesInOneDocument(false);
        loader2.setParagraphBoundary(" paragraphboundary ");

        // add a document store builder
        final DocumentStoreBuilder documentStoreBuilder = new DocumentStoreBuilder();
        documentStoreBuilder.setBasename("index/paragraph-position-test");
        documentStoreBuilder.addSentenceProcessingCompleteListener(new SentenceProcessingCompleteListener() {
            public void processingComplete(final SentenceProcessingCompleteEvent event) {
                docstoreComplete.countDown();
            }
        });
        loader2.addCommand(documentStoreBuilder);

        // and execute it
        final Catalog docstoreCatalog = new CatalogBase();
        docstoreCatalog.addCommand("PositionTestDocstoreBuilder", loader2);
        final ChainExecutor executor2 = new ChainExecutor(docstoreCatalog);
        executor2.execute();

        // wait for indexer to finish - but not forever
        docstoreComplete.await(60, TimeUnit.SECONDS);
    }

    /**
     * Validates that indexing with postion information stores the
     * position data correctly.
     */
    public void testExtractTermsWithPositions() throws NoSuchMethodException,
            IllegalAccessException, ConfigurationException, IOException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        assertNotNull("Article cannont be null", article);
        assertNotNull("Sentence collection cannont be null", sentences);
        assertEquals("There should be 5 sentences", 5, sentences.size());

        // TODO - why is there a leading and trailing space?
        final String[] expectedText = {
                " John, Paul, George and Ringo",
                "It was 20 years ago today",
                "That Sgt. Pepper told his band to play",
                "They've been going in and out of style",
                "But they're guaranteed to raise a smile "
        };

        final DocumentIndexManager manager =
                new DocumentIndexManager("index/paragraph-position-test");
        int i = 0;
        for (Sentence sentence : sentences) {
            final String actualText = sentence.getText();
            assertEquals("Text doesn't match", expectedText[i], actualText);

            int index = 0;
            final List<PositionedTerm> terms = manager.extractTerms(sentence);
            assertEquals("Number of terms do not match",
                    expectedPositionedTerms[i].length, terms.size());
            for (PositionedTerm expectedPositionedTerm : expectedPositionedTerms[i]) {
                final PositionedTerm actualTerm = terms.get(index);
                assertEquals("Incorrect term at index (" + i + ", "
                        + index + ")", expectedPositionedTerm, actualTerm);
                index++;
            }
            i++;
        }
        manager.close();
    }

    /**
     * Validates that the document store postion information is
     * accessed properly.
     */
    public void testDocumentStorePositions() throws NoSuchMethodException,
            IllegalAccessException, ConfigurationException, IOException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final DocumentStoreReader reader =
                new DocumentStoreReader(new DocumentIndexManager("index/paragraph-position-test"));
        for (int i = 0; i <= 1; i++) {
            final List<IntRange> actualRanges = reader.positions(i);
            int index = 0;
            for (PositionedTerm expectedPositionedTerm : expectedPositionedTerms[i]) {
                final IntRange expectedRange = expectedPositionedTerm.getRange();
                final IntRange actualRange = actualRanges.get(index);
                assertEquals("Incorrect range at index " + index,
                        expectedRange, actualRange);
                index++;
            }
        }
        reader.close();
    }

    private class TestFilter extends AbstractSentenceTransformer {
        /**
         * Get the number of articles processed so far.
         *
         * @return The number of articles processed so far
         */
        public int getNumberOfArticlesProcessed() {
            // not needed for this test
            return 0;
        }

        /**
         * Get the number of sentences processed so far.
         *
         * @return The number of sentences processed so far
         */
        public int getNumberOfSentencesProcessed() {
            // not needed for this test
            return 0;
        }

        /**
         * Transform a ArticleSentencesPair into a new ArticleSentencesPair.
         *
         * @param pair The ArticleSentencesPair to be transformed.
         * @return A (possibly null) ArticleSentencesPair based on the original
         * @throws textractor.sentence.SentenceProcessingException If there
         * was an error condition in the textractor sentence processing pipeline
         */
        public ArticleSentencesPair transform(final ArticleSentencesPair pair)
                throws SentenceProcessingException {
            article = pair.article;
            sentences = pair.sentences;
            return pair;
        }
    }
}
