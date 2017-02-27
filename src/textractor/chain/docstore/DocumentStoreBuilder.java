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

package textractor.chain.docstore;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.document.DocumentFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.chain.AbstractSentenceConsumer;
import textractor.database.DocumentIndexManager;
import textractor.database.IndexDetails;
import textractor.datamodel.Article;
import textractor.datamodel.OtmiArticle;
import textractor.datamodel.Sentence;
import textractor.event.sentence.SentenceProcessingCompleteEvent;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.docstore.DocumentStoreWriter;
import textractor.mg4j.docstore.StringPerDocumentWriter;
import textractor.mg4j.docstore.TermDocumentFrequencyWriter;
import textractor.mg4j.document.TextractorFieldInfo;
import textractor.mg4j.index.PositionedTerm;
import textractor.sentence.SentenceProcessingException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link org.apache.commons.chain.Command} that builds a DocumentStore.
 * Note that the current implementation requires that the mg4j index
 * already exist.
 */
public class DocumentStoreBuilder extends AbstractSentenceConsumer {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(DocumentStoreBuilder.class);

    /** An empty int array. */
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    /**
     * Number of articles processed so far.
     */
    private final AtomicInteger numberOfArticlesProcessed =
            new AtomicInteger();

    /**
     * Number of sentences processed so far.
     */
    private final AtomicInteger numberOfSentencesProcessed =
            new AtomicInteger();

    /**
     * Indicates that the process has been started.
     */
    private final AtomicBoolean buildingStarted = new AtomicBoolean();

    /**
     * The basename of the index that corresponds to this document store.
     */
    private String basename;

    /** If the document stores should be optimized. */
    private boolean optimize;

    /** If document frequencies should be written. */
    private boolean writeDocumentFrequency;

    /** The DOI writer. */
    private StringPerDocumentWriter doiWriter;

    /** The set of char sequence classes. */
    private static final Set<Class> CHAR_SEQUENCE_CLASSES = new HashSet<Class>();

    /**
     * Indicates whether or not position information will be written as part
     * of the document store.  This only applies when the sentences
     * contain postion information.
     * @see textractor.datamodel.Sentence#hasPositons()
     */
    private boolean writePositions = true;

    /** The chunk size. */
    private int chunkSize = 1000;

    /** The DocumentIndexManager we are using. */
    private DocumentIndexManager docmanager;

    /** The term document frequence writer we are using. */
    private TermDocumentFrequencyWriter termDocumentFrequencyWriter;

    /** The timer. */
    private final StopWatch timer;

    /**
     * The docStoreWriters for the indicies not named "text" but that are
     * FildType.TEXT.
     */
    private final Map<String, DocumentStoreWriter> docStoreWriters =
        new Object2ObjectOpenHashMap<String, DocumentStoreWriter>();

    /**
     * This writer is specifically for the main "text" index.
     * It is NOT stored within docStoreWriters, so it must be closed
     * separately, etc.
     */
    private DocumentStoreWriter textDocStoreWriter;

    /** If we should be writing DOI values. */
    private boolean writeDoiValues;

    /**
     * Should this document store be created with DOI information for each article?
     *
     * @return True if DOI should be stored with each article. False otherwise.
     */
    public boolean isWriteDoiValues() {
        return writeDoiValues;
    }

    /**
     * Set if we should be writing DOI values.
     * @param writeDoiValues if we should be writing DOI values.
     */
    public void setWriteDoiValues(final boolean writeDoiValues) {
        this.writeDoiValues = writeDoiValues;
    }

    /**
     * Create a new {@link org.apache.commons.chain.Command} to
     * build a Document Store.
     */
    public DocumentStoreBuilder() {
        super();
        timer = new StopWatch();
        timer.start();
    }

