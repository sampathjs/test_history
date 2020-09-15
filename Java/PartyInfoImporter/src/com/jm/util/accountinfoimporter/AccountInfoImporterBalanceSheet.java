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
public class AccountInfoImporterBalanceSheet implements IScript {
    private Table accountInfoTypeTable = null;
    private Table accountTable = null;

    @Override
    public void execute(IContainerContext arg0) throws OException {
        Table logTable = Table.tableNew("Log of account info field updates");
        logTable.addCol("Account Name", COL_TYPE_ENUM.COL_STRING);
        logTable.addCol("Account Info Name", COL_TYPE_ENUM.COL_STRING);
        logTable.addCol("New Value", COL_TYPE_ENUM.COL_STRING);
        logTable.addCol("Status", COL_TYPE_ENUM.COL_STRING);
        accountTable = null;
        accountInfoTypeTable = null;
        try {
            updateAccountInfoFields(logTable);
        } catch (Exception ex) {
            OConsole.oprint("\n" + ex.toString());
            for (StackTraceElement ste : ex.getStackTrace()) {
                OConsole.oprint("\n" + ste.toString());
            }
            logTable.viewTable();
            accountInfoTypeTable.destroy();
            accountTable.destroy();
            throw ex;
        }
        logTable.viewTable();
        accountInfoTypeTable.destroy();
        accountTable.destroy();
    }

    public void updateAccountInfoFields(Table logTable) throws OException {
    	
        updateAccountInfoField("JM CA POLAND@PMM UK-ROY", "UK Balance Sheet Line", "102", logTable);
        updateAccountInfoField("MISC SHIPMENTS@PMM UK-ROY", "UK Balance Sheet Line", "140.01", logTable);
        updateAccountInfoField("PMM UK SAFE@PMM UK-ROY", "UK Balance Sheet Line", "140.01", logTable);
        updateAccountInfoField("PMM UK SAFE@PMM UK-ROY/GRA", "UK Balance Sheet Line", "140.02", logTable);
        updateAccountInfoField("PMM UK SAFE@PMM UK-ROY/ING", "UK Balance Sheet Line", "140.03", logTable);
        
        
        updateAccountInfoField("MISC RECEIPTS@PMM US-VF", "US Balance Sheet Line", "145.03", logTable);
        updateAccountInfoField("PMM US SAFE WD@PMM US-VF", "US Balance Sheet Line", "145.06", logTable);
        updateAccountInfoField("PMM US SAFE WD@PMM US-VF/GRA", "US Balance Sheet Line", "145.07", logTable);
        updateAccountInfoField("PMM US SAFE WD@PMM US-VF/ING", "US Balance Sheet Line", "145.08", logTable);
        updateAccountInfoField("PMM US SAFE WW@PMM US-VF", "US Balance Sheet Line", "145.03", logTable);
        
        updateAccountInfoField("PMM US SAFE WW@PMM US-VF/GRA", "US Balance Sheet Line", "145.04", logTable);
        updateAccountInfoField("PMM US SAFE WW@PMM US-VF/ING", "US Balance Sheet Line", "145.05", logTable);
        updateAccountInfoField("WD-WW TRANSIT@PMM US-VF", "US Balance Sheet Line", "145.06", logTable);
        updateAccountInfoField("WD-WW TRANSIT@PMM US-VF/GRA", "US Balance Sheet Line", "145.07", logTable);
        updateAccountInfoField("WD-WW TRANSIT@PMM US-VF/ING", "US Balance Sheet Line", "145.08", logTable);
        
        updateAccountInfoField("WW-WD TRANSIT@PMM US-VF", "US Balance Sheet Line", "145.03", logTable);
        updateAccountInfoField("WW-WD TRANSIT@PMM US-VF/GRA", "US Balance Sheet Line", "145.04", logTable);
        updateAccountInfoField("WW-WD TRANSIT@PMM US-VF/ING", "US Balance Sheet Line", "145.05", logTable);
        updateAccountInfoField("MISC RECEIPTS@PMM US-VF/ING", "US Balance Sheet Line", "145.05", logTable);
        
 
    }

    private void updateAccountInfoField(final String accountName,
                                        final String accountInfoName,
                                        final String newValue,
                                        Table logTable) throws OException {
        boolean success = false;
        Table accountTable = null;


        try {
            int idOfInfoField = getIdOfAccountInfoField(accountInfoName);
            int accountId = retrieveAccountId(accountName);
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
                String errorMessage = DBUserTable.dbRetrieveErrorInfo(retval,  "\nUpdating account info field '" + accountInfoName + "' for account '" + accountName + "' failed");
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
            logTable.setString("Status", logTableRow, success ? "Succeeded" : "Failed");
        }

    }

    private int retrieveAccountId(String accountName) throws OException {
        if (accountTable == null) {
            accountTable = Table.tableNew("account");
            DBUserTable.structure(accountTable);
            int returnCode = DBaseTable.loadFromDb(accountTable, "account");
        }
        accountTable.sortCol("account_name");
        int row = accountTable.findString("account_name", accountName, SEARCH_ENUM.FIRST_IN_GROUP);
        if (row > 0) {
            return accountTable.getInt("account_id", row);
        } else {
            throw new RuntimeException("Could not find ID for account '" + accountName + "'");
        }
    }

    private int getIdOfAccountInfoField(String typeName) throws OException {
        if (accountInfoTypeTable == null) {
            accountInfoTypeTable = Table.tableNew("account_info_type");
            DBUserTable.structure(accountInfoTypeTable);
            int returnCode = DBaseTable.loadFromDb(accountInfoTypeTable, "account_info_type");
        }
        accountInfoTypeTable.sortCol("type_name");
        int row = accountInfoTypeTable.findString("type_name", typeName, SEARCH_ENUM.FIRST_IN_GROUP);
        if (row > 0) {
            return accountInfoTypeTable.getInt("type_id", row);
        } else {
            throw new RuntimeException("Could not find ID for account info type '" + typeName + "'");
        }
    }
}
