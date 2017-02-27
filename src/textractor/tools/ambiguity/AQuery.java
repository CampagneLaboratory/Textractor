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

package textractor.tools.ambiguity;

import java.util.Collection;

/**
 * User: Fabien Campagne
 * Date: Jul 31, 2005
 * Time: 5:42:50 PM
 */
public final class AQuery {
    private String[] query;

    public AQuery(final String[] query) {
        this.query = query;
    }

    public String[] getKeywords() {
        return query;
    }

    public void setQuery(final String[] query) {
        this.query = query;
    }

    Collection<KeywordPairAnalysed> keywordPairs;

    public Collection<KeywordPairAnalysed> getKeywordPairs() {
        return keywordPairs;
    }

    public void setKeywordPairs(final Collection<KeywordPairAnalysed> keywordPairs) {
        this.keywordPairs = keywordPairs;
    }
}
