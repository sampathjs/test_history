/* Released with version 29-Aug-2019_V17_0_124 of APM */

/*
 Description : This forms part of the Trader Front End, Active Position Manager
 package

 This script perfoms the following :
 - Checks the data that is about to be BCPed into the database to
 make sure the underlying database table exists and that it matches
 the schema
 - The APM Reporting tables are not populated for backouts (for speed)
 This script adds the secondary entity nums to the first table for backouts
 - Calls APM functions to BCP the data in
 - Clears the APM reporting data in preparation for the next entity group

 - Block updates now cope with empty or partially empty data tables

 -------------------------------------------------------------------------------
 Revision No.  Date        Who  Description
 -------------------------------------------------------------------------------
 */

package standard.apm;

import standard.include.APM_Utils;
import standard.include.APM_Utils.EntityType;
import standard.include.ConsoleCaptureWrapper;
import standard.include.ConsoleLogging;
import standard.include.LogConfigurator;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OP_SERVICES_LOG_STATUS;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;

// All code in this file used to exist in APM_UpdateTables.

public class APM_UpdateTables_Impl {
	private APM_Utils m_APMUtils;

	public APM_UpdateTables_Impl() {
		m_APMUtils = new APM_Utils();

	}

	private static final double DOUBLE_ERROR = 1e32; // java.lang.Math.pow(10, 32);

	private static long TotalInsertTime;
	private static long TotalADSInsertTime;
	private static long TotalInsertions;
	private static long TotalDataRows;
	private static long TotalDataColumns;

