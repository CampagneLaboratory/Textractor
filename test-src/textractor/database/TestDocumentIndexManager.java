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

import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.Interval;
import it.unimi.dsi.mg4j.search.IntervalIterator;
import it.unimi.dsi.mg4j.util.MutableString;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.datamodel.Sentence;
import textractor.datamodel.TermOccurrence;
import textractor.mg4j.index.TermIterator;
import textractor.tools.BuildDocumentIndexFromTextDocuments;
import textractor.tools.DocumentQueryResult;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Validates basic functionality of the Document Index is working properly.
 */
public final class TestDocumentIndexManager extends TestCase {
    /**
     * Used to log informational and debug messsages.
     */
    private static final Log LOG =
            LogFactory.getLog(TestDocumentIndexManager.class);
    private DbManager dbm;
    private DocumentIndexManager docmanager;
    private TextractorManager tm;


    @Override
    protected void setUp() throws TextractorDatabaseException,
            IOException, ConfigurationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException,
            ClassNotFoundException, InstantiationException, URISyntaxException {
        dbm = new DbManager();
        dbm.beginTxn();
        docmanager = new DocumentIndexManager("index/junit-test-basename");
        tm = dbm.getTextractorManager();
    }

    @Override
    protected void tearDown() {
        dbm.commitTxn();
    }

    /**
     * Validates that the index manager can properly delete index files.
     * TODO: Note that the test is not currently working properly.
     *
     * @throws TextractorDatabaseException
     * @throws IOException
     * @throws ConfigurationException
     * @see <a href="http://icbtools.med.cornell.edu/mantis/view.php?id=1048">Mantis issue #1048</a>
     */
    public void testDeleteIndex() throws Exception {
        final String indexDirectory = "index";
        final String indexName = "for-delete-test";
        // MG4G 1.1 does not like File.separator in the basename on PC.
        final String basename = indexDirectory + "/" + indexName;

        File[] files;
        final File dir = new File(indexDirectory);
        // get all index files the specified directory
        final FilenameFilter filter = new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.startsWith(indexName);
            }
        };

        // make sure that there are no index files present to begin with
        files = dir.listFiles(filter);
        assertEquals("Test precondition failed.  Please delete all the files"
                + " beginning with '" + indexName + "' in "
                + dir.getAbsolutePath() + "before running this test.",
                0, files.length);

        BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add("Hello, this is the text to index, first document");
        textCollection.add(
                new StringBuffer("second document, in a string buffer"));
        textCollection.add(
                new MutableString("third document, a mutable string"));
        indexBuilder.index(textCollection);

        // now make sure that the index process created at least on index file
        files = dir.listFiles(filter);
        assertTrue("No index files beginning with '" + indexName
                + "' were created in " + dir.getAbsolutePath(),
                files.length > 0);

        // force a garbage collection to ensure that nothing is holding on
        // to the files that should be deleted
        textCollection = null;
        indexBuilder = null;
        files = null;
        System.runFinalization();
        System.gc();

        // create a second document manager that should delete the index files
        final DocumentIndexManager docmanager2 = new DocumentIndexManager(basename);
        docmanager2.close();
        docmanager2.removeIndexFiles();

        // check the files that were created during the test are now gone
        files = dir.listFiles(filter);
        LOG.error("part of test disabled - see issue #1048");
