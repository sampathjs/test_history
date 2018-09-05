package com.matthey.openlink.pnl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.Str;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.openlink.util.logging.PluginLog;

/*
 *History: 
 * 201x-xx-xx	V1.0     	- initial version	
 * 2017-11-16	V1.1	lma	- execute more than constants repository variable minDealNum and succeed if there isn't any update
 *
 */
public class PNL_Backfill_Market_Data implements IScript
{
	private int m_firstResetDate = -1;
	private Table m_indexLoadTable = null;

	/**
	 * Run the script
	 */
	@Override
	public void execute(IContainerContext context) throws OException 
	{
		initPluginLog();
		
		// Retrieve all ComSwap transactions for which we'll be back-filling
		Table transactions = retrieveRelevantTransactions();		
		
		// Prepare transaction data
		prepareTransactionData(transactions);
		
		// Prepare market data for all deals
		Vector<PNL_MarketDataEntry> dataEntries = prepareMarketData(transactions, m_firstResetDate, OCalendar.today()-1);
		
		// Process the data - upload their market data to USER_jm_pnl_market_data
		processDataEntries(transactions, dataEntries);
	}

	/**
	 * Given a set of transactions and their current data entries, save only the non-existing ones to USER table 
	 * @param transactions
	 * @param dataEntries
	 * @throws OException
	 */
	private void processDataEntries(Table transactions, Vector<PNL_MarketDataEntry> dataEntries) throws OException 
	{	
		// Add all entries to DB
		PluginLog.info("PNL_Backfill_Market_Data found " + dataEntries.size() + " new entries. Inserting\n");
		OConsole.message("PNL_Backfill_Market_Data found " + dataEntries.size() + " new entries. Inserting\n");
		if(dataEntries.size() > 0) {  //Check if there is valid data to update, succeed if no data to update
			PNL_UserTableHandler.recordMarketData(dataEntries);		
		}	
	}

	/**
	 * Prepare the market data, loading historical close prices as required
	 * @param transData
	 * @param startDate - first date for which to check resets
	 * @param endDate - last date for which to check resets
	 * @return
	 * @throws OException
	 */
	private Vector<PNL_MarketDataEntry> prepareMarketData(Table transData, int startDate, int endDate) throws OException 
	{				
		PluginLog.info("PNL_Backfill_Market_Data.prepareMarketData from " + OCalendar.formatJd(startDate) + " to "+ OCalendar.formatJd(endDate) + ".\n");
		OConsole.message("PNL_Backfill_Market_Data.prepareMarketData from " + OCalendar.formatJd(startDate) + " to "+ OCalendar.formatJd(endDate) + ".\n");
		if (startDate > endDate)
		{
			PluginLog.info("PNL_Backfill_Market_Data: first reset date " + OCalendar.formatJd(startDate) + " is greater than yesterday. No action taken.\n");
			OConsole.message("PNL_Backfill_Market_Data: first reset date " + OCalendar.formatJd(startDate) + " is greater than yesterday. No action taken.\n");
			return new Vector<PNL_MarketDataEntry>();
		}		
		
		// Create a vector of dates of interest in the period between start and end date (inclusive)
		Vector<Integer> dates = new Vector<Integer>();		
		for (int date = startDate; date <= endDate; date++)
		{
			dates.add(date);
		}
		
		return prepareMarketData(transData, dates);
	}

