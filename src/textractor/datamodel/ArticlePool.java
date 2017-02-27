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

package textractor.datamodel;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Oct 18, 2004
 * Time: 12:40:45 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ArticlePool {
    final ArrayList<ArticleInfo> articleInfos;
    long documentNumberPointer;

    public ArticlePool(){
        articleInfos = new ArrayList<ArticleInfo>();
    }

    public void setEntry(final String filename, final long pmid,
            final long documentNumberRangeLength){
        final ArticleInfo articleInfo = new ArticleInfo();
        articleInfo.setFilename(filename);
        articleInfo.setPmid(pmid);

        final long documentNumberRangeStart = documentNumberPointer;
        articleInfo.setDocumentNumberRangeStart(documentNumberRangeStart);
        articleInfo.setDocumentNumberRangeLength(documentNumberRangeLength);
        documentNumberPointer += documentNumberRangeLength;
        articleInfos.add(articleInfo);
    }

    /**
     * Return the ArticleInfo that includes a given document.
     * @param pmid PubMed id number to get ArticleInfo for
     * @return Article entry that corresponds to the given pubmed id
     */
    public ArticleInfo getEntryByPMID(final long pmid){
        ArticleInfo entry = null;
        for (final ArticleInfo articleInfo : articleInfos) {
            if (articleInfo.getPmid() == pmid) {
                entry = articleInfo;
                break;
            }
        }

        return entry;
    }

    /**
     * Return the ArticleInfo that includes a given document.
     * @param document Document number to get ArticleInfo for
     * @return Article entry that corresponds to the given document
     */
    public ArticleInfo getEntryByDocument(final long document){
        ArticleInfo entry = null;
        for (final ArticleInfo articleInfo : articleInfos) {
            if (articleInfo.getDocumentNumberRangeStart() <= document
                    && (articleInfo.getDocumentNumberRangeStart()
                    + articleInfo.getDocumentNumberRangeLength()) > document) {
                entry = articleInfo;
                break;
            }
        }

        return entry;
    }
}
