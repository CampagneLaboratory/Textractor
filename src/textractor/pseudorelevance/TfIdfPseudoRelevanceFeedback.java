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

package textractor.pseudorelevance;

import textractor.tfidf.TfIdfCalculator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabien Campagne
 *         Date: Nov 30, 2006
 *         Time: 3:39:12 PM
 */
public class TfIdfPseudoRelevanceFeedback implements PseudoRelevanceFeedback {
    private final TfIdfCalculator calculator;

    public TfIdfPseudoRelevanceFeedback(final TfIdfCalculator calculator) {
        this.calculator = calculator;
    }

    public List<QueryTerm> expand(final int[] documents, final QueryTerm[] initialQueryTerms, final int maxAdditionalQueryTerms) throws IOException {
        final int[] numDocMatchForTerm = calculator.allocateNumDocForTerm();
        final float[] tfIdfC = calculator.evaluates(documents, null, numDocMatchForTerm);
        // System.out.println("Getting best scores for " + maxAdditionalQueryTerms);
        final int[] bestIndices = TfIdfCalculator.bestScoresFavorLarge(tfIdfC, maxAdditionalQueryTerms + initialQueryTerms.length);

        final List<QueryTerm> result = new ArrayList<QueryTerm>();
        // System.out.println("Initial terms: " + listTerms(initialQueryTerms));
        for (final int bestIndex : bestIndices) {
            final String term = calculator.getTerm(bestIndex);
            final float score = tfIdfC[bestIndex];
            final int rt=numDocMatchForTerm[bestIndex];
            final int R=documents.length;
            if (notInitialQueryTerm(term, initialQueryTerms)) {
                result.add(new QueryTerm(term, bestIndex, score, rt, R));
            }
            if (result.size() == maxAdditionalQueryTerms) {
                break;
            }
        }
        // System.out.println("-- Returning new words " + result.size());
        return result;
    }

    /**
     * Return back the tf-idf terms that are in at 10% of the documents (to a minimum
     * of 1 fo the documents).
     * @param documents the document numbers to check
     * @param initialQueryTerms initial query terms
     * @param maxAdditionalQueryTerms max addition terms to add
     * @return the list of terms
     * @throws IOException problem reading the tf-idf
     */
    public List<QueryTerm> expandConsensus(final int[] documents,
            final QueryTerm[] initialQueryTerms, final int maxAdditionalQueryTerms) throws IOException {
        final int desiredConsensusDocCount = Math.max(1, documents.length / 10);
        return expandConsensus(documents, initialQueryTerms, maxAdditionalQueryTerms,
                desiredConsensusDocCount);
    }

    /**
     * Same as expand, but rejects terms that occur in less than a
     * documentCountThreshold among the top hits. This version will allow
     * you to specify the minimum number of documents that the terms must
     * be part of.
     * @param documents the document numbers to check
     * @param initialQueryTerms initial query terms
     * @param maxAdditionalQueryTerms max addition terms to add
     * @param desiredConsensusDocCount the desired min # of documents
     * the terms must be in
     * @return the list of terms
     * @throws IOException problem reading the tf-idf
     */
    public List<QueryTerm> expandConsensus(final int[] documents,
            final QueryTerm[] initialQueryTerms, final int maxAdditionalQueryTerms,
            final int desiredConsensusDocCount) throws IOException {
        // if only one document, use regular expand.
        if (documents.length == 1) {
            return expand(documents, initialQueryTerms, maxAdditionalQueryTerms);
        }

        final int[] numDocMatchForTerm = calculator.allocateNumDocForTerm();
        final float[] tfIdfC = calculator.evaluates(documents, null, numDocMatchForTerm);

        final int[] bestIndices = TfIdfCalculator.bestScoresFavorLarge(tfIdfC, 100 * (maxAdditionalQueryTerms + initialQueryTerms.length));

        // filter out terms that occur in only one document:
        // document threshold is 1 or a tenth of the number of top documents used for feedback, whatever is larger.
        final int documentCountThreshold = Math.min(documents.length, desiredConsensusDocCount);

        final List<QueryTerm> result = new ArrayList<QueryTerm>();
        for (final int bestIndex : bestIndices) {
            if (numDocMatchForTerm[bestIndex] <= documentCountThreshold) {
                continue;
            }
            final float score = tfIdfC[bestIndex];
            final String term = calculator.getTerm(bestIndex);
            final int rt = numDocMatchForTerm[bestIndex];
            final int R = documents.length;
            if (notInitialQueryTerm(term, initialQueryTerms)) {
                final QueryTerm queryTerm = new QueryTerm(term, bestIndex, score, rt, R);
                queryTerm.setNumConsensusDocs(numDocMatchForTerm[bestIndex] - 1);
                result.add(queryTerm);
            }

            if (result.size() == maxAdditionalQueryTerms) {
                break;
            }
        }
        // System.out.println("-- Returning new words " + result.size());
        return result;
    }

    private String listTerms(final QueryTerm[] terms) {
        final StringBuffer out = new StringBuffer();
        for (final QueryTerm term : terms) {
            if (out.length() > 0) {
                out.append(",");
            }
            out.append(term.getTerm());
        }
        return out.toString();
    }

    private boolean notInitialQueryTerm(final String term, final QueryTerm[] initalQueryTerms) {
        for (final QueryTerm qt : initalQueryTerms) {
            if (term.equals(qt.getTerm())) {
                return false;
            }
        }
        return true;
    }
}
