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

package textractor.tools.lookup;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.Interval;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.database.DocumentTermPositions;
import textractor.database.TermDocumentPositions;
import textractor.datamodel.LookupResult;
import textractor.datamodel.Sentence;
import textractor.datamodel.TermOccurrence;
import textractor.stemming.PaiceHuskStemmer;
import textractor.tools.DocumentQueryResult;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Look up each term in the dictionay in the indexed terms. Not really a lookup
 * in common sense, but a reverse lookup.
 */
public final class Dictionary {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(Dictionary.class);
    private PaiceHuskStemmer stemmer;
    private final Collection<String> terms;
    public static final String DEFAULT_DICTIONARY =
        "/dictionary/svm.w3.dictionary.10_2_2004.filtered.out_dic.out_org";

    public Dictionary(final String filename, final boolean stemming,
            final boolean checkAndRemoveRedundancy) throws IOException {

        if (checkAndRemoveRedundancy) {
            terms = new HashSet<String>();
        } else {
            terms = new ArrayList<String>();
        }

        if (stemming) {
            try {
                stemmer = new PaiceHuskStemmer(true);
            } catch (final IOException e) {
                LOG.error("An error occured initializing stemming support."
                        + " Defaulting to no stemming", e);
            }
        }

        if (filename == null) {
            // get a reader for the dictionary in the classpath instead.
            final InputStream stream =
                getClass().getResourceAsStream(DEFAULT_DICTIONARY);
            loadDictionary(stream);
        } else {
            loadDictionary(filename);
        }
    }

    /**
     * Load terms from the given filename.
     * @param filename Name of the dictionary file
     * @throws IOException if there is a problem with the file
     */
    private void loadDictionary(final String filename) throws IOException {
        assert filename != null;

        loadDictionary(new FileInputStream(filename));
    }

