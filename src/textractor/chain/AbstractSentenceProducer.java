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

import org.apache.commons.chain.Chain;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.Filter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.event.sentence.SentenceProcessingCompleteEvent;
import textractor.event.sentence.SentenceProcessingCompleteListener;
import textractor.sentence.AbstractSentenceProcessor;
import textractor.sentence.SentenceProcessor;
import textractor.sentence.SentenceProducer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Base class for {@link org.apache.commons.chain.Chain} implementations that
 * contain one or more {@link textractor.sentence.SentenceProcessor} commands.
 */
public abstract class AbstractSentenceProducer extends
        AbstractSentenceProcessor implements Chain, SentenceProducer,
        SentenceProcessingCompleteListener, Callable<Boolean> {
    /** Used to log debug and informational messages. */
    private static final Log LOG =
        LogFactory.getLog(AbstractSentenceProducer.class);

    /** Indicates that processing has completed. */
    private CountDownLatch latch;

    /** Size of the sentence work queue. */
    private int workQueueSize = 10000;

    /** The queue that the consumer thread will work from. */
    private BlockingQueue<ArticleSentencesPair> workQueue =
            new ArrayBlockingQueue<ArticleSentencesPair>(workQueueSize);

    /**
     * The list of {@link org.apache.commons.chain.Command}s configured for
     * this {@link org.apache.commons.chain.Chain}, in the order in which they
     * may delegate processing to the remainder of the
     * {@link org.apache.commons.chain.Chain}.
     */
    protected final List<Command> commands = new ArrayList<Command>();

    /**
     * Flag indicating whether the configuration of our commands list
     * has been frozen by a call to the <code>execute()</code> method.
     */
    protected boolean frozen;

    /**
     * Create a new {@link org.apache.commons.chain.Chain}.
     */
    public AbstractSentenceProducer() {
        super();
    }

    /**
     * Construct a {@link org.apache.commons.chain.Chain} configured with the
     * specified {@link org.apache.commons.chain.Command}.
     *
     * @param command The {@link org.apache.commons.chain.Command} to be
     * configured
     * @throws IllegalArgumentException if <code>command</code> is
     * <code>null</code>
     */
    public AbstractSentenceProducer(final Command command) {
        super();
        addCommand(command);
    }

    /**
     * Construct a {@link org.apache.commons.chain.Chain} configured with the
     * specified {@link org.apache.commons.chain.Command}s.
     *
     * @param commands The {@link Command}s to be configured
     * @throws IllegalArgumentException if <code>commands</code>,
     * or one of the individual {@link org.apache.commons.chain.Command}
     * elements, is <code>null</code>
     */
    public AbstractSentenceProducer(final Command[] commands) {
        super();
        if (commands == null) {
            throw new IllegalArgumentException();
        }
        for (final Command command : commands) {
            addCommand(command);
        }
    }

    /**
     * Construct a {@link org.apache.commons.chain.Chain} configured with the
     * specified {@link org.apache.commons.chain.Command}s.
     *
     * @param commands The {@link org.apache.commons.chain.Command}s to be
     * configured
     * @throws IllegalArgumentException if <code>commands</code>,
     * or one of the individual {@link org.apache.commons.chain.Command}
     * elements, is <code>null</code>
     */
    public AbstractSentenceProducer(final Collection<Command> commands) {
        super();
        if (commands == null) {
            throw new IllegalArgumentException();
        }
        for (final Command command : commands) {
            addCommand(command);
        }
    }

    /**
     * See the {@link org.apache.commons.chain.Chain} JavaDoc.
     *
     * @param command The {@link org.apache.commons.chain.Command} to be added
     * @throws IllegalArgumentException if <code>command</code>
     * is <code>null</code>
     * @throws IllegalStateException if no further configuration is allowed
     */
    public final void addCommand(final Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Command is null.");
        }
        if (frozen) {
            throw new IllegalStateException("Chain execution already started.");
        }
        commands.add(command);
    }

    /**
     * See the {@link org.apache.commons.chain.Chain} JavaDoc.
     *
     * @param context The {@link org.apache.commons.chain.Context} to be
     * processed by this {@link org.apache.commons.chain.Chain}
     * @return <code>true</code> if the processing of this
     * {@link org.apache.commons.chain.Context} has been completed, or
     * <code>false</code> if the processing of this
     * {@link org.apache.commons.chain.Context} should be delegated to a
     * subsequent {@link org.apache.commons.chain.Command} in an enclosing
     * {@link org.apache.commons.chain.Chain}
     * @throws Exception if there is a problem executing the Chain.
     * @throws IllegalArgumentException if <code>context</code> is
     * <code>null</code>
     */
    public final boolean execute(final Context context) throws Exception {
        // Verify our parameters
        if (context == null) {
            throw new IllegalArgumentException("Context is null.");
        }

        // Freeze the configuration of the command list
        frozen = true;

        final int numberOfCommands = commands.size();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Number of commands = " + numberOfCommands);
        }
        if (numberOfCommands < 1) {
            LOG.warn("Chain doesn't have any commands to execute");
        }

        final TextractorContext textractorContext = (TextractorContext) context;

        // we want to add ouselves as a listener for both the producer
        // and the consuer to know when there is no more data to process
        addSentenceProcessingCompleteListener(this);

        // Set the queue to work from
        textractorContext.setWorkQueue(this, workQueue);

        for (final Command command : commands) {
            textractorContext.setWorkQueue(command, workQueue);
            if (command instanceof SentenceProcessingCompleteListener) {
                LOG.debug("adding consumer: " + command.getClass().getName());
                addSentenceProcessingCompleteListener((SentenceProcessingCompleteListener) command);
            }
            if (command instanceof SentenceProcessor) {
                LOG.debug("adding producer to listener: " + command.getClass().getName());
                ((SentenceProcessor)command).addSentenceProcessingCompleteListener(this);
            }
        }
        // start a producer thread
        final ExecutorService executorService =
            textractorContext.getThreadPool();

        final Future<Boolean> producer = executorService.submit(this);
        textractorContext.getWorkThreads().add(producer);

        // a latch for this thread and all the consumers
        latch = new CountDownLatch(numberOfCommands + 1);

        // Execute the commands in this list until one returns true
        // or throws an exception
        boolean saveResult = false;
        Exception saveException = null;
        for (final Command command : commands) {
            try {
                saveResult = command.execute(context);
                if (saveResult) {
                    break;
                }
            } catch (Exception e) {
                saveException = e;
                // we got an exception, so we won't get a completion event
                latch.countDown();
                break;
            }
        }

        // wait until the processing is finished
        latch.await();

        // all done, call postprocess methods on Filters in reverse order
        boolean handled = false;
        final ListIterator<Command> iterator =
            commands.listIterator(commands.size());
        while (iterator.hasPrevious()) {
            final Command command = iterator.previous();
            if (command instanceof Filter) {
                try {
                    if (((Filter)command).postprocess(context, saveException)) {
                        handled = true;
                    }
                } catch (Exception e) {
                    // Silently ignore
                    LOG.debug("Filter there exception", e);
                }
            }
        }

        // the assumption here is that the producer is at the top of the chain
        // so it's safe to shut everything down by now
        executorService.shutdown();

        // Return the exception or result state from the last execute()
        if ((saveException != null) && !handled) {
            throw saveException;
        } else {
            return saveResult;
        }
    }

    /**
     * Create a article sentence pair object from the individual objects for a
     * sentence consumer to process.
     * @param article The article assoicated with the sentences.
     * @param sentences A collection of Sentences to process.
     */
    protected final void produce(final Article article,
                                 final Collection<Sentence> sentences) {
        final ArticleSentencesPair pair =
            new ArticleSentencesPair(article, sentences);
        // add the sentence to the queue for processing
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Work queue is " + workQueue.size() + " / " + workQueueSize);
            }
            workQueue.put(pair);
        } catch (final InterruptedException e) {
            LOG.error("Interrupted while adding", e);
            Thread.currentThread().interrupt();
        }
    }

    public void processingComplete(final SentenceProcessingCompleteEvent event) {
        LOG.debug("Got completion from: " + event.getSource());
        latch.countDown();
    }

    /**
     * Get the size of the sentence production queue.
     * @return The size of the queue.
     */
    public int getWorkQueueSize() {
        return workQueueSize;
    }

    /**
     * Set the size of the sentence production queue.
     * @param size The size of the queue.
     */
    public void setWorkQueueSize(final int size) {
        this.workQueueSize = size;
        this.workQueue =
            new ArrayBlockingQueue<ArticleSentencesPair>(workQueueSize);
    }
}
