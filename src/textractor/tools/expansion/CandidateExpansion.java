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

package textractor.tools.expansion;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.mg4j.index.Index;
import textractor.database.DocumentIndexManager;
import textractor.tools.DocumentQueryResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: campagne
 * Date: Nov 5, 2004
 * Time: 12:49:05 PM
 * To change this template use File | Settings | File Templates.
 */
public final class CandidateExpansion {
    private final List<String> candidateExpansion;
    private int frequency;
    private int nucleusFrequency;
    private CandidateExpansion shorterExpansion;
    private final Int2IntMap newWordCandidates = new Int2IntOpenHashMap();
    /** Set of extension words that did not produce valid expansions. */
    private final Set<Integer> wordsTried = new IntOpenHashSet();

    /**
     * Returns the frequency of the nucleus used to fish this expansion.
     * The nucleus is the pattern that we use to identify the documents,
     * and grow the extension. For instance: ( acronym ).
     *
     * @return frequency of the nucleus.
     */
    public int getNucleusFrequency() {
        return nucleusFrequency;
    }

    /**
     * Sets the frequency of the nucleus used to fish this expansion.
     *
     * @param nucleusFrequency number of times the nucleus occurs in the corpus.
     */
    public void setNucleusFrequency(final int nucleusFrequency) {
        this.nucleusFrequency = nucleusFrequency;
    }

    public CandidateExpansion() {
        this(new ArrayList<String>());
    }

    /**
     * @param indices set of indices, or null if already provided
     * @return The cached result of this query.
     */
    public DocumentQueryResult getQueryResult(final ReferenceSet<Index> indices) {
        if (queryResult != null) {
            return queryResult;
        } else {
            queryResult = new DocumentQueryResult(indices);
            return queryResult;
        }
    }

    public CandidateExpansion(final List<String> list) {
        candidateExpansion = list;

    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(final int frequency) {
        this.frequency = frequency;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        final Iterator<String> it = candidateExpansion.iterator();
        if (it.hasNext()) {
            sb.append(it.next());
        }
        while (it.hasNext()) {
            sb.append(' ');
            sb.append(it.next());

        }
        return sb.toString();
    }

    public int length() {
        return candidateExpansion.size();
    }

    public void add(final String word) {
        candidateExpansion.add(word);
    }

    public void addAll(final CandidateExpansion exp) {
        candidateExpansion.addAll(exp.candidateExpansion);
    }

    public CandidateExpansion tail() {
        return new CandidateExpansion(candidateExpansion.subList(1, candidateExpansion.size()));
    }

    public String head() {
        return candidateExpansion.iterator().next();
    }

    public Iterator<String> iterator() {
        return candidateExpansion.iterator();
    }

    private DocumentQueryResult queryResult;

    @Override
    public boolean equals(final Object obj) {
        return toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public String firstWord() {
        return head();
    }

    public CandidateExpansion getShorterExpansion() {
        return shorterExpansion;
    }

    public void setShorterExpansion(final CandidateExpansion shorterExpansion) {
        this.shorterExpansion = shorterExpansion;
    }

    public String lastWord() {
        return this.candidateExpansion.get(candidateExpansion.size() - 1);
    }

    public Iterator<Integer> iterateNewWords() {
        return newWordCandidates.keySet().iterator();
    }

    public void addSupportToWord(final int word, final int i) {
        final int previousFrequency = newWordCandidates.get(word);
        newWordCandidates.put(word, previousFrequency + i);
    }

    public int getWordSupport(final int word) {
        return newWordCandidates.get(word);
    }

    public void removeNewWord(final int word) {
        newWordCandidates.remove(word);
    }

    public void setWordSupport(final int word, final int inferredWordFrequency) {
        newWordCandidates.put(word, inferredWordFrequency);
    }

    public void adjust(final int minimalSupport, final float inclusionCutoff, final int skippedSentences) {
        Iterator<Integer> words;
        if (skippedSentences > 0) {
            // if sentences were skipped, the real frequency of new words can only be inferred.
            words = iterateNewWords();
            int sumObserved = 0;
            while (words.hasNext()) {
                final int word =  words.next();
                sumObserved += getWordSupport(word);
            }
            words = iterateNewWords();
            while (words.hasNext()) {
                final int word = words.next();
                final int observedFrequency = getWordSupport(word);
                final int inferredMissingFrequency = observedFrequency * skippedSentences / sumObserved;
                final int inferredWordFrequency = observedFrequency + inferredMissingFrequency;
                setWordSupport(word, inferredWordFrequency);
            }
        }
        words = iterateNewWords();
        while (words.hasNext()) {
            final int word =  words.next();
            final int observedFrequency = getWordSupport(word);
            // filter out those words that do not have enough support..
            if (observedFrequency < minimalSupport) {
                removeNewWord(word);
            }

            if (observedFrequency < (inclusionCutoff * 100 / this.frequency)) {
                removeNewWord(word);
            }
        }
    }

    public CandidateExpansion suggestLongerExpansion(final DocumentIndexManager docmanager,
            final boolean extendOnLeft, final int minimalSupport) {
        final Iterator<Integer> wordIterator = iterateNewWords();
        while (wordIterator.hasNext()) {
            final int word = wordIterator.next();

            // create a new candidate expansion with: <word> <previous-candidate-expansion>

            if (word != DocumentIndexManager.NO_SUCH_TERM) {
                if (wordsTried.contains(word)) {
                    continue;
                }
                if (this.getWordSupport(word) < minimalSupport) {
                    continue;
                }

                final CandidateExpansion longerExpansion =
                    new CandidateExpansion();

                if (extendOnLeft) {
                    longerExpansion.add(docmanager.termAsString(word));
                }
                longerExpansion.addAll(this);
                if (!extendOnLeft) {
                    longerExpansion.add(docmanager.termAsString(word));
                }
                longerExpansion.setShorterExpansion(this);
                wordsTried.add(word);
                return longerExpansion;
            }
        }
        return null;
    }
}