//      assertEquals("Files beginning with '" + indexName + "' still exist in "
//              + dir.getAbsolutePath(), 0, files.length);
    }

    /**
     * Validate that empty queries throw exceptions.
     */
    public void testEmptyQuery() throws IOException {
        // do the query:
        final List<String> empty = new ArrayList<String>();
        try {
            docmanager.queryAnd(empty);
        } catch (final RuntimeException e) {
            // pass.
            return;
        }

        fail("Empty queries should throw exception.");
    }

    /**
     * Test that the protein keyword is found in the boot data.
     */
    public void testQueryProtein() throws IOException {
        // do the query:
        final String keyword = "protein";
        final int[] documents = docmanager.query(keyword);
        assertNotNull("keyword(s) matched no documents", documents);

        // assert that each document retrieved contains the keyword:
        for (final int document : documents) {
            final MutableString docText = tm.getSentence(document).getSpaceDelimitedProcessedTerms(docmanager);
            assertNotSame("the keyword \"" + keyword + "\" is not contained in a sentence returned by the document index manager.",
                    -1, docText.indexOf(keyword));
        }

        Arrays.sort(documents);

        // assert that each document not retrieved does not contain the keyword:
        final Iterator<Sentence> it = tm.getSentenceIterator();
        while (it.hasNext()) {
            final Sentence sentence = it.next();
            if (Arrays.binarySearch(documents, (int) sentence.getDocumentNumber()) < 0) {
                // the key was not found => the document is not in the documents array
                final MutableString spaceDelimitedTerms = sentence.getSpaceDelimitedProcessedTerms(docmanager);
                if (spaceDelimitedTerms != null) {
                    final StringTokenizer st = new StringTokenizer(spaceDelimitedTerms.toString(), " ");
                    String token;
                    while (st.hasMoreTokens()) {
                        token = st.nextToken();
                        if (token.equals(keyword)) {
                            assertFalse("This document (" + sentence.getDocumentNumber() + ") was not retrieved but contains the keyword.. " +
                                    "[processed terms: " + sentence.getSpaceDelimitedProcessedTerms(docmanager) + " ]+" +
                                    "[unprocessed terms: " + sentence.getText() + " ]",
                                    token.equals(keyword));
                        }
                    }
                }
            }
        }
    }

    public void testQueryAnd() throws IOException {
        // do the query:
        final List<String> keywords = new ArrayList<String>();
        keywords.add("protein");
        keywords.add("ubiquitin");
        int[] documents = docmanager.queryAnd(keywords);

        assertNotNull("keyword(s) matched no documents", documents);

        // assert that each document retrieved contains the keyword:
        for (final int document : documents) {
            final String doctext = tm.getSentence(document).getText().toLowerCase();
            for (final String kw : keywords) {
                if (doctext.indexOf(kw) == -1) {
                    fail("keyword " + kw + " not contained in sentence returned by document index.");
                }
            }
        }
        // assert that each document not retrieved does not contain the keyword:
        final Iterator<Sentence> it = tm.getSentenceIterator();
        while (it.hasNext()) {
            int count = 0;   // number of required keywords that this sentence has

            final Sentence sentence = it.next();
            if (Arrays.binarySearch(documents, (int) sentence.getDocumentNumber()) < 0) {
                final List<String> keywordsCopy = new ArrayList<String>(keywords.size());
                keywordsCopy.addAll(keywords);

                // the key was not found => the document is not in the documents  array
                final String[] terms = sentence.getSpaceDelimitedProcessedTerms(docmanager).toString().split(" ");
                for (final String term : terms) {
                    if (keywordsCopy.contains(term)) {
                        keywordsCopy.remove(term);
                        count++;
                    }
                }
            }
            assertFalse("This document (" + sentence.getDocumentNumber() + ") was not retrieved but contains the keywords.. " + sentence.getText(), count == keywords.size());
        }

        // test queryAnd when the collection of terms contains a term that is
        // not in the document index.
        keywords.add("Thistermisnotintheindex");
        documents = docmanager.queryAnd(keywords);
        assertNotNull("result set must not be null", documents);
        assertEquals("empty result set must be returned", 0, documents.length);
    }

    public void testQueryOr() throws IOException {
        // do the query:
        final List<String> keywords = new ArrayList<String>();
        keywords.add("protein");
        keywords.add("ubiquitin");
        final int[] documents = docmanager.queryOr(keywords);

        assertNotNull("keyword(s) matched no documents", documents);

        // assert that each document retrieved contains either on of the keywords:
        boolean hasOneKeyword = false;
        for (final int document : documents) {
            final String doctext = tm.getSentence(document).getText();
            for (final String keyword : keywords) {
                if (doctext.indexOf(keyword) == -1) {
                    hasOneKeyword = true;
                }
            }
        }
        assertTrue("The sentence returned by document index contains none of the keywords.", hasOneKeyword);
        // assert that each document not retrieved does not contain the keyword:
        final Iterator<Sentence> it = tm.getSentenceIterator();
        while (it.hasNext()) {
            int count = 0;   // number of required keywords that this sentence has

            final Sentence sentence = it.next();
            if (Arrays.binarySearch(documents, (int) sentence.getDocumentNumber()) < 0) {
                // the key was not found => the document is not in the documents  array
                final String[] terms = sentence.getSpaceDelimitedProcessedTerms(docmanager).toString().split("\\s");
                for (final String term : terms) {
                    if (keywords.contains(term)) {
                        count++;
                    }
                }
            }
            assertFalse("This document (" + sentence.getDocumentNumber() + ") was not retrieved but contains the keywords.. " + sentence.getSpaceDelimitedProcessedTerms(docmanager), count >= 1);
        }
    }

    public void testUnion() {
        final int[] array1 = {1, 2, 3, 6, 8};
        final int[] array2 = {2, 4, 6, 7};
        // 1 2 3 4 6 7 8
        final int[] result = docmanager.union(array1, array2);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertEquals(4, result[3]);
        assertEquals(6, result[4]);
        assertEquals(7, result[5]);
        assertEquals(8, result[6]);
        assertEquals("Size of result (# elements in intersection) does not match.", 7, result.length);
    }

    public void testStemming() throws TextractorDatabaseException, ConfigurationException, IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        // do the query:
        final String keyword = "protein";
        final DocumentIndexManager stemmed_docmanager =
                new DocumentIndexManager(dbm.getTextractorManager().getInfo().getStemmedIndexBasename());
        final int[] documents = docmanager.query(keyword);
        final int[] documents_stemmed_query = stemmed_docmanager.query(keyword);
        assertNotNull("keyword(s) matched no documents", documents);
        assertNotNull("keyword(s) matched no documents in stemmed index", documents_stemmed_query);
        assertTrue("protein keyword matches more results in stemmed index", documents_stemmed_query.length > documents.length);

        assertTrue("Number of terms in stemmed index is less than in non stemmed index.",
                docmanager.getNumberOfTerms() > stemmed_docmanager.getNumberOfTerms());
        stemmed_docmanager.close();
    }

    public void testPositions() throws IOException {
        // do the query:
        final String keyword = "protein";
        final TermDocumentPositions positions = new TermDocumentPositions(keyword);
        final int[] documents = docmanager.query(keyword, positions);

        assertNotNull("keyword(s) matched no documents", documents);

        int document;
        for (int i = 0; i < documents.length; ++i) {
            int matchCount = 0;
            document = documents[i];
            assertNotNull("positions are not null", positions.getPositions(i));
            final int[] positionsInCurrentDocument = positions.getPositions(i);
            assertEquals(positionsInCurrentDocument.length, positions.getAllOccurrenceCounts()[i]);

            final Sentence sentence = tm.getSentence(document);
            final String docText = sentence.getSpaceDelimitedProcessedTerms(docmanager).toString();
            final String[] st = docText.split("[\\s]+");
            for (int position = 0; position < st.length; position++) {
                if (st[position].equals(keyword)) {
                    // check that the position of this keyword has been correctly reported in positions:
                    if (matchCount != Arrays.binarySearch(positionsInCurrentDocument, position)) {
                        System.err.println(matchCount + " " + Arrays.binarySearch(positionsInCurrentDocument, position));
                        System.err.println("mismatch at position " + position + " for sentence: " + tm.getSentence(document).getSpaceDelimitedProcessedTerms(docmanager) +
                                "\n word is : " + st[position]);
                        System.err.println("mismatch at position " + position + " for sentence: " + tm.getSentence(document).getText() +
                                "\n word is : " + st[position]);
                        final StringTokenizer st2 = new StringTokenizer(tm.getSentence(document).getSpaceDelimitedProcessedTerms(docmanager).toString(), " ");
                        int word = 0;
                        while (st2.hasMoreTokens()) {
                            LOG.debug(new StringBuffer("word ").append(word++).append(": ").append(st2.nextToken()).toString());
                        }
                        for (int pi = 0; pi < positionsInCurrentDocument.length; pi++) {
                            LOG.debug(pi + " " + positionsInCurrentDocument[pi]);
                        }
                        fail();
                    }
                    assertEquals("the position of the keyword (" + position + ") must be found at index count (" +
                            Arrays.binarySearch(positionsInCurrentDocument, position) + ") in the position index.",
                            matchCount, Arrays.binarySearch(positionsInCurrentDocument, position));
                    matchCount++;
                }

            }

            assertEquals("keyword occurs no more, no less than needed in positions index.",
                    matchCount, positionsInCurrentDocument.length);
        }
    }

    public void testSortTerms() {
        final TermOccurrence to1 = new TermOccurrence("hi", new int[]{-1}, 9);
        final TermOccurrence to2 = new TermOccurrence("piggy", new int[]{-1}, 6);
        final TermOccurrence to3 = new TermOccurrence("egg", new int[]{-1}, 10);
        final TermOccurrence to4 = new TermOccurrence("boo", new int[]{-1}, 2);
        final Collection<TermOccurrence> c = new ArrayList<TermOccurrence>();
        c.add(to1);
        c.add(to2);
        c.add(to3);
        c.add(to4);
        final ArrayList<TermOccurrence> a = new ArrayList<TermOccurrence>(c);
        Collections.sort(a);
        assertEquals(0, a.indexOf(to3));
        assertEquals(1, a.indexOf(to1));
        assertEquals(2, a.indexOf(to2));
        assertEquals(3, a.indexOf(to4));
    }

    public void testQueryAndExactOrder() throws IOException {
        // do the query:
        final String[] keywords = {"double", "amino", "acid"};

        final DocumentIterator documentIterator = docmanager.queryAndExactOrderMg4jNative(keywords);
        final int[] documents = docmanager.iteratorToInts(documentIterator);
        documentIterator.dispose();
        assertNotNull("keyword(s) matched no documents", documents);

        // assert that each document retrieved contains at least one of the keywords:
        boolean hasOneKeyword = false;
        final int[] positions = new int[keywords.length];
        int currentPosition = 0;
        for (final int document : documents) {
            final String doctext = tm.getSentence(document).getText();
            for (final String kw : keywords) {
                if (doctext.contains(kw)) {
                    final String[] words = doctext.split("\\s");
                    for (int j = 0; j < words.length; j++) {
                        if (words[j].equals(kw)) {
                            positions[currentPosition++] = j;
                        }
                    }
                    hasOneKeyword = true;
                }
                if (!hasOneKeyword) {
                    break;
                }

            }
        }
        assertTrue("The sentence returned by document index contains none of the keywords.", hasOneKeyword);
        for (int i = 0; i < positions.length - 1; i++) {
            assertEquals("Positions must be consecutive for exact order",
                    (positions[i] + 1), positions[i + 1]);
        }
    }

    public void testLastWordIndexed() throws Exception {
        final String basename = "index/with-parentheses";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add("Hello, this is the (text to index), XYA XYB");
        textCollection.add(new StringBuffer("second document,() in a string ZZZA."));
        textCollection.add(new MutableString("(third document, a mutable string.) "));
        textCollection.add(new MutableString("DNA Ring Closure (Circularization) Assay for DNA Bending"));
        textCollection.add("However, the observed effects of k opioid receptor agonists on myocardial cytosolic Ca and pH homeostasis were largely attributable to the capability of these opioids to increase the formation of inositol 1,4,5-trisphosphate and inositol 1,3,4,5-tetraphosphate (8) and to elicit a protein kinase C-dependent stimulation of the Na/H antiporter(5) , indicating that myocardial opioid receptors are coupled to phosphoinositide turnover and protein kinase C (PKC).");
        indexBuilder.index(textCollection);

        final DocumentIndexManager docmanager2 =
                new DocumentIndexManager(basename);
        int[] result = docmanager2.query("XYA");
        assertEquals(1, result.length);
        assertTrue(result[0] == 0);
        result = docmanager2.query("XYB");
        assertEquals(1, result.length);
        assertTrue(result[0] == 0);
        result = docmanager2.query("ZZZA");
        assertEquals(1, result.length);
        assertEquals(1, result[0]);
        docmanager2.close();
    }

    public void testParenthesesIndexing() throws Exception {
        final String basename = "index/with-parentheses";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add("Hello, this is the (text to index), first document");
        textCollection.add(new StringBuffer("second document,() in a string buffer"));

        textCollection.add(new MutableString("(third document a mutable string.) "));

        textCollection.add(new MutableString("DNA Ring Closure (Circularization) Assay for DNA Bending"));
        textCollection.add("However, the observed effects of k opioid receptor agonists on myocardial cytosolic Ca and pH homeostasis were largely attributable to the capability of these opioids to increase the formation of inositol 1,4,5-trisphosphate and inositol 1,3,4,5-tetraphosphate (8) and to elicit a protein kinase C-dependent stimulation of the Na/H antiporter(5) , indicating that myocardial opioid receptors are coupled to phosphoinositide turnover and protein kinase C (PKC).");
        indexBuilder.index(textCollection);
        final DocumentIndexManager docmanager2 = new DocumentIndexManager(basename);
        assertTrue("Didn't find \"(\" in index",
                docmanager2.findTermIndex("(") != DocumentIndexManager.NO_SUCH_TERM);
        assertTrue("Didn't find \")\" in index",
                docmanager2.findTermIndex(")") != DocumentIndexManager.NO_SUCH_TERM);

        String[] keywords = new String[]{"(", "text", "to", "index", ")"};
        TermDocumentPositions termDocumentPositions =
                docmanager2.queryAndExactOrder(keywords);
        assertEquals(1, termDocumentPositions.getDocuments().length);
        assertTrue(termDocumentPositions.getDocuments()[0] == 0);
        keywords = new String[]{"(", ")"};
        termDocumentPositions = docmanager2.queryAndExactOrder(keywords);
        assertEquals(1, termDocumentPositions.getDocuments().length);
        assertTrue(termDocumentPositions.getDocuments()[0] == 1);
        keywords = new String[]{"(", "third", "document", "a", "mutable", "string", ")"};
        termDocumentPositions = docmanager2.queryAndExactOrder(keywords);
        assertEquals(termDocumentPositions.getDocuments().length, 1);
        assertEquals(2, termDocumentPositions.getDocuments()[0]);

        keywords = new String[]{"(", "circularization", ")"};
        termDocumentPositions = docmanager2.queryAndExactOrder(keywords);
        assertEquals(1, termDocumentPositions.getDocuments().length);
        assertEquals(3, termDocumentPositions.getDocuments()[0]);

        keywords = new String[]{"(", "PKC", ")"};
        termDocumentPositions = docmanager2.queryAndExactOrder(keywords);
        assertEquals(1, termDocumentPositions.getDocuments().length);
        final int[] positions = termDocumentPositions.getPositions(0);
        assertEquals(1, positions.length);
        assertEquals(81, positions[0]);    // what is the position?
        docmanager2.close();
    }

    public void testQueryResult() throws Exception {
        final String basename = "index/with-parentheses";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add("Hello, this is the (text to index), first document");
        textCollection.add(new StringBuffer("second document,() in a string buffer"));
        textCollection.add(new MutableString("(third document, a mutable string.) "));
        textCollection.add(new MutableString("DNA Ring Closure (Circularization) Assay for DNA Bending"));
        textCollection.add("However, the observed effects of k opioid receptor agonists on myocardial cytosolic Ca and pH homeostasis were largely attributable to the capability of these opioids to increase the formation of inositol 1,4,5-trisphosphate and inositol 1,3,4,5-tetraphosphate (8) and to elicit a protein kinase C-dependent stimulation of the Na/H antiporter(5) , indicating that myocardial opioid receptors are coupled to phosphoinositide turnover and protein kinase C (PKC).");
        indexBuilder.index(textCollection);

        final DocumentIndexManager docmanager2 = new DocumentIndexManager(basename);
        assertTrue("Didn't find \"(\" in index",
                docmanager2.findTermIndex("(") != DocumentIndexManager.NO_SUCH_TERM);
        assertTrue("Didn't find \")\" in index",
                docmanager2.findTermIndex(")") != DocumentIndexManager.NO_SUCH_TERM);
        final String[] keywords = new String[]{"(", "text", "to", "index", ")"};
        DocumentIterator result = docmanager2.queryAndExactOrderMg4jNative(keywords);
        final DocumentQueryResult resultCache = new DocumentQueryResult(result.indices());

        resultCache.populateFrom(result);
        result.dispose();

        DocumentIterator docIterator = resultCache.getDocumentIterator();
        assertTrue(docIterator.hasNext());
        assertEquals(0, docIterator.nextDocument());
        final IntervalIterator intervalIterator = docIterator.intervalIterator();
        assertTrue(intervalIterator.hasNext());
        final Interval interval = intervalIterator.next();
        assertNotNull(interval);
        assertEquals(5, interval.left);
        assertEquals(9, interval.right);
        assertFalse(intervalIterator.hasNext());
        assertFalse(docIterator.hasNext());
        docIterator.dispose();

        result = docmanager2.extendOnLeft(resultCache, "the");
        assertTrue(result.hasNext());
        assertEquals(0, result.nextDocument());
        result.dispose();

        // test document skips:
        final DocumentQueryResult skipResult =
                new DocumentQueryResult(result.indices());
        int i = 0;
        skipResult.addMatch(i++);  // skip
        skipResult.addMatch(i++);  // skip
        skipResult.addMatch(i++);  // doc=2
        skipResult.addMatch(i++);  // doc=3
        skipResult.addMatch(i++);  // skip
        skipResult.addMatch(i++);  // skip
        skipResult.addMatch(i++);  // doc=6
        skipResult.addMatch(i++);  // doc=7
        docIterator = skipResult.getDocumentIterator();
        assertEquals(2, docIterator.skip(2));
        assertTrue(docIterator.hasNext());
        assertEquals(2, docIterator.nextDocument());
        assertTrue(docIterator.hasNext());
        assertEquals(6, docIterator.skipTo(6));
        assertTrue(docIterator.hasNext());
        assertEquals(7, docIterator.nextDocument());
        assertFalse(docIterator.hasNext());
        assertEquals(Integer.MAX_VALUE, docIterator.skipTo(11));
        docIterator.dispose();
        docmanager2.close();

    }

    public void testSplitTextMethods() throws Exception {
        final String basename = "index/split-test";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add("Hello, this is the text to index, first document");
        textCollection.add(new StringBuffer("second document, in a string buffer"));
        textCollection.add(new MutableString("third document, a mutable string"));
        indexBuilder.index(textCollection);
        final DocumentIndexManager docmanager2 = new DocumentIndexManager(basename);
        final MutableString result = new MutableString();
        docmanager2.splitText("Hello, this is the text to index, first document", result);
        final String[] tokens = result.toString().split(" ");
        final String[] expectedTokens = {
                "Hello",
                ",",
                "this", "is", "the", "text",
                "to", "index", ",", "first", "document"
        };

        assertEquals(expectedTokens.length, tokens.length);
        for (int i = 0; i < tokens.length; ++i) {
            assertEquals(expectedTokens[i], tokens[i]);
            i++;
        }
        docmanager2.close();
    }

    public void testExtractTerms() throws Exception {
        final String basename = "index/extract-test";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);
        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add("Hello, this is the text to index, first document");
        textCollection.add(new StringBuffer("second document, in a string buffer"));
        textCollection.add(new MutableString("third document, a mutable string"));
        indexBuilder.index(textCollection);
        final DocumentIndexManager docmanager2 = new DocumentIndexManager(basename);

        final int[] terms = docmanager2.extractTerms("Hello, this is the text to index, first document");

        final String[] expectedTokens = {
                "Hello",
                ",",
                "this", "is", "the", "text",
                "to", "index", ",", "first", "document"
        };

        assertEquals(expectedTokens.length, terms.length);
        for (int i = 0; i < terms.length; ++i) {
            final MutableString term = new MutableString(expectedTokens[i]);
            docmanager2.processTerm(term);
            assertEquals(docmanager2.findTermIndex(term.toString()), terms[i]);
            i++;
        }

        final int[] termsMutableString = docmanager2.extractTerms(new MutableString("Hello, this is the text to index, first document"));
        assertEquals(expectedTokens.length, terms.length);
        for (int i = 0; i < termsMutableString.length; ++i) {
            final MutableString term = new MutableString(expectedTokens[i]);
            docmanager2.processTerm(term);
            assertEquals(docmanager2.findTermIndex(term.toString()), termsMutableString[i]);
            i++;
        }
        docmanager2.close();
    }

    public void testExtractTermsInternalChar() throws Exception {
        final String basename = "index/extract-internal-test";
        final BuildDocumentIndexFromTextDocuments indexBuilder = new BuildDocumentIndexFromTextDocuments(basename);
        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add("Hello, this is the text to index, first document");
        textCollection.add(new StringBuffer("second document, in a string buffer"));
        textCollection.add(new MutableString("third document, a mutable string"));
        indexBuilder.index(textCollection);
        final DocumentIndexManager docmanager2 =
                new DocumentIndexManager(basename);

        final int[] terms = docmanager2.extractTerms("Hello, this_is the text to_index, first document", '_');
        final String[] expectedTokens = {
                "Hello",
                ",",
                "this", "is", "the", "text",
                "to", "index", ",", "first", "document"
        };

        assertEquals(expectedTokens.length, terms.length);
        for (int i = 0; i < terms.length; ++i) {
            final MutableString term = new MutableString(expectedTokens[i]);
            docmanager2.processTerm(term);
            assertEquals(docmanager2.findTermIndex(term.toString()), terms[i]);
            i++;
        }
        docmanager2.close();
    }

    public void testGetTerms() throws Exception {
        final String basename = "index/get-terms-test";

        // collect the tokens as lower case so we can compare
        final Set<String> tokens = new HashSet<String>();
        tokens.add(",");

        final String document1 =
                "Hello, this is the text to index, first document";
        collectTokens(tokens, document1.toLowerCase());

        final String document2 = "second document, in a string buffer";
        collectTokens(tokens, document2.toLowerCase());

        final String document3 = "third document, a mutable string";
        collectTokens(tokens, document3.toLowerCase());

        final BuildDocumentIndexFromTextDocuments indexBuilder
                = new BuildDocumentIndexFromTextDocuments(basename);

        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add(document1);
        textCollection.add(new StringBuffer(document2));
        textCollection.add(new MutableString(document3));
        indexBuilder.index(textCollection);

        final DocumentIndexManager docmanager2 = new DocumentIndexManager(basename);
        assertEquals(tokens.size(), docmanager2.getNumberOfTerms());
        final TermIterator it = docmanager2.getTerms();
        while (it.hasNext()) {
            // test next term
            final CharSequence nextTerm = it.nextTerm();
            assertNotNull(nextTerm);
            assertTrue(nextTerm.toString(), tokens.contains(nextTerm.toString()));

            // test next term string
            final String nextTermString = it.next();
            assertNotNull(nextTermString);
            assertEquals(nextTerm.toString(), nextTermString);

            // test next
            final Object next = it.next();
            assertNotNull(next);
            assertTrue(next instanceof String);
            assertEquals(nextTermString, (String) next);
        }
        docmanager2.close();
    }

    /**
     * @param tokens
     * @param document
     */
    private void collectTokens(final Set<String> tokens, final String document) {
        final StringTokenizer tokenizer =
                new StringTokenizer(document, ", \t\n\r\f");
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken());
        }
    }

    /**
     * Tests that terms at the very end of a sentence are found properly.
     * This validates issue #1043.
     *
     * @see <a href="http://icbtools.med.cornell.edu/mantis/view.php?id=1043">Mantis issue #1043</a>
     */
    public void testTermAtEndOfSentence() throws Exception {
        final String basename = "index/issue1043-test";
        final String sentence1 = "But, the essential character of Asp-162 and its proximity to site 4 would make it seem likely that sites 3 and 4 are both part of a larger nucleoside-binding or recognition domain for HSV-TKs.";
        final String sentence2 = "The homology between the DRS motif of dGK/dAK and the DRH of HSV-TKs, although very limited in size, suggested the possible importance of these residues in some aspect of nucleoside binding and/or catalysis in Lactobacillus dGK/dAK.";

        final BuildDocumentIndexFromTextDocuments indexBuilder
                = new BuildDocumentIndexFromTextDocuments(basename);

        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add(sentence1);
        textCollection.add(sentence2);
        indexBuilder.index(textCollection);

        final DocumentIndexManager indexManager = new DocumentIndexManager(basename);
        final TermProcessor termProcessor = indexManager.getTermProcessor();

        // search for the term - the original issue would only find
        // one instance, but we really need to find both.
        final MutableString term = new MutableString("HSV-TKs");
        termProcessor.processTerm(term);
        final int[] hits = indexManager.query(term.toString());
        assertEquals(2, hits.length);

        // search for the term with including the period - the current
        // processor will strip the period so we should still find both
        // instances.
        final MutableString termWithPeriod = new MutableString("HSV-TKs.");
        termProcessor.processTerm(termWithPeriod);
        final int[] periodHits = indexManager.query(termWithPeriod.toString());
        assertEquals(2, periodHits.length);
        indexManager.close();
    }
}
