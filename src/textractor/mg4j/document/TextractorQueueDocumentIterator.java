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

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.mg4j.document.Document;
import it.unimi.dsi.mg4j.document.DocumentFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.datamodel.Sentence;
import textractor.util.SentenceFilter;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class TextractorQueueDocumentIterator extends AbstractTextractorDocumentIterator {
    private static final Log LOG =
            LogFactory.getLog(TextractorQueueDocumentIterator.class);
    private final BlockingQueue<Sentence> queue;
    private final Semaphore loaderSemaphore;
    private final int loaderThreadCount;

    public TextractorQueueDocumentIterator(final DocumentFactory factory,
                                           final SentenceFilter sentenceFilter,
                                           final BlockingQueue<Sentence> queue,
                                           final Semaphore semaphore,
                                           final int loaderThreadCount) {
        super(factory, sentenceFilter);
        this.queue = queue;
        this.loaderSemaphore = semaphore;
        this.loaderThreadCount = loaderThreadCount;
    }

    /**
     * Returns the next document.
     *
     * @return the next document, or <code>null</code> if there are no other
     * documents.
     */
    public Document nextDocument() throws IOException {
        final String text;
        final Reference2ObjectMap<Enum<?>, Object> metadata;
        try {
            Sentence sentence;
            do {
                sentence = queue.poll(100, MILLISECONDS);
                if (sentence == null && !moreDocumentsAvailable()) {
                    loaderSemaphore.release(loaderThreadCount);
                    return null;
                }
            } while (sentence == null);

            metadata = sentence.getMetaData();
            if (sentenceFilter.filterSentence(sentence)) {
                text = " ";
                ++filteredSentenceCount;
            } else {
                text = sentence.getText();
            }
        } catch (final InterruptedException e) {
            LOG.error("Interrupted!", e);
            Thread.currentThread().interrupt();
            return null;
        }

        return createDocument(metadata, text);
    }

     /**
      * Returns the next textractor document.
      *
      * @return the next document, or <code>null</code> if there are no other
      * documents.
      */
    public Sentence nextSentence() {
        final Sentence currentSentence;
        try {
            Sentence sentence;
            do {
                sentence = queue.poll(100, MILLISECONDS);
                if (sentence == null && !moreDocumentsAvailable()) {
                    loaderSemaphore.release(loaderThreadCount);
                    return null;
                }
            } while (sentence == null);

            if (sentenceFilter.filterSentence(sentence)) {
                currentSentence = null;
                ++filteredSentenceCount;
            } else {
                currentSentence = sentence;
            }
        } catch (final InterruptedException e) {
            LOG.error("Interrupted!", e);
            Thread.currentThread().interrupt();
            return null;
        }

        return currentSentence;
    }

    private boolean moreDocumentsAvailable() {
        return !loaderSemaphore.tryAcquire(loaderThreadCount);
    }

    /**
     * Closes this document iterator, releasing all resources.
     * <p/>
     * <p>You should always call this method after having finished with this iterator.
     * Implementations are invited to call this method in a finaliser as a safety net, but since there
     * is no guarantee as to when finalisers are invoked, you should not depend on this behaviour.
     */
    public void close() {
    }
}
