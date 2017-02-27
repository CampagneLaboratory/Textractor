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

package textractor.chain.loader;

import junit.framework.TestCase;
import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.impl.CatalogBase;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.IntRange;
import textractor.chain.AbstractSentenceConsumer;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.sentence.SentenceProcessingException;
import textractor.tools.chain.ChainExecutor;

import java.util.Collection;
import java.util.List;

/**
 * Validates the functionality of
 * {@link textractor.chain.loader.Html2TextArticleLoader} and
 * {@link textractor.html.Html2Text}.
 */
public final class TestHtml2TextArticleLoader extends TestCase {
    /**
     * Article under test.
     */
    private Article article;

    /**
     * Sentences under test.
     */
    private Collection<Sentence> sentences;

    /**
     * Reset data between tests.
     */
    protected void setUp() {
        article = null;
        sentences = null;
    }

    /**
     * Test locations with text as a single sentences.
     * @throws Exception if there was a problem locding the test file
     */
    public void testArticleLoaderSingleSentence() throws Exception {
        final Html2TextArticleLoader loader = new Html2TextArticleLoader();
        loader.setAppendSentencesInOneDocument(true);
        loader.setFile("data/test/html/medium.html");

        // add a simple consumer
        loader.addCommand(new TestConsumer());

        // and execute it
        final Catalog catalog = new CatalogBase();
        catalog.addCommand("Html2TextTest", loader);
        final ChainExecutor chainExecutor = new ChainExecutor(catalog);
        chainExecutor.execute();

        assertNotNull("Article cannont be null", article);
        assertNotNull("Sentence collection cannont be null", sentences);
        assertEquals("There should be 1 sentence", 1, sentences.size());

        final String expectedText =
                " John , Paul , George and Ringo It was 20 years ago today That Sgt. Pepper told his band to play They've been going in and out of style But they're guaranteed to raise a smile ";

        final IntRange[] expectedPositionRanges = {
                // "John, Paul, George and Ringo"
                new IntRange(0x1B, 0x36),
                // newline after "</title>"
                new IntRange(0x3F),
                // "It was"
                new IntRange(0x55, 0x5B),
                // "20 years ago today"
                new IntRange(0x5F, 0x70),
                // "<br />" after "today</b><br />"
                new IntRange(0x75),
                // "That "
                new IntRange(0x85, 0x89),
                // "Sgt."
                new IntRange(0x8D, 0x90),
                // "Pepper told"
                new IntRange(0x92, 0x9C),
                // " his band to play"
                new IntRange(0xA1, 0xB1),
                // "<br />" after "play"
                new IntRange(0xB2),
                // They've been going in and out of style"
                new IntRange(0xC2, 0xE7),
                // "<br />" after "style"
                new IntRange(0xE8),
                // "But they're guarenteed to raise a smile"
                new IntRange(0xF8, 0x11E)
        };

        // these are positons inserted by the splitter and don't
        // exist in the real document source
        final int[] artificialPositionIndexes = {
            0, 5, 12, 67, 175 // sentence boundary tags and padding around ","
        };

        final Sentence sentence = sentences.iterator().next();
        final String actualText = sentence.getText();
        assertEquals("Text doesn't match", expectedText, actualText);

        final List<Integer> positions = sentence.getPositions();
        assertNotNull("There should be some positions", positions);
        assertEquals("Number of positons does not match html",
                expectedText.length(), positions.size());

        int index = 0;
        int previousPostion = 0;
        for (IntRange range : expectedPositionRanges) {
            for (int expectedPosition = range.getMinimumInteger();
                 expectedPosition <= range.getMaximumInteger();
                 expectedPosition++) {
                if (ArrayUtils.contains(artificialPositionIndexes, index)) {
                    // this isn't a "real" position, so test it and continue
                    assertEquals("Not a valid position at index " + index
                        + " (" + expectedText.charAt(index) + ")",
                        previousPostion, (int) positions.get(index));
                    index++;
                }
                assertEquals("Mismatch at index " + index
                        + " (" + expectedText.charAt(index) + ")",
                        expectedPosition, (int) positions.get(index));
                previousPostion = expectedPosition;
                index++;
            }
        }

        // test for the last sentence boundary tag
        assertEquals("Not a valid position at index " + index
            + " (" + expectedText.charAt(index) + ")",
            previousPostion, (int) positions.get(index));
        index++;

        assertEquals("Didn't test all positions", positions.size(), index);
    }

