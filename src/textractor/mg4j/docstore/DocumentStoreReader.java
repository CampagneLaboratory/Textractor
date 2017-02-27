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
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.mg4j.io.InputBitStream;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.database.IndexDetails;
import textractor.mg4j.offsets.LongDenseDeltaList;
import textractor.mg4j.offsets.LongDenseList;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Access a DocumentStore on disk.
 * User: Fabien Campagne
 * Date: Oct 29, 2005
 * Time: 5:17:17 PM
 * To change this template use File | Settings | File Templates.
 */
public final class DocumentStoreReader implements Closeable {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(DocumentStoreReader.class);
    /**
     * The basename of this document store.
     */
    private final String basename;
    private final IndexDetails indexDetails;
    private final int numberOfTerms;
    private LongDenseList offsets;
    private int[] smallIndexToTerms;

    private FileInputStream documentInputStream;
    private InputBitStream documentData;

    /**
     * Stream used to read compressed term position information.
     */
    private InputBitStream positionStream;

    /**
     * Offsets for compressed term position information.
     */
    private LongDenseList positionOffsets;

    /**
     * The number of documents in this document store.
     */
    private final int numberOfDocuments;
    public static final int DOCUMENT_NOT_FOUND =
        DocumentStoreWriter.DOCUMENT_NOT_FOUND;
    private final int maxReads;
    private int[] mapDoc2PMID;
    private final boolean readPmids;
    private static final String SOME_WORD = "<someword>";
    private final List<Integer> freqDocument = new IntArrayList();

    /**
     * Object to sync reading from the position file.
     */
    private final Object positionReadSync = new Object();

    /**
     * Indicates that this document store contains raw position data for the
     * terms.
     */
    private final boolean positionsAvailable;

    /**
     * Initialize a document store reader for the "text" index. This will read
     * pmid values.
     *
     * @param docmanager the document index manager
     * @throws IOException When docstore files cannot be read with
     * the given basename.
     */
    public DocumentStoreReader(final DocumentIndexManager docmanager) throws IOException {
        this(docmanager.getIndexDetails("text"), 50, true);
    }

    /**
     * Initialize a document store reader for the inex specified by
     * indexDetailsVal. It is only necessary to "readPmids" if reading from the "text" index.
     *
     * @param indexDetailsVal the document index to read the documents for
     * @param readPmidsVal if true, the pmids file will be read
     * @throws IOException When docstore files cannot be read with
     * the given basename.
     */
    public DocumentStoreReader(
            final IndexDetails indexDetailsVal, final boolean readPmidsVal) throws IOException {
        this(indexDetailsVal, 50, readPmidsVal);
    }

    /**
     * Initialize a document store reader for the inex specified by
     * indexDetailsVal. It is only necessary to "readPmids" if reading from the "text" index.
     *
     * @param indexDetailsVal the document index to read the documents for
     * @param maxReads xx
     * @param readPmidsVal if true, the pmids file will be read
     * @throws IOException When docstore files cannot be read with
     * the given basename.
     */
    public DocumentStoreReader(
            final IndexDetails indexDetailsVal, final int maxReads,
            final boolean readPmidsVal) throws IOException {
        super();
        this.readPmids = readPmidsVal;
        this.indexDetails = indexDetailsVal;
        this.basename = indexDetails.getBasename();
        this.numberOfTerms = indexDetails.getIndex().numberOfTerms;
        this.numberOfDocuments = indexDetails.getIndex().numberOfDocuments;
        this.maxReads = maxReads;
        readOffsets();
        readSmallIndexToTerms();
        openDocumentDataFile();

        final File positionFile =
                new File(DocumentStoreWriter.getPositionFilename(basename));
        final File postionOffsetsFile =
                new File(DocumentStoreWriter.getPositionOffsetFilename(basename));
        positionsAvailable =  positionFile.exists() && positionFile.canRead()
               && postionOffsetsFile.exists() && postionOffsetsFile.canRead();
    }

    /**
     * Read PMID information. This operation is optional. If performed,
     * getPMID can be used.
     *
     * @throws IOException if the pmid map file cannot be read
     * @see #getPMID(int)
     */
    public void readPMIDs() throws IOException {
        if (readPmids && mapDoc2PMID == null) {
            mapDoc2PMID =
                BinIO.loadInts(DocumentStoreWriter.getPMIDMapFilename(basename));
        }
    }

    /**
     * Retrieve the PMID that corresponds to a given document number.
     * This method requires that PMID information has been read.
     *
     * @see #readPMIDs()
     * @param documentNumber Document for which the PMID is sought
     * @return PMID of the article corresponding to the document number
     */
    public long getPMID(final int documentNumber) {
        if (!readPmids) {
             return DOCUMENT_NOT_FOUND;
        }

        assert documentNumber < Integer.MAX_VALUE
            : "Document numbers must be smaller than maximum Integer value.";
        assert mapDoc2PMID != null
            : "readPMIDs must be called before getPMID can succeed.";

        // If previous assertion turns incorrect, code
        return mapDoc2PMID[documentNumber];
    }

