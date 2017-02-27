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

import it.unimi.dsi.fastutil.ints.Int2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;

import java.util.Map.Entry;
import java.util.Set;

/**
 * A class to store features. Feature are identified by a number and a value. The sequence of feature
 * numbers may have large gaps, so it is not appropriate to store the value of features in an array,
 * where the feature number would be the index, and the feature value the value of the feature.
 * <p/>
 * User: campagne
 * Date: Sep 24, 2004
 * Time: 4:42:41 PM
 * To change this template use File | Settings | File Templates.
 */
public final class Features {
    final Int2FloatMap featureMap;

    public Features() {
        this.featureMap = new Int2FloatAVLTreeMap();
    }

    /**
     * Sets the value of a feature.
     *
     * @param identifier Identifies the feature. Identifiers may have large
     * gaps in their sequence.
     * @param value Value to set the feature to.
     */
    public void setFeatureValue(final int identifier, final float value) {
        featureMap.put(identifier, value);

    }

    public float getFeatureValue(final int identifier) {
        return featureMap.get(identifier);
    }

    /**
     * Increment the value of a feature.
     *
     * @param identifier Identifies the feature. Identifiers may have large gaps
     * in their sequence.
     * @return the new value of the feature, or the default value of the
     * feature, if the feature was not set.
     */
    public float incrementFeatureValue(final int identifier) {
        float oldValue = featureMap.get(identifier);
        if (oldValue != featureMap.defaultReturnValue()) {
            final float newValue = ++oldValue;
            featureMap.put(identifier, newValue);
            return newValue;
        } else {
            featureMap.put(identifier, 1);
            return oldValue;
        }
    }

    /**
     * Sets the default value. This value is returned by incrementFeatureValue
     * when the feature was not set before the call.
     *
     * @param defaultValue
     */
    public void setDefaultValue(final float defaultValue) {
        featureMap.defaultReturnValue(defaultValue);
    }

    public float defaultReturnValue() {
        return featureMap.defaultReturnValue();
    }

    /**
     * Clears the features. After this call, identifier and values are reset to
     * the state they were at construction time.
     */
    public void clear() {
        featureMap.clear();
    }

    /**
     * Normalize the features. Divide each feature value by the norm of the
     * feature values.
     * @return The same feature instance, with values normalized.
     */
    public Features normalize() {
        // calculate the norm:   sum the square of each feature and take the square root
        double sumOfSquare = 0;
        final Set<Entry<Integer, Float>> entrySet = featureMap.entrySet();
        for (final Entry<Integer, Float> entry : entrySet) {
            sumOfSquare += entry.getValue()* entry.getValue();
        }

        final double norm = Math.sqrt(sumOfSquare);

        // now divide each feature by the norm:
        for (final Entry<Integer, Float> entry : entrySet) {
            entry.setValue((float)(entry.getValue()/norm));
        }
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        final Set<Entry<Integer, Float>> entrySet = featureMap.entrySet();
        for (final Entry<Integer, Float> entry : entrySet) {
            if (entry.getValue() != 0) {   // feature value is not zero:
                sb.append((entry.getKey() + 1)); // make sure feature id is greater than 0
                sb.append(':');
                sb.append(entry.getValue());
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
