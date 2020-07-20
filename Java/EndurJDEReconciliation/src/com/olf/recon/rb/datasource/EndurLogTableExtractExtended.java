package com.olf.recon.rb.datasource;

import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.recon.enums.RegionBUEnum;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-04-09 	V1.0	joshig01		- Initial Version
 * 2020-07-09   V2.0    joshig01        - Changes to check for reset date rather than maturity date on the deal
 */

public class EndurLogTableExtractExtended implements IScript {

	private final String USER_JM_JDE_INTERFACE_RUN_LOG = "USER_jm_jde_interface_run_log";
	private final String USER_JM_JDE_EXTRACT_DATA = "USER_jm_jde_extract_data";
	private final String MATCH_STALE_DATA = "Mismatch - Stale Data";
	private final String MATCH_IGNORE = "Ignore";
	private final String MATCH = "Match";
	
	/*** Report parameters ***/
	private int queryResultID = 0;
	private String queryResultTable = null;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();
		Table logDeals = Table.tableNew();
		Table deals = Table.tableNew();
		
		int mode = argt.getInt("ModeFlag", 1);
		try {
			Logging.init(this.getClass(),"","");
			Logging.info("Executing EndurLogTableExtract");
		
			/* Meta data collection */
			if (mode == 0) 
			{
				setOutputFormat(returnt);
				
				return;
			}
			Table params = null;
			if (argt.getNumRows() > 0)
			{        	        	        	
				params = argt.getTable("PluginParameters", 1);
				queryResultID = argt.getInt("QueryResultID", 1);
				queryResultTable = argt.getString("QueryResultTable", 1);
			} else {
				params = Table.tableNew();
				params.addCol("dummy", COL_TYPE_ENUM.COL_STRING);
			}
			
			runSql (logDeals, params);
			
			/*** If we have used query then only add deals that are not in interface log table ***/
			if (queryResultID > 0) {
				
				addNotInInterfaceDeals (deals, params);	
				if (deals != null && deals.getNumRows() > 0)
					deals.copyRowAddAll(logDeals);
			}

			returnt.select(logDeals, "*", "deal_num GT 0");
			returnt.addCol("key", COL_TYPE_ENUM.COL_STRING);
			
			/*** Stamp records  -
			 *   Mark as Ignore if at least one record of the deal matches spot equivalent value
			 *   Mark as Match where spot equivalent values match
			 *   Mark as Stale Date if none of records have matching spot equivalent value 
			 ***/
			stampRecords (returnt);
			setDerivedValued(returnt, params); 
			Logging.info("Records returned: " + returnt.getNumRows());
			//returnt.viewTable();
		}
		catch (Exception e) {
			throw new RuntimeException("Exception occurred while running report. \nException: " + e.getMessage() + ".\nTrace: " + e.getStackTrace());
		}
		finally {
			logDeals.destroy();
			deals.destroy();
			Logging.close();
		}
	}

	private void setDerivedValued(Table returnt, Table params) throws OException {
		for (int row = returnt.getNumRows(); row >= 1; row--) {
			String ledgerType = returnt.getString("ledger_type", row);
			int endurDocNum = returnt.getInt("endur_doc_num", row);
			String accountNum = returnt.getString("account_num", row);
			String debitCredit = returnt.getString("debit_credit", row);
			int dealNum = returnt.getInt("deal_num", row);
			double ledgerAmount = returnt.getDouble("ledger_amount", row);
			String region = returnt.getString("region", row);
			String interfaceMode = returnt.getString("interface_mode", row);
			
			String key=null;
			if (interfaceMode.equalsIgnoreCase("SL") && region.equalsIgnoreCase("United States")) {
				key = getKey (endurDocNum, accountNum, debitCredit, ledgerAmount);
			} else if ("CustomerInvoice".equalsIgnoreCase(ledgerType)) {
				key = region.equalsIgnoreCase("China") ? getKey (dealNum, accountNum, debitCredit, ledgerAmount) : getKey (endurDocNum, accountNum, debitCredit, ledgerAmount);
			} else {
				key = getKey (dealNum, accountNum, debitCredit, ledgerAmount);
			}
			returnt.setString("key", row, key);
		}
			
	
	}

	private void runSql(Table returnt, Table params) throws OException {
		List<String> availableParams = getAvailableParams (params);
		StringBuilder sql = new StringBuilder();
		sql.append("\nWITH max_reset_date AS (");
		sql.append("\n     SELECT   r.ins_num");
		sql.append("\n            , ab.deal_tracking_num");
		sql.append("\n            , ab.toolset");
		sql.append("\n            , MAX(r.reset_date) reset_date ");
		sql.append("\n     FROM reset r, ab_tran ab, USER_JM_JDE_INTERFACE_RUN_LOG ulog");
		sql.append("\n     WHERE r.ins_num = ab.ins_num");
		sql.append("\n     AND   ab.deal_tracking_num = ulog.deal_num");
		sql.append("\n     AND   ab.current_flag = 1");
		sql.append("\n     AND   ab.toolset = 15");

		if (availableParams.size() > 0) {
			for (String param : availableParams) {
				String value = getStringParam (params, param);

				sql.append("\n     AND   ulog.").append(param).append(" ");
				sql.append(value);
			}
		}
		
		sql.append("\n    GROUP BY r.ins_num, ab.deal_tracking_num, ab.toolset)");
		sql.append("\nSELECT ulog.extraction_id");
		sql.append("\n      ,ulog.interface_mode");
		sql.append("\n      ,ulog.region");
		sql.append("\n      ,ulog.internal_bunit");
		sql.append("\n      ,ulog.deal_num");
		sql.append("\n      ,ulog.endur_doc_num");
		sql.append("\n      ,ulog.trade_date");
		sql.append("\n      ,ulog.metal_value_date");
		sql.append("\n      ,ulog.cur_value_date");
		sql.append("\n      ,ulog.account_num");
		sql.append("\n      ,ulog.qty_toz");
		sql.append("\n      ,ulog.ledger_amount");
		sql.append("\n      ,ulog.tax_amount");
		sql.append("\n      ,ulog.debit_credit");
		sql.append("\n      ,ulog.ledger_type");
		sql.append("\n      ,ulog.time_in");
		sql.append("\n      ,ulog.doc_date");
		sql.append("\n      ,ISNULL(udata.deal_num, -999) as data_deal_num");
		sql.append("\n      ,ISNULL(udata.spot_equiv_value, -999999999999.99) as spot_equiv_value ");
		sql.append("\n      ,CASE WHEN ulog.region = 'China' ");
		sql.append("\n            THEN CASE WHEN ulog.ledger_type = 'GeneralLedger'");
		sql.append("\n                      THEN CASE WHEN ISNULL(udata.settlement_value,-999999999999.99) = ulog.ledger_amount");
		sql.append("\n                                THEN 1.0 ");
		sql.append("\n                                ELSE 0.0 ");
		sql.append("\n                           END ");
		sql.append("\n                      ELSE 0.0 ");
		sql.append("\n                 END "); 
		sql.append("\n            ELSE CASE WHEN ISNULL(udata.spot_equiv_value,-999999999999.99) = ulog.ledger_amount");
		sql.append("\n                      THEN 1.0 ");
		sql.append("\n                      ELSE 0.0 ");
		sql.append("\n                 END "); 
		sql.append("\n       END as ohd_amt_match ");	
		sql.append("\n      ,CASE WHEN ulog.region = 'China' ");
		sql.append("\n            THEN CASE WHEN ulog.ledger_type = 'GeneralLedger'");
		sql.append("\n                      THEN CASE WHEN ROUND(ISNULL(udata.settlement_value,-999999999999.99),2) = ROUND(ulog.ledger_amount,2)");
		sql.append("\n                                THEN '").append(MATCH).append("' ");	
		sql.append("\n                                ELSE '").append(MATCH_IGNORE).append("' ");
		sql.append("\n                           END ");
		sql.append("\n                      ELSE '").append(MATCH_IGNORE).append("' ");
		sql.append("\n                 END ");
		sql.append("\n            ELSE CASE WHEN ROUND(ISNULL(udata.spot_equiv_value,-999999999999.99),2)  = ROUND(ulog.ledger_amount,2)");
		sql.append("\n                      THEN '").append(MATCH).append("' ");
		sql.append("\n                      ELSE CASE WHEN ulog.ledger_type = 'CustomerInvoice'");
		sql.append("\n                                THEN '").append(MATCH_IGNORE).append("' ");
		sql.append("\n                                ELSE CASE WHEN ab.toolset = 15");
		sql.append("\n                                          THEN CASE WHEN format(ulog.time_in, 'yyyyMMdd') < format(mr.reset_date, 'yyyyMMdd')");
		sql.append("\n                                                    THEN '").append(MATCH_IGNORE).append("' ");
		sql.append("\n                                                    ELSE '").append(MATCH_STALE_DATA).append("' ");
		sql.append("\n                                               END "); 
		sql.append("\n                                          ELSE '").append(MATCH_IGNORE).append("' ");
		sql.append("\n                                     END ");
		sql.append("\n                           END ");
		sql.append("\n                 END ");
		sql.append("\n        END as spot_eq_value_match ");
		sql.append("\n       ,ab.toolset");
		sql.append("\nFROM ").append(USER_JM_JDE_INTERFACE_RUN_LOG).append(" ulog");
		sql.append("\nINNER JOIN ab_tran ab ON ab.deal_tracking_num = ulog.deal_num AND ab.current_flag = 1");
		sql.append("\nLEFT OUTER JOIN ").append(USER_JM_JDE_EXTRACT_DATA).append(" udata ON ulog.deal_num = udata.deal_num");
		sql.append("\nLEFT OUTER JOIN max_reset_date mr ON mr.deal_tracking_num = ulog.deal_num");
		if (availableParams.size() > 0) {
			sql.append("\nWHERE ");
			boolean first = true;
			for (String param : availableParams) {
				String value = getStringParam (params, param);
				sql.append("\n	");
				if (!first) {
					sql.append(" AND ");
				} else {
					first=false;
					sql.append("    ");					
				}
				sql.append("ulog.").append(param).append(" ");
				sql.append(value);
			}
		}
		
		execSQL (returnt, sql.toString());
	}

	private void setOutputFormat(Table output) throws OException {
		output.setTableName(USER_JM_JDE_INTERFACE_RUN_LOG);
		int ret = DBUserTable.structure(output);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			throw new RuntimeException ("Error retrieving structure of table " + USER_JM_JDE_INTERFACE_RUN_LOG);
		}
		output.addCol("key", COL_TYPE_ENUM.COL_STRING);
	}
	
	/**
	 * Find the param row  
	 * 
	 * @param customParam
	 * @return
	 * @throws OException
	 */
	private int getRow(Table params, String customParam) throws OException
	{
		int findRow = -1;
		
		findRow = params.unsortedFindString(1, customParam, SEARCH_CASE_ENUM.CASE_INSENSITIVE);        
		
		return findRow;
	}
	
	public String getStringParam(Table params, String paramName) throws OException
	{
		String paramValue = "";
		
		int findRow = getRow(params, paramName);
		if (findRow > 0)
		{
			paramValue = params.getString(2, findRow);
		}
		
		return paramValue;
	}
	
	public List<String> getAvailableParams(Table params) throws OException
	{
		List<String> availableParams = new ArrayList<>(params.getNumRows());
		Table runLogTable = Table.tableNew(USER_JM_JDE_INTERFACE_RUN_LOG);
		DBUserTable.structure(runLogTable);	
		for (int row=params.getNumRows(); row >= 1; row --) {
			String param = params.getString(1, row);
			if (runLogTable.getColNum(param) > 0) {
				availableParams.add(param);				
			}
		}
		runLogTable.destroy();
		
		return availableParams;
	}
	
	private String getKey (int docOrDealNum, String accountNum, String debitCredit, double ledgerAmount) throws OException {
		return "" + docOrDealNum + "-" 
	              + (accountNum==null?"":accountNum.trim()) + "-" 
				  + (debitCredit==null?"":debitCredit) + "-" 
	              + String.format("%.2f", ledgerAmount);
		
	}
	
	private void stampRecords (Table returnt) throws OException {
		Table amtMatch = Table.tableNew();
		Table atLeastOneMatch = Table.tableNew();
		Table noMatch = Table.tableNew();
		
		try {
			/*** For each deal get the number of matches on spot eq value ***/
			amtMatch.select(returnt, "DISTINCT, deal_num", "deal_num GT 0");
			amtMatch.select(returnt, "SUM, amt_match(deal_amt_match)", "deal_num EQ $deal_num");
			returnt.select(amtMatch, "deal_amt_match", "deal_num EQ $deal_num");
			
			/*** Get the records where at least one spot eq value matches. All such records will be set to Ignore. ***/
			atLeastOneMatch.select(returnt, "deal_num, account_num, debit_credit, spot_eq_value_match", "amt_match EQ 0 AND deal_num GT 0 AND deal_amt_match GT 0");
			atLeastOneMatch.setColValString("spot_eq_value_match", MATCH_IGNORE);
			
			/*** Mark non-matching rows as ignore. Note that matching rows are already marked as such in the SQL. ***/
			returnt.select(atLeastOneMatch, "spot_eq_value_match", "deal_num EQ $deal_num AND account_num EQ $account_num AND debit_credit EQ $debit_credit");
			
			/*** Where there is not even a single matching spot eq value record for a deal, mark all rows for the deal as Stale Data ***/			
			noMatch.select(returnt, "deal_num, account_num, debit_credit, spot_eq_value_match", "deal_amt_match EQ 0 AND toolset NE 15 AND deal_num GT 0 AND data_deal_num GT 0");
			noMatch.setColValString("spot_eq_value_match", MATCH_STALE_DATA);
			returnt.select(noMatch,  "spot_eq_value_match", "deal_num EQ $deal_num AND account_num EQ $account_num AND debit_credit EQ $debit_credit");
						
		} catch (Exception e) {
			throw new OException ("Unable to stamp records as matching spot eq value. \nException: " + e.getMessage());
		} finally {
			amtMatch.destroy();
			atLeastOneMatch.destroy();
			noMatch.destroy();
		}
	}
	
	private void addNotInInterfaceDeals (Table deals, Table params) throws OException {
		
		String region_input = getStringParam (params, "region_input");
		//String interface_mode_input = getStringParam (params, "interface_mode_input").replace("'", "");
		//String region = getStringParam (params, "region");
		//String interface_mode = getStringParam (params, "interface_mode");
		//String time_in = getStringParam (params, "time_in");
		String event_from = getStringParam (params, "event_from");
		String input_date_from = getStringParam (params, "input_date_from");
		
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT DISTINCT ");
		sql.append("\n       -1 as extraction_id");
		sql.append("\n      ,' ' as interface_mode");
		sql.append("\n      ,'" + region_input + "' as region");
		sql.append("\n      ,ab.internal_bunit");
		sql.append("\n      ,ab.deal_tracking_num as deal_num");
		sql.append("\n      ,0 as endur_doc_num");
		sql.append("\n      ,0 as trade_date");
		sql.append("\n      ,0 as metal_value_date");
		sql.append("\n      ,0 as cur_value_date");
		sql.append("\n      ,'' as account_num");
		sql.append("\n      ,ab.position as ohd_qty_toz");
		sql.append("\n      ,ab.position as ohd_ledger_amount");
		sql.append("\n      ,0.00 as ohd_tax_amount");
		sql.append("\n      ,' ' as debit_credit");
		sql.append("\n      ,' ' as ledger_type");
		sql.append("\n      ,null as time_in");
		sql.append("\n      ,0 as doc_date");
		sql.append("\n      ,-1 as data_deal_num");
		sql.append("\n      ,0.00 as ohd_spot_equiv_value ");
		sql.append("\n      ,-1.00 as ohd_amt_match ");
		sql.append("\n      ,'NotSent' as spot_eq_value_match ");
		sql.append("\nFROM ab_tran ab");
		sql.append("\nINNER JOIN ").append(queryResultTable).append(" qr");
		sql.append("\n           ON ab.tran_num = qr.query_result");
		sql.append("\n           AND qr.unique_id = " + queryResultID);
		sql.append("\nINNER JOIN ab_tran_event abte ON abte.tran_num = ab.tran_num");
		sql.append("\nWHERE ab.internal_bunit IN (" + getBUs(region_input) + ")");
		sql.append("\nAND NOT EXISTS ( SELECT 1 FROM ").append(USER_JM_JDE_INTERFACE_RUN_LOG).append(" ulog");
		sql.append("\n                 WHERE ab.deal_tracking_num = ulog.deal_num ");
		//sql.append("\n                 AND ulog.region " + region);
		//sql.append("\n                 AND ulog.interface_mode " + interface_mode);
		//sql.append("\n                 AND ulog.time_in " + time_in);
		sql.append("\n               ) ");
		
		/*** Restrict data to passed parameters ***/
		sql.append("\nAND (    (    ab.input_date " + input_date_from);
		sql.append("\n          AND abte.event_date " + event_from + ")");
		sql.append("\n      OR (abte.event_date " + input_date_from + ")");
		sql.append("\n    )");
		
		execSQL (deals, sql.toString());
	}
	
	private String getBUs (String region) throws OException {
		String business_units = null;
		switch (region) {
		case "United Kingdom": 
			business_units = RegionBUEnum.UK.getBUs(); 
			break;
		case "China":
			business_units = RegionBUEnum.CHINA.getBUs();
			break;
		case "United States":
			business_units = RegionBUEnum.US.getBUs();
			break;
		}
		
		Logging.info("For region: " + region + " BU: " + business_units);
		return business_units;
	}
	
	private void execSQL (Table deals, String sql) throws OException {
		try {
			Logging.info("Executing SQL: " + sql);
			int ret = DBaseTable.execISql(deals, sql.toString());
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new RuntimeException ("Error executing SQL: " + sql);
			}			
		} catch (Exception ex) {
			// ensure SQL is part of exception message
			throw new RuntimeException ("Error executing SQL: " + sql + ".\nException: " + ex.getMessage() + "\nStack: " + ex.getStackTrace());			
		}
	}
}
