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
 * A source of annotation information. Implementations of this interface
 * do not need to be persistent in the way TextFragmentAnnotation is.
 * The interface has all the methods needed to support the export of
 * SVM features.
 */
public interface AnnotationSource {
    int FIRST_TERM = 0;
    int SECOND_TERM = 1;

    /**
     * Obtain the text of this annotation.
     * @param manager The document manager that will be used to obtain
     * the text.
     * @return The text associated with this annotation.
     */
    String getCurrentText(final DocumentIndexManager manager);

    /**
     * Returns the sentence associated with this annotation.
     * @return The sentence associated with this annotation.
     */
    Sentence getSentence();

    /**
     * Returns the text of this annotation in its indexed form.
     * In the indexed form, each term/word of the text is represented
     * by an int.
     * @return  An array of indexed terms, in the order in which these
     * terms occur in the text of this annotation.
     *
     */
    int[] getIndexedTerms();

    /**
     * Calculates and returns the center of the terms described in this
     * annotation.
     * @return The position of the "center" of this annotation.
     */
    int getCenterPosition();

    /**
     * Instructs this annotation to created indexed terms if they do not
     * already exist.
     *
     * @param manager The document manager that will be used to obtain
     * the indexed form of the terms.
     */
    void createIndexedTerms(final DocumentIndexManager manager);

    /**
     * Obtain the annotation corresponding to the type.
     * @param annotationType The type of annotation that should be retrieved.
     * @return The value of this annotation.
     */
    boolean getBooleanAnnotation(final String annotationType);

    /**
     * Returns one of the terms that are the focus of this annotation.
     *
     * @param index Index of the term in this annotation.
     * @return The term at this index in this annotation.
     */
    AnnotatedTerm getTerm(final int index);

    /**
     * Returns the number of terms in this annotation.
     *
     * @return the number of terms in this annotation.
     */
    int getTermNumber();
}
