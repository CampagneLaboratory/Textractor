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

package textractor.caseInsensitive;

import java.util.ArrayList;

/**
 * Interface to read information in a CaseInsensitiveStore.   With an interface, we can define a null implementation.
 *@author Fabien Campagne
 * Date: Oct 13, 2006
 * Time: 2:44:14 PM

 */
public interface CaseInsensitiveStoreReader {
     /**
     * Suggest a list of terms to add to make the
     * search case insensitive. If the term requested
     * has any upper-case in it, am empty list will be returned.
     * If the term doesn't match the criteria where
     * it SHOULD be case-sensitive an empty list will
     * be returned.
     *
     * @param term the term to suggest a list for
     * @return the list of suggested words to make
     *         the term case insensitive.
     */
    ArrayList<String> suggest(String term);
}
