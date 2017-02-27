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

package textractor.mg4j.document;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.mg4j.document.Document;
import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.document.DocumentIterator;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.mg4j.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import textractor.TextractorRuntimeException;
import textractor.util.SentenceFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An iterator over textractor documents.
 */
public abstract class AbstractTextractorDocumentIterator implements DocumentIterator {
    private final DocumentFactory documentFactory;
    protected int filteredSentenceCount;
    protected final SentenceFilter sentenceFilter;

    public AbstractTextractorDocumentIterator(final DocumentFactory factory,
            final SentenceFilter filter) {
        super();
        this.documentFactory = factory;
        this.sentenceFilter = filter;
    }

    protected final Document createDocument(final Reference2ObjectMap<Enum<?>, Object> metadata,
            final String text) throws IOException {
        final Properties sentenceProperties = new Properties();
        sentenceProperties.addProperty("text", text);
        sentenceProperties.addProperty("pubdate", 0L);  // Legacy documents don't have pubdate

        final String encoding =
                (String) metadata.get(PropertyBasedDocumentFactory.MetadataKeys.ENCODING);

        final ByteArrayOutputStream memoryOutputStream =
                new ByteArrayOutputStream();
        try {
            sentenceProperties.save(memoryOutputStream, encoding);
        } catch (ConfigurationException e) {
            throw new TextractorRuntimeException(e);
        }
        final InputStream stream =
                new FastBufferedInputStream(
                        new ByteArrayInputStream(
                                memoryOutputStream.toByteArray()));
        return documentFactory.getDocument(stream, metadata);
    }
}
