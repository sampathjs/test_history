package com.olf.jm;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.logging.PluginLog;

public class SaveIndex implements IScript {

	public void execute(IContainerContext context) throws OException
	{
		setUpLog();
		
		PluginLog.debug("START MarketDataLoaderSaveIndex");
		
		String strMessage;
		
		try
		{
			String strRefSrc;
			
			Table tblArgt = context.getArgumentsTable();
			strRefSrc =  tblArgt.getString("ref_src",1);

			String strSQL = "SELECT "
					+ "urs.* "
					+ ",ipx.price "
					+ "FROM  "
					+ "USER_jm_ref_src urs "
					+ "INNER JOIN idx_def idx on idx.index_name = urs.src_idx and db_status = 1 "
					+ "inner join idx_historical_prices ipx on ipx.index_id = idx.index_id "
					+ "WHERE "
					+ "ipx.last_update >= '" + OCalendar.formatDateInt(OCalendar.today()) +"' "
					+ "and urs.ref_src = '" + strRefSrc + "'";

			Table tblPrices = Table.tableNew();
			DBaseTable.execISql(tblPrices, strSQL);
			
			Table tblTargetIdx = Table.tableNew();
			
			String strWhat = "DISTINCT,target_idx";
			String strWhere = "target_idx NE ''";
			tblTargetIdx.select(tblPrices,strWhat, strWhere);
			
			for(int i=1;i<=tblTargetIdx.getNumRows();i++){
				
				String strTargetIdx =  tblTargetIdx.getString("target_idx",i);
				
				strWhat = "*";
				strWhere = "target_idx EQ " +strTargetIdx;
				
				Table tblCurrTarget = Table.tableNew();
				tblCurrTarget.select(tblPrices,strWhat,strWhere);
				
				int intIndexID = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, strTargetIdx);
				
				Table tblIdxGpts = Index.loadAllGpts (intIndexID);
				
				if (Index.calc (intIndexID) < 1)
				{
					strMessage = "Failed to calculate output values for index #" + intIndexID;
					PluginLog.error (strMessage);
				}
				
				
				tblIdxGpts.setColValDouble("input.bid", 0.0);
				tblIdxGpts.setColValDouble("input.mid", 0.0);
				tblIdxGpts.setColValDouble("input.offer", 0.0);
				
				tblIdxGpts.setColValDouble("effective.bid", 0.0);
				tblIdxGpts.setColValDouble("effective.mid", 0.0);
				tblIdxGpts.setColValDouble("effective.offer", 0.0);
				

				strWhat = "price(input.bid),price(input.mid),price(input.offer)";
				strWhere = "metal EQ $name";
				tblIdxGpts.select(tblCurrTarget,strWhat,strWhere);
				
				PluginLog.debug("\nUpdate grid points for index " + strTargetIdx);
				if (Index.updateGpts (tblIdxGpts, BMO_ENUMERATION.BMO_MID, 0, 1, OCalendar.today(),Ref.getValue(SHM_USR_TABLES_ENUM.IDX_MARKET_DATA_TYPE_TABLE,strRefSrc )) < 1)
				{
					strMessage = "Failed to update index: " + Ref.getName(SHM_USR_TABLES_ENUM.INDEX_TABLE, intIndexID);
					PluginLog.error (strMessage);
				
				}
				else{
					
					PluginLog.info("Succesfully updated gridpoints for index " + strTargetIdx + " ref source " + strRefSrc);
				}
				
				tblCurrTarget.destroy();
				tblIdxGpts.destroy();
				
			}
			
			// Retrieve LME GBP and LME USD prices and divide to create historical price entry
			importHistoricalPrices(tblPrices,strRefSrc);
			
			tblPrices.destroy();
			tblTargetIdx.destroy();
			
		}catch(Exception e){
			
			PluginLog.info("Caught exception " + e.toString());
		}
		
