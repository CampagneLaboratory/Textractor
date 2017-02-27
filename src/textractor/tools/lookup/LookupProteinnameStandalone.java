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

package textractor.tools.lookup;

import com.martiansoftware.jsap.JSAPException;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlparser.util.DefaultParserFeedback;
import static textractor.TextractorConstants.TIMER_LOG;
import textractor.database.DocumentIndexManager;
import textractor.datamodel.ArticlePool;
import textractor.datamodel.LookupResult;
import textractor.datamodel.TermOccurrence;
import textractor.tools.BuildDocumentIndexFromHTMLArticles;
import textractor.util.ParseOutput;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lookup protein names without requiring a backend database.
 */
public final class LookupProteinnameStandalone extends AbstractLookupProteinname {
    /** Used to log debug and informational messages. */
    private static final Log LOG =
        LogFactory.getLog(LookupProteinnameStandalone.class);
    private FileWriter outputWriter;

    /** Minimum ammount of memory required (148 = -Xmx150m). */
    private static final String minHeapMemory = "148";

    /**
     * Create a new standalone protien lookup object.
     */
    public LookupProteinnameStandalone() throws IOException, ConfigurationException {
        super();
        indexBasename = "standalone";
    }

    /**
     * Create a new standalone protein lookup object.
     */
    public LookupProteinnameStandalone(final String[] args) throws IOException {
        super(args);
        indexBasename = "standalone";
    }

    @Override
    public void process(final String[] args) throws IOException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException, JSAPException {
        getOptions(args);
        LOG.info("the index being used: " + indexBasename);

        final StopWatch timer = new StopWatch();
        timer.start();

        final ArticlePool articlePool = new ArticlePool();
        final BuildDocumentIndexFromHTMLArticles indexBuilder =
            new BuildDocumentIndexFromHTMLArticles(indexBasename, false);
        indexBuilder.setParserFeedBackLevel(DefaultParserFeedback.QUIET);
        indexBuilder.setProcessArguments(args);
        indexBuilder.setArticlePool(articlePool);
        indexBuilder.setParenthesesAreWords(true);
        indexBuilder.index(args);
        docManager = new DocumentIndexManager(indexBasename);

        timer.stop();
        TIMER_LOG.debug(LookupProteinnameStandalone.class.getName()
                + ":build index" + timer.toString());
        timer.reset();
        timer.start();

        // lookup each term of dictionary in index
        // iterate through results and create annotations
        final ProteinMutation proteinMutation = new ProteinMutation();
        final Set<String> mutations =
                proteinMutation.identifyMutation(indexBasename);
        dictionary.addTerms(mutations);

        final Collection<LookupResult> lookupResults =
            lookupAllTermsByTerm(indexBasename);
        timer.stop();
        TIMER_LOG.debug(LookupProteinnameStandalone.class.getName()
                + ":lookup" + timer.toString());
        timer.reset();
        timer.start();

        postLookupProcess(lookupResults, articlePool);
        if (outputWriter != null) {
            outputWriter.close();
        }

        docManager.removeIndexFiles();
        timer.stop();
        TIMER_LOG.debug(LookupProteinnameStandalone.class.getName()
                + ":postprocess" + timer.toString());
    }

    /**
     * process the input arguments, exit when
     * the required input file|directory|url is not present
     * the memory allocated is not enough.
     * <p/>
     * the input source (-i or -d or -url) is taken in ProcessDirectory
     *
     * @param args
     */
    private void getOptions(final String[] args) throws IOException {
        subLookupResultsDirectory = CLI.getOption(args, "-subDirectory", "");
        final String exportDir = subLookupResultsDirectory + File.separator;

        // the option to export into a user designated single file, even if the
        // input is a directory
        final String outputFilename = CLI.getOption(args, "-o", null);
        if (outputFilename != null) {
            outputWriter = new FileWriter(exportDir + outputFilename);
        }

        // check if the input file(s) is given
        boolean toPrintUsage = true;
        final boolean toPrintFullHelp = CLI.isKeywordGiven(args, "-help");

        for (final String arg : args) {
            if (arg.matches("^-(i|d|url)$")) {
                toPrintUsage = false;
                break;
            }
        }

        if (toPrintFullHelp) {
            printHelp();
        } else if (toPrintUsage) {
            printUsage();
        }

        if (!toPrintFullHelp && !toPrintUsage) {
            // check if -Xmx150m is given on the command line:
            final long maxMemory = Runtime.getRuntime().maxMemory();

            final int mbytemaxMemory = (int) maxMemory / (1024 * 1024);
            LOG.debug("Max available memory: " + mbytemaxMemory);
            if (mbytemaxMemory < Integer.parseInt(minHeapMemory)) {
                printMemory(mbytemaxMemory);
            }
        }
    }

