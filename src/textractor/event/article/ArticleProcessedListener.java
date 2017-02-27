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

package textractor.event.article;

import textractor.article.ArticleProcessor;
import textractor.datamodel.Article;
import textractor.event.ProcessingEvent;

import java.util.EventListener;

/**
 * A "SentenceProcessed" event gets fired whenever a
 * {@link textractor.sentence.SentenceProcessor} processes a
 * {@link textractor.datamodel.Sentence}.  You can register a
 * SentenceProcessedListener with a SentenceProcessor so as to be notified
 * of sentence updates.
 */
public interface ArticleProcessedListener extends EventListener {
    /**
     * This method gets called when a sentence is processed.
     * @param event A {{@link textractor.event.ProcessingEvent} object
     * describing the event source and the sentence that has been processed.
     */
    void articleProcessed(final ProcessingEvent<ArticleProcessor, Article> event);
}
