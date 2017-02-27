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

package textractor.datamodel;

import textractor.database.DocumentIndexManager;

/**
 * User: Fabien Campagne
 * Date: Jan 19, 2004
 * Time: 1:24:19 PM
 */
public abstract class FeatureCreationParameters {
    protected int windowSize;
    private int parameterNumber;

    public FeatureCreationParameters() {
        super();
    }

    public final int getParameterNumber() {
        return parameterNumber;
    }

    public final void setParameterNumber(final int parameterNumber) {
        this.parameterNumber = parameterNumber;
    }

    /**
     * Returns the size of the window.
     *
     * @return Size of the window around the special word.
     */
    public final int getWindowSize() {
        return windowSize;
    }

    /**
     * Sets the size of the window.
     *
     * @param windowSize Size of the window (in words) each side of the special
     *        word. The total window width will be double this value (minus 1 if
     *        the reference word is ignored).
     */
    public abstract void setWindowSize(int windowSize);

    public abstract String[] getTerms();

    /**
     * Update the index of the termsInWindows in
     * SingleBagOfWordFeatureCretionParameters with the current index.
     */
    public abstract void updateIndex(DocumentIndexManager docmanager);

    /**
     * Clear the terms stored in this parameter. This method is used within an
     * exporter FirstPass method to garantee that no previous terms are stored
     * in the parameters.
     */
    public abstract void clearTerms();

    /**
     * Test if the position is excluded.
     * @param excludedPositions The array of excluded positions.
     * @param position The position.
     * @return True if the position is excluded, False otherwise.
     */
    protected final boolean positionIsExcluded(final int[] excludedPositions,
            final int position) {
        for (int i = 0; i < excludedPositions.length; ++i) {
            if (excludedPositions[i] == position) {
                return true;
            }
        }

        return false;
    }

    public abstract void removeTerm(String term, int indexedTerm);

    /**
     * Convert indexed terms to their string representation. The side effect is
     * that getTerms() returns accurate terms.
     *
     * @param docmanager the index
     */
    public abstract void setTerms(DocumentIndexManager docmanager);
}
