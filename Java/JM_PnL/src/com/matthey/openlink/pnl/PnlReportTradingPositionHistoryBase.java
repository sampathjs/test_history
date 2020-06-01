package com.matthey.openlink.pnl;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.jm.logging.Logging;

public abstract class PnlReportTradingPositionHistoryBase extends PNL_ReportEngine
{
	protected void generateOutputTableFormat(Table output) throws OException
	{		
		output.addCol("bunit", COL_TYPE_ENUM.COL_INT);
		output.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		
		output.addCol("open_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("open_volume", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("open_price", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("open_value", COL_TYPE_ENUM.COL_DOUBLE);
		
		output.addCol("deal_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		output.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
		output.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);
		output.addCol("buy_sell", COL_TYPE_ENUM.COL_INT);
		
		output.addCol("delivery_volume", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("delivery_price", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("delivery_value", COL_TYPE_ENUM.COL_DOUBLE);
		
		output.addCol("deal_profit", COL_TYPE_ENUM.COL_DOUBLE);
		
		output.addCol("close_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("close_volume", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("close_price", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("close_value", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("current_flag", COL_TYPE_ENUM.COL_INT);
	}

	@Override
	protected void registerConversions(Table output) throws OException 
	{
		regRefConversion(output, "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		regRefConversion(output, "metal_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		
		regRefConversion(output, "buy_sell", SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
		
		regDateConversion(output, "open_date");
		regDateConversion(output, "deal_date");
		regDateConversion(output, "close_date");
	}
	
	@Override
	protected void populateOutputTable(Table output) throws OException
	{
		Logging.info("PNL_Report_Trading_Position_History::populateOutputTable called.\n");
		OConsole.message("PNL_Report_Trading_Position_History::populateOutputTable called.\n");
		
		Table dealDetailsData = m_positionHistory.getPositionData();
		
		if ((Table.isTableValid(dealDetailsData) == 1) && (dealDetailsData.getNumRows()>0))
		{
			for (int row = 1; row <= dealDetailsData.getNumRows(); row++)
			{
				dealDetailsData.getTable("trading_pos", row).copyRowAddAllByColName(output);
			}

		}
		output.setColValInt("current_flag", 1);
	}
}