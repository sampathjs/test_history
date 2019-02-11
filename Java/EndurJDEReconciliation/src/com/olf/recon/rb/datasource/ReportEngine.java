package com.olf.recon.rb.datasource;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Vector;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.ReconConfig;
import com.olf.recon.utils.Util;
import com.openlink.util.logging.PluginLog;

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

	/* Made these protected, in-case sub class needs them for anything */
	protected int windowStartDate;
	protected int windowEndDate;
	protected int lastTradeDate;
	protected String windowStartDateStr;
	protected String windowEndDateStr;
	protected String lastTradeDateStr;
	protected HashSet<Integer> excludedCounterparties;
	protected HashSet<Integer> includedLentites;
	protected String exclusionExternalBunitPartyInfo;
	protected String region;
	
	protected ODateTime extractDateTime;
	protected ReconConfig constRepoConfig;
	
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();
		
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
		int defaultLastTradeDate = OCalendar.today()-1;
		extractDateTime = ODateTime.getServerCurrentDateTime();

		/* Get custom report params where provided */
		ReportParameter rp = new ReportParameter(argt);
		windowStartDate = rp.getWindowStartDate(); 
		windowEndDate = rp.getWindowEndDate();
		lastTradeDate = rp.getLastTradeDate();
		excludedCounterparties = rp.getExcludedCounterparties();
		includedLentites = rp.getIncludedInternalLentities();
		exclusionExternalBunitPartyInfo = rp.getExternalBunitPartyInfoExclusion();
		
		region = rp.getRegion();
		if (region == null || "".equalsIgnoreCase(region))
		{
			throw new ReconciliationRuntimeException("No region parameter specified!");
		}
		
		constRepoConfig = new ReconConfig(rp.getRegion());
		 
		/* Override with custom param values where applicable */
		windowStartDate = (windowStartDate != -1) ? windowStartDate : runDate;
		windowEndDate = (windowEndDate != -1) ? windowEndDate : runDate;
		lastTradeDate = (lastTradeDate !=-1)? lastTradeDate : defaultLastTradeDate;
		windowStartDateStr = OCalendar.formatJd(windowStartDate);
		windowEndDateStr = OCalendar.formatJd(windowEndDate);
		
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
		Util.initialiseLog();;
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
	 * Parse Endur date format into JDE date format
	 * 
	 * @param date
	 * @return
	 */
	protected String getDateEndurToJDEFormat(String date)
	{
		String ret = "";
		
		try 
		{
			java.util.Date javaDate = Constants.endurDateFormat.parse(date);
			ret = Constants.jdeStoredProcDateFormat.format(javaDate);
		} 
		catch (ParseException e)
		{
			PluginLog.error("Unable to parse " + date + " into JDE format");
		}
		
		return ret;
	}
	
	/**
	 * Parse JDE date format into Endur date format
	 * 
	 * @param date
	 * @return
	 */
	protected String getDateJdeToEndurFormat(String date)
	{
		String ret = "";
		
		try 
		{
			java.util.Date javaDate = Constants.jdeInboundDateFormat.parse(date);
			ret = Constants.endurDateFormat.format(javaDate);
		} 
		catch (ParseException e)
		{
			PluginLog.error("Unable to parse " + date + " into Endur format");
		}
		
		return ret;
	}
	
	/**
	 * Add custom exception notes to reconciliation output 
	 * 
	 * This is so that records with known defects can be flagged easily to end users.
	 * 
	 * The break notes are stored in user_jm_invoice_rec_notes or user_jm_deal_rec_notes
	 * 
	 * @param tblOutput
	 * @param invoiceNumberColName
	 * @throws OException
	 */
	protected void addReconciliationNotes(Table tblOutput, String userTableName, String userTableKeyColName, String sourceKeyColName) throws OException
	{
		Table tblTemp = Table.tableNew(userTableName);
		int ret = DBUserTable.load(tblTemp);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new ReconciliationRuntimeException("Unable to load table: " + userTableName);
		}
		
		if (tblTemp.getNumRows() > 0 && tblOutput.getNumRows() > 0)
		{
			tblOutput.select(tblTemp, "break_description(reconciliation_note)", userTableKeyColName + " EQ $" + sourceKeyColName);			
		}

		tblTemp.destroy();
	}
	
	/**
	 * Filter out counterparties from data table
	 * 
	 * @param tblData
	 * @param excludedCounterparties
	 * @throws OException
	 */
	protected void filterCounterparties(Table tblData, HashSet<Integer> excludedCounterparties) throws OException
	{
		String bunitColName = "external_bunit";
		
		if (tblData.getColNum("counterparty") > 0)
		{
			bunitColName = "counterparty";
		}
		
		for (int row = tblData.getNumRows(); row >= 1; row--)
		{
			int bunit = tblData.getInt(bunitColName, row);
			
			if (excludedCounterparties.contains(bunit))
			{
				tblData.delRow(row);	
			}
		}	
	}
	
	/**
	 * Filter out included lentities from data table
	 * 
	 * @param tblData
	 * @param excludedCounterparties
	 * @throws OException
	 */
	protected void filterIncludedLentites(Table tblData, HashSet<Integer> includedLentites) throws OException
	{
		for (int row = tblData.getNumRows(); row >= 1; row--)
		{
			int internalLentity = tblData.getInt("internal_lentity", row);
			
			if (!includedLentites.contains(internalLentity))
			{
				tblData.delRow(row);	
			}
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
}
