/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;

import standard.apm.ads.ADSException;
import standard.apm.ads.ADSInterface;
import standard.apm.ads.Factory;
import standard.apm.ads.IStatusLogger;
import standard.include.APM_Utils;
import standard.include.ConsoleLogging;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class APM_BatchOps_ADS {
	private APM_Utils m_APMUtils;

	public APM_BatchOps_ADS() {
		m_APMUtils = new APM_Utils();
	}

	public void runStatusScript(Table tAPMArgumentTable) throws OException {
		boolean useADS = m_APMUtils.useADS(tAPMArgumentTable);
		if (useADS) {
			try {
				m_APMUtils.executeStatusScript(tAPMArgumentTable);
			} catch (Exception t) {
				m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Unknown Exception while calling ADS" + ".Exception details:" + t);
				String message = m_APMUtils.getStackTrace(t);
				m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, message + "\n");
			}
		}
	}

	public int initialiseDatasets(Table tAPMArgumentTable, int entityGroupId) throws OException {
		boolean useADS = m_APMUtils.useADS(tAPMArgumentTable);
		int iRetVal = 1;

		if (useADS) {
			ADSInterface ads;
			try {
				try {
					@SuppressWarnings("unused")
                    IStatusLogger adsLogging = Factory.getStatusLoggingImplementation();
				} catch (Exception t) {
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Unable to load StatusLogger.");
					String message = m_APMUtils.getStackTrace(t);
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message + "\n");
					// return failed...
				}

				ads = Factory.getADSImplementation();

				Table tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);
				Table tScenarios = tAPMArgumentTable.getTable("Scenario_List", 1);
				int datasetTypeId = tAPMArgumentTable.getInt("dataset_type_id", 1);

				for (int iRow = 1; (iRetVal == 1 && (iRow <= tPackageDetails.getNumRows())); iRow++) {
					String sPackageName = tPackageDetails.getString("package_name", iRow);
					
					ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);
					
					Table reportingData = tPackageDetails.getTable("package_data_tables", iRow).getTable(1, 1);
					String cacheName = reportingData.getTableName();

					//m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup()
					iRetVal = ads.createDatasetGridCache(cacheName, reportingData,
							m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getPrimaryEntityFilter(),
							m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getPrimaryEntity(),
							m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroupId(),
							m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroupFilter());

					for (int k = 1; k <= tScenarios.getNumRows(); k++) {
						int scenarioID = tScenarios.getInt("scenario_id", k);
						String scenarioName = tScenarios.getString("scenario_name", k);

						ConsoleLogging.instance().setScenarioContext(tAPMArgumentTable, scenarioName, scenarioID);
						ads.initPendingDatasetGridCache(cacheName, sPackageName, entityGroupId, datasetTypeId, scenarioID,
								m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroupFilter(),
								m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup());

						ConsoleLogging.instance().unSetScenarioContext(tAPMArgumentTable);
					}

					ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
				}
			} catch (InstantiationException e) {
				iRetVal = 0;
				m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Unable to instantiate ADS implementation" + e);
			} catch (IllegalAccessException e1) {
				iRetVal = 0;
				m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Illegal Access: Unable to instantiate ADS implementation" + e1);
			} catch (ClassNotFoundException e2) {
				iRetVal = 0;
				m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "ClassNotFound: Unable to instantiate ADS implementation" + e2);
			} catch (ADSException e3) {
				iRetVal = 0;
				m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Exception while calling ADS: " + e3);
			} catch (Exception t) {
				iRetVal = 0;
				m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Unknown Exception while calling ADS" + ".Exception details:" + t);
				String message = m_APMUtils.getStackTrace(t);
				m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, message + "\n");
			}
		}

		return iRetVal;
	}

	public int commitPendingDatasets(Table tAPMArgumentTable, int entityGroupId) throws OException {
		int iRetVal = 1;
		int iRow;
		String sPackageName;

		Table tPackageDetails = tAPMArgumentTable.getTable("Package Details", 1);
		Table tBatchFailures = tAPMArgumentTable.getTable("Batch Failures", 1);
		tBatchFailures.sortCol("entity_group_id");

		boolean useADS = m_APMUtils.useADS(tAPMArgumentTable);

		if (useADS) {
			String script_name = m_APMUtils.find_script_path("APM_ADSCommitBatchDataSet");
			for (iRow = 1; (iRetVal == 1 && (iRow <= tPackageDetails.getNumRows())); iRow++) {

				sPackageName = tPackageDetails.getString("package_name", iRow);
				ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);

				try {
					ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "APM_ADSCommitBatchDataSet");
					iRetVal = Util.runScript(script_name, tAPMArgumentTable, Util.NULL_TABLE);
				} catch (Exception t) {
					iRetVal = 0;
					m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Exception while calling APM_ADSCommitBatchDataSet: " + t);
					String message = m_APMUtils.getStackTrace(t);
					m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message + "\n");
				}
				
				ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "APM_BatchOps_ADS");

				// Clear the bad update essages for this entity group/package
				if (iRetVal != 0) {
					iRetVal = m_APMUtils.APM_ClearMsgLogForBadUpdates(m_APMUtils.cModeBatch, entityGroupId, -1, sPackageName, tAPMArgumentTable);
					if (iRetVal == 0)
						m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable,
						                                       "Failed to clear bad updates following successful batch completion");
				}

				if (iRetVal != 0) {
					// Mark the batch as complete for this entity group/package
					String entityGroupName = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);
					m_APMUtils.APM_LogStatusMessage(m_APMUtils.cModeBatch, 0, m_APMUtils.cStatusMsgTypeCompleted, "", sPackageName, entityGroupId, -1, -1, -1, -1,
					                                tAPMArgumentTable, null, "Sending " + entityGroupName  + "/" + sPackageName + " online");
					m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Sending " + entityGroupName + "/" + sPackageName + " online");
				}
			}

			ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
		}

		return iRetVal;

	}
}
