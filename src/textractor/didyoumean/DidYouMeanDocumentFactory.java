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

package textractor.didyoumean;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.mg4j.document.Document;
import it.unimi.dsi.mg4j.document.DocumentFactory;
import static it.unimi.dsi.mg4j.document.DocumentFactory.FieldType.TEXT;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;

import java.io.InputStream;

/**
 * User: campagne
 * Date: Nov 8, 2005
 * Time: 3:11:43 PM
 */
public final class DidYouMeanDocumentFactory extends PropertyBasedDocumentFactory {
    // TODO: change these to enums
    public static final int WORD = 0;
    public static final int GRAM3 = 1;
    public static final int GRAM4 = 2;
    public static final int THREE_START = 3;
    public static final int THREE_END = 4;
    public static final int FOUR_START = 5;
    public static final int FOUR_END = 6;
    public static final int GRAM2 = 7;

    private static final String[] NAMES = {
	  "word", "gram3", "gram4", "3start", "3end", "4start", "4end", "gram2"
    };


    /**
     * Returns the number of fields present in the documents produced by this
     * factory.
     *
     * @return the number of fields present in the documents produced by this
     * factory.
     */
    public int numberOfFields() {
        return NAMES.length;
    }

    /**
     * Returns the symbolic name of a field.
     *
     * @param field the index of a field (between 0 inclusive and
     * {@link #numberOfFields()} exclusive}).
     * @return the symbolic name of the <code>field</code>-th field.
     */
    public String fieldName(final int field) {
        ensureFieldIndex(field);
        return NAMES[field];
    }

    /**
     * Returns the index of a field, given its symbolic name.
     *
     * @param fieldName the name of a field of this factory.
     * @return the corresponding index, or -1 if there is no field with name
     * <code>fieldName</code>.
     */
    public int fieldIndex(final String fieldName) {
        for (int i = 0; i < NAMES.length; ++i) {
            if (NAMES[i].equals(fieldName)) {
                return i;
            }
        }

        throw new UnsupportedOperationException("field name: " +
        	fieldName + " not supported");
    }


    public FieldType fieldType(final int field) {
        ensureFieldIndex(field);
        return TEXT;   // all fields are text.
    }

    public DocumentFactory copy() {
        return new DidYouMeanDocumentFactory();
    }


    public Document getDocument(final InputStream rawContent,
                                final Reference2ObjectMap<Enum<?>, Object> metadata) {
        return null;
    }
}
