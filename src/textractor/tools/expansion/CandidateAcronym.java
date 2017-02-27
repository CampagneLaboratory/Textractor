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

package textractor.tools.expansion;

import java.util.StringTokenizer;

/**
 * User: Fabien Campagne
 * Date: Nov 6, 2004
 * Time: 2:44:29 PM
 */
public final class CandidateAcronym {
    private String content;
    private int nucleusFrequency;

    /**
     *
     * @return The number of documents skipped during expansion.
     */
    public int getSkipCount() {
        return skipCount;
    }

    // number of documents skipped:
    private int skipCount;

    /**
     * Returns a string representation of the object. In general, the
     * <code>toString</code> method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p/>
     * The <code>toString</code> method for class <code>Object</code>
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `<code>@</code>', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return content;
    }

    public CandidateAcronym(final String content) {
        this.content = content.trim();
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public int getNucleusFrequency() {
        return nucleusFrequency;
    }

    public void setNucleusFrequency(final int nucleusFrequency) {
        this.nucleusFrequency = nucleusFrequency;
    }

    public int getNumberOfTerms() {
        final StringTokenizer st = new StringTokenizer(this.content, " ");
        return st.countTokens();
    }

    public void skipped(final int skipCount) {
        this.skipCount += skipCount;
    }
}
