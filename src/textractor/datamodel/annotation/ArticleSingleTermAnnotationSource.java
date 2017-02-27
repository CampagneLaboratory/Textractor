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
import textractor.datamodel.Sentence;

/**
 * This implementation acts like an annotation without storing one persistently.
 * Data that should be provided by the annotation is instead provided from an
 * article and a few parameters.
 */
public final class ArticleSingleTermAnnotationSource implements AnnotationSource {
    private final Sentence sentence;
    private final int termPositionInSentence;
    private final int[] indexedTerms;
    private final AnnotatedTerm term;

    public ArticleSingleTermAnnotationSource(final Sentence sentence,
                                             final int[] indexedTerms,
                                             final String termString,
                                             final int termPositionInSentence) {
        this.sentence = sentence;
        this.termPositionInSentence = termPositionInSentence;
        term = new AnnotatedTerm();
        term.setStartPosition(termPositionInSentence);
        this.indexedTerms = indexedTerms;
        term.setText(termString, termPositionInSentence);

    }

    public String getCurrentText(final DocumentIndexManager docmanager) {
        return sentence.getText();
    }

    public Sentence getSentence() {
        return sentence;
    }

    public int[] getIndexedTerms() {
        return indexedTerms;
    }

    public int getCenterPosition() {
        return termPositionInSentence;
    }

    public void createIndexedTerms(final DocumentIndexManager docManager) {
        // do nothing. The indexed terms have already be created.
    }

    /**
     * Always returns false. This annotation source should only be used in test
     * sets. Use persistent annotations if you need support for human
     * annotations.
     *
     * @param annotationType The type of annotation that should be retrieved.
     * @return False.
     */
    public boolean getBooleanAnnotation(final String annotationType) {
        return false;
    }

    public AnnotatedTerm getTerm(final int index) {
        return term;
    }

    /**
     * Only one term in this annotation source.
     *
     * @return 1
     */
    public int getTermNumber() {
        return 1;
    }
}
