/* Released with version 27-Feb-2019_V17_0_7 of APM */

/*
File Name:                      APM_UDSR_BulkPhysVolumes.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Joseph Jiang
Creation Date:                  July 20, 2017
 
Revision History:
                                                
Script Type:                    User-defined simulation result

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    APM Bulk Physical Volumes result	

*/ 

package com.olf.result;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.result.APMUtility.APMUtility;

public class APM_UDSR_BulkPhysVolumes implements IScript
{
	/*  enums was not exported to jvs. */
	static int INV_VAL_TYPE_BALANCE = 0;
	static int INV_VAL_TYPE_FORWARD_BALANCE = 1;
	Table m_ValidTrans = null;
			   
    public void execute(IContainerContext contx) throws OException
    {
      Table argt = Util.NULL_TABLE, returnt = Util.NULL_TABLE;

      int intOperation;

      argt = contx.getArgumentsTable();
      returnt = contx.getReturnTable();      

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
      
      if(m_ValidTrans != null)
    	  m_ValidTrans.destroy();
   }

   public void compute_result(Table argt, Table returnt) throws OException 
   {
      int massUnit = -1;
      int volumeUnit = -1; 
      int energyUnit = -1; 
      int showAllVolumeStatuses = 0;
      int iAttributeGroup;
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

      strVal = APMUtility.GetParamStrValue(tblConfig, "APM Phys Volume Mass Unit");
      if (!strVal.isEmpty())
    	  massUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);
      