		PluginLog.debug("END MarketDataLoaderSaveIndex");
		
	}
	
	
	private void setUpLog() throws OException {
		
    	String logDir   = Util.reportGetDirForToday();
    	String logFile = this.getClass().getSimpleName() + ".log";
    	
		try{
			PluginLog.init("DEBUG", logDir, logFile );	
		}
		catch(Exception e){
			
        	String msg = "Failed to initialise log file: " + Util.reportGetDirForToday() + "\\" + logFile;
        	throw new OException(msg);
		}
	}
	
	
	
	private void importHistoricalPrices( Table tblPrices, String strRefSrc) throws OException {
		Table importTable = null;
//		Map<Integer, Integer> refSource2Holiday= DBHelper.retrieveRefSourceToHolidayMapping();
//		int holId;
//		if (!refSource2Holiday.containsKey(refSourceId)) {
//			holId = defaultHolId;			
//			PluginLog.warn ("There is no mapping for ref source id #" + refSourceId +	" to a holiday id defined in table " + DBHelper.USER_JM_PRICE_WEB_REF_SOURCE_HOL );
//		} else {
//			holId = refSource2Holiday.get(refSourceId);
//		}
		
		String strWhat;
		String strWhere;
		
		strWhat = "*";
		strWhere = "metal EQ XPD";
		
		Table tblCurrMtlPrices = Table.tableNew();
		
		try{
			
			tblCurrMtlPrices.select(tblPrices, strWhat, strWhere);
			
			double dblEURAmount = 0.0;
			double dblGBPAmount = 0.0;
			double dblUSDAmount = 0.0;
			
			for(int i = 1;i<=tblCurrMtlPrices.getNumRows();i++){
					
					if(tblCurrMtlPrices.getString("ccy", i).equals("EUR")){
						
						dblEURAmount=tblCurrMtlPrices.getDouble("price",i);
					}
					
					if(tblCurrMtlPrices.getString("ccy", i).equals("GBP")){
						
						dblGBPAmount=tblCurrMtlPrices.getDouble("price",i);
					}

					if(tblCurrMtlPrices.getString("ccy", i).equals("USD")){
						
						dblUSDAmount=tblCurrMtlPrices.getDouble("price",i);
					}
			}

			
			// FX_GBP.USD = dblUSDAmount/dblGBPAmount
			// FX_EUR.USD = dblUSDAmount/dblEURAmount
			
			double dblGBPUSD = 0.0;
			if(dblUSDAmount != 0.0 && dblGBPAmount != 0.0){
				dblGBPUSD = dblUSDAmount/dblGBPAmount;
			}
			
			if(dblGBPUSD != 0.0){
				saveHistorical(dblGBPUSD, "FX_GBP.USD", strRefSrc) ;	
			}
			
			double dblEURUSD = 0.0;
			if(dblUSDAmount != 0.0 && dblEURAmount != 0.0){
				dblEURUSD = dblUSDAmount/dblEURAmount;
			}
			
			if(dblEURUSD != 0.0){
				
				saveHistorical(dblEURUSD, "FX_EUR.USD", strRefSrc) ;
			}
			
			
		}catch(Exception e){
			
			PluginLog.info("Caught exception " + e.getMessage());
		}

		
		tblCurrMtlPrices.destroy();
	}


	
	private void saveHistorical(double dblPrice, String strIndexName, String strRefSrc) throws OException {
		
		int holId = 0;
		int today = OCalendar.today();
		
		int refSourceId = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE,strRefSrc);
		
		//String indexName = "FX_EUR.USD"; //FX_GBP.USD

		int targetIndexId = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, strIndexName);
		Table importTable = Table.tableNew("New Historical Prices for " + strIndexName);
		
		Table errorLog = Table.tableNew();
		importTable.addCol( "index_id", COL_TYPE_ENUM.COL_INT);
		importTable.addCol( "date", COL_TYPE_ENUM.COL_INT);
		importTable.addCol( "start_date", COL_TYPE_ENUM.COL_INT);
		importTable.addCol( "end_date", COL_TYPE_ENUM.COL_INT);
		importTable.addCol( "yield_basis", COL_TYPE_ENUM.COL_INT);
		importTable.addCol( "ref_source", COL_TYPE_ENUM.COL_INT);
		importTable.addCol( "index_location", COL_TYPE_ENUM.COL_INT);
		importTable.addCol( "price", COL_TYPE_ENUM.COL_DOUBLE);			

		int importRow = importTable.addRow();
		int spotDay = OCalendar.today();
		
		importTable.setInt("index_id", importRow, targetIndexId);
		
		importTable.setInt("date", importRow, today);
		importTable.setInt("start_date", importRow, spotDay);
		importTable.setInt("end_date", importRow, spotDay);
		importTable.setInt("ref_source", importRow, refSourceId);
		importTable.setInt("yield_basis", importRow, 0);
		importTable.setInt("index_location", importRow, 0);


		importTable.setDouble("price", importRow, dblPrice);
		

		int ret = Index.tableImportHistoricalPrices(importTable, errorLog);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			PluginLog.info("Error saving historical prices for " + strIndexName);
			throw new OException ("Error importing historical prices for " + strIndexName);
		}
		else{
			PluginLog.info("Succesfully saved historical prices for " + strIndexName);
		}

		errorLog.destroy();	
		importTable.destroy();
	}
	
}
