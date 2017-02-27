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

import it.unimi.dsi.mg4j.index.IndexIterator;
import textractor.database.DocumentIndexManager;

import java.io.IOException;

/**
 * Fill the result matrix with term co-occurence document counts.
 *
 * @author Fabien Campagne
 *         Date: Oct 29, 2006
 *         Time: 12:09:40 PM
 */
public class TermCoOccurenceCalculator extends TermDependencyCalculator {
    public TermCoOccurenceCalculator(final DocumentIndexManager docManager) {
        super(docManager);

    }

    @Override
    public synchronized void calculate(final TermSimilarityMatrix result,
                                       final int[] termIndex) {
        try {
            final CountingOrOperator iterator = produceOrIterator(termIndex);
            System.out.println("iterator: " + iterator);

            while (iterator.hasNext()) {
                iterator.nextDocument();
                iterator.incrementMatrixElements(result);
            }
            iterator.dispose();

            // copy the half matrix that was populated over the other half:
            final int[] termIndices = result.termIndex;

            for (int y = 0; y < termIndices.length; y++) {
                for (int x = y; x < termIndices.length; x++) {
                    result.setSimilarity(y, x, result.getSimilarity(x, y));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error querying for term co-occurence information.", e);
        }
    }


    private CountingOrOperator produceOrIterator(final int[] termIndex) throws IOException {
        final IndexIterator[] termIterators = new IndexIterator[termIndex.length];
        int i = 0;
        for (final int tIndex : termIndex) {
            termIterators[i] = docManager.getIndex().getReader().documents(tIndex);
            termIterators[i].term(docManager.termAsCharSequence(tIndex));
            termIterators[i].id(tIndex);
            i++;
        }
        return (CountingOrOperator) CountingOrOperator.getInstance(termIterators);
    }
}
