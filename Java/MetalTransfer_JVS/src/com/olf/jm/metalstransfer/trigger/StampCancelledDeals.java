package com.olf.jm.metalstransfer.trigger;

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

public class StampCancelledDeals extends TriggerCancelMetalTransfer {

	public StampCancelledDeals() throws OException {
		
	}

	@Override
	public void execute(IContainerContext context) throws OException {
		Table dealstoStamp = Util.NULL_TABLE;
		try{
			long wflowId = Tpm.getWorkflowId();
               
	        String TrantoStamp = getVariable(wflowId,"DealNum");
	        int DealToStamp = Integer.parseInt(TrantoStamp);
			dealstoStamp = Table.tableNew("USER_strategy_deal_stamp");
			String str = "SELECT * FROM USER_strategy_deal_stamp WHERE deal_num = "+DealToStamp  ;
			int ret = DBaseTable.execISql(dealstoStamp, str);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "DBUserTable.saveUserTable() failed"));
			}
			String Status = "Succeeded";
			stampStatus(dealstoStamp,DealToStamp,1, Status);
		} catch (OException oe) {
			PluginLog.error("DBUserTable.saveUserTable() failed"  + oe.getMessage());
			throw oe;
		} finally {
			if (Table.isTableValid(dealstoStamp) == 1){
				dealstoStamp.destroy();
			}
		}
	}
	private String getVariable(final long wflowId, final String toLookFor) throws OException {
		com.olf.openjvs.Table varsAsTable=null;
		try {
			varsAsTable = Tpm.getVariables(wflowId);
			com.olf.openjvs.Table varSub = varsAsTable.getTable("variable", 1);			
			for (int row=varSub.getNumRows(); row >= 1; row--) {
				String name  = varSub.getString("name", row).trim();				
				String value  = varSub.getString("value", row).trim();
				if (toLookFor.equals(name)) {
					return value;
				}
			}
		} finally {
			varsAsTable = TableUtilities.destroy(varsAsTable);
		}
		return "";
	}

}
