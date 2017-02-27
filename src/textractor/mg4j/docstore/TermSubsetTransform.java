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

package textractor.mg4j.docstore;

import it.unimi.dsi.fastutil.io.BinIO;
import textractor.database.DocumentIndexManager;

import java.io.IOException;

/**
 * This transformation filters out terms that do not meet given criteria.
 *
 * @author Fabien Campagne
 *         Date: Jun 25, 2006
 *         Time: 11:45:19 AM
 */
public class TermSubsetTransform extends TermIndexTransform {
    private int reducedSize;
    private int[] reducedIndexToDelegateIndex;
    private int[] delegateIndexToReducedIndex;
    private TermIndexTransform transform;

    public TermSubsetTransform(final TermIndexTransform transform) {
        this.transform = transform;
        setPosition(transform.getPosition() + 1);
        filterTerms();
    }

    public TermSubsetTransform(final String basename, final int position) throws IOException {

        setPosition(position);
        load(basename);
        reducedSize = reducedIndexToDelegateIndex.length;
        transform = new UnityTransform(reducedSize);
    }

    final void filterTerms() {
        int includedTermNumber = 0;
        final int termNumber = transform.getInitialSize();
        final boolean[]  excludeTerm = new boolean[termNumber];
        for (int termIndex = 0; termIndex < termNumber; ++termIndex) {

            excludeTerm[termIndex] = !includeTerm(termIndex);
            if (!excludeTerm[termIndex]) {
		++includedTermNumber;
	    }
        }

        reducedIndexToDelegateIndex = new int[includedTermNumber];
        int permutedIndex = 0;
        delegateIndexToReducedIndex = new int[termNumber];
        for (int i = 0; i < termNumber; ++i) {

            if (!excludeTerm[i]) {
                reducedIndexToDelegateIndex[permutedIndex] = i;
                delegateIndexToReducedIndex[i] = permutedIndex;
                ++permutedIndex;
            } else {
                delegateIndexToReducedIndex[i] = DocumentIndexManager.NO_SUCH_TERM;
            }
        }
        this.reducedSize = includedTermNumber;
    }

    /**
     * Determine if term must be included in subset.
     *
     * @param termIndex Index of term.
     * @return True if term is included in subset, False otherwise.
     */
    boolean includeTerm(final int termIndex) {
        return true;
    }


    public final int getTransformedSize() {
        return reducedSize;
    }

    @Override
    public final int getInitialSize() {
        return transform.getInitialSize();
    }

    @Override
    public final int getInitialTermIndex(final int transformedIndex) {
        return transform.getInitialTermIndex(reducedIndexToDelegateIndex[transformedIndex]);
    }

    @Override
    public final int getTransformedIndex(final int initialTermIndex) {
        return delegateIndexToReducedIndex[transform.getTransformedIndex(initialTermIndex)];
    }

    @Override
    public final int getFinalSize() {
        return reducedSize;
    }


    @Override
    public final void save(final String basename) throws IOException {
        if (this.reducedIndexToDelegateIndex != null) {
            BinIO.storeInts(this.reducedIndexToDelegateIndex, getSaveFilename(basename, position));
        }
    }

    @Override
    public final void load(final String basename) throws IOException {
        reducedIndexToDelegateIndex = BinIO.loadInts(getSaveFilename(basename, position));
    }


}
