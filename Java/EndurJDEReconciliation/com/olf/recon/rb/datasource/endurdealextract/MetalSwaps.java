package com.olf.recon.rb.datasource.endurdealextract;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.FIXED_OR_FLOAT;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.VALUE_STATUS_ENUM;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.utils.Constants;
import com.openlink.util.logging.PluginLog;

/**
 * Gathers swaps related deal attributes for reconciliation
 */
public class MetalSwaps extends AbstractEndurDealExtract 
{
	private int INS_METAL_SWAP;
	
	public MetalSwaps(int windowStartDate, int windowEndDate) throws OException 
	{
		super(windowStartDate, windowEndDate);
		
		INS_METAL_SWAP = Ref.getValue(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, "METAL-SWAP");
	}

	@Override
	protected Table getData() throws OException 
	{
		Table tblSwapsInfo = null;
		Table tblLatestFixingDates = null;
		
		try
		{
			PluginLog.info("Fetching metal swap data..");
			
			/* Get swap info - most of the info comes from user_jm_jde_extract_data */
			tblSwapsInfo = getSwapInfo();
			
			/* Retrieve latest fixed reset for float legs */
			tblLatestFixingDates = getFinalFixingDateForFloatLegs();
			
			Table tblOutput = Table.tableNew("Metal Swaps");
			
			/* Float leg is primary table, since we only want to display deals that have profile status = known */
			if (tblSwapsInfo.getNumRows() > 0)
			{
				tblSwapsInfo.select(tblLatestFixingDates, "last_fixing_date(trade_date)", "deal_num EQ $deal_num");
				
				for (int row = tblSwapsInfo.getNumRows(); row >= 1; row--)
				{
					int dealNum = tblSwapsInfo.getInt("deal_num", row);
					int tradeDate = tblSwapsInfo.getInt("trade_date", row);
					
					if (tradeDate == 0)
					{
						/* 
						 * If trade date is zero (last fixing date), this indicates the swap hasn't started fixing yet
						 * and these need to be excluded from reconciliation
						 */
						tblSwapsInfo.delRow(row);
						PluginLog.info("Metal swap, deal " + dealNum + " hasn't started fixing yet, excluding from reconciliation..");
					}
				}
				
				/* Select distinct keys first */
				tblOutput.select(tblSwapsInfo, "DISTINCT, deal_num, unit, buy_sell, value_date, metal, currency, position_metal_unit, position_toz, trade_date, settlement_value", 
						"deal_num GT -1");
			}
			
			PluginLog.info("Metal swaps data generated!");
			
			return tblOutput;
		}
		finally
		{	
			if (tblSwapsInfo != null)
			{
				tblSwapsInfo.destroy();
			}
			
			if (tblLatestFixingDates != null) 
			{
				tblLatestFixingDates.destroy();
			}
		}
	}
	
	/**
	 * Return metal swaps monetary attributes and jde spot equivalent information
	 * 
	 * @return
	 * @throws OException
	 */
	private Table getSwapInfo() throws OException 
	{
		Table tblData = Table.tableNew("Metals Ssaps info");
		
		String sqlQuery =
			"SELECT \n" +
				"ab.deal_tracking_num AS deal_num, \n" + 
				"ujde.uom AS unit, \n" +
				"ab.buy_sell, \n" +
				"ujde.delivery_date AS value_date, \n" +
				"ujde.to_currency AS currency, \n" +
				"ujde.metal_volume_uom AS position_metal_unit, \n" +
				"ujde.metal_volume_toz AS position_toz, \n" +
				"ujde.settlement_value, \n" +
				"ujde.from_currency AS metal \n" +
			"FROM \n" + 
			"ab_tran ab \n" +
			"LEFT JOIN " + Constants.USER_JM_JDE_EXTRACT_DATA + " ujde ON ab.deal_tracking_num = ujde.deal_num \n" + 
			"WHERE ab.ins_type = " + INS_METAL_SWAP + " \n" +
			"AND ab.current_flag = 1 \n" + 
			"AND ab.tran_status IN (" + getApplicableTransactionStatusesForSQL() + ") \n" +
			"AND CAST(ujde.delivery_date AS DATETIME) >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' \n" + 
			"AND CAST(ujde.delivery_date AS DATETIME) <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "'  \n" + 
			"ORDER by ab.deal_tracking_num";
		
		int ret = DBaseTable.execISql(tblData, sqlQuery);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException("Unable to run query: " + sqlQuery);
		}
		
		tblData.colConvertDateTimeToInt("value_date");
		
