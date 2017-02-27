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
import it.unimi.dsi.mg4j.util.MutableString;
import textractor.database.DbManager;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;

/**
 * Displays textractor data for articles & sentences to the console.
 * User: campagne
 * Date: Oct 28, 2005
 * Time: 8:27:30 AM
 */
public final class DisplayData {
    private static final int NUMBER_NOT_SPECIFIED = -1;
    private DbManager dbm;

    public DisplayData(final String[] args) throws TextractorDatabaseException {
        dbm = new DbManager(args);
    }

    public static void main(final String[] args) throws TextractorDatabaseException {
        final DisplayData dd = new DisplayData(args);
        dd.process(args);


    }

    private void process(final String[] args) {
        final int sentenceNumber = CLI.getIntOption(args, "-sentence", NUMBER_NOT_SPECIFIED);
        final int articleNumber = CLI.getIntOption(args, "-article", NUMBER_NOT_SPECIFIED);

        dbm.beginTxn();
        if (sentenceNumber != NUMBER_NOT_SPECIFIED) {
            displaySentence(sentenceNumber);
        }
        if (articleNumber != NUMBER_NOT_SPECIFIED) {
            displayArticle(articleNumber);
        }
        dbm.commitTxn();
    }

    private void displaySentence(final int sentenceNumber) {
        final Sentence sentence = dbm.getTextractorManager().getSentence(sentenceNumber);
        final MutableString result = new MutableString();
        result.append("Sequence #");
        result.append(sentenceNumber);
        result.append('\n');
        result.append(" -> article: ");
        result.append(sentence.getArticle().getArticleNumber());
        System.out.println(result);
    }

    private void displayArticle(final int articleNumber) {
        final Article article = dbm.getTextractorManager().getArticleByNumber(articleNumber);
        final MutableString result = new MutableString();
        if (article != null) {
            result.append("Article #");
            result.append(articleNumber);
            result.append('\n');
            final String fieldName = "PMID";
            final String value = Long.toString(article.getPmid());

            printField(result, fieldName, value);
            printField(result, "filename", article.getFilename());
        } else {
            result.append("Article #");
            result.append(String.valueOf(articleNumber));
            result.append(" does not exist.");
        }
        System.out.println(result);
    }

    private void printField(final MutableString result, final String fieldName, final String value) {
        result.append(" ");
        result.append(fieldName);
        result.append(": ");
        if (value != null) {
            result.append(value);
        } else {
            result.append("null");
        }
        result.append('\n');
    }
}
