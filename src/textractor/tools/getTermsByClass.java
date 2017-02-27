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
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.Article;
import textractor.datamodel.FeatureCreationParameters;
import textractor.datamodel.Sentence;
import textractor.datamodel.annotation.SingleTermAnnotation;
import textractor.learning.AnnotationFormatWriter;
import textractor.tools.io.ExportRecords;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Random;


/**
 * A tool to return sentences that may contain protein names.
 * <p/>
 * Creation Date: Jan 14, 2004
 * Creation Time: 6:19:14 PM
 */
public final class getTermsByClass extends ExportRecords {
    private static Log log = LogFactory.getLog(getTermsByClass.class);
    private static final int INT_WRITE_ALL_ANNOTATIONS = -2;
    // will hold the Id of each annotation created by this method.
    private int[] annotationIds;
    private int totalAnnotationNumber;
    private int positiveCount;
    private int negativeCount;
    //private int[] classNamesCount;
    private int newBatchNumber;
    private boolean printText;
    private boolean annotate;
    private int numToWrite;  // write all annotations.
    private boolean randomise_output;
    private boolean printId;
    private AnnotationFormatWriter writer;
    private static final int[] positions = new int[100];  // max a thousand occurences in a given text string.

    public getTermsByClass() {
        super();
        annotationIds = new int[10];
    }

    public static void main(final String[] args) throws IOException, TextractorDatabaseException, ConfigurationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException, URISyntaxException {
        final getTermsByClass tool = new getTermsByClass();
        tool.process(args);
    }

    @Override
    protected void getParameters(final String[] args) throws IOException {
        super.getParameters(args);
        printText = CLI.isKeywordGiven(args, "-text", false);
        annotate = CLI.isKeywordGiven(args, "-annotate", false);
        numToWrite = CLI.getIntOption(args, "-write", INT_WRITE_ALL_ANNOTATIONS);                      // write all annotations.
        randomise_output = CLI.isKeywordGiven(args, "-random", false);

        printId = false;
        writer = null;

        if (annotate) {
            printText = false;
        }

        if (annotate && outputFilename != null){
            writer = new AnnotationFormatWriter(docManager, new FileWriter(outputFilename), false);
        }
        readClassNames(args);
    }

    @Override
    protected void process(final String[] args) throws IOException, TextractorDatabaseException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        getParameters(args);
        super._process(args);
        newBatchNumber = tm.getNextAnnotationBatchNumber();
        System.out.println("Creating annotation batch #" + newBatchNumber);
        System.out.println("First pass: create annotations");

        parseByArticle(log);

        dbm.commitTxn();
        System.out.println(positiveCount + " positive examples were annotated.");
        System.out.println(negativeCount + " negative examples were annotated.");
        System.out.println("ratio positive/negative: " + ((((double) positiveCount) / (double) negativeCount) * 100));
        System.out.println("Second pass: write annotations to file");

