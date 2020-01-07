package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Drilldown_Task implements IScript {
   //#SDBG
/* Released with version 29-Aug-2019_V17_0_124 of APM */

/* EXAMPLE SCRIPT FOR HOW TO DO A CUSTOM DRILLDOWN FOR APM for Non-ADS setup */
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   int i;
   String pageName;
   String userName;
   String columnName;
   String rowName;
   String callType;
   Table drilldownData;
   Table listOfSwitchableColumns;
   Table metadata;

   // extract params from the argt
   pageName = argt.getString( "page_name", 1);
   userName = argt.getString( "user_name", 1);
   columnName = argt.getString( "column_name", 1);
   rowName = argt.getString( "row_name", 1);

   // Output lines to console indicating the script has been called
   OConsole.oprint("Called custom drilldown script: APM Drilldown Task\n");
   OConsole.oprint("UserName: " + userName + " Page: " + pageName + " Column: " + columnName + " Row: " + rowName + "\n");

   // Output call type
   callType = argt.getString( "call_type", 1);
   OConsole.oprint("Custom enrichment script call type: " + callType + ".\n");

   // retrieve the metadata information:
   metadata = argt.getTable( "metadata", 1);

   if(Str.equal(callType, "reduction") != 0)
   {
      int row, numRows;

      listOfSwitchableColumns = argt.getTable( "column_mapper", 1);

      // Enable all of the columns.
      numRows = listOfSwitchableColumns.getNumRows();
      for (row = 1; row <= numRows; row++)
      {
         listOfSwitchableColumns.setInt( "IsEnabled", row, 1);
      }

      listOfSwitchableColumns.copyTableToTable( returnt);
   }
   else
   {
      // This is ENRICHMENT call, get data for enrichment

      // default behaviour - just copy the argt as is
      argt.copyTableToTable( returnt);
   }

   //returnt.viewTableForDebugging();

   Util.exitSucceed();
}



}
