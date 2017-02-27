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

import edu.cornell.med.icb.ncbi.pubmed.PubMedInfoTool;
import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.BulletParser;
import it.unimi.dsi.mg4j.util.parser.ParsingFactory;
import it.unimi.dsi.mg4j.util.parser.WellFormedXmlFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static textractor.TextractorConstants.TIMER_LOG;
import textractor.article.ArticleProducer;
import textractor.article.DefaultArticleProducer;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.parsers.PubmedExtractor;
import textractor.parsers.PubmedLoadExtractor;
import textractor.parsers.PubmedLoader;
import textractor.tools.DefaultSentenceSplitter;
import textractor.tools.SentenceSplitter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Loader that can process pubmed/medline files into {@link Article}s and
 * {@link Sentence}s.  This class is not (yet) thread-safe.
 */
public final class PubmedArticleLoader extends AbstractFileLoader implements PubmedLoader {

    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(PubmedArticleLoader.class);

    /**
     * Name of the element that defines an article.
     */
    private String articleElementName;

    /**
     * Total number of articles processed by this loader.
     */
    private int numberOfArticlesProcessed;

    /**
     * The article producer to use.
     * TODO: This needs to be dynamic
     */
    private final ArticleProducer articleProducer = new DefaultArticleProducer();

    /**
     * Indicates whether or not retractions should be handled.
     */
    private boolean processRetractions = true;

    /**
     * Article map used to process retractions.
     */
    private Long2ByteOpenHashMap retractionMap = new Long2ByteOpenHashMap();

    /**
     * Name of the file to serialize the retraction map to so that it
     * doesn't need to be regenerated each time.
     */
    private String retractionMapFile = "retractionMap.ser";

    /**
     * Indicates that this load will create a reaction map file.
     */
    private boolean writeRetractionMap = true;

    /**
     * Indicates if the title of the article will be processed.
     */
    private boolean loadTitles = true;

    /**
     * Indicates if the abstract of the article will be processed.
     */
    private boolean loadAbstracts = true;

    /**
     * The actual text of an article.
     * TODO: reusing this is NOT thread-safe - it needs to be method local
     */
    private final MutableString fullDocumentText = new MutableString();

    /**
     * Create a new loader.
     */
    public PubmedArticleLoader() {
        super();

        // assume retractions will be processed
        numberOfIterations = 2;

        // but set the default value to 1 which will force all articles to
        // be loaded if retractions aren't handled
        retractionMap.defaultReturnValue((byte) 1);
    }

    /**
     * Process a single file designated by name.
     * @param filename The name of the file to process.
     * @throws IOException if there is a problem reading the file.
     */
    @Override
    public void processFilename(final String filename) throws IOException {
        if (processRetractions && !writeRetractionMap
                && numberOfIterations > 1 && currentIteration == 1) {
            // TODO: clean up this logic - it's messy :-(
            return;
        }
        final StopWatch timer = new StopWatch();
        LOG.info("Scanning " + filename);
        // reset local counter
        numberOfArticlesProcessed = 0;
        timer.start();

        final InputStream stream;
        if (filename.endsWith(".gz") || filename.endsWith(".zip")) {
            stream = new GZIPInputStream(new FileInputStream(filename));
        } else {
            stream = new FileInputStream(filename);
        }

        final FastBufferedReader reader =
                new FastBufferedReader(new InputStreamReader(stream, "UTF-8"));
        char[] buffer = new char[10000];

        // read the whole file in memory:
        int length;
        int offset = 0;

        try {
            while ((length = reader.read(buffer, offset, buffer.length - offset)) > 0) {
                offset += length;
                buffer = CharArrays.grow(buffer, offset + 1);
            }

            // DO NOT TRIM the buffer. Trimming allocates a new buffer and
            // copies the result in the new one. This does in fact use more
            // memory transiently and result in more garbage collection.
        } finally {
            // and close up stuff we don't need anymore
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(stream);
        }

        final ParsingFactory factory = new WellFormedXmlFactory();
        final BulletParser parser = new BulletParser(factory);

        // determine which parser to use depending on whether or not
        // retractions are being processed
        final PubmedExtractor pubmedExtractor;
        if ((processRetractions && currentIteration == 2)
                || !processRetractions) {
            // parse the text of the articles
            pubmedExtractor =  new PubmedLoadExtractor(this, filename);
        } else {
            // pre-process pmids for retractions and updates
            pubmedExtractor = new PmidExtractor();
        }

        if (articleElementName != null) {
            pubmedExtractor.setArticleElementName(articleElementName);
            if (LOG.isInfoEnabled()) {
                LOG.info("Parsing elements " + articleElementName);
            }
        }

        parser.setCallback(pubmedExtractor);

        // parse and collect abbreviations:
        // TODO: begin();

        parser.parse(buffer, 0, offset);
        // TODO: end();

        timer.stop();
        if (TIMER_LOG.isInfoEnabled()) {
            TIMER_LOG.info(timer +  " : " + filename);
        }

        if (LOG.isInfoEnabled()) {
            // count from the producer is from the start of the run
            LOG.info("Loaded " + numberOfArticlesProcessed + " abstracts (cumulative: "
                    + articleProducer.getNumberOfArticlesProcessed() + ")");
        }
    }