    /**
     * Test locations with text split into mulitple sentences.
     * @throws Exception if there was a problem locding the test file
     */
    public void testArticleLoaderMutlipleSentence() throws Exception {
        // create a simple loader
        final Html2TextArticleLoader loader = new Html2TextArticleLoader();
        loader.setFile("data/test/html/medium.html");

        // add a simple consumer
        loader.addCommand(new TestConsumer());

        // and execute it
        final Catalog catalog = new CatalogBase();
        catalog.addCommand("Html2TextTest", loader);
        final ChainExecutor chainExecutor = new ChainExecutor(catalog);
        chainExecutor.execute();

        assertNotNull("Article cannont be null", article);
        assertNotNull("Sentence collection cannont be null", sentences);
        assertEquals("There should be 2 sentences", 2, sentences.size());

        final String[] expectedText = {
                "John , Paul , George and Ringo It was 20 years ago today That Sgt.",
                "Pepper told his band to play They've been going in and out of style But they're guaranteed to raise a smile"
        };

        final IntRange[][] expectedPositionRanges = {{
                // "John"
                new IntRange(0x1B, 0x1E),
                // ", Paul"
                new IntRange(0x1F, 0x24),
                // ", George and Ringo"
                new IntRange(0x25, 0x36),
                // newline after "</title>"
                new IntRange(0x3F),
                // "It was"
                new IntRange(0x55, 0x5B),
                // "20 years ago today"
                new IntRange(0x5F, 0x70),
                // "<br />" after "today</b><br />"
                new IntRange(0x75),
                // "That "
                new IntRange(0x85, 0x89),
                // "Sgt."
                new IntRange(0x8D, 0x90),
        }, {
                // "Pepper told"
                new IntRange(0x92, 0x9C),
                // " his band to play"
                new IntRange(0xA1, 0xB1),
                // "<br />" after "play"
                new IntRange(0xB2),
                // They've been going in and out of style"
                new IntRange(0xC2, 0xE7),
                // "<br />" after "style"
                new IntRange(0xE8),
                // "But they're guarenteed to raise a smile"
                new IntRange(0xF8, 0x11E)
        }};

        // these are positons inserted by the splitter and don't
        // exist in the real document source
        final int[] artificialPositionIndexes = {
            4, 11  // padding around "," in the first sentence
        };

        int i = 0;
        int previousPostion = 0;
        for (Sentence sentence : sentences) {
            final String actualText = sentence.getText();
            assertEquals("Text doesn't match", expectedText[i], actualText);

            final List<Integer> positions = sentence.getPositions();
            assertNotNull("There should be some positions", positions);
            assertEquals("Number of positons does not match html",
                    expectedText[i].length(), positions.size());

            int index = 0;
            for (IntRange range : expectedPositionRanges[i]) {
                for (int expectedPosition = range.getMinimumInteger();
                     expectedPosition <= range.getMaximumInteger();
                     expectedPosition++) {
                    if (i == 0  // the first sentence has artifical positions
                        && ArrayUtils.contains(artificialPositionIndexes, index)) {
                        // this isn't a "real" position, so test it and continue
                        assertEquals("Not a valid position at index " + index
                            + " (" + expectedText[i].charAt(index) + ")",
                            previousPostion, (int) positions.get(index));
                        index++;
                    }
                    assertEquals("Mismatch at index " + index
                            + " (" + expectedText[i].charAt(index) + ")",
                            expectedPosition, (int) positions.get(index));
                    previousPostion = expectedPosition;
                    index++;
                }
            }
            assertEquals("Didn't test all positions", positions.size(), index);
            i++;
        }
    }

