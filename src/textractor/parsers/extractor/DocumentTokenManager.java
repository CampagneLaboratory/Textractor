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

package textractor.parsers.extractor;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.mg4j.util.MutableString;
import textractor.database.DocumentIndexManager;

/**
 * Translates a textractor document (coded as an int[]) into a stream of tokens
 * for JavaCC parsers.
 *
 * @author Fabien Campagne
 *         Date: Nov 16, 2005
 *         Time: 1:35:13 PM
 */
public final class DocumentTokenManager implements TokenManager {
    private final int[] document;
    private int currentPosition;
    /**
     * A map from
     */
    private static final Int2IntMap unlabeledTokensToTerms = new Int2IntOpenHashMap();
    private static final Int2IntMap termToTokenIndex = new Int2IntOpenHashMap();
    private DocumentIndexManager docmanager;
    public static final String WORD_NOT_IN_INDEX = "WORD-NOT-IN-INDEX";
    private static final String SPECIAL_WORD_TOKEN = "\"%WORD%\"";
    private int WordClassIndex;
    private int endPosition;

    /**
     * Initialize with a new document.
     *
     * @param document   Document to be parsed.
     * @param docmanager The manager to use to convert from words to indices.
     */
    public DocumentTokenManager(final int[] document, final DocumentIndexManager docmanager) {
        this.document = document;
        this.currentPosition = -1;
        this.docmanager = docmanager;

        unlabeledTokensToTerms.defaultReturnValue(-1);
        termToTokenIndex.defaultReturnValue(-1);
        final MutableString token = new MutableString();
        // register constant parser tokens:
        int offset = -1;
        for (int i = 0; i < ExtractionEngineConstants.tokenImage.length; ++i) {
            token.setLength(0);
            final String tokenString = ExtractionEngineConstants.tokenImage[i];
            if (tokenString.equals(SPECIAL_WORD_TOKEN)) {
                WordClassIndex = i;
            } else if (tokenString.charAt(0) == '<') { // named TOKEN kinds
                offset++;
            } else
            if (tokenString.charAt(0) == '"') { // token is not named in the parser, it represents a word of the index
                // remove the enclosing quotes:
                token.append(tokenString.subSequence(1, tokenString.length() - 1));
                docmanager.processTerm(token);


                final int index = docmanager.findTermIndex(token);
                if (index != DocumentIndexManager.NO_SUCH_TERM) {
                    // associate the index of the term in the docmanager to the index of the token in the parser.
                    unlabeledTokensToTerms.put(i - offset, index);
                    termToTokenIndex.put(index, i - offset);
                }
            }
        }
        endPosition = document.length;
    }

    /**
     * Initialize with a new document and a starting position in the document.
     * Parsing will start at the position indicated. Previous tokens will be
     * ignored.
     *
     * @param startPosition Position where to start parting in document.
     * @param document      Document to be parsed.
     * @param docmanager    The manager to use to convert from words to indices.
     */
    public DocumentTokenManager(final int[] document, final DocumentIndexManager docmanager, final int startPosition) {
        this(document, docmanager);
        this.currentPosition = startPosition - 1;
    }

    /**
     * Initialize with a new document and a starting position in the document.
     * Parsing will start at the position indicated and will parse until
     * endPosition. &lt;EOF&gt; will be returned past endPosition, so that
     * parsing can be restricted to a portion of the document.
     *
     * @param startPosition Position where to start parting in document.
     * @param endPosition   Calling nextToken past this position will return
     * &lt;EOF&gt;
     * @param document      Document to be parsed.
     * @param docmanager    The manager to use to convert from words to indices.
     */
    public DocumentTokenManager(final int[] document,
                                final DocumentIndexManager docmanager, final int startPosition,
                                final int endPosition) {
        this(document, docmanager);
        this.currentPosition = startPosition - 1;
        this.endPosition = endPosition;
    }

    public Token getNextToken() {
        ++currentPosition;
        final Token token = new Token();

        if (currentPosition >= endPosition) {
            return eof();
        }

        final int word = document[currentPosition];
        final int tokenIndex = termToTokenIndex.get(word);
        if (tokenIndex != termToTokenIndex.defaultReturnValue()) {
            // a specific type of WORD that we map to the parser representation:
            token.kind = tokenIndex;

        } else { // A general WORD
            token.kind = WordClassIndex;
        }

        token.image = docmanager.termAsString(word);

        return token;
    }

    private Token eof() {
        final Token eof = new Token();
        eof.kind = ExtractionEngineConstants.EOF;
        eof.image = ExtractionEngineConstants.tokenImage[ExtractionEngineConstants.EOF];
        return eof;
    }
}
