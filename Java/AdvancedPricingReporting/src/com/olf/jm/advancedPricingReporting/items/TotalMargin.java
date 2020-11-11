package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPriceShortSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class TotalMargin. Item to calculate the total margin, item is dependent on the following total items
 * 3% margin total, 6% margin total and deferred pricing short.
 */
public class TotalMargin extends ItemBase {

	/**
	 * Instantiates a new total margin.
	 *
	 * @param currentContext the current context
	 */
	public TotalMargin(Context currentContext) {
		super(currentContext);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		return new EnumColType[] {EnumFinalBalanceSection.TOTAL_MARGIN.getColumnType()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] {EnumFinalBalanceSection.TOTAL_MARGIN.getColumnName()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		super.addData(toPopulate, reportParameters);

		validateTableStructure(toPopulate);
		
		double totalMargin = 0.0;
		
		totalMargin += toPopulate.getDouble(EnumFinalBalanceSection.TIER_1_VALUE.getColumnName(), 0);
		totalMargin += toPopulate.getDouble(EnumFinalBalanceSection.TIER_2_VALUE.getColumnName(), 0);
		
		
		//totalMargin += toPopulate.getDouble(EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnName(), 0);
		
		Table deferredShort = toPopulate.getTable(EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnName(), 0);
		int columnId = deferredShort.getColumnId(EnumDeferredPriceShortSection.DEFERRED_SHORT.getColumnName());
		totalMargin += deferredShort.calcAsDouble(columnId, EnumColumnOperation.Sum);
		
		toPopulate.setDouble(EnumFinalBalanceSection.TOTAL_MARGIN.getColumnName(), 0, totalMargin);
	}
	
	/**
	 * Validate table structure.
	 *
	 * @param toPopulate the to populate
	 */
	private void validateTableStructure(Table toPopulate) {
		
		String columnName = toPopulate.getColumnNames();
		
		if(!columnName.contains(EnumFinalBalanceSection.TIER_1_VALUE.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.TIER_1_VALUE.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
		if(!columnName.contains(EnumFinalBalanceSection.TIER_2_VALUE.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.TIER_2_VALUE.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
		if(!columnName.contains(EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnName())) {
			String errorMessage = "Error validating table for  total balance calculation. Column  " + EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnName() + " is missing.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
 		}
	}
}
