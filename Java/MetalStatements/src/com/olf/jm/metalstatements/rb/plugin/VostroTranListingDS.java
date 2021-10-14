package com.olf.jm.metalstatements.rb.plugin;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.jm.logging.Logging;

public class VostroTranListingDS extends MetalStatementsDataSource {

	private static final int TRAN_INFO_TYPE_ID_JM_TRANSACTION_ID = 20019;
	private static final int TRAN_INFO_TYPE_ID_LOCO = 20015;
	private static final int ACCOUNT_INFO_TYPE_ID_REPORTING_UNIT = 20003;
	private static final int ACCOUNT_INFO_TYPE_ID_LOCO = 20006;
	private static final int IDX_UNIT_TOZ = 55;
	private static final int ACCOUNT_CLASS_METAL_ACCOUNT_ID = 20002;
	
	private static final int TRAN_STATUS_VALIDATED = EnumTranStatus.Validated.getValue();
	private static final int TRAN_STATUS_MATURED = EnumTranStatus.Matured.getValue();
	private static final int TRAN_STATUS_CLOSEOUT = EnumTranStatus.Closeout.getValue();
	
	
	@Override
	protected void setOutputColumns(Table output) {
		
	}
	
	@Override
	protected void formatOutputData(Table output) {
		
	}

