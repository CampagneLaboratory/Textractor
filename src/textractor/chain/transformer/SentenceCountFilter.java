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

package textractor.chain.transformer;

import textractor.chain.AbstractSentenceTransformer;
import textractor.chain.ArticleSentencesPair;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link textractor.sentence.SentenceTransformer} that removes sentence
 * an article that contain less than .
 */
public final class SentenceCountFilter extends AbstractSentenceTransformer {
    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /** Number of sentences processed so far. */
    private final AtomicInteger numberOfSentencesProcessed =
        new AtomicInteger();

    /**
     * Any artciles with less than #minimumSentenceCount
     * {@link textractor.datamodel.Sentence}s will be removed from the chain.
     */
    private int minimumSentenceCount;

    /**
     * Create a new {@link textractor.sentence.SentenceTransformer}.
     */
    public SentenceCountFilter() {
        super();
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
     * Get the minimum sentence count used to filter out articles.
     * @return The minimum sentence count used to filter out articles.
     */
    public int getMinimumSentenceCount() {
        return minimumSentenceCount;
    }

    /**
     * Get the minimum sentence count used to filter out articles.
     * @param count The minimum sentence count used to filter out articles.
     */
    public void setMinimumSentenceCount(final int count) {
        this.minimumSentenceCount = count;
    }

    /**
     * Remove any {@link textractor.chain.ArticleSentencesPair}s with less
     * than #minimumSentenceCount {@link textractor.datamodel.Sentence}s.
     *
     * @param pair The ArticleSentencesPair to be transformed.
     * @return A (possibly null) ArticleSentencesPair based on the original
     */
    public ArticleSentencesPair transform(final ArticleSentencesPair pair) {
        assert pair != null;
        numberOfArticlesProcessed.getAndIncrement();
        if (pair.sentences.size() < minimumSentenceCount) {
            return null;
        } else {
            return pair;
        }
    }
}
