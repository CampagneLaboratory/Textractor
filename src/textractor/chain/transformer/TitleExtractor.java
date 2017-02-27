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
import textractor.datamodel.Sentence;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link textractor.sentence.SentenceTransformer} that removes sentence
 * an article that contain less than .
 */
public final class TitleExtractor extends AbstractSentenceTransformer {
    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /** Number of sentences processed so far. */
    private final AtomicInteger numberOfSentencesProcessed =
        new AtomicInteger();

    /**
     * Create a new {@link textractor.sentence.SentenceTransformer}.
     */
    public TitleExtractor() {
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
     * Transform the {@link textractor.chain.ArticleSentencesPair} so
     * that only the title sentence remains.
     *
     * @param pair The ArticleSentencesPair to be transformed.
     * @return A (possibly null) ArticleSentencesPair based on the original
     */
    public ArticleSentencesPair transform(final ArticleSentencesPair pair) {
        assert pair != null;
        int sentenceCount = 0;
        final Iterator<Sentence> sentences = pair.sentences.iterator();
        while (sentences.hasNext()) {
            sentences.next();
            // the big assumption here is that the title is the first sentence
            if (sentenceCount++ > 0) {
                sentences.remove();
            }
        }
        numberOfArticlesProcessed.getAndIncrement();
        return pair;
    }
}
