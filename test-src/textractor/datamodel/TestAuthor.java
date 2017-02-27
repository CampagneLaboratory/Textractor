/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

package textractor.datamodel;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Test creation of Authors.
 *
 * @author Kevin Dorff
 */
public class TestAuthor {

    /**
     * Gather a set of authors, with the names parsed from "full name" (single string
     * containing some or all of first name / middle name / initials / last name)
     * and verify it makes an Author object that we expected.
     */
    @Test
    public void testFullName() {
        ArrayList<Author> authors = new ArrayList<Author>();
        authors.add(new Author("Smif"));
        verifyAuthor("", "", "Smif", authors.get(authors.size() - 1));

        authors.add(new Author("Michael G. FitzGerald"));
        verifyAuthor("Michael G", "MG", "FitzGerald", authors.get(authors.size() - 1));

        authors.add(new Author("Masahira Hattori"));
        verifyAuthor("Masahira", "M", "Hattori", authors.get(authors.size() - 1));

        authors.add(new Author("V. Gaspari"));
        verifyAuthor("V", "V", "Gaspari", authors.get(authors.size() - 1));

        authors.add(new Author("J. P. Steffensen"));
        verifyAuthor("J P", "JP", "Steffensen", authors.get(authors.size() - 1));

        authors.add(new Author("Franklin J. D. Serduke"));
        verifyAuthor("Franklin J D", "FJD", "Serduke", authors.get(authors.size() - 1));

        final String allIndexable = Author.getAuthorsIndexText(authors).toString();
        assertEquals(
                "Smif | FitzGerald MG : FitzGerald Michael G | Hattori M : Hattori Masahira | "
                + "Gaspari V | Steffensen JP : Steffensen J P | "
                + "Serduke FJD : Serduke Franklin J D | ", allIndexable);
    }

    /**
     * Verify that the specified authors has the expected names.
     * @param expFirstName the expected first name
     * @param expInitials the expected initials
     * @param expLastName the expected last name
     * @param received the author to check
     */
    private void verifyAuthor(
            final String expFirstName, final String expInitials, final String expLastName,
            final Author received) {
        assertEquals(expFirstName, received.getFirstName());
        assertEquals(expInitials, received.getInitials());
        assertEquals(expLastName, received.getLastName());
    }
}
