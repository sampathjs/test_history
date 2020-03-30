package com.olf.jm.metalstransfer.trigger;

import java.util.ArrayList;
import java.util.List;

import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2020-03-25	V1.1	AgrawA01	- memory leaks, remove console print & formatting changes
 */

public class MetalTransferTriggerScript implements IScript {

	protected String tpmToTrigger = null;

	public MetalTransferTriggerScript() throws OException {
	}

	public void execute(IContainerContext context) throws OException {
		Table dealsToProcess = Util.NULL_TABLE;
		String status = null;
		
		try {
			init();
			PluginLog.info("Process Started");
			// dealsToProcess carries all the deals to be processed
			dealsToProcess = fetchStrategyDeals();
			
			// If cash deals already exist stamp to succeeded in USER_strategy_deals else trigger TPM
			int numRows = dealsToProcess.getNumRows();
			if (numRows == 0) {
				PluginLog.info("No deal  to be processed");
			} else {
				PluginLog.info(numRows + " deals are getting proccessed");
				
				for (int row = 1; row <= numRows; row++) {
					int DealNum = dealsToProcess.getInt("deal_num", row);
					int tranNum = dealsToProcess.getInt("tran_num", row);
					int userId = dealsToProcess.getInt("personnel_id", row);
					String bUnit = dealsToProcess.getString("short_name", row);
					String userName = dealsToProcess.getString("userName", row);
					String name = dealsToProcess.getString("name", row);
					
					List<Integer> cashDealList = getCashDeals(DealNum);
					//Check for latest version of deal, if any amendment happened after stamping in user table
					int latestTranStatus = getLatestVersion(DealNum);
					if (cashDealList.isEmpty()&& latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()) {
						PluginLog.info("No Cash Deal was found for Startegy deal " + DealNum);
						status = processTranNoCashTrade(tranNum,userId,bUnit,userName,name);
					} 
					//Stamp deals to succeeded when stamped in user table after that was deleted.
					else if(cashDealList.isEmpty()&& latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()) {
						PluginLog.info("Deal is already deleted, hence stamping to succeded. No action required");
						status = "Succeeded";
						
					} else if(cashDealList.size() > 0 && latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()) {
						PluginLog.info("Deal is already deleted, cash deals exist hence changing tran_status  to cancelled.");
						dealsToProcess.setInt("tran_status", row,TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt());
						status = processTranWithCashTrade(cashDealList);
						
					} else if (cashDealList.size()>=0 && latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()) {
						PluginLog.info("Strategy " + DealNum+" is already validated and was found for reprocessing. Check validation report for reason"  );
						status = processTranNoCashTrade(tranNum,userId,bUnit,userName,name);
					} else {
						PluginLog.info(cashDealList + " Cash deals were found against Startegy deal " + DealNum);
						status = processTranWithCashTrade(cashDealList);
					}
					
					PluginLog.info("Status updating to Succeeded for deal " + DealNum + " in USER_strategy_deals");
					dealsToProcess.delCol("personnel_id");
					dealsToProcess.delCol("short_name");
					dealsToProcess.delCol("userName");
					dealsToProcess.delCol("name");
					PluginLog.info("Personnel Id is removed from temporary table.");
				
					stampStatus(dealsToProcess, tranNum, row, status);
				}
			}
		} catch (OException oe) {
			PluginLog.error("Failed to trigger TPM " + oe.getMessage());
			Util.exitFail();
			throw oe;
		} finally {
			if (Table.isTableValid(dealsToProcess) == 1){
				dealsToProcess.destroy();
			}
		}
	}

	protected int getLatestVersion(int dealNum) throws OException {
		Table latestVersionTbl = Util.NULL_TABLE;
		int latestStatus =  0;
		
		try {
			PluginLog.info("Retreving latest status for " + dealNum);
			String Str = "SELECT ab.tran_status from ab_tran ab \n"+
						 "WHERE ab.deal_tracking_num ="+dealNum+ "\n"+
						 "AND ab.current_flag = 1";
			
			latestVersionTbl= Table.tableNew();
			int ret = DBaseTable.execISql(latestVersionTbl, Str);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while retrieving latest verion of "+dealNum));
			}
			
		    latestStatus = latestVersionTbl.getInt("tran_status",1);
			PluginLog.info("Latest status for Strategy "+dealNum+ "is " +latestStatus);
		
		} catch (Exception exp) {
			PluginLog.error("Failed to retrieve latest tran status for " + dealNum + exp.getMessage());
		} finally {
			if (Table.isTableValid(latestVersionTbl) == 1) {
				latestVersionTbl.destroy();
			}
		}
			
