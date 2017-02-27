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

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.mg4j.index.Index;
import it.unimi.dsi.mg4j.query.SelectedInterval;
import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParser;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import it.unimi.dsi.mg4j.query.parser.SimpleParser;
import it.unimi.dsi.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.scoredresult.ScoredResult;
import textractor.scoredresult.ScoredResultComparator;
import textractor.stemming.PaiceHuskStemmer;
import textractor.tools.biostems.ObtainPrefixSuffix;
import textractor.tools.biostems.PSStemmer;
import textractor.tools.biostems.ScoredTerm;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DidYouMean class for suggesting new search terms, a la 'Did You Mean' on
 * Google.com.
 */
public class DidYouMean implements DidYouMeanI {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(DidYouMean.class);
    private final DocumentIndexManager docmanager;
    private String didYouMeanBasename;
    private final int maximumResults;
    private final List<DidYouMeanFieldDescriptor> fieldDescriptions;
    private PaiceHuskStemmer stemmer;
    private final Object2ReferenceMap<String, Index> indexMap;
    private final SimilarityScorer scorer;
    private Map<MutableString, Float> prefixSuffixModel;
    private PSStemmer psStemmer;

    /**
     * Initialize the DidYouMean engine.
     *
     * @param documentManager DocumenManager for the main index (the engine will
     *                        find the associated DidYouMean index)
     * @throws IOException If an error occured reading the did you mean index data.
     */
    public DidYouMean(final DocumentIndexManager documentManager) throws IOException, NoSuchMethodException, IllegalAccessException, ConfigurationException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        super();
        docmanager = documentManager;
        maximumResults = 100;
        didYouMeanBasename = docmanager.getBasename() + "-dym";
        fieldDescriptions = setupFieldDescriptions();

