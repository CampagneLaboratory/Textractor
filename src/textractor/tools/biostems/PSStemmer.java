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

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import it.unimi.dsi.mg4j.query.parser.TokenMgrError;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import textractor.didyoumean.DidYouMeanI;
import textractor.scoredresult.ScoredResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This stemmer suggest related morphological word variants using a
 * probabilistic model of prefix/suffix occurence in a corpus.
 *
 * @author Fabien Campagne
 *         Created May 2nd 2006
 *         Time: 11:10:04 AM
 */
public final class PSStemmer {
    private Map<Integer, Object2FloatMap<MutableString>> prefixes; // len-> map for length
    private Map<Integer, Object2FloatMap<MutableString>> suffixes; // len-> map for length
    private DidYouMeanI dym;
    int maxPrefixLength;
    int maxSuffixLength;

    public PSStemmer(final Reader prefixModel, final Reader suffixModel, final DidYouMeanI dym) throws IOException {
        prefixes = new HashMap<Integer, Object2FloatMap<MutableString>>();
        suffixes = new HashMap<Integer, Object2FloatMap<MutableString>>();
        maxPrefixLength = read(prefixModel, prefixes);
        maxSuffixLength = read(suffixModel, suffixes);
        this.dym = dym;
    }

    private int read(final Reader model, final Map<Integer, Object2FloatMap<MutableString>> map) throws IOException {
        int maxLength = 0;
        final BufferedReader br = new BufferedReader(model);
        String line;
        while ((line = br.readLine()) != null) {
            final String[] tokens = line.split("[\t ]+");
            final String affix = tokens[0];
            final int length = affix.length();


            Object2FloatMap<MutableString> lengthDependentMap = map.get(length);
            if (lengthDependentMap == null) {
                lengthDependentMap = new Object2FloatOpenHashMap<MutableString>();
                map.put(length, lengthDependentMap);
            }
            final String probability = tokens[1];
            lengthDependentMap.put(new MutableString(affix).compact(), Float.parseFloat(probability));
            maxLength = Math.max(maxLength, length);
        }
        return maxLength;
    }

    public List<ScoredTerm> suggest(final MutableString word) throws ConfigurationException, IOException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException {
        final Collection<ScoredTerm> prefixStripped = new ArrayList<ScoredTerm>();
       word.toLowerCase();
        // get potential prefixStripped:
        // remove prefixes:
        final int wordLength = word.length();
        for (int length = 0; length < maxPrefixLength && length < wordLength; length++) {
            final Object2FloatMap map = prefixes.get(length);
            if (map != null) {
                final MutableString prefix = word.substring(0, length).compact();
                final MutableString remainder = word.substring(length);
                final float score = map.getFloat(prefix);
                if (score != 0) {
                    prefixStripped.add(new ScoredTerm(remainder, score));
                }
            }
        }
        final Collection<ScoredTerm> stems = new ArrayList<ScoredTerm>();

        // remove suffixes from potential prefixStripped, and combine prefix/suffix removal score:
        for (int length = 0; length < maxSuffixLength && length < wordLength; length++) {
            final Object2FloatMap map = suffixes.get(length);
            if (map == null) {
                continue;
            }
            final int offset = word.length() - length;
            final MutableString suffix = word.substring(offset).compact();
            final float score = map.getFloat(suffix);
            if (score == 0) {
                continue;
            }
            for (final ScoredTerm stemSuffix : prefixStripped) {
                //  System.out.println("last index: " + (stemSuffix.getTerm().length() - length));
                final int lastIndex = stemSuffix.getTerm().length() - length;
                if (lastIndex < 3) {
                    continue;  // reject stems shorter than 3 characters.
                }
                final MutableString stem = stemSuffix.getTerm().substring(0, lastIndex);
                stems.add(new ScoredTerm(stem, score * stemSuffix.getScore()));
            }

        }
        // for each potential stem, rank DYM results by prefix/suffix score
        final List<ScoredTerm> result = new ArrayList<ScoredTerm>();

        final List<ScoredResult> similarTerms;
        try {
            // obtain terms similar to words, ranked by similarity to word:
            similarTerms = dym.suggest(word.toString(), 0.01f);
        } catch (final ParseException parseE) {
            return null; // cannot  stem this word.
        } catch (final TokenMgrError e) {
            return null;
        }

        for (final ScoredResult similarTerm : similarTerms) {
            for (final ScoredTerm potentialStem : stems) {
                final MutableString similar = new MutableString(similarTerm.getTerm().toLowerCase());
                final MutableString stem = potentialStem.getTerm();
                final int stemIndex = similar.indexOf(stem);
                if (stemIndex != -1) {
                    final MutableString prefix = similar.substring(0, stemIndex);
                    final MutableString suffix = similar.substring(stemIndex + stem.length());
                    final double score = (float) potentialStem.getScore() *
                            getScore(prefixes, prefix) * getScore(suffixes, suffix);
                    if (score >= 1E-10) {   // scores below 1E-10 are mostly unrelated terms.
                        appendResult(result, similar, score);
                    }
                }
            }
        }
        Collections.sort(result, new ScoredTermComparator());

        return result;
    }

    private void appendResult(final List<ScoredTerm> results, final MutableString newResult, final double score) {
        for (final ScoredTerm result : results) {
            if (result.getTerm().equals(newResult.toString())) {
                if (result.getScore() > score) {
                    // keep best score
                    return;
                } else {
                    result.setScore(score);
                    return;
                }
            }
        }
        // new result was not found in list, just add:
        results.add(new ScoredTerm(newResult, score));
    }

    private float getScore(final Map<Integer, Object2FloatMap<MutableString>> lengthMap, final MutableString prefix) {
        final Object2FloatMap map = lengthMap.get(prefix.length());
        if (map == null) {
            return 0;
        } else {
            return map.getFloat(prefix);
        }
    }
}
