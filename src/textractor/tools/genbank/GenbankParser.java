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

package textractor.tools.genbank;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * User: campagne
 * Date: Aug 4, 2005
 * Time: 2:47:52 PM
 */
public final class GenbankParser {
    private boolean encounteredEndOfFile;
    private final BufferedReader reader;

    public String getOrganism() {
        return organism.trim();
    }

    private String organism;

    public String getAccessionCode() {
        return accessionCode.trim();
    }

    private String accessionCode;


    public GenbankParser(final BufferedReader reader) {
        this.reader = reader;
        description = new StringBuffer();
        features = new StringBuffer();
    }

    public String getDefinition() {
        return description.toString();
    }

    public String getFeatures() {
        return features.toString();
    }

    public boolean hasNextEntry() throws IOException {
        if (encounteredEndOfFile) {
	    return false;
	} else {
            collectBetweenKeywords("DEFINITION", "ACCESSION", description);
            final String accessionCodeLine = skipToKeyword("ACCESSION");
            if (encounteredEndOfFile) {
		return false;
	    }
            setAccessionCode(accessionCodeLine.substring(accessionCodeLine.lastIndexOf(' ')));

            final String organismLine=skipToKeyword("  ORGANISM");
            if (encounteredEndOfFile) {
		return false;
	    }
            setOrganism(organismLine.substring(organismLine.indexOf(' ',5)));
            collectBetweenKeywords("FEATURES", "ORIGIN", features);
            return true;
        }

    }

    private void setOrganism(final String s) {
        organism = s;
    }

    private void collectBetweenKeywords(final String firstKeyword, final String secondKeyword, final StringBuffer textDestination) throws IOException {
        final String firstLine = skipToKeyword(firstKeyword);
        if (firstLine == null) {
	    return;
	}
        textDestination.append(firstLine.substring(firstLine.indexOf(' ') + 1));
        String line;
        do {
            line = readLine();
            if (line.startsWith(secondKeyword)) {
                pushPreviousLine(line);
                return;
            } else {
		textDestination.append(line);
	    }
        } while (!encounteredEndOfFile);
    }

    private String previousLine;

    private void pushPreviousLine(final String line) {
        previousLine = line;
    }

    private void setAccessionCode(final String s) {
        accessionCode = s;
    }

    public void next() throws IOException {
        skipAfterEntryDelimiter();


        description = new StringBuffer();
        features = new StringBuffer();
    }

    private void skipAfterEntryDelimiter() throws IOException {
        String line;
        do {
            line = readLine();
            if (line != null && line.equals("//")) {
		break;
	    }
        } while (!encounteredEndOfFile);
    }

    private String skipToKeyword(final String keyword) throws IOException {
        String line;
        do {
            line = readLine();
            if (line != null && line.startsWith(keyword)) {
		return line;
	    }
        } while (!encounteredEndOfFile);
        return null;
    }

    private String readLine() throws IOException {
        if (previousLine != null) {
            final String toReturn = previousLine;
            previousLine = null;
            return toReturn;
        }
        final String line = reader.readLine();
        if (line == null) {
	    encounteredEndOfFile = true;
	}
        return line;
    }

    private StringBuffer description;
    private StringBuffer features;

}
