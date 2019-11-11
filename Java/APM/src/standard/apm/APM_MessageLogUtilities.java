/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.apm;

import standard.include.APM_Utils;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class APM_MessageLogUtilities
{
	private APM_Utils m_APMUtils;

   public APM_MessageLogUtilities() {
		m_APMUtils = new APM_Utils();
   }

	/*-------------------------------------------------------------------------------
	Name:          APM_ClearEntriesInMsgLog

	Description:   This routine will clear a set of messages from the apm_msg_log table

	Parameters:    sProcessingMessage - page subject
	           entityNum - entityNum to remove (can be 0 to indicate not for an entity num)

	Return Values: None
	-------------------------------------------------------------------------------*/
	public int APM_ClearEntriesInMsgLog(String sProcessingMessage, int entityNum, Table tAPMArgumentTable, int iServiceID) throws OException {
		Table tPfolio;
		int iRetVal;

		tPfolio = Table.tableNew("params");
		tPfolio.addCol("service_id", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("primary_entity_num", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("completion_msg", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
		tPfolio.addCol("package", COL_TYPE_ENUM.COL_STRING);
		tPfolio.addRow();
		tPfolio.setInt(1, 1, iServiceID);
		tPfolio.setInt(2, 1, entityNum);
		tPfolio.setInt(3, 1, 0); /* don't delete completion messages */
		tPfolio.setString(5, 1, ""); /* empty - unused here */
		iRetVal = m_APMUtils.APM_DBASE_RunProc(tAPMArgumentTable, "USER_clear_apm_msg_log", tPfolio);

		if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Error clearing APM apm_msg_log table prior to batch for service : " + iServiceID + " : "
					+ DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBase.runProc() failed") + "\n");
			iRetVal = 0;
		}

		tPfolio.destroy();

		return iRetVal;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_PurgeGuaranteedMsgLog
	Description:   Purges the guaranteed message log
	Parameters:      Nothing
	Return Values:   Nothing
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public void APM_PurgeGuaranteedMsgLog(Table tAPMArgumentTable) throws OException {
		Table tArgs;
		int iRetVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		ODateTime dPurgeDateTime;

		// Create the function parameters and run the the stored proc
		tArgs = Table.tableNew("params");
		tArgs.addCol("message_type", COL_TYPE_ENUM.COL_INT);
		tArgs.addCol("date_time", COL_TYPE_ENUM.COL_DATE_TIME);
		tArgs.addRow();

		tArgs.setInt(1, 1, -1); // purge for all message types

		dPurgeDateTime = ODateTime.dtNew();

		dPurgeDateTime.setDate(OCalendar.getServerDate() - 1); // yesterdays
		// date
		dPurgeDateTime.setTime(Util.timeGetServerTime());
		tArgs.setDateTime(2, 1, dPurgeDateTime);

		iRetVal = m_APMUtils.APM_DBASE_RunProc(tAPMArgumentTable, "USER_tfe_purgetype_gtd_msg", tArgs);
		tArgs.destroy();
		if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "APM_PurgeGuaranteedMsgLog failed to call stored proc USER_tfe_purgetype_gtd_msg");
			return;
		}

		return;
	}
   
}
