/* Released with version 27-Feb-2019_V17_0_7 of APM */

/*
File Name:                      APM_UDSR_BulkInventoryAttributes.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         John Donath
Creation Date:                  August 23, 2017
 
Revision History:
                                                
Script Type:                    User-defined simulation result

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    APM Bulk Inventory result	

*/ 

package com.olf.result;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Instrument;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.PRICE_ADJ_LEVEL;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.USER_RESULT_OPERATIONS;
import com.olf.result.APMUtility.APMUtility;

/**
 * Implements the APM BUlk Inventory Attributes Sim Result.
 * 
 * @author John D. Donath
 *
 */
public class APM_UDSR_BulkInventoryAttributes implements IScript {

	private Table nominationActivityTypeTbl = Util.NULL_TABLE;
	
	private Table balanceDetailActionTbl = Util.NULL_TABLE;
	
	private int includeZeroes = 0;
	
	/**
	 * Main entry point for Reval
	 * 
	 * @param IContainerContext The context of the reval.
	 * @throws OException
	 */
	@Override
	public void execute(IContainerContext contx) throws OException {
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

	}
	
	/**
	 * Computes the APM result. 
	 * 
	 * @param Table argt The arguments to the Reval
	 * @param Table returnt A table of the Reval's return values.
	 * @throws OException
	 */
	public void compute_result(Table argt, Table returnt) throws OException 
	{
    	Table balanceDetails = Util.NULL_TABLE, balanceDetailsTotal = Util.NULL_TABLE;
    	int tranNum = Util.NOT_FOUND;
    	
    	try
    	{
    		Table tblTrans = argt.getTable("transactions", 1);
    		
    		nominationActivityTypeTbl = Table.tableNew("Nomination Activity Type");
    		DBaseTable.execISql(nominationActivityTypeTbl, "select activity_id, activity_name from nomination_activity_type where active_flag = 1");
    		nominationActivityTypeTbl.addGroupBy("activity_id");
    		nominationActivityTypeTbl.groupBy();
    		
    		balanceDetailActionTbl = Table.tableNew("Balance Details Action Table");
    		DBaseTable.execISql(balanceDetailActionTbl, "select balance_details_action_id, balance_details_action_name from balance_details_action");
    		balanceDetailActionTbl.addGroupBy("balance_details_action_id");
    		balanceDetailActionTbl.groupBy();
    		
     		Table tblAttributeGroups = SimResult.getAttrGroupsForResultType(argt.getInt("result_type", 1));
       		if (tblAttributeGroups.getNumRows() > 0)
    		{
    			int iAttributeGroup = tblAttributeGroups.getInt("result_config_group", 1);
    			Table tblConfig = SimResult.getResultConfig(iAttributeGroup);
    	    	tblConfig.sortCol("res_attr_name");
    	    	
    	    	if (APMUtility.ParamHasValue(tblConfig, "Include Zeroes") > 0)
    	    	{
    	    		String strVal = APMUtility.GetParamStrValue(tblConfig, "Include Zeroes");
    	    		if (!strVal.isEmpty())
    	    			includeZeroes = Ref.getValue(SHM_USR_TABLES_ENUM.YES_NO_TABLE, strVal);
    	    	}
    		}
      		
    		balanceDetailsTotal = Table.tableNew("bulk_inventory");
    		formatBalanceDetails(balanceDetailsTotal);
    		
    		for (int row = 1; row <= tblTrans.getNumRows(); row++)
    		{
    		   	Transaction tr = tblTrans.getTran("tran_ptr", row);
    		   	tranNum = tr.getTranNum();
    		   	int baseInsType = Instrument.getBaseInsType(tr.getInsType());
    		   	int priceAdjLevel = tr.getFieldInt(TRANF_FIELD.TRANF_PRICE_ADJ_LEVEL.toInt());
    		   	if ((baseInsType != INS_TYPE_ENUM.comm_storage.toInt() && baseInsType != INS_TYPE_ENUM.comm_transit.toInt())
    		   			|| priceAdjLevel != PRICE_ADJ_LEVEL.PRICE_ADJ_LEVEL_PARCEL.toInt())
    		   		continue;
    			balanceDetails = tr.getBalanceDetails();
    			
    			addToBalanceDetails(balanceDetailsTotal, balanceDetails, tr);
    			balanceDetails.destroy();
    			balanceDetails = Util.NULL_TABLE;
    		}
    		
    		removeZeroes(balanceDetailsTotal);
    		balanceDetailsTotal.copyTableToTable(returnt);
    		balanceDetailsTotal.destroy();
    		balanceDetailsTotal = Util.NULL_TABLE;
    	}
    	catch (OException oe)
    	{
            APMUtility.APM_Print("APM_UDSR_BulkInventoryAttributes: failed to get bulk inventory attributes for tran# "+ tranNum);
    	}
    	finally
    	{ 
       		if (balanceDetails != Util.NULL_TABLE)
    			balanceDetails.destroy();
    		if (balanceDetailsTotal != Util.NULL_TABLE)
    			balanceDetailsTotal.destroy();
    	}
	}
	
