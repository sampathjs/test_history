package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openjvs.enums.DELIVERY_TYPE_ENUM;
import com.olf.jm.logging.Logging;

public class EJMAccBalRetVostro extends EJMReportDataSource {

	private static final String COL_ACCOUNT_ID = "account_id";
	private static final String COL_ACCOUNTNUMBER = "AccountNumber";
	private static final String COL_POSITION = "position";
	private static final String COL_REPORT_DATE = "report_date";
	private static final String COL_FILTER = "filter";
	
	private static final int TRAN_STATUS_VALIDATED = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();
	private static final int TRAN_STATUS_MATURED = TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt();
	private static final int TRAN_STATUS_CLOSEOUT = TRAN_STATUS_ENUM.TRAN_STATUS_CLOSEOUT.toInt();
	private static final int DELIVERY_TYPE_CASH = DELIVERY_TYPE_ENUM.DELIVERY_TYPE_CASH.toInt();
	private static final int NOSTRO_FLAG_SETTLED = 1;
	private static final int NOSTRO_FLAG_UNSETTLED = 0;

	@Override
	protected void setOutputColumns(Table output) {
		try{
			output.addCol(COL_ACCOUNT_ID, COL_TYPE_ENUM.COL_INT, COL_ACCOUNT_ID);
			output.addCol(COL_ACCOUNTNUMBER, COL_TYPE_ENUM.COL_STRING,COL_ACCOUNTNUMBER);
			output.addCol(COL_POSITION, COL_TYPE_ENUM.COL_DOUBLE, COL_POSITION);
			output.addCol(COL_REPORT_DATE, COL_TYPE_ENUM.COL_STRING, COL_REPORT_DATE);
			output.addCol(COL_FILTER, COL_TYPE_ENUM.COL_INT, COL_FILTER);

			} catch (Exception e) {
				Logging.error("Failed to add columns to output. An exception has occurred : " + e.getMessage());
				throw new EJMReportException(e);
			} 
	}

	@Override
	protected void generateOutputData(Table output) {
		try {
			String account = reportParameter.getStringValue("account");
			String reportDate = reportParameter.getStringValue("Reporting_Date"); 
			Logging.info(String.format("Parameters [account:%s/reportDate:%s]",account,reportDate));
					
			String sqlQuery = " SELECT a.account_id AS " + COL_ACCOUNT_ID + ", a.account_number AS " + COL_ACCOUNTNUMBER + ", \n" +  
							" SUM(-nadv.ohd_position) AS " + COL_POSITION + ",\n" + 
							" '" + reportDate + "' AS " + COL_REPORT_DATE + ",\n" +
							" 0 AS " + COL_FILTER + "\n" +
							" FROM nostro_account_detail_view nadv \n" +
							"  JOIN ab_tran_event_settle ates ON (nadv.event_num=ates.event_num) \n" + 
							"  JOIN account a ON (a.account_id=ates.ext_account_id)\n" + 
							"  JOIN ab_tran ab ON (nadv.tran_num=ab.tran_num AND ab.current_flag = 1 AND ab.offset_tran_num=0)\n" + 
							"  JOIN ab_tran_event ate ON (ates.event_num=ate.event_num AND ates.delivery_type IN (" + DELIVERY_TYPE_CASH + "))\n" +   
							"  JOIN ab_tran ab1 ON (ate.tran_num=ab1.tran_num AND ab1.current_flag = 1 \n" +
							"						AND ab1.tran_status IN (" + TRAN_STATUS_VALIDATED + "," + TRAN_STATUS_MATURED + "," + TRAN_STATUS_CLOSEOUT + ")) \n" +
							" WHERE \n" +
							"	nadv.event_date<='" + reportDate + "'\n" +
							"	AND a.account_number='" + account + "'\n" +
							"   AND ate.event_date<=(SELECT processing_date FROM system_dates) \n" +
							"   AND ates.nostro_flag IN (" + NOSTRO_FLAG_UNSETTLED + "," + NOSTRO_FLAG_SETTLED + ") \n" +
							" GROUP BY a.account_id, a.account_number ";
			
			Logging.debug("Executing sql query : " + sqlQuery);
			int retVal  = DBaseTable.execISql(output, sqlQuery);
			
            if (retVal != OLF_RETURN_SUCCEED.toInt()) {
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
