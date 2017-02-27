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

package textractor.datamodel;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory.MetadataKeys;

import java.io.Serializable;

/**
 * A super class for each type of Textractor Document.
 */
public abstract class TextractorDocument implements Serializable {
    // Default values for document sections
    // TODO: Make an Enum out of these
    public static final int ABSTRACT_SECTION = 0;
    public static final int OTHER_SECTION = 1;
    public static final int REFERENCE_SECTION = 2;

    /**
     * By design, this number exactly match the document number in the MG4J
     * full text index.
     */
    protected long documentNumber;

    /**
     * A tag representing the location of the document from the parent document.
     * (eg: the location of a/ sentence within an abstract)
     */
    private int documentSection;

    public final int getDocumentSection() {
        return documentSection;
    }

    /**
     * Set the document Section.
     * @param section Section of this document.
     */
    public final void setDocumentSection(final int section) {
        this.documentSection = section;
    }


    /**
     * Get the document number. By design, this number exactly match the
     * document number in the MG4J full text index.
     * @return The document number.
     */
    public final long getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Set the document number.
     * @param number Number of this document.
     */
    public final void setDocumentNumber(final long number) {
        this.documentNumber = number;
    }

    /**
     * Get the text contained in this document.
     * @return A string of text
     */
    public abstract String getText();

    /**
     * Get the metadata for this document.
     * @return map containing metadata
     */
    public Reference2ObjectMap<Enum<?>, Object> getMetaData() {
        final Reference2ObjectMap<Enum<?>, Object> metadata =
                new Reference2ObjectOpenHashMap<Enum<?>, Object>();
        metadata.put(MetadataKeys.ENCODING, "UTF-8");
        metadata.put(MetadataKeys.TITLE, Long.toString(documentNumber));
        return metadata;
    }
}
