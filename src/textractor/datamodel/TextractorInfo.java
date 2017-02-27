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
 * Store information about this textractor system (database and full text
 * indexes).
 * User: Fabien Campagne
 * Date: Jan 31, 2004
 * Time: 11:58:18 AM
 */
public final class TextractorInfo {
    private String caseSensitiveIndexBasename;
    private String stemmedIndexBasename;
    private boolean indexParentheses;

    public TextractorInfo() {
        super();
    }

    public boolean isIndexParentheses() {
        return indexParentheses;
    }

    public void setIndexParentheses(final boolean indexParentheses) {
        this.indexParentheses = indexParentheses;
    }

    public void setStemmedIndexBasename(final String stemmedIndexBasename) {
        this.stemmedIndexBasename = stemmedIndexBasename;
    }

    public void setCaseSensitiveIndexBasename(final String caseSensitiveIndexBasename) {
        this.caseSensitiveIndexBasename = caseSensitiveIndexBasename;
    }

    public String getCaseSensitiveIndexBasename() {
        return caseSensitiveIndexBasename;
    }

    public String getStemmedIndexBasename() {
        return stemmedIndexBasename;
    }
}

