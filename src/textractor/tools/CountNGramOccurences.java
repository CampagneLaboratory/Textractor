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

package textractor.tools;

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.IntervalIterator;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * A tool to count how many times n grams listed in a file occur in the corpus.
 * User: campagne
 * Date: May 27, 2004
 * Time: 5:01:43 PM
 * To change this template use File | Settings | File Templates.
 */
public final class CountNGramOccurences {
    private static String line_separator = System.getProperty("line.separator");
    private static DocumentIndexManager docManager;

    public static void main(final String[] args) throws ConfigurationException, NoSuchMethodException, IllegalAccessException, IOException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final String ngram_input = CLI.getOption(args, "-i", null);
        final boolean wrapCase = CLI.isKeywordGiven(args, "-wrapCase", false);
        final String index_basename = CLI.getOption(args, "-basename", null);
        if (ngram_input == null) {
            System.err.println("Please provide the name of the file that contains the n-grams (-i file).");
            System.exit(1);
        }
        final Writer output = null;
        if (index_basename == null) {
            System.err.println("An index basename must be provided");
            System.exit(1);
        } else {
            docManager = new DocumentIndexManager(index_basename);
        }

        final BufferedReader br = new BufferedReader(new FileReader(new File(ngram_input)));
        String line;

        while ((line = br.readLine()) != null) {
            if (wrapCase) {
		line = line.toLowerCase();
	    }


            final int[] ngram = docManager.extractTerms(line);
            if (ngram == null) {
                writeRecord(output, line, 0, 0);
                continue;
            }
            boolean skipThisOne = false;
            if (ngram.length==0) {
		continue;
	    }

            final String[] query = new String[ngram.length];
            for (int i = 0; i < query.length; ++i) {
                if (ngram[i]<0) {
                    skipThisOne=true; // skip this n gram entirely if just one term
                    break;
                }
                // is absent from the index
                if (i < ngram.length) {
                    query[i] = docManager.termAsString(ngram[i]);
                }
            }
            if (skipThisOne) {
		continue;
	    }

            final DocumentIterator documents = docManager.queryAndExactOrderMg4jNative(query);
            int ngramFrequency = 0;
            int docFrequency = 0;

            while (documents.hasNext()) {
                documents.next();
                final IntervalIterator intervalIterator;
                if ((intervalIterator = documents.intervalIterator()) != null) {
                    while (intervalIterator.hasNext()) {
                        intervalIterator.nextInterval();
                        ngramFrequency++;
                    }
                } else {
                    ngramFrequency++;
                }
                docFrequency++;
            }
            writeRecord(output, line, ngramFrequency, docFrequency);
        }
        output.flush();
        output.close();
    }

    // write to the output file
    private static void writeRecord(final Writer output, final String line, final int ngramFrequency, final int docFrequency) throws IOException {
        output.write("" + ngramFrequency);
        output.write("\t" + docFrequency);
        output.write("\t");
        output.write(line);
        output.write(line_separator);
        output.flush();
    }
}
