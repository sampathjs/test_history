package com.matthey.openlink.reporting.runner.generators;



/** Interface for report generator classes. 
 *
 */
public interface IReportGenerator {

	/** Generate the report.
	 * 
	 * @return true on success false otherwise
	 */
	boolean generate();
	
}
