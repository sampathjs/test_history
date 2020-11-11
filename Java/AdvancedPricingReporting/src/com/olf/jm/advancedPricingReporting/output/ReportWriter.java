package com.olf.jm.advancedPricingReporting.output;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Interface ReportWriter. Interface to report writer classes used to output the report data
 */
public interface ReportWriter {

	/**
	 * Generate report. Output the report in the the required format. 
	 *
	 * @param xml the data to output in XML form.
	 */
	void generateReport(String xml);
}
