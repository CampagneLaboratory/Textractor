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

package textractor.database;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.mg4j.index.BitStreamIndex;
import it.unimi.dsi.mg4j.index.Index;
import it.unimi.dsi.mg4j.index.IndexIterator;
import it.unimi.dsi.mg4j.index.IndexReader;
import it.unimi.dsi.mg4j.index.TermMap;
import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.query.parser.QueryParser;
import it.unimi.dsi.mg4j.search.AndDocumentIterator;
import it.unimi.dsi.mg4j.search.ConsecutiveDocumentIterator;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.IntervalIterator;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.Properties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.datamodel.Sentence;
import textractor.mg4j.TermFrequency;
import textractor.mg4j.document.AbstractTextractorDocumentFactory;
import textractor.mg4j.document.TextractorDocumentFactory;
import textractor.mg4j.document.TextractorFieldInfo;
import textractor.mg4j.index.PositionedTerm;
import textractor.mg4j.index.TermIterator;
import textractor.mg4j.io.ProteinWordSplitterReader;
import textractor.mg4j.io.TextractorWordReader;
import textractor.tools.DocumentQueryResult;

import java.io.CharArrayReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The manager provides access to the inverted index files created by MG4J for a
 * specific document collection.
 *
 * This version of the class supports multiple fields in the index. The normal field
 * has an indexAlias of "text". Others that might exist include "authors".
 *
 * One specifies the fields to inclde in the TextractorDocumentFactory or similar class.
 * <p/>
 * @author Fabien Campagne
 * @author Kevin Dorff
 */
public final class DocumentIndexManager implements Closeable {
    /**
     * This special term index indicates that the term does not exist in the
     * full text index.
     */
    public static final int NO_SUCH_TERM = -1;

    /**
     * Default term suffix used with a basename if not provided.
     */
    public static final String DEFAULT_TERM_SUFFIX = "text";

    /**
     * Default property filename used with a basename if not provided.
     */
    public static final String DEFAULT_PROPERTY_FILE_SUFFIX =
            "textractor.properties";

    /**
     * Used to LOG informational and debug messages.
     */
    private static final Log LOG =
            LogFactory.getLog(DocumentIndexManager.class);

    /**
     * Basename used for mg4j index and property files.
     */
    private String basename;

    /**
     * Word Reader used for documents.
     */
    private final Map<String, IndexDetails> textAliasesToIndexMap =
            new Object2ObjectOpenHashMap<String, IndexDetails>();

    private final Map<String, IndexDetails> allAliasesToIndexMap =
            new Object2ObjectOpenHashMap<String, IndexDetails>();

    /** Temporary variable used with splitText. */
    private final MutableString resultExtractTerms = new MutableString();

    /** Temporary variable used with splitText. */
    private final MutableString wordExtractTerms = new MutableString();

    /** Temporary variable used with splitText/wordReader. */
    private final MutableString wordSplitText = new MutableString();

    /** Temporary variable used with splitText/wordReader. */
    private final MutableString nonWordSplitText = new MutableString();

    /**
     * The textractor properties.
     */
    private Properties textractorProperties;

    /** The basename modifiers. */
    private String modifiers;

    /**
     * The map of indexAlias to WordReaders.
     */
    private final Map<String, TextractorWordReader> wordReadersMap =
            new HashMap<String, TextractorWordReader>();

    /** The (option) index fields that were determined by the TextractorDocumentFactory. */
    private List<TextractorFieldInfo> indexFields;

    /** The DocumentFactory being used with this DocumentIndexManager. */
    private AbstractTextractorDocumentFactory factory;

