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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public final class ExpansionAnalyser {
    private static final Log LOG = LogFactory.getLog(ExpansionAnalyser.class);
    private Map<String, List<ExpansionTerm>> expansionMap;
    private final List<RejectedExpansion> rejectedExpansions;
    private String description;

    public ExpansionAnalyser() {
        super();
        expansionMap = new HashMap<String, List<ExpansionTerm>>();
        rejectedExpansions = new ArrayList<RejectedExpansion>();
    }

    public ExpansionAnalyser(final String filename)
        throws IOException, SAXException {
        this();
        loadExpansions(filename);
        setDescription(filename);
    }

    public ExpansionAnalyser(final String filename, final String newDescription)
        throws IOException, SAXException {
        this(filename);
        setDescription(newDescription);
    }


    public void loadRejectedExpansions(final String filename)
        throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader(filename));
        String acronymContent;
        while ((acronymContent = br.readLine()) != null) {
            rejectedExpansions.add(new RejectedExpansion(acronymContent));
        }
    }

    public void loadExpansions(final String filename) throws IOException, SAXException {
        LOG.debug("Loading expansions: " + filename);
        final SAXParser parser = new SAXParser();
        final ExpansionHandler handler = new ExpansionHandler();
        parser.setContentHandler(handler);
        parser.parse(filename);
        expansionMap = handler.getExpansionMap();
    }

    public void echoBasicStatistics() {
        System.out.println("Basic statistics for expansion: "
                + getDescription());
        System.out.println("-> Acronyms expanded: "
                + getExpandedAcronymCount());
        System.out.println("-> Long forms collected: "
                + getLongFormCollectionCount());
    }

    public int getExpandedAcronymCount() {
        return expansionMap.keySet().size();
    }

    public int getAllExpansionsCount() {
        return rejectedExpansions.size() + expansionMap.keySet().size();
    }

    public int getLongFormCollectionCount() {
        final Iterator<String> abbreviationIterator =
            expansionMap.keySet().iterator();
        int count = 0;
        while (abbreviationIterator.hasNext()) {
            final String iteratedAbbreviation = abbreviationIterator.next();
            count += expansionMap.get(iteratedAbbreviation).size();
        }

        return count;
    }

    public static void main(final String[] args) throws IOException, SAXException {
        final ExpansionAnalyser analyser1 = new ExpansionAnalyser(args[0], args[1]);
        final ExpansionAnalyser analyser2 = new ExpansionAnalyser(args[2], args[3]);
        analyser1.loadRejectedExpansions(args[4]);
        analyser2.loadRejectedExpansions(args[4]);

        System.out.println("Comparing: " + analyser1 + " and " + analyser2);

        analyser1.echoBasicStatistics();
        analyser2.echoBasicStatistics();

        analyser1.compareAcronymsWith(analyser2);
        analyser1.compareLongFormsWith(analyser2);

    }

    public void compareLongFormsWith(final ExpansionAnalyser analyser) {
    }

    public void compareAcronymsWith(final ExpansionAnalyser analyser) {
        int posPosCount = 0;
        int posNegCount = 0;
        int negPosCount = 0;
        final int negNegCount = 0;

        Iterator<String> acronymIterator = getAcronymIterator();
        while (acronymIterator.hasNext()) {
            final String acronym = acronymIterator.next();
            if (analyser.expansionsContainAcronym(acronym)) {
                posPosCount++;
            } else {
                posNegCount++;
            }
        }


        acronymIterator = analyser.getAcronymIterator();

        while (acronymIterator.hasNext()) {
            final String acronym = acronymIterator.next();
            if (!expansionsContainAcronym(acronym)) {
                negPosCount++;
            }
        }

        System.out.println("Positive, positive: " + posPosCount);
        System.out.println("Positive, Negative: " + posNegCount);
        System.out.println("Negative, positive: " + negPosCount);
        System.out.println("Negative, negative: " + negNegCount);

    }

    public boolean expansionsContainAcronym(final String acronym) {
        return expansionMap.keySet().contains(acronym);
    }

    public Iterator<String> getAcronymIterator() {
        return expansionMap.keySet().iterator();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
