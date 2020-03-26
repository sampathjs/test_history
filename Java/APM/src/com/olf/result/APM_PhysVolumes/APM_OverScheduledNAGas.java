/* Released with version 07-Nov-2014_V14_1_14 of APM */

/*
File Name:                      APM_PhysVolumesOverScheduled.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Joe Gallagher 
Creation Date:                  June 30, 2014
 
Revision History:
                                                
Script Type:                    User-defined simulation result

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    APM Physical Volumes result	

*/ 

package com.olf.result.APM_PhysVolumes;

import java.util.Vector;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.result.APM_PhysVolumes.APM_PhysVolumesUtilNAGas;
import com.olf.result.APMUtility.APMUtility;

public class APM_OverScheduledNAGas implements IScript
{
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
         compute_result(argt, returnt);
      } 
      else if (intOperation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt()) 
      {
         format_result(returnt);
      }
   }

   public void compute_result(Table argt, Table returnt)
         throws OException 
   {
	  int massUnit = -1;
	  int volumeUnit = -1; 
	  int energyUnit = -1; 
	  int showAllVolumeStatuses = 0;
	  int splitByDay = 0;
	  int iAttributeGroup;
	  int startPoint = 0;
	  int endPoint = 0;	  
	  String volumeTypesStr = "";
	  String strVal;
	  Table tblData = Table.tableNew();
	  Table tblAttributeGroups, tblConfig = null;      
	  Table tblTrans = argt.getTable("transactions", 1);
	  
      // Process parameters    
      tblAttributeGroups = SimResult.getAttrGroupsForResultType(argt.getInt("result_type", 1));

      if(tblAttributeGroups.getNumRows() > 0)
      {
    	  iAttributeGroup = tblAttributeGroups.getInt("result_config_group", 1);
    	  tblConfig = SimResult.getResultConfig(iAttributeGroup);
    	  tblConfig.sortCol("res_attr_name");
      }

      if (APMUtility.ParamHasValue(tblConfig, "APM Phys Volume Mass Unit") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM Phys Volume Mass Unit");
    	  if (!strVal.isEmpty())
    	  {
    		  massUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);
    	  }
      }

      if (APMUtility.ParamHasValue(tblConfig, "APM Phys Volume Volume Unit") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM Phys Volume Volume Unit");
       	  if (!strVal.isEmpty())
    	  {
    		  volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);
    	  }
      }

      if (APMUtility.ParamHasValue(tblConfig, "APM Phys Volume Energy Unit") > 0)
      {
    	  strVal = APMUtility.GetParamStrValue(tblConfig, "APM Phys Volume Energy Unit");
    	  if (!strVal.isEmpty())
    	  {
    		  energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);
    	  }
      }

      if (APMUtility.ParamHasValue(tblConfig, "APM Phys Volume Show All Volume Statuses") > 0)
      {
    	  showAllVolumeStatuses = APMUtility.GetParamIntValue(tblConfig, "APM Phys Volume Show All Volume Statuses");
    	  showAllVolumeStatuses = 1;/*BAV flag always on for over scheduled*/
      }      

      if (APMUtility.ParamHasValue(tblConfig, "APM Phys Volume Split By Day") > 0)
      {
    	  splitByDay = APMUtility.GetParamIntValue(tblConfig, "APM Phys Volume Split By Day");  
    	  splitByDay = 1;
      }      

      if (APMUtility.ParamHasValue(tblConfig, "APM Phys Volume Natural Startpoint") > 0)
      {
    	  startPoint = APMUtility.GetParamDateValue(tblConfig, "APM Phys Volume Natural Startpoint");      
      }
      else
      {
    	  startPoint = OCalendar.parseString("-4lom");
   	  }

      if (APMUtility.ParamHasValue(tblConfig, "APM Phys Volume Natural Endpoint") > 0)
      {
    	  endPoint = APMUtility.GetParamDateValue(tblConfig, "APM Phys Volume Natural Endpoint");      
      }
      else
      {
    	  endPoint = OCalendar.parseString("2lom");
      }

   	  volumeTypesStr = "Nominated,Trading";
      
      // Set default mass and unit values, if not specified
      if (massUnit < 0)
      {
    	  massUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MT");
      }
      if (volumeUnit < 0)
      {
    	  volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MCF");
      }
      if (energyUnit < 0)
      {
    	  energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MMBTU");
      }       
      
      Vector<Integer> allowedInsTypes = new Vector<Integer>();               
      
      // Generate a query on all transactions - instrument types list is empty means
      // all are allowed (yes, physical volumes will provide data for storage and transit
      // deals - this is intentional).
      int iQueryID = APM_PhysVolumesUtilNAGas.generateQuery(argt, allowedInsTypes);
      
      if (iQueryID > 0)
      {
    	  tblData = APM_PhysVolumesUtilNAGas.doCalculations(iQueryID, massUnit, volumeUnit, energyUnit, tblTrans, false, 
    			  									  showAllVolumeStatuses, APM_PhysVolumesUtilNAGas.APM_ConversionStyle.DELIVERY_BASED_CONVERSION,
    			  									  startPoint, endPoint, volumeTypesStr);
    	  
    	  if (splitByDay > 0)
    	  {
    		  Table splitTable = APM_PhysVolumesUtilNAGas.splitIntoDays(tblData);
    		  tblData.destroy();
    		  tblData = splitTable;
    	  }
    	  
	
    	  tblData = APM_PhysVolumesUtilNAGas.calculateOverScheduledQuantity(tblData);
    	  Query.clear(iQueryID);
      }            
      
      if ( tblData.getNumRows() > 0 )
    	  returnt.select(tblData, "*", "deal_num GE 0 AND over_sched_volume GT 0.00");
   }  

   public void format_result(Table returnt) throws OException 
   {
	   APM_PhysVolumesUtilNAGas.formatResult(returnt);
	   returnt.setColTitle("nominated_volume", "Nominated Volume");
	   returnt.setColFormatAsNotnl("nominated_volume", Util.NOTNL_WIDTH,Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
		
	   returnt.setColTitle("over_sched_volume", "Overscheduled Quantity");
	   returnt.setColFormatAsNotnl("over_sched_volume", Util.NOTNL_WIDTH,Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
	   
	   returnt.setColTitle("buy_sell", "Buy/Sell");
	   
   }
}
