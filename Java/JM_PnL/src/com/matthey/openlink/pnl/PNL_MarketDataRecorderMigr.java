package com.matthey.openlink.pnl;
import java.util.Vector;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-03-08	V1.0	jwaechter	- Initial version
 * 2016-03-23	V1.1	jwaechter 	- inverting the exchange rate for ZAR currency
 * 2016-05-02	V1.2	jwaechter	- added special logic for FX currency deals.
 * 2016-05-10	V1.3	jwaechter   - enhanced special logic for FX currency deals 
 */


@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_OPS_SERVICE)
public class PNL_MarketDataRecorderMigr implements IScript {
	
	@Override
	public void execute(IContainerContext context) throws OException
    {
		initPluginLog();

		int finalRegenerateDate = -1;
		int storedRegenerateDate = -1;
		int today = OCalendar.today();
		
		Logging.info("PNL_MarketDataRecorderMigr started.\n");
    	OConsole.message("PNL_MarketDataRecorderMigr started.\n");
    	
        Table argt = context.getArgumentsTable();
                
        Vector<PNL_MarketDataEntry> dataEntries = new Vector<PNL_MarketDataEntry>();
        
        Table dealInfo = argt.getTable("Deal Info", 1);
        
        for (int row = 1; row <= dealInfo.getNumRows(); row++)
        {
        	int tranNum = dealInfo.getInt("tran_num", row);
        	Transaction trn = Transaction.retrieve(tranNum);        	
        	int tradeDate = trn.getFieldInt(TRANF_FIELD.TRANF_TRADE_DATE.toInt());
        	Vector<PNL_MarketDataEntry> thisDealEntries = null, oldEntries = null;
        	
        	if (!needToProcessDeal(trn))
        	{
        		continue;
        	}
        	
        	thisDealEntries = processDeal(trn, true);
        	
            if (thisDealEntries.size() > 0)
            {
            	int dealNum = trn.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
            	oldEntries = new PNL_UserTableHandler().retrieveMarketData(dealNum);
            	
            	if ((oldEntries == null) || (oldEntries.size() == 0))
            	{
            		Logging.info("PNL_MarketDataRecorderMigr:: no prior entry for deal " + dealNum + " found. Processing.\n");
            		OConsole.message("PNL_MarketDataRecorderMigr:: no prior entry for deal " + dealNum + " found. Processing.\n");
            		            		            		
                	// If no entries exist for this deal, add them in         
            		thisDealEntries = processDeal(trn, false);
            		dataEntries.addAll(thisDealEntries);
            		
            		// If this transaction is a back-dated trade, we need to make sure we regenerate historical trading pnl valuations
            		if (tradeDate < today)
            		{
            			storedRegenerateDate = new PNL_UserTableHandler().retrieveRegenerateDate();
            			if ((storedRegenerateDate <= 0) || (tradeDate < storedRegenerateDate))
            			{
            				finalRegenerateDate = (finalRegenerateDate > 0) ? Math.min(finalRegenerateDate, tradeDate) : tradeDate;
            			}
            		}
            	}
            	else if (keyPropertiesDiffer(oldEntries, thisDealEntries))
            	{
            		Logging.info("PNL_MarketDataRecorderMigr:: key fields for deal " + dealNum + " modified. Processing.\n");
            		OConsole.message("PNL_MarketDataRecorderMigr:: key fields for deal " + dealNum + " modified. Processing.\n");
                	// If old entries exist for this deal, but key values have changed, replace       
            		thisDealEntries = processDeal(trn, false);
            		dataEntries.addAll(thisDealEntries);          		
            	}
            	else
            	{
            		Logging.info("PNL_MarketDataRecorderMigr:: key fields for deal " + dealNum + " are not modified. Skipping.\n");
            		OConsole.message("PNL_MarketDataRecorderMigr:: key fields for deal " + dealNum + " are not modified. Skipping.\n");
            	}
            }                   
        }
                
        new PNL_UserTableHandler().recordMarketData(dataEntries);        
        
        if (finalRegenerateDate > 0)
        {
        	new PNL_UserTableHandler().setRegenerateDate(finalRegenerateDate);
        }
        
        Logging.info("PNL_MarketDataRecorderMigr completed.\n");
        OConsole.message("PNL_MarketDataRecorderMigr completed.\n");
        Logging.close();
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
    	int liborIndex = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, "LIBOR.USD");
    	
