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

package textractor.chain.producer;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.chain.AbstractSentenceProducer;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class StringArrayProducer extends AbstractSentenceProducer {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(StringArrayProducer.class);

    /**
     * Total number of articles processed by this loader.
     */
    private final AtomicInteger numberOfArticlesProcessed = new AtomicInteger();

    /**
     * Total number of sentences processed by this loader.
     */
    private final AtomicInteger numberOfSentencesProcessed = new AtomicInteger();

    private final CharSequence[] documents;

    private static final Article DUMMY_ARTICLE = new Article();

    public StringArrayProducer(final CharSequence... documents) {
        super();
        this.documents = documents;
    }

    public StringArrayProducer(final Collection<CharSequence> collection) {
        super();
        this.documents = collection.toArray(new CharSequence[collection.size()]);
    }

    /**
     * Produce a new Sentence.
     *
     * @param article The article the sentence will be associated with.
     * @param text    The text to be used for the sentence.
     * @return The sentence object based on the article and text sequence.
     */
    public final Sentence produce(final Article article,
                                  final CharSequence text) {
        final Sentence sentence = new Sentence(article, text.toString());
        final long sentenceNumber =
                numberOfSentencesProcessed.getAndIncrement();
        sentence.setDocumentNumber(sentenceNumber);
        if (LOG.isTraceEnabled()) {
            final long articleNumber = article.getArticleNumber();
            LOG.trace("[" + articleNumber + ", " + sentenceNumber + "] "
                    + sentence.getText().substring(0,
                    Math.min(50, sentence.getText().length())));
        }
        return sentence;
    }


    /**
     * Thread that will process a array of strings.
     *
     * @return true if processing completed with no problems
     * @throws Exception if there is a problem processing the strings.
     */
    public final Boolean call() throws Exception {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug(BeanUtils.describe(this));
            }

            final List<Sentence> sentences =
                    new ArrayList<Sentence>(documents.length);
            for (final CharSequence document : documents) {
                sentences.add(produce(DUMMY_ARTICLE, document));
            }
            produce(DUMMY_ARTICLE, sentences);

            LOG.debug("Processing complete");
            fireSentenceProcessingCompleteEvent();
        } catch (Throwable t) {
            LOG.fatal("Got an exception in thread " + this.getClass().getName(), t);
            Thread.currentThread().getThreadGroup().interrupt();
            if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw new Exception(t);
            }
        }
        return true;
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
     * Get the number of articles processed so far.
     *
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed.get();
    }

}
