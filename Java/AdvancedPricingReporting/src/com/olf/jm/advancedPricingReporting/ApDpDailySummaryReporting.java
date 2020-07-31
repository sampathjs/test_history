package com.olf.jm.advancedPricingReporting;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.advancedPricingReporting.items.ApprovedTradeLimit;
import com.olf.jm.advancedPricingReporting.items.tables.EnumDailySummarySection;
import com.olf.jm.advancedPricingReporting.reports.ApDpReportParameters;
import com.olf.jm.advancedPricingReporting.reports.DailySummaryReport;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;


// TODO: Auto-generated Javadoc
/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class ApDpDailySummaryReporting. 
 * 
 * Fetches the data needed for the HK advanced and deferred pricing daily customer reports summary, build the required table structure
 * and writes the data to a excel spreadsheet. 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class ApDpDailySummaryReporting extends AbstractGenericScript {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Daily Summary Reporting";
	

	/**  Map defining the column names in the spreadsheet to the internal table columns. */
	private static final Map<EnumDailySummarySection, String> COLUMN_NAME_MAP = 
		    Collections.unmodifiableMap(new HashMap<EnumDailySummarySection, String>() {

				private static final long serialVersionUID = -4115472428821191347L;

			{ 
		        put(EnumDailySummarySection.CUSTOMER_NAME, "Customer");
		        put(EnumDailySummarySection.CUSTOMER_ID, "Customer Id");
		    	put(EnumDailySummarySection.AG_AP_TOZ, "AG AP Toz");
		    	put(EnumDailySummarySection.AU_AP_TOZ, "AU AP Toz");
		    	put(EnumDailySummarySection.IR_AP_TOZ, "IR AP Toz");
		    	put(EnumDailySummarySection.OS_AP_TOZ, "OS AP Toz");
		    	put(EnumDailySummarySection.PD_AP_TOZ, "PD AP Toz");
		    	put(EnumDailySummarySection.PT_AP_TOZ, "PT AP Toz");
		    	put(EnumDailySummarySection.RH_AP_TOZ, "RH AP Toz");
		    	put(EnumDailySummarySection.RU_AP_TOZ, "RU AP Toz");
		        put(EnumDailySummarySection.TIER_1_AP_MARGIN_CALL, "AP margin call tier 1");
		        put(EnumDailySummarySection.TIER_2_AP_MARGIN_CALL, "AP margin call tier 2");
		        put(EnumDailySummarySection.TIER_1_AP_MARGIN_CALL_PERCENT, "AP margin call tier 1 %");
		        put(EnumDailySummarySection.TIER_2_AP_MARGIN_CALL_PERCENT, "AP margin call tier 2 %");
		    	put(EnumDailySummarySection.AG_DP_TOZ, "AG DP Toz");
		    	put(EnumDailySummarySection.AU_DP_TOZ, "AU DP Toz");
		    	put(EnumDailySummarySection.IR_DP_TOZ, "IR DP Toz");
		    	put(EnumDailySummarySection.OS_DP_TOZ, "OS DP Toz");
		    	put(EnumDailySummarySection.PD_DP_TOZ, "PD DP Toz");
		    	put(EnumDailySummarySection.PT_DP_TOZ, "PT DP Toz");
		    	put(EnumDailySummarySection.RH_DP_TOZ, "RH DP Toz");
		    	put(EnumDailySummarySection.RU_DP_TOZ, "RU DP Toz");
		    	put(EnumDailySummarySection.PT_DP_TOZ, "PT DP Toz");
		    	put(EnumDailySummarySection.RH_DP_TOZ, "RH DP Toz");
		    	put(EnumDailySummarySection.RU_DP_TOZ, "RU DP Toz");
		        put(EnumDailySummarySection.TIER_1_DP_MARGIN_CALL, "DP margin call tier 1");
		        put(EnumDailySummarySection.TIER_2_DP_MARGIN_CALL, "DP margin call tier 2");
		        put(EnumDailySummarySection.TIER_1_DP_MARGIN_CALL_PERCENT, "DP margin call tier 1 %");
		        put(EnumDailySummarySection.TIER_2_DP_MARGIN_CALL_PERCENT, "DP margin call tier 2 %");
		        put(EnumDailySummarySection.END_CASH_BALANCE, "End Cash  balance");
		    }});
	



	
	
	/**
	 * Execute.
	 *
	 * @param context the context
	 * @param table the table
	 * @return the table
	 */
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
			DailySummaryReport report = new DailySummaryReport(context);

			
			Date reportingDate = context.getBusinessDate();
			

			ApDpReportParameters parameters = new ApDpReportParameters(20007, -1, reportingDate);
				
			report.generateReport(parameters);
				
			ConstTable reprotData = report.getReportData();
				
			generateReportOutput((Table)reprotData, context);
				
			String displayResults = "true";
			displayResults = constRep.getStringValue("Display Results Data Table", displayResults);
			if(displayResults.compareToIgnoreCase("true") == 0) {
				context.getDebug().viewTable(reprotData);
			}
			
		} catch (Exception e) {
			PluginLog.error("Error running the advanced pricing daily summary report. " + e.getLocalizedMessage());
			throw new RuntimeException("Error running the advanced pricing daily summary report. " + e.getLocalizedMessage());
		}
		
		return context.getTableFactory().createTable("returnT");
	}
	
	/**
	 * Generate report output. Write the report data to an excel file inthe default reporting directory for today
	 *
	 * @param reprotData the  data to output
	 * @param context the script context
	 * @throws OException error on writing the excel file
	 */
	private void generateReportOutput(Table reprotData, Context context) throws OException {

		PluginLog.info("Writing data to file");
		
		Calendar cal = Calendar.getInstance();
		
		DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HHmmss");

		String fileName = context.getIOFactory().getReportDirectory() + "\\HK_AP_DP_Daily_Customer_Report_Summary_" +sdf.format(cal.getTime()) + ".xlsx";
		
		Table data = reprotData.getTable(0, 0);
		
		
		removeNonReportedMetals(data, context);
		
		setColumnNames(data);
		
		context.getTableFactory().toOpenJvs(data).excelSave(fileName);	
		PluginLog.info("Output written to file " + fileName);
	}
	

	/**
	 * Sets the column names to be used in the excel output.
	 *
	 * @param data the table to set the column names on
	 */
	private void setColumnNames(Table data) {
		
		for(EnumDailySummarySection column : EnumDailySummarySection.values()) {
				
			String name = COLUMN_NAME_MAP.get(column);
			
			if(name != null && name.length() > 0) {
				
				
				if(data.getColumnNames().contains(name)) {
					int columnId = data.getColumnId(column.getColumnName());
					data.setColumnName(columnId, name);
				}
			}
		}	
	}
	
	/**
	 * Removes the non reported metals.
	 *
	* @param data the table to remove non reported metals from
	 */
	private void removeNonReportedMetals(Table data, Context context) {
		ApprovedTradeLimit approvedTradeLimit = new ApprovedTradeLimit(context);
		
		approvedTradeLimit.removeNonReportedMetals(data);
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
