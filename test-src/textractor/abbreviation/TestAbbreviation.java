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

package textractor.abbreviation;

import junit.framework.TestCase;
import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.impl.CatalogBase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.chain.indexer.Indexer;
import textractor.chain.producer.StringArrayProducer;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.docstore.DocumentStoreWriter;
import textractor.mg4j.io.ProteinWordSplitterReader;
import textractor.scoredresult.ScoredResult;
import textractor.tools.chain.ChainExecutor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Test case for TestAbbreviation.
 */
public class TestAbbreviation extends TestCase {
    private static final String BASENAME = "index/abbreviation-test";
    private DocumentIndexManager docmanager;
    private static final Log LOG = LogFactory.getLog(TestAbbreviation.class);

    @Override
    protected void setUp() throws Exception {
        final String[] documents = new String[] {
                "first document with some text: desert hedgehog others",
                "second document (which isn't even an abbreviation) with a short minor abbreviation (ka), or another abbreviation (na), abbreviation (ka)",
                "third document with a desert hedgehog (DHH)",
                "Or perhaps for another hedgehog (ANH)",
                "A single abbreviation occurrence (OCC)",
                "here is another reference to the desert hedgehog (DHH)",
                "here is an abbreviation of a protein kinase C (pKc)",
                "here is an (incorrect) abbreviation of a protein kinase C (p450)",
                "here is a dummy abbreviation of a protein kinase C (20)",
                "And one more about the desert hedgehog (DHH)",
                "A document containing two examples (TWO) or a short term examples (TWO) and a false examples (nothing to see here)",
                "A document containing only one examples (ONE)",
                "A document containing only one character (a)",
                "A document containing only one character (b)"
        };

        // create a simple loader
        final StringArrayProducer loader = new StringArrayProducer(documents);

        // add an indexer
        final Indexer indexer = new Indexer();
        indexer.setBasename(BASENAME);
        indexer.setWordReaderClass(ProteinWordSplitterReader.class.getName());
        indexer.setParenthesesAreWords(true);
        loader.addCommand(indexer);

        // and execute it
        final Catalog indexerCatalog = new CatalogBase();
        indexerCatalog.addCommand("TestAbbreviationIndexer", loader);

        final ChainExecutor executor = new ChainExecutor(indexerCatalog);
        executor.execute();

        docmanager = new DocumentIndexManager(BASENAME);
        final DocumentStoreWriter docStoreWriter = new DocumentStoreWriter(docmanager);
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));
        docStoreWriter.optimizeTermOrdering();

        for (int n = 0; n < documents.length; n++) {
            docStoreWriter.appendDocument(n, docmanager.extractTerms(documents[n]));
        }

        docStoreWriter.close();
    }

    @Override
    protected void tearDown() {
        docmanager.close();
    }

    public void testAbbreviationFilteringOfSingleLetters()
            throws ConfigurationException, IOException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException, TextractorDatabaseException {
        final Abbreviation abbreviation = new Abbreviation(docmanager);
        final String longForm = "character";
        final List<ScoredResult> results =
                abbreviation.findAbbreviationsForLongForm(longForm);
        assertEquals(0, results.size());
    }

    public void testAbbreviationsForLongForm() throws ConfigurationException,
            IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            TextractorDatabaseException {
        final Abbreviation abbreviation = new Abbreviation(docmanager);
        final String longForm = "Occurrence";
        final List<ScoredResult> results =
                abbreviation.findAbbreviationsForLongForm(longForm);

        if (LOG.isDebugEnabled()) {
            for (final ScoredResult result : results) {
                LOG.debug(longForm + ": " + result.getTerm());
            }
        }

        assertEquals(1, results.size());
        assertEquals("OCC", results.get(0).getTerm());
    }

    public void testAbbreviationsWithFilteredNumbers()
            throws ConfigurationException, IOException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException, TextractorDatabaseException {
        final Abbreviation abbreviation = new Abbreviation(docmanager);
        final String longForm = "protein kinase C";
        final List<ScoredResult> results =
                abbreviation.findAbbreviationsForLongForm(longForm);

        if (LOG.isDebugEnabled()) {
            for (final ScoredResult result : results) {
                LOG.debug(longForm + ": " + result.getTerm());
            }
        }
        assertEquals(2, results.size());
        assertEquals("p450", results.get(0).getTerm());
        assertEquals("pKc", results.get(1).getTerm());
    }

    public void testAbbreviationsForMultipleOccurancesInSameDocument()
            throws ConfigurationException, IOException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException, TextractorDatabaseException {
        final Abbreviation abbreviation = new Abbreviation(docmanager);
        final String longForm = "abbreviation";
        final List<ScoredResult> results =
                abbreviation.findAbbreviationsForLongForm(longForm);

        if (LOG.isDebugEnabled()) {
            for (final ScoredResult result : results) {
                LOG.debug(longForm + ": " + result.getTerm());
            }
        }

        assertEquals(2, results.size());
        assertEquals("ka", results.get(0).getTerm());
        assertEquals("na", results.get(1).getTerm());

    }

    public void testAbbreviationsForMultipleQueryTerms()
            throws ConfigurationException, IOException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException, TextractorDatabaseException {
        final Abbreviation abbreviation = new Abbreviation(docmanager);
        final String longForm = "desert hedgehog";
        final List<ScoredResult> results =
                abbreviation.findAbbreviationsForLongForm(longForm);

        if (LOG.isDebugEnabled()) {
            for (final ScoredResult result : results) {
                LOG.debug(longForm + ": " + result.getTerm());
            }
        }

        assertEquals(1, results.size());
        assertEquals("DHH", results.get(0).getTerm());

    }

    public void testNoAbbreviations() throws ConfigurationException,
            IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            TextractorDatabaseException {
        final Abbreviation abbreviation = new Abbreviation(docmanager);
        final String longForm = "not found";
        final List<ScoredResult> results =
                abbreviation.findAbbreviationsForLongForm(longForm);

        if (LOG.isDebugEnabled()) {
            for (final ScoredResult result : results) {
                LOG.debug(longForm + ": " + result.getTerm());
            }
        }

        assertEquals(0, results.size());
    }

    public void testSortedByFrequency() throws ConfigurationException,
            IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            TextractorDatabaseException {
        final Abbreviation abbreviation = new Abbreviation(docmanager);
        final String longForm = "examples";
        final List<ScoredResult> results =
                abbreviation.findAbbreviationsForLongForm(longForm);

        if (LOG.isDebugEnabled()) {
            for (final ScoredResult result : results) {
                LOG.debug(longForm + ": " + result.getTerm());
            }
        }

        assertEquals(2, results.size());
        assertEquals("TWO", results.get(0).getTerm());
        assertEquals("ONE", results.get(1).getTerm());
    }
}
