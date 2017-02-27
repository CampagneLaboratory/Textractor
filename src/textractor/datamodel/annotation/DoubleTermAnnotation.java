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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Stores two annotations about a text fragment.
 * Annotations are produced by humans and entered into the database.
 * This class supports this _process.
 * example:
 * --- 1 --- A|B:interaction|bind < enter ...
 * <p/>
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Apr 6, 2004
 * Time: 11:20:13 AM
 * To change this template use File | Settings | File Templates.
 */
public final class DoubleTermAnnotation extends TextFragmentAnnotation {
    public static final int TERMA_NUMBER = 1;
    public static final int TERMB_NUMBER = 2;

    public static final String ANNOTATION_BIND = "bind";
    public static final String ANNOTATION_PHOSPHORYLATE = "phosphorylate";
    public static final int INT_ANNOTATION_BIND = 1;
    public static final int INT_ANNOTATION_PHOSPHORYLATE = 2;

    public static final Collection<String> isOneDirection;

    private AnnotatedTerm termA;
    private AnnotatedTerm termB;

    private static final Object2IntMap<String> annotationMap;

    static {
        isOneDirection = new ArrayList<String>();
        isOneDirection.add(ANNOTATION_PHOSPHORYLATE);

        annotationMap = new Object2IntOpenHashMap<String>(2);
        annotationMap.put(ANNOTATION_BIND, INT_ANNOTATION_BIND);
        annotationMap.put(ANNOTATION_PHOSPHORYLATE, INT_ANNOTATION_PHOSPHORYLATE);
    }

    /**
     * Creates a new Double Term Annotation.
     */
    public DoubleTermAnnotation() {
        super();
        // empty constructor for JDO
    }

    public DoubleTermAnnotation(final int annotationBatch,
            final int annotationNumber) {
        super(annotationBatch, annotationNumber);
        termA = createAnnotatedTerm();
        termB = createAnnotatedTerm();
    }

    @Override
    public String getStringType() {
        if (getBooleanAnnotation(INT_ANNOTATION_BIND)) {
            return "bind";
        } else if (getBooleanAnnotation(INT_ANNOTATION_PHOSPHORYLATE)) {
            return "phosphorylate";
        } else {
            return null;
        }
    }

    /**
     * Calculates and returns the center of the terms described in this
     * annotation. For double term annotations, the center is in the middle of
     * the position of the two terms.
     *
     * @return The position of the "center" of this annotation.
     */
    public int getCenterPosition() {
        return (getTermA().getStartPosition() + getTermB().getStartPosition()) / 2;

    }

    public AnnotatedTerm getTerm(final int index) {
        if (index == 0) {
            return this.termA;
        } else if (index == 1) {
            return this.termB;
        } else {
            throw new ArrayIndexOutOfBoundsException("index cannot be greater than 1.");
        }
    }

    public int getTermNumber() {
        return 2;
    }

    public AnnotatedTerm getTermA() {
        return termA;
    }

    public AnnotatedTerm getTermB() {
        return termB;
    }

    public AnnotatedTerm getTermByNumber(final int termNumber) {
        if (termNumber == TERMA_NUMBER) {
            return getTermA();
        } else if (termNumber == TERMB_NUMBER) {
            return getTermB();
        } else {
            return null;
        }
    }

    @Override
    public Object2IntMap<String> getAnnotationMap() {
        return annotationMap;
    }

    public void switchTerms() {
        final AnnotatedTerm termTemp = termA;
        termA = termB;
        termB = termTemp;
    }
}
