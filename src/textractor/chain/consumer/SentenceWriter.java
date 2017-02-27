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

package textractor.chain.consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.chain.AbstractSentenceConsumer;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.event.sentence.SentenceProcessingCompleteEvent;
import textractor.sentence.SentenceProcessingException;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple {@link textractor.sentence.SentenceConsumer} that prints each
 * sentence consumed with it's corresponding pmid.
 */
public final class SentenceWriter extends AbstractSentenceConsumer {
    /** Used to log debug and informational messages. */
    private static final Log LOG = LogFactory.getLog(SentenceWriter.class);

    /** The writer to print sentences to. */
    private PrintWriter writer;

    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /** Number of sentences processed so far. */
    private final AtomicInteger numberOfSentencesProcessed =
        new AtomicInteger();

    /**
     * Name of the output file to write the sentences to.  Output will be
     * written to System.out if no file is specified.
     */
    private String outputFile;

    private boolean shortOutput;

    /**
     * Create a new {@link textractor.sentence.SentenceConsumer} that writes
     * to @see System#out.
     */
    public SentenceWriter() {
        super();
        this.writer = new PrintWriter(System.out);
    }

    /**
     * Create a new {@link textractor.sentence.SentenceConsumer} that writes
     * to the specified writer.
     * @param writer The writer to use.
     */
    public SentenceWriter(final PrintWriter writer) {
        super();
        this.writer = writer;
    }

    /**
     * Print sentences along with their associated article.
     * @param article The article assoicated with the sentences.
     * @param sentences A collection of Sentences to process.
     * @throws SentenceProcessingException if there was a problem writing
     * the sentences.
     */
    public void consume(final Article article,
            final Collection<Sentence> sentences)
            throws SentenceProcessingException {
        assert article != null : "Cannot process a null Article";
        assert sentences != null : "Cannot process a null Sentence collection";

        // write each sentence as "document #<tab>pmid<tab>text"
        if (sentences.size() > 0) {
            final long pmid = article.getPmid();
            for (final Sentence sentence : sentences) {
                if (isShortOutput()) {
                    writer.printf("%d\t%d\t%d\n", sentence.getDocumentNumber(),
                            pmid, sentence.getText().length());
                } else {
                    writer.printf("%d\t%d\t%s\n", sentence.getDocumentNumber(),
                            pmid, sentence.getText());
                }
                numberOfSentencesProcessed.getAndIncrement();
                fireSentenceProcessedEvent(sentence);
            }
            writer.flush();
        }

        numberOfArticlesProcessed.getAndIncrement();
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
     * Get the name of the output file to write to.
     * @return The name of the output file to write to.
     */
    public String getOutputFile() {
        return outputFile;
    }

    /**
     * Set the name of the output file to write to.
     * @param filename The name of the output file to write to.
     * @throws FileNotFoundException if the file cannot be written to
     * @throws UnsupportedEncodingException if "UTF-8" is not supported
     */
    public void setOutputFile(final String filename)
            throws FileNotFoundException, UnsupportedEncodingException {
        writer = new PrintWriter(filename, "UTF-8");
        this.outputFile = filename;
    }

    public void setShortOutput(final boolean shortOutput) {
        this.shortOutput = shortOutput;
    }

    public boolean isShortOutput() {
        return this.shortOutput;
    }

    /**
     * This method gets called when a sentence processing is complete.
     * @param event A {@link textractor.event.sentence.SentenceProcessingCompleteEvent} object describing
     * the event source.
     */
    @Override
    public void processingComplete(final SentenceProcessingCompleteEvent event) {
        LOG.debug("Got completion from: " + event.getSource());
        if (event.getSource() == this) {
            LOG.debug("Closing the writer");
            writer.flush();
            writer.close();
        }
        super.processingComplete(event);
    }

}
