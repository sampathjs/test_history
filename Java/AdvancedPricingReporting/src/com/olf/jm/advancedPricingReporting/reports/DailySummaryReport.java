/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.reports;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.sections.ApDpDailySummarySection;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

public class DailySummaryReport extends ReportBase {

	/**
	 * Instantiates a new daily summary report.
	 * 
	 * @param currentContext
	 *            the script context
	 */
	public DailySummaryReport(Context currentContext) {
		super(currentContext);

	}

	/**
	 * Builds the report sections that make up the report.
	 */
	protected void buildReportSections() {

		reportSections.add(new ApDpDailySummarySection(context));

	}

	@Override
	protected String getReportName() {
		return "ApDpDailySummaryReport";
	}

}
