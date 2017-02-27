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

package textractor.pseudorelevance;

/**
 * @author Fabien Campagne (Nov 30, 2006)
 */
public class QueryTerm {
    private CharSequence term;
    private int termIndex;
    private int rt;
    private int R;
    private float weight;
    private int numConsensusDocs;

    public QueryTerm(final String term, final float weight) {
        this.term = term;
        this.weight = weight;
        this.termIndex = -1;
        this.numConsensusDocs = -1;
    }

    public QueryTerm(final String term, final float score, final int rt, final int R) {
        this(term, score);
        this.rt = rt;
        this.R = R;
        if (rt > R) {
            this.R = rt;   // rt cannot be more than the number of documents examined.
        }
    }

    public QueryTerm(final String term, final int termIndex, final float score, final int rt, final int R) {
        this(term, score, rt, R);
        this.termIndex = termIndex;
    }

    /**
     * Get the term.
     * @return the term.
     */
    public CharSequence getTerm() {
        return term;
    }

    /**
     * Set the term.
     * @param term the term
     */
    public void setTerm(final CharSequence term) {
        this.term = term;
    }

    /**
     * Get the number of top documents examined for query
     * expansion in which the term appears.
     * @return the number of top documents examined for query
     * expansion in which the term appears
     */
    public int getRt() {
        return rt;
    }

    /**
     * Set the number of top documents examined for query expansion
     * in which the term appears.
     * @param rt the number of top documents examined for query
     * expansion in which the term appears
     */
    public void setRt(final int rt) {
        this.rt = rt;
    }

    /**
     * Get the number of top documents examined for query expansion.
     * @return the number of top documents examined for query expansion
     */
    public int getR() {
        return R;
    }

    /**
     * Set the number of top documents examined for query expansion.
     * @param r the number of top documents examined for query expansion
     */
    public void setR(final int r) {
        R = r;
    }

    /**
     * Get the weight for the term.
     * @return the weight for the term
     */
    public float getWeight() {
        return weight;
    }

    /**
     * Set the weight for the term.
     * @param weight the weight for the term
     */
    public void setWeight(final float weight) {
        this.weight = weight;
    }

    /**
     * Get the term index if defined (or -1 if not defined).
     * @return the term index
     */
    public int getTermIndex() {
        return termIndex;
    }

    /**
     * Set the term index (or use -1 if not defined).
     * @param termIndex the term index
     */
    public void setTermIndex(final int termIndex) {
        this.termIndex = termIndex;
    }

    /**
     * Get the number of consensus documents this term is in
     * (or use -1 if not defined).
     * @return the number of consensus documents this term is in
     */
    public int getNumConsensusDocs() {
        return this.numConsensusDocs;
    }

    /**
     * Set the number of consensus documents this term is in
     * (or use -1 if not defined).
     * @param numConsensusDocs the number of consensus documents this term is in
     */
    public void setNumConsensusDocs(final int numConsensusDocs) {
        this.numConsensusDocs = numConsensusDocs;
    }
}
