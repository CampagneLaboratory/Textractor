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

import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.Fast;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.BulletParser;
import it.unimi.dsi.mg4j.util.parser.ParsingFactory;
import it.unimi.dsi.mg4j.util.parser.WellFormedXmlFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static textractor.TextractorConstants.TIMER_LOG;
import textractor.datamodel.Article;
import textractor.datamodel.OmimArticle;
import textractor.datamodel.Sentence;
import textractor.tools.DefaultSentenceSplitter;
import textractor.tools.SentenceSplitter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Loader that can process pubmed/medline files into {@link Article}s and
 * {@link Sentence}s.
 */
public final class OmimArticleLoader extends AbstractFileLoader implements ParsedArticleHandler {

    private static final Runtime RUNTIME = Runtime.getRuntime();

    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(OmimArticleLoader.class);

    /**
     * Total number of articles processed by this loader.
     */
    private int numberOfArticlesProcessed;

    /**
     * Indicates if the title of the article will be processed.
     */
    private boolean loadTitles = true;

    /**
     * Indicates if the aliases of the article will be processed.
     */
    private boolean loadAliases = true;

    /**
     * Indicates if the abstract of the article will be processed.
     */
    private boolean loadAbstracts = true;

    /**
     * If true abstracts will not be "loaded" but instead
     * just load a blank abstract. This should NOT be
     * changed to true unless you know what you are doing.
     */
    private boolean skipAbstracts;

    /**
     * The {@link textractor.tools.SentenceSplitter} to use to split sentences.
     */
    private SentenceSplitter splitter = new DefaultSentenceSplitter();

    /**
     * The name of the {@link textractor.tools.SentenceSplitter} class to use.
     */
    private String splitterClass = DefaultSentenceSplitter.class.getName();

    private final BulletParser parser;
    private final OmimExtractor omimExtractor;

    /**
     * Create a new loader.
     */
    public OmimArticleLoader() {
        super();
        final ParsingFactory factory = new WellFormedXmlFactory();
        parser = new BulletParser(factory);
        omimExtractor = new OmimExtractor(this);
    }
    /**
     * Process a single file designated by name.
     *
     * @param filename The name of the file to process.
     * @throws IOException if there is a problem reading the file.
     */
    @Override
    public void processFilename(final String filename) throws IOException {

        // Only process XML files
        if (!filename.toLowerCase().endsWith(".xml")) {
            return;
        }

        LOG.info("Scanning " + filename);
        final StopWatch timer = new StopWatch();
        timer.start();

        final InputStream stream;
        if (filename.endsWith(".gz") || filename.endsWith(".zip")) {
            stream = new GZIPInputStream(new FileInputStream(filename));
        } else {
            stream = new FileInputStream(filename);
        }

        final FastBufferedReader reader = new FastBufferedReader(
                new InputStreamReader(stream, "UTF-8"));

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
        stream.close();

        omimExtractor.resetExtractor(filename);
        parser.setCallback(omimExtractor);
        parser.parse(buffer, 0, offset);

        timer.stop();
        if (TIMER_LOG.isInfoEnabled()) {
            TIMER_LOG.info(timer +  " : " + filename);
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("Loaded 1 abstract (cumulative: "
                    + numberOfArticlesProcessed + ") ");
        }
    }

    /**
     * Creates a collection of sentences from the original text of the html
     * article.
     *
     * @param article The article that the new sentences will be associated with
     * @param abstractText The text that will be used to create the sentences
     * @return A collection of senteneces that reflect the original html text
     */
    private Collection<Sentence> loadSentences(final OmimArticle article,
            final MutableString abstractText) {

        LOG.info(String.format("loadSentences for %s avail/free/total/max mem: %s/%s/%s/%s",
                article.getFilename(),
                Fast.formatSize(
                        RUNTIME.freeMemory() +
                        RUNTIME.maxMemory() -
                        RUNTIME.totalMemory()),
                Fast.formatSize(RUNTIME.freeMemory()),
                Fast.formatSize(RUNTIME.totalMemory()),
                Fast.formatSize(RUNTIME.maxMemory())));

        if (skipAbstracts) {
            return new ArrayList<Sentence>();
        }

        // split the text
        final List<MutableString> splitText =
                new ObjectArrayList<MutableString>();
        CollectionUtils.addAll(splitText, splitter.split(abstractText.toString()));

        final Collection<Sentence> sentences;
        if (appendSentencesInOneDocument) {
            sentences = createSentencesInOneDocument(article, splitText);
        } else {
            sentences = createSentences(article, splitText);
        }
        return sentences;
    }

