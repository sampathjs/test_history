package com.olf.jm.metalstransfer.trigger;

import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.UpdateUserTable;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.olf.openjvs.Util;
/*
 * History:
 * 2020-06-27   V1.0    VishwN01	- Fixes for SR 379297
 */
public class StampDealsInUserTables implements IScript {

	private String symtLimitDate;
	private String tpmToTrigger;

	public void execute(IContainerContext context) throws OException {
		Table DealstoProcess = Util.NULL_TABLE;
		Table cancelledDeals = Util.NULL_TABLE;
		Table excludeDeals = Util.NULL_TABLE;
		
		try {
			init();
			Utils.initialiseLog(Constants.Stamp_LOG_FILE);
			ODateTime extractDateTime;
			extractDateTime = ODateTime.getServerCurrentDateTime();
			int currentDate = OCalendar.getServerDate();
			int jdConvertDate = OCalendar.parseStringWithHolId(symtLimitDate,0,currentDate);
			String limitDate = OCalendar.formatJd(jdConvertDate);
			//Fetching deals which are available in user_jm_strategy_exclusion and update the flag as 'Yes'
			excludeDeals = fetchExcludeDeals();
			int count = excludeDeals.getNumRows();
			if(count > 0){
				excludeDeals.setColValCellString("process_Type","Exclude");
				int retval = DBUserTable.insert(excludeDeals);
				if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
		            PluginLog.info("Failed while inserting data \n " + DBUserTable.dbRetrieveErrorInfo(retval, "DBUserTable.insert() failed"));
				}				
			}PluginLog.info(excludeDeals.getNumRows()+" strategy to be excluded , will be inserted in USER_strategy_deals");			
			PluginLog.info("Fetching Strategy deal in 'New' tran_status created on "	+ extractDateTime);	
			DealstoProcess = fetchNewdeals(limitDate);
			int countForNewDeals = DealstoProcess.getNumRows();
			if(countForNewDeals > 0){
				insertDeals(DealstoProcess, extractDateTime);
			}	
			PluginLog.info(DealstoProcess.getNumRows()+" strategy with 'New' tran_status will be stamped in USER_strategy_deals");
			PluginLog.info("Fetching Strategy deal in 'Cancelled' tran_status created on "	+ extractDateTime);	
			
			cancelledDeals = fetchCancelleddeals(limitDate);
			int countForCancelledDeals = cancelledDeals.getNumRows();
			if(countForCancelledDeals > 0){
				insertDeals(cancelledDeals, extractDateTime);
			}		
			PluginLog.info(cancelledDeals.getNumRows()+" strategy with 'Cancelled' tran_status will be stamped in USER_strategy_deals");
			
			PluginLog.info("User table updated with strategy deals");				
		}
			catch (OException oe) {
			PluginLog.error("DBUserTable.saveUserTable() failed"+ oe.getMessage());
			Util.exitFail();
			throw oe;
			
		} finally {
			if (Table.isTableValid(DealstoProcess) == 1) {
				DealstoProcess.destroy();
			}
				if (Table.isTableValid(cancelledDeals) == 1) {
					cancelledDeals.destroy();
			}
				if (Table.isTableValid(excludeDeals) == 1) {
					excludeDeals.destroy();
			}
			}
		
		
	}
 

	private Table fetchExcludeDeals() throws OException {
		Table excludeDeals = Util.NULL_TABLE;
		
		try{
			String sqlQuery = "SELECT 'Exclude' AS process_type, usd.deal_num,usd.tran_num,usd.tran_status,'Excluded' AS status,usd.last_updated,usd.version_number,usd.retry_count,usd.actual_cash_deal_count ,usd.expected_cash_deal_count,usd.workflow_Id \n" 
							  +"FROM USER_jm_strategy_exclusion ue \n"
							  +"LEFT JOIN USER_strategy_deals usd \n"
							  +"ON usd.deal_num = ue.deal_number   \n"
							  +"WHERE NOT exists (SELECT 1 from USER_strategy_deals usd WHERE usd.process_Type = 'Exclude') \n"
							  +"AND usd.tran_status in ("+TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+")"; 				      
					
			excludeDeals = Table.tableNew("USER_strategy_deals");
			PluginLog.info("Fetching excluded Strategy deals for stamping in User table USER_strategy_deals");
			// ALL strategy deals which are not stamped in User table with trans_status NEW and Cancelled
			
			int ret = DBaseTable.execISql(excludeDeals, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.warn(DBUserTable.dbRetrieveErrorInfo(ret, "Failed to save in  User table USER_strategy_deals "));
			}
			
		}catch (OException oe) {
			PluginLog.error("DBUserTable  USER_strategy_deals failed" + oe.getMessage());
			throw oe;
		}
		return excludeDeals;
	}

	protected Table fetchCancelleddeals(String limitDate) throws OException{
		Table cancelDeals = Util.NULL_TABLE;
		try{
			String sqlQuery = "SELECT * FROM (SELECT ab.deal_tracking_num as deal_num,ab.tran_num,ab.tran_status,ab.version_number,process_Type ='Cancellation' FROM ab_tran ab\n" +
							  " WHERE ab.tran_type = "+ TRAN_TYPE_ENUM.TRAN_TYPE_TRADING_STRATEGY.toInt() + "\n" + 
							  "   AND ab.ins_type = " + INS_TYPE_ENUM.strategy.toInt() + "\n" +
							  "   AND ab.toolset = "  + TOOLSET_ENUM.COMPOSER_TOOLSET.toInt() + "\n" +
							  "   AND ab.tran_status in ("+ TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt()+ ")\n" +
							  "   AND ab.Current_flag = 1 \n" +
							  "	  AND ab.last_update >= '" +limitDate+	"')tbl1 \n" +
							  " EXCEPT SELECT * FROM (SELECT deal_num,tran_num,tran_status,version_number, process_Type ='Cancellation' FROM USER_strategy_deals)tbl2";
					
			cancelDeals = Table.tableNew("USER_strategy_deals");
			
			// ALL strategy deals which are not stamped in User table with trans_status NEW and Cancelled
			
			int ret = DBaseTable.execISql(cancelDeals, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.warn(DBUserTable.dbRetrieveErrorInfo(ret, "Failed to save in  User table USER_strategy_deals "));
			}
		}catch (OException oe) {
			PluginLog.error("DBUserTable  USER_strategy_deals failed" + oe.getMessage());
			throw oe;
		}
		return cancelDeals;
	}
