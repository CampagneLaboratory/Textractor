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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import textractor.datamodel.Article;
import textractor.datamodel.FeatureCreationParameters;
import textractor.datamodel.OtmiArticle;
import textractor.datamodel.PaddingDocument;
import textractor.datamodel.RunningCounter;
import textractor.datamodel.Sentence;
import textractor.datamodel.TextractorDocument;
import textractor.datamodel.TextractorInfo;
import textractor.datamodel.annotation.AnnotationSource;
import textractor.datamodel.annotation.DoubleTermAnnotation;
import textractor.datamodel.annotation.SingleTermAnnotation;
import textractor.datamodel.annotation.TextFragmentAnnotation;

import javax.jdo.Extent;
import javax.jdo.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TextractorManager provides methods to query the textractor database.
 * Textractor relies on a database backend to store articles, sentences and
 * associated annotations. <p/> Date: Dec 17, 2003 Time: 6:50:47 PM
 *
 * @author Fabien Campagne
 */
public final class TextractorManager {
    private final DbManager dbm;
    private RunningCounter docCounter;
    private RunningCounter articleCounter;
    private RunningCounter annotationCounter;
    private RunningCounter annotationBatchCounter;
    private RunningCounter exportParameterCounter;
    private TextractorInfo info;

    /**
     * When true, retrieveAll is called on the result of some queries, hinting
     * the JDO implementation that all the elements of the collection should be
     * loaded in memory.
     */
    private boolean retrieveAll;

    private Query queryArticleByPmid;

    /**
     * Constructs an instance of TextractorManager within the context of a
     * database manager.
     *
     * @param dbm The database manager this new instance will be operating with.
     */
    public TextractorManager(final DbManager dbm)
            throws TextractorDatabaseException {
        this.dbm = dbm;
        dbm.beginTxn();
        docCounter = (RunningCounter) dbm.lookup("DocumentRunningCounter");
        if (docCounter == null) {
            docCounter = new RunningCounter();
            docCounter.setNumber(0);
            dbm.bind(docCounter, "DocumentRunningCounter");
        }

        articleCounter = (RunningCounter) dbm.lookup("ArticleRunningCounter");
        if (articleCounter == null) {
            articleCounter = new RunningCounter();
            articleCounter.setNumber(0);
            dbm.bind(articleCounter, "ArticleRunningCounter");
        }

        annotationCounter = (RunningCounter) dbm
                .lookup("AnnotationRunningCounter");
        if (annotationCounter == null) {
            annotationCounter = new RunningCounter();
            annotationCounter.setNumber(0);
            dbm.bind(annotationCounter, "AnnotationRunningCounter");
        }

        annotationBatchCounter = (RunningCounter) dbm
                .lookup("AnnotationBatchRunningCounter");
        if (annotationBatchCounter == null) {
            annotationBatchCounter = new RunningCounter();
            annotationBatchCounter.setNumber(0);
            dbm.bind(annotationBatchCounter, "AnnotationBatchRunningCounter");
        }

        exportParameterCounter = (RunningCounter) dbm
                .lookup("ParametersRunningCounter");
        if (exportParameterCounter == null) {
            exportParameterCounter = new RunningCounter();
            exportParameterCounter.setNumber(0);
            dbm.bind(exportParameterCounter, "ParametersRunningCounter");
        }

        info = (TextractorInfo) dbm.lookup("TextractorInfo");
        if (info == null) {
            info = new TextractorInfo();
            dbm.bind(info, "TextractorInfo");

            final PropertyManager propertyManager =
                PropertyManager.getInstance(dbm.getPropertyFilename());
            info.setCaseSensitiveIndexBasename(propertyManager
                    .getProperty("caseSensitiveIndexBasename"));
            info.setStemmedIndexBasename(propertyManager
                    .getProperty("stemmedIndexBasename"));
        }
        dbm.commitTxn();
    }

    /**
     * Creates an article. The instance returned should be made persistent if
     * needed.
     *
     * @return a new Article.
     * @see DbManager#makePersistent
     */
    public Article createArticle() {
        // TODO: Shouldn't we be setting the article number here?
        return new Article();
    }

    /**
     * Creates an OTMI article. The instance returned should be made
     * persistent if needed.
     *
     * @return a new Article.
     * @see DbManager#makePersistent
     */
    public OtmiArticle createOtmiArticle() {
        // TODO: Shouldn't we be setting the article number here?
        return new OtmiArticle();
    }

