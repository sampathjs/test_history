package com.olf.recon.rb.datasource.endurdealextract;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

public abstract class AbstractEndurDealExtract
{
	/* Start and end date to filter invoices */
	protected int windowStartDate;
	protected int windowEndDate;
	
	public AbstractEndurDealExtract(int windowStartDate, int windowEndDate) throws OException
	{
		this.windowStartDate = windowStartDate;
		this.windowEndDate = windowEndDate;

		PluginLog.info("Abstract Invoice Extract, window_start_date: " + OCalendar.formatDateInt(windowStartDate));
		PluginLog.info("Abstract Invoice Extract, window_end_date: " + OCalendar.formatDateInt(windowEndDate));
		PluginLog.info("Abstract Invoice Extract, current_date for session: " + OCalendar.formatDateInt(OCalendar.today()));
		PluginLog.info("Abstract Invoice Extract, business_date for session: " + OCalendar.formatDateInt(Util.getBusinessDate()));
		PluginLog.info("Abstract Invoice Extract, trading_date for session: " + OCalendar.formatDateInt(Util.getTradingDate()));
	}

	/* These are to be implemented in sub classes to filter invoices accordingly */
	protected abstract Table getData() throws OException;
	
	/**
	 * Return the applicable tran statuses for all deals
	 * 
	 * @return CSV string seperated by int-tran-status
	 */
	protected String getApplicableTransactionStatusesForSQL()
	{
		return TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ", " + TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt();
	}
}
