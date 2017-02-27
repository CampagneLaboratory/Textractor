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

package textractor.database;

import junit.framework.TestCase;
import textractor.tools.BuildDocumentIndexFromTextDocuments;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabien Campagne
 *         Date: Mar 3, 2007
 *         Time: 2:00:54 PM
 */
public class TestInMemoryIndex extends TestCase {
    public void testSkipIndex() throws Exception {
        final String basename = "index/skip-test";
        final String sentence1 = "But, the essential character of Asp-162 and its proximity to site 4 would make it seem likely that sites 3 and 4 are both part of a larger nucleoside-binding or recognition domain for HSV-TKs.";
        final String sentence2 = "The homology between the DRS motif of dGK/dAK and the DRH of HSV-TKs, although very limited in size, suggested the possible importance of these residues in some aspect of nucleoside binding and/or catalysis in Lactobacillus dGK/dAK.";
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(basename);

        final List<CharSequence> textCollection = new ArrayList<CharSequence>();
        textCollection.add(sentence1);
        textCollection.add(sentence2);
        indexBuilder.index(textCollection);

        final DocumentIndexManager indexManager = new DocumentIndexManager(basename + "?inmemory=1");
        assertEquals(basename + "-text", indexManager.getBasename());
        // TODO??  assertTrue(indexManager.getIndex() instanceof SkipInMemoryIndex);
        indexManager.close();
    }
}
