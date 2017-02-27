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

package textractor.tools;

import it.unimi.dsi.mg4j.util.MutableString;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * User: Fabien Campagne
 * Date: Jun 6, 2004
 * Time: 2:44:48 PM
 */
public class TestSentenceSplitter extends TestCase {

    public void testSplitWithoutPunctuationSpacing() {
        final String[] inputs = {
                "It was 20 years ago today, Sgt Pepper told his band to play.",
                "They've been going in and out of style, but they're guaranteed to raise a smile.",
                "So may I introduce to you, the act you’ve known for all these years.",
                "Sgt. Pepper’s Lonely Hearts Club Band."
        };

        final StringBuffer input = new StringBuffer();
        for (final String string : inputs) {
            input.append(' ');
            input.append(string);
        }

        final SentenceSplitter splitter = new DefaultSentenceSplitter();
        final Iterator<MutableString> sentenceIterator =
                splitter.split(input.toString(), false);
        int count = 0;
        while (sentenceIterator.hasNext()) {
            final MutableString sentence = sentenceIterator.next();
            assertEquals(inputs[count], sentence.toString());
            count++;
        }
    }

    /**
     * Split the text and collect its sentences in a list.
     *
     * @param splitter The splitter that should handle the text
     * @param text     English text.
     * @return A list that contains the sentences predicted in the text.
     */
    private Collection<MutableString> splitAndCollect(
            final SentenceSplitter splitter, final String text) {
        final Collection<MutableString> textOfSentences =
                new ArrayList<MutableString>();
        final Iterator<MutableString> it = splitter.split(text);
        while (it.hasNext()) {
            final MutableString sequence = it.next();
            textOfSentences.add(sequence);
        }
        return textOfSentences;
    }

