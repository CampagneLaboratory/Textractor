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

package textractor.tools;

import it.unimi.dsi.mg4j.util.MutableString;

import java.util.List;

/**
 * Class used to bundle text along with a list of positions representing
 * the original location.
 */
public final class PositionedText {
    /**
     * The raw text.
     */
    private final MutableString text;

    /**
     * Positions of characters in the text relative to the original
     * document source.
     */
    private final List<Integer> positions;

    public PositionedText(final MutableString text,
                          final List<Integer> positions) {
        super();
        this.text = text;
        this.positions = positions;
    }

    public MutableString getText() {
        return text;
    }

    public List<Integer> getPositions() {
        return positions;
    }
}
