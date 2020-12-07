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

	private final String METAL_CCY_LIST = "53, 54, 55, 56, 58, 61, 62, 63";
	private final String CCY_LIST = "0,51,52,57";
	private final String DOC_TYPES_LIST = "'Our Doc Num', 'VAT Invoice Doc Num', 'Cancellation Doc Num', 'Cancellation VAT Num'";
	
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
					   + "\n          ,CASE WHEN ab.toolset = " + TOOLSET_ENUM.CASH_TOOLSET.toInt() + " THEN "
					   + "\n                     CASE WHEN cflow.name IN ('Manual VAT' ,'VAT', 'Transportation','Transfer Charge') THEN 'GF'"
					   + "\n                          WHEN cflow.name LIKE 'Metal Rentals - %' THEN "
					   + "\n                               CASE WHEN ISNULL (pif.value, 'No') = 'Yes' THEN "
					   + "\n                                         CASE WHEN ab.buy_sell = " + BUY_SELL_ENUM.SELL.toInt()+ " THEN 'GC'"
					   + "\n                                              WHEN ab.buy_sell = " + BUY_SELL_ENUM.BUY.toInt() + " THEN 'G8'"  
					   + "\n                                         END"
					   + "\n                                    ELSE CASE WHEN (pa.country != 20077 AND ISNULL(abtei.value, 'TBD') = 'TBD') THEN 'GA'"
					   + "\n                                         END"
					   + "\n                               END"
					   + "\n                          ELSE CASE WHEN ab.buy_sell = " + BUY_SELL_ENUM.BUY.toInt() + " THEN "
					   + "\n                                         CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G0'"
					   + "\n                                              WHEN ISNULL(abtei.value, 'TBD') = 'EU Zero Rated' THEN 'G2'"
					   + "\n                                              WHEN ISNULL(abtei.value, 'TBD') = 'UK Std Tax' THEN 'G5'"
					   + "\n                                              WHEN ISNULL(abtei.value, 'TBD') = 'No Tax' THEN 'G4'"
				       + "\n                                              ELSE CASE WHEN (pa.country = 20077 AND ISNULL(abtei.value, 'TBD') = 'TBD') THEN 'GD'"
				       + "\n                                                   END"
				       + "\n                                         END"
					   + "\n                                    WHEN ab.buy_sell = " + BUY_SELL_ENUM.SELL.toInt() + " THEN "
					   + "\n                                         CASE WHEN (pa.country != 20077 AND ISNULL(abtei.value, 'TBD') = 'TBD') THEN 'GA'"
					   + "\n                                         END"
				       + "\n                               END"
					   + "\n                     END"
					   + "\n                ELSE CASE WHEN ab.buy_sell = " + BUY_SELL_ENUM.BUY.toInt() + " THEN "
					   + "\n                               CASE WHEN ab.toolset IN (" + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + "," + TOOLSET_ENUM.FX_TOOLSET.toInt() + ") THEN "
					   + "\n                                         CASE WHEN pa.country != 20077 THEN 'GA'"
					   + "\n                                              ELSE CASE WHEN ISNULL (pif.value, 'No') = 'Yes' THEN 'GC'"
					   + "\n                                                        ELSE CASE WHEN ISNULL(abtei.value, 'TBD') = 'No Tax' THEN 'GD'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'Reverse Charge Gold +' THEN 'GR'" 
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'GD'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'GD'"
				       + "\n                                                             END"
				       + "\n                                                   END"
				       + "\n                                         END"
				       + "\n                                    WHEN ab.toolset IN ("  + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + "," + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() + ") THEN "
				       + "\n                                         CASE WHEN pa.country != 20077 THEN 'GT'"
					   + "\n                                              ELSE CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'GQ'"
					   + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'UK Std Tax' THEN 'GX'"
				       + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'GD'"
				       + "\n                                                   END"
				       + "\n                                         END"
				       + "\n                               END"
					   + "\n                          WHEN ab.buy_sell = " + BUY_SELL_ENUM.SELL.toInt()+ " THEN "
					   + "\n                               CASE WHEN ISNULL(abtei.value, '') = '' THEN "
					   + "\n                                         CASE WHEN pa.country != 20077 THEN 'G0'"
					   + "\n                                              ELSE CASE WHEN ISNULL (pif.value, 'No') = 'Yes' THEN 'G8'"
					   + "\n                                                        ELSE 'G2'"
					   + "\n                                                   END"
					   + "\n                                         END"
					   + "\n                                    ELSE CASE WHEN abtei.value = 'Zero Rated' THEN 'G0'"
				       + "\n                                              WHEN abtei.value = 'UK Std Tax' THEN 'G5'"
				       + "\n                                              WHEN abtei.value = 'EU Zero Rated' THEN 'G2'"
				       + "\n                                              WHEN abtei.value = 'No Tax' THEN 'G4'"
				       + "\n                                              WHEN abtei.value = '+Reverse Charge Gold' THEN 'RG'"
				       + "\n                                              ELSE ''"
				       + "\n                                         END"
				       + "\n                               END"
					   + "\n                          ELSE ' '"
				       + "\n                     END"
					   + "\n           END  as tax_code"
					   + "\n          ,CASE WHEN cflow.name = 'Manual VAT' THEN (sdd.para_position * 5.0) ELSE  sdd.para_position END as net_amount"  
					   + "\n          ,CASE WHEN cflow.name = 'Manual VAT' THEN sdd.para_position ELSE sdd_tax.para_position END as tax_amount"
					   + "\n          ,ISNULL(abtei.value, 'TBD') as tax_rate"
					   + "\n          ,CASE WHEN ab.buy_sell = " + BUY_SELL_ENUM.BUY.toInt() + " THEN 'Metal Buy' "
					   + "\n                WHEN ab.buy_sell = " + BUY_SELL_ENUM.SELL.toInt()+ " THEN 'Metal Sell'"
					   + "\n           END as tran_type"
					   + "\n          ,CASE WHEN cflow.name = 'Manual VAT' THEN ISNULL(abtiv.value,'')"
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
					   + "\n          ,ab.toolset as toolset_id"
					   + "\n   FROM ab_tran ab"
					   + "\n        INNER JOIN party pint ON pint.party_id = ab.internal_bunit"
					   + "\n                              AND pint.short_name = 'JM PMM UK'"
					   + "\n        INNER JOIN toolsets ts ON ts.id_number = ab.toolset"
					   + "\n        INNER JOIN party_address pa ON pa.party_id = ab.external_lentity "
					   + "\n                                    AND pa.address_type = (SELECT pat.address_type_id"
					   + "\n                                                           FROM  party_address_type pat"
					   + "\n                                                           WHERE pat.address_type_name = 'Main'"
					   + "\n                                                          )"  
					   + "\n        INNER JOIN country c ON c.id_number = pa.country"
					   + "\n        INNER JOIN party p ON p.party_id = ab.external_lentity"
					   + "\n                           AND p.int_ext = 1"
					   + "\n        INNER JOIN cflow_type cflow ON cflow.id_number = ab.cflow_type"
					   + "\n        INNER JOIN ab_tran_event abte ON abte.tran_num = ab.tran_num "
					   + "\n                                      AND abte.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt()
					   + "\n                                      AND abte.event_date >= '" + fromDate + "'"
					   + "\n                                      AND abte.event_date <= '" + toDate + "'"
					   + "\n                                      AND (    (abte.currency IN (" + CCY_LIST + ") AND ab.toolset in (6, 10, 36))"
					   + "\n                                           OR  (abte.currency IN (" + METAL_CCY_LIST + ") AND ab.toolset in (9,15))"
					   + "\n                                           )"
					   + "\n        INNER JOIN stldoc_details sdd ON sdd.tran_num = ab.tran_num "
					   + "\n                                      AND sdd.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt()
					   + "\n                                      AND sdd.settle_ccy IN (" + CCY_LIST + ")"
					   + "\n        INNER JOIN currency ccy ON ccy.id_number = sdd.settle_ccy "
					   + "\n        INNER JOIN stldoc_header sdh ON sdh.document_num = sdd.document_num "
					   + "\n                                     AND sdh.doc_version = sdd.doc_version "
					   + "\n                                     AND sdh.doc_type IN (SELECT sdt.doc_type"
					   + "\n                                                          FROM stldoc_document_type sdt"
					   + "\n                                                          WHERE sdt.doc_type_desc = 'Invoice'"
					   + "\n                                                         )"  
					   + "\n        LEFT OUTER JOIN party_info_view pif ON pa.party_id = pif.party_id "
					   + "\n                                            AND pif.type_name = 'JM Group' "
					   + "\n        LEFT OUTER JOIN ab_tran_event_info abtei ON abtei.event_num = sdd.event_num  "
					   + "\n                                                 AND abtei.type_id = (SELECT teinfo.type_id"
					   + "\n                                                                      FROM tran_event_info_types teinfo"
					   + "\n                                                                      WHERE teinfo.type_name = 'Tax Rate Name'"
					   + "\n                                                                     )"
					   + "\n        LEFT OUTER JOIN ab_tran_info_view abtiv ON abtiv.tran_num = ab.tran_num "
					   + "\n                                                AND  abtiv.type_name = 'Customer Invoice Number'"
					   + "\n        LEFT OUTER JOIN ab_tran_event_info abtei_fx ON abtei_fx.event_num = sdd.event_num  "
					   + "\n                                                    AND abtei_fx.type_id  = (SELECT teinfo_fx.type_id"
					   + "\n                                                                             FROM tran_event_info_types teinfo_fx"
					   + "\n                                                                             WHERE teinfo_fx.type_name = 'FX Rate'"
					   + "\n                                                                            )"
					   + "\n        LEFT OUTER JOIN stldoc_info di ON di.document_num = sdd.document_num "
					   + "\n                                       AND  di.type_id IN (SELECT sdoc_types.type_id "
					   + "\n                                                           FROM stldoc_info_types sdoc_types"
					   + "\n                                                           WHERE sdoc_types.type_name in (" + DOC_TYPES_LIST + ")"
					   + "\n                                                          )"
					   + "\n        LEFT OUTER JOIN stldoc_details sdd_tax ON sdd_tax.tran_num = ab.tran_num "
					   + "\n                                               AND sdd_tax.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt()
					   + "\n                                               AND sdd_tax.document_num = sdd.document_num"
					   + "\n   WHERE ab.tran_status IN (" + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ", " + TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() + ")"
					   + "\n )"
					   //Pick records only where invoice number is available. Add amounts for GBP.
					   + "\n , valid_invoice AS (SELECT o.*"
					   + "\n                           ,o.net_amount * fx_rate as net_amount_gbp "
					   + "\n                           ,o.tax_amount * fx_rate as tax_amount_gbp "
					   + "\n                     FROM output o"
					   + "\n                     WHERE len(invoice_id) > 0"
					   + "\n                    )"
					   //Ignore Buy side, non-Cash toolset rows that have external legal entity already in Manual VAT
					   + "\n , non_manual_vat_buy AS (SELECT * "
					   + "\n                          FROM  valid_invoice  "
					   + "\n                          WHERE toolset_id != " + TOOLSET_ENUM.CASH_TOOLSET.toInt()
					   + "\n                          AND buy_sell = 'Buy'"
					   + "\n                          AND ext_lentity_id NOT IN (SELECT DISTINCT ext_lentity_id "
					   + "\n                                                     FROM  valid_invoice"
					   + "\n                                                     WHERE cflow = 'Manual VAT' "
					   + "\n                                                     AND toolset_id = " + TOOLSET_ENUM.CASH_TOOLSET.toInt()
					   + "\n                                                    )"
					   + "\n                         )"
					   //Collect the data for display. Start with non Cash side, Buy records
					   + "\n SELECT * FROM non_manual_vat_buy"
					   + "\n UNION"
					   //Get the remaining buys. We have to use Buy here as Cash toolset has Sell records as well
					   + "\n SELECT * FROM valid_invoice WHERE buy_sell = 'Buy' AND toolset_id = " + TOOLSET_ENUM.CASH_TOOLSET.toInt() 
					   + "\n UNION"
					   //Add the Sells
					   + "\n SELECT * FROM valid_invoice WHERE buy_sell = 'Sell'"
                       + "\n ORDER BY invoice_id"

					   
					   ;
			
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
		output.addCol("toolset_id", COL_TYPE_ENUM.COL_INT);
		output.addCol("net_amount_gbp", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("tax_amount_gbp", COL_TYPE_ENUM.COL_DOUBLE);

	}
}
