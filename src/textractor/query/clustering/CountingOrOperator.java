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

import it.unimi.dsi.mg4j.index.Index;
import it.unimi.dsi.mg4j.index.IndexIterator;
import it.unimi.dsi.mg4j.index.MultiTermIndexIterator;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Fabien Campagne
 * Date: Feb 7, 2007
 * Time: 3:41:54 PM
 * To change this template use File | Settings | File Templates.
 */
class CountingOrOperator extends MultiTermIndexIterator {

    /**
     * Returns an index iterator that merges the given array of iterators.
     * This method requires that at least one iterator is provided. The frequency is computed as a max,
     * and {@link #index()} will return the result of the same method on the first iterator.
     *
     * @param indexIterator the iterators to be joined (at least one).
     * @return a merged index iterator.
     * @throws IllegalArgumentException if no iterators were provided.
     */
    public static IndexIterator getInstance(final IndexIterator... indexIterator) throws IOException {
        return getInstance(Integer.MIN_VALUE, indexIterator);
    }

    /**
     * Returns an index iterator that merges the given array of iterators.
     * <p/>
     * <P>Note that the special case of the empty and of the singleton arrays
     * are handled efficiently. The frequency is computed as a max, and
     * {@link #index()} will return <code>index</code>.
     *
     * @param index         the index that wil be returned by {@link #index()}.
     * @param indexIterator the iterators to be joined.
     * @return a merged index iterator.
     */
    public static IndexIterator getInstance(final Index index, final IndexIterator... indexIterator) throws IOException {
        return getInstance(Integer.MIN_VALUE, index, indexIterator);
    }

    /**
     * Returns an index iterator that merges the given array of iterators.
     * This method requires that at least one iterator is provided.
     *
     * @param defaultFrequency the default term frequency (or {@link Integer#MIN_VALUE} for the max).
     * @param indexIterator    the iterators to be joined (at least one).
     * @return a merged index iterator.
     * @throws IllegalArgumentException if no iterators were provided, or they run on different indices.
     */
    public static IndexIterator getInstance(final int defaultFrequency, final IndexIterator... indexIterator) throws IOException {
        if (indexIterator.length == 0) {
            throw new IllegalArgumentException();
        }
        return getInstance(defaultFrequency, indexIterator[0].index(), indexIterator);
    }

    /**
     * Returns an index iterator that merges the given array of iterators.
     * <p/>
     * <P>Note that the special case of the empty and of the singleton arrays
     * are handled efficiently.
     *
     * @param defaultFrequency the default term frequency (or {@link Integer#MIN_VALUE} for the max).
     * @param index            the index that wil be returned by {@link #index()}.
     * @param indexIterator    the iterators to be joined.
     * @return a merged index iterator.
     * @throws IllegalArgumentException if there is some iterator on an index different from <code>index</code>.
     */
    public static IndexIterator getInstance(final int defaultFrequency, final Index index, final IndexIterator... indexIterator) throws IOException {
        if (indexIterator.length == 0) {
            return index.emptyIndexIterator;
        }
        if (indexIterator.length == 1) {
            return indexIterator[0];
        }
        return new CountingOrOperator(defaultFrequency, indexIterator);
    }

    /**
     * Creates a new document iterator that merges the given array of iterators.
     *
     * @param defaultFrequency the default term frequency (or {@link Integer#MIN_VALUE} for the max).
     * @param indexIterator    the iterators to be joined.
     */
    protected CountingOrOperator(final int defaultFrequency, final IndexIterator... indexIterator) throws IOException {
        super(defaultFrequency, indexIterator);
    }

    void incrementMatrixElements(final TermSimilarityMatrix M) {
        final int s = computeFront();
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < s; j++) {
                M.incrementElement(front[i], front[j]);
            }
        }
    }
     /*   "A A B C D E",
                "A B C F I D S",
                "A S K X Y N N S S",
                "D S B K X U W",
                "A",
                "B E",*/
}
