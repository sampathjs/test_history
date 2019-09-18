package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openjvs.enums.VOLUME_TYPE;
import com.olf.openjvs.enums.CFLOW_TYPE;
import com.openlink.util.logging.PluginLog;

public class EJMSpecificationSummary extends EJMReportDataSource {

	private static final String COL_METAL_CODE = "metalCode";
	private static final String COL_ACCOUNT_NUMBER = "accountNumber";
	private static final String COL_START_DATE = "startDate";
	private static final String COL_END_DATE = "endDate";
	private static final String COL_FORM = "form";
	private static final String COL_PURITY = "purity";
	private static final String COL_BATCH_NUMBER = "batchNumber";
	private static final String COL_TRADE_TYPE = "tradeType";
	private static final String COL_TRADE_DATE = "tradeDate";
	private static final String COL_TRADE_REF = "tradeRef";
	private static final String COL_DISPATCH_WEIGHT = "dispatchWeight";
	private static final String COL_DISPATCH_WEIGHT_UNIT = "dispatchWeightUnit";
	private static final String COL_COUNTRY_OF_ORIGIN = "countryOfOrigin";
	private static final String COL_LGD_NUMBER = "lgdNumber";
	private static final String COL_SHEET_NUMBER = "sheetNumber";
	
	private static final int TRAN_STATUS_VALIDATED = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();
	private static final int CASH_SETTLEMENT_EVENT =  EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt();
	private static final int COMMODITY_CFLOW_TYPE =  CFLOW_TYPE.COMMODITY_CFLOW.toInt();
	private static final int NOMINATED_VOLUME_TYPE = VOLUME_TYPE.VOLUME_TYPE_NOMINATED.toInt();
	private static final int SETTLEMENT_TYPE_PHYSICAL = 2;
	private static final int PERCENT_IDX_UNIT_ID = 57;
	private static final int DELIVERY_TICKET_INFO_LGD_NUMBER  = 20005;
	private static final int TRAN_INFO_JM_TRANSACTION_ID = 20019;
	private static final int INS_TYPE_COMM_PHYS = 48010;
	
