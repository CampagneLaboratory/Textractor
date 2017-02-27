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

package textractor.util;

import edu.mssm.crover.cli.CLI;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Converts the NLProt text writer into a tab delimited format. Discards redundancy in the protein names (keeps only
 * unique protein names predicted in each article). Writes out the information in a tab delimited format where the
 * first column is the article ID and the second column the predicted protein name.
 * <p/>
 * An actual example of the NLProt writer produced from three full text article is given below:
 * <PRE>
 * 74      MgATP
 * 74      T.
 * 74      dAK/dGK
 * 74      HSV-TKs
 * 74      -DRS-
 * 74      Proc
 * 74      H.
 * 74      herpesviral
 * 74      dGK/dAK
 * 74      deoxyguanosine kinase
 * 74      Deoxyadenosine Kinase
 * 74      (^3H)dAdo
 * 74      Deoxycytidine kinase
 * 74      Acad
 * 74      DRS
 * 74      UMCE dGK
 * 74      dGK/dAK(5)
 * 74      al
 * 74      Natl
 * 74      Y.
 * 74      Sequenase
 * 74      Eng
 * 74      Ther
 * 74      Infotrieve
 * 74      deoxycytidine kinase
 * 74      photoaffinity
 * 74      HSV-1
 * 74      dGK
 * 74      R-26
 * 74      () Reprint
 * 74      thymidine kinase
 * 74      Virol
 * 74      JBC
 * 74      1/(dAdo)
 * 74      V.
 * 74      dGK/dAK DRS
 * 74      TK
 * 74      W.
 * 74      NAME
 * 74      R26
 * 74      HSV TKs
 * 74      1/MgATP
 * 74      Vol
 * 74      dGuo
 * 74      dCK
 * 74      Corp
 * 74      || Ives
 * 74      G.
 * 74      pp
 * 74      DRH
 * 74      ASBMB
 * 74      polynucleotide kinase
 * 74      deoxycytidine kinase/deoxyadenosine kinase
 * 74      -DRH-
 * 74      Tel.
 * 74      HSV-1 TK
 * 74      ATP-
 * 74      dCyd
 * 74      UMCE
 * 74      dCK/dAK
 * 74      Sci
 * 74      Jr.
 * 74      PDF
 * 74      V (max)/K
 * 74      deoxyadenosine kinase
 * 74      thymidylate kinase
 * 74      dAdo(7)
 * 74      tandemly
 * 74      dAK
 * 74      (^3H)dGuo
 * 75      MgATP
 * 75      Acetamido-
 * 75      dAK/dGK
 * 75      -DRS-
 * 75      HSV-TKs
 * 75      herpesviral
 * 75      SphI-NotI
 * 75      photocycles
 * 75      dGK/dAK
 * 75      wt
 * 75      deoxyguanosine kinase
 * 75      (^3H)dAdo
 * 75      H+
 * 75      Deoxycytidine kinase
 * 75      small unpolar
 * 75      Fluka
 * 75      UMCE dGK
 * 75      Acad
 * 75      dGK/dAK(5)
 * 75      photocycle
 * 75      al
 * 75      Natl
 * 75      Sequenase
 * 75      Biotech , 5-(iodoacetamido)fluorescein and 5-(bro
 * 75      10C
 * 75      Infotrieve
 * 75      wild type
 * 75      HSV-1
 * 75      R-26
 * 75      JBC
 * 75      tau2 ~5-10
 * 75      dGK/dAK DRS
 * 75      10A
 * 75      (bR)1
 * 75      TK
 * 75      WebLabViewer
 * 75      R26
 * 75      NAME
 * 75      1/MgATP
 * 75      R82A/A160C
 * 75      unphotolyzed protein
 * 75      J.-P.
 * 75      retinylidene
 * 75      M-rise
 * 75      M -1
 * 75      dCK
 * 75      D2O
 * 75      Unphotolyzed
 * 75      10D
 * 75      DRH
 * 75      pp
 * 75      G.
 * 75      -DRH-
 * 75      ATP-
 * 75      tauD2O/tauH2O
 * 75      dCK/dAK
 * 75      BC-
 * 75      Sci
 * 75      V (max)/K
 * 75      Jr.
 * 75      R82A/G231C ()
 * 75      thymidylate kinase
 * 75      dAdo(7)
 * 75      tandemly
 * 75      (^3H)dGuo
 * 75      tauD2O
 * 75      T.
 * 75      M-decay
 * 75      R82A/G231C--
 * 75      Proc
 * 75      top
 * 75      H.
 * 75      R82A/G231C
 * 75      Deoxyadenosine Kinase
 * 75      tauH2O
 * 75      R82Q/D212N
 * 75      || Heyn
 * 75      DRS
 * 75      M in R82A
 * 75      Isr
 * 75      Maarten P
 * 75      Y.
 * 75      Eng
 * 75      Ther
 * 75      G-protein
 * 75      photoaffinity
 * 75      deoxycytidine kinase
 * 75      G-25
 * 75      dGK
 * 75      BMF
 * 75      () Reprint
 * 75      bR
 * 75      K.-H.
 * 75      thymidine kinase
 * 75      1/(dAdo)
 * 75      Virol
 * 75      V.
 * 75      CRC
 * 75      Inst
 * 75      W.
 * 75      revertancy
 * 75      HSV TKs
 * 75      10B
 * 75      Vol
 * 75      dGuo
 * 75      papain
 * 75      Corp
 * 75      || Ives
 * 75      heptahelical
 * 75      mOD
 * 75      ASBMB
 * 75      polynucleotide kinase
 * 75      deoxycytidine kinase/deoxyadenosine kinase
 * 75      Tel.
 * 75      HSV-1 TK
 * 75      UMCE
 * 75      dCyd
 * 75      pH/pD 8
 * 75      PDF
 * 75      deoxyadenosine kinase
 * 75      exponentials
 * 75      dAK
 * 75      TOP
 * 76      MgATP
 * 76      hexokinase II
 * 76      Acetamido-
 * 76      dAK/dGK
 * 76      -DRS-
 * 76      HSV-TKs
 * 76      Hexokinase II
 * 76      herpesviral
 * 76      minigene
 * 76      SphI-NotI
 * 76      Glut1
 * 76      photocycles
 * 76      dGK/dAK
 * 76      QiAmp
 * 76      wt
 * 76      Glut4
 * 76      deoxyguanosine kinase
 * 76      (^3H)dAdo
 * 76      H+
 * 76      Deoxycytidine kinase
 * 76      small unpolar
 * 76      BSA
 * 76      hexokinase I
 * 76      Fluka
 * 76      UMCE dGK
 * 76      Acad
 * 76      dGK/dAK(5)
 * 76      hexokinase
 * 76      photocycle
 * 76      al
 * 76      Lilly
 * 76      Natl
 * 76      Sequenase
 * 76      Biotech , 5-(iodoacetamido)fluorescein and 5-(bro
 * 76      10C
 * 76      Infotrieve
 * 76      wild type
 * 76      HSV-1
 * 76      S.E
 * 76      R-26
 * 76      ECL
 * 76      hexokinase transgene
 * 76      JBC
 * 76      tau2 ~5-10
 * 76      dGK/dAK DRS
 * 76      10A
 * 76      (bR)1
 * 76      TK
 * 76      WebLabViewer
 * 76      R26
 * 76      NAME
 * 76      1/MgATP
 * 76      R82A/A160C
 * 76      unphotolyzed protein
 * 76      J.-P.
 * 76      myosin light chain-2
 * 76      retinylidene
 * 76      M-rise
 * 76      CO2
 * 76      M -1
 * 76      dCK
 * 76      D2O
 * 76      Unphotolyzed
 * 76      NEN
 * 76      10D
 * 76      Glut1 transgene
 * 76      Glut1--
 * 76      per
 * 76      DRH
 * 76      pp
 * 76      G.
 * 76      DeFronzo
 * 76      -DRH-
 * 76      ATP-
 * 76      tauD2O/tauH2O
 * 76      dCK/dAK
 * 76      BC-
 * 76      Sci
 * 76      V (max)/K
 * 76      Jr.
 * 76      R82A/G231C ()
 * 76      thymidylate kinase
 * 76      dAdo(7)
 * 76      tandemly
 * 76      glycogen synthase
 * 76      SJL
 * 76      (^3H)dGuo
 * 76      tauD2O
 * 76      T.
 * 76      M-decay
 * 76      R82A/G231C--
 * 76      Insulin
 * 76      Proc
 * 76      top
 * 76      H.
 * 76      R82A/G231C
 * 76      Drs
 * 76      Purina
 * 76      Deoxyadenosine Kinase
 * 76      tauH2O
 * 76      R82Q/D212N
 * 76      || Heyn
 * 76      DRS
 * 76      M in R82A
 * 76      Isr
 * 76      Maarten P
 * 76      Y.
 * 76      type II hexokinase
 * 76      Eng
 * 76      Ther
 * 76      G-protein
 * 76      photoaffinity
 * 76      deoxycytidine kinase
 * 76      G-25
 * 76      Hexokinase
 * 76      dGK
 * 76      BMF
 * 76      () Reprint
 * 76      bR
 * 76      Hexokinase II--
 * 76      K.-H.
 * 76      thymidine kinase
 * 76      1/(dAdo)
 * 76      Virol
 * 76      V.
 * 76      CRC
 * 76      Inst
 * 76      W.
 * 76      revertancy
 * 76      HSV TKs
 * 76      10B
 * 76      C57BL/6
 * 76      Vol
 * 76      dGuo
 * 76      papain
 * 76      Corp
 * 76      Hex
 * 76      || Ives
 * 76      heptahelical
 * 76      mOD
 * 76      ASBMB
 * 76      polynucleotide kinase
 * 76      albumin
 * 76      deoxycytidine kinase/deoxyadenosine kinase
 * 76      Hexokinase--
 * 76      Tel.
 * 76      Glut1 ) or both transgenes
 * 76      HSV-1 TK
 * 76      UMCE
 * 76      dCyd
 * 76      pH/pD 8
 * 76      PDF
 * 76      deoxyadenosine kinase
 * 76      exponentials
 * 76      dAK
 * 76      TOP
 * 77      MgATP
 * 77      Neuraminidase--
 * 77      galactosialidosis
 * 77      HSV-TKs
 * 77      neuraminidase oligomerized
 * 77      herpesviral
 * 77      SphI-NotI
 * 77      Glut1
 * 77      wt
 * 77      deoxyguanosine kinase
 * 77      Deoxycytidine kinase
 * 77      H+
 * 77      BSA
 * 77      hexokinase I
 * 77      Acad
 * 77      hexokinase
 * 77      neuraminidase
 * 77      al
 * 77      PP34
 * 77      Struct
 * 77      Natl
 * 77      Sialidases
 * 77      Sequenase
 * 77      Biotech , 5-(iodoacetamido)fluorescein and 5-(bro
 * 77      10C
 * 77      Infotrieve
 * 77      HSV-1
 * 77      S.E
 * 77      R-26
 * 77      ECL
 * 77      Agric
 * 77      hexokinase transgene
 * 77      dGK/dAK DRS
 * 77      d'Azzo 275
 * 77      NAME
 * 77      DiDonato
 * 77      1/MgATP
 * 77      J.-P.
 * 77      myosin light chain-2
 * 77      Tex
 * 77      CO2
 * 77      dCK
 * 77      Unphotolyzed
 * 77      D2O
 * 77      NEN
 * 77      Glut1 transgene
 * 77      pp
 * 77      DeFronzo
 * 77      (35S)methionine
 * 77      dCK/dAK
 * 77      tauD2O/tauH2O
 * 77      BC-
 * 77      dAdo(7)
 * 77      beta-galactosidase-
 * 77      (^3H)dGuo
 * 77      SJL
 * 77      Neuraminidase
 * 77      sialoglycoconjugates
 * 77      tauD2O
 * 77      Proc
 * 77      H.
 * 77      Exp
 * 77      Drs
 * 77      46-kDa protein
 * 77      Deoxyadenosine Kinase
 * 77      tunicamycin
 * 77      tauH2O
 * 77      R82Q/D212N
 * 77      || Heyn
 * 77      Maarten P
 * 77      Isr
 * 77      Y.
 * 77      Eng
 * 77      Ther
 * 77      Opin
 * 77      deoxycytidine kinase
 * 77      photoaffinity
 * 77      PP32
 * 77      G-25
 * 77      BMF
 * 77      beta-Galactosidase
 * 77      bR
 * 77      () Reprint
 * 77      Hexokinase II--
 * 77      thymidine kinase
 * 77      MPP54
 * 77      Virol
 * 77      V.
 * 77      CRC
 * 77      W.
 * 77      Inst
 * 77      Corp.
 * 77      HSV TKs
 * 77      10B
 * 77      Huneur
 * 77      || Ives
 * 77      mOD
 * 77      polynucleotide kinase
 * 77      || d'Azzo
 * 77      Hexokinase--
 * 77      HSV-1 TK
 * 77      Glut1 ) or both transgenes
 * 77      dCyd
 * 77      PDF
 * 77      pH/pD 8
 * 77      beta-Galactosidase--
 * 77      TOP
 * 77      Acetamido-
 * 77      hexokinase II
 * 77      dAK/dGK
 * 77      -DRS-
 * 77      Hexokinase II
 * 77      minigene
 * 77      photocycles
 * 77      dGK/dAK
 * 77      QiAmp
 * 77      Glut4
 * 77      (^3H)dAdo
 * 77      small unpolar
 * 77      Fluka
 * 77      UMCE dGK
 * 77      dGK/dAK(5)
 * 77      photocycle
 * 77      Nat
 * 77      Lilly
 * 77      wild type
 * 77      JBC
 * 77      tau2 ~5-10
 * 77      O'Brien
 * 77      10A
 * 77      (bR)1
 * 77      TK
 * 77      WebLabViewer
 * 77      PP20
 * 77      R26
 * 77      der
 * 77      R82A/A160C
 * 77      unphotolyzed protein
 * 77      protective protein/cathepsin A
 * 77      retinylidene
 * 77      P-40
 * 77      M-rise
 * 77      M -1
 * 77      10D
 * 77      Glut1--
 * 77      per
 * 77      G.
 * 77      DRH
 * 77      -DRH-
 * 77      ATP-
 * 77      d'Azzo
 * 77      HPP34
 * 77      accessory protein
 * 77      Sci
 * 77      Jr.
 * 77      V (max)/K
 * 77      R82A/G231C ()
 * 77      thymidylate kinase
 * 77      tandemly
 * 77      glycogen synthase
 * 77      M-decay
 * 77      T.
 * 77      R82A/G231C--
 * 77      Insulin
 * 77      top
 * 77      R82A/G231C
 * 77      Purina
 * 77      Qu
 * 77      beta -galactosidase
 * 77      beta-galactosidase
 * 77      concanavalin
 * 77      DRS
 * 77      M in R82A
 * 77      type II hexokinase
 * 77      G-protein
 * 77      dGK
 * 77      Hexokinase
 * 77      K.-H.
 * 77      1/(dAdo)
 * 77      PPCA
 * 77      intralysosomal
 * 77      revertancy
 * 77      C57BL/6
 * 77      Vol
 * 77      dGuo
 * 77      pBC3
 * 77      HPP54
 * 77      papain
 * 77      Stn
 * 77      pJR2
 * 77      sialidase
 * 77      Corp
 * 77      heptahelical
 * 77      Hex
 * 77      ASBMB
 * 77      albumin
 * 77      deoxycytidine kinase/deoxyadenosine kinase
 * 77      Tel.
 * 77      UMCE
 * 77      deoxyadenosine kinase
 * 77      exponentials
 * 77      dAK
 * 78      MgATP
 * 78      Neuraminidase--
 * 78      K.V.
 * 78      galactosialidosis
 * 78      HSV-TKs
 * 78      C.M
 * 78      neuraminidase oligomerized
 * 78      subtilisin
 * 78      herpesviral
 * 78      SphI-NotI
 * 78      Glut1
 * 78      TNF receptor
 * 78      wt
 * 78      DOA4
 * 78      deoxyguanosine kinase
 * 78      Physiol
 * 78      Yuh1-Ubal
 * 78      Deoxycytidine kinase
 * 78      H+
 * 78      USP14
 * 78      K.R
 * 78      K.D
 * 78      BSA
 * 78      8 kDa protein ubiquitin ( Figure
 * 78      hexokinase I
 * 78      C.F.
 * 78      DTT
 * 78      Acad
 * 78      Ulp1-Smt3
 * 78      hexokinase
 * 78      neuraminidase
 * 78      al
 * 78      PP34
 * 78      Struct
 * 78      Natl
 * 78      S.M.
 * 78      Sialidases
 * 78      Sequenase
 * 78      Biotech , 5-(iodoacetamido)fluorescein and 5-(bro
 * 78      10C
 * 78      Infotrieve
 * 78      polyubiquitin
 * 78      HSV-1
 * 78      S.E
 * 78      R-26
 * 78      ECL
 * 78      Agric
 * 78      hexokinase transgene
 * 78      Y.C.
 * 78      dGK/dAK DRS
 * 78      scUBP15
 * 78      d'Azzo 275
 * 78      J.D
 * 78      NAME
 * 78      F.D.
 * 78      DiDonato
 * 78      1/MgATP
 * 78      J.A.
 * 78      Annu
 * 78      J.-P.
 * 78      ubiquitin-like
 * 78      myosin light chain-2
 * 78      Tex
 * 78      ovalbumin
 * 78      immediate-early protein
 * 78      CO2
 * 78      dCK
 * 78      Unphotolyze
 * <p/>
 * </PRE>
 * <p/>
 * Created by IntelliJ IDEA.
 * User: campagne
 * Date: Aug 2, 2004
 * Time: 5:06:38 PM
 */
public final class ParseNLProtTextOutput extends ParseOutput {
    private static final int BEFORE_ID = 0;
    private static final int AFTER_ID = 1;
    private static final int TABULAR_SECTION = 2;


    public static void main(final String[] args) throws IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {

        final String nlprotOut = CLI.getOption(args, "-i", null);
        final String parsedOutput = CLI.getOption(args, "-o", "parsed-nlprot.out");
        final String basename = CLI.getOption(args, "-basename", null);

        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);

        if (nlprotOut == null || parsedOutput == null) {
            System.err.println("This utility converts the NLProt text writer into a tab delimited file.\n" +
                    "The first column of the file is the identifier of the article/text given to NLPRot;\n" +
                    "The second column is the protein name that was extracted by NLProt (delimited by <n> in the text writer);\n" +
                    "The third column is textractor index format of this protein/gene name;\n" +
                    "The fourth column is the number of occurrences of this protein/gene name in the article.\n");
            System.err.println("usage: -basename <index-basename> -i <nlprot-writer> -o <filename for tab delimited parsed file");
            System.err.println(" <index-basename> names the basename of an index that was built with the text splitting rules that you want applied during this conversion. The index is not used for lookup in any way.");
        }

        try {
            String line;
            final BufferedReader br = new BufferedReader(new FileReader(nlprotOut));
            final BufferedWriter writer = new BufferedWriter(new FileWriter(parsedOutput));
            int state = BEFORE_ID;
            String id = "not-set";
            String term;
            while ((line = br.readLine()) != null) {

                switch (state) {
                    case BEFORE_ID:

                        if (line.startsWith("ID: ")) {
                            final String[] fields = line.split(" ");
                            assert fields.length < 2: "ID: must be followed by identifier";
                            id = fields[1];
                            state = AFTER_ID;
                        }
                        break;
                    case AFTER_ID:

                        //too early
                        //if (line.startsWith("The following protein names could be found by NLProt:")) {

                        if (line.startsWith("NAME                                             ORGANISM            TXT-POS    SCORE   METHOD     DB-ID(S)")) {
                            state = TABULAR_SECTION;
                        }
                        break;
                    case TABULAR_SECTION: {

                        if (line.startsWith("__________________________________________________________________________________________________________________________________________")) {

                            outputTabular(docmanager, id, articleTermsMap, writer);
                            state = BEFORE_ID;
                            articleTermsMap.clear();
                        } else {
                            if (line.length() < 50) {
				continue;
// This is where the interested data is:
// Format is:
// NAME                                             ORGANISM            TXT-POS    SCORE   METHOD     DB-ID(S)
			    }

                            //but "name" sometimes contains funny character,
                            // so substring (0,48) is not always what we want
                            //name = line.substring(0, 49-1);
                            final int last = line.indexOf("  ");
                            term = line.substring(0, last);
                            accumulate(term);
                        }
                    }
                    break;
                }
            }
            writer.flush();

        } catch (final IOException ex1) {

            ex1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}

