package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDispatchDealSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.advancedPricingReporting.sections.ApBuySellFxDealSection;
import com.olf.jm.advancedPricingReporting.sections.ApDispatchDealSection;
import com.olf.jm.advancedPricingReporting.sections.DeferredPricingSection;
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
 * The Class tier 1 margin . Item to calculate the based on the AP and BP tier 1 calculations. Item is dependent on data in the 
 * dispatch and deferred deal sections. 
 */
public class Tier1Margin extends ItemBase {

	/**
	 * Instantiates a new three precent margin.
	 *
	 * @param currentContext the current context
	 * @param report the report
	 */
	public Tier1Margin(Context currentContext, Report report) {
		super(currentContext, report);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		EnumColType[] columnTypes = new EnumColType[] {EnumFinalBalanceSection.TIER_1_VALUE.getColumnType()};
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] columns = new String[] {EnumFinalBalanceSection.TIER_1_VALUE.getColumnName()};
		return columns;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		super.addData(toPopulate, reportParameters);
		validateReportStructure();
		
		ConstTable reportData = report.getReportData();
		Table dispatchDeals = reportData.getTable(ApDispatchDealSection.sectionName(), 0);
		
		double total = 0.0;
		if(dispatchDeals == null ) {
			String errorMessage = "Error calculating the tier 1 margin field. The required section "
					+ ApBuySellFxDealSection.sectionName() + " is not valid";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		if(dispatchDeals.getRowCount() > 0) {
			
			int columnId = dispatchDeals.getColumnId(EnumDispatchDealSection.TIER_1_AP_VALUE.getColumnName());
			total += dispatchDeals.calcAsDouble(columnId, EnumColumnOperation.Sum);
			
			for(int row = 0; row < dispatchDeals.getRowCount(); row++) {
				double lossGain = dispatchDeals.getDouble(EnumDispatchDealSection.LOSS_GAIN.getColumnName(), row);
				
				if(lossGain < 0) {
					total += lossGain;
				}
			}
			//columnId = dispatchDeals.getColumnId(EnumDispatchDealSection.LOSS_GAIN.getColumnName());
			//total += dispatchDeals.calcAsDouble(columnId, EnumColumnOperation.Sum);
			
		}

		Table deferredDeals = reportData.getTable(DeferredPricingSection.sectionName(), 0);
		

		if(deferredDeals == null) {
			String errorMessage = "Error calculating the tier 1 margin field. The required section "
					+ DeferredPricingSection.sectionName() + " is not valid";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		if(deferredDeals.getRowCount() > 0) {
			int columnId = deferredDeals.getColumnId(EnumDeferredPricingSection.TIER_1_DP_VALUE.getColumnName());
			total += deferredDeals.calcAsDouble(columnId, EnumColumnOperation.Sum);
			
		}			
		
		toPopulate.setDouble(EnumFinalBalanceSection.TIER_1_VALUE.getColumnName(), 0, total);
	}
	
	/**
	 * Validate report structure.
	 */
	private void validateReportStructure() {
		// Check that the report contains the required sections for this calculation.
		if(report == null) {
			String errorMessage = "Error calculating the tier 1 margin field. Unable to access the report data.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		ConstTable reportData = report.getReportData();
		
		if(!reportData.getColumnNames().contains(ApDispatchDealSection.sectionName())) {
			String errorMessage = "Error calculating the tier 1 margin field. The required section "
					+ ApDispatchDealSection.sectionName() + " is not in the report.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		if(!reportData.getColumnNames().contains(DeferredPricingSection.sectionName())) {
			String errorMessage = "Error calculating the tier 1 margin field. The required section "
					+ DeferredPricingSection.sectionName() + " is not in the report.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}		
	}		
}
