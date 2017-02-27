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

package textractor.query.clustering;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import textractor.database.DocumentIndexManager;

import java.io.Serializable;

/**
 * Storage for similarity between terms.
 *
 * @author Fabien Campagne
 *         Date: Oct 29, 2006
 *         Time: 11:35:43 AM
 */

@SuppressWarnings("serial")
public class TermSimilarityMatrix implements Serializable {
    private float[][] similarity;
    int[] termIndex;
    private String[] terms;
    private Object2IntMap<String> terms2ElementIndex;
    private Int2IntMap termIndex2ElementIndex;

    /**
     * Construct a matrix to store information about similarity of terms in an index.
     *
     * @param termIndex An array of term indices, according to a termMap.
     */
    public TermSimilarityMatrix(final int[] termIndex) {
        this(termIndex, null);
    }

    /**
     * Construct a matrix to store information about similarity of terms in an index.
     *
     * @param termIndex An array of term indices, according to a termMap.
     * @param terms     When non-null, the string representation of each term. Order must match termIndex order.
     */
    public TermSimilarityMatrix(final int[] termIndex, final String[] terms) {
        super();
        assert (terms == null || termIndex.length == terms.length)
                : "Length of termIndex and terms must match.";
        int countTermNotFound = 0;
        for (final int tIndex : termIndex) {
            if (tIndex == DocumentIndexManager.NO_SUCH_TERM) {
                countTermNotFound++;
            }
        }

        final int termCount = termIndex.length - countTermNotFound;
        final int[] foundTermIndices = new int[termCount];
        final String[] foundTerms = new String[termCount];
        int j = 0;
        for (int i = 0; i < termIndex.length; i++) {
            if (termIndex[i] != DocumentIndexManager.NO_SUCH_TERM) {
                foundTermIndices[j] = termIndex[i];
                if (terms != null) {
                    foundTerms[j] = terms[i];
                }
                j++;
            }
        }
        this.termIndex = foundTermIndices;
        this.terms = terms == null ? null : foundTerms;
        this.similarity = new float[termCount][termCount];
        if (this.terms != null) {
            terms2ElementIndex = new Object2IntOpenHashMap<String>();
        }
        termIndex2ElementIndex = new Int2IntOpenHashMap();
        int i = 0;

        for (final String term : this.terms) {
            terms2ElementIndex.put(term, i);
            termIndex2ElementIndex.put(this.termIndex[i], i);
            i++;
        }
    }

    /**
     * Returns the similarity value between two terms.
     *
     * @param term1 One term.
     * @param term2 The other term.
     * @return Similarity between term1 and term2 (may be assymetric).
     * @throws NullPointerException If terms where not provided at construction time.
     */
    public float getSimilarity(final String term1, final String term2) {
        final int term1ElementIndex = getTermElementIndex(term1);
        final int term2ElementIndex = getTermElementIndex(term2);
        assert term1ElementIndex != term2ElementIndex : "Term indices must be different.";
        return similarity[term1ElementIndex][term2ElementIndex];
    }

    /**
     * Returns the similarity value between two terms.
     *
     * @param termIndex1 The index of one term (index in the termMap).
     * @param termIndex2 The index of the other term (index in the termMap).
     * @return Similarity between term1 and term2 (may be assymetric).
     */
    public float getSimilarity(final int termIndex1, final int termIndex2) {
        return similarity[getTermElementIndex(termIndex1)][getTermElementIndex(termIndex2)];
    }

    /**
     * Associate a similarity value to two terms.
     *
     * @param term1 One term.
     * @param term2 The other term.
     * @param value The similarity value to associate to term1, term2.
     */
    public void setSimilarity(final String term1, final String term2,
                              final float value) {
        similarity[getTermElementIndex(term1)][getTermElementIndex(term2)] = value;
    }

    /**
     * Associate a similarity value to two terms.
     *
     * @param termIndex1 The index of one term (index in the termMap).
     * @param termIndex2 The index of the other term (index in the termMap).
     * @param value      The similarity value to associate to term1, term2.
     */
    public void setSimilarity(final int termIndex1, final int termIndex2,
                              final float value) {
        similarity[getTermElementIndex(termIndex1)][getTermElementIndex(termIndex2)] = value;
    }

    private int getTermElementIndex(final String term) {
        return terms2ElementIndex.get(term);
    }

    private int getTermElementIndex(final int termIndex) {
        return termIndex2ElementIndex.get(termIndex);
    }

    public String[] getTermsArray() {
        return terms;
    }

    /**
     * Term indices filtered for terms that occur in the index.
     *
     * @return Indices of terms that actually occur in the index.
     */

    public int[] getCleanTermIndices() {
        return termIndex;
    }

    public final void incrementElement(final int elementIndex1,
                                       final int elementIndex2) {
        similarity[elementIndex1][elementIndex2]++;
    }
}
