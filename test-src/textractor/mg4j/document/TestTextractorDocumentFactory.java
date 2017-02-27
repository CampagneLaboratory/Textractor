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
import it.unimi.dsi.mg4j.io.NullInputStream;
import it.unimi.dsi.mg4j.util.Properties;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.math.IntRange;
import textractor.mg4j.io.ProteinWordSplitterReader;

import java.io.File;
import java.io.IOException;

/**
 * Validates the {@link textractor.mg4j.document.TextractorDocumentFactory}
 * functions.
 */
public final class TestTextractorDocumentFactory extends TestCase {
    /**
     * The name of the default factory field.
     * @see it.unimi.dsi.mg4j.document.DocumentFactory.FieldType@TEXT
     */
    private static final String DEFAULT_FIELD_NAME =
            DocumentFactory.FieldType.TEXT.name().toLowerCase();

    /**
     * Validates the default intitialization values of the factory.
     *
     * @throws ConfigurationException if there is a problem with the
     * configuration of the factory.
     * @throws ClassNotFoundException if the specified
     * {@link it.unimi.dsi.mg4j.io.WordReader} cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     */
    public void testDefaults() throws IllegalAccessException,
            ConfigurationException, InstantiationException,
            ClassNotFoundException {
        final TextractorDocumentFactory factory =
                new TextractorDocumentFactory();
        validateSingleFieldProperties(factory);

        final  Properties properties = new Properties();
        properties.addProperty(
                PropertyBasedDocumentFactory.MetadataKeys.ENCODING,
                "ISO-8859-13");
        final Reference2ObjectMap<Enum<?>, Object> metadata =
                factory.parseProperties(properties);

        final Document document =
                factory.getDocument(NullInputStream.getInstance(), metadata);
        assertEquals(TextractorDocumentFactory.DEFAULT_WORD_READER_CLASS,
                document.wordReader(0).getClass());
    }

    /**
     * Validates a single field intitialization of the factory.
     *
     * @throws ConfigurationException if there is a problem with the
     * configuration of the factory.
     * @throws ClassNotFoundException if the specified
     * {@link it.unimi.dsi.mg4j.io.WordReader} cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     */
    public void testSingleField() throws IllegalAccessException,
            ConfigurationException, InstantiationException,
            ClassNotFoundException {
        final Properties properties = new Properties();
        properties.addProperty(
                AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                "true");
        properties.addProperty(
                PropertyBasedDocumentFactory.MetadataKeys.WORDREADER,
                ProteinWordSplitterReader.class.getName());
        properties.addProperty(
                PropertyBasedDocumentFactory.MetadataKeys.ENCODING,
                "ISO-8859-13");

        final TextractorDocumentFactory factory =
                new TextractorDocumentFactory(properties);
        validateSingleFieldProperties(factory);

        final Reference2ObjectMap<Enum<?>, Object> metadata =
                factory.parseProperties(properties);
        final Document document =
                factory.getDocument(NullInputStream.getInstance(), metadata);
        assertEquals(ProteinWordSplitterReader.class,
                document.wordReader(0).getClass());
    }

    /**
     * Validates properties for a single field factory.
     * @param factory The factory to validate
     */
    private void validateSingleFieldProperties(final TextractorDocumentFactory factory) {
        assertEquals("Default factory should have one field",
                1, factory.numberOfFields());
        assertEquals("Default factory field type should be text",
                DocumentFactory.FieldType.TEXT, factory.fieldType(0));
        assertEquals("Default factory field name should be text",
                DEFAULT_FIELD_NAME, factory.fieldName(0));
        assertEquals("Default text index should be zero",
                0, factory.fieldIndex(DEFAULT_FIELD_NAME));
        assertEquals("Default factory should return invalid field",
                -1, factory.fieldIndex("foobar"));
    }

    /**
     * Validates a multi field intitialization of the factory.
     *
     * @throws ConfigurationException if there is a problem with the
     * configuration of the factory.
     * @throws ClassNotFoundException if the specified
     * {@link it.unimi.dsi.mg4j.io.WordReader} cannot be found.
     * @throws IllegalAccessException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     * @throws InstantiationException if the factory is unable to create an
     * instance of the specified {@link it.unimi.dsi.mg4j.io.WordReader}.
     * @throws IOException if a temporary file cannot be created for
     * the test.
     */
    public void testMultipleFields() throws IllegalAccessException,
            ConfigurationException, InstantiationException,
            ClassNotFoundException, IOException {
        final File tempFile = File.createTempFile("junit", ".properties");
        final Properties properties = new Properties(tempFile);

        // a three stooge factory
        properties.addProperty("fields", "larry,curly,moe");
        properties.addProperty("field.curly.wordreader",
                ProteinWordSplitterReader.class.getName());

        // shemp has been left out, but should get ignored
        properties.addProperty("field.shemp.wordreader", "stooge.throws.Pies");
        properties.addProperty("field.larry.type",
                DocumentFactory.FieldType.VIRTUAL);
        properties.save();

        // initialize the factory with temporary configuration
        final Properties factoryProperties = new Properties();
        factoryProperties.addProperty(
                ConfigurableTextractorDocumentFactory.MetadataKeys.CONFIGURATION_FILE,
                tempFile.getAbsolutePath());
        final DocumentFactory factory =
                new ConfigurableTextractorDocumentFactory(null, factoryProperties);

        assertEquals("Factory should have three fields",
                3, factory.numberOfFields());
        assertEquals("Factory should not have field named text",
                -1, factory.fieldIndex(DEFAULT_FIELD_NAME));
        assertEquals("Factory should return invalid field",
                -1, factory.fieldIndex("shemp"));

        final IntRange range = new IntRange(0, factory.numberOfFields());
        final int larry = factory.fieldIndex("larry");
        final int curly = factory.fieldIndex("curly");
        final int moe = factory.fieldIndex("moe");
        assertTrue("larry = " + larry, range.containsInteger(larry));
        assertTrue("curly = " + curly, range.containsInteger(curly));
        assertTrue("moe = " + moe, range.containsInteger(moe));

        final Document document = factory.getDocument(null, null);

        // moe and curly are text types, larry is virtual
        assertEquals(DocumentFactory.FieldType.VIRTUAL,
                factory.fieldType(larry));
        assertEquals(DocumentFactory.FieldType.TEXT,
                factory.fieldType(curly));
        assertEquals(DocumentFactory.FieldType.TEXT,
                factory.fieldType(moe));
        // larry and moe have a default word reader, but curly is different
        assertEquals(TextractorDocumentFactory.DEFAULT_WORD_READER_CLASS,
                document.wordReader(larry).getClass());
        assertEquals(TextractorDocumentFactory.DEFAULT_WORD_READER_CLASS,
                document.wordReader(moe).getClass());
        assertEquals(ProteinWordSplitterReader.class,
                document.wordReader(curly).getClass());

    }
}
