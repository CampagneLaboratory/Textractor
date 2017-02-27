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

import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.Properties;
import textractor.mg4j.document.AbstractTextractorDocumentFactory;

import java.io.IOException;
import java.io.Reader;

/**
 * A word reader that splits a biological sequence into overlapping n-grams.
 *
 * @author Fabien Campagne
 *         Date: Oct 13, 2006
 *         Time: 10:46:38 AM
 */
public class BioSequenceWordReader extends FastBufferedReader implements TextractorWordReader {
    //   private Reader reader;
    final MutableString residueStretch;
    private int nGramLength = 1;

    public void setnGramLength(final int length) {
        this.nGramLength = length;
    }

    @Override
    public boolean next(final MutableString word,
                        final MutableString nonWord) throws IOException {
        if (residueStretch.length() > 1) {
            residueStretch.replace(residueStretch.subSequence(1, residueStretch.length()));
        }
        while (residueStretch.length() < nGramLength) {
            final int c = reader.read();
            if (c == -1) {
                // finished reading;
                word.replace(residueStretch);
                nonWord.replace("");
                return false;
            } else {
                final char residue = (char) c;
                residueStretch.append(residue);
            }
        }
        word.replace(residueStretch.subSequence(0, nGramLength));
        nonWord.setLength(0);
        if (nGramLength==1) {
            residueStretch.setLength(0);
        }
        return true;
    }

    public BioSequenceWordReader() {
        residueStretch = new MutableString();

    }

    @Override
    public FastBufferedReader setReader(final Reader reader) {
        super.setReader(reader);
        residueStretch.setLength(0);
        return this;
    }


    public void configure(final Properties properties) {
    }

    public void saveProperties(final Properties properties) {
        properties.clearProperty(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS.name().toLowerCase());   // this reader does not use this property.
        properties.setProperty(PropertyBasedDocumentFactory.MetadataKeys.WORDREADER,
                this.getClass().getName());

    }

    public void configureFromCommandLine(final String[] args) {


    }


}