    public void testSplit() {
        String input = "A long long long long long long long long First sentence. Second sentence.";
        final SentenceSplitter splitter = new DefaultSentenceSplitter();
        Collection<String> textSplits =
                convertToStrings(splitAndCollect(splitter, input));
        assertTrue(textSplits.contains("A long long long long long long long long First sentence."));
        assertTrue(textSplits.contains("Second sentence."));
        assertFalse(textSplits.contains("A long long long long long long long long First sentence. Second sentence."));

        input = "First sentence Smith A.B. Same sentence";
        textSplits = convertToStrings(splitAndCollect(splitter, input));
        assertFalse(textSplits.contains("First sentence Smith A."));
        assertFalse(textSplits.contains("First sentence Smith A.B."));
        assertTrue(textSplits.contains(input));

        input = "N. de Geest, E. Bonten, L. Mann, J. de Sousa-Hitzler, C. Hahn, and A. d'Azzo";
        textSplits = convertToStrings(splitAndCollect(splitter, input));
        assertFalse(textSplits.contains("N."));
        assertFalse(textSplits.contains("N. de Geest , E."));
        assertFalse(textSplits.contains("N. de Geest , E. Bonten , L. Mann , J."));
        assertFalse(textSplits.contains("N. de Geest , E. Bonten , L. Mann , J. de Sousa-Hitzler , C."));
        assertFalse(textSplits.contains("N. de Geest , E. Bonten , L. Mann , J. de Sousa-Hitzler , C. Hahn, and A."));
        assertTrue(textSplits.contains("N. de Geest , E. Bonten , L. Mann , J. de Sousa-Hitzler , C. Hahn , and A. d'Azzo"));

        input = "Cells were harvested immediately in 200 \u00B5l of ice-cold lysis buffer containing 0.1% (v/v) 2-mercaptoethanol, 0.01% (v/v) Triton X-100, 1 mM EDTA, 1 mM EGTA, 10 mM Tris (pH 7.4), 0.2 mM sodium vanadate, and 0.2 mM phenylmethylsulfonyl fluoride.";
        textSplits = convertToStrings(splitAndCollect(splitter, input));
        assertFalse(textSplits.contains("Cells were harvested immediately in 200 \u00B5l of ice-cold lysis buffer containing 0"));
        assertFalse(textSplits.contains("Cells were harvested immediately in 200 \u00B5l of ice-cold lysis buffer containing 0.1% (v/v) 2-mercaptoethanol , 0"));
        assertFalse(textSplits.contains("Cells were harvested immediately in 200 \u00B5l of ice-cold lysis buffer containing 0.1% (v/v) 2-mercaptoethanol , 0.01% (v/v) Triton X-100 , 1 mM EDTA , 1 mM EGTA , 10 mM Tris (pH 7"));
        assertFalse(textSplits.contains("Cells were harvested immediately in 200 \u00B5l of ice-cold lysis buffer containing 0.1% (v/v) 2-mercaptoethanol , 0.01% (v/v) Triton X-100 , 1 mM EDTA , 1 mM EGTA , 10 mM Tris (pH 7.4) , 0"));
        assertFalse(textSplits.contains("Cells were harvested immediately in 200 \u00B5l of ice-cold lysis buffer containing 0.1% (v/v) 2-mercaptoethanol , 0.01% (v/v) Triton X-100 , 1 mM EDTA , 1 mM EGTA , 10 mM Tris (pH 7.4) , 0.2 mM sodium vanadate , and 0"));
        assertTrue(textSplits.contains("Cells were harvested immediately in 200 \u00B5l of ice-cold lysis buffer containing 0.1% (v/v) 2-mercaptoethanol , 0.01% (v/v) Triton X-100 , 1 mM EDTA , 1 mM EGTA , 10 mM Tris (pH 7.4) , 0.2 mM sodium vanadate , and 0.2 mM phenylmethylsulfonyl fluoride."));

        input = "N. de Geest, E. Bonten, L. Mann, J.\n\n\rde Sousa-Hitzler, C. Hahn, and A. d'Azzo";
        textSplits = convertToStrings(splitAndCollect(splitter, input));
        assertFalse(textSplits.contains("N."));
        assertFalse(textSplits.contains("N. de Geest , E."));
        assertFalse(textSplits.contains("N. de Geest , E. Bonten , L. Mann , J."));
        assertTrue(textSplits.contains("N. de Geest , E. Bonten , L. Mann , J. de Sousa-Hitzler , C. Hahn , and A. d'Azzo"));

        input = "Therefore, the R79K substitution seems to \r\nhave a greater effect on dGuo binding than on that of dAdo, but \r\ndGK modification appears to produce a stimulatory conformational effect on \r\nthe opposite subunit, resembling the known unidirectional activation of dAK by either dGuo or dGTP.";
        textSplits = convertToStrings(splitAndCollect(splitter, input));
        assertFalse(textSplits.contains("Therefore , the R79K substitution seems to "));
        assertFalse(textSplits.contains("Therefore , the R79K substitution seems to \r\nhave a greater effect on dGuo binding than on that of dAdo , but "));
        assertFalse(textSplits.contains("Therefore , the R79K substitution seems to \r\nhave a greater effect on dGuo binding than on that of dAdo , but \r\ndGK modification appears to produce a stimulatory conformational effect on "));
        assertTrue(textSplits.contains("Therefore , the R79K substitution seems to have a greater effect on dGuo binding than on that of dAdo , but dGK modification appears to produce a stimulatory conformational effect on the opposite subunit , resembling the known unidirectional activation of dAK by either dGuo or dGTP."));

        input = "Asp-212 resides in the same helix (helix G) as Gly-231. A\r\nconformational change in helix G introduced by the mutation G231C can lead to a different interaction of the Asp-212 side chain with the neighboring residues in R82A/G231C in such a way that the side chain of Asp-212 no longer participates in the complex counterion";
        textSplits = convertToStrings(splitAndCollect(splitter, input));
        assertTrue(textSplits.contains("A conformational change in helix G introduced by the mutation G231C can lead to a different interaction of the Asp-212 side chain with the neighboring residues in R82A/G231C in such a way that the side chain of Asp-212 no longer participates in the complex counterion"));

        input = "As shown, AICD, but not AICD T668A, was phosphorylated by Cdc2 (Supplementary Fig. S1b). Notably, Pin1 bound to Cdc2-phosphorylated AICD, but not AICDT668A, and the binding was abolished by dephosphorylation (Fig. 1e).";
        textSplits = convertToStrings(splitAndCollect(splitter, input));
        assertFalse(textSplits.contains("As shown, AICD, but not AICD T668A, was phosphorylated by Cdc2 (Supplementary Fig."));
        assertTrue(textSplits.contains("As shown , AICD , but not AICD T668A , was phosphorylated by Cdc2 (Supplementary Fig. S1b)."));
        assertTrue(textSplits.contains("Notably , Pin1 bound to Cdc2-phosphorylated AICD , but not AICDT668A , and the binding was abolished by dephosphorylation (Fig. 1e)."));
    }

    private Collection<String> convertToStrings(final Collection<MutableString> textSplits) {
        final Collection<String> result = new ArrayList<String>();
        for (final MutableString mutableString : textSplits) {
            result.add(mutableString.toString().intern());
        }
        return result;
    }

    public void testParagraphSplitter() {
        final String textToSplit1 = "hello paraboundary another short paragraph paraboundary and a third.";
        final SentenceSplitter pSplitter = new ParagraphSplitter(new MutableString("paraboundary"));
        final Iterator<MutableString> paragraphs = pSplitter.split(textToSplit1);
        assertNotNull(paragraphs);
        assertTrue(paragraphs.hasNext());
        assertEquals("hello ", paragraphs.next().toString());
        assertTrue(paragraphs.hasNext());
        assertEquals(" another short paragraph ", paragraphs.next().toString());
        assertTrue(paragraphs.hasNext());
        assertEquals(" and a third.", paragraphs.next().toString());
        assertFalse(paragraphs.hasNext());
    }

    public void testNullSplitter() {
        final String textToSplit =
                "How now brown cow. The quick brown fox jumps over the lazy dog.";
        final SentenceSplitter splitter = new NullSentenceSplitter();
        final Iterator<MutableString> splitText = splitter.split(textToSplit);
        assertNotNull(splitText);
        assertTrue(splitText.hasNext());
        assertEquals(textToSplit, splitText.next().toString());
        assertFalse(splitText.hasNext());
    }
}
