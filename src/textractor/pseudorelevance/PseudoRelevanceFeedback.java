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

package textractor.pseudorelevance;

import java.io.IOException;
import java.util.List;

/**
 * Implementations of this interface support pseudo-relevance feedback. Pseudo-relevance feedback is
 * a form of query expansion which expands a query with terms enriched in top documents returned by
 * the original query.
 *
 * @author Fabien Campagne
 *         Date: Nov 30, 2006
 *         Time: 3:28:37 PM
 */
public interface PseudoRelevanceFeedback {
    /**
     * Inspect top documents and suggest query expansion terms. This method makes no assumption about how the query
     * is represented, distributed or executed. Implementations never return the initial query terms. Only new terms
     * are returned.
     * @param topDocuments  All documents in topDocuments are used to calculate term weights
     * @param initalQueryTerms Terms used in the initial query
     * @param maxAdditionalQueryTerms Maximum number of new terms to return.
     * @return Query terms of expanded query, in decreasing weight order.
     * @throws java.io.IOException When frequency information cannot be obtained.
     */
    List<QueryTerm> expand(int []topDocuments, QueryTerm []initalQueryTerms,
                           int maxAdditionalQueryTerms) throws IOException;
     /**
     * Inspect top documents and suggest query expansion terms common across some top documents.
     * @param topDocuments  All documents in topDocuments are used to calculate term weights
     * @param initalQueryTerms Terms used in the initial query
     * @param maxAdditionalQueryTerms Maximum number of new terms to return.
     * @return Query terms of expanded query, in decreasing weight order.
      * @throws java.io.IOException  When frequency information cannot be obtained.
     */
     List<QueryTerm> expandConsensus(int[] topDocuments, QueryTerm[] initalQueryTerms, int maxAdditionalQueryTerms) throws IOException;

}
