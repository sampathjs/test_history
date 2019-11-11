/* Released with version 29-Aug-2019_V17_0_124 of APM */

/*
 Description : This forms part of the Trader Front End, Active Position Manager
 package

 -------------------------------------------------------------------------------
 Revision No.  Date        Who  Description
 -------------------------------------------------------------------------------
 1.0.0         
 */

package standard.apm;

import standard.apm.ads.ADSException;
import standard.apm.ads.Factory;
import standard.apm.ads.ADSInterface;
import standard.include.APM_Utils;
import standard.include.ConsoleLogging;

import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;

// All code in this file used to exist in APM_ADSCommitBatchDataSet.

public class ADS_CommitBatchDataSet {
	private APM_Utils m_APMUtils;

	public ADS_CommitBatchDataSet() {
		m_APMUtils = new APM_Utils();

	}

	public int execute(Table argt) throws OException {

		boolean datasetKeyFailed;
		String failPackageName;
		int failScenarioID;
		int retVal = 1;

		// Retrieve parameter table
		boolean useADS = m_APMUtils.useADS(argt);

		if (!useADS)
			return retVal;

		try {
			ADSInterface ads = Factory.getADSImplementation();

			Table tPackageDetails = argt.getTable("Package Details", 1);
			Table tBatchFailures = argt.getTable("Batch Failures", 1);
			tBatchFailures.sortCol("entity_group_id");

			Table scenarios = argt.getTable("Scenario_List", 1);
			String packageName = argt.getString("Current Package", 1);

			int entityGroupId = argt.getInt("Current Entity Group Id", 1);
			int datasetTypeId = argt.getInt("dataset_type_id", 1);
			int numScenarios = scenarios.getNumRows();
			int packageRow = tPackageDetails.unsortedFindString("package_name", packageName, com.olf.openjvs.enums.SEARCH_CASE_ENUM.CASE_SENSITIVE);
			String cacheName = tPackageDetails.getTable("package_data_tables", packageRow).getTable(1, 1).getTableName();

			for (int i = 1; i <= numScenarios; i++) {
				int scenarioId = scenarios.getInt("scenario_id", i);
				String scenarioName = scenarios.getString("scenario_name", i);

				ConsoleLogging.instance().setScenarioContext(argt, scenarioName, scenarioId);

				// don't do it if it actually failed earlier
				int firstRow = tBatchFailures.findInt("entity_group_id", entityGroupId, com.olf.openjvs.enums.SEARCH_ENUM.FIRST_IN_GROUP);
				int lastRow = tBatchFailures.findInt("entity_group_id", entityGroupId, com.olf.openjvs.enums.SEARCH_ENUM.LAST_IN_GROUP);
				datasetKeyFailed = false;
				if (firstRow > 0) {
					for (int failRow = firstRow; failRow <= lastRow; failRow++) {
						failPackageName = tBatchFailures.getString("package", failRow);
						failScenarioID = tBatchFailures.getInt("scenario_id", failRow);

						if (failScenarioID == scenarioId && Str.equal(failPackageName, packageName) == 1) {
							datasetKeyFailed = true;
							break;
						}
					}
				}

				if (datasetKeyFailed == true)
					continue;

				retVal = ads.commitPendingDatasetGridCache(cacheName, packageName, entityGroupId, datasetTypeId, scenarioId,
						m_APMUtils.GetCurrentEntityType(argt).getEntityGroupFilter(),
						m_APMUtils.GetCurrentEntityType(argt).getEntityGroup());

				// Clear the bad update messages for this key
				if (retVal != 0) {
					retVal = m_APMUtils.APM_ClearMsgLogForBadUpdates(m_APMUtils.cModeBatch, entityGroupId, scenarioId, packageName, argt);
					if (retVal == 0)
						m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, argt, "Failed to clear bad updates following successful batch completion");
				}

				if (retVal != 0) {
					// Mark the batch as complete for this entity group/package
					String entityGroupName = m_APMUtils.GetCurrentEntityType(argt).getFormattedNameForGroupId(entityGroupId);
					m_APMUtils.APM_LogStatusMessage(m_APMUtils.cModeBatch, 0, m_APMUtils.cStatusMsgTypeCompleted, "", packageName, entityGroupId, scenarioId, -1, -1, -1,
					                                argt, Util.NULL_TABLE, "Sending " + entityGroupName + "/" + packageName + " online");

					m_APMUtils.APM_PrintMessage(argt, "Sending " + entityGroupName + "/" + packageName + " online");
				}
			}
		} catch (InstantiationException e) {
			m_APMUtils.APM_PrintErrorMessage(argt, "Unable to instantiate ADS implementation" + e);
			retVal = 0;
			String message = m_APMUtils.getStackTrace(e);
			m_APMUtils.APM_PrintErrorMessage(argt, message + "\n");
		} catch (IllegalAccessException e) {
			m_APMUtils.APM_PrintErrorMessage(argt, "Illegal Access: Unable to instantiate ADS implementation" + e);
			retVal = 0;
			String message = m_APMUtils.getStackTrace(e);
			m_APMUtils.APM_PrintErrorMessage(argt, message + "\n");
		} catch (ClassNotFoundException e) {
			m_APMUtils.APM_PrintErrorMessage(argt, "ClassNotFound: Unable to instantiate ADS implementation" + e);
			retVal = 0;
			String message = m_APMUtils.getStackTrace(e);
			m_APMUtils.APM_PrintErrorMessage(argt, message + "\n");
		} catch (ADSException e) {
			m_APMUtils.APM_PrintErrorMessage(argt, "Exception while calling ADS: " + e);
			retVal = 0;
			String message = m_APMUtils.getStackTrace(e);
			m_APMUtils.APM_PrintErrorMessage(argt, message + "\n");
		} catch (Exception t) {
			retVal = 0;
			m_APMUtils.APM_PrintErrorMessage(argt, "Unknown Exception while calling ADS: " + t);
			String message = m_APMUtils.getStackTrace(t);
			m_APMUtils.APM_PrintErrorMessage(argt, message + "\n");
		}

		ConsoleLogging.instance().unSetScenarioContext(argt);

		return retVal;
	}
}
