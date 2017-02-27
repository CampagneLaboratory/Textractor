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

import textractor.TextractorProcessor;

/**
 * Implementations of this class perform operations on
 * {@link textractor.datamodel.Article}s.
 */
public interface ArticleProcessor extends TextractorProcessor {
    /**
     * Get the number of articles processed so far.
     * @return The number of articles processed so far
     */
    int getNumberOfArticlesProcessed();

    /**
     * Adds an event listener to be invoked whenever a article is processed.
     * @param listener The event listener to add.
     */
    // TODO void addArticleProcessedListener(final ArticleProcessedListener listener);

    /**
     * Removes an event listener for process events.
     * @param listener The event listener to remove
     */
    // TODO void removeArticleProcessedListener(final ArticleProcessedListener listener);

    /**
     * Adds an event listener to be invoked when article processing is
     * complete.
     * @param listener The event listener to add.
     */
    // TODO void addArticleProcessingCompleteListener(final ArticleProcessingCompleteListener listener);

    /**
     * Removes an event listener for process events.
     * @param listener The event listener to remove
     */
    // TODO void removeArticleProcessingCompleteListener(final ArticleProcessingCompleteListener listener);
}
