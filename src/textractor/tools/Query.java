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

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.DocumentIteratorBuilderVisitor;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.StopWatch;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.mg4j.docstore.DocumentStoreReader;
import textractor.mg4j.docstore.TermDocumentFrequencyReader;
import textractor.tfidf.TfIdfCalculator;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * Queries the document index and retrieves sentences that have a certain set
 * of keywords.
 * User: campagne
 * Date: Jan 9, 2004
 * Time: 5:28:30 PM
 */
public final class Query {
    private String basename;

    public static void main(final String[] args) throws ConfigurationException, IOException, ParseException, QueryParserException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException, TextractorDatabaseException, QueryBuilderVisitorException {
        final Query query = new Query();
        query.process(args);
    }

    public String getBasename() {
        return basename;
    }

    public void setBasename(final String basename) {
        this.basename = basename;
    }

    public void process(final String[] args) throws ConfigurationException,
            IOException, QueryParserException,
            NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            ClassNotFoundException, URISyntaxException,
            QueryBuilderVisitorException {
        final String[] keywords = CLI.getOptions(args, "-k");
        final String phrase = CLI.getOption(args, "-p", null);
        final String queryString = CLI.getOption(args, "-q", null);
        final boolean textOnly = CLI.isKeywordGiven(args, "-t");
        final String outputFilename = CLI.getOption(args, "-o", null);

        if (keywords == null && phrase == null) {
            System.err.println("-k <keyword> must be provided (possibly multiple times).");
            System.err.println("usage: -basename mg4j-index-basename ([-k keyword]+ | [-p phrase]) | [-q MG4J-query]) [-articles]  ");
            System.err.println("The -articles option reports PMIDs of the articles that match the query, one per line.");
            System.err.println("-p : phrase is a sequence of terms enclosed in quotes. Documents that match the phrase in order are returned.");
            System.err.println("-q : query expressed in the MG4J query syntax.");
            System.err.println("-o : output file.");
            System.err.println("-debug : More data about tf-idf calculations");
            System.err.println("-tfidf : Computes tf-idf for document collection.");
            System.exit(1);
        }
        final boolean onlyPMID = CLI.isKeywordGiven(args, "-articles", false);
        final String basename = CLI.getOption(args, "-basename", null);
        final boolean computeTfIdf = CLI.isKeywordGiven(args, "-tfidf");
        final boolean debug = CLI.isKeywordGiven(args, "-debug");

        final PrintWriter printer;
        if (outputFilename == null) {
            printer = new PrintWriter(System.out);
        } else {
            printer = new PrintWriter(outputFilename);
        }

        final DocumentIndexManager docmanager =
                new DocumentIndexManager(basename);
        final DocumentStoreReader docStoreReader =
                new DocumentStoreReader(docmanager);
        docStoreReader.readPMIDs();

        final int[] kTerms = new int[keywords.length];
        for (int i = 0; i < keywords.length; ++i) {
            kTerms[i] = docmanager.findTermIndex(keywords[i]);
            if (kTerms[i] == DocumentIndexManager.NO_SUCH_TERM) {
		noSuchTerm(keywords[i]);
	    }
        }

        DocumentIterator docIterator = null;
        if (kTerms.length > 0) {
            docIterator = docmanager.queryAndMg4jNative(kTerms);
        } else if (phrase != null) {
            docIterator = docmanager.queryAndExactOrderMg4jNative(phrase.split(" "));
        } else if (queryString != null) {
            final it.unimi.dsi.mg4j.query.nodes.Query query =
                    docmanager.getQueryParser().parse(queryString);
            final DocumentIteratorBuilderVisitor documentIteratorBuilderVisitor =
                    new DocumentIteratorBuilderVisitor(null,
                            docmanager.getIndex(), Integer.MAX_VALUE);

            docIterator = query.accept(documentIteratorBuilderVisitor);
        }

        final Set<Long> articles = new HashSet<Long>();
        final List<Integer> documents = new ArrayList<Integer>();
        while (docIterator.hasNext()) {
            final int document = docIterator.nextDocument();
            final long pmid = docStoreReader.getPMID(document);
            documents.add(document);
            if (onlyPMID) {
                articles.add(pmid);
            } else {
                final MutableString sentence = docStoreReader.document(document);

                if (sentence != null) {
                    if (!textOnly) {
                        printer.print(document);
                        printer.print('\t');
                        printer.print(pmid);
                        printer.print('\t');
                    }
                    printer.println(sentence);
                } else {
                    System.err.println("Document #" + document + " returned, but does not connect to sentence.");
                }
            }
        }
        if (onlyPMID) {
            for (final long articlePMID : articles) {
                printer.println(articlePMID);
            }

        }

        System.out.println("");
        System.out.println("Query matched " + documents.size() + " documents");
        System.out.flush();

        if (computeTfIdf) {
            System.out.println("Evaluating tf-idf..");
            System.out.flush();
            final StopWatch timer = new StopWatch();
            timer.start();
            final Random random = new Random(8343);
            final int maxDocAnalysis = Math.min(1000, documents.size());
            System.out.println("Sampling " + maxDocAnalysis + " documents.");
            final int[] docs = new int[maxDocAnalysis];
            for (int i = 0; i < maxDocAnalysis; i++) {
                docs[i] = documents.get(random.nextInt(documents.size()));
            }
            final TermDocumentFrequencyReader termDocumentFrequencyReader =
                    new TermDocumentFrequencyReader(docmanager);
            final PrintWriter pw = new PrintWriter("tfidf.csv");
            final TfIdfCalculator calculator =
                    new TfIdfCalculator(termDocumentFrequencyReader, docmanager, debug);
            final float[] tfIdfs = calculator.evaluates(docs);
            timer.stop();
            System.out.println("tf-idf evaluated in " + timer.toString());
            System.out.println("data written to tfidf.csv");
            int termIndex = 0;
            for (final float tfIdf : tfIdfs) {
                if (tfIdf != 0) {
                    pw.println(termIndex + "," + calculator.getTerm(termIndex) + "," + tfIdf + (debug ? "," + calculator.getSums()[termIndex]
                            + "," + calculator.getDocFrequencies()[termIndex] + "," + calculator.getTermFrequencies()[termIndex] :
                            ""));
                }
                ++termIndex;
            }

            pw.close();
        }
    }

    private void noSuchTerm(final String keyword) {
        System.out.println("Cannot query for keyword: " + keyword
                + " The word was not found in the index.");
        System.exit(1);
    }
}
