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

package textractor.tools.expansion;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public final class ExpansionHandler extends DefaultHandler {
    private final Map<String,List<ExpansionTerm>> expansionMap;
    private String elementContents;
    private String currentShortForm;
    private String currentLongFormFrequency;

    public ExpansionHandler() {
        super();
        expansionMap = new HashMap<String, List<ExpansionTerm>>();
    }

    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("expansion")) {
            elementContents = "";
            currentLongFormFrequency = attributes.getValue("frequency");
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length)
        throws SAXException {
        elementContents = new String(ch, start, length);
    }

    @Override
    public void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
        if (qName.equalsIgnoreCase("short-form")) {
            expansionMap.put(elementContents, new ArrayList<ExpansionTerm>());
            currentShortForm = elementContents;
        }

        if (qName.equalsIgnoreCase("expansion") &&
            !elementContents.contains("\n")) {
                expansionMap.get(currentShortForm).add(new ExpansionTerm(elementContents, Integer.valueOf(currentLongFormFrequency).longValue()));
        }

    }

    public Map<String, List<ExpansionTerm>> getExpansionMap() {
        return expansionMap;
    }
}
