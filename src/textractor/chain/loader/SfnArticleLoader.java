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

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.BulletParser;
import it.unimi.dsi.mg4j.util.parser.Element;
import it.unimi.dsi.mg4j.util.parser.WellFormedXmlFactory;
import it.unimi.dsi.mg4j.util.parser.callback.DefaultCallback;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.TextractorConstants;
import static textractor.TextractorConstants.TIMER_LOG;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.datamodel.SfnArticle;
import textractor.tools.DefaultSentenceSplitter;
import textractor.tools.SentenceSplitter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class SfnArticleLoader extends AbstractFileLoader {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(SfnArticleLoader.class);

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
    private SentenceSplitter splitter = new DefaultSentenceSplitter();

    /**
     * The name of the {@link textractor.tools.SentenceSplitter} class to use.
     */
    private String splitterClass = DefaultSentenceSplitter.class.getName();

    /**
     * Create a new loader.
     */
    public SfnArticleLoader() {
        super();
    }

    /**
     * Process a single file designated by name.
     *
     * @param filename The name of the file to process.
     * @throws java.io.IOException if there is a problem reading the file.
     */
    @Override
    public final void processFilename(final String filename) throws IOException {
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

        final BulletParser parser =
                new BulletParser(new WellFormedXmlFactory());
        parser.setCallback(new SfnExtractor());
        parser.parse(buffer, 0, offset);

        timer.stop();
        if (TIMER_LOG.isInfoEnabled()) {
            TIMER_LOG.info(timer +  " : " + filename);
        }
    }

    /**
     * Get the number of articles processed so far.
     *
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed;
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
     * Class used to parse xml of a Sfn document.
     */
    private class SfnExtractor extends DefaultCallback {
        /**
         * The current article being extracted.
         */
        private SfnArticle article;

        private final Stack<Element> elementStack =
                new ObjectArrayList<Element>();

        /**
         * Element (AdvancedSearch) containing an individual article.
         */
        private Element articleElement;
        /**
         * Element (ControlNumber) containing the control number of an article.
         */
        private Element controlNumberElement;
        /**
         * Element (AbstractBody) containing the abstract of an article.
         */
        private Element abstractBodyElement;
        /**
         * Element (AuthorBlock) containing the authors of an article.
         */
        private Element authorBlockElement;
        /**
         * Element (PublishingTitle) containing the publishing title of an article.
         */
        private Element publishingTitleElement;
        /**
         * Element (SessionTitle) containing the session title of an article.
         */
        private Element sessionTitleElement;
        /**
         * Element (SessionNumber) containing the session number of an article.
         */
        private Element sessionNumberElement;
        /**
         * Element (PresentationNumber) containing the presentation number of an article.
         */
        private Element presentationNumberElement;
        /**
         * Element (SessionDescription) containing the session description of
         * an article.
         */
        private Element sessionDescriptionElement;
        /**
         * Element (SessionStart) containing the session start time of
         * an article.
         */
        private Element sessionStartElement;

        /**
         * Used to parse date fields in the xml.
         */
        final DateFormat isoDateFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        /**
         * Used to store date fields.
         */
        final DateFormat longDateFormat = new SimpleDateFormat("yyyyMMdd");

        private final MutableString trimstr = new MutableString();
        private final MutableString abstractText = new MutableString();

        public SfnExtractor() {
            super();
        }

        private String appendText(final String text,
                                  final char[] characters, final int offset,
                                  final int length) {
            trimstr.length(0);
            if (StringUtils.isNotEmpty(text)) {
                trimstr.append(text);
            }
            trimstr.append(characters, offset, length);
            trimstr.trim();

            return trimstr.toString();
        }

        @Override
        public boolean characters(final char[] characters, final int offset,
                final int length, final boolean flowBroken) {
            if (!elementStack.isEmpty()) {
                final Element currentElement = elementStack.top();

                if (currentElement == controlNumberElement) {
                    trimstr.length(0);
                    trimstr.append(characters, offset, length);
                    trimstr.trim();
                    article.setControlNumber(Long.parseLong(trimstr.toString()));
                } else if (currentElement == sessionTitleElement) {
                    final String text = appendWithoutDuplication(article.getTitle(),
                            appendText("", characters, offset, length), " ");
                    article.setTitle(text);
                } else if (currentElement == publishingTitleElement) {
                    final String text = appendWithoutDuplication(article.getTitle(),
                            appendText("", characters, offset, length), " ");
                    article.setTitle(text);
                } else if (currentElement == sessionNumberElement) {
                    final String text = appendText("", characters, offset, length);
                    try {
                        article.setPmid(Long.parseLong(text));
                    } catch (NumberFormatException e) {
                        System.out.println("Problem converting to long " + text);
                    }
                } else if (currentElement == presentationNumberElement) {
                    final String text = appendText(
                            article.getPresentationNumber(),
                            characters, offset, length);
                    article.setPresentationNumber(text);
                    try {
                        article.setPmid(presentationNumberToPmid(text));
                    } catch (NumberFormatException e) {
                        // Couldn't do it, don't over-ride the pmid
                    }
                } else if (currentElement == sessionDescriptionElement) {
                    final String text = appendText(
                            article.getSessionDescription(),
                            characters, offset, length);
                    article.setSessionDescription(text);
                } else if (currentElement == abstractBodyElement) {
                    trimstr.length(0);
                    trimstr.append(characters, offset, length);
                    trimstr.trim();
                    abstractText.append(trimstr);
                } else if (currentElement == authorBlockElement) {
                    // TODO - extract the author and institution list
                } else if (currentElement == sessionStartElement) {
                    trimstr.length(0);
                    trimstr.append(characters, offset, length);
                    trimstr.trim();
                    long publicationDate;
                    try {
                        final Date date =
                                isoDateFormat.parse(trimstr.toString());
                        publicationDate =
                                Long.parseLong(longDateFormat.format(date));
                    } catch (ParseException e) {
                        LOG.warn("Couldn't parse date: " + trimstr.toString());
                        publicationDate = TextractorConstants.EMPTY_PUBLICATION_DATE;
                    }

                    article.setAdditionalField("pubdate", publicationDate);
                }
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
            parser.parseAttributes(false);

            articleElement = parser.factory.getElement(
                    new MutableString("advancedsearch"));
            abstractBodyElement = parser.factory.getElement(
                    new MutableString("abstractbody"));
            controlNumberElement = parser.factory.getElement(
                    new MutableString("controlnumber"));
            authorBlockElement = parser.factory.getElement(
                    new MutableString("authorblock"));
            sessionTitleElement = parser.factory.getElement(
                    new MutableString("sessiontitle"));
            publishingTitleElement = parser.factory.getElement(
                    new MutableString("publishingtitle"));
            sessionNumberElement = parser.factory.getElement(
                    new MutableString("sessionnumber"));
            presentationNumberElement = parser.factory.getElement(
                    new MutableString("presentationnumber"));
            sessionDescriptionElement = parser.factory.getElement(
                    new MutableString("sessiondescription"));
            sessionStartElement = parser.factory.getElement(
                    new MutableString("sessionstart"));
        }

        private void reset() {
            article = new SfnArticle();
            abstractText.setLength(0);
        }

        @Override
        public void startDocument() {
            assert elementStack.isEmpty() : "Stack should be empty";
            reset();
        }

        @Override
        public void endDocument() {
            assert elementStack.isEmpty() : "Stack should be empty";
        }

        @Override
        public boolean startElement(final Element element, final Map attrMap) {
            elementStack.push(element);
            if (element == articleElement) {
                reset();
            }

            return true;
        }

        @Override
        public boolean endElement(final Element element) {
            elementStack.pop();

            if ((element == articleElement)) {
                article.setFilename(getFile());
                article.setArticleNumber(++numberOfArticlesProcessed);

                String keepText = "";
                if (abstractText.length() > 0) {
                    // Text from abstract
                    keepText = abstractText.toString();
                } else {
                    // No abstract, try from session description
                    // keepText = article.getSessionDescription();
                }

                if (StringUtils.isBlank(keepText)) {
                    // Nothing to do for this document.
                    return true;
                }

                final Iterator<MutableString> splitText =
                        splitter.split(keepText);
                final Collection<Sentence> sentences;

                if (appendSentencesInOneDocument) {
                    sentences = createSentencesInOneDocument(article,
                            splitText, article.getTitle());
                } else {
                    sentences = createSentences(article,
                            splitText, article.getTitle());
                }
                produce(article, sentences);
            }
            return true;
        }
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
                final Article article, final Iterator<MutableString> splitText,
                final String title) {
        final MutableString fullDocumentText = new MutableString();

        if (loadTitles) {
            if (StringUtils.isNotEmpty(title)) {
                // add the boundary tag and positons
                fullDocumentText.append(sentenceBoundary);
                fullDocumentText.append(title);
            } else {
                LOG.warn("Skipping empty title in article " + article.getPmid());
            }
        }

        if (loadAbstracts) {
            // iterate over the text and append them to the document
            while (splitText.hasNext()) {
                // add the boundary tag and positons
                fullDocumentText.append(sentenceBoundary);

                // and add the text
                fullDocumentText.append(splitText.next());
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
            final Iterator<MutableString> splitText,
            final String title) {
        int sentenceCount = 0;
        final List<Sentence> sentences = new ArrayList<Sentence>();
        if (StringUtils.isNotEmpty(title)) {
            final Sentence sentence = produce(article, title);
            sentences.add(sentence);
            sentenceCount++;
        } else {
            LOG.warn("Skipping sentence due to empty title in article "
                    + article.getPmid());
        }

        while (splitText.hasNext()) {
            final MutableString text = splitText.next();
            final Sentence sentence = produce(article, text);
            sentences.add(sentence);
            sentenceCount++;
        }

        if (sentenceCount == 0) {
            LOG.warn("Article " + article.getPmid() + " has no sentences");
        }

        return sentences;
    }

    public static long presentationNumberToPmid(final String pn) throws NumberFormatException {
        assert StringUtils.isNotBlank(pn) : "Presentation number must have a value";
	 	final String[] parts = StringUtils.split(pn, '.');
	 	if (parts.length == 0 || parts.length > 2) {
	 	   throw new NumberFormatException(
                    "Invalid presentation number " + pn +
                            ", split length = " + parts.length);
	 	}
        final int[] iparts = new int[2];
        int pos = 0;
        for (final String part : parts) {
            iparts[pos++] = Integer.parseInt(part);
        }
        return Long.valueOf(String.format("%04d%04d", iparts[0], iparts[1]));
    }

    public static String appendWithoutDuplication(final String prevStr,
            final String newStr, final String separator) {
        if (StringUtils.isBlank(prevStr)) {
            // Previous is blank, just return the new one
            return newStr;
        }
        if (StringUtils.isBlank(newStr)) {
            // New one is blank but previous wasn't
            // just return the old one
            return prevStr;
        }
        if (prevStr.contains(newStr)) {
            return prevStr;
        }
        if (StringUtils.isEmpty(separator)) {
            return prevStr + newStr;
        } else {
            return prevStr + separator + newStr;
        }
    }
}
