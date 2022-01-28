package com.olf.jm.reportbuilder;


import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 24-Nov-2020 |               | Giriraj Joshi   | Initial version.                                                                |
 * | 002 | 08-Mar-2021 |               | Giriraj Joshi   | EPI-1636. 2 bugs fixed.                                                         |
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

			//Get End of Month date for Metal Rental deals
			String eomStartDate = OCalendar.formatJd( OCalendar.getEOM(OCalendar.parseString(fromDate)), DATE_FORMAT.DATE_FORMAT_ISO8601);
			String taxRateToCheck = "Reverse Charge Gold +";
			
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
					   + "\n                     CASE WHEN ab.buy_sell = " + BUY_SELL_ENUM.BUY.toInt() + " THEN "
					   + "\n                               CASE WHEN cflow.name LIKE 'Metal Rentals - %' THEN 'G8'" //For all Buy Metal Rentals code is GC
					   + "\n                                    ELSE CASE WHEN pa.country = 20077 THEN " //Country is UK
					   + "\n                                                   CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G3'" 
					   + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'UK Std Tax' THEN 'G5'" 
					   + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN " 
					   + "\n                                                             CASE WHEN cflow.name = 'Transfer Charge' THEN 'G3'" 
					   + "\n                                                                  WHEN cflow.name = 'VAT' THEN 'G5'" 
					   + "\n                                                                  WHEN cflow.name = 'Transportation' THEN 'G8'" 
					   + "\n                                                                  WHEN cflow.name = 'Shipping Charges' THEN 'G8'" 
					   + "\n                                                             END"
					   + "\n                                                   END"
					   + "\n                                              ELSE " //Country other than UK
					   + "\n                                                   CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G1'"  
					   + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'No Tax' THEN 'G1'"
					   + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'EU Zero Rated' THEN 'G1'" 
					   + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'G1'"
					   + "\n                                                   END"
					   + "\n                                         END"
					   + "\n                               END"
					   + "\n                          WHEN ab.buy_sell = " + BUY_SELL_ENUM.SELL.toInt() + " THEN "
					   + "\n                               CASE WHEN cflow.name LIKE 'Metal Rentals - %' THEN 'GC'" //For all Sell Metal Rentals code is G8
					   + "\n                                    ELSE CASE WHEN pa.country = 20077 THEN " //Country is UK
					   + "\n                                                   CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G3'" 
					   + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'GD'" 
					   + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'No Tax' THEN " 
					   + "\n                                                             CASE WHEN cflow.name = 'Manual VAT' THEN 'GF'" 
					   + "\n                                                                  ELSE 'G1'" 
					   + "\n                                                             END"
					   + "\n                                                   END"
					   + "\n                                              ELSE " //Country other than UK
					   + "\n                                                   CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'GB'"
					   + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'No Tax' THEN 'GB'"
					   + "\n                                                   END"
					   + "\n                                         END"
					   + "\n                               END"
					   + "\n                     END"
					   //For Buy operation
					   + "\n                ELSE CASE WHEN ab.buy_sell = " + BUY_SELL_ENUM.BUY.toInt() + " THEN "
					   + "\n                               CASE WHEN ab.toolset IN (" + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + "," + TOOLSET_ENUM.FX_TOOLSET.toInt() + ") THEN "
					   + "\n                                         CASE WHEN pa.country != 20077 THEN 'GA'"
					   + "\n                                              ELSE CASE WHEN ISNULL (pif.value, 'No') = 'Yes' THEN 'GC'"
					   + "\n                                                        ELSE CASE WHEN ISNULL(abtei.value, 'TBD') = 'No Tax' THEN "
					   + "\n                                                                       CASE WHEN ab.currency IN (53, 54, 55, 56) THEN 'GD' "
					   + "\n                                                                            ELSE 'GF' "
					   + "\n                                                                       END"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'Reverse Charge Gold +' THEN 'GR'" 
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'Reverse Charge Gold -' THEN 'G7'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'GD'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'UK Std Tax' THEN 'GF'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'GF'"
				       + "\n                                                             END"
				       + "\n                                                   END"
				       + "\n                                         END"
				       + "\n                                    WHEN ab.toolset IN ("  + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + ") THEN "
				       + "\n                                         CASE WHEN pa.country = 20077 THEN "
					   + "\n                                                   CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G3'"
					   + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'UK Std Tax' THEN 'G5'"
				       + "\n                                                        WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'G8'"
				       + "\n                                                   END"
					   //+ "\n                                              ELSE CASE WHEN ISNULL(pRegion.value,'') = 'EU' THEN" 
					   + "\n                                              ELSE CASE WHEN c.geographic_zone = 20004 THEN" 
				       + "\n                                                             CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G1'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'EU Zero Rated' THEN 'G3'" 
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'G3'"
					   + "\n                                                             END"
					   + "\n                                                        ELSE 'G1'" 
				       + "\n                                                   END"
				       + "\n                                         END"
				       + "\n                                    WHEN ab.toolset IN (" + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() + ") THEN "
					   + "\n                                         CASE WHEN pa.country = 20077 THEN 'GD' "
					   + "\n                                              ELSE 'GB'"
				       + "\n                                         END"
				       + "\n                               END"
					   + "\n                          WHEN ab.buy_sell = " + BUY_SELL_ENUM.SELL.toInt()+ " THEN "
					   //When party address is in the UK
					   + "\n                               CASE WHEN pa.country = 20077 THEN "
					   + "\n                                         CASE WHEN ISNULL (pif.value, 'No') = 'Yes' THEN 'G8'" //Sell -> UK -> JM Group =  
					   + "\n                                              ELSE CASE WHEN ab.toolset IN (" + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + "," + TOOLSET_ENUM.FX_TOOLSET.toInt() + ") THEN "
					   + "\n                                                             CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G4'"
					   + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'UK Std Tax' THEN 'G5'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'G4'"
					   + "\n                                                             END"
					   + "\n                                                        WHEN ab.toolset IN (" + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + "," + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() + ") THEN "
				       + "\n                                                             CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G3'"
					   + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'UK Std Tax' THEN 'G5'"
					   + "\n                                                             END"
					   + "\n                                                   END"
					   + "\n                                         END"
					   //+ "\n                                    ELSE CASE WHEN ISNULL(pRegion.value,'') = 'EU' THEN" 
					   + "\n                                    ELSE CASE WHEN c.geographic_zone = 20004 THEN" 
					   + "\n                                                   CASE WHEN ab.toolset IN (" + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + "," + TOOLSET_ENUM.FX_TOOLSET.toInt() + ") THEN "
					   + "\n                                                             CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G2'"
					   + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'EU Zero Rated' THEN 'G2'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'Reverse Charge Gold +' THEN 'G2'" 
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'Reverse Charge Gold -' THEN 'G2'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'No Tax' THEN 'G2'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'G2'"
					   + "\n                                                             END"
					   + "\n                                                        WHEN ab.toolset IN (" + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + "," + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() + ") THEN "
				       + "\n                                                             CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G3'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'EU Zero Rated' THEN 'G3'" 
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'Reverse Charge Gold +' THEN 'G2'" 
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'Reverse Charge Gold -' THEN 'G2'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'G3'"
					   + "\n                                                             END"
					   + "\n                                                   END"
					   + "\n                                              ELSE " 
					   + "\n                                                   CASE WHEN ab.toolset IN (" + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + "," + TOOLSET_ENUM.FX_TOOLSET.toInt() + ") THEN "
					   + "\n                                                             CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G0'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'G0'"
					   + "\n                                                             END"
					   + "\n                                                        WHEN ab.toolset IN (" + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + "," + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() + ") THEN "
				       + "\n                                                             CASE WHEN ISNULL(abtei.value, 'TBD') = 'Zero Rated' THEN 'G1'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'No Tax' THEN 'G1'"
				       + "\n                                                                  WHEN ISNULL(abtei.value, 'TBD') = 'TBD' THEN 'G1'"
					   + "\n                                                             END"
					   + "\n                                                   END"
					   + "\n                                         END"
					   + "\n                               END"
					   + "\n                          END"
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
					   + "\n                          ELSE CASE WHEN ISNULL(abtei.value, 'TBD') = 'Reverse Charge Gold -' THEN ISNULL(sdd.para_position, ISNULL(sddh.para_position,0.00)) * 5.0"
					   + "\n                                    ELSE ISNULL(sdd.para_position, ISNULL(sddh.para_position,0.00)) "
					   + "\n                               END"
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
					   ///For Cash deals, it's doc issue date. If none present use trade date. For Commodity and Loan Depot, it's date on the invoice. For all others, use event dates.
					   + "\n          ,CASE WHEN ab.toolset = " + TOOLSET_ENUM.CASH_TOOLSET.toInt() + " THEN "
					   + "\n                     CASE WHEN ab.cflow_type BETWEEN 2023 and 2030 THEN CONVERT(DATETIME,'" + eomStartDate + "')" //ab.trade_date"
					   + "\n                          ELSE sdh.doc_issue_date "
					   + "\n                     END"
					   + "\n                WHEN ab.toolset = " + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt() + " THEN sdh.doc_issue_date"
					   + "\n                WHEN ab.toolset IN ("  + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + ") THEN ISNULL(di.last_update, sdih.last_update)"
					   + "\n                ELSE ISNULL(abte_metal_ccy.event_date, abte.event_date) "
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
					   + "\n          ,CASE WHEN ISNULL(abtei.value, 'TBD') = '" + taxRateToCheck + "' THEN 1 ELSE -1 END as isReverseGold"
					   + "\n   FROM ab_tran ab"
					   + "\n        INNER JOIN party pint ON pint.party_id = ab.internal_bunit"
					   + "\n                              AND pint.short_name = 'JM PMM UK'"
					   + "\n        INNER JOIN party_relationship pr ON pr.business_unit_id = ab.external_bunit"
					   + "\n        INNER JOIN toolsets ts ON ts.id_number = ab.toolset"
					   //Check main address of the external LE. This will be used to get tax codes.
					   + "\n        INNER JOIN party_address pa ON pa.party_id = ab.external_lentity "
					   + "\n                                    AND pa.address_type = (SELECT pat.address_type_id"
					   + "\n                                                           FROM  party_address_type pat"
					   + "\n                                                           WHERE pat.address_type_name = 'Main'"
					   + "\n                                                          )"  
					   + "\n        INNER JOIN country c ON c.id_number = pa.country"
					   //Select all Ext LE and JM PMM US and JM PMM HK
					   + "\n        INNER JOIN party p ON p.party_id = ab.external_lentity"
					   + "\n                           AND ( (p.int_ext = 1) OR (p.int_ext = 0 AND p.party_id IN (20002, 20003)))"
					   //Ignore UK IN-TRANSIT LE party 
					   + "\n                           AND p.party_id != 20770"
					   + "\n        INNER JOIN cflow_type cflow ON cflow.id_number = ab.cflow_type"
					   //For FX and ComSwap, the pick up deals where metal ccy event dates are between the range specified 
					   + "\n        INNER JOIN ab_tran_event abte ON abte.tran_num = ab.tran_num "
					   + "\n                                      AND ( abte.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt()
					   + "\n                                            OR (abte.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt()
					   + "\n                                                AND  EXISTS(SELECT 1 FROM ab_tran_event_info abteif "
					   + "\n                                                            WHERE abteif.event_num = abte.event_num"  
					   + "\n                                                            AND   abteif.type_id = 20002 " 
					   + "\n                                                            AND   abteif.value = 'Reverse Charge Gold -')))"
					   + "\n                                      AND abte.currency IN (" + CCY_LIST + ")"			   
					   + "\n                                      AND (    "
					   + "\n                                               (ab.toolset IN (" + TOOLSET_ENUM.FX_TOOLSET.toInt() 
					   + "\n                                                             , " + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + ")"
					   + "\n                                                AND abte.currency IN (" + CCY_LIST + ")"
					   + "\n                                                AND EXISTS (SELECT 1 "
					   + "\n                                                            FROM ab_tran_event abte_metal"
					   + "\n                                                            WHERE abte_metal.tran_num = ab.tran_num"
					   + "\n                                                            AND   abte_metal.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt()		
					   + "\n                                                            AND   abte_metal.currency IN (" + METAL_CCY_LIST + ")"
					   + "\n                                                            AND   abte_metal.event_date >= '" + fromDate + "'"
					   + "\n                                                            AND   abte_metal.event_date <= '" + toDate + "'"
					   + "\n                                                           )"
					   + "\n                                               )"
					   //For Commodity and LoanDep, only check an event exists. Check dates later. 
					   + "\n                                           OR  "
					   + "\n                                               (ab.toolset IN (" + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt()
					   + "\n                                                             , " + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt()
					   + "\n                                                              )"
					   + "\n                                                AND   abte.tran_num = ab.tran_num"
				       + "\n                                               )"
				       //For cash deals, for metal rentals  check trade date is in the next month of start and end dates
					   + "\n                                           OR  (ab.toolset = " + TOOLSET_ENUM.CASH_TOOLSET.toInt()
					   + "\n                                                AND ( (ab.cflow_type < 2023 OR ab.cflow_type > 2030 "
					   + "\n                                                      )"
					   + "\n                                                     OR (ab.cflow_type BETWEEN 2023 AND 2030"
					   + "\n                                                         AND ab.trade_date >= dateadd (m, 1, '" + fromDate + "')"
					   + "\n                                                         AND ab.trade_date <= dateadd (m, 1, '" + toDate + "')"
					   + "\n                                                        )"
					   + "\n                                                    )"
					   + "\n                                               )"
					   + "\n                                          )"
					   //LBMA and LPPM are party infos on LE. Region is Party Info on BU.
					   + "\n        LEFT OUTER JOIN party_info pLBMA ON p.party_id = pLBMA.party_id AND pLBMA.type_id = 20013"
					   + "\n        LEFT OUTER JOIN party_info pLPPM ON p.party_id = pLPPM.party_id AND pLPPM.type_id = 20020"
					   //+ "\n        LEFT OUTER JOIN party_info pRegion ON ab.external_bunit = pRegion.party_id AND pRegion.type_id = 20035"
					   //For FX and ConSwap get invoice date from metal ccy event date
					   + "\n        LEFT OUTER JOIN ab_tran_event abte_metal_ccy ON abte_metal_ccy.tran_num = ab.tran_num "
					   + "\n                                                     AND ab.toolset IN (" + TOOLSET_ENUM.FX_TOOLSET.toInt() 
					   + "\n                                                                      , " + TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt() + ")"
					   + "\n                                                     AND abte_metal_ccy.event_type = " + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt()	
					   + "\n                                                     AND abte_metal_ccy.currency IN (" + METAL_CCY_LIST + ")"
					   + "\n                                                     AND abte_metal_ccy.event_date >= '" + fromDate + "'"
					   + "\n                                                     AND abte_metal_ccy.event_date <= '" + toDate + "'"	
					   //Join with stldoc_details with cash event type and normal currencies. An outer join is required here as we need to look into history tables later.
					   + "\n        LEFT OUTER JOIN stldoc_details sdd ON sdd.tran_num = ab.tran_num "
					   + "\n                                      AND sdd.event_type IN (" + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt()+ ", " + EVENT_TYPE_ENUM.EVENT_TYPE_TAX_SETTLE.toInt()+ ")"
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
				       //For Loandep deals, we check the date when invoice was issued. For Non Metal Rentals Cash deals check doc issue dates.
					   + "\n                                     AND ( (ab.toolset = " + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt()  
					   + "\n                                            AND sdh.doc_issue_date >= '" + fromDate + "'"
					   + "\n                                            AND sdh.doc_issue_date <= '" + toDate + "'"
					   + "\n                                           )"
					   + "\n                                           OR "
					   + "\n                                           (ab.toolset = " + TOOLSET_ENUM.CASH_TOOLSET.toInt() 
					   + "\n                                            AND (ab.cflow_type < 2023 OR ab.cflow_type > 2030)"
					   + "\n                                            AND sdh.doc_issue_date >= '" + fromDate + "'"
					   + "\n                                            AND sdh.doc_issue_date <= '" + toDate + "'"
					   + "\n                                           )"
					   + "\n                                           OR "
					   + "\n                                           (ab.toolset = " + TOOLSET_ENUM.CASH_TOOLSET.toInt() 
					   + "\n                                            AND ab.cflow_type BETWEEN 2023 AND 2030"
					   + "\n                                           )"
					   + "\n                                           OR"
					   + "\n                                           (ab.toolset NOT IN (" + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt()
					   + "\n                                                              ," + TOOLSET_ENUM.CASH_TOOLSET.toInt() 
					   + "\n                                                              )"
					   + "\n                                           ) "
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
					   //Get invoice documents. For Commodity, make sure they were created within the time range specified
					   + "\n        LEFT OUTER JOIN stldoc_info di ON di.document_num = sdd.document_num "
					   + "\n                                       AND  (    (ab.toolset IN  (" + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + ")"
					   + "\n                                                  AND di.last_update >= '" + fromDate + "'"
					   + "\n                                                  AND di.last_update < dateadd (d, 1, '" + toDate + "')"
					   + "\n                                                 )"
					   //For all other toolsets, do not check here. We would have checked doc issue dates, event dates and trade dates for them. 
					   + "\n                                              OR (ab.toolset IN  (" + TOOLSET_ENUM.CASH_TOOLSET.toInt()
					   + "\n                                                                , " + TOOLSET_ENUM.FX_TOOLSET.toInt() 
					   + "\n                                                                , " + TOOLSET_ENUM.LOANDEP_TOOLSET.toInt()
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
					   + "\n        LEFT OUTER JOIN stldoc_details_hist sddh ON sddh.tran_num = abte.tran_num AND sddh.event_type = 14 AND sddh.event_num = abte.event_num" 
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
					   + "\n SELECT * FROM valid_invoice"  
                       + "\n ORDER BY deal_tracking_num, tran_num, event_num";
			
			//To debug, uncomment below line and see the SQL in the log.
			Logging.info(sql);
			DBaseTable.execISql(returnt, sql); 
			
			//This is to take unique records. But as of last testing there were no duplicates. Feel free to comment.
			returnt.makeTableUnique(); 
			
			//To cater for Reverse Gold, get the data for the type_id in a temp table
			Table tblData = Table.tableNew();
			try
			{
				
				returnt.sortCol("isReverseGold");
	            tblData.select(returnt, "*", "isReverseGold EQ 1 AND tax_amount LT 0.000001");
	            tblData.addCol("toDelete", COL_TYPE_ENUM.COL_INT);
	            int colTran = tblData.getColNum("tran_num");
	            int colDelete = tblData.getColNum("toDelete");
	            
	            tblData.group("tran_num, invoice_id, tax_amount");
	            tblData.sortCol(colTran);
	            int dataCount = tblData.getNumRows();
	            for (int i=1; i <= dataCount; i++)
	            {
	            	int currTran = tblData.getInt(colTran, i);
	            	int prevTran = tblData.getInt(colTran, i-1);
	            	String currInvoice = tblData.getString("invoice_id",i);
	            	String prevInvoice = tblData.getString("invoice_id",i-1);
	            	
	            	if ((currTran == prevTran) && (currInvoice.equalsIgnoreCase(prevInvoice)))
	            	{ 
	            		tblData.setInt(colDelete, i, 1);
	            		//tblData.setDouble(colTaxAmount, i-1, 0.00);
	            		//tblData.setDouble(colTaxAmount, i, 0.00);
	            		//tblData.setDouble(colTaxAmountGBP, i-1, 0.00);
	            		//tblData.setDouble(colTaxAmountGBP, i, 0.00);
	            	}
	            }
	            tblData.sortCol(colTran);
				returnt.select(tblData, "*", "tran_num EQ $tran_num AND isReverseGold EQ $isReverseGold");
				returnt.deleteWhereValue(colDelete, 1);
				returnt.delCol(colDelete);
				returnt.delCol("isReverseGold");
				returnt.sortCol("deal_tracking_num");
			}
			catch (Exception e)
			{
				Logging.error("Unable to filter for Reverse Gold. Message: " + e.getMessage());
				throw new OException(e.getMessage());	
			}
			finally
			{
				tblData.destroy();
			}
			
			//This is to take unique records. 
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
