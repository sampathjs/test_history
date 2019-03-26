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
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.Utils;

public class MetalTransferTriggerScript implements IScript {

	protected String tpmToTrigger = null;

	public MetalTransferTriggerScript() throws OException {
	}

	public void execute(IContainerContext context) throws OException {

		Table dealsToProcess = Util.NULL_TABLE;
		String status;

		try {
			init();
			PluginLog.info("Process Started");
			// dealsToProcess carries all the deals to be processed
			dealsToProcess = fetchStrategyDeals();
			// If cash deals already exist stamp to succeeded in
			// USER_Strategy_Deals else trigger TPM
			int numRows = dealsToProcess.getNumRows();
			PluginLog.info(numRows + "are getting proccessed");
			for (int row = 1; row <= numRows; row++) {
				int DealNum = dealsToProcess.getInt("deal_num", row);
				int tranNum = dealsToProcess.getInt("tran_num", row);
				List<Integer> cashDealList = getCashDeals(DealNum);
				if (cashDealList.isEmpty()) {
					PluginLog.info("No Cash Deal was found for Startegy deal " + DealNum);
					status = processTranNoCashTrade(tranNum);
				} else {
					PluginLog.info(cashDealList + " Cash deals were found against Startegy deal " + DealNum);
					status = processTranWithCashTrade(cashDealList);
				}
				PluginLog.info("Status updating to Succeeded for deal " + DealNum + " in User_Strategy_Deals");
				stampStatus(dealsToProcess, tranNum, row, status);
			}

		}

		catch (OException oe) {
			PluginLog.error("Failed to trigger TPM " + oe.getMessage());
			throw oe;
		} finally {
			if (Table.isTableValid(dealsToProcess) == 1)
				dealsToProcess.destroy();
		}
	}

	// Triggers TPM for tranNum if no cash deals exists in Endur
	protected String processTranNoCashTrade(int tranNum) throws OException {

		Table tpmInputTbl = Tpm.createVariableTable();
		Tpm.addIntToVariableTable(tpmInputTbl, "TranNum", tranNum);
		Tpm.startWorkflow(this.tpmToTrigger, tpmInputTbl);
		PluginLog.info("TPM trigger for deal " + tranNum);
		PluginLog.info("Status updated to Running for deal " + tranNum + " in User_Strategy_Deals");
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
				String Str = "SELECT ab.tran_num as tran_num from ab_tran ab LEFT JOIN ab_tran_info ai \n" + "ON ab.tran_num = ai.tran_num \n"+ 
						"WHERE ai.value = " + dealNum+ " \n" +
						"AND ai.type_id = 20044 "+ " \n" +
						"AND tran_status in (" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() + "," + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ","+ TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt() + ")";
				cashDealsTbl = Table.tableNew();
				int ret = DBaseTable.execISql(cashDealsTbl, Str);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while updating User_strategy_deals failed"));
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
			tbldata = Table.tableNew("USER_Strategy_Deals");
			PluginLog.info("Fetching Strategy deals for cash deal generation");
			String sqlQuery = "SELECT * from USER_Strategy_Deals  \n" +
						      "WHERE status = 'Pending'  \n" + 
						      "AND tran_status =" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt();
			PluginLog.info("Query to be executed: " + sqlQuery);
			int ret = DBaseTable.execISql(tbldata, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while updating User_strategy_deals failed"));
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
			tbldataDelta = Table.tableNew("USER_Strategy_Deals");
			tbldataDelta = tbldata.cloneTable();
			tbldata.copyRowAdd(row, tbldataDelta);

			ODateTime extractDateTime = ODateTime.getServerCurrentDateTime();
			tbldataDelta.setString("status", 1, status);
			tbldataDelta.setDateTime("last_updated", 1, extractDateTime);
			tbldataDelta.clearGroupBy();
			tbldataDelta.group("deal_num,tran_num, tran_status");
			tbldataDelta.groupBy();
			DBUserTable.update(tbldataDelta);
			PluginLog.info("Status updated to Succeeded for tran_num " + TranNum + " in User_Strategy_Deals");
		} catch (OException oe) {
			PluginLog.error("Failed while updating User_strategy_deals failed " + oe.getMessage());
			throw oe;
		} finally {
			if (Table.isTableValid(tbldataDelta) == 1) {
				tbldataDelta.destroy();
			}
		}
	}

}

