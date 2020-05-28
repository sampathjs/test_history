package com.jm.util.accountinfoimporter;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.fnd.RefBase;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2020-03-05		jwaechter		- Initial Version
 */
public class AccountInfoImporterScript implements IScript {
	
	@Override
	public void execute(IContainerContext arg0) throws OException {
		Table logTable = Table.tableNew("Log of account info field updates");
		logTable.addCol("Account Name", COL_TYPE_ENUM.COL_STRING);
		logTable.addCol("Account Info Name", COL_TYPE_ENUM.COL_STRING);
		logTable.addCol("New Value", COL_TYPE_ENUM.COL_STRING);
		logTable.addCol("Status", COL_TYPE_ENUM.COL_STRING);
		updateAccountInfoField ("PMM UK@TANAKA-JPN/ING", "Sort Code", "Test Value", logTable);
		logTable.viewTable();
	}
	
	private void updateAccountInfoField (final String accountName, final String accountInfoName,
			final String newValue, Table logTable) throws OException {
		boolean success = false;
		Table accountTable = null;
		
		
		try {
			int idOfInfoField = getIdOfAccountInfoField (accountInfoName);
			int accountId = retrieveAccountId (accountName);
			accountTable = RefBase.retrieveAccount(accountId);
			// 1 row, column "account_info" contains table with data about the account info fields
			// account_info table consists of two columns info_type_id and info_value (string)
			// with one row per explicitly set account info value.
			
//			accountTable.viewTable();
			Table accountInfoTable = accountTable.getTable("account_info", 1);
			accountInfoTable.sortCol("info_type_id");
			int row = accountInfoTable.findInt("info_type_id", idOfInfoField, SEARCH_ENUM.FIRST_IN_GROUP);
			if (row <= 0) {
				row = accountInfoTable.addRow();
				accountInfoTable.setInt("info_type_id", row, idOfInfoField);
			}
			accountInfoTable.setString("info_value", row, newValue);
			int retval = RefBase.updateAccount(accountTable);
            if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
                String errorMessage = DBUserTable.dbRetrieveErrorInfo(retval, "\nUpdating account info field '" + accountInfoName 
                		+ "' for account '" + accountName + "' failed");
                OConsole.oprint("\n" + errorMessage);
            } else {
            	success = true;
            }
		} finally {
			accountTable = TableUtilities.destroy(accountTable);			
			int logTableRow = logTable.addRow();
			logTable.setString("Account Name", logTableRow, accountName);
			logTable.setString("Account Info Name", logTableRow, accountInfoName);
			logTable.setString("New Value", logTableRow, newValue);
			logTable.setString("Status", logTableRow, success?"Succeeded":"Failed");
		}
		
	}

	private int retrieveAccountId(String accountName) throws OException {
		String sql = "SELECT account_id FROM account WHERE account_name = '" + accountName + "'";
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			int returnCode = DBaseTable.execISql(sqlResult, sql);
			if (sqlResult.getNumRows() > 0) {
				return sqlResult.getInt(1, 1);
			}
			throw new RuntimeException ("Error executing SQL:\n" + sql + "\n" + DBUserTable.dbRetrieveErrorInfo(returnCode, ""));
		} finally {
			sqlResult = TableUtilities.destroy(sqlResult);
		}
	}

	private int getIdOfAccountInfoField(String typeName) throws OException {
		String sql = "SELECT type_id FROM account_info_type WHERE type_name = '" + typeName + "'";
		Table sqlResult = null;
		try {
			sqlResult = Table.tableNew(sql);
			int returnCode = DBaseTable.execISql(sqlResult, sql);
			if (sqlResult.getNumRows() > 0) {
				return sqlResult.getInt(1, 1);
			}
			throw new RuntimeException ("Error executing SQL:\n" + sql + "\n" + DBUserTable.dbRetrieveErrorInfo(returnCode, ""));
		} finally {
			sqlResult = TableUtilities.destroy(sqlResult);
		}		
	}
}
