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

package textractor.tools.expansion;

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.Interval;
import it.unimi.dsi.mg4j.search.IntervalIterator;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import textractor.acronyms.xml.Acronym;
import textractor.acronyms.xml.Expansion;
import textractor.acronyms.xml.Expansions;
import textractor.acronyms.xml.ExpansionsItem;
import textractor.acronyms.xml.ShortForm;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.Sentence;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.docstore.DocumentStoreReader;
import textractor.tools.DocumentQueryResult;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Collect potential expansions of a list of abbreviations or acronyms.
 * User: campagne
 * Date: Jan 9, 2004
 * Time: 5:28:30 PM
 */
public final class CollectExpansions {
    private static final Log LOG = LogFactory.getLog(CollectExpansions.class);
    private float percentThreshold;
    private String template = "( acronym )";
    /**
     * Number of documents inspected to find a new left term.
     */
    private int inspectedDocumentCount;
    private float inclusionCutoff = 0.01f;
    private XMLSerializer xmlSerializer;
    private boolean extendOnLeft;
    private int minimalSupport;
    private boolean verifyExpansions;
    private boolean sample;
    private DocumentIndexManager docmanager;
    private static final int MAX_SENTENCE_INSPECTION = 2000;
    private static final int MAX_SENTENCE_BEFORE_SAMPLE_SKIP = 200;
    private boolean useDocStore;
    private DocumentStoreReader docStoreReader;
    private String outputFilename;
    private String rejectFilename;
    private String acronymsListFileName;
    private String[] commandLineArguments;
    private String basename;
    private TermProcessor termProcessor;
    private Map<Integer, int[]> sentenceCache;
    private IntList intList = new IntArrayList();

    /**
     * Number of composed queries.
     */
    private int queryCount;

    /**
     * Number of individual queries.
     */
    private int termQueryCount;

    public CollectExpansions() {
        super();
        sentenceCache = new Int2ObjectRBTreeMap<int[]>();
    }

    public void setMinimalSupport(final int support) {
        minimalSupport = support;
    }

    public void setUseDocStore(final boolean flag) {
        useDocStore = flag;
    }

    public void setVerifyExpansions(final boolean flag) {
        verifyExpansions = flag;
    }

    public void setExtendOnLeft(final boolean flag) {
        extendOnLeft = flag;
    }

    public void setTemplate(final String newTemplate) {
        template = newTemplate;
    }

    public void setOutputFilename(final String newFilename) {
        outputFilename = newFilename;
    }

    public void setRejectFilename(final String newFilename) {
        rejectFilename = newFilename;
    }

    public void setAcronymsListFileName(final String newFilename) {
        acronymsListFileName = newFilename;
    }

    public void storeCommandLineArguments(final String[] args) {
        commandLineArguments = args;
    }

    public String[] getCommandLineArguments() {
        return commandLineArguments;
    }

    public String getBasename() {
        return basename;
    }

    public void setBasename(final String basename) {
        this.basename = basename;
    }

    public void performCollection() throws ConfigurationException,
            TextractorDatabaseException, NoSuchMethodException,
            IllegalAccessException, IOException, InvocationTargetException,
            InstantiationException, ClassNotFoundException, URISyntaxException,
            SAXException {
        // use Xerces SAX parser:
        System.setProperty("org.xml.sax.parser",
                "org.apache.xerces.parsers.SAXParser");
        System.out.println("Started with template: " + template
                + " and extension on " + (extendOnLeft ? "left" : "right"));

        PrintWriter output = null;
        FileWriter outputFileWriter = null;
        PrintWriter rejectWriter = null;
        BufferedReader br = null;
        try {
            if (outputFilename != null) {
                outputFileWriter = new FileWriter(outputFilename);
                output = new PrintWriter(outputFileWriter);
                final OutputFormat outputFormat = new OutputFormat();
                outputFormat.setIndenting(true);
                xmlSerializer = new XMLSerializer(output, outputFormat);
            } else {
                output = new PrintWriter(System.out);
            }

            rejectWriter = new PrintWriter(new FileWriter(rejectFilename));

            DbManager dbm = null;
            if (!useDocStore) {
                dbm = new DbManager(getCommandLineArguments());
                dbm.beginTxn();
            }
            if (LOG.isInfoEnabled()) {
                LOG.info("using index: " + basename);
            }

            docmanager = new DocumentIndexManager(basename);

            // install a fast term map.
            final StopWatch timer = new StopWatch();
            timer.start();
            System.out.println("Initializing hash term map");
            docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));
            timer.stop();
            System.out.println("Term map installed in " + timer.toString());

