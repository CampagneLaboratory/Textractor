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

import it.unimi.dsi.mg4j.io.InputBitStream;

import java.io.IOException;

/**
 * @author Fabien Campagne
 *         Date: Mar 11, 2006
 *         Time: 1:32:52 PM
 */
public final class LongDenseDeltaList implements LongDenseList {
    /**
     * Position in the offset stream for each random access entry in the offset information.
     */
    private final int[] lowerBound;
    /**
     * The value of the offset for each random access entry in the offset information.
     */
    private final long[] startValue;

    /**
     * Stream over the compressed offset information.
     */
    private final InputBitStream stream;
    /**
     * Maximum number of times stream.readLongDelta() will be called to access an offset.
     */
    private final int maxReads;


    public LongDenseDeltaList(final byte []offsetRawData, final int maxReads, final int numOffsets) throws IOException {
          this(new InputBitStream(offsetRawData),maxReads, numOffsets);
    }

    public LongDenseDeltaList(final InputBitStream offsetRawData,
                              final int maxReads,
                              final int numOffsets) throws IOException {
        this.maxReads = maxReads;
        int slotNumber = (numOffsets / maxReads) + 1;
        // adjust if the division has no reminder, since there will be no
        // reads after the last slotNumber:
        if ((numOffsets % maxReads) == 0) {
            slotNumber -= 1;
        }
        this.lowerBound = new int[slotNumber];
        this.startValue = new long[slotNumber];
        this.stream = offsetRawData;
        prepareRandomAccess(numOffsets);
    }

    private void prepareRandomAccess(final int numOffsets) throws IOException {
        long offset = 0;
        stream.position(0);
        int k = maxReads + 1;
        int slotIndex = 0;
        for (int i = numOffsets; i > 0; --i) {
            offset += stream.readLongDelta();

            if (++k > maxReads) {
                k = 0;

                startValue[slotIndex] = offset;
                final long currentOffsetInStream = stream.readBits();
                assert currentOffsetInStream < Integer.MAX_VALUE  : " Casting to an int must be possible.";

                lowerBound[slotIndex] = (int) currentOffsetInStream;
                ++slotIndex;
            }
        }
    }

    public long getLong(final int index) throws IOException {
        final int slotNumber = index / (maxReads + 1);
        final int k = index % (maxReads + 1);
        if (k == 0) {
            // exact match to an index in startValue:
            return startValue[slotNumber];
        } else {
            long value = startValue[slotNumber];
            stream.position(lowerBound[slotNumber]);
            for (int i = 0; i < k; ++i) {
                value += stream.readLongDelta();
            }
            return value;
        }
    }
}
