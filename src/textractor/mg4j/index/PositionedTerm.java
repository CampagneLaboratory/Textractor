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

package textractor.mg4j.index;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.math.IntRange;

/**
 * Class used to bundle a term index along with a range of positions
 * representing the orignal location of the term in the raw document.
 */
public final class PositionedTerm {
    /**
     * The term index.
     */
    private final int term;

    /**
     * The text of the term.
     */
    private final String text;

    /**
     * Range of positions of characters in the text relative to the original
     * document source.
     */
    private final IntRange range;

    /**
     * Create a new PositionedTerm object.
     * @param termIndex The index of the term with position information.
     * @param positionList The range that the term originally spanned in the
     * raw document.
     */
    public PositionedTerm(final int termIndex,
                          final IntRange positionList) {
        this(termIndex, positionList, null);
    }

    /**
     * Create a new PositionedTerm object.
     * @param termIndex The index of the term with position information.
     * @param positionList The range that the term originally spanned in the
     * raw document.
     * @param termText The actual text of the term
     */
    public PositionedTerm(final int termIndex,
                          final IntRange positionList,
                          final String termText) {
        super();
        assert positionList != null : "Must have a valid position range object";
        this.term = termIndex;
        this.range = positionList;
        this.text = termText;
    }

    /**
     * Get the term index.
     * @return The index for this term
     */
    public int getTerm() {
        return term;
    }

    /**
     * Get the range that the term originally spanned in the raw document.
     * @return A range object that represents the term location(s).
     */
    public IntRange getRange() {
        return range;
    }

    /**
     * Get the term text.
     * @return The text of this term
     */
    public String getText() {
        return text;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof PositionedTerm)) {
          return false;
        }

        if (this == obj) {
          return true;
        }

        final PositionedTerm that = (PositionedTerm) obj;
        return new EqualsBuilder()
                 .append(this.term, that.term)
                 .append(this.text, that.text)
                 .append(this.range, that.range)
                 .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(term).append(text).append(range).toHashCode();
    }

    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("Term: ");
        buffer.append(text);
        buffer.append(" (");
        buffer.append(term);
        buffer.append(") is located at ");
        buffer.append(range.toString());
        return buffer.toString();
    }
}
