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

package textractor.mg4j.docstore;

import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import it.unimi.dsi.mg4j.io.OutputBitStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;

import java.io.*;

/**
 * When one String value per document in the index needs to be written, this class
 * can aid in doing that. This can be used to write DOI values, etc.
 * See StringPerDocumentReader for the class that reads these strings.
 * @author Kevin Dorff
 */
public class StringPerDocumentWriter implements Closeable {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(StringPerDocumentWriter.class);

    /**
     * The basename of the index that corresponds to this document store.
     */
    private final String basename;

    /**
     * Filename suffix for THIS StringPerDocumentWriter (to make unique filenames).
     */
    private final String filenameSuffix;

    /**
     * The data output file.
     */
    private OutputBitStream dataOutput;

    /**
     * The offsets output file.
     */
    private OutputBitStream offsetOutput;

    /** An empty value. Used when there are gaps in the documents that are written. */
    private static final String EMPTY_VALUE = StringUtils.EMPTY;

    /** The current position in the output file. Used to help write the offets data. */
    private int dataPosition;

    /** The last document index number written, to avoid gaps. */
    private int lastDocumentIndexWritten;

    /** The number of documents that have been written. */
    private int writtenDocuments;

    /**
     * Initialize a string per document writer.
     *
     * @param manager The full text index that this document store will be
     * associated to.
     * @param filenameSuffixVal when creating the filenames this writer will write
     * two, this string will be appended after basename and before ".data"
     * and ".offsets". If no value is provided (null or emptys tring) "strings"
     * will be used.
     * @throws FileNotFoundException When docstore files cannot be written with
     * the given basename.
     */
    public StringPerDocumentWriter(
            final DocumentIndexManager manager,
            final String filenameSuffixVal) throws FileNotFoundException {
        this.basename = manager.getBasename();
        if (StringUtils.isBlank(filenameSuffixVal)) {
            this.filenameSuffix = "strings";
        } else {
            this.filenameSuffix = filenameSuffixVal;
        }
        setupWriters();
    }

    /**
     * Set the writers and streams for outputting the string values.
     * @throws FileNotFoundException if we cannot open the
     * files for writing.
     */
    private void setupWriters() throws FileNotFoundException {
        final File storeFile = new File(basename + "-" + filenameSuffix  + ".data");
        final File offsetsFile = new File(basename + "-" + filenameSuffix  + ".offsets");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting up StringPerDocumentWriter writers");
            LOG.debug("data file = " + storeFile.getName());
            LOG.debug("offsets file = " + offsetsFile.getName());
        }

        // Delete the store and offsets files if they exist.
        if (storeFile.exists()) {
            storeFile.delete();
        }
        if (offsetsFile.exists()) {
            storeFile.delete();
        }

        dataOutput = new OutputBitStream(new FastBufferedOutputStream(new FileOutputStream(storeFile)));
        offsetOutput = new OutputBitStream(new FastBufferedOutputStream(new FileOutputStream(offsetsFile)));
    }

    /**
     * Append the string to external storage.
     * @param documentIndex the document index for this article
     * @param data the string to be written
     * @return int bits written
     * @throws IOException error writing data
     */
    public int appendDocumentString(final int documentIndex, final String data) throws IOException {
        fillMissingDocuments(documentIndex);
        final byte[] bytes = data.getBytes();
        final int bitCount = bytes.length * 8;
        dataOutput.write(bytes, (long) bitCount);

        // In the offsets file we will write the position
        // of the string as an int and the size of the string
        // as an int.
        offsetOutput.writeInt(dataPosition, Integer.SIZE);
        offsetOutput.writeInt(bitCount, Integer.SIZE);
        lastDocumentIndexWritten = documentIndex;
        dataPosition += bitCount;
        writtenDocuments++;
        return bitCount;
    }

    /**
     * Append an empty document for each gap in the sequence of document
     * number. Gaps may occur if some documents were deleted from the
     * database after they were submitted. This method pads the index to
     * allow random access. Each gap add an offset but does not cost
     * document content space.
     *
     * @param documentIndex Index of the new document.
     * @throws IOException If an error occurs writting empty documents.
     */
    private void fillMissingDocuments(final int documentIndex) throws IOException {
        while (documentIndex > lastDocumentIndexWritten + 1) {
            // append empty documents until lastDocumentIndexWritten is just before documentIndex.
            appendDocumentString(lastDocumentIndexWritten + 1, EMPTY_VALUE);
        }
    }

    /**
     * The number of documents written by this writter.
     *
     * @return The number of documents written by this writer.
     */
    public int writtenDocuments() {
        return writtenDocuments;
    }

    public void flush() throws IOException {
        dataOutput.flush();
        offsetOutput.flush();
    }

    public void close() throws IOException {
        LOG.debug("Closing up StringPerDocumentWriter writers");
        if (dataOutput != null) {
            dataOutput.flush();
            dataOutput.close();
        }
        if (offsetOutput != null) {
            offsetOutput.flush();
            offsetOutput.close();
        }
    }
}
