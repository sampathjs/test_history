package com.olf.jm.metalstransfer.trigger;

import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.UpdateUserTable;
import com.olf.jm.metalstransfer.utils.Utils;

public class MetalTransferTriggerScript implements IScript {

	protected String tpmToTrigger = null;
	int workflowId = 0;
	String retry_limit = null;

	public MetalTransferTriggerScript() throws OException {
	}

	public void execute(IContainerContext context) throws OException {

		Table dealsToProcess = Util.NULL_TABLE;
		Table failureToProcess = Util.NULL_TABLE;
		String status = null;
		
		int expectedCashDeals = 0;
		int actualCashDeals = 0;
		String isRerun = "No";

		try {
			
			init();
			PluginLog.info("Process Started");
			// dealsToProcess carries all the deals to be processed
			dealsToProcess = fetchStrategyDeals();
			// If cash deals already exist stamp to succeeded in
			// USER_strategy_deals else trigger TPM
			int numRows = dealsToProcess.getNumRows();
			if (numRows == 0){
				PluginLog.info("No deal  to be processed");
			}else{
			PluginLog.info(numRows + " deals are getting processed");
			for (int row = 1; row <= numRows; row++) {
				int DealNum = dealsToProcess.getInt("deal_num", row);
				int tranNum = dealsToProcess.getInt("tran_num", row);
				int userId = dealsToProcess.getInt("personnel_id", row);
				String bUnit = dealsToProcess.getString("short_name", row);
				String userName = dealsToProcess.getString("userName", row);
				String name = dealsToProcess.getString("name", row);
				//int retry_count = dealsToProcess.getInt("retry_count", row);
				
				List<Integer> cashDealList = getCashDeals(DealNum);
				//Check for latest version of deal, if any amendment happened after stamping in user table
				int latestTranStatus = UpdateUserTable.getLatestVersion(DealNum);
				if (cashDealList.isEmpty()&& latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()) {
					PluginLog.info("No Cash Deal was found for Strategy deal " + DealNum);
					status = processTranNoCashTrade(tranNum,userId,bUnit,userName,name);
				} else if (cashDealList.size() > 0 && latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt())
				{	
					processTranWithCashTrade(cashDealList);
					PluginLog.info("Strategy " + DealNum+" is in NEW status and was found for reprocessing. Check validation report for reason"  );
					status = processTranNoCashTrade(tranNum,userId,bUnit,userName,name);
				}
				//Stamp deals to succeeded when stamped in user table after that was deleted.
				else if(cashDealList.isEmpty()&& latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt() )
				{
					PluginLog.info("Deal is already deleted, hence stamping to succeded. No action required");
					status = "Succeeded";
					
				}else if(cashDealList.size() > 0 && latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt() )
				{
					PluginLog.info("Deal is already deleted, cash deals exist hence changing tran_status  to cancelled.");
					dealsToProcess.setInt("tran_status", row,TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt());
					status = processTranWithCashTrade(cashDealList);
				}
				else if (cashDealList.size()>=0 && latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()){
					PluginLog.info("Strategy " + DealNum+" is already validated and was found for reprocessing. Check validation report for reason"  );
					status = processTranNoCashTrade(tranNum,userId,bUnit,userName,name);
					
				}else {
					PluginLog.info(cashDealList + " Cash deals were found against Strategy deal " + DealNum);
					status = processTranWithCashTrade(cashDealList);
				}
				PluginLog.info("Status updating to "+status+" for deal " + DealNum + " in USER_strategy_deals");
				dealsToProcess.delCol("personnel_id");
				dealsToProcess.delCol("short_name");
				dealsToProcess.delCol("userName");
				dealsToProcess.delCol("name");
				PluginLog.info("Personnel Id is removed from temporary table.");
				
				UpdateUserTable.stampStatus(dealsToProcess, tranNum, row, status, expectedCashDeals,actualCashDeals,workflowId, isRerun);
				//mark all strategy in user table with status running for more than 20 mins
				failureToProcess = fetchStrategyDealsfailed();
				int count = failureToProcess.getNumRows();
				if(count > 0){
					for (int rowNum = 1; rowNum <= count; rowNum++){
					int failedDealNum = dealsToProcess.getInt("deal_num", rowNum);
					status = "Pending";
					PluginLog.info(failedDealNum+" was found in 'Running' status from past 20 mins. \n"+ " Updatig satatus of strategy "+failedDealNum+" to "+status+" , Considering TPM ifaile whilrunning.");
					UpdateUserTable.stampStatus(dealsToProcess, failedDealNum, rowNum, status, expectedCashDeals,actualCashDeals,workflowId, isRerun);
				}
				}
			}

		}
		}

		catch (OException oe) {
			PluginLog.error("Failed to trigger TPM " + oe.getMessage());
			Util.exitFail();
			throw oe;
		} finally {
			if (Table.isTableValid(dealsToProcess) == 1){
				dealsToProcess.destroy();
			}
		}
	}

