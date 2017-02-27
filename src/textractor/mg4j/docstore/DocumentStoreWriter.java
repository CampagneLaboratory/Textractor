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

import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.mg4j.index.CompressionFlags;
import it.unimi.dsi.mg4j.index.IndexIterator;
import it.unimi.dsi.mg4j.index.IndexReader;
import it.unimi.dsi.mg4j.io.OutputBitStream;
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.database.IndexDetails;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provides for writing a document store. A DocumentStore provides random
 * access to documents in a corpus. This class lets you write documents to the
 * store. After constructing this writer, you must call appendDocument() in
 * sequence for each document that is to be written to the store. Writting must
 * be done sequentially, it is not possible to insert documents between
 * documents already written. However, gaps in the index of the documents are
 * supported. For instance, if document 0 is submitted, followed by document 4,
 * documents 1,2 and 3 will be created with empty content, so that random
 * access to documents after the first gap work as expected.
 * <p/>
 * A significant storage compression advantage is realized if the writer is
 * optimized before documents are appended (optimized writer may require 50%
 * less storage for the same content). Optimization leverages the term
 * frequency data found in the inverted index. More frequent terms are
 * coded with shorter bit streams.
 * <p/>
 * User: Fabien Campagne
 * Date: Oct 29, 2005
 * Time: 11:57:37 AM
 */
public final class DocumentStoreWriter implements Closeable {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(DocumentStoreWriter.class);

    /**
     * The basename of the index that corresponds to this document store.
     */
    private final String basename;

    private OutputBitStream docStream;
    private CompressionAdaptor docBitStream;

    private OutputBitStream offsetStream;
    private CompressionAdaptor offsetBitStream;

    /**
     * Stream used to write term position information.
     */
    private OutputBitStream positionStream;

    /**
     * {@link textractor.mg4j.docstore.CompressionAdaptor} used to compress
     * term position information.
     */
    private CompressionAdaptor positionBitStream;

    private final boolean writePositions;

    /**
     * Stream used to write offsets for compressed term position information.
     */
    private OutputBitStream positionOffsetStream;

    /**
     * {@link textractor.mg4j.docstore.CompressionAdaptor} used to compress
     * offsets for term position information.
     */
    private CompressionAdaptor positionOffsetBitStream;

    private boolean optimized;

    /**
     * The previous offset value written.
     */
    private long previousOffset;

    /**
     * The previous position offset value written.
     */
    private long previousPositionOffset;

    /**
     * To compress documents.
     * @see it.unimi.dsi.mg4j.index.CompressionFlags
     */
    private static final CompressionFlags.Coding DOC_COMPRESSION =
            CompressionFlags.Coding.GAMMA;

    /**
     * To compress offsets.
     * @see it.unimi.dsi.mg4j.index.CompressionFlags
     */
    private static final CompressionFlags.Coding OFFSET_COMPRESSION =
            CompressionFlags.Coding.DELTA;

    /**
     * To compress positions.
     * @see it.unimi.dsi.mg4j.index.CompressionFlags
     */
    static final CompressionFlags.Coding POSITION_COMPRESSION =
            CompressionFlags.Coding.DELTA;

    /**
     * To compress position offsets.
     * @see it.unimi.dsi.mg4j.index.CompressionFlags
     */
    static final CompressionFlags.Coding POSITION_OFFSET_COMPRESSION =
            CompressionFlags.Coding.DELTA;

    private final IndexDetails indexDetails;
    private final int[] emptyDocument = new int[0];
    private int count;
    private final int[] documentPMIDs;
    public static final int DOCUMENT_NOT_FOUND = -1;

    /**
     * Offset in bits.
     */
    private long lastOffset;
    private long lastPositionOffset;
    private int lastDocumentIndexWritten;
    private final int lastTermIndex;

    private int[] termToSmallIndex;

    /**
     * Initialize a document store writer for the "text" index. This will write
     * pmid values. Positions will be written
     *
     * @param docmanager the document index manager
     * @throws FileNotFoundException When docstore files cannot be written with
     * the given basename.
     */
    public DocumentStoreWriter(final DocumentIndexManager docmanager) throws FileNotFoundException {
        this(docmanager.getIndexDetails("text"), true, true);
    }

