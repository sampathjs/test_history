package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Clear_Bad_Deal_Updates implements IScript {
   //#SDBG

/* Released with version 29-Aug-2019_V17_0_124 of APM */

// THIS SCRIPT REMOVES ALL BAD ENTITY UPDATES FOR APM FROM THE MSG LOG & QUEUE & SENDS OUT ENTITY RERUN NOTIFICATIONS

// forward declarations
// set later in the script
int major_version;
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   // SET THIS HERE IF YOU WANT TO CLEAR A SINGLE SERVICE (RATHER THAN ALL)
   String sServiceName = ""; 

   Table tServiceID, tVersion;
   int iServiceID, i, retval;

   tVersion = Ref.getVersion();
   major_version = tVersion.getInt( "major_version", 1);
   tVersion.destroy();

   if (!sServiceName.isEmpty())
      deleteBadUpdatesForService(sServiceName);
   else
   {
      retval = DBase.runSql("select name from job_cfg where type = 0 and service_group_type in (33, 46)" );
      if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed.  Could not find APM services\n" ) );
         Util.exitFail();
      }

      tServiceID = Table.tableNew();
      DBase.createTableOfQueryResults(tServiceID);

      for (i = 1; i <= tServiceID.getNumRows(); i++)
      {
         deleteBadUpdatesForService(tServiceID.getString( 1, i));
      }

      tServiceID.destroy();
   }
}


/*-------------------------------------------------------------------------------
Name:          deleteBadUpdatesForService
Description:   Finds bad updates for service, deletes them from log & queue 
               & notifies clients.
-------------------------------------------------------------------------------*/
int deleteBadUpdatesForService(String sServiceName) throws OException
{
   Table arg_table, arg_table2, tPackageName;
   Table tServiceID, tBadUpdates;
   String sPackageName, queryStr, sServiceID, sPageName;
   int retval, iServiceID, i;
   int iPrimaryEntityNum, iEntityGroupId, iSecondaryEntityNum, iDatasetTypeId, iScenarioId;

   if(Str.len(sServiceName) == 0)
   {
      if ( major_version < 8 )
      {
         tPackageName = Table.tableNew("package_name");
         tPackageName.addCol("package_name",COL_TYPE_ENUM.COL_STRING);
         tPackageName.addRow();
         USER_GetPackageName(tPackageName);
         sPackageName = tPackageName.getString("package_name",1);
         tPackageName.destroy();
         sServiceName = "APM Opservice Def " + sPackageName;
      }
      else
         sServiceName = "APM";
   }

   // find bad updates for this service in apm_msg_log
   queryStr = "select wflow_id from job_cfg where name = '" + sServiceName + "' and type = 0";
   retval = DBase.runSql(queryStr );
   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
   {
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed.  Could not find service ID\n" ) );
      return 0;
   }

   tServiceID = Table.tableNew();
   DBase.createTableOfQueryResults(tServiceID);
   iServiceID = tServiceID.getInt( 1, 1);
   tServiceID.destroy();

   queryStr = "select distinct secondary_entity_num, primary_entity_num, rtp_page_subject, package, entity_group_id, dataset_type_id, scenario_id from apm_msg_log where service_id = " + iServiceID + " and msg_type = 2 and primary_entity_num > -1";
   retval = DBase.runSql(queryStr );
   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
   {
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed. Could not load bad updates\n" ) );
      tServiceID.destroy();
      return 0;
   }

   tBadUpdates = Table.tableNew();
   DBase.createTableOfQueryResults(tBadUpdates);

   arg_table= Table.tableNew();
   arg_table.addCol( "service_id", COL_TYPE_ENUM.COL_INT);
   arg_table.addCol( "primary_entity_num", COL_TYPE_ENUM.COL_INT);
   arg_table.addCol( "completion_msg", COL_TYPE_ENUM.COL_INT);
   arg_table.addCol( "entity_group_id", COL_TYPE_ENUM.COL_INT);
   arg_table.addCol( "package", COL_TYPE_ENUM.COL_STRING);
   arg_table.addRow();
   arg_table.setInt( 3, 1, 0); /* don't delete completion messages */

   arg_table2 = Table.tableNew();
   arg_table2.addCol( "service_name", COL_TYPE_ENUM.COL_STRING);
   arg_table2.addCol( "secondary_entity_num", COL_TYPE_ENUM.COL_INT);
   arg_table2.addRow();

   for (i = 1; i <= tBadUpdates.getNumRows(); i++)
   {
      iPrimaryEntityNum = tBadUpdates.getInt( "primary_entity_num", i);
      iEntityGroupId = tBadUpdates.getInt( "entity_group_id", i);
      sPackageName = tBadUpdates.getString( "package", i); 
      iSecondaryEntityNum = tBadUpdates.getInt( "secondary_entity_num", i);
      iDatasetTypeId = tBadUpdates.getInt( "dataset_type_id", i);
      sPageName = tBadUpdates.getString( "rtp_page_subject", i); 
      iScenarioId = tBadUpdates.getInt( "scenario_id", i);

      arg_table.setInt( 1, 1, iServiceID);
      arg_table.setInt( 2, 1, iPrimaryEntityNum); 
      arg_table.setInt( 4, 1, iEntityGroupId); 
      arg_table.setString( 5, 1, sPackageName); 

      retval = DBase.runProc("USER_clear_apm_msg_log", arg_table);
      if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed.  Couldn't delete bad update from MESSAGE LOG !! For Primary Entity (dealnum/delivery ID):" + iPrimaryEntityNum + "\n") );
      else
         OConsole.oprint("Deleted bad update for Secondary Entity Num (Tran Num/delivery ID): " + iSecondaryEntityNum + " Primary Entity Num (Deal num/delivery ID): " + iPrimaryEntityNum + " from message log : " + sServiceName + "\n");

      if(retval != 0)
      {
         // The first argument, the setting name
         arg_table2.setString( 1, 1, sServiceName); 
         arg_table2.setInt( 2, 1, iSecondaryEntityNum); 

         retval = DBase.runProc("USER_apm_del_trannum_from_q", arg_table2);
         if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed.  Couldn't delete bad update from UPDATE QUEUE !! For Primary Entity (deal/delivery ID):" + iPrimaryEntityNum + "\n") );
			
         retval = DBase.runProc("USER_apm_del_nom_from_q", arg_table2);
         if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
            OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed.  Couldn't delete bad update from UPDATE QUEUE !! For Primary Entity (deal/delivery ID):" + iPrimaryEntityNum + "\n") );
			
         OConsole.oprint("Deleted Secondary Entity Num (tran num/delivery ID): " + iSecondaryEntityNum + " Primary Entity Num (Dealnum/Delivery ID): " + iPrimaryEntityNum + " from booking queue : " + sServiceName + "\n");            
			
      }

      // now send message to the clients saying bad update has been cleared
      if(retval != 0)
      {
         SendEntityRerunMsg(iSecondaryEntityNum, iPrimaryEntityNum, iEntityGroupId, iDatasetTypeId, iServiceID, sPackageName, sServiceName, sPageName, iScenarioId);
      }
   }

   arg_table.destroy();
   arg_table2.destroy();
   tBadUpdates.destroy();

   return 1;
}