		return latestStatus;
	}

	// Triggers TPM for tranNum if no cash deals exists in Endur
	protected String processTranNoCashTrade(int tranNum, int userId, String bUnit, String userName, String name) throws OException {
		Table tpmInputTbl = Tpm.createVariableTable();
		
		Tpm.addIntToVariableTable(tpmInputTbl, "TranNum", tranNum);
		Tpm.addIntToVariableTable(tpmInputTbl, "userId", userId);
		Tpm.addStringToVariableTable(tpmInputTbl, "bUnit", bUnit);
		Tpm.addStringToVariableTable(tpmInputTbl, "userName", userName);
		Tpm.addStringToVariableTable(tpmInputTbl, "name", name);
		Tpm.startWorkflow(this.tpmToTrigger, tpmInputTbl);
		
		PluginLog.info("TPM trigger for deal " + tranNum + ", UserId for strategy is  " + userId);
		PluginLog.info("Status updated to Running for deal " + tranNum + " in USER_strategy_deals");
		return "Running";
	}

	// init method for invoking TPM from Const Repository
	protected void init() throws OException {
		Utils.initialiseLog(Constants.LOG_FILE_NAME);
		ConstRepository _constRepo = new ConstRepository("Strategy", "NewTrade");
		this.tpmToTrigger = _constRepo.getStringValue("tpmTotrigger");
		
		if (this.tpmToTrigger == null || "".equals(this.tpmToTrigger)) {
			throw new OException("Ivalid TPM defination in Const Repository");
		}
	}

	// No processing required
	protected String processTranWithCashTrade(List<Integer> cashDealList) {
		return "Succeeded";
	}

	// Return True if for strategy Cash exists
	protected List<Integer> getCashDeals(int dealNum) throws OException {
		Table cashDealsTbl = Util.NULL_TABLE;
		List<Integer> cashDealList = new ArrayList<Integer>();

		try {
			if (dealNum > 0) {
				int cashDealCount;
				PluginLog.info("Retreving Cash Deal for Transaction " + dealNum);
				String Str = "SELECT ab.tran_num as tran_num from ab_tran ab LEFT JOIN ab_tran_info ai \n" + 
							 " ON ab.tran_num = ai.tran_num \n" + 
							 " WHERE ai.value = " + dealNum+ " \n" +
							 " AND ai.type_id = 20044 "+ " \n" +
							 " AND tran_status in (" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() + "," + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ","+ TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt() + ")";
				
				cashDealsTbl = Table.tableNew();
				int ret = DBaseTable.execISql(cashDealsTbl, Str);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while updating USER_strategy_deals failed"));
				}
				cashDealCount = cashDealsTbl.getNumRows();
				PluginLog.info("Number of Cash deals returned: " + cashDealCount +" where tran_info type_id is Strategy Num");
				for (int row = 1; row <= cashDealCount; row++) {
					cashDealList.add(cashDealsTbl.getInt("tran_num", row));
				}
			}
		} catch (Exception exp) {
			PluginLog.error("Failed to retrieve data for startegy " + dealNum + exp.getMessage());
		} finally {
			if (Table.isTableValid(cashDealsTbl) == 1) {
				cashDealsTbl.destroy();
			}
		}
		return cashDealList;
	}

	// Fetch strategy deals from Endur for which cash deals are to be generated
	protected Table fetchStrategyDeals() throws OException {
		Table tbldata;
		try {
			tbldata = Table.tableNew("USER_strategy_deals");
			PluginLog.info("Fetching Strategy deals for cash deal generation");
			String sqlQuery = "SELECT us.deal_num,us.tran_num,us.tran_status,us.status,us.last_updated,us.version_number,us.retry_count,ab.personnel_id,p.short_name,CONCAT(pe.first_name,' ',pe.last_name) as userName,pe.name\n"+
							  "FROM USER_strategy_deals us  \n" +
							  "INNER JOIN ab_tran ab ON ab.tran_num = us.tran_num \n"+
							  "INNER JOIN party p ON p.party_id = ab.internal_bunit \n "+
							  "INNER JOIN personnel pe ON pe.id_number = ab.personnel_id \n"+
						      " WHERE us.status = 'Pending'  \n" + 
						      " AND us.tran_status =" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt();
			
			PluginLog.info("Query to be executed: " + sqlQuery);
			int ret = DBaseTable.execISql(tbldata, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while updating USER_strategy_deals failed"));
			}
			PluginLog.info("Number of records returned for processing: " + tbldata.getNumRows());

		} catch (Exception exp) {
			PluginLog.error("Error while fetching startegy Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return tbldata;
	}

	// Stamp status in USER_Strategy_Deals
	protected void stampStatus(Table tbldata, int TranNum, int row, String status) throws OException {
		Table tbldataDelta = Util.NULL_TABLE;
		try {
						
			tbldataDelta = Table.tableNew("USER_strategy_deals");
			tbldataDelta = tbldata.cloneTable();
			tbldata.copyRowAdd(row, tbldataDelta);
			
			//int retry_count = tbldataDelta.getInt("retry_count", row);
			ODateTime extractDateTime = ODateTime.getServerCurrentDateTime();
			tbldataDelta.setString("status", 1, status);
			tbldataDelta.setDateTime("last_updated", 1, extractDateTime);
			//tbldataDelta.setInt("retry_count", row, retry_count);
			
			tbldataDelta.clearGroupBy();
			tbldataDelta.group("deal_num,tran_num,tran_status");
			tbldataDelta.groupBy();
			DBUserTable.update(tbldataDelta);
			PluginLog.info("Status updated to "+status+" for tran_num " + TranNum + " in USER_strategy_deals");
			
		} catch (OException oe) {
			PluginLog.error("Failed while updating USER_strategy_deals failed " + oe.getMessage());
			throw oe;
		} finally {
			if (Table.isTableValid(tbldataDelta) == 1) {
				tbldataDelta.destroy();
			}
		}
	}
}