    /**
     * Initializes the DocumentIndexManager using the basename with a default
     * term suffix and property file suffix.
     * Use THIS version if you want to attempt to open a all of the TEXT indexes in the
     * List[TextractorFieldInfo].
     *
     * @param base String of character (exluding spaces and characters that
     *             cannot be part of a filename) that uniquely identify a collection
     *             of document indexed with MG4J.
     * only try to open the "text" index.
     * @throws ConfigurationException if there is an error setting up properties
     * using the file specified by base and the property file suffix
     * @throws IOException if there was a problem setting up the index or word reader
     */
    public DocumentIndexManager(final String base)
            throws ConfigurationException {

        final String basenameNoModifiers = removeModifiers(base);
        final String modifiersFromBasename = getModifiers(base);

        assert basenameNoModifiers != null;
        assert modifiersFromBasename != null;

        this.basename = basenameNoModifiers + "-" + DEFAULT_TERM_SUFFIX;
        this.modifiers = modifiersFromBasename;
        final String textractorPropertiesFilename =
                getPropertiesFilenameFromBasename(basenameNoModifiers);
        this.textractorProperties =  new Properties(textractorPropertiesFilename);

        final String documentFactoryClassname =
                textractorProperties.getString("documentFactoryClass");
        try {
            this.factory = createDocumentFactory(documentFactoryClassname, basenameNoModifiers);
        } catch (ClassNotFoundException e) {
            LOG.error("Error creating documentFactoryClass " + documentFactoryClassname, e);
        } catch (IllegalAccessException e) {
            LOG.error("Error creating documentFactoryClass " + documentFactoryClassname, e);
        } catch (InstantiationException e) {
            LOG.error("Error creating documentFactoryClass " + documentFactoryClassname, e);
        } catch (InvocationTargetException e) {
            LOG.error("Error creating documentFactoryClass " + documentFactoryClassname, e);
        } catch (NoSuchMethodException e) {
            LOG.error("Error creating documentFactoryClass " + documentFactoryClassname, e);
        }

        this.indexFields = this.factory.getFieldInfoList();
        System.out.println(ArrayUtils.toString(indexFields));
        ensureIndexFields();

        LOG.debug("Using basename: " + basename);

        try {
            initialize();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * Given the factoryClassName and the basename, create the DocumentFactory for this
     * DocumentIndexManager.
     * @param factoryClassNameVal the classname to create
     * @param factoryBasename the basename of the index
     * @return the DocumentFactory
     * @throws InvocationTargetException error reating the DocumentFactory object
     * @throws IllegalAccessException error reating the DocumentFactory object
     * @throws InstantiationException error reating the DocumentFactory object
     * @throws NoSuchMethodException error reating the DocumentFactory object
     * @throws ClassNotFoundException error reating the DocumentFactory object
     */
    private AbstractTextractorDocumentFactory createDocumentFactory(
            final String factoryClassNameVal, final String factoryBasename)
            throws InvocationTargetException, IllegalAccessException, InstantiationException,
            NoSuchMethodException, ClassNotFoundException {
       final String factoryClassName;
        if (StringUtils.isBlank(factoryClassNameVal)) {
            factoryClassName = TextractorDocumentFactory.class.getName();
        } else {
            factoryClassName = factoryClassNameVal;
        }

        final Class<DocumentFactory> factoryClass =
                (Class<DocumentFactory>) Class.forName(factoryClassName);

        try {
            // First try to create a document factory using basename, properties
            return (AbstractTextractorDocumentFactory) TextractorDocumentFactory.getInstance(
                    factoryClass, factoryBasename, textractorProperties);
        } catch (NoSuchMethodException e) {
            // If the factoryClass doesn't support that, make one with just properties
            return (AbstractTextractorDocumentFactory) TextractorDocumentFactory.getInstance(
                    factoryClass, textractorProperties);
        }
    }

    /**
     * Make sure we have indexFields defined. If it isn't defined, make a single "text"
     * indexFields entry.
     * @throws ConfigurationException error making the "text" indexField.
     */
    private void ensureIndexFields() throws ConfigurationException {
        if (indexFields == null) {
            indexFields = new ArrayList<TextractorFieldInfo>(1);
        }
        if (indexFields.size() > 0) {
            // Already initialized
            return;
        }

        final String wordReaderClassname = textractorProperties.getString(
                PropertyBasedDocumentFactory.MetadataKeys.WORDREADER, null);
        final Class wordReaderClass;
        if (wordReaderClassname == null) {
            wordReaderClass = AbstractTextractorDocumentFactory.DEFAULT_WORD_READER_CLASS;
        } else {
            try {
                wordReaderClass = Class.forName(wordReaderClassname);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException(e);
            }
        }
        final TextractorWordReader wordReader;
        try {
            wordReader = (TextractorWordReader) wordReaderClass.newInstance();
        } catch (InstantiationException e) {
            throw new ConfigurationException(e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(e);
        } catch (ClassCastException e) {
            throw new ConfigurationException(e);
        }
        wordReader.configure(textractorProperties);
        final TextractorFieldInfo textField = new TextractorFieldInfo(
                "text", DocumentFactory.FieldType.TEXT, wordReader);
        indexFields.add(textField);
    }

    /**
     * Retrieve the indexFields being used with this index.
     * @return the indexFields
     */
    public List<TextractorFieldInfo> getIndexFields() {
        return indexFields;
    }

    /**
     * Return the proeprties file for the given basename.
     * @param basename the index basename
     */
    public static String getPropertiesFilenameFromBasename(final String basename) {
        return removeModifiers(basename) + "-" + DEFAULT_PROPERTY_FILE_SUFFIX;
    }

    /**
     * Return the actual Proeprties file for the given basename.
     * @param basename the index basename
     */
    public static Properties getPropertiesForBasename(final String basename)
        throws ConfigurationException {
        return new Properties(getPropertiesFilenameFromBasename(basename));
    }
    /**
     * Get the modifiers on the given basename.
     * @param basename the basename that may include modifiers
     * @return just the modifiers
     */
    private static String getModifiers(final String basename) {
        final int modifiersIndex;
        // basenames can end with modifiers. Modifiers are introduced by the '?'
        // character, such as in index?inmemory=1 we extract modifiers to append
        // them at the end of the full basename:
        if ((modifiersIndex = basename.indexOf('?')) != -1) {
            return basename.substring(modifiersIndex);
        } else {
            return "";
        }

    }

    /**
     * Return the modifiers from the basenamem returning just the basename
     * @param basename the basename that may include modifiers
     * @return just the basename
     */
    private static String removeModifiers(final String basename) {
        final int modifiersIndex;
        // basenames can end with modifiers. Modifiers are introduced by the '?' character,
        // such as in index?inmemory=1 we extract modifiers to append them at the end
        // of the full basename:
        if ((modifiersIndex = basename.indexOf('?')) != -1) {
            return basename.substring(0, modifiersIndex);
        } else {
            return basename;
        }
    }

    /**
     * Intializes the index, term processor and word reader using the given
     * properties.
     *
     * @throws IOException if there was a problem setting up the index or
     *                     word reader
     * @throws IllegalAccessException xx
     * @throws ConfigurationException xx
     * @throws ClassNotFoundException xx
     * @throws InstantiationException xx
     * @throws URISyntaxException xx
     * @throws InvocationTargetException xx
     * @throws NoSuchMethodException xx
     */
    private void initialize() throws IOException, ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, URISyntaxException, ConfigurationException,
            InstantiationException, IllegalAccessException {

        if (indexFields == null || indexFields.size() == 0) {
            // The default : no fields specified, assume one field, "text".
            openAliasIndex("text", textractorProperties,
                    PropertyBasedDocumentFactory.MetadataKeys.WORDREADER);
        } else {
            // Alternatively, we have specified the fields. Open the text files with the
            // associated word readers
            for (final TextractorFieldInfo fieldInfo : indexFields) {
                System.out.printf("++ Opening text indices, looking at %s;%s%n",
                        fieldInfo.getName(), fieldInfo.getWordReader());
                if (fieldInfo.getType() == DocumentFactory.FieldType.TEXT) {
                    openAliasIndex(fieldInfo.getName(), fieldInfo.getWordReader());
                } else {
                    openAliasIndex(fieldInfo.getName(), null);
                }
            }
        }
    }

    /**
     * Attempt to open a specific index for the given alias.
     * @param alias the alias we are opening
     * @param properties the properties we are using to open the alias
     * @param wordReaderProperty the word reader property
     * @return the details of the index
     * @throws IOException error opening the index
     * @throws ClassNotFoundException error opening the index
     * @throws InvocationTargetException error opening the index
     * @throws NoSuchMethodException error opening the index
     * @throws URISyntaxException error opening the index
     * @throws ConfigurationException error opening the index
     * @throws InstantiationException error opening the index
     * @throws IllegalAccessException error opening the index
     */
    private IndexDetails openAliasIndex(
            final String alias, final Properties properties, final Enum wordReaderProperty)
            throws IOException, ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, URISyntaxException, ConfigurationException,
            InstantiationException, IllegalAccessException {

        // set up the word reader
        final String wordReaderClass = properties.getString(wordReaderProperty);
        final TextractorWordReader wordReaderObj;
        if (wordReaderClass == null) {
            wordReaderObj = new ProteinWordSplitterReader();
        } else {
            wordReaderObj = (TextractorWordReader) Class.forName(wordReaderClass).newInstance();
        }

        wordReaderObj.configure(properties);
        return openAliasIndex(alias, wordReaderObj);
    }

    /**
     * Attempt to open a specific index for the given alias.
     * @param alias the alias we are opening
     * @return the details of the index
     * @throws IOException error opening the index
     * @throws ClassNotFoundException error opening the index
     * @throws InvocationTargetException error opening the index
     * @throws NoSuchMethodException error opening the index
     * @throws URISyntaxException error opening the index
     * @throws ConfigurationException error opening the index
     * @throws InstantiationException error opening the index
     * @throws IllegalAccessException error opening the index
     */
    private IndexDetails openAliasIndex(final String alias, final TextractorWordReader wordReader)
            throws IOException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, URISyntaxException, ConfigurationException,
            IllegalAccessException, InstantiationException {

        final String aliasIndexBasename = IndexDetails.basenameFromAlias(basename, alias);
        final File aliasIndexFile = new File(aliasIndexBasename + ".index");

        if (aliasIndexFile.exists()) {
            final BitStreamIndex aliasIndex = (BitStreamIndex) Index.getInstance(
                    (aliasIndexBasename + modifiers),
                        /* random-access */ true,
                        /* load-sizes */ true,
                        /* term maps */ true
                        );
            final IndexDetails details = new IndexDetails(aliasIndexBasename, alias, aliasIndex);
            details.setWordReader(wordReader);
            if (wordReader != null) {
                textAliasesToIndexMap.put(alias, details);
                wordReadersMap.put(alias, wordReader);
            }
            allAliasesToIndexMap.put(alias, details);
            LOG.info("!! Loaded DocumentIndexManager with " + alias
                    + " index. mapsize=" + details.getTermMap().size()
                    + " wordreader=" + wordReader);
            return details;
        } else {
            LOG.info("!! No " + alias + " index file"
                    + aliasIndexFile.getCanonicalPath());
            return null;
        }
    }

    /**
     * Initialize this document index manager for text splitting only. When
     * initialized through this constructor, the index manager can only be used
     * to call the following methods:
     * <UL>
     * <LI>splitText</LI>
     * <LI>extractTerms</LI>
     * <LI>getTermProcessor</LI>
     * </UL>
     * Other methods will throw a variety of exceptions
     *
     * @param reader    The word reader implementation that should be used
     *                  to split text.
     * @param processor The term processor implementation that should be used
     */
    public DocumentIndexManager(final TextractorWordReader reader,
                                final TermProcessor processor) {
        final IndexDetails indexDetails = new IndexDetails("text");
        indexDetails.setWordReader(reader);
        indexDetails.setTermProcessor(processor);
        textAliasesToIndexMap.put("text", indexDetails);
        allAliasesToIndexMap.put("text", indexDetails);
    }

    /**
     * Get the text index.
     * @return Index text index
     */
    public Index getIndex() {
        return getIndex("text");
    }

    /**
     * Get the index for the specified index alias.
     * @param indexAlias the index alias to find the index for
     * @return Index the index for the specified index alias
     */
    public Index getIndex(final String indexAlias) {
        final IndexDetails indexDetails = getIndexDetails(indexAlias);
        if (indexDetails != null) {
            return indexDetails.getIndex();
        } else {
            return null;
        }
    }

    /**
     * Get the index details for the specified index alias.
     * @param indexAlias the index alias to find the index details for
     * @return IndexDetails the index details for the specified index alias
     */
    public IndexDetails getIndexDetails(final String indexAlias) {
        return allAliasesToIndexMap.get(indexAlias) ;
    }

    /**
     * Remove all index files for all loaded indicies.
     */
    public void removeIndexFiles() {
        // From files from all loaded indicies
        for (final String indexAlias : allAliasesToIndexMap.keySet()) {
            removeIndexFiles(indexAlias);
        }
    }

    /**
     * Get the set of index alias names for text indices.
     * @return the set of index alias names
     */
    public Map<String, IndexDetails> getTextAliasesToIndexMap() {
        return textAliasesToIndexMap;
    }

    /**
     * Get the set of index alias names for all indices.
     * @return the set of index alias names
     */
    public Map<String, IndexDetails> getAllAliasesToIndexMap() {
        return allAliasesToIndexMap;
    }

    /**
     * Removes the files that contain the index data for the specified index alias.
     * @param indexAlias the index alias to remove files for
     */
    public void removeIndexFiles(final String indexAlias) {
        for (final File file : getIndexFiles(indexAlias)) {
            final String absPath = file.getAbsolutePath();
            final boolean result = file.delete();
            if (result) {
                LOG.info("Deleting file: '" + absPath + "' successful.");
            } else {
                LOG.warn("Deleting file: '" + absPath + "' failed.");
            }
        }
    }

    /**
     * Remove all index files for all loaded indicies when the JVM exists.
     */
    public void removeIndexFilesOnExit() {
        for (final String indexAlias : allAliasesToIndexMap.keySet()) {
            for (final File file : getIndexFiles(indexAlias)) {
                file.deleteOnExit();
            }
        }
    }

    /**
     * Obtain a list of all files for the given index alias.
     * @param indexAlias the index alias to retrieve the likely set of files for
     * @return the list of files for the given index alias
     */
    private Collection<File> getIndexFiles(final String indexAlias) {
        assert basename != null;

        final String indexBasename = IndexDetails.basenameFromAlias(basename, indexAlias);
        final List<File> result = new ArrayList<File>();
        result.add(new File(indexBasename + ".properties"));
        result.add(new File(indexBasename + ".terms"));
        result.add(new File(indexBasename + ".frequencies"));
        result.add(new File(indexBasename + ".offsets"));
        result.add(new File(indexBasename + ".index"));
        result.add(new File(indexBasename + ".sizes"));
        result.add(new File(indexBasename + ".globcounts"));
        result.add(new File(indexBasename + ".mph"));
        final File directory = new File(indexBasename + ".terms");
        File basenameContainingDirectory = directory.getParentFile();
        if (basenameContainingDirectory == null) {
            basenameContainingDirectory = new File(".");
        }

        final String[] names =
                basenameContainingDirectory.list(new FilenameFilter() {
                    public boolean accept(final File dir, final String name) {
                        return name.startsWith(indexBasename + ".batch");
                    }
                });

        for (final String name : names) {
            result.add(new File(name));
        }

        return result;
    }

    /**
     * Returns the number of terms for the "text" index.
     * @return Number of terms in the index.
     */
    public int getNumberOfTerms() {
        return getNumberOfTerms("text");
    }

    /**
     * Returns the number of terms for the specified index alias.
     * @param indexAlias the index alias to get the number of terms for
     * @return Number of terms in the index.
     */
    public int getNumberOfTerms(final String indexAlias) {
        final Index currentIndex = getIndex(indexAlias);
        if (currentIndex != null) {
            return currentIndex.numberOfTerms;
        }
        return 0;
    }

    /**
     * Close all loaded indexes.
     */
    public void close() {
        for (final Map.Entry<String, IndexDetails> indexEntry : allAliasesToIndexMap.entrySet()) {
            final String indexAlias = indexEntry.getKey();
            final IndexDetails indexDetails = indexEntry.getValue();
            try {
                final IndexReader currentIndexReader = indexDetails.getIndexReader();
                if (currentIndexReader != null) {
                    currentIndexReader.close();
                }
            } catch (final IOException e) {
                LOG.warn("Failed to close index reader for index " + indexAlias
                        +  ". Error ignored.", e);
            }
        }
    }

    /**
     * Get the number of documents on the text index.
     * @return the number of documents on the text index
     */
    public int getDocumentNumber() {
        return getDocumentNumber("text");
    }

    /**
     * Get the number of documents for the index specified by indexAlias.
     * @param indexAlias the index alias to get the number of documents for
     * @return the number of documents for the index specified indexAlias
     */
    public int getDocumentNumber(final String indexAlias) {
        final Index currentIndex = getIndex(indexAlias);
        if (currentIndex != null) {
            return currentIndex.numberOfDocuments;
        }
        return 0;
    }

    /**
     * Extract the positioned terms for the given sentence. The terms will
     * be from the "text" index.
     * @param sentence the sentence to get the positions terms for
     * @return the list of positioned terms for the given sentence
     */
    public synchronized List<PositionedTerm> extractTerms(final Sentence sentence) {
        final IndexDetails indexDetails = getIndexDetails("text");
        final TermProcessor currentTermProcessor = indexDetails.getTermProcessor();

        final String sentenceText = sentence.getText();
        final String downcasedSentenceText = sentenceText.toLowerCase();
        final List<Integer> sentencePositions = sentence.getPositions();
        final int length = splitText(indexDetails.getWordReader(),
                sentenceText, resultExtractTerms);
        final List<PositionedTerm> termList = new ArrayList<PositionedTerm>(length);
        int pos;
        int lastPos = 0;
        int i = 0;

        while ((pos = resultExtractTerms.indexOf(' ', lastPos)) >= 0) {
            wordExtractTerms.length(0);
            wordExtractTerms.append(resultExtractTerms.subSequence(lastPos, pos));

            final int start = downcasedSentenceText.indexOf(
                    wordExtractTerms.toString().toLowerCase(), i);
            final int end = start + wordExtractTerms.length();
            final String text = wordExtractTerms.toString();

            currentTermProcessor.processTerm(wordExtractTerms);

            final int termIndex = findTermIndex(indexDetails, wordExtractTerms);
            final PositionedTerm positionedTerm;
            if (sentence.hasPositons()) {
                final int startPosition = sentencePositions.get(start);
                final int endPosition = sentencePositions.get(end - 1);
                final IntRange termRange =
                        new IntRange(startPosition, endPosition);
                positionedTerm = new PositionedTerm(termIndex, termRange, text);
            } else {
                positionedTerm = new PositionedTerm(termIndex, new IntRange(0));
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace("adding " + positionedTerm);
            }
            termList.add(positionedTerm);
            i = end;
            lastPos = pos + 1;
        }
        return termList;
    }

    /**
     * Extracts indexed terms from a document using the "text" index.
     *
     * @param documentContent The document to be converted.
     * @return An array of ints. Each int is the index of the term in the
     *         document index manager.
     */
    public synchronized int[] extractTerms(final CharSequence documentContent) {
        return extractTerms(getIndexDetails("text"), documentContent, null);
    }

    /**
     * Extract the int terms from a document using the given indexAlias index.
     * @param indexDetailsToUse the index to get the int terms from
     * @param documentContent the document to get the terms for
     * @return the int terms
     */
    public synchronized int[] extractTerms(
            final IndexDetails indexDetailsToUse, final CharSequence documentContent) {
        return extractTerms(indexDetailsToUse, documentContent, null);
    }

    /**
     * Extracts indexed terms from a document. Conversion is done with the same
     * method used by MG4J when indexing the documents, expect that an extra
     * word delimiter is considered (internalSeparator). Any words that have
     * this delimiter are split at the delimiter boundary and the parts are
     * resolved independently in the full text index. For instance, let's assume
     * "A B_C,D_E" is the document content, and space and comma are delimit
     * words in the index. If internalSeparator is '_', then the following words
     * will be converted: [ A B C D E]. In contrast, #this.extractTerm would
     * return [A TermNotFound TermNotFound], assuming terms B_C and D_E have not
     * been indexed, or [A B_C D_E] if terms B_C and D_E exist in the index.
     * <p/> This method returns an array of int. Each int of the array
     * represents a term that occurs in the document. The order of terms in the
     * document is preserved in the int array. The int[] is often more
     * convenient to use than a String, since parsing into terms has already
     * been done. Direct access into the array of terms can be useful.
     * The terms will be from the "text" index.
     * @param documentContent   The document to be converted.
     * @param internalSeparator A character used as an extra delimiter.
     * @return An array of ints. Each int is the index of the term in the
     *         document index manager.
     * @deprecated use the version that processes a MutableString
     *             documentContent instead.
     */
    @Deprecated
    public int[] extractTerms(final CharSequence documentContent,
                              final char internalSeparator) {
        return extractTerms(getIndexDetails("text"), documentContent, internalSeparator);
    }

    /**
     * Extracts indexed terms from a document. Conversion is done with the same
     * method used by MG4J when indexing the documents. This method returns an
     * array of int. Each int of the array represents a term that occurs in the
     * document. The order of terms in the document is preserved in the int
     * array. The int[] is often more convenient to use than a String, since
     * parsing into terms has already been done. Direct access into the array of
     * terms can be useful. The terms will be from the "text" index.
     *
     * @param documentContent The document to be converted.
     * @return An array of ints. Each int is the index of the term in the
     *         document index manager.
     */
    public int[] extractSpaceDelimitedTerms(final CharSequence documentContent) {
        return extractTerms(documentContent, '_');
    }

    /**
     * Convert the string in documentContent into index term int's within the index
     * associated with indexAlias.
     * @param indexDetailsToUse the index alias to find the term int's for
     * @param documentContent the string to obtain index term int's for
     * @param internalSeparator the OPTIONAL internal separator, it will split on
     * space (' ') and this value if internalSeparator is not null
     * @return the array of index term int's for the given string
     */
    public synchronized int[] extractTerms(
                final IndexDetails indexDetailsToUse, final CharSequence documentContent,
                final Character internalSeparator) {
        IndexDetails indexDetails = indexDetailsToUse;
        if (indexDetails == null) {
            // Default to text
            indexDetails = getIndexDetails("text");
        }
        final TermProcessor currentTermProcessor = indexDetails.getTermProcessor();
        final int length = splitText(indexDetails.getWordReader(),
                documentContent, resultExtractTerms);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Extracted %d terms from '%s' to '%s'",
                    length, documentContent, resultExtractTerms));
        }

        final int[] array = new int[length];
        int lastPos = 0;
        final char[] resChars = resultExtractTerms.toCharArray();

        final int resultCharLength = resChars.length;
        int tokenIndex = 0;

        for (int i = 0; i < resultCharLength; ++i) {
            if ((resChars[i] == ' ')
                        || (internalSeparator != null && resChars[i] == internalSeparator)) {
                wordExtractTerms.length(0);
                for (int j = lastPos; j < i; ++j) {
                    wordExtractTerms.append(resChars[j]);
                }

                currentTermProcessor.processTerm(wordExtractTerms);
                array[tokenIndex++] = findTermIndex(indexDetails, wordExtractTerms);
                lastPos = i + 1;
            }
        }
        return array;
    }

