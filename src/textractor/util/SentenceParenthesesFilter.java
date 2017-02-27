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

package textractor.util;

import textractor.datamodel.TextractorDocument;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Filter sentences based on parentheses content. For sentences that do not
 * contain parentheses, the filter will always return false. When sentences
 * contain parentheses, the filter will return false only each these parenthesis
 * pair contain a number. The filter can be used to filter out sentences that do
 * not contain abbreviations. It will remove sentences that contain only
 * abbreviations.
 * Filter sentences based on parentheses content.
 * For sentences that do not contain parentheses, the filter will always return false. When sentences contain parentheses,
 * the filter will return false for those sentences with only numbers enclosed in parentheses (e.g., (1) (32)).
 * The filter can be used to filter out sentences that do not contain abbreviations. It will remove sentences
 * that contain only abbreviations.
 * User: Fabien Campagne
 * Date: Nov 7, 2004
 * Time: 10:08:59 AM
 */
public final class SentenceParenthesesFilter implements SentenceFilter, Serializable {
    private static final Pattern MATCHER =
            Pattern.compile("\\([ ]{0,1}([a-zA-Z]+[a-zA-Z0-9]*|[a-zA-Z0-9]*[a-zA-Z]+[a-zA-Z0-9]*)[ ]{0,1}\\)");

    /**
     * Create a new {@link textractor.util.SentenceFilter}
     */
    public SentenceParenthesesFilter() {
        super();
    }

    /**
     * Determine if a sentence should be removed from the index, based on the
     * text that it contains.
     *
     * @param charsequence Text of the sentence
     * @return True if the sentence should be removed from the index, False if
     * the sentence should be indexed.
     */
    public boolean filterSentence(final CharSequence charsequence) {
        final String text = charsequence.toString();
        final int openParenthesisIndex = text.indexOf('(');
        final int closingParenthesisIndex = text.lastIndexOf(')');

        if (openParenthesisIndex == -1 || closingParenthesisIndex == -1) {
            // the sentence does not contain open and closing parentheses
            return true;
        } else if (openParenthesisIndex > closingParenthesisIndex) {
            // parentheses do not match.
            return true;
        } else {
            // spend more time now to reject sentences that contain
            // only (1) or (10) reference like patterns
            int lastOpenIndex = 0;

            while (true) {
                final int currentOpenIndex = text.indexOf('(', lastOpenIndex);
                final int currentCloseIndex = text.indexOf(')', currentOpenIndex);
                if (currentCloseIndex == -1 || currentOpenIndex == -1) {
                    break;
                }

                if (MATCHER.matcher(text.substring(currentOpenIndex, currentCloseIndex+1)).matches()) {
                    return false;   // tried to match (p53) (23a) (2d2a)
                }

                lastOpenIndex = currentOpenIndex + 1;
            }
            return true; // assume that the rest is (1) (23) etc.
        }
    }

    public boolean filterSentence(final TextractorDocument document) {
        return filterSentence(document.getText());
    }
}
