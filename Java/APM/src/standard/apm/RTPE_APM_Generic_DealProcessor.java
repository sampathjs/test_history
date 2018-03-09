/* Released with version 09-Jan-2014_V14_0_6 of APM */

/*
Description : This forms part of the Trader Front End, Active Position Manager
              package

-------------------------------------------------------------------------------
Revision No.  Date        Who  Description
-------------------------------------------------------------------------------
1.0.0         
 */
package standard.apm;
import standard.include.APM_Utils;
import standard.include.ConsoleCaptureWrapper;
import standard.include.LogConfigurator;
import standard.include.StatisticsLogging;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.fnd.RefBase;
import com.olf.openjvs.Ref;


public class RTPE_APM_Generic_DealProcessor implements IScript {
	private APM_Utils m_APMUtils;

	boolean tfeSettingForSaveArgt = false;  // TBD - add global tfe_config setting
	
	public RTPE_APM_Generic_DealProcessor(){
		m_APMUtils = new APM_Utils();

	}

	// defines (needed because of changes in Endur versions over time
	int INDEX_DB_STATUS_VALIDATED = 1;

	// APM_PerformOperation - sim parameters: row identifier enum
	int APM_ROW_VERSION_ID   = 1;
	int APM_ROW_SIM_DEF_ID   = 2;
	int APM_ROW_USER_PERMS   = 3;
	int APM_ROW_GROUP_PERMS  = 4;
	int APM_ROW_PUBLIC_PERMS = 5;

	// PUT_CALL_ENUM.PUT/PUT_CALL_ENUM.CALL not supported pre-v70r1 so use these instead
	int PUT_CONST  = 0;
	int CALL_CONST = 1;

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		Table tAPMArgumentTable = Util.NULL_TABLE;
		Table tRevalParam = Table.tableNew("Reval Param");
		Table tBatchFailures = null;
		String sProcessingMessage = " ";
		String sJobName;
		String sErrMessage="";
		int iQueryId = 0;
		int iMode = m_APMUtils.cModeUnknown;
		int iPortfolioId;
		int iRetVal=1;
		int iJobNum;
		QueryRequest qreq = null;
		int runJobLocallyFlag = 1; 
		Table revalPostreturnt;
				
