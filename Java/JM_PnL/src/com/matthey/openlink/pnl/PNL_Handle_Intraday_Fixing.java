package com.matthey.openlink.pnl;

import java.util.HashSet;
import java.util.Vector;

import com.matthey.openlink.jde_extract.JDE_Data_Manager;
import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 */

/**
 * This class runs a script that will check, for all ComSwap, if, for today's reset, a historical price is available, and if so,
 * will create an entry for that reset in the USER_JM_PNL_MARKET_DATA table and USER_JM_JDE_EXTRACT table.
 *
 */
public class PNL_Handle_Intraday_Fixing implements IScript {
	
	private HashSet<PNL_EntryDataUniqueID> m_knownResets = new HashSet<PNL_EntryDataUniqueID>();
	
	/**
	 * Run the script
	 */
	@Override
	public void execute(IContainerContext context) throws OException {
		initPluginLog();
		
		// Retrieve all ComSwap transactions for which we may need to store the intraday fixing data
		Table transactions = retrieveRelevantTransactions();		
		
		// Prepare transaction data
		prepareTransactionData(transactions);
		
		// Prepare data from USER_JM_PNL_MARKET_DATA
		prepareUserTableData(transactions);
		
		// Prepare market data for all deals
		Vector<PNL_MarketDataEntry> dataEntries = prepareMarketData(transactions);
		
		// Process the data - upload their market data to USER_jm_pnl_market_data and USER_JM_JDE_EXTRACT
		processDataEntries(transactions, dataEntries);
		
		// Clear transaction pointers
		clearEntries(transactions);
		Logging.close();
	}

	private void clearEntries(Table transactions) throws OException {
		int trans = transactions.getNumRows();
		for (int row = 1; row <= trans; row++) {
			Transaction trn = transactions.getTran("tran_ptr", row);
			if (Transaction.isNull(trn) != 1) {
				trn.destroy();
			}
		}
	}
	
	private void prepareUserTableData(Table transactions) throws OException {
		// Retrieve all resets for which we have already stored information in USER_JM_PNL_MARKET_DATA
		Vector<PNL_MarketDataEntry> knownResets = new PNL_UserTableHandler().retrieveMarketData(transactions);		
		for (PNL_MarketDataEntry entry : knownResets) {
			m_knownResets.add(entry.m_uniqueID);
		}		
	}	

	/**
	 * Given a set of transactions and their current data entries, save only the non-existing ones to USER table 
	 * @param transactions
	 * @param dataEntries
	 * @throws OException
	 */
	private void processDataEntries(Table transactions, Vector<PNL_MarketDataEntry> dataEntries) throws OException {	
		// Add all entries to DB
		Logging.info("PNL_Handle_Intraday_Fixing found " + dataEntries.size() + " new entries. Inserting\n");
		
		// Insert into pnl_market_data table
		new PNL_UserTableHandler().recordMarketData(dataEntries);
		
		// Insert into jde_extract table
		JDE_Data_Manager dataManager = new JDE_Data_Manager();
		dataManager.processDeals(transactions);
	}

