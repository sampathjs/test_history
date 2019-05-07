package com.olf.jm.metalstransfer.report;

import com.olf.jm.metalstransfer.utils.Constants;
import com.olf.jm.metalstransfer.utils.Utils;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

public class StrategyIntradayReportFeeder implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {
		Table reportData = Util.NULL_TABLE;
		Table strategyDeals = Util.NULL_TABLE;
		Table nonTaxDeals = Util.NULL_TABLE;
		try{
			Utils.initialiseLog(Constants.Stamp_LOG_FILE);
			strategyDeals = Table.tableNew();
			PluginLog.info("Fetching strategy booked for two days");
			strategyDeals =	fetchStrategyDeals();
			PluginLog.info("Inserting returned strategy to query_reslt");
			int  qid = Query.tableQueryInsert(strategyDeals, "deal_num");
			PluginLog.info("Fetching data for non taxable strategy");
			nonTaxDeals = fetchNonTax();
			PluginLog.info("Fetching data for  taxable strategy");
			reportData = fetchTaxDetails(strategyDeals,qid);
			PluginLog.info("Updating user table for non taxable entries");
			stampDeals(nonTaxDeals);
            stampDeals(reportData);
            PluginLog.info("Updating user table for  taxable entries");
            
            Query.clear(qid);
            PluginLog.info("Clean of query result data completed");
		}catch (OException e) {
						e.printStackTrace();
		}finally{
			if (Table.isTableValid(strategyDeals)==1)
				strategyDeals.destroy();
			if (Table.isTableValid(reportData)==1)
				reportData.destroy();
			if (Table.isTableValid(nonTaxDeals)==1)
				nonTaxDeals.destroy();
			if (Table.isTableValid(nonTaxDeals)==1)
				nonTaxDeals.destroy();	
		}
		return;
	}

	
		
		
	private Table fetchNonTax() throws OException {
		Table reportTable = Util.NULL_TABLE;
		try{
			reportTable = Table.tableNew("USER_Strategy_reportdata");
		String str = "SELECT distinct deal_num, cash_expected = 2 FROM USER_strategy_deals \n"+
				     "WHERE CAST(last_updated as DATE) > CAST(DATEADD(DAY,-2,GETDATE()) AS date) \n"+ 
				     "AND tran_status = 2 \n"+
				     "AND deal_num  not IN (SELECT distinct deal_num FROM USER_Strategy_reportdata)";
		PluginLog.info("Query to be executed: " + str);
		int ret = DBaseTable.execISql(reportTable, str);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while updating USER_strategy_deals failed"));
		}
		PluginLog.info("Number of records returned for processing: " + reportTable.getNumRows());
	} catch (Exception exp) {
		PluginLog.error("Error while fetching startegy Deals " + exp.getMessage());
		throw new OException(exp);
	}
		return reportTable;
	}




	private void stampDeals(Table reportData) {
		
		try {
			//reportData.group("deal_num");
			
			DBUserTable.insert(reportData);
		} catch (OException e) {
			e.printStackTrace();
		}
		
		
	}




	private Table fetchStrategyDeals() throws OException {
		Table tbldata = Util.NULL_TABLE;
		try {
			tbldata = Table.tableNew("USER_strategy_deals");
			PluginLog.info("Fetching Strategy deals for processing");
			String sqlQuery = "SELECT * FROM USER_strategy_deals  \n" + 
						 	  " WHERE tran_status =" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+
						 	  " AND status = 'Succeeded' \n "+
						      " AND CAST(last_updated as DATE) > cast(DATEADD(DAY,-2,GETDATE()) AS date)";
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




	private Table  fetchTaxDetails(Table strategyDeals,int qid ) throws OException {
		Table taxDetails = Util.NULL_TABLE;
		try{ 
			taxDetails = Table.tableNew("USER_strategy_reportdata");
			String sql = "Select E.strategy as deal_num,(E.taxCount+2) as cash_expected from USER_Strategy_Deals usd RIGHT JOIN ("
					+"SELECT deal_num, strategy,count(*) as taxCount FROM ( \n"
					+"SELECT distinct A.deal_num,A.strategy,A.tax_tran_type,B.subtype_name,b.charge_rate,b.add_subtract_id from (SELECT distinct(ab.deal_tracking_num) as deal_num,ai.value as strategy,tts.subtype_name, ab.tran_status,abt.tax_tran_type \n"
					+" FROM ab_tran ab LEFT JOIN ab_tran_info ai \n"
					+" ON ab.tran_num = ai.tran_num \n"
					+" INNER JOIN query_result q ON ai.value = q.query_result and  unique_id = \n"+ qid
					+" INNER JOIN ab_tran ab2 ON ab2.tran_group = ab.tran_group\n"
					+" LEFT JOIN ab_tran_tax abt ON abt.tran_num = ab2.tran_num\n"
					+" LEFT JOIN tax_tran_subtype tts ON tts.tax_tran_subtype_id = abt.tax_tran_subtype\n"
					+" AND ab2.current_flag = 1\n"
					//+" WHERE ai.value = "+ strategyDeals + " \n"
					+" WHERE ab.buy_sell = 1 AND ab.toolset =10 AND ab.ins_sub_type = 10001 AND ai.type_id = 20044  \n"
					+" AND ab.tran_status in (3))A\n"
					+" LEFT JOIN\n"
					+" ( SELECT tax.party_id, tax.charge_rate, add_subtract_id,ts.subtype_name\n"
					+" FROM tax_rate tax\n"
					+" JOIN tax_tran_type_restrict ttt ON (ttt.tax_rate_id = tax.tax_rate_id)\n"
					+" JOIN tax_tran_subtype_restrict tst ON (tst.tax_rate_id = tax.tax_rate_id)\n"
					+" JOIN tax_tran_subtype ts on (ts.tax_tran_subtype_id = tst.tax_tran_subtype_id)\n"
					+" JOIN tax_tran_type tty on (tty.tax_tran_type_id = ttt.tax_tran_type_id)) B\n"
					+" ON A.subtype_name= B.subtype_name\n"
					+" WHERE B.charge_rate >0)C \n"
					+"GROUP BY deal_num,strategy)E \n"
					+"ON E.strategy = usd.Deal_num \n"
					+"and usd.tran_status = 2 ";

			PluginLog.info("Query to be executed: " + sql);
			int ret = DBaseTable.execISql(taxDetails, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while updating USER_strategy_deals failed"));
			}
			
		}finally{
			
		}
		
		return taxDetails;
		
	}
}