    /**
     * Retrieve the document number that corresponds to a given PMID.
     * This method requires that PMID information has been read.
     * Note also that this is an O(n) operation where n is the number
     * of documents in the store.
     *
     * @see #readPMIDs()
     * @param pmid PMID for which the document number is sought
     * @return document number of the article corresponding to the PMID
     * or #DOCUMENT_NOT_FOUND (<code>-1</code>) if not found.
     */
    public int getDocumentNumber(final long pmid) {
        if (!readPmids) {
             return DOCUMENT_NOT_FOUND;
        }

        assert mapDoc2PMID != null
            : "readPMIDs must be called before getDocumentNumber can succeed.";

        return ArrayUtils.indexOf(mapDoc2PMID, (int) pmid);
    }

    /**
     * Open the document data file.
     * @throws IOException error opening data file.
     */
    private void openDocumentDataFile() throws IOException {
        documentInputStream = new FileInputStream(
                DocumentStoreWriter.getDocumentDataFilename(basename));
        documentData = new InputBitStream(new DataInputStream(documentInputStream));
    }

    /**
     * Read the small index to terms.
     * @throws IOException error reading data file.
     */
    private void readSmallIndexToTerms() throws IOException {
        smallIndexToTerms = new int[numberOfTerms + 1];   // +1 stores unknown term
        try {
            BinIO.loadInts(new DataInputStream(new FileInputStream(
                    DocumentStoreWriter.getSmallIndexToTermFilename(basename))), smallIndexToTerms);
        } catch (final FileNotFoundException e) {
            // if no such file, terms were not optimized so smallIndexToTerms must be the invariant:
            for (int i = 0; i < smallIndexToTerms.length; i++) {
                smallIndexToTerms[i] = i;
            }
            smallIndexToTerms[smallIndexToTerms.length - 1] = DocumentIndexManager.NO_SUCH_TERM;
            // map back unknown term to Document Index manager convention.
        }
    }

    /**
     * Read the offets.
     * @throws IOException error reading data file.
     */
    private void readOffsets() throws IOException {
        final long size = (new File(DocumentStoreWriter.getOffsetFilename(basename))).length();
        final InputBitStream offsetBitStream =
                new InputBitStream(DocumentStoreWriter.getOffsetFilename(basename), (int) size);
        offsets = new LongDenseDeltaList(offsetBitStream, maxReads, numberOfDocuments);
    }

    /**
     * Read position offets.
     * @throws IOException error reading data file.
     */
    private void readPostionOffsets() throws IOException {
        final String positionOffsetFilename =
                DocumentStoreWriter.getPositionOffsetFilename(basename);
        final long size = new File(positionOffsetFilename).length();
        final InputBitStream offsetBitStream =
                new InputBitStream(positionOffsetFilename, (int) size);
        positionOffsets =
                new LongDenseDeltaList(offsetBitStream, maxReads, numberOfDocuments);
    }

    /**
     * Retrieves a document from this document store.
     *
     * @param documentIndex the document index number to read
     * @return The text of this document.
     * @throws IOException error reading the data
     */
    public MutableString document(final int documentIndex) throws IOException {
        final MutableString result = new MutableString();
        document(documentIndex, result);
        return result;
    }

    /**
     * Retrieves a document from this document store.
     *
     * @param documentIndex the document index number to read
     * @param result        Text of the document will be <em>appended</em>
     *                      to this MutableString.
     * @throws IOException error reading the data
     */
    public void document(final int documentIndex, final MutableString result) throws IOException {
        final long start = offsets.getLong(documentIndex);
        final long end = offsets.getLong(documentIndex + 1);   // read until the next document.

        documentData.flush();
        documentData.readBits(0);

        documentInputStream.getChannel().position(start >> 3);
        if ((start % 8) != 0) {
            documentData.skip(start % 8);
        }
        documentData.readBits(start);

        final char separator = ' ';

        for (;;) {
            final int token = documentData.readGamma();
            final long read = documentData.readBits();
            if (read > end) {
                break;
            }
            final int term = smallIndexToTerms[token];

            if (term == DocumentIndexManager.NO_SUCH_TERM) {
                result.append(SOME_WORD);
            } else {
                result.append(indexDetails.termAsCharSequence(term));
            }
            result.append(separator);
        }
    }

