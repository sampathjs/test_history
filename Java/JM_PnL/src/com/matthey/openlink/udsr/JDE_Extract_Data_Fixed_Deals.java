package com.matthey.openlink.jde_extract.udsr;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Vector;

import com.matthey.openlink.jde_extract.JDE_Extract_Common;
import com.matthey.openlink.pnl.MTL_Position_Utilities;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Index;
import com.olf.openjvs.Instrument;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.PFOLIO_RESULT_TYPE;
import com.olf.openjvs.enums.RESULT_CLASS;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.USER_RESULT_OPERATIONS;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-06-27	V1.0	mtsteglov	- Initial Version
 * 2017-11-13	V1.1	mstseglov	- Add support for "Is Funding Trade"                                   
 * 2020-05-20	V1.2	jainv02		- EPI-1254 Add support for Implied EFP calculation for Comfut
 */

/**
 * Main Plugin for JDE Extract Data result - Fixed Deals
 * @author mstseglov
 * @version 1.0
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class JDE_Extract_Data_Fixed_Deals implements IScript 
{		
	// We store the row offsets for data for metal price, and any FX conversion
	class MarketData
	{
		int m_metalRow = -1;
		int m_ccyRow = -1;
	}
	
	// For each deal number, store the table row offsets for its metal price, and FX rate properties
	private HashMap<Integer, MarketData> m_dealMktDataMap = null;
	private int m_tozUnit = -1;

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
			OConsole.message(e.toString() + "\r\n");
			PluginLog.error("Plugin: " + this.getClass().getName() + " failed.\r\n");
		}
	}

	protected void calculate(Table argt, Table returnt) throws OException 
	{
		PluginLog.info("Plugin: " + this.getClass().getName() + " calculate called.\r\n");
		
		// Retrieve all relevant pre-requisite results
		Table revalSimResults = argt.getTable("sim_results", 1);						
		Table tranLegResults = revalSimResults.getTable("result_class", RESULT_CLASS.RESULT_TRAN_LEG.toInt());
		
		// Initialise the "toz" unit ID
		m_tozUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, JDE_Extract_Common.S_TOZ_UNIT_NAME);
		
		// Set up output table format
		setOutputFormat(returnt);
		
		// Generate transaction-level data columns
		Table transData = prepareTransactionsData(argt.getTable("transactions", 1));		

		// Retrieve stored market data from USER_JM_PNL_Market_Data
		Table marketData = prepareMarketData(argt.getTable("transactions", 1));
		prepareMarketDataMap(marketData);
		
		// Process FX toolset deals
		PluginLog.info("Process FX toolset deals\n");
		OConsole.message("Process FX toolset deals\n");
		Table fxData = generateFXDataTable(transData, marketData);
		fxData.copyRowAddAllByColName(returnt);

		// Process ComFut toolset deals
		PluginLog.info("Process ComFut toolset deals\n");
		OConsole.message("Process ComFut toolset deals\n");
		Table comFutData = generateComFutDataTable(transData, tranLegResults, marketData);
		comFutData.copyRowAddAllByColName(returnt);		
		
		fxData.destroy();	
		comFutData.destroy();
		marketData.destroy();
	}

	/**
	 * Creates the JDE extract data for ComFut toolset, taking relevant data from Size By Leg and Payment Date By Leg results
	 * @param transData - transaction parameters
	 * @param tranLegResults - "By Leg" results with source data
	 * @param marketData - used to calculate final P&L numbers
	 * @return
	 * @throws OException
	 */
	private Table generateComFutDataTable(Table transData, Table tranLegResults, Table marketData) throws OException
	{	
		Table workData = createOutputTable();	
	
		workData.select(transData, "deal_num, tran_ptr", "toolset EQ " + TOOLSET_ENUM.COM_FUT_TOOLSET.toInt());
		
		// Payment Date is stored as Double, not Integer, so we'll need to convert later
		workData.addCol("pymt_date_d", COL_TYPE_ENUM.COL_DOUBLE);
		
		workData.select(tranLegResults, 
						"currency_id (to_currency), " + 
						PFOLIO_RESULT_TYPE.SIZE_BY_LEG_RESULT.toInt() + "(metal_volume_uom), " + 
						PFOLIO_RESULT_TYPE.PAYMENT_DATE_BY_LEG_RESULT .toInt() + "(pymt_date_d)",
				"deal_num EQ $deal_num AND deal_leg EQ 0 AND deal_pdc EQ 0");
		
		// Show all volumes as positive; note that all futures are expected to be traded in tOz
		workData.mathABSCol("metal_volume_uom");
		workData.copyCol("metal_volume_uom", workData, "metal_volume_toz");
		
		// workData.viewTable();
		
		for (int row = workData.getNumRows(); row >= 1; row--)
		{
			Transaction trn = workData.getTran("tran_ptr", row);
			
			double tradePrice = trn.getFieldDouble(TRANF_FIELD.TRANF_PRICE.toInt());
			int projIdx = trn.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), 0);
			int metal = MTL_Position_Utilities.getCcyForIndex(projIdx);
			
			int deliveryDate = (int) workData.getDouble("pymt_date_d", row);
			double volume = workData.getDouble("metal_volume_toz", row);
			
			double settlementValue = tradePrice * volume;
			
    		// Round settlement value to 2 decimal places
    		settlementValue = Math.round(settlementValue * 100) / 100d;			
			
    		workData.setInt("from_currency", row, metal);    		
    		workData.setInt("delivery_date", row, deliveryDate);
    		workData.setDouble("settlement_value", row, settlementValue);   
    		workData.setDouble("trade_price", row, tradePrice);  
		}
		
		// All fixed price deals are reported as "fixings complete", since there aren't any (unlike swaps)
		workData.setColValString("fixings_complete", JDE_Extract_Common.S_FIXINGS_COMPLETE_YES);
		workData.setColValInt("uom", m_tozUnit);
		
		calculateDerivedDataValues(workData, marketData);
		
		return workData;
	}
	
	private boolean isFundingTrade(Transaction t) throws OException
	{
		try
		{
			String isFundingTrade = t.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, JDE_Extract_Common.S_IS_FUNDING_TRADE_FIELD);
			return "Yes".equalsIgnoreCase(isFundingTrade);
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	/**
	 * Creates the JDE extract data for FX toolset, taking relevant data from transaction properties
	 * @param transData
	 * @param marketData
	 * @return
	 * @throws OException
	 */
	private Table generateFXDataTable(Table transData, Table marketData) throws OException
	{
		Table workData = createOutputTable();	
				
		workData.select(transData, "deal_num, tran_ptr, trade_price, ins_sub_type, near_leg_tran_num", "toolset EQ " + TOOLSET_ENUM.FX_TOOLSET.toInt());
		
		// Iterate backwards, as we may delete current row in place
		for (int row = workData.getNumRows(); row >= 1; row--)
		{
			Transaction trn = workData.getTran("tran_ptr", row);
			
			int insSubType = workData.getInt("ins_sub_type", row);
			
    		int baseCurrency = -1; 
    		int termCurrency = -1; 
 			int metalLeg = -1, ccyLeg = -1;
 			int fromCcy = -1, toCcy = -1;
 			int deliveryDate = -1;
 			double volumeTOZ, volumeUOM = 0.0; 			
 			int uom = m_tozUnit;
 			double settlementValue = 0.0;	 			
 			
 			// Retrieve as String to get the value in native units - retrieving as double just gives tOz volume 
 			String volumeStr = ""; 
 			
 			// Retrieve whether this transaction is "Funding" or not, and store appropriately
 			boolean isFundingTrade = isFundingTrade(trn);
			
 			// Normal handling for FX transactions
			if (insSubType != INS_SUB_TYPE.fx_far_leg.toInt())
			{
	 			baseCurrency = trn.getFieldInt(TRANF_FIELD.TRANF_BASE_CURRENCY.jvsValue());
	 			termCurrency = trn.getFieldInt(TRANF_FIELD.TRANF_BOUGHT_CURRENCY.jvsValue());	 			
	 			
	 			// There are two ways to a model a USD-XPT deal, and both are in use for various metal-ccy pairs
	 			// In one, XPT is the base currency, in another, it is the term currency
	 			// Depending on which it is, we need to pick up different fields
	 			
	    		if (MTL_Position_Utilities.isPreciousMetal(baseCurrency))
	    		{
	    			metalLeg = 0;
	    			ccyLeg = 1;
	    			fromCcy = baseCurrency;
	    			toCcy = termCurrency;
	    			deliveryDate = trn.getFieldInt(TRANF_FIELD.TRANF_SETTLE_DATE.jvsValue());
	    			volumeStr = trn.getField(TRANF_FIELD.TRANF_FX_D_AMT.jvsValue(), 0);
	    			volumeTOZ = trn.getFieldDouble(TRANF_FIELD.TRANF_FX_D_AMT.jvsValue());
	    			uom = trn.getFieldInt(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.jvsValue());
	    			
	    			settlementValue = trn.getFieldDouble(TRANF_FIELD.TRANF_FX_C_AMT.jvsValue());
	    		}
	    		else if (MTL_Position_Utilities.isPreciousMetal(termCurrency))
	    		{
	    			metalLeg = 1;
	    			ccyLeg = 0;
	    			fromCcy = termCurrency;
	    			toCcy = baseCurrency;    			
	    			deliveryDate = trn.getFieldInt(TRANF_FIELD.TRANF_FX_TERM_SETTLE_DATE.jvsValue());
	    			volumeStr = trn.getField(TRANF_FIELD.TRANF_FX_C_AMT.jvsValue(), 0);
	    			volumeTOZ = trn.getFieldDouble(TRANF_FIELD.TRANF_FX_C_AMT.jvsValue());
	    			uom = trn.getFieldInt(TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT.jvsValue());
	    			
	    			settlementValue = trn.getFieldDouble(TRANF_FIELD.TRANF_FX_D_AMT.jvsValue());
	    		}
	    		else
	    		{
	    			// Currency - to - Currency deal, skip for now
	    			workData.delRow(row);
	    			continue;
	    		}				
			}
			else
			{
				// FX swaps are modeled as two separate deals - near leg, and far leg;
				// however, the far leg does not hold any relevant deal information we need, so retrieve
				// the "near leg" side, and query its "far leg" properties instead
				int nearLegTranNum = workData.getInt("near_leg_tran_num", row);
				Transaction nearLegTrn = Transaction.retrieve(nearLegTranNum);
				
	 			baseCurrency = nearLegTrn.getFieldInt(TRANF_FIELD.TRANF_BASE_CURRENCY.jvsValue());
	 			termCurrency = nearLegTrn.getFieldInt(TRANF_FIELD.TRANF_BOUGHT_CURRENCY.jvsValue());
	 			
	 			// There are two ways to a model a USD-XPT deal, and both are in use for various metal-ccy pairs
	 			// In one, XPT is the base currency, in another, it is the term currency
	 			// Depending on which it is, we need to pick up different fields	 			
	    		if (MTL_Position_Utilities.isPreciousMetal(baseCurrency))
	    		{
	    			metalLeg = 0;
	    			ccyLeg = 1;
	    			fromCcy = baseCurrency;
	    			toCcy = termCurrency;
	    			deliveryDate = nearLegTrn.getFieldInt(TRANF_FIELD.TRANF_FX_FAR_BASE_SETTLE_DATE.jvsValue());
	    			volumeStr = nearLegTrn.getField(TRANF_FIELD.TRANF_FX_FAR_D_AMT.jvsValue(), 0);
	    			volumeTOZ = nearLegTrn.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_D_AMT.jvsValue());
	    			uom = nearLegTrn.getFieldInt(TRANF_FIELD.TRANF_FX_FAR_BASE_UNIT.jvsValue());
	    			
	    			settlementValue = nearLegTrn.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_C_AMT.jvsValue());
	    		}
	    		else if (MTL_Position_Utilities.isPreciousMetal(termCurrency))
	    		{
	    			metalLeg = 1;
	    			ccyLeg = 0;
	    			fromCcy = termCurrency;
	    			toCcy = baseCurrency;    			
	    			deliveryDate = nearLegTrn.getFieldInt(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.jvsValue());
	    			volumeStr = nearLegTrn.getField(TRANF_FIELD.TRANF_FX_FAR_C_AMT.jvsValue(), 0);
	    			volumeTOZ = nearLegTrn.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_C_AMT.jvsValue());
	    			uom = nearLegTrn.getFieldInt(TRANF_FIELD.TRANF_FX_FAR_TERM_UNIT.jvsValue());
	    			
	    			settlementValue = trn.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_D_AMT.jvsValue());
	    		}
	    		else
	    		{
	    			// Currency - to - Currency deal, skip for now
	    			workData.delRow(row);
	    			nearLegTrn.destroy();
	    			continue;
	    		}

	    		nearLegTrn.destroy();
			}
			 			
			// Convert the string representation, dropping any thousand separators
    		try
    		{    			
    			volumeUOM = Double.parseDouble(volumeStr.replace(",",""));    			
    		}
    		catch (Exception e)
    		{
				PluginLog.error("Plugin: " + e.toString() +"\n");
    			OConsole.message(e.toString() + "\n");
    		}
    		
    		// Round settlement value to 2 decimal places
    		settlementValue = Math.round(settlementValue * 100) / 100d;
    		    		
    		workData.setInt("from_currency", row, fromCcy);
    		workData.setInt("to_currency", row, toCcy);
    		workData.setInt("delivery_date", row, deliveryDate);
    		workData.setInt("uom", row, uom);
    		workData.setDouble("metal_volume_uom", row, volumeUOM);
    		workData.setDouble("metal_volume_toz", row, volumeTOZ); 
    		workData.setDouble("settlement_value", row, settlementValue);
    		workData.setInt("is_funding_trade", row, isFundingTrade ? 1 : 0);
		}
		
		workData.setColValString("fixings_complete", JDE_Extract_Common.S_FIXINGS_COMPLETE_YES);
		
		// workData.viewTable();

		calculateDerivedDataValues(workData, marketData);
		
		return workData;
	}
	
	private void calculateDerivedDataValues(Table workData, Table marketData) throws OException
	{
		
		for (int row = 1; row <= workData.getNumRows(); row++)
		{
			int dealNum = workData.getInt("deal_num", row);
			
			if (!m_dealMktDataMap.containsKey(dealNum))
			{
				continue;
			}		
			Transaction trn = workData.getTran("tran_ptr", row);
			int toolset = trn.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt(),0);
			double impliedEFP = 0.0;
			if(toolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt() ){
				impliedEFP = getEFP(dealNum);
			}

			double tradePrice = workData.getDouble("trade_price", row);
			double volume = workData.getDouble("metal_volume_uom", row);
			boolean isFundingTrade = (workData.getInt("is_funding_trade", row) > 0);
			
			int uom = workData.getInt("uom", row);
			double convFactor = Transaction.getUnitConversionFactor(uom, m_tozUnit);
						
			int metalMktRow = m_dealMktDataMap.get(dealNum).m_metalRow;
			int ccyMktRow = m_dealMktDataMap.get(dealNum).m_ccyRow;
			
			double discFactor = marketData.getDouble("usd_df", metalMktRow);
			double fwdFxRate = marketData.getDouble("fwd_rate", ccyMktRow);
			double spotFxRate = marketData.getDouble("spot_rate", ccyMktRow);
			
			// If the discount factor is 1.0, set forward rate to match spot rate, as per specification 
			if (Math.abs(discFactor-1.0) < JDE_Extract_Common.EPSILON)
			{
				spotFxRate = fwdFxRate;
			}
			
			double fwdMetalRate = marketData.getDouble("fwd_rate", metalMktRow);
			double spotMetalRate = marketData.getDouble("spot_rate", metalMktRow);
						
			double spotEquivValue = 0.0;

			if(toolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt() && Math.abs(impliedEFP) > 0.0){
				spotEquivValue = volume * (tradePrice - (impliedEFP * discFactor));
			}
			else if (isFundingTrade)
			{
				// For funding trades, these are made at going rate for P&L reporting purposes
				spotEquivValue = volume * convFactor * spotMetalRate / spotFxRate;
			}
			else
			{
				// The generic use case: spotEquivValue = Volume * Cf*(Sp + Df*(Tp/Cf*Ffx-Fp))/Sfx
				spotEquivValue = volume * convFactor * (spotMetalRate + discFactor * (tradePrice / convFactor * fwdFxRate - fwdMetalRate)) / spotFxRate;
			}
			
			// Now, do the rounding in following order
			spotEquivValue = Math.round(spotEquivValue * 100) / 100d;
			
			// Calculate spot equivalent price to four decimal places
			// Note that we back-solve the spot-equivalent-price from spot-equivalent-value after it has been rounded
			// This matches the specification given
			double spotEquivPrice = spotEquivValue / volume;
			spotEquivPrice = Math.round(spotEquivPrice * 10000) / 10000d;			
			
			workData.setDouble("spot_equiv_value", row, spotEquivValue);
			workData.setDouble("spot_equiv_price", row, spotEquivPrice);
			workData.setDouble("usd_df", row, discFactor);
			workData.setDouble("fx_fwd_rate", row, fwdFxRate);
			workData.setDouble("fx_spot_rate", row, spotFxRate);
			workData.setDouble("metal_fwd_rate", row, fwdMetalRate);
			workData.setDouble("metal_spot_rate", row, spotMetalRate);
			workData.setDouble("conv_factor", row, convFactor);
		}
	}

	

	private double getEFP(int dealNum) throws OException{
		Table result = Util.NULL_TABLE;
		
		
		double impliedEFP = BigDecimal.ZERO.doubleValue();
		try{
			result = Table.tableNew();
			String sql = "SELECT implied_efp from USER_jm_implied_efp WHERE deal_num = " + dealNum;

			DBaseTable.execISql(result, sql);

			if(result.getNumRows() == 1){
				impliedEFP = result.getDouble(1, 1);
			}

			return impliedEFP;

		}finally{
			if(Table.isTableValid(result) == 1){
				result.destroy();
			}
		}
	}

	/**
	 * Retrieve market data as a table
	 * @param trans
	 * @return
	 * @throws OException
	 */
	private Table prepareMarketData(Table trans) throws OException
	{		
		Table marketData = new Table("Market Data");
		int queryID = Query.tableQueryInsert(trans, "deal_num");
	
		String sql =
				"SELECT * FROM USER_jm_pnl_market_data ujpm, query_result qr " +
				"WHERE ujpm.deal_num = qr.query_result and qr.unique_id = " + queryID;
		
		DBase.runSqlFillTable(sql, marketData);
		
		Query.clear(queryID);		
		
		// marketData.viewTable();
		
		return marketData;
	}
	
	/**
	 * Prepare a map of "deal number" to its metal price row and FX conversion row
	 * @param marketData
	 * @throws OException
	 */
	private void prepareMarketDataMap(Table marketData) throws OException
	{
		m_dealMktDataMap = new HashMap<Integer, MarketData>();
		
		for (int row = 1; row <= marketData.getNumRows(); row++)
		{
			int dealNum = marketData.getInt("deal_num", row);
			int metalCcy = marketData.getInt("metal_ccy", row);
			boolean isMetal = MTL_Position_Utilities.isPreciousMetal(metalCcy);
			
			if (!m_dealMktDataMap.containsKey(dealNum))
			{
				m_dealMktDataMap.put(dealNum, new MarketData());
			}
			
			MarketData data = m_dealMktDataMap.get(dealNum);
			
			if (isMetal)
			{
				data.m_metalRow = row;
			}
			else
			{
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
	private Table prepareTransactionsData(Table trans) throws OException
	{
		Table workData = trans.cloneTable();

		workData.addCol("base_ins_type", COL_TYPE_ENUM.COL_INT);
		workData.addCol("ins_sub_type", COL_TYPE_ENUM.COL_INT);
		workData.addCol("toolset", COL_TYPE_ENUM.COL_INT);	
		workData.addCol("near_leg_tran_num", COL_TYPE_ENUM.COL_INT);
		workData.addCol("trade_price", COL_TYPE_ENUM.COL_DOUBLE);
		workData.select(trans, "*", "deal_num GE 0");

		int numRows = workData.getNumRows();
		
		Vector<Integer> fxFarLegDeals = new Vector<Integer>();

		for (int row = 1; row <= numRows; row++) 
		{
			Transaction trn = workData.getTran("tran_ptr", row);

			int baseInsType = Instrument.getBaseInsType(trn.getInsType());
			int insSubType = trn.getFieldInt(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt());
			int toolset = trn.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
			
			// "Trade Price" is stored as a Tran Info field to list the price per original unit
			// We need to retrieve and parse it, as core price is per tOz instead
			double tradePrice = 0.0;
			String tradePriceStr = trn.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, JDE_Extract_Common.S_TRADE_PRICE_FIELD);

			try
			{
				if ((tradePriceStr != null) && (tradePriceStr.trim().length() > 0))
				{
					tradePrice = Double.parseDouble(tradePriceStr);
				}				
			}
			catch (Exception e)
			{
				PluginLog.error("Plugin: " + e.toString() +"\n");
				OConsole.message(e.toString() + "\n");
			}
			
			// FX far legs require special consideration, as we need to retrieve relevant data from a different transaction
			// that stores the near leg
			if (insSubType == INS_SUB_TYPE.fx_far_leg.toInt())
			{
				fxFarLegDeals.add(workData.getInt("deal_num", row));
			}
					
			workData.setInt("base_ins_type", row, baseInsType);
			workData.setInt("ins_sub_type", row, insSubType);
			workData.setInt("toolset", row, toolset);
			workData.setDouble("trade_price", row, tradePrice);
		}
		
		// Now, retrieve the appropriate matching "FX near leg" transaction for "FX far leg" transactions
		if (fxFarLegDeals.size() > 0)
		{
			Table output = new Table("");
			Table queryData = new Table("");
			queryData.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
			queryData.addNumRows(fxFarLegDeals.size());
			for (int offset = 0; offset < fxFarLegDeals.size(); offset++)
			{
				queryData.setInt("deal_num", offset + 1, fxFarLegDeals.get(offset));
			}			
			int queryID = Query.tableQueryInsert(queryData, "deal_num");
			
			// Identify the corresponding near leg per each far leg deal
			String sql = "select ab.tran_num tran_num, ab2.tran_num near_leg_tran_num " + 
						"from ab_tran ab, ab_tran ab2, query_result qr " +
						"where ab.tran_group = ab2.tran_group and ab.ins_sub_type = 4001 and " +
						"ab2.ins_sub_type = 4000 and ab.current_flag = 1 and ab2.current_flag = 1 and " +
						"qr.query_result = ab.deal_tracking_num and qr.unique_id = " + queryID;
			
			DBase.runSqlFillTable(sql, output);
			
			workData.select(output, "near_leg_tran_num", "tran_num EQ $tran_num");
			
			Query.clear(queryID);			
			output.destroy();
			queryData.destroy();
		}

		
		return workData;
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
				
		workData.addCol("uom", COL_TYPE_ENUM.COL_INT);		
		workData.addCol("spot_equiv_price", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("metal_volume_toz", COL_TYPE_ENUM.COL_DOUBLE);		
		workData.addCol("trade_price", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("usd_df", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("fx_fwd_rate", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("fx_spot_rate", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("metal_fwd_rate", COL_TYPE_ENUM.COL_DOUBLE);
		workData.addCol("metal_spot_rate", COL_TYPE_ENUM.COL_DOUBLE);	
		workData.addCol("conv_factor", COL_TYPE_ENUM.COL_DOUBLE);
		
		workData.addCol("is_funding_trade", COL_TYPE_ENUM.COL_INT);
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
		returnt.setColFormatAsRef("is_funding_trade", SHM_USR_TABLES_ENUM.YES_NO_TABLE);
		
		returnt.setColFormatAsDate("delivery_date");
		
		returnt.setColFormatAsNotnl("metal_volume_uom", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		
		returnt.setColFormatAsNotnl("settlement_value", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
		returnt.setColFormatAsNotnl("spot_equiv_value", 12, 4, COL_FORMAT_BASE_ENUM.BASE_NONE.jvsValue());
	}
}