    /**
     * Creates a sentence. The instance returned should be made persistent if
     * needed.
     *
     * @return a new Sentence.
     * @see DbManager#makePersistent
     */
    public Sentence createNewSentence(final Article article) {
        return new Sentence(article);
    }

    /**
     * Returns the next available document number. Within the context of a
     * textractor databasem this method always returns incremented numbers.
     * (this method is garanteed never to return the same value twice.) For
     * textractor, documents are sentences. This is the level at which articles
     * are split before being indexed with MG4J.
     *
     * @return The next available document number.
     */
    public synchronized long getNextDocumentNumber() {
        return docCounter.getNextNumber();
    }

    /**
     * Returns the next available document number. Within the context of a
     * textractor databasem this method always returns incremented numbers.
     * (this method is garanteed never to return the same value twice.) For
     * textractor, documents are sentences. This is the level at which articles
     * are split before being indexed with MG4J.
     *
     * @return The next available document number.
     */
    public synchronized long getNextArticleNumber() {
        return articleCounter.getNextNumber();
    }

    /**
     * Obtains a sentence from the database. A sentence is retrieved that has
     * the given document number.
     *
     * @param documentNumber of the sentence to retrieve.
     * @return The sentence that has this documentNumber, or null, if none was
     *         found with this number.
     */
    @SuppressWarnings("unchecked")
    public Sentence getSentence(final long documentNumber) {
        final Sentence sentence;
        final Extent e = dbm.getExtent(Sentence.class, false);
        final Query query = dbm.newQuery();
        query.setCandidates(e);
        query.setFilter("this.documentNumber==" + documentNumber);
        query.compile();
        final Collection<Sentence> result = (Collection) query.execute();
        final Iterator<Sentence> it = result.iterator();
        if (it.hasNext()) {
            sentence = it.next();
            // TODO: e.close(it);
        } else {
            sentence = null;
        }
        return sentence;
    }

    /**
     * Returns an interator over the complete set of sentences. Each sentence in
     * the textractor database is accessible through this iterator. Generally,
     * using this method does not scale up to large databases: the iterator
     * cannot be traversed in a single transaction without creating a large
     * number of instances that eventually consume all the memory in the JVM.
     * Use getSentenceIterator(int lowerBound) instead with a lowerbound.
     *
     * @return Iterator over the sentences in the database.
     */
    public Iterator<Sentence> getSentenceIterator() {
        return getSentenceIterator(-1);
    }

    /**
     * Returns an interator over a subset of sentences. <p/>Each sentence in the
     * textractor database is accessible through this iterator.
     *
     * @param lowerBound Sentences returned will have a documentNumber that is
     *        at least equal to this lowerBound.
     * @param upperBound Returned sentences will have document number less or
     *        equal to this value
     * @return Iterator over the subset of sentences.
     */
    public Iterator<Sentence> getSentenceIterator(final long lowerBound,
            final long upperBound) {
        return getSentenceIterator(lowerBound, upperBound, null);
    }

    /**
     * Returns an interator over a subset of sentences. <p/> Each sentence in
     * the textractor database is accessible through this iterator. Generally,
     * using this method does not scale up to large databases: the iterator
     * cannot be traversed in a single transaction without creating a large
     * number of instances that eventually consume all the memory in the JVM.
     * Use getSentenceIterator(int lowerBound) instead with a lowerbound.
     *
     * @param lowerBound Sentences returned will have a documentNumber that is
     *        at least equal to this lowerBound.
     * @return Iterator over the subset of sentences.
     */

    public Iterator<Sentence> getSentenceIterator(final long lowerBound) {
        return getSentenceIterator(lowerBound, null);
    }

    public Iterator<Sentence> getSentenceIterator(final String filter) {
        return getSentenceIterator(-1, filter);
    }

    public Iterator<Sentence> getSentenceIterator(final String filter,
            final String variableDeclarations, final Map variableValues) {

        return getSentenceIterator(-1, filter, variableDeclarations,
                variableValues);
    }

    public Iterator<Sentence> getSentenceIterator(final long lowerBound,
            final String filter, final String variableDeclarations,
            final Map variableValues) {

        return getSentenceIterator(lowerBound, -1, filter,
                variableDeclarations, variableValues);
    }

