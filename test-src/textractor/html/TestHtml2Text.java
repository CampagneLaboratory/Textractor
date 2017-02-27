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

package textractor.html;

import it.unimi.dsi.mg4j.util.MutableString;
import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.IntRange;
import org.htmlparser.util.ParserException;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Validates the functionality of {@link textractor.html.Html2Text}.
 */
public class TestHtml2Text extends TestCase {
    /**
     * Test with an extremely simple html file.
     * @throws IOException if the test file cannot be read
     * @throws ParserException if there is a problem parsing the html
     */
    public void testSimple() throws IOException, ParserException {
        final String expectedText = "hello";
        final int[] expectedPositions = {6, 7, 8, 9, 10};
        final Html2Text html2Text = new Html2Text();
        final FileReader reader = new FileReader("data/test/html/simple.html");
        html2Text.parse(reader);
        assertEquals("Title should be empty",
                StringUtils.EMPTY, html2Text.getTitle());

        final String actualText = html2Text.getText();
        assertEquals("Text does not match html", expectedText, actualText);

        final List<Integer> positions = html2Text.getPositions();
        assertNotNull("There should be some positions", positions);
        assertEquals("Number of positons does not match html",
                expectedPositions.length, positions.size());

        for (int index = 0; index < expectedPositions.length; index++) {
            assertEquals("Mismatch at index " + index
                    + " (" + expectedText.charAt(index) + ")",
                    expectedPositions[index], (int )positions.get(index));
        }
    }

    /**
     * Test with a very basic html file.
     * NOTE: The assumption here is that the test file has windows
     * line endings.  If this is not the case, the locations might
     * be off due to these differences.
     * @throws IOException if the test file cannot be read
     * @throws ParserException if there is a problem parsing the html
     */
    public void testMedium() throws IOException, ParserException {
        final String expectedTitle = "John, Paul, George and Ringo";
        // note that the text has a space at the beginning and end
        final String expectedText = " " + expectedTitle
                + " It was 20 years ago today That Sgt. Pepper told"
                + " his band to play They've been going in and out of style"
                + " But they're guaranteed to raise a smile ";
        final Html2Text html2Text = new Html2Text();
        final FileReader reader = new FileReader("data/test/html/medium.html");
        html2Text.parse(reader);
        assertEquals("Title does not match html",
                expectedTitle, html2Text.getTitle());

        final String actualText = html2Text.getText();
        assertEquals("Text does not match html", expectedText, actualText);

        final List<Integer> positions = html2Text.getPositions();
        assertNotNull("There should be some positions", positions);
        assertEquals("Number of positons does not match html",
                expectedText.length(), positions.size());

        int index = 0;
        final IntRange[] expectedPositionRanges = {
                // newline after "<head>"
                new IntRange(0x6),
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
                // "Sgt. Pepper told"
                new IntRange(0x8D, 0x9C),
                // " his band to play"
                new IntRange(0xA1, 0xB1),
                // "<br />" after "play"
                new IntRange(0xB2),
                // They've been going in and out of style"
                new IntRange(0xC2, 0xE7),
                // "<br />" after "style"
                new IntRange(0xE8),
                // "But they're guarenteed to raise a smile<br />"
                new IntRange(0xF8, 0x11F)
        };

        for (IntRange range : expectedPositionRanges) {
            for (int expectedPosition = range.getMinimumInteger();
                 expectedPosition <= range.getMaximumInteger();
                 expectedPosition++) {
                assertEquals("Mismatch at index " + index
                        + " (" + expectedText.charAt(index) + ")",
                        expectedPosition, (int )positions.get(index));
                index++;
            }
        }

        assertEquals("Didn't test all positions", positions.size(), index);
    }

