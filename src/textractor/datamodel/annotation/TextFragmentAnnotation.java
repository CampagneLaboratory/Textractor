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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;
import textractor.datamodel.Sentence;

/**
 * Stores annotations about a text fragment. Annotations are produced by humans
 * and entered into the database. This class supoprts this _process.
 * <p/>
 * Creation Date: Jan 15, 2004
 * Creation Time: 3:41:54 PM
 *
 * @author Fabien Campagne
 */

public abstract class TextFragmentAnnotation implements AnnotationSource {
    private int annotationNumber;
    private long annotation;
    private boolean isSingleTerm;
    private boolean annotationImported;
    private int annotationBatchNumber;
    private Sentence sentence;
    private String currentText;
    private int[] indexedTerms;
    private boolean useSentenceText;
    private static final Log LOG =
        LogFactory.getLog(TextFragmentAnnotation.class);

    /**
     * Creates a new TextFragment Annotation.
     */
    public TextFragmentAnnotation() {
        super();
        // empty constructor for JDO
    }

    /**
     * Creates a new TextFragment Annotation.
     *
     * @param batch Number of the annotation batch. One batch groups
     *        possibly many annotations in one set. All the annotations in one
     *        batch have the same batch number and can thus be retrieved
     *        together.
     * @param number Unique number to identify this annotation.
     */
    public TextFragmentAnnotation(final int batch, final int number) {
        super();
        this.annotationBatchNumber = batch;
        this.annotationNumber = number;
        this.useSentenceText = true;
    }

    public final int getAnnotationBatchNumber() {
        return annotationBatchNumber;
    }

    public final int getAnnotationNumber() {
        return annotationNumber;
    }

    /**
     * Returns the text of this annotation. If the annotation uses the sentence
     * as text provider, returns the same as the sentence's getText() method.
     * Otherwise, returns the text stored in this annotation.
     */
    public final String getCurrentText(final DocumentIndexManager docmanager) {
        if (isUseSentenceText()) {
            return sentence.getSpaceDelimitedTerms(docmanager).toString();
        } else {
            return currentText;
        }
    }

    /**
     * Sets the text of this annotation. If a sentence has been set on this
     * annotation, this method has no effect. If no sentence has been attached
     * to this annotation, useSentenceText is forcefully set to false, as it
     * does not make sense to store text in a sentence that does not exist, and
     * therefore the intention of the developer is to give the annotation its
     * own text.
     *
     * @param text Text to store in this annotation.
     */
    public final void setCurrentText(final String text) {
        if (sentence == null) {
            setUseSentenceText(false);
        }

        if (isUseSentenceText()) {
            LOG.error("setCurrentText cannot be called on an annotation for which useSentenceText is true.");
        } else {
            currentText = text;
        }
    }

    public final boolean getIsSingleTerm() {
        return isSingleTerm;
    }

    public final void setIsSingleTerm(final boolean s) {
        isSingleTerm = s;
    }

    /**
     * Returns the sentence from which the text fragment is derived.
     *
     * @return The sentence from which the text fragment is derived, or null
     * if the annotation has not been attached to a sentence.
     */
    public final Sentence getSentence() {
        return sentence;
    }

    /**
     * Attaches this annotation to the sentence from which the text fragment
     * derives.
     *
     * @param sentence Sentence that contains the text fragment.
     */
    public final void setSentence(final Sentence sentence) {
        this.sentence = sentence;
    }

    /**
     * Has this annotation being imported back into the database?
     * Annotations are only correct after the annotation has been imported.
     *
     * @return True if the annotation has been imported. False if the annotation
     * has never been imported.
     */
    public final boolean isAnnotationImported() {
        return annotationImported;
    }

    public final void setAnnotationImported(final boolean annotationImported) {
        this.annotationImported = annotationImported;
    }

    protected final AnnotatedTerm createAnnotatedTerm() {
        return new AnnotatedTerm();
    }

    public abstract Object2IntMap<String> getAnnotationMap();

    public final boolean getBooleanAnnotation(final String annotationType) {
        return getBooleanAnnotation(getAnnotationMap().getInt(annotationType));
    }

    public final boolean getBooleanAnnotation(final int annotationType) {
        if (annotationType > 63) {
            throw new IllegalArgumentException("annotationType "
                    + annotationType + " is unsupported");
        }

        return ((annotation & (1 << annotationType)) != 0);
    }

    public final int[] getIndexedTerms() {
        return indexedTerms;
    }

    /**
     * Returns the type of this annotation as a String. returns protein for
     * SingleTermAnnotations when the term is annotated as a protein,
     * mutation when the term is annotated as a mutation.
     * @return The type of this annotation
     */
    public abstract String getStringType();

    /**
     * Returns whether or not this annotation has any flag true.
     */
    public final boolean isSet() {
        return annotation != 0;
    }

    public final void setAnnotation(final int annotationType, final boolean value) {
        if (annotationType > 63) {
	    throw new IllegalArgumentException("annotationType "
		    + annotationType + " is unsupported");
	}
        if (value) {
            annotation |= (1 << annotationType);
        } else {
            annotation &= 0xFFFFFFFF - (1 << annotationType);
        }
    }

    /** for multiclass
     *
     * @param annotationType
     */
    public final void setAnnotation(final int annotationType) {
        annotation = annotationType;
    }

    /** for multiclass
     *
     * @return The annotation number.
     */
    public final int getAnnotation() {
        return (int)annotation;
    }

    public final void setIndexedTerms(final int[] indexedTerms) {
        this.indexedTerms = indexedTerms;
    }

    /**
     * Instruct this annotation to use the text of the sentence, or its own
     * copy. By default, the annotation will use its own copy of the text.
     * Setting useSentenceText to true can minimize the amount of storage used
     * by annotation who need to contain the same text as the sentence.
     *
     * @param useSentenceText When true, the annotation's getText() method
     *        returns the text of the sentence that it is linked to. When false,
     *        the annotation maintains its own copy of the text.
     */
    public final void setUseSentenceText(final boolean useSentenceText) {
        this.useSentenceText = useSentenceText;
    }

    /**
     * Returnds the value of the useSentenceText property.
     *
     * @return True or False.
     * @see #setUseSentenceText(boolean)
     */
    public final boolean isUseSentenceText() {
        return useSentenceText;
    }

    /**
     * Instructs this annotation to created indexed terms if they do not already
     * exist.
     *
     * @param docManager The document manager that will be used to obtain the
     *        indexed form of the terms.
     */
    public final void createIndexedTerms(final DocumentIndexManager docManager) {
        if (getIndexedTerms() == null || ((getIndexedTerms() != null) && getIndexedTerms().length == 0)) {
            setIndexedTerms(docManager.extractSpaceDelimitedTerms(getSpaceDelimitedIndexFormatTerms(docManager)));
        }
    }

    public final MutableString getSpaceDelimitedIndexFormatTerms(final DocumentIndexManager docmanager){
        if (getSentence() != null) {
            return getSentence().getSpaceDelimitedProcessedTerms(docmanager);
        } else {
            final Sentence transientSentence = new Sentence();
            transientSentence.setText(currentText);
            return transientSentence.getSpaceDelimitedProcessedTerms(docmanager);
        }
    }
}