    /**
     * Gets iterator over the terms of the "text" index. Call close() when you
     * are done with the iterator.
     *
     * @return An iterator over the terms of this index.
     * @throws FileNotFoundException if the terms file cannot be loaded.
     */
    public TermIterator getTerms() throws FileNotFoundException {
        return getTerms("text");
    }

    /**
     * Gets iterator over the terms of the index related to indexAlias.
     * Call close() when you are done with the iterator.
     * @param indexAlias the index alias to obtain the terms for
     * @return An iterator over the terms of this index.
     * @throws FileNotFoundException if the terms file cannot be loaded.
     */
    public TermIterator getTerms(final String indexAlias) throws FileNotFoundException {
        final IndexDetails indexDetails = getIndexDetails(indexAlias);
        assert indexDetails != null && indexDetails.getBasename() != null;
        return new TermIterator(indexDetails.getBasename() + ".terms");
    }

    /**
     * Run an mg4j query using "and exact order". This will run on the "text" index.
     * @param currentTerms the terms to query
     * @return the query document iterator
     * @throws IOException error executing query
     */
    public DocumentIterator queryAndExactOrderMg4jNative(
            final String[] currentTerms) throws IOException {
        final IndexDetails indexDetails = getIndexDetails("text");
        final int[] intArray = new int[currentTerms.length];
        for (int i = 0; i < currentTerms.length; i++) {
            intArray[i] = findTermIndex(indexDetails, currentTerms[i]);
        }

        return queryAndExactOrderMg4jNativeWithIntArray(intArray);
    }

