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

package textractor.tools.lookup;

import com.martiansoftware.jsap.JSAPException;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.LookupResult;
import textractor.learning.AnnotationFormatWriter;
import textractor.test.util.AnnotationFile;
import textractor.test.util.AssertFilesEqual;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

public final class TestLookupTerm extends TestCase {
    /** Database manager used for this test case. */
    private DbManager dbm;

    public TestLookupTerm(final String name) throws TextractorDatabaseException {
        super(name);
        dbm = new DbManager();
    }

    /**
     * Set up the test by initializing the database manager.
     */
    @Override
    protected void setUp() {
        dbm.beginTxn();
    }

    @Override
    protected void tearDown() {
        dbm.commitTxn();
    }

    /**
     * Validates the functionality of the "standalone" lookup.
     *
     * @throws IOException
     * @throws ConfigurationException
     */
    public void TestLookupStandalone() throws ConfigurationException,
            IOException, NoSuchMethodException, IllegalAccessException,
            JSAPException, InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final AbstractLookupProteinname lpns =
                new LookupProteinnameStandalone();
        final String dictionaryFilename = "dictionary/currentDictionary";
        lpns.setDictionary(dictionaryFilename);

        lookup("12646870", lpns);
        lookup("12629548", lpns);
        lookup("150636", lpns);
        lookup("207007", lpns);
        lookup("12496294", lpns);
    }

    private void lookup(final String id, final AbstractLookupProteinname lpns)
            throws IllegalAccessException, NoSuchMethodException,
            ConfigurationException, IOException, JSAPException,
            InvocationTargetException, ClassNotFoundException,
            InstantiationException, URISyntaxException {
        final String inputFilename = "data/testData/" + id + ".html";
        final String outputFilename = "test-results/" + id + ".hit";
        final String expectedOutputFilename =
            "data/testData/expected_" + id + ".hit";

        final String[] input = {
                "-i", inputFilename, "-o", outputFilename
        };
        lpns.process(input);

        final File expectedOutputFile = new File(expectedOutputFilename);
        final File outputFile = new File(outputFilename);

        AssertFilesEqual.assertEquals("lookup id = " + id,
                expectedOutputFile, outputFile);
    }

    public void testLookupMultipleWordTerm() throws IOException,
            TextractorDatabaseException, ConfigurationException,
            NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final String indexBasename = dbm.getTextractorManager().getInfo()
                .getCaseSensitiveIndexBasename();
        final String dictionaryFilename =
            "../data/testData/proteinname_dictionary.txt";
        final String outputFilename = "test-results/lookupResults.out";
        final String expectedOutputFilename =
            "data/testData/expectedLookupResults.out";
        final DocumentIndexManager docmanager =
            new DocumentIndexManager(indexBasename);
        final AnnotationFormatWriter writer =
            new AnnotationFormatWriter(docmanager,
                    new FileWriter(outputFilename), true);

        // iterate through results and create annotations
        final LookupProteinname lpn =
            new LookupProteinname(dictionaryFilename);

        // lookup each term of dictionary in index
        final Collection<LookupResult> results =
            lpn.lookupAllTermsByTerm(indexBasename);
        lpn.setExportAnnotations(true);
        lpn.postLookupProcess(dbm, writer, results);
        writer.flush();

        AnnotationFile.compare(expectedOutputFilename, outputFilename);
        docmanager.close();
        lpn.close();
    }

    /**
     * Validates calculatePartial method of LookupProteinnameStandalone.
     */
    public void testCalculatePartial() throws IOException,
            ConfigurationException {
        // build a term map based on the following terms
        // "a", "a b", "c a", "c a b", "a b c a"
        final Map<String, Integer> terms = new Object2IntOpenHashMap<String>();
        terms.put("a", 5);
        terms.put("a b", 3);
        terms.put("a b c", 1);
        terms.put("c a", 2);
        terms.put("c a b", 1);

        final Object2IntOpenHashMap<String> partialTerms =
            new Object2IntOpenHashMap<String>();

        final LookupProteinnameStandalone lpns =
                new LookupProteinnameStandalone();
        lpns.calculatePartial(terms, partialTerms);

        // why is "c a b" not in the partial terms?
        assertEquals(4, partialTerms.size());
        assertEquals(5, partialTerms.getInt("a"));
        assertEquals(2, partialTerms.getInt("a b"));
        assertEquals(1, partialTerms.getInt("c a"));
        assertEquals(0, partialTerms.getInt("a b c"));
    }
}
