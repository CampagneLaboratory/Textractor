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
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlparser.util.DefaultParserFeedback;
import org.htmlparser.util.ParserException;
import static textractor.TextractorConstants.TIMER_LOG;
import textractor.article.ArticleProducer;
import textractor.article.DefaultArticleProducer;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.html.Html2Text;
import textractor.tools.DefaultSentenceSplitter;
import textractor.tools.PositionedText;
import textractor.tools.SentenceSplitter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Loader that can process generic html files into
 * {@link textractor.datamodel.Article}s and
 * {@link textractor.datamodel.Sentence}s.
 */
public class Html2TextArticleLoader extends AbstractFileLoader {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(Html2TextArticleLoader.class);

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
     * The {@link textractor.html.Html2Text} parser to use to process the html.
     */
    private Html2Text parser = new Html2Text();

    /**
     * The name of the {@link textractor.html.Html2Text} class to use.
     */
    private String parserClass = Html2Text.class.getName();

    // TODO - consider moving splitter to a common superclass for other loaders
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
    public Html2TextArticleLoader() {
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
        LOG.info("Scanning " + filename + " with parser: "
                + parser.getClass().getCanonicalName());
        final StopWatch timer = new StopWatch();
        timer.start();

        // configure the parser before loading
        configureParser(parser);

        final InputStream stream;
        if (filename.endsWith(".gz") || filename.endsWith(".zip")) {
            stream = new GZIPInputStream(new FileInputStream(filename));
        } else {
            stream = new FileInputStream(filename);
        }

        final Reader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        try {
            parser.parse(in, DefaultParserFeedback.QUIET);
        } catch (ParserException e) {
            LOG.error("Error parsing " + filename, e);
            throw new IOException(e.getMessage());
        } finally {
            in.close();
        }

        final Article article = articleProducer.createArticle();

        // split the directory/filename into directory file
        final String[] pmidSource = filename.split("/|\\\\");

        long pmid;
        try {
            // remove .html or .htm from the filename
            pmid = Long.parseLong(pmidSource[pmidSource.length - 1].replaceAll("\\..+", ""));
        } catch (final NumberFormatException e) {
            // couldn't get the pmid, ignore
            pmid = 0;
        }

        article.setFilename(filename);
        article.setPmid(pmid);
        final String link = PubMedInfoTool.pubmedUriFromPmid((int) pmid);
        article.setLink(link);

        final Collection<Sentence> sentences = loadSentences(article,
                parser.getText(), parser.getPositions());
        produce(article, sentences);

        timer.stop();
        if (TIMER_LOG.isInfoEnabled()) {
            TIMER_LOG.info(timer + " : " + filename);
        }

        // adjust count from consumer since it is from the start of the run
        final int counter = articleProducer.getNumberOfArticlesProcessed()
                - numberOfArticlesProcessed;
        numberOfArticlesProcessed += counter;
        if (LOG.isInfoEnabled()) {
            LOG.info("Loaded " + counter + " abstracts (cumulative: "
                    + numberOfArticlesProcessed + ") ");
        }
    }

    /**
     * Creates a collection of sentences from the original text of the html
     * article.
     *
     * @param article The article that the new sentences will be associated with
     * @param text The text that will be used to create the sentences
     * @param positions A list of positions for the text in the html source
     * @return A collection of senteneces that reflect the original html text
     */
    protected Collection<Sentence> loadSentences(final Article article,
            final String text, final List<Integer> positions) {
        final Iterator<PositionedText> splitText =
                splitter.split(text, positions);
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
            final Article article, final Iterator<PositionedText> splitText) {

        int previousPosition = 0;    // use previous position for boundary text
        final MutableString fullDocumentText = new MutableString();
        final List<Integer> fullDocumentPositions = new ArrayList<Integer>();

        // iterate over the text and append them to the document
        while (splitText.hasNext()) {
            // add the boundary tag and positions
            fullDocumentText.append(sentenceBoundary);
            for (int i = 0; i < sentenceBoundary.length(); i++) {
                fullDocumentPositions.add(previousPosition);
            }

            // and add the text with associated positions
            final PositionedText positionedText = splitText.next();
            fullDocumentText.append(positionedText.getText());
            final List<Integer> positions = positionedText.getPositions();
            fullDocumentPositions.addAll(positions);
            if (CollectionUtils.isNotEmpty(positions)) {
                previousPosition = positions.get(positions.size() - 1);
            }
        }

        // add a closing tag if there was anything added to the document
        if (fullDocumentText.length() > 0) {
            fullDocumentText.append(sentenceBoundary);
            for (int i = 0; i < sentenceBoundary.length(); i++) {
                fullDocumentPositions.add(previousPosition);
            }
        }

        final List<Sentence> sentences = new ArrayList<Sentence>();
        final Sentence sentence = produce(article, fullDocumentText);
        sentence.setPositions(fullDocumentPositions);
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
    protected Collection<Sentence> createSentences(final Article article,
            final Iterator<PositionedText> splitText) {
        int sentenceCount = 0;
        final List<Sentence> sentences = new ArrayList<Sentence>();
        while (splitText.hasNext()) {
            sentenceCount++;
            final PositionedText positionedText = splitText.next();
            final Sentence sentence =
                    produce(article, positionedText.getText());
            sentence.setPositions(positionedText.getPositions());
            sentences.add(sentence);
        }

        if (sentenceCount == 0) {
            LOG.warn("Article " + article.getPmid() + " has no sentences");
        }

        return sentences;
    }

    public String getParserClass() {
        return parserClass;
    }

    public void setParserClass(final String classname)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        assert classname != null : "Can't set a null classname";
        this.parserClass = classname;

        // load and create the class now
        final Class clazz = Class.forName(classname);
        this.parser = (Html2Text) clazz.newInstance();
    }

    private void configureParser(final Html2Text parser) {
        parser.setParagraphBoundaryTag(paragraphBoundary);
    }

    public Html2Text getParser() {
        return parser;
    }

    public void setParser(final Html2Text parser) {
        assert parser != null : "Can't set a null parser";
        this.parser = parser;
        this.parserClass = parser.getClass().getName();
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
     * Get the number of articles processed so far.
     *
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed;
    }
}
