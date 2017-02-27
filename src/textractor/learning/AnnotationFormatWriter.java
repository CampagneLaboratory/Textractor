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

import org.apache.commons.lang.SystemUtils;
import textractor.database.DocumentIndexManager;
import textractor.datamodel.annotation.AnnotatedTerm;
import textractor.datamodel.annotation.DoubleTermAnnotation;
import textractor.datamodel.annotation.SingleTermAnnotation;
import textractor.datamodel.annotation.TextFragmentAnnotation;

import java.io.IOException;
import java.io.Writer;
import java.util.StringTokenizer;

/**
 * Writes text in a format suitable for human annotation and machine parsing.
 * The format presents each text fragment as in the sample below:
 <PRE>
 --- identifier ---  < enter here properties as inferred from evidence in the text below:
 text fragment (possibly multiple lines)
 </PRE>
 An actual example will look like this:
 <PRE>
 --- 1 ---  < enter here properties as inferred from evidence in the text below:
 However, two types of mutants were constructed one
 bears the R79K mutation on both the dAK and dGK subunits, while the
 other has the R79K mutation on the dGK subunit only (R79K:dGK).
 --- 2 ---  < enter here properties as inferred from evidence in the text below:
Therefore, the R79K substitution seems to
 have a greater effect on dGuo binding than on that of dAdo, but dGK
 modification appears to produce a stimulatory conformational effect on
 the opposite subunit, resembling the known unidirectional activation of
 dAK by either dGuo or dGTP.
 class of enzymes.
 </PRE>

 * Creation Date: Jan 15, 2004
 * Creation Time: 1:39:20 PM
 * @author Fabien Campagne
 */
public final class AnnotationFormatWriter {
    private final Writer writer;
    private final DocumentIndexManager docmanager;
    private int batchNumber = -1;
    private final boolean isWrapping;

    /**
     * Constructs an instance to write the format to the given writer.
     * @param writer The formatted text fragments will be written to.
     * @param is_wrapping
     */
    public AnnotationFormatWriter(final DocumentIndexManager docmanager,
	    final Writer writer, final boolean is_wrapping) {
        super();
        this.docmanager = docmanager;
        this.writer = writer;
        this.isWrapping = is_wrapping;
    }

    public void writeAnnotation(final SingleTermAnnotation annotation)
        throws IOException {
        final String annotationTypeString;
        if (annotation.getStringType() != null) {
            annotationTypeString = (!annotation.isSet() ? "not-" : "")
                + annotation.getStringType();
        } else {
            annotationTypeString = "";
        }
        writeAnnotation(annotation, annotationTypeString);
    }

    public void writeAnnotation(final SingleTermAnnotation annotation,
            final String annotationTypeString) throws IOException {
        writeFileHeader(annotation);
        final boolean markPosition =
            annotation.getTerm().getStartPosition() != -1;
        final AnnotatedTerm term = annotation.getTerm();
        String termString = "";
        if (term.getTermText() !=  null) {
            termString = term.getTermText();
        }
        final String content = termString + ":" + annotationTypeString;
        writeAnnotationHeader(annotation, content);
        final String toBeWrittenText = annotation.getCurrentText(docmanager);
        final String wrappedText = wrapText(toBeWrittenText, markPosition,
                term.getStartPosition(), term.getTermLength(), isWrapping);
        writer.write(wrappedText + '\n');
    }

    public void writeAnnotation(final DoubleTermAnnotation annotation)
        throws IOException {
        final String annotationTypeString;
        if (annotation.getStringType() != null) {
            annotationTypeString = (!annotation.isSet() ? "not-" : "") + annotation.getStringType();
        } else {
            annotationTypeString = "";
        }
        writeAnnotation(annotation, annotationTypeString);
    }

    /**
     *
     * @param annotation
     * @param annotationTypeString
     * @throws IOException
     */
    public void writeAnnotation(final DoubleTermAnnotation annotation,
            final String annotationTypeString) throws IOException {
        writeFileHeader(annotation);
        final boolean markPosition = !(annotation.getTermA().getStartPosition() == -1 || annotation.getTermB().getStartPosition() == -1);
        final AnnotatedTerm termA = annotation.getTermA();
        final AnnotatedTerm termB = annotation.getTermB();
        final String text = annotation.getCurrentText(docmanager);
        String termAString = "";
        String termBString = "";
        if (termA.getTermText() != null) {
            termAString = termA.getTermText();
        }
        if (termB.getTermText() != null) {
            termBString = termB.getTermText();
        }
        final String content =
            termAString + ":" + annotationTypeString + ":" + termBString;
        writeAnnotationHeader(annotation, content);
        final String text_temp = wrapText(text, markPosition, termA.getStartPosition(), termA.getTermLength(),isWrapping);
        writer.write(wrapText(text_temp, markPosition, termB.getStartPosition(), termB.getTermLength(),isWrapping));
        writer.write('\n');
    }

    public void writeFileHeader(final TextFragmentAnnotation annotation)
        throws IOException {
        if (annotation.getAnnotationBatchNumber() != batchNumber) {
            batchNumber = annotation.getAnnotationBatchNumber();
            writer.write("# The following annotations belong to batch number ");
            writer.write(Integer.toString(batchNumber));
            writer.write('\n');
        }
    }

    private void writeAnnotationHeader(final TextFragmentAnnotation annotation, final String annotation_content) throws IOException {
        final int identifier = annotation.getAnnotationNumber();
        if (annotation.isAnnotationImported()) {
            writer.write("*");
        }
        writer.write("--- ");
        writer.write(Integer.toString(identifier));
        writer.write(" --- ");
        writer.write(annotation_content);
        writer.write(" < enter here properties as inferred from evidence in the text below:");
        writer.write('\n');
    }

    public void flush() throws IOException {
        writer.flush();
    }

   private String wrapText(final String text, final boolean markPosition,
           final int position, final int length, final boolean isWrapping) {
        final StringBuffer wrappedTtext = new StringBuffer();
        int currentLineWordCount = 0;
        final int maxWordCount = 15;
        int totalWordCount = 0;
        final StringTokenizer st = new StringTokenizer(text, " \n");
        while (st.hasMoreTokens()) {
            if (markPosition && totalWordCount == position) {
                wrappedTtext.append('>');
            }
            wrappedTtext.append(st.nextToken());
            if (markPosition && totalWordCount == position + length - 1) {
                wrappedTtext.append('<');
            }
            wrappedTtext.append(' ');
            currentLineWordCount++;
            totalWordCount++;
            if (isWrapping && currentLineWordCount > maxWordCount) {
                wrappedTtext.append(SystemUtils.LINE_SEPARATOR);
                currentLineWordCount = 0;
            }
        }
        return wrappedTtext.toString();
    }
}
