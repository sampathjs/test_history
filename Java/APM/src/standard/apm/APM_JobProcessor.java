/* Released with version 05-Feb-2020_V17_0_126 of APM */

/*
Description : This forms part of the Trader Front End, Active Position Manager
              package

-------------------------------------------------------------------------------
Revision No.  Date        Who  Description
-------------------------------------------------------------------------------
1.0.0         
 */
package standard.apm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import standard.apm.statistics.IApmStatisticsLogger;
import standard.apm.statistics.Scope;
import standard.include.APM_Utils;
import standard.include.APM_Utils.EntityType;
import standard.include.ConsoleCaptureWrapper;
import standard.include.ConsoleLogging;
import standard.include.LogConfigurator;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.QueryRequest;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Sim;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OP_SERVICES_LOG_STATUS;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SIMULATION_RUN_TYPE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.fnd.RefBase;

public class APM_JobProcessor implements IScript {
	private APM_Utils m_APMUtils;

	private int m_runId;

	boolean tfeSettingForSaveArgt = false;  // TBD - add global tfe_config setting

	public APM_JobProcessor() {
		m_APMUtils = new APM_Utils();

		m_runId = -1;
	}

	// APM_PerformOperation - sim parameters: row identifier enum
	int APM_ROW_VERSION_ID = 1;
	int APM_ROW_SIM_DEF_ID = 2;
	int APM_ROW_USER_PERMS = 3;
	int APM_ROW_GROUP_PERMS = 4;
	int APM_ROW_PUBLIC_PERMS = 5;

	// PUT_CALL_ENUM.PUT/PUT_CALL_ENUM.CALL not supported pre-v70r1 so use these instead
	int PUT_CONST = 0;
	int CALL_CONST = 1;

	public void execute(IContainerContext context) throws OException {

		final Calendar startTimestamp = Calendar.getInstance();

		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		Table tAPMArgumentTable = Util.NULL_TABLE;
		Table tRevalParam = Table.tableNew("Reval Param");
		Table tBatchFailures = null;
		String sProcessingMessage = " ";
		String sJobName;
		String sErrMessage = "";
		int iQueryId = 0;
		int iMode = m_APMUtils.cModeUnknown;
		int entityGroupId;
		int iRetVal = 1;
		int iJobNum;
		QueryRequest qreq = null;
		int runJobLocallyFlag = 1;
		Table revalPostreturnt;
		IApmStatisticsLogger entityGroupLogger = null;
		boolean succeeded = true;
		ODateTime processorScriptStartTime = null;
		
		try {
			processorScriptStartTime = ODateTime.dtNew();
			processorScriptStartTime = APM_Utils.getODateTime();

			/////////////////////////////////////////////////////
			//
			// Get the argument table, extract common data we need
			// then enrich with more job specific data
			//
			/////////////////////////////////////////////////////
			if (argt.getNumRows() > 0)
				tAPMArgumentTable = argt.getTable("job_arg_table", 1);
			if (Table.isTableValid(tAPMArgumentTable) == 0) {
				OConsole.oprint("Failed to get processor argt table. Exiting ...");
				// This script should not return an error as it will cause a cluster engine job failure and re-run,
				Util.exitSucceed();
			}
		
			// Set the path/log information.		
			String logFilePath = "";
			if (Str.isEmpty(Util.getEnv("AB_ERROR_LOGS_PATH")) == 1)
				logFilePath = Util.getEnv("AB_OUTDIR") + "/error_logs/";
			else
				logFilePath = Util.getEnv("AB_ERROR_LOGS_PATH") + "/";

			// Set the log path to be used by our logger.
			LogConfigurator.getInstance().setPath(logFilePath);

			String serviceName = tAPMArgumentTable.getString("service_name", 1);

			entityGroupLogger = m_APMUtils.newLogger(Scope.ENTITYGROUP, serviceName);
			entityGroupLogger.start();

			/*
			 	STATISTICS
			 	
				Get the service statistics from tAPMArgumentTable Statistics
				and put them into Map<String,Object>
				
				Call IApmStatisticsLogger::setAll() for entityGroupLogger
			*/
			Table tAllStatistics = tAPMArgumentTable.getTable("Statistics", 1);
			int row = tAllStatistics.unsortedFindString("Scope", "Service", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
			if (row > 0) {
				Table tServiceStatistics = tAllStatistics.getTable("Scope_Statistics", row);
				Map<String,Object> serviceContexts = m_APMUtils.convertStatisticsTableToMap(tServiceStatistics);
				entityGroupLogger.setAll(serviceContexts);
			}
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
			String scriptStartTimestamp = dateFormat.format(startTimestamp.getTime());

			entityGroupLogger.setMetric("entityGroupStartTime", scriptStartTimestamp);

			LogConfigurator.getInstance().setServiceName(serviceName);
			LogConfigurator.getInstance().push(tAPMArgumentTable.getString("Log File", 1));
			tAPMArgumentTable.setString("Log File", 1, LogConfigurator.getInstance().front());

			if (!ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).isOpen()) {
				// Open the console capture and register this object as its owner.
				ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).open(this);
			}

			ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "APM_JobProcessor");
			ConsoleLogging.instance().setProcessIdContext(tAPMArgumentTable, Ref.getProcessId());

			/////////////////////////////////////////////////////
			//
			// Get the split specific arguments
			//
			/////////////////////////////////////////////////////

