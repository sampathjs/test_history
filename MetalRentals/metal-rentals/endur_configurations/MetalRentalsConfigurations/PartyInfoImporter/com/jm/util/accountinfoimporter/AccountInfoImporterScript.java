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
        updateAccountInfoField("PMM US@PMM HK-HK/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM US@PMM HK-HK/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM US@PMM HK-HK", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM US@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM US@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM US@PMM UK-ROY/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM US@CS-ZUR/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM US@HSBC-LDN/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM US@VALE-ACT", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM US@HSBC-ZUR/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("INV BORROWING - UK@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("INV BORROWING - UK@PMM US-VF/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM UK@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM UK@PMM US-VF/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM UK@PMM US-VF/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM UK@PMM HK-HK", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM UK@PMM HK-HK/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM UK@PMM HK-HK/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM HK@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM HK@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM HK@PMM UK-ROY/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM HK@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM HK@PMM US-VF/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM HK@PMM US-VF/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM HK@HSBC-ZUR/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM HK@HSBC-LDN/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("INV BORROWING-UK@PMM HK-HK", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("INV BORROWING-UK@PMM HK-HK/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM SYNGAS@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM REFINING UK@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM REFINING UK@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM REFINING UK@PMM UK-ROY/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM METAL JOINING@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM METAL JOINING@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM AGT UK@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM SA ECT@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM AGT HOLLAND@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM AGT HOLLAND@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM NM AUSTRALIA@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM NM AUSTRALIA@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM MACEDONIA@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM CATALYSTS KOREA@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM BRANDENBERGER@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM BRANDENBERGER LEA@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM BRANDENBERGER@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM ECT@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM ECT (FIAT)@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM INORGANICS SDN BHD@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM JAPAN CHEMICALS GK@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM JAPAN CHEM GK@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM JAPAN G.K@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM JAPAN R&D@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM DUBLIN (MJ)@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM DUBLIN (MJ)@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM PACIFIC (PCT)@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM PCT INDIA@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM NOBLE METALS@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM NOBLE METALS@PMM UK-ROY/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM NOBLE METALS@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM FC - SWINDON@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM RESEARCH CHEM UK@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM RESEARCH CHEM@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM ECT (RUSSIA)@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM CHEM PRODUCTS@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM REF PRODUCTS@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM CHEM PRODUCTS@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM REF PRODUCTS@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM ROYSTON AGT UK@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM ROYSTON AGT UK@PMM UK-ROY/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM MALAYSIA@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM PACIFIC NM@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM PACIFIC NM@PMM HK-HK/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("PMM US Phys@PMM UK-Roy", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("MIGR OFFSET@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM MEXICO@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM AGT@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM AGT@PMM US-VF/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM AGT@PMM US-VF/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM CHEMICALS NA@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM CHEMICALS NA@PMM US-VF/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM ECT NA@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM FUEL CELLS NA@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM FUEL CELLS NA@PMM US-VF/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM NOBLE METALS NA@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM NOBLE METALS NA@PMM US-VF/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM NOBLE METALS NA@PMM US-VF/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM PHARMA MATERIALS@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM PHARMA MATS@PMM US-VF/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM REFINING NA@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM REFINING NA@PMM US-VF/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM REFINING NA@PMM US-VF/GRA", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM TENNESSEE@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM TENNESSEE@PMM US-VF/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM SHANGHAI CATS LEA@PMM CN-SGH", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM ZJG LEA@PMM CN-SGH", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM ROY EMMERICH PGM@PMM UK-ROY", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("MIGR OFFSET@PMM HK-HK", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("MIGR OFFSET@PMM HK-HK/ING", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM SHANGHAI LST LEA@PMM CN-SGH", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM AUTOCAT LEA@PMM CN-SGH", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM SEC@PMM US-VF", "Rentals Interest", "Yes", logTable);
        updateAccountInfoField("JM CA POLAND@PMM UK-ROY", "Rentals Interest", "Yes", logTable);

        updateAccountInfoField("PMM HK PHYSICAL@PMM UK-ROY", "Rentals Interest", "No", logTable);
        updateAccountInfoField("JM JAPAN CHEMICALS@HSBC-LDN/ING", "Rentals Interest", "No", logTable);

        updateAccountInfoField("PMM UK@PMM HK-HK", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("PMM UK@PMM HK-HK/ING", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("PMM UK@PMM HK-HK/GRA", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("PMM HK@PMM UK-ROY", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("PMM HK@PMM UK-ROY/ING", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("PMM HK@PMM UK-ROY/GRA", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("PMM HK@HSBC-ZUR/ING", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("PMM HK@HSBC-LDN/ING", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("INV BORROWING-UK@PMM HK-HK", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("INV BORROWING-UK@PMM HK-HK/ING", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("MIGR OFFSET@PMM HK-HK", "Internal Borrowings", "UK-HK", logTable);
        updateAccountInfoField("MIGR OFFSET@PMM HK-HK/ING", "Internal Borrowings", "UK-HK", logTable);

        updateAccountInfoField("PMM US@PMM UK-ROY", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("PMM US@PMM UK-ROY/ING", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("PMM US@PMM UK-ROY/GRA", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("PMM US@CS-ZUR/ING", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("PMM US@HSBC-LDN/ING", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("PMM US@VALE-ACT", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("PMM US@HSBC-ZUR/ING", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("INV BORROWING - UK@PMM US-VF", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("INV BORROWING - UK@PMM US-VF/ING", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("PMM UK@PMM US-VF", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("PMM UK@PMM US-VF/ING", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("PMM UK@PMM US-VF/GRA", "Internal Borrowings", "UK-US", logTable);
        updateAccountInfoField("MIGR OFFSET@PMM US-VF", "Internal Borrowings", "UK-US", logTable);

        updateAccountInfoField("PMM US@PMM HK-HK/ING", "Internal Borrowings", "US-HK", logTable);
        updateAccountInfoField("PMM US@PMM HK-HK/GRA", "Internal Borrowings", "US-HK", logTable);
        updateAccountInfoField("PMM US@PMM HK-HK", "Internal Borrowings", "US-HK", logTable);
        updateAccountInfoField("PMM HK@PMM US-VF", "Internal Borrowings", "US-HK", logTable);
        updateAccountInfoField("PMM HK@PMM US-VF/ING", "Internal Borrowings", "US-HK", logTable);
        updateAccountInfoField("PMM HK@PMM US-VF/GRA", "Internal Borrowings", "US-HK", logTable);
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
                String errorMessage = DBUserTable.dbRetrieveErrorInfo(retval,
                                                                      "\nUpdating account info field '" +
                                                                      accountInfoName +
                                                                      "' for account '" +
                                                                      accountName +
                                                                      "' failed");
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
