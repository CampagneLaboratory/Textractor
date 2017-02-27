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

import it.unimi.dsi.mg4j.io.OutputBitStream;
import textractor.database.DocumentIndexManager;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Calculates and writes term document frequency in compressed form.
 *
 * @author Fabien Campagne
 *         Date: Jun 18, 2006
 *         Time: 11:25:21 AM
 */
public final class TermDocumentFrequencyWriter implements Closeable {
    private OutputBitStream tdfStream;
    private OutputBitStream offsetStream;
    private long termIdBitsWritten;
    private long termFrequencyBitsWritten;
    private long offsetBitsWritten;
    private long numberOfOffsets;
    private FrequencyStorage frequencies;
    private long termCount;
    private long frequencyNumber;
    private int documentNumber;
    private long sumFrequencies;
    private int termNumber;
    private final String basename;
    private int documentCount;
    private int minimumFrequencySupport;
    private int maximumFrequencySupport;
    /**
     * offset in bits.
     */
    private long lastOffset;
    private long previous;
    private float minDocumentFrequencyRatio;
    private float maxDocumentFrequencyRatio;

    public void setMaximumFrequencySupport(final int maximumFrequencySupport) {
        this.maximumFrequencySupport = maximumFrequencySupport;
    }

    /**
     * Set the minimum frequency required for a term to be recorded in the
     * TF-IDF information. The default value is zero and records any term that
     * occurs one or more time. Setting this value higher will reduce storage
     * space for TF-IDF.
     *
     * @param minimumFrequencySupport
     */
    public void setMinimumFrequencySupport(final int minimumFrequencySupport) {
        this.minimumFrequencySupport = minimumFrequencySupport;
    }

    /**
     * Initialize a TermDocumentFrequencyWriter.
     *
     * @param maximumTermIndex Number of terms in the underlying index.
     *                         Maximum index of a term.
     * @throws FileNotFoundException When term document frequency files cannot
     *                               be written with the given basename.
     */
    public TermDocumentFrequencyWriter(final int maximumTermIndex, final String basename) throws FileNotFoundException {
        final String compressedTermDocumentFrequencyFilename = getDocumentDataFilename(basename);
        final String offsetInfoFilename = getOffsetFilename(basename);
        this.basename = basename;
        final FileOutputStream offsetFileOutputStream = new FileOutputStream(offsetInfoFilename);
        tdfStream = new OutputBitStream(compressedTermDocumentFrequencyFilename);
        offsetStream = new OutputBitStream(offsetFileOutputStream);

        this.termNumber = maximumTermIndex;
        frequencies = new FrequencyStorageImpl(maximumTermIndex);

    }

    /**
     * Initialize a TermDocumentFrequencyWriter.
     *
     * @param docmanager The full text index that this document store will be
     *                   associated to.
     * @throws FileNotFoundException When docstore files cannot be written with
     *                               the given basename.
     */
    public TermDocumentFrequencyWriter(final DocumentIndexManager docmanager,
                                       final float minDocumentFrequencyRatio,
                                       final float maxDocumentFrequencyRatio)
            throws FileNotFoundException {

        this(docmanager.getNumberOfTerms(), docmanager.getBasename());
        this.documentNumber = docmanager.getDocumentNumber();


        final TermIndexTransform transform =
                new TermSubsetTransform(new UnityTransform(this.termNumber + 1)) {
                    @Override
                    boolean includeTerm(final int termIndex) {
                        final int documentFrequency;
                        try {
                            documentFrequency = docmanager.frequency(termIndex);
                        } catch (IOException e) {
                            return false;
                        }
                        return documentFrequency < maxDocumentFrequencyRatio * documentNumber &&
                                documentFrequency > minDocumentFrequencyRatio * documentNumber;
                    }
                };
        frequencies = new FrequencyStorageImpl(transform);
        this.minDocumentFrequencyRatio = minDocumentFrequencyRatio;
        this.maxDocumentFrequencyRatio = maxDocumentFrequencyRatio;
    }

    public static String getDocumentDataFilename(final String basename) {
        return basename + "-term-doc-freqs.data";
    }

    public static String getOffsetFilename(final String basename) {
        return basename + "-term-doc-freqs.offsets";
    }

    public static String getReducedIndexMappingFilename(final String basename) {
        return basename + "-term-doc-freqs.projection";
    }


    protected int getTermDocumentFrequency(final int termIndex) {
        return frequencies.getFrequencyForTerm(termIndex);
    }

