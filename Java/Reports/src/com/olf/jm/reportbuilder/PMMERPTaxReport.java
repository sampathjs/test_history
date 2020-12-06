package com.olf.jm.reportbuilder;


import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 24-Nov-2020 |               | Giriraj Joshi   | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class PMMERPTaxReport implements IScript {

	private final int MAIN_ADDRESS_TYPE = 1;
	private final int STLDOC_TYPE_INVOICE = 1;
	private final int JM_UK_BU = 20006;
	private final int MANUAL_VAT_CFLOW_TYPE = 2009;
	private final int VAT_CFLOW_TYPE = 2018;
	private final int TRANSPORTATION_CFLOW_TYPE = 2044;
	private final int TRANSFER_CHARGE_CFLOW_TYPE = 2020;
	private final int STLDOC_INFO_OUR_DOC_NUM = 20003;
	private final int STLDOC_INFO_VAT_INVOICE_DOC_NUM = 20005;
	private final int STLDOC_INFO_CANCEL_DOC_NUM = 20007;
	private final int STLDOC_INFO_CANCEL_VAT_NUM = 20008;
	private final int EVENT_INFO_TAX_RATE_NAME = 20002;
	private final int EVENT_INFO_FX_RATE = 20005;
	private final String METAL_CCY_LIST = "53, 54, 55, 56, 58, 61, 62, 63";
	private final String CCY_LIST = "0,51,52,57";
	
	@Override
	public void execute(IContainerContext arg0) throws OException {
		Table returnt = arg0.getReturnTable();
		Table argt = arg0.getArgumentsTable();
		
		try {
			Logging.init(this.getClass(), "", "");
			Logging.info("Report started");
			
			if (argt == null) 
			{
				throw new OException ("Unable to get argument details");
			}
			
			int mode = argt.getInt("ModeFlag", 1);
			
			Logging.info("Mode is: " + mode);
			/* Meta data collection */
			if (mode == 0) 
			{
				Logging.info ("Collecting meta-data");
				setOutputFormat(returnt);
				return;
			}
			
			//Get Plugin Parameters
			Table tblParameter = argt.getTable("PluginParameters", 1);
			String fromDate = tblParameter.getString("parameter_value", tblParameter.unsortedFindString("parameter_name", "fromDate", SEARCH_CASE_ENUM.CASE_INSENSITIVE));
			String toDate = tblParameter.getString("parameter_value", tblParameter.unsortedFindString("parameter_name", "toDate", SEARCH_CASE_ENUM.CASE_INSENSITIVE));

			//Run SQLs to get data. 
			Logging.info("Getting data for the dates " + fromDate + " and " + toDate);
			
			String sql = "\n WITH output AS "
					   + "\n ( SELECT 'B005' as entity_code"
					   + "\n          ,getdate() as report_date"
					   //+ "\n          ,'GF' as tax_code"
					   + "\n          ,CASE WHEN ab.toolset = " + TOOLSET_ENUM.CASH_TOOLSET.toInt() + " THEN "
					   + "\n                     CASE WHEN ab.cflow_type = " + MANUAL_VAT_CFLOW_TYPE + " THEN 'GF'"
				       + "\n                          WHEN ab.cflow_type = " + VAT_CFLOW_TYPE + " THEN 'GF'"
					   + "\n                          WHEN ab.cflow_type = " + TRANSPORTATION_CFLOW_TYPE + " THEN 'GF'"
					   + "\n                          WHEN ab.cflow_type = " + TRANSFER_CHARGE_CFLOW_TYPE + " THEN 'GF'"
					   + "\n                          ELSE 'N/A'"
					   + "\n                     END"
					   + "\n                ELSE CASE WHEN ab.buy_sell = " + BUY_SELL_ENUM.BUY.toInt() + " THEN "
					   + "\n                               CASE WHEN ab.toolset IN (" + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + "," + TOOLSET_ENUM.FX_TOOLSET.toInt() + ") THEN "
					   + "\n                                         CASE WHEN pa.country != 20077 THEN 'GA'"
					   + "\n                                              ELSE CASE WHEN ISNULL (pif.value, 'No') = 'Yes' THEN 'GC'"
					   + "\n                                                        ELSE CASE WHEN ISNULL(abtei.value, 'TBD') = 'No Tax' THEN 'GD'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'Reverse Charge Gold +' THEN 'GR'"
				       + "\n                                                             END"
				       + "\n                                                   END"
				       + "\n                                         END"
				       + "\n                               END"
					   + "\n                          WHEN ab.buy_sell = " + BUY_SELL_ENUM.SELL.toInt()+ " THEN "
					   + "\n                               CASE WHEN ISNULL(abtei.value, '') = 'Zero Rated' THEN 'G0'"
				       + "\n                                    WHEN ISNULL(abtei.value, '') = 'UK Std Tax' THEN 'G5'"
				       + "\n                                    WHEN ISNULL(abtei.value, '') = 'EU Zero Rated' THEN 'G2'"
				       + "\n                                    WHEN ISNULL(abtei.value, '') = 'No Tax' THEN 'G4'"
				       + "\n                                    WHEN ISNULL(abtei.value, '') = '+Reverse Charge Gold' THEN 'RG'"
				       + "\n                                    ELSE ''"
				       + "\n                               END"
					   + "\n                          ELSE 'N/A'"
				       + "\n                     END"
					   + "\n           END  as tax_code"
					   + "\n          ,sdd.para_position as net_amount"  
					   + "\n          ,sdd_tax.para_position as tax_amount"
					   + "\n          ,ISNULL(abtei.value, 'TBD') as tax_rate"
					   + "\n          ,CASE WHEN ab.buy_sell = " + BUY_SELL_ENUM.BUY.toInt() + " THEN 'Metal Buy' "
					   + "\n                WHEN ab.buy_sell = " + BUY_SELL_ENUM.SELL.toInt()+ " THEN 'Metal Sell'"
					   + "\n           END as tran_type"
					   + "\n          ,CASE WHEN ab.cflow_type = " + MANUAL_VAT_CFLOW_TYPE + " THEN ISNULL(abtiv.value,'')"
					   + "\n                ELSE ISNULL(di.value,' ') "
					   + "\n           END as invoice_id" 
					   + "\n          ,abte.event_date as invoice_date"
					   + "\n          ,p.short_name as ext_lentity"
					   + "\n          ,c.name as country"
					   + "\n          ,ab.deal_tracking_num"
					   + "\n          ,ab.tran_num"
					   + "\n          ,abte.event_num"
					   + "\n          ,CASE WHEN ab.buy_sell = 0 THEN 'Buy' "
					   + "\n                WHEN ab.buy_sell = 1 THEN 'Sell'"
					   + "\n           END as buy_sell"
					   + "\n          ,ts.name as toolset"
					   + "\n          ,ISNULL(pif.value, 'No') as jm_group"
					   + "\n          ,ccy.name as ccy"
					   + "\n          ,CAST (ISNULL(abtei_fx.value,'1.00') AS DECIMAL (12,6)) as fx_rate "
					   + "\n          ,ab.external_lentity as ext_lentity_id"
					   + "\n          ,cflow.name as cflow"
					   + "\n   FROM ab_tran ab"
					   + "\n        INNER JOIN toolsets ts ON ts.id_number = ab.toolset"
					   + "\n        INNER JOIN party_address pa ON pa.party_id = ab.external_lentity "
					   + "\n                                    AND pa.address_type = " + MAIN_ADDRESS_TYPE
					   + "\n        INNER JOIN country c ON c.id_number = pa.country"
					   + "\n        INNER JOIN party p ON p.party_id = ab.external_lentity"
					   + "\n        INNER JOIN cflow_type cflow ON cflow.id_number = ab.cflow_type"
					   + "\n        INNER JOIN ab_tran_event abte ON abte.tran_num = ab.tran_num "
					   + "\n                                      AND abte.event_type = 14"
					   + "\n                                      AND abte.event_date >= '" + fromDate + "'"
					   + "\n                                      AND abte.event_date <= '" + toDate + "'"
					   + "\n                                      AND (    (abte.currency IN (" + CCY_LIST + ") AND ab.toolset in (6, 10, 36))"
					   + "\n                                           OR  (abte.currency IN (" + METAL_CCY_LIST + ") AND ab.toolset in (9,15))"
					   + "\n                                           )"
					   + "\n        INNER JOIN stldoc_details sdd ON sdd.tran_num = ab.tran_num "
					   + "\n                                      AND sdd.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt()
					   + "\n                                      AND sdd.settle_ccy in (0,51,52,57)"
					   //+ "\n        INNER JOIN ab_tran_event abte_tax ON abte_tax.event_num = sdd.event_num"
					   + "\n        INNER JOIN currency ccy ON ccy.id_number = sdd.settle_ccy "
					   + "\n        INNER JOIN stldoc_header sdh ON sdh.document_num = sdd.document_num "
					   + "\n                                     AND sdh.doc_version = sdd.doc_version "
					   + "\n                                     AND sdh.doc_type = " + STLDOC_TYPE_INVOICE
					   + "\n        LEFT OUTER JOIN party_info_view pif ON pa.party_id = pif.party_id "
					   + "\n                                            AND pif.type_name = 'JM Group' "
					   + "\n        LEFT OUTER JOIN ab_tran_event_info abtei ON abtei.event_num = sdd.event_num  "
					   + "\n                                                 AND abtei.type_id = " + EVENT_INFO_TAX_RATE_NAME
					   + "\n        LEFT OUTER JOIN ab_tran_info_view abtiv ON abtiv.tran_num = ab.tran_num "
					   + "\n                                                AND  abtiv.type_name = 'Customer Invoice Number'"
					   + "\n        LEFT OUTER JOIN ab_tran_event_info abtei_fx ON abtei_fx.event_num = sdd.event_num  "
					   + "\n                                                    AND abtei_fx.type_id = " + EVENT_INFO_FX_RATE
					   + "\n        LEFT OUTER JOIN stldoc_info di ON di.document_num = sdd.document_num "
					   + "\n                                       AND  di.type_id IN ("    + STLDOC_INFO_OUR_DOC_NUM 
					   + "\n                                                             ," + STLDOC_INFO_VAT_INVOICE_DOC_NUM
					   + "\n                                                             ," + STLDOC_INFO_CANCEL_DOC_NUM
					   + "\n                                                             ," + STLDOC_INFO_CANCEL_VAT_NUM 
					   + "\n                                                          )"
					   + "\n        LEFT OUTER JOIN stldoc_details sdd_tax ON sdd_tax.tran_num = ab.tran_num "
					   + "\n                                               AND sdd.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt()
					   + "\n                                               AND sdd_tax.document_num = sdd.document_num"
					   + "\n   WHERE ab.internal_bunit = " + JM_UK_BU
					   + "\n   AND   ab.trade_flag = 1"
					   + "\n   AND   ab.tran_type not in (27, 41, 45)"
					   + "\n   AND   ab.asset_type = 2"
					   + "\n   AND   ab.tran_status IN (" + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ", " + TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() + ")"
					   + "\n ) "
					   + "\n SELECT * FROM output"
					   + "\n WHERE len(invoice_id) > 0";
			
			Logging.info(sql);
			DBaseTable.execISql(returnt, sql);
			
			//returnt.viewTable();
		} catch (Exception e) {
			Logging.error(e.getMessage());
			throw new OException(e.getMessage());			
		} finally {		
			Logging.info("Report completed");
			Logging.close();
		}
	}

	protected void setOutputFormat(Table output) throws OException 
	{
		output.addCol("entity_code", COL_TYPE_ENUM.COL_STRING);
		output.addCol("report_date", COL_TYPE_ENUM.COL_DATE_TIME);
		output.addCol("tax_code", COL_TYPE_ENUM.COL_STRING);
		output.addCol("net_amount", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("tax_amount", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("tax_rate", COL_TYPE_ENUM.COL_STRING);
		output.addCol("tran_type", COL_TYPE_ENUM.COL_STRING);
		output.addCol("invoice_id", COL_TYPE_ENUM.COL_STRING);
		output.addCol("invoice_date", COL_TYPE_ENUM.COL_DATE_TIME);
		output.addCol("ext_lentity", COL_TYPE_ENUM.COL_STRING);
		output.addCol("country", COL_TYPE_ENUM.COL_STRING);
		output.addCol("deal_tracking_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("event_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("buy_sell", COL_TYPE_ENUM.COL_STRING);
		output.addCol("toolset", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jm_group", COL_TYPE_ENUM.COL_STRING);
		output.addCol("ccy", COL_TYPE_ENUM.COL_STRING);
		output.addCol("fx_rate", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("ext_lentity_id", COL_TYPE_ENUM.COL_INT);
		output.addCol("cflow", COL_TYPE_ENUM.COL_STRING);

	}
}
