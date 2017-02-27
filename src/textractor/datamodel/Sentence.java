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

package textractor.datamodel;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory.MetadataKeys;
import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import textractor.database.DocumentIndexManager;
import textractor.util.TextractorUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

public final class Sentence extends TextractorDocument {
    private String text;
    private Collection<String> extraTextFragments;
    private Article article;
    private String[] potentialMutations;
    private int termNumber;
    private boolean maybeProteinMutation;
    private boolean maybeProteinName;

    /**
     * Positions of words in the sentence text relative to the original
     * document source.
     */
    private List<Integer> positions;

    public static final String REFERENCE_TAG = "!!!REFERENCES!!!";
    public static final String REFERENCE_TAG_HTML =
        "<P>" + REFERENCE_TAG + "</P>";

    private transient MutableString spaceDelimitedTermsCache;
    private transient MutableString spaceDelimitedProcessedTermsCache;

    public Sentence() {
        super();
        termNumber = -1;
    }

    public Sentence(final Article article) {
        this();
        this.article = article;
    }

    public Sentence(final Article article, final String text) {
        this(article);
        setText(text);
    }

    /**
     * Obtains the terms that look like mutations in this sentence.
     *
     * @return An array where each element is the term that is a potential
     *         mutation.
     */
    public String[] getPotentialMutations() {
        return potentialMutations;
    }

    /**
     * Stores the terms that look like mutations in this sentence.
     *
     * @param potentialMutations An array where each element is the term that is
     *                           a potential mutation.
     */
    public void setPotentialMutations(final String[] potentialMutations) {
        this.potentialMutations = potentialMutations;
    }

    public boolean isMaybeProteinMutation() {
        return maybeProteinMutation;

    }

    public void setMaybeProteinMutation(final boolean maybeProteinMutation) {
        this.maybeProteinMutation = maybeProteinMutation;
    }

    public boolean isMaybeProteinName() {
        return maybeProteinName;
    }

    public void setMaybeProteinName(final boolean maybeProteinName) {
        this.maybeProteinName = maybeProteinName;
    }

    @Override
    public String getText() {
        return getFullText().toString();
    }

    private MutableString getFullText() {
        final MutableString fullText = new MutableString(text);
        if (extraTextFragments != null) {
            for (final String fragment : extraTextFragments) {
                fullText.append(fragment);
            }
        }
        return fullText;
    }

    //public static final int TEXT_MAX_LENGTH = 32766;
    public static final int TEXT_MAX_LENGTH = 20000;

    // FastObjects String must be shorter than  32767. However, we cannot
    // use the 32766 limit because some objects get corrupted when this limit is used.
    // It looks like FastObjects does not factor Unicode into the calculation of the
    // string length, so that when a unicode string is used that contain special characters,
    // and is just shorter than the limit, the string is accepted but corrupts the database.

