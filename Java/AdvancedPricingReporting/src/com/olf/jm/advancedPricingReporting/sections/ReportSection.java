/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.sections;

import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Interface ReportSection. Defines a section in the output report, contains method to
 * initialise the table structure and populate with data
 */
public interface ReportSection {

	/**
	 * Adds the data to the output table, called to build up the report.
	 *
	 * @param populateWithData the table to populate with data
	 * @param reportParameters the report parameters controlling the data extract
	 */
	void addData(Table populateWithData, ReportParameters reportParameters);
	
	/**
	 * Builds the section table structure.
	 *
	 * @return an empty table with the column names and data types defined.
	 */
	Table  buildSectionTableStructure();
	
	/**
	 * Gets the section name.
	 *
	 * @return the section name
	 */
	String getSectionName();
	
	/** 
	 * Indicates is the current section has a dependency on data generated in the previous section. 
	 */
	boolean needsPriorSections();
	
	
	/**
	 * Format the section data so that it can be used in the output module.
	 */
	Table formatForReporting(Table reportSectionToFormat);
}
