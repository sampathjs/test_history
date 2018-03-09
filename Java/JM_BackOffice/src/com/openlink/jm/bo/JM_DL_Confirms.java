package com.openlink.jm.bo;


import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.sc.bo.docproc.BO_CommonLogic.Query;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-10-14	V1.0	jwaechter	- initial version
 * 2016-04-21   V1.1    scurran     - updated to set the filter to 2 for metal transfers
 * 2016-06-07	V1.2	jwaechter	- Added changes for CR9: 
 *                                    Metal Value date
 *                                    Weight
 *                                    Trade Price
 *                                    Currency
 * 2016-08-18   V1.3    scurran     - change logic for setting the transfer filter to be derived from the 
 *                                    external bu and le                                    
 */

/**
 * Dataload script to create/fill JM specific columns for confirmations.
 * <table>
 *   <tr> 
 *   	<th> Functionality </th>
 *      <th> Description </th>
 *   </tr>
 *   <tr> 
 *   	<td> Creating and Filling Filter 1 </td>
 *      <td> 
 * 			<ol>
 * 				<li> Add int column named {@value #COL_NAME_FILTER_1} </li>
 * 				<li> Retrieve value from pre-existing argt column {@value #IS_COVERAGE_TRAN_INFO_COL}
 * 					 containing value of tran info field "Is Coverage" </li> 
 * 				<li> The value retrieved in step 2( can be "Yes" or "No")
 * 					 is mapped to 1 in case of "Yes" or 0 in all other cases 
 * 					 (including null and empty value)
 * 				</li>
 * 			    <li>
 * 					 Put mapped value into {@value #COL_NAME_FILTER_1}.
 * 				</li>
 * 				<li>
 * 					If {@value #COL_NAME_FILTER_1} contains 0 the retrieve the value from 
 * 					the external LE and BU. If a gm group company and active in GT set to 2
 * 				</li>
 *          </ol>
 *      
 *      </td>
 *   </tr>
 * </table>
 * @author jwaechter
 * @version 1.2
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_DATALOAD)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_DL_Confirms implements IScript {
	// Column name of the source column for logic "Creating and Filling Filter 1"
	private static final String IS_COVERAGE_TRAN_INFO_COL = "tran_info_type_20021";
	
	// Column name of the source column for logic "Creating and Filling Filter 1"
	private static final String SAP_MTRNo_TRAN_INFO_COL = "tran_info_type_20073";
	
	// Column name of the target column for logic "Creating and Filling Filter 1"
    private static final String COL_NAME_FILTER_1 = "filter_1";

    // Name of the event info type Metal Value Date in table 'tran_event_info_types'
    private static final String NAME_EVENT_INFO_METAL_VALUE_DATE = "Metal Value Date";

    private static final String COL_NAME_EVENT_INFO_METAL_VALUE_DATE = "event_info_type_20006";
    
    private static final String COL_NAME_WEIGHT = "weight";

    private static final String COL_NAME_TRADE_PRICE = "tran_info_type_20031";
    
    private static final String COL_NAME_CURRENCY = "currency";

    
	public void execute(IContainerContext context) throws OException
    {
        try { process(context); } 
        catch (Exception e) { PluginLog.error (e.toString()); }
    }

	private void process(IContainerContext context)  throws OException {
		Table argt = context.getArgumentsTable();
		
		argt.addCol(COL_NAME_FILTER_1, COL_TYPE_ENUM.COL_INT);
		argt.setColTitle(COL_NAME_FILTER_1, "Filter 1");
		for (int row = argt.getNumRows(); row >= 1; row--) {
			String coverage = argt.getString(IS_COVERAGE_TRAN_INFO_COL, row);
			int filter1 = (coverage != null && coverage.trim().equalsIgnoreCase("Yes"))?1:0;
			argt.setInt(COL_NAME_FILTER_1, row, filter1);
			
//          Select logic for transfer confirms output is driven by SQL and party info field not
//          the  metal transfer number, support phased SAP go live
//			if(filter1 == 0) {
//				String sapMetalTransfer = argt.getString(SAP_MTRNo_TRAN_INFO_COL, row);
//				if(sapMetalTransfer != null && sapMetalTransfer.trim().length() > 0) {
//					argt.setInt(COL_NAME_FILTER_1, row, 2);
//				}
//			}
		}
		changeTradePriceType (argt);
		addCurrency(argt);
		addWeight(argt);
//		clearLoanDepMetalValueDate(argt);
		
		setFilterForTransfers(argt);
	}
	
	private void setFilterForTransfers(Table argt) throws OException {
		Table sqlResult = null;
		int queryId = -1;
		try {
			queryId = Query.tableQueryInsert(argt, "tran_num");
			String queryResultTable = Query.getResultTableForId(queryId);
			String sql =  
					  " select deal_tracking_num, tran_num, external_bunit ,external_lentity , "
					+ " ISNULL(jmg.value, 'No') as jm_group, ISNULL(gta.value, 'No') as gt_active, "
					+ " case  "
					+ "    when  ISNULL(jmg.value, 'No') = 'Yes' and  ISNULL(gta.value, 'No') = 'Yes' then 2 "
					+ "    else 0 "
					+ " end as xml_confirm, 0 as  " + COL_NAME_FILTER_1
 					+ " from " + queryResultTable + " qr "
					+ " join  ab_tran ab ON ab.tran_num = qr.query_result and ins_type = 27001 and  ins_sub_type = 10001 "
					+ " left join party_info_view jmg on jmg.party_id = external_lentity and jmg.type_name = 'JM Group' "
					+ " left join party_info_view gta on gta.party_id = external_bunit and gta.type_name = 'GT Active' "
					+ " WHERE qr.unique_id = " + queryId;
					
			sqlResult = Table.tableNew("sap transfer");
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql + "\n");
				throw new OException (errorMessage);
			}
		
			argt.select(sqlResult, "xml_confirm("+ COL_NAME_FILTER_1 + ")", "tran_num EQ $tran_num and " + COL_NAME_FILTER_1 + " EQ $" + COL_NAME_FILTER_1);
		} finally {
			if (queryId != -1) {
				Query.clear(queryId);
			}
			if (sqlResult != null) {
				sqlResult.destroy();
			}
		}
	}

	private void clearLoanDepMetalValueDate(Table argt) throws OException {
		int insTypeLoanML = Ref.getValue(SHM_USR_TABLES_ENUM.INS_TYPE_TABLE, "LOAN-ML");
		int insTypeDepoML = Ref.getValue(SHM_USR_TABLES_ENUM.INS_TYPE_TABLE, "DEPO-ML");
		
		for (int row=argt.getNumRows(); row >= 1; row--) {
			int insType = argt.getInt("ins_type", row);
			if (insType == insTypeDepoML || insType == insTypeLoanML) {
				argt.setString(COL_NAME_EVENT_INFO_METAL_VALUE_DATE, row, "");
			}
		}
	}

	private void addWeight(Table argt) throws OException {
		argt.addCol(COL_NAME_WEIGHT, COL_TYPE_ENUM.COL_DOUBLE, "Weight");
		argt.setColFormatAsNotnlAcct(COL_NAME_WEIGHT, 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		int unitToz = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "TOz");
		for (int row=argt.getNumRows(); row >= 1; row--) {
			double weightToz = argt.getDouble("tran_position", row);
			int targetUnit = argt.getInt ("tran_unit", row);
			double factor = Transaction.getUnitConversionFactor(unitToz, targetUnit);
			double weight = factor*weightToz;
			argt.setDouble(COL_NAME_WEIGHT, row, weight);
		}
	}

	private void addCurrency(Table argt) throws OException {
		argt.addCol(COL_NAME_CURRENCY, COL_TYPE_ENUM.COL_STRING, "Currency");
		Table sqlResult = null;
		int queryId = -1;
		try {
			queryId = Query.tableQueryInsert(argt, "tran_num");
			String queryResultTable = Query.getResultTableForId(queryId);
			String sql = 
					"\nSELECT ab.tran_num"
				+	"\n  ,c.name AS " + COL_NAME_CURRENCY
				+	"\nFROM " + queryResultTable + " qr"
				+	"\nINNER JOIN ab_tran ab "
				+	"\n  ON ab.tran_num = qr.query_result"
				+   "\nINNER JOIN instruments i"
				+	"\n  ON i.id_number = ab.ins_type"
				+	"\n    AND i.name IN ('COMM-SWAP', 'METAL-SWAP')"
				+   "\nINNER JOIN parameter p"
				+	"\n  ON p.ins_num = ab.ins_num"
				+	"\n    AND p.param_seq_num  = 1"
				+   "\nINNER JOIN idx_unit u"
				+	"\n  ON u.unit_id = p.unit"
				+   "\nINNER JOIN currency c"
				+	"\n  ON c.id_number = p.currency"
				+	"\nWHERE qr.unique_id = " + queryId
					;
			sqlResult = Table.tableNew("currency and unit by tran num");
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql + "\n");
				throw new OException (errorMessage);
			}
			argt.select(sqlResult, COL_NAME_CURRENCY, "tran_num EQ $tran_num");
			sql = 
					"\nSELECT ab.tran_num"
				+	"\n  ,c.name AS " + COL_NAME_CURRENCY
				+	"\nFROM " + queryResultTable + " qr"
				+	"\nINNER JOIN ab_tran ab "
				+	"\n  ON ab.tran_num = qr.query_result"
				+   "\nINNER JOIN instruments i"
				+	"\n  ON i.id_number = ab.ins_type"
				+	"\n    AND i.name IN ('FX')"
				+   "\nINNER JOIN parameter p"
				+	"\n  ON p.ins_num = ab.ins_num"
				+	"\n    AND p.param_seq_num  = 1"
				+   "\nINNER JOIN idx_unit u"
				+	"\n  ON u.unit_id = p.unit"
				+   "\nINNER JOIN currency c"
				+	"\n  ON c.id_number = p.currency"
				+	"\nWHERE qr.unique_id = " + queryId
					;
			sqlResult.destroy();
			sqlResult = Table.tableNew("currency and unit by tran num");
			ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql + "\n");
				throw new OException (errorMessage);
			}
			argt.select(sqlResult, COL_NAME_CURRENCY, "tran_num EQ $tran_num");

			sql = 
					"\nSELECT ab.tran_num"
				+	"\n  ,c.name AS " + COL_NAME_CURRENCY
				+	"\nFROM " + queryResultTable + " qr"
				+	"\nINNER JOIN ab_tran ab "
				+	"\n  ON ab.tran_num = qr.query_result"
				+   "\nINNER JOIN instruments i"
				+	"\n  ON i.id_number = ab.ins_type"
				+	"\n    AND i.name IN ('LOAN-ML', 'DEPO-ML')"
				+   "\nINNER JOIN parameter p"
				+	"\n  ON p.ins_num = ab.ins_num"
				+	"\n    AND p.param_seq_num  = 0"
				+   "\nINNER JOIN idx_unit u"
				+	"\n  ON u.unit_id = p.unit"
				+   "\nINNER JOIN currency c"
				+	"\n  ON c.id_number = p.currency"
				+	"\nWHERE qr.unique_id = " + queryId
					;
			sqlResult.destroy();
			sqlResult = Table.tableNew("currency and unit by tran num");
			ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql + "\n");
				throw new OException (errorMessage);
			}
			argt.select(sqlResult, COL_NAME_CURRENCY, "tran_num EQ $tran_num");

			
		} finally {
			if (queryId != -1) {
				Query.clear(queryId);
			}
			if (sqlResult != null) {
				sqlResult.destroy();
			}
		}
	}

	private void changeTradePriceType(Table argt) throws OException {
		argt.setColName(COL_NAME_TRADE_PRICE, COL_NAME_TRADE_PRICE + "_temp");
		argt.addCol (COL_NAME_TRADE_PRICE, COL_TYPE_ENUM.COL_DOUBLE, "Trade Price");
		argt.setColFormatAsNotnl(COL_NAME_TRADE_PRICE, 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		for (int row = argt.getNumRows(); row >= 1; row--) {
			String tradePriceUnparsed = argt.getString (COL_NAME_TRADE_PRICE + "_temp", row);
			double tradePriceParsed = 0.0d;
			try {
				if (tradePriceUnparsed != null) {
					tradePriceParsed = Double.parseDouble(tradePriceUnparsed);
				}
			} catch (NumberFormatException ex) {
				// does not matter, assume 0
			}
			argt.setDouble(COL_NAME_TRADE_PRICE, row, tradePriceParsed);
		}
		argt.delCol(COL_NAME_TRADE_PRICE + "_temp");
	}
}
