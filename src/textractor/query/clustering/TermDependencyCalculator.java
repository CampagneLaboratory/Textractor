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

import textractor.database.DocumentIndexManager;

/**
 * Calculate correlation/dependencies among terms.
 *
 * @author Fabien Campagne
 *         Date: Oct 29, 2006
 *         Time: 11:31:43 AM
 */
public class TermDependencyCalculator  {
    protected final DocumentIndexManager docManager;

    public TermDependencyCalculator(final DocumentIndexManager docManager) {
        this.docManager = docManager;
    }


    public TermSimilarityMatrix calculate(final String[] terms)  {
        final int[] termIndex = new int[terms.length];
        int i = 0;
        for (final String term : terms) {
            termIndex[i++] = docManager.findTermIndex(term);
        }
        return calculate(termIndex, terms);
    }

    public TermSimilarityMatrix calculate(final int[] termIndex,
                                          final String[] terms){
        final TermSimilarityMatrix result =
                new TermSimilarityMatrix(termIndex, terms);
        calculate(result, result.getCleanTermIndices());
        return result;
    }

    public void calculate(final TermSimilarityMatrix result,
                          final int[] termIndex)  {
    }
}