	protected Table fetchStrategyDealsfailed() throws OException {
		Table failureData;
		try{
			failureData = Table.tableNew("USER_strategy_deals");
			String sql = "SELECT us.*\n"+
					  "FROM USER_strategy_deals us  \n" +
				      " WHERE us.status = 'Running'  \n" + 
				      " AND us.tran_status =" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+"\n"+
				      " AND us.last_updated < DATEADD(minute, -20, Current_TimeStamp) \n"+
				      " AND us.deal_num NOT IN (Select deal_number from USER_jm_strategy_exclusion) ";
					
			PluginLog.info("Query to be executed: " + sql);
			int ret = DBaseTable.execISql(failureData, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while executing query for fetchTPMfailure "));
			}
			
		} catch (OException exp) {
			PluginLog.error("Error while fetching startegy Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return failureData;
	}

	// Triggers TPM for tranNum if no cash deals exists in Endur
	protected String processTranNoCashTrade(int tranNum, int userId, String bUnit, String userName, String name) throws OException {

		Table tpmInputTbl = Tpm.createVariableTable();
		Tpm.addIntToVariableTable(tpmInputTbl, "TranNum", tranNum);
		Tpm.addIntToVariableTable(tpmInputTbl, "userId", userId);
		Tpm.addStringToVariableTable(tpmInputTbl, "bUnit", bUnit);
		Tpm.addStringToVariableTable(tpmInputTbl, "userName", userName);
		Tpm.addStringToVariableTable(tpmInputTbl, "name", name);
		workflowId = (int) Tpm.startWorkflow(this.tpmToTrigger, tpmInputTbl);
		PluginLog.info("TPM trigger for deal " + tranNum);
		PluginLog.info("UserId for strategy is  " + userId);
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
		_constRepo = new ConstRepository("Alerts", "TransferValidation");
		this.retry_limit = _constRepo.getStringValue("retry_limit");
		if (this.retry_limit == null || "".equals(this.retry_limit)) {
			throw new OException("Ivalid retry limit defined  in Const Repository");
		}
		PluginLog.info("Limit for retry is " + retry_limit+ " configured in User_const_repository");
		
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
				PluginLog.info("Retrieving Cash Deal for Transaction " + dealNum);
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
			String sqlQuery = "SELECT us.*,ab.personnel_id,p.short_name,CONCAT(pe.first_name,' ',pe.last_name) as userName,pe.name\n"+
							  "FROM USER_strategy_deals us  \n" +
							  "INNER JOIN ab_tran ab ON ab.tran_num = us.tran_num \n"+
							  "INNER JOIN party p ON p.party_id = ab.internal_bunit \n "+
							  "INNER JOIN personnel pe ON pe.id_number = ab.personnel_id \n"+
						      " WHERE us.status = 'Pending'  \n" +
							  " AND us.retry_count <" + Integer.parseInt(retry_limit)+ "\n"+
						      " AND us.tran_status =" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+"\n"+
						      " AND us.deal_num NOT IN (Select deal_number from USER_jm_strategy_exclusion) ";
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

}

