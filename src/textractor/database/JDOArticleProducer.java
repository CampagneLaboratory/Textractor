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

import textractor.TransactionalProcessor;
import textractor.article.ArticleProducer;
import textractor.datamodel.Article;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link ArticleProducer} that produces persistent articles.
 */
public final class JDOArticleProducer implements ArticleProducer,
        TransactionalProcessor {
    /** Database manager to produce with. */
    private final transient DbManager dbManager;

    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /**
     * Create a new {@link ArticleProducer}.
     * @throws TextractorDatabaseException if there is a problem accessing the
     * database
     */
    public JDOArticleProducer() throws TextractorDatabaseException {
        this(new DbManager());
    }

    /**
     * Create a new {@link ArticleProducer}.
     * @param dbManager The {@link DbManager} to use for the database
     * @throws TextractorDatabaseException if there is a problem accessing the
     * database
     */
    public JDOArticleProducer(final DbManager dbManager) {
        super();
        this.dbManager = dbManager;
    }

    /**
     * Create a new persistent {@link textractor.datamodel.Article}.  Must be
     * called within a {@link #begin()} and {@link #end()} block.
     * @return A new persistent article.
     */
    public Article createArticle() {
        final Article article = new Article();
        final TextractorManager textractorManager =
            dbManager.getTextractorManager();
        article.setArticleNumber(textractorManager.getNextArticleNumber());
        dbManager.makePersistent(article);
        return article;
    }

    /**
     * Get the number of articles processed so far.
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed.get();
    }

    /**
     * Begin the interaction with this processor.
     * @see DbManager#beginTxn()
     */
    public void begin() {
        if (!dbManager.txnInProgress()) {
            dbManager.beginTxn();
        }
    }

    /**
     * End the interaction with this processor.  Note that this does not
     * necessarily indicate that the processor will not be utilized again.
     * @see DbManager#commitTxn()
     */
    public void end() {
        if (dbManager.txnInProgress()) {
            dbManager.commitTxn();
        }
    }
}
