package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.jm.logging.Logging;

public class EJMAccountExists extends EJMReportDataSource {

	private static final String COL_EXISTS = "does_exists";
	private static final String COL_GTACCOUNTNUMBER = "gtAccountNumber";
	private static final String COL_ACCOUNTNAME = "accountName";
	
	private static final int INFO_TYPE_ID_GT_ACCT_NUMBER= 20018;
	
	@Override
	protected void setOutputColumns(Table output) {
		try{
			output.addCol(COL_EXISTS, COL_TYPE_ENUM.COL_STRING,"exists");
			output.addCol(COL_GTACCOUNTNUMBER, COL_TYPE_ENUM.COL_STRING, "GT Account No" );
			output.addCol(COL_ACCOUNTNAME, COL_TYPE_ENUM.COL_STRING, "Account Name");
			
		} catch (Exception e) {
			Logging.error("Failed to add columns to output. An exception has occurred : " + e.getMessage());
			throw new EJMReportException(e);
		} 
	}
	
	@Override
	protected void generateOutputData(Table output) {
		try {			
			String accountNumber = reportParameter.getStringValue("ACCOUNT_NUMBER"); 
			Logging.info(String.format("Parameters [accountNumber%s]",accountNumber));
			
			String sqlQuery = "IF EXISTS (\n" +
								"	SELECT 1 FROM account WHERE account_number = '" + accountNumber + "'\n" +
								") (\n" +
								"	SELECT 'True' AS " + COL_EXISTS + ", \n" +
								"	   COALESCE(ai.info_value, '')  AS " + COL_GTACCOUNTNUMBER + ",\n" +
								"	   COALESCE(NULLIF(p.long_name,''), p.short_name, 'NOT Linked')  AS " + COL_ACCOUNTNAME + "\n" +
								"	FROM account a\n" +
								"		LEFT JOIN party_account pa ON (a.account_id=pa.account_id)\n" +
								"		LEFT JOIN party p ON (pa.party_id=p.party_id)\n" +
								"		LEFT JOIN account_info ai ON (a.account_id=ai.account_id AND ai.info_type_id=" + INFO_TYPE_ID_GT_ACCT_NUMBER + ")\n" +
								"	WHERE a.account_number = '" + accountNumber + "' \n" +
								" ) ELSE (\n" +
								"	SELECT 'False' AS " + COL_EXISTS + ", '' AS " + COL_GTACCOUNTNUMBER + ", '' AS " + COL_ACCOUNTNAME + " \n" +
								")";
			
			Logging.debug("Executing sql query : " + sqlQuery);
			int retVal  = DBaseTable.execISql(output, sqlQuery);
			
            if (retVal != OLF_RETURN_SUCCEED.toInt()) 
            {
                Logging.error("Failed to execute sql query : " + sqlQuery);
                String error = DBUserTable.dbRetrieveErrorInfo(retVal, "");
                throw new EJMReportException(error);
            }
            
            Logging.info("Number of rows retrieved : " + output.getNumRows());
            
		} catch (Exception e) {
			Logging.error("Failed to generate output data. An exception has occurred : " + e.getMessage());
			throw new EJMReportException(e);
		} 
	}
}
