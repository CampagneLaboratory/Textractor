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

/**
 * Class to calculate the similarity between two strings, based on the
 * Levenshtein distance.
 *
 * Levenshtein, A. (1966). Binary Codes Capable of Correcting Deletions,
 * Insertions, and Reversals. Soviet Physics - Doklady 10, 703-710. (English
 * translation)
 *
 * @see <a href="http://en.wikipedia.org/wiki/Levenshtein">Levenshtein distance</a>
 */
public final class SimilarityScorer {
    private static final int MAX_STRING_LENGTH = 100;
    private final float[][] distanceMatrix;

    public SimilarityScorer() {
        super();
        distanceMatrix = new float[MAX_STRING_LENGTH][MAX_STRING_LENGTH];

        for (int i = 0; i < MAX_STRING_LENGTH; i++) {
            distanceMatrix[i] = new float[MAX_STRING_LENGTH];
        }
    }

    float minimum(final float value1, final float value2, final float value3) {
        return Math.min(Math.min(value1, value2), value3);
    }

    float getEditDistance(final String string1, final String string2) {
        return getEditDistance(string1.toCharArray(), string2.toCharArray());
    }

    float getEditDistance(final char[] string1, final char[] string2) {
        // Get sizes of string for comparison
        final int string1Length = string1.length;
        final int string2Length = string2.length;

        // Initialize edit distance matrix
        for (int i = 0; i <= string1Length; i++) {
            distanceMatrix[i][0] = i;
        }

        for (int j = 0; j < string2Length + 1; j++) {
            distanceMatrix[0][j] = j;
        }

        // Calculate distance scores for deletions, insertions and substitutions
        // and populate distance matrix

        for (int i = 1; i <= string1Length; i++) {
            for (int j = 1; j <= string2Length; j++) {
                final float cost = calculateScore(i - 1, j - 1, string1, string2);
                final float deletionScore = distanceMatrix[i - 1][j] + 1;
                final float insertionScore = distanceMatrix[i][j - 1] + 1;
                final float substitutionScore = distanceMatrix[i - 1][j - 1] + cost;

                distanceMatrix[i][j] = minimum(deletionScore, insertionScore, substitutionScore);
            }
        }

        return distanceMatrix[string1Length][string2Length];
    }

    private float calculateScore(final int position1, final int position2,
            final char[] string1, final char[] string2) {
        if (string1[position1] == string2[position2]) {
            return 0;
        } else {
            return 1;
        }
    }

    public float getSimilarity(final String string1, final String string2) {
        final float editDistance = getEditDistance(string1, string2);
        final float maxLength = Math.max(string1.length(), string2.length());

        if (maxLength == 0.0F) {
            return 1.0F;
        } else {
            return 1.0F - editDistance / maxLength;
        }
    }
}
