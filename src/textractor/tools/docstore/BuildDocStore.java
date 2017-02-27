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

package textractor.tools.docstore;

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.Article;
import textractor.datamodel.OtmiArticle;
import textractor.datamodel.Sentence;
import textractor.datamodel.TextractorDocument;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.docstore.DocumentStoreWriter;
import textractor.mg4j.docstore.StringPerDocumentWriter;
import textractor.mg4j.docstore.TermDocumentFrequencyWriter;

import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Construct a document store from documents in the database.
 * User: campagne
 * Date: Oct 31, 2005
 * Time: 2:55:04 PM
 */
public final class BuildDocStore {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(BuildDocStore.class);

    private transient int chunkSize;
    private transient MutableString sentenceText;
    private transient long lowerBound = -1;
    private transient long count;
    private Query query;

    private Iterator<TextractorDocument> iterator;
    private Collection<TextractorDocument> collection;

    public BuildDocStore() {
        super();
    }

    private void process(final String[] args) throws TextractorDatabaseException, IOException,
            ConfigurationException {
        final String basename = CLI.getOption(args, "-basename", null);
        chunkSize = CLI.getIntOption(args, "-chunk-size", 10000);
        final boolean optimize = CLI.isKeywordGiven(args, "-optimize");
        final boolean writeTermDocFreqs = CLI.isKeywordGiven(args, "-term-doc-freqs");
        final boolean otmi = CLI.isKeywordGiven(args, "-otmi");

        if (basename == null) {
            System.err.println("Index basename must be provided (-basename)");
            System.exit(10);
        }

        final DbManager dbm = new DbManager();
        dbm.beginTxn();

        final TextractorManager textractorManager = dbm.getTextractorManager();
        textractorManager.setRetrieveAll(true);
        // hint DB implementation to pre-fetch the query results.
        final DocumentIndexManager docmanager =
            new DocumentIndexManager(basename);
        final DocumentStoreWriter docStoreWriter =
            new DocumentStoreWriter(docmanager);
        final StringPerDocumentWriter doiWriter;
        if (otmi) {
            doiWriter = new StringPerDocumentWriter(docmanager, "otmi-dois");
        } else {
            doiWriter = null;
        }

        final TermDocumentFrequencyWriter termDocumentFrequencyWriter;
        if (writeTermDocFreqs) {
            termDocumentFrequencyWriter = new TermDocumentFrequencyWriter(docmanager, 0.1f, 0.5f);
        } else {
            termDocumentFrequencyWriter = null;
        }

        sentenceText = new MutableString();

        // install a fast term map.
        final StopWatch timer = new StopWatch();
        timer.start();
        System.out.println("Initializing hash term map");
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));
        timer.stop();
        System.out.println("Term map installed in " + timer.toString());


        if (optimize) {
            timer.reset();
            timer.start();
            System.out.println(
                    "Starting to optimize term ordering (recoding by decreasing frequency)");
            docStoreWriter.optimizeTermOrdering();
            timer.stop();
            System.out.println("Optimization completed in " + timer.toString());
        }

        writePMIDs(dbm, docStoreWriter, textractorManager);
        writeSentences(textractorManager, dbm, docStoreWriter, doiWriter, docmanager,
                termDocumentFrequencyWriter);
        if (writeTermDocFreqs) {
            termDocumentFrequencyWriter.close();
            final PrintWriter pw = new PrintWriter(System.out);
            termDocumentFrequencyWriter.printStatistics(pw);
            pw.flush();
        }

        dbm.commitTxn();
    }

    private void writeSentences(
            final TextractorManager textractorManager, final DbManager dbm,
            final DocumentStoreWriter docStoreWriter, final StringPerDocumentWriter doiWriter,
            final DocumentIndexManager docmanager,
            final TermDocumentFrequencyWriter termDocumentFrequencyWriter) throws IOException {
        final StopWatch timer = new StopWatch();
        timer.start();
        System.out.println("Starting to append documents");
        lowerBound = -1;
        iterator = textractorManager.getDocumentIterator(lowerBound, chunkSize);
        TextractorDocument document;
        int defaultDocumentNumber = 0;
        while ((document = nextSentence(dbm)) != null) {
            sentenceText.setLength(0);

            try {
                sentenceText.append(document.getText());
                final int documentIndex = (int) document.getDocumentNumber();
                final int[] tokens = docmanager.extractTerms(sentenceText);
                if (termDocumentFrequencyWriter != null) {
                    termDocumentFrequencyWriter.appendDocument(documentIndex, tokens);
                }
                docStoreWriter.appendDocument(documentIndex, tokens);

                // Question: Currently, OTMI documents are 1 sentence
                // per document. If, in the future, there is more than
                // 1 sentence per document will this not be appended
                // too many times?
                if (doiWriter != null) {
                    final String doi = obtainOtmiDoi(document);
                    if (doi != null) {
                        doiWriter.appendDocumentString(documentIndex, doi);
                    }
                }
            } catch (final Exception e) {
                LOG.warn("Document #" + defaultDocumentNumber
                        + " An exception was encountered while "
                        + " processing this document. Ignored.", e);
                if (termDocumentFrequencyWriter != null) {
                    termDocumentFrequencyWriter.appendDocument(defaultDocumentNumber,
                            docmanager.extractTerms(""));
                }
                docStoreWriter.appendDocument(defaultDocumentNumber, docmanager.extractTerms(""));
            }
            dbm.getPM().evict(document);
            ++defaultDocumentNumber;
        }
        docStoreWriter.close();
        if (doiWriter != null) {
            doiWriter.close();
        }
        timer.stop();
        System.out.println("Document written in " + timer.toString());
    }

    private String obtainOtmiDoi(final TextractorDocument document) {
        if (!(document instanceof Sentence)) {
            return null;
        }
        final Sentence sentence = (Sentence)document;
        if (!(sentence.getArticle() instanceof OtmiArticle)) {
            return null;
        }

        // Get the otmi doi
        final OtmiArticle otmiArticle =
            (OtmiArticle)sentence.getArticle();
        return otmiArticle.getDoi();
    }

    private void writePMIDs(final DbManager dbm,
                            final DocumentStoreWriter docStoreWriter,
                            final TextractorManager textractorManager) throws IOException {
        final StopWatch timer = new StopWatch();
        timer.start();
        System.out.println("Starting to append PMIDs");
        lowerBound = -1;

        final int numberArticles =
                dbm.getTextractorManager().getLastArticleNumber();

        int n = -1;
        textractorManager.setRetrieveAll(true);
        final PersistenceManager pm = dbm.getPM();

        while (n < numberArticles) {
            final Iterator articles =
                    dbm.getTextractorManager().getArticleIterator(n, n + chunkSize);
            while (articles.hasNext()) {
                try {
                    final Article article = (Article) articles.next();
                    final long pmid = article.getPmid();
                    final long sentenceStart =
                            article.getDocumentNumberRangeStart();
                    final long sentenceEnd =
                            sentenceStart + article.getDocumentNumberRangeLength();

                    for (long i = sentenceStart; i < sentenceEnd; ++i) {
                        docStoreWriter.addDocumentPMID(i, pmid);
                    }
                    pm.evict(article);
                } catch (final JDOException e) {
                    LOG.error("Caught JDOException and continuing", e);
                }
            }

            n = n + chunkSize;
            if (n > numberArticles) {
                n = numberArticles;
            }
            System.out.println("Processed " + n + "/" + numberArticles + " articles.");

            dbm.commitTxn();
            dbm.beginTxn();
        }

        System.out.println("Processed " + n + "/" + numberArticles + " articles.");

        // store the pmids in a file
        System.out.println("Writing to pmid file.");
        docStoreWriter.writePMIDs();

        timer.stop();
        System.out.println("PMIDs written in " + timer);
        dbm.commitTxn();
        dbm.beginTxn();
    }

    private TextractorDocument nextSentence(final DbManager dbm) {
        if (this.iterator.hasNext()) {
            count++;
            return this.iterator.next();
        } else {
            // free resources used for last chunk:
            if (collection != null) {
                try {
                    // for some reason, this seem to release more resources
                    // than doing just closeAll.
                    collection.clear();
                } catch (final UnsupportedOperationException e) {
                    // Some implementations have read-only collections
                    // that cannot be cleared  :-(
                    LOG.warn("Clear is not supported", e);
                }
                collection = null;
            }

            if (query != null) {
                query.closeAll();
            }

            dbm.commitTxn();
            // start a new transaction:
            dbm.beginTxn();
            lowerBound = count - 1;
            System.out.println("Processing... (processed " + count + " sentences.) ");

            // try to get more sentences from the database:
            final TextractorManager textractorManager =
                    dbm.getTextractorManager();
            textractorManager.setRetrieveAll(true);
            query = textractorManager.getDocumentQuery(lowerBound, lowerBound += chunkSize, null);
            this.collection = (Collection<TextractorDocument>) query.execute();
            this.iterator = collection.iterator();

            textractorManager.setRetrieveAll(false);
        }

        if (this.iterator != null && this.iterator.hasNext()) {
            count++;
            return this.iterator.next();
        } else {
            System.out.println("Processed " + count + " sentences.");
            return null; // no more sentences. We are done.
        }
    }

    public static void main(final String[] args) throws TextractorDatabaseException,
            ConfigurationException, IOException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException, ClassNotFoundException, InstantiationException,
            URISyntaxException {
        final BuildDocStore builder = new BuildDocStore();
        builder.process(args);
    }
}
