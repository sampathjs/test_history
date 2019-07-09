package com.olf.jm.reportbuilder;
// Hello World - testing Code Deployment - Jira 852 made correction

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;

public class DealDocumentLocationLink implements IScript {

	@Override
	public void execute(IContainerContext arg0) throws OException {
		Table returnt = arg0.getReturnTable();
		Table argt = arg0.getArgumentsTable();
		Table temp = Util.NULL_TABLE;
		Table dealList = Util.NULL_TABLE;
		
		try {
			Table tblTemp = argt.getTable("PluginParameters", 1);
			String dealTrackingNum = tblTemp.getString("parameter_value", tblTemp.unsortedFindString("parameter_name", "tradeRef", SEARCH_CASE_ENUM.CASE_INSENSITIVE));
			String reference = tblTemp.getString("parameter_value", tblTemp.unsortedFindString("parameter_name", "reference", SEARCH_CASE_ENUM.CASE_INSENSITIVE));
			if (reference==null){
				reference = "Batch Spec";
			}

			String sql = "SELECT ddl.doc_id,ab.tran_num, ddl.deal_tracking_num, do.file_object_type deal_doc_type, ddl.saved_node_id, do.file_object_name,\n" +
						 " do.file_object_source, do.file_object_reference reference,do.file_object_comment link_comment, ddl.user_id,ddl.time_stamp,do.file_object_link_type,\n" +
						 " doc_id sortnum, file_object_source + '\' + file_object_name saved_link, 0 import_pending\n" +
						 " FROM deal_document_link ddl\n" +
					 	 "    JOIN file_object do on (do.node_id = ddl.saved_node_id)\n" +
					 	 "    JOIN ab_tran ab ON (ab.deal_tracking_num = ddl.deal_tracking_num and ab.current_flag = 1)\n" +
						 " WHERE ddl.deal_tracking_num =  " + dealTrackingNum + "\n" +
						 "    AND do.file_object_reference = '" + reference + "'";

			dealList = Table.tableNew();

			DBaseTable.execISql(dealList, sql);

			temp = Table.tableNew();
			temp.select(dealList, "*", "deal_tracking_num GT 0");
			temp.group("reference, doc_id");
			temp.groupSum("reference");
			temp.clearDataRows();
			returnt.select(temp, "*", "deal_tracking_num GT 0");
		} catch (Exception e) {
			System.out.println(e.getMessage());			
		} finally {
			if (Table.isTableValid(temp)!=0){
				temp.destroy();
			}			
			if (Table.isTableValid(dealList)!=0){
				dealList.destroy();
			}
		}
	}

}
