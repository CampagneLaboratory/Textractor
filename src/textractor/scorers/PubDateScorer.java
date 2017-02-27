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

package textractor.scorers;

import it.unimi.dsi.mg4j.index.Index;
import it.unimi.dsi.mg4j.index.IndexIterator;
import it.unimi.dsi.mg4j.index.payload.IntegerPayload;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.score.AbstractWeightedScorer;
import it.unimi.dsi.mg4j.search.score.DelegatingScorer;

import java.io.IOException;

/**
 * This requires a field in the index named "pubdate" that field
 * should contain only int payloads.
 *
 * Derived from the VignaScorer.
 *
 * @author Kevin C. Dorff
 */
public class PubDateScorer extends AbstractWeightedScorer implements DelegatingScorer {
    private final Index pubDateIndex;
    private final boolean reverse;
    private IndexIterator allDocuments;

    public PubDateScorer(final Index pubDateIndex) {
        this(pubDateIndex, false);
    }

    public PubDateScorer(final Index pubDateIndex, final boolean reverse) {
        this.reverse = reverse;
        this.pubDateIndex = pubDateIndex;
    }

    public double score( final Index index ) throws IOException {
        final int document = documentIterator.document();
        double score = 0d;
        if (document == allDocuments.skipTo(document)) {
            final Object payload = allDocuments.payload();
            if (payload instanceof IntegerPayload) {
                if (reverse) {
                    score = - (((IntegerPayload) payload).get());
                } else {
                    score = ((IntegerPayload) payload).get();
                }
            }
        }
        return score;
	}

    @Override
    public void wrap(final DocumentIterator documentIterator) throws IOException {
        super.wrap(documentIterator);
        allDocuments = pubDateIndex.getReader().documents(0);
    }

    @Override
    public int nextDocument() throws IOException {
        final int next = documentIterator.nextDocument();
        if (next == -1) {
            if (allDocuments != null) {
                allDocuments.dispose();
            }
        }
        return next;
    }

    @Override
    public String toString() {
        return "PubDateScorer()";
    }

    public synchronized PubDateScorer copy() {
        final PubDateScorer scorer = new PubDateScorer(pubDateIndex);
        scorer.setWeights(index2Weight);
        return scorer;
    }

    /** Returns true.
     * @return true.
     */
    public boolean usesIntervals() {
        return true;
    }
}
