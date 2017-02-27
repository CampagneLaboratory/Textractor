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

package textractor.parsers;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.Attribute;
import it.unimi.dsi.mg4j.util.parser.BulletParser;
import it.unimi.dsi.mg4j.util.parser.Element;
import it.unimi.dsi.mg4j.util.parser.ParsingFactory;
import it.unimi.dsi.mg4j.util.parser.callback.DefaultCallback;
import static org.apache.commons.collections.MapUtils.EMPTY_MAP;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.TextractorConstants;
import textractor.TextractorException;
import textractor.datamodel.Author;
import textractor.sentence.SentenceProcessingException;
import textractor.util.ICBMutableStringUtils;

import java.io.IOException;
import java.util.*;

/**
 * Pumbed / medline XML parser.
 */
public abstract class PubmedExtractor extends DefaultCallback {

    /** An empty mutable string. */
    private final static MutableString EMPTY_MUTABLE_STRING = new MutableString(0);

    /** Used to log debug and informational messages. */
    private static final Log LOG = LogFactory.getLog(PubmedExtractor.class);

    /**
     * Special for endElement().
     * Comments corrections which should include retraction information.
     * Will get used to save retractions in {@link #characters(char[], int, int, boolean)}.
     * NOTE: there is a special check for the "RetractionOf" atttribute.  This may need
     * to change if at some point we need to parse other infomation from comments corrections.
     */
    private static final String RETRACTIONOF_TAG_PATH =
            "medlinecitationset.medlinecitation.commentscorrectionslist.commentscorrections.pmid";

    /**
     * Special for endElement().
     * The pubmed article tag path.
     */
    private static final String PUBMED_ARTICLE_TAG_PATH =
            "medlinecitationset.medlinecitation";

    /**
     * Special for endElement().
     * The author tag path, used to save each author.
     */
    private static final String AUTHOR_TAG_PATH =
            "medlinecitationset.medlinecitation.article.authorlist.author";

