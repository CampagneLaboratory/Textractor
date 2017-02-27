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

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.Sentence;
import textractor.datamodel.annotation.SingleTermAnnotation;
import textractor.learning.AnnotationFormatWriter;
import textractor.mg4j.index.TermIterator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * A tool identify sentencesthat may countain mutations.
 */
public final class ProteinMutation {
    /** Used to log debug and informational messages. */
    private static final Log LOG = LogFactory.getLog(ProteinMutation.class);

    /**
     * Create a new ProteinMutation object.
     */
    public ProteinMutation() {
        super();
    }

    public void identifyMutation(final Collection<Sentence> sentences) {
        LOG.info("Marking up mutations now.");
        for (final Sentence sentence : sentences) {
            final StringTokenizer st =
                    new StringTokenizer(sentence.getText(), " \t");
            final Set<String> potentialMutations = new HashSet<String>();

            while (st.hasMoreTokens()) {
                final String token = st.nextToken();
                identifyMutation(token, potentialMutations);
            }

            if (potentialMutations.size() != 0) {
                sentence.setMaybeProteinMutation(true);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Detected potential mutation in sentence: "
                            + sentence.getText());
                }
                final String[] strings = new String[potentialMutations.size()];
                potentialMutations.toArray(strings);
                sentence.setPotentialMutations(strings);
            }
        }
    }

    /**
     * If the token is a potential mutation, it is added to the set of
     * mutations.
     * @param token Candidate to add to the potential mutations
     * @param mutations Set of potential mutations to add.
     */
    private void identifyMutation(final String token,
            final Set<String> mutations) {
        if (token.length() >= 3 && isPotentialMutation(token)) {
            mutations.add(token);
        }
    }

    /**
     * Determines if a given string might be a mutation or not.
     * @param token The string to check
     * @return true if the string is potentially a mutation
     */
    private boolean isPotentialMutation(final String token) {
        assert token != null;
        assert token.length() > 0;

        return characterMaybeResidue(token.charAt(0))
                && characterMaybeResidue(token.charAt(token.length() - 1))
                && stringIsNumber(token, 1, token.length() - 1);
    }

    public Set<String> identifyMutation(final String indexBasename)
            throws ConfigurationException, IOException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager documentIndexManager =
            new DocumentIndexManager(indexBasename);

        final Set<String> potentialMutations = new HashSet<String>();
        final TermIterator it = documentIndexManager.getTerms();
        while (it.hasNext()) {
            final String term = it.next();
            identifyMutation(term, potentialMutations);
        }

        documentIndexManager.close();
        return potentialMutations;
    }

    private boolean characterMaybeResidue(final char c) {
        switch (c) {
        case 'A':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
        case 'G':
        case 'H':
        case 'I':
        case 'K':
        case 'L':
        case 'M':
        case 'N':
        case 'P':
        case 'Q':
        case 'R':
        case 'S':
        case 'T':
        case 'V':
        case 'W':
        case 'Y':
        case '-':
            return true;
        default:
            return false;
        }
    }

    private boolean stringIsNumber(final String s, final int start, final int end) {
        return StringUtils.isNumeric(s.substring(Math.max(start, 0), Math.max(0, end)));
    }

    public void process(final String[] args)
            throws TextractorDatabaseException, ConfigurationException,
            IOException {
        final DbManager dbm = new DbManager(args);
        dbm.beginTxn();
        final TextractorManager tm = dbm.getTextractorManager();
        boolean printText = CLI.isKeywordGiven(args, "-text", false);
        final boolean annotate = CLI.isKeywordGiven(args, "-annotate", false);
        final String filename = CLI.getOption(args, "-o", "annotations.out");
        final String basename = CLI.getOption(args, "-basename", null);
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(basename);

        boolean printId = true;
        AnnotationFormatWriter writer = null;
        // only consider sentences form these articles:
        final List<Long> articles = new ArrayList<Long>();
        articles.add(12507430L);
        articles.add(7896799L);
        articles.add(10788455L);

        if (annotate) {
            printText = false;
            printId = false;
            final FileOutputStream fileoutstream =
                new FileOutputStream(filename);
            final Writer fileWriter =
                new OutputStreamWriter(fileoutstream, "UTF-8");
            writer = new AnnotationFormatWriter(docmanager, fileWriter, true);
        }

        long lowerBound = 0;
        final String filter = "this.maybeProteinMutation==true";

        Iterator<Sentence> it = tm.getSentenceIterator(lowerBound, filter);
        int count = 0;
        final int newBatchNumber = tm.getNextAnnotationBatchNumber();
        while (it.hasNext()) {
            final Sentence sentence = it.next();
            if (!articles.contains(sentence.getArticle().getPmid())) {
                continue;
            }

            if (count > 1 & ((count % 10000) == 1)) {
                dbm.commitTxn();
                dbm.beginTxn();
                lowerBound = count;
                it = tm.getSentenceIterator(lowerBound, filter);
            }

            if (printId) {
                System.out.print(sentence.getDocumentNumber());
            }

            if (printText) {
                System.out.print(' ');
                System.out.print(removeNewLines(sentence.getText()));
            } else if (annotate) {
                // for each sentence: convert the sentence to a set of
                // annotations for the mutation attribute.
                final Collection<SingleTermAnnotation> annotations =
                    sentenceToMutationAnnotations(docmanager, tm,
                            sentence, newBatchNumber);
                for (final SingleTermAnnotation annotation : annotations) {
                    // make the annotation persistent, so that we can load back
                    // the value of the attribute:
                    dbm.makePersistent(annotation);

                    // export in the text fragment annotation format:
                    writer.writeAnnotation(annotation);
                    writer.flush();
                }
            }
            if (printText || printId) {
                System.out.print('\n');
            }
            count++;
        }

        if (writer != null) {
            writer.flush();
        }

        dbm.commitTxn();

        if (writer != null) {
            writer.flush();
        }

        docmanager.close();
    }

    private Collection<SingleTermAnnotation> sentenceToMutationAnnotations(
            final DocumentIndexManager docmanager, final TextractorManager tm,
            final Sentence sentence, final int newBatchNumber) {
        final List<SingleTermAnnotation> annotations =
            new ArrayList<SingleTermAnnotation>();
        final String[] tokens = sentence.getPotentialMutations();

        for (final String token : tokens) {
            final String text =
                    sentence.getSpaceDelimitedTerms(docmanager).toString();

            final int[] mutationOccurencesInSentence =
                    countOccurences(text, token);
            for (final int aMutationOccurencesInSentence : mutationOccurencesInSentence) {
                final SingleTermAnnotation annotation =
                        tm.createSingleTermAnnotation(newBatchNumber);
                annotation.setSentence(sentence);
                annotation.getTerm().setStartPosition(aMutationOccurencesInSentence);
                annotation.setUseSentenceText(true);
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    private int[] countOccurences(final String text, final String token) {
        final StringTokenizer st = new StringTokenizer(text, " \t");
        final int[] positions = new int[10];
        int numPositions = 0;
        int currentPosition = 0;
        while (st.hasMoreTokens()) {
            if (token.equals(st.nextToken())) {
                IntArrays.ensureCapacity(positions, numPositions + 1);
                positions[numPositions] = currentPosition;
                numPositions++;
            }
            currentPosition++;
        }

        final int[] trimmed = new int[numPositions];
        System.arraycopy(positions, 0, trimmed, 0, numPositions);
        return trimmed;
    }

    private String removeNewLines(final String text) {
        return text.replace('\n', ' ');
    }

    public static void main(final String[] args)
            throws TextractorDatabaseException, ConfigurationException,
            IOException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException, ClassNotFoundException,
            InstantiationException, URISyntaxException {
        final ProteinMutation proteinMutation = new ProteinMutation();
        proteinMutation.process(args);
    }
}
