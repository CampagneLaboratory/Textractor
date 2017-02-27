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

import edu.cornell.med.fsm4j.core.FSM;
import edu.cornell.med.fsm4j.core.FSMBasic;
import edu.cornell.med.fsm4j.core.FSMSymbols;
import edu.cornell.med.fsm4j.core.State;
import edu.cornell.med.fsm4j.io.FSMLowLevelWriter;
import edu.cornell.med.fsm4j.io.FSMWriter;
import edu.cornell.med.fsm4j.nativelib.FSMLibrarySession;
import edu.cornell.med.fsm4j.nativelib.FSMStatus;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.util.MutableString;
import textractor.mg4j.index.TermIterator;

import java.io.*;
import java.util.ArrayList;


/**
 * Collects prefix and suffixes for a list of stems in a term dictionary.
 * Outputs an FSM archive (far file format for the AT&T FSM library) which can be used to train
 * a language model to parse out prefix/term/suffix limits in terms of the corpus.
 * The following illustrates how the model can be constructed and used.
 * <UL>
 * <LI>
 * Train the language model (as a 2-gram here) [grmcount -n 2 -s 1 -f 2 term-test2.far | grmmake -d none | grmconvert -t failure | fsmclosure > term-test2-n3.lm]</LI>
 * <LI>Translate a term to a finite state automaton in which prefix/term/suffix is equally likely for each character of the term:
 * <p/>
 * <PRE>0       1       fp
 * 0 1 ft
 * 0 1 fs
 * 1       2       lp
 * 1 2 lt
 * 1 2 ls
 * 2       3       op
 * 2 3 ot
 * 2 3 os
 * ...
 * 9       10      cs
 * 9 10 ct
 * 9 10 cp
 * 10      11      es
 * 10 11 et
 * 10 11 ep
 * 11
 * </PRE>
 * <LI> Compile the FSA for the term: [fsmcompile -i symbols.txt florescence-alts.fst > florescence-alts.fsa]
 * <LI> Intersect the term with the language model, find the best path through the FSA and print the path: [fsmintersect florescence-alts.fsa term-test2-n3.lm | fsmbestpath | fsmprint -i symbols.txt]
 * <PRE>
 * 0       1       <epsilon>
 * 1       2       fp      1.47570181
 * 2       3       lp      1.36873293
 * 3       4       op      2.18157053
 * 4       5       rp      2.48307323
 * 5       6       et      3.04800916
 * 6       7       st      3.14820123
 * 7       8       ct      4.70973015
 * 8       9       et      2.91680431
 * 9       10      nt      2.98813868
 * 10      11      cs      4.25810146
 * 11      12      es      2.37579679
 * 12      0.800151765
 * </PRE>
 * <LI>Read the output: when a character is followed by 'p', this character is part of the prefix of the word, 't' - > term, 's'->suffix.
 * <p/>
 * </UL>
 *
 * @author Fabien Campagne
 *         Date: May 8, 2006
 *         Time: 11:03:10 AM
 */
public final class FSMPrefixSuffix {
    private int stateIndex = 1;
    private State previousState;


    public FSMPrefixSuffix() {
        symbols = new FSMSymbols();
        symbols.registerSymbol("<epsilon>");
        symbols.registerSymbol("<s>");
        symbols.registerSymbol("</s>");


    }