    /**
     * Run an mg4j query using "and exact order" specifying int terms instead of
     * strings with the query.  This will run on the "text" index.
     * @param currentIndices the terms int's to query
     * @return the query document iterator
     * @throws IOException error executing query
     */
    public DocumentIterator queryAndExactOrderMg4jNativeWithIntArray(
            final int[] currentIndices) throws IOException {
        final DocumentIterator[] docIterators =
                new DocumentIterator[currentIndices.length];
        for (int i = 0; i < currentIndices.length; i++) {
            // nb. must create a new reader for each term document iterator..
            final int term = currentIndices[i];
            if (term == NO_SUCH_TERM) {
                throw new InternalError("Query failed because the term "
                        + currentIndices[i]
                        + " was not found in the index.");
            }
            docIterators[i] = getIndex("text").getReader().documents(term);
        }

        return ConsecutiveDocumentIterator.getInstance(docIterators);
    }

    /**
     * Run an mg4j query using "and" specifying int terms instead of
     * strings with the query.  This will run on the "text" index.
     * @param terms the terms int's to query
     * @return the query document iterator
     * @throws IOException error executing query
     */
    public DocumentIterator queryAndMg4jNative(final int[] terms) throws IOException {
        final DocumentIterator[] docIterators = new DocumentIterator[terms.length];
        for (int i = 0; i < terms.length; i++) {
            // nb. must create a new reader for each term document
            // iterator..
            final int term = terms[i];
            docIterators[i] = getIndex("text").getReader().documents(term);
        }

        return AndDocumentIterator.getInstance(docIterators);
    }

