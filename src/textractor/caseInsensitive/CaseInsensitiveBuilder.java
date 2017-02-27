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

package textractor.caseInsensitive;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.StopWatch;
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
public final class CaseInsensitiveBuilder {
    /** How often to output what term # we are processing. */
    private static final int OUTPUT_INTERVAL = 100000;

    /**
     * The docmanager to use to build the CaseInsensitiveStore.
     */
    private final DocumentIndexManager docmanager;

    /**
     * Contractor to use to build the CaseInsensitiveStore.
     * @param docmanagerVal The docmanager to use
     */
    public CaseInsensitiveBuilder(final DocumentIndexManager docmanagerVal) {
        this.docmanager = docmanagerVal;
    }

    /**
     * Build the CaseInsensetiveStore.
     * @return CaseInsensitiveStore the CaseInsensitiveStore that was built
     */
    public CaseInsensitiveStore build() throws FileNotFoundException {
        final TermIterator terms = docmanager.getTerms();
        System.out.println("Processing " + docmanager.getNumberOfTerms()
                + " terms...");

        final CaseInsensitiveStore cis = new CaseInsensitiveStore(docmanager);

        for (int count = 0; terms.hasNext(); count++) {
            if ((count % OUTPUT_INTERVAL) == 0) {
                System.out.println("Processing term number " + count);
            }
            final String term = terms.next();
            cis.addTerm(term);
        }

        return cis;
    }

    /**
     * Print usage message for main method.
     * @param options Options used to determine usage
     */
    private static void usage(final Options options) {
        // automatically generate the help statement
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(CaseInsensitiveBuilder.class.getName(), options,
                true);
    }

    /**
     * Command line interface to build the CaseInsensetiveStore.
     * @param args command line arguments
     * @throws ConfigurationException ConfigurationException
     * @throws IOException IOException
     */
    public static void main(final String[] args) throws NoSuchMethodException,
            IllegalAccessException, ConfigurationException, IOException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException {
        // create the Options object
        final Options options = new Options();

        final Option basenameOption =
            new Option("b", "basename", true, "basename to use");
        basenameOption.setArgName("basename");
        basenameOption.setRequired(true);

        final Option dumpOption =
            new Option("d", "dump", false, "dump existing store");

        final Option limitOption =
            new Option("l", "limit", true, "limit number of rows to output for dump");
        limitOption.setArgName("maxrows");

        options.addOption(basenameOption);
        options.addOption(dumpOption);
        options.addOption(limitOption);

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

        final StopWatch timer = new StopWatch();
        timer.start();

        final DocumentIndexManager docmanager =
                new DocumentIndexManager(basename);
        docmanager.setTermMap(new HashTermMap(docmanager.getTerms(), 0));
        if (!line.hasOption("d")) {
            final CaseInsensitiveBuilder builder = new CaseInsensitiveBuilder(docmanager);
            final CaseInsensitiveStore cis = builder.build();
            cis.saveData(basename);
        } else {
            final CaseInsensitiveStore cis = new CaseInsensitiveStore(docmanager, basename);
            int limit = -1;
            if (line.hasOption("l")) {
                final String slimit = line.getOptionValue("l");
                try {
                    limit = Integer.parseInt(slimit);
                } catch (final NumberFormatException e) {
                    // Ooops, not an integer?
                    // Don't sweat it. just dump them all
                    System.out.println("Supplied limit " + limit
                            + " doesn't appear to be an integer."
                            + " Dumping all rows." );
                }
            }
            cis.dumpStore(limit);
        }

        timer.stop();
        System.out.println(timer);
    }
}