    /**
     * Initialize a document store writer.
     *
     * @param indexDetailsVal the document index to write the documents for
     * @param writePmidsVal if true, the pmids file will be written
     * @param writePositionsVal if true, the positions data will be written
     * @throws FileNotFoundException When docstore files cannot be written with
     * the given basename.
     */
    public DocumentStoreWriter(
            final IndexDetails indexDetailsVal,
            final boolean writePmidsVal, final boolean writePositionsVal)
            throws FileNotFoundException {
        this.indexDetails = indexDetailsVal;
        this.basename = indexDetails.getBasename();
        this.writePositions = writePositionsVal;
        if (writePmidsVal) {
            this.documentPMIDs = new int[indexDetails.getIndex().numberOfDocuments];
            Arrays.fill(documentPMIDs, DOCUMENT_NOT_FOUND);
        } else {
            this.documentPMIDs = null;
        }
        lastTermIndex = indexDetails.getIndex().numberOfTerms;

        final String compressedDocumentsFilename =
                getDocumentDataFilename(basename);
        final String offsetInfoFilename = getOffsetFilename(basename);
        final String positionInfoFilename = getPositionFilename(basename);
        final String positionOffsetInfoFilename =
                getPositionOffsetFilename(basename);
        final FileOutputStream offsetFileOutputStream =
                new FileOutputStream(offsetInfoFilename);
        final FileOutputStream docFileOutputStream =
                new FileOutputStream(compressedDocumentsFilename);

        docStream = new OutputBitStream(docFileOutputStream);
        switch (DOC_COMPRESSION) {
            case DELTA:
                docBitStream =
                        new CompressionAdaptorDelta(docStream);
                break;
            case GAMMA:
                docBitStream =
                        new CompressionAdaptorGamma(docStream);
                break;
            default:
                throw new IllegalArgumentException("Unsupported compression: "
                        + DOC_COMPRESSION);
        }

        offsetStream = new OutputBitStream(offsetFileOutputStream);
        switch (OFFSET_COMPRESSION) {
            case DELTA:
                offsetBitStream = new CompressionAdaptorDelta(offsetStream);
                break;
            case GAMMA:
                offsetBitStream = new CompressionAdaptorGamma(offsetStream);
                break;
            default:
                throw new IllegalArgumentException("Unsupported compression: "
                        + DOC_COMPRESSION);
        }


        if (writePositions) {

            final FileOutputStream positionFileOutputStream =
                    new FileOutputStream(positionInfoFilename);
            positionStream = new OutputBitStream(positionFileOutputStream);
            switch (POSITION_COMPRESSION) {
                case DELTA:
                    positionBitStream = new CompressionAdaptorDelta(positionStream);
                    break;
                case GAMMA:
                    positionBitStream = new CompressionAdaptorGamma(positionStream);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported compression: "
                            + DOC_COMPRESSION);
            }

            final FileOutputStream positionOffsetFileOutputStream =
                    new FileOutputStream(positionOffsetInfoFilename);
            positionOffsetStream =
                    new OutputBitStream(positionOffsetFileOutputStream);
            switch (POSITION_OFFSET_COMPRESSION) {
                case DELTA:
                    positionOffsetBitStream =
                            new CompressionAdaptorDelta(positionOffsetStream);
                    break;
                case GAMMA:
                    positionOffsetBitStream =
                            new CompressionAdaptorGamma(positionOffsetStream);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported compression: "
                            + DOC_COMPRESSION);
            }
        }

        lastDocumentIndexWritten = -1;
    }

    /**
     * Create the document data filename based on the index basename.
     * @param basename the index basename
     * @return the document data filename
     */
    public static String getDocumentDataFilename(final String basename) {
        return basename + "-docstore.docs";
    }

    /**
     * Create the document offsets filename based on the index basename.
     * @param basename the index basename
     * @return the document offsets filename
     */
    public static String getOffsetFilename(final String basename) {
        return basename + "-docstore.offsets";
    }

    /**
     * The positions data filename based on the index basename.
     * @param basename the index basename
     * @return the positions data filename
     */
    public static String getPositionFilename(final String basename) {
        return basename + "-docstore-positions";
    }

    /**
     * The positions offsets filename based on the index basename.
     * @param basename the index basename
     * @return the positions offsets filename
     */
    public static String getPositionOffsetFilename(final String basename) {
        return basename + "-docstore-positions.offsets";
    }

    /**
     * Append a document to this docstore at the given document number.
     * @param documentIndex the document number
     * @param tokens the document
     * @return the number of bits written?
     * @throws IOException error appending a document
     */
    public int appendDocument(final int documentIndex, final int[] tokens)
            throws IOException {
        fillMissingDocuments(documentIndex);
        for (int i = 0; i < tokens.length; ++i) {
            if (optimized) {
                // recode term index according to term frequency
                tokens[i] = (tokens[i] == DocumentIndexManager.NO_SUCH_TERM)
                        ? lastTermIndex : termToSmallIndex[tokens[i]];
            } else {
                // just escape unknown terms as a very unfrequent term
                if (tokens[i] == DocumentIndexManager.NO_SUCH_TERM) {
                    tokens[i] = lastTermIndex;
                }
            }
        }

        // write document content.
        final int bitCount = docBitStream.write(tokens);
        // write beginning of this document.
        offsetBitStream.write(calculateOffsetDelta(lastOffset));
        lastOffset = docStream.writtenBits();
        lastDocumentIndexWritten = documentIndex;
        count++;
        return bitCount;
    }

    /**
     * Append position information to the document store.
     * @param ranges The ranges for each term in the document
     * @return The number of bits written to the document store.
     * @throws IOException If the positions cannot be written to
     */
    public int appendPositions(final List<IntRange> ranges) throws IOException {
        if (!writePositions) {
            return 0;
        }
        // the first value is the number of positions (start and end)
        int bitCount = positionBitStream.write(ranges.size());
        // then each start/end pair in order of the terms
        int lastPosition = 0;
        for (final IntRange range : ranges) {
            // store the start position of the term (delta from prior)
            final int startPosition = range.getMinimumInteger();
            bitCount += positionBitStream.write(startPosition - lastPosition);
            lastPosition = startPosition;

            // store the end position of the term (delta from prior)
            final int endPosition = range.getMaximumInteger();
            bitCount += positionBitStream.write(endPosition - lastPosition);
            lastPosition = endPosition;
        }

        positionOffsetBitStream.write(calculatePositionOffsetDelta(lastPositionOffset));
        lastPositionOffset = positionStream.writtenBits();
        return bitCount;
    }

    /**
     * Calculate the offsets delta.
     * @param offset the offset
     * @return the offsets delta
     */
    private long calculateOffsetDelta(final long offset) {
        final long delta = offset - previousOffset;
        previousOffset = offset;
        return delta;
    }

    /**
     * Calculate the positions offsets delta.
     * @param offset the offset
     * @return the positions offsets delta
     */
    private long calculatePositionOffsetDelta(final long offset) {
        final long delta = offset - previousPositionOffset;
        previousPositionOffset = offset;
        return delta;
    }

    /**
     * Append an empty document for each gap in the sequence of document
     * number. Gaps may occur if some documents were deleted from the
     * database after they were submitted. This method pads the index to
     * allow random access. Each gap add an offset but does not cost
     * document content space.
     *
     * @param documentIndex Index of the new document.
     * @throws IOException If an error occurs writting empty documents.
     */
    private void fillMissingDocuments(final int documentIndex) throws IOException {
        while (documentIndex > lastDocumentIndexWritten + 1) {
            // append empty documents until lastDocumentIndexWritten is just before documentIndex.
            appendDocument(lastDocumentIndexWritten + 1, emptyDocument);
        }
    }

    /**
     * Flush the output files.
     * @throws IOException error flushing
     */
    public void flush() throws IOException {
        docStream.flush();
        offsetStream.flush();
        if (writePositions) {
            positionStream.flush();
            positionOffsetStream.flush();
        }
    }

    /**
     * Get the value of writePositions.
     * @return true if we are writing positions
     */
    public boolean getWritePositions() {
        return writePositions;
    }

    /**
     * Finalize (close).
     * @throws Throwable error finalizing
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();

        LOG.warn("DocumentStoreWriter closed through finalize. ");
    }

    /**
     * Close.
     * @throws IOException error closing
     */
    public void close() throws IOException {
        // put some bits at the end so that the reader does not
        // reach EOF when reading past the last document.
        // The value written does not matter.
        docBitStream.write(10);
        docStream.flush();
        docStream.close();
        offsetBitStream.write(calculateOffsetDelta(lastOffset));  // write end of last document.
        offsetStream.flush();
        offsetStream.close();
        if (writePositions) {
            positionStream.flush();
            positionStream.close();
            positionOffsetBitStream.write(calculatePositionOffsetDelta(lastPositionOffset));
            positionOffsetStream.flush();
            positionOffsetStream.close();
        }
    }

    /**
     * Write document PMID info to the pmid map file.
     * @throws IOException if the file cannot be written or created
     */
    public void writePMIDs() throws IOException {
        if (documentPMIDs != null) {
            BinIO.storeInts(documentPMIDs, getPMIDMapFilename(basename));
        }
    }

    /**
     * Get the pmid map filename based on the basename.
     * @param basename the basename to create the filename for
     * @return the pmid map filename
     */
    public static String getPMIDMapFilename(final String basename) {
        return basename + "-docstore.pmids";
    }

    /**
     * Optimize the term ordering.
     * @throws IOException error optimizing
     */
    public void optimizeTermOrdering() throws IOException {
        assert indexDetails != null
                : "optimizeTermOrdering cannot be used when a document index manager "
                + "has not been provided to the constructor";

        final int numberOfTerms = indexDetails.getIndex().numberOfTerms;
        final IntList termFrequencies = new IntArrayList();
        termFrequencies.size(numberOfTerms);

        final IndexReader indexReader = indexDetails.getIndex().getReader();
        for (int term = 0; term < numberOfTerms; term++) {
            final IndexIterator indexIterator = indexReader.documents(term);
            termFrequencies.set(term, indexIterator.frequency());
        }
        indexReader.close();

        final Comparator<Integer> frequencyComparator = new AbstractIntComparator() {
            /**
             * Sorts terms in descending order of occurence frequency.
             */
            @Override
            public int compare(final int term1Index, final int term2Index) {
                return termFrequencies.get(term2Index) - termFrequencies.get(term1Index);
            }
        };


        final IntList terms = new IntArrayList();
        terms.size(numberOfTerms);
        for (int i = 0; i < numberOfTerms; ++i) {
            terms.set(i, i);
        }
        Collections.sort(terms, frequencyComparator);

        termToSmallIndex = new int[numberOfTerms + 1];      // +1 is for an unknown term
        final int[] smallIndexToTerm = new int[numberOfTerms + 1];
        for (int i = 0; i < numberOfTerms; ++i) {
            termToSmallIndex[terms.getInt(i)] = i;
            smallIndexToTerm[i] = terms.getInt(i);
        }
        // an unknown term is very infrequent.
        termToSmallIndex[numberOfTerms] = numberOfTerms;
        // recode unknown as per document index manager convention.
        smallIndexToTerm[numberOfTerms] = DocumentIndexManager.NO_SUCH_TERM;

        final String smallIndexToTermFilename =
                getSmallIndexToTermFilename(basename);
        BinIO.storeInts(smallIndexToTerm, smallIndexToTermFilename);
        optimized = true;
    }

    /**
     * Get the small index to term filename based on the basename.
     * @param basename the basename to create the filename for
     * @return the small index to term filename
     */
    public static String getSmallIndexToTermFilename(final String basename) {
        return basename + "-docstore-opt-terms.index";
    }

    /**
     * The number of documents written by this writter.
     *
     * @return The number of documents written by this writer.
     */
    public int writtenDocuments() {
        return count;
    }

    /**
     * Add a pmid for a document number.
     * @param documentNumber the document number
     * @param pmid the pmid for documentNumber
     */
    public void addDocumentPMID(final long documentNumber, final long pmid) {
        if (documentPMIDs == null) {
            return;
        }
        assert documentNumber <= documentPMIDs.length
                : "Document number " + documentNumber
                + " must be smaller or equal to documentPMIDs.length ("
                + documentPMIDs.length + ")";
        assert documentPMIDs[(int) documentNumber] == -1
                : "doc number = " + documentNumber + ": old = "
                + documentPMIDs[(int) documentNumber] + ", new = " + pmid;

        documentPMIDs[(int) documentNumber] = (int) pmid;
    }
}
