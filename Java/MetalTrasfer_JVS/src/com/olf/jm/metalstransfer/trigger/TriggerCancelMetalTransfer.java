package com.olf.jm.metalstransfer.trigger;

import java.util.List;

import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

public class TriggerCancelMetalTransfer extends MetalTransferTriggerScript {
	public TriggerCancelMetalTransfer() throws OException {
	}	
	protected void init() throws OException {
		Utils.initialiseLog(Constants.LOG_FILE);
	}
	//No Cash trades available, stamp status to "Succeeded" in User Table
	protected String processTranNoCashTrade(int trannum) throws OException {
		PluginLog.info("No Cash deal was retrieve for cancellation against "
				+ trannum);
		return "Succeeded";

	}

	//Using API to cancel Cash trades against the Strategy deals
	protected String processTranWithCashTrade(List<Integer> cashDealList) {
		try {
			int countCash = cashDealList.size();
			for (int rowCount = 0; rowCount < countCash; rowCount++) {
			int tranNum = cashDealList.get(rowCount);
			Transaction tran = Transaction.retrieve(tranNum);
			tran.setField(TRANF_FIELD.TRANF_TRAN_STATUS.toInt(), 0, TRANF_FIELD.TRANF_TRAN_STATUS.toString(),5);
			PluginLog.info("TranNum  " + tranNum	+ "  Cancelled as Strategy was Cancelled");

		}
		}catch (Exception exp) {
			PluginLog.error("Error while Cancelling Cash deal against Strategy " + exp.getMessage());
		}

		return "Succeeded";

	}

	
	@Override
	//Returns all the Cancelled Strategy deals from user table to be processed in status "Pending"
	protected Table fetchStrategyDeals() throws OException {
		Table tbldata;
		try {
			tbldata = Table.tableNew("USER_Strategy_Deals");
			PluginLog.info("Fetching Strategy deals for cash deal generation ");
			String sqlQuery = "SELECT * FROM USER_Strategy_Deals where tran_status = "+ TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt()
					+ "AND status = 'Pending'";
			PluginLog.info("Query to be executed: " + sqlQuery);
			int ret = DBaseTable.execISql(tbldata, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret,
						"DBUserTable.saveUserTable() failed"));
			}
			PluginLog.info("Number of Strategy deals returned: "+ tbldata.getNumRows());

		} catch (Exception exp) {
			PluginLog.error("Error while fetching startgey Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return tbldata;
	}
}
