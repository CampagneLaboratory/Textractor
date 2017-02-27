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

package textractor.mg4j.docstore;

import it.unimi.dsi.mg4j.io.OutputBitStream;

import java.io.IOException;

/**
 * Writes with DELTA coding.
 */
public final class CompressionAdaptorDelta extends CompressionAdaptor {
    public CompressionAdaptorDelta(final OutputBitStream out) {
        super(out);
    }

    @Override
    public int write(final int value) throws IOException {
        return output.writeDelta(value);
    }

    @Override
    public int write(final long value) throws IOException {
        return output.writeLongDelta(value);
    }

    @Override
    public int write(final int[] values) throws IOException {
        final int length = values.length;
        int bitCount = 0;
        int previous = -1;
        for (int i = 0; i < length; ++i) {
            // TODO??  This assumes that the value does not remain the same
            bitCount += output.writeDelta(values[i] - previous - 1);
            previous = values[i];
        }
        return bitCount;
    }
}
