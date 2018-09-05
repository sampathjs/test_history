package com.jm.accountingfeed.rb.datasource;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.jm.accountingfeed.util.ConstRepoConfig;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Super class for running a Report Builder datasource plugin
 * 
 * This class provides placeholder functions for 
 * 1. Collecting the metadata of a report
 * 2. Running the report
 * 3. Formatting the report
 */
public abstract class ReportEngine implements IScript
{
	protected class RefConversionData 
	{
		String m_colName;
		SHM_USR_TABLES_ENUM m_refID;
	}

	protected enum DateConversionType
	{
		TYPE_DMY,
		TYPE_MY,
		TYPE_QY
	}

	protected class DateConversionData
	{
		String m_colName;
		DateConversionType m_type;
	}

	protected class TableConversionData
	{
		String m_colName;
		String m_tableQuery;		
	}

	protected Vector<RefConversionData> m_refConversions = new Vector<RefConversionData>();
	protected Vector<DateConversionData> m_dateConversions = new Vector<DateConversionData>();
	protected Vector<TableConversionData> m_tableConversions = new Vector<TableConversionData>();
	
	protected ODateTime extractDateTime;
	protected ConstRepoConfig constRepoConfig = new ConstRepoConfig();
	protected ReportParameter rp;
	
	protected Table returnt;
	
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		returnt = context.getReturnTable();
		
		com.jm.accountingfeed.util.Util.setupLog();
		
		int mode = argt.getInt("ModeFlag", 1);
		
		/* Meta data collection */
		if (mode == 0) 
		{
			setOutputFormat(returnt);
			
			registerConversions(returnt);
			
			setStandardOutputFieldsFormat(returnt);
			registerStandardConversions(returnt);
			
			performConversions(returnt);
			
			return;
		}
		
		/* Set default values */
		int runDate = OCalendar.today();   
		extractDateTime = ODateTime.getServerCurrentDateTime();

		rp = new ReportParameter(argt.getTable("PluginParameters", 1));
		
		initialise();
		
		/* Add child class report fields */
		setOutputFormat(returnt);
		registerConversions(returnt);

		generateOutput(returnt);
		
		/* Add standard report fields */
		setStandardOutputFieldsFormat(returnt);
		registerStandardConversions(returnt);

		/* Set standard fields for all outputs */
		populateStandardFields(returnt, runDate, extractDateTime);

		/* Finally, do the conversions */
		performConversions(returnt);
		
