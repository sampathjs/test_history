/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;


import standard.include.APM_Utils;
import standard.include.ConsoleLogging;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public class APM_Clear_Bad_Updates implements IScript 
{
	// the utilities to include
	private APM_Utils m_apmUtils;

	//
	public APM_Clear_Bad_Updates() 
	{
		m_apmUtils = new APM_Utils();
	}
	
	//
	public void execute(IContainerContext context) throws OException 
	{
		int returnValue = 1;
		
		try
		{
			Table argt = context.getArgumentsTable();
			
			// initialise basic configuration.
			Table tAPMArgumentTable = Table.tableNew("APM Argument Table");
			InitialiseArgTable( tAPMArgumentTable );
	
			m_apmUtils.APM_PrintMessage(tAPMArgumentTable, "------- CLEARING BAD UPDATES !!! --------");			

			String sServiceName;
			if ( argt.getColNum("ServiceName") > 0 && argt.getNumRows() > 0 )
			{
				for (int i = 1; i <= argt.getNumRows(); i++)
				{
					sServiceName = argt.getString("ServiceName", i);
					updateArgTable(sServiceName, tAPMArgumentTable);
					deleteBadUpdatesForService(sServiceName, tAPMArgumentTable);
				}
			}
			else
			{
				m_apmUtils.APM_PrintMessage(tAPMArgumentTable, "------- NO SERVICE NAMES IN ARGT: CLEARING BAD UPDATES FOR ALL SERVICES --------");
				Table tAPMServices = Table.tableNew("APM_Services");;
				returnValue = m_apmUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tAPMServices, "name", "job_cfg", "type = 0 and service_group_type = 33");
				if (returnValue != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) 
				{
					m_apmUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Unable to find APM services.  Exiting");
					Util.exitFail();
				} 

				for (int i = 1; i <= tAPMServices.getNumRows(); i++)
				{
					sServiceName = tAPMServices.getString(1, i);
					updateArgTable(sServiceName, tAPMArgumentTable);
					deleteBadUpdatesForService(sServiceName, tAPMArgumentTable);
				}
				
			}
			
			m_apmUtils.APM_PrintMessage(tAPMArgumentTable, "------- FINISHED CLEARING BAD UPDATES --------");
			tAPMArgumentTable.destroy();
		
		}
		catch(Exception t)
		{
			returnValue = 0;
			OConsole.oprint( "Exception while calling Clear Bad Updates script: " + t);
			String message = m_apmUtils.getStackTrace(t);
			OConsole.oprint( message+ "\n");
		}
	}
			
	///
	private void InitialiseArgTable(Table tAPMArgumentTable) throws OException
	{
		// Initialise the main user table.
		tAPMArgumentTable.addCol("Log File", COL_TYPE_ENUM.COL_STRING);
		tAPMArgumentTable.addCol("Message Context", COL_TYPE_ENUM.COL_TABLE);
		tAPMArgumentTable.addCol("Debug", COL_TYPE_ENUM.COL_INT);
		tAPMArgumentTable.addCol("service_id", COL_TYPE_ENUM.COL_INT);
		tAPMArgumentTable.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
		tAPMArgumentTable.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
		tAPMArgumentTable.addCol("Simulation Name", COL_TYPE_ENUM.COL_STRING);
		tAPMArgumentTable.addCol("Scenario Names", COL_TYPE_ENUM.COL_TABLE);
		tAPMArgumentTable.addCol("Package Details", COL_TYPE_ENUM.COL_TABLE);
		tAPMArgumentTable.addCol("Scenario Ids", COL_TYPE_ENUM.COL_TABLE);
		tAPMArgumentTable.addCol("Entity Group Ids", COL_TYPE_ENUM.COL_TABLE);
		tAPMArgumentTable.addCol("Bad Updates", COL_TYPE_ENUM.COL_TABLE);
		tAPMArgumentTable.addCol("Main Argt", COL_TYPE_ENUM.COL_TABLE);
		tAPMArgumentTable.addCol("Scenario_List", COL_TYPE_ENUM.COL_TABLE);
		tAPMArgumentTable.addCol("RTP Page Prefix", COL_TYPE_ENUM.COL_STRING);
		tAPMArgumentTable.addRow();

		Table messageContext = Table.tableNew("Message Context");
		messageContext.addCol("ContextName", COL_TYPE_ENUM.COL_STRING);
		messageContext.addCol("ContextValue", COL_TYPE_ENUM.COL_STRING);
		tAPMArgumentTable.setTable("Message Context", 1, messageContext);

		Table mainArgt = Table.tableNew("Main argt");
		tAPMArgumentTable.setTable("Main Argt", 1, mainArgt);

		Table tScenarioList = Table.tableNew("scenario_list");
		tScenarioList.addCol("scenario_id", COL_TYPE_ENUM.COL_INT);
		tScenarioList.addCol("scenario_name", COL_TYPE_ENUM.COL_STRING);
		tScenarioList.addNumRows(1);
		tAPMArgumentTable.setTable("scenario_List", 1, tScenarioList);
		
		tAPMArgumentTable.setInt("Debug", 1, 1);

		ConsoleLogging.instance().setProcessIdContext(tAPMArgumentTable, Ref.getProcessId());

		ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "APM_Clear_Bad_Updates");
	}
	
	private int updateArgTable(String sServiceName, Table tAPMArgumentTable) throws OException 
	{
		Table tServiceID = Table.tableNew("service_id");
		int returnValue = m_apmUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tServiceID, "wflow_id", "job_cfg", "name = '" + sServiceName + "' and type = 0");
		if (returnValue != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() || tServiceID.getNumRows() < 1) 
		{
			m_apmUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Unable to find wflow ID for APM service.");
			tServiceID.destroy();
			return 0;
		}		
		int iServiceID = tServiceID.getInt(1, 1);
		tServiceID.destroy();
		
		// set the values in the tAPMArgumentTable
		tAPMArgumentTable.setInt("service_id", 1, iServiceID);
		tAPMArgumentTable.setString("service_name", 1, sServiceName);

		ConsoleLogging.instance().setServiceContext(tAPMArgumentTable, sServiceName);

		// Setup the log file
		
		String logFileName;
		String logFilePath;
		
		if ( Str.isEmpty(Util.getEnv("AB_ERROR_LOGS_PATH"))==1 )
			logFilePath = Util.getEnv("AB_OUTDIR") + "/error_logs/";
		else
			logFilePath = Util.getEnv("AB_ERROR_LOGS_PATH") + "/";

		logFileName = logFilePath + sServiceName + ".log";
		tAPMArgumentTable.setString("Log File", 1, logFileName);
	
		return 1;
	}

	//
	private int deleteBadUpdatesForService(String sServiceName, Table tAPMArgumentTable) throws OException 
	{
		int iRetVal = 0;

		m_apmUtils.APM_PrintMessage(tAPMArgumentTable, "Attempting to clear Bad Updates for Service: " + sServiceName);
		
		int iServiceID = tAPMArgumentTable.getInt("service_id", 1);
		
		Table tBadIncrementals = Table.tableNew("bad_incrementals");
	   int returnValue = m_apmUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tBadIncrementals, "distinct entity_group_id, rtp_page_subject, dataset_type_id, scenario_id", "apm_msg_log", "service_id = " + iServiceID + " and msg_type = 2 and primary_entity_num > -1");
		if (returnValue != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt() )
		{
			m_apmUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Unable to run SQL to find Entity group IDs containing Bad updates for APM service.");
			tBadIncrementals.destroy();
			return 0;			
		}
		
		if (tBadIncrementals.getNumRows() == 0 )
		{
			m_apmUtils.APM_PrintMessage(tAPMArgumentTable, "Nothing to do.....");
			return 1;
		}

		returnValue =  m_apmUtils.APM_FindBadUpdates(m_apmUtils.cModeBatch, iServiceID, tAPMArgumentTable, Util.NULL_TABLE);
		if ( returnValue == 0)
		{
			m_apmUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Failed to find bad updates for service:" + sServiceName);
			return 0;
		}
				
		for (int i = 1; i <= tBadIncrementals.getNumRows(); i++)
		{
			int iEntityGroupId = tBadIncrementals.getInt(1, i);
			// set the rtp page subject into the user table
			int endStringPosition = Str.findSubString(tBadIncrementals.getString(2, i), ".");
			String prefix = Str.substr(tBadIncrementals.getString(2, i), 0, endStringPosition + 1); // include the dot
			tAPMArgumentTable.setString("RTP Page Prefix", 1, prefix);
			
			// set the dataset type
			int datasetTypeId = tBadIncrementals.getInt(3, i);
			tAPMArgumentTable.setInt("dataset_type_id", 1, datasetTypeId);
			String sDatasetTypeId = m_apmUtils.APM_DatasetTypeIdToName(tAPMArgumentTable, datasetTypeId, false);
			if (Str.len(sDatasetTypeId) < 1)
			   sDatasetTypeId = Str.intToStr(datasetTypeId);

			ConsoleLogging.instance().setDatasetTypeContext(tAPMArgumentTable, sDatasetTypeId, datasetTypeId);
			
			// set the scenario ID
			int scenarioId = tBadIncrementals.getInt(4, i);
			Table tScenarioList = tAPMArgumentTable.getTable("Scenario_List", 1);
			tScenarioList.setInt(1, 1, scenarioId);
			// we don't have access to the scenario name here so just convert the ID to a string
			ConsoleLogging.instance().setScenarioContext(tAPMArgumentTable, Str.intToStr(scenarioId), scenarioId);

			returnValue =  m_apmUtils.APM_ClearMsgLogForBadUpdates(m_apmUtils.cModeBatch, iEntityGroupId, scenarioId, "", tAPMArgumentTable);
			if ( returnValue == 0)
			{
				m_apmUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Failed to clear bad updates for Entity Group (portfolio/service provider):" + iEntityGroupId +
																						", Dataset type: " + datasetTypeId + ", Scenario: " + scenarioId);
				m_apmUtils.APM_PrintMessage(tAPMArgumentTable, "Continuing.....");
				iRetVal = 0;
			}
		}

		ConsoleLogging.instance().unSetDatasetTypeContext(tAPMArgumentTable);
		ConsoleLogging.instance().unSetScenarioContext(tAPMArgumentTable);
		
		return iRetVal;
	}
	
}
