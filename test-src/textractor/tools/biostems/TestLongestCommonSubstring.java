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

package textractor.tools.biostems;

import it.unimi.dsi.mg4j.util.MutableString;
import junit.framework.TestCase;

/**
 * User: Fabien Campagne
 * Date: May 2, 2006
 * Time: 10:58:38 AM
 */
public class TestLongestCommonSubstring extends TestCase {
    public void testLCS() {
        final LongestCommonSubstring lcs = new LongestCommonSubstring(100);
        assertEquals(new MutableString("bon"), lcs.longestSubstring("bon", "bonjour"));
        assertEquals(new MutableString("11222233323"), lcs.longestSubstring("aasas11222233323sdsdsd", "-11222233323kdkd"));

    }
}
