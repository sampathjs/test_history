package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Turn_Off_Oasis implements IScript {
   //#SDBG

// THIS SCRIPT SWITCHES ON ALL PIVOTS FOR APM OASIS POWER

/* Released with version 05-Feb-2020_V17_0_126 of APM */
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   Table arg_table;
   int retval;

   arg_table = Table.tableNew("empty");
   retval = DBase.runProc("USER_apm_turn_oasis_off", arg_table);
   if( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      OConsole.oprint (DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runProc() failed. Cannot switch off OASIS Power pivots !!!\n") );
   else
      OConsole.oprint("Successfully switched OFF all OASIS Power Pivots!!!\n");

   arg_table.destroy();

}


}
