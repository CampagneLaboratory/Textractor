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

package textractor.util;

import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlparser.util.ParserException;
import textractor.crf.TextSegment;
import textractor.database.DocumentIndexManager;
import textractor.html.Html2Text;
import textractor.html.Html2TextNoref;
import textractor.mg4j.docstore.DocumentStoreReader;
import textractor.tools.biostems.LongestCommonSubsequence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A program to map sentence ids to byte counts and byte length in html files.
 * This tool is needed for the TREC genomics track 2006.
 */
public final class HTMLByteLocator {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(HTMLByteLocator.class);

    private String sourceFilename;

    /**
     * The results.
     */
    private final List<TextSegment> results;

    /**
     * The number of results per topic identifier.
     */
    private final Map<Integer, Integer> topicCounts;

    /**
     * The writer used to display the results to the user.
     */
    private Writer outputWriter;

    private boolean isBatchFile;

    private String target;

    /**
     * Name of the writer file to write the results to.
     */
    private String output;

    private String batchFile;

    private DocumentStoreReader reader;

    private String basename = "trec-index/trec-index";

    /**
     * Indicates that informational messages and progress should be written to
     * the console.
     */
    private boolean verbose;

    private boolean unique;

    private boolean skipsingle;

    private boolean noref;

    private int batchLineNumber;

    private long timerStart = System.currentTimeMillis();

    private final Map<Integer, Integer> topicIdToRankNumber = new HashMap<Integer, Integer>();

    private static final int UNDEFINED_PMID = -1;

    private final Map<Integer, String> pmid2File;

    private static final int UNDEFINED_SENTENCEID = -1;

    private static final int UNDEFINED_RANK_NUMBER = -1;

    private static final int UNDEFINED_RANK_VALUE = -1;

    private static final int UNDEFINED_TOPIC_ID = 100;

    private boolean processTargetTerms;

    private WordReader wordReader;

    private TermProcessor termProcessor;

    /**
     * Proportion of characters that must align from the query.
     */
    private float threshold = 0.9f;

    /**
     * Tag assigned by the submitting group that should be distinct from all the
     * group's other runs (and ideally any other group's runs, so it should
     * probably have the group name, e.g., OHSUbaseline).
     */
    private String runtag = "tag";

    private static final int DEFAULT_MAX_LENGTH = 1000;

    private int maxLength = DEFAULT_MAX_LENGTH; // 1000 characters by default

    private static final int DEFAULT_IGNORE_LENGTH = 500;

    private int ignoreLength = DEFAULT_MAX_LENGTH; // 1000 characters by
                                                    // default

