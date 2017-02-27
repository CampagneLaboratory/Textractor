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

import junit.framework.TestCase;

public class TestSentenceFilters extends TestCase {
    public void testParenthesesFilter() {
        final SentenceFilter sentenceFilter = new SentenceParenthesesFilter();
        assertTrue(sentenceFilter.filterSentence(" ("));
        assertTrue(sentenceFilter.filterSentence(" ) ("));
        assertTrue(sentenceFilter.filterSentence(" ( 1 )"));
        assertTrue(sentenceFilter.filterSentence(" ( 22 )"));
        assertFalse(sentenceFilter.filterSentence(" ( 2a )"));
        assertFalse(sentenceFilter.filterSentence(" ( p53 )"));
        assertFalse(sentenceFilter.filterSentence(" ( p53 ) ( 9 ) ("));
        assertFalse(sentenceFilter.filterSentence(" Bla bla (1) and here we have an abbreviation ( ab )"));
        assertTrue(sentenceFilter.filterSentence(" Bla bla ( 1 ) and another reference ( 20 ) "));
    }
}
