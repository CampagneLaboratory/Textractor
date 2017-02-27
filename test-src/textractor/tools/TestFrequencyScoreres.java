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

import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.docstore.DocumentStoreReader;
import textractor.mg4j.docstore.DocumentStoreWriter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public final class TestFrequencyScoreres extends TestCase {
    private static final String BASENAME = "index/scorer-test";

    @Override
    protected void setUp() throws Exception {
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(BASENAME);

        final String[] documents = {
                "There is a light that never goes out",
                "That joke isn't funny anymore, that joke isn't funny anymore",
                "Hand in glove",
                "Shoplifters of the world",
        };

        indexBuilder.index(documents);
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));

        final DocumentStoreWriter docStoreWriter =
                new DocumentStoreWriter(docmanager);
        docStoreWriter.optimizeTermOrdering();

        for (int n = 0; n < documents.length; n++) {
            docStoreWriter.appendDocument(n,
                    docmanager.extractTerms(documents[n]));
        }

        docStoreWriter.close();
        docmanager.close();
    }

    public void testTermFrequencyScorer() throws NoSuchMethodException,
            IllegalAccessException, ConfigurationException, IOException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(BASENAME);
        final DocumentStoreReader reader = new DocumentStoreReader(docmanager);
        final FrequencyScorer scorer = new FrequencyScorer();
        scorer.setDocumentFrequencyCalculator(new RawFrequencyCalculator(docmanager.getDocumentNumber()));
        scorer.setTermFrequencyCalculator(new RawFrequencyCalculator(docmanager.getDocumentNumber()));
        scorer.setDocmanager(docmanager);
        scorer.setReader(reader);
        scorer.setTerm("that");

        assertEquals(3.0, scorer.getTermFrequency());
        assertEquals(2.0, scorer.getDocumentFrequency());
        docmanager.close();
    }

    public void testTFIDFTermFrequencyScorer() throws NoSuchMethodException,
            IllegalAccessException, ConfigurationException, IOException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager = new DocumentIndexManager(BASENAME);
        final DocumentStoreReader reader = new DocumentStoreReader(docmanager);
        final FrequencyScorer scorer = new FrequencyScorer();
        scorer.setDocumentFrequencyCalculator(new LogarithmicDocumentCalculator(docmanager.getDocumentNumber()));
        scorer.setTermFrequencyCalculator(new LogarithmicTermCalculator(docmanager.getDocumentNumber()));
        scorer.setDocmanager(docmanager);
        scorer.setReader(reader);
        scorer.setTerm("that");

        assertEquals(2.09861228866811, scorer.getTermFrequency());
        assertEquals(0.6931471805599453, scorer.getDocumentFrequency());
        assertEquals(1.4546471909787544, scorer.getWeight());
    }
}
