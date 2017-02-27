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

package textractor.database;

/**
 * Contains constants related to database items for Textractor systemwide use.
 */
public final class TextractorDatabaseConstants {
    /**
     * Private constructor so this class cannot be instantiated.
     */
    private TextractorDatabaseConstants() {
    }

    /**
     * Name of parameter that contains the jdo implmentation/vendor to use.
     */
    public static final String JDO_IMPLEMENTATION =
            "textractor.JdoImplementation";

    /**
     * Name of parameter that contains the jdo extension class to use.
     */
    public static final String JDO_EXTENSION_CLASS =
            "textractor.JdoExtensionClass";

    /**
     * Default Jdo extension class to use if none was specified.
     */
    public static final String DEFAULT_JDO_EXTENSION_CLASS =
            textractor.database.PureJdoExtension.class.getName();

    /**
     * Name of Parameter which contains the FastObjects License.
     */
    public static final String FASTOBJECTS_LICENSE = "com.fastobjects.license";
}
