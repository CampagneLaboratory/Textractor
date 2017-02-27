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

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.mg4j.index.Index;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.Interval;
import it.unimi.dsi.mg4j.search.IntervalIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Keeps track of the results of a document query. This can be used as a cache
 * to avoid redoing the query.
 *
 * @author Fabien Campagne Date: Nov 5, 2004 Time: 4:58:46 PM
 */
public final class DocumentQueryResult {
    private final IntArrayList documents;
    private final ReferenceSet<Index> indices;
    private final Map<Integer, List<Interval>> intervalList;

    /**
     * @param indices Set of full text indices that was used to collect this
     *        expansion.
     */
    public DocumentQueryResult(final ReferenceSet<Index> indices) {
        intervalList = new Int2ObjectLinkedOpenHashMap<List<Interval>>();
        documents = new IntArrayList();
        this.indices = indices;
    }

    public DocumentQueryResult(final DocumentIterator result) throws IOException {
        this(result.indices());
        populateFrom(result);
    }

    public int[] getDocuments() {
        documents.trim();
        return documents.elements();
    }

    public ReferenceSet<Index> getIndices() {
        return indices;
    }

    /**
     * Add a match for this expansion. This method must be called once for each
     * document in which this expansion appears.
     *
     * @param document number of the document where this expansion occurs.
     */
    public void addMatch(final int document) {
        documents.add(document);
        intervalList.put(document, new ArrayList<Interval>());
    }

    /**
     * @param document number of the document where this expansion occurs.
     * @param interval Interval of a match in this document.
     */
    public void addInterval(final int document, final Interval interval) {
        final List<Interval> list = intervalList.get(document);
        list.add(interval);
    }

    /**
     * Returns an iterator over the documents and intervals that match this
     * expansion.
     *
     * @return Iterator over the documents of the cached result.
     */
    public DocumentIterator getDocumentIterator() {
        return new QueryResultDocumentIterator(this);
    }

    public int getHitNumber(final int cursor) {
        return documents.getInt(cursor);
    }

    public int getNumberOfHits() {
        return documents.size();
    }

    // TODO: This should probably return a IntervalIterator object
    public Iterator<Interval> getIntervalIterator(final int document) {
        final List<Interval> intervals = intervalList.get(document);
        return intervals.iterator();
    }

    /**
     * Populate the cache from a query result.
     *
     * @param result Result of a full text query.
     */
    public void populateFrom(final DocumentIterator result) throws IOException {
        while (result.hasNext()) {
            final int document = result.nextDocument();
            addMatch(document);
            final IntervalIterator iterator = result.intervalIterator();
            while (iterator.hasNext()) {
                final Interval interval = iterator.nextInterval();
                addInterval(document, interval);
            }
        }
    }
}
