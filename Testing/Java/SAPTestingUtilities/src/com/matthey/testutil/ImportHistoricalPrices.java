package com.matthey.testutil;

import com.matthey.testutil.exception.SapTestUtilException;
import com.olf.openjvs.Index;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.logging.PluginLog;

public class ImportHistoricalPrices extends BulkOperationScript{

	@Override
	public Table generateInputData() throws OException {
		return getArgt();
	}

	@Override
	public Table performBulkOperation(Table tblInput) throws OException,
			SapTestUtilException {
		PluginLog.info(" Started Importing Historical Prices");
		String inputCSVPath = tblInput.getString("csv_path_for_historical_prices", 1);
		PluginLog.debug("The input csv path :"+inputCSVPath);
		
		Table importTableHeader = null, importTable = null, errorTable = null;
		int file_status = 0,import_status = 0;
		
		try{
			importTableHeader = Table.tableNew();
			importTable = Table.tableNew("idx_historical");
			errorTable = Table.tableNew();
	
	        importTable.addCol( "index_id", COL_TYPE_ENUM.COL_STRING);
	        importTable.addCol( "date", COL_TYPE_ENUM.COL_INT);
	        importTable.addCol( "start_date", COL_TYPE_ENUM.COL_INT);
	        importTable.addCol( "end_date", COL_TYPE_ENUM.COL_INT);
	        importTable.addCol( "yield_basis", COL_TYPE_ENUM.COL_STRING);
	        importTable.addCol( "ref_source", COL_TYPE_ENUM.COL_STRING);
	        importTable.addCol( "index_location", COL_TYPE_ENUM.COL_STRING);
	        importTable.addCol( "price", COL_TYPE_ENUM.COL_DOUBLE);
	        
	        file_status = importTableHeader.loadTableFromFileWithHeader(importTable, inputCSVPath);
			PluginLog.debug("The file status :"+file_status);
	        if (file_status == 1){
				import_status = Index.tableImportHistoricalPrices(importTable, errorTable);
				PluginLog.debug("The import status "+import_status);
				if(import_status == 0){
					throw new OException("\nError importing historical prices");
				}
			} else {
				throw new OException("\nError loading File - " + inputCSVPath);
			}
		}
		catch (OException oe){
			OConsole.oprint(oe.getMessage());
			
			Util.exitFail();
			
		}
		finally{
			if(importTableHeader != null)
			{
				importTableHeader.destroy();
			}
			if(importTable != null) 
			{
				importTable.destroy();
			}
			if(errorTable != null) 
			{
				errorTable.destroy();
			}
		}
		PluginLog.info("Ended Import Historical Prices");
		return importTable;
	}

	@Override
	public String getOperationName() {
		
		return "Import Historical Prices";
	}

}
