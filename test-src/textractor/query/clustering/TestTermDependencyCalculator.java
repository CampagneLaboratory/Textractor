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

import junit.framework.TestCase;
import textractor.database.DocumentIndexManager;
import textractor.tools.BuildDocumentIndexFromTextDocuments;

/**
 * @author Fabien Campagne
 *         Date: Oct 29, 2006
 *         Time: 11:58:05 AM
 */
public final class TestTermDependencyCalculator extends TestCase {
    private static final String BASENAME = "index/term-dependency-test";
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

    public void testCalculate() {
        TermDependencyCalculator calculator = new TermDependencyCalculator(docManager);
        String characters = "ABCDEFGHIJKLMNOPQURSTVXYZ";
        String[] terms = getTerms(characters);
        TermSimilarityMatrix matrix;
        matrix = calculator.calculate(terms);
        assertNotNull(matrix);
        assertEquals(0f, matrix.getSimilarity("A", "B"));
    }

    public void testCoOccurenceCalculate() {
        TermDependencyCalculator calculator = new TermCoOccurenceCalculator(docManager);
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
        matrix = calculator.calculate(terms);
        assertNotNull(matrix);
        assertEquals(2f, matrix.getSimilarity("A", "B"));
        assertEquals(2f, matrix.getSimilarity("A", "D"));
        assertEquals(3f, matrix.getSimilarity("B", "D"));
    }

    public void testCoOccurenceCalculate2() {
        TermDependencyCalculator calculator = new TermCoOccurenceCalculator(docManager);
        String characters = "ABCDEFKY";
        //   0 1 2 3 4 5
        //  "A A B C D E",
        //  "A B C F I D S",
        //  "A S K X Y N N S S",
        //  "D S B K X U W",
        //  "A",
        //  "B E",

        String[] terms = getTerms(characters);
        TermSimilarityMatrix matrix;
        matrix = calculator.calculate(terms);
        assertNotNull(matrix);
        assertEquals(2f, matrix.getSimilarity("A", "B"));
        assertEquals(2f, matrix.getSimilarity("A", "D"));
        assertEquals(3f, matrix.getSimilarity("B", "D"));
        assertEquals(2f, matrix.getSimilarity("B", "E"));

        assertEquals(1f, matrix.getSimilarity("A", "Y"));
        try {
            assertEquals(4f, matrix.getSimilarity("A", "A"));
            fail("Check that assertions are enabled. An assertion must have failed.");
        } catch (AssertionError e) {
            // OK, assertions are enabled and the code raised one, as it should.
        }
    }

    private String[] getTerms(String characters) {
        char charTerms[] = characters.toCharArray();
        String []terms = new String[charTerms.length];
        int i = 0;

        for (char cTerm : charTerms) {
            terms[i++] = Character.toString(cTerm);
        }
        return terms;
    }
}
