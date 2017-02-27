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
 * Solves the longest commmon substring problem. Adapted from Wikipedia on May 2nd 2006:
 * http://en.wikipedia.org/wiki/Longest-common_subsequence_problem
 */
public final class LongestCommonSubstring {
    private int[] last;
    private int[] next;
    private int maxLength;

    /**
     * Construct the LCS calculator. The calculator will adapt to longer input strings if they are longer than maxLength,
     * but this will require reallocating storage each time a longer string is encountered. Therefore, it is recommended to
     * construct the LCS calculator with maxLength as long as the expected longest input string.
     * @param maxLength  Maximum length of the input strings that can be handled by this calculator.
     */
    public LongestCommonSubstring(final int maxLength) {
        reset(maxLength);
    }

    private void reset(final int maxLength) {
        this.maxLength = maxLength;
        this.last = new int[maxLength];
        this.next = new int[maxLength];
    }

    MutableString result = new MutableString();

    /**
     * Returns the longest shortest substring (LCS) of two strings. For example, if aString="abon" and bString="bonjour",
     * the LCS is "bon".
     *
     * @param aString First string.
     * @param bString Second string.
     * @return The longest common substring of aString and bString.
     */
    public MutableString longestSubstring(final String aString, final String bString) {
        final int m = aString.length();
        final int n = bString.length();
        if (m>maxLength) {
	    reset(m);
	}
        if (n>maxLength) {
	    reset(n);
	}

        clearArrays();
        int len = 0;
        final char []a = aString.toCharArray();
        final char []b = bString.toCharArray();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (a[i] == b[j]) {
                    next[j] = 1 + (j - 1 >= 0 ? last[j - 1] : 0);
                    if (next[j] > len) {
                        len = next[j];
                        result.setLength(0);
                        final int beginIndex = i - len + 1;
                        final int endIndex = len + beginIndex;
                        result.append(aString.subSequence(beginIndex, endIndex));
                    }
                }
            }
            System.arraycopy(next, 0, last, 0, maxLength);
            clearNext();
        }
        return result;
    }

    private void clearArrays() {
        for (int i = 0; i < maxLength; i++) {
            last[i] = 0;
            next[i] = 0;
        }
    }

    private void clearNext() {
        for (int i = 0; i < maxLength; i++) {

            next[i] = 0;
        }
    }

    public static void main(final String[] args) {
        final LongestCommonSubstring lcs = new LongestCommonSubstring(100);
        System.out.println("bon bonjour : " + lcs.longestSubstring("bon", "bonjour"));
        System.out.println("aasas11222233323sdsdsd -11222233323kdkd : " + lcs.longestSubstring("aasas11222233323sdsdsd", "11222233323kdkd"));
    }

}
