/* Released with version 05-Feb-2020_V17_0_126 of APM */

package standard.apm;

import standard.include.APM_Utils;
import standard.include.ConsoleLogging;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public class APM_BatchOps_SQLITE
{
	private APM_Utils m_APMUtils;
	
	public APM_BatchOps_SQLITE() {
		m_APMUtils = new APM_Utils();
	}
	
   public int initialiseDatasets(Table tAPMArgumentTable, int entityGroupId) throws OException
   {
		XString err_xstring;
		Table tParams;
		int iRetVal = 1;
		int iRow = 1;
		String sPackageName;
		Table tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);

		boolean useADS = m_APMUtils.useADS(tAPMArgumentTable);

		if (useADS) return 1;

		tParams = Table.tableNew();
		err_xstring = Str.xstringNew();

		// Retrieve parameter table
		iRetVal = Apm.performOperation(m_APMUtils.APM_BATCH_START, 1, tParams, err_xstring);
		
		if (iRetVal != 0) {
		   int iOverallRetVal = 1;
			for (iRow = 1; (iRetVal == 1 && (iRow <= tPackageDetails.getNumRows())); iRow++) {
				sPackageName = tPackageDetails.getString("package_name", iRow);
				ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);

				if (!useADS) {
					// Populate the tParams
					tParams.getTable("parameter_value", 1).setInt("entity_group_id", 1, entityGroupId);
					tParams.setTable(2, 2, tAPMArgumentTable.getTable("Scenario_List", 1));
					tParams.getTable("parameter_value", 3).setString("package", 1, sPackageName);
					tParams.getTable("parameter_value", 4).setInt("service_id", 1, tAPMArgumentTable.getInt("service_id", 1));
					tParams.getTable("parameter_value", 5).setInt("dataset_type_id", 1, tAPMArgumentTable.getInt("dataset_type_id", 1));

					// Now perform the operation itself
					iRetVal = Apm.performOperation(m_APMUtils.APM_BATCH_START, 0, tParams, err_xstring);

					if (iRetVal == 0) {
					   m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Error performing operation APM_ActionBatchStartOperation: " + Str.xstringGetString(err_xstring) + "\n");
					   iOverallRetVal = 0;
					}
					
					// Destroy the local parameters but don't destroy the
					// scenarios
					// tables
					tParams.setTable(2, 2, Util.NULL_TABLE);
				}
			}
			iRetVal = iOverallRetVal;

			ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
		}
		tParams.destroy();

		Str.xstringDestroy(err_xstring);

		return iRetVal;
   }

   public int commitPendingDatasets(Table tAPMArgumentTable, int entityGroupId) throws OException
   {
		XString err_xstring;
		Table tParams;
		int iRetVal = 1;
		int iRow;
		String sPackageName;
		
		Table tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);
		Table tBatchFailures = tAPMArgumentTable.getTable("Batch Failures", 1);
		tBatchFailures.sortCol("entity_group_id");
		
		// Retrieve parameter table
		boolean useADS = m_APMUtils.useADS(tAPMArgumentTable);

		// Debug.sleep for 1 sec to make sure msg appears in the right order
		// (after prior messages) in online pfolios screen. Only when not using ADS.
		if (!useADS) {
		  err_xstring = Str.xstringNew();
                  tParams = Table.tableNew();

		  Debug.sleep(1000);

		  iRetVal = Apm.performOperation(m_APMUtils.APM_BATCH_END, 1, tParams, err_xstring);

		  if (iRetVal != 0) {

			// Populate the tParams
			tParams.getTable("parameter_value", 1).setInt("entity_group_id", 1, entityGroupId);
			tParams.setTable(2, 2, tAPMArgumentTable.getTable("Scenario_List", 1));
			tParams.getTable("parameter_value", 4).setInt("service_id", 1, tAPMArgumentTable.getInt("service_id", 1));
			tParams.getTable("parameter_value", 5).setInt("dataset_type_id", 1, tAPMArgumentTable.getInt("dataset_type_id", 1));

			for (iRow = 1; (iRetVal == 1 && (iRow <= tPackageDetails.getNumRows())); iRow++) {

				sPackageName = tPackageDetails.getString("package_name", iRow);
			        tParams.getTable("parameter_value", 3).setString("package", 1, sPackageName);

					ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);

				// the operation adds a row for the dataset ids to the end -
				// remove row if it exists as
				// otherwise we get multiple dataset id rows
				if (tParams.getNumRows() == 6)
					tParams.delRow(6);

				// Now perform the operation itself
				iRetVal = Apm.performOperation(m_APMUtils.APM_BATCH_END, 0, tParams, err_xstring);

				if (iRetVal == 0)
					m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Error performing operation m_APMUtils.APM_BATCH_END: " + Str.xstringGetString(err_xstring) + "\n");

				// Clear the olf batch completion messages for this
				// entity group/package
				if (iRetVal != 0 ) {
					iRetVal = APM_ClearTFEMsgLogForCompletionMsg(entityGroupId, sPackageName, tAPMArgumentTable);
					if (iRetVal == 0)
					   m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Failed to clear apm_msg_log for completion messages");

					// Clear the bad update essages for this entity group/package
					if (iRetVal != 0) {
					   iRetVal =  m_APMUtils.APM_ClearMsgLogForBadUpdates(m_APMUtils.cModeBatch, entityGroupId, -1, sPackageName, tAPMArgumentTable);
					   if (iRetVal == 0)
					      m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Failed to clear bad updates following successful batch completion");
					}

					if (iRetVal != 0 ) {
					   // Mark the batch as complete for this entity group/package
					   String entityGroupName = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);					   
					   m_APMUtils.APM_LogStatusMessage(m_APMUtils.cModeBatch, 0, m_APMUtils.cStatusMsgTypeCompleted, "", sPackageName, entityGroupId, -1, -1, -1, -1, tAPMArgumentTable,
							tParams.getTable("parameter_value", 6), "Sending " + entityGroupName + "/" + sPackageName + " online");
					   m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Sending " + entityGroupName + "/" + sPackageName + " online");
					}
				}
			}
			// Destroy the local parameters but don't destroy the scenarios
			// table
			tParams.setTable(2, 2, Util.NULL_TABLE);

				ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
		  }

		  tParams.destroy();
		  Str.xstringDestroy(err_xstring);
		}
		
		return iRetVal;
   }
   
	private int APM_ClearTFEMsgLogForCompletionMsg(int entityGroupId, String sPackageName, Table tAPMArgumentTable) throws OException {
		Table tPfolio;
		int iRetVal;

		/* set up the table */
		tPfolio = Table.tableNew("params");
		tPfolio.addCol("service_id", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("primary_entity_num", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("completion_msg", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("package", COL_TYPE_ENUM.COL_STRING);
		tPfolio.addRow();
		tPfolio.setInt(1, 1, tAPMArgumentTable.getInt("service_id", 1));
		tPfolio.setInt(2, 1, -1); /* not at entity level */
		tPfolio.setInt(3, 1, 1); /* delete completion messages */
		tPfolio.setInt(4, 1, entityGroupId);
		tPfolio.setString(5, 1, sPackageName);

		iRetVal = m_APMUtils.APM_DBASE_RunProc(tAPMArgumentTable, "USER_clear_apm_msg_log", tPfolio);
		if (iRetVal == 0)
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() of USER_clear_apm_msg_log failed"));

		tPfolio.destroy();

		return iRetVal;
	}
   
}
