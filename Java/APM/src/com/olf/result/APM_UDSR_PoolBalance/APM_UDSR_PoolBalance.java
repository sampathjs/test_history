/* Released with version 19-Mar-2018_V17_0_4 of APM */

/*
File Name:                      APM_UDSR_PoolBalance.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Joe Gallagher
Creation Date:                  March 14, 2014
 
Revision History:
												
Script Type:                    User-defined simulation result

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    The Pool Balance package presents Monthly Into Pool Quantity,
 								Monthly Out of Pool Quantity and the Daily Quantity in APM 
 								to allow viewing of commodity pools.  This document covers 
 								the installation of the package, a description of how to 
 								use it and some example screens. Note that this package 
 								is only available in V15 or later.

*/ 

package com.olf.result.APM_UDSR_PoolBalance;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;


import com.olf.result.APMUtility.*;

import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Pipeline;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import java.util.Vector;

public class APM_UDSR_PoolBalance implements IScript
{
   static String massColArray[] = {};
   
   static String volumeColArray[] = {"quantity", 			"quantity_volume",				"Hourly Quantity\nVolume",
		   					  		 "daily_quantity", 		"daily_quantity_volume",		"Daily Quantity\nVolume",
		   					  		 "monthly_into_pool", 	"monthly_into_pool_volume",		"Monthly Into Pool\nVolume",
		   					  		 "monthly_out_of_pool", "monthly_out_of_pool_volume",	"Monthly Out Of Pool\nVolume",
		   					  		 "monthly_end_balance",	"monthly_end_balance_volume",	"Month End Balance\nVolume"};
   
   static String energyColArray[] = {"quantity", 			"quantity_energy",				"Hourly Quantity\nEnergy",
	  		 						 "daily_quantity", 		"daily_quantity_energy",		"Daily Quantity\nEnergy",
	  		 						 "monthly_into_pool", 	"monthly_into_pool_energy",		"Monthly Into Pool\nEnergy",
	  		 						 "monthly_out_of_pool", "monthly_out_of_pool_energy", 	"Monthly Out Of Pool\nEnergy",
	  		 						 "monthly_end_balance",	"monthly_end_balance_energy",	"Month End Balance\nEnergy"};
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
	   int massUnit = -1, volumeUnit = -1, energyUnit = -1;
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
	      
      if (APMUtility.ParamHasValue(tblConfig, "APM Pool Balance Start Date") > 0){
    	  startPoint = APMUtility.GetParamDateValue(tblConfig, "APM Pool Balance Start Date");}

      if (APMUtility.ParamHasValue(tblConfig, "APM Pool Balance End Date") > 0){
    	  endPoint = APMUtility.GetParamDateValue(tblConfig, "APM Pool Balance End Date");}

      if (APMUtility.ParamHasValue(tblConfig, "APM Pool Balance Volume Types") > 0){
    	  volumeTypesStr = APMUtility.GetParamStrValue(tblConfig, "APM Pool Balance Volume Types");}

