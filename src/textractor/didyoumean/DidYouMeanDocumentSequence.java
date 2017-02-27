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

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.mg4j.document.Document;
import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.document.DocumentIterator;
import it.unimi.dsi.mg4j.document.DocumentSequence;
import it.unimi.dsi.mg4j.index.DiskBasedIndex;
import it.unimi.dsi.mg4j.io.InputBitStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.TermFrequency;
import textractor.mg4j.index.TermIterator;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * User: campagne
 * Date: Nov 8, 2005
 * Time: 3:05:52 PM
 */
public final class DidYouMeanDocumentSequence implements DocumentSequence {
    private TermIterator terms;
    private final DocumentIndexManager docmanager;
    private double frequencyThreshold;
    private double offsetThreshold;
    private LongList offsets;
    private long previousIndex;
    private int termCounter;
    private DocumentFactory factory;

    /** Used to log debug and informational messages. */
    private static final Log LOG =
            LogFactory.getLog(DidYouMeanDocumentSequence.class);

    public DidYouMeanDocumentSequence(final DocumentIndexManager documentIndexManager, final int freq) throws IOException {
        this.docmanager = documentIndexManager;

        LOG.info("Setting offset threshold for frequency count of " + freq);
        offsets = DiskBasedIndex.readOffsets(new InputBitStream(docmanager.getBasename() + ".offsets"), docmanager.getNumberOfTerms());

        setOffsetThreshold(calculateOffsetThreshold(freq));
        this.terms = documentTerms();
        factory = new DidYouMeanDocumentFactory();
    }

    private TermIterator documentTerms() throws FileNotFoundException {
        return docmanager.getTerms();
    }


    private double calculateOffsetThreshold(final int frequencyThreshold) throws IOException {
        final int numberOfTerms = docmanager.getNumberOfTerms();
        long prev = 0;
        double returnThreshold = 0;

        final TermFrequency frequency = new TermFrequency();
        final double frequencyThresholdUpper =
                frequencyThreshold + (frequencyThreshold * 0.03);
        final double frequencyThresholdLower =
                frequencyThreshold - (frequencyThreshold * 0.03);

        for (int i = 0; i < numberOfTerms; ++i) {
            final long current = offsets.getLong(i + 1);
            final long length = current - prev;
            prev = current;

            final String term = docmanager.termAsString(i);
            docmanager.frequency(new String[]{term}, frequency);

            if ((frequency.getOccurenceFrequency() > frequencyThresholdLower) && (frequency.getOccurenceFrequency() < frequencyThresholdUpper)) {
                LOG.debug("Frequency thresholds (lower:actual:upper): "+frequencyThresholdLower + " : " + frequency.getOccurenceFrequency() + " : " + frequencyThresholdUpper);
                returnThreshold = length;
                break;
            }
        }

        if (returnThreshold != 0) {
            LOG.info("Threshold offset set: " + returnThreshold);
        } else {
            LOG.warn("Offset threshold is 0: All terms will be indexed.");
        }
        return returnThreshold;
    }


    public DocumentFactory factory() {
        return factory;
    }

    public void close() throws IOException {
        terms.close();
    }

    public long getTermOffsetLength(final CharSequence string, final int termIndex) {
        final long currentIndex = offsets.getLong(termIndex + 1);
        final long length = currentIndex - previousIndex;

        previousIndex = currentIndex;
        return length;
    }

    public DocumentIterator iterator() throws IOException {
        return new DocumentIterator() {
            public Document nextDocument() throws IOException {
                if (terms.hasNext()) {
                    final CharSequence iteratedString = terms.nextTerm();
                    ++termCounter;
                    if (getTermOffsetLength(iteratedString, termCounter-1) > getOffsetThreshold()) {
                        return createTermDocument( iteratedString);
                    } else {
                        return createTermDocument(" ");
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Terms iterated: " + termCounter);
                    }
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Docmanager reports: "
                	+ docmanager.getNumberOfTerms());
                }
                termCounter = 0;
                previousIndex = 0;
                return null;
            }

            private Document createTermDocument(final CharSequence term) {
                return new DidYouMeanDocument(term);
            }

            public void close() throws IOException {
                terms.close();
            }
        };
    }

    public double getFrequencyThreshold() {
        return frequencyThreshold;
    }

    public void setFrequencyThreshold(final double newFrequency) {
        frequencyThreshold = newFrequency;
    }

    public double getOffsetThreshold() {
        return offsetThreshold;
    }

    public void setOffsetThreshold(final double offsetThreshold) {
        this.offsetThreshold = offsetThreshold;
    }
}
