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

package textractor.tools;

import edu.mssm.crover.cli.CLI;
import textractor.util.ProcessDirectory;
import textractor.util.Tally;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by IntelliJ IDEA.
 * User: campagne
 * Date: Oct 22, 2003
 * Time: 4:58:38 PM
 * To change this template use Options | File Templates.
 */
public final class TallyWords {
    protected final int maxKw;

    public TallyWords(final String[] args) {
        maxKw = CLI.getIntOption(args, "-n", 50);
    }

    public static void main(final String[] args) throws IOException {
        final TallyWords tally = new TallyWords(args);
        tally.run(args);
    }

    public void run(final String[] args) throws IOException {
        final ProcessDirectory processor = new ProcessDirectory(args, ".txt") {
            @Override
	    public void processFile(final Reader reader, final String input,
                    final String output) throws IOException {
                tallyWordsInFile(input, maxKw);
            }
        };
        processor.process();
    }

    private void tallyWordsInFile(final String filename, final int maxKw)
            throws IOException {
        final ReaderMaker maker = new ReaderMaker(new File(filename));
        final Tally tally = new Tally(" '`,\"[]()");
        tally.tallyKeywords(maker);
        final String[] words = tally.findHighestFrequencyKeywords(maxKw);
        final int[] counts = tally.getTally();
        for (int i = 0; i < words.length; i++) {
            System.out.println("word: " + words[i] + "\t\tcount: " + counts[i]);
        }
        System.out.println("done.");
    }
}
