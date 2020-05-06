/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;

import com.olf.openjvs.ODateTime;
import com.olf.openjvs.Afs;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Debug;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.OCalendar;

public class APM_Batch_Failure_Sweeper implements IScript 
{
	// date time from which to pick up entries to rerun
	private int m_PriorDayOffset = 0; // number of days prior to today retrieve failed batches, 0 (today) is default;
	private int m_StartTime = 0; // midnight

	public void execute(IContainerContext context) throws OException 
	{
		int numFailures;
		int failRow;
		Table failedEntityGroupArgs;
		int entityGroupId =-1;
		int firstColon;
		int secondColon;
		String serviceName;
		String afsFileName;
		String entityGroupName="";
		String entityType="";

		OConsole.oprint("!!=========================================================================!!\n");
		OConsole.oprint("Starting APM Batch Sweeper to rerun failed APM batches.\n");
		OConsole.oprint("!!=========================================================================!!\n");
		
		// load failed service arg tables
		failedEntityGroupArgs = Table.tableNew();
		if (RetrieveFailedEntityGroupArgs(failedEntityGroupArgs) == false) {
			OConsole.oprint("Unable to load from abacus_file_system. Exiting.\n");
			failedEntityGroupArgs.destroy();
			Util.exitFail();
		}
			
		// check there is something to re-run
		numFailures = failedEntityGroupArgs.getNumRows();
		if( numFailures < 1) {
			OConsole.oprint("No failed entity groups (service provider or portfolios) found. Exiting.\n");
			failedEntityGroupArgs.destroy();
			Util.exitSucceed();
		}

		// Set up the table for population
		Table failedKeys = Table.tableNew();;
		failedKeys.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
		failedKeys.addCol("simulation_name", COL_TYPE_ENUM.COL_STRING);
		failedKeys.addCol("package_name", COL_TYPE_ENUM.COL_STRING);
		failedKeys.addCol("dataset_type_name", COL_TYPE_ENUM.COL_STRING);
		failedKeys.addCol("entity_type", COL_TYPE_ENUM.COL_STRING);
		failedKeys.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
		failedKeys.addNumRows(numFailures);

		// now execute the rerun - this may or may not fail (the rerun) but not a lot we can do
		String script_name = find_script_path("APM_Rerun_DataSetKey", false);
		int iRetVal = 1;
		
		// re-run each failure
		for (failRow =1; failRow <= numFailures;failRow++) {
			afsFileName = failedEntityGroupArgs.getString("filename",failRow);

			// extract the service name and entity type & group id from the afs file name
			firstColon = Str.findSubString(afsFileName, ":");
			secondColon = Str.findLastSubString(afsFileName, ":");
			int thirdSeparator = Str.findLastSubString(afsFileName, "-");
			serviceName = Str.substr (afsFileName, firstColon+1, secondColon-firstColon-1);
			entityType = Str.substr(afsFileName, secondColon+1, afsFileName.length()-secondColon-1);
			entityGroupId = Str.strToInt(Str.substr(afsFileName, thirdSeparator+1, afsFileName.length()-thirdSeparator-1));

			// delete the entry from the afs table to note we've attempted a re-run
			Afs.deleteTable(afsFileName,1); 
			
			failedKeys.setString("service_name", failRow, serviceName);
			failedKeys.setString("simulation_name", failRow, "");
			failedKeys.setString("package_name", failRow, "");
			failedKeys.setString("dataset_type_name", failRow, "");
			failedKeys.setString("entity_type", failRow, entityType);
			failedKeys.setInt("entity_group_id", failRow, entityGroupId);

			if ( entityType.equals("DEAL") )
			{
			   entityGroupName = Table.formatRefInt(entityGroupId, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
			   OConsole.oprint("Found failure for APM Deal Service " + serviceName + " portfolio " + entityGroupName +"\n");		
			}
			else if ( entityType.equals("NOMINATION") )
			{
			   entityGroupName = Table.formatRefInt(entityGroupId, SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);
			   OConsole.oprint("Found failure for APM Nomination Service " + serviceName + " pipeline " + entityGroupName +"\n");		
			}
			else
			{
			   OConsole.oprint("Error: Unable to support found entity type "+entityType + "\n");
			   iRetVal = 0;
			}
		}
		
		try
		{
		   if ( iRetVal != 0 )		
		      iRetVal = Util.runScript(script_name, failedKeys, Util.NULL_TABLE);
		} catch(Exception t) {
		   iRetVal = 0;
		   OConsole.oprint("Failed to run script: " + script_name);
		}

		failedKeys.destroy();
		failedEntityGroupArgs.destroy();

		if ( iRetVal != 1 )
			Util.exitFail();

		Util.exitSucceed();		
	}
	

	/////////////////////////////////////////////////////
	//
	// LoadArgt
	//
	// Load the APM Service batch argt
	//
	///////////////////////////////////////////////////////
	boolean LoadArgt(String serviceName, int entityGroup, Table serviceArgt) throws OException {

		// get filename to load
		String afsFileName = "APMFailedBatchArgt:"+serviceName+":"+entityGroup;
		OConsole.oprint("Loading APM Service argument table for " + serviceName +" from AFS file "+afsFileName + "\n");		
	
	  // load APM Service argt from AFS file
		if (Afs.retrieveTable(serviceArgt, afsFileName) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
	   {
			OConsole.oprint("Warning: Unable to load APM Service argument table from AFS file "+afsFileName + "\n");
			return false;
	   }
		
		return true;
	}
	

	/////////////////////////////////////////////////////
	//
	// RetrieveFailedEntityGroupArgs
	//
	// Load the APM Service batch argts from AFS
	//
	///////////////////////////////////////////////////////
	boolean RetrieveFailedEntityGroupArgs(Table failedEntityGroupArgs) throws OException {
		
		int iRetVal;
		if (Table.isTableValid(failedEntityGroupArgs) < 1) {
			OConsole.oprint("Error: invalid table passed to RetrieveFailedEntityGroupArgs\n");
			return false;
		}

		// only purge entries after the defined start date/time params
		int m_StartDate = OCalendar.getServerDate() - m_PriorDayOffset; //today		
		ODateTime dPurgeDateTime = ODateTime.dtNew();
		dPurgeDateTime.setDate(m_StartDate);
		dPurgeDateTime.setTime(m_StartTime);
		
		String sqlWhereStr = "filename like '%APMFailedBatchArgt%' and last_update > '" + dPurgeDateTime.formatForDbAccess() + "'";
		iRetVal = UTIL_TABLE_LoadFromDbWithSQL(failedEntityGroupArgs, "filename", "abacus_file_System", sqlWhereStr);

		return (iRetVal == 1) ? true : false;
	}
	

	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// UTIL_TABLE_LoadFromDbWithSQL
	//
	// LoadFromDBWithSQL with retries - same as APM_TABLE_LoadFromDbWithSQL but without depends on apm generic argument table
	//
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public int UTIL_TABLE_LoadFromDbWithSQL(Table table, String what, String from, String where) throws OException {
		int iRetVal;
		int iAttempt;
		int nAttempts = 10;

		iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		for (iAttempt = 0; (iAttempt == 0) || ((iRetVal == DB_RETURN_CODE.SYB_RETURN_DB_RETRYABLE_ERROR.toInt()) && (iAttempt < nAttempts)); ++iAttempt) {
			if (iAttempt > 0)
				Debug.sleep(iAttempt * 1000);

			iRetVal = DBaseTable.loadFromDbWithSQL(table, what, from, where);
		}

		if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
			OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.loadFromDbWithSQL failed "));

		return iRetVal;
	}
	
	/// Find the path to a given script...
	public String find_script_path(String script_name, boolean destroyCache) throws OException {
		String script_path = "";

		Table DirTable;
		int NodeID, row, scriptCount, iRetVal;

		// node ID structure never actually changes so we can cache this call

		DirTable = Table.getCachedTable("dir_table");
		if (Table.isTableValid(DirTable) == 0 && destroyCache)
			Table.destroyCachedTable("dir_table");
			
		if (Table.isTableValid(DirTable) == 0) {
			DirTable = Table.tableNew("dir_table");
			iRetVal = DBaseTable.loadFromDbWithSQL(DirTable, "node_id, node_name, node_type, parent_node_id", "dir_node", "node_type in (6, 7, 8)");
			if (iRetVal != 0)
			{
				Table.cacheTable("dir_table", DirTable);
				DirTable.sortCol("node_name");
			}
		}
		
		int firstRow = DirTable.findString("node_name", script_name, SEARCH_ENUM.FIRST_IN_GROUP);
		int lastRow = DirTable.findString("node_name", script_name, SEARCH_ENUM.LAST_IN_GROUP);
		
		NodeID = 0;
		scriptCount = 0;
		if ( firstRow > 0 )
		{
			for ( row = firstRow; row <= lastRow; row++)
			{
				if ( DirTable.getInt(3, row) == 7 )  // openjvs script
				{
					scriptCount++;
					if ( scriptCount == 1 )
					   NodeID = DirTable.getInt(1, row);
				}
			}
		
			if (NodeID > 0) 
			{
				if ( scriptCount > 1) 
				{
					OConsole.oprint("WARNING: There is more than one JVS script name \"" + script_name + "\". Retriving the path for the first one");
					Util.scriptPostStatus("WARNING: There is more than one JVS script name \"" + script_name + "\". Retriving the path for the first one");
				}
				script_path = find_path("node_id", NodeID, DirTable);
			} 
			else 
			{
				OConsole.oprint("ERROR: Cannnot find a JVS script name:" + script_name);
				Util.scriptPostStatus("ERROR: Cannnot find a JVS script name:" + script_name);
			}
		}
		
		return script_path;
	}
	
	String find_path(String col, int node_id, Table tbl_dir) throws OException {
		int row, parent_node_id;
		String node_name, full_path;

		row = tbl_dir.unsortedFindInt(col, node_id);
		parent_node_id = tbl_dir.getInt("parent_node_id", row);
		node_name = tbl_dir.getString("node_name", row);

		if (parent_node_id == 0)
			return "/" + node_name;
		else
			full_path = find_path("node_id", parent_node_id, tbl_dir);

		return full_path + "/" + node_name;
	}		
}
	

