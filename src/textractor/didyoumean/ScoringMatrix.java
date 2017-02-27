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

package textractor.didyoumean;

import it.unimi.dsi.fastutil.chars.Char2IntMap;
import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * @author Fabien Campagne
 *         Date: Oct 27, 2006
 *         Time: 5:10:34 PM
 */
public class ScoringMatrix {


    /**
     * Creates a scoring matrix from a reader over a matrix file (BLAST matrix file)
     *
     * @param readFrom
     */
    public ScoringMatrix(final Reader readFrom) throws IOException {
        read(readFrom);
    }

    private char[] residueCodes;
    private final Char2IntMap residueIndices = new Char2IntOpenHashMap();
    private int [][] matrixElements;

    private void read(final Reader readFrom) throws IOException {
        final BufferedReader br = new BufferedReader(readFrom);
        String line;
        int lineCount = 0;
        int residueCodeNumber = 0;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#")) {
                continue;
            }

            final String[] tokens = line.split("[ \t]+");
            if (lineCount == 0) {
                residueCodeNumber = tokens.length-2;
                residueCodes = new char[residueCodeNumber];
                for (int i = 1; i < residueCodeNumber; i++) {
                    final char residueCode = tokens[i].charAt(0);
                    residueCodes[i-1] = residueCode;
                    residueIndices.put(residueCode, i-1);
                }
                matrixElements = new int[residueCodeNumber][residueCodeNumber];
            } else {
                final char residueCode = tokens[0].charAt(0);
                if (residueCode == '*') {
                    break;
                }
                for (int j = 1; j < residueCodeNumber; j++) {
                    matrixElements[lineCount - 1][j-1] = Integer.parseInt(tokens[j]);
                }
            }
            lineCount++;

        }
    }

    public ScoringMatrix() {
    }

    /**
     * Score a residue mutation.
     *
     * @param residueA
     * @param residueB
     * @return Score contribution for transforming residue into residueB
     */
    public int score(final char residueA, final char residueB) {
        return matrixElements[residueIndices.get(residueA)][residueIndices.get(residueB)];
    }
}
