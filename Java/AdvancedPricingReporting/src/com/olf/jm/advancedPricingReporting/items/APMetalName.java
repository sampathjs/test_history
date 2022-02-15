/*
 * File updated 05/02/2021, 17:52
 */

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
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class APMetalName. Section item to set the advanced pricing metal name. Item is dependent on the advanced pricing 
 * dispatch deal data. 
 */
public class APMetalName extends ItemBase {

	/**
	 * Instantiates a new AP metal name.
	 *
	 * @param currentContext the current context
	 * @param report the report
	 */
	public APMetalName(Context currentContext, Report report) {
		super(currentContext, report);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		return new EnumColType[] {EnumFinalBalanceSection.AP_METAL_NAME.getColumnType()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] {EnumFinalBalanceSection.AP_METAL_NAME.getColumnName()};
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		Logging.info("Calculating the collected ap metal name field");
		
		super.addData(toPopulate, reportParameters);
		
		validateReportStructure();
		
		ConstTable reportData = report.getReportData();
		Table dispatchDeals = reportData.getTable(ApDispatchDealSection.sectionName(), 0);
		
		if(dispatchDeals == null ) {
			String errorMessage = "Error calculating the collected ap metal name field. The required section "
					+ ApBuySellFxDealSection.sectionName() + " is not valid";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		if(dispatchDeals.getRowCount() > 0) {
			String[] dpMetals = dispatchDeals.getColumnValuesAsString(EnumDispatchDealSection.METAL_SHORT_NAME.getColumnName());
			toPopulate.setString(EnumFinalBalanceSection.AP_METAL_NAME.getColumnName(), 0, arrayToCsvString(dpMetals, toPopulate));
		}
	}
	
	private String arrayToCsvString(String[] apMetals, Table toPopulate) {
		StringBuilder sb = new StringBuilder();
		
		String dpMetals = toPopulate.getString(EnumFinalBalanceSection.DP_METAL_NAME.getColumnName(), 0);
		
		for (String apMetal : apMetals) { 
			if(dpMetals.contains(apMetal)) {
				// If metal name is in the AP list don't add it to the DP list
				continue;
			}

			
		    if (sb.length() > 0) {
		    	sb.append(',');
		    }
		    
		    sb.append(apMetal);
		}
		return sb.toString();		
	}
	
	/**
	 * Validate report structure checking the required data is present.
	 */
	private void validateReportStructure() {
		// Check that the report contains the required sections for this calculation.
		if(report == null) {
			String errorMessage = "Error calculating the collected ap metal name field. Unable to access the report data.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		ConstTable reportData = report.getReportData();
		
		if(!reportData.getColumnNames().contains(ApDispatchDealSection.sectionName())) {
			String errorMessage = "Error calculating the collected ap metal name field. The required section "
					+ ApDispatchDealSection.sectionName() + " is not in the report.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	}	
}
