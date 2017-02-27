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

package textractor.tools.biostems;

import it.unimi.dsi.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.dsi.mg4j.query.parser.ParseException;
import it.unimi.dsi.mg4j.query.parser.QueryParserException;
import it.unimi.dsi.mg4j.util.MutableString;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.DocumentIndexManager;
import textractor.database.TextractorDatabaseException;
import textractor.didyoumean.DidYouMean;
import textractor.didyoumean.DidYouMeanI;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * This test requires the ambiguity dataset index.
 * User: Fabien Campagne
 * Date: May 2, 2006
 * Time: 10:58:38 AM
 */
public class TestBioStemmer extends TestCase {
    private DidYouMeanI dym;

    @Override
    protected void setUp() throws ConfigurationException, IOException,
            IllegalAccessException, NoSuchMethodException,
            InvocationTargetException, ClassNotFoundException,
            InstantiationException, URISyntaxException {
        final DocumentIndexManager docManager =
                new DocumentIndexManager("dataset-a-index/index");
        dym = new DidYouMean(docManager);
    }

    public void testStemmer() throws ConfigurationException, IOException,
            TextractorDatabaseException, ParseException, ClassNotFoundException, QueryParserException, QueryBuilderVisitorException {
        final BioStemmer stemmer = new BioStemmer(dym);
        assertEquals(new MutableString("ubiquitin"), stemmer.stem("ubiquitin1", true));
        assertEquals(new MutableString("ubiquitin"), stemmer.stem("polyubiquitination", true));
        assertEquals(new MutableString("ubiquitin"), stemmer.stem("ubiquitination", true));
        assertEquals(new MutableString("tein"), stemmer.stem("protein", true));
        assertEquals(new MutableString("kin"), stemmer.stem("kinase", true));
        assertEquals(new MutableString("rylations"), stemmer.stem("phosphorylations", true));
        assertEquals(new MutableString("eptide"), stemmer.stem("propeptide", true));
        assertEquals(new MutableString("peptide"), stemmer.stem("polypeptide", true));
        assertEquals(null, stemmer.stem("00", true));
        assertEquals(null, stemmer.stem("\u1212", true));
    }

    public void testPSStemmer() throws ConfigurationException, IOException,
            TextractorDatabaseException, ParseException, ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException,
            URISyntaxException, QueryParserException, QueryBuilderVisitorException {
        final DocumentIndexManager docManager =
                new DocumentIndexManager("dataset-a-index/index");
        final DidYouMeanI dym = new DidYouMean(docManager);
        final PSStemmer stemmer = new PSStemmer(new FileReader("data/biostemmer/prefix-test.probs"),
                new FileReader("data/biostemmer/suffix-test.probs"), dym);

        List<ScoredTerm> results =
                stemmer.suggest(new MutableString("ubiquitination"));
        assertNotNull(results);
        assertTrue(results.contains(new ScoredTerm("ubiquitinated", 6.217274931259453E-4d)));
        assertTrue(results.contains(new ScoredTerm("ubiquitinates", 1.4643630129285157E-4d)));
        assertTrue(results.contains(new ScoredTerm("ubiquitous", 1.0198812105954858E-7d)));

        results = stemmer.suggest(new MutableString("cortex"));
        assertNotNull(results);
        assertTrue(results.contains(new ScoredTerm("cortical", 2.0664410271820088E-7d)));

        results = stemmer.suggest(new MutableString("cortical"));
        assertNotNull(results);
        assertTrue(results.contains(new ScoredTerm("subcortical", 3.209029819117859E-5d)));
        assertTrue(results.contains(new ScoredTerm("cortex", 2.0664410271820088E-7d)));

        results = stemmer.suggest(new MutableString("kinase"));
        assertNotNull(results);
        assertTrue(results.contains(new ScoredTerm("kinases", 0.008037940599024296)));
        assertTrue(results.contains(new ScoredTerm("kind", 8.379923201573547E-6)));
        assertTrue(results.contains(new ScoredTerm("kinin", 1.3183448572817724E-6)));
        assertTrue(results.contains(new ScoredTerm("kinds", 6.069079176995729E-7)));

        results = stemmer.suggest(new MutableString("KINASE"));
        assertNotNull(results);
        assertTrue(results.contains(new ScoredTerm("kinases", 0.008037940599024296)));
        assertTrue(results.contains(new ScoredTerm("kind", 8.379923201573547E-6)));
        assertTrue(results.contains(new ScoredTerm("kinin", 1.3183448572817724E-6)));
        assertTrue(results.contains(new ScoredTerm("kinds", 6.069079176995729E-7)));
    }

