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

package textractor.html;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import textractor.database.DbManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.Article;
import textractor.datamodel.PaddingDocument;
import textractor.datamodel.Sentence;
import textractor.datamodel.TextractorDocument;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * Test loading of Medline abstracts.
 *
 * @author campagne Date: Oct 28, 2005 Time: 11:08:58 AM
 */
public class TestLoadPubmed extends TestCase {
    private PubmedAbstracts2Text2DB loader;
    private DbManager dbm;
    private DatabaseTextConsumer consumer;

    @Override
    protected void setUp() throws TextractorDatabaseException {
        final String[] args = {
                "-force"
        };
        loader = new PubmedAbstracts2Text2DB(args);
        consumer = (DatabaseTextConsumer) loader.getConsumer();
        dbm = consumer.getDbManager();
    }

    @Override
    protected void tearDown() {
        if (loader != null) {
            loader.close();
        }
    }

    public void loadOneAbstract() throws IOException,
            TextractorDatabaseException, ConfigurationException {
        final String filename = "data/pubmed/15817129.xml";
        loadAbstract(filename);

        final long pmid = 15817129;

        final int expectedCount = 13;
        assertNSentences(pmid, expectedCount);

        final Vector<Long> sentenceNumbers = deleteArticle(pmid);

        assertArticleAndSentencesRemoved(pmid, sentenceNumbers);
        assertPaddingDocumentsCreated(sentenceNumbers);
    }

    private void assertPaddingDocumentsCreated(final Vector<Long> sentenceNumbers) {

        dbm.beginTxn();
        final TextractorManager textractorManager = dbm.getTextractorManager();

        for (final long sentenceNumber : sentenceNumbers) {
            final TextractorDocument document =
                    textractorManager.getDocument(sentenceNumber);
            assertNotNull(document);
            assertTrue(document instanceof PaddingDocument);
        }
        dbm.commitTxn();
    }

    public void testMultipleRetractionProcessing()
            throws TextractorDatabaseException, IOException,
            ConfigurationException {
        long pmid = 993;

        // load two abstracts that will be retracted:
        String filename = "data/pubmed/retraction/993.xml";
        loadAbstract(filename);
        int expectedCount = 9;
        assertNSentences(pmid, expectedCount);

        pmid = 239707;
        filename = "data/pubmed/retraction/239707.xml";
        loadAbstract(filename);
        expectedCount = 6;
        assertNSentences(pmid, expectedCount);

        Vector<Long> sentenceNumbers_993, sentenceNumbers_239707;
        sentenceNumbers_993 = collectSentenceNumbers(993);
        sentenceNumbers_239707 = collectSentenceNumbers(239707);

        loadAbstract("data/pubmed/retraction/multipleRetractions.xml");
        // check that the retraction notice was loaded:
        final long retraction_pmid = 195582;
        assertNSentences(retraction_pmid, 3);

        // sentences of the retracted articles must have been removed.

        assertArticleAndSentencesRemoved( 993, sentenceNumbers_993);
        assertPaddingDocumentsCreated(sentenceNumbers_993);
        assertArticleAndSentencesRemoved(239707, sentenceNumbers_239707);
        assertPaddingDocumentsCreated(sentenceNumbers_239707);
    }

    public void testRetractionProcessing() throws TextractorDatabaseException,
            IOException, ConfigurationException {

        final long pmid = 7541111;
        final String filename = "data/pubmed/retraction/7541111.xml";
        loadAbstract(filename);

        final int expectedCount = 12;
        assertNSentences(pmid, expectedCount);

        Vector<Long> sentenceNumbers;
        sentenceNumbers = collectSentenceNumbers(pmid);

        loadAbstract("data/pubmed/retraction/noticeOfRetraction.xml");
        // check that the retraction notice was loaded:
        final long retraction_pmid = 11439953;
        assertNSentences(retraction_pmid, 1);

        // sentences of the retracted article must have been removed.
        assertArticleAndSentencesRemoved( pmid, sentenceNumbers);
        assertPaddingDocumentsCreated(sentenceNumbers);
    }

    public void testUpdates() throws TextractorDatabaseException, IOException,
            ConfigurationException {

        final String filename = "data/pubmed/15817129.xml";

        loadAbstract(filename);

        final long pmid = 15817129;

        int expectedCount = 13;
        assertNSentences(pmid, expectedCount);

        final Vector<Long> sentenceNumbers = collectSentenceNumbers(pmid);

        loadAbstract("data/pubmed/15817129-update.xml");

        assertSentencesRemoved(pmid, sentenceNumbers);
        assertPaddingDocumentsCreated(sentenceNumbers);
        // The update has one more sentence than the original file...
        expectedCount = 14;
        assertNSentences(pmid, expectedCount);
        deleteArticle(pmid);
    }