	/**
	 * Formats the results (column title an formats) of the return table for the result.
	 * 
	 * @param Table returnt The Table whose columns need to be formatted.
	 * @throws OException
	 */
	public void format_result(Table returnt) throws OException 
	{
	    returnt.setColTitle("dealnum", "Deal Num");
		returnt.setColTitle("parcel_id", "Parcel ID");
	    returnt.setColTitle("cargo_id", "Cargo ID");
	    returnt.colHide("cargo_id");
	    returnt.setColTitle("cargo_name", "Cargo");
		returnt.setColTitle("delivery_id", "Delivery ID");
	    returnt.setColTitle("inj_with", "Injection/Withdrawal");
	    returnt.setColTitle("action_int", "Action ID");
	    returnt.colHide("action_int");
	    returnt.setColTitle("activity_type", "Activity Type");
	    returnt.colHide("activity_type");
	    returnt.setColTitle("activity_type_str", "Row Activity"); 
		returnt.setColTitle("start_date", "Date");
	    returnt.setColFormatAsDate("start_date");
		returnt.setColTitle("product", "Product");  
	    returnt.setColTitle("measure_group_id", "Measure Group");	    
	    returnt.setColFormatAsRef("measure_group_id", SHM_USR_TABLES_ENUM.MEASURE_GROUP_TABLE);
	    returnt.setColTitle("pipeline_id", "Pipeline");
	    returnt.setColFormatAsRef("pipeline_id", SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);
	    returnt.setColTitle("facility_id", "Facility");
	    returnt.setColFormatAsRef("facility_id", SHM_USR_TABLES_ENUM.FACILITY_TABLE);
		returnt.setColTitle("strategy_id", "Strategy");	   
	    returnt.setColTitle("vessel", "Vessel");
	    
		returnt.setColTitle("end_date", "End Date");
	    returnt.setColFormatAsDate("end_date");
	    returnt.colHide("end_date");
	    returnt.setColTitle("date_int", "Date Integer");
	    returnt.colHide("date_int");
		returnt.setColTitle("location_id", "Location");
		returnt.colHide("location_id");
	    returnt.setColTitle("tran_num", "Tran Num");
	    returnt.colHide("tran_num");
	    returnt.setColTitle("quantity", "Quantity");
	    returnt.colHide("quantity");

	    returnt.clearGroupBy();
	    returnt.addGroupBy("product");
	    returnt.addGroupBy("strategy_id");
	    returnt.addGroupBy("date_int");
	    returnt.addGroupBy("action_int");
	    returnt.addGroupBy("dealnum");
	    returnt.groupBy();      
	}

