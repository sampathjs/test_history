/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;

import standard.include.APM_Utils;
import standard.include.APM_Utils.EntityType;
import standard.include.ConsoleLogging;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.fnd.ServicesBase;

public class APM_NominationJobOps
{
	private APM_Utils m_APMUtils;

	private static APM_NominationJobOps self;
	
	private APM_NominationJobOps() {
		m_APMUtils = new APM_Utils();
	}

	public static APM_NominationJobOps instance() throws OException {
		if (self == null) {
			self = new APM_NominationJobOps();
		}

		return self;
	}

	public int APM_EnrichNominationTable(int iMode, Table tAPMArgumentTable, int iQueryId) throws OException {
		int iRetVal = 1;

		if (iMode == m_APMUtils.cModeBatch || iMode == m_APMUtils.cModeDoNothing)
			return 1;

		m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Starting to enrich nomination details");

		Table tEntityInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1);

		if ( tEntityInfo.getColNum("primary_entity_num") < 1)
		   tEntityInfo.addCol("primary_entity_num", COL_TYPE_ENUM.COL_INT);

		if ( tEntityInfo.getColNum("secondary_entity_num") < 1)
		   tEntityInfo.addCol("secondary_entity_num", COL_TYPE_ENUM.COL_INT);

		if ( tEntityInfo.getColNum("entity_version") < 1)
		   tEntityInfo.addCol("entity_version", COL_TYPE_ENUM.COL_INT);

