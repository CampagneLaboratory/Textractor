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

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: campagne
 * Date: May 11, 2003
 * Time: 9:08:29 PM
 * To change this template use Options | File Templates.
 */
public final class ReaderMaker {
    private File file;
    private String content;

    public ReaderMaker(final File file) {
        this.file = file;
    }

    public ReaderMaker(final String content) {
        this.content = content;
    }

    public BufferedReader getReader() throws FileNotFoundException {
        if (file != null) {
	    return new BufferedReader(new FileReader(file));
	} else if (content != null) {
	    return new BufferedReader(new StringReader(content));
	}
        return null;
    }
}
