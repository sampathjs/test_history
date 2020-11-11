package com.olf.jm.advancedPricingReporting.items;


import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ItemBase. Base implementation of a section item
 */
public abstract class ItemBase implements SectionItem {

	/** The script context. */
	protected Context context;
	
	/** The report the item belongs to. */
	protected Report report;
	
	/**
	 * Instantiates a new item base.
	 *
	 * @param currentContext the current script context
	 */
	protected ItemBase( Context currentContext) {
		context = currentContext;
		report = null;
	}
	
	/**
	 * Instantiates a new item base.
	 *
	 * @param currentContext the current script context
	 * @param reportParent the report the item is associated with
	 */
	protected ItemBase( Context currentContext, Report reportParent) {
		context = currentContext;
		report = reportParent;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		if(toPopulate.getRowCount() == 0) {
			toPopulate.addRows(1);
		}
		
		if(toPopulate.getRowCount() != 1) {
			throw new RuntimeException("Invalid Number of rows in argument table");
		}

	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#format(com.olf.openrisk.table.Table)
	 */
	public Table format(Table reportSectionToFormat) {
		return reportSectionToFormat;
	}
	
	/**
	 * Run sql.
	 * 
	 * @param sql
	 *            the sql
	 * @return the table
	 */
	protected Table runSQL(final String sql) {

		IOFactory iof = context.getIOFactory();

		Logging.debug("About to run SQL. \n" + sql);

		Table data;
		try {
			data = iof.runSQL(sql);
		} catch (Exception e) {
			String errorMessage = "Error executing SQL: " + sql + ". Error: "
					+ e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}

		return data;
	}	

}
