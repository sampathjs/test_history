package com.olf.recon.rb.datasource.endurmetalledgerextract;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.EVENT_TYPE_ENUM;
import com.olf.recon.rb.datasource.endurdealextract.AbstractEndurDealExtract;
import com.olf.jm.logging.Logging;
import com.olf.recon.utils.Util;

/**
 * Gathers cash related deal attributes for reconciliation
 */
public class Cash extends AbstractEndurDealExtract{

	public Cash(int windowStartDate, int windowEndDate, int lastTradeDate) throws OException {
		super(windowStartDate, windowEndDate, lastTradeDate);
	}

	@Override
	protected Table getData() throws OException {
		Logging.info("Fetching cash data..");
		
		Table tblCash = Table.tableNew("Cash trades");
		
		String sqlQuery =
				"SELECT \n" + 
					"ab.deal_tracking_num AS deal_num, \n" + 
					"ab.buy_sell, \n" +	
					"ab.trade_date, \n" +
					"abte.event_date AS value_date, \n" + 
					"abte.para_position AS position_metal_unit, -- metal position \n" + 
					"abte.currency AS metal, \n" + 
					"abte.unit AS metal_unit, \n" +
					"ai.info_value AS location \n" +
				"FROM \n" + 
				"ab_tran ab \n" + 
				"INNER JOIN ab_tran_settle_account_view abv ON abv.tran_num = ab.tran_num \n" +
				"LEFT JOIN ab_tran_event abte ON ab.tran_num = abte.tran_num AND abte.event_type =" + EVENT_TYPE_ENUM.EVENT_TYPE_CASH_SETTLE.toInt() + " \n" + 
				"LEFT JOIN account_info ai ON abv.account_id = ai.account_id AND ai.info_type_id = 20006 \n" +
				"WHERE ab.toolset = " + TOOLSET_ENUM.CASH_TOOLSET.toInt() + " \n" + 
				"AND ab.ins_sub_type = " + INS_SUB_TYPE.cash_transfer.toInt() + " \n" +
				"AND ab.current_flag = 1 \n" +
				"AND ab.tran_status IN (" + getApplicableTransactionStatusesForSQL() + ") \n" +
				"AND abv.int_ext = 1 \n" + 
				"AND abte.event_date >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' \n" +
				"AND abte.event_date <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "' \n" +
				"AND ab.trade_date <= '" + OCalendar.formatJdForDbAccess(lastTradeDate) + "' \n" +
				"ORDER by ab.deal_tracking_num";
		
		int ret = DBaseTable.execISql(tblCash, sqlQuery);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException("Unable to run query: " + sqlQuery);
		}
		
		/* Convert datetime fields to int for ease of processing */
		tblCash.colConvertDateTimeToInt("value_date");
		tblCash.colConvertDateTimeToInt("trade_date");
	
		/* Add supplementary columns for superclass */
		Util.convertPositionFromTOz(tblCash);
		
		Logging.info("Cash data generated! Number of rows : " + tblCash.getNumRows());
				
		return tblCash;
	}
}
