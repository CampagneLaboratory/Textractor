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
 * Generic Tuple (of three) class, for joining three objects together
 * or returning three objects at once.
 * @author Kevin C. Dorff (Aug 27, 2007)
 * @param <T1> the type of the first object
 * @param <T2> the type of the second object
 * @param <T3> the type of the third object
 */
public class Tuple3<T1, T2, T3> {
    private T1 a;
    private T2 b;
    private T3 c;

    public Tuple3(final T1 a, final T2 b, final T3 c) {
        this.a = a;
        this.b = b;
        this.c = c;
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

    public T3 getC() {
        return c;
    }

    public void setC(final T3 c) {
        this.c = c;
    }

    /*------------ Support ------------*/

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Tuple3 == false) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        final Tuple3 rhs = (Tuple3) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(a, rhs.a)
                .append(b, rhs.b)
                .append(c, rhs.c)
                .isEquals();
    }

    @Override
    public String toString() {
        return "{" +
                a.toString() + ":" +
                b.toString() + ":" +
                c.toString() +
                "}";
    }
}