    /**
     * Extend the result of a query exact order on the left, by one word.
     * This will run on the "text" index.
     * @param cachedResult Result of a previous query
     * @param leftWord     Returned documents will match leftWord |PreviousQuery|,
     *                     in this exact order.
     * @return DocumentIterator that exactly matches the combined query.
     * @throws IOException If an error occurred reading the full
     *                     text index.
     */
    public DocumentIterator extendOnLeft(final DocumentQueryResult cachedResult,
                                         final String leftWord) throws IOException {
        final IndexDetails indexDetails = getIndexDetails("text");
        final DocumentIterator[] docIterators = new DocumentIterator[2];
        docIterators[0] = getIndex("text").getReader().documents(
                findTermIndex(indexDetails, leftWord));
        docIterators[1] = cachedResult.getDocumentIterator();

        return ConsecutiveDocumentIterator.getInstance(docIterators);
    }

    /**
     * Extend the result of a query exact order on the right, by one word.
     * This will run on the "text" index.
     * @param cachedResult Result of a previous query
     * @param rightWord    Returned documents will match |PreviousQuery| rightWord,
     *                     in this exact order.
     * @return DocumentIterator that exactly matches the combined query.
     * @throws IOException If an error occurred reading the full
     *                     text index.
     */
    public DocumentIterator extendOnRight(final DocumentQueryResult cachedResult,
                                          final String rightWord) throws IOException {
        final IndexDetails indexDetails = getIndexDetails("text");
        final DocumentIterator[] docIterators = new DocumentIterator[2];
        docIterators[0] = cachedResult.getDocumentIterator();
        docIterators[1] = getIndex("text").getReader().documents(
                findTermIndex(indexDetails, rightWord));
        return ConsecutiveDocumentIterator.getInstance(docIterators);
    }

    /**
     * Run a query using "and exact order".
     * @param currentTerms the terms to query
     * @return the query document iterator
     * @throws IOException error executing query
     */
    public TermDocumentPositions queryAndExactOrder(
            final String[] currentTerms) throws IOException {
        final List<String> currentTermCollection = Arrays.asList(currentTerms);
        TermDocumentPositions termDocumentPositions =
                new TermDocumentPositions();

        // to see if there exists any document with appearance of all the terms,
        // before testing if they are in the right order.
        int[] documents = queryAnd(currentTermCollection);
        if (documents == null || documents.length == 0) {
            return null;
        } else {
            query(currentTerms[0], termDocumentPositions);
        }

        final TermDocumentPositions[] tpss =
                new TermDocumentPositions[currentTerms.length];
        for (int j = 1; j < currentTerms.length; j++) {
            tpss[j] = new TermDocumentPositions(currentTerms[j]);
            query(currentTerms[j], tpss[j]);
            termDocumentPositions =
                    exactOrder(termDocumentPositions, tpss[j], documents);
            documents = termDocumentPositions.getDocuments();
            if (documents.length == 0) {
                return null;      // return empty set immediately.
            }
        }

        return termDocumentPositions;
    }

    /**
     * Returns the documents that contain the intersection of the keywords.
     *
     * @param keywords Collection of Strings. Each string is a keyword.
     * @return Documents that contain the intersection of keywords. Empty result
     *         sets are returned as an empty int array.
     * @throws IOException xx
     */
    public int[] queryAnd(final Collection<String> keywords) throws IOException {
        if (keywords == null || keywords.size() == 0) {
            throw new IllegalArgumentException("Empty query is not supported.");
        }

        final int numTerms = keywords.size();
        final Iterator<String> keywordIterator = keywords.iterator();
        if (keywords.size() > 1) {
            final int[][] results = new int[numTerms][0];
            // do a query for each term:
            for (int i = 0; i < numTerms; i++) {
                final String keyword = keywordIterator.next();
                results[i] = query(keyword);

                if (results[i].length == 0) {
                    return new int[0]; // return empty set immediately.
                }

                if (i > 0) {
                    // do an intersection between results for term i and for
                    // term 0 store the result in place of results for term 0.
                    results[0] = intersection(results[0], results[i]);
                    if (results[0].length == 0) {
                        return new int[0];      // return empty set immediately.
                    }
                }
            }

            return results[0];
        } else {
            if (keywordIterator.hasNext()) {
                return query(keywordIterator.next());
            } else {
                throw new InternalError("Only single keyword queries are supported at this time.");
            }
        }
    }

    /**
     * Returns the intersection of the two sets of arrays. The two sets must be
     * sorted in an ascending order or the behaviour of this method is
     * undefined.
     *
     * @param array1 First array of integer.
     * @param array2 Second array of integer.
     * @return An array of integer that contains the intersection of array1 and
     *         array2. That is, the resulting array will contain an int value
     *         only iff the value if contained in array1 and array2.
     */
    public int[] intersection(int[] array1, int[] array2) {
        final int[] result = new int[Math.min(array1.length, array2.length)];
        final int[] tmp_array;
        if (array1.length > array2.length) {
            // switch them:
            tmp_array = array1;
            array1 = array2;
            array2 = tmp_array;
        }

        if (array1.length == 0) {
            return new int[0];
        }

        int k = 0;   // index in result array
        int i = 0;   // index in array1
        int j = 0;   // index in array2
        int array1_i;
        int array2_j;
        // 1 2 3 6 7
        // 2 4 6 8
        // > 2 6
        // this garantees that array1.length < array2.length
        do {
            array1_i = array1[i];
            array2_j = array2[j];
            if (array1_i == array2_j) {
                result[k++] = array1_i;
                ++i;
                ++j;
            } else if (array1_i < array2_j) {
                i++;
            } else if (array1_i > array2_j) {
                j++;
            }

            if (i == array1.length || j == array2.length
                    || k == result.length) {
                break;
            }
        } while (true);

        // copy result to an array of the correct size:
        final int[] resized = new int[k];
        for (i = 0; i < k; i++) {
            resized[i] = result[i];
        }

        return resized;
    }

