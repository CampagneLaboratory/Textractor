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

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.w3c.dom.Node;

/**
 * Class to use when performing a Diff that only compares the structure and
 * text of 2 pieces of XML, i.e. where the attribute nodes should be ignored.
 * @see org.custommonkey.xmlunit.Diff#overrideDifferenceListener
 */
public class IgnoreAttributeValuesDifferenceListener
implements DifferenceListener {
    private static final int[] IGNORE_VALUES = new int[] {
        DifferenceConstants.ATTR_VALUE.getId(),
        DifferenceConstants.ATTR_VALUE_EXPLICITLY_SPECIFIED.getId()
    };

    private boolean isIgnoredDifference(final Difference difference) {
        final int differenceId = difference.getId();
        for (int i=0; i < IGNORE_VALUES.length; ++i) {
            if (differenceId == IGNORE_VALUES[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR to ignore
     *  differences in values of TEXT or ATTRIBUTE nodes,
     *  and RETURN_ACCEPT_DIFFERENCE to accept all other
     *  differences.
     * @see org.custommonkey.xmlunit.DifferenceListener#differenceFound(Difference)
     */
    public int differenceFound(final Difference difference) {
        if (isIgnoredDifference(difference)) {
            return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
        } else {
            return RETURN_ACCEPT_DIFFERENCE;
        }
    }

    /**
     * Do nothing
     * @see DifferenceListener#skippedComparison(Node, Node)
     */
    public void skippedComparison(final Node control, final Node test) {
    }
}
