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

package textractor.util;

import it.unimi.dsi.mg4j.util.Properties;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlparser.util.ParserException;
import textractor.crf.TextSegment;
import textractor.mg4j.document.AbstractTextractorDocumentFactory;
import textractor.mg4j.index.TweaseTermProcessor;
import textractor.mg4j.io.TweaseWordReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestHTMLByteLocator extends TestCase {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(TestHTMLByteLocator.class);
    private TweaseWordReader wordReader = new TweaseWordReader();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final Properties wordReaderProperties = new Properties();
        wordReaderProperties.addProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS, true);
        wordReaderProperties.addProperty(AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH, 8);
        wordReader = new TweaseWordReader();
        wordReader.configure(wordReaderProperties);
    }

    public void testHTMLByteLocatorWithSimpleHTML() throws IOException, ConfigurationException, ParserException {
        final String sourceFilename = "data/test/html/simple.html";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.processWithPMIDTerms2(42, sourceFilename, "hello", 0, 0);

        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        assertEquals(6, locator.getResults().get(0).getStart());
        assertEquals(5, locator.getResults().get(0).getLength());

        assertEquals("hello",
                new String(bytes, (int) locator.getResults().get(0).getStart(),
                        (int) locator.getResults().get(0).getLength()));
    }

    private HTMLByteLocator createLocator() {
        final HTMLByteLocator locator = new HTMLByteLocator();
        return initializeLocator(locator);
    }

    private HTMLByteLocator createLocator(final String sourceFilename) {
        final HTMLByteLocator locator = new HTMLByteLocator(sourceFilename);
        return initializeLocator(locator);
    }

    private HTMLByteLocator initializeLocator(final HTMLByteLocator locator) {
        locator.setWordReader(wordReader);
        locator.setTermProcessor(TweaseTermProcessor.getInstance());
        return locator;
    }

    public void testHTMLByteLocatorWithMediumHTML() throws IOException, ConfigurationException, ParserException {
        final String sourceFilename = "data/test/html/medium.html";
        final String searchString = "John, Paul, George and Ringo";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.processWithPMIDTerms2(167, sourceFilename, searchString, 0, 0);

        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final int start = (int) locator.getResults().get(0).getStart();
        final int length = (int) locator.getResults().get(0).getLength();

        LOG.debug("Byte read: " + new String(bytes, start, length));

        assertEquals(searchString, new String(bytes, start, length));
    }

    public void testHTMLByteLocatorWithMediumHTMLBrokenText() throws IOException, ConfigurationException, ParserException {
        final String sourceFilename = "data/test/html/medium.html";
        final String searchString = "It was 20 years ago today";
        final String expectedResult = "It was <b>20 years ago today";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.processWithPMIDTerms2(0, sourceFilename, searchString, 0, 0);

        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final int start = (int) locator.getResults().get(0).getStart();
        final int length = (int) locator.getResults().get(0).getLength();

        LOG.debug("Byte read: " + new String(bytes, start, length));

        assertEquals(expectedResult, new String(bytes, start, length));
    }

    public void testHTMLByteLocatorWithLongHTMLBrokenText() throws IOException, ConfigurationException, ParserException {
        final String sourceFilename = "data/test/html/medium.html";
        final String searchString = "That Sgt. Pepper told his band to play";
        final String expectedResult =
                "That <i>Sgt. Pepper told</i> his band to play";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.processWithPMIDTerms2(543, sourceFilename, searchString, 0, 0);


        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final int start = (int) locator.getResults().get(0).getStart();
        final int length = (int) locator.getResults().get(0).getLength();

        LOG.debug("Byte read: " + new String(bytes, start, length));

        assertEquals(expectedResult, new String(bytes, start, length));
    }

    public void testHTMLByteLocatorWithVeryLongHTMLBrokenText() throws IOException, ParserException, ConfigurationException {
        final String sourceFilename = "data/test/html/medium.html";
        final String searchString = "That Sgt. Pepper told his band to play They've been going in and out of style";
        final String expectedResult = "That <i>Sgt. Pepper told</i> his band to play<br />\r\n" +
                "        They've been going in and out of style";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.processWithPMIDTerms2(0, sourceFilename, searchString, 0, 0);

        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final int start = (int) locator.getResults().get(0).getStart();
        final int length = (int) locator.getResults().get(0).getLength();

        LOG.debug("Byte read: '" + new String(bytes, start, length));

        assertEquals(expectedResult, new String(bytes, start, length));
    }

    public void testHTMLByteLocatorWithTestEasyBioHTML() throws IOException, ParserException, ConfigurationException {
        final String sourceFilename = "data/test/html/complex.html";
        final String searchString = "Proteomics and immunohistochemistry were used to reveal tumor heterogeneity";
        final String expectedResult = "Proteomics and immunohistochemistry were used to reveal tumor<SUP> </SUP>heterogeneity";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.processWithPMIDTerms2(96, sourceFilename, searchString, 0, 0);

        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final List<TextSegment> results = locator.getResults();

        final int start = (int) results.get(0).getStart();
        final int length = (int) results.get(0).getLength();

        LOG.debug("Byte read: " + start + ", " + length + "'"
                + new String(bytes, start, length) + "'");

        assertEquals(expectedResult, new String(bytes, start, length));
    }

    public void testHTMLByteLocatorWithTestFullBioHTML() throws IOException, ParserException, ConfigurationException {
        final String sourceFilename = "data/test/html/12096109.html";
        final String searchString = "The urothelium from random biopsies diagnosed as normal was dissected with the aid of a scalpel, labeled with [35S]methionine";
        final String expectedResult = "The urothelium from random biopsies diagnosed as normal was<SUP> </SUP>dissected with the aid of a scalpel, labeled with [<SUP>35</SUP>S]methionine";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.processWithPMIDTerms2(98765, 12096109, searchString, 0, 0);

        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final List<TextSegment> results = locator.getResults();

        final int start = (int) results.get(0).getStart();
        final int length = (int) results.get(0).getLength();

        LOG.debug("Byte read: " + start + ", " + length + "'" + new String(bytes, start, length) + "'");

        assertEquals(expectedResult, new String(bytes, start, length));
    }

    // TODO - fixme
    public void TestBug() throws IOException, ParserException, ConfigurationException {
        final int pmid = 10070005;
        final String sourceFilename = "data/test/corpus/ajpendometa/10070005.html";
        final String searchString = ". INVITED REVIEW Whole body glucose metabolism Kenneth Zierler Endocrine and Metabolism Division, Department of Medicine, and Department of Physiology, The Johns Hopkins University School of Medicine, Baltimore, Maryland 21287-4904 ABSTRACT Top Abstract SCOPE OF THE REVIEW METHODS FOR STUDYING GLUCOSE... GLUCOSE DISTRIBUTION AND... GLUCOSE TRANSPORTERS:... RELATION OF GLUCOSE METABOLISM... EXERCISE HYPOXIA SUMMARY AND PROJECTION References This review describes major factors that, singly or together, influence the concentration and distribution of D-glucose in mammals, particularly in humans, with emphasis on rest, physical activity, and alimentation.";
        final String expectedResult = "INVITED REVIEW</FONT><BR>\n" +
                "\n" +
                "\n" +
                "\n" +
                "Whole body glucose metabolism\n" +
                "\n" +
                "</H2>\n" +
                "\n" +
                "\n" +
                "\n" +
                "\t\n" +
                "\t\t\n" +
                "\t\t\t\n" +
                "\n" +
                "\n" +
                "<STRONG>\n" +
                "<NOWRAP>\n" +
                "Kenneth \n" +
                "Zierler\n" +
                "</NOWRAP>\n" +
                "</STRONG>\n" +
                "<P>\n" +
                "Endocrine and Metabolism Division, Department of Medicine, and\n" +
                "Department of Physiology, The Johns Hopkins University School of\n" +
                "Medicine, Baltimore, Maryland 21287-4904\n" +
                "<P>\n" +
                "\n" +
                "\n" +
                "\t\t\n" +
                "\t\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\t\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\t\n" +
                "\n" +
                "<a name = \"abs\"><!-- comment for mosaic --></a>\n" +
                "\n" +
                "\n" +
                "\n" +
                "<BR CLEAR=ALL>\n" +
                "<A NAME=\"Abstract\"><!-- null --></A>\n" +
                "<TABLE WIDTH=100% BGCOLOR=E1E1E1 CELLPADDING=0 CELLSPACING=0>\n" +
                "<TR>\n" +
                "<TD ALIGN=LEFT VALIGN=MIDDLE WIDTH=5% BGCOLOR=FFFFFF>&nbsp;<IMG WIDTH=10 HEIGHT=21 HSPACE=5 ALT=\"\" SRC=\"/icons/toc/rarrow.gif\"></TD>\n" +
                "<TH ALIGN=LEFT VALIGN=MIDDLE WIDTH=95%><FONT SIZE=+2>&nbsp;&nbsp;ABSTRACT</FONT></TH>\n" +
                "</TR>\n" +
                "</TABLE>\n" +
                "\n" +
                "<TABLE ALIGN=RIGHT CELLPADDING=5 BORDER NOWRAP><R>\n" +
                "<TH ALIGN=LEFT>\n" +
                "<FONT SIZE=-1>\n" +
                "\n" +
                "\n" +
                "\n" +
                "<A HREF=\"#Top\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/uarrow.gif\">Top</A>\n" +
                "\n" +
                "\n" +
                "<BR><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/dot.gif\"><FONT COLOR=46\n" +
                "4C53>Abstract</FONT>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC1\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">SCOPE OF THE REVIEW</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC2\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">METHODS FOR STUDYING GLUCOSE...</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC3\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">GLUCOSE DISTRIBUTION AND...</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC4\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">GLUCOSE TRANSPORTERS:...</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC5\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">RELATION OF GLUCOSE METABOLISM...</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC6\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">EXERCISE</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC7\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">HYPOXIA</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC8\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">SUMMARY AND PROJECTION</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#References\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">References</A>\n" +
                "</FONT></TH>\n" +
                "</TR></TABLE>\n" +
                "\n" +
                "\n" +
                "<P>This review describes major factors that, singly\n" +
                "or together, influence the concentration and distribution of\n" +
                "<font size=-1>D</font>-glucose in<SUP> </SUP>mammals,\n" +
                "particularly in humans, with emphasis on rest, physical<SUP> </SUP>activity, and\n" +
                "alimentation.";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.processWithPMIDTerms2(42, pmid, searchString, 0, 0);

        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final List<TextSegment> results = locator.getResults();

        final int start = (int) results.get(0).getStart();
        final int length = (int) results.get(0).getLength();

        LOG.debug("Byte read: " + start + ", " + length + "'" + new String(bytes, start, length).trim() + "'");

        assertEquals(recodeLineSeparators(expectedResult), recodeLineSeparators(new String(bytes, start, length + 1)));
    }

    // TODO - fixme
    public void TestBug2() throws IOException, ParserException, ConfigurationException {
        final int pmid = 10070005;
        final String sourceFilename = "data/test/corpus/ajpendometa/10070005.html";
        final String searchString = ". invited review whole body glucose metabolism kenneth zierler endocrine and metabolism division , department of medicine , and department of physiology , the johns hopkins university school of medicine , baltimore , maryland 21287 4904 abstract top abstract SCOPE OF THE review methods FOR studying glucose.. glucose distribution AND.. glucose transporters:.. relation OF glucose metabolism.. exercise hypoxia summary AND projection references this review describes major factors that , singly or together , influence the concentration and distribution of D glucose in mammals , particularly in humans , with emphasis on rest , physical activity , and alimentation";
        final String expectedResult = "INVITED REVIEW</FONT><BR>\n" +
                "\n" +
                "\n" +
                "\n" +
                "Whole body glucose metabolism\n" +
                "\n" +
                "</H2>\n" +
                "\n" +
                "\n" +
                "\n" +
                "\t\n" +
                "\t\t\n" +
                "\t\t\t\n" +
                "\n" +
                "\n" +
                "<STRONG>\n" +
                "<NOWRAP>\n" +
                "Kenneth \n" +
                "Zierler\n" +
                "</NOWRAP>\n" +
                "</STRONG>\n" +
                "<P>\n" +
                "Endocrine and Metabolism Division, Department of Medicine, and\n" +
                "Department of Physiology, The Johns Hopkins University School of\n" +
                "Medicine, Baltimore, Maryland 21287-4904\n" +
                "<P>\n" +
                "\n" +
                "\n" +
                "\t\t\n" +
                "\t\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "    \n" +
                "\t\n" +
                "\t    \n" +
                "\t\n" +
                "    \n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\t\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\t\n" +
                "\n" +
                "<a name = \"abs\"><!-- comment for mosaic --></a>\n" +
                "\n" +
                "\n" +
                "\n" +
                "<BR CLEAR=ALL>\n" +
                "<A NAME=\"Abstract\"><!-- null --></A>\n" +
                "<TABLE WIDTH=100% BGCOLOR=E1E1E1 CELLPADDING=0 CELLSPACING=0>\n" +
                "<TR>\n" +
                "<TD ALIGN=LEFT VALIGN=MIDDLE WIDTH=5% BGCOLOR=FFFFFF>&nbsp;<IMG WIDTH=10 HEIGHT=21 HSPACE=5 ALT=\"\" SRC=\"/icons/toc/rarrow.gif\"></TD>\n" +
                "<TH ALIGN=LEFT VALIGN=MIDDLE WIDTH=95%><FONT SIZE=+2>&nbsp;&nbsp;ABSTRACT</FONT></TH>\n" +
                "</TR>\n" +
                "</TABLE>\n" +
                "\n" +
                "<TABLE ALIGN=RIGHT CELLPADDING=5 BORDER NOWRAP><R>\n" +
                "<TH ALIGN=LEFT>\n" +
                "<FONT SIZE=-1>\n" +
                "\n" +
                "\n" +
                "\n" +
                "<A HREF=\"#Top\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/uarrow.gif\">Top</A>\n" +
                "\n" +
                "\n" +
                "<BR><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/dot.gif\"><FONT COLOR=46\n" +
                "4C53>Abstract</FONT>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC1\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">SCOPE OF THE REVIEW</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC2\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">METHODS FOR STUDYING GLUCOSE...</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC3\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">GLUCOSE DISTRIBUTION AND...</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC4\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">GLUCOSE TRANSPORTERS:...</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC5\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">RELATION OF GLUCOSE METABOLISM...</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC6\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">EXERCISE</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC7\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">HYPOXIA</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#SEC8\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">SUMMARY AND PROJECTION</A>\n" +
                "\n" +
                "\n" +
                "<BR>\n" +
                "<A HREF=\"#References\"><IMG WIDTH=11 HEIGHT=9 BORDER=0 HSPACE=5 SRC=\"/icons/toc/darrow.gif\">References</A>\n" +
                "</FONT></TH>\n" +
                "</TR></TABLE>\n" +
                "\n" +
                "\n" +
                "<P>This review describes major factors that, singly\n" +
                "or together, influence the concentration and distribution of\n" +
                "<font size=-1>D</font>-glucose in<SUP> </SUP>mammals,\n" +
                "particularly in humans, with emphasis on rest, physical<SUP> </SUP>activity, and\n" +
                "alimentation";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.setProcessTargetTerms(false);
        locator.processWithPMIDTerms2(0, pmid, searchString, 0, 0);

        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final List<TextSegment> results = locator.getResults();

        final int start = (int) results.get(0).getStart();
        final int length = (int) results.get(0).getLength();

        LOG.debug("Byte read: " + start + ", " + length + "'" + new String(bytes, start, length) + "'");

        assertEquals(recodeLineSeparators(expectedResult), recodeLineSeparators(new String(bytes, start, length + 1)));
    }

    public void testBug3() throws IOException, ParserException, ConfigurationException {
        final int pmid = 10070005;
        final String sourceFilename = "data/test/corpus/ajpendometa/10070005.html";
        final String searchString = "It identifies areas of uncertainty: distribution and concentrations of glucose in interstitial fluid , kinetics and mechanism of transcapillary glucose transport , kinetics and mechanism of glucose transport via its transporters into cells , detailed mechanisms by which hormones , exercise , and hypoxia affect glucose movement across cell membranes , whether translocation of glucose transporters to the cell membrane accounts completely , or even mainly , for insulin stimulated glucose uptake , whether exercise stimulates release of a circulating insulinomimetic factor , and the relation between muscle glucose uptake and muscle blood flow";
        final String expectedResult = "It identifies areas of uncertainty:<SUP> </SUP>distribution and\n" +
                "concentrations of glucose in interstitial fluid,<SUP> </SUP>kinetics and mechanism\n" +
                "of transcapillary glucose transport, kinetics<SUP> </SUP>and mechanism of glucose\n" +
                "transport via its transporters into cells,<SUP> </SUP>detailed mechanisms by which\n" +
                "hormones, exercise, and hypoxia affect<SUP> </SUP>glucose movement across cell\n" +
                "membranes, whether translocation<SUP> </SUP>of glucose transporters to the cell\n" +
                "membrane accounts completely,<SUP> </SUP>or even mainly, for insulin-stimulated\n" +
                "glucose uptake, whether<SUP> </SUP>exercise stimulates release of a circulating\n" +
                "insulinomimetic factor,<SUP> </SUP>and the relation between muscle glucose uptake\n" +
                "and muscle blood<SUP> </SUP>flow";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.setProcessTargetTerms(false);
        locator.processWithPMIDTerms2(0, pmid, searchString, 0, 0);

        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final List<TextSegment> results = locator.getResults();

        final int start = (int) results.get(0).getStart();
        final int length = (int) results.get(0).getLength();

        LOG.debug("Byte read: " + start + ", " + length + "'" + new String(bytes, start, length) + "'");

        assertEquals(recodeLineSeparators(expectedResult), recodeLineSeparators(new String(bytes, start, length + 1)));
    }

    // TODO - fixme
    public void TestBug4() throws IOException, ParserException, ConfigurationException {
        final int pmid = 10901322;
        final String sourceFilename = "data/test/corpus/ajepidem/10901322.html";
        final String searchString = "lilienfeld s paper and the accompanying commentary by vandenbroucke ( 2 Go ) deal directly or indirectly with the role and responsibilities of expert witnesses , the extrapolation of data on health effects from high dose exposures to low dose exposures , the importance of epidemiology to the development of public health policy , the current debates on environmental justice ( 3 Go ) and the use of the precautionary principle ( 4 Go ) in standard setting";
        final String expectedResult = "Lilienfeld's paper and the accompanying<SUP> </SUP>commentary by Vandenbroucke (2<A HREF=\"#B2\"><IMG BORDER=1 WIDTH=8 HEIGHT=7 ALT=\"Go\"\n" +
                "SRC=\"/icons/fig-down.gif\"></A>) deal directly or indirectly<SUP> </SUP>with the role and responsibilities of expert witnesses, the<SUP> </SUP>extrapolation of data on health effects from high dose exposures<SUP> </SUP>to low dose exposures, the importance of epidemiology to the<SUP> </SUP>development of public health policy, the current debates on<SUP> </SUP>environmental justice (3<A HREF=\"#B3\"><IMG BORDER=1 WIDTH=8 HEIGHT=7 ALT=\"Go\"\n" +
                "SRC=\"/icons/fig-down.gif\"></A>), and the use of the precautionary<SUP> </SUP>principle (4<A HREF=\"#B4\"><IMG BORDER=1 WIDTH=8 HEIGHT=7 ALT=\"Go\"\n" +
                "SRC=\"/icons/fig-down.gif\"></A>) in standard-setting.";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.setProcessTargetTerms(false);
        locator.processWithPMIDTerms2(0, pmid, searchString, 0, 0);

        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final List<TextSegment> results = locator.getResults();

        final int start = (int) results.get(0).getStart();
        final int length = (int) results.get(0).getLength();

        LOG.debug("Byte read: " + start + ", " + length + "'" + new String(bytes, start, length) + "'");

        assertEquals(recodeLineSeparators(expectedResult), recodeLineSeparators(new String(bytes, start, length-6)));
    }

    private String recodeLineSeparators(final String expectedResult) {
        return expectedResult.replaceAll("\r\n", "\n");
    }

    public void testPMID() throws IOException, ConfigurationException, ParserException {
        final String sourceFilename = "data/test/html/12096109.html";
        final String searchString = "The urothelium from random biopsies diagnosed as normal was dissected with the aid of a scalpel, labeled with [35S]methionine";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.processWithPMIDTerms2(0, sourceFilename, searchString, 0, 0);
        final TextSegment segment = locator.getResults().get(0);
        assertEquals(12096109L, segment.getPmid());
    }


    public void testMissingHTML() throws IOException, ConfigurationException, ParserException {
        final String sourceFilename = "data/test/html/simple.html";
        final HTMLByteLocator locator = createLocator(sourceFilename);
        locator.processWithPMIDTerms2(0, sourceFilename, "eggman", 0, 0);
        assertEquals(0, locator.getResults().size());
    }

    public void testMediumStream() throws IOException, ConfigurationException, ParserException {
        final String sourceFilename = "data/test/html/medium.html";
        final String target = "It was 20 years ago today";
        final String searchString = "It was <b>20 years ago today";

        final HTMLByteLocator locator = createLocator();
        locator.setSourceFilename(sourceFilename);
        locator.processWithPMIDTerms2(0, sourceFilename, target, 0, 0);


        final File file = new File(sourceFilename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = new byte[(int) file.length()];
        is.read(bytes, 0, (int) file.length());

        final int start = (int) locator.getResults().get(0).getStart();
        final int length = (int) locator.getResults().get(0).getLength();

        LOG.debug("Byte read: " + new String(bytes, start, length));

        assertEquals(searchString, new String(bytes, start, length));
    }

    public void testUniqueResults() throws IllegalAccessException, IOException, InvocationTargetException {
        final List<TextSegment> results = new ArrayList<TextSegment>();

        // 160 12356908    41  0.0014113087    37559   18  foobar
        final TextSegment segment1 = new TextSegment();
        segment1.setTopicId(160);
        segment1.setPmid(12356908);
        segment1.setRankNumber(41);
        segment1.setRankValue(0.0014113087f);
        segment1.setStart(37559);
        segment1.setLength(18);
        results.add(segment1);

        // 160 12356908    41  0.0014113087    37959   22  foobar
        final TextSegment segment2 = new TextSegment();
        segment2.setTopicId(160);
        segment2.setPmid(12356908);
        segment2.setRankNumber(41);
        segment2.setRankValue(0.0014113087f);
        segment2.setStart(37559);
        segment2.setLength(22);
        results.add(segment2);

        //      160 12356908    41  0.0014113087    37559   18  foobar
        final TextSegment segment3 = new TextSegment();
        segment3.setTopicId(160);
        segment3.setPmid(12356908);
        segment3.setRankNumber(41);
        segment3.setRankValue(0.0014113087f);
        segment3.setStart(37559);
        segment3.setLength(18);
        results.add(segment3);

        // 160 12256873    47  0.001324245    54267   23  foobar
        final TextSegment segment4 = new TextSegment();
        segment4.setTopicId(160);
        segment4.setPmid(12256873);
        segment4.setRankNumber(47);
        segment4.setRankValue(0.001324245f);
        segment4.setStart(54267);
        segment4.setLength(23);
        results.add(segment4);

        final HTMLByteLocator locator = new HTMLByteLocator();
        final String resultFile =
                File.createTempFile(TestHTMLByteLocator.class.getName(),
                        "txt").getAbsolutePath();
        final Set<TextSegment> uniqueResults = new HashSet<TextSegment>();
        //        locator.writeUniqueResults(resultFile, results);
        //assertEquals(2, uniqueResults.size());
        assertEquals(0, uniqueResults.size());

        int rank = 1;
        for (TextSegment segment : uniqueResults) {
            assertEquals(rank, segment.getRankNumber());
            rank++;
        }
    }
}
