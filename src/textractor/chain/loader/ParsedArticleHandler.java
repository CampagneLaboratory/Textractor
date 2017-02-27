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

package textractor.chain.loader;

import it.unimi.dsi.mg4j.util.MutableString;
import textractor.datamodel.Article;

/**
 * This interface enables the article parser class the be a separate class
 * from the loader class (normally it is a sub-class). This iterface
 * allows the parser to call back into the loader class.
 * @author Kevin Dorff (Sep 17, 2007)
 */
public interface ParsedArticleHandler {
    void articleParsed(final Article article, final MutableString abstractText);
}
