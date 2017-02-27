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

import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions for MutableStrings.
 * @author Kevin Dorff
 */
public class ICBMutableStringUtils {

    /** Temp variable for use with removeExtraSpaces. */
    private final MutableString strippedSpacesTemp = new MutableString();

    /** Map for converting 3 letter months to the digits version. */
    private static final Map<MutableString, String> MONTH_LETTERS_TO_DIGITS_MAP;
    static {
        MONTH_LETTERS_TO_DIGITS_MAP = new HashMap<MutableString, String>();
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("jan"), "1");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("feb"), "2");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("mar"), "3");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("apr"), "4");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("may"), "5");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("jun"), "6");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("jul"), "7");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("aug"), "8");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("sep"), "9");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("oct"), "10");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("nov"), "11");
        MONTH_LETTERS_TO_DIGITS_MAP.put(new MutableString("dec"), "12");
    }

    /**
     * NOT THREADSAFE!!
     * Clean up a single Mutable String by doing the following...
     * This converts tabs to spaces. Removes all duplicate spaces
     * and removes all spaces at the head or tail of the string (trim).
     * This is done using just a single temp MutableString, strippedSpacesTemp,
     * so it doesn't need to create and destory lots of extra strings,
     * but this makes this method NOT THREADSAFE.
     * @param fieldValue the MutableString to remove extra spaces for
     * @return returns the same MutableString that has been modified to remove
     * extra spaces.
     */
    public MutableString stripExtraSpaces(final MutableString fieldValue) {
        strippedSpacesTemp.length(0);
        final int length = fieldValue.length();
        char prevChar = '\0';
        boolean stringHead = true;
        int tempLength = 0;
        for (int i = 0; i < length; i++) {
            char curChar = fieldValue.charAt(i);
            if (curChar == '\t') {
                curChar = ' ';
            }
            if (stringHead && curChar != ' ') {
                stringHead = false;
            }
            if (stringHead) {
                continue;
            }
            if (prevChar == ' ' && curChar == ' ') {
                continue;
            }
            tempLength++;
            strippedSpacesTemp.append(curChar);
            prevChar = curChar;
        }
        if (prevChar == ' ') {
            strippedSpacesTemp.length(tempLength - 1);
        }
        fieldValue.length(0);
        fieldValue.append(strippedSpacesTemp);
        return fieldValue;
    }

    /**
     * This performs the same function as removeExtraSpaces but is threadsafe
     * but at the cost of performance. If you know you are running single threaded,
     * use removeExtraSpaces(). This version also has a larger memory footprint as it
     * creates a MutableString.
     * The code here is an exact duplicate of the removeExtraSpaces() method
     * but with a local variable for strippedSpacesTemp. I did this instead of creating
     * a new ICBMutableStringUtils object to reduce the number of extra object creations
     * to just the one MutableString (and the few local simple datatypes).
     * @param fieldValue the MutableString to remove extra spaces for
     * @return returns the same MutableString that has been modified to remove
     * extra spaces.
     */
    public static MutableString stripExtraSpacesThreadsafe(final MutableString fieldValue) {
        final MutableString strippedSpacesTemp = new MutableString();
        final int length = fieldValue.length();
        char prevChar = '\0';
        boolean stringHead = true;
        int tempLength = 0;
        for (int i = 0; i < length; i++) {
            char curChar = fieldValue.charAt(i);
            if (curChar == '\t') {
                curChar = ' ';
            }
            if (stringHead && curChar != ' ') {
                stringHead = false;
            }
            if (stringHead) {
                continue;
            }
            if (prevChar == ' ' && curChar == ' ') {
                continue;
            }
            tempLength++;
            strippedSpacesTemp.append(curChar);
            prevChar = curChar;
        }
        if (prevChar == ' ') {
            strippedSpacesTemp.length(tempLength - 1);
        }
        fieldValue.length(0);
        fieldValue.append(strippedSpacesTemp);
        return fieldValue;
    }

    /**
     * Check if a string is both non blank (not null, not empty) and is numeric
     * (contains only digits).
     * @param s the string to check
     * @return true if both non blank AND numeric.
     */
    public static boolean isNonBlankNumeric(final CharSequence s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        final int length = s.length();
        for (int pos = 0; pos < length; pos++) {
            if (!Character.isDigit(s.charAt(pos))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Take the string s and return the integer value of that string.
     * @param s the string to convert to an int
     * @return the int value
     * @throws NumberFormatException the string didn't contain a valid int
     */
    public static int parseInt(final CharSequence s) throws NumberFormatException {
        return parseInt(s, 10);
    }

    /**
     * Take the string s and return the integer value of that string.
     * This uses the same logic as Integer.parseInt() BUT instead of taking a String,
     * this takes a CharSequence. The advantage of this method over Integer.parseInt()
     * is what you have as a CharSequence (such as MutableString) you don't have to
     * first create a String to just get the int value.
     * @param s the string to convert to an int
     * @param radix the radix to be used while parsing <code>s</code>.
     * @return the int value
     * @throws NumberFormatException the string didn't contain a valid int
     */
    public static int parseInt(final CharSequence s, final int radix) throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("null");
        }

        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix + " less than Character.MIN_RADIX");
        }

        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix + " greater than Character.MAX_RADIX");
        }

        int result = 0;
        boolean negative = false;
        int i = 0;
        final int max = s.length();
        final int limit;
        final int multmin;
        int digit;

        if (max > 0) {
            if (s.charAt(0) == '-') {
                negative = true;
                limit = Integer.MIN_VALUE;
                i++;
            } else {
                limit = -Integer.MAX_VALUE;
            }
            multmin = limit / radix;
            if (i < max) {
                digit = Character.digit(s.charAt(i++), radix);
                if (digit < 0) {
                    throw new NumberFormatException("For input string: \"" + s + "\"");
                } else {
                    result = -digit;
                }
            }
            while (i < max) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(s.charAt(i++), radix);
                if (digit < 0) {
                    throw new NumberFormatException("For input string: \"" + s + "\"");
                }
                if (result < multmin) {
                    throw new NumberFormatException("For input string: \"" + s + "\"");
                }
                result *= radix;
                if (result < limit + digit) {
                    throw new NumberFormatException("For input string: \"" + s + "\"");
                }
                result -= digit;
            }
        } else {
            throw new NumberFormatException("For input string: \"" + s + "\"");
        }
        if (negative) {
            if (i > 1) {
                return result;
            } else {
                // Only got "-"
                throw new NumberFormatException("For input string: \"" + s + "\"");
            }
        } else {
            return -result;
        }
    }

    /**
     * Take a three letter month and conver it to digits. "Jan" becomes "1",
     * Dec becomes "12". This will directly change the MutableString.
     * If the conversion is sucessful, this will return true. This method
     * is destructive, if it fails you will be left with "month" as a lowercase
     * version of whatever was in it before, if it works "month" will contain
     * the new value.
     * @param month the three letter month to convert
     * @return true if the conversion was sucessful
     */
    public static boolean convertThreeLetterMonthToDigits(final MutableString month) {
        if (month == null || month.length() == 0) {
            return false;
        }
        month.toLowerCase();
        final String digits = MONTH_LETTERS_TO_DIGITS_MAP.get(month);
        if (digits != null) {
            month.length(0);
            month.append(digits);
            return true;
        }
        return false;
    }

    /**
     * Return the first item in preferredOrder that exists and is not empty.
     * If all values in preferredOrder are null or empty, this will return
     * defaultValue.
     * @param defaultValue the value to return if all values in preferredOrder are
     * null or length==0.
     * @param preferredOrder the strings to find the first value for
     * @return the non empty value or null if none were found
     */
    public static CharSequence firstNonEmptyValue(
            final CharSequence defaultValue, final CharSequence... preferredOrder) {
        for (final CharSequence value : preferredOrder) {
            if (value != null && value.length() > 0) {
                return value;
            }
        }
        return defaultValue;
    }
}
