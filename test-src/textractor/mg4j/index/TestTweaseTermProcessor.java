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

/**
 * Validates the @{link TweaseTermProcessor} functionality.
 */
public class TestTweaseTermProcessor extends AbstractTestTermProcessor {
    /**
     * The term processor used in the test.
     */
    private final TweaseTermProcessor termProcessor =
            (TweaseTermProcessor) TweaseTermProcessor.getInstance();

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
}