	@Override
	protected void setOutputColumns(Table output) {
		try{
			output.addCol(COL_METAL_CODE, COL_TYPE_ENUM.COL_STRING, COL_METAL_CODE);
			output.addCol(COL_ACCOUNT_NUMBER, COL_TYPE_ENUM.COL_STRING, COL_ACCOUNT_NUMBER);
			output.addCol(COL_START_DATE, COL_TYPE_ENUM.COL_DATE_TIME, COL_START_DATE);
			output.addCol(COL_END_DATE, COL_TYPE_ENUM.COL_DATE_TIME, COL_END_DATE);
			output.addCol(COL_FORM, COL_TYPE_ENUM.COL_STRING, COL_FORM);
			output.addCol(COL_PURITY, COL_TYPE_ENUM.COL_DOUBLE, COL_PURITY);
			output.addCol(COL_BATCH_NUMBER, COL_TYPE_ENUM.COL_STRING, COL_BATCH_NUMBER);
			output.addCol(COL_TRADE_TYPE, COL_TYPE_ENUM.COL_STRING, COL_TRADE_TYPE);
			output.addCol(COL_TRADE_DATE, COL_TYPE_ENUM.COL_DATE_TIME, COL_TRADE_DATE);
			output.addCol(COL_TRADE_REF, COL_TYPE_ENUM.COL_INT, COL_TRADE_REF);
			output.addCol(COL_DISPATCH_WEIGHT, COL_TYPE_ENUM.COL_DOUBLE, COL_DISPATCH_WEIGHT);
			output.addCol(COL_DISPATCH_WEIGHT_UNIT, COL_TYPE_ENUM.COL_INT, COL_DISPATCH_WEIGHT_UNIT);
			output.addCol(COL_COUNTRY_OF_ORIGIN, COL_TYPE_ENUM.COL_INT, COL_COUNTRY_OF_ORIGIN);
			output.addCol(COL_LGD_NUMBER, COL_TYPE_ENUM.COL_STRING, COL_LGD_NUMBER);
			output.addCol(COL_SHEET_NUMBER, COL_TYPE_ENUM.COL_STRING, COL_SHEET_NUMBER);
			
		} catch (Exception e) {
			PluginLog.error("Failed to add columns to output. An exception has occurred : " + e.getMessage());
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
			
			PluginLog.info(String.format("Parameters [toDate:%s/fromDate:%s/accountNumber:%s/metalCode:%s]",toDate,fromDate,accountNumber,metalCode));
			
			String sqlQuery = " SELECT  ccy.name AS metalCode, sea.account_number, csh.day_start_date_time AS fromDate, csh.day_end_date_time AS toDate, cf.comm_form_name AS form, mgi.upper_value AS purity, \n" +
					" 		cb.batch_num AS batchNumber, ati.value AS tradeType, ab.trade_date AS tradeDate, ab.deal_tracking_num AS tradeRef, tdt.delivery_ticket_volume AS dispatchWeight, \n" +
					" 		par.unit AS dispatchWeightUnit, cb.country_of_origin_id AS countryOfOrigin, dti.info_value AS gldNumber, tdt.ticket_id AS sheetNumber \n" +
					" FROM ab_tran ab \n" +
					" INNER JOIN ins_parameter par on (ab.ins_num = par.ins_num and par.settlement_type = " + SETTLEMENT_TYPE_PHYSICAL + " ) \n" +
					" INNER JOIN ( SELECT DISTINCT ate.ins_num, ate.ins_para_seq_num - 1 AS param_seq_num, acc.account_number \n" +
					" 		 		FROM account acc INNER JOIN ab_tran_event_settle ates ON (ates.ext_account_id = acc.account_id) \n" +
					" 		 		INNER JOIN ab_tran_event ate ON (ate.event_num = ates.event_num) \n" +
					" 		 		WHERE ate.event_type = " + CASH_SETTLEMENT_EVENT +" AND ate.pymt_type = " + COMMODITY_CFLOW_TYPE + " ) sea \n" + 
					"			  ON (sea.ins_num = par.ins_num AND sea.param_seq_num = par.param_seq_num) \n" + 
					" INNER JOIN comm_schedule_header csh ON (csh.ins_num = par.ins_num AND csh.param_seq_num = par.param_seq_num and csh.volume_type = " + NOMINATED_VOLUME_TYPE + " ) \n" +
					" INNER JOIN comm_sched_delivery_cmotion csdc ON csdc.delivery_id = csh.delivery_id \n" +
					" INNER JOIN comm_batch cb ON cb.batch_id = csdc.batch_id \n" +
					" INNER JOIN comm_form cf ON cf.comm_form_id = cb.form_id \n" +
					" INNER JOIN (SELECT DISTINCT measure_group_id, upper_value FROM measure_group_item WHERE version_number = 2 AND unit = " + PERCENT_IDX_UNIT_ID + " ) mgi \n" +
					"			  ON mgi.measure_group_id = csdc.measure_group_id  \n" +
					" INNER JOIN tsd_delivery_ticket tdt ON tdt.schedule_id = csh.schedule_id \n" +
					" INNER JOIN currency ccy ON ccy.id_number = ab.currency \n" +
					" LEFT JOIN delivery_ticket_info dti ON dti.delivery_ticket_id = tdt.id_number AND dti.type_id = " + DELIVERY_TICKET_INFO_LGD_NUMBER +  " \n" +
					" LEFT JOIN ab_tran_info ati ON (ati.tran_num = ab.tran_num AND ati.type_id = " + TRAN_INFO_JM_TRANSACTION_ID + " ) \n" +
					" WHERE ab.tran_status = " + TRAN_STATUS_VALIDATED + " AND ab.current_flag = 1 AND ab.ins_type = " + INS_TYPE_COMM_PHYS + " \n" + 
					" and ati.value = 'DP' AND sea.account_number = '" + accountNumber + "' AND ccy.name = '" + metalCode + "'  \n" +
					" AND csh.day_start_date_time >= '" + fromDate + "'  AND csh.day_end_date_time <= '" + toDate + "' \n" +
					" ORDER BY fromDate ASC \n";

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
