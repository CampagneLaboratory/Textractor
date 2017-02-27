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
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.TextractorRuntimeException;
import textractor.mg4j.io.TextractorWordReader;

import java.io.InputStream;
import java.io.StringReader;

/**
 * A factory that can produce MG4J documents from Textractor sentences.
 * <p/>
 * A factory that provides multiple fields containing just the raw input stream;
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
public final class ConfigurableTextractorDocumentFactory extends AbstractTextractorDocumentFactory {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 1L;

    /** The basename. */
    private final String basename;

    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(ConfigurableTextractorDocumentFactory.class);

    /**
     * Case-insensitive keys for metadata passed to
     * {@link it.unimi.dsi.mg4j.document.DocumentFactory#getDocument(
     * java.io.InputStream,it.unimi.dsi.fastutil.objects.Reference2ObjectMap)}.
     *
     * <p>The keys in this class are general-purpose keys that are meaningful
     * for most factories. Specific factory implementations might choose to
     * interpret more keys, but then it is up to the
     * {@link it.unimi.dsi.mg4j.document.DocumentSequence} that uses the
     * factory to provide data for those keys.
     */
    public static enum MetadataKeys {
        /**
         * Name of the property file used to configure the factory.
         */
        CONFIGURATION_FILE,
    }

    /**
     * Default value for field type supported by this factory.
     */
    public static final FieldType DEFAULT_FIELD_TYPE = FieldType.TEXT;

    /**
     * Parse any properties specific to this class.
     * Declare the properties the various wordReader implementations need to store in MG4J Metadata.
     * this should be delegated to the word reader, but it is unclear how at this time.
     * @param key the property to parse
     * @param values the values for that property
     * @param metadata the detadata to parse the property into, if possible
     * @return true if this method was able to parse the property
     * @throws ConfigurationException error parsing property
     */
    @Override
    protected boolean parseProperty(final String key, final String[] values,
                                    final Reference2ObjectMap<Enum<?>, Object> metadata)
            throws ConfigurationException {

        if (sameKey(MetadataKeys.CONFIGURATION_FILE, key)) {
            metadata.put(MetadataKeys.CONFIGURATION_FILE,
                    ensureJustOne(key, values));
            return true;
        }

        return super.parseProperty(key, values, metadata);
    }

    /**
     * Construct a new {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     * @param basenameVal the basename
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
    public ConfigurableTextractorDocumentFactory(
            final String basenameVal,
            final Reference2ObjectMap<Enum<?>, Object> defaultMetadata)
            throws ConfigurationException, ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        super(defaultMetadata);
        this.basename = basenameVal;
        init();
    }

    /**
     * Construct a new {@link it.unimi.dsi.mg4j.document.DocumentFactory}.
     * @param basenameVal the basename
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
    public ConfigurableTextractorDocumentFactory(
            final String basenameVal, final Properties properties)
            throws ConfigurationException, ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        super(properties);
        this.basename = basenameVal;
        init();
    }

    /**
     * Initialize the factory based on the meta data configured by
     * #parseProperty being called by the
     * {@link it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory}.
     * @throws org.apache.commons.configuration.ConfigurationException if
     * there is a problem with the configuration of the factory.
     * @throws ClassNotFoundException if the specified {@link it.unimi.dsi.mg4j.io.WordReader}
     * cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     */
    private void init() throws ConfigurationException, ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        String configurationFile =
                (String) defaultMetadata.get(MetadataKeys.CONFIGURATION_FILE);

        // example configuration file format for 3 fields:
        //   fields: larry,curly,moe
        //   field.larry.wordreader: textractor.mg4j.io.ProteinWordSplitterReader
        //   field.larry.type: VIRTUAL
        //   field.curly.type: TEXT
        //   ...

        if (configurationFile == null) {
            // If we don't have a configuration file, we cannot do anything.
            return;
        }
        if (basename != null) {
            configurationFile = StringUtils.replace(configurationFile, "{BASENAME}", basename);
        }

        LOG.info("!! In ConfigurableTextractorDocumentFactor.init()");
        LOG.info("!! Configuring fields using " + configurationFile);

        fields.clear();
        final PropertiesConfiguration configuration =
                new PropertiesConfiguration(configurationFile);
        final String[] fieldList = configuration.getStringArray("fields");
        for (final String field : fieldList) {
            // name of the field
            LOG.info("Parsing field: " + field);
            final TextractorFieldInfo fieldInfo = new TextractorFieldInfo(field);

            // type of the field
            final String type = configuration.getString("field."
                    + field + ".type", DEFAULT_FIELD_TYPE.name());
            LOG.debug("  type: " + type);
            fieldInfo.type = FieldType.valueOf(type);

            // word reader for the field
            final String wordReaderClassname = configuration.getString(
                    "field." + field + ".wordreader",
                    DEFAULT_WORD_READER_CLASS.getName());
            LOG.debug("  word reader: " + wordReaderClassname);
            final Class wordReaderClass = Class.forName(wordReaderClassname);
            assert TextractorWordReader.class.isAssignableFrom(wordReaderClass) :
                    "invalid word reader class";
            fieldInfo.wordReader =
                    (TextractorWordReader) wordReaderClass.newInstance();

            // add the configuration file as a property
            fieldInfo.properties.addProperty(
                    MetadataKeys.CONFIGURATION_FILE, configurationFile);

            // all other properties for this field are added
            fieldInfo.properties.addAll(configuration.subset("field." + field));

            // ensure all metadata properties are set
            // anything not defined in the file will get set here
            saveMetadata(fieldInfo.properties);

            // initialize the word reader
            fieldInfo.wordReader.configure(fieldInfo.properties);

            fields.add(fieldInfo);
        }
    }

    /**
     * Transfer specific properties from defaultMetadata to properties.
     * @param properties Property object to store metadata to.
     */
    private void saveMetadata(final Properties properties) {
        if (!properties.containsKey(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS)) {
            properties.addProperty(
                    AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                    defaultMetadata.get(
                            AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS));
        }

        if (!properties.containsKey(
                AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH)) {
            properties.addProperty(
                    AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH,
                    resolve(
                        AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH,
                        defaultMetadata,
                        DEFAULT_MINIMUM_DASH_SPLIT_LENGTH));
        }

        if (!properties.containsKey(
                AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS)) {
            properties.addProperty(
                    AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS,
                    resolve(
                        AbstractTextractorDocumentFactory.MetadataKeys.OTHER_CHARACTER_DELIMITERS,
                        defaultMetadata,
                        DEFAULT_OTHER_CHARACTER_DELIMITERS));
        }

        if (!properties.containsKey(
                AbstractTextractorDocumentFactory.MetadataKeys.MAXIMUM_LENGTH_CONSERVE_CASE)) {
            properties.addProperty(
                    AbstractTextractorDocumentFactory.MetadataKeys.MAXIMUM_LENGTH_CONSERVE_CASE,
                    resolve(
                        AbstractTextractorDocumentFactory.MetadataKeys.MAXIMUM_LENGTH_CONSERVE_CASE,
                        defaultMetadata,
                        DEFAULT_MAXIMUM_LENGTH_CONSERVE_CASE));
        }
    }

    /**
    * Creates a copy of this factory.
    * @return a copy of this factory.
    */
    public ConfigurableTextractorDocumentFactory copy() {
        try {
            return new ConfigurableTextractorDocumentFactory(this.basename, this.defaultMetadata);
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
     * {@link it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory}) to various kind of objects.
     * @return the document obtained by parsing the given character sequence.
     */
    public Document getDocument(final InputStream rawContent,
            final Reference2ObjectMap<Enum<?>, Object> metadata) {

        final String encoding;
        if (metadata == null) {
            encoding = "UTF-8";
        } else {
            encoding = (String) resolve(PropertyBasedDocumentFactory.MetadataKeys.ENCODING,
                    metadata, "UTF-8");
        }
        final Properties sentenceProperties = new Properties();
        sentenceProperties.setDelimiterParsingDisabled(true);
        try {
            if (rawContent != null) {
                sentenceProperties.load(rawContent, encoding);
            }
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
             * Returns the content of the given field. This may not support FieldType.VIRTUAL ?
             * But it definately supports FieldType.TEXT and FieldType.INT.
             * @param field the field index.
             * @return the field content; the actual type depends on the field
             * type, as specified by the
             * {@link it.unimi.dsi.mg4j.document.DocumentFactory} that built
             * this document.
             */
            public Object content(final int field) {
                ensureFieldIndex(field);
                final TextractorFieldInfo fieldInfo = fields.get(field);
                if (fieldInfo.getType() == FieldType.INT) {
                    return sentenceProperties.getLong(fieldInfo.getName());
                } else {
                    return new StringReader(sentenceProperties.getString(fieldInfo.getName()));
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

}
