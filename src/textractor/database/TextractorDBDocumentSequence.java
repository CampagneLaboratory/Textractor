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

import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.document.DocumentIterator;
import it.unimi.dsi.mg4j.document.DocumentSequence;
import textractor.mg4j.document.TextractorDocumentFactory;
import textractor.util.SentenceFilter;

/**
 * A document sequence that reads documents from the database.
 *
 * @author Fabien Campagne
 * Date: Oct 23, 2005
 * Time: 12:45:10 PM
 */
public class TextractorDBDocumentSequence implements DocumentSequence {
    protected final DbManager dbm;
    protected final int chunkSize;
    private SentenceFilter sentenceFilter;
    private final DocumentFactory factory;
    boolean ownDbManager;

    /**
     * Construct the sequence with a database manager.
     *
     * @param dbm       The database manager to connect to the textractor database
     * @param factory   The document factory that should be used to create the documents returned by this sequence.
     * @param chunkSize Number of documents to retrieve from the database in on chunk.
     */
    public TextractorDBDocumentSequence(final DbManager dbm,
            final DocumentFactory factory, final int chunkSize) {
        super();
        this.factory = factory;
        this.dbm = dbm;
        this.chunkSize = chunkSize;
        ownDbManager = false;
    }

    /**
     * Construct the sequence, creating a new database manager with default connection parameters.
     *
     * @param factory   The document factory that should be used to create the documents returned by this sequence.
     * @param chunkSize Number of documents to retrieve from the database in on chunk.
     */
    public TextractorDBDocumentSequence(final DocumentFactory factory,
            final int chunkSize) throws TextractorDatabaseException {
        this(new DbManager(), factory, chunkSize);
        ownDbManager = true;
    }

    /**
     * Construct the sequence with a database manager and a sentence filter. The filter provides a mechanism to
     * restrict the set of documents that the document sequence will return.
     *
     * @param dbm       The database manager to connect to the textractor database
     * @param factory   The document factory that should be used to create the documents returned by this sequence.
     * @param chunkSize Number of documents to retrieve from the database in on chunk.
     * @param filter    A filter to decide which sentences from the database should be returned by this sequence.
     */
    public TextractorDBDocumentSequence(final DbManager dbm,
            final TextractorDocumentFactory factory, final int chunkSize,
            final SentenceFilter filter) {
        this(dbm, factory, chunkSize);
        sentenceFilter = filter;
    }

    public DocumentIterator iterator() {
        if (!dbm.txnInProgress()) {
            dbm.beginTxn();
        }
        return new TextractorDBDocumentIterator(
                dbm, factory(), chunkSize, sentenceFilter);
    }

    public DocumentFactory factory() {
        return factory;
    }

    /**
     * Releases database resources. Shutdown the DbManager only if this class created it (the DbManager will not be
     * closed if it was provided by the constructor).
     */
    public void close() {
        if (dbm != null && dbm.txnInProgress()) {
            dbm.commitTxn();

        }
        if (dbm != null && ownDbManager) {
            dbm.shutdown();
        }
    }
}
