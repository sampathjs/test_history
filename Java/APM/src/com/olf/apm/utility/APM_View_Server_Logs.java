package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_View_Server_Logs implements IScript {
   //#SDBG

/* Released with version 05-Feb-2020_V17_0_126 of APM */

// 
// Load the server logs for a given set of parameters, dump the saved logs
// to the console and view the table containing the parameters for the given 
// logs.
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   Table logs;
   String text;
   int serviceId = 79956;
   String packageName = "unknown";
   int entityGroupId = -1;
   int datasetTypeId = 0;
   int scenarioId = -1;
   String where = "";
   int secondaryEntityNum = -1;
   int processId = 5384;
   int ok;
   int attempt;
   int row;

   ok = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
   logs = Table.tableNew("params");

   where = "service_id=" + serviceId
       + " AND package=\'" + packageName + "\'"
       + " AND entity_group_id=" + entityGroupId
       + " AND dataset_type_id=" + datasetTypeId
       + " AND scenario_id=" + scenarioId
       + " AND secondary_entity_num=" + secondaryEntityNum 
       + " AND process_id=" + processId;
   
   for( attempt = 0; (attempt == 0 ) || ((ok == DB_RETURN_CODE.SYB_RETURN_DB_RETRYABLE_ERROR.toInt()) && (attempt < 10)); ++attempt )
   {
      if( attempt > 0 )
         Debug.sleep( attempt * 1000 );

      ok = DBaseTable.loadFromDbWithSQL( logs, "*", "apm_logs", where);
   }

   if(Table.isTableValid( logs ) != 0)
   {
      OConsole.oprint("\n\n\n======\n");
      OConsole.oprint("APM_View_Server_Logs printing logs from apm_logs\n");
      
      for ( row = 1; row <= logs.getNumRows() ; ++row )
      {
         OConsole.oprint( "[Id=" + logs.getInt( "log_id", row ) + "]" );
         OConsole.oprint( "[ServiceId=" + serviceId + "]" );
         OConsole.oprint( "[PackageName=" + packageName + "]" );
         OConsole.oprint( "[EntityGroupId=" + entityGroupId + "]" );
         OConsole.oprint( "[DatasetTypeId=" + datasetTypeId + "]" );
         OConsole.oprint( "[ScenarioId=" + scenarioId + "]" );
         OConsole.oprint( "[SecondaryEntityNum=" + secondaryEntityNum + "]" );
         OConsole.oprint( "[ProcessId=" + processId + "]" );
         OConsole.oprint( "\n" );         

         logs.uncompressColByteArray( logs.getColNum( "blob_data"));
         text = new String(logs.getByteArray(logs.getColNum( "blob_data"), 1));
         OConsole.oprint( text );
         OConsole.oprint( "\n\n" );
      }

      logs.viewTableForDebugging();
      logs.destroy();
   }
}


}