    /**
     * Get a subset of sentences that satisfy certain criteria.
     *
     * @param filter A JDO filter that expresses the criteria that the sentence
     * must fullfil to be in the subset (e.g., int lowerBound = 0;
     * filter = "this.maybeProteinMutation==true")
     */
    public Iterator<Sentence> getSentenceIterator(final long lowerBound,
            final String filter) {
        return getSentenceIterator(lowerBound, -1, filter);
    }

    /**
     * Set the data retrieval behavior.
     *
     * @param retrieveAll When true, retrieveAll is called on the result of some
     *        queries, hinting the JDO implementation that all the elements of
     *        the collection should be loaded in memory.
     */
    public void setRetrieveAll(final boolean retrieveAll) {
        this.retrieveAll = retrieveAll;
    }

    public Iterator<Sentence> getSentenceIterator(final long lowerBound,
            final long upperBound, final String filter) {
        return getSentenceIterator(lowerBound, upperBound, filter, null,
                new HashMap());
    }

    private Query sentenceQueryByArticle;

    /**
     * Get a subset of sentences that satisfy certain criteria.
     *
     * @param lowerBound Returned sentences will have document number greater
     *        than this value
     * @param upperBound Returned sentences will have document number less or
     *        equal to this value
     * @param filter A JDO filter that expresses the criteria that the sentence
     *        must fullfil to be in the subset (e.g., int lowerBound = 0;
     *        filter = "this.maybeProteinMutation==true")
     * @param parameterDeclarations Declaration for parameters used in query
     *        (e.g., "long pmid, Article article"). Declared parameters can
     *        appear on the filter.
     * @param parameterValues One entry for each parameter declared in
     *        parameterDeclarations. For instance, "pmid" -> Long(121231),
     *        "article" -> article@23244
     */
    public Iterator<Sentence> getSentenceIterator(final long lowerBound,
            final long upperBound, final String filter,
            final String parameterDeclarations, final Map parameterValues) {
        final StringBuffer documentNumberFilter = new StringBuffer();
        if (lowerBound != -1) {
            documentNumberFilter.append("this.documentNumber > ");
            documentNumberFilter.append(lowerBound);
            if (upperBound != -1) {
                documentNumberFilter.append(" && ");
            }
        }

        if (upperBound != -1) {
            documentNumberFilter.append(" this.documentNumber <= ");
            documentNumberFilter.append(upperBound);
        }

        if (documentNumberFilter.length() != 0 && filter != null) {
            documentNumberFilter.append(" && ");
        }
        if (filter != null) {
            documentNumberFilter.append(filter);
        }

        final Extent e = dbm.getExtent(Sentence.class, false);
        final Query query = dbm.newQuery();
        if (parameterDeclarations != null) {
            query.declareParameters(parameterDeclarations);
        }
        query.setFilter(documentNumberFilter.toString());
        query.setCandidates(e);
        query.setOrdering("documentNumber ascending");
        query.compile();
        final Collection<Sentence> result =
                (Collection<Sentence>) query.executeWithMap(parameterValues);
        if (retrieveAll) {
            dbm.retrieveAll(result);
        }
        return result.iterator();
    }

    public Iterator<DoubleTermAnnotation> getDoubleTermAnnotationIterator(final int batchId) {
        final Extent e = dbm.getExtent(
                textractor.datamodel.annotation.DoubleTermAnnotation.class,
                false);
        final Query query = dbm.newQuery();
        query.setFilter("this.annotationBatchNumber == " + batchId);
        query.setCandidates(e);
        query.setOrdering("annotationNumber ascending");
        query.compile();
        final Collection<DoubleTermAnnotation> result =
            (Collection<DoubleTermAnnotation>) query.execute();
        return result.iterator();
    }

    /**
     * Returns an interator over the complete set of Articles. Each Article in
     * the textractor database is accessible through this iterator. Generally,
     * using this method does not scale up to large databases: the iterator
     * cannot be traversed in a single transaction without creating a large
     * number of instances that eventually consume all the memory in the JVM.
     * Use getArticleIterator(int lowerBound) instead with a lowerbound.
     */
    public Iterator<Article> getArticleIterator() {
        return getArticleIterator(-1);
    }

    /**
     * Returns an iterator over a subset of sentences. <p/> Each Article in the
     * textractor database is accessible through this iterator. Generally, using
     * this method does not scale up to large databases: the iterator cannot be
     * traversed in a single transaction without creating a large number of
     * instances that eventually consume all the memory in the JVM.
     *
     * @param lowerBound Articles returned will have an articleNumber that is at
     *        least equal to this lowerBound.
     * @return Iterator over the subset of Articles.
     */
    public Iterator<Article> getArticleIterator(final int lowerBound) {
        return getArticleIterator(lowerBound, -1);
    }

