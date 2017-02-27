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

import it.unimi.dsi.mg4j.io.InputBitStream;
import org.apache.commons.io.IOUtils;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.offsets.LongDenseDeltaList;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Reads Term document frequency information from files in compressed form.
 * Term document frequency information associates with each document a tally
 * of the term frequency that appear in this document. This class provides
 * an efficient way to calculate the tf part of tf-idf.
 *
 * @author Fabien Campagne
 *         Date: Jun 18, 2006
 *         Time: 12:07:40 PM
 */
public final class TermDocumentFrequencyReader implements Closeable {
    private InputBitStream tdfStream;
    private LongDenseDeltaList offsets;
    private int numberOfDocuments;
    private int maxReads;
    private FileInputStream tdfInputStream;
    private int termNumber;
    private final String basename;
    private TermIndexTransform transform;

    /**
     * Initialize a TermDocumentFrequencyWriter.
     *
     * @param docmanager The full text index that this document store will be
     *                   associated to.
     * @throws IOException When docstore files cannot be written with the given
     *                     basename.
     */
    public TermDocumentFrequencyReader(final DocumentIndexManager docmanager,
                                       final int maxReads) throws IOException {
        this(docmanager.getBasename(), docmanager.getDocumentNumber(),
                docmanager.getNumberOfTerms() + 1, maxReads);
        transform = new TermSubsetTransform(basename, 1);
    }

    /**
     * Expose the transform that is being used.
     *
     * @return TermIndexTransform the transform that is being used
     */
    public TermIndexTransform getTransform() {
        return transform;
    }

    /**
     * Initialize a TermDocumentFrequencyReader.
     *
     * @param docmanager The full text index that this document store will be
     *                   associated to.
     * @throws IOException When docstore files cannot be written with the given
     *                     basename.
     */
    public TermDocumentFrequencyReader(final DocumentIndexManager docmanager)
            throws IOException {
        this(docmanager, 50);
    }

    /**
     * This constructor is useful for JUnit tests.
     *
     * @param basename
     * @param documentNumber
     * @param termNumber
     * @param maxReads
     * @throws IOException
     */
    public TermDocumentFrequencyReader(final String basename,
                                       final int documentNumber,
                                       final int termNumber,
                                       final int maxReads) throws IOException {
        super();
        this.basename = basename;
        final String compressedTermDocumentFrequencyFilename =
                TermDocumentFrequencyWriter.getDocumentDataFilename(basename);

        tdfInputStream =
                new FileInputStream(compressedTermDocumentFrequencyFilename);
        tdfStream = new InputBitStream(new DataInputStream(tdfInputStream));

        this.termNumber = termNumber;
        this.numberOfDocuments = documentNumber;
        this.maxReads = maxReads;
        readOffsets(basename);

        transform = new UnityTransform(termNumber);
    }

    /**
     * Create storage for frequency information.
     *
     * @return Appropriately sized int array for storing term frequencies.
     */
    public int[] createFrequencyStorage() {
        return new int[getStorageSize()];
    }

    /**
     * Returns the maximum number of frequency.
     *
     * @return Number of frequencies that need to be accessible by the read
     *         method.
     */
    public int getStorageSize() {
        return termNumber + 1;
    }

    public int read(final int documentIndex, final int[] termFrequencies)
            throws IOException {
        return read(documentIndex, termFrequencies, null);
    }

    /**
     * Access term frequency information for a document. When given a document
     * number, this method retrieves the frequency of each term in this
     * document. The method returns the sum of the frequencies and add
     * individual term frequencies to the values found in termFrequencies.
     * Formally, after this method returns,
     * termFrequencies[t]final=termFrequencies[t]initial+f(t,d) where f(t,d) is
     * the frequency of term t in document d, and sum(t,d)=SUM_over_t(f(t,d)).
     *
     * @param documentIndex
     * @param termFrequencies
     * @param numDocForTerm   optional argument. This array can be allocated with
     *                        allocateNumDocForTerm(). The array has one element for each term represented
     *                        in the returned tfIdf values. The value is the number of times that a non zero
     *                        tf-Idf count was added to the term. This value therefore informs about how
     *                        many documents contain this term in documents.
     * @return Sum of f(t,d) for the document.
     * @throws IOException
     */
    public int read(final int documentIndex, final int[] termFrequencies, final int[] numDocForTerm)
            throws IOException {
        final long end = positionDataInputStream(documentIndex);
        // first term index is coded delta:
        int previousTermIndex = tdfStream.readDelta() - 1;
        int termFrequency = tdfStream.readDelta();
        termFrequencies[transform.getInitialTermIndex(previousTermIndex)]
                += termFrequency;
        int sum = termFrequency;
        for (;;) {
            // following term indices are coded as delta from previous:
            final int diff = tdfStream.readDelta();
            final long read = tdfStream.readBits();
            if (read > end) {
                break;
            }
            final int termIndex = diff + previousTermIndex;
            previousTermIndex = termIndex;
            termFrequency = tdfStream.readDelta();
            final int initialTermIndex = transform.getInitialTermIndex(termIndex);
            termFrequencies[initialTermIndex] += termFrequency;
            if (numDocForTerm != null) {
                ++numDocForTerm[initialTermIndex];
            }
            sum += termFrequency;
        }
        return sum;
    }

    private void readOffsets(final String basename) throws IOException {
        final long size = (new File(TermDocumentFrequencyWriter.getOffsetFilename(basename))).length();
        final InputBitStream offsetBitStream = new InputBitStream(TermDocumentFrequencyWriter.getOffsetFilename(basename), (int) size);
        offsets = new LongDenseDeltaList(offsetBitStream, maxReads, numberOfDocuments);
    }

    private long positionDataInputStream(final int documentIndex) throws IOException {
        final long start = offsets.getLong(documentIndex);
        final long end = offsets.getLong(documentIndex + 1);   // read until the next document.

        tdfStream.flush();
        tdfStream.readBits(0);

        tdfInputStream.getChannel().position(start >> 3);
        if ((start % 8) != 0) {
            tdfStream.skip(start % 8);
        }
        tdfStream.readBits(start);
        return end;
    }

    /**
     * Closes this reader and releases any system resources associated
     * with it. If the reader is already closed then invoking this
     * method has no effect.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public void close() throws IOException {
        IOUtils.closeQuietly(tdfInputStream);
        if (tdfStream != null) {
            tdfStream.close();
        }
    }
}
