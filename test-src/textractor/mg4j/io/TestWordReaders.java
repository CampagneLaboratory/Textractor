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

import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.Properties;
import junit.framework.TestCase;
import textractor.mg4j.document.AbstractTextractorDocumentFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * @author campagne
 *         Date: Dec 15, 2005
 *         Time: 3:14:20 PM
 */
public final class TestWordReaders extends TestCase {


    public void testProteinWordSplitterReader() throws IOException {
        final MutableString nonword = new MutableString();
        // now, test without parentheses:
        StringReader sr =
                new StringReader("A (string))   with (parentheses) and.a to-ken");
        ProteinWordSplitterReader reader = new ProteinWordSplitterReader(sr);
        reader.setParenthesesAreWords(false);
        MutableString word = new MutableString();

        assertWithoutParentheses(reader, word, nonword);

        sr = new StringReader("A (string))   with (parentheses) and.a to-ken");
        reader = new ProteinWordSplitterReader(sr);
        reader.setParenthesesAreWords(true);

        assertWithParentheses(reader, word, nonword);

        String text = " (i) In the double mutants R82A/G72C and R82A/A160C";
        sr = new StringReader(text);
        reader = new ProteinWordSplitterReader(sr);
        reader.setParenthesesAreWords(false);
        word = new MutableString();

        assertTrue(reader.next(word, nonword));
        assertEquals("", word.toString());
        assertEquals(" (", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("i", word.toString());
        assertEquals(") ", nonword.toString());

        // With parenthesis as very first character, and characters not word:
        text = "(i) In";
        sr = new StringReader(text);
        reader = new ProteinWordSplitterReader(sr);
        reader.setParenthesesAreWords(false);
        word = new MutableString();

        assertTrue(reader.next(word, nonword));
        assertEquals("", word.toString());
        // This should really return " (", but the missing space should not have
        // a huge impact.
        assertEquals("(", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("i", word.toString());
        assertEquals(") ", nonword.toString());

        // test that special characters . , and others are reported as
        // words when they are not just close to a word:
        text = "Hello, this is the text to index, first document";
        sr = new StringReader(text);
        reader = new ProteinWordSplitterReader(sr);
        reader.setParenthesesAreWords(false);
        word = new MutableString();

        assertTrue(reader.next(word, nonword));
        assertEquals("Hello", word.toString());
        assertEquals("", nonword.toString());
        assertTrue(reader.next(word, nonword));
        assertEquals(",", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("this", word.toString());
        assertEquals(" ", nonword.toString());
    }


    /**
     * Check that TweaseReader works as ProteinWordSplitterReader for a subset
     * of strings.
     * @throws IOException error reading word
     */
    public void testTweaseWordReaderCompatibility() throws IOException {
        final Properties properties = new Properties();
        properties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                true);

        final StringReader sr =
                new StringReader("A (string))   with (parentheses) and.a to-ken");
        final TweaseWordReader reader = new TweaseWordReader(sr);
        reader.configure(properties);
        assertTrue("parentheses should be treated as words",
                reader.isParenthesesAreWords());
        MutableString word = new MutableString();
        MutableString nonword = new MutableString();

        assertWithParentheses(reader, word, nonword);

        properties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                false);
        reader.configure(properties);
        assertFalse("parentheses should not be treated as words",
                reader.isParenthesesAreWords());
        reader.setReader(new StringReader("A (string))   with (parentheses) and.a to-ken"));
        word = new MutableString();
        nonword = new MutableString();
        assertWithoutParentheses(reader, word, nonword);
    }

    /**
     * Check that TweaseReader works as ProteinWordSplitterReader for a subset
     * of strings.
     * @throws IOException error reading word
     */
    public void testTweaseWordReaderNewBehaviour() throws IOException {
        TweaseWordReader reader;
        final Properties properties = new Properties();
        properties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                true);

        final StringReader sr = new StringReader(
                "A (string-string))   with (parentheses) and.a to-ken");
        reader = new TweaseWordReader(sr);
        reader.configure(properties);
        MutableString word = new MutableString();
        MutableString nonword = new MutableString();

        assertWithParenthesesTwease(reader, word, nonword);

        properties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                false);
        reader.configure(properties);
        reader.setReader(new StringReader(
                "A (string-string))   with (parentheses) and.a to-ken"));
        word = new MutableString();
        nonword = new MutableString();
        assertWithoutParenthesesTwease(reader, word, nonword);

