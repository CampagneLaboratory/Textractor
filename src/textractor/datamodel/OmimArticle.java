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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang.ArrayUtils;

import java.util.Collection;

/**
 * A specialized {@link Article} that is used to
 * represent articles published at
 * <a href="http://www.ncbi.nlm.nih.gov/sites/entrez?db=OMIM">
 * Online Mendelian Inheritance in Man&tm;
 * </a>
 */
public final class OmimArticle extends Article {
    private String title;
    private Collection<String> aliases;
    private IntList refPmids;

    public OmimArticle() {
        super();
        refPmids = new IntArrayList();
        aliases = new ObjectArrayList<String>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public Collection<String> getAliases() {
        return aliases;
    }

    public void setAliases(final Collection<String> aliases) {
        this.aliases = aliases;
    }

    public IntList getRefPmids() {
        return refPmids;
    }

    public void setRefPmids(final IntList refPmids) {
        this.refPmids = refPmids;
    }

    @Override
    public String toString() {
        return String.format("[pmid=%d:title=%s:aliases=%s:refPmids=%s]",
                pmid, title, ArrayUtils.toString(aliases),
                ArrayUtils.toString(refPmids));
    }
}
