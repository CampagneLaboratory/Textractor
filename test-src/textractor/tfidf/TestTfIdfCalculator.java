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

import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.docstore.TermDocumentFrequencyReader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class TestTfIdfCalculator extends TestCase {
    public void testTfIdfCalculator() throws ConfigurationException,
            IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager("index/junit-test-basename");
        final TermDocumentFrequencyReader termDocumentFrequencyReader =
                new TermDocumentFrequencyReader(docmanager);
        final TfIdfCalculator calculator =
                new TfIdfCalculator(termDocumentFrequencyReader, docmanager);
        final int documents[] = new int[10];
        for (int i = 0; i < documents.length; i++) {
            documents[i] = i;
        }

        float[] tfIdfs = calculator.evaluates(documents);
        int termIndex = 0;
        assertNotNull(tfIdfs);
        for (float tfIdf : tfIdfs) {
            if (tfIdf != 0) {
                System.out.println("term: " + calculator.getTerm(termIndex) + " tfIdf: " + tfIdf);
            }
            ++termIndex;
        }
        int[] numDocForTerm = calculator.allocateNumDocForTerm();
        assertNotNull(numDocForTerm);
        assertEquals(tfIdfs.length, numDocForTerm.length);
        tfIdfs = calculator.evaluates(documents, null, numDocForTerm);

        assertNotNull(tfIdfs);
        termIndex = 0;
        for (float tfIdf : tfIdfs) {
            int docCount = numDocForTerm[termIndex];
            if (tfIdf != 0) {
                assertTrue("the term must occur in at least one document", docCount > 0);
                System.out.println("term: " + calculator.getTerm(termIndex) + " is common to " + docCount +" documents");
            }
            ++termIndex;
        }


    }


}
