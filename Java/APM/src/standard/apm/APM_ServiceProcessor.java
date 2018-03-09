/* Released with version 29-Oct-2015_V14_2_4 of APM */

/*
 Description : This forms part of the Active Position Manager package
*/

package standard.apm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;

import standard.apm.statistics.IApmStatisticsLogger;
import standard.apm.statistics.Scope;
import standard.include.APM_Utils;
import standard.include.APM_Utils.EntityType;
import standard.include.ConsoleCaptureWrapper;
import standard.include.ConsoleLogging;
import standard.include.LogConfigurator;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.TranOpService;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.enums.OP_SERVICES_LOG_STATUS;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;

public class APM_ServiceProcessor implements IScript {
	private APM_Utils m_APMUtils;

	private int m_runId;

	public APM_ServiceProcessor() {
		m_APMUtils = new APM_Utils();

	}

	// DO WE SAVE THE ARGT OFF TO AFS IN EVENT OF FAILURE ON A BATCH ?
	boolean tfeSettingForSaveArgt = false;  // TBD - add global tfe_config setting
	boolean apmSettingForExitFailOnBatchFail = false; // global setting which controls whether to report failure on entity group fail

	// --------------------------------------------------------------------------
	// ------------- globals -------------------------------
	// --------------------------------------------------------------------------

	// The minimum compatible version of olf_tfe_interface_api DLL
	int APM_COMPATIBLE_INTERFACE_VERSION = 69;

	// Missing packages, unique Package names only.
	Set<String> m_missingPackages;

