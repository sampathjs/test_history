package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Run_ADS_Batch_Param implements IScript {
   /* Released with version 24-Feb-2017_V17_0_6 of APM */

// PARAM script to be used with APM_Run_ADS_Batch script
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   String sSERVICENAME = "APM_1";

   // Build argt
   argt.addCol( "ADSservice_name", COL_TYPE_ENUM.COL_STRING);
   argt.addRow();

   argt.setString( "ADSservice_name", 1, sSERVICENAME);


}


}
