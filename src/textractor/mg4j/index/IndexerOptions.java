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

package textractor.mg4j.index;

import it.unimi.dsi.mg4j.index.CompressionFlags;
import static it.unimi.dsi.mg4j.index.CompressionFlags.Coding;
import static it.unimi.dsi.mg4j.index.CompressionFlags.Component;
import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.TextractorRuntimeException;
import textractor.mg4j.document.AbstractTextractorDocumentFactory;
import textractor.mg4j.io.TextractorWordReader;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * IndexerOptions.
 */
public class IndexerOptions {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(IndexerOptions.class);
    // configuration options for this indexer:

    private String basename;
    public static final String FILTER_NONE = "none";
    public static final String FILTER_ACRONYM = "acronym";
    private boolean createSkips;
    private int quantum;
    private int height;
    private String filter;
    /**
     * The number of occurrences in each batch.
     */
    private String batchSize = "10Mi";
    private TextractorWordReader wordReader;
    private final boolean isLowercaseIndex = true;
    private int chunkSize;
    private Map<Component, Coding> compressionFlags;
    private String termProcessorClassName;

    public void setTermProcessorClassName(final String termProcessorClassName) {
        this.termProcessorClassName = termProcessorClassName;
    }

    public void setWordReaderClassName(final String wordReaderClassName) {
        try {
            wordReader = (TextractorWordReader) Class.forName(wordReaderClassName).newInstance();
        } catch (ClassNotFoundException e) {
            LOG.error("Couldn't load class: " + wordReaderClassName, e);
            throw new TextractorRuntimeException(e);
        } catch (IllegalAccessException e) {
            LOG.error("Couldn't load class: " + wordReaderClassName, e);
            throw new TextractorRuntimeException(e);
        } catch (InstantiationException e) {
            LOG.error("Couldn't load class: " + wordReaderClassName, e);
            throw new TextractorRuntimeException(e);
        }
    }

    public enum StemmerChoice {
        NO_STEMMER,
        PORTER_STEMMER,
        PAICE_HUSK_STEMMER
    }

    private StemmerChoice stemmer;

    /**
     * Minimum index at which a word can be split.
     */
    private int minimumDashSplitLength;

    /**
     * Name of the zip file for the document collection.
     */
    private String zipDocumentCollectionName;

    /**
     * Create a indexer options with a default Word Reader.
     */
    public IndexerOptions() {
        compressionFlags = CompressionFlags.DEFAULT_STANDARD_INDEX;
        stemmer = StemmerChoice.NO_STEMMER;
        termProcessorClassName = LowercaseTermProcessor.class.getName();
    }

    /**
     * Call this method to create a skip index. Skip indices have an embedded
     * skip structure. The skip structure optimizes conjunctive queries.
     *
     * @param quantumVal Quantum for skip index.
     * @param heightVal  Maximum height of towers for skip index.
     * @link http://mg4j.dsi.unimi.it/docs/it/unimi/dsi/mg4j/index/SkipIndexWriter.html
     */
    public final void createSkips(final int quantumVal, final int heightVal) {
        setCreateSkips(true);
        this.setQuantum(quantumVal);
        this.setHeight(heightVal);
    }

    public final TextractorWordReader getWordReader() {
        return wordReader;
    }

    public final void setWordReader(final TextractorWordReader wordReader) {
        this.wordReader = wordReader;
    }

    public final String getBasename() {
        return basename;
    }

    public final void setBasename(final String basename) {
        this.basename = basename;
    }

    public final boolean isLowercaseIndex() {
        return isLowercaseIndex;
    }


    public final String getTermProcessorClassName() {
        return termProcessorClassName;
    }

    public final String getFilter() {
        return filter;
    }

    public final void setFilter(final String filter) {
        this.filter = filter;
    }

    /**
     * Choose if parentheses should be indexed as words.
     *
     * @param parenthesesAreWords True if the index should be built with
     *                            parentheses indexed as words.
     */
    public final void setParenthesesAreWords(final boolean parenthesesAreWords) {
        final Properties props = new Properties();
        wordReader.saveProperties(props);
        props.setProperty(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS,
                Boolean.toString(parenthesesAreWords));
        wordReader.configure(props);

    }

    public final void setBatchSize(final String batchSize) {
        this.batchSize = batchSize;
    }

    public final int getChunkSize() {
        return chunkSize;
    }

    public final void setChunkSize(final int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public static String getFilterNone() {
        return FILTER_NONE;
    }

    public static String getFilterAcronym() {
        return FILTER_ACRONYM;
    }

    public final boolean isCreateSkips() {
        return createSkips;
    }

    public final void setCreateSkips(final boolean createSkips) {
        this.createSkips = createSkips;
    }

    public final int getQuantum() {
        return quantum;
    }

    public final void setQuantum(final int quantum) {
        this.quantum = quantum;
    }

    public final int getHeight() {
        return height;
    }

    public final void setHeight(final int height) {
        this.height = height;
    }

    public final boolean isParenthesesAreWords() {
        assert wordReader != null : "WordReader must be set to read parenthesesAreWord property";
        final Properties props = new Properties();
        getWordReader().saveProperties(props);
        try {
            return props.getBoolean(
                    AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS);
        } catch (NoSuchElementException e) {
            LOG.warn(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS
                    + " property not found.  Defaulting to false.");
            return false;
        }
    }

    public final String getBatchSize() {
        return batchSize;
    }

    public void setTermProcessor(final TermProcessor termProcessor) {
        termProcessorClassName = termProcessor.getClass().getName();
    }

    public TermProcessor getTermProcessor() {
        if (termProcessorClassName == null) {
            return null;
        }
        try {
            return (TermProcessor) Class.forName(
                    termProcessorClassName).getMethod("getInstance").invoke(null);
        } catch (InvocationTargetException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException e) {
            throw new InternalError("Method getInstance must exist in TermProcessor.");
        }
    }

    public final Map<Component, Coding> getCompressionFlags() {
        return compressionFlags;
    }

    public final void setCompressionFlags(final Map<Component, Coding> compressionFlags) {
        this.compressionFlags = compressionFlags;
    }

    public final int getMinimumDashSplitLength() {
        return minimumDashSplitLength;
    }

    public final void setMinimumDashSplitLength(final int minimumDashSplitLength) {
        this.minimumDashSplitLength = minimumDashSplitLength;
    }

    public final String getZipDocumentCollectionName() {
        return zipDocumentCollectionName;
    }

    public final void setZipDocumentCollectionName(final String name) {
        this.zipDocumentCollectionName = name;
    }

    public void setStemmerChoice(final String stemmerChoice) {
        if ("Porter".equalsIgnoreCase(stemmerChoice)) {
            stemmer = StemmerChoice.PORTER_STEMMER;
        } else if ("PaiceHusk".equalsIgnoreCase(stemmerChoice)) {
            stemmer = StemmerChoice.PAICE_HUSK_STEMMER;
        } else {
            stemmer = StemmerChoice.NO_STEMMER;
        }
    }

    public void setStemmerChoice(final StemmerChoice stemmerVal) {
        this.stemmer = stemmerVal;
    }

    public StemmerChoice getStemmer() {
        return stemmer;
    }
}
