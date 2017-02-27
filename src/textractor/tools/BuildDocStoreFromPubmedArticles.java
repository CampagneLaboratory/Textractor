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

import com.martiansoftware.jsap.JSAPException;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.document.DocumentSequence;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.StopWatch;
import textractor.database.DocumentIndexManager;
import textractor.datamodel.Sentence;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.docstore.DocumentStoreWriter;
import textractor.mg4j.docstore.TermDocumentFrequencyWriter;
import textractor.mg4j.document.TextractorDocumentFactory;
import textractor.mg4j.document.TextractorQueueDocumentIterator;
import textractor.mg4j.document.TextractorQueueDocumentSequence;
import textractor.util.NullSentenceFilter;
import textractor.util.SentenceFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * Pass over a document collection to create a document store.
 *
 * @author Fabien Campagne
 *         Date: Sep 11, 2006
 *         Time: 3:00:44 PM
 */
public final class BuildDocStoreFromPubmedArticles extends BuildDocumentIndexFromPubmedArticles {
    public BuildDocStoreFromPubmedArticles(final String[] args) throws NoSuchMethodException,
            IllegalAccessException, ConfigurationException, IOException, JSAPException,
            InvocationTargetException, ClassNotFoundException, InstantiationException {
        super(args);
    }

    @Override
    protected void processDocuments(final String[] args) throws ConfigurationException,
            IOException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, ClassNotFoundException,
            JSAPException {
        int reportingChunkSize = 10000;
        reportingChunkSize = CLI.getIntOption(args, "-chunk-size", reportingChunkSize);
        final boolean optimize = CLI.isKeywordGiven(args, "-optimize");
        final boolean writeTermDocFreqs = CLI.isKeywordGiven(args, "-term-doc-freqs");

        final SentenceFilter sentenceIndexFilter = new NullSentenceFilter();

        Sentence doc;
        final String basename = CLI.getOption(args, "-basename", null);
        final TextractorDocumentFactory factory = (TextractorDocumentFactory)
                PropertyBasedDocumentFactory.getInstance(TextractorDocumentFactory.class,
                        DocumentIndexManager.getPropertiesForBasename(basename));
        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);

        final DocumentSequence docSequence = new TextractorQueueDocumentSequence(factory,
                sentenceIndexFilter, getQueue(), getLoaderSemaphore(),
                NUMBER_OF_LOADER_THREADS);
        final TextractorQueueDocumentIterator docIterator =
                (TextractorQueueDocumentIterator) docSequence.iterator();

        // install a fast term map.
        final StopWatch timer = new StopWatch();
        timer.start();
        System.out.println("Initializing hash term map");
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));
        timer.stop();
        System.out.println("Term map installed in " + timer.toString());
        final DocumentStoreWriter writer = new DocumentStoreWriter(docmanager);

        TermDocumentFrequencyWriter termDocumentFrequencyWriter = null;

        if (writeTermDocFreqs) {
            termDocumentFrequencyWriter = new TermDocumentFrequencyWriter(docmanager, 0.1f, 0.5f);
        }
        if (optimize) {
            timer.reset();
            timer.start();
            System.out.println(
                    "Starting to optimize term ordering (recoding by decreasing frequency)");
            writer.optimizeTermOrdering();
            timer.stop();
            System.out.println("Optimization completed in " + timer.toString());
        }

        int docIndex = 0;

        timer.reset();
        timer.start();
        while ((doc = docIterator.nextSentence()) != null) {

            final int[] tokens = docmanager.extractTerms(doc.getText());
            writer.appendDocument(docIndex, tokens);
            if (termDocumentFrequencyWriter != null) {
                termDocumentFrequencyWriter.appendDocument(docIndex, tokens);
            }
            writer.addDocumentPMID(docIndex, doc.getArticle().getPmid());
            docIndex++;
            if ((docIndex % reportingChunkSize) == 1) {
                timer.suspend();
                float docPerSecond = reportingChunkSize;
                docPerSecond /= timer.getTime();
                docPerSecond *= 1000; // ms -> seconds
                System.out.println("Processed " + docIndex + " textractor documents. Rate: " +
                        (docPerSecond + " document/s"));

                timer.reset();
                timer.start();
            }
        }
        writer.writePMIDs();
        writer.close();

        if (writeTermDocFreqs) {
            termDocumentFrequencyWriter.close();
            final PrintWriter pw = new PrintWriter(System.out);
            termDocumentFrequencyWriter.printStatistics(pw);
            pw.flush();
        }
    }

    public static void main(final String[] args)
            throws IllegalAccessException, NoSuchMethodException,
            ConfigurationException, IOException, JSAPException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException {
        final BuildDocStoreFromPubmedArticles builder =
                new BuildDocStoreFromPubmedArticles(args);
        builder.threadPool.shutdown();
    }
}
