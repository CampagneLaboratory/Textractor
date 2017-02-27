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

import org.htmlparser.util.DefaultParserFeedback;
import textractor.tools.BuildDocumentIndex;
import textractor.util.ProcessDirectory;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Oct 18, 2004
 * Time: 4:57:15 PM
 * To change this template use File | Settings | File Templates.
 */
final class GenbankConversionProcessDirectory extends ProcessDirectory {
    private final Writer writer;
    private int allArticleSentenceCount;

    public int getParserFeedBackLevel() {
        return parserFeedBackLevel;
    }

    public void setParserFeedBackLevel(final int parserFeedBackLevel) {
        this.parserFeedBackLevel = parserFeedBackLevel;
    }

    private int parserFeedBackLevel = DefaultParserFeedback.NORMAL;

    public GenbankConversionProcessDirectory(final String[] args,
                                             final Writer indexInputWriter,
                                             final String acOrganismFilename) {
        super(args, ".seq.gz");

        this.writer = indexInputWriter;
        this.acOrganismFilename = acOrganismFilename;
    }

    private final String acOrganismFilename;

    @Override
    public void processFile(final Reader reader, final String filename,
	    final String outputFilename) throws IOException {
        System.out.println("filename = " + filename);

        int count = 0;
	Writer acOrganismWriter = null;
	if (acOrganismFilename != null) {
	    acOrganismWriter = new FileWriter(acOrganismFilename, true); // append to the  acOrganismFilename file
	}
	final BufferedReader br = new BufferedReader(reader);

	final GenbankParser parser = new GenbankParser(br);
	while (parser.hasNextEntry()) {
	    String definition = parser.getDefinition();
	    if (definition != null && definition.indexOf("similar to") != -1) {
		// definition contains similar to:
		definition =
		    definition.substring(1, definition.indexOf("similar to"));
	    }
	    writer.write(definition);
	    writer.write(" ");
	    writer.write(parser.getFeatures());
	    writer.write(" "); //mg4j omits the last word

	    // produce a file with AC organism on a line for each entry
	    if (acOrganismFilename != null) {
		acOrganismWriter.write(Integer.toString(allArticleSentenceCount));
		acOrganismWriter.write("\t");
		acOrganismWriter.write(parser.getAccessionCode());
		acOrganismWriter.write("\t");
		acOrganismWriter.write(parser.getOrganism());
		acOrganismWriter.write("\n");
	    }
	    writer.write(BuildDocumentIndex.DOCUMENT_SEPARATOR);
	    count++;
	    allArticleSentenceCount++;
	    parser.next();
	}
	if (acOrganismFilename != null) {
	    acOrganismWriter.close();
	}
    }

    public int getTotalSencenceCount() {
        return allArticleSentenceCount;
    }
}