	/**
	 * Adds the columns that the reval produces to the table that it receives as a parameter. 
	 * 
	 * @param Table balanceDetails The table to add columns to.
	 * @throws OException
	 */
	private void formatBalanceDetails(Table balanceDetails) throws OException
	{
		balanceDetails.addCol("dealnum", COL_TYPE_ENUM.COL_INT);
		balanceDetails.setColTitle("dealnum", "Deal Num");
		balanceDetails.addCol("parcel_id", COL_TYPE_ENUM.COL_INT);
		balanceDetails.setColTitle("parcel_id", "Parcel ID");
	    balanceDetails.addCol("cargo_id", COL_TYPE_ENUM.COL_INT);
	    balanceDetails.setColTitle("cargo_id", "Cargo ID");
	    balanceDetails.colHide("cargo_id");
	    balanceDetails.addCol("cargo_name", COL_TYPE_ENUM.COL_STRING);
	    balanceDetails.setColTitle("cargo_name", "Cargo");
		balanceDetails.addCol("delivery_id", COL_TYPE_ENUM.COL_INT);
		balanceDetails.setColTitle("delivery_id", "Delivery ID");
		balanceDetails.addCol("inj_with", COL_TYPE_ENUM.COL_STRING);
		balanceDetails.setColTitle("inj_with", "Injection/Withdrawal");
		balanceDetails.addCol("action_int", COL_TYPE_ENUM.COL_INT);
		balanceDetails.setColTitle("action_int", "Action ID");
		balanceDetails.colHide("action_int");
		balanceDetails.addCol("activity_type", COL_TYPE_ENUM.COL_INT);
		balanceDetails.setColFormatAsRef("activity_type", SHM_USR_TABLES_ENUM.NOMINATION_ACTIVITY_TYPE_TABLE);
		balanceDetails.colHide("activity_type");		
		balanceDetails.addCol("activity_type_str", COL_TYPE_ENUM.COL_STRING);
		balanceDetails.setColTitle("activity_type_str", "Row Activity");		
		balanceDetails.addCol("start_date", COL_TYPE_ENUM.COL_DATE_TIME);
		balanceDetails.setColTitle("start_date", "Date");
		balanceDetails.setColFormatAsDate("start_date");
		balanceDetails.addCol("end_date", COL_TYPE_ENUM.COL_DATE_TIME);
		balanceDetails.setColTitle("end_date", "End Date");
		balanceDetails.setColFormatAsDate("end_date");
		balanceDetails.colHide("end_date");
		balanceDetails.addCol("date_int", COL_TYPE_ENUM.COL_INT);
		balanceDetails.setColTitle("date_int", "Date Integer");
		balanceDetails.colHide("date_int");
		balanceDetails.addCol("product", COL_TYPE_ENUM.COL_INT );
		balanceDetails.setColFormatAsRef("product", SHM_USR_TABLES_ENUM.IDX_SUBGROUP_TABLE);
		balanceDetails.setColTitle("product", "Product");
		balanceDetails.addCol("measure_group_id", COL_TYPE_ENUM.COL_INT);
		balanceDetails.setColTitle("measure_group_id", "Measure Group");
		balanceDetails.setColFormatAsRef("measure_group_id", SHM_USR_TABLES_ENUM.MEASURE_GROUP_TABLE);
	    balanceDetails.addCol("pipeline_id", COL_TYPE_ENUM.COL_INT);
	    balanceDetails.setColTitle("pipeline_id", "Pipeline");
	    balanceDetails.setColFormatAsRef("pipeline_id", SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);		
	    balanceDetails.addCol("facility_id", COL_TYPE_ENUM.COL_INT);
	    balanceDetails.setColTitle("facility_id", "Facility");
	    balanceDetails.setColFormatAsRef("facility_id", SHM_USR_TABLES_ENUM.FACILITY_TABLE);		
		balanceDetails.addCol("strategy_id", COL_TYPE_ENUM.COL_INT);
		balanceDetails.setColFormatAsRef("strategy_id", SHM_USR_TABLES_ENUM.STRATEGY_LISTING_TABLE );
		balanceDetails.setColTitle("strategy_id", "Strategy ID");
	    balanceDetails.addCol("vessel", COL_TYPE_ENUM.COL_STRING);
	    balanceDetails.setColTitle("vessel", "Vessel");		

		balanceDetails.addCol("location", COL_TYPE_ENUM.COL_INT);
		balanceDetails.setColTitle("location", "Location");
		balanceDetails.setColFormatAsRef("location", SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);
		balanceDetails.colHide("location");
		balanceDetails.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
		balanceDetails.setColTitle("tran_num", "Tran Num");
		balanceDetails.colHide("tran_num");
		balanceDetails.addCol("quantity", COL_TYPE_ENUM.COL_DOUBLE);
		balanceDetails.setColTitle("quantity", "Quantity");
		balanceDetails.colHide("quantity");
	}
	
