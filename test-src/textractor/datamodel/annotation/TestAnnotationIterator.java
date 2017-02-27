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

package textractor.datamodel.annotation;

import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.datamodel.TermOccurrence;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: campagne
 * Date: May 28, 2004
 * Time: 4:56:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestAnnotationIterator extends TestCase {
    private static final Log LOG =
            LogFactory.getLog(TestAnnotationIterator.class);
    private DbManager dbm;
    private TextractorManager tm;

    public TestAnnotationIterator(final String name) throws TextractorDatabaseException {
        super(name);
        dbm = new DbManager();
    }

    @Override
    protected void setUp() {
        dbm.beginTxn();
        tm = dbm.getTextractorManager();
    }

    @Override
    protected void tearDown() {
        dbm.commitTxn();
    }

    public void testIterator() throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docManager = new DocumentIndexManager(tm.getInfo().getCaseSensitiveIndexBasename());
        final Article article = tm.createArticle();
        article.setArticleNumber(dbm.getTextractorManager().getNextArticleNumber());
        article.addTermOccurrence(new TermOccurrence("Site", docManager.extractTerms("Site",'_'), 1));
        article.addTermOccurrence(new TermOccurrence("Kinase", docManager.extractTerms("Kinase",'_'), 1));
        dbm.makePersistent(article);
        final Sentence s1 = tm.createNewSentence(article);
        s1.setText("Mutagenesis of Deoxyguanosine Site at Arginine Site 79  Up-regulates");
        s1.setDocumentNumber(tm.getNextDocumentNumber());
        dbm.makePersistent(s1);
        final Sentence s2 = tm.createNewSentence(article);
        s2.setDocumentNumber(tm.getNextDocumentNumber());
        s2.setText("Deoxyadenosine Kinase Subunit of Heterodimeric Enzyme Site from Lactobacillus acidophilus R26  ");
        dbm.makePersistent(s2);

        dbm.commitTxn();
        dbm.beginTxn();
        final ArticleSingleTermAnnotationIterator it = new ArticleSingleTermAnnotationIterator(tm, docManager,
                article, new int[]{docManager.findTermIndex("site")});

        boolean hasNext = it.hasNext();
        assertTrue("iterator must have at least one element.", hasNext);
        Object next = it.next();
        assertNotNull("next() must not return null.", next);
        AnnotationSource annotation = (AnnotationSource) next;
        LOG.debug("annotation.sentence.text: " + annotation.getSentence().getText());
        assertEquals("first annotation returned must be from sentence s1.", s1, annotation.getSentence());
        assertNotNull("indexed term must not be null.", annotation.getIndexedTerms());
        assertEquals("number of terms must be 1.", 1, annotation.getTermNumber());
        AnnotatedTerm at = annotation.getTerm(0);
        assertNotNull("getTerm(0) must not return null.", at);
        assertEquals("site", at.getTermText());
        assertEquals("Position of first Site must be 3", 3, at.getStartPosition());

        hasNext = it.hasNext();

        assertTrue("iterator must have a second element.", hasNext);
        next = it.next();
        annotation = (AnnotationSource) next;
        at = annotation.getTerm(0);
        assertNotNull("getTerm(0) must not return null.", at);
        assertEquals("site", at.getTermText());
        assertEquals("Position of second Site must be 6", 6, at.getStartPosition());

        hasNext = it.hasNext();

        assertTrue("iterator must have a third element.", hasNext);
        next = it.next();
        annotation = (AnnotationSource) next;
        assertEquals("third annotation must be from sentence s2.", s2, annotation.getSentence());
        at = annotation.getTerm(0);
        assertNotNull("getTerm(0) must not return null.", at);
        assertEquals("site", at.getTermText());
        assertEquals("Position of second Site must be 6", 6, at.getStartPosition());

        hasNext = it.hasNext();

        assertFalse("iteration must be finished.", hasNext);
        dbm.delete(s1);
        dbm.delete(s2);
    }
}
