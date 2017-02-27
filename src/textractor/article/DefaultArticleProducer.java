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

package textractor.article;

import textractor.datamodel.Article;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple {@link ArticleProducer} that produces empty articles in memory.
 */
public final class DefaultArticleProducer implements ArticleProducer {
    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /**
     * Create a new {@link ArticleProducer}.
     */
    public DefaultArticleProducer() {
        super();
    }

    /**
     * Creates a new {@link Article}.  The article number will be set based
     * on the number of articles processed.
     * @return a new article
     */
    public Article createArticle() {
        final Article article = new Article();
        article.setArticleNumber(numberOfArticlesProcessed.getAndIncrement());
        return article;
    }

    /**
     * Get the number of articles processed so far.
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed.get();
    }
}
