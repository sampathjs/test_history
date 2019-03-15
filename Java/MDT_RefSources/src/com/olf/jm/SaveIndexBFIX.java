package com.olf.jm;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.logging.PluginLog;

public class SaveIndexBFIX implements IScript {

	public void execute(IContainerContext context) throws OException
	{
		setUpLog();
		
		PluginLog.debug("START SaveIndexBFIX");
		
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

			
			for(int i =1;i<=tblPrices.getNumRows();i++){
				
				double dblPrice = 0.0;
				String strIndexName;
				
				dblPrice = tblPrices.getDouble("price",i);
				strIndexName = tblPrices.getString("target_idx",i);
				
				saveHistorical(dblPrice, strIndexName, strRefSrc);
				
			}
			
			
			
		}catch(Exception e){
			
			PluginLog.info("Caught exception " + e.toString());
		}
		
		PluginLog.debug("END SaveIndexBFIX");
		
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
