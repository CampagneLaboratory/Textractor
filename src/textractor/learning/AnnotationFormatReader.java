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

package textractor.learning;

import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorManager;
import textractor.datamodel.annotation.TextFragmentAnnotation;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Reads text in a format suitable for human annotation and machine parsing.
 * Provides a way to find the TextFragmentAnnotaion that correspond to the
 * annotaions in the file, and update them with the annotation attributes
 * parsed from the file.
 *
 * The format presents each text fragment as in the sample below. Lines that
 * start with *--- indicate that the annotation has been modified by the
 * human annotator and should be updated. Lines that start with ---
 * have not been modified and will not be updated in the database.
 * <PRE>
 * --- identifier --- annotation-attributes < enter here properties as inferred from evidence in the text below:
 * text fragment (possibly multiple lines)
 * --- identifier --- < enter here properties as inferred from evidence in the text below:
 * text fragment (possibly multiple lines)
 * </PRE>
 * An actual example will look like this:
 * <PRE>
 * --- 1 ---  R79K:mutation < enter here properties as inferred from evidence in the text below:
 * However, two types of mutants were constructed one
 * bears the R79K mutation on both the dAK and dGK subunits, while the
 * other has the R79K mutation on the dGK subunit only (R79K:dGK).
 * --- 2 ---  < enter here properties as inferred from evidence in the text below:
 * Therefore, the R79K substitution seems to
 * have a greater effect on dGuo binding than on that of dAdo, but dGK
 * modification appears to produce a stimulatory conformational effect on
 * the opposite subunit, resembling the known unidirectional activation of
 * dAK by either dGuo or dGTP.
 * class of enzymes.
 * </PRE>
 *
 * @author Fabien Campagne
 */
public final class AnnotationFormatReader implements Closeable {
    private final Reader reader;
    private final TextractorManager tm;
    private final DocumentIndexManager docmanager;

    /**
     * Constructs an instance to write the format to the given reader.
     *
     * @param reader The formatted text fragments will be written to.
     */
    public AnnotationFormatReader(final TextractorManager tm, final Reader reader) throws ConfigurationException {
        this.reader = reader;
        this.tm = tm;
        docmanager = new DocumentIndexManager(tm.getInfo().getCaseSensitiveIndexBasename());
    }

    /**
     * Writes a text fragment in a format suitable for annotation.
     */
    public void updateAnnotations() throws IOException, SyntaxErrorException {
        final BufferedReader br = new BufferedReader(reader);
        String line;

        int lineCount = 1;
        boolean annotated;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("*---")) {
                // found annotation that needs updating in the database.
                annotated = true;
                updateAnnotation(line, lineCount, annotated);
            } else if (line.startsWith("---")) {
                // this annotation should be marked as being unannotated
                // (i.e., not updated), so it can be used in the test data set.
                annotated = false;
                updateAnnotation(line, lineCount, annotated);
            }

            // ignore everything else in this file.
            lineCount++;
        }
    }

    private void updateAnnotation(final String line, final int lineNumber,
            final boolean annotated) throws SyntaxErrorException {
        final String[] properties = line.split("(\\s|\\t)+");
        final String annotationId = properties[1];
        if (annotationId == null) {
            throw new SyntaxErrorException("Cannot find annotation number on the line", lineNumber);
        }

        final int annotationNumber;
        try {
            annotationNumber = Integer.parseInt(annotationId);
        } catch (final NumberFormatException e) {
            throw new SyntaxErrorException("Cannot parse annotation number (" + annotationId + ") on the line", lineNumber);
        }

        final TextFragmentAnnotation annotation =
            tm.getAnnotationById(annotationNumber);

        if (annotation == null) {
            throw new SyntaxErrorException("annotation corresponding to number (" + annotationId + ") cannot be found in the database"+line+" ",lineNumber);
        }

        // update annotation here:
        String token;
        if (annotated) {
            int i = 3;
            while (properties.length>i && !((token=properties[i]).equals("<"))) {
                // token within --- and <  is an attribute/property:
                setAnnotationProperty(annotation, token, lineNumber);
                i++;
            }
        }

        annotation.createIndexedTerms(docmanager);
        if (annotated) {
            annotation.setAnnotationImported(true);
        }
    }

    private void setAnnotationProperty(final TextFragmentAnnotation annotation,
            final String token, final int lineNumber)
        throws SyntaxErrorException {
        final int index = token.indexOf(':');
        if (index == -1) {
            throw new SyntaxErrorException("annotation property does not contain a colon (':') (" + token + ")", lineNumber);
        }

        if (index >= token.length()) {
            throw new SyntaxErrorException("annotation does not contain a property after the colon (':') (" + token + ")", lineNumber);
        }

        boolean positive = true;
        String property = token.substring(index + 1, token.length());
        if (property.startsWith("not-")) {
            positive = false;
            property = property.substring(4);
        }

        final int annotationType =
            annotation.getAnnotationMap().getInt(property);
        if (annotationType == annotation.getAnnotationMap().defaultReturnValue()) {
            throw new SyntaxErrorException("annotation_type " + property
                    + " is not in the database or the annotation format is wrong", lineNumber);
        }

        annotation.setAnnotation(annotationType, positive);
    }

    public void close() {
        if (docmanager != null) {
           docmanager.close();
        }
    }
}
