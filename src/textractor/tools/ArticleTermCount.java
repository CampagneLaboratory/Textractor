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
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlparser.util.ParserException;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.datamodel.TermOccurrence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This class determines the frequency of all terms in an Article, and adds
 * the most common terms as an array of TermOccurrence to the relevant Article.
 */
public class ArticleTermCount {
    private static final Log LOG = LogFactory.getLog(ArticleTermCount.class);
    static double exclusionThreshold = 0.4;
    static FileWriter trashCanWriter;
    static final DecimalFormat doubleForm = new DecimalFormat();

    private DbManager dbm;
    private String basename;
    protected ArrayList<String> singleWordExclusionList;
    private String singleWordWildcardExclusionList;
    private ArrayList<String> multipleWordWildCardExclusionList;
    protected String excludedPrefix;
    protected String excludedPostfix;
    private DocumentIndexManager docManager;
    private final boolean toGetAll = true;
    private final boolean toGetMulti = true;
    private final boolean toGetCombined = true;
    int[] frequencies;
    Object[] termList;

    /**
     * Create a new ArticleTermCount object.
     */
    public ArticleTermCount() {
        super();
    }

    public ArticleTermCount(final String[] args) throws IOException,
            TextractorDatabaseException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        dbm = new DbManager(args);
        final TextractorManager textractorManager = dbm.getTextractorManager();
        dbm.beginTxn();
        basename = CLI.getOption(args, "-basename",
            textractorManager.getInfo().getCaseSensitiveIndexBasename());
        dbm.commitTxn();
        System.out.println("Using index: " + basename);
        docManager = new DocumentIndexManager(basename);
        termList = new Object[1024];
        frequencies = new int[100];
        readExclusionList(args);
    }

    private void parseSingleFile(final int targetNumber) throws IOException {
        final int totalFoundTermNumber = 0;
        System.out.println("totalFoundTermNumber: " + totalFoundTermNumber);
        printFrequencies(totalFoundTermNumber, targetNumber);
    }

    private TermOccurrence[] removeRedundancy(final long pmid,
        final TermOccurrence[] topTermOccurrences) throws IOException {
        if (topTermOccurrences == null || topTermOccurrences.length == 0) {
            return null;
        }

        final boolean[] markToRemove = new boolean[topTermOccurrences.length];
        int finalRemovedCount = 0;
        markToRemove[0] = false;

        // remove the terms in the single-word term exclusion list
        for (int i = 0; i < topTermOccurrences.length; i++) {
            if (exclude(pmid, topTermOccurrences[i])) {
                markToRemove[i] = true;
                finalRemovedCount++;
            }
        }

        finalRemovedCount = remove(pmid, 1, topTermOccurrences, markToRemove, finalRemovedCount);
        if (finalRemovedCount == 0) {
            return null;
        }
        final TermOccurrence[] finalTopTermOccurrences = new TermOccurrence[topTermOccurrences.length - finalRemovedCount];
        int newCount = 0;
        for (int i = 0; i < topTermOccurrences.length; i++) {
            if (!markToRemove[i]) {
                finalTopTermOccurrences[newCount++] = topTermOccurrences[i];
            }
        }

        return finalTopTermOccurrences;
    }

    protected final boolean exclude(final long pmid, final TermOccurrence termOccurrence) throws IOException {
        final String termString = termOccurrence.getTerm();
        if (singleWordExclusionList.contains(termString)) {
            writeExclusion(termOccurrence, pmid, "0");
            return true;
        } else if (termString.matches(singleWordWildcardExclusionList)) {
            writeExclusion(termOccurrence, pmid, "1");
            return true;
        } else if (termString.matches(excludedPrefix)) { //term started with "x"
            writeExclusion(termOccurrence, pmid, "2");
            return true;
        } else if (termString.matches("^-.+") || //term started with "-"
                termString.matches(".+-$") || //term ended with "-"
                termString.replaceAll("\\s", "").matches("\\d+")) {
            writeExclusion(termOccurrence, pmid, "3");
            return true;
        } else if (termString.matches(excludedPostfix)) { //term ended with " x"
            writeExclusion(termOccurrence, pmid, "4");
            return true;
        } else {
            final String termStringReplaced = termString.replaceAll("-", " ");
            if (termStringReplaced.matches(excludedPostfix)) {//term ended with "-x"
                writeExclusion(termOccurrence, pmid, "5");
                return true;
            }
        }
        return false;
    }

    protected final void writeExclusion(final TermOccurrence termOccurrence,
                                  final long pmid,
                                  final String category)
            throws IOException {
        final StringBuffer exclusion =
                new StringBuffer(termOccurrence.toString());
        exclusion.append('@');
        exclusion.append(category);
        exclusion.append('#');
        exclusion.append(pmid);
        exclusion.append('\n');
        trashCanWriter.write(exclusion.toString());
    }

    private void writeExclusion(final TermOccurrence termOccurrenceA,
                                final long pmid,
                                final String category,
                                final TermOccurrence termOccurrenceB,
                                final double ratio)
            throws IOException {
        final StringBuffer exclusion =
                new StringBuffer(termOccurrenceA.toString());
        exclusion.append('[');
        exclusion.append(doubleForm.format(ratio));
        exclusion.append(']');
        exclusion.append(termOccurrenceB.toString());
        exclusion.append('@');
        exclusion.append(category);
        exclusion.append('#');
        exclusion.append(pmid);
        exclusion.append('\n');
        trashCanWriter.write(exclusion.toString());
    }

    private int remove(final long pmid, final double exclusionThreshold, final TermOccurrence[] topTermOccurrences, final boolean[] markToRemove, final int removedCount) throws IOException {
        int finalRemovedCount = removedCount;

        for (int i = 0; i < topTermOccurrences.length - 1; i++) {
            if (markToRemove[i]) {
                continue;
            }
            for (int j = i + 1; j < topTermOccurrences.length; j++) {
                if (termContainsSpecialCharacter(topTermOccurrences[i].getTerm()) ||
                        termContainsSpecialCharacter(topTermOccurrences[j].getTerm())) {

                    // skip parentheses terms. We should not have to care.., but
                    // regular expressions cannot be used with terms that
                    // contain special characters.
                    continue;
                }
                if (markToRemove[j]) {
                    continue;
                }
                final double ratio =
                        (double) topTermOccurrences[j].getCount() / (double) topTermOccurrences[i].getCount();
                if (ratio >= exclusionThreshold) {
                    // according to topTermOccurrences.compareTo
                    // string length of topTermOccurrences[j] should always be
                    // longer than topTermOccurrences[i],
                    // so we don't have to worry about i.matches(j)
                    if (topTermOccurrences[j].isIncluding(topTermOccurrences[i])) { // j include i
                        markToRemove[i] = true;
                        markToRemove[j] = false;
                        writeExclusion(topTermOccurrences[i], pmid, "6", topTermOccurrences[j], ratio);
                        finalRemovedCount++;
                        break;
                    } else {
                        markToRemove[i] = false;
                        markToRemove[j] = false;
                    }
                } else {
                    markToRemove[i] = false;
                    markToRemove[j] = false;
                    break;
                }
            }
        }
        return finalRemovedCount;
    }

    private boolean termContainsSpecialCharacter(final String term) {
        return term.indexOf(')') != -1;
    }

    private void printFrequencies(final int totalFoundTermNumber, final int targetNumber) throws IOException {
        final int[] topAllTermIndex =
                findTopTermsIdx(frequencies, totalFoundTermNumber, targetNumber);
        if (topAllTermIndex != null) {
            final TermOccurrence[] topAllTermOccurrences =
                new TermOccurrence[topAllTermIndex.length];

            for (int x = 0; x < topAllTermIndex.length; x++) {
                final String term = termList[topAllTermIndex[x]].toString();
                final int[] indexedTerm = docManager.extractTerms(new MutableString(term));
                topAllTermOccurrences[x] = new TermOccurrence(term, indexedTerm, frequencies[topAllTermIndex[x]]);
            }
            Arrays.sort(topAllTermOccurrences);
            final TermOccurrence[] finalTopTermOccurrences =
                    removeRedundancy(0, topAllTermOccurrences);
            for (int i = 0; i < finalTopTermOccurrences.length; i++) {
                final TermOccurrence termOccurrence = finalTopTermOccurrences[i];
                System.out.println(i + " Frequency: " + termOccurrence.getCount() + " Term: " + termOccurrence.getTerm());
            }
        }
    }

    /**
     * @param article
     * @param targetWord
     * @param nGramNumber number of n-grams for multi-word term identification.
     * @return The frequency of the targetWord.
     */
    private int countTerms(final TextractorManager tm, final DocumentIndexManager docmanager, final Article article, final String targetWord, final int nGramNumber) {
        final Object2IntOpenHashMap<MutableString> termMap = new Object2IntOpenHashMap<MutableString>();   // will store the terms seen in the Article and the number of occurrences
        termMap.defaultReturnValue(-1);
        final MutableString[] previousWords = new MutableString[10];
        int keptNPreviousWords;
        int numTerms = 0;

        termMap.clear();  // wipes this instance so it can get used again
        termMap.trim(1000);  // makes the default size of the instance reasonable
        final MutableString nGramString = new MutableString();
        MutableString eachNgramToKeep;
        final long documentNumberLowerBound =
            article.getDocumentNumberRangeStart() - 1;
        final long documentNumberUpperBound =
            documentNumberLowerBound + 1 + article.getDocumentNumberRangeLength() - 1;
        final Iterator<Sentence> it =
            tm.getSentenceIterator(documentNumberLowerBound, documentNumberUpperBound);

        while (it.hasNext()) {
            final Sentence sentence = it.next();
            final String[] sentenceTerms = sentence.getSpaceDelimitedProcessedTerms(docmanager).toString().split("\\s");
            keptNPreviousWords = 0; //reset counter, n-gram should go across difference sentence.
            for (final String sentenceTerm : sentenceTerms) {
                if (sentenceTerm.equalsIgnoreCase(targetWord)) {
                    System.out.println("%%%");
                }

                // update previous words array: remove the first word
                if (keptNPreviousWords == (previousWords.length)) {
                    System.arraycopy(previousWords, 1, previousWords, 0, keptNPreviousWords - 1);
                    --keptNPreviousWords;
                }

                previousWords[keptNPreviousWords] =
                    new MutableString(sentenceTerm);
                keptNPreviousWords++;
                nGramString.length(0);
                final int maxBound = Math.max(0, keptNPreviousWords - nGramNumber);
                final int minBound = keptNPreviousWords - 1;
                // check if 1 to ngram length word(s) are in the map
                for (int i = minBound; i >= maxBound; --i) {
                    int termMapIndex;
                    // for each n-gram (term of n words):
                    if (i != minBound) {
                        nGramString.insert(0, ' ');
                    }
                    nGramString.insert(0, previousWords[i]);

                    if (!isInMultipleWordExclusionList(nGramString.toString())) {
                        //found new entry
                        if ((termMapIndex = termMap.getInt(nGramString)) == -1)
                        {
                            eachNgramToKeep = new MutableString(nGramString);
                            termMap.put(eachNgramToKeep, termMapIndex = numTerms++);
                            if (termMapIndex >= frequencies.length) {
                                frequencies = IntArrays.ensureCapacity(frequencies, frequencies.length * 2);
                                termList = ObjectArrays.ensureCapacity(termList, termList.length * 2);
                            }
                            termList[termMapIndex] = eachNgramToKeep;
                            frequencies[termMapIndex] = 0;
                        }

                        frequencies[termMapIndex]++;
                        if (targetWord != null && nGramString.equalsIgnoreCase(targetWord)) {
                            System.out.print("*" + frequencies[termMapIndex] + "*");
                        }
                    }
                }
                if (targetWord != null) {
                    System.out.println(sentenceTerm);
                }
            }
        }
        return numTerms;
    }

    /**
     * To check is part of or whole string is within the exclusion list.
     *
     * @param nGramString
     * @return True or False.
     */
    protected final boolean isInMultipleWordExclusionList(final String nGramString) {
        for (final Object aMultipleWordWildCardExclusionList : multipleWordWildCardExclusionList) {
            if (nGramString.indexOf(aMultipleWordWildCardExclusionList.toString()) >= 0) {
                return true;
            }
        }

        return false;
    }

    public void parseArticles(final int nGramNumber,
            final double exclusionThreshold, final int targetNumber)
            throws IOException, ConfigurationException {
        int lowerBound = -1;
        dbm.beginTxn();
        final TextractorManager tm = dbm.getTextractorManager();
        final DocumentIndexManager docmanager =
            new DocumentIndexManager(basename);
        final int numberArticles = tm.getLastArticleNumber();
        int upperBound = 0;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Number of articles in database: " + numberArticles);
        }
        while (upperBound < numberArticles) {
            dbm.commitTxn();
            System.out.println("commit " + upperBound);
            dbm.beginTxn();
            upperBound = lowerBound + 100;
            final Iterator<Article> ai = tm.getArticleIterator(lowerBound, upperBound);
            while (ai.hasNext()) {
                final Article article = ai.next();
                final int numTerms =
                    countTerms(tm, docmanager, article, null, nGramNumber);
                putMostFrequentInArticle(numTerms, article,
                        exclusionThreshold, targetNumber);
            }
            lowerBound = upperBound;
        }
        dbm.commitTxn();
    }

    /**
     * Reads the exclusion list into an ArrayList.
     *
     * @throws java.io.IOException           single excluded lists: (the+) singleWordExclusionList, (the+) singleBiologicalCategoricalList, verbList, PreOnly, PostOnly, PrePost
     *                                       PrePost: occurs either first or last word in the term
     *                                       multipleWordWildCardExclusionList: occurs anywhere in the term, but not part of one word for the multi-word term
     */
    protected final void readExclusionList(final String[] args) throws IOException {
        final String singleWordExclusionListFilename = CLI.getOption(args, "-sexclusion", "exclusion/singleWordExclusionList");
        final String singleBiologicalCategoricalListFilename = CLI.getOption(args, "-sbexclusion", "exclusion/singleBiologicalCategoricalList");
        final String aminoacidFilename = CLI.getOption(args, "-aexclusion", "exclusion/AminoAcids");
        final String verbListFilename = CLI.getOption(args, "-vexclusion", "exclusion/verbList");
        final String excludedPrefixFilename = CLI.getOption(args, "-preexclusion", "exclusion/PreOnly");
        final String excludedPostfixFilename = CLI.getOption(args, "-postexclusion", "exclusion/PostOnly");
        final String excludedPrePostFilename = CLI.getOption(args, "-prepostexclusion", "exclusion/PrePost");

        final String singleWordWildcardExclusionListFilename = CLI.getOption(args, "-swexclusion", "exclusion/singleWordWildcardExclusionList");
        final String multipleWordWildcardExclusionListFilename = CLI.getOption(args, "-mwexclusion", "exclusion/multipleWordWildcardExclusionList");

        excludedPrefix = "^(";
        excludedPostfix = ".+\\s(";
        singleWordWildcardExclusionList = "(^|(.+\\s))(";

        BufferedReader br = new BufferedReader(new FileReader(new File(singleWordExclusionListFilename)));
        String line;
        singleWordExclusionList = new ArrayList<String>();
        while ((line = br.readLine()) != null) {
            singleWordExclusionList.add(line.trim());// one single-word term per line
            singleWordExclusionList.add("the " + line.trim());
        }

        //add biological categorical terms to the same singleWordExclusionList
        br = new BufferedReader(new FileReader(new File(singleBiologicalCategoricalListFilename)));
        while ((line = br.readLine()) != null) {
            singleWordExclusionList.add(line.trim());   // one single-word term per line
            singleWordExclusionList.add("the " + line.trim());
        }

        //add amino acids terms to the same singleWordExclusionList
        br = new BufferedReader(new FileReader(new File(aminoacidFilename)));
        while ((line = br.readLine()) != null) {
            singleWordExclusionList.add(line.trim());   // one single-word term per line
            singleWordExclusionList.add("the " + line.trim());
        }

        //add verbs to the same singleWordExclusionList
        br = new BufferedReader(new FileReader(new File(verbListFilename)));
        while ((line = br.readLine()) != null) {
            singleWordExclusionList.add(line.trim());   // one single-word term per line
            singleWordExclusionList.add("the " + line.trim());
        }

        br = new BufferedReader(new FileReader(new File(excludedPrefixFilename)));
        if ((line = br.readLine()) != null) {
            excludedPrefix += "(" + line.trim() + ")";
            singleWordExclusionList.add(line.trim());
        }
        while ((line = br.readLine()) != null) {
            excludedPrefix += "|(" + line.trim() + ")";
            singleWordExclusionList.add(line.trim());
        }

        br = new BufferedReader(new FileReader(new File(excludedPostfixFilename)));
        if ((line = br.readLine()) != null) {
            excludedPostfix += "(" + line.trim() + ")";
            singleWordExclusionList.add(line.trim());
        }
        while ((line = br.readLine()) != null) {
            excludedPostfix += "|(" + line.trim() + ")";
            singleWordExclusionList.add(line.trim());
        }

        br = new BufferedReader(new FileReader(new File(excludedPrePostFilename)));
        while ((line = br.readLine()) != null) {
            excludedPrefix += "|(" + line.trim() + ")";
            excludedPostfix += "|(" + line.trim() + ")";
            singleWordExclusionList.add(line.trim());
        }

        excludedPrefix += ")\\s.+";
        excludedPostfix += ")$";

        br = new BufferedReader(new FileReader(new File(multipleWordWildcardExclusionListFilename)));
        multipleWordWildCardExclusionList = new ArrayList<String>();
        while ((line = br.readLine()) != null) {
            multipleWordWildCardExclusionList.add(line.trim());   // one multiple-word term per line
        }

        br = new BufferedReader(new FileReader(new File(singleWordWildcardExclusionListFilename)));
        if ((line = br.readLine()) != null) {
	    singleWordWildcardExclusionList += "(" + line.trim() + ")";
	}
        while ((line = br.readLine()) != null) {
            singleWordWildcardExclusionList += "|(" + line.trim() + ")";
        }
        singleWordWildcardExclusionList += ")((\\s.+)|$)";
    }

    private void putMostFrequentInArticle(final int totalFoundTermNumber, final Article article, final double exclusionThreshold, final int targetNumber) throws IOException {
        article.setTargetTermOccurenceIndexAll(null);
        article.setTargetTermOccurenceIndexMulti(null);
        article.setTargetTermOccurenceIndexCombined(null);
        final long PMID = article.getPmid();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Number of non-excluded term_list found: " + totalFoundTermNumber);
        }

        // add the most frequent term_list as TermOccurrences to Article
        final int[] topAllTermIndex =
            findTopTermsIdx(frequencies, totalFoundTermNumber, targetNumber);
        if (topAllTermIndex != null) {
            final TermOccurrence[] topAllTermOccurrences =
                new TermOccurrence[topAllTermIndex.length];
            for (int x = 0; x < topAllTermIndex.length; x++) {
                final String term = termList[topAllTermIndex[x]].toString();
                final int[] indexedTerm =
                    docManager.extractTerms(new MutableString(term));
                topAllTermOccurrences[x] =
                    new TermOccurrence(term, indexedTerm, frequencies[topAllTermIndex[x]]);
            }
            Arrays.sort(topAllTermOccurrences);
            final TermOccurrence[] finalTopTermOccurrences =
                removeRedundancy(PMID, topAllTermOccurrences);

            if (finalTopTermOccurrences != null) {
                article.setMostFrequentTerms(finalTopTermOccurrences);
                final int[] topIndex = new int[finalTopTermOccurrences.length];
                for (int i = 0; i < topIndex.length; i++) {
                    topIndex[i] = i;
                }
                if (toGetAll) { //all terms top 2
                    article.setTargetTermOccurenceIndexAll(topIndex);
                }
                if (toGetMulti || toGetCombined) {
                    final int[] topMultiWordTermOccurenceIndices = findMultiWordTermOccurence(PMID, finalTopTermOccurrences, exclusionThreshold);
                    if (toGetMulti) {//only the top multiword term.
                        if (topMultiWordTermOccurenceIndices != null) {
                            article.setTargetTermOccurenceIndexMulti(topMultiWordTermOccurenceIndices);
                        }
                    }

                    if (toGetCombined) {//combined top terms and excluding the subset of multiword term from top list
                        if (topMultiWordTermOccurenceIndices != null) {
                            final int[] targetTermOccurenceIndices = findTargetTermOccurence(PMID, finalTopTermOccurrences, exclusionThreshold);
                            article.setTargetTermOccurenceIndexCombined(targetTermOccurenceIndices);
                        } else {
                            article.setTargetTermOccurenceIndexCombined(topIndex);//no multiword term, return top 2
                        }
                    }
                }
            }
        }
    }

    private int[] findTargetTermOccurence(final long PMID,
            final TermOccurrence[] finalTopTermOccurrences,
            final double exclusionThreshold) throws IOException {
        if (finalTopTermOccurrences == null) {
            return null;
        }
        final boolean[] markToRemove = new boolean[finalTopTermOccurrences.length];
        markToRemove[0] = false;
        final int finalRemovedCount =
            remove(PMID, exclusionThreshold, finalTopTermOccurrences, markToRemove, 0);
        final int[] finalTopTermOccurrenceIndices =
            new int[finalTopTermOccurrences.length - finalRemovedCount];
        int newCount = 0;
        for (int i = 0; i < finalTopTermOccurrences.length; i++) {
            if (!markToRemove[i]) {
                finalTopTermOccurrenceIndices[newCount++] = i;
            }
        }

        return finalTopTermOccurrenceIndices;
    }

    private int[] findMultiWordTermOccurence(final long pmid,
            final TermOccurrence[] finalTopTermOccurrences,
            final double exclusionThreshold) throws IOException {
        if (finalTopTermOccurrences == null || finalTopTermOccurrences.length == 0) {
            return null;
        }

        final int[] multiWordTermOccurenceIndices =
            new int[finalTopTermOccurrences.length];
        int multiWordTermOccurenceCount = 0;
        for (int i = 0; i < finalTopTermOccurrences.length; i++) {
            if (finalTopTermOccurrences[i].getTerm().split(" ").length > 1) {
                multiWordTermOccurenceIndices[multiWordTermOccurenceCount++] = i;
            }
        }
        if (multiWordTermOccurenceCount == 0) {
            return null;
        }

        final TermOccurrence[] multiWordTermOccurences =
            new TermOccurrence[multiWordTermOccurenceCount];
        for (int i = 0; i < multiWordTermOccurences.length; i++) {
            multiWordTermOccurences[i] =
                finalTopTermOccurrences[multiWordTermOccurenceIndices[i]];
        }

        final int[] filteredMultiWordTermOccurenceIndices = findTargetTermOccurence(pmid, multiWordTermOccurences, exclusionThreshold);
        final int[] realIndices =
            new int[filteredMultiWordTermOccurenceIndices.length];
        for (int i = 0; i < realIndices.length; i++) {
            realIndices[i] = multiWordTermOccurenceIndices[filteredMultiWordTermOccurenceIndices[i]];
        }
        return realIndices;

    }

    public int findMinTopFreqIdx(final int[] topfreq, final int[] topidx) {
        int maxindex = 0;
        for (int x = 1; x < topfreq.length; x++) {
            if (topfreq[x] < topfreq[maxindex]) {
                maxindex = x;
            }
        }
        return maxindex;
    }

    public int[] findTopTermsIdx(final int[] freqs, final int numTerms,
            final int targetNumber) {
        final int[] topTermIndex = new int[numTerms];
        int count = 0;
        for (int i = 0; i < numTerms; i++) {
            if (freqs[i] >= targetNumber) {
                topTermIndex[count++] = i;
            }
        }

        if (count == 0) {
            return null;
        }

        final int[] finalTopTermIndex = new int[count];
        System.arraycopy(topTermIndex, 0, finalTopTermIndex, 0, count);
        return finalTopTermIndex;
    }

    public static void main(final String[] args) throws ParserException, IOException, TextractorDatabaseException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        final ArticleTermCount atc = new ArticleTermCount(args);
        final String singleFile = CLI.getOption(args, "-i", null);
        final int nGramNumber = CLI.getIntOption(args, "-n", 5);
        final int targetNumber = CLI.getIntOption(args, "-t", 30); //the number of sentences that the term should occur
        System.out.println("sentence number threshold: " + targetNumber);
        exclusionThreshold = CLI.getDoubleOption(args, "-e", 0.4);
        trashCanWriter = new FileWriter(CLI.getOption(args, "-trash", "trash/trash.txt"));
        doubleForm.setMaximumFractionDigits(2);
        doubleForm.setMinimumFractionDigits(2);
        if (singleFile != null) {
            atc.parseSingleFile(targetNumber);
        } else {
            atc.parseArticles(nGramNumber, exclusionThreshold, targetNumber);
        }
        trashCanWriter.flush();
    }
}
