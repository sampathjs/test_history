package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Turn_On_Deal_Pivots implements IScript {
   //#SDBG

// THIS SCRIPT SWITCHES ON DEAL LEVEL PIVOTS FOR APM

/* Released with version 29-Aug-2019_V17_0_124 of APM */
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   String sPivotNameList = "'Deal Number', 'Transaction Number'"; // comma separated list

   String sWhat = "package_name, folder_name, filter_name, label, is_splitter, is_filter";
   String sFrom = "tfe_pkg_folder_filters";
   String sWhere = "filter_name in (" + sPivotNameList + ")";
   Table pivot_table;
   int retval, i;

   // set up arg table
   pivot_table= Table.tableNew();
   retval = DBaseTable.loadFromDbWithSQL(pivot_table, sWhat, sFrom, sWhere);
   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
   {
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBaseTable.loadFromDbWithSQL() failed. Cannot continue !!!\n") );
      Util.exitFail();
   }

   // set the params
   pivot_table.setColValInt( "is_splitter", 1); // set the pivots to ON

   for ( i = 1; i <= pivot_table.getNumRows(); i++)
      updatePivot(pivot_table, i);

   pivot_table.destroy();
}

int updatePivot(Table pivot_table, int row) throws OException
{
   int retval;

   // set the params
   Table arg_table = pivot_table.cloneTable();
   arg_table.addRow();

   arg_table.setString( 1, 1, pivot_table.getString( "package_name", row) ); 
   arg_table.setString( 2, 1, pivot_table.getString( "folder_name", row)); 
   arg_table.setString( 3, 1, pivot_table.getString( "filter_name", row)); 
   arg_table.setString( 4, 1, pivot_table.getString( "label", row)); 
   arg_table.setInt( 5, 1, pivot_table.getInt( "is_splitter", row));
   arg_table.setInt( 6, 1, pivot_table.getInt( "is_filter", row));

   // execute
   retval = DBase.runProc("USER_apm_update_filter", arg_table);
   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runProc() failed. Cannot switch on pivot: " + pivot_table.getString( "filter_name", row) + " !!!\n") );

   arg_table.destroy();

   OConsole.oprint("Successfully switched ON the Pivot: " + pivot_table.getString( "filter_name", row) + " !!!\n");

   return retval;
}


}
