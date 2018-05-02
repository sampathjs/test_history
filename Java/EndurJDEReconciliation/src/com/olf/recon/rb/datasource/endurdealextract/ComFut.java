package com.olf.recon.rb.datasource.endurdealextract;

import java.util.HashMap;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Gathers metal futures related deal attributes for reconciliation
 */
public class ComFut extends AbstractEndurDealExtract 
{
	public ComFut(int windowStartDate, int windowEndDate) throws OException 
	{
		super(windowStartDate, windowEndDate);
	}

	@Override
	protected Table getData() throws OException 
	{
		PluginLog.info("Fetching com fut data..");
		
		Table tblComFut = Table.tableNew("ComFut trades");
		
		String sqlQuery =
			"SELECT \n" + 
				"ab.deal_tracking_num AS deal_num, \n" + 
				"ab.trade_date, \n" +	
				"ab.buy_sell, \n" +
				"abte0.para_position AS settlement_value, \n" + 
				"abte0.currency AS currency, \n" + 
				"abte1.para_position AS position_metal_unit, -- metal position \n" + 
				"abte1.para_position AS position_toz, -- metal position \n" + 
				"abte1.currency AS metal, \n" + 
				"abte1.event_date AS value_date, \n" + 
				"abte1.event_type AS metal_delivery_event_type \n" +
			"FROM \n" + 
			"ab_tran ab \n" + 
			"LEFT JOIN ab_tran_event abte0 ON ab.tran_num = abte0.tran_num AND (abte0.event_type = 14) \n" +  
			"LEFT JOIN ab_tran_event abte1 ON ab.tran_num = abte1.tran_num AND (abte1.event_type IN (32,47)) \n" + 
			"WHERE ab.toolset = " + TOOLSET_ENUM.COM_FUT_TOOLSET.toInt() + " \n" + 
			"AND ab.current_flag = 1 \n" +
			"AND ab.tran_status IN (" + getApplicableTransactionStatusesForSQL() + ") \n" +
			"AND abte1.event_date >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' \n" +
			"AND abte1.event_date <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "' \n" +
			"ORDER by ab.deal_tracking_num";
			
		int ret = DBaseTable.execISql(tblComFut, sqlQuery);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException("Unable to run query: " + sqlQuery);
		}
		
		tblComFut.colConvertDateTimeToInt("value_date");
		tblComFut.colConvertDateTimeToInt("trade_date");
		
		int numRows = tblComFut.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			/* Set the metal currency based on the delivery event */
			int eventType = tblComFut.getInt("metal_delivery_event_type", row);
			int metalCurrency = getCurrencyFromMetalEvent(eventType);
			
			tblComFut.setInt("metal", row, metalCurrency);
		}
		
		PluginLog.info("Com fut data generated!");
				
		return tblComFut;
	}
	
	/**
	 * Return the metal currency for a ComFut, given the delivery event type 
	 * 
	 * @param eventType
	 * @return
	 * @throws OException
	 */
	private static HashMap<Integer, Integer> metalEventCurrency = null;
	private int getCurrencyFromMetalEvent(int eventType) throws OException
	{
		if (metalEventCurrency == null)
		{
			metalEventCurrency = new HashMap<Integer, Integer>();
			
			metalEventCurrency.put(EVENT_TYPE_ENUM.EVENT_TYPE_PLATINUM_DELIVERY.toInt(), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XPT"));
			metalEventCurrency.put(EVENT_TYPE_ENUM.EVENT_TYPE_SILVER_DELIVERY.toInt(), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XAU"));
			metalEventCurrency.put(EVENT_TYPE_ENUM.EVENT_TYPE_PALLADIUM_DELIVERY.toInt(), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XPD"));
			metalEventCurrency.put(EVENT_TYPE_ENUM.EVENT_TYPE_RHODIUM_DELIVERY.toInt(), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XRH"));
		}
		
		if (metalEventCurrency.containsKey(eventType))
		{
			return metalEventCurrency.get(eventType);
		}
		
		return 0;
	}
}
