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

package textractor.html;

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlparser.util.DefaultParserFeedback;
import org.htmlparser.util.ParserException;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.datamodel.TextractorDocument;
import textractor.sentence.SentenceProcessingException;
import textractor.tools.DefaultSentenceSplitter;
import textractor.tools.SentenceSplitter;
import textractor.tools.lookup.ProteinMutation;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Converts HTML to Text. This translator uses the alt text of images to replace
 * images. This is useful since many journals use images for greek symbol, and
 * use an alt attribute to render this for text-only browsers.
 */
public abstract class AbstractHtml2Text {
    private static final Log LOG = LogFactory.getLog(AbstractHtml2Text.class);
    private static boolean identifyMutations;
    private static boolean noRef;
    private final boolean doNLProt;
    private boolean sentenceExport;
    private final String sentenceOutputFile;
    private MutableString afterRefText = new MutableString();
    private final MutableString fullDocumentText = new MutableString();
    protected boolean appendSentencesInOneDocument;
    protected boolean noSentenceBoundaryTag;
    private final SentenceSplitter splitter = new DefaultSentenceSplitter();

    /**
     * One NLProt input per directory processed.
     */
    private FileWriter nlprotWriter;

    protected final boolean verbose;
    protected final int articleChunkSize;

    public AbstractHtml2Text(final String[] args) {
        verbose = CLI.isKeywordGiven(args, "-v", false);
        // generate input for nlprot as well.
        doNLProt = CLI.isKeywordGiven(args, "-nlprot", false);
        articleChunkSize = CLI.getIntOption(args, "-chunk-size", 200);
        sentenceOutputFile = CLI.getOption(args, "-s", null); // sentence output filename
        appendSentencesInOneDocument = CLI.isKeywordGiven(args, "-sentence-markup");
        noSentenceBoundaryTag = CLI.isKeywordGiven(args, "-no-sentenceboundary-tag");

        if (sentenceOutputFile != null) {
            sentenceExport = true;
            if (!CLI.isKeywordGiven(args, "-d")) {
                System.out.println("Option -s is only allowed when processing directories (-d).");
                System.exit(1);
            }
            if (appendSentencesInOneDocument) {
                System.out.println("Option -s cannot be used with option -sentence-markup.");
                System.exit(1);
            }
            System.out.println("Exporting sentence list to " + sentenceOutputFile);
        }
    }

    public void process(final String[] args) throws IOException, ParserException, SentenceProcessingException {
        // the HTML to convert
        final String input_fn = CLI.getOption(args, "-i", null); // input file name
        final String url = CLI.getOption(args, "-url", null);
        final String directory = CLI.getOption(args, "-d", null); // directory with html files. Files must have extension .htm or .html to be processed. Extesion .txt is assumed for output.
        final boolean verbose = CLI.isKeywordGiven(args, "-v", false);
        noRef = CLI.isKeywordGiven(args, "-noref", false);
        identifyMutations = CLI.isKeywordGiven(args, "-m", false);

        final URL source;

        InputStreamReader url_reader = null;
        if (url != null) {
            source = new URL(url);
            final DataInputStream dis =
                    new DataInputStream(source.openStream());
            url_reader = new InputStreamReader(dis);
        }

        Reader in = null;
        final TextConsumer consumer = getConsumer();
        consumer.begin();
        if (directory != null) {
            final File dir = new File(directory);
            if (!dir.isDirectory()) {
                System.err.println("-d must be followed with the name of a directory.");
                System.exit(3);
            } else {
                convertDirectory(dir, verbose);
            }
        } else {
            if (input_fn != null) {
                in = new FileReader(input_fn);
            } else if (url_reader != null) {
                in = url_reader;
            }

            if (in == null) {
                System.err.println("Cannot determine source from arguments. Must specify either -i file or -url url. Files have priority.");
                System.exit(0);
            }

            convertFile(in, input_fn);
        }
        consumer.end();
    }

