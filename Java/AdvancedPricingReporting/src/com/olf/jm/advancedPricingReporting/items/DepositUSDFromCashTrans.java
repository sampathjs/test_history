package com.olf.jm.advancedPricingReporting.items;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.calendar.EnumDateFormat;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class DepositUSDFromCashTrans. Item to set the deposit USD item. Value is read from a cash tran with a cashflow 
 * type of AP/DP Margin Payment and a trade date matching the reporting date. 
 */
public class DepositUSDFromCashTrans extends ItemBase {

	/**
	 * Instantiates a new deposit usd from cash trans.
	 *
	 * @param currentContext the current context
	 */
	public DepositUSDFromCashTrans(Context currentContext) {
		super(currentContext);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		EnumColType[] columnTypes = new EnumColType[] {EnumFinalBalanceSection.DEPOSIT_USD.getColumnType()};
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] columns = new String[] {EnumFinalBalanceSection.DEPOSIT_USD.getColumnName()};
		return columns;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		super.addData(toPopulate, reportParameters);

		Date reportDate = reportParameters.getReportDate();
		int externalBu = reportParameters.getExternalBu();
		
		double dollarBalance = calculateDepositUSD(reportDate, externalBu);
		
		toPopulate.setDouble(EnumFinalBalanceSection.DEPOSIT_USD.getColumnName(), 0, dollarBalance);
	}

	/**
	 * Calculate deposit usd, sum together all cash transaction.
	 *
	 * @param reportDate the report date
	 * @param externalBu the external bu id
	 * @return the total deposit amount
	 */
	private double calculateDepositUSD(Date reportDate, int externalBu) {
		Table accountToProcess = getUsdAccountForBu(externalBu, reportDate);
		
		double runningTotal = 0;

		if(accountToProcess != null && accountToProcess.getRowCount() == 1) {
			runningTotal = accountToProcess.getDouble(0, 0);
		} else {
			Logging.warn("Invalid table returned for dollar deposit setting to 0.");
		}
		
		return runningTotal;
	}	
	
	

	/**
	 * Gets the total position for the selected BU.
	 *
	 * @param externalBu the external bu select
	 * @param reportDate the date to select trades for
	 * @return the total usd account
	 */
	private Table getUsdAccountForBu(int externalBu, Date reportDate) {
		StringBuffer sql = new StringBuffer();
	
		String reportDateString = context.getCalendarFactory().getDateDisplayString(reportDate, EnumDateFormat.DlmlyDash);
				
		sql.append(" SELECT Sum(position) AS balance  \n");
		sql.append(" FROM   ab_tran a  \n");
		sql.append("        JOIN cflow_type cft  \n");
		sql.append("          ON cft.id_number = a.cflow_type  \n");
		sql.append("            AND cft.NAME = 'AP/DP Margin Payment'  \n");
		sql.append(" WHERE  tran_status = 3  \n");
		sql.append("        AND current_flag = 1  \n");
		sql.append("        AND trade_date = '").append(reportDateString).append("'  \n");
		sql.append("        AND external_bunit = ").append(externalBu).append("  \n");
		       
		return runSQL(sql.toString());
	}

}
