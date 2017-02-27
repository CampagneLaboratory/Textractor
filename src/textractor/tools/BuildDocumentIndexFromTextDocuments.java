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

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.impl.CatalogBase;
import textractor.chain.indexer.Indexer;
import textractor.chain.producer.StringArrayProducer;
import textractor.mg4j.io.ProteinWordSplitterReader;
import textractor.tools.chain.ChainExecutor;

import java.util.Collection;

public final class BuildDocumentIndexFromTextDocuments {
    private final String basename;

    public BuildDocumentIndexFromTextDocuments(final String basename) {
        this.basename = basename;
    }

    public String getBasename() {
        return basename;
    }

    /**
     * Index the collection of text documents.
     *
     * @param textDocuments Collection of CharSequence (interface implemented by StringBuffer, String, MutableString, etc.).
     * @see java.lang.CharSequence
     */
    public void index(final CharSequence[] textDocuments) throws Exception {
        // create a simple loader
        final StringArrayProducer loader = new StringArrayProducer(textDocuments);

        // add an indexer
        final Indexer indexer = new Indexer();
        indexer.setBasename(basename);
        indexer.setWordReaderClass(ProteinWordSplitterReader.class.getName());
        indexer.setParenthesesAreWords(true);
        loader.addCommand(indexer);

        // and execute it
        final Catalog indexerCatalog = new CatalogBase();
        indexerCatalog.addCommand("Indexer", loader);

        final ChainExecutor executor = new ChainExecutor(indexerCatalog);
        executor.execute();
    }

    /**
     * Index the collection of text documents.
     *
     * @param textDocuments Collection of CharSequence (interface implemented
     * by StringBuffer, String, MutableString, etc.).
     * @see java.lang.CharSequence
     */
    public void index(final Collection<CharSequence> textDocuments) throws Exception {
        index(textDocuments.toArray(new CharSequence[textDocuments.size()]));
    }
}
