package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DELIVERY_TYPE_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

public class EJMAccBalRetInventory extends EJMReportDataSource {

	private static final String COL_ACCOUNT_ID = "account_id";
	private static final String COL_ACCOUNT_NUMBER = "account_number";
	private static final String COL_DATE = "date";
	private static final String COL_METAL = "metal";
	private static final String COL_BALANCE = "balance";
	private static final String COL_REPORTING_UNIT = "reporting_unit";
	private static final String COL_WEIGHTUNIT = "weight_unit";
	
	private static final int TRAN_STATUS_VALIDATED = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt();
	private static final int TRAN_STATUS_MATURED = TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt();
	private static final int TRAN_STATUS_CLOSEOUT = TRAN_STATUS_ENUM.TRAN_STATUS_CLOSEOUT.toInt();
	private static final int DELIVERY_TYPE_CASH = DELIVERY_TYPE_ENUM.DELIVERY_TYPE_CASH.toInt();
	private static final int INS_SUB_TYPE_CASH_TRANSFER = 10001;
	private static final int ACCOUNT_INFO_REPORTINGUNIT = 20003;
	private static final int NOSTRO_FLAG_SETTLED = 1;
	private static final int NOSTRO_FLAG_UNSETTLED = 0;
	private static final int IDX_UNIT_ID_TOZ = 55;
	private static final int INS_VOSTRO = 47001;
	private static final int INS_NOSTRO = 47002;
	private static final int INS_VOSTRO_ML = 47005;
	private static final int INS_NOSTRO_ML = 47006;
	private static final int ACCOUNT_TYPE_VOSTRO = 0;
	private static final int ACCOUNT_TYPE_VOSTRO_ML = 4;
	
	@Override
	protected void setOutputColumns(Table output) {
		try{
			output.addCol(COL_ACCOUNT_ID, COL_TYPE_ENUM.COL_INT, COL_ACCOUNT_ID);
			output.addCol(COL_ACCOUNT_NUMBER, COL_TYPE_ENUM.COL_STRING,COL_ACCOUNT_NUMBER);
			output.addCol(COL_DATE, COL_TYPE_ENUM.COL_STRING, COL_DATE);
			output.addCol(COL_METAL, COL_TYPE_ENUM.COL_STRING, COL_METAL);
			output.addCol(COL_BALANCE, COL_TYPE_ENUM.COL_DOUBLE, COL_BALANCE);
			output.addCol(COL_REPORTING_UNIT, COL_TYPE_ENUM.COL_STRING, COL_REPORTING_UNIT);
			output.addCol(COL_WEIGHTUNIT, COL_TYPE_ENUM.COL_STRING, COL_WEIGHTUNIT);

			} catch (Exception e) {
				PluginLog.error("Failed to add columns to output. An exception has occurred : " + e.getMessage());
				throw new EJMReportException(e);
			} 
	}

