/********************************************************************************
Status:         completed

Revision History:
1.0  - 2010-12-08 - eikesass - initial version
1.1  - 2012-07-19 - qwang    - bugfixing in method prepareDocTypeIdsForDbAccess
                              to build the document type ids.
1.2  - 2013-03-01 - eikesass - fix: query result table in v11
1.3  - 2013-08-28 - jbonetzk - revised table handling, sql for MSSQL, use of PluginLog
1.4  - 2014-03-31 - jbonetzk - full review
1.5  - 2014-09-30 - jbonetzk - don't auto-output processed documents anymore
1.6  - 2016-08-12 - scurran  - created JM version of the file, need additional error 
                              handling around doc process.
1.7	 - 2016-09-02 - jwaechter - Added logic to rerun until all deals that can be processed are processed.                              
1.8  - 2016-09-20 - jwaechter - added exit of loop in case core method StlDoc.processDocs
1.9  - 2017-02-14 - jwaechter - Added max retry count for loop.
1.10 - 2017-02-15 - jwaechter - moved counter-- to start of try block, adjusted counter initialisation
 **********************************************************************************/

package com.openlink.jm.bo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * For each entry in the user table "user_bo_auto_generate" for one of the given 
 * doc types this script processes all documents given by query to the specified 
 * "next_doc_status" 
 *
 * caller require security privilege 43308 (Settlement Desktop - AVS: Process)
 * 
 * @author eikesass
 * @version 1.10
 * @category none
 */
//@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions = false)
public class JM_AutomatedDocumentProcessing implements IScript 
{
	// process related constants
	private final static String DEFINITION_TABLE_NAME       = "user_bo_auto_doc_process";
	private final static int    SEC_PRIV_STLDESKTOP_PROCESS = 44308;
	private final static int 	MAX_RETRY_COUNT = 4;

	// frequently used constants
	private final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

	private ConstRepository constRepo;
	private final boolean output_all = false;

	protected String getConstRepoSubcontext()
	{
		return "Auto Document Processing";
	}

	public void execute(IContainerContext context) throws OException
	{
		// initializes constants that are retrieved from constants repository
		String subContext = getConstRepoSubcontext();
		constRepo= new ConstRepository("BackOffice", subContext);

		String
		logLevel = constRepo.getStringValue("logLevel", "Error"),
		logFile  = constRepo.getStringValue("logFile", getClass().getSimpleName() + ".log"),
		logDir   = constRepo.getStringValue("logDir", null);

		try
		{
			if (logDir == null) PluginLog.init(logLevel);
			else PluginLog.init(logLevel, logDir, logFile);
		}
		catch (Exception e)
		{
			OConsole.oprint("Unable to initialise PluginLog");
		}

		try
		{
			process(context, logLevel, logDir, logFile);
		}
		catch (Throwable t)
		{
			PluginLog.error(PluginLog.LogLevel.DEBUG.equals(PluginLog.getLogLevel())
					? t.toString() : t.getMessage());
			throw new OException(t.getMessage());
		}
		finally
		{
			PluginLog.exitWithStatus();
		}
	}

	private void process(IContainerContext context, String logLevel, String logDir, String logFile) throws Throwable
	{
		ensureUserMayProcessDocuments();

		List<String> docTypeNames = Arrays.asList(
				constRepo.getStringValue("Document Type", "")
				.trim().replaceAll("\\s*,\\s*", ",").split(","));
		String docTypeIds = prepareDocTypeIdsForDbAccess(docTypeNames);

		PluginLog.debug("Processing document type(s) " + docTypeIds);

		Table configurations = loadConfigurationTable(docTypeIds);
		//		PluginLog.debug(configurations);

		processDocuments(configurations, logLevel, logDir, logFile);
		configurations.destroy();
	}

