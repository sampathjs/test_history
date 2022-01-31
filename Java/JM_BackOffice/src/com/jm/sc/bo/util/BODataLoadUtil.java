package com.jm.sc.bo.util;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import org.apache.commons.lang3.ArrayUtils;
import com.olf.jm.logging.Logging;

/*
 * History: 
 * 2015-04-28	V1.0	 Prashanth 	- initial version - inital version to populate few columns which will 
 * 									  be used in JM_DL_Metal, JM_DL_Netting and queryScript
 * 2022-01-12   V1.1	 Denes Nagy - added tolerance level to account balance calculations
 * 2022-01-19   V1.2	 Denes Nagy - past receivables logic integration with MTS tables
 */

public class BODataLoadUtil {
	
	private static final String ACCT_CLASS_METAL = "Metal Account";
	private static final String CONST_REPO_VAR_CONFIRM_STATUS_FR_STP = "confirmStatusForSTP";
	private static final String CONST_REPO_VAR_TOLERANCE = "balanceSignTolerance"; // tolerance level to still count account balance as positive
	private static final String CONST_PAST_REC_FROM_DATE = "countPastReceivablesFromDate";
	private static final String ARGT_COL_NAME_PAST_RECEIVABLES = "past_receivables";
	private static final String ARGT_COL_TITLE_PAST_RECEIVABLES = "Past\nReceivables";
	private static final String ARGT_COL_NAME_CONFIRM_STATUS = "confirm_status";
	private static final String ARGT_COL_TITLE_CONFIRM_STATUS = "Confirm\nStatus";
	private static final String ARGT_COL_NAME_DEAL_METAL_BALANCE = "deal_metal_balance";
	private static final String ARGT_COL_TITLE_DEAL_METAL_BALANCE = "Deal Metal\nBalance";
	private static final String ARGT_COL_NAME_DEAL_METAL_BALANCE_VALUE = "deal_metal_balance_value";
	private static final String ARGT_COL_TITLE_DEAL_METAL_BALANCE_VALUE = "Deal Metal\nBalance Value";
	private static final String ARGT_COL_NAME_ANY_OTHER_BALANCE = "any_other_metal_loco_balance";
	private static final String ARGT_COL_TITLE_ANY_OTHER_BALANCE = "Any Other Metal\nLoco Balance";
	private static final String ARGT_COL_NAME_STP_STATUS = "stp_status_jm";
	private static final String ARGT_COL_TITLE_STP_STATUS = "STP\nStatus JM";

	ConstRepository _constRepo = null;
	private Table argumentTable = null;
	private int qid = 0;
	private String qtbl = "";
	private boolean isQueryScript;
	
	public BODataLoadUtil(Table argt, int qid, String qtbl, boolean isQueryScript) throws OException{
		
		_constRepo = new ConstRepository("BackOffice", "Dataload Metal");
		if(!isQueryScript){
			argumentTable = argt;
		}
		this.qid = qid;
		this.qtbl = qtbl;
		this.isQueryScript = isQueryScript;
	}
	
	/**
	 * This method adds columns to argt - past_receivables, confirm_status, deal_metal_balance
	 * , deal_metal_balance_value, any_other_metal_loco_balance, stp_status_jm
	 * 
	 * @param argt
	 * @throws OException
	 */
	public void loadArgt() throws OException {	

		// Get count of metal accounts in scope for each counterparty
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT sh.document_num, ab.tran_num, ab.external_bunit, abe.event_num, abe.event_type");
		sql.append("\n    , sd.cflow_type, sd.tran_currency, abiv.value tran_info_type_20015");
		sql.append("\n  FROM ab_tran_event abe");
		sql.append("\n  JOIN ").append(qtbl).append(" qr ON abe.event_num = qr.query_result ");
		sql.append("\n       AND qr.unique_id = ").append(qid);
		sql.append("\n  JOIN ab_tran ab ON ab.tran_num = abe.tran_num");
		sql.append("\n  LEFT JOIN ab_tran_info_view abiv ON ab.tran_num = abiv.tran_num AND abiv.type_name = 'Loco'");
		sql.append("\n  JOIN stldoc_details sd ON abe.event_num = sd.event_num ");
		sql.append("\n  JOIN stldoc_header sh ON sd.document_num = sh.document_num AND sh.doc_type = 20001 ");
		sql.append("\n  WHERE ab.tran_status = 3");

		argumentTable = Table.tableNew();
		long currentTime = System.currentTimeMillis();
		Logging.info("Query to load argt details: " + sql.toString());
		DBaseTable.execISql(argumentTable, sql.toString());
		Logging.info("Query for Metal accounts in scope for each counterparty - completed in "
				+ (System.currentTimeMillis() - currentTime) + " ms");
		// Update qid and qtbl so that it reflects query id for tran numbers
		this.qid = Query.tableQueryInsert(argumentTable, "tran_num");
		this.qtbl = Query.getResultTableForId(qid);
	}
	
