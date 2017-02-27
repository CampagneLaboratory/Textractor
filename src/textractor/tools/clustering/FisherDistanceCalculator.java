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

package textractor.tools.clustering;

import edu.cornell.med.icb.clustering.SimilarityDistanceCalculator;
import gominer.Fisher;
import textractor.database.DocumentIndexManager;
import textractor.query.clustering.TermSimilarityMatrix;

import java.io.IOException;

public class FisherDistanceCalculator implements SimilarityDistanceCalculator {
    private TermSimilarityMatrix simMatrix;
    private int[] termCounts;
    private String[] terms;
    private int numDocsInCorpus;
    private Fisher fisher;

    public enum DistanceType {
        AVERAGE,
        MAX,
        MIN,
        CLUSTER_AS_UNIT,
    }

    private DistanceType distanceType;

    private static boolean verbose = false;

    private static void debugMessage(final String msg) {
        if (verbose) {
            System.out.println(msg);
            System.out.flush();
        }
    }

    public static DistanceType convertStringToDistanceType(final String distTypeStr) {
        if (distTypeStr.equals("average")) {
            return DistanceType.AVERAGE;
        } else if (distTypeStr.equals("max")) {
            return DistanceType.MAX;
        } else if (distTypeStr.equals("min")) {
            return DistanceType.MIN;
        } else if (distTypeStr.equals("as-unit")) {
            return DistanceType.CLUSTER_AS_UNIT;
        } else {
            return DistanceType.AVERAGE;
        }
    }

    public static String convertDistanceTypeToString(final DistanceType distType) {
        if (distType == DistanceType.AVERAGE) {
            return "average";
        } else if (distType == DistanceType.MAX) {
            return "max";
        } else if (distType == DistanceType.MIN) {
            return "min";
        } else if (distType == DistanceType.CLUSTER_AS_UNIT) {
            return "as-unit";
        } else {
            return "average";
        }
    }

    public FisherDistanceCalculator(
            final DocumentIndexManager docmanager,
            final TermSimilarityMatrix simMatrix,
            final int[] termIndexes, final String[] terms,
            final DistanceType distanceType) throws IOException {

        // Initialize
        fisher = new Fisher();
        numDocsInCorpus = docmanager.getDocumentNumber();
        final int termCount = termIndexes.length;
        this.terms = terms;
        this.distanceType = distanceType;
        this.simMatrix = simMatrix;

        termCounts = new int[termCount];
        for (int pos = 0; pos < termCount; pos++) {
            termCounts[pos] = docmanager.frequency(termIndexes[pos]);
        }
    }

    public FisherDistanceCalculator(
            final DocumentIndexManager docmanager,
            final TermSimilarityMatrix simMatrix,
            final String[] terms,
            final DistanceType distanceType) throws IOException {
        final int[] termIndexes = new int[terms.length];
        int i=0;
        for (final String term : terms) {
            termIndexes[i++] = docmanager.findTermIndex(term);
        }
        // Initialize
        fisher = new Fisher();
        numDocsInCorpus = docmanager.getDocumentNumber();
        final int termCount = termIndexes.length;
        this.terms = terms;
        this.distanceType = distanceType;
        this.simMatrix = simMatrix;

        termCounts = new int[termCount];
        for (int pos = 0; pos < termCount; pos++) {
            termCounts[pos] = docmanager.frequency(termIndexes[pos]);
        }
    }

    /**
     * Returns the distance between an instance and the instances in a cluster.
     * The default implementation calculates maximum linkeage (max of the distances
     * between instances in the cluster and instanceIndex).
     *
     * @param cluster       Cluster array
     * @param clusterSize   Number of the cluster array that contain instances. Other elements must not be accessed.
     * @param instanceIndex Index of the instance that is compared to the cluster.
     */

