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

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import it.unimi.dsi.mg4j.io.InputBitStream;
import it.unimi.dsi.mg4j.io.OutputBitStream;
import junit.framework.TestCase;

import java.io.*;

public class TestBitReadAndWrite extends TestCase {

    /** File to test reading/writing to/from. */
    private final static String TESTFILE = "test-results/TestBitReadAndWrite.dat";

    /**
     * Test reading and writing bit-wise 15 character Strings.
     */
    public void testBitReadAndWrite() throws FileNotFoundException, IOException {

        // Variables we need to perform the test
        int[] positions = new int[3];
        int[] sizes = new int[3];
        String[] readVals = new String[3];
        String[] writeVals = new String[3];
        writeVals[0] = "123456789012345";
        writeVals[1] = "ABCDEFGHIJKLMNO";
        writeVals[2] = "ENSP00000354687";

        // Delete the test file if it is there
        File doiFile = new File(TESTFILE);
        if (doiFile.exists()) {
            doiFile.delete();
        }

        // Write the strings to a file, bit-wise
        OutputBitStream doiOutput;
        doiOutput = new OutputBitStream(new FastBufferedOutputStream(new FileOutputStream(TESTFILE)));
        writeBits(doiOutput, writeVals[0], 0, positions, sizes);
        writeBits(doiOutput, writeVals[1], 1, positions, sizes);
        writeBits(doiOutput, writeVals[2], 2, positions, sizes);
        doiOutput.close();

        // Read the strings from the file, bit-wise
        InputBitStream doiInput;
        doiInput = new InputBitStream(new FastBufferedInputStream(new FileInputStream(TESTFILE)));
        readVals[0] = readBits(doiInput, 0, positions, sizes);
        readVals[1] = readBits(doiInput, 1, positions, sizes);
        readVals[2] = readBits(doiInput, 2, positions, sizes);

        // Did the write/read correctly?
        assertEquals(writeVals[0], readVals[0]);
        assertEquals(writeVals[1], readVals[1]);
        assertEquals(writeVals[2], readVals[2]);
    }

    private void writeBits(OutputBitStream outstream, String val, int place, int[] pos, int[] size) throws IOException {
        final byte[] bytes = val.getBytes();
        final int bitCount = bytes.length * 8;
        outstream.write(bytes, bitCount);
        size[place] = bitCount;
        if (place==0) {
            pos[place] = 0;
        } else {
            pos[place] = pos[place-1] + size[place-1];
        }
    }

    private String readBits(InputBitStream instream, int place, int[] pos, int[] size) throws IOException {
        int numBytes = size[place] / 8;
        byte[] buffer = new byte[(size[place] / 8)];
        instream.position(pos[place]);
        instream.read(buffer, size[place]);
        return new String(buffer);
    }
}
