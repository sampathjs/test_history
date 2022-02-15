package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class Difference. Calculate the over all profit / loss for the report. Item is dependent on the fields total balance 
 * and total margin. 
 */
public class Difference extends ItemBase {

	/**
	 * Instantiates a new difference item.
	 *
	 * @param currentContext the current context
	 */
	public Difference(Context currentContext) {
		super(currentContext);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		EnumColType[] columnTypes = new EnumColType[] {EnumFinalBalanceSection.DIFFERENCE.getColumnType()};
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] columns = new String[] {EnumFinalBalanceSection.DIFFERENCE.getColumnName()};
		return columns;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		super.addData(toPopulate, reportParameters);

		validateTableStructure(toPopulate);
		
		double difference = 0.0;
		
		difference += toPopulate.getDouble(EnumFinalBalanceSection.TODAYS_BALANCE.getColumnName(), 0);
		difference += toPopulate.getDouble(EnumFinalBalanceSection.TOTAL_MARGIN.getColumnName(), 0);

		
		toPopulate.setDouble(EnumFinalBalanceSection.DIFFERENCE.getColumnName(), 0, difference);
	}

	/**
	 * Validate table structure.
	 *
	 * @param toPopulate the table containing section data
	 */
	private void validateTableStructure(Table toPopulate) {
		
		String columnName = toPopulate.getColumnNames();
		
		if(!columnName.contains(EnumFinalBalanceSection.TODAYS_BALANCE.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.TIER_1_VALUE.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
		if(!columnName.contains(EnumFinalBalanceSection.TOTAL_MARGIN.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.TIER_2_VALUE.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
	}
}
