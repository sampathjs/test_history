package com.olf.recon.rb.datasource.endurdealextract;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

/**
 * Base class for generating a trade listing style output table for reconciliation
 */
public abstract class AbstractEndurDealExtract
{
	/* Start and end date to filter invoices */
	protected int windowStartDate;
	protected int windowEndDate;
	protected int lastTradeDate;
	protected String region;
	
	
	public AbstractEndurDealExtract(int windowStartDate, int windowEndDate)throws OException{
		this.windowStartDate = windowStartDate;
		this.windowEndDate = windowEndDate;
		
	}
	
	public AbstractEndurDealExtract(int windowStartDate, int windowEndDate, String region,int lastTradeDate)throws OException{
		this.windowStartDate = windowStartDate;
		this.windowEndDate = windowEndDate;
		this.region=region;
		this.lastTradeDate = lastTradeDate;
	}
	
	public AbstractEndurDealExtract(int windowStartDate, int windowEndDate, int lastTradeDate) throws OException
	{
		this.windowStartDate = windowStartDate;
		this.windowEndDate = windowEndDate;
		this.lastTradeDate = lastTradeDate;

		Logging.info("Abstract Invoice Extract, window_start_date: " + OCalendar.formatDateInt(windowStartDate));
		Logging.info("Abstract Invoice Extract, window_end_date: " + OCalendar.formatDateInt(windowEndDate));
		Logging.info("Abstract Invoice Extract, current_date for session: " + OCalendar.formatDateInt(OCalendar.today()));
		Logging.info("Abstract Invoice Extract, business_date for session: " + OCalendar.formatDateInt(Util.getBusinessDate()));
		Logging.info("Abstract Invoice Extract, trading_date for session: " + OCalendar.formatDateInt(Util.getTradingDate()));
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
		return TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ", " + TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt()+ ", " + TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt();
	}
}
