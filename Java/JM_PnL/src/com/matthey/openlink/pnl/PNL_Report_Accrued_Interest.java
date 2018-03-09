package com.matthey.openlink.pnl;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;

public class PNL_Report_Accrued_Interest extends PNL_ReportEngine
{
	protected void generateOutputTableFormat(Table output) throws OException
	{		
		output.addCol("bunit", COL_TYPE_ENUM.COL_INT);
		output.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		output.addCol("date", COL_TYPE_ENUM.COL_INT);
		
		output.addCol("accrued_pnl", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("accrued_pnl_this_month", COL_TYPE_ENUM.COL_DOUBLE);	
	}

	@Override
	protected void registerConversions(Table output) throws OException 
	{
		regRefConversion(output, "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		regRefConversion(output, "metal_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		
		regDateConversion(output, "date");
	}
	
	@Override
	protected void populateOutputTable(Table output) throws OException
	{
		PluginLog.info("PNL_Report_Accrued_Interest::populateOutputTable called.\n");
		OConsole.message("PNL_Report_Accrued_Interest::populateOutputTable called.\n");
				
		Table interestData = m_interestPNLAggregator.getData();
		
		if ((interestData != null))
		{
			if (interestData.getNumRows() > 0)
			{
				output.select(interestData, "DISTINCT, int_bu (bunit), group (metal_ccy)", "int_bu GE 0");
				output.select(interestData, "SUM, accrued_pnl (accrued_pnl)", "int_bu EQ $bunit AND group EQ $metal_ccy");
				output.select(interestData, "SUM, accrued_pnl_this_month (accrued_pnl_this_month)", "int_bu EQ $bunit AND group EQ $metal_ccy");
			}			
			interestData.destroy();
		}
	}
}