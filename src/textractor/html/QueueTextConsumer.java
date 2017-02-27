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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A consumer of text that stuffs articles and their sentences into a queue
 * that can be read by an indexing task.
 */
public final class QueueTextConsumer extends TextConsumer {
    /** Used to log debug and informational messages. */
    private static final Log LOG = LogFactory.getLog(QueueTextConsumer.class);

    /** Queue to store articles for processing by an indexer. */
    private final BlockingQueue<Sentence> workQueue;

    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /** Number of sentences processed so far. */
    private final AtomicInteger numberOfSentencesProcessed =
        new AtomicInteger();

    /**
     * Creates a new Queue based text cosumer.
     * @param queue Where consumed sentences should be stored.
     */
    public QueueTextConsumer(final BlockingQueue<Sentence> queue) {
        super();
        this.workQueue = queue;
    }

    /**
     * Begin the interaction with this consumer.
     */
    public void begin() {
        // null implementation
    }

    /**
     * End the interaction with this processor.  Note that this does not
     * necessarily indicate that the processor will not be utilized again.
     */
    public void end() {
        // null implementation
    }

    /**
     * Creates an article.
     * @return an Article.
     */
    public Article createArticle() {
        return new Article();
    }

    /**
     * Produce a new Sentence.
     * @param article The article the sentence will be associated with.
     * @param text The text to be used for the sentence.
     * @return The sentence object based on the article and text sequence.
     */
    public Sentence produce(final Article article, final CharSequence text) {
        return new Sentence(article, text.toString());
    }

    /**
     * Process sentences along with their associated article.
     * @param article The article assoicated with the sentences.
     * @param sentences A collection of Sentences to process.
     */
    public void consume(final Article article,
                        final Collection<Sentence> sentences) {
        consume(article,  sentences,  null);
    }

    /**
     * Process sentences along with their associated article.
     * @param article The article assoicated with the sentences.
     * @param sentences A collection of Sentences to process.
     * @param callback A {@link CheckpointCallback} that will should get
     * invoked when processing is complete.
     */
    @Override
    public void consume(final Article article,
                        final Collection<Sentence> sentences,
                        final CheckpointCallback callback) {
        // number each article
        article.setArticleNumber(numberOfArticlesProcessed.getAndIncrement());

        // number each sentence
        long length = 0;
        for (final Sentence sentence : sentences) {
            sentence.setDocumentNumber(numberOfSentencesProcessed.getAndIncrement());

            // add the sentence to the queue for indexing
            try {
                workQueue.put(sentence);
            } catch (final InterruptedException e) {
                LOG.error("Interrupted while adding", e);
                Thread.currentThread().interrupt();
            }

            if (++length == 1) {
                article.setDocumentNumberRangeStart(sentence.getDocumentNumber());
            }
        }

        // and set the document range
        article.setDocumentNumberRangeLength(length);

        // invoke the callback if any
        if (callback != null) {
            callback.checkpointCallback();
        }
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
}
