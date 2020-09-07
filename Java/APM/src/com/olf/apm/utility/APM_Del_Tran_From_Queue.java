package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Del_Tran_From_Queue implements IScript {
   // THIS SCRIPT deletes a tran num from the deal booking queue
// service/post proc name must be the correct one for the tran_num !!!!!
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   String sServiceName = ""; // SET THIS HERE IF YOU WANT TO OVERRIDE THE DEFAULT NAME
   int tran_num = 123456; /* EDIT THIS VALUE TO MATCH THE TRAN NUM TO DELETE */

   Table arg_table;
   Table target_table;
   Table tPackageName;
   String sPackageName;
   int retval;
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

   arg_table= Table.tableNew();
   arg_table.addCol( "service_name", COL_TYPE_ENUM.COL_STRING);
   arg_table.addCol( "tran_num", COL_TYPE_ENUM.COL_INT);
   arg_table.addRow();

   // The first argument, the setting name
   arg_table.setString( 1, 1, sServiceName); 
   arg_table.setInt( 2, 1, tran_num); 

   retval = DBase.runProc("USER_apm_del_trannum_from_q", arg_table);
   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed" ) );
   else
      OConsole.oprint("Deleted tran_num " + tran_num + " from deal booking queue : " + sServiceName + "\n");
   arg_table.destroy();
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
