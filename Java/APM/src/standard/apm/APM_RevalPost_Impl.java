/* Released with version 05-Feb-2020_V17_0_126 of APM */

/*
 Description : This forms part of the Active Position Manager package

 Will be run automatically as postprocess script on revalservice cluster node if revalservice configured.  
 Otherwise has to be manually called. 

 1)	Calls HandlesResults class
 2) 	Calls UpdateTables script
 3)	Updates return statuses
 4)	Makes sure the simulation result data is destroyed (otherwise results will be aggregated and sent back to caller ? not good !!)
*/

package standard.apm;
import standard.include.APM_Utils;
import standard.include.ConsoleCaptureWrapper;
import standard.include.LogConfigurator;
import standard.include.ConsoleLogging;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.Ref;
import com.olf.apm.ioc.Container;

// All code in this file used to exist in APM_RevalPost.

public class APM_RevalPost_Impl {
	private APM_Utils m_APMUtils;
	
	static {
		Container.createInstance(APM_RevalPost_Impl.class.getClassLoader());
	}
	
	public APM_RevalPost_Impl(){
		m_APMUtils = new APM_Utils();

	}

	public int execute(Table argt, Table returnt) throws OException
	{
		Table tAPMArgumentTable = Util.NULL_TABLE;
		Table tJobArgt = Util.NULL_TABLE;
		Table tBatchFailures = null;
		String sProcessingMessage = " ";
		String sJobName;
		String sErrMessage="";
		int iQueryId;
		int iMode = m_APMUtils.cModeUnknown;
		int entityGroupId;
		int iRetVal=1;
		int iJobNum;
		int noEntitiesInJobFlag;

		try {
		
			/////////////////////////////////////////////////////
			//
			// Get the argument table, extract common data we need
			// then enrich with more job specific data
			//
			/////////////////////////////////////////////////////
			if(argt.getNumRows() >0)
			{
				// check if we are running locally
				if ( argt.getColNum("job_arg_table") > 0 )
				{
					tJobArgt = argt;
				}
				else
				{
					// we must be in revalservice mode
					if ( argt.getColNum("job_argt") > 0 )
					{
						Table engineJobArgt = argt.getTable("job_argt", 1);
						if ( engineJobArgt.getColNum("reval_table") > 0 )
						{
							Table revalTable = engineJobArgt.getTable("reval_table", 1);
							if ( revalTable.getColNum("ApmRevalData") > 0 )
								tJobArgt = revalTable.getTable("ApmRevalData", 1);
							else
							{
								OConsole.oprint("Unrecognised mode.  Missing ApmRevalData from reval_table.  Exiting ...");	
								return 0;
							}
						}
						else
						{
							OConsole.oprint("Unrecognised mode.  Missing reval_table from job_argt.  Exiting ...");	
							return 0;
						}						
					}
					else
					{
						OConsole.oprint("Unrecognised mode.  Missing job_argt from argt.  Exiting ...");
						return 0;
					}
				}
			}
			else
			{
				OConsole.oprint ("Missing row from argt. Exiting ...");
				return 0;
			}
			
			if(Table.isTableValid(tJobArgt) == 0)
			{
				OConsole.oprint ("Failed to get Job argt table. Exiting ...");
				return 0;
			}
			
			tAPMArgumentTable = tJobArgt.getTable("job_arg_table",1);			
			if(Table.isTableValid(tAPMArgumentTable) == 0)
			{
				tJobArgt = Util.NULL_TABLE;
				OConsole.oprint ("Failed to get APM argt table. Exiting ...");
				return 0;
			}
			
			// Set the path/log information.		
			String logFilePath = "";	
			if ( Str.isEmpty(Util.getEnv("AB_ERROR_LOGS_PATH"))==1 )
				logFilePath = Util.getEnv("AB_OUTDIR") + "/error_logs/";
			else
				logFilePath = Util.getEnv("AB_ERROR_LOGS_PATH") + "/";
			
			// Set the log path to be used by our logger.
			LogConfigurator.getInstance().setPath( logFilePath );
			String serviceName = tAPMArgumentTable.getString("service_name", 1);
			LogConfigurator.getInstance().setServiceName( serviceName );
			LogConfigurator.getInstance().push( tAPMArgumentTable.getString("Log File", 1 ) );
			tAPMArgumentTable.setString("Log File", 1, LogConfigurator.getInstance().front());
			
			if ( !ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).isOpen() ) {
				// Open the console capture and register this object as its owner.
				ConsoleCaptureWrapper.getInstance(tAPMArgumentTable).open( this );						
			}

			ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "APM_RevalPost");
			ConsoleLogging.instance().setProcessIdContext(tAPMArgumentTable, Ref.getProcessId());
			