	/**
	 * This method adds columns to argt - past_receivables, confirm_status, deal_metal_balance
	 * , deal_metal_balance_value, any_other_metal_loco_balance, stp_status_jm
	 * 
	 * @param argt
	 * @throws OException
	 */
	public void addColums() throws OException {
		argumentTable.addCol(ARGT_COL_NAME_PAST_RECEIVABLES, COL_TYPE_ENUM.COL_STRING, ARGT_COL_TITLE_PAST_RECEIVABLES);
		argumentTable.addCol(ARGT_COL_NAME_CONFIRM_STATUS, COL_TYPE_ENUM.COL_INT, ARGT_COL_TITLE_CONFIRM_STATUS);
		argumentTable.addCol(ARGT_COL_NAME_DEAL_METAL_BALANCE, COL_TYPE_ENUM.COL_STRING, ARGT_COL_TITLE_DEAL_METAL_BALANCE);
		argumentTable.addCol(ARGT_COL_NAME_DEAL_METAL_BALANCE_VALUE, COL_TYPE_ENUM.COL_DOUBLE, ARGT_COL_TITLE_DEAL_METAL_BALANCE_VALUE);
		argumentTable.addCol(ARGT_COL_NAME_ANY_OTHER_BALANCE, COL_TYPE_ENUM.COL_STRING, ARGT_COL_TITLE_ANY_OTHER_BALANCE);
		argumentTable.addCol(ARGT_COL_NAME_STP_STATUS, COL_TYPE_ENUM.COL_STRING, ARGT_COL_TITLE_STP_STATUS);
	}
	
	
	/**
	 * This method updates the past receivables field to 'Pending'
	 * for the list of business units where they have not yet paid to JM as per AutoMatch recon status.
	 * For all others it is set to 'Cleared'.
	 * @throws OException
	 */
	public void populatePastReceivables() throws OException {
		
		int datefrom = _constRepo.getDateValue(CONST_PAST_REC_FROM_DATE);
		
		argumentTable.setColValString(ARGT_COL_NAME_PAST_RECEIVABLES, "Cleared");
        
        StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT ab_tran.external_bunit, 'Pending' AS ").append(ARGT_COL_NAME_PAST_RECEIVABLES);
		sql.append("\n FROM ab_tran_event, ab_tran, account_view int_account_info, ab_tran_event_settle abes_ai, stldoc_header, stldoc_details std, configuration conf");
        sql.append("\n WHERE (ab_tran.tran_num = ab_tran_event.tran_num)  and (ab_tran_event.event_num = abes_ai.event_num) and (abes_ai.int_account_id = int_account_info.account_id)  and (ab_tran_event.event_num = std.event_num) and (std.document_num = stldoc_header.document_num)");
		sql.append("\n AND (ab_tran.tran_status in (3,4))  and (ab_tran.toolset in (6,9,10,15))"); //validated, matured
		sql.append("\n AND (ab_tran_event.event_date < conf.prev_business_date) and (ab_tran_event.event_date > " + datefrom + ")");  //since date specified in Const Rep
		sql.append("\n AND (ab_tran_event.para_position < 0)"); //only money owed to JM
		sql.append("\n AND (ab_tran_event.event_type in (14,98)) and (ab_tran_event.internal_conf_status = 2)"); //cash & tax settlement, known status
		sql.append("\n AND (int_account_info.account_id in (select account_id from mts_exp_account_number where exp_status_id=1))"); //only for accounts in scope for matching
		sql.append("\n AND (stldoc_header.stldoc_def_id = 20003)");  //payment documents only
		sql.append("\n AND (not exists (select * from mts_event_recon mer LEFT OUTER JOIN mts_cash_applied_link dxr17 ON (mer.event_num=dxr17.object_id) ");
		sql.append("\n LEFT OUTER JOIN mts_cash_applied_link dxr18 ON (dxr18.payment_grp_id = dxr17.payment_grp_id and dxr18.payment_id = 0) ");
		sql.append("\n LEFT OUTER JOIN swift_940t_header dxr19 ON (dxr19.record_id=dxr18.object_id) ");
		sql.append("\n WHERE (mer.event_tracking_num = ab_tran_event.event_tracking_num and ((mer.recon_status_id = 9) or (mer.recon_status_id=10) and (dxr19.recon_status_id=21)))))");
			//for which there is no 'Paid' event recon status record in MTS/AutoMatch and also no 'Partial' event recon status paired with "Matched with bank charge" statement recon status
		
		Table tbl = null;	
		try {
			Logging.info("Query(for "+ARGT_COL_NAME_PAST_RECEIVABLES +"): "  +sql);
			tbl = Table.tableNew();
			long currentTime = System.currentTimeMillis();
			DBaseTable.execISql(tbl, sql.toString());
			Logging.info("Query(for "+ARGT_COL_NAME_PAST_RECEIVABLES +") - completed in " + (System.currentTimeMillis()- currentTime) + " ms");
			
			if (tbl.getNumRows() > 0) {
				argumentTable.select(tbl, ARGT_COL_NAME_PAST_RECEIVABLES, "external_bunit EQ $external_bunit");
			}
		} finally {
			if (tbl != null && Table.isTableValid(tbl) == 1) {
				tbl.destroy();
			}
		}
	}
	
	
	