			// job info
			sJobName = argt.getString("job_name", 1);
			iJobNum = argt.getInt("job_num", 1);

			ConsoleLogging.instance().setJobContext(tAPMArgumentTable, sJobName);

			// run mode - get this up here so that error logging works correctly
			iMode = tAPMArgumentTable.getInt("Script Run Mode", 1);

			int pid = Ref.getProcessId();
			entityGroupLogger.setContext("entityGroupPid", String.valueOf(pid));

			/*
			String uniqueRunId = "";
			int serviceId = tAPMArgumentTable.getInt("service_id", 1);
			if (iMode == m_APMUtils.cModeBatch) {
				m_runId = m_APMUtils.getWorkflowRunId(tAPMArgumentTable, serviceId);

				uniqueRunId = String.valueOf(m_runId);
			} else {
				m_runId = m_APMUtils.getOpServicesRunId(tAPMArgumentTable);

				uniqueRunId = String.format("%d:%d", m_runId, serviceId);
			}
			entityGroupLogger.setContext("runId", uniqueRunId);
			*/
			
			String moduleId = String.format("%d", Ref.getModuleId()); 
			entityGroupLogger.setContext("moduleName", Ref.getModuleName());
			entityGroupLogger.setContext("moduleId", moduleId);

			Table tMainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
			
			m_APMUtils.FindAndSetEntityType(tMainArgt, tAPMArgumentTable);
			
