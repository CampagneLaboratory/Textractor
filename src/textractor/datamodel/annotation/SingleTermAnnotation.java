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
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Stores single annotation about a text fragment.
 * Annotation is produced by humans and entered into the database.
 * This class supports this _process.
 * example:
 * --- 1 --- A:protein < enter ...
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Apr 6, 2004
 * Time: 11:18:29 AM
 * To change this template use File | Settings | File Templates.
 */
public final class SingleTermAnnotation extends TextFragmentAnnotation {
    public static final String ANNOTATION_MUTATION = "mutation";
    public static final String ANNOTATION_PROTEIN = "protein";
    public static final String ANNOTATION_MULTICLASS = "multiclass";
    public static final int INT_ANNOTATION_MUTATION = 1;
    public static final int INT_ANNOTATION_PROTEIN = 2;
    public static final int INT_ANNOTATION_MULTICLASS =3;

    private static final Object2IntMap<String> annotationMap;

    static {
        annotationMap = new Object2IntOpenHashMap<String>(3);
        annotationMap.put(ANNOTATION_MUTATION, INT_ANNOTATION_MUTATION);
        annotationMap.put(ANNOTATION_PROTEIN, INT_ANNOTATION_PROTEIN);
        annotationMap.put(ANNOTATION_MULTICLASS, INT_ANNOTATION_MULTICLASS);
    }

    private AnnotatedTerm term;

    /**
     * Creates a new Single Term Annotation.
     */
    public SingleTermAnnotation() {
        super();
        // empty constructor for JDO
    }

    public SingleTermAnnotation(final int annotationBatch,
                                final int annotationNumber) {
        super(annotationBatch, annotationNumber);
        term = createAnnotatedTerm();
        this.setIsSingleTerm(true);
    }

    public AnnotatedTerm getTerm() {
        return term;
    }

    @Override
    public String getStringType() {
        if (getBooleanAnnotation(INT_ANNOTATION_MUTATION)) {
            return "mutation";
        } else if (getBooleanAnnotation(INT_ANNOTATION_PROTEIN)) {
            return "protein";
        } else {
            return null;
        }
    }

    /**
     * Calculates and returns the center of the terms described in this
     * annotation. For single term annotations, the center is the position of
     * the term itself.
     *
     * @return The position of the term in the text of this annotation.
     */
    public int getCenterPosition() {
        return getTerm().getStartPosition();
    }

    public AnnotatedTerm getTerm(final int index) {
        return getTerm();
    }

    public int getTermNumber() {
        return 1; // there is only one term in this annotation.
    }

    @Override
    public Object2IntMap<String> getAnnotationMap() {
        return annotationMap;
    }
}
