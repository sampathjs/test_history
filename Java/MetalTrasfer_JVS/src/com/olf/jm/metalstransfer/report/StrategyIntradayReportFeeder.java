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
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

public class StrategyIntradayReportFeeder implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {
		Table taxDeals = Util.NULL_TABLE;
		Table strategyDeals = Util.NULL_TABLE;
		Table nonTaxDeals = Util.NULL_TABLE;
		int qid = 0;
		try{
			Utils.initialiseLog(Constants.Stamp_LOG_FILE);
			//strategyDeals = Table.tableNew();
			PluginLog.info("Fetching strategy booked for two days");
			strategyDeals =	fetchStrategyDeals();
			int numStrategyDeal = strategyDeals.getNumRows();
			PluginLog.info("Number of records returned for processing: " + numStrategyDeal);
			if(numStrategyDeal ==0){
				PluginLog.info("No strategy deals identified, Skipping further execution ...");
				return;
			}
			
			PluginLog.info("Inserting returned strategy to query_reslt");
			qid = Query.tableQueryInsert(strategyDeals, "deal_num");
			if(qid == 0){
				throw new OException("Issue when gettign Query Id for tran numbers");
			}
			
			PluginLog.info("Fetching data for non taxable strategy");
			nonTaxDeals = fetchNonTax();
			int numNonTaxDeals = nonTaxDeals.getNumRows();
			PluginLog.info("Number of records returned for processing: " + numNonTaxDeals);
			if(numNonTaxDeals >0){
				PluginLog.info("Updating user table for non taxable entries");
				stampDeals(nonTaxDeals);
			}
			
			PluginLog.info("Fetching data for  taxable strategy");
			taxDeals = fetchTaxDetails(strategyDeals,qid);
			int numTaxDeals = taxDeals.getNumRows();
			PluginLog.info("Number of records returned for processing: " + numTaxDeals);
			
			if(numTaxDeals >0){
				PluginLog.info("Updating user table for  taxable entries");
				stampDeals(taxDeals);
			}
            
            
            
          
          
		}catch (OException e) {
			PluginLog.error(e.getMessage());
			e.printStackTrace();
			Util.exitFail();
					
		}finally{
			if (Table.isTableValid(strategyDeals)==1)
				strategyDeals.destroy();
			if (Table.isTableValid(taxDeals)==1)
				taxDeals.destroy();
			if (Table.isTableValid(nonTaxDeals)==1)
				nonTaxDeals.destroy();
			if (Table.isTableValid(nonTaxDeals)==1)
				nonTaxDeals.destroy();	
			if(qid >0){
			  Query.clear(qid);
			}
		}
		return;
	}

	
		
		
	private Table fetchNonTax() throws OException {
		Table reportTable = Util.NULL_TABLE;
		try{
			reportTable = Table.tableNew("USER_Strategy_reportdata");
		String str = "SELECT distinct deal_num, cash_expected = 2 FROM USER_strategy_deals \n"+
				     "WHERE CAST(last_updated as DATE) > CAST(DATEADD(DAY,-2,GETDATE()) AS date) \n"+ 
				     "AND tran_status = "+TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+ "\n"+
				     "AND deal_num  not IN (SELECT distinct deal_num FROM USER_Strategy_reportdata)";
		PluginLog.info("Query to be executed: " + str);
		int ret = DBaseTable.execISql(reportTable, str);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			PluginLog.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while updating USER_strategy_deals failed"));
		}
		
		
	} catch (OException exp) {
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
		Table data = Util.NULL_TABLE;
		try {
			data = Table.tableNew("USER_strategy_deals");
			PluginLog.info("Fetching Strategy deals for processing");
			String sqlQuery = "SELECT deal_num FROM USER_strategy_deals  \n\r" + 
						 	  " WHERE tran_status =" + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt()+
						 	  " AND status = 'Succeeded' \n\r "+
						      " AND CAST(last_updated as DATE) > cast(DATEADD(DAY,-2,GETDATE()) AS date)";
			PluginLog.info("Query to be executed: " + sqlQuery);
			int ret = DBaseTable.execISql(data, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while updating USER_strategy_deals failed"));
			}
			
			
		
		} catch (OException exp) {
			 
			PluginLog.error("Error while fetching startegy Deals " + exp.getMessage());
			throw new OException(exp);
		}
		return data;
	}




	private Table  fetchTaxDetails(Table strategyDeals,int qid ) throws OException {
		Table taxDetails = Util.NULL_TABLE;
		int type_id = 20044;
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
					+" WHERE ab.buy_sell ="+BUY_SELL_ENUM.SELL.toInt()+ "\n"+
					" AND ab.toolset ="+TOOLSET_ENUM.COMPOSER_TOOLSET.toInt()+"\n"+
					"AND ab.ins_sub_type = "+INS_SUB_TYPE.cash_transaction.toInt()+"\n"+
					"AND ai.type_id = "+type_id+"  \n"
					+" AND ab.tran_status ="+TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+ ")A\n"
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
					+"and usd.tran_status = "+TRAN_STATUS_ENUM.TRAN_STATUS_NEW;

			PluginLog.info("Query to be executed: " + sql);
			int ret = DBaseTable.execISql(taxDetails, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException(DBUserTable.dbRetrieveErrorInfo(ret, "Failed while updating USER_strategy_deals failed"));
			}
		 }catch(OException oe){
			 PluginLog.error("Error while fetching tax details " + oe.getMessage());
			throw new OException(oe);
			 
		 }
		return taxDetails;
		
	}
}

