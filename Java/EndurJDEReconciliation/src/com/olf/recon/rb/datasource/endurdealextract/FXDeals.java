package com.olf.recon.rb.datasource.endurdealextract;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_TYPE_ENUM;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.Util;
import com.olf.jm.logging.Logging;

/**
 * Gathers FX related deal attributes for reconciliation
 */
public class FXDeals extends AbstractEndurDealExtract 
{
	public FXDeals(int windowStartDate, int windowEndDate, int lastTradeDate) throws OException 
	{
		super(windowStartDate, windowEndDate,lastTradeDate);
	}

	@Override
	protected Table getData() throws OException 
	{
		Logging.info("Fetching FX data..");
		
		Table tblFX = Table.tableNew("FX deals");
		
		/* Return all FX trades (forwards, swaps, spots) that are interfaced to JDE */
		String sqlQuery = 	
			"SELECT \n" +
				"ab.deal_tracking_num AS deal_num, \n" +
				"ab.cflow_type, \n" +
				"ab.trade_date, \n" +
				"ab.buy_sell, \n" +
				"abte0.currency AS metal_currency, \n" +
				"abte0.unit AS metal_unit, -- unit display table \n" +
				"abte0.event_date AS metal_event_date, \n" + 
				"abte0.para_position AS metal_position, \n" +
				"abte1.currency AS financial_currency, \n" +
				"abte1.unit AS financial_unit, \n" +
				"abte1.event_date AS financial_event_date, \n" +
				"abte1.para_position AS financial_position, \n" +
				"fx.ccy1, \n" +
				"fx.ccy2 \n" +
			"FROM \n" +
			"ab_tran ab \n" +
			"LEFT JOIN ab_tran_event abte0 ON ab.tran_num = abte0.tran_num AND (abte0.event_type = 14 AND abte0.ins_para_seq_num = 0) \n" +
			"LEFT JOIN ab_tran_event abte1 ON ab.tran_num = abte1.tran_num AND (abte1.event_type = 14 AND abte1.ins_para_seq_num = 1) \n" +
			"LEFT JOIN fx_tran_aux_data fx ON ab.tran_num = fx.tran_num \n" +
			"WHERE ab.toolset = " + TOOLSET_ENUM.FX_TOOLSET.toInt() + " \n" +
			"AND ab.current_flag = 1 \n" +
			"AND ab.tran_type = " + TRAN_TYPE_ENUM.TRAN_TYPE_TRADING.toInt() + " \n" +
			"AND ab.tran_status IN (" + getApplicableTransactionStatusesForSQL() + ") \n" + 
			"AND abte0.event_date >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' AND ab.trade_date <= '" + OCalendar.formatJdForDbAccess(lastTradeDate) + "' \n" +
			"AND abte0.event_date <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "' \n" +

			"ORDER by deal_tracking_num";
		
		int ret = DBaseTable.execISql(tblFX, sqlQuery);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException("Unable to run query: " + sqlQuery);
		}
		
		/* Convert datetime fields to int for ease of processing */
		tblFX.colConvertDateTimeToInt("metal_event_date");
		tblFX.colConvertDateTimeToInt("financial_event_date");
		tblFX.colConvertDateTimeToInt("trade_date");
			
		/* Add supplementary columns for superclass */
		tblFX.addCol("metal", COL_TYPE_ENUM.COL_INT);
		tblFX.addCol("currency", COL_TYPE_ENUM.COL_INT);
		tblFX.addCol("position_metal_unit", COL_TYPE_ENUM.COL_DOUBLE);
		tblFX.addCol("position_toz", COL_TYPE_ENUM.COL_DOUBLE);
		tblFX.addCol("settlement_value", COL_TYPE_ENUM.COL_DOUBLE);
		tblFX.addCol("value_date", COL_TYPE_ENUM.COL_INT);
	
		int toz = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, Constants.TROY_OUNCES);
		int numRows = tblFX.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			int metalCurrency = tblFX.getInt("metal_currency", row);
			int metalUnit = tblFX.getInt("metal_unit", row);
			int financialCurrency = tblFX.getInt("financial_currency", row);
			double metalPosition = tblFX.getDouble("metal_position", row);
			double metalPositionToz = metalPosition;
			double financialPosition = tblFX.getDouble("financial_position", row);
			int metalEventDate = tblFX.getInt("metal_event_date", row);
			int financialEventDate = tblFX.getInt("financial_event_date", row);
			int fxAuxCurrency1 = tblFX.getInt("ccy1", row);
			int fxAuxCurrency2 = tblFX.getInt("ccy2", row);
			
			/* Is there a precious metal component to this deal? */
			boolean isPreciousMetal = Util.isPreciousMetalCurrency(metalCurrency) || Util.isPreciousMetalCurrency(financialCurrency);
			
			if (isPreciousMetal)
			{
				/* Metal settlement date */
				tblFX.setInt("value_date", row, metalEventDate);
			}
			else
			{
				/* Cash settle date */
				tblFX.setInt("value_date", row, financialEventDate);
			}

			if (metalUnit != toz)
			{
				/* 
				 * The db stores all values as Toz (base unit) in ab_tran_events. So if this is a Kg trade (or any other unit different to Toz), 
				 * convert from Toz > trade unit 
				 */
				metalPosition *= Transaction.getUnitConversionFactor(toz, metalUnit);
			}

			if (!isPreciousMetal)
			{
				/* 
				 * For treasury based trades, there is an issue with the tran events not holding the ins seq num events in the 
				 * correct order so we need to perform a flip for such scenarios.
				 */
				double metalPositionTozBeforeFlip = metalPositionToz;
				if (metalCurrency != fxAuxCurrency1)
				{
					metalCurrency = fxAuxCurrency1;
					
					metalPosition = financialPosition;
					metalPositionToz = financialPosition;
				}
				
				if (financialCurrency != fxAuxCurrency2)
				{
					financialCurrency = fxAuxCurrency2;
					financialPosition = metalPositionTozBeforeFlip;
				}
			}

			tblFX.setInt("metal", row, metalCurrency);
			tblFX.setInt("currency", row, financialCurrency);
			tblFX.setDouble("settlement_value", row, financialPosition);
			tblFX.setDouble("position_metal_unit", row, metalPosition);
			tblFX.setDouble("position_toz", row, metalPositionToz);
		}
		
		Logging.info("FX data generated!");
		
		return tblFX;
	}
}
