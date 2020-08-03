package com.olf.jm;

import java.util.Map;
import java.util.TreeMap;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

public class SaveIndexBFIX implements IScript {

	public void execute(IContainerContext context) throws OException
	{
		setUpLog();
		
		Logging.debug("START SaveIndexBFIX");
		
		try
		{
			String strRefSrc;
			
			Table tblArgt = context.getArgumentsTable();
			strRefSrc =  tblArgt.getString("ref_src",1);

			String strSQL;
			strSQL = "SELECT \n";
			strSQL += "urs.* \n";
			strSQL += ",ipx.price \n";
			strSQL += "FROM \n";
			strSQL += "USER_jm_ref_src urs \n";
			strSQL += "INNER JOIN idx_def s_idx on s_idx.index_name = urs.src_idx and db_status = 1 \n";
			strSQL += "LEFT JOIN idx_historical_prices ipx on ipx.index_id = s_idx.index_id and ipx.last_update >= '" + OCalendar.formatDateInt(OCalendar.today()) +"' \n";
			strSQL += "WHERE \n";
			strSQL += "urs.ref_src = '" + strRefSrc + "' \n";
			

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
			
			Logging.info("Caught exception " + e.toString());
		}finally{
			Logging.debug("END SaveIndexBFIX");
			Logging.close();
		}
		
		
		
	}
	
	
	private void setUpLog() throws OException {
		try {
			ConstRepository repository = new ConstRepository("MiddleOffice", "MDT_RefSources");
			String abOutdir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
			 
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = repository.getStringValue("logLevel", "DEBUG"); 
			String logFile = this.getClass().getSimpleName() + ".log";
			String logDir = repository.getStringValue("logDir", abOutdir);
			try {
				Logging.init( this.getClass(), "MiddleOffice", "MDT_RefSources");
			} catch (Exception e) {
				String msg = "Failed to initialise log file: " + logDir + "\\" + logFile;
	        	throw new OException(msg);
			}			
		} catch (OException ex) {
			throw new RuntimeException ("Error initializing the ConstRepo", ex);
		}
		
	}
	
	
	
	private void saveHistorical(double dblPrice, String strIndexName, String strRefSrc) throws OException {
		
		int holId = 0;
		int today = OCalendar.today();
		
		int refSourceId = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE,strRefSrc);

		
		Map<Integer, Integer> refSource2Holiday= retrieveRefSourceToHolidayMapping();

		if (!refSource2Holiday.containsKey(refSourceId)) {
			holId = 0;			
			Logging.warn ("There is no mapping for ref source id #" + refSourceId +	" to a holiday id defined in table "  );
		} else {
			holId = refSource2Holiday.get(refSourceId);
		}

		
		
		
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
		int spotDay = OCalendar.parseStringWithHolId("2d", holId, today);
		
		importTable.setInt("index_id", importRow, targetIndexId);
		
		importTable.setInt("date", importRow, today);
		importTable.setInt("start_date", importRow, spotDay);
		importTable.setInt("end_date", importRow, spotDay);
		importTable.setInt("ref_source", importRow, refSourceId);
		importTable.setInt("yield_basis", importRow, 0);
		importTable.setInt("index_location", importRow, 0);


		importTable.setDouble("price", importRow, dblPrice);
		

		int ret = Index.tableImportHistoricalPrices(importTable, errorLog);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			Logging.info("Error saving historical prices for " + strIndexName);
			throw new OException ("Error importing historical prices for " + strIndexName);
		}
		else{
			Logging.info("Succesfully saved historical prices for " + strIndexName);
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
				Logging.error(message);
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
