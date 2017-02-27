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
import textractor.datamodel.FeatureCreationParameters;
import textractor.datamodel.annotation.AnnotationSource;
import textractor.datamodel.annotation.DoubleTermAnnotation;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: berber
 * Date: May 5, 2004
 * Time: 8:52:35 PM
 * To change this template use File | Settings | File Templates.
 */
public final class PhosphorylateBagOfWordExporter extends DoubleBagOfWordExporter {
    public PhosphorylateBagOfWordExporter(final DocumentIndexManager docmanager) {
        super(docmanager);
    }

    @Override
    public void firstPass(final FeatureCreationParameters fcp,
	    		  final Collection<AnnotationSource> annotations) {
        super.firstPass(fcp, annotations);

        // now, exclude some terms that we really do not want to see in the features:

        // obviously, this list needs to be customized for each training problem, for now.
        // there are two types of words in this list: 1/ protein names that are part of the training set.
        // 2/ terms which are part of the terms in window but are so specific to this article that they can influence the evaluation.
        final String[] excludeTerms = {
                "PIF", "PDK1", "PKB", "PKBalpha","p70 S6K","ERK2","CKII","L1",
                "p90rsk","Raf 1","ankyrin","PKCalpha","PKCgamma","hRARalpha",
                "308","252","22","D978A","F977A","S6","p70","PDK1binds",
                "PIFpeptide","IGF1","T252A","T412A","412E","p412E",
                "PDK1phosphorylates","Ser1181","PKCalphaand","ERK"};
        for (final String excludeTerm : excludeTerms) {
            fcp.removeTerm(excludeTerm, docmanager.findTermIndex(excludeTerm));
        }
        fcp.setTerms(docmanager); // rebuild String representatio of terms.
    }

    @Override
    protected String annotationLabel(final AnnotationSource textFragmentAnnotation) {
        return (textFragmentAnnotation.getBooleanAnnotation(DoubleTermAnnotation.ANNOTATION_PHOSPHORYLATE) ? "1" : "-1");  // for input into SVM Light, interaction/not-interactions must be marked with 1 or -1.
    }
}