    public static void main2(final String[] args) throws IOException,
            ConfigurationException, ParserException {
        final HTMLByteLocator locator = new HTMLByteLocator();
        locator.noref = true;
        locator.unique = true;
        locator.skipsingle = false;
        final String filename = "C:/temp/Trec2006Partial/-9300662.html";
        final List<String> terms = new ArrayList<String>();
        terms
                .add("BSE ) John collinge prion disease group , "
                        + "neurogenetics Unit , imperial college school of medicine at St . "
                        + "Mary s , london , UK received July 11 , 1997 prion diseases are "
                        + "transmissible neurodegenerative disorders which affect a range of "
                        + "mammalian species . In humans they can be inherited and sporadic "
                        + "as well as acquired by exposure to human prions . prions appear "
                        + "to be composed principally of a conformational isomer of host "
                        + "encoded prion protein");
        terms
                .add("CJD ) are rare . Most human cases are sporadic , and their precise aetiology is still unclear . Both human and animal prion diseases share common histopathological features . The classical triad of spongiform vacuolation ( affecting any part of the cerebral grey matter ) , astrocytic proliferation and neuronal loss , may be accompanied by the deposition of amyloid plaques ( 9 ) . these plaques are composed principally of an abnormal , partially protease resistant isoform of a host encoded sialoglycoprotein , prion protein");
        terms.add("PRNP was in a family with CJD");
        terms
                .add("PrP ; ( ii ) insertions encoding additional integral copies of an octapeptide repeat present in a tandem array of five copies in the normal protein ( Fig . 1 ) . The availability of direct gene markers for these diseases has enabled identification of highly atypical cases and has widened the known phenotypic range of these disorders ( 23 , 24 ) . remarkable phenotypic variability may be present in the same family , with phenotypes ranging from classical CJD");
        terms
                .add("prion protein , with either methionine or valine present at residue 129 . In caucasians , 38 are homozygous for the more frequent methionine allele , 51 are heterozygotes and 11 are homozygous for valine ( 27 ) . The large majority of sporadic CJD");
        terms
                .add("CJD cases are homozygotes , with a particular excess of valine homozygotes ( 29 ) . This protective effect of PRNP");
        terms
                .add("prpsc in individuals with inherited prion diseases . Such a model provides an explanation of how a disease can be simultaneously inherited and transmissible . The finding that nearly all sporadic CJD");
        terms
                .add("CJD occurs in homozygotes with respect to a common and apparently innocent protein polymorphism lends strong support to such a mechanism ( 28 ) . again prion");
        terms
                .add("CJD have a more prolonged illness although more detailed studies are required to investigate this further ( 34 ) . PrP");
        terms.add("prion protein . The aetiology of sporadic CJD");
        terms.add("CJD may arise as a result of PrP");
        terms.add("PRNP mutation atypical CJD");
        terms.add("CJD germline PRNP");
        terms
                .add("PrP ( 45 ) . these abnormalities of synaptic inhibition are reminiscent of the neurophysiological abnormalities seen in patients with CJD");
        terms
                .add("PrP null mice , including disrupted Ca2 activated K currents and abnormal intrinsic properties of CA1 pyramidal cells have recently been reported ( 52 , 53 ) . creutzfeldt jakob");
        terms
                .add("BSE , exposure to specified bovine offal ( SBO ) prior to the ban on its inclusion in human foodstuffs in 1989 , was the most likely explanation . A case of the new variant was reported in france soon after ( 59 ) . PRNP");
        terms
                .add("PRNP coding sequence ( 60 ) . recently , transmission of BSE");
        terms
                .add("PrP expression levels . direct experimental evidence that new variant CJD");
        terms
                .add("BSE was provided by molecular analysis of human prion strains ( prpsc");
        terms.add("CJD ( 62 ) . Such prpsc");
        terms.add("PrP were susceptible to three CJD");
        terms.add("PrP , are highly susceptible to CJD");
        terms
                .add("prpsc types have been identified which are associated with different phenotypes of CJD");
        terms.add("CJD is associated with prpsc");
        terms
                .add("BSE when transmitted to several other species ( 62 ) . these data strongly support the protein only hypothesis of infectivity and suggest that strain variation is encoded by a combination of PrP");
        terms.add("CJD . In addition , prpsc");
        terms
                .add("PrP glycoform analysis alone can distinguish a number of mouse passaged scrapie strains ( 76 ) . The combination of fragment size and glycoform analysis should allow better resolution and might be applied , for instance , to study if BSE");
        terms
                .add("protein to encode a disease phenotype has important implications in biology , as it represents a non mendelian form of transmission . It would be surprising , and also itself intriguing , if evolution had not used this mechanism for other proteins in a range of species . The recent identification of prion like mechanisms in yeast is particularly interesting in this regard ( 78 , 79 ) . bovine to human species barrier . As BSE");
        terms
                .add("PrP molecules in the host and inoculum ( 32 ) and strain of agent ; however , BSE");
        terms.add("prpsc and human prions on CJD");
        terms.add("BSE were unaltered in mice expressing human PrP");
        terms.add("PrP with BSE");
        terms
                .add("CJD in these mice ( 68 ) ; it remains to be seen if transmission will occur at longer incubation periods . these mice express valine at polymorphic codon 129 of PRNP");
        terms
                .add("PrP methionine 129 . This is of particular importance given that all new variant CJD");
        terms
                .add("BSE transmission to humans , given the very large numbers of people that have been exposed . genetic susceptibility may well be important in this regard and in particular , PRNP");
        terms
                .add("creutzfeldt jakob disease . academic press , San diego , pp . 331 385 . 10 baldwin , M . A . , stahl , N . , hecker , R . , Pan , K . , burlingame , A . L . , and prusiner , S . B . ( 1992 ) glycosylinositol phospholipid anchors of prion proteins . In S . B . prusiner , J . collinge , J . powell and B . anderton , eds , prion diseases of humans and animals . ellis horwood , london . 11 prusiner , S . B . ( 1991 ) molecular biology of prion diseases . science 252 , 1515 1522 . medline abstract 12 borchelt , D . R . , scott , M . , taraboulos , A . , stahl , N . and prusiner , S . B . ( 1990 ) scrapie and cellular prion proteins differ in their kinetics of synthesis and topology in cultured cells . J . Cell . Biol . 110 , 743 752 . medline abstract 13 caughey , B . and raymond , G . J . ( 1991 ) The scrapie associated form of PrP");
        terms.add("prion protein gene in familial creutzfeldt jakob");
        terms
                .add("creutzfeldt jakob disease . lancet 1 , 51 52 . medline abstract 20 hsiao , K . , baker , H . F . , Crow , T . J . , poulter , M . , Owen , F . , terwilliger , J . D . , westaway , D . , Ott , J . and prusiner , S . B . ( 1989 ) linkage of a prion protein");
        terms
                .add("prion protein genotype predisposes to sporadic creutzfeldt jakob");
        terms
                .add("creutzfeldt jakob disease . lancet 337 , 1441 1442 . medline abstract 30 baker , H . F . , poulter , M . , Crow , T . J . , frith , C . D . , lofthouse , R . , ridley , R . M . and collinge , J . ( 1991 ) amino acid polymorphism in human prion protein");
        terms
                .add("PrP isoforms in scrapie prion replication . Cell 63 , 673 686 . medline abstract 33 weissmann , C . ( 1991 ) spongiform encephalopathies . The prion s progress . nature 349 , 569 571 , 1991 . medline abstract 34 collinge , J . and palmer , M . S . ( 1991 ) CJD");
        terms
                .add("creutzfeldt jakob disease : conclusion of a 15 year investigation in france and review of the world literature . neurology 37 , 895 904 . medline abstract 36 westaway , D . , dearmond , S . J . , cayetano canlas , J . , groth , D . , foster , D . , Yang , S . , torchia , M . , carlson , G . A . and prusiner , S . B . ( 1994 ) degeneration of skeletal muscle , peripheral nerves and the central nervous system in transgenic mice overexpressing wild type prion proteins . Cell 76 , 117 129 . medline abstract 37 Riek , R . , hornemann , S . , wider , G . , billeter , M . , glockshuber , R . and wuthrich , K . ( 1996 ) NMR structure of the mouse prion protein");
        terms
                .add("prion protein null mice : abnormal intrinsic properties of hippocampal CA1 pyramidal cells . brain pathol . ( abstract ) . 54 bateman , D . , hilton , D . , Love , S . , zeidler , M . , Beck , J . and collinge , J . ( 1995 ) sporadic creutzfeldt jakob");
        terms
                .add("creutzfeldt jakob disease in a 26 year old french man . lancet 347 , 1181 . medline abstract 60 collinge , J . , Beck , J . , campbell , T . , estibeiro , K . and Will , R . G . ( 1996 ) prion protein");
        terms
                .add("prion protein gene analysis in new variant cases of creutzfeldt jakob");
        terms
                .add("creutzfeldt jakob disease from humans to transgenic mice expressing chimeric human mouse prion protein");
        terms
                .add("PrP with another protein . Cell 83 , 79 90 . medline abstract 68 collinge , J . , palmer , M . S . , sidle , K . C . L . , Hill , A . F . , gowland , I . , meads , J . , asante , E . , bradley , R . , Doey , L . J . and lantos , P . L . ( 1995 ) unaltered susceptibility to BSE");
        terms.add("BSE in transgenic mice expressing human prion protein");
        terms
                .add("prion protein . nature 375 , 698 700 . medline abstract 74 parchi , P . , castellani , R . , capellari , S . , ghetti , B . , young , K . , Chen , S . G . , farlow , M . , dickson , D . W . , Sims , A . A . F . , trojanowski , J . Q . , petersen , R . B . , and gambetti , P . ( 1996 ) molecular basis of phenotypic variability in sporadic creutzfeldt jakob");
        terms
                .add("creutzfeldt jakob disease . Ann . neurol . 39 , 669 680 . 75 telling , G . C . , parchi , P . , dearmond , S . J . , cortelli , P . , montagna , P . , gabizon , R . , mastrianni , J . , lugaresi , E . , gambetti , P . and prusiner , S . B . ( 1996 ) evidence for the conformation of the pathologic isoform of the prion protein");

        locator.basename = "C:/Trec-2006-Feb07/trec-index";
        locator.initializeReaders();
        locator.uniqueQueries = new ArrayList<String>();
        for (final String term : terms) {
            locator.processWithPMIDTerms2(161, filename, term, 1, 1);
        }
    }

