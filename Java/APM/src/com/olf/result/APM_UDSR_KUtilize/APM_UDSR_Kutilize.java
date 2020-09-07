/* Released with version 03-Apr-2018_V17_0_5 of APM */

/*
 File Name:                      KUtilize.java

 Report Name:                    NONE

 Output File Name:               NONE

 Author:                         Joe Gallagher
 Creation Date:                  March 14, 2014

 Revision History:

 Script Type:                    User-defined simulation result

 Main Script:                    
 Parameter Script:               
 Display Script: 

 Description:                    APM_UDSR_KUtilize

 */

package com.olf.result.APM_UDSR_KUtilize;

import java.util.Vector;

import com.olf.result.APMUtility.*;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.Math;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.INS_SUB_TYPE;

public class APM_UDSR_Kutilize implements IScript 
{
   static int NOM_FLOW = 1;
   static int NOM_OVER = 2;

    /*static String massColArray[] = {};
   
   static String volumeColArray[] = {"entitlement_min_qty", 	"entitlement_min_qty_vol",		"Entitlement Min Qty\nVolume",
				     "entitlement_max_qty", 	"entitlement_max_qty_vol",		"Entitlement Max Qty\nVolume",
		   		     "bav_quantity", 			"bav_quantity_vol",				"BAV Quantity\nVolume",
		   		     "difference", 				"difference_vol",				"Difference\nVolume",
		   		     "flow_volume",				"flow_volume_vol",				"BAV Flow\nVolume",
		   		     "auth_overrun",			"auth_overrun_vol",				"BAV Overrun\nVolume",
		   		     "stranded_volume",			"stranded_volume_vol",			"(Over)/Under\nVolume" };
   
   static String energyColArray[] = {"entitlement_min_qty", 	"entitlement_min_qty_energy",	" Entitlement Min Qty\nEnergy",
				     "entitlement_max_qty", 	"entitlement_max_qty_energy",	"Entitlement Max Qty\nEnergy",
				     "bav_quantity", 			"bav_quantity_energy",			"BAV Quantity\nEnergy",
				     "difference", 				"difference_energy",			"Difference\nEnergy",
	   			     "flow_volume",				"flow_volume_energy",			"BAV Flow\nEnergy",
				     "auth_overrun",			"auth_overrun_energy",			"BAV Overrun\nEnergy",
				     "stranded_volume",			"stranded_volume_energy",		"(Over)/Under\nEnergy"};*/

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

   private void computeResult(Table argt, Table returnt) throws OException 
   {
      
      /*int massUnit = -1, volumeUnit = -1, energyUnit = -1;*/
      int showAllVolumeStatuses = 0;
      int iAttributeGroup;
      int startPoint = -1;
      int endPoint = -1;
      String volumeTypesStr = "", transVolTypesStr = "";
      String strVal;
      Table tblData = Table.tableNew();
      Table tblAttributeGroups, tblConfig;
      Table tblTrans = argt.getTable("transactions", 1);
      String authNomTypesStr = "";
      Table authNomTypesTable = null;
      int breakoutBAVOverrun = 1;

      tblAttributeGroups = SimResult.getAttrGroupsForResultType(argt.getInt("result_type", 1));
      iAttributeGroup = tblAttributeGroups.getInt("result_config_group", 1);
      tblConfig = SimResult.getResultConfig(iAttributeGroup);
      tblConfig.sortCol("res_attr_name");

      if (APMUtility.ParamHasValue(tblConfig, "APM KUtilize Start Date") > 0) 
      {
	    startPoint = APMUtility.GetParamDateValue(tblConfig,"APM KUtilize Start Date");
      }

      if (APMUtility.ParamHasValue(tblConfig, "APM KUtilize End Date") > 0) 
      {
	    endPoint = APMUtility.GetParamDateValue(tblConfig,"APM KUtilize End Date");
      }

      if (APMUtility.ParamHasValue(tblConfig,	"APM KUtilize Storage Volume Types") > 0) 
      {
	    volumeTypesStr = APMUtility.GetParamStrValue(tblConfig,	"APM KUtilize Storage Volume Types");
      }

      if (APMUtility.ParamHasValue(tblConfig,	"APM KUtilize Transport Volume Types") > 0) 
      {
	    transVolTypesStr = APMUtility.GetParamStrValue(tblConfig,"APM KUtilize Transport Volume Types");
      }

      /* if (APMUtility.ParamHasValue(tblConfig, "APM KUtilize Mass Unit") > 0) 
      {
	    strVal = APMUtility.GetParamStrValue(tblConfig,"APM KUtilize Mass Unit");
	    if (!strVal.isEmpty()) 
	    {
	    	massUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE,strVal);
	    }
      }

      if (APMUtility.ParamHasValue(tblConfig, "APM KUtilize Volume Unit") > 0) 
      {
	    strVal = APMUtility.GetParamStrValue(tblConfig,	"APM KUtilize Volume Unit");
	    if (!strVal.isEmpty()) 
	    {
	    	volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE,	strVal);
	    }
      }

      if (APMUtility.ParamHasValue(tblConfig, "APM KUtilize Energy Unit") > 0) 
      {
	    strVal = APMUtility.GetParamStrValue(tblConfig,	"APM KUtilize Energy Unit");
	    if (!strVal.isEmpty()) 
	    {
		     energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE,
				    strVal);
	    }
	    }*/

      if (APMUtility.ParamHasValue(tblConfig,	"APM KUtilize Authorized Nom Types") > 0) 
      {
	    authNomTypesStr = APMUtility.GetParamStrValue(tblConfig,"APM KUtilize Authorized Nom Types");
	    authNomTypesTable = getNomTranTypeTable(authNomTypesStr);
      }

      if (APMUtility.ParamHasValue(tblConfig,"APM KUtilize Breakout BAV Overrun") > 0) 
      {
	    strVal = APMUtility.GetParamStrValue(tblConfig,	"APM KUtilize Breakout BAV Overrun");
	    breakoutBAVOverrun = Ref.getValue(SHM_USR_TABLES_ENUM.YES_NO_TABLE,strVal);
      }

      // Set default mass and unit values, if not specified
      /* if (massUnit < 0) 
      {
	    massUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MT");
      }
      if (volumeUnit < 0) 
      {
	    volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "BBL");
      }
      if (energyUnit < 0) 
      {
	    energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MWh");
	    }*/

      Vector<Integer> allowedInsTypes = new Vector<Integer>();

      int iQueryID = APMUtility.generateDealQuery(argt, allowedInsTypes);

      if (iQueryID > 0) 
      {
	    tblData = doCalculations(iQueryID, /*massUnit, volumeUnit,
						 energyUnit,*/ tblTrans, false, showAllVolumeStatuses, 
							    startPoint, endPoint, volumeTypesStr, transVolTypesStr,
							    authNomTypesTable, breakoutBAVOverrun);

	    if (tblData.getNumRows() > 0)
		     returnt.select(tblData, "*", "deal_tracking_num GE 0");
	
	    Query.clear(iQueryID);
      }
      if(Table.isTableValid(authNomTypesTable) != 0)
    	  authNomTypesTable.destroy();
   }

