package com.olf.jm.advancedPricingReporting.items;

import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Interface SectionItem. Defines an item that makes up a report section
 */
public interface SectionItem {

	/**
	 * Gets the data types for the columns that make up this item.
	 *
	 * @return the data types
	 */
	EnumColType[] getDataTypes();
	
	/**
	 * Gets the column names for the columns that make up this item.
	 *
	 * @return the column names
	 */
	String[] getColumnNames();
	
	/**
	 * Adds the data for this item to the report section.
	 *
	 * @param toPopulate the table to populate with item data
	 * @param reportParameters the report parameters
	 */
	void addData(Table toPopulate, ReportParameters reportParameters);

	/**
	 * Format the item ready for output.
	 *
	 * @param reportSectionToFormat the report section to format
	 * @return the formatted table
	 */
	Table format(Table reportSectionToFormat);
	
}
