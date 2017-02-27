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

package textractor.tools.clustering;

import edu.cornell.med.icb.clustering.QTClusterer;
import edu.cornell.med.icb.clustering.SimilarityDistanceCalculator;
import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.io.TextractorWordReader;
import textractor.query.clustering.TermCoOccurenceLoader;
import textractor.query.clustering.TermSimilarityMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This is a class to test clustering using MutualInformation.
 * @author Kevin Dorff
 *
 */
public class ClusterTool {

    /**
     * Print usage message for main method.
     * @param options Options used to determine usage
     */
    private static void usage(final Options options) {
        // automatically generate the help statement
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(ClusterTool.class.getName(), options, true);
    }

    private static DocumentIndexManager docmanager;

    private static TextractorWordReader wordReader;

    private static TermProcessor termProcessor;

    /**
     * Command line interface to ClusterTool.
     * @param args command line arguments
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws URISyntaxException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ConfigurationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static void main(final String[] args)
            throws ClassNotFoundException, IOException, URISyntaxException,
            InstantiationException, IllegalAccessException,
            ConfigurationException, NoSuchMethodException,
            InvocationTargetException {
        final Options options = new Options();

        final Option basenameOption = new Option("b", "basename", true,
                "basename to use");
        basenameOption.setArgName("basename");
        basenameOption.setRequired(true);

        final Option titlesOption = new Option("t", "titlesfile", true,
                "titles file to use");
        basenameOption.setArgName("titlesfile");
        basenameOption.setRequired(false);

        options.addOption(basenameOption);
        options.addOption(titlesOption);

        CommandLine line = null;

        try {
            // create the command line parser
            final CommandLineParser parser = new BasicParser();
            line = parser.parse(options, args, true);
        } catch (final ParseException e) {
            System.err.println("Error: " + e.getMessage());
            usage(options);
            System.exit(1);
        }

        final List<String> queries = new ArrayList<String>();

        final String basename = line.getOptionValue("b");

        docmanager = new DocumentIndexManager(basename);
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));
        wordReader = docmanager.getWordReader();
        termProcessor = docmanager.getTermProcessor();

        if (line.hasOption("t")) {
            final File titleFile = new File(line.getOptionValue("t"));
            if (!titleFile.exists()) {
                System.out.println("Title file does not exist");
                System.exit(1);
            }
            try {
                readTitlesFile(titleFile, queries);
            } catch (IOException e) {
                System.out.println("Error reading title file "
                        + e.getMessage());
                System.exit(1);
            }
        } else {
            // Add a couple built-in quries since no title
            // file as suggested.
            queries
                    .add("C & cancer & polyposis & adenomatous & protein & kinase & APC & APS & TNT & trinitrotoluene");
            queries
                    .add("virus & capsid & infection & GFP & fluorescence & green protein & fluorescent & DMSO");
        }

        final HashMap<String, Integer> termIndexes = new HashMap<String, Integer>();
        StringBuffer workingTerms;

        final TermCoOccurenceLoader tcoLoader = new TermCoOccurenceLoader(docmanager);

        int queryNumber = 0;
        for (final String query : queries) {
            queryNumber++;
            System.out.flush();
            termIndexes.clear();
            int termCount = 0;
            workingTerms = new StringBuffer();
            // Retrieve the index position of each of the
            // terms in the query, no need to lookup
            // any term more than once.
            final String[] paddedTerms = query.split("&");
            final int numTerms = paddedTerms.length;
            for (final String paddedTerm : paddedTerms) {
                final String term = paddedTerm.trim();
                if (!docmanager.suggestIgnoreTerm(term, numTerms)) {
                    // Not a term to ignore
                    if (!skipWords.contains(term) && (!isNumeric(term)) && (term.length() > 0)) {
                        // Not a term to forcably skip or numeric term
                        if (termIndexes.get(term) == null) {
                            // Only lookup terms ONCE, we know the term is
                            // there or suggestIgnoreTerm would have been true
                            termIndexes.put(term,
                                    docmanager.findTermIndex(term));
                            termCount++;
                            if (workingTerms.length() > 0) {
                                workingTerms.append(", ");
                            }
                            workingTerms.append(term);
                        }
                    } else {
                        // System.out.println("ClusterTool decided to ignore term=" + term);
                    }
                } else {
                    // System.out.println("Docmanager said to ignore term=" + term);
                }
            }

            // Copy the found terms to an array, remove the nonfound ones (-1)
            final int[] usedTermIndexes = new int[termCount];
            final String[] usedTerms = new String[termCount];
            int pos = 0;
            for (final String term : termIndexes.keySet()) {
                final int termIndex = termIndexes.get(term);
                if (termIndex != -1) {
                    usedTermIndexes[pos] = termIndex;
                    usedTerms[pos] = term;
                    pos++;
                }
            }

            System.out.println();
            System.out.println("-------------------------------------");
            System.out.println("Query number = " + queryNumber);
            System.out.println("Orig query   " + query);
            System.out.println("Working with " + workingTerms.toString());
            System.out.flush();

            final TermSimilarityMatrix simMatrix =
                tcoLoader.obtainTermCoOccurenceMatrix(usedTerms);

            {
                // Create a cluster of the correct size
                final QTClusterer clusterer = new QTClusterer(termCount);

                // Create a distance calculator with the terms so we
                // can perform the clustering
                final SimilarityDistanceCalculator distCalc =
                    new FisherDistanceCalculator(docmanager, simMatrix,
                            usedTermIndexes, usedTerms,
                            FisherDistanceCalculator.DistanceType.AVERAGE);

                final float thresh = 0.05f;
                final List<int[]> clusters = clusterer.cluster(distCalc, thresh);
                showClusters("Avg", clusters, usedTerms);
            }
            {
                // Create a cluster of the correct size
                final QTClusterer clusterer = new QTClusterer(termCount);

                // Create a distance calculator with the terms so we
                // can perform the clustering
                final SimilarityDistanceCalculator distCalc =
                    new FisherDistanceCalculator(docmanager, simMatrix,
                            usedTermIndexes, usedTerms,
                            FisherDistanceCalculator.DistanceType.MAX);

                final float thresh = 0.05f;
                final List<int[]> clusters = clusterer.cluster(distCalc, thresh);
                showClusters("Max", clusters, usedTerms);
            }
            {
                // Create a cluster of the correct size
                final QTClusterer clusterer = new QTClusterer(termCount);

                // Create a distance calculator with the terms so we
                // can perform the clustering
                final SimilarityDistanceCalculator distCalc =
                    new FisherDistanceCalculator(docmanager, simMatrix,
                            usedTermIndexes, usedTerms,
                            FisherDistanceCalculator.DistanceType.MIN);

                final float thresh = 0.05f;
                final List<int[]> clusters = clusterer.cluster(distCalc, thresh);
                showClusters("Min", clusters, usedTerms);
            }
            {
                // Create a cluster of the correct size
                final QTClusterer clusterer = new QTClusterer(termCount);

                // Create a distance calculator with the terms so we
                // can perform the clustering
                final SimilarityDistanceCalculator distCalc =
                    new FisherDistanceCalculator(docmanager, simMatrix,
                            usedTermIndexes, usedTerms,
                            FisherDistanceCalculator.DistanceType.CLUSTER_AS_UNIT);

                final float thresh = 0.01f;
                final List<int[]> clusters = clusterer.cluster(distCalc, thresh);
                showClusters("C2P", clusters, usedTerms);
            }
        }

    }

    public static void showClusters(final String label,
            final List<int[]> clusters, final String[] usedTerms) {
        int pos = 0;
        System.out.print(label + ": ");
        for (final int[] cluster : clusters) {
            System.out.print("(");
            final List<String> list = new ArrayList<String>(cluster.length);
            // Build the list
            for (int termPos = 0; termPos < cluster.length; termPos++) {
                list.add(usedTerms[cluster[termPos]]);
            }
            // Sort it
            Collections.sort(list);
            int termPos = 0;
            // Display it
            for (final String term : list) {
                if (termPos++ > 0) {
                    System.out.print(", ");
                }
                System.out.print(term);
            }
            System.out.print(")");
            pos++;
        }
        System.out.println();
        System.out.flush();
    }

    private static void readTitlesFile(final File file, final List<String> queries)
            throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int lineNum = 0;
        while ((line = br.readLine()) != null) {
            if (lineNum++ > 0) {
                if (line.indexOf("#") != 0) {
                    final String[] parts = line.split("\t");
                    queries.add(fixTitle(parts[2]));
                }
            }
        }
    }

    private static final List<String> skipWords = initSkipWords();

    private static List<String> initSkipWords() {
        final List<String> toSkip = new ArrayList<String>();
        toSkip.add(",");
        toSkip.add(".");
        toSkip.add("(");
        toSkip.add(")");
        toSkip.add("a");
        toSkip.add("as");
        toSkip.add("and");
        toSkip.add("the");
        toSkip.add("of");
        toSkip.add("in");
        toSkip.add("On");
        toSkip.add("In");
        toSkip.add("on");
        toSkip.add("for");
        toSkip.add(":");
        toSkip.add("with");
        return toSkip;
    }

    private static boolean isNumeric(final String s) {
        final char[] numbers = s.toCharArray();
        for (int x = 0; x < numbers.length; x++) {
            final char c = numbers[x];
            if ((c >= '0') && (c <= '9')) {
                continue;
            }
            return false; // invalid
        }
        return true; // valid
    }

    /**
     * Use the wordReader to potentially break the terms such as
     * "protein-binding" to "protein binding".
     * @param title the text to break up, if necessary
     * @return String the broken up version
     */
    private static String fixTitle(final String title) {
        if (wordReader == null) {
            return "";
        }

        final StringReader stringReader = new StringReader(title);
        wordReader.setReader(stringReader);

        final MutableString word = new MutableString();
        final MutableString nonword = new MutableString();
        final StringBuffer results = new StringBuffer();

        try {
            while (wordReader.next(word, nonword)) {
                termProcessor.processTerm(word);
                if (results.length() > 0) {
                    results.append(" & ");
                }
                results.append(word.toString());
            }
            return results.toString();
        } catch (IOException e) {
            // There was an error splitting
            // Just return the base word
            // TODO: Log an exception or something.
            return results.toString();
        }
    }

}
