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

package textractor.tools.expansion;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.mg4j.index.Index;
import it.unimi.dsi.mg4j.index.IndexIterator;
import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.SAXException;
import textractor.database.DocumentIndexManager;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class removes commonly occuring words from collected expansions.
 */
public final class ClipExpansions {
    private static final Log LOG = LogFactory.getLog(ClipExpansions.class);
    private Map<String, List<ExpansionTerm>> expansionMap;
    private final Map<String, Long> cachedFrequencies;
    private float thresholdPercentage;
    private static final int POSITIONS = 5;
    private final List<ExpansionTerm>[] positionCount;

    public ClipExpansions(final float newPercentage) {
        cachedFrequencies = new Object2LongOpenHashMap<String>();
        setThresholdPercentage(newPercentage);
        positionCount = new ArrayList[POSITIONS];
        for (int n = 0; n < POSITIONS; n++) {
            positionCount[n] = new ArrayList<ExpansionTerm>();
        }
    }

    public void loadExpansions(final String filename) throws IOException, SAXException {
        LOG.debug("Loading expansions: " + filename);
        final SAXParser parser = new SAXParser();
        final ExpansionHandler handler = new ExpansionHandler();
        parser.setContentHandler(handler);
        parser.parse(filename);
        expansionMap = handler.getExpansionMap();
    }

    public Map<String, Set<ExpansionTerm>> clip(final DocumentIndexManager docmanager) throws IOException {
        final HashMap<String, Set<ExpansionTerm>> clippedLongFormMap =
                new HashMap<String, Set<ExpansionTerm>>();

        final WordReader wordReader = docmanager.getWordReader();
        final TermProcessor termProcessor = docmanager.getTermProcessor();
        final int totalTerms = docmanager.getNumberOfTerms();

        // Calculate threshold
        final float threshold =
                calculateThresholdFrequency(docmanager, wordReader, termProcessor, totalTerms);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Clipping with: " + docmanager.getBasename());
            LOG.debug("Threshold set at: " + threshold);
        }

