package com.matthey.openlink.pnl;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 */

public abstract class PnlReportFundingPNLBase extends PNL_ReportEngine {
	
	protected void generateOutputTableFormat(Table output) throws OException {		
		output.addCol("bunit", COL_TYPE_ENUM.COL_INT);
		output.addCol("metal_ccy", COL_TYPE_ENUM.COL_INT);
		
		output.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		output.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
		output.addCol("deal_reset_id", COL_TYPE_ENUM.COL_INT);
		
		output.addCol("date", COL_TYPE_ENUM.COL_INT);
		
		output.addCol("total_funding_pnl", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("total_funding_pnl_today", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("total_funding_pnl_this_month", COL_TYPE_ENUM.COL_DOUBLE);
		
		output.addCol("funding_pnl", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("funding_pnl_today", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("funding_pnl_this_month", COL_TYPE_ENUM.COL_DOUBLE);
		
		output.addCol("funding_interest_pnl", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("funding_interest_pnl_today", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("funding_interest_pnl_this_month", COL_TYPE_ENUM.COL_DOUBLE);		
	}

	@Override
	protected void registerConversions(Table output) throws OException {
		regRefConversion(output, "bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		regRefConversion(output, "metal_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		
		regDateConversion(output, "date");
	}
	
	@Override
	protected void populateOutputTable(Table output) throws OException {
		PluginLog.info("PNL_Report_Funding_PNL::populateOutputTable called.\n");
		int som = OCalendar.getSOM(reportDate);
		
		Table fundingData = m_fundingPNLAggregator.getData();
		Table fundingInterestData = m_fundingInterestPNLAggregator.getData();

		try {
			if (fundingData != null) {
				output.select(fundingData, "DISTINCT, deal_num, deal_leg, deal_pdc, deal_reset_id, date, int_bu (bunit), group (metal_ccy)",  "deal_num GE 0");
			}
			if (fundingInterestData != null) {
				output.select(fundingInterestData, "DISTINCT, deal_num, deal_leg, deal_pdc, deal_reset_id, date, int_bu (bunit), group (metal_ccy)",  "deal_num GE 0");
			}		
			
			// Now normalise the table to only have a single row per each unique combination
			output.group("deal_num, deal_leg, deal_pdc, deal_reset_id, date");
			output.distinctRows();
			
			if ((fundingData != null) && (fundingData.getNumRows() > 0)) {
				output.select(fundingData, "SUM, value (funding_pnl)", "deal_num EQ $deal_num AND deal_leg EQ $deal_leg AND deal_pdc EQ $deal_pdc AND deal_reset_id EQ $deal_reset_id AND date EQ $date");
			}
			if ((fundingInterestData != null) && (fundingInterestData.getNumRows() > 0)) {
				output.select(fundingInterestData, "SUM, value (funding_interest_pnl)", "deal_num EQ $deal_num AND deal_leg EQ $deal_leg AND deal_pdc EQ $deal_pdc AND deal_reset_id EQ $deal_reset_id AND date EQ $date");
			}
			
			int rows = output.getNumRows();
			for (int row = 1; row <= rows; row++) {
				int date = output.getInt("date", row);
				
				if ((date >= som) && (date <= reportDate)) {
					output.setDouble("funding_pnl_this_month", row, output.getDouble("funding_pnl", row));
					output.setDouble("funding_interest_pnl_this_month", row, output.getDouble("funding_interest_pnl", row));
				}
				if (date == reportDate) {
					output.setDouble("funding_pnl_today", row, output.getDouble("funding_pnl", row));
					output.setDouble("funding_interest_pnl_today", row, output.getDouble("funding_interest_pnl", row));
				}
			}
		
			// Now add the funding and interest columns together to get relevant totals
			output.mathAddCol("funding_pnl", "funding_interest_pnl", "total_funding_pnl");
			output.mathAddCol("funding_pnl_this_month", "funding_interest_pnl_this_month", "total_funding_pnl_this_month");
			output.mathAddCol("funding_pnl_today", "funding_interest_pnl_today", "total_funding_pnl_today");

		} finally {
			if (Table.isTableValid(fundingData) == 1) {
				fundingData.destroy();
			}
			if (Table.isTableValid(fundingInterestData) == 1) {
				fundingInterestData.destroy();
			}
		}
			
	}
}