    public final double distance(final int[] cluster, final int clusterSize, final int instanceIndex) {
        if (distanceType == DistanceType.AVERAGE) {
            debugMessage("Calculating average distance...");
            double sumDistance = 0;
            for (int i = 0; i < clusterSize; ++i) {
                final int anInstance = cluster[i];
                debugMessage("Getting new distance for sum " + i + " / " + clusterSize);
                sumDistance += distance(anInstance, instanceIndex);
                debugMessage("Current sum is " + sumDistance);
            }
            debugMessage("...done");
            return sumDistance / ((double) clusterSize);
        } else if (distanceType == DistanceType.MAX) {
            debugMessage("Calculating max distance...");
            double maxDistance = 0;
            for (int i = 0; i < clusterSize; ++i) {
                final int anInstance = cluster[i];
                maxDistance = Math.max(distance(anInstance, instanceIndex), maxDistance);
            }
            debugMessage("...done");
            return maxDistance;
        } else if (distanceType == DistanceType.MIN) {
            debugMessage("Calculating min distance...");
            double minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < clusterSize; ++i) {
                final int anInstance = cluster[i];
                minDistance = Math.min(distance(anInstance, instanceIndex), minDistance);
            }
            debugMessage("...done");
            return minDistance;
        } else if (distanceType == DistanceType.CLUSTER_AS_UNIT) {
            debugMessage("Calculating cluster->point distance...");
            final double distance = distanceFisherClusterToPoint(cluster, clusterSize, instanceIndex);
            debugMessage("...done");
            return distance;
        }
        return 0.0;
    }

    public double distance(final int x, final int y) {
        final String termX = terms[x];
        final String termY = terms[y];
        debugMessage("Distancing " + termX + ":" + termY);
        final int nx = termCounts[x];
        debugMessage("Getting similarily matrix for terms");
        final int nxy = (int) simMatrix.getSimilarity(termX, termY);
        final int ny = termCounts[y];
        final int N = numDocsInCorpus;
        debugMessage("Getting fisher for terms");
        showMatrix(nx, ny, nxy, N);
        final double dist = fisher.fisher(nx, nxy, nxy + N, ny);
        debugMessage("Distance " + terms[x] + ":" + terms[y] + "=" + dist);
        return dist;
    }

    public double distanceFisherClusterToPoint(final int[] c, final int clusterSize, final int y) {
        final Fisher fisher = new Fisher();
        int nx = Integer.MAX_VALUE;
        int nxy = Integer.MAX_VALUE;
        final StringBuffer theCluster = new StringBuffer();
        for (int pos = 0; pos < clusterSize; pos++) {
            nx = Math.min(termCounts[c[pos]], nx);
            // The below isn't good enough, probably
            final int sim = (int) simMatrix.getSimilarity(terms[c[pos]], terms[y]);
            debugMessage("Sim = " + sim + "  (nxy=" + nxy);
            nxy = Math.min(sim, nxy);
            if (theCluster.length() > 0) {
                theCluster.append(", ");
            }
            theCluster.append(terms[c[pos]]);
        }
        final int ny = termCounts[y];
        final int N = numDocsInCorpus;
        showMatrix(nx, ny, nxy, N);
        final double dist = fisher.fisher(nx, nxy, nxy + N, ny);
        debugMessage("Distance (" + theCluster.toString() + "):" + terms[y] + "=" + dist);
        return dist;
    }

    private static void showMatrix(final int nx, final int ny, final int nxy, final int N) {
        debugMessage("vals=" + nx + ", " + ny + ", " + nxy + ", " + N);
        debugMessage("     Yes  |  No");
        debugMessage("Yes  " + nxy + " | " + (ny - nxy));
        debugMessage("No  " + (nx - nxy) + " | " + (N - nx - nxy));
    }

    private float prob(final int x) {
        return ((float) x + 1) / ((float) numDocsInCorpus);
    }

    private float prob(final float x) {
        return (x + 1) / ((float) numDocsInCorpus);
    }

    public final double getIgnoreDistance() {
        return Integer.MIN_VALUE; // Return the minimum integer value, so that max(min, a)=a;
    }

    @Override
    public String toString() {
        final int termCount = termCounts.length;

        final StringBuffer sb = new StringBuffer();
        sb.append("MutialInformationDistanceCalculator with ");
        sb.append(numDocsInCorpus).append(" documents\n");
        sb.append("termProbs (length=").append(termCounts.length).append(")\n");
        int pos = 0;
        for (final int termProb : termCounts) {
            sb.append("   ").append(terms[pos++]).append("=").append(termProb).append("\n");
        }
        sb.append("simMatrix\n");
        for (int y = 0; y < termCount; y++) {
            for (int x = y; x < termCount; x++) {
                if (x != y) {
                    sb.append("   ").append(x).append(":").append(terms[x]);
                    sb.append(", ").append(y).append(":").append(terms[y]);
                    sb.append(" = ");
                    sb.append(simMatrix.getSimilarity(terms[x], terms[y]));
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    public static void main(final String[] args) {
        final int nx = 212832;
        final int ny = 21611;
        final int nxy = 4944;
        final int N = 16340610;
        showMatrix(nx, ny, nxy, N);
        final Fisher fisher = new Fisher();
        final double dist = fisher.fisher(nx, nxy, nxy + N, ny);
        System.out.println(dist);

    }
}
