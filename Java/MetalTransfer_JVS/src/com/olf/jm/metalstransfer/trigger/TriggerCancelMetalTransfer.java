package com.olf.jm.metalstransfer.trigger;

import java.util.List;


import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;
/*
 * History:
 * 2020-06-27   V1.0    VishwN01	- Adding deleted status to be picked for cancellation of Cash Deal
 */
public class TriggerCancelMetalTransfer extends MetalTransferTriggerScript {
	public TriggerCancelMetalTransfer() throws OException {
	}	
	protected void init() throws OException {
		Utils.initialiseLog(this.getClass().getSimpleName().toString()+".log");
	}
	//No Cash trades available, stamp status to "Succeeded" in User Table
	protected String processTranNoCashTrade(int trannum) throws OException {
		PluginLog.info("No Cash deal was retrieve for cancellation against " + trannum);
		return "Succeeded";

	}

	//Using API to cancel Cash trades against the Strategy deals
	protected String processTranWithCashTrade(List<Integer> cashDealList) {
		Table version = Util.NULL_TABLE;
		try {
			int countCash = cashDealList.size();
			for (int rowCount = 0; rowCount < countCash; rowCount++) {
				int tranNum = cashDealList.get(rowCount);
				PluginLog.info("Cancellation process started for "+tranNum);
				 version = Table.tableNew();
				//Transaction tran = Transaction.retrieve(tranNum);
				//tran.setField(TRANF_FIELD.TRANF_TRAN_STATUS.toInt(),0,"", 5);
				String str = "SELECT MAX(version_number) FROM ab_tran WHERE tran_num = "+tranNum + " AND current_flag = 1";
				int ret = DBaseTable.execISql(version, str);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret,"Failed while getting max version number"));
				}
				int ver_num = version.getInt(1, 1);
				int ret1 = Transaction.cancel(tranNum, ver_num);
				if (ret1 != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
					PluginLog.error("Error while cancelling the deal" +tranNum);
				}
					
				PluginLog.info("TranNum  " + tranNum	+ "  Cancelled as Strategy was Cancelled");
			}
		}catch (Exception exp) {
			PluginLog.error("Error while Cancelling Cash deal against Strategy " + exp.getMessage());
			Util.exitFail();
			
		} finally{
			try {
				if (Table.isTableValid(version)==1){
					version.destroy();
				}
			} catch (OException e) {
				PluginLog.error("Unable to destroy table");
			}
		}

		return "Succeeded";

	}

	
	@Override
	//Returns all the Cancelled Strategy deals from user table to be processed in status "Pending"
	protected Table fetchStrategyDeals() throws OException {
		Table tbldata;
		try {
			tbldata = Table.tableNew("USER_strategy_deals");
			PluginLog.info("Fetching Strategy deals for cash deal generation ");
			String sqlQuery = "SELECT * FROM USER_strategy_deals WHERE tran_status in ("+ TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() + ","+ TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()+ ") AND status = 'Pending'";
			PluginLog.info("Query to be executed: " + sqlQuery);
			int ret = DBaseTable.execISql(tbldata, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "DBUserTable.saveUserTable() failed"));
			}
			PluginLog.info("Number of Strategy deals returned: "+ tbldata.getNumRows());

		} catch (Exception exp) {
			PluginLog.error("Error while fetching startgey Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return tbldata;
	}
}