    public void setText(final String newText) {
        assert newText != null;

        // split very long strings into newText fragments. The first fragment
        // goes into newText, extra fragments go into extraTextFragments.
        final int length = newText.length();
        if (length < TEXT_MAX_LENGTH) {
            this.text = newText;
            extraTextFragments = null;
        } else {
            this.text = newText.substring(0, TEXT_MAX_LENGTH);
            int reducedLength = length - TEXT_MAX_LENGTH;
            int offset = TEXT_MAX_LENGTH;
            extraTextFragments = new ArrayList<String>();
            do {
                final int endIndex = Math.min(TEXT_MAX_LENGTH + offset, length);
                final String newFragment = newText.substring(offset, endIndex);
                extraTextFragments.add(newFragment);
                final int newFragmentLength = newFragment.length();
                reducedLength -= newFragmentLength;
                offset += newFragmentLength;
            }  while (reducedLength > 0);
        }
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(final Article article) {
        this.article = article;
    }

    /**
     * Get the positions of words in the sentence text relative to the original
     * document source.
     * @return a list of positions which may be null if not available
     */
    public List<Integer> getPositions() {
        return positions;
    }

    /**
     * Set the positions of words in the sentence text relative to the original
     * document source.
     * @param positions a list of positions
     */
    public void setPositions(final List<Integer> positions) {
        this.positions = positions;
    }

    /**
     * Does this sentence have position information associated with the text?
     * @return true if position data is available
     */
    public boolean hasPositons() {
        return CollectionUtils.isNotEmpty(positions);
    }

    /**
     * Returns the number of terms in this sentence.
     *
     * @return Number of terms in this sentence, if the method
     *         getSpaceDelimitedTerms was previously called, or -1 if the number
     *         was never calculated.
     */
    public int getTermNumber() {
        return termNumber;
    }

    /**
     * Returns the text of this sentence in a format where words are delimited
     * by a single space character. The algorithm used to split the sentence
     * into terms is the same as used by MG4J, allowing direct calculation of
     * the word positions with a StringTokenizer that would split on spaces. As
     * a side effect, this method calculates and stores the number of terms in
     * this sentence. That number is then available through getTermNumber().
     * <p/> This method does not process the terms: each term is returned with
     * the capitalization that it had in the input text.
     *
     * @param docmanager DocumentIndexManager used to process terms
     * @return the text of this sentence in a format where words are delimited
     *         by a space character, or null if getText() returns null.
     */
    public MutableString getSpaceDelimitedTerms(final DocumentIndexManager docmanager) {
        if (spaceDelimitedTermsCache != null) {
            return spaceDelimitedTermsCache;
        }

        if (getText() == null) {
            return null;
        }
        spaceDelimitedTermsCache = new MutableString();
        final int number =
                docmanager.splitText(getFullText(), spaceDelimitedTermsCache);
        if (termNumber == -1) {
            termNumber = number; // only update if not already calculated.
        }
        spaceDelimitedTermsCache.trim();
        // remove the sentence terminator
        if (spaceDelimitedTermsCache.length() > 1
                && TextractorUtils
                .characterIsSentenceTerminator(spaceDelimitedTermsCache
                        .lastChar())) {
            spaceDelimitedTermsCache.setLength(spaceDelimitedTermsCache
                    .length() - 1);
        }
        return spaceDelimitedTermsCache;
    }

    /**
     * Returns the text of this sentence in a format where words have been
     * processed and are delimited by a single space character. The algorithm
     * used to split the sentence into terms is the same as used by MG4J,
     * allowing direct calculation of the word positions with a StringTokenizer
     * that would split on spaces. As a side effect, this method calculates and
     * stores the number of terms in this sentence. That number is then
     * available through getTermNumber().
     *
     * @param docmanager DocumentIndexManager used to process terms
     * @return the text of this sentence in a format where words are delimited
     *         by a single space character, or null if getText() returns null.
     * @see it.unimi.dsi.mg4j.index.TermProcessor Implementations of this
     *      interface are used to process terms to construct the result.
     */
    public MutableString getSpaceDelimitedProcessedTerms(
            final DocumentIndexManager docmanager) {
        if (spaceDelimitedProcessedTermsCache != null) {
            return spaceDelimitedProcessedTermsCache;
        }

        if (getText() == null) {
            return null;
        }

        if (termNumber == 0) {
            return new MutableString("");
        }

        final String spaceDelimitedTerms =
                getSpaceDelimitedTerms(docmanager).toString();

        spaceDelimitedProcessedTermsCache =
                getSpaceDelimitedProcessedTerms(docmanager, spaceDelimitedTerms);
        return spaceDelimitedProcessedTermsCache;

    }

    /**
     * Process each term with the termProcessor and returns the result.
     *
     * @param docmanager          DocumentIndexManager used to process terms
     * @param spaceDelimitedTerms Input string, where terms are delimited by
     *                            single space characters.
     * @return Input term sequence where terms are substituted by their
     *         processed term equivalent.
     */
    public static MutableString getSpaceDelimitedProcessedTerms(
            final DocumentIndexManager docmanager,
            final String spaceDelimitedTerms) {
        final StringTokenizer st =
                new StringTokenizer(spaceDelimitedTerms, " ");

        final MutableString word = new MutableString();
        final MutableString spaceDelimitedProcessedTerms = new MutableString();
        final TermProcessor termProcessor = docmanager.getTermProcessor();
        while (st.hasMoreTokens()) {
            word.replace(st.nextToken());
            termProcessor.processTerm(word);
            spaceDelimitedProcessedTerms.append(word);
            spaceDelimitedProcessedTerms.append(' ');
        }

        return spaceDelimitedProcessedTerms.trim();
    }

    /**
     * Get the metadata for this document.
     *
     * @return map containing metadata
     */
    @Override
    public Reference2ObjectMap<Enum<?>, Object> getMetaData() {
        final Reference2ObjectMap<Enum<?>, Object> metadata = super.getMetaData();
        final String url = StringUtils.defaultString(article.getLink());
        metadata.put(MetadataKeys.URI, url);
        return metadata;
    }
}