    public static void main(final String[] args) throws IOException,
            ConfigurationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException,
            ClassNotFoundException, InstantiationException,
            URISyntaxException, ParserException {
        final HTMLByteLocator locator = new HTMLByteLocator();
        final long start = System.currentTimeMillis();
        locator.process(args);
        final long elapsedTimeMillis = System.currentTimeMillis() - start;
        final float elapsedTimeMin = elapsedTimeMillis / (60 * 1000F);
        System.out.println("Execution took " + elapsedTimeMin + " minutes");
    }

    public HTMLByteLocator() {
        super();
        pmid2File = new HashMap<Integer, String>();
        results = new ArrayList<TextSegment>();
        topicCounts = new HashMap<Integer, Integer>();
        processTargetTerms = true;
        outputWriter = new PrintWriter(System.out);
    }

    public HTMLByteLocator(final String filename) {
        this();
        this.sourceFilename = filename;
        addFile(new File(sourceFilename));
    }

    private void process(final String[] args) throws ConfigurationException,
            IOException, ParserException {
        final Options options = new Options();
        final Option pmidOption = OptionBuilder.withArgName("pmid").hasArg()
                .withDescription(
                        "The PMID of the article in which to locate the text")
                .create("pmid");
        options.addOption(pmidOption);

        final Option batchOption = OptionBuilder
                .withArgName("batchFile")
                .hasArg()
                .withDescription(
                        "The location of the batch file containing terms to lookup. Each line of the "
                                + "batch file must contain PMID\tsentence-id\ttext-fragment\t The last two fields are optional. If "
                                + "the sentence-id is not provided, the entire article is assumed to match the query. If the "
                                + "text fragment is not provided, the entire sentence is assumed to match the query. It is an error "
                                + "to provide a text fragment without a sentence id unless the text fragment is unique within the article.")
                .create("batch");
        options.addOption(batchOption);

        final Option corpusOption = OptionBuilder
                .withArgName("corpusLocation")
                .hasArg()
                .withDescription(
                        "The corpusLocation of the TREC HTML corpus (parent of all journal-specific directories)")
                .create("l");
        corpusOption.setRequired(true);
        options.addOption(corpusOption);

        final Option targetOption = OptionBuilder.withArgName("target")
                .hasArg().withDescription("The text to locate").create("t");
        options.addOption(targetOption);

        final Option basenameOption = OptionBuilder
                .withArgName("basename")
                .hasArg()
                .withDescription(
                        "A path to load documents. [default is trec-index/index and assumes "
                                + "trec-index exists in the current directory. ]")
                .create("basename");
        options.addOption(basenameOption);

        final Option outputOption = OptionBuilder.withArgName("writer")
                .hasArg().withDescription("Name of file to store writer to")
                .create("writer");
        options.addOption(outputOption);

        final Option thresholdOption = OptionBuilder
                .withArgName("threshold")
                .hasArg()
                .withDescription(
                        "Proportion of characters that must align from the query")
                .create("threshold");
        options.addOption(thresholdOption);

        final Option verboseOption = OptionBuilder.withDescription(
                "Be extra verbose").create("v");
        options.addOption(verboseOption);

        final Option uniqueOption = OptionBuilder
                .withDescription(
                        "Eliminate duplicate results and write them to the writer filename specified appended with \"-unique\"")
                .create("unique");
        options.addOption(uniqueOption);

        final Option skipSingleOption = OptionBuilder
        .withDescription(
                "Skip when the 'terms' only matches a single term \"-skipsingle\"")
        .create("skipsingle");
        options.addOption(skipSingleOption);

        final Option noRefsOption = OptionBuilder.withDescription(
                "Remove references from the HTML file with \"-noref\"")
                .create("noref");
        options.addOption(noRefsOption);

        final Option tagOption = OptionBuilder
                .withArgName("tag")
                .hasArg()
                .withDescription(
                        "A tag assigned by the submitting group that should be distinct from all the group's other runs")
                .create("tag");
        options.addOption(tagOption);

        final Option maxLengthOption = OptionBuilder
                .withArgName("max")
                .hasArg()
                .withDescription(
                        "The maximum length of the text snippet (in characters) to match against the destination article. Snippets longer than max are split in three segments A, B, C. Segments A and C have max length and are used to match against the article")
                .create("max");
        options.addOption(maxLengthOption);

        final Option ignoreLengthOption = OptionBuilder
                .withArgName("ignore")
                .hasArg()
                .withDescription(
                        "The maximum length of the text snippet (in characters) to match against the destination to be considered. Snippets longer than ignore will not be considered unless they are directly found (nonContiguousSearch will not be applied)")
                .create("ignore");
        options.addOption(ignoreLengthOption);

        // parse the command line arguments
        final CommandLineParser parser = new GnuParser();
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        } catch (final ParseException e) {
            System.err.println(e.getMessage());
            usage(options);
            System.exit(1);
        }

