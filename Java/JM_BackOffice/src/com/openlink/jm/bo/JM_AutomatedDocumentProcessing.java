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
import com.olf.jm.logging.Logging;

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
public class JM_AutomatedDocumentProcessing implements IScript  {
	
	// process related constants
	private final static String DEFINITION_TABLE_NAME       = "user_bo_auto_doc_process";
	protected final static int    SEC_PRIV_STLDESKTOP_PROCESS = 44308;
	private final static int 	MAX_RETRY_COUNT = 4;

	// frequently used constants
	private final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

	protected ConstRepository constRepo;
	private final boolean output_all = false;

	protected String getConstRepoSubcontext() throws OException {
		return "Auto Document Processing";
	}
	
	protected int getProcessingIteration() {
		return 0;
	}
	
		
	public void execute(IContainerContext context) throws OException {
		
		// initializes constants that are retrieved from constants repository
		String subContext = getConstRepoSubcontext();
		constRepo= new ConstRepository("BackOffice", subContext);

		String logLevel = constRepo.getStringValue("logLevel", "Error");
		String logFile  = constRepo.getStringValue("logFile", getClass().getSimpleName() + ".log");
		String logDir   = constRepo.getStringValue("logDir", null);

		try {
			Logging.init(this.getClass(), "BackOffice", subContext);
		} catch (Exception e)	{
			OConsole.oprint("Unable to initialise Logging");
		}

		try {
			int secondsPastMidnight = Util.timeGetServerTime();
			int processingIteration = getProcessingIteration();
			Logging.info("Starting JM_AutomatedDocumentProcessing script execution... For: "  + subContext + " Iteration: " + processingIteration );
			process(context, logLevel, logDir, logFile,processingIteration);
			int timeTaken = Util.timeGetServerTime() - secondsPastMidnight ;
			String timeTakenDisplay = getTimeTakenDisplay (timeTaken);
			Logging.info("Ending JM_AutomatedDocumentProcessing script execution...For: "  + subContext + " Iteration: " + processingIteration + " Time Taken:" + timeTakenDisplay );
			
		} catch (Throwable t)		{
			Logging.error(t.getMessage());
			throw new OException(t.getMessage());
			
		} finally {
			Logging.close();
		}
	}


	private void process(IContainerContext context, String logLevel, String logDir, String logFile, int processingIteration) throws Throwable {
		
		ensureUserMayProcessDocuments();

		String docType =constRepo.getStringValue("Document Type", "");
		Logging.info("DocTypes retrieved from Const Repo are '" + docType + "'");
		
		List<String> docTypeNames = Arrays.asList(docType.trim().replaceAll("\\s*,\\s*", ",").split(","));
		String docTypeIds = prepareDocTypeIdsForDbAccess(docTypeNames);

		if (docTypeIds.length() > 0) {
			Logging.debug("Processing document type(s) " + docTypeIds);
			Table configurations = null;
			
			try {
				configurations = loadConfigurationTable(docTypeIds, processingIteration);
				Logging.info(configurations.getNumRows() + " definition retrieved from user table '"  + DEFINITION_TABLE_NAME + "' for docType '" + docType + "'");
				processDocuments(configurations, logLevel, logDir, logFile);
				//configurations.destroy();
				
			} finally {
				if (configurations != null && Table.isTableValid(configurations) == 1) {
					configurations.destroy(); 
					configurations = null;
				}
			}
		} else {
			Logging.error("Error Determining document type(s): " + docType);
		}
	}

