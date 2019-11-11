/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.apm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import standard.apm.statistics.IApmStatisticsLogger;
import standard.apm.statistics.Scope;
import standard.include.APM_Utils;
import standard.include.APM_Utils.EntityType;

import com.olf.openjvs.Apm;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.XString;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public class SQLITE_DataStoreOps
{
	private APM_Utils m_APMUtils;

	private static SQLITE_DataStoreOps self;
	
	private SQLITE_DataStoreOps() {
		m_APMUtils = new APM_Utils();
	}

	public static SQLITE_DataStoreOps instance() throws OException {
		if (self == null) {
			self = new SQLITE_DataStoreOps();
		}

		return self;
	}

	public int Update(int iUpdateMode, int iMode, int entityGroupId, String sPackageName, int iDatasetTypeId, int serviceID, boolean datasetKeyInAnotherService, String sJobName, Table tMainArgt, Table tPackageDataTables, Table argt) throws OException 
	{
		final Calendar startTimestamp = Calendar.getInstance();
		XString err_xstring;
		int iRetVal = 1;
		int iOldEntityGroupId = 0;
		int primaryEntityNum = 0;
		int secondaryEntityNum = 0;
		boolean bIgnoreEntityUpdate = false;
		Table tParams = null;
		Table tNotificationMessage;
		Table tServiceInfo;

		// ARR environment variable to force test for entity to use secondary entity num instead of primary entity num
		// if it is the MTM & Notional Package
		String strUseSecondaryEntityNum;
		boolean bUseSecondaryEntityNum = false;
		
		if (sPackageName.equalsIgnoreCase("MTM & Notional")) {
			if (Str.isEmpty(Util.getEnv("AB_APM_ARR_TEST_MTM_AS_TRANNUM")) == 1)
				bUseSecondaryEntityNum = false;
			else {
				strUseSecondaryEntityNum = Util.getEnv("AB_APM_ARR_TEST_MTM_AS_TRANNUM");
				if (strUseSecondaryEntityNum.equals("1") == true)
					bUseSecondaryEntityNum = true;
				else
					bUseSecondaryEntityNum = false;
			}
		}

		ODateTime entityDBInsertionTime = null;
		ODateTime updateScriptStartTime = ODateTime.dtNew();
		updateScriptStartTime = APM_Utils.getODateTime();

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
		String updateScriptStartTimestamp = dateFormat.format(startTimestamp.getTime());

		primaryEntityNum = argt.getInt("Current Primary Entity Num", 1);

		IApmStatisticsLogger entityGroupIncrementalLogger = null;
		String serviceName = argt.getString("service_name", 1);
		entityGroupIncrementalLogger = m_APMUtils.newLogger(Scope.ENTITYGROUP_INCREMENTAL, serviceName);
		entityGroupIncrementalLogger.start();

		//Put the statistics logged at service and entityGroup scope into the entityGroup incremental logs
		Table tAllStatistics = argt.getTable("Statistics", 1);
		int row = tAllStatistics.unsortedFindString("Scope", "EntityGroup", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		if (row > 0) {
			Table tEntityGroupStatistics = tAllStatistics.getTable("Scope_Statistics", row);
			Map<String,Object> entityGroupContexts = m_APMUtils.convertStatisticsTableToMap(tEntityGroupStatistics);
			entityGroupIncrementalLogger.setAll(entityGroupContexts);
		}

		entityGroupIncrementalLogger.setMetric("entityGroupIncrementalStartTime", updateScriptStartTimestamp);
		int pid = Ref.getProcessId();
		entityGroupIncrementalLogger.setContext("entityGroupIncrementalPid", String.valueOf(pid));
		entityGroupIncrementalLogger.setContext("package", sPackageName);
		entityGroupIncrementalLogger.setMetric("primary_entity_num", String.valueOf(primaryEntityNum));

		// if we are in BACKOUT mode then we need to check whether the dataset key is being monitored by
		// another service.  If it is then we can skip the backout as the other service will do a backout and apply.
		// if its moving completely out of the monitored set then carry on and do the backout
		if ( iUpdateMode == m_APMUtils.APM_BACKOUT )
		{
			if ( datasetKeyInAnotherService == true)
			{
				m_APMUtils.APM_PrintMessage(argt, "Entity moving service. Skipping BACKOUT for entity: " + primaryEntityNum);
				return 1;
			}
		}

		err_xstring = Str.xstringNew();

		tParams = Table.tableNew();
		iRetVal = Apm.performOperation(iUpdateMode, 1, tParams, err_xstring);
		if (iRetVal == 0)
		{
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, argt, "Error performing APM operation (Mode: " +iUpdateMode+ "): " + Str.xstringGetString(err_xstring) + "\n");
			return 0;
		}

		if (iUpdateMode == m_APMUtils.APM_APPLY || iUpdateMode == m_APMUtils.APM_BACKOUT)
		{
			// if no previous entity group specified then we need to work it out from the dataset control table
			iOldEntityGroupId = argt.getInt("Previous Entity Group Id", 1);

			// log the entity info
			int entityRow=0;
			Table tEntityInfo = argt.getTable("Filtered Entity Info", 1);
			if (Table.isTableValid(tEntityInfo)==1) {
				entityRow = tEntityInfo.unsortedFindInt("primary_entity_num", primaryEntityNum);
				if (entityRow > 0) {
					if (iUpdateMode==m_APMUtils.APM_APPLY)
						APM_EntityJobOps.instance().APM_PrintEntityInfoRow(argt, tMainArgt, tEntityInfo, "Inserting Entity: ",entityRow);
					else
						APM_EntityJobOps.instance().APM_PrintEntityInfoRow(argt, tMainArgt, tEntityInfo, "Removing Entity: ",entityRow);

					secondaryEntityNum = tEntityInfo.getInt("secondary_entity_num", entityRow);
					entityGroupIncrementalLogger.setMetric("secondary_entity_num", String.valueOf(secondaryEntityNum));
					
					if ( m_APMUtils.GetCurrentEntityType(argt) == EntityType.DEAL )
					{
					   int insType = tEntityInfo.getInt("ins_type", entityRow);
					   entityGroupIncrementalLogger.setMetric("instype", String.valueOf(insType));
					   int fromStatus = tEntityInfo.getInt("from_status", entityRow);
					   entityGroupIncrementalLogger.setMetric("from", String.valueOf(fromStatus));
					   int toStatus = tEntityInfo.getInt("to_status", entityRow);
					   entityGroupIncrementalLogger.setMetric("to", String.valueOf(toStatus));
					   int updateType = tEntityInfo.getInt("incr_tmaint_type", entityRow);
					   entityGroupIncrementalLogger.setMetric("updatetype", String.valueOf(updateType));
					   int versionNumber = tEntityInfo.getInt("entity_version", entityRow);
					   entityGroupIncrementalLogger.setMetric("versionnum", String.valueOf(versionNumber));
					   int personnelId = tEntityInfo.getInt("personnel_id", entityRow);
					   entityGroupIncrementalLogger.setMetric("personnel_id", String.valueOf(personnelId));
					   entityDBInsertionTime = ODateTime.dtNew();
					   entityDBInsertionTime = tEntityInfo.getDateTime("last_update", 1);
					   ODateTime initiatorScriptStartTime = argt.getDateTime("Initiator_Script_Start_Time", 1);
					   int queueProcessLag = entityDBInsertionTime.computeTotalSecondsInGMTDateRange(initiatorScriptStartTime);
					   entityGroupIncrementalLogger.setMetric("queueProcessLag", String.valueOf(queueProcessLag));
					   entityGroupIncrementalLogger.setMetric("queueProcessLag", String.valueOf(queueProcessLag));
					   String instrumentName = Table.formatRefInt(insType, SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
					   entityGroupIncrementalLogger.setMetric("insname", instrumentName);
					}
					else if ( m_APMUtils.GetCurrentEntityType(argt) == EntityType.NOMINATION )
					{
					   int toStatus = tEntityInfo.getInt("delivery_status", entityRow);
					   entityGroupIncrementalLogger.setMetric("to", String.valueOf(toStatus));
					   int versionNumber = tEntityInfo.getInt("entity_version", entityRow);
					   entityGroupIncrementalLogger.setMetric("versionnum", String.valueOf(versionNumber));
					   int personnelId = tEntityInfo.getInt("personnel_id", entityRow);
					   entityGroupIncrementalLogger.setMetric("personnel_id", String.valueOf(personnelId));
					   entityDBInsertionTime = ODateTime.dtNew();
					   entityDBInsertionTime = tEntityInfo.getDateTime("last_update", 1);
					   ODateTime initiatorScriptStartTime = argt.getDateTime("Initiator_Script_Start_Time", 1);
					   int queueProcessLag = entityDBInsertionTime.computeTotalSecondsInGMTDateRange(initiatorScriptStartTime);
					   entityGroupIncrementalLogger.setMetric("queueProcessLag", String.valueOf(queueProcessLag));
					   entityGroupIncrementalLogger.setMetric("queueProcessLag", String.valueOf(queueProcessLag));
					}
				}
				else
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, argt, "Failed to find Entity "+primaryEntityNum+ " in entity info table in SQLITE_Update()");
			} 
			else
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, argt, "Failed to retrieve entity info table in SQLITE_Update()");     		

			// we have to double check the apm_data and apm_dataset_control table  for a prior update
			// this covers the situation where an entity is changed entity group and then amended again instantly
			// what can happen is that the first update is thrown away and the old entity group is the current entity group
			// in this use case we will get a double count in APM as the position from the old entity group is not backed out
			Table tEntityGroupFromDC = Table.tableNew("old_entity_group_from_dc");
			String sWhere;
			if (bUseSecondaryEntityNum == true) {
				sWhere = "c1.dataset_id = d.dataset_id and d.operation = 1 and d.primary_entity_num= " + secondaryEntityNum + " and c1.package = '" + sPackageName + "' and c1.dataset_type_id = " + iDatasetTypeId;
			}
			else {
				sWhere = "c1.dataset_id = d.dataset_id and d.operation = 1 and d.primary_entity_num= " + primaryEntityNum + " and c1.package = '" + sPackageName + "' and c1.dataset_type_id = " + iDatasetTypeId;
			}
			iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL( argt, tEntityGroupFromDC, 
					"distinct c1.entity_group_id, d.timestamp", 
					"apm_dataset_control c1, apm_data d", 
					sWhere);   

			if (iRetVal == DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
			{
				if (tEntityGroupFromDC.getNumRows() > 0)
				{
					// set old entity group - take the last line as its the most recent
					tEntityGroupFromDC.sortCol(2);
					iOldEntityGroupId = tEntityGroupFromDC.getInt(1, tEntityGroupFromDC.getNumRows() );
					tEntityGroupFromDC.destroy();
				}
			}

			if ( iOldEntityGroupId <= 0 ) // not set as it could not be found
			{
				// set the old entity group to the new one
				iOldEntityGroupId = entityGroupId;
				m_APMUtils.APM_PrintMessage (argt, "Could not find previous entity group. Setting it to the current " + m_APMUtils.GetCurrentEntityType(argt).getEntityGroup() + "! (explicit backouts disabled)");

			}
		}

		// Populate the tParams - common set
		tParams.getTable("parameter_value", 1).setInt("entity_group_id", 1, entityGroupId);
		tParams.setTable(2, 2, argt.getTable("Scenario_List", 1));
		tParams.getTable("parameter_value", 3).setString("package", 1, sPackageName);
		tParams.getTable("parameter_value", 4).setInt("service_id", 1, serviceID);
		tParams.getTable("parameter_value", 5).setInt("dataset_type_id", 1, argt.getInt("dataset_type_id", 1));

		if (iUpdateMode == m_APMUtils.APM_BATCH_WRITE) {
			tParams.getTable("parameter_value", 6).setInt("query", 1, argt.getInt("Job Query", 1));
			tParams.setTable(2, 7, tPackageDataTables);
			tParams.getTable("parameter_value", 8).setInt("tranche", 1, argt.getInt("Tranche", 1));
		} else if (iUpdateMode == m_APMUtils.APM_APPLY) {
			// build params for update perform operation
			tParams.getTable("parameter_value", 6).setInt("query", 1, argt.getInt("Job Query", 1));
			tParams.setTable(2, 7, tPackageDataTables);
			if (bUseSecondaryEntityNum == true) {
				tParams.getTable("parameter_value", 8).setInt("primary_entity_num", 1, secondaryEntityNum);
			}
			else {
				tParams.getTable("parameter_value", 8).setInt("primary_entity_num", 1, primaryEntityNum);
			}
			tParams.getTable("parameter_value", 9).setInt("previous_entity_group_id", 1, iOldEntityGroupId);

			// build the notification message for sending within the
			// tfe_interface_api operation
			// this fn does the table allocation, param destroy
			// later
			// cleans it up
			tNotificationMessage = BuildBackoutsAndApplyMessage(iMode, entityGroupId, iOldEntityGroupId, tMainArgt, argt, tPackageDataTables, sPackageName);

			// set the notification message within the params for
			// the
			// entity apply operation
			tParams.getTable("parameter_value", 10).setTable("notification_message", 1, tNotificationMessage);
			tParams.getTable("parameter_value", 11).setInt("secondary_entity_num", 1, argt.getInt("Current Secondary Entity Num", 1));
			tParams.getTable("parameter_value", 12).setInt("entity_version", 1, argt.getInt("Current Entity Version", 1));
			tParams.getTable("parameter_value", 13).setString("service_name", 1, argt.getString("service_name", 1));

			m_APMUtils.APM_PrintDebugMessage(argt, "Start entity insertion");
		} else {
			// backout !! - therefore we only care about the old pfolio
			tParams.getTable("parameter_value", 1).setInt("entity_group_id", 1, iOldEntityGroupId);
			if (bUseSecondaryEntityNum == true) {
				tParams.getTable("parameter_value", 6).setInt("primary_entity_num", 1, secondaryEntityNum);
			}
			else {
				tParams.getTable("parameter_value", 6).setInt("primary_entity_num", 1, primaryEntityNum);
			}

			// build the notification message for sending within the
			// tfe_interface_api operation
			// this fn does the table allocation, param destroy
			// later
			// cleans it up
			tNotificationMessage = BuildBackoutsAndApplyMessage(iMode, entityGroupId, iOldEntityGroupId, tMainArgt, argt, tPackageDataTables, sPackageName);

			// set the notification message within the params for
			// the
			// entity apply operation
			tParams.getTable("parameter_value", 7).setTable("notification_message", 1, tNotificationMessage);
			// add the previous entity group information to aid in
			// backout
			// operations
			tParams.getTable("parameter_value", 8).setInt("previous_entity_group_id", 1, iOldEntityGroupId);

			tParams.getTable("parameter_value", 9).setInt("secondary_entity_num", 1, argt.getInt("Current Secondary Entity Num", 1));
			tParams.getTable("parameter_value", 10).setInt("entity_version", 1, argt.getInt("Current Entity Version", 1));
			tParams.getTable("parameter_value", 11).setString("service_name", 1, argt.getString("service_name", 1));

			m_APMUtils.APM_PrintDebugMessage(argt, "Start backout operation");
		}

		// if its a backout mode then it could be an amendment.  Two use cases
		// 1) Amendment out of service and NO other service is interested - this is truly a backout
		// 2) Amendment out of service INTO another service 
		//    - throw this one away as it will be picked up by the other service.  
		//      If we process it here it could result in an erroneous backout (after the other service has done an apply)
		//      and missing entity on client
		// Don't do this in ADS land as its unnecessary
		serviceID = argt.getInt("service_id", 1);
		if ( iUpdateMode == m_APMUtils.APM_BACKOUT )
		{
			tServiceInfo = Table.tableNew();
			iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(argt, tServiceInfo, "*", "apm_dataset_control, wflow_running", 
					"apm_dataset_control.service_id = wflow_running.job_cfg_id AND package = " + "'" + sPackageName + "'" + " AND entity_group_id = " + entityGroupId + " AND dataset_type_id = " + iDatasetTypeId + " AND service_id != " + serviceID);

			// if no rows then nothing else is picking up the amendment and therefore its truly a backout
			// otherwise forget about the update
			if ( iRetVal != 1 || tServiceInfo.getNumRows() > 0 )
			{
				bIgnoreEntityUpdate = true;
				m_APMUtils.APM_PrintMessage(argt, "IGNORING UPDATE AS IT IS BEING HANDLED BY ANOTHER SERVICE");
			}
			tServiceInfo.destroy();
		}

		if ( bIgnoreEntityUpdate == false)
		{
			// Now perform the operation itself
			iRetVal = Apm.performOperation(iUpdateMode, 0, tParams, err_xstring);
		}

		// Destroy the local parameters but don't destroy the scenario
		// or results tables
		tParams.setTable(2, 2, Util.NULL_TABLE);

		if (iUpdateMode == m_APMUtils.APM_BATCH_WRITE) 
		{
			tParams.setTable(2, 7, Util.NULL_TABLE); // null reporting data
		} 
		else if (iUpdateMode == m_APMUtils.APM_APPLY) 
		{
			tParams.setTable(2, 7, Util.NULL_TABLE); // null reporting data
		}
		// (nothing to null for backout operation

		tParams.destroy();

		boolean succeeded = true;
		if (iRetVal == 0) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, argt, "Error performing APM operation (Mode: " +iUpdateMode+ "): " + Str.xstringGetString(err_xstring) + "\n");
			succeeded = false;
		}
		
		ODateTime updateScriptStopTime = ODateTime.dtNew();
		updateScriptStopTime = APM_Utils.getODateTime();
		if (entityDBInsertionTime != null) {
			int totalElapsedTime = entityDBInsertionTime.computeTotalSecondsInGMTDateRange(updateScriptStopTime);
			entityGroupIncrementalLogger.setMetric("totalElapsedTime", String.valueOf(totalElapsedTime));
		}
		int insertionDuration = updateScriptStartTime.computeTotalSecondsInGMTDateRange(updateScriptStopTime);
		entityGroupIncrementalLogger.setMetric("insertionDuration", String.valueOf(insertionDuration));

		if(succeeded) {
		    entityGroupIncrementalLogger.stop();
		} else {
		    entityGroupIncrementalLogger.abort();
		}

		m_APMUtils.closeLogger(entityGroupIncrementalLogger, argt);

		Str.xstringDestroy(err_xstring);

		return iRetVal;
	}
	
	/*-------------------------------------------------------------------------------
	Name:          BuildBackoutsAndApplyMessage
	Description:
	Parameters:    tPackageDataTables - the memory table that will be sent in to the APM fns as data
	Return Values: None
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	Table BuildBackoutsAndApplyMessage(int iMode, int entityGroupId, int iOldEntityGroupId, Table tMainArgt, 
			Table tAPMArgumentTable, Table tPackageDataTables, String sPackageName)
	throws OException {
		Table tEntityUpdatePublishParams = Util.NULL_TABLE;
		Table tEntityInfo;
		Table tEntityUpdate;
		Table tBackoutEntityNums;
		Table tApplyEntityNums;
		Table tBackoutDetails;

		int iRetVal = 1;
		int enable_message_certifier;

		String sPageName;

		XString err_xstring;

		iRetVal = 1;
		err_xstring = Str.xstringNew();

		/*
		 * JIMNOTE : The update of the APM backout and apply should really be
		 * done as one transaction ... at the moment it is two
		 */

		tBackoutEntityNums = Table.tableNew("APM Backout Nums");
		tApplyEntityNums = Table.tableNew("APM Apply Nums");
		tBackoutDetails = tPackageDataTables.cloneTable();

		tEntityUpdate = Table.tableNew("APM Update");
		tEntityUpdate.addCol("Backouts", COL_TYPE_ENUM.COL_TABLE);
		tEntityUpdate.addCol("Applies", COL_TYPE_ENUM.COL_TABLE);
		tEntityUpdate.addCol("Datasets", COL_TYPE_ENUM.COL_TABLE);
		tEntityUpdate.addCol("msg_type", COL_TYPE_ENUM.COL_INT);
		tEntityUpdate.addCol("timestamp", COL_TYPE_ENUM.COL_DATE_TIME);
		tEntityUpdate.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
		tEntityUpdate.addCol("package", COL_TYPE_ENUM.COL_STRING);
		tEntityUpdate.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
		tEntityUpdate.addCol("PreviousDatasets", COL_TYPE_ENUM.COL_TABLE);
		tEntityUpdate.addCol("BlobFlag", COL_TYPE_ENUM.COL_INT);
		tEntityUpdate.addCol("Blobs", COL_TYPE_ENUM.COL_TABLE);

		tEntityUpdate.addRow();
		tEntityUpdate.setTable("Backouts", 1, tBackoutEntityNums);
		tEntityUpdate.setTable("Applies", 1, tApplyEntityNums);
		tEntityUpdate.setString("package", 1, sPackageName);
		tEntityUpdate.setInt("msg_type", 1, iMode);
		tEntityUpdate.setInt("entity_group_id", 1, entityGroupId);
		tEntityUpdate.setDateTimeByParts("timestamp", 1, OCalendar.getServerDate(), Util.timeGetServerTime());

		tEntityUpdate.setString("service_name", 1, tMainArgt.getString("service_name", 1));

		// build up the select string to make sure the right columns are in the msg
		String msgColumns = "DISTINCT, primary_entity_num(" + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getPrimaryEntity().toLowerCase() + ")";
		if ( m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getSecondaryEntity().length() > 0 )
			msgColumns = msgColumns + ", secondary_entity_num(" + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getSecondaryEntity().toLowerCase() + ")";
			
		msgColumns = msgColumns + ", entity_version(" + m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityVersion().toLowerCase() + ")";

		/*
		 * Build two TABLES ... one of all the secondary entity nums to backout, and one to
		 * send to the APM backout function
		 */
		if (iMode == m_APMUtils.cModeBackout || iMode == m_APMUtils.cModeBackoutAndApply) {
			iRetVal = BuildBackoutDetails(entityGroupId, iOldEntityGroupId, tMainArgt, tPackageDataTables, tBackoutEntityNums, tBackoutDetails, tAPMArgumentTable, msgColumns );
		}

		if (iRetVal == 1 && (iMode == m_APMUtils.cModeApply || iMode == m_APMUtils.cModeBackoutAndApply)) {
			// When doing the block update the Entity Info table contains data for all entities in the block but
			// the update apply and notification is done on per entity basis. The current entity being processed is set by this script
			// in argt table and this what we use here to get data only for that entity 

			int primaryEntityNum = tAPMArgumentTable.getInt("Current Primary Entity Num", 1);		   
			tEntityInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1);
			tApplyEntityNums.select(tEntityInfo, msgColumns, "primary_entity_num EQ " + primaryEntityNum );
			tApplyEntityNums.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
			tApplyEntityNums.setColValInt("entity_group_id", entityGroupId);
			tApplyEntityNums.addCol("service_id", COL_TYPE_ENUM.COL_INT);
			tApplyEntityNums.setColValInt("service_id", tMainArgt.getInt("Ops Defn ID", 1));
		}

		/* if theres nothing to apply or backout then just exit out */
		if (tBackoutEntityNums.getNumRows() == 0 && tApplyEntityNums.getNumRows() == 0) {
			tEntityUpdate.destroy();

			if (Table.isTableValid(tBackoutDetails) != 0)
				tBackoutDetails.destroy(); // no longer used but here for later
			// usage perhaps?

			Str.xstringDestroy(err_xstring);
			return Util.NULL_TABLE;
		}

		/* All Ok so let's publish this update ... */
		if (iRetVal != 0) {
			sPageName = tAPMArgumentTable.getString("RTP Page Prefix", 1) + Str.intToStr(entityGroupId);
			tEntityUpdate.setTableName(sPageName);

			enable_message_certifier = m_APMUtils.APM_GetOverallPackageSettingInt(tAPMArgumentTable, "enable_message_certifier", 1, 0, 1);

			// get the param table for the publish
			tEntityUpdatePublishParams = Table.tableNew("publish_table_as_xml_params");
			if (Apm.performOperation(m_APMUtils.APM_PUBLISH_TABLE_AS_XML, 1, tEntityUpdatePublishParams, err_xstring) != 0) {
				tEntityUpdatePublishParams.setString("publish_xml_msg_subject", 1, sPageName);
				tEntityUpdatePublishParams.setTable("publish_xml_msg_table", 1, tEntityUpdate);
				tEntityUpdatePublishParams.setInt("publish_xml_msg_scope", 1, m_APMUtils.APM_PUBLISH_SCOPE_EXTERNAL); // global
				// message
				tEntityUpdatePublishParams.setInt("publish_xml_msg_table_level", 1, -1); // recurse
				// into
				// all
				// tables
				tEntityUpdatePublishParams.setInt("publish_xml_msg_enable_certifier", 1, enable_message_certifier);
				tEntityUpdatePublishParams.setInt("publish_xml_msg_type", 1, m_APMUtils.cStatusMsgTypeProcessing);
				tEntityUpdatePublishParams.setInt("publish_xml_msg_entity_group", 1, entityGroupId);
				tEntityUpdatePublishParams.setInt("publish_xml_msg_package_id", 1, -1);
				tEntityUpdatePublishParams.setDateTimeByParts("publish_xml_msg_timestamp", 1, OCalendar.getServerDate(), Util.timeGetServerTime());
			} else {
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Error performing operation m_APMUtils.APM_PUBLISH_TABLE_AS_XML: " + Str.xstringGetString(err_xstring) + "\n");

				if (Table.isTableValid(tBackoutDetails) != 0)
					tBackoutDetails.destroy(); // no longer used but here for
				// later usage perhaps?

				Str.xstringDestroy(err_xstring);
				return Util.NULL_TABLE;
			}
		}

		Str.xstringDestroy(err_xstring);

		if (Table.isTableValid(tBackoutDetails) != 0)
			tBackoutDetails.destroy(); // no longer used but here for later
		// usage perhaps?

		return tEntityUpdatePublishParams;
	}

	/*-------------------------------------------------------------------------------
	Name:          BuildBackoutDetails
	Description:
	Parameters:    tPackageDataTables - the memory table that will be sent in to the APM fns as data
	Return Values: None
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	int BuildBackoutDetails(int entityGroupId, int iOldEntityGroupId, Table tMainArgt, Table tPackageDataTables, 
			Table tBackoutEntityNums, Table tBackoutDetails, Table argt, String messageColumns ) throws OException {
		Table tEntityInfo;
		Table tData;
		Table tBackoutTable;
		Table tFirstBackoutTable;
		int iRetVal = 1;
		int iRow;
		String sWhereString;

		/*
		 * JIMNOTE : This whole function needs to be inside the database
		 * transaction really
		 */
		tEntityInfo = argt.getTable("Filtered Entity Info", 1);

		/*
		 * copy the entity to be backed out from the entityinfo - entity group must
		 * match
		 */

		// When doing the block update the Entity Info table contains data for all Entities in the block but
		// the backout is done on per Entity basis. The current Entity being processed is set by this script
		// in argt table and this what we use here to get data only for that Entity

		int primaryEntityNum = argt.getInt("Current Primary Entity Num", 1 );	   
		sWhereString = "entity_group_id EQ " + entityGroupId + " AND primary_entity_num EQ " + primaryEntityNum;

		String backoutMsgColumns = messageColumns + ", old_entity_group_id(entity_group_id)";
		tBackoutEntityNums.select(tEntityInfo, backoutMsgColumns, sWhereString);

		// if the entity group is changed on the amendment sometimes the Entityinfo will have entries under the old rather than new entity group
		if (tBackoutEntityNums.getNumRows() == 0 )
		{
			sWhereString = "entity_group_id EQ " + iOldEntityGroupId + " AND primary_entity_num EQ " + primaryEntityNum;
			tBackoutEntityNums.select(tEntityInfo, backoutMsgColumns, sWhereString);
		}

		tBackoutEntityNums.makeTableUnique();

		//override the entity group with the old entity group passed in
		tBackoutEntityNums.setColValInt("entity_group_id", iOldEntityGroupId);		

		tBackoutEntityNums.addCol("service_id", COL_TYPE_ENUM.COL_INT);
		tBackoutEntityNums.setColValInt("service_id", tMainArgt.getInt("Ops Defn ID", 1));

		if (tBackoutEntityNums.getNumRows() > 0) {
			tBackoutEntityNums.makeTableUnique();

			/* Clone the APM tables to indicate which tables shoud be backed out */
			for (iRow = 1; iRow <= tPackageDataTables.getNumRows(); iRow++) {
				tData = tPackageDataTables.getTable(1, iRow);
				tBackoutTable = tData.cloneTable();
				tBackoutDetails.addRow();
				tBackoutDetails.setTable(1, iRow, tBackoutTable);
			}

			/*
			 * Populate the first APM table with the Entity numbers & entity groups
			 * to back out
			 */
			tFirstBackoutTable = tBackoutDetails.getTable(1, 1);
			tFirstBackoutTable.select(tBackoutEntityNums, "*", m_APMUtils.GetCurrentEntityType(argt).getPrimaryEntity().toLowerCase() + " GT 0");
		}

		return (iRetVal);
	}
	
}
