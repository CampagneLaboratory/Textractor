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

package textractor.crf;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.PrintWriter;

/**
 * @author Fabien Campagne
 *         Date: Aug 22, 2006
 *         Time: 11:10:56 AM
 */
public final class CRFDatasetWriter implements DatasetWriter {
    private final PrintWriter writer;
    private int typeNumber;
    private boolean testFormat;
    private final Object2IntMap<String> tagType2Number =
            new Object2IntOpenHashMap<String>();

    public CRFDatasetWriter(final PrintWriter writer) {
        this.writer = writer;
    }

    public void writeTag(final CharSequence taggedText, final String tagType) {
        writer.print(taggedText);
        if (testFormat) {
            writer.print('|');
            writer.print(getTagTypeNumber(tagType));
        }
        writer.print("\n");
    }

    public void close() {
        writer.close();
    }

    public int getLabelCount() {
        return typeNumber;
    }

    public int getFeatureCount() {
        return 0;
    }

    public void setTestDataset(final boolean testFormat) {
        this.testFormat = testFormat;
    }

    public int getTagTypeNumber(final String type) {
        final int number = tagType2Number.getInt(type);
        if (number != 0) {
            return number;
        } else {
            tagType2Number.put(type, typeNumber);
            return typeNumber++;
        }
    }
}
