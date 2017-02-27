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

package textractor.util;

import textractor.datamodel.TextractorDocument;

import java.io.Serializable;

/**
 * Filters sentences by length.
 */
public final class SentenceLengthFilter implements SentenceFilter, Serializable {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 1L;

    private int minLength;
    private int maxLength = 500 ; // Integer.MAX_VALUE;

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(final int maxLength) {

        this.maxLength = maxLength;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(final int minLength) {
        this.minLength = minLength;
    }

    public boolean filterSentence(final CharSequence text) {

        return !(text.length() >= minLength && text.length() <= maxLength);
    }

    public boolean filterSentence(final TextractorDocument document) {
        return filterSentence(document.getText());

    }
}
