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

package textractor.mg4j.io;

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.Properties;
import textractor.mg4j.document.AbstractTextractorDocumentFactory;
import static textractor.mg4j.document.TextractorDocumentFactory.DEFAULT_MINIMUM_DASH_SPLIT_LENGTH;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author campagne
 *         Date: Dec 15, 2005
 *         Time: 11:45:06 AM
 */
public final class TweaseWordReader extends ProteinWordSplitterReader {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 1L;
    private List<String> keeperWords;
    private MutableString keeperNonWord;
    private int minimumDashSplitLength;

    /**
     * Create a new Twease Word Reader.
     */
    public TweaseWordReader() {
        super();
        init();
    }

    /**
     * Create a new Twease Word Reader using a given reader.
     */
    public TweaseWordReader(final Reader r) {
        super(r);
        init();
        setReader(r);
    }

    /**
     * Create a new Twease Word Reader using a given reader and buffer size.
     */
    public TweaseWordReader(final Reader r, final int bufSize) {
        super(r, bufSize);
        init();
        setReader(r);
    }

    public TweaseWordReader(final Properties properties) {
        super(properties);
        configure(properties);
    }

    @Override
    public void configure(final Properties properties) {
        minimumDashSplitLength =
                properties.getInt(AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH, DEFAULT_MINIMUM_DASH_SPLIT_LENGTH);
        super.configure(properties);
    }

    @Override
    public void configureFromCommandLine(final String[] args) {
        super.configureFromCommandLine(args);
        minimumDashSplitLength = CLI.getIntOption(args, "--min-length-split", minimumDashSplitLength);
    }

    @Override
    public void saveProperties(final Properties properties) {
        super.saveProperties(properties);

        properties.setProperty(AbstractTextractorDocumentFactory.MetadataKeys.MINIMUM_DASH_SPLIT_LENGTH,
                Integer.toString(minimumDashSplitLength));

    }

    private void init() {
        keeperWords = new ArrayList<String>();
        keeperNonWord = new MutableString();
        minimumDashSplitLength = DEFAULT_MINIMUM_DASH_SPLIT_LENGTH;
    }

    /**
     * Extracts the next word and non-word.
     * <p/>
     * <p>If this method returns true, a new non-empty word, and possibly
     * a new non-word, have been extracted. It is acceptable
     * that the <em>first</em> call to this method after creation
     * or after a call to {@link #setReader(Reader)} returns an empty
     * word. In other words <em>both <code>word</code> and <code>nonWord</code> are maximal</em>.
     *
     * @param word    the next word returned by the underlying reader.
     * @param nonWord the nonword following the next word returned by the underlying reader.
     * @return true if a new word was processed, false otherwise (in which
     *         case both <code>word</code> and <code>nonWord</code> are unchanged).
     */
    @Override
    public boolean next(final MutableString word, final MutableString nonWord)
            throws IOException {
        if (keeperWords.size() > 0) {
            word.setLength(0);
            word.append(keeperWords.get(0));
            if (keeperWords.size() == 1) {  // this was the last word kept, return the final nonWord part
                nonWord.setLength(0);
                nonWord.append(keeperNonWord);
            } else {
                nonWord.setLength(0);
                nonWord.append('-');   // these words were all delimited by '-'.
            }
            keeperWords.remove(0);
            return true;
        }

        final boolean changed = super.next(word, nonWord);

        if (word.length() < minimumDashSplitLength) {
            return changed;
        } else {
            final int index;
            if ((index = word.indexOf('-')) != -1) {
                return splitAtIndex(index, word, nonWord);
            } else {
                return changed;
            }
        }
    }

    private boolean splitAtIndex(final int index, final MutableString word, final MutableString nonWord) {
        setKeeper(word.substring(index + 1), nonWord);        // keep after delimiter

        nonWord.setLength(0);  // keep only delimiter.
        nonWord.append(word.charAt(index));
        word.setLength(index);

        return true;
    }

    private void setKeeper(final CharSequence word, final CharSequence nonWord) {
        final String[] words = word.toString().split("[-]");
        for (final String w : words) {
            if (w.length() > 0) {
                keeperWords.add(w);
            }
        }
        keeperNonWord.setLength(0);
        keeperNonWord.append(nonWord);
    }
}