	/**
	 * Prepare the market data, loading historical close prices as required
	 * @param transData
	 * @param startDate - first date for which to check resets
	 * @param endDate - last date for which to check resets
	 * @return
	 * @throws OException
	 */
	private Vector<PNL_MarketDataEntry> prepareMarketData(Table transData, Collection<Integer> dates) throws OException 
	{			
		Vector<PNL_MarketDataEntry> allDataEntries = new Vector<PNL_MarketDataEntry>();
		int today = OCalendar.today();
		
		try
		{
			// For each date, we have to move the script engine date to that day, then load closing prices
			for (int date : dates)
			{
				// Skip weekends
				int dayOfWeek = OCalendar.getDayOfWeek(date);
				if ((dayOfWeek == 6) || (dayOfWeek == 0))
				{
					continue;
				}
				
				PluginLog.info("PNL_Backfill_Market_Data.prepareMarketData - processing date: " + OCalendar.formatJd(date) + ".\n");
				OConsole.message("PNL_Backfill_Market_Data.prepareMarketData - processing date: " + OCalendar.formatJd(date) + ".\n");
						    			
    			try
    			{
    				Util.setCurrentDate(date);
    				Sim.loadCloseIndexList(m_indexLoadTable, 1, date);
    			}
    			catch (Exception e)
    			{
    				// Log and move on
    				PluginLog.error("PNL_Backfill_Market_Data.prepareMarketData - error: " + e.getMessage() + ".\n");
    				OConsole.message("PNL_Backfill_Market_Data.prepareMarketData - error: " + e.getMessage() + ".\n");
    			}    				
    			
    			Vector<PNL_MarketDataEntry> thisDateEntries = new Vector<PNL_MarketDataEntry>();
    			
    			// For each ComSwap, retrieve relevant fixings for that date, and add to overall list
    			for (int row = 1; row <= transData.getNumRows(); row++)
    			{
    				Transaction trn = transData.getTran("tran_ptr", row);
    				
    				Vector<PNL_MarketDataEntry> dataEntries = PNL_FixingsMarketDataRecorder.processDeal(trn);
    				
    				thisDateEntries.addAll(dataEntries);
    			}
    			
    			PluginLog.info("PNL_Backfill_Market_Data.prepareMarketData - found " + thisDateEntries.size() + " entries for " + OCalendar.formatJd(date) + ".\n");
    			OConsole.message("PNL_Backfill_Market_Data.prepareMarketData - found " + thisDateEntries.size() + " entries for " + OCalendar.formatJd(date) + ".\n");
    			
    			allDataEntries.addAll(thisDateEntries);
			}			
		}
		finally
		{
			Util.setCurrentDate(today);
			Sim.loadIndexList(m_indexLoadTable, 1);
		}
		
		return allDataEntries;
	}

