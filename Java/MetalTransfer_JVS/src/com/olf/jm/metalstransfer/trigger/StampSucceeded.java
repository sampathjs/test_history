package com.olf.jm.metalstransfer.trigger;

import com.olf.jm.logging.Logging;

/*
 * This script takes input from TPM as tranNum and updates the status succeeded after Cash deals are booked.
 */

import com.olf.jm.metalstransfer.utils.UpdateUserTable;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;

/*
 * History:
 *
 * 2020-07-10   V1.1    AgrawA01	- Added logic for empty Status variable
 */

public class StampSucceeded implements IScript {

	public StampSucceeded() throws OException {
	}

	@Override
	public void execute(IContainerContext context) throws OException {
		Table dealstoStamp = Util.NULL_TABLE;
		try {
			long wflowId = Tpm.getWorkflowId();
			int workflowId = (int) wflowId;
			
			init();
			
			String TrantoStamp = getVariable(wflowId, "TranNum");
			int expectedCashDeal = Integer.parseInt(getVariable(wflowId,"ExpectedUpfrontCashDealCount"));
			int expectedTaxDeal = Integer.parseInt(getVariable(wflowId,"ExpectedTaxDealCount"));
			int expectedCount = expectedCashDeal + expectedTaxDeal;
			String TPMstatus = getVariable(wflowId,"Status");
			String isRerun = getVariable(wflowId, "IsRerun");
			int actualCashDeals = Integer.parseInt(getVariable(wflowId, "actualCashDeals"));
			
			int tranToStamp = Integer.parseInt(TrantoStamp);
			Logging.info("Started Stamping process on Strategy "+TrantoStamp);
			
			Logging.info("Retrieved values- Status: " + TPMstatus +", ExpectedUpfrontCashDealCount:" +
					expectedCashDeal + ", ExpectedTaxDealCount: " + expectedTaxDeal + ", actualCashDeals: " + 
					actualCashDeals + " for tran_num " + TrantoStamp);
			
			if (TPMstatus == null || "".equals(TPMstatus)) {
				int latestTranStatus = UpdateUserTable.getLatestVersion(tranToStamp);
				if (latestTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()
						&& (actualCashDeals == expectedCount)) {
					TPMstatus = "Succeeded";
				} else {
					TPMstatus = "Pending";
				}
				Logging.info("Status set to " + TPMstatus + " for strategy " + TrantoStamp);
			}
			
			dealstoStamp = Table.tableNew("USER_strategy_deals");
			String str = "SELECT * FROM USER_strategy_deals where deal_num = "+ tranToStamp;
			int ret = DBaseTable.execISql(dealstoStamp, str);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret, "Unable to execute query on USER_strategy_deals " +str));
				throw new OException("Unable to execute query on USER_strategy_deals " +str);
			}
			
			//String Status = "Succeeded";;
			//Logging.info("Inserting Status as Succeeded in User table for "+TrantoStamp ); 
			String Status = TPMstatus;
			Logging.info("Inserting Status as " + Status + " in User table for "+TrantoStamp ); 

			UpdateUserTable.stampStatus(dealstoStamp, tranToStamp, 1, Status,actualCashDeals,expectedCount, workflowId,isRerun);
			Logging.info("Stamped status to " + Status + " in User_strategy_deals for "+TrantoStamp);
			
		} catch (OException oe) {
			Logging.error("Unbale to access tale USER_strategy_deals "+ oe.getMessage());
			throw oe;
			
		} finally {
			if (Table.isTableValid(dealstoStamp) == 1){
				dealstoStamp.destroy();
			}
			Logging.close();
		}
	}

	private String getVariable(final long wflowId, final String toLookFor) throws OException {
		com.olf.openjvs.Table varsAsTable = Util.NULL_TABLE;
		try {
			varsAsTable = Tpm.getVariables(wflowId);
			Logging.info("Fetching Variables for TPM "+wflowId+" for "+toLookFor );
			if (Table.isTableValid(varsAsTable)==1 || varsAsTable.getNumRows() > 0 ){
				com.olf.openjvs.Table varSub = varsAsTable.getTable("variable", 1);
				for (int row = varSub.getNumRows(); row >= 1; row--) {
					String name = varSub.getString("name", row).trim();
					String value = varSub.getString("value", row).trim();
					if (toLookFor.equals(name)) {
						return value;
					}
				}
			}
		} finally {
			if (Table.isTableValid(varsAsTable) == 1){
				// Possible engine crash destroying table - commenting out Jira 1336
				// varsAsTable = TableUtilities.destroy(varsAsTable);
			}
		}
		return "";
	}
	protected void init() throws OException {
		Utils.initialiseLog(this.getClass().getSimpleName().toString() + ".log");
	}

}
