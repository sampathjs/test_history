package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDailySummarySection;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */


/**
 * The Class JmHkDailySummaryTotals. Item to sum columns, does not add any new columns, adds a new row for the totals.
 */
public class JmHkDailySummaryTotals  extends ItemBase {

	/** The Constant columnsToSum. */
	private final static EnumDailySummarySection columnsToSum[] = new EnumDailySummarySection[] {
			EnumDailySummarySection.AU_AP_TOZ,
			EnumDailySummarySection.PT_AP_TOZ,
			EnumDailySummarySection.PD_AP_TOZ,
			EnumDailySummarySection.RH_AP_TOZ,
			EnumDailySummarySection.AU_DP_TOZ,
			EnumDailySummarySection.PT_DP_TOZ,
			EnumDailySummarySection.PD_DP_TOZ,
			EnumDailySummarySection.RH_DP_TOZ,
			EnumDailySummarySection.AU_DP_SETTLE_VALUE,
			EnumDailySummarySection.PT_DP_SETTLE_VALUE,
			EnumDailySummarySection.PD_DP_SETTLE_VALUE,		
			EnumDailySummarySection.IR_DP_SETTLE_VALUE,		
	};
	
	/**
	 * Instantiates a new jm hk daily summary totals.
	 *
	 * @param context the context
	 */
	public JmHkDailySummaryTotals(Context context) {
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
		toPopulate.setString(EnumDailySummarySection.CUSTOMER_NAME.getColumnName(), newRow, "JM HK Total");
		for(EnumDailySummarySection columnToSum : columnsToSum) {
			Double total = sumColumn(columnToSum, toPopulate);
			
			toPopulate.setDouble(columnToSum.getColumnName(), newRow, total);
		}	
	}

	/**
	 * Sum column.
	 *
	 * @param column the column
	 * @param toPopulate the to populate
	 * @return the double
	 */
	private double sumColumn(EnumDailySummarySection column, Table toPopulate) {
		int columnId = toPopulate.getColumnId(column.getColumnName());
		
		double total = toPopulate.calcAsDouble(columnId, EnumColumnOperation.Sum);

		return total;
	}

}
