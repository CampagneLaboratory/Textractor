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

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Global Property Manager. Singleton Property Manager for storing run-time
 * properties.
 *
 * @author Ethan Cerami
 * @version $Revision: 21593 $
 */
public final class PropertyManager extends Properties {
    private static final Log LOG = LogFactory.getLog(PropertyManager.class);

    private static PropertyManager manager;

    /**
     * Construct a new property manager.
     *
     * @param properties Current list of system properties
     * @param filename Name of the file to read properties from
     */
    private PropertyManager(final Properties properties,
                            final String filename) {
        super(properties);
        initProperties(filename);
    }

    /**
     * Gets an Instance of the Property Manager using a given property file.
     *
     * @param filename Name of the file to read properties from
     * @return a PropertyManager instance
     */
    public static synchronized PropertyManager getInstance(
            final String filename) {
        if (manager == null) {
            manager = new PropertyManager(System.getProperties(), filename);
        }

        return manager;
    }

    /**
     * Initialize Property Manager with configuration properties. Properties
     * are read in config/textractor.properties This file should contain
     * Java properties for textractor. A list of valid properties is available
     * at TODO: create documentation for textractor properties/params.
     *
     * The javax.jdo.option.ConnectionURL property can be overriden with the
     * value of the FODB environment variable (syntax is something like
     * FastObjects://localhost/textractor_dev for a development database
     * running on the local machine).
     *
     * @param filename Name of the file to read properties from
     */
    private void initProperties(final String filename) {
        final String propertyFilename =
            "config" + File.separator + filename;

        // load properties from file if started from command line:
        try {
            final FileInputStream fis = new FileInputStream(propertyFilename);
            this.load(fis);
            LOG.trace("propertyfile " + propertyFilename + " loaded.");
            fis.close();
        } catch (final IOException e) {
            // now try loading it if started as a web application from tomcat
            try {
                LOG.warn("Cannot open " + propertyFilename + " file.");
                LOG.warn("trying other location...");
                final InputStream is =
                        Thread.currentThread().getContextClassLoader()
                                .getResourceAsStream(propertyFilename);
                this.load(is);
                LOG.trace("propertyfile " + propertyFilename + " loaded.");
                is.close();
            } catch (final IOException e2) {
                LOG.error("Cannot open " + propertyFilename + " file.", e);
                LOG.error("Cannot open " + propertyFilename + " file.", e2);
            }
        }
    }

    public String getEnvVar(final String varName, final String def) {
        // TODO - this should only get done once, not each time for every var
        String envVar = null;
        BufferedReader br = null;
        try {
            final Process p;
            final Properties envVars = new Properties();
            final Runtime r = Runtime.getRuntime();
            final String OS = System.getProperty("os.name").toLowerCase();
            if (OS.contains("windows 9")) {
                p = r.exec("command.com /c set");
            } else if ((OS.contains("nt"))
                    || (OS.contains("windows 2000"))
                    || (OS.contains("windows xp"))) {
                p = r.exec("cmd.exe /c set");
            } else {
                // our last hope, we assume Unix (thanks to H. Ware for the fix)
                p = r.exec("env");
            }

            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String var = null;
            String line;
            final String lineSep = System.getProperty("line.separator");

            while ((line = br.readLine()) != null) {
                if (line.indexOf('=') == -1) {
                    // Chunk part of previous env variable
                    // (UNIX env vars can contain embedded new lines)
                    if (var == null) {
                        var = lineSep + line;
                    } else {
                        var += lineSep + line;
                    }
                } else {
                    // New env var...append the previous one if we have it.
                    if (var != null) {
                        final int pos = var.indexOf('=');
                        if (pos == -1) {
                            LOG.warn("Ignoring: " + var);
                        } else {
                            final String key = var.substring(0, pos);
                            final String value = var.substring(pos + 1);
                            envVars.setProperty(key, value);
                        }
                    }
                    var = line;
                }
            }

            // Since we "look ahead" before adding, there's one last env var.
            if (var != null) {
                final int pos = var.indexOf('=');
                if (pos == -1) {
                    LOG.warn("Ignoring: " + var);
                } else {
                    final String key = var.substring(0, pos);
                    final String value = var.substring(pos + 1);
                    LOG.debug("adding: (" + key + ", " + value + ")");
                    envVars.setProperty(key, value);
                }
            }

            envVar = envVars.getProperty(varName, def);
        } catch (final IOException e) {
            LOG.error("Caught exception while reading enviornment", e);
        } finally {
            IOUtils.closeQuietly(br);
        }
        return envVar;
    }
}
