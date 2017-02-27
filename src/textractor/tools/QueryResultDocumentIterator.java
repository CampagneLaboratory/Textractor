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

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.mg4j.index.Index;
import it.unimi.dsi.mg4j.search.AbstractDocumentIterator;
import it.unimi.dsi.mg4j.search.IntervalIterator;
import it.unimi.dsi.mg4j.search.visitor.DocumentIteratorVisitor;

/**
 * Created by IntelliJ IDEA.
 * User: campagne
 * Date: Nov 5, 2004
 * Time: 1:22:38 PM
 */
public final class QueryResultDocumentIterator extends AbstractDocumentIterator {
    private int cursor;
    private final DocumentQueryResult queryResult;
    private int document;

    public QueryResultDocumentIterator(final DocumentQueryResult queryResult) {
        cursor = 0;
        this.queryResult = queryResult;
    }

    public IntervalIterator intervalIterator() {
        return new QueryResultIntervalIterator(queryResult, document);
    }

    public IntervalIterator intervalIterator(final Index index) {
        return new QueryResultIntervalIterator(queryResult, document);
    }

    public Reference2ReferenceMap<Index, IntervalIterator> intervalIterators() {
        return null;
    }

    public ReferenceSet<Index> indices() {
        return queryResult.getIndices();
    }

    public int nextDocument() {
        return nextInt();
    }

    @Override
    public int document() {
        return document;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Skips documents until a position of the iterator
     * where the next document will have a number equal or greater than the nextDocument parameter.
     *
     * @param n The number of the document to skip to.
     * @return  Integer.MAX_VALUE  if the document is not found.
     */
    public int skipTo(final int n) {
        int k = document;
        if (k >= n) {
            return k;
        }

        boolean hadNext;

        while((hadNext = hasNext()) && (k = nextInt() ) < n) {
	    ;
	}
        return hadNext ? k :  Integer.MAX_VALUE;
    }

    // TODO: mg4j 1.1
    public boolean accept(final DocumentIteratorVisitor visitor) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    // TODO: mg4j 1.1
    public boolean acceptOnTruePaths(final DocumentIteratorVisitor visitor) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Deprecated
    public int nextInt() {
        return next();
    }

    @Override
    public int skip(final int i) {
        final int newCursor = Math.min(queryResult.getNumberOfHits(), cursor + i);
        final int result = newCursor - cursor;
        cursor = newCursor;
        return result;

    }

    @Override
    public boolean hasNext() {
        return cursor < queryResult.getNumberOfHits();
    }

    @Override
    public Integer next() {
        try {
            document = queryResult.getHitNumber(cursor);
            return document;
        } finally {
            cursor += 1;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Method is not supported.");
    }

    public void dispose() {
	// nothing to dispose of
    }

    @Override
    public IntervalIterator iterator() {
        return intervalIterator();
    }
}
