package com.matthey.openlink.jde_extract.udsr;

import java.util.HashMap;

import com.matthey.openlink.jde_extract.JDE_Extract_Common;
import com.matthey.openlink.pnl.MTL_Position_Enums;
import com.matthey.openlink.pnl.MTL_Position_Utilities;
import com.matthey.openlink.pnl.PNL_EntryDataUniqueID;
import com.matthey.openlink.pnl.PNL_FixingsMarketDataRecorder;
import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Instrument;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.olf.openjvs.enums.USER_RESULT_OPERATIONS;
import com.olf.openjvs.enums.VALUE_STATUS_ENUM;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-06-29	V1.0	mtsteglov	- Initial Version
 * 2020-02-18   V1.1    agrawa01 	- memory leaks & formatting changes            
 *
 */

/**
 * Main Plugin for JDE Extract Data result - Swaps
 * @author mstseglov
 * @version 1.0
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class JDE_Extract_Data_Swaps implements IScript {
	
	class MarketData {
		int m_metalRow = -1;
		int m_ccyRow = -1;
	}
	
	private HashMap<PNL_EntryDataUniqueID, MarketData> m_dealMktDataMap = null;
	private int m_tozUnit = -1;

	/**
	 * Main function that gets called when the UDSR is being run
	 */
	public void execute(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();		

		USER_RESULT_OPERATIONS op = USER_RESULT_OPERATIONS.fromInt(argt.getInt("operation", 1));
		try 
		{
			Logging.init(this.getClass(), "", "");
			switch (op) 
			{
			case USER_RES_OP_CALCULATE:
				calculate(argt, returnt);
				break;
			case USER_RES_OP_FORMAT:
				format(argt, returnt);				
				break;
			default:
				break;
			}
			Logging.info("Plugin: " + this.getClass().getName() + " finished successfully.\r\n");
			
		} catch (Exception e) {
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
			Logging.error("Plugin: " + this.getClass().getName() + " failed.\r\n");
		}finally{
			Logging.close();
		}
	}

	protected void calculate(Table argt, Table returnt) throws OException 
	{
		Logging.info("Plugin: " + this.getClass().getName() + " calculate called.\r\n");
		
		// Initialise the "tOz" unit
		m_tozUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, JDE_Extract_Common.S_TOZ_UNIT_NAME);
		
		// Set up output table format
		setOutputFormat(returnt);
		Table comSwapData = Util.NULL_TABLE, marketData = Util.NULL_TABLE, transData = Util.NULL_TABLE;
		
		try {
			// Generate transaction-level data columns
			transData = prepareTransactionsData(argt.getTable("transactions", 1));		

			// Retrieve stored market data from USER_JM_PNL_Market_Data
			marketData = prepareMarketData(argt.getTable("transactions", 1));		
			
			// Process ComSwap toolset deals
			Logging.info("Process ComSwap toolset deals\n");
			//OConsole.message("Process ComSwap toolset deals\n");
			comSwapData = generateComSwapDataTable(transData, marketData);
			comSwapData.copyRowAddAllByColName(returnt);
			
		} finally {
			if (Table.isTableValid(transData) == 1) {
				transData.destroy();
			}
			
			if (Table.isTableValid(comSwapData) == 1) {
				comSwapData.destroy();
			}
			
			if (Table.isTableValid(marketData) == 1) {
				marketData.destroy();
			}
		}
	}

	/**
	 * Generate the output data for ComSwap toolset
	 * @param transData - transaction data
	 * @param marketData - market data from USER_JM_PNL_Market_Data
	 * @return
	 * @throws OException
	 */
	private Table generateComSwapDataTable(Table transData, Table marketData) throws OException {
		int fixedLeg = Ref.getValue(SHM_USR_TABLES_ENUM.FX_FLT_TABLE, "Fixed");
		Table workData = createOutputTable();	
			
		workData.select(marketData, "deal_num, deal_leg, deal_pdc, deal_reset_id", "deal_reset_id LT " + PNL_FixingsMarketDataRecorder.S_FX_RESET_OFFSET);
		workData.select(transData, "tran_num, fixings_complete, tran_ptr, toolset", "deal_num EQ $deal_num");
				
		prepareMarketDataMap(marketData);
		int rows = workData.getNumRows();
		
		for (int row = rows; row >= 1; row--) {
			int toolset = workData.getInt("toolset", row);
			
			// Due to the order of enrichment, we have non-ComSwap deals here - delete them now
			if (toolset != TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt()) {
				workData.delRow(row);
				continue;
			}
			
			Transaction trn = workData.getTran("tran_ptr", row);
			int dealNum = workData.getInt("deal_num", row);
			int dealLeg = workData.getInt("deal_leg", row);
			int dealResetID = workData.getInt("deal_reset_id", row);
						
	    	// Take delivery date from payment date on leg 0, since that represents metal movement
	    	int deliveryDate = trn.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), 0, "", 0);		
	    	
			// At JM, price spread is modelled using a Tran Info field, to support cross-currency swaps with a spread in original currency
	    	// Note that we have to store it, rather than add it to reset value, since it is in TOz, not in deal unit
			double idxCcySpd = trn.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, MTL_Position_Enums.s_metalSpreadTranInfoField);	    	
			
			// At JM, contracts are booked where currency conversion is done at market + spread
			// This spread is stored as Tran Info field, and is used to adjust "trade price" (reset price)
			double resetFXSpread = trn.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, MTL_Position_Enums.s_usdFXSpreadTranInfoField);
			
			// JM enter contracts where price is quoted based on market price + a % based on delay between pricing date and delivery date
			// So-called "Contango / Backwardation Rate"
			double cbRate = trn.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, MTL_Position_Enums.s_cbRateTranInfoField);
			
			// Skip the fixed (deliverable) swap leg, we only store resets from floating legs
			int fxFlt = trn.getFieldInt(TRANF_FIELD.TRANF_FX_FLT.toInt(), dealLeg);				
			if (fxFlt == fixedLeg) {				
				continue;
			}
			
			int projIdx = trn.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), dealLeg, "", 0, 0);
			int fromCcy = MTL_Position_Utilities.getCcyForIndex(projIdx);				
			int toCcy = trn.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), dealLeg);
			int uom = trn.getFieldInt(TRANF_FIELD.TRANF_UNIT.toInt(), dealLeg);
			double convFactor = Transaction.getUnitConversionFactor(uom, m_tozUnit);
			
			PNL_EntryDataUniqueID entry = new PNL_EntryDataUniqueID(dealNum, dealLeg, 0, dealResetID);
			if (!m_dealMktDataMap.containsKey(entry)) {
				continue;
			}
			
			double resetNotional = trn.getFieldDouble(TRANF_FIELD.TRANF_RESET_NOTIONAL.toInt(), dealLeg, "", dealResetID);
			// Pick up the final reset value from transaction reset, not the raw value - this will include spread
			double resetValue = trn.getFieldDouble(TRANF_FIELD.TRANF_RESET_VALUE.toInt(), dealLeg, "", dealResetID);
			// If the currency of the pricing index is not the same as leg currency, store relevant data
			double resetFXRate = 0.0;
			if (MTL_Position_Utilities.getPaymentCurrencyForIndex(projIdx) != toCcy) {
				resetFXRate = trn.getFieldDouble(TRANF_FIELD.TRANF_RESET_SPOT_CONV.toInt(), dealLeg, "", dealResetID);
			}
			
			int resetDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_DATE.toInt(), dealLeg, "", dealResetID);	
			// Calculate CB adjustment factor
			double cbAdjustmentFactor = 1.0;
			int cbDayCount = 0;
			if (Math.abs(cbRate) > JDE_Extract_Common.EPSILON) {
				int resetRFISDate = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_RFIS_DATE.toInt(), dealLeg, "", dealResetID);
						
				// Calculate CB factor based on rate across 360 days, and multiply by number of days between pricing and delivery
				cbDayCount =  deliveryDate - resetRFISDate;
				cbAdjustmentFactor = 1 + ((cbRate / 100) / 360) * cbDayCount;						
			}
			
			workData.setDouble("metal_volume_uom", row, Math.abs(resetNotional));
			workData.setDouble("metal_volume_toz", row, Math.abs(resetNotional) * convFactor);
			workData.setDouble("trade_price", row, resetValue);
			workData.setDouble("toz_spread", row, idxCcySpd);
			
			workData.setDouble("reset_fx_rate", row, resetFXRate);
			workData.setDouble("reset_fx_spread", row, resetFXSpread);
			
			workData.setDouble("cb_rate", row, cbRate);
			workData.setDouble("cb_adj_factor", row, cbAdjustmentFactor);
			workData.setInt("cb_day_count", row, cbDayCount);
			
			workData.setInt("uom", row, uom);
			workData.setInt("from_currency", row, fromCcy);
			workData.setInt("to_currency", row, toCcy);
			workData.setInt("delivery_date", row, deliveryDate);
			workData.setInt("reset_date", row, resetDate);
		}
		
		calculateDerivedDataValues(workData, marketData);
		return workData;
	}
	
	/**
	 * Generate an appropriate PNL_EntryDataUniqueID from given data row
	 * @param workData - table of data
	 * @param row - offset row into table
	 * @return
	 * @throws OException
	 */
	private PNL_EntryDataUniqueID getUniqueEntry(Table workData, int row) throws OException {
		int dealNum = workData.getInt("deal_num", row);
		int dealLeg = workData.getInt("deal_leg", row);			
		int dealReset = workData.getInt("deal_reset_id", row);
					
		if (dealReset > PNL_FixingsMarketDataRecorder.S_FX_RESET_OFFSET) {
			dealReset -= PNL_FixingsMarketDataRecorder.S_FX_RESET_OFFSET;
		}
		
		PNL_EntryDataUniqueID entry = new PNL_EntryDataUniqueID(dealNum, dealLeg, 0, dealReset);
		return entry;
	}
		
	/**
	 * Calculate the P&L values according to the algorithm in the JDE extract specification
	 * @param workData
	 * @param marketData
	 * @throws OException
	 */
	private void calculateDerivedDataValues(Table workData, Table marketData) throws OException {
		int rows = workData.getNumRows();
		
		for (int row = 1; row <= rows; row++) {
			PNL_EntryDataUniqueID entry = getUniqueEntry(workData, row);
			
			if (!m_dealMktDataMap.containsKey(entry)) {
				continue;
			}			
			
			double tradePrice = workData.getDouble("trade_price", row);
			double tozSpread = workData.getDouble("toz_spread", row);
			double volume = workData.getDouble("metal_volume_uom", row);
			
			int uom = workData.getInt("uom", row);
			double convFactor = Transaction.getUnitConversionFactor(uom, m_tozUnit);
						
			int metalMktRow = m_dealMktDataMap.get(entry).m_metalRow;
			int ccyMktRow = m_dealMktDataMap.get(entry).m_ccyRow;
			
			double discFactor = marketData.getDouble("usd_df", metalMktRow);
			double fwdFxRate = (ccyMktRow > 0) ? marketData.getDouble("fwd_rate", ccyMktRow) : 1.0;
			double spotFxRate = (ccyMktRow > 0) ? marketData.getDouble("spot_rate", ccyMktRow) : 1.0;
			
			// If the discount factor is 1.0, set forward rate to match spot rate, as per specification
			if (Math.abs(discFactor-1.0) < JDE_Extract_Common.EPSILON) {
				spotFxRate = fwdFxRate;
			}
			
			// Adjust trade price for FX rate spread
			// The one generated by core is "USD_Index_Rate * FX_Rate", but it should be "USD_Index_Rate * (FX_Rate + FX_Spread)"
			// This will match the payment formula on the deal better - note this is not covered by specification as given
			double resetFXRate = workData.getDouble("reset_fx_rate", row);
			double resetFXSpread = workData.getDouble("reset_fx_spread", row);
			
			if (Math.abs(resetFXRate) > JDE_Extract_Common.EPSILON) {				
				if (Math.abs(resetFXSpread) > JDE_Extract_Common.EPSILON) {
					tradePrice = tradePrice * (resetFXRate + resetFXSpread) / resetFXRate;
				}
			}
			
			// Adjust reset price spread (tozSpread) - this is stored on the deal as USD / TOz, even if payment currency is not USD
			// So, adjust tozSpread by (resetFXRate + resetFXSpread)
			if (Math.abs(resetFXRate) > JDE_Extract_Common.EPSILON) {
				tozSpread = tozSpread * (resetFXRate + resetFXSpread);
			}			
			
			// Adjust trade price and tOz spread by a factor based on CB rate and number of days between pricing and delivery
			double cbAdjustmentFactor = workData.getDouble("cb_adj_factor", row);
			if (Math.abs(cbAdjustmentFactor) > JDE_Extract_Common.EPSILON) {
				tradePrice = tradePrice * cbAdjustmentFactor;
				tozSpread = tozSpread * cbAdjustmentFactor;
			}
			
			double fwdMetalRate = marketData.getDouble("fwd_rate", metalMktRow);
			double spotMetalRate = marketData.getDouble("spot_rate", metalMktRow);
			
			// Now run the calculation: spotEquivValue = Volume * Cf*(Sp + Df*(Tp/Cf*Ffx-Fp))/Sfx
			// Adjustment from specification - since reset spread is per TOz, we have to apply it after the conversion
			
			double spotEquivValue = volume * convFactor * (spotMetalRate + discFactor * ((tradePrice / convFactor + tozSpread) * fwdFxRate - fwdMetalRate)) / spotFxRate;
								
			// Settlement value for a single reset is the "reset price" time volume, but reset price needs to be adjusted by 
			// tOz spread (converted to price per deal unit); note that both tradePrice and tozSpread are already adjusted by CB rate factor
			double settlementValue = (tradePrice + tozSpread * convFactor) * volume;
			
			// Implement rounding
			spotEquivValue = Math.round(spotEquivValue * 100) / 100d;
			settlementValue = Math.round(settlementValue * 100) / 100d;
					
			double spotEquivPrice = spotEquivValue / volume;
			spotEquivPrice = Math.round(spotEquivPrice * 10000) / 10000d;
						
			workData.setDouble("settlement_value", row, settlementValue);
			workData.setDouble("spot_equiv_value", row, spotEquivValue);
			workData.setDouble("spot_equiv_price", row, spotEquivPrice);
			workData.setDouble("usd_df", row, discFactor);
			workData.setDouble("fx_fwd_rate", row, fwdFxRate);
			workData.setDouble("fx_spot_rate", row, spotFxRate);
			workData.setDouble("metal_fwd_rate", row, fwdMetalRate);
			workData.setDouble("metal_spot_rate", row, spotMetalRate);
		}
	}

	/**
	 * Retrieve market data as a table
	 * @param trans
	 * @return
	 * @throws OException
	 */	
	private Table prepareMarketData(Table trans) throws OException {		
		Table marketData = new Table("Market Data");
		int queryID = Query.tableQueryInsert(trans, "deal_num");
	
		try {
			String sql =
					"SELECT * FROM USER_jm_pnl_market_data ujpm, query_result qr " +
					"WHERE ujpm.deal_num = qr.query_result and qr.unique_id = " + queryID;
			
			DBase.runSqlFillTable(sql, marketData);
			
		} finally {
			if (queryID > 0) {
				Query.clear(queryID);
			}
		}
		
		return marketData;
	}
	
	/**
	 * Prepare a map of "deal number" to its metal price row and FX conversion row
	 * @param marketData
	 * @throws OException
	 */	
	private void prepareMarketDataMap(Table marketData) throws OException {
		m_dealMktDataMap = new HashMap<PNL_EntryDataUniqueID, MarketData>();
		
		int rows = marketData.getNumRows();
		for (int row = 1; row <= rows; row++) {					
			int dealNum = marketData.getInt("deal_num", row);
			int dealLeg = marketData.getInt("deal_leg", row);			
			int dealReset = marketData.getInt("deal_reset_id", row);
						
			if (dealReset >= PNL_FixingsMarketDataRecorder.S_FX_RESET_OFFSET) {
				dealReset -= PNL_FixingsMarketDataRecorder.S_FX_RESET_OFFSET;
			}
			
			PNL_EntryDataUniqueID entry = new PNL_EntryDataUniqueID(dealNum, dealLeg, 0, dealReset);
			
			int metalCcy = marketData.getInt("metal_ccy", row);
			boolean isMetal = MTL_Position_Utilities.isPreciousMetal(metalCcy);
			
			if (!m_dealMktDataMap.containsKey(entry)) {
				m_dealMktDataMap.put(entry, new MarketData());
			}
			
			MarketData data = m_dealMktDataMap.get(entry);
			
			if (isMetal) {
				data.m_metalRow = row;
			} else {
				data.m_ccyRow = row;
			}
		}
	}

	/**
	 * Returns a table with key fields per transaction object
	 * @param trans - raw argt input transactions table
	 * @return
	 * @throws OException
	 */
	private Table prepareTransactionsData(Table trans) throws OException {
		Table workData = trans.cloneTable();

		workData.addCol("base_ins_type", COL_TYPE_ENUM.COL_INT);
		workData.addCol("ins_sub_type", COL_TYPE_ENUM.COL_INT);
		workData.addCol("toolset", COL_TYPE_ENUM.COL_INT);	
		workData.addCol("fixings_complete", COL_TYPE_ENUM.COL_STRING);
		
		workData.select(trans, "*", "deal_num GE 0");

		int numRows = workData.getNumRows();
		for (int row = 1; row <= numRows; row++) {
			Transaction trn = workData.getTran("tran_ptr", row);

			int baseInsType = Instrument.getBaseInsType(trn.getInsType());
			int insSubType = trn.getFieldInt(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt());
			int toolset = trn.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
			String fixingsComplete = JDE_Extract_Common.S_FIXINGS_COMPLETE_NO;
			
			if (toolset == TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt()) {
				fixingsComplete = areSwapFixingsComplete(trn);
			}
					
			workData.setString("fixings_complete", row, fixingsComplete);
			workData.setInt("base_ins_type", row, baseInsType);
			workData.setInt("ins_sub_type", row, insSubType);
			workData.setInt("toolset", row, toolset);
		}		
		
		return workData;
	}	

	/**
	 * Returns a Y or N dependent on whether all swap fixings are complete
	 * @param trn
	 * @return
	 * @throws OException
	 */
	private String areSwapFixingsComplete(Transaction trn) throws OException {
		boolean bFoundUnfixedReset = false;
		int numParams = trn.getNumRows(-1, TRANF_GROUP.TRANF_GROUP_PARM.toInt());

		for (int param = 0; param < numParams; param++) {
			int totalResetPeriods = trn.getNumRows(param, TRANF_GROUP.TRANF_GROUP_RESET.toInt());
			
			for (int j = 0; j < totalResetPeriods; j++) {			
				int resetStatus = trn.getFieldInt(TRANF_FIELD.TRANF_RESET_VALUE_STATUS.toInt(), param, "", j);		
				if (resetStatus != VALUE_STATUS_ENUM.VALUE_KNOWN.toInt()) {
					bFoundUnfixedReset = true;
					break;
				}
			}
			
			// Stop processing further legs
			if(bFoundUnfixedReset)
				break;
		}
		
		return bFoundUnfixedReset ? JDE_Extract_Common.S_FIXINGS_COMPLETE_NO : JDE_Extract_Common.S_FIXINGS_COMPLETE_YES;
	}

	/**
	 * Create a table with desired output format
	 * @return
	 * @throws OException
	 */
	protected Table createOutputTable() throws OException {
		Table workData = new Table("JDE Extract Data");
		setOutputFormat(workData);
		return workData;
	}
	
	/**
	 * Create columns on input table to match expected UDSR structure
	 * @param workData
	 * @throws OException
	 */
	protected void setOutputFormat(Table workData) throws OException {
		workData.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		workData.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
		workData.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		workData.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
		workData.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);
		workData.addCol("reset_date", COL_TYPE_ENUM.COL_INT);
		
		workData.addCol("fixings_complete", COL_TYPE_ENUM.COL_STRING);

		workData.addCol("from_currency", COL_TYPE_ENUM.COL_INT);
		workData.addCol("to_currency", COL_TYPE_ENUM.COL_INT);

		workData.addCol("delivery_date", COL_TYPE_ENUM.COL_INT);
		
		workData.addCol("settlement_value", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("spot_equiv_value", COL_TYPE_ENUM.COL_DOUBLE);		
		workData.addCol("metal_volume_uom", COL_TYPE_ENUM.COL_DOUBLE);		
		
		workData.addCol("uom", COL_TYPE_ENUM.COL_INT);		
		workData.addCol("spot_equiv_price", COL_TYPE_ENUM.COL_DOUBLE);

		workData.addCol("trade_price", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("toz_spread", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("reset_fx_rate", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("reset_fx_spread", COL_TYPE_ENUM.COL_DOUBLE);
		
		workData.addCol("cb_rate", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("cb_adj_factor", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("cb_day_count", COL_TYPE_ENUM.COL_INT);
		
		workData.addCol("metal_volume_toz", COL_TYPE_ENUM.COL_DOUBLE);		
		workData.addCol("usd_df", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("fx_fwd_rate", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("fx_spot_rate", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("metal_fwd_rate", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("metal_spot_rate", COL_TYPE_ENUM.COL_DOUBLE);		
	}

	/**
	 * Formats the table for user consumption - we keep the original column names so we can match against USER table
	 * @param argt
	 * @param returnt
	 * @throws OException
	 */
	protected void format(Table argt, Table returnt) throws OException {	
		returnt.setColFormatAsRef("from_currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		returnt.setColFormatAsRef("to_currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		
		returnt.setColFormatAsRef("uom", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);

		returnt.setColFormatAsDate("reset_date");
		returnt.setColFormatAsDate("delivery_date");
		
		returnt.setColFormatAsNotnl("metal_volume_uom", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		
		returnt.setColFormatAsNotnl("settlement_value", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColFormatAsNotnl("spot_equiv_value", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		
		returnt.group("deal_num, deal_leg, deal_pdc, deal_reset_id");
	}
}