	/**
	 * This method updates the confirm status field based on the below logic
	 * Show the doc status of the latest "Confirm" type doc that belongs to the same deal;
	 * Show N/A if there is not a Confirm doc found.
	 * Caveat: on FX far legs the confirmation status has to be pulled from the FX near leg
	 * , as that is where it is linked to only.
	 * 
	 * @throws OException
	 */
	public void populateConfirmStatus() throws OException {
		String sql;
		Table tbl=null;
		// = Confirm doc and not FX far leg
		sql =     "\nSELECT sd.tran_num, sh.doc_status AS "+ARGT_COL_NAME_CONFIRM_STATUS
				+ "\nFROM "+qtbl+ " qr"
				+ "\n  INNER JOIN stldoc_document_type sdt ON sdt.doc_type_desc = 'Confirm'"
				+ "\n  INNER JOIN ins_sub_type ist ON ist.name = 'FX-FARLEG'"
				+ "\n  INNER JOIN stldoc_details sd " 
				+ "\n    ON sd.tran_num=qr.query_result"
				+ "\n      AND sd.ins_sub_type != ist.id_number"
				+ "\n  INNER JOIN stldoc_header sh "
				+ "\n    ON sd.document_num=sh.document_num"
				+ "\n      AND sh.doc_type=sdt.doc_type"
				+ "\nWHERE qr.unique_id="+qid
				;
		try {
			tbl = Table.tableNew(sql);
			long currentTime = System.currentTimeMillis();
			Logging.info("Query(for "+ARGT_COL_NAME_CONFIRM_STATUS +"-1): " + sql);
			DBaseTable.execISql(tbl, sql);
			Logging.info("Query(for "+ARGT_COL_NAME_CONFIRM_STATUS +"-1) - completed in " + (System.currentTimeMillis()- currentTime) + " ms");
			
			if (tbl.getNumRows() > 0) {
				argumentTable.select(tbl, ARGT_COL_NAME_CONFIRM_STATUS, "tran_num EQ $tran_num");
			}
		} finally {
			if (tbl != null && Table.isTableValid(tbl) == 1) {
				tbl.destroy();
			}
		}
		
		sql = 	    "\nSELECT qr.query_result as tran_num"
				+   "\n, sh.doc_status as "+ARGT_COL_NAME_CONFIRM_STATUS
				+   "\nFROM " + qtbl + " qr"
				+   "\n  INNER JOIN stldoc_document_type sdt ON sdt.doc_type_desc = 'Confirm'"
				+   "\n  INNER JOIN ins_sub_type ist_far ON ist_far.name = 'FX-NEARLEG'"
				+   "\n  INNER JOIN ins_sub_type ist_near ON ist_near.name = 'FX-FARLEG'"
				+   "\n  INNER JOIN stldoc_header sh "
				+   "\n    ON sh.doc_type=sdt.doc_type"
				+   "\n  INNER JOIN stldoc_details sd "
				+   "\n    ON sd.document_num=sh.document_num"
				+   "\n      AND (sd.tran_num+1)=qr.query_result"
				+   "\n      AND sd.ins_sub_type = ist_near.id_number"
				+   "\n  INNER JOIN ab_tran ab "
				+   "\n    ON qr.query_result=ab.tran_num"
				+   "\n      AND ab.ins_sub_type = ist_far.id_number"
				+   "\nWHERE qr.unique_id="+qid
				; 
				// = Confirm doc and FX far leg => here the confirm is retrieved from the near leg
		try {
			tbl = Table.tableNew(sql);
			long currentTime = System.currentTimeMillis();
			Logging.info("Query(for "+ARGT_COL_NAME_CONFIRM_STATUS +"-2): " + sql);
			DBaseTable.execISql(tbl, sql);
			Logging.info("Query(for "+ARGT_COL_NAME_CONFIRM_STATUS +"-2) - completed in " + (System.currentTimeMillis()- currentTime) + " ms");
			
			if (tbl.getNumRows() > 0) {
				argumentTable.select(tbl, ARGT_COL_NAME_CONFIRM_STATUS, "tran_num EQ $tran_num");
			}
		} finally {
			if (Table.isTableValid(tbl) == 1) {
				tbl.destroy();
			}
		}
	}
	
	
	
