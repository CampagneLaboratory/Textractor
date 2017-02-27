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

package textractor.mg4j.docstore;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.mg4j.document.DocumentSequence;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.Properties;
import junit.framework.TestCase;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.TermFrequency;
import textractor.mg4j.document.DocStoreDocumentCollection;
import textractor.mg4j.io.ProteinWordSplitterReader;
import textractor.tools.BuildDocumentIndexFromTextDocuments;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Tests the DocumentStore.
 * User: Fabien Campagne
 * Date: Oct 29, 2005
 * Time: 1:16:14 PM
 * To change this template use File | Settings | File Templates.
 */
public final class TestDocumentStore extends TestCase {
    public void testDocStoreConsistency() throws Exception {
        final String basename = "index/docstore-test";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        final String[] documents = {
                "Hello, this is the text to index, first document",
                "second document, in a string buffer",
                "third document, a mutable string",
                "Hello, Hello, Hello",
        };

        indexBuilder.index(documents);

        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));

        final DocumentStoreWriter docStoreWriter = new DocumentStoreWriter(docmanager);
        docStoreWriter.optimizeTermOrdering();

        for (int n = 0; n < documents.length; n++) {
            docStoreWriter.appendDocument(n, docmanager.extractTerms(documents[n]));
        }

        docStoreWriter.close();

        final DocumentStoreConsistencyChecker checker =
                new DocumentStoreConsistencyChecker(docmanager);
        assertTrue(checker.checkConsisentcyWithSearchTerm("document"));
        docmanager.close();
    }

    public void testWriteText() throws Exception {
        final String basename = "index/docstore-test";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        final String[] documents = {
                "Hello, this is the text to index, first document",
                "second document, in a string buffer",
                "third document, a mutable string",
                "Hello, Hello, Hello",
        };

        indexBuilder.index(documents);
        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);
        final Iterator<String> it = docmanager.getTerms();
        final TermFrequency termFrequency = new TermFrequency();
        while (it.hasNext()) {
            final String term = it.next();
            docmanager.frequency(new String[]{term}, termFrequency);
        }

        // convert documents to term indices using terms coding in the inverted
        // index
        final int[][] docTokens = new int[documents.length][0];

        for (int i = 0; i < docTokens.length; ++i) {
            docTokens[i] = docmanager.extractTerms(documents[i]);
        }

        boolean optimize = false;
        final int bitsWritten = writeToStore(docmanager, optimize, docTokens);

        assertTrue(bitsWritten > 0);
        optimize = true;
        final int bitsWrittenOptimized =
                writeToStore(docmanager, optimize, docTokens);

        assertTrue(bitsWrittenOptimized > 0);
        assertTrue("Optimizing must reduce the size of the compressed document representation.", bitsWrittenOptimized < bitsWritten);

        final DocumentStoreReader reader = new DocumentStoreReader(docmanager);
        final MutableString result = new MutableString();
        final MutableString splittedText = new MutableString();
        final IntList intResult = new IntArrayList();
        for (int i = 0; i < documents.length; i++) {
            result.setLength(0);
            // result in a MutableString:
            reader.document(i, result);
            assertTrue(result.length() > 0);
            splittedText.setLength(0);
            docmanager.splitText(documents[i], splittedText);   // approximate term processing on this text.
            assertTrue(result.toString().toLowerCase().compareTo(splittedText.toLowerCase().toString()) == 0);

            // result in a IntList:
            intResult.size(0);   // clear list before collecting each document.
            reader.document(i, intResult);
            assertTrue(result.length() > 0);
            final Iterator<Integer> intIterator = intResult.iterator();
            final int[] extractedTerms = docmanager.extractTerms(documents[i]);
            int j = 0;
            while (intIterator.hasNext()) {
                final int word = intIterator.next();
                assertEquals("word must match for document " + i,
                        word, extractedTerms[j++]);
            }
            assertEquals("Number of words must match.",
                    extractedTerms.length, j);
        }
        reader.close();
        docmanager.close();
    }

    public void testDocumentPadding() throws Exception {
        final String basename = createDocumentIndex();
        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);
        final DocumentStoreWriter writer = new DocumentStoreWriter(docmanager);
        final int[] tokens_doc_2 = {1, 2};
        final int[] tokens_doc_5 = {1, 2, 3};

        writer.appendDocument(2, tokens_doc_2);
        writer.appendDocument(5, tokens_doc_5);
        assertEquals(writer.writtenDocuments(), 6); // 0, 1, 2, 3, 4, 5  -> 6 docs
        writer.close();
        docmanager.close();
    }

    /**
     * This test is really artificial. Please note that the document set used
     * to create the index has 9 documents numbered 0-8, but the content does
     * not match was is put in the document store. What matters is the number
     * of documents, because the docStoreWriter obtains this information from
     * the DocumentIndexManager.
     */
    public void testDoc2Pmid() throws Exception {
        final String basename = "index/docstore-test2";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        final String[] documents = {
                "0", "1", "2", "3", "4", "5", "6", "7", "8",
        };

        indexBuilder.index(documents);

        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);
        // now write documents to the store:
        final DocumentStoreWriter writer = new DocumentStoreWriter(docmanager);
        final int[] tokens_doc_0 = {1, 2};
        final int[] tokens_doc_dummy = {};
        final int[] tokens_doc_1 = {1, 2, 3};
        final int[] tokens_doc_5 = {1, 2, 3};
        final int[] tokens_doc_8 = {1, 2, 3};

        writer.appendDocument(0, tokens_doc_0);
        writer.appendDocument(1, tokens_doc_1);
        writer.appendDocument(2, tokens_doc_dummy);
        writer.appendDocument(3, tokens_doc_dummy);
        writer.appendDocument(4, tokens_doc_dummy);
        writer.appendDocument(5, tokens_doc_5);
        writer.appendDocument(6, tokens_doc_dummy);
        writer.appendDocument(7, tokens_doc_dummy);
        writer.appendDocument(8, tokens_doc_8);

        final long pmid_0 = 121212L;
        writer.addDocumentPMID(0, pmid_0);
        final long pmid_1 = 1212L;
        writer.addDocumentPMID(1, pmid_1);
        final long pmid_5 = 348934L;
        writer.addDocumentPMID(5, pmid_5);
        final long pmid_8 = 342390234L;
        writer.addDocumentPMID(8, pmid_8);
        writer.writePMIDs();
        writer.close();

        final DocumentStoreReader reader = new DocumentStoreReader(docmanager);
        reader.readPMIDs();
        assertEquals(pmid_0, reader.getPMID(0));
        assertEquals(0, reader.getDocumentNumber(pmid_0));
        assertEquals(pmid_1, reader.getPMID(1));
        assertEquals(1, reader.getDocumentNumber(pmid_1));
        assertEquals(pmid_5, reader.getPMID(5));
        assertEquals(5, reader.getDocumentNumber(pmid_5));
        assertEquals(pmid_8, reader.getPMID(8));
        assertEquals(8, reader.getDocumentNumber(pmid_8));
        assertEquals(DocumentStoreReader.DOCUMENT_NOT_FOUND, reader.getPMID(2));
        assertEquals(DocumentStoreReader.DOCUMENT_NOT_FOUND, reader.getPMID(3));
        assertEquals(DocumentStoreReader.DOCUMENT_NOT_FOUND, reader.getPMID(4));
        assertEquals(DocumentStoreReader.DOCUMENT_NOT_FOUND, reader.getPMID(6));
        assertEquals(DocumentStoreReader.DOCUMENT_NOT_FOUND, reader.getPMID(7));
        assertEquals(DocumentStoreWriter.DOCUMENT_NOT_FOUND, reader.getPMID(2));
        assertEquals(DocumentStoreReader.DOCUMENT_NOT_FOUND, reader.getDocumentNumber(42));
        reader.close();

        // now, test the DocStoreDocumentCollection:
        final Properties properties = new Properties();
        properties.addProperty(
                PropertyBasedDocumentFactory.MetadataKeys.WORDREADER,
                ProteinWordSplitterReader.class.getName());
