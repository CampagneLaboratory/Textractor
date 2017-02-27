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

import edu.mssm.crover.cli.CLI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.datamodel.TextractorInfo;

import javax.jdo.*;
import java.util.Collection;
import java.util.Iterator;


/**
 * Central point for database access in the system. An instantiation of
 * the DbManager allows
 * <ol>
 * <li>base TransactionManager Interface functionality for database _process
 * delimitation methods</li>
 * <li>Access to related database _process managers</li>
 * </ol>
 */
public final class DbManager implements TransactionManager {
    private static final Log LOG = LogFactory.getLog(DbManager.class);
    private PersistenceManagerFactory _pmf;
    private PersistenceManager _dummyPMF;
    private PersistenceManager _pm;
    private Transaction _txn;
    private TextractorManager _tm;
    private NamingManager _namingManager;
    private String propertyFilename = "textractor.properties";

    private JdoExtension jdoExtension;

    /**
     * Creates a nwe DbManager in the web application context.
     *
     * @throws TextractorDatabaseException
     */
    public DbManager() throws TextractorDatabaseException {
        init(null);
    }

    /**
     * Creates a nwe DbManager in the web application context.
     *
     * @throws TextractorDatabaseException
     */
    public DbManager(final String propertyFilename) throws TextractorDatabaseException {
        setPropertyFilename(propertyFilename);
        init(null);
    }

    public TextractorManager getTextractorManager() {
        return _tm;
    }

    /**
     * Class Constructor. Creates a new DbManager in the command line context.
     *
     * @throws TextractorDatabaseException
     */
    public DbManager(final String[] args) throws TextractorDatabaseException {
        final String property_filename = CLI.getOption(args, "-property", propertyFilename);
        LOG.debug("Using property file: " + property_filename);
        setPropertyFilename(property_filename);
        init(args);
    }

    private void setPropertyFilename(final String filename) {
        propertyFilename = filename;
    }

    protected void init(final String[] args) throws TextractorDatabaseException {
        LOG.debug("Initializing DbManager");

        initJdoExtension();
        initDB();
        initTransaction();
        initManagers();

        if (args != null) {
            beginTxn();
            final TextractorInfo info = getTextractorManager().getInfo();
            if (CLI.isKeywordGiven(args, "-indexParentheses")) {
                info.setIndexParentheses(true);
            }
            commitTxn();
        }
        LOG.debug("end of initializing of DbManager");
    }

    private void initJdoExtension() throws TextractorDatabaseException {
        final PropertyManager propertyManager =
                PropertyManager.getInstance(propertyFilename);
        final String jdoExtensionClass = propertyManager.getProperty(
                TextractorDatabaseConstants.JDO_EXTENSION_CLASS,
                TextractorDatabaseConstants.DEFAULT_JDO_EXTENSION_CLASS);
        LOG.debug("Using JDO Extension: " + jdoExtensionClass);
        try {
            final Class clazz = Class.forName(jdoExtensionClass);
            jdoExtension = (JdoExtension) clazz.newInstance();
        } catch (final ClassNotFoundException e) {
            throw new TextractorDatabaseException(e);
        } catch (final InstantiationException e) {
            throw new TextractorDatabaseException(e);
        } catch (final IllegalAccessException e) {
            throw new TextractorDatabaseException(e);
        }
    }

    public JdoExtension getJdoExtension() {
        return jdoExtension;
    }

    /**
     * Initializes the Database for this class and ensures that a database
     * exists for this class.
     */
    private synchronized void initDB() {
        if (_pmf == null) {
            final PropertyManager propertyManager =
                    PropertyManager.getInstance(propertyFilename);
            LOG.debug("Opening PMF Connection");
            _pmf = JDOHelper.getPersistenceManagerFactory(propertyManager);
            LOG.debug("End:  Opening PMF Connection");

            // handle any jdo implementation specific initialization
            jdoExtension.initDB(_pmf, propertyManager);
        }

        if (_dummyPMF == null) {
            LOG.debug("Creating new Dummy PersistenceManager");
            _dummyPMF = _pmf.getPersistenceManager();
        }

        LOG.debug("Creating new PersistenceManager");
        _pm = _pmf.getPersistenceManager();
    }

    /**
     * 1.Initializes the Transaction object for this class.
     */
    private void initTransaction() {
        //initDB(); //do we need this? db is initialized once in the constructor
        if (_txn == null) {
            _txn = _pm.currentTransaction();
        }
    }

    /**
     * Initializes sub-managers. Checks that sub-managers (managers
     * accessible through getter methods on this instance) are initialized
     * correctly.
     */
    private void initManagers() throws TextractorDatabaseException {
        if (_namingManager == null) {
            _namingManager = new NamingManager(this);
        }
        if (_tm == null) {
            _tm = new TextractorManager(this);
        }
    }

    /**
     * Begin a transaction. To begin a database related _process,
     * the _process must be within a beginTxn()-commitTxn()/abortTxn() block.
     * beginTxn() delimits the beginning of a database related _process.
     */
    public void beginTxn() {
        LOG.debug("beginTxn()");
        _txn.begin();
    }

    /**
     * Commit a transaction. To complete a database related _process such as
     * storing an object, the _process must be within a
     * beginTxn()-commitTxn()/abortTxn() block.
     * commitTxn() delimits the completion of a database related _process.
     */
    public void commitTxn() {
        LOG.debug("commitTxn()");

        if (_namingManager != null) {
            _namingManager.clearCache(false);
        }

        _txn.commit();
    }

