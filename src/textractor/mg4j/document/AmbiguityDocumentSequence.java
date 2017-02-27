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

import it.unimi.dsi.mg4j.document.DocumentIterator;
import textractor.database.DbManager;
import textractor.database.TextractorDBDocumentSequence;
import textractor.database.TextractorDatabaseException;
import textractor.util.SentenceFilter;

/**
 * Created by IntelliJ IDEA.
 * User: Fabien Campagne
 * Date: Oct 23, 2005
 * Time: 2:28:45 PM
 * To change this template use File | Settings | File Templates.
 */
public final class AmbiguityDocumentSequence extends TextractorDBDocumentSequence {
    private SentenceFilter sentenceFilter;

    public AmbiguityDocumentSequence(final DbManager dbManager,
            final TextractorDocumentFactory factory, final int chunkSize,
            final SentenceFilter sentenceIndexFilter) {
        this(dbManager, factory, chunkSize);
        this.sentenceFilter = sentenceIndexFilter;
    }

    public AmbiguityDocumentSequence(final DbManager dbm,
            final TextractorDocumentFactory factory, final int chunkSize) {
        super(dbm, factory, chunkSize);
    }

    public AmbiguityDocumentSequence(final TextractorDocumentFactory factory,
            final int chunkSize) throws TextractorDatabaseException {
        super(new DbManager(), factory, chunkSize);
    }

    @Override
    public DocumentIterator iterator() {
        if (!dbm.txnInProgress()) {
            dbm.beginTxn();
        }
        return new AmbiguityDocumentIterator(
                dbm, sentenceFilter, factory(), chunkSize);
    }

    public void setSentenceFilter(final SentenceFilter filter) {
        this.sentenceFilter = filter;
    }
}