	/**
	 * This method updates the deal_metal_balance field based on the below logic
	 * Check if deal has metal: if no, set to "N/A - deal" and end check.
	 * Check if deal's metal location is not in scope [inscope only if holding bank in JM group], 
	 * if not set to "N/A - location" and end check.
	 * Check metal account balance (metal = deal metal; loco = deal loco) and display its sign:
	 * Positive (including zero) or Negative.
	 *
	 * @throws OException
	 */
	public void populateDealMetalBalance() throws OException {

		// Check if deal has metal: if no, set to "N/A - deal" and end check.
		// Check if deal's metal location is not in scope [holding bank in JM
		// 			group], if not set to "N/A - location" and end check.
		// Check metal account balance (metal=deal metal; loco=deal loco) and
		//			display its sign: Positive (including zero plus tolerance from constant repository) or Negative.

		int tolerance = _constRepo.getIntValue(CONST_REPO_VAR_TOLERANCE);
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT sd.tran_num, sd.external_bunit, sd.document_num, sd.event_type, sd.cflow_type, ");
		sql.append("\n    mes.ext_account_id, ");
		sql.append("\n  CASE WHEN sd.tran_currency NOT IN (SELECT id_number FROM currency WHERE precious_metal = 1) ");
		sql.append("\n      THEN 'N/A - deal'");
		sql.append("\n      WHEN mes.ext_account_id IN (SELECT a.account_id FROM account a ");
		sql.append("\n                                 JOIN party p ON a.holder_id = p.party_id AND p.int_ext != 0)");
		sql.append("\n      THEN 'N/A - location'");
		sql.append("\n      WHEN (acc_bal.running_balance ) >= "+ tolerance +" THEN 'Positive'");
		sql.append("\n      ELSE 'Negative'");
		sql.append("\n  END ").append(ARGT_COL_NAME_DEAL_METAL_BALANCE);
		sql.append("\n, CASE WHEN sd.tran_currency NOT IN (SELECT id_number FROM currency WHERE precious_metal = 1) ");
		sql.append("\n      THEN 0.0");
		sql.append("\n      WHEN mes.ext_account_id IN (SELECT a.account_id FROM account a ");
		sql.append("\n                                 JOIN party p ON a.holder_id = p.party_id AND p.int_ext != 0)");
		sql.append("\n      THEN 0.0");
		sql.append("\n      ELSE acc_bal.running_balance");
		sql.append("\n  END ").append(ARGT_COL_NAME_DEAL_METAL_BALANCE_VALUE);
		sql.append("\nFROM ").append(qtbl).append(" qr");
		sql.append("\n JOIN stldoc_details sd ON sd.tran_num = qr.query_result");
		sql.append("\n LEFT JOIN ab_tran_event me ON sd.tran_num = me.tran_num");
		sql.append("\n      AND me.currency IN (SELECT id_number FROM currency WHERE precious_metal = 1)");
		sql.append("\n LEFT JOIN ab_tran_event_settle mes ON mes.event_num = me.event_num");
		sql.append("\n JOIN ( SELECT DISTINCT ates.ext_account_id, ate.event_date, ates.currency_id");
		sql.append("\n            , ROUND(SUM(-ate.para_position) OVER (PARTITION BY ates.ext_account_id");
		sql.append("\n                , ates.currency_id ORDER BY ate.event_date),6) running_balance");
		sql.append("\n 	      FROM ab_tran_event ate");
		sql.append("\n        JOIN ab_tran_event_settle ates  ON ates.event_num = ate.event_num");
		sql.append("\n        JOIN ab_tran ab ON ab.tran_num = ate.tran_num AND ab.current_flag = 1");
		sql.append("\n              AND tran_status IN (3,4) )");
		sql.append("\n  acc_bal ON acc_bal.ext_account_id = mes.ext_account_id AND acc_bal.currency_id = me.currency");
		sql.append("\n          AND acc_bal.event_date = me.event_date");
		sql.append("\n WHERE qr.unique_id = ").append(qid);
		
		Table dealMetalBalance = Table.tableNew();
		long currentTime = System.currentTimeMillis();
		Logging.info("Query for " + ARGT_COL_NAME_DEAL_METAL_BALANCE + "): " + sql.toString());
		DBaseTable.execISql(dealMetalBalance, sql.toString());
		Logging.info("Query for " + ARGT_COL_NAME_DEAL_METAL_BALANCE + ") - completed in "
				+ (System.currentTimeMillis() - currentTime) + " ms");

		if (dealMetalBalance.getNumRows() > 0) {
			argumentTable.select(dealMetalBalance, ARGT_COL_NAME_DEAL_METAL_BALANCE + ", " 
					+ ARGT_COL_NAME_DEAL_METAL_BALANCE_VALUE, "tran_num EQ $tran_num AND document_num EQ $document_num"
					+ " AND event_type EQ $event_type AND cflow_type EQ $cflow_type");
		}
	}
	
	
	
