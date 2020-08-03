package com.olf.recon.rb.datasource.endurmetalledgerextract;

import java.util.HashMap;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.recon.rb.datasource.endurdealextract.AbstractEndurDealExtract;
import com.olf.recon.utils.Util;
import com.olf.jm.logging.Logging;

/**
 * Gathers commodity related deal attributes for reconciliation
 */
public class Commodity extends AbstractEndurDealExtract{

	public Commodity(int windowStartDate, int windowEndDate, int lastTradeDate) throws OException {
		super(windowStartDate, windowEndDate, lastTradeDate);
	}

	@Override
	protected Table getData() throws OException {
		Logging.info("Fetching commodity data..");
		
		Table tblCommodity = Table.tableNew("Commodity trades");
		
		String sqlQuery =
				"SELECT \n" + 
					"ab.deal_tracking_num AS deal_num, \n" + 
					"ab.trade_date, \n" +	
					"ab.buy_sell, \n" +
					"SUM(abte.para_position) AS position_metal_unit, -- metal position \n" + 
					"abte.currency AS metal, \n" + 
					"abte.unit AS metal_unit, \n" + 
					"abte.event_date AS value_date, \n" + 
					"abte.event_type AS metal_delivery_event_type, \n" +
					"gpl.location_name AS location \n" +
				"FROM \n" + 
				"ab_tran ab \n" + 
				"INNER JOIN parameter par on par.ins_num = ab.ins_num \n" +
				"INNER JOIN ab_tran_event abte ON ab.tran_num = abte.tran_num AND abte.ins_para_seq_num = par.param_seq_num AND (abte.event_type IN (31,32,47,48,20001,20002,20003)) \n" +
				"LEFT JOIN gas_phys_param_view csh ON csh.ins_num = ab.ins_num AND csh.param_seq_num = par.param_seq_num \n" + 
				"LEFT JOIN gas_phys_location gpl ON gpl.location_id = csh.location_id \n" + 
				"WHERE ab.toolset = " + TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() + " \n" + 
				"AND ab.current_flag = 1 \n" +
				"AND ab.tran_status IN (" + getApplicableTransactionStatusesForSQL() + ") \n" +
				"AND abte.event_date >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' \n" +
				"AND abte.event_date <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "' \n" +
				"AND ab.trade_date <= '" + OCalendar.formatJdForDbAccess(lastTradeDate) + "' \n" +
				"GROUP BY \n" +
				"ab.deal_tracking_num, \n" +
				"ab.trade_date, \n" +
				"ab.buy_sell, \n" +
				"abte.currency, \n" +
				"abte.unit, \n" +
				"abte.event_date, \n" +
				"abte.event_type, \n" +
				"gpl.location_name, \n" +
				"par.param_seq_num \n" +
				"ORDER by ab.deal_tracking_num";
		
		int ret = DBaseTable.execISql(tblCommodity, sqlQuery);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException("Unable to run query: " + sqlQuery);
		}
		
		/* Convert datetime fields to int for ease of processing */
		tblCommodity.colConvertDateTimeToInt("value_date");
		tblCommodity.colConvertDateTimeToInt("trade_date");
		
		int numRows = tblCommodity.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			/* Set the metal currency based on the delivery event */
			int eventType = tblCommodity.getInt("metal_delivery_event_type", row);
			int metalCurrency = getCurrencyFromMetalEvent(eventType);
			tblCommodity.setInt("metal", row, metalCurrency);
		}
		
		/* Add supplementary columns for superclass */
		Util.convertPositionFromTOz(tblCommodity);
		
		Logging.info("Commodity data generated! Number of rows : " + tblCommodity.getNumRows());
				
		return tblCommodity;
	}
	
	/**
	 * Return the metal currency for a Commodity, given the delivery event type 
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
			metalEventCurrency.put(Ref.getValue(SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE, "Gold Delivery"), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XAU"));
			metalEventCurrency.put(Ref.getValue(SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE, "Platinum Delivery"), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XPT"));
			metalEventCurrency.put(Ref.getValue(SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE, "Palladium Delivery"), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XPD"));
			metalEventCurrency.put(Ref.getValue(SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE, "Rhodium Delivery"), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XRH"));
			metalEventCurrency.put(Ref.getValue(SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE, "Iridium"), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XIR"));
			metalEventCurrency.put(Ref.getValue(SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE, "Osmium"), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XOS"));
			metalEventCurrency.put(Ref.getValue(SHM_USR_TABLES_ENUM.EVENT_TYPE_TABLE, "Ruthenium"), Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, "XRU"));			
		}
		if (metalEventCurrency.containsKey(eventType))
		{
			return metalEventCurrency.get(eventType);
		}
		return 0;
	}
}
