package com.openlink.jm.bo;

import java.util.ArrayList;
import java.util.List;

import com.matthey.utilities.Utils;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * 
 * There have been errors reported during Automated Confirmation Processing due to TPM engine crash.
 * Sometimes it corrupts the Documents and the document needs to be processed into same doc status again manually.
 * It has been observed for Confirmations in 'Generated' status (Ivanti P1396).
 * 
 * This script automates the manual processing of corrupt documents in same doc status.
 * 
 * Const repo Configurations:
 * 1. Query Name
 * 2. Applicable Doc Status: This is multi select configuration and the scripts only attempts to re-process documents in these statuses
 * 3. Definition Name
 * 
 * Revision History:
 * Version		Updated By			Date		Ticket#			Description
 * -----------------------------------------------------------------------------------
 * 	01			Saurabh Jain	06-Dec-2019					Initial version
 *  02			Arjit Agrawal	20-Apr-2020					Replaced personnelId var with email_recipients constant repo variable
 */
@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class JM_ErrorDocumentProcessing extends JM_AutomatedDocumentProcessing {

	private String logLevel;
	private String logFile;
	private String logDir;
	private String taskName = "";

	protected String getConstRepoSubcontext() throws OException {
		Table refInfo = Util.NULL_TABLE;
		
		try {
			refInfo = Ref.getInfo();
			taskName = refInfo.getString("task_name", 1);
			OConsole.oprint("Script trigerred by Task " + taskName);
			return taskName;
		} finally {
			if (Table.isTableValid(refInfo) == 1) {
				refInfo.destroy();
			}
		}
	}

	public void execute(IContainerContext context) throws OException {
		
		String subContext = getConstRepoSubcontext();
		constRepo= new ConstRepository("BackOffice", subContext);

		logLevel 	= constRepo.getStringValue("logLevel", "Info");
		logFile 	= constRepo.getStringValue("logFile", getClass().getSimpleName() + ".log");
		logDir  	= constRepo.getStringValue("logDir", null);

		try {
			Logging.init(this.getClass(), "BackOffice", subContext);
			
		} catch (Exception e)	{
			throw new OException("Unable to initialise Logging");
		}

		try {
			Logging.info("Script trigerred by Task " + this.taskName);
			Logging.info("Starting JM_ErrorDocumentProcessing" );
			ensureUserMayProcessDocuments();
			processErrorDocuments();
			Logging.info("Ending JM_ErrorDocumentProcessing execution." );
			
		} catch (Throwable t)		{
			Logging.error(t.getMessage());
			throw new OException(t.getMessage());
			
		} finally {
			Logging.close();
		}
	}

	private void processErrorDocuments() throws OException {
		
		Table events = Util.NULL_TABLE;
		Table eventsToProcess = Util.NULL_TABLE;
		List<Integer> dealsToProcess = new ArrayList<>();
		List<Integer> dealsToExclude = new ArrayList<>();
		
		try {
			String queryName = constRepo.getStringValue("queryName", "Confirms: Processing Errors");

			events = loadEvents(queryName);
			
			int eventCount = events.getNumRows();
			if(eventCount <= 0 ) {
				Logging.info("No Error Documents found for re-processing by Task# " + taskName );
				return;
			}
			
			List<Integer> applicableDocStatusList = getApplicableDocStatus();

			eventsToProcess = Table.tableNew();
			eventsToProcess.addCol("event_num", COL_TYPE_ENUM.COL_INT64);
			eventsToProcess.addCol("next_doc_status", COL_TYPE_ENUM.COL_INT);
			for(int row = 1; row <= eventCount; row++) {
				
				int currentStatus = events.getInt("doc_status", row);
				if(applicableDocStatusList.contains(currentStatus)) {
					dealsToProcess.add(events.getInt("deal_num", row));
					int pRow = eventsToProcess.addRow();
					eventsToProcess.setInt64("event_num", pRow, events.getInt64("event_num", row));
					eventsToProcess.setInt("next_doc_status", pRow, currentStatus);
				}
			}
			
			if(eventsToProcess.getNumRows() <= 0 ) {
				Logging.info(String.format("No Error Documents filtered for re-processing by task# %s,  applicableDocStatusList = %s",taskName, applicableDocStatusList) );
				return;
			}

			String errorMessage = processErrorEvents(eventsToProcess, dealsToExclude);
			sendEmail(events, dealsToProcess, dealsToExclude, errorMessage);
			
		} finally {
			if (Table.isTableValid(events) == 1) {
				events.destroy();
				events = Util.NULL_TABLE;
			}
			if (Table.isTableValid(eventsToProcess) == 1) {
				eventsToProcess.destroy();
				eventsToProcess = Util.NULL_TABLE;
			}
		}
		
	}

	/**
	 * Read the applicable doc status from Const Repo
	 * @return
	 * @throws OException
	 */
	private List<Integer> getApplicableDocStatus() throws OException {
		Table applicableDocStatusTable = Util.NULL_TABLE;
		List<Integer> applicableDocStatusList = new ArrayList<>();

		try {
			applicableDocStatusTable = constRepo.getMultiStringValue("applicableDocStatus");
			int numRows = applicableDocStatusTable.getNumRows();
			
			Logging.info("Num of applicable status for re-processing in same Status = " + numRows);
			for(int row = 1; row <= numRows; row++) {
				String docStatus = applicableDocStatusTable.getString(1, row);
				Logging.info("Applicable status " + docStatus);
				applicableDocStatusList.add(Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, docStatus));
				
			}
			
		} finally {
			if (Table.isTableValid(applicableDocStatusTable) == 1) {
				applicableDocStatusTable.destroy();
				applicableDocStatusTable = Util.NULL_TABLE;
			}			
		}
		
		return applicableDocStatusList;
	}

	private Table loadEvents(String queryName) throws OException {
		Logging.info("Loading events using Saved Query '" + queryName + "'");
		
		int queryId = 0;
		Table events = Util.NULL_TABLE;
		try {
			queryId = Query.run(queryName);
			events = Table.tableNew();
			String sqlQuery = 
				"SELECT \n" +
					"d.event_num, h.doc_status, d.deal_tracking_num as deal_num, d.document_num \n" +
				"FROM "  + Query.getResultTableForId(queryId) + " qr \n" +
				"JOIN stldoc_details d ON qr.query_result = d.event_num \n" +
				"JOIN stldoc_header h on h.document_num = d.document_num \n" +
				"WHERE qr.unique_id = " + queryId;
			
			int ret = DBaseTable.execISql(events, sqlQuery);
			
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException("Unable to run query: " + sqlQuery);
			}

			
		} finally {
			if (queryId > 0) {
				Query.clear(queryId);
			}
		}
		return events;
	}
	
	private String processErrorEvents(Table eventsToProcess, List<Integer> dealsToExclude) throws OException {
		
		String errorMessage = null;
		String definitionName = constRepo.getStringValue("definitionName", "Confirms");
		Logging.info("Processing " + eventsToProcess.getNumRows() + " Event(s). definitionName = " + definitionName);
		int defId = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DEFINITIONS_TABLE, definitionName);
		try {
			processSingleStep(eventsToProcess, defId, dealsToExclude, logLevel, logDir, logFile);
		} catch (Throwable t) {
			errorMessage = t.getMessage();
		}
		
		return errorMessage;
	}
	
	private void sendEmail(Table events, List<Integer> dealsToProcess, List<Integer> dealsToExclude, String errorMessage) throws OException {
		
		Table personnel = Util.NULL_TABLE;
		Logging.info("Preparing Email. Failed Deals = " + dealsToExclude);
		
		StringBuilder emailBody = new StringBuilder("Dear Colleague,<br>");
		emailBody.append("Status of Error Document Processing into the same Doc Status by Task :<br><br>" + taskName);
		emailBody.append("<table border = 2>");
		emailBody.append("<tr><b>").append("<td>Deal Num</td>").append("<td>Document Num</td>").append("<td>Event Num</td>").append("<td>Doc Status</td>").append("<td>Comments</td></b></tr>");
		
		int numEvents = events.getNumRows();
		for(int row = 1; row <= numEvents; row++) {
			
			int dealNum = events.getInt("deal_num", row);
			int docNum = events.getInt("document_num", row);
			int eventNum = events.getInt("event_num", row);
			String docStatus = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, events.getInt("doc_status", row));
			
			String comment = "Skipped Processing";
			if(dealsToExclude.contains(dealNum)) {
				comment = "Failed Processing";
			} else if(dealsToProcess.contains(dealNum)) {
				comment = "Attempted Processing";
			}
			
			emailBody.append("<tr><td>").append(dealNum).append("</td>")
			.append("<td>").append(docNum).append("</td>")
			.append("<td>").append(eventNum).append("</td>")
			.append("<td>").append(docStatus).append("</td>")
			.append("<td>").append(comment).append("</td></tr>");
		}
		
		emailBody.append("</table><br>");
		if (errorMessage != null) {
			emailBody.append(errorMessage);
		}
		
		String recipients = constRepo.getStringValue("email_recipients");
		Utils.sendEmail(recipients, "Error Confirmation Re-Processing Status", emailBody.toString(), "", "Mail");
	}

}