			/////////////////////////////////////////////////////
			//
			// Get the split specific arguments
			//
			/////////////////////////////////////////////////////

			Table mainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
			m_APMUtils.FindAndSetEntityType(mainArgt, tAPMArgumentTable);
			
			// job info
			sJobName = tJobArgt.getString("job_name",1);
			iJobNum = tJobArgt.getInt("job_num",1);

			ConsoleLogging.instance().setJobContext(tAPMArgumentTable, sJobName);

			// run mode - get this up here so that error logging works correctly
			iMode = tAPMArgumentTable.getInt("Script Run Mode",1);
	
			// portfolio
			entityGroupId = tJobArgt.getInt("entity_group_id",1);
	
			// set up the rest of the processor arguments specific to this job
			iQueryId = tAPMArgumentTable.getInt("job_query_id",1);
			noEntitiesInJobFlag = tAPMArgumentTable.getInt("Current Job Has No Entities",1);

			// override tranche if we have been triggered via the revalservice method
			if ( argt.getColNum("job_number") > 0 )
				tAPMArgumentTable.setInt( "Tranche", 1, argt.getInt("job_number", 1));
			
			int entityGroupTranche = tAPMArgumentTable.getInt( "Tranche", 1);
			
			tAPMArgumentTable.setInt( "Job Query", 1, iQueryId);

			iMode = tAPMArgumentTable.getInt("Script Run Mode",1);

			if ( iMode != m_APMUtils.cModeBackout)
			{
				sProcessingMessage = "Finished running Simulation for " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId) + " Job# " + iJobNum + " Tranche# " + entityGroupTranche;
				m_APMUtils.APM_LogStatusMessage( iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, sJobName, "", entityGroupId, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
				m_APMUtils.APM_PrintMessage (tAPMArgumentTable, sProcessingMessage);
			}
			
			sProcessingMessage = "Starting to populate tables for " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId) + " Job# " + iJobNum + " Tranche# " + entityGroupTranche;
			m_APMUtils.APM_LogStatusMessage( iMode, 0, m_APMUtils.cStatusMsgTypeProcessingAlways, sJobName, "", entityGroupId, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
			m_APMUtils.APM_PrintMessage( tAPMArgumentTable, sProcessingMessage);
	
			Table tResults = Util.NULL_TABLE;
			// find the results
			boolean foundResults = false;
			if ( tAPMArgumentTable.getColNum("simulation_results") > 0)
			{			
				tResults = tAPMArgumentTable.getTable( "simulation_results", 1); //local
				foundResults = true;
			}
			else
			{
				// probably in revalservice method so look in the out table
				if ( argt.getColNum("engine_returnt") > 0)
				{
					Table engineReturnt = argt.getTable("engine_returnt", 1);
					if (engineReturnt.getColNum("out") > 0)
					{
						Table engineOutput = engineReturnt.getTable("out", 1);
						if (engineOutput.getColNum("srun_table") > 0)
						{
							Table srunTable = engineOutput.getTable("srun_table", 1);
							if (srunTable.getColNum("scenarios") > 0)
							{
								tResults = srunTable.getTable("scenarios", 1);
								foundResults = true;
							}
						}
					}
				}
			}

			if (foundResults == false )
			{
				sErrMessage = "No results found from Sim.runRevalByParamFixed";
				m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, sErrMessage);
				iRetVal = 0;
			}

