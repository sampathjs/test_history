/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.items;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDailySummarySection;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.calendar.EnumDateFormat;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class EndCashBalanceDailySummary. Populates the cash balance details on the daily customer summary report. Balance is read from the 
 * user_jm_ap_dp_balance user table. The AP DP report needs to have been run to populate the balance information.
 */
public class EndCashBalanceDailySummary  extends ItemBase {

	/**
	 * Instantiates a new end cash balance daily summary.
	 *
	 * @param context the context
	 */
	public EndCashBalanceDailySummary(Context context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		return new EnumColType[] {EnumDailySummarySection.END_CASH_BALANCE.getColumnType()};

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] {EnumDailySummarySection.END_CASH_BALANCE.getColumnName()};

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		Table balanceData = getBalanceInfo(reportParameters.getReportDate());
		
		toPopulate.select(balanceData, EnumDailySummarySection.END_CASH_BALANCE.getColumnName(),
				 					 "[IN." + EnumDailySummarySection.CUSTOMER_ID.getColumnName() + "]  == [OUT.customer_id]");
		
		
	}

	/**
	 * Gets the balance info.
	 *
	 * @param reportDate the report date
	 * @return the balance info
	 */
	private Table getBalanceInfo(Date reportDate) {
		StringBuilder sql = new StringBuilder();

		String matchDateString = context.getCalendarFactory()
				.getDateDisplayString(reportDate, EnumDateFormat.DlmlyDash);

		sql.append(" SELECT customer_id,   \n");
		sql.append("    todaysdollar_balance  as ")
				.append(EnumDailySummarySection.END_CASH_BALANCE.getColumnName())
				.append("  \n");
		sql.append(" FROM   user_jm_ap_dp_balance   \n");
		sql.append(" WHERE  open_date = '").append(matchDateString)
				.append("'  \n");

		return runSQL(sql.toString());

	}
		

}
