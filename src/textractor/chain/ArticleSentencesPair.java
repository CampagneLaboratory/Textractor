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

package textractor.chain;

import textractor.datamodel.Article;
import textractor.datamodel.Sentence;

import java.util.Collection;

/**
 * A container used to group sentences together with the article
 * they relate to.
 */
public class ArticleSentencesPair {
    public final Article article;
    public final Collection<Sentence> sentences;

    public ArticleSentencesPair(final Article article,
            final Collection<Sentence> sentences) {
        super();
        assert article != null : "Article cannot be null";
        assert sentences != null : "Sentences cannot be null";
        this.article = article;
        this.sentences = sentences;
    }
}
