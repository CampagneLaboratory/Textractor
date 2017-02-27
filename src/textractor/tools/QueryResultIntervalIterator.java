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

import it.unimi.dsi.mg4j.search.Interval;
import it.unimi.dsi.mg4j.search.IntervalIterator;

import java.io.IOException;
import java.util.Iterator;

public final class QueryResultIntervalIterator implements IntervalIterator {
    private final Iterator<Interval> intervals;

    public QueryResultIntervalIterator(final DocumentQueryResult queryResult,
                                       final int document) {
        intervals = queryResult.getIntervalIterator(document);
    }

    public void reset() {
        throw new UnsupportedOperationException("Method is not supported.");
    }

    public int extent() {
        throw new UnsupportedOperationException("Method is not supported.");
    }

    /**
     * Returns the next interval provided by this interval iterator, or <code>null</code> if no more intervals are available.
     * <p/>
     * <p>This method has been reintroduced in MG4J 1.2 with a different semantics.
     * The special return value <code>null</code> is used to mark the end of iteration. The reason
     * for this change is providing <em>fully lazy</em> iteration over intervals. Fully lazy iteration
     * does not provide an <code>hasNext()</code> method&mdash;you have to actually ask for the next
     * element and check the return value. Fully lazy iteration is much lighter on method calls (half) and
     * in most (if not all) MG4J classes leads to a much simpler logic. Moreover, {@link #nextInterval()}
     * can be specified as throwing an {@link java.io.IOException}, which avoids the pernicious proliferation
     * of try/catch blocks in very short, low-level methods (it was having a detectable impact on performance).
     *
     * @return the next interval, or <code>null</code> if no more intervals are available.
     */
    public Interval nextInterval() throws IOException {
        if (intervals.hasNext()) {
            return intervals.next();
        } else {
            return null;
        }
    }

    public boolean hasNext() {
        return intervals.hasNext();
    }

    @Deprecated
    public Interval next() {
        return intervals.next();
    }

    public void remove() {
        throw new UnsupportedOperationException("Method is not supported.");
    }
}