	/**
	 * Prepare the list of transactions of interest, and initialise m_indexLoadTable from those
	 * @param transData
	 * @throws OException
	 */
	private void prepareTransactionData(Table transData) throws OException
	{
		PluginLog.info("PNL_Backfill_Market_Data.prepareTransactionData\n");
		OConsole.message("PNL_Backfill_Market_Data.prepareTransactionData\n");
		HashSet<Integer> indexesToLoad = new HashSet<Integer>();
		int fixedLeg = Ref.getValue(SHM_USR_TABLES_ENUM.FX_FLT_TABLE, "Fixed");
		int liborIndex = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, "LIBOR.USD");
		int usdCcy = Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "USD");
					
		m_firstResetDate = OCalendar.today();
		
		// Ensure we refresh LIBOR
		indexesToLoad.add(liborIndex);
		
		// Add all PX_ Metals indices, as this will force refresh prices on all child indexes that ComSwaps price off
		String[] metals = new String[] { "XAG", "XAU", "XIR", "XAG", "XOS", "XPD", "XPT", "XRH", "XRU" }; 
		String metalIndexFormat = "PX_<METAL>.USD";
		for (String metal : metals)
		{
			int metalIdx = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, metalIndexFormat.replace("<METAL>", metal));
			
			if (metalIdx > 0)
				indexesToLoad.add(metalIdx);
		}
		
		
		for (int row = 1; row <= transData.getNumRows(); row++)
		{
			Transaction trn = transData.getTran("tran_ptr", row);
			
	    	int numParams = trn.getNumRows(-1, TRANF_GROUP.TRANF_GROUP_PARM.toInt());
			for (int param = 0; param < numParams; param++)
			{
				int fxFlt = trn.getFieldInt(TRANF_FIELD.TRANF_FX_FLT.toInt(), param);

				// Skip the fixed (deliverable) swap leg, we only store resets from floating legs
				if (fxFlt == fixedLeg)
				{				
					continue;
				}
				
				// The earliest reset date will be the first one
				int firstLegResetDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_DATE.jvsValue(), param, "", 0);
				
				// Set the earliest reset date to this one, if it is smaller than the prior one found
				m_firstResetDate = Math.min(m_firstResetDate, firstLegResetDate);
				
				// Retrieve projection index - we will need to load its closing prices
				int projIdx = trn.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), param, "", 0, 0);				
				indexesToLoad.add(projIdx);
				
				// If projection index currency doesn't match leg currency, we will have to convert FX in JDE Extract, 
				// so need to load FX indexes too; if projection index currency is not USD, we have to convert FX for
				// JM Raw P&L Data result
				int legCcy = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), param);
				int idxPymtCcy = MTL_Position_Utilities.getPaymentCurrencyForIndex(projIdx);
				if ((idxPymtCcy != legCcy) || (idxPymtCcy != usdCcy))
				{
					indexesToLoad.add(MTL_Position_Utilities.getDefaultFXIndexForCcy(legCcy));
				}
			}
		}
		
		// We do not want to process any entries earlier than last month (i.e. if we're in June, from May onwards)
		// Some deals don't run on month-end basis (e.g. 171901), so push back one month further
		m_firstResetDate = Math.max(m_firstResetDate, OCalendar.parseString("1cd>-3lom"));

		// Set up m_indexLoadTable
		
		m_indexLoadTable = new Table("Indexes to Load");
		m_indexLoadTable.addCol("index", COL_TYPE_ENUM.COL_INT);
		m_indexLoadTable.addNumRows(indexesToLoad.size());
		int row = 1;
		for (Integer i : indexesToLoad)
		{
			m_indexLoadTable.setInt(1, row, i);
			row++;
		}
		
		// m_indexLoadTable.viewTable();
		PluginLog.info("PNL_Backfill_Market_Data.prepareTransactionData found " + m_indexLoadTable.getNumRows() + " indexes to load.\n");
		OConsole.message("PNL_Backfill_Market_Data.prepareTransactionData found " + m_indexLoadTable.getNumRows() + " indexes to load.\n");
	}

	/**
	 * Retrieve the list of relevant ComSwap transactions
	 * @return
	 * @throws OException
	 */
	private Table retrieveRelevantTransactions() throws OException
	{
		PluginLog.info("PNL_Backfill_Market_Data.retrieveRelevantTransactions\n");
		OConsole.message("PNL_Backfill_Market_Data.retrieveRelevantTransactions\n");
		
		String strMinDealNum = ConfigurationItemPnl.MIN_DEAL_NUM.getValue();

		int minimalDealNum = 0;
		try {
			minimalDealNum = Str.strToInt(strMinDealNum);
		} catch (OException e1) {
			e1.printStackTrace();
		}
		
		Table transData = new Table("Transactions");
		String sql =
				"SELECT ab.deal_tracking_num deal_num, ab.tran_num " +
				"FROM ab_tran ab " +
				"WHERE ab.toolset = 15 AND ab.tran_status in (2,3) AND ab.current_flag = 1"
				+ " AND deal_tracking_num >= " + minimalDealNum;
		
		DBase.runSqlFillTable(sql, transData);
		
		transData.addCol("tran_ptr", COL_TYPE_ENUM.COL_TRAN);
		
		for (int row = 1; row <= transData.getNumRows(); row++)
		{
			int tranNum = transData.getInt("tran_num", row);
			Transaction trn = Transaction.retrieve(tranNum);
			
			transData.setTran("tran_ptr", row, trn);
		}
		
		return transData;
	}

	public void process(Transaction trn, HashSet<Integer> dates) throws OException 
	{
		Table transData = new Table("Trans Data");
		
		transData.addCol("tran_ptr", COL_TYPE_ENUM.COL_TRAN);
		transData.addRow();
		transData.setTran("tran_ptr", 1, trn);	
		
		// Prepare a single transaction's data
		prepareTransactionData(transData);
		
		// Prepare market data for all deals
		Vector<PNL_MarketDataEntry> dataEntries = prepareMarketData(transData, dates);
		
		// Process the data - upload the market data to USER_jm_pnl_market_data
		processDataEntries(transData, dataEntries);		
	}
	
	/**
	 * Initialise standard Plugin log functionality
	 * @throws OException
	 */
	private void initPluginLog() throws OException 
	{	
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");
		String logLevel = ConfigurationItemPnl.LOG_LEVEL.getValue();
		String logFile = ConfigurationItemPnl.LOG_FILE.getValue();
		String logDir = ConfigurationItemPnl.LOG_DIR.getValue();
		if (logDir.trim().isEmpty()) 
		{
			logDir = abOutdir + "\\error_logs";
		}
		if (logFile.trim().isEmpty()) 
		{
			logFile = this.getClass().getName() + ".log";
		}
		try 
		{
			PluginLog.init(logLevel, logDir, logFile);
		} 
		catch (Exception e) 
		{
			throw new RuntimeException (e);
		}
		PluginLog.info("Plugin: " + this.getClass().getName() + " started.\r\n");
	}
	
}
