/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.sections;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.ApDispatchedDeals;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ApDispatchDealSection. Defines the  section containing dispatch advanced pricing deals
 */
public class ApDispatchDealSection extends ReportSectionBase {

	/**
	 * Instantiates a new  dispatch advanced pricing section.
	 *
	 * @param context the context
	 */
	public ApDispatchDealSection(Context context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#addSectionItems()
	 */
	@Override
	protected void addSectionItems() {
		sectionItems.add(new ApDispatchedDeals(context));
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#getSectionName()
	 */
	@Override
	public String getSectionName() {
		return ApDispatchDealSection.sectionName();
	}

	/**
	 * Get the section name.
	 *
	 * @return the string containing the section name
	 */
	public static String sectionName() {
		return "AP Dispatch Deals";
	}
}