    public static void main(final String[] args) throws IOException {

        final FSMPrefixSuffix processor = new FSMPrefixSuffix();
        String word = CLI.getOption(args, "-word", null);
        final String wordList = CLI.getOption(args, "-word-list", null);
        FSMSymbols symbols = null;
        String fsmOutFilename = null;
        FileOutputStream fileOutputStream = null;
        FSMWriter writer = null;
        final String symbolFilename = CLI.getOption(args, "-symbols", null);
        if (word != null || wordList != null) {
            // convert one word for testing:

            if (symbolFilename == null) {
                System.out.println("Please provide -symbols");
                System.exit(1);
            }
            symbols = new FSMSymbols();
            System.out.println("Reading symbols from file " + symbolFilename);
            symbols.read(new FileReader(symbolFilename));
            fsmOutFilename = CLI.getOption(args, "-fsm", word + ".far");
            fileOutputStream = new FileOutputStream(fsmOutFilename);
            writer = new FSMWriter(new FSMLowLevelWriter(fileOutputStream));
        }
        if (word != null) {
            final FSM fsa = convert(word, symbols);
            writer.write(fsa);
            fileOutputStream.close();
            System.out.println("Word " + word + " converted to " + fsmOutFilename);
            System.exit(0);
        }
        if (wordList != null) {
            // for each word in the list parse and write stem '\t' word to the output file.

            final String modelFilename = CLI.getOption(args, "-lm", null);
            if (modelFilename == null) {
                System.out.println("Language model must be provided (-lm).");
                System.exit(1);
            }
            final BufferedReader br = new BufferedReader(new FileReader(wordList));

            while ((word = br.readLine()) != null) {
                final FSM fsa = convert(word, symbols);
                writer.write(fsa);
            }
            fileOutputStream.close();
            final FSMLibrarySession session = new FSMLibrarySession();
            //farfilter 'fsmintersect - all-terms-n2.lm |fsmbestpath |fsmprint -i all-terms-symbols.txt' 3.far
            FSMStatus farfilter = new FSMStatus();
            final String resultFileName = session.allocateCompiledFsmFilename(farfilter, ".tags");
            final OutputStream farfilterOutputStream = new BufferedOutputStream(new FileOutputStream(resultFileName));

            farfilter = session.farfilter("fsmintersect - " + modelFilename + " | fsmbestpath | fsmprint -i " + symbolFilename, fsmOutFilename, farfilterOutputStream);
            if (!farfilter.executionSuccessful()) {
                System.out.println("Farfilter execution failed. Details may be provided below. ");
                farfilter.reportErrors(System.out);
                System.exit(10);
            }
            extractStemsFromOutput(resultFileName);
            System.out.println("Status: " + resultFileName);
        } else {
            processor.process(args);
        }
    }

    private static void extractStemsFromOutput(final String resultFileName) throws IOException {
        final TagOutputParser parser = new TagOutputParser(new BufferedReader(new FileReader(resultFileName)));
        while (parser.hasNext()) {
            System.out.print(parser.getWord());
            System.out.print('\t');
            System.out.print(parser.getPrefix());
            System.out.print('\t');
            System.out.print(parser.getStem());
            System.out.print('\t');
            System.out.print(parser.getFirstLongestStem());
            System.out.print('\t');
            System.out.print(parser.getLastLongestStem());
            System.out.print('\t');
            System.out.print(parser.getSuffix());
            System.out.println();
            System.out.flush();
        }
    }


    private void process(final String[] args) throws IOException {

        final String termFileName = CLI.getOption(args, "-i", null);
        final String stemsFileName = CLI.getOption(args, "-s", null);

        final String fsmOutFilename = CLI.getOption(args, "-fsm", "all-terms.far");
        final File farBasename = new File(fsmOutFilename);

        final int minimalLength = CLI.getIntOption(args, "-ml", 4);

        if (termFileName == null) {
            System.err.println("Dictionary file not found. Provide with option -i");
            System.exit(1);
        }
        if (stemsFileName == null) {
            System.err.println("Stem file not found. Provide with option -s");
            System.exit(1);
        }
        // load all stems:
        final TermIterator stemIt = new TermIterator(stemsFileName);
        while (stemIt.hasNext()) {
            final String term = stemIt.next();
            addStem(term);
        }
        final FileOutputStream fileOutputStream = new FileOutputStream(fsmOutFilename);
        final FSMLowLevelWriter fsmLowLevelWriter = new FSMLowLevelWriter(fileOutputStream);
        final FSMWriter fsmWriter = new FSMWriter(fsmLowLevelWriter);

        // process each term to ID prefix _ suffix:
        final TermIterator termIt = new TermIterator(new FileReader(termFileName), true);
        while (termIt.hasNext()) {
            final String term = termIt.next();
            if (term.length() > minimalLength) {
                extractPrefixSuffix(fsmWriter, term);

            }
        }
        fsmLowLevelWriter.flush();
        fileOutputStream.close();

        final String getSymbolFilename = farBasename.getCanonicalPath().replace(".far", "-symbols.txt");
        writeSymbols(new FileOutputStream(getSymbolFilename));
        normalizeStemLength();
        System.exit(0);
    }

    public void writeSymbols(final FileOutputStream symbolsOutput) throws IOException {
        System.out.print("Writing symbols..");
        System.out.flush();

        symbols.write(symbolsOutput);
        System.out.println(" done");

    }

