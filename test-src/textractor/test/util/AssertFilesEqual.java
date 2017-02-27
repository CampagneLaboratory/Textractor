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

package textractor.test.util;

import junit.framework.Assert;
import org.apache.commons.lang.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Asserts if two files are equal.
 */
public class AssertFilesEqual extends Assert {
    private static String readFile(final File file) throws IOException {
        final StringBuffer result = new StringBuffer();
        final FileReader reader = new FileReader(file);
        final BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
            result.append(line);
            result.append(SystemUtils.LINE_SEPARATOR);

        }
        if (result.length() != 0) {
            result.setLength(result.length() - SystemUtils.LINE_SEPARATOR.length());
        }
        return result.toString();
    }

    /**
     * Asserts that two files are equal.
     */
    public static void assertEquals(final String message,
                                    final File expectedFile,
                                    final File observedFile)
        throws IOException {
        final String expected = readFile(expectedFile);
        final String observed = readFile(observedFile);

        assertEquals(message, expected, observed);
    }

    /**
     * Asserts that two files are equal.
     */
    public static void assertEquals(final File expectedFile,
            final File observedFile) throws IOException {
        assertEquals(null, expectedFile, observedFile);
    }
}