    public void testPSStemmerMedlineModel() throws ConfigurationException,
            IOException, TextractorDatabaseException, ParseException,
            ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException, URISyntaxException, QueryParserException, QueryBuilderVisitorException {
        final DocumentIndexManager docManager =
                new DocumentIndexManager("dataset-a-index/index");
        final DidYouMeanI dym = new DidYouMean(docManager);
        final PSStemmer stemmer = new PSStemmer(new FileReader("data/biostemmer/prefix-medline.probs"),
                new FileReader("data/biostemmer/suffix-medline.probs"), dym);

        List<ScoredTerm> results =
                stemmer.suggest(new MutableString("ubiquitination"));
        assertNotNull(results);
        assertTrue(results.contains(new ScoredTerm("ubiquitin", 9.576611337251961E-4)));
        assertTrue(results.contains(new ScoredTerm("ubiquitinated", 3.012912056874484E-4)));
        assertTrue(results.contains(new ScoredTerm("ubiquitinates", 1.2583569332491606E-4)));
        assertTrue(results.contains(new ScoredTerm("polyubiquitination", 2.314697121619247E-5)));
        assertTrue(results.contains(new ScoredTerm("ubiquitous", 1.7328173385067203E-7)));
        assertTrue(results.contains(new ScoredTerm("ubiquitously", 3.4759739975243065E-8)));
        assertTrue(results.contains(new ScoredTerm("ubiquitylation", 3.428943884387081E-8)));
        assertTrue(results.contains(new ScoredTerm("ubiquitylated", 1.2677088712109708E-8)));

        results = stemmer.suggest(new MutableString("cortex"));
        assertNotNull(results);
        assertTrue(results.contains(new ScoredTerm("neocortex", 7.772619028401095E-6d)));
        assertTrue(results.contains(new ScoredTerm("cortical", 3.3309353852928325E-7d)));

        results = stemmer.suggest(new MutableString("cortical"));
        assertNotNull(results);
        assertTrue(results.contains(new ScoredTerm("subcortical", 4.671675560530275E-5d)));
        assertTrue(results.contains(new ScoredTerm("intracortical", 4.5822431275155395E-5d)));
        assertTrue(results.contains(new ScoredTerm("neocortical", 7.772619028401095E-6d)));
        assertTrue(results.contains(new ScoredTerm("adrenocortical", 1.8368331211604527E-6d)));
        assertTrue(results.contains(new ScoredTerm("cortex", 3.330935669509927E-7d)));

        results = stemmer.suggest(new MutableString("polyphosphorylated"));
        assertNotNull(results);
        assertTrue(results.contains(new ScoredTerm("autophosphorylated", 1.222700163339141E-8d)));

        results = stemmer.suggest(new MutableString("phosphorylated"));
        assertNotNull(results);
        assertTrue(results.contains(new ScoredTerm("phosphorylate", 7.780294981785119E-4d)));
        assertTrue(results.contains(new ScoredTerm("phosphorylation", 3.0129123479127884E-4d)));
        assertTrue(results.contains(new ScoredTerm("dephosphorylated", 2.127589104929939E-4d)));
        assertTrue(results.contains(new ScoredTerm("autophosphorylated", 1.116834391723387E-5d)));
        assertTrue(results.contains(new ScoredTerm("phosphatase", 1.9443286980447283E-9d)));
    }
}
