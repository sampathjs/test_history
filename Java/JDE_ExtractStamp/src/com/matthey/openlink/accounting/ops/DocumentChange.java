package com.matthey.openlink.accounting.ops;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.matthey.openlink.config.ConfigurationItemDocumentChange;
import com.matthey.openlink.enums.EnumUserJmSlDocTracking;
import com.matthey.openlink.reporting.ops.Sent2GLStamp;
import com.matthey.openlink.userTable.UserTableUtils;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericOpsServiceListener;
import com.olf.openrisk.application.EnumOpsServiceType;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-MM-DD	V1.0	pwallace	- Initial Version
 * 2016-07-26	V1.1	jwaechter	- removed processing of document.
 *                                    it's not necessary.
 * 2016-10-03   V1.2    scurran     - update user table rather than the 
 *                                    doc info.   
 * 2018-01-10   V1.3    sma     - add logic of column sap_status  
 */


/**
 * EPMM-1868 Capture Cancelled invoicing within SL reporting
 * 
 * When a document is generated set the status to 'Pending Sent' in the {@value #trackingTableName} table
 * 
 * When a document is cancelled if it has passed 'Sent' status in the {@value #trackingTableName} table
 * reset the value to 'Pending Cancelled'....
 * 
 * @version 1.2
 * @author scurran
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcStldocProcess })
public class DocumentChange extends AbstractGenericOpsServiceListener {

	

	private Session currentSession;

	@SuppressWarnings("serial")
	private static final Map<String, String> ledgerStatus = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put(Sent2GLStamp.STAMP_GL_VALIDATED,Sent2GLStamp.STAMP_GL_CANCELLING);
			put(Sent2GLStamp.STAMP_GL_PENDING, "NOT Sent");
		}
	});
	
	@Override
	public void postProcess(Session session, EnumOpsServiceType type,
			ConstTable table, Table clientData) {
		
		try {
			currentSession = session;
			init();
			writeResultsToTable(table.getTable("data", 0));
		} catch (Exception e) {
			PluginLog.error("Error writing sales ledger info to user table. " + e.getMessage());
		}
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception on initialisation errors or the logger or const repo.
	 */
	private void init() throws Exception {
		try {
			String logLevel = ConfigurationItemDocumentChange.LOG_LEVEL.getValue();
			String logFile = getClass().getSimpleName() + ".log";
			String logDir = ConfigurationItemDocumentChange.LOG_DIR.getValue();

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	

	/**
	 * Write thestatus of all documents to user table USER_jm_sl_doc_tracking.
	 *
	 * @param documentData the document data passed into the script.
	 */
	private void writeResultsToTable(Table documentData) {
		
		int[] documentIds = documentData.getColumnValuesAsInt(EnumUserJmSlDocTracking.DOCUMENT_NUM.getColumnName());

		Table trackingData = UserTableUtils.getTrackingData(documentIds, currentSession);
		
		for (int documentId : documentIds) {
			if (documentId > 0) {
				writeDocumentToTable(documentId, documentData, trackingData);
			}
		}
	}
	
	private void writeDocumentToTable(int documentId, Table documentData, Table trackingData) {
		
		String currentStatus = UserTableUtils.getCurrentStatus(documentId, trackingData);
		
		Table documentDetails = documentData.getTable("details", 0);
		
		if (currentStatus == null ) {
			// No entry found in the tracking table new entry
			
			// Check the document status to see if it's a new or cancelled doc.
			if(documentDetails.getInt("next_doc_status", 0) == 4) {
				PluginLog.info("No entry in the user table, creating a cancelled row for doc " + documentId);
				updateTrackingData(documentId, Sent2GLStamp.STAMP_GL_CANCELLING, documentDetails, true);
			} else {
				PluginLog.info("No entry in the user table, creating a new row for doc " + documentId);
				updateTrackingData(documentId, Sent2GLStamp.STAMP_GL_PENDING, documentDetails, true);
			}
			
		} else {
			// update the existing entry with a new status
			String newStatus = ledgerStatus.get(currentStatus);
			if(newStatus != null) {
				PluginLog.debug("Updating status " + currentStatus + " to " + newStatus);
				updateTrackingData(documentId, newStatus, documentDetails, false);
			}
		}
				
	}
	
	
	private void updateTrackingData(int documentId, String status, Table documentDetails, boolean newRow) {
		
		// Check that the document is an invoice. 
		if(documentDetails.getInt("doc_type", 0) != 1) {
			PluginLog.info("Skipping document " + documentId + " not an invoice.");
			return;
		}
		Table userTableData = UserTableUtils.createTableStructure(currentSession);
		
		userTableData.addRows(1);
		userTableData.setInt(EnumUserJmSlDocTracking.DOCUMENT_NUM.getColumnName(), 0, documentId);
		userTableData.setString(EnumUserJmSlDocTracking.SL_STATUS.getColumnName(), 0, status);
		userTableData.setString(EnumUserJmSlDocTracking.SAP_STATUS.getColumnName(), 0, status);
		userTableData.setDate(EnumUserJmSlDocTracking.LAST_UPDATE.getColumnName(), 0, currentSession.getServerTime());
		userTableData.setInt(EnumUserJmSlDocTracking.PERSONNEL_ID.getColumnName(), 0, currentSession.getUser().getId()); 
		userTableData.setInt(EnumUserJmSlDocTracking.DOC_STATUS.getColumnName(), 0, documentDetails.getInt("next_doc_status", 0));
		userTableData.setInt(EnumUserJmSlDocTracking.LAST_DOC_STATUS.getColumnName(), 0, documentDetails.getInt("curr_doc_status", 0));
		userTableData.setInt(EnumUserJmSlDocTracking.DOC_VERSION.getColumnName(), 0, documentDetails.getInt("doc_version", 0));
		userTableData.setInt(EnumUserJmSlDocTracking.STLDOC_HDR_HIST_ID.getColumnName(), 0, documentDetails.getInt("stldoc_hdr_hist_id", 0));
		
		UserTable userTable = currentSession.getIOFactory().getUserTable(UserTableUtils.trackingTableName);
		if (newRow) {
			PluginLog.info("inserting (" + documentId + ", " + status + ")");
			userTable.insertRows(userTableData);			
		} else {
			PluginLog.info("updating (" + documentId + ", " + status + ")");
			userTable.updateRows(userTableData, EnumUserJmSlDocTracking.DOCUMENT_NUM.getColumnName());
		}		
	}
	
	
	

	

	

	



}
