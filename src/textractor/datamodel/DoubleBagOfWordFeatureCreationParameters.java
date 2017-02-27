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

package textractor.datamodel;

import textractor.database.DocumentIndexManager;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: May 10, 2004
 * Time: 5:26:10 PM
 * To change this template use File | Settings | File Templates.
 */
public final class DoubleBagOfWordFeatureCreationParameters
    extends FeatureCreationParameters {

    private SingleBagOfWordFeatureCreationParameters sfcpA;
    private SingleBagOfWordFeatureCreationParameters sfcpB;

    public DoubleBagOfWordFeatureCreationParameters() {
        super();
        sfcpA = new SingleBagOfWordFeatureCreationParameters();
        sfcpB = new SingleBagOfWordFeatureCreationParameters();
    }

    public void setParameterA(final SingleBagOfWordFeatureCreationParameters sfcp) {
        sfcpA = sfcp;
    }

    public void setParameterB(final SingleBagOfWordFeatureCreationParameters sfcp) {
        sfcpB = sfcp;
    }

    public SingleBagOfWordFeatureCreationParameters getParameterA(){
        return sfcpA;
    }

    public SingleBagOfWordFeatureCreationParameters getParameterB(){
        return sfcpB;
    }

    @Override
    public String[] getTerms() {
        final int lengthA = sfcpA.getTerms().length;
        final int lengthB = sfcpB.getTerms().length;
        final String[] terms = new String[lengthA + lengthB];
        for (int i = 0; i < lengthA; i++) {
            terms[i] = sfcpA.getTerms()[i];
        }
        for (int i = 0; i < lengthB; i++) {
            terms[i + lengthA] = sfcpB.getTerms()[i];
        }
        return terms;
    }

    @Override
    public void updateIndex(final DocumentIndexManager docmanager) {
        sfcpA.updateIndex(docmanager);
        sfcpB.updateIndex(docmanager);
    }

    @Override
    public void clearTerms() {
        sfcpA.clearTerms();
        sfcpB.clearTerms();
    }

    @Override
    public void removeTerm(final String term, final int indexedTerm) {
        sfcpA.removeTerm(term, indexedTerm);
        sfcpB.removeTerm(term, indexedTerm);
    }

    @Override
    public void setTerms(final DocumentIndexManager docmanager) {
        sfcpA.setTerms(docmanager);
        sfcpB.setTerms(docmanager);
    }

    @Override
    public void setWindowSize(final int windowSize) {
        this.windowSize = windowSize;
        this.getParameterA().setWindowSize(windowSize);
        this.getParameterB().setWindowSize(windowSize);
    }
}
