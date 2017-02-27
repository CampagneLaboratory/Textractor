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

package textractor.datamodel;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;

/**
 * Holds parameters to calculate features with the bag of word approach.
 * In the bag of word approach, a window is centered around a word of interest
 * (the position of this word is encoded in the
 * TextFragmentAnnotation#getWordPosition). The window has a size given by the
 * windowSize in this class. A set of words/terms are used in turn to perform
 * boolean tests on the text within the window around the term. The results of
 * these tests provides the features (features are 0 or 1).
 * As an example, consider the following window, centered around term A23P:
 * The mutant A23P exhibits a strong phenotype.
 * If the window size is 2 and the terms considered for calculating the bag of
 * words are "the", "mutant", and "strong" (in this order), then the features
 * calculated for the text within the window in the example will be:
 * 1 1 0
 * The first 1 means that the word "the" occurs in the window around the word
 * of interest. the second 1 that the term "mutant" occurs. The last feature
 * is 0 because the word "strong" does not occur within the window of size 2
 * centered on the word of interest.
 * Note that the order of the terms is significant, because the features must
 * be calculated in exactly the same order when exporting the training set
 * and to perform predictions on the test set. If the order is not maintained,
 * the behavior of prediction engines (such as SVMs) is undefined.
 *
 * User: Fabien Campagne
 * Date: Jan 19, 2004
 * Time: 11:42:11 AM
 *
 */
