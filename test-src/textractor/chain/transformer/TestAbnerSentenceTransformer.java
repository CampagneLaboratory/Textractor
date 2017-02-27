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

package textractor.chain.transformer;

import abner.Tagger;
import junit.framework.TestCase;
import textractor.chain.ArticleSentencesPair;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.sentence.SentenceProcessingException;
import textractor.sentence.SentenceTransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestAbnerSentenceTransformer extends TestCase {
    private static String TEXT = "We have identified a transcriptional "
            + "repressor, Nrg1, in a genetic screen designed to reveal "
            + "negative factors involved in the expression of STA1, which "
            + "encodes a glucoamylase.\n\nThe NRG1 gene encodes a 25-kDa C2H2 "
            + "zinc finger protein which specifically binds to two regions in "
            + "the upstream activation sequence of the STA1 gene, as judged "
            + "by gel retardation and DNase I footprinting analyses. "
            + "Disruption of the NRG1 gene causes a fivefold increase in the "
            + "level of the STA1 transcript in the presence of glucose.";

    private static String EXPECTED_TEXT = "We have identified a abnerprotein, "
            + "abnerprotein, in a genetic screen designed to abnerprotein "
            + "involved in the expression of abnerprotein, which encodes a "
            + "abnerprotein.\n\nThe abnerdna encodes a 25-kDa abnerprotein "
            + "which specifically binds to two regions in the abnerdna of the "
            + "abnerdna, as judged by gel retardation and abnerprotein "
            + "footprinting analyses. Disruption of the abnerdna causes a "
            + "fivefold increase in the level of the abnerrna in the presence "
            + "of glucose.";

    public void testTransformer() throws SentenceProcessingException {
        final Article article = new Article();
        final Sentence sentence = new Sentence();
        sentence.setText(TEXT);
        final List<Integer> positions = new ArrayList<Integer>(TEXT.length());
        for (int i = 0; i < TEXT.length(); i++) {
            positions.add(i);
        }
        sentence.setPositions(positions);
        final Collection<Sentence> sentences = new ArrayList<Sentence>();
        sentences.add(sentence);
        final ArticleSentencesPair pair =
                new ArticleSentencesPair(article, sentences);

        final SentenceTransformer transformer = new AbnerSentenceTransformer();
        final ArticleSentencesPair newPair = transformer.transform(pair);
        assertNotNull(newPair);
        final Sentence newSentence = newPair.sentences.iterator().next();
        assertNotNull(newSentence);
        final String newText = newSentence.getText();
        System.out.println(newText);
        assertEquals(EXPECTED_TEXT, newText);

        // TODO - add test for position adjustment
    }

    public void testTransformerWithNoText() throws SentenceProcessingException {
        final Article article = new Article();
        final Sentence sentence = new Sentence();
        final Collection<Sentence> sentences = new ArrayList<Sentence>();
        sentence.setText("");
        sentence.setPositions(new ArrayList<Integer>());
        sentences.add(sentence);
        final ArticleSentencesPair pair =
                new ArticleSentencesPair(article, sentences);

        final SentenceTransformer transformer = new AbnerSentenceTransformer();
        final ArticleSentencesPair newPair = transformer.transform(pair);
        assertNotNull(newPair);
        final Collection<Sentence> newSentences = newPair.sentences;
        assertNotNull(newSentences);
        assertEquals(1, newSentences.size());
        final Sentence newSentence = newSentences.iterator().next();
        assertNotNull(newSentence);
        assertEquals("", newSentence.getText());
        final List<Integer> newPositions = newSentence.getPositions();
        assertNotNull(newPositions);
        assertTrue(newPositions.isEmpty());
    }

    public static void main(String[] args) {
        Tagger t = new Tagger();

        System.out.println("################################################################");
        System.out.println(TEXT);
        System.out.println("################################################################");
        System.out.println(t.tokenize(TEXT));
        System.out.println("################################################################");
        System.out.println(t.tagABNER(TEXT));
        System.out.println("################################################################");
        System.out.println(t.tagIOB(TEXT));
        System.out.println("################################################################");
        System.out.println(t.tagSGML(TEXT));

        System.out.println("################################################################");
        String[][] ents = t.getEntities(TEXT);
        for (int i=0; i<ents[0].length; i++) {
            System.out.println(ents[1][i]+"\t["+ents[0][i]+"]");
        }
        System.out.println();

        System.out.println("################################################################");
        System.out.println("[PROTEIN SEGMENTS]");
        String[] prots = t.getEntities(TEXT,"PROTEIN");
        for (int i=0; i<prots.length; i++) {
            System.out.println(prots[i]);
        }
        System.out.println();
    }

}
