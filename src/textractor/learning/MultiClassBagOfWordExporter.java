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
import textractor.datamodel.annotation.TextFragmentAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Oct 4, 2004
 * Time: 5:26:44 PM
 * To change this template use File | Settings | File Templates.
 */
public final class MultiClassBagOfWordExporter extends SingleBagOfWordExporter {
    public MultiClassBagOfWordExporter(final DocumentIndexManager docmanager,
                                       final boolean isLastFeatureGroup) {
        super(docmanager, isLastFeatureGroup);
    }

    /** for multiclass input into SVM Light
     *
     * @param textFragmentAnnotation
     * @return
     */
    @Override
    protected String annotationLabel(final AnnotationSource textFragmentAnnotation) {
        return Integer.toString(((TextFragmentAnnotation) textFragmentAnnotation).getAnnotation());
    }
}