	public int execute(Table argt) throws OException {

		int entityGroupId;
		int iRetVal = 1;
		int iLoopRetVal = 1;
		int iMode = 0;
		int iRow;
		Table tPackageDataTables;
		Table tMainArgt;
		Table tPackageDetails;
		String sPackageName;
		String sJobName;
		int iDatasetTypeId;
		int serviceID;

		try {
			/*
			 * argt to this script should be the APMArgumentTable ... quick
			 * validation here
			 */
			if (argt.getNumRows() == 0) {
				OConsole.oprint("Argument table passed into APM_UpdateTables has no rows");
				return 0;
			}

			if (argt.getNumRows() > 1) {
				OConsole.oprint("Argument table passed into APM_UpdateTables has too many rows");
				return 0;
			}

			if (argt.getColNum("Package Details") < 1) {
				OConsole.oprint("No Package Details included in the argument table passed into APM_UpdateTables");
				return 0;
			}

			String logFilePath = "";			
			if ( Str.isEmpty(Util.getEnv("AB_ERROR_LOGS_PATH"))==1 )
				logFilePath = Util.getEnv("AB_OUTDIR") + "/error_logs/";
			else
				logFilePath = Util.getEnv("AB_ERROR_LOGS_PATH") + "/";

			// Set the log path to be used by our logger.
			LogConfigurator.getInstance().setPath( logFilePath );
			LogConfigurator.getInstance().setServiceName( argt.getString("service_name", 1) );
			LogConfigurator.getInstance().push( argt.getString( "Log File", 1 ) );
			argt.setString("Log File", 1, LogConfigurator.getInstance().front());

			if ( !ConsoleCaptureWrapper.getInstance(argt).isOpen() ) {
				// Open the console capture and register this object as its owner.
				ConsoleCaptureWrapper.getInstance(argt).open( this );						
			}

			// m_APMUtils.APM_SetMsgContext(argt, "SCRIPT", "APM_UpdateTables", -1);
			ConsoleLogging.instance().setScriptContext(argt, "APM_UpdateTables");
			tPackageDetails = argt.getTable("Package Details", 1);

			entityGroupId = argt.getInt("Current Entity Group Id", 1);
			iMode = argt.getInt("Script Run Mode", 1);
			tMainArgt = argt.getTable("Main Argt", 1);
			sJobName = argt.getString("Job Name", 1);
			iDatasetTypeId = argt.getInt("dataset_type_id", 1);	
			serviceID = argt.getInt("service_id", 1);

			m_APMUtils.APM_PrintMessage(argt, "Starting to update APM reporting tables for tranche #" + argt.getInt("Tranche", 1));

			for (iRow = 1; (iRetVal == 1 && (iRow <= tPackageDetails.getNumRows())); iRow++) 
			{
				sPackageName = tPackageDetails.getString("package_name", iRow);
				ConsoleLogging.instance().setPackageContext(argt, sPackageName);

				tPackageDataTables = tPackageDetails.getTable("package_data_tables", iRow);

				// Keep track of errors but update as many entity group/package
				// combinations as possible
				if (iMode == m_APMUtils.cModeBlockUpdate)
					iLoopRetVal = UpdateTablesForEntityGroupAndPackageAndBlock(iMode, entityGroupId, sPackageName, iDatasetTypeId, serviceID, sJobName, tMainArgt, tPackageDataTables, argt);
				else
				{
					boolean datasetKeyInAnotherService = false;		
					if ( iMode == m_APMUtils.cModeBackout)
					{
						if ( isDatasetKeyInAnyOtherService(argt, serviceID, entityGroupId, iDatasetTypeId, sPackageName) )
							datasetKeyInAnotherService = true;						
					}
					iLoopRetVal = UpdateTablesForEntityGroupAndPackage(iMode, entityGroupId, sPackageName, iDatasetTypeId, serviceID, datasetKeyInAnotherService, sJobName, tMainArgt, tPackageDataTables, argt);
				}

				if (iLoopRetVal == 0)
					iRetVal = 0;

				if (iLoopRetVal != 0) 
					m_APMUtils.APM_PrintMessage(argt, "APM reporting table update complete for tranche #" + argt.getInt("Tranche", 1));
				else
					m_APMUtils.APM_PrintAndLogErrorMessageSafe(iMode, argt, "APM reporting table update failed for tranche #" + argt.getInt("Tranche", 1));

				ConsoleLogging.instance().unSetPackageContext(argt);
			}
		} finally {
			// Stop any console logging operations.
			if ( iRetVal == 0 ) {
				try {
					ConsoleCaptureWrapper.getInstance(argt).flush();
				} catch ( Throwable exception ) {
					m_APMUtils.APM_PrintErrorMessage(argt, exception.toString());
				}
			} else {
				ConsoleCaptureWrapper.getInstance(argt).close( this );			
			}

			// Reset the logging path in the argt...
			argt.setString("Log File", 1, LogConfigurator.getInstance().pop());
		}

		return iRetVal;
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	//
	// UpdateTablesForEntityGroupAndPackageAndBlock()
	//
	// Do the update for one package/entity group for a update with a block of entities
	// //////////////////////////////////////////////////////////////////////////////////////
	int UpdateTablesForEntityGroupAndPackageAndBlock(int iMode, int entityGroupId, String sPackageName, int iDatasetTypeId, int serviceID, String sJobName, Table tMainArgt, Table tPackageDataTables, Table argt)
	throws OException 
	{
		Table tEntityInfo;
		Table tPackageReportingDataTable;
		Table tPackageDataTableCopy;
		int iEntityRow, iRunMode = -1, iLogStatusCol;
		int iRetVal = 1;

		// the entity info will now ONLY contain unique entities for this update
		tEntityInfo = argt.getTable("Filtered Entity Info", 1); // use the filtered entity info

		// work out if we need to care about whether an entity is in another service
		boolean datasetKeyInAnotherService = false;
		if ( tEntityInfo.getColNum("update_mode") > 0 ) // i.e. if we are in a block
		{
			for (int i = 1; i <= tEntityInfo.getNumRows(); i++) 
			{
				if ( tEntityInfo.getInt("update_mode", i) == m_APMUtils.cModeBackout )
				{
					if ( isDatasetKeyInAnyOtherService(argt, serviceID, entityGroupId, iDatasetTypeId, sPackageName) )
						datasetKeyInAnotherService = true;
					break;
				}
			}
		}

		// get the reporting data
		tPackageReportingDataTable = tPackageDataTables.getTable("APM Reporting Table", 1);
		tPackageDataTableCopy = tPackageReportingDataTable.copyTable();

		iLogStatusCol = tEntityInfo.getColNum("log_status");

		// setup stats vars - only used in ADS
		TotalInsertTime = TotalADSInsertTime = TotalInsertions = TotalDataRows = TotalDataColumns = 0;
		TotalDataColumns = tPackageDataTables.getTable(1,1).getNumCols();

		boolean useADS = m_APMUtils.useADS(argt);
		if ( useADS || m_APMUtils.isActiveDataAnalyticsService(argt) ) //FOR ADS DO A SINGLE BLOCK INSERT
		{
			long startTime = System.currentTimeMillis(); //SDP
			int iLoopRetVal = 1;       // starts with status ok
			int iCurrentPrimaryEntityNum, iCurrentSecondaryEntityNum;
			try
			{
				iLoopRetVal = UpdateTablesForEntityGroupAndPackage(m_APMUtils.cModeBackoutAndApply, entityGroupId, sPackageName, iDatasetTypeId, serviceID, datasetKeyInAnotherService, sJobName, tMainArgt, tPackageDataTables, argt);

				// ok - it may have FAILED - we need to update the return statuses for all entities in block
				// cycle around the entity info as that includes the backouts
				for (int j = 1; j <= tEntityInfo.getNumRows(); j++)
				{
					iCurrentPrimaryEntityNum = tEntityInfo.getInt("primary_entity_num", j);
					iCurrentSecondaryEntityNum = tEntityInfo.getInt("secondary_entity_num", j);

					ConsoleLogging.instance().setSecondaryEntityNumContext(argt, iCurrentSecondaryEntityNum);
					ConsoleLogging.instance().setPrimaryEntityNumContext(argt, iCurrentPrimaryEntityNum);

					int entityVersion = tEntityInfo.getInt("entity_version", j);
					ConsoleLogging.instance().setEntityVersionContext(argt, entityVersion);

					try {
						iRetVal = updateEntityInfoStatus(m_APMUtils.cModeBackoutAndApply, argt, iLogStatusCol, iRetVal, iLoopRetVal, tEntityInfo, entityGroupId, iCurrentSecondaryEntityNum, iCurrentPrimaryEntityNum);
					} catch (Exception e) {
						iRetVal = 0;
						// We need to finish updating the log return statuses.
						// We can't allow an exception to break this loop.
					}
				}

				ConsoleLogging.instance().unSetSecondaryEntityNumContext(argt);

				ConsoleLogging.instance().unSetPrimaryEntityNumContext(argt);

				ConsoleLogging.instance().unSetEntityVersionContext(argt);
			}
			catch(Exception t)
			{
				iRetVal = 0;
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, argt, "Failed to update APM reporting tables for tranche #" + argt.getInt("Tranche", 1) + ".Exception details:"  + t  );
				String message = m_APMUtils.getStackTrace(t);
				m_APMUtils.APM_PrintErrorMessage(argt, message+ "\n");
			}            

			// the UpdateTablesForEntityGroupAndPackage clears the table anyway so don;t delete the data table

			long end = System.currentTimeMillis(); //SDP
			long time = end - startTime; //SDP
			TotalInsertTime = TotalInsertTime + time;
		}
		else
		{ // START ELSE - NON ADS - DO A SINGLE INSERT AT A TIME

			// for each entity select the rows into the reporting table and set the
			// relevant info in the arg table for that entity and run the update
			tEntityInfo.sortCol("secondary_entity_num");
			for (iEntityRow = 1; iEntityRow <= tEntityInfo.getNumRows(); iEntityRow++) 
			{   
				int iCurrentPrimaryEntityNum = -1, iCurrentSecondaryEntityNum = -1;

				int iLoopRetVal = 1;		   // starts with status ok

				try
				{
					// resort the table here - the following fns may have resorted the table accidentally through selects
					tEntityInfo.sortCol("secondary_entity_num");

					tPackageReportingDataTable.clearRows();

					iCurrentPrimaryEntityNum = tEntityInfo.getInt("primary_entity_num", iEntityRow);
					iCurrentSecondaryEntityNum = tEntityInfo.getInt("secondary_entity_num", iEntityRow);

					tPackageReportingDataTable.select(tPackageDataTableCopy, "*", m_APMUtils.GetCurrentEntityType(argt).getPrimaryEntity() + " EQ " + Str.intToStr(iCurrentPrimaryEntityNum));
					
					// set the other values in the main argt
					ConsoleLogging.instance().setSecondaryEntityNumContext(argt, iCurrentSecondaryEntityNum);
					ConsoleLogging.instance().setPrimaryEntityNumContext(argt, iCurrentPrimaryEntityNum);
					ConsoleLogging.instance().setEntityVersionContext(argt, tEntityInfo.getInt("entity_version", iEntityRow));

					int oldEntityGroupId = tEntityInfo.getInt("old_entity_group_id", iEntityRow);
					String oldEntityGroupName = m_APMUtils.GetCurrentEntityType(argt).getFormattedNameForGroupId(oldEntityGroupId);

					ConsoleLogging.instance().setPreviousEntityGroupContext(argt, oldEntityGroupName, oldEntityGroupId);
					// set the run mode for this entity
					iRunMode = tEntityInfo.getInt("update_mode", iEntityRow);
					argt.setInt("Script Run Mode", 1, iRunMode);

					// It is ok if the reporting data table has no sim results for the given entity ( example, Vega package would return
					// no data if volatilities are not set in DB). 

					// run update for this block of entities in this entity group/package
					iLoopRetVal = 1;
					if ( iRunMode != m_APMUtils.cModeDoNothing)
						iLoopRetVal = UpdateTablesForEntityGroupAndPackage(iRunMode, tEntityInfo.getInt("entity_group_id",iEntityRow), sPackageName, iDatasetTypeId, serviceID, datasetKeyInAnotherService, sJobName, tMainArgt, tPackageDataTables, argt);

					// SDP - CODE TO MIMIC A FAILURE for PFOLIO 20094              
					//      			if ( sJobName.equals("20094_1_2"))
					//      				iLoopRetVal = 0;
					//iLoopRetVal = 0;
					iRetVal = updateEntityInfoStatus(iRunMode, argt, iLogStatusCol, iRetVal, iLoopRetVal, tEntityInfo, entityGroupId, iCurrentSecondaryEntityNum, iCurrentPrimaryEntityNum);
				}
				catch(Exception t)
				{
					iLoopRetVal = 0;
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, argt, "Failed to update APM reporting tables for " + m_APMUtils.GetCurrentEntityType(argt) + "#" + iCurrentPrimaryEntityNum + ".Exception details:"  + t  );
					String message = m_APMUtils.getStackTrace(t);
					m_APMUtils.APM_PrintErrorMessage(argt, message+ "\n");
				}

				ConsoleLogging.instance().unSetSecondaryEntityNumContext(argt);
				ConsoleLogging.instance().unSetPrimaryEntityNumContext(argt);
				ConsoleLogging.instance().unSetEntityVersionContext(argt);
			}
		} 

