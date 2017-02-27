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

import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.BulletParser;
import it.unimi.dsi.mg4j.util.parser.Element;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;
import textractor.datamodel.OmimArticle;

/**
 * Class used to parse xml of an OMIM document. When parsing is complete
 * it will use
 * @author Kevin Dorff
 */
public class OmimExtractor extends SimplifiedDefaultCallback {

    /** This will skip mim types 1 (star) and 4 (plus). */
    private static final int[] MIM_SKIP_TYPES = {1, 4};

    /**
     * If true, the current document will not be passed
     * to parsedArticleHandler.
     */
    private boolean skipCurrentDocument;

    /**
     * Set to true to not read the abstract text.
     * Only set this if you are sure you know what you are doing.
     */
    private boolean skipAbstracts;

    /**
     * The current article being parsed from XML.
     */
    private OmimArticle article;

    /**
     * The current abstract text for the current
     * article.
     */
    private final MutableString abstractText;

    /**
     * The XML elements being parsed.
     */
    private Element mimNumberElement;
    private Element titleElement;
    private Element aliasElement;
    private Element textElement;
    private Element textLabelElement;
    private Element pubmedIdElement;
    private Element mimEntryElement;
    private Element mimTypeElement;

    /**
     * The class which we will call back to when we have
     * sucessfully parsed an Article that we want to keep.
     */
    private final ParsedArticleHandler parsedArticleHandler;

    /**
     * Configure the class setting the parsedArticleHandler class,
     * this is the normal constructor to use.
     * @param parsedArticleHandler the parsedArticleHandler that will
     * be accepting the parsed xml result.
     */
    public OmimExtractor(final ParsedArticleHandler parsedArticleHandler) {
        super();
        this.abstractText = new MutableString();
        this.skipCurrentDocument = false;
        this.skipAbstracts = false;
        this.parsedArticleHandler = parsedArticleHandler;
    }

    /**
     * Set if abstracts should be parsed. Do NOT change this unless
     * you are sure about what you are doing.
     * @param skipAbstracts set if abstracts should be parsed
     */
    public void setSkipAbstracts(final boolean skipAbstracts) {
        this.skipAbstracts = skipAbstracts;
    }

    /**
     * Initialize the state between parsed documents.
     */
    @Override
    protected void reset() {
        article = new OmimArticle();
        article.setFilename(filename);
        abstractText.length(0);
        skipCurrentDocument = false;
    }

    /**
     * We have reached the end element. We can now handle the
     * text we collected since the start element. Please it into
     * the article object we are building.
     */
    @Override
    protected void processElement(final XmlElement xmlElement) {
        if (xmlElement.element == pubmedIdElement) {
            if (xmlElement.elementText.length() > 0) {
                final int pubmedIdInt = NumberUtils.toInt(xmlElement.elementText.toString());
                article.getRefPmids().add(pubmedIdInt);
            }
        } else if (xmlElement.element == mimTypeElement) {
            if (xmlElement.elementText.length() > 0) {
                final int mimTypeNum = Integer.parseInt(xmlElement.elementText.toString());
                skipCurrentDocument = ArrayUtils.contains(MIM_SKIP_TYPES, mimTypeNum);
            }
        } else if (xmlElement.element == aliasElement) {
            if (xmlElement.elementText.length() > 0) {
                article.getAliases().add(xmlElement.elementText.toString());
            }
        } else if (xmlElement.element == textElement) {
            if (!skipAbstracts) {
                abstractText.append(xmlElement.elementText);
                if (abstractText.length() > 0 && !abstractText.endsWith(" ")) {
                    abstractText.append(" ");
                }
            }
        } else if (xmlElement.element == textLabelElement) {
            if (!skipAbstracts) {
                abstractText.append(xmlElement.elementText);
                abstractText.append(". ");
            }
        } else if (xmlElement.element == titleElement) {
            if (xmlElement.elementText.length() > 0) {
                article.setTitle(xmlElement.elementText.toString());
            }
            if (xmlElement.elementText.length() == 0) {
                skipCurrentDocument = true;
            }
            if (xmlElement.elementText.startsWith("MOVED TO ")) {
                skipCurrentDocument = true;
            }
            if (xmlElement.elementText.equals("REMOVED FROM DATABASE")) {
                skipCurrentDocument = true;
            }
        } else if (xmlElement.element == mimNumberElement) {
            if (xmlElement.elementText.length() > 0) {
                article.setPmid(NumberUtils.toInt(xmlElement.elementText.toString()));
            }
        } else if (xmlElement.element == mimEntryElement) {
            if (!skipCurrentDocument) {
                if (skipAbstracts || abstractText.length() > 0) {
                    article.setFilename(filename);
                    parsedArticleHandler.articleParsed(article, abstractText.copy());
                }
            }
            // We're done with the current OmimArticle. Be ready to
            // start a new one.
            reset();
        }
    }

    /**
     * Specify the elements we want to parse from the XML file.
     */
    @Override
    public void configure(final BulletParser parser) {
        parser.parseText(true);
        parser.parseTags(true);
        parser.parseAttributes(false);

        mimNumberElement = parser.factory.getElement(
                new MutableString("Mim-entry_mimNumber".toLowerCase()));
        titleElement = parser.factory.getElement(
                new MutableString("Mim-entry_title".toLowerCase()));
        aliasElement = parser.factory.getElement(
                new MutableString("Mim-entry_aliases_E".toLowerCase()));
        textElement = parser.factory.getElement(
                new MutableString("Mim-text_text".toLowerCase()));
        textLabelElement = parser.factory.getElement(
                new MutableString("Mim-text_label".toLowerCase()));
        pubmedIdElement = parser.factory.getElement(
                new MutableString("Mim-reference_pubmedUID".toLowerCase()));
        mimEntryElement = parser.factory.getElement(
                new MutableString("Mim-entry".toLowerCase()));
        mimTypeElement = parser.factory.getElement(
                new MutableString("Mim-entry_mimType".toLowerCase()));
    }
}
