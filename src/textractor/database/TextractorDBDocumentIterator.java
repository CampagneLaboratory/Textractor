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

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.document.Document;
import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.datamodel.TextractorDocument;
import textractor.mg4j.document.AbstractTextractorDocumentIterator;
import textractor.util.SentenceFilter;

import javax.jdo.JDODataStoreException;
import javax.jdo.JDOFatalUserException;
import java.io.IOException;
import java.util.Iterator;

/**
 * An iterator over documents in the database.
 */
public class TextractorDBDocumentIterator extends AbstractTextractorDocumentIterator {
    /** Used to log debug and informational messages. */
    private static final Log LOG =
        LogFactory.getLog(TextractorDBDocumentIterator.class);

    protected final DbManager dbm;
    protected final long chunkSize;
    protected int lowerBound;
    protected Iterator<TextractorDocument> iterator;
    protected int count;

    public TextractorDBDocumentIterator(final DbManager dbmanager,
            final DocumentFactory documentFactory, final int chunkSize,
            final SentenceFilter sentenceFilter) {
        super(documentFactory, sentenceFilter);
        this.dbm = dbmanager;
        lowerBound = -1;
        this.chunkSize = chunkSize;
        final TextractorManager textractorManager = dbm.getTextractorManager();
        textractorManager.setRetrieveAll(true);  // hint DB implementation to pre-fetch the query results.
        iterator = textractorManager.getDocumentIterator(lowerBound, chunkSize);
    }

    protected final Document nextSentence() throws IOException {
        final TextractorDocument document = iterator.next();
        return nextSentence(document);
    }

    protected final Document nextSentence(final TextractorDocument document) throws IOException {
        Reference2ObjectMap<Enum<?>, Object> metadata;
        count++;
        try {
            metadata = document.getMetaData();
            String text = document.getText();
            if (sentenceFilter.filterSentence(document)) {
                text = " ";
                ++filteredSentenceCount;
            }
            return createDocument(metadata, text);
        } catch (final JDODataStoreException e) {
            // TODO figure out why such sentence/missing articles exist and correct the code logic.
            LOG.warn("Document " + document.getDocumentNumber() + " deleted or does not exist exception. Ignored.");
            metadata = new Reference2ObjectOpenHashMap<Enum<?>, Object>();
            metadata.put(PropertyBasedDocumentFactory.MetadataKeys.ENCODING, "UTF-8");

            return createDocument(metadata, StringUtils.EMPTY);
        } catch (final JDOFatalUserException e) {
            // TODO figure out why such sentence/missing articles exist and correct the code logic.
            LOG.warn("Document corrupted exception. Ignored.");
            metadata = new Reference2ObjectOpenHashMap<Enum<?>, Object>();
            metadata.put(PropertyBasedDocumentFactory.MetadataKeys.ENCODING, "UTF-8");

            return createDocument(metadata, StringUtils.EMPTY);
        }
    }

    public Document nextDocument() throws IOException {
        if (iterator.hasNext()) {
            return nextSentence();
        } else {
            // try to get more sentences from the database:
            dbm.commitTxn();
            dbm.beginTxn();
            lowerBound = count - 1;
            final TextractorManager textractorManager =
                dbm.getTextractorManager();
            textractorManager.setRetrieveAll(true);
            iterator = textractorManager.getDocumentIterator(lowerBound, lowerBound += chunkSize);
            textractorManager.setRetrieveAll(false);
        }

        if (iterator != null && iterator.hasNext()) {
            return nextSentence();
        } else {
            LOG.info("Processed " + count + " sentences.");
            return null; // no more sentences. We are done.
        }
    }


    public final void close() throws IOException {
        iterator = null;
        if (count == 0) {
            LOG.info("0 sentence indexed.");
        } else {
            LOG.info("Filtered out " + filteredSentenceCount
                    + " sentences out of " + count + " ("
                    + (100 * filteredSentenceCount / count) + "%)");
        }
    }
}
