/*
 * Copyright (C) 2005-2009 Institute for Computational Biomedicine,
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

package textractor.parsers;

import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.sentence.SentenceProcessingException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Links the PubmedExtractor to the loader.
 * Date: Nov 4, 2005
 * Time: 5:25:39 PM
 */
public final class PubmedLoadExtractor extends PubmedExtractor {
    /** Used to log debug and informational messages. */
    private static final Log LOG = LogFactory.getLog(PubmedLoadExtractor.class);

    private String filename;
    private PubmedLoader loader;

    public PubmedLoadExtractor(final PubmedLoader loader,
                               final String filename) {
        super();
        this.filename = filename;
        this.loader = loader;
    }

    @Override
    public boolean processAbstractText(final MutableString pmid,
        final MutableString title, final MutableString text,
        final Map<String, Object> additionalFieldsMap)
            throws IOException, SentenceProcessingException {
        if (title.length() > 0 || text.length() > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loading article " + pmid.toString());
            }
            loader.convert(pmid, title, text, additionalFieldsMap, filename);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void processNoticeOfRetraction(final MutableString pmid,
                                          final List<String> retractedPmids,
                                          final boolean createArticle) {
        if (LOG.isDebugEnabled()) {
            LOG.info("processing notices of extraction for " + pmid + ", "
                    + ArrayUtils.toString(retractedPmids));
        }
        if (createArticle) {
            loader.createArticle(Long.parseLong(pmid.toString()), filename);
        }

        for (final String retractedPmid : retractedPmids) {
            loader.removeArticle(retractedPmid);
        }
    }
}
