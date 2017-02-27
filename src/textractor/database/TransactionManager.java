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

package textractor.database;

/**
 * Interface that defines base database transaction processes.
 */
public interface TransactionManager {
    /**
     * Abort a transaction.
     * @throws TextractorDatabaseException
     */
    void abortTxn();

    /**
     * Begin a transaction.
     * @throws TextractorDatabaseException
     */
    void beginTxn();

    /**
     * Commit a transaction.
     * @throws textractor.database.TextractorDatabaseException
     */
    void commitTxn() throws TextractorDatabaseException;

    /**
     * Determines whether or not a Transaction is in progress.
     *
     * @return <code>true</code> if a transaction is in progress, otherwise
     *         <code>false</code>
     */
    boolean txnInProgress();

    /**
     * Finalize method for the class.
     */
    void finalize() throws Throwable;
}
