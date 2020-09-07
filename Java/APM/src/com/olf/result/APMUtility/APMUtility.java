/* Released with version 05-Feb-2020_V17_0_8 of APM */

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
   
   /***
    * if the tran table is available it will fetch the transaction from the tran table using the tran num in data table for the specified row.
    * @param dataTable data table which has the tran number
    * @param tranNumCol datatable tran number col id
    * @param dataRow data table row
    * @param tranTable table holding all the trans
    * @param tranTableTranNumCol trantable tran number col
    * @return tran in tran table with tran num or null if not found
    * @throws OException
    */
   private static Transaction getTranFromTable (Table dataTable, int tranNumCol, int dataRow, Table tranTable, int tranTableTranNumCol) throws OException
   {
	   Transaction tran = null;
	   int dealNum, tranNum;
	   int tranDealNum, tranTranNum;
	   boolean found = false;
	   int tranRow = 0;
	   int tranNumRows;
	   
	   if (tranTable != null && tranTableTranNumCol > 0 && tranNumCol > 0)
	   {
		   tranNum = dataTable.getInt (tranNumCol, dataRow);
		   tranRow = tranTable.unsortedFindInt(tranTableTranNumCol, tranNum);
		   
		   if (tranRow > 0)
		   {
			   tran = tranTable.getTran ("tran_ptr", tranRow);
		   }
	   }
	   
	   return tran;
   }
   
   /***
    * if a tran is specified with side, profile seq num or delivery id then tran unit conversion using dual unit of measure logi will be applied, otherwise 
    * the function will use energy conversion table for converting values.
    * @param tran transaction 
    * @param side tran side
    * @param profileSeqNum tran profile seq num
    * @param deliveryId delivery id
    * @param iStartDate start date
    * @param iEndDate end date
    * @param iLocationID location id
    * @param fromUnit from unit for conversion
    * @param toUnit to unit for conversion
    * @return
    * @throws OException
    */
   public static double getConversionFactor (Transaction tran, int side, int profileSeqNum, int deliveryId, int volumeType, int iStartDate, int iEndDate, int iLocationID, int fromUnit, int toUnit, Table duomConversionTable, int tNum, String abColName) throws OException
   {
	   Table conversionTable;
	   double conversionFactor = 1.0;
	   
	   if (tran != null)
	   {
		   if (deliveryId > 0)
		   {
			   conversionFactor = tran.getUnitConversionFactorByDeliveryId(deliveryId, fromUnit, toUnit, side, volumeType, iStartDate, iEndDate);
		   }
		   else if (side >= 0 && profileSeqNum >= 0)
		   {
			   conversionFactor = tran.getUnitConversionFactorByProfile(side, profileSeqNum, fromUnit, toUnit, volumeType, iStartDate, iEndDate);
		   }
		   else if (iStartDate > 0 && iEndDate > 0 && iLocationID > 0)
		   {
			   conversionTable = Transaction.utilGetEnergyConversionFactors (iStartDate, iEndDate, iLocationID, fromUnit, toUnit);
			   conversionFactor = conversionTable.getDouble("Conv Factor", 1);
		   }
		   else
		   {
			   conversionFactor = Transaction.getUnitConversionFactor(fromUnit, toUnit);
		   }
	   }
	   else
	   {
		   conversionFactor = getDuomConversionFactor (duomConversionTable, abColName, tNum, side, profileSeqNum, deliveryId, volumeType, iStartDate, iEndDate, iLocationID, fromUnit, toUnit);
		   
		   if (conversionFactor < 0.0)
		   {
			   conversionTable = Transaction.utilGetEnergyConversionFactors(iStartDate, iEndDate, iLocationID, fromUnit, toUnit);
			   conversionFactor = conversionTable.getDouble("Conv Factor", 1);
		   }
	   }
	   
	   return conversionFactor;
   }
   
   private static double getDuomConversionFactor(Table duomConversionTable, String abColName, int tNum, int side, int profileSeqNum, int deliveryId, int volumeType, int iStartDate, int iEndDate, int iLocationID, int fromUnit, int toUnit) throws OException
   {
	   double conversionFactor = -1.0;
	   
	   if (fromUnit != toUnit && 
		   Util.utilGetUnitTypeFromUnit(fromUnit) != Util.utilGetUnitTypeFromUnit(toUnit) &&
	   	   duomConversionTable != null)
	   {
		   if (deliveryId > 0)
		   {
			   conversionFactor = getUnitConversionFactorById(duomConversionTable, abColName, tNum, side, "delivery_id", deliveryId, fromUnit, toUnit, volumeType, iStartDate, iEndDate);
		   }
		   else if (side >= 0 && profileSeqNum >= 0)
		   {
			   conversionFactor = getUnitConversionFactorById(duomConversionTable, abColName, tNum, side, "profile_seq_num", profileSeqNum, fromUnit, toUnit, volumeType, iStartDate, iEndDate);
		   }
	   }
	   
	   return conversionFactor;
   }

   private static double getConversionFactorForRange (Table ghvTable, int firstRow, int lastRow, int volumeType, int startDate, int endDate, int fromUnit, int toUnit) throws OException
   {
	   double conversionFactor = -1.0;
	   int rowVolumeType;
	   ODateTime startDt, endDt;
	   int bavFlag;

		int unit = ghvTable.getInt("unit", firstRow);
		int secondaryUnit = ghvTable.getInt("secondary_unit", firstRow);
		
		int energyUnit;
		int volumeUnit;
		
		if (Util.utilGetUnitTypeFromUnit(unit) == IDX_UNIT_TYPE.IDX_UNIT_TYPE_ENERGY.toInt())
		{
			energyUnit = unit;
			volumeUnit = secondaryUnit;
		}
		else
		{
			volumeUnit = unit;
			energyUnit = secondaryUnit;
		}
		
		APMWeightedGHV ghv = new APMWeightedGHV (energyUnit, volumeUnit);
		double quantity;
		double newGHV;
		double secQuantity;
		double energyQuantity;
		double volumeQuantity;
		
		for (int row = firstRow; row <= lastRow; row++)
		{
			rowVolumeType = ghvTable.getInt("volume_type", row);
			bavFlag = ghvTable.getInt ("bav_flag", row);
			startDt = ghvTable.getDateTime("gmt_start_date_time", row);
			endDt = ghvTable.getDateTime("gmt_end_date_time", row);
	
			if (volumeType != VOLUME_TYPE.VOLUME_TYPE_BAV.toInt() && rowVolumeType != volumeType)
				continue;
			if (volumeType == VOLUME_TYPE.VOLUME_TYPE_BAV.toInt() && bavFlag == 0)
				continue;
			
			if (startDt.getDate() > endDate || endDt.getDate() < startDate)
				continue;
			
			quantity = ghvTable.getDouble("quantity", row);
			newGHV = ghvTable.getDouble("gross_heating_value", row);
			secQuantity = ghvTable.getDouble("secondary_quantity", row);
		
			energyQuantity = getEnergyQuantity(unit, quantity, secQuantity);
			volumeQuantity = getVolumeQuantity(unit, quantity, secQuantity);
			
			ghv.calculateGHV(energyQuantity, volumeQuantity, newGHV);
		}
		
		conversionFactor = ghv.getConversionFactor (fromUnit, toUnit);
		
		return conversionFactor;
	}
   
   
    private static double getEnergyQuantity (int unit, double quantity, double secQuantity) throws OException
    {
    	if (Util.utilGetUnitTypeFromUnit(unit) == IDX_UNIT_TYPE.IDX_UNIT_TYPE_ENERGY.toInt())
		{
    		return quantity;
		}
		else
		{
			return secQuantity;
		}
    }
   
    private static double getVolumeQuantity (int unit, double quantity, double secQuantity) throws OException
    {
    	if (Util.utilGetUnitTypeFromUnit(unit) == IDX_UNIT_TYPE.IDX_UNIT_TYPE_ENERGY.toInt())
		{
    		return secQuantity;
		}
		else
		{
			return quantity;
		}
    }
    
	private static double getUnitConversionFactorById(Table duomConversionTable, String abColName, int tNum, int side, String idColName, int id, int fromUnit, int toUnit, int volumeType, int iStartDate, int iEndDate) throws OException 
	{
		double conversionFactor = -1.0;
		int abColNameFirstRow, abColNameLastRow;
		int sideFirstRow, sideLastRow;
		int idFirstRow, idLastRow;
		
		if (duomConversionTable != null)
		{
		   Table byProfile = duomConversionTable.getTable (idColName, 1);
		   
		   if (byProfile != null)
		   {
				abColNameFirstRow = byProfile.findInt (abColName, tNum, SEARCH_ENUM.FIRST_IN_GROUP);
				
				if (abColNameFirstRow > 0)
				{
					abColNameLastRow = byProfile.findInt (abColName, tNum, SEARCH_ENUM.LAST_IN_GROUP);
	
					if (side != -1)
						sideFirstRow = byProfile.findIntRange("param_seq_num", abColNameFirstRow, abColNameLastRow, side, SEARCH_ENUM.FIRST_IN_GROUP);
					else
						sideFirstRow = abColNameFirstRow;
					
					if (sideFirstRow > 0)
					{
						if (side != -1)
							sideLastRow = byProfile.findIntRange("param_seq_num", abColNameFirstRow, abColNameLastRow, side, SEARCH_ENUM.LAST_IN_GROUP);
						else
							sideLastRow = abColNameLastRow;
						
						/* search for profile seq num */
						idFirstRow = byProfile.findIntRange(idColName, sideFirstRow, sideLastRow, id, SEARCH_ENUM.FIRST_IN_GROUP);
						if (idFirstRow > 0)
						{
							idLastRow = byProfile.findIntRange(idColName, abColNameFirstRow, abColNameLastRow, id, SEARCH_ENUM.LAST_IN_GROUP);
							
							conversionFactor = getConversionFactorForRange (byProfile, idFirstRow, idLastRow, volumeType, iStartDate, iEndDate, fromUnit, toUnit);
						}
					}
				}
		   
		   }
		}
		   
		return conversionFactor;
	}

    /*
     *     get conversion factor in following order. 
		   1. get from parcel. 
		   2. get from profile if not parcel pricing.
		   3. get from system table if both failed.  
     */
    public static double GetUnitConversionFactorByParcel(int fromUnit, int toUnit, Transaction tran, int side, int profile, int parcelID) throws OException 
    {
       double convFactor = 0.0;
       boolean bConversionFound = false;
				
       if (tran != null && tran != Util.NULL_TRAN)
       {
          if (parcelID > 0)
          {
             convFactor = tran.getUnitConversionFactorByParcelId(parcelID, fromUnit, toUnit, side); 
             if (dcompare(convFactor, 0.0) != 0)
                bConversionFound = true;
          }
       
          int baseInsType = Instrument.getBaseInsType(tran.getInsType());       
          if (!bConversionFound && !Instrument.ins_type_is_comm_fee(baseInsType) && !Instrument.ins_type_is_commodity_financial(baseInsType))
          {
             convFactor = tran.getUnitConversionFactorByProfile(side, profile, fromUnit, toUnit);
             if (dcompare(convFactor, 0.0) != 0)
                 bConversionFound = true;
          }
       }
				
       if (!bConversionFound)
       {
          convFactor = Transaction.getUnitConversionFactor(fromUnit, toUnit);
       }
				
       return convFactor;
    }

