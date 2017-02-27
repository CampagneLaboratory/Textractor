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

/**
 * Article specific to storing OTMI data.
 * @author Kevin Dorff
 */
public final class OtmiArticle extends Article {

    /**
     * DOI for this OTMI article.
     */
    private String doi;

    /**
     * Create a new otmi article.
     */
    public OtmiArticle() {
        super();
    }

    /**
     * Get the doi.
     * @return the doi.
     */
    public String getDoi() {
        return this.doi;
    }

    /**
     * Set the doi.
     * @param newval the new doi value.
     */
    public void setDoi(final String newval) {
        this.doi = newval;
        if (this.doi.indexOf("info:doi/") == 0) {
            // Chop off the beginning for display purposes
            this.doi = this.doi.substring(9, this.doi.length());
        }
    }

    @Override
    public String toString() {
        return "OtmiArticle"
            + ":articleNumber=" + getArticleNumber()
            + ":pmid=" + getPmid()
            + ":doi=" + doi
            + ":link=" + link
            + ":filename=" + filename;
    }
}
