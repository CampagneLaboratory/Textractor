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

package textractor.html;

import edu.mssm.crover.cli.CLI;
import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.BulletParser;
import it.unimi.dsi.mg4j.util.parser.ParsingFactory;
import it.unimi.dsi.mg4j.util.parser.WellFormedXmlFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static textractor.TextractorConstants.TIMER_LOG;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;
import textractor.parsers.PubmedExtractor;
import textractor.parsers.PubmedLoadExtractor;
import textractor.parsers.PubmedLoader;

import javax.jdo.JDOUserException;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Parse XML Pubmed abstracts and load their text into the database.
 */
public final class PubmedAbstracts2Text2DB extends Html2Text2DB
        implements PubmedLoader, CheckpointCallback, Closeable {
    /**
     * Used to LOG debug and informational messages.
     */
    private static final Log LOG =
            LogFactory.getLog(PubmedAbstracts2Text2DB.class);
    private Long2ObjectMap<Article> articleSubmittedInTransaction;
    private Long2ObjectMap<Article> articleDeletedInTransaction;
    private String articleElementName;

    private int cumulativeCounter;

    private XMLConfiguration config;
    private String configFile;

    /**
     * By default, this class will only allow a file to be loaded once.  If this
     * is set to true, then a file may be loaded more than once.
     */
    private boolean forceLoad;

    public PubmedAbstracts2Text2DB(final String[] args) throws TextractorDatabaseException {
        super(args);
        articleSubmittedInTransaction = new Long2ObjectRBTreeMap<Article>();
        articleDeletedInTransaction = new Long2ObjectRBTreeMap<Article>();
        config = new XMLConfiguration();
        config.setRootElementName("medline");
        configFile = "medline.xml";
        config.setFileName(configFile);
    }

    /**
     * Process abstracts with the given arguments.
     *
     * @param args Arguments used to process the abstracts
     * @throws java.io.IOException if there is a problem reading the abstracts
     *                             specified by the arguments
     * @throws org.apache.commons.configuration.ConfigurationException
     *                             if there is a problem with the arguments
     */
    @Override
    public void process(final String[] args) throws IOException {
        final String filename = CLI.getOption(args, "-i", null);
        final String list = CLI.getOption(args, "-list", null);
        final String directory = CLI.getOption(args, "-d", null);
        appendSentencesInOneDocument = CLI.isKeywordGiven(args, "-sentence-markup");
        noSentenceBoundaryTag = CLI.isKeywordGiven(args, "-no-sentenceboundary-tag");

        if (appendSentencesInOneDocument) {
            LOG.debug("-sentence-markup flag activated");
        }
        if (noSentenceBoundaryTag) {
            LOG.debug("-no-sentenceboundary-tag flag activated");
        }

        if (filename == null && list == null && directory == null) {
            System.err.println("usage: -i input | -list filename | -d directory.");
            System.exit(1);
        }

        articleElementName =
                CLI.getOption(args, "-article-element-name", null);

        configFile = CLI.getOption(args, "-config", configFile);
        config.setFileName(configFile);

        forceLoad = CLI.isKeywordGiven(args, "-force");

        System.out.println("processing with:");
        System.out.println("  filename: " + filename);
        System.out.println("  list: " + list);
        System.out.println("  directory: " + directory);
        System.out.println("  config: " + configFile);
        System.out.println("  element name: " + articleElementName);
        System.out.println("  chunk size: " + articleChunkSize);

        loadConfig();

        if (filename != null) {
            processFilename(filename);
        }

        if (list != null) {
            // process the list of filenames:
            final BufferedReader reader =
                new BufferedReader(new FileReader(list));
            try {
                String line;
                // process each filename in turn:
                while ((line = reader.readLine()) != null) {
                    processFilename(line.trim());
                }
            } finally {
                reader.close();
            }
        }

        if (directory != null) {
            final File dir = new File(directory);

            // get all compressed xml files files the specified directory
            final FilenameFilter filter = new FilenameFilter() {
                public boolean accept(final File dir, final String name) {
                    return name.endsWith(".xml.zip")
                            || name.endsWith(".xml.gz");
                }
            };
            final String[] filenames = dir.list(filter);
            // sort the filenames in ascending order so that we ensure
            // they are loaded in the proper order.  The assumption is
            // of course that the files are always named in a way that
            // the names define the order.
            Arrays.sort(filenames);
            for (final String file : filenames) {
                processFilename(directory + File.separator + file);
            }
        }
    }

    /**
     * Load the current configuration.
     */
    private void loadConfig() {
        try {
            config.load();
        } catch (final ConfigurationException e) {
            LOG.warn("property file not loaded, creating a new file", e);
        }
    }

    /**
     * Add the loaded file into the current configuration.
     *
     * @param filename Name of the file to add.
     * @throws ConfigurationException if the file cannot be updated.
     */
    private void updateConfig(final String filename)
            throws ConfigurationException {
        config.addProperty("files.file(-1).name", filename);
        config.addProperty("files.file.date", new Date());
        config.addProperty("files.file.abstracts",
                consumer.getNumberOfArticlesProcessed());
        config.save();
    }

    private void processFilename(
            final String filename) throws IOException {
        if (isFileLoaded(config, filename)) {
            if (!forceLoad) {
                LOG.info("Skipping " + filename
                        + " since it has already been loaded.");
                return;
            } else {
                LOG.warn("Possibly reloading " + filename);
            }
        }

        final StopWatch timer = new StopWatch();
        timer.start();
        LOG.info("Scanning " + filename);

        final InputStream stream;
        if (filename.endsWith(".gz") || filename.endsWith(".zip")) {
            stream = new GZIPInputStream(new FileInputStream(filename));
        } else {
            stream = new FileInputStream(filename);
        }

        final FastBufferedReader reader =
                new FastBufferedReader(new InputStreamReader(stream));
        char[] buffer = new char[10000];

        // read the whole file in memory:
        int length;
        int offset = 0;

        while ((length = reader.read(buffer, offset, buffer.length - offset)) > 0) {
            offset += length;
            buffer = CharArrays.grow(buffer, offset + 1);
        }

        // DO NOT TRIM the buffer. Triming allocates a new buffer and copies the
        // result in the new one. This does in fact
        // use more memory transiently and result in more garbage collection.

        // and close up stuff we don't need anymore
        reader.close();
        stream.close();

        final ParsingFactory factory = new WellFormedXmlFactory();
        final BulletParser parser = new BulletParser(factory);

        final PubmedExtractor pubmedExtractor =
                new PubmedLoadExtractor(this, filename);

        if (articleElementName != null) {
            pubmedExtractor.setArticleElementName(articleElementName);
            LOG.info("Parsing elements " + articleElementName);
        }
        parser.setCallback(pubmedExtractor);

        // parse and collect abbreviations:
        consumer.begin();

        parser.parse(buffer, 0, offset);
        consumer.end();

        timer.stop();
        if (LOG.isDebugEnabled()) {
            LOG.debug(timer +  " : " + filename);
        }
        if (TIMER_LOG.isInfoEnabled()) {
            TIMER_LOG.info(timer +  " : " + filename);
        }

        final int counter = consumer.getNumberOfArticlesProcessed();
        cumulativeCounter += counter;
        LOG.info("Loaded " + counter + " abstracts (cumulative: "
                + cumulativeCounter + ") ");

        try {
            updateConfig(filename);
        } catch (final ConfigurationException e) {
            LOG.error("Error updating configuration file", e);
        }
    }


    public void convert(final MutableString pmid, final MutableString title,
            final MutableString text, final Map<String, Object> additionalFieldsMap,
            final String filename) {
        final long PMID = Long.parseLong(pmid.toString());

        if (LOG.isDebugEnabled()) {
            LOG.debug("converting " + PMID + " in " + filename);
        }

        Article article = findArticle(PMID);
        if (article != null) {
            removePreviousArticle(article, PMID);
        }

        article = createArticle(PMID, filename);
        try {
            final Collection<Sentence> sentences = loadArticleSentences(article,
                    title.toString(), text.toString(), additionalFieldsMap);
            consumer.consume(article, sentences);
        } catch (final Exception e) {
            LOG.error("Exception thrown in loadArticleSentences", e);
            System.exit(10);
        }
    }

    public void checkpointCallback() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Commiting transaction. counter = " + consumer.getNumberOfArticlesProcessed()
                    + ", chunkSize = " + articleChunkSize);
        }
        articleDeletedInTransaction.clear();
        articleSubmittedInTransaction.clear();
    }

    private Article findArticle(final long pmid) {
        // first check in article added during this transaction:
        final Article article = articleSubmittedInTransaction.get(pmid);
        if (article != null) {
            return article;
        }

        if (articleDeletedInTransaction.get(pmid) != null) {
            return null;
        }

        // if not found, check in the database:
        return consumer.getDbManager().getTextractorManager().getArticleByPmid(pmid);
    }


    public Article createArticle(final long pmid,
                                 final String filename) {
        final Article article = consumer.createArticle();
        article.setPmid(pmid);
        article.setFilename(filename);
        articleSubmittedInTransaction.put(pmid, article);
        articleDeletedInTransaction.remove(pmid);
        return article;
    }

    /**
     * Removes article and associated sentences. Medline files are organized so
     * that revised entries appear after previous entries.
     *
     * @param article Article to be removed.
     * @param pmid identifier of article to remove
     */
    private void removePreviousArticle(final Article article, final long pmid) {
        if (!articleDeletedInTransaction.containsKey(pmid)) {
            articleSubmittedInTransaction.remove(article.getPmid());
            try {
                consumer.getDbManager().getTextractorManager().deleteArticle(article);
                articleDeletedInTransaction.put(pmid, article);
            } catch (final JDOUserException e) {
                LOG.info("article could not be deleted because it was not persistent: "+pmid);
            }
        }
    }

    public void removeArticle(final String retractedPmid) {
        final long retractedPMID = Long.parseLong(retractedPmid);
        final Article article = findArticle(retractedPMID);
        if (article != null) {
            removePreviousArticle(article, retractedPMID);
        } else {
            LOG.warn("Cannot process notice of retraction. Article for retracted "
                    + "article cannot be found with PMID " + retractedPmid);
        }
    }

    /**
     * Determines whether or not a file has already been loaded in the database.
     * Note that the path information is ignored, only the short name is used.
     *
     * @param configuration Prior configuration to check against
     * @param filename Name of the file to check
     * @return true if the file has already been loaded
     */
    public static boolean isFileLoaded(final Configuration configuration,
            final String filename) {
        assert filename != null;

        final boolean loaded;
        final Object property = configuration.getProperty("files.file.name");
        // it's more likely that we'll be dealing with Collection, so test first
        if (property instanceof Collection) {
            loaded = ((Collection) property).contains(filename);
        } else {
            loaded = property instanceof String && filename.equals(property);
        }
        return loaded;
    }

    public static void main(final String[] args)
            throws TextractorDatabaseException, IOException, ConfigurationException {
        final PubmedAbstracts2Text2DB converter = new PubmedAbstracts2Text2DB(args);
        converter.process(args);
    }

    public void close() {
        consumer.getDbManager().shutdown();
    }
}
