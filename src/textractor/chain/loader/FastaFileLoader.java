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

import edu.cornell.med.icb.parsers.FastaParser;
import it.unimi.dsi.mg4j.util.MutableString;
import textractor.datamodel.OtmiArticle;
import textractor.datamodel.Sentence;

import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

/**
 * Load a Fasta file as article sentence pairs. Each sequence of the Fasta file generates one article and one sentence.
 * The article doi identifier encodes the Accession code for the sequence parsed out of the FASTA sequence description
 * line.
 *
 * @author Fabien Campagne
 *         Date: Oct 12, 2006
 *         Time: 6:25:57 PM
 */
public class FastaFileLoader extends AbstractFileLoader {
    private final MutableString descriptionLine = new MutableString();
    private final MutableString rawResidues = new MutableString();
    private final MutableString accessionCode = new MutableString();
    private final MutableString proteinResidueCodes = new MutableString();

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    private int maxLength=Integer.MAX_VALUE;

    @Override
    public void processFilename(final String filename) throws IOException {
        FileReader fileReader = null;
        try {
            System.out.println("Processing file: " + filename);
            fileReader = new FileReader(filename);
            final FastaParser reader = new FastaParser(fileReader);
            if (!reader.hasNext()) {
                return;
            }
            while (reader.hasNext()) {
                reader.next(descriptionLine, rawResidues);
                reader.guessAccessionCode(descriptionLine, accessionCode);
                reader.filterProteinResidues(rawResidues, proteinResidueCodes);
               if (proteinResidueCodes.length()>maxLength) {
                   continue;
               }
                final OtmiArticle article = new OtmiArticle();
                article.setDoi(accessionCode.toString());

                final Sentence sentence = produce(article, proteinResidueCodes);
                final Vector<Sentence> uniSentence = new Vector<Sentence>();
                uniSentence.add(sentence);
                article.setPmid(count); //  articles' PMID is the order the sequence appears in the FASTA file.
                produce(article, uniSentence);
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
        }
    }

    private int count;

    public int getNumberOfArticlesProcessed() {
        return count;
    }
}
