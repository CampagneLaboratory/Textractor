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

package textractor.tools.io;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.*;
import textractor.datamodel.annotation.AnnotationSource;
import textractor.datamodel.annotation.DoubleTermAnnotation;
import textractor.datamodel.annotation.SingleTermAnnotation;
import textractor.learning.*;
import textractor.test.util.AssertFilesEqual;
import textractor.test.util.TextractorTestConstants;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * @author Fabien Campagne
 * Date: Jan 19, 2004
 * Time: 12:11:30 PM
 */
public class TestExportTrainingRecords extends TestCase {
    private DbManager dbm;
    private TextractorManager tm;

    public TestExportTrainingRecords(final String name) throws TextractorDatabaseException {
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

    public void testBagOfWordExporter() throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(tm.getInfo().getCaseSensitiveIndexBasename());
        final SingleBagOfWordExporter exporter =
                new MutationBagOfWordExporter(docmanager, true);
        // Setup a collection of two annotations for this test:
        final Vector<AnnotationSource> annotations =
            new Vector<AnnotationSource>();
        final SingleTermAnnotation ann1 = new SingleTermAnnotation(1, 1);
        final SingleTermAnnotation ann2 = new SingleTermAnnotation(1, 2);
        annotations.add(ann1);
        annotations.add(ann2);

        ann1.setCurrentText("Therefore the R79K substitution seems to have a greater effect on dGuo binding than on that of dAdo but dGK modification appears to produce a stimulatory conformational effect on the opposite subunit resembling the known unidirectional activation of dAK by either dGuo or dGTP ");
        ann1.getTerm().setStartPosition(2);
        ann2.setCurrentText("However two types of mutants were constructed one bears the R79K mutation on both the dAK and dGK subunits while the other has the R79K mutation on the dGK subunit only R79K dGK");
        ann2.getTerm().setStartPosition(10);

        final SingleBagOfWordFeatureCreationParameters parameters = new SingleBagOfWordFeatureCreationParameters();
        parameters.setWindowSize(1);
        exporter.firstPass(parameters, annotations);
        assertEquals("Three terms in the bag: the, substitution, mutation", 3, parameters.getTerms().length);
        List<String> terms = Arrays.asList(parameters.getTerms());
        assertTrue("term: the must be part of terms in bag: ", terms.contains("the"));
        assertTrue("term: substitution must be part of terms in bag: ", terms.contains("substitution"));
        assertTrue("term: mutation must be part of terms in bag: ", terms.contains("mutation"));

        assertFalse("term: Therefore must NOT be part of terms in bag: ", terms.contains("Therefore")); // test size of window is correctly interpreted.
        assertFalse("term: seems must NOT be part of terms in bag: ", terms.contains("seems")); // test size of window is correctly interpreted.
        assertFalse("term: on must NOT be part of terms in bag: ", terms.contains("on")); // test size of window is correctly interpreted.
        assertFalse("term: bears must NOT be part of terms in bag: ", terms.contains("bears")); // test size of window is correctly interpreted.
        assertFalse("term: R79K must NOT be part of terms in bag: ", terms.contains("R79K")); // test that the center word is correctly excluded.

        // now, do the same with a window on the left of the word:
        parameters.setWindowSize(1);
        parameters.setWindowLocation(SingleBagOfWordFeatureCreationParameters.LOCATION_LEFT_OF_WORD);
        exporter.firstPass(parameters, annotations);
        assertEquals("One term in the bag: the", 1, parameters.getTerms().length);
        terms = Arrays.asList(parameters.getTerms());
        assertTrue("term: the must be part of terms in bag: ", terms.contains("the"));
        assertTrue("term: substitution must NOT be part of terms in bag: ", !terms.contains("substitution"));
        assertTrue("term: mutation must NOT be part of terms in bag: ", !terms.contains("mutation"));

        assertFalse("term: Therefore must NOT be part of terms in bag: ", terms.contains("Therefore")); // test size of window is correctly interpreted.
        assertFalse("term: seems must NOT be part of terms in bag: ", terms.contains("seems")); // test size of window is correctly interpreted.
        assertFalse("term: on must NOT be part of terms in bag: ", terms.contains("on")); // test size of window is correctly interpreted.
        assertFalse("term: bears must NOT be part of terms in bag: ", terms.contains("bears")); // test size of window is correctly interpreted.
        assertFalse("term: R79K must NOT be part of terms in bag: ", terms.contains("R79K")); // test that the center word is correctly excluded.

        // OK, now, with a window on the right of the word:
        parameters.setWindowSize(1);
        parameters.setWindowLocation(SingleBagOfWordFeatureCreationParameters.LOCATION_RIGHT_OF_WORD);
        exporter.firstPass(parameters, annotations);
        assertEquals("Two terms in the bag: substitution, mutation", 2, parameters.getTerms().length);
        terms = Arrays.asList(parameters.getTerms());
        assertTrue("term: the must NOT be part of terms in bag: ", !terms.contains("the"));
        assertTrue("term: substitution must be part of terms in bag: ", terms.contains("substitution"));
        assertTrue("term: mutation must be part of terms in bag: ", terms.contains("mutation"));

        assertFalse("term: Therefore must NOT be part of terms in bag: ", terms.contains("Therefore")); // test size of window is correctly interpreted.
        assertFalse("term: seems must NOT be part of terms in bag: ", terms.contains("seems")); // test size of window is correctly interpreted.
        assertFalse("term: on must NOT be part of terms in bag: ", terms.contains("on")); // test size of window is correctly interpreted.
        assertFalse("term: bears must NOT be part of terms in bag: ", terms.contains("bears")); // test size of window is correctly interpreted.
        assertFalse("term: R79K must NOT be part of terms in bag: ", terms.contains("R79K")); // test that the center word is correctly excluded.
        docmanager.close();
    }

