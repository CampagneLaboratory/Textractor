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

package textractor.tools.io;

import edu.mssm.crover.cli.CLI;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.tools.Features;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Iterator;

/**
 * Export features for one or several articles.
 * User: campagne
 * Date: Sep 24, 2004
 * Time: 4:33:47 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ExportArticleFeatures {
    private DbManager dbm;

    public static void main(final String[] args) throws IOException, ConfigurationException, TextractorDatabaseException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        final ExportArticleFeatures exporter = new ExportArticleFeatures();
        long articlePMID = CLI.getLongOption(args, "-pmid", -1);
        final String inputFile = CLI.getOption(args, "-input", null);
        final String outputFile = CLI.getOption(args, "-output", null);
        String basename = CLI.getOption(args, "-basename", null);
        if (articlePMID == -1 && inputFile == null) {
            System.err.println("usage: -pmid <article PMID> | -input <input-file> -ouptut <output-file>");
            System.err.println("input-file is a space delimited file with two columns. The first column is the article number,");
            System.err.println("the second column is the label for the set of features produced with the article.");
            System.err.println("output-file will contain the features produced from the articles, in svm-light format. First column is the label, followed by the features.");
            System.exit(1);
        }

        final DbManager dbm = new DbManager(args);
        exporter.setDbm(dbm);
        dbm.beginTxn();
        final TextractorManager textractorManager = dbm.getTextractorManager();
        if (basename == null) {
            basename = textractorManager.getInfo().getCaseSensitiveIndexBasename();
        }

        PrintStream out = System.out;
        if (outputFile != null) {
            System.err.println("Writing output to file " + outputFile);
            out = new PrintStream(new FileOutputStream(outputFile));
        }

        if (inputFile == null) {
            out.print(articlePMID);
            out.print(' ');
            out.println(exporter.export(basename, articlePMID).normalize());
        } else {
            final BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line;
            while ((line = br.readLine()) != null) {
                final String[] fields = line.split("\\s");
                if (fields.length != 2) {
                    System.err.println("File must contain two columns in each line: pmid label. This line is incorrect: " + line);
                    continue;
                }
                final String label = fields[1];
                articlePMID = Integer.parseInt(fields[0]);
                Features features = exporter.export(basename, articlePMID);
                if (features != null) {
                    features = features.normalize();
                    out.print(label);
                    out.print(' ');
                    out.println(features);
                } else {
                    System.err.println("Article " + articlePMID + " not found.");
                }
            }
            out.flush();
        }

        dbm.commitTxn();
    }

    public DbManager getDbm() {
        return dbm;
    }

    public void setDbm(final DbManager dbm) {
        this.dbm = dbm;
    }

    private Features export(final String basename, final long articlePMID) throws IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final Article article =
                dbm.getTextractorManager().getArticleByPmid(articlePMID);
        if (article == null) {
            return null;
        }

        final Features features = new Features();
        final TextractorManager textractorManager = dbm.getTextractorManager();
        final Iterator sentences = textractorManager.getSentenceIterator(article);
        final DocumentIndexManager docManager = new DocumentIndexManager(basename);

        while (sentences.hasNext()) {
            final Sentence sentence = (Sentence) sentences.next();
            final int[] terms = docManager.extractTerms(sentence.getSpaceDelimitedProcessedTerms(docManager).toLowerCase());
            for (final int term : terms) {
                features.incrementFeatureValue(term);
            }
        }
        return features;
    }
}
