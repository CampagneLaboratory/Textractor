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

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.didyoumean.DidYouMean;
import textractor.didyoumean.DidYouMeanI;
import textractor.mg4j.index.TermIterator;
import textractor.stemming.PaiceHuskStemmer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

/**
 * Stem terms in an inverted index term dictionary.
 *
 * @author Fabien Campagne
 *         Date: Apr 14, 2006
 *         Time: 1:03:10 PM
 */
public final class StemTermDictionary {
    private Map<String, Integer> tallies = new Object2IntOpenHashMap<String>();

    public static void main(final String[] args) throws IOException,
            ConfigurationException, TextractorDatabaseException,
            ParseException, ClassNotFoundException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException,
            InstantiationException, URISyntaxException, QueryParserException, QueryBuilderVisitorException {
        final StemTermDictionary processor = new StemTermDictionary();
        processor.process(args);
    }

    private void process(final String[] args) throws IOException, ConfigurationException, TextractorDatabaseException, ParseException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, URISyntaxException, QueryParserException, QueryBuilderVisitorException {
        final String termFileName = CLI.getOption(args, "-i", null);
        final String outFileName = CLI.getOption(args, "-o", "stems.txt");
        final String basename = CLI.getOption(args, "-basename", null);
        final boolean bioStemmer = CLI.isKeywordGiven(args, "-biostemmer");
        final boolean phase1 = CLI.isKeywordGiven(args, "-1");
        final boolean phase2 = CLI.isKeywordGiven(args, "-2");
        final int minimalLength = CLI.getIntOption(args, "-ml", 4);
        final int minimalStemFrequency = CLI.getIntOption(args, "-mf", 10);
        if (phase1 && termFileName == null) {
            System.err.println("Dictionary file not found. Provide with option -i");
            System.exit(1);
        }

        final PrintWriter printer = new PrintWriter(outFileName);
        final String rawStemsfileName = "raw-stems-" + outFileName;

        int count = 0;
        if (phase1) {
            System.out.println("Phase 1...");
            System.out.flush();
            final DocumentIndexManager docManager =
                    new DocumentIndexManager(basename);
            final DidYouMeanI dym = new DidYouMean(docManager);
            final BioStemmer biostemmer = new BioStemmer(dym);
            final PaiceHuskStemmer paicestemmer = new PaiceHuskStemmer(true);
            final TermIterator termIt = new TermIterator(termFileName);
            final PrintWriter untalliedStemPrinter =
                    new PrintWriter(rawStemsfileName);

            while (termIt.hasNext()) {
                final String term = termIt.next();
                final MutableString stem;
                if (bioStemmer) {
                    stem = biostemmer.stem(term,true);
                } else {
                    stem = new MutableString(paicestemmer.stripAffixes(term));
                }

                if (stem != null && stem.length() > minimalLength) {
                    untalliedStemPrinter.println(term + "\t" + stem);
                    untalliedStemPrinter.flush();
                    addStem(stem.toString());

                }
                count += 1;
                if ((count % 1000) == 1) {
                    System.out.println("Processed " + count + " words");
            }
            }
            untalliedStemPrinter.close();
        }
        if (!phase1 && phase2) {
            System.out.println("Reading raw stem data from prior phase 1..");
            System.out.flush();
            // read raw stems from raw file written previously
            final BufferedReader br =
                    new BufferedReader(new FileReader(rawStemsfileName));
            String line;
            while ((line = br.readLine()) != null) {
                final String[] tokens = line.split("[ \t]");
                if (tokens.length == 2) {
                    addStem(tokens[1]);
                }
            }
        }
        if (phase2) {
            System.out.println("Phase 2...");
            System.out.flush();
            // tally stems:
            final Set<Map.Entry<String, Integer>> entries = tallies.entrySet();
            for (final Map.Entry<String, Integer> entry : entries) {
                final int frequency = entry.getValue();
                if (frequency > minimalStemFrequency) {
                    printer.println(frequency + "\t" + entry.getKey());
                }
            }
            printer.close();
        }
        System.exit(0);
    }

    private void addStem(final String term) {
        final int count = tallies.get(term);
        tallies.put(term, count + 1);
    }
}
