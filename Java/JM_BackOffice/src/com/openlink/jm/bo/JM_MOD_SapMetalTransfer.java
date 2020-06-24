/*
 * File Name:      JM_MOD_Custom
 * Project Name:   SettlementDesktop
 * Platform:       OpenJVS
 * Categories:     BO Docs - Module
 * Description:    This is a Module script that runs in a Settlement Desktop Module Set
 * 
 * Revision History:
 * 21.10.15  jbonetzk  initial version:
 *                     - Server Time 'local' as per Specification D377
 * 23.02.16	 jwaechter added retrieval of prior sent document number
 * 01.03.16	 jwaechter added special check for confirms in status 3 Fixed and Sent
 *                     in prior sent document retrieval logic
 * 03.03.16	 jwaechter merged addition of SAP Buy Sell Flag into this file
 * 08.03.16	 jwaechter added defect fix to exclude just the last document version in 
 *                     method createAuxDocInfoTable
 */
package com.openlink.jm.bo;

import standard.back_office_module.include.JVS_INC_STD_DocMsg;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.TABLE_SORT_DIR_ENUM;
import com.olf.jm.logging.Logging;

@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
/** @author jbonetzky@olf.com, jneufert@olf.com */
public class JM_MOD_SapMetalTransfer implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {
		String scriptName = getClass().getSimpleName();
		Logging.info("Processing " + scriptName);
		//JVS_INC_STD_DocMsg.printMessage("Processing " + scriptName);

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
		Logging.info("Completed Processing " + scriptName);
		//JVS_INC_STD_DocMsg.printMessage("Completed Processing " + scriptName);
	}

	private void ITEMLIST_createItemsForSelection(Table itemListTable) throws OException {
		String rootGroupName = "SapMetalTransfer";

		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "From Account Business Unit Code", "From_Account_Business_Unit_Code", 0);
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "To Account Business Unit Code", "To_Account_Business_Unit_Code", 0);
		
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "From Account Legal Entity Code", "From_Account_Legal_Entity_Code", 0);
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "To Account Legal Entity Code", "To_Account_Legal_Entity_Code", 0);

		
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "Reference", "Reference", 0);
		JVS_INC_STD_DocMsg.ItemList.add(itemListTable, rootGroupName, "Third Party Text", "Third_Party_Text", 0);

	}

	private void GENDATA_getStandardGenerationData(Table argt) throws OException {

		Table sapIds = Util.NULL_TABLE;
		Table dealComments = Util.NULL_TABLE;
		try{
			Table eventdataTable  = JVS_INC_STD_DocMsg.getEventDataTable();
			Table gendataTable    = JVS_INC_STD_DocMsg.getGenDataTable();
			Table itemlistTable   = JVS_INC_STD_DocMsg.getItemListTable();

			if (gendataTable.getNumRows() == 0){
				gendataTable.addRow();
			}
			
			int tranNum = eventdataTable.getInt("tran_num", 1);
			
			sapIds = loadSapIds(tranNum);
			
			dealComments = loadDealComments(tranNum);

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
				} else if (internal_field_name.startsWith("From_Account_Business_Unit_Code")) {
					
					int infoRow = sapIds.unsortedFindString("type_name", "From A/C BU", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
					String strValue = infoRow > 0 ? sapIds.getString("bu_value", infoRow) : "";
					
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, strValue);				
				} else if (internal_field_name.startsWith("To_Account_Business_Unit_Code")) {
					
					int infoRow = sapIds.unsortedFindString("type_name", "To A/C BU", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
					String strValue = infoRow > 0 ? sapIds.getString("bu_value", infoRow) : "";
					
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, strValue);					
				} else if (internal_field_name.startsWith("From_Account_Legal_Entity_Code")) {
					
					int infoRow = sapIds.unsortedFindString("type_name", "From A/C BU", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
					String strValue = infoRow > 0 ? sapIds.getString("le_value", infoRow) : "";
					
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, strValue);					
				} else if (internal_field_name.startsWith("To_Account_Legal_Entity_Code")) {
					
					int infoRow = sapIds.unsortedFindString("type_name", "To A/C BU", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
					String strValue = infoRow > 0 ? sapIds.getString("le_value", infoRow) : "";
					
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, strValue);					
				} else if (internal_field_name.startsWith("Reference")) {
					
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, getComment("SAP Transfer 3rd Party Ref", dealComments));		
				} else if (internal_field_name.startsWith("Third_Party_Text"))  {
					
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, getComment("SAP Transfer 3rd Party", dealComments));		
				} else {
					// [n/a]
					JVS_INC_STD_DocMsg.GenData.setField(gendataTable, output_field_name, "[n/a]");
				}
			}
		
		}finally{
			if(Table.isTableValid(dealComments) == 1){
				dealComments.destroy();
			}
			if(Table.isTableValid(sapIds) == 1){
				sapIds.destroy();
			}
		}
	}
	
	private String getComment(String type, Table dealComments) throws OException {
		String comment = "";
		
		for(int i = 1; i <= dealComments.getNumRows(); i++) {
			if(type.equalsIgnoreCase(dealComments.getString("type_name", i))) {
				comment = comment + dealComments.getString("line_text", i);
			}
		}
		
		return comment;
	}
	
	private Table loadSapIds(int tranNum) throws OException {
		String sql = "SELECT abv.type_name, abv.value, p.party_id, piv.type_name AS bu_type, "
				+ " piv.value AS bu_value, piv_le.type_name AS le_type, piv_le.value AS le_value \n"
				+ " FROM ab_tran_info_view abv \n"
				+ " JOIN party p ON p.short_name = value  \n"
				+ " JOIN party_info_view piv ON piv.party_id = p.party_id AND piv.type_name IN ('Ext Business Unit Code', 'Int Business Unit Code') \n"
				+ " JOIN party_relationship pr ON pr.business_unit_id = p.party_id \n"
				+ " JOIN party_info_view piv_le ON piv_le.party_id = pr.legal_entity_id AND piv_le.type_name IN ('Ext Legal Entity Code', 'Int Legal Entity  Code') \n"
				+ " WHERE tran_num IN (SELECT value FROM ab_tran_info_view WHERE tran_num = " + tranNum + " AND type_name = 'Strategy Num') \n"
				+ " AND abv.type_name IN ('From A/C BU', 'To A/C BU') \n";

		Table tbl = Table.tableNew();
		Logging.debug("EXEC "+sql);
		DBaseTable.execISql(tbl, sql);

		return tbl;		
	
	}
	
	private Table loadDealComments(int tranNum) throws OException {
		String sql =  " SELECT type_id, type_name, line_num, line_text \n"
					+ " FROM tran_notepad \n "
					+ " JOIN deal_comments_type ON note_type = type_id \n "
					+ " WHERE tran_num = " + tranNum + " \n "
					+ " ORDER BY type_id, line_num \n ";
		
		
		Table tbl = Table.tableNew();
		Logging.debug("EXEC "+sql);
		DBaseTable.execISql(tbl, sql);

		return tbl;	
	}
	
}
