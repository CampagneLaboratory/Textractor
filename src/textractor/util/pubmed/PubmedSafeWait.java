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

package textractor.util.pubmed;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;
import java.util.Date;

/**
 * Pubmed-Entrez asked that you only perform bulk queries at
 * certain times of day / days of week / and with a certain
 * frequency. This class helps you comply with those requests
 * more easily.
 * @author Kevin Dorff (Sep 20, 2007)
 */
public class PubmedSafeWait {
    /**
     * Used to log meesages with logger.
     */
    private static final Log LOG = LogFactory.getLog(PubmedSafeWait.class);

    /**
     * The time to wait between retrieving XML files in ms.
     */
    private int queryDelayMs;

    /** 4 seconds is the default delay time. */
    private static final int QUERY_DELAY_MS_DEFAULT = 4000;

    /** Delay when waiting for safe time, 15 minutes. */
    private static final int WAIT_SAFE_TIME_PAUSE = 15 * 60 * 1000;

    /**
     * Default contructor. Sets queryDelayMs
     * to the default of 4000 (4s).
     */
    public PubmedSafeWait() {
        this.queryDelayMs = QUERY_DELAY_MS_DEFAULT;
    }

    /**
     * Constructor.
     * @param queryDelayMs delay between queries
     */
    public PubmedSafeWait(final int queryDelayMs) {
        this.queryDelayMs = queryDelayMs;
    }

    /**
     * Set the (minimum) delay between queries in milliseconds,
     * ie the minimum delay when calling waitForNextQuery().
     * Default is 4000 (4 seconds). If you set to a
     * value < 0, the default will be used.
     * @param queryDelayMs the delay between queries
     */
    public void setQueryDelayMs(final int queryDelayMs) {
        if (queryDelayMs < 0) {
            this.queryDelayMs = QUERY_DELAY_MS_DEFAULT;
        } else {
            this.queryDelayMs = queryDelayMs;
        }
    }

    /**
     * Get the minimum delay between queries in milliseconds,
     * ie the minimum delay when calling waitForNextQuery().
     * Default is 4000 (4 seconds).
     * @return the delay between queries
     */
    public int getQueryDelayMs() {
        return this.queryDelayMs;
    }

    /**
     * Returns true if it is currently "safe time".
     * Safe time is any time on Saturday or Sunday.
     * Otherwise safe time is 9pm - 5am.
     * @return true if safe time, false otherwise.
     */
    public static boolean isSafeTime() {
        final Calendar cal = Calendar.getInstance();
        final int day = cal.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            return true;
        }
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 21 || hour < 5;
    }

    /**
     * This method will not return until it is "safe time".
     * @throws InterruptedException
     */
    public void waitForSafeTime() throws InterruptedException {
        while (true) {
            if (isSafeTime()) {
                return;
            }
            // Not a safe time. Try again in fifteen minutes.
            LOG.info("Waiting for safe time. Current time is " + new Date());
            Thread.sleep(WAIT_SAFE_TIME_PAUSE);
        }
    }

    /**
     * This method will wait at least queryDelayMs
     * and will not return until "safe time".
     * @throws InterruptedException
     */
    public void waitForNextSafeQuery() throws InterruptedException {
        Thread.sleep(queryDelayMs);
        waitForSafeTime();
    }

    /**
     * This method will wait at least queryDelayMs
     * and return, not worrying about "safe time".
     * @throws InterruptedException
     */
    public void waitForNextQuery() throws InterruptedException {
        Thread.sleep(queryDelayMs);
    }

}
