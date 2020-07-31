package com.olf.jm.advancedPricingReporting;


import java.util.Date;

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
import com.olf.openjvs.OException;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


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

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Reporting";
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.generic.AbstractGenericScript#execute(com.olf.embedded.application.Context, com.olf.openrisk.table.ConstTable)
	 */
	@Override
	public Table execute(Context context, ConstTable table) {
		
		try {
			init();
		} catch (Exception e) {
			throw new RuntimeException("Error initilising logging. "  + e.getLocalizedMessage());
		}

		try {
			ApDpReport report = new ApDpReport(context);

			validateArgt(table);
			
			Date reportingDate = table.getDate(EnumArgumentTable.RUN_DATE.getColumnName(), 0);
			
			Table buList = table.getTable(EnumArgumentTable.BU_LIST.getColumnName(), 0);
			
			for(int row = 0; row < buList.getRowCount(); row++) {
				// Loop over the BU's that have been selected
				int externalBU = buList.getInt(EnumArgumentTableBuList.BU_ID.getColumnName(), row);

				ApDpReportParameters parameters = new ApDpReportParameters(20007, externalBU, reportingDate);
								
				report.generateReport(parameters);
				
				ConstTable reprotData = report.getReportData();
				
				generateReportOutput(reprotData, context, parameters);
				
				if(PluginLog.getLogLevel().equalsIgnoreCase("debug")) {
					context.getDebug().viewTable(reprotData);
				}
			}
			
		} catch (Exception e) {
			PluginLog.error("Error running the advanced pricing report. " + e.getLocalizedMessage());
			throw new RuntimeException("Error running the advanced pricing report. " + e.getLocalizedMessage());
		}
		
		return context.getTableFactory().createTable("returnT");
	}
	
	private void generateReportOutput(ConstTable reprotData, Context context, ReportParameters reportParameters) {
		
		ReportWriterParameters parameters = new DmsParameters(context, constRep);
		
		ReportWriter writter = new DmsReportWriter(parameters, reportParameters, context);
		
		// Use the JVS to XML as it formats the XML the same as exporting the table. Using the OC method adds an additional layer to the XML
		try {
			//String xmlData = context.getTableFactory().toOpenJvs(reprotData).tableToXMLString();
			String xmlData = context.getTableFactory().toOpenJvs(reprotData).tableToXMLString(1, 0, "", "", 2, 0, 1, 1, 0);
			PluginLog.debug("XML data: " + xmlData);
			writter.generateReport(xmlData);
			//writter.generateReport(reprotData.asXmlString());
		} catch (OException e) {
			PluginLog.error("Error generating the report output. " + e.getLocalizedMessage());
			throw new RuntimeException("Error generating the report output. " + e.getLocalizedMessage());
		}
		
	}
	
	/**
	 * Validate the argument table. Throws a runtime exception on error.
	 *
	 * @param table the argument table to validate
	 */
	private void validateArgt(ConstTable table) {
		// Validate the run date
		if(table == null || table.getRowCount() != 1) {
			String errorMessage = "Error validating script argument table. Expecting 1 row in argument table";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		Date runDate = table.getDate(EnumArgumentTable.RUN_DATE.getColumnName(), 0);
		
		if(runDate == null  ) {
			String errorMessage = "Error validating script argument table. Run date is null";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		Table buList = table.getTable(EnumArgumentTable.BU_LIST.getColumnName(), 0);
		
		if(buList == null || buList.getRowCount() < 1) {
			String errorMessage = "Error validating script argument table. Invalid business unit list expecting at lease one entry.";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
	}
	
	/**
	 * Initilise the logging framework.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}

}
