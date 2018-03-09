package com.matthey.openlink.reporting.runner.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import com.matthey.openlink.reporting.runner.ReportRunnerException;
import com.matthey.openlink.reporting.runner.parameters.IReportParameters;
import com.matthey.openlink.reporting.runner.parameters.IReportWithOutputParameter;
import com.olf.openrisk.application.Session;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

/**
 *
 */
public class GenerateAndCopyOutput extends ReportGeneratorBase implements
        IReportWithOutputParameter {


    /**
     * Generate a report builder report and copy the output report to a new location. The output location is defined in
     * the user table as a parameter
     * 
     * @param newParameters Parameters used to control the generation of the report
     */
    public GenerateAndCopyOutput(final Session session, final IReportParameters newParameters) {

        super(session, newParameters);
    }

    @Override
    protected final boolean initialise() {

        if (!getParameters().hasParameter(OUTPUT_LOCATION_PARAMETER)) {
            String errorMessage = "Parameter " + OUTPUT_LOCATION_PARAMETER + " is not defined.";
            Logger.log(LogLevel.ERROR, LogCategory.General, this.getClass(),errorMessage);
            throw new ReportRunnerException(errorMessage);
        }

        String finalOutputLocation = getParameters().getParameterValue(OUTPUT_LOCATION_PARAMETER);

        File outputDirectory = new File(finalOutputLocation);

        if (!outputDirectory.isDirectory()) {
            String errorMessage = "Parameter " + OUTPUT_LOCATION_PARAMETER
                    + " does not point to a valid location. " + finalOutputLocation + " is invalid";
            Logger.log(LogLevel.ERROR, LogCategory.General, this.getClass(),errorMessage);
            throw new ReportRunnerException(errorMessage);
        }

        return true;
    }

    @Override
    protected final Boolean postProcess() {

        String reportLocation = getReportBuilder().getParameter("ALL", "OUTPUT_FILENAME");

        String finalOutputLocation = getParameters().getParameterValue(OUTPUT_LOCATION_PARAMETER);

        File sourceFile = new File(reportLocation);

        String outputFileName = finalOutputLocation + "\\" + sourceFile.getName();

        File destFile = new File(outputFileName);

        try {
            copyFile(sourceFile, destFile);
        } catch (Exception e) {
            String errorMessage = "Error copying file from " + sourceFile.getAbsolutePath()
                    + " to " + destFile.getAbsolutePath() + ". " + e.getMessage();
            Logger.log(LogLevel.ERROR, LogCategory.General, this.getClass(),errorMessage, e);
            throw new ReportRunnerException(errorMessage);
        }

        return true;
    }

    /**
     * Copy a file.
     * 
     * @param sourceFile Location of the file to copy
     * @param destFile Name and location of the output file
     * @throws IOException on file error
     */
    static void copyFile(final File sourceFile, final File destFile) throws IOException {

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;

        try {
            sourceChannel = new FileInputStream(sourceFile).getChannel();
            destinationChannel = new FileOutputStream(destFile).getChannel();

            if (sourceChannel == null || destinationChannel == null) {
                throw new ReportRunnerException("Error opening channels needed for file copy");
            }
            destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());

        } finally {
            if (sourceChannel != null) {
                sourceChannel.close();
            }
            if (destinationChannel != null) {
                destinationChannel.close();
            }
        }
    }

}
