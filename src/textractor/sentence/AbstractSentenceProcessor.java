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

import textractor.datamodel.Sentence;
import textractor.event.ProcessingEvent;
import textractor.event.sentence.SentenceProcessedListener;
import textractor.event.sentence.SentenceProcessingCompleteEvent;
import textractor.event.sentence.SentenceProcessingCompleteListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Convenience base class for {@link SentenceProcessor} implementations.
 */
public abstract class AbstractSentenceProcessor implements SentenceProcessor {
    /**
     * Sentence processing listeners associated with this processor.
     */
    private final transient Collection<SentenceProcessingCompleteListener>
        sentenceProcessingCompleteListeners =
            Collections.synchronizedCollection(
                    new ArrayList<SentenceProcessingCompleteListener>());

    /**
     * Sentence processing listeners associated with this processor.
     */
    private final transient Collection<SentenceProcessedListener>
        sentenceProcessedListeners =
            Collections.synchronizedCollection(
                    new ArrayList<SentenceProcessedListener>());

    /**
     * Create a new {@link SentenceProcessor}.
     */
    public AbstractSentenceProcessor() {
        super();
    }

    /**
     * Add a SentenceProcessedListener to the listener list.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener The SentenceProcessedListener to be added
     */
    public final void addSentenceProcessedListener(
            final SentenceProcessedListener listener) {
        if (listener != null) {
            sentenceProcessedListeners.add(listener);
        }
    }

    /**
     * Add a SentenceProcessingCompleteListener to the listener list.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener The SentenceProcessingCompleteListener to be added
     */
    public final void addSentenceProcessingCompleteListener(
            final SentenceProcessingCompleteListener listener) {
        if (listener != null) {
            sentenceProcessingCompleteListeners.add(listener);
        }

    }

    /**
     * Remove a SentenceProcessedListener from the listener list.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener The SentenceProcessedListener to be removed
     */
    public final void removeSentenceProcessedListener(
            final SentenceProcessedListener listener) {
        sentenceProcessedListeners.remove(listener);
    }

    /**
     * Remove a SentenceProcessingCompleteListener to the listener list.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener The SentenceProcessingCompleteListener to be removed
     */
    public final void removeSentenceProcessingCompleteListener(
            final SentenceProcessingCompleteListener listener) {
        sentenceProcessingCompleteListeners.remove(listener);
    }

    /**
     * Fire a sentence processed event to the listeners.
     * @param sentence The sentence that was processed.
     */
    protected final void fireSentenceProcessedEvent(final Sentence sentence) {
        if (sentenceProcessedListeners.size() > 0) {
            final ProcessingEvent<SentenceProcessor, Sentence> event =
                new ProcessingEvent<SentenceProcessor, Sentence>(this, sentence);
            for (final SentenceProcessedListener listener : sentenceProcessedListeners) {
                listener.sentenceProcessed(event);
            }
        }
    }

    /**
     * Fire a sentence processing complete event to the listeners.
     */
    protected final void fireSentenceProcessingCompleteEvent() {
        if (sentenceProcessingCompleteListeners.size() > 0) {
            final SentenceProcessingCompleteEvent event =
                new SentenceProcessingCompleteEvent(this);
            for (final SentenceProcessingCompleteListener listener
                    : sentenceProcessingCompleteListeners) {
                listener.processingComplete(event);
            }
        }
    }
}
