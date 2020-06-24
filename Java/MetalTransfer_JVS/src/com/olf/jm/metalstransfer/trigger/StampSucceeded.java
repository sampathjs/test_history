package com.olf.jm.metalstransfer.trigger;
import com.olf.jm.metalstransfer.utils.Constants;
//Plugin takes input from TPM as tranNum and updates the status succeeded after Cash deals are booked.
import java.util.List;

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
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

public class StampSucceeded  extends TriggerCancelMetalTransfer  {

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
			int tranToStamp = Integer.parseInt(TrantoStamp);
			Logging.info("Started Stamping process on Strategy tran_num  "+TrantoStamp);
			dealstoStamp = Table.tableNew("USER_strategy_deals");
			String str = "SELECT * FROM USER_strategy_deals where deal_num = "+ tranToStamp;
			int ret = DBaseTable.execISql(dealstoStamp, str);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret, "Unable to execute query on USER_strategy_deals " +str));
			}
<<<<<<< HEAD
			//String Status = "Succeeded";
			//Logging.info("Inserting Status as Succeeded in User table for "+TrantoStamp ); 
=======
			
			String isRerun = getVariable(wflowId, "IsRerun");
			
			int actualCashDeals = Integer.parseInt(getVariable(wflowId, "actualCashDeals"));
			//String Status = "Succeeded";;
			//PluginLog.info("Inserting Status as Succeeded in User table for "+TrantoStamp ); 
>>>>>>> refs/remotes/origin/v17_master
			String Status = TPMstatus;
<<<<<<< HEAD
			int retry_count = dealstoStamp.getInt("retry_count", 1);
			Logging.info("Inserting Status as " + TPMstatus + " in User table for "+TrantoStamp ); 
=======
			PluginLog.info("Inserting Status as " + TPMstatus + " in User table for "+TrantoStamp ); 
>>>>>>> refs/remotes/origin/v17_master

<<<<<<< HEAD
			UpdateUserTable.stampStatus(dealstoStamp, tranToStamp, 1, Status,retry_count);
			Logging.info("Stamped status to Succeeded in User_strategy_deals for "+TrantoStamp);
=======
			UpdateUserTable.stampStatus(dealstoStamp, tranToStamp, 1, Status,actualCashDeals,expectedCount, workflowId,isRerun);
			PluginLog.info("Stamped status to Succeeded in User_strategy_deals for "+TrantoStamp);
>>>>>>> refs/remotes/origin/v17_master
		} catch (OException oe) {
<<<<<<< HEAD
			Logging.error("Unbale to access tale USER_strategy_deals "+ oe.getMessage());
			Util.exitFail();
=======
			PluginLog.error("Unbale to access tale USER_strategy_deals "+ oe.getMessage());
			throw oe;
>>>>>>> refs/remotes/origin/v17_master
		} finally {
			Logging.close();
			if (Table.isTableValid(dealstoStamp) == 1){
				dealstoStamp.destroy();
			}
		}
	}

<<<<<<< HEAD
	private void init() throws OException {
		try{
			Logging.init(this.getClass(), "MetalTransfer","");
			}catch(Error ex){
	    		throw new RuntimeException("Failed to initialise log file:"+ ex.getMessage());
	    	}
		
		
	}

=======
>>>>>>> refs/remotes/origin/v17_master
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
				varsAsTable = TableUtilities.destroy(varsAsTable);
			}
		}
		return "";
	}
	protected void init() throws OException {
		Utils.initialiseLog(this.getClass().getSimpleName().toString());
	}

}
