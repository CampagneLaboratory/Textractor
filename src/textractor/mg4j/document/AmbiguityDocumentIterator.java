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

package textractor.mg4j.document;

import edu.cornell.med.icb.ncbi.pubmed.PubMedInfoTool;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.mg4j.document.Document;
import it.unimi.dsi.mg4j.document.DocumentFactory;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.database.DbManager;
import textractor.database.TextractorDBDocumentIterator;
import textractor.datamodel.Sentence;
import textractor.datamodel.TextractorDocument;
import textractor.util.NullSentenceFilter;
import textractor.util.SentenceFilter;

import java.io.IOException;

/**
 * Document iterator for ambiguity documents.
 */
public final class AmbiguityDocumentIterator extends TextractorDBDocumentIterator {
    /** Used to log debug and informational messages. */
    private static final Log LOG =
        LogFactory.getLog(AmbiguityDocumentIterator.class);
    private long lastArticleNumber = NOT_DEFINED;
    private final MutableString articleSentencesText;
    private Reference2ObjectMap<Enum< ? >, Object> metadata;

    private static final long NOT_DEFINED = -1;
    private final SentenceFilter sentenceIndexFilter;
    private int filteredSentenceCount;
    private static final MutableString EMPTY_DOCUMENT = new MutableString(" ");

    public AmbiguityDocumentIterator(final DbManager dbm,
            final SentenceFilter setSentenceFilter,
            final DocumentFactory documentFactory, final int chunkSize) {
        super(dbm, documentFactory, chunkSize,  new NullSentenceFilter());
        this.articleSentencesText = new MutableString();
        this.sentenceIndexFilter = setSentenceFilter;
        if (LOG.isDebugEnabled()) {
        LOG.debug("Filtering sentences with "
                + sentenceIndexFilter.getClass().getName());
        }
    }

    public AmbiguityDocumentIterator(final DbManager dbm,
            final DocumentFactory documentFactory, final int chunkSize) {
        this(dbm, new NullSentenceFilter(), documentFactory, chunkSize);
    }

    protected Document nextSentence(final Sentence sentence) throws IOException {
        if (lastArticleNumber != sentence.getArticle().getArticleNumber()) {
            lastArticleNumber = sentence.getArticle().getArticleNumber();
        }
        return super.nextSentence(sentence);
    }

    protected String sentenceToText(final Sentence sentence) {
        final String text = sentence.getText();
        if (sentenceIndexFilter.filterSentence(text)) {
            filteredSentenceCount++;
            return " ";
        } else {
            return text;
        }
    }

    @Override
    public Document nextDocument() throws IOException {
        ensureHasNext();
        if (iterator == null || !iterator.hasNext()) {
            LOG.info("Processed " + count + " sentences.");
            LOG.info("Filtered " + filteredSentenceCount + " sentences out ("
                    + (filteredSentenceCount * 100f / count) + " % of total).");

            return null; // no more sentences. We are done.
        }

        final TextractorDocument document = iterator.next();
        if (document instanceof Sentence) {
            // sentences get the full treatment:
            final Sentence sentence = (Sentence) document;
            if (sentence.getArticle().getArticleNumber() != lastArticleNumber
                    && lastArticleNumber != NOT_DEFINED) {
                // new article:
                final Document result =
                    prepareDocument(metadata, articleSentencesText);
                articleSentencesText.setLength(0);
                // append the first document of this new article:
                appendSentenceText(sentence);
                init(sentence);

                return result;
            }

            if (lastArticleNumber == NOT_DEFINED) {
                init(sentence);
            }
            appendSentenceText(sentence);
        } else {  // other documents are treated as empty:
            return prepareDocument(document.getMetaData(), EMPTY_DOCUMENT);
        }
        return prepareDocument(metadata, EMPTY_DOCUMENT);
    }

    private void init(final Sentence sentence) {
        metadata = assembleMetaData(sentence);
        lastArticleNumber = sentence.getArticle().getArticleNumber();
    }

    private Reference2ObjectMap<Enum< ? >, Object> assembleMetaData(final Sentence sentence) {
        final long index = sentence.getDocumentNumber();

        final Reference2ObjectMap<Enum< ? >, Object> metadata =
                new Reference2ObjectOpenHashMap<Enum< ? >, Object>();
        metadata.put(PropertyBasedDocumentFactory.MetadataKeys.ENCODING, "UTF-8");
        metadata.put(PropertyBasedDocumentFactory.MetadataKeys.TITLE, Long.toString(index));

        final long pmid = sentence.getArticle().getPmid();
        final String url = PubMedInfoTool.pubmedUriFromPmid((int) pmid);
        metadata.put(PropertyBasedDocumentFactory.MetadataKeys.URI, url);
        return metadata;
    }

    private Document prepareDocument(final Reference2ObjectMap<Enum< ? >, Object> metadata,
            final MutableString articleSentencesText) throws IOException {
        return createDocument(metadata, articleSentencesText.toString());
    }

    /**
     * Ensure hasNext.
     */
    private void ensureHasNext() {
        if (!iterator.hasNext()) {
            // try to get more sentences from the database:
            dbm.commitTxn();
            dbm.beginTxn();
            lowerBound = count - 1;
            iterator =
                dbm.getTextractorManager().getDocumentIterator(lowerBound,
                        lowerBound += chunkSize);
        }
    }

    private void appendSentenceText(final Sentence sentence) {
        articleSentencesText.append(sentenceToText(sentence));
        articleSentencesText.append(' ');
        count++;
    }
}
