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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.docstore.DocumentStoreReader;
import textractor.mg4j.docstore.TermDocumentFrequencyWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: Fabien Campagne
 * Date: Jun 22, 2006
 * Time: 5:59:37 PM
 * To change this template use File | Settings | File Templates.
 */
public final class DocStore2TfIdf {
    public static void main(final String[] args) throws ConfigurationException,
            IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        final boolean verbose = CLI.isKeywordGiven(args, "-verbose");
        final String basename = CLI.getOption(args, "-basename", null);
        final float minRelativeDocumentFrequency = CLI.getFloatOption(args, "-mindf", 0.1f);
        final float maxRelativeDocumentFrequency = CLI.getFloatOption(args, "-maxdf", 0.5f);
        int maxDocument = CLI.getIntOption(args, "-max-document", -1);
        final int chunkSize = CLI.getIntOption(args, "-chunk-size", 1000);

        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);
        final DocumentStoreReader docstore = new DocumentStoreReader(docmanager);
        final TermDocumentFrequencyWriter writer = new TermDocumentFrequencyWriter(docmanager,
                minRelativeDocumentFrequency, maxRelativeDocumentFrequency);

        if (maxDocument == -1) {
            maxDocument = docmanager.getDocumentNumber();
        }
        final PrintWriter outPrintWriter = new PrintWriter(System.out);

        final IntList tokens = new IntArrayList();
        for (int documentIndex = 0; documentIndex < maxDocument; ++documentIndex) {
            docstore.document(documentIndex, tokens);
            writer.appendDocument(documentIndex, tokens.toIntArray());
            if ((documentIndex % chunkSize) == 1) {
                outPrintWriter.println("Converted document #" + documentIndex);
                outPrintWriter.println("============================================");
                writer.printStatistics(outPrintWriter);
                outPrintWriter.println("============================================");
            }
            tokens.clear();
        }
        writer.close();
        docstore.close();
        docmanager.close();
        writer.printStatistics(outPrintWriter);
        if (verbose) {
            writer.printSelectedTerms(outPrintWriter, docmanager);
        }
        outPrintWriter.close();
    }
}
