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

package textractor.caseInsensitive;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The Textractor index for Twease is case insensitive for large
 * words but for small words it is case sensitive. This class
 * stores information, collected at indexing time, on how to
 * generate a list of alternatives for case insensitivity.
 *
 * @author Kevin Dorff
 */
public final class CaseInsensitiveStore implements CaseInsensitiveStoreReader {
    /**
     * Minimum size for a "short term".
     */
    private static final int SHORT_TERM_MIN = 2;

    /**
     * Maximum size for a "short term".
     */
    private static final int SHORT_TERM_MAX = 5;

    /**
     * Decorator for newWords when dumping the store.
     */
    private static final String DECORATOR = "(*)";

    /**
     * The logger.
     */
    private static final Log LOG =
            LogFactory.getLog(CaseInsensitiveStore.class);

    /**
     * The term processor to handle the words with.
     */
    private final TermProcessor termProcessor;

    /**
     * The docmanager to use to build the CaseInsensitiveStore.
     */
    private final DocumentIndexManager docmanager;

    /**
     * A map if index numbers from the lowercase version of the word
     * (the key) to the list of mixed case versions of the word
     * (the List). It is possible the key will be >=
     * docmanager.getNumberOfTerms(), if so that word is stored in
     * newWords.
     */
    private Int2ObjectMap wordsMap = new Int2ObjectOpenHashMap();

    /**
     * This Map stores the lowercase version of a word that doesn't
     * actually exist in the actual term list. If Abcd is in the
     * actual term list but "abcd" isn't, it will be stored in this
     * Map. The Long values will be values greater than
     * docmanager.getNumberOfTerms().
     */
    private Map<String, Integer> newWords = new HashMap<String, Integer>();

    /**
     * The reverse mape of newWords. Build only if needed (ie a call
     * is made to lookupTermFromIndex()).
     */
    private Map<Integer, String> newWordsReverse;

    /**
     * The current top value for the Long in newWords.
     */
    private int newWordsCurValue;

    /**
     * Determine if the store can be added to.
     */
    private boolean canAdd;

    /**
     * Create a matcher for checking if a string contains letters
     * (upper or lower case). Initialize the matcher with a dummy value -
     * we will reuse the object repeatedly.
     */
    private final Matcher containsLettersMatcher =
            Pattern.compile(
                    ".*[A-Z]+.*", Pattern.CASE_INSENSITIVE + Pattern.CANON_EQ)
                    .matcher("x");

    /**
     * Create a matcher for checking if a string contains upper case letters.
     * Initialize the matcher with a dummy value - we will reuse the object
     * repeatedly.
     */
    private final Matcher containsUpperLettersMatcher =
            Pattern.compile(".*[A-Z]+.*", Pattern.CANON_EQ).matcher("x");

    /**
     * This version of the constructor is used when creating a
     * CaseInsensitiveStore and should only be used during the
     * indexing phase, not during application runtime.
     *
     * @param docmanagerVal The docmanager to use
     */
    public CaseInsensitiveStore(final DocumentIndexManager docmanagerVal) {
        this.docmanager = docmanagerVal;
        this.newWordsCurValue = this.docmanager.getNumberOfTerms();

        this.termProcessor = docmanagerVal.getTermProcessor();
        this.canAdd = true;
    }

    /**
     * This version of the constructor is used during application runtime.
     * It will load data for the CaseInsensitiveStore created during
     * the indexing phase.
     *
     * @param docmanagerVal The docmanager to use
     * @param basename      The basename to use when loading the persisted
     *                      data for this object
     */
    @SuppressWarnings("unchecked")
    public CaseInsensitiveStore(
            final DocumentIndexManager docmanagerVal, final String basename) {
        this.docmanager = docmanagerVal;
        this.termProcessor = docmanagerVal.getTermProcessor();
        this.newWordsCurValue = this.docmanager.getNumberOfTerms();
        this.canAdd = false;

        try {
            LOG.info("Loading wordsMap...");
            wordsMap = (Int2ObjectOpenHashMap) BinIO.loadObject(
                    basename + "-cis-wordsMap.dat");
        } catch (final IOException e) {
            final String error =
                    "Error reading " + basename + "-cis-wordsMap.dat";
            LOG.error(error, e);
        } catch (final ClassNotFoundException e) {
            final String error =
                    "Error reading " + basename + "-cis-wordsMap.dat";
            LOG.error(error, e);
        }

        try {
            LOG.info("Loading newWords...");
            newWords = (Map<String, Integer>) BinIO.loadObject(
                    basename + "-cis-newWords.dat");
        } catch (final IOException e) {
            final String error = "Error reading " + basename + "-cis-newWords.dat";
            LOG.error(error, e);
        } catch (final ClassNotFoundException e) {
            final String error = "Error reading " + basename + "-cis-newWords.dat";
            LOG.error(error, e);
        }
    }

