/* Released with version 14-Jul-2014_V99_1_5 of APM */

/*
File Name:                      APMUtility.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Joe Gallagher
Creation Date:                  March 14, 2014
 
Revision History:
												
Script Type:                    User-defined simulation result

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    APM Utility functions

*/

package com.olf.result.APMUtility;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import java.util.Vector;
import java.lang.Math;

public class APMUtility 
{
	
   public static int GetParamDateValue(Table tResConfig, String sParamName) throws OException
   {
      int iRow, iDate = -1;
      if (tResConfig == null)
    	  return iDate;
	      
      iRow = tResConfig.findString("res_attr_name", sParamName, SEARCH_ENUM.FIRST_IN_GROUP);   

      if(iRow > 0)
    	  iDate = OCalendar.parseString(tResConfig.getString("value", iRow));

      return iDate;
   }

   public static int GetParamIntValue(Table tResConfig, String sParamName) throws OException
   {
      int iRow, iVal = -1;
      if (tResConfig == null)
    	  return iVal;

      iRow = tResConfig.findString("res_attr_name", sParamName, SEARCH_ENUM.FIRST_IN_GROUP);   
      
      if(iRow > 0)
    	  iVal = Str.strToInt(tResConfig.getString("value", iRow));

      return iVal;   
   }

   public static String GetParamStrValue(Table tResConfig, String sParamName) throws OException
   {
      int iRow;
      String sVal="";
      if (tResConfig == null)
    	  return sVal;

      iRow = tResConfig.findString("res_attr_name", sParamName, SEARCH_ENUM.FIRST_IN_GROUP);   
      
      if(iRow > 0)
    	  sVal = tResConfig.getString("value", iRow);

      return sVal;   
   }

   public static int ParamHasValue(Table tResConfig, String sParamName)  throws OException
   {
      int iRow;
      String sValue = "";

      if (tResConfig == null)
    	  return 0;

      iRow = tResConfig.findString("res_attr_name", sParamName, SEARCH_ENUM.FIRST_IN_GROUP);

      if(iRow > 0)
    	  sValue = tResConfig.getString("value", iRow);

      if ( Str.len(sValue) > 0 )
      {
	 return 1;
      }

      return 0;
   }

   // Generate a query for all transactions of applicable instrument types
   // Blank list is considered to be "all ins types are acceptable"
   public static int generateDealQuery(Table argt, Vector<Integer> baseInsTypes) throws OException 
   {
      int myBaseInsType;
      int iQueryID = 0;

      int iResult = argt.getInt("result_type", 1);
      Table tblTrans = argt.getTable("transactions", 1);
      Table validTrans = tblTrans.cloneTable();

      for (int i = 1; i <= tblTrans.getNumRows(); i++) 
      {
         Transaction tranCurrent = tblTrans.getTran("tran_ptr", i);

	 if (SimResult.isResultAllowedForTran(iResult, tranCurrent) > 0) 
	 {
	    myBaseInsType = Instrument.getBaseInsType(tranCurrent.getInsType());
	    if ((baseInsTypes.size() == 0) || (baseInsTypes.contains(myBaseInsType))) 
	    {
	       tblTrans.copyRowAdd(i, validTrans);
	    }
	 }
      }
      
      if (validTrans.getNumRows() > 0) 
      {
	 iQueryID = Query.tableQueryInsert(validTrans, "tran_num");
      }

      return iQueryID;
   }
   
   // Generate a query for all transactions of applicable instrument types
   // Blank list is considered to be "all ins types are acceptable"
   public static int generateNomQuery(Table argt, Vector<Integer> baseInsTypes) throws OException 
   {
      int iQueryID = 0;
      Table simDef = argt.getTable("sim_def", 1);

      // If query ID is provided as a parameter, use it! (comes from APM nom service)
      if ( simDef.getColNum("APM Nomination Query") > 0)
         iQueryID = simDef.getInt("APM Nomination Query", 1);

      return iQueryID;
   }

   public static Table generateVolTypesTable() throws OException
   {
      Table transport_vol_types = Table.tableNew("Transport Volume Types");
      transport_vol_types.addCol( "volume_type", COL_TYPE_ENUM.COL_INT);
      transport_vol_types.addNumRows( 5);
      transport_vol_types.setInt( 1, 1, VOLUME_TYPE.VOLUME_TYPE_NOMINATED.toInt());
      transport_vol_types.setInt( 1, 2, VOLUME_TYPE.VOLUME_TYPE_SCHEDULED.toInt());
      transport_vol_types.setInt( 1, 3, VOLUME_TYPE.VOLUME_TYPE_ALLOCATED.toInt());
      transport_vol_types.setInt( 1, 4, VOLUME_TYPE.VOLUME_TYPE_ACTUAL.toInt());
      transport_vol_types.setInt( 1, 5, VOLUME_TYPE.VOLUME_TYPE_CONFIRMED.toInt());
      return transport_vol_types;
   }
		
