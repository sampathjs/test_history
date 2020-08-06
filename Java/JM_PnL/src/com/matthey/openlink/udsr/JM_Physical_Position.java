package com.matthey.openlink.udsr;

import com.matthey.openlink.pnl.ConfigurationItemPnl;
import com.matthey.openlink.pnl.MTL_Position_Enums;
import com.matthey.openlink.pnl.MTL_Position_Utilities;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.RESULT_CLASS;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.USER_RESULT_OPERATIONS;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 */

/**
 * JM Physical Position result extends "MTL Position" result and adds Cash data to it
 * @author msteglov
 * @version 1.0
 */

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class JM_Physical_Position implements IScript {

	private static Table s_fixedUnfixed = null;
	private static Table s_positionType = null;	
	
	/**
	 * Main execute function 
	 */
	@Override
	public void execute(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		initLogging();

		USER_RESULT_OPERATIONS op = USER_RESULT_OPERATIONS.fromInt(argt.getInt("operation", 1));
		
		try  {
			switch (op) {
			case USER_RES_OP_CALCULATE:
				calculate(argt, returnt);
				break;
			case USER_RES_OP_FORMAT:
				format(argt, returnt);				
				break;
			}
			Logging.info("Plugin " + this.getClass().getName() + " finished successfully");
		} 
		catch (Exception e) 
		{
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) 
			{
				Logging.error(ste.toString());
			}
			Logging.error("Plugin " + this.getClass().getName() + " failed");
			throw e;
		} 
		finally 
		{
			Logging.close();
		}		
	}
	
	/**
	 * Calculates the UDSR output
	 * @param argt
	 * @param returnt
	 * @throws OException
	 */
	protected void calculate(Table argt, Table returnt) throws OException {
		// Set format of returnt table
		setOutputFormat(returnt);
		
		// Adds cash transactions' data
		addCashDealsData(argt, returnt);
		
		// Adds parent results
		addParentResults(argt, returnt);
	}
	
	/**
	 * Creates entries for Cash toolset transactions
	 * @param argt
	 * @param output
	 * @throws OException
	 */
	private void addCashDealsData(Table argt, Table output) throws OException {
		Table transactions = argt.getTable("transactions", 1);
		
		for (int row = 1; row <= transactions.getNumRows(); row++) {
			Transaction trn = transactions.getTran("tran_ptr", row);			
			int toolset = trn.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
			
			// For each Cash transaction, add its data to output table
			if (toolset == TOOLSET_ENUM.CASH_TOOLSET.toInt()) {
				addCashDealToOutput(trn, output);
			}
		}
	}

	/**
	 * Given a Cash deal, query its properties, and add data to output table
	 * @param trn
	 * @param output
	 * @throws OException
	 */
	private void addCashDealToOutput(Transaction trn, Table output) throws OException {
		int dealNum = trn.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
		int ccy = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt());	
		int settleDate = trn.getFieldInt(TRANF_FIELD.TRANF_SETTLE_DATE.toInt());
		double position = trn.getFieldDouble(TRANF_FIELD.TRANF_POSITION.toInt());
		
		int projIdx = MTL_Position_Utilities.getDefaultFXIndexForCcy(ccy);
		
		// USD cash transfers: there is no FX index for USD, so set as LIBOR.USD
		boolean isUSDTransfer = false;
		if (ccy == Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "USD")) {
			projIdx = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, "LIBOR.USD");
			isUSDTransfer = true;
		}
		
		output.addRow();
		int row = output.getNumRows();
		
		output.setInt("deal_num", row, dealNum);
		output.setInt("metal_ccy", row, ccy);
		output.setInt("proj_idx", row, projIdx);
		
		// Set "settle date" for all dates associated with this transaction
		output.setInt("cflow_date", row, settleDate);
		output.setInt("pricing_start_date", row, settleDate);
		output.setInt("pricing_end_date", row, settleDate);
		output.setInt("pricing_rfis_start_date", row, settleDate);
		output.setInt("pricing_rfis_end_date", row, settleDate);
		
		// Cash, like FX, is considered Fixed always
		output.setInt("fixed_unfixed", row, MTL_Position_Enums.FixedUnfixed.FIXED);
		
		// Set trade price from today's metal price		
		double spotPrice = 0.0;
		if ((projIdx > 0) && (!isUSDTransfer)) {
			spotPrice = MTL_Position_Utilities.getSpotGptRate(projIdx);
		} else if (isUSDTransfer) {
			// For USD transfers, the price of USD is $1.0, by definition
			spotPrice = 1.0;
		}
		
		output.setDouble("usd_trade_price", row, spotPrice);
		output.setDouble("usd_trade_value", row, spotPrice * position);		
		
		// Set Position Type to either "Metal" or "Currency" as appropriate
		boolean isPreciousMetal = MTL_Position_Utilities.isPreciousMetal(ccy);
		int positionType = isPreciousMetal ? MTL_Position_Enums.PositionType.METAL : MTL_Position_Enums.PositionType.CURRENCY;
		output.setInt("position_type", row, positionType);		
		
		output.setDouble("position", row, position);		
	}

	/**
	 * Inherit the results for non-Cash deals from parent result "MTL Position"
	 * @param argt
	 * @param output
	 * @throws OException
	 */
	private void addParentResults(Table argt, Table output) throws OException {
		int mtPositionResultID = Ref.getValue(SHM_USR_TABLES_ENUM.RESULT_TYPE_TABLE, "MTL Position");
		
		// Retrieve all relevant pre-requisite results
		Table revalSimResults = argt.getTable("sim_results", 1);
		Table genResults = revalSimResults.getTable("result_class",RESULT_CLASS.RESULT_GEN.toInt());				

		// Get the pointer to the original "MTL Position" result - do not destroy!
		Table mtlPositionResult = SimResult.findGenResultTable(genResults, mtPositionResultID, -2, -2, -2);
		
		if (Table.isTableValid(mtlPositionResult) == 1) {
			// Copy contents to our result
			mtlPositionResult.copyRowAddAllByColName(output);
		}		
	}

	/**
	 * Set the expected output format - note this should be the same as "MTL Position"
	 * @param workData
	 * @return
	 * @throws OException
	 */
	protected Table setOutputFormat(Table workData) throws OException {
		workData.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		workData.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		workData.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
		workData.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);

		workData.addCol("proj_idx", COL_TYPE_ENUM.COL_INT);

		workData.addCol("position", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("usd_trade_price", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("usd_trade_value", COL_TYPE_ENUM.COL_DOUBLE);

		workData.addCol("cflow_date", COL_TYPE_ENUM.COL_INT);
		workData.addCol("pricing_start_date", COL_TYPE_ENUM.COL_INT);
		workData.addCol("pricing_end_date", COL_TYPE_ENUM.COL_INT);
		workData.addCol("pricing_rfis_start_date", COL_TYPE_ENUM.COL_INT);
		workData.addCol("pricing_rfis_end_date", COL_TYPE_ENUM.COL_INT);		
		workData.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		workData.addCol("position_type", COL_TYPE_ENUM.COL_INT);
		workData.addCol("fixed_unfixed", COL_TYPE_ENUM.COL_INT);

		workData.addCol("ref_source", COL_TYPE_ENUM.COL_INT);

		return workData;		
	}	
	
	/**
	 * Format UDSR output for GUI consumption
	 * @param argt
	 * @param returnt
	 * @throws OException
	 */
	protected void format(Table argt, Table returnt) throws OException {
		returnt.setColTitle("deal_num", "Deal Num");
		returnt.setColTitle("deal_leg", "Deal Leg");
		returnt.setColTitle("deal_pdc", "Deal Profile");		
		returnt.setColTitle("deal_reset_id", "Deal Reset ID");

		returnt.setColTitle("proj_idx", "Index");

		returnt.setColTitle("position", "Position");
		returnt.setColTitle("usd_trade_price", "USD Trade Price");
		returnt.setColTitle("usd_trade_value", "USD Trade Value");

		returnt.setColTitle("cflow_date", "Settle Date");
		returnt.setColTitle("pricing_start_date", "Pricing Start Date");
		returnt.setColTitle("pricing_end_date", "Pricing End Date");
		returnt.setColTitle("pricing_rfis_start_date", "Pricing RFIS\nStart Date");
		returnt.setColTitle("pricing_rfis_end_date", "Pricing RFIS\nEnd Date");		
		returnt.setColTitle("metal_ccy", "Metal \\ Currency");
		returnt.setColTitle("position_type", "Metal \\ Currency\nType");
		returnt.setColTitle("fixed_unfixed", "Fixed \\ Unfixed");
		returnt.setColTitle("ref_source", "Ref Source");

		returnt.setColFormatAsRef("proj_idx", SHM_USR_TABLES_ENUM.INDEX_TABLE);
		returnt.setColFormatAsRef("metal_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		returnt.setColFormatAsRef("ref_source", SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);

		returnt.setColFormatAsDate("cflow_date");
		returnt.setColFormatAsDate("pricing_start_date");
		returnt.setColFormatAsDate("pricing_end_date");
		returnt.setColFormatAsDate("pricing_rfis_start_date");
		returnt.setColFormatAsDate("pricing_rfis_end_date");		

		returnt.setColFormatAsNotnl("position", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColFormatAsNotnl("usd_trade_price", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColFormatAsNotnl("usd_trade_value", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		if (s_fixedUnfixed == null) {
			s_fixedUnfixed = new Table("USER_mtl_fixed_unfixed");
			s_positionType = new Table("USER_mtl_position_type");

			DBUserTable.load(s_fixedUnfixed);
			DBUserTable.load(s_positionType);
		}

		returnt.setColFormatAsTable("fixed_unfixed", s_fixedUnfixed);
		returnt.setColFormatAsTable("position_type", s_positionType);

		// Group Table
		returnt.group("deal_num, deal_leg, deal_pdc, proj_idx, deal_reset_id");		
	}

	/**
	 * Initialise plugin log
	 * @throws OException
	 */
	private void initLogging() throws OException 
	{	
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");
		String logLevel = ConfigurationItemPnl.LOG_LEVEL.getValue();
		String logFile = ConfigurationItemPnl.LOG_FILE.getValue();
		String logDir = ConfigurationItemPnl.LOG_DIR.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir + "\\error_logs";
		}
		if (logFile.trim().equals("")) {
			logFile = this.getClass().getName() + ".log";
		}
		try 
		{
			Logging.init( this.getClass(), ConfigurationItemPnl.CONST_REP_CONTEXT, ConfigurationItemPnl.CONST_REP_SUBCONTEXT);
			
		} 
		catch (Exception e) 
		{
			throw new RuntimeException (e);
		}
		Logging.info("**********" + this.getClass().getName() + " started **********");
	}
}
