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
import textractor.datamodel.DoubleBagOfWordFeatureCreationParameters;
import textractor.datamodel.FeatureCreationParameters;
import textractor.datamodel.annotation.AnnotatedTerm;
import textractor.datamodel.annotation.AnnotationSource;
import textractor.datamodel.annotation.DoubleTermAnnotation;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: May 10, 2004
 * Time: 5:27:52 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DoubleBagOfWordExporter extends BagOfWordExporter implements SVMFeatureExporter, ExcludedPositionProvider {
    private DoubleBagOfWordFeatureCreationParameters dfcp;
    private final SingleBagOfWordExporter boweA;
    private final SingleBagOfWordExporter boweB;

    public DoubleBagOfWordExporter(final DocumentIndexManager docmanager) {
        this.docmanager = docmanager;
        boweA = new ProteinNameBagOfWordExporter(docmanager, false);
        boweA.setExcludedPositionProvider(this);
        boweB = new ProteinNameBagOfWordExporter(docmanager, true);
        boweB.setExcludedPositionProvider(this);

    }

    public void firstPass(final FeatureCreationParameters fcp,
                          final Collection<AnnotationSource> annotations) {
        dfcp = (DoubleBagOfWordFeatureCreationParameters) fcp;
        boweA.firstPass(dfcp.getParameterA(), annotations, AnnotationSource.FIRST_TERM);
        dfcp.getParameterB().setFirstFeatureNumber(dfcp.getParameterA().getLastFeatureNumber() + 1);
        boweB.firstPass(dfcp.getParameterB(), annotations, AnnotationSource.SECOND_TERM);
    }

    public final void secondPass(final int svmClass, final FeatureCreationParameters fcp,
                           final Collection<AnnotationSource> annotations,
                           final Writer writer) throws IOException {
        dfcp = (DoubleBagOfWordFeatureCreationParameters) fcp;

        int totalAnnotations = 0;
        for (final AnnotationSource annotation : annotations) { // for each annotation
            totalAnnotations++;
            secondPass(svmClass, dfcp, annotation, writer);
        }
        System.out.println("secondPass has parsed: " + totalAnnotations);
    }

    public final void secondPass(final int svmClass,
                           final DoubleBagOfWordFeatureCreationParameters dfcp,
                           final AnnotationSource annotation,
                           final Writer writer) throws IOException {
        final int[] indexedTerms;
        annotation.createIndexedTerms(docmanager);
        indexedTerms = annotation.getIndexedTerms();

        if (dfcp.getParameterA().getFirstFeatureNumber() == 0) {
            // write label: 1 if sentence annotated, -1 otherwise.
            //writer.write(textFragmentAnnotation.getAnnotationNumber()+" ");
            exportClassLabel(svmClass, writer, annotation);
        }

        final DoubleTermAnnotation da = (DoubleTermAnnotation) annotation;

        final int windowCenterA = da.getTermA().getStartPosition();
        final int termALength=da.getTermA().getTermLength();
        boweA.exportSingleFeatureWindow(dfcp.getParameterA(), windowCenterA, termALength, indexedTerms, writer);

        final int windowCenterB = da.getTermB().getStartPosition();
        final int termBLength = da.getTermB().getTermLength();
        boweB.exportSingleFeatureWindow(dfcp.getParameterB(), windowCenterB, termBLength, indexedTerms, writer);

    }

    public final String[] getTerms() {
        return dfcp.getTerms();
    }

    public final FeatureCreationParameters createFeatureCreationParameters() {
        return new DoubleBagOfWordFeatureCreationParameters();
    }

    /**
     * Returns the set of positions that should be excluded from the feature export for this annotation.
     *
     * @param annotation The annotation for which excluded positions are sought.
     * @return An array of int, each element of the array codes for one position (in word) within the annotation's text.
     */
    public final int[] excludedPositions(final AnnotationSource annotation) {
        final AnnotatedTerm firstTerm = annotation.getTerm(AnnotationSource.FIRST_TERM);
        final AnnotatedTerm secondTerm = annotation.getTerm(AnnotationSource.SECOND_TERM);

        final int[] exludedPositions = new int[firstTerm.getTermLength() +
                secondTerm.getTermLength()];
        int count = 0;
        for (int i = firstTerm.getStartPosition(); i < firstTerm.getStartPosition()+firstTerm.getTermLength(); ++i) {
            exludedPositions[count++] = i;
        }
        for (int i = secondTerm.getStartPosition(); i < secondTerm.getStartPosition()+secondTerm.getTermLength(); ++i) {
            exludedPositions[count++] = i;
        }
        assert count == exludedPositions.length;
        return exludedPositions;
    }

}