    /**
     * Load terms from the given filename.
     * @param stream Stream that provides the dictionary items
     * @throws IOException if there is a problem with the stream
     */
    private void loadDictionary(final InputStream stream) throws IOException {
        final BufferedReader reader =
            new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            String term = line.trim();
            if (term.length() == 0 || line.charAt(0) == '#') {
                continue;
            }

            if (stemmer != null) {
                final String[] termElements = term.split("\\s");
                final StringBuffer termStemmed =
                    new StringBuffer(stemmer.stripAffixes(termElements[0]));
                for (int i = 1; i < termElements.length; i++) {
                    termStemmed.append(' ');
                    termStemmed.append(stemmer.stripAffixes(termElements[i]));
                }
                term = termStemmed.toString();
            }
            terms.add(term);
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("Load " + terms.size() + " terms.");
        }
    }

    /**
     * Add additional term list to existing dictionary.
     *
     * @param newTerms terms to add
     */
    public void addTerms(final Collection<String> newTerms) {
        if (newTerms != null) {
            terms.addAll(newTerms);
            if (LOG.isInfoEnabled()) {
                LOG.info("Add additional " + newTerms.size() + " terms.");
            }
        }
    }

    public Collection<LookupResult> lookupByTerm(final DocumentIndexManager docmanager) throws IOException {
        final List<LookupResult> results = new ArrayList<LookupResult>();

        for (final String term : terms) {
            final LookupResult result = lookup(term, docmanager);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    /**
     * Using the queryAndExactOrderMg4jNative, it is much slower than my own
     * method.
     *
     * @param originalTerm
     * @param docmanager
     * @return
     */
    public LookupResult lookupNative(final String originalTerm,
                                     final DocumentIndexManager docmanager) throws IOException {
        final MutableString currentTerm = Sentence.getSpaceDelimitedProcessedTerms(docmanager, originalTerm);
        final String[] currentTerms = currentTerm.toString().split("\\s");

        // todo: should be able to find both "A B" and "A-B" situations
        final DocumentIterator documentIterator =
                docmanager.queryAndExactOrderMg4jNative(currentTerms);
        if (documentIterator != null) {
            final DocumentQueryResult resultCache =
                    new DocumentQueryResult(documentIterator);
            final int[] documents = resultCache.getDocuments();

            if (documents.length != 0) {
                final LookupResult result = new LookupResult(originalTerm);
                result.setDocuments(documents);
                final int[] counts = new int[documents.length];
                for (int j = 0; j < documents.length; j++) {
                    final int document = documents[j];
                    final Iterator<Interval> intervalIterator =
                            resultCache.getIntervalIterator(document);
                    while (intervalIterator.hasNext()) {
                        intervalIterator.next();
                        counts[j]++;
                    }
                }
                result.setNumberOfOccurrences(counts);
                return result;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public LookupResult lookup(final String originalTerm,
            final DocumentIndexManager docmanager) throws IOException {
        final MutableString currentTerm =
                Sentence.getSpaceDelimitedProcessedTerms(docmanager,originalTerm);
        final String[] currentTerms = currentTerm.toString().split("\\s");
        // TODO: should be able to find both "A B" and "A-B" situations
        final TermDocumentPositions termDocumentPositions =
                docmanager.queryAndExactOrder(currentTerms);
        if (termDocumentPositions != null) {
            final LookupResult result = new LookupResult(originalTerm);
            result.setValues(termDocumentPositions);
            return result;
        } else {
            return null;
        }
    }

    /**
     * Get a hashMap that contains the documents and the lookupedTerms in each
     * documents.
     *
     * @param docmanager
     * @return
     */
    public Map<Integer, DocumentTermPositions> lookupByDocument(final DocumentIndexManager docmanager) throws IOException {
        final Collection<LookupResult> lookupResults = lookupByTerm(docmanager);
        final Map<Integer, DocumentTermPositions> documentToTerms =
            new Int2ObjectOpenHashMap<DocumentTermPositions>();
        DocumentTermPositions documentTermPositions;

        for (final LookupResult lookupResult : lookupResults) {
            // iterate through all the documents that a certain lookupedTerm occurs in.
            final int[] documents = lookupResult.getDocuments();
            for (final int document : documents) {
                if (documentToTerms.containsKey(document)) {
                    documentTermPositions = documentToTerms.get(document);
                } else {
                    documentTermPositions = new DocumentTermPositions(document);
                }
                documentTermPositions.setPositions(lookupResult.getTerm(), lookupResult.getPositionsByDocument(document));
                documentToTerms.put(document, documentTermPositions);
            }
        }
        removeInclusion(documentToTerms);
        return documentToTerms;
    }

    private void removeInclusion(final Map<Integer, DocumentTermPositions> documentToTerms) {
        final Set<Integer> documents = documentToTerms.keySet();
        for (final int document : documents) {
            final DocumentTermPositions documentTermPositions =
                    documentToTerms.get(document);
            final String[] terms = documentTermPositions.getTerms();
            final TermOccurrence[] termOccurrences = new TermOccurrence[terms.length];
            for (int x = 0; x < termOccurrences.length; x++) {
                final String term = terms[x];
                final int occurrence = documentTermPositions.getPositionsByTerm(term).length;
                termOccurrences[x] = new TermOccurrence(term, null, occurrence);
            }
            Arrays.sort(termOccurrences);
            final boolean[] markToRemove = new boolean[termOccurrences.length];
            for (int i = 0; i < termOccurrences.length - 1; i++) {
                if (markToRemove[i]) {
                    continue;
                }
                for (int j = i + 1; j < termOccurrences.length; j++) {
                    if (markToRemove[j]) {
                        continue;
                    }
                    if (termOccurrences[j].isIncluding(termOccurrences[i])) {//j include i\
                        // todo: consider the occurrence?
                        markToRemove[i] = true;
                        markToRemove[j] = false;
                        break;
                    } else {
                        markToRemove[i] = false;
                        markToRemove[j] = false;
                    }
                }
            }

            for (int i = 0; i < termOccurrences.length; i++) {
                if (markToRemove[i]) {
                    documentTermPositions.deleteTerm(termOccurrences[i].getTerm());
                }
            }

            documentToTerms.put(document, documentTermPositions);
        }
    }
}