    /**
     * Returns the documents that contain the keywords.
     *
     * @param keyword single keyword.
     * @return index of the documents that contain the keyword, or null if the
     *         keyword was not found in the index.
     * @throws IOException xx
     */
    public int[] query(final String keyword) throws IOException {
        return query(keyword, null);
    }

    /**
     * Returns the documents that contain the keywords.
     * This will run on the "text" index.
     * @param keyword   single keyword.
     * @param positions Where to store positions of the keyword in the document
     *                  if needed, int[0] otherwise.
     * @return index of the documents that contain the keyword, or null if the
     *         keyword was not found in the index.
     * @throws IOException xx
     */
    public int[] query(final String keyword, final TermDocumentPositions positions)
            throws IOException {
        final IndexDetails indexDetails = getIndexDetails("text");
        final int term = findTermIndex(indexDetails, keyword);
        if (term < 0) {
            // not found
            return new int[0];
        }
        final IndexIterator indexIterator = getIndex("text").getReader().documents(term);
        final int frequency = indexIterator.frequency();
        final int[] documents = new int[frequency];
        // number of occurences in a document cannot be more than max length
        // of all documents.
        final int[] occ = new int[getMaxDocumentSize()];
        if (positions != null) {
            positions.allocate(documents.length);
        }

        for (int i = 0; i < frequency; i++) {
            final int document = indexIterator.nextDocument();
            documents[i] = document;
            final int result = indexIterator.positions(occ);

            if (positions != null && result > 0) {
                // collect positions:
                positions.setPositions(i, documents[i], occ, result);
            }
            if (result < 0) {
                throw new InternalError("Number of positions cannot be more than the "
                        + "maximum length of all documents in this document index.");
            }
        }
        return documents;
    }

    /**
     * Install a new term map. The new term map will be used by this document
     * manager whenever terms need to be converted to term indices and back and
     * forth. The client is responsible for populating the term map with
     * terms/indices consistent with the inverted index that this manager
     * provides access to. This sets the termMap on the "text" index.
     *
     * @param map The new term map.
     */
    public void setTermMap(final TermMap map) {
        setTermMap("text", map);
    }

    /**
     * Install a new term map. The new term map will be used by this document
     * manager whenever terms need to be converted to term indices and back and
     * forth. The client is responsible for populating the term map with
     * terms/indices consistent with the inverted index that this manager
     * provides access to. This sets the termMap on the indexAlias index.
     * @param indexAlias the index to associate the term map with
     * @param map The new term map.
     */
    public void setTermMap(final String indexAlias, final TermMap map) {
        final IndexDetails indexDetails = getIndexDetails(indexAlias);
        if (indexDetails != null) {
            indexDetails.setTermMap(map);
        }
    }

    public int findTermIndex(final CharSequence term) {
        return findTermIndex(getIndexDetails("text"), term);
    }

    /**
     * Obtain the index of the term in the index.
     * Prefer the method that processes a MutableString for best performance.
     * @param indexDetails the index to find the term int within
     * @param term The term
     * @return Index of this term in the index.
     */
    public int findTermIndex(final IndexDetails indexDetails, final CharSequence term) {
        // TODO creating a new MutableString should not be necessary, but it seems that
        // TODO the Object2Int map used by HashTermMap fails to equal String and MutableString.
        if (term instanceof MutableString) {
            return indexDetails.getTermMap().getNumber(term);
        } else {
            return indexDetails.getTermMap().getNumber(new MutableString(term));
        }
    }

    /**
     * Convert a termIndex index into a character sequence using the "text" index.
     *
     * @param termIndex The termIndex
     * @return Term that corresponds to this index in the full text index.
     */
    public CharSequence termAsCharSequence(final int termIndex) {
        return termAsCharSequence(getIndexDetails("text"), termIndex);
    }

    /**
     * Convert a termIndex index into a character sequence using the index assocaited with
     * indexAlias.
     * @param indexDetails the index to use
     * @param termIndex The termIndex
     * @return Term that corresponds to this index in the full text index.
     */
    public CharSequence termAsCharSequence(final IndexDetails indexDetails, final int termIndex) {
        return indexDetails.getTermMap().getTerm(termIndex);
    }

    /**
     * Run the term processor associated with the "text" index on the given term.
     * @param rawTerm the raw term to process
     * @return the term processed term
     */
    public boolean processTerm(final MutableString rawTerm) {
        return processTerm(getIndexDetails("text"), rawTerm);
    }

    /**
     * Run the term processor associated with the indexDetails index on the given term.
     * @param indexDetails the index whose associated term processor we will use
     * @param rawTerm the raw term to process
     * @return the term processed term
     */
    public boolean processTerm(final IndexDetails indexDetails, final MutableString rawTerm) {
        return indexDetails.getTermProcessor().processTerm(rawTerm);
    }

    /**
     * Get the number of documents in the "text" index.
     * @return Get the number of documents in the "text" index
     */
    public int getMaxDocumentSize() {
        return getMaxDocumentSize("text");
    }

    /**
     * Get the number of documents in the index specified by indexAlias.
     * @param indexAlias the indexAlias to find the number of documents in
     * @return the number of documents in the index specified by indexAlias
     */
    public int getMaxDocumentSize(final String indexAlias) {
        final Index currentIndex = getIndex(indexAlias);
        if (currentIndex != null) {
            return currentIndex.numberOfDocuments;
        }
        return 0;
    }

    /**
     * Modify/update the positions for a particular document in tps2 if those
     * positions satisfy the exact order, but DO NOT remove the positions in
     * documents if those documents do not have any exact order hit. The
     * modified document numbers in tps2 are tracked by documents. Thus, tps2
     * have more information than needed.
     *
     * @param tps1      positions in documents of front term(s) intersection
     * @param tps2      positions of next term and may be modified
     * @param documents the documents that satisfy the exact order
     * @return Positions of terms in this document.
     */
    private TermDocumentPositions exactOrder(
            final TermDocumentPositions tps1, final TermDocumentPositions tps2,
            final int[] documents) {
        final int[] results = new int[documents.length];

        int countDocument = 0;
        for (final int document : documents) {
            final int[] pos1 = tps1.getPositionsByDocument(document);
            final int[] pos2 = tps2.getPositionsByDocument(document);
            final int[] tempPos2 = new int[pos2.length];
            int countPosition = 0;
            for (final int aPos1 : pos1) {
                for (final int aPos2 : pos2) {
                    //if (pos1[k1] + 1 < pos2[k2]) break;
                    if (aPos1 + 1 == aPos2) {
                        tempPos2[countPosition++] = aPos2;
                    }
                }
            }

            if (countPosition > 0) {
                final int[] newPos2 = new int[countPosition];
                System.arraycopy(tempPos2, 0, newPos2, 0, countPosition);
                results[countDocument++] = document;
                tps2.setPositionsByDocument(document,
                        newPos2, countPosition);
            }
        }

        final TermDocumentPositions tpsNew =
                new TermDocumentPositions(tps2.getTerm());
        tpsNew.allocate(countDocument);

        for (int j = 0; j < countDocument; j++) {
            tpsNew.setPositions(j, results[j],
                    tps2.getPositionsByDocument(results[j]));
        }

        return tpsNew;
    }

