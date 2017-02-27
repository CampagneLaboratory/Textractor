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

package textractor.chain.indexer;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.mg4j.document.Document;
import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.document.DocumentIterator;
import it.unimi.dsi.mg4j.document.DocumentSequence;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory.MetadataKeys;
import it.unimi.dsi.mg4j.index.BitStreamIndex;
import it.unimi.dsi.mg4j.index.CompressionFlags;
import it.unimi.dsi.mg4j.index.CompressionFlags.Coding;
import it.unimi.dsi.mg4j.index.CompressionFlags.Component;
import it.unimi.dsi.mg4j.index.SkipBitStreamIndexWriter;
import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.tool.Combine;
import it.unimi.dsi.mg4j.tool.IndexBuilder;
import it.unimi.dsi.mg4j.tool.Paste;
import it.unimi.dsi.mg4j.tool.Scan;
import it.unimi.dsi.mg4j.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.TextractorRuntimeException;
import textractor.chain.AbstractSentenceConsumer;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.mg4j.document.AbstractTextractorDocumentFactory;
import textractor.mg4j.document.ConfigurableTextractorDocumentFactory;
import textractor.mg4j.document.TextractorDocumentFactory;
import textractor.mg4j.document.TextractorFieldInfo;
import textractor.mg4j.index.LowercaseTermProcessor;
import textractor.mg4j.io.ProteinWordSplitterReader;
import textractor.mg4j.io.TextractorWordReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link org.apache.commons.chain.Command} that creates an index with mg4j.
 */
public final class Indexer extends AbstractSentenceConsumer {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(Indexer.class);

    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /** Number of sentences processed so far. */
    private final AtomicInteger numberOfSentencesProcessed =
        new AtomicInteger();

    /** Size of the indexing work queue. */
    private int indexingQueueSize = 10000;

    /**
     * The mg4j indexer works on individual {@link Sentence} objects so we
     * consume {@link textractor.chain.ArticleSentencesPair}s and queue them
     * up for the indexing thread.
     */
    private BlockingQueue<Sentence> indexingQueue =
            new ArrayBlockingQueue<Sentence>(indexingQueueSize);

    /**
     * Indicates that the indexing process has been started.
     */
    private final AtomicBoolean indexingStarted = new AtomicBoolean();

    /**
     * Indicates that the indexing process has finished.
     */
    private final CountDownLatch indexingComplete = new CountDownLatch(1);

    public static final int DOCUMENT_SEPARATOR = 2;

    /**
     * Basename for the index.
     */
    private String basename;

    /**
     * The number of occurrences in each batch.
     */
    private int documentsPerBatch = Scan.DEFAULT_BATCH_SIZE;
    private boolean lowercaseIndex = true;
    private boolean parenthesesAreWords;

    private boolean skips;
    private int quantum = BitStreamIndex.DEFAULT_QUANTUM;
    private int height = BitStreamIndex.DEFAULT_HEIGHT;

    private int scanBufferSize = Scan.DEFAULT_BUFFER_SIZE;
    private int combineBufferSize = Combine.DEFAULT_BUFFER_SIZE;
    private int skipBufferSize = SkipBitStreamIndexWriter.DEFAULT_TEMP_BUFFER_SIZE;
    private int pasteBufferSize = Paste.DEFAULT_MEMORY_BUFFER_SIZE;

    /**
     * The indexConfigurationFile (used if the documentFactoryClass is
     * ConfigurableTextractorDocumentFactory.
     */
    private String indexConfigurationFile;

    /**
     * The document factory class to use.
     */
    private Class<DocumentFactory> documentFactoryClass;

    /**
     * Minimum index at which a word can be split.
     */
    private int minimumDashSplitLength =
        TextractorDocumentFactory.DEFAULT_MINIMUM_DASH_SPLIT_LENGTH;

    /**
     * Name of the zip file for the document collection.
     */
    private String zipDocumentCollectionName;

    private Map<Component, Coding> standardWriterFlags =
            CompressionFlags.DEFAULT_STANDARD_INDEX;

    private Map<Component, Coding> payloadWriterFlags =
            CompressionFlags.DEFAULT_PAYLOAD_INDEX;

    private TextractorWordReader wordReader;
    private String wordReaderClass;

    private String termProcessorClass = LowercaseTermProcessor.class.getName();

