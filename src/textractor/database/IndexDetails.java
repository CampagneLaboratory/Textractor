/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
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

package textractor.database;

import it.unimi.dsi.mg4j.index.BitStreamIndex;
import it.unimi.dsi.mg4j.index.IndexReader;
import it.unimi.dsi.mg4j.index.TermMap;
import it.unimi.dsi.mg4j.index.TermProcessor;
import it.unimi.dsi.mg4j.query.parser.QueryParser;
import it.unimi.dsi.mg4j.query.parser.SimpleParser;
import textractor.mg4j.io.TextractorWordReader;

import java.io.IOException;
import java.io.Serializable;

/**
 * Describe class here.
 *
 * @author Kevin Dorff
 */
public class IndexDetails implements Serializable {
    private final String basename;
    private final String alias;

    private final BitStreamIndex index;
    private TermProcessor termProcessor;
    private final IndexReader indexReader;
    private TermMap termMap;
    private TextractorWordReader wordReader;
    private QueryParser queryParser;

    public IndexDetails(final String aliasVal) {
        this.alias = aliasVal;
        this.basename = "";
        this.index = null;
        this.indexReader = null;
        this.termMap = null;
        this.queryParser = null;
    }

    public IndexDetails(
            final String basenameVal, final String aliasVal, final BitStreamIndex indexVal)
            throws IOException {
        this.basename = basenameVal;
        this.alias = aliasVal;

        index = indexVal;
        termProcessor = index.termProcessor;
        indexReader = index.getReader();
        termMap = index.termMap;
    }

    public String getBasename() {
        return basename;
    }

    public String getAlias() {
        return alias;
    }

    public BitStreamIndex getIndex() {
        return index;
    }

    public void setTermProcessor(final TermProcessor termProcessor) {
        this.termProcessor = termProcessor;
    }

    public TermProcessor getTermProcessor() {
        return termProcessor;
    }

    public IndexReader getIndexReader() {
        return indexReader;
    }

    public void setTermMap(final TermMap map) {
        this.termMap = map;
    }

    public TermMap getTermMap() {
        return termMap;
    }

    public TextractorWordReader getWordReader() {
        return wordReader;
    }

    public synchronized QueryParser getQueryParser() {
        if (queryParser == null) {
            queryParser = new SimpleParser(termProcessor);
        }
        return queryParser;
    }

    public void setWordReader(final TextractorWordReader wordReader) {
        this.wordReader = wordReader;
    }

    public CharSequence termAsCharSequence(final int termIndex) {
        return termMap.getTerm(termIndex);
    }

    public static String basenameFromAlias(final String basename, final String alias) {
        if (basename.endsWith("-text")) {
            return  basename.substring(0, (basename.length() - "-text".length())) + "-" + alias;
        } else {
            return basename + "-" + alias;
        }
    }

}
