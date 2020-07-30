package com.olf.jm.cancellationvalidator;

import com.olf.embedded.application.Context;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.openlink.util.logging.PluginLog;

/**
 * Toolset Interface declares methods which checks cancellation criteria of
 * deals.
 * 
 * @author
 * 
 */
public abstract class AbstractValidator {
	/**
	 * Checks the cancellation criteria of the trade . The criteria can vary for
	 * different Toolset and Instruments.
	 * 
	 * @throws OException
	 */
	protected final Transaction tran;
	protected String errorMessage;
	protected final Context context;
	protected boolean allowOverride;

	// protected final Utility util;

	public AbstractValidator(Transaction tran, String message, Context context) {
		this.tran = tran;
		this.errorMessage = message;
		this.context = context;
		// this.util = new Utility(context) ;

	}

	protected int getDealTradeDate() throws OException {
		int jdTradeDate;
		try {
			String tradeDate = tran.getField(EnumTransactionFieldId.TradeDate).getValueAsString();
			PluginLog.info("Trade Date for deal number " + tran.getDealTrackingId() + " is " + tradeDate);
			jdTradeDate = OCalendar.parseString(tradeDate);
		} catch (OException exp) {
			PluginLog.error("There was an error retrieving Trade date form Transaction for deal " + tran.getDealTrackingId() + "\n" + exp.getMessage());
			throw new OException(exp.getMessage());
		}
		return jdTradeDate;
	}

	protected int getCurrentTradingDate() throws OException {
		int jdCurrentTradingDate;
		try {
			String today = context.getTradingDate().toString();
			PluginLog.info("Current Trading Date " + today);
			jdCurrentTradingDate = OCalendar.parseString(today);
		} catch (OException exp) {
			PluginLog.error("There was an error retrieving Today's trading date while processing  " + tran.getDealTrackingId() + "\n" + exp.getMessage());
			throw new OException(exp.getMessage());
		}
		return jdCurrentTradingDate;
	}

	protected String getErrorMessage() {
		return errorMessage;
	}
	
	protected boolean isAllowOverride(){
		return allowOverride;
	}

	/**
	 * Helper method to run sql statements..
	 * 
	 * @param sql
	 *            the sql to execute
	 * @return the table containing the sql output
	 */
	protected Table runSql(final String sql) {

		IOFactory iof = context.getIOFactory();

		PluginLog.debug("About to run SQL. \n" + sql);

		Table t = null;
		try {
			t = iof.runSQL(sql);
		} catch (Exception e) {
			String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}

		return t;

	}

	protected int monthDiff(int startDate, int endDate) throws OException {
		int yearDiff = OCalendar.getYear(endDate) - OCalendar.getYear(startDate);
		int monthDiff = OCalendar.getMonth(endDate) - OCalendar.getMonth(startDate);
		return yearDiff * 12 + monthDiff;
	}
	
	protected boolean isSameMonth(int firstDate, int secondDate) throws OException {
		boolean flag = false;

		int firstDateSOM = OCalendar.getSOM(firstDate);
		int secondDateSOM = OCalendar.getSOM(secondDate);
		if (firstDateSOM == secondDateSOM) {
			flag = true;
		}
		return flag;

	}

	protected boolean isFutureMonth(int firstDate, int secondDate) throws OException {
		boolean flag = false;
		
		int firstDateSOM = OCalendar.getSOM(firstDate);
		int secondDateSOM = OCalendar.getSOM(secondDate);
		if (firstDateSOM > secondDateSOM) {
			flag = true;
		}

		return flag;
	}
	
	protected boolean isPastMonth(int firstDate, int secondDate) throws OException {
		boolean flag = false;
		
		int firstDateSOM = OCalendar.getSOM(firstDate);
		int secondDateSOM = OCalendar.getSOM(secondDate);
		if (firstDateSOM < secondDateSOM) {
			flag = true;
		}

		return flag;
	}


	//protected abstract boolean isAllowOverride();

	protected abstract boolean isCancellationAllowed() throws OException;
}