public final class SingleBagOfWordFeatureCreationParameters
    extends FeatureCreationParameters {

    /** Used to log debug and informational messages. */
    private static final Log LOG =
            LogFactory.getLog(SingleBagOfWordFeatureCreationParameters.class);

    /**
     * The window is at the right of the reference word.
     * For instance a b >REF-WORD c d < is a window of size 3 at the right
     * of the reference word.
     */
    public static final int LOCATION_RIGHT_OF_WORD = 1;

    /**
     * The window is at the left of the reference word.
     * For instance >a b REF-WORD< is a window of size 3 at the left of the
     * reference word.
     */
    public static final int LOCATION_LEFT_OF_WORD = 2;

    /**
     * The window is centered on the reference word.
     * For instance >a b REF-WORD c d< e is a window of size 3 centered on
     * the reference word.
     */
    public static final int LOCATION_CENTERED_ON_WORD = 3;

    private int firstFeatureNumber;

    private String[] terms;
    private int windowLocation;
    private transient IntArrayList termsInWindows;

    /**
     * Constructs default parameters. By default, the window is centered on the
     * word of interest and has size 1.
     */
    public SingleBagOfWordFeatureCreationParameters() {
        super();
        this.termsInWindows = new IntArrayList();
        this.windowLocation = LOCATION_CENTERED_ON_WORD;
        this.windowSize = 1;
    }

    public int getFirstFeatureNumber() {
        return firstFeatureNumber;
    }

    /**
     * Sets the number of the first feature that this exporter will generate.
     * When this number is zero, the exporter assumes that it should export the
     * label before the first feature.
     *
     * @param firstFeatureNumber
     */
    public void setFirstFeatureNumber(final int firstFeatureNumber) {
        this.firstFeatureNumber = firstFeatureNumber;
    }

    public int getLastFeatureNumber() {
        return firstFeatureNumber + terms.length - 1;
    }

    /**
     * Window location with respect to the reference word.
     *
     * @return One of LOCATION_RIGHT_OF_WORD, LOCATION_LEFT_OF_WORD, or
     * LOCATION_CENTERED_ON_WORD.
     */
    public int getWindowLocation() {
        return windowLocation;
    }

    public void setWindowLocation(final int windowLocation) {
        this.windowLocation = windowLocation;
    }

    /**
     * Returns the terms for which features should be calculated.
     * The terms are arranged in the array in the order in which they should
     * be used to calculate features.
     * @return An array of string. Each string is a term/word.
     */
    @Override
    public String[] getTerms() {
        return terms;
    }

    @Override
    public void updateIndex(final DocumentIndexManager docmanager) {
        termsInWindows = new IntArrayList();
        int noIndexCount = 0;
        for (final String term : terms) {
            final int termIndex = docmanager.findTermIndex(term);
            if (termIndex == DocumentIndexManager.NO_SUCH_TERM) {
                noIndexCount++;
            }
            termsInWindows.add(termIndex);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated termsInWindows size: " + termsInWindows.size());
            LOG.debug("terms not in current index: " + noIndexCount);
        }
    }

    @Override
    public void clearTerms() {
        terms = new String[0];
        termsInWindows = new IntArrayList();
    }

    @Override
    public void removeTerm(final String term, final int indexedTerm) {
        if (termsInWindows.contains(indexedTerm)) {
            final int index = termsInWindows.indexOf(indexedTerm);
            termsInWindows.remove(index);
        }
    }

    /**
     * Convert indexed terms to their string representation. The side effect
     * is that getTerms() returns accurate terms.
     * @param docmanager the index manager
     */
    @Override
    public void setTerms(final DocumentIndexManager docmanager) {
        terms = new String[termsInWindows.size()];
        for (int i = 0; i < terms.length; i++) {
            terms[i] = docmanager.termAsString(termsInWindows.getInt(i));
        }
    }

    public void setTermsInWindows(final IntArrayList termsInWindows) {
        this.termsInWindows = termsInWindows;
    }

    public IntArrayList getTermsInWindows() {
        return this.termsInWindows;
    }

    @Override
    public void setWindowSize(final int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * To find out what terms are in the window and add them to
     * the non-redundant termsInWindows.
     * a b c d e f g h i j
     * wp=5
     * ws=2
     * window contains: d e f g h
     * @param indexedTerms
     * @param wordOfInterestPosition
     * @param termLength
     * @param excludedPositions
     */
    public void addWordsToTerms(final int[] indexedTerms,
            final int wordOfInterestPosition, final int termLength,
            final int[] excludedPositions) {
        final int minIndex = calculateMinWindowIndex(wordOfInterestPosition);
        final int maxIndex = calculateMaxWindowWidth(wordOfInterestPosition,
                termLength, indexedTerms.length);
        for (int i = minIndex; i <= maxIndex; ++i) {
            if (!positionIsExcluded(excludedPositions, i)) { // ignore excluded positions.
                if (!termsInWindows.contains(indexedTerms[i])) {
                    termsInWindows.add(indexedTerms[i]);
                }
            }
        }
    }

    public int calculateMinWindowIndex(final int wordOfInterestPosition) {
        switch (windowLocation) {
            case LOCATION_CENTERED_ON_WORD:
            case LOCATION_LEFT_OF_WORD:
                return Math.max(0, wordOfInterestPosition - windowSize);
            case LOCATION_RIGHT_OF_WORD:
                return wordOfInterestPosition;
            default:
                throw new InternalError("windowLocation not supported: "
                        + windowLocation);
        }
    }

    public int calculateMaxWindowWidth(final int wordOfInterestPosition,
            final int termLength, final int numTerms) {
        switch (windowLocation) {
            case LOCATION_RIGHT_OF_WORD:
            case LOCATION_CENTERED_ON_WORD:
                return Math.min(wordOfInterestPosition + termLength - 1 + windowSize, numTerms-1);   // fixes an ArrayIndexOutOfBoundsException
            case LOCATION_LEFT_OF_WORD:
                return Math.min(wordOfInterestPosition, numTerms - 1);                 // truncate window at maximum number of terms.
            default:
                throw new InternalError("windowLocation not supported: "
                        + windowLocation);
        }
    }

    public boolean windowContainsTerm(final int[] annotationIndexedTerms,
            final int term, final int windowCenter, final int termLength,
            final int minIndex, final int maxIndex) {
        for (int i = minIndex; i < windowCenter && i < annotationIndexedTerms.length; ++i) {
            if (annotationIndexedTerms[i] == term) {
                return true;
            }
        }

        // ignore the center of the window.
        for (int i = windowCenter + termLength; i <= maxIndex; ++i) {
            if (annotationIndexedTerms[i] == term) {
                return true;
            }
        }

        return false;
    }
}