    /**
     * Test with a reasonably complex html file.
     * @throws IOException if the test file cannot be read
     * @throws ParserException if there is a problem parsing the html
     */
    public void testComplex() throws IOException, ParserException {
        final String expectedText = " ."  // "<H2>" is replaced with ". "
                + " Proteomic Strategies to Reveal Tumor"
                + " Heterogeneity among Urothelial Papillomas"
                + " sup*"                // "<SUP><A HREF="#FN4">*</A></SUP>"
                + " ."                   // </H2>" is replaced with ". "
                + " Proteomics and immunohistochemistry were used to reveal"
                + " tumor heterogeneity among urothelial papillomas (UPs) with"
                + " the long term goal of predicting their biological potential"
                + " in terms of outcome. First, we identified proteins that"
                + " were deregulated in invasive fresh lesions as compared with"
                + " normal urothelium, and thereafter we immunostained UPs"
                + " with a panel of antibodies against some of the markers."
                + " Twenty-two major proteins showing variations of 2-fold or"
                + " more in at least one-third of the invasive lesions were"
                + " selected. Specific antibodies against several of the"
                + " proteins were obtained, but only a few reacted positively"
                + " in immunostaining. A panel consisting of antibodies against"
                + " keratinocytes (CKs) 5, 13, 18, and 20 and markers of"
                + " squamous metaplasia (CKs 7, 8, and 14) was used to probe"
                + " normal urothelium and 30 UPs collected during a period of"
                + " five years. Four UPs showed a normal phenotype, whereas the"
                + " rest could be grouped in five major types that shared"
                + " aberrant staining with the CK20 antibody. Type 1"
                + " heterogeneity (n = 4) showed preferred staining of the"
                + " umbrella cells with the CK8 antibody. Type 2 (n = 11) was"
                + " typified by the staining of the basal and intermediate"
                + " layers with the CK20 antibody. Type 3 (n = 7) was"
                + " characterized by the predominant staining of the basal cell"
                + " layer with the CK5 antibody. Type 4 (n = 1) showed areas of"
                + " CK7 negative cells, whereas type 5 (n = 3) showed loss of"
                + " staining of the basal cells with the CK20. 29% of the"
                + " patients experienced recurrences, but none progressed to"
                + " invasive disease. Patients harboring phenotypic alterations"
                + " in the basal cell compartment (types 3 and 5) showed the"
                + " highest number of recurrences (4/7 and 2/3, respectively),"
                + " and all type 3 lesions progressed to a higher degree of"
                + " dedifferentiation. Even though a long term prospective"
                + " study involving a larger sample size is required to assess"
                + " the biological potential of these lesions, we believe that"
                + " this approach will prove instrumental for revealing early"
                + " phenotypic changes in different types of . ";
        final Html2Text html2Text = new Html2Text();
        final FileReader reader = new FileReader("data/test/html/complex.html");
        html2Text.parse(reader);
        assertEquals("Title should be empty",
                StringUtils.EMPTY, html2Text.getTitle());

        final String actualText = html2Text.getText();
        assertEquals("Text does not match html", expectedText, actualText);

        final List<Integer> positions = html2Text.getPositions();
        assertNotNull("There should be some positions", positions);
        assertEquals("Number of positons does not match html",
                expectedText.length(), positions.size());

        // test a few locations
        int keratinocytesIndex = actualText.indexOf("keratinocytes (CKs)");
        final IntRange keratinocytesRange = new IntRange(0x3CA, 0x3DC);
        for (int expectedPosition = keratinocytesRange.getMinimumInteger();
             expectedPosition <= keratinocytesRange.getMaximumInteger();
             expectedPosition++) {
            assertEquals("Mismatch at index " + keratinocytesIndex
                    + " (" + expectedText.charAt(keratinocytesIndex) + ")",
                    expectedPosition, (int )positions.get(keratinocytesIndex));
            keratinocytesIndex++;
        }

        int ck20AntibodyIndex = actualText.indexOf("CK20 antibody.");
        final IntRange[] ck20AntibodyRanges = {
                // "CK20"
                new IntRange(0x52B, 0x52E),
                // "<SUP> </SUP>"
                new IntRange(0x534),
                // "antibody"
                new IntRange(0x53B, 0x544)
        };

        for (IntRange range : ck20AntibodyRanges) {
            for (int expectedPosition = range.getMinimumInteger();
                 expectedPosition <= range.getMaximumInteger();
                 expectedPosition++) {
                assertEquals("Mismatch at index " + ck20AntibodyIndex
                        + " (" + expectedText.charAt(ck20AntibodyIndex) + ")",
                        expectedPosition, (int )positions.get(ck20AntibodyIndex));
                ck20AntibodyIndex++;
            }
        }
    }

    /**
     * Tests
     * * All newlines & tabs are treated as spaces.
     * * Multiple spaces are removed
     */
    public void testStripWhitespace() throws ParserException, IOException {
        Html2Text h = new Html2Text();
        h.parseString(new MutableString("   this   is\n\n\t\t\tsome text    "));
        assertEquals(" this is some text ", h.getText());
    }

