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
 * A simpler word reader for twease. This word reader split at any word position that is not a letter or digit.
 * Further, words that are longer than four characters  are always downcased (four characters is the default, change this value with the MAXIMUM_LENGTH_CONSERVE_CASE property). A class of character (defined by the
 * OTHER_CHARACTER_DELIMITERS property) are always returned as standalone words.
 * Assume OTHER_CHARACTER_DELIMITERS="\\,;", then the string "Alfred2 ; ... anotherword,blahblah" is split into
 * word="alfred2" nonWord=" "
 * word=";" nonWord=" ... "
 * word="anotherword" nonWord=""
 * word="," nonWord=""
 * word="blahblah" nonWord=""
 *
 * @author Fabien Campagne
 *         Date: Sept 26 2006
 *         Time: 11:45:06 AM
 */

public class TweaseWordReader2 extends FastBufferedReader
        implements TextractorWordReader {
    private static final long serialVersionUID = 1L;
    private int maximumCaseLength = 4;
    private String specialCharacterDelimiters;
    private final String PARENTHESES_CHARACTERS = "()";

    /**
     * Terms shorter than value are never downcased. Longer terms always are.
     * @param value maximumCaseLength
     */
    public void setMaximumTermLengthKeepCase(final int value) {
        this.maximumCaseLength = value;
    }

    public void configure(final Properties properties) {
        final boolean parenthesesAreWords =
                properties.getBoolean(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS, false);
        maximumCaseLength = properties.getInt(AbstractTextractorDocumentFactory.MetadataKeys.MAXIMUM_LENGTH_CONSERVE_CASE, 4);
        final Object otherCharacterDelimitersPropertyValue = properties.getProperty(AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS);
        String otherCharacterDelimiters = "\\,;/$.:!?|"; // default match Twease behaviour.
        assert otherCharacterDelimitersPropertyValue == null || otherCharacterDelimitersPropertyValue instanceof String: "OTHER_CHARACTER_DELIMITERS property value must have type string.";

        if (otherCharacterDelimitersPropertyValue != null) {
            otherCharacterDelimiters = (String) otherCharacterDelimitersPropertyValue;
        }
        final String parenthesisDelimiters = (parenthesesAreWords ? PARENTHESES_CHARACTERS : "");
        configure(parenthesesAreWords, otherCharacterDelimiters + parenthesisDelimiters);
    }

    public void saveProperties(final Properties properties) {
        properties.addProperty(PropertyBasedDocumentFactory.MetadataKeys.WORDREADER,
                this.getClass().getName());
        properties.setProperty(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                Boolean.toString(isParenthesesAreWords()));
        properties.setProperty(AbstractTextractorDocumentFactory.MetadataKeys.MAXIMUM_LENGTH_CONSERVE_CASE,
                Integer.toString(maximumCaseLength));
        properties.setProperty(AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS,
                getOtherCharacterDelimiters());
    }

    public void configureFromCommandLine(final String[] args) {
        final boolean parenthesesAreWords =
                CLI.isKeywordGiven(args, INDEX_PARENTHESES_ARGUMENT);
        final String otherCharacterDelimiters =
                CLI.getOption(args, "-other-character-delimiters", getOtherCharacterDelimiters());
        configure(parenthesesAreWords, otherCharacterDelimiters);
    }


    /**
     * Obtain the set of characters that this reader will treat as special words.
     *
     * @return A string made with the concatenation of the special characters.
     */
    public String getSpecialCharacterDelimiters() {
        return specialCharacterDelimiters;
    }

    public String getOtherCharacterDelimiters() {
        if (specialCharacterDelimiters == null) {
            return "";
        } else {
            return specialCharacterDelimiters.replace(PARENTHESES_CHARACTERS, "");
        }
    }

    private void configure(final boolean parenthesesAreWords,
                           final String otherCharacterDelimiters) {
        specialCharacterDelimiters = otherCharacterDelimiters + (parenthesesAreWords ? PARENTHESES_CHARACTERS : "");
    }


    /**
     * Creates a new fast buffered reder by wrapping a given reder with a given
     * buffer size.
     *
     * @param r       a reader to wrap.
     * @param bufSize the size in bytes of the internal buffer.
     */
    public TweaseWordReader2(final Reader r, final int bufSize) {
        super(r, bufSize);
    }

    public TweaseWordReader2() {
        super();
    }

    public TweaseWordReader2(final Properties properties) {
        super();
        configure(properties);
    }

    public TweaseWordReader2(final boolean parenthesesAreWords) {
        super();
        configure(parenthesesAreWords, "");
    }

    public TweaseWordReader2(final boolean parenthesesAreWords, final String otherCharacterDelimiters) {
        super();

        configure(parenthesesAreWords, otherCharacterDelimiters);
    }

    /**
     * Creates a new ProteinWordSpliterReader by wrapping a given reader with a
     * buffer of {@link #DEFAULT_BUFFER_SIZE} characters.
     *
     * @param r a reader to wrap.
     */
    public TweaseWordReader2(final Reader r) {
        super(r);
    }


    public static boolean isStandardWordCharacter(final char c) {
        return Character.isLetterOrDigit(c);
    }

    /**
     * These characters, if encountered outside a word, will be considered as a
     * word.
     *
     * @param c The character to test.
     * @return True if c should be considered as a word by itself.
     */
    public final boolean isSpecialWordCharacter(final char c) {
        return this.specialCharacterDelimiters.indexOf(c) != -1;
    }

    public final boolean isDelimiter(final char c) {
        return !isSpecialWordCharacter(c) && !isStandardWordCharacter(c);
    }

    /**
     * Check if the character can be part of a word. Special word characters return false because they
     * can only exist as standalone words.
     *
     * @param c Character to test.
     * @return True is the character is a word delimited. False otherwise.
     */
    public final boolean characterIsNonDelimiter(final char c) {
        return isStandardWordCharacter(c);

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
            collectNonWord(nonWord);
            downcaseWord(word);
            return true;

        } else {

            for (i = 0; i < avail && isStandardWordCharacter(buffer[pos + i]); i++) {
                // go on until end of allowed character span is found.
            }

            word.append(buffer, pos, i);  // word is made of the characters in the span

            pos += i;
            avail -= i;

            collectNonWord(nonWord);
            downcaseWord(word);
            return true;

        }
    }

    private void collectNonWord(final MutableString nonWord) throws IOException {
        if (!noMoreCharacters() && !isSpecialWordCharacter(buffer[pos])) {
            int i;
            for (i = 0; i < avail && isDelimiter(buffer[pos + i]); i++) {
                // continue until a character that is not a delimiter is found.
                // That span is the nonWord part.
            }

            nonWord.append(buffer, pos, i);
            this.pos += i;
            this.avail -= i;
        }
    }

    private void downcaseWord(final MutableString word) {
        if (word.length() > maximumCaseLength) {
            word.toLowerCase();
        }
    }

    @Override
    public final FastBufferedReader copy() {
        final TweaseWordReader2 copy =
                new TweaseWordReader2(reader, bufferSize);
        copy.setParenthesesAreWords(isParenthesesAreWords());
        return copy;
    }

    public final void setParenthesesAreWords(final boolean parenthesesAreWords) {
        configure(parenthesesAreWords, specialCharacterDelimiters.replace(PARENTHESES_CHARACTERS, ""));
    }

    /**
     * @return Returns the parenthesesAreWords.
     */
    public final boolean isParenthesesAreWords() {
        return specialCharacterDelimiters.contains(PARENTHESES_CHARACTERS);
    }
}
