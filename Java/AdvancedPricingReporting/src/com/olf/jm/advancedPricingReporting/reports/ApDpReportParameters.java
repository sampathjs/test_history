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
 * The Class ApReportParameters. Parameters needed by the advanced / deferred pricing report
 */
public class ApDpReportParameters implements ReportParameters {
	
	/** The internal bu. */
	private final int internalBU;
	
	/** The external bu. */
	private final int externalBU;
	
	/** The date to extract data for */
	private final Date reportDate;
	
	/**
	 * Instantiates a new ap report parameters.
	 *
	 * @param internalBU the internal bu
	 * @param externalBU the external bu
	 * @param reportDate the report date
	 */
	public ApDpReportParameters(int internalBU, int externalBU, Date reportDate) {
		super();
		this.internalBU = internalBU;
		this.externalBU = externalBU;
		this.reportDate = reportDate;
	}



	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.reports.ReportParameters#getInternalBu()
	 */
	@Override
	public int getInternalBu() {
		return internalBU;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.reports.ReportParameters#getExternalBu()
	 */
	@Override
	public int getExternalBu() {
		return externalBU;
	}



	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.reports.ReportParameters#getReportDate()
	 */
	@Override
	public Date getReportDate() {
		return reportDate;
	}

}