        this.verbose = line.hasOption("v");
        this.unique = line.hasOption("unique");
        this.noref = line.hasOption("noref");

        final int pmid;
        if (line.hasOption("pmid")) {
            pmid = Integer.valueOf(line.getOptionValue("pmid"));
        } else {
            pmid = UNDEFINED_PMID;
        }

        if (line.hasOption("batch")) {
            isBatchFile = true;
            batchFile = line.getOptionValue("batch");
        }
        LOG.info("Using batch: " + batchFile);

        final String corpusLocation = line.getOptionValue("l");

        if (line.hasOption("t")) {
            target = line.getOptionValue("t");
        }

        if (line.hasOption("writer")) {
            output = line.getOptionValue("writer");
        }

        if (line.hasOption("basename")) {
            basename = line.getOptionValue("basename");
        }

        if (line.hasOption("threshold")) {
            threshold = Float.parseFloat(line.getOptionValue("threshold"));
        }

        if (line.hasOption("tag")) {
            runtag = line.getOptionValue("tag");
        }
        if (line.hasOption("max")) {
            maxLength = Integer.parseInt(line.getOptionValue("max"));
        } else {
            maxLength = DEFAULT_MAX_LENGTH;
        }
        LOG.info("Using max: " + maxLength);
        if (line.hasOption("ignore")) {
            ignoreLength = Integer.parseInt(line.getOptionValue("ignore"));
        } else {
            ignoreLength = DEFAULT_IGNORE_LENGTH;
        }
        LOG.info("Using ignore: " + ignoreLength);

        if (!isBatchFile) {
            if (target == null || pmid == UNDEFINED_PMID) {
                System.out
                        .println("Please specify a PMID and text to locate.\n");
                usage(options);
                System.exit(0);
            }
        } else {
            if (batchFile == null) {
                System.out
                        .println("Please specify the location of the batch file.\n");
                usage(options);
                System.exit(0);
            }
        }

        if (output != null) {
            outputWriter = new FileWriter(output);
        } else {
            // if no file is specified, use the console
            outputWriter = new PrintWriter(System.out);
        }

        collectFiles(corpusLocation);

        initializeReaders();
        if (isBatchFile) {
            processBatchFile();
        } else {
            processWithPMIDTerms2(UNDEFINED_TOPIC_ID, pmid, target,
                    UNDEFINED_RANK_NUMBER, UNDEFINED_RANK_VALUE);
        }

        outputWriter.close();

        if (verbose) {
            System.out.println("Results:");
            for (final int topicId : topicCounts.keySet()) {
                System.out.println("Topic " + topicId + ": "
                        + topicCounts.get(topicId));
            }
        }

