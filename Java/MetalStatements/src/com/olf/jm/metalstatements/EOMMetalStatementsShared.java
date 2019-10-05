package com.olf.jm.metalstatements;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.olf.embedded.application.Context;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.EnumAuthorizationStatus;
import com.openlink.util.constrepository.ConstRepository;
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
	public static final String SYMBOLICDATE_1LOM = "-1lom";
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT_STATEMENT_PERIOD = new SimpleDateFormat("MMMyyyy");
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT_STATEMENT_PERIOD_OUTDIR = new SimpleDateFormat("yyyy_MM");
	
	private static final String ACCOUNT_INFO_LOCO = "Loco";

	public static final String USER_JM_MONTHLY_METAL_STATEMENT = "USER_jm_monthly_metal_statement";
	public static final String USER_JM_STATEMENT_DETAILS = "USER_jm_statement_details";

	
	public static final String STATEMENT_STATUS_BLOCKED = "BLOCKED";
	public static final String STATEMENT_STATUS_PROCESSING = "PROCESSING";
	public static final String STATEMENT_STATUS_REPROCESS = "REPROCESS";
	
	public  static final String CONTEXT = "EOM";
	public  static final String SUBCONTEXT = "MetalStatements";

	public  static final String COL_REFERENCE = "reference";	// String
	public  static final String COL_ACCOUNT_ID = "account_id"; 	// Int
	public  static final String COL_NOSTRO_VOSTRO = "nostro_vostro"; // Int
	public  static final String COL_STATEMENT_PERIOD = "statement_period"; 	// String
	public  static final String COL_EXTERNAL_LENTITY = "external_lentity";	// Int
	public  static final String COL_INTERNAL_LENTITY = "internal_lentity";	// String
	public  static final String COL_INTERNAL_BUNIT = "internal_bunit";		// Int
	public  static final String COL_METAL_STATEMENT_PRODUCTION_DATE = "metal_statement_production_date";		// DateTime
	public  static final String COL_OUTPUT_PATH = "output_path";		// String
	public  static final String COL_OUTPUT_FILES = "output_files";		// String
	public  static final String COL_FILES_GENERATED = "files_generated";		// String
	public  static final String COL_LAST_MODIFIED = "last_modified";		// DateTime
	public  static final String COL_RUN_DETAIL = "run_detail";		// String
	
	public static final int METAL_ACCOUNT = 20002;
	public static final int ACCOUNT_VOSTRO =0;

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
	
	/**
	 * This holds a map of vostro metal accounts from account table. Account
	 * name is the key and holder id is the value
	 * @throws OException 
	 */

	public static HashMap<String, Integer> refDataAccountHolder(Context context) throws OException {

		HashMap<String, Integer> staticDataAccount = new HashMap<String, Integer>();
		Table staticDataAccountTable = null;
		try {
			/* OL methods(Ref.get etc.) are not used here because post gui
			 selection, it does not run on main thread and can't resolve OL
			 methods.*/
			PluginLog.info("Preparing a map of all the Vostro accounts in the system with account as the key and holder as the value");
			String sqlString = "Select holder_id, account_name from account\n" + "where account_class = " + METAL_ACCOUNT + "\n" + "and account_Status = "
					+ EnumAuthorizationStatus.Authorized.getValue() + "\n" + "and account_type= " + ACCOUNT_VOSTRO;

			staticDataAccountTable = context.getIOFactory().runSQL(sqlString);
			int rowCount = staticDataAccountTable.getRowCount();
			for (int rowId = 0; rowId < rowCount; rowId++) {
				String accountName = staticDataAccountTable.getString("account_name", rowId);
				int holder_id = staticDataAccountTable.getInt("holder_id", rowId);
				if (holder_id > 0) {
					String holderName = context.getStaticDataFactory().getName(EnumReferenceTable.Party, holder_id);
					boolean isExternal = context.getStaticDataFactory().getReferenceObject(BusinessUnit.class, holderName).isExternal();
					if (!isExternal)
						staticDataAccount.put(accountName, holder_id);
				}
			}
		} catch (Exception e) {
			String errorMessage = "Failed while preparing map of the Vostro account in the system " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);

		} finally {
			if (staticDataAccountTable != null) {
				staticDataAccountTable.dispose();
			}
		}
		return staticDataAccount;

	}
	
	
	/**
	 * This map will remove the records/accounts from the staticDataAccount map, for
	 * which the holder is present in accountList
	 * @throws Exception 
	 */

	public static HashMap<String, Integer> filterRefAccountHolderMap(Table accountList, HashMap<String, Integer> refAccountHolder)
			throws OException {
		String accountName = null;
		try {
			PluginLog.info("Start: Filter the accounts which have atleast one deal with their holder (As per account table");
			int rowCount = accountList.getRowCount();
			for (int rowId = 0; rowId < rowCount; rowId++) {
				accountName = accountList.getString("account_name", rowId);
				int holder_id = accountList.getInt("holder_id", rowId);
				Integer staticHolder = refAccountHolder.get(accountName);
				if (staticHolder != null && staticHolder == holder_id) {
					refAccountHolder.remove(accountName);
					PluginLog.info("Account Removed " + accountName);
				}

			}
			return refAccountHolder;
		} catch (Exception e) {
			String errorMessage = "Failed while filtering accounts which have atleast one deal with their holder (As per account table" + accountName;
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);
		}

	}
	
	/**
	 * This method will add dummy entries in accountList for the accounts which are present in  
	 * refAccountHolder and accountList
	 * @throws OException 
	 * 
	 */
	
	public static Table enrichAccountData(Table accountList, HashMap<String, Integer> staticAccountHolder) throws OException {
		Table tblNewRows = null;
		try {
			PluginLog.info("Start: Enter  entry of accounts where there is no deal with the original holder");
			tblNewRows = accountList.cloneStructure();
			for (int rowId = 0; rowId < accountList.getRowCount(); rowId++) {
				String accountName = accountList.getString("account_name", rowId);
				if (staticAccountHolder.containsKey(accountName)) {
					int holder_id = staticAccountHolder.get(accountName);
					int row = tblNewRows.addRows(1);
					tblNewRows.setInt("party_id", row, accountList.getInt("party_id", rowId));
					tblNewRows.setInt("holder_id", row, holder_id);
					tblNewRows.setInt("account_id", row, accountList.getInt("account_id", rowId));
					tblNewRows.setString("account_number", row, accountList.getString("account_number", rowId));
					tblNewRows.setString("account_name", row, accountList.getString("account_name", rowId));
					tblNewRows.setString("loco", row, accountList.getString("loco", rowId));
					PluginLog.info("Entry added: Account "+accountName+ " and holder "+holder_id);
				}

			}

			accountList.appendRows(tblNewRows);
			PluginLog.info("Succesfully entered  enteries.Total number of enteries added are "+tblNewRows.getRowCount());
			return accountList;
		} catch (Exception e) {
			String errorMessage = "Failed while entering  enteries" + e.getMessage();
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);
		} finally {
			if (tblNewRows != null) {
				tblNewRows.dispose();
			}
		}

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
	
	public static  Table removeAccountsForWrongLocations(Context context, int holder_id, int extBuId, Table accountList, Map<String, Set<String>> allowedLocationsForInternalBu) {
		if (allowedLocationsForInternalBu == null || allowedLocationsForInternalBu.size() == 0) {
			allowedLocationsForInternalBu = getAllowedLocationsForInternalBu(context);
		}
		
		//Map<String, Set<String>> allowedLocationsForInternalBu = getAllowedLocationsForInternalBu(context);
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
	
	public static String formatStatementPeriodOutDir(Date statementDate) {
		return SIMPLE_DATE_FORMAT_STATEMENT_PERIOD_OUTDIR.format(statementDate);
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
	
	public static Map<String, Set<String>> getAllowedLocationsForInternalBu (Session session) {
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
	
	public static void init(ConstRepository constRep, String outDir) throws Exception {
		String logLevel = "INFO";
		String logFile = "EOMMetalStatements.log";
		String logDir = outDir;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
	
	public static String getTimeTakenDisplay(int timeTaken) {
		int modHours = 0;
		int modMinutes = 0;
		int modSeconds = 0;
		
		if (timeTaken > 3600) {
			modMinutes =  timeTaken % 3600;
			modHours = (timeTaken - modMinutes)/3600;
			timeTaken = modMinutes; 
		} 
		
		if (timeTaken > 60) {
			modSeconds =  timeTaken % 60;
			modMinutes = (timeTaken - modSeconds)/60;			
		} else {
			modSeconds = timeTaken ;
		}
		return " - Process Time: " + (modHours>0? (" - Process Time: " + modHours + " Hours "):"") + (modMinutes>0? (" " + modMinutes + " Minutes "):"") + (modSeconds>0? (" " + modSeconds + " Seconds "):"")  ;
	}

}
