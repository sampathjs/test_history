package com.matthey.openlink.reporting.runner.parameters;

import java.util.List;

/**
 * Interface for parameters needed to run a report.
 * 
 */
public interface IReportParameters extends IReportWithOutputParameter, IReportNoParameters {


    /**
     * Check is a parameter is defined.
     * 
     * @param parameter Name of parameter to check.
     * @return true is parameters is present false otherwise.
     */
    boolean hasParameter(final String parameter);

    /**
     * Return a parameter value.
     * 
     * @param parameter name of parameter to return value for
     * @return parameter value
     */
    String getParameterValue(final String parameter);

    IReportParameters getCustomParameters(List<String> list);
}