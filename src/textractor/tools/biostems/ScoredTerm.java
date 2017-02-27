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
 * Created by IntelliJ IDEA.
 * User: Fabien Campagne
 * Date: May 31, 2006
 * Time: 4:50:08 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ScoredTerm implements Comparable<ScoredTerm> {
    private MutableString term;
    private double score;

    public ScoredTerm(final MutableString term, final double score) {
        this.term = term;
        this.score = score;
    }

    public ScoredTerm(final String term, final double score) {
        this(new MutableString(term).compact(), score);
    }

    public MutableString getTerm() {
        return term;
    }

    public void setTerm(final MutableString term) {
        this.term = term;
    }

    public double getScore() {
        return score;
    }

    public void setScore(final double score) {
        this.score = score;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ScoredTerm) {
            final ScoredTerm other = (ScoredTerm) obj;
            return other.getTerm().equals(getTerm()) && other.getScore() == getScore();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        final MutableString text = new MutableString();
        text.append(term);
        text.append(' ');
        text.append(score);
        return text.toString();
    }

    @Override
    public int hashCode() {
        return getTerm().hashCode() ^ new Float(score).hashCode();
    }

    public int compareTo(final ScoredTerm scoredTerm) {
        return (scoredTerm.getScore() < getScore() ? -1 :
                scoredTerm.getScore() == getScore() ? 0 :
                        +1);
    }
}
