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

package textractor.parsers.extractor;

import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.tools.BuildDocumentIndexFromTextDocuments;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the Token Manager for Textractor documents.
 * @author Fabien Campagne
 * Date: Nov 16, 2005
 * Time: 1:56:49 PM
 */
public class TestDocumentTokenManager extends TestCase {
    private static final String BASENAME = "index/doc-token-manager";
    private static final String SECOND_DOCUMENT = "Blah blah, Diseases";
    private static final String THIRD_DOCUMENT =
            "Diseases such as Alzheimer's disease are common in PubMed.";
    private static final String FOURTH_DOCUMENT =
            "Alzheimer's disease and parkinson's or cancer are common in PubMed.";
    private static final String FIFTH_DOCUMENT =
            "Blah blah, diseases (e.g., Alzheimer's, cancer, parkinson's) are common in PubMed.";

    public TestDocumentTokenManager() throws Exception {
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(BASENAME);
        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add("Hello, this is the (text to index), XYA XYB");
        textCollection.add(SECOND_DOCUMENT);
        textCollection.add(THIRD_DOCUMENT);
        textCollection.add(FOURTH_DOCUMENT);
        textCollection.add(FIFTH_DOCUMENT);
        indexBuilder.index(textCollection);
    }

    public void testDocumentTokens() throws NoSuchMethodException,
            IllegalAccessException, ConfigurationException, IOException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);

        final int[] doc2 = docmanager.extractTerms(SECOND_DOCUMENT);
        final DocumentTokenManager dtm =
                new DocumentTokenManager(doc2, docmanager);

        Token token = dtm.getNextToken();
        assertEquals(ExtractionEngine.WORD, token.kind);
        assertEquals("blah", token.image);

        token = dtm.getNextToken();
        assertEquals(ExtractionEngine.WORD, token.kind);
        assertEquals("blah", token.image);

        token = dtm.getNextToken();
        assertNotSame(ExtractionEngine.WORD, token.kind);
        assertNotSame(ExtractionEngine.EOF, token.kind);
        assertEquals(",", token.image);

        token = dtm.getNextToken();
        assertNotSame(ExtractionEngine.WORD, token.kind);
        assertNotSame(ExtractionEngine.EOF, token.kind);
        assertEquals("diseases", token.image);

        token = dtm.getNextToken();
        assertEquals(ExtractionEngine.EOF, token.kind);
        assertEquals("<EOF>", token.image);
    }

    public void testParseDiseases() throws ConfigurationException, IOException,
            ParseException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager = new DocumentIndexManager(BASENAME);
        final int[] doc3 = docmanager.extractTerms(THIRD_DOCUMENT);
        final DocumentTokenManager dtm =
                new DocumentTokenManager(doc3, docmanager);
        final ExtractionEngine parser = new ExtractionEngine(dtm);
        final List<String> diseases = new ArrayList<String>();
        parser.diseasesSuchAs(diseases);
        assertTrue(diseases.contains("alzheimer s disease are common in pubmed"));
    }

    public void testEnumeration() throws ParseException, ConfigurationException,
            IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);
        final int[] doc4 = docmanager.extractTerms(FOURTH_DOCUMENT);
        final DocumentTokenManager dtm = new DocumentTokenManager(doc4, docmanager);
        ExtractionEngine parser = new ExtractionEngine(dtm);
        parser.ReInit(new DocumentTokenManager(doc4, docmanager));
        final List<String> diseases = new ArrayList<String>();
        diseases.clear();
        parser.enumeration(diseases);
        assertEquals("alzheimer s disease", diseases.get(0));
        assertEquals("parkinson s", diseases.get(1));
        assertEquals("cancer are common in pubmed", diseases.get(2));
    }

    public void testDiseasesE_g() throws ParseException, ConfigurationException,
            IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager = new DocumentIndexManager(BASENAME);
        final int[] doc5 = docmanager.extractTerms(FIFTH_DOCUMENT);
        final DocumentTokenManager dtm =
                new DocumentTokenManager(doc5, docmanager);
        final ExtractionEngine parser = new ExtractionEngine(dtm);
        parser.ReInit(new DocumentTokenManager(doc5, docmanager, 3)); // 3 is the index of "diseases"
        final List<String> diseases = new ArrayList<String>();
        diseases.clear();
        parser.diseasesE_g(diseases);
        assertEquals("alzheimer s", diseases.get(0));
        assertEquals("cancer", diseases.get(1));
        assertEquals("parkinson s", diseases.get(2));
    }

    public void testBeginEnd() throws ParseException, ConfigurationException,
            IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager = new DocumentIndexManager(BASENAME);
        final int[] doc5 = docmanager.extractTerms(FIFTH_DOCUMENT);
        final DocumentTokenManager dtm =
                new DocumentTokenManager(doc5, docmanager);
        final ExtractionEngine parser = new ExtractionEngine(dtm);
        parser.ReInit(new DocumentTokenManager(doc5, docmanager, 7,9)); // 3 is the index of "diseases"
        // 8 the index of Alzheimer

        // we will not extract the "s"
        final List<String> diseases = new ArrayList<String>();
        diseases.clear();
        parser.enumeration(diseases);
        assertEquals("alzheimer s", diseases.get(0));
        assertEquals(1, diseases.size());
    }

}
