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

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * A tool to count how many different tissues are listed in Genbank entries.
 * This is an exploratory tool, to figure out if we can use keyword matches
 * to extract tissue information from genbank entries that TissueInfo tiimport
 * could not get a tissue from.
 * <p/>
 * User: campagne
 * Date: May 27, 2004
 * Time: 5:01:43 PM
 * To change this template use File | Settings | File Templates.
 */
public final class CountTissueOccurencesInEntries {
    private static String line_separator = System.getProperty("line.separator");
    private static Writer outputWriter;

    public static void main(final String[] args) throws IOException, ConfigurationException, TextractorDatabaseException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final String tissues_input = CLI.getOption(args, "-i", null);
        final boolean wrapCase = CLI.isKeywordGiven(args, "-wrapCase", true);
        final String index_basename = CLI.getOption(args, "-basename", null);
        final String acOrganismFilename = CLI.getOption(args, "-ac-org", null);
        final String nullTissueOutputFilename = CLI.getOption(args, "-tissue-out", "null-tissue-output.data");
        outputWriter = new BufferedWriter(new FileWriter(nullTissueOutputFilename,true));
        if (tissues_input == null) {
            System.err.println("Please provide the name of the file that contains the tissues (-i file).");
            System.exit(1);
        }
        if (acOrganismFilename == null) {
            System.err.println("Please provide the name of the file that contains entryNumber ac organism info (-ac-org file).");
            System.exit(1);
        }
        final String output_filename = CLI.getOption(args, "-o", "output.txt");
        final Writer output = new FileWriter(output_filename);
        final DocumentIndexManager docManager;
        if (index_basename == null) {
            final DbManager dbm = new DbManager(args);
            dbm.beginTxn();
            final TextractorManager tm = dbm.getTextractorManager();
            docManager = new DocumentIndexManager(tm.getInfo().getCaseSensitiveIndexBasename());
            dbm.commitTxn();
        } else {
            docManager = new DocumentIndexManager(index_basename);
        }

        // read the ac-org file and store in maps, keyed on the entry number:
        System.out.println("Reading ac-org file");
        System.out.flush();
        final Map<Integer, String> entryNumberToAccession =
                new Int2ObjectOpenHashMap<String>();
        final Map<Integer, String> entryNumberToOrganism =
                new Int2ObjectOpenHashMap<String>();
        final BufferedReader acOrgReader = new BufferedReader(new FileReader(acOrganismFilename));
        String line;
        while ((line = acOrgReader.readLine()) != null) {
            final StringTokenizer st = new StringTokenizer(line, "\t");
            final int entryNumber = Integer.parseInt(st.nextToken());
            final String ac = st.nextToken();
            final String organism = st.nextToken();
            entryNumberToAccession.put(entryNumber, ac);
            entryNumberToOrganism.put(entryNumber, organism);
        }
        System.out.println("done reading.");
        System.out.flush();

        final BufferedReader br =
                new BufferedReader(new FileReader(new File(tissues_input)));
        final int minDocNumber = 0;
        int maxDocNumber = 0;

        final Map<Integer, Integer> tissueHitsPerEntry = new Int2IntOpenHashMap();
        final Map<Integer, Collection<String>> tissueSetPerEntry =
                new Int2ObjectOpenHashMap<Collection<String>>();
        System.out.println("Iterating through tissues..");
        System.out.flush();
        while ((line = br.readLine()) != null) {              // for each tissue:
            if (wrapCase) {
		line = line.toLowerCase();
	    }
            final int[] ngram = docManager.extractTerms(line);
            if (ngram == null) {
                writeRecord(output, line, 0, 0);
                continue;
            }
            final String[] query = new String[ngram.length];
            for (int i = 0; i < query.length; ++i) {
                if (i < ngram.length) {
                    query[i] = docManager.termAsString(ngram[i]);
                }
            }
            final DocumentIterator documents =
                docManager.queryAndExactOrderMg4jNative(query);
            final int ngramFrequency = 0;
            int docFrequency = 0;

            while (documents.hasNext()) {
                final int documentNumber = documents.nextDocument();
                tissueHitsPerEntry.get(documentNumber);
                maxDocNumber = Math.max(documentNumber, maxDocNumber);
                addTissueToEntry(tissueSetPerEntry, documentNumber, line);
                tissueHitsPerEntry.put(documentNumber, tissueHitsPerEntry.get(documentNumber) + 1); // increment tally of matching tissues
                // for this entry.

                docFrequency++;
            }
            writeRecord(output, line, ngramFrequency, docFrequency);
        }
        System.out.println("Done iterating.");
        System.out.flush();
        for (int entryNumber = minDocNumber; entryNumber <= maxDocNumber; entryNumber++) {
            if (tissueSetPerEntry.get(entryNumber) != null) {
                outputAcOrganismTissues(entryNumberToAccession.get(entryNumber),
                        entryNumberToOrganism.get(entryNumber),
                        tissueSetPerEntry.get(entryNumber));
            }
        }
        output.flush();
        output.close();
        outputWriter.close();
        System.exit(0);
    }

    private static void outputAcOrganismTissues(final String ac,
        final String organism, final Collection<String> tissues) throws IOException {
        outputWriter.write(ac);
        outputWriter.write('\t');
        outputWriter.write(organism);
        outputWriter.write('\t');
        for (final String tissue : tissues) {
            outputWriter.write(tissue);
            outputWriter.write('\t');
        }
        outputWriter.write('\n');
    }

    private static void addTissueToEntry(final Map<Integer, Collection<String>> tissueSetPerEntry, final int documentNumber, final String query) {
        Collection<String> previous = tissueSetPerEntry.get(documentNumber);
        if (previous == null) {
            previous = new ArrayList<String>();
        }

        if (!previous.contains(query)) {
            previous.add(query);
        }

        tissueSetPerEntry.put(documentNumber, previous);
    }

    // write to the output file
    private static void writeRecord(final Writer output, final String line, final int ngramFrequency, final int docFrequency) throws IOException {
        output.write("" + ngramFrequency);
        output.write("\t" + docFrequency);
        output.write("\t");
        output.write(line);
        output.write(line_separator);
    }
}
