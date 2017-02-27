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

import textractor.event.sentence.SentenceProcessingCompleteEvent;

import java.util.EventListener;

/**
 * A "SentenceProcessingComplete" event gets fired whenever a
 * {@link textractor.sentence.SentenceProcessor} has finished processing
 * {@link textractor.datamodel.Sentence}s. You can register a
 * SentenceProcessingCompleteListener with a SentenceProcessor so as to be
 * notified of processing completion.
 */
public interface ArticleProcessingCompleteListener extends EventListener {
    /**
     * This method gets called when a sentence processing is complete.
     * @param event A
     * {@link textractor.event.sentence.SentenceProcessingCompleteEvent}
     * object describing the event source.
     */
    void processingComplete(final SentenceProcessingCompleteEvent event);
}