    /**
     * Returns the documents that contain the union of the keywords.
     *
     * @param keywords Collection of Strings. Each string is a keyword that the
     *                 document returned will contain.
     * @return Documents that contain the union of keywords.
     * @throws IOException xx
     */
    public int[] queryOr(final Collection<String> keywords) throws IOException {
        if (keywords.size() == 0) {
            throw new IllegalArgumentException("Empty query is not supported.");
        }

        final int numTerms = keywords.size();
        final Iterator<String> keywordIterator = keywords.iterator();
        if (keywords.size() > 1) {
            final int[][] results = new int[numTerms][0];
            // do a query for each term:
            for (int i = 0; i < numTerms; i++) {
                results[i] = query(keywordIterator.next());
                if (i > 0) {
                    // do the union between results for term i and for term 0
                    // store the result in place of results for term 0.
                    results[0] = union(results[0], results[i]);
                }
            }
            return results[0];
        } else {
            if (keywordIterator.hasNext()) {
                return query(keywordIterator.next());
            } else {
                throw new InternalError("Only single keyword queries are supported at this time.");
            }
        }
    }

    /**
     * Returns the intersection of the two sets of arrays. The two sets must be
     * sorted in an ascending order or the behaviour of this method is
     * undefined.
     *
     * @param array1 First array of integer.
     * @param array2 Second array of integer.
     * @return An array of integer that contains the intersection of array1 and
     *         array2. That is, the resulting array will contain an int value
     *         only iff the value if contained in array1 and array2.
     */
    public int[] union(final int[] array1, final int[] array2) {
        final int[] result = new int[array1.length + array2.length];

        int k = 0;   // index in result array
        int i = 0;   // index in array1
        int j = 0;   // index in array2
        int array1_i = 0;
        int array2_j = 0;
        // 1 2 3 6 7
        // 2 4 6 8
        // > 1 2 3 4 6 7 8
        boolean ignore_array1 = false;
        boolean ignore_array2 = false;

        do {
            if (!ignore_array1) {
                array1_i = array1[i];
            }
            if (!ignore_array2) {
                array2_j = array2[j];
            }

            if (ignore_array1) {
                result[k++] = array2_j;
                j++;
            } else if (ignore_array2) {
                result[k++] = array1_i;
                i++;
            } else if (array1_i == array2_j) {
                result[k++] = array1_i;
                ++i;
                ++j;
            } else if (array1_i < array2_j) {
                if (k == 0) {
                    result[k++] = array1_i;
                } else if (result[k - 1] != array1_i) {
                    result[k++] = array1_i;
                }
                i++;
            } else if (array1_i > array2_j && !ignore_array2) {
                if (k == 0) {
                    result[k++] = array2_j;
                } else if (result[k - 1] != array2_j) {
                    result[k++] = array2_j;
                }

                j++;
            }

            if (i >= array1.length) {
                ignore_array1 = true;
            }
            if (j >= array2.length) {
                ignore_array2 = true;
            }
            if (ignore_array1 && ignore_array2) {
                break;
            }
        } while (true);

        // copy result to an array of the correct size:
        final int[] resized = new int[k];
        for (i = 0; i < k; i++) {
            resized[i] = result[i];
        }

        return resized;
    }

    /**
     * Given the specified index int's, return the string given the "text" index.
     * @param indexedTerm the index term int's
     * @return a string for those terms
     */
    public String multipleWordTermAsString(final int[] indexedTerm) {
        return multipleWordTermAsString("text", indexedTerm);
    }

    /**
     * Given the specified index int's, return the string given the indexAlias index.
     * @param indexAlias the index alias to use to find the terms
     * @param indexedTerm the index term int's
     * @return a string for those terms
     */
    public String multipleWordTermAsString(final String indexAlias, final int[] indexedTerm) {
        final IndexDetails indexDetails = getIndexDetails(indexAlias);
        final StringBuffer termStringBuffer = new StringBuffer();
        for (final int anIndexedTerm : indexedTerm) {
            termStringBuffer.append(' ');
            termStringBuffer.append(termAsString(indexDetails, anIndexedTerm));
        }
        return termStringBuffer.toString().trim();
    }

    /**
     * Return the given term in the "text" index as a string.
     * @param term the index int term to find
     * @return the string for that term.
     */
    public String termAsString(final int term) {
        return termAsCharSequence(term).toString();
    }

    /**
     * Return the given term in the indexAlias index as a string.
     * @param indexDetails the index to use to find the terms
     * @param term the index int term to find
     * @return the string for that term.
     */
    public String termAsString(final IndexDetails indexDetails, final int term) {
        return termAsCharSequence(indexDetails, term).toString();
    }

    /**
     * Converts a document iterator into a document number int array.
     *
     * @param documentIterator Iterator over the documents.
     * @return Array of document numbers or null if there are no documents
     * @throws IOException xx
     */
    public int[] iteratorToInts(final DocumentIterator documentIterator)
            throws IOException {
        final IntArrayList list = new IntArrayList();

        while (documentIterator.hasNext()) {
            list.add(documentIterator.nextDocument());
        }

        if (list.size() == 0) {
            return null;
        } else {
            return list.elements();
        }
    }

    /**
     * Returns the text of this sentence in a format where words are delimited
     * by a single space character.
     * The algorithm used to split the sentence into terms is the same as used
     * by MG4J, allowing direct calculation
     * of the word positions with a StringTokenizer that would split on spaces.
     * As a side effect, this method calculates and stores the number of terms
     * in this sentence. That number is then available through getTermNumber().
     * This will use wordreader associated with the "text" index.
     * <p/>
     * This method does not process the terms: each term is returned with the
     * capitalization that it had in the input text.
     *
     * @param text   The input text to split into terms.
     * @param result A mutable string where the result will be stored.
     *               Result is the text delimited by single space character.
     * @return The number of terms that were processed.
     */
    public synchronized int splitText(
            final CharSequence text, final MutableString result) {
        return splitText(getIndexDetails("text").getWordReader(), text, result);
    }

