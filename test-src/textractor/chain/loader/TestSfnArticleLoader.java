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

/**
 * Test the SfnArticleLoader
 * @author Kevin Dorff (Oct 24, 2007)
 */
public class TestSfnArticleLoader extends TestCase {

    public void testPresentationNumberToPmid() {
        assertEquals(40002,SfnArticleLoader.presentationNumberToPmid("4.2"));
        assertEquals(30000,SfnArticleLoader.presentationNumberToPmid("3"));
    }

    public void testAppendWithoutDuplication() {
        assertEquals("abc def",SfnArticleLoader.appendWithoutDuplication("abc", "def", " "));
        assertEquals("abc",SfnArticleLoader.appendWithoutDuplication("abc", "abc", " "));
        assertEquals("def",SfnArticleLoader.appendWithoutDuplication("", "def", " "));
        assertEquals("def",SfnArticleLoader.appendWithoutDuplication(null, "def", " "));
        assertEquals("abc",SfnArticleLoader.appendWithoutDuplication("abc", null, " "));
        assertEquals("abcdef",SfnArticleLoader.appendWithoutDuplication("abc", "def", ""));
        assertEquals("abcdef",SfnArticleLoader.appendWithoutDuplication("abc", "def", null));
    }

}