        // Prepare for iterations of each search field
        scorer = new SimilarityScorer();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Index: " + didYouMeanBasename);
        }

        // The following initialization can take some time to run, so it is
        // best to do it only in the constructor.

        // Load field indexes and create map from index aliases (strings) to
        // Index objects
        indexMap = new Object2ReferenceOpenHashMap<String, Index>(Hash.DEFAULT_INITIAL_SIZE, .5f);
        for (final DidYouMeanFieldDescriptor field : fieldDescriptions) {
            indexMap.put(field.getName(),
                    Index.getInstance(didYouMeanBasename + "-" + field.getName() + "?inmemory=1"));    // force loading the DidYouMean index in memory.
        }

        stemmer = new PaiceHuskStemmer(true);
        readPrefixSuffixModel();
        psStemmer = new PSStemmer(getResourceAsReader("prefix-medline.probs"), getResourceAsReader("suffix-medline.probs"), this);
    }

    private Reader getResourceAsReader(final String filename) throws FileNotFoundException {
        InputStream resourceAsStream =
                DidYouMean.class.getResourceAsStream(filename);
        if (resourceAsStream == null) {      // second chance if Jar not in path.
            resourceAsStream =
                    new FileInputStream("data/biostemmer/" + filename);
        }

        return new InputStreamReader(resourceAsStream);
    }

    private void readPrefixSuffixModel() throws IOException {
        prefixSuffixModel = new Object2FloatOpenHashMap<MutableString>();

        InputStream resourceAsStream =
                DidYouMean.class.getResourceAsStream("PrefixSuffixModel");
        if (resourceAsStream == null) {      // second chance if Jar not in path.
            resourceAsStream =
                    new FileInputStream("data/biostemmer/PrefixSuffixModel");
        }

        final BufferedReader br =
                new BufferedReader(new InputStreamReader(resourceAsStream));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                final String[] tokens = line.split("\t");
                if (tokens.length == 2) {
                    final float prob = Float.parseFloat(tokens[1]);
                    final String prefixSuffix = tokens[0];
                    prefixSuffixModel.put(new MutableString(prefixSuffix).compact(), prob);
                }
            }
        } finally {
            br.close();
        }
    }

    private List<DidYouMeanFieldDescriptor> setupFieldDescriptions() {
        final List<DidYouMeanFieldDescriptor> returnArray =
                new ArrayList<DidYouMeanFieldDescriptor>();

        returnArray.add(new DidYouMeanFieldDescriptor("word", DidYouMeanDocumentFactory.WORD, 0.9));
        returnArray.add(new DidYouMeanFieldDescriptor("gram2", DidYouMeanDocumentFactory.GRAM2, 0.6));
        returnArray.add(new DidYouMeanFieldDescriptor("gram3", DidYouMeanDocumentFactory.GRAM3, 0.5));
        returnArray.add(new DidYouMeanFieldDescriptor("gram4", DidYouMeanDocumentFactory.GRAM4, 0.3));
        returnArray.add(new DidYouMeanFieldDescriptor("3start", DidYouMeanDocumentFactory.THREE_START, 1.2));
        returnArray.add(new DidYouMeanFieldDescriptor("3end", DidYouMeanDocumentFactory.THREE_END, 0.7));
        returnArray.add(new DidYouMeanFieldDescriptor("4start", DidYouMeanDocumentFactory.FOUR_START, 0.8));
        returnArray.add(new DidYouMeanFieldDescriptor("4end", DidYouMeanDocumentFactory.FOUR_END, 0.6));
        return returnArray;
    }

    /**
     * Returns "Did you mean" suggestions based on a search term.
     *
     * @param term - the search term
     * @return an ArrayList of suggestions, of null if no suggestions are found
     */
    public List<ScoredResult> suggest(final String term, final float cutoff)
            throws IOException, ConfigurationException,
            ParseException, ClassNotFoundException, QueryParserException,
            QueryBuilderVisitorException {
        return suggest(term, false, cutoff);
    }

    /**
     * Returns "Did you mean" suggestions based on a search term.
     *
     * @param term the search term
     * @param orderWithVignaScore If true, terms are ordered first by vigna
     *                            score, and then by similarity to the search term
     * @return a List of suggestions, of null if no suggestions are found
     */
    public List<ScoredResult> suggest(final String term, final boolean orderWithVignaScore, final float cutoff) throws IOException, ParseException, QueryParserException, QueryBuilderVisitorException {
        // Generate n-grams for search term
        final DidYouMeanDocument dym = new DidYouMeanDocument(term);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Suggest for: " + dym.getContent(0));
        }

        // Set up the scoring weights
        final Reference2DoubleMap<Index> weights =
                new Reference2DoubleOpenHashMap<Index>();
        for (final DidYouMeanFieldDescriptor field : fieldDescriptions) {
            weights.put(indexMap.get(field.getName()), field.getWeight());
        }

        // Instantiate  QueryEngine and turn on
        // multiplexing to allow searches across multiple fields
        final QueryParser queryParser = new SimpleParser(indexMap.keySet(), "word");
        final DidYouMeanQueryEngine queryEngine =
                new DidYouMeanQueryEngine(queryParser, weights, indexMap);
        final ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results =
                new ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>>();

        // Create a new query map, to map fields to query
        final Map<String, String> queryMap = new HashMap<String, String>();
        for (final DidYouMeanFieldDescriptor field : fieldDescriptions) {
            queryMap.put(field.getName(), dym.getContent(field.getFactoryID()).toString());
        }

        // Query!
        queryEngine.result(queryMap, 0, maximumResults, results);

        // Check results
        if (LOG.isDebugEnabled()) {
            LOG.debug("Results for " + term + ": " + results.size());
        }
        int count = 0;
        final ScoredResult[] suggestions =
                new ScoredResult[results.size()];
        for (final DocumentScoreInfo scoreInfo : results) {
            final String termAsString = docmanager.termAsString(scoreInfo.document);
            final float similarityScore = scorer.getSimilarity(termAsString, term);
            suggestions[count] = new ScoredResult(termAsString,
                    (similarityScore * scoreInfo.score) / Math.log(term.length()));
            count++;
        }

        Arrays.sort(suggestions, new ScoredResultComparator());

        final List<ScoredResult> returnArray = new ArrayList<ScoredResult>();
        for (int i = 0; i < suggestions.length; i++) {
            if (LOG.isInfoEnabled()) {
                LOG.info(i + ": threshold : " + suggestions[i].getTerm()
                        + " (" + suggestions[i].getScore() + ")");
            }
            if (suggestions[i].getScore() >= cutoff) {
                if (!(suggestions[i].getTerm().equals(term))) {
                    returnArray.add(suggestions[i]);
                }
            }
        }

        return returnArray;
    }

    /**
     * Suggests terms related to this term. Terms suggested by this method have the same stem as the input term.
     * We use a stemming model tuned for Medline. Terms whose prefix suffix probability are below the model's
     * are not returned. Try experimenting with cutoff values 1E-3 to 1E-4.
     *
     * @param term   Term suggestions are sought for.
     * @param cutoff Probability cutoff. Try 1E-3 or 1E-4.
     * @return Terms syntactically similar to query that share the same stem.
     * @throws ConfigurationException
     * @throws IOException
     * @throws ParseException
     * @throws ClassNotFoundException
     * @deprecated This method will be removed in the near future. Use suggestRelated for biomedical corpora.
     */
    @Deprecated
    public List<ScoredResult> suggestRelated2(final String term, final float cutoff) throws IOException, ParseException, QueryParserException, QueryBuilderVisitorException {
        String stem = stemmer.stripAffixes(term);
        if (stem == null || stem.length() == 0) {
            stem = term;
        }
        final MutableString prefixSuffix = new MutableString();
        final List<ScoredResult> results = suggest(stem, false, 0.04f);  // permissive cutoff
        final Set<ScoredResult> removeSet = new HashSet<ScoredResult>();
        for (final ScoredResult suggestion : results) {
            ObtainPrefixSuffix.extractPrefixSuffix(prefixSuffix, suggestion.getTerm(), stem);
            suggestion.setScore(prefixSuffixModel.get(prefixSuffix));
            if (suggestion.getScore() <= cutoff) {
                removeSet.add(suggestion);
            }

        }
        results.removeAll(removeSet);
        Collections.sort(results, new ScoredResultComparator());
        for (final ScoredResult suggestion : results) {
            LOG.info("suggest-related suggestion: " + suggestion.getTerm() + " score: " + suggestion.getScore());
        }
        return results;
    }

    /**
     * Suggests terms related to this term. Terms suggested by this method
     * are morphologically related to the input term. Each suggestion is
     * associated with a score that indicates how similar the suggestion is to
     * the input term. We use a stemming method tuned for Medline. Terms whose
     * prefix suffix probability are below the model's are not returned. On the
     * Medline corpus, cutoff values around 1E-7 seem to work reasonably well,
     * but a higher cutoff can be used if more stringency is required.
     *
     * @param term Term suggestions are sought for.
     * @param cutoff Probability cutoff. Try 1E-7 for the Medline corpus.
     * @return Terms syntactically similar to query that share the same stem.
     * @throws ConfigurationException
     * @throws IOException
     * @throws ParseException
     * @throws ClassNotFoundException
     */
    public List<ScoredResult> suggestRelated(final String term, final float cutoff) throws ConfigurationException, IOException, ParseException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException {
        final List<ScoredResult> results = new ArrayList<ScoredResult>();
        final List<ScoredTerm> suggestions = psStemmer.suggest(new MutableString(term));
        for (final ScoredTerm suggestion : suggestions) {
            if (suggestion.getScore() >= cutoff) {
                results.add(new ScoredResult(suggestion.getTerm().toString(), suggestion.getScore()));
            }
            LOG.info("suggest-related suggestion: " + suggestion.getTerm() + " score: " + suggestion.getScore());
        }
        return results;
    }

    /**
     * Suggest terms that belong to the same stemmed class as the query term. Use the PaiceHusk stemmer to define
     * the stemming class.
     *
     * @param term
     * @param cutoff
     * @return
     * @throws ConfigurationException
     * @throws IOException
     * @throws ParseException
     * @throws ClassNotFoundException
     * @throws QueryParserException
     * @throws QueryBuilderVisitorException
     */
    public List<ScoredResult> suggestPaiceHusk(final String term, final float cutoff) throws IOException, ParseException, QueryParserException, QueryBuilderVisitorException {
       final String stemmClass=stemmer.stripAffixes(term);
        final List<ScoredResult> results = suggest(term, false, 0.04f);  // permissive cutoff
        final Set<ScoredResult> removeSet = new HashSet<ScoredResult>();
        for (final ScoredResult suggestion : results) {
            final String suggestionStemClass=stemmer.stripAffixes(suggestion.getTerm());
            if (!suggestionStemClass.equalsIgnoreCase(stemmClass)) {
                removeSet.add(suggestion);
            } else {
                suggestion.setScore(1);
            }
        }
        results.removeAll(removeSet);
        for (final ScoredResult suggestion : results) {
            LOG.info("paice-husk-stem-class suggestion: " + suggestion.getTerm() + " score: " + suggestion.getScore());
        }
        return results;
    }

    public String getDidYouMeanBasename() {
        return didYouMeanBasename;
    }

    public void setDidYouMeanBasename(final String basename) {
        this.didYouMeanBasename = basename;
    }
}
