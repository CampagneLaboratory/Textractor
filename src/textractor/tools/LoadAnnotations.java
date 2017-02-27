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
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DbManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.learning.AnnotationFormatReader;
import textractor.learning.SyntaxErrorException;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * A tool to load annotations back into the database.
 */
public final class LoadAnnotations {
    private LoadAnnotations() {
        super();
    }

    public static void main(final String[] args) throws ConfigurationException,
            TextractorDatabaseException, IOException, SyntaxErrorException,
            IllegalAccessException, NoSuchMethodException,
            InvocationTargetException, ClassNotFoundException,
            InstantiationException, URISyntaxException {
        final String filename = CLI.getOption(args, "-i", "annotations.out");
        final DbManager dbm = new DbManager(args);
        dbm.beginTxn();
        final TextractorManager textractorManager = dbm.getTextractorManager();
        final AnnotationFormatReader reader =
            new AnnotationFormatReader(textractorManager,
                    new FileReader(filename));
        reader.updateAnnotations();
        dbm.commitTxn();
    }
}
