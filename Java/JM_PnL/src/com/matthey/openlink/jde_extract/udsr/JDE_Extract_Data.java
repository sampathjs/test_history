package com.matthey.openlink.jde_extract.udsr;

import com.matthey.openlink.jde_extract.JDE_Extract_Common;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.PFOLIO_RESULT_TYPE;
import com.olf.openjvs.enums.RESULT_CLASS;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.USER_RESULT_OPERATIONS;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-07-03	V1.0	mtsteglov	- Initial Version
 *                                    
 *
 */

/**
 * Main Plugin for JDE Extract Data result
 * @author mstseglov
 * @version 1.0
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class JDE_Extract_Data implements IScript 
{	
	public void execute(IContainerContext context) throws OException 
	{
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		USER_RESULT_OPERATIONS op = USER_RESULT_OPERATIONS.fromInt(argt.getInt("operation", 1));
		try 
		{
			switch (op) 
			{
			case USER_RES_OP_CALCULATE:
				calculate(argt, returnt);
				break;
			case USER_RES_OP_FORMAT:
				format(argt, returnt);
				break;
			}
			PluginLog.info("Plugin: " + this.getClass().getName() + " finished successfully.\r\n");
		} 
		catch (Exception e) 
		{
			PluginLog.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) 
			{
				PluginLog.error(ste.toString());
			}
			PluginLog.error("Plugin: " + this.getClass().getName() + " failed.\r\n");
		}
	}

	/**
	 * Generate UDSR output
	 * @param argt
	 * @param returnt
	 * @throws OException
	 */
	protected void calculate(Table argt, Table returnt) throws OException 
	{
		PluginLog.info("Plugin: " + this.getClass().getName() + " calculate called.\r\n");
		
		setOutputFormat(returnt);
		
		// Retrieve all relevant pre-requisite results
		Table revalSimResults = argt.getTable("sim_results", 1);
		Table genResults = revalSimResults.getTable("result_class",RESULT_CLASS.RESULT_GEN.toInt());							
		
		int swapsResultEnum = SimResult.getResultIdFromEnum(JDE_Extract_Common.S_SWAPS_RESULT);
		int fixedDealsResultEnum = SimResult.getResultIdFromEnum(JDE_Extract_Common.S_FIXED_DEALS_RESULT);		
		
		// Retrieve "JDE Extract Data Swaps" result
		Table swapResults = SimResult.getGenResultTables(genResults, swapsResultEnum);
		Table swapResult = (swapResults.getNumRows() > 0) ? swapResults.getTable("results", 1) : null;	
			
		// Retrieve "JDE Extract Data Fixed Deals" result		
		Table fixedDealResults = SimResult.getGenResultTables(genResults, fixedDealsResultEnum);
		Table fixedDealResult = (fixedDealResults.getNumRows() > 0) ? fixedDealResults.getTable("results", 1) : null;	
				
		// Retrieve "Cashflow By Day" Result
		Table cflowByDayResults = SimResult.getGenResultTables(genResults, PFOLIO_RESULT_TYPE.CFLOW_BY_DAY_RESULT.toInt());
		Table cflowByDayResult = cflowByDayResults.getTable("results", 1);			
		
		// Process the two parent results
		processSwapData(swapResult, cflowByDayResult, returnt);
		processFixedDealsData(fixedDealResult, returnt);

		calculateInterestPNL(returnt);
		applyRoundingCorrection(returnt);
		
	}

	/**
	 * Calculate the "interest" P&L based on the differece between settlement value and spot equivalent value
	 * @param workData
	 * @throws OException
	 */
	private void calculateInterestPNL(Table workData) throws OException 
	{
		for (int row = 1; row <= workData.getNumRows(); row++)
		{
			double settlementValue = workData.getDouble("settlement_value", row);
			double spotEquivValue = workData.getDouble("spot_equiv_value", row);
			
			// Round each number to 2 decimal places independently, then take the difference
			double interest = Math.round(settlementValue * 100) / 100d - Math.round(spotEquivValue * 100) / 100d;
			
			workData.setDouble("interest", row, interest);
		}
	}

	private void applyRoundingCorrection(Table tblWorkData ) throws OException {
		
		String strSQL;
		int intQid=0;
		Table tblStlAmt=Table.tableNew();
		
		try{
			
			intQid = Query.tableQueryInsert(tblWorkData, "deal_num");

			strSQL = "SELECT  \n";
			strSQL += "		ats.deal_tracking_num as deal_num,\n";
			strSQL += "		SUM(ABS(ats.settle_amount)) as ate_settle_amount \n";
			strSQL += "FROM \n";
			strSQL += "ab_tran_settle_view  ats \n";
			strSQL += "INNER JOIN ab_tran_event ate ON ate.event_num=ats.event_num \n";
			strSQL += "INNER JOIN query_result qr on qr.query_result = ats.deal_tracking_num and qr.unique_id = " + intQid + "\n";
			strSQL += "INNER JOIN currency ccy on ats.delivery_ccy = ccy.id_number and precious_metal = 0\n";
			strSQL += "INNER JOIN ab_tran ab on ab.tran_num = ate.tran_num and ab.current_flag = 1  \n";
			strSQL += "WHERE \n";
			strSQL += "ats.settle_amount!=0 \n";
			strSQL += "AND ate.event_type=  " + Ref.getValue(SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE, "Cash Settlement") + "   \n";
			strSQL += "AND ate.event_source != " + Ref.getValue(SHM_USR_TABLES_ENUM.EVENT_SOURCE_TABLE, "Split Payment") + "   \n";
			strSQL += "AND ats.tran_status in (" +Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Validated") + "," + Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, "Cancelled") + ") \n";
			strSQL += "AND ab.internal_bunit not in (" + Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "JM PMM HK") + "," + Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, "JM PMM CN") + " ) \n";
			strSQL += "GROUP BY ats.deal_tracking_num \n";
			
			DBaseTable.execISql(tblStlAmt, strSQL);
			
			tblWorkData.select(tblStlAmt,"ate_settle_amount","deal_num EQ $deal_num");
			
			for(int i=1;i<=tblWorkData.getNumRows();i++ ){
				
				double dblSettlementValue;
				dblSettlementValue = tblWorkData.getDouble("settlement_value",i);
				
				double dblAteSettlementValue;
				dblAteSettlementValue = tblWorkData.getDouble("ate_settle_amount",i);
				
				if(dblSettlementValue != 0.0 && dblAteSettlementValue != 0.0){
					
					double dblDiff = dblSettlementValue - dblAteSettlementValue;
					
					if(dblDiff < 0.0 && Math.abs(dblDiff) < 1){ // the db value is GT than the user table value 
						
						double dblInterest = tblWorkData.getDouble("interest",i);
						dblInterest += Math.abs(dblDiff);
						
						tblWorkData.setDouble("interest",i,dblInterest);
						tblWorkData.setDouble("settlement_value",i,dblAteSettlementValue);
					}
					else if (dblDiff > 0.0 && Math.abs(dblDiff) < 1){ // the db value is LT than the user table value 

						double dblInterest = tblWorkData.getDouble("interest",i);
						dblInterest -= dblDiff;
						
						tblWorkData.setDouble("interest",i,dblInterest);
						tblWorkData.setDouble("settlement_value",i,dblAteSettlementValue);
					}
				}
			}
			
			tblWorkData.delCol("ate_settle_amount");
			
		}catch (OException oe){
			PluginLog.info("Exception caught in applyRoundingCorrection: " + oe.toString());
			throw oe;
		}
		finally{
			
			if(tblWorkData.getColNum("ate_settle_amount") > 0){tblWorkData.delCol("ate_settle_amount");}
			if(intQid > 0){Query.clear(intQid);}
			if(Table.isTableValid(tblStlAmt)==1){tblStlAmt.destroy();};
		}
	}
	
	/**
	 * Process fixed deals data - just copy over from "JDE Extract Data Fixed Deals" result
	 * @param fixedDealResult
	 * @param output
	 * @throws OException
	 */
	private void processFixedDealsData(Table fixedDealResult, Table output) throws OException 
	{
		// We can take fixed deal result directly, copying over columns of interest 
		if (fixedDealResult != null)
		{
			fixedDealResult.copyRowAddAllByColName(output);
		}		
	}
	
	/**
	 * Process swaps, retrieving data from JDE Extract Swaps and Cashflow by Day
	 * @param swapResult - JDE Extract Swaps
	 * @param cflowByDayResult - Cashflow by Day, used to set settlement value for fixed swaps
	 * @param output
	 * @throws OException
	 */
	private void processSwapData(Table swapResult, Table cflowByDayResult, Table output) throws OException 
	{
		// If no swap data, exit immediately
		if ((swapResult == null) || (swapResult.getNumRows() < 1))
		{
			return;
		}
		
		Table swapData = createOutputTable();
		swapData.setTableTitle("Swap Data");

		// Retrieve unique deal rows
		swapData.select(swapResult, "DISTINCT, deal_num, fixings_complete, from_currency, to_currency, delivery_date, uom", "deal_num GE 0");
		swapData.group("deal_num");
		swapData.distinctRows();

		// Sum Double values per deal number
		swapData.select(swapResult, "SUM, metal_volume_uom, metal_volume_toz, settlement_value, spot_equiv_value", "deal_num EQ $deal_num");		
		
		// Re-calculate intermediate values per aggregate row
		for (int row = 1; row <= swapData.getNumRows(); row++)
		{
			double volumeUOM = swapData.getDouble("metal_volume_uom", row);
			double volumeTOZ = swapData.getDouble("metal_volume_toz", row);			
			double convFactor = (volumeTOZ > 0.0) ? volumeUOM / volumeTOZ : 0.0;
			
			double settlementValue = swapData.getDouble("settlement_value", row);
			double tradePrice = settlementValue / volumeUOM;	
			tradePrice = Math.round(tradePrice * 10000) / 10000d;
			
			double spotEquivValue = swapData.getDouble("spot_equiv_value", row);
			double spotEquivPrice = spotEquivValue / volumeUOM;
			spotEquivPrice = Math.round(spotEquivPrice * 10000) / 10000d;
			
			swapData.setDouble("conv_factor", row, convFactor);
			swapData.setDouble("trade_price", row, tradePrice);
			swapData.setDouble("spot_equiv_price", row, spotEquivPrice);
		}		
		
		// Enrich the last available FX forward rate
		Table lastFXFwdRate = getLastFwdFXRate(swapResult);
		swapData.select(lastFXFwdRate, "fx_fwd_rate", "deal_num EQ $deal_num");
		
		// Set "settlement value" from "cashflow by day" for deals with fixings complete
		Table fixedSwapsData = createOutputTable();
		fixedSwapsData.setTableTitle("Fixed Swaps Data");
		fixedSwapsData.select(swapData, "*", "fixings_complete EQ Y");
		fixedSwapsData.setColValDouble("settlement_value", 0.0);
		fixedSwapsData.select(cflowByDayResult, "SUM, cflow (settlement_value)", "deal_num EQ $deal_num AND currency EQ $to_currency AND cflow_type EQ 10");
		fixedSwapsData.mathABSCol("settlement_value");
		
		// Re-calculate "trade price" after the swap has fixed
		for (int row = 1; row <= fixedSwapsData.getNumRows(); row++)
		{		
			double volumeUOM = fixedSwapsData.getDouble("metal_volume_uom", row);
			double settlementValue = fixedSwapsData.getDouble("settlement_value", row);
			
			double tradePrice = settlementValue / volumeUOM;	
			tradePrice = Math.round(tradePrice * 10000) / 10000d;
			
			fixedSwapsData.setDouble("trade_price", row, tradePrice);
		}
		
		swapData.select(fixedSwapsData, "settlement_value, trade_price", "deal_num EQ $deal_num");
				
		// Copy to output and delete intermediate tables
		swapData.copyRowAddAllByColName(output);
		swapData.destroy();
		fixedSwapsData.destroy();
		lastFXFwdRate.destroy();
	}
	
	private Table getLastFwdFXRate(Table swapResult) throws OException
	{
		Table lastFXFwdRate = new Table("Last FX Forward Rate");
		lastFXFwdRate.select(swapResult, "DISTINCT, deal_num", "deal_num GE 0");
		lastFXFwdRate.select(swapResult, "reset_date", "deal_num EQ $deal_num");
		lastFXFwdRate.addCol("delete_me", COL_TYPE_ENUM.COL_INT); 
		lastFXFwdRate.setColValInt("delete_me", 0);
		
		lastFXFwdRate.group("deal_num, reset_date");

		// Now, iterate over the temporary table, and remove all prior rows except the one with greatest reset date
		for (int row = lastFXFwdRate.getNumRows(); row >= 2; row--)
		{
			int thisDealNum = lastFXFwdRate.getInt("deal_num", row);
			int prevDealNum = lastFXFwdRate.getInt("deal_num", row - 1);

			int thisResetDate = lastFXFwdRate.getInt("reset_date", row);
			int prevResetDate = lastFXFwdRate.getInt("reset_date", row - 1);

			if (thisDealNum == prevDealNum)
			{
				if (thisResetDate > prevResetDate)
				{
					// Delete prior row, and make sure we compare this row against the new prior row on next loop iteration
					lastFXFwdRate.setInt("delete_me", row-1, 1);
					
				}
				else
				{
					// Delete this row, and move on to prior row
					lastFXFwdRate.setInt("delete_me", row, 1);				}
			}
		}
		lastFXFwdRate.deleteWhereValue("delete_me" , 1);
		lastFXFwdRate.delCol("delete_me");
		lastFXFwdRate.select(swapResult, "fx_fwd_rate", "deal_num EQ $deal_num AND reset_date EQ $reset_date");
		lastFXFwdRate.group("deal_num");
		lastFXFwdRate.distinctRows();		
		
		return lastFXFwdRate;		
	}

	/**
	 * Create a table with desired output format
	 * @return
	 * @throws OException
	 */
	protected Table createOutputTable() throws OException
	{
		Table workData = new Table("JDE Extract Data");

		setOutputFormat(workData);
		
		return workData;
	}
	
	/**
	 * Create columns on input table to match expected UDSR structure
	 * @param workData
	 * @throws OException
	 */
	protected void setOutputFormat(Table workData) throws OException
	{		
		workData.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		workData.addCol("fixings_complete", COL_TYPE_ENUM.COL_STRING);

		workData.addCol("from_currency", COL_TYPE_ENUM.COL_INT);
		workData.addCol("to_currency", COL_TYPE_ENUM.COL_INT);

		workData.addCol("delivery_date", COL_TYPE_ENUM.COL_INT);

		workData.addCol("metal_volume_uom", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("settlement_value", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("spot_equiv_value", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("spot_equiv_price", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("trade_price", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("interest", COL_TYPE_ENUM.COL_DOUBLE);
				
		workData.addCol("uom", COL_TYPE_ENUM.COL_INT);		
		workData.addCol("metal_volume_toz", COL_TYPE_ENUM.COL_DOUBLE);		
		workData.addCol("conv_factor", COL_TYPE_ENUM.COL_DOUBLE);
		
		workData.addCol("fx_fwd_rate", COL_TYPE_ENUM.COL_DOUBLE);
	}

	/**
	 * Formats the table for user consumption - we keep the original column names so we can match against USER table
	 * @param argt
	 * @param returnt
	 * @throws OException
	 */	
	protected void format(Table argt, Table returnt) throws OException 
	{	
		returnt.setColFormatAsRef("from_currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		returnt.setColFormatAsRef("to_currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		
		returnt.setColFormatAsRef("uom", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);
		
		returnt.setColFormatAsDate("delivery_date");
		
		returnt.setColFormatAsNotnl("metal_volume_uom", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		returnt.setColFormatAsNotnl("metal_volume_toz", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		
		returnt.setColFormatAsNotnl("settlement_value", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		returnt.setColFormatAsNotnl("spot_equiv_value", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		returnt.setColFormatAsNotnl("spot_equiv_price", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		returnt.setColFormatAsNotnl("interest", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		
		returnt.setColFormatAsNotnl("trade_price", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
	}
}