      if (APMUtility.ParamHasValue(tblConfig, "APM Pool Balance Volume Unit") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM Pool Balance Volume Unit");
    	  if (!strVal.isEmpty())
    	  {
	    volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);
    	  }
      }

      if (APMUtility.ParamHasValue(tblConfig, "APM Pool Balance Energy Unit") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM Pool Balance Energy Unit");
    	  if (!strVal.isEmpty())
    	  {
	    energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);
    	  }
      }
      
      // Set default mass and unit values, if not specified
      if (volumeUnit < 0)
      {
    	  volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MCF");
      }
      if (energyUnit < 0)
      {
    	  energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MMBTU");
      }       
      
      if (APMUtility.ParamHasValue(tblConfig, "APM Pool Balance Show All Volume Statuses") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM Pool Balance Show All Volume Statuses");
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
		  
	 if ( tblData.getNumRows() > 0 )
	 {
    	  returnt.select(tblData, "*", "service_provider_deal_num GE 0");
	 }
	 unitsCol = tblData.getColNum("unit");
	 deal_start_date_col = tblData.getColNum("day_start_date_time");
	 deal_maturity_date_col = tblData.getColNum("day_end_date_time");
	 location_id_col = tblData.getColNum("location_id");
	 APMUtility.fillInUnits(returnt, volumeUnit, energyUnit);
 	 doEnergyConversion(returnt, unitsCol, -1/*massUnit*/, volumeUnit, energyUnit, deal_start_date_col, deal_maturity_date_col, location_id_col, tblTrans);

         if (apmServiceType != 1) {
            Query.clear(iQueryID);
         }
      }
   }

   private static Table doEnergyConversion(Table outputTable, int unit_col, 
		   int massUnit, int volumeUnit, int energyUnit, int deal_start_date_col, 
		   int deal_maturity_date_col, int location_id_col, Table tranTable) throws OException
	{
	    int dataTableDealNumCol = outputTable.getColNum("service_provider_deal_num");
	    int deliveryIdCol = outputTable.getColNum("delivery_id");
	    int sideCol =  outputTable.getColNum("param_seq_num");
	    int profileSeqNumCol =  outputTable.getColNum("profile_seq_num");
	    int tranTableDealNumCol = tranTable.getColNum ("deal_num");
	    
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
								"deal_tracking_num");
	
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
      String fromRecClauseStr = "";
      String fromDelClauseStr = "";
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
    				  + " ab_tran.start_date <= '" + endPointStr + "' "
    				  + " and comm_schedule_delivery.start_date_time <= '" + endPointStr + "' "
    				  + " and comm_schedule_header.day_start_date_time <= '" + endPointStr + "' "
    				  + " and comm_schedule_detail.day_start_date_time <= '" + endPointStr + "' "
    				  + " ) ";
    	  }
    	  
    	  dateConditionStr = dateConditionStr + " ) ";
      }

      String volumeTypeConditionStr = "";
      int numVolumeTypeRows = volumeTypesTable.getNumRows();
      
      if (numVolumeTypeRows > 0)
      {
    	  // If there's just one volume type then use '=' expression.
    	  if (numVolumeTypeRows == 1)
    	  {
    		  volumeTypeConditionStr = volumeTypeConditionStr + " and comm_schedule_header.volume_type = " + volumeTypesTable.getInt(1, 1) + " ";
    	  }
    	  // For more than one volume type use the 'in' expression.
    	  else
    	  {
    		  volumeTypeConditionStr = volumeTypeConditionStr + " and comm_schedule_header.volume_type in (";
    		  
		      for (int vtRow = 1; vtRow <= numVolumeTypeRows; vtRow++)
		      {
		    	  volumeTypeConditionStr = volumeTypeConditionStr + volumeTypesTable.getInt(1, vtRow);
		    	  if (vtRow < numVolumeTypeRows)
		    		  volumeTypeConditionStr = volumeTypeConditionStr + ", ";
		      }
		      
		      volumeTypeConditionStr = volumeTypeConditionStr + ") "; 
    	  }
      }
      
      String bavConditionStr = "";
      if (showAllVolumeStatuses == 0)
      {
    	  // Show only the BAV volumes.
    	  bavConditionStr = " and comm_schedule_detail.bav_flag = 1 ";
      }
      
      String sqlConditionStr = dateConditionStr + volumeTypeConditionStr + bavConditionStr;
      
      String ZCGWhereClauseStr = "";
      if (apmServiceType == 1)
	  {
    	  // Nomination
		   
		  fromRecClauseStr = " query_result join "
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
			   		    + " gas_phys_pipelines on "
			   		    + "    gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id ";
		  
		  fromDelClauseStr = " query_result join "
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
		   		    + " gas_phys_pipelines on "
		   		    + "    gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id ";
		  //Special logic for zero cost gathering
		  ZCGWhereClauseStr = " and comm_schedule_delivery.delivery_id = query_result.query_result ";
	  }
	  else
	  {
		  // Deal
		   
		  fromRecClauseStr = " query_result join "
			   		    + " ab_tran on "
			   		    + "    ab_tran.tran_num = query_result.query_result join "
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
			   		    + " gas_phys_pipelines on "
			   		    + "    gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id ";
		  
		  fromDelClauseStr = " query_result join "
		   		    + " ab_tran on "
		   		    + "    ab_tran.tran_num = query_result.query_result join "
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
		   		    + " gas_phys_pipelines on "
		   		    + "    gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id ";
		  //Special logic for zero cost gathering
		  ZCGWhereClauseStr = " and ab_tran.tran_num = query_result.query_result ";
	  }
      
	  String sQuery = "  select distinct "
	  		    + "         gas_phys_location.location_id ohi_location_id, "
	  		    + "         phys_header.contract_number, "
	  		    + "         ab_tran.internal_lentity ohi_internal_lentity, " 
	  		    + "         ("
	  		    + "            select distinct "
	  		    + "               a.external_lentity "
	  		    + "            from "
	  		    + "               ab_tran a join "
	  		    + "               comm_sched_deliv_deal c on "
	  		    + "                  c.deal_num = a.deal_tracking_num "
	  		    + "            where "
	  		    + "               a.current_flag = 1 "
	  		    + "               and c.delivery_id = comm_schedule_delivery.delivery_id "
	  		    + "               and c.receipt_delivery = parameter.pay_rec "
	  		    + "         ) ohi_external_lentity, "
	  		    + "         comm_schedule_delivery.delivery_location_id ohi_trans_location_id, "
	  		    + "         ("
	  		    + "            select "
	  		    + "               gas_phys_location.meter_id "
	  		    + "            from "
	  		    + "               gas_phys_location "
	  		    + "            where "
	  		    + "               gas_phys_location.location_id = comm_schedule_delivery.delivery_location_id "
	  		    + "         ) meter_id, "
	  		    + "         comm_schedule_header.volume_type ohi_volume_type, "
	  		    + "         comm_schedule_delivery.nom_transaction_type ohi_nom_transaction_type, "
	  		    + "         comm_schedule_detail.day_start_date_time, "
	  		    + "         comm_schedule_detail.day_end_date_time, "
	  		    + "         comm_schedule_delivery.delivery_id ohi_delivery_id, "
	  		    + "         comm_schedule_delivery.delivery_status ohi_delivery_status, "
	  		    + "         gas_phys_location.pipeline_id ohi_pipeline_id, "
	  		    + "         comm_schedule_detail.fuel_quantity ohd_fuel_quantity, "
	  		    + "         comm_schedule_header.schedule_id ohi_schedule_id, "
	  		    + "         parameter.pay_rec ohi_pay_rec, "
	  		    + "         comm_schedule_detail.bav_flag ohi_bav_flag, "
	  		    + "         comm_schedule_detail.quantity ohd_quantity, "
	  		    + "         comm_schedule_header.unit ohi_unit, "
	  		    + "         comm_schedule_header.param_seq_num ohi_param_seq_num, "
	  		    + "         0 ohi_num_days, "
	  		    + "         gas_phys_pipelines.region_id ohi_region_id, "
	  		    + "         comm_schedule_delivery.start_date_time, "
	  		    + "         comm_schedule_delivery.end_date_time, "
	  		    + "         comm_schedule_delivery.service_provider_deal_num ohi_service_provider_deal_num, "
	  		    + "    	gas_phys_location.index_id "
	  		    + "      from"
	  		    +           fromRecClauseStr
	  		    + "      where"
	  		    + "         query_result.unique_id = " + iQueryID
	  		    + "         and ( "
	  		    + "            ab_tran.ins_type in (48020, 48030) "
	  		    + "            or ab_tran.ins_type in "
	  		    + "               (select id_number from instruments where base_ins_id in (48020, 48030))"
	  		    + "         )"
	  		    + "         and comm_schedule_delivery.delivery_status   <> 8"
	  		    + "         and parameter.pay_rec                        = 1"
	  		    + "         and ab_tran.current_flag                     = 1"
	  		    + "         and gas_phys_location.active                 = 1"
	  		    + "         and comm_schedule_delivery.receipt_location_type = 3"
	  		    +           sqlConditionStr
	  		    + " UNION ALL"
	  		    + "      select"
	  		    + "         gas_phys_location.location_id ohi_location_id, "
	  		    + "         phys_header.contract_number, "
	  		    + "         ab_tran.internal_lentity ohi_internal_lentity, "
	  		    + "         ("
	  		    + "            select distinct "
	  		    + "               a.external_lentity "
	  		    + "            from "
	  		    + "               ab_tran a join "
	  		    + "               comm_sched_deliv_deal c on "
	  		    + "                  c.deal_num = a.deal_tracking_num "
	  		    + "            where"
	  		    + "               a.current_flag = 1 "
	  		    + "               and c.delivery_id = comm_schedule_delivery.delivery_id"
	  		    + "               and c.receipt_delivery = parameter.pay_rec "
	  		    + "         ) ohi_external_lentity, "
	  		    + "         comm_schedule_delivery.receipt_location_id ohi_trans_location_id, "
	  		    + "         ("
	  		    + "            select "
	  		    + "               gas_phys_location.meter_id" 
	  		    + "            from "
	  		    + "               gas_phys_location"
	  		    + "            where "
	  		    + "               gas_phys_location.location_id = comm_schedule_delivery.receipt_location_id "
	  		    + "         ) meter_id, "
	  		    + "         comm_schedule_header.volume_type ohi_volume_type, "
	  		    + "         comm_schedule_delivery.nom_transaction_type ohi_nom_transaction_type, "
	  		    + "         comm_schedule_detail.day_start_date_time, "
	  		    + "         comm_schedule_detail.day_end_date_time, "
	  		    + "         comm_schedule_delivery.delivery_id ohi_delivery_id, "
	  		    + "         comm_schedule_delivery.delivery_status ohi_delivery_status, "
	  		    + "         gas_phys_location.pipeline_id ohi_pipeline_id, "
	  		    + "         (comm_schedule_detail.fuel_quantity) ohd_fuel_quantity, "
	  		    + "         comm_schedule_header.schedule_id ohi_schedule_id, "
	  		    + "         parameter.pay_rec ohi_pay_rec, "
	  		    + "         comm_schedule_detail.bav_flag ohi_bav_flag, "
	  		    + "         comm_schedule_detail.quantity ohd_quantity, "
	  		    + "         comm_schedule_header.unit ohi_unit, "
    	  	    + "         comm_schedule_header.param_seq_num ohi_param_seq_num, "
	  		    + "         0 ohi_num_days, "
	  		    + "         gas_phys_pipelines.region_id ohi_region_id, "
	  		    + "         comm_schedule_delivery.start_date_time, "
	  		    + "         comm_schedule_delivery.end_date_time, "
	  		    + "         comm_schedule_delivery.service_provider_deal_num ohi_service_provider_deal_num, "
	  		    + "    	gas_phys_location.index_id "
	  		    + "      from"
	  		    +           fromDelClauseStr
	  		    + "      where"
	  		    + "         query_result.unique_id = " + iQueryID
	  		    + "         and ("
	  		    + "            ab_tran.ins_type in (48020, 48030) "
	  		    + "            or ab_tran.ins_type in "
	  		    + "               (select id_number from instruments where base_ins_id in (48020, 48030))"
	  		    + "         )"
	  		    + "         and comm_schedule_delivery.delivery_status   <> 8"
	  		    + "         and parameter.pay_rec                        = 0"
	  		    + "         and ab_tran.current_flag                     = 1"
	  		    + "         and gas_phys_location.active                 = 1"
	  		    + "         and comm_schedule_delivery.delivery_location_type = 3"
	  		    +           sqlConditionStr
	  		    + " UNION ALL" 
	  		    + "      select"
	  		    + "         gas_phys_location.location_id ohi_location_id, "
	  		    + "         phys_header.contract_number, "
	  		    + "         ab_tran.internal_lentity ohi_internal_lentity, "
	  		    + "         ("
	  		    + "            select distinct "
	  		    + "               a.external_lentity "
	  		    + "            from "
	  		    + "               ab_tran a join " 
	  		    + "               comm_sched_deliv_deal c on "
	  		    + "                  c.deal_num = a.deal_tracking_num"
	  		    + "            where "
	  		    + "               a.current_flag = 1" 
	  		    + "               and c.delivery_id = comm_schedule_delivery.delivery_id "
	  		    + "               and c.receipt_delivery = parameter.pay_rec "
	  		    + "         ) ohi_external_lentity, "
	  		    + "         comm_schedule_delivery.delivery_location_id ohi_trans_location_id, "
	  		    + "         ("
	  		    + "            select "
	  		    + "               gas_phys_location.meter_id "
	  		    + "            from "
	  		    + "               gas_phys_location "
	  		    + "            where "
	  		    + "               gas_phys_location.location_id = comm_schedule_delivery.delivery_location_id "
	  		    + "         ) meter_id, "
	  		    + "         comm_schedule_header.volume_type ohi_volume_type, "
	  		    + "         comm_schedule_delivery.nom_transaction_type ohi_nom_transaction_type, "
	  		    + "         comm_schedule_detail.day_start_date_time, "
	  		    + "         comm_schedule_detail.day_end_date_time, "
	  		    + "         comm_schedule_delivery.delivery_id ohi_delivery_id, "
	  		    + "         comm_schedule_delivery.delivery_status ohi_delivery_status,"
	  		    + "         gas_phys_location.pipeline_id ohi_pipeline_id, "
	  		    + "         (comm_schedule_detail.fuel_quantity) ohd_fuel_quantity, "
	  		    + "         comm_schedule_header.schedule_id ohi_schedule_id, "
	  		    + "         parameter.pay_rec ohi_pay_rec, "
	  		    + "         comm_schedule_detail.bav_flag ohi_bav_flag, "
	  		    + "         comm_schedule_detail.quantity ohd_quantity, "
	  		    + "         comm_schedule_header.unit ohi_unit, "
   	  		    + "         comm_schedule_header.param_seq_num ohi_param_seq_num, "
	  		    + "         0 ohi_num_days, "
	  		    + "         gas_phys_pipelines.region_id ohi_region_id, "
	  		    + "         comm_schedule_delivery.start_date_time, "
	  		    + "         comm_schedule_delivery.end_date_time, "
	  		    + "         comm_schedule_delivery.service_provider_deal_num ohi_service_provider_deal_num, "
	  		    + "    	gas_phys_location.index_id "
	    		    + " from "
			      + " gas_phys_location " 
			      + " , comm_schedule_delivery "
			      + " , comm_schedule_header "
			      + " , ab_tran "
			      + " , phys_header "
			      + " , comm_schedule_detail "
			      + " , parameter "
			      + " , gas_phys_pipelines "  
			      + " , query_result "
	  		    + "      where"
	  		    + "         query_result.unique_id = " + iQueryID
	  		    + "         and comm_schedule_delivery.delivery_status   <> 8"
	  		    + "         and parameter.pay_rec                        = 1" 
	  		    + "         and ab_tran.current_flag                     = 1"
	  		    + "         and gas_phys_location.active                 = 1"
	  		    + "         and comm_schedule_delivery.receipt_location_type = 3"
	  		    + "         and comm_schedule_delivery.service_provider_deal_num = 7" 
	  		    + "         and comm_schedule_header.volume_type        in (4, 6, 7, 8)"
   	  		    + "	        and gas_phys_location.location_id            = comm_schedule_delivery.receipt_location_id "
			    + "	        and comm_schedule_header.delivery_id         = comm_schedule_delivery.delivery_id "
			    + "	        and comm_schedule_header.schedule_id         = comm_schedule_detail.schedule_id "
			    + "	        and comm_schedule_header.ins_num             = ab_tran.ins_num "
			    + "	        and comm_schedule_header.ins_num             = phys_header.ins_num "
			    + "		and comm_schedule_header.ins_num             = parameter.ins_num "
			    + "	        and ab_tran.ins_num                          = phys_header.ins_num "
			    + "	        and ab_tran.ins_num                          = parameter.ins_num "
			    + "	        and parameter.ins_num                        = phys_header.ins_num "
			    + "	        and comm_schedule_header.param_seq_num       = parameter.param_seq_num "
			    + "	        and gas_phys_location.pipeline_id            = gas_phys_pipelines.pipeline_id "
			    +           sqlConditionStr
			    +	        ZCGWhereClauseStr
	  		    + " UNION ALL"
	  		    + "      select"
	  		    + "         gas_phys_location.location_id ohi_location_id, "
	  		    + "         phys_header.contract_number, "
	  		    + "         ab_tran.internal_lentity ohi_internal_lentity, "
	  		    + "         ("
	  		    + "            select distinct "
	  		    + "               a.external_lentity" 
	  		    + "            from "
	  		    + "               ab_tran a join "
	  		    + "               comm_sched_deliv_deal c on "
	  		    + "                  c.deal_num = a.deal_tracking_num"
	  		    + "            where "
	  		    + "               a.current_flag = 1" 
	  		    + "               and c.delivery_id = comm_schedule_delivery.delivery_id"
	  		    + "               and c.receipt_delivery = parameter.pay_rec "
	  		    + "         ) ohi_external_lentity, "
	  		    + "         comm_schedule_delivery.receipt_location_id ohi_trans_location_id, "
	  		    + "         ("
	  		    + "            select "
	  		    + "               gas_phys_location.meter_id "
	  		    + "            from "
	  		    + "               gas_phys_location "
	  		    + "            where "
	  		    + "               gas_phys_location.location_id = comm_schedule_delivery.receipt_location_id "
	  		    + "         ) meter_id, "
	  		    + "         comm_schedule_header.volume_type ohi_volume_type, "
	  		    + "         comm_schedule_delivery.nom_transaction_type ohi_nom_transaction_type, "
	  		    + "         comm_schedule_detail.day_start_date_time, "
	  		    + "         comm_schedule_detail.day_end_date_time, "
	  		    + "         comm_schedule_delivery.delivery_id ohi_delivery_id, "
	  		    + "         comm_schedule_delivery.delivery_status ohi_delivery_status, "
	  		    + "         gas_phys_location.pipeline_id ohi_pipeline_id, "
	  		    + "         (comm_schedule_detail.fuel_quantity) ohd_fuel_quantity, "
	  		    + "         comm_schedule_header.schedule_id ohi_schedule_id, "
	  		    + "         parameter.pay_rec ohi_pay_rec, "
	  		    + "         comm_schedule_detail.bav_flag ohi_bav_flag, "
	  		    + "         comm_schedule_detail.quantity ohd_quantity, "
	  		    + "         comm_schedule_header.unit ohi_unit, "
	  		    + "         comm_schedule_header.param_seq_num ohi_param_seq_num, "
	  		    + "         0 ohi_num_days,"
	  		    + "         gas_phys_pipelines.region_id ohi_region_id, "
	  		    + "         comm_schedule_delivery.start_date_time, "
	  		    + "         comm_schedule_delivery.end_date_time, "
	  		    + "         comm_schedule_delivery.service_provider_deal_num ohi_service_provider_deal_num, "
	  		    + "    	gas_phys_location.index_id "
	    		    + " from "
			    + "	        gas_phys_location " 
			    + "	        , comm_schedule_delivery "
			    + "	        , comm_schedule_header "
			    + "	        , ab_tran "
			    + "	        , phys_header "
			    + "	        , comm_schedule_detail "
			    + "	        , parameter "
			    + "	        , gas_phys_pipelines "  
			    + "	        , query_result "
	  		    + "      where"
	  		    + "         query_result.unique_id = " + iQueryID
	  		    + "         and comm_schedule_delivery.delivery_status   <> 8"
	  		    + "         and parameter.pay_rec                        = 0" 
	  		    + "         and ab_tran.current_flag                     = 1"
	  		    + "         and gas_phys_location.active                 = 1"
	  		    + "         and comm_schedule_delivery.delivery_location_type = 3"
	  		    + "         and comm_schedule_delivery.service_provider_deal_num = 7" 
	  		    + "         and comm_schedule_header.volume_type        in (4, 6, 7, 8)"
   	  		    + "	        and gas_phys_location.location_id            = comm_schedule_delivery.delivery_location_id "
			    + "	        and comm_schedule_header.delivery_id         = comm_schedule_delivery.delivery_id "
			    + "	        and comm_schedule_header.schedule_id         = comm_schedule_detail.schedule_id "
			    + "	        and comm_schedule_header.ins_num             = ab_tran.ins_num "
			    + "	        and comm_schedule_header.ins_num             = phys_header.ins_num "
			    + "		and comm_schedule_header.ins_num             = parameter.ins_num "
			    + "	        and ab_tran.ins_num                          = phys_header.ins_num "
			    + "	        and ab_tran.ins_num                          = parameter.ins_num "
			    + "	        and parameter.ins_num                        = phys_header.ins_num "
			    + "	        and comm_schedule_header.param_seq_num       = parameter.param_seq_num "
			    + "	        and gas_phys_location.pipeline_id            = gas_phys_pipelines.pipeline_id "
			    +           sqlConditionStr
			    +	        ZCGWhereClauseStr;
					
      com.olf.openjvs.DBase.runSqlFillTable(sQuery, dataTable);
      return dataTable;
   }

   static Table createOutputTable(Table outputTable, Table tmp_t) throws OException
   {
	   int col = outputTable.getColNum( "meter_id");
	   /* Array columns must be added after the table is made. There is no alternative */
	   col += 1;
	   outputTable.insertCol( "days", col, COL_TYPE_ENUM.COL_INT);
	   outputTable.colShow( col);
	   col = outputTable.getColNum( "days");
	   tmp_t.addCol( "days", COL_TYPE_ENUM.COL_INT);

	   outputTable.addCol( "monthly_into_pool",   COL_TYPE_ENUM.COL_DOUBLE);
	   outputTable.addCol( "monthly_out_of_pool", COL_TYPE_ENUM.COL_DOUBLE);
	   col += 1;
	   outputTable.insertCol( "monthly_end_balance",   col,  COL_TYPE_ENUM.COL_DOUBLE);
	   outputTable.colShow( col);
	   outputTable.addCol( "daily_quantity",      COL_TYPE_ENUM.COL_DOUBLE);
	   tmp_t.addCol( "monthly_into_pool",   COL_TYPE_ENUM.COL_DOUBLE);
	   tmp_t.setColFormatAsNotnlAcct( "monthly_into_pool",   12, 0, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
	   tmp_t.addCol( "monthly_out_of_pool", COL_TYPE_ENUM.COL_DOUBLE);
	   tmp_t.setColFormatAsNotnlAcct( "monthly_out_of_pool", 12, 0, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
	   tmp_t.addCol( "monthly_end_balance",   COL_TYPE_ENUM.COL_DOUBLE);
	   tmp_t.setColFormatAsNotnlAcct( "monthly_end_balance",   12, 0, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
	   tmp_t.addCol( "daily_quantity",      COL_TYPE_ENUM.COL_DOUBLE);
	   tmp_t.setColFormatAsNotnlAcct( "daily_quantity",      12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
	   
	   outputTable.addCol( "day_start_date",      COL_TYPE_ENUM.COL_INT);
	   outputTable.addCol( "day_start_time",      COL_TYPE_ENUM.COL_INT);
	   outputTable.addCol( "day_end_date",      COL_TYPE_ENUM.COL_INT);
	   outputTable.addCol( "day_end_time",      COL_TYPE_ENUM.COL_INT);
	   outputTable.colHide("day_start_date");
	   outputTable.colHide("day_start_time");
	   outputTable.colHide("day_end_date");
	   outputTable.colHide("day_end_time");
	   
	   outputTable.addCol( "volume_unit",   COL_TYPE_ENUM.COL_INT);
	   outputTable.addCol( "energy_unit", COL_TYPE_ENUM.COL_INT);
	      
      return outputTable;
   }
				
   private static void formatResult(Table outputTable) throws OException
   {
      outputTable.setColTitle( "location_id", "Pool");
      outputTable.setColFormatAsRef( "location_id", SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);

	  outputTable.setColTitle( "day_start_date_time", "Start DateTime");
	  outputTable.setColFormatAsDateDayStartTime( "day_start_date_time", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT, 0);

	  outputTable.setColTitle( "day_end_date_time", "End DateTime");
	  outputTable.setColFormatAsDateDayStartTimeEnd( "day_end_date_time", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT, 0);

	  outputTable.setColTitle( "pipeline_id", "Pipe");
	  outputTable.setColFormatAsRef( "pipeline_id", SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);

	  outputTable.setColTitle( "region_id", "Region");
	  outputTable.setColFormatAsRef( "region_id", SHM_USR_TABLES_ENUM.GAS_PHYS_REGIONS_TABLE);

	  outputTable.setColTitle( "contract_number", "Contract");

	  outputTable.setColTitle( "delivery_id", "Delivery ID");

	  outputTable.setColTitle( "schedule_id", "Schedule ID");

      outputTable.setColTitle( "trans_location_id", "Location");
      outputTable.setColFormatAsRef( "trans_location_id", SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);

      outputTable.setColTitle( "meter_id", "Meter");

      outputTable.setColTitle( "ins_type", "Ins. Type");
      outputTable.setColFormatAsRef( "ins_type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);

      outputTable.setColTitle( "delivery_status", "Status");
      outputTable.setColFormatAsRef( "delivery_status", SHM_USR_TABLES_ENUM.DELIVERY_STATUS_TABLE);

      outputTable.setColTitle( "pay_rec", "Pay / Rec");
      outputTable.setColFormatAsRef( "pay_rec", SHM_USR_TABLES_ENUM.REC_PAY_TABLE);

      outputTable.setColTitle( "volume_type", "Volume Type");
      outputTable.setColFormatAsRef( "volume_type", SHM_USR_TABLES_ENUM.VOLUME_TYPE_TABLE );

      outputTable.setColTitle( "bav_flag", "BAV Flag");
      outputTable.setColFormatAsRef( "bav_flag", SHM_USR_TABLES_ENUM.YES_NO_TABLE);

      outputTable.setColTitle( "quantity", "Hourly Quantity");
      outputTable.setColFormatAsNotnlAcct( "quantity", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

      outputTable.setColTitle( "unit", "Units");
      outputTable.setColFormatAsRef( "unit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE );
      
      outputTable.setColTitle( "volume_unit",                "Volume Units");
	  outputTable.setColFormatAsRef( "volume_unit",                SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE );
	   
	  outputTable.setColTitle( "energy_unit",                "Energy Units");
	  outputTable.setColFormatAsRef( "energy_unit",                SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE );
	   
      outputTable.setColTitle( "nom_transaction_type", "Trans Type");
      outputTable.setColFormatAsRef( "nom_transaction_type", SHM_USR_TABLES_ENUM.NOM_TRAN_TYPE_TABLE );

      outputTable.setColTitle( "external_lentity", "Counterparty");
      outputTable.setColFormatAsRef( "external_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE );

      outputTable.setColTitle( "internal_lentity", "Internal LE");
      outputTable.setColFormatAsRef( "internal_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE );

      outputTable.setColTitle( "fuel_quantity", "Fuel");
      outputTable.setColFormatAsNotnlAcct( "fuel_quantity", 12, 0, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 1);

      outputTable.setColTitle( "num_days", "Number of Days");
      
      outputTable.setColTitle( "days", "Days");

      outputTable.setColTitle( "contract_number", "Contract");

      outputTable.setColTitle( "start_date_time", "Delivery\nStart Date");
      outputTable.setColFormatAsDateDayStartTime( "start_date_time", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT, 0);

      outputTable.setColTitle( "end_date_time", "Delivery\nEnd Date");
      outputTable.setColFormatAsDateDayStartTimeEnd( "end_date_time", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT, 0);

      outputTable.setColTitle( "service_provider_deal_num", "Service Provider\nDeal Num");
	    
	   outputTable.setColTitle( "day", "Day");
	   outputTable.setColFormatAsDate( "day", DATE_FORMAT.DATE_FORMAT_DAY_OF_MONTH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
	   
	   outputTable.setColTitle( "monthly_end_balance",   "Month End Balance");
	   outputTable.setColFormatAsNotnlAcct( "monthly_end_balance",   12, -1, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
	   
	   outputTable.setColTitle( "monthly_into_pool",   "Into Pool");
	   outputTable.setColFormatAsNotnlAcct( "monthly_into_pool",   12, -1, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
	   
	   outputTable.setColTitle( "monthly_out_of_pool", "Out of Pool");
	   outputTable.setColFormatAsNotnlAcct( "monthly_out_of_pool", 12, -1, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
	   
	   outputTable.setColTitle( "daily_quantity",      "Daily Volume");
	   outputTable.setColFormatAsNotnlAcct( "daily_quantity",      12, -1, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
	   
	   outputTable.setColFormatAsDate( "days", DATE_FORMAT.DATE_FORMAT_DAY_OF_MONTH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
	   
	   outputTable.setColTitle("day_start_date", "Day Start Date");
	   outputTable.setColTitle("day_start_time","Day Start Time");
	   outputTable.setColTitle("day_end_date","Day End Date");
	   outputTable.setColTitle("day_end_time","Day End Time");
	   
	   outputTable.setColTitle( "param_seq_num", "Service Provider\nDeal Side");
	   
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
      if (pay_rec == 0)
      { /* Receive */
         t.setDouble( "monthly_into_pool",   row,  qty);
         t.setDouble( "monthly_end_balance",   row,  qty);
      }
      else
      { /* Pay */
         t.setDouble( "monthly_out_of_pool", row,  qty);
         t.setDouble( "monthly_end_balance",   row, -qty);
      }
   }

   static void expandCSDVolumeToDailyVolumes(Table outputTable, Table tmp_t, int row, int start_date_colnum, int end_date_colnum, int hourly_vol_colnum, int pay_rec_colnum, int first_date, int last_date, int units_colnum, int location_colnum) throws OException
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
	  int 		  pipeLineUnit = Pipeline.getUnit(outputTable.getInt("pipeline_id", row));
	  int		  units		 = outputTable.getInt( units_colnum,    row);
	  Table conversionTable;
	  int convFactorColNum;
	  
	  if(pipeLineUnit != units)
	  {
		  /*When a nomination is created the unit used to nominate is saved to
		  **comm_schedule_detail regardless of it matching the unit on the pipeline
		  **In order to display correctly in APM we must convert back to the unit
		  **of the pipeline for any nominations whose unit does not match the pipeline
		  */
		  int		  location		 = outputTable.getInt( location_colnum,    row);
		  conversionTable = Transaction.utilGetEnergyConversionFactors(start_date, end_date, location, units, pipeLineUnit);
		  convFactorColNum = conversionTable.getColNum("Conv Factor");
		  double dMassConvFactor = conversionTable.getDouble(convFactorColNum,1);
		  outputTable.setDouble(hourly_vol_colnum, row, outputTable.getDouble( hourly_vol_colnum, row) * dMassConvFactor);
		  outputTable.setInt(units_colnum, row, pipeLineUnit);
	  }
	
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
	  
	  /* Make space in tmp_t */
	  if(num_days != 0)
	  {
		 if (end_sec == 0)
		 {
		    --end_date;
		    end_sec = 86400;
		    outputTable.setInt( "day_end_date", row, end_date);
		    outputTable.setInt( "day_end_time", row, end_sec);
		 }
		 
		 hour_vol   = outputTable.getDouble( hourly_vol_colnum, row);		  
	     for (curDate = start_date, row_ct = 0; curDate <= end_date; curDate++)
	     {
	    	day_vol_unconverted = calcDailyVolume (curDate, start_date, start_sec, end_date, end_sec, hour_vol);
	        
			if(day_vol_unconverted != day_vol_unconverted_previous || row_ct == 0/*first time through*/)
	    	{
				tmp_t.addRow();
	    		++row_ct;
	    		
	    		day_vol_unconverted_sum = day_vol_unconverted;
	    		day_vol_unconverted_previous = day_vol_unconverted;
	    	}
	    	else
	    	{
	    		day_vol_unconverted_sum = day_vol_unconverted_sum + day_vol_unconverted;
	    	}
        	tmp_t.setDouble( "daily_quantity", row_ct, day_vol_unconverted);
        	setTableDateQty (tmp_t, row_ct, curDate, day_vol_unconverted_sum, pay_rec);
	     }
	 
	  
	     /* Put the information into the summary table */
	//         outputTable.copyArrayDataRows( row, tmp_t);
	     for(int tmp_t_row = 1; tmp_t_row<= row_ct;  tmp_t_row++)
	     {
	    	 if(tmp_t_row > 1)
	    	 {
	    		 int inputRow = row;//temporary variable because insert row after not working
	    		 //row = outputTable.insertRowAfter(row);
	    		 row = outputTable.addRow();
	    		 outputTable.copyRow(inputRow, outputTable, row);
	    	 }
	    	 
	    	 outputTable.setDouble("monthly_into_pool", row, tmp_t.getDouble("monthly_into_pool", tmp_t_row));
	    	 outputTable.setDouble("monthly_out_of_pool", row, tmp_t.getDouble("monthly_out_of_pool", tmp_t_row));
	    	 outputTable.setDouble("daily_quantity", row, tmp_t.getDouble("daily_quantity", tmp_t_row));
	    	 outputTable.setDouble("monthly_end_balance", row, tmp_t.getDouble("monthly_end_balance", tmp_t_row));
	    	 
	    	 outputTable.setInt("days", row, tmp_t.getInt("days", tmp_t_row));
	    	 
	     }
	     tmp_t.clearRows();
	     outputTable.setInt( "num_days", row, num_days);
	  }

	  convertHourlyFuelQuantityToDaily (outputTable, row);  
	  
	  return;
   }
   
   /**
    * Converts the hourly quantity fuel to daily quantity
    * @param outputTable
    * @param row
    * @throws OException
    */
   private static void convertHourlyFuelQuantityToDaily (Table outputTable, int row) throws OException
   {
	  ODateTime startDt = outputTable.getDateTime ("day_start_date_time", row);
	  ODateTime endDt = outputTable.getDateTime ("day_end_date_time", row);
	  double hourly_fuel_quantity = outputTable.getDouble("fuel_quantity", row);
	  double daily_fuel_quantity = calcDailyVolume (startDt.getDate(), startDt.getDate(), startDt.getTime(), endDt.getDate(), endDt.getTime(), hourly_fuel_quantity);
	  outputTable.setDouble("fuel_quantity", row, daily_fuel_quantity);
   }
   
   void removeEmptyRows ( Table outputTable)throws OException
   {
      int   rows = outputTable.getNumRows();
      int   row;
      int   col  = outputTable.getColNum( "num_days");

      /* Go backwards for speed */
      for (row=rows; row>=1; row--)
      {
         if (outputTable.getInt( col, row) == 0)
            outputTable.delRow( row);
      }

      rows = outputTable.getNumRows();
   }
   
   private static Table expandDailyVolume(Table outputTable, int start_date, int end_date) throws OException
   {
      int         start_dt_colnum   = 0;
      int         end_dt_colnum     = 0;
      int         hourly_vol_colnum = 0;
      int         pay_rec_colnum    = 0;
      int         units_colnum      = 0;
      int         location_colnum   = 0;
      int         spdn_colnum       = 0;
      int         contract_colnum   = 0;
      Table    tmp_t             = Table.tableNew        ("Temp Table");
      int         num_rows;
      int         cur_row;

      outputTable = createOutputTable(outputTable, tmp_t);

      start_dt_colnum   = outputTable.getColNum( "day_start_date_time");
      end_dt_colnum     = outputTable.getColNum( "day_end_date_time");
      hourly_vol_colnum = outputTable.getColNum( "quantity");
      pay_rec_colnum    = outputTable.getColNum( "pay_rec");
      units_colnum      = outputTable.getColNum( "unit");
      location_colnum   = outputTable.getColNum( "trans_location_id");
      spdn_colnum       = outputTable.getColNum( "service_provider_deal_num");
      contract_colnum   = outputTable.getColNum( "contract_number");

      /* Verify required columns */
      if (start_dt_colnum < 0 || end_dt_colnum < 0 || hourly_vol_colnum < 0 || pay_rec_colnum < 0)
         return outputTable;
      
      num_rows = outputTable.getNumRows();

      for (cur_row = 1; cur_row <= num_rows; cur_row++)
      {
         int service_provider_deal_num = outputTable.getInt(spdn_colnum, cur_row);
         outputTable.setInt("day_start_date", cur_row, outputTable.getDate("day_start_date_time", cur_row));
         outputTable.setInt("day_start_time", cur_row, outputTable.getTime("day_start_date_time", cur_row));
         outputTable.setInt("day_end_date", cur_row, outputTable.getDate("day_end_date_time", cur_row));
         outputTable.setInt("day_end_time", cur_row, outputTable.getTime("day_end_date_time", cur_row));

         expandCSDVolumeToDailyVolumes (outputTable, tmp_t, cur_row, start_dt_colnum, end_dt_colnum, hourly_vol_colnum, pay_rec_colnum, start_date, end_date, units_colnum, location_colnum);

         if (service_provider_deal_num < NOM_SYSTEM_TYPE.NOM_SYSTEM_TRAN_ANY_VALID_TRAN.toInt()) 
         {
        	 outputTable.setString(contract_colnum, cur_row, Ref.getShortName(SHM_USR_TABLES_ENUM.SYSTEM_TRAN_TABLE, service_provider_deal_num) );
         }
      }

      tmp_t.destroy();
      
      return outputTable;
   }
}
