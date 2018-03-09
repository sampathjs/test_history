/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;

import standard.include.APM_Utils;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.Debug;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.fnd.ServicesBase;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public class APM_Rerun_DataSetKey implements IScript 
{	
	public void execute(IContainerContext context) throws OException 
	{				
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();
		if(Table.isTableValid(returnt) == 0) returnt = Table.tableNew(); 

		// check the columns exist
		int serviceColNo = argt.getColNum("service_name");
		int simulationColNo = argt.getColNum("simulation_name");
		int packageColNo = argt.getColNum("package_name");
		int datasetColNo = argt.getColNum("dataset_type_name");
		int entityTypeColNo = argt.getColNum("entity_type");
		int entityGroupIdColNo = argt.getColNum("entity_group_id");

		// validation checks 
		if ( serviceColNo < 1 )
		{
		   OConsole.oprint("No service column. Exiting.\n");
		   Util.exitFail();
		}

		if ( argt.getNumRows() < 1 )
		{
		   OConsole.oprint("No rows in argt. Exiting.\n");
		   Util.exitFail();
		}

		if ( simulationColNo < 1 && packageColNo < 1 && datasetColNo < 1 && entityGroupIdColNo < 1 && entityTypeColNo < 1)
		{
		   OConsole.oprint("No key columns in argt. Exiting.\n");
		   Util.exitFail();
		}

		// generate the packages lookup list
		Table tAPMPackages = Table.tableNew("APM_Packages");;
		UTIL_TABLE_LoadFromDbWithSQL(tAPMPackages, "apm_package_id,apm_package_name", "apm_package_names", "apm_package_id > 0");
		tAPMPackages.sortCol(2);

		// generate the datasets lookup list
		Table tAPMDatasets = Table.tableNew("APM_Datasets");
		UTIL_TABLE_LoadFromDbWithSQL(tAPMDatasets, "apm_dataset_id,apm_dataset_name", "apm_dataset_type", "apm_dataset_id > -1");
		tAPMDatasets.sortCol(2);

		// now sort by service name
		argt.addGroupBy(serviceColNo);
		argt.addGroupBy(simulationColNo);
		argt.addGroupBy(datasetColNo);
		argt.addGroupBy(packageColNo);
		argt.addGroupBy(entityTypeColNo);
		argt.addGroupBy(entityGroupIdColNo);
		argt.groupBy();

		String currentService = argt.getString(serviceColNo, 1);
		String currentSimulation = argt.getString(simulationColNo, 1);
		String currentPackage = argt.getString(packageColNo, 1);
		String currentDataset = argt.getString(datasetColNo, 1);
		String currentEntityType = argt.getString(entityTypeColNo, 1);
		int currentEntityGroupId = argt.getInt(entityGroupIdColNo, 1);

		// set up the lookup list tables
		Table entityGroupTable = Table.tableNew();
		entityGroupTable.addCol("value", COL_TYPE_ENUM.COL_STRING);
		entityGroupTable.addCol("id", COL_TYPE_ENUM.COL_INT);

		Table packageTable = Table.tableNew();
		packageTable.addCol("value", COL_TYPE_ENUM.COL_STRING);
		packageTable.addCol("id", COL_TYPE_ENUM.COL_INT);

		Table datasetTable = Table.tableNew();
		datasetTable.addCol("value", COL_TYPE_ENUM.COL_STRING);
		datasetTable.addCol("id", COL_TYPE_ENUM.COL_INT);

		// now loop around to get the params
		for (int i = 1; i <= argt.getNumRows(); i++)
		{
		   String serviceID = argt.getString(serviceColNo, i);
		   String simulation = argt.getString(simulationColNo, i);
		   String packageName = argt.getString(packageColNo, i);
		   String datasetName = argt.getString(datasetColNo, i);
		   String entityType = argt.getString(entityTypeColNo, i);
		   int entityGroupID = argt.getInt(entityGroupIdColNo, i);

		   int packageID = tAPMPackages.findString(2, packageName, com.olf.openjvs.enums.SEARCH_ENUM.FIRST_IN_GROUP);
		   int dataset = tAPMDatasets.findString(2, datasetName, com.olf.openjvs.enums.SEARCH_ENUM.FIRST_IN_GROUP);
		   
		   // now get the equivalents
		   String entityGroupName = "";
		   if ( entityType.equals("DEAL") )
		   {
		      entityGroupName = Table.formatRefInt(entityGroupID, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
		      OConsole.oprint("Found Entry. Deal Service: " + serviceID + ", Simulation: " + simulation + ", Package:" + packageName + ", Dataset: " + datasetName + ", Portfolio: " + entityGroupName + "\n");
		   }
		   else if ( entityType.equals("NOMINATION") )
		   {
		      entityGroupName = Table.formatRefInt(entityGroupID, SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);
		      OConsole.oprint("Found Entry. Nomination Service: " + serviceID + ", Simulation: " + simulation + ", Package:" + packageName + ", Dataset: " + datasetName + ", Pipeline: " + entityGroupName + "\n");
		   }
		   else
		   {
		      OConsole.oprint("Entity type is invalid !! Skipping: " + entityType + "\n");
		      continue;
		   }
		   
		   if ( (packageName.length() > 0 && packageID < 0) || (datasetName.length() > 0 && dataset < 0) || entityGroupID <= 0 )
		   {
		      OConsole.oprint("Entry is invalid !! Skipping\n");
		      continue;
		   }

		   if ( serviceID.equals(currentService) == false || simulation.equals(currentSimulation) == false || datasetName.equals(currentDataset) == false )
		   {
		      // unique elements have changed in key - now run the batch & then clear the params
		      runAPMServiceAndCleanUp(currentService, currentSimulation, entityGroupTable, packageTable, datasetTable);
		   }

		   if ( entityGroupID != currentEntityGroupId || entityGroupTable.getNumRows() == 0 )
		   {
		      OConsole.oprint("Adding Entity group to list to rerun: " + entityGroupName + "\n");
		      int row = entityGroupTable.addRow();		   
		      entityGroupTable.setString(1, row, entityGroupName);
		      entityGroupTable.setInt(2, row, entityGroupID);
		   }

		   if ( (packageName.equals(currentPackage) == false || packageTable.getNumRows() == 0) && packageName.length() > 0)
		   {
		      OConsole.oprint("Adding package to list to rerun: " + packageName + "\n");
		      int row = packageTable.addRow();		   
		      packageTable.setString(1, row, packageName);
		      packageTable.setInt(2, row, packageID);
		   }

		   if ( (datasetName.equals(currentDataset) == false || datasetTable.getNumRows() == 0) && datasetName.length() > 0 )
		   {
		      OConsole.oprint("Adding dataset to list to rerun: " + datasetName + "\n");
		      int row = datasetTable.addRow();		   
		      datasetTable.setString(1, row, datasetName);
		      datasetTable.setInt(2, row, dataset);
		   }

		   currentService = argt.getString(serviceColNo, i);
		   currentSimulation = argt.getString(simulationColNo, i);
		   currentPackage = argt.getString(packageColNo, i);
		   currentDataset = argt.getString(datasetColNo, i);
		   currentEntityGroupId = argt.getInt(entityGroupIdColNo, i);
		}		

		// there will be some stuff left in the list to run
		runAPMServiceAndCleanUp(currentService, currentSimulation, entityGroupTable, packageTable, datasetTable);

		// cleanup
		tAPMDatasets.destroy();
		tAPMPackages.destroy();
		entityGroupTable.destroy();
		packageTable.destroy();
		datasetTable.destroy();
	}

	private int runAPMServiceAndCleanUp(String currentService, String currentSimulation, Table entityGroupTable, Table packageTable, Table datasetTable) throws OException 
	{
		int iRetVal = 1;
		
		try
		{
			Table userPfieldTable = Table.tableNew();
			userPfieldTable.addCol("simulation_name", COL_TYPE_ENUM.COL_STRING);
			userPfieldTable.addCol("internal_portfolio", COL_TYPE_ENUM.COL_TABLE);
			userPfieldTable.addCol("package_name", COL_TYPE_ENUM.COL_TABLE);
			userPfieldTable.addCol("dataset_type_id", COL_TYPE_ENUM.COL_TABLE);
			userPfieldTable.addRow();
			userPfieldTable.setString(1, 1, currentSimulation );
			
			if (entityGroupTable.getNumRows() > 0  )
				userPfieldTable.setTable(2, 1, entityGroupTable.copyTable() );

			if (packageTable.getNumRows() > 0  )
				userPfieldTable.setTable(3, 1, packageTable.copyTable() );

			if (datasetTable.getNumRows() > 0  )
				userPfieldTable.setTable(4, 1, datasetTable.copyTable() );
	
			// set up the user params table
			Table pfield_table = ServicesBase.getServiceMethodProperties(currentService, "ApmService");
			pfield_table.addCol("user_pfield_table", COL_TYPE_ENUM.COL_TABLE);
			pfield_table.setTable("user_pfield_table", 1, userPfieldTable);
	
			Table methodParams = Table.tableNew();
			Table param_tbl = ServicesBase.serviceCreateMethodParamsTable();
			param_tbl.setTable("user_pfield_table", 1, pfield_table);
			param_tbl.setTable("method_params", 1, methodParams);
	
			Table return_tbl = Table.tableNew();
	
			OConsole.oprint("Running APM Service: " + currentService + "\n");
			ServicesBase.serviceRunMethod(currentService, "ApmService", param_tbl, return_tbl);

			// now clean up the tables
			entityGroupTable.clearRows();
			packageTable.clearRows();
			datasetTable.clearRows();
			
			param_tbl.destroy();
			return_tbl.destroy();
		}
		catch(Exception t) 
		{
		   iRetVal = 0;
			OConsole.oprint("Failed to run APM Service: " + currentService + "\n");
		}
				
		return iRetVal;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// UTIL_TABLE_LoadFromDbWithSQL
	//
	// LoadFromDBWithSQL with retries - same as APM_TABLE_LoadFromDbWithSQL but without depends on apm generic argument table
	//
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public int UTIL_TABLE_LoadFromDbWithSQL(Table table, String what, String from, String where) throws OException {
        int iRetVal = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

        int numberOfRetriesThusFar = 0;
        do {
        	// for error reporting further down
        	String message = null;
        	
            try {
            	iRetVal = DBaseTable.loadFromDbWithSQL(table, what, from, where);
            } catch (OException exception) {
                iRetVal = exception.getOlfReturnCode().toInt();
                
                message = exception.getMessage();
            } finally {
                if (iRetVal == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt()) {
                    numberOfRetriesThusFar++;
                    
                    if(message == null) {
                        message = String.format("Query execution retry %1$d of %2$d. Check the logs for possible deadlocks.", numberOfRetriesThusFar, APM_Utils.MAX_NUMBER_OF_DB_RETRIES);
                    } else {
                        message = String.format("Query execution retry %1$d of %2$d [%3$s]. Check the logs for possible deadlocks.", numberOfRetriesThusFar, APM_Utils.MAX_NUMBER_OF_DB_RETRIES, message);
                    }
                    
                    OConsole.oprint(message);

                    Debug.sleep(numberOfRetriesThusFar * 1000);
                } else {
                    // not a retryable error, leave
                    break;
                }
            }
        } while (numberOfRetriesThusFar < APM_Utils.MAX_NUMBER_OF_DB_RETRIES);

		if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.loadFromDbWithSQL failed "));

		return iRetVal;
	}	
}
