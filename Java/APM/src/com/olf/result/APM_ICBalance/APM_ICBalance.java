/* Released with version 29-Sep-2014_V14_1_7 of APM */

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

package com.olf.result.APM_ICBalance;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;


import com.olf.result.APMUtility.*;

import com.olf.openjvs.OException;
import com.olf.openjvs.Pipeline;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import java.util.Vector;

public class APM_ICBalance implements IScript
{
   static String massColArray[] = {};
   
   static String volumeColArray[] = {"month_end_balance_from_pipe", 	"month_end_balance_vol",	"Month End Balance\nVolume",
	     							 "daily_quantity_from_pipe", 		"daily_quantity_vol",		"Daily Quantity\nVolume",
	     							 "monthly_in_from_pipe", 			"monthly_in_vol",			"Monthly In\nVolume",
	     							 "monthly_out_from_pipe", 		"monthly_out_vol",			"Monthly Out\nVolume",
	     							 "fuel_quantity_from_pipe",		"fuel_quantity_vol",		"Fuel\nVolume",
	     							 "quantity_from_pipe",			"quantity_vol",				"Hourly Quantity\nVolume"};

static String energyColArray[] = {"month_end_balance_from_pipe", 	"month_end_balance_energy",	"Month End Balance\nEnergy",
	     						  "daily_quantity_from_pipe", 		"daily_quantity_energy",	"Daily Quantity\nEnergy",
	     						  "monthly_in_from_pipe", 			"monthly_in_energy",		"Monthly In\nEnergy",
	     						  "monthly_out_from_pipe", 		"monthly_out_energy",		"Monthly Out\nEnergy",
	     						  "fuel_quantity_from_pipe",		"fuel_quantity_energy",		"Fuel\nEnergy",
	     						  "quantity_from_pipe",			"quantity_energy",			"Hourly Quantity\nEnergy"};

   @Override
   public void execute(IContainerContext arg0) throws OException 
   {
      Table argt = Util.NULL_TABLE, returnt = Util.NULL_TABLE;
      int intOperation;
      argt = arg0.getArgumentsTable();
      returnt = arg0.getReturnTable();      
      
      // Call the virtual functions according to action type
      intOperation = argt.getInt("operation", 1);
      if (intOperation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt()) 
      {
	 computeResult(argt, returnt);
      }
      else if (intOperation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt())
      {
	 formatResult(returnt);
      }
   }

   private static void computeResult(Table argt, Table returnt) throws OException 
   {
	   int volumeUnit = -1, energyUnit = -1;
	   int showAllVolumeStatuses = 0;
	   int iAttributeGroup;
	   int startPoint = -1;
	   int endPoint = -1;
	   int apmServiceType = 0;
	   String volumeTypesStr = "", transVolTypesStr = "";
	   String strVal;
	   Table tblData = Table.tableNew();
	   Table tblAttributeGroups, tblConfig;
	   Table tblTrans = argt.getTable("transactions", 1);
	   int unitsCol, deal_start_date_col = -1, deal_maturity_date_col = -1, location_id_col = -1;
	            
	   // Check if the query result contains deal numbers or delivery ID's.
	   Table simDef = argt.getTable("sim_def", 1);
	   if(simDef.getColNum("apm_service_type") > 1)
	   {
		   if ("Nomination".equals(simDef.getString("apm_service_type", 1)))
			   apmServiceType = 1;
	   }
	   
	   tblAttributeGroups = SimResult.getAttrGroupsForResultType(argt.getInt("result_type", 1));
	   iAttributeGroup = tblAttributeGroups.getInt("result_config_group", 1);
	   tblConfig = SimResult.getResultConfig(iAttributeGroup);
	   tblConfig.sortCol("res_attr_name");
	      
      if (APMUtility.ParamHasValue(tblConfig, "APM IC Balance Start Date") > 0){
    	  startPoint = APMUtility.GetParamDateValue(tblConfig, "APM IC Balance Start Date");}

      if (APMUtility.ParamHasValue(tblConfig, "APM IC Balance End Date") > 0){
    	  endPoint = APMUtility.GetParamDateValue(tblConfig, "APM IC Balance End Date");}

      if (APMUtility.ParamHasValue(tblConfig, "APM IC Balance Volume Types") > 0){
    	  volumeTypesStr = APMUtility.GetParamStrValue(tblConfig, "APM IC Balance Volume Types");}

      if (APMUtility.ParamHasValue(tblConfig, "APM IC Balance Volume Unit") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM IC Balance Volume Unit");
    	  if (!strVal.isEmpty())
    	  {
	    volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);
    	  }
      }

