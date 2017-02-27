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

import org.apache.commons.configuration.ConfigurationException;
import org.htmlparser.util.ParserException;
import textractor.database.DbManager;
import textractor.database.TextractorDatabaseException;
import textractor.sentence.SentenceProcessingException;

import java.io.IOException;

/**
 * Converts HTML to Text. This translater uses the alt text of images to replace
 * images. This is useful since many journals use images for greek symbol, and
 * use an alt attribute to render this for text-only browsers.
 */
public class Html2Text2DB extends AbstractHtml2Text {
    protected DatabaseTextConsumer consumer;
    public Html2Text2DB(final String[] args) throws TextractorDatabaseException {
        super(args);
        consumer = new DatabaseTextConsumer(new DbManager(args), articleChunkSize);

    }

    @Override
    public final void setConsumer(final TextConsumer consumer) {
        assert consumer instanceof DatabaseTextConsumer : "HTML2Text2DB currently only supports database text consumers.";
        this.consumer = (DatabaseTextConsumer) consumer;
    }

    @Override
    public final TextConsumer getConsumer() {
        return consumer;
    }

    public static void main(final String[] args)
            throws TextractorDatabaseException, ParserException,
            ConfigurationException, IOException, SentenceProcessingException {
        final Html2Text2DB h2T2D = new Html2Text2DB(args);
        h2T2D.process(args);
    }

}
