/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDailySummarySection;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ApprovedTradeLimit. Item to set the trade limits on the daily custom summary report. 
 * Does not add any new columns to the report but adds additional rows.
 */
public class ApprovedTradeLimit  extends ItemBase {

	/** The Constant columnsWithLimits. Defines the columns which have limits*/
	private final static EnumDailySummarySection[] columnsWithLimits = new EnumDailySummarySection[] {
		EnumDailySummarySection.AG_AP_TOZ,
		EnumDailySummarySection.AU_AP_TOZ,
		EnumDailySummarySection.IR_AP_TOZ,
		EnumDailySummarySection.OS_AP_TOZ,
		EnumDailySummarySection.PD_AP_TOZ,
		EnumDailySummarySection.PT_AP_TOZ,
		EnumDailySummarySection.RH_AP_TOZ,
		EnumDailySummarySection.RU_AP_TOZ,	
		
		EnumDailySummarySection.AG_DP_TOZ,
		EnumDailySummarySection.AU_DP_TOZ,
		EnumDailySummarySection.IR_DP_TOZ,
		EnumDailySummarySection.OS_DP_TOZ,
		EnumDailySummarySection.PD_DP_TOZ,
		EnumDailySummarySection.PT_DP_TOZ,
		EnumDailySummarySection.RH_DP_TOZ,
		EnumDailySummarySection.RU_DP_TOZ};

	
	/**
	 * Instantiates a new approved trade limit.
	 *
	 * @param context the script context
	 */
	public ApprovedTradeLimit(Context context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		return null;

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return null;

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {

		int newRow = toPopulate.addRows(1);
		toPopulate.setString(
				EnumDailySummarySection.CUSTOMER_NAME.getColumnName(), newRow,
				"Approved trade limit");

		try (Table tradeLimits = loadTradeLimits()) {
			for (EnumDailySummarySection column : columnsWithLimits) {

				String metal = column.toString().substring(0, 2);
				String pricing_type = column.toString().substring(3, 5);

				ConstTable tradeLimit = tradeLimits.createView("*",
						"[metal] == 'X" + metal + "' AND [pricing_type] == '"
								+ pricing_type + "'");

				if (tradeLimit == null || tradeLimit.getRowCount() != 1) {
					String errorMessage = "Skipping trade limit for metal "
							+ metal + " pricing type " + pricing_type + " no date in user table or invalid data.";
					Logging.info(errorMessage);
				} else {
					toPopulate.setDouble(column.getColumnName(), newRow,
						tradeLimit.getDouble("trade_limit_toz", 0));
				}
			}
		}
	}

	/**
	 * Load trade limits.
	 *
	 * @return the table
	 */
	private Table loadTradeLimits() {
		return  runSQL("select * from USER_jm_ap_dp_trd_limit");
	}
	
	public void removeNonReportedMetals(Table data) {
		try (Table tradeLimits = loadTradeLimits()) {
			for (EnumDailySummarySection column : columnsWithLimits) {

				String metal = column.toString().substring(0, 2);
				String pricing_type = column.toString().substring(3, 5);

				ConstTable tradeLimit = tradeLimits.createView("*",
						"[metal] == 'X" + metal + "' AND [pricing_type] == '"
								+ pricing_type + "'");

				if (tradeLimit == null || tradeLimit.getRowCount() == 0) {
					Logging.info("Removing comuln " + column.getColumnName() + " no data in user table USER_jm_ap_dp_trd_limit");
					data.removeColumn(column.getColumnName());	
				}
			}

		}
	}

}
