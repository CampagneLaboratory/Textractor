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

package textractor.caseInsensitive;

import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Test the CaseInsensitiveStore class.
 * @author Kevin Dorff
 */
public class TestCaseInsensitiveStoreAmbiguity extends TestCase {
    /** The logger. */
    private static final Log LOG =
        LogFactory.getLog(TestCaseInsensitiveStoreAmbiguity.class);

    /** The CaseInsensitiveStore to using for this test. */
    private static CaseInsensitiveStore cis = null;

    /** Basename for the text index. */
    private static String basename  = "dataset-a-index/index";

    /**
     * Required for JUnit.
     * @param name required name parameter
     */
    public TestCaseInsensitiveStoreAmbiguity(final String name) throws
            ConfigurationException, TextractorDatabaseException, IOException,
            NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {

        super(name);
        oneTimeSetUp();
    }

    /**
     * Search for mab where all 5 varients are in the index.
     * @throws TextractorDatabaseException TextractorDatabaseException
     * @throws IOException IOException
     */
    public final void testMab() throws TextractorDatabaseException,
        IOException {

        final List<String> list = cis.suggest("mab");
        assertEquals("List for 'mab' size should be 5", 5, list.size());
        assertTrue("MAB should be in list.", list.contains("MAB"));
        assertTrue("MAb should be in list.", list.contains("MAb"));
        assertTrue("MAb should be in list.", list.contains("Mab"));
        assertTrue("mAB should be in list.", list.contains("mAB"));
        assertTrue("mAb should be in list.", list.contains("mAb"));
    }

    /**
     * Search for pca where only the three in the list are in the index
     * ("pca" itself is not in the index).
     * @throws TextractorDatabaseException TextractorDatabaseException
     * @throws IOException IOException
     */
    public final void testPca() throws TextractorDatabaseException,
        IOException {

        final List<String> list = cis.suggest("pca");
        assertEquals("List for 'pca' size should be 3", 3, list.size());
        assertTrue("PCA should be in list.", list.contains("PCA"));
        assertTrue("PCa should be in list.", list.contains("PCa"));
        assertTrue("pCa should be in list.", list.contains("pCa"));
    }

    /**
     * Search for "11-3h.". The term processor will strip off the
     * trailing dot so we are actually searching for "11-3h"
     * which should return one result, "11-3H".
     * @throws TextractorDatabaseException TextractorDatabaseException
     * @throws IOException IOException
     */
    public final void testTermProcessor() throws TextractorDatabaseException,
        IOException {

        final List<String> list = cis.suggest("3h");
        assertEquals("List for '3h' size should be 1", 1, list.size());
        assertTrue("3H should be in list.", list.contains("3H"));
    }

    /**
     * One time setup for all tests. This assumes the ambiguity
     * index has ALREADY been created.
     * @throws IOException IOException
     * @throws ConfigurationException ConfigurationException
     */
    public static void oneTimeSetUp() throws
            ConfigurationException, IOException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException,
            ClassNotFoundException, InstantiationException, URISyntaxException {

        if (cis != null) {
            // We only want this method to run once and thus
            // the CaseInsensitiveStore will only be built
            // once for all tests.
            return;
        }

        LOG.debug("Running oneTimeSetUp");

        final DocumentIndexManager docmanager =
                new DocumentIndexManager(basename);
        final CaseInsensitiveBuilder cib =
                new CaseInsensitiveBuilder(docmanager);
        cis = cib.build();
        cis.saveData(basename);

        // Load it from disc...
        cis = new CaseInsensitiveStore(docmanager, basename);
        cis.dumpStore(10);
    }
}