	public void execute(IContainerContext context) throws OException {
		final Calendar startTimestamp = Calendar.getInstance();
		ODateTime initiatorScriptStartTime = ODateTime.dtNew();
		initiatorScriptStartTime = APM_Utils.getODateTime();
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		Table tPackageDetails = Util.NULL_TABLE;
		Table tAPMArgumentTable = Table.tableNew("APM Argument Table");
		Table tOpsSvcDefn;
		String sServiceName;
		String sLogFilePath;
		String sLogFilename;
		String sErrMessage = "";
		String sDatasetType;
		int iRetVal = 1;
		int iMode = -1;
		int tfeInterfaceVersion = -1;
		int iServiceId = 0;
		int iDatasetTypeId;
		int iScriptId;

		IApmStatisticsLogger apmStatsServiceScopeLogger = null;

		try {
			// ////////////////////////////////////////////////////////////
			//
			// Create the common args table ...
			// Do this early since it is needed in m_APMUtils.APM_PrintMessage
			//
			// /////////////////////////////////////////////////////////////
			APM_CreateArgumentTable(tAPMArgumentTable);
			// Start with debug on ...
			tAPMArgumentTable.setInt("Debug", 1, 1);
			
			sServiceName = APM_GetServiceName(argt);
			tAPMArgumentTable.setString("service_name", 1, sServiceName);

			apmStatsServiceScopeLogger = m_APMUtils.newLogger(Scope.SERVICE, sServiceName);

			ConsoleLogging.instance().setServiceContext(tAPMArgumentTable, sServiceName);
			apmStatsServiceScopeLogger.setContext("serviceName", sServiceName);

			int pid = Ref.getProcessId();
			apmStatsServiceScopeLogger.setContext("servicePid", String.valueOf(pid));

			ConsoleLogging.instance().setProcessIdContext(tAPMArgumentTable, pid);
			ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "APM_ServiceProcessor");

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
			String scriptStartTimestamp = dateFormat.format(startTimestamp.getTime());
			apmStatsServiceScopeLogger.setMetric("serviceScriptStartTime", scriptStartTimestamp);
 
			if (Str.isEmpty(Util.getEnv("AB_ERROR_LOGS_PATH")) == 1)
				sLogFilePath = Util.getEnv("AB_OUTDIR") + "/error_logs/";
			else
				sLogFilePath = Util.getEnv("AB_ERROR_LOGS_PATH") + "/";

			// Set the log path to be used by our logger.
			LogConfigurator.getInstance().setPath(sLogFilePath);
			LogConfigurator.getInstance().setServiceName(sServiceName);
			LogConfigurator.getInstance().push(LogConfigurator.getInstance().getFileName(true));
			tAPMArgumentTable.setString("Log File", 1, LogConfigurator.getInstance().front());

			if (!ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).isOpen()) {
				// Open the console capture and register this object as its owner.
				ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).open(this);
			}

			EntityType entityType = m_APMUtils.FindAndSetEntityType(argt, tAPMArgumentTable);

			// make sure that primary entity num and secondary entity num are set to -1
			ConsoleLogging.instance().unSetSecondaryEntityNumContext(tAPMArgumentTable);
			ConsoleLogging.instance().unSetPrimaryEntityNumContext(tAPMArgumentTable);
			ConsoleLogging.instance().unSetEntityVersionContext(tAPMArgumentTable);
			
			// Check the ADS environment is set correctly.
			standard.apm.ads.Environment.CheckClassPaths();

			// take 2 copies of the entity info table so that it can be filtered 
			// 1st copy is the entity info de duped
			// 2nd copy is the entity info de duped and for the current entity group (set inside the main entity group loop)
			// do not operate on the original any longer as we need to remove duplicates
			// for block updates
			APM_EntityJobOps.instance().APM_SetEntityInfoCopyFromArgt(tAPMArgumentTable, argt);
			// now filter it to remove duplicate entities - only process the last
			APM_EntityJobOps.instance().APM_FilterEntityInfoTable(tAPMArgumentTable, argt, tAPMArgumentTable.getTable("Global Filtered Entity Info", 1));
				
			if ( m_APMUtils.GetCurrentEntityType(tAPMArgumentTable) == EntityType.DEAL && m_APMUtils.APM_CheckColumn(argt, "Deal Info", COL_TYPE_ENUM.COL_TABLE.toInt()) != 0)
				APM_DealJobOps.instance().APM_ReEvaluateUndoAmendedNewTrans(tAPMArgumentTable, argt, tAPMArgumentTable.getTable("Global Filtered Entity Info", 1));

			// get the script mode
			iMode = APM_EntityJobOps.instance().APM_FindLaunchType(argt, tAPMArgumentTable);
			if (iMode == m_APMUtils.cModeUnknown) {
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Unable to determine APM script launch type ... exiting");

				long elapsedMs = Calendar.getInstance().getTimeInMillis() - startTimestamp.getTimeInMillis();
				apmStatsServiceScopeLogger.setMetric("elapsedMs", String.valueOf(elapsedMs));
				
				apmStatsServiceScopeLogger.setMetric("succeeded", String.valueOf(false));				
				apmStatsServiceScopeLogger.flush();

				m_APMUtils.closeLogger(apmStatsServiceScopeLogger, tAPMArgumentTable);

				Util.exitFail();
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// Determine the initial mode (batch or update) and logfile name
			//
			// //////////////////////////////////////////////////////////////////////////////////
			boolean isBatch = iMode == m_APMUtils.cModeBatch;
			if (isBatch) {
				/* take the name of the service name for the log name */
				sLogFilename = sLogFilePath + sServiceName + "_Batch.log";

				ConsoleLogging.instance().setRunModeContext(tAPMArgumentTable, "Batch");
				apmStatsServiceScopeLogger.setContext("runMode", "Batch");
			} else {
				/*
				 * take the default name plus the monitor ID or service name for the
				 * log name
				 */
				sLogFilename = sLogFilePath + sServiceName + "_Updates.log";

				ConsoleLogging.instance().setRunModeContext(tAPMArgumentTable, "Incremental");
				
				//For statistics, need to differentiate between BlockUpdate and Incremental
				if (iMode == m_APMUtils.cModeBlockUpdate) {
					apmStatsServiceScopeLogger.setContext("runMode", "BlockUpdate");
				}
				else {
					apmStatsServiceScopeLogger.setContext("runMode", "Incremental");
				}
				
				// We may have the op_service_run_id.
				if (argt.getColNum("op_service_run_id") > 0)
					tAPMArgumentTable.setInt("op_services_run_id", 1, argt.getInt("op_service_run_id", 1));
				else
					tAPMArgumentTable.setInt("op_services_run_id", 1, 1);
			}
			tAPMArgumentTable.setString("Log File", 1, sLogFilename);
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Logging output to file: " + sLogFilename);

			// Check and return tfe_interface_api version
			tfeInterfaceVersion = APM_CheckAndGetTFEInterfaceVersion(tAPMArgumentTable);
			if (tfeInterfaceVersion == 0) {
				long elapsedMs = Calendar.getInstance().getTimeInMillis() - startTimestamp.getTimeInMillis();
				apmStatsServiceScopeLogger.setMetric("elapsedMs", String.valueOf(elapsedMs));
				
				apmStatsServiceScopeLogger.setMetric("succeeded", String.valueOf(false));
				apmStatsServiceScopeLogger.flush();

				m_APMUtils.closeLogger(apmStatsServiceScopeLogger, tAPMArgumentTable);

				Util.exitFail();
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// We can be sure that there are columns for things like dataset_type
			// now ...
			// 
			// //////////////////////////////////////////////////////////////////////////////////

			if (iRetVal != 0) {
				iDatasetTypeId = argt.getInt("dataset_type", 1);
				if (iDatasetTypeId < 0)
					iDatasetTypeId = 0;
				tAPMArgumentTable.setInt("dataset_type_id", 1, iDatasetTypeId);

				sDatasetType = m_APMUtils.APM_DatasetTypeIdToName(tAPMArgumentTable, iDatasetTypeId, iMode == m_APMUtils.cModeBatch);

				if (Str.len(sDatasetType) < 1)
					iRetVal = 0;

				if (iRetVal == 0)
					sErrMessage = "Unable to retrieve dataset type details";
				else {
					ConsoleLogging.instance().setDatasetTypeContext(tAPMArgumentTable, sDatasetType, iDatasetTypeId);
					// logging this because if the dataset type can't be named, the script won't do anything else
					apmStatsServiceScopeLogger.setContext("datasetType", String.format("%s(%d)", sDatasetType, iDatasetTypeId));
				}
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// Setup iServiceId
			// 
			// //////////////////////////////////////////////////////////////////////////////////

			if (iRetVal != 0) {
				iServiceId = argt.getInt("service_id", 1);
			}
			tAPMArgumentTable.setInt("service_id", 1, iServiceId);

			String uniqueRunId = "";
			if (isBatch) {
				m_runId = m_APMUtils.getWorkflowRunId(tAPMArgumentTable, iServiceId);

				uniqueRunId = String.valueOf(m_runId);
			} else {
				m_runId = m_APMUtils.getOpServicesRunId(tAPMArgumentTable);

				uniqueRunId = String.format("%d:%d", m_runId, iServiceId);
			}
			apmStatsServiceScopeLogger.setContext("runId", uniqueRunId);

			String hostname = m_APMUtils.getHostname();
			apmStatsServiceScopeLogger.setMetric("hostname", hostname);

			/*
			 	STATISTICS
			 	
				All the statistics before the APM call is made have now 
				been collated so now put them into the tAPMArgumentTable
				
				Create new table in tAPMArgumentTable called Statistics_ServiceScope
				Get the statistics from IApmStatisticsLogger::getAll()
				Put the statistics into tAPMArgumentTable using APM_Utils::convertMapToTable()
			*/
		
			Map<String,Object> serviceContexts = apmStatsServiceScopeLogger.getAll();
			Table tServiceStatistics = m_APMUtils.convertMapToStatisticsTable(serviceContexts);
			if (tServiceStatistics.getNumRows() > 0) {
				//Delete all rows in Statistics table
				Table tStatistics = tAPMArgumentTable.getTable("Statistics", 1);
				tStatistics.clearRows();
				//Add this table at Service scope level
				int row = tStatistics.addRow();
				tStatistics.setString("Scope", row, "Service");
				tStatistics.setTable("Scope_Statistics", row, tServiceStatistics);
			}
			tAPMArgumentTable.setDateTime("Initiator_Script_Start_Time", 1, initiatorScriptStartTime);
			
			// ////////////////////////////////////////////////////////////
			//
			// Fill in some of the processor common args table
			//
			// /////////////////////////////////////////////////////////////
			if (iRetVal != 0) {
				/* Setup the RTP Page Prefix (SYS_APM_<script_id>. */
				if (iMode == m_APMUtils.cModeBatch)
					iScriptId = argt.getInt("script_id", 1);
				else {
					if (argt.getColNum("APM Script ID") > 0) {
						iScriptId = argt.getInt("APM Script ID", 1);
					} else {
						tOpsSvcDefn = argt.getTable("Operation Service Definition", 1);
						iScriptId = tOpsSvcDefn.getInt("defn_update_script_id", 1);
					}
				}
				tAPMArgumentTable.setString("RTP Page Prefix", 1, "SYS_APM_" + Str.intToStr(iScriptId) + ".");
				tAPMArgumentTable.setTable("Main Argt", 1, argt);
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// Fill in the generic sections of the apm argument table
			//
			// //////////////////////////////////////////////////////////////////////////////////
			if (iRetVal != 0) {

				String sProcessingMessage = "'APM_Initialization' started for this entity group";
				m_APMUtils.APM_LogStatusMessage(iMode, 1, m_APMUtils.cStatusMsgTypeProcessingAlways, "", "", -1, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);

				if (returnt != Util.NULL_TABLE && returnt != null)
					tAPMArgumentTable.setTable("Main returnt", 1, returnt);

				iRetVal = APM_EntityJobOps.instance().SetUpArgumentTableForEntityType(tAPMArgumentTable, argt);

				/* Get the Script run mode */
				tAPMArgumentTable.setInt("Script Run Mode", 1, iMode);
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// Load in the package settings from the database and parse to fill in
			// global vars
			//
			// //////////////////////////////////////////////////////////////////////////////////
			if (iRetVal != 0) {
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Start Load of all package details");
				tPackageDetails = APM_LoadAllPackageDetails(iMode, tAPMArgumentTable, argt);
				if (Table.isTableValid(tPackageDetails) == 0)
					iRetVal = 0;
				else
					tAPMArgumentTable.setTable("Package Details", 1, tPackageDetails);
				if (iRetVal == 0)
					sErrMessage = "Failed to load package details";
				else
					m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Completed loading package details");

				if (iMode == m_APMUtils.cModeBatch) {
					Iterator<String> it = m_missingPackages.iterator();
					while (it.hasNext()) {
						String sMissingPackage = it.next();

						ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sMissingPackage);
						String sPackageError = "No data tables/columns enabled for package: " + sMissingPackage + ", no data will be generated for this package.";
						m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sPackageError);
					}

					ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
				}

				// now see if the batch failure sweeping mode has been set
				tfeSettingForSaveArgt = (m_APMUtils.APM_GetOverallPackageSettingInt(tAPMArgumentTable, "batch_fail_sweep_mode", 1, 0, 0) == 1);
				apmSettingForExitFailOnBatchFail = (m_APMUtils.APM_GetOverallPackageSettingInt(tAPMArgumentTable, "fail_service_on_batch_fail", 1, 0, 0) == 1);

				if (iMode == m_APMUtils.cModeBatch && tfeSettingForSaveArgt)
					m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "------- BATCH FAILURES SWEEPING MODE IS SET TO TRUE --------");

				boolean refreshServicePriorRevalType = false;
				if (iMode == m_APMUtils.cModeBatch)
					refreshServicePriorRevalType = true;
				int priorRevalType = m_APMUtils.getPriorRevalTypeForPackages(iMode, tAPMArgumentTable, refreshServicePriorRevalType);
				if (priorRevalType == -1) {
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Conflicting prior Reval Types specified on packages");
					iRetVal = 0;
				}
			}

			// Now set the debug flag according to the overall package settings ...
			tAPMArgumentTable.setInt("Debug", 1, m_APMUtils.outputDebugInfoFlag(tAPMArgumentTable));

			/* set running batch status for all datasets in this service to "Waiting to Run" */
			if (iRetVal != 0 && iMode == m_APMUtils.cModeBatch) {
				// ADS
				if (!m_APMUtils.useADS(tAPMArgumentTable)) {
					// global message
					m_APMUtils.APM_LogStatusMessage(iMode, 1, m_APMUtils.cStatusMsgTypeProcessingAlways, "", "", -1, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, "Waiting to run");

				}
			}

			////////////////////////////////////////////////////////////////////////////////////////////////////
			//
			// If we are incremental processing remove any packages that don't support incremental processing
			//
			////////////////////////////////////////////////////////////////////////////////////////////////////
			if ((iMode != m_APMUtils.cModeBatch) && (iMode != m_APMUtils.cModeDoNothing)) {
				if (APM_RemovePackagesWithoutIncremental(tAPMArgumentTable) < 1) {
					// none left after removing, warn and switch to cModeDoNothing 
					m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "No packages to process that support incremental updates!");
					iMode = m_APMUtils.cModeDoNothing;
				}
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// APM Initialization
			//
			// //////////////////////////////////////////////////////////////////////////////////
			if (iRetVal == 1 && (iMode != m_APMUtils.cModeDoNothing)) {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start APM Initialization");
				iRetVal = APM_Initialization(tAPMArgumentTable, iServiceId);
				if (iRetVal == 0)
					sErrMessage = "Failed APM Initialization";
				else
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End APM Initialization");
			}

			if (iRetVal != 0) {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Starting to populate custom split");
			}

			// catch the instance where the generic package is being called with
			// endur or findur packages
			// - should never happen but logmonitor/screngs soemtimes appear to get
			// into a wierd state where this happens
			if (Table.isTableValid(tPackageDetails) == 1 && tPackageDetails.getNumCols() < 1) {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Skipped update.  Invalid Package for script");
				iMode = m_APMUtils.cModeDoNothing;
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// fill in package specific argument table parameters
			//
			// //////////////////////////////////////////////////////////////////////////////////
			if ((iRetVal == 1) && (iMode != m_APMUtils.cModeDoNothing)) {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Starting to populate argument table");
				iRetVal = APM_PopulateArgumentTable(iMode, tAPMArgumentTable, argt);
				if (iRetVal == 0)
					sErrMessage = "Failed to populate argument table";
				else
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Finished populating argument table");
			}

			if (iRetVal == 1) {
				String sProcessingMessage = "'APM_Initialization' finished for this group";
				m_APMUtils.APM_LogStatusMessage(iMode, 1, m_APMUtils.cStatusMsgTypeProcessing, "", "", -1, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// Run the main proc for each entity group ...
			//
			// //////////////////////////////////////////////////////////////////////////////////
			if (iRetVal != 0) {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Starting to process each entity group");
				iRetVal = APM_MainEntityGroupLoop(tAPMArgumentTable, sServiceName, iMode, argt);
				if (iRetVal == 0)
					sErrMessage = "Failed entity group processing";
				else
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Finished Entity Group processing");
			} else {
				// Log error if necessary - its a global failure as we never entered the entity group loop
				if (sErrMessage.length() > 0)
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
			}

			// NO NEED TO REPORT ERRORS HERE AS HANDLED INSIDE THE MAIN LOOP FN

			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "=================================================");
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "APM Processing Complete");
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "=================================================");

			// finally clean the guaranteed msg log for all entries from yest or
			// before
			// not bothering to check for errors as this is strictly nice to have
			// only do on a batch as the overhead could be severe
			if (iMode == m_APMUtils.cModeBatch) {
				APM_MessageLogUtilities messageLogUtils = new APM_MessageLogUtilities();
				messageLogUtils.APM_PurgeGuaranteedMsgLog(tAPMArgumentTable);
			}

			if (iRetVal == 0)
				ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).flush();
			else
				ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).close(this);

			// Clean & destroy the argument table.
			APM_CleanArgumentTable(tAPMArgumentTable);
			tAPMArgumentTable.destroy();

		} // end try
		catch (Exception t) {
			iRetVal = 0;
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception at top level of APM_ServiceProcessor: " + t);
			String message = m_APMUtils.getStackTrace(t);
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message + "\n");

			try {
				ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).flush();
			} catch (Throwable exception) {
				message = "Error attempting to flush console capture wrapper, " + exception.getMessage();
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message + "\n");
			}
		}

		boolean succeeded = false;

		// For a block Update do not set this to failed as some updates may have succeeded
		// AND we only want to set in the op services log the ones that have failed - not all (which exitfail does)
		if (iRetVal == 0) {
			if (iMode == m_APMUtils.cModeBatch) {
				if (apmSettingForExitFailOnBatchFail == true) {
					succeeded = false;
				} else {
					succeeded = true;
				}
			} else if (iMode == m_APMUtils.cModeBlockUpdate) { // don't fail on blocks as status driven from log_status
				succeeded = true;
			} else {
				// don't fail on incrementals as otherwise the status in the ops service will be running rather than failed
				// theres only 1 row if we have hit an incrmental status
				succeeded = APM_EntityJobOps.instance().SetArgtInfoReturnStatus(tAPMArgumentTable, argt, OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt());
			}
		} else {
			succeeded = true;
		}

		apmStatsServiceScopeLogger.setMetric("succeeded", String.valueOf(succeeded));
		
		long elapsedMs = Calendar.getInstance().getTimeInMillis() - startTimestamp.getTimeInMillis();
		apmStatsServiceScopeLogger.setMetric("elapsedMs", String.valueOf(elapsedMs));
		
		apmStatsServiceScopeLogger.flush();

		m_APMUtils.closeLogger(apmStatsServiceScopeLogger, tAPMArgumentTable);

		if (succeeded) {
			Util.exitSucceed();
		} else {
			Util.exitFail();
		}
	}

	int APM_MainEntityGroupLoop(Table tAPMArgumentTable, String sServiceName, int iMode, Table argt) throws OException {
		int iRetVal = 1;
		int entityGroupId;
		Table tJobResults = Table.tableNew("Job Results");
		int primaryEntityNum = -1;
		int secondaryEntityNum = -1;
		int entityVersion = -1;
		String sErrMessage = "";

		// -----------------------------------------------------------------------------------
		// ------------------------------- Start Entity Group Processing
		// ------------------------
		// -----------------------------------------------------------------------------------

		// update the dataset key statuses
		if (iMode == m_APMUtils.cModeBatch) {
			APM_BatchOps batchOperations = new APM_BatchOps();
			batchOperations.runStatusScript(tAPMArgumentTable);
		}

		Table tSplitProcessJobs = Table.tableNew("split_info");
		Table tLocalJobs = Table.tableNew("local_info");
		APM_ServiceJobs serviceJobs = new APM_ServiceJobs();

		// generate the jobs
		if (iMode != m_APMUtils.cModeDoNothing) {
			serviceJobs.APM_CreateJobTable(tSplitProcessJobs);
			serviceJobs.APM_CreateJobTable(tLocalJobs);
			serviceJobs.APM_CreateJobs(iMode, tSplitProcessJobs, tLocalJobs, tAPMArgumentTable, sServiceName, argt);
		}

		int iOverallRetVal = 1; // if any entity group fails then fail this....

		// issue splitProcess requests first as they are most important
		if (iMode != m_APMUtils.cModeDoNothing && tSplitProcessJobs.getNumRows() > 0) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start issuing requests");
			iRetVal = serviceJobs.APM_IssueSplitRequests(tAPMArgumentTable, sServiceName, iMode, tSplitProcessJobs, tJobResults);
			if (iRetVal == 0)
				sErrMessage = "Job request(s) failed. Probable engine crash.";
			else {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End issuing requests");
				m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, "", "", -1, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, "All processing jobs complete");
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "All processing jobs complete");
			}
			if (iRetVal != 0) {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start checking job results");
				iRetVal = serviceJobs.APM_CheckJobResults(iMode, tAPMArgumentTable, tJobResults);
				if (iRetVal == 0)
					sErrMessage = "One or more jobs Failed";
				else
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End checking job results");
			}
			// also clear the results
			tJobResults.clearRows();

			// individual entity group errors don't affect the loop - but need to be recorded for overall status
			if (iRetVal == 0) {
				iOverallRetVal = 0;
				iRetVal = 1;
			}
		}

		// loop around local jobs
		if (iRetVal == 1 && iMode != m_APMUtils.cModeDoNothing) {
			int jobQueryID;
			Table tjobArgumentTable;
			int numLocalJobs = tLocalJobs.getNumRows();

			if (numLocalJobs > 0)
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Processing " + numLocalJobs + " " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " job requests locally");

			for (int iLocalJob = 1; iLocalJob <= numLocalJobs; iLocalJob++) {
				entityGroupId = tLocalJobs.getInt("entity_group_id", iLocalJob);
				tjobArgumentTable = tLocalJobs.getTable("job_arg_table", iLocalJob);

				ConsoleLogging.instance().setEntityGroupContext(tjobArgumentTable, m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId), entityGroupId);

				// now check whether there are any entities for this entity group
				if (iMode != m_APMUtils.cModeBatch) {
					secondaryEntityNum = tjobArgumentTable.getInt("Current Secondary Entity Num", 1);
					primaryEntityNum = tjobArgumentTable.getInt("Current Primary Entity Num", 1);
					entityVersion = tjobArgumentTable.getInt("Current Entity Version", 1);
					iMode = tjobArgumentTable.getInt("Script Run Mode", 1);
					jobQueryID = tjobArgumentTable.getInt("job_query_id", 1);
					if (jobQueryID == 0) // no entities for this job, -1 means backouts only
						iMode = m_APMUtils.cModeDoNothing;
				}

				if (iMode == m_APMUtils.cModeDoNothing) {
					ConsoleLogging.instance().unSetEntityGroupContext(tjobArgumentTable);
					continue;
				}

				if (iRetVal != 0) {
					m_APMUtils.APM_PrintDebugMessage(tjobArgumentTable, "Start run job processor script locally");
					iRetVal = serviceJobs.APM_ProcessRequestLocally(tjobArgumentTable, iMode, tLocalJobs, iLocalJob, tJobResults);
					if (iRetVal == 0)
						sErrMessage = "Local job processor request failed.";
					else {
						m_APMUtils.APM_PrintDebugMessage(tjobArgumentTable, "Complete run job processor script locally");
						m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, "", "", entityGroupId, -1, secondaryEntityNum, primaryEntityNum, entityVersion, tjobArgumentTable, Util.NULL_TABLE, "Completed processing job");
						m_APMUtils.APM_PrintMessage(tjobArgumentTable, "Completed processing job");
					}
				}

				if (iRetVal != 0) {
					m_APMUtils.APM_PrintDebugMessage(tjobArgumentTable, "Start checking job results");
					iRetVal = serviceJobs.APM_CheckJobResults(iMode, tjobArgumentTable, tJobResults);
					if (iRetVal == 0)
						sErrMessage = "One or more jobs Failed";
					else
						m_APMUtils.APM_PrintDebugMessage(tjobArgumentTable, "End checking job results");
				}

				// also clear the results
				tJobResults.clearRows();

				if (iRetVal == 0) {
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tjobArgumentTable, sErrMessage);
				}

				// individual entity group errors don't affect the loop - but need to be recorded for overall status
				if (iRetVal == 0) {
					iOverallRetVal = 0;
					iRetVal = 1;
				}

				// Clear the reporting entity group Id
				ConsoleLogging.instance().unSetEntityGroupContext(tjobArgumentTable);
			}
		}
		// -----------------------------------------------------------------------------------
		// ------------------------------- end entity group loop
		// ------------------------------
		// -----------------------------------------------------------------------------------
		iRetVal = iOverallRetVal; // set the correct status (if any entity group fails - fail this)

		// also clear those messages with a entity group of 0 - can occur through bad
		// setup
		if (iRetVal == 1 && iMode == m_APMUtils.cModeBatch) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Starting to clear global messages from apm_msg_log");
			iRetVal = m_APMUtils.APM_ClearMsgLogForBadUpdates(iMode, 0, -1, "", tAPMArgumentTable);
			if (iRetVal == 0) {
				m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeFailure, "", "", -1, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, "Failed to clear global messages from message log (apm_msg_log)");
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Failed to clear global messages from message log (apm_msg_log)");
			} else
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Finished clearing global messages from apm_msg_log");
		}

		// finally update the statistics (if its a batch run)
		if (iMode == m_APMUtils.cModeBatch) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Starting to update statistics");
			if (APM_UpdateStatistics(iMode, tAPMArgumentTable) == 0)
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to update statistics");
			else
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Finished updating statistics");
		}

		// clear the split_info rows for the next iteration
		serviceJobs.APM_ClearJobTableRows(iMode, tSplitProcessJobs);
		serviceJobs.APM_ClearJobTableRows(iMode, tLocalJobs);

		tSplitProcessJobs.destroy();
		tLocalJobs.destroy();
		tJobResults.destroy();

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_LoadPackageSettings

	Parameters:    sErrMessage - any error to log

	Return Values: int : retval indicating success (1) or failure (0)
	-------------------------------------------------------------------------------*/
	Table APM_LoadPackageSettings(int iMode, String sPackageName, Table tAPMArgumentTable) throws OException {
		Table tConfigData;
		String sWhere = " ";
		int iRetVal;
		String sCacheTableName;

		iRetVal = 1;

		sCacheTableName = "APM_" + sPackageName + "_ConfigData";

		tConfigData = m_APMUtils.APM_CacheTableGet(sCacheTableName, tAPMArgumentTable);
		if (Table.isTableValid(tConfigData) == 0) {
			tConfigData = Table.tableNew(sPackageName + "_Config");
			sWhere = "(type = 1 and package_name = '" + sPackageName + "') or type = 0";
			iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tConfigData, "setting_name, value, package_name", "tfe_configuration", sWhere);

			if ((iRetVal == 0) || (tConfigData.unsortedFindString("package_name", sPackageName, SEARCH_CASE_ENUM.CASE_SENSITIVE) < 1)) {
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to load APM package specific script settings for package " + sPackageName
				        + ".");
				tConfigData.destroy();
				return Util.NULL_TABLE;
			}

			tConfigData.sortCol("setting_name");

			iRetVal = m_APMUtils.APM_CacheTableAdd(sCacheTableName, "TFE.METADATA.CHANGED", tConfigData.copyTable(), tAPMArgumentTable);
			if (iRetVal == 0) {
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to cache APM package specific script settings for package " + sPackageName
				        + ".");
				tConfigData.destroy();
				return Util.NULL_TABLE;
			}
		}

		return tConfigData;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_RemovePackagesWithoutIncremental
	
	Removes packages from package list if they do not support incremental processing 
	
	Return Values: int : retval indicating success (1) or failure (0)
	-------------------------------------------------------------------------------*/
	int APM_RemovePackagesWithoutIncremental(Table tAPMArgumentTable) throws OException {
		Table packageDetails;
		String packageName;
		int numPackages;

		packageDetails = tAPMArgumentTable.getTable("Package Details", 1);
		numPackages = packageDetails.getNumRows();

		// remove rows for package with incremental_updates turned off
		for (int idx = numPackages; idx >= 1; idx--) {
			packageName = packageDetails.getString("package_name", idx);
			if (m_APMUtils.APM_IsIncrementalOnForPackage(tAPMArgumentTable, packageName) == false) {
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Skipping processing of package '" + packageName + "' - Does not support incremental updates.");
				packageDetails.delRow(idx);
			}
		}

		return packageDetails.getNumRows();
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_LoadAllPackageDetails
	Settings
	Parameters:    sErrMessage - any error to log

	Return Values: int : retval indicating success (1) or failure (0)
	-------------------------------------------------------------------------------*/
	Table APM_LoadAllPackageDetails(int iMode, Table tAPMArgumentTable, Table argt) throws OException {
		Table tPackageDetails, tPackageSettings, tPackageDataTables;
		String sPackageName;
		int iRow, iRetVal;

		iRetVal = 1;

		tPackageDetails = argt.getTable("packages", 1).copyTable();
		if (Table.isTableValid(tPackageDetails) != 0) {
			tPackageDetails.addCol("package_data_tables", COL_TYPE_ENUM.COL_TABLE);
			tPackageDetails.addCol("package_settings", COL_TYPE_ENUM.COL_TABLE);

			// Get the package settings
			for (iRow = tPackageDetails.getNumRows(); iRow >= 1; iRow--) {
				sPackageName = tPackageDetails.getString("package_name", iRow);
				ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);

				if (iMode == m_APMUtils.cModeBatch)
					m_APMUtils.APM_PrintPackageVersion(tAPMArgumentTable, sPackageName);

				tPackageSettings = APM_LoadPackageSettings(iMode, sPackageName, tAPMArgumentTable);
				if (Table.isTableValid(tPackageSettings) == 0) {
					iRetVal = 0;
					break;
				} else {
					tPackageDetails.setTable("package_settings", iRow, tPackageSettings);
				}

				// Create the package APM data table structures
				tPackageDataTables = APM_CreateAPMReportingTable(iMode, tAPMArgumentTable, sPackageName);

				if (tPackageDataTables.getNumRows() == 0 && Str.equal(sPackageName, "Endur") != 1 && Str.equal(sPackageName, "Findur") != 1) {
					if (!m_missingPackages.contains(sPackageName))
						m_missingPackages.add(sPackageName);

					tPackageDetails.delRow(iRow);
					iRetVal = 1;
				} else if (tPackageDataTables == Util.NULL_TABLE) {
					iRetVal = 0;
					break;
				} else {
					tPackageDetails.setTable("package_data_tables", iRow, tPackageDataTables);
				}
			}

			ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
		} else
			iRetVal = 0;

		if (iRetVal == 0) {
			tPackageDetails.destroy();
			return Util.NULL_TABLE;
		}

		return tPackageDetails;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_UpdateBatchStats
	Description:   Inserts a new range of batch stats in tfe_defn_timings
	Parameters:    
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	int APM_UpdateStatistics(int iMode, Table tAPMArgumentTable) throws OException {
		Table tStats;
		int iRetVal, serviceId, iRow;
		String sPackageName;
		Table tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);

		if (iMode != m_APMUtils.cModeBatch)
			return 1;
		iRetVal = 1;

		serviceId = tAPMArgumentTable.getInt("service_id", 1);

		tStats = Table.tableNew("params");
		tStats.addCol("service_id", COL_TYPE_ENUM.COL_INT);
		tStats.addCol("package_name", COL_TYPE_ENUM.COL_STRING);
		tStats.addRow();
		tStats.setInt(1, 1, serviceId);

		for (iRow = 1; (iRetVal == 1 && (iRow <= tPackageDetails.getNumRows())); iRow++) {
			sPackageName = tPackageDetails.getString("package_name", iRow);
			tStats.setString(2, 1, sPackageName);

			iRetVal = m_APMUtils.APM_DBASE_RunProc(tAPMArgumentTable, "USER_apm_insert_b_stats_svc", tStats);

			if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Error updating statistics at end of batch run : "
				        + DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() failed") + "\n");
				iRetVal = 0;
			}
		}
		tStats.destroy();

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_CreateArgumentTable
	Description:   Create the argument table
	Parameters:      
	Return Values:   
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	void APM_CreateArgumentTable(Table tArgumentTable) throws OException {
		Table tMsgContext, tBatchErrs;

		tArgumentTable.addCol("Entity Type", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Selected Entity Groups", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Current Entity Group Id", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Current Package", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Current Scenario", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("Filtered Entity Info", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Global Filtered Entity Info", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("job_query_id", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("Main returnt", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Script Run Mode", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Bad Updates", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Log File", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Debug", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Package Name", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Market Data Source", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Closing Dataset ID", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Closing Dataset Date", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("op_services_run_id", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("service_id", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
		
		tArgumentTable.addCol("Job Query", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Tranche", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Job Name", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Current Job Has No Entities", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("Current Secondary Entity Num", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Current Primary Entity Num", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Current Entity Version", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Previous Entity Group Id", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("Message Context", COL_TYPE_ENUM.COL_TABLE);

		tArgumentTable.addCol("Scenario_List", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Simulation Name", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Overall Package Sim Results", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Sim Def", COL_TYPE_ENUM.COL_TABLE);

		tArgumentTable.addCol("Main Argt", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("RTP Page Prefix", COL_TYPE_ENUM.COL_STRING);

		tArgumentTable.addCol("Package Details", COL_TYPE_ENUM.COL_TABLE); // Package

		tArgumentTable.addCol("Batch Failures", COL_TYPE_ENUM.COL_TABLE); // Package

		tArgumentTable.addCol("Initiator_Script_Start_Time", COL_TYPE_ENUM.COL_DATE_TIME);
		tArgumentTable.addCol("Statistics", COL_TYPE_ENUM.COL_TABLE);
		
		// Contains a set of missing packages...
		m_missingPackages = new HashSet<String>();

		tArgumentTable.addRow();

		Table tStatistics = Table.tableNew("Statistics");
		tStatistics.addCol("Scope", COL_TYPE_ENUM.COL_STRING);
		tStatistics.addCol("Scope_Statistics", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.setTable("Statistics", 1, tStatistics);
		
		tMsgContext = Table.tableNew("Message Context");
		tMsgContext.addCol("ContextName", COL_TYPE_ENUM.COL_STRING);
		tMsgContext.addCol("ContextValue", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.setTable("Message Context", 1, tMsgContext);

		tBatchErrs = Table.tableNew("Batch Failures");
		tBatchErrs.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
		tBatchErrs.addCol("package", COL_TYPE_ENUM.COL_STRING);
		tBatchErrs.addCol("scenario_id", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.setTable("Batch Failures", 1, tBatchErrs);

	}

	int APM_CleanArgumentTable(Table tArgumentTable) throws OException {
		tArgumentTable.setTable("Main Argt", 1, Util.NULL_TABLE);
		tArgumentTable.setTable("Main returnt", 1, Util.NULL_TABLE);
		return 1;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_InitGlobals
	Description:   

	Return Values: 
	-------------------------------------------------------------------------------*/
	String APM_GetServiceName(Table argt) throws OException {
		String sServiceName = " ";

		sServiceName = argt.getString("service_name", 1);
		return sServiceName;
	}
	
	/*-------------------------------------------------------------------------------
	Name:          APM_CheckAndGetTFEInterfaceVersion

	Description:   This routine identifies the version of tfe_interface_api in use,
	           and checks for version set in script then returns value

	Parameters:

	Return Values: 0 if failure otherwise value of tfe_interface version.
	-------------------------------------------------------------------------------*/
	int APM_CheckAndGetTFEInterfaceVersion(Table tAPMArgumentTable) throws OException {
		int tfe_interface_version = APM_Utils.getTfeInterfaceApiVersion();
		m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Version of tfe_interface_api DLL Found = " + tfe_interface_version);
		if (tfe_interface_version < APM_COMPATIBLE_INTERFACE_VERSION) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Version of tfe_interface_api DLL is incompatible with this version of APM.  Required = "
			        + APM_COMPATIBLE_INTERFACE_VERSION);
			return 0;
		}
		return tfe_interface_version;
	}

	// ////////////////////////////////////////////////////////////////
	// User function wrappers
	// ////////////////////////////////////////////////////////////////

	/*-------------------------------------------------------------------------------
	Name:          APM_CreateAPMReportingTable
	Description:   This function should create a Table with one COL_TYPE_ENUM.COL_TABLE column
	           and at least one row

	           Subsequent APM functions rely on the name of the sub-tables

	Return Values: A Table with one COL_TYPE_ENUM.COL_TABLE column and at least one row
	-------------------------------------------------------------------------------*/
	Table APM_CreateAPMReportingTable(int iMode, Table tAPMArgumentTable, String sPackageName) throws OException {
		Table tArgs;
		Table tDataTable;
		int iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		Table tPackageDataTables;
		Table tPackageDataTableCols;
		Table tReportingTables;
		String sDataTableName;
		int iTable;
		int iCol;
		String sTableCacheName;

		// if package name not a generic package (i.e. Endur or Findur) - then break out
		if (Str.equal(sPackageName, "Endur") == 1 || Str.equal(sPackageName, "Findur") == 1) {
			tReportingTables = Table.tableNew("EMPTY Table");
			return (tReportingTables);
		}

		sTableCacheName = "APM_" + sPackageName + "_ReportingTables";

		tReportingTables = m_APMUtils.APM_CacheTableGet(sTableCacheName, tAPMArgumentTable);
		if (Table.isTableValid(tReportingTables) != 0) {
			return (tReportingTables);
		}

		tPackageDataTables = Table.tableNew("Package Data Tables");
		tReportingTables = Table.tableNew("All APM Tables");

		tReportingTables.addCol("APM Reporting Table", COL_TYPE_ENUM.COL_TABLE);

		// Create the function parameters and run the the stored proc
		tArgs = Table.tableNew("params");
		tArgs.addCol("sPackageName", COL_TYPE_ENUM.COL_STRING);
		tArgs.addRow();
		tArgs.setString("sPackageName", 1, sPackageName);
		iRetVal = m_APMUtils.APM_DBASE_RunProc(tAPMArgumentTable, "USER_apm_get_pkg_tables", tArgs);
		tArgs.destroy();

		if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "USER_CreateAPMReportingTable call to USER_apm_get_pkg_tables stored proc failed");
		else {
			iRetVal = DBase.createTableOfQueryResults(tPackageDataTables);

			if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "USER_CreateAPMReportingTable unable to retrieve results from call to USER_apm_get_pkg_tables stored proc failed");
			} else if (tPackageDataTables.getNumRows() < 1) {
				iRetVal = m_APMUtils.NO_PACKAGE_COLUMNS_ENABLED;
			}
		}

		if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {

			for (iTable = 1; iTable <= tPackageDataTables.getNumRows(); iTable++) {
				tReportingTables.addRow();
				sDataTableName = tPackageDataTables.getString(1, iTable);

				tDataTable = Table.tableNew(sDataTableName);
				tDataTable.setTableTitle(sDataTableName);
				tReportingTables.setTable(1, iTable, tDataTable);

				tPackageDataTableCols = m_APMUtils.APM_GetColumnsForTable(iMode, tAPMArgumentTable, sPackageName, sDataTableName);
				if (tPackageDataTableCols == null)
					iRetVal = 0;
				else if (tPackageDataTableCols.getNumRows() < 1) {
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "USER_CreateAPMReportingTable ... no data table cols enabled for package : "
					        + sPackageName + ", table name : " + sDataTableName);
					iRetVal = 0;
				} else {
					for (iCol = 1; iCol <= tPackageDataTableCols.getNumRows(); iCol++) {

						// date and date time targets are converted to integers as they are not natively supported in the data cache
						// date = int julian date, date time = combo of date * secs in day + secs from start of day
						if (tPackageDataTableCols.getInt("column_type", iCol) == COL_TYPE_ENUM.COL_INT.toInt() ||
							tPackageDataTableCols.getInt("column_type", iCol) == COL_TYPE_ENUM.COL_DATE.toInt() || 
							tPackageDataTableCols.getInt("column_type", iCol) == COL_TYPE_ENUM.COL_DATE_TIME.toInt() ) {
							tDataTable.addCol(tPackageDataTableCols.getString("column_name", iCol), COL_TYPE_ENUM.COL_INT);

						} else if (tPackageDataTableCols.getInt("column_type", iCol) == COL_TYPE_ENUM.COL_DOUBLE.toInt()) {
							tDataTable.addCol(tPackageDataTableCols.getString("column_name", iCol), COL_TYPE_ENUM.COL_DOUBLE);
						} else if (tPackageDataTableCols.getInt("column_type", iCol) == COL_TYPE_ENUM.COL_STRING.toInt()) {
							tDataTable.addCol(tPackageDataTableCols.getString("column_name", iCol), COL_TYPE_ENUM.COL_STRING);
						}

					}
				}

				tDataTable.addCol("scenario_id", COL_TYPE_ENUM.COL_INT);
				tDataTable.addCol("scenario_currency", COL_TYPE_ENUM.COL_INT);
				if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
					break;
			}
		}

		if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() || iRetVal == m_APMUtils.NO_PACKAGE_COLUMNS_ENABLED)
			iRetVal = m_APMUtils.APM_CacheTableAdd(sTableCacheName, "TFE.METADATA.CHANGED", tReportingTables.copyTable(), tAPMArgumentTable);

		if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() && iRetVal != m_APMUtils.NO_PACKAGE_COLUMNS_ENABLED) {
			tReportingTables.destroy();
			tReportingTables = Util.NULL_TABLE;
		}

		tPackageDataTables.destroy();

		return (tReportingTables);
	}

	int APM_PopulateArgumentTable(int iMode, Table tAPMArgumentTable, Table argt) throws OException {
		Table tRevalResults;
		int iRetVal = 1;
		String sSimName;

		// default sim to APM Sium Mods
		sSimName = "None";

		// get the sim name
		sSimName = argt.getString("simulation_name", 1);
		if (Str.isEmpty(sSimName) == 1 || Str.equal(sSimName, "None") == 1)
			sSimName = "None";

		// save sim name in argument table
		tAPMArgumentTable.setString("Simulation Name", 1, sSimName);

		// setup the simulation results
		APM_SimResultSetup simResultSetup = new APM_SimResultSetup();
		m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start setup simulation results");
		tRevalResults = simResultSetup.APM_GetAllPackageSimResults(iMode, tAPMArgumentTable);
		if (Table.isTableValid(tRevalResults) == 0)
			iRetVal = 0;
		else {
			tAPMArgumentTable.setTable("Overall Package Sim Results", 1, tRevalResults);
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End setup simulation results");
		}

		if (iRetVal != 0) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start get sim def");
			iRetVal = simResultSetup.APM_GetSimDef(iMode, sSimName, tRevalResults, tAPMArgumentTable);
			if (iRetVal != 0)
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Finished getting sim def");
		}

		// Fill in the scenario user table (used for filter drop-down in gui)
		if (iRetVal != 0) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start fill scenarios user table");
			iRetVal = simResultSetup.APM_GetScenarioList(iMode, tAPMArgumentTable);
			if (iRetVal != 0)
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End fill scenarios user table");
		}

		return iRetVal;
	}

	int APM_Initialization(Table tAPMArgumentTable, int iServiceId) throws OException {
		int iRetVal;
		int iMode;
		Table tEntityGroups = Util.NULL_TABLE, tMainArgt;
		String sProcessingMessage;

		tMainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
		iMode = tAPMArgumentTable.getInt("Script Run Mode", 1);

		m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start Checking Version");
		iRetVal = APM_CheckVersion(iMode, tAPMArgumentTable);
		if (iRetVal != 0)
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End Checking Version");

		if (iRetVal != 0 && iMode == m_APMUtils.cModeBatch) {
			/* find any bad updates & set them in the argt */
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start Finding Bad Updates");
			iRetVal = m_APMUtils.APM_FindBadUpdates(iMode, iServiceId, tAPMArgumentTable, tEntityGroups);
		}

		/* clear the global messages from the message log for this defn */
		if (iRetVal == 1 && iMode == m_APMUtils.cModeBatch) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start Clearing Msg Log for GLOBAL msgs");

			/* find any bad updates & set them in the argt */
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start Finding Bad Updates");
			iRetVal = m_APMUtils.APM_FindBadUpdates(iMode, iServiceId, tAPMArgumentTable, tEntityGroups);

			sProcessingMessage = tAPMArgumentTable.getString("RTP Page Prefix", 1) + iServiceId + "_GLOBAL";
			// global level rather than entity group level
			APM_MessageLogUtilities messageLogUtils = new APM_MessageLogUtilities();
			iRetVal = messageLogUtils.APM_ClearEntriesInMsgLog(sProcessingMessage, -1, tAPMArgumentTable, iServiceId);
			if (iRetVal != 0)
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End Clearing Msg Log for GLOBAL msgs");
		}

		/* Setup Selected Entity groups */
		if (iRetVal != 0) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start getting selected entity groups");
			iRetVal = APM_EntityJobOps.instance().APM_GetSelectedEntityGroups(iMode, tAPMArgumentTable);
			if (iRetVal != 0)
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End getting selected entity groups");
		}

		/* fix the argt to enable block updates if supported by endur cut */
		if ( iRetVal == 1 && m_APMUtils.GetCurrentEntityType(tAPMArgumentTable) == EntityType.DEAL)
			APM_DealJobOps.instance().SetDealBlockMode(iMode, tMainArgt);
			
		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_CheckVersion
	Description:   checks that the database install is compatible with this script
	-------------------------------------------------------------------------------*/
	int APM_CheckVersion(int iMode, Table tAPMArgumentTable) throws OException {
		Table tAPMVersion;
		int iRetVal;

		// !!! Auto-generated versions, DO NOT CHANGE WITHOUT MODIFYING UpdateBuildNumber tool... !!!
		// ============================================
   int iVersionStartMajor = 14;
   int iVersionStartMinor = 2;
   int iVersionStartRevision = 4;

   int iVersionEndMajor = 14;
   int iVersionEndMinor = 2;
   int iVersionEndRevision = 4;

		String sVersion = "14.2.4";
		// ============================================
		// !!! END Auto-generated versions, DO NOT CHANGE WITHOUT MODIFYING UpdateBuildNumber tool... !!!

		String sSchemaVersion;
		String sSchemaStatus;

		if (iMode != m_APMUtils.cModeBatch)
			return 1;

		tAPMVersion = Table.tableNew("tfe_version");
		iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tAPMVersion, "*", "tfe_version", "1=1");
		if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Database error during version number check");
		else {
			sSchemaVersion = tAPMVersion.getString("apm_version", 1);
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "APM Script version = " + sVersion + ", Database Version = " + sSchemaVersion);

			sSchemaStatus = tAPMVersion.getString("db_upgrade", 1);

			if (Str.iEqual(sSchemaStatus, "Completed.") == 0) {
				iRetVal = 0;
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Active Position Manager Server Installation is incomplete.\n"
				        + "Please perform an Active Position Server Installation.\nScript Terminating.\n");
			}

			if (iRetVal == 1) {

				int endStringPosition = Str.findSubString(sSchemaVersion, ".");
				int iDBVersionMajor = m_APMUtils.APM_StrToInt(Str.substr(sSchemaVersion, 0, endStringPosition));

				String tempStr = Str.substr(sSchemaVersion, endStringPosition + 1, Str.len(sSchemaVersion) - endStringPosition - 1);

				endStringPosition = Str.findSubString(tempStr, ".");
				int iDBVersionMinor = m_APMUtils.APM_StrToInt(Str.substr(tempStr, 0, endStringPosition));

				tempStr = Str.substr(tempStr, endStringPosition + 1, Str.len(tempStr) - endStringPosition - 1);
				int iDBVersionRevision = m_APMUtils.APM_StrToInt(tempStr);

				// now compare the versions
				if (iDBVersionMajor < iVersionStartMajor || iDBVersionMajor > iVersionEndMajor
				        || (iDBVersionMajor == iVersionStartMajor && iDBVersionMinor < iVersionStartMinor)
				        || (iDBVersionMajor == iVersionEndMajor && iDBVersionMinor > iVersionEndMinor)
				        || (iDBVersionMajor == iVersionStartMajor && iDBVersionMinor == iVersionStartMinor && iDBVersionRevision < iVersionStartRevision)
				        || (iDBVersionMajor == iVersionEndMajor && iDBVersionMinor == iVersionEndMinor && iDBVersionRevision > iVersionEndRevision)) {
					iRetVal = 0;
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Active Position Manager Server Installation is incompatible with this script.\n"
					        + "Script Version = "
					        + sVersion
					        + "\n"
					        + "Server Installation Version = "
					        + sSchemaVersion
					        + "\n"
					        + "Please perform an Active Position Manager Server Installation.\nScript Terminating.\n");
				}
			}
		}
		tAPMVersion.destroy();
		return iRetVal;
	}

}
