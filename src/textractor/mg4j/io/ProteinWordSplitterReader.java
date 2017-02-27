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

package textractor.mg4j.io;

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.Properties;
import textractor.mg4j.document.AbstractTextractorDocumentFactory;

import java.io.IOException;
import java.io.Reader;

/**
 * User: Fabien Campagne
 * Date: Oct 25, 2004
 * Time: 9:03:54 PM
 */
public class ProteinWordSplitterReader extends FastBufferedReader
        implements TextractorWordReader {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 1L;
    final int nextWord = -1;
    private boolean parenthesesAreWords;

    public void configure(final Properties properties) {
        parenthesesAreWords =
                properties.getBoolean(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS, false);
    }

    public void saveProperties(final Properties properties) {
        properties.setProperty(PropertyBasedDocumentFactory.MetadataKeys.WORDREADER,
                this.getClass().getName());
        properties.setProperty(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                Boolean.toString(isParenthesesAreWords()));

    }

    public void configureFromCommandLine(final String[] args) {
        parenthesesAreWords = CLI.isKeywordGiven(args, INDEX_PARENTHESES_ARGUMENT);
    }


    /**
     * Creates a new fast buffered reder by wrapping a given reder with a given
     * buffer size.
     *
     * @param r       a reader to wrap.
     * @param bufSize the size in bytes of the internal buffer.
     */
    public ProteinWordSplitterReader(final Reader r, final int bufSize) {
        super(r, bufSize);
    }

    public ProteinWordSplitterReader() {
        super();
    }

    public ProteinWordSplitterReader(final Properties properties) {
        super();
        configure(properties);
    }

    public ProteinWordSplitterReader(final boolean parenthesesAreWords) {
        super();
        this.parenthesesAreWords = parenthesesAreWords;
    }

    /**
     * Creates a new ProteinWordSpliterReader by wrapping a given reader with a
     * buffer of {@link #DEFAULT_BUFFER_SIZE} characters.
     *
     * @param r a reader to wrap.
     */
    public ProteinWordSplitterReader(final Reader r) {
        super(r);
    }

    final boolean parenthesisCharacter(final char c) {
        return (c == '(' ||
                c == ')');
    }

    public static boolean isTheCharacterAllowed(final char c) {
        return Character.isLetterOrDigit(c) ||    // those characters can be part of words:
                c == '-'
                || c == '.'
                || c == ':';
    }

    /**
     * These characters, if encountered outside a word, will be considered as a
     * word.
     *
     * @param c The character to test.
     * @return True if c should be considered as a word by itself.
     */
    public final boolean isSpecialWordCharacter(final char c) {
        return c == '-'
                || c == '.'
                || c == ','
                || c == ':'
                || (parenthesesAreWords && parenthesisCharacter(c));
    }

    public final boolean isDelimiter(final char c) {
        return !isSpecialWordCharacter(c) && !isTheCharacterAllowed(c);
    }

    /**
     * Check if the character is a word delimiter. Consider if parentheses
     * should be considered delimiters or part of words, and check the character
     * accordingly.
     *
     * @param c Character to test.
     * @return True is the character is a word delimited. False otherwise.
     */
    public final boolean characterIsNonDelimiter(final char c) {
        return (parenthesesAreWords ?
                (isTheCharacterAllowed(c) || parenthesisCharacter(c)) :
                isTheCharacterAllowed(c));
    }

    /**
     * Check if the character is a word delimiter. Consider if parentheses
     * should be considered delimiters or part of words, and check the character
     * accordingly.
     *
     * @param c Character to test.
     * @return True is the character is a word delimited. False otherwise.
     */
    public final boolean characterIsDelimiter(final char c) {
        return (parenthesesAreWords ?
                (isTheCharacterAllowed(c) || parenthesisCharacter(c)) :
                isTheCharacterAllowed(c));
    }

    /**
     * Extracts the next word and non-word.
     * <p/>
     * <p>If this method returns true, a new non-empty word, and possibly
     * a new non-word, have been extracted. It is acceptable
     * that the <em>first</em> call to this method after creation
     * or after a call to {@link #setReader(Reader)} returns an empty
     * word. In other words <em>both <code>word</code> and <code>nonWord</code> are maximal</em>.
     *
     * @param word    the next word returned by the underlying reader.
     * @param nonWord the nonword following the next word returned by the underlying reader.
     * @return true if a new word was processed, false otherwise (in which
     *         case both <code>word</code> and <code>nonWord</code> are unchanged).
     */
    @Override
    public boolean next(final MutableString word, final MutableString nonWord) throws IOException {
        int i;

        if (noMoreCharacters()) {
            return false;
        }

        word.length(0);
        nonWord.length(0);
        final char firstCharacter = buffer[pos];
        if (avail >= 1 && isSpecialWordCharacter(firstCharacter)) {
            word.setLength(1);    // word is the special character that is a word by itself.
            word.setCharAt(0, firstCharacter);
            pos += 1;
            avail -= 1;
            // collect all delimiter characters that are not special:
            if (noMoreCharacters()) {
                return true;
            }
            for (; ;) {
                for (i = 0; i < avail && ! characterIsNonDelimiter(buffer[pos + i]); i++) {
                    // just count
                }

                nonWord.append(buffer, pos, i);
                pos += i;
                avail -= i;

                if (avail > 0 || noMoreCharacters()) {
                    return true;
                }
            }
        }

        for (; ;) {
            for (i = 0; i < avail && isTheCharacterAllowed(buffer[pos + i]); i++) {
                 // just count
            }

            word.append(buffer, pos, i);
            pos += i;
            avail -= i;

            if (avail > 0 || noMoreCharacters()) {
                break;
            }
        }

        if (noMoreCharacters()) {
            return true;
        }

        for (; ;) {
            for (i = 0; i < avail && isDelimiter(buffer[pos + i]); i++) {
                // just count
            }

            nonWord.append(buffer, pos, i);
            pos += i;
            avail -= i;

            if (avail > 0 || noMoreCharacters()) {
                return true;
            }
        }
    }

    @Override
    public final FastBufferedReader copy() {
        final ProteinWordSplitterReader copy =
                new ProteinWordSplitterReader(reader, bufferSize);
        copy.setParenthesesAreWords(this.parenthesesAreWords);
        return copy;
    }

    public final void setParenthesesAreWords(final boolean parenthesesAreWords) {
        this.parenthesesAreWords = parenthesesAreWords;
    }

    /**
     * @return Returns the parenthesesAreWords.
     */
    public final boolean isParenthesesAreWords() {
        return parenthesesAreWords;
    }
}