	@Override
	protected void generateOutputData(Table output) {
		try {
			String account = reportParameter.getStringValue("account");
			String reportDate = reportParameter.getStringValue("ReportDate"); 
			String reportingUnit = reportParameter.getStringValue("ReportingUnit");
			String metal = reportParameter.getStringValue("metal");
			PluginLog.info(String.format("Parameters [account:%s/reportDate:%s/ReportingUnit:%s/metal:%s]",account,reportDate,reportingUnit,metal));
	
			String sqlQueryNostroABR = " SELECT account_id , currency_id, sum(toz_position) ohd_position, " + IDX_UNIT_ID_TOZ + " as unit\n" +
					 " FROM ( SELECT  ates.int_account_id as account_id, nadv.currency_id, nadv.delivery_type, \n" +
					 "        nadv.portfolio_id, nadv.ohd_position toz_position, \n" +
					 "       (CASE ab.ins_sub_type WHEN " + INS_SUB_TYPE_CASH_TRANSFER + " THEN " + IDX_UNIT_ID_TOZ + " ELSE nadv.unit END) unit , '" + reportDate + "' as report_date\n" +
					 "		 FROM nostro_account_detail_view nadv \n" +
					 "		 INNER JOIN ab_tran_event_settle ates ON (nadv.event_num=ates.event_num)\n" +  
					 "		 INNER JOIN ab_tran ab ON (nadv.tran_num=ab.tran_num AND ab.current_flag=1 \n" +
					 "                                 AND ab.ins_type NOT IN ( " + INS_VOSTRO + "," + INS_NOSTRO + "," + INS_VOSTRO_ML + "," + INS_NOSTRO_ML + " ) \n" + 
					 "                                 AND ab.tran_status IN (" + getApplicableTranStatus() + "))\n" +
					 "		 WHERE nadv.event_date <= '" + reportDate + "'\n" +
					 "		 AND nadv.event_num in ( SELECT ate.event_num \n" +
					 "					FROM ab_tran_event_settle ates  \n" +
					 "					JOIN ab_tran_event ate ON (ates.event_num=ate.event_num) \n" +
					 "					JOIN ab_tran ab ON (ate.tran_num=ab.tran_num AND ab.current_flag=1 AND ab.tran_status IN (" + getApplicableTranStatus() + ") ) \n" + 
					 "					WHERE ates.nostro_flag IN (" + NOSTRO_FLAG_UNSETTLED + "," + NOSTRO_FLAG_SETTLED + "))\n" + 					 
					 "					AND ates.delivery_type = " + DELIVERY_TYPE_CASH + ") nostro \n" +
					 " GROUP BY nostro.account_id, nostro.currency_id";
			
			String sqlQueryVostroABR = " SELECT account_id, currency_id, SUM(pos) as ohd_position , \n" + 
					"  (CASE WHEN unit_id IS NULL OR unit_id = 0 THEN " + IDX_UNIT_ID_TOZ + " ELSE unit_id END) as unit\n" +
					"  FROM ( SELECT ates.ext_account_id as account_id, ates.currency_id, ates.delivery_type, \n" +
					"		 ab.internal_portfolio as portfolio_id, -ates.settle_amount as pos, iu.unit_id\n" +
					"        FROM ab_tran_event_settle ates  \n" +
					"        INNER JOIN ab_tran_event ate ON (ate.event_num = ates.event_num)\n" +
					"        INNER JOIN ab_tran ab ON (ate.tran_num=ab.tran_num)\n" +
					"        INNER JOIN account acc ON (acc.account_id=ates.ext_account_id)\n" +
					"        LEFT JOIN account_info acci ON (acc.account_id = acci.account_id AND acci.info_type_id = "+ ACCOUNT_INFO_REPORTINGUNIT +" )\n" +
					"        LEFT JOIN idx_unit iu ON (iu.unit_label = acci.info_value)\n" +
					"        WHERE ab.current_flag = 1 \n" +
					"           AND ab.offset_tran_num = 0 \n" +
					"           AND ab.ins_type NOT IN ( " + INS_NOSTRO + "," + INS_VOSTRO_ML + "," + INS_NOSTRO_ML + " )\n" +
					"           AND ates.nostro_flag = " + NOSTRO_FLAG_SETTLED + "\n" +
					"           AND acc.account_type IN ( " + ACCOUNT_TYPE_VOSTRO + "," + ACCOUNT_TYPE_VOSTRO_ML + " )\n" +
					"           AND ab.tran_status IN (" + getApplicableTranStatus() + ") \n" +
					"           AND ate.event_date<= '" + reportDate + "'\n" +
					"           AND ates.delivery_type = " + DELIVERY_TYPE_CASH + ") vostro \n" +
					" GROUP BY vostro.account_id ,vostro.currency_id, vostro.unit_id";
			
			String sqlQueryAccountReportingUnit = "	SELECT a.account_id, a.account_number, iu.unit_id, \n" +
					"  CASE WHEN '" + reportingUnit + "' = 'None' THEN coalesce(info.info_value, 'TOz') ELSE '" + reportingUnit + "' END as ReportingUnit \n" + 
					"  FROM account a \n" +
					"  LEFT JOIN account_info_type  ait ON ait.type_name = 'Reporting Unit' \n" + 
				    "  LEFT JOIN account_info info ON a.account_id =  info.account_id AND info.info_type_id = ait.type_id \n" + 
				    "  LEFT JOIN idx_unit iu ON iu.unit_label=info.info_value \n" + 
					"  INNER JOIN ( \n" + 
					"  	  SELECT uc.src_unit_id,uc.factor,uc.dest_unit_id, iu3.unit_label \n" + 
					"  	  FROM unit_conversion uc \n" + 
					"  	  INNER JOIN idx_unit iu2 ON iu2.unit_id=uc.src_unit_id AND iu2.unit_label='TOz' \n" + 
					"  	  INNER JOIN idx_unit iu3 ON iu3.unit_id=uc.dest_unit_id \n" + 
					"  	  UNION ALL \n" + 
					"  	  SELECT uc.unit_id,1,uc.unit_id, uc.unit_label \n" + 
					"  	  FROM idx_unit uc \n" + 
					"  	  WHERE uc.unit_label='TOz'\n" + 
					"   ) conversion ON conversion.dest_unit_id =  iu.unit_id \n";

			String sqlQuery = " SELECT acc_bal.account_id, acc.account_number AS account_number, '" + reportDate +"' AS report_date , ccy.name AS metal, \n" +
					"  acc_bal.ohd_position as balance,  acc.ReportingUnit, idu.unit_label AS unit \n" +
					"  FROM (" + sqlQueryNostroABR + " UNION " + sqlQueryVostroABR + " ) acc_bal \n" + 
					"  INNER JOIN currency ccy ON ccy.id_number = acc_bal.currency_id  \n" +
					"  LEFT JOIN ( " + sqlQueryAccountReportingUnit + " ) acc on acc.account_id = acc_bal.account_id  \n" +
					"  INNER JOIN idx_unit idu on idu.unit_id = acc.unit_id  \n" +
					"  WHERE acc.account_number = '" + account + "' AND ( ccy.name = '" + metal + "' OR '" + metal + "'  = 'None') \n";
					
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
	
	private String getApplicableTranStatus(){
		return TRAN_STATUS_VALIDATED + "," + TRAN_STATUS_MATURED + "," + TRAN_STATUS_CLOSEOUT;
	}
}
