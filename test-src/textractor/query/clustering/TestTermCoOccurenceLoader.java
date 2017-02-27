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

package textractor.query.clustering;

import gominer.Fisher;
import junit.framework.TestCase;
import textractor.database.DocumentIndexManager;
import textractor.tools.BuildDocumentIndexFromTextDocuments;

/**
 * @author Fabien Campagne
 *         Date: Oct 29, 2006
 *         Time: 11:58:05 AM
 */
public final class TestTermCoOccurenceLoader extends TestCase {
    private static final String BASENAME = "index/term-dependency-loader-test";

    private DocumentIndexManager docManager;

    protected void setUp() throws Exception {
        final BuildDocumentIndexFromTextDocuments indexBuilder =
                new BuildDocumentIndexFromTextDocuments(BASENAME);
        final String[] documents = {
                "A A B C D E",
                "A B C F I D S",
                "A S K X Y N N S S",
                "D S B K X U W",
                "A",
                "B E",
        };

        indexBuilder.index(documents);

        docManager = new DocumentIndexManager(BASENAME);
    }

    public void testCoOccurenceLoader() {
        TermCoOccurenceLoader loader = new TermCoOccurenceLoader(docManager);
        String characters = "ABD";
        //   0 1 2 3 4 5
        //  "A A B C D E",
        //  "A B C F I D S",
        //  "A S K X Y N N S S",
        //  "D S B K X U W",
        //  "A",
        //  "B E",

        String[] terms = getTerms(characters);
        TermSimilarityMatrix matrix;
        matrix = loader.obtainTermCoOccurenceMatrix(terms);
        assertNotNull(matrix);
        assertEquals(2f, matrix.getSimilarity("A", "B"));
        assertEquals(2f, matrix.getSimilarity("B", "A"));
        assertEquals(2f, matrix.getSimilarity("A", "D"));
        assertEquals(2f, matrix.getSimilarity("D", "A"));
        assertEquals(3f, matrix.getSimilarity("B", "D"));
        assertEquals(3f, matrix.getSimilarity("D", "B"));
    }

    public void testCoOccurenceLoaderThirdTime() {
        TermCoOccurenceLoader loader = new TermCoOccurenceLoader(docManager);
        String characters = "ABD";
        //   0 1 2 3 4 5
        //  "A A B C D E",
        //  "A B C F I D S",
        //  "A S K X Y N N S S",
        //  "D S B K X U W",
        //  "A",
        //  "B E",

        String[] terms = getTerms(characters);
        TermSimilarityMatrix matrix;
        matrix = loader.obtainTermCoOccurenceMatrix(terms);
        assertNotNull(matrix);
        assertEquals(2f, matrix.getSimilarity("A", "B"));
        assertEquals(2f, matrix.getSimilarity("B", "A"));
        assertEquals(2f, matrix.getSimilarity("A", "D"));
        assertEquals(2f, matrix.getSimilarity("D", "A"));
        assertEquals(3f, matrix.getSimilarity("B", "D"));
        assertEquals(3f, matrix.getSimilarity("D", "B"));
    }

    public void testMakeFilenames() {
        String characters = "ABD";
        String[] terms = getTerms(characters);
        assertEquals("Wildcard failure wc=false", "prefix+-A--B--D-.ser", TermCoOccurenceLoader.makeFilename("prefix+", terms, false));
        assertEquals("Wildcard failure wc=true", "prefix+*-A-*-B-*-D-*.ser", TermCoOccurenceLoader.makeFilename("prefix+", terms, true));
    }

    public void testCoOccurenceLoaderAgain() {
        TermCoOccurenceLoader loader = new TermCoOccurenceLoader(docManager);
        String characters = "AD";
        //   0 1 2 3 4 5
        //  "A A B C D E",
        //  "A B C F I D S",
        //  "A S K X Y N N S S",
        //  "D S B K X U W",
        //  "A",
        //  "B E",

        String[] terms = getTerms(characters);
        TermSimilarityMatrix matrix;
        matrix = loader.obtainTermCoOccurenceMatrix(terms);
        assertNotNull(matrix);
        assertEquals(2f, matrix.getSimilarity("A", "D"));
        assertEquals(2f, matrix.getSimilarity("D", "A"));
    }

    public void testProblematicFisher() {
        int nx = 11906;
        int ny = 2364;
        int nxy = 1;
        int N = 16340610;
        Fisher fisher = new Fisher();
        // The below will infinite loop ... hmm
        //double dist = fisher.fisher(nx, nxy, nxy + N, ny);
    }

    private String[] getTerms(final String characters) {
        final char charTerms[] = characters.toCharArray();
        final String[] terms = new String[charTerms.length];
        int i = 0;

        for (char cTerm : charTerms) {
            terms[i++] = Character.toString(cTerm);
        }
        return terms;
    }

}
