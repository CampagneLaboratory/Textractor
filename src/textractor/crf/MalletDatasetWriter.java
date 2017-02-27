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

package textractor.crf;

import it.unimi.dsi.mg4j.io.WordReader;
import it.unimi.dsi.mg4j.util.MutableString;
import textractor.didyoumean.DidYouMeanDocument;
import textractor.didyoumean.DidYouMeanDocumentFactory;
import textractor.mg4j.io.TweaseWordReader;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

/**
 * @author Fabien Campagne
 *         Date: Aug 22, 2006
 *         Time: 11:17:15 AM
 */
public class MalletDatasetWriter implements DatasetWriter {
    final PrintWriter writer;
    private int featureNumber;
    private final WordReader wordReader;
    protected boolean testFormat;


    public MalletDatasetWriter(final PrintWriter output) {
        this.writer = output;
        word = new MutableString();
        nonWord = new MutableString();
        wordReader = new TweaseWordReader();

    }

    final MutableString word;
    final MutableString nonWord;
    DidYouMeanDocument ngramCalculator;

    public final void writeTag(final CharSequence taggedText, final String label) throws IOException {
        wordReader.setReader(new StringReader(taggedText.toString()));

        while (wordReader.next(word, nonWord)) {
            if (word.length() > 0) {
                if (!testFormat) {
                    writeWord(word, label);
                }
                printFeatures(word, label);
                if (!testFormat) {
                    writer.print(label);
                } else {
                    writeWord(word, label);
                }
                writer.println();
            }

        }
    }

    protected void writeWord(final MutableString word, final String label) {
        writer.print("word_");
        writer.print(word);
    }

    protected void printFeatures(final MutableString word, final String label) {
        ngramCalculator = new DidYouMeanDocument(word);
        MutableString gram3 = ngramCalculator.getContent(DidYouMeanDocumentFactory.GRAM3);
        gram3 = gram3.toLowerCase();
        printGrams("gram3_", gram3);
        MutableString gram2 = ngramCalculator.getContent(DidYouMeanDocumentFactory.GRAM2);
        gram2 = gram2.toLowerCase();

        printGrams("gram2_", gram2);
        writer.print(' ');
    }

    private void printGrams(final String prefix, final MutableString gram3) {
        int nextDelimiterIndex;
        int previousIndex = 0;
        while ((nextDelimiterIndex = gram3.indexOf(' ', previousIndex)) != -1) {
            writer.print(' ');
            writer.print(prefix);
            writer.print(gram3.subSequence(previousIndex, nextDelimiterIndex));

            previousIndex = nextDelimiterIndex + 1;
        }
    }

    public final void close() {
        writer.close();
    }

    public final int getLabelCount() {
        return 0;
    }

    public final int getFeatureCount() {
        return featureNumber;
    }

    public final void setTestDataset(final boolean testFormat) {
        this.testFormat = testFormat;
    }
}
