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
import textractor.database.DbManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.test.util.TextractorTestConstants;

import java.util.Collection;
import java.util.Iterator;

/**
 * Validates annotation batch operations.
 */
public class TestAnnotationBatch extends TestCase {
    /** Database manager to use for the tests. */
    private DbManager dbm;

    /** Textractor manager to use for the tests. */
    private TextractorManager tm;

    @Override
    protected void setUp() throws TextractorDatabaseException {
        dbm = new DbManager();
        tm = dbm.getTextractorManager();
        dbm.beginTxn();
    }

    @Override
    protected void tearDown() {
        if (dbm.txnInProgress()) {
            dbm.commitTxn();
        }
        dbm.shutdown();
    }

    public void testAnnotationsInBatch() {
        final Collection annotations =
            tm.getAnnotationsInBatch(TextractorTestConstants.MUTATION_BATCH_ID);
        assertEquals(39, annotations.size());
    }

    public void testUnannotatedAnnotationsInBatch() {
        int total = 0;
        Collection annotations =
            tm.getUnannotatedAnnotationsInBatch(TextractorTestConstants.MUTATION_BATCH_ID);
        Iterator it = annotations.iterator();
        while (it.hasNext()) {
            final TextFragmentAnnotation textFragmentAnnotation =
                (TextFragmentAnnotation) it.next();
            assertEquals("batch number must be 0",
                    0, textFragmentAnnotation.getAnnotationBatchNumber());
            total++;
        }

        assertTrue("Number of unnanotated annotations in batch 0 must be greater than 8", (total > 8));
        assertEquals("Size must match total count", total, annotations.size());
        assertFalse("annotation must not be annotated",
                tm.getAnnotationById(45).isAnnotationImported());
        assertFalse("annotation must not be annotated",
                tm.getAnnotationById(46).isAnnotationImported());
        assertFalse("annotation must not be annotated",
                tm.getAnnotationById(47).isAnnotationImported());
        assertFalse("annotation must not be annotated",
                tm.getAnnotationById(48).isAnnotationImported());
        assertFalse("annotation must not be annotated",
                tm.getAnnotationById(49).isAnnotationImported());
        annotations = tm.getUnannotatedAnnotationsInBatch(TextractorTestConstants.MUTATION_BATCH_ID, 45, 5);
        it = annotations.iterator();
        int count = 0;
        while (it.hasNext()) {
            final TextFragmentAnnotation textFragmentAnnotation =
                (TextFragmentAnnotation) it.next();
            assertEquals("batch number must be 0",
                    0, textFragmentAnnotation.getAnnotationBatchNumber());
            assertTrue("annotation number must be greater than 3",
                    textFragmentAnnotation.getAnnotationNumber() > 3);
            count++;
        }
        assertEquals("Five annotations must be returned.", 5, count);
    }
}
