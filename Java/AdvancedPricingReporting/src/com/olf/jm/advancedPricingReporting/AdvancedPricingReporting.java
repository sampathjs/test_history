package com.olf.jm.advancedPricingReporting;


import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.advancedPricingReporting.items.tables.EnumArgumentTable;
import com.olf.jm.advancedPricingReporting.items.tables.EnumArgumentTableBuList;
import com.olf.jm.advancedPricingReporting.output.DmsParameters;
import com.olf.jm.advancedPricingReporting.output.DmsReportWriter;
import com.olf.jm.advancedPricingReporting.output.ReportWriter;
import com.olf.jm.advancedPricingReporting.output.ReportWriterParameters;
import com.olf.jm.advancedPricingReporting.reports.ApDpReport;
import com.olf.jm.advancedPricingReporting.reports.ApDpReportParameters;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.calendar.HolidaySchedule;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;

import java.util.Date;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class AdvancedPricingReporting. 
 * 
 * Fetches the data needed for the advanced and deferred pricing exposure reports, build the required table structure
 * and passes the data to DMS for formatting. 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class AdvancedPricingReporting extends AbstractGenericScript {

	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Reporting";
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.generic.AbstractGenericScript#execute(com.olf.embedded.application.Context, com.olf.openrisk.table.ConstTable)
	 */
	@Override
	public Table execute(Context context, ConstTable table) {
		init();

		try {
			validateArgt(table);
			
			Table buList = table.getTable(EnumArgumentTable.EXTERNAL_BU_LIST.getColumnName(), 0);
			Date reportingDate = table.getDate(EnumArgumentTable.START_DATE.getColumnName(), 0);
			Date endDate = table.getDate(EnumArgumentTable.END_DATE.getColumnName(), 0);
			
			HolidaySchedule holidaySchedule = context.getCalendarFactory().getHolidaySchedule("JM HK 0830");
			ApDpReport report = new ApDpReport(context);
			while (!reportingDate.after(endDate)) {
				for (int row = 0; row < buList.getRowCount(); row++) {
					// Loop over the BUs that have been selected
					int externalBU = buList.getInt(EnumArgumentTableBuList.BU_ID.getColumnName(), row);
					ApDpReportParameters parameters = new ApDpReportParameters(20007, externalBU, reportingDate);
					report.generateReport(parameters);
					ConstTable reportData = report.getReportData();
					generateReportOutput(reportData, context, parameters);
				}
				reportingDate = holidaySchedule.getNextGoodBusinessDay(reportingDate);
			}
			
			return context.getTableFactory().createTable("returnT");
		} catch (Exception e) {
			Logging.error("Error running the advanced pricing report. " + e.getLocalizedMessage());
			throw new RuntimeException("Error running the advanced pricing report. " + e.getLocalizedMessage());
		}finally{
			Logging.close();
		}
	}
	
	private void generateReportOutput(ConstTable reportData, Context context, ReportParameters reportParameters) {
		
		// Use the JVS to XML as it formats the XML the same as exporting the table. Using the OC method adds an additional layer to the XML
		try {
			ReportWriterParameters parameters = new DmsParameters(context, new ConstRepository(CONST_REPOSITORY_CONTEXT,
																							   CONST_REPOSITORY_SUBCONTEXT));
			ReportWriter writer = new DmsReportWriter(parameters, reportParameters, context);
			String xmlData = context.getTableFactory().toOpenJvs(reportData).tableToXMLString(1, 0, "", "", 2, 0, 1, 1, 0);
			Logging.debug("XML data: " + xmlData);
			writer.generateReport(xmlData);
		} catch (OException e) {
			Logging.error("Error generating the report output. " + e.getLocalizedMessage());
			throw new RuntimeException("Error generating the report output. " + e.getLocalizedMessage());
		}
	}
	
	/**
	 * Validate the argument table. Throws a runtime exception on error.
	 *
	 * @param table the argument table to validate
	 */
	private void validateArgt(ConstTable table) {
		checkArgument(table != null && table.getRowCount() == 1,
					  "Error validating script argument table. Expecting 1 row in argument table");
		checkNotNull(table.getDate(EnumArgumentTable.START_DATE.getColumnName(), 0),
					 "Error validating script argument table. Start date is null");
		checkNotNull(table.getDate(EnumArgumentTable.END_DATE.getColumnName(), 0),
					 "Error validating script argument table. End date is null");
		Table buList = table.getTable(EnumArgumentTable.EXTERNAL_BU_LIST.getColumnName(), 0);
		checkArgument(buList != null && buList.getRowCount() > 0,
					  "Error validating script argument table. Invalid business unit list expecting at lease one entry.");
	}
	
	/**
	 * Initialise the logging framework.
	 */
	private void init() {
		try {
			Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException("Error initialising logging. " + e.getMessage());
		}
	}
}
