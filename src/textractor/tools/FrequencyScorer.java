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

package textractor.tools;

import textractor.database.DocumentIndexManager;
import textractor.mg4j.TermFrequency;
import textractor.mg4j.docstore.DocumentStoreReader;

import java.io.IOException;

/**
 * FrequencyScorer objects provide a weighted score based on a term's frequency
 * in a corpus. Raw "real" frequencies can be determined by setting the
 * FrequencyCalculators to RawFrequencyCalculators. TF.IDF weights can be
 * computed using LogarithmicDocumentCalculator and LogarithmicTermCalculator
 * for the document and term FrequencyCalculators respectively. The TF.IDF
 * weight can be accessed using getWeight().
 */
public final class FrequencyScorer {
    private DocumentIndexManager docmanager;
    private DocumentStoreReader reader;
    private TermFrequency frequency;
    private FrequencyCalculator documentFrequencyCalculator;
    private FrequencyCalculator termFrequencyCalculator;
    private String term;

    public FrequencyScorer() {
        super();
    }

    /***
     * Retrieves the docmanager of the index.
     * @return DocumentIndexManager - the index document index manager
     */
    public DocumentIndexManager getDocmanager() {
        return docmanager;
    }

    /**
     * Sets the docmanager used to manage the index.
     * @param docmanager a DocumentIndexManager managing the index.
     */
    public void setDocmanager(final DocumentIndexManager docmanager) {
        this.docmanager = docmanager;
    }

    /**
     * Retrieves the DocumentStoreReader of the docstore of the index.
     * @return DocumentStoreReader the docstore reader
     */

    public DocumentStoreReader getReader() {
        return reader;
    }

    /**
     * Sets the DocumentStoreReader used to read the docstore at basename.
     * @param reader a DocumentStoreReader for the docstore
     */

    public void setReader(final DocumentStoreReader reader) {
        this.reader = reader;
    }

    /**
     * Retrieves the TermFrequency object enclosing the term and document
     * frequency values.
     *
     * @return TermFrequency for the term
     */

    public TermFrequency getFrequency() {
        return frequency;
    }

    /**
     * Sets the term and computes the frequency from the DocumentIndexManager.
     * @param term to calculate frequency score for
     */

    public void setTerm(final String term) throws IOException {
        this.term = term;
        if (frequency == null) {
            frequency = new TermFrequency();
        }
        docmanager.frequency(new String[]{getTerm()}, frequency);
    }

    /**
     * Retrieves the term.
     * @return term
     */
    public String getTerm() {
        return term;
    }

    /**
     * Computes and returns the adjusted document frequency, based on the
     * weighting specified by the document FrequencyCalculator.
     *
     * @return double value of the weighted frequency
     */

    public double getDocumentFrequency() {
        getDocumentFrequencyCalculator().setFrequencyCount(frequency.getDocumentFrequency());
        return getDocumentFrequencyCalculator().getFrequency();
    }

    /**
     * Computes and retuns the adjusted term frequency, based on the weighting
     * specified by the term FrequencyCalculator.
     * @return double value fo the weighted frequency
     */
    public double getTermFrequency() {
        getTermFrequencyCalculator().setFrequencyCount(frequency.getOccurenceFrequency());
        return getTermFrequencyCalculator().getFrequency();
    }

    /**
     * Computes and returns the sum of the weighted term and document frequency
     * scores If LogarithmiocTermCalculator and LogarithmicDocumentCalculator
     * are used, this weight is the TD.IDF value.
     *
     * @return double value
     */
    public double getWeight() {
        if (frequency.getOccurenceFrequency() == 0) {
            return 0;
        } else {
            return getTermFrequency() * getDocumentFrequency();
        }
    }

    /**
     * Getter for the document FrequencyCalculator.
     *
     * @return FrequencyCalculator used in determining the document weighted
     *         score
     */
    public FrequencyCalculator getDocumentFrequencyCalculator() {
        return documentFrequencyCalculator;
    }

    /**
     * Sets the FrequencyCalculator used in the document weighted score
     * calculation.
     *
     * @param documentFrequencyCalculator
     */
    public void setDocumentFrequencyCalculator(final FrequencyCalculator documentFrequencyCalculator) {
        this.documentFrequencyCalculator = documentFrequencyCalculator;
    }

    /**
     * Getter for the term FrequencyCalculator.
     * @return FrequencyCalculator used in determining the term weighted score
     */
    public FrequencyCalculator getTermFrequencyCalculator() {
        return termFrequencyCalculator;
    }

    /**
     * Sets the FrequencyCalculator used in the term weighted score calculation.
     * @param termFrequencyCalculator
     */
    public void setTermFrequencyCalculator(final FrequencyCalculator termFrequencyCalculator) {
        this.termFrequencyCalculator = termFrequencyCalculator;
    }
}
