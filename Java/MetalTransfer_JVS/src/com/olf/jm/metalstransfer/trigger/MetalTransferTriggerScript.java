package com.olf.jm.metalstransfer.trigger;

import java.util.ArrayList;
import java.util.List;

import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.UpdateUserTable;
import com.olf.jm.metalstransfer.utils.Utils;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TABLE_SORT_DIR_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;

/*
 * History:
 * 2020-06-27   V1.0    VishwN01	- Fixes for SR 379297
 * 2020-07-10   V1.1    AgrawA01	- Added logic to delete/cancel cash trades for Deleted strategy
 */

public class MetalTransferTriggerScript implements IScript {

	protected String tpmToTrigger = null;
	protected String bufferTime = null;
	int workflowId = 0;
	String retry_limit = null;
	ConstRepository  _constRepo = new ConstRepository("Alerts", "TransferValidation");
	ConstRepository constRepo = new ConstRepository("Strategy", "NewTrade");
	
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
			Logging.info("Process Started");
			// dealsToProcess carries all the deals to be processed
			dealsToProcess = fetchStrategyDeals();
			
			// If cash deals already exist stamp to succeeded in USER_strategy_deals else trigger TPM
			int numRows = dealsToProcess.getNumRows();
			if (numRows == 0) {
				Logging.info("No deal  to be processed");
				
			} else {
				Logging.info(numRows + " deals are getting processed");
				Logging.info("Filtering same account deals.");
				filterSameAccountDeals(dealsToProcess);
			
				for (int row = 1; row <= numRows; row++) {
					int DealNum = dealsToProcess.getInt("deal_num", row);
					int tranNum = dealsToProcess.getInt("tran_num", row);
					int userId = dealsToProcess.getInt("personnel_id", row);
					String bUnit = dealsToProcess.getString("short_name", row);
					String userName = dealsToProcess.getString("userName", row);
					String name = dealsToProcess.getString("name", row);
					//int retry_count = dealsToProcess.getInt("retry_count", row);
					Logging.info("Retrieved values for strategy, Deal: " + DealNum + ", UserId: " + userId
							+ ", bUnit: " + bUnit + ", userName: " + userName);
					
					List<Integer> cashDealList = getCashDeals(DealNum);
					
					//Check for latest version of deal, if any amendment happened after stamping in user table
					int latestTranStatus = UpdateUserTable.getLatestVersion(DealNum);
					if (cashDealList.isEmpty() && latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()) {
						Logging.info("No Cash Deal was found for Strategy deal " + DealNum);
						status = processTranNoCashTrade(tranNum,userId,bUnit,userName,name);
						
					} else if (cashDealList.size() > 0 && latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()) {
						processTranWithCashTrade(cashDealList);
						Logging.info("Strategy " + DealNum+" is in NEW status and was found for reprocessing. Check validation report for reason"  );
						status = processTranNoCashTrade(tranNum,userId,bUnit,userName,name);
						
					} else if (cashDealList.isEmpty() && latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()) {
						//Stamp deals to succeeded when stamped in user table after that was deleted.
						Logging.info("Deal is already deleted, hence stamping to succeeded. No action required");
						status = "Succeeded";
						
					} else if (cashDealList.size() > 0 && latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()) {
						Logging.info("Deal is already deleted, cash deals exist hence changing tran_status  to cancelled.");
						dealsToProcess.setInt("tran_status", row, TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt());
						status = cancelOrDeleteCashTrades(cashDealList);
						
					} else if (latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()) {
						Logging.info("Strategy " + DealNum+" is already validated with "+cashDealList.size()+" associated Cash Deals and was found for reprocessing. Check validation report for reason"  );
						status = "Succeeded";
						
					} else {
						Logging.info(cashDealList + " Cash deals were found against Strategy deal " + DealNum);
						status = processTranWithCashTrade(cashDealList);
					}
					
					dealsToProcess.delCol("personnel_id");
					dealsToProcess.delCol("short_name");
					dealsToProcess.delCol("userName");
					dealsToProcess.delCol("name");
					
					Logging.info("Status updating to "+status+" for strategy " + DealNum + " in USER_strategy_deals");
					UpdateUserTable.stampStatus(dealsToProcess, tranNum, row, status, expectedCashDeals,actualCashDeals,workflowId, isRerun);
					//mark all strategy in user table with status running for more than 20 mins
				}
			}
			
