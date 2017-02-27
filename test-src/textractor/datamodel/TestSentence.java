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

import junit.framework.TestCase;
import textractor.database.DbManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;

import javax.jdo.PersistenceManager;
import java.io.UnsupportedEncodingException;

/**
 * Test basic sentence functions.
 */
public final class TestSentence extends TestCase {
    /**
     * Database manager used within the test.
     */
    private transient DbManager dbManager;

    /**
     * Textractor manager used within the test.
     */
    private transient TextractorManager textractorManager;

    /**
     * Document number of the sentence created during the test.  This is stored
     * so that the sentence can be deleted after the test is complete.
     */
    private transient long documentNumber;

    /**
     * Set up preconditions for the test.  In this case we get the database
     * manager that will be used during the test.
     *
     * @throws TextractorDatabaseException if there was a problem initializing
     * the database manager.
     */
    @Override
    protected void setUp() throws TextractorDatabaseException {
        dbManager = new DbManager();
        textractorManager = dbManager.getTextractorManager();
    }

    /**
     * Clean up after the test is complete.  In this case we delete any
     * temporary database objects created during the test and shutdown
     * the database manager
     */
    @Override
    protected void tearDown() {
        if (!dbManager.txnInProgress()) {
            dbManager.beginTxn();
        }

        final Sentence sentence = textractorManager.getSentence(documentNumber);
        dbManager.delete(sentence);

        if (dbManager.txnInProgress()) {
            dbManager.commitTxn();
        }

        dbManager.shutdown();
    }

    /**
     * This test case ensures that the database can handle UTF-8 encoded
     * text properly.
     *
     * @throws TextractorDatabaseException if there was a problem with
     * the database manager.
     * @throws UnsupportedEncodingException if the JVM does not support
     * UTF-8 encoding
     */
    public void testUTF8EncodedText()
        throws TextractorDatabaseException, UnsupportedEncodingException {
        final String text =
            new String("Hallo, verrückte Welt!".getBytes(), "UTF-8");
        final int byteCount = text.getBytes().length;

        // create a sentence in the database with UTF-8 text
        dbManager.beginTxn();
        documentNumber = textractorManager.getNextDocumentNumber();
        final Sentence sentence = new Sentence();
        sentence.setText(text);
        sentence.setDocumentNumber(documentNumber);
        dbManager.makePersistent(sentence);
        dbManager.commitTxn();

        // clear the database cache just in case
        dbManager.beginTxn();
        final PersistenceManager persistenceManager = dbManager.getPM();
        persistenceManager.evictAll();
        dbManager.commitTxn();

        // get the sentence from the database and compare it to the original
        dbManager.beginTxn();
        final Sentence stored = textractorManager.getSentence(documentNumber);
        assertNotNull("Sentence just stored was not found!", stored);
        final String storedText = stored.getText();
        final int storedByteCount = storedText.getBytes().length;

        assertEquals("Sentence text does not match what was stored!",
                text, storedText);
        assertEquals("Sentence length does not match what was stored.",
                byteCount, storedByteCount);
        dbManager.commitTxn();
    }
}
