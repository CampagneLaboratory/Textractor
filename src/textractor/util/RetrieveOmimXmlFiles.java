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

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import ronaldTschalr.UncompressInputStream;

import java.io.*;
import java.net.ConnectException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

/**
 * Retrieve the OMIM XML files.
 * Some key factors....
 * * If fetching more than 100 records this should only
 *   work between 9pm and 5am OR on weekends.
 * * It should never request more than one record
 *   per 3 seconds.
 * * The MIM document numbers can come from
 *     ftp://ftp.ncbi.nih.gov/repository/OMIM/omim.txt.Z
 *   from
 *      *RECORD*
 *      *FIELD* NO
 *      <the record number here>
 * * The file omim.txt.Z is about 50 megabytes, 118mb uncompressed.
 * @author Kevin Dorff
 */
public class RetrieveOmimXmlFiles {

    /**
     * Used to log meesages with logger.
     */
    private static final Log LOG = LogFactory.getLog(RetrieveOmimXmlFiles.class);

    /* -- Other constants -------------------------------------------------- */

    private static final String FTP_SERVER = "ftp.ncbi.nih.gov";

    private static final String FTP_DIR = "/repository/OMIM";

    private static final String FTP_FILE = "omim.txt.Z";

    private static final String FTP_USERNAME = "ftp";

    private static final String FTP_PASSWORD = "icb@med.cornell.edu";

    /**
     * If there is a ConnectException retrieving the
     * XML file, it will be retried this many times.
     */
    private static final int NUM_RETRY_TIMES = 5;

    /**
     * If there is a ConnectException retrieving the
     * XML file, it will wait this many ms between
     * tries (30 seconds).
     */
    private static final int RETRY_WAIT_MS = 30 * 1000;

    /* -- Properties ------------------------------------------------------- */

    /**
     * The OMIM text file to use to get the MIM numbers to know
     * which XML files to retrieve. The default is "-" which means
     * it will FTP the file down, placing it in the output directory
     * (null or empty string will have the same effect as "-").
     */
    private String omimFile;
    private static final String OMIM_FILE_DEFAULT = "-";
    /**
     * The directory to write the XML files.
     * The current directory is the default.
     */
    private String outputDir;
    private static final String OUTPUT_DIR_DEFAULT = ".";

    /**
     * The time to wait between retrieving XML files
     * in ms. 4 seconds is the default.
     */
    private int queryDelayMs;
    private static final int QUERY_DELAY_MS_DEFAULT = 4000;

    /**
     * The max number of XML files to retrieve.
     * Default is -1 which is defined as unlimited between 9pm and 8am
     * and on weekends or 10 otherwise.
     */
    private int queryLimit;
    private static final int QUERY_LIMIT_DEFAULT = -1;

    /**
     * Set to true to start wait until "safe time" to start
     * executing queries (weekends or 9pm - 5am). This will also
     * check WHILE the program is running that each query is
     * executing during "safe time".
     */
    private boolean safeTimeWait;
    private static final boolean SAFE_TIME_WAIT_DEAFAULT = false;

    /**
     * Set to true to overwrite XML files or false
     * to just skip fetching an XML if it exists.
     */
    private boolean overwrite;
    private static final boolean OVERWRITE_DEAFAULT = false;

    /* -- Properties ------------------------------------------------------- */

    /**
     * Constructor.
     */
    public RetrieveOmimXmlFiles() {
        this.outputDir = OUTPUT_DIR_DEFAULT;
        this.queryDelayMs = QUERY_DELAY_MS_DEFAULT;
        this.queryLimit = QUERY_LIMIT_DEFAULT;
        this.safeTimeWait = SAFE_TIME_WAIT_DEAFAULT;
        this.overwrite = OVERWRITE_DEAFAULT;
    }

    /* -- Set Properties --------------------------------------------------- */

