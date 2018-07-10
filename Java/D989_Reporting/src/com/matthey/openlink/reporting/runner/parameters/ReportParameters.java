package com.matthey.openlink.reporting.runner.parameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.olf.openjvs.OException;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;
import com.openlink.util.constrepository.ConstRepository;

/**
 * Class to encapsulate the report parameters.
 * 
 * 
 */
public class ReportParameters implements IReportParameters {


    private static final String DEFAULT_USER_TABLE = "USER_report_parameters";
    private static final String DEFAULT_PARAMETER_TASK_NAME = "task_name";
    private static final String DEFAULT_VALUE_NAME = "value";
    private static final String DEFAULT_PARAMETER_NAME = "parameter";

    private ConstRepository implementationConstants;
    /**
     * constants repository .
     * 
     */
    public static final String CONST_REPO_CONTEXT = "ReportRunner";
    public static final String CONST_REPO_SUBCONTEXT = "Configuration";

    /**
     * parameters from the user table.
     * 
     */
    private Map<String, String> parameters;

    private Session session;

    public ReportParameters(final String taskName) {
    	this(Application.getInstance().getCurrentSession(), taskName);
    }
    
    /**
     * Load data from the user table and store in internal map.
     * 
     * @param taskName The name of the task data should be loaded for
     */
    public ReportParameters(final Session session, final String taskName) {

        if (taskName == null || taskName.isEmpty()) {
            throw new ReportRunnerParameters("Invalid task name specified");
        }

        this.session = session;

        try {
            implementationConstants = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
            String report_parameters = implementationConstants.getStringValue("UserTable", DEFAULT_USER_TABLE);
            String parameterColumn = implementationConstants.getStringValue("ParameterName", DEFAULT_PARAMETER_NAME);
            String valueColumn = implementationConstants.getStringValue("ParamenterValue", DEFAULT_VALUE_NAME);
            String taskParameterName = implementationConstants.getStringValue("TaskName", DEFAULT_PARAMETER_TASK_NAME);

            loadReportData(taskParameterName, taskName, report_parameters, parameterColumn, valueColumn);
            
        } catch (OException e) {
        	Logger.log(LogLevel.ERROR, LogCategory.General, this.getClass(),"constant repository problem",e);
            throw new ReportRunnerParameters("constant repository problem:CAUSE>" + e.getLocalizedMessage(), e);
        }

    }

    public ReportParameters(final Session session, Map<String, String> arguments) {
    	 this.session = Application.getInstance().getCurrentSession();
        parameters = arguments;
       // return this.session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see reportrunner.parameters.IReportParameters#hasParameter
     */
    @Override
    public final boolean hasParameter(final String parameter) {

        return parameters.containsKey(parameter);
    }

    /*
     * (non-Javadoc)
     * 
     * @see reportrunner.parameters.IReportParameters#getParameterValue
     */
    @Override
    public final String getParameterValue(final String parameter) {

        return (String) parameters.get(parameter);
    }

    /*
     * (non-Javadoc)
     * 
     * @see reportrunner.parameters.IReportParameters#getCustomParameters
     */
    @Override
    public IReportParameters getCustomParameters(List<String> requiredParameters) {

        Map<String, String> custom = new HashMap<>(0);

        for (Entry<String, String> entry : parameters.entrySet()) {

            if (!requiredParameters.contains(entry.getKey().toLowerCase())){
                custom.put(entry.getKey(), entry.getValue());
            }
        }

        return new ReportParameters(session, custom);
    }

    /**
     * Load the report data from the user table and store in the parameter map.
     * 
     * @param taskName used as a lookup in the SQL
     * @param valueColumn
     * @param parameterColumn
     * @param report_parameters
     */
    private void loadReportData(final String taskParameter, final String taskName, final String report_parameters, final String parameterColumn, final String valueColumn) {

        parameters = new HashMap<String, String>();

        String select = "SELECT * FROM " + report_parameters + " WHERE " + taskParameter + " = '" + taskName + "'";

        Table reportData = null;
        try {

            reportData = session.getIOFactory().runSQL(select);

            int numberOfRows = reportData.getRowCount();
            if (numberOfRows == 0) {
                throw new ReportRunnerParameters(String.format("%s for task %s", "No report data found", taskName));
            }

            for (int row = 0; row < numberOfRows; row++) {
                String parameterName = reportData.getString(parameterColumn, row);
                String parameterValue = reportData.getString(valueColumn, row);
                parameters.put(parameterName, parameterValue);
            }

        } catch (Exception ex) {
            throw new ReportRunnerParameters("Error loading report data from user table. " + ex.getMessage());

        } finally {
            if (reportData != null) {
                try {
                    reportData.dispose();

                } catch (Exception e) {
                	System.out.println("Exception thrown in finally block. " + e.getMessage());
                }
            }
        }
    }

}
