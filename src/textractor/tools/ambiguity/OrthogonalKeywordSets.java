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

package textractor.tools.ambiguity;

/**
 * User: Fabien Campagne
 * Date: Jul 31, 2005
 * Time: 6:26:52 PM
 */
public final class OrthogonalKeywordSets implements Comparable<OrthogonalKeywordSets> {
    private String seedKeyword;
    private String firstKeyword;
    private int[] firstSetOfDocuments;
    private String secondKeyword;
    private int intersectionCount;
    private int x;
    private int y;
    private int N1;
    private int N2;
    private int[] secondSetOfDocuments;
    private double score;

    public OrthogonalKeywordSets(final KeywordPairAnalysed keywordPairAnalysed1,
                                 final KeywordPairAnalysed keywordPairAnalysed2,
                                 final int[] intersection) {

        firstKeyword = keywordPairAnalysed1.secondKeyword;
        secondKeyword = keywordPairAnalysed2.secondKeyword;
        seedKeyword = keywordPairAnalysed1.firstKeyword;
        assert keywordPairAnalysed1.firstKeyword.equals(keywordPairAnalysed2.firstKeyword) : "first keyword must be seed keyword and common between the two pairs";
        intersectionCount = intersection.length;

        calculateScore(keywordPairAnalysed1, keywordPairAnalysed2, intersection);
        firstSetOfDocuments = keywordPairAnalysed1.getMatchingDocuments();
        secondSetOfDocuments = keywordPairAnalysed2.getMatchingDocuments();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getN1() {
        return N1;
    }

    public int getN2() {
        return N2;
    }

    private void calculateScore(final KeywordPairAnalysed keywordPairAnalysed1, final KeywordPairAnalysed keywordPairAnalysed2, final int[] intersection) {
        final int W1 = keywordPairAnalysed1.getMatchingDocuments().length;
        final int W2 = keywordPairAnalysed2.getMatchingDocuments().length;
        final int W1_inter_W2 = intersection.length;
        final int S = keywordPairAnalysed1.getSeedCorpusFrequency();


        x = W1_inter_W2;
        y = W2 - W1_inter_W2;
        N1 = W1;
        N2 = S - W1;

        double oldScore = intersection.length * intersection.length + 1;
        oldScore /= keywordPairAnalysed1.getMatchingDocuments().length;
        oldScore /= keywordPairAnalysed2.getMatchingDocuments().length;
        score = 1d/ oldScore;
    }

    public String getSeedKeyword() {
        return seedKeyword;
    }

    public void setSeedKeyword(final String seedKeyword) {
        this.seedKeyword = seedKeyword;
    }

    public String getFirstKeyword() {
        return firstKeyword;
    }

    public void setFirstKeyword(final String firstKeyword) {
        this.firstKeyword = firstKeyword;
    }

    public int[] getFirstSetOfDocuments() {
        return firstSetOfDocuments;
    }

    public void setFirstSetOfDocuments(final int[] firstSetOfDocuments) {
        this.firstSetOfDocuments = firstSetOfDocuments;
    }

    public String getSecondKeyword() {
        return secondKeyword;
    }

    public void setSecondKeyword(final String secondKeyword) {
        this.secondKeyword = secondKeyword;
    }

    public int[] getSecondSetOfDocuments() {
        return secondSetOfDocuments;
    }

    public void setSecondSetOfDocuments(final int[] secondSetOfDocuments) {
        this.secondSetOfDocuments = secondSetOfDocuments;
    }

    public double getScore() {
        return score;
    }

    public void setScore(final double score) {
        this.score = score;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     * <p/>
     * In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of
     * <i>expression</i> is negative, zero or positive.
     * <p/>
     * The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)<p>
     * <p/>
     * The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.<p>
     * <p/>
     * Finally, the implementer must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.<p>
     * <p/>
     * It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * @param otherSet the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    public int compareTo(final OrthogonalKeywordSets otherSet) {
        return Double.compare(score, otherSet.score);
    }

    public int getIntersectionCount() {
        return intersectionCount;
    }

    public boolean matches(final String word1, final String word2) {
        return (getFirstKeyword().equals(word1) && getSecondKeyword().equals(word2)) ||
                (getFirstKeyword().equals(word2) && getSecondKeyword().equals(word1));
    }
}
