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

package textractor.didyoumean;

import com.martiansoftware.jsap.JSAPException;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.index.BitStreamIndex;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.document.TextractorDocumentFactory;
import textractor.tools.BuildDocumentIndexFromDocumentSequence;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * User: campagne
 * Date: Nov 8, 2005
 * Time: 2:47:33 PM
 */
public final class DidYouMeanIndexer {
    /** Used to log informational and debug messages. */
    private static final Log LOG = LogFactory.getLog(DidYouMeanIndexer.class);

    private final DocumentIndexManager docmanager;
    private String batchSize;
    private int quantum;
    private int height;
    private int chunkSize;
    private boolean skips;

    /** Minimum index at which a word can be split. */
    private int minimumDashSplitLength;

    public DidYouMeanIndexer(final DocumentIndexManager docmanager) {
        super();
        this.docmanager = docmanager;
        this.batchSize = "2Mi";
        this.quantum = BitStreamIndex.DEFAULT_QUANTUM;
    	this.height = BitStreamIndex.DEFAULT_HEIGHT;
    }

    public void setChunkSize(final int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public void setQuantum(final int quantum) {
        this.quantum = quantum;
    }

    public void setSkips(final boolean skips) {
        this.skips = skips;
    }

    public void setBatchSize(final String batchSize) {
        this.batchSize = batchSize;
    }

    public int getMinimumDashSplitLength() {
        return minimumDashSplitLength;
    }

    public void setMinimumDashSplitLength(final int minimumDashSplitLength) {
        this.minimumDashSplitLength = minimumDashSplitLength;
    }

    /**
     * Prepares the DidYouMean index.
     *
     * @param frequencyThreshold is the minimum number of times a term should
     *        appear in the corpus to be included in the Did You Mean index.
     */
    public void index(final int frequencyThreshold) throws IOException,
            IllegalAccessException, NoSuchMethodException, ConfigurationException, JSAPException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        final DidYouMeanDocumentSequence documentSequence =
                new DidYouMeanDocumentSequence(docmanager, frequencyThreshold);
        final String basename = docmanager.getBasename() + "-dym";
        final BuildDocumentIndexFromDocumentSequence indexer =
                new BuildDocumentIndexFromDocumentSequence(basename, documentSequence);

        // Obtain the word reader of the main index and configure the did you
        // mean word reader in the same way:
        indexer.getIndexerOptions().setWordReader(docmanager.getWordReader());
        indexer.getIndexerOptions().setCreateSkips(skips);
        indexer.getIndexerOptions().setQuantum(quantum);
        indexer.getIndexerOptions().setHeight(height);
        indexer.getIndexerOptions().setBatchSize(batchSize);
        indexer.getIndexerOptions().setMinimumDashSplitLength(minimumDashSplitLength);
        indexer.index(documentSequence, chunkSize);

    }

    public static void main(final String[] args) throws NoSuchMethodException, IllegalAccessException, ConfigurationException, IOException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException, JSAPException {
        final String basename = CLI.getOption(args, "-basename", null);
        final int frequencyThreshold = CLI.getIntOption(args, "-threshold", 2);
        final int quantum = CLI.getIntOption(args, "-quantum", BitStreamIndex.DEFAULT_QUANTUM);
        final int height = CLI.getIntOption(args, "-height", BitStreamIndex.DEFAULT_HEIGHT);
        final int chunkSize = CLI.getIntOption(args, "-chunk-size", 100000);
        final String batchSize = CLI.getOption(args, "-batch-size", "2Mi");
        final boolean skips = CLI.isKeywordGiven(args, "-skips");
        final int minimumDashSplitLength =
                CLI.getIntOption(args, "--min-length-split", TextractorDocumentFactory.DEFAULT_MINIMUM_DASH_SPLIT_LENGTH);

        final StopWatch timer = new StopWatch();
        timer.start();

        final DocumentIndexManager docmanager =
                new DocumentIndexManager(basename);
        final DidYouMeanIndexer indexer = new DidYouMeanIndexer(docmanager);
        indexer.setBatchSize(batchSize);
        indexer.setChunkSize(chunkSize);
        indexer.setSkips(skips);
        indexer.setQuantum(quantum);
        indexer.setHeight(height);
        indexer.setMinimumDashSplitLength(minimumDashSplitLength);
        indexer.index(frequencyThreshold);

        timer.stop();
        LOG.info(timer);
    }
}
