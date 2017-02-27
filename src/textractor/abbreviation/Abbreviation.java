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

package textractor.abbreviation;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.Interval;
import it.unimi.dsi.mg4j.util.MutableString;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.docstore.DocumentStoreReader;
import textractor.scoredresult.ScoredResult;
import textractor.scoredresult.ScoredResultComparator;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Finds possible abbreviations when given a long form search term, and returns
 * a result ranked by relevance.
 */
public final class Abbreviation {
    private final WordReader wordReader;

    private final DocumentIndexManager docmanager;
    private final DocumentStoreReader reader;
    private final TermProcessor termProcessor;
    private Int2IntMap frequencyHashMap;

    /**
     * Constructor for Abbreviation searcher. Requires a DocumentIndexManager,
     * from which a new word reader will be created.
     *
     * @param newDocManager
     * @throws IOException
     */
    public Abbreviation(final DocumentIndexManager newDocManager) throws IOException {
        this(newDocManager, null);
    }

    /**
     * Constructore for Abbreviation searcher. Use this method if a
     * DocumentStoreReader already exists for the DocumentIndexManager,
     * newDocManager. Otherwise, use Abbreviations(DocumentIndexManager).
     *
     * @param newDocManager
     * @param docStoreReader
     * @throws IOException
     */
    public Abbreviation(final DocumentIndexManager newDocManager,
                        final DocumentStoreReader docStoreReader) throws IOException {
        docmanager = newDocManager;
        if (docStoreReader == null) {
            reader = new DocumentStoreReader(docmanager);
        } else {
            reader = docStoreReader;
        }

        if (docmanager != null) {
            wordReader = docmanager.getWordReader();
            termProcessor = docmanager.getTermProcessor();
        } else {
            wordReader = null;
            termProcessor = null;
        }

        frequencyHashMap = new Int2IntOpenHashMap();
        frequencyHashMap.defaultReturnValue(-1);
    }


    /**
     * Searches for abbreviations of the form:
     * long form (abbreviation)
     * in the index provided by the docmanager.
     *
     * @param longForm is the String to search for
     * @return an ArrayList of abbreviation terms and scores, wrapped as a ScoredResult.
     * @throws IOException
     */
    public List<ScoredResult> findAbbreviationsForLongForm(final String longForm) throws IOException {
        final ArrayList<ScoredResult> returnList = new ArrayList<ScoredResult>();
        final Set<Integer> abbreviationTermFound = new IntOpenHashSet();
        final StringReader stringReader = new StringReader(longForm);
        wordReader.setReader(stringReader);

        final MutableString word = new MutableString();
        final MutableString nonword = new MutableString();
        final List<Integer> searchTermArrayList = new IntArrayList();
        int termIndex;
        while (wordReader.next(word, nonword)) {
            termProcessor.processTerm(word);
            termIndex = docmanager.findTermIndex(word);
            if (termIndex != DocumentIndexManager.NO_SUCH_TERM) {
                searchTermArrayList.add(termIndex);
            }
        }

        stringReader.close();

        if (searchTermArrayList.size() == 0) {
            return new ArrayList<ScoredResult>();
        }

        final int[] searchTerm = new int[searchTermArrayList.size() + 1];

        int termCount = 0;
        for (final int iteratedSearchTerm : searchTermArrayList) {
            searchTerm[termCount] = iteratedSearchTerm;
            termCount++;
        }

        searchTerm[termCount] = docmanager.findTermIndex("(");
        final int closeParenIndex = docmanager.findTermIndex(")");

        final DocumentIterator consecutiveDocumentIterator =
                docmanager.queryAndExactOrderMg4jNativeWithIntArray(searchTerm);
        int closeIndex;
        final IntArrayList intResult = new IntArrayList();
        frequencyHashMap.clear();
        frequencyHashMap.defaultReturnValue(0);

        while (consecutiveDocumentIterator.hasNext()) {
            final int documentIndex = consecutiveDocumentIterator.nextDocument();

            final Iterator<Interval> intervalIterator =
                    consecutiveDocumentIterator.intervalIterator();

            intResult.size(0);
            reader.document(documentIndex, intResult);
            while (intervalIterator.hasNext()) {
                final Interval interval = intervalIterator.next();
                if ((interval.right + 2) < intResult.size()) {
                    termIndex = intResult.getInt(interval.right + 1);
                    closeIndex = intResult.getInt(interval.right + 2);
                    if (closeIndex == closeParenIndex) {
                        abbreviationTermFound.add(termIndex);
                        //log.debug("Abb: added to abbreviationTermFound " + termIndex);

                        final int oldCount = frequencyHashMap.get(termIndex);
                        frequencyHashMap.put(termIndex, oldCount + 1);

                    }
                }
            }
        }
        consecutiveDocumentIterator.dispose();
        final ScoredResult[] abbreviations = new ScoredResult[abbreviationTermFound.size()];
        int abbreviationCount = 0;

        for (final int index : abbreviationTermFound) {
            final ScoredResult term = new ScoredResult();

            term.setTerm(docmanager.termAsString(index));
            term.setScore(frequencyHashMap.get(index));

            abbreviations[abbreviationCount] = term;
            abbreviationCount++;
        }

        Arrays.sort(abbreviations, new ScoredResultComparator());

        // Return top values
        int topScore = 10;
        if (abbreviations.length < topScore) {
            topScore = abbreviations.length;
        }

        for (int n = 0; n < topScore; n++) {
            // Filter numbers and short strings
            if (abbreviations[n].getTerm().length() > 1) {
                try {
                    Integer.valueOf(abbreviations[n].getTerm());
                } catch (final NumberFormatException e) {
                    returnList.add(abbreviations[n]);
                }
            }
        }

        return returnList;
    }
}
