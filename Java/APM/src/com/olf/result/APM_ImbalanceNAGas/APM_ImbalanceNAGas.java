/* Released with version 17-Aug-2016_V17_0_3 of APM */

/*
File Name:                      APM_ICBalance.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Joe Gallagher
Creation Date:                  May 12, 2014
 
Revision History:				1. 10/22/2014	Initial Check in
												
Script Type:                    User-defined simulation result

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    

*/ 

package com.olf.result.APM_ImbalanceNAGas;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;


import com.olf.result.APMUtility.*;

import com.olf.openjvs.Math;
import com.olf.openjvs.OException;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.fnd.OCalendarBase;

import java.util.Vector;

public class APM_ImbalanceNAGas implements IScript
{
   static String massColArray[] = {};/*Not applicable to gas*/
   
   static String volumeColArray[] = {"quantity", 	"quantity_vol",	"Hourly\nVolume",
	   								 "volume_quantity", "volume_quantity_vol",	"Daily\nVolume"};
   
   static String energyColArray[] = {"quantity", 	"quantity_energy",	"Hourly\nEnergy",
			 						 "volume_quantity", "volume_quantity_energy",	"Daily\nEnergy"};

   @Override
   public void execute(IContainerContext arg0) throws OException 
   {
      Table argt = Util.NULL_TABLE, returnt = Util.NULL_TABLE;
      int intOperation;
      argt = arg0.getArgumentsTable();
      returnt = arg0.getReturnTable();      
      
      intOperation = argt.getInt("operation", 1);
      if (intOperation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt()){
    	  computeResult(argt, returnt);}
      else if (intOperation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt()){
    	  formatResult(returnt);}
   }

   private static void computeResult(Table argt, Table returnt) throws OException 
   {
	   int volumeUnit = -1, energyUnit = -1;
	   int showAllVolumeStatuses = 0;
	   int iAttributeGroup;
	   int startPoint = 0;
	   int endPoint = 0;
	   int apmServiceType = 0;
	   String volumeTypesStr = "", transVolTypesStr = "";
	   String strVal;
	   Table tblData = Table.tableNew();
	   Table tblAttributeGroups, tblConfig;
	   Table tblTrans = argt.getTable("transactions", 1);
	   int unitsCol, deal_start_date_col = -1, deal_maturity_date_col = -1, location_id_col = -1;
	            
	   tblAttributeGroups = SimResult.getAttrGroupsForResultType(argt.getInt("result_type", 1));
	   iAttributeGroup = tblAttributeGroups.getInt("result_config_group", 1);
	   tblConfig = SimResult.getResultConfig(iAttributeGroup);
	   tblConfig.sortCol("res_attr_name");
	      
      if (APMUtility.ParamHasValue(tblConfig, "APM Imbalance Start Date") > 0){
    	  startPoint = APMUtility.GetParamDateValue(tblConfig, "APM Imbalance Start Date");}

      if (APMUtility.ParamHasValue(tblConfig, "APM Imbalance End Date") > 0){
    	  endPoint = APMUtility.GetParamDateValue(tblConfig, "APM Imbalance End Date");}

      if (APMUtility.ParamHasValue(tblConfig, "APM Imbalance Volume Types") > 0){
    	  volumeTypesStr = APMUtility.GetParamStrValue(tblConfig, "APM Imbalance Volume Types");}

      if (APMUtility.ParamHasValue(tblConfig, "APM Imbalance Volume Unit") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM Imbalance Volume Unit");
    	  if (!strVal.isEmpty()){
    		  volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);}
      }