    /**
     * Create a new indexer {@link org.apache.commons.chain.Command}.
     * @throws IllegalAccessException error creating object
     * @throws InstantiationException error creating object
     * @throws ClassNotFoundException error creating object
     */
    public Indexer() throws IllegalAccessException, InstantiationException,
            ClassNotFoundException {
        super();
        setWordReaderClass(ProteinWordSplitterReader.class.getName());
        setDocumentFactoryClass(TextractorDocumentFactory.class.getName());
    }

    /**
     * Process sentences along with their associated article into individual
     * sentences so that the {@link it.unimi.dsi.mg4j.tool.IndexBuilder} process
     * can read them properly.
     *
     * @param article   The article assoicated with the sentenceQueue.
     * @param sentences A collection of Sentences to process.
     */
    public void consume(final Article article, final Collection<Sentence> sentences) {
        // start the indexer once we start getting sentences to process
        if (!indexingStarted.getAndSet(true)) {
            final ExecutorService executorService =
                    textractorContext.getThreadPool();
            final Future<Boolean> indexer =
                    executorService.submit(new Callable<Boolean>() {
                        public Boolean call() throws Exception {
                            try {
                                index();
                            } catch (Throwable t) {
                                LOG.fatal("Got an exception in thread "
                                        + this.getClass().getName(), t);
                                Thread.currentThread().getThreadGroup().interrupt();
                                if (t instanceof Exception) {
                                    throw (Exception) t;
                                } else {
                                    throw new Exception(t);
                                }
                            } finally {
                                indexingComplete.countDown();
                            }

                            return true;
                        }
                    });

            textractorContext.getWorkThreads().add(indexer);
        }

        try {
            for (final Sentence sentence : sentences) {
                if (LOG.isTraceEnabled()) {
                    final long articleNumber = article.getArticleNumber();
                    final long sentenceNumber = sentence.getDocumentNumber();
                    LOG.trace("[" + articleNumber + ", " + sentenceNumber + "] "
                        + sentence.getText().substring(0,
                         Math.min(50, sentence.getText().length())));
                }
                indexingQueue.put(sentence);
                numberOfSentencesProcessed.getAndIncrement();
            }
            numberOfArticlesProcessed.getAndIncrement();
        } catch (InterruptedException e) {
            LOG.error("Interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private void index() throws IllegalAccessException, NoSuchMethodException,
            ConfigurationException, IOException, InvocationTargetException,
            InstantiationException, ClassNotFoundException, URISyntaxException {
        if (LOG.isInfoEnabled()) {
            LOG.info("documentsPerBatch: " + documentsPerBatch);

            if (parenthesesAreWords) {
                LOG.info("Indexing parentheses.");
            } else {
                LOG.info("Parentheses are not indexed.");
            }
        }

        // store properties used to create this index
        final Properties properties =
                storeProperties(basename + "-textractor.properties");

        // specify how parentheses should be treated using the full factory path
        properties.addProperty(TextractorDocumentFactory.class.getName() + "."
                + AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS
                .name().toLowerCase(), Boolean.toString(parenthesesAreWords));

        // specify the word reader to use
        properties.addProperty(TextractorDocumentFactory.class.getName() + "."
                + MetadataKeys.WORDREADER.name().toLowerCase(), wordReaderClass);

        // specify which encoding to use
        properties.addProperty(TextractorDocumentFactory.class.getName() + "."
                + MetadataKeys.ENCODING.name().toLowerCase(), "UTF-8");

        // specify threshold used to split
        properties.addProperty(TextractorDocumentFactory.class.getName() + "."
                + AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH
                .name().toLowerCase(), Integer.toString(minimumDashSplitLength));

        final DocumentSequence documentSequence = getSequence(documentFactoryClass, properties);

        IndexBuilder indexBuilder =
                new IndexBuilder(basename, documentSequence);

        // use the specified term processor
        if (StringUtils.isNotBlank(termProcessorClass)) {
            final Class clazz = Class.forName(termProcessorClass);
            final TermProcessor termProcessor =
                (TermProcessor)clazz.getMethod( "getInstance",
                        (Class[])null ).invoke( termProcessorClass,
                        (Object[])null );
            indexBuilder = indexBuilder.termProcessor(termProcessor);
        }

        // specify the database batch size
        indexBuilder = indexBuilder.documentsPerBatch(documentsPerBatch);

        // are we using skips?
        if (skips) {
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

        // now add the compression flags
        indexBuilder = indexBuilder.standardWriterFlags(standardWriterFlags);
        indexBuilder = indexBuilder.payloadWriterFlags(payloadWriterFlags);

        // Are we creating a zip document collection also?
        if (StringUtils.isNotBlank(zipDocumentCollectionName)) {
            indexBuilder = indexBuilder.zipCollectionBasename(zipDocumentCollectionName);
        }

        LOG.debug("Calling index");
        indexBuilder.run();
        LOG.info("Indexing has returned.");

        // now that everything is done, save the properties with
        // the total sentence counts, etc.
        properties.addProperty("sentenceCount",
                numberOfSentencesProcessed.toString());
        properties.addProperty("articleCount",
                numberOfArticlesProcessed.toString());
        properties.save();
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

        LOG.info("documentsPerBatch=" + documentsPerBatch);
        LOG.info("indexingQueueSize=" + indexingQueueSize);
        LOG.info("scanBufferSize=" + scanBufferSize);

        properties.addProperty("dateIndexed", new Date().toString());
        properties.addProperty("documentIndexClass", this.getClass().getName());
        properties.addProperty("documentSeparator", Integer.toString(DOCUMENT_SEPARATOR));
        properties.addProperty(MetadataKeys.ENCODING, "UTF-8");
        properties.addProperty("lowercaseIndex", Boolean.toString(lowercaseIndex));
        properties.addProperty("documentsPerBatch", Integer.toString(documentsPerBatch));
        properties.addProperty("scanBufferSize", Integer.toString(scanBufferSize));
        properties.addProperty("combineBufferSize", Integer.toString(combineBufferSize));
        properties.addProperty("skipBufferSize", Integer.toString(skipBufferSize));
        properties.addProperty("pasteBufferSize", Integer.toString(pasteBufferSize));
        properties.addProperty("documentFactoryClass", documentFactoryClass.getName());

        properties.addProperty(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                Boolean.toString(parenthesesAreWords));
        // word reader properties
        wordReader.configure(properties);
        wordReader.saveProperties(properties);

        if (StringUtils.isNotBlank(zipDocumentCollectionName)) {
            properties.addProperty("zipDocumentCollectionName", zipDocumentCollectionName);
        }

        // addFieldProperties will append the fields, which is only relevant if we
        // are using the ConfigurableTextractorDocumentFactory.
        addFieldProperties(properties);

        // and save everything to a file
        properties.setFileName(filename);
        properties.save();
        return properties;
    }

    /**
     * Add the field definition information from the indexConfigurationFile
     * properties file into properties. This will only do something if
     * indexConfigurationFile is defined, which generally means we are using the
     * ConfigurableTextractorDocumentFactory.
     * @param properties the properties to copy the values into
     * @throws ConfigurationException error setting properties
     */
    @SuppressWarnings("unchecked")
    private void addFieldProperties(final Properties properties) throws ConfigurationException {

        if (indexConfigurationFile == null) {
            return;
        }

        properties.addProperty(
                ConfigurableTextractorDocumentFactory.MetadataKeys.CONFIGURATION_FILE,
                "{BASENAME}" + "-textractor.properties");

        final PropertiesConfiguration indexConfig =
                new PropertiesConfiguration(indexConfigurationFile);
        final Iterator<String> iterator = indexConfig.getKeys();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            properties.addProperty(key, indexConfig.getProperty(key));
        }
    }

    private DocumentSequence getSequence(
            final Class<DocumentFactory> factoryClass,
            final Properties properties) throws IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, InstantiationException {

        DocumentFactory tempFactory;
        try {
            // First try to create a document factory using basename, properties
            tempFactory = TextractorDocumentFactory.getInstance(factoryClass, basename, properties);
        } catch (NoSuchMethodException e) {
            // If the factoryClass doesn't support that, make one with just properties
            tempFactory = TextractorDocumentFactory.getInstance(factoryClass, properties);
        }
        final DocumentFactory factory = tempFactory;

        System.out.println("!! Created a document factory: " + factory.getClass().getName());
        final List<TextractorFieldInfo> indexFields;
        if (factory instanceof AbstractTextractorDocumentFactory) {
            indexFields = ((AbstractTextractorDocumentFactory) factory).getFieldInfoList();
            System.out.println(ArrayUtils.toString(indexFields));
        } else {
            indexFields = null;
        }
        return new DocumentSequence() {
            /**
             * Returns an iterator over the sequence of documents.
             * <p/>
             * <p><strong>Warning</strong>: this method can be safely called
             * just <em>one</em> time. For instance, implementations based
             * on standard input will usually throw an exception if this
             * method is called twice.
             * <p/>
             * <p>Implementations may decide to override this restriction
             * (in particular, if they implement
             * {@link it.unimi.dsi.mg4j.document.DocumentCollection}). Usually,
             * however, it is not possible to obtain <em>two</em> iterators at the
             * same time on a collection.
             *
             * @return an iterator over the sequence of documents.
             * @see it.unimi.dsi.mg4j.document.DocumentCollection
             */
            public DocumentIterator iterator() {
                return new DocumentIterator() {
                    /**
                     * Returns the next document.
                     *
                     * @return the next document, or <code>null</code>
                     *         if there are no other documents.
                     */
                    public Document nextDocument() throws IOException {
                        final String text;
                        final Reference2ObjectMap<Enum<?>, Object> metadata;
                        final Map<String, Object> fieldValues =
                                new Object2ObjectOpenHashMap<String, Object>();
                        try {
                            Sentence sentence;
                            do {
                                sentence = indexingQueue.poll(100, MILLISECONDS);
                                if (sentence == null && productionCompleted) {
                                    return null;
                                }
                            } while (sentence == null);

                            metadata = sentence.getMetaData();
                            text = sentence.getText();
                            if (indexFields != null) {
                                final Map<String, Object> articleFieldValues =
                                        sentence.getArticle().getAdditionalFieldsMap();
                                for (final TextractorFieldInfo fieldInfo : indexFields) {
                                    // Enforce additionalField Object being the right type
                                    final Object value = assureFieldType(fieldInfo,
                                            articleFieldValues.get(fieldInfo.getName()));
                                    if (value != null) {
                                        fieldValues.put(fieldInfo.getName(), value);
                                    }
                                }
                            }
                        } catch (final InterruptedException e) {
                            LOG.error("Interrupted!", e);
                            Thread.currentThread().interrupt();
                            return null;
                        }

                        final Properties sentenceProperties = new Properties();
                        sentenceProperties.addProperty("text", text);
                        for (final Map.Entry<String, Object> entry : fieldValues.entrySet()) {
                            sentenceProperties.addProperty(entry.getKey(), entry.getValue());
                        }

                        final String encoding =
                                (String) metadata.get(
                                        PropertyBasedDocumentFactory.MetadataKeys.ENCODING);

                        final ByteArrayOutputStream memoryOutputStream =
                                new ByteArrayOutputStream();
                        try {
                            sentenceProperties.save(memoryOutputStream, encoding);
                        } catch (ConfigurationException e) {
                            throw new TextractorRuntimeException(e);
                        }
                        final InputStream stream =
                                new FastBufferedInputStream(
                                        new ByteArrayInputStream(
                                                memoryOutputStream.toByteArray()));
                        return factory.getDocument(stream, metadata);
                    }

                    /**
                     * Closes this document iterator, releasing all resources.
                     * <p/>
                     * <p>You should always call this method after having
                     * finished with this iterator.Implementations are invited
                     * to call this method in a finaliser as a safety net, but
                     * since there is no guarantee as to when finalisers are
                     * invoked, you should not depend on this behaviour.
                     */
                    public void close() {
                    }
                };
            }

            /**
             * Returns the factory used by this sequence.
             * <p/>
             * <P>Every document sequence is based on a document factory that
             * transforms raw bytes into a sequence of characters. The factory
             * contains useful information such as the number of fields.
             *
             * @return the factory used by this sequence.
             */
            public DocumentFactory factory() {
                return factory;
            }

            /**
             * Closes this document sequence, releasing all resources.
             * <p/>
             * You should always call this method after having finished
             * with this document sequence. Implementations are invited to
             * call this method in a finaliser as a safety net (even better,
             * implement {@link it.unimi.dsi.mg4j.io.SafelyCloseable}),
             * but since there is no guarantee as to when finalisers are
             * invoked, you should not depend on this behaviour.
             */
            public void close() {
            }

        };
    }

    /**
     * Assure that value is appropriate for fieldInfo.type. If fieldInfo.name
     * is "text" this will just return null. If value is appropriate, this will
     * return value. If the value is inappropriate this will throw IllegalArgumentException
     * as indexing should be killed.
     * @param fieldInfo the field we are indexing the value for
     * @param value the value for the field
     * @return the value or null
     * @throws IllegalArgumentException the value is no appropriate for fieldInfo.type
     */
    public static Object assureFieldType(
            final TextractorFieldInfo fieldInfo, final Object value) {
        if (fieldInfo.getName().equals("text")) {
            return null;
        }
        if (fieldInfo.getType() == DocumentFactory.FieldType.TEXT) {
            if (value == null) {
                return "";
            } else {
                if (value instanceof String) {
                    return value;
                } else {
                    throw new IllegalArgumentException(fieldErrorString(fieldInfo, value));
                }
            }
        } else if (fieldInfo.getType() == DocumentFactory.FieldType.INT) {
            if (value == null) {
                return -1L;
            } else {
                if (value instanceof Long) {
                    return value;
                } else {
                    throw new IllegalArgumentException(fieldErrorString(fieldInfo, value));
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Only field types TEXT and INT are currently supported.");
        }
    }

    /**
     * The value is not valid for the fieldInfo.tyep. Generate the error string.
     * @param fieldInfo the field which is in error
     * @param value the value which is invalid
     * @return the error string
     */
    public static String fieldErrorString(final TextractorFieldInfo fieldInfo, final Object value) {
        String expectedFieldObjectType = "<UNKNOWN?>";
        if (fieldInfo.getType() == DocumentFactory.FieldType.TEXT) {
            expectedFieldObjectType = "java.lang.String";
        } else if (fieldInfo.getType() == DocumentFactory.FieldType.INT) {
            expectedFieldObjectType = "java.lang.Long";
        }
        return String.format(
                "Object value class for field named %s datatype %s "
                        + " should have been of type %s but was %s with value.toString() of %s",
                fieldInfo.getName(), fieldInfo.getType().name(),
                expectedFieldObjectType, value.getClass().getName(), value.toString());
    }

    /**
     * Choose if parentheses should be indexed as words.
     *
     * @param parenthesesAreWords True if the index should be built with
     *                            parentheses indexed as words.
     */
    public void setParenthesesAreWords(final boolean parenthesesAreWords) {
        this.parenthesesAreWords = parenthesesAreWords;
    }

    /**
     * @return the basename
     */
    public String getBasename() {
        return basename;
    }

    /**
     * @param name the basename to set
     */
    public void setBasename(final String name) {
        this.basename = name;
    }

    /**
     * Gets the number of documents per batch.
     *
     * @return the number of documents {@link Scan}
     * will attempt to add to each batch.
     */
    public int getDocumentsPerBatch() {
        return documentsPerBatch;
    }

    /**
     * Sets the number of documents per batch.
     *
     * @param numberOfDocuments the number of documents
     * {@link Scan} will attempt to add to each batch.
     */
    public void setDocumentsPerBatch(final int numberOfDocuments) {
        this.documentsPerBatch = numberOfDocuments;
    }

    /**
     * @return the minimumDashSplitLength
     */
    public int getMinimumDashSplitLength() {
        return minimumDashSplitLength;
    }

    /**
     * @param minimumDashSplitLength the minimumDashSplitLength to set
     */
    public void setMinimumDashSplitLength(final int minimumDashSplitLength) {
        this.minimumDashSplitLength = minimumDashSplitLength;
    }

    /**
     * @return the wordReader
     */
    public TextractorWordReader getWordReader() {
        return wordReader;
    }

    public void setWordReader(final TextractorWordReader wordReader) {
        assert wordReader != null : "Can't set a null word reader";
        this.wordReader = wordReader;
        this.wordReaderClass = wordReader.getClass().getName();
    }

    public void setWordReaderClass(final String wordReader)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        assert wordReader != null : "Can't set a null word reader";
        this.wordReaderClass = wordReader;
        this.wordReader = (TextractorWordReader)
                Class.forName(wordReader).newInstance();
    }

    /**
     * @return the zipDocumentCollectionName
     */
    public String getZipDocumentCollectionName() {
        return zipDocumentCollectionName;
    }

    /**
     * @param zipDocumentCollectionName the zipDocumentCollectionName to set
     */
    public void setZipDocumentCollectionName(final String zipDocumentCollectionName) {
        this.zipDocumentCollectionName = zipDocumentCollectionName;
    }

    /**
     * @return the parenthesesAreWords
     */
    public boolean isParenthesesAreWords() {
        return parenthesesAreWords;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(final int height) {
        this.height = height;
    }

    /**
     * @return the quantum
     */
    public int getQuantum() {
        return quantum;
    }

    /**
     * @param quantum the quantum to set
     */
    public void setQuantum(final int quantum) {
        this.quantum = quantum;
    }

    /**
     * @return the skips
     */
    public boolean isSkips() {
        return skips;
    }

    /**
     * @param skips the skips to set
     */
    public void setSkips(final boolean skips) {
        this.skips = skips;
    }

    public boolean isLowercaseIndex() {
        return lowercaseIndex;
    }

    public void setLowercaseIndex(final boolean lowercaseIndex) {
        this.lowercaseIndex = lowercaseIndex;
    }

    public Map<Component, Coding> getStandardWriterFlags() {
        return standardWriterFlags;
    }

    public void setStandardWriterFlags(final Map<Component, Coding> flags) {
        this.standardWriterFlags = flags;
    }

    public Map<Component, Coding> getPayloadWriterFlags() {
        return payloadWriterFlags;
    }

    public void setPayloadWriterFlags(final Map<Component, Coding> flags) {
        this.payloadWriterFlags = flags;
    }

    public String getTermProcessorClass() {
        return termProcessorClass;
    }

    public void setTermProcessorClass(final String termProcessorClass) {
        this.termProcessorClass = termProcessorClass;
    }

    @SuppressWarnings("unchecked")
    public void setDocumentFactoryClass(final String factoryClassName) {
        assert factoryClassName != null : "Can't set a null document factory";

        System.out.println("Document factory class=" + factoryClassName);

        try {
            this.documentFactoryClass = (Class<DocumentFactory>) Class.forName(factoryClassName);
        } catch (ClassNotFoundException e) {
            LOG.error(e);
            System.exit(1);
        }
    }

    public void setIndexConfigurationFile(final String indexConfigurationFileVal) {
        this.indexConfigurationFile = indexConfigurationFileVal;
    }

    /**
     * Get the number of articles processed so far.
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed.get();
    }

    /**
     * Get the number of sentences processed so far.
     * @return The number of sentences processed so far
     */
    public int getNumberOfSentencesProcessed() {
        return numberOfSentencesProcessed.get();
    }

    /**
     * Indicate that all processing is complete and it's ok to terminate.
     * If false is returned the consumer thread will terminate without firing
     * a {@link textractor.event.sentence.SentenceProcessingCompleteEvent}.
     * Be aware of this and send the event if you override the default
     * behavior.
     *
     * @return true if it's ok to complete.
     */
    @Override
    public boolean okToComplete() {
        // if the indexer never started, it's ok to shutdown but we don't want
        // one to start while we are terminating
        if (indexingStarted.getAndSet(true)) {
            try {
                // it's not ok to terminate until the indexer is done
                indexingComplete.await();
            } catch (InterruptedException e) {
                LOG.error("Interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        LOG.info("Number of Articles: " + numberOfArticlesProcessed.get());
        LOG.info("Number of Sentences: " + numberOfSentencesProcessed.get());

        return true;
    }

    /**
     * @return the combineBufferSize
     */
    public int getCombineBufferSize() {
        return combineBufferSize;
    }

    /**
     * @param size the combineBufferSize to set
     */
    public void setCombineBufferSize(final int size) {
        this.combineBufferSize = size;
    }

    /**
     * @return the pasteBufferSize
     */
    public int getPasteBufferSize() {
        return pasteBufferSize;
    }

    /**
     * @param size the pasteBufferSize to set
     */
    public void setPasteBufferSize(final int size) {
        this.pasteBufferSize = size;
    }

    /**
     * @return the scanBufferSize
     */
    public int getScanBufferSize() {
        return scanBufferSize;
    }

    /**
     * @param size the scanBufferSize to set
     */
    public void setScanBufferSize(final int size) {
        this.scanBufferSize = size;
    }

    /**
     * @return the skipBufferSize
     */
    public int getSkipBufferSize() {
        return skipBufferSize;
    }

    /**
     * @param size the skipBufferSize to set
     */
    public void size(final int size) {
        this.skipBufferSize = size;
    }

    /**
     * Get the size of the indexing queue.
     * @return The size of the queue.
     */
    public int getIndexingQueueSize() {
        return indexingQueueSize;
    }

    /**
     * Set the size of the indexing queue.
     * @param size The size of the queue.
     */
    public void setIndexingQueueSize(final int size) {
        this.indexingQueueSize = size;
        indexingQueue =
                new ArrayBlockingQueue<Sentence>(indexingQueueSize);
    }
}
