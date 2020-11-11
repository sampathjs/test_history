package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDispatchDealSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.advancedPricingReporting.sections.ApBuySellFxDealSection;
import com.olf.jm.advancedPricingReporting.sections.ApDispatchDealSection;
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
 * The Class CollectedAPMetal. sets the collected advanced pricing total, item is depended on the ApDispatchDealSection section 
 * to ready the total amount
 */
public class CollectedAPMetal extends ItemBase {

	/**
	 * Instantiates a new collected ap metal.
	 *
	 * @param currentContext the current context
	 * @param report the report
	 */
	public CollectedAPMetal(Context currentContext, Report report) {
		super(currentContext, report);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		return new EnumColType[] {EnumFinalBalanceSection.COLLECTED_AP_METAL.getColumnType()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] {EnumFinalBalanceSection.COLLECTED_AP_METAL.getColumnName()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		Logging.info("Calculating the collected ap metal field");
		
		super.addData(toPopulate, reportParameters);
		
		validateReportStructure();
		
		ConstTable reportData = report.getReportData();
		Table dispatchDeals = reportData.getTable(ApDispatchDealSection.sectionName(), 0);
		
		if(dispatchDeals == null ) {
			String errorMessage = "Error calculating the collected ap metal field. The required section "
					+ ApBuySellFxDealSection.sectionName() + " is not valid";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		if(dispatchDeals.getRowCount() > 0) {
			
			int columnId = dispatchDeals.getColumnId(EnumDispatchDealSection.TOTAL_DISP_AP_DEAL.getColumnName());
			double total = dispatchDeals.calcAsDouble(columnId, EnumColumnOperation.Sum);
			toPopulate.setDouble(EnumFinalBalanceSection.COLLECTED_AP_METAL.getColumnName(), 0, total);			
		}
	}
	
	/**
	 * Validate report structure checking that the required data is present.
	 */
	private void validateReportStructure() {
		// Check that the report contains the required sections for this calculation.
		if(report == null) {
			String errorMessage = "Error calculating the collected ap metal field. Unable to access the report data.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		ConstTable reportData = report.getReportData();
		
		if(!reportData.getColumnNames().contains(ApDispatchDealSection.sectionName())) {
			String errorMessage = "Error calculating the collected ap metal field. The required section "
					+ ApDispatchDealSection.sectionName() + " is not in the report.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	}	
}
