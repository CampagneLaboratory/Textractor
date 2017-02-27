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

package textractor.sentence;

import textractor.TextractorProcessor;
import textractor.event.sentence.SentenceProcessedListener;
import textractor.event.sentence.SentenceProcessingCompleteListener;

/**
 * Implementations of this class perform operations on
 * {@link textractor.datamodel.Sentence}s and their respective
 * {@link textractor.datamodel.Article}s.
 */
public interface SentenceProcessor extends TextractorProcessor {
    /**
     * Get the number of articles processed so far.
     * @return The number of articles processed so far
     */
    int getNumberOfArticlesProcessed();

    /**
     * Get the number of sentences processed so far.
     * @return The number of sentences processed so far
     */
    int getNumberOfSentencesProcessed();

    /**
     * Adds an event listener to be invoked whenever a sentence is processed.
     * @param listener The event listener to add.
     */
    void addSentenceProcessedListener(final SentenceProcessedListener listener);

    /**
     * Removes an event listener for process events.
     * @param listener The event listener to remove
     */
    void removeSentenceProcessedListener(
            final SentenceProcessedListener listener);

    /**
     * Adds an event listener to be invoked when sentence processing is
     * complete.
     * @param listener The event listener to add.
     */
    void addSentenceProcessingCompleteListener(
            final SentenceProcessingCompleteListener listener);

    /**
     * Removes an event listener for process events.
     * @param listener The event listener to remove
     */
    void removeSentenceProcessingCompleteListener(
            final SentenceProcessingCompleteListener listener);

}