    /**
     * Get the number of articles processed so far.
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed;
    }

    /**
     * Convert an article and associated text into individual senetence
     * objects.
     *
     * @param article The article to process
     * @param title The title of the article
     * @param text The text (i.e., abstract) of the article.
     * @return A collection of sentences that make up the article
     */
    private Collection<Sentence> loadSentences(final Article article,
            final String title, final String text) {
        final Collection<Sentence> sentences;
        final SentenceSplitter splitter = new DefaultSentenceSplitter();

        // TODO: Handle punctuation setting
        final Iterator<MutableString> splittedText = splitter.split(text);
        if (appendSentencesInOneDocument) {
            sentences = createSentences(article, splittedText, title);
        } else {
            sentences = createSentenceOneDocument(article, splittedText, title);
        }
        return sentences;
    }

    /**
     * Index by sentence. Each sentence in the article gets
     * indexed independently.
     * @param article the article to index
     * @param sentencesAsTextIterator the setences in the article
     * @param title the title of the article
     * @return the List of setences in the article
     */
    private Collection<Sentence> createSentences(final Article article,
            final Iterator<MutableString> sentencesAsTextIterator, final String title) {
        int sentenceCount = 0;
        final List<Sentence> sentences = new ArrayList<Sentence>();
        // load the title
        if (loadTitles) {
            if (title.length() > 0) {
                final Sentence sentence = produce(article, title);
                sentences.add(sentence);
                sentenceCount++;
            } else {
                LOG.warn("Skipping sentence due to empty title in article " + article.getPmid());
            }
        }

        // load the abstract
        if (loadAbstracts) {
            while (sentencesAsTextIterator.hasNext()) {
                final MutableString text = sentencesAsTextIterator.next();
                final Sentence sentence = produce(article, text);
                sentences.add(sentence);
                sentenceCount++;
            }
        }

        if (sentenceCount == 0) {
            LOG.warn("Article " + article.getPmid() + " has no sentences");
        }

        return sentences;
    }

    /**
     * Index by article. Each sentence in the article gets
     * merged and they are indexed together.
     * @param article the article to index
     * @param sentencesAsTextIterator the setences in the article
     * @param title the title of the article
     * @return the List of setences in the article, but there will just
     * be one item in the list.
     */
    private List<Sentence> createSentenceOneDocument(final Article article,
            final Iterator<MutableString> sentencesAsTextIterator, final String title) {

        int sentenceCount = 0;
        final List<Sentence> sentences = new ArrayList<Sentence>();

        fullDocumentText.setLength(0);
        fullDocumentText.append(sentenceBoundary);
        if (loadTitles) {
            if (title.length() > 0) {
                fullDocumentText.append(title);
                fullDocumentText.append(sentenceBoundary);
                sentenceCount++;
            } else {
                LOG.warn("Skipping sentence due to empty title in article " + article.getPmid());
            }
        }

        if (loadAbstracts) {
            while (sentencesAsTextIterator.hasNext()) {
                fullDocumentText.append(sentencesAsTextIterator.next());
                fullDocumentText.append(sentenceBoundary);
                sentenceCount++;
            }
        }

        if (sentenceCount == 0) {
            LOG.warn("Article " + article.getPmid() + " has no sentences");
        } else {
            final Sentence sentence = produce(article, fullDocumentText);
            sentences.add(sentence);
        }

        return sentences;
    }

