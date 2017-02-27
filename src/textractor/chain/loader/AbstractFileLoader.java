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

package textractor.chain.loader;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import textractor.chain.AbstractSentenceProducer;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.event.sentence.SentenceProcessingCompleteEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class that can be used to load articles from files and process
 * them into sentences that can be then further processed, indexed or stored
 * into a database via an appropriate
 * {@link textractor.sentence.SentenceConsumer} or
 * {@link textractor.sentence.SentenceProcessor}.
 */
public abstract class AbstractFileLoader extends AbstractSentenceProducer {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(AbstractFileLoader.class);

    /**
     * Total number of sentences processed by this loader.
     */
    private final AtomicInteger numberOfSentencesProcessed = new AtomicInteger();

    /**
     * Name of the file to load/process.
     */
    private String file;

    /**
     * Name of the file containing a list of filenames to load/process.
     */
    private String list;

    /**
     * Name of the directory to load/process.
     */
    private String directory;

    /**
     * Indicates that subdirectories should be processed as well.
     * Note: only applies if directory is set.
     */
    private boolean recursive;

    /**
     * A comma separated list of filename extensions used to filter which
     * files to process.
     */
    private String extensions;

    /**
     * The extensions to process (if any).
     */
    private final List<String> extensionList = new ArrayList<String>();

    /**
     * The number of times to iterate over a file or directory.
     */
    protected int numberOfIterations = 1;

    /**
     * The iteration currently in progress (should start at 1).
     */
    protected int currentIteration;

    /**
     * Name of the file to log the files as they are processed.  If
     * none is specified, no log is created.
     */
    private String processedFileLog;

    /**
     * The file to log the files as they are processed.  If
     * none is specified, no log is created.
     */
    private Writer processedFileLogWriter;

    /**
     * Indicates that each article should produce a single sentence with
     * all the article text.
     */
    protected boolean appendSentencesInOneDocument;

    /**
     * Separator to use when creating sentences in a single document.
     */
    protected static final String SENTENCE_BOUNDARY_TAG = " sentenceboundary ";

    /**
     * Separator to use between paragraphs in a document.
     */
    protected static final String PARAGRAPH_BOUNDARY_TAG = " paragraphboundary ";

    /**
     * This string will be placed between sentences.
     */
    protected String sentenceBoundary = " ";

    /**
     * This string will be placed between sentences.
     */
    protected String paragraphBoundary; // note - default of null is intentional

    /**
     * Create a new {@link textractor.sentence.SentenceProducer} that loads
     * sentences from files.
     */
    public AbstractFileLoader() {
        super();
    }

    /**
     * Processes all the files in a given directory.
     *
     * @param directory The name of the directory to process.
     * @throws IOException if there is a problem reading the directory
     *                     processing the files in the directory.
     */
    public final void processDirectory(final String directory)
            throws IOException {
        processDirectory(directory, TrueFileFilter.INSTANCE);
    }

    /**
     * Processes all the files in a given directory.
     *
     * @param extensions The file extensions to allow, must not be null
     * @param directory  The name of the directory to process.
     * @throws IOException if there is a problem reading the directory
     *                     processing the files in the directory.
     */
    public final void processDirectory(final String directory,
                                       final List<String> extensions) throws IOException {
        final IOFileFilter extensionFilter;
        // define a filter for the files based on the extensions
        if (extensions.isEmpty()) {
            // no extensions, don't filter anything
            extensionFilter = TrueFileFilter.INSTANCE;
        } else {
            extensionFilter = new SuffixFileFilter(extensions);
        }

        final FilenameFilter filter;
        // if recursive is specified, include directories too
        if (recursive) {
            filter = new OrFileFilter(extensionFilter,
                    DirectoryFileFilter.INSTANCE);
        } else {
            filter = extensionFilter;
        }
        processDirectory(directory, filter);
    }

