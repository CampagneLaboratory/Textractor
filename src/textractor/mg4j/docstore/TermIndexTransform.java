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

import java.io.IOException;

/**
 * Support transformation on term indices. Transformations can filter out some
 * terms and keep others, or permutate term indices. Transformations can be
 * applied in sequence.
 *
 * @author Fabien Campagne
 *         Date: Jun 25, 2006
 *         Time: 1:09:02 PM
 */
public abstract class TermIndexTransform {
    int position;

    /**
     * Obtain the term index before transformation.
     *
     * @param transformedIndex index of the term before transformation.
     * @return Index of the term before transformation.
     */
    abstract int getInitialTermIndex(final int transformedIndex);

    /**
     * Obtain the transformed term index.
     *
     * @param initialTermIndex index of a term before transformation.
     * @return Index of the term after transformation.
     */
    abstract int getTransformedIndex(final int initialTermIndex);

    /**
     * Returns the number of terms after transformation.
     *
     * @return number of terms.
     */
    abstract int getFinalSize();

    /**
     * Returns the number of terms before transformation.
     *
     * @return number of terms.
     */
     abstract int getInitialSize();

     protected final String getSaveFilename(final String basename,
	     final int counter) {
	 return basename + "-term-doc-freqs-" + counter + ".projection";
     }

    public final int getPosition() {
        return position;
    }

    public final void setPosition(final int position) {
        this.position = position;
    }
    /**
     * Save information associated with these frequency storage.
     * @param basename Basename of the index associated with this frequencies
     * storage implementation.
     */
    abstract void save(final String basename) throws IOException;

    /**
     * Load information that may have been associated with this frequency
     * storage.
     * @param basename Basename of the index associated with this frequencies
     * storage implementation.
     */
    abstract void load(final String basename) throws IOException;
}
