/* Released with version 05-Feb-2020_V17_0_126 of APM */

package standard.include;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import standard.apm.ADS_GatherServiceStatus;
import standard.apm.ADS_LogStatusMessage;
import standard.apm.statistics.ApmStatisticsLogger;
import standard.apm.statistics.ApmStatisticsLoggerInstantiationException;
import standard.apm.statistics.IApmStatisticsLogger;
import standard.apm.statistics.Scope;
import standard.apm.statistics.logger.LoggingUtilities;


import com.olf.apm.constants.ScriptRunMode;
import com.olf.apm.interfaces.logging.IApmLogger;
import com.olf.openjvs.Afs;
import com.olf.openjvs.Apm;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Debug;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Services;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.XString;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DBTYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.OP_SERVICES_LOG_STATUS;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public class APM_Utils implements IApmLogger, ScriptRunMode {

	// Message levels (only used in calls to APM_Print
	public int cMsgLevelDebug = 1;
	public int cMsgLevelInfo = 2;
	public int cMsgLevelError = 3;

	// Error levels
	public int NO_PACKAGE_COLUMNS_ENABLED = -2000;

	// Status message types ... used for apm_msg_log & ECOM messages to PageServer
	public int cStatusMsgTypeWarn = 1;
	public int cStatusMsgTypeFailure = 2;
	public int cStatusMsgTypeProcessing = 3;
	public int cStatusMsgTypeCompleted = 4; // JIMNOTE : ONLY used to indicate that a batch is complete
	public int cStatusMsgTypeRerun = 5; // JIMNOTE : Used to indicate that a re-run was sucessful
	public int cStatusMsgTypeProcessingAlways = 6; // JIMNOTE : A status message that must always be logged since the batch stats rely on it ..
	public int cStatusMsgTypeStarted = 7; // Batch started
	// At the moment the batch stats look for the following in apm_msg_log :
	// - 'Starting to fill data%' [sent from APM_JobProcessor.mls]
	// - 'Finished updating%' [sent from APM_UpdateTables.mls]
	// - 'Finished filling data%' [sent from APM_JobProcessor.mls]
	// - 'Starting to update%' [sent from APM_UpdateTables.mls]

	// the number of retries to attempt before giving up when querying the database
	// we wrap database calls when they hit vulnerable tables (those subject to
	// rapid change), e.g. ab_tran
	public static final int MAX_NUMBER_OF_DB_RETRIES = 10;

	// The mode types for the APM script fn
	public static int APM_RTPE_CLEAR = 0; // Old APM_PerformOperation ... still used to retrieve the tfe_interface_api version number though
	public int APM_RTPE_BATCH = 1; // Old APM_PerformOperation ... no longer used
	public int APM_RTPE_AMENDMENT = 2; // Old APM_PerformOperation ... no longer used
	public int APM_AVS_SCRIPTCONTEXT = 3;
	public int APM_POSTPROCESS_CLAIM = 4;
	public int APM_BATCH_PARALLEL_UPDATE = 5;
	public int APM_BATCH_PRE_UPDATE = 6;
	public int APM_BATCH_POST_UPDATE = 7;
	public int APM_GET_NUM_SPLIT_ENGINES = 8;
	public int APM_ISSUE_SPLIT_REQUEST = 9;
	public int APM_CREATE_SPLIT_TABLE = 10;
	public int APM_CREATE_METHOD_PARAM_TABLE = 11;
	public int APM_CHECK_SIM_PERMISSIONS = 12;
	public int APM_REFRESH_MARKET_DATA = 13;
	public int APM_PUBLISH_TABLE_AS_XML = 14;
	public int APM_GET_UNIQUE_ID = 15;
	public int APM_RENORMALISE_GUARANTEED_MESSAGE_LOG = 16;
	public int APM_BATCH_START = 17;
	public int APM_BATCH_WRITE = 18;
	public int APM_BATCH_END = 19;
	public int APM_APPLY = 20;
	public int APM_BACKOUT = 21;
	public int APM_CACHE_TABLE_GET = 22;
	public int APM_CACHE_TABLE_ADD = 23;
	public int APM_CACHE_TABLE_DROP = 24;
	public int APM_CACHE_TABLE_INFO = 25;
	public int APM_PRUNE_START = 31;
	public int APM_PRUNE_END = 32;
	public int APM_PRUNE_QUIT = 33;

	public int APM_PUBLISH_SCOPE_EXTERNAL = 0;
	public int APM_PUBLISH_SCOPE_LOCAL = 1;
	public int APM_PUBLISH_SCOPE_INTERNAL = 2;
	public int APM_RETVAL_UPDATE_IGNORED = 2;

	public enum EntityType { 
		//ENUM_VALUE	TEXT
		UNKNOWN(		"UNKNOWN"), 
		DEAL(			"DEAL"), 
		NOMINATION(		"NOMINATION");
		
       private final String value;

       private String entityType;
       private String entityGroupLabel;
       private String entityGroup;
       private String entityGroupDimension;
       private String primaryEntity;
       private String primaryEntityDimension;	
       private String secondaryEntity;
       private String entityVersion;
       private String entityGroupFilter;
       private String primaryEntityFilter;

       EntityType(String value)
       {
          this.value = value;
       }

       @Override public String toString(){
           return value;
       }
       
       public String getEntityType(){
    	   return entityType;
       }
       
       public String getEntityGroupLabel(){
    	   return entityGroupLabel;
       }

       public String getEntityGroup(){
           return entityGroup;
       }
       
       public String getEntityGroupDimension(){
    	   return entityGroupDimension;
       }

       public String getPrimaryEntity(){
           return primaryEntity;
       }
       
       public String getPrimaryEntityDimension(){
    	   return primaryEntityDimension;
       }

       public String getSecondaryEntity(){
           return secondaryEntity;
       }

       public String getEntityVersion(){
           return entityVersion;
       }

       public String getEntityGroupFilter(){
           return entityGroupFilter;
       }
       
       public String getPrimaryEntityFilter(){
           return primaryEntityFilter;
       }
       
       public String getEntityGroupId(){
           return entityGroup+"_id";
       }

       public String getEntityGroupName(){
           return entityGroup+"_name";
       }

       public String getOldEntityGroupId(){
           return "old_"+entityGroup;
       }

       public String getFormattedNameForGroupId(int groupId) throws OException {
	   	switch (value)
	   	{
	   	    case "DEAL":
	   	    	return Table.formatRefInt(groupId, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
	   	    case "NOMINATION":
	   	    	return Table.formatRefInt(groupId, SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);
	   	}
	   	return "UNKNOWN";
       }

       private static boolean isMapPopulated = false;
       private static final Map<String, EntityType> entityMap =
           new HashMap<String, EntityType>();

       static 
       {
    	   if (isMapPopulated == false) 
    	   {
    		   try {
	    		   //Call USER_get_apm_entity_types stored procedure
	    		   Table tAPMEntityTypes;
	    		   tAPMEntityTypes = Table.tableNew("apm_entity_types");
	    		   int iRetVal = APM_DBASE_RunProcFillTable("USER_get_apm_entity_types", tAPMEntityTypes);
	    		   
	    		   if (iRetVal > 0) {
		    		   //Go through each one adding to the map
		   			   for (int row = 1; row <= tAPMEntityTypes.getNumRows(); row++) {
		   				   String entity_type = tAPMEntityTypes.getString("entity_type", row);
		   				   
		   		    	   for (EntityType r : values()) {
		   		    		   if (r.toString().equals(entity_type.toUpperCase())) {
		   		    			   r.entityType = entity_type;
		   		   				   r.entityGroupLabel = tAPMEntityTypes.getString("entity_group_label", row);
		   		   				   r.entityGroup = tAPMEntityTypes.getString("entity_group", row);
		   		   				   r.entityGroupDimension = tAPMEntityTypes.getString("entity_group_dimension", row);
		   		   				   r.primaryEntity = tAPMEntityTypes.getString("primary_entity", row);
		   		   				   r.primaryEntityDimension = tAPMEntityTypes.getString("primary_entity_dimension", row);
		   		   				   r.secondaryEntity = tAPMEntityTypes.getString("secondary_entity", row);
		   		   				   r.entityVersion = tAPMEntityTypes.getString("entity_version", row);
		   		   				   r.entityGroupFilter = tAPMEntityTypes.getString("entity_group_filter", row);
		   		   				   r.primaryEntityFilter = tAPMEntityTypes.getString("primary_entity_filter", row);
		   		   				   entityMap.put(r.toString().toUpperCase(), r);
			   		               break;
		   		    		   }
		   		           }
		   			   }
		   			   if (entityMap.size() > 0)
		   				   isMapPopulated = true;
	    		   }
    			} catch (OException e) {
    				printStackTrace("APM_Utils.EntityType", e);
    			}
           }
       }

       public static EntityType getByValue(String value){
           return entityMap.get(value.toUpperCase());
       }
	}

	public enum ServiceType { 
		//ENUM_VALUE    TEXT
		UNKNOWN(        "UNKNOWN"), 
		APM(            "APM"), 
		ADA(            "ADA");

		private final String value;
		
		ServiceType(String value) {
	          this.value = value;
	       }
		
		@Override public String toString(){
	           return value;
	       }
	}

	private Boolean useADS = null;

	private static EntityType currentEntityType = EntityType.UNKNOWN;
	private static ServiceType currentServiceType = ServiceType.UNKNOWN;


	/*-------------------------------------------------------------------------------
	Name:          APM_GetPackageSettingInt

	Return Values: None
	-------------------------------------------------------------------------------*/
	public int APM_GetPackageSettingInt(Table tAPMArgumentTable, String sPackageName, String sSettingName) {
		int iRow, iPackageRow;
		Table tPackageSettings;
		Table tPackageDetails;

		try {
			tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);

			if (Table.isTableValid(tPackageDetails) != 0) {
				iPackageRow = tPackageDetails.unsortedFindString("package_name", sPackageName, SEARCH_CASE_ENUM.CASE_SENSITIVE);
				if (iPackageRow > 0) {
					tPackageSettings = tPackageDetails.getTable("package_settings", iPackageRow);
					if (Table.isTableValid(tPackageSettings) != 0) {
						iRow = tPackageSettings.findString("setting_name", sSettingName, SEARCH_ENUM.FIRST_IN_GROUP);
						if (iRow > 0)
							return APM_StrToInt(tPackageSettings.getString("value", iRow));
					}
				}
			}
		} catch (OException e) {
			printStackTrace("APM_GetPackageSettingInt ", e);
		}

		return -1;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_GetOverallPackageSettingInt


	Return Values: None
	-------------------------------------------------------------------------------*/
	public int APM_GetOverallPackageSettingInt(Table tAPMArgumentTable, String sSettingName, int bTRUEIfAnyTRUE, int bFALSEIfAnyFALSE, int iDefaultValue) {
		int iRow, iPackageSetting;
		Table tPackageDetails;
		String sPackageName;

		try {
			tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);

			// debug_flag : If ANY packages have debug ON, then overall setting is
			// ON
			// force_intraday_reval_type : If ANY packages need intraday revals,
			// then overall setting is 1
			// export_sim_defs : If ANY packages have this set, then overall setting
			// is 1
			// run_as_avs_script : If ANY packages have this unset (i.e. a reval is
			// needed), then overall the setting is 0
			// enable_batch_procs_msgs : If ANY packages have this set, then overall
			// setting is 1
			// enable_xml_messaging : If ANY packages have this set, then overall
			// setting is 1
			// export_message_certifier : If ANY packages have this set, then
			// overall setting is 1
			// use_apm_loadvol_functionality : If ANY packages have this set, then
			// overall setting is 1

			if (Table.isTableValid(tPackageDetails) != 0) {
				for (iRow = 1; iRow <= tPackageDetails.getNumRows(); iRow++) {
					sPackageName = tPackageDetails.getString("package_name", iRow);
					iPackageSetting = APM_GetPackageSettingInt(tAPMArgumentTable, sPackageName, sSettingName);
					if (iPackageSetting < 0)
						iPackageSetting = iDefaultValue; // Setting not found - move along
					if ((bTRUEIfAnyTRUE != 0) && (iPackageSetting != 0))
						return 1;
					if (bFALSEIfAnyFALSE == 1 && (iPackageSetting == 0))
						return 0;
				}
				if (bTRUEIfAnyTRUE != 0)
					return 0;
				if (bFALSEIfAnyFALSE != 0)
					return 1;
			}
		} catch (OException e) {
			printStackTrace("APM_GetOverallPackageSettingInt ", e);
		}

		return iDefaultValue;
	}

	public int APM_CheckColumn(Table tCheck, String sCol, int iType) throws OException
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

	// returns whether the service calling the script is a nomination, deal (or other) type of service
	public EntityType  FindAndSetEntityType(Table argt, Table tAPMArgumentTable) throws OException {
		EntityType entityType;

		currentEntityType = EntityType.UNKNOWN;		

		// look for "apm_entity_type" column first - that tells us the type of the service
		if (APM_CheckColumn(argt, "apm_entity_type", COL_TYPE_ENUM.COL_STRING.toInt()) != 0 && argt.getString("apm_entity_type", 1).equals("Nomination") ) // nomination
		{
			entityType = EntityType.NOMINATION;
		}
		else
		{
			// if we cannot find the column or its set to nomination then it must be a deal based service
			entityType = EntityType.DEAL;
		}
		
		currentServiceType = ServiceType.UNKNOWN;
		
		if (APM_CheckColumn(argt, "apm_service_type", COL_TYPE_ENUM.COL_STRING.toInt()) != 0 && argt.getString("apm_service_type", 1).equals("ADA"))
		{
			currentServiceType = ServiceType.ADA;
		}
		else
		{
			currentServiceType = ServiceType.APM;
		}
		
		tAPMArgumentTable.setString("Service Type", 1, currentServiceType.toString());

		tAPMArgumentTable.setString("Entity Type", 1, entityType.toString());
		currentEntityType = entityType;	
		APM_PrintMessage(tAPMArgumentTable, "Current Entity Type = " + entityType.toString());	
		return entityType;
	}

	public EntityType GetCurrentEntityType(Table tAPMArgumentTable) throws OException {
		// look for "apm_service_type" column first - that tells us the type of the service
		if ( currentEntityType != EntityType.UNKNOWN )
		{
			return currentEntityType;
		}

		// if unset get it from the tAPMArgumentTable
		EntityType entityType = EntityType.DEAL;
		int colNum = tAPMArgumentTable.getColNum("Entity Type");
		if (colNum > 0 )
			entityType = EntityType.getByValue(tAPMArgumentTable.getString(colNum, 1));		
		else
			APM_PrintMessage(tAPMArgumentTable, "Cannot find Entity Type in table.  Defaulting to DEAL.");	

		currentEntityType = entityType;		
		return entityType;
	}

	/**
	 * To find whether APM is using ADS
	 * 
	 * @reutrn - true if APM is using ADS, false otherwise
	 */
	public boolean useADS(Table tAPMArgumentTable) {

		try {
			if (useADS == null) {
				Table configuration = getConfigurationTable(tAPMArgumentTable);
				int row = configuration.unsortedFindString("setting_name", "use_ads", SEARCH_CASE_ENUM.CASE_SENSITIVE);

				if (row > 0 && Str.equal(configuration.getString("value", row), "1") == 1)
					useADS = true;
				else
					useADS = false;
			}
		} catch (OException e) {
			printStackTrace("useADS ", e);
		}

		return useADS == null ? false : useADS.booleanValue();
	}

	/// Returns true iff we use the database for server logging.  Need a more
	/// generic tfe_configuration reader in the future.
	public boolean useDbForServerLogging(Table tAPMArgumentTable) {
		try {
			Table configuration = getConfigurationTable(tAPMArgumentTable);
			int row = configuration.unsortedFindString("setting_name", "use_db_for_server_logging", SEARCH_CASE_ENUM.CASE_SENSITIVE);

			if (row > 0 && Str.equal(configuration.getString("value", row), "1") == 1)
				return (true);
			else
				return (false);
		} catch (OException e) {
			printStackTrace("useDbForServerLogging ", e);
		}

		return (false);
	}

	/// Returns true if we are saving sim defs and queries to the db.  Need a more
	/// generic tfe_configuration reader in the future.
	public boolean saveSimDefAndQuery(Table tAPMArgumentTable) {
		try {
			Table configuration = getConfigurationTable(tAPMArgumentTable);
			int row = configuration.unsortedFindString("setting_name", "export_sim_defs_and_query", SEARCH_CASE_ENUM.CASE_SENSITIVE);

			if (row > 0 && Str.equal(configuration.getString("value", row), "1") == 1)
				return (true);
			else
				return (false);
		} catch (OException e) {
			printStackTrace("saveSimDefAndQuery ", e);
		}

		return (false);
	}

	/// Returns true if we are outputting debug level.  Need a more
	/// generic tfe_configuration reader in the future.
	public int outputDebugInfoFlag(Table tAPMArgumentTable) {
		try {
			Table configuration = getConfigurationTable(tAPMArgumentTable);
			int row = configuration.unsortedFindString("setting_name", "debug_flag", SEARCH_CASE_ENUM.CASE_SENSITIVE);

			if (row > 0 && Str.equal(configuration.getString("value", row), "1") == 1)
				return 1;
			else
				return 0;
		} catch (OException e) {
			printStackTrace("outputDebugInfo ", e);
		}

		return 0;
	}
	
	///
	private Table getConfigurationTable(Table tAPMArgumentTable) {
		String sCachedName = "APM_Configuration";
		Table configuration = null;
		int retVal = 0;

		try {
			configuration = APM_CacheTableGet(sCachedName, tAPMArgumentTable);

			if (Table.isTableValid(configuration) == 0) {
				configuration = Table.tableNew("tfe_configuration");
				retVal = DBaseTable.loadFromDbWithSQL(configuration, "package_name, setting_name, value", "tfe_configuration", "1=1");

				if (retVal > 0) {
					APM_CacheTableAdd(sCachedName, "TFE.METADATA.CHANGED", configuration.copyTable(), tAPMArgumentTable);
					useADS = null;
				}

			}
		} catch (OException e) {
			printStackTrace("getConfigurationTable ", e);
		}

		return (configuration);
	}

	/**
	 * Print out the full stack trace to the OConsole.
	 * 
	 * @param message
	 * @param e
	 *            @
	 */
	public static void printStackTrace(String message, Exception e) {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			OConsole.oprint(message + ":- " + sw.toString());
		} catch (OException o) {
			// Eat the exception or we'll go recursive...
		}
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

	/// Find the path to a given script...
	public String find_script_path(String script_name) throws OException {
		return find_script_path(script_name, false);
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
			if (iRetVal != 0) {
				Table.cacheTable("dir_table", DirTable);
				DirTable.sortCol("node_name");
			}
		}

		int firstRow = DirTable.findString("node_name", script_name, SEARCH_ENUM.FIRST_IN_GROUP);
		int lastRow = DirTable.findString("node_name", script_name, SEARCH_ENUM.LAST_IN_GROUP);

		NodeID = 0;
		scriptCount = 0;
		if (firstRow > 0) {
			for (row = firstRow; row <= lastRow; row++) {
				if (DirTable.getInt(3, row) == 7)  // openjvs script
				{
					scriptCount++;
					if (scriptCount == 1)
						NodeID = DirTable.getInt(1, row);
				}
			}

			if (NodeID > 0) {
				if (scriptCount > 1) {
					OConsole.oprint("WARNING: There is more than one JVS script name \"" + script_name + "\". Retriving the path for the first one");
					Util.scriptPostStatus("WARNING: There is more than one JVS script name \"" + script_name + "\". Retriving the path for the first one");
				}
				script_path = find_path("node_id", NodeID, DirTable);
			} else {
				OConsole.oprint("ERROR: Cannnot find a JVS script name:" + script_name);
				Util.scriptPostStatus("ERROR: Cannnot find a JVS script name:" + script_name);
			}
		}

		return script_path;
	}

	/**
	 * -------------------------------------------------------------------------------
	 * Name: APM_LogStatusMessage
	 * 
	 * Description: This routine will add a message specifically for progress &
	 * problems encountered when generating a dataset key's APM Reporting data
	 * 
	 * Messages are logged to apm_msg_log. This is keyed by package, entity_group_id,
	 * scenario. As such, if : - sPackage = "" : Message is for all packages -
	 * entityGroupId = -1 : Message is for all entity groups - iScenarioId = -1 :
	 * Message is for all scenarios - secondaryEntityNum = -1 : Message is for all entities.
	 * Note this parameter is ignored for cModeBatch
	 * 
	 * Parameters: iMode - cModeBatch, cModeApply, cModeBackout
	 * iGlobalMsgFlag - if 1, then add just one row to apm_msg_log (overrides
	 * entity groups, scenarios, packages etc.) sPackageName - Current package name
	 * entityGroupId - Current entity group being processed iScenarioId - Current
	 * scenario being processed secondaryEntityNum - Current secondary entity num for incremental
	 * updates primaryEntityNum - Current primary entity num for incremental updates
	 * tAPMArgumentTable - argt for other info (e.g. dataset_type, list of all
	 * packages, entity groups, scenarios) tBatchDatasets - This is yuk ! ... In
	 * the batch end APM Operation, a column is added to the tParams that
	 * details the list of datasets that have been updated. This table needs to
	 * be published in the message to the PageServer so it knows the old & new
	 * dataset Ids sMessage - The message to log
	 * 
	 * Return Values: None
	 * -------------------------------------------------------------------------------
	 */
	public void APM_LogStatusMessage(int iMode, int iGlobalMsgFlag, int iMsgType, String sJobName, String sPackageName, int entityGroupId, int iScenarioId, int secondaryEntityNum, int primaryEntityNum, int entityVersion, Table tAPMArgumentTable, Table tBatchDatasets, String sMessage) throws OException {
		Table tAPMLog;
		Table tMainArgTable;
		Table tScenarios = Util.NULL_TABLE;
		Table tEntityGroupIds = Util.NULL_TABLE;
		Table tPackageDetails = Util.NULL_TABLE;
		Table tEntityInfo = Util.NULL_TABLE;
		int entityGroupRow, iScenarioRow, iPackageRow;
		int iServiceId, enable_batch_proc_msgs;
		int iEntityRow, iBlockSecondaryEntityNum, iBlockEntityVersion, iBlockPrimaryEntityNum, iUpdateMode, iNumEntityRows;
		String sPageSubject;

		// KKD: if iMsgType is batch started and APM is not using ADS then
		// change iMsgType to processing
		if (iMsgType == cStatusMsgTypeStarted && isActivePositionManagerService(tAPMArgumentTable) && !useADS(tAPMArgumentTable)) {
			iMsgType = cStatusMsgTypeProcessing;
		}

		tMainArgTable = tAPMArgumentTable.getTable("Main Argt", 1);
		iServiceId = tAPMArgumentTable.getInt("service_id", 1);

		/*
		 * only insert entries into the database & send out RV messages for
		 * certain types of message
		 */
		if (iMode != cModeBatch && iMsgType != cStatusMsgTypeFailure && iMsgType != cStatusMsgTypeRerun)
			return;

		// process each individual message if in block mode - does this
		// case even exist ?
		if ((iMode == cModeBlockUpdate) && (primaryEntityNum < 1) && (secondaryEntityNum < 1)) {

			if (Table.isTableValid(tAPMArgumentTable.getTable("Filtered Entity Info", 1)) > 0)
				tEntityInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1);
			else
				tEntityInfo = tAPMArgumentTable.getTable("Global Filtered Entity Info", 1);

			iNumEntityRows = tEntityInfo.getNumRows();

			for (iEntityRow = 1; iEntityRow <= iNumEntityRows; iEntityRow++) {
				iBlockSecondaryEntityNum = tEntityInfo.getInt("secondary_entity_num", iEntityRow);
				iBlockEntityVersion = tEntityInfo.getInt("entity_version", iEntityRow);
				iBlockPrimaryEntityNum = tEntityInfo.getInt("primary_entity_num", iEntityRow);
				iUpdateMode = tEntityInfo.getInt("update_mode", iEntityRow);

				// only log failures for those entities which have failed
				if ((iMsgType == cStatusMsgTypeFailure && tEntityInfo.getInt("log_status", iEntityRow) == OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt())
				        || (iMsgType == cStatusMsgTypeRerun)) 
				{
					if ( sPackageName.isEmpty())
					{
						Table packageDetails = tAPMArgumentTable.getTable("Package Details", 1);
						for ( int iLoop = 1; iLoop <= packageDetails.getNumRows(); iLoop++)
						{
							String globalPackageName = packageDetails.getString("package_name", iLoop); 
							if (APM_IsIncrementalOnForPackage(tAPMArgumentTable, globalPackageName) == true)
								APM_LogStatusMessage(iUpdateMode, iGlobalMsgFlag, iMsgType, sJobName, globalPackageName, entityGroupId, iScenarioId, iBlockSecondaryEntityNum, iBlockPrimaryEntityNum, iBlockEntityVersion, tAPMArgumentTable, tBatchDatasets, sMessage);
						}
					}
					else
					{
						if (APM_IsIncrementalOnForPackage(tAPMArgumentTable, sPackageName) == true)
							APM_LogStatusMessage(iUpdateMode, iGlobalMsgFlag, iMsgType, sJobName, sPackageName, entityGroupId, iScenarioId, iBlockSecondaryEntityNum, iBlockPrimaryEntityNum, iBlockEntityVersion, tAPMArgumentTable, tBatchDatasets, sMessage);
					}
				}
			}
			return;
		}

		/* NEVER raise BAD UPDATE IF entity num is zero or negative - makes no sense ! */
		if (iMode != cModeBatch && iMsgType == cStatusMsgTypeFailure && primaryEntityNum < 1) {
			APM_PrintErrorMessage(tAPMArgumentTable, "Not publishing bad update. Entity num is zero !  Report this to OL Support !!!!!!");
			return;
		}

		/* if processing messages are switched off */
		enable_batch_proc_msgs = APM_GetOverallPackageSettingInt(tAPMArgumentTable, "enable_batch_proc_msgs", 1, 0, 1);
		if (iMsgType == cStatusMsgTypeProcessing && enable_batch_proc_msgs == 0)
			return;

		/* these messages are always sent out - otherwise batch stats won't work */
		if (iMsgType == cStatusMsgTypeProcessingAlways)
			iMsgType = cStatusMsgTypeProcessing;

		// Debug.sleep for 1 sec to make sure msg appears in the right order
		// (after prior messages) in online pfolios screen
		if (iMsgType == cStatusMsgTypeFailure && iMode == cModeBatch && isActivePositionManagerService(tAPMArgumentTable) && !useADS(tAPMArgumentTable))
			Debug.sleep(1000);

		// Add details of the job if supplied
		// JIMNOTE ... Cannot pre-pend Job info since batch stats rely on
		// specific message text ! See note at the top of this file ...
		// if (Str.len(sJobName) > 0) sMessage = "[JOB: " + sJobName + "] => " +
		// sMessage;
		if (Str.len(sJobName) > 0)
			sMessage = sMessage + " [JOB: " + sJobName + "]";

		if (secondaryEntityNum > 0 && iMsgType == cStatusMsgTypeFailure) {
			// add bad update text to msg
			if ( GetCurrentEntityType(tAPMArgumentTable) == EntityType.DEAL )
			   sMessage = sMessage + ". Bad update !  Tran Num: " + secondaryEntityNum + ". Deal Num: " + primaryEntityNum;
			else
			   sMessage = sMessage + ". Bad update !  Delivery ID: " + primaryEntityNum;
		}

		tAPMLog = Table.tableNew("apm_msg_log");
		tAPMLog.setTableTitle("apm_msg_log");
		tAPMLog.addCol("secondary_entity_num", COL_TYPE_ENUM.COL_INT);
		tAPMLog.addCol("primary_entity_num", COL_TYPE_ENUM.COL_INT);
		tAPMLog.addCol("msg_type", COL_TYPE_ENUM.COL_INT);
		tAPMLog.addCol("msg_mode", COL_TYPE_ENUM.COL_INT);
		tAPMLog.addCol("msg_text", COL_TYPE_ENUM.COL_STRING);
		tAPMLog.addCol("timestamp", COL_TYPE_ENUM.COL_DATE_TIME);
		tAPMLog.addCol("rtp_page_subject", COL_TYPE_ENUM.COL_STRING);
		tAPMLog.addCol("package", COL_TYPE_ENUM.COL_STRING);
		tAPMLog.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
		tAPMLog.addCol("scenario_id", COL_TYPE_ENUM.COL_INT);
		tAPMLog.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
		tAPMLog.addCol("service_id", COL_TYPE_ENUM.COL_INT);

		tAPMLog.addRow();
		tAPMLog.setInt("msg_type", 1, iMsgType);
		tAPMLog.setInt("msg_mode", 1, iMode);

		/* now put msg into the msg_table (max 255 for sybase) */
		if (DBase.getDbType() == DBTYPE_ENUM.DBTYPE_SYBASE.toInt() && Str.len(sMessage) > 254)
			tAPMLog.setString("msg_text", 1, Str.substr(sMessage, 0, 254));
		else
			tAPMLog.setString("msg_text", 1, sMessage);

		tAPMLog.setInt("dataset_type_id", 1, tAPMArgumentTable.getInt("dataset_type_id", 1));
		tAPMLog.setInt("service_id", 1, iServiceId);
		tAPMLog.setDateTimeByParts("timestamp", 1, OCalendar.getServerDate(), Util.timeGetServerTime());

		if (iGlobalMsgFlag != 0) {
			// Global message. This sits above entity group, package and scenario,
			// so override them if set
			sPackageName = "";
			entityGroupId = iScenarioId = secondaryEntityNum = primaryEntityNum = -1;
		} else {
			// If caller hasn't specified a specific package, then log message
			// for all packages ...
			if (Str.len(sPackageName) < 1)
				tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);
			// If caller hasn't specified a specific entity group, then log message
			// for all entity groups ...
			if (entityGroupId < 0)
				tEntityGroupIds = tAPMArgumentTable.getTable("Selected Entity Groups", 1);
			// If caller hasn't specified a specific scenario, then log message
			// for all scenarios ...
			if (iScenarioId < 0)
				tScenarios = tAPMArgumentTable.getTable("Scenario_List", 1);
			// Get the entity details if an incremental update and entity not
			// specifically passed in ...
			if ((iMode != cModeBatch) && (secondaryEntityNum < 0) && tAPMArgumentTable.getColNum("Filtered Entity Info") > 0)
				tEntityInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1);
			else
			{
				if ( GetCurrentEntityType(tAPMArgumentTable) == EntityType.DEAL )
				   tEntityInfo = tMainArgTable.getTable("Deal Info", 1);
				else if ( GetCurrentEntityType(tAPMArgumentTable) == EntityType.NOMINATION )
				   tEntityInfo = tMainArgTable.getTable("Nom Info", 1);
			}
		}

		if (secondaryEntityNum > 0 && iMsgType == cStatusMsgTypeFailure) {
			// check that a later version of the entity is not already in processing
			// if there is then do NOT raise a bad update
			// the later one is yet to be processed OR 
			// alternatively the later one may already have been processed (grid situation)
			// In either case do not raise a bad update as you could overwrite the later
			// versions status (if already processed) or 
			// the problem may not affect the later version (if not already processed).  

			if (APM_IsLaterEntityVersionProcessingInQueue(tAPMArgumentTable, primaryEntityNum, secondaryEntityNum, entityVersion, iServiceId)) 
			{
			   if ( GetCurrentEntityType(tAPMArgumentTable) == EntityType.DEAL )
				   APM_PrintMessage(tAPMArgumentTable, "Not publishing bad update message as later version processing/processed in queue.  Deal num: " + primaryEntityNum
				        + ", TranNum: " + secondaryEntityNum + ", Version:" + entityVersion);
			   else
				   APM_PrintMessage(tAPMArgumentTable, "Not publishing bad update message as later version processing/processed in queue.  Delivery ID: " + primaryEntityNum
				        + ", Version:" + entityVersion);
			   
				tAPMLog.destroy();
				return;
			}
		}

		/*
		 * Send an ECOM message to the PageServer ... STEVENOTE : I think this
		 * should be at a lower level (i.e. within the loop). There should be a
		 * ONE TO ONE relationship between what's logged in the apm_msg_log, and
		 * ECOM messages sent !!
		 */
		// batches have global messages so it doesn't matter if the entity ID is -1
		// if no entity group table - error raised too early - then also do it here
		if (iMode == cModeBatch || Table.isTableValid(tEntityGroupIds) != 1)
		{
			if (APM_PublishStatusMessage(iMsgType, iMode, tAPMArgumentTable, sJobName, tAPMLog, sPackageName, entityGroupId, iScenarioId, secondaryEntityNum, primaryEntityNum, iServiceId, tBatchDatasets) == 0)
				// Note ... continue to log to apm_msg_log even if the ECOM message
				// failed to be sent
				APM_PrintErrorMessage(tAPMArgumentTable, "Failed to publish message to APM PageServer");
		}

		// Loop through the entity groups if necessary
		for (entityGroupRow = 1; ((entityGroupRow == 1) || (Table.isTableValid(tEntityGroupIds) == 1 && (entityGroupRow <= tEntityGroupIds.getNumRows()))); entityGroupRow++) {
			if (Table.isTableValid(tEntityGroupIds) != 0)
				entityGroupId = tEntityGroupIds.getInt("entity_group_id", entityGroupRow);

			// i.e. for a deal update we want to make sure the entity group is set
			if (iMode != cModeBatch)
			{
				if (APM_PublishStatusMessage(iMsgType, iMode, tAPMArgumentTable, sJobName, tAPMLog, sPackageName, entityGroupId, iScenarioId, secondaryEntityNum, primaryEntityNum, iServiceId, tBatchDatasets) == 0)
					// Note ... continue to log to apm_msg_log even if the ECOM message
					// failed to be sent
					APM_PrintErrorMessage(tAPMArgumentTable, "Failed to publish message to APM PageServer");
			}

			// Loop through the scenarios if necessary
			for (iScenarioRow = 1; ((iScenarioRow == 1) || (Table.isTableValid(tScenarios) == 1 && (iScenarioRow <= tScenarios.getNumRows()))); iScenarioRow++) {
				if (Table.isTableValid(tScenarios) != 0)
					iScenarioId = tScenarios.getInt("scenario_id", iScenarioRow);

				// Loop through the packages if necessary
				for (iPackageRow = 1; ((iPackageRow == 1) || (Table.isTableValid(tPackageDetails) == 1 && (iPackageRow <= tPackageDetails.getNumRows()))); iPackageRow++) {
					if (Table.isTableValid(tPackageDetails) != 0)
						sPackageName = tPackageDetails.getString("package_name", iPackageRow);

					// Loop through the entity list if necessary
					for (iEntityRow = 1; ((iEntityRow == 1) || (Table.isTableValid(tEntityInfo) == 1 && (iEntityRow <= tEntityInfo.getNumRows()))); iEntityRow++) {
						if (Table.isTableValid(tEntityInfo) != 0) {
							// we only need to do this if we are in a block update (i.e. using the filtered entity info table)
							if (tEntityInfo.getColNum("entity_group_id") > 0 && tEntityInfo.getInt("entity_group_id", iEntityRow) == entityGroupId) {
								secondaryEntityNum = tEntityInfo.getInt("secondary_entity_num", iEntityRow);
								primaryEntityNum = tEntityInfo.getInt("primary_entity_num", iEntityRow);
							}
						}

						tAPMLog.setString("package", 1, sPackageName);
						tAPMLog.setInt("entity_group_id", 1, entityGroupId);
						tAPMLog.setInt("scenario_id", 1, iScenarioId);

						tAPMLog.setInt("secondary_entity_num", 1, secondaryEntityNum);
						tAPMLog.setInt("primary_entity_num", 1, primaryEntityNum);

						// we still need to insert entries into table for ADS so that statistics work
						// make sure the table has the right number columns
						if (tAPMLog.getColNum("use_ads") > 0)
							tAPMLog.delCol(tAPMLog.getColNum("use_ads"));

						if (entityGroupId > 0)
							sPageSubject = tAPMArgumentTable.getString("RTP Page Prefix", 1) + Str.intToStr(entityGroupId);
						else
							sPageSubject = tAPMArgumentTable.getString("RTP Page Prefix", 1) + Str.intToStr(iServiceId) + "_GLOBAL";

						tAPMLog.setString("rtp_page_subject", 1, sPageSubject);

						// if there is an error that is logged inside this fn
						APM_DBASE_RunProc(tAPMArgumentTable, "USER_insert_apm_msg_log", tAPMLog);

						if (isActivePositionManagerService(tAPMArgumentTable) && useADS(tAPMArgumentTable)) {
							if (tAPMLog.getColNum("use_ads") <= 0) {
								tAPMLog.addCol("use_ads", COL_TYPE_ENUM.COL_INT);
								tAPMLog.setInt("use_ads", 1, 1);
							}

							int iRetVal = 1;

							try {
								ADS_LogStatusMessage logStatusMessage = new ADS_LogStatusMessage();
								iRetVal = logStatusMessage.execute(tAPMLog);
							} catch (Exception t) {
								iRetVal = 0;
								APM_PrintErrorMessage(tAPMArgumentTable, "Exception while calling ADS_LogStatusMessage: " + t);
								String message = getStackTrace(t);
								APM_PrintErrorMessage(tAPMArgumentTable, message + "\n");
							}

							if (iRetVal == 0) {
								APM_PrintErrorMessage(tAPMArgumentTable, "Failure while calling ADS_LogStatusMessage");
								tAPMLog.destroy();
								throw new OException("UseAds setting is ON but ADS grid is not configured or running.");
							}
						}

					}
				}
			}
		}

		tAPMLog.destroy();
	}

	public int APM_PublishTable(Table tAPMArgumentTable, String sPublishMsgSubject, Table tPublishMsg, int iMsgType, int entityGroupId, int iPackageId) throws OException {
		int enable_xml_messaging;
		int enable_message_certifier;
		Table tPublishParams;
		XString err_xstring = null;
		int iRetVal = 1;

		enable_xml_messaging = APM_GetOverallPackageSettingInt(tAPMArgumentTable, "enable_xml_messaging", 1, 0, 1);
		enable_message_certifier = APM_GetOverallPackageSettingInt(tAPMArgumentTable, "enable_message_certifier", 1, 0, 1);

		// get the param table for the publish
		tPublishParams = Table.tableNew("publish_table_as_xml_params");
		if (enable_xml_messaging == 1 && Apm.performOperation(APM_PUBLISH_TABLE_AS_XML, 1, tPublishParams, err_xstring) == 1) {
			tPublishParams.setString("publish_xml_msg_subject", 1, sPublishMsgSubject);
			tPublishParams.setTable("publish_xml_msg_table", 1, tPublishMsg);
			tPublishParams.setInt("publish_xml_msg_scope", 1, APM_PUBLISH_SCOPE_EXTERNAL); // global
			                                                                               // message
			tPublishParams.setInt("publish_xml_msg_table_level", 1, -1); // recurse
			                                                             // into
			                                                             // all
			                                                             // tables
			tPublishParams.setInt("publish_xml_msg_enable_certifier", 1, enable_message_certifier);
			tPublishParams.setInt("publish_xml_msg_type", 1, iMsgType);
			tPublishParams.setInt("publish_xml_msg_entity_group", 1, entityGroupId);
			tPublishParams.setInt("publish_xml_msg_package_id", 1, iPackageId);
			tPublishParams.setDateTimeByParts("publish_xml_msg_timestamp", 1, OCalendar.getServerDate(), Util.timeGetServerTime());

			err_xstring = Str.xstringNew();
			if (Apm.performOperation(APM_PUBLISH_TABLE_AS_XML, 0, tPublishParams, err_xstring) == 0) {
				APM_PrintErrorMessage(tAPMArgumentTable, "Error performing operation APM_PUBLISH_TABLE_AS_XML : " + Str.xstringGetString(err_xstring) + "\n");
				iRetVal = 0;
			}
			tPublishParams.setTable("publish_xml_msg_table", 1, Util.NULL_TABLE);
			Str.xstringDestroy(err_xstring);
		} else {
			// Publish table as XML perform operation not available, use old
			// table publish
			Services.publishGlobal(tPublishMsg, sPublishMsgSubject);
		}
		tPublishParams.destroy();
		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_PublishStatusMessage

	Description:   This routine is designed to be called only from APM_LogStatusMessage
	            It will publish appropriate messages to the APM PageServer

	Return Values: None
	-------------------------------------------------------------------------------*/
	public int APM_PublishStatusMessage(int iMsgType, int iMode, Table tAPMArgumentTable, String sJobName, Table tAPMLog, String sPackageName, int entityGroupId, int iScenarioId, int secondaryEntityNum, int primaryEntityNum, int iServiceId, Table tBatchDatasets) throws OException {
		String rvMessageHeader = "entity_group_status_changed";
		Table tAPMLogCopy, tScenarios, tPublishMsg;
		int iRetVal = 1;
		String sPageSubject;

		// Only attempt if we are not running ADS.
		if (useADS(tAPMArgumentTable) || isActiveDataAnalyticsService(tAPMArgumentTable))
			return 1;

		// JIMNOTE ... there should be a ONE TO ONE relationship between rows
		// logged in the DB and RV messages sent.
		// As such, each message should include package, entity group, scenario,
		// dataset, entitynum.
		// It seems WRONG to me that we have to add a Table of scenarios for
		// example !!!
		// This needs to be fixed but will involve some PageServer changes !!!

		/*
		 * publish an RV message if we're in the batch run or we have a failure
		 * of some sort or rerun message
		 */
		if (iMode == cModeBatch || iMsgType == cStatusMsgTypeFailure || iMsgType == cStatusMsgTypeRerun) {
			tScenarios = tAPMArgumentTable.getTable("Scenario_List", 1);
			tAPMLogCopy = tAPMLog.copyTable();

			tAPMLogCopy.setString("package", 1, sPackageName);
			tAPMLogCopy.setInt("entity_group_id", 1, entityGroupId);
			tAPMLogCopy.setInt("scenario_id", 1, iScenarioId);
			tAPMLogCopy.setInt("secondary_entity_num", 1, secondaryEntityNum);
			tAPMLogCopy.setInt("primary_entity_num", 1, primaryEntityNum);
			// JIMNOTE : Is this still important or used ? !!
			// STEVENOTE : Yes - the pageserver is still using these stupid
			// things - needs to be refactored to use the dataset key
			if (entityGroupId > 0)
				sPageSubject = tAPMArgumentTable.getString("RTP Page Prefix", 1) + Str.intToStr(entityGroupId);
			else
				sPageSubject = tAPMArgumentTable.getString("RTP Page Prefix", 1) + Str.intToStr(iServiceId) + "_GLOBAL";

			tAPMLogCopy.setString("rtp_page_subject", 1, sPageSubject);

			// Now set the dataset information, and reset the scenario value
			// JIMNOTE : As I mentioned above this seems rubbish ... we've just
			// logged a message to the DB. We should be publishing this
			// log info AND NO MORE OR NO LESS ! As such why do we add columns
			// for datasets & scenarios here ?? !!!
			// STEVENOTE : Agree. There is a reason tho (not necessarily a good
			// one)
			// STEVENOTE : apm_msg_log is a log of messages to do with the
			// status of the datasets. The message being delivered is being used
			// to
			// STEVENOTE : a) update the status of the online pfolio screen
			// (i.e. datasets) . b) trigger a batch update
			// STEVENOTE : The additional cols are being added to give the batch
			// update the info it needs
			// STEVENOTE : We could send 2 messages (one for the dataset status)
			// and another for the batch update but its been optimised into 1
			tAPMLogCopy.addCol("datasets", COL_TYPE_ENUM.COL_TABLE);
			tAPMLogCopy.addCol("scenarios", COL_TYPE_ENUM.COL_TABLE);

			if (Table.isTableValid(tBatchDatasets) == 1)
				tAPMLogCopy.setTable("datasets", 1, tBatchDatasets);

			if (Table.isTableValid(tScenarios) == 1)
				tAPMLogCopy.setTable("scenarios", 1, tScenarios);

			tPublishMsg = Table.tableNew(rvMessageHeader);
			tPublishMsg.setTableName(rvMessageHeader);
			tPublishMsg.addCol(rvMessageHeader, COL_TYPE_ENUM.COL_TABLE);
			tPublishMsg.addRow();
			tPublishMsg.setTable(1, 1, tAPMLogCopy);

			iRetVal = APM_PublishTable(tAPMArgumentTable, "TFE." + rvMessageHeader, tPublishMsg, iMsgType, entityGroupId, -1);
			if (iRetVal == 0)
				APM_PrintErrorMessage(tAPMArgumentTable, "Failed to publish status message");

			tAPMLogCopy.setTable("datasets", 1, Util.NULL_TABLE);
			tAPMLogCopy.setTable("scenarios", 1, Util.NULL_TABLE);

			tPublishMsg.setTable(1, 1, Util.NULL_TABLE);
			tPublishMsg.destroy();
			tAPMLogCopy.destroy();
		}

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_Print
	Description:   This function should only be called by APM_PrintMessage, APM_PrintDebugMessage or APM_PrintErrorMessage
	Parameters:    
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public void APM_Print(int iMsgLevel, Table tAPMArgumentTable, String sProcessingMessage) {

		String sMsg;
		String sLogFilename;
		sMsg = ConsoleLogging.getFullMsgContext(tAPMArgumentTable);

		try {
			sMsg = sMsg + " => " + sProcessingMessage;

			// Write errors & debug messages to the error log
			if (iMsgLevel == cMsgLevelDebug || iMsgLevel == cMsgLevelError) {
				sLogFilename = tAPMArgumentTable.getString("Log File", 1);
				Util.errorWriteString(sLogFilename, sMsg);
			}

			sMsg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ":" + sMsg + "\n";
			OConsole.oprint(sMsg);
		} catch (OException e) {
			printStackTrace("APM_Print", e);
		}
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_PrintMessage
	Description:   Prints out info type messages
	Parameters:    
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public void APM_PrintMessage(Table tAPMArgumentTable, String sProcessingMessage) {
		APM_Print(cMsgLevelInfo, tAPMArgumentTable, sProcessingMessage);
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_PrintDebugMessage
	Description:   Prints out debug type messages if debug enabled
	Parameters:    
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public void APM_PrintDebugMessage(Table tAPMArgumentTable, String sProcessingMessage) throws OException {
		if (tAPMArgumentTable.getInt("Debug", 1) > 0)
			APM_Print(cMsgLevelDebug, tAPMArgumentTable, sProcessingMessage);
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_PrintErrorMessage
	Description:   Prints out error type messages
	Parameters:    
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public void APM_PrintErrorMessage(Table tAPMArgumentTable, String sProcessingMessage) {
		APM_Print(cMsgLevelError, tAPMArgumentTable, "=================================================");
		APM_Print(cMsgLevelError, tAPMArgumentTable, sProcessingMessage);
		APM_Print(cMsgLevelError, tAPMArgumentTable, "=================================================");
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_PrintAndLogErrorMessage
	Description:   Prints out error type messages and sends to DSM
	Parameters:    
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public void APM_PrintAndLogErrorMessage(int iMode, Table tAPMArgumentTable, String sMessage) throws OException {
		APM_PrintErrorMessage(tAPMArgumentTable, sMessage);

		String sJobName = tAPMArgumentTable.getString("Job Name", 1);
		String sPackageName = tAPMArgumentTable.getString("Current Package", 1);
		int entityGroupId = tAPMArgumentTable.getInt("Current Entity Group Id", 1);
		int iScenarioId = tAPMArgumentTable.getInt("Current Scenario", 1);
		int secondaryEntityNum = tAPMArgumentTable.getInt("Current Secondary Entity Num", 1);
		int primaryEntityNum = tAPMArgumentTable.getInt("Current Primary Entity Num", 1);
		int entityVersion = tAPMArgumentTable.getInt("Current Entity Version", 1);
		APM_LogStatusMessage(iMode, 0, cStatusMsgTypeFailure, sJobName, sPackageName, entityGroupId, iScenarioId, secondaryEntityNum, primaryEntityNum, entityVersion, tAPMArgumentTable, Util.NULL_TABLE, sMessage);
	}
	
	/**
	 * Print and log error messages. This method catches all exceptions so it is safe to
	 * use in critical code paths. Much of the APM code uses return values and doesn't
	 * handle exceptions.
	 * 
	 * @param iMode APM mode
	 * @param tAPMArgumentTable APM argument table 
	 * @param sMessage Error/Log message
	 */
	public void APM_PrintAndLogErrorMessageSafe(int iMode, Table tAPMArgumentTable, String sMessage) {
		try {
			APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sMessage);
		} catch (Exception e) {
			String errMsg = String.format("An exception occurred while logging an error message. Exception: '%s'", e.getMessage());
			APM_PrintErrorMessage(tAPMArgumentTable, errMsg);
		}
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_TABLE_TuneGrowth
	Description:   Only tune if no rows > 0 !!
	Parameters:
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public void APM_TABLE_TuneGrowth(Table tDestTable, int iNumRows) throws OException {
		if (iNumRows > 0)
			tDestTable.tuneGrowth(iNumRows);
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_AppendSourceAndDestinationString

	Description:   This function adds a source id and a curved bracketed destination id 

	Parameters:    sSourceId         -  source String
	            sDestinationId    -  destination String to be bracketed
	            sResultString     -  String to append to
	            iCommaFound       -  add a comma before the source String

	Returns:       result String
	-------------------------------------------------------------------------------*/
	public String APM_AppendSourceAndDestinationString(String sSourceId, String sDestinationId, String sResultString, int iCommaFound) throws OException {
		if (iCommaFound != 0) {
			sResultString = sResultString + ",";
		}

		sResultString = sResultString + sSourceId;

		if (Str.len(sDestinationId) > 1) {
			sResultString = sResultString + "(" + sDestinationId + ")";
		}

		return (sResultString);
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_CacheTableAdd

	Description:   Used to add table to cache.

	Parameters:    sCachedName             -  Name to store table under.
	            sRemoveTableMsgSubject  -  Subject to remove table from cache.
	            tToCacheTable           -  Table to cache.
	            tAPMArgumentTable       -  Used for error reporting.

	Returns:       result 1 if all is well.
	-------------------------------------------------------------------------------*/
	public int APM_CacheTableAdd(String sCachedName, String sRemoveTableMsgSubject, Table tToCacheTable, Table tAPMArgumentTable) throws OException {

		XString err_xstring = Str.xstringNew();
		Table tCacheParams;
		int iRetVal = 1;

		tCacheParams = Table.tableNew("CacheTableParams");
		iRetVal = Apm.performOperation(APM_CACHE_TABLE_ADD, 1, tCacheParams, err_xstring);
		if (iRetVal == 0) {
			APM_PrintErrorMessage(tAPMArgumentTable, "Error performing operation APM_CACHE_TABLE_ADD: " + Str.xstringGetString(err_xstring) + "\n");
		} else {
			tCacheParams.getTable(2, 1).setString(1, 1, sCachedName);
			tCacheParams.setTable(2, 2, tToCacheTable);
			tCacheParams.getTable(2, 3).setString(1, 1, sRemoveTableMsgSubject);
			iRetVal = Apm.performOperation(APM_CACHE_TABLE_ADD, 0, tCacheParams, err_xstring);
			if (iRetVal == 0) {
				APM_PrintErrorMessage(tAPMArgumentTable, "Error performing operation APM_CACHE_TABLE_ADD: " + Str.xstringGetString(err_xstring) + "\n");
			}
		}

		Str.xstringDestroy(err_xstring);
		tCacheParams.destroy();

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_CacheTableGet

	Description:   Used to get table from Cache

	Parameters:    sCachedName         -  Cached Name to find table
	            tAPMArgumentTable   -  Used for error reporting.

	Returns:       returns cached table or Util.NULL_TABLE if not found.
	-------------------------------------------------------------------------------*/
	public Table APM_CacheTableGet(String sCachedName, Table tAPMArgumentTable) throws OException {

		XString err_xstring = Str.xstringNew();
		Table tCacheParams;
		Table tCachedTable = Util.NULL_TABLE;
		Table tSafeTable = Util.NULL_TABLE;
		int iRetVal = 1;
		int iCopyRow = 0;

		tCacheParams = Table.tableNew("CacheTableParams");
		iRetVal = Apm.performOperation(APM_CACHE_TABLE_GET, 1, tCacheParams, err_xstring);
		if (iRetVal == 0) {
			Str.xstringDestroy(err_xstring);
			APM_PrintErrorMessage(tAPMArgumentTable, "Error performing operation APM_CACHE_TABLE_GET: " + Str.xstringGetString(err_xstring) + "\n");
			return Util.NULL_TABLE;
		}

		// Set to return ptr to cached table.
		if (tCacheParams.getNumRows() > 0)
			tCacheParams.getTable(2, 2).setInt(1, 1, 0);

		// Set the Cached Name
		if (tCacheParams.getNumRows() > 0)
			tCacheParams.getTable(2, 1).setString(1, 1, sCachedName);

		// Try and Retrieve Cached Table
		iRetVal = Apm.performOperation(APM_CACHE_TABLE_GET, 0, tCacheParams, err_xstring);
		if (iRetVal != 0) {
			iCopyRow = tCacheParams.unsortedFindString("parameter_name", "copy of table", SEARCH_CASE_ENUM.CASE_SENSITIVE);

			if (iCopyRow <= 0) {
				APM_PrintErrorMessage(tAPMArgumentTable, "Error in APM_CacheTableGet, cannot find 'copy of table' table in returned parameters\n");
				Str.xstringDestroy(err_xstring);
				return Util.NULL_TABLE;
			} else {
				// If the table is a copy, we can pass this on safely, otherwise
				// we need to pass a copy on so that
				// the script can delete it...

				int iTableIsCopy = tCacheParams.getTable("parameter_value", iCopyRow).getInt(1, 1);

				if (iTableIsCopy == 1) {
					tSafeTable = tCacheParams.getTable(2, 3);
					tCacheParams.setTable(2, 3, Util.NULL_TABLE);
				} else {
					tCachedTable = tCacheParams.getTable(2, 3);
					tSafeTable = tCachedTable.copyTable();
					tCacheParams.setTable(2, 3, Util.NULL_TABLE);
				}
			}
		}
		// Don't raise error since it probably simply means that the table
		// requested is currently not cached ... this is not an error ...
		// else
		// APM_PrintErrorMessage (tAPMArgumentTable, "Error performing operation
		// APM_CACHE_TABLE_GET: " + Str.xstringGetString(err_xstring) + "\n");

		Str.xstringDestroy(err_xstring);
		tCacheParams.destroy();

		return (tSafeTable);
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_CacheTableDrop

	Description:   Used to remove table from cache.

	Parameters:    sCachedName             -  Name of table to remove.
					tAPMArgumentTable       -  Used for error reporting.

	Returns:       result 1 if all is well.
	-------------------------------------------------------------------------------*/
	public int APM_CacheTableDrop(String sCachedName, Table tAPMArgumentTable) throws OException {

		XString err_xstring = Str.xstringNew();
		Table tCacheParams;
		int iRetVal = 1;

		tCacheParams = Table.tableNew("CacheTableParams");
		iRetVal = Apm.performOperation(APM_CACHE_TABLE_DROP, 1, tCacheParams, err_xstring);
		if (iRetVal == 0) {
			APM_PrintErrorMessage(tAPMArgumentTable, "Error performing operation APM_CacheTableDrop: " + Str.xstringGetString(err_xstring) + "\n");
		} else {
			if (tCacheParams.getNumRows() > 0)
				tCacheParams.getTable(2, 1).setString(1, 1, sCachedName);
			iRetVal = Apm.performOperation(APM_CACHE_TABLE_DROP, 0, tCacheParams, err_xstring);
			// iRetVal of 0 could mean that the table hasn't been setup yet, which is ok for us. 			
		}

		Str.xstringDestroy(err_xstring);
		tCacheParams.destroy();

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_CacheTableInfo

	Description:   Used to get cache

	Parameters:    

	Returns:       result 1 if all is well.
	-------------------------------------------------------------------------------*/
	public int APM_CacheTableInfo(Table tAPMArgumentTable) throws OException {
		XString err_xstring = Str.xstringNew();
		Table tCacheParams;
		int iRetVal = 1;

		tCacheParams = Table.tableNew("CacheTableParams");
		iRetVal = Apm.performOperation(APM_CACHE_TABLE_INFO, 1, tCacheParams, err_xstring);
		if (iRetVal != 0) {
			iRetVal = Apm.performOperation(APM_CACHE_TABLE_INFO, 0, tCacheParams, err_xstring);
			tCacheParams.viewTableForDebugging();
		} else
			APM_PrintErrorMessage(tAPMArgumentTable, "Error performing operation APM_CACHE_TABLE_INFO: " + Str.xstringGetString(err_xstring) + "\n");

		tCacheParams.destroy();
		Str.xstringDestroy(err_xstring);

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_TABLE_LoadFromDbWithSQLCached

	Return Values: Util.NULL_TABLE for failure (0), otherwise a copy of the Table
	-------------------------------------------------------------------------------*/
	public Table APM_TABLE_LoadFromDbWithSQLCached(Table tAPMArgumentTable, String sCachedName, String sRemoveTableMsgSubject, String sWhat, String sFrom, String sWhere) throws OException {
		Table tData;
		int iRetVal;

		iRetVal = 1;

		tData = APM_CacheTableGet(sCachedName, tAPMArgumentTable);
		if (Table.isTableValid(tData) == 0) {
			tData = Table.tableNew(sCachedName);
			iRetVal = APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tData, sWhat, sFrom, sWhere);

			if (iRetVal == 0) {
				APM_PrintErrorMessage(tAPMArgumentTable, "Failed to load data from Db : " + sWhat + " " + sFrom + " " + sWhere);
				tData.destroy();
				return Util.NULL_TABLE;
			}

			iRetVal = APM_CacheTableAdd(sCachedName, sRemoveTableMsgSubject, tData.copyTable(), tAPMArgumentTable);
			if (iRetVal == 0) {
				APM_PrintErrorMessage(tAPMArgumentTable, "Failed to cache APM data table");
				tData.destroy();
				return Util.NULL_TABLE;
			}
		}

		return tData;
	}

	public String APM_DatasetTypeIdToName(Table tAPMArgumentTable, int iDatasetTypeId, boolean batchMode) throws OException {
		Table tDatasetTypeTable;
		int iDatasetTypeRow;
		String retStr;

		if (batchMode)
			APM_CacheTableDrop("APMDatasetTypeTable", tAPMArgumentTable);

		tDatasetTypeTable = APM_TABLE_LoadFromDbWithSQLCached(tAPMArgumentTable, "APMDatasetTypeTable", "TFE.METADATA.CHANGED", "*", "apm_dataset_type", "1=1");

		if (Table.isTableValid(tDatasetTypeTable) == 0) {
			APM_PrintErrorMessage(tAPMArgumentTable, "Failed to load apm_dataset_type table");
			return "";
		}

		iDatasetTypeRow = tDatasetTypeTable.unsortedFindInt("apm_dataset_id", iDatasetTypeId);
		if (iDatasetTypeRow > 0) {
			retStr = tDatasetTypeTable.getString("apm_dataset_name", iDatasetTypeRow);
			tDatasetTypeTable.destroy();
			return retStr;
		}
		APM_PrintErrorMessage(tAPMArgumentTable, "Failed to find dataset type Id " + iDatasetTypeId + " in apm_dataset_type table");
		tDatasetTypeTable.destroy();
		return "";
	}

	public int APM_DatasetTypeNameToId(Table tAPMArgumentTable, String sDatasetTypeName, boolean batchMode) throws OException {
		Table tDatasetTypeTable;
		int iDatasetTypeRow;
		int retInt;

		if (batchMode)
			APM_CacheTableDrop("APMDatasetTypeTable", tAPMArgumentTable);

		tDatasetTypeTable = APM_TABLE_LoadFromDbWithSQLCached(tAPMArgumentTable, "APMDatasetTypeTable", "TFE.METADATA.CHANGED", "*", "apm_dataset_type", "1=1");

		if (Table.isTableValid(tDatasetTypeTable) == 0) {
			APM_PrintErrorMessage(tAPMArgumentTable, "Failed to load apm_dataset_type table");
			return -1;
		}

		iDatasetTypeRow = tDatasetTypeTable.unsortedFindString("apm_dataset_name", sDatasetTypeName, SEARCH_CASE_ENUM.CASE_SENSITIVE);
		if (iDatasetTypeRow > 0) {
			retInt = tDatasetTypeTable.getInt("apm_dataset_id", iDatasetTypeRow);
			tDatasetTypeTable.destroy();
			return retInt;
		}
		APM_PrintErrorMessage(tAPMArgumentTable, "Failed to find dataset type '" + sDatasetTypeName + "' in apm_dataset_type table");
		tDatasetTypeTable.destroy();
		return -1;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_DBASE_RunProc
	Description:   deadlock protected version of the fn
	Parameters:      As per DBase.runProc
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public int APM_DBASE_RunProc(Table tAPMArgumentTable, String sp_name, Table arg_table) throws OException {
		int iRetVal = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

		int numberOfRetriesThusFar = 0;
		Boolean spoofLockDealProc = false; 
		int spoofLockDealRetryFailures = 0;
		int spoofLockDealRetrySleepTime = 0;

		// only for LockDeal/LockEntity, spoof failure if relevant environment variables are set
		if (sp_name.equalsIgnoreCase("user_apm_lock_deal") || sp_name.equalsIgnoreCase("user_apm_lock_entity"))
		{
			if (Str.isEmpty(Util.getEnv("AB_APM_DEV_LOCKDEAL_SP_RETRY_FAILURES")) != 1 )
			{
				try
				{
					spoofLockDealRetryFailures = Integer.parseInt(Util.getEnv("AB_APM_DEV_LOCKDEAL_SP_RETRY_FAILURES").trim());
					spoofLockDealRetryFailures = spoofLockDealRetryFailures < 0 ? 0 : spoofLockDealRetryFailures;				
				}
				catch (NumberFormatException nfe)
				{
					spoofLockDealRetryFailures = 0;				
				}
				
				spoofLockDealProc = (spoofLockDealRetryFailures > 0);
			}

			if (Str.isEmpty(Util.getEnv("AB_APM_DEV_LOCKDEAL_SP_RETRY_SLEEP_TIME")) != 1)
			{
				try
				{
					spoofLockDealRetrySleepTime = Integer.parseInt(Util.getEnv("AB_APM_DEV_LOCKDEAL_SP_RETRY_SLEEP_TIME").trim());
					spoofLockDealRetrySleepTime = spoofLockDealRetrySleepTime < 0 ? 0 : spoofLockDealRetrySleepTime;				
				}
				catch (NumberFormatException nfe)
				{
					spoofLockDealRetrySleepTime = 0;				
				}
			}
			if (spoofLockDealRetryFailures > 0) {
				String message = String.format("LockDeal stored procedure spoofing ON. Spoofing retry failures - %1$d, sleep time - %2$d.", spoofLockDealRetryFailures, spoofLockDealRetrySleepTime);
				APM_PrintMessage(tAPMArgumentTable, message);
			}

		}

		do {
			// for error reporting further down
			String message = null;

			try {
				if (spoofLockDealProc && numberOfRetriesThusFar  < spoofLockDealRetryFailures) {
					iRetVal = OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt();
					Debug.sleep(spoofLockDealRetrySleepTime * 1000);
				}
				else {
					iRetVal = DBase.runProc(sp_name, arg_table);
				}
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

					APM_PrintMessage(tAPMArgumentTable, message);
					
					Debug.sleep(numberOfRetriesThusFar * 1000);
				} else {
					// not retryable, just leave
					break;
				}
			}
		} while (numberOfRetriesThusFar < MAX_NUMBER_OF_DB_RETRIES);
		
		if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			String message = "DBase.runProc() of " + sp_name + " failed";
			APM_PrintErrorMessage(tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, message));
		}

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_DBASE_RunProcFillTable
	Description:   deadlock protected version of the fn
	Parameters:      As per DBase.runProcFillTable
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public static int APM_DBASE_RunProcFillTable(String sp_name, Table arg_table) throws OException {

		int iRetVal = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

		int numberOfRetriesThusFar = 0;
		do {
			// for error reporting further down
			String message = null;
			
			try {
				iRetVal = DBase.runProcFillTable(sp_name, arg_table, arg_table);
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

					message = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ":" + message + "\n";
					OConsole.oprint(message);
					
					Debug.sleep(numberOfRetriesThusFar * 1000);
				} else {
					// not retryable, just leave
					break;
				}
			}
		} while (numberOfRetriesThusFar < MAX_NUMBER_OF_DB_RETRIES);

		if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			String message = "DBase.runProc() of " + sp_name + " failed";
			message = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ":" + DBUserTable.dbRetrieveErrorInfo(iRetVal, message) + "\n";
			OConsole.oprint(message);
		}

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_TABLE_LoadFromDBWithSQL
	Description:   deadlock protected version of the fn
	Parameters:      As per TABLE_LoadFromDBWithSQL
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
    public int APM_TABLE_LoadFromDbWithSQL(Table tAPMArgumentTable, Table table, String what, String from,
                                           String where) throws OException {
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

                    APM_PrintMessage(tAPMArgumentTable, message);

                    Debug.sleep(numberOfRetriesThusFar * 1000);
                } else {
                    // not a retryable error, leave
                    break;
                }
            }
        } while (numberOfRetriesThusFar < MAX_NUMBER_OF_DB_RETRIES);

        if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
            String message = "DBaseTable.loadFromDbWithSQL failed ";
            APM_PrintErrorMessage(tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, message));
        }

        return iRetVal;
    }

	/**
	 * Run ads status script to fill caches
	 * 
	 * @throws OException
	 */
	public void executeServiceStatus(Table tAPMArgumentTable) throws OException {

		// only attempt if we are running ADS
		if (useADS(tAPMArgumentTable) == false || isActiveDataAnalyticsService(tAPMArgumentTable))
			return;

		try {
            ADS_GatherServiceStatus serviceStatus = new ADS_GatherServiceStatus();
            serviceStatus.execute();
			APM_PrintMessage(tAPMArgumentTable, "Finished gathering service status 'ADS_GatherServiceStatus'");
		} catch (Exception t) {
			APM_PrintErrorMessage(tAPMArgumentTable, "Failed to gather service status 'ADS_GatherServiceStatus'\n" + t);
			String message = getStackTrace(t);
			APM_PrintErrorMessage(tAPMArgumentTable, message + "\n");
		}
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_FindBadUpdates

	Description:   This routine finds any bad updates & sets them in the main argument table for use later

	Parameters:    iMode - batch or otherwise
	           tAPMArgumentTable - main argument table

	Return Values: SUCCESS or FAILURE
	-------------------------------------------------------------------------------*/
	public int APM_FindBadUpdates(int iMode, int iServiceId, Table tAPMArgumentTable, Table tEntityGroups) throws OException {
		int iRetVal;
		Table tEntityInfo, tDbError, tUpdateErrors;

		tDbError = Table.tableNew();
		iRetVal = APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tDbError, "distinct primary_entity_num, entity_group_id, secondary_entity_num, package", "apm_msg_log", "primary_entity_num >= 0 AND msg_type = 2 AND service_id = "
		        + iServiceId);
		if (iRetVal != 0) {
			if (iMode != cModeBatch) {
				// use the Filtered table as we are in the pfolio loop
				tEntityInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1);

				// this is called from the pfolio loop and therefore we may have this table already
				// clean it up if thats the case - we need to check again as otherwise we have a timing hole due to old data
				tUpdateErrors = tAPMArgumentTable.getTable("Bad Updates", 1);
				if (Table.isTableValid(tUpdateErrors) == 1)
					tUpdateErrors.destroy();

				tUpdateErrors = tDbError.cloneTable();
				tUpdateErrors.select(tEntityInfo, "primary_entity_num", "primary_entity_num GT 0");
				tUpdateErrors.select(tDbError, "entity_group_id, secondary_entity_num, package", "primary_entity_num EQ $primary_entity_num");
				tUpdateErrors.deleteWhereValue("secondary_entity_num", 0);
				tAPMArgumentTable.setTable("Bad Updates", 1, tUpdateErrors);
				tDbError.destroy();
			} else {
				tAPMArgumentTable.setTable("Bad Updates", 1, tDbError);
			}
		} else {
			APM_PrintErrorMessage(tAPMArgumentTable, "Error loading bad updates from apm_msg_log table\n");
			tDbError.destroy();
		}

		return (iRetVal);
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_ClearMsgLogForBadUpdates

	Description:   This routine clears the message log for a bad update & sends out a message

	Parameters:    iMode - m_APMUtils.cModeBatch, m_APMUtils.cModeApply, m_APMUtils.cModeBackout
	           entityGroupID - Current entity group being processed (-1 if N/A)
	           sPackageName - if blank, then all packages (blank for updates & when clearing global messages)
	           tAPMArgumentTable - argt to APM sub-scripts


	Return Values: None
	-------------------------------------------------------------------------------*/
	public int APM_ClearMsgLogForBadUpdates(int iMode, int entityGroupId, int iScenarioId, String sPackageName, Table tAPMArgumentTable) throws OException {
		Table tBadUpdates, tPfolio, tClearQueue;
		Table tEntityInfo = Util.NULL_TABLE;
		int row, primaryEntityNum, secondaryEntityNum, iServiceID, iRetVal = 1, prevEntityGroupId;
		int prevEntityGroupIdCol = 0, primaryEntityNumCol = 0;
		String sRowPackageName, sProcessingMessage, sServiceName;
		EntityType entityType = GetCurrentEntityType(tAPMArgumentTable);

		// the service ID for this RUN
		iServiceID = tAPMArgumentTable.getInt("service_id", 1);

		if (iMode != cModeBatch) {
			Table tEntityGroupIds = Util.NULL_TABLE;
			iRetVal = APM_FindBadUpdates(iMode, iServiceID, tAPMArgumentTable, tEntityGroupIds);
		}

		sServiceName = tAPMArgumentTable.getString("service_name", 1);
		tBadUpdates = tAPMArgumentTable.getTable("Bad Updates", 1);

		if (iMode == cModeBlockUpdate || iMode == cModeBackoutAndApply) {
 			tEntityInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1);
			prevEntityGroupIdCol = tEntityInfo.getColNum("prev_internal_portfolio");
			primaryEntityNumCol = tEntityInfo.getColNum("primary_entity_num");
 		}

		// add the log_status to the table
		if (iMode == cModeBlockUpdate) {
			// in this instance look at the filtered table as that will contain the 
			// list of updates for the entity groups - this is called within entity group loop for updates
			if (tBadUpdates != Util.NULL_TABLE && Table.isTableValid(tBadUpdates) != 0)
				tBadUpdates.select(tEntityInfo, "log_status", "primary_entity_num EQ $primary_entity_num");
		}

		APM_PrintDebugMessage(tAPMArgumentTable, "Starting to clear bad updates from apm_msg_log");

		/* set up the table */
		tPfolio = Table.tableNew("params");
		tPfolio.addCol("service_id", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("primary_entity_num", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("completion_msg", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("package", COL_TYPE_ENUM.COL_STRING);
		tPfolio.addRow();
		tPfolio.setInt("service_id", 1, iServiceID);
		tPfolio.setInt("completion_msg", 1, 0); /* don't delete completion messages */

		tPfolio.setInt("entity_group_id", 1, entityGroupId);
		tPfolio.setString("package", 1, sPackageName);

		/* set up the table for deleting out of the queue */
		tClearQueue = Table.tableNew("params");
		tClearQueue.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
		tClearQueue.addCol("secondary_entity_num", COL_TYPE_ENUM.COL_INT);
		tClearQueue.addRow();
		tClearQueue.setString("service_name", 1, sServiceName);

		boolean checkPreviousEntityGroupId = (prevEntityGroupIdCol > 0 && primaryEntityNumCol > 0 && entityType == EntityType.DEAL && Table.isTableValid(tEntityInfo) != 0);
  
 		for (row = 1; row <= tBadUpdates.getNumRows(); row++) {
			// skip the bad updates for any of the block that failed
			// we do not want to clear these as theres still a problem
			if (iMode == cModeBlockUpdate) {
				if (tBadUpdates.getInt("log_status", row) == OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt())
					continue;
			}

 			prevEntityGroupId = 0;
 			
 			primaryEntityNum = tBadUpdates.getInt("primary_entity_num", row);
 			
 			// Has the deal been moved from one portfolio to another portfolio?
 			if (checkPreviousEntityGroupId) {
				int entityInfoRow = tEntityInfo.unsortedFindInt(primaryEntityNumCol, primaryEntityNum);
				if (entityInfoRow > 0) {
					prevEntityGroupId = tEntityInfo.getInt(prevEntityGroupIdCol, entityInfoRow);
				}
 			}
 			
 			// Check that we really do care about this one & skip if we don't
 			if (prevEntityGroupId == 0 && entityGroupId != tBadUpdates.getInt("entity_group_id", row))
 				continue;
 
			sRowPackageName = tBadUpdates.getString("package", row);

			// If no package specified, then let's clear all the bad 
			// updates for this entity group ID. This will be the
			// case for updates. For batches the bad updates are
			// cleared as the batch end operation completes.
			if (Str.len(sPackageName) < 1)
				tPfolio.setString("package", 1, sRowPackageName);
			else {
				// If the package doesn't match ... try the next row
				if (Str.equal(sPackageName, sRowPackageName) == 0)
					continue;
			}

			secondaryEntityNum = tBadUpdates.getInt("secondary_entity_num", row);

 			// If the portfolio for the deal has changed then clear the message for the previous portfolio.
 			if (prevEntityGroupId > 0) {
 				tPfolio.setInt("entity_group_id", 1, prevEntityGroupId);
 			}
 
			/* clear from table */
			tPfolio.setInt("primary_entity_num", 1, primaryEntityNum);
			iRetVal = APM_DBASE_RunProc(tAPMArgumentTable, "USER_clear_apm_msg_log", tPfolio);

 			// Return the entity group back to its original value.
 			if (prevEntityGroupId > 0) {
 				tPfolio.setInt("entity_group_id", 1, entityGroupId);
 			}
 
			/*
			 * if we are running from the service then make sure that the bad
			 * updates are deleted from the queue
			 */
			if (iRetVal == 1 && iMode == cModeBatch && entityGroupId != 0) {
				tClearQueue.setInt("secondary_entity_num", 1, secondaryEntityNum);
				iRetVal = APM_DBASE_RunProc(tAPMArgumentTable, "USER_apm_clear_failure_from_q", tClearQueue);
				if (iRetVal == 0)
					APM_PrintErrorMessage(tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() of USER_apm_clear_failure_from_q failed for num : ")
					        + Str.intToStr(primaryEntityNum));
			} else if (iRetVal == 0) {
				APM_PrintErrorMessage(tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() of USER_clear_apm_msg_log failed for num : ")
				        + Str.intToStr(primaryEntityNum));
			}

			if (iRetVal != 0) {
 				// If the portfolio on the deal changed then we should clear any bad updates for the previous portfolio.
 				int logStatusEntityGroupId = (prevEntityGroupId > 0 ? prevEntityGroupId : entityGroupId);
 				
				/* send message */
				sProcessingMessage = "Cleared Error Messages in log for : " + primaryEntityNum;
				APM_LogStatusMessage(iMode, 0, cStatusMsgTypeRerun, "", sRowPackageName, logStatusEntityGroupId, iScenarioId, secondaryEntityNum, primaryEntityNum, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
				APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);
			}
		}

		tPfolio.destroy();
		tClearQueue.destroy();

		if (iRetVal != 0)
			APM_PrintDebugMessage(tAPMArgumentTable, "Finished clearing bad updates from apm_msg_log");

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_setupJobReturnt
	Description:   Keep argument table in synch - called in several places
	Return Values: 
	-------------------------------------------------------------------------------*/
	public void APM_setupJobReturnt(Table jobResults) throws OException {

		if (jobResults.getColNum("job_num") < 1)
			jobResults.addCol("job_num", COL_TYPE_ENUM.COL_INT);
		if (jobResults.getColNum("job_name") < 1)
			jobResults.addCol("job_name", COL_TYPE_ENUM.COL_STRING);
		if (jobResults.getColNum("entity_group_id") < 1)
			jobResults.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
		if (jobResults.getColNum("ret_val") < 1)
			jobResults.addCol("ret_val", COL_TYPE_ENUM.COL_INT);
		if (jobResults.getColNum("error_message") < 1)
			jobResults.addCol("error_message", COL_TYPE_ENUM.COL_STRING);
		if (jobResults.getColNum("Package Data Tables") < 1)
			jobResults.addCol("Package Data Tables", COL_TYPE_ENUM.COL_TABLE);
		if (jobResults.getColNum("Block Update Failures") < 1)
			jobResults.addCol("Block Update Failures", COL_TYPE_ENUM.COL_TABLE);
		if (jobResults.getColNum("update") < 1)
			jobResults.addCol("update", COL_TYPE_ENUM.COL_INT);
		if (jobResults.getColNum("Batch Failures") < 1)
			jobResults.addCol("Batch Failures", COL_TYPE_ENUM.COL_TABLE);

		if (jobResults.getNumRows() < 1)
			jobResults.addRow();
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_TABLE_LoadFromDbWithWhatWhere
	Description:   deadlock protected version of the fn
	Parameters:      As per TABLE_LoadFromDBWithSQL
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
    public int APM_TABLE_LoadFromDbWithWhatWhere(Table tAPMArgumentTable, Table table, String db_tablename,
                                                 Table id_list, String what, String where) throws OException {
        int iRetVal = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

        int numberOfRetriesThusFar = 0;
        do {
        	// for error reporting further down
        	String message = null;
        	
            try {
                iRetVal = DBaseTable.loadFromDbWithWhatWhere(table, db_tablename, id_list, what, where);
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

                    APM_PrintMessage(tAPMArgumentTable, message);
                    
                    Debug.sleep(numberOfRetriesThusFar * 1000);
                } else {
                    // not retryable, leave
                    break;
                }
            }
        } while (numberOfRetriesThusFar < MAX_NUMBER_OF_DB_RETRIES);

        if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
            String message = "DBaseTable.loadFromDbWithWhatWhere failed ";
            APM_PrintErrorMessage(tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, message));
        }

        return iRetVal;
    }

	public int APM_TABLE_QueryInsertN(Table tAPMArgumentTable, Table tTable, String sColumn) throws OException {

		int iQueryId = APM_TABLE_QueryInsert(tAPMArgumentTable, tTable, tTable.getColNum(sColumn));
		if (iQueryId <= 0)
			APM_PrintErrorMessage(tAPMArgumentTable, "Failed to create query ID in APM_TABLE_QueryInsertN.  TableName = " + tTable.getTableName() + ", Column = "
			        + sColumn);

		return iQueryId;
	}

    public int APM_TABLE_QueryInsert(Table tAPMArgumentTable, Table tTable, int iColumn) throws OException {
    int iQueryId = 0;

    int numberOfRetriesThusFar = 0;
    do {
        try {
            iQueryId = Query.tableQueryInsert(tTable, iColumn);
        } catch (OException exception) {
            OLF_RETURN_CODE olfReturnCode = exception.getOlfReturnCode();

            if (olfReturnCode == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR) {
                numberOfRetriesThusFar++;
                
                String message = String.format("Query execution retry %1$d of %2$d [%3$s]. Check the logs for possible deadlocks.", numberOfRetriesThusFar, APM_Utils.MAX_NUMBER_OF_DB_RETRIES, exception.getMessage());
                APM_PrintMessage(tAPMArgumentTable, message);
                
                Debug.sleep(numberOfRetriesThusFar * 1000);                
            } else {
                // not retryable, just leave
                break;
            }
        }
    } while (iQueryId == 0 && numberOfRetriesThusFar < MAX_NUMBER_OF_DB_RETRIES);

    if (iQueryId <= 0) {
        String message = "Failed to create query ID in APM_TABLE_QueryInsert.  TableName = "
                + tTable.getTableName() + ", Colnum = " + iColumn;
        APM_PrintErrorMessage(tAPMArgumentTable, message);
    }

    return iQueryId;
}    

	/*-------------------------------------------------------------------------------
	Name:          getStackTrace
	Description:   returns the stack trace
	Return Values: 
	-------------------------------------------------------------------------------*/
	public String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		t.printStackTrace(pw);
		pw.flush();
		sw.flush();
		return sw.toString();
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_IsLaterEntityVersionProcessingInQueue
	Description:   checks whether a later version of an entity is being processed
	               or has finished processing in the queue
	Parameters:    
	Return Values:   true or false (true if a later version is present in the queue)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public boolean APM_IsLaterEntityVersionProcessingInQueue(Table tAPMArgumentTable, int primaryEntityNum, int secondaryEntityNum, int entityVersion, int serviceId) throws OException {
		String sCachedTableName = "tran_check_params";

		Table entityCheck = Table.getCachedTable(sCachedTableName);
		if (entityCheck == Util.NULL_TABLE || Table.isTableValid(entityCheck) == 0) {
			entityCheck = Table.tableNew("params");
			entityCheck.addCol("primary_entity_num", COL_TYPE_ENUM.COL_INT);
			entityCheck.addCol("secondary_entity_num", COL_TYPE_ENUM.COL_INT);
			entityCheck.addCol("entity_version", COL_TYPE_ENUM.COL_INT);
			entityCheck.addCol("service_id", COL_TYPE_ENUM.COL_INT);
			entityCheck.addRow();
			Table.cacheTable(sCachedTableName, entityCheck);
		}

		entityCheck.setInt(1, 1, primaryEntityNum);
		entityCheck.setInt(2, 1, secondaryEntityNum);
		entityCheck.setInt(3, 1, entityVersion);
		entityCheck.setInt(4, 1, serviceId);

		int retVal = APM_DBASE_RunProc(tAPMArgumentTable, "USER_apm_is_later_entity_in_q", entityCheck);
		if (retVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
			APM_PrintErrorMessage(tAPMArgumentTable, "Call to USER_apm_is_later_entity_in_q stored proc failed");
			return false;
		}

		Table results = Table.tableNew("results");
		retVal = DBase.createTableOfQueryResults(results);
		if (retVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
			APM_PrintErrorMessage(tAPMArgumentTable, "Unable to retrieve results from call to USER_apm_is_later_entity_in_q stored proc failed");
			results.destroy();
			return false;
		}

		int laterEntityVersion = results.getInt("later_version_num", 1);
		int laterSecondaryEntityNum = results.getInt("later_secondary_num", 1);
		results.destroy();

		if (laterEntityVersion > 0 || laterSecondaryEntityNum > 0)
			return true;
		else
			return false;
	}

	private Table APM_GetPackageProperties(Table tAPMArgumentTable) throws OException {

		int iRetVal = 1;

		// get package properties from tfe_package_defs and cache
		String sCacheTableName = "APM_PackageProperties";
		Table tPackageProperties = APM_CacheTableGet(sCacheTableName, tAPMArgumentTable);
		if (Table.isTableValid(tPackageProperties) == 0) {
			tPackageProperties = Table.tableNew("package_properties");
			iRetVal = APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tPackageProperties, "package_name, incremental_updates, package_version", "tfe_package_defs", "on_off_flag=1 or entity_type='ActiveDataAnalytics'");

			if (iRetVal == 0) {
				APM_PrintErrorMessage(tAPMArgumentTable, "Failed to load incremental update and version properties from tfe_package_defs");
				tPackageProperties.destroy();
				return Util.NULL_TABLE;
			}

			tPackageProperties.sortCol("package_name");

			iRetVal = APM_CacheTableAdd(sCacheTableName, "TFE.METADATA.CHANGED", tPackageProperties.copyTable(), tAPMArgumentTable);
			if (iRetVal == 0) {
				APM_PrintErrorMessage(tAPMArgumentTable, "Failed to cache incremental update and version properties from tfe_package_defs.");
				tPackageProperties.destroy();
				return Util.NULL_TABLE;
			}
		}

		return tPackageProperties;
	}

	public void APM_PrintPackageVersion(Table tAPMArgumentTable, String packageName) throws OException {

		Table tPackageProperties = APM_GetPackageProperties(tAPMArgumentTable);
		if (Table.isTableValid(tPackageProperties) != 0) {

			int packageRow = tPackageProperties.findString("package_name", packageName, SEARCH_ENUM.FIRST_IN_GROUP);
			if (packageRow > 0) {
				String packageVersion = tPackageProperties.getString("package_version", packageRow);
				APM_PrintMessage(tAPMArgumentTable, "Package: " + packageName + ". Version = " + packageVersion);
			}
		}
	}

	public boolean APM_IsIncrementalOnForPackage(Table tAPMArgumentTable, String sPackageName) throws OException {

		Table tPackageProperties = APM_GetPackageProperties(tAPMArgumentTable);
		if (Table.isTableValid(tPackageProperties) != 0) {
			// look up the incremental setting
			int packageRow = tPackageProperties.findString("package_name", sPackageName, SEARCH_ENUM.FIRST_IN_GROUP);
			if (packageRow > 0) {
				if (tPackageProperties.getInt("incremental_updates", packageRow) == 1) {
					return true;
				} else {
					return false;
				}
			} else {
				APM_PrintErrorMessage(tAPMArgumentTable, "Failed to find incremental update settings in tfe_package_defs for package " + sPackageName);
			}
		}

		return false;
	}

	/// Partial list of illegal filename characters...
	//private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };

	/// Sanitise filenames so that we replace illegal characters.  The regex below contains what could be a partial list of illegal characters, but will cover the accidental inclusions we often see.
	public static String sanitiseFilename(String input) {
		String output = input.replaceAll(".*[/\n\r\t\0\f`?*\\\\<>|\":].*", "");

		if (output == "")
			output = "santisedFilename.txt";

		return (output);
	}

	/// Get the current datetime as a timestamp.
	public static String getTimestamp() {
		Calendar now = Calendar.getInstance();
		StringBuilder date = new StringBuilder();

		date.append(now.get(Calendar.YEAR));
		date.append("_");
		date.append(now.get(Calendar.MONTH) + 1); // Months are 0-indexed.
		date.append("_");
		date.append(now.get(Calendar.DAY_OF_MONTH));
		date.append("_");
		date.append(now.get(Calendar.HOUR_OF_DAY));
		date.append("_");
		date.append(now.get(Calendar.MINUTE));
		date.append("_");
		date.append(now.get(Calendar.SECOND));
		date.append("_");
		date.append(now.get(Calendar.MILLISECOND));

		return (date.toString());
	}

	// Get the current datetime as an ODateTime object.
	public static ODateTime getODateTime() throws OException {
		Calendar now = Calendar.getInstance();
		ODateTime dateTime = ODateTime.dtNew();

		// Get the julian date		
		StringBuilder date = new StringBuilder();
		date.append(now.get(Calendar.YEAR));
		date.append(String.format("%02d", now.get(Calendar.MONTH) + 1));
		date.append(String.format("%02d", now.get(Calendar.DAY_OF_MONTH)));
		dateTime.setDate(OCalendar.convertYYYYMMDDToJd(date.toString()));

		// Get the seconds elapsed
		Calendar midnight = (Calendar) now.clone();
		midnight.set(Calendar.HOUR_OF_DAY, 0);
		midnight.set(Calendar.MINUTE, 0);
		midnight.set(Calendar.SECOND, 0);
		midnight.set(Calendar.MILLISECOND, 0);
		long seconds = now.getTimeInMillis() - midnight.getTimeInMillis();
		dateTime.setTime((int) seconds / 1000);

		return (dateTime);
	}

	// Get the current tfe_interface_api version...
	public static int getTfeInterfaceApiVersion() throws OException {
		XString errorString = null;
		Table params;
		int version = 0;

		try {
			// Get the error string in here.
			errorString = Str.xstringNew();

			// Using an old function purely to retrieve the version number
			params = Table.tableNew();
			if (Apm.performOperation(APM_Utils.APM_RTPE_CLEAR, 1, params, errorString) == 0) {
				OException exception = new OException("Failure in APM_PerfromOperation(m_APMUtils.APM_RTPE_CLEAR) : " + Str.xstringGetString(errorString) + "\n");
				exception.fillInStackTrace();
				throw (exception);
			}

			version = params.getTable(2, 3).getInt(1, 1); // row 3 is the version of the tfe_interface_api
		} finally {
			Str.xstringDestroy(errorString);
		}

		return (version);
	}

	public boolean skipSimulationForUpdates(int iMode, Table tAPMArgumentTable) throws OException {
		// if its a block update check whether its just full of backouts
		// if so then its not necessary to run the reval
		boolean skipSimulation = false;
		if (iMode == cModeBlockUpdate) {
			Table tEntityInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1);
			int iNumRows = tEntityInfo.getNumRows();
			skipSimulation = true;
			for (int iEntityRow = 1; iEntityRow <= iNumRows; iEntityRow++) {
				if (tEntityInfo.getInt("update_mode", iEntityRow) != cModeBackout) {
					skipSimulation = false;
					break;
				}
			}
		} else if (iMode == cModeBackout)
			skipSimulation = true;

		return skipSimulation;
	}

	private int SaveArgtForRerun(Table tAPMArgumentTable, Table mainArgt, int entityGroupId) throws OException {

		// save the arg table as AFS file
		String serviceName = mainArgt.getString("service_name", 1);
		String afsFileName = "APMFailedBatchArgt:" + serviceName + ":" + GetCurrentEntityType(tAPMArgumentTable) + "-" + entityGroupId;
		if (Afs.saveTable(mainArgt, afsFileName) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			return 0;
		} else {
			return 1;
		}
	}

	public boolean SaveArgtForRerunIfSweeperOn(int iMode, Table mainArgt, Table tAPMArgumentTable, int entityGroupId) throws OException {

		boolean tfeSettingForSaveArgt = (APM_GetOverallPackageSettingInt(tAPMArgumentTable, "batch_fail_sweep_mode", 1, 0, 0) == 1);
		if (iMode == cModeBatch && tfeSettingForSaveArgt) {
			// save the argt
			APM_PrintMessage(tAPMArgumentTable, "Batch for Entity Group: " + GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId) + " failed. Saving details for re-run\n");
			if (SaveArgtForRerun(tAPMArgumentTable, mainArgt, entityGroupId) != 1) {
				APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Unable to save APM Service argument table to AFS file\n");
				return false;
			}
		}
		return true;
	}

	private int RemoveSweeperEntriesForEntityGroup(Table tAPMArgumentTable, Table mainArgt, int entityGroupId) throws OException {

		// save the arg table as AFS file
		String serviceName = mainArgt.getString("service_name", 1);
		String afsFileName = "APMFailedBatchArgt:" + serviceName + ":" + GetCurrentEntityType(tAPMArgumentTable) + "-" + entityGroupId;
		
		Table afsTable = Table.tableNew("afs_table");
		boolean fileExists = true;
		
		// Check if the file that we're trying to delete exists.
		try {
			if (DBaseTable.loadFromDbWithSQL(afsTable, "afs_id", "abacus_file_system", "filename='" + afsFileName + "'") != 0) {
				int afsNumRows = afsTable.getNumRows();
				if (afsNumRows < 1) {
					// The file doesn't exist.
					fileExists = false;
				}
			}
		} finally {
			afsTable.destroy();
		}
		
		// If the file doesn't exist then we're done.
		// We wanted the file delete and it is, so all is okay.
		if (!fileExists) {
			return 1;
		}
		
		try {
			if (Afs.deleteTable(afsFileName, 0) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				return 0;
			} else {
				return 1;
			}
		}
		catch (Exception ex) {
			String error = ex.getMessage();
            APM_PrintMessage(tAPMArgumentTable, error);
			
            return 0;                  
		}
	}

	public boolean DeleteSweeperEntriesForEntityGroup(int iMode, Table mainArgt, Table tAPMArgumentTable, int entityGroupId) throws OException {

		boolean tfeSettingForSaveArgt = (APM_GetOverallPackageSettingInt(tAPMArgumentTable, "batch_fail_sweep_mode", 1, 0, 0) == 1);
		if (iMode == cModeBatch && tfeSettingForSaveArgt) {
			// save the argt
			APM_PrintMessage(tAPMArgumentTable, "Clearing Sweeper Entries for " + GetCurrentEntityType(tAPMArgumentTable).getEntityGroup() + " : " + GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId)
			        + "\n");
			if (RemoveSweeperEntriesForEntityGroup(tAPMArgumentTable, mainArgt, entityGroupId) != 1) {
				APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Unable to delete APM Service argument table to AFS file\n");
				return false;
			}
		}
		return true;
	}

	private Table getRevalTypesTable(int iMode) {
		Table revalTypes = null;
		int retVal = 0;

		try {
			revalTypes = Table.getCachedTable("reval_types");

			if (iMode == cModeBatch) {
				Table.destroyCachedTable("reval_types");
				revalTypes = Util.NULL_TABLE;
			}

			if (Table.isTableValid(revalTypes) == 0) {
				revalTypes = Table.tableNew("reval_types");
				retVal = DBaseTable.loadFromDbWithSQL(revalTypes, "name, id_number", "reval_type", "1=1");

				revalTypes.sortCol("name");
				if (retVal > 0)
					Table.cacheTable("reval_types", revalTypes);
			}
		} catch (OException e) {
			printStackTrace("getRevalTypesTable ", e);
		}

		return (revalTypes);
	}

	public int getPriorRevalTypeForPackages(int iMode, Table tAPMArgumentTable, boolean refreshForService) throws OException {

		String sServiceName = tAPMArgumentTable.getString("service_name", 1);
		Table priorRevalTypeSettings = Table.getCachedTable("prior_reval_types");

		int row = -1;
		if (Table.isTableValid(priorRevalTypeSettings) != 0)
			row = priorRevalTypeSettings.findString("service_name", sServiceName, SEARCH_ENUM.FIRST_IN_GROUP);

		if (refreshForService && row > 0) {
			priorRevalTypeSettings.delRow(row);
			row = -1;
		}

		int priorRevalType = -1;
		if (Table.isTableValid(priorRevalTypeSettings) != 0 && row > 0)
			return priorRevalTypeSettings.getInt("prior_reval_type", row);
		else {
			int run_as_avs_script = APM_GetOverallPackageSettingInt(tAPMArgumentTable, "run_as_avs_script", 0, 1, 0);
			if (run_as_avs_script == 1)
				priorRevalType = 0; // GENERAL

			Table configuration = getConfigurationTable(tAPMArgumentTable).copyTable();
			configuration.sortCol("setting_name");

			// get the list of reval types from the db
			Table revalTypes = getRevalTypesTable(iMode);

			int lastPriorRevalType = -1;
			Table tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);
			for (int iRow = 1; iRow <= tPackageDetails.getNumRows(); iRow++) {
				String sPackageName = tPackageDetails.getString("package_name", iRow);
				int firstRow = configuration.findString("setting_name", "prior_reval_type", SEARCH_ENUM.FIRST_IN_GROUP);
				int lastRow = configuration.findString("setting_name", "prior_reval_type", SEARCH_ENUM.LAST_IN_GROUP);

				int currPriorRevalType = 1; // EOD

				if (firstRow > 0) {
					for (int iLoopCtr = firstRow; iLoopCtr <= lastRow; iLoopCtr++) {
						String sSettingPackageName = configuration.getString("package_name", iLoopCtr);
						if (sSettingPackageName != null && sPackageName.equals(sSettingPackageName)) {
							String strPriorRevalType = configuration.getString("value", iLoopCtr);
							// now convert into the lookup
							int revalRow = revalTypes.findString("name", strPriorRevalType, SEARCH_ENUM.FIRST_IN_GROUP);
							if (revalRow > 0) {
								currPriorRevalType = revalTypes.getInt("id_number", revalRow);
								APM_PrintMessage(tAPMArgumentTable, "Found reval type: " + strPriorRevalType + " in reval types table.");
							} else
								APM_PrintMessage(tAPMArgumentTable, "Cannot find reval type: " + strPriorRevalType + " in reval types table.  Defaulting to EOD");
						}
					}
				}
				if (lastPriorRevalType != -1 && currPriorRevalType != lastPriorRevalType) {
					APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Conflicting prior Reval Types specified on packages.  Reval type " + priorRevalType
					        + " compared to " + currPriorRevalType);
					priorRevalType = -1; // conflicting entries
					break;
				}

				lastPriorRevalType = currPriorRevalType;
				priorRevalType = currPriorRevalType;
			}

			configuration.destroy();

			if (Table.isTableValid(priorRevalTypeSettings) == 0) {
				priorRevalTypeSettings = Table.tableNew("prior_reval_types");
				priorRevalTypeSettings.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
				priorRevalTypeSettings.addCol("prior_reval_type", COL_TYPE_ENUM.COL_INT);
				Table.cacheTable("prior_reval_types", priorRevalTypeSettings);
			}

			row = priorRevalTypeSettings.addRow();
			priorRevalTypeSettings.setString("service_name", row, sServiceName);
			priorRevalTypeSettings.setInt("prior_reval_type", row, priorRevalType);
			priorRevalTypeSettings.sortCol("service_name");
			APM_PrintMessage(tAPMArgumentTable, "Setting prior reval type = " + priorRevalType);
			priorRevalTypeSettings = Util.NULL_TABLE;

			return priorRevalType;
		}
	}

	public int APM_StrToInt(String sVal) throws OException {

		// convert String to int
		int iVal = -1;
		try {
			iVal = Str.strToInt(sVal);
		} catch (Exception e) {
			return -1;
		}

		return iVal;
	}

	public Table APM_GetColumnsForTable(int iMode, Table tAPMArgumentTable, String sPackageName, String sDataTableName) throws OException {
		String sErrMessage = "";
		int iRetVal = 1;

		String sCachedName = "APM_Generic_" + sDataTableName + "_TableCols";
		Table tUniquePackageDataTableCols = APM_CacheTableGet(sCachedName, tAPMArgumentTable);
		if (Table.isTableValid(tUniquePackageDataTableCols) == 0) {
			// Create the function parameters and run the the stored proc
			Table tArgs = Table.tableNew("params");
			tArgs.addCol("sPackageName", COL_TYPE_ENUM.COL_STRING);
			tArgs.addCol("sDataTableName", COL_TYPE_ENUM.COL_STRING);
			tArgs.addRow();
			tArgs.setString("sPackageName", 1, sPackageName);
			tArgs.setString("sDataTableName", 1, sDataTableName);
			
			if (isActiveDataAnalyticsService(tAPMArgumentTable))
			{
				//tArgs.addCol("sSummaryGroupInfo", COL_TYPE_ENUM.COL_STRING);
				//tArgs.setString("sSummaryGroupInfo", 1, "('Portfolio', 'Int Bus Unit', 'Int Legal Entity', 'Ext Legal Entity', 'Instr Type')");
				iRetVal = APM_DBASE_RunProc(tAPMArgumentTable, "USER_apm_get_ada_pkg_tbl_cols", tArgs);
			}
			else
			{
				iRetVal = APM_DBASE_RunProc(tAPMArgumentTable, "USER_apm_get_pkg_tbl_cols", tArgs);
			}

			Table tPackageDataTableCols = Table.tableNew("Package Data Tables");
			if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
				sErrMessage = "APM_GetColumnsForTable call to USER_apm_get_pkg_tbl_cols stored proc failed";
			else {
				iRetVal = DBase.createTableOfQueryResults(tPackageDataTableCols);
				if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
					sErrMessage = "APM_GetColumnsForTable unable to retrieve results from call to USER_apm_get_pkg_tbl_cols stored proc failed";
				} else {
					// now filter out the entries we don't want (unique the table)
					tUniquePackageDataTableCols = Table.tableNew("Package Data Tables");
					;
					tUniquePackageDataTableCols.select(tPackageDataTableCols, "DISTINCT, column_name, result_column_name, result_enum_name, column_type", "column_type GT -1");
					APM_CacheTableAdd(sCachedName, "TFE.METADATA.CHANGED", tUniquePackageDataTableCols.copyTable(), tAPMArgumentTable);
					tPackageDataTableCols.destroy();
				}
			}
			tArgs.destroy();
		}

		if (iRetVal == 0) {
			APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
			return null;
		} else
			return tUniquePackageDataTableCols;
	}

	/**
	 * Returns the <b>unique</b> workflow run id for the batch with the specified service id.
	 * 
	 * @param tAPMArgumentTable
	 *            APM arguments table.
	 * @param serviceId
	 *            The service id to get unique run id corresponding to the instance
	 *            currently
	 *            running the batch.
	 * 
	 * @return Workflow run id for the provided service id.
	 * 
	 * @throws OException
	 */
	public int getWorkflowRunId(Table tAPMArgumentTable, int serviceId) throws OException {
		int workflowRunId = Integer.MIN_VALUE;

		Table workflowRunIdTable = Table.tableNew();

		final int serviceGroupType = 16;
		final String runIdColumnName = "run_id";

		String what = "child_table.wflow_run_id as " + runIdColumnName;
		String from = "wflow_running child_table join wflow_running parent_table on child_table.parent_wflow_run_id = parent_table.wflow_run_id";
		String where = "child_table.service_group_type = " + serviceGroupType + " and parent_table.job_cfg_id = " + serviceId;

		int iRetVal = APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, workflowRunIdTable, what, from, where);

		if (iRetVal > 0) {
			workflowRunId = workflowRunIdTable.getInt(runIdColumnName, 1);
		}

		return workflowRunId;
	}

	/**
	 * Returns the start time of the batch currently running with the specified service id.
	 * 
	 * @param tAPMArgumentTable
	 *            APM arguments table.
	 * @param serviceId
	 *            The service id to get start time corresponding to the instance currently
	 *            running the batch.
	 * 
	 * @return Start time for the provided service id, currently running.
	 * 
	 * @throws OException
	 */
	public ODateTime getWorkflowStartTime(Table tAPMArgumentTable, int serviceId) throws OException {
		ODateTime startTime = ODateTime.dtNew();

		final int serviceGroupType = 16;
		final String startTimeColumnName = "start_time";

		Table workflowRunIdTable = Table.tableNew();
		workflowRunIdTable.addCol(startTimeColumnName, COL_TYPE_ENUM.COL_DATE_TIME);

		String what = "child_table.start_time as " + startTimeColumnName;
		String from = "wflow_running child_table join wflow_running parent_table on child_table.parent_wflow_run_id = parent_table.wflow_run_id";
		String where = "child_table.service_group_type = " + serviceGroupType + " and parent_table.job_cfg_id = " + serviceId;

		int iRetVal = APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, workflowRunIdTable, what, from, where);

		if (iRetVal > 0) {
			startTime = workflowRunIdTable.getDateTime(startTimeColumnName, 1);
		}

		return startTime;
	}

	/**
	 * Returns the current machine's hostname.
	 * 
	 * @throws OException
	 */
	public String getHostname() throws OException {
		String hostname = "<unknown host>";

		Table infoTable = Ref.getInfo();

		hostname = infoTable.getString("hostname", 1);

		infoTable.destroy();

		return hostname;
	}

	/**
	 * Extracts the {@code op_services_run_id} value from the supplied APM arguments table.
	 * 
	 * @param argumentsTable
	 *            APM op services arguments table.
	 * 
	 * @return The {@code op_services_run_id} or {@code -1} in case of failure.
	 */
	public int getOpServicesRunId(Table argumentsTable) {
		int opServicesRunId = -1;

		try {
			Table globalFilteredEntityInfoTable = argumentsTable.getTable("Global Filtered Entity Info", 1);

			int minOpServicesRunId = getMinimumInt(globalFilteredEntityInfoTable, "op_services_run_id");

			if (minOpServicesRunId != Integer.MIN_VALUE) {
				opServicesRunId = minOpServicesRunId;
			}
		} catch (OException exception) {
			this.APM_PrintErrorMessage(argumentsTable, "Exception retrieving the 'Global Filtered Entity Info' table from APM arguments table: "
			        + exception.getMessage());

			String message = this.getStackTrace(exception);
			this.APM_PrintErrorMessage(argumentsTable, message + "\n");

			try {
				ConsoleCaptureWrapper.getInstance(argumentsTable).flush();
			} catch (Throwable throwable) {
				message = "Error attempting to flush console capture wrapper, " + throwable.getMessage();
				this.APM_PrintErrorMessage(argumentsTable, message + "\n");
			}
		}

		return opServicesRunId;
	}

	/**
	 * Returns the minimun of a the values in the column {@code columnName} from the specified
	 * {@code table}.
	 * <p/>
	 * The type of column is assumed to be {@code int}.
	 * 
	 * @param table
	 *            Table to process.
	 * @param columnName
	 *            Name of the column to get the minimum value from.
	 * 
	 * @return Minimum value in the colum {@code columnName}; if a minimum could not be found,
	 *         returns {@link Integer#MIN_VALUE}.
	 * 
	 * @throws OException
	 *             in case of a failure in querying the table.
	 */
	public int getMinimumInt(Table table, String columnName) throws OException {
		int min = Integer.MIN_VALUE;

		int numberOfRows = table.getNumRows();

		if (numberOfRows > 1) {
			int minOpServicesRunId = Integer.MAX_VALUE;
			for (int rowNumber = 1; rowNumber <= numberOfRows; rowNumber++) {
				int runId = table.getInt(columnName, rowNumber);

				if (runId < minOpServicesRunId) {
					minOpServicesRunId = runId;
				}
			}

			min = minOpServicesRunId == Integer.MAX_VALUE ? min : minOpServicesRunId;
		} else {
			min = table.getInt(columnName, 1);
		}

		return min;
	}
