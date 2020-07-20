package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

public class EJMDailyAccountBalance extends EJMReportDataSource {
	
	private static final String COL_ACCOUNTNUMBER = "accountNumber";
	private static final String COL_METALCODE = "metalCode";
	private static final String COL_DATE = "date";
	private static final String COL_BALANCE = "balance";
	private static final String COL_NUMTRANSACTIONS = "numTransactions";
	
	private static final String MIN_RESET_DATE = "01-Jan-2016";
	private static final int INFO_TYPE_ID_REPORTING_UNIT = 20003;

	@Override
	protected void setOutputColumns(Table output) {

		try{
			output.addCol(COL_ACCOUNTNUMBER, COL_TYPE_ENUM.COL_STRING,"account_number");
			output.addCol(COL_METALCODE, COL_TYPE_ENUM.COL_STRING, "currency_id" );
			output.addCol(COL_DATE, COL_TYPE_ENUM.COL_STRING, "report_date");
			output.addCol(COL_BALANCE, COL_TYPE_ENUM.COL_DOUBLE, "position");
			output.addCol(COL_NUMTRANSACTIONS, COL_TYPE_ENUM.COL_INT, "numevent");
			
		} catch (Exception e) {
			Logging.error("Failed to add columns to output. An exception has occurred : " + e.getMessage());
			throw new EJMReportException(e);
		} 
	}

	@Override
	protected void generateOutputData(Table output) {
		try {			
			String metalCode = reportParameter.getStringValue("metalCode");
			String toDate = reportParameter.getStringValue("toDate"); 
			String fromDate = reportParameter.getStringValue("fromDate");
			String account = reportParameter.getStringValue("account"); 
			Logging.info(String.format("Parameters [metalCode:%s/toDate:%s/fromDate:%s/account:%s]",metalCode,toDate,fromDate,account));
			
			String applicableTranStatus = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + "," + TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt();
			int unitIdTOz = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "TOz");
			
			String sqlQuery = " SELECT account_number AS " + COL_ACCOUNTNUMBER + ",currency_id AS " + COL_METALCODE + ",\n" +
							" REPLACE(CONVERT(VARCHAR, reset_date, 106),' ','-') AS " + COL_DATE + ",\n" + 
							" position AS " + COL_BALANCE + ", numevent AS " + COL_NUMTRANSACTIONS + "\n" +
							" FROM\n" +
							"	(SELECT DISTINCT account_number,currency_id, reset_date,\n" +
							"	SUM(COALESCE(-para_position,0) * COALESCE(factor, 1)) OVER (PARTITION BY account_number, currency_id ORDER BY reset_date) AS position,\n" +
							"	SUM(CASE WHEN (para_position * COALESCE(factor, 1)) IS NULL THEN 0 ELSE 1 END) OVER (PARTITION BY reset_date ) AS numevent\n" +
							"	FROM \n" +
							"		(SELECT COALESCE(abs.account_number, '" + account + "') as account_number, COALESCE(abs.name,'" + metalCode + "') as currency_id, gbds.reset_date, abs.para_position, abs.factor\n" +
							"		FROM \n" +
							"			(SELECT DISTINCT reset_date FROM reset where reset_date > '" + MIN_RESET_DATE + "') gbds \n" +
							"			LEFT OUTER JOIN \n" +
							"				(SELECT  acc.account_number,ccy.name, ate.event_date, ate.para_position, uc.factor\n" +
							"				FROM ab_tran ab\n" +
							"					JOIN ab_tran_event ate ON (ate.tran_num = ab.tran_num)\n" +
							"					JOIN ab_tran_event_settle ates ON (ates.event_num = ate.event_num)\n" +
							"					JOIN account acc ON (acc.account_id = ates.ext_account_id) \n" +
							"					JOIN currency ccy ON (ccy.id_number = ates.currency_id)\n" +
							"					LEFT OUTER JOIN account_info ai ON (ai.account_id = acc.account_id AND info_type_id = " + INFO_TYPE_ID_REPORTING_UNIT + ")\n" +
							"					LEFT OUTER JOIN idx_unit iu ON(iu.unit_label = ai.info_value)\n" +
							"					LEFT OUTER JOIN unit_conversion uc ON (uc.dest_unit_id= COALESCE(iu.unit_id, " + unitIdTOz + ") AND uc.src_unit_id = " + unitIdTOz + ")\n" +
							"				WHERE acc.account_number = '" + account + "'\n" +
							"					AND ab.tran_status in (" + applicableTranStatus + ")\n" +
							"					AND ccy.name =  '" + metalCode + "'\n" +
							"				) abs \n" +
							"			ON (gbds.reset_date = abs.event_date)) as raw\n" +
							"	) bals\n" +
							" WHERE reset_date >= '" + fromDate + "'\n" +
							"	AND reset_date <= '" + toDate + "'\n" +
							" ORDER BY reset_date\n" ;
			
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