    /**
     * Returns an iterator over a subset of sentences. <p/> Each Article in the
     * textractor database is accessible through this iterator. Generally, using
     * this method does not scale up to large databases: the iterator cannot be
     * traversed in a single transaction without creating a large number of
     * instances that eventually consume all the memory in the JVM.
     *
     * @param lowerBound Articles returned will have an articleNumber that is at
     *        least equal to this lowerBound.
     * @param upperBound Articles returned will have an articleNumber that is at
     *        most equal to this lowerBound.
     * @return Iterator over the subset of Articles.
     */
    public Iterator<Article> getArticleIterator(final int lowerBound,
            final int upperBound) {
        return getArticleIterator(lowerBound, upperBound, null);
    }

    /**
     * Get a subset of Article that satisfy certain criteria.
     *
     * @param filter A JDO filter that expresses the criteria that the Article
     *        must fulfill to be in the subset (e.g., int lowerBound = 0; filter =
     *        "this.maybeProteinMutation==true")
     */
    public Iterator<Article> getArticleIterator(final int lowerBound,
            final int upperBound, final String filter) {
        final StringBuffer filterBuffer = new StringBuffer();

        if (lowerBound != -1) {
            filterBuffer.append("this.articleNumber > ");
            filterBuffer.append(lowerBound);
            if (upperBound != -1) {
                filterBuffer.append(" && ");
            }
        }

        if (upperBound != -1) {
            filterBuffer.append(" this.articleNumber <= ");
            filterBuffer.append(upperBound);
        }

        if (filter != null) {
            if (filterBuffer.length() != 0) {
                filterBuffer.append(" && ");
            }
            filterBuffer.append(filter);
        }

        final Extent e =
                dbm.getExtent(textractor.datamodel.Article.class, false);
        final Query query = dbm.newQuery();
        query.setFilter(filterBuffer.toString());
        query.setCandidates(e);
        query.setOrdering("articleNumber ascending");
        query.compile();
        final Collection<Article> result = (Collection<Article>) query.execute();
        return result.iterator();
    }

    public Article getArticleByNumber(final long articleNumber) {
        Article article = null;
        final Extent e =
                dbm.getExtent(textractor.datamodel.Article.class, false);
        final Query query = dbm.newQuery();
        query.setFilter("this.articleNumber == " + articleNumber);
        query.setCandidates(e);
        query.compile();
        final Collection<Article> result = (Collection<Article>) query.execute();
        if (result.size() != 0) {
            final Iterator<Article> i = result.iterator();
            if (i.hasNext()) {
                article = i.next();
                // TODO: e.close(i);
            }
        }

        return article;
    }

    public Article getArticleByPmid(final long pmid) {
        final Query query = getArticleByPmid();
        final Object result = query.execute(pmid);
        final Article article = (Article) getOne(result);
        query.close(result);
        query.closeAll();
        return article;

    }

    private Object getOne(final Object collection) {
        final Collection col = (Collection) collection;
        return col.size() == 1 ? col.toArray()[0] : null;
    }

    public Query getArticleByPmid() {
        if (queryArticleByPmid == null) {
            final Extent e =
                    dbm.getExtent(textractor.datamodel.Article.class, false);
            queryArticleByPmid = dbm.newQuery();
            queryArticleByPmid.declareParameters("java.lang.Long pmid");
            queryArticleByPmid.setFilter("this.pmid == pmid");
            queryArticleByPmid.setCandidates(e);
            queryArticleByPmid.compile();
        }
        return queryArticleByPmid;
    }

    /**
     * Returns the last attributed document number.
     *
     * @return last attributed document number.
     */
    public int getLastDocumentNumber() {
        return docCounter.getNumber();
    }

    /**
     * Returns the last attributed article number.
     *
     * @return last attributed article number.
     */
    public int getLastArticleNumber() {
        return articleCounter.getNumber();
    }

    public SingleTermAnnotation createSingleTermAnnotation(final int batchNumber) {
        return new SingleTermAnnotation(batchNumber, annotationCounter.getNextNumber());
    }

    public DoubleTermAnnotation createDoubleTermAnnotation(final int batchNumber) {
        return new DoubleTermAnnotation(batchNumber, annotationCounter.getNextNumber());
    }

