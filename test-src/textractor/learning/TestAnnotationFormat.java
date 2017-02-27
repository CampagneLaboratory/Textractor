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

package textractor.learning;

import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.annotation.SingleTermAnnotation;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: Fabien Campagne
 * Date: Jan 10, 2004
 * Time: 12:39:52 PM
 * To change this template use Options | File Templates.
 */
public class TestAnnotationFormat extends TestCase {
    private DocumentIndexManager docmanager;
    private DbManager dbm;
    private TextractorManager tm;

    public TestAnnotationFormat(final String name)
            throws TextractorDatabaseException, IOException,
            ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        super(name);
        dbm = new DbManager();

        final String basename = "index/junit-test-basename";
        docmanager = new DocumentIndexManager(basename);
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

    public void testWrite() throws IOException, TextractorDatabaseException {
        final StringWriter string_writer = new StringWriter();
        final SingleTermAnnotation annotation1 = new SingleTermAnnotation(0, 1);
        final SingleTermAnnotation annotation2 = new SingleTermAnnotation(0, 2);
        final AnnotationFormatWriter writer =
                new AnnotationFormatWriter(docmanager, string_writer, true);

        final String first_text_fragment = "This is a test";

        annotation2.setUseSentenceText(false);
        annotation1.setCurrentText(first_text_fragment);
        writer.writeAnnotation(annotation1);
        final String second_text_fragment = "This is the second part of the test";
        annotation2.setUseSentenceText(false);
        annotation2.setCurrentText(second_text_fragment);

        writer.writeAnnotation(annotation2);
        writer.flush();

        final String formatted_text =
                "# The following annotations belong to batch number 0\n" +
                        "--- 1 --- : < enter here properties as inferred from evidence in the text below:\n" +
                        first_text_fragment + " \n" +
                        "--- 2 --- : < enter here properties as inferred from evidence in the text below:\n" +
                        second_text_fragment + " \n";
        assertEquals(formatted_text, string_writer.getBuffer().toString());
    }

    public void testWriteAnnotation() throws IOException, TextractorDatabaseException {
        final String first_text_fragment = "This is a test";
        final String second_text_fragment = "This is the second part of the test";

        final int batchCounter = tm.getNextAnnotationBatchNumber();
        final SingleTermAnnotation annotation1 = tm.createSingleTermAnnotation(batchCounter);
        annotation1.setCurrentText(first_text_fragment);
        final SingleTermAnnotation annotation2 = tm.createSingleTermAnnotation(batchCounter);
        annotation2.setCurrentText(second_text_fragment);

        final String formatted_text =
                "# The following annotations belong to batch number " + annotation1.getAnnotationBatchNumber() + "\n" +
                        "--- " + annotation1.getAnnotationNumber() + " --- : < enter here properties as inferred from evidence in the text below:\n" +
                        first_text_fragment + " \n" +
                        "--- " + annotation2.getAnnotationNumber() + " --- : < enter here properties as inferred from evidence in the text below:\n" +
                        second_text_fragment + " \n";

        final StringWriter string_writer = new StringWriter();
        final AnnotationFormatWriter writer = new AnnotationFormatWriter(docmanager, string_writer, true);
        writer.writeFileHeader(annotation1);
        writer.writeAnnotation(annotation1);
        writer.writeAnnotation(annotation2);
        writer.flush();
        assertEquals(formatted_text, string_writer.getBuffer().toString());
    }

    public void testTextFragmentAnnotation() {
        final int batchCounter = tm.getNextAnnotationBatchNumber();
        final SingleTermAnnotation annotation =
                tm.createSingleTermAnnotation(batchCounter);
        for (int i = 0; i < 64; i++) {
            annotation.setAnnotation(i, false);
            assertFalse("i: " + i, annotation.getBooleanAnnotation(i));
            annotation.setAnnotation(i, true);
            assertTrue("i: " + i, annotation.getBooleanAnnotation(i));
        }
    }

    public void testReadWriteAnnotation() throws IOException, TextractorDatabaseException, SyntaxErrorException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        final String first_text_fragment = "This is a test that describes the mutation R79K";
        final String second_text_fragment = "This is the second part of the test";