    /**
     * Tests
     * * alt properties within <img> tags are treated as text and
     *   alt tag values are trimmed of extra spaces (left and right).
     */
    public void testParseImgAlt() throws ParserException, IOException {
        Html2Text h = new Html2Text();
        h.parseString(new MutableString("Some text<img src='abc.gif' alt=' beta ' width='10'>more text"));
        assertEquals("IMG alt parsed wrong", "Some textbetamore text", h.getText());
        h.parseString(new MutableString("Some text <IMG src='abc.gif' alt='alpha'> more text"));
        assertEquals("Some text alpha more text", h.getText());
    }

    /**
     * Tests
     * * &nbsp; is treated as a normal space.
     */
    public void testFixNbsp() throws ParserException, IOException {
        Html2Text h = new Html2Text();
        h.parseString(new MutableString("This is&nbsp;&nbsp;&nbsp;&nbsp;a test"));
        assertEquals("&nbsp parsed wrong", "This is a test", h.getText());
        h.parseString(new MutableString("This is &nbsp;&nbsp;another&nbsp;test"));
        assertEquals("This is another test", h.getText());
    }

    /**
     * Tests
     * * a space (and thus a space) is forced before <p> and <br> tags
     */
    public void testSpaceBeforeBreaks() throws ParserException, IOException {
        Html2Text h = new Html2Text();
        h.parseString(new MutableString("this<p>new para<br>another line<p>more"));
        assertEquals("this new para another line more", h.getText());
    }

    /**
     * Tests
     * * a ". " is forced before and after <H1>, <H2>, <H3>, <H4> but
     *   that ". " won't be placed if there is already a ". " in the
     *   text just preceeding.
     */
    public void testPeriodsAtHeaderTags() throws ParserException, IOException {
        Html2Text h = new Html2Text();
        h.parseString(new MutableString("Hi<h1>how are you<h2>I am fine<h3>thanks<h4>your welcome. <h1>no problem."));
        assertEquals("Hi. how are you. I am fine. thanks. your welcome. no problem. ", h.getText());
    }

    /**
     * Tests
     * * "><IMG ..." will always be treated as "> <IMG "
     */
    public void testSeperatedTagImg() throws ParserException, IOException {
        Html2Text h = new Html2Text();
        h.parseString(new MutableString("some<a href='a.html'><img alt='enchanted even'>ing</a> yes"));
        assertEquals("some enchanted evening yes", h.getText());
    }

    /**
     * Tests
     * * "<sup><a ..." will always be treated as "<sup> sup<a ..."
     */
    public void testSupHandling() throws ParserException, IOException {
        Html2Text h = new Html2Text();
        h.parseString(new MutableString("this<sup>x</sup> and that<sup><a href='that'>blah</a>"));
        assertEquals("thisx and that supblah", h.getText());
    }

    public void testNoParagraphBoundaryTag() throws ParserException, IOException {
        final String boundaryTag = " "; // <P> tags will be replaced with " "
        final String expectedText = boundaryTag + "Paragraph 1." + boundaryTag
                + "Paragraph 2." + boundaryTag + "Paragraph 3.";
        final Html2Text h = new Html2Text();
        h.parseString(new MutableString("<p>Paragraph 1.<p>Paragraph 2.</p><P>Paragraph 3."));
        assertEquals(expectedText, h.getText());
        final List<Integer> positions = h.getPositions();
        assertEquals(expectedText.length(), positions.size());

        final IntRange[] expectedPositionRanges = {
                // <p>
                new IntRange(0x0),
                // "Paragraph 1."
                new IntRange(0x3, 0xE),
                // <p>
                new IntRange(0xF),
                // Paragraph 2.
                new IntRange(0x12, 0x1D),
                // </p><P>
                new IntRange(0x22),
                // Paragraph 3.
                new IntRange(0x25, 0x30)
        };

        int index = 0;
        for (IntRange range : expectedPositionRanges) {
            for (int expectedPosition = range.getMinimumInteger();
                 expectedPosition <= range.getMaximumInteger();
                 expectedPosition++) {
                assertEquals("Mismatch at index " + index
                        + " (" + expectedText.charAt(index) + ")",
                        expectedPosition, (int )positions.get(index));
                index++;
            }
        }

        assertEquals("Didn't test all positions", positions.size(), index);
    }

