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

package textractor.util;

import edu.mssm.crover.cli.CLI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Used to clean the results from the ImportSVMResultsByAnnotation class.
 * Some Articles have the same most frequent terms, so we
 * average the predicted ratio for these.
 */
public final class CleanRatioData {
    public static void main(final String[] args) throws IOException {
        final String statistics_input_filename = CLI.getOption(args, "-i", "statistics.txt");
        final String excel_output_filename = CLI.getOption(args, "-o", "statistics_clean.txt");
        final BufferedReader reader = new BufferedReader(new FileReader(statistics_input_filename));
        final FileWriter writer = new FileWriter(excel_output_filename);
        final CleanRatioData crd = new CleanRatioData();
        final Map<String, String> data = crd.cleanData(reader);
        crd.writeData(writer, data);
    }

    public Map<String, String> cleanData(final BufferedReader reader) throws IOException {
        final Map<String, String> data = new HashMap<String, String>();
        String line;
        while ((line = reader.readLine()) != null) {
            final StringTokenizer st = new StringTokenizer(line);
            final String name = st.nextToken();
            final String ratio_str = st.nextToken();
            final double new_ratio = Double.parseDouble(ratio_str);
            if (data.containsKey(name)) {
                final double previous_ratio = Double.parseDouble(data.get(name));
                final double ratio = (new_ratio + previous_ratio) / 2.0;
                data.remove(name);
                data.put(name, Double.toString(ratio));
            } else {
                data.put(name, ratio_str);
            }
        }
        return data;
    }

    public void writeData(final FileWriter writer, final Map<String, String> data) throws IOException {
        writer.write("term\tratio\n");
        for (final String term : data.keySet()) {
            writer.write(term + "\t" + data.get(term) + "\n");
        }
    }
}
