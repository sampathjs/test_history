/* Released with version 05-Feb-2020_V17_0_126 of APM */

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
import standard.apm.ads.IStatusLogger;
import standard.include.APM_Utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

// All code in this file used to exist in APM_ADSLogStatusMessage.

public class ADS_LogStatusMessage {
	private Boolean useADS = false;

	// Status message types ... 
	public int cStatusMsgTypeWarn = 1;
	public int cStatusMsgTypeFailure = 2;
	public int cStatusMsgTypeProcessing = 3;
	public int cStatusMsgTypeCompleted = 4; 
	public int cStatusMsgTypeBadUpdateRerun = 5;
	public int cStatusMsgTypeProcessingAlways = 6;
	public int cStatusMsgTypeStarted = 7;

	private APM_Utils m_APMUtils;

	public ADS_LogStatusMessage() {
		m_APMUtils = new APM_Utils();
	}
	
	///
	public int execute(Table argt) throws OException {

		int retVal = 1;
		
		// Retrieve parameter table
		if ( argt.getInt("use_ads", 1) == 1 )
				useADS = true;

		if (!useADS)
			return retVal;
		
		String message = argt.getString("msg_text", 1);
		String packageName = argt.getString("package", 1);
		int messageType = argt.getInt("msg_type", 1);
		int entityGroupId = argt.getInt("entity_group_id", 1);
		int scenarioId = argt.getInt("scenario_id", 1);
		int datasetTypeId = argt.getInt("dataset_type_id", 1);
		int serviceId = argt.getInt("service_id", 1);
		int primaryEntityNum = argt.getInt("primary_entity_num", 1);
		int secondaryEntityNum = argt.getInt("secondary_entity_num", 1);
		
		String primaryEntityName = m_APMUtils.GetCurrentEntityType(argt).toString();
		String entityGroupIdName = m_APMUtils.GetCurrentEntityType(argt).getEntityGroup();
		
		retVal = logStatusMessage(messageType, packageName, entityGroupId, scenarioId, datasetTypeId, serviceId, secondaryEntityNum, primaryEntityNum, primaryEntityName, entityGroupIdName, message);

		return retVal;
	}

	/**
	 * Logs the Status Message in to either ADS Message or Status Cache
	 * 
	 * @param iMsgType -
	 *            message type
	 * @param tAPMArgumentTable -
	 *            arg for other info (e.g. dataset_type, list of all packages,
	 *            entityGroupIds, scenarios)
	 * @param packageName -
	 *            Current package name
	 * @param entityGroupId -
	 *            Current entity group id being processed
	 * @param scenario -
	 *            Current scenario being processed
	 * @param datasetType -
	 *            Current datasetType being processed
	 * @param secondaryEntityNum -
	 *            Current secondaryEntityNum for incremental updates
	 * @param primaryEntityNum -
	 *            Current primaryEntityNum for incremental updates
	 * @param primaryEntityName -
	 *            Current primaryEntityName for incremental updates (e.g. Deal / Nomination)
	 * @param entityGroupIdName -
	 *            Current entityGroupIdName for incremental updates (e.g. Portfolio / Pipeline)
	 * @param message -
	 *            The message to log
	 * @throws OException
	 */
	public int logStatusMessage(int iMsgType, String packageName, int entityGroupId, int scenarioId, int datasetTypeId, int serviceId, int secondaryEntityNum, int primaryEntityNum, String primaryEntityName, String entityGroupIdName, String message) throws OException {

		int iRetVal = 1;

		try {
		   IStatusLogger adsLogging = Factory.getStatusLoggingImplementation();

		   try {
		      if (iMsgType == cStatusMsgTypeCompleted) {
		         adsLogging.batchCompleted(packageName, entityGroupId, scenarioId, datasetTypeId, serviceId, entityGroupIdName);
		      } else if (iMsgType == cStatusMsgTypeStarted) {
		         adsLogging.batchStarted(packageName, entityGroupId, scenarioId, datasetTypeId, serviceId, entityGroupIdName);
		      } else if (iMsgType == cStatusMsgTypeBadUpdateRerun) {
		         adsLogging.removeBadUpdate(packageName, entityGroupId, scenarioId, datasetTypeId, serviceId, primaryEntityNum, primaryEntityName, entityGroupIdName);
		         adsLogging.writeProcessingMessage(packageName, entityGroupId, scenarioId, datasetTypeId, serviceId, message);
		      }
		      else if (iMsgType == cStatusMsgTypeFailure) {
		         if (secondaryEntityNum > 0)
		         {
		            adsLogging.addBadUpdate(packageName, entityGroupId, scenarioId, datasetTypeId, serviceId, primaryEntityNum, primaryEntityName, entityGroupIdName);
		            adsLogging.writeProcessingMessage(packageName, entityGroupId, scenarioId, datasetTypeId, serviceId, message);
		         }
		         else
		         {
		            // if it failed we also write the message so we can see the reason for failure
		            adsLogging.writeProcessingMessage(packageName, entityGroupId, scenarioId, datasetTypeId, serviceId, message);
		            adsLogging.batchFailed(packageName, entityGroupId, scenarioId, datasetTypeId, serviceId, entityGroupIdName);
		         }
		      } 
		      else {
		         adsLogging.writeProcessingMessage(packageName, entityGroupId, scenarioId, datasetTypeId, serviceId, message);
		      }

		   } 
		   catch (ADSException e) {
		      printStackTrace("Error logging status message to ADS", e);
            iRetVal = 0;
		   }

		} 
		catch (Exception e) {
		   printStackTrace("Error connecting to ADS!", e);
		   iRetVal = 0;
		}

      return iRetVal;
	}

	/**
	 * Print out the full stack trace to the OConsole.
	 * 
	 * @param message
	 * @param e
	 * @throws OException
	 */
	public static void printStackTrace(String message, Exception e) throws OException {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		OConsole.oprint(message + ":- " + sw.toString());
	}
}
