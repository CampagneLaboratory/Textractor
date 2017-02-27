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

package textractor.chain;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.event.sentence.SentenceProcessingCompleteEvent;
import textractor.event.sentence.SentenceProcessingCompleteListener;
import textractor.sentence.AbstractSentenceProcessor;
import textractor.sentence.SentenceConsumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * {@link org.apache.commons.chain.Command} implementation that executes
 * a {@link textractor.sentence.SentenceConsumer}.
 */
public abstract class AbstractSentenceConsumer
    extends AbstractSentenceProcessor implements Command, SentenceConsumer,
        SentenceProcessingCompleteListener, Callable<Boolean> {
    /** Used to log debug and informational messages. */
    private static final Log LOG =
        LogFactory.getLog(AbstractSentenceConsumer.class);

    /** Indicates the producer is done and there will be no more to consume. */
    protected boolean productionCompleted;

    /** The queue that the consumer thread will work from. */
    private BlockingQueue<ArticleSentencesPair> workQueue;

    /** The {@link Context} that this {@link SentenceConsumer} can use. */
    protected TextractorContext textractorContext;

    /**
     * Create a new {@link Command} to consume sentences.
     */
    public AbstractSentenceConsumer() {
        super();
    }

    public final boolean execute(final Context context) {
        // Verify our parameters
        if (context == null) {
            throw new IllegalArgumentException("Context is null.");
        } else if (!(context instanceof TextractorContext)) {
            throw new IllegalArgumentException("Not a TextractorContext.");
        }

        textractorContext = (TextractorContext) context;

        // get the queue to work from
        workQueue = textractorContext.getWorkQueue(this);

        // we want to add ouselves as a listener for both the producer
        // and the consuer to know when there is no more data to process
        addSentenceProcessingCompleteListener(this);

        // TODO: deal with the producer complete event

        // start a consumer thread
        final ExecutorService executorService =
            textractorContext.getThreadPool();
        final Future<Boolean> consumer = executorService.submit(this);
        textractorContext.getWorkThreads().add(consumer);

        // and let the chain continue it's work
        return CONTINUE_PROCESSING;
    }

    public final Boolean call() throws Exception {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug(BeanUtils.describe(this));
            }
            while (true) {
                final ArticleSentencesPair pair =
                        workQueue.poll(100, MILLISECONDS);
                if (pair != null) {
                    consume(pair.article, pair.sentences);
                } else {
                    if (productionCompleted) {
                        break;
                    }
                }
            }

            if (okToComplete()) {
                fireSentenceProcessingCompleteEvent();
            }
        } catch (Throwable t) {
            LOG.fatal("Got an exception in thread " + this.getClass().getName(), t);
            Thread.currentThread().getThreadGroup().interrupt();
            if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw new Exception(t);
            }
        }
        return true;
    }

    /**
     * Indicate that all processing is complete and it's ok to terminate.
     * If false is returned the consumer thread will terminate without firing
     * a {@link textractor.event.sentence.SentenceProcessingCompleteEvent}.
     * Be aware of this and send the event if you override the default
     * behavior.
     * @return true if it's ok to complete.
     */
    public boolean okToComplete() {
        return true;
    }

    /**
     * This method gets called when a sentence processing is complete.
     * @param event A {@link SentenceProcessingCompleteEvent} object describing
     * the event source.
     */
    public void processingComplete(final SentenceProcessingCompleteEvent event) {
        LOG.debug("Got completion from: " + event.getSource());
        if (event.getSource() != this) {
            productionCompleted = true;
        }
    }
}