    /**
     * Returns a number to identify a new batch of annotations.
     */
    public synchronized int getNextAnnotationBatchNumber() {
        return this.annotationBatchCounter.getNextNumber();

    }

    /**
     * Returns a TextFragmentAnnotaion.
     *
     * @param annotationNumber Number of the annotation to return.
     * @return The annotation, or null if none could be found with this number
     */
    public TextFragmentAnnotation getAnnotationById(final int annotationNumber) {
        TextFragmentAnnotation textFragmentAnnotation = null;
        final Extent e = dbm.getExtent(
                textractor.datamodel.annotation.TextFragmentAnnotation.class,
                true);
        final Query query = dbm.newQuery();
        query.setFilter("this.annotationNumber == " + annotationNumber);
        query.setCandidates(e);
        query.compile();
        final Collection<TextFragmentAnnotation> result =
            (Collection<TextFragmentAnnotation>) query.execute();

        // return one element only, or null:
        if (result.size() != 0) {
            final Iterator<TextFragmentAnnotation> i = result.iterator();
            if (i.hasNext()) {
                textFragmentAnnotation = i.next();
            }
        }

        return textFragmentAnnotation;
    }

    /**
     * Returns a number to identify a new batch of annotations.
     */
    public synchronized int getNextParameterNumber() {
        return this.exportParameterCounter.getNextNumber();

    }

    /**
     * Returns annotations in a specific batch. Only annotations that match
     * these conditions are returned: annotationImported==true. The collection
     * returns contains annotations ordered on their annotationNumber. This
     * garantees that this method returns annotations in a certain order for a
     * specific batch.
     *
     * @param batchId The batch number.
     * @return Collection of annotations in the batch identified by batchId, or
     *         null if the batch could not be found.
     */
    public Collection<AnnotationSource> getAnnotationsInBatch(final int batchId) {
        final Extent e = dbm.getExtent(
                textractor.datamodel.annotation.TextFragmentAnnotation.class,
                true);
        final Query query = dbm.newQuery();
        query.setFilter("this.annotationBatchNumber == " + batchId
                + " && this.annotationImported==true");
        query.setCandidates(e);
        query.setOrdering("annotationNumber ascending");
        query.compile();
        return (Collection<AnnotationSource>) query.execute();
    }

    /**
     * Returns annotations in a specific batch. Only annotations that match
     * these conditions are returned: annotationImported==false. These
     * annotations have NOT been manually annotated. The collection returns
     * contains annotations ordered on their annotationNumber. This guarantees
     * that this method returns annotations in a certain order for a specific
     * batch.
     *
     * @param batchId The batch number.
     * @return Collection of annotations in the batch identified by batchId, or
     *         null if the batch could not be found.
     */
    public Collection<AnnotationSource> getUnannotatedAnnotationsInBatch(final int batchId) {
        final Extent e = dbm.getExtent(
                textractor.datamodel.annotation.TextFragmentAnnotation.class,
                true);
        final Query query = dbm.newQuery();
        query.setFilter("this.annotationBatchNumber == " + batchId
                + " && this.annotationImported==false");
        query.setCandidates(e);
        query.setOrdering("annotationNumber ascending");
        query.compile();
        return (Collection<AnnotationSource>) query.execute();
    }

    public TextractorInfo getInfo() {
        return info;
    }

    /**
     * Returns a FeatureCreationParameters set by parameter set number.
     *
     * @param id Parameter set number of the
     *        SingleBagOfWordFeatureCreationParameters set to return.
     * @return The SingleBagOfWordFeatureCreationParameters, or null if none
     *         could be found with this number.
     */
    public FeatureCreationParameters getParameterSetById(final int id) {
        FeatureCreationParameters featureCreationParameters = null;
        final Extent e = dbm.getExtent(
                textractor.datamodel.FeatureCreationParameters.class, true);
        final Query query = dbm.newQuery();
        query.setFilter("this.parameterNumber == " + id);
        query.setCandidates(e);
        query.compile();
        final Collection<FeatureCreationParameters> result =
            (Collection<FeatureCreationParameters>) query.execute();
        // return one element only, or null:
        if (result != null) {
            final Iterator<FeatureCreationParameters> it = result.iterator();
            if (it.hasNext()) {
                featureCreationParameters = it.next();
            }
        }

        return featureCreationParameters;
    }