    private void printMemory(final int mbyteMaxMemory) {
        System.err.println("");
        System.err.println("Your current allocated memory to this JVM is " + mbyteMaxMemory + "M.");
        System.err.println("The inverted index used by TLookup requires a minimum of " + minHeapMemory + "M of JVM memory.");
        System.err.println("Please increase the amount of memory available to this JVM with the \"-Xmx\" option, " +
                "for instance:");
        System.err.println("");
        System.err.println("java -Xmx150m -jar tlookup.jar ...");
        System.err.println("");
        System.err.println("Try requesting more than " + minHeapMemory + " megabytes if the error persists. Processing large sets of articles ");
        System.err.println("will also require more memory. Adjust this parameter as needed.");
    }

    private void printHelp() {
        printUsage();
        System.err.println("\n" +
                "This  version  of  TLookup is bundled with the protein dictionary\n" +
                "file/version :\n" + Dictionary.DEFAULT_DICTIONARY + "\n" +
                "\n" +
                "This dictionary was built from protein names extracted  and  com-\n" +
                "piled from more than 80,000 biological journal articles.\n" +
                "\n" +
                "Input  filename(s)  should  have the extension \".html\" or \".htm\";\n" +
                "the program will scan for all the \".html\" and \".htm\" files if the\n" +
                "input  is  a  directory.  The  default output is <inputfile>.hit\n" +
                "\n" +
                "\n" +
                "- Credits/Legal\n" +
                "\n" +
                "TLookup was implemented with the Textractor framework  (Lei  Shi,\n" +
                "Fabien Campagne, unpublished) and includes code from the MG4J and\n" +
                "fastutil open source projects (http://mg4j.dsi.unimi.it/).\n" +
                "\n" +
                "TLookup and Textractor were developed at the Institute for Compu-\n" +
                "tational  Biomedicine  (http://icb.med.cornell.edu),at  the Weill\n" +
                "Medical College of Cornell University.\n" +
                "\n" +
                "This software is distributed under the  Gnu  General  Public  Li-\n" +
                "cense.   You   can   obtain   a   copy   of   this   license   at\n" +
                "http://www.gnu.org/licenses/gpl.txt\n" +
                "\n" +
                "- Updates\n" +
                "\n" +
        "Check http://icb.med.cornell.edu/crt/textractor/ for updates.");
        System.exit(0);
    }

    private void printUsage() {
        System.err.println("TLookup v1.1\n" +
                "\n" +
                "TLookup  looks  up terms in an article or articles in a directory\n" +
                "(and its subdirectories), and counts the frequency of  each  term\n" +
                "in  the  article.  A default protein name dictionary is provided,\n" +
                "but can be replaced with the -dict option (see below).\n" +
                "\n" +
                "Usage: java -Xmx150m -jar tlookup.jar [-i <input file> | \n" +
                "                                       -d  <input  directory> |\n" +
                "                                       -url  <url>] \n" +
                "                                      [-o output] \n" +
                "                                      [-dict custom-dictionary]\n" +
                "\n" +
                "For instance,      java -Xmx150m -jar tlookup.jar -d my-papers\n" +
                "\n" +
                "will recursively visit the directory 'my-papers' to locate  files\n" +
                "ending  in .html or .htm.  Such files will be processed individu-\n" +
                "ally to extract the protein names that  they  refer  to.   Output\n" +
                "will be written to a file of the form <input-file>.hit (with html\n" +
                "extension removed from input file), or to \"from_an_url.hit\"  when\n" +
                "the -url option is used.\n" +
                "\n" +
                "Options:\n" +
                "\n" +
                "  -i      Input  file. The file should be provided in HTML, ascii\n" +
                "          or UTF-8.\n" +
                "\n" +
                "  -d     Directory. Name of the  directory  that  contains  input\n" +
                "         files.  The directory  will be scanned to identify input \n" +
                "         files.\n" +
                "\n" +
                "  -v     Verbose. When used with -d, prints which directories and\n" +
                "         which files are processed.\n" +
                "\n" +
                "  -url   URL for a full text article to process. An URL that con-\n" +
                "         tains  spaces  or  special  characters (e.g.,  '?', '*') \n" +
                "         should be enclosed in quotes  or will be mis-interpreted \n" +
                "         by the shell.\n" +
                "\n" +
                "  -dict  Path  to  a protein name dictionary. Use this option to\n" +
                "         provide a custom dictionary. The dictionary must be a\n" +
                "         text file (UTF-8charset) with one protein name per line.\n" +
                "\n" +
                "  -dic   Same as -dict\n" +
                "\n" +
                "  -help  Prints extended help with credits and license information.\n");
        System.exit(0);
    }


