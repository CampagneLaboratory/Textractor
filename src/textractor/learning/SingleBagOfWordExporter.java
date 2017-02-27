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
import textractor.datamodel.SingleBagOfWordFeatureCreationParameters;
import textractor.datamodel.annotation.AnnotationSource;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * Regroups behaviour for the Bag of Word export.
 * User: Fabien Campagne
 * Date: Jan 19, 2004
 * Time: 1:09:00 PM
 */
public abstract class SingleBagOfWordExporter extends BagOfWordExporter implements SVMFeatureExporter {
    private SingleBagOfWordFeatureCreationParameters sfcp;
    private ExcludedPositionProvider excludedPositionProvider;

    /**
     * Set the provider of excluded position that this exporter should use.
     *
     * @param excludedPositionProvider A Provider that will tell us what position this exporter should not consider during its export.
     */
    public final void setExcludedPositionProvider(final ExcludedPositionProvider excludedPositionProvider) {
        this.excludedPositionProvider = excludedPositionProvider;
    }

    public SingleBagOfWordExporter(final DocumentIndexManager docmanager,
                                   final boolean isLastFeatureGroup) {
        this.docmanager = docmanager;
        setLastFeatureGroup(isLastFeatureGroup);
    }

    /**
     * First pass determines the terms that should be considered as features.
     *
     * @param annotations Annotations to examine for words
     * @param fcp         The parameters for this export (must be instances of SingleBagOfWordFeatureCreationParameters).
     */
    public final void firstPass(final FeatureCreationParameters fcp, final Collection<AnnotationSource> annotations) {
        sfcp = (SingleBagOfWordFeatureCreationParameters) fcp;
        firstPass(sfcp, annotations);
    }

    /**
     * First pass determines the terms that should be considered as features.
     *
     * @param annotations Annotations to examine for words
     * @param sfcp        The parameters for this export (must be instances of SingleBagOfWordFeatureCreationParameters).
     * @param termNumber  the term number in DoubleTermAnnotation;
     */
    public final void firstPass(final SingleBagOfWordFeatureCreationParameters sfcp,
                          final Collection<AnnotationSource> annotations,
                          final int termNumber) {
        sfcp.clearTerms();
        if (excludedPositionProvider == null) {
            excludedPositionProvider = new ExcludeWindowCenter();
        }

        for (final AnnotationSource annotation : annotations) { // for each annotation
            annotation.createIndexedTerms(docmanager);

            final int wordOfInterestPosition = annotation.getTerm(termNumber).getStartPosition();
            final int termLength = annotation.getTerm(termNumber).getTermLength();
            final int[] excludedPositions = excludedPositionProvider.excludedPositions(annotation);
            sfcp.addWordsToTerms(annotation.getIndexedTerms(), wordOfInterestPosition, termLength, excludedPositions);
        }
        sfcp.setTerms(docmanager);
    }

    /**
     * First pass determines the terms that should be considered as features. For double term annotations, this method will
     * always consider the first term only.
     *
     * @param annotations Annotations to examine for words
     * @param sfcp        The parameters for this export (must be instances of SingleBagOfWordFeatureCreationParameters).
     */
    public final void firstPass(final SingleBagOfWordFeatureCreationParameters sfcp,
                          final Collection<AnnotationSource> annotations) {
        firstPass(sfcp, annotations, AnnotationSource.FIRST_TERM);
    }

    public final void secondPass(final int svmClass,
                           final FeatureCreationParameters fcp,
                           final Collection<AnnotationSource> annotations,
                           final Writer writer) throws IOException {
        sfcp = (SingleBagOfWordFeatureCreationParameters) fcp;

        // Now, export bag of words: if the term appears in the window around
        // the word in the annotation, output 1, otherwise output 0
        // for the feature.
        for (final AnnotationSource annotation : annotations) { // for each annotation
            secondPass(svmClass, sfcp, annotation, writer);
        }
    }

    public final void secondPass(final int svmClass,
                           final SingleBagOfWordFeatureCreationParameters parameters,
                           final AnnotationSource annotation,
                           final Writer writer) throws IOException {
        annotation.createIndexedTerms(docmanager);
        final int[] annotationIndexedTerms = annotation.getIndexedTerms();

        if (parameters.getFirstFeatureNumber() == 0) {
            // write label: 1 if sentence annotated, -1 otherwise.
            //writer.write(textFragmentAnnotation.getAnnotationNumber()+" ");

            //for multiclass export, an integer will be exported

            exportClassLabel(svmClass, writer, annotation);
        }

        final int windowCenter = annotation.getCenterPosition();
        final int termLength = annotation.getTerm(0).getTermLength();

        exportSingleFeatureWindow(parameters, windowCenter, termLength, annotationIndexedTerms, writer);

    }
    protected final void exportSingleFeatureWindow(final SingleBagOfWordFeatureCreationParameters sfcp,
                                             final int windowCenter,
                                             final int termLength,
                                             final int[] annotationIndexedTerms,
                                             final Writer writer) throws IOException {
        int featureNumber = sfcp.getFirstFeatureNumber();

        // calculate minIndex and maxIndex here instead of letting windowContainsTerm do it repeatedly.
        final int minIndex = sfcp.calculateMinWindowIndex(windowCenter);
        final int maxIndex = sfcp.calculateMaxWindowWidth(windowCenter, termLength, annotationIndexedTerms.length);
        // now, write the features:
        // each feature is 1 or 0, depending on the presence or absence of a given term in the window:
        for (int i = 0; i < sfcp.getTermsInWindows().size(); i++, ++featureNumber) {
            if (sfcp.windowContainsTerm(annotationIndexedTerms, sfcp.getTermsInWindows().getInt(i), windowCenter, termLength, minIndex, maxIndex)) {
                writer.write(Integer.toString(featureNumber + 1));
                writer.write(':');
                writer.write('1');
                writer.write(' ');
            }
        }
        if (isLastFeatureGroup()) {
            writer.write('\n');
        }
    }

    public final FeatureCreationParameters createFeatureCreationParameters() {
        return new SingleBagOfWordFeatureCreationParameters();
    }
}
