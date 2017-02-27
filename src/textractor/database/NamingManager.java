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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.datamodel.naming.NamedObject;

import javax.jdo.Extent;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class NamingManager {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(NamingManager.class);

    /** Database manager associated with this manager. */
    private final DbManager dbManager;

    /**
     * A globaly accessible cache.
     */
    private final Map<String, HashEntry> cache;

    public NamingManager(final DbManager dbm) {
        this.dbManager = dbm;
        cache = Collections.synchronizedMap(new HashMap<String, HashEntry>());

    }

    private Object lookupObject(final String name) {
        Object object = null;

        // check the cache first before accessing the database
        final HashEntry he = cache.get(name);
        if (he != null) {
            object = he.getObject(dbManager);
        } else {
            final NamedObject namedObject = lookupNamedObject(name);
            if (namedObject != null) {
                object = namedObject.getObject();
            }
        }

        return object;
    }


    private NamedObject lookupNamedObject(final String name) {
        NamedObject namedObject = null;
        // check the cache first before accessing the database
        final HashEntry he = cache.get(name);
        if (he != null) {
            namedObject = he.getNamedObject();
        } else {
            final Extent extent = dbManager.getExtent(NamedObject.class, true);
            final Query query = dbManager.newQuery();
            query.setCandidates(extent);
            query.setFilter("this.name == \"" + name + "\"");
            query.compile();
            final Collection result = (Collection) query.execute();
            final Iterator it = result.iterator();
            if (it.hasNext()) {
                namedObject = (NamedObject) it.next();
                final Object object = namedObject.getObject();

                // add element to the cache
                if (object != null) {
                    final Object objectId = dbManager.getObjectId(object);

                    // add the element if the backend returns an valid objectid
                    if (objectId != null) {
                        final HashEntry heNew = new HashEntry(objectId);
                        heNew.setObject(object);
                        heNew.setNamedObject(namedObject);
                        cache.put(name, heNew);
                    }
                }
            }
            query.closeAll();

        }
        return namedObject;
    }

    /**
     * Returns a named object.
     *
     * @param name of the object to retrieve.
     * @return The object associated to the name, or null, if the name could not
     *         be found (or the object was null)
     */
    public Object lookup(final String name) {
        return lookupObject(name);
    }


    public void bind(final Object object, final String name) {
        final NamedObject namedObject = new NamedObject();
        namedObject.setName(name);
        namedObject.setObject(object);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Binding " + name + " as " + namedObject
                    + " oid=" + dbManager.getObjectId(object));
        }
        dbManager.makePersistent(namedObject);
        dbManager.checkpointTxn();

        // add element to the cache
        if ((name != null) && (object != null)) {
            final Object oid = dbManager.getObjectId(object);

            // add the element only, if the backend returns an valid objectid
            if (oid != null) {
                final HashEntry he = new HashEntry(oid);
                he.setObject(object);
                he.setNamedObject(namedObject);
                cache.put(name, he);
            } else {
                LOG.warn("No valid oid returned for \"" + name + "\"");
            }
        }
    }

    public void unbind(final String name) {
        final NamedObject namedObject = lookupNamedObject(name);

        if (namedObject != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unbinding " + name + " as " + namedObject
                        + " oid=" + dbManager.getObjectId(namedObject));
            }
            dbManager.delete(namedObject);

            // remove element from the cache
            if (name != null) {
                cache.remove(name);
            }
        } else {
            LOG.warn("Object \"" + name + "\" not found");
        }
    }

    /**
     * Removes the persistent object references from the cache This must be done
     * always after commiting a transaction. If we would not do this, we would
     * return java objcts, assigned to older transactions. Removing them does
     * not harm. It means simply that they are loaded again from the database.
     *
     * @param complete if true, then removes even the ObjectId references from
     *        the cache.
     */
    public void clearCache(final boolean complete) {
        if (!complete) {
            final PersistenceManager pm = dbManager.getPM();
            for (final HashEntry he : cache.values()) {
                synchronized (he) {
                    if (he.object != null) {
                        pm.evict(he.object);
                    } else {
                        he.clearObject();
                    }
                }
            }
        } else {
            cache.clear();
        }
    }

    private static final class HashEntry {
        private Object object;
        private final Object objectId;
        private NamedObject namedObject;

        public HashEntry(final Object objectId) {
            this.objectId = objectId;
        }

        public void setObject(final Object object) {
            this.object = object;
        }

        public void clearObject() {
            this.object = null;
        }

        public Object getObject(final DbManager dbm) {
            synchronized (this) {
                if (this.object == null) {
                    this.object = dbm.getObjectByObjectId(this.objectId);
                }
            }

            return this.object;
        }

        public Object getObjectId() {
            return this.objectId;
        }

        public NamedObject getNamedObject() {
            return this.namedObject;
        }

        public void setNamedObject(final NamedObject namedObject) {
            this.namedObject = namedObject;
        }
    }
}
