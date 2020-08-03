package com.olf.jm.autosipopulation.persistence;

import java.util.ArrayList;
import java.util.List;

import com.olf.jm.autosipopulation.model.AccountInfoField;
import com.olf.jm.autosipopulation.model.TranInfoField;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2015-05-01	V1.0	jwaechter 	- initial version
 * 2015-08-25	V1.1	jwaechter	- added retrieval of precious metals from currencies
 * 2015-09-15	V1.2	jwaechter	- removed SI info "For Passthrough"
 * 2016-02-03	V1.3	jwaechter	- added methods isDispatchDeal, didUseShortListChange 
 *                                    and retrieveDispatchStatus
 * 2016-05-10	V1.4	jwaechter	- added new methods getSiId and retrieveTransOfTranGroup
 * 2016-06-07	V1.5	jwaechter	- modified method getSiId to work on external bunit if requested
 */
/**
 * Class containg static methods to retrieve data from database
 * @author jwaechter
 * @version 1.5
 */
public class DBHelper {
	
	private static final String VAT_INFO_FILTER = "Yes";
	private static final String USER_TABLE_UNHEDGED_ACCOUNT = "USER_jm_unhedged_account_si";
	private static final String HEDGED_IN_ENDUR_FILTER = "No";

	
	/**
	 * Retrieves table containing the db stored date of all settlement instructions
	 * for a certain transaction.
	 * @param tranNum
	 * @param si
	 * @return Table with the following columns: <br/> <br/>
	 * tran_num(int), settle_instructions(int), int_ext(int), currency_id(int), delivery_type(int) 
	 */
	public static Table retrieveSettleDataTransaction (final int tranNum, Session si) {
		String sql = "\nSELECT absti.tran_num, absti.settle_instructions, "
				+    "\n  absti.int_ext, absti.currency_id, absti.delivery_type"
				+    "\nFROM ab_tran_sttl_inst absti"
				+    "\nWHERE absti.tran_num = " + tranNum;

		Table sqlResult = null;
		Logging.info("Executing SQL(retrieveSettleDataTransaction) query:" + sql);
		sqlResult = si.getIOFactory().runSQL(sql);
		return sqlResult;
	}
	
	public static Table retrieveAccountData (Session si, int insType) {
		
		String sql = "\nSELECT a.account_id"
				+   ", a.account_type" 
				+   ", ISNULL(loco.info_value, '" + AccountInfoField.Loco.getDefault(si.getIOFactory()) + "') AS loco"
				+   ", ISNULL(form.info_value, '" + AccountInfoField.Form.getDefault(si.getIOFactory()) + "') AS form"
				+   ", ISNULL(aloc_type.info_value, '" + AccountInfoField.AllocationType.getDefault(si.getIOFactory()) + "') AS aloc_type"
				+   ", ISNULL(use_shortlist.info_value, '" + AccountInfoField.AUTO_SI_SHORTLIST.getDefault(si.getIOFactory()) + "') AS use_shortlist"
				+   "\n  , si.settle_id"
				+   ", si.settle_name"
				+   ", si.party_id, sins.ins_type "
				+	"\nFROM account a"
				+   "\nLEFT OUTER JOIN account_info loco"
				+   "\n  ON loco.account_id = a.account_id "
				+   "\n   AND loco.info_type_id = (SELECT ait_loco.type_id  FROM account_info_type ait_loco WHERE ait_loco.type_name = '" + AccountInfoField.Loco.getName() + " ')"
				+   "\nLEFT OUTER JOIN account_info form"
				+   "\n  ON form.account_id = a.account_id "
				+   "\n  AND form.info_type_id = (SELECT ait_form.type_id FROM account_info_type ait_form WHERE ait_form.type_name = '" + AccountInfoField.Form.getName() + "')"
				+   "\nLEFT OUTER JOIN account_info aloc_type"
				+   "\n  ON aloc_type.account_id = a.account_id "
				+   "\n  AND aloc_type.info_type_id = (SELECT ait_aloc_type.type_id FROM account_info_type ait_aloc_type WHERE ait_aloc_type.type_name = '" + AccountInfoField.AllocationType.getName() + "')"
				+   "\nLEFT OUTER JOIN account_info use_shortlist"
				+   "\n  ON use_shortlist.account_id = a.account_id "
				+   "\n  AND use_shortlist.info_type_id = (SELECT ait_use_shortlist.type_id FROM account_info_type ait_use_shortlist WHERE ait_use_shortlist.type_name = '" + AccountInfoField.AUTO_SI_SHORTLIST.getName() + "')"
				+   "\nINNER JOIN settle_instructions si"
				+   "\n  ON si.account_id = a.account_id AND si.settle_status = 1 "
				+ 	"\n INNER JOIN stl_ins sins ON sins.settle_id = si.settle_id "
				+   "\n WHERE a.account_status = 1 AND sins.ins_type = " + insType;

		Table sqlResult = null;
		Logging.info("Executing SQL(retrieveAccountData) query: " + sql);
		sqlResult = si.getIOFactory().runSQL(sql);
		return sqlResult;	
	}
	
