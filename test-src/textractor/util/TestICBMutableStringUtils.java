/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

import it.unimi.dsi.mg4j.util.MutableString;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

/**
 * Test the mutable string utility funtions.
 * @author Kevin Dorff
 */
public class TestICBMutableStringUtils {
    /**
     * The utility object for removing extra spaces.
     */
    private ICBMutableStringUtils util = new ICBMutableStringUtils();

    /**
     * Test removing extra spaces.
     */
    @Test
    public void testRemoveExtraSpaces() {
        final String expected = "abc def";

        String s = "abc def";
        assertEquals(expected, util.stripExtraSpaces(new MutableString(s)).toString());

        s = "    abc \t def    ";
        assertEquals(expected, util.stripExtraSpaces(new MutableString(s)).toString());

        s = " \t   abc \t def  \t   ";
        assertEquals(expected, util.stripExtraSpaces(new MutableString(s)).toString());

        s = "\t   abc \t def  \t   ";
        assertEquals(expected, util.stripExtraSpaces(new MutableString(s)).toString());

        s = " \t   abc \t def";
        assertEquals(expected, util.stripExtraSpaces(new MutableString(s)).toString());

        s = "abc \t def \t   \t";
        assertEquals(expected, util.stripExtraSpaces(new MutableString(s)).toString());

        s = "abc \t def";
        assertEquals(expected, util.stripExtraSpaces(new MutableString(s)).toString());

        s = "abc      def";
        assertEquals(expected, util.stripExtraSpaces(new MutableString(s)).toString());
    }

    /**
     * Make sure we start with and finish with the SAME mutable string.
     */
    @Test
    public void testSameString() {
        final MutableString start = new MutableString(" \t\tabc      def\t");
        final MutableString finished = util.stripExtraSpaces(start);
        assertTrue(start == finished);
        assertEquals("abc def", start.toString());
        assertEquals("abc def", finished.toString());
    }

    // ----------------------------------------------------

    /**
     * Test removing extra spaces.
     */
    @Test
    public void testRemoveExtraSpacesStatic() {
        final String expected = "abc def";

        String s = "abc def";
        assertEquals(expected, ICBMutableStringUtils.stripExtraSpacesThreadsafe(
                new MutableString(s)).toString());

        s = "    abc \t def    ";
        assertEquals(expected, ICBMutableStringUtils.stripExtraSpacesThreadsafe(
                new MutableString(s)).toString());

        s = " \t   abc \t def  \t   ";
        assertEquals(expected, ICBMutableStringUtils.stripExtraSpacesThreadsafe(
                new MutableString(s)).toString());

        s = "\t   abc \t def  \t   ";
        assertEquals(expected, ICBMutableStringUtils.stripExtraSpacesThreadsafe(
                new MutableString(s)).toString());

        s = " \t   abc \t def";
        assertEquals(expected, ICBMutableStringUtils.stripExtraSpacesThreadsafe(
                new MutableString(s)).toString());

        s = "abc \t def \t   \t";
        assertEquals(expected, ICBMutableStringUtils.stripExtraSpacesThreadsafe(
                new MutableString(s)).toString());

        s = "abc \t def";
        assertEquals(expected, ICBMutableStringUtils.stripExtraSpacesThreadsafe(
                new MutableString(s)).toString());

        s = "abc      def";
        assertEquals(expected, ICBMutableStringUtils.stripExtraSpacesThreadsafe(
                new MutableString(s)).toString());
    }

    @Test
    public void testParseInt() {
        assertEquals(0, ICBMutableStringUtils.parseInt("0", 10));
        assertEquals(473, ICBMutableStringUtils.parseInt("473", 10));
        assertEquals(0, ICBMutableStringUtils.parseInt("-0", 10));
        assertEquals(0, ICBMutableStringUtils.parseInt("0"));
        assertEquals(473, ICBMutableStringUtils.parseInt("473"));
        assertEquals(0, ICBMutableStringUtils.parseInt("-0"));
        assertEquals(-255, ICBMutableStringUtils.parseInt("-FF", 16));
        assertEquals(102, ICBMutableStringUtils.parseInt("1100110", 2));
        assertEquals(2147483647, ICBMutableStringUtils.parseInt("2147483647", 10));
        assertEquals(-2147483648, ICBMutableStringUtils.parseInt("-2147483648", 10));
        assertEquals(2147483647, ICBMutableStringUtils.parseInt("2147483647"));
        assertEquals(-2147483648, ICBMutableStringUtils.parseInt("-2147483648"));
        assertEquals(411787, ICBMutableStringUtils.parseInt("Kona", 27));
    }

    @Test(expected = NumberFormatException.class)
    public void testParseIntFail1() {
        ICBMutableStringUtils.parseInt("2147483648", 10);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseIntFail2() {
        ICBMutableStringUtils.parseInt("99", 8);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseIntFail3() {
        ICBMutableStringUtils.parseInt("Kona", 10);
    }

    @Test
    public  void testConvertMonths() {
        MutableString s = new MutableString("JAN");
        assertTrue(ICBMutableStringUtils.convertThreeLetterMonthToDigits(s));
        assertEquals("1", s.toString());

        assertFalse(ICBMutableStringUtils.convertThreeLetterMonthToDigits(s));

        assertFalse(ICBMutableStringUtils.convertThreeLetterMonthToDigits(null));

        s.length(0);
        assertFalse(ICBMutableStringUtils.convertThreeLetterMonthToDigits(s));

        s.length(0);
        s.append("December");
        assertFalse(ICBMutableStringUtils.convertThreeLetterMonthToDigits(s));

        s.length(0);
        s.append("Dec");
        assertTrue(ICBMutableStringUtils.convertThreeLetterMonthToDigits(s));
        assertEquals("12", s.toString());
    }

    /**
     * Make sure we start with and finish with the SAME mutable string.
     */
    @Test
    public void testSameStringThreadsafe() {
        final MutableString start = new MutableString(" \t\tabc      def\t");
        final MutableString finished = ICBMutableStringUtils.stripExtraSpacesThreadsafe(start);
        assertTrue(start == finished);
        assertEquals("abc def", start.toString());
        assertEquals("abc def", finished.toString());
    }

}
