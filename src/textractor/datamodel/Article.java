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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.lang.ArrayUtils;
import textractor.database.TextractorManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Article extends ArticleInfo implements Serializable {
    private TermOccurrence[] frequentTerms;
    private long articleNumber;

    private TermPredictionStatistics[] termPredictionStatistics;
    private int[] targetTermOccurenceIndexAll;
    private int[] targetTermOccurenceIndexMulti;
    private int[] targetTermOccurenceIndexCombined;
    private transient MutableString articleTextCache;

    /** The article authors, etc. text to index. */
    private final transient Map<String, Object> additionalFieldsMap;

    /**
     * Create a new article.
     */
    public Article() {
        super();
        this.frequentTerms = new TermOccurrence[0];
        this.additionalFieldsMap = new Object2ObjectOpenHashMap<String, Object>();
    }

    /**
     * Get the text associated with this article.
     *
     * @param tm Textractor Manager used to access the datastore.
     * @return Text associated with this article.
     */
    public String getText(final TextractorManager tm) {
        if (articleTextCache == null) {
            articleTextCache = new MutableString();
            final long documentNumberLowerBound = documentNumberRangeStart - 1;
            final long documentNumberUpperBound =
                documentNumberLowerBound + documentNumberRangeLength;
            final String sfilter =
                "this.documentNumber <= " + documentNumberUpperBound;
            final Iterator<Sentence> it =
                tm.getSentenceIterator(documentNumberLowerBound, sfilter);
            while (it.hasNext()) {
                final Sentence sentence = it.next();
                articleTextCache.append(sentence.getText());
                articleTextCache.append(' ');
            }
        }
        return articleTextCache.toString().trim();
    }

    public long getArticleNumber() {
        return articleNumber;
    }

    public void setArticleNumber(final long articleNumber) {
        this.articleNumber = articleNumber;
    }

    public void setTermPredictionStatistics(
            final TermPredictionStatistics[] stats) {
        this.termPredictionStatistics = stats;
    }

    public void addTermPredictionStatistic(final TermPredictionStatistics stat) {
        final int oldLength;
        if (this.termPredictionStatistics != null) {
            oldLength = this.termPredictionStatistics.length;
        } else {
            oldLength = 0;
        }
        final TermPredictionStatistics[] stats =
            new TermPredictionStatistics[oldLength + 1];
        if (oldLength != 0) {
            System.arraycopy(this.termPredictionStatistics, 0, stats, 0, oldLength);
        }
        stats[oldLength] = stat;
        this.termPredictionStatistics = stats;
    }

    public TermPredictionStatistics[] getTermPredictionStatistics() {
        return this.termPredictionStatistics;
    }

    public void removeTermPredictionStatistics() {
        this.termPredictionStatistics = null;
    }

    /**
     * Sets the most frequent terms.
     * @param terms The terms
     */
    public void setMostFrequentTerms(final TermOccurrence[] terms) {
        this.frequentTerms = terms;
    }

    public void addTermOccurrence(final TermOccurrence to) {
        assert frequentTerms != null : "frequentTerms must not be null";
        final int numTerms = frequentTerms.length;
        final TermOccurrence[] tos = new TermOccurrence[numTerms + 1];
        System.arraycopy(frequentTerms, 0, tos, 0, numTerms);
        tos[numTerms] = to;
        Arrays.sort(tos);
        this.frequentTerms = tos;
    }

    public TermOccurrence getTermOccurrence(final int i) {
        return frequentTerms[i];
    }

    /**
     * Gets the specified number of the most frequent terms in this Article.
     *
     * @param number number of terms to be returned.
     * @return an array of the index positions of the <number> of most frequent
     *         terms.
     */
    public TermOccurrence[] getMostFrequentTerms(final int number) {
        final TermOccurrence[] to = new TermOccurrence[number];
        System.arraycopy(this.frequentTerms, 0, to, 0, number);
        return to;
    }

    /**
     * Returns the specified number of the most frequent terms in this Article,
     * not including terms from the exclusion_list.
     *
     * @param number number of terms to be returned.
     * @param exclusionList terms to be excluded from the terms to be returned.
     * @return an array of the index positions of the number of most frequent
     *         terms.
     */
    public TermOccurrence[] getMostFrequentTerms(final int number,
            final List<String> exclusionList) {
        final TermOccurrence[] to = getMostFrequentTerms(number);
        final int numberTerms = to.length;
        final Collection<TermOccurrence> v =
            new ArrayList<TermOccurrence>();
        for (int x = 0; x < numberTerms; x++) {
            if (!exclusionList.contains(to[x].getTerm())) {
                v.add(to[x]);
            }
        }

        int x = 0;
        final Iterator<TermOccurrence> it = v.iterator();
        final TermOccurrence[] toNoExcluded = new TermOccurrence[v.size()];
        while (it.hasNext()) {
            toNoExcluded[x] = it.next();
            x++;
        }
        return toNoExcluded;
    }

    public TermOccurrence getTermOccurrenceForTerm(final int[] indexedTermSearched) {
        TermOccurrence occurence = null;
        for (final TermOccurrence frequentTerm : frequentTerms) {
            if (Arrays.equals(indexedTermSearched, frequentTerm.getIndexedTerm())) {
                occurence = frequentTerm;
                break;
            }
        }
        return occurence;
    }

    /**
     * Returns the number of most frequent terms.
     *
     * @return the number of most frequent terms stored with this article.
     */
    public int getNumMostFrequentTerms() {
        return frequentTerms.length;
    }

    public void setTargetTermOccurenceIndexAll(final int[] targetTermOccurenceIndexAll) {
        this.targetTermOccurenceIndexAll = targetTermOccurenceIndexAll;
    }

    public int[] getTargetTermOccurenceIndexAll() {
        return this.targetTermOccurenceIndexAll;
    }

    public void setTargetTermOccurenceIndexMulti(
            final int[] targetTermOccurenceIndexMulti) {
        this.targetTermOccurenceIndexMulti = targetTermOccurenceIndexMulti;
    }

    public int[] getTargetTermOccurenceIndexMulti() {
        return this.targetTermOccurenceIndexMulti;
    }

    public void setTargetTermOccurenceIndexCombined(
            final int[] targetTermOccurenceIndexCombined) {
        this.targetTermOccurenceIndexCombined = targetTermOccurenceIndexCombined;
    }

    public int[] getTargetTermOccurenceIndexCombined() {
        return this.targetTermOccurenceIndexCombined;
    }

    /**
     * Get the map of additional fields.
     * @return the map of additional fields
     */
    public Map<String, Object> getAdditionalFieldsMap() {
        return additionalFieldsMap;
    }

    /**
     * Set the value of an indexable field.
     * @param field the field name
     * @param indexableValue the field value
     */
    public void setAdditionalField(final String field, final Object indexableValue) {
        // Enforce additionalField value Object probably being the right type
        // We only want to store String and Long
        if (indexableValue instanceof Long) {
            additionalFieldsMap.put(field, indexableValue);
        } else if (indexableValue instanceof Integer) {
            // Force a Long into the map
            final long value = (Integer) indexableValue;
            additionalFieldsMap.put(field, new Long(value));
        } else if (indexableValue instanceof String) {
            additionalFieldsMap.put(field, indexableValue);
        } else if (indexableValue instanceof CharSequence) {
            additionalFieldsMap.put(field, indexableValue.toString());
        } else {
            throw new IllegalArgumentException(
                    "indexableValue must be Long, Integer, String, or CharSequence.");
        }
    }

    /**
     * String version of this object.
     * @return String version of this object
     */
    @Override
    public String toString() {
        return String.format("Parsed pmid=%s, additionalFields=%s",
                pmid, ArrayUtils.toString(additionalFieldsMap));
    }
}