    private static Table doCalculations(int iQueryID, /*int massUnit,int volumeUnit, int energyUnit,*/ Table tblTrans, boolean flipSigns,
		  int calcAllVolumeStatuses, int startPoint, int endPoint,String volumeTypesStr, String transVolTypesStr,
		  Table authNomTypesTable, int breakoutBAVOverrun) throws OException 
   {
      int numDealRows, num_ctu_rows, ctu_row, row, new_row, dtl_row, deal_tracking_num, num_dtl_rows;
      int deal_num_col, ins_type_col, ins_sub_type_col, ent_start_date_col, ent_maturity_date_col, service_provider_col;
      int deal_start_date_col, deal_maturity_date_col;
      int contract_col, ent_type_col, serv_type_col, enforce_col, ent_name_col, start_date_col;
      int end_date_col, rec_loc_type_col, rec_loc_col, delv_loc_type_col, delv_loc_col, ent_min_qty_col, ent_max_qty_col, delivery_loc_col, facility_id_col;
      int bav_qty_col, diff_col, delv_id_col, delv_vol_type_col, dtl_start_date_col = 0, dtl_end_date_col = 0, dtl_qty_col = 0;
      int unit_col, allow_pathing_col/*, volunit_col, energyunit_col*/;
      int deal_start_date, deal_maturity_date;
      int ctu_contract_col = 0;
      int ctu_ent_type_col = 0, ctu_serv_type_col = 0, ctu_enforce_col = 0, ctu_ent_name_col = 0;
      int ctu_rec_loc_type_col = 0, ctu_rec_loc_col = 0, ctu_delv_loc_type_col = 0, ctu_facility_id_col = 0, ctu_diff_col = 0;
      int ctu_delv_loc_col = 0, ctu_ent_min_qty_col = 0, ctu_ent_max_qty_col = 0, ctu_bav_qty_col = 0, ctu_dtl_col = 0;
      int dtl_delv_id_col = 0;
      int dtl_vol_type_col = 0, dtl_loc_col = 0, dtl_delv_id = 0;
      int ent_type, enforce, service_type, rec_loc_type, facility_id;
      int delv_loc_type, dtl_loc, dtl_vol_type, dtl_start_date, dtl_end_date;
      double ent_min_qty, ent_max_qty, dtl_qty = 0, bav_qty = 0, diff = 0;
      String ent_name, contract, delv_loc, rec_loc;
      int first_row;
      int service_provider, ins_type;
      Table contract_util_table, details_table;
      Table volTypesTable;
      Table transVolTypesTable;
      int ent_start_date, ent_maturity_date;
      int ins_sub_type;
      int unit, allow_pathing;
      int intNomType = NOM_FLOW;
      int authNomTypesTableIsValid = 0;
      double flow_vol = 0, over_vol = 0, stranded_vol = 0.0; // reset counters
      int nom_transaction_type_col, entitlement_group_col, ent_group;
      Table tblNom = Table.tableNew();
      int intTranRow;
      int dtl_nom_tran_type;
      int location_id_col;
      String dealTrackingNumStr;
      String nomTranTypeStr = "";
      Table outputTable = null;
      /*int massUnitCol, volumeUnitCol, energyUnitCol, location_id;
	Table dataTable = retrieveData(iQueryID, massUnit, volumeUnit,flipSigns, calcAllVolumeStatuses, startPoint, endPoint);*/
      Table dataTable = retrieveData(iQueryID, flipSigns, calcAllVolumeStatuses, startPoint, endPoint);
      outputTable = createOutputTable(dataTable);

      /* get output table column numbers */
      deal_num_col = outputTable.getColNum("deal_tracking_num");
      ins_type_col = outputTable.getColNum("ins_type");
      ins_sub_type_col = outputTable.getColNum("ins_sub_type");
      ent_start_date_col = outputTable.getColNum("start_date");
      ent_maturity_date_col = outputTable.getColNum("maturity_date");
      contract_col = outputTable.getColNum("contract_number");
      service_provider_col = outputTable.getColNum("pipeline_id");
      ent_type_col = outputTable.getColNum("entitlement_type");
      serv_type_col = outputTable.getColNum("service_type");
      enforce_col = outputTable.getColNum("enforce");
      ent_name_col = outputTable.getColNum("entitlement_name");
      start_date_col = outputTable.getColNum("ent_start_date");
      end_date_col = outputTable.getColNum("ent_end_date");
      rec_loc_type_col = outputTable.getColNum("rec_loc_type");
      rec_loc_col = outputTable.getColNum("rec_loc");
      delv_loc_type_col = outputTable.getColNum("del_loc_type");
      delv_loc_col = outputTable.getColNum("del_loc");
      ent_min_qty_col = outputTable.getColNum("entitlement_min_qty");
      ent_max_qty_col = outputTable.getColNum("entitlement_max_qty");
      bav_qty_col = outputTable.getColNum("bav_quantity");
      diff_col = outputTable.getColNum("difference");
      delv_id_col = outputTable.getColNum("delivery_id");
      delv_vol_type_col = outputTable.getColNum("volume_type");
      delivery_loc_col = outputTable.getColNum("location");
      facility_id_col = outputTable.getColNum("facility_id");
      deal_start_date_col = outputTable.getColNum("start_date");
      deal_maturity_date_col = outputTable.getColNum("maturity_date");
      unit_col = outputTable.getColNum("unit");
      allow_pathing_col = outputTable.getColNum("allow_pathing");
      /*volunit_col = outputTable.getColNum("volume_volumeunit");
	energyunit_col = outputTable.getColNum("energy_energyunit");*/
      location_id_col = outputTable.getColNum("location_id");
      nom_transaction_type_col = outputTable.getColNum("nom_transaction_type");
      entitlement_group_col = outputTable.getColNum("entitlement_group");

      numDealRows = dataTable.getNumRows();

      /*if (transVolTypesStr.isEmpty())// transVolTypesTable
      {
	 transVolTypesTable = APMUtility.GetMasterVolumeTypes(false);
      } 
      else 
      {
	 transVolTypesTable = APMUtility.GetShowAllVolumeTypesQueryString(transVolTypesStr);
      }

      if (volumeTypesStr.isEmpty())// volTypesTable
      {
	 volTypesTable = APMUtility.GetMasterVolumeTypes(false);
      } 
      else 
      {
	 volTypesTable = APMUtility.GetShowAllVolumeTypesQueryString(volumeTypesStr);
      }*/



      if (transVolTypesStr.isEmpty())/* transVolTypesTable*/{
    	  transVolTypesTable = APMUtility.GetMasterVolumeTypes(false);} 
      else{
    	  transVolTypesTable = APMUtility.GetShowAllVolumeTypesQueryString(transVolTypesStr);}

      if (volumeTypesStr.isEmpty())/*volTypesTable*/{
    	  volTypesTable = APMUtility.GetMasterVolumeTypes(false);} 
      else{
    	  volTypesTable = APMUtility.GetShowAllVolumeTypesQueryString(volumeTypesStr);}
		
      for (row = 1; row <= numDealRows; row++) 
      {
	 deal_tracking_num = dataTable.getInt("deal_tracking_num", row);
	 dealTrackingNumStr = Str.intToStr(deal_tracking_num);
	 if(row == 1)
	    nomTranTypeStr = dealTrackingNumStr;
	 else
	    nomTranTypeStr = nomTranTypeStr + "," + dealTrackingNumStr;
      }
      if(numDealRows > 0)
      {
    	  tblNom.clearRows();
    	  DBaseTable.loadFromDbWithSQL(tblNom,"distinct nom_transaction_type, delivery_id",	"comm_schedule_delivery", "service_provider_deal_num in (" + nomTranTypeStr + ")" );
    	  tblNom.sortCol(2);//delivery_id
      }

      for (row = 1; row <= numDealRows; row++) 
      {
	 deal_tracking_num = dataTable.getInt("deal_tracking_num", row);
	 deal_start_date = dataTable.getDate("start_date", row);
	 deal_maturity_date = dataTable.getDate("maturity_date", row);
	 service_provider = dataTable.getInt("pipeline_id", row);
	 ins_type = dataTable.getInt("ins_type", row);
	 ins_sub_type = dataTable.getInt("ins_sub_type", row);
	 unit = dataTable.getInt("unit", row);
	 allow_pathing = dataTable.getInt("allow_pathing", row);
	 /*location_id = dataTable.getInt("location_id", row);*/
		
	 if (startPoint != -1) 
	 {
		  deal_start_date = (int) Math.max(deal_start_date, startPoint);
	 }

	 if (endPoint != -1) 
	 {
		  deal_maturity_date = (int) Math.min(deal_maturity_date,	endPoint);
	 }

	 if (ins_type == INS_TYPE_ENUM.comm_transport.toInt()) 
	 {
	    /*contract_util_table = Transaction.getContractUtilization(deal_tracking_num, deal_start_date, deal_maturity_date,transVolTypesTable, unit);*/
		  contract_util_table = Transaction.getContractUtilization(deal_tracking_num, deal_start_date, deal_maturity_date,transVolTypesTable, -1/*unit*/);
	 } 
	 else if ((ins_type == INS_TYPE_ENUM.comm_storage.toInt())	|| (ins_type == INS_TYPE_ENUM.comm_storage_balance.toInt())) 
	 {
	    /*contract_util_table = Transaction.getContractUtilization(deal_tracking_num, deal_start_date, deal_maturity_date,volTypesTable, unit);*/
		  contract_util_table = Transaction.getContractUtilization(deal_tracking_num, deal_start_date, deal_maturity_date,volTypesTable, -1/*unit*/);
	 } 
	 else 
	 {
	    /*contract_util_table = Transaction.getContractUtilization(deal_tracking_num, deal_start_date, deal_maturity_date,Util.NULL_TABLE, unit);*/
		  contract_util_table = Transaction.getContractUtilization(deal_tracking_num, deal_start_date, deal_maturity_date,Util.NULL_TABLE, -1/*unit*/);
	 }

	 if (Table.isTableValid(contract_util_table) == 0)
	    continue;

	 num_ctu_rows = contract_util_table.getNumRows();

	 if (ctu_contract_col == 0) 
	 {
	    ctu_contract_col = contract_util_table.getColNum("contract");
	    ctu_ent_type_col = contract_util_table.getColNum("entitlement_type");
	    ctu_serv_type_col = contract_util_table.getColNum("service_type");
	    ctu_enforce_col = contract_util_table.getColNum("enforce");
	    ctu_ent_name_col = contract_util_table.getColNum("entitlement_name");
	    ctu_rec_loc_type_col = contract_util_table.getColNum("rec_loc_type");
	    ctu_rec_loc_col = contract_util_table.getColNum("rec_loc");
	    ctu_delv_loc_type_col = contract_util_table.getColNum("del_loc_type");
	    ctu_delv_loc_col = contract_util_table.getColNum("del_loc");
	    ctu_ent_min_qty_col = contract_util_table.getColNum("entitlement_min_qty");
	    ctu_ent_max_qty_col = contract_util_table.getColNum("entitlement_max_qty");
	    ctu_bav_qty_col = contract_util_table.getColNum("bav_quantity");
	    ctu_dtl_col = contract_util_table.getColNum("details");
	    ctu_facility_id_col = contract_util_table.getColNum("facility_id");
	    ctu_diff_col = contract_util_table.getColNum("difference");
	 }

	 contract_util_table.copyColFormat(ctu_ent_type_col, outputTable,ent_type_col);
	 /*
	    * for each contract util row, get the contract util info and
	    * details table
	    */
	 
	 authNomTypesTableIsValid = Table.isTableValid(authNomTypesTable);
	 
	 for (ctu_row = 1; ctu_row <= num_ctu_rows; ctu_row++) 
	 {
	    contract = contract_util_table.getString(ctu_contract_col,ctu_row);
	    ent_type = contract_util_table.getInt(ctu_ent_type_col, ctu_row);
	    enforce = contract_util_table.getInt(ctu_enforce_col, ctu_row);
	    ent_name = contract_util_table.getString(ctu_ent_name_col,ctu_row);
	    ent_min_qty = contract_util_table.getDouble(ctu_ent_min_qty_col, ctu_row);
	    ent_max_qty = contract_util_table.getDouble(ctu_ent_max_qty_col, ctu_row);
	    rec_loc_type = contract_util_table.getInt(ctu_rec_loc_type_col,ctu_row);
	    rec_loc = contract_util_table.formatCellData(ctu_rec_loc_col,ctu_row);
	    delv_loc_type = contract_util_table.getInt(ctu_delv_loc_type_col, ctu_row);
	    delv_loc = contract_util_table.formatCellData(ctu_delv_loc_col,ctu_row);
	    service_type = contract_util_table.getInt(ctu_serv_type_col,ctu_row);
	    details_table = contract_util_table.getTable(ctu_dtl_col,ctu_row);
	    bav_qty = contract_util_table.getDouble(ctu_bav_qty_col,ctu_row);
	    ent_start_date = contract_util_table.getInt("start_date",ctu_row);
	    ent_maturity_date = contract_util_table.getInt("end_date",ctu_row);
	    facility_id = contract_util_table.getInt(ctu_facility_id_col,ctu_row);
	    diff = contract_util_table.getDouble(ctu_diff_col, ctu_row);
	    ent_group = funcGetEntitleGroup(ent_type);

	    /* get the details table column numbers if there are not set */
	    if (Table.isTableValid(details_table) != 0	&& !(dtl_delv_id_col != 0)) 
	    {
	       dtl_delv_id_col = details_table.getColNum("Delivery ID");
	       dtl_start_date_col = details_table.getColNum("Start Date");
	       dtl_end_date_col = details_table.getColNum("End Date");
	       dtl_vol_type_col = details_table.getColNum("Volume Type");
	       dtl_qty_col = details_table.getColNum("Conv. Qty");
	       dtl_loc_col = details_table.getColNum("Location");
	    }
				
	    if (Table.isTableValid(details_table) == 1)
	       num_dtl_rows = details_table.getNumRows();
	    else
	       num_dtl_rows = 0;
				
	    flow_vol = over_vol = stranded_vol = 0.0; // reset counters
	    first_row = 0;
	    first_row = 0;

	    if (num_dtl_rows <= 0) // Add a row even if no usage for the day
	    {
	       new_row = outputTable.addRow();
	       first_row = new_row;
	       outputTable.setInt(deal_num_col, new_row, deal_tracking_num);
	       outputTable.setInt(ins_type_col, new_row, ins_type);
	       outputTable.setInt(ins_sub_type_col, new_row, ins_sub_type);
	       outputTable.setDateTimeByParts(deal_start_date_col,new_row, deal_start_date, 0);
	       outputTable.setDateTimeByParts(deal_maturity_date_col,new_row, deal_maturity_date, 0);
	       outputTable.setInt(service_provider_col, new_row,service_provider);
	       outputTable.setString(contract_col, new_row, contract);
	       outputTable.setInt(ent_type_col, new_row, ent_type);
	       outputTable.setString(ent_name_col, new_row, ent_name);
	       outputTable.setInt(enforce_col, new_row, enforce);
	       outputTable.setInt(serv_type_col, new_row, service_type);
	       outputTable.setInt(start_date_col, new_row, ent_start_date);
	       outputTable.setInt(end_date_col, new_row, ent_maturity_date);
	       outputTable.setDateTimeByParts(ent_start_date_col, new_row,ent_start_date, 0);
	       outputTable.setDateTimeByParts(ent_maturity_date_col,new_row, ent_maturity_date + 1, 0);
	       outputTable.setInt(rec_loc_type_col, new_row, rec_loc_type);
	       outputTable.setInt(unit_col, new_row, unit);
	       outputTable.setInt(allow_pathing_col, new_row, allow_pathing);
	       if (rec_loc_type >= 0)
		     outputTable.setString(rec_loc_col, new_row, rec_loc);
	       outputTable.setInt(delv_loc_type_col, new_row,delv_loc_type);
	       if (delv_loc_type >= 0)
		     outputTable.setString(delv_loc_col, new_row, delv_loc);
	       outputTable.setInt(facility_id_col, new_row, facility_id);
	       /*outputTable.setInt(massunit_col, new_row, massUnit);
	       outputTable.setInt(volunit_col, new_row, volumeUnit);
	       outputTable.setInt(energyunit_col, new_row, energyUnit);*/
	       outputTable.setInt(entitlement_group_col, new_row,ent_group);
	       /*outputTable.setInt(location_id_col, new_row,location_id);*/
	    } 
	    else 
	    {
	       /* for each detail row, add a row to output table */
	       for (dtl_row = 1; dtl_row <= num_dtl_rows; dtl_row++) 
	       {
		  if (details_table.getRowType(dtl_row) != ROW_TYPE_ENUM.ROW_DATA.toInt())
		     continue;
		  dtl_start_date = details_table.getInt(dtl_start_date_col, dtl_row);
		  dtl_end_date = details_table.getInt(dtl_end_date_col,dtl_row);
		  dtl_delv_id = details_table.getInt(dtl_delv_id_col,dtl_row);
		  dtl_vol_type = details_table.getInt(dtl_vol_type_col,dtl_row);
		  dtl_loc = details_table.getInt(dtl_loc_col, dtl_row);
		  dtl_qty = details_table.getDouble(dtl_qty_col, dtl_row);

		  intTranRow = tblNom.findInt("delivery_id", dtl_delv_id, SEARCH_ENUM.FIRST_IN_GROUP);
		  if (intTranRow > 0)
		     dtl_nom_tran_type = tblNom.getInt("nom_transaction_type", intTranRow);
		  else
		     dtl_nom_tran_type = 0;

		  new_row = outputTable.addRow();
		  outputTable.setInt(deal_num_col, new_row,deal_tracking_num);
		  outputTable.setInt(ins_type_col, new_row, ins_type);
		  outputTable.setInt(ins_sub_type_col, new_row, ins_sub_type);
		  outputTable.setDateTimeByParts(deal_start_date_col,new_row, deal_start_date, 0);
		  outputTable.setDateTimeByParts(deal_maturity_date_col,new_row, deal_maturity_date, 0);
		  outputTable.setInt(service_provider_col, new_row,service_provider);
		  outputTable.setString(contract_col, new_row, contract);
		  outputTable.setInt(ent_type_col, new_row, ent_type);
		  outputTable.setString(ent_name_col, new_row, ent_name);
		  outputTable.setInt(enforce_col, new_row, enforce);
		  outputTable.setInt(serv_type_col, new_row, service_type);
		  outputTable.setInt(start_date_col, new_row,	dtl_start_date);
		  outputTable.setInt(end_date_col, new_row, dtl_end_date);
		  outputTable.setDateTimeByParts(ent_start_date_col, new_row, ent_start_date, 0);
		  outputTable.setDateTimeByParts(ent_maturity_date_col, new_row, ent_maturity_date + 1, 0);
		  outputTable.setInt(entitlement_group_col, new_row,ent_group);
		  outputTable.setInt(unit_col, new_row, unit);
		  outputTable.setInt(allow_pathing_col, new_row, allow_pathing);
		  if (dtl_row == 1) 
		  {
			   first_row = new_row;
			   outputTable.setDouble(ent_min_qty_col, new_row,	ent_min_qty);
			   outputTable.setDouble(ent_max_qty_col, new_row,	ent_max_qty);
		  }
		  /*
		     * ent_type really contains an instrument type in this
		     * particular case
		     */
		  if (ins_type == INS_TYPE_ENUM.comm_storage.toInt() 
				    && ins_sub_type == INS_SUB_TYPE.comm_stor_inj_wth_space.toInt()
				    && ent_type == INS_TYPE_ENUM.comm_cap_space.toInt()) 
		  {
		     if (dtl_row == 1)
			outputTable.setDouble(bav_qty_col, new_row, bav_qty);
		     else
			outputTable.setDouble(bav_qty_col, new_row, 0.0);
		  } 
		  else
		     outputTable.setDouble(bav_qty_col, new_row, dtl_qty);

		  outputTable.setInt(rec_loc_type_col, new_row,rec_loc_type);
		  if (rec_loc_type >= 0)
			   outputTable.setString(rec_loc_col, new_row, rec_loc);
		  outputTable.setInt(delv_loc_type_col, new_row, delv_loc_type);
		  if (delv_loc_type >= 0)
		     outputTable.setString(delv_loc_col, new_row, delv_loc);
		  outputTable.setInt(delv_id_col, new_row, dtl_delv_id);
		  outputTable.setInt(delv_vol_type_col, new_row, dtl_vol_type);
		  outputTable.setInt(delivery_loc_col, new_row, dtl_loc);
		  outputTable.setInt(facility_id_col, new_row, facility_id);
		  /*outputTable.setInt(massunit_col, new_row, massUnit);
		  outputTable.setInt(volunit_col, new_row, volumeUnit);
		  outputTable.setInt(energyunit_col, new_row, energyUnit);*/
		  outputTable.setInt(nom_transaction_type_col, new_row,dtl_nom_tran_type);
		  /*outputTable.setInt(location_id_col, new_row,location_id);*/

		  if(authNomTypesTableIsValid != 0)
		  {
			  intNomType = funcGetNomType(outputTable.getInt("nom_transaction_type", new_row),authNomTypesTable);
			  outputTable.setInt("nom_type", new_row, intNomType);
		  }

		  if(breakoutBAVOverrun == 1 )
		  {
		     if (intNomType == NOM_FLOW) 
		     {
			      flow_vol = flow_vol + dtl_qty;
			      outputTable.setDouble("flow_volume", new_row,dtl_qty);
		     }
		     if (intNomType == NOM_OVER) 
		     {
			      over_vol = over_vol + dtl_qty;
			      outputTable.setDouble("auth_overrun", new_row,	dtl_qty);
		     }
		  }
		  else
		  {
			   flow_vol = flow_vol + dtl_qty;
			   outputTable.setDouble("flow_volume", new_row,dtl_qty);
		  }
	       }
	    }
	    if (first_row != 0) 
	    {
	       if (ent_group == 2) 
	       {
		     flow_vol = flow_vol + bav_qty;
		     outputTable.setDouble("flow_volume", first_row, bav_qty);
	       }
	       if (ins_type == INS_TYPE_ENUM.comm_transport.toInt()
			      && ins_sub_type == INS_SUB_TYPE.comm_trans_entry_exit_flow.toInt()
			      || ins_type == INS_TYPE_ENUM.comm_storage.toInt()
			      && ins_sub_type == INS_SUB_TYPE.comm_stor_inj_wth_space.toInt()) 
	       {
		     // diff already there  
	       }
	       if (ins_type == INS_TYPE_ENUM.comm_transport.toInt()) 
	       {
		     diff = ent_min_qty - bav_qty;
		     stranded_vol = ent_max_qty - flow_vol;
	       } 
	       else if ((ins_type == INS_TYPE_ENUM.comm_storage.toInt()) || (ins_type == INS_TYPE_ENUM.comm_storage_balance.toInt())) 
	       {
		  if (ent_type == ENTITLEMENT_TYPE.ENTITLEMENT_TYPE_GROSS_WITHDRAW.toInt() 
		     || ent_type == ENTITLEMENT_TYPE.ENTITLEMENT_TYPE_NET_WITHDRAW.toInt()) 
		     {
			if (ent_min_qty < bav_qty)
			   diff = ent_min_qty - bav_qty;
			else
			   diff = ent_max_qty - bav_qty;
		     } 
		  else // if (ent_type ==
		       // ENTITLEMENT_TYPE.ENTITLEMENT_TYPE_GROSS_INJECT
		       // || ent_type ==
		       // ENTITLEMENT_TYPE.ENTITLEMENT_TYPE_NET_INJECT)
		     {
			if (ent_min_qty > bav_qty)
			   diff = ent_min_qty - bav_qty;
			else
			   diff = ent_max_qty - bav_qty;
		     }
		     stranded_vol = ent_max_qty - flow_vol;
		  }
		  outputTable.setDouble(diff_col, first_row, diff);
		  outputTable.setDouble(ent_min_qty_col, first_row,ent_min_qty);
		  outputTable.setDouble(ent_max_qty_col, first_row,ent_max_qty);
		  outputTable.setDouble("stranded_volume", first_row,	stranded_vol);
	       }
	    }
	 }
      
      /*massUnitCol = outputTable.getColNum("mass_massunit");
      volumeUnitCol = outputTable.getColNum("volume_volumeunit");
      energyUnitCol = outputTable.getColNum("energy_energyunit");

      doEnergyConversion(outputTable, unit_col, 
				   		 massUnitCol,  volumeUnitCol, energyUnitCol,
				   		 massUnit,  volumeUnit, energyUnit, 
				   		 deal_start_date_col, deal_maturity_date_col, location_id_col);*/
           
      return outputTable;
   }
   
   
    /* private static Table doEnergyConversion(Table outputTable, int unit_col, 
		   								   int massUnitCol, int volumeUnitCol, int energyUnitCol,
		   								   int massUnit, int volumeUnit, int energyUnit, 
		   								   int deal_start_date_col, int deal_maturity_date_col, int location_id_col) throws OException 
{
   APMUtility.doEnergyConversion(outputTable, 
			 unit_col, 
			 massColArray,[mass col pairs to be converted]
			 volumeColArray,[vol col pairs to be converted]
			 energyColArray,[energy col pairs to be converted]
			 massUnit,[modified target]
			 volumeUnit,[modified target] 
			 energyUnit,[modified target]
			 deal_start_date_col,
			 deal_maturity_date_col,
			 location_id_col);

   return outputTable;
}*/

