package com.olf.jm.advancedPricingReporting.reports;

import com.olf.openrisk.table.ConstTable;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Interface Report.
 */
public interface Report {

	/**
	 * Generate the report.
	 *
	 * @param reportParameters the report parameters
	 */
	void generateReport( ReportParameters reportParameters );
	
	/**
	 * Gets the report output data.
	 *
	 * @return the report data
	 */
	ConstTable getReportData();
}