	public static Table retrieveAccountData (Session si, String partyIds, int insType) {
		
		String sql = "\nSELECT a.account_id"
				+   ", a.account_type" 
				+   ", ISNULL(loco.info_value, '" + AccountInfoField.Loco.getDefault(si.getIOFactory()) + "') AS loco"
				+   ", ISNULL(form.info_value, '" + AccountInfoField.Form.getDefault(si.getIOFactory()) + "') AS form"
				+   ", ISNULL(aloc_type.info_value, '" + AccountInfoField.AllocationType.getDefault(si.getIOFactory()) + "') AS aloc_type"
				+   ", ISNULL(use_shortlist.info_value, '" + AccountInfoField.AUTO_SI_SHORTLIST.getDefault(si.getIOFactory()) + "') AS use_shortlist"
				+   "\n  , si.settle_id"
				+   ", si.settle_name"
				+   ", si.party_id, sins.ins_type "
				+	"\nFROM account a"
				+   "\nLEFT OUTER JOIN account_info loco"
				+   "\n  ON loco.account_id = a.account_id "
				+   "\n   AND loco.info_type_id = (SELECT ait_loco.type_id  FROM account_info_type ait_loco WHERE ait_loco.type_name = '" + AccountInfoField.Loco.getName() + " ')"
				+   "\nLEFT OUTER JOIN account_info form"
				+   "\n  ON form.account_id = a.account_id "
				+   "\n  AND form.info_type_id = (SELECT ait_form.type_id FROM account_info_type ait_form WHERE ait_form.type_name = '" + AccountInfoField.Form.getName() + "')"
				+   "\nLEFT OUTER JOIN account_info aloc_type"
				+   "\n  ON aloc_type.account_id = a.account_id "
				+   "\n  AND aloc_type.info_type_id = (SELECT ait_aloc_type.type_id FROM account_info_type ait_aloc_type WHERE ait_aloc_type.type_name = '" + AccountInfoField.AllocationType.getName() + "')"
				+   "\nLEFT OUTER JOIN account_info use_shortlist"
				+   "\n  ON use_shortlist.account_id = a.account_id "
				+   "\n  AND use_shortlist.info_type_id = (SELECT ait_use_shortlist.type_id FROM account_info_type ait_use_shortlist WHERE ait_use_shortlist.type_name = '" + AccountInfoField.AUTO_SI_SHORTLIST.getName() + "')"
				+   "\nINNER JOIN settle_instructions si"
				+   "\n  ON si.account_id = a.account_id AND si.settle_status = 1 "
				+ 	"\n INNER JOIN stl_ins sins ON sins.settle_id = si.settle_id "
				+   "\n WHERE a.account_status = 1 AND si.party_id IN (" + partyIds + ") AND sins.ins_type = " + insType;

		Table sqlResult = null;
		Logging.info("Executing SQL(retrieveAccountData) query: " + sql);
		sqlResult = si.getIOFactory().runSQL(sql);
		return sqlResult;	
	}
	
	public static Table retrieveStlDeliveryTable(Session si) {
		String sql = "\nSELECT sd.settle_id, sd.currency_id, sd.delivery_type"
				+	"\nFROM settlement_delivery sd ";
		
		Table sqlResult = null;
		Logging.info("Executing SQL(retrieveStlDeliveryTable) query: " + sql);
		sqlResult = si.getIOFactory().runSQL(sql);
		return sqlResult;
	}
	
	public static Table retrieveStlDeliveryTable(Session si, String settleIds) {
		String sql = "\nSELECT sd.settle_id, sd.currency_id, sd.delivery_type"
				+	"\nFROM settlement_delivery sd WHERE sd.settle_id IN (" + settleIds + ")";
		
		Table sqlResult = null;
		Logging.info("Executing SQL(retrieveStlDeliveryTable) query: " + sql);
		sqlResult = si.getIOFactory().runSQL(sql);
		return sqlResult;
	}
	
