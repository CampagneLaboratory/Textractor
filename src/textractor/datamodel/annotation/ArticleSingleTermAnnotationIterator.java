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

package textractor.datamodel.annotation;

import textractor.database.DocumentIndexManager;
import textractor.database.TextractorManager;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: campagne
 * Date: May 28, 2004
 * Time: 2:21:21 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ArticleSingleTermAnnotationIterator implements Iterator<ArticleSingleTermAnnotationSource> {
    private Sentence sentence;
    private final int[] indexedTermOfInterest;
    private final DocumentIndexManager docManager;

    private final Iterator<Sentence> sentenceIterator;

    private static final int TERM_NOT_FOUND = -1;
    private int termPositionInSentence = TERM_NOT_FOUND;
    private int[] indexedTerms;

    /**
     * Creates an iterator to iterate through annotations that would be
     * produced from an article and a term.
     *
     * @param tm
     * @param docManager
     * @param article
     * @param indexedTermOfInterest The indexed term that we should look for
     * in sentences from this article.
     */
    public ArticleSingleTermAnnotationIterator(final TextractorManager tm,
            final DocumentIndexManager docManager, final Article article,
            final int[] indexedTermOfInterest) {
        this.docManager = docManager;
        sentenceIterator = tm.getSentenceIterator(article);

        this.indexedTermOfInterest = indexedTermOfInterest;
    }

    public boolean hasNext() {
        while (true) {
            if (sentence == null) {
                if (sentenceIterator.hasNext()) {
                    sentence = sentenceIterator.next();
                } else {
                    return false; // no more sentences for this article. We are done.
                }
            }

            if (sentence == null) {
                return false;
            }

            indexedTerms = docManager.extractSpaceDelimitedTerms(sentence.getSpaceDelimitedProcessedTerms(docManager));
            // termPositionInSentence will be reinitiated to -1 at the end of the search of last sentence "return TERM_NOT FOUND;"
            termPositionInSentence = findTermPositionInSentence(termPositionInSentence + 1);
            if (termPositionInSentence == TERM_NOT_FOUND) {
                sentence = null; // force looking at next sentence.
            } else {
                // we found a term in the sentence. Now, the following fields have been initialized
                // and are ready for a call to next():
                // - article
                // - sentence
                // - termPositionInSentence
                // - indexedTerms
                return true;
            }
        }
    }

    private int findTermPositionInSentence(final int fromStartPosition) {
        for (int i = fromStartPosition; i < indexedTerms.length - indexedTermOfInterest.length + 1; ++i) {
            int found = 0;
            for (int j = 0; j < indexedTermOfInterest.length; j++) {
                if (indexedTerms[i + j] == indexedTermOfInterest[j]) {
                    found++;
                } else {
                    break;
                }
            }
            if (found == indexedTermOfInterest.length) {
                return i;
            }
        }
        return TERM_NOT_FOUND;
    }

    public ArticleSingleTermAnnotationSource next() {
        return new ArticleSingleTermAnnotationSource(sentence, indexedTerms,
                docManager.multipleWordTermAsString(indexedTermOfInterest),
                termPositionInSentence);
    }

    /**
     * This method has no effect.
     */
    public void remove() {
    }
}
