/*
 * File Name:      JM_MOD_Custom
 * Project Name:   SettlementDesktop
 * Platform:       OpenJVS
 * Categories:     BO Docs - Module
 * Description:    This is a Module script that runs in a Settlement Desktop Module Set
 * 
 * Revision History:
 * 21.10.15  jbonetzk  1.0 initial version:
 *                     	- Server Time 'local' as per Specification D377
 * 23.02.16	 jwaechter 1.1	added retrieval of prior sent document number
 * 01.03.16	 jwaechter 1.2	added special check for confirms in status 3 Fixed and Sent
 *                     		in prior sent document retrieval logic
 * 03.03.16	 jwaechter 1.3	merged addition of SAP Buy Sell Flag into this file
 * 08.03.16	 jwaechter 1.4	added defect fix to exclude just the last document version in 
 *                     		method createAuxDocInfoTable
 * 28.09.16  jwaechter 1.5	added functionality to merge comments that are distributed among 
 *                     		multiple rows on the database
 * 06.07.17  sma       1.6	CR52 In the folder Confirms a new item has to be added called "Customer Wording", 
 * 							which will populate the "additional_text" in user_confirm_customer_wording according to external BU    
 * 23.08.17  sma       1.7	SR149121 add field FX Payment Date 
 * 21.12.17  sma       1.8	US live Issue 20 add field End_User
 * 
 * <<some of the change history missing>>
 * 
 * 06.12.19 Jyotsna    2.1	SR 282425: Change of Int Phone Number for Cash Deal Invoices for US region
 * 06.01.20 GuptaN02 Added tags Local Currency and Local Currency Amount to handle local currency invoicing               
 */
package com.openlink.jm.bo;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import standard.back_office_module.include.JVS_INC_STD_DocMsg;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.CFLOW_TYPE;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TABLE_SORT_DIR_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.ODateTimeConversion;

@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
/** @author jbonetzky@olf.com, jneufert@olf.com */
public class JM_MOD_Custom implements IScript {
	
	private static final String DOC_STATUS_FIXED_AND_SENT = "3 Fixed and Sent";
	
	private static final String DOC_STATUS_SENT_TO_CP = "2 Sent to CP";

	final static private int DEFAULT_DB_SERVER_TIME_ZONE = 20007;

	// frequently used constants
	final static private int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
	final static private SEARCH_ENUM SEARCH_FIRST_IN_GROUP = SEARCH_ENUM.FIRST_IN_GROUP;
	final static private SHM_USR_TABLES_ENUM TIME_ZONE_TABLE = SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE;
	private static final String CONTEXT = "BackOffice"; //2.1
    private static final String SUBCONTEXT = "JM_MOD_CUSTOM"; //2.1

	@Override
	public void execute(IContainerContext context) throws OException {
		String scriptName = getClass().getSimpleName();
		JVS_INC_STD_DocMsg.printMessage("Processing " + scriptName);

		Table argt = context.getArgumentsTable();
		switch (argt.getInt( "GetItemList", 1)) {
			case 1: // item list
				ITEMLIST_createItemsForSelection( argt.getTable( "ItemList", 1) );
				break;
			case 0: // gen/xml data
				GENDATA_getStandardGenerationData(argt);
				JVS_INC_STD_DocMsg.setXmlData(argt, scriptName);
				break;
			case 2: // pre-gen data ??
				break;
			default:
				break;
		}
		JVS_INC_STD_DocMsg.printMessage("Completed Processing " + scriptName);
	}

