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

import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.didyoumean.DidYouMean;
import textractor.didyoumean.DidYouMeanI;
import textractor.scoredresult.ScoredResult;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Test case for TestAmbiguityDYM.
 */
public class TestAmbiguityDYM extends TestCase {
    private static final float SCORE_THRESHOLD = 0.03f;

    public void testDidYouMeanWithAmbiguitySet() throws ConfigurationException,
            IOException, TextractorDatabaseException, ParseException,
            ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException, URISyntaxException, QueryParserException, QueryBuilderVisitorException {
        final DocumentIndexManager docmanager =
            new DocumentIndexManager("dataset-a-index/index");
        final DidYouMeanI searchTool = new DidYouMean(docmanager);
        final List<ScoredResult> suggestions =
            searchTool.suggest("estrogen", SCORE_THRESHOLD);
        assertEquals("estrogens", suggestions.get(0).getTerm());
        assertEquals("estrogenic", suggestions.get(1).getTerm());
        assertEquals("oestrogen", suggestions.get(2).getTerm());
        assertEquals("estrous", suggestions.get(3).getTerm());
        assertEquals(4, suggestions.size());
    }
}
