package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.CFLOW_TYPE;
import com.openlink.util.logging.PluginLog;

public class EJMTransactionDetailDTR extends EJMReportDataSource {

	private static final String COL_ACCOUNT_NUMBER = "account_number";
	private static final String COL_DEAL_TRACKING_NUM = "deal_tracking_num";
	private static final String COL_REFERENCE = "reference";
	private static final String COL_JM_TRAN_ID = "jm_tran_id";
	private static final String COL_TRADE_DATE = "trade_date";
	private static final String COL_LEG = "leg";
	private static final String COL_CURRENCY = "currency";
	private static final String COL_VALUE_DATE = "value_date";
	private static final String COL_INDEX_UNIT = "index_unit"; 
	private static final String COL_PARA_POSITION = "para_position";
	private static final String COL_CONTA_ACC_NAME = "contra_account_name";
	private static final String COL_CONTA_ACC_NUMBER = "contra_account_number"; 
	private static final String COL_CONTRA_ACC_REF = "contra_account_ref";
	private static final String COL_CONTRA_WEIGHT_UNIT = "contra_weight_unit";
	private static final String COL_CONTRA_WEIGHT = "contra_weight";
	private static final String COL_CONTRA_AUTH_DATE = "contra_authorisation_date";
	private static final String COL_LOCO = "loco";
	private static final String COL_HAS_SPECIFICATION = "has_specification";
	
	private static final int TRAN_STATUS_VALIDATED = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();
	private static final int CASH_SETTLEMENT_EVENT =  EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt();
	private static final int CFLOW_TYPE_COMMODITY = CFLOW_TYPE.COMMODITY_CFLOW.toInt();
	private static final int DEAL_TYPE_BUY =  BUY_SELL_ENUM.BUY.toInt();
	private static final int DEAL_COMMENT_FROM_ACCOUNT = 20001;
	private static final int DEAL_COMMENT_TO_ACCOUNT = 20002;
	private static final int TRAN_INFO_JM_TRANSACTION_ID = 20019;
	private static final int TRAN_INFO_LOCO_ID = 20015;
	private static final int SETTLEMENT_TYPE_PHYSICAL = 2;
	private static final int IDX_UNIT_TOZ = 55;
	private static final int INS_TYPE_CASH = 27001;
	private static final int INS_TYPE_COMM_PHYS = 48010;
	private static final int INS_SUB_TYPE_CASH_TRANSFER = 10001;
	private static final int ACC_INFO_REPORTING_UNIT = 20003;
	private static final int ACC_TYPE_VOSTRO = 0;
	private static final int ACC_CLASS_METAL = 20002;
	private static final int VOLUME_TYPE_TRADING = 2;
	
