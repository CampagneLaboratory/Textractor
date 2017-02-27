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

import java.io.File;

public class ArticleInfo {
    protected String filename;
    protected long pmid;
    protected long documentNumberRangeStart;
    protected long documentNumberRangeLength;
    /**
     * URL / Link for this article.
     */
    protected String link;

    public ArticleInfo() {
        super();
    }

    public final String getFilename() {
        return filename;
    }

    public final void setFilename(final String filename) {
        this.filename = (new File(filename)).getName();
    }

    public final long getPmid() {
        return pmid;
    }

    public final void setPmid(final long pmid) {
        this.pmid = pmid;
    }

    public final long getDocumentNumberRangeStart() {
        return documentNumberRangeStart;
    }

    public final void setDocumentNumberRangeStart(final long documentNumberRangeStart) {
        this.documentNumberRangeStart = documentNumberRangeStart;
    }

    public final long getDocumentNumberRangeLength() {
        return documentNumberRangeLength;
    }

    public final void setDocumentNumberRangeLength(final long documentNumberRangeLength) {
        this.documentNumberRangeLength = documentNumberRangeLength;
    }

    public final String getID() {
        if (pmid != 0) {
            return Long.toString(pmid);
        } else if (filename != null) {
            return filename;
        } else {
            return "ID-not-set-yet";
        }
    }

    /**
     * Get the link.
     * @return the link.
     */
    public String getLink() {
        return this.link;
    }

    /**
     * Set the link.
     * @param newval the new link value.
     */
    public void setLink(final String newval) {
        this.link = newval;
    }
}
