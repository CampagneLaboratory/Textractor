/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

import edu.cornell.med.icb.util.ICBStringNormalizer;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.Properties;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Queue;

/**
 * PubMedExtractor creates a specifically formatted "authorsIndexText" string. This will
 * read that format. When reading words, this word reader will lowercase all words
 * and remove accents from letters including the special cases of converting "Ø" and "ø" to "o".
 * and "?" to "l".
 * @author Kevin Dorff
 */
public class AuthorsWordReader extends FastBufferedReader implements TextractorWordReader {

    private static final Queue<String> futureWords = new LinkedList<String>();

    private static final CharSet SPECIAL_WORD_CHARS = new CharOpenHashSet();
    static {
        SPECIAL_WORD_CHARS.add(',');
        SPECIAL_WORD_CHARS.add(':');
        SPECIAL_WORD_CHARS.add('|');
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
     * IMPORT NOTE! In the case where removing accents introduces a space, the nonWord may be
     * inaccurate (just forced to ' '). As author names are stripped of accents and the like,
     * it isn't currently possible to keep accurate positions of author names.
     *
     * @param word    the next word returned by the underlying reader.
     * @param nonWord the nonword following the next word returned by the underlying reader.
     * @return true if a new word was processed, false otherwise (in which
     *         case both <code>word</code> and <code>nonWord</code> are unchanged).
     */
    @Override
    public boolean next(final MutableString word, final MutableString nonWord) throws IOException {
        int i;

        if (futureWords.size() == 0 && noMoreCharacters()) {
            return false;
        }

        word.length(0);
        nonWord.length(0);
        if (futureWords.size() != 0) {
            word.append(futureWords.poll());
            nonWord.append(" ");
            return true;
        }

        final char firstCharacter = buffer[pos];

        if (avail >= 1 && isSpecialWordCharacter(firstCharacter)) {
            word.setLength(1);    // word is the special character that is a word by itself.
            word.setCharAt(0, firstCharacter);

            pos += 1;
            avail -= 1;
            collectNonWord(nonWord);
            fixword(word);
            return true;

        } else {

            for (i = 0; i < avail && isStandardWordCharacter(buffer[pos + i]); i++) {
                // go on until end of allowed character span is found.
            }

            word.append(buffer, pos, i);  // word is made of the characters in the span

            pos += i;
            avail -= i;

            collectNonWord(nonWord);
            fixword(word);
            return true;

        }
    }

    public static boolean isStandardWordCharacter(final char c) {
        return !SPECIAL_WORD_CHARS.contains(c) && c != ' ';
    }

    private boolean isSpecialWordCharacter(final char c) {
        return SPECIAL_WORD_CHARS.contains(c);
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

    /**
     * Strip accents, lowercase the word. Remove spaces. At this point, we are dealing
     * with a single word that should contain no spaces. Sometimes removal of accents
     * introduces spaces that shouldn't be there. ??
     * @param word the word without accents, in lower case.
     */
    private void fixword(final MutableString word) {
        final String noAccents = ICBStringNormalizer.removeAccents(word.toString());
        word.setLength(0);
        word.append(noAccents);
        word.toLowerCase();
        checkForFutureWords(word);
    }

    private void checkForFutureWords(final MutableString word) {
        if (word.indexOf(" ") == -1) {
            return;
        }
        final String[] parts = StringUtils.split(word.toString(), ' ');
        int curWord = 0;
        for (final String part : parts) {
            if (part.length() > 0) {
                if (curWord++ == 0) {
                    word.length(0);
                    word.append(part);
                } else {
                    futureWords.add(part);
                }
            }
        }
    }

    public final boolean isDelimiter(final char c) {
        return !isSpecialWordCharacter(c) && !isStandardWordCharacter(c);
    }

    public void configureFromCommandLine(final String[] args) {
    }

    public void configure(final Properties properties) {
    }

    public void saveProperties(final Properties properties) {
    }

}
