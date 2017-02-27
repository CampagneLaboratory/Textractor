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
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.Article;
import textractor.datamodel.TermOccurrence;
import textractor.datamodel.TermPredictionStatistics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Jun 7, 2004
 * Time: 2:55:57 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ImportSVMResults extends ExportImport {
    String svm_filename;
    String excel_output_filename;
    protected boolean printSentences;
    protected boolean printonly;
    protected boolean cleanTermPredictionStats;
    static final int INT_BATCH_NOT_SET = -1;
    int termCount;

    @Override
    protected void getParameters(final String[] args) throws IOException {
        super.getParameters(args);
        svm_filename = CLI.getOption(args, "-i", "svm-results.out");
        excel_output_filename = CLI.getOption(args, "-o", "statistics.txt");
        printonly = CLI.isKeywordGiven(args, "-print", false);
        printSentences = CLI.isKeywordGiven(args, "-sentence", false);
        cleanTermPredictionStats = CLI.isKeywordGiven(args, "-clean", true); // for now, clean by default
    }

    protected void process(final String[] args) throws TextractorDatabaseException, IOException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        super._process(args);
        setPrintSentences(printSentences);
        if (cleanTermPredictionStats) {
            System.err.println("Removing term prediction statistics");
            removeAllTermPredictionStatistics(tm);
        }
        dbm.commitTxn();
        dbm.beginTxn();
        System.out.println("importing: "+svm_filename);
        final BufferedReader reader = new BufferedReader(new FileReader(svm_filename));
        final FileWriter writer = new FileWriter(excel_output_filename);
        if (!printonly) {
            importSVM(reader);
        }
        dbm.checkpointTxn();
        System.err.println("Writing statistics to " + excel_output_filename);
        printResults(tm, writer);
        writer.flush();
        writer.close();
        dbm.commitTxn();
    }

    protected abstract void importSVM(Reader reader) throws IOException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException;

    protected final void removeAllTermPredictionStatistics(final TextractorManager tm) {
        Article article;
        int lowerBound = -1;
        final int num_articles = tm.getLastArticleNumber();
        int upperBound = 0;
        Iterator it;
        while (upperBound < num_articles) {
            upperBound = lowerBound + 100;
            it = tm.getArticleIterator(lowerBound, upperBound);
            while (it.hasNext()) {
                article = (Article) it.next();
                article.removeTermPredictionStatistics();
            }
            lowerBound = upperBound;
        }
    }

    protected final void setPrintSentences(final boolean print) {
        this.printSentences = print;
    }

    protected final boolean getPrintSentences() {
        return this.printSentences;
    }
    protected final void incrementTermCounter(final Article article, final int[] indexedTerm, final double distance) {
        assert article != null : "article must not be null";
        final TermPredictionStatistics[] stats = article.getTermPredictionStatistics();
        boolean stats_found = false;
        int x = 0;
        if (stats != null) {
            while (x < stats.length && !stats_found) {
                if (Arrays.equals(indexedTerm, stats[x].getTermSearched())) {
                    stats_found = true;
                    updateStatistics(stats[x], distance);
                }
                x++;
            }
        }
        if (!stats_found) {
            final TermPredictionStatistics stat = new TermPredictionStatistics(article);
            stat.setTermSearched(indexedTerm);
            updateStatistics(stat, distance);
            article.addTermPredictionStatistic(stat);
        }
    }

    private void updateStatistics(final TermPredictionStatistics stats, final double distance) {
        if (distance > 0) {
	    stats.incrementTermPredictionCount();
	}
        /*
        stats.setSumDistance(stats.getSumDistance() + distance);
        stats.setMinDistance(distance);
        stats.setMaxDistance(distance);
        */
        stats.addDistance(distance);
    }

    /**
     * Calculate the term prediction ratio. This ratio is defined as the number of times the term is predicted to be a protein name
     * in a given article, with respect to the number of times the term appears in the article.
     */
    private void calculateTermPredictionRatio(final TermPredictionStatistics stat, final Article article) {
        final int[] indexedTermSearched = stat.getTermSearched();
        final double predicted_count = stat.getTermPredictionCount();

        final double ratio;
        final TermOccurrence termOccurrenceForTerm = article.getTermOccurrenceForTerm(indexedTermSearched);
        assert termOccurrenceForTerm != null: "Term occurence must exist for term " + indexedTermSearched;
        ratio = predicted_count / ((termOccurrenceForTerm.getCount()));
        stat.setTermPredictionRatio(ratio);

    }

    /**
     * Prints the results into a tab delimited file which can be read by Excel:  ArticleTerm; ratio;
     */
    public void printResults(final TextractorManager tm, final Writer writer) throws IOException, TextractorDatabaseException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanager = new DocumentIndexManager(tm.getInfo().getCaseSensitiveIndexBasename());
        int lowerBound = -1;
        final int num_articles = tm.getLastArticleNumber();
        int upperBound = 0;
        Iterator articles;
        Article article;

        // write header:
        printStatsHeader(writer);
        int articleCount = 0;
        while (upperBound < num_articles) {
            upperBound = lowerBound + 100;
            System.err.println("Upperbound: " + upperBound);
            articles = tm.getArticleIterator(lowerBound, upperBound);
            while (articles.hasNext()) {
                articleCount++;
                article = (Article) articles.next();
                printStatsForArticle(docmanager, article, writer);
            }
            lowerBound = upperBound;
        }
        System.out.println("article printed: "+articleCount+" |term printed:  "+termCount);
        writer.flush();
    }

    public void printStatsHeader(final Writer writer) throws IOException {
        writer.write("Term" + "\t" + "Ratio" + "\t" +
                "Distance" + "\t" +
                "Article\n");
    }

    public void printStatsForArticle(final DocumentIndexManager docManager,
                                     final Article article, final Writer writer)
            throws IOException {
        final TermPredictionStatistics[] stats =
                article.getTermPredictionStatistics();
        for (final TermPredictionStatistics stat : stats) {
            termCount++;
            calculateTermPredictionRatio(stat, article);
            final int[] indexTerm = stat.getTermSearched();
            final String term = docManager.multipleWordTermAsString(indexTerm);
            final double ratio = stat.getTermPredictionRatio();
            final double[] distances = stat.getDistances();
            for (final double distance : distances) {
                writer.write(term + "\t" + ratio + "\t" +
                        distance + "\t" +
                        article.getPmid() + "\n");
            }
        }
    }
}
