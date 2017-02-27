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

import java.io.IOException;

/**
 * Implementations of this interface can store term frequency information.
 * @author Fabien Campagne
 *         Date: Jun 25, 2006
 *         Time: 10:35:56 AM
 */
public interface FrequencyStorage {
    void setTransform(TermIndexTransform transform);

    /**
     * Returns the number of frequencies stored in this instance.
     * @return number of frequencies
     */
    int getSize();

    /**
     * Obtains the frequency value for a frequency index.
     * @param frequencyIndex Index of the frequency to read
     * @return frequency of the term stored at this frequency index
     */
    int getFrequency(final int frequencyIndex);

    /**
     * Returns the frequency for a given term.
     * @param termIndex  Index of the term.
     * @return value of the frequency for the given term index.
     */
    int getFrequencyForTerm(final int termIndex);

    /**
     * Store a term frequency at a given index in this data structure.
     * @param frequencyValue
     */
    void setFrequency(final int frequencyIndex, final int frequencyValue);

    /**
     * Obtain the term index corresponding to an index in this data structure.
     * @param frequencyIndex index in this data structure
     * @return Index of the term corresponding to frequencyIndex
     */

    int getTermIndex(final int frequencyIndex);

    /**
     * Obtain the frequency index corresponding to a term index.
     * @param termIndex index of a term in a full text index
     * @return Index of this data structure that stores the frequency of term.
     */
    int getFrequencyIndex(final int termIndex);

    /**
     * Increment the frequency by a value.
     * @param frequencyIndex Index of the frequency in this data structure.
     * @param increment Value to add to frequency at frequencyIndex
     */
    void incrementFrequency(final int frequencyIndex, int increment);

    /**
     * Returns the number of terms potentially stored in this data structure.
     * This number may be larger than the value returned by getSize() if the
     * implementation does not store frequencies for some terms.
     *
     * @return Number of terms.
     */
    int getTermNumber();

    /**
     * Save information associated with these frequency storage.
     *
     * @param basename Basename of the index associated with this frequencies
     *        storage implementation.
     */
    void save(final String basename) throws IOException;

    /**
     * Load information that may have been associated with this frequency
     * storage.
     *
     * @param basename
     */
    void load(final String basename) throws IOException;

    TermIndexTransform getTransform();
}
