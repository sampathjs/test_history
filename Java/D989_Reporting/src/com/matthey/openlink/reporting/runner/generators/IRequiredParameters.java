package com.matthey.openlink.reporting.runner.generators;

import java.util.List;

/**
 * Define the ReporGenerator parameter requirements
 * 
 */
public interface IRequiredParameters extends IReportGenerator {

    String TYPE_PARAMETER = "report_type";
    String NAME_PARAMETER = "report_name";
    
    List<String> getRequiredParameters();
}
