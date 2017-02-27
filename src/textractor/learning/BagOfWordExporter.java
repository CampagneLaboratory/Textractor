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

import java.io.IOException;
import java.io.Writer;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: May 17, 2004
 * Time: 5:49:48 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BagOfWordExporter {
    protected abstract String annotationLabel(AnnotationSource textFragmentAnnotation);
    protected DocumentIndexManager docmanager;

    /** lastFeatureGroup determine if it is the last feature before end the input line for SVM. It may be changed
     * in more than two bags siutations.
     */
    private boolean lastFeatureGroup;

    public boolean isLastFeatureGroup() {
        return lastFeatureGroup;
    }

    public void setLastFeatureGroup(final boolean lastFeatureGroup) {
        this.lastFeatureGroup = lastFeatureGroup;
    }

    protected final void exportClassLabel(final int svmClass, final Writer writer, final AnnotationSource annotation) throws IOException {
        if (svmClass!=SVMFeatureExporter.fromAnnotationClass){
            writer.write(Integer.toString(svmClass));
        }else{
            writer.write(annotationLabel(annotation));
        }
        writer.write(' ');
    }

}
