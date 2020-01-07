/* Released with version 09-Jan-2014_V14_0_6 of APM */

/*
 Description : This forms part of the Active Position Manager package
*/

package standard.apm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import standard.apm.statistics.ApmStatisticsLogger;
import standard.apm.statistics.ApmStatisticsLoggerInstantiationException;
import standard.apm.statistics.IApmStatisticsLogger;
import standard.apm.statistics.Scope;
import standard.apm.statistics.metrics.Metrics;
import standard.include.APM_Utils;
import standard.include.ConsoleCaptureWrapper;
import standard.include.LogConfigurator;
import standard.include.StatisticsLogging;

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

public class RTPE_APM_Generic implements IScript {
	private APM_Utils m_APMUtils;

	public RTPE_APM_Generic() {
		m_APMUtils = new APM_Utils();

	}

	// DO WE SAVE THE ARGT OFF TO AFS IN EVENT OF FAILURE ON A BATCH ?
	boolean tfeSettingForSaveArgt = false;  // TBD - add global tfe_config setting
	boolean apmSettingForExitFailOnBatchFail = false; // global setting which controls whether to report failure on pfolio fail

	// --------------------------------------------------------------------------
	// ------------- globals -------------------------------
	// --------------------------------------------------------------------------
	// have we more than 1 deal in the deal info ?
	int gNDealsInArgt = 0;
	Table gOrigDealInfoTable;

	// new split types
	int APM_SPLIT_BY_NUM_ENGINES = 0;
	int APM_SPLIT_BY_NUM_DEALS = 1;
	int APM_SPLIT_BY_CUSTOM_PROCESS = 2;
	int APM_SPLIT_BY_NUM_JOBS = 3;
	int APM_SPLIT_NONE = 4;

	// defines (needed because of changes in Endur versions over time
	int INDEX_DB_STATUS_VALIDATED = 1;

	// The minimum compatible version of olf_tfe_interface_api DLL
	int APM_COMPATIBLE_INTERFACE_VERSION = 69;

	// Missing packages, unique Package names only.
	Set<String> m_missingPackages;

	public void execute(IContainerContext context) throws OException {
		final long startMs = System.currentTimeMillis();

		IApmStatisticsLogger apmLogger = null;

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
		// int query_id = -1;
		int tfeInterfaceVersion = -1;
		int iServiceId = 0;
		int iDatasetType;
		int iScriptId;

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

			StatisticsLogging.instance().setProcessIdContext(tAPMArgumentTable, Ref.getProcessId());
			StatisticsLogging.instance().setScriptContext(tAPMArgumentTable, "RTPE_APM_Generic");

			// make sure that tran num and deal num are set to -1
			StatisticsLogging.instance().unSetTranContext(tAPMArgumentTable);
			StatisticsLogging.instance().unSetDealContext(tAPMArgumentTable);

			// initialise the global variables
			sServiceName = APM_InitGlobals(tAPMArgumentTable, argt);
			tAPMArgumentTable.setString("service_name", 1, sServiceName);
			StatisticsLogging.instance().setServiceContext(tAPMArgumentTable, sServiceName);

			try {
				apmLogger = new ApmStatisticsLogger(Scope.SERVICE, sServiceName);
			} catch (ApmStatisticsLoggerInstantiationException exception) {
				throw new OException(exception);
			}

			apmLogger.register(Metrics.ELAPSED_MS.toString(), new Object() {
				@Override
				public String toString() {
					long totalMs = System.currentTimeMillis() - startMs;
					return String.valueOf(totalMs);
				}
			});

			apmLogger.register(Metrics.SERVICE_NAME.toString(), sServiceName);

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

			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "------- RUNNING OpenJVS version --------");

			// Check the ADS environment is set correctly.
			standard.apm.ads.Environment.CheckClassPaths();

			// take 2 copies of the deal info table so that it can be filtered 
			// 1st copy is the deal info de duped
			// 2nd copy is the deal info de duped and for the current pfolio (set inside the main pfolio loop)
			// do not operate on the original any longer as we need to remove duplicates
			// for block deal updates
			if (APM_CheckColumn(argt, "Deal Info", COL_TYPE_ENUM.COL_TABLE.toInt()) != 0) {
				tAPMArgumentTable.setTable("Global Filtered Deal Info", 1, APM_RetrieveDealInfoCopyFromArgt(tAPMArgumentTable, argt));
				// now filter it to remove duplicate transactions - only process the last
				APM_FilterDealInfoTable(tAPMArgumentTable, argt, tAPMArgumentTable.getTable("Global Filtered Deal Info", 1));
				APM_ReEvaluateUndoAmendedNewTrans(tAPMArgumentTable, argt, tAPMArgumentTable.getTable("Global Filtered Deal Info", 1));
			}

