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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.HashTermMap;
import textractor.mg4j.index.TermIterator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Class to build the CaseInsensetiveStore.
 * @author Kevin Dorff
 */
public final class TermChecker {

    /** How often to writer what term # we are processing. */
    private static final int OUTPUT_INTERVAL = 100000;

    /**
     * The docmanager to use to build the CaseInsensitiveStore.
     */
    private final DocumentIndexManager docmanager;

    /**
     * Constructor.
     * @param docmanagerVal The docmanager to use
     */
    public TermChecker(final DocumentIndexManager docmanagerVal) {
        this.docmanager = docmanagerVal;
    }

    /**
     * Build the CaseInsensetiveStore.
     */
    public void check() throws FileNotFoundException {
        final TermIterator terms = docmanager.getTerms();
        System.out.println("Processing " + docmanager.getNumberOfTerms()
                + " terms...");

        for (int count = 0; terms.hasNext(); count++) {
            if ((count % OUTPUT_INTERVAL) == 0) {
                System.out.println("Processing term number " + count);
            }
            final String term = terms.next();
            final int termIndex = docmanager.findTermIndex(term);
            if (termIndex == -1) {
                System.out.println("term '" + term + "' when looked up"
                        + "had an index of -1");
            } else {
                final String newTerm = docmanager.termAsString(termIndex);
                if (!newTerm.equals(term)) {
                    System.out.println("term=" + term
                            + " : newTerm =" + newTerm);
                }
            }
        }
    }

    /**
     * Print usage message for main method.
     * @param options Options used to determine usage
     */
    private static void usage(final Options options) {
        // automatically generate the help statement
        final HelpFormatter formatter = new HelpFormatter();
        formatter
                .printHelp(TermChecker.class.getName(), options, true);
    }

    /**
     * Command line interface to build the CaseInsensetiveStore.
     * @param args command line arguments
     * @throws ConfigurationException ConfigurationException
     * @throws IOException IOException
     */
    public static void main(final String[] args)
            throws ConfigurationException, IOException,
            IllegalAccessException,
            InvocationTargetException, NoSuchMethodException,
            InstantiationException, ClassNotFoundException, URISyntaxException {

        // create the Options object
        final Options options = new Options();

        final Option basenameOption = new Option("b", "basename", true,
                "basename to use");
        basenameOption.setArgName("basename");
        basenameOption.setRequired(true);

        options.addOption(basenameOption);

        // parse the command line arguments
        CommandLine line = null;

        try {
            // create the command line parser
            final CommandLineParser parser = new BasicParser();
            line = parser.parse(options, args, true);
        } catch (final ParseException e) {
            System.err.println("Error: " + e.getMessage());
            usage(options);
            System.exit(1);
        }

        final String basename = line.getOptionValue("b");

        final DocumentIndexManager docmanager =
                new DocumentIndexManager(basename);
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));
        final TermChecker checker = new TermChecker(docmanager);
        checker.check();
    }
}