      strVal = APMUtility.GetParamStrValue(tblConfig, "APM Phys Volume Volume Unit");
      if (!strVal.isEmpty())
    	  volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);      

      strVal = APMUtility.GetParamStrValue(tblConfig, "APM Phys Volume Energy Unit");
      if (!strVal.isEmpty())
    	  energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);

      showAllVolumeStatuses = APMUtility.GetParamIntValue(tblConfig, "APM Phys Volume Show All Volume Statuses");      
      
      volumeTypesStr = APMUtility.GetParamStrValue(tblConfig, "APM Bulk Phys Vol. Volume Types");      
            
      
      // Set default mass and unit values, if not specified
      if (massUnit < 0)
    	  massUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MT");

      if (volumeUnit < 0)
    	  volumeUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "BBL");
      
      if (energyUnit < 0)
    	  energyUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "MWh");
      
      int iQueryID = generateQuery(argt);
      
      if (iQueryID > 0)
      {
    	  tblData = doCalculations(iQueryID, massUnit, volumeUnit, energyUnit, tblTrans, showAllVolumeStatuses, volumeTypesStr);
    	  
    	  Query.clear(iQueryID);
      }            
      
      if ( tblData.getNumRows() > 0 )
    	  returnt.select(tblData, "*", "deal_num GE 0 AND total_quantity NE 0.00");

      tblData.destroy();    
   }  

   public void format_result(Table returnt) throws OException 
   {
      returnt.setColTitle("deal_num", "Deal Number");
      returnt.setColTitle("tran_num", "Transaction\nNumber");
      returnt.setColTitle("ins_num", "Instrument\nNumber");
      returnt.setColTitle("param_seq_num", "Deal Side");
      returnt.setColTitle("profile_seq_num", "Deal Profile\nPeriod");
      returnt.setColTitle("schedule_id", "Schedule ID");
      returnt.setColTitle("delivery_id", "Delivery ID");
      returnt.setColTitle("parcel_id", "Parcel\nID");
      
      returnt.setColTitle("ins_type", "Instrument Type");
      returnt.setColFormatAsRef("ins_type", SHM_USR_TABLES_ENUM.INS_TYPE_TABLE);

      returnt.setColTitle("buy_sell", "Buy/Sell");
      returnt.setColFormatAsRef("buy_sell", SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
      
      returnt.setColTitle("movement_start", "Movement Start Date"); 
      returnt.setColFormatAsDate("movement_start");
      returnt.setColTitle("movement_end", "Movement End Date");
      returnt.setColFormatAsDate("movement_end");

      returnt.setColTitle("volume_type", "Volume Type");
      returnt.setColFormatAsRef("volume_type", SHM_USR_TABLES_ENUM.CRUDE_TSD_VOLUME_TYPES_TABLE);
      
      returnt.setColTitle("bav_flag", "Is BAV\nVolume?");
      returnt.setColFormatAsRef("bav_flag", SHM_USR_TABLES_ENUM.YES_NO_TABLE);

      returnt.setColTitle("cflow_type", "Cflow Type");
      returnt.setColFormatAsRef("cflow_type", SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE);
      
      returnt.setColTitle("event_source", "Event Source");
      returnt.setColFormatAsRef("event_source", SHM_USR_TABLES_ENUM.EVENT_SOURCE_TABLE);

		//  units
      returnt.setColTitle("unit", "Original\nUnit");
      returnt.setColFormatAsRef("unit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);

      returnt.setColTitle("volume_unit", "Volume\nUnit");
      returnt.setColFormatAsRef("volume_unit",
                                SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);

      returnt.setColTitle("mass_unit", "Mass\nUnit");
      returnt.setColFormatAsRef("mass_unit",
				SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);

      returnt.setColTitle("energy_unit", "Energy \n Unit");
      returnt.setColFormatAsRef("energy_unit",
				SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);

		//  quantity
      returnt.setColTitle("total_quantity", "Total Quantity");
      returnt.setColFormatAsNotnl("total_quantity", Util.NOTNL_WIDTH,
                                  Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);

      returnt.setColTitle("volume_quantity", "Volume");
      returnt.setColFormatAsNotnl("volume_volumeunit", Util.NOTNL_WIDTH,
                                  Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

      returnt.setColTitle("mass_quantity", "Mass");
      returnt.setColFormatAsNotnl("mass_massunit", Util.NOTNL_WIDTH,
                                  Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

      returnt.setColTitle("energy_quantity", "Energy");
      returnt.setColFormatAsNotnl("energy", Util.NOTNL_WIDTH,
                                  Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);

		// BAV
      returnt.setColTitle("bav_mass_quantity", "BAV Mass");
      returnt.setColFormatAsNotnl("bav_mass_quantity", Util.NOTNL_WIDTH,
                                  Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

      returnt.setColTitle("bav_volume_quantity", "BAV Volume");
      returnt.setColFormatAsNotnl("bav_volume_volumeunit", Util.NOTNL_WIDTH,
                                  Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
      
      returnt.setColTitle("bav_energy_quantity", "BAV Energy");
      returnt.setColFormatAsNotnl("bav_energy", Util.NOTNL_WIDTH,
                                  Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);
      
      
      returnt.colHide("pay_rec");		
      returnt.colHide("is_cflow");
      returnt.colHide("ins_source_id");
      returnt.colHide("event_source");
      
      returnt.clearGroupBy();
      returnt.addGroupBy("deal_num");
      returnt.addGroupBy("param_seq_num");
      returnt.addGroupBy("profile_seq_num");
      returnt.groupBy();      
   }

    private int generateQuery(Table argt) throws OException 
    {
 		int iQueryID = 0;
 	 	
 		int iResult = argt.getInt("result_type", 1);
 		Table tblTrans = argt.getTable("transactions", 1);
 		m_ValidTrans = tblTrans.cloneTable();
 	
 		for (int i = 1; i <= tblTrans.getNumRows(); i++) {
 			Transaction tranCurrent = tblTrans.getTran("tran_ptr", i);
 	
 			if (SimResult.isResultAllowedForTran(iResult, tranCurrent) > 0) {
 				tblTrans.copyRowAdd(i, m_ValidTrans);
 			}
 		}
 	
 		if (m_ValidTrans.getNumRows() > 0) {
 			iQueryID = Query.tableQueryInsert(m_ValidTrans, "tran_num");
 		}
 		return iQueryID;
    }
        
    private Table doCalculations(int iQueryID, int massUnit, int volumeUnit, int energyUnit, Table tblTrans,
                                           int calcAllVolumeStatuses,  String volumeTypesStr) throws OException
    {
       Table dataTable = retrieveData(iQueryID, calcAllVolumeStatuses, volumeTypesStr, tblTrans);
       int iNumRows = dataTable.getNumRows();

       int iPayRecCol = dataTable.getColNum("pay_rec");
       int iCflowCol = dataTable.getColNum("is_cflow");
       int iCflowTypeCol = dataTable.getColNum("cflow_type");
       int iQuantityCol = dataTable.getColNum("total_quantity");
       int iParcelIdCol = dataTable.getColNum("parcel_id");
       int iEventSourceIdCol = dataTable.getColNum("event_source");
       int iPricingGroupCol = dataTable.getColNum("pricing_group_num");
       int iPricingSourceCol = dataTable.getColNum("pricing_source");
       int iEDTCol = dataTable.getColNum("movement_end");
       int iInvValTypeCol = dataTable.getColNum("inv_val_type");
       int iFeeDefIdCol = dataTable.getColNum("fee_def_id");
       int iPriceLevelCol = dataTable.getColNum("price_adj_level");
       int iParcelPricing = PRICE_ADJ_LEVEL.PRICE_ADJ_LEVEL_PARCEL.toInt();
		
		// Now do the necessary post-processing
       for (int iRow = 1; iRow <= iNumRows; iRow++) 
       {
          int iIsCflow = dataTable.getInt(iCflowCol, iRow);
          int iCflowType = dataTable.getInt(iCflowTypeCol, iRow);
          int iPayRec = dataTable.getInt(iPayRecCol, iRow);  //
          int iPricingGroup = dataTable.getInt(iPricingGroupCol, iRow);
          int iPriceLevel = dataTable.getInt(iPriceLevelCol, iRow);
          int iPricingSource = dataTable.getInt(iPricingSourceCol, iRow);
          int iParcelId = dataTable.getInt(iParcelIdCol, iRow);
          int iInvValType = dataTable.getInt(iInvValTypeCol, iRow);
          int iFeeDefId = dataTable.getInt(iFeeDefIdCol, iRow);
          
          // Fix up the quantity by pay/receive flag only on phys side.
          if (iPayRec == 1 && iIsCflow == 0) {
             double quantity = dataTable.getDouble(iQuantityCol, iRow);
             quantity = -(quantity);
             dataTable.setDouble(iQuantityCol, iRow, quantity);
          }

          // For end time, we want to change 32767:0 to 32766:86400 for better
          // bucketing on client
          int iEndDate = dataTable.getDate(iEDTCol, iRow);
          int iEndTime = dataTable.getTime(iEDTCol, iRow);
          if (iEndTime == 0 &&  iIsCflow == 0)   // only for phys side
          {
             iEndDate--;
             iEndTime = 86400;
             dataTable.setDateTimeByParts(iEDTCol, iRow, iEndDate, iEndTime);
          }
			
          //  the logic to manipulate event_source is copied from getCashflowDetails API. 
          int iEventSourceId = EVENT_SOURCE.EVENT_SOURCE_PHYSCASH.toInt();  // set default 
          if (iIsCflow == 0) 
          {
             if (iCflowType == CFLOW_TYPE.PREMIUM_CFLOW.toInt())
             {
                iEventSourceId = (iParcelId < 0)? EVENT_SOURCE.EVENT_SOURCE_PHYSCASH.toInt() : EVENT_SOURCE.EVENT_SOURCE_PARCEL.toInt();
             }
             else if (iPricingGroup > 0)
             {
                if (iPricingSource == PRICING_SOURCE.PRICE_SOURCE_TRIGGER.toInt() && iPriceLevel == PRICE_ADJ_LEVEL.PRICE_ADJ_LEVEL_PARCEL.toInt())
                    iEventSourceId = EVENT_SOURCE.EVENT_SOURCE_PARCEL.toInt();
                else if (iPricingSource == PRICING_SOURCE.PRICE_SOURCE_INVENTORY_VALUE.toInt())
                {
                   if (iInvValType == INV_VAL_TYPE_BALANCE)
                       iEventSourceId = EVENT_SOURCE.EVENT_SOURCE_BALANCE.toInt();
                   else 
                       iEventSourceId = EVENT_SOURCE.EVENT_SOURCE_FORWARD_BALANCE.toInt();
                }
                else if (iPricingSource == PRICING_SOURCE.PRICE_SOURCE_FEE.toInt())
                {
                   if (iPriceLevel == iParcelPricing && iFeeDefId == FEE_TYPE_DEF.FEE_TYPE_DEF_PARCEL.toInt())					
                       iEventSourceId = EVENT_SOURCE.EVENT_SOURCE_PARCEL_COMMODITY.toInt();
                   else
                       iEventSourceId = EVENT_SOURCE.EVENT_SOURCE_PARCEL.toInt();
                }
                else if (iPricingSource == PRICING_SOURCE.PRICE_SOURCE_INTERBOOK.toInt())
                    iEventSourceId = EVENT_SOURCE.EVENT_SOURCE_FORWARD_BALANCE.toInt();
             }				        
             dataTable.setInt(iEventSourceIdCol, iRow, iEventSourceId);
          }	
       }

       dataTable.setColValInt("mass_unit", massUnit);
       dataTable.setColValInt("volume_unit", volumeUnit);
       dataTable.setColValInt("energy_unit", energyUnit);

       // Now do parcel level conversions
       dataTable.clearGroupBy();
       dataTable.addGroupBy("tran_num");
       dataTable.groupBy();
       int pTranNum = 0;
       int iMassQtyCol = dataTable.getColNum("mass_quantity");
       int iVolumeQtyCol = dataTable.getColNum("volume_quantity");
       int iEnergyQtyCol = dataTable.getColNum("energy_quantity");
       int iBavMassQtyCol = dataTable.getColNum("bav_mass_quantity");
       int iBavVolumeQtyCol = dataTable.getColNum("bav_volume_quantity");
       int iBavEnergyQtyCol = dataTable.getColNum("bav_energy_quantity");
       double convFactorMass = 0.0, convFactorVolume = 0.0, convFactorEnergy = 0.0;
       
       for (int iRow = 1; iRow <= iNumRows; iRow++) 
       {
          int tranNum = dataTable.getInt("tran_num", iRow);
          int tranRow = tblTrans.unsortedFindInt("tran_num", tranNum);
          Transaction tranCurrent = tblTrans.getTran("tran_ptr", tranRow);
          
          int dealLeg = dataTable.getInt("param_seq_num", iRow);
          int dealProfile = dataTable.getInt("profile_seq_num", iRow);
          int origUnit = dataTable.getInt("unit", iRow);
          int iParcelID = dataTable.getInt(iParcelIdCol, iRow);
          
          double volume = dataTable.getDouble("total_quantity", iRow);
          int bavFlag = dataTable.getInt("bav_flag", iRow);
          
          if (pTranNum != tranNum)
          {
             pTranNum = tranNum;
          }

          convFactorMass = APMUtility.GetUnitConversionFactorByParcel( origUnit, massUnit, tranCurrent, dealLeg, dealProfile, iParcelID);
          convFactorVolume = APMUtility.GetUnitConversionFactorByParcel( origUnit, volumeUnit, tranCurrent, dealLeg, dealProfile, iParcelID);
          convFactorEnergy = APMUtility.GetUnitConversionFactorByParcel( origUnit, energyUnit, tranCurrent, dealLeg, dealProfile, iParcelID);

          dataTable.setDouble(iMassQtyCol, iRow, volume * convFactorMass);
          dataTable.setDouble(iVolumeQtyCol, iRow, volume * convFactorVolume);
          dataTable.setDouble(iEnergyQtyCol, iRow, volume * convFactorEnergy);

          dataTable.setDouble(iBavMassQtyCol, iRow, volume * bavFlag * convFactorMass);
          dataTable.setDouble(iBavVolumeQtyCol, iRow, volume * bavFlag * convFactorVolume);
          dataTable.setDouble(iBavEnergyQtyCol, iRow, volume * bavFlag * convFactorEnergy);
       }

       dataTable.delCol("pricing_group_num");
       dataTable.delCol("price_adj_level");
       dataTable.delCol("inv_val_type");
       dataTable.delCol("pricing_source");
       dataTable.delCol("fee_def_id");
       
       return dataTable;
    }

    private Table retrieveData(int iQueryID, int calcAllVolumeStatuses, String volumeTypesStr, Table tblTrans) throws OException
    {
       Table dataTable = Table.tableNew();
       String sQuery;
       String sVolumeStatusCheck = "";
       String sVolumeTypes = "";
       String isNull = com.olf.openjvs.DBase.getDbType() == DBTYPE_ENUM.DBTYPE_ORACLE.toInt() ? "nvl" : "isnull";

       if (calcAllVolumeStatuses == 0) {
          sVolumeStatusCheck = " csh.bav_flag = 1 and ";
       }
       else
           sVolumeTypes = GetShowAllVolumeTypesQueryString(volumeTypesStr);
			

       sQuery = " select "
               + "    t.ins_type, t.buy_sell, t.deal_tracking_num deal_num, t.tran_num, t.ins_num, csh.param_seq_num, "
               + "    csh.profile_seq_num, csh.parcel_id, csh.schedule_id, csh.delivery_id, "
               + isNull + "(physcash.cflow_type, -1) cflow_type, csh.volume_type, csh.bav_flag, "  
               + "    csh.gmt_start_date_time movement_start, csh.gmt_end_date_time movement_end, "
               + "    csh.total_quantity, csh.unit, "
               + "    0 mass_unit, 0 volume_unit, 0 energy_unit, " // Will be used for conversion.
               + "    ip.pricing_source, 0 event_source, " //  event_source is manipulated
               + "    p.pay_rec, ph.price_adj_level, " + isNull +"(fee.fee_def_id,0) fee_def_id, "
               + isNull + "(physcash.cflow_id, -1) ins_source_id, ip.pricing_group_num, "
               + "    0 is_cflow, -1 inv_val_type "
               + " from query_result qr "
               + " join ab_tran t on qr.query_result = t.tran_num and t.toolset = "  + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt()   //limit phys volumes to commodity toolset for now
               + " join phys_header ph on t.ins_num = ph.ins_num "
               + " join parameter p on p.ins_num = ph.ins_num " 
               + " join gas_phys_param gpp on p.ins_num = gpp.ins_num and p.param_seq_num = gpp.param_seq_num "
               + " join ins_price ip on p.ins_num = ip.ins_num and p.param_seq_num = ip.param_seq_num "
               + "    and ((gpp.class_of_business = 0 and ip.pricing_source = 1) or (gpp.class_of_business != 0 and ip.pricing_source = 0) or (gpp.class_of_business = 0 and ph.price_adj_level = 0  and ip.pricing_source = 0))"
               + " left join ins_fee_param fee on ip.ins_num = fee.ins_num and fee.param_seq_num = ip.param_seq_num and ip.pricing_source_id = fee.fee_seq_num "
               + " join comm_schedule_header csh on p.ins_num = csh.ins_num and p.param_seq_num = csh.param_seq_num"
               + "    and ((gpp.class_of_business = 0 and fee.parcel_id = csh.parcel_id) or (gpp.class_of_business != 0 and csh.profile_seq_num = ip.pricing_source_id) or (gpp.class_of_business = 0 and ph.price_adj_level = 0 and csh.profile_seq_num = ip.pricing_source_id)) and "
               + sVolumeStatusCheck  // If looking for BAV only, add here
               + sVolumeTypes        // if only a subset of the volume types add here
               + "    csh.total_quantity != 0.0 "    // Don't want zero-valued entries cluttering the result
               + " left join physcash on t.ins_num = physcash.ins_num and ip.param_seq_num = physcash.param_seq_num and ip.pricing_group_num = physcash.pricing_group_num "
               + " where qr.unique_id = " + iQueryID
               + " order by  deal_num ";

       try 
       {
          com.olf.openjvs.DBase.runSqlFillTable(sQuery, dataTable);
       }
       catch (Exception e)
       {
          OConsole.print("ERROR: " + e.getMessage());
       }
           
       dataTable.addCol("bav_mass_quantity", COL_TYPE_ENUM.COL_DOUBLE);
       dataTable.addCol("bav_volume_quantity", COL_TYPE_ENUM.COL_DOUBLE);
       dataTable.addCol("bav_energy_quantity", COL_TYPE_ENUM.COL_DOUBLE);
       dataTable.addCol("mass_quantity", COL_TYPE_ENUM.COL_DOUBLE);
       dataTable.addCol("volume_quantity", COL_TYPE_ENUM.COL_DOUBLE);
       dataTable.addCol("energy_quantity", COL_TYPE_ENUM.COL_DOUBLE);
           
       // bring in forward balance
       getRowsFromCashflow(dataTable, tblTrans);

       return dataTable;
    }

    /* 
     *   get balance and forward balance volume from cash flow.
     */
    private void getRowsFromCashflow(Table dataTable, Table tblTrans) throws OException
    {
       Table cflowTable = Util.NULL_TABLE;
       Table tmpTable = dataTable.cloneTable();		
       int numRows = m_ValidTrans.getNumRows(); 
       int prevTranNum = Util.NOT_FOUND;
       int tranNum =0;
       
       int iTranNumCol = dataTable.getColNum("tran_num");
       int iSideCol = dataTable.getColNum("param_seq_num");
       int iIsCflowCol = dataTable.getColNum("is_cflow");
       int iCflowTypeCol = dataTable.getColNum("cflow_type");
       int iQuantityCol = dataTable.getColNum("total_quantity");
       int iParcelIdCol = dataTable.getColNum("parcel_id");
       int iEventSourceIdCol = dataTable.getColNum("event_source");
       int iInsSourceIdCol = dataTable.getColNum("ins_source_id");
       int iSDTCol = dataTable.getColNum("movement_start");
       int iEDTCol = dataTable.getColNum("movement_end");
       int iDeliveryIdCol = dataTable.getColNum("delivery_id");
       int iScheduleIdCol = dataTable.getColNum("schedule_id");
       int iBuySellCol = dataTable.getColNum("buy_sell");
       int iVolumeTypeCol = dataTable.getColNum("volume_type");
       int iUnitCol = dataTable.getColNum("unit");
       
       int CFLOW_FORWARD_BALANCE = CFLOW_TYPE.FORWARD_BALANCE_CFLOW.toInt();
       int CFLOW_BALANCE = CFLOW_TYPE.BALANCE_CFLOW.toInt();
       int clfowTypeCol = -1;
       int clfowQtyCol = -1;
       int cfSideCol = -1;
       int cfInsSourceIdCol = -1;
       int cfParcelIdCol = -1 ;
       int cfEventSourceIdCol = -1;		
       int cfVolumeTypeCol = -1;
       int cfSDTCol = -1;
       int cfEDTCol = -1;
       int cfDeliveryIdCol = -1;
       int cfUnitCol = -1;
       int cfTranNumCol = -1;
       
       cflowTable = getParcelAttributesSimResult(tblTrans);
       cflowTable.clearGroupBy();
       cflowTable.group("tran_num");
       
       dataTable.clearGroupBy();
       dataTable.group("tran_num");
       try
       {
          for (int row = numRows; row > 0; row--)
          {
        	 tranNum = m_ValidTrans.getInt("tran_num", row);
             if (prevTranNum != tranNum)
             {
                prevTranNum = tranNum;
                if (clfowTypeCol == -1)
                {
                   clfowTypeCol = cflowTable.getColNum("cflow_type");
                   clfowQtyCol = cflowTable.getColNum("quantity");
                   cfSideCol = cflowTable.getColNum("param_seq_num");
                   cfInsSourceIdCol = cflowTable.getColNum("ins_source_id");
                   cfParcelIdCol = cflowTable.getColNum("parcel_id");
                   cfEventSourceIdCol = cflowTable.getColNum("event_source");		
                   cfVolumeTypeCol = cflowTable.getColNum("volume_type");
                   cfSDTCol = cflowTable.getColNum("movement_start_date");
                   cfEDTCol = cflowTable.getColNum("movement_end_date");
                   cfDeliveryIdCol = cflowTable.getColNum("delivery_id");
                   cfUnitCol = cflowTable.getColNum("uom");
                   cfTranNumCol = cflowTable.getColNum("tran_num");
                }
		
                int cfStartRow = cflowTable.findInt(cfTranNumCol, tranNum, SEARCH_ENUM.FIRST_IN_GROUP);
                int cfEndRow = cflowTable.findInt(cfTranNumCol, tranNum, SEARCH_ENUM.LAST_IN_GROUP);
                if (cfEndRow > 0) 
                {
                   tmpTable.clearRows();
                   for (int iRow = cfStartRow; iRow <= cfEndRow; iRow++)
                   {
                      int cflowType = cflowTable.getInt(clfowTypeCol, iRow);
                      if (cflowType == CFLOW_FORWARD_BALANCE || cflowType == CFLOW_BALANCE && cflowTable.getDouble(clfowQtyCol, iRow) != 0.0)
                      {
                         int lastRow = tmpTable.addRow();
                         int dataTableRow = dataTable.findInt(iTranNumCol, tranNum, SEARCH_ENUM.FIRST_IN_GROUP);
                         if(dataTableRow == -1)
                         {
                        	 tmpTable.setInt("deal_num", lastRow, cflowTable.getInt("deal_tracking_num", iRow));
                        	 tmpTable.setInt("tran_num", lastRow, cflowTable.getInt("tran_num", iRow));
                        	 tmpTable.setInt("ins_num", lastRow, cflowTable.getInt("ins_num", iRow));
                        	 
                        	 // Handle the ins_type which is not inside of cflowtable
                        	 Transaction tran = m_ValidTrans.getTran("tran_ptr", row);
                        	 int ins_type = tran.getInsType();
                        	 tmpTable.setInt("ins_type", lastRow, ins_type);
                         }
                         else
                         {
                        	 dataTable.copyRow(dataTableRow, tmpTable, lastRow);
                         }
                         
                         tmpTable.setInt(iSideCol, lastRow, cflowTable.getInt(cfSideCol, iRow));
                         tmpTable.setInt(iInsSourceIdCol, lastRow, cflowTable.getInt(cfInsSourceIdCol, iRow));
                         tmpTable.setInt(iCflowTypeCol, lastRow, cflowType);
                         tmpTable.setInt(iParcelIdCol, lastRow,  cflowTable.getInt(cfParcelIdCol, iRow));
                         tmpTable.setInt(iEventSourceIdCol, lastRow,  cflowTable.getInt(cfEventSourceIdCol, iRow));
                         tmpTable.setInt(iVolumeTypeCol, lastRow,  cflowTable.getInt(cfVolumeTypeCol, iRow));
                         tmpTable.setInt(iScheduleIdCol, lastRow, 0);
                         tmpTable.setInt(iBuySellCol, lastRow, -1);
                         tmpTable.setInt(iIsCflowCol, lastRow, 1);
                         tmpTable.setDouble(iQuantityCol, lastRow,  cflowTable.getDouble(clfowQtyCol, iRow));
                         tmpTable.setDateTime(iSDTCol, lastRow,  cflowTable.getDateTime(cfSDTCol, iRow));
                         tmpTable.setDateTime(iEDTCol, lastRow,  cflowTable.getDateTime(cfEDTCol, iRow));
                         tmpTable.setInt(iDeliveryIdCol, lastRow,  cflowTable.getInt(cfDeliveryIdCol, iRow));
                         tmpTable.setInt(iUnitCol, lastRow,  cflowTable.getInt(cfUnitCol, iRow));
                      }
                   }
                   tmpTable.copyRowAddAll(dataTable);
                }
             }
          }
       }
       catch (OException e) 
       {
          String errMsg = "\nWarning:  Tran# "+tranNum +" failed to get cashflow table. \nMessage from core code: '" + e.getMessage()+"'\n";
          OConsole.oprint(errMsg);
       }			
       finally
       {	
          tmpTable.destroy();
          if (cflowTable != null)
              cflowTable.destroy();
       }
    }

   private static String GetShowAllVolumeTypesQueryString(String volumeTypes) throws OException
   {
      Table volumeTypeList;
      int startStringPosition, endStringPosition, iFoundRow, iRow;
      int numRowsAdded = 0;
      String temporaryVolumeTypes, volumeTypeStr, volumeTypesStripped;
      Table tMasterListCrudeVolumeTypes;
      String sql_query = "";
      String volTypesSQL = "";
	   
      volumeTypeList = Table.tableNew( "volume_types" );
      volumeTypeList.addCol("volume_type", COL_TYPE_ENUM.COL_STRING );
      volumeTypeList.addCol("volume_type_ID", COL_TYPE_ENUM.COL_INT );
	   
      tMasterListCrudeVolumeTypes = GetMasterVolumeTypes();

      startStringPosition = 1;
      endStringPosition = 1;
	   
      // now split the volume types apart so that we can create an ID comma separated string
      volumeTypesStripped = Str.stripBlanks(volumeTypes);
      if( Str.isNull( volumeTypesStripped ) == 0 && Str.isEmpty( volumeTypesStripped ) == 0 )
      {
         while( endStringPosition > 0 )
         {
            startStringPosition = 0;
            endStringPosition = Str.findSubString( volumeTypes, "," );
                  
            numRowsAdded += 1;           
            volumeTypeList.addRow();
                  
            if ( endStringPosition > 0 )
            {
               volumeTypeList.setString(1, numRowsAdded, Str.substr( volumeTypes, startStringPosition, endStringPosition - startStringPosition ) );
                     
               temporaryVolumeTypes = Str.substr( volumeTypes, endStringPosition + 1, Str.len( volumeTypes ) - endStringPosition - 1 );
               volumeTypes = temporaryVolumeTypes;
            }
            else
            {
               volumeTypeList.setString(1, numRowsAdded, Str.substr( volumeTypes, startStringPosition, Str.len( volumeTypes ) - startStringPosition ) );
            }            
         }  

         // if no rows then exit as we should have something
         if ( volumeTypeList.getNumRows() < 1 )
         {
            volumeTypeList.destroy();
            OConsole.print("APM Bulk Phys Volume Types field populated but no valid values !!! Please correct the simulation result parameter.\n");
            Util.exitFail();
         }

         // now we have a table of statuses - next job is to convert them into ID's
         for (iRow = 1; iRow <= volumeTypeList.getNumRows(); iRow++)
         {
            volumeTypeStr = Str.toUpper(volumeTypeList.getString(1, iRow));

            // BAV handled separately
            if ( Str.equal(volumeTypeStr, "BAV") == 1)
               continue;

            // try to find them in the crude list first
            iFoundRow = tMasterListCrudeVolumeTypes.findString(1, volumeTypeStr, SEARCH_ENUM.FIRST_IN_GROUP);
            if ( iFoundRow > 0 )
            {
               if ( Str.len(volTypesSQL) > 0 )
                  volTypesSQL = volTypesSQL + ",";

               volTypesSQL = volTypesSQL + Str.intToStr(tMasterListCrudeVolumeTypes.getInt(2, iFoundRow));
            }
         }

         // filter on volume type in comm_schedule_header - note this does not include the BAV type
         if ( Str.len(volTypesSQL) > 0 )
            sql_query += " (csh.volume_type in (" + volTypesSQL + ")";

         // check for BAV
         volumeTypeList.sortCol(1);
         iFoundRow = volumeTypeList.findString(1, "BAV", SEARCH_ENUM.FIRST_IN_GROUP);
         if ( Str.len(volTypesSQL) > 0 ) // if BAV required then we need an OR on the bav_flag
         {
            if ( iFoundRow > 0 )
            {
               sql_query += " or csh.bav_flag = 1) and ";
            }
            else
               sql_query += ") and ";
         }
         else  // only value is BAV - same effect as setting show all statuses = 0
            sql_query += " csh.bav_flag = 1 and ";
      }

      if ( volumeTypeList != null && Table.isTableValid(volumeTypeList) == 1)
          volumeTypeList.destroy();

      return sql_query;
   }

   private static Table GetMasterVolumeTypes() throws OException
   {
	   // Prepare the master volume types table from db - this is used to get ID's from specified volume statuses
	   String sCachedTableName = "APM Crude Volume Types";
	   
	   Table tMasterListVolumeTypes = Table.getCachedTable(sCachedTableName);
	   if (Table.isTableValid(tMasterListVolumeTypes) == 0)
	   {
	      tMasterListVolumeTypes = Table.tableNew();
	      int retval = 0;
	      retval = DBaseTable.loadFromDbWithSQL(tMasterListVolumeTypes, "crude_name, id_number",  "volume_type",  "crude_active_flag = 1");
	    	  
	      if( retval != 0)
	      {
	         // uppercase them so that any case sensitive stuff does not rear its ugly head
	         int numRows = tMasterListVolumeTypes.getNumRows();
	         for (int iRow = 1; iRow <= numRows; iRow++)
	         {
	            String volumeTypeStr = Str.toUpper(tMasterListVolumeTypes.getString(1, iRow));
	            tMasterListVolumeTypes.setString( 1, iRow, volumeTypeStr);
	         }

	         // sort so that the types can be easily found
	         tMasterListVolumeTypes.sortCol(1);
	         Table.cacheTable(sCachedTableName, tMasterListVolumeTypes);
	      }
	   }	
	   
	   return tMasterListVolumeTypes;
   }

   private static Table getParcelAttributesSimResult(Table transactions) throws OException
   {
	   Table retTab = null, resultList = null;
	   int numTrans = transactions.getNumRows();
	   Transaction tran;
	   Reval rData = null;
	   
	   if (numTrans > 0)
	   { 
		   rData = Reval.create();			
		   resultList = Sim.createResultListForSim();
		   SimResult.addResultForSim(resultList, SimResultType.create("USER_RESULT_APM_PARCEL_ATTRIBUTES"));	
		   int simDefId = resultList.getInt(1, 1);
		   
		   for(int x=1; x<=numTrans; x++)
		   {
			   tran = transactions.getTran("tran_ptr", x);
			   rData.addTransaction(tran);
		   }
		   
		   Table results = rData.computeResults(resultList);
		   Table genResults = results.getTable(1, 4);
		   retTab = SimResult.findGenResultTable(genResults, simDefId, -2, -2, -2).copyTable();
	   }
	   
	   if (resultList != null)
		   resultList.destroy();
	   if (rData != null)
		   rData.destroy();

	   return retTab;
   }
}
