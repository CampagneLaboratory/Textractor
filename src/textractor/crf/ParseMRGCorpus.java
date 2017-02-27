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

package textractor.crf;

import edu.mssm.crover.cli.CLI;
import textractor.util.ProcessDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse the MRG file format (see http://bioie.ldc.upenn.edu/publications/latest_release/doc/Treebanking/readme-mrg-files.html)
 *
 * @author Fabien Campagne
 *         Date: Aug 18, 2006
 *         Time: 5:39:10 PM
 */
public final class ParseMRGCorpus {
    private final int typeNumber = 1;
    private List<TaggedDocument> taggedDocuments;

    public void parse(final Reader input) throws IOException {
        taggedDocuments = new ArrayList<TaggedDocument>();
        final BufferedReader br = new BufferedReader(input);
        parseSections(br);
    }

    private void parseSections(final BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            final TaggedDocument doc = new TaggedDocument();
            if (line.startsWith(";section")) {
                final String type = "(SEC";
                final TextSpan sectionSpan = extractSectionSpan(line);
                final String previousLine = extractText(br, type, doc);
                extractTags(br, previousLine, sectionSpan, doc);
                taggedDocuments.add(doc);
            } else if (line.startsWith(";sentence")) {
                final String type = "(SENT";
                final TextSpan sectionSpan = extractSectionSpan(line);
                final String previousLine = extractText(br, type, doc);
                extractTags(br, previousLine, sectionSpan, doc);
                taggedDocuments.add(doc);
            }

        }
    }

    private String extractTags(final BufferedReader br,
                               final String previousLine,
                               final TextSpan sectionSpan,
                               final TaggedDocument doc) throws IOException {
        if (previousLine != null) {
            parseOneTagLine(sectionSpan, previousLine, doc);
        }
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith(";[")) {
                parseOneTagLine(sectionSpan, line, doc);
            } else {
                return line;
            }
        }
        return null;
    }

    private void parseOneTagLine(final TextSpan sectionSpan,
                                 final String line,
                                 final TaggedDocument doc) {
        final String[] tokens;
        if (line.contains("]...[")) {
            // ignore multi-part tags for now.
            // TODO support multi-part tags. e.g., tags that jump over regions of text that are not tagged.
            return;
        } else {
            tokens = line.split(";\\[|\\]:|\\.\\.|\\:\"|\"");
        }

        final TextSpan span = new TextSpan();
        span.start = Integer.parseInt(tokens[1]) - sectionSpan.start;
        span.end = Integer.parseInt(tokens[2]) - sectionSpan.start;
        final Tag tag = new Tag();
        tag.span = span;
        tag.type = tokens[3];
        tag.text = tokens[4];
        doc.tags.add(tag);
    }

    private String extractText(final BufferedReader br,
                               final String type,
                               final TaggedDocument doc) throws IOException {
        String line;
        boolean multipleLines = false;
        while ((line = br.readLine()) != null) {
            if (line.startsWith(type)) {
                return null;
            } else if (line.startsWith(";[") && line.substring(0, 10).contains("..")) {
                return line;
            }

            if (multipleLines) {
                doc.text.append(' ');
            }

            doc.text.append(line.substring(1));
            if (!multipleLines) {
                multipleLines = true;
            }
        }
        return null;
    }

    private TextSpan extractSectionSpan(final String line) {
        final String[] tokens = line.split("Span:|\\.\\.");
        final TextSpan span = new TextSpan();
        span.start = Integer.parseInt(tokens[1]);
        span.end = Integer.parseInt(tokens[2]);

        return span;
    }

    public List<TaggedDocument> getTaggedDocuments() {
        return taggedDocuments;
    }

    public static void main(final String[] args) throws IOException {
        final ParseMRGCorpus parser = new ParseMRGCorpus();
        final boolean crfFormat = CLI.isKeywordGiven(args, "-crfFormat");
        final boolean testFormat = CLI.isKeywordGiven(args, "-test");
        final boolean contextOnly = CLI.isKeywordGiven(args, "-context-only");
        final String outputFilename = CLI.getOption(args, "-o", null);
        if (outputFilename == null) {
            System.out.println("Please provide an output filename (-o)");
            System.exit(10);
        }
        final PrintWriter output = new PrintWriter(new FileWriter(outputFilename));
        final DatasetWriter writer = (crfFormat ? new CRFDatasetWriter(output) :
                (contextOnly ? new MalletContextOnlyDatasetWriter(output) :
                        new MalletDatasetWriter(output)));
        writer.setTestDataset(testFormat);
        final ProcessDirectory dirProcessor = new ProcessDirectory(args, ".mrg") {
            @Override
	    public void processFile(final Reader reader, final String filename, final String output_filename) throws IOException {
                parseOneFile(parser, filename, writer);
            }
        };
        dirProcessor.process();
        writer.close();
        System.out.println("Number of labels: " + writer.getLabelCount());
        System.out.println("Number of features: " + writer.getFeatureCount());
    }

    private static void parseOneFile(final ParseMRGCorpus parser,
                                     final String inputFilename,
                                     final DatasetWriter writer) throws IOException {
        parser.parse(new FileReader(inputFilename));
        for (final TaggedDocument doc : parser.getTaggedDocuments()) {
            int lastTagEnd = 0;
            for (final Tag currentTag : doc.tags) {
                if (currentTag.span.start < lastTagEnd) {
                    // ignore tags that overlap
                    continue;
                }
                final CharSequence nonTaggedText =
                    doc.text.subSequence(lastTagEnd, currentTag.span.start);
                lastTagEnd = currentTag.span.end;

                writer.writeTag(nonTaggedText, "NOTAG");
                writer.writeTag(currentTag.text, currentTag.type);
            }
            if (lastTagEnd < doc.text.length()) {
                writer.writeTag(doc.text.subSequence(lastTagEnd, doc.text.length()), "NOTAG");
            }

        }
    }

    public int getTagTypeCount() {
        return typeNumber;
    }
}
