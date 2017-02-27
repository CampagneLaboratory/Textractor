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

package textractor.datamodel.naming;

/**
 * Allows binding of a persistent object to a name.
 */
public final class NamedObject {
    /**
     * Name of the object.
     */
    private String name;

    /**
     * The object itself.
     */
    private Object object;

    /**
     * Create a new NamedObject.
     */
    public NamedObject() {
        super();
    }

    /**
     * Get the name of the object.
     * @return The name of the object
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the object.
     * @param name The name of the object.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get the object.
     * @return The object.
     */
    public Object getObject() {
        return object;
    }

    /**
     * Set the object.
     * @param object The object
     */
    public void setObject(final Object object) {
        this.object = object;
    }
}
