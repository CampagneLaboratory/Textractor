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

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.mg4j.document.Document;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import textractor.mg4j.io.TweaseWordReader2;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * A factory that can produce MG4J documents from Textractor sentences.
 * <p/>
 * A factory that provides a single field containing just the raw input stream;
 * the encoding is set using the property
 * {@link it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory.MetadataKeys#ENCODING}.
 * The field is named <samp>text</samp>, but you can change the name using the
 * property <samp>fieldname</samp>.
 * <p/>
 * <p/>
 * By default, the {@link it.unimi.dsi.mg4j.io.WordReader} provided by this
 * factory is just a {@link it.unimi.dsi.mg4j.io.FastBufferedReader}, but you
 * can specify an alternative word reader using the property
 * {@link it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory.MetadataKeys#WORDREADER}.
 * For instance, if you need to index a list of identifiers to retrieve
 * documents from the collection more easily, you can use a
 * {@link it.unimi.dsi.mg4j.io.LineWordReader} to index each line of a file as a
 * whole.
 * <p/>
 * A default encoding can be provided using the property
 * {@link it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory.MetadataKeys#ENCODING}.
 */
public abstract class AbstractTextractorDocumentFactory extends PropertyBasedDocumentFactory {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Case-insensitive keys for metadata passed to
     * {@link it.unimi.dsi.mg4j.document.DocumentFactory#getDocument(java.io.InputStream,
     * it.unimi.dsi.fastutil.objects.Reference2ObjectMap)}.
     *
     * <p>The keys in this class are general-purpose keys that are meaningful
     * for most factories. Specific factory implementations might choose to
     * interpret more keys, but then it is up to the
     * {@link it.unimi.dsi.mg4j.document.DocumentSequence} that uses the
     * factory to provide data for those keys.
     */
    public static enum MetadataKeys {
        /**
         * Indicates that parenthesis characters are treated as words.
         */
        PARENTHESESAREWORDS,
        /**
         * Minimum index at which a word can be split.
         */
        MINIMUM_DASH_SPLIT_LENGTH,
        /**
         * Other characters that the reader should consider as standalone words.
         * When these characters are encountered, the reader stops the current
         * word, if any, and makes the character a word by itself.
         * For instance, if OTHER_CHARACTER_DELIMITERS=";!", parsing
         * "a; !" will result in the words "a", ";", "!" being returned,
         * and the nonWords: "", " ", "".
         */
        OTHER_CHARACTER_DELIMITERS,
        /**
         * The maximum number of characters in a term that guarantee that the
         * term is not downcased.
         */
        MAXIMUM_LENGTH_CONSERVE_CASE,
    }

    /**
     * Default value for the word reader class supported by this factory.
     */
    public static final Class DEFAULT_WORD_READER_CLASS =
            TweaseWordReader2.class;

    /**
     * The maximum number of characters in a term that guarantee that the
     * term is not downcased.
     */
    public static final int DEFAULT_MAXIMUM_LENGTH_CONSERVE_CASE = 4;

    /**
     * Default character delimiters to match Twease behaviour.
     */
    public static final String DEFAULT_OTHER_CHARACTER_DELIMITERS =
            "\\,;/$.:!?|";

    /**
     * Default miniumm index at which a word can be split.
     */
    public static final int DEFAULT_MINIMUM_DASH_SPLIT_LENGTH = 8;

    /**
     * Fields and configuration associated with this
     * {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     */
    protected final List<TextractorFieldInfo> fields = new ArrayList<TextractorFieldInfo>();

    /**
     * Declare the properties the various wordReader implementations need to store in
     * MG4J Metadata. This should be delegated to the word reader, but it is unclear
     * how at this time.
     * @param key the property key
     * @param values the values property values
     * @param metadata the properties metadata
     * @throws ConfigurationException error configuring
     */
    @Override
    protected boolean parseProperty(
            final String key, final String[] values,
            final Reference2ObjectMap<Enum<?>, Object> metadata) throws ConfigurationException {
        if (sameKey(MetadataKeys.PARENTHESESAREWORDS, key)) {
            metadata.put(MetadataKeys.PARENTHESESAREWORDS,
                    Boolean.valueOf(ensureJustOne(key, values)));
            return true;
        }

        if (sameKey(MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH, key)) {
            metadata.put(MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH,
                    Integer.valueOf(ensureJustOne(key, values)));
            return true;
        }

        if (sameKey(MetadataKeys.OTHER_CHARACTER_DELIMITERS, key)) {
            metadata.put(MetadataKeys.OTHER_CHARACTER_DELIMITERS,
                    ensureJustOne(key, values));
            return true;
        }

        if (sameKey(MetadataKeys.MAXIMUM_LENGTH_CONSERVE_CASE, key)) {
            metadata.put(MetadataKeys.MAXIMUM_LENGTH_CONSERVE_CASE,
                    Integer.valueOf(ensureJustOne(key, values)));
            return true;
        }

        if (sameKey(PropertyBasedDocumentFactory.MetadataKeys.WORDREADER, key)) {
            try {
                metadata.put(PropertyBasedDocumentFactory.MetadataKeys.WORDREADER,
                        Class.forName(ensureJustOne(key, values)));
            } catch (final ClassNotFoundException e) {
                throw new ConfigurationException(e);
            }
            return true;
        }

        // encoding applies to the entire document
        if (sameKey(PropertyBasedDocumentFactory.MetadataKeys.ENCODING, key)) {
            metadata.put(PropertyBasedDocumentFactory.MetadataKeys.ENCODING,
                    Charset.forName(ensureJustOne(key, values)).toString());
            return true;
        }

        return super.parseProperty(key, values, metadata);
    }

    /**
     * Construct a new {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     * @throws org.apache.commons.configuration.ConfigurationException if there is
     * a problem with the configuration of the factory.
     * @throws ClassNotFoundException if the specified {@link it.unimi.dsi.mg4j.io.WordReader}
     * cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     */
    public AbstractTextractorDocumentFactory() throws ConfigurationException,
            ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        super();
    }

    /**
     * Construct a new {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     * @param defaultMetadata meta data used to configure this factory
     * @throws org.apache.commons.configuration.ConfigurationException if there
     * is a problem with the configuration of the factory.
     * @throws ClassNotFoundException if the specified {@link it.unimi.dsi.mg4j.io.WordReader}
     * cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     */
    public AbstractTextractorDocumentFactory(
            final Reference2ObjectMap<Enum<?>, Object> defaultMetadata)
            throws ConfigurationException, ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        super(defaultMetadata);
    }

    /**
     * Construct a new {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     * @param properties properties used to configure this factory
     * @throws org.apache.commons.configuration.ConfigurationException if
     * there is a problem with the configuration of the factory.
     * @throws ClassNotFoundException if the specified {@link it.unimi.dsi.mg4j.io.WordReader}
     * cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     */
    public AbstractTextractorDocumentFactory(final Properties properties)
            throws ConfigurationException, ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        super(properties);
    }

    /**
     * Construct a new {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     * @param property properties used to configure this factory
     * @throws org.apache.commons.configuration.ConfigurationException if
     * there is a problem with the configuration of the factory.
     * @throws ClassNotFoundException if the specified {@link it.unimi.dsi.mg4j.io.WordReader}
     * cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     */
    public AbstractTextractorDocumentFactory(final String[] property)
            throws ConfigurationException {
        super(property);
    }

    /**
     * Returns the number of fields present in the documents produced by this
     * factory.
     *
     * @return the number of fields present in the documents produced by
     * this factory.
     */
    public final int numberOfFields() {
        return fields.size();
    }

    /**
     * Return the list of fields for thsi index.
     *
     * @return The List[TextractorFieldInfo] for this index.
     */
    public final List<TextractorFieldInfo> getFieldInfoList() {
        return fields;
    }

    /**
     * Returns the symbolic name of a field.
     *
     * @param field the index of a field (between 0 inclusive and
     * {@link #numberOfFields()} exclusive).
     * @return the symbolic name of the <code>field</code>-th field.
     */
    public final String fieldName(final int field) {
        ensureFieldIndex(field);
        return fields.get(field).name;
    }

    /**
     * Returns the index of a field, given its symbolic name.
     *
     * @param fieldName the name of a field of this factory.
     * @return the corresponding index, or -1 if there is no field with name
     * <code>fieldName</code>.
     */
    public final int fieldIndex(final String fieldName) {
        for (int i = 0; i < fields.size(); i++) {
            final TextractorFieldInfo fieldInfo = fields.get(i);
            if (fieldInfo.name.equals(fieldName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the type of a field.
     * <p/>
     * <p>The possible types are defined in
     * {@link FieldType}.
     *
     * @param field the index of a field (between 0 inclusive and
     * {@link #numberOfFields()} exclusive}).
     * @return the type of the <code>field</code>-th field.
     */
    public final FieldType fieldType(final int field) {
        ensureFieldIndex(field);
        return fields.get(field).type;
    }

    /**
     * Returns the document obtained by parsing the given byte stream.
     *
     * {@link it.unimi.dsi.mg4j.document.DocumentCollection}.
     * @param metadata a map from enums (e.g., keys taken in
     * {@link it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory}) to various kind of objects.
     *
     * @return the document obtained by parsing the given character sequence.
     */
    public final Document getDocumentNoContent(
            final Reference2ObjectMap<Enum<?>, Object> metadata) {
        return new Document() {
            /**
             * The title of this document.
             *
             * @return the title to be used to refer to this document.
             */
            public CharSequence title() {
                return (CharSequence) resolve(
                        PropertyBasedDocumentFactory.MetadataKeys.TITLE, metadata);
            }

            /**
             * Returns a string representation of the object.
             *
             * @return a string representation of the object.
             */
            @Override
            public String toString() {
                return title().toString();
            }

            /**
             * A URI that is associated to this document.
             *
             * @return the URI associated to this document,
             * or <code>null</code>.
             */
            public CharSequence uri() {
                return (CharSequence) resolve(
                        PropertyBasedDocumentFactory.MetadataKeys.URI, metadata);
            }

            /**
             * Returns the content of the given field.
             *
             * @param field the field index.
             * @return the field content; the actual type depends on the field
             * type, as specified by the
             * {@link it.unimi.dsi.mg4j.document.DocumentFactory} that built
             * this document.
             */
            public Object content(final int field) {
                return null;
            }

            /**
             * Returns a word reader for the given field.
             *
             * @param field the field index.
             * @return a word reader object that should be used to break the
             * given field.
             */
            public WordReader wordReader(final int field) {
                ensureFieldIndex(field);
                return fields.get(field).wordReader;
            }

            /**
             * Closes this document, releasing all resources.
             */
            public void close() {
            }
        };
    }
}
