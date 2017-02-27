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

import com.martiansoftware.jsap.JSAPException;
import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.mg4j.document.DocumentSequence;
import org.apache.commons.configuration.ConfigurationException;
import org.htmlparser.util.DefaultParserFeedback;
import textractor.datamodel.ArticlePool;
import textractor.mg4j.document.TextractorDocumentFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * User: Fabien Campagne
 * Date: Oct 17, 2004
 * Time: 1:19:53 PM
 */
public class BuildDocumentIndexFromHTMLArticles extends BuildDocumentIndex {
    private String basename;
    private boolean stemming;

    public final int getParserFeedBackLevel() {
        return parserFeedBackLevel;
    }

    public final void setParserFeedBackLevel(final int parserFeedBackLevel) {
        this.parserFeedBackLevel = parserFeedBackLevel;
    }

    protected int parserFeedBackLevel = DefaultParserFeedback.NORMAL;

    @Override
    public final String getDefaultBasename(final String basename, final boolean stemming) {
        return this.basename;
    }

    @Override
    protected final BuildDocumentIndex createNew() {
        return new BuildDocumentIndexFromHTMLArticles(basename, stemming);
    }


    @Override
    public int serializeTextSourceToWriter(final OutputStreamWriter writer, final int chunkSize) throws IOException {
        // todo rewrite for collection of articles (i.e., directory or input file, as described in args)

        final HTMLArticleConversionProcessDirectory htmlArticleConversionProcessDirectory =
                new HTMLArticleConversionProcessDirectory(args, writer, articlePool);

        htmlArticleConversionProcessDirectory.setParserFeedBackLevel(parserFeedBackLevel);
        htmlArticleConversionProcessDirectory.process();
        return htmlArticleConversionProcessDirectory.getTotalSencenceCount();
    }

    @Override
    public final DocumentSequence documentSequence(final TextractorDocumentFactory factory) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    protected ArticlePool articlePool;

    public final ArticlePool getArticlePool() {
        return articlePool;
    }

    public final void setArticlePool(final ArticlePool articlePool) {
        this.articlePool = articlePool;
    }

    protected String[] args;


    public final void setProcessArguments(final String[] args) {
        this.args = args;
    }

    public final String getBasename() {
        return basename;
    }

    public BuildDocumentIndexFromHTMLArticles(final String basename) {
        this.basename = basename;
        this.stemming = false;
    }

    public BuildDocumentIndexFromHTMLArticles(final String[] args, final String basename, final boolean stemming) {
        this(basename, stemming);
        setCommandLineArguments(args);
        setArticlePool(new ArticlePool());
    }

    public BuildDocumentIndexFromHTMLArticles(final String basename, final boolean stemming) {
        this.basename = basename;
        this.stemming = stemming;
    }

    /**
     * Index the collection of text documents.
     *
     * @param args Arguments of the process, must include -d or -i or -url for source of articles.
     * @see CharSequence
     */
    public final void index(final String[] args) throws NoSuchMethodException,
            IllegalAccessException, ConfigurationException, IOException,
            JSAPException, InvocationTargetException, ClassNotFoundException,
            InstantiationException {
        setProcessArguments(args);
        index(stemming, 0, basename);
    }

    public final void setCommandLineArguments(final String[] args) {
        this.args = args;
    }

    public static void main(final String[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException, IOException, JSAPException, ConfigurationException {
        final String basename = CLI.getOption(args, "-basename", null);
        final BuildDocumentIndexFromHTMLArticles docindexer = new BuildDocumentIndexFromHTMLArticles(args, basename, false);
        final boolean parenthesesAreWords = CLI.isKeywordGiven(args, "-indexParentheses");
        docindexer.indexerOptions.setParenthesesAreWords(parenthesesAreWords);
        docindexer.process(args);
        System.exit(0);
    }
}