        // Clip words with frequencies below the threshold
        for (final String acronym : expansionMap.keySet()) {
            final List<ExpansionTerm> longForms =
                    expansionMap.get(acronym);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Clipping " + longForms.size() + " long forms for "
                        + acronym);
            }

            final MutableString word = new MutableString();
            final MutableString nonword = new MutableString();
            final HashSet<ExpansionTerm> clippedLongForms =
                    new HashSet<ExpansionTerm>();
            for (final ExpansionTerm thisExpansionTerm : longForms) {
                final StringBuffer clippedLongFormBuffer = new StringBuffer();
                final String longForm = thisExpansionTerm.getTerm();
                final long frequencyCount = thisExpansionTerm.getFrequency();

                final StringReader stringReader = new StringReader(longForm);
                wordReader.setReader(stringReader);
                while (wordReader.next(word, nonword)) {
                    termProcessor.processTerm(word);
                    if (cachedFrequencies.get(word.toString()) < threshold) {
                        if (clippedLongFormBuffer.length() != 0) {
                            clippedLongFormBuffer.append(' ');
                        }
                        clippedLongFormBuffer.append(word.toString());
                    }
                }
                final String clippedLongForm = clippedLongFormBuffer.toString();

                // Check if long form exists in clippedLongForm collection
                boolean found = false;
                for (final ExpansionTerm iteratedTerm : clippedLongForms) {
                    if (iteratedTerm.getTerm().equals(clippedLongForm)) {
                        found = true;
                    }
                }

                final ExpansionTerm expansionTerm = new ExpansionTerm();

                if (!found) {
                    expansionTerm.setFrequency(frequencyCount);
                    expansionTerm.setTerm(clippedLongForm);
                    expansionTerm.setShortForm(acronym);
                    clippedLongForms.add(expansionTerm);
                }
            }

            clippedLongFormMap.put(acronym, clippedLongForms);
        }

        return clippedLongFormMap;
    }

    private float calculateThresholdFrequency(
            final DocumentIndexManager docmanager,
            final WordReader wordReader,
            final TermProcessor termProcessor,
            final int totalTerms) throws IOException {
        long maximumFrequency = 0;
        final Index index = docmanager.getIndex();

        for (final String acronym : expansionMap.keySet()) {
            final List<ExpansionTerm> longForms = expansionMap.get(acronym);
            final MutableString word = new MutableString();
            final MutableString nonword = new MutableString();

            for (final ExpansionTerm longForm1 : longForms) {
                final String longForm = longForm1.getTerm();
                int longFormPosition = 0;

                final StringReader stringReader = new StringReader(longForm);
                wordReader.setReader(stringReader);
                while (wordReader.next(word, nonword)) {
                    termProcessor.processTerm(word);
                    final int term = docmanager.findTermIndex(word);
                    long frequency = 0;

                    if (cachedFrequencies.get(word.toString()) == null) {
                        if (term != DocumentIndexManager.NO_SUCH_TERM) {
                            if ((term + 1) <= totalTerms) {
                                final IndexIterator ii = index.documents(term);
                                while (ii.hasNext()) {
                                    ii.next();
                                    frequency += ii.count();
                                }
                            }
                        } else {
                            frequency = 0;
                        }

                        cachedFrequencies.put(word.toString(), frequency);
                    } else {
                        frequency = cachedFrequencies.get(word.toString());
                    }

                    if (frequency > maximumFrequency) {
                        maximumFrequency = frequency;
                    }

                    if (longFormPosition < POSITIONS) {
                        incrementPositionCounter(word.toString(), longFormPosition);
                    }

                    longFormPosition++;
                }
            }
        }

        return maximumFrequency - (thresholdPercentage * maximumFrequency);
    }

    /**
     * Returns the frequency count of a term at a sentence position.
     * @param word
     * @param position
     * @return Long of the frequency of the word at sentence location 'position'
     */
    public long positionCountFor(final String word, final int position) {
        return getExpansionTermFor(word, positionCount[position]).getFrequency();
    }

    /**
     * Convenience method to retrieve ExpansionTerm objects for words from an an
     * List that contains all objects for a particular sentence location.
     *
     * @param word the term
     * @param positionArray the array from which the object should be retrieved
     * @return ExpansionTerm for the word at a sentence position
     */
    public ExpansionTerm getExpansionTermFor(final String word,
            final List<ExpansionTerm> positionArray) {
        for (final ExpansionTerm term : positionArray) {
            if (term.getTerm().equals(word)) {
                return term;
            }
        }
        return new ExpansionTerm(word, 0L);
    }

    /**
     * Increaments the frequency count for a particular term at a particular
     * sentence location.
     *
     * @param word the term for which the frequency is incremented
     * @param position the sentence position of the term
     */
    public void incrementPositionCounter(final String word,
            final int position) {
        final ExpansionTerm expansionTerm =
                getExpansionTermFor(word, positionCount[position]);
        positionCount[position].remove(expansionTerm);
        expansionTerm.setFrequency(expansionTerm.getFrequency() + 1);
        positionCount[position].add(expansionTerm);
    }

    /**
     * Returns position counts as an array of ArrayList of ExpansionTerm
     * objects. Each ExpansionTerm holds details of the expansion term and it's
     * associated frequency. Each ArrayList holds a collection of Expansion
     * terms for a particular sentence position The ArrayList[] array holds the
     * ArrayLists for each sentence position stored (default 5)
     *
     * @return List[] of ExpansionTerm objects
     */

    public List<ExpansionTerm>[] getPositionCounts() {
        return positionCount;
    }

    public float getThresholdPercentage() {
        return thresholdPercentage;
    }

    public void setThresholdPercentage(final float thresholdPercentage) {
        this.thresholdPercentage = thresholdPercentage;
    }
}