			// get the script mode
			iMode = APM_FindLaunchType(argt, tAPMArgumentTable);
			if (iMode == m_APMUtils.cModeUnknown) {
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Unable to determine APM script launch type ... exiting");
				Util.exitFail();
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// Determine the initial mode (batch or deal) and logfile name
			//
			// //////////////////////////////////////////////////////////////////////////////////
			if (iMode == m_APMUtils.cModeBatch) {
				/* take the name of the service name for the log name */
				sLogFilename = sLogFilePath + sServiceName + "_Batch.log";

				StatisticsLogging.instance().setRunModeContext(tAPMArgumentTable, "Batch");

				apmLogger.register(Metrics.RUN_MODE.toString(), "Batch");
			} else {
				/*
				 * take the default name plus the monitor ID or service name for the
				 * log name
				 */
				sLogFilename = sLogFilePath + sServiceName + "_Updates.log";

				StatisticsLogging.instance().setRunModeContext(tAPMArgumentTable, "Incremental");

				apmLogger.register(Metrics.RUN_MODE.toString(), "Incremental");

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
				apmLogger.log(Metrics.SUCCEEDED.toString(), String.valueOf(false));
				apmLogger.clear();

				Util.exitFail();
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// We can be sure that there are columns for things like dataset_type
			// now ...
			// 
			// //////////////////////////////////////////////////////////////////////////////////

			if (iRetVal != 0) {
				iDatasetType = argt.getInt("dataset_type", 1);
				if (iDatasetType < 0)
					iDatasetType = 0;
				tAPMArgumentTable.setInt("dataset_type", 1, iDatasetType);
				sDatasetType = m_APMUtils.APM_DatasetTypeIdToName(tAPMArgumentTable, iDatasetType, iMode == m_APMUtils.cModeBatch);
				if (Str.len(sDatasetType) < 1)
					iRetVal = 0;
				if (iRetVal == 0)
					sErrMessage = "Unable to retrieve dataset type details";
				else
					// m_APMUtils.APM_SetMsgContext(tAPMArgumentTable, "DATASET TYPE", sDatasetType, iDatasetType);
					StatisticsLogging.instance().setDatasetTypeContext(tAPMArgumentTable, sDatasetType, iDatasetType);
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// Setup iServiceId
			// 
			// //////////////////////////////////////////////////////////////////////////////////

			if (iRetVal != 0) {
				iServiceId = argt.getInt("service_id", 1);
			}
			tAPMArgumentTable.setInt("service", 1, iServiceId);

			boolean isBatch = apmLogger.get(Metrics.RUN_MODE.toString()).equalsIgnoreCase("Batch");

			if (isBatch) {
				int runId = m_APMUtils.getWorkflowRunId(tAPMArgumentTable, iServiceId);
				apmLogger.register(Metrics.RUN_ID.toString(), runId);
			} // for the else, the op service id

			int pid = Ref.getProcessId();
			apmLogger.register(Metrics.PID.toString(), pid);

			String hostname = m_APMUtils.getHostname();
			apmLogger.register(Metrics.HOSTNAME.toString(), hostname);

			if (isBatch) {
				ODateTime startTimeOlf = m_APMUtils.getWorkflowStartTime(tAPMArgumentTable, iServiceId);
				String startTime = Str.dtToString(startTimeOlf, DATE_FORMAT.DATE_FORMAT_ISO8601_EXTENDED.toInt());
				apmLogger.register(Metrics.START_TIME.toString(), startTime);
			} // for the else get from the op service id - does it actually exist

			// ////////////////////////////////////////////////////////////
			//
			// Fill in some of the deal processor common args table
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

				String sProcessingMessage = "'APM_Initialization' started for this portfolio group";
				m_APMUtils.APM_LogStatusMessage(iMode, 1, m_APMUtils.cStatusMsgTypeProcessingAlways, "", "", -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE,
				                                sProcessingMessage);
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);

				if (returnt != Util.NULL_TABLE && returnt != null)
					tAPMArgumentTable.setTable("Main returnt", 1, returnt);

				/* get the market data source param */
				tAPMArgumentTable.setInt("Market Data Source", 1, argt.getInt("market_data_source", 1));

				/* get the closing dataset ID param (if it exists) */
				tAPMArgumentTable.setInt("Closing Dataset ID", 1, argt.getInt("closing_dataset_id", 1));

				/* get the closing dataset date param (if it exists) */
				tAPMArgumentTable.setString("Closing Dataset Date", 1, argt.getString("closing_dataset_date", 1));

				// check the closing dataset date is ok
				if (Str.len(argt.getString("closing_dataset_date", 1)) > 0) {
					if (OCalendar.parseString(argt.getString("closing_dataset_date", 1)) < 0) {
						m_APMUtils.APM_PrintMessage(tAPMArgumentTable,
						                            "Invalid Date String specified for Closing Dataset Date property in the APM service.  Aborting");
						iRetVal = 0;
					}
				}

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

						StatisticsLogging.instance().setPackageContext(tAPMArgumentTable, sMissingPackage);
						String sPackageError = "No data tables/columns enabled for package: " + sMissingPackage + ", no data will be generated for this package.";
						m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sPackageError);
					}

					StatisticsLogging.instance().unSetPackageContext(tAPMArgumentTable);
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
			tAPMArgumentTable.setInt("Debug", 1, m_APMUtils.APM_GetOverallPackageSettingInt(tAPMArgumentTable, "debug_flag", 1, 0, 0));

			/* set running batch status for all datasets in this service to "Waiting to Run" */
			if (iRetVal != 0 && iMode == m_APMUtils.cModeBatch) {
				// ADS
				if (!m_APMUtils.useADS(tAPMArgumentTable)) {
					// global message
					m_APMUtils.APM_LogStatusMessage(iMode, 1, m_APMUtils.cStatusMsgTypeProcessingAlways, "", "", -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE,
					                                "Waiting to run");

				}
			}

