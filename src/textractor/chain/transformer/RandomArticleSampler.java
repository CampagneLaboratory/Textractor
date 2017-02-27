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

import cern.jet.random.engine.MersenneTwister;
import textractor.chain.AbstractSentenceTransformer;
import textractor.chain.ArticleSentencesPair;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link textractor.sentence.SentenceTransformer} that randomly selects
 * articles for further processing.
 */
public final class RandomArticleSampler extends AbstractSentenceTransformer {
    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /** Number of sentences processed so far. */
    private final AtomicInteger numberOfSentencesProcessed =
        new AtomicInteger();

    /**
     * Seed to use for the random number generator.  See
     * {@link cern.jet.random.engine.MersenneTwister}
     */
    private int seed = MersenneTwister.DEFAULT_SEED;

    /**
     * Threshold used to select the subset of articles.  0.1 would indicate
     * that 10% of the articles should be processed.
     */
    private double threshold = 0.1;


    /**
     * Indicates processing has been intialized.
     */
    private final AtomicBoolean intialized = new AtomicBoolean();

    /**
     * Random number generator to use.
     */
    private MersenneTwister twister;

    /**
     * Create a new {@link textractor.sentence.SentenceTransformer} that
     * that randomly selects articles for further processing.
     */
    public RandomArticleSampler() {
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
     * Remove random articles from the chain.
     *
     * @param pair The ArticleSentencesPair to be transformed.
     * @return A (possibly null) ArticleSentencesPair based on the original
     */
    public ArticleSentencesPair transform(final ArticleSentencesPair pair) {
        assert pair != null;
        if (!intialized.getAndSet(true)) {
            twister = new MersenneTwister(seed);
        }

        numberOfArticlesProcessed.getAndIncrement();
        if (twister.nextDouble() <= threshold) {
            return pair;
        } else {
            return null;
        }
    }

    /**
     * Get the seed used to initialize the random number generator.
     * @return The seed used to initialize the random number generator
     */
    public int getSeed() {
        return seed;
    }

    /**
     * Set the seed used to initialize the random number generator.
     * @param seed The seed used to initialize the random number generator
     */
    public void setSeed(final int seed) {
        this.seed = seed;
    }

    /**
     * Get the threshold to use when determining whether or not to keep an
     * article.
     * @return The threshold to use when filtering articles.
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Set the threshold to use when determining whether or not to keep an
     * article.  Values should be bounded [0..1].  The larger the value,
     * the more articles will be kept.
     * @param threshold The threshold to use when filtering articles.
     */
    public void setThreshold(final double threshold) {
        this.threshold = threshold;
    }
}
