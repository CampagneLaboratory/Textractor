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

/**
 * Created by Fabien Campagne.
 */

import edu.mssm.crover.cli.CLI;
import textractor.database.DbManager;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.Sentence;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;

/**
 * Prints documents from the database into the MG document format. Individual
 * documents are separated by ascii code 002.
 */
public final class PrintDocuments {
    public PrintDocuments() {
        super();
    }

    public static void main(final String[] args) throws FileNotFoundException, TextractorDatabaseException {
        final DbManager dbm = new DbManager(args);
        dbm.beginTxn();

        final int lowerBound = CLI.getIntOption(args, "-lower-bound", -1);
        final int upperBound = CLI.getIntOption(args, "-upper-bound",
                dbm.getTextractorManager().getSentenceCount());
        final String outputFilename = CLI.getOption(args, "-o", null);
        final PrintStream printer;
        if (outputFilename != null) {
            printer = new PrintStream(new FileOutputStream(outputFilename));
        } else {
            printer = new PrintStream(System.out);
        }

        final Iterator<Sentence> it =
            dbm.getTextractorManager().getSentenceIterator(lowerBound,
                    upperBound);
        while (it.hasNext()) {
            final Sentence sentence = it.next();
            printer.println(sentence.getText());

        }
        dbm.commitTxn();
    }
}

