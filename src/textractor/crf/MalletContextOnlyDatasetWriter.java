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

import it.unimi.dsi.mg4j.util.MutableString;

import java.io.PrintWriter;

/**
 * Writes tags in Mallet format (see SimpleTagger) such that only context
 * information can be used for the labeling task. This is achieved by replacing
 * each word labeled with a tag with the tag type. For instance, if the sequence
 * the little duck:noun is pretty were to be tagged to label with type noun, the
 * this class would create the little noun:noun is pretty.
 *
 * @author Fabien Campagne
 *         Date: Aug 22, 2006
 *         Time: 3:55:37 PM
 */
public final class MalletContextOnlyDatasetWriter extends MalletDatasetWriter
	implements DatasetWriter {
    public MalletContextOnlyDatasetWriter(final PrintWriter output) {
        super(output);
    }

    @Override
    protected void writeWord(final MutableString word, final String label) {
        if (testFormat) {
            super.writeWord(word, label);
        } else {
            writer.print("word_");
            writer.print((!"NOTAG".equals(label)) ? label : word);
        }
    }

    @Override
    protected void printFeatures(final MutableString word, final String label) {
        if (!"NOTAG".equals(label)) {
            writer.print(' ');
        } else {
            super.printFeatures(word, label);
        }
    }
}