//CAUSE A FAILURE
//if ( iEntityGroupId == 20018 )
//{
//	iRetVal = 0;
//	tBatchFailures = tAPMArgumentTable.getTable( "Batch Failures", 1);
//	int row = tBatchFailures.addRow();
//	tBatchFailures.setInt("entity_group_id", row, 20018);
//	tBatchFailures.setString("package", row, "Cashflows");
//	tBatchFailures.setInt("scenario_id", row, 1);	
//	m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, "wibble");
//}
	
			if ( iRetVal == 1 && !m_APMUtils.skipSimulationForUpdates(iMode, tAPMArgumentTable))
			{
				iRetVal = APM_EntityJobOps.instance().CheckSimResultsValid(iMode, tAPMArgumentTable, tResults );
		
				tBatchFailures = tAPMArgumentTable.getTable( "Batch Failures", 1);
				if(Table.isTableValid(tBatchFailures) == 0)
				{
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Invalid batch failures list in job arguments");
					iRetVal=0;
				}
		
				Table tJobScenarios = tJobArgt.getTable("scenario_list",1);
				if(Table.isTableValid(tJobScenarios) == 0)
				{
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Invalid scenario list in job arguments");
					iRetVal=0;
				}
			
				if ( iRetVal > 0 )
				{
					APM_HandleSimResults handleSimResults = new APM_HandleSimResults(iMode, sJobName, tAPMArgumentTable, entityGroupId, entityGroupTranche, tResults, tJobScenarios, tBatchFailures, noEntitiesInJobFlag, iQueryId);
					iRetVal = handleSimResults.HandleResults();
					if(iRetVal == 0) 
					{              
					    sErrMessage = "Failed to Fill APM reporting tables for " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId) + ", tranche #" + entityGroupTranche;
					}
				}
			}
		
			/////////////////////////////////////////////////////
			//
			// Update
			//
			///////////////////////////////////////////////////// 
			if (iRetVal == 1 )
			{
				sProcessingMessage = "Finished filling data for " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId) + ", tranche #" + entityGroupTranche;
				m_APMUtils.APM_LogStatusMessage( iMode, 0, m_APMUtils.cStatusMsgTypeProcessingAlways, sJobName, "", entityGroupId, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
				m_APMUtils.APM_PrintMessage( tAPMArgumentTable, sProcessingMessage);
				
				m_APMUtils.qaModeSaveResultToFile(tAPMArgumentTable, serviceName, sJobName, iMode);
	
				try
				{
					APM_UpdateTables_Impl updateTables = new APM_UpdateTables_Impl();
					iRetVal = updateTables.execute(tAPMArgumentTable);
				}
				catch(Exception t)
				{
					iRetVal = 0;
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception while calling APM_UpdateTables: " + t);
					String message = m_APMUtils.getStackTrace(t);
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message+ "\n");
				}

				ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "APM_RevalPost");
				if ( iRetVal == 0)
				{
					sErrMessage = "Failed to update APM Reporting tables for " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId) + ", tranche #" + entityGroupTranche;
					m_APMUtils.APM_PrintAndLogErrorMessageSafe( iMode, tAPMArgumentTable, sErrMessage);
				}
			}
			
			////////////////////////////////////////////////////////////////////////////////
			//
			// Fill in the job return table
			//
			////////////////////////////////////////////////////////////////////////////////
			m_APMUtils.APM_setupJobReturnt(returnt);
			returnt.setInt("job_num",1,iJobNum);
			returnt.setString("job_name",1,sJobName);
			returnt.setInt("entity_group_id",1,entityGroupId);
			
			// set it here as the we will get succeeded if only 1 key out of many failed
			if (iMode == m_APMUtils.cModeBatch )
			{
				if ( Table.isTableValid(tBatchFailures) != 0 )
					returnt.setTable("Batch Failures", 1, tBatchFailures.copyTable());
			}
			
			// save the job result and any data to pass back in result table
			if(iRetVal == 0)
			{
				returnt.setInt("ret_val",1,0);
				returnt.setString("error_message",1,sErrMessage);
				
				// if block update check what failed
				if (iMode == m_APMUtils.cModeBlockUpdate)
				{
					Table tEntityInfo = tAPMArgumentTable.getTable("Filtered Entity Info",1); // use the filtered one (inside pfolio loop)
					Table blockFails = tEntityInfo.cloneTable();
					blockFails.select(tEntityInfo, "*", "log_status EQ " + OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt());
					returnt.setTable("Block Update Failures", 1, blockFails);
					blockFails = Util.NULL_TABLE;
				}		
				
				// If the entity group failed save off the argt
				m_APMUtils.SaveArgtForRerunIfSweeperOn(iMode, mainArgt, tAPMArgumentTable, entityGroupId);												
			}  
			else
			{
				returnt.setInt("ret_val",1,1);
				returnt.setString("error_message",1,"Job Succeeded");      
			}
			
			// OConsole.oprint out the error message for this job
			if(iRetVal == 0) // no need to do status message here as the detailed msg will already have been sent
				m_APMUtils.APM_PrintErrorMessage( tAPMArgumentTable, sErrMessage);
						
			ConsoleLogging.instance().unSetJobContext(tAPMArgumentTable);
	
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
			String logfile = LogConfigurator.getInstance().pop();
			tAPMArgumentTable.setString("Log File", 1, logfile);	
		}
		catch(Exception t)
		{
			m_APMUtils.APM_PrintErrorMessage( tAPMArgumentTable, "General exception t" + t);
			iRetVal = 0;
		}	

		// In case of an error the error code/message is returned to and handled by the main script.
		tJobArgt = Util.NULL_TABLE; // make sure this is nulled
		
		return iRetVal;	
	}
}
