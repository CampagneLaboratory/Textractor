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
 * Storage for term frequencies.
 * @author Fabien Campagne
 *         Date: Jun 25, 2006
 *         Time: 11:27:00 AM
 */
public class FrequencyStorageImpl implements FrequencyStorage {
    private int[] frequencies;
    private TermIndexTransform transform;

    public FrequencyStorageImpl(final int maxTermIndex) {
        super();
        setTransform(new UnityTransform(maxTermIndex + 1));
    }

    public FrequencyStorageImpl(final TermIndexTransform transform) {
        super();
        setTransform(transform);
    }

    public void setTransform(final TermIndexTransform transform) {
        this.transform = transform;
        frequencies = new int[transform.getFinalSize()];
    }

    public int getSize() {
        return frequencies.length;
    }

    public int getFrequency(final int frequencyIndex) {
        return frequencies[frequencyIndex];
    }

    public int getFrequencyForTerm(final int termIndex) {
        return getFrequency(getFrequencyIndex(termIndex));
    }

    public void setFrequency(final int frequencyIndex, final int frequencyValue) {
        frequencies[frequencyIndex] = frequencyValue;
    }

    public int getTermIndex(final int frequencyIndex) {
        return transform.getInitialTermIndex(frequencyIndex);
    }

    public int getFrequencyIndex(final int termIndex) {
        return transform.getTransformedIndex(termIndex);
    }

    public void incrementFrequency(final int frequencyIndex, final int increment) {
        frequencies[frequencyIndex] += increment;
    }

    public int getTermNumber() {
        return transform.getInitialSize();
    }

    public void save(final String basename) throws IOException {
        transform.save(basename);
    }

    public void load(final String basename) throws IOException {
        transform.load(basename);
    }

    public TermIndexTransform getTransform() {
        return transform;
    }
}
