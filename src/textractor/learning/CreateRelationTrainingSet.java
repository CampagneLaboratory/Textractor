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

package textractor.learning;

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.query.nodes.Query;
import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.DocumentIteratorBuilderVisitor;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.mg4j.docstore.DocumentStoreReader;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabien campagne
 */
public final class CreateRelationTrainingSet {
    public static void main(final String[] args) throws ConfigurationException,
            IOException, ParseException, IllegalAccessException,
            NoSuchMethodException, QueryParserException,
            TextractorDatabaseException, InvocationTargetException,
            ClassNotFoundException, InstantiationException, URISyntaxException,
            QueryBuilderVisitorException {
        final CreateRelationTrainingSet query = new CreateRelationTrainingSet();
        query.process(args);
    }

    public String getBasename() {
        return basename;
    }

    public void setBasename(final String basename) {
        this.basename = basename;
    }

    private String basename;

    public void process(final String[] args) throws ConfigurationException, IOException, QueryParserException,
            TextractorDatabaseException, QueryBuilderVisitorException {
        final String inputFilename = CLI.getOption(args, "-i", null);
        final String outputFilename = CLI.getOption(args, "-o", null);
        final boolean normalizeOption = CLI.isKeywordGiven(args, "-normalize");
        final boolean clipOption = CLI.isKeywordGiven(args, "-clip");
        final int clip = CLI.getIntOption(args, "-clip", 3);

        if (clipOption && !normalizeOption) {
            System.out.println("-clip must be used with -normalize");
            System.exit(1);
        }

        if (inputFilename == null || outputFilename == null) {
            System.err.println("usage: -basename mg4j-index-basename -i input -o output [-normalize] [-clip int] ");
            System.err.println("The input file must contain a set of lines. Each line consists of names separated by the character '|' ");
            System.err.println("Each name is a synonym for the concept listed on the line. ");
            System.err.println("A training set is produced that encodes the sentences that match the conjunction of concepts in the input.  ");
            System.err.println("A concept is matched by a sentence if any of its names appear in the sentence.\n ");
            System.err.println("For instance, if the input file contains: ");
            System.err.println("Concept A|A_1|A_2");
            System.err.println("Concept B|Ba_1|B+");
            System.err.println("These sentences will be written to the output if they exist in the corpus identified by basename:");
            System.err.println("Concept A is like Concept B");
            System.err.println("Concept A is similar to B+");
            System.err.println("A_2 is similar to Ba_1");
            System.err.println("");
            System.err.println("Further, if the option -normalize is used, the concepts are normalized and written as:");
            System.err.println("CONCEPT_1 is like CONCEPT2");
            System.err.println("CONCEPT_1 is similar to CONCEPT_2");
            System.err.println("Concept B|Ba_1|B+");
            System.err.println("");
            System.err.println("If the option -clip n is used together with -normalize, the concepts are clipped, keeping only n words on the left\n" +
                    "of the left-most concept occurence and n words on the right of the righ-most concept occurrence");
            System.err.println("CONCEPT_1 is like CONCEPT2");
            System.exit(1);
        }

        final DbManager dbm = new DbManager(args);
            dbm.beginTxn();
        final TextractorManager tm = dbm.getTextractorManager();
        final String basename = CLI.getOption(args, "-basename",
                tm.getInfo().getCaseSensitiveIndexBasename());
        final DocumentIndexManager docmanager =
                new DocumentIndexManager(basename);
        final DocumentStoreReader docStore =
                new DocumentStoreReader(docmanager);
            final PrintWriter writer = new PrintWriter(outputFilename);
        final MutableString queryString =
                prepareQuery(docmanager, inputFilename);
        System.out.println("Query: " + queryString);

        final Query query =
            docmanager.getQueryParser().parse(queryString.toString());
        final DocumentIteratorBuilderVisitor documentIteratorBuilderVisitor =
                new DocumentIteratorBuilderVisitor(null,
                        docmanager.getIndex(), Integer.MAX_VALUE);

        final IntList terms = new IntArrayList();
        final MutableString sentenceText = new MutableString();
        final DocumentIterator docIterator =
                query.accept(documentIteratorBuilderVisitor);
        while (docIterator.hasNext()) {
            final int document = docIterator.nextDocument();
            sentenceText.setLength(0);
            terms.clear();
            docStore.document(document, sentenceText);
            docStore.document(document, terms);
            System.out.println("doc: " + document
        	    + " contains the keyword(s): " + sentenceText);
            if (normalizeOption) {
        	normalize(terms);
                if (clipOption) {
                    clip(terms, clip);
                }
            }

            appendOutput(writer, docmanager, terms);
        }
        writer.close();
        dbm.commitTxn();
    }

