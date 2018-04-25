package com.olf.jm.advancedPricingReporting.items;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPriceShortSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPricingSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDispatchDealSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.items.tables.TableColumnHelper;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.advancedPricingReporting.sections.DeferredPricingSection;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class DeferredPricingShort. Item to set the deferred pricing short field. 
 */
public class DeferredPricingShort extends ItemBase {

	/**
	 * Instantiates a new deferred pricing short.
	 *
	 * @param currentContext the current context
	 * @param report the report
	 */
	public DeferredPricingShort(Context currentContext, Report report) {
		super(currentContext, report);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {
		EnumColType[] columnTypes = new EnumColType[] {EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnType()};
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		String[] columns = new String[] {EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnName()};
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
		
		Table deferredDealSection = reportData.getTable(DeferredPricingSection.sectionName(), 0);
		
		if(deferredDealSection == null) {
			String errorMessage = "Error calculating the deferred pricing short field. The required section "
					+ DeferredPricingSection.sectionName() + " is not valid";
			
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}

		TableColumnHelper<EnumDeferredPriceShortSection> tableHelper = new TableColumnHelper<EnumDeferredPriceShortSection>();
		Table dpShort = tableHelper.buildTable(context, EnumDeferredPriceShortSection.class, EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnName());

		
		if(deferredDealSection.getRowCount() > 0) {
			String what = EnumDispatchDealSection.METAL_SHORT_NAME.getColumnName() + ", " + EnumDeferredPricingSection.TOTAL_DP_VALUE.getColumnName() + "->" + EnumDeferredPriceShortSection.DEFERRED_SHORT.getColumnName();
			String where = "[IN." + EnumDeferredPricingSection.TOTAL_DP_VALUE.getColumnName() + "] != 0.0";
			
			dpShort.select(deferredDealSection, what, where);	
			
		} 
		
		if(dpShort.getRowCount() == 0) {
			dpShort.addRows(1);
		}
		toPopulate.setTable(EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnName(), 0, dpShort);					
		
	}
	
	/**
	 * Validate report structure.
	 */
	private void validateReportStructure() {
		// Check that the report contains the required sections for this calculation.
		if(report == null) {
			String errorMessage = "Error calculating the deferred pricing short field. Unable to access the report data.";
			
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		ConstTable reportData = report.getReportData();
		
		if(!reportData.getColumnNames().contains(DeferredPricingSection.sectionName())) {
			String errorMessage = "Error calculating the deferred pricing short field. The required section "
					+ DeferredPricingSection.sectionName() + " is not in the report.";
			
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}		
	}		
}
