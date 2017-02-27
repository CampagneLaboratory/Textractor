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

package textractor.sentence;

import textractor.datamodel.Article;
import textractor.datamodel.Sentence;

/**
 * A sentence producer creates a {@link Sentence} based on a sequence
 * of text and an associated {@link Article}.
 */
public interface SentenceProducer extends SentenceProcessor {
    /**
     * Produce a new Sentence.
     * @param article The article the sentence will be associated with.
     * @param text The text to be used for the sentence.
     * @return The sentence object based on the article and text sequence.
     * @throws SentenceProcessingException If there was an error condition
     * in the textractor sentence processing pipeline
     */
    Sentence produce(final Article article, final CharSequence text)
        throws SentenceProcessingException;
}
