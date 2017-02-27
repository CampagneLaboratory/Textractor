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

package textractor.tfidf;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.mg4j.search.score.ScoredDocumentBoundedSizeQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.docstore.DocumentStoreReader;
import textractor.mg4j.docstore.TermDocumentFrequencyReader;
import textractor.mg4j.docstore.TermSubsetTransform;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Fabien Campagne
 *         Date: Jun 22, 2006
 *         Time: 2:24:09 PM
 */
public final class TfIdfCalculator {
    private static final Log LOG = LogFactory.getLog(TfIdfCalculator.class);

    private TermDocumentFrequencyReader tfIdfReader;
    private TermSubsetTransform transform;
    private final int numberOfTerms;
    private final DocumentIndexManager docmanager;
    private int[] allTermFrequencies;
    private DocumentStoreReader docstore;
    private boolean debug;

    /**
     * These will be re-initialized and re-used
     * every time evaluates() is called.
     */
    private int currentStorageSize;
    private int[] termFrequencies;

    /**
     * Debug variables.
     */
    private int[] docFrequencies;
    private long[] sums;
    private int[] debugTermFrequencies;

    public TfIdfCalculator(final TermDocumentFrequencyReader reader,
                           final DocumentIndexManager docmanager)
            throws IOException {
        this(docmanager);
        this.tfIdfReader = reader;
        if (tfIdfReader.getTransform() instanceof TermSubsetTransform) {
            this.transform = (TermSubsetTransform) tfIdfReader.getTransform();
        }
    }

    public TfIdfCalculator(final TermDocumentFrequencyReader reader,
                           final DocumentIndexManager docmanager,
                           final boolean debug) throws IOException {
        this(reader, docmanager);
        debug(debug);
    }

    private void debug(final boolean debug) {
        if (debug) {
            docFrequencies = new int[this.numberOfTerms];
            sums = new long[this.numberOfTerms];
            debugTermFrequencies = new int[this.numberOfTerms];
            this.debug = debug;
        }
    }

    public TfIdfCalculator(final DocumentStoreReader docstore,
                           final DocumentIndexManager docmanager)
            throws IOException {
        this(docmanager);
        this.docstore = docstore;
    }

    public TfIdfCalculator(final DocumentStoreReader docstore,
                           final DocumentIndexManager docmanager,
                           final boolean debug) throws IOException {
        this(docstore, docmanager);
        debug(debug);
    }

    public TfIdfCalculator(final DocumentIndexManager docmanager)
            throws IOException {
        this.numberOfTerms = docmanager.getNumberOfTerms();
        this.docmanager = docmanager;
        cacheDocumentFrequencies();
    }

    private void cacheDocumentFrequencies() throws IOException {
        allTermFrequencies = new int[this.numberOfTerms];
        for (int termIndex = 0; termIndex < this.numberOfTerms; termIndex++) {
            allTermFrequencies[termIndex] = docmanager.frequency(termIndex);
        }
    }

    /**
     * Initialize variables before evaluate.
     *
     * @param storageSize storageSize to use.
     */
    private void initEvaluates(final int storageSize) {
        if (storageSize != currentStorageSize) {
            currentStorageSize = storageSize;
            termFrequencies = new int[storageSize];
        } else {
            for (int i = 0; i < storageSize; i++) {
                termFrequencies[i] = 0;
            }
        }
    }

    /**
     * Scans a set of documents, calculating TF-IDF for each term
     * that occurs in these documents.
     *
     * @param documents the documents to calculate tf-idf for
     * @return An array of tf-idf values (see getTermMap()).
     */
    public float[] evaluates(final int[] documents) throws IOException {
        return evaluates(documents, null);
    }

    /**
     * Scans a set of documents, calculating TF-IDF for each term
     * that occurs in these documents.
     *
     * @param documents the documents to calculate tf-idf for
     * @param tfIdfC    optional argument, when processing a tfIdfSi the
     *                  associated tfIdfC. If provided this may speed up the tfIdfSi
     *                  creation.
     * @return An array of tf-idf values (see getTermMap()).
     */
    public final float[] evaluates(final int[] documents, final float[] tfIdfC)
            throws IOException {
        return this.evaluates(documents, tfIdfC, null);
    }

    /**
     * Return the NumDocForTerm array.
     *
     * @return Return the NumDocForTerm array.
     */
    public int[] allocateNumDocForTerm() {
        final int storageSize;
        if (tfIdfReader != null) {
            storageSize = tfIdfReader.getStorageSize();
        } else {
            storageSize = numberOfTerms + 1;
        }
        return new int[storageSize];
    }