			////////////////////////////////////////////////////////////////////////////////////////////////////
			//
			// If we are incremental processing remove any packages that don't support incremental processing
			//
			////////////////////////////////////////////////////////////////////////////////////////////////////
			if ((iMode != m_APMUtils.cModeBatch) && (iMode != m_APMUtils.cModeDealDoNothing)) {
				if (APM_RemovePackagesWithoutIncremental(tAPMArgumentTable) < 1) {
					// none left after removing, warn and switch to cModeDealDoNothing 
					m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "No packages to process that support incremental updates!");
					iMode = m_APMUtils.cModeDealDoNothing;
				}
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// APM Initialization
			//
			// //////////////////////////////////////////////////////////////////////////////////
			if (iRetVal == 1 && (iMode != m_APMUtils.cModeDealDoNothing)) {
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
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Skipped deal update.  Invalid Package for script");
				iMode = m_APMUtils.cModeDealDoNothing;
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// fill in package specific argument table parameters
			//
			// //////////////////////////////////////////////////////////////////////////////////
			if ((iRetVal == 1) && (iMode != m_APMUtils.cModeDealDoNothing)) {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Starting to populate argument table");
				iRetVal = APM_PopulateArgumentTable(iMode, tAPMArgumentTable, argt);
				if (iRetVal == 0)
					sErrMessage = "Failed to populate argument table";
				else
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Finished populating argument table");
			}

			if (iRetVal == 1) {
				String sProcessingMessage = "'APM_Initialization' finished for this portfolio group";
				m_APMUtils.APM_LogStatusMessage(iMode, 1, m_APMUtils.cStatusMsgTypeProcessing, "", "", -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE,
				                                sProcessingMessage);
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);
			}