    /**
     * Save the CaseInsensitiveStore to a file.
     *
     * @param basename The basename to use when persisting
     *                 the data in this object
     */
    public void saveData(final String basename) {
        if (!canAdd) {
            LOG.error("Cannot save data because canAdd=" + canAdd);

            // Only save it when we are in add mode
            return;
        }

        tightenData();

        try {
            LOG.info("Saving wordsMap...");
            BinIO.storeObject(
                    wordsMap, basename + "-cis-wordsMap.dat");
        } catch (final IOException e) {
            final String error =
                    "Error writing " + basename + "-cis-wordsMap.dat";
            LOG.error(error, e);
        }

        try {
            LOG.info("Saving newWords...");
            BinIO.storeObject(newWords, basename + "-cis-newWords.dat");
        } catch (final IOException e) {
            final String error =
                    "Error writing " + basename + "-cis-newWords.dat";
            LOG.error(error, e);
        }

        canAdd = false;
    }

    /**
     * During the indexing phase, wordsMap should be a map
     * of (int, ArrayList[Integer]). It is more efficient
     * to store (int, int[]). This will convert the entries
     * to the tighter format for storage.
     */
    @SuppressWarnings("unchecked")
    private void tightenData() {
        LOG.info("Tightening data");
        // Process the wordsMap, make it smaller
        final Set<Integer> keys = wordsMap.keySet();
        for (final int key : keys) {
            getWordsAsArray(key);
        }
    }

    /**
     * Return the words at map key value key as an array.
     * This can be called to just make sure what you are
     * getting back is what you expect or can be used to
     * tighten the values in the map.
     *
     * @param key the hashmap key to return
     * @return int[] of words
     */
    @SuppressWarnings("unchecked")
    private int[] getWordsAsArray(final int key) {
        final Object oValues = wordsMap.get(key);
        if (oValues == null) {
            return null;
        } else if (oValues instanceof ArrayList) {
            final ArrayList<Integer> values = (ArrayList<Integer>) oValues;

            // Make an int array with the values
            final int[] tightValues = new int[values.size()];

            for (int pos = 0; pos < values.size(); pos++) {
                tightValues[pos] = values.get(pos);
            }
            // Rpleace loose values with tight values
            wordsMap.put(key, tightValues);
            return tightValues;
        } else {
            return (int[]) oValues;
        }
    }

    /**
     * Return the words at map key value key as an ArrayList[Integer].
     * This can be called to just make sure what you are
     * getting back is what you expect or can be used to
     * loosen the values in the map.
     *
     * @param key the hashmap key to return
     * @return ArrayList[Integer] of words
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Integer> getWordsAsList(final int key) {
        final Object oValues = wordsMap.get(key);
        if (oValues == null) {
            return null;
        } else if (oValues instanceof ArrayList) {
            return (ArrayList) oValues;
        } else {
            // Loosen up this instance
            final int[] values = (int[]) oValues;

            // Make an int array with the values
            final ArrayList<Integer> looseValues = new ArrayList<Integer>();

            for (final int value : values) {
                looseValues.add(value);
            }
            // Rpleace tight values with loose values
            wordsMap.put(key, looseValues);
            return looseValues;
        }
    }

    /**
     * Try to add a term to the CaseInsensitiveStore.
     *
     * @param term the term to try to add
     * @return boolean true if term added to the CaseInsensitiveStore
     *         or if it is not necessary to add it to the CaseInsensitiveStore
     */
    public boolean addTerm(final String term) {
        if (!canAdd) {
            return false;
        }

        if (!shouldEvaluate(term)) {
            return false;
        }

        if (containsUpperCaseLetters(term)) {
            final Integer wordPos = getLowerCaseIndex(term, true);
            ArrayList<Integer> words = getWordsAsList(wordPos);

            if (words == null) {
                // First word for this list...
                final int index = docmanager.findTermIndex(term);
                if (index != -1) {
                    words = new ArrayList<Integer>();
                    words.add(index);
                    wordsMap.put(wordPos, words);
                    LOG.debug("Adding term to new list " + term);
                } else {
                    LOG.debug(
                            "Skipping term that couldn't be "
                                    + "re-found in index " + term
                                    + " this happens with terms with"
                                    + " accented characters.");
                }
            } else {
                final int index = docmanager.findTermIndex(term);
                if (index != -1) {
                    words.add(index);
                    LOG.debug("Adding term to existing list " + term);
                } else {
                    LOG.debug(
                            "Skipping term that couldn't be "
                                    + "re-found in index " + term
                                    + " this happens with terms with"
                                    + " accented characters.");
                }
            }
        }

        return true;
    }