	@Override
	protected Table buildOutput(Session session, Table output) {
		String sqlQuery = null;
		String eventDateRangePart = "";
		String tradeDateFilter = "";
		String tranStatusFilter = "";
		
		try {
			Logging.info(String.format("Running report: %s", this.parameter.getStringValue("REPORT_NAME")));
			String accountId = this.parameter.getStringValue("accountID");
			String fromDate = this.parameter.getStringValue("ReportDate");
			String toDate = this.parameter.getStringValue("StatementDate");
			String isJMLoco = this.parameter.getStringValue("IsJMLoco");
			String reportType = this.parameter.getStringValue("ReportType");
			
			if (reportType == null || "".equals(reportType)) {
				String message = String.format("ReportType parameter found missing or having empty value in report: %s", this.parameter.getStringValue("REPORT_NAME")); 
				Logging.error(message);
				throw new RuntimeException(message);
			}
			
			if ("FORWARD".equalsIgnoreCase(reportType)) {
				eventDateRangePart = " AND ate.event_date > CONVERT(date, '" + toDate + "') ";
				tradeDateFilter = " AND CONVERT(date, '" + toDate + "') >= ab.trade_date ";
				tranStatusFilter = String.valueOf(TRAN_STATUS_VALIDATED);
				
			} else if ("MATURED".equalsIgnoreCase(reportType)) {
				eventDateRangePart = " AND ate.event_date > CONVERT(date, '" + fromDate + "') AND ate.event_date <= CONVERT(date, '" + toDate + "')";
				tradeDateFilter = "";
				tranStatusFilter = String.valueOf(TRAN_STATUS_VALIDATED);
				
			} else if ("SUMMARY".equalsIgnoreCase(reportType)) {
				eventDateRangePart = " AND (ate.event_date != CONVERT(date, '" + fromDate + "') AND ate.event_date <= CONVERT(date, '" + toDate + "'))";
				tradeDateFilter = "";
				tranStatusFilter = String.format("%d, %d, %d", TRAN_STATUS_VALIDATED, TRAN_STATUS_MATURED, TRAN_STATUS_CLOSEOUT);
			}
			
			Logging.info(String.format("Input Parameters [accountId:%s], [fromDate:%s], [toDate:%s], [isJMLoco:%s], [ReportType:%s]", accountId, fromDate, toDate, isJMLoco, reportType));
			
			sqlQuery = "SELECT a.account_number \n"
							+ ", ates.ext_account_id AS VostroAccount_id \n"
							+ ", a.account_type \n"
							+ ", a.account_name \n"
							+ ", a.account_class \n"
							+ ", ac.account_class_name \n"
							+ ", ab.deal_tracking_num \n"
							+ ", ab.tran_num \n"
							+ ", ab.tran_group \n"
							+ ", ab.tran_status \n"
							+ ", ati.value AS JM_Tran_Id \n"
							+ ", CONCAT(ati.value, ab.deal_tracking_num) AS custom_ref \n"
							+ ", ab.internal_portfolio \n"
							+ ", ab.ins_type \n"
							+ ", ate.event_num \n"
							+ ", ab.trade_date \n"
							+ ", ab.reference \n"
							+ ", ate.ins_seq_num \n"
							+ ", ab.start_date \n"
							+ ", ab.maturity_date AS end_date \n"
							+ ", ate.event_date AS value_date \n"
							+ ", ab.buy_sell \n"
							+ ", ab.price price \n"
							+ ", ate.currency AS Currency \n"
							+ ", c.description AS currency_name \n"
							+ ", -ate.para_position IndexPosition \n"
							+ ", (CASE WHEN ate.para_position = 0.0 THEN '0' ELSE c.description END) AS currency1 \n"
							+ ", (CASE WHEN ate.currency IN (SELECT id_number FROM currency WHERE precious_metal = 1) AND ate.event_type = 14 THEN 'TOz' ELSE 'Currency' END) AS index_unit \n"
							+ ", 'UConv' AS UnitConv \n"
							+ ", 'TP' AS TradePosition \n"
							+ ", ate.unit AS TradedUnit \n"
							+ ", unit.unit_id /*AS ReportingUnit*/ \n"
							+ ", unit.unit_label \n"
							+ ", 'RP Pos' AS ReportingPosition \n"
							+ ", (CASE WHEN ate.currency IN (SELECT id_number FROM currency WHERE precious_metal = 1) AND ate.event_type = 14 THEN 0 ELSE ate.currency END) AS settlementCurrency \n"
							+ ", COALESCE (atiL.value, aiL.info_value) AS Loco \n"
							+ ", aiL.info_value AS account_loco \n"
							+ ", tn.line_num \n"
							+ ", tn.note_type \n"
							+ ", (CASE ati.value WHEN 'TR' THEN (ISNULL(NULLIF(LEFT(tn.line_text, Charindex (CHAR(10), tn.line_text)), ''), tn.line_text)) ELSE '' END) AS first_line \n" 
							+ ", con.factor \n"
							+ ", (CASE WHEN -ate.para_position < 0.0 THEN (-1)*-ate.para_position*con.factor ELSE 0.0 END) AS debit \n"
							+ ", (CASE WHEN -ate.para_position > 0.0 THEN -ate.para_position*con.factor ELSE 0.0 END) AS credit \n"
							
							+ " FROM ab_tran ab \n"
								+ " INNER JOIN ab_tran_event ate ON (ate.tran_num = ab.tran_num "
																		+ " AND ate.event_type = 14 " + eventDateRangePart + ") \n" 
								+ " INNER JOIN ab_tran_event_settle ates ON (ate.event_num = ates.event_num) \n"
								+ " INNER JOIN currency c ON (c.id_number = ate.currency) \n"
								+ " INNER JOIN account a ON (a.account_id = ates.ext_account_id "
																+ " AND a.account_id = CAST ('" + accountId + "' AS Int) "
																+ "	AND a.account_class = " + ACCOUNT_CLASS_METAL_ACCOUNT_ID + ") \n"
								+ " INNER JOIN account_class ac ON (ac.account_class_id = a.account_class) \n"
								+ " LEFT JOIN ab_tran_info ati ON (ab.tran_num = ati.tran_num AND ati.type_id = " + TRAN_INFO_TYPE_ID_JM_TRANSACTION_ID + ") \n"
								+ " LEFT JOIN ab_tran_info atiL ON (ab.tran_num = atiL.tran_num AND atiL.type_id = " + TRAN_INFO_TYPE_ID_LOCO + ") \n"
								+ " LEFT JOIN account_info ai ON (a.account_id = ai.account_id AND ai.info_type_id = " + ACCOUNT_INFO_TYPE_ID_REPORTING_UNIT + ") \n"
								+ " LEFT JOIN account_info aiL ON (a.account_id = aiL.account_id AND aiL.info_type_id  = " + ACCOUNT_INFO_TYPE_ID_LOCO + ") \n"
								+ " LEFT JOIN idx_unit unit ON (unit.unit_label = ai.info_value) \n"
								+ " LEFT JOIN USER_jm_loco l ON (l.loco_name = COALESCE (atiL.value, aiL.info_value)) \n"
								
								+ " LEFT JOIN (SELECT max(tn2.comment_num) as comment_num, tran_num, note_type FROM tran_notepad tn2 GROUP BY tran_num, note_type) max_comment" 
										+ " ON (max_comment.tran_num = ab.tran_num "
											+ "	AND max_comment.note_type = (CASE WHEN ab.buy_sell = 0 THEN 20001 WHEN ab.buy_sell = 1 THEN 20002 END)) \n"
								+ " LEFT JOIN tran_notepad tn ON (max_comment.tran_num = tn.tran_num AND max_comment.note_type = tn.note_type AND tn.comment_num = max_comment.comment_num and tn.line_num = 0) \n" // EPI-1933 fix to remove duplicate rows because of multiple deal comments 
							
								+ "INNER JOIN ( \n"
									+ " SELECT uc.factor, uc.dest_unit_id FROM unit_conversion uc WHERE uc.src_unit_id = " + IDX_UNIT_TOZ + " \n"
									+ " UNION \n"
									+ " SELECT 1, " + IDX_UNIT_TOZ + " \n"
								+ ") con ON con.dest_unit_id = unit.unit_id \n"
						
						+ " WHERE ab.tran_status IN (" + tranStatusFilter + ") \n"
								+ tradeDateFilter  + " \n"
								+ " AND ab.current_flag = 1 \n"
								+ " AND ab.offset_tran_num < 1 \n"
								+ " AND ISNULL(l.is_pmm_id, 0) = '" + isJMLoco + "' \n";
			
			Logging.debug("Executing sql query : " + sqlQuery);
			
			output = session.getIOFactory().runSQL(sqlQuery);
			
            Logging.info("Number of rows retrieved : " + output.getRowCount());
            if (output == null || output.getColumnCount() == 0) {
            	throw new Exception(String.format("No data retrieved by executing query-%s", sqlQuery));
            }
            
		} catch (Exception e) {
			Logging.error(String.format("Failed to generate output data. An exception has occurred: %s", e.getMessage()));
			throw new RuntimeException(e);
		}
		
		return output;
	}

}
