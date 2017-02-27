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

import cern.jet.random.engine.MersenneTwister;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.Properties;
import textractor.TextractorRuntimeException;
import textractor.mg4j.document.AbstractTextractorDocumentFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Date;

/**
 * A word reader which acts like @link BioSequenceWordReader, but shuffles residues before  splitting words.
 * User: Fabien Campagne
 * Date: Oct 16, 2006
 * Time: 6:59:39 PM
 */
public class ShufflingBioSequenceWordReader extends BioSequenceWordReader implements TextractorWordReader {
    private boolean shuffleResidues;
    private MersenneTwister random;

    public ShufflingBioSequenceWordReader() {
        random = new MersenneTwister(new Date());
        shuffleResidues = true;
    }

    public boolean isShuffleResidues() {
        return shuffleResidues;
    }

    public void setShuffleResidues(final boolean shuffleResidues) {
        this.shuffleResidues = shuffleResidues;
    }

    @Override
    public synchronized FastBufferedReader setReader(final Reader reader) {
        return super.setReader(shuffleResidues ? shuffle(reader) : reader);

    }

    final MutableString buffer = new MutableString();
    final MutableString shuffled = new MutableString();


    @Override
    public void saveProperties(final Properties properties) {
        properties.clearProperty(AbstractTextractorDocumentFactory.MetadataKeys.PARENTHESESAREWORDS.name().toLowerCase());   // this reader does not use this property.
        super.saveProperties(properties);
    }

    private synchronized Reader shuffle(final Reader reader) {
        buffer.setLength(0);
        shuffled.setLength(0);
        try {
            final int length = buffer.read(reader, 1000);

            for (int i = 0; i < length; i++) {
                char c;
                int randomIndex;
                do {
                    randomIndex = (int) (random.raw() * (length));
                    c = buffer.charAt(randomIndex);
                } while (c == '\0');
                buffer.setCharAt(randomIndex, '\0');
                shuffled.append(c);
            }
            // reader.close();
            return new StringReader(shuffled.toString());
        } catch (IOException e) {
            e.printStackTrace();
            throw new TextractorRuntimeException(e);
        }
    }


    @Override
    public void configureFromCommandLine(final String[] args) {
        super.configureFromCommandLine(args);
        setShuffleResidues(CLI.isKeywordGiven(args, "-shuffleResidues"));
    }

    public void setSeed(final int seed) {
        random = new MersenneTwister(seed);
    }

}
