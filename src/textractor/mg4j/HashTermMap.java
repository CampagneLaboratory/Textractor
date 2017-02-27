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

package textractor.mg4j;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.mg4j.index.TermMap;
import it.unimi.dsi.mg4j.util.MutableString;
import textractor.database.DocumentIndexManager;
import textractor.mg4j.index.TermIterator;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * A term map implementation that should allow fast retrieval. The speed comes at the price
 * of a large amount of memory.
 */
public final class HashTermMap implements TermMap, Serializable {
    private final Map<Integer, CharSequence> index2Term;
    private final Object2IntOpenHashMap<CharSequence> term2Index;

    /**
     * Used during deserialization to verify that objects are compatible.
     */
    public static final long serialVersionUID = 1L;

    /**
     * Construct and populate this term map.
     * Each term in the file provided as input is entered in the map with an index that reflects
     * the order of the term in the input file.
     *
     * @param termFilename Name of the file that contains the terms to initialize this map with.
     */
    public HashTermMap(final String termFilename) throws IOException {
        this(new TermIterator(termFilename), 0);
    }

    /**
     * Construct and populate this term map.
     * Each term in the termIterator  entered in the map with an index that reflects
     * the order of the term in the iterator.
     *
     * @param termIterator Iterator over the termIterator that must be added to this term map.
     */
    public HashTermMap(final TermIterator termIterator, final int startingIndex) throws IOException {
        this();
        int index = startingIndex;

        while (termIterator.hasNext()) {
            // we create a new MutableString for each term because the term is used as a key in a map.
            final MutableString term = new MutableString(termIterator.nextTerm());
            term.compact();

            index2Term.put(index, term);
            term2Index.put(term, index);
            ++index;
        }
        termIterator.close();
    }

    public HashTermMap() {
        super();
        index2Term = new Int2ObjectRBTreeMap<CharSequence>();
        term2Index = new Object2IntOpenHashMap<CharSequence>();
        term2Index.defaultReturnValue(DocumentIndexManager.NO_SUCH_TERM);
    }

    /**
     * Returns the ordinal number corresponding to the given term, or possibly (but not necessarily) -1 if the term was
     * not indexed.
     *
	 * @param term a term.
	 * @return its ordinal number, or possibly (but not necessarily) -1 if the term was not indexed.
	 * @deprecated As of MG4J 1.2, replaced by {@link #getNumber(CharSequence)}.
	 */
	@Deprecated
    public int getIndex(final CharSequence term) {
        return term2Index.getInt(term);
    }

    /**
     * Returns the ordinal number corresponding to the given term, or possibly (but not necessarily) -1 if the term was
     * not indexed.
     *
     * @param term a term.
     * @return its ordinal number, or possibly (but not necessarily) -1 if the term was not indexed.
     */
    public int getNumber(final CharSequence term) {
        return term2Index.getInt(term);
    }

    /**
     * Returns true if this prefix map supports {@linkplain #getTerm(int)
     * term retrieval}.
     *
     * @return true if this prefix map supports {@linkplain #getTerm(int)
     * term retrieval}.
     */
    public boolean hasTerms() {
        return true;
    }

    /**
     * Returns the term corresponding to the given index (optional operation).
     *
     * @param index a term index.
     * @return the corresponding term, or possibly (but not necessarily) <code>null</code> if the term was not indexed.
     */

    public CharSequence getTerm(final int index) {
        return index2Term.get(index);
    }

    public MutableString getTerm(final int i, final MutableString mutableString) {
        mutableString.setLength(0);
        mutableString.append(getTerm(i));
        return mutableString;
    }

    /**
     * Returns the number of terms in this term map.
     * @return the number of terms in this term map.
     */
    public int size() {
        return term2Index.size();
    }
}
