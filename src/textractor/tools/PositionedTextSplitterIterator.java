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
import textractor.mg4j.io.ProteinWordSplitterReader;
import textractor.util.TextractorUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A splitter that deals with text that has positional information.
 * This is basically a direct copy of
 * {@link textractor.tools.SentenceSplitterIterator} which should be
 * probably removed in favor of this class.
 */
final class PositionedTextSplitterIterator implements Iterator<PositionedText> {
    private static final int BEFORE_FIRST_SENTENCE = 0;
    private int currentPosition = BEFORE_FIRST_SENTENCE;
    private boolean spacePunctuation;

    /**
     * The original text string.
     */
    private final String text;

    /**
     * The length of the original text string.
     */
    private final int textLength;

    /**
     * Raw byte positions of the text relative to the original source.
     */
    private final List<Integer> positions;
    private int previousPosition;

    char cip0 = ' ';
    char cip1 = ' '; // character at i +1 of cc
    char cip2 = ' '; // character at i +2 of cc
    char cip3 = ' '; // character at i +3 of cc
    char cip4 = ' '; // character at i +4 of cc
    char cip5 = ' '; // character at i +5 of cc
    char cip6 = ' '; // character at i +6 of cc

    /**
     * The current result string for the next iteration.
     */
    private final MutableString result = new MutableString(500);

    /**
     * The position list for the next iteration.
     */
    private final List<Integer> resultPositions = new ArrayList<Integer>(500);

    public PositionedTextSplitterIterator(final String text,
                                          final List<Integer> positions,
                                          final boolean punctuationSpacers) {
        this(text, positions);
        spacePunctuation = punctuationSpacers;
    }

    public PositionedTextSplitterIterator(final String text,
                                          final List<Integer> positions) {
        super();
        // Note: the SentenceSplitterIterator "squished" whitespace
        // here we assume that the text passed in has already been formatted
        this.text = text;
        this.positions = positions;
        spacePunctuation = true;
        this.textLength = this.text.length();
        if (textLength > 3) {
            appendToResult(this.text.charAt(0), this.positions.get(0));
            appendToResult(this.text.charAt(1), this.positions.get(1));
            appendToResult(this.text.charAt(2), this.positions.get(2));
        }
    }

