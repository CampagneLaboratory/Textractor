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
import com.martiansoftware.jsap.stringparsers.LongSizeStringParser;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.document.DocumentSequence;
import it.unimi.dsi.mg4j.document.InputStreamDocumentSequence;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory.MetadataKeys;
import it.unimi.dsi.mg4j.index.BitStreamIndex;
import it.unimi.dsi.mg4j.index.CompressionFlags;
import static it.unimi.dsi.mg4j.index.CompressionFlags.Coding;
import static it.unimi.dsi.mg4j.index.CompressionFlags.Component;
import it.unimi.dsi.mg4j.tool.IndexBuilder;
import it.unimi.dsi.mg4j.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.TextractorRuntimeException;
import textractor.mg4j.document.AbstractTextractorDocumentFactory;
import textractor.mg4j.document.TextractorDocumentFactory;
import textractor.mg4j.index.IndexerOptions;
import textractor.mg4j.index.LowercaseTermProcessor;
import textractor.mg4j.index.PaiceHuskStemmingTermProcessor;
import textractor.mg4j.index.PorterStemmingTermProcessor;
import textractor.mg4j.io.ProteinWordSplitterReader;
import textractor.mg4j.io.TextractorWordReader;
import textractor.util.NullSentenceFilter;
import textractor.util.SentenceFilter;
import textractor.util.SentenceParenthesesFilter;
import textractor.util.SentenceReferenceFilter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;

/**
 * Prints documents from the database into the MG document format. Individual
 * documents are separated by ascii code 002.
 */
public abstract class BuildDocumentIndex implements Runnable, UncaughtExceptionHandler {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(BuildDocumentIndex.class);
    public static final char DOCUMENT_SEPARATOR = '\002';
    public static final int DOCUMENT_SEPARATOR_UNICODE = 2;
    public static final char[] DOCUMENT_SEPARATOR_CHAR_ARRAY = new char[]{DOCUMENT_SEPARATOR};
    public static final String FILTER_NONE = "none";
    public static final String FILTER_ACRONYM = "acronym";
    public static final String FILTER_REFERENCE = "reference";

    protected SentenceFilter sentenceIndexFilter;
    protected IndexerOptions indexerOptions;

    private PipedInputStream pipedInputStream;
    private Thread runningThread;
    private Throwable runningThreadException;

    protected boolean usePipe;

    public BuildDocumentIndex() {
        super();
        usePipe = true;
        sentenceIndexFilter = new NullSentenceFilter();
        indexerOptions = new IndexerOptions();
    }

    public PipedInputStream getPipedInputStream() {
        return pipedInputStream;
    }

    public void setPipedInputStream(final PipedInputStream pipedInputStream) {
        this.pipedInputStream = pipedInputStream;
    }

    public IndexerOptions getIndexerOptions() {
        return indexerOptions;
    }

    public void setIndexerOptions(final IndexerOptions indexerOptions) {
        this.indexerOptions = indexerOptions;
    }

    public void run() {
        if (LOG.isInfoEnabled()) {
            LOG.info("batch-size: " + indexerOptions.getBatchSize());

            if (indexerOptions.isParenthesesAreWords()) {
                LOG.info("Indexing parentheses.");
            } else {
                LOG.info("Parentheses are not indexed.");
            }
        }

        try {
            final String basename = indexerOptions.getBasename();
            // store properties used to create this index
            final Properties properties =
                    storeProperties(basename + "-textractor.properties");

            // specify how parentheses should be treated using the full factory path
            properties.addProperty(TextractorDocumentFactory.class.getName() + "." + AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS.name().toLowerCase(), Boolean.toString(indexerOptions.isParenthesesAreWords()));

            // specify the word reader to use
            properties.addProperty(TextractorDocumentFactory.class.getName() + "." + MetadataKeys.WORDREADER.name().toLowerCase(), indexerOptions.getWordReader().getClass().getName());

            // specify which encoding to use
            properties.addProperty(TextractorDocumentFactory.class.getName() + "." + MetadataKeys.ENCODING.name().toLowerCase(), "UTF-8");

            // specify which encoding to use using the full factory path
            properties.addProperty(TextractorDocumentFactory.class.getName() + "." + AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH.name().toLowerCase(), Integer.toString(indexerOptions.getMinimumDashSplitLength()));

            final DocumentSequence documentSequence = getSequence(
                    pipedInputStream, TextractorDocumentFactory.class,
                    DOCUMENT_SEPARATOR_UNICODE, properties);

            IndexBuilder indexBuilder =
                    new IndexBuilder(basename, documentSequence);

            // use the specified term processor
            indexBuilder = indexBuilder.termProcessor(indexerOptions.getTermProcessor());

            // specify the database batch size
            indexBuilder = indexBuilder.documentsPerBatch((int) LongSizeStringParser.parseSize(indexerOptions.getBatchSize()));

            // are we using skips?
            if (indexerOptions.isCreateSkips()) {
                final int quantum = indexerOptions.getQuantum();
                final int height = indexerOptions.getHeight();

                if (LOG.isInfoEnabled()) {
                    LOG.info("Skips enabled. Quantum: " + quantum
                            + " height: " + height);
                }

                // yes
                indexBuilder = indexBuilder.skips(true);

                // then we also need to specify quantum
                indexBuilder = indexBuilder.quantum(quantum);

                // and height
                indexBuilder = indexBuilder.height(height);
            } else {
                indexBuilder = indexBuilder.skips(false);
            }

            final Map<Component, Coding> compressionFlags =
                    indexerOptions.getCompressionFlags();

            // now add the compression flags
            indexBuilder = indexBuilder.standardWriterFlags(compressionFlags);

            // TODO - separate payload flags
            indexBuilder = indexBuilder.payloadWriterFlags(CompressionFlags.DEFAULT_PAYLOAD_INDEX);

            // Are we creating a zip document collection also?
            final String zipDocumentCollectionName =
                    indexerOptions.getZipDocumentCollectionName();
            if (StringUtils.isNotBlank(zipDocumentCollectionName)) {
                indexBuilder = indexBuilder.zipCollectionBasename(zipDocumentCollectionName);
            }

            indexBuilder.run();
        } catch (final Exception e) {
            LOG.error("Caught exception while indexing", e);

            // don't squash the exception, but ensure that the calling
            // thread will get the error
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new TextractorRuntimeException(e);
            }
        }