    /**
     * Validates that paragraph boundaries are correctly stored and positioned.
     * @throws Exception if there was a problem locding the test file
     */
    public void testParagraphTags() throws Exception {
        final Html2TextArticleLoader loader = new Html2TextArticleLoader();
        loader.setAppendSentencesInOneDocument(true);
        loader.setParagraphBoundary(Html2TextArticleLoader.PARAGRAPH_BOUNDARY_TAG);
        loader.setFile("data/test/html/12096113.html");

        // add a simple consumer
        loader.addCommand(new TestConsumer());

        // and execute it
        final Catalog catalog = new CatalogBase();
        catalog.addCommand("Html2TextTest", loader);
        final ChainExecutor chainExecutor = new ChainExecutor(catalog);
        chainExecutor.execute();

        assertNotNull("Article cannont be null", article);
        assertNotNull("Sentence collection cannont be null", sentences);
        assertEquals("There should be 1 sentence", 1, sentences.size());

        // expected results in the form of:
        //  - position in sentence
        //  - position of first space in boundary relative to original text
        //  - position of boundary relative to original text
        final int[] expectedResults = {
                216, 0x2E1, 0x2EC,
                540, 0x489, 0x492,
                2402, 0x1155, 0x1162,
                4023, 0x1929, 0x1936,
                4605, 0x1C10, 0x1C1D,
                5169, 0x22F4, 0x2301,
                5591, 0x2585, 0x2592,
                6884, 0x2CF1, 0x2CFE,
                7764, 0x311B, 0x3128,
                8462, 0x3489, 0x3496,
                9215, 0x384F, 0x385C,
                9843, 0x3B6F, 0x3B7C,
                11237, 0x426F, 0x427C,
                12753, 0x4C81, 0x4C82,
                13354, 0x4FA9, 0x4FB6,
                14227, 0x581D, 0x582A,
                15429, 0x5DDA, 0x5DE7,
                16518, 0x658C, 0x658D,
                17900, 0x6C2B, 0x6C38,
                18510, 0x6EE0, 0x6EED,
                19083, 0x7170, 0x717D,
                19648, 0x7447, 0x7454,
                20730, 0x7A06, 0x7A13,
                21505, 0x805E, 0x805F,
                22258, 0x8452, 0x845F,
                23832, 0x8DEF, 0x8DF0,
                25114, 0x943E, 0x944B,
                26201, 0x9C06, 0x9C07,
                27028, 0xA459, 0xA466,
                28392, 0xAB08, 0xAB15,
                30406, 0xB4D2, 0xB4DF,
                30526, 0xB6B2, 0xB6BF,
                30624, 0xB873, 0xB874,
                30725, 0xB8EC, 0xB8F3,
                30942, 0xBA66, 0xBA66,
                31225, 0xBC36, 0xBC36,
                31359, 0xBD49, 0xBD49,
                31669, 0xBF25, 0xBF25,
                31715, 0xC000, 0xC000,
                32102, 0xC1DD, 0xC1DD,
                32361, 0xC843, 0xC843,
                32510, 0xC9D5, 0xC9D5,
                32731, 0xCBCB, 0xCBCB,
                33045, 0xCDE3, 0xCDE3,
                33342, 0xCFDE, 0xCFDE,
                33548, 0xD1A0, 0xD1A0,
                33872, 0xD3DE, 0xD3DE,
                34068, 0xD542, 0xD542,
                34414, 0xD774, 0xD774,
                34697, 0xD965, 0xD965,
                34887, 0xDAFD, 0xDAFD,
                35080, 0xDC95, 0xDC95,
                35320, 0xDDD6, 0xDDD6,
                35505, 0xDEE6, 0xDEE6,
                35782, 0xE0AF, 0xE0AF,
                36124, 0xE2D7, 0xE2D7,
                36402, 0xE4E5, 0xE4E5,
                36671, 0xE6EA, 0xE6EA,
                36964, 0xE868, 0xE868,
                37152, 0xEA12, 0xEA12,
                37357, 0xEBCD, 0xEBCD
        };
        final Sentence sentence = sentences.iterator().next();
        final String text = sentence.getText();
        final List<Integer> positions = sentence.getPositions();

        int i = 0;
        int pos = 0;
        while ((pos = text.indexOf(Html2TextArticleLoader.PARAGRAPH_BOUNDARY_TAG, pos)) != -1) {
            assertEquals("incorrect boundary position at index " + i,
                    expectedResults[i], pos);

            // check positon of first space
            i++;
            assertEquals("incorrect whitespace position at index " + i,
                    expectedResults[i], (int) positions.get(pos));

            // and check the rest of the position bounaries
            i++;
            for (int j = pos + 1; j < pos + Html2TextArticleLoader.PARAGRAPH_BOUNDARY_TAG.length(); j++) {
                assertEquals("incorrect tag position at index " + i + ", " + j,
                        expectedResults[i], (int) positions.get(j));
            }
            pos += Html2TextArticleLoader.PARAGRAPH_BOUNDARY_TAG.length();

            // increment for the next iteration
            i++;
        }

        assertEquals("Didn't test all positions", expectedResults.length, i);
    }

    /**
     * A simple {@link textractor.sentence.SentenceConsumer} used to
     * store the article and sentence objects created during the html
     * load process.
     */
    private class TestConsumer extends AbstractSentenceConsumer {
        /**
         * Get the number of articles processed so far.
         *
         * @return The number of articles processed so far
         */
        public int getNumberOfArticlesProcessed() {
            // not needed for this test
            return 0;
        }

        /**
         * Get the number of sentences processed so far.
         *
         * @return The number of sentences processed so far
         */
        public int getNumberOfSentencesProcessed() {
            // not needed for this test
            return 0;
        }

        /**
         * Process sentences along with their associated article.
         *
         * @param a The article assoicated with the sentences.
         * @param s A collection of Sentences to process.
         * @throws textractor.sentence.SentenceProcessingException
         * If there was an error condition in the textractor sentence
         * processing pipeline
         */
        public void consume(final Article a, final Collection<Sentence> s)
                throws SentenceProcessingException {
            article = a;
            sentences = s;
        }
    }
}