            if (useDocStore) {   // initialize the document store reader if needed
                System.out.println("Using doc-store.");
                docStoreReader = new DocumentStoreReader(docmanager);
            }

            termProcessor = docmanager.getTermProcessor();
            printHeader();

            br = new BufferedReader(new FileReader(acronymsListFileName));

            String acronymContent;
            while ((acronymContent = br.readLine()) != null) {
                if (StringUtils.isBlank(acronymContent)) {
                    continue;
                }

                if (acronymContent.charAt(0) == '#') {  //skip comments
                    continue;
                }

                final CandidateAcronym acronym =
                        new CandidateAcronym(acronymContent);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Calling expandAcronym(" + acronym + ")");
                }
                timer.reset();
                timer.start();
                final Set<CandidateExpansion> expansions =
                        expandAcronym(dbm, docmanager, acronym);
                timer.suspend();
                if (expansions.size() > 0) {
                    appendAcronymToOutput(acronym, expansions, output, timer);
                    for (final CandidateExpansion expansion : expansions) {
                        System.out.println("Acronym: " + acronym
                                + " expansion: " + expansion);
                    }
                } else {
                    System.out.println("No expansions: rejecting: "
                            + acronym.getContent());
                    rejectWriter.println(acronym.getContent()); // was not expanded
                }
            }

            xmlSerializer.endElement("acronyms");
            if (outputFileWriter != null) {
                outputFileWriter.close();
            }
            if (!useDocStore && dbm != null) {
                dbm.commitTxn();
            }

            System.out.println("Expansion collection complete.");

            if (useDocStore) {   //  close the document store reader if needed
                System.out.println("Closing doc-store.");
                docStoreReader.close();
            }

            docmanager.close();
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(outputFileWriter);
            IOUtils.closeQuietly(rejectWriter);
            IOUtils.closeQuietly(br);
        }
    }

    public void process(final String[] args) throws ConfigurationException,
            IllegalAccessException, NoSuchMethodException, IOException,
            TextractorDatabaseException, SAXException, InvocationTargetException,
            ClassNotFoundException, InstantiationException, URISyntaxException {
        acronymsListFileName = CLI.getOption(args, "-i", null);

        if (acronymsListFileName == null) {
            System.err.println("-i must be provided. The argument should be the filename of the list of acronym to expand.");
            System.err.println("usage: -i acronym-list [-basename mg4j-index-basename] [-o output.xml]");
            System.exit(1);
        }

        basename = CLI.getOption(args, "-basename", null);
        outputFilename = CLI.getOption(args, "-o", null);
        template = CLI.getOption(args, "-template", "( acronym )");
        rejectFilename = CLI.getOption(args, "-reject-list", "reject-list.txt");
        percentThreshold = CLI.getFloatOption(args, "-threshold", 10f);
        extendOnLeft = !CLI.isKeywordGiven(args, "-right", false);
        final int inclusionCutoffInt = CLI.getIntOption(args, "-ic", 10);
        minimalSupport = CLI.getIntOption(args, "-ms", 5);
        verifyExpansions = CLI.isKeywordGiven(args, "-verify-expansions");
        sample = CLI.isKeywordGiven(args, "-sample");
        useDocStore = CLI.isKeywordGiven(args, "-doc-store");

        // convert percentage to float number 0-1:
        this.inclusionCutoff = inclusionCutoffInt;

        storeCommandLineArguments(args);
        performCollection();
    }

    private void printHeader() {
        try {
            final StringBuffer buffer = new StringBuffer(256);
            xmlSerializer.startDocument();

            buffer.append("Generated by CollectExpansions on ")
                    .append(new Date()).append("\n")
                    .append(" Parameter used for this run:\n")
                    .append("    percentThreshold: ").append(percentThreshold).append("\n")
                    .append("    minimalSupport: ").append(minimalSupport).append("\n")
                    .append("    inclusionCutoff: ").append(inclusionCutoff).append("\n")
                    .append("    sample: ").append(sample).append("\n")
                    .append("    MAX_SENTENCE_INSPECTION: ").append(MAX_SENTENCE_INSPECTION).append("\n")
                    .append("    MAX_SENTENCE_BEFORE_SAMPLE_SKIP: ").append(MAX_SENTENCE_BEFORE_SAMPLE_SKIP).append("\n")
                    .append("-->\n");
            xmlSerializer.comment(buffer.toString());
            xmlSerializer.startElement("", "", "acronyms", new AttributesImpl());
        } catch (final SAXException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void appendAcronymToOutput(final CandidateAcronym acronym,
                                       final Set<CandidateExpansion> expansions,
                                       final Writer output,
                                       final StopWatch timer) {
        if (expansions.size() > 0) {
            final Acronym castorAcronym = new Acronym();
            castorAcronym.setShortForm(new ShortForm());
            castorAcronym.getShortForm().setContent(acronym.getContent());
            castorAcronym.getShortForm().setFrequency(acronym.getNucleusFrequency());
            castorAcronym.setExpansions(new Expansions());

            for (final CandidateExpansion expansion : expansions) {
                final ExpansionsItem item = new ExpansionsItem();
                item.setExpansion(new Expansion());
                item.getExpansion().setContent(expansion.toString());
                item.getExpansion().setFrequency(expansion.getFrequency());
                castorAcronym.getExpansions().addExpansionsItem(item);
            }

            try {
                castorAcronym.marshal(xmlSerializer);
                xmlSerializer.comment(" The previous acronym search took: " +
                        DurationFormatUtils.formatDuration(timer.getTime(), "m")
                        + " or " +
                        DurationFormatUtils.formatDuration(timer.getTime(), "S")
                        + "\n" +
                        "Number of MG4J composed queries required: " + queryCount + " \n" +
                        "Number of MG4J individual term queries required: " + termQueryCount + "\n" +
                        "Number of documents inspected to expand term on left: " + inspectedDocumentCount + "\n" +
                        "Number of documents skipped: " + acronym.getSkipCount() + "\n" +
                        "");

                if (output != null) {
                    output.flush();
                }
            } catch (final MarshalException e) {
                e.printStackTrace();
            } catch (final ValidationException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Set<CandidateExpansion> expandAcronym(final DbManager dbm,
                                                  final DocumentIndexManager docmanager,
                                                  final CandidateAcronym acronym) throws TextractorDatabaseException,
            IOException {
        final Set<CandidateExpansion> expansions; // result: Set<List<String>> set of list of words in expansions.
        expansions = new HashSet<CandidateExpansion>();
        CandidateExpansion candidateExpansion = new CandidateExpansion();
        final Set<CandidateExpansion> allPossibleCandidates = new HashSet<CandidateExpansion>(); // set of lists such as candidateExpansion

        int matchCount;

        clearSentenceCache();
        int acronymFrequency = 1;
        queryCount = 0;
        termQueryCount = 0;
        inspectedDocumentCount = 0;
        do {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Calling calculateFrequency(acronym: " + acronym + ",candidate expansion: " + candidateExpansion + ")");
            }
            matchCount = calculateFrequency(acronym, candidateExpansion, docmanager, dbm);
            if (candidateExpansion.length() == 0) {
                // matchCount is the frequency of (acronym)
                acronymFrequency = matchCount;
            }
            if (matchCount == 0) {
                LOG.info("Error: this query should have matched: acronym: >" + candidateExpansion.toString() + "< (" + acronym + ")");
                break;
            }
            if (ExpansionHasEnoughSupport(matchCount, acronymFrequency)) {
                CandidateExpansion longerExpansion;
                do {
                    longerExpansion = candidateExpansion.suggestLongerExpansion(docmanager, extendOnLeft, getMinimalSupport(acronym));
                    if (longerExpansion != null) {
                        allPossibleCandidates.add(longerExpansion);
                    }
                } while (longerExpansion != null);


            } else {
                stoppedMatching(candidateExpansion, expansions, allPossibleCandidates);
            }

            LOG.debug("Number of possible candidates: " + allPossibleCandidates.size());
            // use the next available possible candidate, and remove from list of possible:
            final Iterator<CandidateExpansion> iterator = allPossibleCandidates.iterator();
            if (iterator.hasNext()) {
                candidateExpansion = iterator.next();
                iterator.remove();
            } else {
                break;
            }
        } while (allPossibleCandidates.size() > -1);

        final Iterator<CandidateExpansion> it = expansions.iterator();
        while (it.hasNext()) {
            final CandidateExpansion expansion = it.next();
            // remove this expansion if it does not reach minimal support conditions:
            if (expansion.getFrequency() < minimalSupport) {
                LOG.debug("Removing " + expansion + " (not enough support)");
                it.remove();
            }
        }

        for (final CandidateExpansion expansion : expansions) {
            // prune the result: remove each successful expansion that is shorter than this one.
            expansions.remove(expansion.getShorterExpansion());

        }
        for (final CandidateExpansion expansion : expansions) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Confirmed expansion " + acronym + " -->" + expansion);
            }
        }


        return expansions;
    }

    private int getMinimalSupport(final CandidateAcronym acronym) {
        return (int) Math.max((percentThreshold * acronym.getNucleusFrequency() / 100f), minimalSupport);
    }

    private void stoppedMatching(final CandidateExpansion candidateExpansion,
                                 final Set<CandidateExpansion> expansions,
                                 final Set<CandidateExpansion> allPossibleCandidates) {
        confirmExpansion(candidateExpansion, expansions);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Stopped matching candidateExpansion: " + candidateExpansion);
        }
        // remove the candidate:
        allPossibleCandidates.remove(candidateExpansion);

    }

    /**
     * Confirm that expansion has required support..
     *
     * @param confirmedExpansion
     * @param expansions
     */
    private void confirmExpansion(final CandidateExpansion confirmedExpansion,
                                  final Set<CandidateExpansion> expansions) {
        if (testInclusionCutoff(confirmedExpansion)) {
            expansions.add(confirmedExpansion);
        }
    }

    /**
     * Test if this expansion includes a significant proportion of the
     * occurences of the shorter expansion.
     *
     * @param candidateExpansion
     * @return True if inclusion is above cuttoff, False otherwise.
     */
    private boolean testInclusionCutoff(final CandidateExpansion candidateExpansion) {
        final CandidateExpansion shorterExpansion =
                candidateExpansion.getShorterExpansion();
        if (shorterExpansion == null) {
            return true;
        }

        final float minimumValue =
                ((float) shorterExpansion.getFrequency()) * inclusionCutoff / 100f;

        return candidateExpansion.getFrequency() >= minimumValue;
    }

    private boolean ExpansionHasEnoughSupport(final int matchCount, final int acronymFrequency) {
        return matchCount > Math.max((percentThreshold / 100f * acronymFrequency), minimalSupport); // matchCount at least thresold % of the acronymFrequency
        // and contained in at least 'minimalSupport' sentences.
        //return matchCount >= percentThreshold;
    }

    /**
     * Query the index with "candidate-expansion (acronym)" and calculate the
     * frequency of the terms.
     * This method takes advantage of the cached results from previous queries
     * (with shorter expansions) and add one word on the left.
     *
     * @param acronym
     * @param candidateExpansion
     * @param docmanager
     * @param dbm
     * @return The frequency of the expansion ( acronym ).
     * @throws TextractorDatabaseException
     */
    private int calculateFrequency(final CandidateAcronym acronym,
                                   final CandidateExpansion candidateExpansion,
                                   final DocumentIndexManager docmanager,
                                   final DbManager dbm) throws TextractorDatabaseException, IOException {
        final String[] terms;
        // skip acronyms that are not in the index:
        if (docmanager.findTermIndex(convert(acronym.getContent()).toString()) == DocumentIndexManager.NO_SUCH_TERM) {
            return 0;
        }

        terms = constructFullQuery(acronym, candidateExpansion, extendOnLeft);

        // check that each term exists in the index:
        for (final String term : terms) {
            if (docmanager.findTermIndex(term) == DocumentIndexManager.NO_SUCH_TERM) {
                return 0;
            }
        }

        final DocumentIterator matches;
        if (candidateExpansion.getShorterExpansion() == null) {
            LOG.debug("Consecutive native query");
            matches = docmanager.queryAndExactOrderMg4jNative(terms);
            queryCount++;
            termQueryCount += terms.length;
        } else {
            final DocumentQueryResult cachedResult =
                    candidateExpansion.getShorterExpansion().getQueryResult(null);
            if (extendOnLeft){
                matches = docmanager.extendOnLeft(cachedResult, candidateExpansion.firstWord());
            } else {
                matches = docmanager.extendOnRight(cachedResult, candidateExpansion.lastWord());
            }
            queryCount++;
            termQueryCount += 1;
        }

        int matchCount = 0;
        int willSkipN = 0;
        int[] sentence;
        int inspectedSinceLastSkip = 0;
        while (matches.hasNext()) {
            final int document = matches.nextDocument();
            final DocumentQueryResult queryResult = candidateExpansion.getQueryResult(matches.indices());

            // this intervalIterator corresponds to the current document (matches.nextDocument()).
            final IntervalIterator intervalIterator = matches.intervalIterator();
            queryResult.addMatch(document);

            if (willSkipN > 0) {  // we skip some sentence lookup here.
                /* We do not use the index skip method because we cache the result of the full query
                   for reuse in subsequent candidate expansions. If we used skip, our cache would not
                   be consistent between the parent expansion and the shorter expansion.
                */
                --willSkipN;
                continue;
            }
            sentence = getSentence(dbm, document);
            ++inspectedDocumentCount;

            ++inspectedSinceLastSkip;

            //iterate through all the interval in current document
            while (intervalIterator.hasNext()) {
                final Interval interval = intervalIterator.next();
                final int extraPosition =
                        (extendOnLeft ? interval.left - 1 : interval.right + 1);

                queryResult.addInterval(document, interval);

                final int potentialExpansionLeftWord =
                        getExtraWord(sentence, extraPosition);

                if (potentialExpansionLeftWord != DocumentIndexManager.NO_SUCH_TERM) {
                    // verifying expansions slows everything down to a crawl but is useful for debugging.
                    if (verifyExpansions) {
                        verifyExpansion(docmanager, potentialExpansionLeftWord, candidateExpansion, acronym, sentence, document);
                    }
                    candidateExpansion.addSupportToWord(potentialExpansionLeftWord, 1);
                }

                ++matchCount;
                if (sample) {
                    final int skipCount;
                    if (acronym.getNucleusFrequency() != 0) {
                        // we know the frequency of the nucleus:

                        skipCount = Math.max(((acronym.getNucleusFrequency() / MAX_SENTENCE_INSPECTION) - 1), 0);
                    } else {
                        // we approximate.
                        if (matchCount < MAX_SENTENCE_BEFORE_SAMPLE_SKIP) {
                            skipCount = 0;
                        } else {
                            skipCount = (int) Math.round(Math.log(inspectedSinceLastSkip));
                        }
                    }
                    if (skipCount > 0) {
                        willSkipN = skipCount;
                        acronym.skipped(skipCount);
                        matchCount += skipCount;
                        inspectedSinceLastSkip = 0;
                    }
                }
            }
        }
        matches.dispose();
        if (candidateExpansion.getShorterExpansion() == null) {
            acronym.setNucleusFrequency(matchCount);
        }

        candidateExpansion.setFrequency(matchCount);
        adjustNewWords(candidateExpansion, acronym.getNucleusFrequency());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Query matched #" + matchCount + " sentences");
        }
        return matchCount;
    }

    private int[] getSentence(final DbManager dbm, final int document) throws IOException {
        int[] result = sentenceCache.get(document);
        if (result != null) {
            return result;
        }  else {
            result = getSentenceFromStore(dbm, document);
            sentenceCache.put(document, result);
            return result;
        }
    }

    private int[] getSentenceFromStore(final DbManager dbm, final int document) throws IOException {
        if (useDocStore) {
            intList.clear();
            docStoreReader.document(document, intList);
            return intList.toIntArray();
        } else { // use database
            final Sentence sentence =
                    dbm.getTextractorManager().getSentence(document);
            return docmanager.extractTerms(sentence.getText());
        }
    }

    private void clearSentenceCache() {
        sentenceCache.clear();
    }

    private void adjustNewWords(final CandidateExpansion candidateExpansion,
                                final int skippedSentences) {
        candidateExpansion.adjust(minimalSupport, inclusionCutoff, skippedSentences);

    }

    private MutableString convert(final CharSequence content) {
        final MutableString word = new MutableString(content);
        termProcessor.processTerm(word);
        return word;
    }

    /**
     * Constructs the nucleus query. This query is ( acronym ). The token acronym is
     * replaced with the acronym that is currently being expanded. For instance,
     * <p/>
     * ( acronym )
     * <p/>
     * is replaced with
     * <p/>
     * ( PKC ) if PKC is the current acronym.
     *
     * @param acronym The current acronym
     * @return An array of strings with words that constitute the nucleus query. For instance "(" "PKC" ")".
     */
    private String[] constructNucleusQuery(final CandidateAcronym acronym) {
        final StringTokenizer st = new StringTokenizer(template, " ");
        final String[] result =
                new String[st.countTokens() - 1 + acronym.getNumberOfTerms()];
        int i = 0;
        while (st.hasMoreTokens()) {
            final String token = st.nextToken();
            if ("acronym".equals(token)) {
                final StringTokenizer sta =
                        new StringTokenizer(acronym.getContent(), " ");
                while (sta.hasMoreTokens()) {
                    result[i++] = sta.nextToken();
                }
            } else {
                result[i++] = token;
            }
        }
        return result;
    }

    /**
     * Assemble the query: candidate-expansion (acronym).
     *
     * @param acronym
     * @param candidateExpansion
     * @param extendOnLeft
     * @return An array where each element is a word of the query (in order).
     */
    private String[] constructFullQuery(final CandidateAcronym acronym,
                                        final CandidateExpansion candidateExpansion, final boolean extendOnLeft) {
        final String[] acronymTerms = constructNucleusQuery(acronym);
        final StringBuffer nucleusQuery = new StringBuffer();
        for (int i = 0; i < acronymTerms.length; i++) {
            final String acronymTerm = acronymTerms[i];
            if (i != 0) {
                nucleusQuery.append(' ');
            }
            nucleusQuery.append(convert(acronymTerm));

        }

        final String[] terms =
                new String[acronymTerms.length + candidateExpansion.length()];
        final Iterator<String> expansionIt = candidateExpansion.iterator();
        int position = 0;
        // assemble the query: candidate-expansion (acronym)
        if (extendOnLeft) {   // put the expansion on left of acronym
            if (LOG.isDebugEnabled()) {
                LOG.debug("Querying for: " + candidateExpansion + " " + nucleusQuery);
            }

            while (expansionIt.hasNext()) {
                terms[position] = convert(expansionIt.next()).toString();
                ++position;
            }
        }

        for (final String acronymTerm : acronymTerms) {
            terms[position] = convert(acronymTerm).toString();
            ++position;
        }

        if (!extendOnLeft) {     // put the expansion on right of acronym
            if (LOG.isDebugEnabled()) {
                LOG.debug("Querying for: " + nucleusQuery + " " + candidateExpansion);
            }
            while (expansionIt.hasNext()) {
                terms[position] = convert(expansionIt.next()).toString();
                ++position;
            }
        }
        return terms;
    }

    private void verifyExpansion(final DocumentIndexManager docmanager,
                                 final int potentialExpansionLeftWord,
                                 final CandidateExpansion candidateExpansion,
                                 final CandidateAcronym acronym,
                                 final int[] sentence,
                                 final int referenceDocument) throws IOException {

        final String[] acronymTerms =
                new String[]{"(", acronym.getContent().trim(), ")"};

        // to extend the expansion.
        final String[] terms = new String[1 + acronymTerms.length + candidateExpansion.length()];

        int position = 0;
        // assemble the query: candidate-expansion (acronym)
        final StringBuffer query = new StringBuffer();

        final Iterator<String> expansionIt = candidateExpansion.iterator();
        final MutableString firstWord =
                new MutableString(potentialExpansionLeftWord);
        termProcessor.processTerm(firstWord);

        terms[position++] = firstWord.toString();
        query.append(firstWord);
        query.append(' ');

        while (expansionIt.hasNext()) {
            final MutableString word = new MutableString(expansionIt.next());
            termProcessor.processTerm(word);
            terms[position] = word.toString();
            query.append(terms[position]);
            query.append(' ');
            ++position;
        }

        for (final String acronymTerm : acronymTerms) {
            final MutableString term = new MutableString(acronymTerm);
            termProcessor.processTerm(term);
            terms[position] = term.toString();
            query.append(terms[position]);
            query.append(' ');
            ++position;
        }
        System.out.println("Querying for: " + query.toString());
        System.out.flush();
        boolean found = false;
        final DocumentIterator matches =
                docmanager.queryAndExactOrderMg4jNative(terms);

        while (matches.hasNext()) {
            final int document = matches.nextDocument();
            if (document == referenceDocument) {
                found = true;
            }
        }

        if (!found) {
            System.err.println("Cannot verify left word: " + potentialExpansionLeftWord + candidateExpansion +
                    " (" + acronym + ") in sentence # " + referenceDocument + " with text: " + docmanager.toText(sentence));
            System.exit(10);
        }
    }


    /**
     * Returns the word at the specific position in sentence.
     *
     * @param documentTerms Terms of the document.
     * @param position      The position of the word in the sentence
     * @return The word at position position in the sentence, or NoSuchTerm is the position is outside of the document.
     */
    private int getExtraWord(final int[] documentTerms, final int position) {
        if (position >= 0 && position < documentTerms.length) {
            return documentTerms[position];
        } else {
            return DocumentIndexManager.NO_SUCH_TERM;
        }
    }

    public float getPercentThreshold() {
        return percentThreshold;
    }

    public void setPercentThreshold(final float threshold) {
        this.percentThreshold = threshold;
    }

    public static void main(final String[] args) throws NoSuchMethodException,
            IllegalAccessException, ConfigurationException, IOException,
            TextractorDatabaseException, InvocationTargetException,
            SAXException, InstantiationException, ClassNotFoundException,
            URISyntaxException {
        final CollectExpansions query = new CollectExpansions();
        query.process(args);
    }
}
