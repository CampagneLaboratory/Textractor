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
public class TermSelectionPseudoRelevanceFeedback implements PseudoRelevanceFeedback {
    private final TfIdfCalculator calculator;

    public TermSelectionPseudoRelevanceFeedback(final TfIdfCalculator calculator) {
        this.calculator = calculator;
    }

    private float significanceThreshold = 1E-4f;

    public void setSignificanceThreshold(final float significanceThreshold) {
        this.significanceThreshold = significanceThreshold;
    }

    public List<QueryTerm> expand(final int[] documents, final QueryTerm[] initalQueryTerms, final int maxAdditionalQueryTerms) throws IOException {

        final int[] numDocForTerm = calculator.allocateNumDocForTerm();
        final float[] tsv = calculator.termSelectionValue(documents, numDocForTerm);
        System.out.println("Getting best scores for " + maxAdditionalQueryTerms);
        final int[] bestIndices = TfIdfCalculator.bestScoresFavorSmall(tsv, maxAdditionalQueryTerms + initalQueryTerms.length);
        final List<QueryTerm> result = new ArrayList<QueryTerm>();
        System.out.println("Initial terms: " + listTerms(initalQueryTerms));
        for (final int bestIndex : bestIndices) {
            final String term = calculator.getTerm(bestIndex);
            final float score = tsv[bestIndex];
            final int rt = numDocForTerm[bestIndex];
            final int R = documents.length;
            if (score <= significanceThreshold && notInitialQueryTerm(term, initalQueryTerms)) {
                result.add(new QueryTerm(term, score, rt, R));
            }
            if (result.size() == maxAdditionalQueryTerms) {
                break;
            }
        }
        System.out.println("-- Returning new words " + result.size());
        return result;
    }

    public List<QueryTerm> expandConsensus(final int[] documents, final QueryTerm[] initalQueryTerms, final int maxAdditionalQueryTerms) throws IOException {
        // if only one document, use regular expand.
        if (documents.length == 1) {
            return expand(documents, initalQueryTerms, maxAdditionalQueryTerms);
        }

        final int[] numDocForTerm = calculator.allocateNumDocForTerm();
        final float[] tsv = calculator.termSelectionValue(documents, numDocForTerm);
        System.out.println("Getting best scores for " + maxAdditionalQueryTerms);
        int[] bestIndices = TfIdfCalculator.bestScoresFavorSmall(tsv, 2 * (maxAdditionalQueryTerms + initalQueryTerms.length));
        // filter out terms that occur in only one document:
        // document threshold is 1 or a tenth of the number of top documents used for feedback, whatever is larger.
        final int documentCountThreshold = Math.max(1, documents.length / 10);

        for (final int bestIndex : bestIndices) {
            if (numDocForTerm[bestIndex] <= documentCountThreshold) {
                System.out.println("term at index " + bestIndex + "occured in only " + numDocForTerm[bestIndex] +
                        " documents. Removed.");
                tsv[bestIndex] = 1;
            }
        }
        bestIndices = TfIdfCalculator.bestScoresFavorSmall(tsv, maxAdditionalQueryTerms + initalQueryTerms.length);
        final List<QueryTerm> result = new ArrayList<QueryTerm>();
        System.out.println("Initial terms: " + listTerms(initalQueryTerms));
        for (final int bestIndex : bestIndices) {
            final String term = calculator.getTerm(bestIndex);
            final float score = tsv[bestIndex];
            final int rt = numDocForTerm[bestIndex];
            final int R = documents.length;
            if (score <= significanceThreshold && notInitialQueryTerm(term, initalQueryTerms)) {

                result.add(new QueryTerm(term, score, rt, R));
            }
            if (result.size() == maxAdditionalQueryTerms) {
                break;
            }
        }
        System.out.println("-- Returning new words " + result.size());
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
