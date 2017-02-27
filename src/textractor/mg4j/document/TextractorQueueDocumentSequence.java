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

package textractor.mg4j.document;

import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.document.DocumentIterator;
import it.unimi.dsi.mg4j.document.DocumentSequence;
import textractor.datamodel.Sentence;
import textractor.util.SentenceFilter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

public final class TextractorQueueDocumentSequence implements DocumentSequence {
    private final DocumentFactory factory;
    private final SentenceFilter filter;
    private final BlockingQueue<Sentence> queue;
    private final Semaphore loaderSemaphore;
    private final int loaderThreadCount;

    public TextractorQueueDocumentSequence(final DocumentFactory factory,
                                           final SentenceFilter filter,
                                           final BlockingQueue<Sentence> queue,
                                           final Semaphore semaphore,
                                           final int loaderThreadCount) {
        super();
        this.factory = factory;
        this.filter = filter;
        this.queue = queue;
        this.loaderSemaphore = semaphore;
        this.loaderThreadCount = loaderThreadCount;
    }
    /**
     * Returns an iterator over the sequence of documents.
     * <p/>
     * <p><strong>Warning</strong>: this method can be safely called
     * just <em>one</em> time. For instance, implementations based
     * on standard input will usually throw an exception if this
     * method is called twice.
     * <p/>
     * <p>Implementations may decide to override this restriction
     * (in particular, if they implement
     * {@link it.unimi.dsi.mg4j.document.DocumentCollection}). Usually,
     * however, it is not possible to obtain <em>two</em> iterators at the
     * same time on a collection.
     *
     * @return an iterator over the sequence of documents.
     * @see it.unimi.dsi.mg4j.document.DocumentCollection
     */

    public DocumentIterator iterator() {
        return new TextractorQueueDocumentIterator(factory, filter, queue,
                loaderSemaphore, loaderThreadCount);
    }

    /**
     * Returns the factory used by this sequence.
     * <p/>
     * <P>Every document sequence is based on a document factory that
     * transforms raw bytes into a sequence of characters. The factory
     * contains useful information such as the number of fields.
     *
     * @return the factory used by this sequence.
     */

    public DocumentFactory factory() {
        return factory;
    }

    /**
     * Closes this document sequence, releasing all resources.
     * <p/>
     * <p>You should always call this method after having finished with this
     * document sequence. Implementations are invited to call this method in a
     * finaliser as a safety net (even better, implement
     * {@link it.unimi.dsi.mg4j.io.SafelyCloseable}), but since there
     * is no guarantee as to when finalisers are invoked, you should not
     * depend on this behaviour.
     */
    public void close() {
    }
}
