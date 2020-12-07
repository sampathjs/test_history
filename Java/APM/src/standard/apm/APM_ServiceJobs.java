/* Released with version 05-Feb-2020_V17_0_126 of APM */

package standard.apm;

import standard.include.APM_Utils;
import standard.include.APM_Utils.EntityType;
import standard.include.ConsoleLogging;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.fnd.ServicesBase;

public class APM_ServiceJobs
{
	private APM_Utils m_APMUtils;
	
   public APM_ServiceJobs() {
		m_APMUtils = new APM_Utils();
   }

	public Table APM_BuildSplitRequests(Table tAPMArgumentTable, int iMode, Table tSplitInfo) throws OException {
		int iRetVal = 1;
		int iRow;
		Table tScriptArgt;
		Table tSplitRequests = Util.NULL_TABLE;
		Table tParams;
		XString err_xstring;

		if (tSplitInfo.getNumRows() < 1)
			return Util.NULL_TABLE;

		err_xstring = Str.xstringNew();

		// create the request table via perform operation
		tParams = Table.tableNew();
		iRetVal = Apm.performOperation(m_APMUtils.APM_CREATE_SPLIT_TABLE, 1, tParams, err_xstring);

		if (iRetVal != 0) {
			tSplitRequests = tParams.getTable(2, 1).copyTable();
			tSplitRequests.addNumRows(tSplitInfo.getNumRows());

			for (iRow = 1; iRow <= tSplitRequests.getNumRows(); iRow++) {
				tScriptArgt = tSplitInfo.cloneTable();
				tScriptArgt.addRow();
				tSplitInfo.copyRow(iRow, tScriptArgt, 1); // @@ leak check ??
				// Required job name will be shown on a grid monitor and request log
				tSplitRequests.setString("job_name", iRow, "APM_JobProcessor_Job_" + iRow); 
				tSplitRequests.setString("script_name", iRow, "APM_JobProcessor");/* Required */
				tSplitRequests.setTable("argt", iRow, tScriptArgt);/* Required */
				// optional sequence number
				tSplitRequests.setInt("seq_num", iRow, 0);
				// optional delay between the jobs
				tSplitRequests.setInt("delay_factor", iRow, 0); 
				// optional - tells the scheduler what to do next in case of engine or job - number of retries
				tSplitRequests.setInt("what_next", iRow, 0);
			}

			if ( tSplitRequests.getColNum("computation_date") > 0)
			{
				// set the current date as the computation date automatically - this means we are consistent with the revalservice
				tSplitRequests.setColValInt("computation_date", OCalendar.today() );
			}			
		}

		if (Table.isTableValid(tParams) != 0)
			tParams.destroy();

		Str.xstringDestroy(err_xstring);

		if ((Table.isTableValid(tSplitRequests) == 0) || (iRetVal == 0))
			return Util.NULL_TABLE;
		else
			return tSplitRequests;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_IssueSplitRequests
	Description:   Send the split request to the appropriate processing mechanism (cluster/grid/local script)
	           collates the results into a result table

	Return Values: 
	-------------------------------------------------------------------------------*/
	public int APM_IssueSplitRequests(Table tAPMArgumentTable, String sServiceName, int iMode, Table tSplitInfo, Table tResults) throws OException {
		int iRetVal = 1;
		int iRow;
		Table tOperationtParams = Util.NULL_TABLE;
		Table tParams = Util.NULL_TABLE;
		Table tMethodReturnt = Util.NULL_TABLE;
		Table tSplitRequests;
		Table tJobRequest;
		Table tJobResult;
		Table tIssueSplitParam = Util.NULL_TABLE;
		Table tTempCleanup;
		XString err_xstring;

		// if no requests then return, not an error as empty entity groups are
		// valid
		if (tSplitInfo.getNumRows() < 1)
			return 1;

		err_xstring = Str.xstringNew();

		// add the result table cols
		m_APMUtils.APM_setupJobReturnt(tResults);

		// create the split request argument table
		tSplitRequests = APM_BuildSplitRequests(tAPMArgumentTable, iMode, tSplitInfo);
		if (Table.isTableValid(tSplitRequests) == 0) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to build split requests");
			iRetVal = 0;
		}

		// create the request table via perform operation
		if (iRetVal != 0) {
			tOperationtParams = Table.tableNew();
			iRetVal = Apm.performOperation(m_APMUtils.APM_CREATE_METHOD_PARAM_TABLE, 1, tOperationtParams, err_xstring);
			if (iRetVal == 0) {
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to get method param table for split request : " + Str.xstringGetString(err_xstring) + "\n");
			}
			if (iRetVal != 0) {
				tParams = tOperationtParams.getTable(2, 1).copyTable();
			}
		}

		if (iRetVal != 0) {
			// fill in parameters for service method
			// Set to 1 if split process will run simulation. This will make sure that same index data is used by each engine
			tParams.setInt("mktd_cache", 1, 1); 
			tParams.setTable("method_params", 1, tSplitRequests);

			// It is important to set the following flag. If not set then split cluster engines
			// will not take on a dispatcher's role when submitting to RevalService and a separate screng/cluster
			// will be used for that purpose.
			tParams.setInt( "act_as_dispatcher", 1, 1);

			// doesn't mean much for SplitProcess method since each engine will set its returnt into request table
			tMethodReturnt = Table.tableNew(); 

			// get the parameters for the issue
			tIssueSplitParam = Table.tableNew("Split Method Param");
			iRetVal = Apm.performOperation(m_APMUtils.APM_ISSUE_SPLIT_REQUEST, 1, tIssueSplitParam, err_xstring);
			if (iRetVal == 0) {
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to get parameters to issue split request : " + Str.xstringGetString(err_xstring) + "\n");
			}

			// fill in tParams and issue request
			if (iRetVal != 0) {

				tIssueSplitParam.getTable(2, 1).setString(1, 1, sServiceName);
				tIssueSplitParam.getTable(2, 2).setString(1, 1, "SplitProcess");
				tIssueSplitParam.getTable(2, 3).setTable(1, 1, tParams);
				tIssueSplitParam.getTable(2, 4).setTable(1, 1, tMethodReturnt);
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Sending " + tSplitRequests.getNumRows() + " entity group job requests to SplitProcess cluster");
				// NB Services.serviceRunMethod() used by tfe_interface_api
				// doesn't fill in an error String
				int iRetValForSplitProcess = Apm.performOperation(m_APMUtils.APM_ISSUE_SPLIT_REQUEST, 0, tIssueSplitParam, err_xstring);
				if (iRetValForSplitProcess == 0) {
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "One or more entity group level jobs dispatched to the SplitProcess cluster have failed !");
				}
			}
		}