    public void testMultipleArticleDeletes()
            throws TextractorDatabaseException, IOException,
            ConfigurationException {
        loadOneAbstract();

        final String filename = "data/pubmed/15817129.xml";
        loadAbstract(filename);

        final long pmid = 15817129;

        int expectedCount = 13;
        assertNSentences(pmid, expectedCount);

        final Vector<Long> sentenceNumbers = collectSentenceNumbers(pmid);

        loadAbstract("data/pubmed/15817129-multiple-deletes.xml");

        assertSentencesRemoved(pmid, sentenceNumbers);
        assertPaddingDocumentsCreated(sentenceNumbers);
        // The update has two more sentences than the original file...
        expectedCount = 15;
        assertNSentences(pmid, expectedCount);
        deleteArticle(pmid);
    }

    private void assertArticleAndSentencesRemoved(final long pmid,
                                                  final Vector<Long> sentenceNumbers) {
        dbm.beginTxn();
        final TextractorManager textractorManager = dbm.getTextractorManager();

        final Article article = textractorManager.getArticleByPmid(pmid);
        assertNull("Article must not be found after deletion.", article);

        for (final long sentenceNumber : sentenceNumbers) {
            assertNull(textractorManager.getSentence(sentenceNumber));
        }
        dbm.commitTxn();
    }

    /**
     * This method tests that sentences have been removed. It does not check the
     * article.
     */
    private long assertSentencesRemoved(final long pmid,
                                        final Vector<Long> sentenceNumbers) {
        Article article;
        dbm.beginTxn();
        final TextractorManager textractorManager = dbm.getTextractorManager();

        article = textractorManager.getArticleByPmid(pmid);
        final long articleNumber = article.getArticleNumber();
        for (final long sentenceNumber : sentenceNumbers) {
            assertNull(textractorManager.getSentence(sentenceNumber));
        }

        dbm.commitTxn();
        return articleNumber;
    }

    private Vector<Long> deleteArticle(final long pmid) {
        consumer.begin();
        final TextractorManager textractorManager = dbm.getTextractorManager();
        final Article article = textractorManager.getArticleByPmid(pmid);
        final Vector<Long> sentenceNumbers =
                collectSentenceNumbers(textractorManager, article);
        textractorManager.deleteArticle(article);

        consumer.end();
        return sentenceNumbers;
    }

    private int countSentences(final TextractorManager textractorManager,
                               final Article article) {
        final Iterator it = textractorManager.getSentenceIterator(article);
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }

    private Vector<Long> collectSentenceNumbers(final long pmid) {
        Vector<Long> sentenceNumbers;
        consumer.begin();
        final TextractorManager textractorManager = dbm.getTextractorManager();
        final Article article = textractorManager.getArticleByPmid(pmid);
        sentenceNumbers = collectSentenceNumbers(textractorManager, article);
        consumer.end();
        return sentenceNumbers;

    }

    private Vector<Long> collectSentenceNumbers(
            final TextractorManager textractorManager, final Article article) {
        final Iterator it = textractorManager.getSentenceIterator(article);
        final Vector<Long> result = new Vector<Long>();

        while (it.hasNext()) {
            final Sentence sentence = (Sentence) it.next();
            result.add(sentence.getDocumentNumber());
        }
        return result;
    }

    private void assertNSentences(final long pmid, final int expectedCount) {
        dbm.beginTxn();
        final TextractorManager textractorManager = dbm.getTextractorManager();
        final Article article = textractorManager.getArticleByPmid(pmid);
        assertNotNull("Article " + pmid + " should not be null", article);
        assertEquals(pmid, article.getPmid());

        final int count = countSentences(textractorManager, article);

        assertEquals(
                "This abstract must have " + expectedCount + " sentences.",
                expectedCount, count);

        dbm.commitTxn();
    }

    private void loadAbstract(final String filename)
            throws TextractorDatabaseException, IOException,
            ConfigurationException {
         final String[] args = {
                "-force", "-i", filename
        };
        loader.process(args);
    }

    /**
     * Test that validates that the pubmed configuration is initialized
     * properly. NOTE: This will only run as part of the suite.
     */
    public void preConfigTest() {
        final File configFile = new File("medline.xml");
        if (configFile.exists()) {
            configFile.delete();
        }

        assertFalse(configFile.exists());
    }

    /**
     * Test that validates that the pubmed configuration is written
     * properly after all the other tests have been run.
     * NOTE: This will only run as part of the suite.
     */
    public void postConfigTest() throws ConfigurationException {
        final File configFile = new File("medline.xml");
        assertTrue(configFile.exists());
        assertTrue(configFile.canRead());

        assertTrue(PubmedAbstracts2Text2DB.isFileLoaded(
                new XMLConfiguration("medline.xml"),
                "data/pubmed/15817129.xml"));
    }

    /**
     * Return a test suite for this set of cases.
     *
     * @return A test suite for this class.
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();

        suite.addTest(TestSuite.createTest(TestLoadPubmed.class,
                "preConfigTest"));

        suite.addTestSuite(TestLoadPubmed.class);

        suite.addTest(TestSuite.createTest(TestLoadPubmed.class,
                "postConfigTest"));

        return suite;
    }
}