		try {
			/////////////////////////////////////////////////////
			//
			// Get the argument table, extract common data we need
			// then enrich with more job specific data
			//
			/////////////////////////////////////////////////////
			if(argt.getNumRows() >0)
				tAPMArgumentTable = argt.getTable("deal_arg_table",1);
			if(Table.isTableValid(tAPMArgumentTable) == 0)
			{
				OConsole.oprint ("Failed to get deal processor argt table. Exiting ...");
				// This script should not return an error as it will cause a cluster engine job failure and re-run,
				Util.exitSucceed();
			}
			
			// Set the path/log information.		
			String logFilePath = "";	
			if ( Str.isEmpty(Util.getEnv("AB_ERROR_LOGS_PATH"))==1 )
				logFilePath = Util.getEnv("AB_OUTDIR") + "/error_logs/";
			else
				logFilePath = Util.getEnv("AB_ERROR_LOGS_PATH") + "/";
			
			// Set the log path to be used by our logger.
			LogConfigurator.getInstance().setPath( logFilePath );
			LogConfigurator.getInstance().setServiceName( tAPMArgumentTable.getString("service_name", 1) );
			LogConfigurator.getInstance().push( tAPMArgumentTable.getString("Log File", 1 ) );
			tAPMArgumentTable.setString("Log File", 1, LogConfigurator.getInstance().front());
			
			if ( !ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).isOpen() ) {
				// Open the console capture and register this object as its owner.
				ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).open( this );						
			}

			// m_APMUtils.APM_SetMsgContext (tAPMArgumentTable, "SCRIPT", "RTPE_APM_Generic_DealProcessor", -1);
			StatisticsLogging.instance().setScriptContext(tAPMArgumentTable, "RTPE_APM_Generic_DealProcessor");
			// m_APMUtils.APM_SetMsgContext(tAPMArgumentTable, "PID", Str.intToStr(Ref.getProcessId()), Ref.getProcessId());
			StatisticsLogging.instance().setProcessIdContext(tAPMArgumentTable, Ref.getProcessId());
	
			/////////////////////////////////////////////////////
			//
			// Get the split specific arguments
			//
			/////////////////////////////////////////////////////

			// job info
			sJobName = argt.getString("job_name",1);
			iJobNum = argt.getInt("job_num",1);

			StatisticsLogging.instance().setJobContext(tAPMArgumentTable, sJobName);

			// run mode - get this up here so that error logging works correctly
			iMode = tAPMArgumentTable.getInt("Script Run Mode",1);
	
			// portfolio
			iPortfolioId = argt.getInt("portfolio_id",1);
			String portfolioName = Table.formatRefInt(iPortfolioId, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);

			StatisticsLogging.instance().setPortfolioContext(tAPMArgumentTable, portfolioName, iPortfolioId);

			int iTranNum = tAPMArgumentTable.getInt("Current TranNum", 1);
			int iDealNum = tAPMArgumentTable.getInt("Current DealNum", 1);			
			m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeStarted, "", "", iPortfolioId, -1, iTranNum, iDealNum, tAPMArgumentTable, Util.NULL_TABLE, "Starting to process ...");
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "================================================================");
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Starting to process portfolio " + Table.formatRefInt(iPortfolioId, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE) + " ...");
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "================================================================");
			
			runJobLocallyFlag = argt.getInt("Running Locally",1);
			if(runJobLocallyFlag == 1)
				revalPostreturnt = argt.getTable("returnt",1);
			else
				revalPostreturnt = returnt;
			
			// double check the return table is constructed correctly 
			m_APMUtils.APM_setupJobReturnt(revalPostreturnt);

			// make sure the correct job name and number is in there
			 revalPostreturnt.setString("job_name", 1, sJobName);
			 revalPostreturnt.setInt("job_num", 1, iJobNum);
			 revalPostreturnt.setInt("portfolio_id", 1, iPortfolioId);
			
			// set up the rest of the deal processor arguments specific to this job
			//set portfolio number ... "Current Portfolio" is used later in UpdateTables	
			iQueryId = -1;
			int noDealsInPfolioFlag = 0;			
			if (iMode == m_APMUtils.cModeBatch ) {
				Table tMainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
				
				// Call APM batch start to create the dataset information
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Initiating batch start operation for portfolio");
				APM_BatchOps batchOperations = new APM_BatchOps();
				iRetVal = batchOperations.initialiseDatasets(tAPMArgumentTable, iPortfolioId);
				if (iRetVal == 0)
					sErrMessage = "Failed batch start operation for portfolio";
				else
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Completed batch start operation for portfolio");
				
				if (iRetVal != 0 )
				{
					Table accessiblePortfolios = RefBase.retrievePersonnelPortfolios(RefBase.getUserId());
					if ( accessiblePortfolios.unsortedFindInt("portfolio_id", iPortfolioId) > 0)
					{
				      APM_ExecuteQuery executeQuery = new APM_ExecuteQuery();			
				      qreq = executeQuery.createQueryIdFromMainArgt(iMode, tAPMArgumentTable, tMainArgt, iPortfolioId);
				      iQueryId = qreq.getQueryId();
				      Table queryCount = Table.tableNew();
						m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, queryCount, "count(*)", "query_result", "unique_id = " + iQueryId);
						
						int numTransactions = queryCount.getInt(1,1);
						m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Query executed.  ID: " + iQueryId + " Count of transactions = " + numTransactions);
						queryCount.destroy();
						
						if (numTransactions == 0 )
						{
							// no eligible transactions - so set the no deals in pfolio flag
							noDealsInPfolioFlag = 1; 
							m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "No eligible deals returned for query.  Treating as empty portfolio.");
						}
					}
					else
					{
						// not allowed access to this portfolio - so set the no deals in pfolio flag
						noDealsInPfolioFlag = 1; 
						sProcessingMessage = "WARNING !! No access allowed to portfolio for this user: " + RefBase.getUserName() + ".  Treating as empty portfolio.";
						m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "WARNING !! No access allowed to portfolio for this user: " + RefBase.getUserName() + ".  Treating as empty portfolio.");
						m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeWarn, sJobName, "", iPortfolioId, -1, iTranNum, iDealNum, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
					}
				}
			}
			else 
				iQueryId = tAPMArgumentTable.getInt("pfolio_query_id", 1);				
			
			tAPMArgumentTable.setInt( "Tranche", 1, 1); // set to 1 (default), can be overridden if revalservice mthod used
			tAPMArgumentTable.setInt( "Job Query", 1, iQueryId);
			tAPMArgumentTable.setInt( "Current Pfolio Has No Deals",1, noDealsInPfolioFlag); 

			// check the query is ok
			if (iRetVal != 0 )
			{
				if ( iQueryId == 0 && noDealsInPfolioFlag == 0 )
				{
					sErrMessage = "Invalid query";
					m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, sErrMessage);
					iRetVal=0;
				}
			}
				
			if(iRetVal != 0)
				iMode = tAPMArgumentTable.getInt("Script Run Mode",1);
	
	
			/////////////////////////////////////////////////////
			//
			// Do the user initialization (load deals, setup sim)
			//
			/////////////////////////////////////////////////////
			if (iRetVal == 1 && (iMode != m_APMUtils.cModeDealDoNothing))
			{
				iRetVal = APM_Initialization (iMode, iQueryId, tRevalParam, tAPMArgumentTable, sJobName, argt.copyTable());
				if(iRetVal == 0)
				{
					sErrMessage = "Initialization failed";
					m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, sErrMessage);
				}
			}

			if(iRetVal != 0)
			{
				sProcessingMessage = "Starting to fill data for Portfolio: " + Table.formatRefInt(iPortfolioId, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE) + " Job# " + iJobNum;
				m_APMUtils.APM_LogStatusMessage( iMode, 0, m_APMUtils.cStatusMsgTypeProcessingAlways, sJobName, "", iPortfolioId, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
				m_APMUtils.APM_PrintMessage( tAPMArgumentTable, sProcessingMessage);
			}
	
			/////////////////////////////////////////////////////
			//
			// Fill Reporting Tables
			//
			///////////////////////////////////////////////////// 
			tBatchFailures = tAPMArgumentTable.getTable("Batch Failures", 1);
	
			if (iRetVal == 1 )
			{
				if ( noDealsInPfolioFlag == 1)
				{
					// just set the retval to succeeded as there are no deals to process
					revalPostreturnt.setInt("ret_val",1,1);	
				}
				else
				{
					iRetVal = APM_FillPackageDataTables( iMode, iQueryId, iPortfolioId, tAPMArgumentTable, tRevalParam,  sJobName, argt,  tBatchFailures, revalPostreturnt);		
					if(iRetVal == 0) 
					{              
						sErrMessage = "Failed to Run Sim or Fill APM reporting tables for portfolio: " + Table.formatRefInt(iPortfolioId, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
					}
				}
			}

			// if update after split then we don't pass pack the reporting tables as this script was working
			// with the main script's tables (CallN)
			if(runJobLocallyFlag == 1)
			{
				Table pkgDataTables = revalPostreturnt.getTable("Package Data Tables",1);
				if (Table.isTableValid(pkgDataTables) != 0)
						pkgDataTables.destroy();
			}
			
			// OConsole.oprint out the error message for this job
			if(iRetVal == 0) // no need to do status message here as the detailed msg will already have been sent
				m_APMUtils.APM_PrintErrorMessage( tAPMArgumentTable, sErrMessage);
	
			// Perform APM batch end operation to set dataset as active
			if ((iRetVal == 1) && (iMode == m_APMUtils.cModeBatch)) {
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Initiating batch end operation for portfolio");
				APM_BatchOps batchOperations = new APM_BatchOps();
				iRetVal = batchOperations.commitPendingDatasets(tAPMArgumentTable, iPortfolioId);
				if (iRetVal == 0)
					sErrMessage = "Failed batch end operation for portfolio";
				else
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Completed batch end operation for portfolio");
			}

			// if success then clear bad deal updates & send messages. Note
			// the equivalent is done for successful batch completions
			// just after the batch end operation
			// it may be that some of a block succeeded 
			// - so we need to clear those that were successful 
			if ((iRetVal == 1 && iMode != m_APMUtils.cModeBatch ) ||
				 (iRetVal == 0 && iMode == m_APMUtils.cModeBlockDealUpdate) ) 
			{					
				iRetVal =  m_APMUtils.APM_ClearTFEMsgLogForBadDealUpdates(iMode, iPortfolioId, -1, "", tAPMArgumentTable);
				if (iRetVal == 0)
					sErrMessage = "Failed to clear bad deal updates from message log (tfe_msg_log)";
			}

			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "================================================================");
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Completed processing portfolio " + Table.formatRefInt(iPortfolioId, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE));
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "================================================================");

			// If the portfolio failed save off the argt
			Table mainArgt = tAPMArgumentTable.getTable("Main Argt",1);
			if ( iRetVal == 0)
				m_APMUtils.SaveArgtForRerunIfSweeperOn(iMode, mainArgt, tAPMArgumentTable, iPortfolioId);								
			else
				m_APMUtils.DeleteSweeperEntriesForPortfolio(iMode, mainArgt, tAPMArgumentTable, iPortfolioId);								
				
		} finally {
			StatisticsLogging.instance().unSetJobContext(tAPMArgumentTable);

		   // get rid of the portfolio level query ID - no leaks please
			APM_ServiceJobs serviceJobs = new APM_ServiceJobs();
			serviceJobs.APM_DestroyPfolioQueryID(iMode, tAPMArgumentTable);

			// Stop any console logging operations.
			if ( iRetVal == 0 ) {
				try {
					ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).flush();
				} catch ( Throwable exception ) {
					m_APMUtils.APM_PrintErrorMessage( tAPMArgumentTable, exception.toString());
				}		
			}  else {
				ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).close( this );			
			}
			
			// Reset the log path
			String logFile = LogConfigurator.getInstance().pop();
			tAPMArgumentTable.setString("Log File", 1, logFile);

			// clean up
			tRevalParam.destroy();

			if ( qreq != null ) // this will only be set if we have the new V11 where we execute the query
			{
				if ( iQueryId > 0 )
					Query.clear(iQueryId);
				qreq.destroy(); 
			}			
			
			// This script should only return an error if the SplitProcess is in play and its a batch
			// then the splitprocess retry count can kick in to retry the portfolio level job
			// Otherwise in case of an error the error code/message is returned to and handled by the main script.
			if ( iRetVal == 0 && iMode == m_APMUtils.cModeBatch && runJobLocallyFlag == 0) 
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