	/**
	 * Appends the rows in the table it receives as the second parameter to the table in the first parameter.
	 * It keeps the table sorted by its keys. 
	 * 
	 * @param Table balanceDetailsTotal The table to append to
	 * @param Table balanceDetails Contains the rows in the balance details reval to append.
	 * @param Transaction tr The transaction whose balance report is being appended.
	 * @throws OException
	 */
	private void addToBalanceDetails(Table balanceDetailsTotal, Table balanceDetails, Transaction tr) throws OException
	{
		for (int row = 1; row <= balanceDetails.getNumRows(); row++)
		{
			int product = balanceDetails.getInt("product", row);
			int strategy = balanceDetails.getInt("strategy_id", row);
			int date = balanceDetails.getDate("date", row);
			int action = balanceDetails.getInt("action", row);

			int productFirstRow = balanceDetailsTotal.findInt("product", product, SEARCH_ENUM.FIRST_IN_GROUP);
			if (productFirstRow < 1)
			{
				balanceDetailsTotal.insertRowBefore(productFirstRow*-1);
				insertRowToBalanceTotal(balanceDetailsTotal, balanceDetails, tr, productFirstRow*-1, row);
				continue;
			}
			
			int productLastRow = balanceDetailsTotal.findInt("product", product, SEARCH_ENUM.LAST_IN_GROUP);
			int strategyFirstRow = balanceDetailsTotal.findIntRange("strategy_id", productFirstRow, productLastRow, strategy, SEARCH_ENUM.FIRST_IN_GROUP);
			if (strategyFirstRow < 1)
			{
				balanceDetailsTotal.insertRowBefore(strategyFirstRow*-1);
				insertRowToBalanceTotal(balanceDetailsTotal, balanceDetails, tr, strategyFirstRow*-1, row);
				continue;
			}
			
			int strategyLastRow = balanceDetailsTotal.findIntRange("strategy_id", productFirstRow, productLastRow, strategy, SEARCH_ENUM.LAST_IN_GROUP);
			int dateFirstRow = balanceDetailsTotal.findIntRange("date_int", strategyFirstRow, strategyLastRow, date, SEARCH_ENUM.FIRST_IN_GROUP);
			if (dateFirstRow < 1)
			{
				balanceDetailsTotal.insertRowBefore(dateFirstRow*-1);
				insertRowToBalanceTotal(balanceDetailsTotal, balanceDetails, tr, dateFirstRow*-1, row);
				continue;
			}
			
			int dateLastRow = balanceDetailsTotal.findIntRange("date_int", strategyFirstRow, strategyLastRow, date, SEARCH_ENUM.LAST_IN_GROUP);
			int actionFirstRow = balanceDetailsTotal.findIntRange("action", dateFirstRow, dateLastRow, action, SEARCH_ENUM.FIRST_IN_GROUP);
			if (actionFirstRow < 1)
			{
				balanceDetailsTotal.insertRowBefore(actionFirstRow*-1);
				insertRowToBalanceTotal(balanceDetailsTotal, balanceDetails, tr, actionFirstRow*-1, row);
				continue;
			}
			
			int actionLastRow = balanceDetailsTotal.findIntRange("action", dateFirstRow, dateLastRow, action, SEARCH_ENUM.LAST_IN_GROUP);
			balanceDetailsTotal.insertRowAfter(actionLastRow);
			insertRowToBalanceTotal(balanceDetailsTotal, balanceDetails, tr, actionLastRow+1, row);
		}
		
		balanceDetailsTotal.clearGroupBy();
		balanceDetailsTotal.addGroupBy("product");
		balanceDetailsTotal.addGroupBy("strategy_id");
		balanceDetailsTotal.addGroupBy("date_int");
		balanceDetailsTotal.addGroupBy("action_int");
		balanceDetailsTotal.addGroupBy("dealnum");
		balanceDetailsTotal.groupBy();
	}
	
