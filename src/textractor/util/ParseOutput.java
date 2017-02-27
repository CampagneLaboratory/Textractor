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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import textractor.database.DocumentIndexManager;
import textractor.datamodel.Sentence;
import textractor.datamodel.TermOccurrence;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;

public abstract class ParseOutput {
    static final Map<String, Integer> articleTermsMap = new Object2IntOpenHashMap<String>();

    protected static void accumulate(final String term) {
        if (!articleTermsMap.containsKey(term)) {
            articleTermsMap.put(term, 1);
        } else {
            articleTermsMap.put(term, 1 + articleTermsMap.get(term));
        }
    }

    protected static void outputTabular(final DocumentIndexManager docmanager,
            final String id, final Map<String, Integer> articleTermsMap,
            final Writer writer) throws IOException {
        final TermOccurrence[] lookupedTermOccurrences =
            convertToTermOccurrence(articleTermsMap);
        String term;
        for (final TermOccurrence lookupedTermOccurrence : lookupedTermOccurrences) {
            term = lookupedTermOccurrence.getTerm();
            final Sentence termTemp = new Sentence();
            termTemp.setText(term);
            final String indexFormatedName =
                    termTemp.getSpaceDelimitedProcessedTerms(docmanager).toString();

            writer.write(id + '\t' + term + '\t' + indexFormatedName + '\t'
                    + lookupedTermOccurrence.getCount() + '\n');
        }
    }

    public static TermOccurrence[] convertToTermOccurrence(final Map<String, Integer> articleTermsMap) {
        final TermOccurrence[] lookupedTermOccurrences =
            new TermOccurrence[articleTermsMap.size()];

        int count = 0;
        for (final String term : articleTermsMap.keySet()) {
            lookupedTermOccurrences[count++] =
                    new TermOccurrence(term, null, articleTermsMap.get(term));
        }

        Arrays.sort(lookupedTermOccurrences);
        return lookupedTermOccurrences;
    }
}
