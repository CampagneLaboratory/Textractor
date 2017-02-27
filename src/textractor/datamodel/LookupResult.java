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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import textractor.database.TermDocumentPositions;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Apr 14, 2004
 * Time: 8:35:05 PM
 * To change this template use File | Settings | File Templates.
 */
public final class LookupResult extends TermDocumentPositions {
    private final String originalTerm;
    private int[] numberOfOccurrences;
    private Object2IntMap<String> occurrenceByArticle;

    /**
     * The TermDocumentPositions term will be changed along the
     * queryAndExactOrder process.
     *
     * @param term
     */
    public LookupResult(final String term) {
        super(term);
        this.originalTerm = term;
    }

    @Override
    public String getTerm(){
        return originalTerm;
    }

    public Object2IntMap<String> getOccurrenceByArticle(final ArticlePool articlePool) {
        if (occurrenceByArticle == null) {
            calculateOccurrenceByArticle(articlePool);
        }
        return occurrenceByArticle;
    }

    public int[] getNumberOfOccurrences() {
        if (numberOfOccurrences == null) {
            calculateNumberOfOccurrences();
        }
        return numberOfOccurrences;
    }

    private void calculateNumberOfOccurrences() {
        numberOfOccurrences = new int[documents.length];
        for (int j = 0; j < documents.length; j++) {
            final int document = documents[j];
            numberOfOccurrences[j] = getPositionsByDocument(document).length;
        }
    }

    public void setNumberOfOccurrences(final int[] numberOfOccurrences) {
        this.numberOfOccurrences = numberOfOccurrences;
    }

    private void calculateOccurrenceByArticle(final ArticlePool articlePool) {
        if (numberOfOccurrences == null) {
            calculateNumberOfOccurrences();
        }

        occurrenceByArticle = new Object2IntOpenHashMap<String>();

        for (int j = 0; j < documents.length; j++) {
            final int document = documents[j];
            final String articleID =
                articlePool.getEntryByDocument(document).getID();
            if (!occurrenceByArticle.containsKey(articleID)) {
                occurrenceByArticle.put(articleID, numberOfOccurrences[j]);
            } else {
                final int temp = occurrenceByArticle.getInt(articleID);
                occurrenceByArticle.put(articleID, temp + numberOfOccurrences[j]);
            }
        }
    }
}
