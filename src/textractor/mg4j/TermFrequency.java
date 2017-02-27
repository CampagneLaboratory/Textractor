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

package textractor.mg4j;

/**
 * Holds frequency information for a term.
 * User: Fabien Campagne
 * Date: Oct 29, 2005
 * Time: 2:12:57 PM
 */
public final class TermFrequency {
    /** The number of documents the term is found in. */
    private int documentFrequency;

    /** The number of times the term occurs in documents. */
    private int occurenceFrequency;

    /**
     * Document frequency is the number of documents the term is found in.
     * Multiple occurences in a given document count for one document occurence.
     * @return The document frequency for the term.
     */
    public int getDocumentFrequency() {
        return documentFrequency;
    }

    public void setDocumentFrequency(final int documentFrequency) {
        this.documentFrequency = documentFrequency;
    }

    /**
     * Occurence frequency is the number of times the term occurs in documents.
     * Multiple occurences in a given document are counted.
     * @return The occurence frequency for the term.
     */
    public int getOccurenceFrequency() {
        return occurenceFrequency;
    }

    public void setOccurenceFrequency(final int occurenceFrequency) {
        this.occurenceFrequency = occurenceFrequency;
    }
}