    /**
     * Returns annotations in a specific batch. Only annotations that match
     * these conditions are returned: annotationImported==false. These
     * annotations have NOT been manually annotated. The collection returns
     * contains annotations ordered on their annotationNumber. This guarantees
     * that this method returns annotations in a certain order for a specific
     * batch.
     *
     * @param batchId The batch number.
     * @param lowerBound The annotation number of annotations will be greater OR
     *        equal than this number
     * @param maxAnnotationsPerChunk At most this number of annotations will be
     *        returned per method call. Warning: this method assumes that
     *        annotations are numbered sequentially. If this is not the case the
     *        number of returned annotations may not match
     *        maxAnnotationsPerChunk.
     * @return Collection of annotations in the batch identified by batchId, or
     *         null if the batch could not be found.
     */
    public Collection<AnnotationSource> getUnannotatedAnnotationsInBatch(final int batchId,
            final int lowerBound, final int maxAnnotationsPerChunk) {
        final Extent e = dbm.getExtent(
                textractor.datamodel.annotation.TextFragmentAnnotation.class,
                true);
        final Query query = dbm.newQuery();
        final int upperBound = lowerBound + maxAnnotationsPerChunk;
        query.setFilter("this.annotationBatchNumber == " + batchId
                + " && this.annotationImported==false "
                + "&& this.annotationNumber>=" + lowerBound
                + " && this.annotationNumber<" + upperBound);
        query.setCandidates(e);
        query.setOrdering("annotationNumber ascending");
        query.compile();
        return (Collection<AnnotationSource>) query.execute();
    }

    public Iterator<Sentence> getSentenceIterator(final Article article) {
        final String filter = " this.article == varArticle";

        final Map<String, Article> map = new HashMap<String, Article>();
        map.put("varArticle", article);
        return getSentenceIterator(filter, Article.class.getCanonicalName()
                + " varArticle", map);
    }

    public Query getSentenceQueryByArticle() {
        if (sentenceQueryByArticle == null) {
            final Extent e = dbm.getExtent(Sentence.class, false);
            final Query query = dbm.newQuery();
            query.declareParameters(Article.class.getCanonicalName()
                    + " paramArticle");

            query.setFilter("this.article == paramArticle");
            query.setCandidates(e);
            query.setOrdering("documentNumber ascending");
            query.compile();
            sentenceQueryByArticle = query;
        }
        return sentenceQueryByArticle;
    }

    public int getLatestAnnotationBatchNumber() {
        return this.annotationBatchCounter.getNumber() - 1;
    }

    /**
     * Convert a set of document numbers to PMIDS. The PMIDs are the PMID of the
     * article referenced by the document/sentences in the set.
     */
    public long[] sentenceToPMID(final int[] documentNumber) {
        // TODO rewrite this method so that it works for large input arrays
        long[] pmids = null;
        final List<Long> documentCollection = new ArrayList<Long>();
        for (final int doc : documentNumber) {
            documentCollection.add((long) doc);
        }

        final Extent e =
                dbm.getExtent(textractor.datamodel.Sentence.class, false);
        final Query query = dbm.newQuery();
        query.setCandidates(e);
        query.declareImports("import java.util.List");
        query.declareParameters("List documents");
        query.setFilter("documents.contains(this.documentNumber)");
        query.compile();
        final Collection<Sentence> result =
                (Collection) query.execute(documentCollection);
        if (result != null) {
            int i = 0;
            pmids = new long[result.size()];
            for (final Sentence sentence : result) {
                pmids[i++] = sentence.getArticle().getPmid();
            }
        }
        return pmids;
    }

    /**
     * Delete an article with all its sentences.
     *
     * @param article Article to delete.
     */
    public void deleteArticle(final Article article) {
        final Query query = getSentenceQueryByArticle();
        final Set<Long> docNumbers = new LongOpenHashSet();
        final Collection<Sentence> result =
                (Collection) query.execute(article);

        final Collection deleteSet = new ArrayList(result.size());
        for (final Sentence sentence : result) {
            deleteSet.add(sentence);
            docNumbers.add(sentence.getDocumentNumber());
        }
        deleteSet.add(article);
        dbm.deleteAll(deleteSet);

        // free resources:
        query.close(result);
        query.closeAll();

        // now, create one padding document for each sentence deleted:
        for (final long docNumber : docNumbers) {
            final TextractorDocument paddingDoc = new PaddingDocument();
            paddingDoc.setDocumentNumber(docNumber);
            dbm.makePersistent(paddingDoc);
        }
    }

