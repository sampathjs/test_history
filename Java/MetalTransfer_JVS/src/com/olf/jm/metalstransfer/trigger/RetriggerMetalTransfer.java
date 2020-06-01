package com.olf.jm.metalstransfer.trigger;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

public class RetriggerMetalTransfer extends MetalTransferTriggerScript {

	public RetriggerMetalTransfer() throws OException {
		super();
		
	}	
	
	protected Table fetchStrategyDeals() throws OException {
		Table failureData;
		try{
			failureData = Table.tableNew("USER_strategy_deals");
			String sql = "SELECT us.deal_num,us.tran_num,us.tran_status,us.status,us.last_updated,us.version_number,ab.personnel_id,p.short_name,CONCAT(pe.first_name,' ',pe.last_name) as userName,pe.name\n"+
					  "FROM USER_strategy_deals us  \n" +
					  "INNER JOIN ab_tran ab ON ab.tran_num = us.tran_num \n"+
					  "INNER JOIN party p ON p.party_id = ab.internal_bunit \n "+
					  "INNER JOIN personnel pe ON pe.id_number = ab.personnel_id \n"+
				      " WHERE us.status = 'Running'  \n" + 
				      " AND us.tran_status =" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+"\n"+
					 " AND us.last_updated < DATEADD(minute, -20, Current_TimeStamp) \n"+
				      "AND ab.last_update < DATEADD(minute, -20, Current_TimeStamp)";
					
			Logging.info("Query to be executed: " + sql);
			int ret = DBaseTable.execISql(failureData, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while executing query for fetchTPMfailure "));
			}
			
		} catch (OException exp) {
			Logging.error("Error while fetching startegy Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return failureData;
	}

}
