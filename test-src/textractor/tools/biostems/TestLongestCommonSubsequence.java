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

package textractor.tools.biostems;

import junit.framework.TestCase;

/**
 * User: Fabien Campagne
 * Date: July 17th 2006
 * Time: 8:12:38 AM
 */
public class TestLongestCommonSubsequence extends TestCase {
    public void testLCS() {
        final LongestCommonSubsequence lcs = new LongestCommonSubsequence();
        lcs.longestSubsequence("bon", "-b-o-n-jour");

        assertEquals(0, lcs.getMatchStartIndexForFirst());
        assertEquals(2, lcs.getMatchEndIndexForFirst());
        assertEquals(1, lcs.getMatchStartIndexForSecond());
        assertEquals(5, lcs.getMatchEndIndexForSecond());
        assertEquals("bon", lcs.getLCS().toString());
        assertEquals(0, lcs.getGapCountFirst());
        assertEquals(2, lcs.getGapCountSecond());
        lcs.longestSubsequence("/,,,bo..n", ".b-o-n-jour");

        assertEquals(4, lcs.getMatchStartIndexForFirst());
        assertEquals(8, lcs.getMatchEndIndexForFirst());
        assertEquals(1, lcs.getMatchStartIndexForSecond());
        assertEquals(5, lcs.getMatchEndIndexForSecond());

        assertEquals("bon", lcs.getLCS().toString());
        assertEquals(2, lcs.getGapCountFirst());
        assertEquals(2, lcs.getGapCountSecond());
    }
}
