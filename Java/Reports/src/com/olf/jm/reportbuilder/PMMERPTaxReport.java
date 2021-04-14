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

	//Add Precious Metals to a list- XAG 53, XAU 54, XPD 55, XPT 56, XRH 58, XIR 61, XOS 62, XRU 63
	private final String METAL_CCY_LIST = "53, 54, 55, 56, 58, 61, 62, 63";
	
	//Add USD, EUR 51, GBP 52 and ZAR 57 to another list 
	private final String CCY_LIST = "0,51,52,57";
	
	//List of document types that are considered in the report
	private final String DOC_TYPES_OUR = "'Our Doc Num', 'Cancellation Doc Num'";
	private final String DOC_TYPES_VAT = "'VAT Invoice Doc Num', 'Cancellation VAT Num'";
	
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
			
			//Get Plugin Parameters. User will input From and To dates.
			Table tblParameter = argt.getTable("PluginParameters", 1);
			String fromDate = tblParameter.getString("parameter_value", tblParameter.unsortedFindString("parameter_name", "fromDate", SEARCH_CASE_ENUM.CASE_INSENSITIVE));
			String toDate = tblParameter.getString("parameter_value", tblParameter.unsortedFindString("parameter_name", "toDate", SEARCH_CASE_ENUM.CASE_INSENSITIVE));

			//Run SQLs to get data. 
			Logging.info("Getting data for the dates " + fromDate + " and " + toDate);
			
			String sql = "\n WITH output AS "
					   + "\n ( SELECT "
					   + "\n          'B005' as entity_code"
					   + "\n          ,getdate() as report_date"
					   //Get tax codes for Cash deals 
					   // GF when cashflow is either Manual VAT, VAT, Transportation or Transfer Charge
					   //    For Metal Rentals - if JM Group then GC if a sell deal, G8 otherwise
					   //                      - For non JM Group, if country of residence is not UK and tax rate not defined then GA
					   // Other cashflows we will check the operations.
					   //    - If Buy then depending on tax rate we will get tax code. If tax rate not defined then if UK is country of the LE then GD else G0.
					   //    - If Sell then if tax rate not defined and country of LE is not UK then GA, G0 otherwise.
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
				       + "\n                                              WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN "
					   + "\n                                                   CASE WHEN pa.country = 20077 THEN  'GD'"
				       + "\n                                                        ELSE 'G0'"
				       + "\n                                                   END"
				       + "\n                                         END"
					   + "\n                                    WHEN ab.buy_sell = " + BUY_SELL_ENUM.SELL.toInt() + " THEN "
					   + "\n                                         CASE WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN "
					   + "\n                                                   CASE WHEN pa.country != 20077 THEN 'GA'"
					   + "\n                                                        ELSE 'G0'"
					   + "\n                                                   END"
					   + "\n                                              WHEN ISNULL(abtei.value, 'TBD') = 'No Tax' THEN 'G1'"
				       + "\n                                              ELSE ''"
					   + "\n                                         END"
				       + "\n                               END"
					   + "\n                     END"
					   //Get tax code for non-Cash deals. Here again driver is the operation
					   //If Buy - If FX and ComSwap
					   //            -- If country of LE is not UK then GA.
					   //            -- If part of JM Group then GC
					   //            -- For different tax rates, set to different tax codes
					   //       - If Commodity or Loan Deposit
					   //            -- If country of LE is not UK then GT
					   //            -- For different tax rates, set to different tax codes
					   //If Sell - If no tax rate defined, country of LE is not UK then G0. If part of JM Group then G8. For other cases, G2.
					   //        - If a tax rate is defined, then set to different tax code depending on the rate defined.
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
				       //Net Amount
					   //Deals with cashflow VAT and Manual VAT have the tax as amount on the deal. To get Net amount we need to multiply by 5 as the rate assumed is 20%.
				       //For cancelled invoices we need to reverse the sign to show reversal
					   //Position is obtained from stldoc_details table. If an entry is not present here, we get from historical data. Endur removes entry from this table if a deal is cancelled.
					   + "\n          ,CASE WHEN cflow.name IN ('VAT', 'Manual VAT') THEN "
					   + "\n                     CASE WHEN ISNULL(sdit.type_name,ISNULL(sdith.type_name,' ')) IN ('Cancellation VAT Num', 'Cancellation Doc Num') THEN (-1.0 * ISNULL(sdd.para_position,ISNULL(sddh.para_position,0.00)) * 5.0) "
					   + "\n                          ELSE (ISNULL(sdd.para_position, ISNULL(sddh.para_position,0.00)) * 5.0) "
					   + "\n                     END"
					   + "\n                ELSE CASE WHEN ISNULL(sdit.type_name,ISNULL(sdith.type_name,' ')) IN ('Cancellation VAT Num', 'Cancellation Doc Num') THEN ( -1.0 * ISNULL(sdd.para_position,ISNULL(sddh.para_position,0.00))) "
					   + "\n                          ELSE  ISNULL(sdd.para_position, ISNULL(sddh.para_position,0.00)) "
					   + "\n                     END"
					   + "\n           END as net_amount"  
					   //Tax Amount
					   //As mentioned above, tax amount is already on deals with cashflows VAT and Manual VAT. For other deals, we need to get this from the Tax Settlement event
					   //As the case with net amount, reverse the sign for cancelled invoices. Also same reasoning for getting data from history table.
					   + "\n          ,CASE WHEN cflow.name IN ('VAT', 'Manual VAT') THEN "
					   + "\n                     CASE WHEN ISNULL(sdit.type_name,ISNULL(sdith.type_name,' ')) IN ('Cancellation VAT Num', 'Cancellation Doc Num') THEN (-1.0 * ISNULL(sdd.para_position, ISNULL(sddh.para_position,0.00))) "
					   + "\n                          ELSE ISNULL(sdd.para_position, ISNULL(sddh.para_position, 0.00)) "
					   + "\n                     END"
					   + "\n                ELSE CASE WHEN ISNULL(sdit.type_name,ISNULL(sdith.type_name,' ')) IN ('Cancellation VAT Num', 'Cancellation Doc Num') THEN ( -1.0 * ISNULL(sdd_tax.para_position, ISNULL(sdd_tax_hist.para_position,0.00))) "
					   + "\n                          ELSE  ISNULL(sdd_tax.para_position, ISNULL(sdd_tax_hist.para_position,0.00)) "
					   + "\n                     END "
					   + "\n           END as tax_amount"
					   //Tax Rate - Added in Event Infos
					   + "\n          ,ISNULL(abtei.value, 'TBD') as tax_rate"
					   //Tran Type - Metal Buy for a buy deal, Metal Sell otherwise
					   + "\n          ,CASE WHEN ab.buy_sell = " + BUY_SELL_ENUM.BUY.toInt() + " THEN 'Metal Buy' "
					   + "\n                WHEN ab.buy_sell = " + BUY_SELL_ENUM.SELL.toInt()+ " THEN 'Metal Sell'"
					   + "\n           END as tran_type"
					   //Invoice Number
					   //For Manual VAT cashflow deals, we have created a new tran info and store value in it. Obtain value from the tran info.
					   //For others, obtain from stldoc_info. If not here, look into history table.
					   + "\n          ,CASE WHEN cflow.name = 'Manual VAT' THEN ISNULL(abtiv.value,'')"
					   + "\n                ELSE ISNULL(di.value,ISNULL(sdih.value,''))"
					   + "\n           END as invoice_id" 
					   //Invoice Date
					   ///For Cash deals, it's trade date. For Commodity and Loan Depot, it's date on the invoice. For all others, use event dates.
					   + "\n          ,CASE WHEN ab.toolset = " + TOOLSET_ENUM.CASH_TOOLSET.toInt() + " THEN ab.trade_date"
					   + "\n                WHEN ab.toolset IN ("  + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + "," + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() + ") THEN ISNULL(di.last_update, sdih.last_update)"
					   + "\n                ELSE abte.event_date "
					   + "\n           END as invoice_date"
					   //External Legal Entity
					   + "\n          ,p.short_name as ext_lentity"
					   + "\n          ,c.name as country"
					   + "\n          ,ab.deal_tracking_num"
					   + "\n          ,ab.tran_num"
					   + "\n          ,abte.event_num"
					   + "\n          ,CASE WHEN ab.buy_sell = 0 THEN 'Buy' "
					   + "\n                WHEN ab.buy_sell = 1 THEN 'Sell'"
					   + "\n           END as buy_sell"
					   + "\n          ,ts.name as toolset"
					   //JM Group from party info
					   + "\n          ,ISNULL(pif.value, 'No') as jm_group"
					   + "\n          ,ISNULL(ccy.name, ccy_hist.name) as ccy"
					   + "\n          ,CAST (ISNULL(abtei_fx.value,'1.00') AS DECIMAL (12,6)) as fx_rate "
					   + "\n          ,ab.external_lentity as ext_lentity_id" 	
					   + "\n          ,cflow.name as cflow"
					   + "\n          ,ab.toolset as toolset_id"
					   + "\n   FROM ab_tran ab"
					   + "\n        INNER JOIN party pint ON pint.party_id = ab.internal_bunit"
					   + "\n                              AND pint.short_name = 'JM PMM UK'"
					   + "\n        INNER JOIN toolsets ts ON ts.id_number = ab.toolset"
					   //Check main address of the external LE. This will be used to get tax codes.
					   + "\n        INNER JOIN party_address pa ON pa.party_id = ab.external_lentity "
					   + "\n                                    AND pa.address_type = (SELECT pat.address_type_id"
					   + "\n                                                           FROM  party_address_type pat"
					   + "\n                                                           WHERE pat.address_type_name = 'Main'"
					   + "\n                                                          )"  
					   + "\n        INNER JOIN country c ON c.id_number = pa.country"
					   + "\n        INNER JOIN party p ON p.party_id = ab.external_lentity"
					   + "\n                           AND p.int_ext = 1"
					   //Ignore UK IN-TRANSIT LE party 
					   + "\n                           AND p.party_id != 20770"
					   + "\n        INNER JOIN cflow_type cflow ON cflow.id_number = ab.cflow_type"
					   //For Cash deals, check the trade dates are in the range specified. 
					   //For FX and ComSwap, the dates specified are for a Metal Ccy. But pick up record for std ccy whenever it's settle date is.
					   + "\n        INNER JOIN ab_tran_event abte ON abte.tran_num = ab.tran_num "
					   + "\n                                      AND abte.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt()	
					   + "\n                                      AND abte.currency IN (" + CCY_LIST + ")"			   
					   + "\n                                      AND (    (ab.toolset IN (" + TOOLSET_ENUM.FX_TOOLSET.toInt() 
					   + "\n                                                             , " + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + ")"
					   + "\n                                                AND abte.currency IN (" + CCY_LIST + ")"
					   + "\n                                                AND abte.event_date >= '" + fromDate + "'"
					   + "\n                                                AND EXISTS (SELECT 1 "
					   + "\n                                                            FROM ab_tran_event abte_metal"
					   + "\n                                                            WHERE abte_metal.tran_num = ab.tran_num"
					   + "\n                                                            AND   abte_metal.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt()		
					   + "\n                                                            AND   abte_metal.currency IN (" + METAL_CCY_LIST + ")"
					   + "\n                                                            AND   abte_metal.event_date >= '" + fromDate + "'"
					   + "\n                                                            AND   abte_metal.event_date <= '" + toDate + "'"
					   + "\n                                                           )"
					   + "\n                                               )"
					   + "\n                                           OR  (ab.toolset IN  (" + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() 
					   + "\n                                                              , " + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + ")"
					   + "\n                                                AND   abte.tran_num = ab.tran_num"
				       + "\n                                               )"
				       //For cash deals, just include everything. We will later check in stldoc_header table.
					   + "\n                                           OR  (ab.toolset = " + TOOLSET_ENUM.CASH_TOOLSET.toInt()
					   //+ "\n                                                AND ab.trade_date >= '" + fromDate + "'"
					   //+ "\n                                                AND ab.trade_date <= '" + toDate + "'"
					   + "\n                                               )"
					   + "\n                                          )"
					   //Join with stldoc_details with cash event type and normal currencies. An outer join is required here as we need to look into history tables later.
					   + "\n        LEFT OUTER JOIN stldoc_details sdd ON sdd.tran_num = ab.tran_num "
					   + "\n                                      AND sdd.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt()
					   + "\n                                      AND sdd.settle_ccy IN (" + CCY_LIST + ")"
				       + "\n                                      AND sdd.event_num = abte.event_num"
					   + "\n        LEFT OUTER JOIN currency ccy ON ccy.id_number = sdd.settle_ccy "
				       //Check only Invoices. Ignore invoices in status Approval Required
					   + "\n        INNER JOIN stldoc_header sdh ON sdh.document_num = sdd.document_num "
					   + "\n                                     AND sdh.doc_version = sdd.doc_version "
					   + "\n                                     AND sdh.doc_status != 15"
					   + "\n                                     AND sdh.doc_type IN (SELECT sdt.doc_type"
					   + "\n                                                          FROM stldoc_document_type sdt"
					   + "\n                                                          WHERE sdt.doc_type_desc = 'Invoice'"
					   + "\n                                                         )"  
				       //For cash deals, we check the date when invoice was issued. For other toolsets, we check in events above.
					   + "\n                                     AND ( (ab.toolset = 10 "  
					   + "\n                                            AND sdh.doc_issue_date >= '" + fromDate + "'"
					   + "\n                                            AND sdh.doc_issue_date <= '" + toDate + "'"
					   + "\n                                           )"
					   + "\n                                           OR"
					   + "\n                                           (ab.toolset != 10) "
					   + "\n                                         )"
					   //Check if Ext LE is part of JM Group or not
					   + "\n        LEFT OUTER JOIN party_info_view pif ON pa.party_id = pif.party_id "
					   + "\n                                            AND pif.type_name = 'JM Group' "
					   //Tax rates are stored on event infos
					   + "\n        LEFT OUTER JOIN ab_tran_event_info abtei ON abtei.event_num = sdd.event_num  "
					   + "\n                                                 AND abtei.type_id = (SELECT teinfo.type_id"
					   + "\n                                                                      FROM tran_event_info_types teinfo"
					   + "\n                                                                      WHERE teinfo.type_name = 'Tax Rate Name'"
					   + "\n                                                                     )"
					   //New tran info for Manual VAT cashflow.
					   + "\n        LEFT OUTER JOIN ab_tran_info_view abtiv ON abtiv.tran_num = ab.tran_num "
					   + "\n                                                AND  abtiv.type_name = 'Customer Invoice Number'"
					   //FX Rate is also on event info
					   + "\n        LEFT OUTER JOIN ab_tran_event_info abtei_fx ON abtei_fx.event_num = sdd.event_num  "
					   + "\n                                                    AND abtei_fx.type_id  = (SELECT teinfo_fx.type_id"
					   + "\n                                                                             FROM tran_event_info_types teinfo_fx"
					   + "\n                                                                             WHERE teinfo_fx.type_name = 'FX Rate'"
					   + "\n                                                                            )"
					   //Get invoice documents. For Loan Depot and Commodity, make sure they were created within the time range specified
					   + "\n        LEFT OUTER JOIN stldoc_info di ON di.document_num = sdd.document_num "
					   + "\n                                       AND  (    (ab.toolset IN  (" + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() 
					   + "\n                                                                , " + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + ")"
					   + "\n                                                  AND di.last_update >= '" + fromDate + "'"
					   + "\n                                                  AND di.last_update <= '" + toDate + "'"
					   + "\n                                                 )"
					   + "\n                                              OR (ab.toolset IN  (" + TOOLSET_ENUM.CASH_TOOLSET.toInt()
					   + "\n                                                                , " + TOOLSET_ENUM.FX_TOOLSET.toInt() 
					   + "\n                                                                , " + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + ")"
					   + "\n                                                  AND 1=1"
					   + "\n                                                 )"
					   + "\n                                            )"
					   //If VAT invoice was generated, give it priority. 
					   + "\n                                       AND  ( di.type_id IN (SELECT sdoc_types.type_id "
					   + "\n                                                             FROM stldoc_info_types sdoc_types"
					   + "\n                                                             WHERE sdoc_types.type_name in (" + DOC_TYPES_VAT + ")"
					   + "\n                                                            )"
					   + "\n                                              OR  ( di.type_id IN (SELECT sdoc_types2.type_id "
					   + "\n                                                                   FROM stldoc_info_types sdoc_types2"
					   + "\n                                                                   WHERE sdoc_types2.type_name in (" + DOC_TYPES_OUR + ")"
				       + "\n                                                                  )"
					   + "\n                                                    AND NOT EXISTS (SELECT 1 FROM stldoc_info sdi2, stldoc_info_types sdoc_types3"
					   + "\n                                                                    WHERE sdi2.document_num = sdd.document_num and sdi2.type_id = sdoc_types3.type_id"
					   + "\n                                                                    AND   sdoc_types3.type_name in (" + DOC_TYPES_VAT + ")"
					   + "\n                                                                   )"
					   + "\n                                                  )"
					   + "\n                                            )"
				       + "\n        LEFT OUTER JOIN stldoc_info_types sdit ON sdit.type_id = di.type_id"
					   //Join to get tax numbers
					   + "\n        LEFT OUTER JOIN stldoc_details sdd_tax ON sdd_tax.tran_num = ab.tran_num "
					   + "\n                                               AND sdd_tax.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt()
					   + "\n                                               AND sdd_tax.document_num = sdd.document_num"
				       + "\n                                               AND (sdd.ins_seq_num = -1 OR sdd_tax.ins_seq_num = sdd.ins_seq_num)"
					   //We have to get documents that were cancelled and not present in the main tables.
				       //This happens when more than 1 deal were part of an invoice, one of the deal was later cancelled and invoice re-issued
					   //For Loan Depot and Commodity, invoices generated between the date range specified must be considered
					   + "\n        LEFT OUTER JOIN stldoc_details_hist sddh ON sddh.tran_num = ab.tran_num AND sddh.event_type = 14" 
					   + "\n        LEFT OUTER JOIN stldoc_info_h sdih ON sdih.document_num = sddh.document_num"
					   + "\n                                           AND  (    (ab.toolset IN  (" + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() 
					   + "\n                                                                , " + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + ")"
					   + "\n                                                      AND di.last_update >= '" + fromDate + "'"
					   + "\n                                                      AND di.last_update <= '" + toDate + "'"
					   + "\n                                                     )"
					   + "\n                                                 OR (ab.toolset IN  (" + TOOLSET_ENUM.CASH_TOOLSET.toInt()
					   + "\n                                                                   , " + TOOLSET_ENUM.FX_TOOLSET.toInt() 
					   + "\n                                                                   , " + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + ")"
					   + "\n                                                     AND 1=1"
					   + "\n                                                    )"
					   + "\n                                               )"
					   //Again give priority to VAT documents generated.
					   + "\n                                           AND  ( sdih.type_id IN (SELECT sdoc_types3.type_id "
					   + "\n                                                                 FROM stldoc_info_types sdoc_types3"
					   + "\n                                                                 WHERE sdoc_types3.type_name in (" + DOC_TYPES_VAT + ")"
					   + "\n                                                                )"
					   + "\n                                                  OR  ( sdih.type_id IN (SELECT sdoc_types4.type_id "
					   + "\n                                                                       FROM stldoc_info_types sdoc_types4"
					   + "\n                                                                       WHERE sdoc_types4.type_name in (" + DOC_TYPES_OUR + ")"
				       + "\n                                                                      )"
					   + "\n                                                        AND NOT EXISTS (SELECT 1 FROM stldoc_info_h sdih2, stldoc_info_types sdoc_types5"
					   + "\n                                                                        WHERE sdih2.document_num = sddh.document_num "
					   + "\n                                                                        AND   sdih2.type_id = sdoc_types5.type_id"
					   + "\n                                                                        AND   sdoc_types5.type_name in (" + DOC_TYPES_VAT + ")"
					   + "\n                                                                       )"
					   + "\n                                                      )"
					   + "\n                                                )"
					   //We need to make sure that the same document number is not present in the main table
					   + "\n                                           AND NOT EXISTS (SELECT 1 FROM stldoc_info sdi_main"
					   + "\n                                                           WHERE sdi_main.document_num = sdih.document_num"
					   + "\n                                                           AND   sdi_main.type_id = sdih.type_id)"
					   //Tax data from history table
					   + "\n        LEFT OUTER JOIN stldoc_details_hist sdd_tax_hist ON sdd_tax_hist.document_num = sddh.document_num "
					   + "\n                                                         AND sdd_tax_hist.tran_num = sddh.tran_num"
					   + "\n                                                         AND sdd_tax_hist.event_type = "  + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt()
					   + "\n        LEFT OUTER JOIN currency ccy_hist ON ccy_hist.id_number = sddh.settle_ccy"
					   + "\n        LEFT OUTER JOIN stldoc_info_types sdith ON sdith.type_id = sdih.type_id"
					   //Only Validated and Cancelled deals are to be considered
					   + "\n   WHERE ab.tran_status IN (" + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ", " + TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() + ")"
					   + "\n )"
					   //Pick records only where invoice number is available. Add amounts for GBP.
					   + "\n , valid_invoice AS (SELECT o.*"
					   + "\n                           ,o.net_amount * fx_rate as net_amount_gbp "
					   + "\n                           ,o.tax_amount * fx_rate as tax_amount_gbp "
					   + "\n                     FROM output o"
					   + "\n                     WHERE len(invoice_id) > 0"
					   + "\n                    )"
					   //Ignore Buy side, non-Cash and non Commodity toolset rows that have external legal entity already in Manual VAT
					   + "\n , non_manual_vat_buy AS (SELECT * "
					   + "\n                          FROM  valid_invoice  "
					   + "\n                          WHERE toolset_id NOT IN ( " + TOOLSET_ENUM.CASH_TOOLSET.toInt() 
					   + "\n                                                   ," + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + ")"
					   + "\n                          AND   buy_sell = 'Buy'"
					   + "\n                          AND   ext_lentity_id NOT IN (SELECT DISTINCT ext_lentity_id "
					   + "\n                                                       FROM  valid_invoice"
					   + "\n                                                       WHERE cflow = 'Manual VAT' "
					   + "\n                                                       AND toolset_id = " + TOOLSET_ENUM.CASH_TOOLSET.toInt()
					   + "\n                                                      )"
					   + "\n                          UNION"
					   + "\n                          SELECT * "
					   + "\n                          FROM  valid_invoice  "
					   + "\n                          WHERE toolset_id = " +  TOOLSET_ENUM.COMMODITY_TOOLSET.toInt()
					   + "\n                          AND   buy_sell = 'Buy'"
					   + "\n                         )"
					   //Collect the data for display. Start with non Cash side, Buy records
					   + "\n SELECT * FROM non_manual_vat_buy"
					   + "\n UNION"
					   //Get the remaining buys. We have to use Buy here as Cash toolset has Sell records as well
					   + "\n SELECT * FROM valid_invoice WHERE buy_sell = 'Buy' AND toolset_id = " + TOOLSET_ENUM.CASH_TOOLSET.toInt() 
					   + "\n UNION"
					   //Add the Sells
					   + "\n SELECT * FROM valid_invoice WHERE buy_sell = 'Sell'"
                       + "\n ORDER BY deal_tracking_num, tran_num, event_num";
			
			//To debug, uncomment below line and see the SQL in the log.
			//Logging.info(sql);
			DBaseTable.execISql(returnt, sql); 
			
			//This is to take unique records. But as of last testing there were no duplicates. Feel free to comment.
			returnt.makeTableUnique(); 
			
			//Below line for debugging purpose only.
			//returnt.viewTable();
		} catch (Exception e) {
			Logging.error(e.getMessage());
			throw new OException(e.getMessage());			
		} finally {		
			Logging.info("Report completed");
			Logging.close();
		}
	}

	/**
	 * This method is required for ReportBuilder to get meta data
	 * @param output The table that will hold the data to be displayed on ReportBuilder screen.
	 * @throws OException
	 */
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