    private void codeAsFSA(final FSM fsa, final String prefix, final char pstState, final float frequency) {
        codeAsFSA(fsa, new MutableString(prefix), pstState, frequency);
    }


    private final MutableString prefixSuffix = new MutableString();
    private final MutableString prefix = new MutableString();
    private final MutableString suffix = new MutableString();

    private void extractPrefixSuffix(final FSMWriter fsmWriter, final String term) throws IOException {
        for (final String stem : stems) {
            final int stemIndex = term.indexOf(stem);
            if (stemIndex != -1) {
                final FSM fsa = new FSMBasic();
                tallyStemLength(stem);
                extractPrefixSuffix(prefixSuffix, prefix, suffix, term, stemIndex, stem);
                final int stemLength = stem.length();
                final float effectiveCount = getStemLengthNormFactor(stemLength);       // adjust the

                // denominator so that most effective counts are around 1. When they are much larger,  overflows
                // occur (even in the tropical semiring) and counts end up infinite.

                //      System.out.println("effectiveCount: "+effectiveCount+" stem: "+stem);
                codeAsFSA(fsa, prefix, 'p', effectiveCount); // prefix

                codeAsFSA(fsa, stem, 't', effectiveCount); // term

                codeAsFSA(fsa, suffix, 's', effectiveCount);

                if (fsa.getNumberOfStates() > 0) {
                    // Make last state of this FSM final:
                    previousState.setFinalCost(0);
                    fsmWriter.write(fsa);
                }
                previousState = null;
            }
            previousState = null;
        }

    }

    final int[] stemLengthTallies = new int[100];

    private void tallyStemLength(final String stem) {
        stemLengthTallies[stem.length()] += 1;
    }

    public void normalizeStemLength() {
        final float[] stemLengthWeights = new float[stemLengthTallies.length];
        float sum = 0;
        for (final int tally : stemLengthTallies) {
            sum += tally;
        }
        int i = 0;
        for (final int tally : stemLengthTallies) {
            stemLengthWeights[i] = (tally) / sum;
            if (stemLengthWeights[i] != 0) {
		System.out.println(i + "\t" + stemLengthWeights[i]);
	    }
            ++i;
        }
    }

    private void codeAsFSA
            (final FSM fsa, final MutableString
                    prefix, final char pstState, final float frequency) {
        if (prefix.length() == 0) {
	    return;
	}
        if (previousState == null) {
            stateIndex = 0;
            previousState = fsa.createNonTerminalState(0);
            fsa.setInitialStateIndex(stateIndex);
            stateIndex++;
            // System.out.println("initial state");
        }
        for (int i = 0; i < prefix.length(); i++) {
            final char c = prefix.charAt(i);
            final State newState = fsa.createNonTerminalState(stateIndex++);

            final char[] codedState = { c, pstState };
            fsa.createArc(previousState, newState, symbols.registerSymbol(new String(codedState)), -(float) Math.log(frequency));
            // System.out.println("arc on "+new String(codedState));
            previousState = newState;
        }

    }


    private static final MutableString dummy = new MutableString();

    public static void extractPrefixSuffix
            (final MutableString
                    result, final String
                    term, final String
                    stem) {
        final int stemIndex = term.indexOf(stem);

        extractPrefixSuffix(result, dummy, dummy, term, stemIndex, stem);
    }

    private static void extractPrefixSuffix
            (final MutableString prefixSuffix, final MutableString prefix, final MutableString suffix, final String term,
             final int stemIndex, final String stem) {

        prefixSuffix.setLength(0);
        prefix.setLength(0);
        suffix.setLength(0);
        if (stemIndex == -1) {
	    return;
	}
        final CharSequence prefixSeq = term.subSequence(0, stemIndex);
        prefixSuffix.append(prefixSeq);
        prefix.append(prefixSeq);
        prefixSuffix.append("_");
        final CharSequence suffixSeq = term.subSequence(stemIndex + stem.length(), term.length());
        prefixSuffix.append(suffixSeq);
        suffix.append(suffixSeq);


    }

    final ArrayList<String> stems = new ArrayList<String>();

    private void addStem
            (final String
                    term) {
        stems.add(term);
    }

    private FSMSymbols symbols = new FSMSymbols();

