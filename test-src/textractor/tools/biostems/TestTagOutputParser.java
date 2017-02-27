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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * Created by IntelliJ IDEA.
 * User: Fabien Campagne
 * Date: May 25, 2006
 * Time: 12:32:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestTagOutputParser extends TestCase {
    private String input =
            "0       1       <epsilon>\n" +
                    "1       2       cp      2.9931221\n" +
                    "2       3       at      2.56630564\n" +
                    "3       4       rt      1.60278344\n" +
                    "4       5       <epsilon>\n" +
                    "5       2.47692585\n" +
                    "0       1       <epsilon>\n" +
                    "1       2       cp      2.9931221\n" +
                    "2       3       ap      2.00122237\n" +
                    "3       4       rt      2.35288715\n" +
                    "4       5       tt      2.54501653\n" +
                    "5       6       <epsilon>\n" +
                    "6       1.77396262\n" +
                    "0       1       <epsilon>\n" +
                    "1       2       cp      2.9931221\n" +
                    "2       3       at      2.56630564\n" +
                    "3       4       rt      1.60278344\n" +
                    "4       5       dt      2.77674031\n" +
                    "5       6       <epsilon>\n" +
                    "6       1.42673874\n" +
                    "0       1       <epsilon>\n" +
                    "1       2       bp      3.37163973\n" +
                    "2       3       ip      2.27594686\n" +
                    "3       4       nt      3.19147539\n" +
                    "4       5       dt      2.32279706\n" +
                    "5       6       es      2.44171834\n" +
                    "6       7       ds      2.58322692\n" +
                    "7       8       <epsilon>\n" +
                    "8       0.0508319475\n" +
                    "0       1       <epsilon>\n" +
                    "1       2       bp      3.37163973\n" +
                    "2       3       ip      2.27594686\n" +
                    "3       4       nt      3.19147539\n" +
                    "4       5       dt      2.32279706\n" +
                    "5       6       is      2.85219049\n" +
                    "6       7       ns      1.48494494\n" +
                    "7       8       gs      1.6556747\n" +
                    "8       9       <epsilon>\n" +
                    "9       0.138159573\n" +
                    "0       1       <epsilon>\n" +
                    "1       2       pt      3.64474845\n" +
                    "2       3       ht      2.34677982\n" +
                    "3       4       os      3.7114141\n" +
                    "4       5       ss      2.53530121\n" +
                    "5       6       <epsilon>       0.623690724\n" +
                    "6       7       pp      2.98842692\n" +
                    "7       8       hp      3.03875422\n" +
                    "8       9       ot      2.4575274\n" +
                    "9       10      kt      4.29229116\n" +
                    "10      11      it      1.88099158\n" +
                    "11      12      nt      1.00034833\n" +
                    "12      13      as      3.4350853\n" +
                    "13      14      ss      2.84538269\n" +
                    "14      15      es      1.56842196\n" +
                    "15      16      ss      2.30806231\n" +
                    "16      17      <epsilon>\n" +
                    "17      0.151531026\n" +

                    "0       1       <epsilon>\n" +
                    "1       2       pp      2.98842692\n" +
                    "2       3       hp      3.03875422\n" +
                    "3       4       ot      2.4575274\n" +
                    "4       5       st      2.46773934\n" +
                    "5       6       ps      4.4673624\n" +
                    "6       7       hs      3.41008782\n" +
                    "7       8       os      2.32745314\n" +
                    "8       9       <epsilon>       1.03706849\n" +
                    "9       10      ft      4.73978806\n" +
                    "10      11      rt      2.1243248\n" +
                    "11      12      ut      3.16599941\n" +
                    "12      13      ks      6.10737658\n" +
                    "13      14      <epsilon>       1.40613091\n" +
                    "14      15      tp      3.31750679\n" +
                    "15      16      op      2.72416925\n" +
                    "16      17      kt      4.07844687\n" +
                    "17      18      it      1.6809448\n" +
                    "18      19      nt      1.00034833\n" +
                    "19      20      as      3.4350853\n" +
                    "20      21      ss      2.84538269\n" +
                    "21      22      es      1.56842196\n" +
                    "22      23      <epsilon>\n" +
                    "23      0.481596678";

    public void testParser() throws IOException {
        final StringReader reader = new StringReader(input);
        TagOutputParser parser = new TagOutputParser(new BufferedReader(reader));
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("cp at rt"), parser.readTags());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("cp ap rt tt"), parser.readTags());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("cp at rt dt"), parser.readTags());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("bp ip nt dt es ds"), parser.readTags());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("bp ip nt dt is ns gs"), parser.readTags());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("pt ht os ss pp hp ot kt it nt as ss es ss"), parser.readTags());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("pp hp ot st ps hs os ft rt ut ks tp op kt it nt as ss es"), parser.readTags());
        assertFalse(parser.hasNext());

        parser = new TagOutputParser(new BufferedReader(new StringReader(input)));
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("car"), parser.getWord());
        assertEquals(new MutableString("c"), parser.getPrefix());
        assertEquals(new MutableString("ar"), parser.getStem());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("cart"), parser.getWord());

        assertEquals(new MutableString("ca"), parser.getPrefix());
        assertEquals(new MutableString("rt"), parser.getStem());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("card"), parser.getWord());

        assertEquals(new MutableString("c"), parser.getPrefix());
        assertEquals(new MutableString("ard"), parser.getStem());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("binded"), parser.getWord());

        assertEquals(new MutableString("bi"), parser.getPrefix());
        assertEquals(new MutableString("nd"), parser.getStem());
        assertEquals(new MutableString("ed"), parser.getSuffix());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("bp ip nt dt is ns gs"), parser.readTags());
        assertEquals(new MutableString("binding"), parser.getWord());

        assertEquals(new MutableString("bi"), parser.getPrefix());
        assertEquals(new MutableString("nd"), parser.getStem());
        assertEquals(new MutableString("ing"), parser.getSuffix());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("pt ht os ss pp hp ot kt it nt as ss es ss"), parser.readTags());
        assertEquals(new MutableString("phosphokinases"), parser.getWord());

        assertEquals(new MutableString("ph"), parser.getPrefix());
        assertEquals(new MutableString("phokin"), parser.getStem());
        assertEquals(new MutableString("osases"), parser.getSuffix());
        assertTrue(parser.hasNext());
        assertEquals(new MutableString("phosphofruktokinase"), parser.getWord());
        assertEquals(new MutableString("pp hp ot st ps hs os ft rt ut ks tp op kt it nt as ss es"), parser.readTags());
        assertEquals(new MutableString("phto"), parser.getPrefix());
        assertEquals(new MutableString("osfrukin"), parser.getStem());
        assertEquals(new MutableString("fru"), parser.getFirstLongestStem());
        assertEquals(new MutableString("kin"), parser.getLastLongestStem());
        assertEquals(new MutableString("phokase"), parser.getSuffix());
        assertFalse(parser.hasNext());
    }


}
