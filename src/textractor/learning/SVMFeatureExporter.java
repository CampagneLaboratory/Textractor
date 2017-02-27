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

import textractor.datamodel.FeatureCreationParameters;
import textractor.datamodel.annotation.AnnotationSource;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * User: Fabien Campagne
 * Date: Jan 19, 2004
 * Time: 1:21:31 PM
 */
public interface SVMFeatureExporter {
    int fromAnnotationClass = 0;
    int positiveClass = 1;
    int negativeClass = -1;

    /**
     * Determine what the features are.
     * @param annotations Annotations to examine for words
     * @param parameters The parameters for this export.
     */
    void firstPass(final FeatureCreationParameters parameters,
                   final Collection<AnnotationSource> annotations);

    /**
     * Write the features to the writer.
     * @param parameters Parameters for this export.
     * @param annotations Annotations to convert to features.
     * @param writer Where to write the features.
     * @throws IOException  If an error occured writing to the writer.
     */
    void secondPass(final int svmClass,
                    final FeatureCreationParameters parameters,
                    final Collection<AnnotationSource> annotations,
                    final Writer writer) throws IOException;

    /**
     * Return corresponding type of FeatureCreationParameters.
     *
     * @return
     */
    FeatureCreationParameters createFeatureCreationParameters();
}