    private void postLookupProcess(final Collection<LookupResult> lookupResults,
            final ArticlePool articlePool) throws IOException {
        final Set<String> articleIDPool = new HashSet<String>();
        for (final LookupResult lookupResult : lookupResults) {
            articleIDPool.addAll(lookupResult.getOccurrenceByArticle(articlePool).keySet());
        }
        LOG.info("parsed " + articleIDPool.size() + " articles.");
        postProcessHits(lookupResults, articleIDPool, articlePool);
    }

    /**
     * Post process the hits for each individual articles according to PMID.
     * 1) find the overlap of hits, including combining the neighboring hits
     * 2) remove totally included hits, e.g., the "a b" have same number of appearrance of "a a b"
     *
     * @param lookupResults
     * @param articleIDPool
     * @throws IOException
     */
    private void postProcessHits(final Collection<LookupResult> lookupResults,
            final Set<String> articleIDPool,
            final ArticlePool articlePool) throws IOException {
        final String exportDir = subLookupResultsDirectory + File.separator;

        for (final String articleID : articleIDPool) {
            // decide whether or not to export result in one large file or
            // separate files
            final FileWriter articleHitWriter;
            if (outputWriter == null) {
                articleHitWriter = new FileWriter(exportDir + articleID + ".hit");
            } else {
                articleHitWriter = outputWriter;
            }

            final Map<String, Integer> articleTermsMap =
                new Object2IntOpenHashMap<String>();
            final Map<String, Integer> articlePartialTermsMap =
                new Object2IntOpenHashMap<String>();

            for (final LookupResult lookupResult : lookupResults) {
                if (lookupResult.getOccurrenceByArticle(articlePool).containsKey(articleID)) {
                    final String term = lookupResult.getTerm();
                    final int frequency =
                        lookupResult.getOccurrenceByArticle(articlePool).getInt(articleID);
                    articleTermsMap.put(term, frequency);
                }
            }

            // findExtraFragment should be run before findOverlap,
            // because it does not consider overlap,
            // but only neighboring.
            findExtraFragment(articleTermsMap, articlePool, articleID);
            findOverlap(articleTermsMap, articlePool, articleID);

            calculatePartial(articleTermsMap, articlePartialTermsMap);

            final Object[] articleTerms = articleTermsMap.keySet().toArray();
            Arrays.sort(articleTerms);
            for (final Object articleTerm : articleTerms) {
                final String term = articleTerm.toString();
                if (articleTermsMap.get(term) > articlePartialTermsMap.get(term)) {
                    articleHitWriter.write(term + "\t" +
                            articleTermsMap.get(term) + "\t" +
                            articlePartialTermsMap.get(term) + "\n");
                }
            }

            if (outputWriter == null) {
                articleHitWriter.close();
            }
        }
    }

    /**
     * Calculate the occurence of a term as a part of the other term e.g. to
     * match "a" in "a b" or "c a" but not in "c a b", because "c a" is part of
     * "c a b" "a b" and "c a" are called second level
     *
     * @param articleTermsMap
     * @param articlePartialTermsMap
     */
    protected void calculatePartial(final Map<String, Integer> articleTermsMap,
            final Map<String, Integer> articlePartialTermsMap) {
        TermOccurrence[] lookupedTermOccurrences =
            ParseOutput.convertToTermOccurrence(articleTermsMap);

        for (int j = 0; j < lookupedTermOccurrences.length - 1; j++) {
            final String termA = lookupedTermOccurrences[j].getTerm();
            for (int k = j + 1; k < lookupedTermOccurrences.length; k++) {
                final String termB = lookupedTermOccurrences[k].getTerm();
                if (termB.matches("(^|(.+\\s))(" + Pattern.quote(termA) + ")((\\s.+)|$)") &&
                        lookupedTermOccurrences[j].getCount() == lookupedTermOccurrences[k].getCount()) {
                    articleTermsMap.remove(termA);
                }
            }
        }

        lookupedTermOccurrences =
            ParseOutput.convertToTermOccurrence(articleTermsMap);
        final Collection<String> partialList = new ArrayList<String>();
        for (int j = 0; j < lookupedTermOccurrences.length - 1; j++) {
            final String termA = lookupedTermOccurrences[j].getTerm();
            int partial = 0;
            partialList.clear();
            for (int k = j + 1; k < lookupedTermOccurrences.length; k++) {
                final String termB = lookupedTermOccurrences[k].getTerm();
                if (termB.matches("(^|(.+\\s))(" + Pattern.quote(termA) + ")((\\s.+)|$)")) {
                    boolean secondLevel = true;
                    for (final String aPartialList : partialList) {
                        if (termB.matches("(^|(.+\\s))(" + Pattern.quote(aPartialList) + ")((\\s.+)|$)")) {
                            secondLevel = false;
                            break;
                        }
                    }
                    if (secondLevel) {
                        partialList.add(termB);
                        partial += lookupedTermOccurrences[k].getCount();
                    }
                }
            }
            articlePartialTermsMap.put(termA, partial);
        }
    }