/*
	public ODateTime getOpServiceStartTime(int opServicesRunId, Table apmArgumentsTable) throws OException {
		final String startTimeColumnName = "start_time";

		String what = "row_creation as " + startTimeColumnName;
		String from = "op_services_log_detail";
		String where = "op_services_run_id = " + opServicesRunId;

		Table opServicesTable = Table.tableNew();
		opServicesTable.addCol(startTimeColumnName, COL_TYPE_ENUM.COL_DATE_TIME);

		int returnCode = APM_TABLE_LoadFromDbWithSQL(apmArgumentsTable, opServicesTable, what, from, where);

		ODateTime startTime = ODateTime.dtNew();
		if (returnCode > 0) {
			startTime = opServicesTable.getDateTime(startTimeColumnName, 1);
		}

		return startTime;
	}
*/
	/**
	 * Creates and returns a new instance of {@link IApmStatisticsLogger}.
	 * 
	 * @param scope
	 *            Loggign scope.
	 * 
	 * @param context
	 *            Logging context. Used in naming the log file.
	 * 
	 * @return Instance of statistics logger.
	 * 
	 * @throws OException
	 *             in case logger creation failed.
	 */
	public IApmStatisticsLogger newLogger(Scope scope, String context) throws OException {
		IApmStatisticsLogger apmLogger = null;

		try {
			apmLogger = new ApmStatisticsLogger(scope, context);
		} catch (ApmStatisticsLoggerInstantiationException exception) {
			throw new OException(exception);
		}

		return apmLogger;
	}

	/**
	 * Closes the specified {@code logger}.
	 * 
	 * @param logger
	 *            Logger to close.
	 * @param apmArguments
	 *            APM arguments table. Used in error logging.
	 * 
	 * @return
	 */
	public boolean closeLogger(IApmStatisticsLogger logger, Table apmArguments) {
		boolean closedOk = false;

		try {
			logger.close();

			closedOk = true;
		} catch (Exception exception) {
			this.APM_PrintErrorMessage(apmArguments, "Exception when closing logger: " + exception.getMessage());

			String message = this.getStackTrace(exception);
			this.APM_PrintErrorMessage(apmArguments, message + "\n");

			try {
				ConsoleCaptureWrapper.getInstance(apmArguments).flush();
			} catch (Throwable throwable) {
				message = "Error attempting to flush console capture wrapper, " + throwable.getMessage();
				this.APM_PrintErrorMessage(apmArguments, message + "\n");
			}

			closedOk = false;
		}

		return closedOk;
	}

	/**
	 * Clears lock files.
	 */
	public void clearLockFiles() {
		LoggingUtilities.clearLockFiles();
	}
	
	/**
	 * Converts a map of metrics to a table {@link IApmStatisticsLogger}.
	 * 
	 * @param metrics
	 *            a map of metrics.
	 * 
	 * @return a table of metrics.
	 * 
	 */
	public Table convertMapToStatisticsTable(Map<String, Object> metrics) {
		
		Table tStatistics = null;
		try {
			//Create the statistics table
			tStatistics = Table.tableNew();
			tStatistics.addCol("key", COL_TYPE_ENUM.COL_STRING);
			tStatistics.addCol("value", COL_TYPE_ENUM.COL_STRING);

			//Now add each metric to the table
			if (metrics != null) {
				for (Entry<String, Object> pair : metrics.entrySet()) {
					String key = pair.getKey();
					String value = pair.getValue().toString();
					int row = tStatistics.addRow();
					tStatistics.setString("key", row, key);
					tStatistics.setString("value", row, value);
				}
			}
			
		} catch (OException e) {
			// If exception thrown just don't return any stats
		}
		
		return tStatistics;
	}
	
	/**
	 * Converts metrics in a table to a map of metrics {@link IApmStatisticsLogger}.
	 * 
	 * @param tStatistics
	 * 			a table of metrics.
	 *
	 * @return a map of metrics.
	 */
	public Map<String,Object> convertStatisticsTableToMap(Table tStatistics) {

		Map<String,Object> metrics = new HashMap<String,Object>();
		try {
			for (int row = 1; row <= tStatistics.getNumRows(); row++) {
				String key = tStatistics.getString("key", row);
				String value = tStatistics.getString("value", row);
				metrics.put(key,value);
			}
		} catch (OException e) {
			// If exception thrown just don't return any stats
		}
		
		return metrics;
	}

	public int APM_DestroyJobQueryID(int mode, Table tAPMArgumentTable) throws OException {

	   int jobQueryID = tAPMArgumentTable.getInt("job_query_id", 1);

	   if ( jobQueryID > 0)
	   {
	   	   Query.clear(jobQueryID);
	   	   tAPMArgumentTable.setInt("job_query_id", 1, 0);
	   }

	   return 1;
    }

	public int InsertIncrementalOldStyleStatistic(Table tAPMArgumentTable, int secondaryEntityNum, int entityVersion, String sPackageName) throws OException {

		int iRetVal = 1;
		
		Table tStats = Table.tableNew("params");
		tStats.addCol("secondary_entity_num", COL_TYPE_ENUM.COL_INT);
		tStats.addCol("entity_version", COL_TYPE_ENUM.COL_INT);
		tStats.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
		tStats.addCol("package_name", COL_TYPE_ENUM.COL_STRING);
		tStats.addCol("module_name", COL_TYPE_ENUM.COL_STRING);
		tStats.addCol("pid", COL_TYPE_ENUM.COL_INT);
		tStats.addCol("hostname", COL_TYPE_ENUM.COL_STRING);
		
		String serviceName = tAPMArgumentTable.getString("service_name", 1);
		String moduleName = Ref.getModuleName();
		int pid = Ref.getProcessId();
		String hostname = getHostname();
		
		tStats.addRow();
		tStats.setInt(1, 1, secondaryEntityNum);
		tStats.setInt(2, 1, entityVersion);
		tStats.setString(3, 1, serviceName);
		tStats.setString(4, 1, sPackageName);
		tStats.setString(5, 1, moduleName);
		tStats.setInt(6, 1, pid);
		tStats.setString(7, 1, hostname);
		
		iRetVal = APM_DBASE_RunProc(tAPMArgumentTable, "USER_apm_insert_entity_info", tStats);

		if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
			APM_PrintErrorMessage(tAPMArgumentTable, "Error inserting incremental stats for service : " + serviceName + " : "
					+ DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() failed") + "\n");
			iRetVal = 0;
		}

		tStats.destroy();

	
		return 1;
	}

	/**
	 * Iff in QA mode (i.e., the environment variable {@code AB_APM_QA_MODE} is set to {@code TRUE})
	 * this method will save the APM-enriched simulation results according to
	 * whichever implementation of {@link IScript} is found and loaded.
	 * <br/>
	 * If the QA results path (set in the environment variable {@code APM_QA_RESULTS_PATH}) is not
	 * found or is not a directory, nothing will be done.
	 * <br/>
	 * If a suitable {@link IScript} implementation (set either by the environment variable
	 * {@code APM_QA_RESULTS_HANDLER_CLASS} or, if not found, by the Java system property
	 * {@code apmQaResultsHandlerClass}; if none of the previous are found, then it'll default to a
	 * hard-coded class name) cannot be loaded and instantiated, nothing will be done.
	 * 
	 * @param apmArgumentsTable
	 *            The APM arguments table.
	 * @param serviceName
	 *            Name of the service producing the results.
	 * @param jobName
	 *            The name of the job processing/saving the results.
	 * @param mode
	 *            The APM Service execution mode.
	 */
	public void qaModeSaveResultToFile(final Table apmArgumentsTable, final String serviceName, final String jobName, final int mode) {
		try {
			String qaModeValue = Util.getEnv("AB_APM_QA_MODE");
			boolean isQaModeEnabled = "TRUE".equalsIgnoreCase(qaModeValue);

			if (isQaModeEnabled) {
				String qaResultsPathValue = Util.getEnv("APM_QA_RESULTS_PATH");
				if (qaResultsPathValue != null) {
					OConsole.message("[QA MODE] APM_QA_RESULTS_PATH = " + qaResultsPathValue + System.lineSeparator());

					Path qaResultsPath = Paths.get(qaResultsPathValue);

					if (Files.exists(qaResultsPath) && Files.isDirectory(qaResultsPath)) {
						String qaResultsHandlerClassName = Util.getEnv("APM_QA_RESULTS_HANDLER_CLASS");

						if (qaResultsHandlerClassName == null) {
							qaResultsHandlerClassName = System.getProperty("apmQaResultsHandlerClass");
						}

						if (qaResultsHandlerClassName == null) {
							qaResultsHandlerClassName = "com.olf.apm.qa.QaSimResultsHtmlHandler";
						}

						Class<?> qaResultsHandlerClass = Class.forName(qaResultsHandlerClassName);
						Constructor<?> constructor = null;
						try {
							constructor = qaResultsHandlerClass.getConstructor();
							Object newObject = constructor.newInstance();

							IScript qaResultsHandler = (IScript) newObject;

							Table packageDetailsTable = apmArgumentsTable.getTable("Package Details", 1);

							int numberOfPackages = packageDetailsTable.getNumRows();
							for (int packageRow = 1; packageRow <= numberOfPackages; packageRow++) {
								Table dataTables = packageDetailsTable.getTable("package_data_tables", packageRow);
								Table dataTableCopy = dataTables.getTable(1, 1).copyTable();

								OConsole.message("[QA MODE] Processing " + dataTableCopy.getTableName() + " for job " + jobName + "..."
								        + System.lineSeparator());

								IContainerContext context = new IContainerContext() {
									@Override
									public Table getReturnTable() {
										return null;
									}

									@Override
									public Table getArgumentsTable() {
										Table table = null;

										try {
											table = new Table("qa_args");

											table.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
											table.addCol("job_name", COL_TYPE_ENUM.COL_STRING);
											table.addCol("mode", COL_TYPE_ENUM.COL_INT);
											table.addCol("qa_results_path", COL_TYPE_ENUM.COL_STRING);
											table.addCol("results_table", COL_TYPE_ENUM.COL_TABLE);

											int rowIndex = table.addRow();

											table.setString("service_name", rowIndex, serviceName);
											table.setString("job_name", rowIndex, jobName);
											table.setInt("mode", rowIndex, mode);
											table.setString("qa_results_path", rowIndex, qaResultsPath.toString());
											table.setTable("results_table", rowIndex, dataTableCopy);
										} catch (OException exception) {
											String message = "[QA MODE] Failed to create the context for the QA script: " + exception.getMessage()
											        + System.lineSeparator();
											APM_PrintErrorMessage(apmArgumentsTable, message);
										}

										return table;
									}
								};

								qaResultsHandler.execute(context);

								OConsole.message("[QA MODE] Finished processing " + dataTableCopy.getTableName() + " for job " + jobName + "!"
								        + System.lineSeparator());
							}
						} catch (Exception exception) {
							String message = "[QA MODE] Failed to load the results handler class: " + exception.getMessage() + System.lineSeparator();
							OConsole.message(message);
						}
					}
				}
			}
		} catch (Exception exception) {
			String message = "[QA MODE] Failed to save the APM results to file: " + exception.getMessage() + System.lineSeparator();
			OConsole.message(message);
		}
	}
	
        public boolean isActivePositionManagerService(Table tAPMArgumentTable) throws OException
        {
                return (currentServiceType == ServiceType.APM);
        }

	public boolean isActiveDataAnalyticsService(Table tAPMArgumentTable) throws OException
	{
		return (currentServiceType == ServiceType.ADA);
	}
}