	/**
	 * Prepare the market data for today
	 * @param transData
	 * @return
	 * @throws OException
	 */
	private Vector<PNL_MarketDataEntry> prepareMarketData(Table transData) throws OException {
		Vector<PNL_MarketDataEntry> allDataEntries = new Vector<PNL_MarketDataEntry>();		
		
		// Refresh historical prices, as this script is run after the historical prices have been loaded
		MTL_Position_Utilities.refreshHistPrices();
		
		try {
			// For each ComSwap, retrieve relevant fixings for that date, and add to overall list
			for (int row = 1; row <= transData.getNumRows(); row++) {
				Transaction trn = transData.getTran("tran_ptr", row);
				Vector<PNL_MarketDataEntry> dataEntries = PNL_FixingsMarketDataRecorder.processDeal(trn);
				
				// For each data entry, let's check -
				//		Does its relevant historical price already exist?
				//		Is there no entry for it in the user-jm-pnl-market-data table?
				
				for (PNL_MarketDataEntry entry : dataEntries) {
					boolean shouldAdd = true;
					
					// Check if this already has an entry in USER_JM_PNL_MARKET_DATA
					// If yes, we do not need to insert it
					if (m_knownResets.contains(entry.m_uniqueID)) {
						shouldAdd = false;
					}
					
					// Retrieve the relevant parameters from the deal-leg-reset to see if it has a known historical price 
					int leg = entry.m_uniqueID.m_dealLeg;
					int resetID = entry.m_uniqueID.m_dealReset >= PNL_FixingsMarketDataRecorder.S_FX_RESET_OFFSET ? 
							entry.m_uniqueID.m_dealReset - PNL_FixingsMarketDataRecorder.S_FX_RESET_OFFSET : 
							entry.m_uniqueID.m_dealReset;
					
					int projIdx = trn.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), leg);
					int resetDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_DATE.toInt(), leg, "", resetID);
					int rfisDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_RFIS_DATE.toInt(), leg, "", resetID);
					int refSource = trn.getFieldInt(TRANF_FIELD.TRANF_REF_SOURCE.toInt(), leg);
					
					// Check if this reset has an entry in historical prices; if no, we should not store its data
					if (!MTL_Position_Utilities.hasHistPrice(projIdx, resetDate, rfisDate, refSource)) {
						shouldAdd = false;
					}
					
