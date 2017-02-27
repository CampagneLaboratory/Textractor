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
 * Created by IntelliJ IDEA.
 * User: Fabien Campagne
 * Date: Oct 29, 2005
 * Time: 12:17:04 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CompressionAdaptor {
    protected final OutputBitStream output;

    public CompressionAdaptor(final OutputBitStream out) {
        this.output = out;
    }

    public abstract int write(final int value) throws IOException;
    public abstract int write(final long value) throws IOException;
    public abstract int write(final int[] values) throws IOException;

    public final OutputBitStream getOutput() {
        return output;
    }
}
