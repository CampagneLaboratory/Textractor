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
import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.TextractorRuntimeException;
import textractor.mg4j.io.TextractorWordReader;

import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

/**
 * A factory that can produce MG4J documents from Textractor sentences.
 * This ONLY supports using the "text" field. If you need more, please use the
 * ConfigurableTextractorDocumentFactory.
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
public final class TextractorDocumentFactory extends AbstractTextractorDocumentFactory {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 4L;

    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(TextractorDocumentFactory.class);

    /**
     * Construct a new {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     * @throws ConfigurationException if there is a problem with the
     * configuration of the factory.
     * @throws ClassNotFoundException if the specified {@link WordReader}
     * cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link WordReader}.
     */
    public TextractorDocumentFactory() throws ConfigurationException,
            ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        super();
        init();
    }

    /**
     * Construct a new {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     * @param defaultMetadata meta data used to configure this factory
     * @throws ConfigurationException if there is a problem with the
     * configuration of the factory.
     * @throws ClassNotFoundException if the specified {@link WordReader}
     * cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link WordReader}.
     */
    public TextractorDocumentFactory(final Reference2ObjectMap<Enum<?>, Object>
            defaultMetadata) throws ConfigurationException, ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        super(defaultMetadata);
        init();
    }

    /**
     * Construct a new {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     * @param properties properties used to configure this factory
     * @throws ConfigurationException if there is a problem with the
     * configuration of the factory.
     * @throws ClassNotFoundException if the specified {@link WordReader}
     * cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link WordReader}.
     */
    public TextractorDocumentFactory(final Properties properties)
            throws ConfigurationException, ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        super(properties);
        init();
    }

    /**
     * Construct a new {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     * @param property properties used to configure this factory
     * @throws ConfigurationException if there is a problem with the
     * configuration of the factory.
     * @throws ClassNotFoundException if the specified {@link WordReader}
     * cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link WordReader}.
     */
    public TextractorDocumentFactory(final String[] property)
            throws ConfigurationException, ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        super(property);
        init();
    }

    /**
     * Initialize the factory based on the meta data configured by
     * #parseProperty being called by the
     * {@link it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory}.
     * @throws ConfigurationException if there is a problem with the
     * configuration of the factory.
     * @throws ClassNotFoundException if the specified {@link WordReader}
     * cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link WordReader}.
     */
    private void init() throws
            IllegalAccessException, InstantiationException {
        LOG.info("Initializing factory with default metadata");
        final TextractorFieldInfo fieldInfo = new TextractorFieldInfo("text");
        fieldInfo.type = FieldType.TEXT;
        final Class wordReaderClass =
                (Class) resolve(PropertyBasedDocumentFactory.MetadataKeys.WORDREADER,
                        defaultMetadata, DEFAULT_WORD_READER_CLASS);
        fieldInfo.wordReader =
                (TextractorWordReader) wordReaderClass.newInstance();
        saveMetadata(fieldInfo.properties);

        // initialize the word reader
        fieldInfo.wordReader.configure(fieldInfo.properties);
        fields.add(fieldInfo);
    }

    /**
     * Save the metadata into the properties.
     * @param properties Property object to store metadata to.
     */
    private void saveMetadata(final Properties properties) {
        if (!properties.containsKey(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS)) {
            properties.addProperty(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                    defaultMetadata.get(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS));
        }

        if (!properties.containsKey(AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH)) {
            properties.addProperty(AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH,
                    resolve(AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH,
                            defaultMetadata,
                            DEFAULT_MINIMUM_DASH_SPLIT_LENGTH));
        }

        if (!properties.containsKey(AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS)) {
            properties.addProperty(AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS,
                    resolve(AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS,
                            defaultMetadata,
                            DEFAULT_OTHER_CHARACTER_DELIMITERS));
        }

        if (!properties.containsKey(AbstractTextractorDocumentFactory.MetadataKeys.MAXIMUM_LENGTH_CONSERVE_CASE)) {
            properties.addProperty(AbstractTextractorDocumentFactory.MetadataKeys.MAXIMUM_LENGTH_CONSERVE_CASE,
                    resolve(AbstractTextractorDocumentFactory.MetadataKeys.MAXIMUM_LENGTH_CONSERVE_CASE,
                            defaultMetadata,
                            DEFAULT_MAXIMUM_LENGTH_CONSERVE_CASE));
        }
    }

    /**
     * Creates a copy of this factory.
     * @return a copy of this factory.
     */
    public TextractorDocumentFactory copy() {
        try {
            return new TextractorDocumentFactory(this.defaultMetadata);
        } catch (ConfigurationException e) {
            throw new TextractorRuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new TextractorRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new TextractorRuntimeException(e);
        } catch (InstantiationException e) {
            throw new TextractorRuntimeException(e);
        }
    }


    /**
     * Returns the document obtained by parsing the given byte stream.
     *
     * @param rawContent the raw content from which the document should be
     * extracted; it must not be closed, as resource management is a
     * responsibility of the
     * {@link it.unimi.dsi.mg4j.document.DocumentCollection}.
     * @param metadata a map from enums (e.g., keys taken in
     * {@link PropertyBasedDocumentFactory}) to various kind of objects.
     * @return the document obtained by parsing the given character sequence.
     */
    public Document getDocument(final InputStream rawContent,
                                final Reference2ObjectMap<Enum<?>, Object> metadata) {
        final String encoding = (String) resolveNotNull(
                PropertyBasedDocumentFactory.MetadataKeys.ENCODING, metadata);
        final Properties sentenceProperties = new Properties();
        sentenceProperties.setDelimiterParsingDisabled(true);
        try {
            sentenceProperties.load(rawContent, encoding);
        } catch (ConfigurationException e) {
            throw new TextractorRuntimeException(e);
        }

        return new Document() {
            /**
             * The title of this document.
             *
             * @return the title to be used to refer to this document.
             */
            public CharSequence title() {
                return (CharSequence) resolve(PropertyBasedDocumentFactory.MetadataKeys.TITLE, metadata);
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
                return (CharSequence) resolve(PropertyBasedDocumentFactory.MetadataKeys.URI, metadata);
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
                ensureFieldIndex(field);
                if (field == 0) {
                    return new StringReader(sentenceProperties.getString("text"));
                } else {
                    throw new IndexOutOfBoundsException(Integer.toString(field));
                }
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

    public static PropertyBasedDocumentFactory getInstance(
            final Class<DocumentFactory> klass, final String basename, final Properties properties )
            throws InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        System.out.println("Trying to create a " + klass.getName());
        return (AbstractTextractorDocumentFactory) (klass.getConstructor(
                String.class, Properties.class).newInstance(basename, properties));
    }
}