      if (APMUtility.ParamHasValue(tblConfig, "APM Imbalance Energy Unit") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM Imbalance Energy Unit");
    	  if (!strVal.isEmpty()){
    		  energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);}
      }
      
      if (volumeUnit < 0){
    	  volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MCF");}
      
      if (energyUnit < 0){
    	  energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MMBTU");}       
      
      if (APMUtility.ParamHasValue(tblConfig, "APM Imbalance Show All Volume Statuses") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM Imbalance Show All Volume Statuses");
    	  if (!strVal.isEmpty()){
    		  showAllVolumeStatuses = Ref.getValue(SHM_USR_TABLES_ENUM.YES_NO_TABLE, strVal);}
      }
      
      Vector<Integer> allowedInsTypes = new Vector<Integer>();
      int iQueryID = APMUtility.generateDealQuery(argt, allowedInsTypes);
      
      if (iQueryID > 0)
      {
		 tblData = doCalculations(iQueryID, tblTrans, false,
					  showAllVolumeStatuses, /*ConversionStyle.DELIVERY_BASED_CONVERSION,*/
					  startPoint, endPoint, volumeTypesStr, transVolTypesStr, apmServiceType);
			  
		 if ( tblData.getNumRows() > 0 ){
	    	  returnt.select(tblData, "*", "deal_tracking_num GE 0");}
		 
		 unitsCol = tblData.getColNum("unit");
		 deal_start_date_col = tblData.getColNum("day_start_date_time");
		 deal_maturity_date_col = tblData.getColNum("day_end_date_time");
		 location_id_col = tblData.getColNum("location_id");
		 APMUtility.fillInUnits (returnt, volumeUnit, energyUnit);
	 	 doEnergyConversion(returnt, unitsCol, volumeUnit, energyUnit, deal_start_date_col, deal_maturity_date_col, location_id_col, tblTrans);
	 	 addCumulativeColumns(returnt);
	 	 deleteUnnecessaryColumns(returnt);
		 Query.clear(iQueryID);
      }
   }

   private static Table doEnergyConversion(Table outputTable, int unit_col, 
		    int volumeUnit, int energyUnit, int deal_start_date_col, 
		   int deal_maturity_date_col, int location_id_col, Table tranTable) throws OException
	{
	   	int dataTableDealNumCol = outputTable.getColNum("tran_num");
	    int deliveryIdCol = outputTable.getColNum("delivery_id");
	    int sideCol =  outputTable.getColNum("param_seq_num");
	    int profileSeqNumCol =  outputTable.getColNum("profile_seq_num");
	    int tranTableDealNumCol = tranTable.getColNum ("tran_num");
	    
	    APMUtility.doConversion(outputTable, 
								unit_col, 
								massColArray,/*mass col pairs to be converted*/
								volumeColArray,/*vol col pairs to be converted*/
								energyColArray,/*energy col pairs to be converted*/
								-1,/*mass conversion not implemented for APM_ICBalance*/
								volumeUnit,/*modified target*/ 
								energyUnit,/*modified target*/
								deal_start_date_col,
								deal_maturity_date_col,
								location_id_col, 
								dataTableDealNumCol, 
								deliveryIdCol, 
								sideCol, 
								profileSeqNumCol, 
								tranTable, 
								tranTableDealNumCol,
								"tran_num");
	
		return outputTable;
	}
		
   private static Table doCalculations(int iQueryID, Table tblTrans, boolean flipSigns, int showAllVolumeStatuses, 
			int startPoint, int endPoint, String volumeTypesStr, String transVolTypesStr,
			int apmServiceType)
			throws OException 
   {
	   int      out_row, num_out_rows, day, volume_date_colnum, del_day_colnum, del_month_colnum;

	   Table volumeTypesTable = APMUtility.GetShowAllVolumeTypesQueryString(volumeTypesStr);
	   
	   startPoint = OCalendarBase.getSOM(startPoint);
	   startPoint = OCalendarBase.getSOM(startPoint-1);
	   Table outputTable = retrieveData(iQueryID, flipSigns, showAllVolumeStatuses, startPoint, endPoint, volumeTypesTable, apmServiceType);
	   
	   outputTable.addCol("start_date_date", COL_TYPE_ENUM.COL_INT);
	   outputTable.addCol("start_date_time", COL_TYPE_ENUM.COL_INT);
	   outputTable.addCol("end_date_date", COL_TYPE_ENUM.COL_INT);
	   outputTable.addCol("end_date_time", COL_TYPE_ENUM.COL_INT);
	   for (int intRow = 1; intRow <= outputTable.getNumRows(); intRow++)
	   {
	      int pay_rec = outputTable.getInt( "pay_rec" , intRow );
	      if (pay_rec == RECEIVE_PAY_ENUM.PAY.toInt())
	      {
	         double hourlyVolume = outputTable.getDouble( "quantity", intRow );
	         hourlyVolume = Math.abs(hourlyVolume) * -1.0;

	         outputTable.setDouble( "quantity", intRow, hourlyVolume );
	      }
	      int iStartDateDate = outputTable.getDate("day_start_date_time", intRow);
	      int iStartDateTime = outputTable.getTime("day_start_date_time", intRow);
	      int iEndDateDate = outputTable.getDate("day_end_date_time", intRow);
	      int iEndDateTime = outputTable.getTime("day_end_date_time", intRow);
	      
	      if (iEndDateTime == 0) 
	      {
	    	  iEndDateDate--;
	    	  iEndDateTime -= 86400;
	      }
	      
	      outputTable.setInt("start_date_date", intRow, iStartDateDate);
	      outputTable.setInt("start_date_time", intRow, iStartDateTime);
	      outputTable.setInt("end_date_date", intRow, iEndDateDate);
	      outputTable.setInt("end_date_time", intRow, iEndDateTime);
	   }
	   
	   NomPosition.expandDailyVolume(outputTable , "day_start_date_time", "day_end_date_time", "quantity", 
			   						startPoint, endPoint, endPoint/*intDailyViewEndDate*/ , 1/*expand_rows Arrays not supported in APM*/, 
			   						"volume_date", "volume_quantity", 0/*intUnitID conversion done in script*/, "unit", 
			   						"location_id", "time_zone", "day_start_time", "", "", 
			   						"gmt_start_date_time", "gmt_end_date_time");
	   
	   
//	  startPoint = OCalendarBase.getSOM(startPoint);
// 	  startPoint = OCalendarBase.getSOM(startPoint-1);	  
	  //Table tblPriorImbal = showCumulativeImbalance(outputTable, OCalendarBase.getEOM(startPoint) + 1);
//	  Table tblPriorImbal = showCumulativeImbalance(outputTable, OCalendarBase.getSOM(startPoint));
	  //startPoint = OCalendarBase.getEOM(startPoint);

//	  if (tblPriorImbal != null )
//	  {
//		  if (tblPriorImbal.getNumRows()> 0)
//		  {
//			  NomPosition.expandDailyVolume(tblPriorImbal , "day_start_date_time", "day_end_date_time", "quantity", 
//				   						startPoint, endPoint, endPoint/*intDailyViewEndDate*/ , 0/*expand_rows*/, 
//				   						"volume_date", "volume_quantity", 0/*intUnitID*/, "unit", 
//				   						"location_id", "time_zone", "day_start_time", "", "", 
//				   						"gmt_start_date_time", "gmt_end_date_time");
//		   
//		   //Copy the starting balances to the outputTable table
//		   outputTable.select( tblPriorImbal, "*", "ins_num GT 0");
//		   }
//	   	   tblPriorImbal.destroy();
//	  }
      outputTable.addCol( "delivery_day",   COL_TYPE_ENUM.COL_INT);
      outputTable.addCol( "delivery_month", COL_TYPE_ENUM.COL_INT);
      volume_date_colnum = outputTable.getColNum( "volume_date");
      del_day_colnum     = outputTable.getColNum( "delivery_day");
      del_month_colnum   = outputTable.getColNum( "delivery_month");

      num_out_rows = outputTable.getNumRows();
      for (out_row = 1; out_row <= num_out_rows; out_row++)
      {
    	  day = outputTable.getInt( volume_date_colnum, out_row);
		  outputTable.setInt( del_day_colnum, out_row, day);
		  outputTable.setInt( del_month_colnum, out_row, OCalendar.getSOM (day));
      }
	   
      outputTable.addCol( "volume_unit",   COL_TYPE_ENUM.COL_INT);
      outputTable.addCol( "energy_unit", COL_TYPE_ENUM.COL_INT);

	   return outputTable;
   }

   private static Table retrieveData(int iQueryID, boolean flipSigns, int showAllVolumeStatuses,
				    int startPoint, int endPoint, Table volumeTypesTable,
				    int apmServiceType)
				    throws OException 
   {
      Table dataTable = Table.tableNew();
      
      String dateConditionStr = "";
      if (startPoint != 0 || endPoint != 0)
      {
    	  dateConditionStr = " and ( ";
    	  
    	  if (startPoint != -1)
    	  {
    		  String startPointStr = OCalendar.formatJdForDbAccess(startPoint);
    		  String endPointStr = OCalendar.formatJdForDbAccess(endPoint);
    		  dateConditionStr = dateConditionStr
    				  + " ( "
//    				  + " ab_tran.maturity_date >= '" + startPointStr + "' "
//    				  + " and comm_schedule_delivery.end_date_time >= '" + startPointStr + "' "
//    				  + " and ab_tran.start_date <= '" + endPointStr + "' "
//    				  + " and comm_schedule_delivery.start_date_time <= '" + endPointStr + "' "
    				  + " ab_tran.maturity_date >= '" + startPointStr + "' "
    				  + " and comm_schedule_detail.day_end_date_time >= '" + startPointStr + "' "
    				  + " and ab_tran.start_date <= '" + endPointStr + "' "
    				  + " and comm_schedule_detail.day_start_date_time <= '" + endPointStr + "' "
    				  + " ) ";
    	  }
    	  
    	  dateConditionStr = dateConditionStr + " ) ";
      }

      String volumeTypeConditionStr = "";
      int numVolumeTypeRows = volumeTypesTable.getNumRows();
      
      if (numVolumeTypeRows > 0)
      {
    	  int volumeType;
    	  // If there's just one volume type then use '=' expression.
    	  if (numVolumeTypeRows == 1)
    	  {
    		  volumeType = volumeTypesTable.getInt(1, 1);
    		  if(volumeType == 4 || volumeType == 6 || volumeType == 7 ||volumeType == 8)
    		  {
    			  volumeTypeConditionStr = volumeTypeConditionStr + " and comm_schedule_header.volume_type = " + volumeTypesTable.getInt(1, 1) + " ";
    		  }
    	  }
    	  // For more than one volume type use the 'in' expression.
    	  else
    	  {
    		  int volumeStrCreated = 0;  
   		  
		      for (int vtRow = 1; vtRow <= numVolumeTypeRows; vtRow++)
		      {
		    	  volumeType = volumeTypesTable.getInt(1, vtRow);
		    	  if(volumeType == 4 || volumeType == 6 || volumeType == 7 ||volumeType == 8)/*volume types supported by ICBalance*/
		    	  {
		    		  if(volumeStrCreated == 0)
		    		  {
		    			  volumeTypeConditionStr = volumeTypeConditionStr + " and comm_schedule_header.volume_type in (";
		    			  volumeStrCreated = 1;
		    		  }
		    		  else
		    		  {
		    			  volumeTypeConditionStr = volumeTypeConditionStr + ", ";
		    		  }
		    		  volumeTypeConditionStr = volumeTypeConditionStr + volumeTypesTable.getInt(1, vtRow);
		    	  }
		      }
		      if(volumeStrCreated == 0)/*Chose all volume types that were not supported*/
		      {
		    	  volumeTypeConditionStr = volumeTypeConditionStr + " and comm_schedule_header.volume_type in (-1";
		      }
   			  volumeTypeConditionStr = volumeTypeConditionStr + ") "; 
    	  }
      }
      else
      {
    	  volumeTypeConditionStr = " and comm_schedule_header.volume_type IN(4, 6, 7, 8) ";
      }
    	  
      
      String bavConditionStr = "";
      if (showAllVolumeStatuses == 0)
      {
    	  // Show only the BAV volumes.
    	  bavConditionStr = " and comm_schedule_detail.bav_flag = 1 ";
      }
      
      String sqlConditionStr = dateConditionStr + volumeTypeConditionStr + bavConditionStr;
      
      String fromClauseStr;
     
   	  fromClauseStr = " query_result join "
		   		 + " ab_tran on "
		   		 + "    ab_tran.tran_num = query_result.query_result join "
		   		 + " comm_schedule_header  on "
		   		 + " 	ab_tran.ins_num = comm_schedule_header.ins_num join " 
 			     + " comm_schedule_detail on "
 		   		 + " 	comm_schedule_detail.schedule_id = comm_schedule_header.schedule_id join "
 		   		 + " parameter on "
		   		 + " 	parameter.ins_num = comm_schedule_header.ins_num "
		   		 + " 	and parameter.param_seq_num = comm_schedule_header.param_seq_num join "
 		   		 + " phys_header on "
 		   		 + " 	phys_header.ins_num = comm_schedule_header.ins_num join "
 		   		 + " gas_phys_location on "
 		   		 + " 	gas_phys_location.location_id = comm_schedule_header.location_id join "
 		   		 + " gas_phys_param on "
		   		 + " 	gas_phys_param.ins_num = comm_schedule_header.ins_num "
		   		 + " 	and gas_phys_param.param_seq_num = comm_schedule_header.param_seq_num join "
		   		 + " gas_phys_pipelines on "
		   		 + " 	gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id ";
      
      String sQuery = "  select distinct "
		   + " ab_tran.tran_num "
		   + " , ab_tran.deal_tracking_num "
		   + " , ab_tran.ins_num "
		   + " , ab_tran.trade_date "
		   + " , ab_tran.start_date "
		   + " , ab_tran.maturity_date "
		   + " , ab_tran.internal_bunit "
		   + " , ab_tran.external_bunit "
		   + " , ab_tran.internal_lentity "
		   + " , ab_tran.external_lentity "
		   + " , ab_tran.internal_portfolio "
		   + " , ab_tran.ins_type "
		   + " , ab_tran.ins_sub_type "
		   + " , ab_tran.buy_sell "
		   + " , ab_tran.internal_contact "
		   + " , ab_tran.reference "
		   + " , comm_schedule_header.delivery_id "
		   + " , comm_schedule_header.location_id "
		   + " , comm_schedule_detail.day_start_date_time "
		   + " , comm_schedule_detail.day_end_date_time " 
		   + " , comm_schedule_header.volume_type " 
		   + " , comm_schedule_detail.bav_flag " 
		   + " , comm_schedule_detail.quantity " 
		   + " , comm_schedule_header.unit " 
		   + " , comm_schedule_header.param_seq_num "
		   + " , comm_schedule_header.profile_seq_num "
		   + " , parameter.pay_rec "
		   + " , phys_header.service_type " 
		   + " , gas_phys_location.pipeline_id " 
		   + " , gas_phys_location.zone_id " 
		   + " , gas_phys_location.meter_id "
		   + " , gas_phys_location.vpool_id "
		   + " , phys_header.contract_number " 
		   + " , phys_header.allow_pathing " 
		   + " , 0   grand_total "
		   + " , ab_tran.tran_status "
		   + " , gas_phys_location.idx_subgroup "
		   + " , gas_phys_param.measure_group_id "
		   + " , gas_phys_param.time_zone "
		   + " , gas_phys_param.day_start_time "
		   + " , gas_phys_pipelines.region_id "
		   + " , gas_phys_location.geo_loc_id "
		   + " , gas_phys_param.loc_service_type "
		   + " , phys_header.cash_out "
		   + " , gas_phys_location.index_id "
		   + " from " 
		   + fromClauseStr 		 
		   + " where "
		   + " ab_tran.current_flag = 1 "
		   + " and ab_tran.base_ins_type = 48040 "
		   + " and query_result.unique_id = "
		   + iQueryID
		   + " and comm_schedule_detail.quantity != 0"
		   + sqlConditionStr;
					
      com.olf.openjvs.DBase.runSqlFillTable(sQuery, dataTable);
      return dataTable;
   }

   private static void formatResult(Table outputTable) throws OException
   {
	   outputTable.setColTitle( "delivery_id",         "Nom: Delivery ID");
	   
	   outputTable.setColTitle( "cumulative_imbal",    "Rolling Imbalance");

	   outputTable.setColTitle( "cumulative_imbal_volume",    "Rolling Imbalance\nVolume");
	   
	   outputTable.setColTitle( "cumulative_imbal_energy",    "Rolling Imbalance\nEnergy");

	   outputTable.setColTitle( "pay_rec",             "Nom: Pay/Receive");
	   outputTable.setColFormatAsRef( "pay_rec",             SHM_USR_TABLES_ENUM.REC_PAY_TABLE);

	   outputTable.setColTitle( "location_id",         "Loc: Name");
	   outputTable.setColFormatAsRef( "location_id",         SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);

	   outputTable.setColTitle( "meter_id",            "Loc: Meter Num");

	   outputTable.setColTitle( "pipeline_id",         "Loc: Pipeline");
	   outputTable.setColFormatAsRef( "pipeline_id",         SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);

	   outputTable.setColTitle( "zone_id",             "Loc: Zone");
	   outputTable.setColFormatAsRef( "zone_id",             SHM_USR_TABLES_ENUM.GAS_PHYS_ZONE_TABLE );

	   outputTable.setColTitle( "vpool_id",            "Loc: Value Pool");
	   outputTable.setColFormatAsRef( "vpool_id",            SHM_USR_TABLES_ENUM.GAS_PHYS_VALUE_POOL_TABLE );

	   outputTable.setColTitle( "loc_service_type",    "Loc: Loc Service Type");
	   outputTable.setColFormatAsRef( "loc_service_type",    SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE );

	   outputTable.setColTitle( "bav_flag",            "Vol: BAV Flag");
	   outputTable.setColFormatAsRef( "bav_flag",            SHM_USR_TABLES_ENUM.YES_NO_TABLE);

	   outputTable.setColTitle( "day_start_date_time", "Vol: Start DateTime");
	   outputTable.setColFormatAsDate( "day_start_date_time", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);

	   outputTable.setColTitle( "day_end_date_time",   "Vol: End DateTime");
	   outputTable.setColFormatAsDate( "day_end_date_time",   DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);

	   outputTable.setColTitle( "quantity",            "Vol: Hourly Quantity");
	   outputTable.setColFormatAsNotnlAcct( "quantity",            12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

	   outputTable.setColTitle( "volume_type",         "Vol: Volume Type");
	   outputTable.setColFormatAsRef( "volume_type",         SHM_USR_TABLES_ENUM.GAS_TSD_VOLUME_TYPES_TABLE );

	   outputTable.setColTitle( "external_bunit",      "Deal: External BU");
	   outputTable.setColFormatAsRef( "external_bunit",      SHM_USR_TABLES_ENUM.PARTY_TABLE );

	   outputTable.setColTitle( "service_type",        "Deal: Service Type");
	   outputTable.setColFormatAsRef( "service_type",        SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE );

	   outputTable.setColTitle( "unit",                "Vol: Units");
	   outputTable.setColFormatAsRef( "unit",                SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE );

	   outputTable.setColTitle( "volume_unit",                "Volume Units");
	   outputTable.setColFormatAsRef( "volume_unit",                SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE );
	   
	   outputTable.setColTitle( "energy_unit",                "Energy Units");
	   outputTable.setColFormatAsRef( "energy_unit",                SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE );
	   
	   outputTable.setColTitle( "param_seq_num",       "Deal: Side");

	   outputTable.setColTitle( "tran_status",         "Deal: Status");
	   outputTable.setColFormatAsRef( "tran_status",         SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE );

	   outputTable.setColTitle( "ins_type",            "Deal: Ins. Type");
	   outputTable.setColFormatAsRef( "ins_type",            SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE );

	   outputTable.setColTitle( "ins_sub_type",        "Deal: Ins. Sub-type");
	   outputTable.setColFormatAsRef( "ins_sub_type",        SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE );

	   outputTable.setColTitle( "tran_num",            "Deal: Tran Number");

	   outputTable.setColTitle( "deal_tracking_num",   "Deal: Tracking Number");

	   outputTable.setColTitle( "ins_num",             "Deal: Ins Number");

	   outputTable.setColTitle( "contract_number",     "Deal: Contract");

	   outputTable.setColTitle( "allow_pathing",       "Deal: Allow Pathing");
	   outputTable.setColFormatAsRef( "allow_pathing",       SHM_USR_TABLES_ENUM.NO_YES_TABLE );

	   outputTable.setColTitle( "trade_date",          "Deal: Trade Date");
	   outputTable.setColFormatAsDate( "trade_date",          DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT );

	   outputTable.setColTitle( "start_date",          "Deal: Start Date");
	   outputTable.setColFormatAsDate( "start_date",          DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT );

	   outputTable.setColTitle( "maturity_date",       "Deal: End Date");
	   outputTable.setColFormatAsDate( "maturity_date",       DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT );

	   outputTable.setColTitle( "buy_sell",            "Deal: Buy/Sell");
	   outputTable.setColFormatAsRef( "buy_sell",            SHM_USR_TABLES_ENUM.BUY_SELL_TABLE );

	   outputTable.setColTitle( "internal_contact",    "Deal: Trader");

	   outputTable.setColTitle( "reference",           "Deal: Reference");

	   outputTable.setColTitle( "sequence",            "z_sequence");

	   outputTable.setColTitle( "internal_bunit",      "Deal: Int. Business Unit");
	   outputTable.setColFormatAsRef( "internal_bunit",      SHM_USR_TABLES_ENUM.PARTY_TABLE );

	   outputTable.setColTitle( "internal_lentity",    "Deal: Int. Legal Entity");
	   outputTable.setColFormatAsRef( "internal_lentity",    SHM_USR_TABLES_ENUM.PARTY_TABLE );

	   outputTable.setColTitle( "external_lentity",    "Deal: Ext. Legal Entity");
	   outputTable.setColFormatAsRef( "external_lentity",    SHM_USR_TABLES_ENUM.PARTY_TABLE );

	   outputTable.setColTitle( "internal_portfolio",  "Deal: Int. Portfolio");
	   outputTable.setColFormatAsRef( "internal_portfolio",  SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE );

	   outputTable.setColTitle( "region_id",           "Deal: Region");
	   outputTable.setColFormatAsRef( "region_id",           SHM_USR_TABLES_ENUM.GAS_PHYS_REGIONS_TABLE );

	   outputTable.setColTitle( "geo_loc_id",          "Deal: Geographic Loc");
	   outputTable.setColFormatAsRef( "geo_loc_id",          SHM_USR_TABLES_ENUM.GEO_LOCATION_TABLE );

	   outputTable.setColTitle( "grand_total",         "Grand Total");

	   outputTable.setColTitle( "idx_subgroup", "Loc: Index Subgroup");
	   outputTable.setColFormatAsRef( "idx_subgroup", SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE);

	   outputTable.setColTitle( "measure_group_id", "Loc: Measure Group");
	   outputTable.setColFormatAsRef( "measure_group_id", SHM_USR_TABLES_ENUM.MEASURE_GROUP_TABLE);
	   
	   outputTable.setColTitle( "volume_quantity",     "Vol: Quantity");
	   outputTable.setColFormatAsNotnlAcct( "volume_quantity",     12, 0, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

	   outputTable.setColTitle( "volume_date",         "Vol: Date");
	   outputTable.setColFormatAsDate( "volume_date",         DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);

	   outputTable.setColTitle( "delivery_day",        "Vol: Delivery Day");
	   outputTable.setColFormatAsDate( "delivery_day",        DATE_FORMAT.DATE_FORMAT_DAY_OF_MONTH, DATE_LOCALE.DATE_LOCALE_DEFAULT);

	   outputTable.setColTitle( "delivery_month",      "Vol: Delivery Month");
	   outputTable.setColFormatAsDate( "delivery_month",      DATE_FORMAT.DATE_FORMAT_IMM, DATE_LOCALE.DATE_LOCALE_DEFAULT);
	   
	   outputTable.setColTitle( "time_zone",      "Time Zone");
	   outputTable.setColFormatAsRef( "time_zone", SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE);
	   
	   outputTable.colHide("day_start_time");

 	   APMUtility.formatEnergyConversion(outputTable, massColArray, volumeColArray, energyColArray);
   }
   
//   private static Table showCumulativeImbalance(Table outputTable, int stardJD)throws OException
//   {
//	String strSelectSql="", strFromSql="", strWhereSql="", dateConditionStr = "", strSql = "";
//   		
//   	Table tblStartBal = Table.tableNew();
//   	// Get the unique set of instrument numbers
//   	if(outputTable.getNumRows() == 0)
//   		return tblStartBal;
//   	
//   	int intQueryId = Query.tableQueryInsert(outputTable, "tran_num");
//   	//int intQueryId = Query.tableQueryInsert(outputTable, "ins_num");
//   	if (intQueryId <= 0)
//   		return tblStartBal;
//   	//String queryTableName = Query.getResultTableForId(intQueryId);
//   	// Get Database version of start date of page
//   	String  strProfileDate = OCalendar.formatJdForDbAccess(stardJD);
//   	
//   	strSelectSql = "SELECT ab_tran.tran_num, ab_tran.deal_tracking_num, ab_tran.ins_num, ab_tran.trade_date, " +
//       " ab_tran.start_date, ab_tran.maturity_date, ab_tran.internal_bunit, ab_tran.external_bunit, ab_tran.internal_lentity, " +
//       " ab_tran.external_lentity, ab_tran.internal_portfolio, ab_tran.ins_type, ab_tran.ins_sub_type, ab_tran.buy_sell, ab_tran.internal_contact, " +
//       " ab_tran.reference, ab_tran.tran_status, comm_schedule_header.delivery_id, gas_phys_location.location_id, gas_phys_location.pipeline_id, gas_phys_location.meter_id, gas_phys_location.vpool_id, " +
//       " gas_phys_location.idx_subgroup, gas_phys_location.measure_group_id, comm_schedule_detail.day_start_date_time, comm_schedule_detail.day_end_date_time, comm_schedule_header.volume_type, comm_schedule_header.unit, comm_schedule_header.param_seq_num,  " +
//       " case when (to_date('" + strProfileDate+ "' ) - to_date(comm_schedule_detail.day_end_date_time)) < 1 then " +
//       " ((comm_schedule_detail.day_end_date_time - comm_schedule_detail.day_start_date_time) * comm_schedule_detail.quantity) - comm_schedule_detail.quantity " +
//       " else ((comm_schedule_detail.day_end_date_time - comm_schedule_detail.day_start_date_time) * comm_schedule_detail.quantity) end as ohd_quantity, " +
//       " ins_parameter.pay_rec, phys_header.service_type, phys_header.contract_number, gas_phys_pipelines.region_id, gas_phys_location.zone_id, gas_phys_location.geo_loc_id, " +
//       " phys_header.allow_pathing, 1 AS bav_flag,  gas_phys_param.time_zone, gas_phys_param.day_start_time, gas_phys_param.loc_service_type, phys_header.cash_out ";
//   	
//   	strFromSql = "FROM query_result join "
//   				+ " ab_tran on "
//   					+ " ab_tran.tran_num = query_result.query_result join " 
//   				+ " comm_schedule_header on "
//   					+ " comm_schedule_header.ins_num = ab_tran.ins_num join " 
//   				+ " comm_schedule_detail on "
//   					+ " comm_schedule_detail.schedule_id = comm_schedule_header.schedule_id join "
//   	   			+ " ins_parameter on "
//   					+ " ins_parameter.ins_num = comm_schedule_header.ins_num "
//   					+ " and ins_parameter.param_seq_num = comm_schedule_header.param_seq_num join "
//				+ " gas_phys_param on "
//   					+ " gas_phys_param.ins_num = ab_tran.ins_num "
//   					+ " and ins_parameter.param_seq_num = gas_phys_param.param_seq_num join  "
//   				+ " phys_header on "
//   					+ " comm_schedule_header.ins_num = phys_header.ins_num join "
//   				+ " gas_phys_location on "
//   					+ " gas_phys_location.location_id = comm_schedule_header.location_id join "
//   				+ " gas_phys_pipelines gas_phys_pipelines on "
//   					+ " gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id ";
//       
//   	
//   	strWhereSql = " where query_result.unique_id = " + Str.intToStr(intQueryId) + " "
//   				+ " and phys_header.cash_out = 0 " 
//   	       		+ " and ab_tran.current_flag = 1 " 
//   	       		+ " and ab_tran.base_ins_type = " + INS_TYPE_ENUM.comm_imbalance.toInt() + " " 
//   	    		+ " and comm_schedule_detail.quantity > 0 " 
//   	    		+ " and comm_schedule_detail.bav_flag = 1";
//   	
//   	if (stardJD != 0)
//    {
//  	  dateConditionStr = "";
//  	  
//  	  dateConditionStr = dateConditionStr
//  				  + " and comm_schedule_detail.day_end_date_time <= '" + strProfileDate + "'"
//  				  + " and ab_tran.maturity_date >= '" + strProfileDate + "'";  //this will exclude expired deals
//  	 }
//   	
//   	strSql = strSelectSql + strFromSql + strWhereSql + dateConditionStr;
//
//   	DBaseTable.execISql(tblStartBal, strSql);
//
//    double hourlyVolume = 0.0;
//    int intNumRowsM = tblStartBal.getNumRows();
//   	for (int intCount = 1; intCount <= intNumRowsM; intCount++) 
//   	{
//   		// Set the end date to end on the first day of the current month
//   		tblStartBal.setDateTimeByParts( "day_start_date_time", intCount, (stardJD - 1), 0);
//   		tblStartBal.setDateTimeByParts( "day_end_date_time", intCount, stardJD, 0);
//   		int pay_rec = tblStartBal.getInt( "pay_rec" , intCount );
//   	    if (pay_rec == RECEIVE_PAY_ENUM.PAY.toInt())
//   	    {
//   	         hourlyVolume = tblStartBal.getDouble( "quantity", intCount );
//   	         hourlyVolume = Math.abs(hourlyVolume) * -1.0;
//   	         tblStartBal.setDouble( "quantity", intCount, hourlyVolume );
//   	    }
//   	}	
//   			
//   	Query.clear(intQueryId);
//       
//   	return  tblStartBal;
//   }
   
   private static void addCumulativeColumns(Table outputTable) throws OException
   {
	   outputTable.addCol("cumulative_imbal", COL_TYPE_ENUM.COL_DOUBLE);
	   outputTable.addCol("cumulative_imbal_volume", COL_TYPE_ENUM.COL_DOUBLE);
	   outputTable.addCol("cumulative_imbal_energy", COL_TYPE_ENUM.COL_DOUBLE);
	   int cash_out_col = outputTable.getColNum("cash_out");
	   for(int count = 1; count <= outputTable.getNumRows(); count++)
	   {
		   if(outputTable.getInt(cash_out_col, count) == 0/*cashout zero*/)
		   {
			   double loopQuantity = outputTable.getDouble("volume_quantity", count);
			   double loopQuantityVolume = outputTable.getDouble("volume_quantity_vol", count);
			   double loopQuantityEnergy = outputTable.getDouble("volume_quantity_energy", count);
			   outputTable.setDouble("cumulative_imbal", count, loopQuantity );
			   outputTable.setDouble("cumulative_imbal_volume", count, loopQuantityVolume );
			   outputTable.setDouble("cumulative_imbal_energy", count, loopQuantityEnergy );  
		   }
	   }
   }
   
   private static void deleteUnnecessaryColumns(Table outputTable) throws OException
   {
	   outputTable.delCol ("profile_seq_num");
   }
}
