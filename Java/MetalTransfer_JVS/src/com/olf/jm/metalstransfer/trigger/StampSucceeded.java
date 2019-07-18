package com.olf.jm.metalstransfer.trigger;
//Plugin takes input from TPM as tranNum and updates the status succeeded after Cash deals are booked.
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

public class StampSucceeded extends TriggerCancelMetalTransfer {

	public StampSucceeded() throws OException {

	}

	@Override
	public void execute(IContainerContext context) throws OException {
		Table dealstoStamp = Util.NULL_TABLE;
		try {
			long wflowId = Tpm.getWorkflowId();
			init();
			String TrantoStamp = getVariable(wflowId, "TranNum");
			int tranToStamp = Integer.parseInt(TrantoStamp);
			PluginLog.info("Started Stamping process on Strategy tran_num  "+TrantoStamp);
			dealstoStamp = Table.tableNew("USER_strategy_deals");
			String str = "SELECT * FROM USER_strategy_deals where deal_num = "+ tranToStamp;
			int ret = DBaseTable.execISql(dealstoStamp, str);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Unable to execute query on USER_strategy_deals " +str));
			}
			String Status = "Succeeded";
			PluginLog.info("Inserting Status as Succeeded in User table for "+TrantoStamp ); 
			stampStatus(dealstoStamp, tranToStamp, 1, Status);
			PluginLog.info("Stamped status to Succeeded in User_strategy_deals for "+TrantoStamp);
		} catch (OException oe) {
			PluginLog.error("Unbale to access tale USER_strategy_deals "+ oe.getMessage());
			throw oe;
		} finally {
			if (Table.isTableValid(dealstoStamp) == 1){
				dealstoStamp.destroy();
			}
		}
	}

	private String getVariable(final long wflowId, final String toLookFor) throws OException {
		com.olf.openjvs.Table varsAsTable = Util.NULL_TABLE;
		try {
			varsAsTable = Tpm.getVariables(wflowId);
			PluginLog.info("Fetching Variables for TPM "+wflowId+" for "+toLookFor );
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

}
