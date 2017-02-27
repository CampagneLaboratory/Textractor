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

package textractor.mg4j.index;

import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.stemming.PaiceHuskStemmer;

import java.io.IOException;

/**
 * A term processor that uses the Paice/Husk Stemmer to process terms.
 */
public final class PaiceHuskStemmingTermProcessor extends TweaseTermProcessor {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 2L;

    /**
     * Used to log informational and debug messages.
     */
    private static final Log LOG =
            LogFactory.getLog(PaiceHuskStemmingTermProcessor.class);

    private PaiceHuskStemmer stemmer;
    private static final PaiceHuskStemmingTermProcessor INSTANCE =
            new PaiceHuskStemmingTermProcessor();

    public static TermProcessor getInstance() {
        return INSTANCE;
    }

    /**
     * Create a new @{link TermProcessor}.
     */
    private PaiceHuskStemmingTermProcessor() {
        super();
        try {
            stemmer = new PaiceHuskStemmer(true);
        } catch (final IOException e) {
            LOG.error("An error occured initializing stemming support. Defaulting to no stemming", e);
            stemmer = null;
        }
    }

    /**
     * Processes the given term, leaving the result in the same mutable string.
     *
     * @param term a mutable string containing the term to be processed,
     * or <code>null</code>.
     * @return true if the term is not <code>null</code> and should be indexed,
     * false otherwise.
     */
    @Override
    public boolean processTerm(final MutableString term) {
        boolean pleaseIndex = stemmer != null && super.processTerm(term);
        if (pleaseIndex) {
            // do stemming here:
            final String stemmed = stemmer.stripAffixes(term.toString());
            if (stemmed != null && stemmed.length() > 0) {
                term.replace(stemmed);
            } else {
                pleaseIndex = false;
            }
        }
        return pleaseIndex;
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
