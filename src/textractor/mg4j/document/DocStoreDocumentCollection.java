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

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import edu.cornell.med.icb.ncbi.pubmed.PubMedInfoTool;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.document.AbstractDocumentCollection;
import it.unimi.dsi.mg4j.document.Document;
import it.unimi.dsi.mg4j.document.DocumentCollection;
import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory.MetadataKeys;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.TextractorRuntimeException;
import textractor.database.DocumentIndexManager;
import textractor.database.IndexDetails;
import textractor.mg4j.docstore.DocumentStoreReader;
import textractor.mg4j.docstore.StringPerDocumentReader;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * The DocStore document collection for all TEXT indicies.
 */
public final class DocStoreDocumentCollection extends AbstractDocumentCollection
        implements Serializable {
    /**
     * Used to log debug and informational messaages.
     */
    private static final Log LOG =
            LogFactory.getLog(DocStoreDocumentCollection.class);
    /**
     * The factory used to return {@link it.unimi.dsi.mg4j.document.Document}s.
     */
    private final AbstractTextractorDocumentFactory factory;

    /** The document index manager. */
    private final transient DocumentIndexManager docmanager;

    /** The doiReader. */
    private final transient StringPerDocumentReader doiReader;

    /**
     * A map of index aliases to document store readers. This is application
     * for "TEXT" indicies.
     */
    private final transient Map<String, DocumentStoreReader> aliasToReadersMap =
            new HashMap<String, DocumentStoreReader>();

    /**
     * Creates a document collection based on the documents in the document
     * store associated to a full text index.
     *
     * @param docmanagerVal Manager for the full text index associated with the
     *        document store.
     * @throws IOException error opening appropriate files?
     */
    public DocStoreDocumentCollection(
        final DocumentIndexManager docmanagerVal) throws IOException {
        this.docmanager = docmanagerVal;
        this.factory = docmanagerVal.getDocumentFactory();

        for (final TextractorFieldInfo fieldInfo : factory.getFieldInfoList()) {
            if (fieldInfo.getType() == DocumentFactory.FieldType.TEXT) {
                final String indexAlias = fieldInfo.getName();
                final IndexDetails indexDetails = docmanager.getIndexDetails(indexAlias);
                if (indexDetails != null) {
                    final boolean readPmids = "text".equals(indexAlias);
                    try {
                        final DocumentStoreReader reader = new DocumentStoreReader(
                                indexDetails , readPmids);
                        aliasToReadersMap.put(indexAlias, reader);
                        if (readPmids) {
                            reader.readPMIDs();
                        }
                    } catch (FileNotFoundException e) {
                        // It doesn't appear there is a docstore for this index
                        LOG.warn("No docstore located for " + indexAlias, e);
                    }
                }
            }
        }

        doiReader = StringPerDocumentReader.obtainReader(docmanager, "otmi-dois");
    }

    /**
     * Returns the current DocumentStoreReader for the "text" alias.
     *
     * @return DocumentStoreReader the current DocumentStoreReader.
     */
    public DocumentStoreReader getDocumentStoreReader() {
        return aliasToReadersMap.get("text");
    }

    /**
     * Returns the current DocumentStoreReader.
     * @param indexAlias the index alias to obtain the document store reader for.
     * @return DocumentStoreReader the current DocumentStoreReader.
     */
    public DocumentStoreReader getDocumentStoreReader(final String indexAlias) {
        return aliasToReadersMap.get(indexAlias);
    }

    /**
     * Returns the number of documents in the document store/ full text index.
     *
     * @return Number of documents (sentences) in the database.
     */
    public int size() {
        return docmanager.getDocumentNumber();
    }

    /**
     * Obtain the Document for document number index within the "text" index.
     * @param index the document number
     * @return the Document
     * @throws IOException error retrieving the document
     */
    public Document document(final int index) throws IOException {
        return factory.getDocument(stream("text", index), metadata("text", index));
    }

    /**
     * Obtain the Document for document number index within the indexAlias index.
     * @param indexAlias the indexAlias from which to retrieve the document
     * @param index the document number
     * @return the Document
     * @throws IOException error retrieving the document
     */
    public Document document(final String indexAlias, final int index) throws IOException {
        return factory.getDocument(stream(indexAlias, index), metadata(indexAlias, index));
    }

    /**
     * Obtain the Document for document number index within the "text" index,
     * but do not retrieve the document content.
     * @param index the document number
     * @return the Document (but without the content)
     * @throws IOException error retrieving the document
     */
    public Document documentNoContent(final int index) {
        return factory.getDocumentNoContent(metadata("text", index));
    }

    /**
     * Obtain the Document for document number index within the indexAlias index,
     * but do not retrieve the document content.
     * @param indexAlias the indexAlias from which to retrieve the document
     * @param index the document number
     * @return the Document (but without the content)
     * @throws IOException error retrieving the document
     */
    public Document documentNoContent(final String indexAlias, final int index) {
        return factory.getDocumentNoContent(metadata(indexAlias, index));
    }

    /**
     * Obtain the DOI for document number index within the "text" index.
     * @param index the document number
     * @return the DOI
     * @throws IOException error retrieving the document
     */
    public String doi(final int index) throws IOException {
        if (doiReader != null) {
            return doiReader.readStringForDocument(index);
        } else {
            return null;
        }
    }

    /**
     * Obtain the stream for document number index within the "text" index.
     * @param index the document number
     * @return the stream for the document
     * @throws IOException error retrieving the document
     */
    public InputStream stream(final int index) throws IOException {
        return stream("text", index);
    }

    /**
     * Obtain the stream for document number index within the indexAlias index.
     * @param indexAlias the indexAlias from which to retrieve the stream
     * @param index the document number
     * @return the stream for the document
     * @throws IOException error retrieving the document
     */
    public InputStream stream(final String indexAlias, final int index) throws IOException {
        final MutableString result = new MutableString();
        final DocumentStoreReader currentReader = aliasToReadersMap.get(indexAlias);
        if (currentReader == null) {
            return null;
        }
        currentReader.document(index, result);
        if (result.length() == 0) {
            throw new IllegalStateException("Sentence #" + index
                    + " is not found in the document store.");
        }

        return new FastBufferedInputStream(new ByteArrayInputStream(result
                .toString().getBytes("UTF-8")));
    }

    /**
     * Creates metadata for the document at index within the "text" index.
     * @param index a document index.
     * @return the metadata for the document <code>index</code>.
     * @throws IOException xx
     */
    public Reference2ObjectMap<Enum< ? >, Object> metadata(final int index) throws IOException {
        return metadata("text", index);
    }

    /**
     * Creates metadata for the document at index within the indexAlias index.
     * @param indexAlias the indexAlias for which to create metadata
     * @param index a document index.
     * @return the metadata for the document <code>index</code>.
     * @throws IOException xx
     */
    public Reference2ObjectMap<Enum< ? >, Object> metadata(
            final String indexAlias, final int index) {
        final DocumentStoreReader currentReader = aliasToReadersMap.get(indexAlias);
        if (currentReader == null) {
            return null;
        }

        ensureDocumentIndex(index);


        final long pmid = currentReader.getPMID(index);

        final Reference2ObjectOpenHashMap<Enum< ? >, Object> metadata =
                new Reference2ObjectOpenHashMap<Enum< ? >, Object>();
        metadata.put(MetadataKeys.ENCODING, "UTF-8");
        metadata.put(MetadataKeys.TITLE, Long.toString(index));

        // TODO - Marko
        final String url = PubMedInfoTool.pubmedUriFromPmid((int) pmid);
        metadata.put(MetadataKeys.URI, url);
        return metadata;
    }

    /**
     * Make a copy of this DocStoreDocumentCollection.
     * @return the copy
     */
    public DocumentCollection copy() {
        try {
            return new DocStoreDocumentCollection(docmanager);
        } catch (final IOException e) {
            LOG.error("Couldn't create copy", e);
            throw new TextractorRuntimeException(e);
        }
    }

    /**
     * Return the DocumentFactory associated with this DocStoreDocumentCollection.
     * @return the DocumentFactory
     */
    public DocumentFactory factory() {
        return factory;
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        for (final DocumentStoreReader currentReader : aliasToReadersMap.values()) {
            currentReader.close();
        }
        if (doiReader != null) {
            doiReader.close();
        }
        super.close();
    }

    public static void main(final String[] arg)
            throws IOException, JSAPException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException,
            ConfigurationException, ClassNotFoundException, URISyntaxException {
        final SimpleJSAP jsap = new SimpleJSAP(
                DocStoreDocumentCollection.class.getName(),
                "Builds a document collection from sentences in the database.",
                new Parameter[] {
                        new FlaggedOption(
                                "basename",
                                JSAP.STRING_PARSER,
                                JSAP.NO_DEFAULT,
                                JSAP.REQUIRED,
                                'b',
                                "basename",
                                "Basename of the index that this document collection "
                                        + "represents (token splitting rules will be "
                                        + "obtained from this source)")
                                .setAllowMultipleDeclarations(true),
                        new FlaggedOption("output", JSAP.STRING_PARSER,
                                JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output",
                                "The file name to use when serializing the collection object")
                });

        final JSAPResult jsapResult = jsap.parse(arg);
        if (jsap.messagePrinted()) {
            return;
        }

        // get the document factory
        final String basename = jsapResult.getString("basename");

        // create a document collection from the database
        final String filename = jsapResult.getString("output");
        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);
        final DocStoreDocumentCollection documentCollection =
                new DocStoreDocumentCollection(docmanager);

        // and serialize the object for later use
        System.out.println("Document collection contains " + documentCollection.size()
                + " documents.");
        System.out.println("Storing document collection to: " + filename);
        BinIO.storeObject(documentCollection, filename);
    }
}
