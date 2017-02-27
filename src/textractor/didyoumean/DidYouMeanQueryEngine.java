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

package textractor.didyoumean;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.mg4j.index.Index;
import it.unimi.dsi.mg4j.query.IntervalSelector;
import it.unimi.dsi.mg4j.query.QueryEngine;
import it.unimi.dsi.mg4j.query.SelectedInterval;
import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitor;
import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.QueryParser;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import it.unimi.dsi.mg4j.search.DocumentIterator;
import it.unimi.dsi.mg4j.search.DocumentIteratorBuilderVisitor;
import it.unimi.dsi.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.mg4j.search.score.Scorer;
import it.unimi.dsi.mg4j.search.score.VignaScorer;
import it.unimi.dsi.mg4j.util.MutableString;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class to process queries with multiple fields.
 */
public final class DidYouMeanQueryEngine {
    private final QueryEngine engine;
    /**
     * Used to LOG informational and debug messages.
     */
    private static final Log LOG =
            LogFactory.getLog(DidYouMeanQueryEngine.class);

    public DidYouMeanQueryEngine(final QueryParser queryParser,
                                 final Reference2DoubleMap<Index> weights,
                                 final Object2ReferenceMap<String, Index> indexMap) {
	super();
        engine = new QueryEngine(queryParser, getBuilderVisitor(indexMap), indexMap);
        engine.multiplex = true;
        engine.setWeights(weights);
        engine.score(new Scorer[]{new VignaScorer()}, new double[]{1});
        engine.intervalSelector = new IntervalSelector(3, 40);
    }

    private QueryBuilderVisitor<DocumentIterator> getBuilderVisitor(final Object2ReferenceMap<String, Index> indexMap) {
        return new DocumentIteratorBuilderVisitor(indexMap, indexMap.get("word"), 0);
    }

    public int result(final Map<String, String> queryMap, final int offset, final int length, final ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results ) throws IOException, QueryParserException, QueryBuilderVisitorException {
        return multifieldScoredResult(queryMap, offset, length, results);
    }


    public int multifieldScoredResult(final Map<String, String> queryMap,
                                      final int offset, final int length,
                                      final ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results )
            throws IOException, QueryParserException, QueryBuilderVisitorException {


        final String queryString = multifieldProcess(queryMap);
        if (LOG.isDebugEnabled()) {
            LOG.debug("DYM query: " + queryString);
        }
        return engine.process(queryString, offset, length, results);
    }

    /**
     * Create the multifield MG4J query term, based on the length of the full
     * word, as Did You Mean suggestions for shorter words require fewer fields.
     */

    private String multifieldProcess(final Map<String, String> queryMap) {
        if (!engine.multiplex) {
            return queryMap.get("word");
        }

        final List<String> fieldArray = new ArrayList<String>();

        if (queryMap.get("word").length() < 5) {
            fieldArray.add("word");
            fieldArray.add("gram2");
            fieldArray.add("gram3");
        } else if (queryMap.get("word").length() < 9) {
            fieldArray.add("word");
            fieldArray.add("3start");
            fieldArray.add("3end");
            fieldArray.add("gram3");
        } else {
            fieldArray.addAll(queryMap.keySet());
        }

        return getQueryStringWithFields(fieldArray, queryMap);
    }

    private String getQueryStringWithFields(final List<String> fields,
            final Map<String, String>queryMap) {
        final MutableString queryString = new MutableString();
        for (final String field : fields) {
            final String query = queryMap.get(field);
            if (StringUtils.isNotBlank(query)) {
                if (queryString.length() != 0) {
                    queryString.append(" OR ");
                }
                queryString.append(field);
                queryString.append(":(");
                queryString.append(query);
                queryString.append(')');
            }
        }

        return queryString.toString();
    }
}
