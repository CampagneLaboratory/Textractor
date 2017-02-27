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

package textractor.tools.expansion;

/**
 *
 */
public final class ExpansionTerm {
    private String term;
    private long frequency;
    private String shortForm;

    public ExpansionTerm() {
        super();
    }

    public ExpansionTerm(final String newTerm, final long newFrequency) {
        super();
        setTerm(newTerm);
        setFrequency(newFrequency);
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(final String term) {
        this.term = term;
    }

    public long getFrequency() {
        return frequency;
    }

    public void setFrequency(final long frequency) {
        this.frequency = frequency;
    }

    public String getShortForm() {
        return shortForm;
    }

    public void setShortForm(final String shortForm) {
        this.shortForm = shortForm;
    }
}