    /**
     * Returns true if this is a short term that we should
     * make case insensitive.
     *
     * @param term the term to check
     * @return boolean true if this is a short term we should
     *         make case insensetive.
     */
    private boolean shouldEvaluate(final String term) {
        return containsLetters(term) && withinShortTermLength(term);

    }

    /**
     * Returns true if the term contains letters (not just numbers
     * or symbols).
     *
     * @param term the term to check
     * @return true if the term contains contains letters
     */
    private boolean containsLetters(final String term) {
        if ((term == null) || (term.length() == 0)) {
            return false;
        }

        containsLettersMatcher.reset(term);

        return containsLettersMatcher.find();
    }

    /**
     * Returns true if the term contains only lower case letters.
     *
     * @param term the term to check
     * @return true if the term contains contains letters
     */
    private boolean containsUpperCaseLetters(final String term) {
        if ((term == null) || (term.length() == 0)) {
            return false;
        }

        containsUpperLettersMatcher.reset(term);

        return containsUpperLettersMatcher.find();
    }

    /**
     * Returns true if the term is within the length
     * of "short term" that textractor will treat
     * as case sensitive.
     *
     * @param term the term to check
     * @return true if the term is within the "short term length"
     */
    private boolean withinShortTermLength(final String term) {
        if (term == null) {
            return false;
        }

        final int length = term.length();
        return length >= SHORT_TERM_MIN && length <= SHORT_TERM_MAX;
    }

    /**
     * Obtain the index value for the term. This will first look in
     * newWords then in the document namager.
     *
     * @param term   The term to get the lower case index for
     * @param insert true if the term can be inserted to newWords
     *               (should only be true when building the CaseInsensitiveStore not
     *               during runtime).
     * @return Integer the index value
     */
    private Integer getLowerCaseIndex(final String term, final boolean insert) {
        final String lcTerm = term.toLowerCase();
        Integer wordPos = newWords.get(lcTerm);

        if (wordPos == null) {
            // Look in the real words list
            wordPos = docmanager.findTermIndex(lcTerm);

            if (wordPos == -1) {
                wordPos = null;
            }
        }

        if ((wordPos == null) && canAdd && insert) {
            // Add the word to newWords
            LOG.debug("Adding term to newWords" + lcTerm);
            wordPos = this.newWordsCurValue;
            newWords.put(lcTerm, wordPos);
            this.newWordsCurValue++;
        }

        return wordPos;
    }

    /**
     * This should only be used after the entire CaseInsensitiveStore
     * is build. It can take an index and return a term. If the index
     * index < docmanager.getNumberOfTerms() it will use the
     * docmanager index, otherwise it will look in newWordsReverse.
     * If decorateNewWords is present and not null it will be
     * appended to terms that come from newWords.
     *
     * @param index            the index to look up
     * @param decorateNewWords string to append to terms that come from
     *                         newWords to decorate them.
     * @return String the term
     */
    private String lookupTermFromIndex(
            final int index, final String decorateNewWords) {
        if (newWordsReverse == null) {
            buildNewWordsReverse();
        }

        if (index >= docmanager.getNumberOfTerms()) {
            final StringBuffer output =
                    new StringBuffer(newWordsReverse.get(index));

            if (decorateNewWords != null) {
                output.append(decorateNewWords);
            }

            return output.toString();
        } else {
            return docmanager.termAsString(index);
        }
    }

    /**
     * Build a HashMap of the newWords with the key and value
     * reversed. This exists so lookupTermFromIndex()
     * can be used. It is not recommended this be run
     * against the entire dataset but primarily exists
     * for testing purposes. This can be useful for
     * displaying the entire CaseInsensitiveStore in
     * a textual format, such as using dumpStore().
     */
    private void buildNewWordsReverse() {
        newWordsReverse = new HashMap<Integer, String>();

        for (final String term : newWords.keySet()) {
            final Integer index = newWords.get(term);
            newWordsReverse.put(index, term);
        }
    }

