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
import textractor.util.SentenceFilter;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link textractor.sentence.SentenceTransformer} that transforms
 * sentences using a specific {@link textractor.util.SentenceFilter}
 */
public final class Filter extends AbstractSentenceTransformer {
    /**
     * Number of articles processed so far.
     */
    private final AtomicInteger numberOfArticlesProcessed =
            new AtomicInteger();

    /**
     * Number of sentences processed so far.
     */
    private final AtomicInteger numberOfSentencesProcessed =
            new AtomicInteger();

    /**
     * The class name of the {@link textractor.util.SentenceFilter} to use.
     */
    private String sentenceFilterClass;

    /**
     * The {@link textractor.util.SentenceFilter} to use.
     */
    private SentenceFilter sentenceFilter;

    /**
     * Create a new {@link textractor.sentence.SentenceTransformer}.
     */
    public Filter() {
        super();
    }

    /**
     * Get the number of articles processed so far.
     *
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed.get();
    }

    /**
     * Get the number of sentences processed so far.
     *
     * @return The number of sentences processed so far
     */
    public int getNumberOfSentencesProcessed() {
        return numberOfSentencesProcessed.get();
    }

    public String getSentenceFilterClass() {
        return sentenceFilterClass;
    }

    public void setSentenceFilterClass(final String filter)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        assert filter != null : "Can't set a null filter";
        this.sentenceFilterClass = filter;

        // load and create the class now
        final Class clazz = Class.forName(filter);
        this.sentenceFilter = (SentenceFilter) clazz.newInstance();

    }

    public SentenceFilter getSentenceFilter() {
        return sentenceFilter;
    }

    public void setSentenceFilter(final SentenceFilter filter) {
        assert filter != null : "Can't set a null filter";
        this.sentenceFilter = filter;
        this.sentenceFilterClass = filter.getClass().getName();
    }

    /**
     * Remove sentences using the given filter.
     *
     * @param pair The ArticleSentencesPair to be transformed.
     * @return A (possibly null) ArticleSentencesPair based on the original
     */
    public ArticleSentencesPair transform(final ArticleSentencesPair pair) {
        assert pair != null : "The article/sentences pair cannot be null";
        assert sentenceFilter != null : "The sentence filter cannot be null";
        int countDocumentFiltered = 0;
        // Note: we must use an iterator here since it's the only safe
        // way to remove items from the collection
        for (Iterator<Sentence> it = pair.sentences.iterator(); it.hasNext();) {
            final Sentence sentence = it.next();
            if (sentenceFilter.filterSentence(sentence)) {
                it.remove();
                ++countDocumentFiltered;
            }
            numberOfSentencesProcessed.getAndIncrement();
        }
        if (countDocumentFiltered < pair.sentences.size()) {
            numberOfArticlesProcessed.getAndIncrement();
        } else {
            // remove the article entirely if all the sentences were removed.
            return null;
        }
        return pair;
    }
}