// ============================================================================
// Start Package name section
// (APM_Generic_PackageName.txt)
// ============================================================================

/*-------------------------------------------------------------------------------
Name:          USER_APM_GetPackageName
Description:   Returns the name of this package
-------------------------------------------------------------------------------*/
int USER_GetPackageName(Table tPackageName) throws OException
{
   tPackageName.setString("package_name",1,"Generic");
   return 1;
}

// ============================================================================
// End Package name section
// (APM_Generic_PackageName.txt)
// ============================================================================

// ============================================================================
// Start Publish Msg section
// ============================================================================

/*-------------------------------------------------------------------------------
Name:          PublishMsgToAPM
Description:   Publishes a message to the clients
-------------------------------------------------------------------------------*/
void PublishMsgToAPM(int iMsgType, int iEntityGroupId, Table tAPMLog) throws OException
{
   // consts - from APM scripts
   String rvMessageHeader = "pfolio_status_changed";
   int APM_PUBLISH_TABLE_AS_XML = 14;
   int APM_PUBLISH_SCOPE_EXTERNAL = 0;

   XString err_xstring;
   Table tRVMsg,tPublishParams;

   err_xstring = Str.xstringNew();
   tRVMsg = Table.tableNew(rvMessageHeader);
   tRVMsg.addCol( rvMessageHeader, COL_TYPE_ENUM.COL_TABLE);
   tRVMsg.addRow();
   tRVMsg.setTable( 1, 1, tAPMLog);

   // get the param table for the publish
   tPublishParams = Table.tableNew("publish_table_as_xml_params");
   if(Apm.performOperation(APM_PUBLISH_TABLE_AS_XML, 1, tPublishParams, err_xstring ) != 0)
   {
      tPublishParams.setString("publish_xml_msg_subject", 1, "TFE." + rvMessageHeader );         
      tRVMsg.setTableName( rvMessageHeader );        

      tPublishParams.setTable( "publish_xml_msg_table", 1, tRVMsg ); 
      tPublishParams.setInt("publish_xml_msg_scope", 1, APM_PUBLISH_SCOPE_EXTERNAL ); // global message
      tPublishParams.setInt("publish_xml_msg_table_level", 1, -1 ); // recurse into all tables
      tPublishParams.setInt( "publish_xml_msg_enable_certifier", 1, 0);         
      tPublishParams.setInt( "publish_xml_msg_type", 1, iMsgType);  
      tPublishParams.setInt( "publish_xml_msg_entity_group", 1, iEntityGroupId );  
      tPublishParams.setInt( "publish_xml_msg_package_id", 1, -1 );
      tPublishParams.setDateTimeByParts( "publish_xml_msg_timestamp", 1, OCalendar.getServerDate(), Util.timeGetServerTime() );  
         
      if(Apm.performOperation(APM_PUBLISH_TABLE_AS_XML, 0, tPublishParams, err_xstring ) == 0)
      {
         OConsole.oprint("Failed to send message to APM clients !!!\n");
      }
         
      tPublishParams.setTable( "publish_xml_msg_table", 1, Util.NULL_TABLE);
   }

   tPublishParams.destroy();
   tRVMsg.setTable( 1, 1, Util.NULL_TABLE);
   tRVMsg.destroy();
   /* Bug in garbage collection means I can't do this next line ! */
   // Str.xstringDestroy (err_xstring);
}

