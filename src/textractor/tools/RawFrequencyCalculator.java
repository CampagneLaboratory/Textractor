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

package textractor.tools;

/**
 *
 */
public class RawFrequencyCalculator implements FrequencyCalculator {
    int frequency;
    int documentCount;

    public RawFrequencyCalculator(final int newCount) {
        setDocumentCount(newCount);
    }

    public final void setFrequencyCount(final int newFrequency) {
        frequency = newFrequency;
    }

    public final int getFrequencyCount() {
        return frequency;
    }

    public double getFrequency() {
        return frequency;
    }

    public final int getDocumentCount() {
        return documentCount;
    }

    public final void setDocumentCount(final int newCount) {
        documentCount = newCount;
    }
}
