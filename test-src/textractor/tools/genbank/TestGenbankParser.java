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

package textractor.tools.genbank;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * User: campagne
 * Date: Aug 4, 2005
 * Time: 3:06:37 PM
 */
public class TestGenbankParser extends TestCase {
    public void testParse() throws IOException {
        final StringReader sr = new StringReader(entries);
        final GenbankParser parser = new GenbankParser(new BufferedReader(sr));
        assertTrue(parser.hasNextEntry());
        assertNotNull(parser.getDefinition());
        assertTrue(parser.getDefinition().contains("T3107 MVAT4 bloodstream form of serodeme WRATat1.1 Trypanosoma"));
        assertTrue(parser.getDefinition().contains("brucei rhodesiense cDNA 5' similar to (U00048) [Caenorhabditis"));
        assertTrue(parser.getDefinition().contains("elegans], mRNA sequence"));
        assertEquals(parser.getOrganism(), "Trypanosoma brucei rhodesiense");

        assertNotNull(parser.getFeatures());
        assertNotNull(parser.getAccessionCode());
        assertEquals("AA003462", parser.getAccessionCode());
        parser.next();
        assertTrue(parser.hasNextEntry());
        assertNotNull(parser.getDefinition());
        assertTrue(parser.getDefinition().contains("EST936541 Field isolate cDNA library Plasmodium vivax cDNA clone"));
        assertTrue(parser.getDefinition().contains("PVMHK04 5' end, mRNA sequence"));

        assertNotNull(parser.getFeatures());
        assertTrue(parser.getFeatures().contains(" 1A and Sfi 1B sites. Mass excision of library "));
        assertEquals(parser.getOrganism(), "Plasmodium vivax");
        assertNotNull(parser.getAccessionCode());
        assertEquals("CX018222", parser.getAccessionCode());
        parser.next();
        assertFalse(parser.hasNextEntry());
    }