    private static final Map<String, String> XML_PATH_TO_FIELD_MAP;
    static {
        XML_PATH_TO_FIELD_MAP = new HashMap<String, String>();
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.article.abstract.abstracttext", "abstract");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.article.articletitle", "title");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.pmid", "pmid");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.datecreated.year", "date-year");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.datecreated.month", "date-month");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.datecreated.day", "date-day");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.article.authorlist.author.lastname",
                "author-lastname");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.article.authorlist.author.firstname",
                "author-firstname");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.article.authorlist.author.forename",
                "author-forename");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.article.authorlist.author.initials",
                "author-initials");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.article.journal.title",
                "journal-title");
        XML_PATH_TO_FIELD_MAP.put(
                "medlinecitationset.medlinecitation.article.journal.isoabbreviation",
                "journal-isoabbr");
        XML_PATH_TO_FIELD_MAP.put(RETRACTIONOF_TAG_PATH, "retractionof");
    }

    /**
     * Conversion of journal "journal-title" or "journal-isoabbr" to other versions
     * to include in the index.
     */
    private static final Map<MutableString, MutableString> JOURNAL_NAME_ALTERNATIVES_MAP;
    static {
        JOURNAL_NAME_ALTERNATIVES_MAP = new HashMap<MutableString, MutableString>();
    }

    /** Map of the nested tag to field we will store data in singleArticleFieldsMap. */
    private final Map<Deque<Element>, String> nestedTagToFieldMap =
            new Object2ObjectOpenHashMap<Deque<Element>, String>();

    /** Elements to translate to other elements. */
    private final Map<Element, Element> elementTranslationsMap =
            new Object2ObjectOpenHashMap<Element, Element>();

    /**
     * The element to ignore (may be placed as a value in the elementTranslationsMap
     * to have the parser ignore elements, ie, to NOT place them on elementStack.).
     */
    private Element elementIgnore;

    /**
     * The "CommentsCorrections" element used to check for retractions.
     */
    private Attribute refTypeAttribute;

    /** The tag path list we will be at in endElement where for processing an author. */
    private Deque<Element> authorEndTagList;

    /** The tag path list we will be at in endElement for processing retractionof. */
    private Deque<Element> retractionEndTagList;

    /** The tag path list we will be at in endElement to save the current article. */
    private Deque<Element> articleEndTagList;

    /**
     * The current element stack. This isn't a {@link java.util.Stack} or an
     * {@link it.unimi.dsi.fastutil.objects.ObjectArrayList} implementation of a stack
     * because I wanted, for performance reasons, to push/pop elements from the HEAD of
     * the list, not from the tail. The reason for this is I believe this will make
     * comparisons faster and and lookups from {@link #nestedTagToFieldMap} faster.
     */
    private final Deque<Element> elementStack = new LinkedList<Element>();

    /**
     * A parallel stack containing the attributes corresponding to elements in
     * {@link #elementStack}.  Note that it was not combined into a single list since
     * comparisons are done frequently on the element stack.  In general, we don't care
     * and don't add the overhead of the comparisons.
     */
    private final Deque<Map<Attribute, String>> attributeStack =
            new LinkedList<Map<Attribute, String>>();

    /**
     * As we parse a single article, this map will hold the values
     * (will be reset in {@link #reset()}.
     */
    private final Map<String, MutableString> singleArticleFieldsMap =
            new Object2ObjectOpenHashMap<String, MutableString>();

    /**
     * The list of retracted PMIDs for the current document (will be reset in {@link #reset()}.
     */
    private final List<String> retractedPmids = new ArrayList<String>();

    /**
     * The list of authors for the current document (will be reset within {@link #reset()}.
     */
    private final List<Author> authors = new ArrayList<Author>();

    /** The parser we are using. */
    private final ICBMutableStringUtils mutableStringUtil = new ICBMutableStringUtils();


    /**
     * Populate {@link #nestedTagToFieldMap}.
     * @param factory The parsing factory used by the parser.
     */
    private void populateNestedTagToFieldMap(final ParsingFactory factory) {
        // Tag paths we want to save and the field within nestedTagToFieldMap where we
        // want to save the information. Will be used in characters().
        for (final Map.Entry<String, String> entry : XML_PATH_TO_FIELD_MAP.entrySet()) {
            nestedTagToFieldMap.put(makeList(factory, entry.getKey()), entry.getValue());
        }

        // The synthetic "ignore" element, can be associated with specific
        // elements in elementTranslationsMap so cause elements to not be placed
        // on the elementStack
        elementIgnore = factory.getElement(new MutableString("ignore-element"));

        // store the comments corrections specific attribute that contains retraction information
        refTypeAttribute = factory.getAttribute(new MutableString("reftype"));

        // Element translations, to support a similar set of documents
        // Any element in this map that is associated with elementIgnore
        // will NOT be placed on elementStack when parsing. Key Elements in this map
        // will become value elements during startElement and endElement, so they
        // are equivalent in the case of XML documents that are very similar
        // but specific nodes are named differently.
        elementTranslationsMap.put(
                factory.getElement(new MutableString("pubmedarticleset")),
                factory.getElement(new MutableString("medlinecitationset")));
        elementTranslationsMap.put(
                factory.getElement(new MutableString("pubmedarticle")), elementIgnore);

        // Special case tag paths, which be used in endElement()
        authorEndTagList = makeList(factory, AUTHOR_TAG_PATH);
        retractionEndTagList = makeList(factory, RETRACTIONOF_TAG_PATH);
        articleEndTagList = makeList(factory, PUBMED_ARTICLE_TAG_PATH);
    }

    /**
     * No longer supported. If you find a use for this, please modify
     * the class to support it, but I couldn't tell that it was necessary
     * any longer, so I modified this class to be more readable but that
     * precludes the use of this property.
     * @param dummy ignored value
     */
    @Deprecated
    public void setArticleElementName(final String dummy) {
        LOG.error(String.format("setArticleElementName([%s]) is no longer supported.", dummy));
    }

    /**
     * Take a "." separated tag path and convert it to a list of elements.
     * The list will be a FILO list, in the reverse order so the string
     * "A.B.C.D" will return a list populated (D, C, B, A) as that should
     * be faster to compare against in the non-matching cases.
     * @param factory The parsing factory used by the parser.
     * @param tagPath the "." separated xml tag nexting list
     * @return the List[element] for the tagPath
     */
    private Deque<Element> makeList(final ParsingFactory factory, final String tagPath) {
        final LinkedList<Element> result = new LinkedList<Element>();
        final String[] parts = StringUtils.split(tagPath, ".");
        for (String part : parts) {
            result.push(factory.getElement(new MutableString(part)));
        }
        return result;
    }

    /**
     * Create the parser. Make sure we have an empty value for all known
     * fields within {@link #singleArticleFieldsMap}.
     */
    protected PubmedExtractor() {
        super();
    }

    /**
     * Configure the parser to parse text.
     * @param parser parser to configure
     */
    @Override
    public final void configure(final BulletParser parser) {
        parser.parseText(true);
        parser.parseTags(true);

        // tell the parser to extract any attributes called "reftype"
        // CommentsCorrections with a RefType="RetractionOf" contain subelements
        // that indicate an article was retracted
        parser.parseAttributes(true);
        final ParsingFactory factory = parser.factory;
        parser.parseAttribute(factory.getAttribute(new MutableString("reftype")));

        populateNestedTagToFieldMap(factory);

        // Initialize all known fields to empty
        for (String field : nestedTagToFieldMap.values()) {
            singleArticleFieldsMap.put(field, new MutableString());
        }
    }

    /**
     * Start of document.
     */
    @Override
    public final void startDocument() {
        assert elementStack.isEmpty() : "Element stack should be empty";
        assert attributeStack.isEmpty() : "Attribute stack should be empty";
        reset();
    }

    /**
     * End of document.
     */
    @Override
    public void endDocument() {
        assert elementStack.isEmpty() : "Element stack should be empty";
        assert attributeStack.isEmpty() : "Attribute stack should be empty";
    }

    /**
     * The trimmed value of the text we have received. We use this
     * to check if we received any text worth noting.
     */
    private final MutableString trimStr = new MutableString();

    /**
     * Received XML characters. If they are for an element we are interested in,
     * save them to the singleArticleFieldsMap for saving later.
     * @param characters the characters
     * @param offset the offset of the characters we are interested in
     * @param length the length of the characters we are interested in
     * @param flowBroken true of flow is broken
     * @return true
     */
    @Override
    public final boolean characters(final char[] characters, final int offset,
                                    final int length, final boolean flowBroken) {
        // Get the characters
        trimStr.length(0);
        trimStr.append(characters, offset, length);
        trimStr.trim();
        if (trimStr.length() > 0) {
            final String field = nestedTagToFieldMap.get(elementStack);
            if (field != null) {
                // The normal case, add the value to the singleArticleFieldsMap map
                MutableString fieldValue = singleArticleFieldsMap.get(field);
                if (fieldValue == null) {
                    fieldValue = new MutableString();
                    singleArticleFieldsMap.put(field, fieldValue);
                }
                // Add all characters, no trimming, etc. We'll clean stuff up later
                fieldValue.append(characters, offset, length);
            }
        }
        return true;
    }

    /**
     * Clear the current document.
     */
    private void reset() {
        // Initialize all fields in the map are empty (and exist)
        for (String field : singleArticleFieldsMap.keySet()) {
            MutableString fieldValue = singleArticleFieldsMap.get(field);
            if (fieldValue == null) {
                // Generally should not be necessary, but just in case
                fieldValue = new MutableString();
                singleArticleFieldsMap.put(field, fieldValue);
            } else {
                fieldValue.length(0);
            }
        }
        authors.clear();
        retractedPmids.clear();
    }

    /**
     * We have found an XML end-element tag.
     * @param elementOrig the current element
     * @param attributes attributes map for this element
     * @return true
     */
    @Override
    public final boolean startElement(final Element elementOrig,
                                      final Map<Attribute, MutableString> attributes) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("element: " + elementOrig + ", " + attributes);
        }
        final Element element = translateElement(elementOrig);
        if (element == null) {
            return true;
        }
        elementStack.push(element);

        // store the attributes locally - since this is a callback, we can't store the map directly
        // since we only ask for the attributes we really need it should be empty most of the time
        final Map<Attribute, String> localAttributes;
        if (!attributes.isEmpty()) {
            localAttributes = new Reference2ObjectOpenHashMap<Attribute, String>(attributes.size());
            for (Map.Entry<Attribute, MutableString> entry : attributes.entrySet()) {
                localAttributes.put(entry.getKey(), entry.getValue().toString());
            }
        } else {
            localAttributes = EMPTY_MAP;
        }
        attributeStack.push(localAttributes);
        return true;
    }

    /**
     * We have found an XML end-element tag.
     * @param elementOrig the current element
     * @return true
     */
    @Override
    public final boolean endElement(final Element elementOrig) {
        final Element element = translateElement(elementOrig);
        if (element == null) {
            return true;
        }
        if (element == authorEndTagList.peekFirst() && elementStack.equals(authorEndTagList)) {
            // Special case, closing the "author" element. Add the author name to the list
            appendAuthorNames();
        } else if (element == retractionEndTagList.peekFirst()
                && elementStack.equals(retractionEndTagList)) {
            // Special case, closing the "Comments Corrections" element.
            // determine if this element is a retraction

            // remove the pmid attributes to get at the comments corrections
            final Map<Attribute, String> pmidAttributes = attributeStack.pop();
            final Map<Attribute, String> correctionsAttributes = attributeStack.peek();
            final String refType = correctionsAttributes.get(refTypeAttribute);
            // the current element is a pmid for a retraction
            if ("RetractionOf".equals(refType)) {
                appendRetraction();
            } else {
                // this wasn't a retraction so clear any pmid string collected
                final MutableString retractionOf = singleArticleFieldsMap.get("retractionof");
                retractionOf.length(0);
            }
            // push the pmid attribute back on
            attributeStack.push(pmidAttributes);
        } else if (element == articleEndTagList.peekFirst()
                && elementStack.equals(articleEndTagList)) {
            // Special case, closing the article, save the article
            saveArticle();
        }

        // Pop until we find the expected element
        while (true) {
            attributeStack.pop();             // we don't care what the attributes were anymore

            final Element popped = elementStack.pop();
            // Empty bodied XML elements like <X/> do not seem to go through endElement
            if (element == popped) {
                break;
            }
        }
        return true;
    }

    /**
     * If the element is an {@link #elementIgnore} this will return null. If the element
     * is in the  elementTranslationsMap map, this will return the associated element.
     * Otherwise this will return elementOrig.
     * @param element the element to translate
     * @return the translated element as described above
     */
    private Element translateElement(final Element element) {
        final Element translated = elementTranslationsMap.get(element);
        if (translated == null) {
            return element;
        } else if (translated == elementIgnore) {
            return null;
        } else {
            return translated;
        }
    }

    /**
     * Save the article. Data is stored within {@link #singleArticleFieldsMap} and within
     * the {@link #authors} and {@link #retractedPmids} collections.
     */
    private void saveArticle() {
        removeExtraSpacesForFields();
        setAltFieldValue(JOURNAL_NAME_ALTERNATIVES_MAP, "journal-title", "journal-title-alt");
        setAltFieldValue(JOURNAL_NAME_ALTERNATIVES_MAP, "journal-isoabbr", "journal-isoabbr-alt");
        boolean createArticleForRetractionNotice = true;
        final MutableString pmid =  singleArticleFieldsMap.get("pmid");
        final MutableString text =  singleArticleFieldsMap.get("abstract");
        final MutableString title = singleArticleFieldsMap.get("title");
        final MutableString dateYear = singleArticleFieldsMap.get("date-year");
        final MutableString journal = mergeFields("|", "journal-title", "journal-isoabbr",
                "journal-title-alt", "journal-isoabbr-alt");
        if (text.length() > 0 || title.length() > 0) {
            // if the retraction notice has an abstract or title it is submitted here
            try {
                Map<String, Object> additionalFieldsMap = new HashMap<String, Object>();
                additionalFieldsMap.put("pubdate", makeIntPubDate());
                additionalFieldsMap.put("authors", Author.getAuthorsIndexText(authors));
                additionalFieldsMap.put("year", dateYear.toString());
                additionalFieldsMap.put("journal", journal.toString());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Saving article " + pmid);
                }
                createArticleForRetractionNotice =
                        !processAbstractText(pmid, title, text, additionalFieldsMap);
            } catch (final IOException e) {
                LOG.warn("Caught IOException processing retration", e);
            } catch (final TextractorException e) {
                LOG.warn("Caught Exception processing retration", e);
            }
        }

        if (retractedPmids.size() > 0) {
            processNoticeOfRetraction(pmid, retractedPmids, createArticleForRetractionNotice);
        }
        reset();
    }

    /**
     * Checks if the value at singleArticleFieldsMap[srcField] is a key in altMap
     * and sets singleArticleFieldsMap[destField] to the associated value.
     * @param altMap the translation map
     * @param srcField the source field for singleArticleFieldsMap
     * @param destField the dest field for singleArticleFieldsMap to store the
     * translated value.
     */
    private void setAltFieldValue(
            final Map<MutableString, MutableString> altMap, final String srcField,
            final String destField) {
        final MutableString fieldVal = singleArticleFieldsMap.get(srcField);
        if (fieldVal != null) {
            final MutableString altFieldVal = altMap.get(fieldVal);
            if (altFieldVal != null) {
                singleArticleFieldsMap.put(destField, altFieldVal);
            }
        }
    }

    /**
     * Merges all of the non-empty fields within singleArticleFieldsMap specified
     * by fields into the first field, and then returns that field. This DOES alter
     * the contents of singleArticleFieldsMap.get(fields[0]). Values will be
     * separated by a single space character.
     * @param separator string value to place after each field, or null if no separator
     * should be used. This is a value in addition the space between the fields that will
     * always be included
     * @param fields the fields within singleArticleFieldsMap to merge
     * @return the merged values.
     */
    private MutableString mergeFields(final String separator, final String... fields) {
        if (fields == null) {
            return EMPTY_MUTABLE_STRING;
        }
        MutableString base = null;
        for (final String fieldName : fields) {
            final MutableString fieldVal = singleArticleFieldsMap.get(fieldName);
            if (fieldVal != null && fieldVal.length() > 0) {
                if (base == null) {
                    base = fieldVal;
                } else {
                    base.append(' ');
                    base.append(fieldVal);
                }
                if (separator != null) {
                    base.append(' ');
                    base.append(separator);
                }
            }
        }
        if (base == null) {
            return EMPTY_MUTABLE_STRING;
        } else {
            return base;
        }
    }

    /**
     * Clean up the text within each field of {@link #nestedTagToFieldMap}.
     * NOTE: This should not be done to files where we are keeping exact
     * positions as this will mess up positioning stuff.
     */
    private void removeExtraSpacesForFields() {
        for (String field : nestedTagToFieldMap.values()) {
            mutableStringUtil.stripExtraSpaces(singleArticleFieldsMap.get(field));
        }
    }

    /**
     * Given the author names within lastName, firstName, foreName, initials
     * append the appropriate names to the string authorsIndexText.
     */
    private void appendAuthorNames() {
        final MutableString lastName =
                mutableStringUtil.stripExtraSpaces(singleArticleFieldsMap.get("author-lastname"));
        final MutableString firstName =
                mutableStringUtil.stripExtraSpaces(singleArticleFieldsMap.get("author-firstname"));
        final MutableString foreName =
                mutableStringUtil.stripExtraSpaces(singleArticleFieldsMap.get("author-forename"));
        final MutableString initials =
                mutableStringUtil.stripExtraSpaces(singleArticleFieldsMap.get("author-initials"));
        if ((lastName.length() + firstName.length() + foreName.length() + initials.length()) == 0) {
            // No authors
            return;
        }
        final CharSequence firstNameVal = ICBMutableStringUtils.firstNonEmptyValue(
                EMPTY_MUTABLE_STRING, foreName, firstName, initials);
        authors.add(new Author(lastName.toString(), firstNameVal.toString(), initials.toString()));

        // Clear the current author values in preparation for the next author
        lastName.length(0);
        firstName.length(0);
        foreName.length(0);
        initials.length(0);
    }

    /**
     * Append a retraction.
     */
    private void appendRetraction() {
        final MutableString retractionOf = singleArticleFieldsMap.get("retractionof");
        mutableStringUtil.stripExtraSpaces(retractionOf);
        if (retractionOf.length() == 0) {
            return;
        }
        retractedPmids.add(retractionOf.toString());
        retractionOf.length(0);
    }

    /**
     * Take the values in dateYear/dateMonth/dateDay and create a long value
     * to reperesent the date.
     * @return the date in a long
     */
    private long makeIntPubDate() {
        final MutableString year = singleArticleFieldsMap.get("date-year");
        final MutableString month = singleArticleFieldsMap.get("date-month");
        final MutableString day = singleArticleFieldsMap.get("date-day");

        if (!ICBMutableStringUtils.isNonBlankNumeric(year)) {
            // Cannot use it
            return TextractorConstants.EMPTY_PUBLICATION_DATE;
        }
        if (!ICBMutableStringUtils.isNonBlankNumeric(month)) {
            if (!ICBMutableStringUtils.convertThreeLetterMonthToDigits(month)) {
                // Default to month 1
                month.length(0);
                month.append("1");
            }
        }
        if (!ICBMutableStringUtils.isNonBlankNumeric(day)) {
            // Default to day 1
            day.length(0);
            day.append("1");
        }

        return (ICBMutableStringUtils.parseInt(year) * 10000)
                + (ICBMutableStringUtils.parseInt(month) * 100)
                + ICBMutableStringUtils.parseInt(day);
    }

    /**
     * Process the text of this document.
     *
     * @param pmidVal the pmid of the document
     * @param titleVal the title of the document
     * @param textVal the text of the document
     * @param additionalFieldsMap additional fields to index
     * @return True if an article was created for this PMID.
     * @throws IOException error processing sentence
     * @throws SentenceProcessingException error processing sentence
     */
    public abstract boolean processAbstractText(
            final MutableString pmidVal, final MutableString titleVal, final MutableString textVal,
            final Map<String, Object> additionalFieldsMap)
            throws IOException, SentenceProcessingException;

    /**
     * Process retraction notices.
     *
     * @param pmidVal the pmid of the retraction
     * @param retractedPmidsVal the retracted pmids
     * @param createArticleVal True if this method may create an article to
     * represent the retraction notice
     */
    public abstract void processNoticeOfRetraction(
            final MutableString pmidVal, final List<String> retractedPmidsVal,
            final boolean createArticleVal);
}