    	Vector<PNL_MarketDataEntry> dataEntries = new Vector<PNL_MarketDataEntry>();
    	
    	// Add data entries for leg one and leg zero
    	dataEntries.add(new PNL_MarketDataEntry());
    	dataEntries.add(new PNL_MarketDataEntry());
    	
    	int dealNum = trn.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
    	int tradeDate = trn.getFieldInt(TRANF_FIELD.TRANF_TRADE_DATE.toInt());
    	
    	int fxFixingDate = trn.getFieldInt(TRANF_FIELD.TRANF_FX_DATE.toInt());
    	
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
    		String cflowType = trn.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt());

    		int oldTransactionId = trn.getFieldInt(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Migr Id");
    		String baseCurrency = trn.getField(TRANF_FIELD.TRANF_BASE_CURRENCY.toInt());
    		String bougthCurrency = trn.getField(TRANF_FIELD.TRANF_BOUGHT_CURRENCY.toInt());
    		
    		
    		double tradePrice = trn.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0,  "Trade Price");
    		double spotRate = trn.getFieldDouble(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 0,  "");
    		double dealtRate = trn.getFieldDouble(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 0,  "");
    		OConsole.oprint("\nRates: tradePrice=" +  tradePrice + " spotRate=" + spotRate + " dealtRate=" + dealtRate);
    		Logging.info("\nRates: tradePrice=" +  tradePrice + " spotRate=" + spotRate + " dealtRate=" + dealtRate);
    		
    		String tradeUnit = trn.getField(TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT.toInt());
    		if (tradeUnit.equalsIgnoreCase("Currency")) {
    			tradeUnit = trn.getField(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.toInt());
    		}
    		double exctr = getExcrte (oldTransactionId);
    		
    		if ("ZAR".equals(baseCurrency) || "ZAR".equals(bougthCurrency)) {
    			exctr = 1/exctr;
    		}
    		
    		if (tradeUnit.equalsIgnoreCase("Currency")) {
    			switch (cflowType) {
    			case "Spot": 
    				if ("USD".equals(baseCurrency) && "ZAR".equals(bougthCurrency)) {
    					dataEntries.get(0).m_spotRate = 1;
               			dataEntries.get(0).m_forwardRate = 1;    				        				
               			dataEntries.get(1).m_spotRate = 1/tradePrice;
               			dataEntries.get(1).m_forwardRate = 1/tradePrice;
    				} else if ("ZAR".equals(baseCurrency) && "USD".equals(bougthCurrency)) {
    					dataEntries.get(0).m_spotRate = 1;
               			dataEntries.get(0).m_forwardRate = 1;    				        				
               			dataEntries.get(1).m_spotRate = 1/tradePrice;
               			dataEntries.get(1).m_forwardRate = 1/tradePrice;
    				} else if ("USD".equals(baseCurrency) && !"USD".equals(bougthCurrency)) {
                		dataEntries.get(0).m_spotRate = 1;
                   		dataEntries.get(0).m_forwardRate = 1;    				        				
                		dataEntries.get(1).m_spotRate = tradePrice;
                   		dataEntries.get(1).m_forwardRate = tradePrice;    				
        			} else if (!"USD".equals(baseCurrency) && "USD".equals(bougthCurrency)) {
                		dataEntries.get(0).m_spotRate = tradePrice;
                   		dataEntries.get(0).m_forwardRate = tradePrice;    				
                		dataEntries.get(1).m_spotRate = 1;
                   		dataEntries.get(1).m_forwardRate = 1;
        			} else if (!"USD".equals(baseCurrency) && !"USD".equals(bougthCurrency)) {
                		dataEntries.get(0).m_spotRate = tradePrice;
                   		dataEntries.get(0).m_forwardRate = tradePrice;    				
                		dataEntries.get(1).m_spotRate = exctr;
                   		dataEntries.get(1).m_forwardRate = exctr;
        			}
    				break;
    			case "Forward":
    				if ("USD".equals(baseCurrency) && "ZAR".equals(bougthCurrency)) {
    					dataEntries.get(0).m_spotRate = 1;
               			dataEntries.get(0).m_forwardRate = 1;    				        				
               			dataEntries.get(1).m_spotRate = 1/dealtRate;
               			dataEntries.get(1).m_forwardRate = 1/dealtRate;
    				} else if ("ZAR".equals(baseCurrency) && "USD".equals(bougthCurrency)) {
    					dataEntries.get(0).m_spotRate = 1;
               			dataEntries.get(0).m_forwardRate = 1;		        				
               			dataEntries.get(1).m_spotRate = 1/dealtRate;
               			dataEntries.get(1).m_forwardRate = 1/dealtRate;
    				} else if ("USD".equals(baseCurrency) && ("GBP".equals(bougthCurrency) || "EUR".equals(bougthCurrency))) {
                		dataEntries.get(0).m_spotRate = dealtRate;
                   		dataEntries.get(0).m_forwardRate = dealtRate;    				        				
                		dataEntries.get(1).m_spotRate = 1;
                   		dataEntries.get(1).m_forwardRate = 1;    					
    				} else if ("USD".equals(baseCurrency) && !"USD".equals(bougthCurrency)) {
                		dataEntries.get(0).m_spotRate = 1;
                   		dataEntries.get(0).m_forwardRate = 1;    				        				
                		dataEntries.get(1).m_spotRate = dealtRate;
                   		dataEntries.get(1).m_forwardRate = dealtRate;    				
        			} else if (!"USD".equals(baseCurrency) && "USD".equals(bougthCurrency)) {
                		dataEntries.get(0).m_spotRate = dealtRate;
                   		dataEntries.get(0).m_forwardRate = dealtRate;    				
                		dataEntries.get(1).m_spotRate = 1;
                   		dataEntries.get(1).m_forwardRate = 1;
        			} else if (!"USD".equals(baseCurrency) && !"USD".equals(bougthCurrency)) {
                		dataEntries.get(0).m_spotRate = dealtRate;
                   		dataEntries.get(0).m_forwardRate = dealtRate;    				
                		dataEntries.get(1).m_spotRate = exctr;
                   		dataEntries.get(1).m_forwardRate = exctr;
        			}
    				break;
    			}
    		} else {
        		switch (cflowType) {
        		case "Spot":
        			if ("TOz".equals(tradeUnit) && ("USD".equals(baseCurrency) || "USD".equals(bougthCurrency))) {
                		dataEntries.get(0).m_spotRate = tradePrice;
                   		dataEntries.get(0).m_forwardRate = tradePrice;    				
                		dataEntries.get(1).m_spotRate = 1;
                   		dataEntries.get(1).m_forwardRate = 1;    				
                   		
        			} else if ("TOz".equals(tradeUnit) && !"USD".equals(baseCurrency) && !"USD".equals(bougthCurrency)) {
                		dataEntries.get(0).m_spotRate = tradePrice*exctr;
                   		dataEntries.get(0).m_forwardRate = tradePrice*exctr;    				
                		dataEntries.get(1).m_spotRate = exctr;
                   		dataEntries.get(1).m_forwardRate = exctr;    				
        			} else if (!"TOz".equals(tradeUnit) && ("USD".equals(baseCurrency) || "USD".equals(bougthCurrency))) {
                		dataEntries.get(0).m_spotRate = spotRate;
                   		dataEntries.get(0).m_forwardRate = spotRate;
                		dataEntries.get(1).m_spotRate = 1;
                   		dataEntries.get(1).m_forwardRate = 1;    				
        			} else if (!"TOz".equals(tradeUnit) && !"USD".equals(baseCurrency) && !"USD".equals(bougthCurrency)) {
                		dataEntries.get(0).m_spotRate = spotRate*exctr;
                   		dataEntries.get(0).m_forwardRate = spotRate*exctr;
                		dataEntries.get(1).m_spotRate = exctr;
                   		dataEntries.get(1).m_forwardRate = exctr;    				
        			} else {
        				String message = "FX Spot assignment logic undfined";
        				Logging.error(message);
        				OConsole.oprint(message);
        				throw new OException (message);
        			}
        			break;
        		case "Forward":
        			if ("TOz".equals(tradeUnit) && ("USD".equals(baseCurrency) || "USD".equals(bougthCurrency))) {
                		dataEntries.get(0).m_spotRate = dealtRate;
                   		dataEntries.get(0).m_forwardRate = dealtRate;    				
                		dataEntries.get(1).m_spotRate = 1;
                   		dataEntries.get(1).m_forwardRate = 1;    				
                   		
        			} else if ("TOz".equals(tradeUnit) && !"USD".equals(baseCurrency) && !"USD".equals(bougthCurrency)) {
                		dataEntries.get(0).m_spotRate = dealtRate*exctr;
                   		dataEntries.get(0).m_forwardRate = dealtRate*exctr;    				
                		dataEntries.get(1).m_spotRate = exctr;
                   		dataEntries.get(1).m_forwardRate = exctr;
        			} else if (!"TOz".equals(tradeUnit) && ("USD".equals(baseCurrency) || "USD".equals(bougthCurrency))) {
                		dataEntries.get(0).m_spotRate = dealtRate;
                   		dataEntries.get(0).m_forwardRate = dealtRate;
                		dataEntries.get(1).m_spotRate = 1;
                   		dataEntries.get(1).m_forwardRate = 1;
        			} else if (!"TOz".equals(tradeUnit) && !"USD".equals(baseCurrency) && !"USD".equals(bougthCurrency)) {
                		dataEntries.get(0).m_spotRate = dealtRate*exctr;
                   		dataEntries.get(0).m_forwardRate = dealtRate*exctr;
                		dataEntries.get(1).m_spotRate = exctr;
                   		dataEntries.get(1).m_forwardRate = exctr;    				
        			} else {
        				String message = "FX Forward assignment logic undfined";
        				Logging.error(message);
        				OConsole.oprint(message);
        				throw new OException (message);
        			}    			
        			break;
        		}    			
    		}
     	}
    	
    	return dataEntries;
    }

    private double getExcrte(int oldTransactionId) throws OException {
    	String sql = 
    			"\nSELECT md.excrte"
    		+	"\nFROM " + ConfigurationItem.USER_TABLE_1.getValue() + " md"
    		+	"\nWHERE md.deal_id = " + oldTransactionId
    		;
    	Table tab = null;
    	try {
    		tab = Table.tableNew("exctre value retrieved for " + oldTransactionId + " from " + ConfigurationItem.USER_TABLE_1.getValue());
    		int ret = DBaseTable.execISql(tab, sql);
    		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
    			String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Error exeucting SQL " + sql + ":\n");
        		Logging.error(errorMessage);
    			OConsole.message(errorMessage);
        		throw new OException (errorMessage);
    		}
    		if  (tab.getNumRows() < 1) {
    			String errorMessage = "Could not find row for deal_id=" + oldTransactionId + " in user table " + 
    					ConfigurationItem.USER_TABLE_1.getValue();
        		Logging.error(errorMessage);
    			OConsole.oprint(errorMessage);
    			throw new OException (errorMessage);    			
    		}
    		if (tab.getNumRows() > 1) {
    			String errorMessage = "Found more than one row for deal_id=" + oldTransactionId + " in user table " + 
    					ConfigurationItem.USER_TABLE_1.getValue();
        		Logging.error(errorMessage);
        		OConsole.oprint(errorMessage);
    			throw new OException (errorMessage);
    		}
    		String excrte = tab.getString(1, 1);
    		
    		return Double.parseDouble(excrte);
    	} finally {
    		if (tab != null) {
    			tab.destroy();
    			tab = null;
    		}
    	}
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
    	   	
    	dataEntries.get(0).m_uniqueID = new PNL_EntryDataUniqueID(dealNum, 0, 0, 0);
    	dataEntries.get(0).m_tradeDate = tradeDate;
    	dataEntries.get(0).m_fixingDate = fixingDate;
    	dataEntries.get(0).m_metalCcy = MTL_Position_Utilities.getCcyForIndex(projIdx);
    	dataEntries.get(0).m_indexID = projIdx; 

    	
    	if (!criticalFieldsOnly)
    	{
    		double price = trn.getFieldDouble(TRANF_FIELD.TRANF_PRICE.toInt());
    		dataEntries.get(0).m_spotRate = price;
    		dataEntries.get(0).m_forwardRate = price;
        	dataEntries.get(0).m_usdDF = 1;
    	}
    	
    	dataEntries.get(1).m_uniqueID = new PNL_EntryDataUniqueID(dealNum, 1, 0, 0);
    	dataEntries.get(1).m_tradeDate = tradeDate;
    	dataEntries.get(1).m_fixingDate = fixingDate;
    	dataEntries.get(1).m_metalCcy = 0;
    	dataEntries.get(1).m_indexID = 0;
    	dataEntries.get(1).m_spotRate = 1.0;
    	dataEntries.get(1).m_forwardRate = 1.0;
    	dataEntries.get(1).m_usdDF = dataEntries.get(0).m_usdDF;
    	
    	return dataEntries;    	
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

