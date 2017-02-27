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

package textractor.tools;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: Fabien Campagne
 * Date: Jan 10, 2004
 * Time: 12:39:52 PM
 * To change this template use Options | File Templates.
 */
public class TestFeatures extends TestCase {
    public void testNormalize() {
        final Features features = new Features();
        features.setFeatureValue(1, 1);
        features.setFeatureValue(20, 1);
        assertEquals("Normalize must return the same instance", features.normalize(), features);
        final float delta = 1e-6f;
        final float expected = (float) (1 / Math.sqrt(2));
        assertEquals(expected, features.getFeatureValue(1), delta);
        assertEquals(expected, features.getFeatureValue(20), delta);

        features.setFeatureValue(1, 2);
        features.setFeatureValue(20, 3);
        final float expectedNorm = (float)Math.sqrt(2 * 2 + 3 * 3);
        features.normalize();
        assertEquals("Normalize must return the same instance", features.normalize(), features);
        assertEquals(2 / expectedNorm, features.getFeatureValue(1), delta);
        assertEquals(3 / expectedNorm, features.getFeatureValue(20), delta);
    }
}
