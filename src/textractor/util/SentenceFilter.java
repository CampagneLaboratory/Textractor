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

package textractor.util;

import textractor.datamodel.TextractorDocument;

/**
 * User: Fabien Campagne Date: Nov 7, 2004 Time: 10:08:02 AM
 */
public interface SentenceFilter {
    /**
     * Determine if a sentence should be removed from the index, based on the
     * text that it contains.
     *
     * @param text Text of the sentence
     * @return True if the sentence should be removed from the index, False if
     *         the sentence should be indexed.
     */
    boolean filterSentence(CharSequence text);

    /**
     * Determine if a sentence should be removed from the index, based on the
     * text that it contains.
     *
     * @param document Document to be filtered
     * @return True if the sentence should be removed from the index, False if
     *         the sentence should be indexed.
     */
    boolean filterSentence(TextractorDocument document);
}
