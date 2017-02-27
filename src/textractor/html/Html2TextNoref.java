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

package textractor.html;

import it.unimi.dsi.mg4j.util.MutableString;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Lei Shi
 * Date: Jul 26, 2005
 * Time: 6:09:55 PM
 * To change this template use File | Settings | File Templates.
 */
public final class Html2TextNoref extends Html2Text {

    @Override
    protected void modifyHtml(final MutableString inString) throws IOException {
        super.modifyHtml(inString);

        int ref = inString.indexOf("<!-- Bibliography");
        if (ref == -1) {
            ref = inString.indexOf("<h2>References</h2><p>");
        }
        if (ref == -1) {
            ref = inString.indexOf("<A NAME=\"References\">");
        }
        //jbc1995
        if (ref == -1) {
            ref = inString.indexOf("<a name = \"ref\">");
        }
        //embo, pnas
        if (ref == -1) {
            ref = inString.indexOf("<A NAME=\"BIBL\">");
        }
        //biochemistry
        if (ref == -1) {
            ref = inString.indexOf("<BBGR>");
        }
        if (ref > 0) {
            inString.replace(ref, inString.length(), "</BODY> </HTML>");
        }
    }
}
