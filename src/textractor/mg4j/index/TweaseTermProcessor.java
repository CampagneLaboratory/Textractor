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

package textractor.mg4j.index;

import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.util.MutableString;

/**
 * @author Fabien Campagne
 *         Date: Oct 11, 2006
 *         Time: 11:49:56 AM
 */
public class TweaseTermProcessor implements TermProcessor {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 2L;

    private static final TermProcessor INSTANCE = new TweaseTermProcessor();

    /**
     * Terms shorter than value are never downcased. Longer terms always are.
     */
    private int maximumTermLengthKeepCase = 4;

    /**
     * Create a new {@link TermProcessor}.
     */
    protected TweaseTermProcessor() {
        super();
    }

    public static TermProcessor getInstance() {
        return INSTANCE;
    }

    /**
     * Processes the given term, leaving the result in the same mutable string.
     *
     * @param term a mutable string containing the term to be processed,
     * or <code>null</code>.
     * @return true if the term is not <code>null</code> and should be indexed,
     * false otherwise.
     */
    public boolean processTerm(final MutableString term) {
        final boolean pleaseIndex;
        if (term == null || term.length() < 1) {
            pleaseIndex = false;
        } else {
            if (term.length() > maximumTermLengthKeepCase) {
                term.toLowerCase();
            }
            pleaseIndex = true;
        }
        return pleaseIndex;
    }

    /**
     * Processes the given prefix, leaving the result in the same mutable
     * string.
     *
     * @param prefix a mutable string containing a prefix to be processed,
     * or <code>null</code>.
     * @return true if the prefix is not <code>null</code> and there might be
     * an indexed word starting with <code>prefix</code>, false otherwise.
     */
    public boolean processPrefix(final MutableString prefix) {
        return processTerm(prefix);
    }

    /**
     * Terms shorter than value are never downcased. Longer terms always are.
     * @param value maximumTermLengthKeepCase
     */
    public void setMaximumTermLengthKeepCase(final int value) {
        this.maximumTermLengthKeepCase = value;
    }

    /**
     * Terms shorter than value are never downcased. Longer terms always are.
     * @return maximumTermLengthKeepCase
     */
    public int getMaximumTermLengthKeepCase() {
        return maximumTermLengthKeepCase;
    }

    public TermProcessor copy() {
        return this;
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
