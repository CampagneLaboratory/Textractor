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

package textractor.html;

import junit.framework.TestCase;

/**
 * Test the text extracting visitor (parsing HTML).
 * @author Kevin Dorff
 */
public class TestTextractorTextExtractingVisitor extends TestCase {
    /**
     * Test transforming Greek chars to unicode.
     */
    public void testGreekTransform() {
        assertNull(TextractorTextExtractingVisitor.unicodeGreekCharToString('a'));
        assertNull(TextractorTextExtractingVisitor.unicodeGreekCharToString('z'));
        // Uppercase Alpha
        assertEquals("alpha", TextractorTextExtractingVisitor.unicodeGreekCharToString('\u0391'));
        // Lowercase gamma
        assertEquals("gamma", TextractorTextExtractingVisitor.unicodeGreekCharToString('\u03b3'));
    }
}
