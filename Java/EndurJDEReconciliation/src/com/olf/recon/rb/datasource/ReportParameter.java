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
		/*
		 * The below custom parameters are used for EndurJDE Reconciliation - Endur ML Extract
		 */
		public static final String INCLUDE_INTERNAL_BUNIT = "include_internal_bunit";
		public static final String INCLUDE_HOLDING_BANK = "include_holding_bank";
		public static final String EXCLUDE_INSTRUMENT_TYPE = "exclude_instrument_type";
		
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
	
	/**
	 * Return the internal business units if set
	 * 
	 * @return
	 * @throws OException
	 */
	public HashSet<Integer> getIncludedInternalBunit() throws OException
	{
		/* Empty by default */
		HashSet<Integer> set = new HashSet<Integer>();
		
		int relevantRow = getRow(CustomParam.INCLUDE_INTERNAL_BUNIT);
		if (relevantRow > 0)
		{
			String includedBunits = tblParam.getString(2, relevantRow);
			
			if (includedBunits != null && !includedBunits.equalsIgnoreCase(""))
			
			try
			{
				String split[] = includedBunits.split(",");
					
				int internal_bunits = split.length;
				for (int i = 0 ; i < internal_bunits; i++)
				{
					String internal_bunit = split[i].trim();
					int internal_bunit_id = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, internal_bunit);
					
					set.add(internal_bunit_id);
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
	 * Return the holding banks if set
	 * 
	 * @return
	 * @throws OException
	 */
	public HashSet<Integer> getIncludedHoldinBank() throws OException
	{
		/* Empty by default */
		HashSet<Integer> set = new HashSet<Integer>();
		
		int relevantRow = getRow(CustomParam.INCLUDE_HOLDING_BANK);
		if (relevantRow > 0)
		{
			String includedHoldingBanks = tblParam.getString(2, relevantRow);
			
			if (includedHoldingBanks != null && !includedHoldingBanks.equalsIgnoreCase(""))
			
			try
			{
				String split[] = includedHoldingBanks.split(",");
					
				int holding_banks = split.length;
				for (int i = 0 ; i < holding_banks; i++)
				{
					String holding_bank = split[i].trim();
					int holding_banks_id = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, holding_bank);
					
					set.add(holding_banks_id);
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
	 * Return the instrument type if set
	 * 
	 * @return
	 * @throws OException
	 */
	public HashSet<Integer> getExcludedInstrumentType() throws OException
	{
		/* Empty by default */
		HashSet<Integer> set = new HashSet<Integer>();
		
		int relevantRow = getRow(CustomParam.EXCLUDE_INSTRUMENT_TYPE);
		if (relevantRow > 0)
		{
			String excludedInstumentType = tblParam.getString(2, relevantRow);
			
			if (excludedInstumentType != null && !excludedInstumentType.equalsIgnoreCase(""))
			
			try
			{
				String split[] = excludedInstumentType.split(",");
					
				int ins_types = split.length;
				for (int i = 0 ; i < ins_types; i++)
				{
					String ins_type = split[i].trim();
					int ins_type_id = Ref.getValue(SHM_USR_TABLES_ENUM.INS_TYPE_TABLE, ins_type);
					
					set.add(ins_type_id);
				}
			}
			catch (Exception e)
			{
				/* Fail safe */
			}			
		}
		
		return set;
	}
}
