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
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlparser.util.ParserException;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.Article;
import textractor.html.Html2Text;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Iterator;

/**
 * Writes articles in the database as text files. Files are written in the same
 * directory as where the html input was located. The html substring is replaced
 * by txt in the filenames to create the output file.
 */
public final class WriteArticlesAsText {
    private static final Log LOG = LogFactory.getLog(WriteArticlesAsText.class);

    public static void main(final String[] args) throws ParserException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException, TextractorDatabaseException, IOException {
        final WriteArticlesAsText atc = new WriteArticlesAsText();
        final String basename = CLI.getOption(args, "-basename", null);
        final DbManager dbm = new DbManager(args);
        atc.parseArticles(dbm, basename);
        System.exit(0);
    }

    public void parseArticles(final DbManager dbm, final String basename) throws IOException, ParserException, ConfigurationException {
        final int numberArticles;
        int n = -1;

        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);
        dbm.beginTxn();
        numberArticles = dbm.getTextractorManager().getLastArticleNumber();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Number of articles in database: " + numberArticles);
        }
        while (n < numberArticles) {
            dbm.checkpointTxn();
            // get Articles in batches of 1000
            final Iterator<Article> it =
                dbm.getTextractorManager().getArticleIterator(n, n + 1000);
            while (it.hasNext()) {
                final Article article = it.next();
                processArticle(docmanager, article);
            }
            n = n + 1000;
        }
        dbm.commitTxn();
    }


    public void processArticle(final DocumentIndexManager docmanager, final Article a) throws IOException, ParserException {
        final Reader in = new InputStreamReader(new FileInputStream(a.getFilename()));
        final Html2Text parser = new Html2Text();
        parser.parse(in);
        in.close();
        final String s = parser.getText();
        final FastBufferedReader input = new FastBufferedReader(new StringReader(s), 1024 * 1024);
        final String outputFilename = a.getFilename().replaceAll("html", "txt");
        final BufferedWriter output = new BufferedWriter(new FileWriter(outputFilename));
        MutableString line = new MutableString();
        while ((line = input.readLine(line)) != null) {
            line.write(output);
            output.write('\n');
        }
        output.close();
    }
}
