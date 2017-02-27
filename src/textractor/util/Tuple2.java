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

package textractor.util;

import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * Generic Tuple (of two) class, for joining two objects together
 * or returning two objects at once.
 * @author Kevin C. Dorff (Aug 27, 2007)
 * @param <T1> the type of the first object
 * @param <T2> the type of the second object
 */
public class Tuple2<T1, T2> {
    private T1 a;
    private T2 b;

    public Tuple2(final T1 a, final T2 b) {
        this.a = a;
        this.b = b;
    }

    /*------------ Getters for tuple parts ------------*/

    public T1 getA() {
        return a;
    }

    public void setA(final T1 a) {
        this.a = a;
    }

    public T2 getB() {
        return b;
    }

    public void setB(final T2 b) {
        this.b = b;
    }

    /*------------ Support ------------*/

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Tuple2 == false) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        final Tuple2 rhs = (Tuple2) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(a, rhs.a)
                .append(b, rhs.b)
                .isEquals();
    }

    @Override
    public String toString() {
        return "{" +
                a.toString() + ":" +
                b.toString() +
                "}";
    }

}
