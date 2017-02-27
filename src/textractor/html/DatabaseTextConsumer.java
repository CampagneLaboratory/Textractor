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

package textractor.html;

import textractor.database.DbManager;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;

import java.util.Collection;

/**
 * A consumer that submits text to a database.
 *
 * @author Fabien Campagne
 *         Date: Aug 16, 2006
 *         Time: 4:06:12 PM
 */
public final class DatabaseTextConsumer extends TextConsumer
    implements CheckpointCallback {
    /** Database manager to produce with. */
    private DbManager dbManager;

    /** Number of articles processed so far. */
    private int numberOfArticlesProcessed;

    /** Number of sentences processed so far. */
    private int numberOfSentencesProcessed;

    private final int articleChunkSize;

    public DatabaseTextConsumer(final DbManager dbm, final int articleChunkSize) {
        this.dbManager = dbm;
        this.articleChunkSize = articleChunkSize;
    }

    /**
     * Begin the interaction with this processor.
     * @see DbManager#beginTxn()
     */
    public void begin() {
        numberOfArticlesProcessed = 0;
        dbManager.beginTxn();
    }

    /**
     * End the interaction with this processor.  Note that this does not
     * necessarily indicate that the processor will not be utilized again.
     * @see DbManager#commitTxn()
     */
    public void end() {
        dbManager.commitTxn();
    }

    public Article createArticle() {
        return dbManager.getTextractorManager().createArticle();
    }

    public Sentence produce(final Article article, final CharSequence text) {
        return new Sentence(article, text.toString());
    }

    public void consume(final Article article, final Collection<Sentence> sentences) {
        consume(article, sentences, this);
    }

    @Override
    public void consume(final Article article, final Collection<Sentence> sentences, final CheckpointCallback callback) {
        // number article and sentences:
        article.setArticleNumber(dbManager.getTextractorManager().getNextArticleNumber());
        long length = 0;

        for (final Sentence sentence : sentences) {
            length++;
            numberOfSentencesProcessed++;

            sentence.setDocumentNumber(dbManager.getTextractorManager()
                    .getNextDocumentNumber());

            if (length == 1) {
                article.setDocumentNumberRangeStart(sentence.getDocumentNumber());
            }
        }
        article.setDocumentNumberRangeLength(length);
        // make persistent:
        dbManager.makePersistent(article);
        dbManager.makePersistentAll(sentences);

        ++numberOfArticlesProcessed;

        if ((numberOfArticlesProcessed % articleChunkSize) == 0) {
            dbManager.commitTxn();
            callback.checkpointCallback();
            dbManager.beginTxn();
        }
    }

    public DbManager getDbManager() {
        return dbManager;
    }

    /**
     * Get the number of articles processed so far.
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed;
    }

    /**
     * Get the number of sentences processed so far.
     * @return The number of sentences processed so far
     */
    public int getNumberOfSentencesProcessed() {
        return numberOfSentencesProcessed;
    }

    public void setDbManager(final DbManager dbm) {
        this.dbManager = dbm;
    }

    public void checkpointCallback() {
    }
}
