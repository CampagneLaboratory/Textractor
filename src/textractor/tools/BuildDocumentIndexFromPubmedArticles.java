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

package textractor.tools;

import com.martiansoftware.jsap.JSAPException;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.mg4j.document.DocumentSequence;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.BulletParser;
import it.unimi.dsi.mg4j.util.parser.ParsingFactory;
import it.unimi.dsi.mg4j.util.parser.WellFormedXmlFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static textractor.TextractorConstants.TIMER_LOG;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.html.AbstractHtml2Text;
import textractor.html.QueueTextConsumer;
import textractor.html.TextConsumer;
import textractor.mg4j.document.TextractorDocumentFactory;
import textractor.mg4j.document.TextractorQueueDocumentSequence;
import textractor.parsers.PubmedExtractor;
import textractor.parsers.PubmedLoadExtractor;
import textractor.parsers.PubmedLoader;
import textractor.sentence.SentenceProcessingException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.zip.GZIPInputStream;

/**
 * Builds a document index directly from a set of pubmed articles.
 */
public class BuildDocumentIndexFromPubmedArticles {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(BuildDocumentIndexFromPubmedArticles.class);

    /**
     * Indicates how many threads can potentially be feeding the document load
     * process.
     */
    protected static final int NUMBER_OF_LOADER_THREADS = 1;

    /**
     * Indicates how many threads can potentially be feeding the document index
     * process.
     */
    private static final int NUMBER_OF_INDEXER_THREADS = 1;

