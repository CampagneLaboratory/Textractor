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

import edu.cornell.med.icb.util.ICBStringNormalizer;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An author for an Article.
 *
 * @author Kevin Dorff
 */
public class Author implements Serializable {

    /** An empty mutable string. */
    public static final MutableString EMPTY_MUTABLE_STRING = new MutableString(0);

    /** The author's last name. */
    private final String lastName;

    /** The author's first name. */
    private final String firstName;

    /** The author's initials. */
    private final String initials;

    /** Indexing characters. */
    private static final char[] SPECIAL_INDEXING_CHARS = new char[] {':', '|', ','};
    private static final char[] SPECIAL_OTHER_CHARS = new char[] {']', '-'};

    /**
     * Constructor.
     * @param lastNameVal the author's last name
     * @param firstNameVal the author's first name
     * @param initialsVal the author's initials (if not provided they will be created based
     * on the value in firstNameVal)
     */
    public Author(final String lastNameVal, final String firstNameVal, final String initialsVal) {
        if (lastNameVal == null) {
            this.lastName = "";
        } else {
            this.lastName = ICBStringNormalizer.removeAccents(lastNameVal);
        }
        if (firstNameVal == null) {
            this.firstName = "";
        } else {
            this.firstName = ICBStringNormalizer.removeAccents(firstNameVal);
        }
        this.initials = makeInitials(this.firstName, ICBStringNormalizer.removeAccents(initialsVal));
    }

    /**
     * An author will full name such as "Michael J. Fox". Any part of the name may be missing.
     * This will parse to Firstname="Michael J", Initials="MJ", Lastname="Fox".
     * @param fullName the authors full name.
     */
    public Author(final String fullName) {
        final String[] parts = StringUtils.split(ICBStringNormalizer.removeAccents(fullName), ' ');
        final int partsLength = parts.length;
        if (partsLength == 0) {
            this.firstName = "";
            this.lastName = "";
            this.initials = "";
            return;
        }

        this.lastName = parts[partsLength - 1];
        if (parts.length == 1) {
            this.firstName = "";
            this.initials = "";
        } else {
            final StringBuilder firstName = new StringBuilder();
            for (int i = 0; i < partsLength - 1; i++) {
                // Removing trailing '.' from name parts
                final int length = parts[i].length();
                if (length == 0) {
                    continue;
                }
                if (firstName.length() != 0) {
                    firstName.append(" ");
                }
                if (parts[i].charAt(length - 1) == '.') {
                    firstName.append(parts[i].substring(0, length - 1));
                } else {
                    firstName.append(parts[i]);
                }
            }
            String firstNameStr = firstName.toString();
            while (firstName.indexOf("  ") != -1) {
                // Remove all double spaces
                firstNameStr = firstNameStr.replaceAll("  ", " ");
            }
            this.firstName = firstNameStr;
            this.initials = makeInitials(firstNameStr, null);
        }
        if (this.lastName.toLowerCase().contains("rosamund")) {
            System.out.println("!! Found questionable author " + toString());
        }
    }

    /**
     * Get the author's last name.
     * @return the
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Get the author's first name.
     * @return the author's first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Get the author's initials.
     * @return the author's initials
     */
    public String getInitials() {
        return initials;
    }

    /**
     * Given firstNameVal and initialsVal, return the initials for the author.
     * @param firstNameVal the first name to use as a fallback value for the initials
     * in the case that initialsVal is null or empty
     * @param initialsVal the authors initials
     * @return the authors initials
     */
    public static String makeInitials(final String firstNameVal, final String initialsVal) {
        if (initialsVal != null && initialsVal.length() > 0) {
            // We already have initials
            return initialsVal;
        }

        if (firstNameVal == null || firstNameVal.length() == 0) {
            return "";
        }

        final MutableString initialsCreated = new MutableString(2);
        final String[] names = StringUtils.split(firstNameVal, ' ');
        for (final String name : names) {
            if (name.length() > 0) {
                initialsCreated.append(name.charAt(0));
            }
        }
        initialsCreated.toUpperCase();
        return initialsCreated.toString();
    }

