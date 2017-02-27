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

package textractor.event;

import textractor.TextractorProcessor;

import java.util.EventObject;

/**
 * Events associated with processing.
 * @param <P> The {@link TextractorProcessor} that generated the event
 * @param <T> The target associated with this event.
 */
public class ProcessingEvent<P extends TextractorProcessor, T> extends EventObject {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Indicates the type of event.
     */
    public enum EventType {
        /** Indicates the target object has been produced. */
        Produced,
        /** Indicates the target object has been consumed. */
        Consumed,
        /** Indicates the target objet has been transformed. */
        Transformed
    }

    /**
     * The type of processing performed on the target.
     */
    private EventType type;

    /**
     * The object that was processed.
     */
    private T target;

    /**
     * Creates a new event object.
     * @param processor The processsor that generated the event.
     * @param target The object that was processed.
     */
    public ProcessingEvent(final P processor, final T target) {
        super(processor);
        this.target = target;
    }

    public EventType getType() {
        return type;
    }

    public void setType(final EventType type) {
        this.type = type;
    }

    public P getProcessor() {
        return (P) getSource();
    }

    public void setProcessor(final P processor) {
        this.source = processor;
    }

    public T getTarget() {
        return target;
    }

    public void setTarget(final T target) {
        this.target = target;
    }
}
