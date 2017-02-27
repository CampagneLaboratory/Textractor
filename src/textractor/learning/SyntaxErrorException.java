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

package textractor.learning;

/**
 *
 * Thrown when syntax error occurs when reading TextFragmentAnnotation format files.
 * Creation Date: Jan 16, 2004
 * Creation Time: 5:37:49 PM
 * @author Fabien Campagne
 */
public final class SyntaxErrorException extends Exception {
    private int lineNumber;

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public SyntaxErrorException(final String s, final int lineNumber) {
        super("An error occured while parsing a TextFragmentAnnotation file: "+s+". The error occured at line "+lineNumber);
    this.lineNumber=lineNumber;
    }
}