	/**
	 * Populates the values of one row from the table passed as the second parameter to the table in the 
	 * first parameter. 
	 * 
	 * @param Table balanceDetailsTotal The table to insert the row to.
	 * @param Table balanceDetails The table which contains the row to insert.
	 * @param Transaction tr The transaction that the balance details report is from.
	 * @param int toRow The row in the target table to insert to.
	 * @param int fromRow The row in the source table to copy from.
	 * @throws OException
	 */
	private void insertRowToBalanceTotal(Table balanceDetailsTotal, Table balanceDetails, Transaction tr, int toRow, int fromRow)
	throws OException
	{
		int parcelID = balanceDetails.getInt("parcel_id", fromRow);
		balanceDetailsTotal.setInt("parcel_id", toRow, parcelID);
		int deliveryID = balanceDetails.getInt("delivery_id", fromRow);
		balanceDetailsTotal.setInt("delivery_id", toRow, deliveryID);
		int date = balanceDetails.getDate("date", fromRow);
		balanceDetailsTotal.setDateTimeByParts("start_date", toRow, date, 0);
		balanceDetailsTotal.setDateTimeByParts("end_date", toRow, date, 0);
		balanceDetailsTotal.setInt("date_int", toRow, date);
		balanceDetailsTotal.setInt("product", toRow, balanceDetails.getInt("product", fromRow));
		balanceDetailsTotal.setInt("measure_group_id", toRow, balanceDetails.getInt("measure_group_id", fromRow));
		int locationID = balanceDetails.getInt("location", fromRow);
		balanceDetailsTotal.setInt("location", toRow, locationID);		
		balanceDetailsTotal.setInt("strategy_id", toRow, balanceDetails.getInt("strategy_id", fromRow));
		balanceDetailsTotal.setInt("dealnum", toRow, tr.getInsFromTran().getDealNum());
		balanceDetailsTotal.setInt("tran_num", toRow, tr.getTranNum());
		int action = balanceDetails.getInt("action", fromRow);
		balanceDetailsTotal.setInt("action_int", toRow, action);
		boolean isBalance = false;
		switch (action)
		{
			case 1:	case 2: case 3: case 4: case 10: case 12: case 14: case 16: case 18: case 20:
				balanceDetailsTotal.setString("inj_with", toRow, "Injection");
				break;
			case 5: case 6: case 7: case 8: case 11: case 13: case 15: case 17: case 19: case 21:
				balanceDetailsTotal.setString("inj_with", toRow, "Withdrawal");
				break;
			case 9:
				balanceDetailsTotal.setString("inj_with", toRow, "Balance");
				isBalance = true;
				break;
			default:
				break;
		}
		
		int activityType = balanceDetails.getInt("activity_type", fromRow); 
		balanceDetailsTotal.setInt("activity_type", toRow, activityType);
		
		if (isBalance)
			balanceDetailsTotal.setString("activity_type_str", toRow, "");
		else
		{
			if (activityType > 0)
			{
				int nomRow = nominationActivityTypeTbl.findInt("activity_id", activityType, SEARCH_ENUM.FIRST_IN_GROUP);
				balanceDetailsTotal.setString("activity_type_str", toRow, nominationActivityTypeTbl.getString("activity_name", nomRow));
			}
			else
			{
				int balanceDetailsRow = balanceDetailActionTbl.findInt("balance_details_action_id", action, SEARCH_ENUM.FIRST_IN_GROUP);
				balanceDetailsTotal.setString("activity_type_str", toRow, balanceDetailActionTbl.getString("balance_details_action_name", balanceDetailsRow));
			}
		}
		
		balanceDetailsTotal.setDouble("quantity", toRow, balanceDetails.getDouble("quantity", fromRow));
		
		balanceDetailsTotal.setInt("cargo_id", toRow, balanceDetails.getInt("cargo_id", fromRow));
		String cargoName = balanceDetails.getString("cargo_name", fromRow);
		if (cargoName == null || cargoName.equals(""))
			cargoName = " ";
		balanceDetailsTotal.setString("cargo_name", toRow, cargoName);
		String vessel = balanceDetails.getString("vessel", fromRow);
		if (vessel == null || vessel.equals(""))
			vessel = " ";
		balanceDetailsTotal.setString("vessel", toRow, vessel);
		balanceDetailsTotal.setInt("facility_id", toRow, balanceDetails.getInt("facility_id", fromRow));
		balanceDetailsTotal.setInt("pipeline_id", toRow, balanceDetails.getInt("pipeline_id", fromRow));	
	}

	/**
	 * Removes the rows with zero quantity from the table passed in as a parameter.
	 * 
	 * @param Table balanceDetailsTotal The table to remove zeroes from.
	 * @throws OException
	 */
	private void removeZeroes(Table balanceDetailsTotal) throws OException
	{		
		if (includeZeroes == 0)
		{			
			for (int row = 1; row <= balanceDetailsTotal.getNumRows();)
				if (balanceDetailsTotal.getDouble("quantity", row) == 0.0)
				{
						balanceDetailsTotal.delRow(row);
				}
				else
				{
					row++;
				}
		}
	}
}