    /**
     * Process sentences along with their associated article.
     *
     * @param article   The article assoicated with the sentences.
     * @param sentences A collection of Sentences to process.
     * @throws SentenceProcessingException If there was an error condition
     *                                     in the textractor sentence processing pipeline
     */
    public void consume(final Article article, final Collection<Sentence> sentences)
            throws SentenceProcessingException {
        try {
            // initialize once we start getting sentences to process
            if (!buildingStarted.getAndSet(true)) {
                initialize();
            }

            final Map<String, int[]> fieldTermListMap = new
                    Object2ObjectOpenHashMap<String, int[]>();
            for (final String fieldName : docStoreWriters.keySet()) {
                final CharSequence data = asCharSequence(
                        article.getAdditionalFieldsMap().get(fieldName));
                if (data != null) {
                    fieldTermListMap.put(
                            fieldName,
                            docmanager.extractTerms(docmanager.getIndexDetails(fieldName), data));
                } else {
                    fieldTermListMap.put(fieldName, EMPTY_INT_ARRAY);
                }
            }

            for (final Sentence sentence : sentences) {
                final int docIndex =
                        numberOfSentencesProcessed.getAndIncrement();
                final int[] tokens;
                if (writePositions) {
                    // TODO - what if there aren't positions in the sentence?
                    final List<PositionedTerm> termList =
                            docmanager.extractTerms(sentence);
                    final List<IntRange> ranges =
                            new ArrayList<IntRange>(termList.size());
                    tokens = new int[termList.size()];
                    for (int i = 0; i < termList.size(); i++) {
                        final PositionedTerm term = termList.get(i);
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Term " + term.getTerm() + ": \""
                                    + term.getText() + "\" " + term.getRange());
                        }
                        tokens[i] = term.getTerm();
                        ranges.add(term.getRange());
                    }
                    textDocStoreWriter.appendPositions(ranges);
                } else {
                    tokens = docmanager.extractTerms(sentence.getText());
                }
                textDocStoreWriter.appendDocument(docIndex, tokens);

                if (LOG.isTraceEnabled()) {
                    final long articleNumber = article.getArticleNumber();
                    LOG.trace("[" + articleNumber + ", " + docIndex + "] "
                            + sentence.getText().substring(0,
                            Math.min(50, sentence.getText().length())));
                }

                for (final String fieldName : docStoreWriters.keySet()) {
                    docStoreWriters.get(fieldName).appendDocument(
                            docIndex, fieldTermListMap.get(fieldName));
                }

                // Question: Currently, OTMI documents are 1 sentence
                // per document. If, in the future, there is more than
                // 1 sentence per document will this not be appended
                // too many times?
                if (writeDoiValues) {
                    final String doi = obtainOtmiDoi(article);
                    if (doi != null) {
                        doiWriter.appendDocumentString(docIndex, doi);
                    }
                }

                if (termDocumentFrequencyWriter != null) {
                    termDocumentFrequencyWriter.appendDocument(docIndex, tokens);
                }
                textDocStoreWriter.addDocumentPMID(docIndex, article.getPmid());
                if (((docIndex + 1) % chunkSize) == 0) {
                    timer.suspend();
                    float docPerSecond = chunkSize;
                    docPerSecond /= timer.getTime();
                    docPerSecond *= 1000; // ms -> seconds
                    System.out.println("Processed " + docIndex + " textractor documents. Rate: "
                            + (docPerSecond + " document/s"));
                    timer.stop();
                    timer.reset();
                    timer.start();

                }
            }
        } catch (IOException e) {
            LOG.error("Problem with documentStoreWriter", e);
            throw new SentenceProcessingException(e);
        }
    }

    /**
     * Of obj is a CharSequence, this will return obj casted to a CharSequence.
     * Otherwise it will return a null. This uses the Set[Class] CHAR_SEQUENCE_CLASSES
     * to speed this up.
     * @param obj the object in question
     * @return the object as a CharSequence (or null)
     */
    public static CharSequence asCharSequence(final Object obj) {
        if (obj == null) {
            return null;
        }
        final Class objClass = obj.getClass();
        if (CHAR_SEQUENCE_CLASSES.contains(objClass)) {
            return (CharSequence) obj;
        }
        for (final Class interfaceClass : objClass.getInterfaces()) {
            if (interfaceClass == CharSequence.class) {
                CHAR_SEQUENCE_CLASSES.add(objClass);
                return (CharSequence) obj;
            }
        }
        return null;
    }

    /**
     * For a given article, return the DOI of the article is an
     * OtmiArticle. Otherwise return null.
     * @param article the article to obtain the DOI for
     * @return the DOI or null
     */
    private String obtainOtmiDoi(final Article article) {
        if (!(article instanceof OtmiArticle)) {
            return null;
        }

        // Get the otmi doi
        final OtmiArticle otmiArticle = (OtmiArticle) article;
        return otmiArticle.getDoi();
    }

    /**
     * Start the document store builder process.
     * @throws IOException output error
     */
    private void initialize() throws IOException {
        final StopWatch initTimer = new StopWatch();
        initTimer.start();

        // initialize the document index manager here too so that
        // problems are caught early on.
        final List<TextractorFieldInfo> indexFields;
        try {
            docmanager = new DocumentIndexManager(basename);
            indexFields = docmanager.getIndexFields();
        } catch (ConfigurationException e) {
            throw new IOException(e);
        }

        LOG.debug("Initializing hash term map");
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));
        initTimer.stop();
        LOG.debug("Term map installed in " + initTimer.toString());

        if (indexFields != null) {
            for (final TextractorFieldInfo fieldInfo : indexFields) {
                if (fieldInfo.getType() == DocumentFactory.FieldType.TEXT) {
                    final IndexDetails fieldIndexDetails =
                            docmanager.getIndexDetails(fieldInfo.getName());
                    if (fieldIndexDetails != null) {
                        if (fieldInfo.getName().equals("text")) {
                            textDocStoreWriter = new DocumentStoreWriter(
                                    fieldIndexDetails, true, writePositions);
                        } else {
                            docStoreWriters.put(fieldInfo.getName(),
                                    new DocumentStoreWriter(fieldIndexDetails, false, false));
                        }
                    }
                }
            }
        }

        if (writeDoiValues) {
            doiWriter = new StringPerDocumentWriter(docmanager, "otmi-dois");
        }

        if (writeDocumentFrequency) {
            termDocumentFrequencyWriter =
                    new TermDocumentFrequencyWriter(docmanager, 0.1f, 0.5f);
        }

        if (optimize) {
            initTimer.reset();
            initTimer.start();
            LOG.debug("Starting to optimize term ordering (recoding by decreasing frequency)");

            for (final String fieldName : docStoreWriters.keySet()) {
                docStoreWriters.get(fieldName).optimizeTermOrdering();
            }
            textDocStoreWriter.optimizeTermOrdering();
            initTimer.stop();
            LOG.debug("Optimization completed in " + initTimer.toString());
        }
    }

    /**
     * Get the number of articles processed so far.
     *
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed.get();
    }

    /**
     * Get the number of sentences processed so far.
     *
     * @return The number of sentences processed so far
     */
    public int getNumberOfSentencesProcessed() {
        return numberOfSentencesProcessed.get();
    }

    /**
     * Get if optimize is set.
     * @return if optimize is set
     */
    public boolean isOptimize() {
        return optimize;
    }

    /**
     * Set if optimize is set.
     * @param optimize if optimize is set
     */
    public void setOptimize(final boolean optimize) {
        this.optimize = optimize;
    }

    /**
     * Get the basename.
     * @return the basename
     */
    public String getBasename() {
        return basename;
    }

    /**
     * Set the basename.
     * @param name name the basename to set
     */
    public void setBasename(final String name) {
        this.basename = name;
    }

    /**
     * Get the chunk size.
     * @return the chunk size
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Set the chunk size.
     * @param chunkSize chunkSize the chunk size
     */
    public void setChunkSize(final int chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     * Indicates whether or not position information will be written as part
     * of the document store.
     * @return true if positions should be written when available
     */
    public boolean isWritePositions() {
        return writePositions;
    }

    /**
     * Indicate whether position information (if available) should be written
     * as part of the document store.
     * @param write true if position information should be written,
     * false otherwise
     */
    public void setWritePositions(final boolean write) {
        this.writePositions = write;
    }

    /**
     * Get if we are writing document frequency.
     * @return if we are writing document frequency.
     */
    public boolean isWriteDocumentFrequency() {
        return writeDocumentFrequency;
    }

    /**
     * Set if we are writing document frequency.
     * @param writeDocumentFrequency if we are writing document frequency.
     */
    public void setWriteDocumentFrequency(final boolean writeDocumentFrequency) {
        this.writeDocumentFrequency = writeDocumentFrequency;
    }

    /**
     * This method gets called when a sentence processing is complete. Close streams,
     * etc.
     *
     * @param event A {@link textractor.event.sentence.SentenceProcessingCompleteEvent}
     * object describing the event source.
     */
    @Override
    public void processingComplete(final SentenceProcessingCompleteEvent event) {
        LOG.debug("Got completion from: " + event.getSource());
        super.processingComplete(event);
        // close everything up if we are done
        if (event.getSource() == this) {
            LOG.info("Number of Articles: " + numberOfSentencesProcessed.get());
            LOG.info("Number of Sentences: " + numberOfSentencesProcessed.get());
            try {
                LOG.info("Writing final pmids");
                textDocStoreWriter.writePMIDs();

                LOG.info("Closing the document stores");
                for (final DocumentStoreWriter docStoreWriter : docStoreWriters.values()) {
                    docStoreWriter.close();
                }
                textDocStoreWriter.close();

                docStoreWriters.clear();
                textDocStoreWriter = null;

                if (doiWriter != null) {
                    doiWriter.close();
                }

                if (termDocumentFrequencyWriter != null) {
                    LOG.info("Closing the document frequency writer");
                    termDocumentFrequencyWriter.close();
                    final PrintWriter pw = new PrintWriter(System.out);
                    termDocumentFrequencyWriter.printStatistics(pw);
                    pw.flush();
                }

                docmanager.close();
            } catch (IOException e) {
                LOG.error("Couldn't complete document store properly", e);
            }
        }
    }
}