    public int appendDocument(final int documentIndex, final int[] tokens) throws IOException {
        calculateTermDocumentFrequency(tokens);
        int previousFrequencyIndex = -1;
        ++documentCount;
        // write first term index with !=0 frequency in gamma coding
        // write following term index in delta coding.

        for (int frequencyIndex = 0; frequencyIndex < frequencies.getSize(); ++frequencyIndex) {

            if (frequencies.getFrequency(frequencyIndex) > minimumFrequencySupport) {
                if (previousFrequencyIndex == -1) {
                    termIdBitsWritten += tdfStream.writeDelta(frequencyIndex + 1);
                } else {
                    final int value = frequencyIndex - previousFrequencyIndex;
                    termIdBitsWritten += tdfStream.writeDelta(value);

                }
                ++termCount;
                previousFrequencyIndex = frequencyIndex;
                // write frequency for term in document  in delta.

                termFrequencyBitsWritten += tdfStream.writeDelta(frequencies.getFrequency(frequencyIndex));
                sumFrequencies += frequencies.getFrequency(frequencyIndex);
                ++frequencyNumber;

            }
        }
        offsetBitsWritten += offsetStream.writeDelta(calculateDelta(lastOffset));  // write beginning of this document.
        ++numberOfOffsets;
        lastOffset = tdfStream.writtenBits();
        return (int) tdfStream.writtenBits();
    }


    private int calculateDelta(final long lastOffset) {
        final long delta = lastOffset - previous;
        previous = lastOffset;
        return (int) delta;
    }

    private void calculateTermDocumentFrequency(final int[] document) {

        final int docLength = document.length;

        for (int i = 0; i < frequencies.getSize(); ++i) {
            frequencies.setFrequency(i, 0);
        }

        for (int docPositionIndex = 0; docPositionIndex < docLength; ++docPositionIndex) {
            final int originalTermIndex = document[docPositionIndex];
            if (originalTermIndex != DocumentIndexManager.NO_SUCH_TERM) {
                final int frequencyIndex = frequencies.getFrequencyIndex(originalTermIndex);
                if (frequencyIndex != DocumentIndexManager.NO_SUCH_TERM) {
                    frequencies.incrementFrequency(frequencyIndex, 1);
                }
            }
        }
    }

    public void close() throws IOException {
        tdfStream.writeDelta(10); // put some bits at the end so that the reader does not
// reach EOF when reading past the last document. The value written does not matter.
        tdfStream.flush();
        tdfStream.close();
        offsetStream.writeDelta(calculateDelta(lastOffset));  // write end of last document.
        offsetStream.flush();
        offsetStream.close();
        frequencies.save(basename);

    }


    public void printStatistics(final PrintWriter writer) {
        final float bitsPerTermID = (float) termIdBitsWritten / (float) frequencyNumber;
        writer.println("Minimum support: " + (minDocumentFrequencyRatio * documentNumber));
        writer.println("Maximum support: " + (maxDocumentFrequencyRatio * documentNumber));
        writer.println("Number of documents: " + documentNumber);
        writer.println("Number of document processed: " + documentCount);
        writer.println("Number of frequencies: " + frequencyNumber);
        writer.println("Number of included terms: " + frequencies.getSize());
        writer.println("Number of initial terms: " + frequencies.getTermNumber());

        writer.println("Bits per term ID: " + bitsPerTermID);
        final float bitsPerFrequency = ((float) termFrequencyBitsWritten / (float) frequencyNumber);
        writer.println("Bits per frequency: " + bitsPerFrequency);
        writer.println("Average frequency: " + (float) sumFrequencies / (float) frequencyNumber);
        writer.println("Average term per document: " + termCount / documentCount);
        final float bitsPerDocument = ((float) termFrequencyBitsWritten + (float) termIdBitsWritten) / documentCount;
        writer.println("Bytes per document: " + bitsPerDocument / 8f);

        writer.flush();
    }

    public void printSelectedTerms(final PrintWriter pw, final DocumentIndexManager docmanager) throws IOException {
        pw.println("frequencyIndex\ttermIndex\tterm\tfrequency\trelativeFrequency");
        for (int frequencyIndex = 0; frequencyIndex < this.frequencies.getSize(); frequencyIndex++) {
            final int termIndex = frequencies.getTermIndex(frequencyIndex);
            pw.println(frequencyIndex + "\t" + termIndex + "\t" +
                    docmanager.termAsCharSequence(termIndex) + "\t" +
                    docmanager.frequency(termIndex) + "\t" +
                    ((float) docmanager.frequency(termIndex) / (float) docmanager.getDocumentNumber()));
        }
        pw.flush();
    }
}
