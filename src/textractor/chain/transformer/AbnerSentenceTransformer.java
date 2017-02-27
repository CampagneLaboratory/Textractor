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

package textractor.chain.transformer;

import abner.Tagger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.chain.AbstractSentenceTransformer;
import textractor.chain.ArticleSentencesPair;
import textractor.datamodel.Sentence;
import textractor.sentence.SentenceProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link textractor.sentence.SentenceTransformer} which will replace
 * sentence named entities text and positions with their more generic
 * type names (i.e., protein, dna, etc.)
 */
public final class AbnerSentenceTransformer
        extends AbstractSentenceTransformer {
    /** Used to log debug and informational messages. */
    private static final Log LOG =
        LogFactory.getLog(AbnerSentenceTransformer.class);

    /** Number of articles processed so far. */
    private final AtomicInteger numberOfArticlesProcessed =
        new AtomicInteger();

    /** Number of sentences processed so far. */
    private final AtomicInteger numberOfSentencesProcessed =
        new AtomicInteger();

    /**
     * Used to perform named entity tagging.
     */
    private static final Tagger TAGGER = new Tagger();

    /**
     * Create a new {@link textractor.sentence.SentenceTransformer} that
     * that replaces named entities in a sentence.
     */
    public AbnerSentenceTransformer() {
        super();
    }

    /**
     * Get the number of articles processed so far.
     * @return The number of articles processed so far
     */
    public int getNumberOfArticlesProcessed() {
        return numberOfArticlesProcessed.get();
    }

    /**
     * Get the number of sentences processed so far.
     * @return The number of sentences processed so far
     */
    public int getNumberOfSentencesProcessed() {
        return numberOfSentencesProcessed.get();
    }

    /**
     * Transform text to replace named entities using the {@link abner.Tagger}.
     *
     * @param pair The ArticleSentencesPair to be transformed.
     * @return A (possibly null) ArticleSentencesPair based on the original
     * @throws textractor.sentence.SentenceProcessingException If there was an
     * error condition in the textractor sentence processing pipeline
     */
    public ArticleSentencesPair transform(final ArticleSentencesPair pair)
            throws SentenceProcessingException {
        assert pair != null;
        for (final Sentence sentence : pair.sentences) {
            final String text = sentence.getText();
            final List<Integer> positions = sentence.getPositions();

            final StringBuffer newText = new StringBuffer(text.length());
            final List<Integer> newPositions =
                    new ArrayList<Integer>(positions.size());

            int lastIndex = 0;

            // Big assumption here that the entities are in order they are found
            final String[][] entities = TAGGER.getEntities(text);
            for (int i = 0; i < entities[0].length; i++) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(entities[1][i] + " [" + entities[0][i] + "]");
                }
                final String entity = entities[0][i];
                final String entityType = entities[1][i];
                final int start = text.indexOf(entity, lastIndex);
                if (start != -1) {
                    // copy the previous text and positions
                    for (int j = lastIndex; j < start; j++) {
                        newText.append(text.charAt(j));
                        newPositions.add(positions.get(j));
                    }

                    // replace the entity with it's type (i.e., "[PROTEIN]")
                    newText.append("abner");
                    newText.append(entityType.replaceAll("_", "").toLowerCase());

                    // but keep the old positions
                    final int length = entity.length();
                    final int oldStart = positions.get(start);
                    final int oldEnd = positions.get(start + length - 1);
                    newPositions.add(oldStart);
                    for (int j = start + 1; j < start + length; j++) {
                        newPositions.add(oldEnd);
                    }
                    lastIndex = start + length;
                }
            }

            // copy the last text and positions
            for (int j = lastIndex; j < text.length(); j++) {
                newText.append(text.charAt(j));
                newPositions.add(positions.get(j));
            }

            sentence.setText(newText.toString());
            sentence.setPositions(newPositions);

            numberOfSentencesProcessed.getAndIncrement();
        }
        numberOfArticlesProcessed.getAndIncrement();
        return pair;
    }
}
