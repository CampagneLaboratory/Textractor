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

package textractor.tools.lookup;

import com.martiansoftware.jsap.JSAPException;
import edu.mssm.crover.cli.CLI;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.database.DocumentTermPositions;
import textractor.datamodel.LookupResult;
import textractor.stemming.PaiceHuskStemmer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

/**
 * Provides basic functionality for looking up protein names.
 */
public abstract class AbstractLookupProteinname {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(AbstractLookupProteinname.class);

    protected String indexBasename;

    /**
     * Dictionary used to lookup terms.
     */
    protected Dictionary dictionary;

    /**
     * Document manager used to lookup terms.
     */
    protected DocumentIndexManager docManager;

    /**
     * Indicates that stemming is enabled.
     */
    protected boolean stemming;
    protected PaiceHuskStemmer stemmer;

    protected boolean checkAndRemoveRedundancy;
    protected String subLookupResultsDirectory = "";

    /**
     * Default directory name for the dictionary.
     */
    public static final String DICTIONARY_DIRECTORY = "dictionary";

    /**
     * Directory name for results of a lookup.
     */
    public static final String LOOKUP_RESULTS_DIRECTORY = "lookup_results";

    public AbstractLookupProteinname() throws IOException {
        this(new String[]{});
    }

    public AbstractLookupProteinname(final String[] args) throws IOException {
        super();
        initializeDictionary(args);
    }

    public void setDictionary(final String filename) throws IOException {
        dictionary = new Dictionary(filename, stemming, checkAndRemoveRedundancy);
    }

    public Collection<LookupResult> lookupAllTermsByTerm(final String basename) throws NoSuchMethodException, IllegalAccessException, ConfigurationException, IOException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager manager =
                new DocumentIndexManager(basename);
        try {

            return dictionary.lookupByTerm(manager);
        } finally {
            manager.close();
        }
    }

    protected final Map<Integer, DocumentTermPositions> lookupAllTermsByDocument(final String basename) throws IOException, ConfigurationException {
        final DocumentIndexManager manager =
                new DocumentIndexManager(basename);
        return dictionary.lookupByDocument(manager);
    }

    private void initializeDictionary(final String[] args) throws IOException {
        stemming = CLI.isKeywordGiven(args, "-stemming", false);
        checkAndRemoveRedundancy =
                CLI.isKeywordGiven(args, "-checkAndRemoveRedundancy", false);

        if (stemming) {
            try {
                stemmer = new PaiceHuskStemmer(true);
            } catch (final IOException e) {
                LOG.error("An error occured initializing stemming support. Defaulting to no stemming", e);
            }
        }

        final String dictionaryFilename = CLI.getOption(args, "-dic", null);
        setDictionary(dictionaryFilename);
    }

    abstract void process(final String[] args)
            throws IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException, JSAPException;
}
