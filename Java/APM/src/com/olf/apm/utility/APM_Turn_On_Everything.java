package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Turn_On_Everything implements IScript {
   //#SDBG

// THIS SCRIPT SWITCHES ON ALL PACKAGES/PIVOTS/COLUMNS FOR APM

/* Released with version 05-Feb-2020_V17_0_126 of APM */
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   Table arg_table;
   int retval;

   arg_table = Table.tableNew("empty");
   retval = DBase.runProc("USER_apm_turn_everything_on", arg_table);
   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runProc() failed. Cannot switch on all packages/pivots/columns!!!\n") );
   else
      OConsole.oprint("Successfully switched ON all packages/Columns/Pivots!!!\n");

   arg_table.destroy();

}


}
