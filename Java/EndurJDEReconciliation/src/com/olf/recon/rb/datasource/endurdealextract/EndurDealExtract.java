package com.olf.recon.rb.datasource.endurdealextract;

import java.math.BigDecimal;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.rb.datasource.EndurExtract;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.Util;
import com.olf.jm.logging.Logging;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class EndurDealExtract extends EndurExtract {
	
	@Override
	protected void setOutputFormat(Table output) throws OException 
	{
		output.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("ins_type", COL_TYPE_ENUM.COL_INT);
		output.addCol("tran_status", COL_TYPE_ENUM.COL_INT);
		output.addCol("buy_sell", COL_TYPE_ENUM.COL_INT);
		output.addCol("internal_lentity", COL_TYPE_ENUM.COL_INT);
		output.addCol("internal_lentity_str", COL_TYPE_ENUM.COL_STRING);
       	output.addCol("counterparty", COL_TYPE_ENUM.COL_INT);
       	output.addCol("counterparty_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("trade_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("trade_date_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("value_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("value_date_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("metal", COL_TYPE_ENUM.COL_INT);
		output.addCol("metal_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("currency", COL_TYPE_ENUM.COL_INT);
		output.addCol("currency_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("position_metal_unit", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("position_toz", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("settlement_value", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("spot_equivalent_price", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("spot_equivalent_value", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("reconciliation_note", COL_TYPE_ENUM.COL_STRING);
	}

	@Override
	protected Table generateOutput(Table output) throws OException 
	{
		Table tblFxDeals = null;
		Table tblComFutDeals = null;
		Table tblMetalSwaps = null;
		Table tblSpotEquiv = null;
		
		try
		{
			FXDeals fxDeals = new FXDeals(windowStartDate, windowEndDate, lastTradeDate);
			ComFut comFutDeals = new ComFut(windowStartDate, windowEndDate,lastTradeDate);
			MetalSwaps metalSwapDeals = new MetalSwaps(windowStartDate, windowEndDate, region,lastTradeDate);
			
			tblFxDeals = fxDeals.getData();
			tblComFutDeals = comFutDeals.getData();
			tblMetalSwaps = metalSwapDeals.getData();
			
			tblFxDeals.copyRowAddAllByColName(output);
			tblComFutDeals.copyRowAddAllByColName(output);
			tblMetalSwaps.copyRowAddAllByColName(output);
			
			/* 
			 *  Reconciliation currently only covers financial scenarios. PMM UK
			 *  confirmed that they do not intend to reconcile physical scenarios, and ML
			 *  is not part of the UK interface anyway.
			 *  
			 *  However, COMM-PHYS deals are sent to the ML for US (HK unknown at the time of writing this - Oct 2017)
			 *  The below snippet is here as a place holder in case COMM-PHYS need to be added to reconciliation
			 *  
			 *  if (!region.equalsIgnoreCase(ReportingDeskName.UK.toString()))
			 *	{
			 *		CommPhys commPhys = new CommPhys(windowStartDate, windowEndDate);
			 *		tblCommPhys = commPhys.getData();
			 *		tblCommPhys.copyRowAddAllByColName(output);
			 *	}
			 */
			
			Logging.info("Enriching Supplementary Ref Data (ins type, party details)");
			enrichSupplementaryData(output);
			
			/* Enrich spot equivalent price */
			Logging.info("Populating SpotEquivalent");
			enrichSpotEquivalentInfo(output,region);
			
			/* Enrich spot equivalent for migrated deals */
			Logging.info("Populating SpotEquivalent for migrated deals");
			enrichSpotEquivalentForMigratedDeals(output);
			
			/* Calculate position and settlement */
			calculatePositionAndSettlement(output);

			/* Filter out rows which are not needed as per reference data param config in Report Builder */
			filterCounterparties(output, excludedCounterparties);
			filterIncludedLentites(output, includedLentites);
			
			/* Remove same day cancellations from output as these are not sent to JDE */
			removeSameDayCancellations(output);
			
			/* Remove rows that don't have a specific external bunit party info populated depending on the region etc */
			removeNonRegionalRecords(output);
			
			/* Add reconciliation notes */
			addReconciliationNotes(output, Constants.USER_JM_DEAL_REC_NOTES, "deal_num", "deal_num");
			
			/* Apply rounding */
			round(output);
			
			return output;
		}
		finally
		{
			if (tblFxDeals != null) 
			{
				tblFxDeals.destroy();
			}
			
			if (tblComFutDeals != null) 
			{
				tblComFutDeals.destroy();
			}
			
			if (tblMetalSwaps != null)
			{
				tblMetalSwaps.destroy();
			}
			
			if (tblSpotEquiv != null)
			{
				tblSpotEquiv.destroy();
			}
		}	
	}

	@Override
	protected void registerConversions(Table output) throws OException 
	{
		regRefConversion(output, "ins_type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		regRefConversion(output, "tran_status", SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
		regRefConversion(output, "metal", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		regRefConversion(output, "currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		regRefConversion(output, "buy_sell", SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
	}

	@Override
	protected void groupOutputData(Table output) throws OException 
	{
		output.group("deal_num");
		output.groupBy();
	}
	
	@Override
	protected void formatOutputData(Table output) throws OException 
	{
		output.copyColFormatDate("trade_date", "trade_date_str", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
		output.copyColFormatDate("value_date", "value_date_str", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
		output.copyColFromRef("metal", "metal_str", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		output.copyColFromRef("currency", "currency_str", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);	
		
		/* Use Reg.getShortName as Ref.GetName only returns the first 30 chars - known OLF issue */ 
		int numRows = output.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			int externalBunitId = output.getInt("counterparty", row);
			int internalLentityId = output.getInt("internal_lentity", row);
			
			output.setString("counterparty_str", row, Ref.getShortName(SHM_USR_TABLES_ENUM.PARTY_TABLE, externalBunitId));
			output.setString("internal_lentity_str", row, Ref.getShortName(SHM_USR_TABLES_ENUM.PARTY_TABLE, internalLentityId));
		}
	}
	
	/**
	 * Adjust spot equivalent signage as it gets recalculated using the "position_metal_unit" column
	 * 
	 * @param output
	 * @throws OException
	 */
	private void calculatePositionAndSettlement(Table output) throws OException 
	{
		int numRows = output.getNumRows();
		
		for (int row = 1; row <= numRows; row++)
		{
			int buySell = output.getInt("buy_sell", row);
			int tranStatus = output.getInt("tran_status", row);
			double positionMetalUnit = Math.abs(output.getDouble("position_metal_unit", row));
			double positionToz = Math.abs(output.getDouble("position_toz", row));
			double settlementValue = Math.abs(output.getDouble("settlement_value", row));
			double spotEquivalentValue = Math.abs(output.getDouble("spot_equivalent_value", row));
			
			int counterparty = output.getInt("counterparty", row);
			String counterpartyStr = Ref.getShortName(SHM_USR_TABLES_ENUM.PARTY_TABLE, counterparty);
			
			if (buySell == BUY_SELL_ENUM.SELL.toInt())
			{
				/* 
				 * If buy, metal position is positive, cash is negative
				 * If sell, metal position is negative, cash is positive
				 */
				positionMetalUnit *= -1;
				positionToz *= -1;
			}
			else if (buySell == BUY_SELL_ENUM.BUY.toInt())
			{
				settlementValue *= -1;
				spotEquivalentValue *= -1;
			}
			
			output.setDouble("position_metal_unit", row, positionMetalUnit);
			output.setDouble("position_toz", row, positionToz);
			output.setDouble("spot_equivalent_value", row, spotEquivalentValue);
			output.setDouble("settlement_value", row, settlementValue);
			
			/* 
			 * For metal swaps, set spot price fields to zero as we are not interested in reconciling these. 
			 * Same for FX curency trades that face JM TREASURY 
			 */
			if ("JM TREASURY - BU".equalsIgnoreCase(counterpartyStr))
			{
				output.setDouble("spot_equivalent_price", row, 0.0);
				output.setDouble("spot_equivalent_value", row, 0.0);		
			}

			/* Set all monetary fields to zero for Cancelled trades */
			if (tranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt())
			{
				output.setDouble("position_metal_unit", row, 0.0);
				output.setDouble("position_toz", row, 0.0);
				output.setDouble("settlement_value", row, 0.0);
				output.setDouble("spot_equivalent_price", row, 0.0);
				output.setDouble("spot_equivalent_value", row, 0.0);
			}
		}
	}
	
	/**
	 * Enrich Spot equivalent data for trades using the db table 'USER_jm_jde_extract_data'
	 * 
	 * @param tblData
	 * @throws OException
	 */
	private void enrichSpotEquivalentInfo(Table tblData,String region) throws OException
	{
		if (tblData.getNumRows() <= 0)
		{
			return;
		}
		
		int queryId = 0;
		Table marketData = null;
		
		try
		{
			queryId = Query.tableQueryInsert(tblData, "deal_num");
			
			if (queryId > 0)
			{
				marketData = Table.tableNew("Spot Equiv Market Data");

				String sqlQuery = 
					"SELECT \n" +
						"qr.query_result as deal_num, \n" +
						"ujde.spot_equiv_price as spot_equivalent_price, \n " +
						"ujde.spot_equiv_value AS spot_equivalent_value \n" +
					"FROM query_result qr \n" +
					"JOIN " + Constants.USER_JM_JDE_EXTRACT_DATA_HIST + " ujde ON qr.query_result = ujde.deal_num \n" +
					"JOIN (select deal_num,max(hist_last_update) as update_time from "+ Constants.USER_JM_JDE_EXTRACT_DATA_HIST + "\n"+
					"where hist_update_type=0 and hist_last_update <= (select max(extraction_end_time) from user_jm_ledger_extraction where region like '"+region+"') \n"+
				    "GROUP BY deal_num) t on t.deal_num=ujde.deal_num and t.update_time = ujde.hist_last_update \n"+
				    "WHERE qr.unique_id = " + queryId + "\n" +
				    "AND   ujde.hist_update_type = 0";
				    		

				int ret = DBaseTable.execISql(marketData, sqlQuery);

				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new ReconciliationRuntimeException("Unable to run query: " + sqlQuery);
				}

				tblData.select(marketData, "spot_equivalent_price, spot_equivalent_value", "deal_num EQ $deal_num");				
			}
		}
		finally
		{
			if (marketData != null)
			{
				marketData.destroy();
			}
			
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
		}
	}

	/**
	 * Get spot equivalent for migrated trades - this is stored in user_migr_deals_all
	 * 
	 * @param tblData
	 * @throws OException
	 */
	private void enrichSpotEquivalentForMigratedDeals(Table tblData) throws OException 
	{
		Table tblOutput = null;
		int queryId = 0;
		
		try
		{
			if (tblData.getNumRows() > 0)
			{
				queryId = Query.tableQueryInsert(tblData, "deal_num");
				
				if (queryId > 0)
				{
					tblOutput = Table.tableNew();
					
					String sqlQuery = 
						"SELECT \n" +
							"ab.deal_tracking_num AS deal_num, \n" +
							"ab.ins_type, ab.tran_status, \n" +
							"ab.internal_lentity, \n" +
							"ab.external_bunit, \n" +
						"CAST(umd.sprceq AS FLOAT) AS spot_equivalent_price \n" +
						"FROM query_result qr \n" +
						"JOIN ab_tran ab ON qr.query_result = ab.deal_tracking_num  \n" +
						"JOIN " + Constants.USER_MIGR_DEALS_ALL + " umd ON umd.booked_deal_num = ab.deal_tracking_num \n" +
						"WHERE qr.unique_id = " + queryId + " \n" +
						"AND ab.current_flag = 1";
							
					int ret = DBaseTable.execISql(tblOutput, sqlQuery);
					
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						throw new ReconciliationRuntimeException("Unable to run query: " + sqlQuery);
					}
					
					tblData.select(tblOutput, "spot_equivalent_price", "deal_num EQ $deal_num");
				}
			}	
		}
		finally
		{
			if (tblOutput != null)
			{
				tblOutput.destroy();
			}
			
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
		}
	}
	
	/**
	 * Round cash amounts and positions as specified
	 * 
	 * @param output
	 * @throws OException
	 */
	private void round(Table output) throws OException
	{
		int numRows = output.getNumRows();
		
		for (int row = 1; row <= numRows; row++)
		{
			double positionToz = output.getDouble("position_toz", row);
			double settlementValue = output.getDouble("settlement_value", row);
			double spotEquivalentValue = output.getDouble("spot_equivalent_value", row);

            BigDecimal positionTozBD = Util.roundPosition(positionToz);
            BigDecimal settlementValueBD = Util.roundCashAmount(settlementValue);
            BigDecimal spotEquivalentValueBD = Util.roundCashAmount(spotEquivalentValue);
            
			double roundedPositionToz = positionTozBD.doubleValue();
			double roundedSettlementValue = settlementValueBD.doubleValue();
			double roundedSpotEquivalentValue = spotEquivalentValueBD.doubleValue();
			
			output.setDouble("position_toz", row, roundedPositionToz);
			output.setDouble("settlement_value", row, roundedSettlementValue);
			output.setDouble("spot_equivalent_value", row, roundedSpotEquivalentValue);
		}
	}
}
