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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Fabien Campagne
 *         Date: Feb 8, 2007
 *         Time: 1:50:35 PM
 */
public class PositionedParagraphSplitterIterator implements Iterator<PositionedText> {
    private final MutableString textToSplit;
    private final MutableString paragraphMarkerTag;
    private int position;

    /**
     * Raw byte positions of the text relative to the original source.
     */
    private final List<Integer> positions;

    public PositionedParagraphSplitterIterator(final String text,
                                               final List<Integer> positions,
                                               final MutableString markerTag) {
        super();
        textToSplit = new MutableString(text);
        this.positions = positions;
        paragraphMarkerTag = markerTag;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext() {
        return position < textToSplit.length();
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @throws java.util.NoSuchElementException iteration has no more elements.
     */
    public PositionedText next() {
        if (position >= textToSplit.length()) {
            throw new NoSuchElementException();
        }

        int nextPosition = textToSplit.indexOf(paragraphMarkerTag, position);
        if (nextPosition == -1) {
            nextPosition = textToSplit.length();
        }

        // make copies of the results
        final MutableString txt =
                textToSplit.substring(position, nextPosition);
        final List<Integer> pos = new ArrayList<Integer>(txt.length());
        pos.addAll(positions.subList(position, nextPosition));

        position = nextPosition + paragraphMarkerTag.length();

        return new PositionedText(txt, pos);
    }

    public void remove() {
        throw new UnsupportedOperationException("remove method is not supported by ParagraphSplitterIterator");
    }
}
