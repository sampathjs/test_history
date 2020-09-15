package com.olf.jm.advancedPricingReporting.items;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPriceShortSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumSquaredMetalPositionSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumUserJmApDpBalances;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.calendar.EnumDateFormat;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class TodaysBalance. Item to calculate the total balance for today and write the result to the balance user table.
 * 
 *  Item is dependent on the following items 
 *  DOLLAR_BALANCE,DEPOSIT_USD,DEPOSIT_HKD,SQUATED_METAL_POSITION,COLLECTED_AP_METAL and COLLECTED_DP_METAL
 */
public class TodaysBalance extends ItemBase {

	/**
	 * Instantiates a new todays balance.
	 *
	 * @param currentContext the current context
	 */
	public TodaysBalance(Context currentContext) {
		super(currentContext);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		EnumColType[] columnTypes = new EnumColType[] {EnumFinalBalanceSection.TODAYS_BALANCE.getColumnType()};
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] columns = new String[] {EnumFinalBalanceSection.TODAYS_BALANCE.getColumnName()};
		return columns;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		super.addData(toPopulate, reportParameters);
		
		
		validateTableStructure(toPopulate);
		
		double totalBalance = 0.0;
		
		totalBalance += toPopulate.getDouble(EnumFinalBalanceSection.DOLLAR_BALANCE.getColumnName(), 0);
		totalBalance += toPopulate.getDouble(EnumFinalBalanceSection.DEPOSIT_USD.getColumnName(), 0);
		totalBalance += toPopulate.getDouble(EnumFinalBalanceSection.DEPOSIT_HKD.getColumnName(), 0);
		//totalBalance += toPopulate.getDouble(EnumFinalBalanceSection.SQUATED_METAL_POSITION.getColumnName(), 0);
		
		Table squaredMetalPos = toPopulate.getTable(EnumFinalBalanceSection.SQUARED_METAL_POSITION.getColumnName(), 0);
		int columnId = squaredMetalPos.getColumnId(EnumSquaredMetalPositionSection.SQUARED_METAL_VALUE.getColumnName());
		totalBalance  += squaredMetalPos.calcAsDouble(columnId, EnumColumnOperation.Sum);
		
		
		totalBalance += toPopulate.getDouble(EnumFinalBalanceSection.COLLECTED_AP_METAL.getColumnName(), 0);
		totalBalance += toPopulate.getDouble(EnumFinalBalanceSection.COLLECTED_DP_METAL.getColumnName(), 0);
		
		toPopulate.setDouble(EnumFinalBalanceSection.TODAYS_BALANCE.getColumnName(), 0, totalBalance);
		
		updateBalanceTable(reportParameters, totalBalance);
	}
	
	/**
	 * Update balance table.
	 *
	 * @param reportParameters the report parameters
	 * @param totalBalance the total balance
	 */
	private void updateBalanceTable(ReportParameters reportParameters, double totalBalance) {
		
		
		Table updateData = loadCurrentBalanceEntry(reportParameters);	
		
		if(updateData == null || updateData.getRowCount() != 1) {
			String errorMessage = "Error updateing the balance table with the current balance, unable to load user table.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		updateData.setDouble(EnumUserJmApDpBalances.TOTAL_BALANCE.getColumnName(), 0, totalBalance);

		UserTable userTable = context.getIOFactory().getUserTable("USER_jm_ap_dp_balance");
		
		userTable.updateRows(updateData, EnumUserJmApDpBalances.OPEN_DATE.getColumnName() + ", " + EnumUserJmApDpBalances.CUSTOMER_ID.getColumnName());

	}
	
	/**
	 * Load current balance entry.
	 *
	 * @param reportParameters the report parameters
	 * @return the table
	 */
	private Table loadCurrentBalanceEntry(ReportParameters reportParameters) {
		int externalBu = reportParameters.getExternalBu();
		Date reportDate = reportParameters.getReportDate();
		
		StringBuffer sql = new StringBuffer();
		
		String matchDateString = context.getCalendarFactory().getDateDisplayString(reportDate, EnumDateFormat.DlmlyDash);
		
		sql.append(" SELECT *  \n");
		sql.append(" FROM   user_jm_ap_dp_balance  \n");
		sql.append(" WHERE  customer_id = ").append(externalBu).append("  \n");
		sql.append("        AND open_date = '").append(matchDateString).append("'  \n");

		
		return runSQL(sql.toString());		
	}

	/**
	 * Validate table structure.
	 *
	 * @param toPopulate the to populate
	 */
	private void validateTableStructure(Table toPopulate) {
		
		String columnName = toPopulate.getColumnNames();
		
		if(!columnName.contains(EnumFinalBalanceSection.DOLLAR_BALANCE.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.DOLLAR_BALANCE.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
		if(!columnName.contains(EnumFinalBalanceSection.DEPOSIT_USD.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.DEPOSIT_USD.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
		if(!columnName.contains(EnumFinalBalanceSection.DEPOSIT_HKD.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.DEPOSIT_HKD.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
		if(!columnName.contains(EnumFinalBalanceSection.SQUARED_METAL_POSITION.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.SQUARED_METAL_POSITION.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
		if(!columnName.contains(EnumFinalBalanceSection.COLLECTED_AP_METAL.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.COLLECTED_AP_METAL.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
		if(!columnName.contains(EnumFinalBalanceSection.COLLECTED_DP_METAL.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.COLLECTED_DP_METAL.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
	}
	

}
