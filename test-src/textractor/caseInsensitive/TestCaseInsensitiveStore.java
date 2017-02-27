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

import com.martiansoftware.jsap.JSAPException;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.tools.BuildDocumentIndexFromTextDocuments;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;

/**
 * Test the CaseInsensitiveStore class.
 * @author Kevin Dorff
 */
public class TestCaseInsensitiveStore extends TestCase {
    /** The logger. */
    private static final Log LOG =
        LogFactory.getLog(TestCaseInsensitiveStore.class);

    /** The CaseInsensitiveStore to using for this test. */
    private static CaseInsensitiveStore cis = null;

    /** Basename for the text index. */
    private static final String BASENAME = "index/caseinsensetive-test";

    /**
     * Required for JUnit.
     * @param name required name parameter
     */
    public TestCaseInsensitiveStore(final String name)
            throws Exception, IllegalAccessException,
            ConfigurationException, IOException, JSAPException,
            InvocationTargetException, ClassNotFoundException,
            InstantiationException, URISyntaxException {
        super(name);
        oneTimeSetUp();
    }

    /**
     * Search request with a word with upper case letters. Putting
     * letters in upper case forces case sensitivity, so,
     * the returned list should be empty.
     */
    public void testUpperCaseRequest() {
        final List<String> list = cis.suggest("HELLO");
        assertEquals("List for 'HELLO' size should be 0", 0, list.size());
    }

    /**
     * Search request with a word that is not in the index, so,
     * the returned list should be empty.
     */
    public void testNotInStoreRequest() {
        final List<String> list = cis.suggest("dogma");
        assertEquals("List for 'dogma' size should be 0", 0, list.size());
    }

    /**
     * Search request with a word that is too long and will already
     * be case insensitive, so, the returned list should be empty.
     */
    public void testLongWordRequest() {
        // The word is too long, should not be in list.
        final List<String> list = cis.suggest("estrogens");
        assertEquals("List for 'estrogens' size should be 0", 0, list.size());
    }

    /**
     * Search request with a word that is too short, so, the returned list
     * should be empty.
     */
    public void testShortWordRequest() {
        // The word is too long, should not be in list.
        final List<String> list = cis.suggest("a");
        assertEquals("List for 'a' size should be 0", 0, list.size());
    }

    /**
     * Search request with a word that should return normal results.
     */
    public void testProperRequest() {
        final List<String> list = cis.suggest("hello");
        assertEquals("List for 'hello' size should be 2", 2, list.size());
        assertTrue("Hello should be in list.", list.contains("HEllo"));
        assertTrue("HELLO should be in list.", list.contains("HELLO"));
    }

    /**
     * The lowercase version of the word is not in the list,
     * but, an upper case version is in the lists, there should
     * be results.
     */
    public void testWordText() {
        final List<String> list = cis.suggest("text");
        assertEquals("List for 'text' size should be 2", 2, list.size());
        assertTrue("TExt should be in list.", list.contains("TExt"));
        assertTrue("TEXT should be in list.", list.contains("TEXT"));
    }

    /**
     * The lowercase version is the only one in the list, so,
     * the returned list should be empty.
     */
    public void testWordThis() {
        final List<String> list = cis.suggest("this");
        assertEquals("List for 'this' size should be 0", 0, list.size());
    }

    /**
     * One time setup for all tests. This will create the
     * index/caseinsensetive-test index.
     * @throws IOException IOException
     * @throws ConfigurationException ConfigurationException
     */
    public static void oneTimeSetUp() throws Exception,
            NoSuchMethodException, ConfigurationException, IOException,
            JSAPException, InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        if (cis != null) {
            // We only want this method to run once and thus
            // the CaseInsensitiveStore will only be built
            // once for all tests.
            return;
        }

        LOG.debug("Running oneTimeSetUp");

        final BuildDocumentIndexFromTextDocuments indexBuilder =
            new BuildDocumentIndexFromTextDocuments(BASENAME);

        final List<CharSequence> textCollection = new Vector<CharSequence>();
        final String[] documents = {
                "HEllo, this is the TExt to index, first document",
                "second estrogens, Estrogens, "
                + "ESTROGENS estrogenic, document, in a string buffer",
                "third, hello a mutable string doccie",
                "Hello, document document document HELLO, hello, TEXT",
                "estrogenic, estrogenemia, estrogens, estrogenically, estrogen"
        };

        for (final String doc : documents) {
            textCollection.add(doc);
        }

        indexBuilder.index(documents);

        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);
        final CaseInsensitiveBuilder cib =
                new CaseInsensitiveBuilder(docmanager);
        cis = cib.build();
        cis.saveData(BASENAME);

        // Load it from disc...
        cis = new CaseInsensitiveStore(docmanager, BASENAME);
        cis.dumpStore(10);
    }
}
