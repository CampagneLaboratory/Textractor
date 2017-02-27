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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static textractor.TextractorConstants.TIMER_LOG;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.DocumentTermPositions;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.LookupResult;
import textractor.datamodel.Sentence;
import textractor.datamodel.TermOccurrence;
import textractor.datamodel.annotation.DoubleTermAnnotation;
import textractor.datamodel.annotation.SingleTermAnnotation;
import textractor.learning.AnnotationFormatWriter;
import textractor.util.ParseOutput;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LookupProteinname extends AbstractLookupProteinname {
    /** Used to log debug and informational messages. */
    private static final Log LOG = LogFactory.getLog(LookupProteinname.class);

    private boolean exportAnnotations;
    private boolean interaction;
    private DbManager dbm;

    public LookupProteinname(final String dictionaryFilename) throws IOException, TextractorDatabaseException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        this(new String[] {
                "-dic",
                DICTIONARY_DIRECTORY + File.separator + dictionaryFilename
        });
    }

    public LookupProteinname(final String[] args) throws IOException, TextractorDatabaseException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        super(args);
        interaction = CLI.isKeywordGiven(args, "-interaction", false);
        exportAnnotations =
            CLI.isKeywordGiven(args, "-exportAnnotations", false);

        dbm = new DbManager(args);
        dbm.beginTxn();
        final String indexBasename =
            dbm.getTextractorManager().getInfo().getCaseSensitiveIndexBasename();
        dbm.commitTxn();
        LOG.info("the index being used: " + indexBasename);
        docManager = new DocumentIndexManager(indexBasename);
    }
    public void close(){
        if (docManager != null) {
            docManager.close();
        }
    }

    public void setExportAnnotations(final boolean exportAnnotations){
        this.exportAnnotations = exportAnnotations;
    }

    @Override
    public void process(final String[] args) throws
            IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final StopWatch timer = new StopWatch();
        timer.start();
        dbm.beginTxn();

        final String singleTermOutputFilename =
            CLI.getOption(args, "-os", LOOKUP_RESULTS_DIRECTORY
                    + File.separator + subLookupResultsDirectory
                    + File.separator + "singleTermLookupResults.out");

        // lookup each term of dictionary in index
        // iterate through results and create annotations
        AnnotationFormatWriter swriter = null;
        if (exportAnnotations) {
            swriter = new AnnotationFormatWriter(docManager,
                    new FileWriter(singleTermOutputFilename), true);
        }

        final Collection<LookupResult> singleTermLookupResults =
            lookupAllTermsByTerm(indexBasename);
        TIMER_LOG.debug("lookup only: " + timer.toString());
        postLookupProcess(dbm, swriter, singleTermLookupResults);
        TIMER_LOG.debug("lookup postprocess: " + timer.toString());
        if (swriter != null) {
            swriter.flush();
        }

        if (interaction) {
            final String doubleTermOutputFilename = CLI.getOption(args, "-od",
                    LOOKUP_RESULTS_DIRECTORY + File.separator
                    + subLookupResultsDirectory + File.separator
                    + "doubleTermlLookupResults.out");
            final AnnotationFormatWriter dwriter =
                new AnnotationFormatWriter(docManager,
                        new FileWriter(doubleTermOutputFilename), true);
            final Map<Integer, DocumentTermPositions> dresults =
                lookupAllTermsByDocument(indexBasename);
            outputPotentialDoubleTermAnnotations(dbm, dwriter, dresults);
            dwriter.flush();
        }

        dbm.commitTxn();
    }

    public void postLookupProcess(final DbManager dbm,
                                  final AnnotationFormatWriter writer,
                                  final Collection<LookupResult> results)
        throws IOException {
        final TextractorManager textractorManager = dbm.getTextractorManager();
        final int newBatchNumber =
            textractorManager.getNextAnnotationBatchNumber();
        int totalAnnotations = 0;
        final List<String> pmidPool = new ArrayList<String>();
        final List<String> termPool = new ArrayList<String>();

        for (final LookupResult lookupResult : results) {
            final int[] documents = lookupResult.getDocuments();
            final int[] counts = lookupResult.getNumberOfOccurrences();
            final String term = lookupResult.getTerm();
            for (int j = 0; j < documents.length; j++) {
                final int document = documents[j];
                final String pmid =
                    textractorManager.getSentence(document).getArticle().getID();
                if (!pmidPool.contains(pmid)) {
                    pmidPool.add(pmid);
                }

                final int termOccurrenceNumber = counts[j];
                if (exportAnnotations) {
                    final Collection<SingleTermAnnotation> annotations =
                        sentenceToProteinNameAnnotations(textractorManager,
                                document, term, newBatchNumber);
                    for (final SingleTermAnnotation annotation : annotations) {
                        totalAnnotations++;
                        // make the annotation persistent, so that we can
                        // load back the value of the attribute:
                        dbm.makePersistent(annotation);
                        // export in the text fragment annotation format:
                        writer.writeAnnotation(annotation);
                    }

                    if (annotations.size() == 0) {
                        LOG.error(pmid + "\t" + term + "\t"
                                + annotations.size() + "\tdocuments_number:"
                                + documents.length);
                        LOG.error(textractorManager.getSentence(document)
                                .getSpaceDelimitedProcessedTerms(docManager));
                    }

                    if (annotations.size() != termOccurrenceNumber) {
                        LOG.error("PMID: " + pmid);
                        LOG.error("document: " + document + ":"
                                + annotations.size() + "|" + term + "|"
                                + termOccurrenceNumber);
                    }
                }
                termPool.add(pmid + "\t" + term + "\t" + termOccurrenceNumber);
            }
        }

        printHits(termPool, pmidPool, textractorManager);

        if (exportAnnotations) {
            LOG.info("Single ProteinNameAnnotations: " + totalAnnotations);
        }
    }

    /**
     * Print the hits for each individual articles according to PMID.
     *
     * @param termPool
     * @param PMIDPool
     * @throws IOException
     */
    private void printHits(final List<String> termPool, final List<String> PMIDPool,
            final TextractorManager tm) throws IOException {
        final String exportDir = LOOKUP_RESULTS_DIRECTORY + File.separator
            + subLookupResultsDirectory + File.separator;
        final String[][] hitString = new String[termPool.size()][3];

        int frequency;
        int originalFrequency;

        for (int i = 0; i < termPool.size(); i++) {
            hitString[i] = termPool.get(i).split("\t");
        }

        for (final String currentPMIDString : PMIDPool) {
            final String filePrefix = exportDir + currentPMIDString;
            final FileWriter articleHitWriter =
                new FileWriter(filePrefix + ".hit");
            final FileWriter articlePartialWriter =
                new FileWriter(filePrefix + ".hit.partial");
            final FileWriter articleExtraWriter =
                    new FileWriter(exportDir + currentPMIDString + ".hit.extra");
            final List<String> articleTerms = new ArrayList<String>();
            final Map<String, Integer> articleTermsMap =
                new Object2IntOpenHashMap<String>();
            final Object2IntMap<String> articlePartialTermsMap =
                new Object2IntOpenHashMap<String>();

            for (int j = 0; j < termPool.size(); j++) {
                if (hitString[j][0].equals(currentPMIDString)) {
                    final String term = hitString[j][1];
                    frequency = Integer.parseInt(hitString[j][2]);
                    if (!articleTerms.contains(term)) {
                        articleTerms.add(term);
                        articleTermsMap.put(term, frequency);
                    } else {
                        originalFrequency = articleTermsMap.get(term);
                        articleTermsMap.put(term, frequency + originalFrequency);
                    }
                }
            }

            final List<String> extra = new ArrayList<String>();

            // to find the extra terms not in dictionary, such as "the rho" and
            // "rho gtpase" form "the rho gtpase"
            for (int j = 0; j < articleTerms.size(); j++) {
                final String termA = articleTerms.get(j);
                final String[] wordsA = termA.split("\\s");
                for (int k = 0; k < articleTerms.size(); k++) {
                    if (j == k) {
                        continue;
                    }
                    final String termB = articleTerms.get(k);
                    final String[] wordsB = termB.split("\\s");
                    if (wordsA.length > 1 && wordsB.length > 1) {
                        final int minLength = Math.min(wordsA.length, wordsB.length);

                        // maximum overlapping length should be minLength-1;
                        for (int overlappingLength = 1; overlappingLength < minLength; overlappingLength++) {
                            int overlapped = 0;
                            for (int currentPosition = 0; currentPosition < overlappingLength; currentPosition++) {
                                if (wordsA[wordsA.length - overlappingLength + currentPosition].equals(wordsB[currentPosition])) {
                                    overlapped++;
                                }
                            }

                            if (overlapped == overlappingLength) {
                                final StringBuffer tempBuffer =
                                        new StringBuffer(termA);
                                for (int addedWordPosition = overlappingLength; addedWordPosition < wordsB.length; addedWordPosition++) {
                                    tempBuffer.append(' ');
                                    tempBuffer.append(wordsB[addedWordPosition]);
                                }
                                final String tempTerm = tempBuffer.toString();

                                if (!articleTermsMap.containsKey(tempTerm)) {
                                    final LookupResult lookupResult =
                                        dictionary.lookup(tempTerm, docManager);
                                    if (lookupResult != null) {
                                        final long startDocument = tm.getArticleByPmid(Long.parseLong(currentPMIDString)).getDocumentNumberRangeStart();
                                        final long numberOfDocument = tm.getArticleByPmid(Long.parseLong(currentPMIDString)).getDocumentNumberRangeLength();
                                        frequency = 0;
                                        for (int z = 0; z < lookupResult.getDocuments().length; z++) {
                                            final int document = lookupResult.getDocuments()[z];
                                            if (document >= startDocument && document - startDocument < numberOfDocument) {
                                                frequency += lookupResult.getNumberOfOccurrences()[z];
                                            }
                                        }

                                        // even if the extra term can be queryAndExactOrder in index,
                                        // it may not be in this article, so check frequency>0
                                        if (frequency > 0) {
                                            articleExtraWriter.write(tempTerm + "\t" + frequency + "\n");
                                            extra.add(tempTerm);
                                            articleTermsMap.put(tempTerm, frequency);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            articleTerms.addAll(extra);

            final TermOccurrence[] lookupedTermOccurrences =
                    ParseOutput.convertToTermOccurrence(articleTermsMap);

            // todo: write a junit test
            // calculate the occurence of a term as a part of the other term
            // e.g. to match "a" in "a b" or "c a" but not in "c a b", because "c a" is part of "c a b"
            // "a b" and "c a" are called second level
            for (int j = 0; j < lookupedTermOccurrences.length - 1; j++) {
                final String termA = lookupedTermOccurrences[j].getTerm();
                int partial = 0;
                final List<String> partialList = new ArrayList<String>();
                for (int k = j + 1; k < lookupedTermOccurrences.length; k++) {
                    final String termB = lookupedTermOccurrences[k].getTerm();
                    if (termB.matches("(^|(.+\\s))(" + termA + ")((\\s.+)|$)")) {
                        boolean secondLevel = true;
                        for (final String aPartialList : partialList) {
                            if (termB.matches("(^|(.+\\s))(" + aPartialList + ")((\\s.+)|$)")) {
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


            for (final String term : articleTerms) {
                if (articleTermsMap.get(term) > articlePartialTermsMap.getInt(term)) {
                    articleHitWriter.write(term + "\t"
                            + articleTermsMap.get(term) + "\t"
                            + articlePartialTermsMap.get(term) + "\n");
                } else {
                    articlePartialWriter.write(term + "\t"
                            + articleTermsMap.get(term) + "\t"
                            + articlePartialTermsMap.get(term) + "\n");
                }
            }
            articleExtraWriter.close();
            articleHitWriter.close();
            articlePartialWriter.close();
        }
    }

    private void outputPotentialDoubleTermAnnotations(final DbManager dbm,
            final AnnotationFormatWriter writer,
            final Map<Integer, DocumentTermPositions> documentNumbersToTerms)
        throws IOException {
        int totalAnnotations = 0;

        final TextractorManager textractorManager = dbm.getTextractorManager();
        final int newBatchNumber = textractorManager.getNextAnnotationBatchNumber();
        final Set<Integer> documentNumbers = documentNumbersToTerms.keySet();
        for (final int document : documentNumbers) {
            final String[] terms =
                    documentNumbersToTerms.get(document).getTerms();
            if (terms.length > 1) {
                // iterate through every possible pair of lookupedTerms in this
                // document
                for (int i = 0; i < terms.length - 1; i++) {
                    final String termA = terms[i];
                    for (int j = i + 1; j < terms.length; j++) {
                        final String termB = terms[j];
                        final Collection<DoubleTermAnnotation> annotations =
                                sentenceToDoubleProteinNamesAnnotations(textractorManager, document, termA, termB, newBatchNumber);
                        for (final DoubleTermAnnotation annotation : annotations) {
                            totalAnnotations++;
                            // make the annotation persistent, so that we can load back the value of the attribute:
                            dbm.makePersistent(annotation);
                            // export in the text fragment annotation format:
                            writer.writeAnnotation(annotation);
                        }
                    }
                }
            }
        }
        LOG.info("Double ProteinNameAnnotations: " + totalAnnotations);
    }

    private Collection<SingleTermAnnotation> sentenceToProteinNameAnnotations(
            final TextractorManager tm, final int document, final String term,
            final int newBatchNumber) {
        final Sentence sentence = tm.getSentence(document);
        final List<SingleTermAnnotation> annotations =
            new ArrayList<SingleTermAnnotation>();
        final String text =  // use the index format text to search for protein name
            sentence.getSpaceDelimitedProcessedTerms(docManager).toString();

        final String[] textterms = text.split("\\s");
        final List<Integer> termPositions = termPositionsInText(textterms, term);

        for (final int termPosition : termPositions) {
            final SingleTermAnnotation annotation = tm
                    .createSingleTermAnnotation(newBatchNumber);
            annotation.setCurrentText(sentence.getSpaceDelimitedTerms(
                    docManager).toString()); // output in the original format
            annotation.setSentence(sentence);
            annotation.getTerm().setText(term, termPosition);
            annotations.add(annotation);
        }
        return annotations;
    }

    private static int termLength(final String term) {
        return term.split("\\s").length;
    }

    private Collection<DoubleTermAnnotation>
        sentenceToDoubleProteinNamesAnnotations(final TextractorManager tm,
                final int document, final String termAText,
                final String termBText, final int newBatchNumber) {
        final Sentence sentence = tm.getSentence(document);
        final List<DoubleTermAnnotation> annotations =
            new ArrayList<DoubleTermAnnotation>();
        final String text =
            sentence.getSpaceDelimitedProcessedTerms(docManager).toString();
        final String[] textterms = text.split("\\s");
        final List<Integer> termAPositions =
                termPositionsInText(textterms, termAText);
        final List<Integer> termBPositions =
            termPositionsInText(textterms, termBText);
        final int termALength = termLength(termAText);
        final int termBLength = termLength(termBText);

        for (final int positionA : termAPositions) {
            for (final int positionB : termBPositions) {
                // there should be at least one word in between termA and termB
                if (positionA > positionB) {
                    if ((positionB + termBLength + 1) > positionA) {
                        break;
                    }
                } else if (positionA < positionB) {
                    if ((positionA + termALength + 1) > positionB) {
                        break;
                    }
                } else {
                    break;
                }
                final DoubleTermAnnotation annotation =
                        tm.createDoubleTermAnnotation(newBatchNumber);
                annotation.setCurrentText(text);
                annotation.setSentence(sentence);

                annotation.getTermA().setText(termAText, positionA);
                annotation.getTermB().setText(termBText, positionB);
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    /**
     * An alternative way to calculate the positions of a term in the sentence
     * (textterms) should not be used for real calculation and may be kept for
     * test.
     *
     * @param textterms
     * @param term
     * @return
     */
    private List<Integer> termPositionsInText(final String[] textterms,
            final String term) {
        final MutableString currentTerms =
            Sentence.getSpaceDelimitedProcessedTerms(docManager, term);
        final String currentTerm = currentTerms.toString();
        final List<Integer> termPositions = new IntArrayList();
        for (int i = 0; i < textterms.length - termLength(currentTerm) + 1; i++) {
            final StringBuffer textSegmentBuffer = new StringBuffer();
            for (int j = 0; j < termLength(currentTerm); j++) {
                final String nextTerm;
                if (stemmer != null) {
                    nextTerm = stemmer.stripAffixes(textterms[i + j]);
                } else {
                    nextTerm = textterms[i + j];
                }
                textSegmentBuffer.append(nextTerm);
                textSegmentBuffer.append(' ');
            }

            final String textSegment = textSegmentBuffer.toString();
            if (currentTerm.equals(textSegment.trim())) {
                termPositions.add(i);
            }
        }
        return termPositions;
    }

    public static void main(final String[] args) throws IllegalAccessException, NoSuchMethodException, ConfigurationException, IOException, JSAPException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException, TextractorDatabaseException {
        final AbstractLookupProteinname lpn = new LookupProteinname(args);
        lpn.process(args);
    }
}