	/**
	 * Returns the data about which settle instruction is linked to which instrument
	 * @param si
	 * @return Table with the following columns: <br/> <br/>
	 * settle_id(int), ins_type(int)
	 */
	public static Table retrieveStlInsTable(Session si) {
		String sql = "\nSELECT stl_ins.settle_id"
				+ 	 "\n,  stl_ins.ins_type "
//				+ 	 "\n,  ISNULL(si.info_value, sit.default_value) AS for_passthrough"
				+	 "\nFROM stl_ins"
//				+    "\nJOIN settle_instruction_info_type sit"
//				+    "\n   ON sit.type_name = '" + StlInsInfo.ForPassthrough.getName() + "'"
//				+    "\nLEFT OUTER JOIN settle_instruction_info si"
//				+    "\n   ON si.settle_id = stl_ins.settle_id"
//				+    "\n   AND si.info_type_id = sit.type_id"

				;
		Table sqlResult = null;
		Logging.info("Executing SQL(retrieveStlInsTable) query:" + sql);
		sqlResult = si.getIOFactory().runSQL(sql);
		return sqlResult;
	}
	
	/**
	 * Retrieves list of currencies (ids) that are precious metals.
	 * @param session
	 * @return
	 */
	public static List<Integer> retrievePreciousMetalList(Session session) {
		String sql = "SELECT id_number FROM currency WHERE precious_metal = 1";
		Table sqlResult;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);	
		} catch (RuntimeException ex) {
			Logging.error("Error Executing SQL " + sql + " : " + ex);
			throw ex;
		}
		List<Integer> preciousMetals = new ArrayList<> ();
		for (TableRow row : sqlResult.getRows()) {
			int precMetalId = row.getInt("id_number");
			preciousMetals.add(precMetalId);
		}
		return preciousMetals;
	}
		
	/**
	 * Retrieves the value of the tran info field "Dispatch Status" for a given deal #
	 */
	public static String retrieveDispatchStatus(Session session, int dealTrackingNum) {
		String sql = 
				"\nSELECT ISNULL(abi.value, (SELECT tit2.default_value FROM tran_info_types tit2 WHERE tit2.type_name = '" 
			+  TranInfoField.DISPATCH_STATUS.getName() + "')  )"
			+	"\nFROM ab_tran ab "
			+   "\nLEFT OUTER JOIN ab_tran_info abi"
			+   "\n  ON abi.tran_num = ab.tran_num"
			+   "\nAND abi.type_id = (SELECT tit.type_id FROM tran_info_types tit WHERE tit.type_name = '" 
			+  TranInfoField.DISPATCH_STATUS.getName() + "')"
			+   "\nWHERE ab.deal_tracking_num = " + dealTrackingNum
			+   "\n  AND ab.current_flag = 1"
			;
		Table sqlResult = null;
		try {
			Logging.info("Executing SQL(retrieveDispatchStatus) query:" + sql);
			sqlResult = session.getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() == 1) {
				return sqlResult.getString(0, 0);
			}
			throw new RuntimeException ("Could not retrieve value of tran info field " 
					+ TranInfoField.DISPATCH_STATUS.getName() + " for deal #"
					+ dealTrackingNum); 
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
		
	}

	/**
     * Given a certain tran num this methods checks if the value of transaction info field 
	 * "Use Auto SI Shortlist" is different compared to the provided in memory value.
	 */
	public static boolean didUseShortListChange(Session session, int tranNum,
			String useAutoSiShortList) {
		String sql = 
				"\nSELECT ISNULL(abi.value, (SELECT tit2.default_value FROM tran_info_types tit2 WHERE tit2.type_name = '" 
			+  TranInfoField.USE_AUTO_SI_SHORTLIST.getName() + "')  )"
			+	"\nFROM ab_tran ab "
			+   "\nLEFT OUTER JOIN ab_tran_info abi"
			+   "\n  ON abi.tran_num = ab.tran_num"
			+   "\nAND abi.type_id = (SELECT tit.type_id FROM tran_info_types tit WHERE tit.type_name = '" 
			+  TranInfoField.USE_AUTO_SI_SHORTLIST.getName() + "')"
			+   "\nWHERE ab.tran_num = " + tranNum
			;
		Table sqlResult = null;
		try {
			Logging.info("Executing SQL(didUseShortListChange) query:" + sql);
			sqlResult = session.getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() == 1) {
				String oldValue = sqlResult.getString(0, 0);
				if (oldValue.equals(useAutoSiShortList)) {
					return false;
				} else {
					return true;
				}
			}
			return true;
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}		
	}

	/**
	 * Checks if a provided deal number belongs to a dispatch deal
	 * that is a deal that is referenced as dispatch in comm_sched_deliv_deal
	 */
	public static boolean isDispatchDeal(Session session, int dealTrackingNum) {
		String sql = 
				"\nSELECT csdd.delivery_id" 
			+	"\nFROM ab_tran ab "
			+   "\nINNER JOIN comm_sched_deliv_deal csdd"
			+   "\n  ON csdd.deal_num = ab.deal_tracking_num"
			+   "\n  AND csdd.receipt_delivery = 1"
			+   "\nWHERE ab.deal_tracking_num = " + dealTrackingNum
			+   "\n  AND ab.current_flag = 1"
			;
		Table sqlResult = null;
		try {
			Logging.info("Executing SQL(isDispatchDeal) query:" + sql);
			sqlResult = session.getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() == 1) {
				return true;
			}
			return false;
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
	}
	
	/**
	 * This method retrieves the ID of a settlement instruction such that the following conditions hold:
	 * <ol>
  	 *   <li> the party of the account associacted to the SI is either the internal or the external party of the provided transaction </li> 
	 *   <li> the settle currency matched settleCcy </li>
	 *   <li> the value of the account info field "VAT and Cash" of the account associated to the SI  is matching {@value #VAT_INFO_FILTER}</li>
	 *   <li> 
	 *      the instrument type of the provided transaction and the event type of the provided event match a row 
	 *      in {@value #USER_TABLE_UNHEDGED_ACCOUNT} that has the column value "hedged_in_endur" set to 
	 *      {@value #HEDGED_IN_ENDUR_FILTER}
	 *   </li>
	 * </ol>
	 */
	public static int getSiId(Session session, String settleCcy, long eventId, int tranNum, boolean isInt) {
		String unitType = isInt?"internal_bunit":"external_bunit";
		String sql = 
				"\nSELECT DISTINCT si.settle_id "
			+	"\nFROM si_view si"
			+	"\nINNER JOIN currency c"
			+	"\n  ON c.id_number = si.currency_id"
			+	"\n    AND c.name = '" + settleCcy + "'"
			+	"\nINNER JOIN party p"
			+	"\n  ON p.party_id = (SELECT ab2." + unitType + " FROM ab_tran ab2 WHERE ab2.tran_num = " + tranNum  + ")"
			+	"\n    AND si.party_id = p.party_id"
			+	"\n    AND p.int_ext = 0" // 0 = internal
			+   "\nINNER JOIN account a"
			+	"\n  ON a.account_id = si.account_id"
			+ 	"\nINNER JOIN account_info_type vit"
			+	"\n	 ON vit.type_name = '" + AccountInfoField.VAT_AND_CASH.getName() + "'"
			+	"\nINNER JOIN " + USER_TABLE_UNHEDGED_ACCOUNT + " uua"
			+	"\n  ON uua.ins_type_id = (SELECT ab.ins_type FROM ab_tran ab WHERE ab.tran_num = " + tranNum + ")" 
			+	"\n    AND uua.event_type_id = (SELECT e.event_type FROM ab_tran_event e WHERE e.event_num = " + eventId + ")" 
			+ 	"\n	   AND uua.hedged_in_endur='" + HEDGED_IN_ENDUR_FILTER + "'"
			+ 	"\nLEFT OUTER JOIN account_info ai"
			+	"\n  ON ai.account_id = a.account_id"
			+	"\n  	AND ai.info_type_id = vit.type_id"
			+   "\nWHERE ISNULL(ai.info_value, vit.default_value) = '" + VAT_INFO_FILTER + "'";
		
		Logging.info("Executing SQL(getSiId) query:" + sql);
		try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
			if (sqlResult.getRowCount() == 0) {
				return -1;
			}
			if (sqlResult.getRowCount() > 1) {
				StringBuilder sis = new StringBuilder();
				boolean first = true;
				for (TableRow row : sqlResult.getRows()) {
					if (!first) {
						sis.append(", ");
					}
					sis.append(row.getInt("settle_id"));
					first = false;
				}
				String message = "Retrieved more than one possible settlement instructions (" + sis + ") for"
						+ " currency '" + settleCcy + "' for event " + eventId + " of transaction #" + tranNum;
				Logging.error(message);
				throw new RuntimeException (message);
			}
			return sqlResult.getInt("settle_id", 0);
		}
	}

	/**
	 * Retrives a list of all transaction that belong to the same tran group as the provided deal.
	 */
	public static List<Transaction> retrieveTransOfTranGroup(Session session,
			int dealTrackingId) {
		ArrayList<Transaction> transactions = new ArrayList<>();
		String sql = 
				"\nSELECT ab_group.tran_num "
			+	"\nFROM ab_tran ab"
			+	"\n  INNER JOIN ab_tran ab_group ON ab_group.tran_group = ab.tran_group"
			+	"\nWHERE ab.deal_tracking_num = " + dealTrackingId
			+	"\n  AND ab.current_flag = 1";
		
		Logging.info("Executing SQL(retrieveTransOfTranGroup) query:" + sql);
		try (Table transactionsOfGroup = session.getIOFactory().runSQL(sql)) {
			for (int row = transactionsOfGroup.getRowCount()-1; row >= 0; row--) {
				int tranId = transactionsOfGroup.getInt("tran_num", row);
				Transaction tran = session.getTradingFactory().retrieveTransactionById(tranId);
				transactions.add(tran);
			}
		}		
		return transactions;
	}


	/**
	 * To prevent instantiation
	 */
	private DBHelper () {
	}
	
	/*
	*//**
	 * Retrieves table containing joined information about accounts and tables.
	 * @param si
	 * @return Table with the following columns: <br/> <br/>
	 * account_id(int), account_type(int), loco (String), form(String), aloc_type(String),
	 * settle_id(int), settle_name(String), party_id(int)
	 *//*
	public static Table retrieveAccountData (Session si) {
		String sql = "\nSELECT a.account_id"
				+   ", a.account_type" 
				+   ", ISNULL(loco.info_value, '" + AccountInfoField.Loco.getDefault(si.getIOFactory()) + "') AS loco"
				+   ", ISNULL(form.info_value, '" + AccountInfoField.Form.getDefault(si.getIOFactory()) + "') AS form"
				+   ", ISNULL(aloc_type.info_value, '" + AccountInfoField.AllocationType.getDefault(si.getIOFactory()) + "') AS aloc_type"
				+   ", ISNULL(use_shortlist.info_value, '" + AccountInfoField.AUTO_SI_SHORTLIST.getDefault(si.getIOFactory()) + "') AS use_shortlist"
				+   "\n  , si.settle_id"
				+   ", si.settle_name"
				+   ", si.party_id"
				+	"\nFROM account a"
				+   "\nLEFT OUTER JOIN account_info loco"
				+   "\n  ON loco.account_id = a.account_id "
				+   "\n   AND loco.info_type_id = (SELECT ait_loco.type_id  FROM account_info_type ait_loco WHERE ait_loco.type_name = '" + AccountInfoField.Loco.getName() + " ')"
				+   "\nLEFT OUTER JOIN account_info form"
				+   "\n  ON form.account_id = a.account_id "
				+   "\n  AND form.info_type_id = (SELECT ait_form.type_id FROM account_info_type ait_form WHERE ait_form.type_name = '" + AccountInfoField.Form.getName() + "')"
				+   "\nLEFT OUTER JOIN account_info aloc_type"
				+   "\n  ON aloc_type.account_id = a.account_id "
				+   "\n  AND aloc_type.info_type_id = (SELECT ait_aloc_type.type_id FROM account_info_type ait_aloc_type WHERE ait_aloc_type.type_name = '" + AccountInfoField.AllocationType.getName() + "')"
				+   "\nLEFT OUTER JOIN account_info use_shortlist"
				+   "\n  ON use_shortlist.account_id = a.account_id "
				+   "\n  AND use_shortlist.info_type_id = (SELECT ait_use_shortlist.type_id FROM account_info_type ait_use_shortlist WHERE ait_use_shortlist.type_name = '" + AccountInfoField.AUTO_SI_SHORTLIST.getName() + "')"
				+   "\nINNER JOIN settle_instructions si"
				+   "\n  ON si.account_id = a.account_id AND si.settle_status = 1 "
				+   "\n WHERE a.account_status = 1";

		Table sqlResult = null;
		Logging.info("Executing SQL(retrieveAccountData) query:" + sql);
		sqlResult = si.getIOFactory().runSQL(sql);
		return sqlResult;	
	}
	
	*//**
	 * Returns the data about which settle instruction is linked to which currency/delivery type
	 * @param si
	 * @return Table with the following columns: <br/> <br/>
	 * settle_id(int), currency_id(int), delivery_type(int)
	 *//*
	public static Table retrieveStlDeliveryTable(Session si) {
		String sql = "\nSELECT sd.settle_id, sd.currency_id, sd.delivery_type"
				+	"\nFROM settlement_delivery sd";
		
		Table sqlResult = null;
		Logging.info("Executing SQL(retrieveStlDeliveryTable) query:" + sql);
		sqlResult = si.getIOFactory().runSQL(sql);
		return sqlResult;
	}*/
}