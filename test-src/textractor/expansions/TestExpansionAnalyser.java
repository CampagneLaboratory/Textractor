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
import org.xml.sax.SAXException;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.docstore.DocumentStoreWriter;
import textractor.tools.BuildDocumentIndexFromTextDocuments;
import textractor.tools.expansion.CollectExpansions;
import textractor.tools.expansion.ExpansionAnalyser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Test case for TestExpansionAnalyser.
 */

public class TestExpansionAnalyser extends TestCase {
    private static final String BASENAME = "index/expansion-test";
    private static final String OUTPUT_FILENAME =
        "test-results/test-expansions.xml";

    @Override
    protected void setUp() throws Exception {
        final BuildDocumentIndexFromTextDocuments indexBuilder =
            new BuildDocumentIndexFromTextDocuments(BASENAME);
        final String[] documents = {
                "first document with some text: desert hedgehog (DHH)",
                "second document with info about the desert hedgehog (DHH)",
                "the the the about, text:, text:, text:, text:, the, the, the, text:",
        };

        indexBuilder.index(documents);

        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);
        final DocumentStoreWriter docStoreWriter =
                new DocumentStoreWriter(docmanager);
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));
        docStoreWriter.optimizeTermOrdering();

        for (int n = 0; n < documents.length; n++) {
            docStoreWriter.appendDocument(n, docmanager.extractTerms(documents[n]));
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

    public void testBasicAnalysis() throws ConfigurationException, IOException,
            SAXException, IllegalAccessException, NoSuchMethodException,
            TextractorDatabaseException, InvocationTargetException,
            ClassNotFoundException, InstantiationException, URISyntaxException {
        final CollectExpansions collector = new CollectExpansions();
        setupExpansionCollector(collector, BASENAME);
        collector.performCollection();

        final ExpansionAnalyser analyser =
                new ExpansionAnalyser(OUTPUT_FILENAME);
        analyser.echoBasicStatistics();

        assertEquals(1, analyser.getExpandedAcronymCount());
        assertEquals(2, analyser.getLongFormCollectionCount());
    }
}
