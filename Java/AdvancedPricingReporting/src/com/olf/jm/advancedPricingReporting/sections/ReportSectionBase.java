package com.olf.jm.advancedPricingReporting.sections;

import java.util.ArrayList;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.items.SectionItem;
import com.olf.jm.advancedPricingReporting.reports.Report;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ReportSectionBase. Base implementation of a report section
 */
public abstract class ReportSectionBase implements ReportSection {

	/** The section items. */
	protected List<SectionItem> sectionItems;
	
	/** The script context. */
	protected Context context;
	
	/** The report the section belongs to */
	protected Report report;
	
	/**
	 * Instantiates a new report section base.
	 *
	 * @param context the script context
	 */
	protected ReportSectionBase( Context context) {
		this.context = context;
		
		report = null;
		
		sectionItems = new ArrayList<SectionItem>();
		
		addSectionItems();
	}
	
	/**
	 * Instantiates a new report section base.
	 *
	 * @param context the script context
	 * @param report the report the section belongs to
	 */
	protected ReportSectionBase( Context context, Report report) {
		this.context = context;
		
		this.report = report;
		
		sectionItems = new ArrayList<SectionItem>();
		
		addSectionItems();
	}	
	/**
	 * Adds the section items. Implemented by derived classes to add
	 * report item to the current section. 
	 */
	protected abstract void addSectionItems(); 
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSection#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table populateWithData, ReportParameters reportParameters) {
		for(SectionItem item : sectionItems) {
			item.addData(populateWithData, reportParameters);
		}		
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSection#buildSectionTableStructure()
	 */
	@Override
	public Table buildSectionTableStructure() {
		Table section = context.getTableFactory().createTable(getSectionName());
		
		for(SectionItem item : sectionItems) {
			section.addColumns(item.getColumnNames(), item.getDataTypes());
		}
		return section;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSection#getSectionName()
	 */
	@Override
	public abstract String getSectionName();
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSection#needsPriorSections()
	 */
	@Override 
	public boolean needsPriorSections() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.sections.ReportSection#formatForReporting(com.olf.openrisk.table.Table)
	 */
	@Override
	public Table formatForReporting(Table reportSectionToFormat) {
		for(SectionItem item : sectionItems) {
			reportSectionToFormat = item.format(reportSectionToFormat);
		}	
		
		return reportSectionToFormat;
	}

}
