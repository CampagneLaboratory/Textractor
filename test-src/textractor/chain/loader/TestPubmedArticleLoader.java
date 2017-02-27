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

package textractor.chain.loader;

import junit.framework.TestCase;

public class TestPubmedArticleLoader extends TestCase {

    /**
     * Required for JUnit.
     * @param name required name parameter
     */
    public TestPubmedArticleLoader(final String name) {
        super(name);
    }

    /**
     * Test padWithSpaces with null.
     */
    public final void testPadWithSpacesNull() {
        assertEquals("Incorrect padding on null", " ",
                PubmedArticleLoader.padWithSpaces(null));
    }

    /**
     * Test padWithSpaces with empty string.
     */
    public final void testPadWithSpacesEmptyStr() {
        assertEquals("Incorrect padding on null", " ",
                PubmedArticleLoader.padWithSpaces(""));
    }

    /**
     * Test padWithSpaces with string of spaces.
     */
    public final void testPadWithSpacesEmptySpaces() {
        assertEquals("Incorrect padding on null", " ",
                PubmedArticleLoader.padWithSpaces(" "));
        assertEquals("Incorrect padding on null", " ",
                PubmedArticleLoader.padWithSpaces("        "));
    }

    /**
     * Test padWithSpaces with empty string.
     */
    public final void testPadWithSpacesEmptyNonPaddedWord() {
        assertEquals("Incorrect padding on null", " someword ",
                PubmedArticleLoader.padWithSpaces("someword"));
        assertEquals("Incorrect padding on null", " other word ",
                PubmedArticleLoader.padWithSpaces("other word"));
    }

    /**
     * Test padWithSpaces with empty string.
     */
    public final void testPadWithSpacesEmptyPaddedWord() {
        assertEquals("Incorrect padding on null", " someword ",
                PubmedArticleLoader.padWithSpaces(" someword "));
        assertEquals("Incorrect padding on null", " someword ",
                PubmedArticleLoader.padWithSpaces("     someword     "));
        assertEquals("Incorrect padding on null", " other word ",
                PubmedArticleLoader.padWithSpaces(" other word "));
        assertEquals("Incorrect padding on null", " other word ",
                PubmedArticleLoader.padWithSpaces("     other word     "));
    }
}
