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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.Article;
import textractor.datamodel.TermOccurrence;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Jul 17, 2004
 * Time: 8:04:56 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ExportTermsByArticle extends ExportImport{
    private static final Log LOG =
	LogFactory.getLog(ExportTermsByArticle.class);

    public static void main(final String[] args) throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final ExportTermsByArticle et = new ExportTermsByArticle();
        et.process(args);
    }

    protected void process(final String[] args) throws TextractorDatabaseException, IOException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        getParameters(args);
        super._process(args);
        writer.write("Counter\tNGram\tPMID\trank\tcount\tterm\n");
        parseByArticle(LOG);
        writer.flush();
        writer.close();
        dbm.commitTxn();
    }

    @Override
    protected int toDoForEachArticle(final Article article, final int[] targetTermOccurrenceIndex, int totalRecordCounter) throws IOException {
        for (int i = 0; i < targetTermOccurrenceIndex.length; i++) {
            if (i < article.getNumMostFrequentTerms()) {
                final TermOccurrence mostFrequentTerm = article.getTermOccurrence(targetTermOccurrenceIndex[i]);
                ++totalRecordCounter;
                writer.write(totalRecordCounter +
                        "\t" + mostFrequentTerm.getIndexedTerm().length +
                        "\t" + article.getPmid() +
                        "\t" + targetTermOccurrenceIndex[i] +
                        "\t" + mostFrequentTerm.getCount() +
                        "\t" + docManager.multipleWordTermAsString(mostFrequentTerm.getIndexedTerm()) + "\n");
            } else {
        	LOG.error("not enough frequent term in article." + article.getFilename());
            }
        }
        return totalRecordCounter;
    }
}
