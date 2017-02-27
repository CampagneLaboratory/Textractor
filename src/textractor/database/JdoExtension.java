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

import javax.jdo.Extent;
import javax.jdo.PersistenceManagerFactory;
import java.util.Properties;

/**
 * Common JDO functions that may have vendor specific requirements or
 * optimizations.
 */
public interface JdoExtension {
    /**
     * Used to perform any special initialization for the jdo implementation.
     *
     * @param factory factory for this jdo implementation
     * @param properties Any JDO property specifications that may be needed to
     *        initialize
     */
    void initDB(final PersistenceManagerFactory factory,
                final Properties properties);

    /**
     * Utility function to determine the number of items in an Extent.
     *
     * @param extent The extent to get the size of
     * @return the number of items in the extent
     */
    int size(final Extent extent);
}
