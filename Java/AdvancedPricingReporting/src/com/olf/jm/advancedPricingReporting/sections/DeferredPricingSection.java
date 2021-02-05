/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.sections;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.DeferredPricingDeals;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class DeferredPricingSection. Defines the section containing details on the deferred pricing deals
 */
public class DeferredPricingSection extends ReportSectionBase {

	/**
	 * Instantiates a new deferred pricing section.
	 *
	 * @param context the context
	 */
	public DeferredPricingSection(Context context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#addSectionItems()
	 */
	@Override
	protected void addSectionItems() {
		sectionItems.add(new DeferredPricingDeals(context));
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#getSectionName()
	 */
	@Override
	public String getSectionName() {
		return DeferredPricingSection.sectionName();
	}


	/**
	 * Get the section name.
	 *
	 * @return the string containing the section name
	 */
	public static String sectionName() {
		return "Deferred Pricing";
	}

}
