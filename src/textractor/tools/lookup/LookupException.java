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

package textractor.tools.lookup;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Apr 26, 2004
 * Time: 6:21:21 PM
 * To change this template use File | Settings | File Templates.
 */
public final class LookupException extends Exception {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 1L;

    public LookupException(final String e) {
        super("An error occured while reading in the list being looked up: "
                + e);
    }
}
