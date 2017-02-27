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

package textractor.mg4j.offsets;

import java.io.IOException;

/**
 * This data structure associates integer keys to long values. The key
 * assumption is that values appear in increasing order (as they do in the
 * offsets of the inverted index), that is if key 2 > key 1 then,
 * value 2> value 1. This assumption needs only hold in each LongDenseGroup
 * for this implementation.
 *
 * @author Fabien Campagne
 *         Date: Mar 9, 2006
 *         Time: 10:32:39 PM
 */
public interface LongDenseList {
    long getLong(final int index) throws IOException;
}
