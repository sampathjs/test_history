/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.apm;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import standard.apm.ads.ADSException;
import standard.apm.ads.ADSInterface;
import standard.apm.ads.Factory;
import standard.apm.ads.IStatusLogger;
import standard.include.APM_Utils;
import standard.include.ApmLookupManager;
import standard.include.ApmMetadataManager;
import standard.include.ApmOlapManager;
import standard.include.ConsoleLogging;

import com.olf.apm.data.EndurDatabase;
import com.olf.apm.exception.DatabaseException;
import com.olf.apm.exception.FactoryException;
import com.olf.apm.exception.InvalidOlapMetadataException;
import com.olf.apm.exception.OlapMetadataVersionMismatch;
import com.olf.apm.logging.Logger;
//import com.olf.openjvs.OException;
//import com.olf.openjvs.Table;
//import com.olf.openjvs.Util;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class APM_BatchOps_ADS {
   private APM_Utils m_APMUtils;

   public APM_BatchOps_ADS() {
      m_APMUtils = new APM_Utils();
   }

   public void runStatusScript(Table tAPMArgumentTable) throws OException {
      boolean useADS = m_APMUtils.useADS(tAPMArgumentTable);
      if (useADS) {
         try {
            m_APMUtils.executeServiceStatus(tAPMArgumentTable);
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

            // used to collect package names to filter the olap metadata/lookup items
            Set<String> packageNames = new HashSet<>();

            for (int iRow = 1; (iRetVal == 1 && (iRow <= tPackageDetails.getNumRows())); iRow++) {
               String sPackageName = tPackageDetails.getString("package_name", iRow);

               // keep track of package names for olap metadata/lookup management
               packageNames.add(sPackageName);

               ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);

               Table reportingData = tPackageDetails.getTable("package_data_tables", iRow).getTable(1, 1);
               String cacheName = reportingData.getTableName();

               // m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup()
               iRetVal = ads.createDatasetGridCache(cacheName, reportingData, m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getPrimaryEntityFilter(), m_APMUtils
                        .GetCurrentEntityType(tAPMArgumentTable).getPrimaryEntity(), m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroupId(), m_APMUtils
                        .GetCurrentEntityType(tAPMArgumentTable).getEntityGroupFilter());

               for (int k = 1; k <= tScenarios.getNumRows(); k++) {
                  int scenarioID = tScenarios.getInt("scenario_id", k);
                  String scenarioName = tScenarios.getString("scenario_name", k);

                  ConsoleLogging.instance().setScenarioContext(tAPMArgumentTable, scenarioName, scenarioID);
                  ads.initPendingDatasetGridCache(cacheName, sPackageName, entityGroupId, datasetTypeId, scenarioID, m_APMUtils.GetCurrentEntityType(tAPMArgumentTable)
                           .getEntityGroupFilter(), m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityGroup());

                  initialiseDirectory(tAPMArgumentTable, cacheName, sPackageName, entityGroupId, datasetTypeId, scenarioName);
                  
                  ConsoleLogging.instance().unSetScenarioContext(tAPMArgumentTable);
               }

               ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
            }

            // after datasets have been initialised, setup the lookup and olap caches
            initialiseLookupListsAndMetadata(tAPMArgumentTable, packageNames);

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
         for (iRow = 1; (iRetVal == 1 && (iRow <= tPackageDetails.getNumRows())); iRow++) {

            sPackageName = tPackageDetails.getString("package_name", iRow);
            ConsoleLogging.instance().setPackageContext(tAPMArgumentTable, sPackageName);

            try {
               ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "ADS_CommitBatchDataSet");
               ADS_CommitBatchDataSet commitBatchDataSet = new ADS_CommitBatchDataSet();
               iRetVal = commitBatchDataSet.execute(tAPMArgumentTable);
            } catch (Exception t) {
               iRetVal = 0;
               m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Exception while calling ADS_CommitBatchDataSet: " + t);
               String message = m_APMUtils.getStackTrace(t);
               m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message + "\n");
            }

            ConsoleLogging.instance().setScriptContext(tAPMArgumentTable, "APM_BatchOps_ADS");

			if (iRetVal != 0 ) {
				iRetVal = APM_ClearTFEMsgLogForCompletionMsg(entityGroupId, sPackageName, tAPMArgumentTable);
				if (iRetVal == 0)
				   m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Failed to clear apm_msg_log for completion messages");
			}
			
            // Clear the bad update essages for this entity group/package
            if (iRetVal != 0) {
               iRetVal = m_APMUtils.APM_ClearMsgLogForBadUpdates(m_APMUtils.cModeBatch, entityGroupId, -1, sPackageName, tAPMArgumentTable);
               if (iRetVal == 0)
                  m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Failed to clear bad updates following successful batch completion");
            }

            if (iRetVal != 0) {
               // Mark the batch as complete for this entity group/package
               String entityGroupName = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupId);
               m_APMUtils.APM_LogStatusMessage(m_APMUtils.cModeBatch, 0, m_APMUtils.cStatusMsgTypeCompleted, "", sPackageName, entityGroupId, -1, -1, -1, -1, tAPMArgumentTable,
                        null, "Sending " + entityGroupName + "/" + sPackageName + " online");
               m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Sending " + entityGroupName + "/" + sPackageName + " online");
            }
         }

         ConsoleLogging.instance().unSetPackageContext(tAPMArgumentTable);
      }

      return iRetVal;
   }

	private void initialiseLookupListsAndMetadata(Table tAPMArgumentTable, Set<String> packageNames) throws Exception {
		String saveApmOlapToAds = System.getenv("AB_APM_SAVE_OLAP_TO_ADS");

		// only proceed if the apm client (olap) metadata is to be saved into ads
		if (saveApmOlapToAds != null && saveApmOlapToAds.equalsIgnoreCase("true")) {
			try (Logger logger = Logger.instance()) {
				logger.info("Initialising lookup lists and OLAP metadata...");

				EndurDatabase databaseFacade = new EndurDatabase();

				// load the apm client metadata definitions
				ApmMetadataManager metadataManager = null;
				try {
					metadataManager = new ApmMetadataManager(databaseFacade, packageNames);
				} catch (DatabaseException exception) {
					// do error reporting, then let the exception go through
					m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Exception while loading the lookup lists data from the database: "
					        + exception.getMessage());

					throw exception;
				}
				
				ApmLookupManager lookupManager = null;
				try {
					lookupManager = new ApmLookupManager(databaseFacade);
				} catch (FactoryException exception) {
					// do error reporting, then let the exception go through
					m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Exception when instantiating the OLAP Metadata API facade: "
					        + exception.getMessage());

					throw exception;
				}
				
				lookupManager.createLookupLists(metadataManager.getFilterDefinitions());
				lookupManager.save();

				ApmOlapManager olapManager = null;
				try {
					olapManager = new ApmOlapManager();
				} catch (FactoryException exception) {
					// do error reporting, then let the exception go through
					m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Exception when instantiating the OLAP Metadata API facade: "
					        + exception.getMessage());

					throw exception;
				}

				olapManager.createMetadata(metadataManager.getConfigurationDefinition(), metadataManager.getPackageDefinitions(), metadataManager.getFilterDefinitions(), metadataManager.getColumnDefinitions());

				try {
					olapManager.save();
				} catch (OlapMetadataVersionMismatch exception) {
					// do error reporting, then let the exception go through
					m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "OLAP metadata version mismatch: "
					        + exception.getMessage());

					throw exception;
				} catch (InvalidOlapMetadataException exception) {
					// do error reporting, then let the exception go through
					m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Invalid OLAP metadata: "
					        + exception.getMessage());

					throw exception;
				}

				try {
					String serviceName = tAPMArgumentTable.getString("service_name", 1);
					String filename = "C:\\apm_mondrian_schema_" + serviceName.replace(' ', '-') + ".xml";

					olapManager.exportAsMondrian(filename);
				} catch (IOException exception) {
					// do error reporting, then let the exception go through
					m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "Exception when exporting the Mondrian schema XML: "
					        + exception.getMessage());
				} catch (OException exception) {
					// do error reporting, then let the exception go through
					m_APMUtils.APM_PrintAndLogErrorMessage(m_APMUtils.cModeBatch, tAPMArgumentTable, "The filename to export the Mondrian schema is not valid: "
					        + exception.getMessage());
				}
				logger.info("Finished committing lookup lists and OLAP metadata into ADS.");
			} finally {
				Logger.clearLockFiles();
			}
		}
	}
   
   private void initialiseDirectory(Table tAPMArgumentTable, String cacheName, String packageName, int entityGroupID, int datasetTypeID, String scenarioName) throws Exception {
      String saveApmOlapToAds = System.getenv("AB_APM_SAVE_OLAP_TO_ADS");

      // only proceed if the apm client (olap) metadata is to be saved into ads
      if (saveApmOlapToAds == null || !saveApmOlapToAds.equalsIgnoreCase("true")) {
    	  return;
      }
      
      String entityGroupName = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getFormattedNameForGroupId(entityGroupID);
      String entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable).getEntityType();
      String datasetTypeName = m_APMUtils.APM_DatasetTypeIdToName(tAPMArgumentTable, datasetTypeID, false);
      
      ApmOlapManager.createDirectory(tAPMArgumentTable, cacheName, packageName, entityGroupName, entityType, datasetTypeName, scenarioName);
   }

   private int APM_ClearTFEMsgLogForCompletionMsg(int iEntityGroupId, String sPackageName, Table tAPMArgumentTable) throws OException {
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
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() of USER_clear_apm_msg_log failed"));

		tEntity.destroy();

		return iRetVal;
	}
}
