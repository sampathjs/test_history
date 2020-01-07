package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Drilldown_Enrichment implements IScript {
   //#SDBG

/* EXAMPLE SCRIPT FOR HOW TO DO A CUSTOM DRILLDOWN FOR APM 2.9.1 with ADS ONWARDS */
/* This script won't work with SQLite */
/* 02-Jun-2010 */
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

   // determine what type of call is this one
   int callTypeColumnIndex = argt.getColNum( "call_type");
   Table drilldownData = argt.getTable( "drilldown_data", 1);
   Table listOfColumns = argt.getTable( "list_of_columns", 1);


   OConsole.oprint("TRYING TO EXECUTE APM_DRILLDOWN_ENRICHMENT_SCRIPT:\n");

   argt.viewTableForDebugging();

   callType = argt.getString( "call_type", 1);
   OConsole.oprint("Call type: " + callType + ".\n");


   if (callTypeColumnIndex < 0)
   {
      // Can't determine if this is reduction and enrichment call type.
      // Complain and exit

      OConsole.oprint("Column " + "call_type" + " not present in the argument data.\n");
      OConsole.oprint("Terminating custom drilldown script.\n");
      Util.exitFail();

   }


   if(Str.equal(callType, "reduction") != 0)
   {

      // ----------------------------------------------------------------------------
      // Perform reduction now!
      //drilldownData.viewTableForDebugging();

      returnt.select( listOfColumns, "*", "is_enabled GE 0");

      for (i = returnt.getNumRows(); i >= 10; i--)
      {
         returnt.setInt( "is_enabled", i, 0);
      }


      //while(returnt.getNumRows() > 2)
      //{
      //   returnt.delRow( returnt.getNumRows());
      //}

      Util.exitSucceed();


   }
   else
   {

   // default behaviour - just copy the drilldown as is
   returnt.select( drilldownData, "*", "dealnum GT 0");

   Util.exitSucceed();






   // extract params from the argt

   pageName = argt.getString( "page_name", 1);
   userName = argt.getString( "user_name", 1);
   columnName = argt.getString( "column_name", 1);
   rowName = argt.getString( "row_name", 1);
  
   // Output lines to console indicating the script has been called
   OConsole.oprint("Called custom drilldown script: APM Exposure Change Drilldown\n");
   OConsole.oprint("UserName: " + userName + " Page: " + pageName + " Column: " + columnName + " Row: " + rowName + "\n");

   // this is the only page we want to manipulate
   if(Str.equal(pageName, "Exposure Change") != 0)
   {
      if(Str.equal(columnName, "Change On Day") != 0)
      {
         if ( formatDrillDownForExposureChange(drilldownData, returnt) == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
            Util.exitSucceed();
         else
            Util.exitFail();
      }
      else if(Str.equal(columnName, "Current") != 0)
      {
         if ( formatDrillDownForCurrentExposure(drilldownData, returnt) == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
            Util.exitSucceed();
         else
            Util.exitFail();
      }
      else if(Str.equal(columnName, "Start Of Day") != 0)
      {
         if ( formatDrillDownForStartOfDayExposure(drilldownData, returnt) == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() )
            Util.exitSucceed();
         else
            Util.exitFail();
      }
   }
   else
   {
      argt.setString( "error_string", 1, "Drilldown script called for unrecognised page !!!");
      Util.exitFail();
   }

   // default behaviour - just copy the drilldown as is
   returnt.select( drilldownData, "*", "dealnum GT 0");

   Util.exitSucceed();

}

}

/* THIS FN ROLLS UP THE DATA TO THE LEG LEVEL */
void rollupDrilldownExposureData(Table drilldownData) throws OException
{
   int i;

   //drilldownData.viewTableForDebugging();

   // first roll the data up in the drilldowndata to make sure we have no duplicates
   drilldownData.group( "dealnum, leg, scenario_id, projindex, disc_index, trangptid, dataset_type");
   for (i = drilldownData.getNumRows(); i > 1; i--)
   {
      if ( drilldownData.getInt( "dealnum", i) == drilldownData.getInt( "dealnum", i-1) &&
           drilldownData.getInt( "leg", i) == drilldownData.getInt( "leg", i-1) &&
           drilldownData.getInt( "scenario_id", i) == drilldownData.getInt( "scenario_id", i-1) &&
           drilldownData.getInt( "projindex", i) == drilldownData.getInt( "projindex", i-1) &&
           drilldownData.getInt( "disc_index", i) == drilldownData.getInt( "disc_index", i-1) &&
           drilldownData.getInt( "dataset_type", i) == drilldownData.getInt( "dataset_type", i-1) &&
           drilldownData.getInt( "trangptid", i) == drilldownData.getInt( "trangptid", i-1) )
      {
         drilldownData.setDouble( "fv_delta", i-1, drilldownData.getDouble( "fv_delta", i) + drilldownData.getDouble( "fv_delta", i-1));
         drilldownData.delRow( i);
      }
   }

//   drilldownData.viewTableForDebugging();

}

/* THIS FN CUSTOMISES THE DRILLDOWN FOR THE FMLA COLUMN */
int formatDrillDownForExposureChange(Table drilldownData, Table returnt) throws OException
{
   String dataCols, whereStr;   
   Table   tTempTable;

   tTempTable = Table.tableNew();

   // get the right schema into the returnt
   dataCols = "dealnum,portfolio,leg, scenario_id, projindex, disc_index, trangptid, Current_Exposure, Start_Exposure, Start_Exposure (Change)";   
   tTempTable.select( drilldownData, dataCols, "dealnum EQ 0");

   // now copy the list of unique keys into the table
   dataCols = "DISTINCT, dealnum,portfolio,leg, scenario_id, projindex, disc_index, trangptid";
   whereStr = "dealnum GT 0";
   tTempTable.select( drilldownData, dataCols, whereStr);   

   // now copy the current numbers across
   dataCols = "dealnum,portfolio,leg, scenario_id, projindex, disc_index, trangptid, Current_Exposure";
   whereStr = "dealnum EQ $dealnum AND portfolio EQ $portfolio AND leg EQ $leg AND scenario_id EQ $scenario_id AND projindex EQ $projindex AND disc_index EQ $disc_index AND trangptid EQ $trangptid AND dataset_type EQ 0";
   tTempTable.select( drilldownData, dataCols, whereStr);   

   // now the start of day numbers
   dataCols = "Start_Exposure";
   whereStr = "dealnum EQ $dealnum AND portfolio EQ $portfolio AND leg EQ $leg AND scenario_id EQ $scenario_id AND projindex EQ $projindex AND disc_index EQ $disc_index AND trangptid EQ $trangptid AND dataset_type EQ 1";
   tTempTable.select( drilldownData, dataCols, whereStr);

   // now compute the difference and populate change column
   tTempTable.mathSubCol( "Current_Exposure", "Start_Exposure", "Change");

   returnt.select( tTempTable, "*", "dealnum GT 0");   
   tTempTable.destroy();

   return OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
}

/* THIS FN CUSTOMISES THE DRILLDOWN FOR THE CURRENT COLUMN */
int formatDrillDownForCurrentExposure(Table drilldownData, Table returnt) throws OException
{
   String dataCols, whereStr;
   Table   tTempTable;
   Table   tFormatedTable;

   tTempTable = Table.tableNew();
   tFormatedTable = Table.tableNew();

   // rollup the drilldown exposure data
   rollupDrilldownExposureData(drilldownData);

   // get the right numbers into the returnt
   dataCols = "dealnum,portfolio,leg, scenario_id, projindex, disc_index, trangptid, fv_delta (Current_Exposure), dataset_type";
   whereStr = "dataset_type EQ 0 AND dealnum GT 0";
   tTempTable.select( drilldownData, dataCols, whereStr);

   // apply formatting
   applyFormatToTable(tTempTable);

   // convert table as per applied formatting
   tFormatedTable = convertTableToFormattedVersion(tTempTable);
   returnt.select( tFormatedTable, "*", "dealnum GT 0");

   // clean up
   tTempTable.destroy();
   tFormatedTable.destroy();

   return OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
}

/* THIS FN CUSTOMISES THE DRILLDOWN FOR THE START OF DAY COLUMN */
int formatDrillDownForStartOfDayExposure(Table drilldownData, Table returnt) throws OException
{
   String dataCols, whereStr;
   Table   tTempTable;
   Table   tFormatedTable;

   tTempTable = Table.tableNew();
   tFormatedTable = Table.tableNew();

   // rollup the drilldown exposure data
   rollupDrilldownExposureData(drilldownData);

   // get the right numbers into the returnt
   dataCols = "dealnum,portfolio,leg, scenario_id, projindex, disc_index, trangptid, fv_delta (Start_Exposure), dataset_type";
   whereStr = "dataset_type NE 0 AND dealnum GT 0";
   tTempTable.select( drilldownData, dataCols, whereStr);

   // apply formatting
   applyFormatToTable(tTempTable);

   // convert table as per applied formatting
   tFormatedTable = convertTableToFormattedVersion(tTempTable);
   returnt.select( tFormatedTable, "*", "dealnum GT 0");

   // clean up
   tTempTable.destroy();
   tFormatedTable.destroy();

   return OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();
}


Table convertTableToFormattedVersion(Table tToConvert) throws OException
{
   Table outputTable;
   int col, iNumCols; 

   // go through the input table and make sure the widths are set ok
   iNumCols = tToConvert.getNumCols();
   for (col = 1; col <= iNumCols; col++)
   {
      if (tToConvert.getColFormat( col) == COL_FORMAT_TYPE_ENUM.FMT_REF.toInt() && tToConvert.formatGetWidth( col) < 64 )
      {
         tToConvert.formatSetWidth( col, 64);
      }
   }

   // convert retaining table types for those other than dates and strings
   outputTable = tToConvert.copyTableFormatted( 0);

   // set the table name
   if ( tToConvert.getTableTitle() != null && Str.len(tToConvert.getTableTitle()) > 0 )
      outputTable.setTableName( tToConvert.getTableTitle());
   else
      outputTable.setTableName( tToConvert.getTableName());

   // set the column titles
   for (col = 1; col <= iNumCols; col++)
   {
      if ( tToConvert.getColTitle( col) != null && Str.len(tToConvert.getColTitle( col)) > 0 )
         outputTable.setColName( col, tToConvert.getColTitle( col));
   }   

   return outputTable;
}

/* THIS FN APPLIES FORMATTING TO COLUMNS */
void applyFormatToTable(Table tTable) throws OException
{
   tTable.setColFormatAsRef( "projindex",    SHM_USR_TABLES_ENUM.INDEX_TABLE);
   tTable.setColFormatAsRef( "disc_index",   SHM_USR_TABLES_ENUM.INDEX_TABLE);
   tTable.setColFormatAsRef( "portfolio",    SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
}

}
