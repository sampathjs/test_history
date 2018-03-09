package com.olf.jm.metalstatements;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.olf.embedded.application.Context;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

/*
 * History: 
 * 2016-07-13	V1.0	jwaechter	- Initial Version created by moving code
 *                                    from EOMMetalStatements to this place.
 * 2016-11-09	V1.1	jwaechter	- Added hasDefaultAuthorizedLegalEntity   
 * 2016-11-10	V1.2	jwaechter	- Bugfix in hasDefaultAuthorizedLegalEntity  
 * 2016-11-23   V1.3    jwaechter	- Added check: default LE has to be  authorised
 * 2017-01-10	V1.4	jwaechter	- Changed join to use tran# instead of deal#   
 *                                    in method getUsedAccounts
 * 2017-02-07	V1.5	jwaechter	- Overloaded method to check if default LE is
 *                                    authorized (V1.3)
 */

/**
 * Helper class containing methods shared among {@link EOMMetalStatements} 
 * and {@link EOMMetalStatementsParam}
 * @author jwaechter
 * @version 1.5
 */
public class EOMMetalStatementsShared {
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT_STATEMENT_PERIOD = new SimpleDateFormat("MMMyyyy");
	private static final String ACCOUNT_INFO_LOCO = "Loco";

	public static Table getUsedAccounts(Context context) {
		String sqlString = "SELECT DISTINCT abt.external_bunit AS party_id, atsv.internal_bunit AS holder_id, a.account_id, \n"
        				 + "                a.account_number, a.account_name, ISNULL(ai_loco.info_value, ait.default_value) AS loco\n"
        				 + "FROM ab_tran_settle_view atsv \n"
        				 + "INNER JOIN ab_tran abt ON abt.tran_num = atsv.tran_num AND abt.current_flag = 1 \n"
        				 + "INNER JOIN account a ON atsv.ext_account_id = a.account_id AND a.account_type = 0 AND a.account_status = 1 \n"
        				 + "INNER JOIN account_info_type ait ON ait.type_name = '" + ACCOUNT_INFO_LOCO + "'\n"
        				 + "LEFT OUTER JOIN account_info ai_loco ON ai_loco.info_type_id = ait.type_id\n"
        				 + "  AND ai_loco.account_id = a.account_id "
           				 + "WHERE atsv.account_class = 20002  \n";
		Table accountList = context.getIOFactory().runSQL(sqlString);
		return accountList;
	}
	
	public static Table getAccountsForHolder(Table accountList, int holderId) {
		ConstTable view = accountList.createConstView("*", "[holder_id] == " + holderId);
		Table filteredAccountList = view.asTable();
		view.dispose();
		return filteredAccountList;
	}
	
	public static boolean hasDefaultAuthorizedLegalEntity(Session session, BusinessUnit bu) {
		return hasDefaultAuthorizedLegalEntity (session, bu.getId());
	}	

	public static boolean hasDefaultAuthorizedLegalEntity(Session session, int buId) {
		String sql = "\nSELECT pr.business_unit_id" 
				+    "\nFROM party_relationship pr"
				+	 "\nINNER JOIN party p"
				+	 "\n  ON p.party_id = pr.legal_entity_id"
				+	 "\n  AND p.party_status = 1" // authorized
				+    "\nWHERE pr.business_unit_id = " + buId
				+    "\n  AND pr.def_legal_flag = 1" // 1 == yes
				;
		try (Table sqlResult = session.getIOFactory().runSQL(sql)) {
			if (sqlResult.getRowCount() != 0) {
				return true;
			} else {
				return false;
			}					
		}
	}	


	
	public static  Table removeAccountsForWrongLocations(Context context,
			int holder_id, int extBuId, Table accountList) {
		Map<String, Set<String>> allowedLocationsForInternalBu = getAllowedLocationsForInternalBu(context);
		String rowExpression = "[holder_id] == " + holder_id + " AND [party_id] == " + extBuId;
		Table accounts = context.getTableFactory().createTable("Accounts matching location and party");
		accounts.select(accountList, "*", rowExpression);
		for (int row=accounts.getRowCount()-1; row >= 0; row--) {
			int holderId = accounts.getInt("holder_id", row);
			String holder = context.getStaticDataFactory().getName(EnumReferenceTable.Party, holderId);
			String loco = accounts.getString("loco", row);
			if (!allowedLocationsForInternalBu.containsKey(holder)) {
				String message = "Party '" + holder + "' not found in USER_jm_loco table";
				PluginLog.error(message);
				accounts.removeRow(row);
				continue;
			}
			if (!allowedLocationsForInternalBu.get(holder).contains(loco)) {
				accounts.removeRow(row);
			}
		}
		return accounts;
	}
	
