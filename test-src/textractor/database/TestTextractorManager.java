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

package textractor.database;

import it.unimi.dsi.mg4j.util.MutableString;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.datamodel.TermOccurrence;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Fabien Campagne
 * Date: Jan 10, 2004
 * Time: 12:39:52 PM
 * To change this template use Options | File Templates.
 */
public class TestTextractorManager extends TestCase {
    /** Used to log debug and informational messages. */
    public static final Log LOG =
        LogFactory.getLog(TestTextractorManager.class);

    /** Database manager to use for the tests. */
    private DbManager dbm;

    /** Textractor manager to use for the tests. */
    private TextractorManager tm;

    public TestTextractorManager(final String name)
        throws TextractorDatabaseException {
        super(name);
        dbm = new DbManager();
        tm = dbm.getTextractorManager();
    }

    @Override
    protected void setUp() {
        dbm.beginTxn();
    }

    @Override
    protected void tearDown() {
        dbm.commitTxn();
    }

    public void testSentenceIterator() {
        final Iterator it = tm.getSentenceIterator();
        assertNotNull(it);
        while (it.hasNext()) {
            assertNotNull("iterator valid", it.next());
        }
    }

    public void testSentenceIteratorLowerBound() {
        final int lowerBound = 10;
        final Iterator it = tm.getSentenceIterator(lowerBound);
        assertNotNull(it);
        Sentence sentence;
        while (it.hasNext()) {
            sentence = (Sentence) it.next();
            assertTrue(sentence.getDocumentNumber() > lowerBound);
        }
    }

    public void testSentenceIteratorLowerAndUpperBounds() {
        final int lowerBound = 10;
        final long upperBound = 20;
        Iterator it = tm.getSentenceIterator(lowerBound, upperBound);
        assertNotNull(it);
        Sentence sentence;
        int count = 0;
        while (it.hasNext()) {
            sentence = (Sentence) it.next();
            assertTrue(sentence.getDocumentNumber() > lowerBound);
            assertTrue(sentence.getDocumentNumber() <= upperBound);
            count++;
        }
        assertEquals(10, count);

        it = tm.getSentenceIterator(-1, upperBound);
        assertNotNull(it);
    }

    public void testSentenceIteratorArticle() {
        final Article article = tm.createArticle();
        article.setArticleNumber(dbm.getTextractorManager().getNextArticleNumber());
        dbm.makePersistent(article);
        final Sentence s1 = tm.createNewSentence(article);
        s1.setDocumentNumber(tm.getNextDocumentNumber());
        dbm.makePersistent(s1);
        final Sentence s2 = tm.createNewSentence(article);
        s2.setDocumentNumber(tm.getNextDocumentNumber());
        dbm.makePersistent(s2);

        dbm.commitTxn();
        dbm.beginTxn();

        final Iterator it = tm.getSentenceIterator(article);
        assertNotNull(it);
        Sentence sentence;
        long lastDocumentNumber = -1;
        int count = 0;
        while (it.hasNext()) {
            sentence = (Sentence) it.next();
            dbm.retrieve(sentence);
            assertEquals("all the sentences should be from the query article.", article, sentence.getArticle());
            assertTrue("sentences should be returned in increasing number of documentNumber.", sentence.getDocumentNumber() > lastDocumentNumber);
            lastDocumentNumber = sentence.getDocumentNumber();
            count++;
        }
        assertEquals("Count must be 2", 2, count);
        dbm.delete(article);
        dbm.delete(s1);
        dbm.delete(s2);
    }

    // TODO??
    /*   public void testTermCount() {
           Collection t = tm.getSentence(1).getArticle().getAllTermOccurrences();
           Vector v = new Vector(t);
           assertEquals(15, ((TermOccurrence)v.elementAt(0)).getCount());
       }

       public void testNumTerms() {
           Collection t = tm.getSentence(1).getArticle().getAllTermOccurrences();
           assertEquals(3450, t.size());
       }*/

    public void testTermCount() {
        final Article article = tm.getArticleByNumber(0);
        final TermOccurrence to = article.getTermOccurrence(1);
        assertEquals("dGK", to.getTerm());
        assertEquals(56, to.getCount());
    }

    public void testSentencesToPMIDs() {
        final int[] docs = {1, 416};
        final long[] result = tm.sentenceToPMID(docs);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(7896799, result[0]);
        assertEquals(12507430, result[1]);
    }

    public void testShortSentence() {
        final Article article = new Article();
        final String text = "some text";
        final Sentence longSentence = new Sentence(article, text);
        assertEquals(longSentence.getText(), text);

    }

    public void testVeryLongSentence() {
        final MutableString longText = new MutableString();
        final char[] choices = {'a', 'b', 'c', 'd', 'e', 'f'};
        final int targetLength = Sentence.TEXT_MAX_LENGTH * 5 + 1292;
        for (int i = 0; i < targetLength; i++) {
            final char character = choices[(int) Math.round(Math.random() * (choices.length - 1))];

            longText.append(character);
        }
        assertEquals(targetLength, longText.length());
        final Article article = new Article();
        final Sentence longSentence = new Sentence(article, longText.toString());
        assertEquals(longSentence.getText(), longText.toString());
    }
}