    /**
     * Scans a set of documents, calculating TF-IDF for each term
     * that occurs in these documents.
     *
     * @param documents     the documents to calculate tf-idf for
     * @param tfIdfC        optional argument, when processing a tfIdfSi the
     *                      associated tfIdfC. If provided this may speed up the tfIdfSi
     *                      creation.
     * @param numDocForTerm optional argument. This array can be allocated with
     *                      allocateNumDocForTerm(). The array has one element for each term represented
     *                      in the returned tfIdf values. The value is the number of times that a non zero
     *                      tf-Idf count was added to the term. This value therefore informs about how
     *                      many documents contain this term in documents.
     * @return An array of tf-idf values (see getTermMap()).
     */
    public float[] evaluates(final int[] documents, final float[] tfIdfC, final int[] numDocForTerm) throws IOException {
        final int storageSize;
        if (tfIdfReader != null) {
            storageSize = tfIdfReader.getStorageSize();
        } else {
            storageSize = numberOfTerms + 1;
        }
        initEvaluates(storageSize);
        // This needs to be created every time because
        // it will be returned and used outside of this class.
        final float[] tfIdf = new float[storageSize];
        long sum = 0;
        if (transform == null) {
            LOG.error("Cannot execute TfIdCalculator.evaluates(). "
                    + "tfIdrReader.transform must be TermSubsetTransform");
            throw new IOException("tfIdrReader.transform must exist and be a "
                    + "TermSubsetTransform");
        }

        // evaluates tf in this set of documents:
        for (final int doc : documents) {
            if (docstore != null) {
                sum += docstore.frequencies(doc, termFrequencies, numDocForTerm);
            }
            else {
                sum += this.tfIdfReader.read(doc, termFrequencies, numDocForTerm);
            }
        }
        final float numberOfDocuments = docmanager.getDocumentNumber();

        final int compactSize = transform.getTransformedSize();
        int termIndex;
        for (int compactIndex = 0; compactIndex < compactSize; compactIndex++) {
            termIndex = transform.getInitialTermIndex(compactIndex);
            if (termFrequencies[termIndex] != 0) {
                if ((tfIdfC != null) && (tfIdfC[termIndex] == 0)) {
                    // tfIdfC[termIndex] is zero so there is no
                    // need to calculate tfIdf[termIndex] as it will
                    // multiple to zero in the scoring.
                    continue;
                }
                // final int documentFrequency = docmanager.frequency(termIndex);
                final int documentFrequency = allTermFrequencies[termIndex];
                final float idf = (float) Math.log(numberOfDocuments / documentFrequency);
                final float tf = ((float) termFrequencies[termIndex] / (float) sum);
                tfIdf[termIndex] = tf * idf;
                if (numDocForTerm != null) {
                    // increment the count of documents that contain this term.
                    ++numDocForTerm[termIndex];
                }
                if (debug) {
                    this.sums[termIndex] = sum;
                    this.debugTermFrequencies[termIndex] = termFrequencies[termIndex];
                    this.docFrequencies[termIndex] = documentFrequency;
                }
            }
        }
        return tfIdf;
    }

    /**
     * Scans a set of documents, calculating the term selection value for each term
     * that occurs in these documents.
     * The term selection value (TSV) is calculated as defined in Robertson SE, Walker S. Okapi/Keenbow at TREC-8. 1999.
     * TSV= pow(nt/N , rt)* Combinations.choose(rt among R). With R the number of top documents examined (R=documents.length)
     *
     * @param documents     the top documents to calculate TSV for
     * @param numDocForTerm required argument. This array can be allocated with
     *                      allocateNumDocForTerm(). The array has one element for each term represented
     *                      in the returned TSV values. This value  informs about how many documents
     *                      contain this term in documents (i.e., nt part of TSV).
     * @return An array of TSV values (see getTermMap()).
     */
    public float[] termSelectionValue(final int[] documents, final int[] numDocForTerm) throws IOException {
        final double R = documents.length;
        final int storageSize;
        if (tfIdfReader != null) {
            storageSize = tfIdfReader.getStorageSize();
        } else {
            storageSize = numberOfTerms + 1;
        }
        initEvaluates(storageSize);
        // This needs to be created every time because
        // it will be returned and used outside of this class.
        final float[] tsv = new float[storageSize];
        if (transform == null) {
            LOG.error("Cannot execute TfIdCalculator.evaluates(). "
                    + "tfIdrReader.transform must be TermSubsetTransform");
            throw new IOException("tfIdrReader.transform must exist and be a "
                    + "TermSubsetTransform");
        }

        final float N = docmanager.getDocumentNumber();

        final int compactSize = transform.getTransformedSize();
        int termIndex;
        for (int compactIndex = 0; compactIndex < compactSize; compactIndex++) {
            termIndex = transform.getInitialTermIndex(compactIndex);
            if (termFrequencies[termIndex] != 0) {

                final float rt = numDocForTerm[termIndex];
                final float documentFrequencyInCorpus_nt = allTermFrequencies[termIndex];
                final double nt_N_Pow_rt = Math.pow(documentFrequencyInCorpus_nt / N, rt);
                final float combination = choose((long) rt, (long) R);

                tsv[termIndex] = (float) nt_N_Pow_rt * combination;
            }
        }
        return tsv;
    }

