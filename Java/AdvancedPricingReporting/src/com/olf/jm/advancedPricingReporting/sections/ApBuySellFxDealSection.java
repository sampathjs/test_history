/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.sections;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.ApBuySellFxDeals;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ApBuySellFxDealSection. Defines the  section containing buy / sell FX deals
 */
public class ApBuySellFxDealSection extends ReportSectionBase {

	/**
	 * Instantiates a new buy / sell advanced pricing section
	 *
	 * @param context the context
	 */
	public ApBuySellFxDealSection(Context context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#addSectionItems()
	 */
	@Override
	protected void addSectionItems() {
		sectionItems.add(new ApBuySellFxDeals(context));
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#getSectionName()
	 */
	@Override
	public String getSectionName() {
		return ApBuySellFxDealSection.sectionName();
	}

	/**
	 * Get the section name.
	 *
	 * @return the string containing the section name
	 */
	public static String sectionName() {
		return "AP Buy Sell FX Deals";
	}

}
