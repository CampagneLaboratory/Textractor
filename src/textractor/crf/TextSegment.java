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

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 *
 */
public final class TextSegment implements Comparable<TextSegment> {
    private String text;
    private int score;
    /**
     * The byte offset in the Doc ID file where the passage begins, where the
     * first character of the file is offset 0.
     */
    private long start;

    /**
     * The length of the passage in bytes, in 8-bit ASCII, not Unicode.
     */
    private long length;

    /**
     * Name of the HTML file minus the .html extension. This is the PMID that
     * has been designated by Highwire, even though we now know that this may
     * not be the true PMID assigned by the NLM (i.e., used in MEDLINE). But
     * this is the official identifier for the document.
     */
    private long pmid;

    /**
     * Rank of the passage for the topic, starting with 1 for the top-ranked
     * passage and preceding down to as high as 1000.
     */
    private int rankNumber;

    /**
     * System-assigned score for the rank of the passage, an internal number
     * that should descend in value from passages ranked higher.
     */
    private float rankValue;

    /**
     * For TREC 2006 Genomics Track this should befrom 160 to 187.
     */
    private int topicId;

    public TextSegment() {
        super();
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public int getScore() {
        return score;
    }

    public void setScore(final int score) {
        this.score = score;
    }

    public long getStart() {
        return start;
    }

    public void setStart(final long start) {
        this.start = start;
    }

    public long getLength() {
        return length;
    }

    public void setLength(final long length) {
        this.length = length;
    }

    public long getPmid() {
        return pmid;
    }

    public void setPmid(final long pmid) {
        this.pmid = pmid;
    }

    public int getRankNumber() {
        return rankNumber;
    }

    public void setRankNumber(final int rankNumber) {
        this.rankNumber = rankNumber;
    }

    public float getRankValue() {
        return rankValue;
    }

    public void setRankValue(final float rankValue) {
        this.rankValue = rankValue;
    }

    /**
     * @return the topicId
     */
    public int getTopicId() {
        return topicId;
    }

    /**
     * @param topicId the topicId to set
     */
    public void setTopicId(final int topicId) {
        this.topicId = topicId;
    }

    @Override
    public String toString() {
        final char fieldsep = '\t';
        final StringBuffer buffer = new StringBuffer(128);
        buffer.append(topicId);
        buffer.append(fieldsep);
        buffer.append(pmid);
        buffer.append(fieldsep);
        buffer.append(rankNumber);
        buffer.append(fieldsep);
        buffer.append(rankValue);
        buffer.append(fieldsep);
        buffer.append(start);
        buffer.append(fieldsep);
        buffer.append(length);
        return buffer.toString();
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof TextSegment)) {
            return false;
        }

        final TextSegment segment = (TextSegment)object;
        return new EqualsBuilder().
                append(topicId, segment.topicId).
                append(pmid, segment.pmid).
                append(start, segment.start).
                append(length, segment.length).
                append(text, segment.text).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
            append(topicId).
            append(pmid).
            append(start).
            append(length).
            append(text).
            toHashCode();
    }

    public int compareTo(final TextSegment segment) {
        if (this.equals(segment)) {
            return 0;
        } else {
            return new CompareToBuilder().
                append(topicId, segment.topicId).
                append(rankNumber, segment.rankNumber).
                toComparison();
        }
    }
}
