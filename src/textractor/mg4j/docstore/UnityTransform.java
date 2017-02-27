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
 * This transformation does not change term indices. It is used as a default
 * transform.
 * @author Fabien Campagne
 *         Date: Jun 25, 2006
 *         Time: 1:14:57 PM
 */
public final class UnityTransform extends TermIndexTransform {
    private final int size;

    public UnityTransform(final int size) {
	super();
        this.size = size;
        position = 0;
    }

    @Override
    public int getInitialTermIndex(final int transformedIndex) {
        return transformedIndex;
    }

    @Override
    public int getTransformedIndex(final int initialTermIndex) {
        return initialTermIndex;
    }

    @Override
    public int getFinalSize() {
        return size;
    }

    @Override
    public int getInitialSize() {
        return size;
    }

    @Override
    public void save(final String basename) throws IOException {
        // nothing to save
    }

    @Override
    public void load(final String basename) throws IOException {
       // nothing to load
    }
}
