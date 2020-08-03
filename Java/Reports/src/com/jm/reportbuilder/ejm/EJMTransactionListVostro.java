package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

public class EJMTransactionListVostro extends EJMReportDataSource {

	private static final String COL_ACCOUNT_NUMBER = "account_number";
	private static final String COL_CURRENCY = "currency";
	private static final String COL_DEAL_TRACKING_NUM = "deal_tracking_num";
	private static final String COL_START_DATE = "start_date";
	private static final String COL_END_DATE = "end_date";
	private static final String COL_TRADE_DATE = "trade_date";
	private static final String COL_VALUE_DATE = "value_date";
	private static final String COL_INDEX_POSITION = "index_position";
	private static final String COL_CONVERSION_FACTOR = "conversion_factor";
	private static final String COL_INS_SEQ_NUM = "ins_seq_num";
	private static final String COL_JM_TRAN_ID = "jm_tran_id";
	private static final String COL_REFERNCE = "reference";
	
	private static final int TRAN_STATUS_VALIDATED = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();
	private static final int TRAN_STATUS_MATURED = TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt();
	private static final int TRAN_STATUS_CLOSEOUT = TRAN_STATUS_ENUM.TRAN_STATUS_CLOSEOUT.toInt();
	private static final int INS_TYPE_CASH = 27001;
	private static final int EVENT_TYPE_CASH_SETTLEMENT = 14;
	private static final int ACC_INFO_REPORTING_UNIT = 20003;
	private static final int DEAL_COMMENT_FROM_ACCOUNT = 20001;
	private static final int DEAL_COMMENT_TO_ACCOUNT = 20002;
	private static final int TRAN_INFO_JM_TRANSACTION_ID = 20019;
		
	@Override
	protected void setOutputColumns(Table output) {
		try{
			output.addCol(COL_ACCOUNT_NUMBER , COL_TYPE_ENUM.COL_STRING, COL_ACCOUNT_NUMBER );
			output.addCol(COL_CURRENCY , COL_TYPE_ENUM.COL_STRING, COL_CURRENCY );
			output.addCol(COL_DEAL_TRACKING_NUM , COL_TYPE_ENUM.COL_INT, COL_DEAL_TRACKING_NUM );
			output.addCol(COL_START_DATE , COL_TYPE_ENUM.COL_DATE_TIME, COL_START_DATE );
			output.addCol(COL_END_DATE , COL_TYPE_ENUM.COL_DATE_TIME, COL_END_DATE );
			output.addCol(COL_TRADE_DATE , COL_TYPE_ENUM.COL_DATE_TIME, COL_TRADE_DATE );
			output.addCol(COL_VALUE_DATE , COL_TYPE_ENUM.COL_DATE_TIME, COL_VALUE_DATE );
			output.addCol(COL_INDEX_POSITION , COL_TYPE_ENUM.COL_DOUBLE, COL_INDEX_POSITION );
			output.addCol(COL_CONVERSION_FACTOR , COL_TYPE_ENUM.COL_DOUBLE, COL_CONVERSION_FACTOR );
			output.addCol(COL_INS_SEQ_NUM , COL_TYPE_ENUM.COL_INT, COL_INS_SEQ_NUM );
			output.addCol(COL_JM_TRAN_ID , COL_TYPE_ENUM.COL_STRING, COL_JM_TRAN_ID );
			output.addCol(COL_REFERNCE , COL_TYPE_ENUM.COL_STRING, COL_REFERNCE );
			
			} catch (Exception e) {
				Logging.error("Failed to add columns to output. An exception has occurred : " + e.getMessage());
				throw new EJMReportException(e);
			} 
	}

	@Override
	protected void generateOutputData(Table output) {
		try {
			String toDate = reportParameter.getStringValue("toDate");
			String fromDate = reportParameter.getStringValue("fromDate"); 
			String accountNumber = reportParameter.getStringValue("accountNumber");
			String metalCode = reportParameter.getStringValue("metalCode"); 
			String selectionUnit = reportParameter.getStringValue("SelectionUnit");
			
			Logging.info(String.format("Parameters [toDate:%s/fromDate:%s/accountNumber:%s/metalCode:%s/selectionUnit:%s]",toDate,fromDate,accountNumber,metalCode,selectionUnit));
			
			String sqlConversionFactor = " SELECT uc.src_unit_id, uc.factor, uc.dest_unit_id, iu3.unit_label \n" +
					"  FROM unit_conversion uc \n" +
					"    INNER JOIN idx_unit iu2 ON (iu2.unit_id = uc.src_unit_id AND iu2.unit_label = '" + selectionUnit + "') \n" +
					"    INNER JOIN idx_unit iu3 ON iu3.unit_id = uc.dest_unit_id \n" +
					" UNION ALL \n" +
					" SELECT uc.unit_id, 1, uc.unit_id, uc.unit_label \n" +
					"  FROM idx_unit uc \n" +
					"  WHERE uc.unit_label = '" + selectionUnit + "' \n";
					
			String sqlQuery = " SELECT a.account_number ,ccy.name as currency, ab.deal_tracking_num, ab.start_date, ab.maturity_date AS end_date ,ab.trade_date \n" +
					" 	  ,ate.event_date AS value_date ,-ate.para_position AS index_position, con.factor AS conversion_factor, ate.ins_seq_num, ati.value AS JM_Tran_Id \n" +
					" 	  ,(CASE WHEN ab.ins_type = " + INS_TYPE_CASH + " THEN tn.line_text ELSE ab.reference END) AS reference \n" +
					" FROM ab_tran ab \n" +
					" INNER JOIN ab_tran_event ate ON (ate.tran_num=ab.tran_num AND ate.event_type = " + EVENT_TYPE_CASH_SETTLEMENT + ") \n" +
					" INNER JOIN ab_tran_event_settle ates ON ate.event_num = ates.event_num \n" +
					" INNER JOIN account a ON a.account_id = ates.ext_account_id \n" +
					" INNER JOIN currency ccy ON ccy.id_number = ate.currency \n" +
					" LEFT JOIN account_info ai ON (a.account_id = ai.account_id AND ai.info_type_id = " + ACC_INFO_REPORTING_UNIT + ") \n" +
					" LEFT JOIN idx_unit unit ON unit.unit_label = ai.info_value \n" +
					" LEFT JOIN ("+ sqlConversionFactor +") con ON con.dest_unit_id = unit.unit_id \n" +
					" LEFT JOIN tran_notepad tn ON (tn.tran_num = ab.tran_num AND tn.note_type = (CASE  WHEN ab.buy_sell = 0 THEN " + DEAL_COMMENT_FROM_ACCOUNT + " ELSE " + DEAL_COMMENT_TO_ACCOUNT + " END)) \n" +
					" LEFT JOIN ab_tran_info ati ON (ati.tran_num = ab.tran_num AND ati.type_id =  " + TRAN_INFO_JM_TRANSACTION_ID + ") \n" +
					" WHERE ab.current_flag = 1 AND ab.offset_tran_num < 1 \n" +
					" AND ab.tran_status IN (" + TRAN_STATUS_VALIDATED + "," + TRAN_STATUS_MATURED + "," + TRAN_STATUS_CLOSEOUT + ") \n" +
					" AND ate.event_date BETWEEN '" + fromDate + "' AND '" + toDate + "' \n" +
					" AND a.account_number = '" + accountNumber + "' AND ccy.name = '" + metalCode + "' \n" +
					" ORDER BY start_date ASC \n";
			
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