        LOG.info("Indexing has returned.");
    }

    public void process(final String[] args) throws ConfigurationException,
            IOException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException,
            ClassNotFoundException, JSAPException {
        final StopWatch timer = new StopWatch();
        timer.start();

        String basename = CLI.getOption(args, "-basename", null);
        final boolean stemming = CLI.isKeywordGiven(args, "-stemming", false);
        final String stemmerChoice = CLI.getOption(args, "-stemmer", "PaiceHusk");  // can be PaiceHusk or Porter
        final int chunkSize = CLI.getIntOption(args, "-chunk-size", 100000);

        // use -filter acronym to index only sentences that contain acronyms/abbreviations
        final String filter = CLI.getOption(args, "-filter", FILTER_NONE);
        basename = getDefaultBasename(basename, stemming);
        final String batchSizeOption = CLI.getOption(args, "-batch-size", "10Mi");
        if (CLI.isKeywordGiven(args, "-skips")) {
            indexerOptions.createSkips(CLI.getIntOption(args, "-quantum", BitStreamIndex.DEFAULT_QUANTUM), CLI.getIntOption(args, "-height", BitStreamIndex.DEFAULT_HEIGHT));
        }

        if (basename == null) {
            System.err.println("-basename is required. ");
            System.exit(0);
        }

        final String wordReaderClassName = CLI.getOption(args, "-wordReader",
                ProteinWordSplitterReader.class.getName());
        final TextractorWordReader wordReader =
                (TextractorWordReader) Class.forName(wordReaderClassName).newInstance();

        final String termProcessorClassName = CLI.getOption(args, "-termProcessor",
                LowercaseTermProcessor.class.getName());
        final String zipDocumentCollectionName =
                CLI.getOption(args, "-zip", null);

        // let the word reader determine its configuration from the command line arguments:
        wordReader.configureFromCommandLine(args);

        LOG.info("Building index: " + basename);
        indexerOptions.setWordReader(wordReader);
        indexerOptions.setFilter(filter);
        indexerOptions.setBatchSize(batchSizeOption);
        indexerOptions.setChunkSize(chunkSize);
        indexerOptions.setZipDocumentCollectionName(zipDocumentCollectionName);
        indexerOptions.setTermProcessorClassName(termProcessorClassName);
        if (stemming) {
            indexerOptions.setStemmerChoice(stemmerChoice);
        } else {
            indexerOptions.setStemmerChoice(IndexerOptions.StemmerChoice.NO_STEMMER);
        }

        index(stemming, chunkSize, basename);

        timer.stop();
        System.out.println(timer);
    }

    /**
     * This method returns a basename, to use when the user provided none on the
     * command line.
     *
     * @param basename Basename provided on the command line.
     * @param stemming Whether stemming is request
     * @return The basename provided on the command line, or a default basename
     */
    public abstract String getDefaultBasename(final String basename,
                                              final boolean stemming);

    public void index(final boolean stemming, final int chunkSize,
                      final String basename) throws IOException {
        initStemming(stemming);

        if (IndexerOptions.getFilterNone().equals(indexerOptions.getFilter())) {
            sentenceIndexFilter = new NullSentenceFilter();
        } else if (IndexerOptions.FILTER_ACRONYM.equals(indexerOptions.getFilter())) {
            sentenceIndexFilter = new SentenceParenthesesFilter();
            LOG.debug("Installing sentence filter: Acronyms");
        } else if (IndexerOptions.FILTER_ACRONYM.equals(indexerOptions.getFilter())) {
            sentenceIndexFilter = new SentenceReferenceFilter();
            LOG.debug("Installing sentence filter: References");
        }
        pass(chunkSize, basename);
    }

    protected void initStemming(final boolean stemming) {
        switch (getIndexerOptions().getStemmer()) {
            case PAICE_HUSK_STEMMER:
                getIndexerOptions().setTermProcessorClassName(PaiceHuskStemmingTermProcessor.class.getName());
                System.out.println("Indexing with PaiceHusk stemmer.");
                break;
            case PORTER_STEMMER:
                getIndexerOptions().setTermProcessorClassName(PorterStemmingTermProcessor.class.getName());
                System.out.println("Indexing with Porter stemmer (snowball implementation).");
                break;
            case NO_STEMMER:
                System.out.println("Indexing without stemmer.");
                break;
        }
    }

    private void pass(final int chunkSize, final String basename)
            throws IOException {
        final PipedOutputStream pipedOutput = new PipedOutputStream();
        final BufferedOutputStream output =
                new BufferedOutputStream(pipedOutput);
        indexerOptions.setBasename(basename);

        BuildDocumentIndex pass = createNew();
        pass.setIndexerOptions(getIndexerOptions());
        pass.startPass(basename, pipedOutput);

        LOG.info("chunk-size: " + chunkSize);
        final int count;
        if (usePipe) {
            LOG.info("Using Pipe");
            final OutputStreamWriter writer =
                    new OutputStreamWriter(output, "UTF-8");
            count = serializeTextSourceToWriter(writer, chunkSize);
            writer.flush();
            writer.close();
            if (count == 0) {
                LOG.error("There are no sentences to index.");
                System.exit(1);
            }
        }
        LOG.info("Waiting for indexing to finish..");

        while (pass.runningThread.isAlive()) {
            try {
                pass.runningThread.join();
            } catch (final InterruptedException e) {
                LOG.error("Interrupted", e);
                Thread.currentThread().interrupt();
                return;
            }
        }

        try {
            if (pass.runningThreadException != null) {
                LOG.error("Caught exception from indexing thread.",
                        pass.runningThreadException);
                throw new TextractorRuntimeException(pass.runningThreadException);
            }
        } finally {
            pass = null;
        }

        LOG.info("Indexing done.");
    }

    /**
     * Factory method. Returns a new instance of this class. Must be overriden
     * by sub-classes.
     *
     * @return a new BuildDocumentIndex object
     */
    protected abstract BuildDocumentIndex createNew();

    public abstract int serializeTextSourceToWriter(OutputStreamWriter writer, int chunkSize) throws IOException;

    public void startPass(final String basename, final PipedOutputStream pipedOutput) throws IOException {
        setPipedInputStream(new PipedInputStream(pipedOutput));
        runningThread = new Thread(this);
        runningThread.setUncaughtExceptionHandler(this);
        runningThread.start();
    }

    /**
     * Stores properties of this class into the given filename.
     *
     * @param filename Name of the file to store the properties in
     * @return A property object containing the properties stored.
     * @throws ConfigurationException if the file cannot be written properly
     */
    protected Properties storeProperties(final String filename)
            throws ConfigurationException {
        final Properties properties = new Properties();
        properties.addProperty("dateIndexed", new Date().toString());
        properties.addProperty("documentIndexClass", this.getClass().getName());
        properties.addProperty("documentSeparator", new String(DOCUMENT_SEPARATOR_CHAR_ARRAY));
        properties.addProperty(MetadataKeys.ENCODING, "UTF-8");
        properties.addProperty("lowercaseIndex", indexerOptions.isLowercaseIndex()?"true":"false");
        properties.addProperty("sentenceFilterClass", sentenceIndexFilter.getClass().getName());
        properties.addProperty("batchSize", indexerOptions.getBatchSize());

        // word reader properties
        indexerOptions.getWordReader().saveProperties(properties);


        final String zipDocumentCollectionName =
                indexerOptions.getZipDocumentCollectionName();

        if (StringUtils.isNotBlank(zipDocumentCollectionName)) {
            properties.addProperty("zipDocumentCollectionName", zipDocumentCollectionName);
        }

        // and save everything to a file
        properties.save(filename);
        return properties;
    }

    public DocumentSequence getSequence(final InputStream stream,
                                        final Class factoryClass,
                                        final int delimiter,
                                        final Properties properties)
            throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException,
            InstantiationException {

        final TextractorDocumentFactory factory = (TextractorDocumentFactory) PropertyBasedDocumentFactory.getInstance(TextractorDocumentFactory.class,
                properties);

        if (usePipe) {
            LOG.debug("Using Pipe");
            return new InputStreamDocumentSequence(stream, delimiter, factory);
        } else {
            LOG.debug("No pipe, accessing sentences directly from database");
            return documentSequence(factory);
        }
    }

    public abstract DocumentSequence documentSequence(final TextractorDocumentFactory factory);

    public void setParenthesesAreWords(final boolean flag) {
        indexerOptions.setParenthesesAreWords(flag);
    }

    /**
     * Method invoked when the given thread terminates due to the
     * given uncaught exception.
     * <p>Any exception thrown by this method will be ignored by the
     * Java Virtual Machine.
     *
     * @param t the thread
     * @param e the exception
     */
    public void uncaughtException(final Thread t, final Throwable e) {
        runningThreadException = e;
        LOG.error("Caught exception from thread " + t.getName(), e);
    }
}