		if ( tEntityInfo.getColNum("entity_group_id") < 1)
		   tEntityInfo.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);

		if ( tEntityInfo.getColNum("old_entity_group_id") < 1)
		   tEntityInfo.addCol("old_entity_group_id", COL_TYPE_ENUM.COL_INT);

		int pipeline_id;
		int delivery_id;
		int version;

		for (int row = 1; row <= tEntityInfo.getNumRows(); row++) 
		{
		   pipeline_id = tEntityInfo.getInt("pipeline_id", row);
		   delivery_id = tEntityInfo.getInt("delivery_id", row);
		   version = tEntityInfo.getInt("version_number", row);

		   tEntityInfo.setInt("primary_entity_num", row, delivery_id);
		   tEntityInfo.setInt("secondary_entity_num", row, delivery_id);
		   tEntityInfo.setInt("entity_version", row, version);
		   tEntityInfo.setInt("entity_group_id", row, pipeline_id);
		   tEntityInfo.setInt("old_entity_group_id", row, pipeline_id);
		
		   //Get the user_id and the last_update fields from comm_schedule_delivery and enrich in tEntityInfo
		   Table tNomInfoDB;
		   tNomInfoDB = Table.tableNew("Nom Info");
		   tNomInfoDB.addCols("I(user_id), T(last_update)");

		   // Query onto the database
		   String sWhat, sFrom, sWhere;
		   sWhat = "user_id, last_update";
		   sFrom = "comm_schedule_delivery";
		   sWhere = "delivery_id = " + delivery_id;
		   iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tNomInfoDB, sWhat, sFrom, sWhere);
		
		   if (Table.isTableValid(tNomInfoDB) == 0) {
			   m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to enrich nomination details");
			   return 0;
		   } 
		
		   if (tNomInfoDB.getNumRows() > 0 )
		   {
			   if ( tEntityInfo.getColNum("personnel_id") < 1)
				   tEntityInfo.addCol("personnel_id", COL_TYPE_ENUM.COL_INT);

			   if ( tEntityInfo.getColNum("last_update") < 1)
				   tEntityInfo.addCol("last_update", COL_TYPE_ENUM.COL_DATE_TIME);			
		   }
		   else
		   {
			   // in this instance the nomination no longer exists in the db so
			   // effectively make the script skip it
			   tNomInfoDB.destroy();
			   return 1;
		   }
		
		   for (int dbRow = 1; dbRow <= tNomInfoDB.getNumRows(); dbRow++) 
		   {
			   //Add the user_id to personnel_id
			   tEntityInfo.setInt("personnel_id", row, tNomInfoDB.getInt("user_id", dbRow));
			   //Copy last_update
			   tEntityInfo.setDateTime("last_update", row, tNomInfoDB.getDateTime("last_update", dbRow));
		   }
			
		   tNomInfoDB.destroy();
		}
		

		if (iRetVal == 0)
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to enrich nomination details");
		else
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Finished enriching nomination details");
		
		return iRetVal;

	}

	public int APM_FindLaunchType(Table tMainArgt, Table tAPMArgumentTable) throws OException
	{
		String sProcessingMessage;
		int iBlockUpdates = 0;

		// batch
		if (m_APMUtils.APM_CheckColumn(tMainArgt, "selected_criteria", COL_TYPE_ENUM.COL_TABLE.toInt()) == 1
		        && Table.isTableValid(tMainArgt.getTable("selected_criteria", 1)) == 1) {
			if (m_APMUtils.APM_CheckColumn(tMainArgt, "method_id", COL_TYPE_ENUM.COL_INT.toInt()) == 1 && tMainArgt.getInt("method_id", 1) > 0)
				return (m_APMUtils.cModeBatch);
		}

		// must be an incremental update

		Table tArgtNomInfo = tMainArgt.getTable("Nom Info", 1);
		if (Table.isTableValid(tArgtNomInfo) == 0 || (tArgtNomInfo.getNumRows() < 1)) {
			// When there is no nom info, return "do nothing"
			return m_APMUtils.cModeDoNothing;
		}
		Table tArgtNomTable = tArgtNomInfo.getTable("nom_table", 1);
		if (Table.isTableValid(tArgtNomTable) == 0 || (tArgtNomTable.getNumRows() < 1)) {
			// When there is no nom table, return "do nothing"
			return m_APMUtils.cModeDoNothing;
		}


		Table tOpSvcDefn = tMainArgt.getTable("Operation Service Definition", 1);
		Table tNomInfo = tAPMArgumentTable.getTable("Global Filtered Entity Info", 1);

		// if we've got more than one nom enter block mode
		// use the argt nom info to decide whether we have a block but then add col to filtered tran info
		int iNumNomRows = tArgtNomTable.getNumRows();
		if (iNumNomRows > 1) {
			if (tNomInfo.getColNum("update_mode") < 1)
				tNomInfo.addCol("update_mode", COL_TYPE_ENUM.COL_INT);
			iBlockUpdates = 1;
		}

		iNumNomRows = tNomInfo.getNumRows(); // reset to actual number of rows

		// for each nom (or only one if we're not doing a block update)
		int iNomMode = 0;

		int delivery_id = 0;
		int version_number = 0;
		int delivery_status = 0;

		for (int iNomRow = 1; iNomRow <= iNumNomRows; iNomRow++) {
			iNomMode = 0;

			delivery_id = tNomInfo.getInt("delivery_id", iNomRow);
			version_number = tNomInfo.getInt("version_number", iNomRow);
			delivery_status = tNomInfo.getInt("delivery_status", iNomRow);

			if (delivery_id <= 0)
				iNomMode = m_APMUtils.cModeDoNothing;
			else if (delivery_status == 8)
				iNomMode = m_APMUtils.cModeBackout;
			else
				iNomMode = m_APMUtils.cModeBackoutAndApply;

			if (iNomMode != 0) {
				if (iBlockUpdates != 0)
					tNomInfo.setInt("update_mode", iNomRow, iNomMode);
				continue;
			}
			// log an error and return as soon as we get an unknown run type as
			// we can't guarantee the whole set of noms
			tNomInfo.setInt("update_mode", iNomRow, m_APMUtils.cModeUnknown);
			sProcessingMessage = "ERROR: Unknown APM Script Mode !!!";
			sProcessingMessage = sProcessingMessage + " Delivery ID: " + tNomInfo.getInt("delivery_id", iNomRow);
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, sProcessingMessage);
			return m_APMUtils.cModeUnknown;
		}

		// if we've got block updates return that as the run mode, if not just
		// return the standard run mode for the single nomination
		if (iBlockUpdates == 1)
			return m_APMUtils.cModeBlockUpdate;
		else
			return iNomMode;
	}

	public int APM_GetSelectedPipelines(int iMode, Table tAPMArgumentTable) throws OException {
		int iRetVal = 1;

		Table tPipelines = Table.tableNew();
		tPipelines.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);

		Table tMainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
		if (iMode == m_APMUtils.cModeBatch) {
			Table tSelectedCriteria = tMainArgt.getTable("selected_criteria", 1);

			String criteriaTableColName = "filter_table";
			if (tSelectedCriteria.getColNum("criteria_table") > 0)
				criteriaTableColName = "criteria_table";

			String criteriaTypeColName = "filter_type";
			if (tSelectedCriteria.getColNum("criteria_type") > 0)
				criteriaTypeColName = "criteria_type";

			int iRow = 0;				
			if (tSelectedCriteria.getColNum(criteriaTypeColName) > 0)
				iRow = tSelectedCriteria.unsortedFindInt(criteriaTypeColName, SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE.toInt());
			else
				iRow = tSelectedCriteria.unsortedFindString("criteria_name", "Pipeline", SEARCH_CASE_ENUM.CASE_SENSITIVE);

			Table tSelected;
			if (iRow > 0) {
				if (tSelectedCriteria.getColNum(criteriaTableColName) > 0) {
					tSelected = tSelectedCriteria.getTable(criteriaTableColName, iRow);
					tPipelines.select(tSelected, "id(entity_group_id)", "id GT 0");
				} else {
					tSelected = tSelectedCriteria.getTable("selected", iRow);
					tPipelines.select(tSelected, "id_number(entity_group_id)", "id_number GT 0");
				}
			}
		} else {
			// use the global table as we are outside the pfolio loop
			Table tEntityInfo = tAPMArgumentTable.getTable("Global Filtered Entity Info", 1);
			if (tEntityInfo.getColNum("pipeline_id") > 0) {
				tPipelines.select(tEntityInfo, "DISTINCT, pipeline_id (entity_group_id)", "pipeline_id GT 0");
			}
		}

		if (iRetVal == 1 && (tPipelines.getNumRows() < 1)) {
			// If we can't find find any pipelines for a batch, fail the batch
			if (iMode == m_APMUtils.cModeBatch) {
				iRetVal = 0;
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "No pipelines specified in APM service Selected Criteria. Exiting ...");
			} else {
				// For an update, just log an error message but don't set
				// the error as otherwise the nom will retry forever
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "No pipelines found for this update");
			}
		} else {
			tPipelines.makeTableUnique();
			tAPMArgumentTable.setTable("Selected Entity Groups", 1, tPipelines);
		}

   	 	return iRetVal;
	}

	public int APM_AdjustLaunchType(int mode, Table tAPMArgumentTable, int pipeline_id) throws OException {
		String iNomStr = "\n";
		int iOrigMode, delivery_id;
		Table tNomInfo = Util.NULL_TABLE;
		QueryRequest qreq = null;
		Table queryNoms = Util.NULL_TABLE;
		int iQueryId = 0;
		int nom_pipeline_id;
		int version;
		int row = 0;

		// save the original mode so if we're in block update mode individual
		// update modes don't overwrite it
		iOrigMode = mode;

		// check that the insert is in a pipeline that we actually care about
		if (mode != m_APMUtils.cModeBatch) {

			Table tMainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
			tNomInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1); 
			
			// if theres a saved query we need to check against it later on to see if updates still in query
			boolean savedQueryOnService = false;			
			queryNoms = Table.tableNew();
			if ( tMainArgt.getColNum("query_name") > 0 )
			{
				String query_name = tMainArgt.getString("query_name", 1);
				if(Str.equal(query_name, "None") != 1)
				{
				   savedQueryOnService = true;
				   qreq = APM_ExecuteNominationQuery.instance().createQueryIdFromMainArgt(mode, tAPMArgumentTable, tMainArgt, pipeline_id);
				   iQueryId = qreq.getQueryId();
				   m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, queryNoms, "query_result", "query_result", "unique_id = " + iQueryId + " order by query_result");
				}				
			}
				
			// cycle around the Nominfo & for the current pfolio adjust the mode
			// you only get more than 1 entry in the entityinfo at all if its a block
			for (row = 1; row <= tNomInfo.getNumRows(); row++) {
				delivery_id = tNomInfo.getInt("delivery_id", row);
				version = tNomInfo.getInt("version_number", row);
				nom_pipeline_id = tNomInfo.getInt("pipeline_id", row);

				/* make sure we match up the correct row */
				if (nom_pipeline_id != pipeline_id)
						continue;

				// set here as this is the first time we actually identify the current delivery_id
				if (iOrigMode == m_APMUtils.cModeBlockUpdate) {
					// build up a delivery id String for the block of updates
					if ((row > 1) && (Str.len(iNomStr) > 0))
						iNomStr = iNomStr + "\n";
					iNomStr = iNomStr + " " + Str.intToStr(delivery_id);

					// get the mode for this update
					mode = tNomInfo.getInt("update_mode", row);
				} else {
					// set the nom info context
					ConsoleLogging.instance().setSecondaryEntityNumContext(tAPMArgumentTable, delivery_id);
					
					delivery_id = tNomInfo.getInt("delivery_id", row);
					ConsoleLogging.instance().setPrimaryEntityNumContext(tAPMArgumentTable, delivery_id);
					
					version = tNomInfo.getInt("version_number", row);
					ConsoleLogging.instance().setEntityVersionContext(tAPMArgumentTable, version);
					
					String pipelineName = Table.formatRefInt(nom_pipeline_id, SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);
					ConsoleLogging.instance().setPreviousEntityGroupContext(tAPMArgumentTable, pipelineName, nom_pipeline_id);
				}
				
				// if theres a saved query then check whether the update falls into it
				if ( mode == m_APMUtils.cModeBackoutAndApply && savedQueryOnService )
				{
					if ( queryNoms.findInt(1, delivery_id, SEARCH_ENUM.FIRST_IN_GROUP) < 1 )
					   mode = m_APMUtils.cModeBackout;
				}
				
				// if we're in block mode save the mode for each update
				if (iOrigMode == m_APMUtils.cModeBlockUpdate) {
					tNomInfo.setInt("update_mode", row, mode);
				} else {
					/*
					 * will only be 1 row that matches this portfolio if not in
					 * block mode
					 */
					break;
				}
			}

		}

		// if we're in block mode restore the mode back to block mode so we
		// don't enter into the mode of the last nom in the table
		if (iOrigMode == m_APMUtils.cModeBlockUpdate) {
			ConsoleLogging.instance().setSecondaryEntityNumContext(tAPMArgumentTable, "Block");
			APM_PrintAllNominationInfo(tAPMArgumentTable, tAPMArgumentTable.getTable("Main Argt", 1), 
										tAPMArgumentTable.getTable("Filtered Entity Info", 1),"");
			return m_APMUtils.cModeBlockUpdate;
		} else if (iOrigMode != m_APMUtils.cModeBatch)
		{
			delivery_id = tNomInfo.getInt("delivery_id", row);
			ConsoleLogging.instance().setSecondaryEntityNumContext(tAPMArgumentTable, delivery_id);
			version = tNomInfo.getInt("version_number", row);
			ConsoleLogging.instance().setEntityVersionContext(tAPMArgumentTable, version);

				
			APM_PrintAllNominationInfo(tAPMArgumentTable,tAPMArgumentTable.getTable("Main Argt", 1), 
										tAPMArgumentTable.getTable("Filtered Entity Info", 1),"");			
			
		}

		queryNoms.destroy();	
		if ( qreq != null ) // this will only be set if we have the new V11 where we execute the query
		{
			if ( iQueryId > 0 )					
				Query.clear(iQueryId);
			qreq.destroy(); 
		}			
		
		return mode;
	}

	public int APM_CreateQueryIDForPipeline(int mode, Table tAPMArgumentTable, int pipeline) throws OException {
	   if (mode == m_APMUtils.cModeBatch)
	      return 0;

	   int pfolioQueryID;

	   m_APMUtils.APM_DestroyJobQueryID(mode, tAPMArgumentTable);

	   Table pipelineFilteredNoms = tAPMArgumentTable.getTable("Filtered Entity Info", 1);
	   Table nominationList = Table.tableNew();
	   nominationList.select(pipelineFilteredNoms, "delivery_id", "delivery_id GT 0");

	   // set a query ID that matches this
	   int pipelineQueryID = 0;
	   if ( nominationList.getNumRows() > 0 )
	   {
	      pipelineQueryID = m_APMUtils.APM_TABLE_QueryInsertN(tAPMArgumentTable, nominationList, "delivery_id");
	      tAPMArgumentTable.setInt("job_query_id", 1, pipelineQueryID);	   
	   }
	   else
	   {
	      if ( pipelineFilteredNoms.getNumRows() < 1 )
	         m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Skipping update. No currently valid nominations in argt for pipeline " + Table.formatRefInt(pipeline, SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE) + " ...");
	   }

	   nominationList.destroy();
	   return pipelineQueryID;
	}

	public int RemoveNominationBackoutsFromQueryID(int mode, Table tAPMArgumentTable, int currPipeline, int pipelineQueryID)  throws OException {
   	 	return pipelineQueryID;
	}

	public void APM_PrintNominationInfoRow(Table tAPMArgumentTable, Table argt, Table tNomInfo, String header, int row) throws OException {
		int deliveryIdCol = tNomInfo.getColNum("delivery_id");
		int pipelineCol = tNomInfo.getColNum("pipeline_id");
		int nomVersionCol = tNomInfo.getColNum("version_number");
		int runIdCol = tNomInfo.getColNum("op_services_run_id");
		int deliveryStatusCol = tNomInfo.getColNum("delivery_status");
		int opsDefnCol = argt.getColNum("Operation Service Definition");

		int delivery_id = tNomInfo.getInt(deliveryIdCol, row);
		int pipeline_id = tNomInfo.getInt(pipelineCol, row);
		int nom_version = tNomInfo.getInt(nomVersionCol, row);
		int runId = tNomInfo.getInt(runIdCol, row);
		int delivery_status = tNomInfo.getInt(deliveryStatusCol, row);

		int expDefnId = 0;
		if (opsDefnCol > 0)
			expDefnId = argt.getTable(opsDefnCol, 1).getInt("exp_defn_id", 1);

		String pipelineName = Table.formatRefInt(pipeline_id, SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);

		String deliveryStatusName = Table.formatRefInt(delivery_status, SHM_USR_TABLES_ENUM.DELIVERY_STATUS_TABLE);

		// build message
		String nomMessage = header + "Delivery ID: " + delivery_id + ",  Version: " + nom_version + ",  Pipeline: " + pipelineName + ",  RunId: " + runId + ", ExpDefnId: " + expDefnId + ", Delivery Status: " + deliveryStatusName;

		m_APMUtils.APM_PrintMessage(tAPMArgumentTable, nomMessage);

   	 	return;
	}

	public void APM_PrintAllNominationInfo(Table tAPMArgumentTable, Table argt, Table tEntityInfo, String header) throws OException {

		if (Table.isTableValid(tAPMArgumentTable) == 0) {
			OConsole.oprint("Invalid argument table in PrintAllEntityInfo()\n");
			return;
		}

		if (Table.isTableValid(tEntityInfo) == 0) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Invalid entity info table in PrintAllEntityInfo()\n");
			return;
		}

		int numRows = tEntityInfo.getNumRows();

		if (numRows < 1) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Empty entity info table in PrintAllEntityInfo()");
			return;
		}

		for (int row = 1; row <= numRows; row++) {
			APM_PrintNominationInfoRow(tAPMArgumentTable, argt, tEntityInfo, header, row);
		}
	}

	public Table APM_RetrieveNominationInfoCopyFromArgt(Table tAPMArgumentTable, Table argt) throws OException {
		Table nominationInfo = Util.NULL_TABLE;
		Table nominationTable = Util.NULL_TABLE;
		Table opServicesLog;
		nominationInfo = argt.getTable("Nom Info", 1);
		
		if ( nominationInfo == Util.NULL_TABLE) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Could not find the Nom Info table in the argt");
		}
			
		nominationTable = nominationInfo.getTable("nom_table", 1).copyTable();

		// If a single update we need to go into the op_services_log table to get the op services run id.
		// For block update we should have the id already (also tables don't match so can't do join anyway!)
		if (nominationTable.getNumRows() == 1) {
			opServicesLog = argt.getTable("op_services_log", 1);
			if (Table.isTableValid(opServicesLog) == 1) {
				if (opServicesLog.getNumRows() > 1) {
					m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "More than one row in op_services_log for single nomination update. Taking first entry!");
				}

				String runIDColName = "run_id";
				if (opServicesLog.getColNum("op_services_run_id") > 0)
					runIDColName = "op_services_run_id";
				nominationTable.setInt("op_services_run_id", 1, opServicesLog.getInt(runIDColName, 1));
			}
		}
		return nominationTable;
	}

	public int APM_FilterNominationInfoTable(Table tAPMArgumentTable, Table argt, Table tEntityInfo) throws OException {
		// get rid of multiple lines for the same delivery ID - we only want to process the latest
		// note we are operating on a copy of the argt nom info - not the original !
		int row, nominationRow, nominationNo, opsRunID;

		Table uniqueNominationNums = Table.tableNew();
		uniqueNominationNums.select(tEntityInfo, "DISTINCT, delivery_id", "delivery_id GT 0");
		for (row = 1; row <= uniqueNominationNums.getNumRows(); row++) {
			nominationNo = uniqueNominationNums.getInt("delivery_id", row);
			opsRunID = 0;

			// identify the highest ops run ID (i.e. latest entry) for this nomination
			for (nominationRow = 1; nominationRow <= tEntityInfo.getNumRows(); nominationRow++) {
				if (nominationNo == tEntityInfo.getInt("delivery_id", nominationRow) && tEntityInfo.getInt("op_services_run_id", nominationRow) > opsRunID) {
					opsRunID = tEntityInfo.getInt("op_services_run_id", nominationRow);
				}
			}

			// now cycle through the nomination info table and remove the older duplicates
			for (nominationRow = tEntityInfo.getNumRows(); nominationRow >= 1; nominationRow--) {
				if (nominationNo == tEntityInfo.getInt("delivery_id", nominationRow)) {
					if (tEntityInfo.getInt("op_services_run_id", nominationRow) < opsRunID) {
						APM_PrintNominationInfoRow(tAPMArgumentTable, argt, tEntityInfo, "Removing nomination with older ops service run id: ", nominationRow);
						tEntityInfo.delRow(nominationRow);
					}
				}
			}
		}

		// check that there is only 1 row per nomination
		if (uniqueNominationNums.getNumRows() < tEntityInfo.getNumRows()) {
			// grrr...why aren't we unique - have to do it manually now and just blow away rows until we are unique
			uniqueNominationNums.addCol("found_flag", COL_TYPE_ENUM.COL_INT);
			uniqueNominationNums.sortCol(1);
			for (nominationRow = tEntityInfo.getNumRows(); nominationRow >= 1; nominationRow--) {
				row = uniqueNominationNums.findInt(1, tEntityInfo.getInt("delivery_id", nominationRow), com.olf.openjvs.enums.SEARCH_ENUM.FIRST_IN_GROUP);
				if (uniqueNominationNums.getInt(2, row) == 0)
					uniqueNominationNums.setInt(2, row, 1); // not found already, set the found flag
				else {
					APM_PrintNominationInfoRow(tAPMArgumentTable, argt, tEntityInfo, "Removing nomination due to duplicate nomination num: ", nominationRow);
					tEntityInfo.delRow(nominationRow); // found already, delete
				}
			}
		}

		uniqueNominationNums.destroy();

		return 1;
	}

	public boolean SetNomArgtReturnStatus(Table argt, int status) throws OException {

		Table tNomInfo = argt.getTable("Nom Info", 1);
		if (Table.isTableValid(tNomInfo) == 1) {
		   tNomInfo.setInt("log_status", 1, status);
		   return true;
		}
		return false;
	}

	public void SetBlockUpdateStatuses(Table tMainArgt, Table blockFails) throws OException {

		Table tNomInfo = tMainArgt.getTable("Nom Info",1);
		tNomInfo.select(blockFails, "log_status", "delivery_id EQ $delivery_id AND op_services_run_id EQ $op_services_run_id");
	}

	public Table GetDatasetKeysForService(Table tAPMArgumentTable, int serviceID, String serviceName) throws OException {

		Table pfield_tbl = Services.getServiceMethodProperties(serviceName, "ApmNomService");

		Table cached_table = null;
		if (pfield_tbl.unsortedFindString("pfield_name", "pipeline_id", SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0)
			cached_table = Services.getMselPicklistTable(pfield_tbl, "pipeline_id");

		if (cached_table == null)
			return null;

		Table pipeline_list = cached_table.copyTable();
		// make sure the pipeline col name is obvious
		if (pipeline_list.getColNum("id") > 0)
			pipeline_list.setColName("id", "entity_group_id");

		pipeline_list.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
		pipeline_list.addCol("service_id", COL_TYPE_ENUM.COL_INT);
		pipeline_list.addCol("temp_int", COL_TYPE_ENUM.COL_INT);

		pipeline_list.setColValInt("service_id", serviceID);

		int datasetType = 0;
		if (pfield_tbl.unsortedFindString("pfield_name", "dataset_type_id", SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0) {
			datasetType = Services.getPicklistId(pfield_tbl, "dataset_type_id");
			pipeline_list.setColValInt("dataset_type_id", datasetType);
		}

		int pkgRow = pfield_tbl.unsortedFindString("pfield_name", "package_name", SEARCH_CASE_ENUM.CASE_SENSITIVE);
		if (pkgRow > 0) {
			Table packageTable = Services.getMselPicklistTable(pfield_tbl, "package_name");
			pipeline_list.select(packageTable, "value(package)", "id GE $temp_int");
		}

		pipeline_list.delCol("temp_int");
		pfield_tbl.destroy();
		return pipeline_list;

	}

	public int GetUnderlyingSimResultRow(int iMode, Table tAPMArgumentTable, String sDataTableName, Table tPackageDataTableCols) throws OException {

		   /* Find the underlying sim result for the given table - the one where delivery_id comes from */
		   int iUnderlyingResultRow = tPackageDataTableCols.unsortedFindString( "column_name", "delivery_id", SEARCH_CASE_ENUM.CASE_SENSITIVE);
		   if (iUnderlyingResultRow < 1)
		   {
			   String sErrMessage = "USER_APM_FillDataTableFromResultFills failed to identify underlying simulation result for table '" + sDataTableName + "'" ;
			   tAPMArgumentTable.setString( "Error String", 1, sErrMessage);
			   m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
			   return 0;
		   }

		return iUnderlyingResultRow;

	}

	public int CheckSimResultsValid(int iMode, Table tAPMArgumentTable, Table tResults) throws OException {

		int iRetVal = 1;
		if (Table.isTableValid(tResults) == 0 )
		{
					String sErrMessage = "No results valid from Sim.runRevalByParamFixed";
					m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, sErrMessage);
					iRetVal = 0;
		}
		return iRetVal;
	}

	public Table EnrichSimulationResults(Table tResults, int iType, int iResultClass, String sWhat) throws OException {
		Table tSelectedResult = Util.NULL_TABLE;

		if (tResults.getNumRows() > 0)
		{
			if (iResultClass == RESULT_CLASS.RESULT_GEN.toInt())
			{
				int iStartGroup = tResults.findInt( RESULT_GEN_COL_ENUM.GEN_RESULT_TYPE.toInt(), iType, SEARCH_ENUM.FIRST_IN_GROUP);
				if (iStartGroup > 0)
				{
					int iEndGroup   = tResults.findInt( RESULT_GEN_COL_ENUM.GEN_RESULT_TYPE.toInt(), iType, SEARCH_ENUM.LAST_IN_GROUP);
					int iNumRows    = (iEndGroup - iStartGroup)+1; 

					tSelectedResult = tResults.getTable( RESULT_GEN_COL_ENUM.GEN_RESULT_TABLE.toInt(), iStartGroup).cloneTable();

					for (int x = 0; x < iNumRows; x++)
					{
					    tResults.getTable( RESULT_GEN_COL_ENUM.GEN_RESULT_TABLE.toInt(), x+iStartGroup).copyRowAddAll(tSelectedResult);
					}
				}
			}
			else
			{
				tSelectedResult = Table.tableNew( "Tran Result" );
				tSelectedResult.select( tResults, sWhat, "deal_num GT 0");
			}
		}  

		return tSelectedResult;
	}

	public int SetupRevalParamForNominations(int iMode, Table tAPMArgumentTable, String sJobName, int iQueryId, int entityGroupId, Table tRevalParam) throws OException {

   	 	// we don't want to refresh the market data
   	 	tRevalParam.setInt("RefreshMktd", 1, 0);

   	 	if (tRevalParam.getColNum("DoGlobalPortfolioCalcs") > 0)
			tRevalParam.setInt("DoGlobalPortfolioCalcs", 1, 0);

		/* get sim info */
		Table tSimDef = tRevalParam.getTable("SimulationDef", 1);

   	 	// we want to add the apm_service_type to the argt (and set it to "Nomination")
		if (tSimDef.getColNum("apm_service_type") < 1)
			tSimDef.addCol("apm_service_type", COL_TYPE_ENUM.COL_STRING);

		tSimDef.setString("apm_service_type", 1, "Nomination");

   	 	// we want to set the query as the nomination query to use in the sim and set the query for the sim to a single update

		//find a single valid tran num - cache as the query can take some time
		//don't use the APM specific caching as its not needed for this
		Table querySingleDeal = Table.getCachedTable("APM Nomination Query");
		if (Table.isTableValid(querySingleDeal) == 0) {

			//find a simple deal for the sim to load up - not a complicated one
			//do this to speed up the reval
			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Finding & caching Single Deal Number");
			querySingleDeal = Table.tableNew();

			// tran_status - 1 = Pending
			// tran_status - 2 = New
			// tran_status - 3 = Validated
			// tran_status - 7 = Proposed
			// tran_type   - 0 = Trading

			m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, querySingleDeal, "min(tran_num)", "ab_tran", "tran_status in (1, 2, 3, 7) and tran_type = 0 and toolset not in (33,36,37,10,38)");

			//------------------------------------------------------------
			// CHECK #1
			// Check against a single row with tran_status = 0,				
			int iReceivedTranNum = 0;
			if (querySingleDeal.getNumRows() > 0) {
				iReceivedTranNum = querySingleDeal.getInt(1, 1);
			} else {
				iReceivedTranNum = 0;
			}
			// if we don't get any rows then we have to load up a complicated deal.
			if (querySingleDeal.getNumRows() < 1 || iReceivedTranNum == 0) {
				m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, querySingleDeal, "min(tran_num)", "ab_tran", "tran_status in (1, 2, 3, 7) and tran_type = 0");
			}

			//------------------------------------------------------------
			// CHECK #2
			// Check against a single row with tran_status = 0, 
			// but for all toolsets available				
			if (querySingleDeal.getNumRows() > 0) {
				iReceivedTranNum = querySingleDeal.getInt(1, 1);
			} else {
				iReceivedTranNum = 0;
			}

			if (iReceivedTranNum == 0) {
				// Was unable to retrieve any useful information
				// Failed to retrieve data for the 'APM Nomination Query' cache. At least one 'Trading' type deal with status 'Pending' or 'Validated' required. Please contact APM Support. 

				String sProcessingMessage = "Failed to retrieve data for the 'APM Nomination Query' cache. At least one 'Trading' type deal with status 'Pending' or 'Validated' required. Please contact APM Support.";
				m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, sJobName, "", entityGroupId, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);
			}

			Table.cacheTable("APM Nomination Query", querySingleDeal);
		}
		int newQueryId = m_APMUtils.APM_TABLE_QueryInsert(tAPMArgumentTable, querySingleDeal, 1);

		//now set this to fool the sim
		tRevalParam.setInt("QueryId", 1, newQueryId);

		//add the real query ID into its own column in the sim def
		//this will be accessed by the results
		if (tSimDef.getColNum("APM Nomination Query") < 1)
			tSimDef.addCol("APM Nomination Query", COL_TYPE_ENUM.COL_INT);

		tSimDef.setInt("APM Nomination Query", 1, iQueryId);

		// we also need to set the single deal query column so that we get the deal info enriched
		// this will be accessed by the results
		// to do this we take the nomination query ID and run SQL to return the deal numbers for the noms
		// then we insert a query for that too !
		// TODO - check that the deal query ID is cleared (we don't want to leak)
		
		String dealSQLWhat = "service_provider_deal_num";
		String dealSQLFrom = "comm_schedule_delivery cm, query_result qr";
		String dealSQLWhere = "qr.query_result = cm.delivery_id AND qr.unique_id = " + iQueryId;
		Table tNomDeals = Table.tableNew("Nom Deals");
		int iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tNomDeals, dealSQLWhat, dealSQLFrom, dealSQLWhere);
		if ( iRetVal != 0 && Table.isTableValid(tNomDeals) != 0)
		{
			int dealQueryId = m_APMUtils.APM_TABLE_QueryInsert(tAPMArgumentTable, tNomDeals, 1);			
			
			if (tSimDef.getColNum("APM Single Deal Query") < 1)
				tSimDef.addCol("APM Single Deal Query", COL_TYPE_ENUM.COL_INT);

			tSimDef.setInt("APM Single Deal Query", 1, dealQueryId);
		}
		else
		{
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to retrieve deal numbers for the noms");
			return 0;
		}

   	 	return 1;
	}

	public int SetUpArgumentTableForNominations(Table tAPMArgumentTable, Table argt) throws OException {
   	 	// nothing specific to set that is nom based
   	 	return 1;
	}

 	
}
