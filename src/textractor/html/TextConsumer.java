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

package textractor.html;

import textractor.article.ArticleProducer;
import textractor.chain.ArticleSentencesPair;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.sentence.AbstractSentenceProcessor;
import textractor.sentence.SentenceConsumer;
import textractor.sentence.SentenceProducer;
import textractor.sentence.TransactionalSentenceProcessor;

import java.util.Collection;

/**
 * A consumer of text. Consumers are responsible for processing articles and
 * their sentences.
 *
 * @author Fabien Campagne
 *         Date: Aug 16, 2006
 *         Time: 4:06:12 PM
 */
public abstract class TextConsumer extends AbstractSentenceProcessor
    implements TransactionalSentenceProcessor, ArticleProducer,
               SentenceProducer, SentenceConsumer {

    /**
     * Process sentences along with their associated article.
     * @param article The article assoicated with the sentences.
     * @param sentences A collection of Sentences to process.
     * @param callback A {@link CheckpointCallback} that will should get
     * invoked when processing is complete.
     */
    abstract void consume(final Article article,
                          final Collection<Sentence> sentences,
                          final CheckpointCallback callback);

    /**
     * Create a article sentence pair object from the individual objects.
     * @param article The article assoicated with the sentences.
     * @param sentences A collection of Sentences to process.
     */
    public ArticleSentencesPair produce(final Article article,
                                 final Collection<Sentence> sentences) {
        return new ArticleSentencesPair(article, sentences);
    }

}
