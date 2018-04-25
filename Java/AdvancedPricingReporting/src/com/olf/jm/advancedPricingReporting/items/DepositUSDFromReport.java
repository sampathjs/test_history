package com.olf.jm.advancedPricingReporting.items;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class DepositUSDFromReport. Calculate the USD deposit amount by running the account balance report. 
 */
public class DepositUSDFromReport extends ItemBase {

	/**
	 * Instantiates a new deposit usd from report item.
	 *
	 * @param currentContext the current context
	 */
	public DepositUSDFromReport(Context currentContext) {
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
	 * Calculate usd deposit amount.
	 *
	 * @param reportDate the report date
	 * @param externalBu the external bu
	 * @return the double
	 */
	private double calculateDepositUSD(Date reportDate, int externalBu) {
		Table accountToProcess = getUsdAccountForBu(externalBu);
		
		double runningTotal = 0;
		for(int row = 0; row < accountToProcess.getRowCount(); row++) {
			String accountNumber = accountToProcess.getString("account_number", row);
			runningTotal += runAccountBalanceReport( reportDate, accountNumber);
		}
		
		return runningTotal;
	}	
	
	
	/**
	 * Run account balance report.
	 *
	 * @param reportDate the report date
	 * @param accountNumber the account number
	 * @return the double
	 */
	private double runAccountBalanceReport(Date reportDate, String accountNumber) {
		
		PluginLog.info("Abount to run account balance report for account " + accountNumber  + " report date " + reportDate);
		
		SimpleDateFormat sdf = new SimpleDateFormat ("dd-MMM-yyyy");
		Map<String, String> params = new HashMap<String, String>();
		params.put("report_name", "JM (Vostro) Account Balance By Account");
		params.put("ReportAccount", accountNumber);
		params.put("ReportDate", sdf.format(reportDate));
				
		double balance = 0.0;
		/* Removed as no longer needed, if required D989_Reporting project must be referenced
		IReportParameters newParameters = new com.matthey.openlink.reporting.runner.parameters.ReportParameters(context, params);
		GenerateAndOverrideParameters balances = new GenerateAndOverrideParameters(context, newParameters);		
		
		if (balances.generate()) {
			Table output = balances.getResults();
			if (null != output && output.getRowCount() > 0) {
				int colId = output.getColumnId("position");
				balance = output.calcAsDouble(colId, EnumColumnOperation.Sum);
			}
		}
		*/
		return balance;
	}

	/**
	 * get the account names that contain the dollar balance.
	 *
	 * @param externalBu the external bu
	 * @return table containing the account to use when calculating the balance
	 */
	private Table getUsdAccountForBu(int externalBu) {
		StringBuffer sql = new StringBuffer();
		
		sql.append(" SELECT party_id, \n");
		sql.append("        pa.account_id, \n");
		sql.append("        account_number, \n");
		sql.append("        account_name \n");
		sql.append(" FROM   party_account pa \n");
		sql.append("        JOIN account_view acc \n");
		sql.append("          ON acc.account_id = pa.account_id\n"); 
		sql.append("             AND currency_id = 0 \n");
		sql.append(" WHERE  party_id = ").append(externalBu).append("\n");
		
		return runSQL(sql.toString());
	}
	
}
