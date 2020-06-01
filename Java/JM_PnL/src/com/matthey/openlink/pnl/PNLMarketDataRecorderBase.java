package com.matthey.openlink.pnl;
/* 2020-05-20	V1.0	jainv02		- EPI 1254 - Add support for Implied EFP calculation for Comfut*/

import java.math.BigDecimal;
import java.util.Vector;

import com.matthey.openlink.jde_extract.IJdeDataManager;
import com.matthey.openlink.jde_extract.JDE_Data_Manager;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public abstract class PNLMarketDataRecorderBase implements IScript{
	
	public abstract IJdeDataManager getDataManager();
	
	public abstract IPnlUserTableHandler getUserTableHandler();
	
	public abstract String getInterestCurveName();
	
	public abstract void calculateFXDate(PNL_MarketDataEntry dataEntry, int liborIndex, int fxFixingDate) throws OException;
	
	@Override
	public void execute(IContainerContext context) throws OException
    {
		initPluginLog();
		int finalRegenerateDate = -1;
		int storedRegenerateDate = -1;
		int today = OCalendar.today();
		
		PluginLog.info("PNL_MarketDataRecorder started. Date is: " + OCalendar.today() + "\n");
    	OConsole.message("PNL_MarketDataRecorder started. Date is: " + OCalendar.today() + "\n");    	
    	
        Table argt = context.getArgumentsTable();
                
        Vector<PNL_MarketDataEntry> dataEntries = new Vector<PNL_MarketDataEntry>();
        Vector<Integer> jdeDealList = new Vector<Integer>();
        
        Table dealInfo = argt.getTable("Deal Info", 1);

    	int retval = Index.refreshDb(1);
    	if( retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
        {
    		PluginLog.error( DBUserTable.dbRetrieveErrorInfo( retval, "Index.refreshDb Failed." ) + "\n" );
    		OConsole.oprint( DBUserTable.dbRetrieveErrorInfo( retval, "Index.refreshDb Failed." ) + "\n" );
        }

        for (int row = 1; row <= dealInfo.getNumRows(); row++)
        {
        	int tranNum = dealInfo.getInt("tran_num", row);
        	Transaction trn = Transaction.retrieve(tranNum);        	
        	int tradeDate = trn.getFieldInt(TRANF_FIELD.TRANF_TRADE_DATE.toInt());
        	Vector<PNL_MarketDataEntry> thisDealEntries = null, oldEntries = null;
        	
        	if (getDataManager().needToProcessTransaction(trn))
        	{
        		jdeDealList.add(tranNum);
        	}
        	
        	if (!needToProcessDeal(trn))
        	{
        		continue;
        	}
        	
        	thisDealEntries = processDeal(trn, true);
        	
            if (thisDealEntries.size() > 0)
            {
            	int dealNum = trn.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
            	oldEntries = getUserTableHandler().retrieveMarketData(dealNum);
            	
            	if ((oldEntries == null) || (oldEntries.size() == 0))
            	{
            		PluginLog.info("PNL_MarketDataRecorder:: no prior entry for deal " + dealNum + " found. Processing.\n");
            		OConsole.message("PNL_MarketDataRecorder:: no prior entry for deal " + dealNum + " found. Processing.\n");
            		            		            		
                	// If no entries exist for this deal, add them in         
            		thisDealEntries = processDeal(trn, false);
            		dataEntries.addAll(thisDealEntries);
            		
            		// If this transaction is a back-dated trade, we need to make sure we regenerate historical trading pnl valuations
            		if (tradeDate < today)
            		{
            			storedRegenerateDate = getUserTableHandler().retrieveRegenerateDate();
            			if ((storedRegenerateDate <= 0) || (tradeDate < storedRegenerateDate))
            			{
            				finalRegenerateDate = (finalRegenerateDate > 0) ? Math.min(finalRegenerateDate, tradeDate) : tradeDate;
            			}
            		}
            	}
            	else if (keyPropertiesDiffer(oldEntries, thisDealEntries))
            	{
            		PluginLog.info("PNL_MarketDataRecorder:: key fields for deal " + dealNum + " modified. Processing.\n");
            		OConsole.message("PNL_MarketDataRecorder:: key fields for deal " + dealNum + " modified. Processing.\n");
                	// If old entries exist for this deal, but key values have changed, replace       
            		thisDealEntries = processDeal(trn, false);
            		dataEntries.addAll(thisDealEntries);          		
            	}
            	else
            	{
            		PluginLog.info("PNL_MarketDataRecorder:: key fields for deal " + dealNum + " are not modified. Skipping.\n");
            		OConsole.message("PNL_MarketDataRecorder:: key fields for deal " + dealNum + " are not modified. Skipping.\n");
            	}
            }                   
        }
                
        getUserTableHandler().recordMarketData(dataEntries);        
        
        if (finalRegenerateDate > 0)
        {
        	getUserTableHandler().setRegenerateDate(finalRegenerateDate);
        }
        
        // Call to store JDE staging area data - we want to update the JDE extract on any deal amendment
        // as non-market data may have changed, such as volume, which will affect the final P&L calculation
        if (jdeDealList.size() > 0)
        {
        	getDataManager().processDeals(jdeDealList);
        }
        PluginLog.info("PNL_MarketDataRecorder completed. Date is: " + OCalendar.today() + "\n");
        OConsole.message("PNL_MarketDataRecorder completed. Date is: " + OCalendar.today() + "\n");
    }
	
	private boolean needToProcessDeal(Transaction trn) throws OException
	{
		boolean retVal = false;
		int toolset = trn.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
		
		if ((toolset == TOOLSET_ENUM.FX_TOOLSET.toInt()) || (toolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt()))
		{
			retVal = true;
		}
		
		return retVal;
	}
	
	private Vector<PNL_MarketDataEntry> processDeal(Transaction trn, boolean criticalFieldsOnly) throws OException
	{
		Vector<PNL_MarketDataEntry> thisDealEntries = null;
		int toolset = trn.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
		
    	if (toolset == TOOLSET_ENUM.FX_TOOLSET.toInt())
    	{
    		thisDealEntries = processFXDeal(trn, criticalFieldsOnly);
    	}
    	else if (toolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt())
    	{
    		thisDealEntries = processComFutDeal(trn, criticalFieldsOnly);
    	}
    	
    	return thisDealEntries;
	}
	
	private boolean keyPropertiesDiffer(Vector<PNL_MarketDataEntry> entry1, Vector<PNL_MarketDataEntry> entry2) throws OException
	{
		if (entry1.size() != entry2.size())
		{
			return true;
		}
		
		for (int i = 0; i < entry1.size(); i++)
		{
			if (!entry1.get(i).m_uniqueID.equals(entry2.get(i).m_uniqueID))
			{
				return true;
			}
			
			if (entry1.get(i).m_tradeDate != entry2.get(i).m_tradeDate)
			{
				return true;
			}			
			
			if (entry1.get(i).m_fixingDate != entry2.get(i).m_fixingDate)
			{
				return true;
			}
			
			if (entry1.get(i).m_indexID != entry2.get(i).m_indexID)
			{
				return true;
			}
			
			if (entry1.get(i).m_metalCcy != entry2.get(i).m_metalCcy)
			{
				return true;
			}			
		}
		
		return false;
	}
    
    private Vector<PNL_MarketDataEntry> processFXDeal(Transaction trn, boolean criticalFieldsOnly) throws OException
    {
    	boolean bLoadedHistoricalClosingPrices = false;
    	int today = OCalendar.today();
    	int liborIndex = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, getInterestCurveName());
    	
    	Vector<PNL_MarketDataEntry> dataEntries = new Vector<PNL_MarketDataEntry>();
    	
    	// Add data entries for leg one and leg zero
    	dataEntries.add(new PNL_MarketDataEntry());
    	dataEntries.add(new PNL_MarketDataEntry());
    	
    	int dealNum = trn.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
    	int tradeDate = trn.getFieldInt(TRANF_FIELD.TRANF_TRADE_DATE.toInt());
    	
    	int fxFixingDate = trn.getFieldInt(TRANF_FIELD.TRANF_FX_DATE.toInt());
    	if (fxFixingDate == 0)
    	{    		
    		int insSubType = trn.getFieldInt(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt());
    		int tranGroup =	trn.getFieldInt(TRANF_FIELD.TRANF_TRAN_GROUP.toInt());
    		 		
    		/*
    		 * FX far leg is modelled as a separate transaction that doesn't have an entry in fx_tran_aux_data itself,
    		 * so TRANF_FX_DATE, TRANF_FX_FAR_DATE etc do not work - need to do an SQL call to retrieve the primary transaction's
    		 * data (shares the same transaction group)
    		 */    		
    		if ((insSubType == com.olf.openjvs.enums.INS_SUB_TYPE.fx_far_leg.toInt()) && (tranGroup > 0))
    		{
    			Table dataTable = Table.tableNew();
    			String sql = "select max (fx.far_date) from fx_tran_aux_data fx, ab_tran ab where ab.tran_group = " + tranGroup + " and ab.current_flag = 1 and fx.tran_num = ab.tran_num";
    			DBase.runSqlFillTable(sql, dataTable);
    			
    			if (Table.isTableValid(dataTable) == 1)
    			{
    				fxFixingDate = dataTable.getInt(1, 1);
    				dataTable.destroy();
    			}
    		}
    	}
    	
    	int ccyLegZero = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), 0);
    	int ccyLegOne = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), 1);
    	
    	dataEntries.get(0).m_uniqueID = new PNL_EntryDataUniqueID(dealNum, 0, 0, 0);
    	dataEntries.get(0).m_tradeDate = tradeDate;
    	dataEntries.get(0).m_fixingDate = fxFixingDate;
    	dataEntries.get(0).m_metalCcy = ccyLegZero;
    	dataEntries.get(0).m_indexID = MTL_Position_Utilities.getDefaultFXIndexForCcy(ccyLegZero);
    	
    	dataEntries.get(1).m_uniqueID = new PNL_EntryDataUniqueID(dealNum, 1, 0, 0);
    	dataEntries.get(1).m_tradeDate = tradeDate;
    	dataEntries.get(1).m_fixingDate = fxFixingDate;
    	dataEntries.get(1).m_metalCcy = ccyLegOne;
    	dataEntries.get(1).m_indexID = MTL_Position_Utilities.getDefaultFXIndexForCcy(ccyLegOne);
     	    	
    	if (!criticalFieldsOnly)
    	{    		
    		Table idxToLoad = null;
    		boolean bSucceeded = false;
    		
    		try
    		{        		
    			idxToLoad = new Table("Indexes to Load");
    			idxToLoad.addCol("index", COL_TYPE_ENUM.COL_INT);
    			idxToLoad.addNumRows(1);
    			idxToLoad.setInt(1, 1, liborIndex);
    			
    			// Either side can be USD-nominated (with relevant FX index zero \ missing), so check each independently
    			for (int i = 0;  i <= 1; i++)
    			{
        			if (dataEntries.get(i).m_indexID > 0)
        			{
        				idxToLoad.addRow();
        				idxToLoad.setInt(1, idxToLoad.getNumRows(), dataEntries.get(i).m_indexID);
        			}    				
    			}
   			
    			// Load either yesterday's closing prices, or current universal prices
        		if (tradeDate < today)
        		{       			
        			Util.setCurrentDate(tradeDate);
        			Sim.loadCloseIndexList(idxToLoad, 1, tradeDate);
        			
        			bLoadedHistoricalClosingPrices = true;
        		}
        		else
        		{        			
        			Sim.loadIndexList(idxToLoad, 1);
        		}
        		        		
    			for (int i = 0;  i <= 1; i++)
    			{
    				PNL_MarketDataEntry dataEntry = dataEntries.get(i);
    				/*
    				if (dataEntry.m_indexID > 0)
    				{					  
    					dataEntry.m_spotRate = MTL_Position_Utilities.getSpotGptRate(dataEntry.m_indexID);
    					dataEntry.m_forwardRate = MTL_Position_Utilities.getRateForDate(dataEntry.m_indexID, fxFixingDate);
    					dataEntry.m_usdDF = MTL_Position_Utilities.getRateForDate(liborIndex, fxFixingDate);   
    				   
						// If this leg's currency is quoted as "Ccy per USD", convert to "USD per Ccy"
						if (!MTL_Position_Utilities.getConvention(dataEntry.m_metalCcy))
						{
							if (dataEntry.m_spotRate > 0)
								dataEntry.m_spotRate = 1 / dataEntry.m_spotRate;
							   
							if (dataEntry.m_forwardRate > 0)
								dataEntry.m_forwardRate = 1 / dataEntry.m_forwardRate;            		
						}
    				}
    				*/
    				calculateFXDate(dataEntry, liborIndex, fxFixingDate);
    			}
            	
            	bSucceeded = true;
    		}
    		catch (Exception e)
    		{
    			PluginLog.error("processFXDeal - " + e.toString() + "\n");
				OConsole.message("processFXDeal - " + e.toString() + "\n");
				
        		if (tradeDate < today)
        		{    			
        			Util.setCurrentDate(today);
        			Sim.loadIndexList(idxToLoad, 1);
        			
        			// If we failed to load historical closing prices, at least try to get the current ones
        			if (!bLoadedHistoricalClosingPrices)
        			{
            			for (int i = 0;  i <= 1; i++)
            			{
            				PNL_MarketDataEntry dataEntry = dataEntries.get(i);
         
            				if (dataEntry.m_indexID > 0)
            				{
            					dataEntry.m_spotRate = MTL_Position_Utilities.getSpotGptRate(dataEntry.m_indexID);
            					dataEntry.m_forwardRate = MTL_Position_Utilities.getRateForDate(dataEntry.m_indexID, fxFixingDate);
            					dataEntry.m_usdDF = MTL_Position_Utilities.getRateForDate(liborIndex, fxFixingDate);
            				}
            			}
           			}        			
        		}
    		}
    		
    		if ((bSucceeded) && (tradeDate < today))
    		{
    			Util.setCurrentDate(today);
    			Sim.loadIndexList(idxToLoad, 1);
    		}
    		
    		if (Table.isTableValid(idxToLoad) == 1)
			{
				idxToLoad.destroy();
			}    		
     	}
    	
    	return dataEntries;
    }

    private Vector<PNL_MarketDataEntry> processComFutDeal(Transaction trn, boolean criticalFieldsOnly) throws OException
    {
    	boolean bLoadedHistoricalClosingPrices = false;
    	Vector<PNL_MarketDataEntry> dataEntries = new Vector<PNL_MarketDataEntry>();
    	
    	int today = OCalendar.today();
    	int liborIndex = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, "LIBOR.USD");    	
    	
    	// Add data entries for leg one and leg zero
    	dataEntries.add(new PNL_MarketDataEntry());
    	dataEntries.add(new PNL_MarketDataEntry());
    
    	int dealNum = trn.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
    	int fixingDate = trn.getFieldInt(TRANF_FIELD.TRANF_EXPIRATION_DATE.toInt());
    	int tradeDate = trn.getFieldInt(TRANF_FIELD.TRANF_TRADE_DATE.toInt());
    	int projIdx = trn.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), 0);
    	int metal = MTL_Position_Utilities.getCcyForIndex(projIdx);
    	   	
    	dataEntries.get(0).m_uniqueID = new PNL_EntryDataUniqueID(dealNum, 0, 0, 0);
    	dataEntries.get(0).m_tradeDate = tradeDate;
    	dataEntries.get(0).m_fixingDate = fixingDate;
    	dataEntries.get(0).m_metalCcy = metal;
    	dataEntries.get(0).m_indexID = projIdx; 
    	
    	PluginLog.info("PNL_MarketDataRecorder::processComFutDeal - " + dealNum + "\n");
    	OConsole.message("PNL_MarketDataRecorder::processComFutDeal - " + dealNum + "\n");
    	
    	if (!criticalFieldsOnly)
    	{
    		Table idxToLoad = null;
    		
    		try
    		{        	
       			idxToLoad = new Table("Indexes to Load");
    			idxToLoad.addCol("index", COL_TYPE_ENUM.COL_INT);
    			idxToLoad.addNumRows(2);
    			idxToLoad.setInt(1, 1, liborIndex);
    			idxToLoad.setInt(1, 2, dataEntries.get(0).m_indexID);    			
    			
        		if (tradeDate < today)
        		{
        			PluginLog.info("PNL_MarketDataRecorder::processComFutDeal - loading closing prices for " + OCalendar.formatJd(tradeDate) + ".\n");
        			OConsole.message("PNL_MarketDataRecorder::processComFutDeal - loading closing prices for " + OCalendar.formatJd(tradeDate) + ".\n");
        			Util.setCurrentDate(tradeDate);
        			Sim.loadCloseIndexList(idxToLoad, 1, tradeDate);
        			
        			bLoadedHistoricalClosingPrices = true;
        		}
        		else
        		{
        			PluginLog.info("PNL_MarketDataRecorder::processComFutDeal - loading universal prices.\n");
        			OConsole.message("PNL_MarketDataRecorder::processComFutDeal - loading universal prices.\n");
        			Sim.loadIndexList(idxToLoad, 1);
        		}
        		
            	dataEntries.get(0).m_spotRate = MTL_Position_Utilities.getRateForDate(dataEntries.get(0).m_indexID, OCalendar.today());
            	dataEntries.get(0).m_forwardRate = MTL_Position_Utilities.getRateForDate(dataEntries.get(0).m_indexID, fixingDate);
            	dataEntries.get(0).m_usdDF = MTL_Position_Utilities.getRateForDate(liborIndex, fixingDate);
            	
    		}
    		finally
    		{
        		if (tradeDate < today)
        		{    			
        			Util.setCurrentDate(today);
        			Sim.loadIndexList(idxToLoad, 1);
        			
        			if (!bLoadedHistoricalClosingPrices)
        			{
                    	dataEntries.get(0).m_spotRate = MTL_Position_Utilities.getRateForDate(dataEntries.get(0).m_indexID, OCalendar.today());
                    	dataEntries.get(0).m_forwardRate = MTL_Position_Utilities.getRateForDate(dataEntries.get(0).m_indexID, fixingDate);
                    	dataEntries.get(0).m_usdDF = MTL_Position_Utilities.getRateForDate(liborIndex, fixingDate);        				
        			}
        		}
    			
    			if (Table.isTableValid(idxToLoad) == 1)
    			{
    				idxToLoad.destroy();
    			}
    		}
    	}
    	
    	dataEntries.get(1).m_uniqueID = new PNL_EntryDataUniqueID(dealNum, 1, 0, 0);
    	dataEntries.get(1).m_tradeDate = tradeDate;
    	dataEntries.get(1).m_fixingDate = fixingDate;
    	dataEntries.get(1).m_metalCcy = 0;
    	dataEntries.get(1).m_indexID = 0;
    	dataEntries.get(1).m_spotRate = 1.0;
    	dataEntries.get(1).m_forwardRate = 1.0;
    	dataEntries.get(1).m_usdDF = dataEntries.get(0).m_usdDF;
    	
		
    	saveImpliedEFP(dealNum, fixingDate, metal);
    	
    	
    	return dataEntries;    	
    }
    
    private void saveImpliedEFP(int dealNum, int expirationDate, int metal) throws OException {
    	
		
		double impliedEFP = getEFPData(metal, expirationDate);
		
		if(Double.compare(impliedEFP, BigDecimal.ZERO.doubleValue()) == 0){
			PluginLog.info("Skipping EFP Save: Deal Num: " + dealNum + " ; EFP: " + impliedEFP + "\n");
			return;
		}
		
		PluginLog.info("Saving EFP Save: Deal Num: " + dealNum + " ; EFP: " + impliedEFP + "\n");
		getUserTableHandler().saveImpliedEFP(dealNum, impliedEFP);
		PluginLog.info("Saved EFP Save: Deal Num: " + dealNum + " ; EFP: " + impliedEFP + "\n");
		
		
	}
    private double getEFPData(int metal, int expirationDate) throws OException {


		String efpCurve;
		double impliedEFP = 0.0;
		Table indexOutput = Util.NULL_TABLE;
		ConstRepository repo;
		try{
			
			repo = new ConstRepository("PNL", "ImpliedEFP");
			
			boolean spotEqChangeApplicable = Boolean.parseBoolean(repo.getStringValue("SpotEq_Applicable"));
			if(!spotEqChangeApplicable){
				return impliedEFP;
			}

			if(Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XPD") == metal){
				efpCurve = 	repo.getStringValue("Curve_XPD");
			}
			else if(Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XPT") == metal){
				efpCurve = 	repo.getStringValue("Curve_XPT");
			}
			else{
				throw new OException("Unmapped Metal for EFP Curve");
			}

			indexOutput = Index.getOutput(efpCurve);				
			int numRows = indexOutput.getNumRows();
			int colNumContract = indexOutput.getColNum("Contract");
			int colNumDate = indexOutput.getColNum("Date");
			int colNumPrice = indexOutput.getColNum("Price (Mid)");

			for(int i = 1; i<=numRows; i++){
				String contract = indexOutput.getString(colNumContract, i);
				if (contract.contains("@") || contract.length() == 0){		

					continue;
				}

				double price = indexOutput.getDouble(colNumPrice, i);
				int date = indexOutput.getInt(colNumDate, i);

				if(date == expirationDate){

					impliedEFP = price;
					break;
				}



			}
		}finally{
			if(Table.isTableValid(indexOutput) ==1 ){
				indexOutput.destroy();
			}

		}

		return impliedEFP;
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
