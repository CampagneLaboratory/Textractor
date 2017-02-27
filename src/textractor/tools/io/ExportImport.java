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
import org.apache.commons.logging.Log;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Jul 15, 2004
 * Time: 3:28:55 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ExportImport {
    //to get different categories of terms from a article
    protected int getTermCategory;
    protected static int toGetAll = 0;
    protected static int toGetMulti = 1;
    protected static int toGetCombined = 2;

    //to export records according different categories of term.
    protected int exportCategory;
    protected static int toExportAll = 0;
    protected static int toExportListedTerms = 1;
    protected static final int toExportWildCardTerms = 2;

    //a file that contains names of terms
    // to be considered as true positive protein names, in all articles.
    protected List<String> positiveNames;
    protected List<String> negativeNames;
    protected List<String>[] classesNames;
    protected String outputFilename;
    Writer writer;

    protected DbManager dbm;
    protected TextractorManager tm;
    protected DocumentIndexManager docManager;

    protected final int[] getTargetTermOccurrenceIndex(final Article article) {
        final int[] targetTermOccurenceIndex;
        if (getTermCategory == toGetCombined) {
            targetTermOccurenceIndex = article.getTargetTermOccurenceIndexCombined();
        } else if (getTermCategory == toGetMulti) {
            targetTermOccurenceIndex = article.getTargetTermOccurenceIndexMulti();
        } else {
            targetTermOccurenceIndex = article.getTargetTermOccurenceIndexAll();
        }
        return targetTermOccurenceIndex;
    }

    protected void getParameters(final String[] args) throws IOException {
        getTermCategory = CLI.getIntOption(args, "-t", toGetAll);
        System.out.println("Using term category: " + getTermCategory);
        outputFilename = CLI.getOption(args, "-o", null);

        exportCategory = CLI.getIntOption(args, "-exportCategory", toExportAll);
        if (exportCategory == toExportListedTerms) {
            readClassNames(args);
        }
    }

    protected final void _process(final String[] args) throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final String basename = CLI.getOption(args, "-basename", null);
        docManager = new DocumentIndexManager(basename);
        System.out.println("Using index: " + basename);

        dbm = new DbManager(args);
        tm = dbm.getTextractorManager();
        dbm.beginTxn();

        if (outputFilename != null) {
            writer = new FileWriter(outputFilename);
        }
    }

    protected final void readClassNames(final String[] args) throws IOException {
        final String positiveTermsFilename = CLI.getOption(args, "-positiveNames", null);      //a file that contains names of terms
        final String negativeTermsFilename = CLI.getOption(args, "-negativeNames", null);

        final String classesListFilename = CLI.getOption(args, "-classesList", null);

        if (classesListFilename != null) {
            final BufferedReader br = new BufferedReader(new FileReader(classesListFilename));
            String line;
            final List<String> classesList = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
                classesList.add(line.trim());
            }
            classesNames = new List[classesList.size()];
            for (int i = 0; i < classesList.size(); i++) {
                classesNames[i] = new ArrayList<String>();
                readClassNames(classesList.get(i), classesNames[i]);
            }
        } else {
            if (positiveTermsFilename != null) {
                positiveNames = new ArrayList<String>();
                readClassNames(positiveTermsFilename, positiveNames);
            }
            if (negativeTermsFilename != null) {
                negativeNames = new ArrayList<String>();
                readClassNames(negativeTermsFilename, negativeNames);
            }
        }

        if (classesNames != null) {
            System.out.println("Exporting " + classesNames.length + "classes of names");
        } else if (positiveNames != null && negativeNames != null) {
            System.out.println("Using positive names from file " + positiveTermsFilename);
            System.out.println("Using negative names from file " + negativeTermsFilename);
        } else {
            positiveNames = null;
            negativeNames = null;
            System.out.println("Protein names ignored.");
        }
    }

    protected final void readClassNames(final String termsFilename, final List<String> terms) throws IOException {
        if (termsFilename != null) {
            final BufferedReader br =
        	new BufferedReader(new FileReader(termsFilename));
            String line;
            while ((line = br.readLine()) != null) {
                final String term =
                    Sentence.getSpaceDelimitedProcessedTerms(docManager,
                	    line.trim()).toString();
                terms.add(term);
            }
        }
    }

    protected final void parseByArticle(final Log log) throws IOException {
        Iterator ai;
        int totalRecordCounter = 0;
        int lowerBound = -1;
        final int num_articles = tm.getLastArticleNumber();
        int upperBound = 0;
        while (upperBound < num_articles) {
            dbm.commitTxn();
            System.out.println("commit " + upperBound);
            dbm.beginTxn();
            upperBound = lowerBound + 100;
            ai = tm.getArticleIterator(lowerBound, upperBound);
            while (ai.hasNext()) {
                final Article article = (Article) ai.next();
                final int[] targetTermOccurrenceIndex = getTargetTermOccurrenceIndex(article);
                if (targetTermOccurrenceIndex == null || targetTermOccurrenceIndex.length == 0) {
		    continue;
		}
                if (targetTermOccurrenceIndex[0] == -1) {
		    continue;
		}
                totalRecordCounter = toDoForEachArticle(article, targetTermOccurrenceIndex, totalRecordCounter);
            }
            lowerBound = upperBound;
        }
        log.info("parsed records " + Integer.toString(totalRecordCounter));
    }

    protected int toDoForEachArticle(final Article article, final int[] targetTermOccurrenceIndex, final int totalRecordCounter) throws IOException {
        return 0;
    }
}
