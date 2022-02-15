/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.sections;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.MetalPricesForToday;
import com.olf.jm.advancedPricingReporting.items.PartyAddress;
import com.olf.jm.advancedPricingReporting.items.PartyAddress.EnumPartyAddressType;
import com.olf.jm.advancedPricingReporting.items.PartyAddress.EnumPartyType;
import com.olf.jm.advancedPricingReporting.reports.Report;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ReportHeader. The report header for the advanced pricing report
 */
public class ReportHeader extends ReportSectionBase {

	/**
	 * Instantiates a new report header section.
	 *
	 * @param context the script context
	 */
	public ReportHeader(Context context, Report report) {
		super(context, report);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#addSectionItems()
	 */
	@Override
	protected void addSectionItems() {
		sectionItems.add(new PartyAddress(context, EnumPartyAddressType.MAIN, EnumPartyType.INTERNAL));
		sectionItems.add(new PartyAddress(context, EnumPartyAddressType.MAIN, EnumPartyType.EXTERNAL));
		sectionItems.add(new MetalPricesForToday(context, report));
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSectionBase#getSectionName()
	 */
	@Override
	public String getSectionName() {
		return "ReportHeader";
	}



	
}
