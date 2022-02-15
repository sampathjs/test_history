package com.olf.jm.advancedPricingReporting.sections;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.ApDailySummary;
import com.olf.jm.advancedPricingReporting.items.ApprovedTradeLimit;
import com.olf.jm.advancedPricingReporting.items.DailySummaryCustomers;
import com.olf.jm.advancedPricingReporting.items.DpDailySummary;
import com.olf.jm.advancedPricingReporting.items.EndCashBalanceDailySummary;
import com.olf.jm.advancedPricingReporting.items.JmHkDailySummaryTotals;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ApDpDailySummarySection. Defines the  section containing daily customer report details
 */
public class ApDpDailySummarySection extends ReportSectionBase {

	/**
	 * Instantiates a new  summary section.
	 *
	 * @param context the context
	 */
	public ApDpDailySummarySection(Context context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#addSectionItems()
	 */
	@Override
	protected void addSectionItems() {
		sectionItems.add(new DailySummaryCustomers(context));
		sectionItems.add(new ApDailySummary(context));
		sectionItems.add(new DpDailySummary(context));
		sectionItems.add(new EndCashBalanceDailySummary(context));
		sectionItems.add(new JmHkDailySummaryTotals(context));
		sectionItems.add(new ApprovedTradeLimit(context));
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#getSectionName()
	 */
	@Override
	public String getSectionName() {
		return ApDpDailySummarySection.sectionName();
	}

	/**
	 * Get the section name.
	 *
	 * @return the string containing the section name
	 */
	public static String sectionName() {
		return "AP DP Daily Summary";
	}
}
