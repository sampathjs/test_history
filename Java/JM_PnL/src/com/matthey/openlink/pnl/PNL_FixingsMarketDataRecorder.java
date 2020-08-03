package com.matthey.openlink.pnl;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import com.matthey.openlink.jde_extract.JDE_Data_Manager;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.olf.openjvs.enums.VALUE_STATUS_ENUM;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 */

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_OPS_SERVICE)
public class PNL_FixingsMarketDataRecorder implements IScript {
	
	public final static int S_FX_RESET_OFFSET = 10 * 1000;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		
		initLogging();
		Logging.info("PNL_FixingsMarketDataRecorder started. Date is: " + OCalendar.formatJd(OCalendar.today()) + "\n");
    	    	
        Table argt = context.getArgumentsTable();
                        
        Vector<PNL_MarketDataEntry> dataEntries = new Vector<PNL_MarketDataEntry>();
        Vector<Integer> transactionsProcessed = new Vector<Integer>();
        
        Table dealInfo = argt.getTable("Deal Info", 1);

    	int retval = Index.refreshDb(1);
    	if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
    		Logging.error( DBUserTable.dbRetrieveErrorInfo( retval, "Index.refreshDb Failed." ) + "\n" );
        }
    	
		// Refresh historical prices, as this ops service is run after the historical prices have been loaded
		MTL_Position_Utilities.refreshHistPrices();
		
		Transaction trn = Util.NULL_TRAN;
    	int rows = dealInfo.getNumRows();
        for (int row = 1; row <= rows; row++) {
        	int dealNum = dealInfo.getInt("deal_tracking_num", row);
        	int tranNum = dealInfo.getInt("tran_num", row);        	
        	int toolset = dealInfo.getInt("toolset", row);
        	       	
        	if (!needToProcessDeal(toolset)) {
        		Logging.info("PNL_FixingsMarketDataRecorder: transaction processing triggered for tran num: " + tranNum + ". Not interested.\n");
        		continue;
        	}
        	
        	try {
        		trn = Transaction.retrieve(tranNum);        	
            	Vector<PNL_MarketDataEntry> thisDealEntries = null;        	
            	
            	Logging.info("PNL_FixingsMarketDataRecorder: transaction processing triggered for tran num: " + tranNum + ". Processing.\n");
        		
            	// We need to retrieve existing market data for the transactions, so that we know 
        		//		if we have missing market data for historical resets
        		// 		if there are already existing entries for today from PNL_Handle_Intraday_Fixing (e.g. for AM fixing on a deal that prices off both AM and PM)
            	Vector<PNL_MarketDataEntry> marketDataEntries = new PNL_UserTableHandler().retrieveMarketData(dealNum);
            	HashMap<PNL_EntryDataUniqueID, PNL_MarketDataEntry> marketDataEntryMap = new HashMap<PNL_EntryDataUniqueID, PNL_MarketDataEntry>();

            	for (PNL_MarketDataEntry marketDataEntry : marketDataEntries) {
            		marketDataEntryMap.put(marketDataEntry.m_uniqueID, marketDataEntry);
            	}
            	
        		// Pre-create a list of existing market data keys (deal-leg-pdc-reset), and a flag to indicate if they are still valid
        		HashMap<PNL_EntryDataUniqueID, Boolean> existingKeyValidMap = new HashMap<PNL_EntryDataUniqueID, Boolean>();
        		
        		for (PNL_EntryDataUniqueID id : marketDataEntryMap.keySet()) {
        			// Initialise everything to FALSE, then set to true as we find matching entries in current tran data
        			existingKeyValidMap.put(id, false);
        		}            	
            	        	
            	// Add today's resets to the saved market data table, unless it has been saved already     
        		thisDealEntries = processDeal(trn);
        		addTodayEntries(thisDealEntries, marketDataEntryMap, dataEntries, existingKeyValidMap);    	

        		// Check if any historical market reset dates are missing, and if so, return the list of such
        		HashSet<Integer> missingResetDates = new HashSet<Integer>();
        		HashSet<PNL_EntryDataUniqueID> unmatchedExistingEntries = new HashSet<PNL_EntryDataUniqueID>();    		
        		
        		checkMissingResetMarketData(trn, marketDataEntryMap, missingResetDates, existingKeyValidMap);
        		
        		// Now that we have processed all active resets, check existingKeyValidMap and see which keys were un-matched
        		// These keys will be deleted from user table
        		for (PNL_EntryDataUniqueID key : existingKeyValidMap.keySet()) {
        			// Is the key valid? 
        			if (existingKeyValidMap.get(key) == false) {
        				// If not valid, add to list of "bad keys"
        				unmatchedExistingEntries.add(key);
        			}
        		}
        		
        		// If any historical entries are present, but do not match existing structure, they must be deleted
        		// (e.g. prior version of the deal had FX conversion, this one does not, or the dates were moved forward so 
        		// old pre-saved reset IDs are now in the future, and must be deleted 
        		if (unmatchedExistingEntries.size() > 0) {
        			Logging.info("PNL_FixingsMarketDataRecorder: found un-matched existing entries. Deleting.\r\n");        			
        			new PNL_UserTableHandler().deleteMarketData(unmatchedExistingEntries);
        		}
        		
        		// If any historical resets are missing, or have mis-matching values, process them now
        		// Note that we do this after we have deleted USER table entries for unmatched values, as otherwise
        		// we could delete newly added historical reset rows
        		if (missingResetDates.size() > 0) {
        			Logging.info("PNL_FixingsMarketDataRecorder: found missing reset dates. Processing.\r\n"); 
        			PNL_Backfill_Market_Data backfillProcessor = new PNL_Backfill_Market_Data();
        			backfillProcessor.process(trn, missingResetDates);
        		}
        	} catch (Exception e)  {
        		Logging.error("PNL_FixingsMarketDataRecorder failed to process historical market data entries for deal: " + dealNum 
        				+ "\n" + e.getMessage() + "\n");
        	} finally {
        		if (Transaction.isNull(trn) != 1) {
        			trn.destroy();
        		}
        	}
    		
    		// Add this tran num to "transactions processed"
    		transactionsProcessed.add(tranNum);
        }
        
        if (dataEntries.size() > 0) {
            // First, record market data
            new PNL_UserTableHandler().recordMarketData(dataEntries);
         
           	// Now, trigger the JDE staging area re-calculation
           	JDE_Data_Manager dataManager = new JDE_Data_Manager();
           	dataManager.processDeals(transactionsProcessed);        		
        }
        
        Logging.info("PNL_FixingsMarketDataRecorder completed. Date is: " + OCalendar.formatJd(OCalendar.today()) + "\n");
        Logging.close();
    }
	
	/**
	 * Add today's entries to the list of those to be exported to user_jm_pnl_market_data.
	 * If an entry already exists for this reset, check if any key fields amended; if so, overwrite.
	 * Otherwise, leave the earlier entry in.
	 * 
	 * @param input - today's proposed market data entries
	 * @param marketDataEntryMap - the Map of existing market data entries
	 * @param output - final new market data entries to be stored, overwriting as necessary
	 */
	private void addTodayEntries( Vector<PNL_MarketDataEntry> input, HashMap<PNL_EntryDataUniqueID, PNL_MarketDataEntry> marketDataEntryMap,
			Vector<PNL_MarketDataEntry> output, HashMap<PNL_EntryDataUniqueID, Boolean> existingKeyValidMap) {
		
		for (PNL_MarketDataEntry newEntry : input) {
			boolean bShouldAdd = false;
			
			// This key exists, and we should not force-remove any versions of it (if necessary, we will overwrite it below)
			existingKeyValidMap.put(newEntry.m_uniqueID, true);
			
			if (!marketDataEntryMap.containsKey(newEntry.m_uniqueID)) {
				// If there is no pre-existing market data entry, add the new one in
				bShouldAdd = true;
				
				// Also, add it into the HashMap, as it should contain all reset entries for this deal
				marketDataEntryMap.put(newEntry.m_uniqueID, newEntry);								
			} else {
				
				PNL_MarketDataEntry oldEntry = marketDataEntryMap.get(newEntry.m_uniqueID);
				
				if ((newEntry.m_tradeDate != oldEntry.m_tradeDate) || 
					(newEntry.m_indexID != oldEntry.m_indexID) ||
					(newEntry.m_metalCcy != oldEntry.m_metalCcy) ||
					(newEntry.m_fixingDate != oldEntry.m_fixingDate))
				{
					// If the pre-existing market data entry's key fields do not match, this is a major deal amendment,
					// and we should overwrite existing market data
					bShouldAdd = true;					
				}
			}
			
			if (bShouldAdd) {
				output.add(newEntry);
			}
		}
	}

	/**
	 * Checks whether any old reset data is missing or corrupt (because of deal amendment changing basic deal features)
	 * @param trn - the transaction
	 * @param marketDataEntryMap - map of reset data to its values
	 * @return
	 * @throws OException
	 */
	private void checkMissingResetMarketData(Transaction trn, HashMap<PNL_EntryDataUniqueID, PNL_MarketDataEntry> marketDataEntryMap,
			HashSet<Integer> missingResetDates, HashMap<PNL_EntryDataUniqueID, Boolean> existingKeyValidMap) throws OException {
		
		int today = Util.getTradingDate(); //OCalendar.today();
		int dealNum = trn.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
		int fixedLeg = Ref.getValue(SHM_USR_TABLES_ENUM.FX_FLT_TABLE, "Fixed");
		int usdCcy = Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "USD");
			
    	// Take delivery date from payment date on fixed leg, since that represents metal movement
    	int deliveryDate = trn.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), 0, "", 0);		
		
    	int numParams = trn.getNumRows(-1, TRANF_GROUP.TRANF_GROUP_PARM.toInt());
		for (int param = 0; param < numParams; param++) {
			
			int fxFlt = trn.getFieldInt(TRANF_FIELD.TRANF_FX_FLT.toInt(), param);

			// Skip the fixed (deliverable) swap leg, we only store resets from floating legs
			if (fxFlt == fixedLeg) {				
				continue;
			}
			
			// Retrieve leg-level parameters to check against saved USER table data
			int legCcy = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), param);			
			int pymtDate = trn.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), param, "", 0);			
			int projIdx = trn.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), param, "", 0, 0);						
			
			int totalResetPeriods = trn.getNumRows(param, TRANF_GROUP.TRANF_GROUP_RESET.toInt());
			for (int j = 0; j < totalResetPeriods; j++) {
				int blockEnd = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_BLOCK_END.toInt(), param, "", j);
				
				// Skip block-end resets
				if (blockEnd > 0){
					continue;		
				}
				
				int resetDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_DATE.toInt(), param, "", j);
				
				// Retrieve matching reset ID
				PNL_EntryDataUniqueID resetUniqueID = new PNL_EntryDataUniqueID(dealNum, param, 0, j);				
				
				// Skip resets which are still in the future
				if (resetDate >= today) {
					continue;
				}
				
				if (!marketDataEntryMap.containsKey(resetUniqueID)) {
					updateResetDatesToRegenerate(missingResetDates, resetDate, resetUniqueID, "Missing Reset Data for" + dealNum + ", " + param + ", " + j);
					continue;
				}
				if (existingKeyValidMap.containsKey(resetUniqueID)) {
					existingKeyValidMap.put(resetUniqueID, true);					
				}
				
				PNL_MarketDataEntry existingEntry = marketDataEntryMap.get(resetUniqueID);
				
				if (existingEntry.m_tradeDate != resetDate) {
					// Changed reset date = old spot and fwd price will be no longer appropriate
					updateResetDatesToRegenerate(missingResetDates, resetDate, resetUniqueID, "Mismatch reset date for" + dealNum + ", " + param + ", " + j);
					continue;					
				}
				if (existingEntry.m_indexID != projIdx) {
					// Changed pricing index = old spot and fwd price will be no longer appropriate
					updateResetDatesToRegenerate(missingResetDates, resetDate, resetUniqueID, "Mismatch projection index for" + dealNum + ", " + param + ", " + j);
					continue;					
				}				
				if (existingEntry.m_fixingDate != deliveryDate) {
					// Changed delivery (fixing) date = old fwd price will be no longer appropriate
					updateResetDatesToRegenerate(missingResetDates, resetDate, resetUniqueID, "Mismatch delivery date for" + dealNum + ", " + param + ", " + j);
					continue;					
				}				
				
				// If there is FX conversion, need to store its details too - note that we need to store it
				// whenever it doesn't match leg currency for JDE Extract, and whenever it is not USD for "JM Raw PNL Data"
				int idxPymtCcy = MTL_Position_Utilities.getPaymentCurrencyForIndex(projIdx);
				if ((idxPymtCcy != legCcy) || (idxPymtCcy != usdCcy)) {
					
					PNL_EntryDataUniqueID fxResetUniqueID = new PNL_EntryDataUniqueID(dealNum, param, 0, j + S_FX_RESET_OFFSET);
					if (!marketDataEntryMap.containsKey(fxResetUniqueID)) {
						updateResetDatesToRegenerate(missingResetDates, resetDate, resetUniqueID, "Missing FX Reset Data for" + dealNum + ", " + param + ", " + j);
						continue;
					}	
					
					if (existingKeyValidMap.containsKey(fxResetUniqueID)) {
						existingKeyValidMap.put(fxResetUniqueID, true);					
					}					
					
					PNL_MarketDataEntry fxExistingEntry = marketDataEntryMap.get(fxResetUniqueID);
					if (fxExistingEntry.m_tradeDate != resetDate) {
						// Changed reset date = old spot and fwd price will be no longer appropriate
						updateResetDatesToRegenerate(missingResetDates, resetDate, resetUniqueID, "Mismatch FX reset date for" + dealNum + ", " + param + ", " + j);
						continue;					
					}		
					
					if (fxExistingEntry.m_indexID != MTL_Position_Utilities.getDefaultFXIndexForCcy(legCcy)) {
						// Changed currency / FX index = old spot and fwd price will be no longer appropriate
						updateResetDatesToRegenerate(missingResetDates, resetDate, resetUniqueID, "Mismatch FX index for" + dealNum + ", " + param + ", " + j);
						continue;						
					}
					
					if (fxExistingEntry.m_fixingDate != pymtDate) {
						// Changed payment date = old fwd price will be no longer appropriate
						updateResetDatesToRegenerate(missingResetDates, resetDate, resetUniqueID, "Mismatch FX forward (payment) date for" + dealNum + ", " + param + ", " + j);
						continue;					
					}						
				}
			}
		}
	}
	
	private void updateResetDatesToRegenerate(HashSet<Integer> regenerateDates, int date, PNL_EntryDataUniqueID id, String reason) throws OException {
		
		regenerateDates.add(date);
		Logging.info("PNL_FixingsMarketDataRecorder: Will regenerate reset for date: " + OCalendar.formatJd(date) + ", deal: " + id.m_dealNum + " - " + reason);
	}

	/**
	 * Check if this deal is of interest to this Ops Service
	 * @param trn
	 * @return
	 * @throws OException
	 */
	private boolean needToProcessDeal(int toolset) throws OException {
		boolean retVal = false;		
		
		if (toolset == TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt()) {
			retVal = true;
		}
		
		return retVal;
	}
	
	/**
	 * Generate relevant data
	 * @param trn
	 * @param criticalFieldsOnly
	 * @return
	 * @throws OException
	 */
	static Vector<PNL_MarketDataEntry> processDeal(Transaction trn) throws OException {
		
		Vector<PNL_MarketDataEntry> thisDealEntries = null;
		int toolset = trn.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
		
    	if (toolset == TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt()) {
    		thisDealEntries = processComSwapDeal(trn);
    	}
    	
    	return thisDealEntries;
	}
	  
    private static Vector<PNL_MarketDataEntry> processComSwapDeal(Transaction trn) throws OException {
    	
    	Vector<PNL_MarketDataEntry> dataEntries = new Vector<PNL_MarketDataEntry>();
    	
    	int today = OCalendar.today();
    	int liborIndex = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, "LIBOR.USD");    	
    	int usdCcy = Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "USD");
    	
    	int dealNum = trn.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
    	int fixedLeg = Ref.getValue(SHM_USR_TABLES_ENUM.FX_FLT_TABLE, "Fixed");
    	
    	// Take delivery date from payment date on fixed leg, since that represents metal movement
    	int deliveryDate = trn.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), 0, "", 0);
    	
    	double usdDF = MTL_Position_Utilities.getRateForDate(liborIndex, deliveryDate);
    	
    	int numParams = trn.getNumRows(-1, TRANF_GROUP.TRANF_GROUP_PARM.toInt());
		for (int param = 0; param < numParams; param++) {
			int fxFlt = trn.getFieldInt(TRANF_FIELD.TRANF_FX_FLT.toInt(), param);

			// Skip the fixed (deliverable) swap leg, we only store resets from floating legs
			if (fxFlt == fixedLeg) {				
				continue;
			}
			
			int pymtDate = trn.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), param, "", 0);
			
			int projIdx = trn.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), param, "", 0, 0);
			int projIdxMetal = MTL_Position_Utilities.getCcyForIndex(projIdx);
			
			int legCcy = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), param);
			
			int totalResetPeriods = trn.getNumRows(param, TRANF_GROUP.TRANF_GROUP_RESET.toInt());
			for (int j = 0; j < totalResetPeriods; j++) {
				
				int resetDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_DATE.toInt(), param, "", j);
				
				int blockEnd = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_BLOCK_END.toInt(), param, "", j);
				
				// Skip block-end resets
				if (blockEnd > 0){
					continue;				
				}
				
				// Skip anything that is not today
				if (resetDate != today) {
					continue;
				}
				
				// Check that we either:
				//		- Have the reset status as "known"
				//		- Have a historical price for this reset				
				int resetStatus = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_VALUE_STATUS.toInt(), param, "", j);								
				if (resetStatus != VALUE_STATUS_ENUM.VALUE_KNOWN.toInt()) {
					// Check historical price
					int rfisDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_RFIS_DATE.toInt(), param, "", j);
					int refSource = trn.getFieldInt(TRANF_FIELD.TRANF_REF_SOURCE.toInt(), param);

					if (!MTL_Position_Utilities.hasHistPrice(projIdx, resetDate, rfisDate, refSource)) {
						continue;
					}
				}
				

				PNL_MarketDataEntry entry = new PNL_MarketDataEntry();
				
				entry.m_uniqueID = new PNL_EntryDataUniqueID(dealNum, param, 0, j);
				entry.m_tradeDate = today;					
				entry.m_indexID = projIdx;
				entry.m_metalCcy = projIdxMetal;
				entry.m_fixingDate = deliveryDate;
				entry.m_spotRate = MTL_Position_Utilities.getSpotGptRate(entry.m_indexID);
				entry.m_forwardRate = MTL_Position_Utilities.getRateForDate(entry.m_indexID, deliveryDate);
				entry.m_usdDF = usdDF;
				
				dataEntries.add(entry);
				
				// If there is FX conversion, need to store its details too - note that we need to store it
				// whenever it doesn't match leg currency for JDE Extract, and whenever it is not USD for "JM Raw PNL Data"
				int idxPymtCcy = MTL_Position_Utilities.getPaymentCurrencyForIndex(projIdx);
				if ((idxPymtCcy != legCcy) || (idxPymtCcy != usdCcy)) {
					
					PNL_MarketDataEntry fxEntry = new PNL_MarketDataEntry();
					
					fxEntry.m_uniqueID = new PNL_EntryDataUniqueID(dealNum, param, 0, j + S_FX_RESET_OFFSET);
					fxEntry.m_tradeDate = today;					
					fxEntry.m_indexID = MTL_Position_Utilities.getDefaultFXIndexForCcy(legCcy);
					fxEntry.m_metalCcy = legCcy;
					fxEntry.m_fixingDate = pymtDate;
					fxEntry.m_spotRate = MTL_Position_Utilities.getSpotGptRate(fxEntry.m_indexID);
					fxEntry.m_forwardRate = MTL_Position_Utilities.getRateForDate(fxEntry.m_indexID, pymtDate);
					fxEntry.m_usdDF = usdDF;
					
					dataEntries.add(fxEntry);
				}							
			}			
		}
				
		for (PNL_MarketDataEntry dataEntry : dataEntries) {
			// If this leg's currency is quoted as "Ccy per USD", convert to "USD per Ccy"
			if (!MTL_Position_Utilities.getConvention(dataEntry.m_metalCcy)) {
				if (dataEntry.m_spotRate > 0){
					dataEntry.m_spotRate = 1 / dataEntry.m_spotRate;
				}
				   
				if (dataEntry.m_forwardRate > 0){
					dataEntry.m_forwardRate = 1 / dataEntry.m_forwardRate;            		
				}
			}
		}
    	
    	return dataEntries;
    }	
    
    /**
	 * Initialise standard Plugin log functionality
	 * @throws OException
	 */
	private void initLogging() throws OException 	{	
		
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");
		String logLevel = ConfigurationItemPnl.LOG_LEVEL.getValue();
		String logFile = ConfigurationItemPnl.LOG_FILE.getValue();
		String logDir = ConfigurationItemPnl.LOG_DIR.getValue();
		if (logDir.trim().isEmpty()) {
			logDir = abOutdir + "\\error_logs";
		}
		if (logFile.trim().isEmpty())  {
			logFile = this.getClass().getName() + ".log";
		}
		
		try  {
			Logging.init( this.getClass(), ConfigurationItemPnl.CONST_REP_CONTEXT, ConfigurationItemPnl.CONST_REP_SUBCONTEXT);
			
		}  catch (Exception e) {
			throw new RuntimeException (e);
		}
		Logging.info("Plugin: " + this.getClass().getName() + " started.\r\n");
	}
}
