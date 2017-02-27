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
import textractor.abbreviation.Abbreviation;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.scoredresult.ScoredResult;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Test case for TestAmbiguityAbbreviation.
 */
public class TestAmbiguityAbbreviation extends TestCase {
    public void testAbbreviationSearchTNT() throws ConfigurationException,
            IOException, TextractorDatabaseException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager("dataset-a-index/index");
        final Abbreviation abbreviation = new Abbreviation(docmanager);
        final List<ScoredResult> results =
                abbreviation.findAbbreviationsForLongForm("trinitrotoluene");
        assertEquals(1, results.size());
        assertEquals("TNT", results.get(0).getTerm());
        assertEquals(350.0, results.get(0).getScore());
    }

    public void testAbbreviationSearchMAPK() throws ConfigurationException,
            IOException, TextractorDatabaseException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager("dataset-a-index/index");
        final Abbreviation abbreviation = new Abbreviation(docmanager);
        final List<ScoredResult> results =
                abbreviation.findAbbreviationsForLongForm("mitogen activated protein kinase");

        assertEquals(3, results.size());
        assertEquals("MAPK", results.get(0).getTerm());
        assertEquals("p38mapk", results.get(1).getTerm());
        assertEquals("apmapk", results.get(2).getTerm());
        assertEquals("MAPK", 12.0, results.get(0).getScore());
        assertEquals("p38mapk", 1.0, results.get(1).getScore());
        assertEquals("apmapk", 1.0, results.get(2).getScore());
    }
}
