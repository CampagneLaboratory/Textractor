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

package textractor.ambiguity;

import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.tools.expansion.ClipExpansions;
import textractor.tools.expansion.CollectExpansions;
import textractor.tools.expansion.ExpansionTerm;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestAmbiguityClipExpansions extends TestCase {
    private static final Log LOG =
            LogFactory.getLog(TestAmbiguityClipExpansions.class);
    private static final String BASENAME = "dataset-a-index/index";
    private static final String OUTPUT_FILENAME =
            "test-results/test-ambiguity-expansions.xml";

    private void setupExpansionCollector(final CollectExpansions collector,
                                         final String indexBasename) {
        collector.setAcronymsListFileName("data/testData/TestExpansions.txt");
        collector.setOutputFilename(OUTPUT_FILENAME);
        collector.setExtendOnLeft(true);
        collector.setMinimalSupport(1);
        collector.setRejectFilename("test-results/test-ambiguity-rejections.txt");
        collector.setBasename(indexBasename);
        collector.setTemplate("( acronym )");
        collector.setUseDocStore(true);
        collector.setVerifyExpansions(false);
        collector.setPercentThreshold(10.0f);
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

    public void testExpansionClipping()
            throws ConfigurationException, IOException, SAXException,
            TextractorDatabaseException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException,
            ClassNotFoundException, InstantiationException, URISyntaxException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);
        final CollectExpansions collector = new CollectExpansions();
        setupExpansionCollector(collector, BASENAME);
        collector.setMinimalSupport(10);
        collector.performCollection();

        final ClipExpansions clipper = new ClipExpansions(0.5f);
        clipper.loadExpansions(OUTPUT_FILENAME);
        final Map<String, Set<ExpansionTerm>> clippedAcronyms =
                clipper.clip(docmanager);

        assertNotNull("Clipped acronyms should not be null", clippedAcronyms);
        final Set<ExpansionTerm> apcTerms = clippedAcronyms.get("APC");
        assertNotNull("APC terms should not be null", apcTerms);

        // get the actual list of term strings
        final List<String> actualApcTerms = new ArrayList<String>(apcTerms.size());
        for (final ExpansionTerm expansionTerm : apcTerms) {
            final String term = expansionTerm.getTerm();
            LOG.debug("adding " + term);
            actualApcTerms.add(term);
        }

        // and compare them to the expected
        final String[] expectedApcTerms = {
            "antigen presenting cells",
            "activated protein C",
            "adenomatous polyposis coli"
        };

        int count = 0;
        for (final String term : expectedApcTerms) {
            assertTrue("(" + count + ") " + term, actualApcTerms.contains(term));
            count++;
        }

// TODO - current code generates too many clipped terms...
//        assertEquals("Actual number of clipped terms does not match expected",
//                expectedApcTerms, count);

        echoPositionMap(clipper.getPositionCounts());
    }
}
