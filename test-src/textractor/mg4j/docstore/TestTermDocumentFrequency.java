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

package textractor.mg4j.docstore;

import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.TextractorDatabaseException;

import java.io.IOException;

/**
 * @author Fabien Campagne
 *         Date: Jun 18, 2006
 *         Time: 11:48:58 AM
 */
public class TestTermDocumentFrequency extends TestCase {

    public void testWriteReadOneDoc() throws TextractorDatabaseException, ConfigurationException, IOException {

        final String basename = "index/term-doc-freqs-test";
        final int maximumTermIndex = 5;
        final TermDocumentFrequencyWriter writer = new TermDocumentFrequencyWriter(maximumTermIndex, basename);
        final int [] tokens = {1, 2, 1, 1, 0, 1, 4, 2, 2, 5};

        final int written = writer.appendDocument(0, tokens);
        assertEquals(1, writer.getTermDocumentFrequency(0));
        assertEquals(4, writer.getTermDocumentFrequency(1));
        assertEquals(3, writer.getTermDocumentFrequency(2));
        assertEquals(0, writer.getTermDocumentFrequency(3));
        assertEquals(1, writer.getTermDocumentFrequency(4));
        assertEquals(1, writer.getTermDocumentFrequency(5));
        writer.close();
        assertTrue("Some bits must have been written", written > 0);

        final int documentNumber = 1;
        final int maxReads = 2;
        final TermDocumentFrequencyReader reader = new TermDocumentFrequencyReader(basename, documentNumber, maximumTermIndex,
                maxReads);
        final int[] frequencies = reader.createFrequencyStorage();
        final int sum = reader.read(0, frequencies);
        assertEquals(10, sum);

        assertEquals(1, frequencies[0]);
        assertEquals(4, frequencies[1]);
        assertEquals(3, frequencies[2]);
        assertEquals(0, frequencies[3]);
        assertEquals(1, frequencies[4]);
        assertEquals(1, frequencies[5]);

        // test that term doc frequencies are added to previous values in frequencies, not set:
        reader.read(0, frequencies);
        assertEquals(1 * 2, frequencies[0]);
        assertEquals(4 * 2, frequencies[1]);
        assertEquals(3 * 2, frequencies[2]);
        assertEquals(0 * 2, frequencies[3]);
        assertEquals(1 * 2, frequencies[4]);
        assertEquals(1 * 2, frequencies[5]);
    }

    public void testWriteReadAFewDocs() throws TextractorDatabaseException, ConfigurationException, IOException {

        final String basename = "index/term-doc-freqs-test";
        final int maximumTermIndex = 5;
        final TermDocumentFrequencyWriter writer = new TermDocumentFrequencyWriter(maximumTermIndex, basename);
        final int [] tokens_0 = {1, 2, 5, 1, 3, 1, 4, 2, 2, 5};
        final int [] tokens_1 = {3, 3, 3, 3};
        final int [] tokens_2 = {1, 2, 5, 1, 1, 1, 2, 2, 5};

        int written = writer.appendDocument(0, tokens_0);
        assertTrue("Some bits must have been written", written > 0);
        written = writer.appendDocument(0, tokens_1);
        assertTrue("Some bits must have been written", written > 0);
        written = writer.appendDocument(0, tokens_2);
        assertTrue("Some bits must have been written", written > 0);

        writer.close();

        final int documentNumber = 3;
        final int maxReads = 1;
        final TermDocumentFrequencyReader reader = new TermDocumentFrequencyReader(basename, documentNumber, maximumTermIndex,
                maxReads);
        final int[] frequencies = reader.createFrequencyStorage();
        reader.read(0, frequencies);
        assertEquals(3, frequencies[1]);
        assertEquals(3, frequencies[2]);
        assertEquals(1, frequencies[3]);
        assertEquals(1, frequencies[4]);
        assertEquals(2, frequencies[5]);

        reader.read(1, frequencies);
        assertEquals(3, frequencies[1]);
        assertEquals(3, frequencies[2]);
        assertEquals(1 + 4, frequencies[3]);
        assertEquals(1, frequencies[4]);
        assertEquals(2, frequencies[5]);

        reader.read(2, frequencies);
        assertEquals(3 + 4, frequencies[1]);
        assertEquals(3 + 3, frequencies[2]);
        assertEquals(1 + 4, frequencies[3]);
        assertEquals(1, frequencies[4]);
        assertEquals(2 + 2, frequencies[5]);
    }

    public void testSubsetTermsFrequencyStorage() {
        final FrequencyStorageImpl frequencies = new FrequencyStorageImpl(10);
        final TermSubsetTransform subsetTransform = new TermSubsetTransform(frequencies.getTransform()) {
            @Override
	    boolean includeTerm(final int termIndex) {
                return termIndex % 2 == 1; // include odd terms only
            }
        };
        frequencies.setTransform(subsetTransform);
        assertEquals(5, subsetTransform.getTransformedSize());
        for (int i = 0; i < subsetTransform.getTransformedSize(); i++) {
	    frequencies.setFrequency(i, i);
	}
        int j = 0;
        for (int i = 0; i < frequencies.getTermNumber(); i++) {
            if ((i % 2) == 1) {
                assertEquals(j, frequencies.getFrequencyForTerm(i));
                ++j;
            }
        }

    }
}