    /**
     * Retrieves a document from this document store. The document is
     * represented as a list of integer, where each integer codes for a word.
     * Word coding is given by the document manager this document store is
     * associated to.
     *
     * @param documentIndex Index of the document to retrieve.
     * @param result        Words of the document will be <em>appended</em> to
     *                      this IntList.
     * @return nothing useful? a zero, it appears.
     * @throws IOException error reading the data
     */
    public int document(final int documentIndex, final List<Integer> result)
            throws IOException {
        final long start = offsets.getLong(documentIndex);
        final long end = offsets.getLong(documentIndex + 1);   // read until the next document.

        documentData.flush();
        documentData.readBits(0);

        documentInputStream.getChannel().position(start >> 3);
        if ((start % 8) != 0) {
            documentData.skip(start % 8);
        }
        documentData.readBits(start);

        final int count = 0;
        for (;;) {
            final int token = documentData.readGamma();
            final long read = documentData.readBits();
            if (read > end) {
                break;
            }
            final int term = smallIndexToTerms[token];
            result.add(term);
        }
        return count;
    }

    /**
     * Get term permutation.
     */
    public int[] getTermPermutation() {
        final int[] termPermutation = new int[smallIndexToTerms.length];

        for (int i = 0; i < termPermutation.length; ++i) {
            final int termIndex = smallIndexToTerms[i];
            if (termIndex != DocumentIndexManager.NO_SUCH_TERM) {
                termPermutation[termIndex] = i;
            }
        }
        return termPermutation;
    }

    /**
     * Get frequencies.
     */
    public int frequencies(
            final int documentIndex, final int[] termFrequencies, final int[] numDocForTerm)
            throws IOException {
        freqDocument.clear();
        document(documentIndex, freqDocument);

        int sum = 0;
        for (final int termIndex : freqDocument) {
            if (termIndex != DocumentIndexManager.NO_SUCH_TERM) {
                termFrequencies[termIndex] += 1;
                if (numDocForTerm != null) {
                    ++numDocForTerm[termIndex];
                }
            }
            ++sum;
        }
        return sum;
    }

    /**
     * Get the range of positions that this term occupied in the original
     * document source.
     * @param documentIndex index of the document to get the positions for
     * @return A range of positons.  This will be null if this document
     * store does not contain positon information
     * @throws IOException if there is a problem reading the positions
     * @see #isPositionsAvailable()
     */
    public List<IntRange> positions(final int documentIndex) throws IOException {
        List<IntRange> ranges = null;
        if (positionsAvailable) {
            synchronized (positionReadSync) {
                // lazy load the offsets
                if (positionOffsets == null) {
                    readPostionOffsets();
                }
                // and the position stream
                if (positionStream == null) {
                    positionStream = new InputBitStream(
                            DocumentStoreWriter.getPositionFilename(basename));
                }

                final long offset = positionOffsets.getLong(documentIndex);
                positionStream.position(offset);

                final int numberOfPositions;
                switch (DocumentStoreWriter.POSITION_COMPRESSION) {
                    case DELTA:
                        numberOfPositions = positionStream.readDelta();
                        break;
                    case GAMMA:
                        numberOfPositions = positionStream.readGamma();
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unsupported compression: "
                                        + DocumentStoreWriter.POSITION_COMPRESSION);
                }

                ranges = new ArrayList<IntRange>(numberOfPositions);

                int lastPosition = 0;
                for (int i = 0; i < numberOfPositions; i++) {
                    final int start;
                    final int end;

                    switch (DocumentStoreWriter.POSITION_COMPRESSION) {
                        case DELTA:
                            start = positionStream.readDelta() + lastPosition;
                            lastPosition = start;
                            end = positionStream.readDelta() + lastPosition;
                            lastPosition = end;
                            break;
                        case GAMMA:
                            start = positionStream.readGamma();
                            end = start + positionStream.readGamma();
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Unsupported compression: "
                                            + DocumentStoreWriter.POSITION_COMPRESSION);
                    }

                    final IntRange range = new IntRange(start, end);
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("range of " + i + " is " + range);
                    }
                    ranges.add(range);
                }
            }
        }

        return ranges;
    }

    /**
     * Does this document store contain postion information?
     * @return true if the document store has postion information
     */
    public boolean isPositionsAvailable() {
        return positionsAvailable;
    }

    /**
     * Get the total number of terms in the index associated with this document
     * store.
     * @return the number of terms
     */
    public int getNumberOfTerms() {
        return numberOfTerms;
    }

    /**
     * Get the total number of documents in the index associated with this
     * document store.
     * @return the number of documents
     */
    public int getNumberOfDocuments() {
        return numberOfDocuments;
    }

    /**
     * Closes this reader and releases any system resources associated
     * with it. If the reader is already closed then invoking this
     * method has no effect.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public void close() throws IOException {
        IOUtils.closeQuietly(documentInputStream);
        if (documentData != null) {
            documentData.close();
        }
        if (positionStream != null) {
            positionStream.close();
        }
    }
}
