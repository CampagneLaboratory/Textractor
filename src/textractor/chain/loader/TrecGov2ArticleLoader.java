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

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.mg4j.io.SegmentedInputStream;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlparser.util.DefaultParserFeedback;
import org.htmlparser.util.ParserException;
import static textractor.TextractorConstants.TIMER_LOG;
import textractor.datamodel.Article;
import textractor.datamodel.OtmiArticle;
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Loader that can process TREC GOV2 files into
 * {@link textractor.datamodel.Article}s and
 * {@link textractor.datamodel.Sentence}s.
 */
public final class TrecGov2ArticleLoader extends AbstractFileLoader {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(TrecGov2ArticleLoader.class);

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
     * Indicates if the title of the article will be processed.
     */
    private boolean loadTitles = true;

    /**
     * Indicates if the abstract of the article will be processed.
     */
    private boolean loadAbstracts = true;

    private static final byte[] DOC_OPEN;
    private static final byte[] DOC_CLOSE;
    private static final byte[] DOCNO_OPEN;
    private static final byte[] DOCNO_CLOSE;
    private static final byte[] DOCHDR_OPEN;
    private static final byte[] DOCHDR_CLOSE;

    static {
        try {
            DOC_OPEN = "<DOC>".getBytes("ASCII");
            DOC_CLOSE = "</DOC>".getBytes("ASCII");
            DOCNO_OPEN = "<DOCNO>".getBytes("ASCII");
            DOCNO_CLOSE = "</DOCNO>".getBytes("ASCII");
            DOCHDR_OPEN = "<DOCHDR>".getBytes("ASCII");
            DOCHDR_CLOSE = "</DOCHDR>".getBytes("ASCII");
        } catch (UnsupportedEncodingException cantHappen) {
            throw new RuntimeException(cantHappen);
        }
    }

    /**
     * Used to indicate what has been processed in from the document header.
     */
    private enum HeaderProcessingState {
        NOT_STARTED,
        STARTED,
        URI_COLLECTED
    }

    private final byte[] buffer = new byte[8 * 1024];

    private String documentNumber;
    private String documentUri;

    /**
     * A compact description of the location and of the internal segmentation of
     * a TREC document inside a file.  Essentially lifted from
     * {@link it.unimi.dsi.mg4j.document.TRECDocumentCollection}.
     */
    private static class TRECDocumentDescriptor {
        /** The starting position of this document in the file. */
        private final long startMarker;
        /** The starting position of the content of this document in the file. */
        private final int intermediateMarkerDiff;
        /** The ending position. */
        private final int stopMarkerDiff;

        public TRECDocumentDescriptor(final long start,
                                      final long intermediateMarker,
                                      final long stop) {
            this.startMarker = start;
            this.intermediateMarkerDiff = (int) (intermediateMarker - start);
            this.stopMarkerDiff = (int) (stop - start);
        }

        public final long[] toSegments() {
            return new long[] {
                    startMarker,
                    startMarker + intermediateMarkerDiff,
                    stopMarkerDiff + startMarker
            };
        }
    }

    /**
     * The list of document descriptors.  We assume that descriptors within
     * the same file are contiguous
     */
    private final transient ObjectArrayList<TRECDocumentDescriptor> descriptors =
            new ObjectArrayList<TRECDocumentDescriptor>();

    /**
     * Total number of articles processed by this loader.
     */
    private int numberOfArticlesProcessed;

    /**
     * Create a new loader.
     */
    public TrecGov2ArticleLoader() {
        super();
    }

    /**
     * Used to get a stream from the name of the file.
     * @param filename The name of the file to open.
     * @return The InputStream associated with the file
     * @throws IOException if there was a problem getting the stream.
     */
    private InputStream openFileStream(final String filename) throws IOException {
        final InputStream stream;
        if (filename.endsWith(".gz") || filename.endsWith(".zip")) {
            stream = new GZIPInputStream(new FileInputStream(filename));
        } else {
            stream = new FileInputStream(filename);
        }
        return stream;
    }

