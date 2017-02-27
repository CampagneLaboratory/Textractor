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

package textractor.scoredresult;

import textractor.IResultTerm;

import java.io.Serializable;

/**
 *
 */
@SuppressWarnings("serial")
public final class ScoredResult implements IResultTerm, Serializable,
    Comparable<ScoredResult> {
    private String term;
    private double score;

    public ScoredResult() {
        super();
    }

    public ScoredResult(final String termVal, final double scoreVal) {
        setTerm(termVal);
        setScore(scoreVal);
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(final String termVal) {
        this.term = termVal;
    }

    public double getScore() {
        return score;
    }

    public void setScore(final double score) {
        this.score = score;
    }

    public int compareTo(final ScoredResult compare) {
        final int returnValue;

        if (compare.getScore() > getScore()) {
            returnValue = 1;
        } else if (compare.getScore() == getScore()) {
            returnValue = 0;
        } else {
            returnValue = -1;
        }

        return returnValue;
    }

    @Override
    public boolean equals(final Object anObject) {
        if (anObject == null) {
            return false;
        }

        if (!(anObject instanceof ScoredResult)) {
            return false;
        }

        final ScoredResult comp = (ScoredResult) anObject;
        return comp.getTerm().equals(term) && comp.getScore() == score;

    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()4
     */
    @Override
    public String toString() {
        return String.format("{'%s':%d}", term, score);
    }
}