	private void processDocuments(Table definitions, String logLevel, String logDir, String logFile) throws Throwable
	{
		// Define Variables For Loop
		int numRows = definitions.getNumRows();
		int definitionId;
		int nextDocStatus;
		String queryName;
		Table events = null;
		boolean isDebug = PluginLog.LogLevel.DEBUG.equals(PluginLog.getLogLevel());

		PluginLog.debug("Looping thru "+numRows+" definition"+(numRows!=1?"s":"")+" ...");
		// For each definition that has to be processed
		Map<String, Throwable> issues = new HashMap<>();
		for (int iCount = 1; iCount <= numRows; iCount++)
		{
			queryName = "unknown";
			boolean terminate=false;
			List<Integer> dealsToExclude = new ArrayList<>();
			int counter = MAX_RETRY_COUNT+1;
			do {
				try {
					counter--;
					nextDocStatus = definitions.getInt("next_doc_status", iCount);
					definitionId  = definitions.getInt("def_id", iCount);
					queryName     = definitions.getString("query_name", iCount);

					if (isDebug) // prevent from generating message otherwise
						PluginLog.debug("Looping for SD Definition: "+Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DEFINITIONS_TABLE, definitionId)+"(id:"+definitionId+")"
								+ " / SD Query: "+queryName+" / Next Status: "+Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, nextDocStatus)+"(id:"+nextDocStatus+")");

					events = loadEvents(queryName, dealsToExclude);

					// do only process non-empty event table	
					if (events.getNumRows() > 0 && counter >= 0) {
						setNextDocStatus(events, nextDocStatus);
						processSingleStep(events, definitionId, dealsToExclude, logLevel, logDir, logFile);
					} else {
						if (counter < 0) {
							PluginLog.warn("Terminating processing of documents as max number of attempts are exceeded.");
							PluginLog.warn(events.toXhtml());
							PluginLog.warn("Excluded deals: " + dealsToExclude);
						}
						terminate = true;
					}
					events.destroy();				
				} catch (Throwable t) {
					PluginLog.error("Error processing query " + queryName);
					PluginLog.error(t.toString());
					for (StackTraceElement ste : t.getStackTrace()) {
						PluginLog.error(ste.toString());
					}
					issues.put(queryName, t);
				}
			} while (!terminate);

		}
		if (issues.size() > 0) {
			throw new RuntimeException ("In total " + issues.size() + " definitions of " 
					+ numRows + " encountered errors. Please check log file for details."
					+ "Exception Summary: " + issues.toString()
					);
		}
	}

	/**
	 * processes one single step of the list of process steps from the 
	 * configuration list
	 * @param events
	 * @param definitionId
	 * @param dealsToExclude 
	 * @param logFile 
	 * @param logDir 
	 * @param logLevel 
	 * @throws Throwable 
	 */
	private void processSingleStep(Table events, int definitionId, List<Integer> dealsToExclude, String logLevel, String logDir, String logFile) throws Throwable
	{
		int rows = events.getNumRows();
		String sNumEvents = ""+rows+" "+(rows == 1 ? "event" : "events");

		Table processTable = Table.tableNew("Process Table");
		try
		{
			Table outputResults = Table.tableNew();

			int result = StlDoc.processDocs(definitionId, (output_all?1:0), events, processTable, outputResults);
			if (logDir == null) PluginLog.init(logLevel);
			else PluginLog.init(logLevel, logDir, logFile);
			if (result != OLF_RETURN_SUCCEED) {
				dealsToExclude.add(-1);
				throw new Throwable("Error processing docs. ");				
			}

			// Check the process table to see if the document was successfully processed. 
			Table processResults = Table.tableNew();
			processResults.select(processTable, "DISTINCT, deal_tracking_num, document_num, last_process_status, last_process_message", "event_num GT 0 AND last_process_status EQ -12");

			if(processResults.getNumRows() > 0) {
				// Error detected processing one or more documents. 
				int numErrors = processResults.getNumRows();
				StringBuffer errorMessage = new StringBuffer();
				errorMessage.append("Error processing the following deal(documents)[error message] "); 
				for(int errorRow = 1; errorRow <= numErrors; errorRow++) {
					if(errorRow > 1) {
						errorMessage.append(", ");
					}
					dealsToExclude.add(processResults.getInt("deal_tracking_num", errorRow));

					errorMessage.append(String.format("%d(%d)[%s]", processResults.getInt("deal_tracking_num", errorRow),
							processResults.getInt("document_num", errorRow),
							processResults.getString("last_process_message", errorRow)));
				}
				PluginLog.error(errorMessage.toString());
				throw new OException(errorMessage.toString());
			}
			PluginLog.debug(sNumEvents+" successfully processed");
		}
		catch (Throwable t)
		{
			PluginLog.error("Error processing "+sNumEvents);
			throw t;
		}
		finally { processTable.destroy(); processTable = null; }
	}

	/**
	 * loads all relevant events to be processed. events are defined by the 
	 * query in the configuration table.
	 * You can define deals to exclude in case they can't be processed.
	 * @param queryName
	 * @param dealsToExclude
	 * @return
	 * @throws OException
	 */
	private Table loadEvents(String queryName, List<Integer> dealsToExclude) throws OException
	{
		PluginLog.debug("Loading events using Saved Query '" + queryName + 
				"', excluding the following deal_tracking_numbers: " + dealsToExclude);

		Table tbl = Table.tableNew();
		int ret = Query.executeDirect (queryName, null, tbl, "ab_tran_event.event_num, ab_tran_event.tran_num");// 'tran_num' is not really used, but retrieved for information purposes
		if (ret != OLF_RETURN_SUCCEED) {
			tbl.destroy();
			throw new OException("Failed when executing Query '" + queryName + "'(code "+ret+")");
		}
		tbl.setTableTitle("Queried Events");
		if (dealsToExclude.size() > 0) {
			String sql = generateExclusionSql(dealsToExclude);
			Table dealToTranMap = Table.tableNew();
			ret = DBaseTable.execISql(dealToTranMap, sql);
			if (ret != OLF_RETURN_SUCCEED) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Failed to execute sql '" + sql + "'");
				dealToTranMap.destroy();
				tbl.destroy();
				throw new OException(message);
			}
			dealToTranMap.deleteWhere("tran_num", tbl, "tran_num");
			dealToTranMap.destroy();
		}
		if (dealsToExclude.contains(-1)) {
			tbl.clearRows();
		}
		int rows = tbl.getNumRows();
		String sNumEvents = ""+rows+" "+(rows == 1 ? "event" : "events");
		PluginLog.debug("Query '"+queryName+"' returned "+sNumEvents);
		return tbl;
	}

	private String generateExclusionSql(List<Integer> dealsToExclude) {
		String sql = "\nSELECT ab.tran_num, ab.deal_tracking_num"
				+	 "\nFROM ab_tran ab"
				+	 "\nWHERE ab.deal_tracking_num IN (";
		boolean first = true;
		for (int dealTrackingNum : dealsToExclude) {
			if (!first) {
				sql += ",";
			} 
			sql += dealTrackingNum;
			first = false;
		}
		sql 	   += 	 ")";
		return sql;
	}

	private void setNextDocStatus(Table events, int nextDocStatus) throws OException
	{
		int rows = events.getNumRows();
		String sNumEvents = ""+rows+" "+(rows == 1 ? "event" : "events");

		PluginLog.debug("Setting next document status " + nextDocStatus + " for " + sNumEvents);

		events.addCol("next_doc_status", COL_TYPE_ENUM.COL_INT);
		events.setColValInt("next_doc_status", nextDocStatus);
	}

	private String prepareDocTypeIdsForDbAccess(List<String> docTypeNames) throws OException
	{
		String docTypeIds = "";
		int id;

		for (String name: docTypeNames)
		{
			id = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, name);

			if (id >= 0)
				docTypeIds += "," + id;
			else
				PluginLog.error("Document type '" + name + "' is invalid and will be skipped.");
		}

		// remove the very first comma
		if (docTypeIds.trim().length() > 0)
			docTypeIds = docTypeIds.substring(1);

		return docTypeIds;
	}

	/**
	 * checks if user is allowed to process settlement documents via script
	 * @throws OException
	 */
	private void ensureUserMayProcessDocuments() throws OException
	{
		if (Util.userCanAccess(SEC_PRIV_STLDESKTOP_PROCESS) != 1)
		{
			String message = "User " + Ref.getUserName() + " is not allowed to process documents. Security Right 44308 is missing";
			throw new OException(message);
		}
	}

	/**
	 * retrieves all definitions from user table for the given document types
	 * @param docTypeIds
	 * @return
	 */
	private Table loadConfigurationTable(String docTypeIds) throws OException
	{
		// Get all definitions to be processed from User Table
		StringBuilder sSql = new StringBuilder("SELECT * ");
		sSql.append(" FROM " + DEFINITION_TABLE_NAME);
		sSql.append(" WHERE active = 1");
		if (!docTypeIds.isEmpty())
			sSql.append(" AND doc_type in (" + docTypeIds + ")");
		sSql.append(" ORDER BY sequence");

		Table definitions = Table.tableNew();
		if (DBaseTable.execISql(definitions, sSql.toString()) != 1)
		{
			String message = "Error executing sql statement \"" + sSql + "\"";
			throw new OException(message);
		}
		return definitions;
	}
}