    public static FSM convert(final String term, final FSMSymbols symbols) {
        final FSM fsa = new FSMBasic();
        int stateIndex = 0;
        final State initial = fsa.createNonTerminalState(stateIndex++);
        fsa.setInitialStateIndex(0);
        State previousState = initial;
        State newState = fsa.createNonTerminalState(stateIndex++);
        fsa.createArc(previousState, newState, symbols.getCode("<s>"), 0);
        fsa.createArc(previousState, newState, symbols.getCode("<epsilon>"), 0);
        previousState = newState;

        for (int i = 0; i < term.length(); i++) {
            final char character = term.charAt(i);
            newState = fsa.createNonTerminalState(stateIndex++);
            fsa.createArc(previousState, newState, symbols.getCode(character + "p"), 0);
            fsa.createArc(previousState, newState, symbols.getCode(character + "t"), 0);
            fsa.createArc(previousState, newState, symbols.getCode(character + "s"), 0);
            previousState = newState;
        }
        newState = fsa.createState(stateIndex, 0);
        fsa.createArc(previousState, newState, symbols.getCode("<s>"), 0);
        fsa.createArc(previousState, newState, symbols.getCode("<epsilon>"), 0);
        return fsa;
    }

    private static final float[] stemLengthNormFactors = new float[100];

    private float getStemLengthNormFactor(final int stemLength) {
        float stemLengthNormFactor = stemLengthNormFactors[stemLength];
        if (stemLengthNormFactor == 0) {
            if (stemLengthNormFactor < 5) {
		stemLengthNormFactor = stemLengthNormFactors[5];   // assume same as length 5.
	    } else {
		stemLengthNormFactor = stemLengthNormFactors[34]; // minimum non-null value
	    }

        }
        return stemLengthNormFactor;
    }

    static {
        stemLengthNormFactors[5] = 0.41960725f;
        stemLengthNormFactors[6] = 0.20863375f;
        stemLengthNormFactors[7] = 0.12317198f;
        stemLengthNormFactors[8] = 0.083455116f;
        stemLengthNormFactors[9] = 0.056557246f;
        stemLengthNormFactors[10] = 0.038070165f;
        stemLengthNormFactors[11] = 0.025063127f;
        stemLengthNormFactors[12] = 0.016313866f;
        stemLengthNormFactors[13] = 0.010210605f;
        stemLengthNormFactors[14] = 0.006527696f;
        stemLengthNormFactors[15] = 0.003975716f;
        stemLengthNormFactors[16] = 0.0027346478f;
        stemLengthNormFactors[17] = 0.0018830925f;
        stemLengthNormFactors[18] = 0.0012947939f;
        stemLengthNormFactors[19] = 9.616934E-4f;
        stemLengthNormFactors[20] = 5.453178E-4f;
        stemLengthNormFactors[21] = 3.6264976E-4f;
        stemLengthNormFactors[22] = 2.014721E-4f;
        stemLengthNormFactors[23] = 1.4774619E-4f;
        stemLengthNormFactors[24] = 9.93929E-5f;
        stemLengthNormFactors[25] = 3.4921828E-5f;
        stemLengthNormFactors[26] = 3.2235534E-5f;
        stemLengthNormFactors[27] = 5.6412184E-5f;
        stemLengthNormFactors[28] = 1.6117767E-5f;
        stemLengthNormFactors[29] = 1.8804061E-5f;
        stemLengthNormFactors[30] = 8.058883E-6f;
        stemLengthNormFactors[31] = 8.058883E-6f;
        stemLengthNormFactors[32] = 2.6862945E-6f;
        stemLengthNormFactors[33] = 2.6862945E-6f;
        stemLengthNormFactors[34] = 2.6862945E-6f;


        /**
         * 5	0.41960725
         6	0.20863375
         7	0.12317198
         8	0.083455116
         9	0.056557246
         10	0.038070165
         11	0.025063127
         12	0.016313866
         13	0.010210605
         14	0.006527696
         15	0.003975716
         16	0.0027346478
         17	0.0018830925
         18	0.0012947939
         19	9.616934E-4
         20	5.453178E-4
         21	3.6264976E-4
         22	2.014721E-4
         23	1.4774619E-4
         24	9.93929E-5
         25	3.4921828E-5
         26	3.2235534E-5
         27	5.6412184E-5
         28	1.6117767E-5
         29	1.8804061E-5
         30	8.058883E-6
         31	8.058883E-6
         32	2.6862945E-6
         33	2.6862945E-6
         34	2.6862945E-6
         */
    }
}
