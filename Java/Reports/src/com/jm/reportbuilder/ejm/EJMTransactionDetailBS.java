package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

public class EJMTransactionDetailBS extends EJMReportDataSource {

	private static final String COL_ACCOUNT_NUMBER = "account_number";
	private static final String COL_ACCOUNT_ID = "account_id";
	private static final String COL_DEAL_TRACKING_NUM = "deal_tracking_num";
	private static final String COL_TRAN_NUM = "tran_num";
	private static final String COL_EVENT_NUM = "event_num";
	private static final String COL_JM_TRAN_ID = "jm_tran_id";
	private static final String COL_TRADE_DATE = "trade_date";
	private static final String COL_REFERNCE = "reference";
	private static final String COL_INS_SEQ_NUM = "ins_seq_num";
	private static final String COL_VALUE_DATE = "value_date";
	private static final String COL_PRICE = "price"; 
	private static final String COL_CURRENCY = "currency";
	private static final String COL_INDEX_POSITION = "index_position";
	private static final String COL_UNIT_CONV = "unit_conv";
	private static final String COL_REPORTING_UNIT = "reporting_unit"; 
	private static final String COL_SETTLEMENT_CURRENCY = "settlement_currency";
	private static final String COL_LOCO = "loco";
	
	private static final int TRAN_STATUS_VALIDATED = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();
	private static final int TRAN_STATUS_MATURED = TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt();
	private static final int TRAN_STATUS_CLOSEOUT = TRAN_STATUS_ENUM.TRAN_STATUS_CLOSEOUT.toInt();
	private static final int CASH_SETTLEMENT_EVENT =  EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt();
	private static final int DEAL_TYPE_BUY =  BUY_SELL_ENUM.BUY.toInt();
	private static final int DEAL_COMMENT_FROM_ACCOUNT = 20001;
	private static final int DEAL_COMMENT_TO_ACCOUNT = 20002;
	private static final int INS_TYPE_CASH = 27001;
	private static final int TRAN_INFO_LOCO_ID = 20015;
	private static final int TRAN_INFO_JM_TRANSACTION_ID = 20019;
	private static final int ACC_INFO_REPORTING_UNIT = 20003;
	private static final int ACC_TYPE_VOSTRO = 0;
	private static final int ACC_CLASS_METAL = 20002;
	private static final int IDX_UNIT_TOZ = 55;
	
	@Override
	protected void setOutputColumns(Table output) {
		try{
			output.addCol(COL_ACCOUNT_NUMBER , COL_TYPE_ENUM.COL_STRING, COL_ACCOUNT_NUMBER );
			output.addCol(COL_ACCOUNT_ID , COL_TYPE_ENUM.COL_INT, COL_ACCOUNT_ID );
			output.addCol(COL_DEAL_TRACKING_NUM , COL_TYPE_ENUM.COL_INT, COL_DEAL_TRACKING_NUM );
			output.addCol(COL_TRAN_NUM , COL_TYPE_ENUM.COL_INT, COL_TRAN_NUM );
			output.addCol(COL_EVENT_NUM , COL_TYPE_ENUM.COL_INT64, COL_EVENT_NUM );
			output.addCol(COL_JM_TRAN_ID , COL_TYPE_ENUM.COL_STRING, COL_JM_TRAN_ID );
			output.addCol(COL_TRADE_DATE , COL_TYPE_ENUM.COL_DATE_TIME, COL_TRADE_DATE );
			output.addCol(COL_REFERNCE , COL_TYPE_ENUM.COL_STRING, COL_REFERNCE );
			output.addCol(COL_INS_SEQ_NUM , COL_TYPE_ENUM.COL_INT, COL_INS_SEQ_NUM );
			output.addCol(COL_VALUE_DATE , COL_TYPE_ENUM.COL_DATE_TIME, COL_VALUE_DATE );
			output.addCol(COL_PRICE , COL_TYPE_ENUM.COL_DOUBLE, COL_PRICE );
			output.addCol(COL_CURRENCY , COL_TYPE_ENUM.COL_STRING, COL_CURRENCY );
			output.addCol(COL_INDEX_POSITION , COL_TYPE_ENUM.COL_DOUBLE, COL_INDEX_POSITION );
			output.addCol(COL_UNIT_CONV , COL_TYPE_ENUM.COL_DOUBLE, COL_UNIT_CONV );
			output.addCol(COL_REPORTING_UNIT , COL_TYPE_ENUM.COL_STRING, COL_REPORTING_UNIT );
			output.addCol(COL_SETTLEMENT_CURRENCY , COL_TYPE_ENUM.COL_STRING, COL_SETTLEMENT_CURRENCY );
			output.addCol(COL_LOCO , COL_TYPE_ENUM.COL_STRING, COL_LOCO );

			} catch (Exception e) {
				PluginLog.error("Failed to add columns to output. An exception has occurred : " + e.getMessage());
				throw new EJMReportException(e);
			} 
	}

