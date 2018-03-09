/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;

import standard.include.APM_Utils;
import standard.include.ConsoleLogging;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class APM_SimResultSetup
{
	private APM_Utils m_APMUtils;

	// APM_PerformOperation - sim parameters: row identifier enum
	private int APM_ROW_VERSION_ID = 1;
	private int APM_ROW_SIM_DEF_ID = 2;
	private int APM_ROW_USER_PERMS = 3;
	private int APM_ROW_GROUP_PERMS = 4;
	private int APM_ROW_PUBLIC_PERMS = 5;
	
   public APM_SimResultSetup() {
		m_APMUtils = new APM_Utils();
   }

	private int APM_GetMinUDSRResultId(Table tAPMArgumentTable) throws OException {
		Table tUserSimResults;
		int iMinUserSimId;
		int iRetVal;

		/*
		 * Min User Id Check
		 */
		iMinUserSimId = 20001; // Default

		tUserSimResults = m_APMUtils.APM_CacheTableGet("APM_min_user_result_id", tAPMArgumentTable);
		if (Table.isTableValid(tUserSimResults) == 0) {
			/* get the base Id of user defined simulation results */
			tUserSimResults = Table.tableNew("User Sim Results");
			iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tUserSimResults, "min(result_type)", "pfolio_result_detail", "result_type > 0");
			if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
				m_APMUtils.APM_CacheTableAdd("APM_min_user_result_id", "TFE.METADATA.CHANGED", tUserSimResults.copyTable(), tAPMArgumentTable);
			} else {
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.loadFromDbWithSQL failed "));
			}
		}

		if (tUserSimResults.getNumRows() > 0)
			iMinUserSimId = tUserSimResults.getInt(1, 1);

		return iMinUserSimId;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_GetSimDef

	Description:   Get the sim def specified by the APM service.
	           Remove any non-UDSRs specified in the scenarios.
	           We leave UDSRs since they may be used by a scenario to massage
	           other results

	-------------------------------------------------------------------------------*/
	public int APM_GetSimDef(int iMode, String sSimName, Table tResList, Table tAPMArgumentTable) throws OException {
		Table tSimDefCache;
		Table tAPMSims;
		Table tScenarios;
		Table tOldSrcResultList;
		Table tNewSrcResultList;
		int iScenario;
		int iRetVal = 1;
		int iSimId = -1;
		int iChkSimId;
		int iGlobalSimCnt = 0;
		int iRows;
		int iLoop;
		int iMinUserSimId;
		String sScenarioName;
		String sServiceName;
		String sCacheTableName;

		/* key by sim name & service name as otherwise different services running the same sim but different pkgs */
		/* will run the wrong sim */
		sServiceName = tAPMArgumentTable.getString("service_name", 1);      
		sCacheTableName = sSimName + "_" + sServiceName;

		/*
		 * Load the global simulation definition - cache for updates,
		 * refresh on batch If it doesn't exist, create one containing only the
		 * base scenario.
		 */
		tSimDefCache = Table.getCachedTable(sCacheTableName);
		if (tSimDefCache == Util.NULL_TABLE || iMode == m_APMUtils.cModeBatch || Table.isTableValid(tSimDefCache) == 0) {
			tAPMSims = Table.tableNew("APM Sims");

				/*
				 * When a user saves a new sim
				 * def, an entry will be created within the specified directory
				 * (re: DIR_NODE) To define the sim def as global, the user must
				 * select the public read permission (OL permissions replicate
				 * unix permissions).
				 * 
				 * NOTE: If more than 1 global sim def exists or all the defs
				 * have been defined as non global, issue an error and exit
				 */
				iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tAPMSims, "sim_def_id", "sim_def", "name = '" + sSimName + "'");
				if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.loadFromDbWithSQL failed "));
					tAPMSims.destroy();
					return 0;
				}

				if ((iRows = tAPMSims.getNumRows()) > 0) {
					/*
					 * As it is possible to have the same def residing in
					 * different directories, we must ensure only 1 global
					 * defintion exists with that name.
					 */
					for (iLoop = 1; iLoop <= iRows; iLoop++) {
						iChkSimId = tAPMSims.getInt(1, iLoop);
						if (APM_isGlobalSimDef(iMode, iChkSimId, tAPMArgumentTable) != 0) {
							iGlobalSimCnt++;
							if (iChkSimId > iSimId) // make sure the latest one
								// is taken if more than 1
								iSimId = iChkSimId;
						}
					}
					if (iGlobalSimCnt == 0) {
						m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Unable to run Simulation " + sSimName + " - it is not defined as Global. "
								+ "This can be changed by granting read access to all users.\n");
						tAPMSims.destroy();
						return 0;
					} else if (iGlobalSimCnt > 1) {
						m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Running latest version of Simulation " + sSimName
								+ " - more that 1 global definition exists with this name.\n");
					}
				} else {
					m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "No global simulation was found, defaulting to base scenario.");
					iSimId = -1;
				}

			tAPMSims.destroy();

			tSimDefCache = Sim.loadSimulation(iSimId);
			if (Table.isTableValid(tSimDefCache) == 0) {
				tSimDefCache = Sim.createSimDefTable();
				Sim.addSimulation(tSimDefCache, sSimName);
				Sim.addScenario(tSimDefCache, sSimName, "Base");
			}

			iMinUserSimId = APM_GetMinUDSRResultId(tAPMArgumentTable);
			tScenarios = tSimDefCache.getTable("scenario_def", 1);
			for (iScenario = 1; iScenario <= tScenarios.getNumRows(); iScenario++) {
				sScenarioName = tScenarios.getString("scenario_name", iScenario);
				// remove non-user defined simulation results from default sim
				// setup
				tOldSrcResultList = tScenarios.getTable("scenario_result_list", iScenario);

				if (Table.isTableValid(tOldSrcResultList) == 1) {
					tNewSrcResultList = tOldSrcResultList.cloneTable();
					tNewSrcResultList.select(tOldSrcResultList, "*", "id GE " + Str.intToStr(iMinUserSimId));
					tOldSrcResultList.destroy();
					tScenarios.setTable("scenario_result_list", iScenario, tNewSrcResultList);
				}
				// Add all the results specified by the packages ...
				Sim.addResultListToScenario(tSimDefCache, sSimName, sScenarioName, tResList.copyTable());
			}
			tOldSrcResultList = Util.NULL_TABLE;
			tNewSrcResultList = Util.NULL_TABLE;
			tScenarios = Util.NULL_TABLE;

			Table.cacheTable(sCacheTableName, tSimDefCache);
		}

		tAPMArgumentTable.setTable("Sim Def", 1, tSimDefCache.copyTable());
		tSimDefCache = Util.NULL_TABLE;
		return 1;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_GetScenarioList

	Description:   Get a list of scenarios for later use by the GUI for the scenario 
	           filter drop down

	-------------------------------------------------------------------------------*/
	public int APM_GetScenarioList(int iMode, Table tAPMArgumentTable) throws OException {
		Table tSimDefCache;
		Table tScenarios;
		Table tScenarioList = Table.tableNew("scenario_list");
		Table tArgs;
		Table tScenarioListResult;
		int iScenario;
		int iScenarioId = 0;
		String sScenarioName;
		int iRetVal = 1;
		boolean publishFilterRefresh = false;

		tSimDefCache = tAPMArgumentTable.getTable("Sim Def", 1);
		tScenarios = tSimDefCache.getTable("scenario_def", 1);

		// ---------------- create and populate list of scenarios with ids for processing
		tScenarioList.addCol("scenario_id", COL_TYPE_ENUM.COL_INT);
		tScenarioList.addCol("scenario_name", COL_TYPE_ENUM.COL_STRING);
		tScenarioList.addNumRows(tScenarios.getNumRows());

		// Check the current apm_scenario_list table to see if we need to add a new scenario.
		Table tCurrentScenarios = Table.tableNew("");     
		iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL( tAPMArgumentTable, tCurrentScenarios, "*", "apm_scenario_list", "1=1" );
		   
		for (iScenario = 1; iScenario <= tScenarios.getNumRows(); iScenario++)
		{
			int iRow = tCurrentScenarios.unsortedFindString("scenario_name", tScenarios.getString("scenario_name", iScenario), SEARCH_CASE_ENUM.CASE_SENSITIVE );		      
			if ( iRow < 0 )
			{
				publishFilterRefresh = true;
				break;
			}   
		}
		tCurrentScenarios.destroy();
		
		// build table for stored proc
		tArgs = Table.tableNew("params");
		tArgs.addCol("scenario_name", COL_TYPE_ENUM.COL_STRING);
		tArgs.addRow();

		// if a scenario for the simulation is not saved in the user table add
		// it and assign an ID
		for (iScenario = 1; iScenario <= tScenarios.getNumRows(); iScenario++) {
			sScenarioName = tScenarios.getString("scenario_name", iScenario);
			// Create the function parameters and run the the stored proc
			tArgs.setString(1, 1, sScenarioName);

			ConsoleLogging.instance().setScenarioContext(tAPMArgumentTable, sScenarioName, iScenario);
			
			// call proc to get scenario id from apm_scenario_list, will
			// insert into table and assign id
			// if not found
			iRetVal = m_APMUtils.APM_DBASE_RunProc(tAPMArgumentTable, "USER_apm_update_scenariolist", tArgs);
			if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Unable to execute USER_apm_update_scenariolist");
			} else {
				tScenarioListResult = Table.tableNew("scenario_list_results");
				iRetVal = DBase.createTableOfQueryResults(tScenarioListResult);
				if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Unable to retrieve results of USER_apm_update_scenariolist");
				} else {
					if (tScenarioListResult.getNumRows() > 0) {
						iScenarioId = tScenarioListResult.getInt("scenario_id", 1);
					} else {
						m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "No rows returned from USER_apm_update_scenariolist");
						iRetVal = 0;
					}
				}
				tScenarioListResult.destroy();
			}

			if (iRetVal != 0) {
				// save the scenario id and name
				tScenarioList.setInt("scenario_id", iScenario, iScenarioId);
				tScenarioList.setString("scenario_name", iScenario, sScenarioName);
			} else {
				break;
			}
		}

		ConsoleLogging.instance().unSetScenarioContext(tAPMArgumentTable);

		if (iRetVal != 0) {
			// save the scenario list in argt
			tScenarioList.sortCol("scenario_name");
			tAPMArgumentTable.setTable("Scenario_List", 1, tScenarioList);
		} else {
			tScenarioList.destroy();
		}

		if ( publishFilterRefresh )
		{
			// Create and send a table containing a user table changed message.		    
			Table tMessage = Table.tableNew("table_changed");
			Table tPayload = Table.tableNew("payload");
		    tMessage.addCol("payload", COL_TYPE_ENUM.COL_TABLE);
		    tMessage.addRow();
		    tMessage.setTable("payload", 1, tPayload);		    
		    tPayload.addCol("table_name", COL_TYPE_ENUM.COL_STRING);
		    tPayload.addRow();
		    tPayload.setString(1, 1, "apm_scenario_list");			
		    m_APMUtils.APM_PublishTable(tAPMArgumentTable, "TFE.table_changed", tMessage, -1, -1, -1);
		    tMessage.destroy();
		}
		
		tArgs.destroy();
		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:            APM_isGlobalSimDef
	Description:     Check if the simulation definition is defined as global -
	             a global defintion is defined as one which has the public
	             read permission enabled. 
	Parameters:      iSimId: Simultaion Defintion Id
	             tAPMArgumentTable: Script Arguments table
	Return Values:   1  = Global
	             0 = NOT Global
	-------------------------------------------------------------------------------*/
	private int APM_isGlobalSimDef(int iMode, int iSimId, Table tAPMArgumentTable) throws OException {
		XString xsError = null;
		Table tParams, tSimDef, tSimPerms;
		int iPermGranted = 0;
		int iRetVal = 0;

		/*
		 * Two columns will be defined for the paramter table; 1 => parameter
		 * name (String) 2 => parameter value (table)
		 * 
		 * Two rows will added to the parameter table; 1 => version ID of the
		 * TFE intferace API in core code 2 => sim def id 3 => table containing
		 * user read, write, execute check flags 4 => table containing group
		 * read, write, execute check flags 5 => table containing public read,
		 * write, execute check flags
		 * 
		 */
		tParams = Table.tableNew("APM Parameters");
		if (Apm.performOperation(m_APMUtils.APM_CHECK_SIM_PERMISSIONS, 1, tParams, xsError) == 0) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to initialise parameters for Simulation Permissions Check: " + Str.xstringGetString(xsError) + "\n");
			tParams.destroy();
			return 0;
		}

		/*
		 * Check if we have PUBLIC read permissions for the specified sim def.
		 */
		tSimDef = tParams.getTable("parameter_value", APM_ROW_SIM_DEF_ID);
		tSimDef.setInt("sim_def_id", 1, iSimId);

		tSimPerms = tParams.getTable("parameter_value", APM_ROW_PUBLIC_PERMS);
		tSimPerms.setInt("check_read", 1, 1);

		iPermGranted = Apm.performOperation(m_APMUtils.APM_CHECK_SIM_PERMISSIONS, 0, tParams, xsError);
		if (Str.isNotEmpty(Str.xstringGetString(xsError)) == 1) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to check simulation definition permissions: " + Str.xstringGetString(xsError) + "\n");
			tParams.destroy();
			return 0;
		}

		Str.xstringDestroy(xsError);

		tParams.destroy();

		iRetVal = iPermGranted;
		return iRetVal;
	}
  
	/*-------------------------------------------------------------------------------
	Name:          APM_GetPackageSimResults
	Description:   Get the list of sim results to run for this package. This will 
	           depend on which columns and filters are on/off
	Parameters:
	Return Values:   Table 
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	private Table APM_GetPackageSimResults(int iMode, Table tAPMArgumentTable, String sPackageName, Table tPackageDataTables) throws OException {
		String sResultColDepName;
		Table tArgs;
		Table tCachedColDepResults;
		int iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		Table tPackageDataTableCols;
		String sDataTableName;
		int iTable;

		sResultColDepName = "APM_" + sPackageName + "_ResultColumnDependencies";

		tCachedColDepResults = m_APMUtils.APM_CacheTableGet(sResultColDepName, tAPMArgumentTable);
		if (Table.isTableValid(tCachedColDepResults) == 0) {
			tCachedColDepResults = Table.tableNew(sResultColDepName);
			for (iTable = 1; iTable <= tPackageDataTables.getNumRows(); iTable++) {
				sDataTableName = tPackageDataTables.getTable(1, iTable).getTableName();

				tPackageDataTableCols = m_APMUtils.APM_GetColumnsForTable(iMode, tAPMArgumentTable, sPackageName,  sDataTableName);
				if (tPackageDataTableCols == null)
					iRetVal = 0; // message already logged
				else if (tPackageDataTableCols.getNumRows() < 1) {
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "APM_GetPackageSimResults ... no data table cols enabled for package : " + sPackageName
							+ ", table name : " + sDataTableName);
					iRetVal = 0;
				} else {
					tCachedColDepResults.select(tPackageDataTableCols, "result_enum_name(result_dependency)", "column_type GE 0");
				}
				
				if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
					break;
			}

			if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
				tCachedColDepResults.destroy();
				return Util.NULL_TABLE;
			}
			tCachedColDepResults.makeTableUnique();
			m_APMUtils.APM_CacheTableAdd(sResultColDepName, "TFE.METADATA.CHANGED", tCachedColDepResults.copyTable(), tAPMArgumentTable);
		}

		return (tCachedColDepResults);
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_GetAllPackageSimResults

	Description:   This function sets up the RevalParam in 'argt' to include the results

	Parameters:    iMode - m_APMUtils.cModeBatch, m_APMUtils.cModeApply, m_APMUtils.cModeBackout

	Returns:       0 for FAIL, 1 for SUCCESS
	-------------------------------------------------------------------------------*/
	public Table APM_GetAllPackageSimResults(int iMode, Table tAPMArgumentTable) throws OException {
		Table tResList;
		Table tResults;
		Table tPackageDataTables;
		String sPackageName;
		String strResultAsString;
		int iRow, iResultCount;
		int iResultAsEnum;
		int iRetVal = 1;
		Table tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);

		tResList = Sim.createResultListForSim();
		if (Table.isTableValid(tPackageDetails) != 0) {
			for (iRow = 1; (iRetVal == 1 && (iRow <= tPackageDetails.getNumRows())); iRow++) {
				sPackageName = tPackageDetails.getString("package_name", iRow);

				ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);
				tPackageDataTables = tPackageDetails.getTable("package_data_tables", iRow);
				tResults = APM_GetPackageSimResults(iMode, tAPMArgumentTable, sPackageName, tPackageDataTables);

				if (Table.isTableValid(tResults) == 0) {
					iRetVal = 0;
					break;
				}

				for (iResultCount = 1; iResultCount <= tResults.getNumRows(); iResultCount++) {
					strResultAsString = tResults.getString("result_dependency", iResultCount);

					try
					{
						iResultAsEnum = SimResult.getResultIdFromEnum(strResultAsString);
					}
					catch(OException ex)
					{
						m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Exception in function APM_GetAllPackageSimResults: " + ex);
						String message = m_APMUtils.getStackTrace(ex);
						m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message+ "\n");										
						m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to find ID for simulation result " + strResultAsString + " for package " + sPackageName + ", in the Simulation Result configuration.  Check it exists in Admin Mgr.");
						iRetVal = 0;
						break;						
					}
					
					/* Add in Sim results dynamically found */
					SimResult.addResultForSim(tResList, SimResultType.create(SimResult.getResultEnumFromId(iResultAsEnum)));
				}
				tResults.destroy();
			}

			ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
		}

		if (iRetVal == 0) {
			tResList.destroy();
			return (Util.NULL_TABLE );
		}

		return tResList;
	}
	
}
