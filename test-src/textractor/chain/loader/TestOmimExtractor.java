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

import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.BulletParser;
import it.unimi.dsi.mg4j.util.parser.ParsingFactory;
import it.unimi.dsi.mg4j.util.parser.WellFormedXmlFactory;
import junit.framework.TestCase;
import textractor.datamodel.Article;
import textractor.datamodel.OmimArticle;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

/**
 * Test the OmimExtractor.
 * @author Kevin Dorff (Sep 18, 2007)
 */
public class TestOmimExtractor extends TestCase implements ParsedArticleHandler {

    private OmimArticle disease;
    private MutableString abstractText;
    char[] fileBuffer;
    int fileSize;
    BulletParser parser;
    OmimExtractor omimExtractor;

    protected void setUp() {
        disease = null;
        abstractText = null;
        fileBuffer = null;
        fileSize = 0;

        final ParsingFactory factory = new WellFormedXmlFactory();
        parser = new BulletParser(factory);
        omimExtractor = new OmimExtractor(this);
        parser.setCallback(omimExtractor);
    }

    public void testParseXmlNormal() throws IOException {
        // read the file and parse it
        String filename = "data/testData/omim-test/omimlike.xml";
        readFile(filename);
        omimExtractor.resetExtractor(filename);
        parser.parse(fileBuffer, 0, fileSize);

        // Verify the parsed object
        assertEquals(123456, disease.getPmid());
        assertEquals("Some title entry", disease.getTitle());
        assertEquals("TEXT. The first mim text entry. CLINICAL FEATURES. " +
                "Some text about climical features ", abstractText.toString());
        assertEquals("omimlike.xml", disease.getFilename());
        checkStringList(disease.getAliases(), "ALIAS1", "ALIAS2", "ALIAS3", "ALIAS4");
        checkIntList(disease.getRefPmids(), 329392, 302340);
    }

    public void testParseXmlNoAbstract() throws IOException {
        // read the file and parse it
        String filename = "data/testData/omim-test/omimlike.xml";
        readFile(filename);
        omimExtractor.setSkipAbstracts(true);
        omimExtractor.resetExtractor(filename);
        parser.parse(fileBuffer, 0, fileSize);

        // Verify the parsed object
        assertEquals(123456, disease.getPmid());
        assertEquals("Some title entry", disease.getTitle());
        assertEquals("", abstractText.toString());
        assertEquals("omimlike.xml", disease.getFilename());
        checkStringList(disease.getAliases(), "ALIAS1", "ALIAS2", "ALIAS3", "ALIAS4");
        checkIntList(disease.getRefPmids(), 329392, 302340);
    }

    public void testParseXmlBadType() throws IOException {
        // read the file and parse it
        String filename = "data/testData/omim-test/omimlike-2.xml";
        readFile(filename);
        omimExtractor.setSkipAbstracts(true);
        omimExtractor.resetExtractor(filename);
        parser.parse(fileBuffer, 0, fileSize);

        // Verify the parsed object
        assertNull(disease);
    }

    private void readFile(final String filename)  throws IOException {
        InputStream stream;
        if (filename.endsWith(".gz") || filename.endsWith(".zip")) {
            stream = new GZIPInputStream(new FileInputStream(filename));
        } else {
            stream = new FileInputStream(filename);
        }

        FastBufferedReader reader = new FastBufferedReader(
                new InputStreamReader(stream, "UTF-8"));

        fileBuffer = new char[10000];

        // read the whole file in memory:
        int length;
        fileSize = 0;

        while ((length = reader.read(fileBuffer, fileSize, fileBuffer.length - fileSize)) > 0) {
            fileSize += length;
            fileBuffer = CharArrays.grow(fileBuffer, fileSize + 1);
        }

        // DO NOT TRIM the fileBuffer. Triming allocates a new fileBuffer and copies the
        // result in the new one. This does in fact
        // use more memory transiently and result in more garbage collection.

        // and close up stuff we don't need anymore
        reader.close();
        stream.close();
    }

    public void articleParsed(Article article, MutableString abstractText) {
        this.disease = (OmimArticle)article;
        this.abstractText = abstractText;
    }

    private void checkStringList(final Collection<String> haveVals, final String... expVals) {
        assertEquals(expVals.length, haveVals.size());
        for (String expVal : expVals) {
            assertTrue(haveVals.contains(expVal));
        }
    }

    private void checkIntList(final IntList haveVals, final int... expVals) {
        assertEquals(expVals.length, haveVals.size());
        for (int expVal : expVals) {
            assertTrue(haveVals.contains(expVal));
        }
    }

}
