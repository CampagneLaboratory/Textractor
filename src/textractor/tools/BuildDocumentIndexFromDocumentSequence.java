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

import it.unimi.dsi.mg4j.document.DocumentSequence;
import textractor.mg4j.document.TextractorDocumentFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * User: campagne
 * Date: Nov 8, 2005
 * Time: 3:01:44 PM
 */
public final class BuildDocumentIndexFromDocumentSequence extends BuildDocumentIndex {
    private DocumentSequence documentSequence;
    private final String basename;

    public BuildDocumentIndexFromDocumentSequence(final String basename,
            final DocumentSequence documentSequence) {
        super();
        this.basename = basename;
        this.documentSequence = documentSequence;
        usePipe = false;
        indexerOptions.setBatchSize("2Mi");
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
        return this.basename;
    }

    /**
     * Index the collection of text documents.
     *
     * @param sequence The sequence of documents to index.
     * @see it.unimi.dsi.mg4j.document.DocumentSequence
     */
    public void index(final DocumentSequence sequence, final int chunkSize)
            throws
            IOException {
        this.documentSequence = sequence;
        index(false, chunkSize, basename);
    }

    /**
     * Factory method. Returns a new instance of this class. Must be overriden
     * by sub-classes.
     */
    @Override
    protected BuildDocumentIndex createNew() {
        return new BuildDocumentIndexFromDocumentSequence(basename,
                this.documentSequence);
    }

    @Override
    public int serializeTextSourceToWriter(final OutputStreamWriter writer,
            final int chunkSize) throws IOException {
        throw new UnsupportedOperationException("Not supported by this implementation. Use documentSequence instead.");
    }

    @Override
    public DocumentSequence documentSequence(final TextractorDocumentFactory factory) {
        return documentSequence;
    }
}
