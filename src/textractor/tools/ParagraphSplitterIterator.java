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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Fabien Campagne
 *         Date: Feb 8, 2007
 *         Time: 1:50:35 PM
 */
public class ParagraphSplitterIterator implements Iterator<MutableString> {
    private final MutableString textToSplit;
    private final MutableString paragraphMarkerTag;
    private int position;

    public ParagraphSplitterIterator(final String text,
                                     final MutableString markerTagText) {
        super();
        textToSplit = new MutableString(text);
        paragraphMarkerTag = markerTagText;
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
     * @throws NoSuchElementException iteration has no more elements.
     */
    public MutableString next() {
        if (position >= textToSplit.length()) {
            throw new NoSuchElementException();
        }
        int nextPosition = textToSplit.indexOf(paragraphMarkerTag, position);
        if (nextPosition == -1) {
            nextPosition = textToSplit.length();
        }
        final MutableString nextParagraph =
                textToSplit.substring(position, nextPosition);
        position = nextPosition + paragraphMarkerTag.length();
        return nextParagraph;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove method is not supported by ParagraphSplitterIterator");
    }
}
