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

package textractor.html;

import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.io.IOUtils;
import textractor.datamodel.Sentence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public final class Html2TextTagRef extends Html2Text {

    @Override
    protected void modifyHtml(final MutableString inString) throws IOException {
        super.modifyHtml(inString);

        BufferedReader in = null;
        try {
            boolean found = false;
            in = new BufferedReader(new FileReader("data/html/referenceTags.txt"));

            String tag;
            int ref = -1;
            int length = 0;
            int notedPosition;
            do {
                tag = in.readLine();
                if (tag != null) {
                    notedPosition = inString.indexOf(tag);
                    if (notedPosition > 0) {
                        found = true;
                        length = tag.length();
                        ref = notedPosition;
                    }
                }
            } while (tag != null);
            if (found) {
                inString.replace(ref, ref + length, Sentence.REFERENCE_TAG_HTML);
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
