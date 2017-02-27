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

package textractor.didyoumean;

import it.unimi.dsi.mg4j.document.Document;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.util.MutableString;

import java.io.CharArrayReader;
import java.io.IOException;

/**
 * User: campagne
 * Date: Nov 8, 2005
 * Time: 3:23:18 PM
 */
public final class DidYouMeanDocument implements Document {
    private final MutableString word;
    private final int wordLength;

    private static final FastBufferedReader wordReader = new FastBufferedReader();
    static final MutableString result = new MutableString();

    public DidYouMeanDocument(final CharSequence word) {
        if (word instanceof MutableString) {
            this.word = (MutableString) word;
        } else {
            this.word = new MutableString(word);
        }
        wordLength = word.length();
    }


    public CharSequence title() {
        return word;
    }

    public CharSequence uri() {
        return null;
    }

    public Object content(final int field) throws IOException {
        return new CharArrayReader(getContent(field).toCharArray());
    }

    public synchronized MutableString getContent(final int field) {
        switch (field) {
            case DidYouMeanDocumentFactory.WORD:
                return word;
            case DidYouMeanDocumentFactory.GRAM3:
                return threeGrams(word);
            case DidYouMeanDocumentFactory.GRAM4:
                return fourGrams(word);
            case DidYouMeanDocumentFactory.THREE_START:
                return threeStart(word);
            case DidYouMeanDocumentFactory.THREE_END:
                return threeEnd(word);
            case DidYouMeanDocumentFactory.FOUR_END:
                return fourEnd(word);
            case DidYouMeanDocumentFactory.FOUR_START:
                return fourStart(word);
            case DidYouMeanDocumentFactory.GRAM2:
                return twoGrams(word);
            default:
                throw new IndexOutOfBoundsException(Integer.toString(field));
        }
    }

    private MutableString fourStart(final MutableString word) {
        result.setLength(0);
        ngram(result, word, 4, 0);
        return result;
    }

    private MutableString threeEnd(final MutableString word) {
        result.setLength(0);
        ngram(result, word, 3, word.length() - 3);
        return result;
    }

    private MutableString fourEnd(final MutableString word) {
        result.setLength(0);
        ngram(result, word, 4, word.length() - 4);
        return result;
    }

    private MutableString threeStart(final MutableString word) {
        result.setLength(0);
        ngram(result, word, 3, 0);
        return result;
    }

    private MutableString twoGrams(final MutableString word) {
        return nGrams(word, 2);
    }

    private MutableString threeGrams(final MutableString word) {
        return nGrams(word, 3);
    }

    private MutableString fourGrams(final MutableString word) {
        return nGrams(word, 4);
    }


    public WordReader wordReader(final int field) {
        return wordReader.setReader(new CharArrayReader(contentCharArray(field)));
    }

    private char[] contentCharArray(final int field) {
        return getContent(field).toCharArray();
    }

    public void close() throws IOException {
    }

    private synchronized MutableString nGrams(final MutableString word, final int length) {
        result.setLength(0);

        for (int i = 0; i < wordLength; ++i) {
            ngram(result, word, length, i);
        }

        return result;
    }

    private synchronized void ngram(final MutableString result,
                                    final MutableString word,
                                    final int length, final int position) {
        if (length <= wordLength) {
            final int beginIndex = Math.max(0, position);
            final int endIndex = Math.min(position + length, wordLength);
            if (endIndex >= beginIndex && endIndex - beginIndex >= length) {
                result.append(word.subSequence(beginIndex, endIndex));
                result.append(' ');
            }
        }
    }
}
