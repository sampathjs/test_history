package com.olf.recon.rb.datasource;

import java.util.HashSet;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Report builder parameters
 */
public class ReportParameter
{
	/* This will be loaded in the constructor */
	private Table tblParam = Util.NULL_TABLE;
	
	public class CustomParam 
	{
		/* 
		 * These are the current Report Builder custom parameters we define
		 * in the Parameter tab of reports.
		 */
		public static final String WINDOW_START_DATE = "window_start_date";
		public static final String WINDOW_END_DATE = "window_end_date";
		public static final String EXCLUDE_COUNTERPARTY = "exclude_counterparty";
		public static final String INCLUDE_INTERNAL_LENTITY = "include_internal_lentity";
		public static final String EXCLUDE_IF_EXT_BUNIT_PARTY_INFO_EMPTY = "exclude_if_ext_bunit_party_info_empty";	
		public static final String REGION = "region";
		public static final String LAST_TRADE_DATE = "last_trade_date";
	}
	
	/**
	 * Constructor 
	 * 
	 * @param tblArgt - argt of a report builder datasource plugin
	 * @throws OException
	 */
	public ReportParameter(Table tblArgt) throws OException
	{ 
		if (tblArgt.getNumRows() > 0)
		{        	        	        	
			tblParam = tblArgt.getTable("PluginParameters", 1);
		}
	}
	
	/**
	 * Find the param row  
	 * 
	 * @param customParam
	 * @return
	 * @throws OException
	 */
	private int getRow(String customParam) throws OException
	{
		int findRow = -1;
		
		findRow = tblParam.unsortedFindString(1, customParam, SEARCH_CASE_ENUM.CASE_INSENSITIVE);        
		
		return findRow;
	}
		
	/**
	 * Get window start date
	 * 
	 * @return - julian run date 
	 * @throws OException 
	 */
	public int getWindowStartDate() throws OException
	{
		int runDate = -1;
		
		int runDateRow = getRow(CustomParam.WINDOW_START_DATE);
		if (runDateRow > 0)
		{
			String runDateStr = tblParam.getString(2, runDateRow);
			int runDateInt = OCalendar.parseString(runDateStr);

			if (runDateInt > 0)
			{
				runDate = runDateInt;

				PluginLog.info("RunDate will be " + OCalendar.formatJd(runDate) + ".\r\n");
			}
		}
		
		return runDate;
	}
	
	/**
	 * Get window start date
	 * 
	 * @return - julian run date 
	 * @throws OException 
	 */
	public int getWindowEndDate() throws OException
	{
		int runDate = -1;
		
		int runDateRow = getRow(CustomParam.WINDOW_END_DATE);
		if (runDateRow > 0)
		{
			String runDateStr = tblParam.getString(2, runDateRow);
			int runDateInt = OCalendar.parseString(runDateStr); 

			if (runDateInt > 0)
			{
				runDate = runDateInt;

				PluginLog.info("RunDate will be " + OCalendar.formatJd(runDate) + ".\r\n");
			}
		}
		
		return runDate;
	}
	
	public int getLastTradeDate() throws OException
	{
		int runDate = -1;
		
		int runDateRow = getRow(CustomParam.LAST_TRADE_DATE);
		if (runDateRow > 0)
		{
			String runDateStr = tblParam.getString(2, runDateRow);
			int runDateInt = OCalendar.parseString(runDateStr);

			if (runDateInt > 0)
			{
				runDate = runDateInt;

				PluginLog.info("RunDate will be " + OCalendar.formatJd(runDate) + ".\r\n");
			}
		}
		
		return runDate;
	}
	
	
	
	
	/**
	 * Get a list of excluded counterparties, if set
	 * 
	 * @return
	 * @throws OException
	 */
	public HashSet<Integer> getExcludedCounterparties() throws OException
	{
		/* Empty by default */
		HashSet<Integer> set = new HashSet<Integer>();
		
		int relevantRow = getRow(CustomParam.EXCLUDE_COUNTERPARTY);
		if (relevantRow > 0)
		{
			String excludedCounterparties = tblParam.getString(2, relevantRow);
			
			if (excludedCounterparties != null && !excludedCounterparties.equalsIgnoreCase(""))
			{
				try
				{
					String split[] = excludedCounterparties.split(",");

					int bunits = split.length;
					for (int i = 0 ; i < bunits; i++)
					{
						String bunit = split[i].trim();
						
						int bunitId = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, bunit);

						set.add(bunitId);
					}
				}
				catch (Exception e)
				{
					/* Fail-safe */
				}
			}
		}
		
		return set;
	}
	
	/**
	 * Get a list of the included lentities, if set 
	 * 
	 * @return
	 * @throws OException
	 */
	public HashSet<Integer> getIncludedInternalLentities() throws OException
	{
		/* Empty by default */
		HashSet<Integer> set = new HashSet<Integer>();
		
		int relevantRow = getRow(CustomParam.INCLUDE_INTERNAL_LENTITY);
		if (relevantRow > 0)
		{
			String excludedCounterparties = tblParam.getString(2, relevantRow);
			
			if (excludedCounterparties != null && !excludedCounterparties.equalsIgnoreCase(""))
			
			try
			{
				String split[] = excludedCounterparties.split(",");
					
				int lentities = split.length;
				for (int i = 0 ; i < lentities; i++)
				{
					String lentity = split[i].trim();
					int lentityId = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, lentity);
					
					set.add(lentityId);
				}
			}
			catch (Exception e)
			{
				/* Fail safe */
			}			
		}
		
		return set;
	}
	
	/**
	 * Get external bunit party info exclusion if exists
	 * 
	 * @return
	 * @throws OException
	 */
	public String getExternalBunitPartyInfoExclusion() throws OException
	{
		String partyInfo = "";
		
		int findRow = getRow(CustomParam.EXCLUDE_IF_EXT_BUNIT_PARTY_INFO_EMPTY);
		if (findRow > 0)
		{
			partyInfo = tblParam.getString(2, findRow);
		}
		
		return partyInfo;
	}

	/**
	 * Return the region associated with the definition
	 * 
	 * @return
	 * @throws OException
	 */
  
	public String getRegion() throws OException
	{
		String region = "";

		int findRow = getRow(CustomParam.REGION);
		if (findRow > 0)
		{
			region = tblParam.getString(2, findRow);
		}
		
		return region;
	}
}
