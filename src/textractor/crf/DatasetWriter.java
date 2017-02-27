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

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Fabien Campagne
 * Date: Aug 22, 2006
 * Time: 11:08:43 AM

 */
public interface DatasetWriter extends Closeable {
    void writeTag(CharSequence nonTaggedText, String tagType) throws IOException;
    int getLabelCount();
    int getFeatureCount();
    void setTestDataset(boolean testFormat);
}
