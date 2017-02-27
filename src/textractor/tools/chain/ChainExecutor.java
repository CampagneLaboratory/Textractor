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

package textractor.tools.chain;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.chain.TextractorContext;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class ChainExecutor {
    private static final Log LOG = LogFactory.getLog(ChainExecutor.class);
    private final Context context = new TextractorContext();
    private final List<Command> commands = new ArrayList<Command>();

    public ChainExecutor(final URL url) throws Exception {
        super();
        final ConfigParser parser = new ConfigParser();
        parser.parse(url);

        final Catalog catalog = CatalogFactoryBase.getInstance().getCatalog();
        initialize(catalog);
    }

    public ChainExecutor(final Catalog catalog) {
        initialize(catalog);
    }

    private void initialize(final Catalog catalog) {
        final Iterator<String> names = catalog.getNames();
        while (names.hasNext()) {
            final String name = names.next();
            final Command command = catalog.getCommand(name);
            commands.add(command);
        }
    }

    public void execute() throws Exception {
        for (final Command command : commands) {
            command.execute(context);
        }
    }

    /**
     * Print usage message for main method.
     * @param options Options used to determine usage
     */
    private static void usage(final Options options) {
        // automatically generate the help statement
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(ChainExecutor.class.getName(), options, true);
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(final String[] args)
            throws ParseException, MalformedURLException {
        // create the Options object
        final Options options = new Options();

        // configuration as a url
        final Option urlOption =
            new Option("u", "url", true, "url of configuration");
        urlOption.setArgName("url");

        // configuration as a filename
        final Option fileOption =
            new Option("f", "file", true, "Name of configuration file");
        fileOption.setArgName("file");

        final OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(true);
        optionGroup.addOption(urlOption);
        optionGroup.addOption(fileOption);

        options.addOptionGroup(optionGroup);

        // parse the command line arguments
        final CommandLine line;
        try {
            // create the command line parser
            final CommandLineParser parser = new BasicParser();
            line = parser.parse(options, args, true);
        } catch (final ParseException e) {
            usage(options);
            throw e;
        }

        final URL url;
        if (line.hasOption('u')) {
            url = new URL(line.getOptionValue('u'));
        } else {
            final File file = new File(line.getOptionValue('f'));
            url = file.toURI().toURL();
        }

        try {
            final ChainExecutor chainExecutor = new ChainExecutor(url);
            // TODO: We may want to put the command line in the context
            //loader.context.put("args", args);
            chainExecutor.execute();
        } catch (Throwable t) {
            LOG.fatal("Chain execution threw exception", t);
            // we need to shut everything down
            System.exit(-1);
        }
    }
}
