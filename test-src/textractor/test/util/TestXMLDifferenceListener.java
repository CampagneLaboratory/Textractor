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

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.XMLTestCase;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;


/**
 * Set of tests used to validate custom DifferenceListeners for XMLUnit tests.
 */
public class TestXMLDifferenceListener extends XMLTestCase {
    /**
     * This test validates that two xml strings that contain the same structure
     * but different text values will be reported as differnent with the
     * IgnoreAttributeValuesDifferenceListener.
     *
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public void testXMLTextDifference() throws SAXException, IOException,
            ParserConfigurationException {
        // here the street address values are different
        final String expectedXML = "<location><street-address>22 any street</street-address><postcode>XY00 99Z</postcode></location>";
        final String actualXML = "<location><street-address>20 east cheap</street-address><postcode>EC3M 1EB</postcode></location>";
        final DifferenceListener differenceListener =
            new IgnoreAttributeValuesDifferenceListener();
        final Diff diff = new Diff(expectedXML, actualXML);
        diff.overrideDifferenceListener(differenceListener);

        // so the strings should not be considered the same
        assertXMLEqual(diff, false);
    }

    /**
     * This test validates that two xml strings that contain different structure
     * but the same values will be reported as differnent with the
     * IgnoreAttributeValuesDifferenceListener.
     *
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public void testXMLNodeNameDifference() throws SAXException, IOException,
            ParserConfigurationException {
        // here the street values are the same, but the actual xml uses
        // "zipcode" instead of "postcode"
        final String expectedXML = "<location><street-address>22 any street</street-address><postcode>XY00 99Z</postcode></location>";
        final String actualXML = "<location><street-address>22 any street</street-address><zipcode>XY00 99Z</zipcode></location>";
        final DifferenceListener differenceListener =
            new IgnoreAttributeValuesDifferenceListener();
        final Diff diff = new Diff(expectedXML, actualXML);
        diff.overrideDifferenceListener(differenceListener);

        // so the strings should not be considered the same
        assertXMLEqual(diff, false);
    }

    /**
     * This test validates that two xml strings that contain the same structure
     * and text values but have differences in just the attributes of some nodes
     * will be reported as being the same with the
     * IgnoreAttributeValuesDifferenceListener.
     *
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public void testXMLAtrributeDifference() throws SAXException, IOException,
            ParserConfigurationException {
        // here the nodes and text are all the same but the difference
        // is with the attribute id of the location
        final String expectedXML = "<location id=\"42\"><street-address>22 any street</street-address><postcode>XY00 99Z</postcode></location>";
        final String actualXML = "<location id=\"647\"><street-address>22 any street</street-address><postcode>XY00 99Z</postcode></location>";
        final DifferenceListener differenceListener =
            new IgnoreAttributeValuesDifferenceListener();
        final Diff diff = new Diff(expectedXML, actualXML);
        diff.overrideDifferenceListener(differenceListener);

        // so the strings should be considered the same
        assertXMLEqual(diff, true);
    }

    /**
     * This test validates that two xml strings that contain the same structure
     * and text values but the actual XML added a new and unexpected attribute
     * and should be reported as different with the
     * IgnoreAttributeValuesDifferenceListener.
     *
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public void testXMLUnexpectedAtrribute() throws SAXException, IOException,
            ParserConfigurationException {
        // here all the nodes and text are the same, but the expected value
        // added an attribute "type" to the postcode
        final String expectedXML = "<location><street-address>22 any street</street-address><postcode>XY00 99Z</postcode></location>";
        final String actualXML = "<location><street-address>22 any street</street-address><postcode type=\"zip\">XY00 99Z</postcode></location>";
        final DifferenceListener differenceListener =
            new IgnoreAttributeValuesDifferenceListener();
        final Diff diff = new Diff(expectedXML, actualXML);
        diff.overrideDifferenceListener(differenceListener);

        // so the strings should be considered the same
        assertXMLEqual(diff, false);
    }
}
