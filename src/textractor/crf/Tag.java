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

package textractor.crf;

import it.unimi.dsi.mg4j.util.MutableString;

/**
 * User: Fabien Campagne
 * Date: Aug 18, 2006
 * Time: 6:18:40 PM
 * To change this template use File | Settings | File Templates.
 */
final class Tag {
    TextSpan span;
    String type;
    String text;

    @Override
    public String toString() {
        final MutableString buffer = new MutableString();
        buffer.append("tag, type: ");
        buffer.append(type);
        buffer.append(" text: ");
        buffer.append(text);
        buffer.append(' ');
        buffer.append(span);
        return buffer.toString();
    }
}
