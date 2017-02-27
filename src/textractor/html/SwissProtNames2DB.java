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
import org.apache.commons.configuration.ConfigurationException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.sigpath.bioimport.xml.Entries;
import org.sigpath.bioimport.xml.Entry;
import textractor.database.DbManager;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.Article;
import textractor.datamodel.Sentence;

import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Import SwissProt/Trembl names from XML files in the SigPath format.  Description
 * for each protein is imported as a pair (article, sentence).
 */
public final class SwissProtNames2DB extends Html2Text2DB {
    public SwissProtNames2DB(final String[] args) throws TextractorDatabaseException {
        super(args);
    }

    public static void main(final String[] args) throws TextractorDatabaseException, IOException, ConfigurationException {
        final SwissProtNames2DB converter = new SwissProtNames2DB(args);
        converter.process(args);
    }

    /**
     * Process abstracts with the given arguments and database manager.
     *
     */
    @Override
    public void process(final String[] args) throws IOException {
        process(consumer.getDbManager(), args);
    }

    /**
     * Process abstracts with the given arguments and database manager.
     *
     * @param dbm  Database manager to process the abstracts with
     * @param args Arguments used to process the abstracts
     * @throws java.io.IOException if there is a problem reading the abstracts
     *                             specified by the arguments
     */
    private void process(final DbManager dbm, final String[] args) throws IOException {
        final String input_fn = CLI.getOption(args, "-i", null);
        if (input_fn == null) {
            System.err.println("usage: -i input. ");
            System.exit(1);
        }
        Entries input = null;
        try {
            System.out.print("Reading input..");
            System.out.flush();
            final FileReader reader = new FileReader(input_fn);
            input = (Entries) Entries.unmarshal(reader);
            System.out.println(" done.");
            System.out.flush();
        } catch (final MarshalException e) {
            System.err.println("An error occured unmarshaling the input");
            System.exit(10);
        } catch (final ValidationException e) {
            System.err.println("An error occured validating the input");
            System.exit(10);
        }
        final Enumeration eInput = input.enumerateEntry();
        while (eInput.hasMoreElements()) {
            final Entry proteinEntry = (Entry) eInput.nextElement();
            convertEntryToArticle(dbm, proteinEntry);

        }
        System.exit(0);
    }

    static long PMID = 1;

    public void convertEntryToArticle(final DbManager dbm, final Entry entry) {
        dbm.beginTxn();

        final String text = entry.getDescription().getContent();

        final Article article = dbm.getTextractorManager().createArticle();
        dbm.makePersistent(article);
        article.setPmid(++PMID);
        article.setArticleNumber(dbm.getTextractorManager().getNextArticleNumber());

        final Sentence sentence =
                dbm.getTextractorManager().createNewSentence(article);
        sentence.setText(text);
        sentence.setDocumentNumber(dbm.getTextractorManager().getNextDocumentNumber());
        if (PMID < 10) {
            System.out.println("text: "+text);
        }
        dbm.makePersistent(sentence);
        dbm.commitTxn();
    }
}