   public static Table generateStorageVolTypesTable() throws OException
   {
      Table storage_vol_types = Table.tableNew("Storage Volume Types");
      storage_vol_types.addCol( "volume_type", COL_TYPE_ENUM.COL_INT);
      storage_vol_types.addNumRows( 7);
      storage_vol_types.setInt( 1, 1, VOLUME_TYPE.VOLUME_TYPE_UNSCHEDULED.toInt());
      storage_vol_types.setInt( 1, 2, VOLUME_TYPE.VOLUME_TYPE_NOMINATED.toInt());
      storage_vol_types.setInt( 1, 3, VOLUME_TYPE.VOLUME_TYPE_SCHEDULED.toInt());
      storage_vol_types.setInt( 1, 4, VOLUME_TYPE.VOLUME_TYPE_ALLOCATED.toInt());
      storage_vol_types.setInt( 1, 5, VOLUME_TYPE.VOLUME_TYPE_ACTUAL.toInt());
      storage_vol_types.setInt( 1, 6, VOLUME_TYPE.VOLUME_TYPE_CONFIRMED.toInt());
      storage_vol_types.setInt( 1, 7, VOLUME_TYPE.VOLUME_TYPE_FORECASTED.toInt());
      return storage_vol_types;
   }

   public static Table splitIntoDays(Table tblData) throws OException 
   {

      Table splitTable, rowTable, output;
      Integer iRow;

      rowTable = tblData.cloneTable();
      splitTable = tblData.cloneTable();

      for (iRow = 1; iRow <= tblData.getNumRows(); iRow++) 
      {
	 tblData.copyRowAdd(iRow, rowTable);
	 output = splitRowsDaily(rowTable);// This method will split the rows
										 // into a daily rows.
	 output.copyRowAddAll(splitTable);
	 output.destroy();
	 rowTable.clearDataRows();
      }

      rowTable.destroy();

      int iSDTCol = splitTable.getColNum("gmt_start_date_time");
      int iEDTCol = splitTable.getColNum("gmt_end_date_time");
      int iSDCol = splitTable.getColNum("startdate");
      int iEDCol = splitTable.getColNum("enddate");
      int iSTCol = splitTable.getColNum("start_time");
      int iETCol = splitTable.getColNum("end_time");

      for (iRow = 1; iRow <= splitTable.getNumRows(); iRow++) 
      {
	 // Fix up the dates by converting to a date+time combo
	 int iStartDate = splitTable.getDate(iSDTCol, iRow);
	 int iStartTime = splitTable.getTime(iSDTCol, iRow);

	 splitTable.setInt(iSDCol, iRow, iStartDate);
	 splitTable.setInt(iSTCol, iRow, iStartTime);

	 // For end time, we want to change 32767:0 to 32766:86400 for better
	 // bucketing on client

	 int iEndDate = splitTable.getDate(iEDTCol, iRow);
	 int iEndTime = splitTable.getTime(iEDTCol, iRow);

	 if (iEndTime == 0) 
	 {
	    iEndDate--;
	    iEndTime = 86400;
	 }

	 splitTable.setInt(iEDCol, iRow, iEndDate);
	 splitTable.setInt(iETCol, iRow, iEndTime);
      }

      return splitTable;
   }