/*-------------------------------------------------------------------------------
Name:          SendEntityRerunMsg
Description:   Sends a Rerun message to the clients
-------------------------------------------------------------------------------*/
void SendEntityRerunMsg(int iSecondaryEntityNum, int iPrimaryEntityNum, int iEntityGroupId, int iDatasetType, int iService, 
                     String sPackage, String sServiceName, String sPageName, int iScenarioId) throws OException
{
   // consts - from APM scripts
   int      cMsgTypeEntityRerun   = 5;
   int      cModeEntityBackoutAndApply = 4;

   String sProcessingMessage;
   int iScriptId, iRow;
   Table tAPMLog, tScenarios;

   tAPMLog = Table.tableNew("apm_msg_log");
   tAPMLog.setTableTitle( "apm_msg_log");
   tAPMLog.addCol( "secondary_entity_num", COL_TYPE_ENUM.COL_INT );
   tAPMLog.addCol( "primary_entity_num", COL_TYPE_ENUM.COL_INT );
   tAPMLog.addCol( "msg_type", COL_TYPE_ENUM.COL_INT );
   tAPMLog.addCol( "msg_mode", COL_TYPE_ENUM.COL_INT );
   tAPMLog.addCol( "msg_text", COL_TYPE_ENUM.COL_STRING );
   tAPMLog.addCol( "timestamp", COL_TYPE_ENUM.COL_DATE_TIME );
   tAPMLog.addCol( "rtp_page_subject", COL_TYPE_ENUM.COL_STRING );
   tAPMLog.addCol( "package", COL_TYPE_ENUM.COL_STRING );
   tAPMLog.addCol( "entity_group_id", COL_TYPE_ENUM.COL_INT );
   tAPMLog.addCol( "scenario_id", COL_TYPE_ENUM.COL_INT );
   tAPMLog.addCol( "dataset_type_id", COL_TYPE_ENUM.COL_INT );
   tAPMLog.addCol( "service_id", COL_TYPE_ENUM.COL_INT );
   tAPMLog.addCol( "scenarios", COL_TYPE_ENUM.COL_TABLE );

   iRow = tAPMLog.addRow();
   tAPMLog.setInt( "secondary_entity_num", iRow, iSecondaryEntityNum);
   tAPMLog.setInt( "primary_entity_num", iRow, iPrimaryEntityNum);
   tAPMLog.setInt( "msg_type", iRow, cMsgTypeEntityRerun);
   tAPMLog.setInt( "msg_mode", iRow, cModeEntityBackoutAndApply);

   tScenarios = Table.tableNew("scenarios");
   tScenarios.addCol( "scenario_id", COL_TYPE_ENUM.COL_INT);
   tScenarios.addCol( "scenario_name", COL_TYPE_ENUM.COL_STRING);
   tScenarios.addRow();
   tScenarios.setInt( "scenario_id", 1, iScenarioId);
   tAPMLog.setTable( "scenarios", iRow, tScenarios);

   sProcessingMessage = "Cleared Error Messages in log for Primary Entity (Deal/Delivery ID): " + iPrimaryEntityNum + " in entity group ID: " + iEntityGroupId;
   tAPMLog.setString( "msg_text", iRow, sProcessingMessage);   

   tAPMLog.setDateTimeByParts( "timestamp", iRow, OCalendar.getServerDate(), Util.timeGetServerTime());
   tAPMLog.setString( "rtp_page_subject", iRow, sPageName);

   tAPMLog.setString( "package", iRow, sPackage);
   tAPMLog.setInt( "entity_group_id", iRow, iEntityGroupId);
   tAPMLog.setInt( "scenario_id", iRow, iScenarioId);
   tAPMLog.setInt( "dataset_type_id", iRow, iDatasetType);
   tAPMLog.setInt( "service_id", iRow, iService);

   OConsole.oprint("Sending Entity Rerun Message to clients.  Secondary Entity Num (Tran Num/Delivery ID): " + iSecondaryEntityNum + " Primary Entity Num (Deal num/delivery ID): " + iPrimaryEntityNum + " from service : " + sServiceName + "\n");            
   PublishMsgToAPM(cMsgTypeEntityRerun, iEntityGroupId, tAPMLog);
   tAPMLog.destroy();

}

// ============================================================================
// End Publish Msg section
// ============================================================================



}
