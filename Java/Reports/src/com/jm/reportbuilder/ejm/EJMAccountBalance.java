package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.openlink.util.logging.PluginLog;

@com.olf.openjvs.PluginType(com.olf.openjvs.enums.SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class EJMAccountBalance extends EJMReportDataSource {
	
	private static final String COL_ACCOUNT_NUMBER = "account_number";
	private static final String COL_DATE = "date_col";
	private static final String COL_CURRENCY_ID = "currency_id";
	private static final String COL_BALANCE = "balance";
	private static final String COL_HASSPECIFICATIONS = "hasSpecifications";
	private static final String COL_HASTRANSACTIONS = "hasTransactions";
	private static final String COL_WEIGHTUNIT = "weightUnit";
	
	private static final int INFO_TYPE_ID_REPORTING_UNIT = 20003;
	
	@Override
	protected void setOutputColumns(Table output) {
		try{
			output.addCol(COL_ACCOUNT_NUMBER, COL_TYPE_ENUM.COL_STRING,"Account Number");
			output.addCol(COL_DATE, COL_TYPE_ENUM.COL_STRING, "Date" );
			output.addCol(COL_CURRENCY_ID, COL_TYPE_ENUM.COL_STRING, "Metal");
			output.addCol(COL_BALANCE, COL_TYPE_ENUM.COL_DOUBLE, "Balance");
			output.addCol(COL_HASSPECIFICATIONS, COL_TYPE_ENUM.COL_STRING, "Has Specifications");
			output.addCol(COL_HASTRANSACTIONS, COL_TYPE_ENUM.COL_STRING, "Has Transactions");
			output.addCol(COL_WEIGHTUNIT, COL_TYPE_ENUM.COL_STRING, "Weight Unit");
			
		} catch (Exception e) {
			PluginLog.error("Failed to add columns to output. An exception has occurred : " + e.getMessage());
			throw new EJMReportException(e);
		} 
	}
	
	@Override
	protected void generateOutputData(Table output) {
		try {			
			String reportDate = reportParameter.getStringValue("ReportDate");
			String accountNumber = reportParameter.getStringValue("account"); 
			PluginLog.info(String.format("Parameters [reportDate:%s/accountNumber:%s]",reportDate,accountNumber));
			
			String applicableTranStatus = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + "," + TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt();
			int unitIdTOz = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "TOz");
			int toolsetIdCommodity = TOOLSET_ENUM.COMMODITY_TOOLSET.toInt();
			
			
			String sqlQuery = "SELECT acc.account_number AS " + COL_ACCOUNT_NUMBER + ", '" + reportDate + "' AS " + COL_DATE + ",  ccy.name AS " + COL_CURRENCY_ID + ",\n" +
								"   SUM(-ate.para_position * COALESCE(uc.factor, 1)) AS " + COL_BALANCE + ", \n" +
								"   (CASE WHEN SUM(CASE WHEN toolset = " + toolsetIdCommodity + " THEN 1 ELSE 0 END) = 0 THEN 'N' ELSE 'Y' END) AS " + COL_HASSPECIFICATIONS + ",\n" +
								"   (CASE WHEN SUM(CASE WHEN toolset = " + toolsetIdCommodity + " THEN 0 ELSE 1 END) = 0 THEN 'N' ELSE 'Y' END) AS " + COL_HASTRANSACTIONS + ",\n" +
								"   iu.unit_label AS " + COL_WEIGHTUNIT + "\n" +
								" FROM ab_tran ab\n" +
								"   JOIN ab_tran_event ate ON (ate.tran_num = ab.tran_num)\n" +
								"   JOIN ab_tran_event_settle ates ON (ates.event_num = ate.event_num)\n" +
								"   JOIN account acc ON (acc.account_id = ates.ext_account_id)\n" +
								"   JOIN currency ccy ON (ccy.id_number = ates.currency_id)\n" +
								"   LEFT OUTER JOIN account_info ai ON (ai.account_id = acc.account_id AND info_type_id = " + INFO_TYPE_ID_REPORTING_UNIT + ")\n" +
								"   LEFT OUTER JOIN idx_unit iu ON (iu.unit_label = ai.info_value)\n" +
								"   LEFT OUTER JOIN unit_conversion uc ON(uc.dest_unit_id= COALESCE(iu.unit_id, " + unitIdTOz + " ) AND uc.src_unit_id = " + unitIdTOz + ")\n" +
								" WHERE acc.account_number = '" + accountNumber + "'\n" +
								"   AND ab.tran_status IN (" + applicableTranStatus + ")\n" +
								"   AND event_date <= '" + reportDate + "'\n" +
								" GROUP BY  account_number,  ccy.name, iu.unit_label, ccy.id_number\n" +
								" ORDER BY ccy.id_number";
			
			PluginLog.debug("Executing sql query : " + sqlQuery);
			int retVal  = DBaseTable.execISql(output, sqlQuery);
			
            if (retVal != OLF_RETURN_SUCCEED.toInt()) 
            {
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
