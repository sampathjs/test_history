/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.reports;

import java.util.Date;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Interface ReportParameters. Defines the parameters needed to run a report
 */
public interface ReportParameters {

	/**
	 * Gets the internal bu.
	 *
	 * @return the internal bu
	 */
	int getInternalBu();
	
	/**
	 * Gets the external bu.
	 *
	 * @return the external bu
	 */
	int getExternalBu();
	
	
	/**
	 * Gets the date to extract date for.
	 *
	 * @return the report date
	 */
	Date getReportDate();
}
