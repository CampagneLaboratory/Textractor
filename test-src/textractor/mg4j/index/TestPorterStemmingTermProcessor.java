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
 * Validates the @{link PorterStemmingTermProcessor} functionality.
 */
public class TestPorterStemmingTermProcessor extends AbstractTestTermProcessor {
    /**
     * The term processor used in the test.
     */
    private final PorterStemmingTermProcessor termProcessor =
            (PorterStemmingTermProcessor) PorterStemmingTermProcessor.getInstance();

    /**
     * Get the term processor to be used in the test.
     *
     * @return the term processor to use
     */
    public final TermProcessor getTermProcessor() {
        return termProcessor;
    }

    /**
     * The length at which terms should not be downcased.
     *
     * @return the length at which terms should not be downcased.
     */
    public final int getMaximumTermLengthKeepCase() {
        return termProcessor.getMaximumTermLengthKeepCase();
    }

    /**
     * Validate that the term "Phosphorylation" gets stemmed properly.
     */
    public final void testPhosphorylation() {
        final MutableString term = new MutableString("Phosphorylation");
        boolean processed = termProcessor.processTerm(term);
        assertTrue("Term should be processed", processed);
        assertEquals("Term should be modified and lowercase",
                "phosphoryl", term.toString());
    }

    /**
     * Validate that the term "UbIquItiNAtiOn" gets stemmed properly.
     */
    public final void testUbiquitination() {
        final MutableString term = new MutableString("uBiquItiNAtiOn");
        boolean processed = termProcessor.processTerm(term);
        assertTrue("Term should be processed", processed);
        assertEquals("Term should be modified and lowercase",
                "ubiquitin", term.toString());
    }
}
