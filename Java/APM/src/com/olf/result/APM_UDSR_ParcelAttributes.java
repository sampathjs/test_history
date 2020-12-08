/* Released with version 05-Feb-2020_V17_0_8 of APM */
/*
File Name:                 APM_UDSR_ParcelAttributes.java

Date Of Last Revision:     10-Apr-2015 - New UDSR
			   			   19-Aug-2015 - use core api to get parcel attributes from trans.
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back Parcel Attributes from core db tables.
                            
*/

package com.olf.result;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.result.APMUtility.APMUtility;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)

public class APM_UDSR_ParcelAttributes implements IScript{
   private boolean bIsTitleXferLocFromLastActivity = false; /* Title Transfer is based on Last Activity's Location */
   private boolean bIsFwdBalMovmtDateFromPrcParcel = false; /* Movement Date on Forward Balance based on Pricing Parcel */

    public void execute(IContainerContext context) throws OException 
    {
       Table argt = context.getArgumentsTable();
       Table returnt = context.getReturnTable();

       int operation;

       operation = argt.getInt( "operation", 1);
       
       if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt() )
           compute_result(argt, returnt);
       else if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt() )
           format_result(returnt);
       
       Util.exitSucceed();
    }

	/*-------------------------------------------------------------------------------
          Description:   Parcel Attributes result using core db tables.
          -------------------------------------------------------------------------------*/
    void compute_result(Table argt, Table returnt) throws OException
    {    
		Table tblTrans = argt.getTable("transactions", 1);		
		Table tranStrategy = Util.NULL_TABLE;
		Table result = Util.NULL_TABLE;
		Table simResult = Util.NULL_TABLE;
		
		APMUtility.APM_Print("APM_UDSR_ParcelAttributes: loading result table for " + tblTrans.getNumRows() + " trans." );
		
		retrieve_sim_parameters(argt); 
		
		for (int i = 1; i <= tblTrans.getNumRows(); i++)
		{
			Transaction tran = tblTrans.getTran("tran_ptr", i);
			int int_bunit = tran.getInternalBunit();
			int int_portfolio = tran.getInternalPortfolio();
			
			if (1 == i) 
			{
				simResult = tran.getCashflowDetailsSchema();
			}
			
			try
			{
				result = tran.getCashflowDetails(Util.NULL_TABLE);
	
				if (null == result || result == Util.NULL_TABLE || result.getNumRows() == 0) 
				{
					tranStrategy = tran.getStrategies();
					tranStrategy.addCol("int_bunit", COL_TYPE_ENUM.COL_INT);
					tranStrategy.addCol("int_portfolio", COL_TYPE_ENUM.COL_INT);
					tranStrategy.addCol("quantity", COL_TYPE_ENUM.COL_DOUBLE);
					tranStrategy.addCol("uom", COL_TYPE_ENUM.COL_INT);
					tranStrategy.addCol("rec_del", COL_TYPE_ENUM.COL_INT);
					tranStrategy.addCol("price_unit", COL_TYPE_ENUM.COL_INT);
					tranStrategy.addCol("cflow_type", COL_TYPE_ENUM.COL_INT);
					
					double contractSize = 0
						 , position = tran.getFieldDouble(TRANF_FIELD.TRANF_POSITION);
					int toolset = tran.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), 0, "");
					
					if (toolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt())
					{
						Transaction holdingTran = tran.getHoldingTran();
						contractSize = holdingTran.getFieldDouble(TRANF_FIELD.TRANF_NOTNL, 0, "");
					}
					
					for (int x=1; x<= tranStrategy.getNumRows(); x++) {
						int side = tranStrategy.getInt("deal_leg", x);
						int uom = tran.getFieldInt(TRANF_FIELD.TRANF_UNIT, side, "");
						int priceUnit = tran.getFieldInt(TRANF_FIELD.TRANF_PRICE_UNIT, side, "");
						int buySell = tran.getFieldInt(TRANF_FIELD.TRANF_BUY_SELL, side, "");
						int cashflowType = tran.getFieldInt(TRANF_FIELD.TRANF_CFLOW_PHYSCASH_TYPE, side, "");
						if (toolset == TOOLSET_ENUM.COMMODITY_TOOLSET.toInt())
						{
							contractSize = tran.getFieldDouble(TRANF_FIELD.TRANF_NOTNL, side, "");
						}

							tranStrategy.setInt("int_bunit", x, int_bunit);
							tranStrategy.setInt("int_portfolio", x, int_portfolio);
						tranStrategy.setInt("uom", x, uom);
						tranStrategy.setInt("price_unit", x, priceUnit);
						
						if (toolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt())
						{
							int eventSource = tranStrategy.getInt("event_source", x);
							if (eventSource == EVENT_SOURCE.EVENT_SOURCE_PROFILE.toInt())
							{
								cashflowType = CFLOW_TYPE.INTEREST_CFLOW.toInt();
								if (position < 0)
									position *= -1;
						}
							else if (eventSource == EVENT_SOURCE.EVENT_SOURCE_TRANSACTION_PAYMENT.toInt())
							{
								cashflowType = CFLOW_TYPE.PREMIUM_CFLOW.toInt();
								if (position > 0)
									position *= -1;
							}

							if (buySell == BUY_SELL_ENUM.BUY.toInt())
							{
								tranStrategy.setInt("rec_del", x, RECEIVE_PAY_ENUM.PAY.toInt());
							}
							else /* if (buySell == BUY_SELL_ENUM.SELL.toInt()) */
							{
								tranStrategy.setInt("rec_del", x, RECEIVE_PAY_ENUM.RECEIVE.toInt());
							}
						}

						tranStrategy.setInt("cflow_type", x, cashflowType);
						tranStrategy.setDouble("quantity", x, contractSize * position);						
					}
					//-- Sets column names so the select will copy to the same column
					tranStrategy.setColName("deal_num", "deal_tracking_num");
					tranStrategy.setColName("deal_leg", "param_seq_num");
					tranStrategy.setColName("ins_source_id", "cflow_id");
					tranStrategy.setColName("ins_seq_num", "cflow_seq_num");
					tranStrategy.setColName("deal_pdc", "profile_seq_num");
					tranStrategy.setColFormatAsRef("uom", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);
					tranStrategy.setColFormatAsRef("rec_del", SHM_USR_TABLES_ENUM.REC_PAY_TABLE);
					tranStrategy.setColFormatAsRef("price_unit", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);
					tranStrategy.setColFormatAsRef("cflow_type", SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE);
					simResult.select(tranStrategy, "*", "deal_tracking_num GE 0"); //-- Appends table to results table
					tranStrategy.destroy();
				}
				else 
				{
					for (int x=1; x <= result.getNumRows(); x++) 
					{
						if (result.getInt("strategy_id", x) == 0) 
						{
							result.setInt("int_bunit", x, int_bunit);
							result.setInt("int_portfolio", x, int_portfolio);
						}
					}
					
               if ( (bIsTitleXferLocFromLastActivity || bIsFwdBalMovmtDateFromPrcParcel) && result.getNumRows() > 0 )
               {
                  int base_ins_type = Instrument.getBaseInsType(tran.getInsType());                  
                  if ( INS_TYPE_ENUM.comm_transit.toInt() == base_ins_type || INS_TYPE_ENUM.comm_storage.toInt() == base_ins_type )
                  {                  
                     repalce_xfer_location_and_movement_date(tran, result);
                  }
               }              
					
					result.copyRowAddAll(simResult); //-- Append to result table
					result.destroy();
				}
			}
			catch (OException e)
			{
				tranStrategy.destroy();
				result.destroy();
				APMUtility.APM_Print("APM_UDSR_ParcelAttributes: failed to get parcel attributes for tran# "+ tran.getTranNum());
			}
		}
		simResult.setColName("cflow_id", "ins_source_id"); //-- Sets column name to more generic one since now it is a mix
		simResult.copyTableToTable(returnt);
		simResult.destroy();
    }
		
    public void format_result(Table returnt) throws OException 
    {
       returnt.setColTitle("deal_num", "Deal Number");
       returnt.setColTitle("tran_num", "Transaction\nNumber");
       returnt.setColTitle("ins_num", "Instrument\nNumber");
       returnt.setColTitle("parcel_id", "Parcel\nID");
       returnt.setColTitle("parcel_group_id", "Parcel\nGroup ID");
       returnt.setColTitle("parcel_strategy_id", "Parcel\nStrategy ID");
       returnt.setColFormatAsRef("cflow_type", SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE);
       returnt.setColFormatAsRef("strategy_id", SHM_USR_TABLES_ENUM.STRATEGY_LISTING_TABLE);
       returnt.setColFormatAsRef("uom", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);
       returnt.setColFormatAsRef("mtm_location_id", SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);
       returnt.setColFormatAsRef("intent_active", SHM_USR_TABLES_ENUM.YES_NO_TABLE);
       returnt.setColFormatAsRef("intent_location", SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);
       returnt.setColFormatAsRef("ref_source", SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);
       returnt.setColFormatAsRef("volume_type", SHM_USR_TABLES_ENUM.VOLUME_TYPE_TABLE);
       returnt.setColFormatAsRef("measure_group_id", SHM_USR_TABLES_ENUM.MEASURE_GROUP_TABLE);
       returnt.setColFormatAsRef("price_unit", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);
       returnt.setColFormatAsRef("rec_del", SHM_USR_TABLES_ENUM.REC_PAY_TABLE);
       returnt.setColFormatAsRef("idx_subgroup", SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE);
       returnt.setColFormatAsRef("transport_class", SHM_USR_TABLES_ENUM.TRANSPORT_CLASS_TABLE);
       returnt.setColFormatAsRef("title_xfer_location_id", SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);
    }
    
    /*
     *  Retrieve simulation parameters "Title Transfer is based on" and "Movement Date on Forward Balance is based on"
     *  from APM Parcel Attributes Configuration.
     */
    void retrieve_sim_parameters(Table argt) throws OException 
    {
       int iAttributeGroup;
       String strVal;
       Table tblAttributeGroups, tblConfig = null;      
      
       tblAttributeGroups = SimResult.getAttrGroupsForResultType(argt.getInt("result_type", 1));

       if ( tblAttributeGroups.getNumRows() > 0 )
       {
         iAttributeGroup = tblAttributeGroups.getInt("result_config_group", 1);
         tblConfig = SimResult.getResultConfig(iAttributeGroup);
         
         if ( tblConfig.getNumRows() > 0 )
         {
            tblConfig.sortCol("res_attr_name");         
            strVal = APMUtility.GetParamStrValue(tblConfig, "Title Transfer is based on");                 
            if ( !strVal.isEmpty() )
               bIsTitleXferLocFromLastActivity = strVal.equalsIgnoreCase("Last Activity Location");
            
            strVal = APMUtility.GetParamStrValue(tblConfig, "Movement Date on Forward Balance is based on");                 
            if ( !strVal.isEmpty() )
               bIsFwdBalMovmtDateFromPrcParcel = strVal.equalsIgnoreCase("Pricing Parcel");
         }
       }
    }
    
    /*
     * Replaces title transfer location on all balance and forward balance and movement date on forward balance cash flows.
     */
    void repalce_xfer_location_and_movement_date(Transaction tran, Table tblParcelDetails) throws OException 
    {       
       Table tmpBalanceTable = Table.tableNew();
       Table tmpMvmtDateTable = Table.tableNew();
       int newXferPipeline = 0;
       int newXferZone = 0;
       int newXferFacility = 0;
       int newXferLocation = 0;
       
       try
       {
          if ( bIsTitleXferLocFromLastActivity ) /* find the last activity location */
          {       
             tmpBalanceTable.select(tblParcelDetails, "movement_start_date, parcel_id, title_xfer_pipeline, title_xfer_zone, title_xfer_facility, title_xfer_location_id",
                                                      "title_xfer_location_id GT 0" );
             
             int numBalRows = tmpBalanceTable.getNumRows();
             
             if ( numBalRows > 0 )
             {          
                tmpBalanceTable.clearGroupBy();
                tmpBalanceTable.addGroupBy("movement_start_date");
                tmpBalanceTable.addGroupBy("parcel_id");
                tmpBalanceTable.groupBy();
                
                /* last row is the latest activity */
                newXferPipeline = tmpBalanceTable.getInt("title_xfer_pipeline", numBalRows);
                newXferZone     = tmpBalanceTable.getInt("title_xfer_zone", numBalRows);
                newXferFacility = tmpBalanceTable.getInt("title_xfer_facility", numBalRows);
                newXferLocation = tmpBalanceTable.getInt("title_xfer_location_id", numBalRows);          
             }
          }
          
          if ( bIsFwdBalMovmtDateFromPrcParcel ) /* build a pricing parcel table base on forward balance and commodity cflows */
          {
             tmpMvmtDateTable.select(tblParcelDetails, "parcel_id, movement_start_date, movement_end_date", "parcel_id GT 0");
             tmpMvmtDateTable.clearGroupBy();
             tmpMvmtDateTable.addGroupBy("parcel_id");
             tmpMvmtDateTable.groupBy();
          }
                    
          int num_rows = tblParcelDetails.getNumRows();
          int iCflowTypeCol    = tblParcelDetails.getColNum("cflow_type");
          int iXferPipelineCol = tblParcelDetails.getColNum("title_xfer_pipeline");
          int iXferZoneCol     = tblParcelDetails.getColNum("title_xfer_zone");
          int iXferFacilityCol = tblParcelDetails.getColNum("title_xfer_facility");
          int iXferLocationCol = tblParcelDetails.getColNum("title_xfer_location_id");
          int iParcelIdCol     = tblParcelDetails.getColNum("parcel_id");
          int iPrcParcelIdCol  = tblParcelDetails.getColNum("pricing_parcel_id");
          int iMvmtStartDTCol  = tblParcelDetails.getColNum("movement_start_date");
          int iMvmtEndDTCol    = tblParcelDetails.getColNum("movement_end_date");
                       
          /* set transfer location, etc. */
          for (int row=1; row <= num_rows; row++) 
          {
             int cflow_type = tblParcelDetails.getInt(iCflowTypeCol, row);
                             
             /* replace title transfer location on unmatched balance, forward balance */
             if ( (cflow_type == CFLOW_TYPE.FORWARD_BALANCE_CFLOW.toInt() || cflow_type == CFLOW_TYPE.BALANCE_CFLOW.toInt()) )
             {
                if ( bIsTitleXferLocFromLastActivity && newXferLocation > 0 )
                {
                   tblParcelDetails.setInt(iXferPipelineCol, row, newXferPipeline);                
                   tblParcelDetails.setInt(iXferZoneCol,     row, newXferZone);                
                   tblParcelDetails.setInt(iXferFacilityCol, row, newXferFacility);                
                   tblParcelDetails.setInt(iXferLocationCol, row, newXferLocation);
                }
                
                if ( bIsFwdBalMovmtDateFromPrcParcel && cflow_type == CFLOW_TYPE.FORWARD_BALANCE_CFLOW.toInt() )
                {
                   int parcelId        = tblParcelDetails.getInt(iParcelIdCol, row);
                   int pricingParcelId = tblParcelDetails.getInt(iPrcParcelIdCol, row);
                   
                   if ( parcelId > 0 && pricingParcelId > 0 && parcelId != pricingParcelId && tmpMvmtDateTable.getNumRows() > 0) /* get movement date from pricing parcel */
                   {
                      int dateRow = tmpMvmtDateTable.findInt(1, pricingParcelId, SEARCH_ENUM.FIRST_IN_GROUP);
                      if ( dateRow > 0 )
                      {
                         ODateTime mvmtStartDT = tmpMvmtDateTable.getDateTime(2, dateRow);
                         ODateTime mvmtEndDT   = tmpMvmtDateTable.getDateTime(2, dateRow);                         
                         tblParcelDetails.setDateTime(iMvmtStartDTCol, row, mvmtStartDT);
                         tblParcelDetails.setDateTime(iMvmtEndDTCol,   row, mvmtEndDT);
                      }
                   }                
                }                
             }                                                  
          }          
       }         
       catch (OException e)
       {
          APMUtility.APM_Print("APM_UDSR_ParcelAttributes: failed to repalce_xfer_location_and_movement_date for tran# "+ tran.getTranNum());
       }
       finally
       {    
          tmpBalanceTable.destroy();
          tmpMvmtDateTable.destroy();
       }
    }
}