    /**
     * Returns the text of this sentence in a format where words are delimited
     * by a single space character.
     * The algorithm used to split the sentence into terms is the same as used
     * by MG4J, allowing direct calculation
     * of the word positions with a StringTokenizer that would split on spaces.
     * As a side effect, this method calculates and stores the number of terms
     * in this sentence. That number is then available through getTermNumber().
     * This will use the specified wordreader.
     * <p/>
     * This method does not process the terms: each term is returned with the
     * capitalization that it had in the input text.
     *
     * @param currentWordReader the word reader to use to split the text
     * @param text   The input text to split into terms.
     * @param result A mutable string where the result will be stored.
     *               Result is the text delimited by single space character.
     * @return The number of terms that were processed.
     */
    public synchronized int splitText(
        final WordReader currentWordReader,
        final CharSequence text, final MutableString result) {
        int termCount = 0;
        wordSplitText.setLength(0);
        nonWordSplitText.setLength(0);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Splitting the text " + text);
        }
        if (text != null && result != null) {
            result.setLength(0);
            Reader characterReader = null;
            try {
                if (text instanceof MutableString) {
                    characterReader = new CharArrayReader(((MutableString )text).toCharArray());
                } else if (text instanceof String) {
                    characterReader = new StringReader((String) text);
                } else {
                    characterReader = new StringReader(text.toString());
                }
                currentWordReader.setReader(characterReader);
                while (currentWordReader.next(wordSplitText, nonWordSplitText)) {
                    if (wordSplitText.length() != 0) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("... [" + termCount + "]=" + wordSplitText);
                        }
                        ++termCount;
                        result.append(wordSplitText);
                        result.append(' ');
                    }
                }
            } catch (final IOException e) {
                LOG.error("Couldn't split text", e);
                throw new InternalError(e.getMessage());
            } finally {
                IOUtils.closeQuietly(characterReader);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("... termCount=" + termCount);
        }
        return termCount;
    }

    /**
     * Get the basename for the "text" index.
     * @return Returns the basename.
     */
    public String getBasename() {
        return basename;
    }

    /**
     * Get the term processor for the "text" index.
     * @return Returns the term processor.
     */
    public TermProcessor getTermProcessor() {
        return getTermProcessor("text");
    }

    /**
     * Get the term processor for the index specified by indexAlias.
     * @param indexAlias the index alias to find the term processor for
     * @return Returns the term processor.
     */
    public TermProcessor getTermProcessor(final String indexAlias) {
        final IndexDetails indexDetails = getIndexDetails(indexAlias);
        if (indexDetails != null) {
            return indexDetails.getTermProcessor();
        }
        return null;
    }

    /**
     * Get the word reader for the "text" index.
     * @return Returns the word reader.
     */
    public TextractorWordReader getWordReader() {
        return getWordReader("text");
    }

    /**
     * Return the word reader for the specified indexalias.
     * @param indexAlias the index alias to get the word reader for.
     * @return the word reader for the specified index or null of the
     * specified index does not exist.
     */
    public TextractorWordReader getWordReader(final String indexAlias) {
        final IndexDetails indexDetails = getIndexDetails(indexAlias);
        if (indexDetails != null) {
            return indexDetails.getWordReader();
        }
        return null;
    }

    /**
     * Obtain the map of alias to word readers.
     * @return Returns the word readers map.
     */
    public Map<String, TextractorWordReader> getWordReadersMap() {
        return wordReadersMap;
    }

    /**
     * Calculates the number of times a query matches a corpus.
     *
     * @param query     Successive terms whose frequencies will be counted.
     * @param frequency Where frequencies will be written.
     * @return Occurence frequency, for convenience.
     * @throws IOException error getting frequency
     */
    public int frequency(final String[] query, final TermFrequency frequency)
            throws IOException {
        final DocumentIterator documents = queryAndExactOrderMg4jNative(query);
        int occurenceFrequency = 0;
        int docFrequency = 0;

        while (documents.hasNext()) {
            documents.nextDocument();
            final IntervalIterator intervalIterator =
                    documents.intervalIterator();
            if (intervalIterator != null) {
                while (intervalIterator.hasNext()) {
                    intervalIterator.nextInterval();
                    occurenceFrequency++;
                }
            } else {
                occurenceFrequency++;
            }
            docFrequency++;
        }
        frequency.setDocumentFrequency(docFrequency);
        frequency.setOccurenceFrequency(occurenceFrequency);
        documents.dispose();
        return occurenceFrequency;
    }

    /**
     * Obtain frequency information for a single term in the index for the indexAlias named "text".
     *
     * @param term Term to get frequency for
     * @return The count of documents that contain this term in the index.
     * @throws IOException error getting frequency
     */
    public int frequency(final int term) throws IOException {
        return frequency("text", term);
    }

    /**
     * Obtain frequency information for a single term in the index for the specified indexAlias.
     *
     * @param indexAlias the index to get the frequency for the term
     * @param term Term to get frequency for
     * @return The count of documents that contain this term in the index.
     * @throws IOException error getting frequency
     */
    public int frequency(final String indexAlias, final int term) throws IOException {
        final IndexDetails indexDetails = getIndexDetails(indexAlias);
        if (indexDetails != null) {
            final IndexReader currentIndexReader = indexDetails.getIndexReader();
            if (currentIndexReader != null) {
                final IndexIterator indexIterator = currentIndexReader.documents(term);
                return indexIterator.frequency();
            }
        }
        return 0;
    }

    /**
     * Returns a human readable text for the document content within the "text" index.
     *
     * @param documentText the list of int terms to obtain the docuemnt text for
     * @return A string, where each coded word was replaced by its human
     *         readable term.
     */
    public MutableString toText(final int[] documentText) {
        return toText("text", documentText);
    }

    /**
     * Returns a human readable text for the document content within the specified
     * indexAlias index.
     * @param indexAlias the indexAlias to use when obtaining the text
     * @param documentText the list of int terms to obtain the docuemnt text for
     * @return A string, where each coded word was replaced by its human
     *         readable term.
     */
    public MutableString toText(final String indexAlias, final int[] documentText) {
        final IndexDetails indexDetails = getIndexDetails(indexAlias);
        final MutableString result = new MutableString();
        for (final int word : documentText) {
            result.append(termAsCharSequence(indexDetails, word));
            result.append(' ');
        }
        return result;
    }

    /**
     * Returns a query parser configured against this document index for the indexAlias "text".
     * The query parser parses queries in the MG4J query syntax, implement and execute
     * them.
     *
     * @return query parser configured against this document index for indexAlias "test"
     */
    public QueryParser getQueryParser() {
        return getQueryParser("text");
    }

    /**
     * Returns a query parser configured against this document index for the specified indexAlias.
     * The query parser parses queries in the MG4J query syntax, implement and execute
     * them.
     * @param indexAlias the indexAlias to obtain the query parser for
     * @return query parser configured against this document index for specified indexAlias
     */
    public QueryParser getQueryParser(final String indexAlias) {
        final IndexDetails indexDetails = getIndexDetails(indexAlias);
        if (indexDetails != null) {
            return indexDetails.getQueryParser();
        } else {
            return null;
        }
    }

    /**
     * Obtain the properties use to configure the document index manager.
     * @return the properties
     */
    public Properties getTextractorProperties() {
        return textractorProperties;
    }

    /**
     * Suggest if a term should be ignored in a query. Terms that occur in more than 50% of the
     * documents could be ignored if they are involved in a top-level OR statement. (e.g.  A | B).
     *
     * @param term the term to determine if it should be ignored
     * @param termCount The number of terms at top level in a disjunctive query
     * (A | B | C |(D|E)) has four words.
     * @return True if the word could be ignored, false otherwise.
     */
    public boolean suggestIgnoreTerm(final String term, final int termCount) {
        final MutableString processedTerm = new MutableString(term);
        final IndexDetails indexDetails = getIndexDetails("text");
        final TermProcessor currentTermProcessor = indexDetails.getTermProcessor();
        currentTermProcessor.processTerm(processedTerm);
        final int termIndex = findTermIndex(indexDetails, processedTerm);
        if (termIndex == NO_SUCH_TERM) {
            return true;
        }

        try {
            final int documentFrequency = frequency(termIndex);

            final int numberOfDocuments = getDocumentNumber();
            final int threshold = (numberOfDocuments / (termCount));
            return documentFrequency > threshold;
        } catch (IOException e) {
            LOG.error("Problem getting frequency of term " + termIndex, e);
            // suggest to keep the word, since we do not know its frequency.
            return false;
        }
    }

    /**
     * Return the document factory that is being used.
     * @return the document factory
     */
    public AbstractTextractorDocumentFactory getDocumentFactory() {
        return this.factory;
    }
}
