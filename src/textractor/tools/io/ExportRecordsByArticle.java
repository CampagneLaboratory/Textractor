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
import textractor.datamodel.FeatureCreationParameters;
import textractor.datamodel.TermOccurrence;
import textractor.datamodel.annotation.AnnotationSource;
import textractor.datamodel.annotation.ArticleSingleTermAnnotationIterator;
import textractor.datamodel.annotation.ArticleSingleTermAnnotationSource;
import textractor.learning.SVMFeatureExporter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Jun 2, 2004
 * Time: 5:48:28 PM
 * Export input for svm without creating annotation.
 */
public final class ExportRecordsByArticle extends ExportRecords {
    private static final Log LOG =
	LogFactory.getLog(ExportRecordsByArticle.class);
    private FeatureCreationParameters parameters;

    public static void main(final String[] args) throws TextractorDatabaseException, IOException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        final ExportRecordsByArticle esr = new ExportRecordsByArticle();
        esr.process(args);
    }

    @Override
    protected void process(final String[] args) throws TextractorDatabaseException, IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        super.getParameters(args);
        super.process(args);
    }

    @Override
    public int export(final FeatureCreationParameters parameters) throws IOException, TextractorDatabaseException {
        this.parameters = parameters;
        parseByArticle(LOG);
        return this.parameters.getParameterNumber();
    }

    @Override
    protected int toDoForEachArticle(final Article article, final int[] targetTermOccurrenceIndex, int totalRecordCounter) throws IOException {
        for (int i = 0; i < targetTermOccurrenceIndex.length; i++) {
            if (i < article.getNumMostFrequentTerms()) {
                final TermOccurrence mostFrequentTerm = article.getTermOccurrence(targetTermOccurrenceIndex[i]);

                int svmClass = SVMFeatureExporter.fromAnnotationClass;
                if (exportCategory == toExportListedTerms){
                    if (positiveNames.contains(mostFrequentTerm.getTerm())){
                        svmClass = SVMFeatureExporter.positiveClass;
                    } else if (negativeNames.contains(mostFrequentTerm.getTerm())){
                        svmClass=SVMFeatureExporter.negativeClass;
                    } else {
                        return totalRecordCounter;//exported only records containing listed terms.
                    }
                }
                totalRecordCounter = exportSourceRecordsOfOneTerm(svmClass, article, mostFrequentTerm, totalRecordCounter);
            } else {
                LOG.error("not enough frequent term in article." + article.getFilename());
            }
        }
        return totalRecordCounter;
    }

    private int exportSourceRecordsOfOneTerm(final int svmClass, final Article article, final TermOccurrence mostFrequentTerm, int totalRecordCounter) throws IOException {
        ArticleSingleTermAnnotationIterator astai = new ArticleSingleTermAnnotationIterator(tm, docManager, article, mostFrequentTerm.getIndexedTerm());
        final List<AnnotationSource> annotations =
            new ArrayList<AnnotationSource>();
        while (astai.hasNext()) {
            totalRecordCounter++;
            annotations.add(astai.next());
        }

        if (annotations.size()!=mostFrequentTerm.getCount()){// (mostFrequentTerm.getTerm().equals("0")) {
            LOG.error(article.getFilename() + " " + article.getArticleNumber() + " " + mostFrequentTerm.getTerm());
            LOG.error(Integer.toString(annotations.size()) + " " + Integer.toString(mostFrequentTerm.getCount()));
            astai = new ArticleSingleTermAnnotationIterator(tm, docManager,
                    article, mostFrequentTerm.getIndexedTerm());
            int errorCount = 0;
            while (astai.hasNext()) {
                errorCount++;
                final ArticleSingleTermAnnotationSource articleSingleTermAnnotationSource = astai.next();
                LOG.error(errorCount+" text: " + articleSingleTermAnnotationSource.getCurrentText(docManager));
            }

            System.exit(1);
        }

        exporter.secondPass(svmClass, parameters, annotations, writer);
        return totalRecordCounter;
    }
}