    public void testParagraphBoundaryTag() throws ParserException, IOException {
        final String boundaryTag = " foobar ";
        final String expectedText = boundaryTag + "Paragraph 1." + boundaryTag
                + "Paragraph 2." + boundaryTag + "Paragraph 3.";
        final Html2Text h = new Html2Text();
        h.setParagraphBoundaryTag(boundaryTag);
        h.parseString(new MutableString("<p>Paragraph 1.<p>Paragraph 2.</p><P>Paragraph 3."));
        assertEquals(boundaryTag + "Paragraph 1." + boundaryTag + "Paragraph 2."
            + boundaryTag + "Paragraph 3.", h.getText());
        assertEquals(expectedText, h.getText());
        final List<Integer> positions = h.getPositions();
        assertEquals(expectedText.length(), positions.size());

        final IntRange[] expectedPositionRanges = {
                // <p> (replaced with " foobar ")
                new IntRange(0x0), new IntRange(0x0), new IntRange(0x0),
                new IntRange(0x0), new IntRange(0x0), new IntRange(0x0),
                new IntRange(0x0), new IntRange(0x0),
                // "Paragraph 1."
                new IntRange(0x3, 0xE),
                // <p> (replaced with " foobar ")
                new IntRange(0xF), new IntRange(0xF), new IntRange(0xF),
                new IntRange(0xF), new IntRange(0xF), new IntRange(0xF),
                new IntRange(0xF), new IntRange(0xF),
                // Paragraph 2.
                new IntRange(0x12, 0x1D),
                // <p> (replaced with " foobar ")
                new IntRange(0x22), new IntRange(0x22), new IntRange(0x22),
                new IntRange(0x22), new IntRange(0x22), new IntRange(0x22),
                new IntRange(0x22), new IntRange(0x22),
                // Paragraph 3.
                new IntRange(0x25, 0x30)
        };

        int index = 0;
        for (IntRange range : expectedPositionRanges) {
            for (int expectedPosition = range.getMinimumInteger();
                 expectedPosition <= range.getMaximumInteger();
                 expectedPosition++) {
                assertEquals("Mismatch at index " + index
                        + " (" + expectedText.charAt(index) + ")",
                        expectedPosition, (int )positions.get(index));
                index++;
            }
        }

        assertEquals("Didn't test all positions", positions.size(), index);
    }
    public void testNormalizeAlt() {
        String beta="{beta}";
        assertEquals("beta",TextractorTextExtractingVisitor.normalizeAlt(beta));
        assertEquals("beta",TextractorTextExtractingVisitor.normalizeAlt("beta"));
        assertEquals("{donotnormalize",TextractorTextExtractingVisitor.normalizeAlt("{donotnormalize"));
        assertEquals("donotnormalize}",TextractorTextExtractingVisitor.normalizeAlt("donotnormalize}"));
    }

    /**
     * Tests parsing HTML -> Unicode -> text of greek chars.
     * @throws ParserException problem parsing HTML
     * @throws IOException ioexception
     */
    public void testGreekFromHtml() throws ParserException, IOException {
        Html2Text h = new Html2Text();
        h.parseString(new MutableString("this &Alpha; is &gamma; nice!"));
        int[] expectedPositions = {0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 12, 13, 14, 15,
            16, 16, 16, 16, 16, 23, 24, 25, 26, 27, 28};
        // Uppercase Alpha, lowercase gamma
        assertEquals("this alpha is gamma nice!", h.getText());
        List<Integer> positions = h.getPositions();
        assertEquals("Incorrect positions count", expectedPositions.length, positions.size());
        for (int i = 0; i < expectedPositions.length; i++) {
            int pos = positions.get(i);
            int expPos = expectedPositions[i];
            assertEquals("Position[" + i + "] incorrect", expPos, pos);
        }
    }

    /**
     * Tests parsing Unicode -> text of greek chars.
     * @throws ParserException problem parsing HTML
     * @throws IOException ioexception
     */
    public void testGreekFromUnicode() throws ParserException, IOException {
        Html2Text h = new Html2Text();
        h.parseString(new MutableString("this " + '\u0391' + " is " + '\u03b3' + " nice!"));
        int[] expectedPositions = {0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 6, 7, 8, 9,
            10, 10, 10, 10, 10, 11, 12, 13, 14, 15, 16};
        // Uppercase Alpha, lowercase gamma
        assertEquals("this alpha is gamma nice!", h.getText());
        List<Integer> positions = h.getPositions();
        assertEquals("Incorrect positions count", expectedPositions.length, positions.size());
        for (int i = 0; i < expectedPositions.length; i++) {
            int pos = positions.get(i);
            int expPos = expectedPositions[i];
            assertEquals("Position[" + i + "] incorrect", expPos, pos);
        }
    }

}
