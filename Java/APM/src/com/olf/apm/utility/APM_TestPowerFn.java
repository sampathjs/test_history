package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_TestPowerFn implements IScript {
   //#SDBG

/* Released with version 29-Aug-2019_V17_0_124 of APM */

/* script to ask params and pass into power fn */
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   runPowerfn(argt);
}

void runPowerfn(Table editTable) throws OException
{
   Table timeFrames;
   Table lShapedData;
   Table queryCriteria;
   Table tempTable;

   ODateTime startDateTime;
   ODateTime endDateTime;

   int queryId;
   int timeZone; 
   int incrementType = -1; 
   int dateSequence = -1; 
   int valuationProduct; 
   int dateFormat; 
   int dateLocale; 
   int timeFormat;
   int i;

   // Create a query to pull back the required transaction...
   
   queryId = editTable.getInt( "query", 1);

   // Create the start and end times

   startDateTime = editTable.getDateTime( "Start_Date", 1);
   endDateTime = editTable.getDateTime( "End_Date", 1);
   startDateTime.setTime(0);
   endDateTime.setTime(86400);
           
   // Create a table to hold the timeframes   
   timeFrames = Table.tableNew( "timeFrames" );

   // The time zone
   if ( editTable.getInt( "TimeZone", 1) > 1000 )
      timeZone = editTable.getInt( "TimeZone", 1);
   else
      timeZone = 1000; /* GMT */                 

   // The increment type and date sequence, 
   if ( editTable.getInt( "Granularity", 1) == 1 ) // hourly
   {
      incrementType = PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_60_MINUTE.toInt();
      dateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt(); 
   }
   else if ( editTable.getInt( "Granularity", 1) == 2 ) // daily
   {
      incrementType = -1;
      dateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt(); 
   }
   else if ( editTable.getInt( "Granularity", 1) == 3 ) // monthly
   {
      incrementType = -1;

      if (editTable.getInt( "Date_Sequence", 1) > 0)
      {
         // PWR_Generate... function will subtract 1000 from the custom date sequence ID passed in
         dateSequence = editTable.getInt( "Date_Sequence", 1) + 1000;
      }
      else
      {
         dateSequence = 2;
      }
   }

   // The valuation product, use the deals valuation product
   valuationProduct = -1;   

   // The extra query criteria

   queryCriteria = Table.tableNew("query_criteria");
   queryCriteria.addCol( "query_type", COL_TYPE_ENUM.COL_INT );
   queryCriteria.addCol( "query_what_str", COL_TYPE_ENUM.COL_STRING );
   queryCriteria.addCol( "query_from_str",  COL_TYPE_ENUM.COL_STRING );
   queryCriteria.addCol( "query_where_str", COL_TYPE_ENUM.COL_STRING );
   
   queryCriteria.addRow();
   queryCriteria.setInt( 1, 1, 0 );
   queryCriteria.setString( 2, 1, ",(param_reset_header.proj_index) startdate, (param_reset_header.proj_index) enddate, (param_reset_header.proj_index) start_time, (param_reset_header.proj_index) end_time, (param_reset_header.rate_multiplier) cost, (param_reset_header.rate_multiplier) bav_volume, (param_reset_header.rate_multiplier) bav_cost, (ptad.time_zone) power_timezone, (ptad.phys_tran_type) power_initial_term, (ptad.service_type) power_service_type, (ptad.choice) power_choice, (ptad.product) power_product, (ptad.pt_of_receipt_loc) power_rec_loc, (ptad.pt_of_delivery_loc) power_del_loc" );

   queryCriteria.addRow();
   queryCriteria.setInt( 1, 2, 1 );

   if ( editTable.getInt( "Show_All_Volume_Statuses", 1) == 0 ) // BAV only
      queryCriteria.setString( 4, 2, "and tsd.status not in ( 13, 14) and tsd.bav_flag = 1" );
   else
      queryCriteria.setString( 4, 2, "and tsd.status not in ( 13, 14)" );

   // The date format

   dateFormat = DATE_FORMAT.DATE_FORMAT_DEFAULT.toInt(); // Util.NOT_FOUND
   dateLocale = 0; // DATE_LOCALE.DATE_LOCALE_US
   timeFormat = 3; // TIME_FORMAT.TIME_FORMAT_HM24

   // Generate volumes

    lShapedData = Power.generateVolumetricPositions (
                      queryId, 
                      startDateTime, 
                      endDateTime, 
                      timeZone, 
                      incrementType, 
                      dateSequence, 
                      valuationProduct, 
                      0, 
                      queryCriteria, 
                      dateFormat, 
                      dateLocale, 
                      timeFormat, 
                      1, 
                      timeFrames);
                      
   // Dump the data

   lShapedData.viewTableForDebugging();
   timeFrames.viewTableForDebugging();

   /*
   for ( i = 1; i <= lShapedData.getNumRows(); i++ )
   {
      tempTable = lShapedData.createTableFromArray( lShapedData.getColNum( "loc_starting_period"), i );
   
      if ( Table.isTableValid( tempTable ) == 1 )
      {
         tempTable.setTableName( "From " + i );
         tempTable.viewTableForDebugging();
         tempTable.destroy();
      }

      tempTable = lShapedData.createTableFromArray( lShapedData.getColNum( "loc_stopping_period"), i );
   
      if ( Table.isTableValid( tempTable ) == 1 )
      {
         tempTable.setTableName( "To " + i );
         tempTable.viewTableForDebugging();
         tempTable.destroy();
      }
   }
   */

   // Clean up.

   lShapedData.destroy();
   timeFrames.destroy();
   queryCriteria.destroy();
}


}