    public static int[] bestScoresFavorSmall(final float[] scoresInput, final int n) {
        final ScoredDocumentBoundedSizeQueue top = new ScoredDocumentBoundedSizeQueue(n);
        float maxScore = 0;
        for (final float score : scoresInput) {
            // Get the largest score so we can reverse the list, largest first
            maxScore = Math.max(score, maxScore);
        }
        for (int i = 0; i < scoresInput.length; i++) {
            if (scoresInput[i]!=0) {
                top.enqueue(i, (maxScore - scoresInput[i]));
            }
        }
        final IntArrayList result = new IntArrayList();
        while (!top.isEmpty()) {
            final DocumentScoreInfo termInfo = top.dequeue();
            final int termIndex = termInfo.document;

            result.add(termIndex);
        }
        Collections.reverse(result);
        return result.toIntArray();
    }

    /**
     * Returns the indices of the n best scores in scoresInput.
     * @param scoresInput
     * @param n
     * @return An array where each element is an index in scoresInput for a n-top ranking score.
     */
  public static int[] bestScoresFavorLarge(final float[] scoresInput, final int n) {
        final ScoredDocumentBoundedSizeQueue top = new ScoredDocumentBoundedSizeQueue(n);
        for (int i = 0; i < scoresInput.length; i++) {
            if (scoresInput[i]!=0) {
                top.enqueue(i, scoresInput[i]);
            }
        }
        final IntArrayList result = new IntArrayList();
        while (!top.isEmpty()) {
            final DocumentScoreInfo termInfo = top.dequeue();
            final int termIndex = termInfo.document;

            result.add(termIndex);
        }
        Collections.reverse(result);
        return result.toIntArray();
    }

    private long choose(final long n, final long k) {
        assert
                !(n < 0 || k < 0) : "Invalid negative parameter in Choose()";
        if (n < k) {
            return 0;
        }
        if (n == k) {
            return 1;
        }

        final long delta;
        final long iMax;

        if (k < n - k) // ex: Choose(100,3)
        {
            delta = n - k;
            iMax = k;
        } else         // ex: Choose(100,97)
        {
            delta = k;
            iMax = n - k;
        }

        long ans = delta + 1;

        for (long i = 2; i <= iMax; ++i) {
            ans = (ans * (delta + i)) / i;
        }

        return ans;
    }

    /**
     * Retuns the tf-idf score based on two tf-idf vectors.
     * This is done by doing a vector multiplication on the
     * two vectors and them summing the result.
     *
     * @param tfIdfSi tf-idf vector of a single document
     * @param tfIdfC  tf-idf vector of the set of context documents
     * @return float the score
     */
    public float calculateTfIdfScore(final float[] tfIdfSi, final float[] tfIdfC)
            throws IOException {

        assert((tfIdfSi != null) && (tfIdfC != null))
                : "tfIdfSi and tfIdfC must not be null";
        assert(tfIdfSi.length == tfIdfC.length)
                : "tfIdfSi.length and tfIdfC.length must be equal";

        if (transform == null) {
            LOG.error("Cannot execute TfIdCalculator.calculateTfIdScore(). "
                    + "tfIdrReader.transform must be TermSubsetTransform");
            throw new IOException("tfIdrReader.transform must exist and be a "
                    + "TermSubsetTransform");
        }

        float sum = 0;
        float si;
        int termIndex;
        final int compactSize = transform.getTransformedSize();
        for (int compactIndex = 0; compactIndex < compactSize; compactIndex++) {
            termIndex = transform.getInitialTermIndex(compactIndex);
            si = tfIdfSi[termIndex];
            // si will never have a value unless c has a value
            // so we only need to check if si has a value
            if (si != 0) {
                sum += (tfIdfC[termIndex] * si);
            }
        }
        return sum;
    }

    /**
     * This perfoms a quick sum of the vector elements.
     * I wrote this so I could compare two implementations
     * of creating the tf-idf vector.
     *
     * @param tfIdf
     * @return Sum of the elements.
     */
    public float sum(final float[] tfIdf) {
        float sum = 0;
        int termIndex;
        final int compactSize = transform.getTransformedSize();
        for (int compactIndex = 0; compactIndex < compactSize; compactIndex++) {
            termIndex = transform.getInitialTermIndex(compactIndex);
            sum += tfIdf[termIndex];
        }
        return sum;
    }

    public int[] getDocFrequencies() {
        return docFrequencies;
    }

    public long[] getSums() {
        return sums;
    }

    public int[] getTermFrequencies() {
        return debugTermFrequencies;
    }

    /**
     * Returns a map from term index to term. The index of the float in the result from the evaluates
     * method indicates which term the TF-IDF value is associated with.  For instance, if evaluates(doc)[12]=0.4
     * getTermMap().getTerm(12) indicates which term has TF-IDF 0.4.
     *
     * @return a map from term index to term.
     */
    public String getTerm(final int index) {
        return docmanager.termAsString(index);
    }
}