		if ( useADS && m_APMUtils.isActivePositionManagerService(argt) )
		{
			// ONLY output stats in ADS as APM_PerformOperation will deal with Non ADS
			OConsole.oprint("Entity Group=" + entityGroupId);
			OConsole.oprint(" Package=" + sPackageName);
			OConsole.oprint(" TotalInsertions=" + TotalInsertions);
			OConsole.oprint(" TotalDataRows=" + TotalDataRows);
			OConsole.oprint(" TotalDataColumns=" + TotalDataColumns);
			OConsole.oprint(" TotalADSInsertTime=" + TotalADSInsertTime + " ms. ");
			OConsole.oprint("TotalInsertTime=" + TotalInsertTime + " ms. ");
			double AvgTotalInsertTime = TotalInsertTime / Math.max(TotalInsertions, 1); 
			double AvgDataRows = TotalDataRows / Math.max(TotalInsertions, 1); 
			double AvgADSInsertTime = TotalADSInsertTime / Math.max(TotalInsertions, 1); 
			double AvgAPMInsertTime = (TotalInsertTime - TotalADSInsertTime) / Math.max(TotalInsertions, 1); 

			OConsole.oprint("Avg Total Insert Time=" + AvgTotalInsertTime + " ms. ");
			OConsole.oprint("Avg Data Rows=" + AvgDataRows);
			OConsole.oprint(" Avg Total Insert Time in APM code=" + AvgAPMInsertTime + " ms. ");
			OConsole.oprint("Avg Total Insert Time in ADS code=" + AvgADSInsertTime + " ms. \n");
		}

