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

package textractor.tools.ambiguity;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Fabien Campagne
 * Date: Jul 31, 2005
 * Time: 5:51:05 PM
 */
public final class KeywordPairAnalysed {
    String firstKeyword;
    String secondKeyword;
    private int[] matchingDocuments;
    private int seedCorpusFrequency;

    public KeywordPairAnalysed(final String firstKeyword,
            final String secondKeyword) {
        this.firstKeyword = firstKeyword;
        this.secondKeyword = secondKeyword;
    }

    public int getSeedCorpusFrequency() {
        return seedCorpusFrequency;
    }

    public Collection<String> getKeywordCollection() {
        final Collection<String> queryPair = new ArrayList<String>();
        queryPair.add(firstKeyword);
        queryPair.add(secondKeyword);
        return queryPair;
    }

    public String getFirstKeyword() {
        return firstKeyword;
    }

    public void setFirstKeyword(final String firstKeyword) {
        this.firstKeyword = firstKeyword;
    }

    public String getSecondKeyword() {
        return secondKeyword;
    }

    public void setSecondKeyword(final String secondKeyword) {
        this.secondKeyword = secondKeyword;
    }

    public int[] getMatchingDocuments() {
        return matchingDocuments;
    }

    public void setMatchingDocuments(final int[] matchingDocuments) {
        this.matchingDocuments = matchingDocuments;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(" kw2: ");
        sb.append(secondKeyword);
        sb.append(" matches: ");
        sb.append(getMatchingDocuments().length);
        return sb.toString();
    }

    public void setSeedCorpusFrequency(final int seedFrequency) {
            this.seedCorpusFrequency = seedFrequency;
    }
}