   private static Table retrieveData(int iQueryID, /*int massUnit,
						     int volumeUnit,*/ boolean flipSigns, int calcAllVolumeStatuses,
				    int startPoint, int endPoint) throws OException 
   {
      Table dataTable = Table.tableNew();
      String sQuery;
      String startPointStr = "";
      String endPointStr = "";
      String dateConditionStr = "";
      
      if (startPoint != -1 || endPoint != -1) 
      {
	 dateConditionStr = " and ( ";
	 
	 if (startPoint != -1) 
	 {
	    startPointStr = OCalendar.formatJdForDbAccess(startPoint);
	    dateConditionStr = dateConditionStr	+ " ab_tran.maturity_date > '" + startPointStr + "' ";
	 }

	 if (endPoint != -1) 
	 {
	    endPointStr = OCalendar.formatJdForDbAccess(endPoint);
	    
	    if (startPoint != -1)
	       dateConditionStr = dateConditionStr + " and ";
	    
	    dateConditionStr = dateConditionStr + " ab_tran.start_date < '"	+ endPointStr + "' ";
	 }
      dateConditionStr = dateConditionStr + " ) ";
      }

      sQuery = " select distinct "
	       + "    ab_tran.deal_tracking_num, "
	       + "    ab_tran.ins_type, "
	       + "    ab_tran.ins_sub_type, "
	       + "    ab_tran.start_date, "
	       + "    ab_tran.maturity_date, "
	       + "    phys_header.pipeline_id, "
	       + "    phys_header.contract_number, "
	       + "    ins_parameter.unit, "
	       + "    phys_header.allow_pathing "
/*	       + "    gas_phys_location.index_id "*/
/*	       + "    gas_phys_param.location_id "*/
	       + " from "
	       + "    ab_tran join "
	       + "    phys_header on "
	       + "       phys_header.ins_num = ab_tran.ins_num join "
	       + "    gas_phys_param on "
	       + "       gas_phys_param.ins_num = phys_header.ins_num join "
	       + "    ins_parameter on "
	       + "       ab_tran.ins_num = ins_parameter.ins_num and ins_parameter.param_seq_num = 0 join "
/*		   + "    comm_schedule_header on "
		   + "       ab_tran.ins_num = comm_schedule_header.ins_num join "
		   + "    gas_phys_location on "
		   + "       gas_phys_location.location_id = comm_schedule_header.location_id join "*/
		   + "    query_result on "
	       + "       query_result.query_result = ab_tran.tran_num "
	       + " where "
	       + "    ab_tran.toolset = 36 "
	       + "    and ab_tran.base_ins_type in (48020, 48030, 48600) "
	       + "    and ab_tran.tran_status in (2, 3) "
	       + "    and (ab_tran.ins_sub_type = 9203 "
	       + "       or   ab_tran.ins_num in (select ins_num from swing_constraints) "
	       + "       or   ab_tran.ins_num in (select ins_num from contract_detail_entitlements) "
	       + "       or   (ab_tran.ins_sub_type = 9101 " /* check if master ta deal's subta deal has cde */
	       + "            and exists (select 1 from contract_detail_entitlements cde, ab_tran ab1, comm_subta_header tah, comm_subta_period tap " 
	       + "                where ab1.tran_status in (2, 3) and ab1.deal_tracking_num =  tap.deal_tracking_num and tap.subta_header_id=tah.subta_header_id "
	       + "                and ab_tran.start_date <= tap.period_end_date and ab_tran.maturity_date >= tap.period_start_date and tah.contract_id = phys_header.contract_id "
	       + "                and tap.update_status in (0, 2, 6) and tah.update_status in (0, 2, 6) )))"
/*	       + "    and gas_phys_param.location_id != 0 "*/
	       + dateConditionStr
	       + "    and   query_result.unique_id = " + iQueryID;

      com.olf.openjvs.DBase.runSqlFillTable(sQuery, dataTable);

   return dataTable;
   }

