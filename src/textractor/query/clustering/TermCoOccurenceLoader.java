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

package textractor.query.clustering;

import edu.cornell.med.icb.util.ICBStringNormalizer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fill the result matrix with term co-occurence document counts.
 *
 * @author Kevin C. Dorff Date: Nov 21, 2006 Time: 12:09:40 PM
 */
public class TermCoOccurenceLoader {

    /**
     * Used to log meesages with logger.
     */
    private static final Log log = LogFactory.getLog(TermCoOccurenceLoader.class);

    private final DocumentIndexManager docmanager;

    private final TermCoOccurenceCalculator tcoc;

    public TermCoOccurenceLoader(final DocumentIndexManager docmanager) {
        this.docmanager = docmanager;
        tcoc = new TermCoOccurenceCalculator(docmanager);
    }

    private static final boolean verbose = false;

    private static void debugMessage(final String msg) {
        if (verbose) {
            System.out.println(msg);
            System.out.flush();
        }
    }

    /**
     * Obtain a TermCoOccurance matrix. If one already exists,
     * read it from disc. If you have to create one, write it
     * to disc after you create it.
     * TODO: If there is a matrix which is a SUPERSET of this
     * matrix, that would work fine, too.
     * @param inTerms the terms to use.
     * @return TermSimilarityMatrix the TermCoOccurance matrix
     * for the specified terms.
     */
    public TermSimilarityMatrix obtainTermCoOccurenceMatrix(
            final String[] inTerms) {

        // Sort the list of terms
        final List<String> termsList = new ArrayList<String>(inTerms.length);
        for (final String paddedTerm : inTerms) {
            final String term = paddedTerm.trim();
            if (term.length() > 0) {
                // Not a blank term
                if (!termsList.contains(term)) {
                    // Term not in the list already
                    termsList.add(term);
                }
            }
        }

        // Since it is a CO-occurence matrix that we are going
        // to create, we need at least TWO terms. Otherwise
        // just return an empty matrix.
        if (termsList.size() < 2) {
            // No terms
            final int[] emptyi = new int[0];
            final String[] emptys = new String[0];
            return new TermSimilarityMatrix(emptyi, emptys);
        }
        Collections.sort(termsList);
        final String[] terms = termsList.toArray(new String[termsList.size()]);

        final File serFile = bestMatchFile(docmanager.getBasename() +
                "-TermCo-", terms);
        if (!serFile.exists()) {
            final TermSimilarityMatrix simMatrix = makeMatrix(terms);
            dehydrateMatrix(serFile, simMatrix);
            return simMatrix;
        } else {
            return hydrateMatrix(serFile, terms);
        }
    }

    /**
     * Make the TermSimilarityMatrix from scratch. This should also "normalize"
     * (fix) the matrix for consumption by the clusterer.
     * @param terms
     * @return
     */
    private TermSimilarityMatrix makeMatrix(final String[] terms) {
        debugMessage("Making the matrix");

        final TermSimilarityMatrix simMatrix;
        // Create the matrix

        simMatrix = tcoc.calculate( terms);
        // Fix the matrix
        fixMatrix(simMatrix, terms);
        System.out.println("Created new matrix");
        return simMatrix;
    }

    private TermSimilarityMatrix hydrateMatrix(final File serFile, final String[] terms) {
        debugMessage("De-Serializing matrix");

        // Read the matrix from the disc
        TermSimilarityMatrix simMatrix = null;

        FileInputStream fis = null;
        ObjectInputStream in = null;
        try {
            debugMessage("Reading file");
            fis = new FileInputStream(serFile);
            in = new ObjectInputStream(fis);
            simMatrix = (TermSimilarityMatrix)in.readObject();
            in.close();
            debugMessage("...done");
        } catch(IOException e) {
            log.error("Error de-serializing simMatrix", e);
        } catch(ClassNotFoundException e) {
            log.error("Error de-serializing simMatrix", e);
        }

        // Make sure the terms match
        if (simMatrix != null) {
            if (!termsCompare(simMatrix, terms)) {
                debugMessage("Terms didn't compare");
                simMatrix = null;
            }
        }

        // Make a new one if we had trouble reading it
        // or the terms don't match
        // -- then serialize it
        if (simMatrix == null) {
            debugMessage("Problem de-serializing, re-making matrix");
            simMatrix = makeMatrix(terms);
            dehydrateMatrix(serFile, simMatrix);
        } else {
            System.out.println("Using pre-existing matrix");
        }

        return simMatrix;
    }

