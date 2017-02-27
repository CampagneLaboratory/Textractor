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
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.Attribute;
import it.unimi.dsi.mg4j.util.parser.BulletParser;
import it.unimi.dsi.mg4j.util.parser.Element;
import it.unimi.dsi.mg4j.util.parser.ParsingFactory;
import it.unimi.dsi.mg4j.util.parser.WellFormedXmlFactory;
import it.unimi.dsi.mg4j.util.parser.callback.DefaultCallback;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.TextractorConstants;
import static textractor.TextractorConstants.TIMER_LOG;
import textractor.datamodel.Article;
import textractor.datamodel.Author;
import textractor.datamodel.OtmiArticle;
import textractor.datamodel.Sentence;
import textractor.sentence.SentenceProcessingException;
import textractor.tools.NullSentenceSplitter;
import textractor.tools.SentenceSplitter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Loader that can process pubmed/medline files into {@link Article}s and
 * {@link Sentence}s.
 */
public final class OtmiArticleLoader extends AbstractFileLoader {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(OtmiArticleLoader.class);

    /**
     * Total number of articles processed by this loader.
     */
    private int numberOfArticlesProcessed;

    /**
     * Indicates if the title of the article will be processed.
     */
    private boolean loadTitles = true;

    /**
     * Indicates if the abstract of the article will be processed.
     */
    private boolean loadAbstracts = true;

    /**
     * The {@link textractor.tools.SentenceSplitter} to use to split sentences.
     */
    private SentenceSplitter splitter = new NullSentenceSplitter();

    /**
     * The name of the {@link textractor.tools.SentenceSplitter} class to use.
     */
    private String splitterClass = NullSentenceSplitter.class.getName();

