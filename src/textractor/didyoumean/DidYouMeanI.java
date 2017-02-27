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

import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import org.apache.commons.configuration.ConfigurationException;
import textractor.scoredresult.ScoredResult;

import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Fabien Campagne
 * Date: Oct 16, 2006
 * Time: 11:36:25 AM
 * To change this template use File | Settings | File Templates.
 */
public interface DidYouMeanI {
    List<ScoredResult> suggest(String term, float cutoff)
            throws IOException, ConfigurationException,
            ParseException, ClassNotFoundException, QueryParserException,
            QueryBuilderVisitorException;

    List<ScoredResult> suggest(String term, boolean orderWithVignaScore, float cutoff) throws IOException, ParseException, QueryParserException, QueryBuilderVisitorException;

    List<ScoredResult> suggestRelated(String term, float cutoff) throws ConfigurationException, IOException, ParseException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException;

    List<ScoredResult> suggestPaiceHusk(String term, float cutoff) throws ConfigurationException, IOException, ParseException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException;
}