   public static Table splitRowsDaily(Table rowTable) throws OException 
   {
      Table output = rowTable.cloneTable();

      int timezone = rowTable.getInt("time_zone", 1);

      // Get the start and end date and convert from GMT to CED
      ODateTime gmtSDT, gmtEDT, localSDT = ODateTime.dtNew(), localEDT = ODateTime
			.dtNew();

      gmtSDT = rowTable.getDateTime("gmt_start_date_time", 1);
      gmtSDT.convertFromGMT(localSDT, timezone);

      gmtEDT = rowTable.getDateTime("gmt_end_date_time", 1);
      gmtEDT.convertFromGMT(localEDT, timezone);

      // Get number of seconds, then get number of days, rounding upwards
      Integer numSeconds = gmtSDT.computeTotalSecondsInGMTDateRange(gmtEDT);
      Integer numDays = (int) Math.ceil((double) localSDT
			.computeTotalSecondsInGMTDateRange(localEDT) / 86400.0);

      // Pre-create the requisite number of rows for efficiency
      output.addNumRows(numDays);

      ODateTime currentLocalSDT = ODateTime.dtNew();
      ODateTime currentgmtSDT = ODateTime.dtNew();
      ODateTime currentLocalEDT = ODateTime.dtNew();
      ODateTime currentgmtEDT = ODateTime.dtNew();

      for (int testCounter = 0; testCounter < numDays; testCounter++) 
      {
	 currentLocalSDT.setDateTime(localSDT.getDate() + testCounter,localSDT.getTime());
	 currentLocalEDT.setDateTime(localSDT.getDate() + testCounter + 1,localSDT.getTime());
	 if (currentLocalEDT.getDate() > localEDT.getDate()) 
	 {
	    currentLocalEDT.setDateTime(localEDT.getDate(),localEDT.getTime());
	 }
	 else if ((currentLocalEDT.getDate() == localEDT.getDate())
		  && (currentLocalEDT.getTime() > localEDT.getTime())) 
	 {
	    currentLocalEDT.setDateTime(localEDT.getDate(),localEDT.getTime());
	 }

	 currentLocalSDT.convertToGMT(currentgmtSDT, timezone);
	 currentLocalEDT.convertToGMT(currentgmtEDT, timezone);
	 // Use counter+1, as table offsets are 1-based
	 rowTable.copyRow(1, output, testCounter + 1);

	 Integer dealVolumeType = output.getInt("deal_volume_type",testCounter + 1);
	 output.setDateTime("gmt_start_date_time", testCounter + 1,currentgmtSDT);
	 output.setDateTime("gmt_end_date_time", testCounter + 1,currentgmtEDT);
	 double factor = 1.0;

	 // If deal volume type is hourly, check number of seconds
	 if (dealVolumeType == 0) 
	 {
	    factor = (double) currentgmtSDT.computeTotalSecondsInGMTDateRange(currentgmtEDT)/ (double) numSeconds;
	 } 
	 else 
	 {
	    // The factor is 1 divided by total number of days in schedule
	    factor = 1 / (double) numDays;
	 }

	 //for (int i = 0; i < doubleValCols.length; i++) {
		  //output.setDouble(doubleValCols[i], testCounter + 1,
			//	output.getDouble(doubleValCols[i], testCounter + 1)
				 //		* factor);
	 //}
      }
      return output;
   }
   public static Table GetShowAllVolumeTypesQueryString(String volumeTypes) throws OException
   {
      Table volumeTypeList;
      int iFoundRow, iRow;
      int numRowsAdded = 0;
      String volumeTypeStr, volumeTypesStripped;
      Table tMasterListCrudeVolumeTypes, tMasterListGasVolumeTypes;
      Table returnt = Table.tableNew();
      returnt.addCol( "volume_type", COL_TYPE_ENUM.COL_INT);
			   
      volumeTypeList = Table.tableNew( "volume_types" );
      volumeTypeList.addCol("volume_type", COL_TYPE_ENUM.COL_STRING );
			   
      tMasterListCrudeVolumeTypes = GetMasterVolumeTypes(true);
      tMasterListGasVolumeTypes = GetMasterVolumeTypes(false);
			   
      // now split the volume types apart so that we can create an ID comma separated string
      volumeTypesStripped = Str.stripBlanks(volumeTypes);
      if( Str.isNull( volumeTypesStripped ) == 0 && Str.isEmpty( volumeTypesStripped ) == 0 )
      {
    	  String delims = "[,;]";
    	  String[] tokens = volumeTypes.split(delims);
    	  for (int i = 0; i < tokens.length; i++)
    	  {
    		  numRowsAdded = volumeTypeList.addRow();
    		  volumeTypeList.setString(1, numRowsAdded, Str.toUpper(tokens[i]));
    	  }

	 // if no rows then exit as we should have something
	 if ( volumeTypeList.getNumRows() < 1 )
	 {
	    volumeTypeList.destroy();
	    OConsole.print("APM Gas Position Volume Types field populated but no valid values !!! Please correct the simulation result mod\n");
	    Util.exitFail();
	 }

	 // now we have a table of statuses - next job is to convert them into ID's
	 for (iRow = 1; iRow <= volumeTypeList.getNumRows(); iRow++)
	 {
		int num_rows;
	    volumeTypeStr = volumeTypeList.getString(1, iRow);

	    // BAV handled separately
	    if ( Str.equal(volumeTypeStr, "BAV") == 1)
	       continue;
	    // try to find them in the crude list first
	    iFoundRow = tMasterListCrudeVolumeTypes.findString(2, volumeTypeStr, SEARCH_ENUM.FIRST_IN_GROUP);
	    if ( iFoundRow > 0 )
	    {
	       returnt.setInt(1, returnt.addRow(), tMasterListCrudeVolumeTypes.getInt(1, iFoundRow));
	    }
	    else
	    {	
	       // not found there - so now try in the Gas list
	       iFoundRow = tMasterListGasVolumeTypes.findString(2, volumeTypeStr, SEARCH_ENUM.FIRST_IN_GROUP);
	       if ( iFoundRow > 0 )
	       {
		      returnt.setInt(1, returnt.addRow(), tMasterListGasVolumeTypes.getInt(1, iFoundRow));
	       }
	       else
	       {
		     volumeTypeList.destroy();
		     OConsole.print("Cannot find volume type from APM Gas Position Volume Types field: '" + volumeTypeStr + "'. Please correct the simulation result mod\n");
		     Util.exitFail();
	       }
	    }
	 }
      }
		      
	 if ( volumeTypeList != null && Table.isTableValid(volumeTypeList) == 1)
	    volumeTypeList.destroy();

	 return returnt;
   }
   public static Table GetMasterVolumeTypes(boolean getCrudeRatherThanGasVolumesTypes) throws OException
   {
	 // Prepare the master volume types table from db - this is used to get ID's from specified volume statuses
	 String sCachedTableName = "APM Gas Volume Types";
	 if (getCrudeRatherThanGasVolumesTypes)
		  sCachedTableName = "APM Crude Volume Types";
			   
	 Table tMasterListVolumeTypes = Table.getCachedTable(sCachedTableName);
	 if (Table.isTableValid(tMasterListVolumeTypes) == 0)
	 {
	    tMasterListVolumeTypes = Table.tableNew();
	    int retval = 0;
	    if (getCrudeRatherThanGasVolumesTypes)
		  retval = DBaseTable.loadFromDbWithSQL(tMasterListVolumeTypes, "id_number, crude_name",  "volume_type",  "crude_active_flag = 1");
	    else
		  retval = DBaseTable.loadFromDbWithSQL(tMasterListVolumeTypes, "id_number, gas_name",  "volume_type",  "gas_active_flag = 1");
			    	  
	    if( retval != 0)
	    {
	       // uppercase them so that any case sensitive stuff does not rear its ugly head
	       int numRows = tMasterListVolumeTypes.getNumRows();
	       for (int iRow = 1; iRow <= numRows; iRow++)
	       {
		  String volumeTypeStr = Str.toUpper(tMasterListVolumeTypes.getString(2, iRow));
		  tMasterListVolumeTypes.setString( 2, iRow, volumeTypeStr);
	       }

	       // sort so that the types can be easily found
	       tMasterListVolumeTypes.sortCol(2);
	       
	       Table.cacheTable(sCachedTableName, tMasterListVolumeTypes);
	    }
	 }	
			   
	 return tMasterListVolumeTypes;
   }
   public static Table doEnergyConversion(Table dataTable, int quantityUnitCol, 
		   												String[] massCols, String[] volumeCols, String[] energyCols,
		   												int massUnit, int volumeUnit, int energyUnit,
		   												int startDateCol, int matDateCol, int locationIDCol)throws OException 
	{
	   /*String[] massCols, String[] volumeCols,String[] energyCols 
	    *must be passed in as column sets like below 
	    *[column_to_be_convertedA , column_containing_converted_valueA, "Column Title A"]
		*[column_to_be_convertedB , column_containing_converted_valueB, "Column Title B"]
		*[column_to_be_convertedC , column_containing_converted_valueC, "Column Title C"]
	    * */
	   Table massConversionTable, volConversionTable, energyConversionTable;
	   int iStartDate,iEndDate,iNumRows,iRow, fromUnit, iLocationID;
	   double dMassConvFactor = 0, dVolConvFactor = 0, dEnergyConvFactor = 0;
	   int lastStartDate = -1, lastEndDate = -1, lastLocationID = -1, lastFromUnit = -1;
	   Boolean columnsAdded = false;
	   iNumRows = dataTable.getNumRows();
	   int multipleConvAcrossDates = 0;
	   
	   if(massCols.length > 0)
	   {
		   if(dataTable.getColNum(massCols[1]) > 0)
			   columnsAdded = true;	   
	   }
	   else if (volumeCols.length > 0)
	   {
		   if(dataTable.getColNum(volumeCols[1]) > 0)
			   columnsAdded = true;	   
	   }
	   else if (energyCols.length > 0)
	   {
		   if(dataTable.getColNum(energyCols[1]) > 0)
			   columnsAdded = true;		   
	   }
	   
	   if(iNumRows > 0 && columnsAdded == false)//check to see if the columns have been added already	
	   {
		   //add columns that will contain the converted values
		   for(int i = 1; i < massCols.length; i = i + 3)
		   {
			   dataTable.addCol(massCols[i],COL_TYPE_ENUM.COL_DOUBLE);
		   }
		   for(int i = 1; i < volumeCols.length; i = i + 3)
		   {
			   dataTable.addCol(volumeCols[i],COL_TYPE_ENUM.COL_DOUBLE);
		   }
		   for(int i = 1; i < energyCols.length; i = i + 3)
		   {
			   dataTable.addCol(energyCols[i],COL_TYPE_ENUM.COL_DOUBLE);
		   }
	   }
	   
	   for (iRow = 1; iRow <= iNumRows; iRow ++) 
	   {
		   iStartDate = dataTable.getInt(startDateCol, iRow);
		   iEndDate = dataTable.getInt(matDateCol, iRow);
		   iLocationID = dataTable.getInt(locationIDCol, iRow);
		   fromUnit = dataTable.getInt(quantityUnitCol, iRow);
		   
		   //if fields have not changed from last iteration through for loop do not need to look up conversions again
		   if((iStartDate != lastStartDate) || (iEndDate != lastEndDate) || 
			  (iLocationID != lastLocationID) || (fromUnit != lastFromUnit))
		   {
			   if(massUnit >= 0)
			   {
				   massConversionTable = Transaction.utilGetEnergyConversionFactors(iStartDate, iEndDate, iLocationID, fromUnit, massUnit);
				   dMassConvFactor = massConversionTable.getDouble("Conv Factor",1);
			   }
			   if(volumeUnit >= 0)
			   {
				   volConversionTable = Transaction.utilGetEnergyConversionFactors(iStartDate, iEndDate, iLocationID, fromUnit, volumeUnit);
				   dVolConvFactor = volConversionTable.getDouble("Conv Factor",1);
			   }
				   
			   if(energyUnit >= 0)
			   {
				   energyConversionTable = Transaction.utilGetEnergyConversionFactors(iStartDate, iEndDate, iLocationID, fromUnit, energyUnit);
				   dEnergyConvFactor = energyConversionTable.getDouble("Conv Factor",1);
			   }
		   
			   multipleConvAcrossDates = 0;
		   }

		   for(int i = 0; i < massCols.length; i++)
		   {
			   if(multipleConvAcrossDates == 1)
			   {
				   iRow = dataTable.insertRowAfter(iRow);//increment iRow when energy conversion different across date range
			   }
			   dataTable.setDouble(massCols[i+1], iRow, dMassConvFactor * dataTable.getDouble(massCols[i], iRow));
			   i = i + 2;
		   }
		   
		   for(int i = 0; i < volumeCols.length; i++)
		   {
			   dataTable.setDouble(volumeCols[i+1], iRow, dVolConvFactor * dataTable.getDouble(volumeCols[i], iRow));
			   i = i + 2;
		   }
		   
		   for(int i = 0; i < energyCols.length; i++)
		   {
			   dataTable.setDouble(energyCols[i+1], iRow, dEnergyConvFactor * dataTable.getDouble(energyCols[i], iRow));
			   i = i + 2;
		   }
		   
		   lastStartDate = iStartDate;
		   lastEndDate = iEndDate;
		   lastLocationID = iLocationID;
		   lastFromUnit = fromUnit;
	   }

	return dataTable;
	}
   
   public static Table formatEnergyConversion(Table outputTable, String massColArray[], String volumeColArray[], String energyColArray[]) throws OException
   {
	   for(int i = 1; i < massColArray.length; i +=3)
	   {
		   outputTable.setColTitle(massColArray[i], massColArray[i+1]);
		   outputTable.setColFormatAsNotnlAcct(massColArray[i], Util.NOTNL_WIDTH,	Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
	   }
	   
	   for(int i = 1; i < volumeColArray.length; i +=3)
	   {
		   outputTable.setColTitle(volumeColArray[i], volumeColArray[i+1]);
		   outputTable.setColFormatAsNotnlAcct(volumeColArray[i], Util.NOTNL_WIDTH,	Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
	   }
	   
	   for(int i = 1; i < energyColArray.length; i +=3)
	   {
		   outputTable.setColTitle(energyColArray[i], energyColArray[i+1]);
		   outputTable.setColFormatAsNotnlAcct(energyColArray[i], Util.NOTNL_WIDTH,	Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
	   }
	   
	   return outputTable;
   }

}
