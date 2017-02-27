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

import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

/**
 * An iterator over terms in an MG4J index. The iterator is getting term
 * directly from the index files.
 * <p/>
 * User: campagne
 * Date: Oct 18, 2005
 * Time: 4:27:50 PM
 */
public final class TermIterator implements Iterator<String>, Closeable {
    private static final Log LOG = LogFactory.getLog(TermIterator.class);
    private String filename = "(terms from java.io.Reader)";
    private FastBufferedReader reader;
    private MutableString nextTerm;
    private boolean consumed;
    boolean inputHasTermFrequency;
    int frequency;

    /**
     * Constructs a term iterator.
     *
     * @param filename The file of the index that lists all the indexed terms.
     * @throws FileNotFoundException If the file cannot be found.
     */
    public TermIterator(final String filename) throws FileNotFoundException {
        try {
            this.reader = new FastBufferedReader(
                    new InputStreamReader(
                            new FileInputStream( filename ), "UTF-8" ));
        } catch (final UnsupportedEncodingException e) {
            throw new FileNotFoundException("Could not open the file '"
                    + filename  + "' as UTF-8 " + e.getMessage());
        }
        nextTerm = new MutableString();
        consumed = true;
        this.filename = filename;
    }

    /**
     * Constructs a term iterator.
     *
     * @param reader Access to the term information.
     */
    public TermIterator(final Reader reader) {
        this.reader = new FastBufferedReader(reader);
        nextTerm = new MutableString();
        consumed = true;
    }

    /**
     * Constructs a term iterator for an input file with frequency information.
     * The format of the input file must be frequency \t term (tab delimited two
     * field, first integer, second string).
     *
     * @param reader Provides a list of terms terms with or without frequency.
     * @throws FileNotFoundException If the file cannot be found.
     */
    public TermIterator(final Reader reader, final boolean inputHasTermFrequency) {
        this(reader);
        this.inputHasTermFrequency = inputHasTermFrequency;
        if (!this.inputHasTermFrequency) {
            frequency = 1;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        reader.close();
        super.finalize();
    }

    /**
     * Closes this iterator. Frees resources (i.e., file descriptor) used by
     * this iterator.
     *
     * @throws IOException When an error occurs closing the reader.
     */
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Test if there is a next term.
     *
     * @return True if a call to next() will return a value. False otherwise.
     */
    public boolean hasNext() {
        if (consumed) {
            try {
                final MutableString line = reader.readLine(nextTerm);
                if (!inputHasTermFrequency) {
                    nextTerm = line;
                } else {
                    if (line != null) {
                        final int delimiterIndex = line.indexOf('\t');
                        this.frequency = Integer.parseInt(line.subSequence(0, delimiterIndex).toString());
                        this.nextTerm = line.substring(delimiterIndex + 1);
                    } else {
                        this.nextTerm = null;
                    }
                }
            } catch (final IOException e) {
                LOG.error("", e);
                throw new InternalError("An error occurred accessing terms in file " + filename +
                        " details may be provided below\n" + e);
            }
            consumed = false;

        }
        return (nextTerm != null);
    }

    /**
     * Obtains the next term.
     *
     * @return The next term (a String).
     */
    public String next() {
        consumed = true;
        return nextTerm.toString();
    }

    public CharSequence nextTerm() {
        consumed = true;
        return nextTerm;
    }

    public MutableString nextMutableStringTerm() {
        consumed = true;
        return nextTerm;
    }

    /**
     * Not supported.
     */
    public void remove() {
        throw new UnsupportedOperationException("Removing is not supported from this iterator type.");
    }

    /**
     * This method returns the frequency of the term if provided in the term
     * list input, or 1 if the term list does not provide frequency information.
     *
     * @return Frequency of the current term.
     */
    public int getFrequency() {
        return frequency;
    }
}