    /**
     * Get a subset of document that satisfy certain criteria.
     *
     * @param lowerBound Returned documents will have document number greater
     *        than this value
     * @param upperBound Returned documents will have document number less or
     *        equal to this value
     * @param filter A JDO filter that expresses the criteria that the document
     *        must fullfil to be in the subset (e.g., filter =
     *        "this.maybeProteinMutation==true")
     * @return Iterator over the subset of documents.
     */
    @SuppressWarnings("unchecked")
    public Iterator<TextractorDocument> getDocumentIterator(
            final long lowerBound, final long upperBound, final String filter) {
        final Query query = getDocumentQuery(lowerBound, upperBound, filter);
        final Collection result = (Collection) query.execute();

        if (retrieveAll) {
            dbm.retrieveAll(result);
        }

        return result.iterator();
    }

    public Query getDocumentQuery(final long lowerBound,
                                  final long upperBound,
                                  final String filter) {
        final StringBuffer documentNumberFilter = new StringBuffer();
        if (lowerBound != -1) {
            documentNumberFilter.append("this.documentNumber > ");
            documentNumberFilter.append(lowerBound);
            if (upperBound != -1) {
                documentNumberFilter.append(" && ");
            }
        }

        if (upperBound != -1) {
            documentNumberFilter.append(" this.documentNumber <= ");
            documentNumberFilter.append(upperBound);
        }

        if (documentNumberFilter.length() != 0 && filter != null) {
            documentNumberFilter.append(" && ");
        }
        if (filter != null) {
            documentNumberFilter.append(filter);
        }

        final Extent e = dbm.getExtent(TextractorDocument.class, true);
        final Query query = dbm.newQuery();

        query.setFilter(documentNumberFilter.toString());
        query.setCandidates(e);
        query.setOrdering("documentNumber ascending");
        query.compile();
        return query;
    }

    /**
     * Returns an interator over a subset of documents. <p/> Each document in
     * the textractor database is accessible through this iterator.
     *
     * @param lowerBound Documents returned will have a documentNumber that is
     *        at least equal to this lowerBound.
     * @param upperBound Returned documents will have document number less or
     *        equal to this value
     * @return Iterator over the subset of documents.
     */
    public Iterator<TextractorDocument> getDocumentIterator(
            final long lowerBound, final long upperBound) {
        return getDocumentIterator(lowerBound, upperBound, null);
    }

    /**
     * Retrieve a document in the database. Use this method when you need to
     * retrieve padding documents and other types of documents. If looking for
     * sentences, use getSentence().
     *
     * @param documentNumber The number of the document to retrieve.
     * @return The document if found in the database, or null.
     */
    public TextractorDocument getDocument(final long documentNumber) {
        TextractorDocument textractorDocument = null;
        final Extent e = dbm.getExtent(TextractorDocument.class, true);
        final Query query = dbm.newQuery();
        query.setCandidates(e);
        query.setFilter("this.documentNumber==" + documentNumber);
        query.compile();
        final Collection<TextractorDocument> result =
            (Collection<TextractorDocument>) query.execute();
        if (result != null) {
            final Iterator<TextractorDocument> it = result.iterator();
            if (it.hasNext()) {
                textractorDocument = it.next();
            }
        }

        return textractorDocument;
    }

    /**
     * Get the total number of sentences in the datastore.
     * @see textractor.datamodel.Sentence
     * @return The number of sentences in the database.
     */
    public int getSentenceCount() {
        final Extent e = dbm.getExtent(Sentence.class, false);
        return dbm.getJdoExtension().size(e);
    }

    /**
     * Get the total number of articles in the datastore.
     * @see textractor.datamodel.Article
     * @return The number of articles in the database.
     */
    public int getArticleCount() {
        final Extent e = dbm.getExtent(Article.class, true);
        return dbm.getJdoExtension().size(e);
    }

    /**
     * Get the total number of documents in the datastore.
     * @see textractor.datamodel.TextractorDocument
     * @return The number of documents in the database.
     */
    public int getDocumentCount() {
        final Extent e = dbm.getExtent(TextractorDocument.class, true);
        return dbm.getJdoExtension().size(e);
    }

    /**
     * Return the number of articles in the database.
     */
    public int getNumberOfArticlesProcessed() {
        return articleCounter.getNumber();
    }
}