		// copy the tResults from each job into the passed tResults table
		// NB. We fill this in even if the request failed to be issued so we
		// can handle the failed job(s) later
		tResults.clearRows();
		for (iRow = 1; iRow <= tSplitRequests.getNumRows(); iRow++) {
			tJobRequest = tSplitRequests.getTable("argt", iRow);
			tJobResult = tSplitRequests.getTable("returnt", iRow);
			// need to be careful as if a job crashed or the request failed
			// to be issued there might not be a table here in each row
			if (Table.isTableValid(tJobResult) != 0) {
				tJobResult.copyRowAdd(1, tResults);
			} else {
				// job failed to fill in return table (engine probably
				// crashed or machine died etc..)
				// get the job details from the original request table and
				// fill in error
				int resultsRow = tResults.addRow();
				tJobRequest = tSplitRequests.getTable("argt", iRow);
				tResults.setInt("job_num", resultsRow, tJobRequest.getInt("job_num", 1));
				tResults.setString("job_name", resultsRow, tJobRequest.getString("job_name", 1));
				tResults.setInt("entity_group_id", iRow, tJobRequest.getInt("entity_group_id", 1));				
				tResults.setInt("ret_val", resultsRow, 0);
				tResults.setString("error_message", resultsRow, "JOB DIED");
			}
		}

		if (Table.isTableValid(tOperationtParams) != 0) {
			tOperationtParams.setTable(2, 1, Util.NULL_TABLE);
			tOperationtParams.destroy();
		}

