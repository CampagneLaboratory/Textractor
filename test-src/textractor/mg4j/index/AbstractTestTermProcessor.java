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
import junit.framework.TestCase;
import org.apache.commons.lang.RandomStringUtils;

/**
 * Validates the @{link TweaseTermProcessor} functionality.
 */
public abstract class AbstractTestTermProcessor extends TestCase {
    /**
     * Get the term processor to be used in the test.
     * @return the term processor to use
     */
    public abstract TermProcessor getTermProcessor();

    /**
     * The length at which terms should not be downcased.
     * @return the length at which terms should not be downcased.
     */
    public abstract int getMaximumTermLengthKeepCase();

    /**
     * Validates that null terms are not processed and do not throw
     * exceptions.
     */
    public final void testNullTerm() {
        final TermProcessor termProcessor = getTermProcessor();
        assertFalse("null term should not be processed",
                termProcessor.processTerm(null));
        assertFalse("null prefix should not be processed",
                termProcessor.processPrefix(null));
    }

    /**
     * Validates that empty terms are not processed and do not throw
     * exceptions.
     */
    public final void testEmptyTerm() {
        final TermProcessor termProcessor = getTermProcessor();
        assertFalse("empty term should not be processed",
                termProcessor.processTerm(new MutableString()));
        assertFalse("empty prefix should not be processed",
                termProcessor.processPrefix(new MutableString()));
    }

    /**
     * Validates that short terms (below the threshold defined by the
     * term processor) are processed but not downcased.
     */
    public final void testDowncaseShortTerm() {
        final TermProcessor termProcessor = getTermProcessor();
        // a short random term in uppper case characters
        final int length = getMaximumTermLengthKeepCase();
        final String shortTerm =
                RandomStringUtils.randomAlphabetic(length).toUpperCase();
        final MutableString term = new MutableString(shortTerm);
        boolean processed = termProcessor.processTerm(term);
        assertTrue("Term should be processed", processed);

        // TODO: it's possible that a stemmer will truncate the term
        assertEquals("Term should not be modified", shortTerm, term.toString());
    }

    /**
     * Validates that longer terms (above the threshold defined by the
     * term processor) are processed but not downcased.
     */
    public final void testDowncaseLongTerm() {
        final TermProcessor termProcessor = getTermProcessor();
        // a long random term in uppper case characters
        final String longTerm = RandomStringUtils.randomAlphabetic(
                getMaximumTermLengthKeepCase() + 1).toUpperCase();
        final MutableString term = new MutableString(longTerm);
        boolean processed = termProcessor.processTerm(term);
        assertTrue("Term should be processed", processed);

        // the term may have been truncated, but should only be lowercase
        for (char c : term.array()) {
            assertTrue("Term should only contain lowercase characters",
                    Character.isLowerCase(c));
        }
    }
}
