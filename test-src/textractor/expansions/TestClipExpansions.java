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

package textractor.expansions;

import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.xml.sax.SAXException;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.docstore.DocumentStoreWriter;
import textractor.test.util.AssertFilesEqual;
import textractor.tools.BuildDocumentIndexFromTextDocuments;
import textractor.tools.expansion.ClipExpansions;
import textractor.tools.expansion.ClippedExpansionXMLWriter;
import textractor.tools.expansion.CollectExpansions;
import textractor.tools.expansion.ExpansionTerm;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Test case for {@link textractor.tools.expansion.ClipExpansions}.
 */
public final class TestClipExpansions extends TestCase {
    private static final String BASENAME = "index/expansion-test";
    private static final String OUTPUT_FILENAME =
            "test-results/test-expansions.xml";

    private DocumentIndexManager docmanager;
    private static final Log LOG = LogFactory.getLog(TestClipExpansions.class);

    @Override
    protected void tearDown() {
        docmanager.close();
    }

    @Override
    protected void setUp() throws Exception {
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(BASENAME);
        final String[] documents = {
                "first document with some text: desert hedgehog (DHH)",
                "second document with info about the desert hedgehog (DHH)",
                "the the the about, text:, text:, text:, text:, the, the, the, text:"
        };

        indexBuilder.index(documents);

        docmanager = new DocumentIndexManager(BASENAME);
        final DocumentStoreWriter docStoreWriter =
                new DocumentStoreWriter(docmanager);
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));
        docStoreWriter.optimizeTermOrdering();

        for (int n = 0; n < documents.length; n++) {
            docStoreWriter.appendDocument(n,
                    docmanager.extractTerms(documents[n]));
        }

        docStoreWriter.close();
        docmanager.close();
    }

    private void setupExpansionCollector(final CollectExpansions collector,
                                         final String indexBasename) {
        collector.setAcronymsListFileName("data/testData/TestExpansions.txt");
        collector.setOutputFilename(OUTPUT_FILENAME);
        collector.setExtendOnLeft(true);
        collector.setMinimalSupport(1);
        collector.setRejectFilename("test-results/test-rejections.txt");
        collector.setBasename(indexBasename);
        collector.setTemplate("( acronym )");
        collector.setUseDocStore(true);
        collector.setVerifyExpansions(false);
    }

    public void testExpansionClipping() throws IllegalAccessException,
            NoSuchMethodException, ConfigurationException, IOException,
            SAXException, InvocationTargetException, ClassNotFoundException,
            InstantiationException, URISyntaxException, TextractorDatabaseException {
        final CollectExpansions collector = new CollectExpansions();
        setupExpansionCollector(collector, BASENAME);
        collector.performCollection();

        final ClipExpansions clipper = new ClipExpansions(0.2f);
        clipper.loadExpansions(OUTPUT_FILENAME);
        final Map<String, Set<ExpansionTerm>> clippedAcronyms =
                clipper.clip(docmanager);

        assertNotNull(clippedAcronyms);
        assertEquals(1, clippedAcronyms.get("DHH").size());
        final ArrayList<String> expected = new ArrayList<String>();
        expected.add("desert hedgehog");
        final Iterator<ExpansionTerm> iteratedResults =
                clippedAcronyms.get("DHH").iterator();
        int count = 0;
        while (iteratedResults.hasNext()) {
            assertEquals(expected.get(count), iteratedResults.next().getTerm());
            count++;
        }
    }

    public void testExpansionClippingPositionCounts()
            throws ConfigurationException, IOException, SAXException,
            TextractorDatabaseException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException,
            ClassNotFoundException, InstantiationException, URISyntaxException {
        final CollectExpansions collector = new CollectExpansions();
        setupExpansionCollector(collector, BASENAME);
        collector.performCollection();

        final ClipExpansions clipper = new ClipExpansions(0.2f);
        clipper.loadExpansions(OUTPUT_FILENAME);
        clipper.clip(docmanager);

        assertNotNull(clipper.getPositionCounts());
        echoPositionMap(clipper.getPositionCounts());
        final List<ExpansionTerm>[] expansionMap = clipper.getPositionCounts();

        assertEquals("the", expansionMap[0].get(0).getTerm());
        assertEquals(1L, expansionMap[0].get(0).getFrequency());

        assertEquals("text:", expansionMap[0].get(1).getTerm());
        assertEquals(1L, expansionMap[0].get(1).getFrequency());

        assertEquals("desert", expansionMap[1].get(0).getTerm());
        assertEquals(2L, expansionMap[1].get(0).getFrequency());

        assertEquals("hedgehog", expansionMap[2].get(0).getTerm());
        assertEquals(2L, expansionMap[2].get(0).getFrequency());

    }

    public void testExpansionWriter() throws ConfigurationException,
            IOException, SAXException, TextractorDatabaseException,
            MarshalException, ValidationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException,
            ClassNotFoundException, InstantiationException, URISyntaxException {
        final CollectExpansions collector = new CollectExpansions();
        setupExpansionCollector(collector, BASENAME);
        collector.performCollection();

        final ClipExpansions clipper = new ClipExpansions(0.2f);
        clipper.loadExpansions(OUTPUT_FILENAME);
        final Map<String, Set<ExpansionTerm>> clippedAcronyms =
                clipper.clip(docmanager);
        final ClippedExpansionXMLWriter xmlWriter =
                new ClippedExpansionXMLWriter("test-results/test-expansion-writer.xml");
        xmlWriter.writeXML(clippedAcronyms);
        AssertFilesEqual.assertEquals(new File("data/testData/expected/clipped-expansions.xml"), new File("test-results/test-expansion-writer.xml"));

    }

    /**
     * Tests the expansion clipping with output from a Medline expansion
     * collection found in test/expansions/output/expansions.xml.
     *
     * @throws IOException
     * @throws SAXException
     */
    public void testExpansionClippingWithLargeExpansionSet()
            throws IOException, SAXException {
        final ClipExpansions clipper = new ClipExpansions(0.5f);
        clipper.loadExpansions("data/testData/expansions/expansions.xml");
        clipper.clip(docmanager);
        final List<ExpansionTerm>[] positionMapArray =
                clipper.getPositionCounts();
        echoPositionMap(positionMapArray);
        assertEquals(5, positionMapArray.length);
        assertEquals(475, positionMapArray[0].size());
        assertEquals(296, positionMapArray[1].size());
        assertEquals(185, positionMapArray[2].size());
        assertEquals(136, positionMapArray[3].size());
        assertEquals(75, positionMapArray[4].size());
    }

    private void echoPositionMap(final List<ExpansionTerm>[] positionMapArray) {
        if (LOG.isDebugEnabled()) {
            for (int n = 0; n < positionMapArray.length; n++) {
                for (final ExpansionTerm expansionTerm : positionMapArray[n]) {
                        LOG.debug(n + ": " + expansionTerm.getTerm()
                                + " (" + expansionTerm.getFrequency() + ")");
                }
            }
        }
    }
}