	private void ITEMLIST_createItemsForSelection(Table itemListTable) throws OException {
		String rootGroupName = "Generic";
		ServerTime.createServerTimeItems(itemListTable, rootGroupName);

		rootGroupName = "Confirms";
		// add items specific to Confirms here
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "Prior Sent Document Num", "Prior_Sent_Document_Num", 0);
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "SAP Buy Sell Flag", "SAP_Buy_Sell_Flag", 0);
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "Customer Wording", "Customer_Wording", 0); //CR52
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "FX Payment Date", "FX_Pymt_Date", 0); //SR149121
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "End User", "End_User", 0); //US live - Issue 20
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "Transfer Subject Suffix", "Transfer_Subject_Suffix", 0); 
		rootGroupName = "Invoices";
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "Comments Table", "SettleData_Charges", 0);
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "Comments Table Num Rows", "SettleData_Charges_NumRows", 0);
	
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "US Charges", "US_Charges_cFlow_Types", 0);//2.1
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "olfTable_DocNumVATNum", "olfTable_DocNumVATNum", 0);
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "Local Currency", "Local_Currency", 0);
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "Local Currency Amount", "Local_Currency_Amount", 0);
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "Local Currency Account", "Local_Currency_Account", 0);
	}

	private void GENDATA_getStandardGenerationData(Table argt) throws OException {
		Table tblServerTime = ServerTime.prepareServerTimeData();
		tblServerTime.sortCol(2); // column#2 is time zone label

		Table eventdataTable  = JVS_INC_STD_DocMsg.getEventDataTable();
		Table gendataTable    = JVS_INC_STD_DocMsg.getGenDataTable();
		Table itemlistTable   = JVS_INC_STD_DocMsg.getItemListTable();

		if (gendataTable.getNumRows() == 0){
			gendataTable.addRow();
		}

		String commentsTableName = null; // used as flag
		String commentsTableNameNumRows = null; // used as flag
		String priorSentDocNum = null; // used as a flag

		String internal_field_name = null, output_field_name = null;
		int internal_field_name_col_num = itemlistTable.getColNum("internal_field_name");
		int output_field_name_col_num   = itemlistTable.getColNum("output_field_name");
		itemlistTable.sortCol(output_field_name_col_num, TABLE_SORT_DIR_ENUM.TABLE_SORT_DIR_DESCENDING);
		for (int row = itemlistTable.getNumRows(); row > 0; --row) {
			
			internal_field_name = itemlistTable.getString(internal_field_name_col_num, row);
			output_field_name   = itemlistTable.getString(output_field_name_col_num, row);

			// skip empty
			if (internal_field_name == null || internal_field_name.trim().length() == 0){
				continue;
			} else if (internal_field_name.startsWith("Prior_Sent_Document_Num")) {
				priorSentDocNum = output_field_name;
			} else if (internal_field_name.startsWith("ServerDate_")) {
				String strTZ = internal_field_name.substring("ServerDate_".length()).trim();
				String strValue = ServerTime.extractServerDate(tblServerTime, strTZ);
				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, strValue);
			} else if (internal_field_name.startsWith("ServerTime_")) {
				String strTZ = internal_field_name.substring("ServerTime_".length()).trim();
				String strValue = ServerTime.extractServerTime(tblServerTime, strTZ);
				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, strValue);
			} else if (internal_field_name.equals("SettleData_Charges")) {
				commentsTableName = output_field_name;
			} else if (internal_field_name.equals("SettleData_Charges_NumRows")) {
				commentsTableNameNumRows = output_field_name;
			} else if (internal_field_name.equals("SAP_Buy_Sell_Flag")) {
				
				int buySell  = eventdataTable.getInt("buy_sell", 1);
				if(buySell == 0) {
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, "B");
				} else if(buySell == 1 ) {
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, "S");				
				} else {
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, "E");
				}
			} else if (internal_field_name.equals("Customer_Wording")) {				
				String sql
				= "select at.deal_tracking_num, cw.*"
				+ " from ab_tran at"	
				+ " join USER_Confirm_Customer_Wording cw on at.external_bunit = cw.business_unit " 
				+ " where at.tran_num = " + eventdataTable.getInt("tran_num", 1) 
				+ " AND at.current_flag = 1 AND at.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()  
				+ " ORDER BY at.deal_tracking_num, cw.business_unit, cw.sequence"
				;
				Table tbl = Table.tableNew();
				DBaseTable.execISql(tbl, sql);
				
				String text = tbl.getString("additional_text", 1); 
				for(int i = 2; i<= tbl.getNumRows(); i++) {
					 text = text + " " + tbl.getString("additional_text", i); 	
				}
				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, text);
				tbl.destroy();
			} else if (internal_field_name.equals("FX_Pymt_Date"))  {				
				String sql
				= "select at.deal_tracking_num, fx.term_settle_date"
				+ " from ab_tran at"	
				+ " join fx_tran_aux_data fx on at.tran_num = fx.tran_num " 
				+ " where at.tran_num = " + eventdataTable.getInt("tran_num", 1) 
				+ " AND at.current_flag = 1 AND at.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()  
//				+ " AND at.ins_type in (" + CFLOW_TYPE.FX_SPOT_CFLOW.jvsValue() + ", " + CFLOW_TYPE.FX_CFLOW.jvsValue() + ")" //36, 13
				;
				Table tbl = Table.tableNew();
				DBaseTable.execISql(tbl, sql);
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
				String settleDate = ODateTimeConversion.convertODateTimeToString(tbl.getDateTime("term_settle_date", 1), sdf);
		
				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, settleDate);
				tbl.destroy();
			} else if (internal_field_name.equals("End_User"))  {				
				//US live issue 20
				String sql
				= "select piv.value "
				+ " from party_info_view piv, ab_tran at "	
				+ " where piv.type_name = 'JM Group' "
				+ " AND at.tran_num = " + eventdataTable.getInt("tran_num", 1) 
				+ " AND at.external_lentity = piv.party_id"
				;
				Table tbl = Table.tableNew();
				DBaseTable.execISql(tbl, sql);
				
				String partyInfoJMGroup = tbl.getString("value", 1); 
				
				String endUser = "";
				if("Yes".equals(partyInfoJMGroup)){
					sql
					= "select u.end_user_customer from "
					+ " (select * from ab_tran_info_view where type_name = 'End User' "	
					+ " AND tran_num = " + eventdataTable.getInt("tran_num", 1) 
					+ " ) ai "
					+ "JOIN (select DISTINCT end_user_customer from user_jm_end_user_view where is_end_user = 1) u "
					+ " ON ai.value = u.end_user_customer"
					;
					tbl = Table.tableNew();
					DBaseTable.execISql(tbl, sql);
					endUser = tbl.getString("end_user_customer", 1); 
				}
				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, endUser);
				tbl.destroy();
			}
			//
			else if (internal_field_name.equals("Transfer_Subject_Suffix")) {
				String InstrumentSubType = gendataTable.getString("olfInsSubTypeShort", row).replaceAll("\\s+", "");
				String transferSuffix = "";
				if (InstrumentSubType.equalsIgnoreCase("CashTransfer")) {
					String extBu = gendataTable.getString("olfExtBUShortName", row);
					String strategyFromAccBUShortName = gendataTable.getString("olfMtlTfStratInfo_From_BU", row);
					String strategyToAccBUShortName = gendataTable.getString("olfMtlTfStratInfo_To_BU", row);
					String strategyFromAccBULongName= Ref.getPartyLongName(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, strategyFromAccBUShortName));
					String strategyToAccBULongName=Ref.getPartyLongName(Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, strategyToAccBUShortName));
					transferSuffix = (extBu.equalsIgnoreCase(strategyToAccBUShortName)) ? strategyFromAccBULongName : strategyToAccBULongName;
					}
				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, transferSuffix);
			}
			//2.1 Starts
			else if (internal_field_name.equals("US_Charges_cFlow_Types"))  {  
				
				//Reading US charges types (cash flow types) from const repo
                Table usChargesCflowTypes = Util.NULL_TABLE;
                
                ConstRepository repository = new ConstRepository(CONTEXT, SUBCONTEXT);                                      
                usChargesCflowTypes = repository.getMultiStringValue("US_Charges"); 

                HashSet<String> cFlowSet = new HashSet<>();
                for (int rowcount=1;rowcount<=usChargesCflowTypes.getNumRows();rowcount++){
                                
                                String cflowtype = usChargesCflowTypes.getString("value", rowcount);
                                cFlowSet.add(cflowtype);            
                                
                }
                for(int rowCount=1;rowCount<=eventdataTable.getNumRows();rowCount++)
                {
                // retrieving instrument type from eventdataTable 
                int insType = eventdataTable.getInt("ins_type", rowCount);
                
                //retrieving cashflowtype from eventdataTable
                int intcflowType = eventdataTable.getInt("cflow_type", 1);
                String cFlow=Ref.getName(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, intcflowType);
                             
                if(insType ==  Ref.getValue(SHM_USR_TABLES_ENUM.INS_TYPE_TABLE, "Cash")&& cFlowSet.contains(cFlow)){
                JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, "1");
                break;
                }
                else{
                
                JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, "0");
                }
                }
                usChargesCflowTypes.destroy();
                	
			}//2.1 ends
			
			else if (internal_field_name.equals("Local_Currency")) {
				String localCurrency = eventdataTable.getString("Local Currency", 1);
				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, localCurrency);
		
			}
			
			else if (internal_field_name.equals("Local_Currency_Amount")) {
					int localCurrencyAmount = eventdataTable.getInt("Local Currency Amount", 1);
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, localCurrencyAmount);
				}
			
			
			else if (internal_field_name.equals("Local_Currency_Account")) {
				String localCurrencyAccount = eventdataTable.getString("Local Currency Account", 1);
				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, localCurrencyAccount);
			}
			
			else {
				// [n/a]
				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, "[n/a]");
			}
		}

		if (priorSentDocNum != null) {
			int nextDocStatus = eventdataTable.getInt("next_doc_status", 1);
			int docType = eventdataTable.getInt("doc_type", 1);
			int docVersion = eventdataTable.getInt("doc_version", 1);
			String nextDocStatusName = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, nextDocStatus);
			String docTypeName = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, docType);
			boolean useConfirmFixedAndSentLogic = false;
			if (	"Confirm".equals(docTypeName) &&	"3 Fixed and Sent".equals(nextDocStatusName)) {
				useConfirmFixedAndSentLogic = true;
			}			
			Table auxDocInfoTable = createAuxDocInfoTable (eventdataTable, useConfirmFixedAndSentLogic, docVersion);
			int priorSentDocNumValue = 0;
			if (auxDocInfoTable.getNumRows() > 0) {
				priorSentDocNumValue = auxDocInfoTable.getInt("prior_sent_doc_num", 1);
			}
			JVS_INC_STD_DocMsg.GenData.setField(gendataTable, priorSentDocNum, priorSentDocNumValue);			
		}
		
		if (commentsTableName != null || commentsTableNameNumRows != null) {
			final String NOTE_TYPE_INVOICE = "Invoice";

			int qID = Query.tableQueryInsert(eventdataTable, "tran_num");
			String qTbl = Query.getResultTableForId(qID);
			String sql
				= "select  c.line_text, c.comment_date, c.time_stamp, c.line_num, c.comment_num, at.deal_tracking_num, at.ins_type, at.reference, at.trade_date, at.position, at.price, at.rate, at.currency, at.cflow_type, at.ins_sub_type"
				+ " from ab_tran at"	
				+ " join "+qTbl+" q on q.query_result=at.tran_num and q.unique_id="+qID
				+ " join tran_notepad c on c.tran_num=at.tran_num"
				+ " join deal_comments_type ct on ct.type_id=c.note_type and ct.type_name='"+NOTE_TYPE_INVOICE+"'"
				+ " ORDER BY at.deal_tracking_num, c.comment_num, c.line_num"
				;
			Table tbl = Table.tableNew(commentsTableName);
			DBaseTable.execISql(tbl, sql);
			Query.clear(qID);
		

			for (int row=tbl.getNumRows(); row >= 1; row--) {
				int commentNum = tbl.getInt("comment_num", row);
				int lineNum = tbl.getInt("line_num", row);
				int dealTrackingNum = tbl.getInt("deal_tracking_num", row);
				int lesserRowSameComment = getLesserRowSameComment (tbl, commentNum, lineNum, dealTrackingNum, row);
				if (lesserRowSameComment != Integer.MAX_VALUE) {
					String commentHigh = tbl.getString("line_text", row);
					String commentLow = tbl.getString("line_text", lesserRowSameComment);
					String concat = commentLow += commentHigh;
					tbl.setString("line_text", lesserRowSameComment, concat);
					tbl.delRow(row);
				}
			}
			
			tbl.delCol("line_num");
			tbl.delCol("comment_num");

			
			int rows = tbl.getNumRows();
			if (commentsTableNameNumRows != null) {

				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, commentsTableNameNumRows, rows);
			}

			if (commentsTableName != null) {
				tbl.setColFormatAsRef("ins_type", SHM_USR_TABLES_ENUM.INS_TYPE_TABLE);
				tbl.setColFormatAsRef("currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
				tbl.setColFormatAsRef("cflow_type", SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE);
				tbl.setColFormatAsRef("ins_sub_type", SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE);

				tbl.sortCol("deal_tracking_num");
				tbl.insertCol("row_num", 1, COL_TYPE_ENUM.COL_INT);
				if (rows > 0){
					tbl.setColIncrementInt(1, 1, 1);
				} else {
					tbl.addRow();
				}

				JVS_INC_STD_DocMsg.GenData.setField(gendataTable, tbl);
			} else {
				tbl.destroy();
			}
		}

		tblServerTime.destroy();
	}
	
	private int getLesserRowSameComment(Table tbl, int commentNum, int lineNum, int dealTrackingNum, int row) throws OException {
		
		for (int lesserRow = row-1; lesserRow >= 1; lesserRow--) {
			int commentNumLesser = tbl.getInt("comment_num", lesserRow);
			int lineNumLesser = tbl.getInt("line_num", lesserRow);
			int dealTrackingNumLesser = tbl.getInt("deal_tracking_num", lesserRow);
			if (   dealTrackingNum == dealTrackingNumLesser 
				&& commentNum == commentNumLesser
				&& lineNum > lineNumLesser
					) {
				return lesserRow;
			}
		}
		return Integer.MAX_VALUE;
	}

	private Table createAuxDocInfoTable(Table eventdataTable, boolean useConfirmFixedAndSentLogic, int curDocVer) throws OException {
		Table auxDocInfo = Table.tableNew("Aux Doc Info");
		int dealQueryID = -1;
		int docType = -1;
		int externalBunit=-1;
		int externalLentity=-1;

		if (eventdataTable.getNumRows() > 0) {
			docType = eventdataTable.getInt("doc_type", 1);
			externalBunit = eventdataTable.getInt("external_bunit", 1);
			externalLentity = eventdataTable.getInt("external_lentity", 1);
		} else {
			auxDocInfo.addCol("deal_num", COL_TYPE_ENUM.COL_INT, "Deal Tracking Num");
			auxDocInfo.addCol("prior_sent_doc_num", COL_TYPE_ENUM.COL_INT, "Prior Sent Document Num");
			return auxDocInfo;
		}
		try {
			dealQueryID = Query.tableQueryInsert(eventdataTable, "deal_tracking_num");
			String queryTableName = Query.getResultTableForId(dealQueryID);
			String sql =
					"\n(SELECT q.query_result AS \"deal_num\", hist.doc_version, max(hist.document_num) AS \"prior_sent_doc_num\""
				+	"\nFROM " + queryTableName + " q"
				+	"\nINNER JOIN stldoc_details_hist sdh"
				+   "\n  ON sdh.deal_tracking_num = q.query_result"
				+   "\n  AND sdh.external_bunit = " + externalBunit
				+   "\n  AND sdh.external_lentity = " + externalLentity
				+   "\nINNER JOIN stldoc_header_hist hist"
				+   "\n  ON hist.document_num = sdh.document_num"
				+   "\n  AND hist.doc_type = " + docType
				+   "\n  AND hist.document_sent_flag = 1"
//				+	"\n  AND hist.doc_version < (SELECT MAX (hist2.doc_version) FROM stldoc_header_hist hist2 WHERE hist2.document_num = hist.document_num)" // + curDocVer
				+ (useConfirmFixedAndSentLogic?(
					"\nINNER JOIN stldoc_document_status sds"
				+   "\n  ON sds.doc_status = hist.doc_status"
				+	"\n  AND sds.doc_status_desc = '" + DOC_STATUS_FIXED_AND_SENT + "'"):(
					"\nINNER JOIN stldoc_document_status sds"
				+   "\n  ON sds.doc_status = hist.doc_status"
				+	"\n  AND sds.doc_status_desc = '" + DOC_STATUS_SENT_TO_CP + "'"))
				
				+	"\nINNER JOIN stldoc_output_log sol"
 				+   "\n  ON hist.document_num = sol.document_num"
				+   "\n   and hist.doc_version = sol.doc_version"
				+   "\n   and send_status = 1"

 
				+	"\nWHERE q.unique_id = " + dealQueryID
				+	"\nGROUP BY q.query_result, hist.doc_version)"
				+   "\nEXCEPT"
				+ "\n(SELECT q.query_result AS \"deal_num\", sh.doc_version, max(sh.document_num) AS \"prior_sent_doc_num\""
				+	"\nFROM " + queryTableName + " q"
				+	"\nINNER JOIN stldoc_details sd"
				+   "\n  ON sd.deal_tracking_num = q.query_result"
				+   "\n  AND sd.external_bunit = " + externalBunit
				+   "\n  AND sd.external_lentity = " + externalLentity
				+   "\nINNER JOIN stldoc_header sh"
				+   "\n  ON sh.document_num = sd.document_num"
				+   "\n  AND sh.doc_type = " + docType
				+ (useConfirmFixedAndSentLogic?(
						"\nINNER JOIN stldoc_document_status sds"
					+   "\n  ON sds.doc_status = sh.doc_status"
					+	"\n  AND sds.doc_status_desc = '" + DOC_STATUS_FIXED_AND_SENT + "'"):
						(
						"\nINNER JOIN stldoc_document_status sds"
					+   "\n  ON sds.doc_status = sh.doc_status"
					+	"\n  AND sds.doc_status_desc = '" + DOC_STATUS_SENT_TO_CP + "'"))
		//		+	"\nINNER JOIN stldoc_output_log sol"
 //				+   "\n  ON sd.document_num = sol.document_num"
//				+   "\n   and sd.doc_version = sol.doc_version"
//				+   "\n   and send_status = 1"					
				+	"\nWHERE q.unique_id = " + dealQueryID + " AND sd.document_num = (SELECT MAX(sd2.document_num) FROM stldoc_details sd2 WHERE sd2.deal_tracking_num = q.query_result)"
				+	"\nGROUP BY q.query_result, sh.doc_version)"
					;
			int ret = DBaseTable.execISql(auxDocInfo, sql);
			if (ret != OLF_RETURN_SUCCEED) {
				String errorMessage = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql + "\n");				
				PluginLog.error(errorMessage);
				throw new OException (errorMessage);
			}
			return auxDocInfo;
		} finally {
			if (dealQueryID != -1) {
				Query.clear(dealQueryID);
			}
		}
	}

	private static class ServerTime {
		
		static void createServerTimeItems(Table itemListTable, String parentGroup) throws OException {
			String groupName = "";
			if (parentGroup != null && (parentGroup=parentGroup.trim()).length() > 0){
				groupName += parentGroup + ",";
			}
			groupName += "Server Time";

			Table tblTimezones = null;
			
			try {
				tblTimezones = loadTimeZones(true);
				String groupNameTzDesc, label;
				int colDesc  = tblTimezones.getColNum("description"),
					colLabel = tblTimezones.getColNum("time_zone_label");
			
				for (int row=tblTimezones.getNumRows(); row > 0; --row) {
					groupNameTzDesc = groupName+","+tblTimezones.getString(colDesc, row);
					label = tblTimezones.getString(colLabel, row);
					JVS_INC_STD_DocMsg.ItemList.add(itemListTable, groupNameTzDesc, "Server Date", "ServerDate_"+label, 0);
					JVS_INC_STD_DocMsg.ItemList.add(itemListTable, groupNameTzDesc, "Server Time", "ServerTime_"+label, 0);
				}
			} finally { 
				if (tblTimezones != null) {
					tblTimezones.destroy(); 
				}
			}
		}

		private static Table loadTimeZones(boolean activeOnly) throws OException {
			
			String sql = "select time_zone_id, time_zone_label, description from time_zones";
			if (activeOnly){
				sql += " where active_flag=1";
			}
			Table tbl = Table.tableNew("time_zones");
			try {
				if (OLF_RETURN_SUCCEED != DBaseTable.execISql(tbl, sql))
					throw new OException("");
			} catch (OException e) {
				tbl.destroy();
				throw new OException("Failed to execute sql statement:\n"+sql);
			}
			return tbl;
		}

		static Table prepareServerTimeData() throws OException {
			// TODO adjust entire logic in case server is in GMT zone
			int serverTZ = retrieveServerTimezone();
			Table tbl = loadTimeZones(true);
			tbl.sortCol(1); // column#1 is time zone id
			int serverTzRow = tbl.findInt(1, serverTZ, SEARCH_FIRST_IN_GROUP);
			if (serverTzRow <= 0) {
				
				tbl.destroy();
				throw new OException("Failed to deal with server time zone (seems not active): "+Ref.getName(TIME_ZONE_TABLE, serverTZ)+"("+serverTZ+")");
			}

			tbl.addCol("server_datetime", COL_TYPE_ENUM.COL_DATE_TIME);
			int colDT = tbl.getColNum("server_datetime");
			tbl.setDateTime(colDT, serverTzRow, ODateTime.getServerCurrentDateTime());
			ODateTime dtServer = tbl.getDateTime(colDT, serverTzRow);
			tbl.sortCol(2); // column#2 is time zone label
			serverTzRow = tbl.unsortedFindInt(1, serverTZ);

			int serverTzGmtRow = tbl.findString(2, "GMT", SEARCH_FIRST_IN_GROUP);
			if (serverTzGmtRow <= 0) {
				
				serverTzGmtRow = tbl.addRow();
				tbl.setInt(1, serverTzGmtRow, 1000);
				tbl.setString(2, serverTzGmtRow, "GMT");
			}
			tbl.setDateTime(colDT, serverTzGmtRow, ODateTime.dtNew());
			ODateTime dtServerGMT = tbl.getDateTime(colDT, serverTzGmtRow);
			dtServer.convertToGMT(dtServerGMT, serverTZ/*, 0*/);

			for (int row=tbl.getNumRows(); row > 0; --row) {
				
				if (row==serverTzRow || row==serverTzGmtRow){
					continue;
				}

				tbl.setDateTime(colDT, row, ODateTime.dtNew());
				ODateTime dt = tbl.getDateTime(colDT, row);
				dtServerGMT.convertFromGMT(dt, tbl.getInt(1, row)/*, 0*/);
			}

			{
				// zero fall-back
				int row = tbl.addRow();
				tbl.setInt(1, row, 0);
				tbl.setString(2, row, "ZERO");
				tbl.setDateTime(colDT, row, ODateTime.dtNew());
			}

			tbl.addCols("I(server_date)I(server_time)");
			int colDate = tbl.getColNum("server_date"), colTime = tbl.getColNum("server_time");
			tbl.setColFormatAsDate(colDate, DATE_FORMAT.DATE_FORMAT_DEFAULT, DATE_LOCALE.DATE_LOCALE_DEFAULT);
			tbl.setColFormatAsTime24HR(colTime);
			
			for (int row=tbl.getNumRows(); row > 0; --row) {
				tbl.setInt(colDate, row, tbl.getDate(colDT, row));
				tbl.setInt(colTime, row, tbl.getTime(colDT, row));
			}
			tbl.convertColToString(colDate);
			tbl.convertColToString(colTime);

			return tbl;
		}

		private static int retrieveServerTimezone() throws OException {
			int intTZ = DEFAULT_DB_SERVER_TIME_ZONE;
			String strTZ;

			JVS_INC_STD_DocMsg.printMessage("Default time zone is (" + intTZ + ") " + Ref.getName(TIME_ZONE_TABLE, intTZ));

			// first attempt: AB_DB_SERVER_TIME_ZONE_FOR_DISPLAY
			strTZ = SystemUtil.getEnvVariable("AB_DB_SERVER_TIME_ZONE_FOR_DISPLAY");
			if (strTZ != null && (strTZ=strTZ.trim()).length() > 0 && (intTZ=Ref.getValue(TIME_ZONE_TABLE, strTZ)) > 0) {
				JVS_INC_STD_DocMsg.printMessage("Retrieved " + strTZ + " from AB_DB_SERVER_TIME_ZONE_FOR_DISPLAY");
				return intTZ;
			}

			// second attempt: AB_DB_SERVER_TIME_ZONE
			strTZ = SystemUtil.getEnvVariable("AB_DB_SERVER_TIME_ZONE");
			if (strTZ != null && (strTZ=strTZ.trim()).length() > 0 && (intTZ=Ref.getValue(TIME_ZONE_TABLE, strTZ)) > 0) {
				JVS_INC_STD_DocMsg.printMessage("Retrieved " + strTZ + " from AB_DB_SERVER_TIME_ZONE");
				return intTZ;
			}

			// further attempts here

			// final attempt: hard-coded value
			intTZ = DEFAULT_DB_SERVER_TIME_ZONE;
			if (intTZ == Ref.getValue(TIME_ZONE_TABLE, Ref.getName(TIME_ZONE_TABLE, intTZ))) {
				JVS_INC_STD_DocMsg.printMessage("Retrieved default time zone " + Ref.getName(TIME_ZONE_TABLE, intTZ));
				return intTZ;
			}

			throw new OException("Failed to retrieve time zone for server");
		}

		static String extractServerDate(Table tblServerTime, String destTZ) throws OException {
			
			int intTzRow = tblServerTime.findString(2, destTZ, SEARCH_FIRST_IN_GROUP);
			if (intTzRow <= 0) {
				JVS_INC_STD_DocMsg.printWarning("Failed to retrieve server date for " + destTZ);
				intTzRow = tblServerTime.findString(2, "ZERO", SEARCH_FIRST_IN_GROUP);
			}
			return tblServerTime.getString("server_date", intTzRow);
		}

		static String extractServerTime(Table tblServerTime, String destTZ) throws OException {
			int intTzRow = tblServerTime.findString(2, destTZ, SEARCH_FIRST_IN_GROUP);
			if (intTzRow <= 0) {
				JVS_INC_STD_DocMsg.printWarning("Failed to retrieve server time for " + destTZ);
				intTzRow = tblServerTime.findString(2, "ZERO", SEARCH_FIRST_IN_GROUP);
			}
			return tblServerTime.getString("server_time", intTzRow);
		}
	}
}
