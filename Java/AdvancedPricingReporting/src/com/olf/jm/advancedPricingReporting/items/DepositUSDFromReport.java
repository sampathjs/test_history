package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;


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
		return new EnumColType[] {EnumFinalBalanceSection.DEPOSIT_USD.getColumnType()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] {EnumFinalBalanceSection.DEPOSIT_USD.getColumnName()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		super.addData(toPopulate, reportParameters);

		int externalBu = reportParameters.getExternalBu();
		
		double dollarBalance = calculateDepositUSD(externalBu);
		
		toPopulate.setDouble(EnumFinalBalanceSection.DEPOSIT_USD.getColumnName(), 0, dollarBalance);
	}

	/**
	 * Calculate usd deposit amount.
	 *
	 * @param externalBu the external bu
	 * @return the double
	 */
	private double calculateDepositUSD(int externalBu) {
		Table accountToProcess = getUsdAccountForBu(externalBu);
		
		double runningTotal = 0;
		for(int row = 0; row < accountToProcess.getRowCount(); row++) {
			runningTotal += runAccountBalanceReport();
		}
		
		return runningTotal;
	}	
	
	
	/**
	 * Run account balance report.
	 */
	private double runAccountBalanceReport() {
		return 0.0;
	}

	/**
	 * get the account names that contain the dollar balance.
	 *
	 * @param externalBu the external bu
	 * @return table containing the account to use when calculating the balance
	 */
	private Table getUsdAccountForBu(int externalBu) {
		String sql = " SELECT party_id, \n" +
					 "        pa.account_id, \n" +
					 "        account_number, \n" +
					 "        account_name \n" +
					 " FROM   party_account pa \n" +
					 "        JOIN account_view acc \n" +
					 "          ON acc.account_id = pa.account_id\n" +
					 "             AND currency_id = 0 \n" +
					 " WHERE  party_id = " +
					 externalBu +
					 "\n";
		return runSQL(sql);
	}
	
}