    /**
     * Processes all the files that match a filter in a given directory.
     *
     * @param directory The name of the directory to process.
     * @param filter    Filename filter
     * @throws IOException if there is a problem reading the directory
     *                     processing the files in the directory.
     */
    public final void processDirectory(final String directory,
                                       final FilenameFilter filter) throws IOException {
        assert directory != null : "directory cannot be null";
        assert filter != null : "filter cannot be null";
        final File dir = new File(directory);
        final String[] filenames = dir.list(filter);

        if (filenames != null) {
            // sort the filenames in ascending order so that we ensure
            // they are loaded in the proper order.  The assumption is
            // of course that the files are always named in a way that
            // the names define the order.
            Arrays.sort(filenames);
            for (final String name : filenames) {
                final String filename = directory + File.separator + name;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Processing " + filename);
                }

                // parse subdirectories
                final File file = new File(filename);
                if (file.isDirectory()) {
                    if (recursive) {
                        processDirectory(filename, filter);
                    } else if (LOG.isDebugEnabled()) {
                        LOG.debug("Skipping directory " + filename);
                    }
                } else {
                    processFile(filename);
                }
            }
        } else {
            LOG.warn("No files found in " + directory);
        }
    }

    /**
     * Process a list of filenames. Lists can contain comments, which are ignored. Comments are any line that starts
     * with a '#' character.
     *
     * @param list The name of the file containing the list
     * @throws IOException if there is a problem reading the list or
     *                     processing the files in the list
     */
    public final void processFileList(final String list) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(list));
        try {
            String line;
            // process each filename in turn:
            while ((line = reader.readLine()) != null) {
                final String filename = line.trim();
                if (filename.length() == 0 || filename.charAt(0) == '#') {
                    //skip comments and empty lines.
                    continue;
                }
                processFile(filename);
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Logs the filename and invokes the appropriate #processFile method.
     *
     * @param filename The name of the file to process
     * @throws IOException if there is a problem reading the file.
     */
    private void processFile(final String filename)
            throws IOException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing " + filename + " (pass "
                    + currentIteration + " of " + numberOfIterations + ")");
        }
        if (processedFileLogWriter != null) {
            processedFileLogWriter.append(filename);
            processedFileLogWriter.append(SystemUtils.LINE_SEPARATOR);
            processedFileLogWriter.flush();
        }
        processFilename(filename);
    }

    /**
     * Process a single file designated by name.
     *
     * @param filename The name of the file to process.
     * @throws IOException if there is a problem reading the file.
     */
    public abstract void processFilename(final String filename)
            throws IOException;

    /**
     * Thread that will process a file, directory or list of files.
     *
     * @return true if processing completed with no problems
     * @throws IOException if there is a problem processing the file(s).
     */
    public final Boolean call() throws Exception {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug(BeanUtils.describe(this));
            }

            if (StringUtils.isNotEmpty(processedFileLog)) {
                final File processedFileLogFile = new File(processedFileLog);
                if (!processedFileLogFile.canWrite()) {
                    LOG.error("Can't write to " + processedFileLog);
                } else {
                    processedFileLogWriter = new BufferedWriter(new FileWriter(processedFileLogFile));
                }
            }

            // iterate over the directory, file or list
            for (currentIteration = 1; currentIteration <= numberOfIterations; currentIteration++) {
                beginIteration(currentIteration);
                if (file != null) {
                    processFile(file);
                } else if (list != null) {
                    processFileList(list);
                } else if (directory != null) {
                    processDirectory(directory, extensionList);
                } else {
                    LOG.error("No file or directory specified.");
                    return false;
                }

                endIteration(currentIteration);
            }
            LOG.debug("Processing complete");
            fireSentenceProcessingCompleteEvent();
        } catch (Throwable t) {
            LOG.fatal("Got an exception in thread " + this.getClass().getName(), t);
            Thread.currentThread().getThreadGroup().interrupt();
            if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw new Exception(t);
            }
        }
        return true;
    }

    /**
     * Produce a new Sentence.
     *
     * @param article The article the sentence will be associated with.
     * @param text    The text to be used for the sentence.
     * @return The sentence object based on the article and text sequence.
     */
    public final Sentence produce(final Article article,
                                  final CharSequence text) {
        final Sentence sentence = new Sentence(article, text.toString());
        final long sentenceNumber =
                numberOfSentencesProcessed.getAndIncrement();
        sentence.setDocumentNumber(sentenceNumber);
        if (LOG.isTraceEnabled()) {
            final long articleNumber = article.getArticleNumber();
            LOG.trace("[" + articleNumber + ", " + sentenceNumber + "] "
                    + sentence.getText().substring(0,
                    Math.min(50, sentence.getText().length())));
        }
        return sentence;
    }

    /**
     * Get the number of sentences processed so far.
     *
     * @return The number of sentences processed so far
     */
    public int getNumberOfSentencesProcessed() {
        return numberOfSentencesProcessed.get();
    }

    /**
     * Get the name of the directory to process.
     *
     * @return the name of the directory to process
     */
    public final String getDirectory() {
        return directory;
    }

    /**
     * Set the name of the directory to process.
     *
     * @param name the name of the directory to process
     */
    public final void setDirectory(final String name) {
        this.directory = name;
        if (this.directory.startsWith("TMPDIR/")) {
            // Support for the cluster
            this.directory = this.directory.replace("TMPDIR", System.getenv("TMPDIR"));
        }
    }

    /**
     * Get the name of the file to process.
     *
     * @return the name of the file to process
     */
    public final String getFile() {
        return file;
    }

    /**
     * Set the name of the file to process.
     *
     * @param name the name of the file to process
     */
    public final void setFile(final String name) {
        this.file = name;
        if (this.file.startsWith("TMPDIR/")) {
            // Support for the cluster
            this.file = this.file.replace("TMPDIR", System.getenv("TMPDIR"));
        }
    }

    /**
     * Get the name of the file containing the list of files to process.
     *
     * @return the name of the file to process
     */
    public final String getList() {
        return list;
    }

    /**
     * Set the name of the file containing the list of files to process.
     *
     * @param name the name of the file to process
     */
    public final void setList(final String name) {
        this.list = name;
    }

    /**
     * Should subdirectories be processed as well as files.
     *
     * @return true if subdirectories should be processed
     */
    public boolean isRecursive() {
        return recursive;
    }

    /**
     * Indicated whether subdirectories should be processed as well as files.
     *
     * @param value true if subdirectories should be processed
     */
    public void setRecursive(final boolean value) {
        this.recursive = value;
    }

    /**
     * Get the filename extensions used to filter which files to process.
     *
     * @return A comma separated list of filename extensions
     */
    public final String getExtensions() {
        return extensions;
    }

    /**
     * Set the filename extensions used to filter which files to process.
     *
     * @param extensionString A comma separated list of filename extensions
     */
    public final void setExtensions(final String extensionString) {
        assert extensionString != null : "Extension list cannot be null";
        this.extensions = extensionString;
        extensionList.clear();
        final StringTokenizer tokens =
                new StringTokenizer(extensionString, ",");
        while (tokens.hasMoreTokens()) {
            final String extension = tokens.nextToken().trim();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Processing extension " + extension);
            }
            if (StringUtils.isNotEmpty(extension)) {
                extensionList.add(extension);
            } else {
                LOG.warn("Ignoring extension " + extension);
            }
        }
    }

    /**
     * Get the filename extensions used to filter which files to process.
     *
     * @return A comma separated list of filename extensions
     */
    public final List<String> getExtensionList() {
        return extensionList;
    }

    /**
     * Get the name (if any) of the file where the names of the files
     * processed are written to.
     *
     * @return A filename or null.
     */
    public final String getProcessedFileLog() {
        return processedFileLog;
    }

    /**
     * Set the name of the file where the names of the files
     * processed are written to.
     *
     * @param name The name of the file to write to.
     */
    public final void setProcessedFileLog(final String name) {
        this.processedFileLog = name;
    }

    /**
     * Called at the start of an iteration.  Interested parties should
     * override this.
     * @param iteration The iteration that just completed
     */
    protected void beginIteration(final int iteration) {
    }

    /**
     * Called at the end of an iteration.  Interested parties should
     * override this.
     * @param iteration The iteration that just completed
     */
    protected void endIteration(final int iteration) {
    }

    /**
     * This method gets called when a sentence processing is complete.
     *
     * @param event A {@link textractor.event.sentence.SentenceProcessingCompleteEvent} object describing
     *              the event source.
     */
    @Override
    public void processingComplete(final SentenceProcessingCompleteEvent event) {
        LOG.debug("Got completion from: " + event.getSource());
        if (event.getSource() == this) {
            IOUtils.closeQuietly(processedFileLogWriter);
        }
        super.processingComplete(event);
    }

    public boolean isAppendSentencesInOneDocument() {
        return appendSentencesInOneDocument;
    }

    public void setAppendSentencesInOneDocument(final boolean value) {
        this.appendSentencesInOneDocument = value;
    }

    /**
     * Get the string that will be placed between paragraphs.
     * @return the String that will be placed between paragraphs
     */
    public String getParagraphBoundaryTag() {
        return paragraphBoundary;
    }

    /**
     * Set the string that will be placed between paragraphs.
     * @param boundaryString the String that will be placed between paragraphs
     */
    public void setParagraphBoundary(final String boundaryString) {
        this.paragraphBoundary = boundaryString;
    }

    /**
     * Get the string that will be placed between sentences.
     * @return the String that will be placed between sentences
     */
    public String getSentenceBoundary() {
        return sentenceBoundary;
    }

    /**
     * This string will be placed between sentences.
     * If an empty string or null is specified
     * a single space will be used, otherwise it will
     * insert the specified String (padded by a single space on both
     * sides, even if the padding isn't manually specified or the
     * string is over-padded).
     * @param boundaryString the String that should be placed between sentences
     */
    public void setSentenceBoundary(final String boundaryString) {
        this.sentenceBoundary = padWithSpaces(boundaryString);
    }

    public static String padWithSpaces(final String stringToPad) {
        if (stringToPad == null) {
            return " ";
        } else {
            final String trimmedStr = stringToPad.trim();
            if (trimmedStr.length() == 0) {
                return " ";
            } else {
                return " " + trimmedStr + " ";
            }
        }
    }
}
