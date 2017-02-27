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

import java.util.Arrays;

/**
 * In parallel to DocumentTermPositions, indexed by term stores positions of
 * terms. An instance of this class is passed to DocumentIndexManager.query to
 * collect positions for positions of terms in the documents.
 */
public class TermDocumentPositions {
    /** store function from document number to document index in table positions. */
    protected int[] documents;
    protected int[][] positions;
    protected String term;

    /**
     * Returns the number of occurrence of the term in each document, or int[0]
     * if no positions.
     *
     * @return An array of the number of occurrences. One, per document, in the
     *         same order as the documents returned by the query.
     */
    public final int[] getAllOccurrenceCounts() {
        if (positions == null) {
            return new int[0];
        }

        final int[] result = new int[positions.length];

        for (int i = 0; i < positions.length; i++) {
            final int[] positionsInDocument = positions[i];
            result[i] = positionsInDocument.length;
        }
        return result;
    }


    public TermDocumentPositions() {
        super();
    }

    public TermDocumentPositions(final String term) {
        super();
        this.term = term;
    }

    public final void setValues(final TermDocumentPositions source) {
        documents = source.documents;
        positions = source.positions;
        term = source.term;
    }

    /**
     * Allocate storage for a given number of documents.
     *
     * @param numberOfDocuments Number of documents for which positions will be
     *        collected.
     */
    public final void allocate(final int numberOfDocuments) {
        documents = new int[numberOfDocuments];
        positions = new int[numberOfDocuments][];
    }

    /**
     * Set the positions of the term in the document.
     *
     * @param documentIndex Index of the document for which to set positions.
     * @param occurrences Each integer in this array represents the location of
     *        an occurence of the term in the document. Location is given in
     *        number of words from the start of the sentence.
     * @param numberOfPositions Number of positions for this document.
     */
    public final void setPositions(final int documentIndex, final int document,
            final int[] occurrences, final int numberOfPositions) {
        documents[documentIndex] = document;

        // allocate for num positions
        positions[documentIndex] = new int[numberOfPositions];

        // copy into allocated array. occurences may be larger but we copy only
        // numPositions elements.
        System.arraycopy(occurrences, 0,
                positions[documentIndex], 0, numberOfPositions);
    }

    /**
     * Set the positions of the term in the document.
     *
     * @param occurrences Each integer in this array represents the location of
     *        an occurence of the term in the document. Location is given in
     *        number of words from the start of the sentence.
     * @param numberOfPositions Number of positions for this document.
     */
    public final void setPositionsByDocument(final int document,
            final int[] occurrences, final int numberOfPositions) {
        final int documentIndex = Arrays.binarySearch(documents, document);
        positions[documentIndex] = new int[numberOfPositions];
        // copy into allocated array. occurences may be larger but we copy only
        // numPositions elements.
        System.arraycopy(occurrences, 0,
                positions[documentIndex], 0, numberOfPositions);
    }

    /**
     * Set the positions of the term in the document.
     *
     * @param occurrences Each integer in this array represents the location of
     *        an occurence of the term in the document. Location is given in
     *        number of words from the start of the sentence.
     */
    public final void setPositions(final int documentIndex,
            final int document, final int[] occurrences) {
        documents[documentIndex] = document;
        positions[documentIndex] = occurrences;
    }

    /**
     * Returns positions of the positions of the term in the document.
     *
     * @return An array of integer. Each integer represents the location of an
     *         occurence of the term in the document. Location is given in
     *         number of words from the start of the sentence.
     */
    public final int[] getPositions(final int documentIndex) {
        return positions[documentIndex];
    }

    public final void setDocuments(final int[] documents) {
        this.documents = documents;
    }

    /**
     * Returns positions of the positions of the term in the document.
     *
     * @return An array of integer. Each integer represents the location of an
     *         occurence of the term in the document. Location is given in
     *         number of words from the start of the sentence.
     */
    public final int[] getPositionsByDocument(final int document) {
        final int documentIndex = Arrays.binarySearch(documents, document);
        return getPositions(documentIndex);
    }

    public String getTerm() {
        return term;
    }

    public final int[] getDocuments() {
        return documents;
    }
}