    /**
     * Specify the output directory
     * @param outputDir the output directory
     */
    public void setOutputDir(final String outputDir) {
        this.outputDir = outputDir.replaceAll("\\\\", "/");
        if (!this.outputDir.endsWith("/")) {
            this.outputDir += "/";
        }
    }

    /**
     * Specify the omim-file (omim.txt.Z).
     * @param omimFile the omim-input file
     */
    public void setOmimFile(final String omimFile) {
        this.omimFile = omimFile;
    }


    /**
     * The delay between queries in milliseconds.
     * Default is 4000 (4 seconds)
     * @param queryDelayMs the delay between queries
     */
    public void setQueryDelayMs(final int queryDelayMs) {
        this.queryDelayMs = queryDelayMs;
    }

    /**
     * The limit to the number of queries. The default is
     * -1, which is defined as unlimited between 9pm and 8am
     * and on weekends or 10 otherwise.
     * Default is 3000 (3 seconds)
     * @param queryLimit the delay between queries
     */
    public void setQueryLimit(final int queryLimit) {
        this.queryLimit = queryLimit;
    }

    /**
     * Set to true to start wait until "safe time" to start
     * executing queries (weekends or 9pm - 5am). This will also
     * check WHILE the program is running that each query is
     * executing during "safe time".
     * @param safeTimeWait true if safe time wait should be enabled
     */
    public void setSafeTimeWait(final boolean safeTimeWait) {
        this.safeTimeWait = safeTimeWait;
    }

    /**
     * Set if XML files should be downloaded AGAIN if they already
     * exist. Default is false, they will be skipped if they exist
     * in the output dir.
     * @param overwrite true if files that exist should be overwritten
     */
    public void setOverwrite(final boolean overwrite) {
        this.overwrite = overwrite;
    }

    /* -- Actual work ------------------------------------------------------ */