    /**
     * Thread pool for loading and indexing.
     * Pool size = nCPU * (1 + tload/tindex)
     */
    protected final ExecutorService threadPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() + 1
                    + NUMBER_OF_LOADER_THREADS + NUMBER_OF_INDEXER_THREADS);

    /**
     * Queue to read articles for indexing.
     */
    private final BlockingQueue<Sentence> queue;

    /**
     * Semaphore that all loader threads acquire. When the semaphore becomes
     * available to the consumer(s), the loading process is done.
     */
    private final Semaphore loaderSemaphore;

    public BuildDocumentIndexFromPubmedArticles(final String[] args)
            throws NoSuchMethodException, IllegalAccessException,
            ConfigurationException, IOException, JSAPException,
            InvocationTargetException, ClassNotFoundException,
            InstantiationException {
        super();

        // create the queue for the loading and indexing threads to use
        this.queue = new ArrayBlockingQueue<Sentence>(10000);

        loaderSemaphore = new Semaphore(NUMBER_OF_LOADER_THREADS);

        // create the file loader and start it
        final Loader loader = new Loader(args);
        threadPool.execute(loader);

        // wait until at least one of the loaders has started before indexing
        while (queue.peek() == null) {
            Thread.yield();
        }

        processDocuments(args);
    }

    protected void processDocuments(final String[] args)
            throws ConfigurationException, IOException, InstantiationException,
            IllegalAccessException, InvocationTargetException,
            NoSuchMethodException, ClassNotFoundException, JSAPException {
        // create the indexer - when the indexer is done, process will return
        final Indexer indexer = new Indexer();
        indexer.process(args);
    }

    /**
     * The class loads articles from a series of files and populates a shared
     * queue for indexing.
     */
    private final class Loader extends AbstractHtml2Text implements
            PubmedLoader, Runnable {
        private TextConsumer consumer;
        private final String articleElementName;
        private int cumulativeCounter;
        private final String filename;
        private final String list;
        private final String directory;

        public Loader(final String[] args) {
            super(args);
            consumer = new QueueTextConsumer(queue);
            appendSentencesInOneDocument = CLI.isKeywordGiven(args,
                    "-sentence-markup");
            noSentenceBoundaryTag = CLI.isKeywordGiven(args,
                    "-no-sentenceboundary-tag");
            articleElementName = CLI.getOption(args, "-article-element-name",
                    null);
            filename = CLI.getOption(args, "-i", null);
            list = CLI.getOption(args, "-list", null);
            directory = CLI.getOption(args, "-d", null);

            if (filename == null && list == null && directory == null) {
                System.err.println("usage: -i input | -list filename | -d directory.");
                System.exit(1);
            }
        }

        @Override
        public void setConsumer(final TextConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public TextConsumer getConsumer() {
            return consumer;
        }

        /**
         * Process abstracts with the given arguments.
         *
         * @see Thread#run()
         */
        public void run() {
            if (appendSentencesInOneDocument) {
                LOG.debug("-sentence-markup flag activated");
            }

            if (noSentenceBoundaryTag) {
                LOG.debug("-no-sentenceboundary-tag flag activated");
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("processing with:");
                LOG.debug("  filename: " + filename);
                LOG.debug("  list: " + list);
                LOG.debug("  directory: " + directory);
                LOG.debug("  element name: " + articleElementName);
                LOG.debug("  chunk size: " + articleChunkSize);
            }

            try {
                loaderSemaphore.acquire();
            } catch (final InterruptedException e) {
                LOG.error("Interrupted", e);
                Thread.currentThread().interrupt();
                return;
            }

            try {
                if (filename != null) {
                    processFilename(filename);
                }

                if (list != null) {
                    processFileList(list);
                }

                if (directory != null) {
                    processDirectory(directory);
                }
            } catch (final IOException e) {
                LOG.fatal("Error processing articles", e);
                e.printStackTrace(System.err);
                System.exit(2);
            } finally {
                LOG.debug("Processing complete");
                // release the semaphore so any indexing threads
                // know we are done.
                loaderSemaphore.release();
            }
        }

        /**
         * Processes the files in a given directory.
         *
         * @param directory The name of the directory to process.
         * @throws IOException if there is a problem reading the directory
         *                     processing the files in the directory.
         */
        private void processDirectory(final String directory)
                throws IOException {
            final File dir = new File(directory);

            // get all compressed xml files files the specified directory
            final FilenameFilter filter = new FilenameFilter() {
                public boolean accept(final File dir, final String name) {
                    return name.endsWith(".xml.zip")
                            || name.endsWith(".xml.gz");
                }
            };
            final String[] filenames = dir.list(filter);
            // sort the filenames in ascending order so that we ensure
            // they are loaded in the proper order. The assumption is
            // of course that the files are always named in a way that
            // the names define the order.
            Arrays.sort(filenames);
            for (final String file : filenames) {
                processFilename(directory + File.separator + file);
            }
        }

        /**
         * Process a list of filenames.
         *
         * @param list The name of the file containing the list
         * @throws IOException if there is a problem reading the list or
         *                     processing the files in the list
         */
        private void processFileList(final String list) throws IOException {
            final BufferedReader reader =
                    new BufferedReader(new FileReader(list));
            try {
                String line;
                // process each filename in turn:
                while ((line = reader.readLine()) != null) {
                    processFilename(line.trim());
                }
            } finally {
                reader.close();
            }
        }

        private void processFilename(final String filename) throws IOException {
            final StopWatch timer = new StopWatch();
            timer.start();
            LOG.info("Scanning " + filename);

            InputStream stream;
            if (filename.endsWith(".gz") || filename.endsWith(".zip")) {
                stream = new GZIPInputStream(new FileInputStream(filename));
            } else {
                stream = new FileInputStream(filename);
            }

            FastBufferedReader reader =
                    new FastBufferedReader(new InputStreamReader(stream));
            char[] buffer = new char[10000];

            // read the whole file in memory:
            int length;
            int offset = 0;

            while ((length = reader.read(buffer, offset, buffer.length - offset)) > 0) {
                offset += length;
                buffer = CharArrays.grow(buffer, offset + 1);
            }

            // DO NOT TRIM the buffer. Triming allocates a new buffer and copies the
            // result in the new one. This does in fact
            // use more memory transiently and result in more garbage collection.

            // and close up stuff we don't need anymore
            reader.close();
            reader = null;
            stream.close();
            stream = null;

            final ParsingFactory factory = new WellFormedXmlFactory();
            final BulletParser parser = new BulletParser(factory);

            final PubmedExtractor pubmedExtractor =
                    new PubmedLoadExtractor(this, filename);

            if (articleElementName != null) {
                pubmedExtractor.setArticleElementName(articleElementName);
                LOG.info("Parsing elements " + articleElementName);
            }
            parser.setCallback(pubmedExtractor);

            // parse and collect abbreviations:
            consumer.begin();

            parser.parse(buffer, 0, offset);
            consumer.end();

            timer.stop();
            if (LOG.isDebugEnabled()) {
                LOG.debug(timer +  " : " + filename);
            }
            if (TIMER_LOG.isInfoEnabled()) {
                TIMER_LOG.info(timer +  " : " + filename);
            }
            // adjust count from consumer since it is from the start of the run
            final int counter =
                consumer.getNumberOfArticlesProcessed() - cumulativeCounter;
            cumulativeCounter += counter;
            LOG.info("Loaded " + counter + " abstracts (cumulative: "
                    + cumulativeCounter + ") ");
        }

        public Article createArticle(final long pmid, final String filename) {
            final Article article = consumer.createArticle();
            article.setPmid(pmid);
            article.setFilename(filename);
            return article;
        }

        public void convert(final MutableString pmidString,
                final MutableString title, final MutableString text,
                final Map<String, Object> additionalFieldsMap, final String filename)
                throws SentenceProcessingException {
            final long pmid = Long.parseLong(pmidString.toString());

            if (LOG.isDebugEnabled()) {
                LOG.debug("converting " + pmid + " in " + filename);
            }

            final Article article = createArticle(pmid, filename);
            final Collection<Sentence> sentences =
                    loadArticleSentences(article, title.toString(),
                            text.toString(), additionalFieldsMap);
            consumer.consume(article, sentences);
        }

        public void removeArticle(final String retractedPmid) {
            // TODO
        }
    }

    private final class Indexer extends BuildDocumentIndex {
        public Indexer() {
            super();
            usePipe = false;
        }

        /**
         * This method returns a basename, to use when the user provided none
         * on the command line.
         *
         * @param basename Basename provided on the command line.
         * @param stemming Whether stemming is request
         * @return The basename provided on the command line, or a default
         * basename
         */
        @Override
        public String getDefaultBasename(final String basename,
                final boolean stemming) {
            return basename;
        }

        /**
         * Factory method. Returns a new instance of this class. Must be
         * overriden by sub-classes.
         *
         * @return a new BuildDocumentIndex object
         */
        @Override
        protected BuildDocumentIndex createNew() {
            return new Indexer();
        }

        @Override
        public int serializeTextSourceToWriter(final OutputStreamWriter writer,
                final int chunkSize) throws IOException {
            throw new UnsupportedOperationException(
                    "Should not be called. This implementation does not use the pipe mechanism.");
        }

        @Override
        public DocumentSequence documentSequence(
                final TextractorDocumentFactory factory) {
            return new TextractorQueueDocumentSequence(factory,
                    sentenceIndexFilter, queue, loaderSemaphore,
                    NUMBER_OF_LOADER_THREADS);
        }
    }

    protected final Semaphore getLoaderSemaphore() {
        return loaderSemaphore;
    }

    @Override
    protected final void finalize() throws Throwable {
        threadPool.shutdown();
        super.finalize();
    }

    public static void main(final String[] args) throws IllegalAccessException,
            NoSuchMethodException, ConfigurationException, IOException,
            JSAPException, InvocationTargetException, InstantiationException,
            ClassNotFoundException {
        final BuildDocumentIndexFromPubmedArticles builder =
            new BuildDocumentIndexFromPubmedArticles(args);
        builder.threadPool.shutdown();
    }

    protected final BlockingQueue<Sentence> getQueue() {
        return queue;
    }
}