      if (APMUtility.ParamHasValue(tblConfig, "APM IC Balance Energy Unit") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM IC Balance Energy Unit");
    	  if (!strVal.isEmpty()){
    		  energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);}
      }
      
      if (volumeUnit < 0){
    	  volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MCF");}
      
      if (energyUnit < 0){
    	  energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MMBTU");}       
      
      if (APMUtility.ParamHasValue(tblConfig, "APM IC Balance Show All Volume Statuses") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM IC Balance Show All Volume Statuses");
    	  if (!strVal.isEmpty()){
    		  showAllVolumeStatuses = Ref.getValue(SHM_USR_TABLES_ENUM.YES_NO_TABLE, strVal);}
      }
      
      Vector<Integer> allowedInsTypes = new Vector<Integer>();
      int iQueryID = 0;
      if ( apmServiceType != 1 ) //coming from trading manager
          iQueryID = APMUtility.generateDealQuery(argt, allowedInsTypes);
      else // coming from nom update (APM nomination service)
          iQueryID = APMUtility.generateNomQuery(argt, allowedInsTypes);
      
      if (iQueryID > 0)
      {
		 tblData = doCalculations(iQueryID, tblTrans, false,
					  showAllVolumeStatuses, /*ConversionStyle.DELIVERY_BASED_CONVERSION,*/
					  startPoint, endPoint, volumeTypesStr, transVolTypesStr, apmServiceType);
			  
		 if ( tblData.getNumRows() > 0 ){
	    	  returnt.select(tblData, "*", "service_provider_deal_num GE 0");}
		 
		 unitsCol = tblData.getColNum("unit");
		 deal_start_date_col = tblData.getColNum("day_start_date_time");
		 deal_maturity_date_col = tblData.getColNum("day_end_date_time");
		 location_id_col = tblData.getColNum("location_id");
	 	 doEnergyConversion(returnt, unitsCol, volumeUnit, energyUnit, deal_start_date_col, deal_maturity_date_col, location_id_col);

		 Query.clear(iQueryID);
      }
   }

   private static Table doEnergyConversion(Table outputTable, int unit_col, 
		    int volumeUnit, int energyUnit, int deal_start_date_col, 
		   int deal_maturity_date_col, int location_id_col) throws OException
	{
		APMUtility.doEnergyConversion(outputTable, 
		unit_col, 
		massColArray,/*mass col pairs to be converted*/
		volumeColArray,/*vol col pairs to be converted*/
		energyColArray,/*energy col pairs to be converted*/
		-1,/*mass conversion not implemented for APM_ICBalance*/
		volumeUnit,/*modified target*/ 
		energyUnit,/*modified target*/
		deal_start_date_col,
		deal_maturity_date_col,
		location_id_col);
	
		return outputTable;
	}
		
   private static Table doCalculations(int iQueryID, Table tblTrans, boolean flipSigns, int showAllVolumeStatuses, 
			int startPoint, int endPoint, String volumeTypesStr, String transVolTypesStr,
			int apmServiceType)
			throws OException 
   {
	   Table volumeTypesTable = APMUtility.GetShowAllVolumeTypesQueryString(volumeTypesStr);
	   
	   Table outputTable = retrieveData(iQueryID, flipSigns, showAllVolumeStatuses, startPoint, endPoint, volumeTypesTable, apmServiceType);
	   
	   outputTable = expandDailyVolume (outputTable , startPoint, endPoint);
	   
	   return outputTable;
   }

   private static Table retrieveData(int iQueryID, boolean flipSigns, int showAllVolumeStatuses,
				    int startPoint, int endPoint, Table volumeTypesTable,
				    int apmServiceType)
				    throws OException 
   {
      Table dataTable = Table.tableNew();
      
      String dateConditionStr = "";
      if (startPoint != -1 || endPoint != -1)
      {
    	  dateConditionStr = " and ( ";
    	  
    	  if (startPoint != -1)
    	  {
    		  String startPointStr = OCalendar.formatJdForDbAccess(startPoint);
    		  dateConditionStr = dateConditionStr
    				  + " ( "
    				  + " ab_tran.maturity_date > '" + startPointStr + "' "
    				  + " and comm_schedule_delivery.end_date_time > '" + startPointStr + "' "
    				  + " and comm_schedule_header.day_end_date_time > '" + startPointStr + "' "
    				  + " and comm_schedule_detail.day_end_date_time > '" + startPointStr + "' "
    				  + " ) ";
    	  }
    	  
    	  if (endPoint != -1)
    	  {	  
    		  String endPointStr = OCalendar.formatJdForDbAccess(endPoint);
    		  
    		  if (startPoint != -1)
    			  dateConditionStr = dateConditionStr + " and ";
    		  
    		  dateConditionStr = dateConditionStr
    				  + " ( "
    				  + " ab_tran.start_date < '" + endPointStr + "' "
    				  + " and comm_schedule_delivery.start_date_time < '" + endPointStr + "' "
    				  + " and comm_schedule_header.day_start_date_time < '" + endPointStr + "' "
    				  + " and comm_schedule_detail.day_start_date_time < '" + endPointStr + "' "
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
      
      String sqlConditionStr = dateConditionStr + /*volumeTypeConditionStr +*/ bavConditionStr;
      
      String recFromClauseStr;
      String delFromClauseStr;
      String ZCGWhereClauseStr = "";
      
      if (apmServiceType == 1)
	  {
    	  // Nomination
		   
    	  recFromClauseStr = " query_result join "
		  		    + " comm_schedule_delivery on "
		   		    + "    comm_schedule_delivery.delivery_id = query_result.query_result join "
		   		    + " ab_tran on "
		   		    + "    ab_tran.deal_tracking_num = comm_schedule_delivery.service_provider_deal_num join "
		   		    + " phys_header on "
		   		    + "    phys_header.ins_num = ab_tran.ins_num join "
		   		    + " parameter on "
		   		    + "    parameter.ins_num = ab_tran.ins_num join "
		   		    + " comm_schedule_header on "
		   		    + "    comm_schedule_header.ins_num = ab_tran.ins_num "
		   		    + "    and comm_schedule_header.delivery_id = comm_schedule_delivery.delivery_id "
		   		    + "    and comm_schedule_header.param_seq_num = parameter.param_seq_num join "
		   		    + " comm_schedule_detail on "
		   		    + "    comm_schedule_detail.schedule_id = comm_schedule_header.schedule_id join "
		   		    + " gas_phys_location on "
		   		    + "    gas_phys_location.location_id = comm_schedule_delivery.receipt_location_id join "
		   		    + " gas_phys_connection on "
					+ "    gas_phys_connection.connection_id = gas_phys_location.interconnect_loc_id join "
		   		    + " gas_phys_pipelines on "
		   		    + "    gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id ";
    	  
    	  delFromClauseStr = " query_result join "
		  		    + " comm_schedule_delivery on "
		   		    + "    comm_schedule_delivery.delivery_id = query_result.query_result join "
		   		    + " ab_tran on "
		   		    + "    ab_tran.deal_tracking_num = comm_schedule_delivery.service_provider_deal_num join "
		   		    + " phys_header on "
		   		    + "    phys_header.ins_num = ab_tran.ins_num join "
		   		    + " parameter on "
		   		    + "    parameter.ins_num = ab_tran.ins_num join "
		   		    + " comm_schedule_header on "
		   		    + "    comm_schedule_header.ins_num = ab_tran.ins_num "
		   		    + "    and comm_schedule_header.delivery_id = comm_schedule_delivery.delivery_id "
		   		    + "    and comm_schedule_header.param_seq_num = parameter.param_seq_num join "
		   		    + " comm_schedule_detail on "
		   		    + "    comm_schedule_detail.schedule_id = comm_schedule_header.schedule_id join "
		   		    + " gas_phys_location on "
		   		    + "    gas_phys_location.location_id = comm_schedule_delivery.delivery_location_id join "
		   		    + " gas_phys_connection on "
					+ "    gas_phys_connection.connection_id = gas_phys_location.interconnect_loc_id join "
		   		    + " gas_phys_pipelines on "
		   		    + "    gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id ";
    	  
    	  //Special logic for zero cost gathering
    	  ZCGWhereClauseStr = " and comm_schedule_delivery.delivery_id = query_result.query_result ";
	  }
	  else
	  {
		  // Deal
		   
		  recFromClauseStr = " query_result join "
		   		    + " ab_tran on "
		   		    + "    ab_tran.deal_tracking_num = query_result.query_result join "
		   		    + " phys_header on "
		   		    + "    phys_header.ins_num = ab_tran.ins_num join "
		   		    + " parameter on "
		   		    + "    parameter.ins_num = ab_tran.ins_num join "
		   		    + " comm_schedule_delivery on "
		   		    + "    comm_schedule_delivery.service_provider_deal_num = ab_tran.deal_tracking_num join "
		   		    + " comm_schedule_header on "
		   		    + "    comm_schedule_header.ins_num = ab_tran.ins_num "
		   		    + "    and comm_schedule_header.delivery_id = comm_schedule_delivery.delivery_id "
		   		    + "    and comm_schedule_header.param_seq_num = parameter.param_seq_num join "
		   		    + " comm_schedule_detail on "
		   		    + "    comm_schedule_detail.schedule_id = comm_schedule_header.schedule_id join "
		   		    + " gas_phys_location on "
		   		    + "    gas_phys_location.location_id = comm_schedule_delivery.receipt_location_id join "
					+ " gas_phys_connection on "
					+ "    gas_phys_connection.connection_id = gas_phys_location.interconnect_loc_id join "
		   		    + " gas_phys_pipelines on "
		   		    + "    gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id ";
		  
		  delFromClauseStr = " query_result join "
		   		    + " ab_tran on "
		   		    + "    ab_tran.deal_tracking_num = query_result.query_result join "
		   		    + " phys_header on "
		   		    + "    phys_header.ins_num = ab_tran.ins_num join "
		   		    + " parameter on "
		   		    + "    parameter.ins_num = ab_tran.ins_num join "
		   		    + " comm_schedule_delivery on "
		   		    + "    comm_schedule_delivery.service_provider_deal_num = ab_tran.deal_tracking_num join "
		   		    + " comm_schedule_header on "
		   		    + "    comm_schedule_header.ins_num = ab_tran.ins_num "
		   		    + "    and comm_schedule_header.delivery_id = comm_schedule_delivery.delivery_id "
		   		    + "    and comm_schedule_header.param_seq_num = parameter.param_seq_num join "
		   		    + " comm_schedule_detail on "
		   		    + "    comm_schedule_detail.schedule_id = comm_schedule_header.schedule_id join "
		   		    + " gas_phys_location on "
		   		    + "    gas_phys_location.location_id = comm_schedule_delivery.delivery_location_id join "
					+ " gas_phys_connection on "
					+ "    gas_phys_connection.connection_id = gas_phys_location.interconnect_loc_id join "
		   		    + " gas_phys_pipelines on "
		   		    + "    gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id ";
		  
		  ZCGWhereClauseStr = " and ab_tran.deal_tracking_num = query_result.query_result ";
	  }
      
      String sQuery = "  select distinct "
		      + " gas_phys_connection.connection_id ohi_connection_id "
		      + " ,  gas_phys_location.pipeline_id ohi_pipeline_id "
		      + " ,  ab_tran.internal_lentity ohi_internal_lentity "
		      + " ,  phys_header.contract_number "
		      + " ,  (select distinct a.external_lentity "
			  + "      from "
			  + "        ab_tran a join "
			  + "        comm_sched_deliv_deal c on "
			  + "        c.deal_num = a.deal_tracking_num "
			  + "      where "
			  + "        a.current_flag = 1 "
			  + "        and c.delivery_id = comm_schedule_delivery.delivery_id "
			  + "        and c.receipt_delivery = parameter.pay_rec "
			  + "        ) ohi_external_lentity " 
      		   + " ,  comm_schedule_delivery.receipt_location_id ohi_trans_location_id " 
      		 + " ,   (select gas_phys_location.meter_id "
	  		 + "      from "
	  		 + "        gas_phys_location "
	  		 + "      where "
	  		 + "        gas_phys_location.location_id = comm_schedule_delivery.delivery_location_id "
	  		 + "         ) meter_id "
		   + " ,  comm_schedule_header.volume_type ohi_volume_type "
		   + " ,  comm_schedule_delivery.nom_transaction_type ohi_nom_transaction_type "
		   + " ,  gas_phys_location.location_type ohi_location_type "
		   + " ,  gas_phys_location.location_id ohi_location_id "
		   + " ,  comm_schedule_detail.day_start_date_time "
		   + " ,  comm_schedule_detail.day_end_date_time "
		   + " ,  comm_schedule_delivery.delivery_id ohi_delivery_id " 
		   + " ,  comm_schedule_delivery.delivery_status ohi_delivery_status " 
		   + " ,  ab_tran.price ohd_price " 
		   + " ,  comm_schedule_delivery.fuel_quantity ohd_fuel_quantity_from_pipe "
		   + " ,  comm_schedule_header.schedule_id ohi_schedule_id "
		   + " ,  0 ohi_ins_type "
		   + " ,  parameter.pay_rec ohi_pay_rec "
		   + " ,  comm_schedule_detail.bav_flag ohi_bav_flag " 
		   + " ,  comm_schedule_detail.quantity ohd_quantity_from_pipe "
		   + " ,  comm_schedule_header.unit ohi_unit "
		   + " ,  gas_phys_location.zone_id ohi_zone_id "
		   + " ,  0    ohi_to_pipeline_id "
		   + " ,  comm_schedule_delivery.pipeline_id    ohi_from_pipeline_id "
		   + " ,  0    ohi_selectedpipe "
		   + " ,  comm_schedule_delivery.start_date_time "
		   + " ,  comm_schedule_delivery.end_date_time "
		   + " ,  0    ohi_num_days "
		   + " ,  comm_schedule_delivery.service_provider_deal_num ohi_service_provider_deal_num "
 		   + " , gas_phys_location.index_id "
		   + " from "
		+ delFromClauseStr 
		+ " where "
		   + " (   ab_tran.ins_type in (48020, 48030) or ab_tran.ins_type in (select id_number from instruments where base_ins_id in (48020, 48030))) " 
		   + " and comm_schedule_delivery.delivery_status  <> 8 "
		   + " and parameter.pay_rec                        = 0 "
		   + " and ab_tran.current_flag                     = 1 "
		   + " and gas_phys_location.active                 = 1 "
		   + " and comm_schedule_delivery.delivery_location_type = 2 "
		   + " and parameter.start_date                     <= comm_schedule_detail.day_start_date_time "
		   + " and ab_tran.start_date                       <= comm_schedule_detail.day_start_date_time "
		   + " and comm_schedule_delivery.pipeline_id	     = gas_phys_pipelines.pipeline_id "
		   + " and query_result.unique_id = "
		   + iQueryID
		   + sqlConditionStr
		+ " UNION ALL "
		+ " select distinct "
		   + " gas_phys_connection.connection_id ohi_connection_id "
		   + " ,  gas_phys_location.pipeline_id ohi_pipeline_id " 
		   + " ,  ab_tran.internal_lentity ohi_internal_lentity "
		   + " ,  phys_header.contract_number "
        + " ,  (select distinct a.external_lentity "
	    + "      from "
	    + "        ab_tran a join "
	    + "        comm_sched_deliv_deal c on "
	    + "        c.deal_num = a.deal_tracking_num "
	    + "      where "
	    + "        a.current_flag = 1 "
	    + "        and c.delivery_id = comm_schedule_delivery.delivery_id "
	    + "        and c.receipt_delivery = parameter.pay_rec "
	    + "        ) ohi_external_lentity "
		+ " ,  comm_schedule_delivery.delivery_location_id ohi_trans_location_id "
 		+ " ,   (select gas_phys_location.meter_id "
 		+ "      from "
 		+ "        gas_phys_location "
 		+ "      where "
 		+ "        gas_phys_location.location_id = comm_schedule_delivery.delivery_location_id "
 		+ "         ) meter_id "
		+ " ,  comm_schedule_header.volume_type ohi_volume_type "
		+ " ,  comm_schedule_delivery.nom_transaction_type ohi_nom_transaction_type "
		+ " ,  gas_phys_location.location_type ohi_location_type " 
		+ " ,  gas_phys_location.location_id ohi_location_id "
		+ " ,  comm_schedule_detail.day_start_date_time "
		+ " ,  comm_schedule_detail.day_end_date_time "
		+ " ,  comm_schedule_delivery.delivery_id ohi_delivery_id "
		+ " ,  comm_schedule_delivery.delivery_status ohi_delivery_status "
		+ " ,  ab_tran.price ohd_price "
		+ " ,  comm_schedule_delivery.fuel_quantity ohd_fuel_quantity_from_pipe "
		+ " ,  comm_schedule_header.schedule_id ohi_schedule_id "
		+ " ,  0 ohi_ins_type "
		+ " ,  parameter.pay_rec ohi_pay_rec "
		+ " ,  comm_schedule_detail.bav_flag ohi_bav_flag "
		+ " ,  comm_schedule_detail.quantity ohd_quantity_from_pipe "
		+ " ,  comm_schedule_header.unit ohi_unit "
		+ " ,  gas_phys_location.zone_id ohi_zone_id "
		+ " ,  comm_schedule_delivery.pipeline_id    ohi_to_pipeline_id "
		+ " ,  0    ohi_from_pipeline_id "
		+ " ,  0    ohi_selectedpipe "
		+ " ,  comm_schedule_delivery.start_date_time "
		+ " ,  comm_schedule_delivery.end_date_time "
		+ " ,  0    ohi_num_days "
		+ " ,  comm_schedule_delivery.service_provider_deal_num ohi_service_provider_deal_num "
		+ " , gas_phys_location.index_id "
	    + " from "
		+ recFromClauseStr 
	     + " where "
		+ " (   ab_tran.ins_type in (48020, 48030) "
		+ "  or ab_tran.ins_type in (select id_number from instruments where base_ins_id in (48020, 48030))) "
		+ " and comm_schedule_delivery.delivery_status  <> 8 "
		+ " and parameter.pay_rec                        = 1 "
		+ " and ab_tran.current_flag                     = 1 "
		+ " and gas_phys_location.active                 = 1 "
		+ " and comm_schedule_delivery.receipt_location_type = 2 "
		+ " and parameter.start_date                     <= comm_schedule_detail.day_start_date_time "
		+ " and ab_tran.start_date                       <= comm_schedule_detail.day_start_date_time "
		+ " and comm_schedule_delivery.pipeline_id	     = gas_phys_pipelines.pipeline_id "
		+ " and query_result.unique_id = "
		   + iQueryID
		+ sqlConditionStr
	     + " UNION ALL "
	     + " select distinct "
		+ " gas_phys_connection.connection_id ohi_connection_id " 
		+ " ,  gas_phys_location.pipeline_id ohi_pipeline_id "
		+ " ,  ab_tran.internal_lentity ohi_internal_lentity "
		+ " ,  phys_header.contract_number "
        + " ,  (select distinct a.external_lentity "
	    + "      from "
	    + "        ab_tran a join "
	    + "        comm_sched_deliv_deal c on "
	    + "        c.deal_num = a.deal_tracking_num "
	    + "      where "
	    + "        a.current_flag = 1 "
	    + "        and c.delivery_id = comm_schedule_delivery.delivery_id "
	    + "        and c.receipt_delivery = parameter.pay_rec "
	    + "        ) ohi_external_lentity "
		+ " ,  comm_schedule_delivery.receipt_location_id ohi_trans_location_id "
 		+ " ,   (select gas_phys_location.meter_id "
 		+ "      from "
 		+ "        gas_phys_location "
 		+ "      where "
 		+ "        gas_phys_location.location_id = comm_schedule_delivery.delivery_location_id "
 		+ "         ) meter_id "
		   + " ,  comm_schedule_header.volume_type ohi_volume_type "
		   + " ,  comm_schedule_delivery.nom_transaction_type ohi_nom_transaction_type "
		   + " ,  gas_phys_location.location_type ohi_location_type "
		   + " ,  gas_phys_location.location_id ohi_location_id "
		   + " ,  comm_schedule_detail.day_start_date_time "
		   + " ,  comm_schedule_detail.day_end_date_time "
		   + " ,  comm_schedule_delivery.delivery_id ohi_delivery_id "
		   + " ,  comm_schedule_delivery.delivery_status ohi_delivery_status "
		   + " ,  ab_tran.price ohd_price "
		   + " ,  comm_schedule_delivery.fuel_quantity ohd_fuel_quantity_from_pipe "
		   + " ,  comm_schedule_header.schedule_id ohi_schedule_id "
		   + " ,  0 ohi_ins_type "
		   + " ,  parameter.pay_rec ohi_pay_rec "
		   + " ,  comm_schedule_detail.bav_flag ohi_bav_flag "
		   + " ,  comm_schedule_detail.quantity ohd_quantity_from_pipe "
		   + " ,  comm_schedule_header.unit ohi_unit "
		   + " ,  gas_phys_location.zone_id ohi_zone_id "
		   + " ,  comm_schedule_delivery.pipeline_id    ohi_to_pipeline_id "
		   + " ,  0    ohi_from_pipeline_id "
		   + " ,  0    ohi_selectedpipe "
		   + " ,  comm_schedule_delivery.start_date_time "
		   + " ,  comm_schedule_delivery.end_date_time "
		   + " ,  0    ohi_num_days "
		   + " ,  comm_schedule_delivery.service_provider_deal_num ohi_service_provider_deal_num "
 		   + " , gas_phys_location.index_id "
		   + " from "
		   + " gas_phys_location " 
		   + " , comm_schedule_delivery "
		   + " , comm_schedule_header "
		   + " , ab_tran "
		   + " , phys_header "
		   + " , comm_schedule_detail "
		   + " , parameter "
		   + " , gas_phys_connection "
		   + " , gas_phys_pipelines "  
		   + " , query_result "
		+ " where "
		   + " comm_schedule_delivery.delivery_status  <> 8 "
		   + " and parameter.pay_rec  = 1 "
		   + " and ab_tran.current_flag = 1 "
		   + " and gas_phys_location.active = 1 "
		   + " and comm_schedule_delivery.receipt_location_type = 2 "
		   + " and comm_schedule_delivery.service_provider_deal_num IN (1, 7) "
		   + volumeTypeConditionStr
		   + " and gas_phys_location.location_id   			= comm_schedule_delivery.receipt_location_id "
		   + " and comm_schedule_header.delivery_id         = comm_schedule_delivery.delivery_id "
		   + " and comm_schedule_header.schedule_id         = comm_schedule_detail.schedule_id "
		   + " and comm_schedule_header.ins_num             = ab_tran.ins_num "
		   + " and comm_schedule_header.ins_num             = phys_header.ins_num "
		   + " and comm_schedule_header.ins_num             = parameter.ins_num "
		   + " and ab_tran.ins_num                          = phys_header.ins_num "
		   + " and ab_tran.ins_num                          = parameter.ins_num "
		   + " and parameter.ins_num                        = phys_header.ins_num "
		   + " and comm_schedule_header.param_seq_num       = parameter.param_seq_num "
		   + " and gas_phys_connection.connection_id        = gas_phys_location.interconnect_loc_id "
		   + " and comm_schedule_delivery.pipeline_id	     = gas_phys_pipelines.pipeline_id "
		   + ZCGWhereClauseStr
		   + " and query_result.unique_id = "
		   + iQueryID
		   + sqlConditionStr
		   + " UNION ALL "
		   + " select distinct gas_phys_connection.connection_id ohi_connection_id "
		   + " ,  gas_phys_location.pipeline_id ohi_pipeline_id "
		   + " ,  ab_tran.internal_lentity ohi_internal_lentity "
		   + " ,  phys_header.contract_number "
	       + " ,  (select distinct a.external_lentity "
		   + "      from "
		   + "        ab_tran a join "
		   + "        comm_sched_deliv_deal c on "
		   + "        c.deal_num = a.deal_tracking_num "
		   + "      where "
		   + "        a.current_flag = 1 "
		   + "        and c.delivery_id = comm_schedule_delivery.delivery_id "
		   + "        and c.receipt_delivery = parameter.pay_rec "
		   + "        ) ohi_external_lentity "
		   + " ,  comm_schedule_delivery.delivery_location_id ohi_trans_location_id "
	 	   + " ,   (select gas_phys_location.meter_id "
	 	   + "      from "
	 	   + "        gas_phys_location "
	 	   + "      where "
	 	   + "        gas_phys_location.location_id = comm_schedule_delivery.delivery_location_id "
	 	   + "         ) meter_id "
		   + " ,  comm_schedule_header.volume_type ohi_volume_type "
		   + " ,  comm_schedule_delivery.nom_transaction_type ohi_nom_transaction_type "
		   + " ,  gas_phys_location.location_type ohi_location_type "
		   + " ,  gas_phys_location.location_id ohi_location_id "
		   + " ,  comm_schedule_detail.day_start_date_time "
		   + " ,  comm_schedule_detail.day_end_date_time "
		   + " ,  comm_schedule_delivery.delivery_id ohi_delivery_id "
		   + " ,  comm_schedule_delivery.delivery_status ohi_delivery_status "
		   + " ,  ab_tran.price ohd_price "
		   + " ,  comm_schedule_delivery.fuel_quantity ohd_fuel_quantity_from_pipe "
		   + " ,  comm_schedule_header.schedule_id ohi_schedule_id "
		   + " ,  0 ohi_ins_type "
		   + " ,  parameter.pay_rec ohi_pay_rec "
		   + " ,  comm_schedule_detail.bav_flag ohi_bav_flag "
		   + " ,  comm_schedule_detail.quantity ohd_quantity_from_pipe "
		   + " ,  comm_schedule_header.unit ohi_unit "
		   + " ,  gas_phys_location.zone_id ohi_zone_id "
		   + " ,  0    ohi_to_pipeline_id "
		   + " ,  comm_schedule_delivery.pipeline_id   ohi_from_pipeline_id "
		   + " ,  0    ohi_selectedpipe "
		   + " ,  comm_schedule_delivery.start_date_time "
		   + " ,  comm_schedule_delivery.end_date_time "
		   + " ,  0    ohi_num_days "
		   + " ,  comm_schedule_delivery.service_provider_deal_num ohi_service_provider_deal_num "
 		   + " , gas_phys_location.index_id "
		   + " from "
		   + " gas_phys_location " 
		   + " , comm_schedule_delivery "
		   + " , comm_schedule_header "
		   + " , ab_tran "
		   + " , phys_header "
		   + " , comm_schedule_detail "
		   + " , parameter "
		   + " , gas_phys_connection "
		   + " , gas_phys_pipelines "  
		   + " , query_result "
		+ " where "
		+ " comm_schedule_delivery.delivery_status  <> 8 "
		+ " and parameter.pay_rec                        = 0 "
		+ " and ab_tran.current_flag                     = 1 "
		+ " and gas_phys_location.active                 = 1 "
		+ " and comm_schedule_delivery.delivery_location_type = 2 "
		+ " and comm_schedule_delivery.service_provider_deal_num IN (1, 7) "
		+ volumeTypeConditionStr
		+ " and gas_phys_location.location_id            = comm_schedule_delivery.delivery_location_id "
		+ " and comm_schedule_header.delivery_id         = comm_schedule_delivery.delivery_id "
		+ " and comm_schedule_header.schedule_id         = comm_schedule_detail.schedule_id "
		+ " and comm_schedule_header.ins_num             = ab_tran.ins_num "
		+ " and comm_schedule_header.ins_num             = phys_header.ins_num "
		+ " and comm_schedule_header.ins_num             = parameter.ins_num "
		+ " and ab_tran.ins_num                          = phys_header.ins_num "
		+ " and ab_tran.ins_num                          = parameter.ins_num "
		+ " and parameter.ins_num                        = phys_header.ins_num "
		+ " and comm_schedule_header.param_seq_num       = parameter.param_seq_num "
		+ " and gas_phys_connection.connection_id        = gas_phys_location.interconnect_loc_id "
		+ " and comm_schedule_delivery.pipeline_id	     = gas_phys_pipelines.pipeline_id "
		+ ZCGWhereClauseStr 
		+ " and query_result.unique_id = "
		+ iQueryID
		+ sqlConditionStr;
					
      com.olf.openjvs.DBase.runSqlFillTable(sQuery, dataTable);
      return dataTable;
   }

   static Table createOutputTable(Table outputTable, Table workTable) throws OException
   {
       int   col;

       col = outputTable.getColNum( "meter_id");
       col +=1;
       outputTable.insertCol( "days", col,           COL_TYPE_ENUM.COL_INT);
       outputTable.addCol( "daily_quantity_from_pipe",      COL_TYPE_ENUM.COL_DOUBLE);
       outputTable.addCol( "monthly_in_from_pipe",          COL_TYPE_ENUM.COL_DOUBLE);
       outputTable.addCol( "monthly_out_from_pipe",         COL_TYPE_ENUM.COL_DOUBLE);
       col = outputTable.getColNum( "days");
       col +=1;
       outputTable.insertCol( "month_end_balance_from_pipe", col,   COL_TYPE_ENUM.COL_DOUBLE);

       workTable.addCol( "days", COL_TYPE_ENUM.COL_INT);
       workTable.addCol( "daily_quantity_from_pipe",      COL_TYPE_ENUM.COL_DOUBLE);
       workTable.addCol( "monthly_in_from_pipe",          COL_TYPE_ENUM.COL_DOUBLE);
       workTable.addCol( "monthly_out_from_pipe",         COL_TYPE_ENUM.COL_DOUBLE);
       workTable.addCol( "month_end_balance_from_pipe",   COL_TYPE_ENUM.COL_DOUBLE);
	   
	   outputTable.addCol( "day_start_date",      COL_TYPE_ENUM.COL_INT);
	   outputTable.addCol( "day_start_time",      COL_TYPE_ENUM.COL_INT);
	   outputTable.addCol( "day_end_date",      COL_TYPE_ENUM.COL_INT);
	   outputTable.addCol( "day_end_time",      COL_TYPE_ENUM.COL_INT);
	   outputTable.colHide("day_start_date");
	   outputTable.colHide("day_start_time");
	   outputTable.colHide("day_end_date");
	   outputTable.colHide("day_end_time");

      return outputTable;
   }
				
   private static void formatResult(Table outputTable) throws OException
   {
       int      col = outputTable.getColNum( "connection_id" );

       outputTable.setColTitle( col, "Interconnect");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.GAS_PHYS_CONNECTION_TABLE);

       col = outputTable.getColNum( "connection_name" );
       outputTable.setColTitle( col, "Connection Name");

       col = outputTable.getColNum( "contract_number" );
       outputTable.setColTitle( col, "Contract");

       col = outputTable.getColNum( "location_id" );
       outputTable.setColTitle( col, "IC Location");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);

       col = outputTable.getColNum( "meter_id" );
       outputTable.setColTitle( col, "Meter");

       col = outputTable.getColNum( "day_start_date_time" );
       outputTable.setColTitle( col, "Start DateTime");
       outputTable.setColFormatAsDateDayStartTime( col, DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT, 0);

       col = outputTable.getColNum( "day_end_date_time" );
       outputTable.setColTitle( col, "End DateTime");
       outputTable.setColFormatAsDateDayStartTimeEnd( col, DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT, 0);

       col = outputTable.getColNum( "delivery_id" );
       outputTable.setColTitle( col, "Delivery ID");

       col = outputTable.getColNum( "delivery_status" );
       outputTable.setColTitle( col, "Status");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.DELIVERY_STATUS_TABLE);

       col = outputTable.getColNum( "available_quantity" );
       outputTable.setColTitle( col, "Available Quantity");
       outputTable.setColFormatAsNotnlAcct( col, 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

       col = outputTable.getColNum( "price" );
       outputTable.setColTitle( col, "Rate");
       outputTable.setColFormatAsNotnlAcct( col, 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);

       col = outputTable.getColNum( "pipeline_id" );
       outputTable.setColTitle( col, "Pipe");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);

       col = outputTable.getColNum( "fuel_quantity_from_pipe" );
       outputTable.setColTitle( col, "Fuel");
       outputTable.setColFormatAsNotnlAcct( col, 12, 0, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 1);

       col = outputTable.getColNum( "nom_transaction_type" );
       outputTable.setColTitle( col, "Trans Type");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.NOM_TRAN_TYPE_TABLE );

       col = outputTable.getColNum( "schedule_id" );
       outputTable.setColTitle( col, "Schedule ID");

       col = outputTable.getColNum( "trans_location_id" );
       outputTable.setColTitle( col, "Location");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);

       col = outputTable.getColNum( "ins_type" );
       outputTable.setColTitle( col, "Ins. Type");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);

       col = outputTable.getColNum( "pay_rec" );
       outputTable.setColTitle( col, "Pay / Rec");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.REC_PAY_TABLE);

       col = outputTable.getColNum( "volume_type" );
       outputTable.setColTitle( col, "Volume Type");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.VOLUME_TYPE_TABLE );

       col = outputTable.getColNum( "bav_flag" );
       outputTable.setColTitle( col, "BAV Flag");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.YES_NO_TABLE);

       col = outputTable.getColNum( "quantity_from_pipe" );
       outputTable.setColTitle( col, "Hourly Quantity");
       outputTable.setColFormatAsNotnlAcct( col, 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

       col = outputTable.getColNum( "unit" );
       outputTable.setColTitle( col, "Units");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE );

       col = outputTable.getColNum( "service_type" );
       outputTable.setColTitle( col, "Service Type");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE );

       col = outputTable.getColNum( "deal_tracking_num" );
       outputTable.setColTitle( col, "Deal Tracking Num");

       col = outputTable.getColNum( "external_lentity" );
       outputTable.setColTitle( col, "Counterparty");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.PARTY_TABLE );

       col = outputTable.getColNum( "internal_lentity" );
       outputTable.setColTitle( col, "Internal LE");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.PARTY_TABLE );

       col = outputTable.getColNum( "zone_id" );
       outputTable.setColTitle( col, "Zone");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.GAS_PHYS_ZONE_TABLE );

       col = outputTable.getColNum( "location_type" );
       outputTable.setColTitle( col, "Location Type");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.GAS_PHYS_LOC_TYPE );

       col = outputTable.getColNum( "num_days" );
       outputTable.setColTitle( col, "Number of Days");

       col = outputTable.getColNum( "to_pipeline_id" );
       outputTable.setColTitle( col, "To Pipeline");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE );

       col = outputTable.getColNum( "from_pipeline_id" );
       outputTable.setColTitle( col, "From Pipeline");
       outputTable.setColFormatAsRef( col, SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE );

       col = outputTable.getColNum( "selectedpipe");
       outputTable.setColTitle( col, "UnusedSelection");

       col = outputTable.getColNum( "start_date_time");
       outputTable.setColTitle( col, "Delivery\nStart Date");
       outputTable.setColFormatAsDateDayStartTime( col, DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT, 0);

       col = outputTable.getColNum( "end_date_time");
       outputTable.setColTitle( col, "Delivery\nEnd Date");
       outputTable.setColFormatAsDateDayStartTimeEnd( col, DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT, 0);

       col = outputTable.getColNum( "service_provider_deal_num");
       outputTable.setColTitle( col, "Service Provider\nDeal Num");
       
       col = outputTable.getColNum( "service_provider_deal_num");
       outputTable.setColTitle( col, "Service Provider\nDeal Num");
       
       col = outputTable.getColNum( "daily_quantity_from_pipe");
       outputTable.setColTitle( col, "Daily Quantity From Pipe");
       outputTable.setColFormatAsNotnlAcct( col, 12, 0, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
       
       col = outputTable.getColNum( "monthly_in_from_pipe");
       outputTable.setColTitle( col, "Monthly In From Pipe");
       outputTable.setColFormatAsNotnlAcct( col, 12, 0, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
       
       col = outputTable.getColNum( "monthly_out_from_pipe");
       outputTable.setColTitle( col, "Monthly Out From Pipe");
       outputTable.setColFormatAsNotnlAcct( col, 12, 0, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
       
       col = outputTable.getColNum( "month_end_balance_from_pipe");
       outputTable.setColTitle( col, "Balance From Pipe");
       outputTable.setColFormatAsNotnlAcct( col, 12, 0, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
       
       col = outputTable.getColNum( "days");
       outputTable.setColTitle( col, "Days");
       
       col = outputTable.getColNum( "day_start_date");
       outputTable.setColTitle( col, "Day Start Date");
       outputTable.colHide(col);
       
       col = outputTable.getColNum( "day_start_time");
       outputTable.setColTitle( col, "Day Start Time");
       outputTable.colHide(col);
       
       col = outputTable.getColNum( "day_end_date");
       outputTable.setColTitle( col, "Day End Date");
       outputTable.colHide(col);
       
       col = outputTable.getColNum( "day_end_time");
       outputTable.setColTitle( col, "Day End Time");
       outputTable.colHide(col);
       
       col = outputTable.getColNum( "month_end_balance_to_pipe");
       outputTable.setColTitle( col, "Balance To Pipe");
       
       col = outputTable.getColNum( "daily_quantity_to_pipe");
       outputTable.setColTitle( col, "Daily Quantity To Pipe");
       
       col = outputTable.getColNum( "monthly_in_to_pipe");
       outputTable.setColTitle( col, "Monthly In To Pipe");
       
       col = outputTable.getColNum( "monthly_out_to_pipe");
       outputTable.setColTitle( col, "Monthly Out To Pipe");
       
       col = outputTable.getColNum( "fuel_quantity_to_pipe");
       outputTable.setColTitle( col, "Fuel Quantity To Pipe");
       
       col = outputTable.getColNum( "quantity_to_pipe");
       outputTable.setColTitle( col, "Quantity To Pipe");
       
	   APMUtility.formatEnergyConversion(outputTable, massColArray, volumeColArray, energyColArray);
   }
   
   static double calcDailyVolume(int curDate, int start_date, int start_sec, int end_date, int end_sec, double hour_vol) throws OException
   {
      double        b_sec = 0;
      double        e_sec = 86400.0; /* 86400 seconds in a full day */

      /* Handle partial days */
      if (curDate == start_date)
         b_sec = start_sec;
      if (curDate == end_date)
         e_sec = end_sec;

      return (hour_vol * (e_sec - b_sec)/3600.0); /* 3600 seconds in a full hour */
   }
   
   static void setTableDateQty(Table t, int row, int date, double qty, int pay_rec) throws OException//volume
   {
      t.setInt( "days",           row, date);//set days only once in first setTableDateXXX function
      t.setDouble( "daily_quantity_from_pipe", row, qty);
      if (pay_rec == 0)
      { /* Receive */
         t.setDouble( "monthly_in_from_pipe",   row,  qty);
         t.setDouble( "month_end_balance_from_pipe",   row,  qty);
      }
      else
      { /* Pay */
         t.setDouble( "monthly_out_from_pipe", row,  qty);
         t.setDouble( "month_end_balance_from_pipe",   row, -qty);
      }
   }

   static void expandCSDVolumeToDailyVolumes(Table outputTable, Table workTable, int row, int start_date_colnum, int end_date_colnum, 
		   									int hourly_vol_colnum_from_pipe, 
		   									int pay_rec_colnum, int first_date, int last_date, 
		   									int units_colnum, int location_colnum) throws OException
   {
                  /* Get the values from the Table */
	  int         start_date = outputTable.getDate( start_date_colnum, row);
	  int         start_sec  = outputTable.getTime( start_date_colnum, row);
	  int         end_date   = outputTable.getDate( end_date_colnum,   row);
	  int         end_sec    = outputTable.getTime( end_date_colnum,   row);
	  double      hour_vol;
	  int         pay_rec    = outputTable.getInt( pay_rec_colnum,    row);
	  int         curDate, num_days, row_ct = 0;
	  double      day_vol_unconverted = 0;
	  int         time_diff;
	  double day_vol_unconverted_previous = 0;
	  double day_vol_unconverted_sum = 0;
	  Table conversionTable;
	  int 		  iFromPipelineUnit = Pipeline.getUnit(outputTable.getInt("from_pipeline_id", row));
	  int 		  iToPipelineUnit = Pipeline.getUnit(outputTable.getInt("to_pipeline_id", row));
	  int		  units		 = outputTable.getInt( units_colnum,    row);
	  int 		  convFactorColNum;
	  int		  location = -1;
	  double 	  dMassConvFactor = 0;
	  double	  dMassConvFactorToPipe = 0;
	  
	  if(iFromPipelineUnit != units)
	  {
		  /*When a nomination is created the unit used to nominate is saved to
		  **comm_schedule_detail regardless of it matching the unit on the pipeline
		  **In order to display correctly in APM we must convert back to the unit
		  **of the pipeline for any nominations whose unit does not match the pipeline
		  */
		  location		 	= outputTable.getInt( location_colnum,    row);
		  conversionTable 	= Transaction.utilGetEnergyConversionFactors(start_date, end_date, location, units, iFromPipelineUnit);
		  convFactorColNum 	= conversionTable.getColNum("Conv Factor");
		  dMassConvFactor 	= conversionTable.getDouble(convFactorColNum,1);
		  outputTable.setDouble(hourly_vol_colnum_from_pipe, row, outputTable.getDouble( hourly_vol_colnum_from_pipe, row) * dMassConvFactor);
	  }
	  
	  location		 	= outputTable.getInt( location_colnum,    row);
	  conversionTable 	= Transaction.utilGetEnergyConversionFactors(start_date, end_date, location, units, iToPipelineUnit);
	  convFactorColNum 	= conversionTable.getColNum("Conv Factor");
	  dMassConvFactorToPipe	= conversionTable.getDouble(convFactorColNum,1);
	  
	  outputTable.setInt("units_col_from_pipe", row, iFromPipelineUnit);
	  outputTable.setInt("units_col_to_pipe", row, iToPipelineUnit);
	
	  /* Restrict the range of dates to be within first and last */
	  if (first_date > start_date)
	  {
	     start_date = first_date;
	     start_sec  = 0;
	  }
	  if ((last_date > 0) && (last_date < end_date))
	  {
	     end_date   = last_date + 1;
	     end_sec    = 0;
	  }
	
	  /* Check for negative date range */
	  time_diff     = end_date - start_date;
	  if (time_diff == 0)
	  {
	     time_diff  = end_sec - start_sec;
	  }
	  if (time_diff < 0)
	  {
	     int tmp_date = start_date;
	     int tmp_sec  = start_sec;
	
	     start_date   = end_date;
	     end_date     = tmp_date;
	
	     start_sec    = end_sec;
	     end_sec      = tmp_sec;
	  }
	  else if (time_diff == 0)
	  {
	     return;  /* 0 time, therefore nothing to do */
	  }
	
	  num_days = end_date - start_date;
	  /* Check for partial last day */
	  if (end_sec > 0)
		  ++num_days;
	  
	  /* Make space in workTable */
	  if(num_days != 0)
	  {
		 if (end_sec == 0)
		 {
		    --end_date;
		    end_sec = 86400;
		 }
		 hour_vol   = outputTable.getDouble( hourly_vol_colnum_from_pipe, row);
	     for (curDate = start_date, row_ct = 0; curDate <= end_date; curDate++)
	     {
	    	day_vol_unconverted = calcDailyVolume (curDate, start_date, start_sec, end_date, end_sec, hour_vol);
	        
			if(day_vol_unconverted != day_vol_unconverted_previous || row_ct == 0/*first time through*/)
	    	{
				workTable.addRow();
	    		++row_ct;
	    		
	    		day_vol_unconverted_sum = day_vol_unconverted;
	    		day_vol_unconverted_previous = day_vol_unconverted;
	    	}
	    	else
	    	{
	    		day_vol_unconverted_sum = day_vol_unconverted_sum + day_vol_unconverted;
	    	}
        	setTableDateQty (workTable, row_ct, curDate, day_vol_unconverted_sum, pay_rec);
	     }
	 
	     for(int tmp_t_row = 1; tmp_t_row<= row_ct;  tmp_t_row++)
	     {
	    	 if(tmp_t_row > 1)
	    	 {
	    		 int inputRow = row;//temporary variable because insert row after not working
	    		 //row = outputTable.insertRowAfter(row);
	    		 row = outputTable.addRow();
	    		 outputTable.copyRow(inputRow, outputTable, row);
	    	 }
	    	 
	    	 outputTable.setDouble("daily_quantity_from_pipe", row, workTable.getDouble("daily_quantity_from_pipe", tmp_t_row));
	    	 outputTable.setDouble("monthly_in_from_pipe", row, workTable.getDouble("monthly_in_from_pipe", tmp_t_row));
	    	 outputTable.setDouble("monthly_out_from_pipe", row, workTable.getDouble("monthly_out_from_pipe", tmp_t_row));
	    	 outputTable.setDouble("month_end_balance_from_pipe", row, workTable.getDouble("month_end_balance_from_pipe", tmp_t_row));
	    	 outputTable.setInt("days", row, workTable.getInt("days", tmp_t_row));

	    	 /* to pipe values*/
   		  	 outputTable.setDouble("quantity_to_pipe", row, outputTable.getDouble("quantity_from_pipe", row) * dMassConvFactorToPipe);
   		  	 outputTable.setDouble("fuel_quantity_to_pipe", row, outputTable.getDouble("fuel_quantity_from_pipe", row) * dMassConvFactorToPipe);
   		  	 
   		  
   		  	 outputTable.setDouble("daily_quantity_to_pipe", row, workTable.getDouble("daily_quantity_from_pipe", tmp_t_row) * dMassConvFactorToPipe);
	   		 outputTable.setDouble("monthly_in_to_pipe", row, workTable.getDouble("monthly_in_from_pipe", tmp_t_row) * dMassConvFactorToPipe);
	   		 outputTable.setDouble("monthly_out_to_pipe", row, workTable.getDouble("monthly_out_from_pipe", tmp_t_row) * dMassConvFactorToPipe);
	   		 outputTable.setDouble("month_end_balance_to_pipe", row, workTable.getDouble("month_end_balance_from_pipe", tmp_t_row) * dMassConvFactorToPipe);
	   		 outputTable.setInt("days", row, workTable.getInt("days", tmp_t_row));
	     }
	     workTable.clearRows();
	     outputTable.setInt( "num_days", row, num_days);
	  }


	  
	  return;
   }
   
   private static Table expandDailyVolume(Table outputTable, int start_date, int end_date) throws OException
   {
      int         start_dt_colnum   = 0;
      int         end_dt_colnum     = 0;
      int         hourly_vol_colnum_from_pipe = 0;
      int		  hourly_vol_colnum_to_pipe = 0;
      int         pay_rec_colnum    = 0;
      int         units_colnum      = 0;
      int         location_colnum   = 0;
      int         spdn_colnum       = 0;
      int         contract_colnum   = 0;
      Table    workTable             = Table.tableNew        ("Temp Table");
      int         num_rows;
      int         cur_row;
      String strTargetCol, strSourceCol;
      String   strICCol= "location_id";
      String   strOutColName = "";
      String   strSelColFilter = "";
      Table tblSelectedPipes = Util.NULL_TABLE;
      int      intFilterPipes = 0;
      
      outputTable = createOutputTable(outputTable, workTable);
      
      //  add the other side of the interconnect pipeline
      strTargetCol = "from_pipeline_id";
      strSourceCol = "to_pipeline_id";
      MASTER_AddOtherPipe(outputTable, strSourceCol, strTargetCol, strICCol);

      strTargetCol = "to_pipeline_id";
      strSourceCol = "from_pipeline_id";
      MASTER_AddOtherPipe(outputTable, strSourceCol, strTargetCol, strICCol);

      strOutColName   = "selectedpipe";
      strSelColFilter = "id";
      MASTER_FilterPipes(outputTable, tblSelectedPipes, strOutColName, strSelColFilter, intFilterPipes);

      start_dt_colnum   = outputTable.getColNum( "day_start_date_time");
      end_dt_colnum     = outputTable.getColNum( "day_end_date_time");
      hourly_vol_colnum_from_pipe = outputTable.getColNum( "quantity_from_pipe");
      pay_rec_colnum    = outputTable.getColNum( "pay_rec");
      units_colnum      = outputTable.getColNum( "unit");
      location_colnum   = outputTable.getColNum( "trans_location_id");
      spdn_colnum       = outputTable.getColNum( "service_provider_deal_num");
      contract_colnum   = outputTable.getColNum( "contract_number");

      /* Verify required columns */
      if (start_dt_colnum < 0 || end_dt_colnum < 0 || hourly_vol_colnum_from_pipe < 0 ||pay_rec_colnum < 0)
         return outputTable;
      
      num_rows = outputTable.getNumRows();
      
      if(outputTable.getColNum("month_end_balance_to_pipe") < 0)
      {
    	  outputTable.addCol("month_end_balance_to_pipe",COL_TYPE_ENUM.COL_DOUBLE);
    	  outputTable.addCol("daily_quantity_to_pipe",COL_TYPE_ENUM.COL_DOUBLE);
    	  outputTable.addCol("monthly_in_to_pipe",COL_TYPE_ENUM.COL_DOUBLE);
    	  outputTable.addCol("monthly_out_to_pipe",COL_TYPE_ENUM.COL_DOUBLE);
    	  outputTable.addCol("fuel_quantity_to_pipe",COL_TYPE_ENUM.COL_DOUBLE);
    	  outputTable.addCol("quantity_to_pipe",COL_TYPE_ENUM.COL_DOUBLE);
    	  
    	  outputTable.addCol("units_col_from_pipe",COL_TYPE_ENUM.COL_INT);
    	  outputTable.addCol("units_col_to_pipe",COL_TYPE_ENUM.COL_INT);
      }
      
      for (cur_row = 1; cur_row <= num_rows; cur_row++)
      {
         int service_provider_deal_num = outputTable.getInt(spdn_colnum, cur_row);
         int iEndDate = outputTable.getDate("day_end_date_time", cur_row);
         int iEndTime = outputTable.getTime("day_end_date_time", cur_row);
         outputTable.setInt("day_start_date", cur_row, outputTable.getDate("day_start_date_time", cur_row));
         outputTable.setInt("day_start_time", cur_row, outputTable.getTime("day_start_date_time", cur_row));
         if (iEndTime == 0) 
         {
				iEndDate--;
				iEndTime = 86400;
		}
         outputTable.setInt("day_end_date", cur_row, iEndDate);
         outputTable.setInt("day_end_time", cur_row, iEndTime);

         expandCSDVolumeToDailyVolumes (outputTable, workTable, cur_row, start_dt_colnum, end_dt_colnum, 
        		 						 hourly_vol_colnum_from_pipe,
        		 						pay_rec_colnum, start_date, end_date,units_colnum, location_colnum);

         if (service_provider_deal_num < NOM_SYSTEM_TYPE.NOM_SYSTEM_TRAN_ANY_VALID_TRAN.toInt()) 
         {
        	 outputTable.setString(contract_colnum, cur_row, Ref.getShortName(SHM_USR_TABLES_ENUM.SYSTEM_TRAN_TABLE, service_provider_deal_num) );
         }
      }

      workTable.destroy();
      return outputTable;
   }
   
   private static void MASTER_AddOtherPipe(Table tblOutput, String strSourceCol, String strTargetCol, String strICCol) throws OException  
   {
       int numRows = tblOutput.getNumRows();
       int locationID = 0;
       int otherLocationID = 0;
       int sourcePipeID = -1;
	   int targetPipeID = -1;

       for(int count = 1; count <= numRows; count++)
       {
    	   sourcePipeID = tblOutput.getInt(strSourceCol,count);
    	   targetPipeID = tblOutput.getInt(strTargetCol,count);
    	   locationID = tblOutput.getInt(strICCol,count);
    	   if(sourcePipeID > 0 && targetPipeID == 0)
    	   {
    		   otherLocationID = Pipeline.getPipelineAcrossInterconnect(locationID);
    		   tblOutput.setInt(strTargetCol, count, otherLocationID);
    	   }
       }
       return;
   }
   private static void MASTER_FilterPipes(Table tblOut, Table tblSelPipes, String strOutColName, String strSelColFilter, int intFilterPipes)  throws OException
   {
       String   strWhat = null;
       String   strWhere = null;
       String   strToPipelineCol = "to_pipeline_id";
       String   strFromPipelineCol = "from_pipeline_id";

       // do the filtering
       if (intFilterPipes == 1) {
           // copy the pipeline id to the target column
           strWhat = strSelColFilter + "(" + strOutColName + ")";
           strWhere = strSelColFilter + " EQ $" + strToPipelineCol;
           tblOut.select( tblSelPipes, strWhat, strWhere);
           strWhere = strSelColFilter + " EQ $" + strFromPipelineCol;
           tblOut.select( tblSelPipes, strWhat, strWhere);
       } else {
           // populate the entire column with a non-zero value so we get all
           // rows back
           tblOut.setColValInt( strOutColName, 1);
       }
   }
}