					// If we are still OK to add, do so
					if (shouldAdd) {
						allDataEntries.add(entry);
					}
				}						
			}	
		} catch (Exception e) {
			String msg = "PNL_Handle_Intraday_Fixing.prepareMarketData threw exception: " + e.getMessage() + "\n";
			Logging.info(msg);
		}
		
		String msg = "PNL_Handle_Intraday_Fixing.prepareMarketData found " + allDataEntries.size() + " entries to process.\n";
		Logging.info(msg);
		
		return allDataEntries;
	}

	/**
	 * Prepare the list of transactions of interest, and initialise m_indexLoadTable from those
	 * @param transData
	 * @throws OException
	 */
	private void prepareTransactionData(Table transData) throws OException {
		Logging.info("PNL_Handle_Intraday_Fixing.prepareTransactionData\n");
		HashSet<Integer> indexesToLoad = new HashSet<Integer>();
		int fixedLeg = Ref.getValue(SHM_USR_TABLES_ENUM.FX_FLT_TABLE, "Fixed");
		int liborIndex = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, "LIBOR.USD");
					
		int today = OCalendar.today();
		
		// Ensure we refresh LIBOR
		indexesToLoad.add(liborIndex);
		
		// Add all PX_ Metals indices, as this will force refresh prices on all child indexes that ComSwaps price off
		String[] metals = new String[] { "XAG", "XAU", "XIR", "XAG", "XOS", "XPD", "XPT", "XRH", "XRU" }; 
		String metalIndexFormat = "PX_<METAL>.USD";
		for (String metal : metals) {
			int metalIdx = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, metalIndexFormat.replace("<METAL>", metal));
			
			if (metalIdx > 0)
				indexesToLoad.add(metalIdx);
		}		
		
		int rows = transData.getNumRows();
		for (int row = rows; row >= 1; row--) {
			Transaction trn = transData.getTran("tran_ptr", row);
			boolean bTodayResetFound = false;
			
	    	int numParams = trn.getNumRows(-1, TRANF_GROUP.TRANF_GROUP_PARM.toInt());
			for (int param = 0; param < numParams; param++) {
				int fxFlt = trn.getFieldInt(TRANF_FIELD.TRANF_FX_FLT.toInt(), param);

				// Skip the fixed (deliverable) swap leg, we only store resets from floating legs
				if (fxFlt == fixedLeg) {				
					continue;
				}
				
				int totalResetPeriods = trn.getNumRows(param, TRANF_GROUP.TRANF_GROUP_RESET.toInt());
				for (int reset = 0; reset < totalResetPeriods; reset++) {
					int blockEnd = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_BLOCK_END.toInt(), param, "", reset);
					
					// Skip block-end resets
					if (blockEnd > 0)
						continue;		
					
					int resetDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_DATE.toInt(), param, "", reset);
					
					if (resetDate == today) {
						bTodayResetFound = true;				
						break;
					}
				}				
				
				// Retrieve projection index - we will need to load its closing prices
				int projIdx = trn.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), param, "", 0, 0);				
				indexesToLoad.add(projIdx);
				
				// If projection index currency doesn't match leg currency, we will have to convert FX, so need to load FX indexes too
				int legCcy = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), param);
				if (MTL_Position_Utilities.getPaymentCurrencyForIndex(projIdx) != legCcy) {
					indexesToLoad.add(MTL_Position_Utilities.getDefaultFXIndexForCcy(legCcy));
				}				
			}
			
			// If this transaction does not have any resets today, delete it from the list
			if (!bTodayResetFound) {				
				transData.delRow(row);
			}			
		}	
		
		// Set up indexLoadTable, and load universal prices
		Table indexLoadTable = new Table("Indexes to Load");
		try {
			indexLoadTable.addCol("index", COL_TYPE_ENUM.COL_INT);
			indexLoadTable.addNumRows(indexesToLoad.size());
			int row = 1;
			for (Integer i : indexesToLoad) {
				indexLoadTable.setInt(1, row, i);
				row++;
			}		
			
			Sim.loadIndexList(indexLoadTable, 1);
			Logging.info("PNL_Handle_Intraday_Fixing.prepareTransactionData found " + indexLoadTable.getNumRows() + " indexes to load.\n");
			
		} finally {
			if (Table.isTableValid(indexLoadTable) == 1) {
				indexLoadTable.destroy();
			}
		}
	}

	/**
	 * Retrieve the list of relevant ComSwap transactions
	 * @return
	 * @throws OException
	 */
	private Table retrieveRelevantTransactions() throws OException {
		Logging.info("PNL_Handle_Intraday_Fixing.retrieveRelevantTransactions\n");
		Table transData = new Table("Transactions");
		String sql =
				"SELECT ab.deal_tracking_num deal_num, ab.tran_num " +
				"FROM ab_tran ab " +
				"WHERE ab.toolset = 15 AND ab.tran_status in (2,3) AND ab.current_flag = 1";
		
		DBase.runSqlFillTable(sql, transData);
		transData.addCol("tran_ptr", COL_TYPE_ENUM.COL_TRAN);
		int rows = transData.getNumRows();
		
		for (int row = 1; row <= rows; row++) {
			int tranNum = transData.getInt("tran_num", row);
			Transaction trn = Transaction.retrieve(tranNum);
			transData.setTran("tran_ptr", row, trn);
		}
		
		return transData;
	}

	public void process(Transaction trn, HashSet<Integer> dates) throws OException {
		Table transData = new Table("Trans Data");
		
		transData.addCol("tran_ptr", COL_TYPE_ENUM.COL_TRAN);
		transData.addRow();
		transData.setTran("tran_ptr", 1, trn);	
		
		// Prepare a single transaction's data
		prepareTransactionData(transData);
		
		// Prepare market data for all deals
		Vector<PNL_MarketDataEntry> dataEntries = prepareMarketData(transData);
		
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
			Logging.init( this.getClass(), ConfigurationItemPnl.CONST_REP_CONTEXT, ConfigurationItemPnl.CONST_REP_SUBCONTEXT);
			
		} 
		catch (Exception e) 
		{
			throw new RuntimeException (e);
		}
		Logging.info("Plugin: " + this.getClass().getName() + " started.\r\n");
	}	
}
