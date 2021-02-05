/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.sections;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.APMetalName;
import com.olf.jm.advancedPricingReporting.items.CollectedAPMetal;
import com.olf.jm.advancedPricingReporting.items.CollectedDPMetal;
import com.olf.jm.advancedPricingReporting.items.DPMetalName;
import com.olf.jm.advancedPricingReporting.items.DailyInterest;
import com.olf.jm.advancedPricingReporting.items.DeferredPricingShort;
import com.olf.jm.advancedPricingReporting.items.DepositHKD;
import com.olf.jm.advancedPricingReporting.items.DepositUSDFromCashTrans;
import com.olf.jm.advancedPricingReporting.items.Difference;
import com.olf.jm.advancedPricingReporting.items.DollarBalance;
import com.olf.jm.advancedPricingReporting.items.Tier2Margin;
import com.olf.jm.advancedPricingReporting.items.SquaredMetalPosition;
import com.olf.jm.advancedPricingReporting.items.Tier1Margin;
import com.olf.jm.advancedPricingReporting.items.TodaysBalance;
import com.olf.jm.advancedPricingReporting.items.TotalMargin;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDeferredPriceShortSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumFinalBalanceSection;
import com.olf.jm.advancedPricingReporting.items.tables.EnumSquaredMetalPositionSection;
import com.olf.jm.advancedPricingReporting.items.tables.TableColumnHelper;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class TotalMarginFinalBalanceSection. Defines the report section containing all the total balance information
 */
public class TotalMarginFinalBalanceSection extends ReportSectionBase {

	

	/**
	 * Instantiates a new  balance section .
	 *
	 * @param context the script context
	 * @param report the current report
	 */
	public TotalMarginFinalBalanceSection(Context context, Report report) {
		super(context, report);
		
		if(report == null) {
			String errorMessage = "Error initialising the balance section, report is null.";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#addSectionItems()
	 */
	@Override
	protected void addSectionItems() {


		sectionItems.add(new DPMetalName(context, report));
		sectionItems.add(new APMetalName(context, report));
		sectionItems.add(new DollarBalance(context));
		sectionItems.add(new DepositUSDFromCashTrans(context));
		sectionItems.add(new DepositHKD(context));
		sectionItems.add(new SquaredMetalPosition(context, report));
		sectionItems.add(new CollectedDPMetal(context, report));
		sectionItems.add(new CollectedAPMetal(context, report));	
		sectionItems.add(new DailyInterest(context, report));
		sectionItems.add(new TodaysBalance(context));
		sectionItems.add(new Tier1Margin(context, report));
		sectionItems.add(new Tier2Margin(context, report));
		sectionItems.add(new DeferredPricingShort(context, report));
		sectionItems.add(new TotalMargin(context));
		sectionItems.add(new Difference(context));
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#getSectionName()
	 */
	@Override
	public String getSectionName() {
		return "Total Margin and Final Balance";
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#formatForReporting(com.olf.openrisk.table.Table)
	 */
	@Override
	public Table formatForReporting(Table reportSectionToFormat) {
		TableColumnHelper<EnumFinalBalanceSection> columnHelper = new TableColumnHelper<>();
		
		columnHelper.formatTableForOutput(EnumFinalBalanceSection.class, reportSectionToFormat);
		
		TableColumnHelper<EnumDeferredPriceShortSection> columnHelperSubTable = new TableColumnHelper<>();
		Table formatted = columnHelperSubTable.formatTableForOutput(EnumDeferredPriceShortSection.class, reportSectionToFormat.getTable(EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnName(), 0));		
		reportSectionToFormat.setTable(EnumFinalBalanceSection.DEFERRED_PRICING_SHORT.getColumnName(), 0, formatted);
		
		TableColumnHelper<EnumSquaredMetalPositionSection> columnHelperSubTable2 = new TableColumnHelper<>();
		Table formatted2 = columnHelperSubTable2.formatTableForOutput(EnumSquaredMetalPositionSection.class, reportSectionToFormat.getTable(EnumFinalBalanceSection.SQUARED_METAL_POSITION.getColumnName(), 0));		
		reportSectionToFormat.setTable(EnumFinalBalanceSection.SQUARED_METAL_POSITION.getColumnName(), 0, formatted2);
		
		
		return reportSectionToFormat;
	}
	
	


}