//Add columns to user table
protected Table insertDeals(Table DealstoProcess,ODateTime extractDateTime)throws OException{
	try {
		
		DealstoProcess.addCol("status", COL_TYPE_ENUM.COL_STRING);
		DealstoProcess.addCol("last_updated", COL_TYPE_ENUM.COL_DATE_TIME);
		DealstoProcess.setColValString("status", "Pending");
		DealstoProcess.setColValDateTime("last_updated", extractDateTime);
		DealstoProcess.addCol("retry_count", COL_TYPE_ENUM.COL_INT);
		DealstoProcess.setColValInt("retry_count",0);		
		DealstoProcess.addCol("expected_cash_deal_count",COL_TYPE_ENUM.COL_INT);
		DealstoProcess.addCol("actual_cash_deal_count",COL_TYPE_ENUM.COL_INT);
		DealstoProcess.addCol("workflow_Id",COL_TYPE_ENUM.COL_INT);
		DealstoProcess.setColValInt("expected_cash_deal_count",0);
		DealstoProcess.setColValInt("actual_cash_deal_count",0);
		DealstoProcess.setColValInt("workflow_Id",0);
		int retval = DBUserTable.insert(DealstoProcess);
		if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
            PluginLog.info("Failed while inserting data \n " + DBUserTable.dbRetrieveErrorInfo(retval, "DBUserTable.insert() failed"));
		}

	} catch (OException oe) {
		PluginLog.error("Unable to add column to table " + oe.getMessage());
		throw oe;
	}
		return DealstoProcess;
}
//init method for invoking TPM from Const Repository
	protected void init() throws OException {
		Utils.initialiseLog(Constants.LOG_FILE_NAME);
		ConstRepository _constRepo = new ConstRepository("Strategy", "NewTrade");
		this.tpmToTrigger = _constRepo.getStringValue("tpmTotrigger");
		if (this.tpmToTrigger == null || "".equals(this.tpmToTrigger)) {
			throw new OException("Ivalid TPM defination in Const Repository");
		}
		this.symtLimitDate = _constRepo.getStringValue("symtLimitDate");
		if (this.symtLimitDate == null || "".equals(this.symtLimitDate)) {
			throw new OException("Ivalid TPM defination in Const Repository");
		}
		}
	
// Fetch Deals to be stamped
	protected Table fetchNewdeals(String limitDate) throws OException{
		Table newDeals = Util.NULL_TABLE;
		
		try{
			String sqlQuery = "SELECT process_Type ='New', ab.deal_tracking_num as deal_num,ab.tran_num,ab.tran_status,ab.version_number FROM ab_tran ab\n" +
					  " WHERE ab.tran_type = "+ TRAN_TYPE_ENUM.TRAN_TYPE_TRADING_STRATEGY.toInt() + "\n" + 
					  "   AND ab.ins_type = " + INS_TYPE_ENUM.strategy.toInt() + "\n" +
					  "   AND ab.toolset = "  + TOOLSET_ENUM.COMPOSER_TOOLSET.toInt() + "\n" +
					  "   AND ab.tran_status in ("+ TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+ ")\n" +
					  "   AND ab.Current_flag = 1 \n" +
					  "	  AND ab.trade_date >= '"+limitDate+ "'\n"+
					  " AND ab.tran_num not in (select tran_num from USER_strategy_deals)";
					
			newDeals = Table.tableNew("USER_strategy_deals");
			PluginLog.info("Fetching Strategy deals for stamping in User table USER_strategy_deals");
			// ALL strategy deals which are not stamped in User table with trans_status NEW and Cancelled
			
			int ret = DBaseTable.execISql(newDeals, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.warn(DBUserTable.dbRetrieveErrorInfo(ret, "Failed to save in  User table USER_strategy_deals "));
			}
		}catch (OException oe) {
			PluginLog.error("DBUserTable  USER_strategy_deals failed" + oe.getMessage());
			throw oe;
		}
		return newDeals;
	}
}

