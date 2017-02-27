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

import java.util.Collection;

/**
 * A sentence consumer processes {@link Sentence}s along with
 * their associated {@link Article}.  Consumers are encouraged to accept
 * {@link textractor.event.sentence.SentenceProcessingCompleteEvent} objects
 * from their corresponding {@link SentenceProducer} or
 * {@link SentenceTransformer}.
 */
public interface SentenceConsumer extends SentenceProcessor {
    /**
     * Process sentences along with their associated article.
     * @param article The article assoicated with the sentences.
     * @param sentences A collection of Sentences to process.
     * @throws SentenceProcessingException If there was an error condition
     * in the textractor sentence processing pipeline
     */
    void consume(final Article article, final Collection<Sentence> sentences)
        throws SentenceProcessingException;
}