	/**
	 * This method updates the any_other_metal_loco_balance field based on the below logic
	 * Check if counterparty has any metal account in scope: if not, set to "N/A - cp" and end check.
	 * Check if any of the counterparty's metal account balances is negative, across all JM group 
	 * locations and metals, excluding the deal's own location and metal: 
	 * if yes, set to "[acct identifier] - Negative" (the first negative balance), 
	 * otherwise "All Positive". Sign check includes tolerance.
	 * 
	 * @throws OException
	 */
	public void populateAnyOtherBalance() throws OException {

		int tolerance = _constRepo.getIntValue(CONST_REPO_VAR_TOLERANCE);

		// Get count of metal accounts in scope for each counterparty
		int acm = Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_CLASS_TABLE, ACCT_CLASS_METAL);
		StringBuilder maInScopeSql = new StringBuilder();
		maInScopeSql.append("SELECT count(a.account_number) metal_acc_in_scope, pa.party_id");
		maInScopeSql.append("\n  FROM account a ");
		maInScopeSql.append("\n  JOIN party_account pa ON a.account_id = pa.account_id");
		maInScopeSql.append("\n  JOIN party ho ON a.holder_id = ho.party_id  AND ho.int_ext = 0");
		maInScopeSql.append("\n WHERE a.account_class = ").append(acm);
		maInScopeSql.append("\n   AND a.account_status = 1");
		maInScopeSql.append("\n GROUP BY pa.party_id");
		maInScopeSql.append("\n ORDER BY pa.party_id");
		Table maInScope = Table.tableNew();
		long currentTime = System.currentTimeMillis();
		Logging.info("Query for Metal accounts in scope for each counterparty: " + maInScopeSql.toString());
		DBaseTable.execISql(maInScope, maInScopeSql.toString());
		Logging.info("Query for Metal accounts in scope for each counterparty - completed in "
				+ (System.currentTimeMillis() - currentTime) + " ms");