			// //////////////////////////////////////////////////////////////////////////////////
			//
			// Run the main proc for each portfolio ...
			//
			// //////////////////////////////////////////////////////////////////////////////////
			if (iRetVal != 0) {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Starting to process each portfolio");
				iRetVal = APM_MainPortfolioLoop(tAPMArgumentTable, sServiceName, iMode, argt);
				if (iRetVal == 0)
					sErrMessage = "Failed portfolio processing";
				else
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Finished portfolio processing");
			} else {
				// Log error if necessary - its a global failure as we never entered the portfolio loop
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
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception at top level of RTPE_APM_Generic: " + t);
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
			} else if (iMode == m_APMUtils.cModeBlockDealUpdate) // don't fail on blocks as status driven from log_status
			{
				succeeded = true;
			} else {
				// don't fail on incrementals as otherwise the status in the ops service will be running rather than failed
				// theres only 1 row if we have hit an incrmental status
				Table tDealInfo = argt.getTable("Deal Info", 1);
				if (Table.isTableValid(tDealInfo) == 1) {
					tDealInfo.setInt("log_status", 1, OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt());

					succeeded = true;
				} else {
					succeeded = false;
				}
			}
		} else {
			succeeded = true;
		}

		apmLogger.log(Metrics.SUCCEEDED.toString(), String.valueOf(succeeded));
		try {
			apmLogger.close();
		} catch (Exception exception) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception when closing logger: " + exception.getMessage());

			String message = m_APMUtils.getStackTrace(exception);
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message + "\n");

			try {
				ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).flush();
			} catch (Throwable throwable) {
				message = "Error attempting to flush console capture wrapper, " + throwable.getMessage();
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message + "\n");
			}
		}

		if (succeeded) {
			Util.exitSucceed();
		} else {
			Util.exitFail();
		}
	}

	int APM_MainPortfolioLoop(Table tAPMArgumentTable, String sServiceName, int iMode, Table argt) throws OException {
		int iRetVal = 1;
		int iPortfolioId;
		Table tJobResults = Table.tableNew("Job Results");
		int iDealNum = -1;
		int iTranNum = -1;
		String sErrMessage = "";

		// -----------------------------------------------------------------------------------
		// ------------------------------- Start Portfolio Processing
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
		if (iMode != m_APMUtils.cModeDealDoNothing) {
			serviceJobs.APM_CreateJobTable(tSplitProcessJobs);
			serviceJobs.APM_CreateJobTable(tLocalJobs);
			serviceJobs.APM_CreateJobs(iMode, tSplitProcessJobs, tLocalJobs, tAPMArgumentTable, sServiceName, argt);
		}

		int iOverallRetVal = 1; // if any portfolio fails then fail this....

		// issue splitProcess requests first as they are most important
		if (iMode != m_APMUtils.cModeDealDoNothing && tSplitProcessJobs.getNumRows() > 0) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start issuing requests");
			iRetVal = serviceJobs.APM_IssueSplitRequests(tAPMArgumentTable, sServiceName, iMode, tSplitProcessJobs, tJobResults);
			if (iRetVal == 0)
				sErrMessage = "Job request(s) failed. Probable engine crash.";
			else {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End issuing requests");
				m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, "", "", -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE,
				                                "All processing jobs complete");
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

			// individual portfolio errors don't affect the loop - but need to be recorded for overall status
			if (iRetVal == 0) {
				iOverallRetVal = 0;
				iRetVal = 1;
			}
		}

		// loop around local jobs
		if (iRetVal == 1 && iMode != m_APMUtils.cModeDealDoNothing) {
			int pfolioQueryID;
			Table tjobArgumentTable;
			int numLocalJobs = tLocalJobs.getNumRows();

			if (numLocalJobs > 0)
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Processing " + numLocalJobs + " portfolio job requests locally");

			for (int iLocalJob = 1; iLocalJob <= numLocalJobs; iLocalJob++) {
				iPortfolioId = tLocalJobs.getInt("portfolio_id", iLocalJob);
				tjobArgumentTable = tLocalJobs.getTable("deal_arg_table", iLocalJob);

				StatisticsLogging.instance().setPortfolioContext(tjobArgumentTable, Table.formatRefInt(iPortfolioId, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE),
				                                                 iPortfolioId);

				// now check whether there are any deals for this pfolio
				if (iMode != m_APMUtils.cModeBatch) {
					iTranNum = tjobArgumentTable.getInt("Current TranNum", 1);
					iDealNum = tjobArgumentTable.getInt("Current DealNum", 1);
					iMode = tjobArgumentTable.getInt("Script Run Mode", 1);
					pfolioQueryID = tjobArgumentTable.getInt("pfolio_query_id", 1);
					if (pfolioQueryID == 0) // no deals for this portfolio, -1 means backouts only
						iMode = m_APMUtils.cModeDealDoNothing;
				}

				if (iMode == m_APMUtils.cModeDealDoNothing) {
					StatisticsLogging.instance().unSetPortfolioContext(tjobArgumentTable);
					continue;
				}

				if (iRetVal != 0) {
					m_APMUtils.APM_PrintDebugMessage(tjobArgumentTable, "Start run deal processor script locally");
					iRetVal = serviceJobs.APM_ProcessRequestLocally(tjobArgumentTable, iMode, tLocalJobs, iLocalJob, tJobResults);
					if (iRetVal == 0)
						sErrMessage = "Local deal processor job request failed.";
					else {
						m_APMUtils.APM_PrintDebugMessage(tjobArgumentTable, "Complete run deal processor script locally");
						m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, "", "", iPortfolioId, -1, iTranNum, iDealNum,
						                                tjobArgumentTable, Util.NULL_TABLE, "Completed processing job");
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

				// individual portfolio errors don't affect the loop - but need to be recorded for overall status
				if (iRetVal == 0) {
					iOverallRetVal = 0;
					iRetVal = 1;
				}

				// Clear the reporting portfolio Id
				StatisticsLogging.instance().unSetPortfolioContext(tjobArgumentTable);
			}
		}
		// -----------------------------------------------------------------------------------
		// ------------------------------- end portfolio loop
		// ------------------------------
		// -----------------------------------------------------------------------------------
		iRetVal = iOverallRetVal; // set the correct status (if any pfolio fails - fail this)

		// also clear those messages with a pfolio of 0 - can occur through bad
		// setup
		if (iRetVal == 1 && iMode == m_APMUtils.cModeBatch) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Starting to clear global messages from tfe_msg_log");
			iRetVal = m_APMUtils.APM_ClearTFEMsgLogForBadDealUpdates(iMode, 0, -1, "", tAPMArgumentTable);
			if (iRetVal == 0) {
				m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeFailure, "", "", -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE,
				                                "Failed to clear global messages from message log (tfe_msg_log)");
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Failed to clear global messages from message log (tfe_msg_log)");
			} else
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Finished clearing global messages from tfe_msg_log");
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
	Name:          APM_CheckColumn
	Description:   <what the function does>
	Parameters:      <any parameters it accepts>
	Return Values:   <any return values>
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	int APM_CheckColumn(Table tCheck, String sCol, int iType) throws OException
	// Checks the presence of column "sCol" in table tCheck with type iType
	// Return 1 for OK, 0 for absent/wrong type
	{
		// Column present
		if (tCheck.getColNum(sCol) < 1)
			return (0);

		// With correct type
		if (tCheck.getColType(sCol) != iType)
			return (0);

		return (1);
	} // APM_CheckColumn/3.

	/*-------------------------------------------------------------------------------
	Name:          APM_FindLaunchType
	Description:   This function determines wether the script has been run
	           in batch or deal mode

	Parameters:   <any parameters it accepts>
	Return Values: The function returns cMode?? if the script is run in deal mode,
	           m_APMUtils.cModeBatch if the script is run in batch mode.
	           If the way that the script can not be determined,m_APMUtils.cModeUnknown is returned.
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	int APM_FindLaunchType(Table tMainArgt, Table tAPMArgumentTable) throws OException
	// Analyses the table to decide if it is running in batch, or as an ops
	// service script
	{
		Table tDealInfo;
		String sProcessingMessage;

		// batch
		if (APM_CheckColumn(tMainArgt, "selected_criteria", COL_TYPE_ENUM.COL_TABLE.toInt()) == 1
		        && Table.isTableValid(tMainArgt.getTable("selected_criteria", 1)) == 1) {
			if (APM_CheckColumn(tMainArgt, "method_id", COL_TYPE_ENUM.COL_INT.toInt()) == 1 && tMainArgt.getInt("method_id", 1) > 0)
				return (m_APMUtils.cModeBatch);
		}

		// deal booking
		/*
		 * See if this script is running as an Ops Service and if so in what
		 * context ...
		 */
		if (APM_CheckColumn(tAPMArgumentTable, "Global Filtered Deal Info", COL_TYPE_ENUM.COL_TABLE.toInt()) != 0) {
			tDealInfo = tAPMArgumentTable.getTable("Global Filtered Deal Info", 1);

			if (Table.isTableValid(tDealInfo) == 0 || (tDealInfo.getNumRows() < 1)) {
				// When there is no deal info, return "do nothing"
				// This can happen where it is an internal deal, and APM is not
				// interested in this side's portfolio
				return m_APMUtils.cModeDealDoNothing;
			}

			// handle deal mode - this also handles/logs errors if mode unknown
			// for deals
			return APM_FindDealLaunchType(tMainArgt, tAPMArgumentTable);
		}

		// if we get this far we're confused !
		sProcessingMessage = "ERROR: Unknown APM Script Mode !!!\n";
		m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, sProcessingMessage);

		return (m_APMUtils.cModeUnknown);
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_FindDealLaunchType
	Description:   This function determines run mode for each deal update

	-------------------------------------------------------------------------------*/
	int APM_FindDealLaunchType(Table tMainArgt, Table tAPMArgumentTable) throws OException {
		Table tDealInfo;
		Table tOpSvcDefn, tOpsCriteria = Util.NULL_TABLE, tTranStatus, tArgtDealInfo;
		int iToStatus, iFromStatus;
		int iDealRow;
		int iDealMode = 0;
		int iNumDealRows;
		int iBlockUpdates = 0;
		String sProcessingMessage;

		// in this instance look to see if we are getting a block from the argt
		// a block of 1 deal is the same as a block of n deals even if the filtered
		// table has 1 compared to n
		tArgtDealInfo = tMainArgt.getTable("Deal Info", 1);
		tDealInfo = tAPMArgumentTable.getTable("Global Filtered Deal Info", 1);

		if (Table.isTableValid(tArgtDealInfo) == 0 || (tArgtDealInfo.getNumRows() < 1)) {
			// When there is no deal info, return "do nothing"
			// This can happen where it is an internal deal, and APM is not
			// interested in this side's portfolio
			return m_APMUtils.cModeDealDoNothing;
		}

		tOpSvcDefn = tMainArgt.getTable("Operation Service Definition", 1);

		// if we've got more than one deal enter block mode, whether endur cut
		// will give us a block or not, internal deals could also give > 1 deal
		// use the argt deal info to decide whether we have a block but then add col to filtered tran info
		iNumDealRows = tArgtDealInfo.getNumRows();
		if (iNumDealRows > 1) {
			if (tDealInfo.getColNum("apm_deal_mode") < 1)
				tDealInfo.addCol("apm_deal_mode", COL_TYPE_ENUM.COL_INT);
			iBlockUpdates = 1;
		}

		int iOpsCriteriaColNum = tOpSvcDefn.getColNum("criteria_types_checked");
		if (iOpsCriteriaColNum > 0)
			tOpsCriteria = tOpSvcDefn.getTable(iOpsCriteriaColNum, 1);
		tTranStatus = Util.NULL_TABLE;
		if (Table.isTableValid(tOpsCriteria) != 0) {
			int iTranStatusRow = tOpsCriteria.unsortedFindInt("criteria_type", 4); // tran statuses
			tTranStatus = tOpsCriteria.getTable("selected", iTranStatusRow);
		}

		iNumDealRows = tDealInfo.getNumRows(); // reset to actual number of rows

		// for each deal (or only one if we're not doing a block update)
		for (iDealRow = 1; iDealRow <= iNumDealRows; iDealRow++) {
			iToStatus = tDealInfo.getInt("to_status", iDealRow);
			iFromStatus = tDealInfo.getInt("from_status", iDealRow);

			/* make sure we are processing an adjustment we really want
			 * for instance we don't want to process a schedule change on an
			 * amended new trade unless we actually care about amended new status */
			if (Table.isTableValid(tTranStatus) != 0) {
				/* if we are moving to an internal status (>= 10000)
				 * then we don't want to do anything if the original status is not of interest
				 * i.e. is not in the tran status list */
				if (iToStatus >= 10000 && tTranStatus.unsortedFindInt("id", iFromStatus) < 1)
					iDealMode = m_APMUtils.cModeDealDoNothing;

				// we also do not want to do anything if we are at cancelled new status and cancelled new is not being monitored
				// otherwise the position moves from validated to cancelled new in the APM pages
				if (iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED_NEW.toInt() && tTranStatus.unsortedFindInt("id", iToStatus) < 1)
					iDealMode = m_APMUtils.cModeDealDoNothing;
			}

			/* backout if a deal exists */
			if (iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_TEMPLATE.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_SPLIT_CLOSED.toInt()
			        || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_GIVEUP_CLOSED.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED_CLOSED.toInt()
			        || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()) {
				iDealMode = m_APMUtils.cModeDealBackout;
			}

			/*
			 * NB. Cancellation has been added in here so that we can get cell
			 * updates in APM
			 */
			if (iDealMode == 0
			        && (iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_PROPOSED.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_PENDING.toInt()
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_ROLLOVER_NEW.toInt()
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_BUYOUT_SPLIT_NEW.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt()
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED.toInt() /* must include amended status for 2 step amends */
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED_NEW.toInt() /* must include amended new status in case people add amended new to list of monitored statuses */
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_BUYOUT.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CLOSEOUT.toInt()
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED_NEW
			                .toInt())) {
				iDealMode = m_APMUtils.cModeDealBackoutAndApply;
			}

			/*
			 * Internal statuses - got greater than equal test to avoid having
			 * to add when new internal statuses added into Endur (has tripped
			 * us up - causes bad deal updates)
			 */
			if (iDealMode == 0 && iToStatus >= 10000) {
				iDealMode = m_APMUtils.cModeDealBackoutAndApply;
			}

			if (iDealMode != 0) {
				if (iBlockUpdates != 0)
					tDealInfo.setInt("apm_deal_mode", iDealRow, iDealMode);
				continue;
			}

			// log an error and return as soon as we get an unknown run type as
			// we can't guarantee the whole set of deals
			tDealInfo.setInt("apm_deal_mode", iDealRow, m_APMUtils.cModeUnknown);
			sProcessingMessage = "ERROR: Unknown APM Script Mode !!!";
			sProcessingMessage = sProcessingMessage + " Deal Number: " + tDealInfo.getInt("deal_tracking_num", iDealRow);
			sProcessingMessage = sProcessingMessage + " Tran Number: " + tDealInfo.getInt("tran_num", iDealRow);
			sProcessingMessage = sProcessingMessage + " From Status: " + iFromStatus;
			sProcessingMessage = sProcessingMessage + " To Status: " + iToStatus;
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, sProcessingMessage);
			return m_APMUtils.cModeUnknown;
		}

		// if we've got block updates return that as the run mode, if not just
		// return the standard run mode for the single deal
		if (iBlockUpdates == 1)
			return m_APMUtils.cModeBlockDealUpdate;
		else
			return iDealMode;

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
				StatisticsLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);

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

			StatisticsLogging.instance().unSetPackageContext(tAPMArgumentTable);
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

		serviceId = tAPMArgumentTable.getInt("service", 1);

		tStats = Table.tableNew("params");
		tStats.addCol("rtp_def_id", COL_TYPE_ENUM.COL_INT);
		tStats.addCol("package_name", COL_TYPE_ENUM.COL_STRING);
		tStats.addRow();
		tStats.setInt(1, 1, serviceId);

		for (iRow = 1; (iRetVal == 1 && (iRow <= tPackageDetails.getNumRows())); iRow++) {
			sPackageName = tPackageDetails.getString("package_name", iRow);
			tStats.setString(2, 1, sPackageName);

			iRetVal = m_APMUtils.APM_DBASE_RunProc(tAPMArgumentTable, "USER_tfe_insert_b_stats_svc", tStats);

			if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable,
				                                 "Error updating statistics at end of batch run : "
				                                         + DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() failed") + "\n");
				iRetVal = 0;
			}
		}
		tStats.destroy();

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_FilterDealInfoTable
	Description:  Removes any duplicates from the deal info block so we don't 
	              process the same deal multiple times (no point)
	-------------------------------------------------------------------------------*/
	int APM_FilterDealInfoTable(Table tAPMArgumentTable, Table argt, Table tDealInfo) throws OException {

		// get rid of multiple lines for the same transaction - we only want to process the latest
		// note we are operating on a copy of the argt dealinfo - not the original !
		int row, dealRow, dealNo, opsRunID;

		Table uniqueDealNums = Table.tableNew();
		uniqueDealNums.select(tDealInfo, "DISTINCT, deal_tracking_num", "deal_tracking_num GT 0");
		for (row = 1; row <= uniqueDealNums.getNumRows(); row++) {
			dealNo = uniqueDealNums.getInt("deal_tracking_num", row);
			opsRunID = 0;

			// identify the highest ops run ID (i.e. latest entry) for this deal
			for (dealRow = 1; dealRow <= tDealInfo.getNumRows(); dealRow++) {
				if (dealNo == tDealInfo.getInt("deal_tracking_num", dealRow) && tDealInfo.getInt("op_services_run_id", dealRow) > opsRunID) {
					opsRunID = tDealInfo.getInt("op_services_run_id", dealRow);
				}
			}

			// now cycle through the deal info table and remove the older duplicates
			for (dealRow = tDealInfo.getNumRows(); dealRow >= 1; dealRow--) {
				if (dealNo == tDealInfo.getInt("deal_tracking_num", dealRow)) {
					if (tDealInfo.getInt("op_services_run_id", dealRow) < opsRunID) {
						m_APMUtils.APM_PrintDealInfoRow(tAPMArgumentTable, argt, tDealInfo, "Removing tran with older ops service run id: ", dealRow);
						tDealInfo.delRow(dealRow);
					}
				}
			}
		}

		// check that there is only 1 row per deal
		if (uniqueDealNums.getNumRows() < tDealInfo.getNumRows()) {
			// grrr...why aren't we unique - have to do it manually now and just blow away rows until we are unique
			uniqueDealNums.addCol("found_flag", COL_TYPE_ENUM.COL_INT);
			uniqueDealNums.sortCol(1);
			for (dealRow = tDealInfo.getNumRows(); dealRow >= 1; dealRow--) {
				row = uniqueDealNums.findInt(1, tDealInfo.getInt("deal_tracking_num", dealRow), com.olf.openjvs.enums.SEARCH_ENUM.FIRST_IN_GROUP);
				if (uniqueDealNums.getInt(2, row) == 0)
					uniqueDealNums.setInt(2, row, 1); // not found already, set the found flag
				else {
					m_APMUtils.APM_PrintDealInfoRow(tAPMArgumentTable, argt, tDealInfo, "Removing tran due to duplicate dealnum: ", dealRow);
					tDealInfo.delRow(dealRow); // found already, delete
				}
			}
		}

		uniqueDealNums.destroy();

		return 1;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_ReEvaluateUndoAmendedNewTrans
	Description:  re-evaluates any trades at amended new that are rolled back using undo amend
	              It works out the prior tran and resubmits it.
	-------------------------------------------------------------------------------*/
	int APM_ReEvaluateUndoAmendedNewTrans(Table tAPMArgumentTable, Table argt, Table tDealInfo) throws OException {

		int row, newRow;
		Table undoAmendTranList = Table.tableNew("Undo Amend list");
		undoAmendTranList.addCol("deal_tracking_num", COL_TYPE_ENUM.COL_INT);

		// work out what trades are at amended new and are being rolled back
		for (row = 1; row <= tDealInfo.getNumRows(); row++) {
			if (tDealInfo.getInt("from_status", row) == TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED_NEW.toInt()
			        && tDealInfo.getInt("to_status", row) == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()) {
				newRow = undoAmendTranList.addRow();
				undoAmendTranList.setInt(1, newRow, tDealInfo.getInt("deal_tracking_num", row));
			}
		}

		if (undoAmendTranList.getNumRows() == 0) {
			undoAmendTranList.destroy();
			return 1;
		}

		int newQueryId = m_APMUtils.APM_TABLE_QueryInsert(tAPMArgumentTable, undoAmendTranList, 1);

		// work out the latest validated tran nums associated with the deals having an undo amend undertaken 
		Table validatedtranList = Table.tableNew("Validated Tran list");
		int iRetVal = m_APMUtils
		        .APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, validatedtranList, "max(ab.tran_num), ab.deal_tracking_num", "ab_tran ab, query_result q",
		                                     "q.unique_id = " + newQueryId
		                                             + " and ab.deal_tracking_num = q.query_result and ab.tran_status = 3 group by ab.deal_tracking_num");

		// now send all the latest validated transactions into the re-evaluation API
		int validatedQueryId = 0;
		if (iRetVal != 0 && validatedtranList.getNumRows() > 0) {
			validatedQueryId = m_APMUtils.APM_TABLE_QueryInsert(tAPMArgumentTable, validatedtranList, 1);
			TranOpService.reEvaluateTrades(validatedQueryId);
		}

		// cleanup
		Query.clear(newQueryId);
		Query.clear(validatedQueryId);
		validatedtranList.destroy();
		undoAmendTranList.destroy();
		return 1;
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

		tArgumentTable.addCol("Selected Portfolios", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Current Portfolio", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Current Package", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Current Scenario", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("Filtered Deal Info", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Global Filtered Deal Info", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("pfolio_query_id", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("Main returnt", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Script Run Mode", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Bad Deal Updates", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Log File", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Debug", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Package Name", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Market Data Source", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Closing Dataset ID", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Closing Dataset Date", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("op_services_run_id", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("service", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("dataset_type", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("Job Query", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Tranche", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Job Name", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Current Pfolio Has No Deals", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("Current TranNum", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Current DealNum", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Current DealNum Version", COL_TYPE_ENUM.COL_INT);
		tArgumentTable.addCol("Previous Portfolio", COL_TYPE_ENUM.COL_INT);

		tArgumentTable.addCol("Message Context", COL_TYPE_ENUM.COL_TABLE);

		tArgumentTable.addCol("Portfolio_Scenario_List", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Simulation Name", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.addCol("Overall Package Sim Results", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("Sim Def", COL_TYPE_ENUM.COL_TABLE);

		tArgumentTable.addCol("Main Argt", COL_TYPE_ENUM.COL_TABLE);
		tArgumentTable.addCol("RTP Page Prefix", COL_TYPE_ENUM.COL_STRING);

		tArgumentTable.addCol("Package Details", COL_TYPE_ENUM.COL_TABLE); // Package

		tArgumentTable.addCol("Batch Failures", COL_TYPE_ENUM.COL_TABLE); // Package

		// Contains a set of missing packages...
		m_missingPackages = new HashSet<String>();

		tArgumentTable.addRow();

		tMsgContext = Table.tableNew("Message Context");
		tMsgContext.addCol("ContextName", COL_TYPE_ENUM.COL_STRING);
		tMsgContext.addCol("ContextValue", COL_TYPE_ENUM.COL_STRING);
		tArgumentTable.setTable("Message Context", 1, tMsgContext);

		tBatchErrs = Table.tableNew("Batch Failures");
		tBatchErrs.addCol("portfolio", COL_TYPE_ENUM.COL_INT);
		tBatchErrs.addCol("package", COL_TYPE_ENUM.COL_STRING);
		tBatchErrs.addCol("scenario", COL_TYPE_ENUM.COL_INT);
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
	String APM_InitGlobals(Table tAPMArgumentTable, Table argt) throws OException {
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
				m_APMUtils
				        .APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable,
				                                     "USER_CreateAPMReportingTable unable to retrieve results from call to USER_apm_get_pkg_tables stored proc failed");
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

						if (tPackageDataTableCols.getInt("column_type", iCol) == COL_TYPE_ENUM.COL_INT.toInt()) {
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
		Table tPortfolios = Util.NULL_TABLE, tMainArgt;
		String sProcessingMessage;
		int dealGroupingColNum;

		tMainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
		iMode = tAPMArgumentTable.getInt("Script Run Mode", 1);

		m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start Checking Version");
		iRetVal = APM_CheckVersion(iMode, tAPMArgumentTable);
		if (iRetVal != 0)
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End Checking Version");

		if (iRetVal != 0 && iMode == m_APMUtils.cModeBatch) {
			/* find any bad deal updates & set them in the argt */
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start Finding Bad Deal Updates");
			iRetVal = m_APMUtils.APM_FindbadDealUpdates(iMode, iServiceId, tAPMArgumentTable, tPortfolios);
		}

		/* clear the global messages from the message log for this defn */
		if (iRetVal == 1 && iMode == m_APMUtils.cModeBatch) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start Clearing Msg Log for GLOBAL msgs");

			/* find any bad deal updates & set them in the argt */
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start Finding Bad Deal Updates");
			iRetVal = m_APMUtils.APM_FindbadDealUpdates(iMode, iServiceId, tAPMArgumentTable, tPortfolios);

			sProcessingMessage = tAPMArgumentTable.getString("RTP Page Prefix", 1) + iServiceId + "_GLOBAL";
			// global level rather than portfolio level
			APM_MessageLogUtilities messageLogUtils = new APM_MessageLogUtilities();
			iRetVal = messageLogUtils.APM_ClearEntriesInTFEMsgLog(sProcessingMessage, -1, tAPMArgumentTable, iServiceId);
			if (iRetVal != 0)
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End Clearing Msg Log for GLOBAL msgs");
		}

		/* Setup Selected Portfolios */
		if (iRetVal != 0) {
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Start getting selected portfolios");
			iRetVal = APM_GetSelectedPortfolios(iMode, tAPMArgumentTable);
			if (iRetVal != 0)
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "End getting selected portfolios");
		}

		/* fix the argt to enable block deal updates if supported by endur cut */
		if ((iRetVal == 1) && ((iMode == m_APMUtils.cModeDealApply) || (iMode == m_APMUtils.cModeDealBackout) || (iMode == m_APMUtils.cModeDealBackoutAndApply))) {
			dealGroupingColNum = tMainArgt.getColNum("deal_grouping");
			if (dealGroupingColNum > 0)
				tMainArgt.setInt("deal_grouping", 1, 1);
		}

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
		int iVersionStartMinor = 0;
		int iVersionStartRevision = 6;

		int iVersionEndMajor = 14;
		int iVersionEndMinor = 0;
		int iVersionEndRevision = 6;

		String sVersion = "14.0.6";
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
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable,
					                                       "Active Position Manager Server Installation is incompatible with this script.\n" + "Script Version = "
					                                               + sVersion + "\n" + "Server Installation Version = " + sSchemaVersion + "\n"
					                                               + "Please perform an Active Position Manager Server Installation.\nScript Terminating.\n");
				}
			}
		}
		tAPMVersion.destroy();
		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_GetSelectedPortfolios
	Description:   Creates a table of one column that lists all the internal portfolios
	           that are specified in the service selection criteria or argt
	Parameters:
	Return Values: tPortfolio, The table of internal portfolio numbers
	Effects:       none
	-------------------------------------------------------------------------------*/
	int APM_GetSelectedPortfolios(int iMode, Table tAPMArgumentTable) throws OException {
		Table tDealInfo;
		Table tPortfolios;
		Table tSelectedCriteria;
		Table tSelected;
		Table tMainArgt;
		int iRetVal = 1;
		int iRow;

		tPortfolios = Table.tableNew();
		tPortfolios.addCol("intpfolio", COL_TYPE_ENUM.COL_INT);

		tMainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
		if (iMode == m_APMUtils.cModeBatch) {
			tSelectedCriteria = tMainArgt.getTable("selected_criteria", 1);

			String criteriaTableColName = "filter_table";
			if (tSelectedCriteria.getColNum("criteria_table") > 0)
				criteriaTableColName = "criteria_table";

			String criteriaTypeColName = "filter_type";
			if (tSelectedCriteria.getColNum("criteria_type") > 0)
				criteriaTypeColName = "criteria_type";

			if (tSelectedCriteria.getColNum(criteriaTypeColName) > 0)
				iRow = tSelectedCriteria.unsortedFindInt(criteriaTypeColName, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE.toInt());
			else
				iRow = tSelectedCriteria.unsortedFindString("criteria_name", "Internal Portfolio", SEARCH_CASE_ENUM.CASE_SENSITIVE);

			if (iRow > 0) {
				if (tSelectedCriteria.getColNum(criteriaTableColName) > 0) {
					tSelected = tSelectedCriteria.getTable(criteriaTableColName, iRow);
					tPortfolios.select(tSelected, "id(intpfolio)", "id GT 0");
				} else {
					tSelected = tSelectedCriteria.getTable("selected", iRow);
					tPortfolios.select(tSelected, "id_number(intpfolio)", "id_number GT 0");
				}
			}
		} else {
			// use the global table as we are outside the pfolio loop
			tDealInfo = tAPMArgumentTable.getTable("Global Filtered Deal Info", 1);
			if (tDealInfo.getColNum("internal_portfolio") > 0) {
				tPortfolios.select(tDealInfo, "DISTINCT, internal_portfolio (intpfolio)", "internal_portfolio GT 0");
			}
		}

		if (iRetVal == 1 && (tPortfolios.getNumRows() < 1)) {
			// If we can't find find any portfolios for a batch, fail the batch
			if (iMode == m_APMUtils.cModeBatch) {
				iRetVal = 0;
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "No portfolios specified in APM service Selected Criteria. Exiting ...");
			} else {
				// For a deal update, just log an error message but don't set
				// the error as otherwise the deal will retry forever
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "No portfolios found for this deal");
			}
		} else {
			tPortfolios.makeTableUnique();
			tAPMArgumentTable.setTable("Selected Portfolios", 1, tPortfolios);
		}

		return (iRetVal);
	}

	/*
	 * Get a copy of the "Deal Info" table from the argt, 
	 * 
	 * Also fixes up the op_services_run_id for single deal updates
	 * 
	 */
	Table APM_RetrieveDealInfoCopyFromArgt(Table tAPMArgumentTable, Table argt) throws OException {
		Table dealInfo;
		Table opServicesLog;
		dealInfo = argt.getTable("Deal Info", 1).copyTable();

		// If a single deal update we need to go into the op_services_log table to get the op services run id.
		// For block update we should have the id already (also tables don't match so can't do join anyway!)
		if (dealInfo.getNumRows() == 1) {
			opServicesLog = argt.getTable("op_services_log", 1);
			if (Table.isTableValid(opServicesLog) == 1) {
				if (opServicesLog.getNumRows() > 1) {
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "More than one row in op_services_log for single deal update. Taking first entry!");
				}

				String runIDColName = "run_id";
				if (opServicesLog.getColNum("op_services_run_id") > 0)
					runIDColName = "op_services_run_id";
				dealInfo.setInt("op_services_run_id", 1, opServicesLog.getInt(runIDColName, 1));
			}
		}
		return dealInfo;
	}
}
