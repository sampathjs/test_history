package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Purge_Deal_Queue implements IScript {
   // THIS SCRIPT PURGES THE CURRENT INCREMENTAL BOOKING QUEUE FOR APM OF EVERYTHING UP HALF AN HOUR AGO
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   String sServiceName = ""; // SET THIS HERE IF YOU WANT TO OVERRIDE THE DEFAULT NAME
   Table arg_table;
   Table target_table;
   Table tPackageName;
   String sPackageName;
   int retval;
   ODateTime start_date = ODateTime.dtNew();
   int today, time;
   Table tVersion;
   int major_version;

   tVersion = Ref.getVersion();
   major_version = tVersion.getInt( "major_version", 1);
   tVersion.destroy();

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

   start_date = ODateTime.strToDateTime("01/01/01 1:01:01pm");

   today = OCalendar.getServerDate();
   time = Util.timeGetServerTime();
   time = time - 1800; /* take off half an hour */
   if (time <= 0) time = 1;

   arg_table= Table.tableNew();
   arg_table.addCol( "service_name", COL_TYPE_ENUM.COL_STRING);
   arg_table.addCol( "start_date", COL_TYPE_ENUM.COL_DATE_TIME);
   arg_table.addCol( "end_date", COL_TYPE_ENUM.COL_DATE_TIME);
   arg_table.addRow();

   // The first argument, the setting name
   arg_table.setString( 1, 1, sServiceName); 
   arg_table.setDateTime( 2, 1, start_date); 
   arg_table.setDateTimeByParts( 3, 1, today, time); 

   retval = DBase.runProc("USER_apm_prune_entity_queue", arg_table);
   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed" ) );
   else
      OConsole.oprint ("APM_Purge Current Incremental Queue Succeeded" );

   arg_table.destroy();
   start_date.destroy();
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



}