	@Override
	protected void generateOutputData(Table output) {
		try {
			String accountNumber = reportParameter.getStringValue("accountNumber");
			String tradeRef = reportParameter.getStringValue("tradeRef");
			String reportingUnit = reportParameter.getStringValue("ReportingUnit"); 
			PluginLog.info(String.format("Parameters [accountNumber:%s/tradeRef:%s/reportingUnit:%s]",accountNumber,tradeRef,reportingUnit));
					
			String sqlQuery = " SELECT a.account_number \n" +
				    " ,ates.ext_account_id AS account_id \n" +
				    " ,ab.deal_tracking_num \n" +
					" ,ab.tran_num \n" +
				    " ,ate.event_num \n" +
				    " ,atiT.value AS jm_tran_id \n" +
				    " ,ab.trade_date \n" +
				    " ,(CASE WHEN ab.ins_type= " + INS_TYPE_CASH + " THEN tn.line_text ELSE ab.reference END) reference \n" +
				    " ,ate.ins_seq_num \n" +
				    " ,ate.event_date AS value_date \n" +
				    " ,ab.price \n" +
				    " ,ccy.name AS currency \n" +
				    " ,-ate.para_position index_position \n" +
				    " ,con.factor unit_conv \n" +
					" ,CASE WHEN '" + reportingUnit + "' = 'None' THEN coalesce(ai.info_value, 'TOz') ELSE '" + reportingUnit + "' END AS reporting_unit \n" +
				    " ,CASE WHEN ate.currency IN (select id_number from currency where precious_metal=1) AND ate.event_type = " + CASH_SETTLEMENT_EVENT + " THEN 'USD' ELSE ccy.name END  AS settlement_currency \n" +
				    " ,COALESCE ( atiL.value , atiT.value ) as loco \n" +
				    " FROM ab_tran ab \n" +
				    " INNER JOIN ab_tran_event ate on ate.tran_num=ab.tran_num AND  ate.event_type = " + CASH_SETTLEMENT_EVENT + "  \n" +
				    " INNER JOIN ab_tran_event_settle ates ON (ate.event_num=ates.event_num) \n" +
				    " INNER JOIN account a ON a.account_id=ates.ext_account_id AND a.account_number = '" + accountNumber + "' \n" +
				    " LEFT JOIN tran_notepad tn ON (tn.tran_num = ab.tran_num AND tn.note_type = (CASE WHEN ab.buy_sell= " + DEAL_TYPE_BUY + " THEN " + DEAL_COMMENT_FROM_ACCOUNT + " ELSE " + DEAL_COMMENT_TO_ACCOUNT + " END)) \n" +
				    " LEFT JOIN ab_tran_info atiL ON (ab.tran_num = atiL.tran_num AND atiL.type_id = " + TRAN_INFO_LOCO_ID + " ) \n" +
				    " LEFT JOIN ab_tran_info atiT ON (ab.tran_num = atiT.tran_num AND atiT.type_id = " + TRAN_INFO_JM_TRANSACTION_ID + " ) \n" +
				    " LEFT JOIN account_info ai ON a.account_id=ai.account_id AND ai.info_type_id =  " + ACC_INFO_REPORTING_UNIT +  " \n" +
				    " LEFT JOIN idx_unit unit ON unit.unit_label=ai.info_value \n" +
					" LEFT JOIN (SELECT uc.factor,uc.dest_unit_id FROM unit_conversion uc WHERE uc.src_unit_id = " + IDX_UNIT_TOZ + " \n" +
					"            UNION SELECT 1, " + IDX_UNIT_TOZ + " ) con ON con.dest_unit_id = unit.unit_id \n" +
				    " INNER JOIN Currency ccy ON ccy.id_number = ate.currency \n" +
				    " WHERE  ab.tran_status IN (" + TRAN_STATUS_VALIDATED + "," + TRAN_STATUS_MATURED + "," + TRAN_STATUS_CLOSEOUT + ") \n" +
				    "   AND ab.current_flag = 1 \n" +
				    "   AND ab.offset_tran_num < 1 \n" +
					"   AND ab.tran_num = " + tradeRef + " \n" +
					"   AND a.account_type = " + ACC_TYPE_VOSTRO + " \n" +
					"   AND account_class = " + ACC_CLASS_METAL + " \n" +
					"   AND atiT.value IN ('BM','SM') \n";
			
			PluginLog.debug("Executing sql query : " + sqlQuery);
			int retVal  = DBaseTable.execISql(output, sqlQuery);
			
            if (retVal != OLF_RETURN_SUCCEED.toInt()) {
                PluginLog.error("Failed to execute sql query : " + sqlQuery);
                String error = DBUserTable.dbRetrieveErrorInfo(retVal, "");
                throw new EJMReportException(error);
            }
            
            PluginLog.info("Number of rows retrieved : " + output.getNumRows());
            
		} catch (Exception e) {
			PluginLog.error("Failed to generate output data. An exception has occurred : " + e.getMessage());
			throw new EJMReportException(e);
		} 	
	}
}
