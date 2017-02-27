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

import it.unimi.dsi.mg4j.util.MutableString;

/**
 * Solves the longest commmon sunsequence problem. Adapted from Wikipedia on July 16th 2006:
 * http://en.wikipedia.org/wiki/Longest-common_subsequence_problem
 */
public final class LongestCommonSubsequence {
    public static final byte UP_LEFT = 1;
    public static final byte UP = 2;
    public static final byte LEFT = 3;
    private byte [][] b;
    private CharSequence aString;
    private CharSequence bString;
    private int LCS_max_i;
    private int LCS_min_i = Integer.MAX_VALUE;
    private int LCS_min_j = Integer.MAX_VALUE;
    private int LCS_max_j;
    private int gapCountFirst;
    private int gapCountSecond;
    private MutableString LCS = new MutableString();

    public int getMatchStartIndexForFirst() {
        return LCS_min_i;
    }

    public int getMatchEndIndexForFirst() {
        return LCS_max_i;
    }

    public int getMatchStartIndexForSecond() {
        return LCS_min_j;
    }

    public int getMatchEndIndexForSecond() {
        return LCS_max_j;
    }

    /**
     * calculates the longest shortest subsequence (LCS) of two strings. For example, if firstString="abon" and secondString="-b-o--n-jour",
     * the LCS is "bon".
     *
     * @param firstString  First string.
     * @param secondString Second string.
     */
    public void longestSubsequence(final CharSequence firstString, final CharSequence secondString) {
        this.aString = firstString;
        this.bString = secondString;

        final int m = firstString.length();
        final int n = secondString.length();
        final int[][] c = new int[m][n];
        b = new byte[m][n];

        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                if (firstString.charAt(i) == secondString.charAt(j)) {
                    c[i][j] = c_i_j(c, i - 1, j - 1) + 1;
                    b[i][j] = UP_LEFT;
                } else if (c_i_j(c, i - 1, j) >= c_i_j(c, i, j - 1)) {
                    c[i][j] = c_i_j(c, i - 1, j);
                    b[i][j] = UP;
                } else {
                    c[i][j] = c_i_j(c, i, j - 1);
                    b[i][j] = LEFT;
                }
            }
        }

        scanB();
    }

    /**
     * Return the number of gaps in the first string, when aligned to the second.
     *
     * @return Number of gaps in the first string.
     */
    public int getGapCountFirst() {
        return gapCountFirst;
    }

    /**
     * Return the number of gaps in the second string, when aligned to the first.
     *
     * @return Number of gaps in the second string.
     */
    public int getGapCountSecond() {
        return gapCountSecond;
    }

    private void scanB() {
        LCS_max_i = 0;
        LCS_min_i = Integer.MAX_VALUE;
        LCS_min_j = Integer.MAX_VALUE;
        LCS_max_j = 0;

        LCS.setLength(0);
        gapCountFirst = 0;
        gapCountSecond = 0;
        int gapCountSecondDelta = 0;
        int gapCountFirstDelta = 0;
        boolean matching = false;
        int i = aString.length() - 1;
        int j = bString.length() - 1;
        for (; i >= 0 && j >= 0;) {

            switch (b[i][j]) {
                case 0:
                    return;
                case LEFT:
                    --j;
                    if (matching) {
			++gapCountSecondDelta;
		    }
                    break;
                case UP:
                    --i;
                    if (matching) {
			++gapCountFirstDelta;
		    }
                    break;
                case UP_LEFT:
                    matching = true;
                    LCS.insert(0, aString.charAt(i));
                    LCS_max_i = Math.max(i, LCS_max_i);
                    LCS_min_i = Math.min(i, LCS_min_i);
                    LCS_max_j = Math.max(j, LCS_max_j);
                    LCS_min_j = Math.min(j, LCS_min_j);
                    gapCountFirst += gapCountFirstDelta;
                    gapCountSecond += gapCountSecondDelta;
                    gapCountFirstDelta = 0;
                    gapCountSecondDelta = 0;
                    --i;
                    --j;
                    break;
            }
        }

    }

    private int c_i_j(final int[][] c, final int i, final int j) {
        if (i < 0 || j < 0) {
            return 0;
        } else {
            return c[i][j];
        }
    }


    public static void main(final String[] args) {
        final LongestCommonSubsequence lcs = new LongestCommonSubsequence();
        lcs.longestSubsequence("bon", "-b-o-n-jour");
        lcs.longestSubsequence("/bon", ".b-o-n-jour");
    }

    public MutableString getLCS() {
        return LCS;
    }
}
