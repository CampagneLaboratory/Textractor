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

import edu.cornell.med.icb.clustering.MaxLinkageDistanceCalculator;
import edu.cornell.med.icb.clustering.QTClusterer;
import edu.cornell.med.icb.clustering.SimilarityDistanceCalculator;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.database.PropertyManager;
import textractor.database.TextractorDatabaseException;
import textractor.tools.ambiguity.AQuery;
import textractor.tools.ambiguity.KeywordPairAnalysed;
import textractor.tools.ambiguity.OrthogonalKeywordSets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: Fabien Campagne
 * Date: Jul 30, 2005
 * Time: 5:02:01 PM
 */
public final class AnalyzeQueries {
    private Collection<AQuery> queries;
    private Object2IntMap<String> seedWordCountInQueries;

    public static void main(final String[] args) throws IOException, TextractorDatabaseException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        final String propertyFilename = CLI.getOption(args, "-property", "textractor.properties");
        final String defaultQueriesFilename = PropertyManager.getInstance(propertyFilename).getProperty("dataset.a.path") +
                File.separator + "queries.txt";
        final String queriesFilename = CLI.getOption(args, "-queries", defaultQueriesFilename);
        final String basename = CLI.getOption(args, "-basename", "ambiguity-index");

        final AnalyzeQueries aqueries = new AnalyzeQueries();
        aqueries.process(queriesFilename, basename);
        System.exit(1);
    }

    public AnalyzeQueries() {
        super();
        queries = new ArrayList<AQuery>();
        seedWordCountInQueries = new Object2IntOpenHashMap<String>();

    }

    private void process(final String queriesFilename, final String basename) throws IOException, TextractorDatabaseException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        // populate queries field from content of file:
        readQueries(queriesFilename);

        // find those words that occur in multiple queries and are potentially ambigous.
        // We call these words seeds.
        final Collection<String> seeds = findPotentiallyAmbiguousWords(queries);
        System.out.println("seeds: " + seeds);


        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);

        // process each seed to find if it is ambigious
        for (final String seed : seeds) { // for each potentially ambiguous term:
            final List<KeywordPairAnalysed> kwIntersection =
                new ArrayList<KeywordPairAnalysed>();
            for (final AQuery query : queries) { // in each query:
                kwIntersection.addAll(findKeywordPairs(docmanager, seed, query));
            }

            // now we need to find a way to extract those kwIntersection of words that separate the
            // documents into mostly not overlapping sets, e.g., find index i,j such that i,j!=seedWordIndex
            // and overlap(matches[i], matches[j])< threshold.

            // count how many times two documents are shared between matches[i] and matches[j]. Normalize
            // to the total number of documents in i and j.
            // a better way to do this would be to work out the statistics to figure out the probability
            // that the amount of overlap observed between the matches is due to chance.
            final List<OrthogonalKeywordSets> allPotentialOrthogonalSets =
                new ArrayList<OrthogonalKeywordSets>();

            for (int i = 0; i < kwIntersection.size(); ++i) {
                for (int j = 0; j < i; ++j) {
                    final KeywordPairAnalysed keywordPairAnalysed1 = kwIntersection.get(i);
                    final KeywordPairAnalysed keywordPairAnalysed2 = kwIntersection.get(j);
                    final int[] intersection = docmanager.intersection(keywordPairAnalysed1.getMatchingDocuments(),
                            keywordPairAnalysed2.getMatchingDocuments());

                    final OrthogonalKeywordSets oset = new OrthogonalKeywordSets(keywordPairAnalysed1, keywordPairAnalysed2,
                            intersection);

                    allPotentialOrthogonalSets.add(oset);
                }

            }
            Collections.sort(allPotentialOrthogonalSets);

            final String[] allWordsArray = buildWordArray(allPotentialOrthogonalSets);
            // Here, we would like to cluster words, as if the score was a semantic distance
            // measure. For instance, we want to find that troponin and t are part of the
            // same semantic cluster, while trinitrotoluene and troponin belong to different
            // clusters.


            final QTClusterer clusterer = new QTClusterer(allWordsArray.length);
            clusterer.setClustersCannotOverlap(false); // allow a term to be part of several semantic clusters.
            final SimilarityDistanceCalculator similarityDistanceCalculator =
                    new MaxLinkageDistanceCalculator() {
                public double distance(final int instanceIndex, final int otherInstanceIndex) {
                    final String word1 = allWordsArray[instanceIndex];
                    final String word2 = allWordsArray[otherInstanceIndex];
                    for (final OrthogonalKeywordSets oset : allPotentialOrthogonalSets) {
                        if (oset.matches(word1, word2)) {
                            return oset.getScore();
                        }
                    }
                    return getIgnoreDistance();
                }
            };
            final float distanceThreshold = 100;
            final List<int[]> clusters =
                clusterer.cluster(similarityDistanceCalculator, distanceThreshold);

            System.out.println("");
            System.out.println(clusters.size() + " clusters found with seed " + seed);
            int i = 0;
            for (final int[] cluster : clusters) {
                System.out.print("Cluster " + i++ + " contains:");
                for (final int wordIndex : cluster) {
                    System.out.print(" ");
                    System.out.print(allWordsArray[wordIndex]);
                }
                System.out.println("");
            }


        }
    }

    private String[] buildWordArray(final List<OrthogonalKeywordSets> allPotentialOrthogonalSets) {
        // assemble the list of words that we want to cluster:
        final Set<String> allWords = new HashSet<String>();
        for (final OrthogonalKeywordSets oset : allPotentialOrthogonalSets) {
            allWords.add(oset.getFirstKeyword());
            allWords.add(oset.getSecondKeyword());
        }

        final String[] allWordsArray = new String[allWords.size()];
        allWords.toArray(allWordsArray);
        return allWordsArray;
    }

    private Collection<KeywordPairAnalysed> findKeywordPairs(
            final DocumentIndexManager docmanager, final String seedWord,
            final AQuery query) throws IOException {
        boolean foundSeedWord = false;
        int seedWordIndex = -1;

        // find the frequency of the seed in the corpus:
        final int seedFrequency = docmanager.query(seedWord).length;

        for (int i = 0; i < query.getKeywords().length; ++i) {
            final String word = query.getKeywords()[i];
            if (word.equalsIgnoreCase(seedWord)) {
                foundSeedWord = true;
                seedWordIndex = i;
            }
        }
        if (!foundSeedWord) {
            // do not process this query if it does not contain the seed
            return new ArrayList<KeywordPairAnalysed>();
        }

        seedWordCountInQueries.put(seedWord, seedWordCountInQueries.getInt(seedWord) + 1);

        // for each pair of seedWord and query word:
        final Collection<KeywordPairAnalysed> pairsAnalysed =
            new ArrayList<KeywordPairAnalysed>();
        for (int i = 0; i < query.getKeywords().length; i++) {
            if (i != seedWordIndex) {
                final KeywordPairAnalysed kwpair =
                        new KeywordPairAnalysed(wrap(query.getKeywords()[seedWordIndex]), wrap(query.getKeywords()[i]));
                kwpair.setSeedCorpusFrequency(seedFrequency);
                // query and store result in index of matches that correspond
                // to this pair (index of the second word in the pair in the query):
                kwpair.setMatchingDocuments(docmanager.queryAnd(kwpair.getKeywordCollection()));
                pairsAnalysed.add(kwpair);
            }
        }
        return pairsAnalysed;
    }

    private String wrap(final String seedWord) {
        return seedWord.toLowerCase();
    }

    private Collection<String> findPotentiallyAmbiguousWords(final Collection<AQuery> queries) {
        // count how many times a word appear in a query.
        final Object2IntOpenHashMap<String> wordCounts =
                new Object2IntOpenHashMap<String>();
        // keep only words that appear in more than one query. (assumption: each query
        // tries to focus the search onto one sense of the word).
        wordCounts.defaultReturnValue(0);
        for (final AQuery query : queries) {
            for (final String word : query.getKeywords()) {
                final String wordKey = wrap(word); // wrap case
                wordCounts.put(wordKey, wordCounts.getInt(wordKey) + 1);
            }
        }

        final Collection<String> result = new ArrayList<String>();
        for (final Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
            if (entry.getValue() > 1) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private void readQueries(final String queriesFilename) throws IOException {
        final BufferedReader br =
                new BufferedReader(new FileReader(queriesFilename));
        String line;
        while ((line = br.readLine()) != null) {
            final String[] query = line.split("[ \t]");
            queries.add(new AQuery(query));
        }
    }
}