    private void setCurrentPosition(final int position) {
        this.currentPosition = position;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext() {
        return currentPosition < textLength;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     */
    public PositionedText next() {
        if (textLength <= 0) {
            throw new NoSuchElementException();
        }

        for (int i = currentPosition; i < textLength - 6; ++i) {
            cip0 = text.charAt(i);
            cip1 = text.charAt(i + 1);
            cip2 = text.charAt(i + 2);
            cip3 = text.charAt(i + 3);
            cip4 = text.charAt(i + 4);
            cip5 = text.charAt(i + 5);
            cip6 = text.charAt(i + 6);

            if (isSpacePunctuation()) {
                if (TextractorUtils.characterIsPunctuation(cip3) &&
                        cip4 == ' ') {
                    // add an invalid position since this is not in the source
                    appendToResult(' ', previousPosition);
                }
            }

            appendToResult(cip3, positions.get(i + 3));

            if (TextractorUtils.characterIsSentenceTerminator(cip3)) {
                if (Character.isDigit(cip4)) {
                    continue; //exclude the real numbers (with dot) such as 0.45
                }
                final int punctuationCount = countPunctuation(cip0, cip1, cip2, cip3, cip4, cip5, cip6);
                final int spaceCount = countSpaces(cip0, cip1, cip2, cip3, cip4, cip5, cip6);
                if (
                        result.length() >= 40 &&
                                !(cip0 == 'F' && cip1 == 'i' && cip2 == 'g' ) && //excluding "Fig. " situation
                                !(ProteinWordSplitterReader.isTheCharacterAllowed(cip4)) && //excluding ".X" situation
                                (
                                        (punctuationCount == 1 && spaceCount <= 1)
                                                || (cip1 == ')' && Character.isSpaceChar(cip2) && Character.isSpaceChar(cip4)) //to split sentence at ") . " situation.
                                                || (cip2 == ')' && Character.isSpaceChar(cip1) && Character.isSpaceChar(cip4)) //to split sentence at " ). " situation.
                                                || (Character.isSpaceChar(cip4) && Character.isSpaceChar(cip6) && cip5 == 'A') //to split sentence at ". A " situation
                                                || (Character.isSpaceChar(cip0) && Character.isSpaceChar(cip4) && cip1 == 'p' && cip2 == 'H')//to split sentence at " pH. " situation
                                )
                        ) {
                    // split sentence at cip3 if only one punctuation mark in the window of 5 chars, and
                    // if the previous sentence is at least 40 characters long..
                    return nextSentenceText(i, result, resultPositions);
                }
            }
        }

        int index = textLength - 3;
        int position;
        if (index < 0 || index > positions.size()) {
            position = previousPosition;
        } else {
            position = positions.get(index);
        }
        appendToResult(cip4, position);

        index++;  // textLength - 1
        if (index < 0 || index > positions.size()) {
            position = previousPosition;
        } else {
            position = positions.get(index);
        }
        appendToResult(cip5, position);

        index++;  // textLength - 1
        if (index < 0 || index > positions.size()) {
            position = previousPosition;
        } else {
            position = positions.get(index);
        }
        appendToResult(cip6, position); // the last one should be a sentence terminator
        return nextSentenceText(textLength, result, resultPositions);
    }

    private int countSpaces(final char cip0, final char cip1, final char cip2,
                            final char cip3, final char cip4, final char cip5,
                            final char cip6) {
        int count = 0;
        if (Character.isSpaceChar(cip0)) {
            count++;
        }
        if (Character.isSpaceChar(cip1)) {
            count++;
        }
        if (Character.isSpaceChar(cip2)) {
            count++;
        }
        if (Character.isSpaceChar(cip3)) {
            count++;
        }
        if (Character.isSpaceChar(cip4)) {
            count++;
        }
        if (Character.isSpaceChar(cip5)) {
            count++;
        }
        if (Character.isSpaceChar(cip6)) {
            count++;
        }
        return count;
    }

    private int countPunctuation(final char cip0, final char cip1, final char cip2, final char cip3,
                                 final char cip4, final char cip5, final char cip6) {
        int count = 0;
        if (characterIsSentenceTerminator(cip0)) {
            count++;
        }
        if (characterIsSentenceTerminator(cip1)) {
            count++;
        }
        if (characterIsSentenceTerminator(cip2)) {
            count++;
        }
        if (characterIsSentenceTerminator(cip3)) {
            count++;
        }
        if (characterIsSentenceTerminator(cip4)) {
            count++;
        }
        if (characterIsSentenceTerminator(cip5)) {
            count++;
        }
        if (characterIsSentenceTerminator(cip6)) {
            count++;
        }
        return count;

    }

    private boolean characterIsSentenceTerminator(final char cip6) {
        return TextractorUtils.characterIsSentenceTerminator(cip6);
    }

    /**
     * This method is not implemented for this iterator. Do not use.
     */
    public void remove() {
        throw new UnsupportedOperationException("Not implemented");
    }

    private PositionedText nextSentenceText(final int position,
                                            final MutableString result,
                                            final List<Integer> resultPositions) {
        setCurrentPosition(position + 1);

        // trim the string and resulting positions
        // like MutableString, whitespace is any character smaller than
        // '\u0020' (the ASCII space).
        int start = 0;                     // first character in the result
        int end = result.length() - 1;     // last character in the result

        // trim from the front
        while (start < end && result.charAt(start) <= ' ') {
            start++;
        }

        // and trim from the back
        while (end > start && result.charAt(end) <= ' ') {
            end--;
        }

        // make copies of the results
        final MutableString txt = result.substring(start, end + 1);
        final List<Integer> pos = new ArrayList<Integer>(txt.length());
        pos.addAll(resultPositions.subList(start, end + 1));

        // and clear the local versions for the next iteration
        result.setLength(0);
        resultPositions.clear();
        previousPosition = 0;

        return new PositionedText(txt, pos);
    }

    public boolean isSpacePunctuation() {
        return spacePunctuation;
    }

    public void setSpacePunctuation(final boolean spacePunctuation) {
        this.spacePunctuation = spacePunctuation;
    }

    /**
     * Append a character and position to the results.
     * @param c The character to append
     * @param position The position of the character being appended
     */
    private void appendToResult(final char c, final int position) {
        result.append(c);
        resultPositions.add(position);
        previousPosition = position;
    }
}
