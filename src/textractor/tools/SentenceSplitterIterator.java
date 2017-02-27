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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * User: Fabien Campagne
 * Date: Oct 17, 2004
 * Time: 6:51:55 PM
 */
final class SentenceSplitterIterator implements Iterator<MutableString> {
    private static final int BEFORE_FIRST_SENTENCE = 0;
    private int currentPosition = BEFORE_FIRST_SENTENCE;
    private boolean spacePunctuation;
    private final int textLength;
    private String localtext;

    char cip0 = ' ';
    char cip1 = ' '; // character at i +1 of cc
    char cip2 = ' '; // character at i +2 of cc
    char cip3 = ' '; // character at i +3 of cc
    char cip4 = ' '; // character at i +4 of cc
    char cip5 = ' '; // character at i +5 of cc
    char cip6 = ' '; // character at i +6 of cc
    final MutableString result = new MutableString(500);

    public SentenceSplitterIterator(final String text, final boolean punctuationSpacers) {
        this(text);
        spacePunctuation = punctuationSpacers;
    }

    public SentenceSplitterIterator(final String text) {
        localtext = text.replaceAll("[\t\n\r]", " ");  // replace all the \r \n with " "
        localtext = localtext.replaceAll("([^\\s])\\s{2,}([^\\s])", "$1 $2");
        spacePunctuation = true;
        this.textLength = localtext.length();
        if (textLength > 3) {
            result.append(localtext.charAt(0));
            result.append(localtext.charAt(1));
            result.append(localtext.charAt(2));
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
     * @throws NoSuchElementException iteration has no more elements.
     */
    public MutableString next() {
        if (localtext.length() <= 0) {
            throw new NoSuchElementException();
        }

        final int length = localtext.length();

        for (int i = currentPosition; i < length - 6; ++i) {
            cip0 = localtext.charAt(i);
            cip1 = localtext.charAt(i + 1);
            cip2 = localtext.charAt(i + 2);
            cip3 = localtext.charAt(i + 3);
            cip4 = localtext.charAt(i + 4);
            cip5 = localtext.charAt(i + 5);
            cip6 = localtext.charAt(i + 6);

            if (isSpacePunctuation()) {
                if (TextractorUtils.characterIsPunctuation(cip3) &&
                        cip4 == ' ') {
                    result.append(' ');
                }
            }

            result.append(cip3);

            if (TextractorUtils.characterIsSentenceTerminator(cip3)) {
                if (Character.isDigit(cip4)) {
                    continue; //exclude the real numbers (with dot) such as 0.45
                }
                final int punctuationCount = countPunctuation(cip0, cip1, cip2, cip3, cip4, cip5, cip6);
                final int spaceCount = countSpaces(cip0, cip1, cip2, cip3, cip4, cip5, cip6);
                if (
                        result.length() >= 40 &&
                                !(cip0 == 'F' && cip1 == 'i' && cip2 == 'g' ) && //excluding "Fig. " situation
                                !(ProteinWordSplitterReader.isTheCharacterAllowed(cip4)) && //excluding ".X" siuation
                                (
                                        (punctuationCount == 1 && spaceCount <= 1)
                                                || (cip1 == ')' && Character.isSpaceChar(cip2) && Character.isSpaceChar(cip4)) //to split sentence at ") . " situation.
                                                || (cip2 == ')' && Character.isSpaceChar(cip1) && Character.isSpaceChar(cip4)) //to split sentence at " ). " situation.
                                                || (Character.isSpaceChar(cip4) && Character.isSpaceChar(cip6) && cip5 == 'A') //to split sentence at ". A " situation
                                                || (Character.isSpaceChar(cip0) && Character.isSpaceChar(cip4) && cip1 == 'p' && cip2 == 'H')//to split sentence at " pH. " situation
                                )
                        ) {
                    //split sentence at cip3 if only one punctuation mark in the window of 5 chars, and
                    // if the previous sentence is at least 40 characters long..
                    return nextSentenceText(i, result);

                }

            }
        }

        result.append(cip4);
        result.append(cip5);
        result.append(cip6); //the last one should be a sentence terminator
        return nextSentenceText(textLength, result);
    }

    private int countSpaces(final char cip0, final char cip1, final char cip2,
                            final char cip3, final char cip4, final char cip5, final char cip6) {
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
        throw new InternalError("Not implemented");
    }

    private MutableString nextSentenceText(final int position,
                                           final MutableString result) {
        setCurrentPosition(position + 1);
        final MutableString tmp = result.trim().copy();
        result.setLength(0);
        return tmp;
    }

    public boolean isSpacePunctuation() {
        return spacePunctuation;
    }

    public void setSpacePunctuation(final boolean spacePunctuation) {
        this.spacePunctuation = spacePunctuation;
    }
}
