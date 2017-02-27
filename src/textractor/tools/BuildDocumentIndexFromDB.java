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
import it.unimi.dsi.mg4j.document.DocumentSequence;
import it.unimi.dsi.mg4j.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DbManager;
import textractor.database.TextractorDBDocumentSequence;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.mg4j.document.TextractorDocumentFactory;
import textractor.mg4j.index.DBIndexerOptions;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * Used to builds a the document index from the database.
 */
public class BuildDocumentIndexFromDB extends BuildDocumentIndex {
    public BuildDocumentIndexFromDB() {
        super();
        indexerOptions = new DBIndexerOptions();
        usePipe = false;
    }

    protected final DbManager dbm() {
        return ((DBIndexerOptions) (getIndexerOptions())).getDbManager();
    }

    /**
     * This method returns a basename, to use when the user provided none on the
     * command line.
     *
     * @param basename Basename provided on the command line.
     * @param stemming Whether stemming is request
     * @return The basename provided on the command line, or a default basename
     */
    @Override
    public String getDefaultBasename(final String basename,
            final boolean stemming) {
        final String defaultBasename;
        if (basename == null) {
            dbm().beginTxn();
            if (stemming) {
                defaultBasename = dbm().getTextractorManager().getInfo()
                        .getStemmedIndexBasename();
            } else {
                defaultBasename = dbm().getTextractorManager().getInfo()
                        .getCaseSensitiveIndexBasename();
            }
            dbm().commitTxn();
        } else {
            defaultBasename = basename;
        }
        return defaultBasename;
    }

    @Override
    protected BuildDocumentIndex createNew() {
        return new BuildDocumentIndexFromDB();
    }

    @Override
    public int serializeTextSourceToWriter(final OutputStreamWriter writer,
            final int chunkSize) throws IOException {
        throw new UnsupportedOperationException(
                "Should not be called. This implementation does not use the pipe mechanism.");
    }

    public DBIndexerOptions getDBIndexerOptions() {
        return (DBIndexerOptions) getIndexerOptions();
    }

    @Override
    public DocumentSequence documentSequence(final TextractorDocumentFactory factory) {
        return new TextractorDBDocumentSequence(dbm(), factory,
                indexerOptions.getChunkSize(), sentenceIndexFilter);
    }

    public static void main(final String[] args) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, InstantiationException, IOException,
            TextractorDatabaseException, JSAPException, ConfigurationException {
        final BuildDocumentIndexFromDB docindexer =
            new BuildDocumentIndexFromDB();
        docindexer.getDBIndexerOptions().setDbManager(new DbManager(args));

        docindexer.process(args);
    }

    /**
     * Stores properties of this class into the given filename.
     *
     * @param filename Name of the file to store the properties in
     * @throws ConfigurationException if the file cannot be written properly
     */
    @Override
    protected final Properties storeProperties(final String filename)
            throws ConfigurationException {
        final Properties properties = super.storeProperties(filename);

        // get some properties from the database
        try {
            addDatabaseProperties(properties);
        } catch (final TextractorDatabaseException e) {
            throw new ConfigurationException(e);
        }

        // and save everything to a file
        properties.save(filename);
        return properties;
    }

    /**
     * Add properties about the current database to a properties object.
     * @param properties The properties object to modify
     */
    private void addDatabaseProperties(final Properties properties)
        throws TextractorDatabaseException {
        assert properties != null;

        final DbManager dbm = new DbManager();
        final TextractorManager textractorManager = dbm.getTextractorManager();
        dbm.beginTxn();
        properties.addProperty("sentenceCount",
                Integer.toString(textractorManager.getSentenceCount()));
        properties.addProperty("documentCount",
                Integer.toString(textractorManager.getDocumentCount()));
        dbm.commitTxn();
    }
}
