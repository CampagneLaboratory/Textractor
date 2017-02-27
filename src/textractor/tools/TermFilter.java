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
import textractor.database.DocumentIndexManager;
import textractor.datamodel.Sentence;
import textractor.datamodel.TermOccurrence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: Lei Shi
 * Date: Feb 28, 2005
 * Time: 1:38:10 PM
 */
public final class TermFilter extends ArticleTermCount {
    private static String forProteinname = "exclusion/forProteinnameFileOnly";
    private static String forOrganismname = "exclusion/forOrganismnameFileOnly";
    private static boolean proteinname;
    private static boolean organismname;


    public TermFilter(final String[] args) throws IOException {
        super();
        termList = new Object[1024];
        frequencies = new int[100];
        readExclusionList(args);
    }

    public static void main(final String[] args) throws IOException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final TermFilter termFilter = new TermFilter(args);
        final String filename = CLI.getOption(args, "-i", null);
        final String basename = CLI.getOption(args, "-basename", null);

        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);

        trashCanWriter = new FileWriter(CLI.getOption(args, "-trash", "trash/trash.txt"));
        proteinname = CLI.isKeywordGiven(args, "-protein", false);
        if (!proteinname) {
            organismname = CLI.isKeywordGiven(args, "-organism", false);
        }
        doubleForm.setMaximumFractionDigits(2);
        doubleForm.setMinimumFractionDigits(2);
        if (filename != null) {
            termFilter.filterProteinnameFile(docmanager, filename);
        } else {
            System.err.println("Need a file");
            System.exit(0);
        }
        trashCanWriter.flush();
        System.exit(0);
    }

    private void filterProteinnameFile(final DocumentIndexManager docmanager, final String filename) throws IOException {
        getOptionalTerm();

        final BufferedReader bufferedReader;
        String line;

        final FileWriter filterWriter = new FileWriter(filename + ".filtered");
        bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), "UTF-8"));
        while ((line = bufferedReader.readLine()) != null) {

            final String[] terms = line.split("\\sand\\s");
            String tempTerm;
            String processedTerm;
            for (final String term : terms) {
                processedTerm = Sentence.getSpaceDelimitedProcessedTerms(docmanager, term).toString();
                final TermOccurrence termOccurrence = new TermOccurrence(processedTerm, null, 1);

                //for something like "GB" or "the GB", less than 2 character
                tempTerm = processedTerm.replaceAll("(^the)|\\.|\\s|-", "");
                if (tempTerm.length() < 3) {
                    writeExclusion(termOccurrence, 0, "d0");
                    continue;
                }

                if (proteinname) {
                    //for something like "546-GB", "5AB" or "the 546-GB", "the 5AB"
                    tempTerm = processedTerm.replaceAll("(^the)|(^\\d+)-?|\\s", "");
                    if (tempTerm.length() < 3) {
                        writeExclusion(termOccurrence, 0, "d1");
                        continue;
                    }

                    //for something like "(the) 12-456" but not "14-3-3"
                    if (processedTerm.matches("^(the\\s)?\\d.+")) {
                        tempTerm = processedTerm.replaceAll("(^the)|\\d|\\s", "");
                        if (tempTerm.length() < 2) {
                            writeExclusion(termOccurrence, 0, "d2");
                            continue;
                        }
                    }
                }
                if ((!isInMultipleWordExclusionList(processedTerm)) && (!exclude(0, termOccurrence))) {
                    filterWriter.write(processedTerm + "\n");
                }
            }

        }
        filterWriter.flush();
    }

    private void getOptionalTerm() throws IOException {
        BufferedReader bufferedReader = null;
        if (proteinname) {
            bufferedReader = new BufferedReader(new FileReader(new File(forProteinname)));
        } else if (organismname) {
            bufferedReader = new BufferedReader(new FileReader(new File(forOrganismname)));
        }

        if (bufferedReader != null) {
            String line;
            if (proteinname) {
                excludedPostfix = excludedPostfix.substring(0, excludedPostfix.length() - 2);
            }
            while ((line = bufferedReader.readLine()) != null) {
                if (proteinname) {
                    excludedPostfix += "|(" + line.trim() + ")";
                }
                singleWordExclusionList.add(line.trim());
            }
            if (proteinname) {
                excludedPostfix += ")$";
            }
        }

        if (organismname) {
            excludedPrefix=excludedPrefix.replaceAll("\\|\\(J.\\.\\)", "");
        }
    }

}
