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

package textractor.util;

import textractor.database.DbManager;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.RunningCounter;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: Sep 1, 2004
 * Time: 6:12:10 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ResetParameters {
    public static void main(final String[] args) throws TextractorDatabaseException {
        final DbManager dbm=new DbManager(args);

        dbm.beginTxn();

        final Iterator parametersIt=dbm.getExtentIterator("textractor.datamodel.FeatureCreationParameters", true);
        while(parametersIt.hasNext()){
            dbm.delete(parametersIt.next());
        }
        ((RunningCounter) dbm.lookup("ParametersRunningCounter")).setNumber(0);

        dbm.commitTxn();
    }
}