		// Get negative metal account balance for all accounts liked to each counterparty
		StringBuilder maNegBalSql = new StringBuilder();
		maNegBalSql.append("SELECT acc.account_id, ab.external_bunit, p1.short_name as customer");
		maNegBalSql.append("\n   , ates.currency_id as metal, ROUND(SUM(-ate.para_position), 6) balance, ai.info_value loco");
		maNegBalSql.append("\n   , ROW_NUMBER() OVER (PARTITION BY external_bunit ORDER BY ROUND(SUM(-ate.para_position),6)) rn");
		maNegBalSql.append("\n  FROM ab_tran ab");
		maNegBalSql.append("\n  JOIN ab_tran_event ate ON ate.tran_num = ab.tran_num");
		maNegBalSql.append("\n  JOIN ab_tran_event_settle ates  ON ates.event_num = ate.event_num");
		maNegBalSql.append("\n  JOIN account acc  ON acc.account_id = ates.ext_account_id AND acc.account_class = ").append(acm);
		maNegBalSql.append("\n  JOIN idx_unit iu ON iu.unit_id= ates.unit");
		maNegBalSql.append("\n  JOIN party p1 ON p1.party_id = ab.external_bunit");
		maNegBalSql.append("\n  JOIN party ho ON acc.holder_id = ho.party_id  AND ho.int_ext = 0");
		maNegBalSql.append("\n  JOIN account_info ai ON ai.account_id = acc.account_id AND ai.info_type_id = ");
		maNegBalSql.append("\n        (SELECT type_id FROM account_info_type WHERE type_name = 'Loco')");
		maNegBalSql.append("\n  JOIN user_jm_loco ujl ON ujl.loco_name = ai.info_value");
		maNegBalSql.append("\n WHERE ab.tran_status IN (3,4)");
		maNegBalSql.append("\n   AND ab.current_flag = 1");
		maNegBalSql.append("\n   AND ate.event_date <= '").append(OCalendar.formatDateInt(OCalendar.today())).append("'");
		maNegBalSql.append("\n GROUP BY acc.account_id, ab.external_bunit, p1.short_name, ates.currency_id, ai.info_value");
		maNegBalSql.append("\n HAVING ROUND(SUM(-ate.para_position),6) < " + tolerance + "");
		maNegBalSql.append("\n ORDER BY ab.external_bunit, rn");

		
		Table maNegBal = Table.tableNew();
		currentTime = System.currentTimeMillis();
		Logging.info("Query for negative metal account balance for every account linked to each counterparty: "
				+ maInScopeSql.toString());
		DBaseTable.execISql(maNegBal, maNegBalSql.toString());
		Logging.info("Query for negative metal account balance for every account linked to each counterparty - "
				+ "completed in " + (System.currentTimeMillis() - currentTime) + " ms");

