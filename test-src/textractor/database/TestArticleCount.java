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

package textractor.database;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.datamodel.Article;
import textractor.datamodel.TermOccurrence;

/**
 * Tests ArticleTermCount. Adding the test before refactoring the tool to work
 * with terms that are n-grams.
 *
 * @author lshi
 */
public class TestArticleCount extends TestCase {
    private static final Log LOG = LogFactory.getLog(TestArticleCount.class);
    DbManager dbm;
    TextractorManager tm;

    public TestArticleCount(final String name) throws TextractorDatabaseException {
        super(name);
        dbm = new DbManager();
    }

    @Override
    protected void setUp() {
        dbm.beginTxn();
        tm = dbm.getTextractorManager();
    }

    @Override
    protected void tearDown() {
        dbm.commitTxn();
    }

    /**
     * Checks that ArticleTermCount counted correctly for article 7896799 and
     * the most frequent terms.
     */
    public void testBoot7896799() {
        final String[] expectedTerms = {
                "dAK", "dGK"
        };

        final int[] expectedTermCount = {
                67, 56
        };

        final Article article = tm.getArticleByPmid(7896799);
        checkArticleCounts(article, expectedTerms, expectedTermCount);
    }

    /**
     * Checks that ArticleTermCount counted correctly for article 12507430 and
     * the most frequent terms.
     */
    public void testBoot12507430() {
        final String[] expectedTerms = {
                "HAUSP", "ubal", "ubiquitin", "p53"
        };

        final int[] expectedTermCount = {
                151, 70, 57, 50
        };
        final Article article = tm.getArticleByPmid(12507430);
        checkArticleCounts(article, expectedTerms, expectedTermCount);
    }

    /**
     * Checks that ArticleTermCount counted correctly for article 10788455 and
     * the most frequent terms.
     */
    public void testBoot10788455() {
        final String[] expectedTerms = {
                "R82A", "G231C", "proton", "R82A G231C", "proton release"
        };

        final int[] expectedTermCount = {
                176, 101, 99, 71, 44
        };

        final Article article = tm.getArticleByPmid(10788455);
        checkArticleCounts(article, expectedTerms, expectedTermCount);
    }

    private void checkArticleCounts(final Article article,
            final String[] expectedTerms, final int[] expectedTermCount) {
        final TermOccurrence[] termOccurrences =
            article.getMostFrequentTerms(expectedTerms.length);
        for (int i = 0; i < termOccurrences.length; i++) {
            final TermOccurrence termOccurrence = termOccurrences[i];

            if (LOG.isDebugEnabled()) {
                LOG.debug("Term: " + termOccurrence.getTerm());
                LOG.debug("Term frequency: " + termOccurrence.getCount());
            }

            final String pmid = Long.toString(article.getPmid());
            assertEquals(pmid, expectedTerms[i], termOccurrence.getTerm());
            assertEquals(pmid, expectedTermCount[i], termOccurrence.getCount());
        }
    }
}
