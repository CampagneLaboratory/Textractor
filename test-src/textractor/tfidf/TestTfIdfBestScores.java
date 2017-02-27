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

package textractor.tfidf;

import junit.framework.TestCase;

import java.io.IOException;


/**
 * Test executing a query programatically with
 * adding in DYM terms.
 * @author Kevin Dorff
 */
public class TestTfIdfBestScores extends TestCase {

    /**
     * Test that the vigna scorer is returning scored items.
     * @throws TweaseException twease exception running query
     * @throws IOException ioexception exception running query
     */
    public final void testFavorLarge() {
        float[] scores = {3.0f, 7.0f, 4.0f, 1.0f, 5.0f, 2.0f, 6.0f};
        // We want this to return 1, 6, 4 (such as, 7.0f, 6.0f, 5.0f)
        int[] best = TfIdfCalculator.bestScoresFavorLarge(scores, 3);
        displayList("Favor large: ", scores, best);
        assertEquals("best has wrong size", 3, best.length);
        assertEquals("best[0] wrong", 1, best[0]);
        assertEquals("best[1] wrong", 6, best[1]);
        assertEquals("best[2] wrong", 4, best[2]);
    }

    /**
     * Test that the no scorer is returning non-scored items
     * (ie score==0.0).
     * @throws TweaseException twease exception running query
     * @throws IOException ioexception exception running query
     */
    public final void testFavorSmall() {
        float[] scores = {3.0f, 7.0f, 4.0f, 1.0f, 5.0f, 2.0f, 6.0f};
        // We want this to return 3, 5, 0  (such as, 1.0f, 2.0f, 3.0f)
        int[] best = TfIdfCalculator.bestScoresFavorSmall(scores, 3);
        displayList("Favor small: ", scores, best);
        assertEquals("best has wrong size", 3, best.length);
        assertEquals("best[0] wrong", 3, best[0]);
        assertEquals("best[1] wrong", 5, best[1]);
        assertEquals("best[2] wrong", 0, best[2]);
    }

    public final void displayList(String tag, float[] scores,int[] best) {
        int pos = 0;
        System.out.print(tag);
        for (int eachbest : best) {
            if (pos++ > 0) {
                System.out.print(", ");
            }
            System.out.print(scores[eachbest]);
        }
        System.out.println();
    }

}