    public void testBagOfWordsSVMExport() throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final StringWriter writer = new StringWriter();
        final ExportTrainingRecords etr =
                new ExportTrainingRecords(dbm, TextractorTestConstants.MUTATION_BATCH_ID, writer, null);
        final int windowWidth = 3;
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(dbm.getTextractorManager().getInfo().getCaseSensitiveIndexBasename());

        etr.setExporter(new MutationBagOfWordExporter(docmanager, true));
        final SingleBagOfWordFeatureCreationParameters parameters = new SingleBagOfWordFeatureCreationParameters();
        parameters.setWindowSize(windowWidth);
        etr.export(parameters);

        writer.flush();
        writer.close();

        final String expectedResultFile = "data/testData/expected_featureTest2.txt";
        compareFeatures(writer, parameters, expectedResultFile);
        docmanager.close();
    }

    public void testBagOfWordsSplitSVMExport() throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final StringWriter training_writer = new StringWriter();
        final StringWriter test_writer = new StringWriter();
        final ExportTrainingRecords etr = new ExportTrainingRecords(dbm, TextractorTestConstants.MUTATION_BATCH_ID, training_writer, test_writer);
        final int windowWidth = 3;
        final DocumentIndexManager docmanager = new DocumentIndexManager(dbm.getTextractorManager().getInfo().getCaseSensitiveIndexBasename());

        etr.setExporter(new MutationBagOfWordExporter(docmanager, true));
        etr.setFold(2);   // fold = 2 must split 50%/50% in training and test sets.
        etr.setSeed(1);
        final SingleBagOfWordFeatureCreationParameters parameters = new SingleBagOfWordFeatureCreationParameters();
        parameters.setWindowSize(windowWidth);
        etr.export(parameters);

        training_writer.flush();
        training_writer.close();
        test_writer.close();
        test_writer.flush();

        String expectedResultFile = "data/testData/expected_featureTest0.txt";
        compareFeatures(training_writer, parameters, expectedResultFile);
        expectedResultFile = "data/testData/expected_featureTest1.txt";
        compareFeatures(test_writer, parameters, expectedResultFile);
        docmanager.close();
    }

    private void compareFeatures(final StringWriter stringWriter,
                                 final FeatureCreationParameters parameters,
                                 final String expectedResultFilename)
            throws IOException {
        final String testResultFilename = "test-results/featureTest.txt";
        final FileOutputStream fileoutstream = new FileOutputStream(testResultFilename);
        final Writer fileWriter = new OutputStreamWriter(fileoutstream, "UTF-8");
        extractTermFromParameter(stringWriter, fileWriter, parameters);

        final File expectedResultFile = new File(expectedResultFilename);
        final File testResultFile = new File(testResultFilename);
        AssertFilesEqual.assertEquals(expectedResultFile, testResultFile);
    }

    private void extractTermFromParameter(final StringWriter stringWriter,
                                          final Writer fileWriter,
                                          final FeatureCreationParameters parameters)
            throws IOException {
        final String[] records = stringWriter.getBuffer().toString().split("\n");

        for (final String record : records) {
            final String[] features = record.split(" ");
            for (int j = 1; j < features.length; j++) {
                final int feature = Integer.parseInt(features[j].split(":")[0]);
                fileWriter.write(parameters.getTerms()[feature - 1] + "|");
            }
            fileWriter.write("\n");
        }
        fileWriter.flush();
        fileWriter.close();
    }

    public void testProteinBagOfWordsSVMExportTrainingAndTest() throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final StringWriter training_writer = new StringWriter();
        final StringWriter test_writer = new StringWriter();
        final ExportTrainingRecords etr =
                new ExportTrainingRecords(dbm, TextractorTestConstants.MUTATION_BATCH_ID, training_writer, test_writer);
        final int windowWidth = 3;
        final SingleBagOfWordFeatureCreationParameters parameters =
                new SingleBagOfWordFeatureCreationParameters();
        parameters.setWindowSize(windowWidth);
        etr.setSeed(1);
        etr.setFold(2);
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(dbm.getTextractorManager().getInfo().getCaseSensitiveIndexBasename());
        etr.setExporter(new ProteinNameBagOfWordExporter(docmanager, true));
        etr.export(parameters);

        training_writer.flush();
        training_writer.close();
        test_writer.flush();
        test_writer.close();

        String expectedResultFile = "data/testData/expected_featureTest3.txt";
        compareFeatures(training_writer, parameters, expectedResultFile);
        expectedResultFile = "data/testData/expected_featureTest4.txt";
        compareFeatures(test_writer, parameters, expectedResultFile);
        docmanager.close();
    }

    public void testBagOfWordsSVMExportTest() throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final StringWriter test_writer = new StringWriter();
        final ExportTrainingRecords etr =
                new ExportTrainingRecords(dbm, TextractorTestConstants.MUTATION_BATCH_ID, null, test_writer);
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(dbm.getTextractorManager().getInfo().getCaseSensitiveIndexBasename());
        final FeatureCreationParameters parameters = tm.getParameterSetById(0);
        assertNotNull("Parameter set 0 must be found in the database.", parameters);

        parameters.updateIndex(docmanager);//todo
        etr.setParameterSet(0);
        etr.setWindowWidth(3);
        etr.setExporter(new MutationBagOfWordExporter(docmanager, true));
        etr.export(parameters);       // use mutation set - shorter output!

        test_writer.flush();
        test_writer.close();

        final String expectedResultFile = "data/testData/expected_featureTest5.txt";
        compareFeatures(test_writer, parameters, expectedResultFile);
        docmanager.close();
    }

    /**
     * <ol>
     * <li>create annotations for protein names. Must have several annotations
     * for the same protein name
     * <li>export unannotated annotations.
     * <li>import SVM results that have been produced with the exported traning
     * set.
     * </ol>
     */
    public void testBagOfWordsSVMResultsImport() throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final String first_text_fragment = "This Text is about protein bacteriorhodopsin and indicates that bacteriorhodopsin is a protein";
        final String second_text_fragment = "This Text is about gene Press and indicates that Press is not a protein.";
        final DocumentIndexManager docManager = new DocumentIndexManager(dbm.getTextractorManager().getInfo().getCaseSensitiveIndexBasename());

        final Article article = dbm.getTextractorManager().createArticle();
        article.setArticleNumber(dbm.getTextractorManager().getNextArticleNumber());
        article.setFilename("testBagOfWordsSVMResultsImport");
        article.setPmid(0);
        article.addTermOccurrence(new TermOccurrence("bacteriorhodopsin",
                docManager.extractTerms("bacteriorhodopsin", '_'), 2));
        article.addTermOccurrence(new TermOccurrence("press",
                docManager.extractTerms("press", '_'), 2));

        final Sentence sentence = new Sentence(article);
        sentence.setText("testBagOfWordsSVMResultsImport");
        sentence.setDocumentNumber(tm.getNextDocumentNumber());
        final int newBatchID = tm.getNextAnnotationBatchNumber();
        final SingleTermAnnotation annotation1 = tm.createSingleTermAnnotation(newBatchID);
        final SingleTermAnnotation annotation2 = tm.createSingleTermAnnotation(newBatchID);

        annotation1.setCurrentText(first_text_fragment);
        annotation1.getTerm().setText("bacteriorhodopsin", 5);

        annotation1.createIndexedTerms(docManager);

        annotation2.setCurrentText(first_text_fragment);
        annotation2.getTerm().setText("bacteriorhodopsin", 9);
        annotation2.createIndexedTerms(docManager);

        final SingleTermAnnotation annotation3 = tm.createSingleTermAnnotation(newBatchID);
        annotation3.setCurrentText(second_text_fragment);
        annotation3.getTerm().setText("press", 5);
        annotation3.createIndexedTerms(docManager);


        final SingleTermAnnotation annotation4 = tm.createSingleTermAnnotation(newBatchID);
        annotation4.setCurrentText(second_text_fragment);
        annotation4.getTerm().setText("press", 9);
        annotation4.createIndexedTerms(docManager);

        // link the annotations to the same sentence. We only care about the article to collect stats.
        annotation1.setSentence(sentence);
        annotation2.setSentence(sentence);
        annotation3.setSentence(sentence);
        annotation4.setSentence(sentence);

        // TODO - delete these after the test
        dbm.makePersistent(annotation1);
        dbm.makePersistent(annotation2);
        dbm.makePersistent(annotation3);
        dbm.makePersistent(annotation4);
        dbm.makePersistent(sentence);
        dbm.makePersistent(article);

        dbm.checkpointTxn();
        // export the training annotations.

        StringWriter writer = new StringWriter();
        final ExportTrainingRecords etr = new ExportTrainingRecords(dbm, newBatchID, writer, null);
        etr.setExporter(new ProteinNameBagOfWordExporter(docManager, false));
        etr.export(new SingleBagOfWordFeatureCreationParameters());
        writer.close();

        final String svm_results = "1.0564107\n" +
                "0.90242382\n" +
                "-1.262374\n" +
                "1.4117494\n";

        final StringReader reader = new StringReader(svm_results);

        final TextractorManager tm = dbm.getTextractorManager();
        final Collection annotations =
                tm.getUnannotatedAnnotationsInBatch(TextractorTestConstants.MUTATION_BATCH_ID);
        final ImportSVMResultsByAnnotation importer = new ImportSVMResultsByAnnotation(dbm, newBatchID);
        importer.setAnnotations(annotations);
        importer.importSVM(reader);
        writer = new StringWriter();
        importer.printStatsHeader(writer);
        importer.printStatsForArticle(docManager, article, writer);

        writer.flush();

        final String test_expected = "Term\tRatio\tDistance\tArticle\n" +
                "bacteriorhodopsin\t1.0\t1.0564107\t0\n" +
                "bacteriorhodopsin\t1.0\t0.90242382\t0\n" +
                "press\t0.5\t-1.262374\t0\n" +
                "press\t0.5\t1.4117494\t0\n";

        assertEquals(test_expected, writer.getBuffer().toString());
        docManager.close();
    }

    public void testDoubleTermExporter() throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(tm.getInfo().getCaseSensitiveIndexBasename());
        final DoubleBagOfWordExporter exporter =
                new PhosphorylateBagOfWordExporter(docmanager);
        // Setup a collection of two annotations for this test:
        final Vector<AnnotationSource> annotations =
            new Vector<AnnotationSource>();
        final DoubleTermAnnotation ann1 = new DoubleTermAnnotation(2, 1);
        final DoubleTermAnnotation ann2 = new DoubleTermAnnotation(2, 2);
        annotations.add(ann1);
        annotations.add(ann2);

        ann1.setCurrentText("Therefore the R79K substitution seems to have a greater effect on dGuo binding than on that of dAdo but dGK modification appears to produce a stimulatory conformational effect on the opposite subunit resembling the known unidirectional activation of dAK by either dGuo or dGTP ");
        ann1.getTermA().setStartPosition(2);
        ann1.getTermB().setStartPosition(11);
        ann2.setCurrentText("However two types of mutants were constructed one bears the R79K mutation on both the dAK and dGK subunits while the other has the R79K mutation on the dGK subunit only R79K dGK");
        ann2.getTermA().setStartPosition(10);
        ann2.getTermB().setStartPosition(17);

        final DoubleBagOfWordFeatureCreationParameters parameters = new DoubleBagOfWordFeatureCreationParameters();
        parameters.setWindowSize(1);
        exporter.firstPass(parameters, annotations);
        assertEquals("Seven terms in the bag: the, substitution, mutation, on, binding, and, subunits", 7, parameters.getTerms().length);

        final List<String> terms = Arrays.asList(parameters.getTerms());
        assertTrue("term: the must be part of terms in bag: ", terms.contains("the"));
        assertTrue("term: substitution must be part of terms in bag: ", terms.contains("substitution"));
        assertTrue("term: mutation must be part of terms in bag: ", terms.contains("mutation"));
        assertTrue("term: mutation must be part of terms in bag: ", terms.contains("on"));
        assertTrue("term: mutation must be part of terms in bag: ", terms.contains("binding"));
        assertTrue("term: mutation must be part of terms in bag: ", terms.contains("and"));
        assertTrue("term: subunits must be part of terms in bag: ", terms.contains("subunits"));

        assertFalse("term: Therefore must NOT be part of terms in bag: ", terms.contains("Therefore")); // test size of window is correctly interpreted.
        assertFalse("term: seems must NOT be part of terms in bag: ", terms.contains("seems")); // test size of window is correctly interpreted.
        assertFalse("term: bears must NOT be part of terms in bag: ", terms.contains("bears")); // test size of window is correctly interpreted.
        assertFalse("term: R79K must NOT be part of terms in bag: ", terms.contains("R79K")); // test that the center word is correctly excluded.
        assertFalse("term: on must NOT be part of terms in bag: ", terms.contains("effect")); // test size of window is correctly interpreted.
        assertFalse("term: on must NOT be part of terms in bag: ", terms.contains("than")); // test size of window is correctly interpreted.
        assertFalse("term: on must NOT be part of terms in bag: ", terms.contains("dAK")); // test size of window is correctly interpreted.
        assertFalse("term: on must NOT be part of terms in bag: ", terms.contains("while")); // test size of window is correctly interpreted.
        docmanager.close();
    }

    public void testDoubleTermExporterOverlap() throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(tm.getInfo().getCaseSensitiveIndexBasename());
        final DoubleBagOfWordExporter exporter = new PhosphorylateBagOfWordExporter(docmanager);
        // Setup a collection of two annotations for this test:
        final Vector<AnnotationSource> annotations = new Vector<AnnotationSource>();
        final DoubleTermAnnotation ann1 = new DoubleTermAnnotation(3, 1);
        final DoubleTermAnnotation ann2 = new DoubleTermAnnotation(3, 2);
        annotations.add(ann1);
        annotations.add(ann2);

        ann1.setCurrentText("Therefore the R79K substitution on dGuo binding than on that of dAdo but dGK modification appears to produce a stimulatory conformational effect on the opposite subunit resembling the known unidirectional activation of dAK by either dGuo or dGTP ");
        ann1.getTermA().setStartPosition(2);
        ann1.getTermB().setStartPosition(5);
        ann2.setCurrentText("on both the dAK and dGK subunits while the other has the R79K mutation on the dGK subunit only R79K dGK");
        ann2.getTermA().setStartPosition(3);
        ann2.getTermB().setStartPosition(5);

        final DoubleBagOfWordFeatureCreationParameters parameters =
                new DoubleBagOfWordFeatureCreationParameters();
        parameters.setWindowSize(4);
        exporter.firstPass(parameters, annotations);

        final List<String> terms = Arrays.asList(parameters.getTerms());
        assertTrue("term must be part of terms in bag: ", terms.contains("therefore"));
        assertTrue("term must be part of terms in bag: ", terms.contains("the"));
        assertTrue("term must be part of terms in bag: ", terms.contains("substitution"));
        assertTrue("term must be part of terms in bag: ", terms.contains("on"));
        assertTrue("term must be part of terms in bag: ", terms.contains("binding"));
        assertTrue("term must be part of terms in bag: ", terms.contains("than"));
        assertTrue("term must be part of terms in bag: ", terms.contains("on"));
        assertTrue("term must be part of terms in bag: ", terms.contains("that"));
        assertTrue("term must be part of terms in bag: ", terms.contains("both"));
        assertTrue("term must be part of terms in bag: ", terms.contains("and"));
        assertTrue("term must be part of terms in bag: ", terms.contains("subunits"));
        assertTrue("term must be part of terms in bag: ", terms.contains("while"));
        assertTrue("term must be part of terms in bag: ", terms.contains("other"));

        // the following test will fail because the current source code has a bug.
        // TODO: fix DoubleBagOfWordExporter to correctly handle overlap in the two single bag of word windows
        assertFalse("term must NOT be part of terms in bag: ", terms.contains("R79K")); // test size of window is correctly interpreted.
        assertFalse("term must NOT be part of terms in bag: ", terms.contains("dGuo")); // test size of window is correctly interpreted.
        assertFalse("term must NOT be part of terms in bag: ", terms.contains("dAK")); // test size of window is correctly interpreted.
        assertFalse("term must NOT be part of terms in bag: ", terms.contains("dGK")); // test size of window is correctly interpreted.

        // the total would be thirteen if the two bag of words used the same term space, but this is not the case.
        // what is the correct number of terms?
        assertEquals("Exactly 20 terms must be in the bag",
                20, parameters.getTerms().length);
        docmanager.close();
    }

    public void testExcludedPositions() throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager = new DocumentIndexManager(tm.getInfo().getCaseSensitiveIndexBasename());
        final DoubleBagOfWordExporter exporter = new PhosphorylateBagOfWordExporter(docmanager);
        // Setup a collection of two annotations for this test:
        final DoubleTermAnnotation ann1 = new DoubleTermAnnotation(4, 1);
        ann1.setCurrentText("Therefore the R79K substitution seems to have a greater effect on dGuo binding than on that of dAdo but dGK modification appears to produce a stimulatory conformational effect on the opposite subunit resembling the known unidirectional activation of dAK by either dGuo or dGTP ");
        ann1.getTermA().setText("the R79K substitution", 1);
        ann1.getTermB().setText("have a greater effect", 6);
        int[] excludedPositions = exporter.excludedPositions(ann1);
        assertNotNull(excludedPositions);
        assertEquals("There must be 7 positions excluded from the export.", 7, excludedPositions.length);
        IntArrayList excludedList = new IntArrayList(excludedPositions);
        assertTrue("Position 1 must be excluded.", excludedList.contains(1));
        assertTrue("Position 2 must be excluded.", excludedList.contains(2));
        assertTrue("Position 3 must be excluded.", excludedList.contains(3));
        assertTrue("Position 6 must be excluded.", excludedList.contains(6));
        assertTrue("Position 7 must be excluded.", excludedList.contains(7));
        assertTrue("Position 8 must be excluded.", excludedList.contains(8));
        assertTrue("Position 9 must be excluded.", excludedList.contains(9));

        assertFalse("Position 0 must not be excluded.", excludedList.contains(0));
        assertFalse("Position 10 must not be excluded.", excludedList.contains(10));
        assertFalse("Position 4 must not be excluded.", excludedList.contains(4));
        assertFalse("Position 5 must not be excluded.", excludedList.contains(5));

        final ExcludeWindowCenter ewc = new ExcludeWindowCenter();
        excludedPositions = ewc.excludedPositions(ann1);
        assertNotNull(excludedPositions);
        assertEquals("There must be 3 positions excluded from the export.", 3, excludedPositions.length);
        excludedList = new IntArrayList(excludedPositions);
        assertTrue("Position 1 must be excluded.", excludedList.contains(1));
        assertTrue("Position 2 must be excluded.", excludedList.contains(2));
        assertTrue("Position 3 must be excluded.", excludedList.contains(3));

        assertFalse("Position 6 must not be excluded.", excludedList.contains(6));
        assertFalse("Position 7 must not  be excluded.", excludedList.contains(7));
        assertFalse("Position 8 must not be excluded.", excludedList.contains(8));
        assertFalse("Position 9 must not be excluded.", excludedList.contains(9));
        assertFalse("Position 0 must not be excluded.", excludedList.contains(0));
        assertFalse("Position 10 must not be excluded.", excludedList.contains(10));
        docmanager.close();
    }
}