    private void convertDirectory(final File directory, final boolean verbose) throws IOException, ParserException, SentenceProcessingException {
        if (doNLProt && directory.isDirectory()) {
            final String path = directory.getAbsolutePath() + System.getProperty("file.separator") + directory.getName() + ".nlprot";
            System.err.println("Producing NLProt input: " + path);
            nlprotWriter = new FileWriter(new File(path));
        }
        if (verbose) {
            System.out.println("Converting directory " + directory.getAbsolutePath());
        }

        Collection<Sentence> sentences;
        Writer sentenceWriter = null;
        try {
            if (sentenceExport) {
                System.out.println("Saving sentences to: " + sentenceOutputFile);
                sentenceWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sentenceOutputFile), "UTF8"));
            }

            final FilenameFilter filter = new FilenameFilter() {
                public boolean accept(final File file, final String filename) {
                    if (filename.lastIndexOf('.') == -1) {
                        return false; // no . in filename
                    }

                    final String extension = filename.substring(filename.lastIndexOf('.'), filename.length());
                    return ".html".equals(extension) || ".htm".equals(extension);
                }
            };

            final File[] files = directory.listFiles(filter);
            if (files != null) {
                for (final File file : files) {
                    if (verbose) {
                        System.out.println("Converting " + file.getAbsolutePath());
                    }
                    sentences = convertFile(new FileReader(file), file.getAbsolutePath());

                    // Export sentences
                    if (sentenceExport && sentenceWriter != null) {
                        for (final Sentence sentence : sentences) {
                            String documentSection = "OTHER";
                            if (sentence.getDocumentSection() == TextractorDocument.REFERENCE_SECTION) {
                                documentSection = "REF";
                            }
                            if (!sentence.getText().equalsIgnoreCase("")) {
                                sentenceWriter.write(sentence.getArticle().getPmid() + "\t" + sentence.getDocumentNumber() + "\t" + documentSection + "\t" + sentence.getText() + "\n");
                            }
                        }
                        sentenceWriter.flush();
                    }

                }
            }
        } finally {
            IOUtils.closeQuietly(sentenceWriter);
        }

        final FilenameFilter dirFilter = new FilenameFilter() {
            public boolean accept(final File file, final String filename) {
                return false;
            }
        };

        final File[] dirs = directory.listFiles(dirFilter);
        if (dirs != null) {
            for (final File dir : dirs) {
                if (verbose) {
                    System.out.println("Converting " + dir.getAbsolutePath());
                }
                convertDirectory(dir, verbose);
            }
        }
        if (doNLProt && nlprotWriter != null) {
            nlprotWriter.flush();
            nlprotWriter.close();
            nlprotWriter = null;
        }
    }

    private Collection<Sentence> convertFile(final Reader in,
                                             final String input)
            throws IOException, ParserException, SentenceProcessingException {
        final Html2Text parser;
        if (sentenceExport) {
            parser = new Html2TextTagRef();
        } else {
            if (noRef) {
                parser = new Html2TextNoref();
            } else {
                parser = new Html2Text();
            }
        }

        parser.parse(in, DefaultParserFeedback.QUIET);
        in.close();

        final TextConsumer consumer = getConsumer();
        final Article article = consumer.createArticle();

        // split the directory/filename into directory file
        final String[] PMID_source = input.split("/|\\\\");

        long PMID = 0;
        try {
            PMID = Long.parseLong(PMID_source[PMID_source.length - 1].replaceAll("\\..+", "")); //remove .html or .htm from the filename
        } catch (final NumberFormatException e) {
            // ignore.
        }

        article.setFilename(input);
        article.setPmid(PMID);
        LOG.info(Long.toString(PMID));

        final String text = parser.getText();

        if (nlprotWriter != null && doNLProt) {
            String id = Long.toString(article.getPmid());
            if (id == null) {
                id = Long.toString(article.getArticleNumber(), 10);
            }
            nlprotWriter.write(id);
            nlprotWriter.write(">");
        }

        final Collection<Sentence> sentences =
                loadArticleSentences(article, "", text, null);
        if (nlprotWriter != null && doNLProt) {
            nlprotWriter.write(System.getProperty("line.separator"));
        }

        consumer.consume(article, sentences);
        return sentences;

    }

    protected final Collection<Sentence> loadArticleSentences(
            final Article article, final String title, final String text,
            final Map<String, Object> additionalFieldsMap)
            throws SentenceProcessingException {

        final Collection<Sentence> sentences;
        final Iterator<MutableString> splittedText =
                splitter.split(text, !sentenceExport);
        if (this.appendSentencesInOneDocument) {
            sentences = createSentenceOneDocument(article, splittedText, title);
        } else {
            sentences = createSentences(article, splittedText, title);
        }

        if (identifyMutations) {
            final ProteinMutation proteinMutation = new ProteinMutation();
            proteinMutation.identifyMutation(sentences);
        }
        return sentences;

    }

    protected final Collection<Sentence> createSentenceOneDocument(
            final Article article,
            final Iterator<MutableString> sentencesAsTextIterator,
            final String title) throws SentenceProcessingException {
        afterRefText.setLength(0);

        final String sentenceTag =
                noSentenceBoundaryTag ? " " : " sentenceboundary ";
        fullDocumentText.setLength(0);
        fullDocumentText.append(sentenceTag);
        fullDocumentText.append(title);
        fullDocumentText.append(sentenceTag);

        while (sentencesAsTextIterator.hasNext()) {
            fullDocumentText.append(sentencesAsTextIterator.next());
            fullDocumentText.append(sentenceTag);
        }

        final TextConsumer consumer = getConsumer();
        final List<Sentence> sentences = new ArrayList<Sentence>();
        final Sentence uniqueSentence =
                consumer.produce(article, fullDocumentText.toString());
        // The HTML import doesn't have a date, just use the default
        sentences.add(uniqueSentence);
        return sentences;
    }

    protected final Collection<Sentence> createSentences(final Article article,
                                                         final Iterator<MutableString> sentencesAsTextIterator,
                                                         final String title) throws SentenceProcessingException {
        final TextConsumer consumer = getConsumer();
        int sentenceCount = 0;
        final List<Sentence> sentences = new ArrayList<Sentence>();
        if (title.length() > 0) {
            final Sentence sentence = consumer.produce(article, title);
            // The HTML import doesn't have a date, just use the default
            sentences.add(sentence);
            sentenceCount++;
        } else {
            LOG.warn("Skipping sentence due to empty title in article "
                    + article.getPmid());
        }
        afterRefText.setLength(0);

        boolean isReference = false;
        while (sentencesAsTextIterator.hasNext()) {
            sentenceCount++;
            MutableString text = sentencesAsTextIterator.next();

            final int referenceTagStartIndex;
            text.insert(0, afterRefText); // insert text from previous partial ref if any.

            if (sentenceExport) {
                if ((referenceTagStartIndex = text.indexOf(Sentence.REFERENCE_TAG)) != -1) {
                    afterRefText = text.substring(referenceTagStartIndex + Sentence.REFERENCE_TAG.length());
                    text = text.substring(0, referenceTagStartIndex);
                } else {
                    afterRefText.setLength(0);
                }
            }

            final Sentence sentence = consumer.produce(article, text.toString());
            // The HTML import doesn't have a date, just use the default
            sentences.add(sentence);

            if (sentenceExport) {
                if (isReference) {
                    sentence.setDocumentSection(TextractorDocument.REFERENCE_SECTION);
                }

                if (afterRefText.length() > 0) {   // there is text left after the reference tag, this means the rest
                    // of the text is part of the reference section.
                    isReference = true;
                }
            }
        }

        if (sentenceCount == 0) {
            LOG.warn("Article " + article.getPmid() + " has no sentences");
        }

        return sentences;
    }

    public abstract void setConsumer(final TextConsumer consumer);
    public abstract TextConsumer getConsumer();
}
