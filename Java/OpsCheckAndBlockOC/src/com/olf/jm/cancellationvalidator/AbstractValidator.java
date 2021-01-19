package com.olf.jm.cancellationvalidator;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.jm.logging.Logging;

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
			  
			Date dtTradeDate = tran.getField(EnumTransactionFieldId.TradeDate).getValueAsDate();
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy");
			String strTradeDate = formatter.format(dtTradeDate);
			
			Logging.info("Trade Date for deal number " + tran.getDealTrackingId() + " is " + strTradeDate);
			jdTradeDate = OCalendar.parseString(strTradeDate);
		} catch (OException exp) {
			Logging.error("There was an error retrieving Trade date form Transaction for deal " + tran.getDealTrackingId() + "\n" + exp.getMessage());
			throw new OException(exp.getMessage());
		}
		return jdTradeDate;
	}

	protected int getCurrentTradingDate() throws OException {
		int jdCurrentTradingDate;
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy");
		    String today = formatter.format(context.getTradingDate());  
			Logging.info("Current Trading Date " + today);
			jdCurrentTradingDate = OCalendar.parseString(today);
		} catch (OException exp) {
			Logging.error("There was an error retrieving Today's trading date while processing  " + tran.getDealTrackingId() + "\n" + exp.getMessage());
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

		Logging.debug("About to run SQL. \n" + sql);

		Table t = null;
		try {
			t = iof.runSQL(sql);
		} catch (Exception e) {
			String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}

		return t;

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
