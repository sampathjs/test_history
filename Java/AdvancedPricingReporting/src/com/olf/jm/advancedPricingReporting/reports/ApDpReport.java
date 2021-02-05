/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.reports;

import java.util.ArrayList;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.sections.ApBuySellFxDealSection;
import com.olf.jm.advancedPricingReporting.sections.DeferredPricingSection;
import com.olf.jm.advancedPricingReporting.sections.ReportHeader;
import com.olf.jm.advancedPricingReporting.sections.ApDispatchDealSection;
import com.olf.jm.advancedPricingReporting.sections.ReportSection;
import com.olf.jm.advancedPricingReporting.sections.TotalMarginFinalBalanceSection;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ApDpReport. A report containing information about advanced and deferred pricing deals. 
 */
public class ApDpReport implements Report {
	
	/** The context. */
	private final Context context;
	
	/** The report sections. */
	private final List<ReportSection> reportSections;
	
	/** The report data. */
	private Table reportData;	
	
	/**
	 * Instantiates a new advanced and deferred pricing report.
	 *
	 * @param currentContext the script context
	 */
	public ApDpReport(Context currentContext){
		context = currentContext;
	
		reportSections = new ArrayList<>();
		
		buildReportSections();
		
	}
	

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.reports.Report#generateReport(com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void generateReport(ReportParameters reportParameters) {
		
		reportData = context.getTableFactory().createTable("ApReport");
		
		
		
		buildTableStructure();
		
		addData(reportParameters);
		
		formatData();
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
	 * Builds the report sections that make up the report.
	 */
	private void buildReportSections() {
		
		reportSections.add( new ApDispatchDealSection(context));
		reportSections.add( new ApBuySellFxDealSection(context));
		reportSections.add( new DeferredPricingSection(context));
		reportSections.add( new TotalMarginFinalBalanceSection(context, this));
		reportSections.add( new ReportHeader(context, this));
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


	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.reports.Report#getReportData()
	 */
	@Override
	public ConstTable getReportData() {
		return reportData;
	}

}