    private final String entries = "LOCUS       AA003462                 391 bp    mRNA    linear   EST 12-AUG-1996\n" +
            "DEFINITION  T3107 MVAT4 bloodstream form of serodeme WRATat1.1 Trypanosoma\n" +
            "            brucei rhodesiense cDNA 5' similar to (U00048) [Caenorhabditis\n" +
            "            elegans], mRNA sequence.\n" +
            "ACCESSION   AA003462\n" +
            "VERSION     AA003462.1  GI:1446937\n" +
            "KEYWORDS    EST.\n" +
            "SOURCE      Trypanosoma brucei rhodesiense\n" +
            "  ORGANISM  Trypanosoma brucei rhodesiense\n" +
            "            Eukaryota; Euglenozoa; Kinetoplastida; Trypanosomatidae;\n" +
            "            Trypanosoma.\n" +
            "REFERENCE   1  (bases 1 to 391)\n" +
            "  AUTHORS   Djikeng,A., Donelson,J.E. and Majiwa,P.A.O.\n" +
            "  TITLE     Generation of expressed sequence tags as physical landmarks in the\n" +
            "            genome of Trypanosoma brucei\n" +
            "  JOURNAL   Unpublished (1996)\n" +
            "COMMENT     Contact: Majiwa PAO\n" +
            "            Molecular Biology Unit\n" +
            "            International Livestock Research Institute\n" +
            "            P.O. Box 30709, Nairobi, Kenya\n" +
            "            Tel: 254-2 630743\n" +
            "            Fax: 254-2 631499\n" +
            "            Email: p.majiwa@cgnet.com\n" +
            "            Seq primer: T3 primer.\n" +
            "FEATURES             Location/Qualifiers\n" +
            "     source          1..391\n" +
            "                     /organism=\"Trypanosoma brucei rhodesiense\"\n" +
            "                     /mol_type=\"mRNA\"\n" +
            "                     /sub_species=\"rhodesiense\"\n" +
            "                     /db_xref=\"taxon:31286\"\n" +
            "                     /clone_lib=\"MVAT4 bloodstream form of serodeme WRATat1.1\"\n" +
            "                     /note=\"Vector: Lambda ZAP II (Stratagene); Site_1: EcorI;\n" +
            "                     Site_2: XhoI; The mRNA was purified from a cloned\n" +
            "                     population of bloodstream trypanosomes reexpressing the\n" +
            "                     MVAT4 metacyclic variant surface glycoprotein (VSG). A\n" +
            "                     unidirectional oligo dT-primed EcoRI/XhoI cDNA library was\n" +
            "                     constructed in lambda ZAP II (Stratagene).\"\n" +
            "ORIGIN\n" +
            "        1 aacaaccaca tgatgttagg agagattagt cattaccctg atccgccgca aacatccctt\n" +
            "       61 gttcattcaa catcccatag cttgcaagta ccttaattac agtctcggag gcaaaatcgt\n" +
            "      121 cagttccacc aaactcatca aacccaataa tactgtggaa agtgttcttg ttctctacta\n" +
            "      181 gcataagcgt cggcagcatc agaacattaa atctttccgc aagagaaggt attctctcca\n" +
            "      241 catcaacata gcagaatcgc gtttcgaaat gttgaggaag aaaaaaaaaa aaaagagcaa\n" +
            "      301 caaaatgact cgcgctggat ttaagggtaa ggtgctcggc aaggaaaaga aacttgcgct\n" +
            "      361 gcttgaggcc cgaagaaggc ggctgaagtc g\n" +
            "//\n" +
            "LOCUS       CX018222                 843 bp    mRNA    linear   EST 01-JAN-2005\n" +
            "DEFINITION  EST936541 Field isolate cDNA library Plasmodium vivax cDNA clone\n" +
            "            PVMHK04 5' end, mRNA sequence.\n" +
            "ACCESSION   CX018222\n" +
            "VERSION     CX018222.1  GI:56957263\n" +
            "KEYWORDS    EST.\n" +
            "SOURCE      Plasmodium vivax (malaria parasite P. vivax)\n" +
            "  ORGANISM  Plasmodium vivax\n" +
            "            Eukaryota; Alveolata; Apicomplexa; Haemosporida; Plasmodium.\n" +
            "REFERENCE   1  (bases 1 to 843)\n" +
            "  AUTHORS   Carlton,J.M. and Cui,L.\n" +
            "  TITLE     A survey of genes in Plasmodium vivax by EST sequencing\n" +
            "  JOURNAL   Unpublished (2004)\n" +
            "COMMENT     Contact: Jane Carlton\n" +
            "            Parasite Genomics Group\n" +
            "            The Institute for Genomic Research\n" +
            "            9712 Medical Center Drive, Rockville, MD 20850, USA\n" +
            "            Tel: 301-530-9319\n" +
            "            Fax: 301-838-0208\n" +
            "            Email: carlton@tigr.org\n" +
            "            Seq primer: TI.\n" +
            "FEATURES             Location/Qualifiers\n" +
            "     source          1..843\n" +
            "                     /organism=\"Plasmodium vivax\"\n" +
            "                     /mol_type=\"mRNA\"\n" +
            "                     /strain=\"Field isolate\"\n" +
            "                     /db_xref=\"taxon:5855\"\n" +
            "                     /clone=\"PVMHK04\"\n" +
            "                     /clone_lib=\"Field isolate cDNA library\"\n" +
            "                     /note=\"Vector: Lambda TriplEx2; Site_1: Sfi 1A; Site_2:\n" +
            "                     Sfi 1B; Plasmodium vivax field isolate cDNA library made\n" +
            "                     in lambda TriplEx2. Inserts cloned unidirectionally in the\n" +
            "                     Sfi 1A and Sfi 1B sites. Mass excision of library produced\n" +
            "                     inserts in pTriplEx2 plasmid. Inserts sequenced from\n" +
            "                     either 5' or 3' end using TriplEx2 sequencing primer or\n" +
            "                     polydT 24 bp primer respectively.\"\n" +
            "ORIGIN\n" +
            "        1 cgggggagga aagaagcaac tgcttgattt ccccacccct tgaacgcatt cggcatattt\n" +
            "       61 tacgttgcgt cgttcccttc tgctgagtca ctttcggatt gaaatttttt ctcctttttc\n" +
            "      121 gaaaatgctc gcagcgcaca ggtaggaggt aaactgggcg ctgttttgac actaactttt\n" +
            "      181 tgcaacggct atacttcttc gctgcgtcag cgaggggaaa atcaagatga acgccccggg\n" +
            "      241 gaaggaaaag cggctatcgc tgctcatatg ccgcgtcaac atgctgataa acttgctgca\n" +
            "      301 aagcagactg gcgttcccac tgatataccc attcaacacc accgcgctga acaatgaaaa\n" +
            "      361 gtcgctggag ttgtacttaa agaaattacg gaaggatgga aactttgacg aagaagcttt\n" +
            "      421 tatgaaaacc ctggcgttta taacaccgtc tatcataacc ctgacgaagt tggttcatgt\n" +
            "      481 tttgaaggca ggccattctt tatgtagcta ctcgggagtg tccaccaaat tgaagtatat\n" +
            "      541 ggacaggaga cggggagcta gtgccctaat gcagcatgag acaaaactgc acttattgag\n" +
            "      601 accgcaggtg tggagactgc taaagcccaa tgatgaaatg aacacgaaac atttgaggtg\n" +
            "      661 gaggaagtga ccacacgggg ggaagcgggg tcgaagtttg caccagaagg gttttggtgg\n" +
            "      721 gcctccacag gggaaaaaca aaatgggttg aagtgaaacg aaaaggggtg caaactgcta\n" +
            "      781 tgcgaggtga atcgattgac ggcgtgtgga gagacacatg ggaaggttct tctatccccc\n" +
            "      841 cca\n" +
            "//\n" +
            "";
}
