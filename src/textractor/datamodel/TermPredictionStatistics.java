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

public final class TermPredictionStatistics {
    private Article article;
    private int[] indexedTermSearched;
    private double termPredictionRatio;
    private int termPredictionCount;

    /**
     * Distance of the term to the SVM prediction hyperplane. min is the minimum
     * distance for the term in this article. max is the maximum distance, avg
     * the average distance for the term in this article, sum is the sum of the
     * distances.
     */
    private double minDistance;
    private double maxDistance;
    private double sumDistance;

    private double[] distances;

    public TermPredictionStatistics() {
        super();
        this.distances = new double[0];
        this.termPredictionCount = 0;
        this.minDistance = 1e12; // a large value, so that minDistance will
                                 // not be 0 if the
        // min distance is greater than 0.
    }

    public TermPredictionStatistics(final Article article) {
        this();
        this.article = article;
    }

    /**
     * Returns the minimum value of this distance. Nagative values are
     * considered to determine the minimum distance.
     *
     * @return Returns the minimum value of the SVM distance to the hyperplane,
     *         for this term.
     */
    public double getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(final double currentDistance) {
        this.minDistance = Math.min(currentDistance, this.minDistance);
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(final double currentDistance) {
        this.maxDistance = Math.max(currentDistance, this.maxDistance);
    }

    public double getAvgDistance() {
        return sumDistance / (termPredictionCount);
    }

    public double getSumDistance() {
        return sumDistance;
    }

    public void setSumDistance(final double sumDistance) {
        this.sumDistance = sumDistance;
    }

    public void setTermSearched(final int[] indexedTerm) {
        this.indexedTermSearched = indexedTerm;
    }

    public int[] getTermSearched() {
        return this.indexedTermSearched;
    }

    public void setTermPredictionRatio(final double ratio) {
        this.termPredictionRatio = ratio;
    }

    public double getTermPredictionRatio() {
        return this.termPredictionRatio;
    }

    public void setTermPredictionCount(final int count) {
        this.termPredictionCount = count;
    }

    public void addToTermPredictionCount(final int count) {
        this.termPredictionCount += count;
    }

    public void setArticle(final Article article) {
        this.article = article;
    }

    public Article getArticle() {
        return this.article;
    }

    public void incrementTermPredictionCount() {
        this.termPredictionCount++;
    }

    public int getTermPredictionCount() {
        return this.termPredictionCount;
    }

    public void addDistance(final double distance) {
        final double[] distancesTemp = new double[distances.length + 1];
        System.arraycopy(distances, 0, distancesTemp, 0, distances.length);
        distancesTemp[distances.length] = distance;
        distances = distancesTemp;
    }

    public double[] getDistances() {
        return distances;
    }
}
