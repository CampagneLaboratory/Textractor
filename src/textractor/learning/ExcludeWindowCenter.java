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

package textractor.learning;

import textractor.datamodel.annotation.AnnotatedTerm;
import textractor.datamodel.annotation.AnnotationSource;

/**
 * User: Fabien Campagne
 * Date: May 29, 2004
 * Time: 7:38:04 PM
 */
public final class ExcludeWindowCenter implements ExcludedPositionProvider {
    public ExcludeWindowCenter()  {
       super();
    }

    /**
     * Returns the set of positions that should be excluded from the feature
     * export for this annotation. This implementation assumes a single term
     * and excludes the term at the center of the window.
     *
     * @param annotation The annotation for which excluded positions are
     * sought.
     * @return An array of int, each element of the array codes for one
     * position (in word) within the annotation's text.
     */
    public int[] excludedPositions(final AnnotationSource annotation) {
        final AnnotatedTerm firstTerm =
            annotation.getTerm(AnnotationSource.FIRST_TERM);
        final int[] excludedPositions = new int[firstTerm.getTermLength()];
        int count = 0;
        for (int i = firstTerm.getStartPosition(); i < firstTerm.getStartPosition() + firstTerm.getTermLength(); ++i) {
            excludedPositions[count++] = i;
        }
        return excludedPositions;
    }
}
