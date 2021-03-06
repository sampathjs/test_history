package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPriceShortSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDispatchDealSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFxDealSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumSquaredMetalPositionSection;
import com.olf.jm.advancedPricingReporting.items.tables.TableColumnHelper;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.advancedPricingReporting.sections.ApBuySellFxDealSection;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class SquatedMetalPosition. Item to calculate the squared metal position. Item is dependent on data FX buy / sell
 * section
 */
public class SquaredMetalPosition extends ItemBase {

	/**
	 * Instantiates a new squated metal position.
	 *
	 * @param currentContext the current context
	 * @param report the report
	 */
	public SquaredMetalPosition(Context currentContext, Report report) {
		super(currentContext, report);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		EnumColType[] columnTypes = new EnumColType[] {EnumFinalBalanceSection.SQUARED_METAL_POSITION.getColumnType()};
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] columns = new String[] {EnumFinalBalanceSection.SQUARED_METAL_POSITION.getColumnName()};
		return columns;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		Logging.info("Calculating the squated_metal_position");
		super.addData(toPopulate, reportParameters);
		
		validateReportStructure();
		
		ConstTable reportData = report.getReportData();
		Table buySellFxDeals = reportData.getTable(ApBuySellFxDealSection.sectionName(), 0);
		
		if(buySellFxDeals == null ) {
			String errorMessage = "Error calculating the Squated PT/PD Position. The required section "
					+ ApBuySellFxDealSection.sectionName() + " is not valid";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		TableColumnHelper<EnumSquaredMetalPositionSection> tableHelper = new TableColumnHelper<EnumSquaredMetalPositionSection>();
		Table squaredMetalPos = tableHelper.buildTable(context, EnumSquaredMetalPositionSection.class, EnumFinalBalanceSection.SQUARED_METAL_POSITION.getColumnName());
	
		if(buySellFxDeals.getRowCount() > 0) {
			
			//Table dpShort = context.getTableFactory().createTable(EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnName());
			
			
			String what = EnumFxDealSection.METAL_SHORT_NAME.getColumnName() + ", " + EnumFxDealSection.LOSS_GAIN.getColumnName() + "->" + EnumSquaredMetalPositionSection.SQUARED_METAL_VALUE.getColumnName();
			String where = "[IN." + EnumFxDealSection.LOSS_GAIN.getColumnName() + "] != 0.0";
			
			squaredMetalPos.select(buySellFxDeals, what, where);
			
			
			squaredMetalPos.calcColumn(EnumSquaredMetalPositionSection.SQUARED_METAL_VALUE.getColumnName(), EnumSquaredMetalPositionSection.SQUARED_METAL_VALUE.getColumnName() + " * 1.0");
								
			//int columnId = dispatchDeals.getColumnId(EnumDeferredPricingSection.TOTAL_DP_VALUE.getColumnName());
			//double total = dispatchDeals.calcAsDouble(columnId, EnumColumnOperation.Sum);
		} 

		if(squaredMetalPos.getRowCount() == 0) {
			squaredMetalPos.addRows(1);
		}
		toPopulate.setTable(EnumFinalBalanceSection.SQUARED_METAL_POSITION.getColumnName(), 0, squaredMetalPos);					
		
	}
	
	/**
	 * Validate report structure.
	 */
	private void validateReportStructure() {
		// Check that the report contains the required sections for this calculation.
		if(report == null) {
			String errorMessage = "Error calculating the Squated PT/PD Position. Unable to access the report data.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		ConstTable reportData = report.getReportData();
		
		if(!reportData.getColumnNames().contains(ApBuySellFxDealSection.sectionName())) {
			String errorMessage = "Error calculating the Squated PT/PD Position. The required section "
					+ ApBuySellFxDealSection.sectionName() + " is not in the report.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		

	}
	
}