Parameters:    iMode - m_APMUtils.cModeBatch, m_APMUtils.cModeDealApply, m_APMUtils.cModeDealBackout

Returns:       0 for FAIL, 1 for SUCCESS
-------------------------------------------------------------------------------*/
	int APM_Initialization (int iMode, int iQueryId, Table tRevalParam, Table tAPMArgumentTable, String sJobName, Table argt) throws OException
	{
		int iRetVal;
		Table tPortfolioScenarios;
		String   sSimName;

		sSimName = argt.getString("simulation_name",1);

		// list of scenarios for this processing job
		tPortfolioScenarios = argt.getTable("scenario_list",1);
		if(Table.isTableValid(tPortfolioScenarios) == 0)
		{
			m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, "Invalid scenario list");
			iRetVal=0;
		}  

		// set the scenario list for the current portfolio in the argument table
		tAPMArgumentTable.setTable("Portfolio_Scenario_List",1,tPortfolioScenarios.copyTable()); 

		iRetVal = APM_SetupRevalParam (iMode, tRevalParam, tPortfolioScenarios, sSimName, tAPMArgumentTable, argt);

		return iRetVal;
	}


	/*-------------------------------------------------------------------------------
Name:          APM_SetupRevalParam

Description:   This function sets up the RevalParam in 'argt' to include a
               base and double shift scenario

Parameters:    iMode - m_APMUtils.cModeBatch, m_APMUtils.cModeDealApply, m_APMUtils.cModeDealBackout
               tDealInfo - Collated deal data for all deals

Returns:       0 for FAIL, 1 for SUCCESS
-------------------------------------------------------------------------------*/
	int APM_SetupRevalParam (int iMode, Table tRevalParam, Table tPortfolioScenarios, String sSimName, Table tAPMArgumentTable, Table argt) throws OException
	{
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
		Sim.createRevalTable (tRevalParam);
		tRevalParam.setInt( "ClearCache", 1, 0);
		// Setting this flag should refresh the indexes & volatilities. However, it does not work for 
		// all branches so we must explicity refresh the market data.
		// For performance reasons, we do want to refresh the market data for every deal booking event
		tRevalParam.setInt( "RefreshMktd", 1, 0);

		// changes for reval service
		String sServiceName = tAPMArgumentTable.getString("service_name", 1);
		APM_ServiceJobs serviceJobs = new APM_ServiceJobs();
		if ( iMode == m_APMUtils.cModeBatch )
		{
			if(serviceJobs.APM_GetNumRevalServiceEngines(tAPMArgumentTable, sServiceName) == 0 )
			{
				tRevalParam.setInt( "AsmId", 1, -1);
			}
			else
			{
				if ( tRevalParam.getColNum("ApmRevalData") < 1)
					tRevalParam.addCol( "ApmRevalData", COL_TYPE_ENUM.COL_TABLE);	
				
				tRevalParam.setTable("ApmRevalData", 1, argt);
				tRevalParam.setString( "ServiceName", 1, sServiceName);
			}
		}
		else
		{
			tRevalParam.setInt( "AsmId", 1, -1);
		}
		
		if (tAPMArgumentTable.getInt("Market Data Source",1) == 2 )
		{
			// if a closing dataset ID not specified revert to universal
			if ( tAPMArgumentTable.getInt("Closing Dataset ID",1) > 0 )
			{
				// use closing prices
				tRevalParam.setInt( "UseClose", 1, 1);
				tRevalParam.setInt( "ClosingDatasetId", 1, tAPMArgumentTable.getInt("Closing Dataset ID",1));
			}
			else if ( iMode == m_APMUtils.cModeBatch && tAPMArgumentTable.getInt("Market Data Source",1) == 0 )
				tRevalParam.setInt( "RefreshMktd", 1, 1);

		}
		else if ((iMode == m_APMUtils.cModeBatch) && (tAPMArgumentTable.getInt("Market Data Source",1) == 0))
		{
			tRevalParam.setInt( "RefreshMktd", 1, 1);
		}    

		// Populate the revaluation parameters table
		tRevalParam.setInt( "SimRunId", 1, -1);
		tRevalParam.setInt( "SimDefId", 1, -1);

		force_intraday_reval_type = m_APMUtils.APM_GetOverallPackageSettingInt(tAPMArgumentTable, "force_intraday_reval_type", 1, 0, 0);

		// If the package requires prior EOD results to be loaded, it must be run as an INTRADAY revaluation
		// This is due to some buggy core code, where loading EOD results is dependent not on the SIM result itself
		// but on whether the revaluation is run as an INTRADAY (as opposed to GENERAL)
		if (force_intraday_reval_type > 0)
		{
			tRevalParam.setInt( "RunType", 1, SIMULATION_RUN_TYPE.INTRA_DAY_SIM_TYPE.toInt());
		}
		else
		{
			tRevalParam.setInt( "RunType", 1, SIMULATION_RUN_TYPE.GEN_SIM_TYPE.toInt());
		}

		tSourceSimDef = tAPMArgumentTable.getTable("Sim Def",1);
		tSimDef = Sim.createSimDefTable();
		Sim.addSimulation(tSimDef, sSimName);

		tSourceScenarios = tSourceSimDef.getTable( "scenario_def", 1);

		tScenarios = tSourceScenarios.cloneTable();
		tSimDef.setTable( "scenario_def", 1, tScenarios);

		tPortfolioScenarios.sortCol("scenario_name");
		// for each scenario
		for (iScenario = 1; iScenario <= tSourceScenarios.getNumRows(); iScenario++)
		{
			// Simulation definition
			sScenarioName = tSourceScenarios.getString( "scenario_name", iScenario);

			if (tPortfolioScenarios.findString( "scenario_name",sScenarioName, SEARCH_ENUM.FIRST_IN_GROUP) > 0)
			{
				// scenario is list of job scenarios, add as normal
				tSourceScenarios.copyRowAdd( iScenario, tScenarios);
				iNewScenarioRow = tScenarios.getNumRows();
				tScenarios.setString( "scenario_name", iNewScenarioRow, sScenarioName);
				tScenarios.setInt( "scenario_id", iNewScenarioRow, iNewScenarioRow);				
			}
			else
				continue; // not interested in this scenario
		}
		
		
		// Add details of the tran_info filter/splitter config to the sim def. This will be used by APM UDSRs (APM_UDSR_DealInfo)
		sWhat = "distinct tfd.filter_id, tfd.filter_name, tfd.ref_list_id, aesr.result_column_name, tfd.filter_type";
		sFrom = "tfe_filter_defs tfd, apm_pkg_enrichment_config apec, apm_enrichment_source_results aesr";
		sWhere = "tfd.filter_type in (5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17) and tfd.filter_name = apec.enrichment_name " +
                         "and apec.on_off_flag = 1 and aesr.enrichment_name = apec.enrichment_name";
   
		tEnabledTranInfoFilters = m_APMUtils.APM_TABLE_LoadFromDbWithSQLCached(tAPMArgumentTable, "APM Enabled Tran Info Filters", "TFE.METADATA.CHANGED", sWhat, sFrom, sWhere);
		if ( Table.isTableValid(tEnabledTranInfoFilters) == 0 )
		{   
			m_APMUtils.APM_PrintAndLogErrorMessage (iMode, tAPMArgumentTable, "Failed to load tran_info filter/splitter configuration details" );
			iRetVal = 0;
		}    
		else
		{
			tSimDef.addCol("APM Enabled Tran Info Filters", COL_TYPE_ENUM.COL_TABLE);
			tSimDef.setTable("APM Enabled Tran Info Filters", 1, tEnabledTranInfoFilters);
		}

		tRevalParam.setTable( "SimulationDef", 1, tSimDef);

		// NOW IF A CLOSING DATASET DATE HAS BEEN SPECIFIED ADD A DATE HORIZON MOD
		if ( tRevalParam.getInt( "UseClose", 1) == 1 
				&& (Str.len(tAPMArgumentTable.getString("Closing Dataset Date",1)) > 0)
				&& (Str.equal(tAPMArgumentTable.getString("Closing Dataset Date",1),"0d") == 0))	// closing dataset date != 0d		 
		{
			// ok - so this is going to be for a prior date - unset useclose as that 
			// causes todays close to be loaded accidentally
			tRevalParam.setInt( "UseClose", 1, 0);

			for (iScenario = 1; iScenario <= tScenarios.getNumRows(); iScenario++)
			{
				sScenarioName = tScenarios.getString( "scenario_name", iScenario);

				// If there is already a date horizon mod do not override and do not add one
				Table scenarioConfig = tScenarios.getTable("scenario_config_table", iScenario);
				if ( scenarioConfig != Util.NULL_TABLE && Table.isTableValid(scenarioConfig) != 0)
				{
				if ( scenarioConfig.unsortedFindInt("target_type", 4) > 0 )
					continue;
				}
				
				// add the date horizon mod
				Sim.addHorizonDateMod(tSimDef, tSimDef.getString("name", 1), sScenarioName, 0, tAPMArgumentTable.getString("Closing Dataset Date",1));
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

Description:   This function is called for each portfolio in tern
               The function should process trade data (often by running
               simulations) and subsequently fill the tPackageDataTables

Parameters:    iMode - m_APMUtils.cModeBatch, m_APMUtils.cModeDealApply, m_APMUtils.cModeDealBackout
               iPortfolio- the portfolio id the deals fall into
               tPackageDataTables - lower level functions should fill this

Returns:       0 for FAIL, 1 for SUCCESS
-------------------------------------------------------------------------------*/
	int APM_FillPackageDataTables(int iMode, int iQueryId, int iPortfolio, Table tAPMArgumentTable, Table tRevalParam, String sJobName, Table argt, Table  tBatchFailures, Table revalPostreturnt) throws OException
	{
		int iRetVal;
		Table tDealInfo;
		Table tJobScenarios;
		String   sSimName;
		int iLogStatusCol;

		sSimName = argt.getString("simulation_name",1);

		// list of scenarios for this processing job
		tJobScenarios = argt.getTable("scenario_list",1);
		if(Table.isTableValid(tJobScenarios) == 0)
		{
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Invalid scenario list in job arguments");
			iRetVal=0;
		}

		iRetVal = APM_RunSimulation (iMode, iQueryId, iPortfolio, tJobScenarios, tAPMArgumentTable, tRevalParam, sSimName, sJobName, argt, tBatchFailures, revalPostreturnt);

		//iRetVal = 0;
		//OConsole.oprint ("##### Failing Simulation Test #####\n");
		// set a failed status in the deal info table so the deal get re-run by the log monitor when block mode in operation
		if ((iRetVal == 0) && (iMode != m_APMUtils.cModeBatch))
		{
			// If we're in block mode then set the log_status to failed for all deals in this block
			// This will create a bad deal update for all deals in block but they will then be re-reun by
			// the log monitor singularly so after the first log monitor run only the bad deals in the block will remain bad
			tDealInfo = tAPMArgumentTable.getTable("Filtered Deal Info",1); // use the filtered one (inside pfolio loop)
			iLogStatusCol = tDealInfo.getColNum("log_status");
			if (iLogStatusCol > 0)
			{
				// now we need to only update the status of the deals affected by the failure
				// if we are in grid this could be a subset of the whole deal info
				// as defined by the query_id
				Table tBlockTranNum = Table.tableNew();
				tBlockTranNum.addCol("query_result", COL_TYPE_ENUM.COL_INT);
				tBlockTranNum.addCol("log_status", COL_TYPE_ENUM.COL_INT);
				m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tBlockTranNum, "query_result", "query_result", "unique_id = " + iQueryId);
				tBlockTranNum.setColValInt(2, OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt());
				tDealInfo.select(tBlockTranNum, "log_status", "query_result EQ $tran_num");
				tBlockTranNum.destroy();
			}            
		}


		return iRetVal;
	}

	int APM_SetupRevalParam(int iMode, Table tAPMArgumentTable, String sJobName, int iQueryId, int iPortfolio, Table tRevalParam) throws OException
	{
		int iRetVal=1;
		int iReceivedTranNum;
		
		int run_as_avs_script = m_APMUtils.APM_GetOverallPackageSettingInt(tAPMArgumentTable, "run_as_avs_script", 0, 1, 0);
		
		/* get scenario info */
		Table tSimDef = tRevalParam.getTable( "SimulationDef", 1);

		// Need to set the portfolio on the reval param, otherwise the simulation results that need prior EODs will not be able
		// to load this data
		tRevalParam.setInt( "Portfolio", 1, iPortfolio);

		//Creating a query id based on the DISTINCT list of transactions
		tRevalParam.setInt( "QueryId", 1, iQueryId);

		//check for the prior reval types
		if ( tRevalParam.getColNum( "PriorRunType") > 0 )
		{
			int priorRevalType = m_APMUtils.getPriorRevalTypeForPackages(iMode, tAPMArgumentTable, false);
			tRevalParam.setInt( "PriorRunType", 1, priorRevalType);
		}
		
		// for results where we don't care about really running the sim
		// add the query ID column & set it so we don't load deals/refresh mktd
		// useful for packages which are not running proper sims, just OpenJvs scripts
		if ( run_as_avs_script == 1 )
		{
			//don't refresh the market data
			tRevalParam.setInt( "RefreshMktd", 1, 0);

			if ( tRevalParam.getColNum( "PriorRunType") > 0 )
				tRevalParam.setInt( "PriorRunType", 1, 0);
			if ( tRevalParam.getColNum( "DoGlobalPortfolioCalcs") > 0 )
				tRevalParam.setInt( "DoGlobalPortfolioCalcs", 1, 0);

			//find a single valid tran num - cache as the query can take some time
			//don't use the APM specific caching as its not needed for this
			Table querySingleDeal = Table.getCachedTable("APM Single Deal Query");
			if(Table.isTableValid(querySingleDeal) == 0)
			{
				//find a simple deal for the sim to load up - not a complicated one
				//do this to speed up the reval
				m_APMUtils.APM_PrintMessage (tAPMArgumentTable, "Finding & caching Single Deal Number");
				querySingleDeal = Table.tableNew();
				
				// tran_status - 1 = Pending
				// tran_status - 2 = New
				// tran_status - 3 = Validated
				// tran_status - 7 = Proposed
				// tran_type   - 0 = Trading
				
				m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, querySingleDeal, "min(tran_num)", "ab_tran", 
				"tran_status in (1, 2, 3, 7) and tran_type = 0 and toolset not in (33,36,37,10,38)");

				//------------------------------------------------------------
				// CHECK #1
				// Check against a single row with tran_status = 0,				
				if ( querySingleDeal.getNumRows() > 0 ) 
				{
					iReceivedTranNum = querySingleDeal.getInt(1, 1);					
				}
				else
				{
					iReceivedTranNum = 0;
				}
				// if we don't get any rows then we have to load up a complicated deal.
				if ( querySingleDeal.getNumRows() < 1  || iReceivedTranNum == 0 )
				{
					m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, querySingleDeal, "min(tran_num)", "ab_tran", 
					"tran_status in (1, 2, 3, 7) and tran_type = 0");
				}
				
				//------------------------------------------------------------
				// CHECK #2
				// Check against a single row with tran_status = 0, 
				// but for all toolsets available				
				if ( querySingleDeal.getNumRows() > 0 ) 
				{
					iReceivedTranNum = querySingleDeal.getInt(1, 1);
				}
				else
				{
					iReceivedTranNum = 0;
				}
				
				if ( iReceivedTranNum  == 0 )         
				{
					// Was unable to retrieve any useful information
					// Failed to retrieve data for the 'APM Single Deal Query' cache. At least one 'Trading' type deal with status 'Pending' or 'Validated' required. Please contact APM Support. 

					String sProcessingMessage = "Failed to retrieve data for the 'APM Single Deal Query' cache. At least one 'Trading' type deal with status 'Pending' or 'Validated' required. Please contact APM Support.";
					m_APMUtils.APM_LogStatusMessage( iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, sJobName, "", iPortfolio, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
					m_APMUtils.APM_PrintMessage( tAPMArgumentTable, sProcessingMessage);            
				}				

				Table.cacheTable("APM Single Deal Query", querySingleDeal);
			}
			int newQueryId = m_APMUtils.APM_TABLE_QueryInsert(tAPMArgumentTable, querySingleDeal, 1);

			//now set this to fool the sim
			tRevalParam.setInt( "QueryId", 1, newQueryId);

			//add the real query ID into its own column in the sim def
			//this will be accessed by the results
			if ( tSimDef.getColNum( "APM Single Deal Query") < 1 )
				tSimDef.addCol( "APM Single Deal Query", COL_TYPE_ENUM.COL_INT);

			tSimDef.setInt( "APM Single Deal Query", 1, iQueryId);
		}
		else
		{
			if ( tSimDef.getColNum( "APM Single Deal Query") > 0 )
				tSimDef.delCol( tSimDef.getColNum( "APM Single Deal Query" ) );      
		}

		// Enrich the table with dataset type, portfolio and table of scenarios - used by appropriate filters
		if ( tSimDef.getColNum( "APM Dataset Type ID") <= 0 )
		{
			tSimDef.addCol( "APM Dataset Type ID", COL_TYPE_ENUM.COL_INT);
			tSimDef.setInt( "APM Dataset Type ID", 1, tAPMArgumentTable.getInt( "dataset_type", 1));
		}

		if ( tSimDef.getColNum( "APM Portfolio ID") <= 0 )
		{
			tSimDef.addCol( "APM Portfolio ID", COL_TYPE_ENUM.COL_INT);
			tSimDef.setInt( "APM Portfolio ID", 1, iPortfolio);
		}

		if ( tSimDef.getColNum( "APM Scenario IDs") <= 0 )
		{
			tSimDef.addCol( "APM Scenario IDs", COL_TYPE_ENUM.COL_TABLE);
			tSimDef.setTable( "APM Scenario IDs", 1, tAPMArgumentTable.getTable( "Portfolio_Scenario_List", 1).copyTable());
		}

		if ( tSimDef.getColNum( "APM Run Mode") <= 0 )
		{
			tSimDef.addCol( "APM Run Mode", COL_TYPE_ENUM.COL_INT);
			tSimDef.setInt( "APM Run Mode", 1, iMode);
		}
		
		return iRetVal;
	}
	
	int APM_RunSimulation (int iMode, int iQueryId, int iPortfolio, Table tJobScenarios, Table tAPMArgumentTable, 
			Table tRevalParam, String sSimName, String sJobName, Table argt, Table tBatchFailures, Table revalPostreturnt) throws OException
	{
		Table tResults = Util.NULL_TABLE;
		Table tOldRevalParam;
		int iRetVal=1;
		String sProcessingMessage;
		String sSavedSimName;
		int export_sim_defs;

		boolean runRevalPostLocally = true;
		APM_ServiceJobs serviceJobs = new APM_ServiceJobs();
		if ( iMode == m_APMUtils.cModeBatch )
		{
			String sServiceName = tAPMArgumentTable.getString("service_name", 1);
			if(serviceJobs.APM_GetNumRevalServiceEngines(tAPMArgumentTable, sServiceName) > 0 )
				runRevalPostLocally = false;	
		}
		
		if ( !m_APMUtils.skipSimulationForDealUpdates(iMode, tAPMArgumentTable))
		{
			// get package settings
			export_sim_defs = m_APMUtils.APM_GetOverallPackageSettingInt(tAPMArgumentTable, "export_sim_defs", 1, 0, 0);
	
			sProcessingMessage = "Starting to run Simulation for Portfolio: " + Table.formatRefInt(iPortfolio, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
			m_APMUtils.APM_LogStatusMessage( iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, sJobName, "", iPortfolio, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
			m_APMUtils.APM_PrintMessage( tAPMArgumentTable, sProcessingMessage);
	
			iRetVal = APM_SetupRevalParam(iMode, tAPMArgumentTable, sJobName, iQueryId, iPortfolio, tRevalParam);
			
			// export the sim defn if it has been requested
			if ( export_sim_defs == 1 && iMode == m_APMUtils.cModeBatch)
			{
				sSavedSimName = tAPMArgumentTable.getString( "service_name", 1) + " " + Table.formatRefInt(iPortfolio, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
				if ( Str.len(sSavedSimName) > 32 ) //Math.max size of name is 32 char - otherwise we get an error
					sSavedSimName = Str.substr(sSavedSimName, 0, 32);
	
				tRevalParam.getTable( "SimulationDef", 1).setString("name", 1, sSavedSimName);
				Sim.saveSimulation(tRevalParam.getTable( "SimulationDef", 1), sSavedSimName, 1, 0, 1 );
				m_APMUtils.APM_PrintMessage (tAPMArgumentTable, "Saved simulation to " + sSavedSimName );
			}
	
			// Set the reval param table to argt table
			if (argt.getColNum( "RevalParam") < 1)
				argt.addCol( "RevalParam", COL_TYPE_ENUM.COL_TABLE);
			tOldRevalParam = argt.getTable( "RevalParam", 1);
			if(Table.isTableValid(tOldRevalParam) != 0) tOldRevalParam.destroy();
			argt.setTable( "RevalParam", 1, tRevalParam.copyTable());
				
			tResults = Util.NULL_TABLE;
			try
			{
				tResults = Sim.runRevalByParamFixed(argt);
			}
			catch(Exception t)
			{
				tResults = Util.NULL_TABLE;
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception while calling sim run: " + t);
				String message = m_APMUtils.getStackTrace(t);
				m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, message);
				iRetVal = 0;
				
				// now we need to populate the errors correctly
				revalPostreturnt.setString("error_message", 1, "Exception whilst running simulation");
				revalPostreturnt.setInt("ret_val",1,0);
						
				// if block update check what failed
				if (iMode == m_APMUtils.cModeBlockDealUpdate)
				{
					Table tDealInfo = tAPMArgumentTable.getTable("Filtered Deal Info",1); // use the filtered one (inside pfolio loop)
					Table blockFails = tDealInfo.cloneTable();
					blockFails.select(tDealInfo, "*", "log_status EQ " + OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt());
					revalPostreturnt.setTable("Block Update Failures", 1, blockFails);
					blockFails = Util.NULL_TABLE;
				}							
			}
		}

		// set it here as the we will get succeeded if only 1 key out of many failed
		if (iMode == m_APMUtils.cModeBatch )
			revalPostreturnt.setTable("Batch Failures", 1, tBatchFailures.copyTable());
		
		if ( tAPMArgumentTable.getColNum("simulation_results") < 1 )
			tAPMArgumentTable.addCol("simulation_results", COL_TYPE_ENUM.COL_TABLE);

		if ( iRetVal == 1 )
		{
			if ( runRevalPostLocally )
			{
				tAPMArgumentTable.setTable("simulation_results", 1, tResults);
				
				String script_name = m_APMUtils.find_script_path("APM_RevalPost");
				try
				{
					iRetVal = Util.runScript(script_name, argt, revalPostreturnt);
				}
				catch(Exception t)
				{
					iRetVal = 0;
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception while calling APM_RevalPost: " + t);
					String message = m_APMUtils.getStackTrace(t);
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message+ "\n");
				}

				StatisticsLogging.instance().setScriptContext(tAPMArgumentTable, "RTPE_APM_Generic_DealProcessor");
				if ( iRetVal == 0)
				{
					String sErrMessage = "Failed to call APM_RevalPost for portfolio : " + Table.formatRefInt(iPortfolio, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
					m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, sErrMessage);
				}
			}
			else
				revalPostreturnt.setInt("ret_val",1,1);				
		}
		
		if ( Table.isTableValid(tResults) != 0)
			tResults.destroy();
		
		tAPMArgumentTable.setTable("simulation_results", 1, Util.NULL_TABLE);
		
		return iRetVal;
	}
	
}
