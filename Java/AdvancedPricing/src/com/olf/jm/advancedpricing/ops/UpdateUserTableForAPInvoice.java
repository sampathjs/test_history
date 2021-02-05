/*
 * File updated 05/02/2021, 17:53
 */

package com.olf.jm.advancedpricing.ops;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericOpsServiceListener;
import com.olf.jm.advancedpricing.model.ApUserTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.EnumOpsServiceType;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumBuySell;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2018-01-26	V1.0	 sma     - update user table rather than the 
 *                                    doc info.                                   
 */

/**
 * Tracking invoices Generated/Cancelled invoicing within Advanced Pricing
 * 
 * @version 1.0
 * @author sma
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcStldocProcess })
public class UpdateUserTableForAPInvoice extends AbstractGenericOpsServiceListener {


	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private static final String CONST_REPOSITORY_CONTEXT = "Util";
	
	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private static final String CONST_REPOSITORY_SUBCONTEXT = "Advanced Pricing Updater";
		
	@Override
	public void postProcess(Session session, EnumOpsServiceType type,
			ConstTable table, Table clientData) {
		
		try {
			init(session, this.getClass().getSimpleName());
			writeInvoicInfoToTable(session, table.getTable("data", 0));
		} catch (Exception e) {
			Logging.error("Error writing document information info to user table. " + e.getMessage());
		}finally{
			Logging.close();
		}
	}

	/**
	 * Write thestatus of all documents to user table USER_jm_sl_doc_tracking.
	 *
	 * @param documentData the document data passed into the script.
	 */
	private void writeInvoicInfoToTable(Session session, Table documentData) {
		
		int[] documentIds = documentData.getColumnValuesAsInt("document_num");
		
		for (int documentId : documentIds) {
			if (documentId > 0) {
				updateUserTbl(session, documentId, documentData);
			} else {
				Logging.info("No document created, no need to update the user table " + ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.getName() +".");
			}
		}
	}
	
	private void updateUserTbl(Session session, int documentId, Table documentData) {
		int findRow = documentData.find(0, documentId, 0);
		Table documentDetails = documentData.getTable("details", findRow);
		Table tblUpdate = null; 
		for(int row = 0; row < documentDetails.getRowCount(); row++) {
			// Check that the document is an invoice. 
			if(documentDetails.getInt("doc_type", row) != 1) {
				Logging.info("Skipping document " + documentId + " not an invoice.");
				return;
			}	
			
			long eventNum = documentDetails.getLong("event_num", row);
			int buySell = documentDetails.getInt("buy_sell", row);
			if(eventNum != 0 && buySell == EnumBuySell.Sell.getValue()) {
				int docStatusId = documentDetails.getInt("next_doc_status", row);
				Table tblLink  = session.getIOFactory().runSQL("SELECT * FROM " + ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.getName() 
						+ " WHERE sell_event_num = " + eventNum);

				if(tblLink.getRowCount()>0) {
					int lastRowNum;
					if(tblUpdate == null) {
						tblUpdate = tblLink.cloneData();
						lastRowNum = tblUpdate.getRowCount()-1;
					} else {
						tblUpdate.addRow();
						lastRowNum = tblUpdate.getRowCount()-1;
						tblUpdate.copyRowData(tblLink, 0, lastRowNum);
					}
					
					tblUpdate.setInt("invoice_doc_num", lastRowNum, documentId);
					tblUpdate.setString("invoice_status", lastRowNum, getDocStatusDesc(session, docStatusId));
					
					tblUpdate.setDate("last_update", lastRowNum, session.getServerTime());
					tblLink.dispose();
				}
				
			}
		}		
		if(tblUpdate != null && tblUpdate.getRowCount()>0){
			UserTable userTableLink = session.getIOFactory().getUserTable(ApUserTable.USER_TABLE_ADVANCED_PRICING_LINK.getName());	
			userTableLink.updateRows(tblUpdate, "buy_deal_num, sell_deal_num, metal_type, sell_event_num");	
			
			userTableLink.dispose();		
			tblUpdate.dispose();
		}
	}
	
	
	private String getDocStatusDesc(Session session, int docStatusId) {
		String sql = "select doc_status_desc from stldoc_document_status where doc_status = " + docStatusId; 
			//AND LEFTER OUTER JOIN with event info 

		    Table eventInfoTypeTbl = session.getIOFactory().runSQL(sql);
			String doc_status_desc = eventInfoTypeTbl.getString("doc_status_desc", 0);
		return doc_status_desc;
	}

	/**
	 * Initial plug-in log by retrieving logging settings from constants repository.
	 * @param class1 
	 * @param context
	 */
	private void init(Session session, String pluginName)  {	
		try {
			String abOutdir = Util.getEnv("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPOSITORY_CONTEXT, 
					CONST_REPOSITORY_SUBCONTEXT);
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", pluginName + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				Logging.init(this.getClass(), CONST_REPOSITORY_CONTEXT, 
						CONST_REPOSITORY_SUBCONTEXT);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			Logging.info(pluginName + " started");
		} catch (OException e) {
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
		}
	}

}