    /**
     * Process a single file designated by name.
     *
     * @param filename The name of the file to process.
     * @throws java.io.IOException if there is a problem reading the file.
     */
    @Override
    public void processFilename(final String filename) throws IOException {
        int numberOfDocuments  = 0;
        LOG.info("Scanning " + filename);
        final StopWatch timer = new StopWatch();
        timer.start();

        // configure the parser before loading
        configureParser(parser);

        // read the file to extract mark positions of individual documents
        // within the file
        FastBufferedInputStream fbis = null;
        try {
            fbis = new FastBufferedInputStream(openFileStream(filename));

            long currStart = 0;
            long currInter = 0;
            long oldPos = 0;
            boolean pastHeader = false;
            boolean startedBlock = false;

            descriptors.clear();

            int l;
            while ((l = fbis.readLine(buffer)) != -1)  {
                if (l == buffer.length) {
                    // We filled the buffer, which means we have a very very long line. Let's skip it.
                    while ((l = fbis.readLine(buffer)) == buffer.length) {
                        ;
                    }
                } else {
                    if (!startedBlock && equals(buffer, l, DOC_OPEN)) {
                        currStart = oldPos;
                        startedBlock = true; // Start of the current block (includes <DOC> marker)
                    } else if (startedBlock && equals(buffer, l, DOC_CLOSE)) {
                        final long currStop = oldPos;
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Setting markers <" + currStart + ", "
                                    + currInter + ", " + currStop + ">" );
                        }
                        descriptors.add(new TRECDocumentDescriptor(currStart, currInter, currStop));
                        startedBlock = pastHeader = false;
                    } else if (startedBlock && !pastHeader && equals(buffer, l, DOCHDR_CLOSE)) {
                        currInter = fbis.position();
                        pastHeader = true;
                    }
                    oldPos = fbis.position();
                }
            }

        } finally {
            IOUtils.closeQuietly(fbis);
        }

        // Take the positions stored above and "convert" them to
        // individual streams for processing.  The SegmentedInputStream
        // allows us to do this without constantly opening and closing
        // a file.
        InputStream iStream = null;
        SegmentedInputStream siStream = null;
        try {
            iStream = openFileStream(filename);
            siStream = new SegmentedInputStream(iStream);
            for (final TRECDocumentDescriptor descriptor : descriptors) {
                siStream.addBlock(descriptor.toSegments());
            }

            boolean done = false;
            while (!done) {
                parseHeader(siStream);
                siStream.reset();
                parseContent(siStream, filename);

                final Article article =
                        createArticle(documentNumber, documentUri, filename);
                final Collection<Sentence> sentences = loadSentences(article,
                        parser.getText(), parser.getPositions());
                produce(article, sentences);
                numberOfDocuments++;

                if (siStream.hasMoreBlocks()) {
                    siStream.nextBlock();
                } else {
                    done = true;
                }
            }
        } finally {
            IOUtils.closeQuietly(siStream);
            IOUtils.closeQuietly(iStream);
        }

        timer.stop();
        if (TIMER_LOG.isInfoEnabled()) {
            TIMER_LOG.info(timer +  " : " + filename);
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("Loaded " + numberOfDocuments + " documents (cumulative: "
                    + numberOfArticlesProcessed + ") ");
        }
    }

    /**
     * Parse the header section of an individual document within a file.
     * @param stream An input stream representing just the header section
     * @throws IOException If there is a problem reading from the stream.
     */
    private void parseHeader(final InputStream stream) throws IOException {
        HeaderProcessingState state = HeaderProcessingState.NOT_STARTED;
        boolean foundDocNo = false;
        final FastBufferedInputStream headerStream =
                new FastBufferedInputStream(stream);
        int l;
        while ((l = headerStream.readLine(buffer)) != -1) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(new String(buffer, 0, l));
            }
            if (!foundDocNo && startsWith(buffer, l, DOCNO_OPEN)) {
                foundDocNo = true;
                documentNumber = new String(buffer, DOCNO_OPEN.length,
                        l - (DOCNO_OPEN.length + DOCNO_CLOSE.length));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Processing document number: " + documentNumber);
                }
            }

            switch (state) {
                case NOT_STARTED:
                    if (equals(buffer, l, DOCHDR_OPEN)) {
                        state = HeaderProcessingState.STARTED;
                    }
                    break;
                case STARTED:
                    state = HeaderProcessingState.URI_COLLECTED;
                    documentUri = new String(buffer, 0, l);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Parse the content of a single document within a file.  This basically
     * hands off the stream to the html {@link #parser}.
     * @param stream An input stream representing the document to be parsed.
     * @param filename The name of the file that contains the document
     * @throws IOException If there is a problem reading from the stream.
     */
    private void parseContent(final InputStream stream, final String filename) throws IOException {
        final Reader in = new BufferedReader(new InputStreamReader(stream));
        try {
            parser.parse(in, DefaultParserFeedback.QUIET);
        } catch (ParserException e) {
            LOG.error("Error parsing " + filename, e);
            throw new IOException(e.getMessage());
        }
        // NOTE: we don't close the reader here, the stream will be closed later
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

    /**
     * Used to determine if two byte arrays are equal to one another.
     * @param a The first array to compare.
     * @param len The number of bytes to compare (must be >= a.length)
     * @param b The second array to compare
     * @return true if all the bytes are the same, false otherwise
     */
    private static boolean equals(final byte[] a, int len, final byte[] b) {
        if (len != b.length) {
            return false;
        }

        while (len-- != 0) {
            if (a[len] != b[len]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Used to determine if the beginning bytes in a byte array match the other.
     * @param a The array to check
     * @param l The number of bytes from the array that should be checked
     * @param b The byte sequence to check for
     * @return true if the bytes are the same, false otherwise
     */
    private static boolean startsWith(final byte[] a, final int l, final byte[] b) {
        int len = b.length;
        if (len > l) {
            return false;
        }

        while (len-- != 0) {
            if (a[len] != b[len]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Create an {@link textractor.datamodel.Article} using the given
     * parameters.  Note that we're "cheating" here by using an OtmiArticle.
     * In reality, with the current design we should have made a TrecGov2Article
     * but this will allow us to not have to change the twease application.
     *
     * @param identifier identifier for the article.
     * @param uri uri of the article
     * @param filename Filename that the article came from
     * @return A new article.
     */
    private OtmiArticle createArticle(final String identifier,
                                      final String uri, final String filename) {
        // TODO: Don't use an OTMIArticle, but create a new type
        final OtmiArticle article = new OtmiArticle();
        article.setPmid(-1);
        article.setDoi(identifier);
        article.setLink(uri);
        article.setArticleNumber(numberOfArticlesProcessed++);
        article.setFilename(filename);
        return article;
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
}
