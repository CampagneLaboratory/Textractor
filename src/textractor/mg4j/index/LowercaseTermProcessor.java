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
 * The class name is now confusing. This is really the term processor that
 * implements the protein name transformations used in Lei Shi and
 * Fabien Campagne, BMC Bioinformatics 2004.
 *
 * User: lshi
 * Date: Jun 21, 2004
 * Time: 2:06:15 PM
 */
public final class LowercaseTermProcessor implements TermProcessor {
    private static final LowercaseTermProcessor INSTANCE =
            new LowercaseTermProcessor();

    private LowercaseTermProcessor() {
        super();
    }

    public static TermProcessor getInstance() {
        return INSTANCE;
    }

    public boolean processTerm(final MutableString term) {
        boolean pleaseIndex = true;
        if (term != null) {
            if (term.length() < 1) {
                pleaseIndex = false;
                return pleaseIndex;
            }

            // remove the "." at the end of a word but not for something like " G.", however, do it for digits "0."
            if (term.charAt(term.length() - 1) == '.' &&
                    (term.length() > 2 || Character.isDigit(term.charAt(0)))) {
                term.setLength(term.length() - 1);
            }

            // allow terms with 5 or less letters all in uppercase to stay that way
            if (term.length() >= 2 && term.length() < 6) {
                for (int i = 1; i < term.length(); i++) {
                    if (Character.isUpperCase(term.charAt(i))) {
                        return pleaseIndex;
                    }
                }
            }

            // Do not lowercase if less to three caracters "pH" or "aa"
            // situations, "PH" and "AA" can be refer to proteins.
            if (term.length() >= 3) {
                term.toLowerCase();
            }
        }
        return pleaseIndex;

    }

    public boolean processPrefix(final MutableString prefix) {
	return processTerm(prefix);
    }

    public LowercaseTermProcessor copy() {
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
