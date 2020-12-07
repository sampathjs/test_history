/* Released with version 05-Feb-2020_V17_0_126 of APM */

package standard.apm;

import standard.include.APM_Utils;
import standard.include.ConsoleLogging;

import com.olf.apm.interfaces.datasink.IDataSink;
import com.olf.apm.ioc.Container;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class APM_BatchOps_ADA
{
	private APM_Utils m_APMUtils;
	private IDataSink m_db;

	static {
		Container.createInstance(APM_BatchOps_ADA.class.getClassLoader());
	}
	
	public APM_BatchOps_ADA()
	{
		m_APMUtils = new APM_Utils();
		
		m_db = (IDataSink) Container.instance().getInstance("IDataSink");
	}

	public int initialiseDatasets(Table tAPMArgumentTable, int entityGroupId) throws OException
	{	
		// Do nothing.
		return 1;
	}

	public int commitPendingDatasets(Table tAPMArgumentTable, int entityGroupId) throws OException
	{	
		// Do nothing.
		return 1;
	}

	public int truncateDataTable(Table tAPMArgumentTable) throws OException
	{
		int retVal = 1;

		try
		{
			Table packageDetailTable = tAPMArgumentTable.getTable("Package Details", 1);

			int numRows = packageDetailTable.getNumRows();
			for (int row = 1; row <= numRows; row++)
			{
				String packageName = packageDetailTable.getString("package_name", row);
				
				ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, packageName);
				
				Table reportingData = packageDetailTable.getTable("package_data_tables", row).getTable(1, 1);
				String cacheName = reportingData.getTableName();

				try
				{
					retVal = m_db.initializeDatasetForBatch(tAPMArgumentTable, cacheName);
				}
				catch (OException e)
				{
					retVal = 0;
				}
				
				ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
				
				if (retVal == 0)
				{
					break;
				}
			}
		}
		catch (Exception e)
		{
			retVal = 0;
			m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Unknown Exception while calling ADA" + ".Exception details:" + e);
			String message = m_APMUtils.getStackTrace(e);
			m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, message + "\n");
		}

		return retVal;
	}

	public int commitDataTable(Table tAPMArgumentTable) throws OException
	{
		int retVal = 1;

		Table tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);
		Table tBatchFailures = tAPMArgumentTable.getTable("Batch Failures", 1);
		Table tEntityGroups = tAPMArgumentTable.getTable("Selected Entity Groups", 1);
		tBatchFailures.sortCol("entity_group_id");

		// If there are any failures then we will not commit.
		if (tBatchFailures.getNumRows() > 0)
		{
			return 0;
		}

		// Commit each package.
		int packageDetailsNumRows = tPackageDetails.getNumRows();
		for (int row = 1; retVal == 1 && row <= packageDetailsNumRows; row++)
		{
			String packageName = tPackageDetails.getString("package_name", row);
			
			ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, packageName);
			
			String cacheName = tPackageDetails.getTable("package_data_tables", row).getTable(1, 1).getTableName();

			try
			{
				retVal = m_db.commitDatasetForBatch(tAPMArgumentTable, cacheName);
			}
			catch (OException e)
			{
				retVal = 0;
			}
			
			ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
		}

		// If committing the batch failed the don't clear the bad update message log.
		if (retVal == 0)
		{
			return retVal;
		}
		
		// Clear bad update message log.
		int entityGroupsNumRows = tEntityGroups.getNumRows();
		for (int entityRow = 1; entityRow <= entityGroupsNumRows; entityRow++)
		{
			int entityGroupId = tEntityGroups.getInt(1, entityRow);
			String entityGroupName = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);

			ConsoleLogging.instance().setEntityGroupContext(tAPMArgumentTable, entityGroupName, entityGroupId);

			for (int packageRow = 1; packageRow <= packageDetailsNumRows; packageRow++)
			{
				String sPackageName = tPackageDetails.getString("package_name", packageRow);

				ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);

				Table scenarios = tAPMArgumentTable.getTable("Scenario_List", 1);
				int numScenarios = scenarios.getNumRows();

				for (int scenarioRow = 1; scenarioRow <= numScenarios; scenarioRow++)
				{
					int scenarioId = scenarios.getInt("scenario_id", scenarioRow);
					String scenarioName = scenarios.getString("scenario_name", scenarioRow);

					ConsoleLogging.instance().setScenarioContext(tAPMArgumentTable, scenarioName, scenarioId);

					// Clear the bad update messages for this key
					if (retVal != 0)
					{
						retVal = m_APMUtils.APM_ClearMsgLogForBadUpdates(m_APMUtils.cModeBatch, entityGroupId, scenarioId, sPackageName, tAPMArgumentTable);
						if (retVal == 0)
						{
							m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Failed to clear bad updates following successful batch completion");
						}
					}

					if (retVal != 0)
					{
						// Mark the batch as complete for this entity group/package
						m_APMUtils.APM_LogStatusMessage(m_APMUtils.cModeBatch, 0, m_APMUtils.cStatusMsgTypeCompleted, "", sPackageName, entityGroupId, scenarioId, -1, -1, -1,
								tAPMArgumentTable, Util.NULL_TABLE, "Sending " + entityGroupName + "/" + sPackageName + " online");

						m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Sending " + entityGroupName + "/" + sPackageName + " online");
					}

					ConsoleLogging.instance().unSetScenarioContext(tAPMArgumentTable);
				}

				if (retVal != 0 )
				{
					retVal = APM_ClearTFEMsgLogForCompletionMsg(entityGroupId, sPackageName, tAPMArgumentTable);
					if (retVal == 0)
					{
						m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Failed to clear apm_msg_log for completion messages");
					}
				}

				// Clear the bad update essages for this entity group/package
				if (retVal != 0)
				{
					retVal = m_APMUtils.APM_ClearMsgLogForBadUpdates(m_APMUtils.cModeBatch, entityGroupId, -1, sPackageName, tAPMArgumentTable);
					if (retVal == 0)
					{
						m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Failed to clear bad updates following successful batch completion");
					}
				}

				if (retVal != 0) {
					// Mark the batch as complete for this entity group/package
					m_APMUtils.APM_LogStatusMessage(m_APMUtils.cModeBatch, 0, m_APMUtils.cStatusMsgTypeCompleted, "", sPackageName, entityGroupId, -1, -1, -1, -1, tAPMArgumentTable,
							null, "Sending " + entityGroupName + "/" + sPackageName + " online");
					m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Sending " + entityGroupName + "/" + sPackageName + " online");
				}

				ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
			}

			ConsoleLogging.instance().unSetEntityGroupContext(tAPMArgumentTable);
		}

		return retVal;
	}

	private int APM_ClearTFEMsgLogForCompletionMsg(int iEntityGroupId, String sPackageName, Table tAPMArgumentTable) throws OException
	{
		Table tEntity;
		int iRetVal;

		/* set up the table */
		tEntity = Table.tableNew("params");
		tEntity.addCol("service_id", COL_TYPE_ENUM.COL_INT);
		tEntity.addCol("primary_entity_num", COL_TYPE_ENUM.COL_INT);
		tEntity.addCol("completion_msg", COL_TYPE_ENUM.COL_INT);
		tEntity.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
		tEntity.addCol("package", COL_TYPE_ENUM.COL_STRING);
		tEntity.addRow();
		tEntity.setInt(1, 1, tAPMArgumentTable.getInt("service_id", 1));
		tEntity.setInt(2, 1, -1); /* not at entity level */
		tEntity.setInt(3, 1, 1); /* delete completion messages */
		tEntity.setInt(4, 1, iEntityGroupId);
		tEntity.setString(5, 1, sPackageName);

		iRetVal = m_APMUtils.APM_DBASE_RunProc(tAPMArgumentTable, "USER_clear_apm_msg_log", tEntity);
		if (iRetVal == 0)
		{
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() of USER_clear_apm_msg_log failed"));
		}

		tEntity.destroy();

		return iRetVal;
	}
	
	/// <summary>
	/// Cleanup the dataset control table in the event that the batch fails.
	/// </summary>
	public int cleanDatasetControlTable(Table tAPMArgumentTable) throws OException
	{
		int retVal = 0;
		
		try
		{
			retVal = m_db.cleanDatasetControlTable(tAPMArgumentTable);
		}
		catch (OException e)
		{
			retVal = 0;
		}
		
		return retVal;
	}
}