        reader.setReader(new StringReader(
                "string-string a-AAAA BBBBBBB- CCCC-CCCCCC- AS"));
        assertHarderTwease(reader, word, nonword);

        reader.setReader(new StringReader(
                "graft-vs.-host-disease"));
        assertTrue(reader.next(word, nonword));
        assertEquals("graft", word.toString());
        assertEquals("-", nonword.toString());
        assertTrue(reader.next(word, nonword));
        assertEquals("vs.", word.toString());
        assertEquals("-", nonword.toString());
        assertTrue(reader.next(word, nonword));
        assertEquals("host", word.toString());
        assertEquals("-", nonword.toString());
        assertTrue(reader.next(word, nonword));
        assertEquals("disease", word.toString());
        assertEquals("", nonword.toString());

    }

    /**
     * Check that TweaseReader works as ProteinWordSplitterReader for a subset
     * of strings.
     * @throws IOException error reading word
     */
    public void testTweaseWordReader2() throws IOException {
        TweaseWordReader2 reader;
        final Properties properties = new Properties();
        properties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                true);
        properties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS, "");
        final StringReader sr = new StringReader(
                "A (string-string))   with (parentheses) and.a to-ken");
        reader = new TweaseWordReader2(sr);
        reader.configure(properties);
        MutableString word = new MutableString();
        MutableString nonword = new MutableString();

        assertWithParenthesesTwease2(reader, word, nonword);

        properties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                false);
        properties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS, "");

        reader.configure(properties);
        reader.setReader(new StringReader(
                "A (string-string))   with (parentheses) and.a to-ken"));
        word = new MutableString();
        nonword = new MutableString();
        assertWithoutParenthesesTwease2(reader, word, nonword);

        reader.setReader(new StringReader(
                "string-string a-AAAA BBBBBBB- CCCC-CCCCCC- AS"));
        assertHarderTwease2(reader, word, nonword);

        reader.setReader(new StringReader(
                "graft-vs.-host-disease"));
        assertTrue(reader.next(word, nonword));
        assertEquals("graft", word.toString());
        assertEquals("-", nonword.toString());
        assertTrue(reader.next(word, nonword));
        assertEquals("vs", word.toString());
        assertEquals(".-", nonword.toString());
        assertTrue(reader.next(word, nonword));
        assertEquals("host", word.toString());
        assertEquals("-", nonword.toString());
        assertTrue(reader.next(word, nonword));
        assertEquals("disease", word.toString());
        assertEquals("", nonword.toString());

        properties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS, true);
        properties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS,
                ":\\,;/*+!?%"); // espace coma or the Properties logic will split into two strings.
        reader.configure(properties);

        reader.setReader(new StringReader(
                "word:w2d, jdskjd : 5t3s;923/232ldks+sd!rew?ww ? ?"));
        assertTrue(reader.next(word, nonword));
        assertEquals("word", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(":", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("w2d", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(",", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("jdskjd", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(":", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("5t3s", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(";", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("923", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("/", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("232ldks", word.toString());
        assertEquals("", nonword.toString());
        //     "word:w2d, jdskjd : 5t3s;923/232ldks+sd!rew?ww"));
        assertTrue(reader.next(word, nonword));
        assertEquals("+", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("sd", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("!", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("rew", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("?", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("ww", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("?", word.toString());
        assertEquals(" ", nonword.toString());


        assertTrue(reader.next(word, nonword));
        assertEquals("?", word.toString());
        assertEquals("", nonword.toString());
        assertFalse(reader.next(word, nonword));
    }

    private void assertHarderTwease(
            final it.unimi.dsi.mg4j.io.WordReader reader, final MutableString word,
            final MutableString nonword) throws IOException {
        // "string-string a-AAAA BBB- CC-CCC-"
        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("a-AAAA", word.toString()); // too short to be split
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("BBBBBBB", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("CCCC", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("CCCCCC", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("AS", word.toString());
        assertEquals("", nonword.toString());
    }

    private void assertHarderTwease2(
            final it.unimi.dsi.mg4j.io.WordReader reader, final MutableString word,
            final MutableString nonword) throws IOException {
        // "string-string a-AAAA BBB- CC-CCC-"
        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("a", word.toString()); // too short to be split
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("AAAA", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("bbbbbbb", word.toString());   // downcased because longer than 4 characters.
        assertEquals("- ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("CCCC", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("cccccc", word.toString());
        assertEquals("- ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("AS", word.toString());
        assertEquals("", nonword.toString());
    }

    private void assertWithParentheses(final WordReader reader,
                                       final MutableString word,
                                       final MutableString nonword)
            throws IOException {
        // "A (string))   with (parentheses) and.a to-ken"
        assertTrue(reader.next(word, nonword));
        assertEquals("A", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("(", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(")", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(")", word.toString());
        assertEquals("   ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("with", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("(", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("parentheses", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(")", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("and.a", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("to-ken", word.toString());
        assertEquals("", nonword.toString());

        assertFalse(reader.next(word, nonword));
    }

    private void assertWithParenthesesTwease(final WordReader reader,
                                             final MutableString word,
                                             final MutableString nonword)
            throws IOException {
        // "A (string))   with (parentheses) and.a to-ken"
        assertTrue(reader.next(word, nonword));
        assertEquals("A", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("(", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(")", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(")", word.toString());
        assertEquals("   ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("with", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("(", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("parentheses", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(")", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("and.a", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("to-ken", word.toString());
        assertEquals("", nonword.toString());

        assertFalse(reader.next(word, nonword));
    }

    private void assertWithoutParentheses(final WordReader reader,
                                          final MutableString word,
                                          final MutableString nonword)
            throws IOException {
        // "A (string))   with (parentheses) and.a to-ken"
        assertTrue(reader.next(word, nonword));
        assertEquals("A", word.toString());
        assertEquals(" (", nonword.toString());


        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("))   ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("with", word.toString());
        assertEquals(" (", nonword.toString());


        assertTrue(reader.next(word, nonword));
        assertEquals("parentheses", word.toString());
        assertEquals(") ", nonword.toString());


        assertTrue(reader.next(word, nonword));
        assertEquals("and.a", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("to-ken", word.toString());
        assertEquals("", nonword.toString());

        assertFalse(reader.next(word, nonword));
    }

    private void assertWithoutParenthesesTwease(final WordReader reader,
                                                final MutableString word,
                                                final MutableString nonword)
            throws IOException {
        // "A (string-string))   with (parentheses) and.a to-ken"
        assertTrue(reader.next(word, nonword));
        assertEquals("A", word.toString());
        assertEquals(" (", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("))   ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("with", word.toString());
        assertEquals(" (", nonword.toString());


        assertTrue(reader.next(word, nonword));
        assertEquals("parentheses", word.toString());
        assertEquals(") ", nonword.toString());


        assertTrue(reader.next(word, nonword));
        assertEquals("and.a", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("to-ken", word.toString());
        assertEquals("", nonword.toString());

        assertFalse(reader.next(word, nonword));
    }

    private void assertWithoutParenthesesTwease2(final WordReader reader,
                                                 final MutableString word,
                                                 final MutableString nonword)
            throws IOException {
        // "A (string-string))   with (parentheses) and.a to-ken"
        assertTrue(reader.next(word, nonword));
        assertEquals("A", word.toString());
        assertEquals(" (", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("))   ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("with", word.toString());
        assertEquals(" (", nonword.toString());


        assertTrue(reader.next(word, nonword));
        assertEquals("parentheses", word.toString());
        assertEquals(") ", nonword.toString());


        assertTrue(reader.next(word, nonword));
        assertEquals("and", word.toString());
        assertEquals(".", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("a", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("to", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("ken", word.toString());
        assertEquals("", nonword.toString());

        assertFalse(reader.next(word, nonword));
    }

    /**
     * Ensures that the configuring the protein word reader property object
     * sets the "ParenthesesAreWords" property correctly.
     */
    public void testConfigureParenthesesAreWordsWithProperties() {
        final ProteinWordSplitterReader wordReader = new ProteinWordSplitterReader();
        assertFalse(wordReader.isParenthesesAreWords());

        // send in a bad specification for indexing parentheses
        final Properties badProperties = new Properties();
        badProperties.setProperty("parenthesisAreWords", true);
        wordReader.configure(badProperties);
        assertFalse(wordReader.isParenthesesAreWords());

        // now make sure that the property gets set properly
        final Properties goodProperties = new Properties();
        goodProperties.setProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS, true);
        wordReader.configure(goodProperties);
        assertTrue(wordReader.isParenthesesAreWords());
    }

    private void assertWithParenthesesTwease2(final WordReader reader,
                                              final MutableString word,
                                              final MutableString nonword)
            throws IOException {
        // "A (string))   with (parentheses) and.a to-ken"
        assertTrue(reader.next(word, nonword));
        assertEquals("A", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("(", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("string", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(")", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(")", word.toString());
        assertEquals("   ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("with", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("(", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("parentheses", word.toString());
        assertEquals("", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals(")", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("and", word.toString());
        assertEquals(".", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("a", word.toString());
        assertEquals(" ", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("to", word.toString());
        assertEquals("-", nonword.toString());

        assertTrue(reader.next(word, nonword));
        assertEquals("ken", word.toString());
        assertEquals("", nonword.toString());

        assertFalse(reader.next(word, nonword));
    }

    public void testBioSequenceWordReader() throws IOException {
        BioSequenceWordReader reader = new BioSequenceWordReader();
        reader.setnGramLength(3);
        reader.setReader(new StringReader("abraca"));
        MutableString word = new MutableString();
        MutableString nonWord = new MutableString();
        assertTrue(reader.next(word, nonWord));
        assertEquals("abr", word.toString());
        assertEquals(0, nonWord.length());
        assertTrue(reader.next(word, nonWord));
        assertEquals("bra", word.toString());
        assertEquals(0, nonWord.length());
        assertTrue(reader.next(word, nonWord));
        assertEquals("rac", word.toString());
        assertEquals(0, nonWord.length());
        assertTrue(reader.next(word, nonWord));
        assertEquals("aca", word.toString());
        assertEquals(0, nonWord.length());
        assertFalse(reader.next(word, nonWord));

        reader.setReader(new StringReader("abrac"));

        assertTrue(reader.next(word, nonWord));
        assertEquals("abr", word.toString());
        assertEquals(0, nonWord.length());
        assertTrue(reader.next(word, nonWord));
        assertEquals("bra", word.toString());
        assertEquals(0, nonWord.length());
        assertTrue(reader.next(word, nonWord));
        assertEquals("rac", word.toString());
        assertEquals(0, nonWord.length());
        assertFalse(reader.next(word, nonWord));

        reader.setReader(new StringReader("ABC"));

        assertTrue(reader.next(word, nonWord));
        assertEquals("ABC", word.toString());
        assertEquals(0, nonWord.length());

        assertFalse(reader.next(word, nonWord));

        ShufflingBioSequenceWordReader shufflereader = new ShufflingBioSequenceWordReader();
        shufflereader.setnGramLength(3);
        shufflereader.setSeed(1232323235);
        shufflereader.setReader(new StringReader("ABC"));

        assertTrue(shufflereader.next(word, nonWord));
        assertEquals("ACB", word.toString());
        assertEquals(0, nonWord.length());

        assertFalse(shufflereader.next(word, nonWord));
    }

    public void testOneGram() throws IOException {
        BioSequenceWordReader reader = new BioSequenceWordReader();
        reader.setnGramLength(1);
        reader.setReader(new StringReader("abraca"));
        MutableString word = new MutableString();
        MutableString nonWord = new MutableString();
        assertTrue(reader.next(word, nonWord));
        assertEquals("a", word.toString());
        assertEquals(0, nonWord.length());
        assertTrue(reader.next(word, nonWord));
        assertEquals("b", word.toString());
        assertEquals(0, nonWord.length());
        assertTrue(reader.next(word, nonWord));
        assertEquals("r", word.toString());
        assertEquals(0, nonWord.length());
        assertTrue(reader.next(word, nonWord));
        assertEquals("a", word.toString());
        assertEquals(0, nonWord.length());
        assertTrue(reader.next(word, nonWord));
        assertEquals("c", word.toString());
        assertEquals(0, nonWord.length());
        assertTrue(reader.next(word, nonWord));
        assertEquals("a", word.toString());
        assertEquals(0, nonWord.length());
        assertFalse(reader.next(word, nonWord));
    }

    /**
     * Check that TweaseReader works as ProteinWordSplitterReader for a subset
     * of strings.
     * @throws IOException error reading the author
     */
    public void testAuthorWordReader() throws IOException {
        AuthorsWordReader reader = new AuthorsWordReader();

        MutableString authors = new MutableString();
        authors.append("Dorff, KC");
        authors.append(" : Dorff, Kevin Charles");
        authors.append(" : Kevin Charles Dorff");
        authors.append(" : KC Dorff");
        authors.append(" : Dorff_KC");
        authors.append(" : KC_Dorff");
        authors.append(" | Smith, JD");
        authors.append(" : Smith, John Doe");
        authors.append(" : John Doe Smith");
        authors.append(" : JD Smith");
        authors.append(" : Smith_JD");
        authors.append(" : JD_SMITH");
        authors.append(" | ");
        reader.setReader(new StringReader(authors.toString()));

        MutableString word = new MutableString();
        MutableString nonWord = new MutableString();

        String[][] expecteds = new String[][] {
                {"dorff", ""}, {",", " "}, {"kc", " "},
                {":", " "}, {"dorff", ""}, {",", " "}, {"kevin", " "}, {"charles", " "},
                {":", " "}, {"kevin", " "}, {"charles", " "}, {"dorff", " "},
                {":", " "}, {"kc", " "}, {"dorff", " "},
                {":", " "}, {"dorff_kc", " "},
                {":", " "}, {"kc_dorff", " "},
                {"|", " "}, {"smith", ""}, {",", " "}, {"jd", " "},
                {":", " "}, {"smith", ""}, {",", " "}, {"john", " "}, {"doe", " "},
                {":", " "}, {"john", " "}, {"doe", " "}, {"smith", " "},
                {":", " "}, {"jd", " "}, {"smith", " "},
                {":", " "}, {"smith_jd", " "},
                {":", " "}, {"jd_smith", " "},
                {"|", " "}
        };

        for (String[] expected : expecteds) {
            assertTrue(reader.next(word, nonWord));
            // System.out.printf("ex='%s':'%s'  act='%s':'%s'%n",
            //     expected[0], expected[1], word, nonWord);
            assertEquals(expected[0], word.toString());
            assertEquals(expected[1], nonWord.toString());
        }
        assertFalse(reader.next(word, nonWord));
    }

}
