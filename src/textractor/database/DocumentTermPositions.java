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

package textractor.database;

import java.util.HashMap;
import java.util.Map;

/**
 * In parallel to TermDocumentPositions, indexed by document
 * <p/>
 * Created by IntelliJ IDEA.
 * User: Lei Shi
 * Date: Feb 2, 2005
 * Time: 11:22:58 AM
 */
public final class DocumentTermPositions {
    private int document;
    private final Map<String, int[]> termPositions;

    public DocumentTermPositions() {
        termPositions = new HashMap<String, int[]>();
    }

    public DocumentTermPositions(final int document) {
        this();
        this.document = document;
    }

    /**
     * Returns positions of the positions of the term in the document.
     *
     * @param term Term to get positions for
     * @return An array of integer. Each integer represents the location of an
     * occurence of the term in the document. Location is given in number of
     * words from the start of the sentence.
     */
    public int[] getPositionsByTerm(final String term) {
        return termPositions.get(term);
    }

    public String[] getTerms() {
        final String[] terms = new String[termPositions.size()];
        return termPositions.keySet().toArray(terms);
    }

    public int getDocument() {
        return document;
    }

    public void setPositions(final String term, final int[] positions) {
        termPositions.put(term, positions);
    }

    public void deleteTerm(final String term) {
        termPositions.remove(term);
    }
}
