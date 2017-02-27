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

package textractor.didyoumean;

/**
 * Describes a Did You Mean field.
 */

public final class DidYouMeanFieldDescriptor {
    private String name;
    private int factoryID;
    private double weight;

    public DidYouMeanFieldDescriptor(final String fieldName,
            final int fieldFactoryID, final double fieldWeight) {
	super();
        name = fieldName;
        factoryID = fieldFactoryID;
        weight = fieldWeight;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getFactoryID() {
        return factoryID;
    }

    public void setFactoryID(final int factoryID) {
        this.factoryID = factoryID;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(final double weight) {
        this.weight = weight;
    }
}