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
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.mg4j.util.MutableString;
import textractor.mg4j.index.TermIterator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Obtain prefix and suffixes for a list of stems in a term dictionary.
 *
 * @author Fabien Campagne
 *         Date: Apr 14, 2006
 *         Time: 1:03:10 PM
 */
public final class ObtainPrefixSuffix {
    private long suffixCount;
    private long prefixCount;
    private int minimalStemFrequency = 10;
    private final Map<MutableString, Long> prefixTallies =
	new Object2LongOpenHashMap<MutableString>();
    private final Map<MutableString, Long> suffixTallies =
	new Object2LongOpenHashMap<MutableString>();
    private final Set<MutableString> stems =
	new ObjectOpenHashSet<MutableString>();


    public static void main(final String[] args) throws IOException {
        final ObtainPrefixSuffix processor = new ObtainPrefixSuffix();
        processor.process(args);
    }

    private void process(final String[] args) throws FileNotFoundException {
        final String termFileName = CLI.getOption(args, "-i", null);
        final String stemsFileName = CLI.getOption(args, "-s", null);
        final int minimalLength = CLI.getIntOption(args, "-ml", 4);
        this.minimalStemFrequency = CLI.getIntOption(args, "-mf", 3);

        if (termFileName == null || stemsFileName == null) {
            System.err.println("Dictionary file not found. Provide with option -i");
            System.exit(1);
        }
        // load all stems:
        final TermIterator stemIt = new TermIterator(stemsFileName);
        while (stemIt.hasNext()) {
            final String term = stemIt.next();
            addStem(term);
        }

        // process each term to ID prefix _ suffix:
        final TermIterator termIt =
            new TermIterator(new FileReader(termFileName), true);
        int termCount = 0;
        while (termIt.hasNext()) {
            final MutableString term = termIt.nextMutableStringTerm();
            if (term.length() > minimalLength) {
                extractPrefixSuffix(term, termIt.getFrequency());
                if ((termCount++ + 1) % 1000 == 1) {
		    System.out.println("Processed " + termCount + " terms.");
		}
            }
        }

        writeProbabilities(prefixTallies, prefixCount, "prefix.probs");
        writeProbabilities(suffixTallies, suffixCount, "suffix.probs");

        System.exit(0);
    }

    private void writeProbabilities(final Map<MutableString, Long> prefixTallies, final long prefixCount, final String probFilename) throws FileNotFoundException {
        System.out.println("Writing " + probFilename);
        System.out.flush();
        final PrintWriter printer = new PrintWriter(probFilename);
        // write probabilities:
        for (final Entry<MutableString, Long> entry : prefixTallies.entrySet()) {
            final long frequency = entry.getValue();
            if (frequency > minimalStemFrequency) {
                final double probability = frequency / prefixCount;
                printer.print(entry.getKey());
                printer.print("\t");
                printer.print(probability);
                printer.print("\n");
            }
        }
        printer.close();
    }

    private void extractPrefixSuffix(final MutableString term, final int termFrequency) {
	// the following is equivalent to term.indexOf(stem) for each stem, except that it is about 1,000 times faster..
        final int termLength = term.length();
        for (int length = termLength; length >= 1; --length) {
            final int maxOffset = termLength - length;
            for (int i = 0; i <= maxOffset; i++) {

                final MutableString potentialStem =
                    term.substring(i, i + length);
                if (stems.contains(potentialStem)) {
                    extractPrefixSuffix(term, i, potentialStem, termFrequency);
                }
            }
        }
    }

    public static void extractPrefixSuffix(final MutableString prefixSuffix, final String term, final String stem) {
        final int stemIndex = term.indexOf(stem);
        prefixSuffix.setLength(0);
        if (stemIndex == -1) {
	    return;
	}
        final CharSequence prefix = term.subSequence(0, stemIndex);
        prefixSuffix.append(prefix);

        prefixSuffix.append('_');
        final CharSequence suffix =
            term.subSequence(stemIndex + stem.length(), term.length());
        prefixSuffix.append(suffix);
    }

    private void extractPrefixSuffix(final MutableString term,
	    final int stemIndex, final MutableString stem,
	    final int termFrequency) {
        if (stemIndex == -1) {
	    return;
	}

        final MutableString prefix = term.substring(0, stemIndex);

        addCount(prefix, prefixTallies, termFrequency);
        prefixCount += termFrequency;

        final MutableString suffix =
            term.substring(stemIndex + stem.length(), term.length());

        addCount(suffix, suffixTallies, termFrequency);
        suffixCount += termFrequency;
    }

    private void addCount(final MutableString prefix,
	    final Map<MutableString, Long> tallyMap,
	    final int termFrequency) {
        final long count = tallyMap.get(prefix);
        tallyMap.put(prefix, count + termFrequency);
    }

    private void addStem(final String term) {
        stems.add(new MutableString(term).compact());
    }
}
