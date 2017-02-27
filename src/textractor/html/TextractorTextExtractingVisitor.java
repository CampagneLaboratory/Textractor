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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Attribute;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.util.Translate2;
import org.htmlparser.visitors.NodeVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts text from a web page and remembers the numeric
 * position of each kept character in the file.
 * <p/>
 * All newlines & tabs are treated as a single space
 * Multiple spaces are removed<br/>
 * alt properties within &lt;img&gt; tags are treated as text  <br/>
 * XHTML character entities such as &amp;nbsp; are converted
 * to their unicode variation (including within an img/alt tag). <br/>
 * &amp;nbsp; is treated as a normal space. <br/>
 * a space (and thus a non-breaking space) is forced before &lt;p&gt; and &lt;br&gt; tags<br/>
 * a ". " is forced before and after &lt;H1&gt;, &lt;H2&gt;, &lt;H3&gt;, &lt;H4&gt;  v
 * ". " will never be immediately followed by another ". "  <br/>
 * ">&amp;IMG ..." will always be treated as "> &amp;IMG "     <br/>
 * "&amp;sup&gt;&amp;a ..." will always be treated as "&amp;sup&gt; sup&amp;a ..."      <br/>
 * <p/>
 * The processed text within the &amp;title&gt; tag will be available
 * via getExtractedTitle(). If keepTitleInBody is true, that same
 * title will be within getExtractedText().
 * getExtractedPositions() returns an int[] which specifies
 * the positions of every character within the original
 * file. The length of the int[] is the same as the length
 * of the string returned by getExtractedText().
 * This class is based on the TextExtractingVisitor class
 * provided by org.htmlparser. This class uses a modified
 * version of Translate (also by org.htmlparser) that
 * can aid with tracking the positions which is required here.
 */
public final class TextractorTextExtractingVisitor extends NodeVisitor {
    private static final char[] greekChars = {913, 914, 915, 916, 917, 918, 919, 920, 921, 922, 923, 924,
            925, 926, 927, 928, 929, 931, 932, 933, 934, 935, 936, 937, 945, 946, 947, 948, 949, 950, 951,
            952, 953, 954, 955, 956, 957, 958, 959, 960, 961, 962, 963, 964, 965, 966, 967, 968, 969};
    private static final char greekStart = greekChars[0];
    private static final char greekEnd = greekChars[greekChars.length - 1];
    private static final String[] greekStrings = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta",
            "Eta", "Theta", "Iota", "Kappa", "Lamda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma",
            "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega", "alpha", "beta", "gamma", "delta", "epsilon",
            "zeta", "eta", "theta", "iota", "kappa", "lamda", "mu", "nu", "xi", "omicron", "pi", "rho",
            "finalsigma", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega"};

    private final MutableString textAccumulator;

    private final MutableString titleAccumulator;

    private boolean inTitle;

    private final boolean keepTitleInBody;

    private final List<Integer> positions;

    private final MutableString originalText;
    private String paragraphMarkerTag;

    public TextractorTextExtractingVisitor(final MutableString originalText) {
        this.textAccumulator = new MutableString(originalText.length());
        this.titleAccumulator = new MutableString(100);
        this.originalText = originalText;
        this.keepTitleInBody = true;
        this.inTitle = false;
        this.positions = new ArrayList<Integer>(originalText.length());
    }

    public MutableString getExtractedText() {
        return textAccumulator;
    }

    public MutableString getExtractedTitle() {
        return titleAccumulator;
    }

    public List<Integer> getExtractedPositions() {
        return positions;
    }

    @Override
    public void visitStringNode(final Text stringNode) {
        appendText(stringNode.getStartPosition(), stringNode.getText());
    }

    private char lastChar = 'x';

    /**
     * Updates text and position information.
     *
     * @param pos      The start position of the text in the original html
     * @param origText The original text from the html
     */
    private void appendText(final int pos, final String origText) {
        appendText(pos, origText, true);
    }