	public static String formatStatementPeriod(Date statementDate) {
		return SIMPLE_DATE_FORMAT_STATEMENT_PERIOD.format(statementDate);
	}
	
	public static boolean doesMetalStatementRowExist(Context context,
			int intBUId, int extBUId, int accountId) {
		IOFactory iof = context.getIOFactory();
		StaticDataFactory sdf = context.getStaticDataFactory();		
		BusinessUnit intBU = (BusinessUnit)sdf.getReferenceObject(BusinessUnit.class, intBUId);
		BusinessUnit extBU = (BusinessUnit)sdf.getReferenceObject(BusinessUnit.class, extBUId);

		Date statementDate = context.getCalendarFactory().createSymbolicDate("-1lom").evaluate();
		String dateFormatted = EOMMetalStatementsShared.formatStatementPeriod(statementDate);
		
		String sql = 
				"\nSELECT * FROM " + EOMMetalStatements.USER_JM_MONTHLY_METAL_STATEMENT + " u"
			+	"\nWHERE u.reference = 'BLOCKED'"
			+	"\n  AND u.account_id = " + accountId
			+	"\n  AND u.external_lentity = " + extBU.getDefaultLegalEntity().getId()
			+	"\n  AND u.internal_lentity = '" + intBU.getDefaultLegalEntity().getName() + "'"
			+   "\n  AND u.internal_bunit = " + intBUId
			+	"\n  AND u.statement_period = '" + dateFormatted + "'"
				;
		try (Table sqlResult = iof.runSQL(sql);) {
			if (sqlResult.getRowCount() == 0) {
				return false;
			} else {
				return true;
			}
		}
	}
	
	private static Map<String, Set<String>> getAllowedLocationsForInternalBu (Session session) {
		Map<String, Set<String>> allowedLocationsForInternalBu = new HashMap<>();
		try (UserTable jmLocoUserTable = session.getIOFactory().getUserTable("USER_jm_loco");
			 Table jmLoco = jmLocoUserTable.retrieveTable();)
			{
			for (int row=jmLoco.getRowCount()-1; row >= 0; row--) {
				String loco  = jmLoco.getString("loco_name", row);
				String intBu = jmLoco.getString("int_BU", row);
				boolean isPmmId = jmLoco.getInt("is_pmm_id", row) == 1;
				if (isPmmId ) {
					if (!allowedLocationsForInternalBu.containsKey(intBu)) {
						allowedLocationsForInternalBu.put(intBu, new HashSet<String>());
					}
					Set<String> allowedLocos = allowedLocationsForInternalBu.get(intBu);
					allowedLocos.add(loco);
				} else if (!isPmmId) {
					if (!allowedLocationsForInternalBu.containsKey("JM PMM UK")) {
						allowedLocationsForInternalBu.put("JM PMM UK", new HashSet<String>());
					}
					Set<String> allowedLocos = allowedLocationsForInternalBu.get("JM PMM UK");
					allowedLocos.add(loco);
				}
			}
		}
		return allowedLocationsForInternalBu;
	}
	
	
	/**
	 * To prevent initialisation
	 */
	private EOMMetalStatementsShared () {
		
	}
}
