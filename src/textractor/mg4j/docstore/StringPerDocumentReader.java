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

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.mg4j.io.InputBitStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DocumentIndexManager;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * When one String value per document in the index needs to be read, this class
 * can aid in doing that. This can be used to write DOI values, etc.
 * See StringPerDocumentWriter for the class that writes these strings.
 * @author Kevin Dorff
 */
public class StringPerDocumentReader  implements Closeable {
    /**
     * We are using writeInt for pos and writeInt for string length,
     * so, the string offset entry size is 8 bytes. This is in bits.
     */
    public static final int OFFSET_ENTRY_SIZE = 4 * 2 * 8;

    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(StringPerDocumentReader.class);

    /**
     * The basename of the index that corresponds to this document store.
     */
    private final String basename;

    /**
     * Filename suffix for THIS StringPerDocumentReader (to make unique filenames).
     */
    private final String filenameSuffix;

    /**
     * The data file.
     */
    private InputBitStream dataInput;

    /**
     * The offset file.
     */
    private InputBitStream offsetInput;

    /**
     * Object to sync reading from the DOI data file.
     */
    private final Object dataReadSync = new Object();

    /**
     * Object to sync reading from the DOI offsets file.
     */
    private final Object offsetsReadSync = new Object();

    public static StringPerDocumentReader obtainReader(
            final DocumentIndexManager manager,
            final String filenameSuffix) {
        final String basename = manager.getBasename();
        final File dataFile = new File(getDataFilename(basename, filenameSuffix));
        final File offsetsFile = new File(getOffsetsFilename(basename, filenameSuffix));
        if (dataFile.exists() && dataFile.isFile() && dataFile.canRead()
                && offsetsFile.exists() && offsetsFile.isFile() && offsetsFile.canRead()) {
            try {
                return new StringPerDocumentReader(manager, filenameSuffix);
            } catch (FileNotFoundException e) {
                return null;
            }
        } else {
            return null;
        }
    }

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
    private StringPerDocumentReader(
            final DocumentIndexManager manager,
            final String filenameSuffixVal) throws FileNotFoundException {
        this.basename = manager.getBasename();
        if (StringUtils.isBlank(filenameSuffixVal)) {
            this.filenameSuffix = "strings";
        } else {
            this.filenameSuffix = filenameSuffixVal;
        }
        setupReaders();
    }



    /**
     * Set the DOI readers, if the files are available.
     * DOI access is completely automatic if it the data is there.
     * @throws FileNotFoundException error opening data files
     */
    private void setupReaders() throws FileNotFoundException {
        final File dataFile = new File(getDataFilename(basename, filenameSuffix));
        final File offsetsFile = new File(getOffsetsFilename(basename, filenameSuffix));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting up StringPerDocumentWriter readers");
            LOG.debug("data file = " + dataFile.getName());
            LOG.debug("offsets file = " + offsetsFile.getName());
        }

        if (!(dataFile.exists() || offsetsFile.exists())) {
            // The files aren't there. Guess we won't use them
            return;
        }
        dataInput = new InputBitStream(new FastBufferedInputStream(new FileInputStream(dataFile)));
        offsetInput = new InputBitStream(new FastBufferedInputStream(new FileInputStream(offsetsFile)));
    }

    public static String getDataFilename(final String basename, final String filenameSuffix) {
        return basename + "-" + filenameSuffix  + ".data";
    }

    public static String getOffsetsFilename(final String basename, final String filenameSuffix) {
        return basename + "-" + filenameSuffix  + ".offsets";
    }

    /**
     * Retrieves the string value for the document if it exists.
     * @param documentIndex the document index for this article
     * @return The string value for the document
     * @throws java.io.IOException error writing data
     */
    public String readStringForDocument(final int documentIndex) throws IOException {
        if ((dataInput == null) || (offsetInput == null)) {
            // Nothing
            return "";
        }

        final int doiOffset;
        final int doiSize;
        synchronized (offsetsReadSync) {
            // Sync this so changing position and reading are atomic
            offsetInput.position((long) documentIndex * OFFSET_ENTRY_SIZE);
            doiOffset = offsetInput.readInt(Integer.SIZE);
            doiSize = offsetInput.readInt(Integer.SIZE);
        }
        synchronized (dataReadSync) {
            final byte[] buffer = new byte[doiSize / 8];
            dataInput.position(doiOffset);
            dataInput.read(buffer, doiSize);
            return new String(buffer);
        }
    }

    /**
     * Closes this reader and releases any system resources associated
     * with it. If the reader is already closed then invoking this
     * method has no effect.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public void close() throws IOException {
        LOG.debug("Closing up StringPerDocumentReader readers");
        if (dataInput != null) {
            dataInput.close();
        }
        if (offsetInput != null) {
            offsetInput.close();
        }
    }

}
