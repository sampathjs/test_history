package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDailySummarySection;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */


/**
 * The Class DailySummaryCustomers. Item to set the customer details on the daily summary report. The customers are pulled from
 * the user table user_jm_ap_dp_margin_percn. If the customer has a margin % recored it will be reported.
 */
public class DailySummaryCustomers extends ItemBase {

	/**
	 * Instantiates a new daily summary customers.
	 *
	 * @param context the context
	 */
	public DailySummaryCustomers(Context context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {

		return new EnumColType[] {EnumDailySummarySection.CUSTOMER_NAME.getColumnType(), EnumDailySummarySection.CUSTOMER_ID.getColumnType()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] {EnumDailySummarySection.CUSTOMER_NAME.getColumnName(), EnumDailySummarySection.CUSTOMER_ID.getColumnName()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {

		Table partyData = loadPartyData();
		
		toPopulate.select(partyData, EnumDailySummarySection.CUSTOMER_ID.getColumnName() + 
				 					 ", " + EnumDailySummarySection.CUSTOMER_NAME.getColumnName(), 
				 					 "[IN." + EnumDailySummarySection.CUSTOMER_ID.getColumnName() + "] > 0");
		
	}

	/**
	 * Load party data.
	 *
	 * @return the table
	 */
	private Table loadPartyData() {
		StringBuffer sql = new StringBuffer();
		

		sql.append(" SELECT DISTINCT customer_id as " +EnumDailySummarySection.CUSTOMER_ID.getColumnName() + ", ");
		sql.append("         short_name as " +  EnumDailySummarySection.CUSTOMER_NAME.getColumnName());
		sql.append(" FROM   user_jm_ap_dp_margin_percn ");
		sql.append(" JOIN party ");
		sql.append("  ON customer_id = party_id ");
		
		return runSQL(sql.toString());
		
	}

}
