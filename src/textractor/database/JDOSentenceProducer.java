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

package textractor.database;

import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.sentence.AbstractSentenceProcessor;
import textractor.sentence.SentenceProcessingException;
import textractor.sentence.SentenceProducer;
import textractor.sentence.TransactionalSentenceProcessor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class JDOSentenceProducer extends AbstractSentenceProcessor implements
        SentenceProducer, TransactionalSentenceProcessor {
    /** Database manager to produce with. */
    protected transient DbManager dbManager;

    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /** Number of sentences processed so far. */
    private final AtomicInteger numberOfSentencesProcessed =
        new AtomicInteger();

    public JDOSentenceProducer() {
        super();
    }

    /**
     *
     */
    public Sentence produce(final Article article, final CharSequence text)
            throws SentenceProcessingException {
        final Sentence sentence = new Sentence(article, text.toString());
        dbManager.makePersistent(sentence);
        return sentence;
    }

    /**
     * Get the number of articles processed so far.
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed.get();
    }

    /**
     * Get the number of sentences processed so far.
     * @return The number of sentences processed so far
     */
    public int getNumberOfSentencesProcessed() {
        return numberOfSentencesProcessed.get();
    }

    public final void begin() {
        if (!dbManager.txnInProgress()) {
            dbManager.beginTxn();
        }
    }

    public final void end() {
        if (dbManager.txnInProgress()) {
            dbManager.commitTxn();
        }
    }

}
