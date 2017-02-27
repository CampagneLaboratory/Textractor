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

import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.TextractorDatabaseException;
import textractor.test.util.AnnotationFile;
import textractor.tools.lookup.ProteinMutation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class TestIdentifyMutations extends TestCase {
    public void testIdentifyMutations() throws ConfigurationException,
            IOException, TextractorDatabaseException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException,
            ClassNotFoundException, InstantiationException, URISyntaxException {
        final String expected = "data/testData/expected_mutations.txt";
        final String actual = "test-results/mutations.txt";

        final String[] args = {
                "-annotate", "-o", actual, "-basename",
                "index/junit-test-basename"
        };

        final ProteinMutation pm = new ProteinMutation();
        pm.process(args);

        AnnotationFile.compare(expected, actual);
    }
}