		// set run mode back to entity block
		argt.setInt("Script Run Mode", 1, m_APMUtils.cModeBlockUpdate);

		// reset tran data back to block mode
		ConsoleLogging.instance().setSecondaryEntityNumContext(argt, "Block");
		ConsoleLogging.instance().unSetPrimaryEntityNumContext(argt);
		ConsoleLogging.instance().unSetEntityVersionContext(argt);
		ConsoleLogging.instance().unSetPreviousEntityGroupContext(argt);

		// clean up
		tPackageDataTableCopy.destroy();

		return iRetVal;
	}

	int updateEntityInfoStatus( int iRunMode, Table argt, int iLogStatusCol, 
			int iRetVal, int iLoopRetVal, Table tEntityInfo, 
			int entityGroupId, int iCurrentSecondaryEntityNum, int iCurrentPrimaryEntityNum)
	throws Exception 
	{	

		try
		{
			// set the overall status so we know if something has failed
			if (iLoopRetVal == 0)
				iRetVal = 0;

			// Set the log status to true so this doesn't run again, we only do
			// this in block mode as standard updates don't check this on
			// completion
			if (iLogStatusCol > 0) 
			{
				// find the row in the entity info - and set appropriate status
				int iEntityRow = tEntityInfo.unsortedFindInt("secondary_entity_num", iCurrentSecondaryEntityNum);
				if (iLoopRetVal > 0) 
				{
					// set the state to succeeded but if the entity has already
					// failed for another package don't set it to succeeded
					// as we want to fail the entity for all packages otherwise if
					// the last package is ok we won't rerun the entity.
					if (tEntityInfo.getInt("log_status", iEntityRow) != OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt())
						tEntityInfo.setInt("log_status", iEntityRow, OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_SUCCEEDED.toInt()); // Succeeded
					// don't really need to do this, just for completeness
					else
						tEntityInfo.setInt("log_status", iEntityRow, OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt()); // set
					// log_status to failed if its already failed for another package
				} 
				else 
				{
					tEntityInfo.setInt("log_status", iEntityRow, OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt()); // set
					// log_status to failed

					// Raise bad update as it won't be handled higher up
					String sErrMessage = "Failed to update APM Reporting tables for " + m_APMUtils.GetCurrentEntityType(argt).getEntityGroup() + " : " + m_APMUtils.GetCurrentEntityType(argt).getFormattedNameForGroupId(entityGroupId) + " (block update)";
					m_APMUtils.APM_PrintAndLogErrorMessage(iRunMode, argt, sErrMessage);
				}
			}
		}
		catch (Exception ex)
		{
			throw ex; 
		}

		return iRetVal;
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	//
	// UpdateTablesForEntityGroupAndPackage()
	//
	// Do the upate for one package/entity group
	// //////////////////////////////////////////////////////////////////////////////////////
	int UpdateTablesForEntityGroupAndPackage(int iMode, int entityGroupId, String sPackageName, int iDatasetTypeId, int serviceID, boolean datasetKeyInAnotherService, String sJobName, Table tMainArgt, Table tPackageDataTables, Table argt) throws OException {
		int iRetVal = 1;
		int iRow;
		int iPortfolioCol;
		Table tData;
		Table tOpserviceDef;
		String sProcessingMessage;

		if (iMode == m_APMUtils.cModeBatch)
			sProcessingMessage = "Starting to update tables for tranche #" + argt.getInt("Tranche", 1);
		else
			sProcessingMessage = "Starting incremental update to tables";

		m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeProcessingAlways, sJobName, sPackageName, entityGroupId, -1, -1, -1, -1, argt, Util.NULL_TABLE, sProcessingMessage);
		m_APMUtils.APM_PrintMessage(argt, sProcessingMessage);

		m_APMUtils.APM_PrintDebugMessage(argt, "Start Verification");

		/* basic verification of the APM reporting tables */
		if (VerifyAPMReportingTable(iMode, argt, entityGroupId, tPackageDataTables) == 0) {
			return 0;
		}

		/* verify the passed in data itself */
		if (iRetVal != 0)
			iRetVal = VerifyAPMData(iMode, argt, sPackageName, entityGroupId, tPackageDataTables, sJobName);

		/*
		 * if run from an APM Service the Ops Defn Id needs to be added to the
		 * main argt and filled in
		 */
		if (iMode != m_APMUtils.cModeBatch) {
			if (tMainArgt.getColNum("Ops Defn ID") < 1)
				tMainArgt.addCol("Ops Defn ID", COL_TYPE_ENUM.COL_INT);
			tOpserviceDef = tMainArgt.getTable("Operation Service Definition", 1);
			tMainArgt.setInt("Ops Defn ID", 1, tOpserviceDef.getInt("exp_defn_id", 1));
		}

		m_APMUtils.APM_PrintDebugMessage(argt, "End Verification");

		// ///////////////////////////////////////////////////
		//
		// Store reporting data in apm_data table via the APM_PerformOperation
		// call
		//
		// ///////////////////////////////////////////////////
		if (iRetVal != 0) {
			int iUpdateMode = -1;

			if (iMode == m_APMUtils.cModeBatch) {
				iUpdateMode = m_APMUtils.APM_BATCH_WRITE;
			} else if ((iMode == m_APMUtils.cModeApply) || (iMode == m_APMUtils.cModeBackoutAndApply)) {
				iUpdateMode = m_APMUtils.APM_APPLY;
			} else if (iMode == m_APMUtils.cModeBackout) {
				iUpdateMode = m_APMUtils.APM_BACKOUT;
			}

			long startTime = System.currentTimeMillis(); //SDP
			iRetVal = APM_DataStoreOps.instance().Update(iUpdateMode, iMode, entityGroupId, sPackageName, iDatasetTypeId, serviceID, datasetKeyInAnotherService, sJobName, tMainArgt, tPackageDataTables, argt);
			long endTime = System.currentTimeMillis(); //SDP
			long ADStime = endTime - startTime; //SDP
			// NOW SET THE OVERALL TIMINGS
			TotalInsertions = TotalInsertions + 1;
			TotalADSInsertTime = TotalADSInsertTime + ADStime;
			TotalDataRows = TotalDataRows + tPackageDataTables.getTable(1,1).getNumRows();			
		}

		/*
		 * Clear all the reporting tables now and delete the 'portfolio' col
		 * that was added
		 */
		for (iRow = 1; iRow <= tPackageDataTables.getNumRows(); iRow++) {
			tData = tPackageDataTables.getTable(1, iRow);
			tData.clearRows(); /*
			 * clear the table _before_ changing the
			 * columns, to avoid unneccessary realloactions
			 */

			iPortfolioCol = tData.getColNum("portfolio");
			if ((iPortfolioCol > 0) && (tData.getColType(iPortfolioCol) == COL_TYPE_ENUM.COL_INT.toInt()))
				tData.delCol(iPortfolioCol);
		}

		// iRetVal=0; // bad update test
		if (iRetVal != 0) {
			if (iMode == m_APMUtils.cModeBatch) {
				sProcessingMessage = "Finished updating tables for tranche #" + argt.getInt("Tranche", 1);			   
			}
			else {
				if (iRetVal == m_APMUtils.APM_RETVAL_UPDATE_IGNORED)
					sProcessingMessage = "Ignored incremental update to tables. No cache exists, batch needs to run";
				else
					sProcessingMessage = "Finished incremental update to tables";			      
			}

			m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeProcessingAlways, sJobName, sPackageName, entityGroupId, -1, -1, -1, -1, argt, Util.NULL_TABLE, sProcessingMessage);
		} else {
			if (iMode == m_APMUtils.cModeBatch)
				sProcessingMessage = "Failed to update tables for tranche #" + argt.getInt("Tranche", 1);
			else
				sProcessingMessage = "Failed incremental update to tables";

			m_APMUtils.APM_PrintAndLogErrorMessageSafe(iMode, argt, sProcessingMessage);
		}

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          VerifyAPMData
	Description:   Checks that the memory table has no invalid data
	           If it does then it outputs a warning & sets the data to a valid 0 value
	           Currently only invalid doubles are checked.
	Parameters:    tPackageDataTables - the memory table that will be sent in to the APM fns as data
	Return Values: None
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	int VerifyAPMData(int iMode, Table tAPMArgumentTable, String sPackageName, int entityGroupId, Table tPackageDataTables, String sJobName) throws OException {
		Table tTable;
		int iTable, iCol, iRow, iNumTables, iNumRows, iNumCols, isError;
		String sTableName;
		String sProcessingMessage;

		/* check all the APM data tables */
		iNumTables = tPackageDataTables.getNumRows();
		for (iTable = 1; iTable <= iNumTables; iTable++) {
			/* only 1 col in main table which is of Table col type */
			tTable = tPackageDataTables.getTable(1, iTable);
			sTableName = tTable.getTableName();
			iNumCols = tTable.getNumCols();
			for (iCol = 1; iCol <= iNumCols; iCol++) {
				/* check all columns of type DOUBLE for bad double values */
				if (tTable.getColType(iCol) == COL_TYPE_ENUM.COL_DOUBLE.toInt()) {
					isError = 0;
					iNumRows = tTable.getNumRows();
					for (iRow = 1; iRow <= iNumRows; iRow++) {
						/* if bad set to 0.0 & log on a per column basis */
						if (tTable.getDouble(iCol, iRow) == DOUBLE_ERROR) {
							tTable.setDouble(iCol, iRow, 0.0);
							isError = 1;
						}
					}

					/*
					 * this should get logged into the error table when its
					 * implemented
					 */
					if (isError == 1) {
						sProcessingMessage = "Bad Double Values set to 0 in APM Table: " + sTableName + " Column Name: " + tTable.getColName(iCol);
						m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeWarn, sJobName, sPackageName, entityGroupId, -1, -1, -1, -1, tAPMArgumentTable,
								Util.NULL_TABLE, sProcessingMessage);
						m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);
					}
				}
			}
		}

		return 1;
	}

	/*-------------------------------------------------------------------------------
	Name:          VerifyAPMReportingTable
	Description:   Checks that the memory table has no invalid data
	Parameters:    tPackageDataTables - the memory table that will be sent in to the APM fns as data
	Return Values: None
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	int VerifyAPMReportingTable(int iMode, Table tAPMArgumentTable, int entityGroupId, Table tPackageDataTables) throws OException {
		if (Table.isTableValid(tPackageDataTables) == 0) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Invalid APM reporting tables generated by script\n");
			return 0;
		}

		if (tPackageDataTables.getNumCols() != 1) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Too many columns encountered in APM reporting tables generated by script\n");
			return 0;
		}

		if (tPackageDataTables.getColType(1) != COL_TYPE_ENUM.COL_TABLE.toInt()) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Invalid column type encountered in APM reporting tables generated by script\n");
			return 0;
		}

		/* We expect at least two (i.e. to include the appended APM log) ... */
		if (tPackageDataTables.getNumRows() < 1) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "No APM reporting tables generated by script\n");
			return 0;
		}
		return 1;
	}
	
	// tests whether the key passed through for the nominated service is also in any other service
	// this allows us to distinguish between entities moving between services or completely out of the monitored set of keys
	public boolean isDatasetKeyInAnyOtherService(Table tAPMArgumentTable, int serviceID, int entityGroupId, int iDatasetTypeId, String packageName) throws OException {

		Table datasetKeys = getAllOnlineServiceDatasetKeys(tAPMArgumentTable);
		if (datasetKeys == null)
			return false;

		Table OtherEntries = Table.tableNew();
		OtherEntries.select(datasetKeys, "service_id, package", "entity_group_id EQ " + entityGroupId + " AND dataset_type_id EQ " + iDatasetTypeId + " AND service_id NE "
		        + serviceID);
		int row = OtherEntries.unsortedFindString("package", packageName, SEARCH_CASE_ENUM.CASE_SENSITIVE);
		OtherEntries.destroy();
		datasetKeys.destroy();
		if (row > 0)
			return true;
		else
			return false;
	}

	// gets a table of the dataset keys monitored by online services
	// note the scenario is not included as we don't care about them
	// as this is used by the check for backouts and that does not need
	// to know about scenario as the backout is done for all scenarios in the key
	public Table getAllOnlineServiceDatasetKeys(Table tAPMArgumentTable) throws OException {

		Table allKeys = null;

		// get list of all online services
		Table onlineServices = Table.tableNew();
		int iRetVal = 1;
		if ( m_APMUtils.GetCurrentEntityType(tAPMArgumentTable) == EntityType.DEAL )
			iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, onlineServices, "job_cfg_id, name", "wflow_running", "service_group_type = 33");
		else if ( m_APMUtils.GetCurrentEntityType(tAPMArgumentTable) == EntityType.NOMINATION )
			iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, onlineServices, "job_cfg_id, name", "wflow_running", "service_group_type = 46");
			
		if (iRetVal <= 0)
			return null; // error will be logged in above fn

		for (int row = 1; row <= onlineServices.getNumRows(); row++) {
			Table keys = APM_EntityJobOps.instance().GetDatasetKeysForService(tAPMArgumentTable, onlineServices.getInt(1, row), onlineServices.getString(2, row));
			if (keys != null) {
				if (allKeys == null)
					allKeys = keys.copyTable();
				else
					keys.copyRowAddAll(allKeys);
			}
		}

		return allKeys;
	}
	
}
