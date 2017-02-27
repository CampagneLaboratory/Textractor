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

import it.unimi.dsi.mg4j.util.MutableString;

public final class TermOccurrence implements Comparable<TermOccurrence> {
    private String term;
    private int count;
    private int[] indexedTerm;

    /**
     * Creates a new TermOccurence.
     */
    public TermOccurrence() {
        super();
        // empty constructor for JDO.
    }

    public TermOccurrence(final String term, final int[] indexedTerm, final int count) {
        this.term = term;
        this.count = count;
        this.indexedTerm = indexedTerm;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(final String term) {
        this.term = term;
    }

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public int[] getIndexedTerm() {
        return indexedTerm;
    }

    public void setIndexedTerm(final int[] indexedTerm) {
        this.indexedTerm = indexedTerm;
    }

    // implementation of Comparable
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof TermOccurrence)) {
            return false;
        }

        final TermOccurrence t = (TermOccurrence) o;
        if (this.count != t.getCount()) {
            return false;
        }

        if (this.term == null) {
            return t.getTerm() == null;
        } else {
            return this.term.equals(t.getTerm());
        }
    }

    @Override
    public int hashCode() {
        return 31 * this.term.hashCode() + Integer.toString(this.count).hashCode();
    }

    /**
     * Natural ordering of TermOccurrence - by term count.
     */
    public int compareTo(final TermOccurrence o) {
        int diff = o.getCount() - this.count;
        if (diff != 0) {
            return diff;
        }

        diff = this.term.length() - o.getTerm().length();
        if (diff != 0) {
            return diff;
        }
        return this.term.compareTo(o.getTerm());
    }

    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append(this.term);
        buf.append('|');
        buf.append(this.count);
        return buf.toString();
    }

    public boolean isIncluding(final TermOccurrence termOccurrence) {
        return term.matches("(^|(.+\\s))" + escape(termOccurrence.getTerm()) + "((\\s.+)|$)");
    }

    /**
     * Espace each character of the term. This is required when using regular
     * expressions.
     *
     * @param term Term to escape
     * @return Escaped string where each character of term is prefixed with
     * character '\'.
     */
    private String escape(final String term) {
        final MutableString escapedTerm = new MutableString();
        final int length = term.length();
        for (int i = 0; i < length; i++) {
            escapedTerm.append('[');
            escapedTerm.append(term.charAt(i));
            escapedTerm.append(']');
        }
        return escapedTerm.toString();
    }
}
