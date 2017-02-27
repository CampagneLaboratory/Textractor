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

package textractor.util;

import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.docstore.DocumentStoreReader;
import textractor.tools.DefaultSentenceSplitter;
import textractor.tools.SentenceSplitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Iterator;

/**
 * Utility class used to extract sentences from a document store and index.
 */
public final class SentenceExtractor {
    private static DocumentStoreReader reader;
    private static PrintWriter writer;
    private static final SentenceSplitter splitter =
            new DefaultSentenceSplitter();
    private static int minSentenceNumber = Integer.MIN_VALUE;
    private static int maxSentenceNumber = Integer.MAX_VALUE;

    /**
     * Create a new SentenceExtractor.
     */
    private SentenceExtractor() {
        super();
    }

    /**
     * Write sentences from an the document store.
     * @param pmid The pubmed identifier of the document to write
     * @throws IOException if there is a problem reading from the document
     * store or if there is a problem writing to the output
     */
    private static void writeDocument(final long pmid)
            throws IOException {
        final int documentNumber = reader.getDocumentNumber(pmid);
        // get the text from the document reader and remove the leading
        // space prior to any sentence terminator (.;?!) in the sentences.
        final String text = reader.document(documentNumber)
                .replace(" . ", ". ").replace(" ; ", "; ")
                .replace(" ? ", "? ").replace(" ! ", "! ")
                .toString();
        final Iterator<MutableString> sentences = splitter.split(text);
        int count = 1;
        while (sentences.hasNext()) {
            final MutableString sentence = sentences.next();
            if (count >= minSentenceNumber && count <= maxSentenceNumber) {
                writer.printf("%d\t%d\t%s\n", documentNumber, pmid, sentence);
            }
            count++;
        }
        writer.flush();
    }

    /**
     * Print usage message for main method.
     * @param options Options used to determine usage
     */
    private static void usage(final Options options) {
        // automatically generate the help statement
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(SentenceExtractor.class.getName(), options, true);
    }

    public static void main(final String[] args) throws IOException,
            ParseException, NoSuchMethodException, IllegalAccessException,
            ConfigurationException, InvocationTargetException,
            InstantiationException, ClassNotFoundException, URISyntaxException {
        // create the Options object
        final Options options = new Options();

        // basename of the index
        final Option basenameOption = new Option("b", "basename", true,
                "Basename of the index/docstore");
        basenameOption.setArgName("basename");
        basenameOption.setRequired(true);
        options.addOption(basenameOption);

        // single pmid
        final Option pmidOption =
                new Option("p", "pmid", true, "Pubmed identifier");
        pmidOption.setArgName("pmid");

        // configuration as a filename
        final Option fileOption = new Option("f", "file", true,
                "Name of title file to use for getting the pmids");
        fileOption.setArgName("file");

        // sentence range (starts at 1)
        final Option sentenceNumberOption = new Option("n", "number", true,
                "Sentence number/range to extract");
        sentenceNumberOption.setArgs(2);
        options.addOption(sentenceNumberOption);

        // can either specify a pmid or title file, but not both
        final OptionGroup optionGroup = new OptionGroup();
        optionGroup.addOption(pmidOption);
        optionGroup.addOption(fileOption);
        options.addOptionGroup(optionGroup);

        // name of the output file
        final Option outputOption = new Option("o", "output", true,
                "Name of the file to write results to");
        outputOption.setArgName("output");
        options.addOption(outputOption);

        final Option helpOption =
                new Option("h", "help", false, "Display usage information");
        options.addOption(helpOption);

        // parse the command line arguments
        final CommandLine commandLine;
        try {
            // create the command line parser
            final CommandLineParser parser = new BasicParser();
            commandLine = parser.parse(options, args, true);
        } catch (final ParseException e) {
            usage(options);
            throw e;
        }

        // display help if the user wants it.
        if (commandLine.hasOption("h")) {
            usage(options);
            return;
        }

        // output file to write to
        if (commandLine.hasOption("o")) {
            writer = new PrintWriter(commandLine.getOptionValue("o"));
        } else {
            writer = new PrintWriter(System.out);
        }

        if (commandLine.hasOption('n')) {
            final String[] numbers = commandLine.getOptionValues("n");
            minSentenceNumber = Integer.valueOf(numbers[0]);
            if (numbers.length > 1) {
                maxSentenceNumber = Integer.valueOf(numbers[1]);
            } else {
                maxSentenceNumber = minSentenceNumber;
            }
            assert minSentenceNumber <= maxSentenceNumber :
                    "min sentence must be less than or equal to max";
        }

        // index basename
        final String basename = commandLine.getOptionValue("b");
        reader = new DocumentStoreReader(new DocumentIndexManager(basename));
        reader.readPMIDs();

        writer.println("docNumber\tpmid\tsentence");
        writer.flush();

        if (commandLine.hasOption("p")) {
            // a single pmid
            final long pmid = Long.valueOf(commandLine.getOptionValue("p"));
            writeDocument(pmid);
        } else if (commandLine.hasOption("f")) {
            // a title file containing multiple pmids
            final String titleFilename = commandLine.getOptionValue("f");
            final LineIterator lines =
                    FileUtils.lineIterator(new File(titleFilename), null);
            // skip first line (assume header)
            if (lines.hasNext()) {
                lines.next();
            }
            // process each line
            while (lines.hasNext()) {
                final String line = lines.nextLine();
                final String[] columns = line.split("\t");
                // file format is "documentNumber\tpmid\tsentence"
                assert columns.length == 3 : "Invalid line: " + line;
                writeDocument(Integer.valueOf(columns[1]));
            }
            lines.close();
        } else {
            // all the documents in the store
            for (int i = 0; i < reader.getNumberOfDocuments(); i++) {
                final long pmid = reader.getPMID(i);
                writeDocument(pmid);
            }
        }
        writer.close();
    }
}
