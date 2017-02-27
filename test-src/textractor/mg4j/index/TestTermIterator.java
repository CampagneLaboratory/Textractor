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

import it.unimi.dsi.mg4j.util.MutableString;
import junit.framework.TestCase;

import java.io.FileNotFoundException;
import java.io.StringReader;

/**
 * Test the term iterator.
 * User: Fabien Campagne
 * Date: May 26, 2006
 * Time: 12:19:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestTermIterator extends TestCase {
    private String termsNoFrequency = "apple\n" +
            "rabbit\n" +
            "ragout";

    private String termsWithFrequency = "2\tapple\n" +
            "1\trabbit\n" +
            "1\tragout";

    public void testTermIteratorNoFrequency() throws FileNotFoundException {

        final TermIterator it = new TermIterator(new StringReader(termsNoFrequency));

        assertTrue(it.hasNext());
        assertEquals(new MutableString("apple"), it.nextTerm());
        assertTrue(it.hasNext());
        assertEquals(new MutableString("rabbit"), it.nextTerm());
        assertTrue(it.hasNext());
        assertEquals(new MutableString("ragout"), it.nextTerm());
        assertFalse(it.hasNext());
    }

    public void testTermIteratorWithFrequency() throws FileNotFoundException {

        final TermIterator it = new TermIterator(new StringReader(termsWithFrequency), true);

        assertTrue(it.hasNext());
        assertEquals(new MutableString("apple"), it.nextTerm());
        assertEquals(2, it.getFrequency());
        assertTrue(it.hasNext());
        assertEquals(new MutableString("rabbit"), it.nextTerm());
        assertEquals(1, it.getFrequency());
        assertTrue(it.hasNext());
        assertEquals(1, it.getFrequency());
        assertEquals(new MutableString("ragout"), it.nextTerm());
        assertEquals(1, it.getFrequency());
        assertFalse(it.hasNext());
    }
}
