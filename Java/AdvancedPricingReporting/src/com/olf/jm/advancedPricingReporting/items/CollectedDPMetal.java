package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingData;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFxDealData;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFxDealSection;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.advancedPricingReporting.sections.ApBuySellFxDealSection;
import com.olf.jm.advancedPricingReporting.sections.DeferredPricingSection;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class CollectedDPMetal. sets the collected deferred pricing total, item is depended on the DeferredPricingSection section 
 * to ready the total amount
 */
public class CollectedDPMetal extends ItemBase {

	/**
	 * Instantiates a new collected deferred pricing metal item.
	 *
	 * @param currentContext the current context
	 * @param report the report
	 */
	public CollectedDPMetal(Context currentContext, Report report) {
		super(currentContext, report);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		return new EnumColType[] {EnumFinalBalanceSection.COLLECTED_DP_METAL.getColumnType()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] {EnumFinalBalanceSection.COLLECTED_DP_METAL.getColumnName()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		super.addData(toPopulate, reportParameters);
		
		validateReportStructure();
		
		ConstTable reportData = report.getReportData();
		Table deals = reportData.getTable(DeferredPricingSection.sectionName(), 0);
		
		if(deals == null) {
			String errorMessage = "Error calculating the collected dp metal field. The required section "
					+ ApBuySellFxDealSection.sectionName() + " is not valid";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		if(deals.getRowCount() >0) {
			double collectedDp = 0;
			for(int row = 0; row< deals.getRowCount(); row++) {
				Table dealData = deals.getTable(EnumFxDealSection.REPORT_DATA.getColumnName(), row);
				
				if(dealData != null && dealData.getRowCount() >0) {
					ConstTable fxMatched = dealData.createConstView("*", "["+EnumFxDealData.TYPE.getColumnName()+"] == 'FX Matched'");
					
					if(fxMatched != null && fxMatched.getRowCount() >0) {
						int columnId = fxMatched.getColumnId(EnumDeferredPricingData.SETTLEMENT_VALUE.getColumnName());
						collectedDp += fxMatched.calcAsDouble(columnId, EnumColumnOperation.Sum);
					}
				}
					
			}
			//int columnId = dispatchDeals.getColumnId(EnumDeferredPricingSection.TOTAL_DISPATCHED.getColumnName());
			//double total = dispatchDeals.calcAsDouble(columnId, EnumColumnOperation.Sum);
			toPopulate.setDouble(EnumFinalBalanceSection.COLLECTED_DP_METAL.getColumnName(), 0, collectedDp * -1.0);
		}

	}
	
	/**
	 * Validate report structure.
	 */
	private void validateReportStructure() {
		// Check that the report contains the required sections for this calculation.
		if(report == null) {
			String errorMessage = "Error calculating the collected dp metal field. Unable to access the report data.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		ConstTable reportData = report.getReportData();
		
		if(!reportData.getColumnNames().contains(DeferredPricingSection.sectionName())) {
			String errorMessage = "Error calculating the collected dp metal field. The required section "
					+ DeferredPricingSection.sectionName() + " is not in the report.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	}		
}
