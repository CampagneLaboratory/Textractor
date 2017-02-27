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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import it.unimi.dsi.mg4j.query.parser.TokenMgrError;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.TextractorDatabaseException;
import textractor.didyoumean.DidYouMeanI;
import textractor.scoredresult.ScoredResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Removes word prefix and suffixes leveraging a large word dictionary.
 *
 * @author Fabien Campagne
 *         Created May 2nd 2006
 *         Time: 11:10:04 AM
 */
public final class BioStemmer {
    private DidYouMeanI dym;
    private int maxInspect = 50;
    private LongestCommonSubstring lcs;
    private Object2IntMap<MutableString> tallies;

    public BioStemmer(final DidYouMeanI dym) {
        super();
        this.dym = dym;
        lcs = new LongestCommonSubstring(50);
        tallies = new Object2IntOpenHashMap<MutableString>();
    }

    public MutableString stem(final String word) throws ConfigurationException, IOException, TextractorDatabaseException, ParseException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException {
        return stem(word, false);
    }

    /**
     * Process word and returns a substring where likely prefix and suffix have
     * been removed.
     *
     * @param word
     * @return the Word stripped of prefix and suffixes or null if stemming is
     *         not possible for this word.
     */
    public MutableString stem(String word, final boolean stripPrefixSuffix) throws ConfigurationException, IOException, TextractorDatabaseException, ParseException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException {
        word = word.toLowerCase();
        final List<ScoredResult> similarTerms;
        try {
            // obtain terms similar to words, ranked by similarity to word:
            similarTerms = dym.suggest(word, 0.01f);
        } catch (final ParseException parseE) {
            return null; // cannot  stem this word.
        } catch (final TokenMgrError e) {
            return null;
        }
        MutableString maxFrequencySubstring;
        final String wordToStem = word;

        boolean done = false;
        boolean stripPrefix = stripPrefixSuffix;
        boolean stripSuffix = stripPrefixSuffix;
        do {
            maxFrequencySubstring = obtainBestOverlap(similarTerms, word);
            if (maxFrequencySubstring == null) {
                return null;
            }
            if (!stripPrefixSuffix || maxFrequencySubstring.equals(word)) {
                return maxFrequencySubstring;
            }
            final String substring = maxFrequencySubstring.toString();

            final int index = wordToStem.indexOf(substring);
            final int suffixIndex = wordToStem.length() - substring.length();
            final int remainderLength = word.length() - substring.length();
            if (index == 0 && stripPrefix) {   //substring is a prefix
                if (substring.length() <= remainderLength) {    // do not remove prefix if this would result in a stem
                    // shorter than the prefix..
                    word = word.substring(substring.length());
                }
                stripPrefix = false;
                if (!stripSuffix) {
                    return new MutableString(word);
                }
            } else if (index == suffixIndex && stripSuffix) {         // substring is a suffix
                // do not remove suffix if this would result in a stem
                // shorter than the suffix..

                if (substring.length() <= remainderLength) {
                    word = word.substring(0, word.length() - substring.length());
                }
                stripSuffix = false;
                if (!stripPrefix) {
                    return new MutableString(word);
                }
            } else {
                done = true;
            }
        } while (!done);

        return maxFrequencySubstring;
    }

    private MutableString obtainBestOverlap(
            final List<ScoredResult> similarTerms, final String word) {
        MutableString stem;

        // extract the stem (word stripped of prefix and suffix) of each similar word
        // and tally each unique stem.
        tallies.clear();
        for (int i = 0; i < maxInspect && i < similarTerms.size(); i++) {
            stem = lcs.longestSubstring(word, similarTerms.get(i).getTerm().toLowerCase());
            tally(stem);
        }

        // identify the stem with the highest frequency in the maxInspect most similar words:
        MutableString maxFrequencyStem = null;
        int maxFrequency = 0;
        for (final Map.Entry<MutableString, Integer> entry : tallies.entrySet()) {
            final MutableString tmpStem = entry.getKey();
            if (entry.getValue() > maxFrequency && tmpStem.length() >= 3) {
                maxFrequency = entry.getValue();
                maxFrequencyStem = tmpStem;
            }
        }
        return maxFrequencyStem;
    }

    private void tally(MutableString stem) {
        stem = stem.copy().compact();
        final int count = tallies.getInt(stem);
        tallies.put(stem, count + 1);
    }
}
