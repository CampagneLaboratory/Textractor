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

package textractor.tools;

import it.unimi.dsi.mg4j.util.MutableString;
import org.htmlparser.util.DefaultParserFeedback;
import org.htmlparser.util.ParserException;
import textractor.TextractorRuntimeException;
import textractor.datamodel.ArticlePool;
import textractor.html.Html2Text;
import textractor.util.ProcessDirectory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Oct 18, 2004
 * Time: 4:57:15 PM
 * To change this template use File | Settings | File Templates.
 */
public final class HTMLArticleConversionProcessDirectory extends ProcessDirectory {
    private int parserFeedBackLevel = DefaultParserFeedback.NORMAL;
    private final Writer writer;
    private int allArticleSentenceCount;
    private final ArticlePool articlePool;

    public int getParserFeedBackLevel() {
        return parserFeedBackLevel;
    }

    public void setParserFeedBackLevel(final int parserFeedBackLevel) {
        this.parserFeedBackLevel = parserFeedBackLevel;
    }

    public HTMLArticleConversionProcessDirectory(final String[] args,
            final Writer indexInputWriter, final ArticlePool articlePool) {
        super(args, ".htm");
        this.setFileFilterExtension2(".html");
        this.writer = indexInputWriter;
        this.articlePool = articlePool;
    }

    @Override
    public void processFile(final Reader reader, final String filename,
            final String output_filename) throws IOException {
        final Html2Text converter = new Html2Text();
        int count = 0;
        try {
            converter.parse(reader, parserFeedBackLevel);
        } catch (final ParserException e) {
            throw new TextractorRuntimeException(e);
        }
        final String text = converter.getText();
        final SentenceSplitter splitter = new DefaultSentenceSplitter();
        final Iterator<MutableString> it = splitter.split(text);
        while (it.hasNext()) {
            final MutableString document = it.next();
            writer.write(document.array());
            writer.write(" "); // mg4j omits the last word
            writer.write(BuildDocumentIndex.DOCUMENT_SEPARATOR);
            count++;
            allArticleSentenceCount++;
        }
        // parse PMID out of filename
        long PMID = 0;
        if (filename != null) {
            final String[] PMID_source = filename.split("/|\\\\"); // split the directory/filename into directory file
            if (PMID_source.length > 1) {
                final String PMIDString = PMID_source[PMID_source.length - 1]
                        .replaceAll("\\..+", ""); // remove .html or .htm from
                                                    // the filename
                if (PMIDString.matches("^\\d+$")) {
                    PMID = Long.parseLong(PMIDString);
                }
            }
        }
        articlePool.setEntry(filename, PMID, count);
    }

    public int getTotalSencenceCount() {
        return allArticleSentenceCount;
    }
}
