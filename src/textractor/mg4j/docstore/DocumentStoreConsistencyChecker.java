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

package textractor.mg4j.docstore;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;

public final class DocumentStoreConsistencyChecker implements Closeable {
    private DocumentIndexManager docmanager;
    private final DocumentStoreReader reader;

    public DocumentStoreConsistencyChecker(final DocumentIndexManager documentIndexManager) throws IOException {
        docmanager = documentIndexManager;
        reader = new DocumentStoreReader(docmanager);
    }

    public static void main(final String[] args) throws NoSuchMethodException, IllegalAccessException, ConfigurationException, IOException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        final DocumentIndexManager docmanagerToTest =
                new DocumentIndexManager(args[0]);
        final DocumentStoreConsistencyChecker checker =
                new DocumentStoreConsistencyChecker(docmanagerToTest);
        final boolean found = checker.checkConsisentcyWithSearchTerm("kinase");

        if (found) {
            System.out.println("Consistency check passed");
        } else {
            System.out.println("Consistency check failed");
        }
    }

    public boolean checkConsisentcyWithSearchTerm(final String term) throws IOException {
        final List<Integer> intResult = new IntArrayList();
        final int[] resultIndices = docmanager.query(term);
        boolean found = false;

        for (int n = 0; n < resultIndices.length; n++) {
            reader.document(n, intResult);
            for (final int index : intResult) {
                if (docmanager.termAsString(index).equals(term)) {
                    found = true;
                }
            }
        }
        return found;
    }

    public void setDocmanager(final DocumentIndexManager newManager) {
        docmanager = newManager;
    }

    public DocumentIndexManager getDocmanager() {
        return docmanager;
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public void close() throws IOException {
        reader.close();
    }
}
