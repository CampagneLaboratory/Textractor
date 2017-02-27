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

import org.apache.commons.chain.Command;
import org.apache.commons.chain.impl.ContextBase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Defines a {@link org.apache.commons.chain.Context} used in a chain of
 * sentence processing commands.
 */
public class TextractorContext extends ContextBase {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 2L;

    /**
     * An {@link ExecutorService} that can be used to start new threads.
     * TODO: This needs to be configurable
     */
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * Queues that the producer and consumer threads will work from.
     * Consumers may be working from different producers in the chain.
     */
    private final Map<Command, BlockingQueue<ArticleSentencesPair>> workQueues =
        new ConcurrentHashMap<Command, BlockingQueue<ArticleSentencesPair>>();

    /**
     * Threads running in this context.
     */
    private List<Future<Boolean>> workThreads =
        new CopyOnWriteArrayList<Future<Boolean>>();

    /**
     * Create a new {@link org.apache.commons.chain.Context}.
     */
    public TextractorContext() {
        super();
    }

    /**
     * Get the queue to process work from.
     * @param command {@link Command} to get the queue for
     * @return A queue of {@link ArticleSentencesPair} objects.
     */
    public final BlockingQueue<ArticleSentencesPair>
        getWorkQueue(final Command command) {
        return workQueues.get(command);
    }

    /**
     * Set the queue to process work from.
     * @param command {@link Command} to set the queue for
     * @param queue the queue to set
     */
    public final void setWorkQueue(final Command command,
        final BlockingQueue<ArticleSentencesPair> queue) {
        workQueues.put(command, queue);
    }

    /**
     * @return the workThreads
     */
    public List<Future<Boolean>> getWorkThreads() {
        return workThreads;
    }

    /**
     * @param threads the workThreads to set
     */
    public void setWorkThreads(final List<Future<Boolean>> threads) {
        this.workThreads = threads;
    }

    /**
     * An {@link ExecutorService} that can be used to start new threads.
     * @return The thread pool for this chain.
     */
    public final ExecutorService getThreadPool() {
        return threadPool;
    }

    /**
     * Define the {@link ExecutorService} that can be used to start new threads.
     * @param pool The thread pool for this chain.
     */
    public final void setThreadPool(final ExecutorService pool) {
        this.threadPool = pool;
    }
}
