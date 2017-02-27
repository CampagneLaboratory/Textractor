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

import org.apache.commons.lang.WordUtils;
import textractor.chain.AbstractSentenceTransformer;
import textractor.chain.ArticleSentencesPair;
import textractor.datamodel.Sentence;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple {@link textractor.sentence.SentenceTransformer} that capitalizes
 * all the words in a sentence.
 */
public class SentenceCapitalizer extends AbstractSentenceTransformer {
    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /** Number of sentences processed so far. */
    private final AtomicInteger numberOfSentencesProcessed =
        new AtomicInteger();

    /**
     * Create a new {@link textractor.sentence.SentenceTransformer} that
     * that capitalizes all the words in a sentence.
     */
    public SentenceCapitalizer() {
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
     * Capitalize each word in the sentences.
     *
     * @param pair The ArticleSentencesPair to be transformed.
     * @return A (possibly null) ArticleSentencesPair based on the original
     */
    public ArticleSentencesPair transform(final ArticleSentencesPair pair) {
        assert pair != null;
        for (final Sentence sentence : pair.sentences) {
            sentence.setText(WordUtils.capitalizeFully(sentence.getText()));
            numberOfSentencesProcessed.getAndIncrement();
        }
        numberOfArticlesProcessed.getAndIncrement();
        return pair;
    }
}