/***
    * checks if the mass, volume and energy columns exists if they do not it will automatically add them
    * @param dataTable data table
    * @param massCols mass columns
    * @param volumeCols volume columns 
    * @param energyCols energy columns
    * @throws OException
    */
   private static void doColumns (Table dataTable, String[] massCols, String[] volumeCols, String[] energyCols) throws OException
   {
	   boolean columnsAdded = false;
	   int iNumRows = dataTable.getNumRows();
	   
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
   }
   
   public static Table doConversion (Table dataTable, int quantityUnitCol, String[] massCols, String[] volumeCols, String[] energyCols,
		   												int massUnit, int volumeUnit, int energyUnit,
		   												int startDateCol, int matDateCol, int locationIDCol, int dataTableTranNumCol, int deliveryIdCol, int sideCol, int profileSeqNumCol, Table tranTable, int tranTableTranNumCol, String abColName)throws OException 
	{
	   int iStartDate,iEndDate,iNumRows,iRow, fromUnit, iLocationID;
	   double dMassConvFactor = 0, dVolConvFactor = 0, dEnergyConvFactor = 0;
	   int lastStartDate = -1, lastEndDate = -1, lastLocationID = -1, lastFromUnit = -1;
	   int volumeTypeCol;
	   Transaction tran;
	   iNumRows = dataTable.getNumRows();
	   int multipleConvAcrossDates = 0;
	   int deliveryId;
	   int side, profileSeqNum;
	   int volumeType;
	   Transaction lastTran;
	   int lastSide, lastProfileSeqNum, lastDeliveryId, lastVolumeType;
	   int tNum;
	   
	   doColumns (dataTable, massCols, volumeCols, energyCols);
	   volumeTypeCol = dataTable.getColNum ("volume_type");
	   lastTran = null;
	   lastSide = -1;
	   lastProfileSeqNum = -1;
	   lastDeliveryId = 0;
	   lastVolumeType = -1;
	   Table duomConversionTable = null;
	   
	   duomConversionTable = generateDuomConversionTable (dataTable, dataTableTranNumCol, tranTable, tranTableTranNumCol, abColName);
	   
	   for (iRow = 1; iRow <= iNumRows; iRow ++) 
	   {
		   iStartDate = dataTable.getInt(startDateCol, iRow);
		   iEndDate = dataTable.getInt(matDateCol, iRow);
		   iLocationID = dataTable.getInt(locationIDCol, iRow);
		   fromUnit = dataTable.getInt(quantityUnitCol, iRow);
		   tran = getTranFromTable (dataTable, dataTableTranNumCol, iRow, tranTable, tranTableTranNumCol);
		   tNum = dataTable.getInt (dataTableTranNumCol, iRow);
		   
		   if (volumeTypeCol > 0)
			   volumeType = dataTable.getInt (volumeTypeCol, iRow);
		   else
			   volumeType = VOLUME_TYPE.VOLUME_TYPE_BAV.toInt();
		   
		   if (deliveryIdCol > 0)
			   deliveryId = dataTable.getInt (deliveryIdCol, iRow);
		   else
			   deliveryId = 0;
		   
		   if (sideCol > 0)
			   side = dataTable.getInt (sideCol, iRow);
		   else
			   side = -1;
		   
		   if (profileSeqNumCol > 0)
			   profileSeqNum = dataTable.getInt (profileSeqNumCol, iRow);
		   else
			   profileSeqNum = -1;
		   
		   //if fields have not changed from last iteration through for loop do not need to look up conversions again
		   if((iStartDate != lastStartDate) || (iEndDate != lastEndDate) || 
			  (iLocationID != lastLocationID) || (fromUnit != lastFromUnit) || 
			  deliveryId != lastDeliveryId || profileSeqNum != lastProfileSeqNum || 
			  tran != lastTran || side != lastSide || volumeType != lastVolumeType)
		   {
			   
			   if(massUnit >= 0)
			   {
				   dMassConvFactor = getConversionFactor (tran, side, profileSeqNum, deliveryId, volumeType, iStartDate, iEndDate, iLocationID, fromUnit, massUnit, duomConversionTable, tNum, abColName);
			   }

			   if(volumeUnit >= 0)
			   {
				   dVolConvFactor = getConversionFactor (tran, side, profileSeqNum, deliveryId, volumeType, iStartDate, iEndDate, iLocationID, fromUnit, volumeUnit, duomConversionTable, tNum, abColName);
			   }
				   
			   if(energyUnit >= 0)
			   {
				   dEnergyConvFactor = getConversionFactor (tran, side, profileSeqNum, deliveryId, volumeType, iStartDate, iEndDate, iLocationID, fromUnit, energyUnit, duomConversionTable, tNum, abColName);
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
		   lastTran = tran;
		   lastSide = side;
		   lastProfileSeqNum = profileSeqNum;
		   lastDeliveryId = deliveryId;
		   lastVolumeType = volumeType;
	   }
	   
	   
	   if (duomConversionTable != null)
		   duomConversionTable.destroy();
	   
	   return dataTable;
	}
   
   public static Table generateDuomConversionTable(Table dataTable, int dataTableTranNumCol, Table tranTable, int tranTableTranNumCol, String abColName) throws OException {
	   Transaction tran;
	   int numRows = dataTable.getNumRows();
	   int t_num;
	   int frow;
	   int pipelineId;
	   boolean isDuomPipeline = false;
	   Table qTranTable = new Table("duom_ghv_lookup_table");
	   Table duomConversionTable = null;
	   qTranTable.addCol(abColName, COL_TYPE_ENUM.COL_INT);
	
	   for (int row = 1; row <= numRows; row ++) 
	   {
		   t_num = dataTable.getInt(dataTableTranNumCol, row);
		   pipelineId = dataTable.getInt("pipeline_id", row);
		   
		   isDuomPipeline = Pipeline.isPipelineDualUnitOfMeasure(pipelineId);
		   if (isDuomPipeline == false)
			   continue;
		   
		   tran = getTranFromTable (dataTable, dataTableTranNumCol, row, tranTable, tranTableTranNumCol);
		   
		   if (tran == null)
		   {
			   frow = qTranTable.findInt(1, t_num, SEARCH_ENUM.FIRST_IN_GROUP);
			   
			   if (frow < 1)
			   {
				   qTranTable.insertRowBefore(-frow);
				   qTranTable.setInt(1, -frow, t_num);
			   }
		   }
	   }
	   
	   if (qTranTable.getNumRows() > 0) {
		   int queryId = Query.tableQueryInsert(qTranTable, 1);
		   
		   String sql = "select ab.deal_tracking_num, ab.tran_num, ab.ins_num, csh.schedule_id, csh.delivery_id, csh.volume_type, csh.profile_seq_num, csh.param_seq_num,  ip.unit, gpp.secondary_unit, csd.gmt_start_date_time, csd.gmt_end_date_time, csd.bav_flag, csd.quantity, csd.secondary_quantity, csd.gross_heating_value " +
				   "from ab_tran ab, comm_schedule_header csh,  comm_schedule_detail csd, gas_phys_param gpp , ins_parameter ip, gas_phys_location gpl, gas_phys_pipelines gppl, query_result qr " +
				   "where ab.ins_num = csh.ins_num and  " +
				   "csh.schedule_id = csd.schedule_id and  " +
				   "ab.ins_num = gpp.ins_num and  " +
				   "ab.current_flag = 1 and  " +
				   "gpp.param_seq_num = csh.param_seq_num and " + 
				   "ip.ins_num = ab.ins_num and  " +
				   "ip.param_seq_num = csh.param_seq_num and " + 
				   "gpp.location_id = gpl.location_id and " +
				   "gpl.pipeline_id = gppl.pipeline_id and " +
				   "gppl.dual_uom_pipeline = 1 and  " +
				   "csd.gross_heating_value > 0.0 and  " +
				   "qr.query_result = ab." + abColName + " and " +
				   "qr.unique_id = " + queryId;
		   duomConversionTable = new Table("duom_conversion_table");
		   Table byProfile = new Table ("conversion");
		   
		   DBaseTable.execISql(byProfile, sql);
		   Query.clear(queryId);
		   
		   duomConversionTable.addCol ("delivery_id", COL_TYPE_ENUM.COL_TABLE);
		   duomConversionTable.addCol ("profile_seq_num", COL_TYPE_ENUM.COL_TABLE);

		   Table byDeliveryId = byProfile.copyTable();
		   
		   byProfile.addGroupBy(abColName);
		   byProfile.addGroupBy("param_seq_num");
		   byProfile.addGroupBy("profile_seq_num");
		   byProfile.addGroupBy("volume_type");
		   byProfile.addGroupBy("gmt_start_date_time");
		   byProfile.groupBy();

		   byDeliveryId.addGroupBy(abColName);
		   byDeliveryId.addGroupBy("delivery_id");
		   byDeliveryId.addGroupBy("param_seq_num");
		   byDeliveryId.addGroupBy("volume_type");
		   byDeliveryId.addGroupBy("gmt_start_date_time");
		   byDeliveryId.groupBy();
		   
		   duomConversionTable.addRow();

		   duomConversionTable.setTable ("delivery_id", 1, byDeliveryId);
		   duomConversionTable.setTable ("profile_seq_num", 1, byProfile);
	   }
	   
	   qTranTable.destroy();
	   return duomConversionTable;
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
	   
	   iNumRows = dataTable.getNumRows();
	   int multipleConvAcrossDates = 0;
	   
	   doColumns (dataTable, massCols, volumeCols, energyCols);
	   
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
   
   public static int getQueryID( Table tTable, String sColumn ) throws OException
   {
       final int nAttempts = 10;
       
       int iQueryId = 0;
       
       int numberOfRetriesThusFar = 0;
       do {
          try {
	            // db call
             iQueryId = Query.tableQueryInsert( tTable, sColumn );
          } catch (OException exception) {
             OLF_RETURN_CODE olfReturnCode = exception.getOlfReturnCode();
             
             if (olfReturnCode == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR) {
                numberOfRetriesThusFar++;
	                
                String message = String.format("Query execution retry %1$d of %2$d [%3$s]. Check the logs for possible deadlocks.", numberOfRetriesThusFar, nAttempts, exception.getMessage());
                OConsole.oprint(message);
	                
                Debug.sleep(numberOfRetriesThusFar * 1000);
             } else {
                // it's not a retryable error, so leave
                break;
             }
          }
       } while (iQueryId == 0 && numberOfRetriesThusFar < nAttempts);
       
       return iQueryId;
   }
   
    /*-------------------------------------------------------------------------------
      Description:   makes sure date time added into msg
      -------------------------------------------------------------------------------*/
    public static void APM_Print(String sProcessingMessage) throws OException
    {
       String sMsg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ": " +
              sProcessingMessage + "\n";
       OConsole.oprint(sMsg);
    }
   
   public static void fillInUnits (Table outputTable, int volumeUnit, int energyUnit) throws OException
   {
	   int volCol = outputTable.getColNum( "volume_unit" );
       int energyCol = outputTable.getColNum( "energy_unit" );
       
       int numRows = outputTable.getNumRows();
       
       for (int row = 1; row <= numRows; row++)
       {
    	   if (volCol > 0)
    		   outputTable.setInt (volCol, row, volumeUnit);
    	   if (energyCol > 0)
    		   outputTable.setInt(energyCol, row, energyUnit);
       }
   }

   public static final double OLF_DOUBLE_CMP_EPSILON = 1e-14;
   
   /**
    * Compare 2 Doubles (using default epsilon of 1e-14) Return 0 if they are Equivalent. Return 1 if first number is larger, -1 if second number is larger.
    * 
    * Example:
    * public class SampleScript implements IScript
	* {
	*    public void execute(IContainerContext context) throws OException
	*    {  
	*	    double a = 1.0, b = 1.01;
	*
	*	    int isEqual = Math.dcompare(a, b);
	*	    if (isEqual == 0)
	*		    OConsole.oprint("\nA =" + a "; b = " + b; " + " They are within epsilon of each other.");
	*	     if (isEqual == -1)
	*		    OConsole.oprint("\nA =" + a "; b = " + b; " + " B is larger.");
	*	     if (isEqual == 1)
	*		    OConsole.oprint("\nA =" + a "; b = " + b; " + " A is larger.");
	*    }
    * }
    * 
    * @param a The first double to compare
    * @param b The second double to compare
    * @return Return 1 if first number is larger, -1 if second number is larger, 0 when they are equal
    */
   public static int dcompare(double a, double b) {
	   return dcompare(a, b, OLF_DOUBLE_CMP_EPSILON);
   }
   
   /**
    * Compares 2 double values using epsilon as the max allowable error for the double representation.
    * 
    * public class SampleScript implements IScript
	* {
	*    public void execute(IContainerContext context) throws OException
	*    {  
	*	     double a = 1.0, b = 1.01;
	*        double epsilon = 0.1;
    *
	*        int isEqual = Math.dcompare(a, b, epsilon);
	*	     if (isEqual == 0)
	*		    OConsole.oprint("\nA =" + a "; b = " + b; " + " They are within epsilon of each other.");
	*	     if (isEqual == -1)
	*		    OConsole.oprint("\nA =" + a "; b = " + b; " + " B is larger.");
	*	     if (isEqual == 1)
	*		    OConsole.oprint("\nA =" + a "; b = " + b; " + " A is larger.");
	*    }
    * }
    * 
    * @param a The first double to compare
    * @param b The second double to compare
    * @param epsilon The max difference between d1 and d2 to be used that will define them as equal.
    * @return Return 1 if first number is larger, -1 if second number is larger, 0 when they are equal
    */
   public static int dcompare(double a, double b, double epsilon) {
	   int    exponent;
	   double calculated_epsilon;
	   double difference;
	   double max_dval;

	   if (a == 0.0 || b == 0.0)
	   {
	       calculated_epsilon = epsilon;
	   }
	   else
	   {
	      max_dval = (Math.abs(a) > Math.abs(b) ? a : b);
	      exponent = Math.getExponent(max_dval);
	      calculated_epsilon = epsilon * Math.pow(2, exponent);
	   }

	   difference         = a - b;

	   if (difference > calculated_epsilon)
	   {
	      /* dval1 > dval2 */
	      return 1;
	   }
	   else if (difference < -calculated_epsilon)
	   {
	      /* dval1 < dval2 */
	      return -1;
	   }
	   /* else -calculated_epsilon <= difference <= calculated_epsilon */

	   /* a == b */
	   return 0;
   }   
}
