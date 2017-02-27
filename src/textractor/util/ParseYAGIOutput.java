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

package textractor.util;

import edu.mssm.crover.cli.CLI;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Oct 13, 2004
 * Time: 11:13:53 AM
 * To change this template use File | Settings | File Templates.
 */
public final class ParseYAGIOutput extends ParseOutput {

    public static void main(final String[] args) throws IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final String basename = CLI.getOption(args, "-basename", null);

        final String YAGIOut = CLI.getOption(args, "-i", null);
        final String parsedOutput = CLI.getOption(args, "-o", "parsed-yagi.out");
        if (YAGIOut == null || parsedOutput == null) {
            System.err.println("This utility converts the YAGI writer into a tab delimited file.\n" +
                    "The first column of the file is the identifier of the article/text given to YAGI;\n" +
                    "The second column is the protein name that was extracted by YAGI (delimited by <GENE> in the text writer);\n" +
                    "The third column is textractor index format of this protein/gene name;\n" +
                    "The fourth column is the number of occurrences of this protein/gene name in the article.\n");
            System.err.println("usage: -basename <index-basename> -i <YAGI-writer> -o <filename for tab delimited parsed file");
            System.err.println(" <index-basename> names the basename of an index that was built with the text splitting rules that you want applied during this conversion. The index is not used for lookup in any way.");

        }

        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);

        String line;
        final BufferedReader br = new BufferedReader(new FileReader(YAGIOut));
        final BufferedWriter writer = new BufferedWriter(new FileWriter(parsedOutput));
        String id = "not-set";

        String term;

        while ((line = br.readLine()) != null)

        {
            final String[] termLine = line.split("\t");
            if (!termLine[0].equals(id)) {
                if (!id.equals("not-set")) {
                    outputTabular(docmanager, id, articleTermsMap, writer);
                    articleTermsMap.clear();
                }
                id = termLine[0];
            }
            term = termLine[1];
            accumulate(term);

        }

        outputTabular(docmanager, id, articleTermsMap, writer);

        writer.flush();
    }
}
