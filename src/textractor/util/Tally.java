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

package textractor.util;

import textractor.tools.ReaderMaker;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: campagne
 * Date: Oct 22, 2003
 * Time: 3:58:06 PM
 * To change this template use Options | File Templates.
 */
public final class Tally {
    private List<String> keywords;
    private List<Integer> counts;
    private long keywordCount;
    private final String delimiters;
    private double[] frequencies;
    private int[] tally;

    public Tally(final String delimiters) {
        reset();
        this.delimiters = delimiters;

    }

    public void reset() {
        keywords = new ArrayList<String>();
        counts = new ArrayList<Integer>();
        keywordCount = 0;

    }

    public double[] getFrequencies() {
        return frequencies;
    }

    public int[] getTally() {
        return tally;
    }

    /**
     * Returns howMany high frequency keywords.
     */
    public String[] findHighestFrequencyKeywords(final int howMany) {
        final String[] result = new String[howMany];
        frequencies = new double[howMany];
        tally = new int[howMany];

        for (int i = 0; i < howMany - 1; i++) {
            // find the most occurent keyword:
            final int maxCount = Collections.max(counts);
            final int maxIndex = counts.indexOf(maxCount);
            result[i] = keywords.get(maxIndex);
            tally[i] = maxCount;
            frequencies[i] = ((double) maxCount) / keywordCount;

            // set the count of the one found to 0 so we will not find it next time:
            counts.set(maxIndex, 0);
        }
        return result;
    }

    /** Finds returns the n highest frequency keywords in the input. */
    public void tallyKeywords(final ReaderMaker readerMaker) throws IOException {
        final BufferedReader br = new BufferedReader(readerMaker.getReader());
        String line;
        while ((line = br.readLine()) != null) {
            // for each line of input
            final StringTokenizer st = new StringTokenizer(line, delimiters);
            while (st.hasMoreTokens()) { // for each token in the input:
                processKeyword(st.nextToken());
            }
        }
    }

    public void processKeyword(final String keyword) {
        keywordCount++;
        final int index = keywords.indexOf(keyword);

        final int value;
        if (index == -1) {
            value = 1;
            keywords.add(keyword);
            counts.add(value);
        } else {
            value = counts.get(index) + 1;
            keywords.set(index, keyword);
            counts.set(index, value);
        }
    }
}