    private void clip(final IntList intTerms, final int clip) {
        // find min and max concept positions:
        final int[] terms = intTerms.toIntArray();
        int minConceptIndex = terms.length;
        int maxConceptIndex = 0;
        for (int i = 0; i < terms.length; i++) {
            if (terms[i] < -1) {
                minConceptIndex = Math.min(minConceptIndex, i);
                maxConceptIndex = Math.max(maxConceptIndex, i);
            }
        }
        final int max = Math.min(terms.length, maxConceptIndex + clip);
        final int min = Math.max(0, minConceptIndex - clip);

        // remove terms after maxConceptIndex.
        intTerms.removeElements(max, terms.length);

        // remove terms before maxConceptIndex.
        intTerms.removeElements(0, min);
    }

    private void normalize(final IntList terms) {
        // find concepts in terms and replace by normalized form:
        int positionInSentence = 0;

        boolean stop = false;
        final int[] intTerms = terms.toIntArray();
        while (positionInSentence < intTerms.length && !stop) {
            int conceptIndex = 0;
            for (final int[][] concept : concepts) { // for each concept
                for (final int[] synonym : concept) {
                    if (synonymMatches(synonym, intTerms, positionInSentence)) {
                        normalizeSynonym(synonym, conceptIndex, terms, positionInSentence);
                        normalize(terms);    // process multiple concept occurence recursively
                        stop = true;
                        break;
                    }

                    if (stop) {
                        break;
                    }
                }
                conceptIndex++;
            }
            positionInSentence++;
        }

    }

    private void normalizeSynonym(final int[] synonym, final int conceptIndex, final IntList terms, final int positionInSentence) {
        terms.removeElements(positionInSentence, positionInSentence + synonym.length);
        terms.add(positionInSentence, termConceptIndex(conceptIndex));
    }

    private int termConceptIndex(final int conceptIndex) {
        return -2 - conceptIndex;
        // concept index 0 has term index -2, and decreasing.
    }

    private int termIndexToConceptIndex(final int termIndex) {
        return -termIndex - 2;
        // concept index 0 has term index -2, and decreasing.
    }

    private boolean synonymMatches(final int[] synonym, final int[] intTerms, final int positionInSentence) {
        for (int i = 0; i < synonym.length; i++) {
            if (positionInSentence + i >= intTerms.length) {
                return false;
            }

            if (synonym[i] != intTerms[positionInSentence + i]) {
                return false;
            }
        }
        return true;
    }

    private void appendOutput(final PrintWriter writer,
                              final DocumentIndexManager docManager,
                              final IntList terms) {
        System.out.println("Writing terms:");

        for (final int term : terms) {
            final CharSequence word;
            if (term <= -2) {
                word = "CONCEPT_" + Integer.toString(termIndexToConceptIndex(term) + 1);
            } else {
		word = docManager.termAsCharSequence(term);
	    }
            writer.print(word);
            writer.print(' ');
        }
        writer.println(); // sentence separator is new line.

    }

    private int[][][] concepts;  // [concept-index][synonym-index][term-index]

    private MutableString prepareQuery(final DocumentIndexManager docManager, final String inputFilename) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(inputFilename));
        String line;
        final MutableString query = new MutableString();
        final List<List<List<String>>> rawConcepts = new ArrayList<List<List<String>>>();
        final WordReader wordReader = docManager.getWordReader();
        while ((line = reader.readLine()) != null) {
            final String[] conceptSynonyms = line.split("[|]");
            query.append(" ( ");
            final List<List<String>> concept = new ArrayList<List<String>>();
            for (int i = 0; i < conceptSynonyms.length; i++) {
                final String conceptSynonym = conceptSynonyms[i];
                boolean skip = false;
                wordReader.setReader(new CharArrayReader(conceptSynonym.toCharArray()));
                final MutableString word = new MutableString();
                final MutableString nonWord = new MutableString();
                final List<String> synonym = new ArrayList<String>();
                while (wordReader.next(word, nonWord)) {
                    docManager.getTermProcessor().processTerm(word);
                    if (docManager.findTermIndex(word) == DocumentIndexManager.NO_SUCH_TERM) {
                        // if any word is not in the index, skip the synonym.
                        skip = true;
                    }

                    synonym.add(word.toString());
                }

                if (!skip) {
                    concept.add(synonym);
                    query.append('"');
                    query.append(conceptSynonym);
                    query.append('"');
                    if (i != conceptSynonyms.length - 1) {
                	query.append(" | ");
		    }
                }
            }
            rawConcepts.add(concept);
            query.append(" ) ");
        }

        concepts = new int[rawConcepts.size()][][];
	// convert raw concepts to term indices:
	int i = 0; // concept index
	for (final List<List<String>> rawConcept : rawConcepts) {
	    concepts[i] = new int[rawConcept.size()][];
	    int j = 0; // synonym index

	    for (final List<String> synonym : rawConcept) {
		int k = 0; // term index
		concepts[i][j] = new int[synonym.size()];

		for (final String rawTerm : synonym) {
		    concepts[i][j][k] = docManager.findTermIndex(rawTerm);
		    k++;
		}
		j++;
	    }
	    i++;
	}

        return query;
    }
}