	private void processDocuments(Table definitions, String logLevel, String logDir, String logFile) throws Throwable {
		
		// Define Variables For Loop
		int numRows = definitions.getNumRows();
		int definitionId;
		int nextDocStatus;
		String queryName;
		String defName;
		Table events = null;
		

		Logging.debug("Looping thru "+numRows+" definition"+(numRows!=1?"s":"")+" ...");
		// For each definition that has to be processed
		Map<String, Throwable> issues = new HashMap<>();
		for (int iCount = 1; iCount <= numRows; iCount++) {
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
					defName = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DEFINITIONS_TABLE, definitionId);

					if (queryName == null || queryName.length() == 0) {
						String msg = "Invalid query name '" + queryName + "' found for definition->" + defName;
						throw new OException(msg);
						
					} else {
						
							// prevent from generating message otherwise
							Logging.debug("Looping for SD Definition: " + defName + "(id:"+definitionId+")"
									+ " / SD Query: "+queryName+" / Next Status: "+Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, nextDocStatus)+"(id:"+nextDocStatus+")");
						
						events = loadEvents(queryName, dealsToExclude);

						// do only process non-empty event table	
						if (events.getNumRows() > 0 && counter >= 0) {
							setNextDocStatus(events, nextDocStatus);
							processSingleStep(events, definitionId, dealsToExclude, logLevel, logDir, logFile);
						} else {
							if (counter < 0) {
								Logging.warn("Terminating processing of documents as max number of attempts are exceeded.");
								Logging.warn(events.toXhtml());
								Logging.warn("Excluded deals: " + dealsToExclude);
							}
							terminate = true;
						}
						//events.destroy();	
					}
								
				} catch (Throwable t) {
					terminate = true;
					Logging.error("Error processing query " + queryName);
					Logging.error(t.toString());
					for (StackTraceElement ste : t.getStackTrace()) {
						Logging.error(ste.toString());
					}
					issues.put(queryName, t);
					
				} finally {
					if (events != null && Table.isTableValid(events) == 1) {
						events.destroy();
						events = null;
					}
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
	protected void processSingleStep(Table events, int definitionId, List<Integer> dealsToExclude, String logLevel, String logDir, String logFile) throws Throwable {
		int rows = events.getNumRows();
		String sNumEvents = ""+rows+" "+(rows == 1 ? "event" : "events");
		String defName = "";
		Table processResults = null;
		Table outputResults = null;
		Table processTable = null;
		String timeTakenDisplay = "";
		
		try {
			processTable = Table.tableNew("Process Table");
			outputResults = Table.tableNew();
			defName = Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DEFINITIONS_TABLE, definitionId);
			
			int secondsPastMidnight = Util.timeGetServerTime();
			int result = StlDoc.processDocs(definitionId, (output_all?1:0), events, processTable, outputResults);
			
			int timeTaken = Util.timeGetServerTime() - secondsPastMidnight ;
			timeTakenDisplay = getTimeTakenDisplay (timeTaken);
			
			
			if (result != OLF_RETURN_SUCCEED) {
				dealsToExclude.add(-1);
				String errorMsg = DBUserTable.dbRetrieveErrorInfo(result, "Failed to process documents using StlDoc.processDoc API for definition->" + defName);
				throw new Throwable(errorMsg);	
				//throw new Throwable("Error processing docs for definitionId->" + definitionId + " using StlDoc.processDoc API ->"  );
			}

			// Check the process table to see if the document was successfully processed.
			Logging.info("Checking processTable to see if the documents were successfully processed for definition->" + defName);
			processResults = Table.tableNew();
			processResults.select(processTable, "DISTINCT, deal_tracking_num, document_num, last_process_status, last_process_message", "event_num GT 0 AND last_process_status EQ -12");

			if(processResults.getNumRows() > 0) {
				// Error detected processing one or more documents. 
				int numErrors = processResults.getNumRows();
				Logging.info(numErrors + " error rows identified with last_process_status = -12, detecting error in processing documents for definition->" + defName);
				
				StringBuffer errorMessage = new StringBuffer();
				errorMessage.append("Error processing the following deal(documents)[error message] "); 
				
				for(int errorRow = 1; errorRow <= numErrors; errorRow++) {
					if(errorRow > 1) {
						errorMessage.append(", ");
					}
					dealsToExclude.add(processResults.getInt("deal_tracking_num", errorRow));

					errorMessage.append(String.format("%d(%d)[%s]", processResults.getInt("deal_tracking_num", errorRow), 
							processResults.getInt("document_num", errorRow), processResults.getString("last_process_message", errorRow)));
				}
				Logging.error(errorMessage.toString());
				throw new OException(errorMessage.toString());
				
			} else {
				Logging.info("No error rows identified with last_process_status = -12, while processing documents for definition->" + defName);
				Logging.info(sNumEvents + " successfully processed" + " Time taken to process: " + timeTakenDisplay);
			}
			//Logging.debug(sNumEvents+" successfully processed");
		} catch (Throwable t) {
			Logging.error("Error processing "+sNumEvents + " for definition->" + defName + " Time taken to process: " + timeTakenDisplay);
			throw t;
		} finally { 
			if (processTable != null && Table.isTableValid(processTable) == 1) {
				processTable.destroy(); processTable = null; 
			}
			if (outputResults != null && Table.isTableValid(outputResults) == 1) {
				outputResults.destroy(); outputResults = null; 
			}
			if (processResults != null && Table.isTableValid(processResults) == 1) {
				processResults.destroy(); processResults = null; 
			}
		}
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
	private Table loadEvents(String queryName, List<Integer> dealsToExclude) throws OException {
		int ret;
		Table tbl = null;
		Table dealToTranMap = null;
		
		if (dealsToExclude.size() > 0) {
			Logging.info("Loading events using Saved Query '" + queryName + "', excluding the following deal_tracking_numbers: " + dealsToExclude);
		} else {
			Logging.info("Loading events using Saved Query '" + queryName );
		}
		
		try {
			//tbl = Table.tableNew();
			tbl = executeSavedQuery(queryName);
			
			tbl.setTableTitle("Queried Events");
			if (dealsToExclude.size() > 0) {
				String sql = generateExclusionSql(dealsToExclude);
				
				dealToTranMap = Table.tableNew();
				ret = DBaseTable.execISql(dealToTranMap, sql);
				if (ret != OLF_RETURN_SUCCEED) {
					String message = DBUserTable.dbRetrieveErrorInfo(ret, "Failed to execute sql '" + sql + "'");
					//dealToTranMap.destroy();
					//tbl.destroy();
					throw new OException(message);
				}
				dealToTranMap.deleteWhere("tran_num", tbl, "tran_num");
				//dealToTranMap.destroy();
			}
			if (dealsToExclude.contains(-1)) {
				tbl.clearRows();
			}
			
			int rows = tbl.getNumRows();
			String sNumEvents = ""+rows+" "+(rows == 1 ? "event" : "events");
			Logging.info("Query '"+queryName+"' returned "+sNumEvents);
			
		} catch (OException oe) {
			if (tbl != null && Table.isTableValid(tbl) == 1) {
				tbl.destroy(); tbl = null;
			}
			Logging.error("Error in loadEvents method->" + oe.getMessage());
			throw oe;
			
		} finally {
			if (dealToTranMap != null && Table.isTableValid(dealToTranMap) == 1) {
				dealToTranMap.destroy(); dealToTranMap = null;
			}
		}
		
		return tbl;
	}

	/**
	 * Updated the API call from Query.executeDirect(..) to Query.run(..) - as the BO saved query is unable to execute the 
	 * script defined in Additional Criteria Panel of saved query.
	 * 
	 * @param queryName
	 * @return
	 * @throws OException
	 */
	private Table executeSavedQuery(String queryName) throws OException {
		//int ret = Query.executeDirect(queryName, null, tbl, "ab_tran_event.event_num, ab_tran_event.tran_num");// 'tran_num' is not really used, but retrieved for information purposes
		int qId = -1;
		Table tEvents = Util.NULL_TABLE;
		
		try {
			qId = Query.run(queryName, null);
			if (qId <= 0) {
				throw new OException("Invalid queryId(" + qId + ") returned on execution of BO saved query(" + queryName + ")");
			}
			
			tEvents = Table.tableNew();
			String sql = "\n SELECT ate.event_num, ate.tran_num "
						+	"\n FROM ab_tran_event ate "
						+ 	"\n INNER JOIN " + Query.getResultTableForId(qId) + " qr ON qr.query_result = ate.event_num "
						+ 	"\n WHERE qr.unique_id = " + qId;
			
			Logging.info("Running SQL query:" + sql);
			int ret = DBaseTable.execISql(tEvents, sql);
			
			if (ret != OLF_RETURN_SUCCEED) {
				String errorMsg = DBUserTable.dbRetrieveErrorInfo(ret, "Failed to execute SQL query->'" + sql + "'");
				throw new OException(errorMsg);
			}
		} finally {
			if (qId > 0) {
				Query.clear(qId);
			}
		}
		
		return tEvents;
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

	private void setNextDocStatus(Table events, int nextDocStatus) throws OException {
		int rows = events.getNumRows();
		String sNumEvents = ""+rows+" "+(rows == 1 ? "event" : "events");

		Logging.info("Setting next document status " + nextDocStatus + " for " + sNumEvents);

		events.addCol("next_doc_status", COL_TYPE_ENUM.COL_INT);
		events.setColValInt("next_doc_status", nextDocStatus);
	}

	private String prepareDocTypeIdsForDbAccess(List<String> docTypeNames) throws OException {
		String docTypeIds = "";
		int id = -1;
		boolean firstPass = true;
		
		for (String name: docTypeNames)	{
			try {
				id = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_TYPE_TABLE, name);
				
				if (id >= 0) {
					if (firstPass) {
						docTypeIds +=  id;
					} else {
						docTypeIds += "," + id;
					}
					firstPass = false;
				} else {
					Logging.error("Document type '" + name + "' is invalid and will be skipped.");
				}
				
			} catch (OException oe) {
				Logging.error("Skipping invalid document type '" + name + "' found->" + oe.getMessage());
				continue;
			}
		}
		
		// remove the very first comma
		/*if (docTypeIds.trim().length() > 0){
			docTypeIds = docTypeIds.substring(1);
		}*/
		
		return docTypeIds;
	}

	/**
	 * checks if user is allowed to process settlement documents via script
	 * @throws OException
	 */
	protected void ensureUserMayProcessDocuments() throws OException {
		if (Util.userCanAccess(SEC_PRIV_STLDESKTOP_PROCESS) != 1) {
			String message = "User " + Ref.getUserName() + " is not allowed to process documents. Security Right 44308 is missing";
			throw new OException(message);
		}
	}

	/**
	 * retrieves all definitions from user table for the given document types
	 * @param docTypeIds
	 * @param processingIteration 
	 * @return
	 */
	private Table loadConfigurationTable(String docTypeIds, int processingIteration) throws OException {
		// Get all definitions to be processed from User Table
		String sSql = "SELECT * \n" + 
						  " FROM " + DEFINITION_TABLE_NAME + "\n" + 
						  " WHERE active = 1 \n";
		if (!docTypeIds.isEmpty()){
			sSql += " AND doc_type in (" + docTypeIds + ")\n";
		}
		sSql += " AND processing_iteration = " + processingIteration + "\n";
		sSql += " ORDER BY sequence";

		Table definitions = null;
		
		try {
			definitions = Table.tableNew();
			Logging.info("Executing SQL->" + sSql);
			
			if (DBaseTable.execISql(definitions, sSql) != 1) {
				String message = "Error executing sql statement \"" + sSql + "\"";
				throw new OException(message);
			}
			
		} catch (OException oe) {
			Logging.error("Error in loadConfigurationTable method->" + oe.getMessage());
			if (definitions != null && Table.isTableValid(definitions) == 1) {
				definitions.destroy(); definitions = null;
			}
			throw oe;
		}
		
		return definitions;
	}
	private String getTimeTakenDisplay(int timeTaken) {
		
		int modHours = 0;
		int modMinutes = 0;
		int modSeconds = 0;
		if (timeTaken > 3600){
			modMinutes =  timeTaken % 3600;
			modHours = (timeTaken - modMinutes)/3600;
			timeTaken = modMinutes; 
		} 
		
		if (timeTaken > 60){
			modSeconds =  timeTaken % 60;
			modMinutes = (timeTaken - modSeconds)/60;			
		} else {
			modSeconds = timeTaken ;
		}
		
		return (modHours>0? (" " + modHours + " Hours "):"") + (modMinutes>0? (" " + modMinutes + " Minutes "):"") + (modSeconds>0? (" " + modSeconds + " Seconds "):"")  ;
	}

}
