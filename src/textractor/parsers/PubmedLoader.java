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

package textractor.parsers;

import it.unimi.dsi.mg4j.util.MutableString;
import textractor.datamodel.Article;
import textractor.sentence.SentenceProcessingException;

import java.util.Map;

/**
 * Specifies methods needed to load and process text articles from PubMed.
 */
public interface PubmedLoader {
    /**
     * "Convert" raw data from pubmed articles into internal textractor
     * representation for further processing.
     *
     * @param pmid PubMed identifier for the article to be processed
     * @param title Title of the article
     * @param text Text of the article to be processed
     * @param additionalFieldsMap additional fields to index
     * @param filename Original name of the file that contained the article
     * @throws SentenceProcessingException if there is a problem parsing
     * the text.
     */
    void convert(final MutableString pmid, final MutableString title,
            final MutableString text, final Map<String, Object> additionalFieldsMap,
            final String filename) throws SentenceProcessingException;

    /**
     * Create an {@link Article} using the given parameters.
     * @param pmid Pubmed identifier for the article.
     * @param filename Filename that the article came from
     * @return A new article.
     */
    Article createArticle(final long pmid, final String filename);

    void removeArticle(final String pmid);
}