        dbm.beginTxn();
        // now, the annotations are persistent, we know how many they are, we can get them out in random order if needed:
        if (annotate && outputFilename != null) {
            SingleTermAnnotation annotation;

            final int num_anns = totalAnnotationNumber;
            log.info("Number of annotations: " + num_anns);
            int ann_num;
            final Random random = new Random();
            random.setSeed(1618033988);

            for (int x = 0; x < num_anns; ++x) {
                if (randomise_output) {
                    ann_num = (random.nextInt(x));
                } else {
                    ann_num = x;
                }
                annotation = (SingleTermAnnotation) tm.getAnnotationById(annotationIds[ann_num]);
                writer.writeAnnotation(annotation);
                if (numToWrite != INT_WRITE_ALL_ANNOTATIONS) {
		    --numToWrite;
		}
                if (numToWrite == 0) {
		    break; // stop when so many have been written.
		}
            }
        }
        if (writer != null) {
	    writer.flush();
	}
        dbm.commitTxn();
        if (writer != null) {
	    writer.flush();
	}
        System.exit(0);
    }

    @Override
    protected int toDoForEachArticle(final Article article, final int[] targetTermOccurrenceIndex, int totalRecordCounter) throws IOException {
        final String[] frequentTerms =
            new String[targetTermOccurrenceIndex.length];
        for (int i = 0; i < targetTermOccurrenceIndex.length; i++) {
            // only annotate the sentences with the most frequent terms within SAME article.
            frequentTerms[i] = article.getTermOccurrence(targetTermOccurrenceIndex[i]).getTerm();
        }

        final long documentNumberLowerBound = article.getDocumentNumberRangeStart() - 1;
        final long documentNumberUpperBound = documentNumberLowerBound + 1
        	+ article.getDocumentNumberRangeLength() - 1;
        final String sfilter =
            "this.documentNumber <= " + documentNumberUpperBound;
        final Iterator<Sentence> it =
            tm.getSentenceIterator(documentNumberLowerBound, sfilter);

        while (it.hasNext()){
            final Sentence sentence = it.next();
            if (printId) {
		System.out.print(sentence.getDocumentNumber());
	    }
            if (printText) {
                System.out.print(" ");
                System.out.print(removeNewLines(sentence.getText()));
            } else if (annotate) {
                // for each sentence: convert the sentence to a set of annotation for the mutation attribute.
                // take the top two most frequently occurring terms, and extract all sentences containing them.

                sentenceTofrequentTermsAnnotations(sentence, newBatchNumber, frequentTerms);
                totalRecordCounter++;
            }
            if (printText || printId) {
		System.out.print('\n');
	    }
        }
        return totalRecordCounter;
    }

    @Override
    public int export(final FeatureCreationParameters parameters) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private void sentenceTofrequentTermsAnnotations(final Sentence sentence, final int newBatchNumber,
                                                  final String[] frequentTerms) {
        int[] termOccurrencesInSentence;
        final MutableString text = sentence.getSpaceDelimitedProcessedTerms(docManager);
        final int[] indexedTerms = docManager.extractSpaceDelimitedTerms(text);
        for (final String term : frequentTerms) { // for each protein name
            if (term == null) {
		continue;   // skip null protein names (most frequent was not defined
	    }
            // in the article).
            termOccurrencesInSentence = countOccurrences(indexedTerms,
                    docManager.extractTerms(new MutableString(term)));
            for (final int aTermOccurrencesInSentence : termOccurrencesInSentence) {  // for each time the protein name occurs in the sentence:
                final SingleTermAnnotation annotation = tm.createSingleTermAnnotation(newBatchNumber);
                annotation.setUseSentenceText(true);
                annotation.setSentence(sentence);

                annotation.getTerm().setText(term, aTermOccurrencesInSentence);

                //for multiclass export

                if (classesNames != null) {
                    for (int k = 0; k < classesNames.length; k++) {
                        if (classesNames[k].contains(term)) {
                            annotation.setAnnotationImported(true);
                            annotation.setAnnotation(k + 1); //class number should be 1,2,3 instead of 0,1,2
                            break;
                        }
                    }
                }

                //for binary export
                if (positiveNames != null && positiveNames.contains(term)) {
                    // this term is considered a true protein name, so we e mark the annotation as annotated and
                    // set the protein attribute to true
                    annotation.setAnnotationImported(true);
                    annotation.setAnnotation(SingleTermAnnotation.INT_ANNOTATION_PROTEIN, true);
                    positiveCount++;
                }
                if (negativeNames != null && negativeNames.contains(term)) {
                    // this term is considered a false protein name, so we mark the annotation as annotated and
                    // set the protein attribute to false
                    annotation.setAnnotationImported(true);
                    annotation.setAnnotation(SingleTermAnnotation.INT_ANNOTATION_PROTEIN, false);
                    negativeCount++;
                }
                totalAnnotationNumber++;
                annotationIds = ensureCapacity(annotationIds, (int) (totalAnnotationNumber * 1.2)); // make sure array is at least 20% larger than needed.
                annotationIds[totalAnnotationNumber] = annotation.getAnnotationNumber();
                dbm.makePersistent(annotation);
            }
        }
    }

    private int[] ensureCapacity(final int[] array, final int mininumCapacity) {
        final int length = array.length;
        if (length < mininumCapacity) {
            final int[] newArray = new int[mininumCapacity];
            System.arraycopy(array, 0, newArray, 0, length);
            return newArray;
        } else {
	    return array;
	}
    }

    private static synchronized int[] countOccurrences(final int[] indexedTerms, final int[] indexedTermOfInterest) {
        int numPositions = 0;
        for (int i = 0; i < indexedTerms.length-indexedTermOfInterest.length+1; ++i) {
            int found=0;
            for (int j=0; j<indexedTermOfInterest.length;j++){
                if (indexedTerms[i+j] == indexedTermOfInterest[j]){
                    found++;
                } else {
		    break;
		}
            }
            if (found==indexedTermOfInterest.length){
                if (numPositions > 99) {
		    break;    // limit on number of occurences that will be considered.
		}
                positions[numPositions] = i;
                numPositions++;
            }
        }

        final int[] trimmed = new int[numPositions];
        System.arraycopy(positions, 0, trimmed, 0, numPositions);
        return trimmed;
    }

    public static String removeNewLines(final String text) {
        return text.replace('\n', ' ');
    }

}