		formatOutputData(returnt);
		groupOutputData(returnt);
	}

	/**
	 * Placeholder for any *initialise* specific functionality
	 *  
	 * @param output
	 * @throws OException
	 */
	protected void initialise() throws OException
	{
		com.jm.accountingfeed.util.Util.setupLog(constRepoConfig);
	}
	
	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	protected void setupLog() throws OException
	{
		String abOutDir =  SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs\\";
		
		String logDir = constRepoConfig.getValue("logDir", abOutDir);
		String logLevel = constRepoConfig.getValue("logLevel", "INFO");
		String logFile = constRepoConfig.getValue("logFile", "EndurAccountingFeedInterface.log");

        try
        {
        	if (logDir.trim().equals("")) 
        	{
        		PluginLog.init(logLevel);
        	}
        	else  
        	{
        		PluginLog.init(logLevel, logDir, logFile);
        	}
        } 
		catch (Exception e) 
		{
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
		
		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}
	
	/**
	 * Create custom report columns 
	 *  
	 * @param output
	 * @throws OException
	 */
	protected abstract void setOutputFormat(Table output) throws OException;   
	
	/**
	 * Generate the actual output
	 * 
	 * @param output
	 * @return
	 * @throws OException
	 */
	protected abstract Table generateOutput(Table output) throws OException;

	/**
	 * Register any ref-format column conversions
	 * 
	 * @param output
	 * @throws OException
	 */
	protected abstract void registerConversions(Table output) throws OException;   

	/**
	 * Format the output data
	 * 
	 * @param output
	 * @throws OException
	 */
	protected abstract void formatOutputData(Table output) throws OException;
	
	/**
	 * Group the output data
	 * 
	 * @param output
	 * @throws OException
	 */
	protected abstract void groupOutputData(Table output) throws OException;
	
	/**
	 * Add any standard output fields to the output. All reports will have these.
	 * 
	 * @param output
	 * @throws OException
	 */
	protected void setStandardOutputFieldsFormat(Table output) throws OException
	{   
		output.addCol("reporting_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("extract_datetime", COL_TYPE_ENUM.COL_DATE_TIME);
	}

	/**
	 * Register any conversions for standard columns
	 * 
	 * @param output
	 * @throws OException
	 */
	protected void registerStandardConversions(Table output) throws OException
	{   
		regDateConversion(output, "reporting_date");   	
	}
	
	/**
	 * Populate standard fields - all reports will have these.
	 * 
	 * @param output
	 * @param runDate
	 * @param extractDateTime
	 * @param reportingDesk
	 * @throws OException
	 */
	protected void populateStandardFields(Table output, int runDate, ODateTime extractDateTime) throws OException
	{    	
		output.setColValInt("reporting_date", runDate);
		output.setColValDateTime("extract_datetime", extractDateTime);
	}
	
	/**
	 * Register a RefID-type data conversion for the final output table
	 *
	 * @param dataTable
	 * @param colName
	 * @param refID
	 * @throws OException
	 */
	protected void regRefConversion(Table dataTable, String colName, SHM_USR_TABLES_ENUM refID) throws OException
	{    	
		RefConversionData convData = new RefConversionData();

		dataTable.addCol(colName + "_str", COL_TYPE_ENUM.COL_STRING);

		convData.m_colName = colName;
		convData.m_refID = refID;

		m_refConversions.add(convData);
	}

	/**
	 * Register the need for date formatting on given column for the final output table
	 *
	 * @param dataTable
	 * @param colName
	 * @throws OException
	 */
	protected void regDateConversion(Table dataTable, String colName) throws OException
	{
		/* If no parameter passed in, assume DMY */
		regDateConversion(dataTable, colName, DateConversionType.TYPE_DMY);

	}

	/**
	 * Register the need for date formatting on given column for the final output table
	 *
	 * @param dataTable
	 * @param colName
	 * @param type
	 * @throws OException
	 */
	protected void regDateConversion(Table dataTable, String colName, DateConversionType type) throws OException
	{ 
		DateConversionData convData = new DateConversionData();

		dataTable.addCol(colName + "_str", COL_TYPE_ENUM.COL_STRING);

		convData.m_colName = colName;
		convData.m_type = type;

		m_dateConversions.add(convData);
	}
	
	/**
	 * Register a Table-type data conversion for the final output table
	 *
	 * @param dataTable
	 * @param colName
	 * @param tableQuery
	 * @throws OException
	 */
	protected void regTableConversion(Table dataTable, String colName, String tableQuery) throws OException
	{    	
		TableConversionData convData = new TableConversionData();

		dataTable.addCol(colName + "_str", COL_TYPE_ENUM.COL_STRING);

		convData.m_colName = colName;
		convData.m_tableQuery = tableQuery;

		m_tableConversions.add(convData);
	}
	
	/**
	 * Perform data type conversions on the final output table according to registered requirements	 
	 *
	 * @param output
	 * @throws OException
	 */
	protected void performConversions(Table output) throws OException
	{
		for (RefConversionData conv : m_refConversions)
		{
			output.copyColFromRef(conv.m_colName, conv.m_colName + "_str", conv.m_refID);
			output.setColName(conv.m_colName, "orig_" + conv.m_colName);
			output.setColName(conv.m_colName + "_str", conv.m_colName);
		}

		for (DateConversionData conv : m_dateConversions)
		{
			switch (conv.m_type)
			{
			case TYPE_DMY:
				output.copyColFormatDate(conv.m_colName, conv.m_colName + "_str", 
						DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_EUROPE);
				break;
			case TYPE_MY:
				for (int row = 1; row <= output.getNumRows();row++)
				{
					int jd = output.getInt(conv.m_colName, row);
					String monthStr = OCalendar.getMonthStr(jd) + "-" + OCalendar.getYear(jd);
					output.setString(conv.m_colName + "_str", row, monthStr);
				}    				  				
				break;
			case TYPE_QY:
				output.copyColFormatDate(conv.m_colName, conv.m_colName + "_str", 
						DATE_FORMAT.DATE_FORMAT_QUARTER_YEAR, DATE_LOCALE.DATE_LOCALE_EUROPE);      				
				break;    		
			}
			output.setColName(conv.m_colName, "orig_" + conv.m_colName);
			output.setColName(conv.m_colName + "_str", conv.m_colName);     		
		}

		for (TableConversionData conv : m_tableConversions)
		{
			Table convTable = Table.tableNew();
			DBaseTable.execISql(convTable, conv.m_tableQuery);

			// Force the column names, just in case
			convTable.setColName(1, "id");
			convTable.setColName(2, "name");

			output.select(convTable, "name(" + conv.m_colName + "_str" + ")", "id EQ $" + conv.m_colName); 

			output.setColName(conv.m_colName, "orig_" + conv.m_colName);
			output.setColName(conv.m_colName + "_str", conv.m_colName);
		}    	
	}
	
	/**
	 * Remove rows from "tblSource", which already exist in "tblDestination" - based on the key column.
	 * 
	 * @param tblSource
	 * @param tblDestination
	 * @param KeyColumn
	 * @throws OException
	 */
	protected void removeRowsWhereExist(Table tblSource, Table tblDestination, String keyColumn) throws OException
	{
		if (tblSource.getNumRows() > 0)
		{
			/* Get a distinct list of records based on the key, and a flag = 1 against all of these rows */
			Table tblDistinct = Table.tableNew();
			tblDistinct.select(tblDestination, "DISTINCT, " + keyColumn, keyColumn + " GT -1");
			tblDistinct.addCol("flag", COL_TYPE_ENUM.COL_INT);
			tblDistinct.setColValInt("flag", 1);
			
			/* Select "flag" into "tblSource" based on key-match, and then remove rows where flag == 1 */
			tblSource.select(tblDistinct, "flag", keyColumn + " EQ $" + keyColumn);
			tblSource.deleteWhereValue("flag", 1);
			tblSource.delCol("flag");	
			
			/* Clean up */
			tblDistinct.destroy();	
		}
	}
	
	/**
	 * Build a table.select WHERE clause based on the keys passed in
	 * 
	 * @param distinctKeys - csv string containg key columns
	 * @return
	 */
	protected String getTableWhereClause(String distinctKeys)
	{
		String whereClauseKeys[]  = distinctKeys.split(","); 
		List<String> whereKeyColumns = Arrays.asList(whereClauseKeys);
		
		StringBuilder builder = new StringBuilder();
		for (String col : whereKeyColumns)
		{
			if (!builder.toString().equalsIgnoreCase(""))
			{
				builder.append(" AND ");
			}

			String column = col.trim();
			
			builder.append(column);
			builder.append(" EQ $");
			builder.append(column);
		}
		
		return builder.toString();
	}
}