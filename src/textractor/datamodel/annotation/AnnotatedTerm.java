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

package textractor.datamodel.annotation;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Apr 7, 2004
 * Time: 11:54:48 AM
 * To change this template use File | Settings | File Templates.
 */
public final class AnnotatedTerm {
    private String text;
    private int startPosition;
    private int termLength;

    public AnnotatedTerm() {
        super();
        startPosition = -1;
    }

    public void setText(final String text, final int startPosition) {
        setStartPosition(startPosition);
        setText(text);
    }

    private void setText(final String t) {
        final String[] textTemp = t.split("\\s");
        setTermLength(textTemp.length);
        text = textTemp[0];
        if (termLength > 1) {
            for (int i = 1; i < textTemp.length; i++) {
                text += "_" + textTemp[i];
            }
        }
    }

    private void setTermLength(final int length) {
        termLength = length;
    }

    public String getTermText() {
        return text;
    }

    /**
     * Sets the position of the AnnotatedTerm to which this annotation refers
     * to.
     *
     * @param start Position of the AnnotatedTerm in the sentence
     * (assuming words are separated by spaces or newlines). Position 0 is the
     * first word of the sentence.
     */
    public void setStartPosition(final int start) {
        startPosition = start;
        termLength = 1;
    }

    /**
     * Obtain the start position of the AnnotatedTerm to which this annotation
     * refers to.
     *
     * @return Returns the position, or -1 if no position has been specified.
     */
    public int getStartPosition() {
        return startPosition;
    }

    /**
     * Obtain the end position of the AnnotatedTerm to which this annotation
     * refers to.
     *
     * @return Returns the position, or -1 if no position has been specified.
     */
    public int getEndPosition() {
        return startPosition + termLength - 1;

    }

    public int getTermLength() {
        return termLength;
    }
}
