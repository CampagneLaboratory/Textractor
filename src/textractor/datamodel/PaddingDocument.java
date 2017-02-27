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

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.mg4j.document.PropertyBasedDocumentFactory.MetadataKeys;
import org.apache.commons.lang.StringUtils;

/**
 * Instances of this class are used to replace documents that have been deleted.
 * They help maintain consistently increasing document numbers.
 * Date: Nov 4, 2005
 * Time: 3:55:08 PM
 */
public final class PaddingDocument extends TextractorDocument {
    @Override
    public String getText() {
        return StringUtils.EMPTY;
    }

    @Override
    public Reference2ObjectMap<Enum<?>, Object> getMetaData() {
        final Reference2ObjectMap<Enum<?>, Object> metadata = super.getMetaData();
        final String title = (String) metadata.get(MetadataKeys.TITLE);
        metadata.put(MetadataKeys.TITLE, title + " (empty)");
        return metadata;
    }
}
