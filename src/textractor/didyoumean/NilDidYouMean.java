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
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation that always returns no suggestions.
 *
 * @author Fabien Campagne
 *         Date: Oct 16, 2006
 *         Time: 11:30:13 AM
 */
public class NilDidYouMean implements DidYouMeanI {


    public List<ScoredResult> suggest(final String term, final float cutoff) throws IOException, ConfigurationException, ParseException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException {
        return new ArrayList<ScoredResult>();
    }

    public List<ScoredResult> suggest(final String term, final boolean orderWithVignaScore, final float cutoff) throws IOException, ParseException, QueryParserException, QueryBuilderVisitorException {
        return new ArrayList<ScoredResult>();
    }

    public List<ScoredResult> suggestRelated(final String term, final float cutoff) throws ConfigurationException, IOException, ParseException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException {
        return new ArrayList<ScoredResult>();
    }

    public List<ScoredResult> suggestPaiceHusk(final String term, final float cutoff) throws IOException, ParseException, QueryParserException, QueryBuilderVisitorException {
        return new ArrayList<ScoredResult>();
    }
}
