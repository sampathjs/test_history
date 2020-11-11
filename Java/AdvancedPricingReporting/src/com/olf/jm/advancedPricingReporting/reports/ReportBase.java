package com.olf.jm.advancedPricingReporting.reports;

import java.util.ArrayList;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.sections.ReportSection;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;

public abstract class ReportBase implements Report{

	/** The context. */
	protected Context context;
	/** The report sections. */
	protected List<ReportSection> reportSections;
	/** The report data. */
	private Table reportData;

	public ReportBase(Context currentContext){
		context = currentContext;
	
		reportSections = new ArrayList<>();
		
		buildReportSections();
		
	}

	protected abstract void buildReportSections();
	
	protected abstract String getReportName();

	@Override
	public void generateReport(ReportParameters reportParameters) {
		reportData = context.getTableFactory().createTable(getReportName());
	
		buildTableStructure();
		
		addData(reportParameters);
		
		formatData();	
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.reports.Report#getReportData()
	 */
	@Override
	public ConstTable getReportData() {
		return reportData;
	}

	/**
	 * Format data.
	 */
	private void formatData() {
		for(ReportSection section : reportSections) {
			String sectionName = section.getSectionName();
			
			Table reportSection = reportData.getTable(sectionName, 0);
			
			reportSection = section.formatForReporting(reportSection);
			
			reportData.setTable(sectionName, 0, reportSection);
		}
	}

	/**
	 * Adds the data to the output table.
	 *
	 * @param reportParameters the report parameters
	 */
	private void addData(ReportParameters reportParameters) {
		for(ReportSection section : reportSections) {
			String sectionName = section.getSectionName();
			
			Table reportSection = reportData.getTable(sectionName, 0);
			
			section.addData(reportSection, reportParameters);
		}
	}

	/**
	 * Builds the table structure.
	 */
	private void buildTableStructure() {
		
		
		for(ReportSection section : reportSections) {
			String sectionName = section.getSectionName();
								
			reportData.addColumn(sectionName, EnumColType.Table);
		}
		
		reportData.addRows(1);
	
		
		for(ReportSection section : reportSections) {
			String sectionName = section.getSectionName();
				
			Table reportSection = section.buildSectionTableStructure();
			
			reportData.setTable(sectionName, 0, reportSection);
		}		
	}

}