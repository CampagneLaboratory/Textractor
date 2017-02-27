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

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

/**
 * Estimates probabilities for prefix suffix model (max likelihood estimates).
 *
 * @author Fabien Campagne
 *         Date: Apr 14, 2006
 *         Time: 1:03:10 PM
 */
public final class MaxLikelihoodPrefixSuffix {
    private Object2IntMap<String> tallies = new Object2IntOpenHashMap<String>();

    public static void main(final String[] args) throws IOException {
        final MaxLikelihoodPrefixSuffix processor =
                new MaxLikelihoodPrefixSuffix();
        processor.process(args);
    }

    private void process(final String[] args) throws IOException {
        final String prefixSuffixFileName = CLI.getOption(args, "-i", null);

        final String outFileName = CLI.getOption(args, "-o", "probs_pre_suff.txt");
        final int minimalStemFrequency = CLI.getIntOption(args, "-mf", 3);
        if (prefixSuffixFileName == null) {
            System.err.println("Prefix suffix file not found. Provide with option -i");
            System.exit(1);
        }
        System.out.println("Processing with minimal prefix/suffix frequency= "+minimalStemFrequency);
        // load all prefix suffix occurences:
        final BufferedReader reader = new BufferedReader(new FileReader(prefixSuffixFileName));
        String line;
        int count = 0;  // number of prefix suffix  occurences
        while ((line = reader.readLine()) != null) {
            final String[] tokens = line.split("\t");
            if (tokens.length == 2) {
                final String prefixSuffix = tokens[1];
                addPrefixSuffix(prefixSuffix);
                count++;
            }
        }

        final PrintWriter printer = new PrintWriter(outFileName);
        // write probabilities:
        final Set<Map.Entry<String, Integer>> entries = tallies.entrySet();
        for (final Map.Entry<String, Integer> entry : entries) {
            final int frequency = entry.getValue();
            if (frequency > minimalStemFrequency) {
                double P = frequency;
                P /= count;
                printer.print(entry.getKey());
                printer.print("\t");
                printer.print(P);
                printer.print("\n");
            }
        }
        printer.close();
        System.exit(0);
    }


    private void addPrefixSuffix(final String prefixSuffix) {
        final int count = tallies.getInt(prefixSuffix);
        tallies.put(prefixSuffix, count + 1);
    }


}
