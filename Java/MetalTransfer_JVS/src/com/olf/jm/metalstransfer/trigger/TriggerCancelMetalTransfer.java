package com.olf.jm.metalstransfer.trigger;

import java.util.List;

import com.olf.jm.metalstransfer.utils.Constants;
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
 * 2020-03-25	V1.1	AgrawA01	- memory leaks, remove console print & formatting changes
 */

public class TriggerCancelMetalTransfer extends MetalTransferTriggerScript {
	
	public TriggerCancelMetalTransfer() throws OException {
	}	
	
	protected void init() throws OException {
		Utils.initialiseLog(Constants.LOG_FILE);
	}
	
	//No Cash trades available, stamp status to "Succeeded" in User Table
	protected String processTranNoCashTrade(int trannum) throws OException {
		PluginLog.info("No Cash deal was retrieve for cancellation against " + trannum);
		return "Succeeded";
	}

	//Using API to cancel Cash trades against the Strategy deals
	protected String processTranWithCashTrade(List<Integer> cashDealList) {
		try {
			int countCash = cashDealList.size();
			for (int rowCount = 0; rowCount < countCash; rowCount++) {
				int tranNum = cashDealList.get(rowCount);
				PluginLog.info("Cancellation process started for tran #"+tranNum);

				int ver_num = fetchVersionNum(tranNum);
				int ret1 = Transaction.cancel(tranNum, ver_num);
				if (ret1 != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
					PluginLog.error("Error while cancelling the deal #" +tranNum);
				}
				PluginLog.info("TranNum #" + tranNum + "  Cancelled as Strategy was Cancelled");
			}
		} catch (Exception exp) {
			PluginLog.error("Error while Cancelling Cash deal against Strategy " + exp.getMessage());
			Util.exitFail();
		}
		return "Succeeded";
	}

	private int fetchVersionNum(int tranNum) throws OException {
		Table version = Util.NULL_TABLE;
		try {
			String str = "SELECT MAX(version_number) FROM ab_tran "
					+ " WHERE tran_num = "+tranNum + " AND current_flag = 1";
			
			version = Table.tableNew();
			int ret = DBaseTable.execISql(version, str);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret,"Failed while getting max version number"));
			}
			return version.getInt(1, 1);
			
		} finally {
			if (Table.isTableValid(version) == 1) {
				version.destroy();
			}
		}
	}
	
	@Override
	//Returns all the Cancelled Strategy deals from user table to be processed in status "Pending"
	protected Table fetchStrategyDeals() throws OException {
		Table tbldata;
		try {
			tbldata = Table.tableNew("USER_strategy_deals");
			PluginLog.info("Fetching Strategy deals for cash deal generation ");
			
			String sqlQuery = "SELECT us.deal_num, us.tran_num, us.tran_status, us.status"
					+ ", us.last_updated, us.version_number, us.retry_count"
					+ ", ab.personnel_id, p.short_name, CONCAT(pe.first_name,' ',pe.last_name) as userName"
					+ ", pe.name\n"
					+ " FROM USER_strategy_deals us  \n"
					+ " INNER JOIN ab_tran ab ON ab.tran_num = us.tran_num \n"
					+ " INNER JOIN party p ON p.party_id = ab.internal_bunit \n "
					+ " INNER JOIN personnel pe ON pe.id_number = ab.personnel_id \n"
					+ " WHERE us.status = 'Pending'  \n" 
						+ " AND us.tran_status =" + TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt();
			
			//String sqlQuery = "SELECT * FROM USER_strategy_deals WHERE tran_status = "+ TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() + " AND status = 'Pending'";
			PluginLog.info("Query to be executed: " + sqlQuery);
			int ret = DBaseTable.execISql(tbldata, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Fetch Cancelled StrategyDeals failed"));
			}
			PluginLog.info("Number of Strategy deals returned: "+ tbldata.getNumRows());

		} catch (Exception exp) {
			PluginLog.error("Error while fetching startgey Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return tbldata;
	}
}



