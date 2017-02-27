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

package textractor.chain.loader;

import junit.framework.TestCase;
import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.impl.CatalogBase;
import org.apache.commons.io.FileUtils;
import textractor.chain.AbstractSentenceConsumer;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.sentence.SentenceProcessingException;
import textractor.test.util.AssertFilesEqual;
import textractor.tools.chain.ChainExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TestFileLoader extends TestCase {
    private final List<String> filenames = new ArrayList<String>();

    public void setUp() {
        filenames.clear();
    }

    /**
     * Validate that a single file can be loaded.
     * @throws Exception if there is a problem executing the chain
     */
    public void testLoadSingleFile() throws Exception {
        final String filename = "This is a test filename";
        // create a simple loader
        final TestLoader loader = new TestLoader();
        loader.setFile(filename);
        // add a simple consumer
        loader.addCommand(new TestConsumer());

        // and execute it
        final Catalog catalog = new CatalogBase();
        catalog.addCommand("SingleFileLoaderTest", loader);
        final ChainExecutor chainExecutor = new ChainExecutor(catalog);
        chainExecutor.execute();

        // check the results
        assertEquals(1, filenames.size());
        assertEquals(filename, filenames.get(0));
    }

    /**
     * Validate that the file processing log is written properly.
     * @throws Exception if there is a problem executing the chain
     */
    public void testFileLog() throws Exception {
        final String[] filenameArray = {
                "This is a test filename",
                "How now brown cow",
                "/some/unix/directory/file.txt",
                "I like bananas",
                "C:\\Windows\\Has Silly\\Paths\\And spaces too.htm(2)"
        };

        // create a list of filenames in a file
        File list = File.createTempFile(this.getClass().getName(), null);
        FileUtils.writeLines(list, null, Arrays.asList(filenameArray));

        // and create temp file for the log
        File log = File.createTempFile(this.getClass().getName(), null);

        // create a simple loader
        final TestLoader loader = new TestLoader();
        loader.setList(list.getAbsolutePath());
        loader.setProcessedFileLog(log.getAbsolutePath());

        // add a simple consumer
        loader.addCommand(new TestConsumer());

        // and execute it
        final Catalog catalog = new CatalogBase();
        catalog.addCommand("LoggerTest", loader);
        final ChainExecutor chainExecutor = new ChainExecutor(catalog);
        chainExecutor.execute();

        // check the results
        assertEquals(filenameArray.length, filenames.size());
        for (String file : filenameArray) {
            assertTrue(file + " not processed", filenames.contains(file));
        }

        AssertFilesEqual.assertEquals("log file doesn't match file list",
                list, log);
    }

    private class TestLoader extends AbstractFileLoader {
        /**
         * Process a single file designated by name.
         *
         * @param filename The name of the file to process.
         * @throws java.io.IOException if there is a problem reading the file.
         */
        public void processFilename(final String filename) throws IOException {
            filenames.add(filename);
            produce(new Article(), new ArrayList<Sentence>());
        }

        /**
         * Get the number of articles processed so far.
         *
         * @return The number of articles processed so far
         */
        public int getNumberOfArticlesProcessed() {
            // not needed for this test
            return 0;
        }
    }

    private class TestConsumer extends AbstractSentenceConsumer {
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
         * Process sentences along with their associated article.
         *
         * @param article   The article assoicated with the sentences.
         * @param sentences A collection of Sentences to process.
         * @throws textractor.sentence.SentenceProcessingException
         *          If there was an error condition
         *          in the textractor sentence processing pipeline
         */
        public void consume(final Article article, final Collection<Sentence> sentences) throws SentenceProcessingException {
            assertNotNull("Article cannont be null", article);
            assertNotNull("Sentence collection cannont be null", sentences);
        }
    }
}
