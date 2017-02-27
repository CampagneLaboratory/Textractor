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

package textractor.learning;

import textractor.database.DocumentIndexManager;
import textractor.datamodel.annotation.AnnotationSource;
import textractor.datamodel.annotation.SingleTermAnnotation;

/**
 * Returns label 1 when the annotation has protein name attribute set,
 * 0 otherwise.
 *
 * User: Fabien Campagne
 * Date: Jan 31, 2004
 * Time: 2:12:33 PM
 */
public final class ProteinNameBagOfWordExporter extends SingleBagOfWordExporter {
    public ProteinNameBagOfWordExporter(final DocumentIndexManager docmanager,
                                        final boolean isLastFeatureGroup) {
        super(docmanager, isLastFeatureGroup);
    }

    @Override
    protected String annotationLabel(final AnnotationSource textFragmentAnnotation) {
        return (textFragmentAnnotation.getBooleanAnnotation(SingleTermAnnotation.ANNOTATION_PROTEIN)?"1":"-1");  // for input into SVM Light, proteins/not-proteins must be marked with 1 or -1.
    }

}
