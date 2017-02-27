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
import textractor.datamodel.Article;

import javax.jdo.JDOHelper;

/**
 *
 */
public class TestJDOProducer extends TestCase {
    /** Database manager to test with. */
    protected transient DbManager dbManager;

    @Override
    public void setUp() throws TextractorDatabaseException {
        dbManager = new DbManager();
    }

    @Override
    public void tearDown() {
        dbManager.shutdown();
    }

    public void testArticleProduction() throws TextractorDatabaseException {
        final TextractorManager textractorManager =
            dbManager.getTextractorManager();
        final JDOArticleProducer producer = new JDOArticleProducer(dbManager);
        producer.begin();
        final int originalCount = textractorManager.getArticleCount();
        final Article article = producer.createArticle();
        producer.end();

        assertTrue("Article should be persistent",
                JDOHelper.isPersistent(article));
        dbManager.beginTxn();
        final int newCount = textractorManager.getArticleCount();
        dbManager.commitTxn();
        assertEquals("No article created", originalCount + 1, newCount);
    }
}
