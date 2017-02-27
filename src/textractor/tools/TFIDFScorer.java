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

package textractor.tools;

import it.unimi.dsi.mg4j.index.Index;
import it.unimi.dsi.mg4j.search.score.AbstractScorer;
import it.unimi.dsi.mg4j.search.score.Scorer;

/**
 * TF IDF Scorer (term frequency).
 * This class is not currently used. All tf-idf scoring takes
 * places in Twease's QueryEngine.
 */
public final class TFIDFScorer extends AbstractScorer {
    public TFIDFScorer() {
        super();
    }

    /**
     * Computes the TFIDF score of an interval iterator.
     * @return the score.
     */
    public double score() {
        return 0;
    }

    public double score(final Index index) {
        return 0;
    }

    public boolean usesIntervals() {
        return false;
    }

    public Scorer copy() {
        return new TFIDFScorer();
    }

    @Override
    public String toString() {
        return "TFIDFScorer()";
    }
}
