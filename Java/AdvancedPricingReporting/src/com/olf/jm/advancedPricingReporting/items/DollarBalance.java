package com.olf.jm.advancedPricingReporting.items;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumUserJmApDpBalances;
import com.olf.jm.advancedPricingReporting.items.tables.TableColumnHelper;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.calendar.EnumDateFormat;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class DollarBalance. Read the balance from the user table USER_jm_ap_dp_balance for the reporting data. 
 * If there is not an entry for the current date copy the total balance from the previous row and create a new entry for the 
 * reporting date. 
 */
public class DollarBalance extends ItemBase {

	/** The Constant STATUS_COL_NAME. */
	private static final String STATUS_COL_NAME = "status";
	
	/**
	 * Instantiates a new dollar balance item.
	 *
	 * @param currentContext the current context
	 */
	public DollarBalance(Context currentContext) {
		super(currentContext);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		EnumColType[] columnTypes = new EnumColType[] {EnumFinalBalanceSection.DOLLAR_BALANCE.getColumnType()};
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] columns = new String[] {EnumFinalBalanceSection.DOLLAR_BALANCE.getColumnName()};
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
		
		double dollarBalance = calculateDollarBalance(reportDate, externalBu);
		
		toPopulate.setDouble(EnumFinalBalanceSection.DOLLAR_BALANCE.getColumnName(), 0, dollarBalance);
	}

	/**
	 * Calculate dollar balance reading the date from the user table USER_jm_ap_dp_balance.
	 *
	 * @param reportDate the report date
	 * @param externalBu the external bu
	 * @return the dollar balance
	 */
	private double calculateDollarBalance(Date reportDate, int externalBu) {

		
		double dollarBalance = 0;
		
		Table balanceInfo = getBalanceInfo(externalBu, reportDate);
		
		if(balanceInfo == null) {
			String errorMessage = "Error loading the balance info from table USER_jm_ap_dp_balance. Invalid table returned.";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		switch(balanceInfo.getRowCount()) {
			case 0:
				dollarBalance = createNewEntry(externalBu, reportDate);
			break;
			
			case 1:
				dollarBalance = processSingleRow(balanceInfo, reportDate);
				break;

			case 2:
				dollarBalance = processTwoRows(balanceInfo, reportDate);
				break;				
			default:	
				String errorMessage = "Error loading the balance info from table USER_jm_ap_dp_balance. "
						+ " Invalid table returned, expecting 1 or 2 rows but found " +balanceInfo.getRowCount() + " rows.";
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);		
		}
		
		return dollarBalance;
	}	




	/**
	 * Process the data from the user table where 2 rows have been found, the previous row and the current row. 
	 * Table the value from the current row. 
	 *
	 * @param balanceInfo the balance info read from the table USER_jm_ap_dp_balance
	 * @param reportDate the report date
	 * @return the dollar balance
	 */
	private double processTwoRows(Table balanceInfo, Date reportDate) {

		ConstTable currentRow = balanceInfo.createConstView("*", "[" + STATUS_COL_NAME + "] == 'current'");
		
		if(currentRow == null  || currentRow.getRowCount()  != 1) {
			String errorMessage = "Error loading the balance info from table USER_jm_ap_dp_balance. "
					+ " Invalid table returned, unable to find a current row for the reporting date " + reportDate;
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);		
		}
		
		return currentRow.getDouble(EnumUserJmApDpBalances.DOLLAR_BALANCE.getColumnName(), 0);
	}

	/**
	 * Process the data from the table USER_jm_ap_dp_balance when a single row is detected. If row is current read the 
	 * value directly. If the row is for the previous reporting period create a new entry for the current reporting date.
	 *
	 * @param balanceInfo the balance info read from the table USER_jm_ap_dp_balance
	 * @param reportDate the report date
	 * @return the dollar balance
	 */
	private double processSingleRow(Table balanceInfo, Date reportDate) {
		String rowStatus = balanceInfo.getString(STATUS_COL_NAME, 0);
		
		// single row for the current reporting date
		if(rowStatus.equalsIgnoreCase("current")) {
			return balanceInfo.getDouble(EnumUserJmApDpBalances.DOLLAR_BALANCE.getColumnName(), 0);
		}
		
		// Row must be for the previous reporting period, create new entry for today.
		double dolarBalance = createNewBalanceEntry(balanceInfo, reportDate);
		
		return dolarBalance;
	}
	

	

	/**
	 * Creates the new balance entry for the current reporting date.
	 *
	 * @param balanceInfo the balance info from the table USER_jm_ap_dp_balance
	 * @param reportDate the report date
	 * @return the double
	 */
	private double createNewBalanceEntry(Table balanceInfo, Date reportDate) {
		double dollarBalance = balanceInfo.getDouble(EnumUserJmApDpBalances.TOTAL_BALANCE.getColumnName(), 0);
		TableColumnHelper<EnumUserJmApDpBalances> tableHelper = new TableColumnHelper<EnumUserJmApDpBalances>();
		
		
		Table insertData = tableHelper.buildTable(context, EnumUserJmApDpBalances.class);
		
		insertData.addRows(1);
		insertData.setRowValues(0, new Object[] 
				{balanceInfo.getInt(EnumUserJmApDpBalances.CUSTOMER_ID.getColumnName(), 0),
				 reportDate,
				 balanceInfo.getDate(EnumUserJmApDpBalances.OPEN_DATE.getColumnName(),  0),
				 dollarBalance,
				 0.0});
		
		UserTable userTable = context.getIOFactory().getUserTable("USER_jm_ap_dp_balance");
		
		userTable.insertRows(insertData);
		
		return dollarBalance;
	}
	
	/**
	 * Creates the new entry in the user table for new customers.
	 *
	 * @param externalBu the external bu to create the entry for
	 * @param reportDate the report date to use when populating the table
	 * @return the 0 
	 */
	private double createNewEntry(int externalBu, Date reportDate) {
		TableColumnHelper<EnumUserJmApDpBalances> tableHelper = new TableColumnHelper<EnumUserJmApDpBalances>();
		
		SymbolicDate lastGoodBusinessDay = context.getCalendarFactory().createSymbolicDate("-1cd");
		Table insertData = tableHelper.buildTable(context, EnumUserJmApDpBalances.class);
		
		insertData.addRows(1);
		insertData.setRowValues(0, new Object[] 
				{externalBu,
				 reportDate,
				 lastGoodBusinessDay.evaluate(reportDate),
				 0.0,
				 0.0});
		
		UserTable userTable = context.getIOFactory().getUserTable("USER_jm_ap_dp_balance");
		
		userTable.insertRows(insertData);
		
		return 0;
	}

	/**
	 * Gets the balance info from the table USER_jm_ap_dp_balance for the selected BU.
	 *
	 * @param externalBu the external bu
	 * @param reportDate the report date
	 * @return the balance info
	 */
	private Table getBalanceInfo(int externalBu, Date reportDate) {
		StringBuffer sql = new StringBuffer();
		
		String matchDateString = context.getCalendarFactory().getDateDisplayString(reportDate, EnumDateFormat.DlmlyDash);
		
		sql.append(" SELECT *,  \n");
		sql.append("        'current' AS ").append(STATUS_COL_NAME).append("  \n");
		sql.append(" FROM   user_jm_ap_dp_balance  \n");
		sql.append(" WHERE  customer_id = ").append(externalBu).append("  \n");
		sql.append("        AND open_date = '").append(matchDateString).append("'  \n");
		sql.append(" UNION  \n");
		sql.append(" SELECT *,  \n");
		sql.append("        'previous' AS ").append(STATUS_COL_NAME).append("  \n");
		sql.append(" FROM   user_jm_ap_dp_balance  \n");
		sql.append(" WHERE  customer_id = ").append(externalBu).append("  \n");
		sql.append("        AND open_date = (SELECT Max(open_date)  \n");
		sql.append("                         FROM   user_jm_ap_dp_balance  \n");
		sql.append("                         WHERE  customer_id = ").append(externalBu).append("  \n");
		sql.append("                         AND open_date < '").append(matchDateString).append("') \n");	

		
		return runSQL(sql.toString());
	}
	
}
