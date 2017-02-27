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

package textractor.mg4j.io;

import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.util.Properties;

/**
 * @author campagne Date: Dec 14, 2005 Time: 4:48:02 PM
 */
public interface TextractorWordReader extends WordReader {
    /**
     * Configure the word reader according to the textractor index properties.
     * This should restore the word reader to a state exactly equivalent to when
     * the index was built.
     *
     * @param properties Propeties to use when configuring the reader
     */
    void configure(final Properties properties);

    /**
     * Save the parameter of this word reader to the specified properties object.
     *
     * @param properties
     */
    void saveProperties(Properties properties);

    /**
     * Let the word reader obtain its parameters from the command line.
     * @param args
     */
    void configureFromCommandLine(String[] args);
    String INDEX_PARENTHESES_ARGUMENT = "-indexParentheses";
}