//        final Reference2ObjectMap<Enum,Object> metadata =
//                new Reference2ObjectOpenHashMap<Enum,Object>();
//        metadata.put(PropertyBasedDocumentFactory.MetadataKeys.WORDREADER,
//                ProteinWordSplitterReader.class);
        final DocumentSequence documentSequence =
            new DocStoreDocumentCollection(docmanager);
        documentSequence.close();
        docmanager.close();
    }


    public void testWriteUnknownTerms() throws Exception {
        testUnknownTerms(/* optimization */ true);
        testUnknownTerms(/* optimization */ false);
    }

    /**
     * This test is really artificial. Please note that the document set used
     * to create the index has 9 documents numbered 0-1, but the content does
     * not match was is put in the document store. What matters is the number
     * of documents, because the docStoreWriter obtains this information from
     * the DocumentIndexManager.
     */
    private void testUnknownTerms(final boolean optimizeTerms)
            throws Exception {
        final String basename = "index/docstore-test2";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        final String[] documents = {
                "1", "2 3",
        };
        indexBuilder.index(documents);

        final DocumentIndexManager docmanager =
                new DocumentIndexManager(basename);

        // now write documents to the store:
        final DocumentStoreWriter writer = new DocumentStoreWriter(docmanager);
        if (optimizeTerms) {
            writer.optimizeTermOrdering();
        }
        final int[] tokens_doc_0 = {1, DocumentIndexManager.NO_SUCH_TERM};
        final int[] tokens_doc_1 = {DocumentIndexManager.NO_SUCH_TERM, 2, 3};

        writer.appendDocument(0, tokens_doc_0);
        writer.appendDocument(1, tokens_doc_1);
        writer.close();

        final DocumentStoreReader reader = new DocumentStoreReader(docmanager);
        final List<Integer> doc0 = new IntArrayList();
        reader.document(0, doc0);
        assertEquals(tokens_doc_0.length, doc0.size());
        assertEquals(DocumentIndexManager.NO_SUCH_TERM, (int) doc0.get(1));

        final IntList doc1 = new IntArrayList();
        reader.document(1, doc1);
        assertEquals(tokens_doc_1.length, doc1.size());
        assertEquals(DocumentIndexManager.NO_SUCH_TERM, (int) doc1.get(0));
        reader.close();
        docmanager.close();
    }

    private String createDocumentIndex() throws Exception {
        final String basename = "index/docstore-test";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        final String[] documents = {
                "Hello, this is the text to index, first document",
                "second document, in a string buffer",
                "third document, a mutable string",
                "Hello, Hello, Hello",
        };

        indexBuilder.index(documents);
        return basename;
    }

    private int writeToStore(final DocumentIndexManager docmanager,
                             final boolean optimize, final int[][] docTokens)
            throws IOException {
        // now write documents to the store:
        final DocumentStoreWriter writer = new DocumentStoreWriter(docmanager);

        if (optimize) {
            writer.optimizeTermOrdering();
        }
        int i = 0;
        int bitsWritten = 0;
        for (final int [] doc : docTokens) {
            bitsWritten += writer.appendDocument(i++, doc);
        }
        writer.close();
        return bitsWritten;
    }
}
