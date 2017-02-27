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

package textractor.tools.biostems;

import it.unimi.dsi.mg4j.util.MutableString;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Parse the tag output of the FSA stemming process.
 * User: Fabien Campagne
 * Date: May 25, 2006
 * Time: 11:27:32 AM
 * To change this template use File | Settings | File Templates.
 */
public final class TagOutputParser {
    private BufferedReader reader;
    private boolean lastPositionReturned;
    private MutableString parsedData;

    public TagOutputParser(final BufferedReader reader) {
        this.reader = reader;
        lastPositionReturned = false;
        parsedData = new MutableString(40);
    }

    public boolean hasNext() throws IOException {
        parsedData.setLength(0);
        readData(reader, parsedData);
        return !lastPositionReturned;
    }

    /**
     * Returns a sequence of space separated-two character sequences (e.g., "cp at rt")
     *
     * @return
     * @throws IOException
     */
    public MutableString readTags() throws IOException {
        return parsedData.copy().compact();
    }

    private void readData(final BufferedReader reader, final MutableString parsedData) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            final String[] tokens = line.split("[ \t]+");
            if (tokens.length < 2) {
		continue;
	    }
            if (tokens.length == 2) {
                // found end of record, final node e.g.,   "5       2.47692585\n"
                return;

            }
            final String taggedCharacter = tokens[2];
            if (!taggedCharacter.equals("<epsilon>")) {
                if (this.parsedData.length() > 0) {
		    parsedData.append(' ');
		}
                parsedData.append(taggedCharacter);
            }
        }
        lastPositionReturned = true;
    }

    public void remove() {
        assert false: "remove is not supported by this implementation.";
    }


    public MutableString getPrefix() {
        final char tag = 'p';
        return extractTag(tag);
    }

    private MutableString extractTag(final char tag) {
        final MutableString prefix = new MutableString(10);
        for (int i = 0; i < parsedData.length(); i += 3) {
            final char c = parsedData.charAt(i + 1);
            if (c == tag) {
		prefix.append(parsedData.charAt(i));
	    }
        }
        return prefix;
    }


    public MutableString getStem() {
        final char tag = 't';
        return extractTag(tag);
    }

    public MutableString getFirstLongestStem() {
        final MutableString longestStem = new MutableString(10);
        final MutableString currentStem = new MutableString(10);
        for (int i = 0; i < parsedData.length(); i += 3) {
            final char c = parsedData.charAt(i + 1);
            if (c == 't') {
		currentStem.append(parsedData.charAt(i));
	    } else {
                // a break in the tag sequence ends the stem
                if (currentStem.length() > longestStem.length()) {
                    longestStem.length(0);
                    longestStem.append(currentStem);
                    currentStem.setLength(0);
                }
            }
        }
        return longestStem;

    }

    public MutableString getLastLongestStem() {
        final MutableString longestStem = new MutableString(10);
        final MutableString currentStem = new MutableString(10);
        for (int i = 0; i < parsedData.length(); i += 3) {
            final char c = parsedData.charAt(i + 1);
            if (c == 't') {
		currentStem.append(parsedData.charAt(i));
	    } else {
                // a break in the tag sequence ends the stem
                if (currentStem.length() >= longestStem.length()) {
                    longestStem.length(0);
                    longestStem.append(currentStem);
                    currentStem.setLength(0);
                }
            }
        }
        return longestStem;

    }

    public MutableString getSuffix() {
        final char tag = 's';
        return extractTag(tag);
    }

    public MutableString getWord() {
        final MutableString word = new MutableString(10);
        for (int i = 0; i < parsedData.length(); i += 3) {
            word.append(parsedData.charAt(i));
        }
        return word;
    }
}

