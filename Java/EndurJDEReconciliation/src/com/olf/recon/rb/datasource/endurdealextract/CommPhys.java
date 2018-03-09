package com.olf.recon.rb.datasource.endurdealextract;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_TYPE_ENUM;
import com.olf.recon.utils.Constants;
import com.openlink.util.logging.PluginLog;

public class CommPhys extends AbstractEndurDealExtract 
{
	public CommPhys(int windowStartDate, int windowEndDate) throws OException 
	{
		super(windowStartDate, windowEndDate);
	}

	@Override
	protected Table getData() throws OException 
	{
		PluginLog.info("Fetching Comm Phys deals..");
		
		Table tblCommPhys = Table.tableNew("Comm Phys deals");
		
		/* Return all COMM PHYS trades that are interfaced to JDE */
		String sqlQuery = 	
			"SELECT \n" +
				"ab.deal_tracking_num AS deal_num, \n" +
				"ab.trade_date, \n" +
				"ab.buy_sell, \n" +
				"ate.currency AS metal, \n" +
				"ate.unit AS metal_unit, \n" +
				"ate.para_position AS position_metal_unit, \n" +
				"p.pymt_date AS value_date \n" +
			"FROM ab_tran ab \n" +
			"LEFT JOIN ab_tran_event ate ON ab.tran_num = ate.tran_num \n" +
			"LEFT JOIN profile p ON ate.ins_num = p.ins_num AND ate.ins_para_seq_num = p.param_seq_num \n" +
			"WHERE ab.ins_type = 48010 \n" +
			"AND ab.current_flag = 1 \n" +
			"AND ate.currency IN (SELECT id_number FROM currency WHERE precious_metal = 1) \n" +
			"AND ab.tran_type = " + TRAN_TYPE_ENUM.TRAN_TYPE_TRADING.toInt() + " \n" +
			"AND ab.tran_status IN (" + getApplicableTransactionStatusesForSQL() + ") \n" + 
			"AND p.pymt_date >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' AND p.pymt_date <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "' \n" +
			"ORDER by deal_tracking_num";
		
		int ret = DBaseTable.execISql(tblCommPhys, sqlQuery);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new RuntimeException("Unable to run query: " + sqlQuery);
		}
		
		/* Convert datetime fields to int for ease of processing */
		tblCommPhys.colConvertDateTimeToInt("trade_date");
		tblCommPhys.colConvertDateTimeToInt("value_date");
		
		/* Add supplementary columns for superclass */
		tblCommPhys.addCol("position_toz", COL_TYPE_ENUM.COL_DOUBLE);
	
		int numRows = tblCommPhys.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			int metalUnit = tblCommPhys.getInt("metal_unit", row);
			double metalPosition = tblCommPhys.getDouble("position_metal_unit", row);
			
			double metalPositionToz = metalPosition;
			if (metalUnit != Constants.TROY_OUNCES)
			{
				/* 
				 * The db stores all values as Toz (base unit) in ab_tran_events. So if this is a Kg trade (or any other unit different to Toz), 
				 * convert from Toz > trade unit 
				 */
				metalPosition *= Transaction.getUnitConversionFactor(Constants.TROY_OUNCES, metalUnit);
			}

			tblCommPhys.setDouble("position_metal_unit", row, metalPosition);
			tblCommPhys.setDouble("position_toz", row, metalPositionToz);
		}
		
		PluginLog.info("Comm Phys data generated!");
		
		return tblCommPhys;
	}
}