			// entity group ID
			entityGroupId = argt.getInt("entity_group_id", 1);
			String entityGroupName = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);

			ConsoleLogging.instance().setEntityGroupContext(tAPMArgumentTable, entityGroupName, entityGroupId);
			entityGroupLogger.setContext("entityGroupId", String.valueOf(entityGroupId));
			entityGroupLogger.setContext("entityGroupName", entityGroupName);
			
			int secondaryEntityNum = tAPMArgumentTable.getInt("Current Secondary Entity Num", 1);
			int primaryEntityNum = tAPMArgumentTable.getInt("Current Primary Entity Num", 1);
			int entityVersion = tAPMArgumentTable.getInt("Current Entity Version", 1);
			m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeStarted, "", "", entityGroupId, -1, secondaryEntityNum, primaryEntityNum, entityVersion, tAPMArgumentTable, Util.NULL_TABLE, "Starting to process ...");
			
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "================================================================");
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Starting to process " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + entityGroupName + " ...");
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "================================================================");

			runJobLocallyFlag = argt.getInt("Running Locally", 1);
			if (runJobLocallyFlag == 1)
				revalPostreturnt = argt.getTable("returnt", 1);
			else
				revalPostreturnt = returnt;

			// double check the return table is constructed correctly 
			m_APMUtils.APM_setupJobReturnt(revalPostreturnt);

			// make sure the correct job name and number is in there
			revalPostreturnt.setString("job_name", 1, sJobName);
			revalPostreturnt.setInt("job_num", 1, iJobNum);
			revalPostreturnt.setInt("entity_group_id", 1, entityGroupId);

			// set up the rest of the job processor arguments specific to this job
			iQueryId = -1;
			int noEntitiesInEntityGroupFlag = 0;
			if (iMode == m_APMUtils.cModeBatch) {
				// Call APM batch start to create the dataset information
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Initiating batch start operation for entity group");
				APM_BatchOps batchOperations = new APM_BatchOps();
				iRetVal = batchOperations.initialiseDatasets(tAPMArgumentTable, entityGroupId);
				if (iRetVal == 0)
					sErrMessage = "Failed batch start operation for entity group";
				else
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Completed batch start operation for entity group");

				if (iRetVal != 0) {
					boolean allowedAccess = true;
					if ( m_APMUtils.GetCurrentEntityType(tAPMArgumentTable) == EntityType.DEAL )
					{
                                           if (!PortfolioAccessAllowed(entityGroupId))
                                              allowedAccess = false;
					}
					if (allowedAccess) {
						qreq = APM_EntityJobOps.instance().createQueryIdFromMainArgt(iMode, tAPMArgumentTable, tMainArgt, entityGroupId);
						iQueryId = qreq.getQueryId();
						Table queryCount = Table.tableNew();
						m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, queryCount, "count(*)", "query_result", "unique_id = " + iQueryId);

						int numEntities = queryCount.getInt(1, 1);
						m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Query executed.  ID: " + iQueryId + " Count of entities = " + numEntities);
						queryCount.destroy();

						entityGroupLogger.setMetric("numberOfEntities", String.valueOf(numEntities));

						if (numEntities == 0) {
							// no eligible transactions - so set the no entities in pfolio flag
							noEntitiesInEntityGroupFlag = 1;
							m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "No eligible entities (deals or noms) returned for query.  Treating as empty entity group.");
						}
					} else {
						// not allowed access to this portfolio - so set the no entities in pfolio flag
						noEntitiesInEntityGroupFlag = 1;
						sProcessingMessage = "WARNING !! No access allowed to portfolio for this user: " + RefBase.getUserName() + ".  Treating as empty portfolio.";
						m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "WARNING !! No access allowed to portfolio for this user: " + RefBase.getUserName()
						        + ".  Treating as empty portfolio.");
						m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeWarn, sJobName, "", entityGroupId, -1, secondaryEntityNum, primaryEntityNum, entityVersion, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);

						entityGroupLogger.setMetric("numberOfEntities", String.valueOf(0));
					}
				}
			} else {
				iQueryId = tAPMArgumentTable.getInt("job_query_id", 1);
				Table globalFilteredEntityInfo = tAPMArgumentTable.getTable("Global Filtered Entity Info", 1);
				int numberOfRows = globalFilteredEntityInfo.getNumRows();
				entityGroupLogger.setMetric("numberOfEntities", String.valueOf(numberOfRows));
			}

			tAPMArgumentTable.setInt("Tranche", 1, 1); // set to 1 (default), can be overridden if revalservice mthod used
			tAPMArgumentTable.setInt("Job Query", 1, iQueryId);
			tAPMArgumentTable.setInt("Current Job Has No Entities", 1, noEntitiesInEntityGroupFlag);

			// check the query is ok
			if (iRetVal != 0) {
				if (iQueryId == 0 && noEntitiesInEntityGroupFlag == 0) {
					sErrMessage = "Invalid query";
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
					iRetVal = 0;
				}
			}

			if (iRetVal != 0)
				iMode = tAPMArgumentTable.getInt("Script Run Mode", 1);
			
			/*
			 	STATISTICS
			 	
				All the statistics before the APM call is made have now 
				been collated so now put them into the tAPMArgumentTable
				
				Create new table in tAPMArgumentTable called Statistics_EntityGroupScope
				Get the statistics from IApmStatisticsLogger::getAll()
				Put the statistics into tAPMArgumentTable using APM_Utils::convertMapToTable()
				
				This will flow down to the Simulation and the entityGroupIncremental scope
			*/			
	
			Map<String,Object> entityGroupContexts = entityGroupLogger.getAll();
			Table tEntityGroupStatistics = m_APMUtils.convertMapToStatisticsTable(entityGroupContexts);
			if (tEntityGroupStatistics.getNumRows() > 0) {
				//Add this table at EntityGroup scope level
				Table tStatistics = tAPMArgumentTable.getTable("Statistics", 1);
				row = tStatistics.addRow();
				tStatistics.setString("Scope", row, "EntityGroup");
				tStatistics.setTable("Scope_Statistics", row, tEntityGroupStatistics);
			}
			
			/////////////////////////////////////////////////////
			//
			// Do the user initialization (setup sim)
			//
			/////////////////////////////////////////////////////
			if (iRetVal == 1 && (iMode != m_APMUtils.cModeDoNothing)) {
				iRetVal = APM_Initialization(iMode, iQueryId, tRevalParam, tAPMArgumentTable, sJobName, argt.copyTable());
				if (iRetVal == 0) {
					sErrMessage = "Initialization failed";
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
				}
			}

			if (iRetVal != 0) {
				sProcessingMessage = "Starting to fill data for " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId) + " Job# "
				        + iJobNum;
				m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeProcessingAlways, sJobName, "", entityGroupId, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);
			}

			/////////////////////////////////////////////////////
			//
			// Fill Reporting Tables
			//
			///////////////////////////////////////////////////// 
			tBatchFailures = tAPMArgumentTable.getTable("Batch Failures", 1);

			if (iRetVal == 1) {
				if (noEntitiesInEntityGroupFlag == 1) {
					// just set the retval to succeeded as there are no entities to process
					revalPostreturnt.setInt("ret_val", 1, 1);
				} else {
					iRetVal = APM_FillPackageDataTables(iMode, iQueryId, entityGroupId, tAPMArgumentTable, tRevalParam, sJobName, argt, tBatchFailures, revalPostreturnt);
					if (iRetVal == 0) {
						sErrMessage = "Failed to Run Sim or Fill APM reporting tables for " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : "
						        + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);
					}
				}
			}

			// if update after split then we don't pass pack the reporting tables as this script was working
			// with the main script's tables (CallN)
			if (runJobLocallyFlag == 1) {
				Table pkgDataTables = revalPostreturnt.getTable("Package Data Tables", 1);
				if (Table.isTableValid(pkgDataTables) != 0)
					pkgDataTables.destroy();
			}

			// OConsole.oprint out the error message for this job
			if (iRetVal == 0) // no need to do status message here as the detailed msg will already have been sent
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, sErrMessage);

			// Perform APM batch end operation to set dataset as active
			if ((iRetVal == 1) && (iMode == m_APMUtils.cModeBatch)) {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Initiating batch end operation for entity group");
				APM_BatchOps batchOperations = new APM_BatchOps();
				iRetVal = batchOperations.commitPendingDatasets(tAPMArgumentTable, entityGroupId);
				if (iRetVal == 0)
					sErrMessage = "Failed batch end operation";
				else
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Completed batch end operation");
			}

			//Check value of iRetVal here, as if there was a failure iRetVal can get set
			//back to a success value in the call to APM_ClearMsgLogForBadUpdates below
			if (iRetVal == 0)
				succeeded = false;

			// if success then clear bad updates & send messages. Note
			// the equivalent is done for successful batch completions
			// just after the batch end operation
			// it may be that some of a block succeeded 
			// - so we need to clear those that were successful 
			if ((iRetVal == 1 && iMode != m_APMUtils.cModeBatch) || (iRetVal == 0 && iMode == m_APMUtils.cModeBlockUpdate)) {
				iRetVal = m_APMUtils.APM_ClearMsgLogForBadUpdates(iMode, entityGroupId, -1, "", tAPMArgumentTable);
				if (iRetVal == 0)
					sErrMessage = "Failed to clear bad updates from message log (apm_msg_log)";
			}
			
			long elapsedMs = Calendar.getInstance().getTimeInMillis() - startTimestamp.getTimeInMillis();
			entityGroupLogger.setMetric(IApmStatisticsLogger.ELAPSED_MILLISECONDS_KEY, String.valueOf(elapsedMs));

			ODateTime processorScriptCompletionTime = ODateTime.dtNew();
			processorScriptCompletionTime = APM_Utils.getODateTime();
			int jobDuration = 0;
			if ((processorScriptStartTime != null) && (processorScriptCompletionTime != null)) {
				jobDuration = processorScriptStartTime.computeTotalSecondsInGMTDateRange(processorScriptCompletionTime);
			}
			entityGroupLogger.setMetric("jobDuration", String.valueOf(jobDuration));
					

			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "================================================================");
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Completed processing " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId));
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "================================================================");
			
			// If the entity group failed save off the argt
			Table mainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
			if (iRetVal == 0)
				m_APMUtils.SaveArgtForRerunIfSweeperOn(iMode, mainArgt, tAPMArgumentTable, entityGroupId);
			else
				m_APMUtils.DeleteSweeperEntriesForEntityGroup(iMode, mainArgt, tAPMArgumentTable, entityGroupId);

		} catch(Exception exception) {
		    // log out the exception - the fail/succeed logic remains the same, but any exceptions aren't allowed to propagate
		    m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, exception.getMessage());
		} finally {
			
			if(succeeded) {
			    entityGroupLogger.stop();
			} else {
			    entityGroupLogger.abort();
			}
			
			ConsoleLogging.instance().unSetJobContext(tAPMArgumentTable);
		
			// get rid of the entity group level query ID - no leaks please
			APM_ServiceJobs serviceJobs = new APM_ServiceJobs();
			m_APMUtils.APM_DestroyJobQueryID(iMode, tAPMArgumentTable);

			// Stop any console logging operations.
			if (iRetVal == 0) {
				try {
					ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).flush();
				} catch (Throwable exception) {
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, exception.toString());
				}
			} else {
				ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).close(this);
			}

			// Reset the log path
			String logFile = LogConfigurator.getInstance().pop();
			tAPMArgumentTable.setString("Log File", 1, logFile);

			// clean up
			tRevalParam.destroy();

			if (qreq != null) // this will only be set if we have the new V11 where we execute the query
			{
				if (iQueryId > 0)
					Query.clear(iQueryId);
				qreq.destroy();
			}

			m_APMUtils.closeLogger(entityGroupLogger, tAPMArgumentTable);

			// This script should only return an error if the SplitProcess is in play and its a batch
			// then the splitprocess retry count can kick in to retry the entity group level job
			// Otherwise in case of an error the error code/message is returned to and handled by the main script.
			if (iRetVal == 0 && iMode == m_APMUtils.cModeBatch && runJobLocallyFlag == 0)
				Util.exitFail();
			else
				Util.exitSucceed();

		}

		Util.exitSucceed();
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_Initialization

	Description:   This function is called once per run

	           It is also often used to setup a simulation definition
	           (usually set as the RevalParam in 'argt')

	Parameters:    iMode - m_APMUtils.cModeBatch, m_APMUtils.cModeApply, m_APMUtils.cModeBackout

	Returns:       0 for FAIL, 1 for SUCCESS
	-------------------------------------------------------------------------------*/
	int APM_Initialization(int iMode, int iQueryId, Table tRevalParam, Table tAPMArgumentTable, String sJobName, Table argt) throws OException {
		int iRetVal;
		Table tScenarios;
		String sSimName;

		sSimName = argt.getString("simulation_name", 1);

		// list of scenarios for this processing job
		tScenarios = argt.getTable("scenario_list", 1);
		if (Table.isTableValid(tScenarios) == 0) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Invalid scenario list");
			iRetVal = 0;
		}

		// set the scenario list for the current entity group in the argument table
		tAPMArgumentTable.setTable("Scenario_List", 1, tScenarios.copyTable());

		iRetVal = APM_SetupRevalParam(iMode, tRevalParam, tScenarios, sSimName, tAPMArgumentTable, argt);

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_SetupRevalParam

	Description:   This function sets up the RevalParam in 'argt' to include a
	           base and double shift scenario

	Parameters:    iMode - m_APMUtils.cModeBatch, m_APMUtils.cModeApply, m_APMUtils.cModeBackout

	Returns:       0 for FAIL, 1 for SUCCESS
	-------------------------------------------------------------------------------*/
	int APM_SetupRevalParam(int iMode, Table tRevalParam, Table tEntityGroupScenarios, String sSimName, Table tAPMArgumentTable, Table argt) throws OException {
		Table tSimDef;
		Table tSourceSimDef;
		Table tSourceScenarios;
		Table tScenarios;
		Table tEnabledTranInfoFilters;
		int force_intraday_reval_type;
		int iScenario;
		int iNewScenarioRow;
		String sScenarioName, sWhat, sFrom, sWhere;
		int iRetVal;

		iRetVal = 1;

		// Create the revaluation parameters table
		Sim.createRevalTable(tRevalParam);
		tRevalParam.setInt("ClearCache", 1, 0);
		// Setting this flag should refresh the indexes & volatilities. However, it does not work for 
		// all branches so we must explicity refresh the market data.
		// For performance reasons, we do NOT want to refresh the market data for every update event
		tRevalParam.setInt("RefreshMktd", 1, 0);

		// changes for reval service
		String sServiceName = tAPMArgumentTable.getString("service_name", 1);
		APM_ServiceJobs serviceJobs = new APM_ServiceJobs();
		if (iMode == m_APMUtils.cModeBatch) {
			if (serviceJobs.APM_GetNumRevalServiceEngines(tAPMArgumentTable, sServiceName) == 0) {
				tRevalParam.setInt("AsmId", 1, -1);
			} else {
				if (tRevalParam.getColNum("ApmRevalData") < 1)
					tRevalParam.addCol("ApmRevalData", COL_TYPE_ENUM.COL_TABLE);

				tRevalParam.setTable("ApmRevalData", 1, argt);
				tRevalParam.setString("ServiceName", 1, sServiceName);
			}
		} else {
			tRevalParam.setInt("AsmId", 1, -1);
		}

		if (tAPMArgumentTable.getInt("Market Data Source", 1) == 2) {
			// if a closing dataset ID not specified revert to universal
			if (tAPMArgumentTable.getInt("Closing Dataset ID", 1) > 0) {
				// use closing prices
				tRevalParam.setInt("UseClose", 1, 1);
				tRevalParam.setInt("ClosingDatasetId", 1, tAPMArgumentTable.getInt("Closing Dataset ID", 1));
			} else if (iMode == m_APMUtils.cModeBatch ) {				
				tRevalParam.setInt("RefreshMktd", 1, 1);
				if ( tRevalParam.getColNum("PublishIndex") > 0 )
					   tRevalParam.setInt("PublishIndex", 1, 0);	
			}
		} else if ((iMode == m_APMUtils.cModeBatch) && (tAPMArgumentTable.getInt("Market Data Source", 1) == 0)) {
			tRevalParam.setInt("RefreshMktd", 1, 1);
			if ( tRevalParam.getColNum("PublishIndex") > 0 )
				   tRevalParam.setInt("PublishIndex", 1, 0);	
		}

		// Populate the revaluation parameters table
		tRevalParam.setInt("SimRunId", 1, -1);
		tRevalParam.setInt("SimDefId", 1, -1);

		force_intraday_reval_type = m_APMUtils.APM_GetOverallPackageSettingInt(tAPMArgumentTable, "force_intraday_reval_type", 1, 0, 0);

		// If the package requires prior EOD results to be loaded, it must be run as an INTRADAY revaluation
		// This is due to some buggy core code, where loading EOD results is dependent not on the SIM result itself
		// but on whether the revaluation is run as an INTRADAY (as opposed to GENERAL)
		if (force_intraday_reval_type > 0) {
			tRevalParam.setInt("RunType", 1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt());
		} else {
			tRevalParam.setInt("RunType", 1, SIMULATION_RUN_TYPE.GEN_SIM_TYPE.toInt());
		}

		tSourceSimDef = tAPMArgumentTable.getTable("Sim Def", 1);
		tSimDef = Sim.createSimDefTable();
		Sim.addSimulation(tSimDef, sSimName);

		tSourceScenarios = tSourceSimDef.getTable("scenario_def", 1);

		tScenarios = tSourceScenarios.cloneTable();
		tSimDef.setTable("scenario_def", 1, tScenarios);

		tEntityGroupScenarios.sortCol("scenario_name");
		// for each scenario
		for (iScenario = 1; iScenario <= tSourceScenarios.getNumRows(); iScenario++) {
			// Simulation definition
			sScenarioName = tSourceScenarios.getString("scenario_name", iScenario);

			if (tEntityGroupScenarios.findString("scenario_name", sScenarioName, SEARCH_ENUM.FIRST_IN_GROUP) > 0) {
				// scenario is list of job scenarios, add as normal
				tSourceScenarios.copyRowAdd(iScenario, tScenarios);
				iNewScenarioRow = tScenarios.getNumRows();
				tScenarios.setString("scenario_name", iNewScenarioRow, sScenarioName);
				tScenarios.setInt("scenario_id", iNewScenarioRow, iNewScenarioRow);
			} else
				continue; // not interested in this scenario
		}

		// Add details of the tran_info filter/splitter config to the sim def. This will be used by APM UDSRs
		sWhat = "distinct tfd.filter_id, tfd.filter_name, tfd.ref_list_id, aesr.result_column_name, aesr.column_name_append, tfd.filter_type";
		sFrom = "tfe_filter_defs tfd, apm_pkg_enrichment_config apec, apm_enrichment_source_results aesr";
		sWhere = "tfd.filter_type in (5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19) and tfd.filter_name = apec.enrichment_name "
		        + "and apec.on_off_flag = 1 and aesr.enrichment_name = apec.enrichment_name";

		tEnabledTranInfoFilters = m_APMUtils.APM_TABLE_LoadFromDbWithSQLCached(tAPMArgumentTable, "APM Enabled Tran Info Filters", "TFE.METADATA.CHANGED", sWhat, sFrom, sWhere);
		if (Table.isTableValid(tEnabledTranInfoFilters) == 0) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to load tran_info filter/splitter configuration details");
			iRetVal = 0;
		} else {
			tSimDef.addCol("APM Enabled Tran Info Filters", COL_TYPE_ENUM.COL_TABLE);
			tSimDef.setTable("APM Enabled Tran Info Filters", 1, tEnabledTranInfoFilters);
		}

		tRevalParam.setTable("SimulationDef", 1, tSimDef);

		// NOW IF A CLOSING DATASET DATE HAS BEEN SPECIFIED ADD A DATE HORIZON MOD
		if (tRevalParam.getInt("UseClose", 1) == 1 && (Str.len(tAPMArgumentTable.getString("Closing Dataset Date", 1)) > 0)
		        && (Str.equal(tAPMArgumentTable.getString("Closing Dataset Date", 1), "0d") == 0))	// closing dataset date != 0d		 
		{
			// ok - so this is going to be for a prior date - unset useclose as that 
			// causes todays close to be loaded accidentally
			tRevalParam.setInt("UseClose", 1, 0);

			for (iScenario = 1; iScenario <= tScenarios.getNumRows(); iScenario++) {
				sScenarioName = tScenarios.getString("scenario_name", iScenario);

				// If there is already a date horizon mod do not override and do not add one
				Table scenarioConfig = tScenarios.getTable("scenario_config_table", iScenario);
				if (scenarioConfig != Util.NULL_TABLE && Table.isTableValid(scenarioConfig) != 0) {
					if (scenarioConfig.unsortedFindInt("target_type", 4) > 0)
						continue;
				}

				// add the date horizon mod
				Sim.addHorizonDateMod(tSimDef, tSimDef.getString("name", 1), sScenarioName, 0, tAPMArgumentTable.getString("Closing Dataset Date", 1));
				// set the pricing method to current for hist prices and resets
				Sim.setHorizonDatePricingMethod(tSimDef, tSimDef.getString("name", 1), sScenarioName, 1, "", "", -1);
				// set the PV date to current date (today)
				Sim.setHorizonDatePVDateMode(tSimDef, tSimDef.getString("name", 1), sScenarioName, 0);
			}
		}

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_FillPackageDataTables

	Description:   This function is called for each entity group in turn
	           The function should process trade data (often by running
	           simulations) and subsequently fill the tPackageDataTables

	Parameters:    iMode - m_APMUtils.cModeBatch, m_APMUtils.cModeApply, m_APMUtils.cModeBackout
	           entityGroupId- the entity group id the entities fall into
	           tPackageDataTables - lower level functions should fill this

	Returns:       0 for FAIL, 1 for SUCCESS
	-------------------------------------------------------------------------------*/
	int APM_FillPackageDataTables(int iMode, int iQueryId, int entityGroupId, Table tAPMArgumentTable, Table tRevalParam, String sJobName, Table argt, Table tBatchFailures, Table revalPostreturnt) throws OException {
		int iRetVal;
		Table tEntityInfo;
		Table tJobScenarios;
		String sSimName;
		int iLogStatusCol;

		sSimName = argt.getString("simulation_name", 1);

		// list of scenarios for this processing job
		tJobScenarios = argt.getTable("scenario_list", 1);
		if (Table.isTableValid(tJobScenarios) == 0) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Invalid scenario list in job arguments");
			iRetVal = 0;
		}

		iRetVal = APM_RunSimulation(iMode, iQueryId, entityGroupId, tJobScenarios, tAPMArgumentTable, tRevalParam, sSimName, sJobName, argt, tBatchFailures, revalPostreturnt);

		//iRetVal = 0;
		//OConsole.oprint ("##### Failing Simulation Test #####\n");
		// set a failed status in the info table so the update gets re-run by the log monitor when block mode in operation
		if ((iRetVal == 0) && (iMode != m_APMUtils.cModeBatch)) {
			// If we're in block mode then set the log_status to failed for all entities in this block
			// This will create a bad update for all entities in block but they will then be re-reun by
			// the log monitor singularly so after the first log monitor run only the bad updates in the block will remain bad
			tEntityInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1); // use the filtered one (inside pfolio loop)
			iLogStatusCol = tEntityInfo.getColNum("log_status");
			if (iLogStatusCol > 0) {
				// now we need to only update the status of the entities affected by the failure
				// if we are in grid this could be a subset of the whole info table
				// as defined by the query_id
				Table tBlockEntities = Table.tableNew();
				tBlockEntities.addCol("query_result", COL_TYPE_ENUM.COL_INT);
				tBlockEntities.addCol("log_status", COL_TYPE_ENUM.COL_INT);
				m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tBlockEntities, "query_result", "query_result", "unique_id = " + iQueryId);
				tBlockEntities.setColValInt(2, OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt());
				tEntityInfo.select(tBlockEntities, "log_status", "query_result EQ $secondary_entity_num");
				tBlockEntities.destroy();
			}
		}

		return iRetVal;
	}

	int APM_SetupRevalParam(int iMode, Table tAPMArgumentTable, String sJobName, int iQueryId, int entityGroupId, Table tRevalParam) throws OException {
		int iRetVal = 1;

		/* get scenario info */
		Table tSimDef = tRevalParam.getTable("SimulationDef", 1);

		//Creating a query id based on the DISTINCT list of entities
		tRevalParam.setInt("QueryId", 1, iQueryId);

		//check for the prior reval types
		if (tRevalParam.getColNum("PriorRunType") > 0) {
			int priorRevalType = m_APMUtils.getPriorRevalTypeForPackages(iMode, tAPMArgumentTable, false);
			tRevalParam.setInt("PriorRunType", 1, priorRevalType);
		}

		APM_EntityJobOps.instance().SetupRevalParamForEntityType(iMode, tAPMArgumentTable, sJobName, iQueryId, entityGroupId, tRevalParam);
		
		// Enrich the table with dataset type, entity group and table of scenarios - used by appropriate filters
		if (tSimDef.getColNum("APM Dataset Type ID") <= 0) {
			tSimDef.addCol("APM Dataset Type ID", COL_TYPE_ENUM.COL_INT);
			tSimDef.setInt("APM Dataset Type ID", 1, tAPMArgumentTable.getInt("dataset_type_id", 1));
		}
		
		if (tSimDef.getColNum("APM Scenario IDs") <= 0) {
			tSimDef.addCol("APM Scenario IDs", COL_TYPE_ENUM.COL_TABLE);
			tSimDef.setTable("APM Scenario IDs", 1, tAPMArgumentTable.getTable("Scenario_List", 1).copyTable());
		}

		if (tSimDef.getColNum("APM Run Mode") <= 0) {
			tSimDef.addCol("APM Run Mode", COL_TYPE_ENUM.COL_INT);
			tSimDef.setInt("APM Run Mode", 1, iMode);
		}
		
		// INCREMENTAL_DW ENHANCEMENT 
		// ADD service name and mode to the argt so that UDSR can have more context
		// Use case is to extract data to an incremental table
		// the service name
		if ( tSimDef.getColNum( "apm_service_name") <= 0 )
		{
			tSimDef.addCol( "apm_service_name", COL_TYPE_ENUM.COL_STRING);
			tSimDef.setColTitle( "apm_service_name", "APM Service Name");
			tSimDef.setString( "apm_service_name", 1, tAPMArgumentTable.getString("service_name", 1));
		}

		// simplified mode true/false are we running in batch mode
		if ( tSimDef.getColNum( "apm_running_as_batch") <= 0 )
		{
			tSimDef.addCol( "apm_running_as_batch", COL_TYPE_ENUM.COL_INT);
			tSimDef.setColTitle( "apm_running_as_batch", "APM Running in Batch Mode");
			if ( iMode == m_APMUtils.cModeBatch )
				tSimDef.setInt( "apm_running_as_batch", 1, 1); // only set to true if we are running in batch mode
		}		
		Table mainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
		// ops service table so that in incremental mode we know exactly what we are running against - version number is useful
		if ( iMode != m_APMUtils.cModeBatch && mainArgt.getColNum("op_services_log") > 0 && tSimDef.getColNum( "op_services_log_detail") <= 0 )
		{
			Table opServicesLogDetail = mainArgt.getTable("op_services_log",  1);
			if ( opServicesLogDetail != Util.NULL_TABLE) {				
				tSimDef.addCol( "op_services_log_detail", COL_TYPE_ENUM.COL_TABLE);
				tSimDef.setTable( "op_services_log_detail", 1, opServicesLogDetail.copyTable());
			}
		}
		
		// END INCREMENTAL_DW ENHANCEMENT

		return iRetVal;
	}

	int APM_RunSimulation(int iMode, int iQueryId, int entityGroupId, Table tJobScenarios, Table tAPMArgumentTable, Table tRevalParam, String sSimName, String sJobName, Table argt, Table tBatchFailures, Table revalPostreturnt) throws OException {
		final Calendar startTimestamp = Calendar.getInstance();

		Table tResults = Util.NULL_TABLE;
		Table tOldRevalParam;
		int iRetVal = 1;
		String sProcessingMessage;
		String sSavedSimName;
		boolean export_sim_defs;
		boolean succeeded = true;
		
		String sServiceName = tAPMArgumentTable.getString("service_name", 1);
		IApmStatisticsLogger simulationLogger = m_APMUtils.newLogger(Scope.SIMULATION, sServiceName);
		simulationLogger.start();
		
		//Put the entityGroup statistics into the simulation logs
		Table tAllStatistics = tAPMArgumentTable.getTable("Statistics", 1);
		int row = tAllStatistics.unsortedFindString("Scope", "EntityGroup", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		if (row > 0) {
		   Table tEntityGroupStatistics = tAllStatistics.getTable("Scope_Statistics", row);
		   Map<String,Object> entityGroupContexts = m_APMUtils.convertStatisticsTableToMap(tEntityGroupStatistics);
		   simulationLogger.setAll(entityGroupContexts);
		}
		
		simulationLogger.setContext("simulationName", sSimName);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
		String scriptStartTimestamp = dateFormat.format(startTimestamp.getTime());

		simulationLogger.setMetric("simulationStartTime", scriptStartTimestamp);

		{
			Table scenariosTable = tAPMArgumentTable.getTable("Scenario_List", 1);

			StringBuilder scenariosInfo = new StringBuilder();

			int numberOfRows = scenariosTable.getNumRows();
			for (int rowNumber = 1; rowNumber <= numberOfRows; rowNumber++) {
				String scenarioName = scenariosTable.getString("scenario_name", rowNumber);

				scenariosInfo.append(scenarioName);
				if (rowNumber < numberOfRows) {
					scenariosInfo.append(",");
				}
			}
			simulationLogger.setContext("scenarios", scenariosInfo.toString());
		}

		{
			Table packageDetails = tAPMArgumentTable.getTable("Package Details", 1);

			StringBuilder packageInfo = new StringBuilder();

			int numberOfRows = packageDetails.getNumRows();
			for (int rowNumber = 1; rowNumber <= numberOfRows; rowNumber++) {
				String packageName = packageDetails.getString("package_name", rowNumber);

				packageInfo.append(packageName);
				if (rowNumber < numberOfRows) {
					packageInfo.append(",");
				}
			}
			simulationLogger.setContext("packages", packageInfo.toString());
		}

		boolean runRevalPostLocally = true;
		APM_ServiceJobs serviceJobs = new APM_ServiceJobs();
		if (iMode == m_APMUtils.cModeBatch) {
			if (serviceJobs.APM_GetNumRevalServiceEngines(tAPMArgumentTable, sServiceName) > 0)
				runRevalPostLocally = false;
		}

		if (!m_APMUtils.skipSimulationForUpdates(iMode, tAPMArgumentTable)) {
			// get package settings
			export_sim_defs = m_APMUtils.saveSimDefAndQuery(tAPMArgumentTable);

			sProcessingMessage = "Starting to run Simulation for " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);
			m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, sJobName, "", entityGroupId, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);

			iRetVal = APM_SetupRevalParam(iMode, tAPMArgumentTable, sJobName, iQueryId, entityGroupId, tRevalParam);

			// export the sim defn if it has been requested
			if (export_sim_defs && iMode == m_APMUtils.cModeBatch) {
				sSavedSimName = tAPMArgumentTable.getString("service_name", 1) + " " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);
				if (Str.len(sSavedSimName) > 32) //Math.max size of name is 32 char - otherwise we get an error
					sSavedSimName = Str.substr(sSavedSimName, 0, 32);

				tRevalParam.getTable("SimulationDef", 1).setString("name", 1, sSavedSimName);
				Sim.saveSimulation(tRevalParam.getTable("SimulationDef", 1), sSavedSimName, 1, 0, 1);
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Saved simulation to " + sSavedSimName);
			}

			// Set the reval param table to argt table
			if (argt.getColNum("RevalParam") < 1)
				argt.addCol("RevalParam", COL_TYPE_ENUM.COL_TABLE);
			tOldRevalParam = argt.getTable("RevalParam", 1);
			if (Table.isTableValid(tOldRevalParam) != 0)
				tOldRevalParam.destroy();
			argt.setTable("RevalParam", 1, tRevalParam.copyTable());

			tResults = Util.NULL_TABLE;
			try {
				tResults = Sim.runRevalByParamFixed(argt);
			} catch (Exception t) {
				tResults = Util.NULL_TABLE;
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception while calling sim run: " + t);
				String message = m_APMUtils.getStackTrace(t);
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, message);
				iRetVal = 0;

				// now we need to populate the errors correctly
				revalPostreturnt.setString("error_message", 1, "Exception whilst running simulation");
				revalPostreturnt.setInt("ret_val", 1, 0);

				// if block update check what failed
				if (iMode == m_APMUtils.cModeBlockUpdate) {
					Table blockFails = tAPMArgumentTable.getTable("Filtered Entity Info", 1).copyTable(); // use the filtered one (inside pfolio loop)
					blockFails.setColValInt("log_status", OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt());
					revalPostreturnt.setTable("Block Update Failures", 1, blockFails);
					blockFails = Util.NULL_TABLE;
				}
				
				succeeded = false;
			}
		}

		// set it here as the we will get succeeded if only 1 key out of many failed
		if (iMode == m_APMUtils.cModeBatch)
			revalPostreturnt.setTable("Batch Failures", 1, tBatchFailures.copyTable());

		if (tAPMArgumentTable.getColNum("simulation_results") < 1)
			tAPMArgumentTable.addCol("simulation_results", COL_TYPE_ENUM.COL_TABLE);

		if (iRetVal == 1) {
			if (runRevalPostLocally) {
				tAPMArgumentTable.setTable("simulation_results", 1, tResults);

				try {
					APM_RevalPost_Impl revalPost = new APM_RevalPost_Impl();
					iRetVal = revalPost.execute(argt, revalPostreturnt);
				} catch (Exception t) {
					iRetVal = 0;
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception while calling APM_RevalPost: " + t);
					String message = m_APMUtils.getStackTrace(t);
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message + "\n");
					succeeded = false;
				}

				ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "APM_JobProcessor");
				if (iRetVal == 0) {
					String sErrMessage = "Failed to call APM_RevalPost for " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);
					m_APMUtils.APM_PrintAndLogErrorMessageSafe(iMode, tAPMArgumentTable, sErrMessage);
					succeeded = false;
				}
			} else
				revalPostreturnt.setInt("ret_val", 1, 1);
		}

		if (Table.isTableValid(tResults) != 0)
			tResults.destroy();

		tAPMArgumentTable.setTable("simulation_results", 1, Util.NULL_TABLE);

		if(succeeded) {
		    simulationLogger.stop();
		} else {
		    simulationLogger.abort();
		}

		m_APMUtils.closeLogger(simulationLogger, tAPMArgumentTable);

		return iRetVal;
	}

     boolean PortfolioAccessAllowed(int portfolioId) throws OException {
        final int UNLIMITED_PORTFOLIO_ACCESS = 696;
        Table accessiblePortfolios = Table.tableNew();

        // Does the user have access to all portfolios?
        if (Util.userCanAccess(UNLIMITED_PORTFOLIO_ACCESS) != 0) {
           Ref.loadFromRef(accessiblePortfolios, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
        } else {
           Ref.loadFromRef(accessiblePortfolios, SHM_USR_TABLES_ENUM.PERS_PFOLIO_TABLE);
        }

        boolean allowed = (accessiblePortfolios.unsortedFindInt(1, portfolioId) > 0);

        accessiblePortfolios.destroy();

        return allowed;
     }
}
