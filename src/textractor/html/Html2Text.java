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

import it.unimi.dsi.mg4j.util.MutableString;
import org.htmlparser.Parser;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.util.DefaultParserFeedback;
import org.htmlparser.util.ParserException;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Converts HTML into UTF-8 Text. This translator uses the alt text of images to
 * replace images. This is useful since many journals use images for Greek
 * symbols, and use an alt attribute to render this for text-only browsers.
 */
public class Html2Text {
    private String text;
    private String title;
    private List<Integer> positions;
    private CharSequence paragraphMarkerTag;

    public Html2Text() {
        super();
    }

    public final void parse(final Reader in) throws ParserException, IOException {
        // Read the file then parse it
        parseString(readDataToString(in), DefaultParserFeedback.QUIET);
    }

    public final void parse(final Reader in, final int feedbackLevel) throws ParserException, IOException {
        // Read the file then parse it
        parseString(readDataToString(in), feedbackLevel);
    }

    public void parseString(final MutableString inString) throws ParserException, IOException {
        parseString(inString, DefaultParserFeedback.QUIET);
    }

    public void parseString(final MutableString inString,
                            final int feedbackLevel)
            throws ParserException, IOException {
        modifyHtml(inString);

        final Lexer lexer = new Lexer(new Page(inString.toString(), "UTF-8"));
        final Parser parser =
                new Parser(lexer, new DefaultParserFeedback(feedbackLevel));

        final TextractorTextExtractingVisitor visitor =
                new TextractorTextExtractingVisitor(inString);
        visitor.setParagraphMarkerTag(paragraphMarkerTag);
        parser.visitAllNodesWith(visitor);
        text = visitor.getExtractedText().toString();
        positions = visitor.getExtractedPositions();
        title = visitor.getExtractedTitle().toString();
    }

    protected void modifyHtml(final MutableString inString) throws IOException {
        // Do nothing in this base case
    }

    private MutableString readDataToString(final Reader reader) throws IOException {
        final char[] buffer = new char[10000];
        final MutableString ms = new MutableString();

        // read the whole file in memory:
        int length;
        while ((length = reader.read(buffer, 0, 10000)) > 0) {
            ms.append(buffer, 0, length);
        }

        return ms;
    }

    public final String getText() {
        return text;
    }

    public final String getTitle() {
        return title;
    }

    public final List<Integer> getPositions() {
        return positions;
    }

    public void setParagraphBoundaryTag(final CharSequence tag) {
       this.paragraphMarkerTag = tag;
    }
}