   static Table createOutputTable(Table tblMasterDetails) throws OException 
   {
      Table output = Table.tableNew();
      output = tblMasterDetails.cloneTable();
      output.addCol("ent_start_date", COL_TYPE_ENUM.COL_INT);
      output.addCol("ent_end_date", COL_TYPE_ENUM.COL_INT);
      output.addCol("entitlement_type", COL_TYPE_ENUM.COL_INT);
      output.addCol("service_type", COL_TYPE_ENUM.COL_INT);
      output.addCol("enforce", COL_TYPE_ENUM.COL_INT);
      output.addCol("entitlement_name", COL_TYPE_ENUM.COL_STRING);
      output.addCol("rec_loc_type", COL_TYPE_ENUM.COL_INT);
      output.addCol("rec_loc", COL_TYPE_ENUM.COL_STRING);
      output.addCol("del_loc_type", COL_TYPE_ENUM.COL_INT);
      output.addCol("del_loc", COL_TYPE_ENUM.COL_STRING);
      output.addCol("entitlement_min_qty", COL_TYPE_ENUM.COL_DOUBLE);
      output.addCol("entitlement_max_qty", COL_TYPE_ENUM.COL_DOUBLE);
      output.addCol("delivery_id", COL_TYPE_ENUM.COL_INT);
      output.addCol("volume_type", COL_TYPE_ENUM.COL_INT);
      output.addCol("location", COL_TYPE_ENUM.COL_INT);
      output.addCol("bav_quantity", COL_TYPE_ENUM.COL_DOUBLE);
      /*output.addCol("mass_massunit", COL_TYPE_ENUM.COL_INT);
      output.addCol("volume_volumeunit", COL_TYPE_ENUM.COL_INT);
      output.addCol("energy_energyunit", COL_TYPE_ENUM.COL_INT);*/
      output.addCol("difference", COL_TYPE_ENUM.COL_DOUBLE);
      output.addCol("facility_id", COL_TYPE_ENUM.COL_INT);

      // Overrun
      output.addCol("flow_volume", COL_TYPE_ENUM.COL_DOUBLE);
      output.addCol("auth_overrun", COL_TYPE_ENUM.COL_DOUBLE);
      output.addCol("stranded_volume", COL_TYPE_ENUM.COL_DOUBLE);
      output.addCol("nom_type", COL_TYPE_ENUM.COL_INT);
      output.addCol("entitlement_group", COL_TYPE_ENUM.COL_INT);
      output.addCol("service_type", COL_TYPE_ENUM.COL_INT);
      output.addCol("nom_transaction_type", COL_TYPE_ENUM.COL_INT);

      return output;
   }