		int numOfRows = argumentTable.getNumRows();
		Table cptyMANegBal = Table.tableNew();
		for (int row = 1; row <= numOfRows; row++) {
			int extBU = argumentTable.getInt("external_bunit", row);
			int tranCurrency = argumentTable.getInt("tran_currency", row);
			String loco = argumentTable.getString("tran_info_type_20015", row);
			int rowNumOfMAInScope = maInScope.findInt("party_id", extBU, SEARCH_ENUM.FIRST_IN_GROUP);
			cptyMANegBal.select(maNegBal, "*", "external_bunit EQ " + extBU + " AND metal NE " + tranCurrency 
					+ " AND loco NE '" + loco + "'");
			cptyMANegBal.sortCol("rn");
			// Check if counterparty has any metal account in scope: if not, set to "N/A - cp" and end check.
			if (rowNumOfMAInScope == Util.NOT_FOUND) {
				argumentTable.setString(ARGT_COL_NAME_ANY_OTHER_BALANCE, row, "N/A - cp");
			} else if (cptyMANegBal.getNumRows() > 0) {
				// Check if any of the counterparty's metal account balances is negative,
				// across all JM group locations and metals, excluding the deal's own location and metal:
				// if yes, set to "[acct identifier] - Negative" (the first negative balance),
				int account = cptyMANegBal.getInt("account_id", 1);
				argumentTable.setString(ARGT_COL_NAME_ANY_OTHER_BALANCE, row, "[" + account + "] - Negative");
			} else {
				// otherwise "All Positive".
				argumentTable.setString(ARGT_COL_NAME_ANY_OTHER_BALANCE, row, "All Positive");
			}
			cptyMANegBal.clearRows();
		}

		if (cptyMANegBal != null && Table.isTableValid(cptyMANegBal) == 1) {
			cptyMANegBal.destroy();
		}
		Logging.info(ARGT_COL_NAME_ANY_OTHER_BALANCE + " updated");
	}
	
	
	
	/**
	 * This method updates the stp_status_JM field based on the below logic
	 * Only mark a record with "STP" if all the followings are fulfilled:
	 * 1) Past Receivables in "Cleared" status
	 * 2) Confirm Status in "3 Confirmed", "4 Completed", "3 Fixed and sent" or "N/A"
	 * 3) Neither metal balance fields are "Negative" (N/A-s are acceptable)
	 * 
	 * @throws OException
	 */
	public void populateStpStatus() throws OException {

		// Only mark a record with "STP" if all the followings are fulfilled:
		// 1) Past Receivables in "Cleared" status
		// 2) Confirm Status in "3 Confirmed", "4 Completed", "3 Fixed and sent" or "N/A"
		// 3) Neither metal balance fields are "Negative" (N/A-s are acceptable)

		String[] confirmStatusCR = _constRepo.getStringValue(CONST_REPO_VAR_CONFIRM_STATUS_FR_STP).split(",");
		int[] acceptableConfirmStatus = new int[confirmStatusCR.length];
		for (int i = 0; i < confirmStatusCR.length; i++) {
			acceptableConfirmStatus[i] = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE,
					confirmStatusCR[i].trim());
		}
		
		int numOfRows = argumentTable.getNumRows();
		for (int row = 1; row <= numOfRows; row++) {
			String pastReceivable = argumentTable.getString("past_receivables", row);
			int confirmStatus = argumentTable.getInt("confirm_status", row);
			String dealMetalBalance = argumentTable.getString("deal_metal_balance", row);
			String anyOtherMetalLocoBalance = argumentTable.getString("any_other_metal_loco_balance", row);
			
			if ("Cleared".equalsIgnoreCase(pastReceivable)
					&& ArrayUtils.contains(acceptableConfirmStatus, confirmStatus)
					&& (!"Negative".equalsIgnoreCase(dealMetalBalance))
					&& (!anyOtherMetalLocoBalance.contains("Negative"))) {
				argumentTable.setString(ARGT_COL_NAME_STP_STATUS, row, "STP");
			} else {
				argumentTable.setString(ARGT_COL_NAME_STP_STATUS, row, "Manual");
			}
		}
	}

	
	/**
	 * This method returns argumentTable
	 * 
	 * @throws OException
	 */
	public Table getArgumentTable() {
		return argumentTable;
	}
	
	
	/**
	 * This method closes table and query id for tran number
	 * 
	 * @throws OException
	 */
	public void close() throws OException {

		if (isQueryScript && Table.isTableValid(argumentTable) == 0) {
			argumentTable.destroy();
		}
		if (isQueryScript && qid > 0) {
			// For Query script clear qid created for tran numbers and not the qid for event numbers
			Query.clear(qid);
		}
	}

}
