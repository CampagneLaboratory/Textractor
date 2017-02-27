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

package textractor.stemming;

import junit.framework.TestCase;
import org.tartarus.snowball.ext.SnowballPorterStemmer;

import java.io.IOException;

/**
 * @author Fabien Campagne
 *         Date: Sep 18, 2006
 *         Time: 12:03:15 PM
 */
public class TestStemmers extends TestCase {
    public final void testPaiceHusk() throws IOException {
        final PaiceHuskStemmer stemmer = new PaiceHuskStemmer(true);
        assertEquals("phosphoryl", stemmer.stripAffixes("phosphorylation"));
        assertEquals("ubiquitin", stemmer.stripAffixes("ubiquitination"));
    }

    public final void testPorter() throws IOException {
        final SnowballPorterStemmer stemmer = new SnowballPorterStemmer();
        stemmer.setCurrent("phosphorylation");
        stemmer.stem();
        assertEquals("phosphoryl", stemmer.getCurrent());
        stemmer.setCurrent("ubiquitination");
        stemmer.stem();
        assertEquals("ubiquitin", stemmer.getCurrent());
    }
}