    /**
     * Updates text and position information.
     *
     * @param pos               The start position of the text in the original html
     * @param origText          The original text from the html
     * @param incrementPosition if true, the positions stored will
     *                          be incremented by the size of the text.  This is the typical situation
     *                          since the text appeared in the original source.  If false, the
     *                          start position will be used for all positions in this case.
     */
    private void appendText(final int pos, final String origText,
                            final boolean incrementPosition) {
        final MutableString text = new MutableString(
                Translate2.decodeSameLength(origText));
        simplifyWhitespace(text);
        replaceNbspWithSpace(text);

        if (inTitle) {
            titleAccumulator.append(text);
            if (!keepTitleInBody) {
                return;
            }
        }

        if (text.equals(". ")) {
            // Check if the last two characters were ". ",
            // if so, we don't need to repeat ourselves.
            if (textAccumulator.endsWith(". ")) {
                return;
            }
            if (textAccumulator.endsWith(".")) {
                text.setLength(0);
                text.append(" ");
            }
        }

        final int length = text.length();
        for (int strPos = 0; strPos < length; strPos++) {
            final char curChar = text.charAt(strPos);
            if (curChar != '\0') {
                final String greekString = unicodeGreekCharToString(curChar);
                if (greekString != null) {
                    // Character handled, it was Greek.
                    outputGreekString(pos + strPos, greekString);
                } else if ((curChar == ' ') && (lastChar == ' ')) {
                    // Skip this character.
                } else {
                    textAccumulator.append(curChar);
                    if (incrementPosition) {
                        positions.add(pos + strPos);
                    } else {
                        positions.add(pos);
                    }
                }
                lastChar = curChar;
            }
        }
    }

    /**
     * Convert the incoming (unicode) char to the equivalent
     * Greek string or null if the incoming char is NOT
     * a Greek unicode character.
     * @param curChar incoming (unicode) char
     * @return Greek string (or null if char isn't greek)
     */
    public static String unicodeGreekCharToString(final char curChar) {
        if ((curChar >= greekStart) && (curChar <= greekEnd)) {
            // Within the Greek range. Is it really Greek?
            final int pos = ArrayUtils.indexOf(greekChars, curChar);
            if (pos == -1) {
                // Not really Greek
                return null;
            }
            // It IS Greek return the string
            return greekStrings[pos].toLowerCase();
        } else {
            // Not within the range of Greek character values
            return null;
        }
    }

    private void outputGreekString(final int pos, final String greekString) {
        if (StringUtils.isBlank(greekString)) {
            // No greek word to deal with
            return;
        }
        // Character IS Greek
        final int length = greekString.length();
        for (int i = 0; i < length; i++) {
            textAccumulator.append(greekString.charAt(i));
            positions.add(pos);
        }
    }

    private void simplifyWhitespace(final MutableString text) {
        text.replace("\n", " ");
        text.replace("\r", " ");
        text.replace("\t", " ");
    }

    private void replaceNbspWithSpace(final MutableString text) {
        text.replace('\u00a0', ' ');
    }

