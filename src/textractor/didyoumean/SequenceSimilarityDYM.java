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

package textractor.didyoumean;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.scoredresult.ScoredResult;
import textractor.scoredresult.ScoredResultComparator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * @author Fabien Campagne
 *         Date: Oct 26, 2006
 *         Time: 1:44:30 PM
 */
public class SequenceSimilarityDYM implements DidYouMeanI {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(SequenceSimilarityDYM.class);

    private final String allToProteinResidues = "ARNDCQEGHILKMFPSTWYVBZX";
    private ScoringMatrix matrix;
    private final DocumentIndexManager docManager;

    public List<ScoredResult> suggest(final String term, final float cutoff) throws IOException, ConfigurationException, ParseException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException {
        return new ArrayList<ScoredResult>();
    }

    public void setScoringMatrix(final ScoringMatrix matrix) {
        this.matrix = matrix;
    }

    public List<ScoredResult> suggest(final String term, final boolean orderWithVignaScore, final float cutoff) throws IOException, ParseException, QueryParserException, QueryBuilderVisitorException {
        return new ArrayList<ScoredResult>();
    }

    // now, create each combination:
    int[] combinationIndices;
    int currentIndex;

    /**
     * Construct a DYM implementation to suggest alternative to short biological sequence segments.
     *
     * @param docManager Used to filter suggestions by frequency. If you supply null, suggestions are not filtered.
     */
    public SequenceSimilarityDYM(final DocumentIndexManager docManager) {
        matrix = new IdentityScoringMatrix();
        this.docManager = docManager;
    }

    public SequenceSimilarityDYM() {
        this(null);
    }

    public List<ScoredResult> suggestRelated(final String term, final float cutoff) throws ConfigurationException, IOException, ParseException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException {
        final Object2ObjectMap<MutableString, ScoredResult> result = new Object2ObjectLinkedOpenHashMap<MutableString, ScoredResult>();
        LOG.info("In suggestRelated, query term: " + term + " cutoff: " + cutoff);
        // Build the residueChoices array. This array contains, for each position in term, an array of possible
        // toResidue choices.
        final char[][] residueChoices = new char[term.length()][];

        for (int i = 0; i < term.length(); i++) {
            residueChoices[i] = allToProteinResidues.toCharArray();
        }

        combinationIndices = new int[residueChoices.length];

        counter = 0;

        int minScore = Integer.MAX_VALUE;
        int sumOfScores = 0;
        do {

            int score = 0;
            final MutableString mutatedTerm = new MutableString();
            for (int i = 0; i < residueChoices.length; i++) {
                final char toResidue = residueChoices[i][combinationIndices[i]];
                mutatedTerm.append(toResidue);
                score += matrix.score(term.charAt(i), toResidue);
            }

            if (!mutatedTerm.equals(term)) {

                int mutatedTermIndex = 0;
                if (docManager != null &&
                        ((mutatedTermIndex = docManager.findTermIndex(mutatedTerm)) == DocumentIndexManager.NO_SUCH_TERM))
                {
                    // do not add the term if it never occurs in the corpus.
                    //      System.out.println("Term does not occur in corpus: " + mutatedTerm);
                } else {
                    final ScoredResult newScoreResult = new ScoredResult(mutatedTerm.toString(), score);
                    if (result.get(mutatedTerm) == null) {
                        sumOfScores += Math.abs(score);
                        minScore = Math.min(minScore, score);
                        result.put(mutatedTerm, newScoreResult);
                    }
                }

            } else {
                //         System.out.println("mutatedTerm: " + mutatedTerm + " ignored");
            }

        }
        while (hasNextCombination(combinationIndices, (int) Math.pow(allToProteinResidues.length(), residueChoices.length)));

        final ObjectList<ScoredResult> list = new ObjectArrayList<ScoredResult>();

        LOG.info("minScore: " + minScore + " sumOfScores: " + sumOfScores);
        for (final ScoredResult sr : result.values()) {

            final double score = sr.getScore() - minScore;
            sr.setScore((score) / (double) sumOfScores);
            if (sr.getScore() >= cutoff) {
                list.add(sr);
            }
        }
        final int maxShow = 10;
        int count = 0;

        Collections.sort(list, new ScoredResultComparator());
        final Iterator<ScoredResult> it = list.iterator();
        while (it.hasNext()) {
            final ScoredResult sr = it.next();
            if (count++ < maxShow) {
                LOG.info("suggestion: " + sr.getTerm() + " score: " + sr.getScore());
            }
        }
        LOG.info("suggest  " + list.size() + " for term: " + term);
        return list;
    }


    int counter;

    private boolean hasNextCombination(final int[] combinationIndices, final int maxIterations) {
        final String combination = Integer.toString(counter, allToProteinResidues.length());
        for (int i = 0; i < combinationIndices.length; i++) {
            combinationIndices[i] = i < combination.length() ?
                    Integer.parseInt(combination.substring(i, i + 1), allToProteinResidues.length()) :
                    0;
        }

        return counter++ < maxIterations;
    }

    public List<ScoredResult> suggestPaiceHusk(final String term, final float cutoff) throws IOException, ParseException, QueryParserException, QueryBuilderVisitorException {
        return new ArrayList<ScoredResult>();
    }
}
