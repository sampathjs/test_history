package com.olf.jm.metalstransfer.utils;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
<<<<<<< HEAD
import com.olf.jm.logging.Logging;
=======
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;
>>>>>>> refs/remotes/origin/v17_master
public class UpdateUserTable
{

// Stamp status in USER_Strategy_Deals	

public static void stampStatus(Table tbldata, int TranNum, int row, String status, int actualCashDeals ,int expectedCount,int workflowId, String isRerun ) throws OException
{
		Table tbldataDelta = Util.NULL_TABLE;
		try {
			//long wflowId = Tpm.getWorkflowId();					
			int latestTranStatus = getLatestVersion(TranNum);
			tbldataDelta = Table.tableNew("USER_strategy_deals");
			tbldataDelta = tbldata.cloneTable();
			tbldata.copyRowAdd(row, tbldataDelta);
			//int retry_count = tbldataDelta.getInt("retry_count", row);
			ODateTime extractDateTime = ODateTime.getServerCurrentDateTime();
			tbldataDelta.setInt("workflow_Id", 1, workflowId);
			tbldataDelta.setString("status", 1, status);			
			tbldataDelta.setInt("tran_status", 1, latestTranStatus);
			tbldataDelta.setInt("actual_cash_deal_count", 1, actualCashDeals);
			tbldataDelta.setInt("expected_cash_deal_count", 1, expectedCount);
			tbldataDelta.setDateTime("last_updated", 1, extractDateTime);
			//tbldataDelta.setInt("retry_count", row, retry_count);
			int retry_count = tbldataDelta.getInt("retry_count", 1);
			if (status.equalsIgnoreCase("Pending")||status.equalsIgnoreCase("Succeeded") && isRerun.equalsIgnoreCase("No")  ){
				retry_count++;
				PluginLog.info(TranNum + " is re-processed, therefore retry count is updated to "+retry_count+" in user_strategy_deals");
			}
			
			tbldataDelta.setInt("retry_count", 1, retry_count);
			tbldataDelta.clearGroupBy();
			tbldataDelta.group("deal_num,tran_num,process_Type");
			tbldataDelta.groupBy();
			
			DBUserTable.update(tbldataDelta);
<<<<<<< HEAD
			Logging.info("Status updated to "+status+" for tran_num " + TranNum + " in USER_strategy_deals");
=======
			PluginLog.info("For strategy "+TranNum+" processed under TPM workflow ID "+workflowId+ ", status ="+status+" and trans_status = "+latestTranStatus+" and  actualCashDeals = "+actualCashDeals+" and  expectedCount = "+expectedCount+" is updated at "+ extractDateTime+ "\n" );
>>>>>>> refs/remotes/origin/v17_master
		} catch (OException oe) {
			Logging.error("Failed while updating USER_strategy_deals failed " + oe.getMessage());
			throw oe;
		} finally {
			if (Table.isTableValid(tbldataDelta) == 1) {
				tbldataDelta.destroy();
			}
		}
	}

public static int getLatestVersion(int dealNum) throws OException {
	Table latestVersionTbl = Util.NULL_TABLE;
	int latestStatus =  0;
	try{
		latestVersionTbl= Table.tableNew();
	PluginLog.info("Retrieving latest status for " + dealNum);
	String Str = "SELECT ab.tran_status from ab_tran ab \n"+
				 "WHERE ab.deal_tracking_num ="+dealNum+ "\n"+
				 "AND ab.current_flag = 1";
	latestVersionTbl= Table.tableNew();
	int ret = DBaseTable.execISql(latestVersionTbl, Str);
	if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
		PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while retrieving latest verion of "+dealNum));
	}
    latestStatus = latestVersionTbl.getInt("tran_status",1);
	PluginLog.info("Latest status for Strategy "+ dealNum+ " is " +latestStatus);
	
	}catch (Exception exp) {
		PluginLog.error("Failed to retrieve latest tran status for " + dealNum + exp.getMessage());
	} finally {
		if (Table.isTableValid(latestVersionTbl) == 1) {
			latestVersionTbl.destroy();
		}
	}
		
	return latestStatus;
}
}