    /**
     * Checkpoints a transaction. This is similar to doing a commitTxn()
     * followed by beginTxn() but keep references open.
     */
    public void checkpointTxn() {
        LOG.debug("checkpointTxn()");
        /*
        com.poet.jdo.Transactions.checkpoint(_txn);
        */
        commitTxn();
        beginTxn();
    }

    /**
     * Abort a transaction. To abort a database related _process such as a
     * failure while attempting to store an object, the _process must be
     * begun with a beginTxn() call followed by an abortTxn() call when the
     * _process fails, or a commitTxn() call when the _process successfully
     * completes. abortTxn() delimits the end of a database _process when the
     * _process fails
     */
    public void abortTxn() {
        LOG.debug("abortTxn()");
        _txn.rollback();
    }

    /**
     * Checks that a valid Transaction is in progress during a database _process,
     *
     * @return <code>true</code> if valid Transaction is in progress for this thread, else return <code>false</code>
     */
    public boolean txnInProgress() {
        return _txn != null && _txn.isActive();
    }

    /**
     * Retrieve an instance from the database.
     * This is only a hint to the PersistenceManager that the application
     * intends to use the instance, and its field values should be retrieved.
     */
    public void retrieve(final Object obj) {
        _pm.retrieve(obj);
    }

    /**
     * Retrieve instances from the store. This is only a hint to the
     * PersistenceManager that the application intends to use the instances, and
     * their field values should be retrieved.
     *
     * @param c Collection of objects to be retrived.
     */
    public void retrieveAll(final Collection c) {
        _pm.retrieveAll(c);
    }

    /**
     * Makes a transient object persistent.
     */
    public void makePersistent(final Object obj) {
        _pm.makePersistent(obj);
    }

    /**
     * Returns the object id of a firstclass object (JDO persistent object).
     *
     * @param obj The object you want the oid from.
     * @return the oid of object obj
     */
    public Object getObjectId(final Object obj) {
        return _pm.getObjectId(obj);
    }

    /**
     * Returns the object for a certain object id of a firstclass object (JDO
     * persistent object).
     *
     * @param objid an object id.
     * @return the object
     */
    public Object getObjectByObjectId(final Object objid) {
        return _pm.getObjectById(objid, false);
    }

    /**
     * Same as makePersistent
     */
    public void bind(final Object obj) {
        makePersistent(obj);
    }

    /**
     * Binds a transient object to a name and make it persistent.
     */
    public void bind(final Object obj, final String name) {
        _namingManager.bind(obj, name);
    }

    /**
     * Release the association between the name and a stored object. After this
     * call, lookup(name) will return null.
     */
    public void unbind(final String name) {
        _namingManager.unbind(name);
    }

    /**
     * Returns the reference to the named object, or null if the
     * name cannot be found.
     */
    public Object lookup(final String name) throws TextractorDatabaseException {
        return _namingManager.lookup(name);
    }

    /**
     * Deletes an object from the database. This call expilcitly removes an
     * object from the database. In general, it is not necessary to call this
     * method to delete an object. Objects are garbage collected when they are
     * no longer referenced. In some special circumstances, though, it is useful
     * to immediately remove the object. This method can then be used.
     */
    public boolean delete(final Object object) {
        _pm.deletePersistent(object);
        return true;
    }

    public boolean deleteAll(final Collection c) {
        _pm.deletePersistentAll(c);
        return true;
    }


    /**
     * Close the persistence manager upon garbage collection.
     *
     * @throws Throwable if an error occurs.
     */
    @Override
    public void finalize() throws Throwable {
        shutdown();
        super.finalize();
    }

    public void shutdown() {
        if (_pm != null && !_pm.isClosed()) {
            final Transaction _txn = _pm.currentTransaction();
            if ((_txn != null) && (_txn.isActive())) {
                this.abortTxn();
            }
            _pm.close();
        } else if (LOG.isTraceEnabled()) {
            LOG.trace("pm already closed.");
        }

        _pm = null;
    }

    public PersistenceManager getPM() {
        return this._pm;
    }

    public Extent getExtent(final String classname, final boolean getSubclasses)
            throws TextractorDatabaseException {
        try {
            return getExtent(Class.forName(classname), getSubclasses);
        } catch (final ClassNotFoundException e) {
            throw new TextractorDatabaseException(e);
        }
    }

    public Extent getExtent(final Class extentClass, final boolean getSubclasses) {
        return _pm.getExtent(extentClass, getSubclasses);
    }

    public Iterator getExtentIterator(final String classname, final boolean getSubclasses) throws TextractorDatabaseException {
        return getExtent(classname, getSubclasses).iterator();
    }

    public Iterator getExtentIterator(final Class extentClass, final boolean getSubclasses) {
        return getExtent(extentClass, getSubclasses).iterator();
    }

    public Query newQuery() {
        return this._pm.newQuery();
    }

    public String getPropertyFilename() {
        return propertyFilename;
    }

    /**
     * Make element of a collection persistent.
     *
     * @param objects Elements of the collection will be made persistent.
     */
    public void makePersistentAll(final Collection objects) {
        getPM().makePersistentAll(objects);
    }
}