    private void dehydrateMatrix(final File serFile,
            final TermSimilarityMatrix simMatrix) {
        debugMessage("Serializing matrix");

        // Remove any previous version
        if (serFile.exists()) {
            debugMessage("Deleting previous");
            serFile.delete();
        }
        // Serialize the matrix to disc
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            debugMessage("Writing file");
            fos = new FileOutputStream(serFile);
            out = new ObjectOutputStream(fos);
            out.writeObject(simMatrix);
            out.close();
            debugMessage("...done");
        } catch (IOException e) {
            log.error("Error serializing simMatrix", e);
            serFile.delete();
        }
    }

    /**
     * Check the termsList against the matrix and make sure
     * the terms list is comparable.
     * @param simMatrix
     * @param terms
     * @return
     */
    private boolean termsCompare(final TermSimilarityMatrix simMatrix,
            final String[] terms) {

        debugMessage("Comparing terms from existing matrix");
        final String[] matrixTerms = simMatrix.getTermsArray();

        // Make sure ever term in termsList is in matrixTerms
        boolean found;
        for (final String term : terms) {
            found = false;
            for (final String matrixTerm : matrixTerms) {
                if (term.equals(matrixTerm)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Find the best match file for term co-occurences.
     * The exact match is preferable. If that doesn't exist
     * look for a super-set match. It is assumed that the
     * terms are ALREADY SORTED.
     * @param prefix filename prefix, ends in ".ser".
     * @param terms the terms required.
     * @return the filename of the best match file.
     */
    public static File bestMatchFile(final String prefix,
            final String[] terms) {
        final File bestMatch = new File(makeFilename(prefix, terms, false));
        if (bestMatch.exists()) {
            return bestMatch;
        }

        // Try to find a superset...
        final int dirPos = Math.max(prefix.lastIndexOf("\\"),prefix.lastIndexOf("/"));
        final String dirName;
        String subPrefix = prefix;
        if (dirPos == -1) {
            dirName = ".";
        } else {
            dirName = prefix.substring(0, dirPos);
            subPrefix = prefix.substring(dirPos + 1, prefix.length());
        }
        final String[] allFiles = new File(dirName).list();
        final String wildCardFiles = makeFilename(subPrefix, terms, true);
        for (final String file : allFiles) {
            if (FilenameUtils.wildcardMatch(file, wildCardFiles)) {
                debugMessage("Found a superset: " + dirName + "/" + file);
                return new File(dirName + "/" + file);
            }
        }

        // No super-set files were found. We'll have to make one
        // using the bestMatch filename.
        return bestMatch;
    }

    /**
     * Take a list of terms and make a filename from them for reading/writing
     * serialization of a TermSimilarityMatrix.
     * @param prefix filename prefix
     * @param terms the list of terms to make a filename for
     * @return
     */
    public static String makeFilename(final String prefix,
            final String[] terms, final boolean wildcards) {
        final StringBuffer fname = new StringBuffer(prefix);
        for (final String term : terms) {
            if (wildcards) {
                fname.append("*");
            }
            fname.append("-").append(filenameSafeString(term)).append("-");
        }
        if (wildcards) {
            fname.append("*");
        }
        fname.append(".ser");
        debugMessage("Safe filename = " + fname.toString());
        return fname.toString();
    }

    /**
     * Remove any accents, replace characters that are hazardous to filenames
     * with "_".
     * @param word the word to make "filename-safe".
     * @return the "filename-safe" version of the word.
     */
    private static String filenameSafeString(final String word) {
        return ICBStringNormalizer.removeAccents(word).replaceAll("[/\\\\:*?\"<>|%#;@&=~]", "_");
    }

    /**
     * Convert have of the matrix from number of matching documents to number of
     * matching documents dividied by the total number of documents.
     * @param terms
     */
    private void fixMatrix(final TermSimilarityMatrix simMatrix,
            final String[] terms) {
        debugMessage("Fixing the matrix");
        final int termCount = terms.length;
        for (int y = 0; y < termCount; y++) {
            final String termY = terms[y];
            for (int x = y; x < termCount; x++) {
                if (x != y) {
                    final String termX = terms[x];
                    final float val = simMatrix.getSimilarity(termX, termY);

                    simMatrix.setSimilarity(termX, termY, val);
                    simMatrix.setSimilarity(termY, termX, val);
                }
            }
        }
    }

}
