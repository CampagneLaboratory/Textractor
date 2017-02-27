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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.Attribute;
import it.unimi.dsi.mg4j.util.parser.Element;
import org.apache.commons.lang.ArrayUtils;

import java.util.Map;
import java.util.Set;

/**
 * Represents the current element being parsed, including the element
 * the attributes, and the text of the element.
 * @author Kevin Dorff (Sep 19, 2007)
 */
public class XmlElement {
    public final Element element;
    public final Map<CharSequence,MutableString> attributesMap;
    public final MutableString elementText;
    public XmlElement(final Element element, final Map<Attribute, MutableString> attributesMap) {
        this.element = element;
        this.attributesMap = copyMap(attributesMap);
        this.elementText = new MutableString();
    }

    private Map<CharSequence,MutableString> copyMap(final Map<Attribute, MutableString> map) {
        final Set<Attribute> keys = map.keySet();
        final Map<CharSequence,MutableString> newMap =
                new Object2ObjectOpenHashMap<CharSequence,
                        MutableString>(keys.size());
        for (final Attribute key : keys) {
            newMap.put(key.name, map.get(key).copy());
        }
        return newMap;
    }

    @Override
    public String toString() {
        return String.format("%s attributes=%s text=%s",
                element.toString(),
                ArrayUtils.toString(attributesMap),
                elementText);
    }
}