    /**
     * Create a new loader.
     */
    public OtmiArticleLoader() {
        super();
    }
    /**
     * Process a single file designated by name.
     *
     * @param filename The name of the file to process.
     * @throws IOException if there is a problem reading the file.
     */
    @Override
    public void processFilename(final String filename) throws IOException {
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

        // DO NOT TRIM the buffer. Trimming allocates a new buffer and copies the
        // result in the new one. This does in fact
        // use more memory transiently and result in more garbage collection.

        // and close up stuff we don't need anymore
        reader.close();
        stream.close();

        final ParsingFactory factory = new WellFormedXmlFactory();
        final BulletParser parser = new BulletParser(factory);

        final OtmiExtractor otmiExtractor = new OtmiExtractor(filename);
        parser.setCallback(otmiExtractor);
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
     * @param title Title of the article
     * @param abstractText The text that will be used to create the sentences
     * @return A collection of sentences that reflect the original html text
     */
    private Collection<Sentence> loadSentences(final Article article,
                                               final MutableString title, final List<String> abstractText) {

        // split the text
        final List<MutableString> splitText =
                new ObjectArrayList<MutableString>();
        for (final String text : abstractText) {
            CollectionUtils.addAll(splitText, splitter.split(text));
        }

        final Collection<Sentence> sentences;
        if (appendSentencesInOneDocument) {
            sentences = createSentencesInOneDocument(article, splitText, title);
        } else {
            sentences = createSentences(article, splitText, title);
        }
        return sentences;
    }

    /**
     * Creates a single {@link textractor.datamodel.Sentence} from an
     * {@link textractor.datamodel.Article}.
     *
     * @param article The article to process
     * @param splitText Iterator over the text in the article
     * @param title Title of the article
     * @return A collection containing a single sentence
     */
    private Collection<Sentence> createSentencesInOneDocument(
            final Article article, final List<MutableString> splitText,
            final MutableString title) {
        final MutableString fullDocumentText = new MutableString();

        if (loadTitles) {
            if (title.length() > 0) {
                // add the boundary tag and positons
                fullDocumentText.append(sentenceBoundary);
                fullDocumentText.append(title);
            } else {
                LOG.warn("Skipping empty title in article " + article.getPmid());
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
     * @param title Title of the article
     * @return A collection of sentences.
     */
    private Collection<Sentence> createSentences(final Article article,
                                                 final List<MutableString> splitText, final MutableString title) {
        int sentenceCount = 0;
        final List<Sentence> sentences = new ArrayList<Sentence>();
        if (title.length() > 0) {
            final Sentence sentence = produce(article, title);
            sentences.add(sentence);
            sentenceCount++;
        } else {
            LOG.warn("Skipping sentence due to empty title in article "
                    + article.getPmid());
        }

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
     * "Convert" raw data from Otmi articles into internal textractor
     * representation for further processing.
     *
     * @param doi identifier for the article to be processed
     * @param link URL for the article
     * @param title Title of the article
     * @param text Text of the article to be processed
     * @param additionalFieldsMap additional fields to index
     * @param filename Original name of the file that contained the article
     * the text.
     */
    private void convert(final MutableString doi, final MutableString link,
                         final MutableString title, final List<String> text,
                         final Map<String, Object> additionalFieldsMap, final String filename) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("converting otmi with doi=" + doi + " in " + filename);
        }

        final OtmiArticle article = createOtmiArticle(doi, link, filename);
        if (additionalFieldsMap != null) {
            for (final String field : additionalFieldsMap.keySet()) {
                article.setAdditionalField(field, additionalFieldsMap.get(field));
            }
        }
        final Collection<Sentence> sentences =
                loadSentences(article, title, text);
        produce(article, sentences);
    }

    /**
     * Create an {@link OtmiArticle} using the given parameters.
     * @param doi identifier for the article.
     * @param link doi link
     * @param filename Filename that the article came from
     * @return A new article.
     */
    private OtmiArticle createOtmiArticle(final MutableString doi,
                                          final MutableString link, final String filename) {
        // TODO: Use the article producer
        final OtmiArticle article = new OtmiArticle();
        article.setPmid(-1);
        article.setDoi(doi.toString());
        article.setLink(link.toString());
        article.setArticleNumber(numberOfArticlesProcessed++);
        article.setFilename(filename);
        return article;
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

    /**
     * Class used to parse xml of an OTMI document.
     */
    private final class OtmiExtractor extends DefaultCallback {
        /**
         * The text resulting from the parsing process.
         */
        private final MutableString text = new MutableString();
        private final MutableString doi = new MutableString();
        private final MutableString link = new MutableString();
        private final MutableString title = new MutableString();
        private final MutableString published = new MutableString();
        private final MutableString trimstr = new MutableString();
        private final MutableString currentAuthor = new MutableString();
        private final List<String> otmiSnippets = new ObjectArrayList<String>();

        private final List<Author> authors = new ArrayList<Author>();

        private Element authorElement;
        private Element nameElement;

        private Element entryElement;
        private Element titleElement;
        private Element idElement;
        private Element linkElement;
        private Element otmiSectionElement;
        private Element otmiRawtextElement;
        private Element otmiSnippetElement;
        private Element otmiFigureElement;
        private Element otmiPublishedElement;
        private Attribute hrefAttrib;
        private Attribute nameAttrib;

        private boolean collectId;
        private boolean collectTitle;
        private boolean collectText;
        private boolean collectPublished;
        private boolean haveRawText;

        private boolean inAuthor;
        private boolean inName;

        private final String filename;

        public OtmiExtractor(final String filename) {
            super();
            this.filename = filename;
        }

        @Override
        public boolean characters(final char[] characters, final int offset,
                                  final int length, final boolean flowBroken) {
            trimstr.length(0);
            trimstr.append(characters, offset, length);
            trimstr.trim();
            if (collectText) {
                text.append(trimstr);
            }
            if (collectTitle) {
                title.append(trimstr);
            }
            if (collectId) {
                doi.append(trimstr);
            }
            if (collectPublished) {
                published.append(trimstr);
            }
            if (inAuthor && inName) {
                currentAuthor.append(trimstr);
            }

            return true;
        }

        /**
         * Configure the parser to parse text.
         */
        @Override
        public void configure(final BulletParser parser) {
            parser.parseText(true);
            parser.parseTags(true);
            parser.parseAttributes(true);
            hrefAttrib = parser.factory.getAttribute(new MutableString("href"));
            nameAttrib = parser.factory.getAttribute(new MutableString("name"));
            parser.parseAttribute(hrefAttrib);
            parser.parseAttribute(nameAttrib);

            authorElement = parser.factory.getElement(new MutableString("atom:author"));
            nameElement = parser.factory.getElement(new MutableString("atom:name"));
            entryElement = parser.factory.getElement(new MutableString("atom:entry"));
            titleElement = parser.factory.getElement(new MutableString("atom:title"));
            idElement = parser.factory.getElement(new MutableString("atom:id"));
            linkElement = parser.factory.getElement(new MutableString("atom:link"));
            otmiSectionElement = parser.factory.getElement(new MutableString(
                    "otmi:section"));
            otmiRawtextElement = parser.factory.getElement(new MutableString(
                    "otmi:rawtext"));
            otmiSnippetElement = parser.factory.getElement(new MutableString(
                    "otmi:snippet"));
            otmiFigureElement = parser.factory.getElement(new MutableString(
                    "otmi:figure"));
            otmiPublishedElement = parser.factory.getElement(new MutableString(
                    "atom:published"));

            assert authorElement != null : "Cannot find support for arbitrary XML element names";
            assert nameElement != null : "Cannot find support for arbitrary XML element names";
            assert entryElement != null : "Cannot find support for arbitrary XML element names";
            assert titleElement != null : "Cannot find support for arbitrary XML element names";
            assert idElement != null : "Cannot find support for arbitrary XML element names";
            assert linkElement != null : "Cannot find support for arbitrary XML element names";
            assert otmiSectionElement != null
                    : "Cannot find support for arbitrary XML element names";
            assert otmiRawtextElement != null
                    : "Cannot find support for arbitrary XML element names";
            assert otmiSnippetElement != null
                    : "Cannot find support for arbitrary XML element names";
            assert otmiFigureElement != null
                    : "Cannot find support for arbitrary XML element names";
        }

        @Override
        public boolean endElement(final Element element) {
            if ((element == otmiSectionElement) || (element == otmiFigureElement)) {
                haveRawText = false;
            } else if (element == entryElement && !otmiSnippets.isEmpty()) {
                // [entry] is currently the top level. it is likely we will
                // have [entries] later, we will need to modify this...
                final Map<String, Object> additionalFieldsMap = new HashMap<String, Object>();
                additionalFieldsMap.put("pubdate", makeIntPubDate());
                additionalFieldsMap.put("authors", Author.getAuthorsIndexText(authors));
                processAbstractText(doi, link, title, otmiSnippets, additionalFieldsMap);

                doi.setLength(0);
                link.setLength(0);
                text.setLength(0);
                title.setLength(0);
                published.setLength(0);
                otmiSnippets.clear();
                inAuthor = false;
                inName = false;
                authors.clear();
                currentAuthor.setLength(0);
            } else if (element == otmiSnippetElement) {
                otmiSnippets.add(text.toString());
                text.setLength(0);
            } else if (element == authorElement) {
                inAuthor = false;
            } else if (element == nameElement) {
                inName = false;
                appendAuthor();
            }
            return true;
        }

        /**
         * Given the author names within lastName, firstName, foreName, initials
         * append the appropriate names to the string authorsIndexText.
         */
        private void appendAuthor() {
            if (currentAuthor.length() == 0) {
                // No authors
                LOG.info("!! Author not found during parsing.");
                return;
            }
            final Author newAuthor = new Author(currentAuthor.toString());
            authors.add(newAuthor);
            LOG.info("!! Appending author " + newAuthor.toString());
            currentAuthor.setLength(0);
        }

        /**
         * Process the text of this abstract.
         *
         * @param doiVal the doi of the document
         * @param linkVal the url of the document
         * @param titleVal the title of the document
         * @param textVal the text of the document
         * @param additionalFieldsMap additional fields to index
         * @return True if an article was created for this doi.
         * @throws SentenceProcessingException error processing the sentence
         */
        private boolean processAbstractText(final MutableString doiVal,
                                            final MutableString linkVal, final MutableString titleVal,
                                            final List<String> textVal, final Map<String, Object> additionalFieldsMap) {
            if (title.length() > 0 || !textVal.isEmpty()) {
                // Save association of articleId with id from document here
                // TODO: add the real date here
                convert(doiVal, linkVal, titleVal, textVal, additionalFieldsMap, filename);
                return true;
            } else {
                return false;
            }
        }

        /**
         * Convert the current date from OTMI format, such as
         * "2006-03-23T00:00:00Z" into a java.util.Date.
         * @return a Date or null if no OTMI formatted date
         * was found.
         */
        private long makeIntPubDate() {
            final String pubdate = published.toString().trim();
            if (StringUtils.isBlank(pubdate)) {
                return TextractorConstants.EMPTY_PUBLICATION_DATE;
            }
            if (pubdate.length() != 20) {
                LOG.warn(String.format("The OMTI date seemed invalid "
                        + "normal format is \"%s\", provided date was \"%s\"",
                        "2006-03-23T00:00:00Z", pubdate));
                return TextractorConstants.EMPTY_PUBLICATION_DATE;
            }
            final String year = pubdate.substring(0, 4);
            final String dash1 = pubdate.substring(4, 5);
            final String month = pubdate.substring(5, 7);
            final String dash2 = pubdate.substring(7, 8);
            final String day = pubdate.substring(8, 10);
            final String tee = pubdate.substring(10, 11);

            if ((!dash1.equals("-")) || (!dash2.equals("-")) || (!tee.equals("T"))) {
                LOG.warn(String.format("The OMTI date seemed invalid "
                        + "normal format is \"%s\", provided date was \"%s\"",
                        "2006-03-23T00:00:00Z", pubdate));
                return TextractorConstants.EMPTY_PUBLICATION_DATE;
            }
            if ((!StringUtils.isNumeric(year))
                    || (!StringUtils.isNumeric(month))
                    || (!StringUtils.isNumeric(day))) {
                LOG.warn(String.format("The OMTI date seemed invalid "
                        + "normal format is \"%s\", provided date was \"%s\"",
                        "2006-03-23T00:00:00Z", pubdate));
                return TextractorConstants.EMPTY_PUBLICATION_DATE;
            }
            return (Integer.parseInt(year) * 10000) + (Integer.parseInt(month) * 100)
                    + Integer.parseInt(day);
        }

        @Override
        public void startDocument() {
            text.length(0);
            doi.length(0);
            link.length(0);
            title.length(0);
            published.length(0);
            authors.clear();
            inAuthor = false;
            inName = false;
        }

        @Override
        public boolean startElement(final Element element, final Map attrMap) {
            collectText = false;
            collectId = false;
            collectTitle = false;
            collectPublished = false;
            final Object attr;
            if (element == entryElement) {
                LOG.trace("Starting a new article");
            } else if (element == titleElement) {
                collectTitle = true;
                LOG.trace("Going to get the title");
            } else if (element == idElement) {
                collectId = true;
                LOG.trace("Going to get the id");
            } else if (element == linkElement) {
                LOG.trace("Attributes size = " + attrMap.size());
                attr = attrMap.get(hrefAttrib);
                if (attr != null) {
                    link.append(attr.toString());
                    LOG.trace("link=" + link.toString());
                } else {
                    LOG.trace("No href attribute on <link>");
                }
            } else if (element == otmiSectionElement) {
                LOG.trace("Attributes size = " + attrMap.size());
                attr = attrMap.get(nameAttrib);
                if (attr != null) {
                    LOG.trace("secion=" + attr.toString());
                } else {
                    LOG.trace("No name attribute on <otmi:section>");
                }
            } else if (element == otmiPublishedElement) {
                collectPublished = true;
                published.setLength(0);
            } else if (element == otmiRawtextElement) {
                collectText = true;
                haveRawText = true;
                LOG.trace("Going to get rawtext");
            } else if (element == otmiSnippetElement) {
                if (!haveRawText) {
                    collectText = true;
                    LOG.trace("Going to get snippet");
                } else {
                    LOG.trace("Have rawtext, skipping snippet");
                }
            } else if (element == authorElement) {
                inAuthor = true;
            } else if (element == nameElement) {
                inName = true;
            }

            return true;
        }
    }
}
