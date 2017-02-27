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

package textractor.datamodel;

import java.util.Collection;

/**
 * A specialized {@link textractor.datamodel.Article} that is used to
 * represent articles presented/published at
 * <a href="http://www.sfn.org/">The Society for Neuroscience</a>
 * annual meeting.
 */
public final class SfnArticle extends Article {
    private Collection<String> authors;
    private String title;
    private String sessionDescription;
    private String institution;
    private long controlNumber;
    private String presentationNumber;

    public SfnArticle() {
        super();
        setPmid(-1);
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(final String institution) {
        this.institution = institution;
    }

    public String getSessionDescription() {
        return sessionDescription;
    }

    public void setSessionDescription(final String sessionDescription) {
        this.sessionDescription = sessionDescription;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public long getControlNumber() {
        return controlNumber;
    }

    public void setControlNumber(final long controlNumber) {
        this.controlNumber = controlNumber;
    }

    public String getPresentationNumber() {
        return presentationNumber;
    }

    public void setPresentationNumber(final String presentationNumber) {
        this.presentationNumber = presentationNumber;
    }
}
