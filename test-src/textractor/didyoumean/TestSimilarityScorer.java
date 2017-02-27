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

package textractor.didyoumean;

import junit.framework.TestCase;

/**
 * Test case for SimilarityScorer.
 */
public class TestSimilarityScorer extends TestCase {
    /**
     * Calculates edit distance and similarity score between 'hedgehog' and
     * 'edgesmog'.
     * Edit 1: hedgesog to hedgeog (substitution)
     * Edit 2: hedgesog to hedgesmog (insertion)
     * Edit 2: hedgesmog to edgesmog (deletion)
     */
    public void testSimilarityScoreWithDifferentStrings() {
        final String firstString = "hedgehog";
        final String secondString = "edgesmog";
        final SimilarityScorer scorer = new SimilarityScorer();
        assertEquals(3f,
                scorer.getEditDistance(firstString, secondString));
        assertEquals(0.625f,
                scorer.getSimilarity(firstString, secondString));
    }

    /**
     * Calculates edit distance and similarity score between identical words.
     */
    public void testSimilarityScoreWithIdenticalStrings() {
        final String firstString = "hedgehog";
        final String secondString = "hedgehog";

        final SimilarityScorer scorer = new SimilarityScorer();

        assertEquals(0f,
                scorer.getEditDistance(firstString, secondString));
        assertEquals(1.0f,
                scorer.getSimilarity(firstString, secondString));
    }

    /**
     * Tests minimum number calculator.
     */
    public void testMinimum() {

        final SimilarityScorer scorer = new SimilarityScorer();

        assertEquals(4f, scorer.minimum(4, 5, 6));
        assertEquals(4f, scorer.minimum(5, 4, 6));
        assertEquals(4f, scorer.minimum(6, 5, 4));

        assertEquals(1f, scorer.minimum(1, 1, 1));

        assertEquals(1f, scorer.minimum(1, 2, 2));
        assertEquals(-1f, scorer.minimum(-1, 2, 2));
    }
}
