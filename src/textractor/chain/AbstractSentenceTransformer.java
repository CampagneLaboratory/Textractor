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
import org.apache.commons.chain.Chain;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.Filter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.event.sentence.SentenceProcessingCompleteEvent;
import textractor.event.sentence.SentenceProcessingCompleteListener;
import textractor.sentence.AbstractSentenceProcessor;
import textractor.sentence.SentenceProcessor;
import textractor.sentence.SentenceTransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Base class for {@link org.apache.commons.chain.Chain} implementations that
 * contain one or more {@link textractor.sentence.SentenceProcessor} commands.
 */
public abstract class AbstractSentenceTransformer extends
            AbstractSentenceProcessor implements Chain, SentenceTransformer,
            SentenceProcessingCompleteListener, Callable<Boolean> {
    /** Used to log debug and informational messages. */
    private static final Log LOG =
        LogFactory.getLog(AbstractSentenceTransformer.class);

    /** The queue that the transformer will read to. */
    private BlockingQueue<ArticleSentencesPair> inputQueue;

    /** Size of the transform output work queue. */
    private int outputQueueSize = 10000;

    /** The queue that the transformer will write to. */
    private BlockingQueue<ArticleSentencesPair> outputQueue =
            new ArrayBlockingQueue<ArticleSentencesPair>(outputQueueSize);

    /** Indicates the producer is done and there will be no more to consume. */
    protected boolean productionCompleted;

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
    public AbstractSentenceTransformer() {
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
    public AbstractSentenceTransformer(final Command command) {
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
    public AbstractSentenceTransformer(final Command[] commands) {
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
    public AbstractSentenceTransformer(final Collection<Command> commands) {
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
            return CONTINUE_PROCESSING;
        }

        final TextractorContext textractorContext = (TextractorContext) context;

        // we want to add ourselves as a listener for both the producer
        // and the consumer to know when there is no more data to process
        addSentenceProcessingCompleteListener(this);

        // Get the queue to work from
        inputQueue = textractorContext.getWorkQueue(this);

        for (final Command command : commands) {
            textractorContext.setWorkQueue(command, outputQueue);
            if (command instanceof SentenceProcessingCompleteListener) {
                LOG.debug("adding consumer: " + command.getClass().getName());
                addSentenceProcessingCompleteListener((SentenceProcessingCompleteListener) command);
            }
            if (command instanceof SentenceProcessor) {
                LOG.debug("adding producer to listener: " + command.getClass().getName());
                ((SentenceProcessor)command).addSentenceProcessingCompleteListener(this);
            }
        }
        // start a prducer thread
        final ExecutorService executorService =
            textractorContext.getThreadPool();

        final Future<Boolean> transformer = executorService.submit(this);
        textractorContext.getWorkThreads().add(transformer);

        // a latch for this thread and all the consumers
        // TODO latch = new CountDownLatch(numberOfCommands + 1);

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
                // TODO latch.countDown();
                break;
            }
        }

        // wait until the processing is finished
        // TODO latch.await();

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
                    LOG.debug("Filter threw exception", e);
                }
            }
        }

        // Return the exception or result state from the last execute()
        if ((saveException != null) && !handled) {
            throw saveException;
        } else {
            return saveResult;
        }
    }

    public final Boolean call() throws Exception {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug(BeanUtils.describe(this));
            }
            while (true) {
                final ArticleSentencesPair pair = inputQueue.poll(100, MILLISECONDS);
                if (pair != null) {
                    final ArticleSentencesPair transformedPair = transform(pair);
                    if (transformedPair != null) {
                        outputQueue.put(transformedPair);
                    }
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
     * This method gets called when a sentence processing is complete.
     * @param event A {@link SentenceProcessingCompleteEvent} object
     * describing the event source.
     */
    public void processingComplete(final SentenceProcessingCompleteEvent event) {
        LOG.debug("Got completion from: " + event.getSource());
        if (event.getSource() != this) {
            productionCompleted = true;
        }
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
     * Get the size of the sentence transformation queue.
     * @return The size of the queue.
     */
    public int getOutputQueueSize() {
        return outputQueueSize;
    }

    /**
     * Set the size of the sentence transformation queue.
     * @param size The size of the queue.
     */
    public void setOutputQueueSize(final int size) {
        this.outputQueueSize = size;
        this.outputQueue =
            new ArrayBlockingQueue<ArticleSentencesPair>(outputQueueSize);
    }
}
