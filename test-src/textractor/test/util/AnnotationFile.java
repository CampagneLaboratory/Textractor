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

package textractor.test.util;

import junit.framework.Assert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Utilities useful for testing and comparing annotation batch files.
 */
public class AnnotationFile extends Assert {
    /**
     * Compares two annotation format files for equality ignoring batch
     * and actual index numbers
     *
     * @param expected name of the expected file
     * @param actual name of the actual file
     * @throws IOException if there is a problem reading either file
     */
    public static void compare(final String expected, final String actual)
        throws IOException {
        final String line1 =
            "# The following annotations belong to batch number ";

        final BufferedReader expectedReader =
            new BufferedReader(new FileReader(expected));
        final BufferedReader acutalReader =
            new BufferedReader(new FileReader(actual));

        // make sure heading matches
        final String expectedLine = expectedReader.readLine();
        assertNotNull(expectedLine);
        assertTrue(expectedLine.startsWith(line1));

        final String actualLine = acutalReader.readLine();
        assertNotNull(actualLine);
        assertTrue(expectedLine.startsWith(line1));

        // collect the list of annotations
        final Collection<Annotation> expectedAnnotations =
            collectAnnotations(expectedReader);
        final Collection<Annotation> actualAnnotations =
            collectAnnotations(acutalReader);

        // make sure they are the same
        assertFalse("The expected annotation list should not be empty",
                expectedAnnotations.isEmpty());
        assertEquals("The number of annotations differ",
                expectedAnnotations.size(), actualAnnotations.size());

        // iterate over the list and make sure that the list is the same
        for (final Annotation actualAnnotation : actualAnnotations) {
            assertTrue("Not expected: " + actualAnnotation.toString(),
                    expectedAnnotations.contains(actualAnnotation));
            // remove found items so that we can make sure they all were there
            assertTrue(expectedAnnotations.remove(actualAnnotation));
        }

        // we should have removed all the items by the end of the loop
        assertTrue(expectedAnnotations.isEmpty());
    }

    /**
     * Extracts annotations from the given reader object.
     *
     * @param reader The reader to collect annotations from.
     * @return A list of annotations in the file
     * @throws IOException if there is a problem with the reader.
     */
    private static Collection<Annotation> collectAnnotations(
            final BufferedReader reader) throws IOException {
        final Collection<Annotation> annotations = new ArrayList<Annotation>();
        String line = null;
        StringBuffer sentence = new StringBuffer();
        Annotation annotation = null;

        while ((line = reader.readLine()) != null) {
            // found annotation that needs updating in the database.
            if (line.startsWith("---")) {
                if (annotation != null) {
                    annotation.setSentence(sentence.toString());
                    annotations.add(annotation);
                    sentence = new StringBuffer();
                }
                annotation = new Annotation();

                final String[] properties = line.split("(\\s|\\t)+");
                assertEquals("---", properties[0]);
                // we don't really care what the value is, but
                // we want to make sure that there is a valid number there
                try {
                    Integer.parseInt(properties[1]);
                } catch (final NumberFormatException e) {
                    fail("There is no valid number in " + line);
                }
                assertEquals("---", properties[2]);
                annotation.setTerm(properties[3]);
                assertEquals("<", properties[4]);
                assertTrue(line.endsWith("< enter here properties as inferred from evidence in the text below:"));
            } else {
                sentence.append(line);
            }
        }

        if (annotation != null) {
            annotation.setSentence(sentence.toString());
            annotations.add(annotation);
        }
        return annotations;
    }

    /**
     * Class to support reading annotation files.
     */
    private static class Annotation {
        /** Term in the file. */
        private String term;
        /** Sentence in the file. */
        private String sentence;
        /**
         * @return Returns the sentence.
         */
        public String getSentence() {
            return sentence;
        }
        /**
         * @param sentence The sentence to set.
         */
        public void setSentence(final String sentence) {
            this.sentence = sentence;
        }
        /**
         * @return Returns the term.
         */
        public String getTerm() {
            return term;
        }
        /**
         * @param term The term to set.
         */
        public void setTerm(final String term) {
            this.term = term;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object obj) {
            // check for null cases
            if (obj == null) {
                return false;
            }

            final Annotation annotation = (Annotation )obj;
            if (term == null && annotation.getTerm() != null) {
                return false;
            }
            if (sentence == null && annotation.getSentence() != null) {
                return false;
            }

            // not null, so compare string values
            return term.equals(annotation.getTerm())
                    && sentence.equals(annotation.getSentence());
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            final StringBuffer buffer = new StringBuffer("--- ");
            buffer.append(term);
            buffer.append(" --- ");
            buffer.append(sentence);
            return buffer.toString();
        }
    }
}
