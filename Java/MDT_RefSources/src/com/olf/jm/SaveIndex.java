package com.olf.jm;

import java.util.Map;
import java.util.TreeMap;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

public class SaveIndex implements IScript {

	public void execute(IContainerContext context) throws OException
	{
		setUpLog();
		
		PluginLog.debug("START MarketDataLoaderSaveIndex");
		
		String strRefSrc;
		
		Table tblArgt = context.getArgumentsTable();
		strRefSrc =  tblArgt.getString("ref_src",1);


		PluginLog.info("Saving prices for ref_src " + strRefSrc);
		savePrices(strRefSrc);

			
		PluginLog.info("Saving derived fx rates for ref src " + strRefSrc );
		saveDerivedFXRates(strRefSrc);
		
		PluginLog.debug("END MarketDataLoaderSaveIndex");
		
	}
	
	private void savePrices(String strRefSrc) {

		try
		{

			String strMessage;
			
			// Get prices pending to be saved from today's import  
			String strSQL;
			strSQL = "SELECT \n";
			strSQL += "urs.* \n";
			strSQL += ",ipx.price \n";
			strSQL += ",imd.dataset_time \n";
			strSQL += "FROM \n";
			strSQL += "USER_jm_ref_src urs \n";
			strSQL += "INNER JOIN idx_def s_idx on s_idx .index_name = urs.src_idx and s_idx.db_status = 1 \n";
			strSQL += "INNER JOIN idx_market_data_type imdt on imdt.name = urs.ref_src \n";
			strSQL += "INNER JOIN idx_def t_idx on t_idx.index_name = urs.target_idx and t_idx.db_status = 1 \n";
			strSQL += "LEFT JOIN idx_historical_prices ipx on ipx.index_id = s_idx.index_id and ipx.last_update >= '" + OCalendar.formatDateInt(OCalendar.today()) +"' \n";
			strSQL += "LEFT JOIN idx_market_data imd on t_idx.index_id = imd.index_id  and imd.dataset_type = imdt.id_number and imd.dataset_time >= '" + OCalendar.formatDateInt(OCalendar.today()) +"' \n";
			strSQL += "WHERE \n";
			strSQL += "urs.ref_src = '" + strRefSrc + "' \n";
			strSQL += "and dataset_time is null \n";
			strSQL += "order by target_idx \n";;
			
			Table tblPrices = Table.tableNew();
			DBaseTable.execISql(tblPrices, strSQL);

			PluginLog.info("Found " + tblPrices.getNumRows() + " imported prices pending a save for ref source " + strRefSrc + " on " + OCalendar.formatDateInt(OCalendar.today()));
			
			
			// Get list of Target Index (NON_JM curve) needed to be saved
			Table tblTargetIdx = Table.tableNew();
			
			String strWhat = "DISTINCT,target_idx";
			String strWhere = "target_idx NE ''";
			tblTargetIdx.select(tblPrices,strWhat, strWhere);
						

			// Loop through each target index (NON_JM curve) and save prices against them 
			for(int i=1;i<=tblTargetIdx.getNumRows();i++){
				
				String strTargetIdx =  tblTargetIdx.getString("target_idx",i);
				PluginLog.info("Processing prices for index " + strTargetIdx  + " for ref src " + strRefSrc);
				

				// Get price relevant for current target index
				strWhat = "*";
				strWhere = "target_idx EQ " +strTargetIdx;
				Table tblCurrTarget = Table.tableNew();
				tblCurrTarget.select(tblPrices,strWhat,strWhere);
				
				// validation checks
				
				// Check all prices imported for ref_src and target_idx
				boolean blnAllPricesImported = PriceImportReporting.allPricesImported(strRefSrc,strTargetIdx, OCalendar.today());
				if(blnAllPricesImported == false){
					PluginLog.info("Missing prices found for " + strTargetIdx +  " " + strRefSrc);
				}
				
				// Check that prices today are not the same as yesterdays prices 
				boolean blnYestPricesDifferent = PriceImportReporting.areYestPricesDifferent(strRefSrc, strTargetIdx, OCalendar.today());
				if(blnYestPricesDifferent == false){
					PluginLog.info("Yesterdays prices the same for " + strTargetIdx +  " " + strRefSrc);
				}
				
				if(blnAllPricesImported == true && blnYestPricesDifferent == true){
					
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
					if (Index.updateGpts (tblIdxGpts, BMO_ENUMERATION.BMO_MID, 0, 1, OCalendar.today(),Ref.getValue(SHM_USR_TABLES_ENUM.IDX_MARKET_DATA_TYPE_TABLE,strRefSrc )) < 1){
						setUpLog();
						strMessage = "Failed to update index: " + Ref.getName(SHM_USR_TABLES_ENUM.INDEX_TABLE, intIndexID);
						PluginLog.error (strMessage);
					
					}
					else{
						
						setUpLog();
						PluginLog.info("Succesfully updated gridpoints for index " + strTargetIdx + " ref source " + strRefSrc);
					}
					tblIdxGpts.destroy();
				}
				else{
					//PluginLog.debug("\n Missing or stale imported prices for ref src " + strRefSrc + " for index" + strTargetIdx + " - Prices will not saved/distributed for this index until all the latest prices exist.");
					PluginLog.debug("\nPrices will not saved/distributed for this index until all the latest prices exist.");
				}
				tblCurrTarget.destroy();
			}
			
			tblPrices.destroy();
			tblTargetIdx.destroy();

		}catch(Exception e){
			
			PluginLog.info("Caught exception " + e.toString());
		}
		
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
	
	
	
	private void saveDerivedFXRates( String strRefSrc) throws OException {
		
		String strWhat;
		String strWhere;
		
		
		Table tblSrcMtlPrices = Table.tableNew();
		
		Table tblPrices = Table.tableNew();
		
		try{

			String strSQL;
			strSQL = "SELECT \n";
			strSQL += "urs.* \n";
			strSQL += ",ipx.price \n";
			strSQL += "FROM \n";
			strSQL += "USER_jm_ref_src urs \n";
			strSQL += "INNER JOIN idx_def s_idx on s_idx .index_name = urs.src_idx and s_idx.db_status = 1 \n";
			strSQL += "LEFT JOIN idx_historical_prices ipx on ipx.index_id = s_idx.index_id and ipx.last_update >= '" + OCalendar.formatDateInt(OCalendar.today()) +"' \n";
			strSQL += "WHERE \n";
			strSQL += "urs.ref_src = '" + strRefSrc + "' \n";
			strSQL += "order by target_idx \n";;
			
			tblPrices = Table.tableNew();
			DBaseTable.execISql(tblPrices, strSQL);

			PluginLog.info("Found " + tblPrices.getNumRows() + " imported prices to save a derived FX rate for ref source " + strRefSrc + " on " + OCalendar.formatDateInt(OCalendar.today()));
			
			boolean blnAllPricesImported = PriceImportReporting.allPricesImported(strRefSrc,"", OCalendar.today());
			
			if(blnAllPricesImported == true){
				
				String strSrcMetalRate="";
				
				if(strRefSrc.equals("LME AM") || strRefSrc.equals("LME PM")){
					
					strSrcMetalRate = "XPT"; 
				}
				else if(strRefSrc.equals("LBMA AM") || strRefSrc.equals("LBMA PM")){
				
					strSrcMetalRate = "XAU";
				}
				 if(strRefSrc.equals("LBMA Silver")){
					 
					 strSrcMetalRate = "XAG";
				}
				
				if((Str.isNull(strSrcMetalRate) == 0) && !strSrcMetalRate.isEmpty()){
					
					strWhat = "*";
					strWhere = "metal EQ " + strSrcMetalRate;
					tblSrcMtlPrices.select(tblPrices, strWhat, strWhere);
					
					double dblEURAmount = 0.0;
					double dblGBPAmount = 0.0;
					double dblUSDAmount = 0.0;
					
					for(int i = 1;i<=tblSrcMtlPrices.getNumRows();i++){
							
							if(tblSrcMtlPrices.getString("ccy", i).equals("EUR")){
								
								dblEURAmount=tblSrcMtlPrices.getDouble("price",i);
							}
							
							if(tblSrcMtlPrices.getString("ccy", i).equals("GBP")){
								
								dblGBPAmount=tblSrcMtlPrices.getDouble("price",i);
							}

							if(tblSrcMtlPrices.getString("ccy", i).equals("USD")){
								
								dblUSDAmount=tblSrcMtlPrices.getDouble("price",i);
							}
					}

					
					// FX_GBP.USD = dblUSDAmount/dblGBPAmount
					double dblGBPUSD = 0.0;
					if(dblUSDAmount != 0.0 && dblGBPAmount != 0.0){
						dblGBPUSD = dblUSDAmount/dblGBPAmount;
					}
					
					if(dblGBPUSD != 0.0){
						saveHistorical(dblGBPUSD, "FX_GBP.USD", strRefSrc) ;	
					}
					
					
					// FX_EUR.USD = dblUSDAmount/dblEURAmount
					double dblEURUSD = 0.0;
					if(dblUSDAmount != 0.0 && dblEURAmount != 0.0){
						dblEURUSD = dblUSDAmount/dblEURAmount;
					}
					
					if(dblEURUSD != 0.0){
						
						saveHistorical(dblEURUSD, "FX_EUR.USD", strRefSrc) ;
					}
				}
				else{
					PluginLog.info("Ref Src not supported for derived fx rate ");
				}

			}else{
				
				PluginLog.info("Not all prices saved for " + strRefSrc + " - unable to save derived fx rates");
			}
			
			
		}catch(Exception e){
			
			PluginLog.info("Caught exception " + e.getMessage());
		}

		tblPrices.destroy();
		tblSrcMtlPrices.destroy();
	}


	
	private void saveHistorical(double dblPrice, String strIndexName, String strRefSrc) throws OException {
		
		int holId = 0;
		int today = OCalendar.today();
		
		int refSourceId = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE,strRefSrc);

		
		Map<Integer, Integer> refSource2Holiday= retrieveRefSourceToHolidayMapping();

		if (!refSource2Holiday.containsKey(refSourceId)) {
			holId = 0;			
			PluginLog.warn ("There is no mapping for ref source id #" + refSourceId +	" to a holiday id defined in table "  );
		} else {
			holId = refSource2Holiday.get(refSourceId);
		}

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
	
	
	public static Map<Integer, Integer> retrieveRefSourceToHolidayMapping () throws OException {
		String sql = 
				"\nSELECT map.ref_source, map.holiday_id"
			+	"\nFROM USER_jm_price_web_ref_source_hol map";
		
		Map<Integer, Integer> map = new TreeMap<>();
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret < 1) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing sql " + sql);
				PluginLog.error(message);
				throw new OException (message);
			}
			for (int row=sqlResult.getNumRows(); row >= 1;row--) {
				int refSource = sqlResult.getInt("ref_source", row);
				int holId = sqlResult.getInt("holiday_id", row);
				map.put(refSource, holId);
			}
		} finally {
			if (sqlResult != null) {
				sqlResult = TableUtilities.destroy(sqlResult);
			}
		}
		return map;
	}
	
}