	@Override
	protected void setOutputColumns(Table output) {
		try {
			output.addCol(COL_ACCOUNT_NUMBER , COL_TYPE_ENUM.COL_STRING, COL_ACCOUNT_NUMBER );
			output.addCol(COL_DEAL_TRACKING_NUM , COL_TYPE_ENUM.COL_INT, COL_DEAL_TRACKING_NUM );
			output.addCol(COL_REFERENCE , COL_TYPE_ENUM.COL_STRING, COL_REFERENCE );
			output.addCol(COL_JM_TRAN_ID , COL_TYPE_ENUM.COL_STRING, COL_JM_TRAN_ID );
			output.addCol(COL_TRADE_DATE , COL_TYPE_ENUM.COL_DATE_TIME, COL_TRADE_DATE );
			output.addCol(COL_LEG , COL_TYPE_ENUM.COL_INT, COL_LEG );
			output.addCol(COL_CURRENCY , COL_TYPE_ENUM.COL_STRING, COL_CURRENCY );
			output.addCol(COL_VALUE_DATE , COL_TYPE_ENUM.COL_DATE_TIME, COL_VALUE_DATE );
			output.addCol(COL_INDEX_UNIT, COL_TYPE_ENUM.COL_STRING, COL_INDEX_UNIT );
			output.addCol(COL_PARA_POSITION , COL_TYPE_ENUM.COL_DOUBLE, COL_PARA_POSITION );
			output.addCol(COL_CONTA_ACC_NAME , COL_TYPE_ENUM.COL_STRING, COL_CONTA_ACC_NAME );
			output.addCol(COL_CONTA_ACC_NUMBER , COL_TYPE_ENUM.COL_STRING, COL_CONTA_ACC_NUMBER );
			output.addCol(COL_CONTRA_ACC_REF , COL_TYPE_ENUM.COL_STRING, COL_CONTRA_ACC_REF );
			output.addCol(COL_CONTRA_WEIGHT_UNIT , COL_TYPE_ENUM.COL_STRING, COL_CONTRA_WEIGHT_UNIT );
			output.addCol(COL_CONTRA_WEIGHT , COL_TYPE_ENUM.COL_DOUBLE, COL_CONTRA_WEIGHT );
			output.addCol(COL_CONTRA_AUTH_DATE , COL_TYPE_ENUM.COL_DATE_TIME, COL_CONTRA_AUTH_DATE );
			output.addCol(COL_LOCO , COL_TYPE_ENUM.COL_STRING, COL_LOCO );
			output.addCol(COL_HAS_SPECIFICATION , COL_TYPE_ENUM.COL_STRING, COL_HAS_SPECIFICATION );
			
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
			PluginLog.info(String.format("Parameters [accountNumber:%s/tradeRef:%s]",accountNumber,tradeRef));
			
			String sqlQueryTransfer = " SELECT a.account_number \n" +
					" 	,ab.deal_tracking_num \n" +
					" 	,COALESCE(NULLIF(CASE WHEN ab.buy_sell = " + DEAL_TYPE_BUY + " THEN tn1.line_text ELSE tn2.line_text END,''),ab1.reference) AS reference \n" +
					" 	,atiT.value AS jm_tran_id \n" +
					" 	,ab.trade_date \n" +
					" 	,ate.ins_seq_num AS leg \n" +
					" 	,ccy.description AS currency \n" +
					" 	,ate.event_date AS value_date \n" +
					" 	,unit.unit_label AS index_unit \n" +
					" 	,ate.para_position * con.factor AS para_position \n" +
					" 	,p.long_name AS contra_account_name \n" +
					" 	,a1.account_number AS contra_account_number \n" +
					" 	,ab.reference AS contra_account_ref \n" +
					" 	,unit.unit_label AS contra_weight_unit \n" +
					" 	,ate1.para_position * con.factor AS contra_weight \n" +
					" 	,ab.trade_date contra_authorisation_date \n" +
					" 	,'NA' as loco \n" +
					" 	,'NA' AS has_specification \n" + 
					" FROM ab_tran ab \n" +
					" INNER JOIN ab_tran_event ate on ate.tran_num=ab.tran_num AND  ate.event_type = " + CASH_SETTLEMENT_EVENT + " \n" +
					" INNER JOIN ab_tran_event_settle ates ON (ate.event_num=ates.event_num) \n" +
					" INNER JOIN account a ON a.account_id=ates.ext_account_id AND a.account_number = '" + accountNumber + "' \n" +
					" INNER JOIN currency ccy on ccy.id_number = ate.currency \n" +
					" INNER JOIN ab_tran ab1 ON (ab.tran_group = ab1.tran_group AND ab.deal_tracking_num != ab1.deal_tracking_num) \n" +
					" INNER JOIN ab_tran_event ate1 on ate1.tran_num=ab1.tran_num AND  ate1.event_type = " + CASH_SETTLEMENT_EVENT + " \n" +
					" INNER JOIN ab_tran_event_settle ates1 ON (ate1.event_num=ates1.event_num) \n" +
					" INNER JOIN account a1 ON a1.account_id=ates1.ext_account_id \n" +
					" INNER JOIN party p on ab1.external_bunit = p.party_id \n" +
					" LEFT JOIN ab_tran_info atiT ON (ab.tran_num = atiT.tran_num AND atiT.type_id = " + TRAN_INFO_JM_TRANSACTION_ID + " ) \n" +
					" LEFT JOIN tran_notepad tn1 ON (ab.tran_num = tn1.tran_num AND tn1.note_type = " + DEAL_COMMENT_FROM_ACCOUNT + ") \n" +   
					" LEFT JOIN tran_notepad tn2 ON (ab.tran_num = tn2.tran_num AND tn2.note_type = " + DEAL_COMMENT_TO_ACCOUNT + ") \n" +
					" LEFT JOIN account_info ai ON a.account_id=ai.account_id AND ai.info_type_id = " + ACC_INFO_REPORTING_UNIT + " \n" +
					" LEFT JOIN idx_unit unit ON unit.unit_label=ai.info_value \n" +
					" LEFT JOIN (SELECT uc.factor,uc.dest_unit_id FROM unit_conversion uc WHERE uc.src_unit_id = " + IDX_UNIT_TOZ + " \n" +
					"            UNION SELECT 1, " + IDX_UNIT_TOZ + " ) con ON con.dest_unit_id = unit.unit_id \n" +
					" WHERE ab.tran_status = " + TRAN_STATUS_VALIDATED + " \n" +
					" 	AND ab.deal_tracking_num = " + tradeRef + "  \n" +
					"   AND ab.current_flag = 1 \n" +
					" 	AND ab.ins_type = " + INS_TYPE_CASH + " \n" +
					" 	AND ab.ins_sub_type = " + INS_SUB_TYPE_CASH_TRANSFER + " \n" +
					" 	AND a.account_type = " + ACC_TYPE_VOSTRO + " \n" +
					" 	AND a.account_class = " + ACC_CLASS_METAL + " \n" +
					" 	AND atiT.value = 'TR' \n";

			String sqlQueryDispatchReceipt = " SELECT a.account_number  \n" +
					"   ,ab.deal_tracking_num \n" +
					" 	,ab.reference \n" +
					" 	,atiT.value AS jm_tran_id \n" +
					" 	,ab.trade_date \n" +
					" 	,ate.ins_seq_num AS leg \n" +
					" 	,ccy.description AS currency \n" +
					" 	,csh.day_start_date_time AS  value_date \n" +
					" 	,unit.unit_label AS index_unit \n" +
					" 	,ate.para_position * con.factor AS para_position \n" +
					" 	,'NA' contra_account_name \n" +
					" 	,'0' contra_account_number \n" +
					" 	,COALESCE(NULLIF(CASE WHEN ab.buy_sell = " + DEAL_TYPE_BUY + " THEN tn2.line_text ELSE tn1.line_text END,''),'NA') AS contra_account_ref \n" +
					" 	,'NA' contra_weight_unit \n" +
					" 	,0.0 contra_weight \n" +
					" 	,ab.trade_date contra_authorisation_date \n" +
					" 	,atiL.value as loco \n" +
					" 	,CASE WHEN atiT.value = 'DP' THEN 'Yes' ELSE 'No' END AS has_specification \n" + 
					" FROM ab_tran ab \n" +
					" INNER JOIN ins_parameter ins ON (ab.ins_num = ins.ins_num and ins.settlement_type = " + SETTLEMENT_TYPE_PHYSICAL + ") \n" +
					" INNER JOIN ab_tran_event ate ON (ate.tran_num=ab.tran_num AND ate.ins_para_seq_num - 1 = ins.param_seq_num AND ate.ins_num=ins.ins_num) \n" +
					" INNER JOIN ab_tran_event_settle ates ON (ate.event_num=ates.event_num) \n" +
					" INNER JOIN account a ON a.account_id=ates.ext_account_id AND a.account_number = '" + accountNumber + "' \n" + 
					" LEFT JOIN ab_tran_info atiT ON (ab.tran_num = atiT.tran_num AND atiT.type_id = " + TRAN_INFO_JM_TRANSACTION_ID + " ) \n" +
					" LEFT JOIN ab_tran_info atiL ON (ab.tran_num = atiL.tran_num AND atiL.type_id = " + TRAN_INFO_LOCO_ID + " ) \n" +
					" LEFT JOIN tran_notepad tn1 ON (ab.tran_num = tn1.tran_num AND tn1.note_type= " + DEAL_COMMENT_FROM_ACCOUNT + ") \n" +   
					" LEFT JOIN tran_notepad tn2 ON (ab.tran_num = tn2.tran_num AND tn2.note_type= " + DEAL_COMMENT_TO_ACCOUNT + ") \n" +
					" INNER JOIN comm_schedule_header csh ON (csh.ins_num=ins.ins_num and csh.param_seq_num=ins.param_seq_num  and csh.volume_type = " + VOLUME_TYPE_TRADING + ") \n" +
					" LEFT JOIN account_info ai ON a.account_id=ai.account_id AND ai.info_type_id = " + ACC_INFO_REPORTING_UNIT + "  \n" +
					" LEFT JOIN idx_unit unit ON unit.unit_label=ai.info_value \n" +
					" LEFT JOIN (SELECT uc.factor,uc.dest_unit_id FROM unit_conversion uc WHERE uc.src_unit_id = " + IDX_UNIT_TOZ + " \n" +
					"            UNION SELECT 1, " + IDX_UNIT_TOZ + " ) con ON con.dest_unit_id = unit.unit_id \n" +
					" INNER JOIN currency ccy on ccy.id_number = unit.unit_id \n" +
					" WHERE ab.tran_status = " + TRAN_STATUS_VALIDATED + " \n" +
					" 	AND ab.deal_tracking_num = " + tradeRef + "  \n" +
					"   AND ab.current_flag = 1 \n" +
					" 	AND ab.ins_type = " + INS_TYPE_COMM_PHYS + " \n" +
					" 	AND ate.event_type = " + CASH_SETTLEMENT_EVENT +" \n" +
					" 	AND ate.pymt_type = " + CFLOW_TYPE_COMMODITY + " \n" +
					" 	AND atiT.value IN ('DP','RC') \n";
			
			String sqlQuery = sqlQueryTransfer + " UNION " + sqlQueryDispatchReceipt;
			
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
