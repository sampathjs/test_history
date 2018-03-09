package com.matthey.openlink.reporting.runner;

import java.lang.reflect.Constructor;

import com.matthey.openlink.reporting.runner.generators.IReportGenerator;
import com.matthey.openlink.reporting.runner.generators.IRequiredParameters;
import com.matthey.openlink.reporting.runner.parameters.IReportParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;

/**
 * Factory for instantiating correct object based on supplied arguments
 */
public final class ReportBuilderFactory {


    /**
     * prevent instantiation of helper class
     * 
     */
    private ReportBuilderFactory() {

    }

    /**
     * constants repository .
     * 
     */
    public static final String CONST_REPO_CONTEXT = "ReportRunner";
    public static final String CONST_REPO_SUBCONTEXT = "Configuration";

    /**
     * Factory method to load the class required to run a given report. Parameters are defined in a user table.
     * 
     * @param session
     *            effective session for running instance
     * @param taskName
     *            Name of task running the report; required in user table(parameter) lookup.
     * 
     * @return instance of a IReportGenerator class used to run the report.
     */
    public static IReportGenerator getGenerator(final Session session, final String taskName) {

        if (taskName == null || taskName.isEmpty()) {
            throw new ReportRunnerException("Invalid task name specified");
        }

        ConstRepository repository = null;
        String parameter;
        try {
            repository = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
            parameter = repository.getStringValue("type_parameter", IRequiredParameters.TYPE_PARAMETER);

        } catch (ConstantTypeException | ConstantNameException | OException e) {
            throw new ReportRunnerException("Unable to get generator, reason:"
                    + e.getLocalizedMessage(), e);
        }

        IReportParameters reportParameters = new ReportParameters(session, taskName);

        if (!reportParameters.hasParameter(parameter)) {
            throw new ReportRunnerException(String.format(
                    "Mandatory parameter %s is not defined for task %s", parameter, taskName));
        }

        String reportType = reportParameters.getParameterValue(parameter);

        IReportGenerator reportRunner = loadClass(session, reportType, reportParameters);

        if (reportRunner == null) {
            throw new ReportRunnerException(String.format(
                    "Reporting type %s is not valid for task %s", reportType, taskName));
        }

        return reportRunner;
    }

    /**
     * Helper method to load the generator class.
     * 
     * @param session active session
     * @param reportType report type to load
     * @param reportParameters reporting parameters used in the construction of the generator.
     * @return report generator class
     */
    private static IReportGenerator loadClass(final Session session, final String reportType,
            final IReportParameters reportParameters) {

        String generatorClass = "";
        if (reportType.contains(".")) {
            // The report type contains the full package name
            generatorClass = reportType;

        } else {
            String generatorPackage = IReportGenerator.class.getPackage().getName();
            generatorClass = generatorPackage + "." + reportType;
        }

        ClassLoader classLoader = ReportBuilderFactory.class.getClassLoader();

        Class<?> reportRunnerClass = null;
        try {
            reportRunnerClass = classLoader.loadClass(generatorClass);

        } catch (ClassNotFoundException e) {
        	Logger.log(LogLevel.ERROR, LogCategory.General, ReportBuilderFactory.class, String.format("Unable to find %s", reportType), e);
            return null;
        }

        IReportGenerator reportRunner = null;
        try {
            Constructor<?> constructor = reportRunnerClass.getDeclaredConstructor(Session.class,
                    IReportParameters.class);
            constructor.setAccessible(true);

            reportRunner = (IReportGenerator) constructor.newInstance(session, reportParameters);

        } catch (Exception e) {
        	Logger.log(LogLevel.ERROR, LogCategory.General, ReportBuilderFactory.class, "Unable to get instance ctor:" + e.getMessage(), e);
            e.printStackTrace();
            return null;
        }

        return reportRunner;
    }
}