    @Override
    public void visitTag(final Tag tag) {
        if (isTitleTag(tag)) {
            inTitle = true;
        }
        if (isImageTag(tag)) {
            // If we have "><IMG ..." insert a space in the position of the "<"
            // which will get eaten by the parser.
            if (originalText.substring(tag.getStartPosition() - 1, tag.getStartPosition()).equals(">")) {
                appendText(tag.getStartPosition(), " ");
            }

            final Attribute alt = tag.getAttributeEx("ALT");
            if (alt != null) {
                extractAttribute(tag, alt);
            }
        }
        // If we have "<sup><a ..." insert a " sup" at the place
        // of "<sup>" which will get eaten by the parser.
        if (isSupTag(tag)) {
            try {
                final MutableString postSup = originalText.substring(tag.getStartPosition() + 5, tag.getStartPosition() + 8);
                if (postSup.equalsIgnoreCase("<a ")) {
                    appendText(tag.getStartPosition(), " sup");
                }
            } catch (StringIndexOutOfBoundsException e) {
                // It didn't match, no biggie.
            }
        }
        // If we have a <Hx> tag insert a ". " in the position of the "<"
        // which will get eaten by the parser. This makes sure
        // headers start as a new sentence.
        if (isHeaderTag(tag)) {
            appendText(tag.getStartPosition(), ". ");
        }
        // If we have a <P> or <BR>< tag insert a "\n" in the position of the "<"
        // which will get eaten by the parser. This makes sure
        // breaks start as a new word. Note that appendText
        // will convert this to a " ".
        if (isBreakTag(tag)) {
            appendText(tag.getStartPosition(), "\n");
        }

        // if we are inserting an artificial paragraph marker, we don't
        // want to increment positions based on the size of the marker
        // since the tag did not appear in the original source.
        if (paragraphMarkerTag != null && tag.getTagName().equals("P")) {
            appendText(tag.getStartPosition(), paragraphMarkerTag, false);
        }
    }

    @Override
    public void visitEndTag(final Tag tag) {
        if (isTitleTag(tag)) {
            inTitle = false;
        }
        // If we have a </Hx> tag insert a ". " in the position of the "<"
        // which will get eaten by the parser. This makes sure
        // headers end on as a sentence.
        if (isHeaderTag(tag)) {
            appendText(tag.getStartPosition(), ". ");
        }
    }

    public void extractAttribute(final Tag tag, final Attribute alt) {

        //
        // Note that this is inexact... We know that there is an
        // ALT attribute containing some specified text, here were are trying
        // to find that text within the whole <img> tag.
        // BUT if that string occurs elsewhere we might match it
        // to the wrong place. This isn't REALLY a big deal,
        // but, it should be known that it is currently possible
        // we might match it to some other part of the <img>
        // tag. This should be improved to make sure it
        // matches to the ALT attribute - but doing that
        // would be easier if the Attribute object
        // contained .getStartPosition() which it doesn't.
        // I have requested to the htmlparser developer
        // that he add this feature.

        final int pos = tag.getStartPosition();
        final String trimmedAlt = StringUtils.trimToEmpty(alt.getValue());

        // We add +1 because tag.getText doesn't include the leading "<".
        final int altPos = pos + tag.getText().indexOf(trimmedAlt) + 1;
        if (altPos == pos) {
            // Didn't find it.
            return;
        }
        final String noBracesAlt = normalizeAlt(trimmedAlt);
        appendText(altPos, noBracesAlt);
    }

    public static String normalizeAlt(final String trimmedAlt) {
        String noBracesAlt = trimmedAlt;
        if (trimmedAlt.startsWith("{") && trimmedAlt.endsWith("}")) {
          // some journals like to have braces around "beta" such as in "{beta}". We remove them for normalization.
            noBracesAlt = trimmedAlt.substring(1,
                    trimmedAlt.length() - 1);
        }
        return noBracesAlt;
    }

    private boolean isTitleTag(final Tag tag) {
        return tag.getTagName().equals("TITLE");
    }

    private boolean isHeaderTag(final Tag tag) {
        return tag.getTagName().equals("H1") || tag.getTagName().equals("H2")
                || tag.getTagName().equals("H3")
                || tag.getTagName().equals("H4");
    }

    private boolean isBreakTag(final Tag tag) {
        return tag.getTagName().equals("P") || tag.getTagName().equals("BR");
    }

    private boolean isImageTag(final Tag tag) {
        return tag.getTagName().equals("IMG");
    }

    private boolean isSupTag(final Tag tag) {
        return tag.getTagName().equals("SUP");
    }

    public void setParagraphMarkerTag(final CharSequence tag) {
        this.paragraphMarkerTag = (tag != null) ? tag.toString() : null;
    }
}
