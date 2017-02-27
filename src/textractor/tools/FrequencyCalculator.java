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
 * FrequencyCalculator classes are used to extend the FrequencyScorer class.
 * FrequencyScorer uses FrequencyCalculators to compute adjusted document and
 * term frequencies.
 */
public interface FrequencyCalculator {
    /**
     * Get the raw integer frequency count.
     * @return frequency of term
     */
    int getFrequencyCount();

    /**
     * Set the raw integer frequency count.
     * @param frequency of term
     */
    void setFrequencyCount(int frequency);

    /**
     * Returns computed frequency.
     * @return The computed frequency
     */
    double getFrequency();
}
