package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.advancedPricingReporting.sections.DeferredPricingSection;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class DPMetalName.Section item to set the deferred pricing metal name. Item is dependent on the deferred pricing 
 *  deal data. 
 */
public class DPMetalName extends ItemBase {

	/**
	 * Instantiates a new deferred pricing metal name item. 
	 *
	 * @param currentContext the current context
	 * @param report the report
	 */
	public DPMetalName(Context currentContext, Report report) {
		super(currentContext, report);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		EnumColType[] columnTypes = new EnumColType[] {EnumFinalBalanceSection.DP_METAL_NAME.getColumnType()};
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] columns = new String[] {EnumFinalBalanceSection.DP_METAL_NAME.getColumnName()};
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
		Table dispatchDeals = reportData.getTable(DeferredPricingSection.sectionName(), 0);
		
		if(dispatchDeals == null) {
			String errorMessage = "Error calculating the collected dp metal name field. The required section "
					+ DeferredPricingSection.sectionName() + " is not valid";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		if(dispatchDeals.getRowCount() > 0) {
			String[] dpMetals = dispatchDeals.getColumnValuesAsString(EnumDeferredPricingSection.METAL_SHORT_NAME.getColumnName());
			
			toPopulate.setString(EnumFinalBalanceSection.DP_METAL_NAME.getColumnName(), 0, arrayToCsvString(dpMetals, toPopulate));
		}

	}
	
	private String arrayToCsvString(String[] dpMetals, Table toPopulate) {
		StringBuilder sb = new StringBuilder();
		
		String apMetals = toPopulate.getString(EnumFinalBalanceSection.AP_METAL_NAME.getColumnName(), 0);
		
		for (String dpMetal : dpMetals) { 
		
			if(apMetals.contains(dpMetal)) {
				// If metal name is in the AP list don't add it to the DP list
				continue;
			}
			// Check if the 
		    if (sb.length() > 0) {
		    	sb.append(',');
		    }
		    sb.append(dpMetal);
		}
		return sb.toString();		
	}
	
	/**
	 * Validate report structure.
	 */
	private void validateReportStructure() {
		// Check that the report contains the required sections for this calculation.
		if(report == null) {
			String errorMessage = "Error calculating the collected dp metal name field. Unable to access the report data.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		ConstTable reportData = report.getReportData();
		
		if(!reportData.getColumnNames().contains(DeferredPricingSection.sectionName())) {
			String errorMessage = "Error calculating the collected dp metal name field. The required section "
					+ DeferredPricingSection.sectionName() + " is not in the report.";
			
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	}		
}
