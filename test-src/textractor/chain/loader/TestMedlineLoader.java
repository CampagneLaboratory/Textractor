/*
 * Copyright (C) 2009 Institute for Computational Biomedicine,
 *                    Weill Medical College of Cornell University
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

package textractor.chain.loader;

import junit.framework.TestCase;
import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.impl.CatalogBase;
import org.apache.commons.io.FileUtils;
import textractor.chain.AbstractSentenceConsumer;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.sentence.SentenceProcessingException;
import textractor.tools.chain.ChainExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMedlineLoader extends TestCase {
    private final List<Long> pmids = new ArrayList<Long>();

    public void setUp() throws Exception {
        super.setUp();
        pmids.clear();
    }

    /**
     * Validate that a medline sample file can be loaded properly and that
     * retractions are not included in the final results.
     * @throws Exception if there is a problem executing the chain
     */
    public void testMedlineSampleWithRetractions() throws Exception {
        final String filename = "data/2011-medline-sample/medsamp2011.xml";
        // create a simple loader
        final PubmedArticleLoader loader = new PubmedArticleLoader();
        loader.setFile(filename);
        // add a simple consumer
        loader.addCommand(new TestConsumer());

        // and execute it
        final Catalog catalog = new CatalogBase();
        catalog.addCommand("MedlineSampleLoader", loader);
        final ChainExecutor chainExecutor = new ChainExecutor(catalog);
        chainExecutor.execute();

        // check the results
        assertTrue("There should be some pmids after parsing", pmids.size() > 0);
        assertTrue("PMID 8655018 should be included", pmids.contains(8655018L));
        assertTrue("PMID 973217 should be included", pmids.contains(973217L));

        // The following articles should have been retracted
        assertFalse("PMID 11543891 should be retracted", pmids.contains(11543891L));
        assertFalse("PMID 11600210 should be retracted", pmids.contains(11600210L));
        assertFalse("PMID 9634358 should be retracted", pmids.contains(9634358L));
        // 156 with 3 RetractionIn
        assertEquals("There should be a total of 153 articles", 153, pmids.size());
    }

    /**
     * Validate that a medline sample file can be loaded properly and that
     * retracted articles are kept if the option is given.
     * @throws Exception if there is a problem executing the chain
     */
    public void testMedlineSampleWithoutRetractions() throws Exception {
        final String filename = "data/2011-medline-sample/medsamp2011.xml";
        // create a simple loader
        final PubmedArticleLoader loader = new PubmedArticleLoader();
        loader.setFile(filename);
        loader.setProcessRetractions(false);
        // add a simple consumer
        loader.addCommand(new TestConsumer());

        // and execute it
        final Catalog catalog = new CatalogBase();
        catalog.addCommand("MedlineSampleLoader", loader);
        final ChainExecutor chainExecutor = new ChainExecutor(catalog);
        chainExecutor.execute();

        // check the results
        assertTrue("There should be some pmids after parsing", pmids.size() > 0);
        assertTrue("PMID 8655018 should be included", pmids.contains(8655018L));
        assertTrue("PMID 973217 should be included", pmids.contains(973217L));

        assertTrue("PMID 11543891 should not have been retracted", pmids.contains(11543891L));
        assertTrue("PMID 11600210 should not have been retracted", pmids.contains(11600210L));
        assertTrue("PMID 9634358 should not have been retracted", pmids.contains(9634358L));

        assertEquals("There should be a total of 156 articles", 156, pmids.size());
    }

    public void testMultipleFileProcessing() throws Exception {
        final File[] testFiles = {
            new File("data/pubmed/retraction/993.xml"),
            new File("data/pubmed/retraction/239707.xml")
        };
        final File testFileList = File.createTempFile("medline", ".txt");
        FileUtils.writeLines(testFileList, Arrays.asList(testFiles));

        // create a simple loader
        final PubmedArticleLoader loader = new PubmedArticleLoader();
        loader.setList(testFileList.getAbsolutePath());
        // add a simple consumer
        loader.addCommand(new TestConsumer());

        // and execute it
        final Catalog catalog = new CatalogBase();
        catalog.addCommand("MedlineSampleLoader", loader);
        final ChainExecutor chainExecutor = new ChainExecutor(catalog);
        chainExecutor.execute();

        // check the results
        assertTrue("There should be some pmids after parsing", pmids.size() > 0);
        assertTrue("PMID 993 should be included", pmids.contains(993L));
        assertTrue("PMID 239707 should be included", pmids.contains(239707L));
        assertEquals("There should no other pmids after parsing", 2, pmids.size());
    }

    public void testMultipleRetractionProcessing() throws Exception {
        final File[] testFiles = {
            new File("data/pubmed/retraction/993.xml"),
            new File("data/pubmed/retraction/239707.xml"),
            new File("data/pubmed/retraction/multipleRetractions.xml")
        };
        final File testFileList = File.createTempFile("medline", ".txt");
        FileUtils.writeLines(testFileList, Arrays.asList(testFiles));

        // create a simple loader
        final PubmedArticleLoader loader = new PubmedArticleLoader();
        loader.setList(testFileList.getAbsolutePath());
        // add a simple consumer
        loader.addCommand(new TestConsumer());

        // and execute it
        final Catalog catalog = new CatalogBase();
        catalog.addCommand("MedlineSampleLoader", loader);
        final ChainExecutor chainExecutor = new ChainExecutor(catalog);
        chainExecutor.execute();

        // check the results
        assertTrue("There should be some pmids after parsing", pmids.size() > 0);
        assertFalse("PMID 993 should not be included", pmids.contains(993L));
        assertFalse("PMID 239707 should not be included", pmids.contains(239707L));
    }

    private class TestConsumer extends AbstractSentenceConsumer {
        /** Number of articles processed so far. */
        private final AtomicInteger numberOfArticlesProcessed = new AtomicInteger();

        /** Number of sentences processed so far. */
        private final AtomicInteger numberOfSentencesProcessed = new AtomicInteger();

        /**
         * Get the number of articles processed so far.
         *
         * @return The number of articles processed so far
         */
        public int getNumberOfArticlesProcessed() {
            // not needed for this test
            return numberOfArticlesProcessed.get();
        }

        /**
         * Get the number of sentences processed so far.
         *
         * @return The number of sentences processed so far
         */
        public int getNumberOfSentencesProcessed() {
            // not needed for this test
            return numberOfSentencesProcessed.get();
        }

        /**
         * Process sentences along with their associated article.
         *
         * @param article   The article assoicated with the sentences.
         * @param sentences A collection of Sentences to process.
         * @throws textractor.sentence.SentenceProcessingException
         *          If there was an error condition
         *          in the textractor sentence processing pipeline
         */
        public void consume(final Article article, final Collection<Sentence> sentences)
                throws SentenceProcessingException {
            assertNotNull("Article cannont be null", article);
            numberOfArticlesProcessed.addAndGet(1);
            pmids.add(article.getPmid());

            assertNotNull("Sentence collection cannont be null", sentences);
            numberOfSentencesProcessed.addAndGet(sentences.size());
        }
    }
}
