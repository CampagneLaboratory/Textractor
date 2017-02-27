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

package textractor.parsers;

import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.util.MutableString;
import it.unimi.dsi.mg4j.util.parser.BulletParser;
import it.unimi.dsi.mg4j.util.parser.ParsingFactory;
import it.unimi.dsi.mg4j.util.parser.WellFormedXmlFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * User: campagne Date: Oct 12, 2005 Time: 12:53:01 PM.
 */
public final class ParsePubmedFast extends PubmedExtractor {
    private PrintWriter output;
    private int abbreviationCount;

    public static void main(final String[] args) throws IOException {
        final ParsePubmedFast processor = new ParsePubmedFast();
        processor.process(args);
    }

    private void process(final String[] args) throws IOException {
        final String filename = args[0];
        final String outputFilename = args[1];

        if (args.length > 2) {
            final String articleElementName = args[2];
            setArticleElementName(articleElementName);
        }

        System.out.println("Scanning " + filename);
        System.out.println("Writing to " + outputFilename);
        System.out.flush();
        InputStream stream = new FileInputStream(filename);
        final FileWriter out = new FileWriter(outputFilename);
        output = new PrintWriter(out);

        if (filename.endsWith(".gz") || filename.endsWith(".zip")) {
            stream = new GZIPInputStream(stream);
        }
        final FastBufferedReader reader = new FastBufferedReader(
                new InputStreamReader(stream));
        char[] buffer = new char[10000];

        final ParsingFactory factory = new WellFormedXmlFactory();
        final BulletParser parser = new BulletParser(factory);

        parser.setCallback(this);
        // read the whole file in memory:
        int length;
        int offset = 0;

        while ((length = reader.read(buffer, offset, buffer.length - offset)) > 0) {
            offset += length;
            buffer = CharArrays.grow(buffer, offset + 1);
        }
        // parse and collect abbreviations:
        parser.parse(buffer, 0, offset);

        System.out.println("Found " + abbreviationCount
                + " potential abbreviations.");
        output.flush();
        out.flush();
        output.close();
        System.exit(0);
    }

    @Override
    public boolean processAbstractText(final MutableString pmid,
            final MutableString title, final MutableString text,
            final Map<String, Object> additionalFieldsMap) {
        processAbstract(pmid, text, additionalFieldsMap);
        return false;
    }

    @Override
    public void processNoticeOfRetraction(final MutableString pmid,
            final List<String> retractedPmid, final boolean create) {
        // not required for this application.
    }

    public void processAbstract(final CharSequence pmid,
            final MutableString abstractText, final Map<String, Object> additionalFieldsMap) {
        int lastOpeningOffset = -1;
        while ((lastOpeningOffset = abstractText.indexOf("(",
                lastOpeningOffset + 1)) > 0) {
            final int lastFirstClosingOffset = abstractText.indexOf(')',
                    lastOpeningOffset);
            if (lastFirstClosingOffset == -1) {
                continue; // parenthesis is not closed.
            }
            final MutableString abbreviation =
                abstractText.substring(lastOpeningOffset + 1,
                        lastFirstClosingOffset);
            if (abbreviation.indexOf(' ') > 0) {
                continue;
            }

            int digitCount = 0;
            for (int i = 0; i < abbreviation.length(); ++i) {
                final char currentChar = abbreviation.charAt(i);

                if (Character.isDigit(currentChar) || currentChar == '-'
                        || currentChar == '+' || currentChar == '/'
                        || currentChar == '.' || currentChar == ','
                        || currentChar == '(' || currentChar == ')'
                        || currentChar == '#' || currentChar == '*'
                        || currentChar == '%' || currentChar == ':'
                        || currentChar == '>' || currentChar == '<') {
                    ++digitCount;
                }
            }

            final boolean abbrIsNumber = digitCount == abbreviation.length();
            if (!abbrIsNumber) {
                abbreviation.println(output);
                abbreviationCount += 1;
            }
        }
    }
}
