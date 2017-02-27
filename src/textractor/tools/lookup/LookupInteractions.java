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

package textractor.tools.lookup;

import edu.mssm.crover.cli.CLI;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DbManager;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.database.TextractorManager;
import textractor.datamodel.annotation.AnnotatedTerm;
import textractor.datamodel.annotation.DoubleTermAnnotation;
import textractor.learning.AnnotationFormatWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Apr 26, 2004
 * Time: 4:11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public final class LookupInteractions {
    /** Used to log debug and informational messages. */
    private static final Log LOG = LogFactory.getLog(LookupInteractions.class);

    private Collection<Interaction> interactions;
    private int positiveCounter;
    private int negativeCounter;
    private int notFoundCounter;
    private int exclusionCounter;
    private int exclusionDistance = -1;
    private AnnotationFormatWriter iwriter;

    private static final class Interaction {
        private String termA;
        private String termB;
        private String interactionType;

        public Interaction(final String ta, final String ti, final String tb) throws LookupException {
            String tiValidation = ti;
            if (ti.startsWith("not-")) {
                tiValidation = ti.substring(4);
            }

            final DoubleTermAnnotation annotation = new DoubleTermAnnotation();
            // to make sure the interactionType will be recognized.
            if (annotation.getAnnotationMap().getInt(tiValidation)
                    == annotation.getAnnotationMap().defaultReturnValue()) {
		throw new LookupException("annotation_type " + ti
                        + " is not in the database or the annotation format is wrong");
	    }
            termA = concatenate(ta);
            termB = concatenate(tb);
            interactionType = ti;
        }

        private String concatenate(final String terms) {
            final String[] tempTerm = terms.split("\\s");
            final StringBuffer concatenated = new StringBuffer(tempTerm[0]);
            for (int i = 1; i < tempTerm.length; i++) {
                concatenated.append('_');
                concatenated.append(tempTerm[i]);
            }
            return concatenated.toString();
        }

        public String getTermA() {
            return termA;
        }

        public String getTermB() {
            return termB;
        }

        public String getInteractionType() {
            return interactionType;
        }
    }

    public static void main(final String[] args) throws IOException, TextractorDatabaseException, LookupException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        System.out.println("Make sure you have runned LookupProteinname before!");

        final LookupInteractions li = new LookupInteractions();
        li.process(args);
    }

    private void process(final String[] args) throws TextractorDatabaseException, IOException, LookupException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final String basename = CLI.getOption(args, "-basename", null);
        final DocumentIndexManager docmanager = new DocumentIndexManager(basename);

        final String interactionListFilename = CLI.getOption(args, "-i", "data/phosphorylateExamples/Interactions.txt");
        final String interactionOutputFilename = CLI.getOption(args, "-o", "interactionLookupResults.out");
        exclusionDistance = CLI.getIntOption(args, "-exclusionDistance", 20);
        final int batchId = CLI.getIntOption(args, "-batchId", 1);

        interactions = new ArrayList<Interaction>();
        loadInteractionList(interactionListFilename);

        iwriter = new AnnotationFormatWriter(docmanager, new FileWriter(interactionOutputFilename), true);
        final DbManager dbm = new DbManager(args);
        dbm.beginTxn();
        bootstrapInteractionAnnotation(dbm, batchId);
        dbm.commitTxn();

        LOG.info("positive interaction annotations: " + positiveCounter);
        LOG.info("negative interaction annotations: " + negativeCounter);
        LOG.info("excluded annotations according to the distance " + exclusionDistance + " between the terms: " + exclusionCounter);
        LOG.info("annotations within exclusionDistance but do not match any interactions: " + notFoundCounter);
    }

    private void loadInteractionList(final String interactionListFilename) throws LookupException, IOException {
        final File interactionList = new File(interactionListFilename);
        String line;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(interactionList), "UTF-8"));
        try {
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() != 0 && line.charAt(0) != '#') {
                    final String[] interactionStrings = line.trim().split(":");
                    if (interactionStrings.length != 3) {
                        throw new LookupException("the interaction is in wrong format!");
                    }
                    final Interaction interaction =
                            new Interaction(interactionStrings[0], interactionStrings[1], interactionStrings[2]);
                    interactions.add(interaction);
                }
            }
        } finally {
            reader.close();
        }
        LOG.info("Load " + interactions.size() + " interactions.");
    }

    private void bootstrapInteractionAnnotation(final DbManager dbm, final int batchId) throws IOException {
        final TextractorManager tm = dbm.getTextractorManager();
        final Iterator tmi = tm.getDoubleTermAnnotationIterator(batchId);
        while (tmi.hasNext()) {
            final DoubleTermAnnotation tfa = (DoubleTermAnnotation) tmi.next();
            final AnnotatedTerm termA = tfa.getTermA();
            final AnnotatedTerm termB = tfa.getTermB();
            if (termA.getStartPosition() < termB.getStartPosition()) {
                if (termB.getStartPosition() - termA.getEndPosition() > exclusionDistance
                        || termA.getStartPosition() - termB.getEndPosition() > exclusionDistance) {
                    exclusionCounter++;
                    continue;
                }

            }

            final String termAText = termA.getTermText();
            final String termBText = termB.getTermText();

            boolean found = false;
            for (final Interaction interaction : interactions) {
                if ((termAText.equalsIgnoreCase(interaction.getTermA()) && termBText.equalsIgnoreCase(interaction.getTermB()))
                        || (termBText.equalsIgnoreCase(interaction.getTermA()) && termAText.equalsIgnoreCase(interaction.getTermB())))
                {
                    // if it is one direction interaction, make sure the terms are in correct sequences.
                    if (DoubleTermAnnotation.isOneDirection.contains(interaction.getInteractionType()) && (termBText.equalsIgnoreCase(interaction.getTermA()) && termAText.equalsIgnoreCase(interaction.getTermB()))) {
                        tfa.switchTerms();
                    }
                    boolean positive = true;
                    final String property = interaction.getInteractionType();
                    final String annotationType;
                    if (property.startsWith("not-")) {
                        negativeCounter++;
                        positive = false;
                        annotationType = property.substring(4);
                    } else {
                        positiveCounter++;
                        annotationType = property;
                    }
                    tfa.setAnnotationImported(true);
                    tfa.setAnnotation(tfa.getAnnotationMap().getInt(annotationType), positive);
                    iwriter.writeAnnotation(tfa, property);
                    found = true;
                    break;
                }
            }
            if (!found) {
                notFoundCounter++;
            }
        }

        if (iwriter != null) {
            iwriter.flush();
        }
    }
}
