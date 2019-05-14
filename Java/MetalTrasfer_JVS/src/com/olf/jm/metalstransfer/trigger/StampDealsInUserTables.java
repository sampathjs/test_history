package com.olf.jm.metalstransfer.trigger;

import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.logging.PluginLog;
import com.olf.openjvs.Util;

public class StampDealsInUserTables implements IScript {

	public void execute(IContainerContext context) throws OException {
		Table DealstoProcess = Util.NULL_TABLE;
		
		try {
			Utils.initialiseLog(Constants.Stamp_LOG_FILE);
			ODateTime extractDateTime;
			extractDateTime = ODateTime.getServerCurrentDateTime();
			PluginLog.info("Fetching Strategy deal created on "	+ extractDateTime);	
			DealstoProcess = fetchdeals();
			PluginLog.info(DealstoProcess.getNumRows()+" will be stamped in USER_strategy_deals");
			formatTable(DealstoProcess, extractDateTime);
			PluginLog.info("User table updated with strategy deals");				
		} catch (OException oe) {
			PluginLog.error("DBUserTable.saveUserTable() failed"+ oe.getMessage());
			throw oe;
		} finally {
			if (Table.isTableValid(DealstoProcess) == 1) {
				DealstoProcess.destroy();
			}
		}
	}
//Add columns to user table
protected Table formatTable(Table DealstoProcess,ODateTime extractDateTime)throws OException{
	try {
		
		DealstoProcess.addCol("status", COL_TYPE_ENUM.COL_STRING);
		DealstoProcess.addCol("last_updated", COL_TYPE_ENUM.COL_DATE_TIME);
		DealstoProcess.setColValString("status", "Pending");
		DealstoProcess.setColValDateTime("last_updated", extractDateTime);
		DBUserTable.insert(DealstoProcess);
	} catch (OException oe) {
		PluginLog.error("Unable to add column to table " + oe.getMessage());
		throw oe;
	}
	return DealstoProcess;
}
	
// Fetch Deals to be stamped
	protected Table fetchdeals() throws OException{
		Table tbldata = Util.NULL_TABLE;
		try{
			String sqlQuery = "SELECT ab.deal_tracking_num as deal_num,ab.tran_num,ab.tran_status,ab.version_number  FROM ab_tran ab\n" +
							  " WHERE ab.tran_type = "+ TRAN_TYPE_ENUM.TRAN_TYPE_TRADING_STRATEGY.toInt() + "\n" + 
							  "   AND ab.ins_type = " + INS_TYPE_ENUM.strategy.toInt() + "\n" +
							  "   AND ab.toolset = "  + TOOLSET_ENUM.COMPOSER_TOOLSET.toInt() + "\n" +
							  "   AND ab.tran_status in ("+ TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+ ","+ TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt()+ ")\n" +
							  "   AND ab.Current_flag = 1 \n" +
							  " AND ab.deal_tracking_num not in (select deal_num from USER_strategy_deals)";
					
			tbldata = Table.tableNew("USER_strategy_deals");
			PluginLog.info("Fetching Strategy deals for stamping in User table USER_strategy_deals");
			// ALL strategy deals which are not stamped in User table with trans_status NEW and Cancelled
			
			int ret = DBaseTable.execISql(tbldata, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.warn(DBUserTable.dbRetrieveErrorInfo(ret, "Failed to save in  User table USER_strategy_deals "));
			}
		}catch (OException oe) {
			PluginLog.error("DBUserTable  USER_strategy_deals failed" + oe.getMessage());
			throw oe;
		}
		return tbldata;
	}
}
