/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

package textractor.mg4j.document;

import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.util.Properties;
import textractor.mg4j.io.TextractorWordReader;

import java.io.Serializable;

/**
 * The details, including the name, wordreader, type, etc. of a specific field in the index.
 * Many indexes only have a single field named "text" but some will have several of these.
 *
 * @author Fabien Campagne
 * @author Kevin Dorff
 */
/**
 * Holds information about the fields supported by this factory.
 */
public final class TextractorFieldInfo implements Serializable {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the field.
     */
    protected final String name;

    /**
     * The type of the field.
     */
    protected DocumentFactory.FieldType type;

    /**
     * The word reader used for the field.
     */
    protected TextractorWordReader wordReader;

    /**
     * Additional properties for this field.
     */
    protected final Properties properties = new Properties();

    /**
     * Construct a new FieldInfo object for this factory.
     * @param fieldName name of the field
     */
    public TextractorFieldInfo(final String fieldName) {
        super();
        assert fieldName != null : "Field Name cannot be null";
        this.name = fieldName;
    }

    /**
     * Create a new one, fully populated.
     * @param fieldNameVal the field name
     * @param typeVal the tpe
     * @param wordReaderVal the wordReader
     */
    public TextractorFieldInfo(
            final String fieldNameVal, final DocumentFactory.FieldType typeVal,
            final TextractorWordReader wordReaderVal) {
        this(fieldNameVal);
        this.type = typeVal;
        this.wordReader = wordReaderVal;
    }

    public TextractorWordReader getWordReader() {
        return wordReader;
    }


    public DocumentFactory.FieldType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return String.format("name=%s:type=%s:properties=%s", name, type, properties.toString());
    }
}