    /**
     * "Convert" raw data from pubmed articles into internal textractor
     * representation for further processing.
     *
     * @param pmidString PubMed identifier for the article to be processed
     * @param title Title of the article
     * @param text Text of the article to be processed
     * @param additionalFieldsMap additional fields to index
     * @param filename Original name of the file that contained the article
     */
    public void convert(
            final MutableString pmidString, final MutableString title, final MutableString text,
            final Map<String, Object> additionalFieldsMap, final String filename) {
        final long pmid = Long.parseLong(pmidString.toString());

        if (LOG.isDebugEnabled()) {
            LOG.debug("converting " + pmid + " in " + filename);
        }

        if (decrementArticleCount(pmid) == 0) {
            final Article article = createArticle(pmid, filename);
            if (additionalFieldsMap != null) {
                for (final Map.Entry<String, Object> entry : additionalFieldsMap.entrySet()) {
                    article.setAdditionalField(entry.getKey(), entry.getValue());
                }
            }
            article.setLink(PubMedInfoTool.pubmedUriFromPmid((int) pmid));
            final Collection<Sentence> sentences =
                    loadSentences(article, title.toString(), text.toString());
            produce(article, sentences);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping " + pmid + ", count = "
                        + retractionMap.get(pmid));
            }
        }
    }

    /**
     * Create an {@link Article} using the given parameters.
     * @param pmid Pubmed identifier for the article.
     * @param filename Filename that the article came from
     * @return A new article.
     */
    public Article createArticle(final long pmid, final String filename) {
        final Article article = articleProducer.createArticle();
        article.setPmid(pmid);
        article.setFilename(filename);
        numberOfArticlesProcessed++;
        return article;
    }

    /**
     * Removes an article from the collection of articles.  Most likely this
     * is because the article has been retracted.
     *
     * @param pmid The id of the file
     */
    public void removeArticle(final String pmid) {
        // TODO
    }

    /**
     * @return the articleElementName
     */
    public String getArticleElementName() {
        return articleElementName;
    }

    /**
     * @param elementName the name of the article element in the file
     */
    public void setArticleElementName(final String elementName) {
        this.articleElementName = elementName;
    }

    /**
     * Are retractions being handled?
     * @return true if retractions are being handled
     */
    public boolean isProcessRetractions() {
        return processRetractions;
    }

    /**
     * Indicates whether or not retractions should be handled.
     * @param process true if retractions should be handled
     */
    public void setProcessRetractions(final boolean process) {
        this.processRetractions = process;
        // if we are processing retractions, then this implementation
        // must perform two iterations over the files in order to
        // build a retraction list
        if (processRetractions) {
            numberOfIterations = 2;
        } else {
            numberOfIterations = 1;
        }
    }

    /**
     * Get the name of the file to serialize the retraction map to.
     * @return The name of the file that holds the retraction information
     */
    public String getRetractionMapFile() {
        return retractionMapFile;
    }

    /**
     * Set the name of the file to serialize the retraction map to.
     * @param filename Name of the file that holds the retraction information
     */
    public void setRetractionMapFile(final String filename) {
        this.retractionMapFile = filename;
    }

    /**
     * Does this load will create a reaction map file?
     * @return true of the retraction map will be written, false otherwise
     */
    public boolean isWriteRetractionMap() {
        return writeRetractionMap;
    }

    /**
     * Should this load will create a reaction map file?
     * @param writeMap true of the retraction map should be written,
     * false otherwise
     */
    public void setWriteRetractionMap(final boolean writeMap) {
        this.writeRetractionMap = writeMap;
    }

    /**
     * Do the titles of articles get processed?
     * @return true if titles are to be loaded, false otherwise
     */
    public boolean isLoadTitles() {
        return loadTitles;
    }

    /**
     * Should the titles of articles will be processed?
     * @param load true if titles should be loaded, false otherwise
     */
    public void setLoadTitles(final boolean load) {
        this.loadTitles = load;
    }


    /**
     * Does the abstract text of articles get processed?
     * @return true if abstracts are to be loaded, false otherwise
     */
    public boolean isLoadAbstracts() {
        return loadAbstracts;
    }

    /**
     * Should the abstract text of articles will be processed?
     * @param load true if abstracts should be loaded, false otherwise
     */
    public void setLoadAbstracts(final boolean load) {
        this.loadAbstracts = load;
    }

    private int decrementArticleCount(final long pmid) {
        final int newCount = retractionMap.get(pmid) - 1;
        assert newCount >= 0 : "Count should never be less than 0";

        // only put a value back if there is a pmid there already
        if (retractionMap.containsKey(pmid)) {
            retractionMap.put(pmid, (byte) newCount);
        }
        return newCount;
    }

    /**
     * Called at the start of an iteration.
     * @param iteration The iteration that just completed
     */
    @Override
    protected void beginIteration(final int iteration) {
        LOG.debug("Starting iteration " + iteration);
        // load the retraction map unless we wrote one
        if (processRetractions && !writeRetractionMap && iteration == 1) {
            if (StringUtils.isNotBlank(retractionMapFile)) {
                try {
                    retractionMap = (Long2ByteOpenHashMap) BinIO.loadObject(retractionMapFile);
                } catch (IOException e) {
                    LOG.error("Couldn't store " + retractionMapFile, e);
                } catch (ClassNotFoundException e) {
                    LOG.error("Couldn't store " + retractionMapFile, e);
                }
            } else {
                LOG.error("Retraction map file has not been specified");
            }
        }
        super.beginIteration(iteration);
    }

    /**
     * Called at the end of an iteration.
     * @param iteration The iteration that just completed
     */
    @Override
    protected void endIteration(final int iteration) {
        LOG.debug("EndingIteration " + iteration);
        if (processRetractions && iteration == 1) {
            // "squish" the retractionMap as best we can
            final Iterator<Map.Entry<Long, Byte>> entries = retractionMap.entrySet().iterator();
            while (entries.hasNext()) {
                final Map.Entry<Long, Byte> entry = entries.next();
                // we can remove anything that has only been seen once
                if (entry.getValue() == 1) {
                    entries.remove();
                }
            }
            final boolean trimmed = retractionMap.trim(retractionMap.size());
            if (!trimmed) {
                LOG.warn("Couldn't trim the article count map");
            }

            if (writeRetractionMap) {
                if (StringUtils.isNotBlank(retractionMapFile)) {
                    try {
                        BinIO.storeObject(retractionMap, retractionMapFile);
                    } catch (IOException e) {
                        LOG.error("Couldn't store " + retractionMapFile, e);
                    }
                } else {
                    LOG.error("Retraction map file has not been specified");
                }
            }
        }
        super.endIteration(iteration);
    }

    /**
     * A {@link textractor.parsers.PubmedExtractor} that collects PMIDs in
     * order to process retraction notices properly.
     */
    private class PmidExtractor extends PubmedExtractor {
        /**
         * Process the text of this article.
         *
         * @param pmidString Pubmed identifier of the article
         * @param title Title of the article
         * @param text Text of the article
         * @param additionalFieldsMap additional fields to index
         * @return True if an article was created for this PMID
         */
        @Override
        public boolean processAbstractText(final MutableString pmidString,
                final MutableString title, final MutableString text,
                final Map<String, Object> additionalFieldsMap) {
            incrementArticleCount(pmidString);
            return false;
        }

        /**
         * Process retraction notices.
         *
         * @param pmidString Pubmed identifier of the article
         * @param retractedPmidStrings Pubmed identifiers of the articles
         * to retract
         * @param createArticle  True if this method may create an article to
         *                       represent the retraction notice. False if
         */
        @Override
        public void processNoticeOfRetraction(final MutableString pmidString,
                                              final List<String> retractedPmidStrings,
                                              final boolean createArticle) {
            assert retractedPmidStrings != null;
            if (createArticle) {
                incrementArticleCount(pmidString);
            }
            for (final String retractedPmidString : retractedPmidStrings) {
                final long retractedPmid = Long.parseLong(retractedPmidString);
                if (!retractionMap.containsKey(retractedPmid)) {
                    LOG.error("Retracted unknown article: " + retractedPmid);
                }
                retractionMap.put(retractedPmid, Byte.MAX_VALUE);
            }
        }

        /**
         * Update the retraction map whenever an article is retracted.
         * @param pmidString Pubmed identifier of the retracted article
         */
        private void incrementArticleCount(final MutableString pmidString) {
            final long pmid = Long.parseLong(pmidString.toString());
            if (retractionMap.containsKey(pmid)) {
                // the article was already processed at least once
                // so we need to use it's previous count
                final byte count = retractionMap.get(pmid);
                retractionMap.put(pmid, (byte) (count + 1));
            } else {
                // this is the first time we've seen this article
                retractionMap.put(pmid, (byte) 1);
            }
        }
    }
}