			failureToProcess = fetchStrategyDealsFailed();
			int count = failureToProcess.getNumRows();
			if (count > 0) {
				for (int rowNum = 1; rowNum <= count; rowNum++) {
					int failedDealNum = failureToProcess.getInt("deal_num", rowNum);
					status = "Pending";
					Logging.info(failedDealNum + " was found in 'Running' status from past 20 mins. \n"
									+ " Updating status of strategy "+ failedDealNum + " to " + status);
					UpdateUserTable.stampStatus(failureToProcess, failedDealNum, rowNum, status, expectedCashDeals,
							actualCashDeals, this.workflowId, isRerun);
				}
			}
		} catch (OException oe) {
			Logging.error("Failed to trigger TPM " + oe.getMessage());
			Util.exitFail();
			throw oe;
			
		} finally {
			if (Table.isTableValid(dealsToProcess) == 1){
				dealsToProcess.destroy();
			}
			if (Table.isTableValid(failureToProcess) == 1){
				failureToProcess.destroy();
			}
		}
	}

	protected Table fetchStrategyDealsFailed() throws OException {
		Table failureData = Util.NULL_TABLE;
		
		try {
			this.bufferTime = _constRepo.getStringValue("bufferTime");
			if (this.bufferTime == null || "".equals(this.bufferTime)) {
				throw new OException("Invalid buffer time defined in Const Repository");
			}
			
			Logging.info("bufferTime is " + bufferTime+ " configured in User_const_repository");
			failureData = Table.tableNew("USER_strategy_deals");
			String sql = "SELECT us.*\n"+
					  "FROM USER_strategy_deals us  \n" +
				      " WHERE us.status = 'Running'  \n" + 
				      " AND us.tran_status =" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+"\n"+
				      " AND us.last_updated < DATEADD(minute,-"+bufferTime+", Current_TimeStamp) \n";
					
			Logging.info("Query to be executed: " + sql);
			int ret = DBaseTable.execISql(failureData, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while executing query for fetchStrategyDealsFailed method"));
				throw new OException("Failed while executing query for fetchStrategyDealsFailed method");
			}
			
		} catch (OException exp) {
			Logging.error("Error while fetching failed strategy Deals " + exp.getMessage());
			throw exp;
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
		
		this.workflowId = (int) Tpm.startWorkflow(this.tpmToTrigger, tpmInputTbl);
		Logging.info("TPM triggered for strategy " + tranNum + " with workflowId " + this.workflowId);
		
		return "Running";
	}

	// init method for invoking TPM from Const Repository
	protected void init() throws OException {
		Utils.initialiseLog(Constants.LOG_FILE_NAME);
		
		this.tpmToTrigger = constRepo.getStringValue("tpmTotrigger");
		if (this.tpmToTrigger == null || "".equals(this.tpmToTrigger)) {
			throw new OException("Invalid TPM definition found in Const Repository");
		}
		
		this.retry_limit = _constRepo.getStringValue("retry_limit");
		if (this.retry_limit == null || "".equals(this.retry_limit)) {
			throw new OException("Invalid retry limit defined in Const Repository");
		}
		Logging.info("Limit for retry is " + retry_limit+ " configured in User_const_repository");
	}

	// No processing required
	protected String processTranWithCashTrade(List<Integer> cashDealList) {
		return "Succeeded";
	}
	
	/**
	 * Method to delete/cancel CASH Transfer trades booked for a strategy which was later processed to Deleted status.
	 * 
	 * @param cashDealList
	 * @return
	 * @throws OException
	 */
	protected String cancelOrDeleteCashTrades(List<Integer> cashDealList) throws OException {
		Transaction cashTran;
		int countCash = cashDealList.size();
		int tradesProcessed = 0;
		
		for (int rowCount = 0; rowCount < countCash; rowCount++) {
			int tranNum = cashDealList.get(rowCount);
			Logging.info("Cancellation/Deletion process started for " + tranNum);
			
			cashTran = Util.NULL_TRAN;
			try {
				cashTran = Transaction.retrieve(tranNum);
				int status = cashTran.getFieldInt(TRANF_FIELD.TRANF_TRAN_STATUS.toInt());
				int version = cashTran.getFieldInt(TRANF_FIELD.TRANF_VERSION_NUM.toInt());
				
				if (status == TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()) {
					Logging.info("Deleting cash trade " + tranNum);
					Transaction.delete(tranNum, version);
					Logging.info("Deleted cash trade " + tranNum);
					
				} else if (status == TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()) {
					Logging.info("Cancelling cash trade " + tranNum);
					Transaction.cancel(tranNum, version);
					Logging.info("Cancelled cash trade " + tranNum);
				}
				tradesProcessed++;
				
			} catch (OException exp) {
				Logging.error("Error while cancelling/deleting Cash deal "+ tranNum +" against Strategy, Error: " 
							+ exp.getMessage());
				
			} finally {
				if (Transaction.isNull(cashTran) != 1) {
					cashTran.destroy();
				}
			}
		}
		
		if (tradesProcessed != countCash) {
			return "Pending";
		}
		return "Succeeded";
	}

	// Return True if for strategy Cash exists
	protected List<Integer> getCashDeals(int dealNum) throws OException {
		Table cashDealsTbl = Util.NULL_TABLE;
		List<Integer> cashDealList = new ArrayList<Integer>();

		try {
			if (dealNum > 0) {
				int cashDealCount;
				Logging.info("Retrieving Cash Deals for strategy " + dealNum);
				String sql = "SELECT ab.tran_num AS tran_num "
						+ " FROM ab_tran ab "
						+ " LEFT JOIN ab_tran_info ai ON ab.tran_num = ai.tran_num \n" 
						+ " WHERE ai.value = " + dealNum + " AND ai.type_id = 20044 \n" +
							 " AND tran_status IN (" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() + "," 
							 		+ TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + "," 
							 		+ TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt() + ")";
				
				cashDealsTbl = Table.tableNew();
				int ret = DBaseTable.execISql(cashDealsTbl, sql);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					Logging.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while retrieving Cash Deals for strategy " + dealNum));
					throw new OException("Failed while retrieving Cash Deals for strategy " + dealNum);
				}
				
				cashDealCount = cashDealsTbl.getNumRows();
				Logging.info("Number of Cash deals returned " + cashDealCount +" for strategy " + dealNum);
				for (int row = 1; row <= cashDealCount; row++) {
					cashDealList.add(cashDealsTbl.getInt("tran_num", row));
				}
			}
		} catch (Exception exp) {
			Logging.error("Failed to retrieve cash trades for strategy " + dealNum + ", Error: " + exp.getMessage());
			throw exp;
			
		} finally {
			if (Table.isTableValid(cashDealsTbl) == 1) {
				cashDealsTbl.destroy();
			}
		}
		
		return cashDealList;
	}

	//Fetch strategy deals from user table which are validated and in pending status and were retry 3 times
	protected Table strategyValidatedButPending() throws OException {
		Table strategyStillPending = Util.NULL_TABLE;
		
		try {
			strategyStillPending = Table.tableNew();
			String querySql = "SELECT us.*,ab.personnel_id, p.short_name, CONCAT(pe.first_name,' ',pe.last_name) as userName, pe.name\n"+
					  "FROM USER_strategy_deals us  \n" +
					  "INNER JOIN ab_tran ab ON ab.tran_num = us.tran_num \n"+
					  "INNER JOIN party p ON p.party_id = ab.internal_bunit \n "+
					  "INNER JOIN personnel pe ON pe.id_number = ab.personnel_id \n"+
				      " WHERE us.status = 'Pending'  \n" +
				      " AND us.last_updated <= GetDate()-"+Integer.parseInt(retry_limit)+ "\n"+
					  " AND us.retry_count >=" + Integer.parseInt(retry_limit)+ "\n"+
				      " AND us.tran_status = " +TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+" \n"+
				      " AND us.deal_num NOT IN (Select deal_number from USER_jm_strategy_exclusion) ";
			
			Logging.info("Query to be executed: " + querySql);
			int ret = DBaseTable.execISql(strategyStillPending, querySql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while executing query "+querySql));
				throw new OException("Failed while fetching Validated strategy but Pending status in user table");
			}
			
		} catch (OException exp) {
			Logging.error("Error in strategyValidatedButPending method: " + exp.getMessage());
			throw exp;
		}
		
		return strategyStillPending;
	}
	
	// Fetch strategy deals from Endur for which cash deals are to be generated
	protected Table fetchStrategyDeals() throws OException {
		Table tbldata = Util.NULL_TABLE;
		Table validatedButPending = Util.NULL_TABLE;
		
		try {
			tbldata = Table.tableNew("USER_strategy_deals");
			Logging.info("Fetching Strategy deals for cash deal generation");
			
			String sqlQuery = "SELECT us.*, ab.personnel_id, p.short_name, CONCAT(pe.first_name,' ',pe.last_name) as userName, pe.name\n"+
							  "FROM USER_strategy_deals us  \n" +
							  "INNER JOIN ab_tran ab ON ab.tran_num = us.tran_num \n"+
							  "INNER JOIN party p ON p.party_id = ab.internal_bunit \n "+
							  "INNER JOIN personnel pe ON pe.id_number = ab.personnel_id \n"+
						      " WHERE us.status = 'Pending'  \n" +
							  " AND us.retry_count <" + Integer.parseInt(retry_limit)+ "\n"+
						      " AND us.tran_status in (" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()+") \n"+
						      " AND us.deal_num NOT IN (Select deal_number from USER_jm_strategy_exclusion) ";
			Logging.info("Query to be executed: " + sqlQuery);
			int ret = DBaseTable.execISql(tbldata, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while fetching Strategy deals for cash deal generation"));
				throw new OException("Failed while fetching Strategy deals for cash deal generation");
			}
			
			//Fetch strategy deals in Pending and tran status is validated and is retried 3 times
			validatedButPending = strategyValidatedButPending();
			if (validatedButPending.getNumRows() > 0) {
				validatedButPending.copyRowAddAll(tbldata);
				Logging.info(validatedButPending.getNumRows()+" strategy deals were found in User_strategy_deals which are validated and not processed");
			}
			
			Logging.info("Number of records returned for processing: " + tbldata.getNumRows());

		} catch (OException exp) {
			Logging.error("Error while fetching strategy Deals, Error: " + exp.getMessage());
			throw exp;
			
		} finally {
			if (Table.isTableValid(validatedButPending) == 1) {
				validatedButPending.destroy();
			}
		}
		
		return tbldata;
	}
	
	/**
	 * 
	 * This function takes the deals to be processed and adds any deals at Running to the current list which have the same account as any deal in the current list.
	 * It then creates a list of deals from the current list which all have the same account as the "same-account" list.
	 * These entries are then removed from the main list
	 * If there is an entry in the "same-account" list at Running the none of the entries per account are added to the main list
	 * If there are no entries in the "same-account" list at Running per account then only the lowest tran_num from the "same-account" list is added to be processed
	 * 
	 * @param tblDealsToProcess
	 * @param tblSameAccDealsToProcess
	 */
	private void filterSameAccountDeals(Table tblDealsToProcess) {
		try {
			String strSQL;
			int intQid;
			// Add to tblDealsToProcess any deals that are currently Running which have the same account as any deal in tblDealsToProcess
			intQid = Query.tableQueryInsert(tblDealsToProcess, "tran_num");
			
			strSQL = "SELECT \n"
					+ "us.deal_num "
					+ ",us.tran_num"
					+ ",us.tran_status"
					+ ",us.status"
					+ ",us.last_updated"
					+ ",us.version_number"
					+ ",ab.personnel_id"
					+ ",p.short_name"
					+ ",CONCAT(pe.first_name,' ',pe.last_name) as userName"
					+ ",pe.name \n"
					+ ",acc.account_id \n"
					+ "FROM USER_strategy_deals us  \n" 
				    + "INNER JOIN ab_tran ab ON ab.tran_num = us.tran_num \n"
				    + "INNER JOIN party p ON p.party_id = ab.internal_bunit \n "
				    + "INNER JOIN personnel pe ON pe.id_number = ab.personnel_id \n"
				    + "INNER JOIN ab_tran_info_view ativ on us.tran_num = ativ.tran_num and ativ.type_name = 'From A/C' \n"
				    + "INNER JOIN account acc on ativ.value = acc.account_name \n"
				    + "WHERE us.status = 'Running'  \n" 
				    + "AND us.tran_status =" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+"\n"
					+ "AND account_id in (SELECT  "
					+ "						acc.account_id \n"
					+ "						FROM \n "
					+ "						query_result qr"
					+ "						INNER JOIN ab_tran_info_view ativ on qr.query_result = ativ.tran_num and ativ.type_name = 'From A/C' \n"
					+ "						INNER JOIN account acc on ativ.value = acc.account_name \n"
					+ "						WHERE \n"
					+ "						qr.unique_id = " + intQid + ") \n";
			Table tblRunning = Table.tableNew();
			DBaseTable.execISql(tblRunning, strSQL);
			Logging.info("Found " + tblRunning.getNumRows() + " deals at Running which have same account as current list of deals to be processed.");

			if (tblRunning.getNumRows() > 0) {
				tblRunning.copyRowAddAll(tblDealsToProcess);
				Logging.info("Deals to filter has " + tblDealsToProcess.getNumRows() + " rows after adding Running deals.");
			}
			
			tblRunning.destroy();
			Query.clear(intQid);
			
			if(tblDealsToProcess.getNumRows() > 1){
				// GET A LIST OF DEALS WHICH HAVE SAME ACCOUNT LIST
				intQid = Query.tableQueryInsert(tblDealsToProcess, "tran_num");
				
				// get number of deals per account for the current processing list
				strSQL = "SELECT \n";
				strSQL += "acc.account_id \n";
				strSQL += ",count(*) as deal_count \n";
				strSQL += "FROM \n";
				strSQL += "query_result qr \n";
				strSQL += "INNER JOIN ab_tran_info_view ativ on qr.query_result = ativ.tran_num and ativ.type_name = 'From A/C' \n";
				strSQL += "INNER JOIN account acc on ativ.value = acc.account_name \n" ;
				strSQL += "WHERE \n";
				strSQL += "qr.unique_id = " + intQid + " \n";
				strSQL += "GROUP BY acc.account_id \n";
				
				Table tblDealAccountCount = Table.tableNew();
				DBaseTable.execISql(tblDealAccountCount, strSQL);
				
				// Enhance tblDealsToProcess with the count of deals per account
				String strWhat = "deal_count";
				String strWhere = "account_id EQ $account_id";
				tblDealsToProcess.select(tblDealAccountCount, strWhat, strWhere);
				
				// Create tblSameAccountDeals which is the deals which have the same account
				strWhat = "*";
				strWhere = "deal_num GT 0 AND deal_count GE 2";;
				Table tblSameAccDealsToProcess = Table.tableNew();
				tblSameAccDealsToProcess.select(tblDealsToProcess,strWhat,strWhere);
				Logging.info("Found "  + tblSameAccDealsToProcess.getNumRows() + " deals which belong to same account and these will be filtered.");
				
				// Remove same account deals from the main list
				for (int i = tblDealsToProcess.getNumRows();i>0;i--) {
					if(tblDealsToProcess.getInt("deal_count", i) > 1){
						Logging.info("Removing tran " + tblDealsToProcess.getInt("tran_num",i) + " as in this run it belongs to a batch of deals with the same acccount " +  Ref.getName(SHM_USR_TABLES_ENUM.ACCOUNT_TABLE, tblDealsToProcess.getInt("account_id",i))+ " . A single deal from this batch will added below.");
						tblDealsToProcess.delRow(i);
					}
				}
				
				tblDealsToProcess.delCol("deal_count");
				tblSameAccDealsToProcess.delCol("deal_count");
				
				// PROCESS SAME ACCOUNT LIST
				
				// For each account in the same-account list
				// Add back a single tran from the "same-account" list if none for that account are at Running			
				
				strWhat = "DISTINCT,account_id";
				strWhere = "account_id GT 0";
				Table tblAccounts = Table.tableNew();
				tblAccounts.select(tblSameAccDealsToProcess,strWhat,strWhere);
				
				Logging.info("Found " + tblAccounts.getNumRows() + " accounts which have multiple deals");
				Table tblCurrAcctDealsToProcess = Table.tableNew();
				
				for(int i =1;i<=tblAccounts.getNumRows();i++){
					int intCurrAcct = tblAccounts.getInt("account_id", i);
					tblCurrAcctDealsToProcess.clearRows();
					tblCurrAcctDealsToProcess.select(tblSameAccDealsToProcess, "*", "account_id EQ " + intCurrAcct);

					Logging.info("Filtering multiple deals against account " + Ref.getName(SHM_USR_TABLES_ENUM.ACCOUNT_TABLE, intCurrAcct));
					
					// If any of current same account deals are at running skip any deals for this account
					int intRowNum = tblCurrAcctDealsToProcess.unsortedFindString("status", "Running", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
					if (intRowNum > 0) {
						Logging.info("Found tran  " + tblCurrAcctDealsToProcess.getInt("tran_num",intRowNum)+ "   at Running for this account. Skipping account until next run.");
						continue;
					} else {
						// Else take the lowest tran_num and add to tblDealsToProcess
						tblCurrAcctDealsToProcess.sortCol("tran_num", TABLE_SORT_DIR_ENUM.TABLE_SORT_DIR_ASCENDING);
						tblCurrAcctDealsToProcess.copyRowAdd(1, tblDealsToProcess);

						Logging.info("Found tran " + tblCurrAcctDealsToProcess.getInt("tran_num", 1) + " and adding to list for this run.");
					}
				}
				
				tblCurrAcctDealsToProcess.destroy();
				Query.clear(intQid);
				tblAccounts.destroy();
				tblDealAccountCount.destroy();
				
			} else {
				Logging.info("Only 1 deal to process for this run -nothing further to filter");
			}
		} catch(Exception e) {
			Logging.info("Caught exception e" + e.toString());
		}
	}

}