    /**
     * Dump a textual version of the store to System.out.
     *
     * @param maxRowsOut the maximum number of rows to display. -1 means
     *                   display all rows.
     */
    @SuppressWarnings("unchecked")
    public final void dumpStore(final int maxRowsOut) {
        System.out.println("Contents of CaseInsensitiveStore");
        System.out.println("--------------------------------");

        System.out.println("Data stored in wordsMap");
        if (maxRowsOut != -1) {
            System.out.println("Limiting output to " + maxRowsOut
                    + " rows");
        }

        int count = 1;
        final Set<Integer> keys = wordsMap.keySet();
        final Map<Integer, Integer> sizes = new HashMap<Integer, Integer>();
        final ArrayList<String> zeros = new ArrayList<String>();
        StringBuffer sb = null;
        boolean displayOutput = true;
        for (final int key : keys) {
            final int[] iterms = getWordsAsArray(key);

            if (displayOutput) {
                sb = new StringBuffer();
                sb.append("  ").append(lookupTermFromIndex(key, DECORATOR))
                        .append(" -> ");
            }

            final int curSize = iterms.length;
            if (curSize == 0) {
                zeros.add(lookupTermFromIndex(key, DECORATOR));
            }

            Integer mapPos = sizes.get(curSize);
            if (mapPos == null) {
                mapPos = 1;
                sizes.put(curSize, mapPos);
            } else {
                sizes.put(curSize, mapPos + 1);
            }

            if (displayOutput) {
                for (final int val : iterms) {
                    sb.append(lookupTermFromIndex(val, DECORATOR)).append(" ");
                }
                System.out.println(sb.toString());
            }

            if ((maxRowsOut != -1) && (count == maxRowsOut)) {
                displayOutput = false;
            }

            count++;
        }

        System.out.println("Data Store Info");
        System.out.println("basename=" + docmanager.getBasename());
        System.out.println("terms in ds=" + docmanager.getNumberOfTerms());
        System.out.println("newWords.size=" + newWords.size());
        System.out.println("wordsMap.size=" + wordsMap.size());
        System.out.println("number of sizes=" + sizes.size());
        System.out.println("Sizes info");
        for (final int key : sizes.keySet()) {
            final int curSize = sizes.get(key);
            System.out.println("  " + key + " : " + curSize);
        }
        if (zeros.size() > 0) {
            System.out.println("Terms with no expansions");
            for (final String key : zeros) {
                System.out.println("  " + key);
            }
        }
        System.out.println("--------------------------------");
    }

    /**
     * Brief description of the state of the datastructure.
     *
     * @return String brief description.
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(60);
        sb.append("CaseInsensitiveStore:basename=");
        sb.append(docmanager.getBasename());
        sb.append(":newWords.size=").append(newWords.size());
        sb.append(":wordsMap.size=");

        if (wordsMap == null) {
            sb.append("<NULL>");
        } else {
            sb.append(wordsMap.size());
        }

        return sb.toString();
    }

    public ArrayList<String> suggest(final String term) {
        final ArrayList<String> output = new ArrayList<String>();

        LOG.info("CaseInsensitive Lookup for " + term);

        if ((term == null) || (term.length() == 0)) {
            // No term supplied, send no suggestions.
            LOG.info("-- No results, empty term");
            return output;
        }

        final MutableString mterm = new MutableString(term);
        termProcessor.processTerm(mterm);
        final String sterm = mterm.toString();

        if (!sterm.equals(term)) {
            LOG.info("-- Term processor changed our term to " + sterm);
        }

        if (containsUpperCaseLetters(sterm)) {
            // The term contains upper case letters
            // Forcing case sensitivity, send no suggestions.
            LOG.info("-- No results, contains uppercase letters");
            return output;
        }

        // Look for the term in the newWords list then in the docmanager list
        final Integer wordPos = getLowerCaseIndex(sterm, false);

        if (wordPos == null) {
            // Not in any of the lists
            LOG.info("-- No results, term not in index");
            return output;
        }

        if ((wordsMap == null) || (wordsMap.size() == 0)) {
            LOG.error("wordsMap is null or empty");
            return output;
        }

        if ((newWords == null) || (newWords.size() == 0)) {
            LOG.error("newWords is null or empty");
            return output;
        }

        // We have an index to the lowercase version.
        // Find the mixed-case alternatives
        final int[] isuggestions = getWordsAsArray(wordPos);

        if (isuggestions == null) {
            LOG.info("-- No results, no suggestions found");
            return output;
        }

        // Build an ArrayList<String> of the mixed-case alternatives
        for (final int isuggestion : isuggestions) {
            final String suggestion = docmanager.termAsString(isuggestion);
            LOG.info("-- RESULTS: " + suggestion);
            output.add(suggestion);
        }

        return output;
    }
}