		if (Table.isTableValid(tIssueSplitParam) != 0) {
			// tfe_interface_api owns these sub-tables, don't kill
			tIssueSplitParam.getTable(2, 3).setTable(1, 1, Util.NULL_TABLE);
			tIssueSplitParam.getTable(2, 4).setTable(1, 1, Util.NULL_TABLE);
			tIssueSplitParam.destroy();
		}

		if (Table.isTableValid(tParams) != 0) {
			tParams.setTable("method_params", 1, Util.NULL_TABLE);
			tParams.destroy();
		}

		// kill the original split parameters
		for (iRow = 1; iRow <= tSplitRequests.getNumRows(); iRow++) {
			// job results now has ownership so NULL returnt before killing
			// parent table
			tSplitRequests.setTable("returnt", iRow, Util.NULL_TABLE);
			tTempCleanup = tSplitRequests.getTable("argt", iRow);
			if (Table.isTableValid(tTempCleanup) != 0) {
				// don't kill shared arg table
				tTempCleanup.setTable("job_arg_table", 1, Util.NULL_TABLE);
			}
		}
		tSplitRequests.destroy();
		tMethodReturnt.destroy();
		Str.xstringDestroy(err_xstring);

		return iRetVal;
	}
	
	public int APM_ProcessRequestLocally(Table tAPMArgumentTable, int iMode, Table tLocalJobs, int jobRow, Table tResults, Table argt) throws OException {
		int iRetVal = 1;
		int iRow = 0;
		Table tJobResult = Util.NULL_TABLE;
		// make sure that we process empty entity groups, otherwise we cannot clean
		// data changed when APM Service is not running
		// if (tSplitInfo.getNumRows()<1) return 1;

		// add the result table columns
		m_APMUtils.APM_setupJobReturnt(tResults);

		Table tSplitInfo = tLocalJobs.cloneTable();
		tLocalJobs.copyRowAdd(jobRow, tSplitInfo);
		
		// run the subscript, save the script name as the processor will rename it
		String script_name = m_APMUtils.find_script_path("APM_JobProcessor");

		try
		{
			iRetVal = Util.runScript(script_name, tSplitInfo, Util.NULL_TABLE);
		}
		catch(Exception t)
		{
			iRetVal = 0;
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception while calling APM_JobProcessor: " + t);
			String message = m_APMUtils.getStackTrace(t);
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message+ "\n");
		}
		
		ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "APM_ServiceJobs");
		if (iRetVal == 0) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Failed to run job processor script locally");
		}

		if (iRetVal != 0) {
			// only one result to fill in here
			tJobResult = tSplitInfo.getTable("returnt", 1);
			if (Table.isTableValid(tJobResult) != 0) {
				tResults.setInt("job_num", 1, tJobResult.getInt("job_num", 1));
				tResults.setString("job_name", 1, tJobResult.getString("job_name", 1));
				tResults.setInt("entity_group_id", 1, tJobResult.getInt("entity_group_id", 1));
				tResults.setInt("ret_val", 1, tJobResult.getInt("ret_val", 1));
				tResults.setString("error_message", 1, tJobResult.getString("error_message", 1));

				if ( iMode == m_APMUtils.cModeBlockUpdate && Table.isTableValid(tJobResult.getTable("Block Update Failures", 1)) != 0 ) {
				   tResults.setTable("Block Update Failures", 1, tJobResult.getTable("Block Update Failures", 1).copyTable());
					
					Table argtDealInfo = argt.getTable("Deal Info", 1);
					Table blockFailures = tJobResult.getTable("Block Update Failures", 1);
					for (int i = 1; i <= blockFailures.getNumRows(); i++) {
						int argtRow = argtDealInfo.unsortedFindInt("tran_num", blockFailures.getInt("tran_num", i)); 
						argtDealInfo.setInt("log_status", argtRow, OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt());
					}
				}

				if ( iMode == m_APMUtils.cModeBatch && Table.isTableValid(tJobResult.getTable("Batch Failures", 1)) != 0 )
				   tResults.setTable("Batch Failures", 1, tJobResult.getTable("Batch Failures", 1).copyTable());
			} else {
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to retrieve tResults from locally run job processor script");
				// job failed to fill in return table (screng probably crashed
				// or machine died etc..)
				// get the job details from the original request table and fill
				// in error
				tResults.setInt("job_num", iRow, tSplitInfo.getInt("job_num", 1));
				tResults.setString("job_name", iRow, tSplitInfo.getString("job_name", 1));
				tResults.setInt("entity_group_id", iRow, tSplitInfo.getInt("entity_group_id", 1));
				tResults.setInt("ret_val", iRow, 0);
				tResults.setString("error_message", 1, "JOB DIED");
				iRetVal = 0;
				
				// no need to worry about setting the block update failures as they will already be set in the argt if run locally
			}
		}

		// blank this so it doesn't get killed again when we clear the split info rows later
		tSplitInfo.setTable("returnt", 1, Util.NULL_TABLE);
		tLocalJobs.setTable("returnt", jobRow, Util.NULL_TABLE);
		
		if (Table.isTableValid(tSplitInfo) != 0)
			tSplitInfo.destroy();
		
		return iRetVal;
	}

	public int APM_CheckJobResults(int iMode, Table tAPMArgumentTable, Table tJobResults) throws OException {
		int iRow;
		int iJobNum;
		int iStatus;
		int entityGroupId;
		int iRetVal = 1;
		String sJobName;
		String sMessage;
		String sLogMessage;
		Table blockFails;
		Table batchFails;

		Table tArgtBatchFails = tAPMArgumentTable.getTable( "Batch Failures",1);
		tArgtBatchFails.clearRows();
		
		for (iRow = 1; iRow <= tJobResults.getNumRows(); iRow++) {
			iJobNum = tJobResults.getInt("job_num", iRow);
			sJobName = tJobResults.getString("job_name", iRow);
			entityGroupId = tJobResults.getInt("entity_group_id", iRow);
			sMessage = tJobResults.getString("error_message", iRow);
			iStatus = tJobResults.getInt("ret_val", iRow);
			blockFails = tJobResults.getTable("Block Update Failures", iRow);
			batchFails = tJobResults.getTable("Batch Failures", iRow);

			// we might have an overall success status but it doesn;t mean that a key didn't faIL
			if ( batchFails != Util.NULL_TABLE && Table.isTableValid(batchFails) != 0)
			{
				tArgtBatchFails.select(batchFails, "*", "entity_group_id GT 0");
				batchFails.destroy();
				tJobResults.setTable("Batch Failures", iRow, Util.NULL_TABLE);
			}
			
			ConsoleLogging.instance().setJobContext(tAPMArgumentTable, sJobName);
			String entityGroupName = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);
			ConsoleLogging.instance().setEntityGroupContext(tAPMArgumentTable, entityGroupName, entityGroupId);
			
			if (iStatus != 0) {
				sLogMessage = "Job #" + iJobNum + " (" + sJobName + ") Ok : " + sMessage;
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sLogMessage);
			} else {
				sLogMessage = "Job #" + iJobNum + " (" + sJobName + ") !! FAILED !! : " + sMessage;
				m_APMUtils.APM_PrintAndLogErrorMessageSafe(iMode, tAPMArgumentTable, sLogMessage);
				iRetVal = 0;
				
				Table tMainArgt = tAPMArgumentTable.getTable( "Main Argt",1);

				// now update the argt here with any failures from block updates to make sure that
				// the log monitor picks them up (only will be set if the gird is used with a custom split for incrementals)
				if ( blockFails != Util.NULL_TABLE && Table.isTableValid(blockFails) != 0)
				{
					// IN THIS INSTANCE CHANGE THE ARGT entity INFO !! - we don't care about old versions in the argt so no need to change the select
					APM_EntityJobOps.instance().SetBlockUpdateStatuses(tAPMArgumentTable, tMainArgt, blockFails);
					blockFails.destroy();
					tJobResults.setTable("Block Update Failures", iRow, Util.NULL_TABLE);
				}
				
				if ( iRetVal == 0)
					m_APMUtils.SaveArgtForRerunIfSweeperOn(iMode, tMainArgt, tAPMArgumentTable, entityGroupId);				
			}

			ConsoleLogging.instance().unSetJobContext(tAPMArgumentTable);
			ConsoleLogging.instance().unSetEntityGroupContext(tAPMArgumentTable);
		}
		return iRetVal;
	}
   
	public int APM_CreateJobTable(Table tJobTable) throws OException {
		tJobTable.addCol("job_num", COL_TYPE_ENUM.COL_INT);
		tJobTable.addCol("job_name", COL_TYPE_ENUM.COL_STRING);
		tJobTable.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
		tJobTable.addCol("Running Locally", COL_TYPE_ENUM.COL_INT);
		tJobTable.addCol("job_arg_table", COL_TYPE_ENUM.COL_TABLE); 
		tJobTable.addCol("returnt", COL_TYPE_ENUM.COL_TABLE); 
		tJobTable.addCol("simulation_name", COL_TYPE_ENUM.COL_STRING);
		tJobTable.addCol("scenario_list", COL_TYPE_ENUM.COL_TABLE);
		return 1;
	}

	public int APM_ClearJobTableRows(int iMode, Table tJobTable) throws OException {
		int iRow;

		for (iRow = 1; iRow <= tJobTable.getNumRows(); iRow++) {
			// null the common job processor arg table so it doesn't get
			// destroyed during the row clear
			tJobTable.setTable("job_arg_table", iRow, Util.NULL_TABLE);
			tJobTable.setTable("scenario_list", iRow, Util.NULL_TABLE);
		}

		tJobTable.clearRows();

		return 1;
	}
   
	public int APM_CreateJobs(int iMode, Table tSplitProcessJobs, Table tLocalJobs, Table tAPMArgumentTable, String sServiceName, Table argt) throws OException {

		int iRetVal = 1;
		
		// run all jobs locally unless there is a cluster specified on the SplitProcess method for batches
		int runAllJobsLocallyFlag = 1; 
		if (iMode == m_APMUtils.cModeBatch) {
			int numEnginesAvailable = APM_GetNumSplitProcessEngines(tAPMArgumentTable, sServiceName);
			if ( numEnginesAvailable > 0 )
			{
				runAllJobsLocallyFlag = 0; // run jobs on SplitProcess cluster assuming they have more than 1 entity in a group (portfolio or service provider)
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "SplitProcess cluster available. Splitting by group (portfolio or pipeline) onto SplitProcess cluster ON. Number of engines = " + numEnginesAvailable);				
			}
			else
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "SplitProcess cluster not available. Running all jobs locally");				
		}

		if (iMode == m_APMUtils.cModeBatch) {
			int numEnginesAvailable = APM_GetNumRevalServiceEngines(tAPMArgumentTable, sServiceName);
			if ( numEnginesAvailable > 0 )
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "RevalService cluster available. Splitting within each entity group (portfolio) ON.  Number of engines (portfolio job tranches) = " + numEnginesAvailable);				
			else if ( m_APMUtils.GetCurrentEntityType(tAPMArgumentTable) == EntityType.DEAL )
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "RevalService cluster not available. Not splitting within entity group (portfolio).");				
		}
		
		// now work out how many jobs (1 per entity group) are required
		int numJobs = 0;
		Table tEntityGroups = tAPMArgumentTable.getTable("Selected Entity Groups", 1);
		numJobs = tEntityGroups.getNumRows();
		m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Number of " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup()  + " jobs: " + numJobs);
		if ( numJobs < 1)
		{
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Cannot get number of splits - no groups in selected groups table");
			iRetVal = 0;
		}			
		
		if ( iRetVal == 1 ) 
		{	
			Table tScenarioList = tAPMArgumentTable.getTable("Scenario_List", 1);

			for (int iJob = 1; iJob <= numJobs; iJob++) 
			{
				// work out whether this entity group should be run locally (if no entities in it)
				int entityGroupId = tEntityGroups.getInt(1, iJob);
				
				String entityGroupName = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);
				ConsoleLogging.instance().setEntityGroupContext(tAPMArgumentTable, entityGroupName, entityGroupId);
								
				// populate the appropriate job table
				if ( runAllJobsLocallyFlag == 1 )
				{
					int iRow = tLocalJobs.addRow();
					tLocalJobs.setInt("entity_group_id", iRow, entityGroupId);
					tLocalJobs.setInt("job_num", iRow, iJob);
					tLocalJobs.setInt("Running Locally", iRow, runAllJobsLocallyFlag);
					tLocalJobs.setString("job_name", iRow, entityGroupId + "_" + iJob + "_" + numJobs);
					tLocalJobs.setString("simulation_name", iRow, tAPMArgumentTable.getString("Simulation Name", 1));	
	
					// if we're running locally create a return table for the script to fill in
					Table returnTable = Table.tableNew("returnt");
					m_APMUtils.APM_setupJobReturnt(returnTable);
					returnTable.addRow();
					returnTable.setInt("job_num", 1, iJob);
					returnTable.setString("job_name", 1, tLocalJobs.getString("job_name", iRow) );
					returnTable.setInt("entity_group_id", 1, entityGroupId);
					tLocalJobs.setTable("returnt", iRow, returnTable);

					// before filtering by pfolio
					tLocalJobs.setTable("scenario_list", iRow, tScenarioList.copyTable());
	
					// put in common args - don't want to duplicate a table ref
					Table tempArgt = tAPMArgumentTable.copyTable();
					
					//updates will always run locally
					if ( iMode != m_APMUtils.cModeBatch)
					{
						if ( m_APMUtils.GetCurrentEntityType(tAPMArgumentTable) == EntityType.DEAL )
						   iRetVal = APM_FilterEntityInfoForCurrentEntityGrouping(iMode, tempArgt, entityGroupId, "internal_portfolio");
						else if ( m_APMUtils.GetCurrentEntityType(tAPMArgumentTable) == EntityType.NOMINATION )
						   iRetVal = APM_FilterEntityInfoForCurrentEntityGrouping(iMode, tempArgt, entityGroupId, "pipeline_id");

						if ( iRetVal == 1 )
						{
						   int pfolioQueryID = APM_EntityJobOps.instance().APM_CreateQueryIDForEntityGroup(iMode, tempArgt, entityGroupId);
						   iRetVal = APM_EntityJobOps.instance().APM_EnrichEntityTable(iMode, tempArgt, pfolioQueryID);
						   
						   if ( iRetVal == 1 )
						   {
							  int iJobMode = APM_EntityJobOps.instance().APM_AdjustLaunchType(iMode, tempArgt, entityGroupId);					
							  tempArgt.setInt("Script Run Mode", 1, iJobMode);
						   }
						
						   pfolioQueryID = APM_EntityJobOps.instance().RemoveBackoutsFromQueryID(iMode, tempArgt, entityGroupId, pfolioQueryID);
						} 
					}
					
					tLocalJobs.setTable("job_arg_table", iRow, tempArgt);
				}
				else 
				{
					// running on SplitProcess
					int iRow = tSplitProcessJobs.addRow();
					tSplitProcessJobs.setInt("entity_group_id", iRow, entityGroupId);
					tSplitProcessJobs.setInt("job_num", iRow, iJob);
					tSplitProcessJobs.setInt("Running Locally", iRow, runAllJobsLocallyFlag);
					tSplitProcessJobs.setString("job_name", iRow, entityGroupId + "_" + iJob + "_" + numJobs);
					tSplitProcessJobs.setString("simulation_name", iRow, tAPMArgumentTable.getString("Simulation Name", 1));	
	
					// before filtering by pfolio
					tSplitProcessJobs.setTable("scenario_list", iRow, tScenarioList.copyTable());
	
					// put in common args - don't want to duplicate a table ref
					Table tempArgt = tAPMArgumentTable.copyTable();
					tSplitProcessJobs.setTable("job_arg_table", iRow, tempArgt);
				}
				
				// if there is a failure quit
				if ( iRetVal == 0 )
					break;
			}				
		}

		ConsoleLogging.instance().unSetEntityGroupContext(tAPMArgumentTable);
				
		return iRetVal;
	}
		
	public int APM_GetNumRevalServiceEngines(Table tAPMArgumentTable, String sServiceName) throws OException {

		if ( m_APMUtils.GetCurrentEntityType(tAPMArgumentTable) == EntityType.DEAL )
			return APM_GetNumEnginesForServiceMethod(tAPMArgumentTable, sServiceName, "RevalService");
		else 
			return 0;
	}
	
	public int APM_GetNumSplitProcessEngines(Table tAPMArgumentTable, String sServiceName) throws OException {

		return APM_GetNumEnginesForServiceMethod(tAPMArgumentTable, sServiceName, "SplitProcess");
	}

	private int APM_GetNumEnginesForServiceMethod(Table tAPMArgumentTable, String sServiceName, String sMethodName) throws OException {
		int iRetVal = 1, iDebugLevel = 0, iDebugFlag;
		Table tParams;
		XString err_xstring;
		int numEngines = -1;

		// first determine if the method even exists
		// revalservice method will not exist on old services 
		Table pfield_table = ServicesBase.getServiceMethodProperties(sServiceName, sMethodName);
		if ( pfield_table == Util.NULL_TABLE || Table.isTableValid(pfield_table) == 0 )
		{
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "'" + sMethodName + "' method not present for service: '" + sServiceName + "'");
			return 0;
		}
		
		err_xstring = Str.xstringNew();

		// get the params
		tParams = Table.tableNew();
		// error String not used here as error means using older
		// tfe_interface_api filled in her
		iRetVal = Apm.performOperation(m_APMUtils.APM_GET_NUM_SPLIT_ENGINES, 1, tParams, err_xstring);

		// call the operation to get the number of engines
		if (Table.isTableValid(tParams) == 1 && (iRetVal == 1)) {
			iDebugFlag = tAPMArgumentTable.getInt("Debug", 1);

			/* ==== set the params ===== */
			// row 1 is service name
			tParams.getTable(2, 1).setString(1, 1, sServiceName); 

			// row 4 is method name
			tParams.getTable(2, 4).setString(1, 1, sMethodName); 
			
			/* ==== execute ===== */
			if (iDebugFlag != 0) {
				iDebugLevel = Debug.getDEBUG();
				Debug.setDEBUG(DEBUG_LEVEL_ENUM.DEBUG_LOW);
			}

			// to keep backward compatibility failure here is not an error as
			// could be older tfe_interface_api
			iRetVal = Apm.performOperation(m_APMUtils.APM_GET_NUM_SPLIT_ENGINES, 0, tParams, err_xstring);

			if (iDebugFlag != 0)
				Debug.setDEBUG(DEBUG_LEVEL_ENUM.fromInt(iDebugLevel));
		}

		if (iRetVal != 0) {
			numEngines = tParams.getTable(2, 2).getInt(1, 1);
		} else {
			// operation failed or operation does not exist (per-v80), either
			// way no engines
			numEngines = 0;
		}

		Str.xstringDestroy(err_xstring);

		tParams.destroy();
		return numEngines;
	}

	private int APM_FilterEntityInfoForCurrentEntityGrouping(int mode, Table tAPMArgumentTable, int currentEntityGroupID, String entityGroupColName) throws OException {
	   // Filters the entity table so that only entities for current grouping ID (pfolio or pipeline) are passed down

	   int entityRow;

	   if (mode == m_APMUtils.cModeBatch)
	      return 1;

	   // get rid of the previous entity groups entity info & query ID
	   APM_DestroyFilteredEntityInfoForJob(mode, tAPMArgumentTable);

	   Table globalFilteredEntities = tAPMArgumentTable.getTable("Global Filtered Entity Info", 1).copyTable();

	   // now filter the entity info for the current entity group
	   for (entityRow = globalFilteredEntities.getNumRows(); entityRow >= 1; entityRow--)
	   {
	   	   if ( globalFilteredEntities.getInt(entityGroupColName, entityRow) != currentEntityGroupID )
	   	   	   	   globalFilteredEntities.delRow(entityRow);
	   }

	   // replace in the argument table
	   tAPMArgumentTable.setTable("Filtered Entity Info", 1, globalFilteredEntities);

	   return 1;
    }

	private int APM_DestroyFilteredEntityInfoForJob(int mode, Table tAPMArgumentTable) throws OException {

	   Table jobFilteredEntities = tAPMArgumentTable.getTable("Filtered Entity Info", 1);

	   // now delete the "Filtered Entity Info" if it already exists - it will be replaced
	   if (jobFilteredEntities != Util.NULL_TABLE && Table.isTableValid(jobFilteredEntities) != 0) 
	   {
	      jobFilteredEntities.destroy();
	      tAPMArgumentTable.setTable("Filtered Entity Info", 1, Util.NULL_TABLE);
	   }

	   return 1;
    }
	
}
