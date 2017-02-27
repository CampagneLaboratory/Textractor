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

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import textractor.acronyms.xml.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class ClippedExpansionXMLWriter {
    private String outputFilename;
    private XMLSerializer xmlSerializer;

    public ClippedExpansionXMLWriter(final String newOutputFilename) {
        super();
        this.outputFilename = newOutputFilename;
    }

    public void writeXML(final Map<String, Set<ExpansionTerm>> expansionMap)
        throws SAXException, IOException, MarshalException, ValidationException {
        FileWriter outputFileWriter = null;
        if (outputFilename != null) {
            outputFileWriter = new FileWriter(outputFilename);
            final PrintWriter output = new PrintWriter(outputFileWriter);
            final OutputFormat outputFormat = new OutputFormat();
            outputFormat.setIndenting(true);
            xmlSerializer = new XMLSerializer(output, outputFormat);
        }

        xmlSerializer.startDocument();
        xmlSerializer.comment("Comment");
        xmlSerializer.startElement("", "", "acronyms", new AttributesImpl());

        for (final String shortForm : expansionMap.keySet()) {
            addExpansionTermToOutput(shortForm, expansionMap.get(shortForm));
        }

        xmlSerializer.endElement("acronyms");
        xmlSerializer.endDocument();

        if (outputFileWriter != null) {
            outputFileWriter.close();
        }
    }


    private void addExpansionTermToOutput(final String shortForm,
            final Set<ExpansionTerm> expansions)
        throws MarshalException, ValidationException, IOException {
        if (expansions.size() > 0) {
            final Acronym acronym = new Acronym();
            acronym.setShortForm(new ShortForm());
            acronym.getShortForm().setContent(shortForm);
            acronym.getShortForm().setFrequency(1);

            acronym.setExpansions(new Expansions());

            for (final ExpansionTerm expansion : expansions) {
                final ExpansionsItem item = new ExpansionsItem();
                item.setExpansion(new Expansion());
                item.getExpansion().setContent(expansion.getTerm());
                item.getExpansion().setFrequency(
                        (int) expansion.getFrequency());

                acronym.getExpansions().addExpansionsItem(item);
            }

            acronym.marshal(xmlSerializer);
        }
    }


    public void setOutputFilename(final String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public String getOutputFilename() {
        return outputFilename;
    }
}