    /**
     * Creates a single {@link textractor.datamodel.Sentence} from an
     * {@link textractor.datamodel.Article}.
     *
     * @param article The article to process
     * @param splitText Iterator over the text in the article
     * @return A collection containing a single sentence
     */
    private Collection<Sentence> createSentencesInOneDocument(
                final OmimArticle article, final List<MutableString> splitText) {
        final MutableString fullDocumentText = new MutableString();

        if (loadTitles) {
            if (article.getTitle().length() > 0) {
                // add the boundary tag and positons
                fullDocumentText.append(sentenceBoundary);
                fullDocumentText.append(article.getTitle());
            } else {
                LOG.warn("Skipping empty title in article " + article.getPmid());
            }
        }

        if (loadAliases) {
            if (article.getAliases() != null) {
                for (final String alias : article.getAliases()) {
                    if (alias.length() > 0) {
                        fullDocumentText.append(sentenceBoundary);
                        fullDocumentText.append("Alias: ");
                        fullDocumentText.append(alias);
                    }
                }
            }
        }

        if (loadAbstracts) {
            // iterate over the text and append them to the document
            for (final MutableString text : splitText) {
                // add the boundary tag and positons
                fullDocumentText.append(sentenceBoundary);

                // and add the text
                fullDocumentText.append(text);
            }
        }

        // add a closing tag if there was anything added to the document
        if (fullDocumentText.length() > 0) {
            fullDocumentText.append(sentenceBoundary);
        }

        final List<Sentence> sentences = new ArrayList<Sentence>();
        final Sentence sentence = produce(article, fullDocumentText);
        sentences.add(sentence);
        return sentences;
    }

    /**
     * Creates {@link textractor.datamodel.Sentence}s from an
     * {@link textractor.datamodel.Article}.
     *
     * @param article The article to process
     * @param splitText Iterator over the text in the article
     * @return A collection of sentences.
     */
    private Collection<Sentence> createSentences(final OmimArticle article,
            final List<MutableString> splitText) {
        int sentenceCount = 0;
        final List<Sentence> sentences = new ArrayList<Sentence>();
        if (article.getTitle().length() > 0) {
            final Sentence sentence = produce(article, article.getTitle());
            sentences.add(sentence);
            sentenceCount++;
        } else {
            LOG.warn("Skipping sentence due to empty title in article "
                    + article.getPmid());
        }

        // Aliases are not added here in any case as it doesn't make
        // sense to add them. In fact splitting, splitting at sentence
        // probably doesn't make sense for OMIM at all ?

        for (final MutableString text : splitText) {
            final Sentence sentence = produce(article, text);
            sentences.add(sentence);
            sentenceCount++;
        }

        if (sentenceCount == 0) {
            LOG.warn("Article " + article.getPmid() + " has no sentences");
        }

        return sentences;
    }

    /**
     * Get the number of articles processed so far.
     *
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed;
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
     * Should we skip the abstracts altogether?
     * Do NOT set to true unless you are sure of what
     * you are doing.
     * @return true if abstracts will be skipped.
     */
    public boolean isSkipAbstracts() {
        return skipAbstracts;
    }

    /**
     * Should we skip the abstracts altogether?
     * Do NOT set to true unless you are sure of what
     * you are doing.
     * @param skipAbstracts if abstracts will be skipped.
     */
    public void setSkipAbstracts(final boolean skipAbstracts) {
        this.skipAbstracts = skipAbstracts;
        omimExtractor.setSkipAbstracts(skipAbstracts);
    }

    /**
     * Do the aliases of articles get processed?
     * @return true if alises are to be loaded, false otherwise
     */
    public boolean isLoadAliases() {
        return loadAliases;
    }

    /**
     * Should the aliases of articles will be processed?
     * @param load true if aliases should be loaded, false otherwise
     */
    public void setLoadAliases(final boolean load) {
        this.loadAliases = load;
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

    public String getSplitterClass() {
        return splitterClass;
    }

    public void setSplitterClass(final String classname)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        assert classname != null : "Can't set a null classname";
        this.splitterClass = classname;

        // load and create the class now
        final Class clazz = Class.forName(classname);
        this.splitter = (SentenceSplitter) clazz.newInstance();
    }

    public SentenceSplitter getSplitter() {
        return splitter;
    }

    public void setSplitter(final SentenceSplitter splitter) {
        assert splitter != null : "Can't set a null splitter";
        this.splitter = splitter;
        this.splitterClass = splitter.getClass().getName();
    }

    public void articleParsed(final Article article, final MutableString abstractText) {
        final OmimArticle omimArticle;
        if (article instanceof OmimArticle) {
            omimArticle = (OmimArticle)article;
        } else {
            return;
        }
        omimArticle.setArticleNumber(numberOfArticlesProcessed++);

        if (LOG.isDebugEnabled()) {
            LOG.debug("converting omim with mim-number=" +
                    omimArticle.getPmid() + " in " +
                    omimArticle.getFilename());
        }

        final Collection<Sentence> sentences =
                loadSentences(omimArticle, abstractText);
        produce(omimArticle, sentences);

    }

}