    /**
     * The text that represents this author, used for indexing.
     * @return the indexing text
     */
    public String getIndexableText() {
        final MutableString authorsIndexText = new MutableString();

        final Set<String> lastNamesList = new LinkedHashSet<String>();
        final Set<String> firstNamesList = new LinkedHashSet<String>();
        addNameVariantionsToList(lastNamesList, lastName.trim());
        addNameVariantionsToList(firstNamesList, initials.trim());
        addNameVariantionsToList(firstNamesList, firstName.trim());

        final Set<String> authorsSet = new LinkedHashSet<String>();
        // Lots of ways to format the first / last names for increased searchability
        addNamesToSet(authorsSet, lastNamesList, firstNamesList);

        if (authorsSet.size() > 0) {
            int i = 0;
            for (final String name : authorsSet) {
                if (i++ > 0) {
                    authorsIndexText.append(" : ");
                }
                authorsIndexText.append(name);
            }
            authorsIndexText.append(" | ");
        }

        return authorsIndexText.toString();
    }

    /**
     * Given the set of names add some variations.
     * @param nameList the name variations
     * @param name the name to add
     */
    private void addNameVariantionsToList(final Set<String> nameList, final String name) {
        if (name == null || name.length() == 0) {
            return;
        }
        final String noSpecials = indexableName(name);

        if (noSpecials.length() > 0) {
            nameList.add(noSpecials);
        }
    }

    /**
     * Append a first/last names with the given separators, using all combinations of each.
     * @param dest the set to add the name to
     * @param src1 the (first/last) names to append first
     * @param src2 the (first/last) names to append second
     */
    private void addNamesToSet(
            final Set<String> dest,
            final Set<String> src1,
            final Set<String> src2) {

        if (src1.size() == 0 && src2.size() == 0) {
            // Seems really unlikely
        } else if (src1.size() == 0) {
            // One of first/last names is empty
            dest.addAll(src2);
        } else if (src2.size() == 0) {
            // The other of first/last names is empty
            dest.addAll(src1);
        } else {
            // We have both first and last names
            for (final String src1item : src1) {
                for (final String src2item : src2) {
                    dest.add(String.format("%s %s", src1item, src2item));
                }
            }
        }
    }

    public static String indexableName(final String originalName) {
        final MutableString indexable = new MutableString(originalName);
        // Remove the indexing chars, not necessary
        for (final char indexChar : SPECIAL_INDEXING_CHARS) {
            indexable.replace(indexChar, ' ');
        }
        // Change hyphenated names to non-hyphenated
        for (final char indexChar : SPECIAL_OTHER_CHARS) {
            indexable.replace(indexChar, " ");
        }
        final int correctedToPos = indexable.indexOf(" [corrected to ");
        if (correctedToPos != -1) {
            indexable.length(correctedToPos);
        }
        // Remove double spaces
        while (indexable.indexOf("  ") != -1) {
            indexable.replace("  ", " ");
        }
        indexable.trim();
        return indexable.toString();

    }

    /**
     * Given a list of authors, reutrn the index text for the entire list.
     * @param authors the list of authors
     * @return the index string for all of them.
     */
    public static MutableString getAuthorsIndexText(final List<Author> authors) {
        if (authors == null) {
            return EMPTY_MUTABLE_STRING;
        }
        final MutableString authorsIndexText = new MutableString();
        for (final Author author : authors) {
            authorsIndexText.append(author.getIndexableText());
        }
        return authorsIndexText;
    }

    /**
     * Human readable version of this object.
     * @return this object as a human readable string
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[fn=").append(firstName).append(",");
        sb.append("i=").append(initials).append(",");
        sb.append("ln=").append(lastName).append("]");
        return sb.toString();
    }
}