   private static void formatResult(Table output) throws OException 
   {
      output.setColFormatAsRef("ins_type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
      output.setColFormatAsRef("ins_sub_type",SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE);
      output.setColFormatAsRef("entitlement_type",SHM_USR_TABLES_ENUM.ENTITLEMENT_TYPE_TABLE);
      output.setColFormatAsRef("service_type",SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE);
      output.setColFormatAsRef("enforce", SHM_USR_TABLES_ENUM.YES_NO_TABLE);
      output.setColFormatAsDate("ent_start_date");
      output.setColFormatAsDate("ent_end_date");
      output.setColFormatAsDate("deal_start_date");
      output.setColFormatAsDate("deal_maturity_date");
      output.setColFormatAsRef("rec_loc_type",SHM_USR_TABLES_ENUM.ENTITLE_LOC_TYPE_TABLE);
      output.setColFormatAsRef("del_loc_type",SHM_USR_TABLES_ENUM.ENTITLE_LOC_TYPE_TABLE);
      output.setColFormatAsRef("location",SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);
      output.setColFormatAsRef("volume_type",SHM_USR_TABLES_ENUM.GAS_VOLUME_TYPES_TABLE);
      output.setColFormatAsRef("pipeline_id",	SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);
      output.setColFormatAsRef("facility_id",	SHM_USR_TABLES_ENUM.FACILITY_TABLE);
      output.setColFormatAsNotnlAcct("entitlement_min_qty", Util.NOTNL_WIDTH,	Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
      output.setColFormatAsNotnlAcct("entitlement_max_qty", Util.NOTNL_WIDTH,	Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
      output.setColFormatAsNotnlAcct("bav_quantity", Util.NOTNL_WIDTH,Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
      output.setColFormatAsNotnlAcct("difference", Util.NOTNL_WIDTH,Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
/*      output.setColFormatAsRef("mass_massunit",SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
      output.setColFormatAsRef("volume_volumeunit",SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
      output.setColFormatAsRef("energy_energyunit",SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);*/
      output.setColFormatAsRef("unit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
      output.setColFormatAsRef("allow_pathing", SHM_USR_TABLES_ENUM.YES_NO_TABLE);

      output.setColFormatAsNotnlAcct("flow_volume", Util.NOTNL_WIDTH,	Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
      output.setColFormatAsNotnlAcct("auth_overrun", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
      output.setColFormatAsNotnlAcct("stranded_volume", Util.NOTNL_WIDTH,	Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

      output.setColTitle("allow_pathing", "Allow Pathing");
      /*output.setColTitle("index_id", "Index Id");*/
      output.setColTitle("contract_number", "Contract");
      output.setColTitle("pipeline_id", "Service provider");
      output.setColTitle("start_date", "Ent Start Date");
      output.setColTitle("maturity_date", "Ent Maturity Date");
      output.setColTitle("ent_start_date", "Start Date");
      output.setColTitle("ent_end_date", "End Date");
      output.setColTitle("deal_start_date", "Deal Start Date");
      output.setColTitle("deal_maturity_date", "Deal Maturity Date");
      output.setColTitle("entitlement_type", "Entitlement Type");
      output.setColTitle("service_type", "Service Type");
      output.setColTitle("enforce", "Enforce");
      output.setColTitle("entitlement_name", "Entitlement Name");
      output.setColTitle("rec_loc_type", "Rec Loc Type");
      output.setColTitle("rec_loc", "Rec Loc");
      output.setColTitle("del_loc_type", "Del Loc Type");
      output.setColTitle("del_loc", "Del Loc");
      output.setColTitle("entitlement_min_qty", "Entitlement Min Quantity");
      output.setColTitle("entitlement_max_qty", "Entitlement Max Quantity");
      output.setColTitle("bav_quantity", "BAV Quantity");
      output.setColTitle("difference", "Difference");
      output.setColTitle("deal_tracking_num", "Deal Num");
      output.setColTitle("location", "Location");
      output.setColTitle("volume_type", "Volume Type");
      output.setColTitle("delivery_id", "Delivery ID");
      output.setColTitle("ins_type", "Instrument Type");
      output.setColTitle("facility_id", "Facility");
      output.setColTitle("ins_sub_type", "Instrument Sub Type");
/*      output.setColTitle("massunit", "Mass Unit");
      output.setColTitle("volunit", "Volume Unit");
      output.setColTitle("energyunit", "Energy Unit");*/
      output.setColTitle("unit", "Unit");

/*      output.setColTitle("mass_massunit", "Mass Unit");
      output.setColTitle("volume_volumeunit", "Volume Unit");
      output.setColTitle("energy_energyunit", "Energy Unit");*/

      output.setColTitle("flow_volume", "BAV Flow");
      output.setColTitle("auth_overrun", "BAV Overrun");
      output.setColTitle("stranded_volume", "(Over)/Under");
      output.setColTitle("nom_type", "Nomination Type");
      output.setColTitle("entitlement_group", "Entitlement Group");
      output.setColTitle("nom_transaction_type", "Nom Transaction Type");
      
/*      APMUtility.formatEnergyConversion(output, massColArray, volumeColArray, energyColArray);*/
   }

   public static Table getNomTranTypeDBTable() throws OException 
   {
      String sCachedTableName = "APM Nom Tran Type";
      Table tNomTranTypes = Table.getCachedTable(sCachedTableName);
      if (Table.isTableValid(tNomTranTypes) == 0) 
      {
	 tNomTranTypes = Table.tableNew();
	 int retval = 0;
	 retval = DBaseTable.loadFromDbWithSQL(tNomTranTypes, "nom_tran_type_id, type_name", "nom_tran_type", "1=1");

	 if (retval != 0) 
	 {
	    int numRows = tNomTranTypes.getNumRows();
	    for (int iRow = 1; iRow <= numRows; iRow++) 
	    {
	       String volumeTypeStr = Str.toUpper(tNomTranTypes.getString(2, iRow));
	       tNomTranTypes.setString(2, iRow, volumeTypeStr);
	    }
	       tNomTranTypes.sortCol(2); /* sort by type_name so we can find the nom_tran_type_id by type_name */
	       Table.cacheTable(sCachedTableName, tNomTranTypes);
	 }
      }

      return tNomTranTypes;
   }

   public static int funcGetNomType(int intDtlNomTranType,	Table authNomTypesTable) throws OException 
   {
      int intNomType = NOM_FLOW;

      if (authNomTypesTable.findInt("nom_tran_type_id", intDtlNomTranType,SEARCH_ENUM.FIRST_IN_GROUP) > 0) 
      {
	    intNomType = NOM_OVER;
      } 
      else 
      {
	    intNomType = NOM_FLOW;
      }

      return intNomType;
   }

   private static Table getNomTranTypeTable(String authNomTypesStr) throws OException 
   {
      Table authNomTypesDBTable;
      Table authNomTypesTable = Table.tableNew();

      if (!authNomTypesStr.isEmpty()) 
      {
	 int rowFound = -1;
	 int newRow = 0;
	 int nomTranTypeId = -1;

	 authNomTypesDBTable = getNomTranTypeDBTable();

	 if (Str.isNull(authNomTypesStr) == 0 && Str.isEmpty(authNomTypesStr) == 0) 
	 {
	    String delims = "[,]";
	    String[] tokens = authNomTypesStr.split(delims);
	    authNomTypesTable.addCol("nom_tran_type_id",COL_TYPE_ENUM.COL_INT);
	    for (int i = 0; i < tokens.length; i++) 
	    {
	       rowFound = authNomTypesDBTable.findString("type_name",Str.toUpper(tokens[i]), SEARCH_ENUM.FIRST_IN_GROUP);
	       if (rowFound > 0) 
	       {
		  nomTranTypeId = authNomTypesDBTable.getInt(	"nom_tran_type_id", rowFound);
		  newRow = authNomTypesTable.addRow();
		  authNomTypesTable.setInt(1, newRow, nomTranTypeId);
	       }
	    }
	 }
      }
      return authNomTypesTable;
   }

   private static int funcGetEntitleGroup(int intEntitlementType) 
   {
      int intEntitleGroup = 0;

      switch (intEntitlementType) 
      {
      case 3: // Contract
      case 2: // contract Path
      case 1: // contract Delivery
      case 0: // contract Receipt
      case 14: // Maximum Daily Quantity
      case 15: // Net Receipt
	    intEntitleGroup = 1;
	    break;
      case 6: // Balance
      case 9: // Maximum Storage Qty
      case 16: // Balance-AvgDaily
      case 17: // Balance-Min
      case 18: // Balance-Max
	    intEntitleGroup = 2;
	    break;
      case 5: // Gross Inject
      case 8: // Net Inject
      case 10: // Maximum Daily Inject Qty
      case 12: // Maximum Monthly Inject Qty
	    intEntitleGroup = 3;
	    break;
      case 4: // Gross Withdraw
      case 7: // Net Withdraw
      case 11: // Maximum Daily Withdraw Qty
      case 13: // Maximum Monthly Withdraw Qty
	    intEntitleGroup = 4;
	    break;
      default:
	    intEntitleGroup = 0;
	    break;
      } // end switch

      return intEntitleGroup;
   }
}