        final int batchCounter = tm.getNextAnnotationBatchNumber();
        final SingleTermAnnotation annotation1 = tm.createSingleTermAnnotation(batchCounter);

        annotation1.setCurrentText(first_text_fragment);
        final SingleTermAnnotation annotation2 = tm.createSingleTermAnnotation(batchCounter);
        annotation2.setCurrentText(second_text_fragment);

        dbm.makePersistent(annotation1);
        dbm.makePersistent(annotation2);
        dbm.checkpointTxn();
        // typically, here the annotations would be written with AnnotationFormatWriter,
        // hand modified and read back with AnnotationFormatReader.

        // We just do the last step:

        final String hand_modified_text =
                "*--- " + annotation1.getAnnotationNumber() + " --- R79K:mutation < enter here properties as inferred from evidence in the text below:\n" +
                        first_text_fragment + " \n" +
                        "--- " + annotation2.getAnnotationNumber() + " ---  < enter here properties as inferred from evidence in the text below:\n" +
                        second_text_fragment + " \n";

        final StringReader string_reader = new StringReader(hand_modified_text);
        final AnnotationFormatReader reader = new AnnotationFormatReader(tm, string_reader);

        reader.updateAnnotations();

        // and check that annotations that were marked with * have been updated in the DB:

        assertTrue("annotation1 must have been imported.", annotation1.isAnnotationImported());
        assertFalse("annotation2 must not have been imported.", annotation2.isAnnotationImported());

        // check that annotaion1 has attribute mutation:
        assertTrue("annotation1 must have attribute mutation.", annotation1.getBooleanAnnotation(SingleTermAnnotation.ANNOTATION_MUTATION));
        reader.close();
    }

    public void testWriteWithMarkPosition() throws IOException, TextractorDatabaseException {
        final StringWriter string_writer = new StringWriter();
        final AnnotationFormatWriter writer = new AnnotationFormatWriter(docmanager, string_writer, true);
        final SingleTermAnnotation annotation1 = new SingleTermAnnotation(0, 1);
        final SingleTermAnnotation annotation2 = new SingleTermAnnotation(0, 2);

        final String first_text_fragment = "This is a test";
        annotation1.setCurrentText(first_text_fragment);
        writer.writeAnnotation(annotation1);
        final String second_text_fragment = "This perhaps \n\nis the second part of the test";
        annotation2.setCurrentText(second_text_fragment);
        annotation2.getTerm().setText("is", 2);
        writer.writeAnnotation(annotation2);


        writer.flush();

        final String formatted_text =
                "# The following annotations belong to batch number 0\n" +
                        "--- 1 --- : < enter here properties as inferred from evidence in the text below:\n" +
                        first_text_fragment + " \n" +
                        "--- 2 --- is: < enter here properties as inferred from evidence in the text below:\n" +
                        "This perhaps >is< the second part of the test \n";
        assertEquals(formatted_text, string_writer.getBuffer().toString());


    }

    public void testProteinAnnotationWriting() throws IOException {
        final StringWriter string_writer = new StringWriter();
        final AnnotationFormatWriter writer = new AnnotationFormatWriter(docmanager, string_writer, true);
        final SingleTermAnnotation annotation1 = new SingleTermAnnotation(0, 1);
        final SingleTermAnnotation annotation2 = new SingleTermAnnotation(0, 2);
        final String first_text_fragment = "This is a test";

        annotation1.setCurrentText(first_text_fragment);
        writer.writeAnnotation(annotation1);
        final String second_text_fragment = "This perhaps \n\nis the second part of the test";
        annotation2.setCurrentText(second_text_fragment);
        annotation2.getTerm().setText("is", 2);
        annotation2.setAnnotationImported(true);
        annotation2.setAnnotation(SingleTermAnnotation.INT_ANNOTATION_PROTEIN,
                true);
        writer.writeAnnotation(annotation2);

        writer.flush();

        final String formatted_text =
                "# The following annotations belong to batch number 0\n" +
                        "--- 1 --- : < enter here properties as inferred from evidence in the text below:\n" +
                        first_text_fragment + " \n" +
                        "*--- 2 --- is:protein < enter here properties as inferred from evidence in the text below:\n" +
                        "This perhaps >is< the second part of the test \n";
        assertEquals(formatted_text, string_writer.getBuffer().toString());
    }
}