		return tblData;
	}
	
	/**
	 * For float legs of metal swap trades, get the most latest fixing date by looking at the reset table
	 * 
	 * @return
	 * @throws OException
	 */
	private Table getFinalFixingDateForFloatLegs() throws OException
	{
		Table tblData = Table.tableNew("Final fixing dates for metal swaps");
		
		String sqlQuery =
			"SELECT \n" +
				"ab.deal_tracking_num AS deal_num, \n" +
				"MAX(r.start_date) AS last_fixing_date \n" +
			"FROM \n" +
			"ab_tran ab \n" +
			"LEFT JOIN ab_tran_event abte ON ab.tran_num = abte.tran_num AND (abte.event_type = 14) \n" +
			"LEFT JOIN ab_tran_event_info atei ON abte.event_num = atei.event_num AND atei.type_id = 20006 -- metal value date \n" + 
			"LEFT JOIN parameter p ON p.ins_num = abte.ins_num AND p.param_seq_num = abte.ins_para_seq_num \n" +
			"LEFT JOIN profile pf ON p.ins_num = pf.ins_num AND pf.param_seq_num = p.param_seq_num \n" +
			"LEFT JOIN reset r ON r.ins_num = ab.ins_num \n" +
			"WHERE ab.ins_type = " + INS_METAL_SWAP + " \n" +
			"AND ab.tran_status IN (" + getApplicableTransactionStatusesForSQL() + ") \n" +
			"AND ab.current_flag = 1 \n" +
			"AND CAST(atei.value AS DATETIME) >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' \n" +
			"AND CAST(atei.value AS DATETIME) <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "' \n" +
			"AND p.fx_flt = " + FIXED_OR_FLOAT.FLOAT_RATE.toInt() + " \n" +
			"AND r.value_status IN (" + VALUE_STATUS_ENUM.VALUE_KNOWN.toInt() + ", " + VALUE_STATUS_ENUM.VALUE_FIXED.toInt() + ") \n" +
			"GROUP BY ab.deal_tracking_num \n" +
			"ORDER by ab.deal_tracking_num";

		int ret = DBaseTable.execISql(tblData, sqlQuery);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new ReconciliationRuntimeException("Unable to run query: " + sqlQuery);
		}

		tblData.colConvertDateTimeToInt("last_fixing_date");
		
		return tblData;
	}
	
	/**
	 * For float legs of metal swap trades, get the first reset date for deals that are yet to start fixing
	 * 
	 * This is usually the starting trade date of the deal, however a hit to the reset table is used here in any any 
	 * reset offsets are applied which would potentially shift the first reset
	 * 
	 * @return
	 * @throws OException
	 */
	@SuppressWarnings("unused")
	private Table getFirstFixingDateForFloatLegs() throws OException
	{
		Table tblData = Table.tableNew("Final fixing dates for metal swaps");
		
		String sqlQuery =
			"SELECT \n" +
				"ab.deal_tracking_num AS deal_num, \n" +
				"MIN(r.start_date) AS first_fixing_date \n" +
			"FROM \n" +
			"ab_tran ab \n" +
			"LEFT JOIN ab_tran_event abte ON ab.tran_num = abte.tran_num AND (abte.event_type = 14) \n" +
			"LEFT JOIN ab_tran_event_info atei ON abte.event_num = atei.event_num AND atei.type_id = 20006 -- metal value date \n" + 
			"LEFT JOIN parameter p ON p.ins_num = abte.ins_num AND p.param_seq_num = abte.ins_para_seq_num \n" +
			"LEFT JOIN profile pf ON p.ins_num = pf.ins_num AND pf.param_seq_num = p.param_seq_num \n" +
			"LEFT JOIN reset r ON r.ins_num = ab.ins_num \n" +
			"WHERE ab.ins_type = " + INS_METAL_SWAP + " \n" +
			"AND ab.tran_status IN (" + getApplicableTransactionStatusesForSQL() + ") \n" +
			"AND ab.current_flag = 1 \n" +
			"AND CAST(atei.value AS DATETIME) >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' \n" +
			"AND CAST(atei.value AS DATETIME) <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "' \n" +
			"AND p.fx_flt = " + FIXED_OR_FLOAT.FLOAT_RATE.toInt() + " \n" +
			"AND r.value_status NOT IN (" + VALUE_STATUS_ENUM.VALUE_KNOWN.toInt() + ", " + VALUE_STATUS_ENUM.VALUE_FIXED.toInt() + ") \n" +
			"GROUP BY ab.deal_tracking_num \n" +
			"ORDER by ab.deal_tracking_num";

		int ret = DBaseTable.execISql(tblData, sqlQuery);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new ReconciliationRuntimeException("Unable to run query: " + sqlQuery);
		}

		tblData.colConvertDateTimeToInt("first_fixing_date");
		
		return tblData;
	}
}
