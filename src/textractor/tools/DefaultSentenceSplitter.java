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
import java.util.List;

/**
 * Splits text into sentences. This code is refactored from a previous
 * version in Html2Text2DB.
 * User: Fabien Campagne
 * Date: Oct 17, 2004
 * Time: 6:20:54 PM
 */
public class DefaultSentenceSplitter implements SentenceSplitter {
    /**
     * Create a new Sentence Splitter.
     */
    public DefaultSentenceSplitter() {
        super();
    }

   /**
     * Split the text and return an iterator over predicted sentences.
     * The iterator provides access to CharSequence instances. One for each
     * predicted sentence.
     * @param text English text.
     * @return An iterator over the sentences predicted in the text.
     */
    public Iterator<MutableString> split(final String text) {
        return new SentenceSplitterIterator(text);
    }

    /**
     * Split the text and return an iterator over predicted sentences,
     * without adding spacers around punctuation.
     * @param text The text to be split
     * @param punctuationSpacing
     * @return An iterator over the sentences predicted in the text.
     */
    public Iterator<MutableString> split(final String text,
            final boolean punctuationSpacing) {
        return new SentenceSplitterIterator(text, punctuationSpacing);
    }

    /**
     * Split the text and return an iterator over predicted sentences.
     * The iterator provides access to text and their associated
     * positions. One for each predicted sentence.
     * @param text English text.
     * @param positions A list of positons of the text in the original source
     * @return An iterator over the sentences predicted in the text.
     */
    public Iterator<PositionedText> split(final String text,
                                          final List<Integer> positions) {
        return new PositionedTextSplitterIterator(text, positions);
    }

    /**
     * Split the text and return an iterator over predicted sentences.
     * The iterator provides access to CharSequence instances. One for each
     * predicted sentence.
     * @param text English text.
     * @param positions A list of positons of the text in the original source
     * @return An iterator over the sentences predicted in the text.
     */
    public Iterator<PositionedText> split(final String text,
                                          final List<Integer> positions,
                                          final boolean punctuationSpacing) {
        return new PositionedTextSplitterIterator(text, positions, punctuationSpacing);
    }
}
