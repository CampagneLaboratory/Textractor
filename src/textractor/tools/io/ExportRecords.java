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

package textractor.tools.io;

import edu.mssm.crover.cli.CLI;
import org.apache.commons.configuration.ConfigurationException;
import textractor.database.TextractorDatabaseException;
import textractor.datamodel.FeatureCreationParameters;
import textractor.datamodel.annotation.DoubleTermAnnotation;
import textractor.datamodel.annotation.SingleTermAnnotation;
import textractor.learning.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: lshi
 * Date: May 17, 2004
 * Time: 3:30:30 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ExportRecords extends ExportImport{
    protected SVMFeatureExporter exporter;
    public static final int NO_TEST_SET = -1;
    public static final int NO_PARAMETER_SET = -1;
    protected int parameter_set;
    protected int windowWidth;
    protected String recordType;
    protected boolean verbose;
    protected int batchId;

    protected void process(final String[] args) throws IOException, TextractorDatabaseException, ConfigurationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, URISyntaxException {
        super._process(args);
        if (outputFilename == null) {
            System.err.println("Output file must be provided (-o).");
            printHelp();
            System.exit(1);
        }
        if (batchId == -1 ) {
            System.err.println("Batch id must be provided (-batch)");
            printHelp();
            System.exit(1);
        }
        if (batchId == -2 ) {
            batchId=tm.getLatestAnnotationBatchNumber();
            System.out.println("using batch: "+batchId);
        }
        initExporter();
        final FeatureCreationParameters parameters = initParameter();
        final int paramNumber = export(parameters);
        System.err.println("Parameters of this export saved in parameter number " + paramNumber);
        writer.flush();
        writer.close();
        dbm.commitTxn();
    }

    @Override
    protected void getParameters(final String[] args) throws IOException {
        super.getParameters(args);

        batchId = CLI.getIntOption(args, "-batch", -1);
        windowWidth = CLI.getIntOption(args, "-window", 5);
        recordType = CLI.getOption(args, "-recordType", "protein");
        parameter_set = CLI.getIntOption(args, "-parameter", NO_PARAMETER_SET);
        verbose = CLI.isKeywordGiven(args, "-v", false);

        if (CLI.isKeywordGiven(args, "-help")) {
            printHelp();
            System.exit(1);
        }
    }

    public abstract int export(FeatureCreationParameters parameters) throws IOException, TextractorDatabaseException;

    public void printHelp(){
    }

    public final void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    public final void setWindowWidth(final int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public final void setBatchId(final int batchId) {
        this.batchId = batchId;
    }

    public final void setParameterSet(final int parameter_set) {
        this.parameter_set = parameter_set;
    }

    protected final void initExporter() {
        if (recordType.equals(SingleTermAnnotation.ANNOTATION_PROTEIN)) {
            exporter = new ProteinNameBagOfWordExporter(docManager, true);
        } else if (recordType.equals(SingleTermAnnotation.ANNOTATION_MUTATION)) {
            exporter = new MutationBagOfWordExporter(docManager, true);
        } else if (recordType.equals(DoubleTermAnnotation.ANNOTATION_PHOSPHORYLATE)) {
            exporter = new PhosphorylateBagOfWordExporter(docManager);
        } else if (recordType.equals(SingleTermAnnotation.ANNOTATION_MULTICLASS)){
            exporter = new MultiClassBagOfWordExporter(docManager, true);
        }
    }

    protected final FeatureCreationParameters initParameter() {
        FeatureCreationParameters parameters=null;
        if (parameter_set != NO_PARAMETER_SET) {
            parameters = tm.getParameterSetById(parameter_set);
            if (parameters == null) {
                System.err.println("Parameter set " + parameter_set + " cannot be found in the database.");
                System.exit(1);
            }
            //update the index for the terms from the current index
            parameters.updateIndex(docManager);
        }else{
            parameters=exporter.createFeatureCreationParameters();
            parameters.setParameterNumber(tm.getNextParameterNumber());
            parameters.setWindowSize(windowWidth);
        }
        return parameters;
    }
}