        System.exit(0);
    }

    /**
     * Writes a text segment to the given writer.
     *
     * @param writer Writer to write teh segment to
     * @param segment Segment to write
     * @throws IOException if there is a problem writing the segment
     */
    private void writeSegment(final TextSegment segment, final Writer writer)
            throws IOException {
        writer.write(segment.toString());
        writer.write('\t');
        writer.write(runtag);
        writer.write("\n");
        writer.flush();
    }

    private void initializeReaders() throws ConfigurationException,
            IOException {
        if (reader == null) {
            System.out.println("Initializing document index...");
            System.out.flush();
            final DocumentIndexManager docmanager = new DocumentIndexManager(
                    basename);
            reader = new DocumentStoreReader(docmanager);

            reader.readPMIDs();
            System.out.println("Document index ready.");
            System.out.flush();
            processTargetTerms = false;
            wordReader = docmanager.getWordReader();
            termProcessor = docmanager.getTermProcessor();
            System.out.println("wordReader="
                    + wordReader.getClass().getCanonicalName());
            System.out.println("termProcessor="
                    + termProcessor.getClass().getCanonicalName());
        }
    }

    public void processBatchFile() throws IOException, ParserException {
        System.out.println("Processing lookups in " + batchFile);
        final InputStream is = new FileInputStream(batchFile);
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null) {
            lineCount++;
            if (line.charAt(0) == '#') {
                if (verbose) {
                    System.out.println("Skipping " + line);
                }
                continue; // ignore comments.
            }

            if (verbose) {
                System.out.println("Processing " + line);
            }

            final String[] tokens = line.split("[\t]");

            int pmid = UNDEFINED_PMID;
            String terms = null;
            int sentenceId = UNDEFINED_SENTENCEID;
            int rankNumber = UNDEFINED_RANK_NUMBER;
            float rankValue = UNDEFINED_RANK_VALUE;
            int topicId = UNDEFINED_TOPIC_ID;
            try {
                if (tokens.length > 0 && tokens[0].length() > 0) {
                    pmid = Integer.parseInt(tokens[0]);
                }

                if (tokens.length > 1 && tokens[1].length() > 0) {
                    sentenceId = Integer.parseInt(tokens[1]);
                }

                if (tokens.length > 2 && tokens[2].length() > 0) {
                    rankNumber = Integer.parseInt(tokens[2]);
                }

                if (tokens.length > 3 && tokens[3].length() > 0) {
                    rankValue = Float.parseFloat(tokens[3]);
                }

                if (tokens.length > 4 && tokens[4].length() > 0) {
                    terms = tokens[4].trim();
                }

                if (tokens.length > 5 && tokens[5].length() > 0) {
                    topicId = Integer.parseInt(tokens[5]);
                }

                if (verbose) {
                    System.out.println("pmid = " + pmid);
                    System.out.println("sentence id = " + sentenceId);
                    System.out.println("rank number = " + rankNumber);
                    System.out.println("rank value = " + rankValue);
                    System.out.println("terms = " + terms);
                    System.out.println("topic id = " + topicId);
                }

                batchLineNumber++;

                if (batchLineNumber % 250 == 0) {
                    final long newTime = System.currentTimeMillis();
                    final long elapsedTimeMillis = newTime - timerStart;
                    timerStart = newTime;
                    final float elapsedTimeSec = elapsedTimeMillis / (1000F);
                    LOG.info("Last two 250 lines took " + elapsedTimeSec
                            + " seconds.");
                }

                process(topicId, pmid, sentenceId, rankNumber, rankValue,
                        terms);
            } catch (final NumberFormatException e) {
                System.err
                        .println("Error: line "
                                + lineCount
                                + " Could not be parsed (integer field is not a number). Line ignored.");
            }
        }
    }

    private void process(final int topicId, final int pmid,
            final int sentenceId, final int rankNumber, final float rankValue,
            final String terms) throws IOException, ParserException {
        if (pmid != UNDEFINED_PMID && sentenceId != UNDEFINED_SENTENCEID) {
            processSentenceID(topicId, sentenceId, terms, rankNumber,
                    rankValue);
        } else if (pmid != UNDEFINED_PMID) {
            if (terms == null) {
                // just pmid, no text:
                processPMID(topicId, pmid, rankNumber, rankValue);
            } else {
                // text fragment with pmid, but not sentence ID:
                boolean skipLine = false;
                if (skipsingle) {
                    if (terms.indexOf(" ") == -1) {
                        skipLine = true;
                    }
                }
                if (!skipLine) {
                    // Skip single terms, only process if there are >1 terms
                    processWithPMIDTerms2(topicId, pmid, terms, rankNumber,
                            rankValue);
                }
            }
        }
    }

    /**
     * Display command line usage for the user.
     *
     * @param options Possible options for the command line.
     */
    private static void usage(final Options options) {
        final String header = "This tool locates fragments of text within the HTML of the TREC corpus.";
        final String footer = "Example for a single term lookup: "
                + "\tjava -jar locator.jar -l ./corpus -t 'text to locate'"
                + "\n"
                + "Example of batch lookups:"
                + "\tjava -jar locator.jar -l ./corpus -batch /path/to/batchfile";

        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar locator.jar", header, options, footer);
    }

    public void processSentenceID(final int topicId, final int sentenceId,
            String terms, final int rankNumber, final float rankValue)
            throws IOException, ParserException {
        if (terms == null) {
            // obtain terms from docstore:
            terms = reader.document(sentenceId).toString();
        }
        processWithPMIDTerms2(topicId, (int) reader.getPMID(sentenceId),
                terms, rankNumber, rankValue);
    }

    public void processPMID(final int topicId, final int pmid,
            final int rankNumber, final float rankValue) throws IOException {
        final String articleFilename = pmid2File.get(pmid);
        if (verbose) {
            System.out.println("Processing " + articleFilename);
        }

        if (articleFilename == null) {
            System.err.println("Error: Cannot locate article for PMID: "
                    + pmid);
            return;
        }

        final File articleFile = new File(articleFilename);
        final long length = articleFile.length();
        final TextSegment segment = createTextSegment(topicId, pmid,
                rankNumber, rankValue, 0, length);
        addResult(segment, results, topicCounts, outputWriter);
    }

    /**
     * Adds a text segment to the result list and write it to the writer stream.
     *
     * @param segment The segment to add
     * @param collection The collection to add the segment to
     * @param countMap A map of topics to result count
     */
    private void addResult(final TextSegment segment,
            final Collection<TextSegment> collection,
            final Map<Integer, Integer> countMap) {
        // add the segment to the result list
        collection.add(segment);

        // update the count to include this segment
        updateTopicCount(segment, countMap);
    }

    /**
     * Adds a text segment to the result list and write it to the writer stream.
     *
     * @param segment The segment to add
     * @param collection The collection to add the segment to
     * @param countMap A map of topics to result count
     * @param writer The writer object to print details to
     * @throws IOException if there is a problem writing the segment to the
     * stream
     */
    private void addResult(final TextSegment segment,
            final Collection<TextSegment> collection,
            final Map<Integer, Integer> countMap, final Writer writer)
            throws IOException {
        addResult(segment, collection, countMap);
        // and write the information to the writer stream
        writeSegment(segment, writer);
    }

    /**
     * @param segment
     * @param countMap
     */
    private void updateTopicCount(final TextSegment segment,
            final Map<Integer, Integer> countMap) {
        // store the total count for this topic
        final int topicId = segment.getTopicId();
        final int count;
        if (countMap.containsKey(topicId)) {
            count = countMap.get(topicId);
        } else {
            count = 0;
        }
        countMap.put(topicId, count + 1);
    }

    private class TextSpanLimits {
        int startIndex;

        int endIndex;

    }

    public void processWithPMIDTerms(final int topicId,
            final String articleFilename, String targetTerms,
            final int rankNumber, final float rankValue,
            final List<Integer> positions,
            final MutableString processedArticleTerms) throws IOException {
        if (verbose) {
            System.out.println("Searching for target: '" + targetTerms
                    + "' with threshold of " + threshold);
        }
        if (targetTerms.startsWith(". ")) {
            targetTerms = targetTerms.substring(2);
            // offset = -1;
        }
        final MutableString processedTargetTerms = new MutableString();
        final int correction = extractTerms(targetTerms, processedTargetTerms);
        TextSpanLimits span = new TextSpanLimits();
        span.startIndex = processedArticleTerms.indexOf(processedTargetTerms);

        if (span.startIndex == -1) {
            // The quick check using indexOf failed. We now need to search
            // using a Non-Contiguous method.
            final int processedTargetTermsLen = processedTargetTerms.length();

            if (processedTargetTermsLen > ignoreLength) {
                LOG.warn("[" + batchLineNumber + "] Text exceeds "
                        + "ignoreLength. Skipping.");
                // We aren't going to use this at all.
                return;
            }

            if (processedTargetTermsLen < maxLength) {
                span = matchNonContiguous(processedArticleTerms,
                        processedTargetTerms, span);
                if (span == null) {
                    return;
                }
            } else {
                final int splitLength;
                if (processedTargetTermsLen < (2 * maxLength)) {
                    splitLength = processedTargetTermsLen / 2;
                } else {
                    splitLength = maxLength;
                }

                LOG.warn("[" + batchLineNumber + "] Text exceeds maxLength, "
                        + "using splitting method (split at " + splitLength
                        + ").");

                // when processedTargetTerms is too long for alignment, we split
                // into three parts A,B,C
                // map A and C to the article, and assume processedTargetTerms
                // spans from A.start to C.end
                final TextSpanLimits spanA = matchNonContiguous(
                        processedArticleTerms, processedTargetTerms.substring(
                                0, splitLength), span);
                if (spanA == null) {
                    return;
                }
                final int end = processedTargetTerms.length() - 1;

                if (end - maxLength < 0) {
                    return;
                }
                if (end > processedArticleTerms.length()) {
                    return;
                }

                final TextSpanLimits spanB = matchNonContiguous(
                        processedArticleTerms, processedTargetTerms.substring(
                                end - splitLength, end), span);
                if (spanB == null) {
                    return;
                }
                span.startIndex = spanA.startIndex;
                span.endIndex = spanB.endIndex;
            }
        } else {
            // The quick check using indexOf worked!
            span.endIndex = span.startIndex + processedTargetTerms.length() - 1;
        }

        final long pmid = getPmidForArticle(articleFilename);
        final int start = positions.get(span.startIndex); // positions[span.startIndex];
        final long length =
        // positions[span.endIndex + (processTargetTerms ? 0 : correction)] -
        // start;
        positions.get(span.endIndex + (processTargetTerms ? 0 : correction))
                - start;

        Integer newRankNumber = rankNumber;
        if (unique) {
            // Get the correct rank number, increment within topicId
            newRankNumber = topicIdToRankNumber.get(topicId);
            if (newRankNumber == null) {
                newRankNumber = 1;
            }
            topicIdToRankNumber.put(topicId, newRankNumber + 1);
        }

        final TextSegment segment = createTextSegment(topicId, pmid,
                newRankNumber, rankValue, start, length);
        addResult(segment, results, topicCounts, outputWriter);
    }

    private TextSpanLimits matchNonContiguous(
            final MutableString processedArticleTerms,
            final MutableString processedTargetTerms,
            final TextSpanLimits span) {
        // try non-contigous match:
        final LongestCommonSubsequence lcs = new LongestCommonSubsequence();
        lcs.longestSubsequence(processedArticleTerms, processedTargetTerms);
        final MutableString LCS = lcs.getLCS();
        if (LCS.length() > (threshold * processedTargetTerms.length())) {
            span.startIndex = lcs.getMatchStartIndexForFirst();
            span.endIndex = span.startIndex + LCS.length();
            return span;
        } else {
            return null;
        }
    }

    /**
     * Creates a new TextSegment object with the given parameters.
     *
     * @param topicId The topic id related to this segment
     * @param pmid Pubmed identifier of this segment
     * @param rankNumber Rank number of this segment
     * @param rankValue Rank value of this segment
     * @param start index of this segment
     * @param length length of this segment
     * @return A new TextSegment object
     */
    public TextSegment createTextSegment(final int topicId, final long pmid,
            final int rankNumber, final float rankValue, final int start,
            final long length) {
        final TextSegment segment = new TextSegment();
        segment.setTopicId(topicId);
        segment.setPmid(pmid);
        segment.setStart(start);
        segment.setLength(length);
        segment.setRankNumber(rankNumber);
        segment.setRankValue(rankValue);
        return segment;
    }

    /**
     * Creates a new dummy TextSegment when there are no entries for a specific
     * topicId.
     *
     * @param topicId The topic id related to this segment
     * @return A new TextSegment object
     */
    public TextSegment createDummyTextSegment(final int topicId) {
        final TextSegment segment = new TextSegment();
        segment.setTopicId(topicId);
        segment.setPmid(0);
        segment.setStart(0);
        segment.setLength(1);
        segment.setRankNumber(1);
        segment.setRankValue(0.0F);
        return segment;
    }

    public void processWithPMIDTerms2(final int topicId, final int pmid,
            final String targetTerms, final int rankNumber,
            final float rankValue) throws IOException, ParserException {
        final String articleFilename = pmid2File.get(pmid);
        if (verbose) {
            System.out.println("Processing " + articleFilename);
        }

        if (articleFilename == null) {
            System.err.println("Error: Cannot locate article for PMID: "
                    + pmid);
            return;
        }
        processWithPMIDTerms2(topicId, articleFilename, targetTerms,
                rankNumber, rankValue);
    }

    public void setWordReader(final WordReader wordReader) {
        this.wordReader = wordReader;
    }

    public void setTermProcessor(final TermProcessor termProcessor) {
        this.termProcessor = termProcessor;
    }

    private int lastTopicId = -1;

    private String lastArticleFilename;

    private List<Integer> lastPositions2;

    private MutableString lastProcessedArticleText;

    private List<String> uniqueQueries = new ArrayList<String>();

    public void processWithPMIDTerms2(final int topicId,
            final String articleFilename, final String targetTerms,
            final int rankNumber, final float rankValue) throws IOException,
            ParserException {

        final MutableString processedArticleText;
        final List<Integer> positions2;
        // int[] positions2 = null;

        if (lastArticleFilename != null
                && lastArticleFilename.equals(articleFilename)) {
            // Reading from the same PMID as before. Just
            // use the results we got last time.
            processedArticleText = lastProcessedArticleText;
            positions2 = lastPositions2;
        } else {
            LOG.info("[" + batchLineNumber + "] Processing PMID file");

            final File input = new File(articleFilename);
            final Reader reader = new FileReader(input);
            final Html2Text html2Text;
            if (noref) {
                html2Text = new Html2TextNoref();
            } else {
                html2Text = new Html2Text();
            }

            html2Text.parse(reader);
            final String noTagsArticleText = html2Text.getText();
            final List<Integer> positions = html2Text.getPositions();

            final StringReader stringReader = new StringReader(noTagsArticleText);
            wordReader.setReader(stringReader);

            final MutableString word = new MutableString();
            final MutableString nonWord = new MutableString();
            int byteCount = 0;

            processedArticleText = new MutableString();
            // The new length of positions2 is unknown, it will certainly
            // be larger than positions because the wordReader
            // will introduce lots of extra spaces, ie, "this (abc) that"
            // will become "this ( abc ) that " after it passes through
            // this phase
            // positions2 = new int[positions.size()];
            positions2 = new ArrayList<Integer>();
            while (wordReader.next(word, nonWord)) {
                if (word.length() > 0) {
                    final int oldLength = word.length();
                    termProcessor.processTerm(word);
                    final int newLength = word.length();
                    final int correction = oldLength - newLength;
                    for (int i = 0; i <= newLength; i++) {
                        positions2.add(positions.get(byteCount) + i);
                        // positions2[processedArticleText.length() + i]
                        // += positions.get(byteCount) + i;
                    }
                    processedArticleText.append(word);
                    processedArticleText.append(' ');
                    byteCount += correction;
                }
                byteCount += word.length() + nonWord.length();
            }
        }

        if ((lastTopicId != topicId) && (lastTopicId != -1)) {

            // We changed topicId and/or pmid, restart the list of
            // unique query strings
            if (lastTopicId != topicId) {
                if (lastTopicId != -1) {
                    if ((lastTopicId + 1) != topicId) {
                        // We skipped a topic ID and need to insert one or
                        // more dummy entries
                        for (int dummyId = lastTopicId + 1; dummyId < topicId; dummyId++) {
                            results.add(createDummyTextSegment(dummyId));
                        }
                    }
                }
            }
            if (!lastArticleFilename.equals(articleFilename)) {
                uniqueQueries = new ArrayList<String>();
            }
        }

        if (unique) {
            if (uniqueQueries.contains(targetTerms)) {
                // Nothing to do, we have done this query before for
                // the current topic/pmid.
                LOG.warn("[" + batchLineNumber + "] "
                        + "Skipping non-unique query for this topic/pmid");
                return;
            } else {
                uniqueQueries.add(targetTerms);
            }
        }

        // Remember for next time, speed up processing
        // if we use the same PMID again (which is pretty likely)
        lastArticleFilename = articleFilename;
        lastProcessedArticleText = processedArticleText;
        lastPositions2 = positions2;
        lastTopicId = topicId;

        processWithPMIDTerms(topicId, articleFilename, targetTerms,
                rankNumber, rankValue, positions2, processedArticleText);

        if (verbose) {
            System.out.println("Found " + results.size() + " results");
        }
    }

    public int extractTerms(final String input, final MutableString result)
            throws IOException {
        int correction = 0;

        if (!processTargetTerms) {
            result.append(input);
        } else {
            wordReader.setReader(new StringReader(input));
            final MutableString word = new MutableString();
            final MutableString nonWord = new MutableString();
            result.setLength(0);

            while (wordReader.next(word, nonWord)) {
                if (word.length() > 0) {
                    final int oldLength = word.length();
                    termProcessor.processTerm(word);
                    final int newLength = word.length();

                    correction += oldLength - newLength;
                    result.append(word);
                    result.append(' ');
                }
            }
        }
        return correction;
    }

    public Map<Integer, String> collectFiles(final String corpusLocation) {
        final File dir = new File(corpusLocation);

        if (!dir.exists() || !dir.canRead() || !dir.isDirectory()) {
            System.err.println("'" + corpusLocation
                    + "' does not exist or is not readable.");
            System.exit(1);
        }

        System.out.println("Initializing HTML corpus...");
        System.out.flush();

        // Set up file and directory filters
        final FilenameFilter htmlFilter = new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".html");
            }
        };

        final FileFilter dirFilter = new FileFilter() {
            public boolean accept(final File file) {
                return file.isDirectory();
            }
        };

        // Collect files in parent directory
        for (final File child : dir.listFiles(htmlFilter)) {
            addFile(child);
        }

        // Collect files in sub directories
        final File[] directories = dir.listFiles(dirFilter);
        if (directories.length > 0) {
            for (final File directory : directories) {
                final String completeLocation = directory.toString();
                final File subdirectory = new File(completeLocation);
                for (final File child : subdirectory.listFiles(htmlFilter)) {
                    addFile(child);
                }
            }
        }

        System.out.println("HTML corpus ready.");
        System.out.flush();
        return pmid2File;
    }

    private void addFile(final File file) {
        final String filename = file.getName();
        final String pmidString;
        final int dotIndex = filename.lastIndexOf('.');

        if (dotIndex != -1) {
            pmidString = filename.substring(0, dotIndex);
        } else {
            pmidString = filename;
        }

        try {
            final int pmid = Integer.parseInt(pmidString);
            if (verbose) {
                System.out.println("Adding " + file.getAbsolutePath());
            }
            pmid2File.put(pmid, file.getAbsolutePath());
        } catch (final NumberFormatException e) {
            LOG.warn("Skipping file " + filename);
        }
    }

    public long getPmidForArticle(String filename) {
        final File file = new File(filename);
        filename = file.getName();
        final String pmidString;
        final int dotIndex = filename.lastIndexOf('.');

        if (dotIndex != -1) {
            pmidString = filename.substring(0, dotIndex);
        } else {
            pmidString = filename;
        }

        try {
            return Integer.parseInt(pmidString);

        } catch (final NumberFormatException e) {
            LOG.warn("Skipping file " + filename);
        }
        return -1;
    }

    public void setProcessTargetTerms(final boolean processTargetTerms) {
        this.processTargetTerms = processTargetTerms;
    }

    public List<TextSegment> getResults() {
        return results;
    }

    public long getCount() {
        return 0;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public void setSourceFilename(final String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }

    public String pmidFromFilename() {
        final File file = new File(getSourceFilename());
        return file.getName().replace(".html", "");
    }
}