    /**
     * Find the extra terms not in dictionary, such as "the rho" and
     * "rho gtpase" form "the rho gtpase".
     *
     * @param articleTermsMap
     * @param articlePool
     * @param articleID
     */
    private void findOverlap(final Map<String, Integer> articleTermsMap,
                             final ArticlePool articlePool,
                             final String articleID) throws IOException {
        final Set<String> remove = new HashSet<String>();

        final String[] articleTermsObjects =
                articleTermsMap.keySet().toArray(new String[articleTermsMap.size()]);
        for (int j = 0; j < articleTermsObjects.length; j++) {
            final String termA = articleTermsObjects[j];
            final String[] wordsA = termA.split("\\s");
            for (int k = 0; k < articleTermsObjects.length; k++) {
                if (j == k) {
                    continue;
                }
                final String termB = articleTermsObjects[k];
                final String[] wordsB = termB.split("\\s");
                final int minLength = Math.min(wordsA.length, wordsB.length);

                // maximum overlapping length should be minLength-1;
                for (int overlappingLength = 0; overlappingLength < minLength; overlappingLength++) {
                    int overlapped = 0;
                    for (int currentPosition = 0; currentPosition < overlappingLength; currentPosition++) {
                        if (wordsA[wordsA.length - overlappingLength + currentPosition].equals(wordsB[currentPosition])) {
                            overlapped++;
                        }
                    }

                    if (overlapped == overlappingLength) {
                        final StringBuffer tempBuffer = new StringBuffer(termA);
                        for (int addedWordPosition = overlappingLength; addedWordPosition < wordsB.length; addedWordPosition++) {
                            tempBuffer.append(' ');
                            tempBuffer.append(wordsB[addedWordPosition]);
                        }
                        final String tempTerm = tempBuffer.toString();

                        lookupExtra(articleTermsMap, tempTerm, articlePool, articleID);
                        if (articleTermsMap.get(tempTerm) == articleTermsMap.get(termA)) {
                            remove.add(termA);
                        }
                        if (articleTermsMap.get(tempTerm) == articleTermsMap.get(termB)) {
                            remove.add(termB);
                        }
                    }
                }
            }
        }
        removePartial(remove, articleTermsMap);
    }

    private void removePartial(final Set<String> remove,
                               final Map<String, Integer> articleTermsMap) {
        for (final String term : remove) {
            articleTermsMap.remove(term);
        }
    }

    private void lookupExtra(final Map<String, Integer> articleTermsMap,
            final String tempTerm, final ArticlePool articlePool,
            final String articleID) throws IOException {
        if (!articleTermsMap.containsKey(tempTerm)) {
            final LookupResult lookupResult =
                dictionary.lookup(tempTerm, docManager);
            if (lookupResult != null &&
                lookupResult.getOccurrenceByArticle(articlePool).containsKey(articleID)) {
                    articleTermsMap.put(tempTerm, lookupResult.getOccurrenceByArticle(articlePool).getInt(articleID));
            }
        }
    }

    /**
     * Find the extra terms not in dictionary, such as "the rho" and
     * "rho gtpase" form "the rho gtpase".
     *
     * @param articleTermsMap
     * @param articlePool
     * @param articleID
     */
    private void findExtraFragment(final Map<String, Integer> articleTermsMap,
            final ArticlePool articlePool, final String articleID)
        throws IOException {
        final List<String> proteinNameFragments = new ArrayList<String>();
        String line;
        final BufferedReader reader =
            new BufferedReader(new FileReader(new File("dictionary"
                    + File.separator + "ProteinNameFragments")));
        while ((line = reader.readLine()) != null) {
            proteinNameFragments.add(line.trim());
        }

        final Set<String> remove = new HashSet<String>();
        final Object[] articleTermsObjects = articleTermsMap.keySet().toArray();
        for (final Object articleTermsObject : articleTermsObjects) {
            final String termA = articleTermsObject.toString();
            for (final Object proteinNameFragment : proteinNameFragments) {
                final String termB = proteinNameFragment.toString();
                final String tempTerm =
                    (new StringBuffer(termA).append(' ').append(termB)).toString();
                lookupExtra(articleTermsMap, tempTerm, articlePool, articleID);
                if (articleTermsMap.get(tempTerm) == articleTermsMap.get(termA)) {
                    remove.add(termA);
                }
            }
        }
        removePartial(remove, articleTermsMap);
    }

    public static void main(final String[] args) throws IllegalAccessException,
            NoSuchMethodException, ConfigurationException, IOException,
            JSAPException, InvocationTargetException, ClassNotFoundException,
            InstantiationException, URISyntaxException {
        final AbstractLookupProteinname lpns =
            new LookupProteinnameStandalone(args);
        lpns.process(args);
        System.exit(0);
    }
}
