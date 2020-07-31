package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.jm.logging.Logging;



public class EJMStatements extends EJMReportDataSource {
	
	private static final String COL_ACCOUNTNUMBER = "accountNumber";
	private static final String COL_ACCOUNT_ID = "account_id";
	private static final String COL_YEAR = "year";
	private static final String COL_MONTH = "month";
	private static final String COL_TYPE = "type";
	private static final String COL_DOCUMENTPATH = "documentPath";

	@Override
	protected void setOutputColumns(Table output) {
		try{
			output.addCol(COL_ACCOUNTNUMBER, COL_TYPE_ENUM.COL_STRING,"account_number");
			output.addCol(COL_ACCOUNT_ID, COL_TYPE_ENUM.COL_INT, "account_id" );
			output.addCol(COL_YEAR, COL_TYPE_ENUM.COL_INT, "year");
			output.addCol(COL_MONTH, COL_TYPE_ENUM.COL_STRING, "month");
			output.addCol(COL_TYPE, COL_TYPE_ENUM.COL_STRING, "type");
			output.addCol(COL_DOCUMENTPATH, COL_TYPE_ENUM.COL_STRING, "location");
			
		} catch (Exception e) {
			Logging.error("Failed to add columns to output. An exception has occurred : " + e.getMessage());
			throw new EJMReportException(e);
		} 
	}
	
	@Override
	protected void generateOutputData(Table output) {
		try {			

			int year = 0;
			String accountNumber = reportParameter.getStringValue("accountNumber"); 
			year = Integer.parseInt(reportParameter.getStringValue("year"));
			String month = reportParameter.getStringValue("month"); 
			String type = reportParameter.getStringValue("type"); 
			Logging.info(String.format("Parameters [accountNumber:%s/year:%d/month:%s/type:%s]",accountNumber,year,month,type));
			
			String sqlQuery = "SELECT ujsd.account_number AS " + COL_ACCOUNTNUMBER + ",ujsd.account_id AS " + COL_ACCOUNT_ID + ",\n" +
								" ujsd.year AS " + COL_YEAR + ", ujsd.month AS " + COL_MONTH + ",\n" +
								" ujsd.type AS " + COL_TYPE + ", ujsd.location AS " + COL_DOCUMENTPATH + "\n" + 
								" FROM USER_jm_statement_details ujsd\n" +
								" WHERE ujsd.account_number = '" + accountNumber + "'\n" +
								" AND ujsd.year = " +  year  + "\n" +
								" AND ujsd.month='" + month + "'\n" +
								" AND ujsd.type='" + type + "'";
			
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
