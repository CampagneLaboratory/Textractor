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

package textractor.chain.loader;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.Attribute;
import it.unimi.dsi.mg4j.util.parser.Element;
import it.unimi.dsi.mg4j.util.parser.callback.DefaultCallback;

import java.util.Map;

/**
 * Simplifies creating an MG4J BulletParser parser for
 * XML files which specify one or more articles/documents.
 * A class is created that supplised the
 * following methods configure(), reset(), processElement().
 * The rest is handled by this class.
 * @author Kevin Dorff (Sep 18, 2007)
 */
public abstract class SimplifiedDefaultCallback extends DefaultCallback {
    /**
     * The parsing stack. We are currently parsing the "top" of this
     * stack. As an element starts it will be pushed on here
     * and as it finished it will be popped from here.
     */
    private final Stack<XmlElement> elementStack;

    /**
     * The current line while parsing. We use this so we don't have
     * to re-create an object over and over when parsing XML characters.
     */
    private final MutableString currentText;

    private static final MutableString NO_BODY = new MutableString();

    /**
     * The filename being proessed.
     */
    protected String filename;

    /**
     * Configure the class.
     */
    public SimplifiedDefaultCallback() {
        // We NEED parsedArticleHandler to perofrm this task
        // so this constructor isn't publically accessible.
        super();
        elementStack = new ObjectArrayList<XmlElement>();
        this.filename = null;
        this.currentText = new MutableString();
    }

    /**
     * Start parsing a new xml file.
     * @param filename the filename to parse.
     */
    public void resetExtractor(final String filename) {
        this.filename = filename;
        reset();
    }

    /**
     * Process (more) characters between start and
     * end elements.
     * @param characters the characters
     * @param offset the fileSize of the start of the charactesr
     * @param length the length of characters
     * @param flowBroken true if flow is broken
     * @return always returns true
     */
    @Override
    public boolean characters(final char[] characters, final int offset,
            final int length, final boolean flowBroken) {
        if (!elementStack.isEmpty()) {
            // The current characters
            currentText.length(0);
            currentText.append(characters, offset, length);

            // Add it to the text for the current element
            final XmlElement xmlElement = elementStack.top();
            if (xmlElement.elementText.length() == 0) {
                // If this is the first text for the element, trim the
                // left of the text
                currentText.trimLeft();
                xmlElement.elementText.append(currentText);
            }
        }
        return true;
    }

    /**
     * We have reached the end element. If it is an element
     * we are interested in we can start collecting text
     * for that element.
     * @param element the element being pared
     * @param attrMap the attributes map for the element
     */
    @Override
    public final boolean startElement(final Element element,
            final Map<Attribute,MutableString> attrMap) {
        final XmlElement xmlElement = new XmlElement(element, attrMap);
        elementStack.push(xmlElement);
        return true;
    }

    /**
     * We have reached the end element. We can now handle the
     * text we collected since the start element. Please it into
     * the article object we are building.
     */
    @Override
    public boolean endElement(final Element element) {
        XmlElement xmlElement;
        assert !elementStack.isEmpty();
        while (true) {
            xmlElement = elementStack.pop();
            final MutableString elementText = xmlElement.elementText;
            elementText.trimRight();
            processElement(xmlElement);
            if (xmlElement.element == element) {
                // This is the normal case, bome elements like
                // <tag att="something"/> will never get popped
                // by BulletParser. Here they can be processed
                // like any other element.
                break;
            }
        }

        return true;
    }

    @Override
    public void startDocument() {
        assert elementStack.isEmpty() : "Stack should be empty";
        reset();
    }

    @Override
    public void endDocument() {
        assert elementStack.isEmpty() : "Stack should be empty";
    }

    /**
     * This method is called to reset the state of the
     * parser between documents.
     */
    protected abstract void reset();

    /**
     * When an element has been parsed, this method will be called
     * providing the element that has been parsed, the text for
     * the element, and the attributes on that element.
     */
    protected abstract void processElement(final XmlElement xmlElement);
}
