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

package textractor.mg4j.offsets;

import it.unimi.dsi.mg4j.io.OutputBitStream;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * @author Fabien Campagne Date: Mar 9, 2006 Time: 11:04:27 PM
 */
public class TestLongDenseList extends TestCase {
    /** Used to log debug and informational messages. */
    private static final Log LOG = LogFactory.getLog(TestLongDenseList.class);

    public void testDeltaSet() throws IOException {
        final byte[] array = new byte[100];
        final OutputBitStream streamer = new OutputBitStream(array);
        final long[] offsets = {
                10, 300, 450, 650, 1000, 1290, 1700
        };
        long previous = 0;
        int numOffsets = 0;
        for (final long offset : offsets) {
            streamer.writeLongDelta(offset - previous);
            numOffsets++;
            previous = offset;
        }

        final LongDenseList list = new LongDenseDeltaList(array, 2, numOffsets);
        if (LOG.isDebugEnabled()) {
            for (int i = 0; i < offsets.length; ++i) {
                LOG.debug("i: " + i + " getLong(i): " + list.getLong(i));
            }
        }

        for (int i = 0; i < offsets.length; ++i) {
            assertEquals(("test failed for index: " + i), offsets[i],
                    list.getLong(i));

        }
    }

    public void testAbsoluteSet() throws IOException {
        final byte[] array = new byte[100];
        final OutputBitStream streamer = new OutputBitStream(array);
        final long[] offsets = {
                10, 300, 450, 650, 1000, 1290, 1700
        };
        int numOffsets = 0;
        for (final long offset : offsets) {
            streamer.writeLongDelta(offset);
            numOffsets++;
        }

        final LongDenseList list =
            new LongDenseAbsoluteList(array, 2, numOffsets);
        if (LOG.isDebugEnabled()) {
            for (int i = 0; i < offsets.length; ++i) {
                LOG.debug("i: " + i + " getLong(i): " + list.getLong(i));
            }
        }

        for (int i = 0; i < offsets.length; ++i) {
            assertEquals(("test failed for index: " + i), offsets[i],
                    list.getLong(i));
        }
    }
}