    private static boolean isSafeTime() {
        final Calendar cal = Calendar.getInstance();
        final int day = cal.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            return true;
        }
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 21 || hour < 5;
    }

    private void waitForSafeTime() throws InterruptedException {
        while (true) {
            if (isSafeTime()) {
                return;
            }
            System.out.println("Waiting for safe time. " + new Date());
            // Try again in two minutes.
            Thread.sleep(120 * 1000);
        }
    }

    private void waitForNextQuery() throws InterruptedException {
        Thread.sleep(queryDelayMs);
    }

    private File ftpOmimFile() throws IOException {
        System.out.println("Retrieving OMIM file from FTP");
        final File omimFileFile = new File(outputDir + "omim.txt.Z");
        if (omimFileFile.exists()) {
            omimFileFile.delete();
        }

        // FTP the file
        final FTPClient ftp = new FTPClient();
        ftp.enterLocalActiveMode();
        ftp.connect(FTP_SERVER);
        ftp.login(FTP_USERNAME, FTP_PASSWORD);
        ftp.changeWorkingDirectory(FTP_DIR);
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        final OutputStream output = new FileOutputStream(omimFileFile);
        ftp.retrieveFile(FTP_FILE, output);
        IOUtils.closeQuietly(output);
        ftp.disconnect();
        return omimFileFile;
    }

    private IntList retrieveOmimNumbers() throws IOException {
        final File omimFileFile;
        if (StringUtils.isBlank(omimFile) || omimFile.equals("-")) {
            omimFileFile = ftpOmimFile();
        } else {
            omimFileFile = new File(omimFile);
        }

        System.out.println("Scanning OMIM file for MIM numbers");
        final LineIterator lineIterator = IOUtils.lineIterator(
                new InputStreamReader(
                        new UncompressInputStream(
                                new FileInputStream(omimFileFile))));

        final IntList otmiNumbers = new IntArrayList();
        boolean theEndFound = false;
        try {
            boolean keepNext = false;
            String line;
            int lineNum = 0;
            while (lineIterator.hasNext()) {
                line = lineIterator.nextLine();
                lineNum++;
                if (keepNext) {
                    keepNext = false;
                    final int num = NumberUtils.toInt(line, -1);
                    if (num != -1) {
                        otmiNumbers.add(num);
                    } else {
                        // Some major parsing error occurred. Report and die.
                        System.out.printf(
                                "Cannot determine MIM number within '%s' on line %d\n",
                                line, lineNum);
                        System.exit(-1);
                    }
                }
                if (line.equals("*FIELD* NO")) {
                    keepNext = true;
                } else if (line.equals("*THEEND*")) {
                    theEndFound = true;
                }
            }
        } finally {
            LineIterator.closeQuietly(lineIterator);
        }

        if (!theEndFound) {
            System.out.println(
                    "The transferred OMIM file was incomplete," +
                            "*THEEND* not found.");
            System.exit(-1);
        }

        return otmiNumbers;
    }

    private boolean retrieveXml(final int mimNumber) throws IOException, InterruptedException {
        final String url = "http://eutils.ncbi.nlm.nih.gov/entrez/dispomim.cgi?id="+mimNumber+"&cmd=xml&tool=icb&email=icb.at.med.cornell.edu";
        final File outFile = new File(outputDir + mimNumber + ".xml");
        if (outFile.exists() && !overwrite) {
            LOG.info("File " + outFile.getName() +
                    " already existed. Skipping.");
            return false;
        }
        for (int tryNum = 0; tryNum < NUM_RETRY_TIMES; tryNum++) {
            try {
                FileUtils.copyURLToFile(new URL(url), outFile);
                LOG.info("Retrieved file " + outFile.getName());
                waitForNextQuery();
                return true;
            } catch (ConnectException e) {
                LOG.error("Problem retrieving file " +
                        outFile.getName() +
                        " will try " + NUM_RETRY_TIMES + " files");
                Thread.sleep(RETRY_WAIT_MS);
            }
        }
        return false;
    }

    public void start() throws IOException, InterruptedException {

        if (queryLimit == -1) {
            if (!safeTimeWait) {
                if (!isSafeTime()) {
                    queryLimit = 10;
                }
            }
        }

        System.out.println("RetrieveOmimXmlFiles...");
        final IntList omimNumbers = retrieveOmimNumbers();
        System.out.printf("OMIM File is %s.\n", omimFile);
        System.out.printf("Output dir is %s.\n", outputDir);
        System.out.printf("Query Limit is %d.\n", queryLimit);
        System.out.printf("Located %d OMIM numbers.\n", omimNumbers.size());

        int count = 0;
        for (final int omimNumber : omimNumbers) {
            if (queryLimit != -1) {
                if (count >= queryLimit) {
                    break;
                }
            }
            if (safeTimeWait) {
                waitForSafeTime();
            }

            // query
            if (retrieveXml(omimNumber)) {
                System.out.println("Retrieved " + omimNumber);
                count++;
            } else {
                System.out.println("Skipped " + omimNumber);
            }
        }
        System.out.println("Retrieved " + count + " XML files");
    }

    public static void main(final String[] args) throws IOException, InterruptedException {
        final RetrieveOmimXmlFiles work = new RetrieveOmimXmlFiles();
        work.setOmimFile(CLI.getOption(args, "-omim-file", OMIM_FILE_DEFAULT));
        work.setOutputDir(CLI.getOption(args, "-output-dir", OUTPUT_DIR_DEFAULT));
        work.setQueryLimit(CLI.getIntOption(args, "-limit", QUERY_LIMIT_DEFAULT));
        work.setQueryDelayMs(CLI.getIntOption(args, "-delay-ms", QUERY_DELAY_MS_DEFAULT));
        work.setSafeTimeWait(CLI.isKeywordGiven(args, "-safe-time-wait", SAFE_TIME_WAIT_DEAFAULT));
        work.setOverwrite(CLI.isKeywordGiven(args, "-overwrite", OVERWRITE_DEAFAULT));

        work.start();
    }

}

